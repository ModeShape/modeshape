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

import static org.modeshape.jcr.ModeShapeLexicon.ACCESS_CONTROLLABLE;
import static org.modeshape.jcr.ModeShapeLexicon.ACCESS_LIST_NODE_NAME;
import static org.modeshape.jcr.ModeShapeLexicon.ACCESS_LIST_NODE_TYPE;
import static org.modeshape.jcr.ModeShapeLexicon.PERMISSION;
import static org.modeshape.jcr.ModeShapeLexicon.PERMISSION_PRINCIPAL_NAME;
import static org.modeshape.jcr.ModeShapeLexicon.PERMISSION_PRIVILEGES_NAME;
import java.security.Principal;
import java.util.Map;
import java.util.Set;
import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
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
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.SessionCache;
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
        JcrAccessControlList acl;
        if (!found(acl = findAccessList(path, true))) {
            // access list is not assigned, use default
            return defaultACL.hasPrivileges(session.context().getSecurityContext(), privileges);
        }

        // perform checking of the privileges
        return acl.isEmpty() || acl.hasPrivileges(session.context().getSecurityContext(), privileges);
    }

    @Override
    public Privilege[] getPrivileges( String path ) throws PathNotFoundException, RepositoryException {
        // recursively search for first available access list
        JcrAccessControlList acl;
        if (!found(acl = findAccessList(path, true))) {
            // access list is not assigned, use default
            return defaultACL.getPrivileges(session.context().getSecurityContext());
        }
        // access list found, ask it to get defined privileges
        return acl.getPrivileges(session.context().getSecurityContext());
    }

    @Override
    public AccessControlPolicy[] getPolicies( String absPath )
        throws PathNotFoundException, AccessDeniedException, RepositoryException {
        if (session.isReadOnly()) {
            throw new AccessDeniedException(JcrI18n.permissionDenied.text(absPath, "read access control content"));
        }

        if (!hasPrivileges(absPath, new Privilege[] {privileges.forName(Privilege.JCR_READ_ACCESS_CONTROL)})) {
            throw new AccessDeniedException();
        }

        AccessControlList acl = findAccessList(absPath, false);
        return acl == null ?  new AccessControlPolicy[0] : new AccessControlPolicy[] {acl};
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
    public AccessControlPolicyIterator getApplicablePolicies( String path )
        throws PathNotFoundException, AccessDeniedException, RepositoryException {
        if (session.isReadOnly()) {
            throw new AccessDeniedException(JcrI18n.permissionDenied.text(path, "read access control content"));
        }
        // Current implementation supports only one policy - access list
        // So we need to check the node specified by path for the policy bound to it
        // if node has already policy bound then we need return empty list and
        // access list otherwise
        Node node = session.getNode(path, true);
        if (node.hasNode(ACCESS_LIST_NODE_NAME.getString())) {
            // node already has policy, nothing is applicable except it
            return AccessControlPolicyIteratorImpl.EMPTY;
        }
        // access list still applicable
        return new AccessControlPolicyIteratorImpl(new JcrAccessControlList(this, path));
    }

    @Override
    public void setPolicy( String path,
                           AccessControlPolicy policy )
        throws PathNotFoundException, AccessControlException, AccessDeniedException, LockException, VersionException,
        RepositoryException {
        if (session.isReadOnly()) {
            throw new AccessDeniedException(JcrI18n.permissionDenied.text(path, "read access control content"));
        }

        if (!hasPrivileges(path, new Privilege[] {privileges.forName(Privilege.JCR_MODIFY_ACCESS_CONTROL)})) {
            throw new AccessDeniedException();
        }

        // we support only access list then cast policy to access list
        if (!(policy instanceof AccessControlList)) {
            throw new AccessControlException("");
        }
        JcrAccessControlList acl = (JcrAccessControlList)policy;

        // binding given policy to the specified path as special child node
        AbstractJcrNode node = session.getNode(path, true);

        if (node.isExternal()) {
            throw new RepositoryException(JcrI18n.aclsOnExternalNodesNotAllowed.text());
        }
        // make node access controllable and add special child node which belongs to the access list
        node.addMixin(ACCESS_CONTROLLABLE.getString(), false);

        AbstractJcrNode aclNode = node.hasNode(ACCESS_LIST_NODE_NAME.getString()) ?
                                  node.getNode(ACCESS_LIST_NODE_NAME.getString(), true) :
                                  node.addAclNode(ACCESS_LIST_NODE_NAME.getString(), ACCESS_LIST_NODE_TYPE.getString());
        // store entries as child nodes of acl
        for (AccessControlEntry ace : acl.getAccessControlEntries()) {
            assert (ace.getPrincipal() != null);
            String name = ace.getPrincipal().getName();

            AbstractJcrNode entryNode = null;
            if (aclNode.hasNode(name)) {
                entryNode = aclNode.getNode(name, true);
            } else {
                entryNode = aclNode.addAclNode(name, PERMISSION.getString());
                session.aclAdded(1);
            }
            entryNode.setPropertyInAccessControlScope(PERMISSION_PRINCIPAL_NAME.getString(), ace.getPrincipal().getName());
            entryNode.setPropertyInAccessControlScope(PERMISSION_PRIVILEGES_NAME.getString(), privileges(ace.getPrivileges()));
        }

        // delete removed entries
        NodeIterator it = aclNode.getNodesInternal();
        while (it.hasNext()) {
            Node entryNode = it.nextNode();
            String name = entryNode.getProperty(PERMISSION_PRINCIPAL_NAME.getString()).getString();
            if (!acl.hasEntry(name)) {
                entryNode.remove();
                session.aclRemoved(1);
            }
        }
    }

    @Override
    public void removePolicy( String path,
                              AccessControlPolicy policy )
        throws PathNotFoundException, AccessControlException, AccessDeniedException, LockException, VersionException,
        RepositoryException {
        if (session.isReadOnly()) {
            throw new AccessDeniedException(JcrI18n.permissionDenied.text(path, "read access control content"));
        }
        try {
            if (!hasPrivileges(path, new Privilege[] {privileges.forName(Privilege.JCR_MODIFY_ACCESS_CONTROL)})) {
                throw new AccessDeniedException();
            }
            AbstractJcrNode node = session.getNode(path);
            if (node.isExternal()) {
                throw new RepositoryException(JcrI18n.aclsOnExternalNodesNotAllowed.text());
            }
            if (node.hasNode(ACCESS_LIST_NODE_NAME.getString())) {
                AbstractJcrNode aclNode = node.getNode(ACCESS_LIST_NODE_NAME.getString(), true);
                session.aclRemoved(aclNode.childCount());
                aclNode.remove();
                node.removeMixin(ACCESS_CONTROLLABLE.getString());
            }
        } catch (PathNotFoundException e) {
        }
    }

    /**
     * Recursively searches for the available access list.
     * 
     * @param absPath the absolute path of the node
     * @return JCR defined access list object.
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    private JcrAccessControlList findAccessList( String absPath, boolean searchParents ) throws PathNotFoundException, RepositoryException {
        //this will not load any nodes in the JCR session, but will load the entire hierarchy in the node cache
        CachedNode startingNode = session.cachedNode(session.pathFactory().create(absPath), false);
        SessionCache sessionCache = session.cache();
        Map<String, Set<String>> permissions = startingNode.getPermissions(sessionCache);
        CachedNode node = startingNode;
        if (permissions == null && searchParents) {
            //walk up the hierarchy until we get a set of permissions or we reach the root or a missing parent
            while (permissions == null) {
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

        if (permissions == null) {
            return null;
        }

        // create a new access list object
        String aclPath = startingNode.getKey().equals(node.getKey()) ? absPath : node.getPath(sessionCache).getString();
        JcrAccessControlList acl = new JcrAccessControlList(this, aclPath);
        for (String principalName : permissions.keySet()) {
            Set<String> privileges = permissions.get(principalName);
            acl.addAccessControlEntry(principal(principalName), privileges(privileges));
        }
        return acl;
    }

    /**
     * Extracts names of the given privileges.
     * 
     * @param privileges the list of privileges.
     * @return names of the given privileges.
     */
    private String[] privileges( Privilege[] privileges ) {
        String[] names = new String[privileges.length];
        for (int i = 0; i < privileges.length; i++) {
            names[i] = privileges[i].getName();
        }
        return names;
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

    /**
     * Tests given object for null.
     * 
     * @param o the given object
     * @return true if the given object not null and false otherwise.
     */
    private boolean found( Object o ) {
        return o != null;
    }

    public boolean hasPermission( Path absPath,
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
