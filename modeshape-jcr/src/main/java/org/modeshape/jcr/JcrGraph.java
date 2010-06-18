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
package org.modeshape.jcr;

import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.RepositorySourceException;

/**
 * A special subclass of {@link Graph} that allows the {@link ExecutionContext} to be redefined for the graph.
 */
class JcrGraph extends Graph {

    /**
     * Create a graph instance that uses the supplied repository and {@link ExecutionContext context}.
     * 
     * @param sourceName the name of the source that should be used
     * @param connectionFactory the factory of repository connections
     * @param context the context in which all executions should be performed
     * @return the new graph
     * @throws IllegalArgumentException if the source or context parameters are null
     * @throws RepositorySourceException if a source with the supplied name does not exist
     */
    public static JcrGraph create( String sourceName,
                                   RepositoryConnectionFactory connectionFactory,
                                   ExecutionContext context ) {
        return new JcrGraph(sourceName, connectionFactory, context);
    }

    /**
     * Create a graph instance that uses the supplied {@link RepositoryConnection} and {@link ExecutionContext context}.
     * 
     * @param connection the connection that should be used
     * @param context the context in which all executions should be performed
     * @return the new graph
     * @throws IllegalArgumentException if the connection or context parameters are null
     */
    public static JcrGraph create( final RepositoryConnection connection,
                                   ExecutionContext context ) {
        CheckArg.isNotNull(connection, "connection");
        final String connectorSourceName = connection.getSourceName();
        RepositoryConnectionFactory connectionFactory = new RepositoryConnectionFactory() {
            public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
                if (connectorSourceName.equals(sourceName)) return connection;
                return null;
            }
        };
        return new JcrGraph(connectorSourceName, connectionFactory, context);
    }

    /**
     * Create a graph instance that uses the supplied {@link RepositoryConnection} and {@link ExecutionContext context}.
     * 
     * @param source the source that should be used
     * @param context the context in which all executions should be performed
     * @return the new graph
     * @throws IllegalArgumentException if the connection or context parameters are null
     */
    public static JcrGraph create( final RepositorySource source,
                                   ExecutionContext context ) {
        CheckArg.isNotNull(source, "source");
        final String connectorSourceName = source.getName();
        RepositoryConnectionFactory connectionFactory = new RepositoryConnectionFactory() {
            public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
                if (connectorSourceName.equals(sourceName)) return source.getConnection();
                return null;
            }
        };
        return new JcrGraph(connectorSourceName, connectionFactory, context);
    }

    private ExecutionContext context;

    /**
     * @param sourceName
     * @param connectionFactory
     * @param context
     */
    protected JcrGraph( String sourceName,
                        RepositoryConnectionFactory connectionFactory,
                        ExecutionContext context ) {
        super(sourceName, connectionFactory, context);
        this.context = super.getContext();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.Graph#getContext()
     */
    @Override
    public ExecutionContext getContext() {
        return context;
    }

    /**
     * @param context Sets context to the specified value.
     */
    void setContext( ExecutionContext context ) {
        this.context = context;
    }

}
