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
package org.modeshape.jcr.api;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Service provider for the JCR2 {@code RepositoryFactory} interface. This class provides a single public method,
 * {@link #getRepository(Map)}, that enables finding a JCR repository that is registered in JNDI.
 * <p>
 * The canonical way to get a reference to this class is to use the {@link ServiceLoader}. For example, when the
 * {@link Repository} instance is registered in JNDI, the following code will find it via the ServiceLoader:
 * 
 * <pre>
 * String jndiUrl = &quot;jndi:jcr/local/myRepository&quot;;
 * Map parameters = Collections.singletonMap(JndiRepositoryFactory.URL, configUrl);
 * Repository repository;
 * 
 * for (RepositoryFactory factory : ServiceLoader.load(RepositoryFactory.class)) {
 *     repository = factory.getRepository(parameters);
 *     if (repository != null) break;
 * }
 * </pre>
 * 
 * Or, if the ModeShape engine is registered in JNDI at "jcr/local", the same technique can be used with a slightly modified URL
 * parameter:
 * 
 * <pre>
 * String jndiUrl = &quot;jndi:jcr/local?repositoryName=myRepository&quot;; // Different URL
 * Map parameters = Collections.singletonMap(JndiRepositoryFactory.URL, configUrl);
 * Repository repository;
 * 
 * for (RepositoryFactory factory : ServiceLoader.load(RepositoryFactory.class)) {
 *     repository = factory.getRepository(parameters);
 *     if (repository != null) break;
 * }
 * </pre>
 * 
 * Alternatively, the repository name can be provided completely separately from the JNDI URL (again, if the ModeShape engine is
 * registered in JNDI at "jcr/local"):
 * 
 * <pre>
 * String jndiUrl = &quot;jndi:jcr/local&quot;;
 * String repoName = &quot;myRepository&quot;;
 * 
 * Map&lt;String, String&gt; parameters = new HashMap&lt;String, String&gt;();
 * parameters.put(org.modeshape.jcr.api.JndiRepositoryFactory.URL, jndiUrl);
 * parameters.put(org.modeshape.jcr.api.JndiRepositoryFactory.REPOSITORY_NAME, repoName);
 * 
 * Repository repository = null;
 * for (RepositoryFactory factory : ServiceLoader.load(RepositoryFactory.class)) {
 *     repository = factory.getRepository(parameters);
 *     if (repository != null) break;
 * }
 * </pre>
 * 
 * @see #getRepository(Map)
 * @see RepositoryFactory#getRepository(Map)
 */
public class JndiRepositoryFactory implements javax.jcr.RepositoryFactory {

    /**
     * The name of the key for the ModeShape JCR URL in the parameter map.
     * <p>
     * For example, define a URL that points to the configuration file for your repository:
     * 
     * <pre>
     * String jndiUrl = &quot;jndi:jcr/local/myRepository&quot;;
     * 
     * Map&lt;String, String&gt; parameters = new HashMap&lt;String, String&gt;();
     * parameters.put(org.modeshape.jcr.api.JndiRepositoryFactory.URL, configUrl);
     * 
     * Repository repository = null;
     * for (RepositoryFactory factory : ServiceLoader.load(RepositoryFactory.class)) {
     *     repository = factory.getRepository(parameters);
     *     if (repository != null) break;
     * }
     * </pre>
     * 
     * </p>
     */
    public static final String URL = RepositoryFactory.URL;

    /**
     * The name of the key for the ModeShape JCR repository name in the parameter map. This can be used as with a {@link #URL URL}
     * that contains the JNDI name of a {@link Repositories} implementation.
     * <p>
     * For example:
     * 
     * <pre>
     * String jndiUrl = &quot;jndi:jcr/local&quot;;
     * String repoName = &quot;myRepository&quot;;
     * 
     * Map&lt;String, String&gt; parameters = new HashMap&lt;String, String&gt;();
     * parameters.put(org.modeshape.jcr.api.JndiRepositoryFactory.URL, jndiUrl);
     * parameters.put(org.modeshape.jcr.api.JndiRepositoryFactory.REPOSITORY_NAME, repoName);
     * 
     * Repository repository = null;
     * for (RepositoryFactory factory : ServiceLoader.load(RepositoryFactory.class)) {
     *     repository = factory.getRepository(parameters);
     *     if (repository != null) break;
     * }
     * </pre>
     * 
     * </p>
     */
    public static final String REPOSITORY_NAME = RepositoryFactory.REPOSITORY_NAME;

    /**
     * The name of the URL parameter that specifies the repository name.
     */
    public static final String REPOSITORY_NAME_PARAM = "repositoryName";

    @SuppressWarnings( {"rawtypes", "unchecked"} )
    @Override
    public Repository getRepository( Map parameters ) throws RepositoryException {
        URL url = getUrlFrom(parameters);
        if (url == null) return null;

        if (!"jndi".equals(url.getProtocol())) {
            // This URL is not a JNDI URL and therefore we don't understand it ...
            return null;
        }

        // Now try to look up the repository in JNDI ...
        return getRepositoryFromJndi(url, parameters);
    }

    protected URL getUrlFrom( Map<String, Object> parameters ) {
        if (parameters == null) return null;
        Object rawUrl = parameters.get(RepositoryFactory.URL);
        if (rawUrl == null) {
            return null;
        }

        // Convert the raw value to a URL object ...
        URL url = null;
        if (rawUrl instanceof URL) {
            url = (URL)rawUrl;
        } else {
            url = urlFor(rawUrl.toString(), null);
        }
        return url;
    }

    protected String getRepositoryNameFrom( URL url,
                                            Map<String, Object> parameters ) {
        if (parameters != null) {
            // First look in the parameters ...
            Object repoName = parameters.get(REPOSITORY_NAME);
            if (repoName != null) {
                return repoName.toString();
            }
        }

        // Then look for a query parameter in the URL ...
        String query = url.getQuery();
        if (query != null) {
            for (String keyValuePair : query.split("&")) {
                String[] splitPair = keyValuePair.split("=");

                if (splitPair.length == 2 && REPOSITORY_NAME_PARAM.equals(splitPair[0])) {
                    return splitPair[1];
                }
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
        assert jcrUrl != null;
        assert !jcrUrl.isEmpty();
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
            return null;
        }
    }

    /**
     * Returns a hashtable with the same key/value mappings as the given map
     * 
     * @param map the set of key/value mappings to convert; may not be null
     * @return a hashtable with the same key/value mappings as the given map; may be empty, never null
     */
    private Hashtable<String, String> hashtable( Map<String, Object> map ) {
        assert map != null;
        Hashtable<String, String> hash = new Hashtable<String, String>(map.size());
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            hash.put(entry.getKey(), value != null ? value.toString() : null);
        }
        return hash;
    }

    /**
     * Attempts to look up a {@link Repository} at the given JNDI name. All parameters in the parameters map are passed to the
     * {@link InitialContext} constructor in a {@link Hashtable}.
     * 
     * @param url the URL containing the JNDI name of the {@link Repository} or {@link Repositories} instance; may not be null
     * @param parameters any additional parameters that should be passed to the {@code InitialContext}'s constructor; may be empty
     *        or null
     * @return the Repository object from JNDI, if one exists at the given name
     * @throws RepositoryException if there is a problem obtaining the repository
     */
    private Repository getRepositoryFromJndi( URL url,
                                              Map<String, Object> parameters ) throws RepositoryException {
        String jndiName = url.getPath();
        if (jndiName == null) return null;

        // There should be a parameter with the name ...
        try {
            InitialContext ic = new InitialContext(hashtable(parameters));

            Object ob = ic.lookup(jndiName);
            if (ob instanceof Repository) {
                // The object in JNDI is a Repository, so simply return it ...
                return (Repository)ob;

            } else if (ob instanceof Repositories) {
                // The object in JNDI was a Repositories object that allows us to look up the Repository by name
                Repositories repos = (Repositories)ob;

                // Get the name from the parameters or the URL ...
                String repositoryName = getRepositoryNameFrom(url, parameters);

                // Now look up the repository by name ...
                return repos.getRepository(repositoryName);
            }
            return null;
        } catch (NamingException ne) {
            return null;
        }
    }

    /**
     * Get the names of the available repositories given the supplied parameters (which must have a URL pointing to a
     * {@link Repositories} object).
     * 
     * @param parameters map of string key/value pairs as repository arguments or <code>null</code> if none are provided and a
     *        client wishes to connect to a default repository.
     * @return the immutable set of repository names provided by this server; may be empty if the implementation does not
     *         understand the passed <code>parameters</code>.
     * @throws RepositoryException if if no suitable repository is found or another error occurs.
     * @throws RepositoryException
     */
    public Set<String> getRepositoryNames( Map<String, Object> parameters ) throws RepositoryException {
        URL url = getUrlFrom(parameters);
        if (url == null) return null;

        if (!"jndi".equals(url.getProtocol())) {
            // This URL is not a JNDI URL and therefore we don't understand it ...
            return null;
        }

        String jndiName = url.getPath();
        try {
            InitialContext ic = new InitialContext(hashtable(parameters));

            Object ob = ic.lookup(jndiName);
            if (ob instanceof NamedRepository) {
                // The object in JNDI is a Repository, so simply return it ...
                return Collections.singleton(((NamedRepository)ob).getName());

            } else if (ob instanceof Repositories) {
                // The object in JNDI was a Repositories object that allows us to look up the Repository by name
                Repositories repos = (Repositories)ob;

                // Now look up the repository by name ...
                return repos.getRepositoryNames();
            }
        } catch (NamingException ne) {
            // do nothing ...
        }
        return Collections.emptySet();
    }

}
