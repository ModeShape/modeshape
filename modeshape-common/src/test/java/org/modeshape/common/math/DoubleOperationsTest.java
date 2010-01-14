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

package org.modeshape.common.math;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class DoubleOperationsTest {

    private DoubleOperations ops = new DoubleOperations();

    @Test
    public void shouldReturnProperExponenentInScientificNotation() {
        assertEquals(-3, ops.getExponentInScientificNotation(0.0010d));
        assertEquals(-3, ops.getExponentInScientificNotation(0.0020d));
        assertEquals(-3, ops.getExponentInScientificNotation(0.009999d));
        assertEquals(-2, ops.getExponentInScientificNotation(0.010d));
        assertEquals(-2, ops.getExponentInScientificNotation(0.020d));
        assertEquals(-2, ops.getExponentInScientificNotation(0.09999d));
        assertEquals(-1, ops.getExponentInScientificNotation(0.10d));
        assertEquals(-1, ops.getExponentInScientificNotation(0.20d));
        assertEquals(-1, ops.getExponentInScientificNotation(0.9999d));
        assertEquals(0, ops.getExponentInScientificNotation(0.0d));
        assertEquals(0, ops.getExponentInScientificNotation(1.0d));
        assertEquals(0, ops.getExponentInScientificNotation(2.0d));
        assertEquals(0, ops.getExponentInScientificNotation(9.999d));
        assertEquals(1, ops.getExponentInScientificNotation(10.0d));
        assertEquals(1, ops.getExponentInScientificNotation(20.0d));
        assertEquals(1, ops.getExponentInScientificNotation(99.999d));
        assertEquals(2, ops.getExponentInScientificNotation(100.0d));
        assertEquals(2, ops.getExponentInScientificNotation(200.0d));
        assertEquals(2, ops.getExponentInScientificNotation(999.999d));
        assertEquals(3, ops.getExponentInScientificNotation(1000.0d));
        assertEquals(3, ops.getExponentInScientificNotation(2000.0d));
        assertEquals(3, ops.getExponentInScientificNotation(9999.999d));
    }

    @Test
    public void shouldRoundNumbersGreaterThan10() {
        assertEquals(101.0d, ops.roundUp(101.2523d, 0), 0.01d);
        assertEquals(101.0d, ops.roundUp(101.2323d, 0), 0.01d);
        assertEquals(101.3d, ops.roundUp(101.2523d, 1), 0.01d);
        assertEquals(101.2d, ops.roundUp(101.2323d, 1), 0.01d);
        assertEquals(110.0d, ops.roundUp(109.2323d, -1), 1d);
        assertEquals(100.0d, ops.roundUp(101.2323d, -1), 1d);
    }

    @Test
    public void shouldKeepSignificantFigures() {
        assertEquals(12.012d, ops.keepSignificantFigures(12.0123456, 5), 0.0001d);
        assertEquals(12.013d, ops.keepSignificantFigures(12.0125456, 5), 0.0001d);
        assertEquals(12.01d, ops.keepSignificantFigures(12.0123456, 4), 0.0001d);
        assertEquals(12.0d, ops.keepSignificantFigures(12.0123456, 3), 0.0001d);
        assertEquals(12.0d, ops.keepSignificantFigures(12.0123456, 2), 0.0001d);
        assertEquals(10.0d, ops.keepSignificantFigures(12.0123456, 1), 0.0001d);
        assertEquals(1300.0d, ops.keepSignificantFigures(1320.0d, 2), 0.001d);
    }
}
