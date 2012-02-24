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

import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.jcr.Repository;

/**
 * An extension to the standard {@link javax.jcr.RepositoryFactory} interface, with ModeShape-specific constants and additional
 * {@link #shutdown()} methods.
 * <p>
 * ModeShape's RepositoryFactory implementation looks for two parameters:
 * <ul>
 * <li><code><b>org.modeshape.jcr.URL</b></code> - This parameter specifies the URL of the configuration file for the ModeShape
 * engine with an optional "<code>repositoryName</code>" URL parameter specifying which of the repository defined in the
 * configuration file should be used. Alternatively, if the URL begins with "<code>jndi:</code>", the object in the JNDI name will
 * be looked up, and returned if it is a {@link Repository} instance or used to find the Repository instance if the JNDI object is
 * the ModeShape engine.
 * <li><code><b>org.modeshape.jcr.RepositoryName</b></code> - This parameter specifies the name of the repository that is to be
 * used, and that is an alternative to the "<code>repositoryName</code> URL parameter.
 * </ul>
 * Often, both properties will be used, resulting in ModeShape's factory using this logic:
 * <ol>
 * <li>Look for an already-deployed repository with the name given by <code>org.modeshape.jcr.RepositoryName</code>. If one is
 * found, then return that Repository instance.</li>
 * <li>Look for the repository's configuration file at the URL given by <code>org.modeshape.jcr.URL</code>. If the file had
 * already been loaded, find the repository and return it; otherwise attempt to load the file, start the engine, and find the
 * Repository instance with the given name.</li>
 * </ol>
 * <h2>Use the Standard JCR API</h2>
 * <p>
 * The best way for your application to use the RepositoryFactory is to use only the JCR API, and load the properties from a file.
 * This way, only the file has implementation-specific information, while your application uses only the standard JCR API:
 * 
 * <pre>
 * Properties parameters = new Properties();
 * parameters.load(...); // Load from a stream or reader
 * 
 * Repository repository = null;
 * for (RepositoryFactory factory : ServiceLoader.load(RepositoryFactory.class)) {
 *     repository = factory.getRepository(parameters);
 *     if (repository != null) break;
 * }
 * </pre>
 * 
 * </p>
 * <h2>Use the ModeShape constants</h2>
 * <p>
 * If you'd rather your application programmatically create the parameters to pass to JCR's RepositoryFactory, and your
 * application is already dependent upon the ModeShape public API, you can use the constants in this interface to build your
 * parameter.
 * </p>
 * <p>
 * The preferred approach is to specify the location of the ModeShape configuration file and the name of the repository using
 * separate parameters:
 * 
 * <pre>
 * String configUrl = &quot;file://path/to/configFile.xml&quot;; // URL that points to ModeShape's configuration file
 * String repoName = &quot;MyRepository&quot;; // Name of the repository
 * 
 * Map&lt;String, String&gt; parameters = new HashMap&lt;String, String&gt;();
 * parameters.put(org.modeshape.jcr.api.RepositoryFactory.URL, configUrl);
 * parameters.put(org.modeshape.jcr.api.RepositoryFactory.RepositoryName, repoName);
 * 
 * Repository repository = null;
 * for (RepositoryFactory factory : ServiceLoader.load(RepositoryFactory.class)) {
 *     repository = factory.getRepository(parameters);
 *     if (repository != null) break;
 * }
 * </pre>
 * 
 * </p>
 * <p>
 * Note, however, that the repository name could have been specified with a URL parameter:
 * 
 * <pre>
 * String configUrl = &quot;file://path/to/configFile.xml?repositoryName=MyRepository&quot;;
 * 
 * Map&lt;String, String&gt; parameters = new HashMap&lt;String, String&gt;();
 * parameters.put(org.modeshape.jcr.api.RepositoryFactory.URL, configUrl);
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
public interface RepositoryFactory extends javax.jcr.RepositoryFactory {

    /**
     * The name of the key for the ModeShape JCR URL in the parameter map.
     * <p>
     * For example:
     * 
     * <pre>
     * String configUrl = &quot;file://path/to/configFile.xml?repositoryName=myRepository&quot;; // URL that points to your configuration file
     * 
     * Map&lt;String, String&gt; parameters = new HashMap&lt;String, String&gt;();
     * parameters.put(org.modeshape.jcr.api.RepositoryFactory.URL, configUrl);
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
    public static final String URL = "org.modeshape.jcr.URL";

    /**
     * The name of the key for the ModeShape JCR repository name in the parameter map. This can be used as an alternative to
     * specifying the repository name as a URL parameter within the {@link #URL URL}.
     * <p>
     * For example:
     * 
     * <pre>
     * String configUrl = &quot;file://path/to/configFile.xml&quot;; // URL that points to your configuration file
     * String repoName = &quot;myRepository&quot;; // Name of your repository defined within the configuration file
     * 
     * Map&lt;String, String&gt; parameters = new HashMap&lt;String, String&gt;();
     * parameters.put(org.modeshape.jcr.api.RepositoryFactory.URL, configUrl);
     * parameters.put(org.modeshape.jcr.api.RepositoryFactory.REPOSITORY_NAME, repoName);
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
    public static final String REPOSITORY_NAME = "org.modeshape.jcr.RepositoryName";

    /**
     * Begin the shutdown process for all the {@code JcrEngine JcrEngines} created by calls to {@link #getRepository(Map)}.
     * <p>
     * Calling {@code #getRepository(Map)} with a file-based URL parameter causes a new {@code JcrEngine} to be instantiated and
     * started. Any {@code JcrEngine} created in this manner must be stored by the {@code RepositoryFactory} implementation.
     * Invoking this method iteratively invokes the {@code shutdown()} method on each {@code JcrEngine}.
     * </p>
     * <p>
     * This method merely initiates the shutdown process for each {@code JcrEngine}. There is no guarantee that the shutdown
     * process will have completed prior to this method returning. The {@link #shutdown(long, TimeUnit)} method provides the
     * ability to wait until all engines are shutdown or the given time elapses.
     * </p>
     * <p>
     * Invoking this method does not preclude creating new {@code JcrEngines} with future calls to {@link #getRepository(Map)}.
     * Any caller using this method as part of an application shutdown process should take care to cease invocations of
     * {@link #getRepository(Map)} prior to invoking this method.
     * </p>
     */
    public void shutdown();

    /**
     * Begin the shutdown process for all the {@code JcrEngine JcrEngines} created by calls to {@link #getRepository(Map)}.
     * <p>
     * Calling {@code #getRepository(Map)} with a file-based URL parameter causes a new {@code JcrEngine} to be instantiated and
     * started. Any {@code JcrEngine} created in this manner must be stored by the {@code RepositoryFactory} implementation.
     * Invoking this method iteratively invokes the {@code shutdown()} method on each {@code JcrEngine} and then iteratively
     * invokes the {@code awaitTermination(long, TimeUnit)} method to await termination.
     * </p>
     * <p>
     * Although this method initiates the shutdown process for each {@code JcrEngine} and invokes the {@code awaitTermination}
     * method, there is still no guarantee that the shutdown process will have completed prior to this method returning. It is
     * possible for the time required to shutdown one or more of the engines to exceed the provided time window.
     * </p>
     * <p>
     * Invoking this method does not preclude creating new {@code JcrEngines} with future calls to {@link #getRepository(Map)}.
     * Any caller using this method as part of an application shutdown process should take care to cease invocations of
     * {@link #getRepository(Map)} prior to invoking this method.
     * </p>
     * 
     * @param timeout the maximum time per engine to allow for shutdown
     * @param unit the time unit of the timeout argument
     * @return <tt>true</tt> if all engines completely shut down and <tt>false</tt> if the timeout elapsed before it was shut down
     *         completely
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean shutdown( long timeout,
                             TimeUnit unit ) throws InterruptedException;

    /**
     * Returns the {@link Repositories} instance referenced by the {@code jcrUrl} parameter.
     * <p>
     * If the {@code jcrUrl} parameter contains a valid, ModeShape-compatible URL for a {@code Repositories} instance that has not
     * yet been started, that {@code Repositories instance} will be created and started as a side effect of this method.
     * </p>
     * 
     * @param jcrUrl the ModeShape-compatible URL that specifies the {@code JcrEngine} to be used; may not be null
     * @return the {@code Repositories} instance specified by the given url if one exists, otherwise {@code null}
     */
    public Repositories getRepositories( String jcrUrl );

}
