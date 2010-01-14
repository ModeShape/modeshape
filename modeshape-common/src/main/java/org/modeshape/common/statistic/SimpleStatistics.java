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
package org.modeshape.common.statistic;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.math.MathOperations;
import org.modeshape.common.text.Inflector;
import org.modeshape.common.util.StringUtil;

/**
 * Encapsulation of the statistics for a series of values to which new values are frequently added. The statistics include the
 * {@link #getMinimum() minimum}, {@link #getMaximum() maximum}, {@link #getTotal() total (aggregate sum)}, and {@link #getMean()
 * mean (average)}. See {@link DetailedStatistics} for a subclass that also calculates the {@link DetailedStatistics#getMedian()
 * median}, {@link DetailedStatistics#getStandardDeviation() standard deviation} and the {@link DetailedStatistics#getHistogram()
 * histogram} of the values.
 * <p>
 * This class is threadsafe.
 * </p>
 * 
 * @param <T> the number type used in these statistics
 */
@ThreadSafe
public class SimpleStatistics<T extends Number> {

    protected final MathOperations<T> math;
    private int count = 0;
    private T total;
    private T maximum;
    private T minimum;
    private T mean;
    private Double meanValue;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public SimpleStatistics( MathOperations<T> operations ) {
        this.math = operations;
        this.total = this.math.createZeroValue();
        this.maximum = this.math.createZeroValue();
        this.minimum = null;
        this.mean = this.math.createZeroValue();
        this.meanValue = 0.0d;
    }

    /**
     * Add a new value to these statistics.
     * 
     * @param value the new value
     */
    public void add( T value ) {
        Lock lock = this.lock.writeLock();
        try {
            lock.lock();
            doAddValue(value);
        } finally {
            lock.unlock();
        }
    }

    /**
     * A method that can be overridden by subclasses when {@link #add(Number) add} is called. This method is called within the
     * write lock, and does real work. Therefore, subclasses should call this method when they overwrite it.
     * 
     * @param value the value already added
     */
    protected void doAddValue( T value ) {
        if (value == null) return;
        // Modify the basic statistics ...
        ++this.count;
        this.total = math.add(this.total, value);
        this.maximum = this.math.maximum(this.maximum, value);
        this.minimum = this.math.minimum(this.minimum, value);
        // Calculate the mean and standard deviation ...
        int count = getCount();
        if (count == 1) {
            // M(1) = x(1)
            this.meanValue = value.doubleValue();
            this.mean = value;
        } else {
            double dValue = value.doubleValue();
            double dCount = count;
            // M(k) = M(k-1) + ( x(k) - M(k-1) ) / k
            this.meanValue = this.meanValue + ((dValue - this.meanValue) / dCount);
            this.mean = this.math.create(this.meanValue);
        }
    }

    /**
     * Get the aggregate sum of the values in the series.
     * 
     * @return the total of the values, or 0.0 if the {@link #getCount() count} is 0
     */
    public T getTotal() {
        Lock lock = this.lock.readLock();
        lock.lock();
        try {
            return this.total;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get the maximum value in the series.
     * 
     * @return the maximum value, or 0.0 if the {@link #getCount() count} is 0
     */
    public T getMaximum() {
        Lock lock = this.lock.readLock();
        lock.lock();
        try {
            return this.maximum;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get the minimum value in the series.
     * 
     * @return the minimum value, or 0.0 if the {@link #getCount() count} is 0
     */
    public T getMinimum() {
        Lock lock = this.lock.readLock();
        lock.lock();
        try {
            return this.minimum != null ? this.minimum : (T)this.math.createZeroValue();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get the number of values that have been measured.
     * 
     * @return the count
     */
    public int getCount() {
        Lock lock = this.lock.readLock();
        lock.lock();
        try {
            return this.count;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Return the approximate mean (average) value represented as an instance of the operand type. Note that this may truncate if
     * the operand type is not able to have the required precision. For the accurate mean, see {@link #getMeanValue() }.
     * 
     * @return the mean (average), or 0.0 if the {@link #getCount() count} is 0
     */
    public T getMean() {
        Lock lock = this.lock.readLock();
        lock.lock();
        try {
            return this.mean;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Return the mean (average) value.
     * 
     * @return the mean (average), or 0.0 if the {@link #getCount() count} is 0
     * @see #getMean()
     */
    public double getMeanValue() {
        Lock lock = this.lock.readLock();
        lock.lock();
        try {
            return this.meanValue;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Reset the statistics in this object, and clear out any stored information.
     */
    public void reset() {
        Lock lock = this.lock.writeLock();
        lock.lock();
        try {
            doReset();
        } finally {
            lock.unlock();
        }
    }

    public MathOperations<T> getMathOperations() {
        return math;
    }

    protected ReadWriteLock getLock() {
        return this.lock;
    }

    /**
     * Method that can be overridden by subclasses when {@link #reset()} is called. This method is called while the object is
     * locked for write and does work; therefore, the subclass should call this method.
     */
    protected void doReset() {
        this.total = this.math.createZeroValue();
        this.maximum = this.math.createZeroValue();
        this.minimum = null;
        this.mean = this.math.createZeroValue();
        this.meanValue = 0.0d;
        this.count = 0;
    }

    @Override
    public String toString() {
        int count = this.getCount();
        String samples = Inflector.getInstance().pluralize("sample", count);
        return StringUtil.createString("{0} {1}: min={2}; avg={3}; max={4}",
                                       count,
                                       samples,
                                       this.minimum,
                                       this.mean,
                                       this.maximum);
    }

}
