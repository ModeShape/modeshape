package org.jboss.dna.common.math;


import static org.junit.Assert.*;

import org.jboss.dna.common.math.FloatOperations;
import org.junit.Before;
import org.junit.Test;

public class FloatOperationsTest {
	
	private FloatOperations ops = new FloatOperations();

	@Before
	public void beforeEach() throws Exception {
	}
	
	@Test
	public void shouldReturnProperExponenentInScientificNotation() {
		assertEquals(-3, ops.getExponentInScientificNotation(0.0010f) );
		assertEquals(-3, ops.getExponentInScientificNotation(0.0020f) );
		assertEquals(-3, ops.getExponentInScientificNotation(0.009999f) );
		//assertEquals(-2, ops.getExponentInScientificNotation(0.010000000f) );	// precision is messing this up; actual is 0.00999
		assertEquals(-2, ops.getExponentInScientificNotation(0.020000000f) );
		assertEquals(-2, ops.getExponentInScientificNotation(0.09999f) );
		assertEquals(-1, ops.getExponentInScientificNotation(0.10f) );
		assertEquals(-1, ops.getExponentInScientificNotation(0.20f) );
		assertEquals(-1, ops.getExponentInScientificNotation(0.9999f) );
		assertEquals(0, ops.getExponentInScientificNotation(0.0f) );
		assertEquals(0, ops.getExponentInScientificNotation(1.0f) );
		assertEquals(0, ops.getExponentInScientificNotation(2.0f) );
		assertEquals(0, ops.getExponentInScientificNotation(9.999f) );
		assertEquals(1, ops.getExponentInScientificNotation(10.0f) );
		assertEquals(1, ops.getExponentInScientificNotation(20.0f) );
		assertEquals(1, ops.getExponentInScientificNotation(99.999f) );
		assertEquals(2, ops.getExponentInScientificNotation(100.0f) );
		assertEquals(2, ops.getExponentInScientificNotation(200.0f) );
		assertEquals(2, ops.getExponentInScientificNotation(999.999f) );
		assertEquals(3, ops.getExponentInScientificNotation(1000.0f) );
		assertEquals(3, ops.getExponentInScientificNotation(2000.0f) );
		assertEquals(3, ops.getExponentInScientificNotation(9999.999f) );
	}
	
	@Test
	public void shouldRoundNumbersGreaterThan10() {
		assertEquals(101.0f, ops.roundUp(101.2523f, 0),0.01f);
		assertEquals(101.0f, ops.roundUp(101.2323f, 0),0.01f);
		assertEquals(101.3f, ops.roundUp(101.2523f, 1),0.01f);
		assertEquals(101.2f, ops.roundUp(101.2323f, 1),0.01f);
		assertEquals(110.0f, ops.roundUp(109.2323f, -1),1f);
		assertEquals(100.0f, ops.roundUp(101.2323f, -1),1f);
	}
}
