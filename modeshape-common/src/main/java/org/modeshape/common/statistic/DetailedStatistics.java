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

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.math.MathOperations;
import org.modeshape.common.text.Inflector;
import org.modeshape.common.util.StringUtil;

/**
 * Encapsulation of the statistics for a series of values to which new values are frequently added. The statistics include the
 * {@link #getMinimum() minimum}, {@link #getMaximum() maximum}, {@link #getTotal() total (aggregate sum)},
 * {@link #getMean() mean (average)}, {@link #getMedian() median}, {@link #getStandardDeviation() standard deviation} and the
 * {@link #getHistogram() histogram} of the values.
 * <p>
 * This class uses an efficient running calculation of the mean and standard deviation that is not as susceptible to roundoff
 * errors as other traditional algorithms. The recursive algorithm is as follows, where M is the median value, sigma is the
 * standard deviation, and S is a variable used in the calculation of sigma:
 * 
 * <pre>
 *   M(1) = x(1)
 *   S(1) = 0
 *   M(k) = M(k-1) + ( x(k) - M(k-1) ) / k
 *   S(k) = S(k-1) + ( x(k) - M(k-1) ) * (x(k) - M(k))
 * </pre>
 * 
 * Then, the standard deviation for n values in x is
 * 
 * <pre>
 * sigma = sqrt(S(n) / n)
 * </pre>
 * 
 * </p>
 * Unlike the other quantities, the median value (the value at which half of the values are greater and half the values are lower)
 * cannot be calculated incrementally. Therefore, this class does record the values so that the median can be properly calculated.
 * This fact should be kept in mind when performing statistics on large numbers of values.
 * </p>
 * <p>
 * This class is threadsafe.
 * </p>
 * @param <T> the number type for these statistics
 */
@ThreadSafe
public class DetailedStatistics<T extends Number> extends SimpleStatistics<T> {

    private T median;
    private Double medianValue;
    private double s = 0.0d; // used in the calculation of standard deviation (sigma)
    private double sigma = 0.0d;
    private final List<T> values = new LinkedList<T>();
    private final List<T> unmodifiableValues = Collections.unmodifiableList(this.values);
    private Histogram<T> histogram;

    public DetailedStatistics( MathOperations<T> operations ) {
        super(operations);
        this.medianValue = 0.0d;
        this.median = this.math.createZeroValue();
    }

    /**
     * Get the values that have been recorded in these statistics. The contents of this list may change if new values are
     * {@link #add(Number) added} in another thread.
     * @return the unmodifiable collection of values, in insertion order
     */
    public List<T> getValues() {
        return this.unmodifiableValues;
    }

    @Override
    protected void doAddValue( T value ) {
        if (value == null) {
            return;
        }
        double previousMean = this.getMeanValue();
        super.doAddValue(value);
        this.values.add(value);
        this.medianValue = null;

        // Calculate the mean and standard deviation ...
        int count = getCount();
        if (count == 1) {
            this.s = 0.0d;
            this.sigma = 0.0d;
        } else {
            double dValue = value.doubleValue();
            double dCount = count;
            // M(k) = M(k-1) + ( x(k) - M(k-1) ) / k
            double meanValue = previousMean + ((dValue - previousMean) / dCount);
            // S(k) = S(k-1) + ( x(k) - M(k-1) ) * ( x(k) - M(k) )
            this.s = this.s + (dValue - previousMean) * (dValue - meanValue);
            // sigma = sqrt( S(n) / (n-1) )
            this.sigma = Math.sqrt(this.s / dCount);
        }
    }

    /**
     * Return the approximate mean (average) value represented as an instance of the operand type. Note that this may truncate if
     * the operand type is not able to have the required precision. For the accurate mean, see {@link #getMedianValue() }.
     * @return the mean (average), or 0.0 if the {@link #getCount() count} is 0
     */
    public T getMedian() {
        getMedianValue();
        return this.median;
    }

    /**
     * Return the median value.
     * @return the median value, or 0.0 if the {@link #getCount() count} is 0
     * @see #getMedian()
     */
    public double getMedianValue() {
        Lock lock = this.getLock().writeLock();
        try {
            lock.lock();
            int count = this.values.size();
            if (count == 0) {
                return 0.0d;
            }
            if (this.medianValue == null) {
                // Sort the values in numerical order..
                Comparator<T> comparator = this.math.getComparator();
                Collections.sort(this.values, comparator);
                this.medianValue = 0.0d;
                // If there is only one value, then the median is that value ...
                if (count == 1) {
                    this.medianValue = this.values.get(0).doubleValue();
                }
                // If there is an odd number of values, find value that is in the middle ..
                else if (count % 2 != 0) {
                    this.medianValue = this.values.get(((count + 1) / 2) - 1).doubleValue();
                }
                // Otherwise, there is an even number of values, so find the average of the middle two values ...
                else {
                    int upperMiddleValueIndex = count / 2;
                    int lowerMiddleValueIndex = upperMiddleValueIndex - 1;
                    double lowerValue = this.values.get(lowerMiddleValueIndex).doubleValue();
                    double upperValue = this.values.get(upperMiddleValueIndex).doubleValue();
                    this.medianValue = (lowerValue + upperValue) / 2.0d;
                }
                this.median = this.math.create(this.medianValue);
                this.histogram = null;
            }
        } finally {
            lock.unlock();
        }
        return this.medianValue;
    }

    /**
     * Return the standard deviation. The standard deviation is a measure of the variation in a series of values. Values with a
     * lower standard deviation has less variance in the values than a series of values with a higher standard deviation.
     * @return the standard deviation, or 0.0 if the {@link #getCount() count} is 0 or if all of the values are the same.
     */
    public double getStandardDeviation() {
        Lock lock = this.getLock().readLock();
        lock.lock();
        try {
            return this.sigma;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Return the histogram of the {@link #getValues() values}. This method returns a histogram where all of the buckets are
     * distributed normally and all have the same width. In this case, the 'numSigmas' should be set to 0. For other variations,
     * see {@link #getHistogram(int)}.
     * @return the histogram
     * @see #getHistogram(int)
     */
    public Histogram<T> getHistogram() {
        return getHistogram(0);
    }

    /**
     * Return the histogram of the {@link #getValues() values}. This method is capable of creating two kinds of histograms. The
     * first kind is a histogram where all of the buckets are distributed normally and all have the same width. In this case, the
     * 'numSigmas' should be set to 0. See {@link #getHistogram()}.
     * <p>
     * The second kind of histogram is more useful when most of the data that is clustered near one value. This histogram is
     * focused around the values that are up to 'numSigmas' above and below the {@link #getMedian() median}, and all values
     * outside of this range are placed in the first and last bucket.
     * </p>
     * @param numSigmas the number of standard deviations from the {@link #getMedian() median}, or 0 if the buckets of the
     * histogram should be evenly distributed
     * @return the histogram
     * @see #getHistogram()
     */
    public Histogram<T> getHistogram( int numSigmas ) {
        Lock lock = this.getLock().writeLock();
        lock.lock();
        try {
            Histogram<T> hist = new Histogram<T>(this.math, this.values);
            if (numSigmas > 0) {
                // The 'getMediaValue()' method will reset the current histogram, so don't set it...
                hist.setStrategy(this.getMedianValue(), this.getStandardDeviation(), numSigmas);
            }
            this.histogram = hist;
            return this.histogram;
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void doReset() {
        super.doReset();
        this.medianValue = 0.0d;
        this.median = this.math.createZeroValue();
        this.s = 0.0d;
        this.sigma = 0.0d;
        this.values.clear();
    }

    @Override
    public String toString() {
        int count = this.getCount();
        String samples = Inflector.getInstance().pluralize("sample", count);
        return StringUtil.createString("{0} {1}: min={2}; avg={3}; median={4}; stddev={5}; max={6}", count, samples, this.getMinimum(), this.getMean(), this.getMedian(), this.getStandardDeviation(),
                                       this.getMaximum());
    }

}
