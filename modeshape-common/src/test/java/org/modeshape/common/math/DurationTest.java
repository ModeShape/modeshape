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
import static org.junit.Assert.assertNotNull;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;

public class DurationTest {

    private Duration duration;

    @Before
    public void beforeEach() {
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

    /**
     * Need to be careful about comparing strings, because {@link DecimalFormat} is used and relies upon the default locale. What
     * we do know is:
     * <ul>
     * <li>the hours will be at least 2 digits (zero-padded at beginning);</li>
     * <li>the minutes will be 2 digits (zero-padded at beginning);</li>
     * <li>the seconds will be formatted according to "00.000,###" using DecimalFormat and the default locale (which may use a
     * different decimal point delimiter than '.') and where the ',' is only used if there are more than 3 digits used in the
     * fractional part;</li>
     * <li>the hours, minutes and seconds will be delimited with a ':';</li>
     */
    @Test
    public void shouldRepresentTimeInProperFormat() {
        this.duration = this.duration.add(2, TimeUnit.SECONDS);
        assertEquals(this.duration.toString().startsWith("00:00:02"), true);
        assertEquals(this.duration.toString().endsWith("000"), true);

        this.duration = new Duration(1100, TimeUnit.MILLISECONDS);
        this.duration = this.duration.add(1 * 60, TimeUnit.SECONDS);
        this.duration = this.duration.add(1 * 60 * 60, TimeUnit.SECONDS);
        assertEquals(this.duration.toString().startsWith("01:01:01"), true);
        assertEquals(this.duration.toString().endsWith("100"), true);

        this.duration = new Duration(30100123, TimeUnit.MICROSECONDS);
        this.duration = this.duration.add(20 * 60, TimeUnit.SECONDS);
        this.duration = this.duration.add(10 * 60 * 60, TimeUnit.SECONDS);
        assertEquals(this.duration.toString().startsWith("10:20:30"), true);
        assertEquals(this.duration.toString().endsWith("100,123"), true);
    }

}
