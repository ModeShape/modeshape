package org.modeshape.jcr;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.event.EventContext;
import javax.naming.event.NamespaceChangeListener;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.spi.ObjectFactory;
import org.modeshape.common.collection.Problem;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.util.Logger;
import org.xml.sax.SAXException;

/**
 * The {@code JndiRepositoryFactory} class provides a means of initializing and accessing {@link Repository repositories} in a
 * JNDI tree. <h2>Example JNDI Configurations</h2> <h3>Tomcat 5.5</h3>
 * <p>
 * The following configuration can be added to added to server.xml in Tomcat 5.5 to initialize a {@code JcrRepository} and add it
 * to the JNDI tree.
 * 
 * <pre>
 *   &lt;GlobalNamingResources&gt;
 *         &lt;!-- Other configuration omitted --&gt;
 *      &lt;Resource name=&quot;jcr/local&quot; auth=&quot;Container&quot;
 *           type=&quot;javax.jcr.Repository&quot;
 *           factory=&quot;org.modeshape.jcr.JndiRepositoryFactory&quot;
 *           configFile=&quot;/tck/default/configRepository.xml&quot;
 *           repositoryName=&quot;Test Repository Source&quot;
 *           /&gt;
 *   &lt;/GlobalNamingResources&gt;
 * </pre>
 * 
 * This will create a repository loaded from the or file &quot;/tck/default/configRepository.xml&quot; and return the JCR
 * repository named &quot;Test Repository Source&quot;. The name of the repository is important as a single configuration file may
 * contain configuration information for many JCR repositories.
 * </p>
 */
public class JndiRepositoryFactory implements ObjectFactory {

    private static final String CONFIG_FILE = "configFile";
    private static final String REPOSITORY_NAME = "repositoryName";

    private static JcrEngine engine;
    protected static final Logger log = Logger.getLogger(JndiRepositoryFactory.class);

    /**
     * {@link JcrConfiguration#loadFrom(java.io.InputStream) Initializes} and {@link JcrEngine#start() starts} the {@code
     * JcrEngine} managed by this factory.
     * 
     * @param configFileName the name of the file containing the configuration information for the {@code JcrEngine}; may not be
     *        null. This method will first attempt to load this file as a resource from the classpath. If no resource with the
     *        given name exists, the name will be treated as a file name and loaded from the file system.
     * @throws IOException if there is an error or problem reading the configuration resource at the supplied path
     * @throws SAXException if the contents of the configuration resource are not valid XML
     * @throws RepositoryException if the {@link JcrEngine#start() JcrEngine could not be started}
     * @see JcrConfiguration#loadFrom(java.io.InputStream)
     * @see Class#getResourceAsStream(String)
     */
    private static synchronized void initializeEngine( String configFileName )
        throws IOException, SAXException, RepositoryException {
        if (engine != null) return;

        log.info(JcrI18n.engineStarting);
        long start = System.currentTimeMillis();

        JcrConfiguration config = new JcrConfiguration();
        InputStream configStream = JndiRepositoryFactory.class.getResourceAsStream(configFileName);

        if (configStream == null) {
            try {
                configStream = new FileInputStream(configFileName);
            } catch (IOException ioe) {
                throw new RepositoryException(ioe);
            }
        }

        engine = config.loadFrom(configStream).build();
        engine.start();

        Problems problems = engine.getProblems();
        for (Problem problem : problems) {
            switch (problem.getStatus()) {
                case ERROR:
                    log.error(problem.getThrowable(), problem.getMessage(), problem.getParameters());
                    break;
                case WARNING:
                    log.warn(problem.getThrowable(), problem.getMessage(), problem.getParameters());
                    break;
                case INFO:
                    log.info(problem.getThrowable(), problem.getMessage(), problem.getParameters());
                    break;
            }
        }

        if (problems.hasErrors()) {
            throw new RepositoryException(JcrI18n.couldNotStartEngine.text());
        }
        log.info(JcrI18n.engineStarted, (System.currentTimeMillis() - start));
    }

    /**
     * Creates an {@code JcrRepository} using the reference information specified.
     * <p>
     * This method first attempts to convert the {@code obj} parameter into a {@link Reference reference to JNDI configuration
     * information}. If that is successful, a {@link JcrEngine} will be created (if not previously created by a call to this
     * method) and it will be configured from the resource or file at the location specified by the {@code configFile} key in the
     * reference. After the configuration is successful, the {@link JcrEngine#getRepository(String) JcrEngine will be queried} for
     * the repository with the name specified by the value of the @{code repositoryName} key in the reference.
     * </p>
     * 
     * @param obj the reference to the JNDI configuration information; must be a non-null instance of {@link Reference}
     * @param name ignored
     * @param nameCtx ignored
     * @param environment ignored
     * @return the repository; never null
     * @throws IOException if there is an error or problem reading the configuration resource at the supplied path
     * @throws SAXException if the contents of the configuration resource are not valid XML
     * @throws NamingException if there is an error registering the namespace listener
     * @throws RepositoryException if the {@link JcrEngine#start() JcrEngine could not be started}, the named repository does not
     *         exist in the given configuration resource, or the named repository could not be created
     */
    public JcrRepository getObjectInstance( Object obj,
                                            Name name,
                                            Context nameCtx,
                                            Hashtable<?, ?> environment )
        throws IOException, SAXException, RepositoryException, NamingException {
        if (!(obj instanceof Reference)) return null;

        Reference ref = (Reference)obj;

        if (engine == null) {
            RefAddr configFile = ref.get(CONFIG_FILE);
            assert configFile != null;

            initializeEngine(configFile.getContent().toString());

            if (nameCtx instanceof EventContext) {
                EventContext evtCtx = (EventContext)nameCtx;

                NamespaceChangeListener listener = new NamespaceChangeListener() {

                    public void namingExceptionThrown( NamingExceptionEvent evt ) {
                        evt.getException().printStackTrace();
                    }

                    public void objectAdded( NamingEvent evt ) {
                    }

                    public void objectRemoved( NamingEvent evt ) {
                        Object oldObject = evt.getOldBinding().getObject();
                        if (!(oldObject instanceof JcrEngine)) return;

                        JcrEngine engine = (JcrEngine)oldObject;

                        log.info(JcrI18n.engineStopping);
                        long start = System.currentTimeMillis();
                        engine.shutdown();
                        try {
                            engine.awaitTermination(30, TimeUnit.SECONDS);
                            log.info(JcrI18n.engineStopped, (System.currentTimeMillis() - start));
                        } catch (InterruptedException ie) {
                            // Thread.interrupted();
                        }
                    }

                    public void objectRenamed( NamingEvent evt ) {
                    }

                };

                evtCtx.addNamingListener(name, EventContext.OBJECT_SCOPE, listener);
            }
        }

        assert engine != null;

        RefAddr repositoryName = ref.get(REPOSITORY_NAME);
        assert repositoryName != null;

        return engine.getRepository(repositoryName.getContent().toString());
    }

}
