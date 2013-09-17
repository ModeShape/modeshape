package org.modeshape.web.server;

import org.modeshape.web.client.JcrService;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
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
                no.setAcessControlList(getAccessList(session.getAccessControlManager(), node));
                children.add(no);
            }

        } catch (RepositoryException e) {
        }

        return children;
    }

    private JcrAccessControlList getAccessList(AccessControlManager acm, Node node) throws RepositoryException {
        JcrAccessControlList acl = new JcrAccessControlList();
        
        AccessControlList accessList = (AccessControlList) acm.getPolicies(node.getPath())[0];
        AccessControlEntry[] entries = accessList.getAccessControlEntries();
        
        for (AccessControlEntry entry : entries) {
            JcrACLEntry en = new JcrACLEntry();
            en.setPrincipal(entry.getPrincipal().getName());
            Privilege[] privileges = entry.getPrivileges();
            for (Privilege p : privileges) {
                en.add(new JcrPermission(p.getName()));
            }
        }
        return acl;
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
}
