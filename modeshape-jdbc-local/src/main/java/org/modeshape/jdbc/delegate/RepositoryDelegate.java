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
package org.modeshape.jdbc.delegate;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Set;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.QueryResult;
import org.modeshape.jdbc.DriverInfo;

/**
 * Represents the communication interface thru which the JDBC logic will obtain a connection and issue commands to the Jcr layer.
 */

public interface RepositoryDelegate {

    /**
     * Call to get the connection information.
     * 
     * @return ConnectionInfo
     */
    ConnectionInfo getConnectionInfo();

    /**
     * Called when the {@link Statement} the is closed. This enables the underlying connection to the JcrRepository remain open
     * until the statement is finished using it.
     */
    void closeStatement();

    /**
     * Call to close the delegate connection and any outstanding transactions will be closed.
     * 
     * @see java.sql.Connection#close()
     */
    void close();

    /**
     * Call to get {@link NodeType} based on specified name
     * 
     * @param name
     * @return NodeType
     * @throws RepositoryException
     */
    NodeType nodeType( String name ) throws RepositoryException;

    /**
     * Call to get all the {@link NodeType}s defined.
     * 
     * @return List of all the node types.
     * @throws RepositoryException
     */
    Collection<NodeType> nodeTypes() throws RepositoryException;

    /**
     * Call to execute the <code>query</code> based on the specified JCR language.
     * 
     * @param query is the query expression to execute
     * @param language is the JCR language the <code>query</code> should be executed based on.
     * @return QueryResult is the JCR query result
     * @throws RepositoryException
     */
    QueryResult execute( String query,
                         String language ) throws RepositoryException;

    /**
     * Generate the plan for the <code>query</code> based on the specified JCR language.
     * 
     * @param query is the query expression to execute
     * @param language is the JCR language the <code>query</code> should be executed based on.
     * @return the string representation of the query plan
     * @throws RepositoryException
     */
    String explain( String query,
                    String language ) throws RepositoryException;

    /**
     * Call to create the connection based on the implementation of this interface.
     * 
     * @param info the driver information
     * @return Connection
     * @throws SQLException
     * @see java.sql.Driver#connect(String, java.util.Properties)
     */
    Connection createConnection( DriverInfo info ) throws SQLException;

    /**
     * @see java.sql.Connection#commit()
     * @throws RepositoryException
     */

    void commit() throws RepositoryException;

    /**
     * @see java.sql.Connection#rollback()
     * @throws RepositoryException
     */
    void rollback() throws RepositoryException;

    /**
     * @see java.sql.Connection#isValid(int)
     * @param timeout
     * @return boolean indicating if timeout is valid
     * @throws RepositoryException
     */
    boolean isValid( int timeout ) throws RepositoryException;

    /**
     * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
     * @param iface
     * @return boolean
     */
    boolean isWrapperFor( Class<?> iface );

    /**
     * @see java.sql.Wrapper#unwrap(java.lang.Class)
     * @param iface
     * @param <T>
     * @return <T> T
     * @throws SQLException
     */
    <T> T unwrap( Class<T> iface ) throws SQLException;

    /**
     * Called to get all the repository names currently available in the ModeShapeEngine.
     *
     * @return Set<String> of repository names
     * @throws RepositoryException
     */
    Set<String> getRepositoryNames() throws RepositoryException;

    /**
     * Returns the value for the requested <code>descriptorKey</code>
     * 
     * @param descriptorKey
     * @return String descriptor value
     */
    String getDescriptor( String descriptorKey );

}
