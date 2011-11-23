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

import static org.junit.Assert.*;
import org.junit.Test;
import org.modeshape.jcr.perftests.StatisticsCalculator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit test for the 5nr summary calculations.
 *
 * @author Horia Chiorean
 */
public class StatisticsCalculatorTest {

    @Test
    public void fiveNrSummaryTest() {
        assertNull(StatisticsCalculator.calculate5NrSummary(null));
        assertNull(StatisticsCalculator.calculate5NrSummary(Collections.<Long>emptyList()));
        assertFiveNrSummary(Arrays.asList(1l), new double[] {1, 1, 1, 1, 1});
        assertFiveNrSummary(Arrays.asList(1l, 2l), new double[]{1, 1, 1.5, 2, 2});
        assertFiveNrSummary(Arrays.asList(1l, 2l, 3l), new double[]{1, 1, 2, 3, 3});
        assertFiveNrSummary(Arrays.asList(1l, 2l, 3l, 4l), new double[]{1, 1.5, 2.5, 3.5, 4});
        assertFiveNrSummary(Arrays.asList(1l, 2l, 3l, 4l, 5l), new double[]{1, 1.5, 3, 4.5, 5});
        assertFiveNrSummary(Arrays.asList(1l, 2l, 3l, 4l, 5l, 6l), new double[]{1, 2, 3.5, 5, 6});
        assertFiveNrSummary(Arrays.asList(1l, 2l, 3l, 4l, 5l, 6l, 7l), new double[]{1, 2, 4, 6, 7});
        assertFiveNrSummary(Arrays.asList(1l, 2l, 3l, 4l, 5l, 6l, 7l, 8l), new double[]{1, 2.5, 4.5, 6.5, 8});
    }

    private void assertFiveNrSummary(List<Long> input, double[] expectedOutput) {
        double[] result = StatisticsCalculator.calculate5NrSummary(input);
        assertEquals(5, result.length);
        assertArrayEquals(expectedOutput, result, 0);

        Collections.reverse(input);

        result = StatisticsCalculator.calculate5NrSummary(input);
        assertEquals(5, result.length);
        assertArrayEquals(expectedOutput, result, 0);
    }
}
