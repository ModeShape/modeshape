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
        assertEquals(0, this.duration.getComponents().getHours());
        assertEquals(0, this.duration.getComponents().getMinutes());
        assertEquals(0.0d, this.duration.getComponents().getSeconds(), 0.00001d);
    }

    @Test
    public void shouldHaveComponentsWhenInitialized() {
        assertNotNull(this.duration.getComponents());
    }

    @Test
    public void shouldBeAllowedToAddSeconds() {
        this.duration = this.duration.add(1, TimeUnit.SECONDS);
        assertEquals(0, this.duration.getComponents().getHours());
        assertEquals(0, this.duration.getComponents().getMinutes());
        assertEquals(1.0d, this.duration.getComponents().getSeconds(), 0.00001d);
    }

    @Test
    public void shouldRepresentTimeInProperFormat() {
        this.duration = this.duration.add(2, TimeUnit.SECONDS);
        assertEquals("00:00:02.000", this.duration.toString());

        this.duration = new Duration(1100, TimeUnit.MILLISECONDS);
        this.duration = this.duration.add(1 * 60, TimeUnit.SECONDS);
        this.duration = this.duration.add(1 * 60 * 60, TimeUnit.SECONDS);
        assertEquals("01:01:01.100", this.duration.toString());

        this.duration = new Duration(30100123, TimeUnit.MICROSECONDS);
        this.duration = this.duration.add(20 * 60, TimeUnit.SECONDS);
        this.duration = this.duration.add(10 * 60 * 60, TimeUnit.SECONDS);
        assertEquals("10:20:30.100,123", this.duration.toString());
    }

}
