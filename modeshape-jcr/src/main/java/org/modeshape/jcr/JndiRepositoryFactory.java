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
import org.modeshape.jcr.api.Repositories;
import org.modeshape.repository.ModeShapeEngine;
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
 * <p>
 * Note that if the "repositoryName" property is not specified or is empty, the factory will register the ModeShape engine at the
 * supplied location in JNDI. This approach is compatible with the traditional JNDI URLs used with the
 * {@link JcrRepositoryFactory JCR 2.0-style RepositoryFactory}.
 * </p>
 */
public class JndiRepositoryFactory implements ObjectFactory {

    private static final String CONFIG_FILE = "configFile";
    private static final String REPOSITORY_NAME = "repositoryName";
    private static final String TYPE = "type";

    private static JcrEngine engine;
    protected static final Logger log = Logger.getLogger(JndiRepositoryFactory.class);

    /**
     * {@link JcrConfiguration#loadFrom(java.io.InputStream) Initializes} and {@link JcrEngine#start() starts} the
     * {@code JcrEngine} managed by this factory.
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
    public Object getObjectInstance( Object obj,
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

        // Determine the repository name that we're supposed to use/register ...
        RefAddr repositoryName = ref.get(REPOSITORY_NAME);
        String repoName = null;
        if (repositoryName != null) {
            repoName = repositoryName.getContent().toString();
            if (repoName != null && repoName.trim().length() == 0) repoName = null;
        }

        // Determine the type that we're supposed to create/register ...
        RefAddr type = ref.get(TYPE);
        if (type != null) {
            String typeName = type.getContent().toString();
            if (typeName != null && typeName.trim().length() == 0) typeName = null;

            // See if the type value matches a classname we know how to deal with ...
            if (Repositories.class.getName().equals(typeName) || JcrEngine.class.getName().equals(typeName)
                || ModeShapeEngine.class.getName().equals(typeName)) {
                if (repositoryName != null) {
                    // Log a warning ...
                    log.warn(JcrI18n.repositoryNameProvidedWhenRegisteringEngineInJndi, name, repoName, typeName);
                }
                // We're supposed to register the engine ...
                return engine;
            }
            if (!Repository.class.getName().equals(typeName) && !JcrRepository.class.getName().equals(typeName)
                && !org.modeshape.jcr.api.Repository.class.getName().equals(typeName)) {
                // We only know how to reigster the repository (other than engine), so return null ...
                return null;
            }
        } else {
            // There's no type ...
            log.warn(JcrI18n.typeMissingWhenRegisteringEngineInJndi, name, Repositories.class.getName());
            // and base what we register purely upon whether there's a name ...
            if (repoName == null) {
                // This factory registers an engine or a repository, and they didn't specify a repository ...
                return engine;
            }

            // Otherwise there is a repository name, so continue ...
        }

        // We know we're supposed to register the Repository instance, so look for the name ...
        if (repoName == null) {
            if (repositoryName == null) {
                log.error(JcrI18n.repositoryNameNotProvidedWhenRegisteringRepositoryInJndi, name);
            } else {
                log.error(JcrI18n.emptyRepositoryNameProvidedWhenRegisteringRepositoryInJndi, name);
            }
        }

        Repository repository = engine.getRepository(repoName);
        if (repository == null) {
            // Build up a single string with the names of the available repositories ...
            StringBuilder repoNames = new StringBuilder();
            boolean first = true;
            for (String existingName : engine.getRepositoryNames()) {
                if (first) first = false;
                else repoNames.append(", ");
                repoNames.append('"').append(existingName).append('"');
            }
            log.error(JcrI18n.invalidRepositoryNameWhenRegisteringRepositoryInJndi, name, repoName, repoNames);
            return null;
        }
        return repository;
    }
}
