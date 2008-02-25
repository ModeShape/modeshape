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

package org.jboss.dna.common.monitor;

import static org.hamcrest.core.Is.is;
import static org.jboss.dna.common.junit.IsCloseTo.closeTo;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class ProgressStatusTest {

    private ProgressStatus status;
    private String validActivityName;
    private String validTaskName;

    @Before
    public void beforeEach() throws Exception {
        this.validActivityName = "Reading from file X";
        this.validTaskName = "Checking for file";
        this.status = new ProgressStatus(this.validActivityName, this.validTaskName, 10.0d, false);
    }

    @Test
    public void shouldComputePercentageAs100PercentIfWithinPrecision() {
        assertThat(ProgressStatus.computePercentage(100.0d - (ProgressStatus.PERCENT_PRECISION / 2.0d), 100.0d), is(closeTo(100.0d, ProgressStatus.PERCENT_PRECISION)));
    }

    @Test
    public void shouldComputePercentageOfZeroWorkAsZero() {
        // Note that we should not get a divide by zero !!!
        assertThat(ProgressStatus.computePercentage(0.0d, 0.0d), is(closeTo(0.0d, ProgressStatus.PERCENT_PRECISION)));
        assertThat(ProgressStatus.computePercentage(0.0d, 100.0d), is(closeTo(0.0d, ProgressStatus.PERCENT_PRECISION)));
    }

    @Test
    public void shouldHaveToStringThatIncludesPercentage() {
        // System.out.println(this.status);
        assertThat(this.status.toString().indexOf("10.0 %") > 0, is(true));
    }
}
