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

import java.util.Collections;
import java.util.List;
import org.modeshape.graph.GraphI18n;

/**
 * A {@link RequestException} that wraps multiple failed {@link Request} objects.
 */
public class MultipleRequestFailuresException extends RequestException {

    private static final long serialVersionUID = 1L;

    private final List<Request> failedRequests;
    private final int totalNumberOfRequests;
    private transient String msg = null;

    /**
     * @param failedRequests the failed requests, where each request contains its own exception
     * @param totalNumberOfRequests the total number of requests that were submitted
     */
    public MultipleRequestFailuresException( List<Request> failedRequests,
                                             int totalNumberOfRequests ) {
        super();
        this.failedRequests = Collections.unmodifiableList(failedRequests);
        this.totalNumberOfRequests = totalNumberOfRequests;
        assert this.failedRequests != null;
        assert this.totalNumberOfRequests > 0;
    }

    /**
     * @return totalNumberOfRequests
     */
    public int getTotalNumberOfRequests() {
        return totalNumberOfRequests;
    }

    /**
     * @return failedRequests
     */
    public List<Request> getFailedRequests() {
        return failedRequests;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Throwable#getMessage()
     */
    @Override
    public String getMessage() {
        if (msg == null) {
            // Build a single composite error message ...
            StringBuilder str = new StringBuilder();
            for (Request requestWithError : failedRequests) {
                str.append("\n");
                str.append("\t" + requestWithError + " --> " + requestWithError.getError().getMessage());
            }
            int numberOfErrors = failedRequests.size();
            if (totalNumberOfRequests == CompositeRequest.UNKNOWN_NUMBER_OF_REQUESTS) {
                msg = GraphI18n.multipleErrorsWhileExecutingManyRequests.text(numberOfErrors, str.toString());
            } else {
                msg = GraphI18n.multipleErrorsWhileExecutingRequests.text(numberOfErrors, totalNumberOfRequests, str.toString());
            }
        }
        return msg;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Throwable#toString()
     */
    @Override
    public String toString() {
        return getMessage();
    }

}
