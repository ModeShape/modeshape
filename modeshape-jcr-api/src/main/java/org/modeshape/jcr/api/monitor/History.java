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

import java.util.concurrent.TimeUnit;
import org.modeshape.jcr.api.value.DateTime;

/**
 * An immutable history of a metric for a given window in time.
 * 
 * @since 3.0
 */
public interface History {
    
    /**
     * A history with no statistics, no window, no start or end time, and no duration.
     */
    public History NO_HISTORY = new History() {

        /**
         * {@inheritDoc}
         * <p>
         * <strong>Always returns <code>null</code>.</strong>
         * 
         * @see org.modeshape.jcr.api.monitor.History#getWindow()
         */
        @Override
        public Window getWindow() {
            return null;
        }

        /**
         * {@inheritDoc}
         * <p>
         * <strong>Always returns <code>0</code> (zero).</strong>
         * 
         * @see org.modeshape.jcr.api.monitor.History#getTotalDuration(java.util.concurrent.TimeUnit)
         */
        @Override
        public long getTotalDuration( final TimeUnit unit ) {
            return 0;
        }

        /**
         * {@inheritDoc}
         * <p>
         * <strong>Always returns <code>null</code>.</strong>
         * 
         * @see org.modeshape.jcr.api.monitor.History#getStartTime()
         */
        @Override
        public DateTime getStartTime() {
            return null;
        }

        /**
         * {@inheritDoc}
         * <p>
         * <strong>Always returns <code>null</code>.</strong>
         * 
         * @see org.modeshape.jcr.api.monitor.History#getEndTime()
         */
        @Override
        public DateTime getEndTime() {
            return null;
        }

        /**
         * {@inheritDoc}
         * <p>
         * <strong>Always returns an empty statistics array.</strong>
         * 
         * @see org.modeshape.jcr.api.monitor.History#getStats()
         */
        @Override
        public Statistics[] getStats() {
            return Statistics.NO_STATISTICS;
        }

    };

    /**
     * Get the kind of window.
     * 
     * @return the window type; never null
     */
    public Window getWindow();

    /**
     * Get the total duration of this history window.
     * 
     * @param unit the desired time unit; if null, then {@link TimeUnit#SECONDS} is used
     * @return the duration
     */
    public long getTotalDuration( TimeUnit unit );

    /**
     * Get the timestamp (including time zone information) at which this history window starts.
     * 
     * @return the time at which this window starts
     */
    public DateTime getStartTime();

    /**
     * Get the timestamp (including time zone information) at which this history window ends.
     * 
     * @return the time at which this window ends
     */
    public DateTime getEndTime();

    /**
     * Get the statistics for that make up the history.
     * 
     * @return the statistics; never null, but the array may contain null if the window is longer than the lifetime of the
     *         repository
     */
    Statistics[] getStats();
}
