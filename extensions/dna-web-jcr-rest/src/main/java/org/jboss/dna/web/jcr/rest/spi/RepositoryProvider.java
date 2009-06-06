package org.jboss.dna.web.jcr.rest.spi;

import java.util.Set;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 * Interface for any class that provides access to one or more local JCR repositories. Repository providers must provide a public,
 * no-argument constructor.
 */
public interface RepositoryProvider {

    /**
     * Returns an active session for the given workspace name in the named repository.
     * 
     * @param request the servlet request; may not be null or unauthenticated
     * @param repositoryName the name of the repository in which the session is created
     * @param workspaceName the name of the workspace to which the session should be connected
     * @return an active session with the given workspace in the named repository
     * @throws RepositoryException if any other error occurs
     */
    public Session getSession( HttpServletRequest request, 
                                String repositoryName,
                                String workspaceName ) throws RepositoryException;
    
    /**
     * Returns the available repository names
     * 
     * @return the available repository names; may not be null
     */
    Set<String> getJcrRepositoryNames();

    /**
     * Signals the repository provider that it should initialize itself based on the provided {@link ServletContext servlet context}
     * and begin accepting connections.
     * 
     * @param context the servlet context for the REST servlet
     */
    void startup(ServletContext context);
    /**
     * Signals the repository provider that it should complete any pending transactions, shutdown, and release
     * any external resource held.
     */
    void shutdown();

}
