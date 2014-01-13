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
package org.modeshape.common.util;

import org.modeshape.common.annotation.GuardedBy;
import org.modeshape.common.annotation.ThreadSafe;

/**
 * This is a simple unique and (non-monotonically) increasing counter that is based upon the current time in milliseconds. This
 * object will never return two identical values on the local machine, yet is safe to be called concurrently from multiple
 * threads. The resulting counter values can be used as unique keys that are naturally sorted by time.
 * <p>
 * This works by left-shifting the actual current time in millis by 16 bits, and then using these 16 bits to store a unique
 * counter value for each millisecond. When a new counter value is needed, the current time is compared to the last time for which
 * a value was generated. If the times are different, then the counter is reset to 0; otherwise, the counter is advanced.
 * </p>
 * <p>
 * To obtain a unique value, simply call:
 * 
 * <pre>
 * long value = counter.next();
 * </pre>
 * 
 * This time-based counter also has the advantage of being able to identify the range of values that were created before or during
 * a given instant in time. For example, all counter values obtained after January 10, 2014 at 12:12:41.845-06:00 (which has a
 * <code>System.currentTimeMillis()</code> value of {@code 1389378406}) will be greater than or equal to the following value:
 * 
 * <pre>
 * long timeInMillis = 1389378406L;
 * long minValue = counter.getCounterStartingAt(timeInMillis);
 * </pre>
 * 
 * Using this and similar methods, one can obtain all counter values that might have been generated, for example, sometime during
 * 2012:
 * 
 * <pre>
 * long janFirst2012 = 1325397600L; // Jan 1 2012 at 00:00.000 UTC
 * long janFirst2013 = 1357020000L; // Jan 1 2013 at 00:00.000 UTC
 * long smallest = counter.getCounterStartingAt(janFirst2012);
 * long justLarger = counter.getCounterStartingAt(janFirst2013);
 * </pre>
 * 
 * Then all counter values generated during 2012 will satisfy:
 * 
 * <pre>
 * smallest &gt;= value &amp;&amp; value &lt; justLarger
 * </pre>
 * 
 * </p>
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
@ThreadSafe
public final class TimeBasedCounter {

    /**
     * By default each {@link TimeBasedCounter} instance will use 16 bits for the counter. That results in a maximum counter value
     * of 65535 distinct values per millisecond (which is likely sufficient), while also leaving enough bits in the long to store
     * a value well past the year 10,000.
     */
    public static final short DEFAULT_BITS_IN_COUNTER = 16;

    public static TimeBasedCounter createCounter() {
        return new TimeBasedCounter(DEFAULT_BITS_IN_COUNTER);
    }

    public static TimeBasedCounter createCounter( int bitsUsedInCounter ) {
        return new TimeBasedCounter((short)bitsUsedInCounter);
    }

    /**
     * The number of bits used in the counter portion of the long values.
     */
    private final short counterBits;

    /**
     * The maximum counter value given the number of {@link #counterBits}.
     */
    private final long maximumCounterValue;

    /**
     * The last millis in UTC that was seen. This is only accessed and modified within the synchronized {@link #counterFor(long)}
     * method.
     */
    @GuardedBy( "this" )
    private volatile long lastMillis;

    /**
     * The last counter that was used with the current value of {@link #lastMillis}. This is only accessed and modified within the
     * synchronized {@link #counterFor(long)} method.
     */
    @GuardedBy( "this" )
    private volatile int counter;

    /**
     * Create a new counter instance.
     * 
     * @param bitsUsedInCounter the number of bits to be used in the counter.
     */
    protected TimeBasedCounter( short bitsUsedInCounter ) {
        this.counterBits = bitsUsedInCounter;
        this.maximumCounterValue = (1L << bitsUsedInCounter) - 1L;
    }

    /**
     * Get the next counter value for the current time in UTC.
     * 
     * @return a long that is determined by the current time in UTC and a unique counter value for the current time.
     */
    public long next() {
        // Note that per Oracle the currentTimeMillis is the current number of seconds past the epoch
        // in UTC (not in local time). Therefore, processes with exactly synchronized clocks will
        // always get the same value regardless of their timezone ...
        final long timestamp = System.currentTimeMillis();
        final long increment = counterFor(timestamp);
        if (increment <= maximumCounterValue) {
            return (timestamp << counterBits) + increment;
        }
        // The counter is surprisingly too high, so try again (repeatedly) until we get to the next millisecond ...
        return this.next();
    }

    /**
     * Obtain the first (earliest) counter value that would have been generated at the specified UTC time. This counter value is
     * equal to or smaller than all counter values generated at or since that time.
     * 
     * @param millisInUtc the number of milliseconds (in UTC) past epoch, and the time at which {@link #next()} might have been
     *        called
     * @return a long value that is the earliest possible counter value for the given time
     */
    public long getCounterStartingAt( long millisInUtc ) {
        return (millisInUtc << counterBits);
    }

    /**
     * Obtain the largest (latest) counter value that would have been generated at the specified UTC time. This counter value is
     * equal to or greater than all counter values generated at or before that time.
     * 
     * @param millisInUtc the number of milliseconds (in UTC) past epoch, and the time at which {@link #next()} might have been
     *        called
     * @return a long value that is the latest possible counter value for the given time
     */
    public long getCounterEndingAt( long millisInUtc ) {
        return (millisInUtc << counterBits) + maximumCounterValue;
    }

    /**
     * Obtain the first (earliest) counter value that would have been generated after the specified UTC time. This counter value
     * is greater than all counters generated at or before that time.
     * 
     * @param millisInUtc the number of milliseconds (in UTC) past epoch, and the time at which {@link #next()} might have been
     *        called
     * @return a long value that is the latest possible counter value for the given time
     */
    public long getCounterEndingAfter( long millisInUtc ) {
        return (millisInUtc + 1) << counterBits;
    }

    private synchronized int counterFor( long offsetTimestamp ) {
        if (offsetTimestamp == lastMillis) {
            // Just increment the counter and return ...
            return ++counter;
        }
        // Otherwise, the timestamp has changed, so set it and reset the counter ...
        lastMillis = offsetTimestamp;
        counter = 0;
        return counter;
    }
}
