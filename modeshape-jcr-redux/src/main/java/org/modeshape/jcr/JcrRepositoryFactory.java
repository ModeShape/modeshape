/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.infinispan.schematic.document.ParsingException;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.Logger;
import org.modeshape.jcr.api.Repositories;
import org.modeshape.jcr.api.RepositoryFactory;

/**
 * Service provider for the JCR2 {@code RepositoryFactory} interface. This class provides a single public method,
 * {@link #getRepository(Map)}, that allows for a runtime link to a ModeShape JCR repository.
 * <p>
 * The canonical way to get a reference to this class is to use the {@link ServiceLoader}:
 * 
 * <pre>
 * String configUrl = ... ; // URL that points to your configuration file
 * Map parameters = Collections.singletonMap(JcrRepositoryFactory.URL, configUrl);
 * Repository repository;
 * 
 * for (RepositoryFactory factory : ServiceLoader.load(RepositoryFactory.class)) {
 *     repository = factory.getRepository(parameters);
 *     if (repository != null) break;
 * }
 * </pre>
 * 
 * It is also possible to instantiate this class directly.
 * 
 * <pre>
 * RepositoryFactory repoFactory = new JcrRepositoryFactory();    
 * String url = ... ; // URL that points to your configuration file
 * Map params = Collections.singletonMap(JcrRepositoryFactory.URL, url);
 * 
 * Repository repository = repoFactory.getRepository(params);]]></programlisting>
 * </pre>
 * 
 * </p>
 * <p>
 * Several URL formats are supported:
 * <ul>
 * <li><strong>JNDI location of repository</strong> - The URL contains the location in JNDI of an existing
 * <code>javax.jcr.Repository</code> instance. For example, "<code>jndi:jcr/local/my_repository</code>" is a URL that identifies
 * the JCR repository located in JNDI at the name "jcr/local/my_repository". Note that the use of such URLs requires that the
 * repository already be registered in JNDI at that location.</li>
 * <li><strong>JNDI location of engine and repository name</strong> - The URL contains the location in JNDI of an existing
 * ModeShape {@link JcrEngine engine} instance and the name of the <code>javax.jcr.Repository</code> repository as a URL query
 * parameter. For example, "<code>jndi:jcr/local?repositoryName=my_repository</code>" identifies a ModeShape engine registered in
 * JNDI at "jcr/local", and looks in that engine for a JCR repository named "<code>my_repository</code>".</li>
 * <li><strong>Location of a repository configuration</strong> - The URL contains a location that is resolvable to a configuration
 * file for the repository. If the configuration file has not already been loaded by the factory, then the configuration file is
 * read and used to deploy a new repository; subsequent uses of the same URL will return the previously deployed repository
 * instance. Several URL schemes are supported, including <code>classpath:</code>, "<code>file:</code>", <code>http:</code> and
 * any other URL scheme that can be {@link URL#openConnection() resolved and opened}. For example, "
 * <code>file://path/to/myRepoConfig.json</code>" identifies the file on the file system at the absolute path "
 * <code>/path/to/myRepoConfig.json</code>"; "<code>classpath://path/to/myRepoConfig.json</code>" identifies the file at "
 * <code>/path/to/myRepoConfig.json</code>" on the classpath, and "<code>http://www.example.com/path/to/myRepoConfig.json</code>
 * " identifies the file "<code>myRepoConfig.json</code>" at the given URL.</li>
 * </ul>
 * </p>
 * 
 * @see #getRepository(Map)
 * @see RepositoryFactory#getRepository(Map)
 */
@ThreadSafe
public class JcrRepositoryFactory implements RepositoryFactory {

    private static final Logger LOG = Logger.getLogger(JcrRepositoryFactory.class);

    /**
     * The engine that hosts the deployed repository instances.
     */
    private static final JcrEngine ENGINE = new JcrEngine();

    /** The name of the key for the ModeShape JCR URL in the parameter map */
    public static final String URL = "org.modeshape.jcr.URL";

    /** The name of the URL parameter that specifies the repository name. */
    public static final String REPOSITORY_NAME_PARAM = "repositoryName";

    static {
        ENGINE.start();
    }

    /**
     * Shutdown this engine to stop all repositories created by calls to {@link #getRepository(Map)}, terminate any ongoing
     * background operations (such as sequencing), and reclaim any resources that were acquired by the repositories. This method
     * may be called multiple times, but only the first time has an effect.
     * <p>
     * Calling this static method is identical to calling the {@link #shutdown()} method on any JcrRepositoryFactory instance, and
     * is provided for convenience.
     * </p>
     * <p>
     * Invoking this method does not preclude creating new {@link Repository} instances with future calls to
     * {@link #getRepository(Map)}. Any caller using this method as part of an application shutdown process should take care to
     * cease invocations of {@link #getRepository(Map)} prior to invoking this method.
     * </p>
     * <p>
     * This method returns immediately, even before the repositories have been shut down. However, the caller can simply call the
     * {@link Future#get() get()} method on the returned {@link Future} to block until all repositories have shut down. Note that
     * the {@link Future#get(long, TimeUnit)} method can be called to block for a maximum amount of time.
     * </p>
     * 
     * @return a future that allows the caller to block until the engine is shutdown; any error during shutdown will be thrown
     *         when {@link Future#get() getting} the repository from the future, where the exception is wrapped in a
     *         {@link ExecutionException}. The value returned from the future will always be true if the engine shutdown (or was
     *         not running), or false if the engine is still running.
     */
    public static Future<Boolean> shutdownAll() {
        return ENGINE.shutdown();
    }

    /**
     * Returns a reference to the appropriate repository for the given parameter map, if one exists. Although the
     * {@code parameters} map can have any number of entries, this method only considers the entry with the key
     * JcrRepositoryFactory#URL.
     * <p>
     * The value of this key is treated as a URL with the format {@code PROTOCOL://PATH[?repositoryName=REPOSITORY_NAME]} where
     * PROTOCOL is "jndi" or "file", PATH is the JNDI name of the JcrEngine or the path to the configuration file, and
     * REPOSITORY_NAME is the name of the repository to return if there is more than one JCR repository in the given JcrEngine or
     * configuration file.
     * </p>
     * 
     * @param parameters a map of parameters to use to look up the repository; may be null
     * @return the repository specified by the value of the entry with key {@link #URL}, or null if any of the following are true:
     *         <ul>
     *         <li>the parameters map is empty; or,</li>
     *         <li>there is no parameter with the {@link #URL}; or,</li>
     *         <li>the value for the {@link #URL} key is null or cannot be parsed into a ModeShape JCR URL; or,</li>
     *         <li>the ModeShape JCR URL is parseable but does not point to a JcrEngine (in the JNDI tree) or a configuration file
     *         (in the classpath or file system); or,</li>
     *         <li>or there is an error starting up the JcrEngine with the given configuration information.</li>
     *         <ul>
     * @see RepositoryFactory#getRepository(Map)
     */
    @Override
    @SuppressWarnings( {"unchecked", "rawtypes"} )
    public Repository getRepository( Map parameters ) {
        LOG.debug("Trying to load ModeShape JCR Repository with parameters: " + parameters);
        if (parameters == null) return null;

        Object rawUrl = parameters.get(URL);
        if (rawUrl == null) {
            LOG.debug("No parameter found with key: " + URL);
            return null;
        }

        URL url = null;
        if (rawUrl instanceof URL) {
            url = (URL)rawUrl;
        } else {
            url = urlFor(rawUrl.toString(), null);
        }

        if (url == null) return null;
        return getRepository(url, parameters);
    }

    protected Repository getRepository( URL url,
                                        Map<String, String> parameters ) {

        // See if the URL refers to a Repository instance in JNDI, which is probably what would be required
        // when registering particular repository instances rather than the engine (e.g., via JndiRepositoryFactory).
        // This enables JCR-2.0-style lookups while using JCR-1.0-style of registering individual Repository instances in JNDI.
        if ("jndi".equals(url.getProtocol())) {
            Repository repository = getRepositoryFromJndi(url.getPath(), parameters);
            if (repository != null) return repository;
        } else {
            // Otherwise just use the URL ...
            return getRepositoryFromConfigFile(url);
        }

        LOG.debug("Could not load or find a ModeShape repository using the URL '{1}'", url);
        return null;
    }

    /**
     * Returns a {@link JcrEngine} for the configuration in the given file.
     * <p>
     * If a {@link JcrEngine} has already been loaded by this class for the given configuration file, that engine will be reused.
     * </p>
     * 
     * @param configUrl the URL to the file in the file system or relative to the classpath; may not be null
     * @return a {@code JcrEngine} that was initialized from the given configuration file or null if no engine could be
     *         initialized from that file without errors.
     */
    private JcrRepository getRepositoryFromConfigFile( URL configUrl ) {
        assert configUrl != null;

        try {
            if ("file".equals(configUrl.getProtocol())) {
                try {
                    // Strip any query parameters from the incoming file URLs by creating a new URL with the same protocol, host,
                    // and
                    // port,
                    // but using the URL path as returned by URL#getPath() instead of the URL path and query parameters as
                    // returned by
                    // URL#getFile().
                    //
                    // We need to strip for the file protocol only because an URL with a file protocol and query parameters has
                    // the
                    // query parameters appended to the file name (e.g., "file:/tmp/foo/bar?repositoryName=foo" turns into an
                    // attempt
                    // to access the "/tmp/foo/bar?repositoryName=foo" file). Other protocol handlers like http handle this
                    // better.
                    configUrl = new URL(configUrl.getProtocol(), configUrl.getHost(), configUrl.getPort(), configUrl.getPath());
                } catch (MalformedURLException mfe) {
                    // This shouldn't be possible, since we're creating a new URL from an existing, valid URL
                    throw new IllegalStateException(mfe);
                }
            }

            // Make sure the engine is started ...
            switch (ENGINE.getState()) {
                case NOT_RUNNING:
                    ENGINE.start();
                    break;
                case STOPPING:
                    // Wait until it's shutdown ...
                    try {
                        ENGINE.shutdown().get(10, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        // ignore and let if fail ...
                    }
                    break;
                case RUNNING:
                case STARTING:
                    // do nothing ...
            }

            // Look for an existing repository with the same URL ...
            String configKey = configUrl.toString();
            if (ENGINE.getRepositoryKeys().contains(configKey)) {
                try {
                    return ENGINE.getRepository(configKey);
                } catch (NoSuchRepositoryException e) {
                    // Must have been removed since we checked, so just continue on to redeploy it ...
                }
            }

            // Otherwise, we need to deploy and start a new repository ...

            // Now try reading the configuration ...
            RepositoryConfiguration config = null;
            if ("file".equals(configUrl.getProtocol())) {
                // Strip any query parameters from the incoming file URLs by creating a new URL with the same protocol, host, and
                // port,
                // but using the URL path as returned by URL#getPath() instead of the URL path and query parameters as returned by
                // URL#getFile().
                //
                // We need to strip for the file protocol only because an URL with a file protocol and query parameters has the
                // query parameters appended to the file name (e.g., "file:/tmp/foo/bar?repositoryName=foo" turns into an attempt
                // to access the "/tmp/foo/bar?repositoryName=foo" file). Other protocol handlers like http handle this better.
                try {
                    config = RepositoryConfiguration.read(configUrl);
                } catch (ParsingException e) {
                    // Try reading from the classpath ...
                    try {
                        String path = classpathResource(configUrl);
                        if (path.length() == 0) throw e;
                        config = RepositoryConfiguration.read(path);
                    } catch (Throwable t) {
                        // This didn't work, so throw the original exception
                        throw e;
                    }
                }
            } else if ("classpath".equals(configUrl.getProtocol())) {
                // Look for the configuration file on the classpath ...
                String path = classpathResource(configUrl);
                config = RepositoryConfiguration.read(path);
            } else {
                // Just try resolving the URL ...
                config = RepositoryConfiguration.read(configUrl);
            }

            // Now deploy the new repository, using the URL as the key ...
            JcrRepository repository = ENGINE.deploy(config, configKey);
            repository.start();
            return repository;
        } catch (RepositoryException err) {
            LOG.debug(err, "Unable to start repository for configuration file at '{0}': {1}", configUrl, err.getMessage());
            return null;
        } catch (IOException err) {
            LOG.debug(err, "Unable to start repository for configuration file at '{0}': {1}", configUrl, err.getMessage());
            return null;
        } catch (NamingException err) {
            LOG.debug(err, "Unable to start repository for configuration file at '{0}': {1}", configUrl, err.getMessage());
            return null;
        }
    }

    private String classpathResource( URL url ) {
        String path = url.getPath();
        while (path.startsWith("/") && path.length() > 1) {
            path = path.substring(1);
        }
        return path.length() != 0 ? path : null;
    }

    /**
     * Returns a hashtable with the same key/value mappings as the given map
     * 
     * @param map the set of key/value mappings to convert; may not be null
     * @return a hashtable with the same key/value mappings as the given map; may be empty, never null
     */
    private Hashtable<String, String> hashtable( Map<String, String> map ) {
        assert map != null;

        Hashtable<String, String> hash = new Hashtable<String, String>(map.size());

        for (Map.Entry<String, String> entry : map.entrySet()) {
            hash.put(entry.getKey(), entry.getValue());
        }

        return hash;
    }

    /**
     * Attempts to look up a {@link Repository} at the given JNDI name. All parameters in the parameters map are passed to the
     * {@link InitialContext} constructor in a {@link Hashtable}.
     * 
     * @param jndiName the JNDI name of the JCR repository; may not be null
     * @param parameters any additional parameters that should be passed to the {@code InitialContext}'s constructor; may be empty
     *        or null
     * @return the Repository object from JNDI, if one exists at the given name
     */
    private Repository getRepositoryFromJndi( String jndiName,
                                              Map<String, String> parameters ) {
        if (parameters == null) parameters = Collections.emptyMap();

        // There should be a parameter with the name ...
        String repoName = parameters.get(REPOSITORY_NAME_PARAM);
        if (repoName != null && repoName.trim().length() == 0) repoName = null;

        try {
            InitialContext ic = new InitialContext(hashtable(parameters));

            Object ob = ic.lookup(jndiName);
            if (ob instanceof JcrEngine) {
                JcrEngine engine = (JcrEngine)ob;
                switch (engine.getState()) {
                    case NOT_RUNNING:
                    case STOPPING:
                        LOG.error(JcrI18n.engineAtJndiLocationIsNotRunning, jndiName);
                        return null;
                    case RUNNING:
                    case STARTING:
                        break; // continue
                }
                // There should be a parameter with the name ...
                if (repoName == null) {
                    // No repository name was specified, so see if there's just one in the engine ...
                    if (engine.getRepositories().size() == 1) {
                        repoName = engine.getRepositories().keySet().iterator().next();
                    }
                }
                if (repoName != null) {
                    repoName = repoName.trim();
                    if (repoName.length() != 0) {
                        // Look for a repository with the supplied name ...
                        try {
                            JcrRepository repository = engine.getRepository(repoName);
                            switch (repository.getState()) {
                                case STARTING:
                                case RUNNING:
                                    return repository;
                                default:
                                    LOG.error(JcrI18n.repositoryIsNotRunningOrHasBeenShutDownInEngineAtJndiLocation,
                                              repoName,
                                              jndiName);
                                    return null;
                            }
                        } catch (NoSuchRepositoryException e) {
                            LOG.warn(JcrI18n.repositoryNotFoundInEngineAtJndiLocation, repoName, jndiName);
                            return null;
                        }
                    }
                }
                // At this point, warn about a missing repository name ...
                LOG.warn(JcrI18n.missingRepositoryNameInUrlContainingJndiLocationOfEngine, jndiName, REPOSITORY_NAME_PARAM);
            } else if (ob instanceof Repositories) {
                Repositories repos = (Repositories)ob;
                try {
                    return repos.getRepository(repoName);
                } catch (RepositoryException e) {
                    LOG.warn(JcrI18n.repositoryNotFoundInEngineAtJndiLocation, repoName, jndiName);
                }
            } else if (ob instanceof Repository) {
                // Just return the repository instance ...
                return (Repository)ob;
            }
            return null;
        } catch (NamingException ne) {
            return null;
        }
    }

    @Override
    public Future<Boolean> shutdown() {
        return ENGINE.shutdown();
    }

    @Override
    public boolean shutdown( long timeout,
                             TimeUnit unit ) throws InterruptedException {
        try {
            return ENGINE.shutdown().get(timeout, unit);
        } catch (ExecutionException e) {
            LOG.error(e, JcrI18n.errorShuttingDownJcrRepositoryFactory);
            return false;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Returns the repository with the given name from the {@link Repositories} referenced by {@code jcrUrl} if the engine and the
     * named repository exist, null otherwise.
     * <p>
     * If the {@code jcrUrl} parameter contains a valid, ModeShape-compatible URL for a {@link JcrEngine} that has not yet been
     * started, that {@code JcrEngine} will be created and {@link JcrEngine#start() started} as a side effect of this method.
     * </p>
     * <p>
     * If the {@code repositoryName} parameter is null, the repository name specified in the {@code jcrUrl} parameter will be used
     * instead. If no repository name is specified in the {@code jcrUrl} parameter and the {@code JcrEngine} has exactly one
     * repository, that repository will be used. Otherwise, this method will return {@code null}.
     * </p>
     * 
     * @param jcrUrl the ModeShape-compatible URL that specifies the {@link Repositories} to be used; may not be null
     * @param repositoryName the name of the repository to return; may be null
     * @return the {@link Repository} with the given name from the {@link Repositories} referenced by {@code jcrUrl} if the engine
     *         exists and can be started (or is already started), and the named repository exists or {@code null} is provided as
     *         the {@code repositoryName} and the given engine has exactly one repository or the {@code jcrUrl} specifies a
     *         repository. If any of these conditions do not hold, {@code null} is returned.
     * @throws RepositoryException if the named repository exists but cannot be accessed
     * @see JcrEngine#getRepository(String)
     */
    public Repository getRepository( String jcrUrl,
                                     String repositoryName ) throws RepositoryException {
        URL url = urlFor(jcrUrl, repositoryName);
        if (url == null) return null;
        return getRepository(url, null);
    }

    /**
     * Returns the repository names in the {@link JcrEngine} referenced by the {@code jcrUrl} parameter.
     * <p>
     * If the {@code jcrUrl} parameter contains a valid, ModeShape-compatible URL for a {@link JcrEngine} that has not yet been
     * started, that {@code JcrEngine} will be created and {@link JcrEngine#start() started} as a side effect of this method.
     * </p>
     * 
     * @param jcrUrl the ModeShape-compatible URL that specifies the {@link JcrEngine} to be used; may not be null
     * @return the set of repository names in the given engine referred to by the {@code jcrUrl} parameter if that engine exists
     *         and it can be started (or is already started), otherwise {@code null}
     */
    public Set<String> getRepositoryNames( String jcrUrl ) {
        Repositories repos = getRepositories(jcrUrl);
        return repos != null ? repos.getRepositoryNames() : null;
    }

    @Override
    public Repositories getRepositories( String jcrUrl ) {
        URL url = urlFor(jcrUrl, null);
        if (url == null) return null;

        if ("jndi".equals(url.getProtocol())) {
            String jndiName = url.getPath();
            try {
                InitialContext ic = new InitialContext();

                Object ob = ic.lookup(jndiName);
                if (ob instanceof JcrEngine) {
                    JcrEngine engine = (JcrEngine)ob;
                    switch (engine.getState()) {
                        case NOT_RUNNING:
                        case STOPPING:
                            LOG.error(JcrI18n.engineAtJndiLocationIsNotRunning, jndiName);
                            return null;
                        case RUNNING:
                        case STARTING:
                            break; // continue
                    }
                    return engine;
                } else if (ob instanceof Repositories) {
                    return (Repositories)ob;
                } else if (ob != null) {
                    // The object is not what we expected, but figure out which error to report ...
                    for (Class<?> intrfc : ob.getClass().getInterfaces()) {
                        if (intrfc.getName().equals(Repositories.class.getName())) {
                            // We found an instance of the Repositories class, but not the Repositories class on our
                            // classloader. Therefore, the application must have the ModeShape JCR API JAR on
                            // their classpath while we also have the JAR on a different classloader.
                            I18n msg = JcrI18n.potentialClasspathErrorAtJndiLocation;
                            throw new ClassCastException(msg.text(jndiName, ob.getClass().getName()));
                        }
                    }
                    String className = ob.getClass().getName();
                    I18n msg = JcrI18n.repositoriesNotFoundInEngineAtJndiLocation;
                    throw new ClassCastException(msg.text(jndiName, className, Repositories.class.getName()));
                }
            } catch (NamingException ne) {
                // eat
            }
        }
        return null;
    }

    /**
     * Convenience method to convert a {@code String} into an {@code URL}. This method never throws a
     * {@link MalformedURLException}, but may throw a {@link NullPointerException} if {@code jcrUrl} is null.
     * 
     * @param jcrUrl the string representation of an URL that should be converted into an URL; may not be null
     * @param repoName the optional name of the repository; may be null
     * @return the URL version of {@code jcrUrl} if {@code jcrUrl} is a valid URL, otherwise null
     */
    private URL urlFor( String jcrUrl,
                        String repoName ) {
        if (jcrUrl == null || jcrUrl.isEmpty()) {
            throw new IllegalStateException(JcrI18n.invalidJcrUrl.text(jcrUrl));
        }

        try {
            if (repoName != null) {
                repoName = repoName.trim();
                String queryParam = "?" + REPOSITORY_NAME_PARAM + "=";
                if (repoName.length() != 0 && !jcrUrl.contains(queryParam)) {
                    jcrUrl = jcrUrl + queryParam + repoName;
                }
            }
            return new URL(jcrUrl.toString());
        } catch (MalformedURLException mue) {
            LOG.debug("Could not parse URL: " + mue.getMessage());
            return null;
        }
    }
}
