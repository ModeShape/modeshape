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

import java.util.Collection;
import javax.jcr.Credentials;
import org.modeshape.web.client.RemoteException;
import org.modeshape.web.shared.RepositoryName;

/**
 *
 * @author kulikov
 */
public interface Connector {
    /**
     * Logs in with given credentials.
     * 
     * @param creds 
     */
    public void login(Credentials creds);
    
    /**
     * Gets name of user currently logged in.
     * 
     * @return user name or null if not logged yet.
     */
    public String userName();
    
    /**
     * Gets list of all available repositories.
     * 
     * @return 
     */
    public Collection<RepositoryName> getRepositories();
    
    /**
     * Searches repository with given name.
     * 
     * @param name the name of the repository to search.
     * @return repository instance or null if not found.
     */
    public LRepository find(String name) throws RemoteException;
    
    public Collection<RepositoryName> search(String name) throws RemoteException;
    
}
