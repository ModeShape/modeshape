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
package org.modeshape.web.server;

import java.io.Serializable;
import java.util.Collection;
import javax.servlet.ServletContext;
import org.modeshape.web.shared.RemoteException;
import org.modeshape.web.shared.RepositoryName;

/**
 * @author kulikov
 */
public interface Connector extends Serializable {
    /**
     * Starts this connector using given context.
     * 
     * @param context the context 
     * @throws RemoteException if there is an error with the connector
     */
    public void start(ServletContext context) throws RemoteException;
    
    /**
     * Logs in with given credentials.
     * 
     * @param username the user name
     * @param password user's password.
     */
    public void login( String username, String password ) throws RemoteException;

    /**
     * Logs out.
     */
    public void logout();
    
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
