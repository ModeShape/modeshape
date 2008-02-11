package org.jboss.dna.common.math;


import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.jboss.dna.common.math.Duration;
import org.junit.Before;
import org.junit.Test;

public class DurationTest {
	
	private Duration duration;

	@Before
	public void beforeEach() throws Exception {
		this.duration = new Duration(0);
	}

	@Test
	public void shouldBeEmptyWhenInitialized() {
		assertEquals(0,    this.duration.getComponents().getHours() );
		assertEquals(0,    this.duration.getComponents().getMinutes() );
		assertEquals(0.0d, this.duration.getComponents().getSeconds(),0.00001d);
	}

	@Test
	public void shouldHaveComponentsWhenInitialized() {
		assertNotNull(this.duration.getComponents());
	}

	@Test
	public void shouldBeAllowedToAddSeconds() {
		this.duration = this.duration.add(1,TimeUnit.SECONDS);
		assertEquals(0,    this.duration.getComponents().getHours() );
		assertEquals(0,    this.duration.getComponents().getMinutes() );
		assertEquals(1.0d, this.duration.getComponents().getSeconds(),0.00001d);
	}
	
	
	@Test
	public void shouldRepresentTimeInProperFormat() {
		this.duration = this.duration.add(2, TimeUnit.SECONDS );
		assertEquals("00:00:02.000", this.duration.toString() );
		
		this.duration = new Duration(1100, TimeUnit.MILLISECONDS );
		this.duration = this.duration.add(1 * 60, TimeUnit.SECONDS);
		this.duration = this.duration.add(1 * 60 * 60, TimeUnit.SECONDS );
		assertEquals("01:01:01.100", this.duration.toString() );
		
		this.duration = new Duration(30100123, TimeUnit.MICROSECONDS );
		this.duration = this.duration.add(20 * 60, TimeUnit.SECONDS);
		this.duration = this.duration.add(10 * 60 * 60, TimeUnit.SECONDS );
		assertEquals("10:20:30.100,123", this.duration.toString() );
	}


}
