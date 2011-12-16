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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import org.junit.After;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.graph.ExecutionContext;
import static org.modeshape.graph.connector.RepositorySourceLoadHarness.runLoadTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

/**
 * @author Randall Hauch
 * @author Horia Chiorean
 */
public class RepositoryConnectionPoolTest {

    private RepositoryConnectionPool pool;
    private RepositorySource source;
    private ExecutionContext context;

    @Before
    public void beforeEach() {
        source = new TimeDelayingRepositorySource("source 1");
        pool = new RepositoryConnectionPool(source, 1, 1, 100, TimeUnit.SECONDS);
        context = null;
    }

    @After
    public void afterEach() throws Exception {
        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);
    }

    @Test
    public void shouldBeCreatedInRunningState() {
        assertThat(pool.isShutdown(), is(false));
        assertThat(pool.isTerminating(), is(false));
        assertThat(pool.isTerminated(), is(false));
        assertThat(pool.getTotalConnectionsCreated(), is(0l));
        assertThat(pool.getTotalConnectionsUsed(), is(0l));
    }

    @Test
    public void shouldShutdownWhenNoConnectionsWereCreatedOrUsed() throws InterruptedException {
        assertThat(pool.isShutdown(), is(false));
        assertThat(pool.isTerminating(), is(false));
        assertThat(pool.isTerminated(), is(false));
        assertThat(pool.getTotalConnectionsCreated(), is(0l));
        assertThat(pool.getTotalConnectionsUsed(), is(0l));

        for (int i = 0; i != 4; ++i) {
            pool.shutdown();
            assertThat(pool.isShutdown() || pool.isTerminating() || pool.isTerminated(), is(true));
            pool.awaitTermination(2, TimeUnit.SECONDS);
            assertThat(pool.isTerminated(), is(true));
            assertThat(pool.getTotalConnectionsCreated(), is(0l));
            assertThat(pool.getTotalConnectionsUsed(), is(0l));
        }
    }

    @Test
    public void shouldCreateConnectionAndRecoverWhenClosed() throws RepositorySourceException {
        assertThat(pool.getTotalConnectionsCreated(), is(0l));
        assertThat(pool.getTotalConnectionsUsed(), is(0l));

        RepositoryConnection conn = pool.getConnection();
        assertThat(conn, is(notNullValue()));
        assertThat(pool.getTotalConnectionsCreated(), is(1l));
        assertThat(pool.getTotalConnectionsUsed(), is(1l));
        assertThat(pool.getPoolSize(), is(1));

        conn.close();

        assertThat(pool.getTotalConnectionsCreated(), is(1l));
        assertThat(pool.getTotalConnectionsUsed(), is(1l));
        assertThat(pool.getPoolSize(), is(1));
    }

    @Test
    public void shouldAllowShutdownToBeCalledMultipleTimesEvenWhenShutdown()
            throws RepositorySourceException, InterruptedException {
        assertThat(pool.getTotalConnectionsCreated(), is(0l));
        assertThat(pool.getTotalConnectionsUsed(), is(0l));

        RepositoryConnection conn = pool.getConnection();
        assertThat(conn, is(notNullValue()));
        assertThat(pool.getTotalConnectionsCreated(), is(1l));
        assertThat(pool.getTotalConnectionsUsed(), is(1l));
        assertThat(pool.getPoolSize(), is(1));

        conn.close();

        assertThat(pool.getTotalConnectionsCreated(), is(1l));
        assertThat(pool.getTotalConnectionsUsed(), is(1l));
        assertThat(pool.getPoolSize(), is(1));

        pool.shutdown();
        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);
        assertThat(pool.isTerminated(), is(true));
        assertThat(pool.getTotalConnectionsCreated(), is(1l));
        assertThat(pool.getTotalConnectionsUsed(), is(1l));
        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);
        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);
    }

    @Test( expected = IllegalStateException.class )
    public void shouldNotCreateConnectionIfPoolIsNotRunning() throws RepositorySourceException, InterruptedException {
        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);
        assertThat(pool.isTerminated(), is(true));
        assertThat(pool.getTotalConnectionsCreated(), is(0l));
        assertThat(pool.getTotalConnectionsUsed(), is(0l));
        pool.getConnection(); // this should fail with illegal state
    }

    @Test
    public void shouldAllowConnectionsToBeClosedMoreThanOnceWithNoIllEffects() throws RepositorySourceException {
        assertThat(pool.getTotalConnectionsCreated(), is(0l));
        assertThat(pool.getTotalConnectionsUsed(), is(0l));

        RepositoryConnection conn = pool.getConnection();
        assertThat(conn, is(notNullValue()));
        assertThat(pool.getTotalConnectionsCreated(), is(1l));
        assertThat(pool.getTotalConnectionsUsed(), is(1l));
        assertThat(pool.getPoolSize(), is(1));

        conn.close();
        conn.close();
    }

    @Test
    public void shouldBlockClientsWhenNotEnoughConnections() throws Exception {
        int numConnectionsInPool = 1;
        int numClients = 2;
        RepositoryConnectionPool pool = new RepositoryConnectionPool(source);
        pool.setCorePoolSize(numConnectionsInPool);
        pool.setMaximumPoolSize(numConnectionsInPool);
        RepositoryOperation.Factory<Integer> operationFactory = RepositorySourceLoadHarness.createMultipleLoadOperationFactory(10);
        runLoadTest(context, pool, numClients, 100, TimeUnit.MILLISECONDS, operationFactory);
        pool.shutdown();
        pool.awaitTermination(4, TimeUnit.SECONDS);
    }

    @Test
    public void shouldLimitClientsToRunSequentiallyWithOneConnectionInPool() throws Exception {
        int numConnectionsInPool = 1;
        int numClients = 3;
        RepositoryConnectionPool pool = new RepositoryConnectionPool(source);
        pool.setCorePoolSize(numConnectionsInPool);
        pool.setMaximumPoolSize(numConnectionsInPool);
        RepositoryOperation.Factory<Integer> operationFactory = RepositorySourceLoadHarness.createMultipleLoadOperationFactory(10);
        runLoadTest(context, pool, numClients, 100, TimeUnit.MILLISECONDS, operationFactory);
        pool.shutdown();
        pool.awaitTermination(4, TimeUnit.SECONDS);
    }

    @Test
    public void shouldClientsToRunConncurrentlyWithTwoConnectionsInPool() throws Exception {
        int numConnectionsInPool = 2;
        int numClients = 10;
        RepositoryConnectionPool pool = new RepositoryConnectionPool(source);
        pool.setCorePoolSize(numConnectionsInPool);
        pool.setMaximumPoolSize(numConnectionsInPool);
        RepositoryOperation.Factory<Integer> operationFactory = RepositorySourceLoadHarness.createMultipleLoadOperationFactory(10);
        runLoadTest(context, pool, numClients, 100, TimeUnit.MILLISECONDS, operationFactory);
        pool.shutdown();
        pool.awaitTermination(4, TimeUnit.SECONDS);
    }

    @Ignore( "doesn't run on hudson" )
    @Test
    public void shouldClientsToRunConncurrentlyWithMultipleConnectionInPool() throws Exception {
        int numConnectionsInPool = 10;
        int numClients = 50;
        RepositoryConnectionPool pool = new RepositoryConnectionPool(source);
        pool.setCorePoolSize(numConnectionsInPool);
        pool.setMaximumPoolSize(numConnectionsInPool);
        RepositoryOperation.Factory<Integer> operationFactory = RepositorySourceLoadHarness.createMultipleLoadOperationFactory(20);
        List<Future<Integer>> results = runLoadTest(context, pool, numClients, 200, TimeUnit.MILLISECONDS, operationFactory);
        int total = 0;
        for (Future<Integer> result : results) {
            assertThat(result.isDone(), is(true));
            if (result.isDone()) {
                total += result.get();
            }
        }
        assertThat(total, is(20 * numClients));
        pool.shutdown();
        pool.awaitTermination(4, TimeUnit.SECONDS);
    }

    @Test
    public void shouldReturnTrueFromPingIfRunning() throws Exception {
        int numConnectionsInPool = 2;
        RepositoryConnectionPool pool = new RepositoryConnectionPool(source);
        pool.setCorePoolSize(numConnectionsInPool);
        pool.setMaximumPoolSize(numConnectionsInPool);
        assertThat(pool.ping(), is(true));
        assertThat(pool.ping(20, TimeUnit.SECONDS), is(true));
        pool.shutdown();
        pool.awaitTermination(4, TimeUnit.SECONDS);
    }

    @Test
    public void shouldReturnFalseFromPingIfNotRunning() throws Exception {
        int numConnectionsInPool = 2;
        RepositoryConnectionPool pool = new RepositoryConnectionPool(source);
        pool.setCorePoolSize(numConnectionsInPool);
        pool.setMaximumPoolSize(numConnectionsInPool);
        pool.shutdown();
        pool.awaitTermination(4, TimeUnit.SECONDS);
        assertThat(pool.ping(), is(false));

    }

    @FixFor("MODE-1347")
    @Test
    public void shouldntBlockOnConcurrentOpenCloseConnectionScenario1() throws Exception {
        final RepositoryConnectionPool pool = new RepositoryConnectionPool(source);
        final Random rnd = new Random();
        final int maxWaitSeconds = 10;

        ExecutorService executorService = Executors.newCachedThreadPool();

        //open & close twice the number of connections that the pool can handle, pausing in-between
        for (int i = 0; i < pool.getMaximumPoolSize() * 2; i++) {
            executorService.submit(new Callable<RepositoryConnection>() {
                public RepositoryConnection call() throws Exception {
                    RepositoryConnection connection = pool.getConnection();
                    assertNotNull(connection);
                    long waitMillis = TimeUnit.SECONDS.toMillis(rnd.nextInt(maxWaitSeconds));
                    Thread.sleep(waitMillis);
                    connection.close();
                    return connection;
                }
            });
        }

        //open the maximum number of connections the pool can handle, without closing
        List<Future<Void>> futureTasks = new ArrayList<Future<Void>>();
        for (int i = 0; i < pool.getMaximumPoolSize(); i++) {
            futureTasks.add(executorService.submit(new Callable<Void>() {
                public Void call() throws Exception {
                    RepositoryConnection connection = pool.getConnection();
                    assertNotNull(connection);
                    long waitMillis = TimeUnit.SECONDS.toMillis(rnd.nextInt(maxWaitSeconds));
                    Thread.sleep(waitMillis);
                    return null;
                }
            }));
        }

        //wait for each of the the latest tasks to finish
        for (Future<Void> futureTask : futureTasks) {
            futureTask.get();
        }

        executorService.shutdown();
    }

    @FixFor("MODE-1347")
    @Test
    public void shouldntBlockOnConcurrentOpenCloseConnectionScenario2() throws Exception {
        final RepositoryConnectionPool pool = new RepositoryConnectionPool(source);
        ExecutorService executorService = Executors.newCachedThreadPool();

        //open the maximum number of connections + 1
        final List<Future<RepositoryConnection>> futureConnections = new ArrayList<Future<RepositoryConnection>>();
        for (int i = 0; i <= pool.getMaximumPoolSize(); i++) {
            Callable<RepositoryConnection> openConnectionTask = new Callable<RepositoryConnection>() {
                public RepositoryConnection call() throws Exception {
                    RepositoryConnection connection = pool.getConnection();
                    assertNotNull(connection);
                    return connection;
                }
            };
            futureConnections.add(executorService.submit(openConnectionTask));
        }

        //close the first connection
        executorService.submit(new Callable<Void>() {
            public Void call() throws Exception {
                futureConnections.get(0).get().close();
                return null;
            }
        });

        //wait for all the submitted jobs to finish
        for (int i = 1; i < futureConnections.size(); i++) {
           futureConnections.get(i).get();
        }

        executorService.shutdown();
    }

}
