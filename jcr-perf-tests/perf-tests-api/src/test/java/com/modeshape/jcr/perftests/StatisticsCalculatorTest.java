package com.modeshape.jcr.perftests;

import org.junit.Assert;
import static org.junit.Assert.*;
import org.junit.Test;
import org.modeshape.jcr.perftests.StatisticsCalculator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
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
