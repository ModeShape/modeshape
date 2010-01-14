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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import org.modeshape.common.i18n.MockI18n;
import org.modeshape.common.math.FloatOperations;
import org.modeshape.common.math.MathOperations;
import org.modeshape.common.statistic.Histogram;
import org.modeshape.common.text.Inflector;
import org.modeshape.common.util.Logger;
import org.junit.Test;

public class HistogramTest {

    private Logger logger = Logger.getLogger(HistogramTest.class);
    private Inflector inflector = Inflector.getInstance();

    public static <T extends Number> Histogram<T> createRandomHistogram( T minimum,
                                                                         T maximum,
                                                                         int numberOfValues,
                                                                         MathOperations<T> ops ) {
        List<T> values = new ArrayList<T>();
        Random rng = new Random();
        for (int i = 0; i != numberOfValues; ++i) {
            T newValue = ops.random(minimum, maximum, rng);
            values.add(newValue);
        }
        return new Histogram<T>(ops, values);
    }

    public static <T extends Number> void writeHistogramToLog( Logger logger,
                                                               Histogram<T> histogram,
                                                               int barLength,
                                                               String description ) {
        logger.info(MockI18n.passthrough, description != null ? description : "Histogram:");
        List<String> barGraph = histogram.getTextGraph(barLength);
        for (String line : barGraph) {
            logger.debug("  " + line);
        }
    }

    public <T extends Number> void assertBucketValueCount( Histogram<T> histogram,
                                                           long... values ) {
        List<Histogram<T>.Bucket> buckets = histogram.getBuckets();
        // Check the number of buckets ...
        assertEquals("The number of buckets didn't match expected number", values.length, buckets.size());
        // Check the number of values ...
        for (int i = 0; i != buckets.size(); ++i) {
            assertEquals("The " + inflector.ordinalize(i + 1) + " bucket didn't have the expected number of values",
                         values[i],
                         buckets.get(i).getNumberOfValues());
        }
    }

    @Test
    public void shouldCorrectlyPlaceAnOddNumberOfFloatValuesIntoSameOddNumberOfBuckets() {
        Float[] values = {3.0f, 1.0f, 2.0f, 4.0f};
        Histogram<Float> gram = new Histogram<Float>(new FloatOperations(), values);
        gram.setBucketCount(3);
        // HistogramTest.writeHistogramToLog(this.logger, gram, 0,
        // "shouldCorrectlyPlaceAnOddNumberOfFloatValuesIntoSameOddNumberOfBuckets");
        assertBucketValueCount(gram, 1, 1, 2);
    }

    @Test
    public void shouldCorrectlyPlaceAnEvenNumberOfFloatValuesIntoSameEvenNumberOfBuckets() {
        Float[] values = {3.0f, 1.0f, 2.0f, 4.0f};
        Histogram<Float> gram = new Histogram<Float>(new FloatOperations(), values);
        gram.setBucketCount(4);
        // HistogramTest.writeHistogramToLog(this.logger, gram, 0,
        // "shouldCorrectlyPlaceAnEvenNumberOfFloatValuesIntoSameEvenNumberOfBuckets");
        assertBucketValueCount(gram, 1, 1, 1, 1);

    }

    @Test
    public void shouldCorrectlyPlaceAnOddNumberOfFloatValuesIntoSmallerNumberOfBuckets() {
        Float[] values = {3.0f, 1.0f, 2.0f};
        Histogram<Float> gram = new Histogram<Float>(new FloatOperations(), values);
        gram.setBucketCount(2);
        // HistogramTest.writeHistogramToLog(this.logger, gram, 0,
        // "shouldCorrectlyPlaceAnEvenNumberOfFloatValuesIntoSameEvenNumberOfBuckets");
        assertBucketValueCount(gram, 1, 2);
    }

    @Test
    public void shouldCorrectlyPlaceAnEvenNumberOfFloatValuesIntoSmallerNumberOfBuckets() {
        Float[] values = {3.0f, 1.0f, 2.0f, 4.0f};
        Histogram<Float> gram = new Histogram<Float>(new FloatOperations(), values);
        gram.setBucketCount(2);
        // HistogramTest.writeHistogramToLog(this.logger, gram, 0,
        // "shouldCorrectlyPlaceAnEvenNumberOfFloatValuesIntoSmallerNumberOfBuckets");
        assertBucketValueCount(gram, 2, 2);
    }

    @Test
    public void shouldReturnListOfBuckets() {
        Float[] values = {3.0f, 1.0f, 2.0f, 4.0f};
        Histogram<Float> gram = new Histogram<Float>(new FloatOperations(), values);
        assertTrue(gram.getBuckets() instanceof LinkedList<?>);
    }

    @Test
    public void shouldCorrectlyPlaceAnOddNumberOfFloatValuesIntoSmallerNumberOfBucketsWithMinimumAndMaximumRanges() {
        Float[] values = {3.0f, 1.0f, 2.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f};
        Histogram<Float> gram = new Histogram<Float>(new FloatOperations(), values);
        gram.setBucketCount(5);
        // HistogramTest.writeHistogramToLog(this.logger, gram, 0,
        // "shouldCorrectlyPlaceAnOddNumberOfFloatValuesIntoSmallerNumberOfBucketsWithMinimumAndMaximumRanges");
        assertBucketValueCount(gram, 2, 2, 2, 2, 2);
    }

    @Test
    public void shouldCorrectlyPlaceAnOddNumberOfFloatValuesIntoSmallerNumberOfBucketsWithMinimumRanges() {
        Float[] values = {3.0f, 1.0f, 2.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 9.999f};
        Histogram<Float> gram = new Histogram<Float>(new FloatOperations(), values);
        gram.setBucketCount(5);
        // HistogramTest.writeHistogramToLog(this.logger, gram, 0,
        // "shouldCorrectlyPlaceAnOddNumberOfFloatValuesIntoSmallerNumberOfBucketsWithMinimumRanges");
        assertBucketValueCount(gram, 2, 2, 2, 2, 2);
    }

    @Test
    public void shouldCorrectlyConstructHistogramWithStandardDeviation() {
        Float[] values = {3.0f, 1.0f, 2.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 9.999f, 3.1f, 3.2f, 3.3f, 3.21f, 3.22f, 3.33f};
        // RunningStatistics<Float> stats = new RunningStatistics<Float>(new FloatOperations());
        // for (Float value : values) stats.add(value);
        // System.out.println(stats);
        Histogram<Float> gram = new Histogram<Float>(new FloatOperations(), values);
        gram.setBucketCount(6);
        gram.setStrategy(3.315f, 2.52367f, 1);

        HistogramTest.writeHistogramToLog(this.logger, gram, 0, "shouldCorrectlyConstructHistogramWithStandardDeviation");
        assertBucketValueCount(gram, 1, 1, 7, 1, 1, 5);
    }

    @Test
    public void shouldCorrectlyPlace1000RandomFloatValues() {
        Histogram<Float> gram = createRandomHistogram(10.0f, 100.0f, 1000, new FloatOperations());
        // gram.setDesiredRange(0.0f,100.0f);
        HistogramTest.writeHistogramToLog(this.logger, gram, 0, "Histogram of 1000 random float values in "
                                                                + gram.getBucketCount() + " buckets: ");
    }

    @Test
    public void shouldCorrectlyConstructBoundariesWithWindowSmallerThanActualFloats() {
        List<Float> boundaries = Histogram.getBucketBoundaries(new FloatOperations(), 10.0f, 20.0f, 5.0f, 25.0f, 12, 3);
        assertNotNull(boundaries);
        assertEquals(13, boundaries.size());
        Float[] expectedBoundaries = {5.0f, 10.0f, 11f, 12f, 13f, 14f, 15f, 16f, 17f, 18f, 19f, 20f, 25f};
        assertArrayEquals(expectedBoundaries, boundaries.toArray(new Float[boundaries.size()]));
    }

    @Test
    public void shouldCorrectlyConstructBoundariesWithWindowSmallerThanActualNarrowlyVaryingFloats() {
        List<Float> boundaries = Histogram.getBucketBoundaries(new FloatOperations(),
                                                               10.00020f,
                                                               10.00030f,
                                                               10.00011f,
                                                               10.00050f,
                                                               12,
                                                               3);
        assertNotNull(boundaries);
        assertEquals(13, boundaries.size());
        assertEquals(10.00011f, boundaries.get(0), 0.00001f);
        assertEquals(10.00020f, boundaries.get(1), 0.00001f);
        assertEquals(10.00021f, boundaries.get(2), 0.00001f);
        assertEquals(10.00022f, boundaries.get(3), 0.00001f);
        assertEquals(10.00023f, boundaries.get(4), 0.00001f);
        assertEquals(10.00024f, boundaries.get(5), 0.00001f);
        assertEquals(10.00025f, boundaries.get(6), 0.00001f);
        assertEquals(10.00026f, boundaries.get(7), 0.00001f);
        assertEquals(10.00027f, boundaries.get(8), 0.00001f);
        assertEquals(10.00028f, boundaries.get(9), 0.00001f);
        assertEquals(10.00029f, boundaries.get(10), 0.00001f);
        assertEquals(10.00030f, boundaries.get(11), 0.00001f);
        assertEquals(10.00050f, boundaries.get(12), 0.00001f);
    }

}
