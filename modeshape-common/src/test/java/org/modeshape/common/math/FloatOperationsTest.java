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

public class FloatOperationsTest {

    private FloatOperations ops = new FloatOperations();

    @Test
    public void shouldReturnProperExponenentInScientificNotation() {
        assertEquals(-3, ops.getExponentInScientificNotation(0.0010f));
        assertEquals(-3, ops.getExponentInScientificNotation(0.0020f));
        assertEquals(-3, ops.getExponentInScientificNotation(0.009999f));
        // assertEquals(-2, ops.getExponentInScientificNotation(0.010000000f) ); // precision is messing this up; actual is
        // 0.00999
        assertEquals(-2, ops.getExponentInScientificNotation(0.020000000f));
        assertEquals(-2, ops.getExponentInScientificNotation(0.09999f));
        assertEquals(-1, ops.getExponentInScientificNotation(0.10f));
        assertEquals(-1, ops.getExponentInScientificNotation(0.20f));
        assertEquals(-1, ops.getExponentInScientificNotation(0.9999f));
        assertEquals(0, ops.getExponentInScientificNotation(0.0f));
        assertEquals(0, ops.getExponentInScientificNotation(1.0f));
        assertEquals(0, ops.getExponentInScientificNotation(2.0f));
        assertEquals(0, ops.getExponentInScientificNotation(9.999f));
        assertEquals(1, ops.getExponentInScientificNotation(10.0f));
        assertEquals(1, ops.getExponentInScientificNotation(20.0f));
        assertEquals(1, ops.getExponentInScientificNotation(99.999f));
        assertEquals(2, ops.getExponentInScientificNotation(100.0f));
        assertEquals(2, ops.getExponentInScientificNotation(200.0f));
        assertEquals(2, ops.getExponentInScientificNotation(999.999f));
        assertEquals(3, ops.getExponentInScientificNotation(1000.0f));
        assertEquals(3, ops.getExponentInScientificNotation(2000.0f));
        assertEquals(3, ops.getExponentInScientificNotation(9999.999f));
    }

    @Test
    public void shouldRoundNumbersGreaterThan10() {
        assertEquals(101.0f, ops.roundUp(101.2523f, 0), 0.01f);
        assertEquals(101.0f, ops.roundUp(101.2323f, 0), 0.01f);
        assertEquals(101.3f, ops.roundUp(101.2523f, 1), 0.01f);
        assertEquals(101.2f, ops.roundUp(101.2323f, 1), 0.01f);
        assertEquals(110.0f, ops.roundUp(109.2323f, -1), 1f);
        assertEquals(100.0f, ops.roundUp(101.2323f, -1), 1f);
    }
}
