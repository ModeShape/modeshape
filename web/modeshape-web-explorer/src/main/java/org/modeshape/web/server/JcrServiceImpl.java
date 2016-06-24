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
package org.modeshape.web.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
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
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;
import org.modeshape.common.logging.Logger;
import org.modeshape.web.client.JcrService;
import org.modeshape.web.shared.RemoteException;
import org.modeshape.web.shared.Acl;
import org.modeshape.web.shared.JcrNode;
import org.modeshape.web.shared.JcrNodeType;
import org.modeshape.web.shared.JcrPermission;
import org.modeshape.web.shared.JcrProperty;
import org.modeshape.web.shared.JcrRepositoryDescriptor;
import org.modeshape.web.shared.Policy;
import org.modeshape.web.shared.RepositoryName;
import org.modeshape.web.shared.ResultSet;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.RepositoryStatistics;
import org.modeshape.jcr.api.monitor.DurationMetric;
import org.modeshape.jcr.api.monitor.History;
import org.modeshape.jcr.api.monitor.Statistics;
import org.modeshape.jcr.api.monitor.ValueMetric;
import org.modeshape.jcr.api.monitor.Window;
import org.modeshape.web.server.impl.MsDurationMetric;
import org.modeshape.web.server.impl.MsValueMetric;
import org.modeshape.web.server.impl.TimeUnit;
import org.modeshape.web.shared.BackupParams;
import org.modeshape.web.shared.RestoreParams;
import org.modeshape.web.shared.Stats;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings( "serial" )
public class JcrServiceImpl extends RemoteServiceServlet implements JcrService {

    private final static String CONNECTOR_CLASS_PARAMETER = "connector-class";    
    private final static String DEFAULT_CONNECTOR_CLASS = 
            "org.modeshape.web.server.impl.ConnectorImpl";    
    
    private final static Logger logger = Logger.getLogger(JcrServiceImpl.class);
    
    
    private Connector connector() throws RemoteException {
        Connector connector = (Connector)getThreadLocalRequest().getSession(true).getAttribute("connector");
        if (isUnknown(connector)) {
            String clsName = getConnectorClassName();
            if (isUnknown(clsName)) {
                clsName = DEFAULT_CONNECTOR_CLASS;
            }
            connector = loadConnector(clsName, getServletContext());
            getThreadLocalRequest().getSession(true).setAttribute("connector", connector);
        }
        return connector;
    }

    private String getConnectorClassName() {
        return getServletContext().getInitParameter(CONNECTOR_CLASS_PARAMETER);
    }
    
    @Override
    public String getRequestedURI() {
        String uri = (String)getThreadLocalRequest().getSession().getAttribute("initial.uri");
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
        ServletContext context = getServletContext();
        
        //get user's credentials from servlet context
        String uname = (String)context.getAttribute("uname");
        String passwd = (String)context.getAttribute("password");
        
        //login to the repositories
        if (uname != null) {
            connector().login(uname, passwd);
        }
        
        //return user's name
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
    public String logout() {
        try {
            connector().logout();
        } catch (RemoteException e) {
            //nothing to do here in case of the exception
        } finally {
            //clean up session
            getThreadLocalRequest().getSession().invalidate();
            
            //clean up context
            getServletContext().removeAttribute("uname");
            getServletContext().removeAttribute("password");
            
            //redirect to initial page. it will trigger login form
            return getThreadLocalRequest().getContextPath() + "/Console.html";
        }
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
            JcrNode node = new JcrNode(repository, workspace, n.getName(), n.getPath(), n.getPrimaryNodeType().getName());
            node.setMixins(mixinTypes(n));
            node.setProperties(getProperties(repository, workspace, path, n));

            node.setPropertyDefs(propertyDefs(n));
            
            try {
                node.setAcl(getAcl(repository, workspace, path));
            } catch (AccessDeniedException e) {
                node.setAcl(null);
            }

            NodeIterator it = n.getNodes();
            node.setChildCount(it.getSize());
            return node;
        } catch (RepositoryException e) {
            logger.error(e,  JcrI18n.unexpectedException, e.getMessage());
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public Collection<JcrNode> childNodes(String repository, String workspace, String path, int index, int count) throws RemoteException {
        if (repository == null || workspace == null) {
            return null;
        }
        try {
            Session session = connector().find(repository).session(workspace);
            Node n = session.getNode(path);

            NodeIterator it = n.getNodes();
            
            ArrayList<JcrNode> res = new ArrayList();
            int i = 0;
            while (it.hasNext()) {
                Node child = it.nextNode();
                if (i >= index && i < (index + count)) {
                    res.add(new JcrNode(repository, workspace, child.getName(), child.getPath(), child.getPrimaryNodeType().getName()));
                }
                i++;
            }
            return res;
        } catch (RepositoryException e) {
            logger.error(e,  JcrI18n.unexpectedException, e.getMessage());
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
        ArrayList<String> list = new ArrayList<>();

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
                policy.enable(privilege.getName());
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
     * @param repository the repository name
     * @param workspace the workspace name
     * @param path the path to the node
     * @param node the node instance
     * @return list of node's properties.
     * @throws RepositoryException
     */
    private Collection<JcrProperty> getProperties( String repository, String workspace, String path, Node node ) throws RepositoryException {
        ArrayList<PropertyDefinition> names = new ArrayList<>();
        
        NodeType primaryType = node.getPrimaryNodeType();
        PropertyDefinition[] defs = primaryType.getPropertyDefinitions();

        names.addAll(Arrays.asList(defs));
        
        NodeType[] mixinType = node.getMixinNodeTypes();
        for (NodeType type : mixinType) {
            defs = type.getPropertyDefinitions();
            names.addAll(Arrays.asList(defs));
        }

        ArrayList<JcrProperty> list = new ArrayList<>();
        for (PropertyDefinition def :  names) {
            
            String name = def.getName();
            String type = PropertyType.nameFromValue(def.getRequiredType());
            
            Property p = null;
            try {
                p = node.getProperty(def.getName());
            } catch (PathNotFoundException e) {
            }

            String display = values(def, p);
            String value = def.isMultiple() ? multiValue(p)
                    : singleValue(p, def, repository, workspace, path);
            list.add(new JcrProperty(name, type, value, display));
            
        }
        return list;
    }    

    private String singleValue(Property p, PropertyDefinition def, 
            String repository, String workspace, String path) throws RepositoryException {
        switch (def.getRequiredType()) {
            case PropertyType.BINARY:
                HttpServletRequest request = this.getThreadLocalRequest();
                String context = request.getContextPath();
                return String.format("%s/binary/node?repository=%s&workspace=%s&path=%s&property=%s",
                        context, repository, workspace, path, def.getName());
            default:
                return p == null ? "N/A" : p.getValue() != null ? p.getValue().getString() : "N/A";
        }
    }
    
    private String multiValue(Property p) throws RepositoryException {
        if (p == null) {
            return "";
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
    
    /**
     * Displays property value as string
     * 
     * @param pd the property definition
     * @param p the property to display
     * @return property value as text string
     * @throws RepositoryException
     */
    private String values( PropertyDefinition pd, Property p ) throws RepositoryException {
        if (p == null) {
            return "N/A";
        }
        
        if (pd.getRequiredType() == PropertyType.BINARY) {
            return "BINARY";
        }
        
        if (!p.isMultiple()) {
            return p.getString();
        }

        return multiValue(p);
    }

    @Override
    public JcrRepositoryDescriptor repositoryInfo( String repository ) throws RemoteException {
        JcrRepositoryDescriptor desc = new JcrRepositoryDescriptor();

        try {
            Repository repo = connector().find(repository).repository();
            String keys[] = repo.getDescriptorKeys();
            for (int i = 0; i < keys.length; i++) {
                Value value = repo.getDescriptorValue(keys[i]);
                desc.add(keys[i], value != null ? value.getString() : "N/A");
            }
        } catch (RemoteException | IllegalStateException | RepositoryException e) {
            throw new RemoteException(e);
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
            ArrayList<String[]> rows = new ArrayList<>();
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
        } catch (RemoteException | RepositoryException | IllegalStateException e) {
            throw new RemoteException(e);
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
    public JcrNode addNode( String repository,
                         String workspace,
                         String path,
                         String name,
                         String primaryType ) throws RemoteException {
        Session session = connector().find(repository).session(workspace);
        try {
            Node parent = (Node)session.getItem(path);
            Node n = parent.addNode(name, primaryType);
            JcrNode node = new JcrNode(repository, workspace, n.getName(), n.getPath(), n.getPrimaryNodeType().getName());
            node.setMixins(mixinTypes(n));
            node.setProperties(getProperties(repository, workspace, path, n));
            node.setPropertyDefs(propertyDefs(n));
            return node;
        } catch (RepositoryException e) {
            logger.debug("Could not add node: " + e.getMessage());
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
    public void setProperty(JcrNode node, String name, String value) throws RemoteException {
        Session session = connector().find(node.getRepository()).session(node.getWorkspace());
        try {
            Node n = session.getNode(node.getPath());
            n.setProperty(name, value);
        } catch (Exception e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public void setProperty(JcrNode node, String name, Boolean value) throws RemoteException {
        Session session = connector().find(node.getRepository()).session(node.getWorkspace());
        try {
            Node n = session.getNode(node.getPath());
            n.setProperty(name, value);
        } catch (Exception e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public void setProperty(JcrNode node, String name, Date value) throws RemoteException {
        Session session = connector().find(node.getRepository()).session(node.getWorkspace());
        try {
            Node n = session.getNode(node.getPath());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(value);
            n.setProperty(name, calendar);
        } catch (Exception e) {
            throw new RemoteException(e.getMessage());
        }
    }
    
    public void setProperty( String repository,
                             String workspace,
                             String path,
                             String name,
                             Object value ) throws RemoteException {
        Session session = connector().find(repository).session(workspace);
        try {
            Node node = (Node)session.getItem(path);
            switch (type(node, name)) {
                case PropertyType.BOOLEAN:
                    node.setProperty(name, (Boolean)value);
                    break;
                case PropertyType.DATE:
                    node.setProperty(name, (Calendar)value);
                    break;
                case PropertyType.DECIMAL:
                    node.setProperty(name, BigDecimal.valueOf(Double.parseDouble((String)value)));
                    break;
                case PropertyType.DOUBLE:
                    node.setProperty(name, Double.parseDouble((String)value));
                    break;
                case PropertyType.LONG:
                    node.setProperty(name, Long.parseLong((String)value));
                    break;
                case PropertyType.NAME:
                    node.setProperty(name, (String)value);
                    break;
                case PropertyType.PATH:
                    node.setProperty(name, (String)value);
                    break;
                case PropertyType.STRING:
                    node.setProperty(name, (String)value);
                    break;
                case PropertyType.URI:
                    node.setProperty(name, (String)value);
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
    public void removeAccessList( String repository,
                               String workspace,
                               String path,
                               String principal ) throws RemoteException {
        Session session = connector().find(repository).session(workspace);
        try {
            AccessControlManager acm = session.getAccessControlManager();
            AccessControlList acl = this.findAccessList(acm, path);

            AccessControlEntry entry = pick(acl, principal);
            acl.removeAccessControlEntry(entry);
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
                                     String workspace, String superType,
                                     boolean allowAbstract ) throws RemoteException {
        Session session = connector().find(repository).session(workspace);
        ArrayList<String> list = new ArrayList<>();
        try {
            NodeTypeManager mgr = session.getWorkspace().getNodeTypeManager();
            NodeTypeIterator it = mgr.getPrimaryNodeTypes();
            while (it.hasNext()) {
                NodeType nodeType = it.nextNodeType();
                if (nodeType.isAbstract() && !allowAbstract) {
                    continue;
                }
                if (superType != null 
                        && !isInset(superType, nodeType.getDeclaredSupertypeNames())) {
                    continue;
                }
                list.add(nodeType.getName());
            }
            String[] res = new String[list.size()];
            list.toArray(res);
            return res;
        } catch (RepositoryException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    private boolean isInset(String name, String[] list) {
        for (int i = 0; i < list.length; i++) {
            if (name.equals(list[i])) return true;
        }
        return false;
    }
    
    @Override
    public String[] getMixinTypes( String repository,
                                   String workspace,
                                   boolean allowAbstract ) throws RemoteException {
        Session session = connector().find(repository).session(workspace);
        ArrayList<String> list = new ArrayList<>();
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
        } catch (AccessDeniedException ex) {
            throw new RemoteException("Access violation: " + ex.getMessage());
        } catch (ItemExistsException ex) {
            throw new RemoteException("Item exist: " + ex.getMessage());
        } catch (ReferentialIntegrityException ex) {
            throw new RemoteException("Referential integrity violation: " + ex.getMessage());
        } catch (ConstraintViolationException ex) {
            throw new RemoteException("Constraint violation: " + ex.getMessage());
        } catch (InvalidItemStateException ex) {
            throw new RemoteException("Invalid item state: " + ex.getMessage());
        } catch (VersionException ex) {
            throw new RemoteException("version error: " + ex.getMessage());
        } catch (LockException ex) {
            throw new RemoteException("Lock violation: " + ex.getMessage());
        } catch (NoSuchNodeTypeException ex) {
            throw new RemoteException("Node type problem: " + ex.getMessage());
        } catch (RepositoryException ex) {
            throw new RemoteException("Generic error: " + ex.getMessage());
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
            AccessControlList acl = this.findAccessList(acm, path);

            AccessControlEntry entry = pick(acl, principal);
            acl.removeAccessControlEntry(entry);

            Privilege[] privs = enabled ? includePrivilege(acm, entry.getPrivileges(), permission) : excludePrivilege(entry.getPrivileges(),
                                                                                                                      permission);
            acl.addAccessControlEntry(entry.getPrincipal(), privs);
            acm.setPolicy(path, acl);
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
        ArrayList<Privilege> list = new ArrayList<>();

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
        ArrayList<Privilege> list = new ArrayList<>();

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

    @Override
    public Collection<JcrNodeType> nodeTypes( String repository,
                                              String workspace ) throws RemoteException {
        Session session = connector().find(repository).session(workspace);
        ArrayList<JcrNodeType> list = new ArrayList<>();
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
                        String name,
                        BackupParams params) throws RemoteException {
        connector().find(repository).backup(name, params);
    }

    @Override
    public void restore( String repository,
                         String name,
                         RestoreParams params) throws RemoteException {
        connector().find(repository).restore(name, params);
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
        } catch (RemoteException | IOException | RepositoryException e) {
            throw new RemoteException(e);
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
        } catch (RemoteException | IOException | RepositoryException e) {
            throw new RemoteException();
        }
    }

    @Override
    public void refreshSession(String repository, String workspace, 
        boolean keepChanges) throws RemoteException {
        try {
            connector().find(repository).session(workspace).refresh(keepChanges);
        } catch (RemoteException | RepositoryException e) {
            throw new RemoteException(e);
        }
    }

    @Override
    public Collection<Stats> getValueStats(String repository, String param, String tu) throws RemoteException {
        ArrayList<Stats> stats = new ArrayList<>();

        try {
            Window w = TimeUnit.find(tu).window();
            ValueMetric m = MsValueMetric.find(param).metric();

            JcrRepository repo = (JcrRepository) connector().find(repository).repository();

            RepositoryStatistics repositoryStatistics = repo.getRepositoryStatistics();
            History history = repositoryStatistics.getHistory(m, w);

            Statistics[] s = history.getStats();
            for (int i = 0; i < s.length; i++) {
                double min = s[i] != null ? s[i].getMinimum() : 0;
                double max = s[i] != null ? s[i].getMaximum() : 0;
                double avg = s[i] != null ? s[i].getMean() : 0;
                stats.add(new Stats(min, max, avg));
            }
        } catch (Exception e) {
            throw new RemoteException(e);
        }
        return stats;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Stats> getDurationStats(String repository, String param, String tu) throws RemoteException {
        ArrayList<Stats> stats = new ArrayList();

        try {
            Window w = TimeUnit.find(tu).window();
            DurationMetric m = MsDurationMetric.find(param).metric();

            JcrRepository repo = (JcrRepository) connector().find(repository).repository();

            RepositoryStatistics repositoryStatistics = repo.getRepositoryStatistics();
            History history = repositoryStatistics.getHistory(m, w);

            Statistics[] s = history.getStats();
            for (int i = 0; i < s.length; i++) {
                double min = s[i] != null ? s[i].getMinimum() : 0;
                double max = s[i] != null ? s[i].getMaximum() : 0;
                double avg = s[i] != null ? s[i].getMean() : 0;
                stats.add(new Stats(min, max, avg));
            }
        } catch (Exception e) {
            throw new RemoteException(e);
        }
        return stats;
    }

    @Override
    public String[] getValueMetrics() throws RemoteException {
        return MsValueMetric.getNames();
    }

    @Override
    public String[] getDurationMetrics() throws RemoteException {
        return MsDurationMetric.getNames();
    }

    @Override
    public String[] getTimeUnits() throws RemoteException {
        return TimeUnit.names();
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
    
    private Connector loadConnector(String clsName, ServletContext context) throws RemoteException {
        try {
            Class<?> cls = getClass().getClassLoader().loadClass(clsName);
            Connector connector = (Connector) cls.newInstance();
            connector.start(context);
            return connector;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | RemoteException e) {
            throw new RemoteException(e);
        }
    }
    
    private boolean isUnknown(Object o) {
        return o == null;
    }
    
}
