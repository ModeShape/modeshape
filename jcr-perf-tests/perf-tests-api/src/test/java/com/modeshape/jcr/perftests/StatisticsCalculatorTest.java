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
package com.modeshape.jcr.perftests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.modeshape.jcr.perftests.StatisticalData;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit test for the 5nr summary calculations.
 *
 * @author Horia Chiorean
 */
public class StatisticsCalculatorTest {

    @Test( expected = IllegalArgumentException.class )
    public void fiveNumberSummaryInvalidValues() {
        new StatisticalData(Collections.<Number>emptyList());
        new StatisticalData(new Double[0]);
    }

    @Test
    public void fiveNumberSummary() {
        assertFiveNrSummary(new double[] {1, Double.NaN, 1, Double.NaN, 1}, 1.0);
        assertFiveNrSummary(new double[] {1, 1, 1.5, 2, 2}, 1.0, 2.0);
        assertFiveNrSummary(new double[] {1, 1, 2, 3, 3}, 1.0, 2.0, 3.0);
        assertFiveNrSummary(new double[] {1, 1.5, 2.5, 3.5, 4}, 1.0, 2.0, 3.0, 4.0);
        assertFiveNrSummary(new double[] {1, 1.5, 3, 4.5, 5}, 1.0, 2.0, 3.0, 4.0, 5.0);
        assertFiveNrSummary(new double[] {1, 2, 3.5, 5, 6}, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0);
        assertFiveNrSummary(new double[] {1, 2, 4, 6, 7}, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0);
        assertFiveNrSummary(new double[] {1, 2.5, 4.5, 6.5, 8}, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0);
    }

    private void assertFiveNrSummary( double[] expectedOutput, Double... input ) {
        double[] result = new StatisticalData(input).fiveNumberSummary();
        assertEquals(5, result.length);
        assertArrayEquals(expectedOutput, result, 0);

        List<Double> valuesList = Arrays.asList(input);
        Collections.reverse(valuesList);

        result = new StatisticalData(valuesList.toArray(new Double[0])).fiveNumberSummary();
        assertEquals(5, result.length);
        assertArrayEquals(expectedOutput, result, 0);
    }
}
