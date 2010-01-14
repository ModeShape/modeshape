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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.modeshape.common.i18n.MockI18n;
import org.modeshape.common.util.Logger;
import org.modeshape.common.util.NamedThreadFactory;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.basic.RootPath;
import org.modeshape.graph.request.ReadNodeRequest;

/**
 * A test harness for using repository connections under load.
 * 
 * @author Randall Hauch
 */
public class RepositorySourceLoadHarness {

    public static Future<Integer> execute( RepositoryConnectionPool pool,
                                           ExecutionContext context,
                                           long maxTime,
                                           TimeUnit maxTimeUnit ) throws InterruptedException {
        int numTimes = 1;
        int numClients = 1;
        RepositoryOperation.Factory<Integer> operationFactory = RepositorySourceLoadHarness.createMultipleLoadOperationFactory(numTimes);
        List<Future<Integer>> results = runLoadTest(context, pool, numClients, maxTime, maxTimeUnit, operationFactory);
        return results.get(0);
    }

    public static <T> List<Future<T>> runLoadTest( ExecutionContext context,
                                                   RepositoryConnectionPool pool,
                                                   int numClients,
                                                   long maxTime,
                                                   TimeUnit maxTimeUnit,
                                                   RepositoryOperation.Factory<T> clientFactory ) throws InterruptedException {
        // Create the clients ...
        Collection<RepositoryOperation<T>> clients = new ArrayList<RepositoryOperation<T>>();
        for (int i = 0; i != numClients; ++i) {
            clients.add(clientFactory.create());
        }

        // and run the test ...
        return runLoadTest(context, pool, maxTime, maxTimeUnit, clients);
    }

    public static <T> List<Future<T>> runLoadTest( ExecutionContext context,
                                                   RepositoryConnectionPool pool,
                                                   long maxTime,
                                                   TimeUnit maxTimeUnit,
                                                   RepositoryOperation<T>... clients ) throws InterruptedException {
        // Create the client collection ...
        Collection<RepositoryOperation<T>> clientCollection = new ArrayList<RepositoryOperation<T>>();
        for (RepositoryOperation<T> client : clients) {
            if (client != null) clientCollection.add(client);
        }
        // and run the test ...
        return runLoadTest(context, pool, maxTime, maxTimeUnit, clientCollection);
    }

    public static <T> List<Future<T>> runLoadTest( ExecutionContext context,
                                                   RepositoryConnectionPool pool,
                                                   long maxTime,
                                                   TimeUnit maxTimeUnit,
                                                   Collection<RepositoryOperation<T>> clients ) throws InterruptedException {
        assert pool != null;
        assert clients != null;
        assert clients.size() > 0;

        // Create an Executor Service, using a thread factory that makes the first 'n' thread all wait for each other ...
        ExecutorService clientPool = null;
        if (clients.size() == 1) {
            ThreadFactory threadFactory = new NamedThreadFactory("load");
            clientPool = Executors.newSingleThreadExecutor(threadFactory);
        } else {
            final ThreadFactory threadFactory = new TestThreadFactory(clients.size());
            clientPool = Executors.newFixedThreadPool(clients.size(), threadFactory);
        }

        try {
            // Wrap each client by a callable and by another that uses a latch ...
            List<Callable<T>> callables = RepositoryOperations.createCallables(context, pool, clients);

            // Run the tests ...
            List<Future<T>> futures = clientPool.invokeAll(callables, maxTime, maxTimeUnit);
            return futures;
        } finally {
            // Shut down the pool of clients ...
            clientPool.shutdown();
            if (!clientPool.awaitTermination(5, TimeUnit.SECONDS)) {
                String msg = "Unable to shutdown clients after 5 seconds";
                Logger.getLogger(RepositorySourceLoadHarness.class).error(MockI18n.passthrough, msg);
            }
        }

    }

    /**
     * A thread factory that makes an initial set of threads wait until all of those threads are created and ready. This is useful
     * in testing to ensure that the first threads created don't get a jump start.
     * 
     * @author Randall Hauch
     */
    protected static class TestThreadFactory implements ThreadFactory {

        protected final int totalNumberOfThreads;
        protected final CountDownLatch latch;

        public TestThreadFactory( int numberOfThreadsToWait ) {
            this.latch = new CountDownLatch(numberOfThreadsToWait);
            this.totalNumberOfThreads = numberOfThreadsToWait;
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
                    if (totalNumberOfThreads > 1) {
                        // There are other threads, and we want to synchronize with them ...
                        try {
                            // Count down the number of threads that are to reach this point (if 0, this doesn't do anything)
                            latch.countDown();
                            // Wait for all threads to reach this point ...
                            latch.await();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    super.run();
                }
            };
        }
    }

    /**
     * Return an operation factory that produces {@link RepositoryOperation} instances that each call
     * {@link RepositoryConnection#execute(ExecutionContext, org.modeshape.graph.request.Request)} the supplied number of times,
     * intermixed with random math operations and {@link Thread#yield() yielding}.
     * 
     * @param callsPerOperation the number of <code>load</code> calls per RepositoryOperation
     * @return the factory
     */
    public static RepositoryOperation.Factory<Integer> createMultipleLoadOperationFactory( final int callsPerOperation ) {
        return new RepositoryOperation.Factory<Integer>() {

            public RepositoryOperation<Integer> create() {
                return new CallLoadMultipleTimes(callsPerOperation);
            }
        };
    }

    public static class CallLoadMultipleTimes implements RepositoryOperation<Integer> {

        private final int count;

        public CallLoadMultipleTimes( int count ) {
            Logger.getLogger(RepositorySourceLoadHarness.class).debug("Creating repository operation to call {0} times", count);
            this.count = count;
        }

        /**
         * {@inheritDoc}
         */
        public String getName() {
            return Thread.currentThread().getName() + "-CallLoadMultipleTimes";
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.connector.RepositoryOperation#run(org.modeshape.graph.ExecutionContext,
         *      org.modeshape.graph.connector.RepositoryConnection)
         */
        public Integer run( ExecutionContext context,
                            RepositoryConnection connection ) throws RepositorySourceException {
            Logger.getLogger(RepositorySourceLoadHarness.class).debug("Running {0} operation", this.getClass().getSimpleName());
            int total = count;
            for (int i = 0; i != count; ++i) {
                // Add two random numbers ...
                int int1 = random(this.hashCode() ^ (int)System.nanoTime() * i);
                if (i % 2 == 0) {
                    Thread.yield();
                }
                connection.execute(context, new ReadNodeRequest(Location.create(RootPath.INSTANCE), "workspace1"));
                int int2 = random(this.hashCode() ^ (int)System.nanoTime() + i);
                total += Math.min(Math.abs(Math.max(int1, int2) + int1 * int2 / 3), count);
            }
            Logger.getLogger(RepositorySourceLoadHarness.class).debug("Finishing {0} operation", this.getClass().getSimpleName());
            return total < count ? total : count; // should really always return count
        }
    }

    /**
     * A "random-enough" number generator that is cheap and that has no synchronization issues (like some other random number
     * generators).
     * <p>
     * This was taken from <a href="http://wwww.jcip.org">Java Concurrency In Practice</a> (page 253).
     * </p>
     * 
     * @param seed the seed, typically based on a hash code and nanoTime
     * @return a number that is "random enough"
     */
    public static int random( int seed ) {
        seed ^= (seed << 6);
        seed ^= (seed >>> 21);
        seed ^= (seed << 7);
        return seed;
    }

}
