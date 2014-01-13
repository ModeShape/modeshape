/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.api;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * An extension to the standard {@link javax.jcr.RepositoryFactory} interface, with ModeShape-specific constants and additional
 * {@link #shutdown()} methods.
 * <p>
 * ModeShape's RepositoryFactory implementation looks for two parameters:
 * <ul>
 * <li><code><b>org.modeshape.jcr.URL</b></code> - This parameter specifies the URL of the configuration file for the repository
 * to be used.
 * <li><code><b>org.modeshape.jcr.RepositoryName</b></code> - This parameter specifies the name of the repository that is to be
 * used.
 * </ul>
 * Often, both properties will be used, resulting in ModeShape's factory using this logic:
 * <ol>
 * <li>Look for an already-deployed repository with the name given by <code>org.modeshape.jcr.RepositoryName</code>. If one is
 * found, then return that Repository instance.</li>
 * <li>Look for the repository's configuration file at the URL given by <code>org.modeshape.jcr.URL</code>. If the file had
 * already been loaded, find the repository and return it; otherwise attempt to load the file, deploy the repository, and return
 * the Repository instance.
 * <li>
 * </ol>
 * <p>
 * But strictly speaking, only the <code>org.modeshape.jcr.api.URL</code> parameter is required, since the configuration file
 * contains the name of the repository. So why supply the <code>org.modeshape.jcr.RepositoryName</code> parameter? Because
 * ModeShape's {@link javax.jcr.RepositoryFactory#getRepository(Map)} method can look up an existing repository by name faster
 * than it can load the configuration file. In other words, using both parameters makes for a faster operation.
 * </p>
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
 * parameters.
 * 
 * <pre>
 * String configUrl = &quot;file://path/to/configFile.json&quot;; // URL that points to the repository's configuration file
 * String repoName = &quot;MyRepository&quot;; // Name of the repository (this is optional)
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
 */
public interface RepositoryFactory extends javax.jcr.RepositoryFactory, Repositories {

    /**
     * The name of the key for the ModeShape JCR URL in the parameter map.
     * <p>
     * For example, define a URL that points to the configuration file for your repository:
     * 
     * <pre>
     * \// Define a 
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
     * String configUrl = &quot;file://path/to/configFile.json&quot;; // URL that points to your configuration file
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
     * Shutdown this engine to stop all repositories created by calls to {@link #getRepository(Map)}, terminate any ongoing
     * background operations (such as sequencing), and reclaim any resources that were acquired by the repositories. This method
     * may be called multiple times, but only the first time has an effect.
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
     *         {@link java.util.concurrent.ExecutionException}. The value returned from the future will always be true if the engine shutdown (or was
     *         not running), or false if the engine is still running.
     *
     * @deprecated since 3.4, this method should not be used. Code using it should change to use {@link org.modeshape.jcr.api.RepositoriesContainer} instead
     */
    @Deprecated
    public Future<Boolean> shutdown();

    /**
     * Shutdown this engine to stop all repositories created by calls to {@link #getRepository(Map)}, terminate any ongoing
     * background operations (such as sequencing), and reclaim any resources that were acquired by the repositories. This method
     * may be called multiple times, but only the first time has an effect.
     * <p>
     * This method is equivalent to calling "<code>shutdown().get(timeout,unit)</code>" on this method.
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
     * @param timeout the maximum time per engine to allow for shutdown
     * @param unit the time unit of the timeout argument
     * @return <tt>true</tt> if all engines completely shut down and <tt>false</tt> if the timeout elapsed before it was shut down
     *         completely
     * @throws InterruptedException if interrupted while waiting
     *
     * @deprecated since 3.4, this method should not be used. Code using it should change to use {@link org.modeshape.jcr.api.RepositoriesContainer} instead
     */
    @Deprecated
    public boolean shutdown( long timeout,
                             TimeUnit unit ) throws InterruptedException;


    /**
     * @deprecated since 3.4, this method should not be used. Code using it should change to use {@link org.modeshape.jcr.api.RepositoriesContainer} instead
     */
    @Override
    @Deprecated
    Set<String> getRepositoryNames();

    /**
     * @deprecated since 3.4, this method should not be used. Code using it should change to use {@link org.modeshape.jcr.api.RepositoriesContainer} instead
     */
    @Override
    @Deprecated
    javax.jcr.Repository getRepository( String repositoryName ) throws javax.jcr.RepositoryException;
}
