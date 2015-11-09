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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.jcr.RepositoryException;

/**
 * Interface which defines components acting as containers for one or more {@link Repository} instances.
 * A repository container is an entity that can manage a number of repositories, each repository being identified by a name.
 * It is also able to start/{@link #shutdown()}/return the names of all the repositories it manages.
 * <p/>
 * This is meant to replace the "container aspect" of the current {@link RepositoryFactory} interface.
 * {@link RepositoryFactory} should only be used for creating/obtaining a new repository instance, as defined in the
 * {@link javax.jcr.RepositoryFactory} contract.
 * <p/>
 * When looking for and initializing specific repositories, this will use a similar {@link java.util.Map} of named parameters as
 * {@link RepositoryFactory} but with a more "loose semantic":
 * <ul>
 *     <li>
 *         when a repository should be initialized for the first time, the {@link #getRepository(String, java.util.Map)} should
 *         be called, where the map of parameters contains a parameter named <b>{@code org.modeshape.jcr.URL}</b> which has
 *         a {@code String} value representing either the path to a JSON configuration file or, if it uses the {@code jndi}
 *         protocol, the name under which the repository is bound in JNDI.
 *     </li>
 *     <li>
 *         if a repository has already been initialized & started up as described above, the {@link #getRepository(String, java.util.Map)}
 *         method can be called with a {@code null} or empty map, as the container already holds a reference to the repository.
 *     </li>
 * </ul>
 * <p/>
 * <h2>Getting a RepositoriesContainer instance</h2>
 * In order to use this service, clients will need to use the ModeShape API via the standard {@link java.util.ServiceLoader} mechanism,
 *
 * <pre>
 *   Iterator containersIterator = ServiceLoader.load(RepositoriesContainer.class).iterator();
 *   if (!containersIterator.hasNext()) {
 *    // no implementations are found on the classpath, meaning the modeshape-jcr.jar has not been correctly loaded.
 *   }
 *   //there shouldn't be more than 1 container
 *   RepositoriesContainer repositoriesContainer = containersIterator.next();
 * </pre>
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 3.4
 */
public interface RepositoriesContainer {

    /**
     * The name of the key for the ModeShape JCR URL in the parameter map.
     *
     * @see RepositoryFactory#URL
     */
    String URL = RepositoryFactory.URL;

    /**
     * The name of the key for the ModeShape JCR repository name in the parameter map. This can be used as an alternative to
     * specifying the repository name as a URL parameter within the {@link #URL URL}.
     *
     * @see RepositoryFactory#REPOSITORY_NAME
     */
    String REPOSITORY_NAME = RepositoryFactory.REPOSITORY_NAME;

    /**
     * Shutdown this engine to stop all repositories created by calls to {@link #getRepository(String, java.util.Map)}, terminate any ongoing
     * background operations (such as sequencing), and reclaim any resources that were acquired by the repositories. This method
     * may be called multiple times, but only the first time has an effect.
     * <p>
     * Invoking this method does not preclude creating new {@link javax.jcr.Repository} instances with future calls to
     * {@link #getRepository(String, java.util.Map)}. Any caller using this method as part of an application shutdown process should take care to
     * cease invocations of {@link #getRepository(String, java.util.Map)} prior to invoking this method.
     * </p>
     * <p>
     * This method returns immediately, even before the repositories have been shut down. However, the caller can simply call the
     * {@link java.util.concurrent.Future#get() get()} method on the returned {@link java.util.concurrent.Future} to block until all repositories have shut down. Note that
     * the {@link java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)} method can be called to block for a maximum amount of time.
     * </p>
     *
     * @return a future that allows the caller to block until the engine is shutdown; any error during shutdown will be thrown
     *         when {@link java.util.concurrent.Future#get() getting} the repository from the future, where the exception is wrapped in a
     *         {@link java.util.concurrent.ExecutionException}. The value returned from the future will always be true if the engine shutdown (or was
     *         not running), or false if the engine is still running.
     */
    Future<Boolean> shutdown();

    /**
     * Shutdown this engine to stop all repositories created by calls to {@link #getRepository(String, java.util.Map)}, terminate any ongoing
     * background operations (such as sequencing), and reclaim any resources that were acquired by the repositories. This method
     * may be called multiple times, but only the first time has an effect.
     * <p>
     * This method is equivalent to calling "<code>shutdown().get(timeout,unit)</code>" on this method.
     * </p>
     * <p>
     * Invoking this method does not preclude creating new {@link javax.jcr.Repository} instances with future calls to
     * {@link #getRepository(String, java.util.Map)}. Any caller using this method as part of an application shutdown process should take care to
     * cease invocations of {@link #getRepository(String, java.util.Map)} prior to invoking this method.
     * </p>
     * <p>
     * This method waits until all the repositories have been successfully shut-down, or the given timeout has elapsed.
     * </p>
     * @param timeout the maximum time per engine to allow for shutdown
     * @param unit the time unit of the timeout argument
     * @return <tt>true</tt> if all engines completely shut down and <tt>false</tt> if the timeout elapsed before it was shut down
     *         completely
     * @throws InterruptedException if interrupted while waiting
     */
    boolean shutdown(long timeout,
                     TimeUnit unit) throws InterruptedException;

    /**
     * Asynchronously shutdown the deployed {@link javax.jcr.Repository} instance with the given the name, and return a future that will
     * return whether the Repository instance is shutdown. If the Repository is not running, the resulting future will return
     * immediately.
     * <p>
     * Note that the caller does not have to wait for the shutdown to completed. However, to do so the caller merely calls
     * {@link Future#get() get()} or {@link Future#get(long, TimeUnit) get(long,TimeUnit)} on the future to return a boolean flag
     * specifying whether the Repository instance is shutdown (not running). Note that any exceptions thrown during the shutdown
     * will be wrapped in an {@link ExecutionException} thrown by the Future's <code>get</code> methods.
     * </p>
     *
     * @param repositoryName the name of the deployed repository
     * @return a future wrapping the asynchronous shutdown process; never null, and {@link Future#get()} will return whether the
     * @throws IllegalArgumentException if the repository name is null, blank or invalid
     * @throws RepositoryException if there is no repository with the specified name or if any other unexpected error occurs.
     * 
     * @since 5.0
     */
    Future<Boolean> shutdownRepository( String repositoryName ) throws RepositoryException;

    /**
     * Returns the names of all the available repositories, using an optional map of parameters which may be used to initialize
     * additional repositories, which will also be returned.
     *
     * @param parameters map of string key/value pairs as repository arguments or {@code null} if none are provided
     * @return the immutable set of repository names provided by this container; never {@code null}
     * @throws javax.jcr.RepositoryException if there is an error performing the lookup.
     *
     * @see RepositoryFactory#getRepository(java.util.Map)
     */
     Set<String> getRepositoryNames( Map<?, ?> parameters ) throws RepositoryException;

    /**
     * Return the JCR Repository with the supplied name and an optional map of parameters which can be used to initialize the
     * repository.
     *
     * @param repositoryName the name of the repository to return; may be {@code null} if, for example, the parameters map already
     * contains this information.
     * @param parameters map of string key/value pairs as repository arguments. My be {@code null} if no configuration parameters
     * exist.
     * @return the repository with the given name or {@code null} if no repository is found
     * @throws javax.jcr.RepositoryException if there is an error communicating with the repository
     *
     * @see RepositoryFactory#getRepository(java.util.Map)
     */
    Repository getRepository(String repositoryName,
                             Map<?, ?> parameters) throws javax.jcr.RepositoryException;
}
