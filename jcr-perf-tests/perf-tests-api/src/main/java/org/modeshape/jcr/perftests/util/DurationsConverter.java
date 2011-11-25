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
package org.modeshape.jcr.perftests.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Class which converts between lists for various durations as {@link java.util.concurrent.TimeUnit}
 *
 * @author Horia Chiorean
 */
public final class DurationsConverter {

    private DurationsConverter() {
    }

    /**
     * Converts a list of durations expresses as nanoseconds, to a desired time unit. This method overcomes the loss of precision
     * when using {@link TimeUnit#convert(long, java.util.concurrent.TimeUnit)}
     *
     * @param nanos a list of nanosecond values
     * @param toUnit a <code>TimeUnit</code> to which the nanoseconds will be converted
     * @return a list of double values, representing the converted units.
     */
    public static List<Double> convertFromNanos( List<Long> nanos, TimeUnit toUnit ) {
        List<Double> convertedDurations = new ArrayList<Double>(nanos.size());
        for (long durationNano : nanos) {
            convertedDurations.add(convertFromNanos(durationNano, toUnit));
        }
        return convertedDurations;
    }

    private static double convertFromNanos( double nanoSeconds, TimeUnit toUnit ) {
        switch (toUnit) {
            case NANOSECONDS: {
                return nanoSeconds;
            }
            case MILLISECONDS: {
                return nanoSeconds / Math.pow(10, 6);
            }
            case SECONDS: {
                return nanoSeconds / Math.pow(10, 9);
            }
            case MINUTES: {
                return (nanoSeconds / Math.pow(10, 9)) / 60;
            }
            default: {
                return toUnit.convert((long)nanoSeconds, TimeUnit.NANOSECONDS);
            }
        }
    }
}
