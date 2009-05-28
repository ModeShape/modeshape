package org.jboss.dna.web.jcr.rest;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;



public class DnaJcrDeployer implements ServletContextListener {

    public static final String DEFAULT_JNDI_NAME = "java:comp/env/org/jboss/dna/Engine";

    public static final String SYSTEM_PROPERTY_JNDI_NAME = "org.jboss.dna.dnaEngineJndiName";

    public static final String INIT_PARAMETER_JNDI_NAME = "org.jboss.dna.dnaEngineJndiName";

    public void contextDestroyed( ServletContextEvent event ) {
        RepositoryFactory.shutdown();
    }

    /**
     * Mounts a DNA engine
     */
    public void contextInitialized( ServletContextEvent event ) {
        RepositoryFactory.initialize(event.getServletContext());
    }
}
