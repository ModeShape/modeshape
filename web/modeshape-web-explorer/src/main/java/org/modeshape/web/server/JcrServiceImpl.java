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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
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
import org.modeshape.common.logging.Logger;
import org.modeshape.web.client.JcrService;
import org.modeshape.web.client.RemoteException;
import org.modeshape.web.server.impl.ConnectorImpl;
import org.modeshape.web.shared.Acl;
import org.modeshape.web.shared.JcrAccessControlList;
import org.modeshape.web.shared.JcrNode;
import org.modeshape.web.shared.JcrNodeType;
import org.modeshape.web.shared.JcrPermission;
import org.modeshape.web.shared.JcrPolicy;
import org.modeshape.web.shared.JcrProperty;
import org.modeshape.web.shared.JcrRepositoryDescriptor;
import org.modeshape.web.shared.Policy;
import org.modeshape.web.shared.RepositoryName;
import org.modeshape.web.shared.ResultSet;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings( "serial" )
public class JcrServiceImpl extends RemoteServiceServlet implements JcrService {

    private final static Logger logger = Logger.getLogger(JcrServiceImpl.class);

    private Connector connector() throws RemoteException {
        Connector connector = (Connector)getThreadLocalRequest().getSession(true).getAttribute("connector");
        if (connector == null) {
            connector = new ConnectorImpl();
            getThreadLocalRequest().getSession(true).setAttribute("connector", connector);
        }
        return connector;
    }

    @Override
    public String getRequestedURI() {
        String uri = (String)getThreadLocalRequest().getSession(true).getAttribute("initial.uri");
        if (uri != null) {
            logger.debug("Requested URI " + uri);
            return uri;
        }

        uri = getThreadLocalRequest().getRequestURI();
        String servletPath = getThreadLocalRequest().getServletPath();

        String res = uri.substring(0, uri.indexOf(servletPath));
        logger.debug("Requested URI " + uri);
        return res;
    }

    @Override
    public String getUserName() throws RemoteException {
        String uname = (String)getThreadLocalRequest().getSession().getAttribute("uname");
        String passwd = (String)getThreadLocalRequest().getSession().getAttribute("password");
        if (uname != null) {
            connector().login(uname, passwd);
        }
        String res = connector().userName();
        return res;
    }

    @Override
    public Collection<RepositoryName> getRepositories() throws RemoteException {
        return connector().getRepositories();
    }

    @Override
    public Collection<RepositoryName> findRepositories( String criteria ) throws RemoteException {
        return connector().search(criteria);
    }

    @Override
    public String[] getWorkspaces( String repositoryName ) throws RemoteException {
        return connector().find(repositoryName).getWorkspaces();
    }

    @Override
    public void login( String userName,
                       String password ) throws RemoteException {
        connector().login(userName, password);
    }

    @Override
    public JcrNode node( String repository,
                         String workspace,
                         String path ) throws RemoteException {
        logger.debug("Requested node in repository '" + repository + "', workspace '" + workspace + "', path '" + path + "'");

        if (repository == null || workspace == null) {
            return null;
        }
        try {
            Session session = connector().find(repository).session(workspace);
            Node n = session.getNode(path);

            // convert into value object
            JcrNode node = new JcrNode(n.getName(), n.getPath(), n.getPrimaryNodeType().getName());
            node.setMixins(mixinTypes(n));
            node.setProperties(getProperties(n));

            node.setPropertyDefs(propertyDefs(n));
            node.setAcl(getAcl(repository, workspace, path));

            NodeIterator it = n.getNodes();
            while (it.hasNext()) {
                Node child = it.nextNode();
                node.addChild(new JcrNode(child.getName(), child.getPath(), child.getPrimaryNodeType().getName()));
            }
            return node;
        } catch (RepositoryException e) {
            e.printStackTrace();
            throw new RemoteException(e.getMessage());
        }
    }

    private String[] mixinTypes( Node node ) throws RepositoryException {
        NodeType[] values = node.getMixinNodeTypes();
        String[] res = new String[values.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = values[i].getName();
        }
        return res;
    }

    /**
     * Gets the list of properties available to the given node.
     * 
     * @param node the node instance.
     * @return list of property names.
     * @throws RepositoryException
     */
    private String[] propertyDefs( Node node ) throws RepositoryException {
        ArrayList<String> list = new ArrayList<String>();

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

    private Acl getAcl( String repository,
                        String workspace,
                        String path ) throws RepositoryException, RemoteException {
        Session session = connector().find(repository).session(workspace);

        AccessControlManager acm = session.getAccessControlManager();
        AccessControlList accessList = findAccessList(acm, path);

        if (accessList == null) {
            return null;
        }

        Acl acl = new Acl();

        AccessControlEntry[] entries = accessList.getAccessControlEntries();
        for (AccessControlEntry entry : entries) {
            Policy policy = new Policy();
            policy.setPrincipal(entry.getPrincipal().getName());
            Privilege[] privileges = entry.getPrivileges();
            for (Privilege privilege : privileges) {
                policy.add(JcrPermission.forName(privilege.getName()));
            }
            acl.addPolicy(policy);
        }
        return acl;
    }

    /**
     * Searches access list for the given node.
     * 
     * @param acm access manager
     * @param path the path to the node
     * @return access list representation.
     * @throws RepositoryException
     */
    private AccessControlList findAccessList( AccessControlManager acm,
                                              String path ) throws RepositoryException {
        AccessControlPolicy[] policy = acm.getPolicies(path);

        if (policy != null && policy.length > 0) {
            return (AccessControlList)policy[0];
        }

        policy = acm.getEffectivePolicies(path);
        if (policy != null && policy.length > 0) {
            return (AccessControlList)policy[0];
        }

        return null;
    }

    /**
     * Reads properties of the given node.
     * 
     * @param node the node instance
     * @return list of node's properties.
     * @throws RepositoryException
     */
    private Collection<JcrProperty> getProperties( Node node ) throws RepositoryException {
        ArrayList<JcrProperty> list = new ArrayList<JcrProperty>();
        PropertyIterator it = node.getProperties();
        while (it.hasNext()) {
            Property p = it.nextProperty();
            JcrProperty property = new JcrProperty(p.getName(), PropertyType.nameFromValue(p.getType()), values(p));
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
    private String values( Property p ) throws RepositoryException {
        if (!p.isMultiple()) {
            return p.getString();
        }

        Value[] values = p.getValues();

        if (values == null || values.length == 0) {
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
    public JcrRepositoryDescriptor repositoryInfo( String repository ) {
        JcrRepositoryDescriptor desc = new JcrRepositoryDescriptor();

        try {
            Repository repo = connector().find(repository).repository();
            String keys[] = repo.getDescriptorKeys();
            for (int i = 0; i < keys.length; i++) {
                Value value = repo.getDescriptorValue(keys[i]);
                desc.add(keys[i], value != null ? value.getString() : "N/A");
            }
        } catch (Exception e) {
            logger.debug("Error getting repository information", e);
        }
        return desc;
    }

    @Override
    public ResultSet query( String repository,
                            String workspace,
                            String text,
                            String lang ) throws RemoteException {
        ResultSet rs = new ResultSet();
        try {
            Session session = connector().find(repository).session(workspace);

            QueryManager qm = session.getWorkspace().getQueryManager();
            Query q = qm.createQuery(text, lang);

            QueryResult qr = q.execute();

            rs.setColumnNames(qr.getColumnNames());
            ArrayList<String[]> rows = new ArrayList<String[]>();
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
            logger.debug("Query result: " + rs.getRows().size());
            return rs;
        } catch (Exception e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public String[] supportedQueryLanguages( String repository,
                                             String workspace ) throws RemoteException {
        try {
            Session session = connector().find(repository).session(workspace);
            QueryManager qm = session.getWorkspace().getQueryManager();
            return qm.getSupportedQueryLanguages();
        } catch (RepositoryException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public void addNode( String repository,
                         String workspace,
                         String path,
                         String name,
                         String primaryType ) throws RemoteException {
        Session session = connector().find(repository).session(workspace);
        try {
            Node node = (Node)session.getItem(path);
            node.addNode(name, primaryType);
        } catch (RepositoryException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public void removeNode( String repository,
                            String workspace,
                            String path ) throws RemoteException {
        Session session = connector().find(repository).session(workspace);
        try {
            Node node = (Node)session.getItem(path);
            node.remove();
        } catch (PathNotFoundException e) {
            logger.debug(e.getLocalizedMessage());
        } catch (RepositoryException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public void addMixin( String repository,
                          String workspace,
                          String path,
                          String mixin ) throws RemoteException {
        Session session = connector().find(repository).session(workspace);
        try {
            Node node = (Node)session.getItem(path);
            node.addMixin(mixin);
        } catch (PathNotFoundException e) {
            logger.debug(e.getLocalizedMessage());
        } catch (RepositoryException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public void removeMixin( String repository,
                             String workspace,
                             String path,
                             String mixin ) throws RemoteException {
        Session session = connector().find(repository).session(workspace);
        try {
            Node node = (Node)session.getItem(path);
            node.removeMixin(mixin);
        } catch (PathNotFoundException e) {
            logger.debug(e.getLocalizedMessage());
        } catch (RepositoryException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public void setProperty( String repository,
                             String workspace,
                             String path,
                             String name,
                             String value ) throws RemoteException {
        Session session = connector().find(repository).session(workspace);
        try {
            Node node = (Node)session.getItem(path);
            switch (type(node, name)) {
                case PropertyType.BOOLEAN:
                    node.setProperty(name, Boolean.parseBoolean(value));
                    break;
                case PropertyType.DATE:
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(new SimpleDateFormat("").parse(value));
                    node.setProperty(name, cal);
                    break;
                case PropertyType.DECIMAL:
                    node.setProperty(name, BigDecimal.valueOf(Double.parseDouble(value)));
                    break;
                case PropertyType.DOUBLE:
                    node.setProperty(name, Double.parseDouble(value));
                    break;
                case PropertyType.LONG:
                    node.setProperty(name, Long.parseLong(value));
                    break;
                case PropertyType.NAME:
                    node.setProperty(name, value);
                    break;
                case PropertyType.PATH:
                    node.setProperty(name, value);
                    break;
                case PropertyType.STRING:
                    node.setProperty(name, value);
                    break;
                case PropertyType.URI:
                    node.setProperty(name, value);
                    break;

            }
        } catch (PathNotFoundException e) {
            logger.debug(e.getLocalizedMessage());
        } catch (Exception e) {
            logger.debug("Unexpected error", e);
            throw new RemoteException(e.getMessage());
        }
    }

    private int type( Node node,
                      String propertyName ) throws RepositoryException {
        PropertyDefinition[] defs = node.getPrimaryNodeType().getPropertyDefinitions();
        for (PropertyDefinition def : defs) {
            if (def.getName().equals(propertyName)) {
                return def.getRequiredType();
            }
        }

        NodeType[] mixins = node.getMixinNodeTypes();
        for (NodeType type : mixins) {
            defs = type.getPropertyDefinitions();
            for (PropertyDefinition def : defs) {
                if (def.getName().equals(propertyName)) {
                    return def.getRequiredType();
                }
            }
        }

        return -1;
    }

    @Override
    public void addAccessList( String repository,
                               String workspace,
                               String path,
                               String principal ) throws RemoteException {
        Session session = connector().find(repository).session(workspace);
        try {
            AccessControlManager acm = session.getAccessControlManager();
            Privilege allPermissions = acm.privilegeFromName(Privilege.JCR_ALL);

            AccessControlList acl = (AccessControlList)acm.getApplicablePolicies(path).nextAccessControlPolicy();
            acl.addAccessControlEntry(new SimplePrincipal(principal), new Privilege[] {allPermissions});
            acm.setPolicy(path, acl);
            // session.save();
        } catch (PathNotFoundException e) {
            logger.debug(e.getLocalizedMessage());
        } catch (RepositoryException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public String[] getPrimaryTypes( String repository,
                                     String workspace,
                                     boolean allowAbstract ) throws RemoteException {
        Session session = connector().find(repository).session(workspace);
        ArrayList<String> list = new ArrayList<String>();
        try {
            NodeTypeManager mgr = session.getWorkspace().getNodeTypeManager();
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
    public String[] getMixinTypes( String repository,
                                   String workspace,
                                   boolean allowAbstract ) throws RemoteException {
        Session session = connector().find(repository).session(workspace);
        ArrayList<String> list = new ArrayList<String>();
        try {
            NodeTypeManager mgr = session.getWorkspace().getNodeTypeManager();
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
    public void save( String repository,
                      String workspace ) throws RemoteException {
        Session session = connector().find(repository).session(workspace);
        try {
            session.save();
        } catch (Exception e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public void updateAccessList( String repository,
                                  String workspace,
                                  String path,
                                  JcrAccessControlList acl ) throws RemoteException {
        Session session = connector().find(repository).session(workspace);
        try {
            AccessControlManager acm = session.getAccessControlManager();
            AccessControlPolicy[] policies = acm.getPolicies(path);

            AccessControlList accessList = null;
            if (policies != null && policies.length > 0) {
                accessList = (AccessControlList)policies[0];
            } else {
                accessList = (AccessControlList)acm.getApplicablePolicies(path).nextAccessControlPolicy();
            }

            clean(accessList);
            update(acm, acl, accessList);

            acm.setPolicy(path, accessList);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public void updateAccessList( String repository,
                                  String workspace,
                                  String path,
                                  String principal,
                                  JcrPermission permission,
                                  boolean enabled ) throws RemoteException {
        Session session = connector().find(repository).session(workspace);
        try {
            AccessControlManager acm = session.getAccessControlManager();
            AccessControlList acl = (AccessControlList)acm.getPolicies(path)[0];

            AccessControlEntry entry = pick(acl, principal);
            acl.removeAccessControlEntry(entry);

            Privilege[] privs = enabled ? includePrivilege(acm, entry.getPrivileges(), permission) : excludePrivilege(entry.getPrivileges(),
                                                                                                                      permission);
            acl.addAccessControlEntry(entry.getPrincipal(), privs);
        } catch (Exception e) {
            throw new RemoteException(e.getMessage());
        }
    }

    /**
     * Picks access entry for the given principal.
     * 
     * @param acl
     * @param principal
     * @return the ACL entry
     * @throws RepositoryException
     */
    private AccessControlEntry pick( AccessControlList acl,
                                     String principal ) throws RepositoryException {
        for (AccessControlEntry entry : acl.getAccessControlEntries()) {
            if (entry.getPrincipal().getName().equals(principal)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Excludes given privilege.
     * 
     * @param privileges
     * @param permission
     * @return the privileges
     */
    private Privilege[] excludePrivilege( Privilege[] privileges,
                                          JcrPermission permission ) {
        ArrayList<Privilege> list = new ArrayList<Privilege>();

        for (Privilege privilege : privileges) {
            if (!privilege.getName().equalsIgnoreCase(permission.getName())) {
                list.add(privilege);
            }
        }

        Privilege[] res = new Privilege[list.size()];
        list.toArray(res);
        return res;
    }

    /**
     * Includes given privilege.
     * 
     * @param acm the access control manager
     * @param privileges
     * @param permission
     * @return the privileges
     * @throws RepositoryException
     */
    private Privilege[] includePrivilege( AccessControlManager acm,
                                          Privilege[] privileges,
                                          JcrPermission permission ) throws RepositoryException {
        ArrayList<Privilege> list = new ArrayList<Privilege>();

        for (Privilege privilege : privileges) {
            if (!privilege.getName().equalsIgnoreCase(permission.getName())) {
                list.add(privilege);
            }
        }

        list.add(acm.privilegeFromName(permission.getJcrName()));

        Privilege[] res = new Privilege[list.size()];
        list.toArray(res);
        return res;
    }

    private void update( AccessControlManager acm,
                         JcrAccessControlList a1,
                         AccessControlList a2 ) throws AccessControlException, RepositoryException {
        Collection<JcrPolicy> policies = a1.entries();
        for (JcrPolicy policy : policies) {
            a2.addAccessControlEntry(new SimplePrincipal(policy.getPrincipal()), privileges(acm, policy.getPermissions()));
        }
    }

    private Privilege[] privileges( AccessControlManager acm,
                                    Collection<JcrPermission> permissions ) throws AccessControlException, RepositoryException {
        Privilege[] privileges = new Privilege[permissions.size()];
        int i = 0;
        for (JcrPermission permission : permissions) {
            privileges[i++] = acm.privilegeFromName(permission.getName());
        }
        return privileges;
    }

    private void clean( AccessControlList acl ) throws RepositoryException {
        AccessControlEntry[] entries = acl.getAccessControlEntries();
        for (AccessControlEntry entry : entries) {
            acl.removeAccessControlEntry(entry);
        }
    }

    @Override
    public Collection<JcrNodeType> nodeTypes( String repository,
                                              String workspace ) throws RemoteException {
        Session session = connector().find(repository).session(workspace);
        ArrayList<JcrNodeType> list = new ArrayList<JcrNodeType>();
        try {
            NodeTypeManager mgr = session.getWorkspace().getNodeTypeManager();

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

    @Override
    public void renameNode( String repository,
                            String workspace,
                            String path,
                            String name ) throws RemoteException {
        Session session = connector().find(repository).session(workspace);
        try {
            Node node = session.getNode(path);
            session.move(path, node.getParent().getPath() + "/" + name);
        } catch (Exception e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public void backup( String repository,
                        String name ) throws RemoteException {
        connector().find(repository).backup(name);
    }

    @Override
    public void restore( String repository,
                         String name ) throws RemoteException {
        connector().find(repository).restore(name);
    }

    @Override
    public void export( String repository,
                        String workspace,
                        String path,
                        String location,
                        boolean skipBinary,
                        boolean noRecurse ) throws RemoteException {
        File file = new File(location);
        try {
            FileOutputStream fout = new FileOutputStream(file);
            connector().find(repository).session(workspace).exportSystemView(path, fout, skipBinary, noRecurse);
        } catch (Exception e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public void importXML( String repository,
                           String workspace,
                           String path,
                           String location,
                           int option ) throws RemoteException {
        File file = new File(location);
        try {
            FileInputStream fin = new FileInputStream(file);
            connector().find(repository).session(workspace).getWorkspace().importXML(path, fin, option);
        } catch (Exception e) {
            throw new RemoteException(e.getMessage());
        }
    }

    private class SimplePrincipal implements Principal {

        private String name;

        protected SimplePrincipal( String name ) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
