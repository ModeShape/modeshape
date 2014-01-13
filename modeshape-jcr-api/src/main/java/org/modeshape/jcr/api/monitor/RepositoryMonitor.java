/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.api.monitor;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;

/**
 * A thread-safe component that provides histories and statistics on a variety of metrics.
 * 
 * @since 3.0
 */
public interface RepositoryMonitor {

    Set<DurationMetric> ALL_DURATION_METRICS = Collections.unmodifiableSet(EnumSet.allOf(DurationMetric.class));
    Set<ValueMetric> ALL_VALUE_METRICS = Collections.unmodifiableSet(EnumSet.allOf(ValueMetric.class));
    Set<Window> ALL_WINDOWS = Collections.unmodifiableSet(EnumSet.allOf(Window.class));

    /**
     * A monitor without history.
     */
    RepositoryMonitor EMPTY_MONITOR = new RepositoryMonitor() {

        /**
         * {@inheritDoc}
         * <p>
         * <strong>Always returns {@link RepositoryMonitor#ALL_DURATION_METRICS}.</strong>
         * 
         * @see org.modeshape.jcr.api.monitor.RepositoryMonitor#getAvailableDurationMetrics()
         */
        @Override
        public Set<DurationMetric> getAvailableDurationMetrics() {
            return ALL_DURATION_METRICS;
        }

        /**
         * {@inheritDoc}
         * <p>
         * <strong>Always returns {@link RepositoryMonitor#ALL_VALUE_METRICS}.</strong>
         * 
         * @see org.modeshape.jcr.api.monitor.RepositoryMonitor#getAvailableValueMetrics()
         */
        @Override
        public Set<ValueMetric> getAvailableValueMetrics() {
            return ALL_VALUE_METRICS;
        }

        /**
         * {@inheritDoc}
         * <p>
         * <strong>Always returns {@link RepositoryMonitor#ALL_WINDOWS}.</strong>
         * 
         * @see org.modeshape.jcr.api.monitor.RepositoryMonitor#getAvailableWindows()
         */
        @Override
        public Set<Window> getAvailableWindows() {
            return ALL_WINDOWS;
        }

        /**
         * {@inheritDoc}
         * <p>
         * <strong>Always returns {@link History#NO_HISTORY}.</strong>
         * 
         * @see org.modeshape.jcr.api.monitor.RepositoryMonitor#getHistory(org.modeshape.jcr.api.monitor.ValueMetric,
         *      org.modeshape.jcr.api.monitor.Window)
         */
        @Override
        public History getHistory( ValueMetric metric,
                                   Window windowInTime ) {
            return History.NO_HISTORY;
        }

        /**
         * {@inheritDoc}
         * <p>
         * <strong>Always returns {@link History#NO_HISTORY}.</strong>
         * 
         * @see org.modeshape.jcr.api.monitor.RepositoryMonitor#getHistory(org.modeshape.jcr.api.monitor.DurationMetric,
         *      org.modeshape.jcr.api.monitor.Window)
         */
        @Override
        public History getHistory( DurationMetric metric,
                                   Window windowInTime ) {
            return History.NO_HISTORY;
        }

        /**
         * {@inheritDoc}
         * <p>
         * <strong>Always returns an empty duration activity array.</strong>
         * 
         * @see org.modeshape.jcr.api.monitor.RepositoryMonitor#getLongestRunning(org.modeshape.jcr.api.monitor.DurationMetric)
         */
        @Override
        public DurationActivity[] getLongestRunning( DurationMetric metric ) {
            return DurationActivity.NO_DURATION_RECORDS;
        }

    };

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
