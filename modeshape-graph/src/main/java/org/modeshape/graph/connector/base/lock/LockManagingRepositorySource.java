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
package org.modeshape.graph.connector.base.lock;

import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.base.Repository;

/**
 * Interface for repository sources that allow users to configure and manage their locking strategy.
 * 
 * @see LockProvider
 * @see LockStrategy
 */
public interface LockManagingRepositorySource extends RepositorySource {

    /**
     * Sets the {@link LockProvider lock provider class name} for this {@link Repository repository}.
     * 
     * @param lockProviderClassName the class name of the lock provider; null indicates that the default for the repository should
     *        be used. Each repository is free to define its own default lock provider.
     */
    void setLockProviderClassName( String lockProviderClassName );

    /**
     * Returns the class name of the current lock provider for this repository.
     * 
     * @return the class name of the current lock provider for this repository; never null.
     */
    String getLockProviderClassName();

    /**
     * Sets the {@link LockStrategy lock strategy class name} for this {@link Repository repository}.
     * 
     * @param lockStrategyClassName the class name of the lock strategy; null indicates that the default for the repository should
     *        be used. Each repository is free to define its own default lock strategy.
     */
    void setLockStrategyClassName( String lockStrategyClassName );

    /**
     * Returns the class name of the current lock strategy for this repository.
     * 
     * @return the class name of the current lock strategy for this repository; never null.
     */
    String getLockStrategyClassName();

    /**
     * Returns the current {@link LockProvider}
     * 
     * @return the current lock provider; never null
     */
    LockProvider getLockProvider();

    /**
     * Returns the current {@link LockStrategy}
     * 
     * @return the current lock strategy; never null
     */
    LockStrategy getLockStrategy();

}
