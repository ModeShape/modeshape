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

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.math.MathOperations;
import org.modeshape.common.util.StringUtil;

/**
 * A representation of a histogram of values.
 * 
 * @param <T> the type of value
 */
@NotThreadSafe
public class Histogram<T extends Number> {

    public static final int DEFAULT_BUCKET_COUNT = 10;
    public static final int DEFAULT_SIGNIFICANT_FIGURES = 4;

    protected final MathOperations<T> math;
    protected final List<T> values;
    private int bucketCount = DEFAULT_BUCKET_COUNT;
    private int significantFigures = DEFAULT_SIGNIFICANT_FIGURES;
    private BigDecimal bucketWidth;
    private LinkedList<Bucket> buckets;
    private BucketingStrategy actualValueStrategy = new DefaultBucketingStrategy();
    private BucketingStrategy bucketingStrategy = actualValueStrategy;

    public Histogram( MathOperations<T> operations,
                      List<T> values ) {
        this.math = operations;
        this.values = new LinkedList<T>(values);
        this.buckets = new LinkedList<Bucket>();
        this.bucketWidth = null;
        // Sort the data using natural order ...
        Collections.sort(this.values, this.math.getComparator());
    }

    public Histogram( MathOperations<T> operations,
                      T... values ) {
        this(operations, Arrays.asList(values));
    }

    public BucketingStrategy getStrategy() {
        return this.bucketingStrategy;
    }

    /**
     * @return math
     */
    public MathOperations<T> getMathOperations() {
        return this.math;
    }

    /**
     * Set the histogram to use the standard deviation to determine the bucket sizes.
     * 
     * @param median
     * @param standardDeviation
     * @param sigma
     */
    public void setStrategy( double median,
                             double standardDeviation,
                             int sigma ) {
        this.bucketingStrategy = new StandardDeviationBucketingStrategy(median, standardDeviation, sigma);
        this.bucketWidth = null;
    }

    /**
     * Set the histogram to use the supplied minimum and maximum values to determine the bucket size.
     * 
     * @param minimum
     * @param maximum
     */
    public void setStrategy( T minimum,
                             T maximum ) {
        this.bucketingStrategy = new ExplicitBucketingStrategy(minimum, maximum);
        this.bucketWidth = null;
    }

    /**
     * Set the histogram to use the actual minimum and maximum values to determine the bucket sizes.
     */
    public void setStrategyToDefault() {
        this.bucketingStrategy = this.actualValueStrategy;
        this.bucketWidth = null;
    }

    public int getSignificantFigures() {
        return significantFigures;
    }

    /**
     * Set the number of significant figures used in the calculation of the bucket widths.
     * 
     * @param significantFigures the number of significant figures for the bucket widths
     * @return this histogram, useful for method-chaining
     * @see #DEFAULT_SIGNIFICANT_FIGURES
     */
    public Histogram<T> setSignificantFigures( int significantFigures ) {
        if (significantFigures != this.significantFigures) {
            this.significantFigures = significantFigures;
            this.bucketWidth = null;
            this.buckets.clear();
        }
        return this;
    }

    /**
     * Return the number of buckets in this histogram.
     * 
     * @return the number of buckets.
     */
    public int getBucketCount() {
        return bucketCount;
    }

    /**
     * Set the number of buckets that this histogram will use.
     * 
     * @param count the number of buckets
     * @return this histogram, useful for method-chaining
     * @see #DEFAULT_BUCKET_COUNT
     */
    public Histogram<T> setBucketCount( int count ) {
        if (count != this.bucketCount) {
            this.bucketCount = count;
            this.bucketWidth = null;
            this.buckets.clear();
        }
        return this;
    }

    /**
     * Get the buckets in this histogram. If the histogram has not yet been computed, this method will cause it to be generated.
     * The resulting list should not be modified.
     * 
     * @return the histogram buckets.
     */
    public List<Bucket> getBuckets() {
        compute();
        return this.buckets;
    }

    protected void compute() {
        // Only compute if there is not already a histogram ...
        if (this.bucketWidth != null) return;

        // Find the lower and upper bounds of the histogram using the strategy ...
        T lowerBound = this.bucketingStrategy.getLowerBound();
        T upperBound = this.bucketingStrategy.getUpperBound();

        // Find the actual minimum and maximum values ...
        T actualMinimum = this.actualValueStrategy.getLowerBound();
        T actualMaximum = this.actualValueStrategy.getUpperBound();

        // Create the buckets ...
        List<T> boundaries = getBucketBoundaries(this.math,
                                                 lowerBound,
                                                 upperBound,
                                                 actualMinimum,
                                                 actualMaximum,
                                                 this.bucketCount,
                                                 this.significantFigures);
        this.buckets.clear();
        int numBuckets = boundaries.isEmpty() ? 0 : boundaries.size() - 1;
        for (int i = 0; i != numBuckets; ++i) {
            this.buckets.add(new Bucket(boundaries.get(i), boundaries.get(i + 1)));
        }

        // Create the histogram by adding values to each range ...
        Iterator<Bucket> intervalIterator = this.buckets.iterator();
        Bucket currentInterval = null;
        for (T value : this.values) {
            while (currentInterval == null || currentInterval.checkValue(value, !intervalIterator.hasNext()) > 0) {
                if (!intervalIterator.hasNext()) break;
                currentInterval = intervalIterator.next();
            }
            if (currentInterval != null) currentInterval.addValue(value);
        }
    }

    /**
     * Return the total number of values that have gone into this histogram.
     * 
     * @return the total number of values
     * @see Bucket#getPercentageOfValues()
     */
    public long getTotalNumberOfValues() {
        return this.values.size();
    }

    protected float getMaximumPercentage() {
        float maxPercentage = 0.0f;
        for (Bucket bucket : this.buckets) {
            maxPercentage = Math.max(maxPercentage, bucket.getPercentageOfValues());
        }
        return maxPercentage;
    }

    protected long getMaximumCount() {
        long maxCount = 0l;
        for (Bucket bucket : this.buckets) {
            maxCount = Math.max(maxCount, bucket.getNumberOfValues());
        }
        return maxCount;
    }

    /**
     * Generate a textual (horizontal) bar graph of this histogram.
     * 
     * @param maxBarLength the maximum bar length, or 0 if the bar length is to represent actual counts
     * @return the strings that make up the histogram
     */
    public List<String> getTextGraph( int maxBarLength ) {
        compute();
        if (maxBarLength < 1) maxBarLength = (int)this.getMaximumCount();
        final float barLengthForHundredPercent = this.buckets.isEmpty() ? maxBarLength : 100.0f * maxBarLength
                                                                                         / getMaximumPercentage();
        final String fullLengthBar = StringUtil.createString('*', (int)barLengthForHundredPercent);
        List<String> result = new LinkedList<String>();
        // First calculate the labels and the max length ...
        int maxLowerBoundLength = 0;
        int maxUpperBoundLength = 0;
        for (Bucket bucket : this.buckets) {
            maxLowerBoundLength = Math.max(bucket.getLowerBound().toString().length(), maxLowerBoundLength);
            maxUpperBoundLength = Math.max(bucket.getUpperBound().toString().length(), maxUpperBoundLength);
        }

        // Create the header ...
        int rangeWidth = 1 + maxLowerBoundLength + 3 + maxUpperBoundLength + 1;
        int barWidth = maxBarLength + 20;
        result.add(StringUtil.justifyLeft("Ranges", rangeWidth, ' ') + " Distribution");
        result.add(StringUtil.createString('-', rangeWidth) + ' ' + StringUtil.createString('-', barWidth));
        for (Bucket bucket : this.buckets) {
            float percent = bucket.getPercentageOfValues();
            long number = bucket.getNumberOfValues();
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            sb.append(StringUtil.justifyLeft(bucket.getLowerBound().toString(), maxLowerBoundLength, ' '));
            sb.append(" - ");
            sb.append(StringUtil.justifyLeft(bucket.getUpperBound().toString(), maxUpperBoundLength, ' '));
            sb.append("] ");
            int barLength = Math.max((int)(barLengthForHundredPercent * percent / 100.0f), 0);
            if (barLength == 0 && number != 0) barLength = 1; // make sure there is a bar for all non-zero buckets
            sb.append(fullLengthBar.substring(0, barLength));
            if (number != 0) {
                sb.append(" ");
                sb.append(number);
                sb.append(" (");
                sb.append(new DecimalFormat("###.#").format(percent));
                sb.append("%)");
            }
            result.add(sb.toString());
        }
        return result;
    }

    protected static <T> List<T> getBucketBoundaries( MathOperations<T> math,
                                                      T lowerBound,
                                                      T upperBound,
                                                      T actualMinimum,
                                                      T actualMaximum,
                                                      int bucketCount,
                                                      int bucketWidthSigFigs ) {
        lowerBound = math.compare(lowerBound, actualMinimum) < 0 ? actualMinimum : lowerBound;
        upperBound = math.compare(actualMaximum, upperBound) < 0 ? actualMaximum : upperBound;
        if (math.compare(lowerBound, upperBound) == 0) {
            List<T> boundaries = new ArrayList<T>();
            boundaries.add(lowerBound);
            boundaries.add(upperBound);
            return boundaries;
        }
        final boolean extraLowerBucketNeeded = math.compare(lowerBound, actualMinimum) > 0;
        final boolean extraUpperBucketNeeded = math.compare(actualMaximum, upperBound) > 0;
        if (extraLowerBucketNeeded) --bucketCount;
        if (extraUpperBucketNeeded) --bucketCount;

        // Compute the delta between the lower and upper bound ...
        T totalWidth = math.subtract(upperBound, lowerBound);
        int totalWidthScale = math.getExponentInScientificNotation(totalWidth);

        // Modify the lower bound by rounding down to the next lower meaningful value,
        // using the scale of the totalWidth to determine how to round down.
        T roundedLowerBound = math.roundDown(lowerBound, -totalWidthScale);
        T roundedUpperBound = math.roundUp(upperBound, -totalWidthScale);

        // Create the ranges ...
        double finalLowerBound = math.doubleValue(roundedLowerBound);
        double finalUpperBound = math.doubleValue(roundedUpperBound);
        double finalBucketCount = bucketCount;
        double bucketWidth = (finalUpperBound - finalLowerBound) / finalBucketCount;

        // DoubleOperations doubleOps = new DoubleOperations();
        // bucketWidth = doubleOps.keepSignificantFigures(bucketWidth,bucketWidthSigFigs);

        List<T> boundaries = new ArrayList<T>();
        if (bucketWidth > 0.0d) {
            if (extraLowerBucketNeeded) boundaries.add(actualMinimum);
            double nextBoundary = finalLowerBound;
            for (int i = 0; i != bucketCount; ++i) {
                boundaries.add(math.create(nextBoundary));
                nextBoundary = nextBoundary + bucketWidth;
                // nextBoundary = doubleOps.roundUp(nextBoundary + bucketWidth, bucketWidthSigFigs );
            }
            boundaries.add(roundedUpperBound);
            if (extraUpperBucketNeeded) boundaries.add(actualMaximum);
        }
        return boundaries;
    }

    /**
     * Represents a bucket in a histogram.
     */
    public class Bucket implements Comparable<Bucket> {

        private final T lowerBound;
        private final T upperBound;
        private final T width;
        private long numValues;

        protected Bucket( T lowerBound,
                          T upperBound ) {
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            this.width = Histogram.this.math.subtract(upperBound, lowerBound);
        }

        /**
         * Get the lower bound of this bucket.
         * 
         * @return the lower bound
         */
        public T getLowerBound() {
            return lowerBound;
        }

        /**
         * Get the upper bound of this bucket.
         * 
         * @return the upper bound
         */
        public T getUpperBound() {
            return upperBound;
        }

        /**
         * Get the width of this bucket.
         * 
         * @return the width
         */
        public T getWidth() {
            return this.width;
        }

        /**
         * Return the percentage of values in the histogram that appear in this bucket.
         * 
         * @return the percentage of all values in the histogram that appear in this bucket.
         */
        public float getPercentageOfValues() {
            float total = Histogram.this.getTotalNumberOfValues();
            if (total == 0.0f) return 0.0f;
            float numValuesFloat = this.numValues;
            return 100.0f * numValuesFloat / total;
        }

        /**
         * Add a value to this bucket
         * 
         * @param value
         */
        protected void addValue( T value ) {
            ++this.numValues;
        }

        /**
         * Get the number of values in this bucket.
         * 
         * @return the number of values
         */
        public long getNumberOfValues() {
            return this.numValues;
        }

        /**
         * Check whether the value fits in this bucket.
         * 
         * @param value the value to check
         * @param isLast
         * @return 0 if the value fits in this bucket, -1 if the value fits in a prior bucket, or 1 if the value fits in a later
         *         bucket
         */
        public int checkValue( T value,
                               boolean isLast ) {
            if (Histogram.this.math.compare(this.lowerBound, value) > 0) return -1;
            if (isLast) {
                if (Histogram.this.math.compare(value, this.upperBound) > 0) return 1;
            } else {
                if (Histogram.this.math.compare(value, this.upperBound) >= 0) return 1;
            }
            return 0;
        }

        public int compareTo( Bucket that ) {
            // This is lower if 'that' has a lowerBound that is greater than 'this' lower bound ...
            if (Histogram.this.math.compare(this.lowerBound, that.lowerBound) < 0) return -1;
            if (Histogram.this.math.compare(this.lowerBound, that.lowerBound) > 0) return 1;
            // The lower bounds are the same, so 'this' is lower if 'that' has an upperBound that is greater than 'this' lower
            // bound ...
            if (Histogram.this.math.compare(this.upperBound, that.upperBound) < 0) return -1;
            if (Histogram.this.math.compare(this.upperBound, that.upperBound) > 0) return 1;
            return 0;
        }

        protected Class<T> getNumberClass() {
            return Histogram.this.math.getOperandClass();
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj != null && obj.getClass() == this.getClass()) {
                Bucket that = (Bucket)obj;
                if (this.getNumberClass().isAssignableFrom(that.getNumberClass())) {
                    if (Histogram.this.math.compare(this.lowerBound, that.lowerBound) != 0) return false;
                    if (Histogram.this.math.compare(this.upperBound, that.upperBound) != 0) return false;
                    if (Histogram.this.math.compare(this.width, that.width) != 0) return false;
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "[" + this.lowerBound + "," + this.upperBound + ")";
        }

    }

    public abstract class BucketingStrategy {

        public List<T> getValues() {
            return Histogram.this.values;
        }

        public abstract T getLowerBound();

        public abstract T getUpperBound();
    }

    public class DefaultBucketingStrategy extends BucketingStrategy {

        @Override
        public T getLowerBound() {
            if (getValues().isEmpty()) return Histogram.this.math.createZeroValue();
            return getValues().get(0);
        }

        @Override
        public T getUpperBound() {
            if (getValues().isEmpty()) return Histogram.this.math.createZeroValue();
            return getValues().get(getValues().size() - 1);
        }
    }

    public class ExplicitBucketingStrategy extends BucketingStrategy {

        private final T lowerBound;
        private final T upperBound;

        protected ExplicitBucketingStrategy( T lowerBound,
                                             T upperBound ) {
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
        }

        @Override
        public T getLowerBound() {
            return this.lowerBound;
        }

        @Override
        public T getUpperBound() {
            return this.upperBound;
        }
    }

    public class StandardDeviationBucketingStrategy extends BucketingStrategy {

        private final double median;
        private final double standardDeviation;
        private final int numberOfDeviationsAboveAndBelow;

        protected StandardDeviationBucketingStrategy( double median,
                                                      double standardDeviation,
                                                      int numDeviationsAboveAndBelow ) {
            this.median = median;
            this.standardDeviation = Math.abs(standardDeviation);
            this.numberOfDeviationsAboveAndBelow = Math.abs(numDeviationsAboveAndBelow);
        }

        @Override
        public T getLowerBound() {
            double lower = this.median - (standardDeviation * numberOfDeviationsAboveAndBelow);
            return Histogram.this.math.create(lower);
        }

        @Override
        public T getUpperBound() {
            double upper = this.median + (standardDeviation * numberOfDeviationsAboveAndBelow);
            return Histogram.this.math.create(upper);
        }
    }

}
