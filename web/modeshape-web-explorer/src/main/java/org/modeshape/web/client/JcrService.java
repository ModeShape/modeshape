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
package org.modeshape.web.client;

import org.modeshape.web.shared.RemoteException;
import java.util.Collection;
import org.modeshape.web.shared.JcrNode;
import org.modeshape.web.shared.JcrNodeType;
import org.modeshape.web.shared.JcrPermission;
import org.modeshape.web.shared.JcrRepositoryDescriptor;
import org.modeshape.web.shared.ResultSet;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import java.util.Date;
import org.modeshape.web.shared.BackupParams;
import org.modeshape.web.shared.RepositoryName;
import org.modeshape.web.shared.RestoreParams;
import org.modeshape.web.shared.Stats;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("jcr")
public interface JcrService extends RemoteService {

    /**
     * Initial URI requested by user.
     * 
     * @return URI as text.
     */
    public String getRequestedURI();

    /**
     * Currently logged in user.
     * 
     * @return user's name of null if not logged in.
     * @throws RemoteException 
     */
    public String getUserName() throws RemoteException;
    
    /**
     * Lists all available repositories.
     *
     * @return collection of object representing repository.
     * @throws RemoteException if there is a problem communicating with a remote service
     */
    public Collection<RepositoryName> getRepositories() throws RemoteException;

    /**
     * Searches repositories with given criteria
     *
     * @param criteria the parameter for searching repository
     * @return collection of object representing repository.
     * @throws RemoteException if there is a problem communicating with a remote service
     */
    public Collection<RepositoryName> findRepositories(String criteria) throws RemoteException;
    
    /**
     * Enlists workspaces available for the given repository and user previously
     * logged in.
     * 
     * @param repositoryName the name of the repository.
     * @return names of the available workspaces or empty array.
     * @throws RemoteException 
     */
    public String[] getWorkspaces(String repositoryName) throws RemoteException;

    
    /**
     * Provides access for the given user to the given repository.
     *
     * @param userName the name of the user
     * @param password the user's password
     * @throws RemoteException if there is a problem using the repository
     */
    public void login(String userName, String password) throws RemoteException;

    /**
     * Logs out from all repositories.
     */
    public String logout();
    
    /**
     * Gets node at the given path.
     * 
     * @param repository
     * @param workspace
     * @param path
     * @return
     * @throws RemoteException 
     */
    public JcrNode node(String repository, String workspace, String path) throws RemoteException;

    /**
     * Gets set of child nodes for the node at the given path.
     * 
     * @param repository
     * @param workspace
     * @param path
     * @param index
     * @param count
     * @return
     * @throws RemoteException 
     */
    public Collection<JcrNode> childNodes(String repository, String workspace, String path, int index, int count) throws RemoteException;
    
    /**
     * Gets repository capabilities.
     *
     *@param repository the repository
     * @return description of the repository capabilities.
     */
    public JcrRepositoryDescriptor repositoryInfo(String repository) throws RemoteException;

    /**
     * Gets all registered node types.
     *
     * @param repository
     * @param workspace
     * @return the node types
     * @throws RemoteException if there is a problem using the repository
     */
    public Collection<JcrNodeType> nodeTypes(String repository, String workspace) throws RemoteException;

    /**
     * Gets supported query languages.
     *
     * @param repository
     * @param workspace
     * @return language names
     * @throws RemoteException if there is a problem using the repository
     */
    public String[] supportedQueryLanguages(String repository, String workspace) throws RemoteException;

    /**
     * Executes query.
     *
     * @param repository
     * @param workspace
     * @param text the query text.
     * @param lang query language name
     * @return Query result
     * @throws RemoteException if there is a problem using the repository
     */
    public ResultSet query(String repository, String workspace, String text,
            String lang) throws RemoteException;

    /**
     * Adds new node.
     *
     * @param repository
     * @param workspace
     * @param path the path to the parent node
     * @param name the name of node to add
     * @param primaryType the primary type of the node to add.
     * @return the node
     * @throws RemoteException if there is a problem using the repository
     */
    public JcrNode addNode(String repository,
            String workspace,
            String path,
            String name,
            String primaryType) throws RemoteException;


    /**
     * 
     * @param repository
     * @param workspace
     * @param path
     * @param name
     * @throws RemoteException 
     */
    public void renameNode(String repository, String workspace, String path, String name) throws RemoteException;
    
    /**
     * Deletes node.
     *
     * @param repository
     * @param workspace
     * @param path the pass to the node to be deleted.
     * @throws RemoteException if there is a problem using the repository
     */
    public void removeNode(String repository, String workspace, String path) throws RemoteException;

    /**
     * Adds mixin to the node.
     *
     * @param repository
     * @param workspace
     * @param path the path to the node
     * @param mixin mixin to add
     * @throws RemoteException Any exception on the server side
     */
    public void addMixin(String repository, String workspace, String path,
            String mixin) throws RemoteException;

    /**
     * Removes mixin from the given node.
     *
     * @param repository
     * @param workspace
     * @param path the path to the node
     * @param mixin mixin to remove
     * @throws RemoteException any server side exception.
     */
    public void removeMixin(String repository, String workspace, String path,
            String mixin) throws RemoteException;

    public void setProperty(JcrNode node, String name, String value) 
            throws RemoteException;

    public void setProperty(JcrNode node, String name, Boolean value) 
            throws RemoteException;

    public void setProperty(JcrNode node, String name, Date value) 
            throws RemoteException;

    
    /**
     * Creates empty access list for given principal.
     *
     * @param repository
     * @param workspace
     * @param path the path to the node.
     * @param principal the principal name
     * @throws RemoteException
     */
    public void addAccessList(String repository, String workspace,String path,
            String principal) throws RemoteException;

    
    /**
     * Deletes access list.
     * 
     * @param repository
     * @param workspace
     * @param path
     * @param principal
     * @throws RemoteException 
     */
    public void removeAccessList( String repository,
                               String workspace,
                               String path,
                               String principal ) throws RemoteException;
    
    public void updateAccessList(String repository, String workspace, String path, String principal,
            JcrPermission permission, boolean enabled) throws RemoteException;

    /**
     * Reads list of primary types.
     *
     * @param repository
     * @param workspace
     * @param superType 
     * @param allowAbstract true if allow to load abstract node types.
     * @return list of type names.
     * @throws RemoteException
     */
    public String[] getPrimaryTypes(String repository, String workspace, 
            String superType,
            boolean allowAbstract) throws RemoteException;

    /**
     * Reads list of mixin types.
     *
     * @param repository
     * @param workspace
     * @param allowAbstract true if allow to load abstract node types.
     * @return list of type names.
     * @throws RemoteException
     */
    public String[] getMixinTypes(String repository, String workspace,boolean allowAbstract) throws RemoteException;

    /**
     * Saves changes in the current session.
     *
     * @param repository
     * @param workspace
     * @throws RemoteException if there is a problem using the repository
     */
    public void save(String repository, String workspace) throws RemoteException;
    
    public void backup(String repository, String name, BackupParams params) throws RemoteException;
    public void restore(String repository, String name, RestoreParams params) throws RemoteException;
    
    public void export(String repository, String workspace, String path, String location, boolean skipBinary, boolean noRecurse) 
            throws RemoteException;
    public void importXML(String repository, String workspace, String path, String location, int option) 
            throws RemoteException;
    
    /**
     * Reload session to the given workspace and repository.
     * 
     * @param repository the repository name
     * @param workspace the workspace name
     * @param keepChanges true if session should keep modification and false 
     * otherwise
     * @throws RemoteException 
     */
    public void refreshSession(String repository, String workspace, 
        boolean keepChanges) throws RemoteException;
    
    /**
     * Obtains repository metrics.
     * 
     * @param repository
     * @param param value metric name
     * @param tu time window name
     * @return
     * @throws RemoteException 
     */
    public Collection<Stats> getValueStats(String repository, String param, String tu) throws RemoteException;

    /**
     * Obtains repository metrics.
     * 
     * @param repository
     * @param param duration metric name
     * @param tu time window name
     * @return
     * @throws RemoteException 
     */
    public Collection<Stats> getDurationStats(String repository, String param, String tu) throws RemoteException;
    
    /**
     * Gets names of available value metrics.
     * 
     * @return
     * @throws RemoteException 
     */
    public String[] getValueMetrics() throws RemoteException;
    /**
     * Gets names of available duration metrics.
     * 
     * @return
     * @throws RemoteException 
     */
    public String[] getDurationMetrics() throws RemoteException;
    
    
    /**
     * Gets possible time frames.
     * 
     * @return
     * @throws RemoteException 
     */
    public String[] getTimeUnits() throws RemoteException;
}
