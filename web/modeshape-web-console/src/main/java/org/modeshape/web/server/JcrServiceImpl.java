package org.modeshape.web.server;

import org.modeshape.web.client.JcrService;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.jcr.Credentials;
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
import javax.jcr.nodetype.NodeTypeManager;
import javax.naming.InitialContext;
import org.jboss.logging.Logger;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.web.shared.JcrNode;
import org.modeshape.web.client.RemoteException;
import org.modeshape.web.shared.JcrProperty;
import org.modeshape.web.shared.JcrRepositoryDescriptor;

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
                children.add(no);
            }

        } catch (RepositoryException e) {
        }

        return children;
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
}
