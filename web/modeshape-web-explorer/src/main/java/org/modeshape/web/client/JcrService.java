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

import java.util.Collection;
import java.util.List;
import org.modeshape.web.shared.JcrAccessControlList;
import org.modeshape.web.shared.JcrNode;
import org.modeshape.web.shared.JcrNodeType;
import org.modeshape.web.shared.JcrPermission;
import org.modeshape.web.shared.JcrRepositoryDescriptor;
import org.modeshape.web.shared.ResultSet;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath( "jcr" )
public interface JcrService extends RemoteService {

    public String getRequestedURI();

    /**
     * Provides access for the given user to the given repository.
     * 
     * @param jndiName the jndi name of the repository.
     * @param userName the name of the user
     * @param password the user's password
     * @param workspace workspace to access
     * @return true in case of successful log in.
     * @throws RemoteException if there is a problem using the repository
     */
    public boolean login( String jndiName,
                          String userName,
                          String password,
                          String workspace ) throws RemoteException;

    /**
     * Provides access to the root node.
     * 
     * @return object representing root node.
     * @throws RemoteException if there is a problem using the repository
     */
    public JcrNode getRootNode() throws RemoteException;

    /**
     * Gets children for the node under specified path.
     * 
     * @param path the path to the node
     * @return children nodes.
     * @throws RemoteException if there is a problem using the repository
     */
    public List<JcrNode> childNodes( String path ) throws RemoteException;

    /**
     * Gets repository capabilities.
     * 
     * @return description of the repository capabilities.
     */
    public JcrRepositoryDescriptor repositoryInfo();

    /**
     * Gets all registered node types.
     * 
     * @return the node types
     * @throws RemoteException if there is a problem using the repository
     */
    public Collection<JcrNodeType> nodeTypes() throws RemoteException;

    /**
     * Gets supported query languages.
     * 
     * @return language names
     */
    public String[] supportedQueryLanguages();

    /**
     * Executes query.
     * 
     * @param text the query text.
     * @param lang query language name
     * @return Query result
     */
    public ResultSet query( String text,
                            String lang );

    /**
     * Adds new node.
     * 
     * @param path the path to the parent node
     * @param name the name of node to add
     * @param primaryType the primary type of the node to add.
     * @throws RemoteException if there is a problem using the repository
     */
    public void addNode( String path,
                         String name,
                         String primaryType ) throws RemoteException;

    /**
     * Deletes node.
     * 
     * @param path the pass to the node to be deleted.
     * @throws RemoteException if there is a problem using the repository
     */
    public void removeNode( String path ) throws RemoteException;

    /**
     * Adds mixin to the node.
     * 
     * @param path the path to the node
     * @param mixin mixin to add
     * @throws RemoteException Any exception on the server side
     */
    public void addMixin( String path,
                          String mixin ) throws RemoteException;

    /**
     * Removes mixin from the given node.
     * 
     * @param path the path to the node
     * @param mixin mixin to remove
     * @throws RemoteException any server side exception.
     */
    public void removeMixin( String path,
                             String mixin ) throws RemoteException;

    /**
     * Set's property value.
     * 
     * @param path the path to the node.
     * @param name the name of the property to add.
     * @param value the text representation of the value
     * @throws RemoteException if there is a problem using the repository
     */
    public void setProperty( String path,
                             String name,
                             String value ) throws RemoteException;

    public void updateAccessList( String path,
                                  JcrAccessControlList acl ) throws RemoteException;

    /**
     * Creates empty access list for given principal.
     * 
     * @param path the path to the node.
     * @param principal the principal name
     * @throws RemoteException
     */
    public void addAccessList( String path,
                               String principal ) throws RemoteException;

    /**
     * Modifies access control list.
     * 
     * @param path the path to the node
     * @param principal name of the principal
     * @param permissions list of permissions.
     * @throws RemoteException if there is a problem using the repository
     */
    public void updateAccessList( String path,
                                  String principal,
                                  JcrPermission[] permissions ) throws RemoteException;

    /**
     * Removes access list for the principal.
     * 
     * @param path the path to the node.
     * @param principal the name of the principal.
     * @throws RemoteException
     */
    public void removeAccessList( String path,
                                  String principal ) throws RemoteException;

    /**
     * Reads list of primary types.
     * 
     * @param allowAbstract true if allow to load abstract node types.
     * @return list of type names.
     * @throws RemoteException
     */
    public String[] getPrimaryTypes( boolean allowAbstract ) throws RemoteException;

    /**
     * Reads list of mixin types.
     * 
     * @param allowAbstract true if allow to load abstract node types.
     * @return list of type names.
     * @throws RemoteException
     */
    public String[] getMixinTypes( boolean allowAbstract ) throws RemoteException;

    /**
     * Saves changes in the current session.
     * 
     * @throws RemoteException if there is a problem using the repository
     */
    public void save() throws RemoteException;
}
