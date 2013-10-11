/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
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
package org.modeshape.web.server;

import org.modeshape.web.client.JcrService;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import java.math.BigDecimal;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;
import javax.naming.InitialContext;
import org.modeshape.web.shared.JcrNode;
import org.modeshape.web.client.RemoteException;
import org.modeshape.web.shared.JcrPolicy;
import org.modeshape.web.shared.JcrAccessControlList;
import org.modeshape.web.shared.JcrNodeType;
import org.modeshape.web.shared.JcrPermission;
import org.modeshape.web.shared.JcrProperty;
import org.modeshape.web.shared.JcrRepositoryDescriptor;
import org.modeshape.web.shared.ResultSet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class JcrServiceImpl extends RemoteServiceServlet implements JcrService {

    private final static Logger log = Logger.getLogger("JcrServiceImpl");

    @Override
    public boolean login(String jndiName, String userName, String password, String workspace) throws RemoteException {
        try {
            InitialContext context = new InitialContext();
            Repository repository = (Repository) context.lookup(jndiName);

            Session session = null;
            if (userName != null && userName.length() > 0) {
                SimpleCredentials creds = new SimpleCredentials(userName, password.toCharArray());
                session = repository.login(creds, workspace);
            } else {
                session = repository.login(workspace);
            }

            log.info("Logged in :" + session);
            this.getThreadLocalRequest().getSession().setAttribute("session", session);
            return true;
        } catch (Exception e) {
            log.info("Failed Login. " + e.getMessage());
            throw new RemoteException(e.getMessage());
        }
    }

    /**
     * Provides access to the active jcr session.
     * 
     * @return reference to the session object
     * @throws RemoteException when session has expired or does not exists.
     */
    private Session session() throws RemoteException {
        Session session = (Session) this.getThreadLocalRequest().getSession().getAttribute("session");
        if (session == null) {
            throw new RemoteException("Session has expired");
        }
        return session;
    }
    
    @Override
    public JcrNode getRootNode() throws RemoteException {
        try {
            //take root node
            Node root = session().getRootNode();
            
            //convert into value object
            JcrNode node = new JcrNode("root", root.getPath(), root.getPrimaryNodeType().getName());
            node.setProperties(getProperties(root));
            node.setAcessControlList(getAccessList(session().getAccessControlManager(), root));
            
            return node;
        } catch (RepositoryException e) {
            throw new RemoteException(e.getMessage());
        }
    }
    
    @Override
    public List<JcrNode> childNodes(String path) throws RemoteException {
        ArrayList<JcrNode> children = new ArrayList();
        try {
            Node node = (Node) session().getItem(path);
            System.out.println("----- path: " + path);
            NodeIterator it = node.getNodes();

            while (it.hasNext()) {
                Node n = it.nextNode();
                System.out.println("+++++ child: " + n.getName());
                JcrNode childNode = new JcrNode(n.getName(), n.getPath(), n.getPrimaryNodeType().getName());
                childNode.setProperties(getProperties(n));
                childNode.setAcessControlList(getAccessList(session().getAccessControlManager(), node));
                childNode.setMixins(mixins(n));
                childNode.setPropertyDefs(propertyDefs(n));
                children.add(childNode);
            }

        } catch (RepositoryException e) {
            log.log(Level.SEVERE, "Unexpected error", e);
            throw new RemoteException(e.getMessage());
        }

        return children;
    }

    /**
     * Gets the list of properties available to the given node.
     * 
     * @param node the node instance.
     * @return list of property names.
     * @throws RepositoryException 
     */
    private String[] propertyDefs(Node node) throws RepositoryException {
        ArrayList<String> list = new ArrayList();
        
        NodeType primaryType = node.getPrimaryNodeType();
        PropertyDefinition[] defs = primaryType.getPropertyDefinitions();
        
        for (PropertyDefinition def : defs) {
            if (!def.isProtected()) {
                list.add(def.getName());
            }
        }
        
        NodeType[] mixinType = node.getMixinNodeTypes();
        for (NodeType type : mixinType) {
            defs = type.getPropertyDefinitions();
            for (PropertyDefinition def : defs) {
                if (!def.isProtected()) {
                    list.add(def.getName());
                }
            }
        }
        
        String[] res = new String[list.size()];
        list.toArray(res);
        return res;
    }
    
    /**
     * Lists mixin types of the given node.
     * 
     * @param node given node
     * @return list of mixin types names.
     * @throws RepositoryException 
     */
    private String[] mixins(Node node) throws RepositoryException {
        NodeType[] mixins = node.getMixinNodeTypes();
        int len = mixins != null ? mixins.length : 0;
        String[] res = new String[len];
        for (int i = 0; i < res.length; i++) {
            res[i] = mixins[i].getName();
        }
        return res;
    }
    
    
    private JcrAccessControlList getAccessList(AccessControlManager acm, Node node) throws RepositoryException {
        AccessControlList accessList = findAccessList(acm, node);
        if (accessList == null) {
            return JcrAccessControlList.defaultInstance();
        }
        
        JcrAccessControlList acl = new JcrAccessControlList();

        AccessControlEntry[] entries = accessList.getAccessControlEntries();
        for (AccessControlEntry entry : entries) {
            JcrPolicy en = new JcrPolicy();
            en.setPrincipal(entry.getPrincipal().getName());
            Privilege[] privileges = entry.getPrivileges();
            for (Privilege p : privileges) {
                en.add(new JcrPermission(p.getName()));
            }
        }
        return acl;
    }
    
    /**
     * Searches access list for the given node.
     * 
     * @param acm access manager 
     * @param node the node instance
     * @return access list representation.
     * @throws RepositoryException 
     */
    private AccessControlList findAccessList(AccessControlManager acm, Node node) throws RepositoryException {
        AccessControlPolicy[] policy = acm.getPolicies(node.getPath());
        
        if (policy != null && policy.length > 0) {
            return (AccessControlList)policy[0];
        }
        
        Node parent = null;
        try {
            parent = node.getParent();
        } catch (ItemNotFoundException e) {
            return null;
        }
        
        return findAccessList(acm, parent);
    }
    
    /**
     * Reads properties of the given node.
     * 
     * @param node the node instance
     * @return list of node's properties.
     * @throws RepositoryException 
     */
    private Collection<JcrProperty> getProperties(Node node) throws RepositoryException {
        ArrayList<JcrProperty> list = new ArrayList();
        PropertyIterator it = node.getProperties();
        while (it.hasNext()) {
            Property p = it.nextProperty();
            JcrProperty property = new JcrProperty(p.getName(),
                    PropertyType.nameFromValue(p.getType()), values(p));
            property.setProtected(p.getDefinition().isProtected());
            property.setProtected(p.getDefinition().isMultiple());
            list.add(property);
        }
        return list;
    }

    /**
     * Displays property value as string
     * 
     * @param p the property to display
     * @return property value as text string
     * @throws RepositoryException 
     */
    private String values(Property p) throws RepositoryException {
        if (!p.isMultiple()) {
            return p.getString();
        }
        
        Value[] values = p.getValues();
        
        if (values == null) {
            return "";
        }
        
        if (values.length == 1) {
            return values[0].getString();
        }
        
        String s = values[0].getString();
        for (int i = 1; i < values.length; i++) {
            s += "," + values[i].getString();
        }
        
        return s;
    }
    
    @Override
    public JcrRepositoryDescriptor repositoryInfo() {
        Session session = (Session) this.getThreadLocalRequest().getSession().getAttribute("session");
        JcrRepositoryDescriptor desc = new JcrRepositoryDescriptor();
        
        Repository repo = session.getRepository();
        try {
            String keys[] = session.getRepository().getDescriptorKeys();
            for (int i = 0; i < keys.length; i++) {
                Value value = repo.getDescriptorValue(keys[i]);
                desc.add(keys[i], value != null ? value.getString() : "N/A");
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return desc;
    }
    

    @Override
    public ResultSet query(String text, String lang) {
        Session session = (Session) this.getThreadLocalRequest().getSession().getAttribute("session");
        ResultSet rs = new ResultSet();
        try {
            QueryManager qm = session.getWorkspace().getQueryManager();
            Query q = qm.createQuery(text, lang);
            
            QueryResult qr = q.execute();
            
            rs.setColumnNames(qr.getColumnNames());
            ArrayList<String[]> rows = new ArrayList();
            RowIterator it = qr.getRows();
            while (it.hasNext()) {
                Row row = it.nextRow();
                String[] list = new String[qr.getColumnNames().length];

                for (int i = 0; i < qr.getColumnNames().length; i++) {
                    Value v = row.getValue(qr.getColumnNames()[i]);
                    list[i] = v != null ? v.getString() : "null";
                }

                rows.add(list);
            }
            
            rs.setRows(rows);
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return rs;
    }

    @Override
    public String[] supportedQueryLanguages() {
        Session session = (Session) this.getThreadLocalRequest().getSession().getAttribute("session");
        ResultSet rs = new ResultSet();
        try {
            QueryManager qm = session.getWorkspace().getQueryManager();
            return qm.getSupportedQueryLanguages();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void addNode(String path, String name, String primaryType) throws RemoteException {
        try {
            Node node = (Node) session().getItem(path);
            node.addNode(name, primaryType);            
        } catch (RepositoryException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public void removeNode(String path) throws RemoteException {
        try {
            Node node = (Node) session().getItem(path);
            node.remove();
        } catch (RepositoryException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public void addMixin(String path, String mixin) throws RemoteException {
        try {
            Node node = (Node) session().getItem(path);
            node.addMixin(mixin);
            System.out.println("Added mixing to the node: " + path);
        } catch (RepositoryException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public void removeMixin(String path, String mixin) throws RemoteException {
        try {
            Node node = (Node) session().getItem(path);
            node.removeMixin(mixin);
        } catch (RepositoryException e) {
            throw new RemoteException(e.getMessage());
        }
    }
    
    @Override
    public void setProperty(String path, String name, String value) throws RemoteException {
        try {
            Node node = (Node) session().getItem(path);
            switch (type(node,name)) {
                case PropertyType.BOOLEAN :
                    node.setProperty(name, Boolean.parseBoolean(value));
                    break;
                case PropertyType.DATE :
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(new SimpleDateFormat("").parse(value));
                    node.setProperty(name, cal);
                    break;
                case PropertyType.DECIMAL :
                    node.setProperty(name, BigDecimal.valueOf(Double.parseDouble(value)));
                    break;
                case PropertyType.DOUBLE :
                    node.setProperty(name, Double.parseDouble(value));
                    break;
                case PropertyType.LONG :
                    node.setProperty(name, Long.parseLong(value));
                    break;
                case PropertyType.NAME :
                    node.setProperty(name, value);
                    break;
                case PropertyType.PATH :
                    node.setProperty(name, value);
                    break;
                case PropertyType.STRING :
                    node.setProperty(name, value);
                    break;
                case PropertyType.URI :
                    node.setProperty(name, value);
                    break;
                    
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Unexpected error", e);
            throw new RemoteException(e.getMessage());
        }
    }

    /**
     * Determine property type.
     * 
     * @param node
     * @param name
     * @return 
     */
    private int type(Node node, String name) throws RepositoryException {
        PropertyDefinition[] defs = node.getPrimaryNodeType().getPropertyDefinitions();
        for (PropertyDefinition def : defs) {
            if (def.getName().equals(name)) {
                return def.getRequiredType();
            }
        }
        
        NodeType[] mixins = node.getMixinNodeTypes();
        for (NodeType type : mixins) {
            defs = type.getPropertyDefinitions();
            for (PropertyDefinition def : defs) {
                if (def.getName().equals(name)) {
                    return def.getRequiredType();
                }
            }
        }
        
        return -1;
    }
    
    @Override
    public void addAccessList(String path, String principal) throws RemoteException {
        try {
            AccessControlManager acm = session().getAccessControlManager();
            AccessControlPolicy[] policies = acm.getPolicies(path);
            if (policies != null && policies.length > 0) {
                AccessControlList acl = (AccessControlList) policies[0];
                acl.addAccessControlEntry(new SimplePrincipal(principal), new Privilege[]{});
            }
        } catch (RepositoryException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public void updateAccessList(String path, String principal, JcrPermission[] permissions) throws RemoteException {
        try {
            AccessControlManager acm = session().getAccessControlManager();
            AccessControlPolicy[] policies = acm.getPolicies(path);
            if (policies != null && policies.length > 0) {
                AccessControlList acl = (AccessControlList) policies[0];
                acl.removeAccessControlEntry(find(acl.getAccessControlEntries(), principal));
                acl.addAccessControlEntry(new SimplePrincipal(principal), privileges(acm, permissions));
            }
        } catch (RepositoryException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public void removeAccessList(String path, String principal) throws RemoteException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Searches access control entry for given principal inside given entry set.
     * 
     * @param entries the set of entries
     * @param principal the name of the principal
     * @return access control entry.
     */
    private AccessControlEntry find(AccessControlEntry[] entries, String principal) throws RemoteException {
        for (AccessControlEntry entry : entries) {
            if (entry.getPrincipal().getName().equals(principal)) {
                return entry;
            }
        }
        throw new RemoteException("Access list has been deleted");
    }
    
    /**
     * Converts permissions objects to JCR privileges.
     * 
     * @param permissions permissions object
     * @return JCR privileges
     */
    private Privilege[] privileges(AccessControlManager acm, JcrPermission[] permissions) 
            throws AccessControlException, RepositoryException {
        Privilege[] privileges = new Privilege[permissions.length];
        for (int i = 0; i < privileges.length; i++) {
            privileges[i] = acm.privilegeFromName(permissions[i].getName());
        }
        return privileges;
    }
    
    @Override
    public String[] getPrimaryTypes(boolean allowAbstract) throws RemoteException {
        ArrayList<String> list = new ArrayList();
        try {
            NodeTypeManager mgr = session().getWorkspace().getNodeTypeManager();
            NodeTypeIterator it = mgr.getPrimaryNodeTypes();
            while (it.hasNext()) {
                NodeType nodeType = it.nextNodeType();
                if (!nodeType.isAbstract() || allowAbstract) {
                    list.add(nodeType.getName());
                }
            }
            String[] res = new String[list.size()];
            list.toArray(res);            
            return res;
        } catch (RepositoryException e) {
            throw new RemoteException(e.getMessage());
        }
    }
    
    @Override
    public String[] getMixinTypes(boolean allowAbstract) throws RemoteException {
        ArrayList<String> list = new ArrayList();
        try {
            NodeTypeManager mgr = session().getWorkspace().getNodeTypeManager();
            NodeTypeIterator it = mgr.getMixinNodeTypes();
            while (it.hasNext()) {
                NodeType nodeType = it.nextNodeType();
                if (!nodeType.isAbstract() || allowAbstract) {
                    list.add(nodeType.getName());
                }
            }
            String[] res = new String[list.size()];
            list.toArray(res);            
            return res;
        } catch (RepositoryException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public void save()  throws RemoteException {
        try {
            session().save();
        } catch (RepositoryException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public void updateAccessList(String path, JcrAccessControlList acl) throws RemoteException {
        try {
            AccessControlManager acm = session().getAccessControlManager();
            AccessControlPolicy[] policies = acm.getPolicies(path);
            
            AccessControlList accessList = null;
            if (policies != null && policies.length > 0) {
                accessList = (AccessControlList) policies[0];
            } else {
                accessList = (AccessControlList) acm.getApplicablePolicies(path).nextAccessControlPolicy();
            }
        
            clean(accessList);
            update(acm, acl, accessList);
            
            acm.setPolicy(path, accessList);
        } catch (Exception e) {
            throw new RemoteException(e.getMessage());
        }
    }
    
    private void update(AccessControlManager acm, JcrAccessControlList a1, AccessControlList a2) throws AccessControlException, RepositoryException {
        Collection<JcrPolicy> policies = a1.entries();
        for (JcrPolicy policy : policies) {
            a2.addAccessControlEntry(
                    new SimplePrincipal(policy.getPrincipal()), privileges(acm, policy.getPermissions()));
        }
    }
    
    private Privilege[] privileges(AccessControlManager acm, Collection<JcrPermission> permissions) throws AccessControlException, RepositoryException {
        Privilege[] privileges = new Privilege[permissions.size()];
        int i = 0;
        for (JcrPermission permission : permissions) {
            privileges[i++] = acm.privilegeFromName(permission.getName());
        }
        return privileges;
    }
    
    private void clean(AccessControlList acl) throws RepositoryException {
        AccessControlEntry[] entries = acl.getAccessControlEntries();
        for (AccessControlEntry entry : entries) {
            acl.removeAccessControlEntry(entry);
        }
    }
    
    @Override
    public Collection<JcrNodeType> nodeTypes() throws RemoteException {
        ArrayList<JcrNodeType> list = new ArrayList();
        try {
            NodeTypeManager mgr = session().getWorkspace().getNodeTypeManager();

            NodeTypeIterator it = mgr.getAllNodeTypes();
            while (it.hasNext()) {
                NodeType type = it.nextNodeType();
                JcrNodeType jcrType = new JcrNodeType();
                jcrType.setName(type.getName());
                jcrType.setAbstract(type.isAbstract());
                jcrType.setPrimary(!type.isMixin());
                jcrType.setMixin(type.isMixin());
                list.add(jcrType);
            }
        } catch (Exception e) {
            throw new RemoteException(e.getMessage());
        }
        return list;
    }
    
    private class SimplePrincipal implements Principal {
        private String name;
        
        protected SimplePrincipal(String name) {
            this.name = name;
        }
        
        
        @Override
        public String getName() {
            return name;
        }
        
    }
}
