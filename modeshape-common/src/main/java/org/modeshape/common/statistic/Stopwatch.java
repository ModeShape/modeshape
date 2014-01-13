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
package org.modeshape.common.statistic;

import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.math.Duration;
import org.modeshape.common.math.DurationOperations;

/**
 * Provides a mechanism to measure time in the same way as a physical stopwatch.
 */
@NotThreadSafe
public class Stopwatch implements Comparable<Stopwatch> {

    private long lastStarted;
    private final SimpleStatistics<Duration> stats;
    private final DetailedStatistics<Duration> detailedStats;
    private String description;

    public Stopwatch() {
        this(true);
    }

    public Stopwatch( boolean detailedStats ) {
        this(detailedStats, null);
    }

    public Stopwatch( boolean detailedStats,
                      String description ) {
        this.description = description != null ? description : "";
        this.detailedStats = detailedStats ? new DetailedStatistics<Duration>(new DurationOperations()) : null;
        this.stats = detailedStats ? this.detailedStats : new SimpleStatistics<Duration>(new DurationOperations());
        reset();
    }

    public String getDescription() {
        return this.description;
    }

    /**
     * Start the stopwatch and begin recording the statistics a new run. This method does nothing if the stopwatch is already
     * {@link #isRunning() running}
     * 
     * @see #isRunning()
     */
    public void start() {
        if (!this.isRunning()) {
            this.lastStarted = System.nanoTime();
        }
    }

    /**
     * Stop the stopwatch and record the statistics for the latest run. This method does nothing if the stopwatch is not currently
     * {@link #isRunning() running}
     * 
     * @see #isRunning()
     */
    public void stop() {
        if (this.isRunning()) {
            long duration = System.nanoTime() - this.lastStarted;
            this.lastStarted = 0l;
            this.stats.add(new Duration(duration));
        }
    }

    /**
     * Record the statistics for the latest run, but keep the stopwatch going. This method does nothing if the stopwatch is not
     * currently {@link #isRunning() running}
     * 
     * @see #isRunning()
     */
    public void lap() {
        if (this.isRunning()) {
            long now = System.nanoTime();
            long duration = now - this.lastStarted;
            this.lastStarted = now;
            this.stats.add(new Duration(duration));
        }
    }

    /**
     * Return the number of runs (complete starts and stops) this stopwatch has undergone.
     * 
     * @return the number of runs.
     * @see #isRunning()
     */
    public int getCount() {
        return this.stats.getCount();
    }

    /**
     * Return whether this stopwatch is currently running.
     * 
     * @return true if running, or false if not
     */
    public boolean isRunning() {
        return this.lastStarted != 0;
    }

    /**
     * Get the total duration that this stopwatch has recorded.
     * 
     * @return the total duration, or an empty duration if this stopwatch has not been used since creation or being
     *         {@link #reset() reset}
     */
    public Duration getTotalDuration() {
        return this.stats.getTotal();
    }

    /**
     * Get the average duration that this stopwatch has recorded.
     * 
     * @return the average duration, or an empty duration if this stopwatch has not been used since creation or being
     *         {@link #reset() reset}
     */
    public Duration getAverageDuration() {
        return this.stats.getMean();
    }

    /**
     * Get the median duration that this stopwatch has recorded.
     * 
     * @return the median duration, or an empty duration if this stopwatch has not been used since creation or being
     *         {@link #reset() reset}
     */
    public Duration getMedianDuration() {
        return this.detailedStats != null ? this.detailedStats.getMedian() : new Duration(0l);
    }

    /**
     * Get the minimum duration that this stopwatch has recorded.
     * 
     * @return the total minimum, or an empty duration if this stopwatch has not been used since creation or being
     *         {@link #reset() reset}
     */
    public Duration getMinimumDuration() {
        return this.stats.getMinimum();
    }

    /**
     * Get the maximum duration that this stopwatch has recorded.
     * 
     * @return the maximum duration, or an empty duration if this stopwatch has not been used since creation or being
     *         {@link #reset() reset}
     */
    public Duration getMaximumDuration() {
        return this.stats.getMaximum();
    }

    /**
     * Return this stopwatch's simple statistics.
     * 
     * @return the statistics
     * @see #getDetailedStatistics()
     */
    public SimpleStatistics<Duration> getSimpleStatistics() {
        return this.stats;
    }

    /**
     * Return this stopwatch's detailed statistics, if they are being kept.
     * 
     * @return the statistics
     * @see #getSimpleStatistics()
     */
    public DetailedStatistics<Duration> getDetailedStatistics() {
        return this.detailedStats;
    }

    /**
     * Return true if detailed statistics are being kept.
     * 
     * @return true if {@link #getDetailedStatistics() detailed statistics} are being kept, or false if only
     *         {@link #getSimpleStatistics() simple statistics} are being kept.
     */
    public boolean isDetailedStatistics() {
        return this.detailedStats != null;
    }

    /**
     * Return the histogram of this stopwatch's individual runs. Two different kinds of histograms can be created. The first kind
     * is a histogram where all of the buckets are distributed normally and all have the same width. In this case, the 'numSigmas'
     * should be set to 0.
     * <p>
     * <i>Note: if only {@link #getSimpleStatistics() simple statistics} are being kept, the resulting histogram is always empty.
     * <p>
     * The second kind of histogram is more useful when most of the data that is clustered near one value. This histogram is
     * focused around the values that are up to 'numSigmas' above and below the {@link #getMedianDuration() median}, and all
     * values outside of this range are placed in the first and last bucket.
     * </p>
     * 
     * @param numSigmas the number of standard deviations from the {@link #getMedianDuration() median}, or 0 if the buckets of the
     *        histogram should be evenly distributed
     * @return the histogram
     */
    public Histogram<Duration> getHistogram( int numSigmas ) {
        return this.detailedStats != null ? this.detailedStats.getHistogram(numSigmas) : new Histogram<Duration>(
                                                                                                                 this.stats.getMathOperations());
    }

    /**
     * Reset this stopwatch and clear all statistics.
     */
    public void reset() {
        this.lastStarted = 0l;
        this.stats.reset();
    }

    @Override
    public int compareTo( Stopwatch that ) {
        return this.getTotalDuration().compareTo(that.getTotalDuration());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getTotalDuration());
        if (this.stats.getCount() > 1) {
            sb.append(" (");
            sb.append(this.stats.getCount()).append(" samples, avg=");
            sb.append(this.getAverageDuration());
            sb.append("; median=");
            sb.append(this.getMedianDuration());
            sb.append("; min=");
            sb.append(this.getMinimumDuration());
            sb.append("; max=");
            sb.append(this.getMaximumDuration());
            sb.append(")");
        }
        return sb.toString();
    }

}
