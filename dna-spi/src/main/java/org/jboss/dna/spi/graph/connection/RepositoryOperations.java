/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.spi.graph.connection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.common.util.LogContext;

/**
 * @author Randall Hauch
 */
public class RepositoryOperations {

    /**
     * Call the supplied operation, using a connection from this pool.
     * 
     * @param <T> the return type for the operation
     * @param connectionFactory the factory for the connection to use
     * @param operation the operation to be run using a new connection obtained from the factory
     * @return the results from the operation
     * @throws RepositorySourceException if there was an error obtaining the new connection
     * @throws InterruptedException if the thread was interrupted during the operation
     * @throws IllegalArgumentException if the operation is null
     * @see #createCallable(RepositoryConnectionFactory, RepositoryOperation)
     * @see #createCallables(RepositoryConnectionFactory, Iterable)
     * @see #createCallables(RepositoryConnectionFactory, RepositoryOperation...)
     */
    public static <T> T call( RepositoryConnectionFactory connectionFactory, RepositoryOperation<T> operation ) throws RepositorySourceException, InterruptedException {
        ArgCheck.isNotNull(operation, "repository operation");
        // Get a connection ...
        T result = null;
        LogContext.set("context", operation.getName());
        RepositoryConnection conn = connectionFactory.getConnection();
        try {
            // And run the client with the connection ...
            result = operation.run(conn);
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
     * @param connectionFactory the factory for the connection to use
     * @param operation the operation to be run using a new connection obtained from the factory
     * @return the callable
     * @see #call(RepositoryConnectionFactory, RepositoryOperation)
     * @see #createCallables(RepositoryConnectionFactory, Iterable)
     * @see #createCallables(RepositoryConnectionFactory, RepositoryOperation...)
     */
    public static <T> Callable<T> createCallable( final RepositoryConnectionFactory connectionFactory, final RepositoryOperation<T> operation ) {
        ArgCheck.isNotNull(operation, "repository operation");
        return new Callable<T>() {

            /**
             * Execute by getting a connection from this pool, running the client, and return the connection to the pool.
             * 
             * @return the operation's result
             * @throws Exception
             */
            public T call() throws Exception {
                return RepositoryOperations.call(connectionFactory, operation);
            }
        };
    }

    /**
     * Return a collection of callable objects that, when run, perform the supplied repository operations against connections in
     * this pool.
     * 
     * @param <T> the return type for the operations
     * @param connectionFactory the factory for the connection to use
     * @param operations the operations to be run using connections from the factory
     * @return the collection of callables
     * @see #call(RepositoryConnectionFactory, RepositoryOperation)
     * @see #createCallable(RepositoryConnectionFactory, RepositoryOperation)
     * @see #createCallables(RepositoryConnectionFactory, Iterable)
     */
    public static <T> List<Callable<T>> createCallables( final RepositoryConnectionFactory connectionFactory, final RepositoryOperation<T>... operations ) {
        List<Callable<T>> callables = new ArrayList<Callable<T>>();
        for (final RepositoryOperation<T> operation : operations) {
            callables.add(createCallable(connectionFactory, operation));
        }
        return callables;
    }

    /**
     * Return a collection of callable objects that, when run, perform the supplied repository operations against connections in
     * this pool.
     * 
     * @param <T> the return type for the operations
     * @param connectionFactory the factory for the connection to use
     * @param operations the operations to be run using connections from the factory
     * @return the collection of callables
     * @see #call(RepositoryConnectionFactory, RepositoryOperation)
     * @see #createCallable(RepositoryConnectionFactory, RepositoryOperation)
     * @see #createCallables(RepositoryConnectionFactory, RepositoryOperation...)
     */
    public static <T> List<Callable<T>> createCallables( final RepositoryConnectionFactory connectionFactory, Iterable<RepositoryOperation<T>> operations ) {
        List<Callable<T>> callables = new ArrayList<Callable<T>>();
        for (final RepositoryOperation<T> operation : operations) {
            callables.add(createCallable(connectionFactory, operation));
        }
        return callables;
    }

    /**
     * Return a collection of callable objects that, when run, perform the supplied repository operations against connections in
     * this pool.
     * 
     * @param <T> the return type for the operations
     * @param connectionFactory the factory for the connection to use
     * @param operations the operations to be run using connections from the factory
     * @return the collection of callables
     * @see #call(RepositoryConnectionFactory, RepositoryOperation)
     * @see #createCallable(RepositoryConnectionFactory, RepositoryOperation)
     * @see #createCallables(RepositoryConnectionFactory, RepositoryOperation...)
     */
    public static <T> List<Callable<T>> createCallables( final RepositoryConnectionFactory connectionFactory, Iterator<RepositoryOperation<T>> operations ) {
        List<Callable<T>> callables = new ArrayList<Callable<T>>();
        while (operations.hasNext()) {
            final RepositoryOperation<T> operation = operations.next();
            callables.add(createCallable(connectionFactory, operation));
        }
        return callables;
    }

}
