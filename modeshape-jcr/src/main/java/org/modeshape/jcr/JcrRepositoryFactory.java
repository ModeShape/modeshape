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
 * Map params = Collections.singletonMap(JcrRepositoryFactory.URL, configUrl);
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
 * file:src/test/resources/configRepository.xml?repositoryName=MyRepository}) OR a {@link JcrEngine} stored in the JNDI tree
 * (e.g., {@code jndi://name/of/JcrEngine/resource?repositoryName=MyRepository}).
 * </p>
 * 
 * @see #getRepository(Map)
 * @see RepositoryFactory#getRepository(Map)
 */
@ThreadSafe
public class JcrRepositoryFactory implements RepositoryFactory {

    private static final Logger LOG = Logger.getLogger(JcrRepositoryFactory.class);

    /**
     * A map of configuration file locations to existing engines. This map helps ensure that JcrEngines are not recreated with
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

        String rawUrl = (String)parameters.get(URL);
        if (rawUrl == null) {
            LOG.debug("No parameter found with key: " + URL);
            return null;
        }

        URL url;
        try {
            url = new URL(rawUrl);
        } catch (MalformedURLException mue) {
            LOG.debug("Could not parse URL: " + mue.getMessage());
            return null;
        }

        if (url.getPath() == null || url.getPath().trim().length() == 0) {
            LOG.debug("Cannot have null or empty path in repository URL");
            return null;
        }

        JcrEngine engine = null;

        if ("jndi".equals(url.getProtocol())) {
            engine = getEngineFromJndi(url.getPath(), parameters);
        } else {
            engine = getEngineFromConfigFile(url);
        }

        if (engine == null) {
            LOG.debug("Could not load engine from URL: " + url);
            return null;
        }

        String query = url.getQuery();
        String repositoryName = null;
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
            Set<String> repositoryNames = engine.getRepositoryNames();

            if (repositoryNames.size() != 1) {
                LOG.debug("No repository name provided in URL and multiple repositories configured in engine with following names: "
                          + repositoryNames);
                return null;
            }

            repositoryName = repositoryNames.iterator().next();
        }

        try {
            LOG.debug("Trying to access repository: " + repositoryName);
            return engine.getRepository(repositoryName);
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

        synchronized (ENGINES) {
            String configKey = configUrl.toString();

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
     * Attempts to look up a JcrEngine at the given JNDI name. All parameters in the parameters map are passed to the
     * {@link InitialContext} constructor in a {@link Hashtable}.
     * 
     * @param engineJndiName the JNDI name of the JCR engine; may not be null
     * @param parameters any additional parameters that should be passed to the {@code InitialContext}'s constructor; may be empty
     *        but may not be null
     * @return the JcrEngine from JNDI, if one exists at the given name
     */
    private JcrEngine getEngineFromJndi( String engineJndiName,
                                         Map<String, String> parameters ) {
        try {
            InitialContext ic = new InitialContext(hashtable(parameters));

            Object ob = ic.lookup(engineJndiName);
            if (!(ob instanceof JcrEngine)) return null;
            return (JcrEngine)ob;
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
}
