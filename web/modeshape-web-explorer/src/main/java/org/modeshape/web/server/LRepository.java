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

import javax.jcr.Repository;
import javax.jcr.Session;
import org.modeshape.web.client.RemoteException;

/**
 *
 * @author kulikov
 */
public interface LRepository {
    /**
     * Name of the repository.
     * 
     * @return 
     */
    public String name();
    

    /**
     * Provides access to the original repository.
     * 
     * @return original JCR repository;
     */
    public Repository repository();
    
    /**
     * Gets list of available workspaces.
     * 
     * @return 
     */
    public String[] getWorkspaces();
    
    /**
     * Gets session to the given workspace.
     * 
     * @param workspace the name of the workspace.
     * @return jcr session object.
     */
    public Session session(String workspace) throws RemoteException;
    
    
    public void backup(String workspace, String name) throws RemoteException;
    public void restore(String workspace, String name) throws RemoteException;
    
}
