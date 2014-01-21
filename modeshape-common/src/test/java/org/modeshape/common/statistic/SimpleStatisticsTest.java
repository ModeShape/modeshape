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
package org.modeshape.common.statistic;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.modeshape.common.math.FloatOperations;
import org.modeshape.common.math.IntegerOperations;

public class SimpleStatisticsTest {

    private SimpleStatistics<Integer> intStats = new SimpleStatistics<Integer>(new IntegerOperations());
    private SimpleStatistics<Float> floatStats = new SimpleStatistics<Float>(new FloatOperations());

    // private Logger logger = Logger.getLogger(SimpleStatisticsTest.class);

    @Test
    public void shouldHaveValidValuesWhenUnused() {
        assertThat(this.intStats.getCount(), is(0));
        assertThat(this.intStats.getMinimum(), is(0));
        assertThat(this.intStats.getMaximum(), is(0));
        assertThat(this.intStats.getMean(), is(0));
    }

    @Test
    public void shouldCorrectStatisitcValuesWhenUnusedOnce() {
        this.intStats.add(10);
        assertThat(this.intStats.getCount(), is(1));
        assertThat(this.intStats.getMinimum(), is(10));
        assertThat(this.intStats.getMaximum(), is(10));
        assertThat(this.intStats.getMeanValue(), is(10.0d));
    }

    @Test
    public void shouldCorrectStatisitcValuesWhenUsedAnOddNumberOfTimesButMoreThanOnce() {
        this.intStats.add(1);
        this.intStats.add(2);
        this.intStats.add(3);
        assertThat(this.intStats.getCount(), is(3));
        assertThat(this.intStats.getMinimum(), is(1));
        assertThat(this.intStats.getMaximum(), is(3));
        assertThat(this.intStats.getMeanValue(), is(2.0d));
    }

    @Test
    public void shouldCorrectStatisitcValuesWhenUsedAnEvenNumberOfTimes() {
        this.intStats.add(2);
        this.intStats.add(4);
        this.intStats.add(1);
        this.intStats.add(3);
        assertThat(this.intStats.getCount(), is(4));
        assertThat(this.intStats.getMinimum(), is(1));
        assertThat(this.intStats.getMaximum(), is(4));
        assertThat(this.intStats.getMeanValue(), is(2.5d));
    }

    @Test
    public void shouldCorrectStatisitcValuesWhenAllValuesAreTheSame() {
        this.intStats.add(2);
        this.intStats.add(2);
        this.intStats.add(2);
        this.intStats.add(2);
        assertThat(this.intStats.getCount(), is(4));
        assertThat(this.intStats.getMinimum(), is(2));
        assertThat(this.intStats.getMaximum(), is(2));
        assertThat(this.intStats.getMeanValue(), is(2.0d));
    }

    @Test
    public void shouldCorrectStatisitcValuesForComplexIntegerData() {
        this.intStats.add(19);
        this.intStats.add(10);
        this.intStats.add(20);
        this.intStats.add(7);
        this.intStats.add(73);
        this.intStats.add(72);
        this.intStats.add(42);
        this.intStats.add(9);
        this.intStats.add(47);
        this.intStats.add(24);
        System.out.println(this.intStats);
        assertThat(this.intStats.getCount(), is(10));
        assertThat(this.intStats.getMinimum(), is(7));
        assertThat(this.intStats.getMaximum(), is(73));
        assertEquals(32.3d, this.intStats.getMeanValue(), 0.0001d);
    }

    @Test
    public void shouldCorrectStatisitcValuesForComplexFloatData() {
        this.floatStats.add(1.9f);
        this.floatStats.add(1.0f);
        this.floatStats.add(2.0f);
        this.floatStats.add(0.7f);
        this.floatStats.add(7.3f);
        this.floatStats.add(7.2f);
        this.floatStats.add(4.2f);
        this.floatStats.add(0.9f);
        this.floatStats.add(4.7f);
        this.floatStats.add(2.4f);
        System.out.println(this.floatStats);
        assertThat(this.floatStats.getCount(), is(10));
        assertThat(this.floatStats.getMinimum(), is(0.7f));
        assertThat(this.floatStats.getMaximum(), is(7.3f));
        assertEquals(3.23f, this.floatStats.getMeanValue(), 0.0001f);
    }

    @Test
    public void shouldHaveNoStatisticValuesAfterUnusedAndReset() {
        this.intStats.add(19);
        this.intStats.add(10);
        this.intStats.add(20);
        assertThat(this.intStats.getCount(), is(3));
        this.intStats.reset();
        assertThat(this.intStats.getCount(), is(0));
        assertThat(this.intStats.getMinimum(), is(0));
        assertThat(this.intStats.getMaximum(), is(0));
        assertThat(this.intStats.getMean(), is(0));
    }

    @Test
    public void shouldHaveStringRepresentationWithoutStatisticsForSingleSample() {
        this.intStats.add(19);
        String str = this.intStats.toString();
        System.out.println(str);
        assertTrue(str.matches("1 sample.*"));
        assertTrue(str.matches(".*min=\\d{1,5}.*"));
        assertTrue(str.matches(".*max=\\d{1,5}.*"));
        assertTrue(str.matches(".*avg=\\d{1,5}.*"));
    }

    @Test
    public void shouldHaveStringRepresentationWithStatisticsForMultipleSample() {
        this.intStats.add(19);
        this.intStats.add(10);
        this.intStats.add(20);
        String str = this.intStats.toString();
        System.out.println(str);
        assertTrue(str.matches("^\\d{1,5}.*"));
        assertTrue(str.matches(".*3 samples.*"));
        assertTrue(str.matches(".*min=\\d{1,5}.*"));
        assertTrue(str.matches(".*max=\\d{1,5}.*"));
        assertTrue(str.matches(".*avg=\\d{1,5}.*"));
    }

}
