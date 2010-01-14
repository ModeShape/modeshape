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
package org.modeshape.graph.connector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.LogContext;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryConnectionPool;
import org.modeshape.graph.connector.RepositorySourceException;

/**
 * @author Randall Hauch
 */
public class RepositoryOperations {

    /**
     * Call the supplied operation, using a connection from this pool.
     * 
     * @param <T> the return type for the operation
     * @param context the context in which the operation is to execute; may not be null
     * @param pool the pool to use
     * @param operation the operation to be run using a new connection obtained from the factory
     * @return the results from the operation
     * @throws RepositorySourceException if there was an error obtaining the new connection
     * @throws InterruptedException if the thread was interrupted during the operation
     * @throws IllegalArgumentException if the operation is null
     * @see #createCallable(ExecutionContext, RepositoryConnectionPool, RepositoryOperation)
     * @see #createCallables(ExecutionContext, RepositoryConnectionPool, Iterable)
     * @see #createCallables(ExecutionContext, RepositoryConnectionPool, RepositoryOperation...)
     */
    public static <T> T call( ExecutionContext context,
                              RepositoryConnectionPool pool,
                              RepositoryOperation<T> operation ) throws RepositorySourceException, InterruptedException {
        CheckArg.isNotNull(operation, "repository operation");
        // Get a connection ...
        T result = null;
        LogContext.set("context", operation.getName());
        RepositoryConnection conn = pool.getConnection();
        try {
            // And run the client with the connection ...
            result = operation.run(context, conn);
        } finally {
            conn.close();
        }
        LogContext.clear();
        return result;
    }

    /**
     * Return a callable object that, when run, performs the supplied repository operation against a connection obtained from the
     * supplied factory.
     * 
     * @param <T> the return type for the operation
     * @param context the context in which the operation is to execute; may not be null
     * @param pool the pool to use
     * @param operation the operation to be run using a new connection obtained from the factory
     * @return the callable
     * @see #call(ExecutionContext, RepositoryConnectionPool, RepositoryOperation)
     * @see #createCallables(ExecutionContext, RepositoryConnectionPool, Iterable)
     * @see #createCallables(ExecutionContext, RepositoryConnectionPool, RepositoryOperation...)
     */
    public static <T> Callable<T> createCallable( final ExecutionContext context,
                                                  final RepositoryConnectionPool pool,
                                                  final RepositoryOperation<T> operation ) {
        CheckArg.isNotNull(operation, "repository operation");
        return new Callable<T>() {

            /**
             * Execute by getting a connection from this pool, running the client, and return the connection to the pool.
             * 
             * @return the operation's result
             * @throws Exception
             */
            public T call() throws Exception {
                return RepositoryOperations.call(context, pool, operation);
            }
        };
    }

    /**
     * Return a collection of callable objects that, when run, perform the supplied repository operations against connections in
     * this pool.
     * 
     * @param <T> the return type for the operations
     * @param context the context in which the operation is to execute; may not be null
     * @param pool the pool to use
     * @param operations the operations to be run using connections from the factory
     * @return the collection of callables
     * @see #call(ExecutionContext, RepositoryConnectionPool, RepositoryOperation)
     * @see #createCallable(ExecutionContext, RepositoryConnectionPool, RepositoryOperation)
     * @see #createCallables(ExecutionContext, RepositoryConnectionPool, Iterable)
     */
    public static <T> List<Callable<T>> createCallables( final ExecutionContext context,
                                                         final RepositoryConnectionPool pool,
                                                         final RepositoryOperation<T>... operations ) {
        List<Callable<T>> callables = new ArrayList<Callable<T>>();
        for (final RepositoryOperation<T> operation : operations) {
            callables.add(createCallable(context, pool, operation));
        }
        return callables;
    }

    /**
     * Return a collection of callable objects that, when run, perform the supplied repository operations against connections in
     * this pool.
     * 
     * @param <T> the return type for the operations
     * @param context the context in which the operation is to execute; may not be null
     * @param pool the pool to use
     * @param operations the operations to be run using connections from the factory
     * @return the collection of callables
     * @see #call(ExecutionContext, RepositoryConnectionPool, RepositoryOperation)
     * @see #createCallable(ExecutionContext, RepositoryConnectionPool, RepositoryOperation)
     * @see #createCallables(ExecutionContext, RepositoryConnectionPool, RepositoryOperation...)
     */
    public static <T> List<Callable<T>> createCallables( final ExecutionContext context,
                                                         final RepositoryConnectionPool pool,
                                                         Iterable<RepositoryOperation<T>> operations ) {
        List<Callable<T>> callables = new ArrayList<Callable<T>>();
        for (final RepositoryOperation<T> operation : operations) {
            callables.add(createCallable(context, pool, operation));
        }
        return callables;
    }

    /**
     * Return a collection of callable objects that, when run, perform the supplied repository operations against connections in
     * this pool.
     * 
     * @param <T> the return type for the operations
     * @param context the context in which the operation is to execute; may not be null
     * @param pool the pool to use
     * @param operations the operations to be run using connections from the factory
     * @return the collection of callables
     * @see #call(ExecutionContext, RepositoryConnectionPool, RepositoryOperation)
     * @see #createCallable(ExecutionContext, RepositoryConnectionPool, RepositoryOperation)
     * @see #createCallables(ExecutionContext, RepositoryConnectionPool, RepositoryOperation...)
     */
    public static <T> List<Callable<T>> createCallables( final ExecutionContext context,
                                                         final RepositoryConnectionPool pool,
                                                         Iterator<RepositoryOperation<T>> operations ) {
        List<Callable<T>> callables = new ArrayList<Callable<T>>();
        while (operations.hasNext()) {
            final RepositoryOperation<T> operation = operations.next();
            callables.add(createCallable(context, pool, operation));
        }
        return callables;
    }

}
