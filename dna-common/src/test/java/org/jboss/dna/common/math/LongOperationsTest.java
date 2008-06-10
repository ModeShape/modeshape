/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.dna.common.math;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class LongOperationsTest {

    private LongOperations ops = new LongOperations();

    @Test
    public void shouldReturnProperExponenentInScientificNotation() {
        assertEquals(0l, ops.getExponentInScientificNotation(0l));
        assertEquals(0l, ops.getExponentInScientificNotation(1l));
        assertEquals(0l, ops.getExponentInScientificNotation(2l));
        assertEquals(0l, ops.getExponentInScientificNotation(9l));
        assertEquals(1l, ops.getExponentInScientificNotation(10l));
        assertEquals(1l, ops.getExponentInScientificNotation(20l));
        assertEquals(1l, ops.getExponentInScientificNotation(99l));
        assertEquals(2l, ops.getExponentInScientificNotation(100l));
        assertEquals(2l, ops.getExponentInScientificNotation(200l));
        assertEquals(2l, ops.getExponentInScientificNotation(999l));
        assertEquals(3l, ops.getExponentInScientificNotation(1000l));
        assertEquals(3l, ops.getExponentInScientificNotation(2000l));
        assertEquals(3l, ops.getExponentInScientificNotation(9999l));
    }

    @Test
    public void shouldRoundUpNumbersGreaterThan10() {
        assertThat(ops.roundUp(-101l, 0), is(-101l));
        assertThat(ops.roundUp(-101l, 1), is(-101l));
        assertThat(ops.roundUp(-101l, 1), is(-101l));
        assertThat(ops.roundUp(-101l, -1), is(-100l));
        assertThat(ops.roundUp(-109l, -1), is(-110l));
        assertThat(ops.roundUp(101l, 0), is(101l));
        assertThat(ops.roundUp(101l, 0), is(101l));
        assertThat(ops.roundUp(101l, 1), is(101l));
        assertThat(ops.roundUp(101l, 1), is(101l));
        assertThat(ops.roundUp(109l, -1), is(110l));
        assertThat(ops.roundUp(101l, -1), is(100l));
    }

    @Test
    public void shouldRoundDownNumbersGreaterThan10() {
        assertThat(ops.roundDown(-101l, 0), is(-101l));
        assertThat(ops.roundDown(-101l, 1), is(-101l));
        assertThat(ops.roundDown(-101l, 1), is(-101l));
        assertThat(ops.roundDown(-101l, -1), is(-100l));
        assertThat(ops.roundDown(-109l, -1), is(-100l));
        assertThat(ops.roundDown(101l, 0), is(101l));
        assertThat(ops.roundDown(101l, 0), is(101l));
        assertThat(ops.roundDown(101l, 1), is(101l));
        assertThat(ops.roundDown(101l, 1), is(101l));
        assertThat(ops.roundDown(109l, -1), is(100l));
        assertThat(ops.roundDown(101l, -1), is(100l));
    }

    @Test
    public void shouldKeepSignificantFigures() {
        assertThat(ops.keepSignificantFigures(0l, 2), is(0l));
        assertThat(ops.keepSignificantFigures(1201234l, 5), is(1201200l));
        assertThat(ops.keepSignificantFigures(1201254l, 5), is(1201300l));
        assertThat(ops.keepSignificantFigures(1201234l, 4), is(1201000l));
        assertThat(ops.keepSignificantFigures(1201234l, 3), is(1200000l));
        assertThat(ops.keepSignificantFigures(1201234l, 2), is(1200000l));
        assertThat(ops.keepSignificantFigures(1201234l, 1), is(1000000l));
        assertThat(ops.keepSignificantFigures(-1320l, 2), is(-1300l));
    }
}
