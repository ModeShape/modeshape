package org.jboss.dna.web.jcr.rest.spi;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.servlet.ServletContext;
import org.jboss.dna.jcr.JcrConfiguration;
import org.jboss.dna.jcr.JcrEngine;
import org.xml.sax.SAXException;

public class DnaJcrRepositoryProvider implements RepositoryProvider {

    public static final String CONFIG_FILE = "org.jboss.dna.web.jcr.rest.CONFIG_FILE";

    private JcrEngine jcrEngine;

    public DnaJcrRepositoryProvider() {
    }

    public Set<String> getJcrRepositoryNames() {
        return new HashSet<String>(jcrEngine.getRepositoryNames());
    }

    public Repository getRepository( String repositoryName ) throws RepositoryException {
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
}
