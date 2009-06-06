package org.jboss.dna.web.jcr.rest.spi;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.jboss.dna.jcr.JcrConfiguration;
import org.jboss.dna.jcr.JcrEngine;
import org.jboss.dna.jcr.SecurityContextCredentials;
import org.jboss.dna.web.jcr.rest.ServletSecurityContext;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.UnauthorizedException;
import org.xml.sax.SAXException;

public class DnaJcrRepositoryProvider implements RepositoryProvider {

    public static final String CONFIG_FILE = "org.jboss.dna.web.jcr.rest.CONFIG_FILE";

    private JcrEngine jcrEngine;

    public DnaJcrRepositoryProvider() {
    }

    public Set<String> getJcrRepositoryNames() {
        return new HashSet<String>(jcrEngine.getRepositoryNames());
    }

    private Repository getRepository( String repositoryName ) throws RepositoryException {
        return jcrEngine.getRepository(repositoryName);
    }

    public void startup( ServletContext context ) {
        String configFile = context.getInitParameter(CONFIG_FILE);
        
        try {
            InputStream configFileInputStream = getClass().getResourceAsStream(configFile);
            jcrEngine = new JcrConfiguration().loadFrom(configFileInputStream).build();
            jcrEngine.start();
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        } catch (SAXException saxe) {
            throw new IllegalStateException(saxe);
        }

    }

    public void shutdown() {
        jcrEngine.shutdown();
    }

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
                                String workspaceName ) throws RepositoryException {
        assert request != null;
        assert request.getUserPrincipal() != null: "Request must be authorized";

        // Sanity check in case assertions are disabled
        if (request.getUserPrincipal() == null) {
            throw new UnauthorizedException("Client is not authorized");
        }
        
        Repository repository;
        
        try {
            repository = getRepository(repositoryName);
            
        } catch (RepositoryException re) {
            throw new NotFoundException(re.getMessage(), re);
        }
        
        return repository.login(new SecurityContextCredentials(new ServletSecurityContext(request)), workspaceName);

    }
}
