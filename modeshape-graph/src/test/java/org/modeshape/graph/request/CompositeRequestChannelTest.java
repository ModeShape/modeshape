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
package org.modeshape.graph.request;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.connector.MockRepositoryConnection;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryConnectionFactory;

/**
 * 
 */
public class CompositeRequestChannelTest {

    private ExecutionContext context;
    private String sourceName;
    private CompositeRequestChannel channel;
    private List<Request> requests;
    private ExecutorService executor;
    private LinkedList<Request> executedRequests;
    private RepositoryConnection connection;
    @Mock
    private RepositoryConnectionFactory connectionFactory;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        context = new ExecutionContext();
        sourceName = "SourceA";
        channel = new CompositeRequestChannel(sourceName);
        requests = new ArrayList<Request>();
        requests.add(new MockRequest());
        requests.add(new MockRequest());
        requests.add(new MockRequest());
        requests.add(new MockRequest());

        // Create the mock connection ...
        executedRequests = new LinkedList<Request>(); // this is where requests submitted to the connection will go
        connection = new MockRepositoryConnection(sourceName, executedRequests);

        // Stub the connection factory ...
        when(connectionFactory.createConnection(sourceName)).thenReturn(connection);

        // Create the executor ...
        executor = Executors.newSingleThreadExecutor();
    }

    protected static class MockRequest extends Request {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public RequestType getType() {
            return RequestType.INVALID;
        }
        
        
    }

    @Test
    public void shouldCreateEmptyIteratorIfDoneCalledBeforeObtainingIterator() {
        channel.close();
        Iterator<Request> iter = channel.createIterator();
        assertThat(iter.hasNext(), is(false));
    }

    @Test
    public void shouldCreateEmptyIteratorIfDoneCalledAfterObtainingIterator() {
        Iterator<Request> iter = channel.createIterator();
        channel.close();
        assertThat(iter.hasNext(), is(false));
    }

    @Test
    public void shouldCreateIteratorOverRequestsAddedToChannelAfterObtainingIterator() {
        Iterator<Request> iter = channel.createIterator();
        // Add the requests ...
        for (Request request : requests) {
            channel.add(request);
        }
        // Call done ...
        channel.close();
        // Start iterating ...
        for (Request expected : requests) {
            assertThat(iter.hasNext(), is(true));
            assertThat(iter.next(), is(sameInstance(expected)));
        }
        assertThat(iter.hasNext(), is(false));
    }

    @Test
    public void shouldCreateIteratorOverRequestsAddedToChannelAfterBeginningIteration() {
        Iterator<Request> iter = channel.createIterator();
        // Add the requests in a separate thread ...
        new Thread(new AddRequestsRunnable(channel, requests, 100)).start();
        // Start iterating ...
        for (Request expected : requests) {
            assertThat(iter.hasNext(), is(true)); // blocks
            assertThat(iter.next(), is(sameInstance(expected)));
        }
        assertThat(iter.hasNext(), is(false));
    }

    @Test
    public void shouldSubmitRequestsToConnection() throws Exception {
        // Start the channel ...
        channel.start(executor, context, connectionFactory);

        // Submit the requests to the channel ...
        for (Request request : requests) {
            channel.add(request);
        }

        // Mark the channel as done ...
        channel.close();

        // Wait until the channel has completed ...
        channel.await();

        // Verify that all the requests to the channel were processed ...
        Iterator<Request> iter = executedRequests.iterator();
        for (Request expected : requests) {
            assertThat(iter.hasNext(), is(true));
            assertThat(iter.next(), is(sameInstance(expected)));
        }
        assertThat(iter.hasNext(), is(false));
    }

    @Test
    public void shouldSubmitBlockedRequestsToConnection() throws Exception {
        // Start the channel ...
        channel.start(executor, context, connectionFactory);

        // Submit the requests to the channel ...
        List<CountDownLatch> latches = new ArrayList<CountDownLatch>();
        for (Request request : requests) {
            CountDownLatch latch = new CountDownLatch(1);
            latches.add(latch);
            channel.add(request, latch);
        }

        // Mark the channel as done ...
        channel.close();

        // Wait until the channel has completed ...
        channel.await();

        // Verify that all of the latches were decremented ...
        for (CountDownLatch latch : latches) {
            latch.await();
            assertThat(latch.getCount(), is(0L));
        }

        // Verify that all the requests to the channel were processed ...
        Iterator<Request> iter = executedRequests.iterator();
        for (Request expected : requests) {
            assertThat(iter.hasNext(), is(true));
            assertThat(iter.next(), is(sameInstance(expected)));
        }
        assertThat(iter.hasNext(), is(false));
    }

    protected static class AddRequestsRunnable implements Runnable {
        private final CompositeRequestChannel channel;
        private final Iterator<Request> requests;
        private final int intervalInMillis;

        protected AddRequestsRunnable( CompositeRequestChannel channel,
                                       List<Request> requests,
                                       int intervalInMillis ) {
            this.requests = requests.iterator();
            this.intervalInMillis = intervalInMillis;
            this.channel = channel;
        }

        public void run() {
            while (requests.hasNext()) {
                try {
                    Thread.sleep(intervalInMillis);
                } catch (InterruptedException err) {
                    fail(err.getMessage());
                }
                channel.add(requests.next());
            }
            // Call done ...
            channel.close();
        }
    }
}
