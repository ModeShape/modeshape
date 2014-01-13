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
package org.modeshape.common.math;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class IntegerOperationsTest {

    private IntegerOperations ops = new IntegerOperations();

    @Test
    public void shouldReturnProperExponenentInScientificNotation() {
        assertEquals(0, ops.getExponentInScientificNotation(0));
        assertEquals(0, ops.getExponentInScientificNotation(1));
        assertEquals(0, ops.getExponentInScientificNotation(2));
        assertEquals(0, ops.getExponentInScientificNotation(9));
        assertEquals(1, ops.getExponentInScientificNotation(10));
        assertEquals(1, ops.getExponentInScientificNotation(20));
        assertEquals(1, ops.getExponentInScientificNotation(99));
        assertEquals(2, ops.getExponentInScientificNotation(100));
        assertEquals(2, ops.getExponentInScientificNotation(200));
        assertEquals(2, ops.getExponentInScientificNotation(999));
        assertEquals(3, ops.getExponentInScientificNotation(1000));
        assertEquals(3, ops.getExponentInScientificNotation(2000));
        assertEquals(3, ops.getExponentInScientificNotation(9999));
    }

    @Test
    public void shouldRoundNumbersGreaterThan10() {
        assertThat(ops.roundUp(-101, 0), is(-101));
        assertThat(ops.roundUp(-101, 1), is(-101));
        assertThat(ops.roundUp(-101, 1), is(-101));
        assertThat(ops.roundUp(-101, -1), is(-100));
        assertThat(ops.roundUp(-109, -1), is(-110));
        assertThat(ops.roundUp(101, 0), is(101));
        assertThat(ops.roundUp(101, 0), is(101));
        assertThat(ops.roundUp(101, 1), is(101));
        assertThat(ops.roundUp(101, 1), is(101));
        assertThat(ops.roundUp(109, -1), is(110));
        assertThat(ops.roundUp(101, -1), is(100));
    }

    @Test
    public void shouldKeepSignificantFigures() {
        assertThat(ops.keepSignificantFigures(0, 2), is(0));
        assertThat(ops.keepSignificantFigures(1201234, 5), is(1201200));
        assertThat(ops.keepSignificantFigures(1201254, 5), is(1201300));
        assertThat(ops.keepSignificantFigures(1201234, 4), is(1201000));
        assertThat(ops.keepSignificantFigures(1201234, 3), is(1200000));
        assertThat(ops.keepSignificantFigures(1201234, 2), is(1200000));
        assertThat(ops.keepSignificantFigures(1201234, 1), is(1000000));
        assertThat(ops.keepSignificantFigures(-1320, 2), is(-1300));
    }
}
