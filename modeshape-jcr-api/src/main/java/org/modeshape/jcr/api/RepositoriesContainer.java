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
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.jcr.RepositoryException;

/**
 * Interfaces which defines components acting as containers for one or more {@link Repository} instances.
 * <p/>
 * This is semantically different from a {@link RepositoryFactory} in that a repository factory should only be concerned with
 * creating/obtaining a new repository instance. A {@link RepositoryFactory} does not expose any contract around handling "multiple repositories".
 * <p/>
 * When looking for specific repositories however, this will use a similar {@link java.util.Map} of named parameters as
 * {@link RepositoryFactory} instance, but in a more "specific" manner.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public interface RepositoriesContainer {

    /**
     * The name of the key for the ModeShape JCR URL in the parameter map.
     *
     * @see {@link RepositoryFactory#URL}
     */
    public static final String URL = RepositoryFactory.URL;

    /**
     * The name of the key for the ModeShape JCR repository name in the parameter map. This can be used as an alternative to
     * specifying the repository name as a URL parameter within the {@link #URL URL}.
     *
     * @see {@link RepositoryFactory#REPOSITORY_NAME}
     */
    public static final String REPOSITORY_NAME = RepositoryFactory.REPOSITORY_NAME;

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
    public Future<Boolean> shutdown();

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
     * This method returns immediately, even before the repositories have been shut down. However, the caller can simply call the
     * {@link Future#get() get()} method on the returned {@link Future} to block until all repositories have shut down. Note that
     * the {@link Future#get(long, java.util.concurrent.TimeUnit)} method can be called to block for a maximum amount of time.
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
     * Returns the names of all the available repositories, using an optional map of parameters which may be used to initialize
     * additional repositories, which will also be returned.
     *
     * @param parameters map of string key/value pairs as repository arguments or {@link null} if none are provided
     * @return the immutable set of repository names provided by this container; never {@code null}
     * @throws javax.jcr.RepositoryException if there is an error performing the lookup.
     *
     * @see RepositoryFactory#getRepository(java.util.Map)
     */
    public Set<String> getRepositoryNames( Map<?, ?> parameters ) throws RepositoryException;

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
    public javax.jcr.Repository getRepository( String repositoryName,
                                               Map<?, ?> parameters ) throws javax.jcr.RepositoryException;
}
