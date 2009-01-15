/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.connector;

import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositorySourceException;

/**
 * An operation that runs against a repository connection.
 * 
 * @author Randall Hauch
 * @param <T> the type of result returned by the client
 */
public interface RepositoryOperation<T> {

    /**
     * Get the name of this operation.
     * 
     * @return the operation's name
     */
    String getName();

    /**
     * Run the operation using the supplied connection.
     * 
     * @param context the environment in which this operation is executing; may not be null
     * @param connection the connection; may not be null
     * @return the result of the operation
     * @throws RepositorySourceException if there is a problem with the connection
     * @throws InterruptedException if this thread was interrupted
     */
    T run( ExecutionContext context,
           RepositoryConnection connection ) throws RepositorySourceException, InterruptedException;

    /**
     * A factory interface for creating repository operations.
     * 
     * @param <T> the type of result for the operations
     * @author Randall Hauch
     */
    public static interface Factory<T> {

        /**
         * Create a repository operation that returns the result of type T.
         * 
         * @return the operation
         */
        RepositoryOperation<T> create();
    }
}
