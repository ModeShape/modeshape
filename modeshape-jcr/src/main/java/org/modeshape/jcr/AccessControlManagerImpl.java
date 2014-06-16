/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr;

import java.security.Principal;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
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
 */
public class AccessControlManagerImpl implements AccessControlManager {

    public static final String MODE_ACCESS_CONTROLLABLE = "mode:accessControllable";
    public static final String MODE_ACCESS_LIST_NODE = "mode:Acl";
    private static final String ACCESS_LIST_NODE = "mode:acl";
    private static final String MODE_ACCESS_LIST_ENTRY_NODE = "mode:Permission";
    private static final String PRINCIPAL_NAME = "name";
    private static final String PRIVILEGES = "privileges";

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
        if (!found(acl = findAccessList(path))) {
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
        if (!found(acl = findAccessList(path))) {
            // access list is not assigned, use default
            return defaultACL.getPrivileges(session.context().getSecurityContext());
        }
        // access list found, ask it to get defined privileges
        Privilege[] pp = acl.getPrivileges(session.context().getSecurityContext());
        return pp;
    }

    @Override
    public AccessControlPolicy[] getPolicies( String path )
        throws PathNotFoundException, AccessDeniedException, RepositoryException {
        if (session.isReadOnly()) {
            throw new AccessDeniedException(JcrI18n.permissionDenied.text(path, "read access control content"));
        }

        if (!hasPrivileges(path, new Privilege[] {privileges.forName(Privilege.JCR_READ_ACCESS_CONTROL)})) {
            throw new AccessDeniedException();
        }

        Node node = session.getNode(path, true);
        if (node.hasNode(ACCESS_LIST_NODE)) {
            JcrAccessControlList policy = new JcrAccessControlList(this, path);

            // load entries
            AbstractJcrNode aclNode = ((AbstractJcrNode)node).getNode(ACCESS_LIST_NODE, true);
            NodeIterator it = aclNode.getNodesInternal();
            while (it.hasNext()) {
                Node entryNode = it.nextNode();

                String principalName = entryNode.getProperty(PRINCIPAL_NAME).getString();
                Value[] values = entryNode.getProperty(PRIVILEGES).getValues();

                Privilege[] privileges = new Privilege[values.length];
                for (int i = 0; i < privileges.length; i++) {
                    privileges[i] = this.privilegeFromName(values[i].getString());
                }

                policy.addAccessControlEntry(principal(principalName), privileges);
            }
            return new AccessControlPolicy[] {policy};
        }
        return new AccessControlPolicy[] {};
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
        if (node.hasNode(ACCESS_LIST_NODE)) {
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
        // make node access controllable and add specsial child node
        // which belongs to the access list
        node.addMixin(MODE_ACCESS_CONTROLLABLE, false);

        AbstractJcrNode aclNode = node.hasNode(ACCESS_LIST_NODE) ? node.getNode(ACCESS_LIST_NODE, true) : node.addAclNode(ACCESS_LIST_NODE,
                                                                                                                    MODE_ACCESS_LIST_NODE);
        // store entries as child nodes of acl
        for (AccessControlEntry ace : acl.getAccessControlEntries()) {
            assert (ace.getPrincipal() != null);
            String name = ace.getPrincipal().getName();

            AbstractJcrNode entryNode = null;
            if (aclNode.hasNode(name)) {
                entryNode = aclNode.getNode(name, true);
            } else {
                entryNode = aclNode.addAclNode(name, MODE_ACCESS_LIST_ENTRY_NODE);
                session.aclAdded(1);
            }
            entryNode.setPropertyInAccessControlScope(PRINCIPAL_NAME, ace.getPrincipal().getName());
            entryNode.setPropertyInAccessControlScope(PRIVILEGES, privileges(ace.getPrivileges()));
        }

        // delete removed entries
        NodeIterator it = aclNode.getNodesInternal();
        while (it.hasNext()) {
            Node entryNode = it.nextNode();
            String name = entryNode.getProperty(PRINCIPAL_NAME).getString();
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
            if (node.hasNode(ACCESS_LIST_NODE)) {
                AbstractJcrNode aclNode = node.getNode(ACCESS_LIST_NODE, true);
                session.aclRemoved(aclNode.childCount());
                aclNode.remove();
                node.removeMixin(MODE_ACCESS_CONTROLLABLE);
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
    public JcrAccessControlList findAccessList( String absPath ) throws PathNotFoundException, RepositoryException {
        AbstractJcrNode node = session.getNode(absPath, true);
        while (!node.hasNode(ACCESS_LIST_NODE)) {
            try {
                node = node.getParent();
            } catch (ItemNotFoundException e) {
                break;
            }
        }

        if (node.hasNode(ACCESS_LIST_NODE)) {
            return acl(node.getNode(ACCESS_LIST_NODE, true));
        }

        return null;
    }

    /**
     * Constructs AccessControlList object from node.
     * 
     * @param node the node which represents the access list.
     * @return JCR defined AccessControlList object.
     * @throws RepositoryException
     */
    private JcrAccessControlList acl( AbstractJcrNode node ) throws RepositoryException {
        // create new access list object
        JcrAccessControlList acl = new JcrAccessControlList(this, node.getPath());

        // fill access list with entries
        NodeIterator entryNodes = node.getNodesInternal();
        while (entryNodes.hasNext()) {
            // pickup next entry
            Node entry = entryNodes.nextNode();

            String name = entry.getProperty(PRINCIPAL_NAME).getString();
            Value[] privileges = entry.getProperty(PRIVILEGES).getValues();

            acl.addAccessControlEntry(principal(name), privileges(privileges));
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
    private Privilege[] privileges( Value[] names ) throws ValueFormatException, AccessControlException, RepositoryException {
        Privilege[] privileges = new Privilege[names.length];
        for (int i = 0; i < names.length; i++) {
            privileges[i] = privilegeFromName(names[i].getString());
        }
        return privileges;
    }

    /**
     * Tests given object for null.
     * 
     * @param o the givem object
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