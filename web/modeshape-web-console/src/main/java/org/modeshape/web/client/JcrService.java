package org.modeshape.web.client;

import org.modeshape.web.shared.JcrNode;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import java.util.List;
import org.modeshape.web.shared.JcrRepositoryDescriptor;
import org.modeshape.web.shared.ResultSet;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("jcr")
public interface JcrService extends RemoteService {
    /**
     * Provides access for the given user to the given repository.
     * 
     * @param jndiName the jndi name of the repository.
     * @param userName the name of the user
     * @param password the user's password
     * @param workspace workspace to access
     * @return true in case of successful log in.
     */
    public boolean login(String jndiName, String userName, String password, 
            String workspace) throws RemoteException;
    /**
     * Gets children for the node under specified path.
     * 
     * @param path the path to the node
     * @return children nodes.
     */
    public List<JcrNode> childNodes(String path);

    /**
     * Gets repository capabilities.
     * 
     * @return description of the repository capabilities.
     */
    public JcrRepositoryDescriptor repositoryInfo();
    
    /**
     * Gets supported query languages.
     * 
     * @return language names
     */
    public String[] supportedQueryLanguages();
    
    /**
     * Executes query.
     * 
     * @param text the query text.
     * @param lang query language name
     * @return Query result
     */
    public ResultSet query(String text, String lang);
    
}
