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
 * This is a generator of simple unique and (non-monotonically) increasing long-value keys that incorporate the time in which they
 * were generated. A single generator instance will never return two identical keys, and all keys are unique and naturally sorted
 * in time. The generator is safe to be called concurrently from multiple threads.
 * <p>
 * Each long key value contains the time (in milliseconds) at which the key is generated, left-shifted by 16 bits. Those 16 bits
 * are then used to store a counter value that is unique for each millisecond. When a new key is needed, the current time is
 * compared to the last time for which a key was generated. If the times are different, then the counter is reset to 0; otherwise,
 * the counter is incremented.
 * </p>
 * <p>
 * Use a single generator instance for each sequence of keys.
 * </p>
 * <p>
 * To obtain a unique value, simply call:
 * 
 * <pre>
 * TimeBasedKeys keys = TimeBasedKeys.create();
 * ...
 * long key = keys.nextKey();
 * </pre>
 * 
 * Because the keys are time-based, the generator can also identify the range of keys that were created before or after a given
 * instant in time, or within a range of times. For example, all keys obtained after January 10, 2014 at 12:12:41.845-06:00 (which
 * has a <code>System.currentTimeMillis()</code> value of {@code 1389378406}) will be greater than or equal to the following
 * value:
 * 
 * <pre>
 * long timeInMillis = 1389378406L;
 * long minKey = keys.getKeyStartingAt(timeInMillis);
 * </pre>
 * 
 * Using this and similar methods, one can obtain all counter values that might have been generated, for example, sometime during
 * 2012:
 * 
 * <pre>
 * long janFirst2012 = 1325397600L; // Jan 1 2012 at 00:00.000 UTC
 * long janFirst2013 = 1357020000L; // Jan 1 2013 at 00:00.000 UTC
 * long smallestKey = keys.getKeyStartingAt(janFirst2012);
 * long justLargerKey = keys.getKeyStartingAt(janFirst2013);
 * </pre>
 * 
 * Then all keys generated during 2012 will therefore satisfy:
 * 
 * <pre>
 * smallestKey &gt;= key &amp;&amp; key &lt; justLargerKey
 * </pre>
 * 
 * </p>
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
@ThreadSafe
public final class TimeBasedKeys {

    /**
     * By default each {@link TimeBasedKeys} instance will use 16 bits for the counter. That results in a maximum of of 65535
     * distinct values per millisecond (which is likely sufficient), while also leaving enough bits in the long to store a value
     * well past the year 10,000.
     */
    public static final short DEFAULT_BITS_IN_COUNTER = 16;

    /**
     * Create a new generator that uses 16 bits for the counter portion of the keys.
     * 
     * @return the generator instance; never null
     */
    public static TimeBasedKeys create() {
        return new TimeBasedKeys(DEFAULT_BITS_IN_COUNTER);
    }

    /**
     * Create a new generator that uses the specified number of bits for the counter portion of the keys.
     * 
     * @param bitsUsedInCounter the number of bits in the counter portion of the keys; must be a positive number for which theere
     * is enough space to left shift without overflowing.
     * @return the generator instance; never null
     */
    public static TimeBasedKeys create( int bitsUsedInCounter ) {
        CheckArg.isPositive(bitsUsedInCounter, "bitsUsedInCounter");
        int maxAvailableBitsToShift = Long.numberOfLeadingZeros(System.currentTimeMillis());
        CheckArg.isLessThan(bitsUsedInCounter, maxAvailableBitsToShift, "bitsUsedInCounter");
        return new TimeBasedKeys((short)bitsUsedInCounter);
    }

    /**
     * The number of bits used in the counter portion of the key.
     */
    private final short counterBits;

    /**
     * The maximum counter portion of the key given the number of {@link #counterBits}.
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
     * Create a new key generator.
     * 
     * @param bitsUsedInCounter the number of bits to be used in the counter.
     */
    protected TimeBasedKeys( short bitsUsedInCounter ) {
        this.counterBits = bitsUsedInCounter;
        this.maximumCounterValue = (1L << bitsUsedInCounter) - 1L;
    }

    /**
     * Get the next key for the current time in UTC.
     * 
     * @return a long that is determined by the current time in UTC and a unique counter value for the current time.
     */
    public long nextKey() {
        // Note that per Oracle the currentTimeMillis is the current number of seconds past the epoch
        // in UTC (not in local time). Therefore, processes with exactly synchronized clocks will
        // always get the same value regardless of their timezone ...
        final long timestamp = System.currentTimeMillis();
        final int increment = counterFor(timestamp);
        if (increment <= maximumCounterValue) {
            return (timestamp << counterBits) + increment;
        }
        // The counter is surprisingly too high, so try again (repeatedly) until we get to the next millisecond ...
        return this.nextKey();
    }

    /**
     * Obtain the first (earliest) key that would have been generated <em>at</em> the specified UTC time. The resulting key is
     * equal to or smaller than all keys generated at or since that time.
     * 
     * @param millisInUtc the number of milliseconds (in UTC) past epoch, and the time at which {@link #nextKey()} might have been
     *        called
     * @return a long value that is the earliest possible key for the given time
     */
    public long getCounterStartingAt( long millisInUtc ) {
        return (millisInUtc << counterBits);
    }

    /**
     * Obtain the largest (latest) key that would have been generated <em>at</em> the specified UTC time. The resulting key is
     * equal to or greater than all keys generated at or before that time.
     * 
     * @param millisInUtc the number of milliseconds (in UTC) past epoch, and the time at which {@link #nextKey()} might have been
     *        called
     * @return a long value that is the latest possible key for the given time
     */
    public long getCounterEndingAt( long millisInUtc ) {
        return (millisInUtc << counterBits) + maximumCounterValue;
    }

    /**
     * Obtain the first (earliest) key that would have been generated <em>after</em> the specified UTC time. The resulting key is
     * greater than all keys generated at or before that time.
     * 
     * @param millisInUtc the number of milliseconds (in UTC) past epoch, and the time at which {@link #nextKey()} might have been
     *        called
     * @return a long value that is the latest possible key for the given time
     */
    public long getCounterEndingAfter( long millisInUtc ) {
        return (millisInUtc + 1) << counterBits;
    }

    /**
     * Obtain the milliseconds since epoch in UTC that the supplied key was generated. The value is the same value that would have
     * been returned by {@link System#currentTimeMillis()} when the key was generated.
     * 
     * @param key the key
     * @return the generated time, in millseconds past epoch
     */
    public long getTimeGenerated( long key ) {
        return key < maximumCounterValue ? 0 : key >> counterBits;
    }

    /**
     * Determine the counter portion of the key given the supplied time. This method is synchronized to ensure that the
     * {@link #counter} and {@link #lastMillis} are updated atomically.
     * 
     * @param timestamp the current timestamp, and the new value for the {@link #lastMillis} field
     * @return the next available counter for the given timestamp
     */
    private synchronized int counterFor( long timestamp ) {
        if (timestamp == lastMillis) {
            // Just increment the counter...
            counter += 1;
        } else {
            // Otherwise, the timestamp has changed, so set it and reset the counter ...
            lastMillis = timestamp;
            counter = 0;
        }
        return counter;
    }
}
