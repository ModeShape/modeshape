/*
 * JBoss, Home of Professional Open Source
 * Copyright [2011], Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
