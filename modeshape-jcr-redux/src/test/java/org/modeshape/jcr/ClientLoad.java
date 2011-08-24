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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.modeshape.common.annotation.Immutable;

/**
 * A simple load-testing client.
 */
public class ClientLoad {

    public static <Result> List<Client<Result>> runSimultaneously( final int numClients,
                                                                   final Callable<Result> callable ) throws InterruptedException {
        return run(numClients, callable, 30, 50, TimeUnit.SECONDS);
    }

    public static <Result> List<Client<Result>> run( final int numClients,
                                                     final Callable<Result> callable ) throws InterruptedException {
        return run(numClients, callable, 0, 30, TimeUnit.SECONDS);
    }

    protected static <Result> List<Client<Result>> run( final int numClients,
                                                        final Callable<Result> callable,
                                                        final long initialBarrier,
                                                        long finishWaitTime,
                                                        final TimeUnit timeUnit ) throws InterruptedException {
        final CyclicBarrier barrier = new CyclicBarrier(numClients / 4);
        final CountDownLatch latch = new CountDownLatch(numClients);
        final ExecutorService clientService = Executors.newFixedThreadPool(numClients + 2);
        final List<Future<?>> clientFutures = new ArrayList<Future<?>>(numClients);
        final List<Client<Result>> results = new ArrayList<Client<Result>>(numClients);
        try {
            for (int i = 0; i != numClients; ++i) {
                clientFutures.add(clientService.submit(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        Result result = null;
                        Throwable error = null;
                        long durationInNanos = 0L;
                        try {
                            if (initialBarrier > 0L) {
                                // Wait for everyone to be ready, but no more than the initial barrier ...
                                barrier.await(initialBarrier, timeUnit);
                            }

                            long start = System.nanoTime();
                            result = callable.call();
                            durationInNanos = System.nanoTime() - start;
                            latch.countDown();
                        } catch (Exception e) {
                            error = e;
                        } finally {
                            results.add(new Client<Result>(result, true, durationInNanos, error));
                        }
                        return null;
                    }
                }));
            }
        } finally {
            clientService.shutdown();
        }

        // Wait for the clients to all finish (twice as long as they'll all wait to start ...
        latch.await(finishWaitTime, timeUnit);
        return results;
    }

    public static <Result> void assertAllSucceeded( List<Client<Result>> clientResults,
                                                    long maximumDuration,
                                                    TimeUnit unit ) {
        for (Client<Result> client : clientResults) {
            assertThat(client.isSuccess(), is(true));
            assertThat(client.getTime(unit) < maximumDuration, is(true));
        }
    }

    public static <Result> void forEachResult( List<Client<Result>> clientResults,
                                               ClientResultProcessor<Result> operation ) throws Exception {
        Exception firstError = null;
        for (Client<Result> client : clientResults) {
            try {
                operation.process(client);
            } catch (Exception e) {
                firstError = firstError != null ? firstError : e;
            }
        }
        if (firstError != null) throw firstError;
    }

    public static interface ClientResultProcessor<Result> {
        void process( Client<Result> clientResult ) throws Exception;
    }

    @Immutable
    public static class Client<Result> {
        private final Result result;
        private final boolean success;
        private final long timeInNanos;
        private final Throwable t;

        protected Client( Result result,
                          boolean success,
                          long timeInNanos,
                          Throwable t ) {
            this.result = result;
            this.success = success;
            this.timeInNanos = timeInNanos;
            this.t = t;
        }

        public boolean isSuccess() {
            return success;
        }

        public Result getResult() {
            return result;
        }

        public Throwable getError() {
            return t;
        }

        public long getTimeInNanos() {
            return timeInNanos;
        }

        public long getTime( TimeUnit unit ) {
            return unit.convert(timeInNanos, TimeUnit.NANOSECONDS);
        }

    }

}
