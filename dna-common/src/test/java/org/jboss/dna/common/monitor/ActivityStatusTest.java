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
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jboss.dna.common.i18n.MockI18n;
import org.jboss.dna.common.util.Logger;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class ActivityStatusTest {

    private static final String VALID_ACTIVITY_NAME = "Reading from file X";
    private static final String VALID_TASK_NAME = "Checking for file";

    private ActivityStatus status;
    private List<UnlocalizedActivityInfo> infos;

    @Before
    public void beforeEach() {
        infos = new ArrayList<UnlocalizedActivityInfo>();
        infos.add(new UnlocalizedActivityInfo(Logger.Level.INFO, null, null, null, null, null, null));
        this.status = new ActivityStatus(MockI18n.passthrough, new Object[] {VALID_ACTIVITY_NAME}, MockI18n.passthrough,
                                         new Object[] {VALID_TASK_NAME}, 10.0d, false, infos, Locale.US);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNoActivityName() {
        new ActivityStatus(null, new Object[] {VALID_ACTIVITY_NAME}, MockI18n.passthrough, new Object[] {VALID_TASK_NAME}, 10.0d,
                           false, infos, Locale.US);
    }

    @Test
    public void shouldAllowNoActivityNameParameters() {
        new ActivityStatus(MockI18n.noPlaceholders, null, MockI18n.passthrough, new Object[] {VALID_TASK_NAME}, 10.0d, false,
                           infos, Locale.US);
    }

    @Test
    public void shouldAllowNoTaskName() {
        new ActivityStatus(MockI18n.passthrough, new Object[] {VALID_ACTIVITY_NAME}, null, null, 10.0d, false, infos, Locale.US);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowTaskNameParametersWithNoTaskName() {
        new ActivityStatus(MockI18n.passthrough, new Object[] {VALID_ACTIVITY_NAME}, null, new Object[] {VALID_TASK_NAME}, 10.0d,
                           false, infos, Locale.US);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNoCapturedActivityInformation() {
        new ActivityStatus(MockI18n.passthrough, new Object[] {VALID_ACTIVITY_NAME}, MockI18n.passthrough,
                           new Object[] {VALID_TASK_NAME}, 10.0d, false, null, Locale.US);
    }

    @Test
    public void shouldAllowNoLocale() {
        new ActivityStatus(MockI18n.passthrough, new Object[] {VALID_ACTIVITY_NAME}, MockI18n.passthrough,
                           new Object[] {VALID_TASK_NAME}, 10.0d, false, infos, null);
    }

    @Test
    public void shouldComputePercentageAs100PercentIfWithinPrecision() {
        assertThat(ActivityStatus.computePercentage(100.0d - (ActivityStatus.PERCENT_PRECISION / 2.0d), 100.0d),
                   is(closeTo(100.0d, ActivityStatus.PERCENT_PRECISION)));
    }

    @Test
    public void shouldComputePercentageOfZeroWorkAsZero() {
        // Note that we should not get a divide by zero !!!
        assertThat(ActivityStatus.computePercentage(0.0d, 0.0d), is(closeTo(0.0d, ActivityStatus.PERCENT_PRECISION)));
        assertThat(ActivityStatus.computePercentage(0.0d, 100.0d), is(closeTo(0.0d, ActivityStatus.PERCENT_PRECISION)));
    }

    @Test
    public void shouldHaveToStringThatIncludesPercentage() {
        assertThat(this.status.toString().indexOf("10.0 %") > 0, is(true));
    }

    @Test
    public void shouldProvideCapturedActivityInforation() {
        assertThat(status.getCapturedInformation().length > 0, is(true));
    }

    @Test
    public void shouldProvideActivityName() {
        assertThat(status.getActivityName(), is(VALID_ACTIVITY_NAME));
    }

    @Test
    public void shouldProvideTaskName() {
        assertThat(status.getTaskName(), is(VALID_TASK_NAME));
    }
}
