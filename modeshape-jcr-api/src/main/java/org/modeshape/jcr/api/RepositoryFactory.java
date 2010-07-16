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

public interface RepositoryFactory extends javax.jcr.RepositoryFactory {

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
