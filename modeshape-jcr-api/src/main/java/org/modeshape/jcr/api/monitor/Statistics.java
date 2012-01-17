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
