/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr;

import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.jcr.AccessDeniedException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;
import javax.jcr.version.VersionException;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.security.SecurityContext;
import org.modeshape.jcr.security.SimplePrincipal;
import org.modeshape.jcr.security.acl.AccessControlPolicyIteratorImpl;
import org.modeshape.jcr.security.acl.JcrAccessControlList;
import org.modeshape.jcr.security.acl.Privileges;
import org.modeshape.jcr.value.Path;

/**
 * AccessControlManager implementation. AccessControlManager has been implemented suppose that node is associated with access
 * control list which defines deny/allow actions for the given principal. Principals are transparent and can represent users or
 * groups. ACLs are stored per node in a special child node called <bold>mode:acl</bold>. This node has a list of <bold>
 * mode:{$principal_name}</bold> child nodes which has multi-value property permissions where permissions are defined by JCR
 * specifications. {node} {mode:AccessControllable} +mode:acl{mode:Acl} +user-name{mode:permission} -permissions {String} To make
 * node access controllable ModeShape adds "mode:AccessControllable" type to mixin types of the node. Access list related nodes
 * are defined as protected do disallow normal add/remove/save item methods. Access list defined for the node also has affect on
 * all child nodes unless child node defines its own Access list. Empty ACL means all permissions. Initially the default access
 * list is assigned to the root node with all permissions granted to everyone. Access Control Manager implementation does not
 * break any existing mechanism of authentications. It acts as an abstract secondary resource control feature. On the first stage
 * one of the existing security acts and if it grants permission the ACL is asked to check permissions.
 * 
 * @author kulikov
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class AccessControlManagerImpl implements AccessControlManager {

    private static final AccessControlPolicy[] EMPTY_POLICIES = new AccessControlPolicy[0];

    // session under resource control
    private final JcrSession session;

    // list of objects implemented privileges
    private final Privileges privileges;

    // default access list granted all permissions to everyone.
    private final JcrAccessControlList defaultACL;

    protected AccessControlManagerImpl( JcrSession session ) {
        this.session = session;
        this.privileges = new Privileges(session);
        this.defaultACL = JcrAccessControlList.defaultAcl(this);
    }

    /**
     * Gets full list of known and supported privileges irrespective of the path.
     * 
     * @return list of privileges.
     */
    public Privilege[] privileges() {
        return privileges.listOfSupported();
    }

    @Override
    public Privilege[] getSupportedPrivileges( String path ) {
        return privileges.listOfSupported();
    }

    @Override
    public Privilege privilegeFromName( String name ) throws AccessControlException, RepositoryException {
        Privilege p = privileges.forName(name);
        if (p == null) {
            throw new AccessControlException(name + " is not a valid name for privilege");
        }
        return p;
    }

    @Override
    public boolean hasPrivileges( String path,
                                  Privilege[] privileges ) throws PathNotFoundException, RepositoryException {
        // recursively search for first available access list
        JcrAccessControlList acl = getApplicableACL(path);
        return acl.isEmpty() || acl.hasPrivileges(securityContext(), privileges);
    }

    private JcrAccessControlList getApplicableACL( String path ) throws RepositoryException {
        JcrAccessControlList acl = findAccessList(path, true);
        return acl != null ? acl : defaultACL;
    }

    @Override
    public Privilege[] getPrivileges( String path ) throws PathNotFoundException, RepositoryException {
        // recursively search for first available access list
        JcrAccessControlList acl = getApplicableACL(path);
        // access list found, ask it to get defined privileges
        return acl.getPrivileges(securityContext());
    }

    @Override
    public AccessControlPolicy[] getPolicies( String absPath )
        throws PathNotFoundException, AccessDeniedException, RepositoryException {
        if (session.isReadOnly()) {
            throw new AccessDeniedException(JcrI18n.permissionDenied.text(absPath, "read access control content"));
        }

        if (!hasPrivileges(absPath, new Privilege[] {privileges.forName(Privilege.JCR_READ_ACCESS_CONTROL)})) {
            throw new AccessDeniedException(JcrI18n.permissionDenied.text(absPath, "read access control content"));
        }

        AccessControlList acl = findAccessList(absPath, false);
        return acl == null ? EMPTY_POLICIES : new AccessControlPolicy[] {acl};
    }

    private SecurityContext securityContext() {
        return session.context().getSecurityContext();
    }

    @Override
    public AccessControlPolicy[] getEffectivePolicies( String path )
        throws PathNotFoundException, AccessDeniedException, RepositoryException {
        AccessControlPolicy[] policies = getPolicies(path);
        if (policies.length == 0) {
            return new AccessControlPolicy[] {(AccessControlPolicy)this.getApplicablePolicies(path).next()};
        }
        return policies;
    }

    @Override
    public AccessControlPolicyIterator getApplicablePolicies( String absPath )
        throws PathNotFoundException, AccessDeniedException, RepositoryException {
        if (session.isReadOnly()) {
            throw new AccessDeniedException(JcrI18n.permissionDenied.text(absPath, "read access control content"));
        }

        JcrAccessControlList acl = getApplicableACL(absPath);
        if (!acl.isEmpty()
            && !acl.hasPrivileges(securityContext(), new Privilege[] {privileges.forName(Privilege.JCR_READ_ACCESS_CONTROL)})) {
            throw new AccessDeniedException();
        }

        CachedNode node = session.cachedNode(session.pathFactory().create(absPath), false);
        if (node.hasACL(session.cache())) {
            // we only support 1 ACL per node; therefore if the node already has an ACL, we don't want to allow any additional
            // ones
            return AccessControlPolicyIteratorImpl.EMPTY;
        }
        // the node doesn't have an ACL yet, so return a new, empty ACL which can be used by clients to set privileges
        return new AccessControlPolicyIteratorImpl(new JcrAccessControlList(absPath));
    }

    @Override
    public void setPolicy( String absPath,
                           AccessControlPolicy policy )
        throws PathNotFoundException, AccessControlException, AccessDeniedException, LockException, VersionException,
        RepositoryException {
        if (session.isReadOnly()) {
            throw new AccessDeniedException(JcrI18n.permissionDenied.text(absPath, "read access control content"));
        }

        if (!hasPrivileges(absPath, new Privilege[] {privileges.forName(Privilege.JCR_MODIFY_ACCESS_CONTROL)})) {
            throw new AccessDeniedException(JcrI18n.permissionDenied.text(absPath, "modify access control content"));
        }

        // we support only access list then cast policy to access list
        if (!(policy instanceof JcrAccessControlList)) {
            throw new AccessControlException("Invalid policy class (expected JcrAccessControlList): "
                                             + policy.getClass().getSimpleName());
        }
        JcrAccessControlList acl = (JcrAccessControlList)policy;
        Map<String, Set<String>> privilegesByPrincipalName = privilegesByPrincipalName(acl);
        try {
            CachedNode cacheNode = session.cachedNode(session.pathFactory().create(absPath), false);
            SessionCache cache = session.cache();
            MutableCachedNode mutableNode = cache.mutable(cacheNode.getKey());
            MutableCachedNode.PermissionChanges permissionChanges = mutableNode.setPermissions(cache, privilegesByPrincipalName);
            session.aclAdded(permissionChanges.addedPrincipalsCount());
            session.aclRemoved(permissionChanges.removedPrincipalsCount());
        } catch (UnsupportedOperationException e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    public void removePolicy( String absPath,
                              AccessControlPolicy policy )
        throws PathNotFoundException, AccessControlException, AccessDeniedException, LockException, VersionException,
        RepositoryException {
        if (session.isReadOnly()) {
            throw new AccessDeniedException(JcrI18n.permissionDenied.text(absPath, "read access control content"));
        }

        if (!hasPrivileges(absPath, new Privilege[] {privileges.forName(Privilege.JCR_MODIFY_ACCESS_CONTROL)})) {
            throw new AccessDeniedException(JcrI18n.permissionDenied.text(absPath, "modify access control content"));
        }

        try {
            CachedNode cacheNode = session.cachedNode(session.pathFactory().create(absPath), false);
            SessionCache cache = session.cache();
            MutableCachedNode mutableNode = cache.mutable(cacheNode.getKey());
            MutableCachedNode.PermissionChanges permissionChanges = mutableNode.removeACL(cache);
            session.aclRemoved(permissionChanges.removedPrincipalsCount());
        } catch (UnsupportedOperationException e) {
            throw new RepositoryException(e);
        }
    }

    private Map<String, Set<String>> privilegesByPrincipalName( JcrAccessControlList acl ) {
        Map<String, Set<String>> result = new HashMap<>();
        for (AccessControlEntry ace : acl.getAccessControlEntries()) {
            assert (ace.getPrincipal() != null);
            String name = ace.getPrincipal().getName();
            Set<String> privileges = new HashSet<>();
            for (Privilege privilege : ace.getPrivileges()) {
                privileges.add(privilege.getName());
            }
            result.put(name, privileges);
        }
        return result;
    }

    /**
     * Recursively searches for the available access list. If this method is invoked with the {@code searchParents} flag, it will
     * attempt to find the first non-empty permissions set on a node in the hierarchy. 
     * 
     * @param absPath the absolute path of the node
     * @param searchParents flag specifying whether the ancestors should be searched for the access control list
     * @return JCR defined access list object.
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    private JcrAccessControlList findAccessList( String absPath,
                                                 boolean searchParents ) throws PathNotFoundException, RepositoryException {
        // this will not load any nodes in the JCR session, but might load the entire hierarchy in the node cache
        CachedNode startingNode = session.cachedNode(session.pathFactory().create(absPath), false);
        SessionCache sessionCache = session.cache();
        Map<String, Set<String>> permissions = startingNode.getPermissions(sessionCache);
        CachedNode node = startingNode;
        // walk up the hierarchy until we get a set of non-empty permissions or we reach the root or a missing parent
        if (searchParents) {
            while (permissions == null || permissions.isEmpty()) {
                NodeKey parentKey = node.getParentKey(sessionCache);
                if (parentKey == null) {
                    break;
                }
                node = sessionCache.getNode(parentKey);
                if (node == null) {
                    break;
                }
                permissions = node.getPermissions(sessionCache);
            }
        }

        if (permissions == null || node == null) {
            return null;
        }

        // create a new access list object
        String aclPath = startingNode.getKey().equals(node.getKey()) ? absPath : node.getPath(sessionCache).getString();
        JcrAccessControlList acl = new JcrAccessControlList(aclPath);
        for (String principalName : permissions.keySet()) {
            Set<String> privileges = permissions.get(principalName);
            acl.addAccessControlEntry(principal(principalName), privileges(privileges));
        }
        return acl;
    }

    /**
     * Constructs list of Privilege objects using privilege's name.
     * 
     * @param names names of privileges
     * @return Privilege objects.
     * @throws ValueFormatException
     * @throws AccessControlException
     * @throws RepositoryException
     */
    private Privilege[] privileges( Set<String> names ) throws ValueFormatException, AccessControlException, RepositoryException {
        Privilege[] privileges = new Privilege[names.size()];
        int i = 0;
        for (String name : names) {
            privileges[i++] = privilegeFromName(name);
        }
        return privileges;
    }

    protected boolean hasPermission( Path absPath,
                                     String... actions ) {
        // convert actions to privileges
        Privilege[] permissions = new Privilege[actions.length];
        for (int i = 0; i < actions.length; i++) {
            permissions[i] = privileges.forAction(actions[i]);
        }

        // check privileges for the given path
        try {
            return this.hasPrivileges(absPath.toString(), permissions);
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Gets principal instance for the given name. This method uses feature of the security context to discover known principals.
     * 
     * @param name the name of the principal.
     * @return principal instance.
     */
    private Principal principal( String name ) {
        return SimplePrincipal.newInstance(name);
    }
}
