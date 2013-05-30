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

import javax.jcr.AccessDeniedException;
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
import org.modeshape.jcr.security.AbstractSecurityContext;
import org.modeshape.jcr.security.AdvancedAuthorizationProvider;
import org.modeshape.jcr.security.acl.AccessControlPolicyIteratorImpl;
import org.modeshape.jcr.security.acl.JcrAccessControlList;
import org.modeshape.jcr.security.acl.PrincipalImpl;
import org.modeshape.jcr.security.acl.Privileges;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;

/**
 *
 * @author kulikov
 */
public class AccessControlManagerImpl extends AbstractSecurityContext 
    implements AccessControlManager, AdvancedAuthorizationProvider {
    
    public static final String NAME = "ACCESS_CONTROL";    
    public static final String NT_ACCESS_CONTROLLABLE = "mix:accessControllable";
    public static final String ACCESS_LIST_NODE = "mode:acl";
    public static final String NT_ACCESS_LIST_NODE = "mode:Acl";
    public static final String NT_PERMISSIONS = "mode:Acl";
    
    private JcrSession session;
    
    public AccessControlManagerImpl(JcrSession session) {
        this.session = session;
    }
    
    @Override
    public Privilege[] getSupportedPrivileges(String path) throws PathNotFoundException, RepositoryException {
        return Privileges.listOfSupported();
    }

    @Override
    public Privilege privilegeFromName(String name) throws AccessControlException, RepositoryException {
        return Privileges.forName(name);
    }

    @Override
    public boolean hasPrivileges(String path, Privilege[] privileges) throws PathNotFoundException, RepositoryException {
        session.addContextData("role", "ACCESS_CONTROL");
        try {
            //recursively search for first available access list
            JcrAccessControlList acl;
            if (!found(acl = findAccessList(path))) {
                //access list is not assigned so grant everything
                return true;
            }
            //access list found, ask it to perform testing
            return acl.hasPrivileges(session.getUserID(), privileges);
        } finally {
            session.addContextData("role", null);
        }
    }

    @Override
    public Privilege[] getPrivileges(String path) throws PathNotFoundException, RepositoryException {
        session.addContextData("role", "ACCESS_CONTROL");
        try {
            //recursively search for first available access list
            JcrAccessControlList acl;
            if (!found(acl = findAccessList(path))) {
                //access list is not assigned so grant everything
                return new Privilege[]{Privileges.ALL};
            }
            //access list found, ask it to get defined privileges
            return acl.getPrivileges(session.getUserID());
        } finally {
            session.addContextData("role", null);
        }
    }

    @Override
    public AccessControlPolicy[] getPolicies(String path) throws PathNotFoundException, AccessDeniedException, RepositoryException {
        if (!hasPrivileges(path, new Privilege[]{Privileges.READ_ACCESS_CONTROL})) {
            throw new AccessDeniedException();
        }
        session.addContextData("role", "ACCESS_CONTROL");
        try {
            Node node = session.getNode(path);
            if (node.hasNode(ACCESS_LIST_NODE)) {
                JcrAccessControlList policy = new JcrAccessControlList();
                return new AccessControlPolicy[]{policy};
            }
            return new AccessControlPolicy[]{};
        } finally {
            session.addContextData("role", null);
        }
    }

    @Override
    public AccessControlPolicy[] getEffectivePolicies(String path) throws PathNotFoundException, AccessDeniedException, RepositoryException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public AccessControlPolicyIterator getApplicablePolicies(String path) throws PathNotFoundException, AccessDeniedException, RepositoryException {
        session.addContextData("role", "ACCESS_CONTROL");
        try {
            //Current implementation supports only one policy - access list
            //So we need to check the node specified by path for the policy bound to it
            //if node has already policy bound then we need return empty list and
            //access list otherwise
            Node node = session.getNode(path);
            if (node.hasNode(ACCESS_LIST_NODE)) {
                //node already has policy, nothing is applicable except it
                return AccessControlPolicyIteratorImpl.EMPTY;
            }
            //access list still applicable
            return new AccessControlPolicyIteratorImpl(new JcrAccessControlList());
        } finally {
            session.addContextData("role", null);
        }
    }

    @Override
    public void setPolicy(String path, AccessControlPolicy policy) throws PathNotFoundException, AccessControlException, AccessDeniedException, LockException, VersionException, RepositoryException {
        if (!hasPrivileges(path, new Privilege[]{Privileges.MODIFY_ACCESS_CONTROL})) {
            throw new AccessDeniedException();
        }
        
        session.addContextData("role", "ACCESS_CONTROL");
        try {
            //we support only access list then cast policy to access list
            AccessControlList acl = (AccessControlList) policy;

            //binding given policy to the specified path as special child node
            AbstractJcrNode node = session.getNode(path);
            //make node access controllable and add specsial child node 
            //which belongs to the access list
            node.addMixin(NT_ACCESS_CONTROLLABLE, false);

            Node policyNode = node.addNode(ACCESS_LIST_NODE, "mode:Acl");

            //store entries as child nodes of acl
            for (AccessControlEntry ace : acl.getAccessControlEntries()) {
                String name = ace.getPrincipal().getName();

                //store access list entry
                AbstractJcrNode entryNode = ((JcrNode) policyNode).addNode(name, "mode:Permission");
                entryNode.setProperty("name", ace.getPrincipal().getName());
                entryNode.setProperty("privileges", privileges(ace.getPrivileges()));
            }
        } finally {
            session.addContextData("role", null);
        }
    }

    @Override
    public void removePolicy(String path, AccessControlPolicy policy) throws PathNotFoundException, AccessControlException, AccessDeniedException, LockException, VersionException, RepositoryException {
        session.addContextData("role", "ACCESS_CONTROL");
        try {
            if (!hasPrivileges(path, new Privilege[]{Privileges.MODIFY_ACCESS_CONTROL})) {
                throw new AccessDeniedException();
            }
            Node node = session.getNode(path);
            if (node.hasNode(ACCESS_LIST_NODE)) {
                Node aclNode = node.getNode(ACCESS_LIST_NODE);
                aclNode.remove();
                node.removeMixin("mix:accessControllable");
            }
        } finally {
            session.addContextData("role", null);
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
    public JcrAccessControlList findAccessList(String absPath) throws PathNotFoundException, RepositoryException {
        Path path = pathFactory().create(absPath);
        while (!path.isRoot()) {
            Node node;
            try {
                node = session.getNode(path.toString());
            } catch (PathNotFoundException e) {
                return null;
            }
            if (node.hasNode(ACCESS_LIST_NODE)) {
                return acl(node.getNode(ACCESS_LIST_NODE));
            }
            
            path = path.getAncestor(1);
        }
        
        Node node = session.getRootNode();
        if (node.hasNode(ACCESS_LIST_NODE)) {
            return acl(node.getNode(ACCESS_LIST_NODE));
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
    private JcrAccessControlList acl(Node node) throws RepositoryException {
        //create new access list object
        JcrAccessControlList acl = new JcrAccessControlList();
        
        //fill access list with entries
        NodeIterator entryNodes = node.getNodes();
        while (entryNodes.hasNext()) {
            //pickup next entry 
            Node entry = entryNodes.nextNode();
            
            String name = entry.getProperty("name").getString();
            Value[] privileges = entry.getProperty("privileges").getValues();
            
            acl.addAccessControlEntry(new PrincipalImpl(name), privileges(privileges));
        }
        return acl;
    }
    
    /**
     * Extracts names of the given privileges.
     * 
     * @param privileges the list of privileges.
     * @return names of the given privileges.
     */
    private String[] privileges(Privilege[] privileges) {
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
    private Privilege[] privileges(Value[] names) throws ValueFormatException, AccessControlException, RepositoryException {
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
    private boolean found(Object o) {
        return o != null;
    }
    
    /**
     * Gets access to the path factory.
     * 
     * @return path factory object.
     */
    private PathFactory pathFactory() {
        return session.context().getValueFactories().getPathFactory();
    }

    @Override
    public boolean hasPermission(Context context, Path absPath, String... actions) {
        Privilege[] privileges = new Privilege[actions.length];  
        for (int i = 0; i < actions.length; i++) {
            privileges[i] = Privileges.forAction(actions[i]);
        }
        
        try {
            return this.hasPrivileges(absPath.toString(), privileges);
        } catch (Exception e) {
            return true;
        } 
    }

    @Override
    public boolean isAnonymous() {
        return session.context().getSecurityContext().isAnonymous();
    }

    @Override
    public String getUserName() {
        return session.getUserID();
    }

    @Override
    public boolean hasRole(String roleName) {
        return true;
    }

    @Override
    public void logout() {
    }
    
}
