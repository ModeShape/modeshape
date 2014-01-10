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
package org.modeshape.common.util;

import org.modeshape.common.annotation.GuardedBy;
import org.modeshape.common.annotation.ThreadSafe;

/**
 * This is a simple unique and (non-monotonically) increasing counter that is based upon the current time in milliseconds. This
 * object will never return two identical values on the local machine. Additionally, the resulting counter can be used to sort
 * based upon time. And, since the resulting counter values are purely a function of the time at which they are {@link #next()
 * created}, one can always obtain the range of counter values for a given time period.
 * <p>
 * For example, all counter values obtained after January 10, 2014 at 12:12:41.845-06:00 (which has a
 * <code>System.currentTimeMillis()</code> value of {@code 1389377561845}) will be greater than or equal to the following value:
 * 
 * <pre>
 * long timeInMillis = 1389377561845L;
 * long minValue = counter.getEarliestCounterForTime(timeInMillis);
 * </pre>
 * 
 * Meanwhile, all counter values obtained prior to this same instant in time will be less than the following value:
 * 
 * <pre>
 * long greatThanMaxValue = counter.getLatestExclusiveCounterForTime(timeInMillis);
 * </pre>
 * 
 * and less than or equal to the following value:
 * 
 * <pre>
 * long maxValue = counter.getLatestInclusiveCounterForTime(timeInMillis);
 * </pre>
 * 
 * </p>
 * <p>
 * Therefore, for example, to obtain all counter values that might have been generated sometime during 2012, simply use these same
 * methods:
 * 
 * <pre>
 * long janFirst2012 = 1325397600; // Jan 1 2012 at 00:00.000 UTC
 * long janFirst2013 = 1357020000; // Jan 1 2013 at 00:00.000 UTC
 * long smallest = counter.getEarliestCounterForTime(janFirst2012);
 * long justLarger = counter.getEarliestCounterForTime(janFirst2013);
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
        assert false;
        return this.next();
    }

    /**
     * Obtain the smallest counter value that is equal to or smaller than all counters generated at or since the given time in
     * UTC.
     * 
     * @param millis the number of milliseconds (in UTC) past epoch, and the time at which {@link #next()} might have been called
     * @return a long value that is the earliest possible counter value for the given time
     */
    public long getEarliestCounterForTime( long millis ) {
        return (millis << counterBits);
    }

    /**
     * Obtain the largest counter value that is equal to or greater than all counters generated at or before the given time in
     * UTC.
     * 
     * @param millis the number of milliseconds (in UTC) past epoch, and the time at which {@link #next()} might have been called
     * @return a long value that is the latest possible counter value for the given time
     */
    public long getLatestInclusiveCounterForTime( long millis ) {
        return (millis << counterBits) + maximumCounterValue;
    }

    /**
     * Obtain the largest counter value that is greater than all counters generated at or before the given time in UTC.
     * 
     * @param millis the number of milliseconds (in UTC) past epoch, and the time at which {@link #next()} might have been called
     * @return a long value that is the latest possible counter value for the given time
     */
    public long getLatestExclusiveCounterForTime( long millis ) {
        return (millis + 1) << counterBits;
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
