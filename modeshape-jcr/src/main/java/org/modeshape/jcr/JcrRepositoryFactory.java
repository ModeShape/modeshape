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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.collection.Problem;
import org.modeshape.common.util.Logger;
import org.modeshape.jcr.api.Repositories;
import org.modeshape.jcr.api.RepositoryFactory;
import org.xml.sax.SAXException;

/**
 * Service provider for the JCR2 {@code RepositoryFactory} interface. This class provides a single public method,
 * {@link #getRepository(Map)}, that allows for a runtime link to a ModeShape JCR repository.
 * <p>
 * The canonical way to get a reference to this class is to use the ServiceLocator:
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
 * String configUrl = ... ; // URL that points to your configuration file
 * Map params = Collections.singletonMap(JcrRepositoryFactory.URL, configUrl);
 * 
 * Repository repository = repoFactory.getRepository(params);]]></programlisting>
 * </pre>
 * 
 * </p>
 * <p>
 * The <code>configUrl</code> used in the sample above should point to a configuration file (e.g., {@code
 * file:src/test/resources/configRepository.xml?repositoryName=MyRepository}) OR a {@link Repositories} instance stored in the
 * JNDI tree (e.g., {@code jndi://name/of/Repositories/resource?repositoryName=MyRepository}).
 * </p>
 * 
 * @see #getRepository(Map)
 * @see RepositoryFactory#getRepository(Map)
 */
@ThreadSafe
public class JcrRepositoryFactory implements RepositoryFactory {

    private static final Logger LOG = Logger.getLogger(JcrRepositoryFactory.class);

    /**
     * A map of configuration file locations to existing engines. This map helps ensure that Repositories are not recreated with
     * every call to {@link #getRepository(Map)}.
     */
    private static final Map<String, JcrEngine> ENGINES = new HashMap<String, JcrEngine>();

    /** The name of the key for the ModeShape JCR URL in the parameter map */
    public static final String URL = "org.modeshape.jcr.URL";

    /** The name of the URL parameter that specifies the repository name. */
    public static final String REPOSITORY_NAME_PARAM = "repositoryName";

    /**
     * Returns a reference to the appropriate repository for the given parameter map, if one exists. Although the {@code
     * parameters} map can have any number of entries, this method only considers the entry with the key JcrRepositoryFactory#URL.
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
    @SuppressWarnings( "unchecked" )
    @Override
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
            url = urlFor(rawUrl.toString());
        }

        if (url == null) return null;

        Repositories repositories = repositoriesFor(url, parameters);
        if (repositories == null) return null;

        String repositoryName = repositoryNameFor(repositories, url);
        if (repositoryName == null) return null;

        try {
            LOG.debug("Trying to access repository: " + repositoryName);
            return repositories.getRepository(repositoryName);
        } catch (RepositoryException re) {
            LOG.debug(re, "Could not load repository named '{0}'", repositoryName);
            return null;
        }
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
    private JcrEngine getEngineFromConfigFile( URL configUrl ) {
        assert configUrl != null;

        /*
         * Strip any query parameters from the incoming file URLs by creating a new URL with the same protocol, host, and port,
         * but using the URL path as returned by URL#getPath() instead of the URL path and query parameters as returned by 
         * URL#getFile().
         * 
         * We need to strip for the file protocol only because an URL with a file protocol and query parameters has the 
         * query parameters appended to the file name (e.g., "file:/tmp/foo/bar?repositoryName=foo" turns into an attempt
         * to access the "/tmp/foo/bar?repositoryName=foo" file).  Other protocol handlers like http handle this better.
         */
        if ("file".equals(configUrl.getProtocol())) {
            try {
                configUrl = new URL(configUrl.getProtocol(), configUrl.getHost(), configUrl.getPort(), configUrl.getPath());
            } catch (MalformedURLException mfe) {
                // This shouldn't be possible, since we're creating a new URL from an existing, valid URL
                throw new IllegalStateException(mfe);
            }
        }
        String configKey = configUrl.toString();

        synchronized (ENGINES) {
            JcrEngine engine = ENGINES.get(configKey);
            if (engine != null) return engine;

            JcrConfiguration config = new JcrConfiguration();
            try {
                config.loadFrom(configUrl);
            } catch (FileNotFoundException fnfe) {
                // If this is a file protocol, double-check that the configuration isn't on the classpath
                if (!"file".equals(configUrl.getProtocol())) {
                    LOG.warn(fnfe, JcrI18n.couldNotStartEngine);
                    return null;

                }
                try {
                    InputStream in = getClass().getResourceAsStream(configUrl.getPath());
                    if (in == null) {
                        LOG.debug(fnfe, JcrI18n.couldNotStartEngine.text());
                        return null;
                    }
                    config.loadFrom(in);
                } catch (IOException ioe) {
                    LOG.debug(fnfe, JcrI18n.couldNotStartEngine.text());
                    return null;
                } catch (SAXException se) {
                    LOG.debug(fnfe, JcrI18n.couldNotStartEngine.text());
                    return null;
                }
            } catch (IOException ioe) {
                LOG.warn(ioe, JcrI18n.couldNotStartEngine);
                return null;
            } catch (SAXException se) {
                LOG.warn(se, JcrI18n.couldNotStartEngine);
                return null;
            }
            engine = config.build();
            engine.start();

            if (engine.getProblems().hasProblems()) {
                LOG.warn(JcrI18n.couldNotStartEngine);
                for (Problem problem : engine.getProblems()) {
                    LOG.warn(problem.getMessage(), problem.getParameters());
                }

                engine.shutdown();
                return null;
            }

            ENGINES.put(configKey, engine);
            return engine;
        }
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
     * Attempts to look up a {@link Repositories} at the given JNDI name. All parameters in the parameters map are passed to the
     * {@link InitialContext} constructor in a {@link Hashtable}.
     * 
     * @param engineJndiName the JNDI name of the JCR engine; may not be null
     * @param parameters any additional parameters that should be passed to the {@code InitialContext}'s constructor; may be empty
     *        or null
     * @return the Repositories object from JNDI, if one exists at the given name
     */
    private Repositories getRepositoriesFromJndi( String engineJndiName,
                                                  Map<String, String> parameters ) {
        try {
            if (parameters == null) parameters = Collections.emptyMap();
            InitialContext ic = new InitialContext(hashtable(parameters));

            Object ob = ic.lookup(engineJndiName);
            if (ob instanceof Repositories) {
                return (Repositories)ob;
            }
            return null;
        } catch (NamingException ne) {
            return null;
        }
    }

    @Override
    public void shutdown() {
        synchronized (ENGINES) {
            for (JcrEngine engine : ENGINES.values()) {
                engine.shutdown();
            }

            ENGINES.clear();
        }
    }

    @Override
    public boolean shutdown( long timeout,
                             TimeUnit unit ) throws InterruptedException {
        synchronized (ENGINES) {
            for (JcrEngine engine : ENGINES.values()) {
                engine.shutdown();
            }

            boolean allShutDownClean = true;
            for (JcrEngine engine : ENGINES.values()) {
                allShutDownClean &= engine.awaitTermination(timeout, unit);
            }

            ENGINES.clear();
            return allShutDownClean;
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
        URL url = urlFor(jcrUrl);
        if (url == null) return null;

        Repositories repositories = repositoriesFor(url, null);
        if (repositories == null) return null;

        if (repositoryName == null) {
            repositoryName = repositoryNameFor(repositories, url);
        }

        return repositories.getRepository(repositoryName);
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
        URL url = urlFor(jcrUrl);
        if (url == null) return null;

        Repositories repositories = repositoriesFor(url, null);

        if (repositories == null) return null;

        return repositories.getRepositoryNames();
    }

    /**
     * Returns the {@link JcrEngine} referenced by the {@code url} parameter.
     * <p>
     * If the {@code url} parameter contains a valid, ModeShape-compatible URL for a {@link JcrEngine} that has not yet been
     * started, that {@code JcrEngine} will be created and {@link JcrEngine#start() started} as a side effect of this method.
     * </p>
     * 
     * @param url the ModeShape-compatible URL that specifies the {@link JcrEngine} to be used; may not be null
     * @param parameters an optional list of parameters that will be passed into the JNDI {@link InitialContext} if the {@code
     *        url} parameter specifies a ModeShape URL that uses the JNDI protocol; may be null or empty
     * @return the {@code JcrEngine} referenced by the given URL if it can be accessed and started (if not already started),
     *         otherwise {@code null}.
     */
    private Repositories repositoriesFor( URL url,
                                          Map<String, String> parameters ) {
        if (url.getPath() == null || url.getPath().trim().length() == 0) {
            LOG.debug("Cannot have null or empty path in repository URL");
            return null;
        }

        Repositories repositories = null;

        if ("jndi".equals(url.getProtocol())) {
            repositories = getRepositoriesFromJndi(url.getPath(), parameters);
        } else {
            repositories = getEngineFromConfigFile(url);
        }

        if (repositories == null) {
            LOG.debug("Could not load engine from URL: " + url);
            return null;
        }

        return repositories;
    }

    /**
     * Convenience method to convert a {@code String} into an {@code URL}. This method never throws a
     * {@link MalformedURLException}, but may throw a {@link NullPointerException} if {@code jcrUrl} is null.
     * 
     * @param jcrUrl the string representation of an URL that should be converted into an URL; may not be null
     * @return the URL version of {@code jcrUrl} if {@code jcrUrl} is a valid URL, otherwise null
     */
    private URL urlFor( String jcrUrl ) {
        if (jcrUrl == null || jcrUrl.isEmpty()) {
            throw new IllegalStateException(JcrI18n.invalidJcrUrl.text(jcrUrl));
        }

        try {
            return new URL(jcrUrl.toString());
        } catch (MalformedURLException mue) {
            LOG.debug("Could not parse URL: " + mue.getMessage());
            return null;
        }
    }

    /**
     * Returns the repository name to use for the given URL. If the {@code url} contains a query parameter named {@code
     * repositoryName}, the value of that query parameter is returned. If the {@code url} does not contain a query paramer named
     * {@code repositoryName} then {@code engine} is checked to see if it contains exactly one repository. If so, that repository
     * is returned. If {@code engine} contains more than one JCR repository and the {@code repositoryName} parameter is {@code
     * null}, then {@code null} is returned.
     * <p>
     * NOTE: If a repository name is provided in the {@code url} parameter, this method does not validate that a repository with
     * that name exists in {@code engine}.
     * </p>
     * 
     * @param repositories the container of the named repositories, used to check for a default repository name if none is
     *        provided in the {@code url}; may be null only if {@code url} explicitly provides a repository name
     * @param url the url for which the repository name should be returned; may not be null
     * @return the repository name to use based on the algorithm described above; may be null
     */
    private String repositoryNameFor( Repositories repositories,
                                      URL url ) {
        String repositoryName = null;
        String query = url.getQuery();
        if (query != null) {
            for (String keyValuePair : query.split("&")) {
                String[] splitPair = keyValuePair.split("=");

                if (splitPair.length == 2 && REPOSITORY_NAME_PARAM.equals(splitPair[0])) {
                    repositoryName = splitPair[1];
                    break;
                }
            }
        }

        if (repositoryName == null) {
            Set<String> repositoryNames = repositories.getRepositoryNames();

            if (repositoryNames.size() != 1) {
                LOG.debug("No repository name provided in URL and multiple repositories configured in engine with following names: "
                          + repositoryNames);
                return null;
            }

            repositoryName = repositoryNames.iterator().next();
        }
        return repositoryName;
    }

    @Override
    public Repositories getRepositories( String jcrUrl ) {
        URL url = urlFor(jcrUrl);
        if (url == null) return null;
        return repositoriesFor(url, null);
    }

}
