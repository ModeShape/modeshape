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
/**
 * Repositories may either use the internal locking capabilities of their underlying source to support proper read and write isolation, or
 * they may leverage the framework provided in this package.
 * <p/>
 * Repository locking behavior can vary on two different dimensions: provider and strategy.  The lock provider determines what underlying locking
 * mechanism used to implement the locking.  A {@link JvmLockProvider lock provider based on Java locks} is provided by ModeShape.  Other potential
 * locking mechanisms include lock providers based on file locking or JGroups communication.  The lock strategy determines the granularity of the locks and currently
 * ranges from providing shared an exclusive locks {@link WorkspaceLockStrategy at the workspace level} to {@link RepositoryLockStrategy repository-level locks} that
 * mirror current locking behavior in most ModeShape repositories to {@link NoLockStrategy ignoring locks altogether}.  Obviously,
 * using no locks at all is only safe for read-only repositories.
 * <p/>
 * Repository implementations that wish to leverage this locking behavior should have their {@link org.modeshape.graph.connector.RepositorySource} implementation implement the
 * {@link LockManagingRepositorySource} interface.  The actual implementation of these methods should generally be delegated to the {@link RepositorySourceLockManager} class.    
 * 
 */

package org.modeshape.graph.connector.base.lock;

