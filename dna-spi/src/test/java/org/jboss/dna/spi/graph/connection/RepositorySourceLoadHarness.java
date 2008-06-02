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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.jboss.dna.common.i18n.MockI18n;
import org.jboss.dna.common.util.Logger;

/**
 * A test harness for using repository connections under load.
 * @author Randall Hauch
 */
public class RepositorySourceLoadHarness {

    public static <T> List<T> runLoadTest( RepositoryConnectionFactory connectionFactory, int numConnectionsInPool, int numClients, long maxTime, TimeUnit maxTimeUnit,
                                           RepositoryOperation.Factory<T> clientFactory ) throws InterruptedException, ExecutionException {
        // Create the clients ...
        Collection<RepositoryOperation<T>> clients = new ArrayList<RepositoryOperation<T>>();
        for (int i = 0; i != numClients; ++i) {
            clients.add(clientFactory.create());
        }

        // and run the test ...
        return runLoadTest(connectionFactory, numConnectionsInPool, maxTime, maxTimeUnit, clients);
    }

    public static <T> List<T> runLoadTest( RepositoryConnectionFactory connectionFactory, int numConnectionsInPool, long maxTime, TimeUnit maxTimeUnit, RepositoryOperation<T>... clients )
        throws InterruptedException, ExecutionException {
        // Create the client collection ...
        Collection<RepositoryOperation<T>> clientCollection = new ArrayList<RepositoryOperation<T>>();
        for (RepositoryOperation<T> client : clients) {
            if (client != null) clientCollection.add(client);
        }
        // and run the test ...
        return runLoadTest(connectionFactory, numConnectionsInPool, maxTime, maxTimeUnit, clientCollection);
    }

    public static <T> List<T> runLoadTest( RepositoryConnectionFactory connectionFactory, int numConnectionsInPool, long maxTime, TimeUnit maxTimeUnit, Collection<RepositoryOperation<T>> clients )
        throws InterruptedException, ExecutionException {
        assert connectionFactory != null;
        assert numConnectionsInPool > 0;
        assert clients != null;
        assert clients.size() > 0;

        // Create a connection pool ...
        final RepositoryConnectionPool connectionPool = new RepositoryConnectionPool(connectionFactory, numConnectionsInPool, numConnectionsInPool, 10, TimeUnit.SECONDS);

        // Create an Executor Service, using a thread factory that makes the first 'n' thread all wait for each other ...
        final ThreadFactory threadFactory = new TestThreadFactory(clients.size(), connectionPool);
        final ExecutorService clientPool = Executors.newFixedThreadPool(clients.size(), threadFactory);

        try {
            // Wrap each client by a callable and by another that uses a latch ...
            List<Callable<T>> callables = connectionPool.callables(clients);

            // Run the tests ...
            List<Future<T>> futures = clientPool.invokeAll(callables, maxTime, maxTimeUnit);

            // Whether or not all clients completed, process the results ...
            List<T> results = new ArrayList<T>();
            for (Future<T> future : futures) {
                if (future.isDone() && !future.isCancelled()) {
                    // Record the results ...
                    results.add(future.get());
                } else {
                    // Record the results as null
                    results.add(null);
                    // Cancell any operation that is not completed
                    future.cancel(true);
                }
            }
            // Return the results ...
            return results;
        } finally {
            try {
                // Shut down the pool of clients ...
                clientPool.shutdown();
                if (!clientPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    String msg = "Unable to shutdown clients after 5 seconds";
                    Logger.getLogger(RepositorySourceLoadHarness.class).error(MockI18n.passthrough, msg);
                }
            } finally {
                // Shut down the connections ...
                connectionPool.shutdown();
                if (!connectionPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    String msg = "Unable to shutdown connections after 5 seconds";
                    Logger.getLogger(RepositorySourceLoadHarness.class).error(MockI18n.passthrough, msg);
                }
            }
        }

    }

    /**
     * A thread factory that makes an initial set of threads wait until all of those threads are created and ready. This is useful
     * in testing to ensure that the first threads created don't get a jump start.
     * @author Randall Hauch
     */
    protected static class TestThreadFactory implements ThreadFactory {

        protected final CountDownLatch latch;
        protected final RepositoryConnectionPool pool;

        public TestThreadFactory( int numberOfThreadsToWait, RepositoryConnectionPool pool ) {
            this.latch = new CountDownLatch(numberOfThreadsToWait);
            this.pool = pool;
        }

        /**
         * {@inheritDoc}
         */
        public Thread newThread( Runnable runnable ) {
            return new Thread(runnable) {

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void run() {
                    try {
                        // Count down the thread count (if 0, this doesn't do anything)
                        latch.countDown();
                        // Wait for all threads to reach this point ...
                        latch.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    // Check that the number of connections-in-use is smaller than the maximum pool size
                    if (pool != null) assert pool.getInUseCount() <= pool.getMaximumPoolSize();
                    super.run();
                }
            };
        }
    }

}
