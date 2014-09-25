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
package org.modeshape.web.server;

import java.io.Serializable;
import java.util.Collection;
import org.modeshape.web.client.RemoteException;
import org.modeshape.web.shared.RepositoryName;

/**
 * @author kulikov
 */
public interface Connector extends Serializable {
    /**
     * Logs in with given credentials.
     * 
     * @param username the user name
     * @param password user's password.
     */
    public void login( String username, String password );

    /**
     * Gets name of user currently logged in.
     * 
     * @return user name or null if not logged yet.
     */
    public String userName();

    /**
     * Gets list of all available repositories.
     * 
     * @return the collection of repository names
     */
    public Collection<RepositoryName> getRepositories();

    /**
     * Searches repository with given name.
     * 
     * @param name the name of the repository to search.
     * @return repository instance or null if not found.
     * @throws RemoteException if there is an error getting the repository
     */
    public LRepository find( String name ) throws RemoteException;

    public Collection<RepositoryName> search( String name ) throws RemoteException;

}
