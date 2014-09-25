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
package org.modeshape.web.client;

import java.util.Collection;
import org.modeshape.web.shared.JcrAccessControlList;
import org.modeshape.web.shared.JcrNode;
import org.modeshape.web.shared.JcrNodeType;
import org.modeshape.web.shared.JcrPermission;
import org.modeshape.web.shared.JcrRepositoryDescriptor;
import org.modeshape.web.shared.ResultSet;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import org.modeshape.web.shared.RepositoryName;

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

    public JcrNode node(String repository, String workspace, String path) throws RemoteException;

    /**
     * Gets repository capabilities.
     *
     *@param repository the repository
     * @return description of the repository capabilities.
     */
    public JcrRepositoryDescriptor repositoryInfo(String repository);

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
     * @throws RemoteException if there is a problem using the repository
     */
    public void addNode(String repository,
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

    /**
     * Set's property value.
     *
     * @param repository
     * @param workspace
     * @param path the path to the node.
     * @param name the name of the property to add.
     * @param value the text representation of the value
     * @throws RemoteException if there is a problem using the repository
     */
    public void setProperty(String repository, String workspace, String path,
            String name,
            String value) throws RemoteException;

    public void updateAccessList(String repository, String workspace, String path,
            JcrAccessControlList acl) throws RemoteException;

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

    
    public void updateAccessList(String repository, String workspace, String path, String principal,
            JcrPermission permission, boolean enabled) throws RemoteException;

    /**
     * Reads list of primary types.
     *
     * @param repository
     * @param workspace
     * @param allowAbstract true if allow to load abstract node types.
     * @return list of type names.
     * @throws RemoteException
     */
    public String[] getPrimaryTypes(String repository, String workspace, boolean allowAbstract) throws RemoteException;

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
    
    public void backup(String repository, String name) throws RemoteException;
    public void restore(String repository, String name) throws RemoteException;
    
    public void export(String repository, String workspace, String path, String location, boolean skipBinary, boolean noRecurse) 
            throws RemoteException;
    public void importXML(String repository, String workspace, String path, String location, int option) 
            throws RemoteException;
}
