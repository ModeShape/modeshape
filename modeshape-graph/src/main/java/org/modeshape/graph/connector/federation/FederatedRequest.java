/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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

import java.util.concurrent.CountDownLatch;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.graph.request.Request;

/**
 * A wrapper for a request submitted to the federated repository, and the corresponding source-specific {@link ProjectedRequest
 * projected requests}.
 */
@NotThreadSafe
class FederatedRequest {
    static final CountDownLatch CLOSED_LATCH = new CountDownLatch(0);

    private final Request original;
    private CountDownLatch forkLatch;
    private int incompleteCount;
    private ProjectedRequest first;

    FederatedRequest( Request original ) {
        this.original = original;
    }

    public Request original() {
        return original;
    }

    public final FederatedRequest add( Request request,
                                       boolean isSameLocationAsOriginal,
                                       boolean isComplete,
                                       Projection projection,
                                       Projection secondProjection ) {
        assert forkLatch == null;
        if (!isComplete) ++incompleteCount;
        if (first == null) {
            if (isSameLocationAsOriginal) {
                first = new MirrorRequest(request, isComplete, projection, secondProjection);
            } else {
                first = new ProjectedRequest(request, isComplete, projection, secondProjection);
            }
        } else {
            first.addNext(request, isComplete, projection);
        }
        return this;
    }

    public final FederatedRequest add( Request request,
                                       boolean isSameLocationAsOriginal,
                                       boolean isComplete,
                                       Projection projection ) {
        return add(request, isSameLocationAsOriginal, isComplete, projection, null);
    }

    public void freeze() {
        if (forkLatch == null) {
            forkLatch = incompleteCount > 0 ? new CountDownLatch(incompleteCount) : CLOSED_LATCH;
        }
    }

    public ProjectedRequest getFirstProjectedRequest() {
        return first;
    }

    public boolean hasIncompleteRequests() {
        return incompleteCount != 0;
    }

    public CountDownLatch getLatch() {
        freeze();
        return forkLatch;
    }

    public void await() throws InterruptedException {
        if (forkLatch != null) forkLatch.await();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Federated request: ").append(original).append("\n");
        ProjectedRequest projected = first;
        while (projected != null) {
            sb.append("  - ").append(projected).append("\n");
            projected = projected.next();
        }
        return sb.toString();
    }

    class ProjectedRequest {
        private final Projection projection;
        private final Projection projection2;
        private final Request request;
        private final boolean isComplete;
        private ProjectedRequest next;

        protected ProjectedRequest( Request request,
                                    boolean isComplete,
                                    Projection projection,
                                    Projection secondProjection ) {
            this.projection = projection;
            this.request = request;
            this.isComplete = isComplete;
            this.projection2 = secondProjection;
        }

        public final Projection getProjection() {
            return projection;
        }

        public final Projection getSecondProjection() {
            return projection2;
        }

        public final Request getRequest() {
            return request;
        }

        public final boolean isComplete() {
            return isComplete;
        }

        public boolean isSameLocation() {
            return false;
        }

        public final ProjectedRequest next() {
            return next;
        }

        public final boolean hasNext() {
            return next != null;
        }

        protected final ProjectedRequest addNext( Request request,
                                                  boolean isComplete,
                                                  Projection projection,
                                                  Projection secondProjection ) {
            ProjectedRequest last = this;
            while (last.next != null) {
                last = last.next;
            }
            last.next = new ProjectedRequest(request, isComplete, projection, secondProjection);
            return last.next;
        }

        protected final ProjectedRequest addNext( Request request,
                                                  boolean isComplete,
                                                  Projection projection ) {
            return addNext(request, isComplete, projection, null);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Projects to: ");
            sb.append(request);
            if (projection != null) {
                sb.append(" using ");
                sb.append(projection);
                if (projection2 != null) {
                    sb.append(" and ");
                    sb.append(projection2);
                }
            }
            if (isComplete) {
                sb.append(" (complete)");
            }
            return sb.toString();
        }
    }

    class MirrorRequest extends ProjectedRequest {
        protected MirrorRequest( Request request,
                                 boolean isComplete,
                                 Projection projection,
                                 Projection secondProjection ) {
            super(request, isComplete, projection, secondProjection);
        }

        @Override
        public boolean isSameLocation() {
            return true;
        }
    }

}
