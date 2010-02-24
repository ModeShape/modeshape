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
package org.modeshape.graph.connector.federation;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.util.concurrent.CountDownLatch;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.graph.connector.federation.FederatedRequest.ProjectedRequest;
import org.modeshape.graph.request.Request;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Mock;

/**
 * 
 */
public class FederatedRequestTest {

    private FederatedRequest request;
    @Mock
    private Request original;
    private Projection[] projection;
    private Request[] projectedRequest;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        request = new FederatedRequest(original);
        projection = new Projection[] {mock(Projection.class), mock(Projection.class), mock(Projection.class)};
        projectedRequest = new Request[] {mock(Request.class), mock(Request.class), mock(Request.class)};
    }

    @Test
    public void shouldHaveOriginalRequest() {
        assertThat(request.original(), is(sameInstance(original)));
    }

    @Test
    public void shouldAddFirstProjectedRequest() {
        request.add(projectedRequest[0], false, false, projection[0]);
        assertThat(request.hasIncompleteRequests(), is(true));
        assertThat(request.getFirstProjectedRequest().getProjection(), is(sameInstance(projection[0])));
        assertThat(request.getFirstProjectedRequest().getRequest(), is(sameInstance(projectedRequest[0])));
        assertThat(request.getFirstProjectedRequest().hasNext(), is(false));
        assertThat(request.getFirstProjectedRequest().next(), is(nullValue()));
    }

    @Test
    public void shouldAddMultipleProjectedRequests() {
        request.add(projectedRequest[0], false, false, projection[0]);
        request.add(projectedRequest[1], false, true, projection[1]);
        request.add(projectedRequest[2], false, false, projection[2]);
        assertThat(request.hasIncompleteRequests(), is(true));

        ProjectedRequest first = request.getFirstProjectedRequest();
        assertThat(first.getProjection(), is(sameInstance(projection[0])));
        assertThat(first.getRequest(), is(sameInstance(projectedRequest[0])));
        assertThat(first.isComplete(), is(false));
        assertThat(first.hasNext(), is(true));
        assertThat(first.next(), is(notNullValue()));

        ProjectedRequest second = first.next();
        assertThat(second.getProjection(), is(sameInstance(projection[1])));
        assertThat(second.getRequest(), is(sameInstance(projectedRequest[1])));
        assertThat(second.isComplete(), is(true));
        assertThat(second.hasNext(), is(true));
        assertThat(second.next(), is(notNullValue()));

        ProjectedRequest third = second.next();
        assertThat(third.getProjection(), is(sameInstance(projection[2])));
        assertThat(third.getRequest(), is(sameInstance(projectedRequest[2])));
        assertThat(third.isComplete(), is(false));
        assertThat(third.hasNext(), is(false));
        assertThat(third.next(), is(nullValue()));
    }

    @Test
    public void shouldAddMultipleProjectedRequestsAddedInMethodChain() {
        request.add(projectedRequest[0], false, false, projection[0])
               .add(projectedRequest[1], false, true, projection[1])
               .add(projectedRequest[2], false, false, projection[2]);
        assertThat(request.hasIncompleteRequests(), is(true));

        ProjectedRequest first = request.getFirstProjectedRequest();
        assertThat(first.getProjection(), is(sameInstance(projection[0])));
        assertThat(first.getRequest(), is(sameInstance(projectedRequest[0])));
        assertThat(first.isComplete(), is(false));
        assertThat(first.hasNext(), is(true));
        assertThat(first.next(), is(notNullValue()));

        ProjectedRequest second = first.next();
        assertThat(second.getProjection(), is(sameInstance(projection[1])));
        assertThat(second.getRequest(), is(sameInstance(projectedRequest[1])));
        assertThat(second.isComplete(), is(true));
        assertThat(second.hasNext(), is(true));
        assertThat(second.next(), is(notNullValue()));

        ProjectedRequest third = second.next();
        assertThat(third.getProjection(), is(sameInstance(projection[2])));
        assertThat(third.getRequest(), is(sameInstance(projectedRequest[2])));
        assertThat(third.isComplete(), is(false));
        assertThat(third.hasNext(), is(false));
        assertThat(third.next(), is(nullValue()));
    }

    @Test
    public void shouldCreateCountDownLatchUponFreezeIfNumberOfIncompleteProjectedRequestsIsNonZero() {
        request.add(projectedRequest[0], false, false, projection[0]);
        request.freeze();
        assertThat(request.getLatch(), is(notNullValue()));
        assertThat(request.getLatch().getCount(), is(1L));
        assertThat(request.hasIncompleteRequests(), is(true));
    }

    @Test
    public void shouldUseClosedLatchUponFreezeIfNumberOfIncompleteProjectedRequestsIsZero() {
        request.add(projectedRequest[0], false, true, projection[0]);
        request.freeze();
        assertThat(request.getLatch(), is(sameInstance(FederatedRequest.CLOSED_LATCH)));
        assertThat(request.hasIncompleteRequests(), is(false));
    }

    @Test
    public void shouldNotBlockWhenAwaitingOnRequestWithNoIncompleteRequests() throws InterruptedException {
        request.add(projectedRequest[0], false, true, projection[0]);
        request.freeze();
        assertThat(request.hasIncompleteRequests(), is(false));
        request.await();
    }

    @Test
    public void shouldBlockWhenAwaitingOnRequestWithIncompleteRequests() throws InterruptedException {
        request.add(projectedRequest[0], false, false, projection[0]);
        request.add(projectedRequest[1], false, false, projection[1]);
        request.add(projectedRequest[2], false, false, projection[2]);
        request.freeze();
        assertThat(request.hasIncompleteRequests(), is(true));
        CountDownLatch latch = request.getLatch();
        assertThat(latch.getCount(), is(3L));
        Stopwatch sw = new Stopwatch();
        sw.start();
        new Thread(new CountDownRunnable(latch, 100L)).start();
        request.await(); // this blocks until the latch reaches 0
        assertThat(latch.getCount(), is(0L));
        sw.stop();
        assertThat(sw.getTotalDuration().getDurationInMilliseconds().intValue() >= 250, is(true));
    }

    protected static class CountDownRunnable implements Runnable {
        private final CountDownLatch latch;
        private final long sleepTimeInMillis;

        protected CountDownRunnable( CountDownLatch latch,
                                     long sleepTimeInMillis ) {
            assert latch != null;
            this.latch = latch;
            this.sleepTimeInMillis = sleepTimeInMillis;
        }

        public void run() {
            while (latch.getCount() != 0) {
                try {
                    Thread.sleep(sleepTimeInMillis);
                } catch (InterruptedException err) {
                    System.err.println("Error: interrupted while sleeping before counting down in " + this.getClass().getName());
                }
                latch.countDown();
            }
        }
    }
}
