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

import org.modeshape.web.shared.JcrNode;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import java.util.List;
import org.modeshape.web.shared.JcrRepositoryDescriptor;
import org.modeshape.web.shared.ResultSet;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("jcr")
public interface JcrService extends RemoteService {
    /**
     * Provides access for the given user to the given repository.
     * 
     * @param jndiName the jndi name of the repository.
     * @param userName the name of the user
     * @param password the user's password
     * @param workspace workspace to access
     * @return true in case of successful log in.
     */
    public boolean login(String jndiName, String userName, String password, 
            String workspace) throws RemoteException;
    
    /**
     * Provides access to the root node.
     * 
     * @return object representing root node.
     */
    public JcrNode getRootNode() throws RemoteException;
    
    /**
     * Gets children for the node under specified path.
     * 
     * @param path the path to the node
     * @return children nodes.
     */
    public List<JcrNode> childNodes(String path);

    /**
     * Gets repository capabilities.
     * 
     * @return description of the repository capabilities.
     */
    public JcrRepositoryDescriptor repositoryInfo();
    
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
    public ResultSet query(String text, String lang);
    
    /**
     * Adds new node.
     * 
     * @param path the path to the parent node
     * @param name the name of node to add
     * @param primaryType the primary type of the node to add.
     */
    public void addNode(String path, String name, String primaryType) throws RemoteException;
    
    /**
     * Deletes node.
     * 
     * @param path the pass to the node to be deleted.
     */
    public void removeNode(String path) throws RemoteException;
    
    /**
     * Adds mixin to the node.
     * 
     * @param path the path to the node
     * @param mixin mixin to add
     * @throws RemoteException Any exception on the server side
     */
    public void addMixin(String path, String mixin) throws RemoteException;
}
