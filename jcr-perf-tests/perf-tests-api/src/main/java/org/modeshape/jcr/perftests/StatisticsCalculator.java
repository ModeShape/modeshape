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
package org.modeshape.jcr.perftests;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Class which reports various statistics based on a set of performance data.
 *
 * @author Horia Chiorean
 */
public final class StatisticsCalculator {

    private StatisticsCalculator() {
    }

    /**
     * Returns an array of values representing [Min, 1st Quartile, Median, 3rd Quartile, Maximum] values, from an list of provided
     * values.
     *
     * @return the 5 nr summary values or <code>null</code> if the input is null or empty
     */
    public static double[] calculate5NrSummary( List<Long> valuesList ) {
        if (valuesList == null || valuesList.isEmpty()) {
            return null;
        }
        long[] values = convertToAscArray(valuesList);
        if (values.length == 1) {
            return new double[] {values[0], values[0], values[0], values[0], values[0]};
        }
        if (values.length == 2) {
            return new double[] {values[0], values[0], avg(values[0], values[1]), values[1], values[1]};
        }
        double[] result = new double[5];

        result[0] = min(values);
        int middleIdx = values.length / 2;

        result[1] = median(Arrays.copyOfRange(values,0, middleIdx));
        if (values.length % 2 == 0) {
            result[2] = avg(values[middleIdx - 1], values[middleIdx]);
            result[3] = median(Arrays.copyOfRange(values, middleIdx, values.length));
        } else {
            result[2] = values[middleIdx];
            result[3] = median(Arrays.copyOfRange(values, middleIdx + 1, values.length));
        }

        result[4] = max(values);

        return result;
    }

    private static long[] convertToAscArray( List<Long> valuesList ) {
        Collections.sort(valuesList);
        long[] values = new long[valuesList.size()];
        for (int i = 0; i < valuesList.size(); i++) {
            values[i] = valuesList.get(i);
        }
        return values;
    }

    private static long min( long... values ) {
        long min = Long.MAX_VALUE;
        for (Long value : values) {
            min = Math.min(min, value);
        }
        return min;
    }

    private static long max( long... values ) {
        long max = Long.MIN_VALUE;
        for (Long value : values) {
            max = Math.max(max, value);
        }
        return max;
    }

    private static double median( long... values ) {
        if (values.length == 1) {
            return values[0];
        }
        int middleIdx = values.length / 2;
        if (values.length % 2 == 0) {
            return avg(values[middleIdx - 1], values[middleIdx]);
        } else {
            return values[middleIdx];
        }
    }

    private static double avg( long... values ) {
        long sum = 0;
        for (long value : values) {
            sum += value;
        }
        return ((double)sum) / values.length;
    }
}
