package org.modeshape.jcr;

import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
import org.modeshape.common.logging.Logger;
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
 *           configFile=&quot;/path/to/repository-config.json&quot;
 *           repositoryName=&quot;Test Repository Source&quot;
 *           /&gt;
 *   &lt;/GlobalNamingResources&gt;
 * </pre>
 * 
 * This will create a repository loaded from the or file &quot;/path/to/configRepository.xml&quot; and return the JCR repository
 * named &quot;Test Repository Source&quot;. The name of the repository will be used to more quickly look up the repository if it
 * has been previously loaded.
 * </p>
 * <p>
 * Note that if the "repositoryName" property is not specified or is empty, the factory will register the ModeShape engine at the
 * supplied location in JNDI. This approach is compatible with the traditional JNDI URLs used with the
 * {@link JcrRepositoryFactory JCR 2.0-style RepositoryFactory}.
 * </p>
 */
public class JndiRepositoryFactory implements ObjectFactory {

    private static final String CONFIG_FILE = "configFile";
    private static final String CONFIG_FILES = "configFiles";
    private static final String REPOSITORY_NAME = "repositoryName";

    private static final JcrEngine engine = new JcrEngine();
    protected static final Logger log = Logger.getLogger(JndiRepositoryFactory.class);

    /**
     * Method that shuts down the JDNI repository factory's engine, usually for testing purposes.
     * 
     * @return a future that allows the caller to block until the engine is shutdown; any error during shutdown will be thrown
     *         when {@link Future#get() getting} the result from the future, where the exception is wrapped in a
     *         {@link ExecutionException}. The value returned from the future will always be true if the engine shutdown (or was
     *         not running), or false if the engine is still running.
     */
    static Future<Boolean> shutdown() {
        return engine.shutdown();
    }

    /**
     * Get or initialize the JCR Repository instance as described by the supplied configuration file and repository name.
     * 
     * @param configFileName the name of the file containing the configuration information for the {@code JcrEngine}; may not be
     *        null. This method will first attempt to load this file as a resource from the classpath. If no resource with the
     *        given name exists, the name will be treated as a file name and loaded from the file system. May be null if the
     *        repository should already exist.
     * @param repositoryName the name of the repository; may be null if the repository name is to be read from the configuration
     *        file (note that this does require parsing the configuration file)
     * @param nameCtx the naming context used to register a removal listener to shut down the repository when removed from JNDI;
     *        may be null
     * @param jndiName the name in JNDI where the repository is to be found
     * @return the JCR repository instance
     * @throws IOException if there is an error or problem reading the configuration resource at the supplied path
     * @throws RepositoryException if the repository could not be started
     * @throws NamingException if there is an error registering the namespace listener
     */
    private static synchronized JcrRepository getRepository( String configFileName,
                                                             String repositoryName,
                                                             final Context nameCtx,
                                                             final Name jndiName )
        throws IOException, RepositoryException, NamingException {

        if (repositoryName != null) {
            // Make sure the engine is running ...
            engine.start();

            // See if we can shortcut the process by using the name ...
            try {
                JcrRepository repository = engine.getRepository(repositoryName);
                switch (repository.getState()) {
                    case STARTING:
                    case RUNNING:
                        return repository;
                    default:
                        log.error(JcrI18n.repositoryIsNotRunningOrHasBeenShutDown, repositoryName);
                        return null;
                }
            } catch (NoSuchRepositoryException e) {
                if (configFileName == null) {
                    // No configuration file given, so we can't do anything ...
                    throw e;
                }
                // Nothing found, so continue ...
            }
        }

        RepositoryConfiguration config = RepositoryConfiguration.read(configFileName);
        if (repositoryName == null) repositoryName = config.getName();
        else if (!repositoryName.equals(config.getName())) {
            // The repository
            log.error(JcrI18n.repositoryNameDoesNotMatchConfigurationName, repositoryName, config.getName(), configFileName);
        }

        // Try to deploy and start the repository ...
        JcrRepository repository = engine.deploy(config);
        try {
            engine.startRepository(repository.getName()).get();
        } catch (InterruptedException e) {
            Thread.interrupted();
            throw new RepositoryException(e);
        } catch (ExecutionException e) {
            throw new RepositoryException(e.getCause());
        }

        // Register the JNDI listener, to shut down the repository when removed from JNDI ...
        if (nameCtx instanceof EventContext) {
            EventContext evtCtx = (EventContext)nameCtx;

            NamespaceChangeListener listener = new NamespaceChangeListener() {

                @Override
                public void namingExceptionThrown( NamingExceptionEvent evt ) {
                    evt.getException().printStackTrace();
                }

                @Override
                public void objectAdded( NamingEvent evt ) {
                    // do nothing ...
                }

                @SuppressWarnings( "synthetic-access" )
                @Override
                public void objectRemoved( NamingEvent evt ) {
                    Object oldObject = evt.getOldBinding().getObject();
                    if (!(oldObject instanceof JcrRepository)) return;

                    JcrRepository repository = (JcrRepository)oldObject;
                    String repoName = repository.getName();
                    try {
                        engine.shutdownRepository(repoName).get();
                    } catch (NoSuchRepositoryException e) {
                        // Ignore this ...
                    } catch (InterruptedException ie) {
                        log.error(ie, JcrI18n.errorWhileShuttingDownRepositoryInJndi, repoName, jndiName);
                        // Thread.interrupted();
                    } catch (ExecutionException e) {
                        log.error(e.getCause(), JcrI18n.errorWhileShuttingDownRepositoryInJndi, repoName, jndiName);
                    } finally {
                        // Try to shutdown the repository only if there are no more running repositories.
                        // IOW, shutdown but do not force shutdown of running repositories ...
                        engine.shutdown(false); // no need to block on the futured returned by 'shutdown(boolean)'
                    }
                }

                @Override
                public void objectRenamed( NamingEvent evt ) {
                    // do nothing ...
                }

            };

            evtCtx.addNamingListener(jndiName, EventContext.OBJECT_SCOPE, listener);
        }

        return repository;
    }

    /**
     * Creates an {@code JcrRepository} or ModeShape engine using the reference information specified.
     * <p>
     * This method first attempts to convert the {@code obj} parameter into a {@link Reference reference to JNDI configuration
     * information}. If that is successful, a {@link JcrEngine} will be created (if not previously created by a call to this
     * method) and it will be configured from the resource or file at the location specified by the {@code configFile} key in the
     * reference. After the configuration is successful, the {@link JcrEngine#getRepository(String) JcrEngine will be queried} for
     * the repository with the name specified by the value of the @{code repositoryName} key in the reference.
     * </p>
     * 
     * @param obj the reference to the JNDI configuration information; must be a non-null instance of {@link Reference}
     * @param name the name of the object
     * @param nameCtx used to register the ModeShape engine when no repository name is provided in the Reference obj
     * @param environment ignored
     * @return the object registered in JDNI, which will be the named repository name or (if no repository name is given) the
     *         ModeShape engine; never null
     * @throws IOException if there is an error or problem reading the configuration resource at the supplied path
     * @throws SAXException if the contents of the configuration resource are not valid XML
     * @throws NamingException if there is an error registering the namespace listener
     * @throws RepositoryException if the {@link JcrEngine#start() JcrEngine could not be started}, the named repository does not
     *         exist in the given configuration resource, or the named repository could not be created
     */
    @Override
    public Object getObjectInstance( Object obj,
                                     final Name name,
                                     final Context nameCtx,
                                     Hashtable<?, ?> environment )
        throws IOException, SAXException, RepositoryException, NamingException {
        if (!(obj instanceof Reference)) return null;
        Reference ref = (Reference)obj;

        // Get the name of the repository
        RefAddr repositoryName = ref.get(REPOSITORY_NAME);
        String repoName = repositoryName != null ? repositoryName.getContent().toString() : null;

        // Get the configuration file
        RefAddr configFileRef = ref.get(CONFIG_FILE);
        String configFile = configFileRef != null ? configFileRef.getContent().toString() : null;

        RefAddr configFilesRef = ref.get(CONFIG_FILES);
        Set<String> configFiles = configFilesRef != null ? parseStrings(configFilesRef.getContent().toString()) : null;

        engine.start();
        if (repoName != null && configFile != null) {
            // Start the named repository ...
            return getRepository(configFile, repoName, nameCtx, name);
        }
        if (configFiles != null) {
            // Start the configured repositories ...
            for (String file : configFiles) {
                getRepository(file, null, nameCtx, name);
            }
            return engine;
        }
        return null;
    }

    protected Set<String> parseStrings( String value ) {
        if (value == null) return null;
        value = value.trim();
        if (value == null) return null;
        Set<String> result = new HashSet<String>();
        for (String strValue : value.split(",")) {
            if (strValue == null) continue;
            strValue = strValue.trim();
            if (strValue == null) continue;
            result.add(strValue);
        }
        return result;
    }
}
