package org.modeshape.jmx;

import java.beans.ConstructorProperties;
import org.modeshape.jcr.api.monitor.Statistics;

/**
 * Value holder exposed by JXM which provides statistical information via {@link Statistics}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class StatisticalData implements Statistics {
    private final int count;
    private final long maximum;
    private final long minimum;
    private final double mean;
    private final double variance;


    /**
     * @param count number of elements in the sample
     * @param maximum max value from the sample
     * @param minimum min value from the sample
     * @param mean sample mean
     * @param variance sample variance
     */
    @ConstructorProperties({"count", "maximum", "minimum", "mean", "variance"})
    public StatisticalData( int count,
                            long maximum,
                            long minimum,
                            double mean,
                            double variance ) {
        this.count = count;
        this.maximum = maximum;
        this.minimum = minimum;
        this.mean = mean;
        this.variance = variance;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public long getMaximum() {
        return maximum;
    }

    @Override
    public long getMinimum() {
        return minimum;
    }

    @Override
    public double getMean() {
        return mean;
    }

    @Override
    public double getVariance() {
        return variance;
    }

    @Override
    public double getStandardDeviation() {
        return variance <= 0.0d ? 0.0d : Math.sqrt(variance);
    }
}
