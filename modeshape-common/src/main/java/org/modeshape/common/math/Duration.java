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
package org.modeshape.common.math;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import net.jcip.annotations.Immutable;

/**
 * A number representing an immutable duration of time. This is intended to be used in the same manner as other {@link Number}
 * subclasses.
 */
@Immutable
public class Duration extends Number implements Comparable<Duration> {

    private static final long serialVersionUID = 1L;

    private final long durationInNanos;
    private Components components;

    /**
     * Create a duration given the number of nanoseconds.
     * 
     * @param nanos the number of nanoseconds in the duration
     */
    public Duration( long nanos ) {
        this(nanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Create a duration and the time unit.
     * 
     * @param duration the duration in the supplied time units
     * @param unit the time unit
     */
    public Duration( long duration,
                     TimeUnit unit ) {
        this.durationInNanos = TimeUnit.NANOSECONDS.convert(duration, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double doubleValue() {
        return this.durationInNanos;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float floatValue() {
        return this.durationInNanos;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int intValue() {
        return (int)this.durationInNanos;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long longValue() {
        return this.durationInNanos;
    }

    public BigDecimal toBigDecimal() {
        return new BigDecimal(this.durationInNanos);
    }

    /**
     * Add the supplied duration to this duration, and return the result.
     * 
     * @param duration the duration to add to this object
     * @param unit the unit of the duration being added; may not be null
     * @return the total duration
     */
    public Duration add( long duration,
                         TimeUnit unit ) {
        long durationInNanos = TimeUnit.NANOSECONDS.convert(duration, unit);
        return new Duration(this.durationInNanos + durationInNanos);
    }

    /**
     * Subtract the supplied duration from this duration, and return the result.
     * 
     * @param duration the duration to subtract from this object
     * @param unit the unit of the duration being subtracted; may not be null
     * @return the total duration
     */
    public Duration subtract( long duration,
                              TimeUnit unit ) {
        long durationInNanos = TimeUnit.NANOSECONDS.convert(duration, unit);
        return new Duration(this.durationInNanos - durationInNanos);
    }

    /**
     * Add the supplied duration to this duration, and return the result. A null value is treated as a duration of 0 nanoseconds.
     * 
     * @param duration the duration to add to this object
     * @return the total duration
     */
    public Duration add( Duration duration ) {
        return new Duration(this.durationInNanos + (duration == null ? 0l : duration.longValue()));
    }

    /**
     * Subtract the supplied duration from this duration, and return the result. A null value is treated as a duration of 0
     * nanoseconds.
     * 
     * @param duration the duration to subtract from this object
     * @return the resulting duration
     */
    public Duration subtract( Duration duration ) {
        return new Duration(this.durationInNanos - (duration == null ? 0l : duration.longValue()));
    }

    /**
     * Multiply the duration by the supplied scale factor, and return the result.
     * 
     * @param scale the factor by which the duration is to be scaled.
     * @return the scaled duration
     */
    public Duration multiply( long scale ) {
        return new Duration(this.durationInNanos * scale);
    }

    /**
     * Divide the duration by the supplied number, and return the result.
     * 
     * @param denominator the factor by which the duration is to be divided.
     * @return the resulting duration
     */
    public Duration divide( long denominator ) {
        return new Duration(this.durationInNanos / denominator);
    }

    /**
     * Divide the duration by another duration to calculate the ratio.
     * 
     * @param duration the duration that this duration is to be divided by; may not be null
     * @return the resulting duration
     */
    public double divide( Duration duration ) {
        return this.toBigDecimal().divide(duration.toBigDecimal()).doubleValue();
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo( Duration that ) {
        if (that == null) return 1;
        return this.durationInNanos < that.durationInNanos ? -1 : this.durationInNanos > that.durationInNanos ? 1 : 0;
    }

    /**
     * Return the total duration in nanoseconds.
     * 
     * @return the total duration in nanoseconds
     */
    public long getDuratinInNanoseconds() {
        return this.durationInNanos;
    }

    /**
     * Return the total duration in microseconds, which may contain a fraction part for the sub-microsecond component.
     * 
     * @return the total duration in microseconds
     */
    public BigDecimal getDurationInMicroseconds() {
        return this.toBigDecimal().divide(new BigDecimal(1000));
    }

    /**
     * Return the total duration in microseconds, which may contain a fraction part for the sub-microsecond component.
     * 
     * @return the total duration in microseconds
     */
    public BigDecimal getDurationInMilliseconds() {
        return this.toBigDecimal().divide(new BigDecimal(1000000));
    }

    /**
     * Return the total duration in microseconds, which may contain a fraction part for the sub-microsecond component.
     * 
     * @return the total duration in microseconds
     */
    public BigDecimal getDurationInSeconds() {
        return this.toBigDecimal().divide(new BigDecimal(1000000000));
    }

    /**
     * Return the duration components.
     * 
     * @return the individual time components of this duration
     */
    public Components getComponents() {
        if (this.components == null) {
            // This is idempotent, so no need to synchronize ...

            // Calculate how many seconds, and don't lose any information ...
            BigDecimal bigSeconds = new BigDecimal(this.durationInNanos).divide(new BigDecimal(1000000000));
            // Calculate the minutes, and round to lose the seconds
            int minutes = bigSeconds.intValue() / 60;
            // Remove the minutes from the seconds, to just have the remainder of seconds
            double dMinutes = minutes;
            double seconds = bigSeconds.doubleValue() - dMinutes * 60;
            // Now compute the number of full hours, and change 'minutes' to hold the remainding minutes
            int hours = minutes / 60;
            minutes = minutes - (hours * 60);
            this.components = new Components(hours, minutes, seconds);
        }
        return this.components;
    }

    /**
     * Get the duration value in the supplied unit of time.
     * 
     * @param unit the unit of time for the returned value; may not be null
     * @return the value of this duration in the supplied unit of time
     */
    public long getDuration( TimeUnit unit ) {
        if (unit == null) throw new IllegalArgumentException();
        return unit.convert(durationInNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Writes the duration in a form containing hours, minutes, and seconds, including the fractional part of the seconds. The
     * format is essentially <code>HHH:MM:SS.mmm,mmm</code>, where
     * <dl>
     * <dt>HHH</dt>
     * <dd>is the number of hours written in at least 2 digits (e.g., "03")</dd>
     * <dt>MM</dt>
     * <dd>is the number of hours written in at least 2 digits (e.g., "03")</dd>
     * <dt>SS</dt>
     * <dd>is the number of hours written in at least 2 digits (e.g., "03")</dd>
     * <dt>mmm,mmm</dt>
     * <dd>is the fractional part of seconds, written in at least millisecond precision and up to microsecond precision. The comma
     * appears if more than 3 digits are used.
     * </dl>
     * 
     * @return a string representation of the duration
     */
    @Override
    public String toString() {
        // Insert a comma after the milliseconds, if there are enough digits ..
        return this.getComponents().toString().replaceAll("(\\d{2}).(\\d{3})(\\d{1,3})", "$1.$2,$3");
    }

    /**
     * The atomic components of this duration, broken down into whole hours, minutes and (fractional) seconds.
     */
    public class Components {

        private final int hours;
        private final int minutes;
        private final double seconds;

        protected Components( int hours,
                              int minutes,
                              double seconds ) {
            this.hours = hours;
            this.minutes = minutes;
            this.seconds = seconds;
        }

        /**
         * Get the whole hours in this duration.
         * 
         * @return the hours
         */
        public int getHours() {
            return hours;
        }

        /**
         * Get the whole minutes in this duration.
         * 
         * @return the minutes, from 0 to 59.
         */
        public int getMinutes() {
            return minutes;
        }

        /**
         * Get the duration's seconds component.
         * 
         * @return the number of seconds, including fractional part.
         */
        public double getSeconds() {
            return seconds;
        }

        /**
         * Return the duration as a string in a form containing hours, minutes, and seconds, including the fractional part of the
         * seconds. The format is essentially <code>HHH:MM:SS.mmm</code>, where
         * <dl>
         * <dt>HHH</dt>
         * <dd>is the number of hours written in at least 2 digits (e.g., "03")</dd>
         * <dt>MM</dt>
         * <dd>is the number of hours written in at least 2 digits (e.g., "03")</dd>
         * <dt>SS</dt>
         * <dd>is the number of hours written in at least 2 digits (e.g., "03")</dd>
         * <dt>mmm</dt>
         * <dd>is the fractional part of seconds, written with 3-6 digits (any trailing zeros are dropped)
         * </dl>
         * 
         * @return a string representation of the duration components
         */
        @Override
        public String toString() {
            // Format the string, and have at least 2 digits for the hours, minutes and whole seconds,
            // and between 3 and 6 digits for the fractional part of the seconds...
            String result = new DecimalFormat("######00").format(hours) + ':' + new DecimalFormat("00").format(minutes) + ':'
                            + new DecimalFormat("00.000###").format(seconds);
            return result;
        }
    }

}
