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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.lock.Lock;
import javax.jcr.observation.EventListener;
import javax.jcr.query.Query;
import javax.jcr.version.VersionManager;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.text.Inflector;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.core.ExecutionContext;
import org.modeshape.jcr.value.DateTime;
import org.modeshape.jcr.value.DateTimeFactory;

/**
 * A component that records statistics for a variety of repository metrics, and makes the statistics available for a variety of
 * windows. The value metrics currently include:
 * <ol>
 * <li><b>{@link ValueMetric#SESSION_COUNT active sessions}</b> - the number of sessions that are open during the window;</li>
 * <li><b>{@link ValueMetric#QUERY_COUNT active queries}</b> - the number of queries that were executing during the window;</li>
 * <li><b>{@link ValueMetric#WORKSPACE_COUNT workspaces}</b> - the number of workspaces in existence during the window;</li>
 * <li><b>{@link ValueMetric#LISTENER_COUNT listeners}</b> - the number of listeners registered during the window;</li>
 * <li><b>{@link ValueMetric#SESSION_SCOPED_LOCK_COUNT session-scoped locks}</b> - the number of locks held by sessions during the
 * window;</li>
 * <li><b>{@link ValueMetric#NON_SCOPED_LOCK_COUNT non-scoped locks}</b> - the number of non-scoped locks held during the window;</li>
 * <li><b>{@link ValueMetric#SESSION_SAVES save operations}</b> - the number of Session save operations performed the window;</li>
 * <li><b>{@link ValueMetric#NODE_CHANGES changed nodes}</b> - the number of nodes that were created, updated, or deleted during
 * the window;</li>
 * </ol>
 * and the metrics that record durations include:
 * <ol>
 * <li><b>{@link DurationMetric#SESSION_LIFETIME session lifetime}</b> - the duration of the sessions closed during the window;</li>
 * <li><b>{@link DurationMetric#QUERY_EXECUTION_TIME query execution time}</b> - the duration of queries that finished during the
 * window;</li>
 * <li><b>{@link DurationMetric#SEQUENCER_EXECUTION_TIME sequencer execution time}</b> - the duration of sequencing operations
 * completed during the window;</li>
 * </ol>
 * This class provides a way to obtain the {@link History history} for a particular metric during a specified window, where the
 * window is comprised of the {@link Statistics statistics} (the average value, minimum value, maximum value, variance, standard
 * deviation, number of samples, and time interval of the statistics) for:
 * <ol>
 * <li>each ten 5-second intervals during the last minute (60 seconds); or</li>
 * <li>each minute during the last hour (60 minutes); or</li>
 * <li>each hour during the last day (24 hours); or</li>
 * <li>each day during the last week (7 days); or</li>
 * <li>each week during the last year (52 weeks)</li>
 * </ol>
 * <p>
 * To use, simply instantiate and {@link #start(ScheduledExecutorService) start} it by supplying a
 * {@link ScheduledExecutorService} instance, which is used to create a periodic task that runs every 5 seconds to roll up
 * measured metrics into the various statistic windows. When completed, simply call {@link #stop()} to have the object clean up
 * after itself.
 * </p>
 */
@ThreadSafe
public class RepositoryStatistics {

    /**
     * The metrics for which ModeShape captures statistics on durations.
     * 
     * @see RepositoryStatistics#getHistory(DurationMetric, Window)
     * @see RepositoryStatistics#getLongestRunning(DurationMetric)
     */
    public static enum DurationMetric {
        /**
         * The metric that captures the duration of {@link Query#execute() query executions}. Note that the payload of the
         * {@link DurationActivity} instances are the query strings.
         */
        QUERY_EXECUTION_TIME("query-execution-time"),
        /**
         * The metric that captures the duration of {@link Session sessions}. Note that the payload of the
         * {@link DurationActivity} instances are the session usern IDs.
         */
        SESSION_LIFETIME("session-lifetime"),
        /**
         * The metric that captures the duration of sequencer executions. Note that the payload of the {@link DurationActivity}
         * instances are strings containing the sequencer name and the input and output paths.
         */
        SEQUENCER_EXECUTION_TIME("sequencer-execution-time");

        private static final Map<String, DurationMetric> BY_LITERAL;
        private static final Map<String, DurationMetric> BY_NAME;
        static {
            Map<String, DurationMetric> byName = new HashMap<String, DurationMetric>();
            Map<String, DurationMetric> byLiteral = new HashMap<String, DurationMetric>();
            for (DurationMetric metric : DurationMetric.values()) {
                byLiteral.put(metric.getLiteral().toLowerCase(), metric);
                byName.put(metric.name().toLowerCase(), metric);
            }
            BY_LITERAL = Collections.unmodifiableMap(byLiteral);
            BY_NAME = Collections.unmodifiableMap(byName);
        }
        private final String literal;

        private DurationMetric( String literal ) {
            this.literal = literal;
        }

        /**
         * The literal string form of the duration metric.
         * 
         * @return the literal string form; never null or empty
         */
        public String getLiteral() {
            return literal;
        }

        /**
         * Get the {@link DurationMetric} that has the supplied literal.
         * 
         * @param literal the literal (can be of any case); may not be null
         * @return the duration metric, or null if there is no DurationMetric enum value for the given literal string
         */
        public static DurationMetric fromLiteral( String literal ) {
            if (literal == null) return null;
            literal = literal.toLowerCase();
            DurationMetric metric = BY_LITERAL.get(literal);
            if (metric == null) BY_NAME.get(literal);
            return metric;
        }
    }

    /**
     * The metrics for which ModeShape captures statistics on running values.
     * 
     * @see RepositoryStatistics#getHistory(ValueMetric, Window)
     */
    public static enum ValueMetric {
        /**
         * The metric that captures the number of {@link Session sessions} that are open during the window.
         */
        SESSION_COUNT("session-count"),
        /**
         * The metric that captures the number of {@link Query queries} that are executing during the window.
         */
        QUERY_COUNT("query-count"),
        /**
         * The metric that records the number of {@link Workspace workspaces} in existance during the window.
         */
        WORKSPACE_COUNT("workspace-count"),
        /**
         * The metric that records the number of {@link EventListener observation listeners} in existance during the window.
         */
        LISTENER_COUNT("listener-count"),
        /**
         * The metric that records the number of {@link Lock#isSessionScoped() session-scoped} {@link Lock JCR locks} in existance
         * during the window.
         */
        SESSION_SCOPED_LOCK_COUNT("session-scoped-lock-count"),
        /**
         * The metric that records the number of {@link Lock#isSessionScoped() non-session-scoped} {@link Lock JCR locks} in
         * existance during the window.
         */
        NON_SCOPED_LOCK_COUNT("non-scoped-lock-count"),
        /**
         * The metric that captures the number of {@link Session#save()} calls that have occurred during the window.
         */
        SESSION_SAVES("session-saves"),
        /**
         * The metric that captures the number of nodes that were created, updated, or deleted during the window as part of the
         * {@link Session#save()}, {@link VersionManager#checkin(String)},
         * {@link Workspace#importXML(String, java.io.InputStream, int) Workspace.import} and other calls that change content.
         */
        NODE_CHANGES("node-changes");

        private static final Map<String, ValueMetric> BY_LITERAL;
        private static final Map<String, ValueMetric> BY_NAME;
        static {
            Map<String, ValueMetric> byName = new HashMap<String, ValueMetric>();
            Map<String, ValueMetric> byLiteral = new HashMap<String, ValueMetric>();
            for (ValueMetric metric : ValueMetric.values()) {
                byLiteral.put(metric.getLiteral().toLowerCase(), metric);
                byName.put(metric.name().toLowerCase(), metric);
            }
            BY_LITERAL = Collections.unmodifiableMap(byLiteral);
            BY_NAME = Collections.unmodifiableMap(byName);
        }
        private final String literal;

        private ValueMetric( String literal ) {
            this.literal = literal;
        }

        /**
         * The literal string form of the value metric.
         * 
         * @return the literal string form; never null or empty
         */
        public String getLiteral() {
            return literal;
        }

        /**
         * Get the {@link ValueMetric} that has the supplied literal.
         * 
         * @param literal the literal (can be of any case); may not be null
         * @return the value metric, or null if there is no ValueMetric enum value for the given literal string
         */
        public static ValueMetric fromLiteral( String literal ) {
            if (literal == null) return null;
            literal = literal.toLowerCase();
            ValueMetric metric = BY_LITERAL.get(literal);
            if (metric == null) BY_NAME.get(literal);
            return metric;
        }
    }

    /**
     * The specification of the window for obtaining the history and statistics for a given metric.
     * 
     * @see RepositoryStatistics#getHistory(DurationMetric, Window)
     * @see RepositoryStatistics#getHistory(ValueMetric, Window)
     */
    public static enum Window {
        /**
         * The window for accessing a metric's history and statistics for ten 5-second intervals during the last minute (60
         * seconds).
         */
        PREVIOUS_60_SECONDS("previous-60-seconds"),
        /**
         * The window for accessing a metric's history and statistics for each minute during the last hour (60 minutes).
         */
        PREVIOUS_60_MINUTES("previous-60-minutes"),
        /**
         * The window for accessing a metric's history and statistics for each hour during the last day (24 hours).
         */
        PREVIOUS_24_HOURS("previous-24-hours"),
        /**
         * The window for accessing a metric's history and statistics for each day during the last week (7 days).
         */
        PREVIOUS_7_DAYS("previous-7-days"),
        /**
         * The window for accessing a metric's history and statistics for each week during the last year (52 weeks).
         */
        PREVIOUS_52_WEEKS("previous-52-wees");

        private static final Map<String, Window> BY_LITERAL;
        private static final Map<String, Window> BY_NAME;
        static {
            Map<String, Window> byLiteral = new HashMap<String, Window>();
            Map<String, Window> byName = new HashMap<String, Window>();
            for (Window window : Window.values()) {
                byLiteral.put(window.getLiteral().toLowerCase(), window);
                byName.put(window.name().toLowerCase(), window);
            }
            BY_LITERAL = Collections.unmodifiableMap(byLiteral);
            BY_NAME = Collections.unmodifiableMap(byName);
        }
        private final String literal;

        private Window( String literal ) {
            this.literal = literal;
        }

        /**
         * The literal string form of the window.
         * 
         * @return the literal string form; never null or empty
         */
        public String getLiteral() {
            return literal;
        }

        /**
         * Get the {@link Window} that has the supplied literal.
         * 
         * @param literal the literal (can be of any case); may not be null
         * @return the window, or null if there is no Window enum value for the given literal string
         */
        public static Window fromLiteral( String literal ) {
            if (literal == null) return null;
            literal = literal.toLowerCase();
            Window window = BY_LITERAL.get(literal);
            if (window == null) BY_NAME.get(literal);
            return window;
        }
    }

    /**
     * The maximum number of longest-running queries to retain.
     */
    public static final int MAXIMUM_LONG_RUNNING_QUERY_COUNT = 15;

    /**
     * The maximum number of longest-running sequencing operations to retain.
     */
    public static final int MAXIMUM_LONG_RUNNING_SEQUENCING_COUNT = 15;

    /**
     * The maximum number of longest-running sessions to retain. Note that all active sessions (those that are not logged out) are
     * excluded from this list.
     */
    public static final int MAXIMUM_LONG_RUNNING_SESSION_COUNT = 15;

    /**
     * The frequency at which the metric values are rolled into statistics.
     */
    public static final long CAPTURE_INTERVAL_IN_SECONDS = 5L;

    private static final long CAPTURE_DELAY_IN_SECONDS = 5L;

    protected static final long DURATION_OF_52_WEEKS_WINDOW_IN_SECONDS = TimeUnit.SECONDS.convert(MetricHistory.MAX_WEEKS,
                                                                                                  TimeUnit.DAYS) / 7;
    protected static final long DURATION_OF_7_DAYS_WINDOW_IN_SECONDS = TimeUnit.SECONDS.convert(MetricHistory.MAX_DAYS,
                                                                                                TimeUnit.DAYS);
    protected static final long DURATION_OF_24_HOURS_WINDOW_IN_SECONDS = TimeUnit.SECONDS.convert(MetricHistory.MAX_HOURS,
                                                                                                  TimeUnit.HOURS);
    protected static final long DURATION_OF_60_MINUTES_WINDOW_IN_SECONDS = TimeUnit.SECONDS.convert(MetricHistory.MAX_MINUTES,
                                                                                                    TimeUnit.MINUTES);
    protected static final long DURATION_OF_60_SECONDS_WINDOW_IN_SECONDS = TimeUnit.SECONDS.convert(MetricHistory.MAX_SECONDS
                                                                                                    * CAPTURE_INTERVAL_IN_SECONDS,
                                                                                                    TimeUnit.SECONDS);

    private static final DurationActivity[] NO_DURATION_RECORDS = new DurationActivity[0];
    private static final Statistics[] NO_STATISTICS = new Statistics[0];

    private final ConcurrentMap<DurationMetric, DurationHistory> durations = new ConcurrentHashMap<DurationMetric, DurationHistory>();
    private final ConcurrentMap<ValueMetric, ValueHistory> values = new ConcurrentHashMap<ValueMetric, ValueHistory>();
    private final AtomicReference<ScheduledFuture<?>> rollupFuture = new AtomicReference<ScheduledFuture<?>>();
    private final DateTimeFactory timeFactory;

    private final AtomicReference<DateTime> secondsStartTime = new AtomicReference<DateTime>();
    private final AtomicReference<DateTime> minutesStartTime = new AtomicReference<DateTime>();
    private final AtomicReference<DateTime> hoursStartTime = new AtomicReference<DateTime>();
    private final AtomicReference<DateTime> daysStartTime = new AtomicReference<DateTime>();
    private final AtomicReference<DateTime> weeksStartTime = new AtomicReference<DateTime>();

    RepositoryStatistics( ExecutionContext context ) {
        this.timeFactory = context.getValueFactories().getDateFactory();
    }

    /**
     * Start recording statistics.
     * 
     * @param service the executor service that should be used to capture the statistics; may not be null
     */
    void start( ScheduledExecutorService service ) {
        if (rollupFuture.get() != null) {
            // already started ...
            return;
        }

        // Pre-populate the metrics (overwriting any existing history object) ...
        durations.put(DurationMetric.QUERY_EXECUTION_TIME, new DurationHistory(TimeUnit.MILLISECONDS,
                                                                               MAXIMUM_LONG_RUNNING_QUERY_COUNT));
        durations.put(DurationMetric.SEQUENCER_EXECUTION_TIME, new DurationHistory(TimeUnit.MILLISECONDS,
                                                                                   MAXIMUM_LONG_RUNNING_SEQUENCING_COUNT));
        durations.put(DurationMetric.SESSION_LIFETIME, new DurationHistory(TimeUnit.MILLISECONDS,
                                                                           MAXIMUM_LONG_RUNNING_SESSION_COUNT));
        values.put(ValueMetric.SESSION_COUNT, new ValueHistory(false));
        values.put(ValueMetric.QUERY_COUNT, new ValueHistory(false));
        values.put(ValueMetric.WORKSPACE_COUNT, new ValueHistory(false));
        values.put(ValueMetric.LISTENER_COUNT, new ValueHistory(false));
        values.put(ValueMetric.SESSION_SCOPED_LOCK_COUNT, new ValueHistory(false));
        values.put(ValueMetric.NON_SCOPED_LOCK_COUNT, new ValueHistory(false));
        values.put(ValueMetric.SESSION_SAVES, new ValueHistory(true));
        values.put(ValueMetric.NODE_CHANGES, new ValueHistory(true));

        this.rollupFuture.set(service.scheduleAtFixedRate(new Runnable() {
            @SuppressWarnings( "synthetic-access" )
            @Override
            public void run() {
                rollup();
            }
        }, CAPTURE_DELAY_IN_SECONDS, CAPTURE_INTERVAL_IN_SECONDS, TimeUnit.SECONDS));
    }

    /**
     * Stop recording statistics.
     */
    void stop() {
        ScheduledFuture<?> future = this.rollupFuture.getAndSet(null);
        if (future != null && !future.isDone() && !future.isCancelled()) {
            // Stop running the scheduled job, letting any currently running rollup finish ...
            future.cancel(false);
        }
    }

    /**
     * Method called once every second by the scheduled job.
     * 
     * @see #start(ScheduledExecutorService)
     */
    @SuppressWarnings( "fallthrough" )
    private void rollup() {
        DateTime now = timeFactory.create();
        Window largest = null;
        for (DurationHistory history : durations.values()) {
            largest = history.rollup();
        }
        for (ValueHistory history : values.values()) {
            largest = history.rollup();
        }
        // Note that we do expect to fall through, as the 'largest' represents the largest window that was changed,
        // while all smaller windows were also changed ...
        switch (largest) {
            case PREVIOUS_52_WEEKS:
                this.weeksStartTime.set(now);
                // fall through!!
            case PREVIOUS_7_DAYS:
                this.daysStartTime.set(now);
                // fall through!!
            case PREVIOUS_24_HOURS:
                this.hoursStartTime.set(now);
                // fall through!!
            case PREVIOUS_60_MINUTES:
                this.minutesStartTime.set(now);
                // fall through!!
            case PREVIOUS_60_SECONDS:
                this.secondsStartTime.set(now);
        }
    }

    private final DateTime mostRecentTimeFor( Window window ) {
        switch (window) {
            case PREVIOUS_52_WEEKS:
                return this.weeksStartTime.get();
            case PREVIOUS_7_DAYS:
                return this.daysStartTime.get();
            case PREVIOUS_24_HOURS:
                return this.hoursStartTime.get();
            case PREVIOUS_60_MINUTES:
                return this.minutesStartTime.get();
            case PREVIOUS_60_SECONDS:
                return this.secondsStartTime.get();
        }
        throw new SystemFailureException("Should never happen");
    }

    /**
     * Get the statics for the specified value metric during the given window in time. The oldest statistics will be first, while
     * the newest statistics will be last.
     * 
     * @param metric the value metric; may not be null
     * @param windowInTime the window specifying which statistics are to be returned; may not be null
     * @return the history of the metrics; never null but possibly empty if there are no statistics being captures for this
     *         repository
     */
    public History getHistory( ValueMetric metric,
                               Window windowInTime ) {
        assert metric != null;
        assert windowInTime != null;
        ValueHistory history = values.get(metric);
        Statistics[] stats = history != null ? history.getHistory(windowInTime) : NO_STATISTICS;
        return new History(stats, mostRecentTimeFor(windowInTime), windowInTime);
    }

    /**
     * Get the statics for the specified duration metric during the given window in time. The oldest statistics will be first,
     * while the newest statistics will be last.
     * 
     * @param metric the duration metric; may not be null
     * @param windowInTime the window specifying which statistics are to be returned; may not be null
     * @return the history of the metrics; never null but possibly empty if there are no statistics being captures for this
     *         repository
     */
    public History getHistory( DurationMetric metric,
                               Window windowInTime ) {
        assert metric != null;
        assert windowInTime != null;
        DurationHistory history = durations.get(metric);
        Statistics[] stats = history != null ? history.getHistory(windowInTime) : NO_STATISTICS;
        return new History(stats, mostRecentTimeFor(windowInTime), windowInTime);
    }

    /**
     * Get the longest-running activities recorded for the specified metric. The results contain the duration records in order of
     * increasing duration, with the activity with the longest duration appearing last in the array.
     * 
     * @param metric the duration metric; may not be null
     * @return the activities with the longest durations; never null but possibly empty if no such activities were performed
     */
    public DurationActivity[] getLongestRunning( DurationMetric metric ) {
        assert metric != null;
        DurationHistory history = durations.get(metric);
        return history != null ? history.getLongestRunning() : NO_DURATION_RECORDS;
    }

    /**
     * Record an incremental change to a value, called by the code that knows when and how the metric changes.
     * 
     * @param metric the metric; may not be null
     * @param incrementalValue the positive or negative increment
     * @see #increment(ValueMetric)
     * @see #decrement(ValueMetric)
     * @see #recordDuration(DurationMetric, long, TimeUnit, String)
     */
    void increment( ValueMetric metric,
                    long incrementalValue ) {
        assert metric != null;
        ValueHistory history = values.get(metric);
        if (history != null) history.recordIncrement(incrementalValue);
    }

    /**
     * Record an increment in a value, called by the code that knows when and how the metric changes.
     * 
     * @param metric the metric; may not be null
     * @see #increment(ValueMetric, long)
     * @see #decrement(ValueMetric)
     * @see #recordDuration(DurationMetric, long, TimeUnit, String)
     */
    void increment( ValueMetric metric ) {
        assert metric != null;
        ValueHistory history = values.get(metric);
        if (history != null) history.recordIncrement(1L);
    }

    /**
     * Record a specific value for a metric, called by the code that knows when and how the metric changes.
     * 
     * @param metric the metric; may not be null
     * @param value the value for the metric
     * @see #increment(ValueMetric, long)
     * @see #decrement(ValueMetric)
     * @see #recordDuration(DurationMetric, long, TimeUnit, String)
     */
    void set( ValueMetric metric,
              long value ) {
        assert metric != null;
        ValueHistory history = values.get(metric);
        if (history != null) history.recordNewValue(1L);
    }

    /**
     * Record an decrement in a value, called by the code that knows when and how the metric changes.
     * 
     * @param metric the metric; may not be null
     * @see #increment(ValueMetric)
     * @see #increment(ValueMetric, long)
     * @see #recordDuration(DurationMetric, long, TimeUnit, String)
     */
    void decrement( ValueMetric metric ) {
        assert metric != null;
        ValueHistory history = values.get(metric);
        if (history != null) history.recordIncrement(-1L);
    }

    /**
     * Record a new duration for the given metric, called by the code that knows about the duration.
     * 
     * @param metric the metric; may not be null
     * @param duration the duration
     * @param timeUnit the time unit of the duration
     * @param payload the payload; may be null or a lightweight representation of the activity described by the duration
     * @see #increment(ValueMetric)
     * @see #increment(ValueMetric, long)
     * @see #decrement(ValueMetric)
     */
    void recordDuration( DurationMetric metric,
                         long duration,
                         TimeUnit timeUnit,
                         String payload ) {
        assert metric != null;
        DurationHistory history = durations.get(metric);
        if (history != null) history.recordDuration(duration, timeUnit, payload);
    }

    /**
     * Abstract base class for the {@link ValueHistory} and {@link DurationHistory} classes. This class tracks the statistics for
     * various periods of time, and to roll up the statistics. The design takes advantage of the fact that we know up front how
     * many statistics to keep for each {@link Window window}, and uses a fixed-size array as a ring of ordered values. It also is
     * designed to minimize lock contention.
     * <p>
     * Each instance of this class consumes 5x4bytes for the integer couters, 5x8bytes for the references to the arrays, and
     * 143x8byte references to the Statistics objects (for a total of 1204 bytes). Each Statistics instances consumes 36 bytes
     * (1x4byte integer, 2x8byte longs, 2x8byte doubles), so 149 instances consumes 5364bytes. Thus the total memory required for
     * a single MetricHistory instance is 6568 bytes (6.41kB).
     * </p>
     * <p>
     * So ten metrics require 64.1kB of memory, assuming the repository has been running for 52 weeks. (If not, then not all of
     * the Statistics arrays will be filled.)
     * </p>
     */
    @ThreadSafe
    protected static abstract class MetricHistory {

        private static final int MAX_SECONDS = 60 / (int)CAPTURE_INTERVAL_IN_SECONDS;
        private static final int MAX_MINUTES = 60;
        private static final int MAX_HOURS = 24;
        private static final int MAX_DAYS = 7;
        private static final int MAX_WEEKS = 52;

        private final Statistics[] seconds = new Statistics[MAX_SECONDS];
        private final Statistics[] minutes = new Statistics[MAX_HOURS];
        private final Statistics[] hours = new Statistics[MAX_HOURS];
        private final Statistics[] days = new Statistics[MAX_DAYS];
        private final Statistics[] weeks = new Statistics[MAX_WEEKS];

        private int currentSecond = 0;
        private int currentMinute = 0;
        private int currentHour = 0;
        private int currentDay = 0;
        private int currentWeek = 0;

        /**
         * A read-write lock around the Statistics[]. It might be possible if this single lock proves to be a bottleneck (really
         * only when getting history stops the rolling up of new statistics). We use fair locks to minimize the wait time for any
         * readers and writers.
         */
        private final ReadWriteLock lock = new ReentrantReadWriteLock(false);

        /**
         * Method to convert the values/durations captured within the last second and roll them into the history. This method
         * should be called only by {@link RepositoryStatistics#rollup()}.
         * 
         * @return the largest window that was modified; never null
         */
        abstract Window rollup();

        /**
         * This method should be called by the {@link #rollup()} method with the statistics computed for the values/durations
         * since the last invocation.
         * 
         * @param stats the new statistics for the most recent second; may not be null
         * @return the largest window that was modified; never null
         */
        protected Window recordStatisticsForLastSecond( Statistics stats ) {
            java.util.concurrent.locks.Lock lock = this.lock.writeLock();
            try {
                lock.lock();
                seconds[currentSecond++] = stats;
                if (currentSecond == MAX_SECONDS) {
                    // The we've finished filling the seconds, so reset the index
                    // and roll the seconds statistics into a new cumulative statistic for the last minute ...
                    currentSecond = 0;
                    Statistics cumulative = statisticsFor(seconds);
                    minutes[currentMinute++] = cumulative;
                    if (currentMinute == MAX_MINUTES) {
                        // The we've finished filling the minutes, so reset the index
                        // and roll the minutes statistics into a new cumulative statistic for the last hour ...
                        currentMinute = 0;
                        cumulative = statisticsFor(minutes);
                        hours[currentHour++] = cumulative;
                        if (currentHour == MAX_HOURS) {
                            // The we've finished filling the hours, so reset the index
                            // and roll the hour statistics into a new cumulative statistic for the last day ...
                            currentHour = 0;
                            cumulative = statisticsFor(hours);
                            days[currentDay++] = cumulative;
                            if (currentDay == MAX_DAYS) {
                                // The we've finished filling the days, so reset the index
                                // and roll the days statistics into a new cumulative statistic for the last week ...
                                currentDay = 0;
                                cumulative = statisticsFor(days);
                                weeks[currentWeek++] = cumulative;
                                if (currentWeek == MAX_WEEKS) {
                                    // The we've finished filling the weeks, so reset the index ...
                                    currentWeek = 0;
                                }
                                return Window.PREVIOUS_52_WEEKS;
                            }
                            return Window.PREVIOUS_7_DAYS;
                        }
                        return Window.PREVIOUS_24_HOURS;
                    }
                    return Window.PREVIOUS_60_MINUTES;
                }
                return Window.PREVIOUS_60_SECONDS;
            } finally {
                lock.unlock();
            }
        }

        protected Statistics[] getHistory( Window window ) {
            java.util.concurrent.locks.Lock lock = this.lock.readLock();
            try {
                lock.lock();
                switch (window) {
                    case PREVIOUS_60_SECONDS:
                        return copyStatistics(seconds, currentSecond);
                    case PREVIOUS_60_MINUTES:
                        return copyStatistics(minutes, currentMinute);
                    case PREVIOUS_24_HOURS:
                        return copyStatistics(hours, currentHour);
                    case PREVIOUS_7_DAYS:
                        return copyStatistics(days, currentDay);
                    case PREVIOUS_52_WEEKS:
                        return copyStatistics(weeks, currentWeek);
                }
                return null;
            } finally {
                lock.unlock();
            }
        }

        private final Statistics[] copyStatistics( Statistics[] stats,
                                                   int startingIndex ) {
            final int size = stats.length;
            Statistics[] results = new Statistics[size];
            if (startingIndex == 0) {
                // Copy the full array in one shot ...
                System.arraycopy(stats, 0, results, 0, size);
            } else {
                // Copy the array in two parts ...
                final int numAfterStartingIndex = size - startingIndex;
                final int numBeforeStartingIndex = size - numAfterStartingIndex;
                System.arraycopy(stats, startingIndex, results, 0, numAfterStartingIndex);
                System.arraycopy(stats, 0, results, numAfterStartingIndex, numBeforeStartingIndex);
            }
            return results;
        }
    }

    /**
     * The {@link MetricHistory} specialization used for recording the statistics for running values.
     */
    @ThreadSafe
    protected static final class ValueHistory extends MetricHistory {
        private final AtomicLong value = new AtomicLong();
        private final boolean resetCounterUponRollup;

        protected ValueHistory( boolean resetCounterUponRollup ) {
            this.resetCounterUponRollup = resetCounterUponRollup;
        }

        void recordIncrement( long increment ) {
            this.value.addAndGet(increment);
        }

        void recordNewValue( long value ) {
            this.value.set(value);
        }

        @Override
        Window rollup() {
            long value = resetCounterUponRollup ? this.value.getAndSet(0L) : this.value.get();
            return recordStatisticsForLastSecond(statisticsFor(value));
        }
    }

    /**
     * The {@link MetricHistory} specialization used for recording the statistics for activities with measured durations.
     */
    @Immutable
    public static final class DurationActivity implements Comparable<DurationActivity> {
        protected final long duration;
        protected final String payload;
        protected final TimeUnit timeUnit;

        protected DurationActivity( long duration,
                                    TimeUnit timeUnit,
                                    String payload ) {
            this.duration = duration;
            this.payload = payload;
            this.timeUnit = timeUnit;
        }

        /**
         * Get the duration of this activity.
         * 
         * @param unit the desired time unit for the duration
         * @return the duration in the specified time unit
         */
        public long getDuration( TimeUnit unit ) {
            return unit.convert(duration, this.timeUnit);
        }

        /**
         * Get the payload for this activity.
         * 
         * @return the payload; may be null
         */
        public String getPayload() {
            return payload;
        }

        @Override
        public int compareTo( DurationActivity that ) {
            if (this == that) return 0;
            // Return the opposite of natural ordering, so smallest durations come first ...
            return (int)(this.duration - that.duration);
        }

        @Override
        public String toString() {
            return "Duration: " + getDuration(TimeUnit.SECONDS) + " sec, " + payload;
        }
    }

    @ThreadSafe
    protected static final class DurationHistory extends MetricHistory {
        private final Queue<DurationActivity> duration1 = new ConcurrentLinkedQueue<DurationActivity>();
        private final Queue<DurationActivity> duration2 = new ConcurrentLinkedQueue<DurationActivity>();
        private final AtomicReference<Queue<DurationActivity>> durations = new AtomicReference<Queue<DurationActivity>>();
        private final TimeUnit timeUnit;
        private final int retentionSize;
        private final PriorityBlockingQueue<DurationActivity> largestDurations;

        protected DurationHistory( TimeUnit timeUnit,
                                   int retentionSize ) {
            assert retentionSize > 0;
            this.durations.set(duration1);
            this.timeUnit = timeUnit;
            this.retentionSize = retentionSize;
            this.largestDurations = new PriorityBlockingQueue<RepositoryStatistics.DurationActivity>(this.retentionSize + 5);
        }

        /**
         * Record a new duration. This method should be as fast as possible, since it is called within production code.
         * 
         * @param value the duration
         * @param timeUnit the time unit; may not be null
         * @param payload a payload that should be tracked with this duration if it is among the largest
         */
        void recordDuration( long value,
                             TimeUnit timeUnit,
                             String payload ) {
            value = this.timeUnit.convert(value, timeUnit);
            this.durations.get().add(new DurationActivity(value, this.timeUnit, payload));
        }

        @Override
        Window rollup() {
            // Swap the queue (which should work, since we should be the only concurrent thread doing this) ...
            Queue<DurationActivity> durations = null;
            if (this.durations.weakCompareAndSet(duration1, duration2)) {
                durations = duration1;
            } else {
                this.durations.weakCompareAndSet(duration2, duration1);
                durations = duration2;
            }
            // Make a copy to minimize the time spent using the durations ...
            List<DurationActivity> records = new ArrayList<DurationActivity>(durations);
            durations.clear();

            // Now add to the largest durations and compute the statistics ...
            int numRecords = records.size();
            long[] values = new long[numRecords];
            int i = 0;
            for (DurationActivity record : records) {
                values[i++] = record != null ? record.duration : 0L;
                this.largestDurations.add(record);
                while (this.largestDurations.size() > this.retentionSize) {
                    this.largestDurations.poll(); // remove the smallest duration from the front of the queue
                }
            }

            Statistics stats = statisticsFor(values);
            return recordStatisticsForLastSecond(stats);
        }

        DurationActivity[] getLongestRunning() {
            List<DurationActivity> records = new ArrayList<DurationActivity>(this.largestDurations);
            return records.toArray(new DurationActivity[records.size()]);
        }
    }

    /**
     * Utility method to construct the statistics for a series of values.
     * 
     * @param value the single value
     * @return the core statistics; never null
     */
    public static Statistics statisticsFor( long value ) {
        return new Statistics(1, value, value, value, 0.0d);
    }

    /**
     * Utility method to construct the statistics for a series of values.
     * 
     * @param values the values; the array reference may not be null but the array may be empty
     * @return the core statistics; never null
     */
    public static Statistics statisticsFor( long[] values ) {
        int length = values.length;
        if (length == 0) return EMPTY_STATISTICS;
        if (length == 1) return statisticsFor(values[0]);
        long total = 0L;
        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;
        for (long value : values) {
            total += value;
            max = Math.max(max, value);
            min = Math.min(min, value);
        }
        double mean = ((double)total) / length;
        double varianceSquared = 0.0d;
        double distance = 0.0d;
        for (long value : values) {
            distance = mean - value;
            varianceSquared = varianceSquared + (distance * distance);
        }
        return new Statistics(length, min, max, mean, Math.sqrt(varianceSquared));
    }

    /**
     * Utility method to construct the composite statistics for a series of sampled statistics.
     * 
     * @param statistics the sample statistics that are to be combined; the array reference may not be null but the array may be
     *        empty
     * @return the composite statistics; never null
     */
    public static Statistics statisticsFor( Statistics[] statistics ) {
        int length = statistics.length;
        if (length == 0) return EMPTY_STATISTICS;
        if (length == 1) return statistics[0] != null ? statistics[0] : EMPTY_STATISTICS;
        int count = 0;
        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;
        double mean = 0.0d;
        double variance = 0.0d;
        // Compute the min, max, and mean ...
        for (Statistics stat : statistics) {
            if (stat == null) continue;
            count += stat.getCount();
            max = Math.max(max, stat.getMaximum());
            min = Math.min(min, stat.getMinimum());
            mean = mean + (stat.getMean() * stat.getCount());
        }
        mean = mean / count;

        // Compute the new variance using the new mean ...
        double meanDelta = 0.0d;
        for (Statistics stat : statistics) {
            if (stat == null) continue;
            meanDelta = stat.getMean() - mean;
            variance = variance + (stat.getCount() * (stat.getVariance() + (meanDelta * meanDelta)));
        }
        return new Statistics(count, min, max, mean, variance);
    }

    private static final Statistics EMPTY_STATISTICS = new Statistics(0, 0L, 0L, 0.0d, 0.0d);

    /**
     * The statistics for a sample of values. The statistics include the {@link #getMinimum() minimum}, {@link #getMaximum()
     * maximum}, {@link #getMean() mean (average)}, {@link #getVariance() variance} and {@link #getStandardDeviation() standard
     * deviation}.
     * <p>
     * The median value is not included in these statistics, since the median value cannot be rolled up given a series of
     * statistics without having the original values. It is possible to compute the weighted median, but this loses
     * effectiveness/value the more times it is rolled up.
     * </p>
     */
    @Immutable
    public static final class Statistics {

        private final int count;
        private final long maximum;
        private final long minimum;
        private final double mean;
        private final double variance; // just the square of the standard deviation

        protected Statistics( int count,
                              long max,
                              long min,
                              double mean,
                              double variance ) {
            this.count = count;
            this.maximum = max;
            this.minimum = min;
            this.mean = mean;
            this.variance = variance;
        }

        /**
         * Get the number of samples to which these statistics apply.
         * 
         * @return the number of samples; never negative
         */
        public int getCount() {
            return count;
        }

        /**
         * Get the maximum of the sampled values.
         * 
         * @return the maximum value
         */
        public long getMaximum() {
            return maximum;
        }

        /**
         * Get the minimum of the sampled values.
         * 
         * @return the minimum value
         */
        public long getMinimum() {
            return minimum;
        }

        /**
         * The mean (or average) of the sampled values. This is returned as a double to reduce the lost of precision.
         * 
         * @return the mean or average value
         */
        public double getMean() {
            return mean;
        }

        /**
         * Get the variance of the sampled values, which is the average of the squared differences from the {@link #getMean()
         * mean}.
         * 
         * @return the variance; never negative
         */
        public double getVariance() {
            return variance;
        }

        /**
         * Get the standard deviation of the sampled values, which is a measure of how spread out the numbers are and is the
         * square root of the {@link #getVariance() variance}.
         * 
         * @return the standard deviation; never negative
         */
        public double getStandardDeviation() {
            return variance <= 0.0d ? 0.0d : Math.sqrt(variance);
        }

        @Override
        public String toString() {
            long count = this.getCount();
            String samples = Inflector.getInstance().pluralize("sample", count > 1L ? 2 : 1);
            return StringUtil.createString("{0} {1}: min={2}; avg={3}; max={4}; dev={5}",
                                           count,
                                           samples,
                                           this.minimum,
                                           this.mean,
                                           this.maximum,
                                           this.getStandardDeviation());
        }
    }

    /**
     * A history of a metric for a given window in time.
     * 
     * @see RepositoryStatistics#getHistory(DurationMetric, Window)
     * @see RepositoryStatistics#getHistory(ValueMetric, Window)
     */
    @Immutable
    public static final class History {
        private final Statistics[] stats;
        private final DateTime endTime;
        private final Window window;

        protected History( Statistics[] stats,
                           DateTime endTime,
                           Window window ) {
            this.stats = stats;
            this.endTime = endTime;
            this.window = window;
        }

        /**
         * Get the kind of window.
         * 
         * @return the window type; never null
         */
        public Window getWindow() {
            return window;
        }

        /**
         * Get the total duration of this history window.
         * 
         * @param unit the desired time unit; if null, then {@link TimeUnit#SECONDS} is used
         * @return the duration
         */
        public long getTotalDuration( TimeUnit unit ) {
            if (unit == null) unit = TimeUnit.SECONDS;
            switch (window) {
                case PREVIOUS_52_WEEKS:
                    return unit.convert(DURATION_OF_52_WEEKS_WINDOW_IN_SECONDS, TimeUnit.SECONDS);
                case PREVIOUS_7_DAYS:
                    return unit.convert(DURATION_OF_7_DAYS_WINDOW_IN_SECONDS, TimeUnit.SECONDS);
                case PREVIOUS_24_HOURS:
                    return unit.convert(DURATION_OF_24_HOURS_WINDOW_IN_SECONDS, TimeUnit.SECONDS);
                case PREVIOUS_60_MINUTES:
                    return unit.convert(DURATION_OF_60_MINUTES_WINDOW_IN_SECONDS, TimeUnit.SECONDS);
                case PREVIOUS_60_SECONDS:
                    return unit.convert(DURATION_OF_60_SECONDS_WINDOW_IN_SECONDS, TimeUnit.SECONDS);
            }
            throw new SystemFailureException("Should never happen");
        }

        /**
         * Get the timestamp (including time zone information) at which this history window starts.
         * 
         * @return the time at which this window starts
         */
        public DateTime getStartTime() {
            return endTime.minus(getTotalDuration(TimeUnit.SECONDS), TimeUnit.SECONDS);
        }

        /**
         * Get the timestamp (including time zone information) at which this history window ends.
         * 
         * @return the time at which this window ends
         */
        public DateTime getEndTime() {
            return endTime;
        }

        /**
         * Get the statistics for that make up the history.
         * 
         * @return the statistics; never null, but the array may contain null if the window is longer than the lifetime of the
         *         repository
         */
        public Statistics[] getStats() {
            return stats;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            long width = 0L;
            switch (window) {
                case PREVIOUS_52_WEEKS:
                    sb.append("Previous 52 weeks");
                    width = TimeUnit.SECONDS.convert(7, TimeUnit.DAYS);
                    break;
                case PREVIOUS_7_DAYS:
                    sb.append("Previous 7 days");
                    width = TimeUnit.SECONDS.convert(1, TimeUnit.DAYS);
                    break;
                case PREVIOUS_24_HOURS:
                    sb.append("Previous 24 hours");
                    width = TimeUnit.SECONDS.convert(1, TimeUnit.HOURS);
                    break;
                case PREVIOUS_60_MINUTES:
                    sb.append("Previous 60 minutes");
                    width = TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES);
                    break;
                case PREVIOUS_60_SECONDS:
                    sb.append("Previous 60 seconds");
                    width = TimeUnit.SECONDS.convert(CAPTURE_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
                    break;
            }
            DateTime startTime = getStartTime();
            sb.append(", starting at ");
            sb.append(startTime);
            sb.append(" and ending at ");
            sb.append(getEndTime());
            int i = 0;
            for (Statistics stat : stats) {
                ++i;
                if (stat == null) continue;
                sb.append("\n  ");
                sb.append(stat);
                sb.append("  at  ");
                sb.append(startTime.plus(i * width, TimeUnit.SECONDS));
            }
            return sb.toString();
        }
    }
}
