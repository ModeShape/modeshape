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
package org.modeshape.jdbc.delegate;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.QueryResult;


/**
 *	Represents the communication interface thru which the JDBC logic will obtain a connection and issue commands to the Jcr layer.
 */

public interface RepositoryDelegate {
    
    /**
     * Call to get the connection information.
     * @return ConnectionInfo
     */
    ConnectionInfo getConnectionInfo();
    
    /**
     * Called when the {@link Statement} the is closed.  This enables the underlying connection to the
     * JcrRepository remain open until the statement is finished using it.
     */
    void closeStatement();
    
    /**
     * Call to close the delegate connection and any outstanding
     * transactions will be closed.
     * 
     * @see java.sql.Connection#close()
     */
    void close();
   

    /**
     * Call to get {@link NodeType} based on specified name
     * @param name
     * @return NodeType
     * @throws RepositoryException
     */
    NodeType nodeType( String name ) throws RepositoryException;
    
    /**
     * Call to get all the {@link NodeType}s defined.
     * @return List of all the node types.
     * @throws RepositoryException
     */
    List<NodeType> nodeTypes( ) throws RepositoryException;

   
    /**
     * Call to execute the sql <code>query</code> based on the specified Jcr language.
     * 
     * @param query is the sql query to execute
     * @param language is the JCR language the <code>query</code> should be executed based on.
     * @return QueryResult is the JCR query result
     * @throws RepositoryException 
     */
    QueryResult execute(String query, String language) throws RepositoryException;
    

    /**
     * Call to create the connection based on the implementation of this interface.
     * @return Connection
     * @throws SQLException
     * 
     * @see java.sql.Driver#connect(String, java.util.Properties)
     */
    Connection createConnection() throws SQLException;
    
    /**
     * 
     * @see java.sql.Connection#commit()
     * 
     * @throws RepositoryException 
     */

    void commit() throws RepositoryException;
    
    /**
     * 
     * @see java.sql.Connection#rollback()
     * 
     * @throws RepositoryException 
     */
    void rollback() throws RepositoryException;
    
    /**
     * 
     * @see java.sql.Connection#isValid(int)
     * 
     * @param timeout 
     * @return boolean indicating if timeout is valid
     * @throws RepositoryException 

     */
    boolean isValid( int timeout ) throws RepositoryException ;
    
    
    /**
     * 
     * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
     *
     * @param iface 
     * @return boolean
     */
    boolean isWrapperFor( Class<?> iface ) ;
    
    /**
     * 
     * @see java.sql.Wrapper#unwrap(java.lang.Class)
     * 
     * @param iface 
     * @param <T> 
     * @return <T> T
     * @throws SQLException 

     */
    <T> T unwrap( Class<T> iface ) throws SQLException;
   
    /**
     * Called to get all the repository names currently available in the JcrEngine.
     * @return Set<String> of repository names
     * @throws RepositoryException
     */
    Set<String> getRepositoryNames() throws RepositoryException;
    
    /**
     * Returns the value for the requested <code>descriptorKey</code>
     * @param descriptorKey 
     * @return String descriptor value
     */
    String getDescriptor(String descriptorKey);

}
