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
package org.modeshape.jcr.api.monitor;


/**
 * The immutable statistics for a sample of values. The statistics include the {@link #getMinimum() minimum},
 * {@link #getMaximum() maximum}, {@link #getMean() mean (average)}, {@link #getVariance() variance} and
 * {@link #getStandardDeviation() standard deviation}.
 * <p>
 * The median value is not included in these statistics, since the median value cannot be rolled up given a series of statistics
 * without having the original values. It is possible to compute the weighted median, but this loses effectiveness/value the more
 * times it is rolled up.
 * </p>
 * 
 * @since 3.0
 */
public interface Statistics {

    /**
     * An empty statistics array.
     */
    Statistics[] NO_STATISTICS = new Statistics[0];

    /**
     * Get the number of samples to which these statistics apply.
     * 
     * @return the number of samples; never negative
     */
    int getCount();

    /**
     * Get the maximum of the sampled values.
     * 
     * @return the maximum value
     */
    long getMaximum();

    /**
     * Get the minimum of the sampled values.
     * 
     * @return the minimum value
     */
    long getMinimum();

    /**
     * The mean (or average) of the sampled values. This is returned as a double to reduce the lost of precision.
     * 
     * @return the mean or average value
     */
    double getMean();

    /**
     * Get the variance of the sampled values, which is the average of the squared differences from the {@link #getMean() mean}.
     * 
     * @return the variance; never negative
     */
    double getVariance();

    /**
     * Get the standard deviation of the sampled values, which is a measure of how spread out the numbers are and is the square
     * root of the {@link #getVariance() variance}.
     * 
     * @return the standard deviation; never negative
     */
    double getStandardDeviation();
}
