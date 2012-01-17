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
package org.modeshape.jcr.api.monitor;

import java.util.Set;
import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;

/**
 * A thread-safe component that provides histories and statistics on a variety of metrics.
 * 
 * @since 3.0
 */
public interface RepositoryMonitor {

    /**
     * Get the ValueMetric enumerations that are available for use by the caller with {@link #getHistory(ValueMetric, Window)}.
     * 
     * @return the immutable set of ValueMetric instances; never null but possibly empty if the caller has no permissions to see
     *         any value metrics
     * @see #getHistory(ValueMetric, Window)
     */
    Set<ValueMetric> getAvailableValueMetrics();

    /**
     * Get the DurationMetric enumerations that are available for use by the caller with
     * {@link #getHistory(DurationMetric, Window)}.
     * 
     * @return the immutable set of DurationMetric instances; never null but possibly empty if the caller has no permissions to
     *         see any duration metrics
     * @see #getHistory(DurationMetric, Window)
     * @see #getLongestRunning(DurationMetric)
     */
    Set<DurationMetric> getAvailableDurationMetrics();

    /**
     * Get the Window enumerations that are available for use by the caller with {@link #getHistory(DurationMetric, Window)} and
     * {@link #getHistory(ValueMetric, Window)}.
     * 
     * @return the immutable set of DurationMetric instances; never null but possibly empty if the caller has no permissions to
     *         see any windows
     * @see #getHistory(DurationMetric, Window)
     * @see #getHistory(ValueMetric, Window)
     */
    Set<Window> getAvailableWindows();

    /**
     * Get the statics for the specified value metric during the given window in time. The oldest statistics will be first, while
     * the newest statistics will be last.
     * 
     * @param metric the value metric; may not be null
     * @param windowInTime the window specifying which statistics are to be returned; may not be null
     * @return the history of the metrics; never null but possibly empty if there are no statistics being captures for this
     *         repository
     * @throws AccessDeniedException if the session does not have privileges to monitor the repository
     * @throws RepositoryException if there is an error obtaining the history
     * @see #getAvailableValueMetrics()
     * @see #getAvailableWindows()
     */
    public History getHistory( ValueMetric metric,
                               Window windowInTime ) throws AccessDeniedException, RepositoryException;

    /**
     * Get the statics for the specified duration metric during the given window in time. The oldest statistics will be first,
     * while the newest statistics will be last.
     * 
     * @param metric the duration metric; may not be null
     * @param windowInTime the window specifying which statistics are to be returned; may not be null
     * @return the history of the metrics; never null but possibly empty if there are no statistics being captures for this
     *         repository
     * @throws AccessDeniedException if the session does not have privileges to monitor the repository
     * @throws RepositoryException if there is an error obtaining the history
     * @see #getAvailableDurationMetrics()
     * @see #getAvailableWindows()
     */
    public History getHistory( DurationMetric metric,
                               Window windowInTime ) throws AccessDeniedException, RepositoryException;

    /**
     * Get the longest-running activities recorded for the specified metric. The results contain the duration records in order of
     * increasing duration, with the activity with the longest duration appearing last in the array.
     * 
     * @param metric the duration metric; may not be null
     * @return the activities with the longest durations; never null but possibly empty if no such activities were performed
     * @throws AccessDeniedException if the session does not have privileges to monitor the repository
     * @throws RepositoryException if there is an error obtaining the history
     * @see #getAvailableDurationMetrics()
     */
    public DurationActivity[] getLongestRunning( DurationMetric metric ) throws AccessDeniedException, RepositoryException;

}
