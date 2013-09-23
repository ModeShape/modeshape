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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
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
import javax.naming.InitialContext;
import org.jboss.logging.Logger;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.web.shared.JcrNode;
import org.modeshape.web.client.RemoteException;
import org.modeshape.web.shared.JcrACLEntry;
import org.modeshape.web.shared.JcrAccessControlList;
import org.modeshape.web.shared.JcrPermission;
import org.modeshape.web.shared.JcrProperty;
import org.modeshape.web.shared.JcrRepositoryDescriptor;
import org.modeshape.web.shared.ResultSet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class JcrServiceImpl extends RemoteServiceServlet implements JcrService {

    private final static Logger log = Logger.getLogger(JcrServiceImpl.class);

    @Override
    public boolean login(String jndiName, String userName, String password, String workspace) throws RemoteException {
        try {
            InitialContext context = new InitialContext();
            JcrRepository repository = (JcrRepository) context.lookup(jndiName);

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
    public List<JcrNode> childNodes(String path) {
        Session session = (Session) this.getThreadLocalRequest().getSession().getAttribute("session");
        if (session == null) {
            log.error("Session has expired");
        }

        ArrayList<JcrNode> children = new ArrayList();
        try {
            Node node = (Node) session.getItem(path);
            NodeIterator it = node.getNodes();

            while (it.hasNext()) {
                Node n = it.nextNode();
                JcrNode no = new JcrNode(
                        n.getName(),
                        n.getPath(),
                        n.getPrimaryNodeType().getName());
                no.setProperties(getProperties(n));
                try {
                    no.setAcessControlList(getAccessList(session.getAccessControlManager(), node));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                children.add(no);
            }

        } catch (RepositoryException e) {
        }

        return children;
    }

    private JcrAccessControlList getAccessList(AccessControlManager acm, Node node) throws RepositoryException {
        JcrAccessControlList acl = new JcrAccessControlList();
        
        AccessControlList accessList = findAccessList(acm, node);
        
        if (accessList != null) {
            AccessControlEntry[] entries = accessList.getAccessControlEntries();

            for (AccessControlEntry entry : entries) {
                JcrACLEntry en = new JcrACLEntry();
                en.setPrincipal(entry.getPrincipal().getName());
                Privilege[] privileges = entry.getPrivileges();
                for (Privilege p : privileges) {
                    en.add(new JcrPermission(p.getName()));
                }
            }
        } else {
            JcrACLEntry en = new JcrACLEntry();
            en.setPrincipal("EVERYONE");
            en.add(new JcrPermission(Privilege.JCR_ALL));
            acl.add(en);
        }
        
        return acl;
    }
    
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
    
    private Collection<JcrProperty> getProperties(Node node) throws RepositoryException {
        ArrayList<JcrProperty> list = new ArrayList();
        PropertyIterator it = node.getProperties();
        while (it.hasNext()) {
            Property p = it.nextProperty();
            list.add(new JcrProperty(p.getName(),
                    PropertyType.nameFromValue(p.getType()), p.getValue().getString()));
        }
        return list;
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
}
