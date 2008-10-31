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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.stub;
import java.util.Locale;
import org.jboss.dna.common.i18n.MockI18n;
import org.jboss.dna.common.util.EmptyIterator;
import org.jboss.dna.common.util.Logger.Level;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;
import org.slf4j.Marker;

/**
 * @author jverhaeg
 */
public final class CapturedActivityInfoTest {

    private CapturedActivityInfo info;
    @Mock
    Marker marker;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        stub(marker.iterator()).toReturn(new EmptyIterator<Marker>());
        info = new CapturedActivityInfo(Level.INFO, MockI18n.passthrough, new Object[] {"task"}, marker, new Throwable(),
                                        MockI18n.passthrough, new Object[] {"message"}, Locale.US);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNoType() {
        new CapturedActivityInfo(null, MockI18n.passthrough, new Object[] {"task"}, marker, new Throwable(),
                                 MockI18n.passthrough, new Object[] {"message"}, Locale.US);
    }

    @Test
    public void shouldAllowNoTaskName() {
        new CapturedActivityInfo(Level.INFO, null, null, marker, new Throwable(), MockI18n.passthrough, new Object[] {"message"},
                                 Locale.US);
    }

    @Test
    public void shouldAllowNoTaskNameParameters() {
        new CapturedActivityInfo(Level.INFO, null, null, marker, new Throwable(), MockI18n.noPlaceholders, null, Locale.US);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowTaskNameParametersWithNoTaskName() {
        new CapturedActivityInfo(Level.INFO, null, new Object[] {"task"}, marker, new Throwable(), MockI18n.passthrough,
                                 new Object[] {"message"}, Locale.US);
    }

    @Test
    public void shouldAllowNoMarker() {
        new CapturedActivityInfo(Level.INFO, MockI18n.passthrough, new Object[] {"task"}, null, new Throwable(),
                                 MockI18n.passthrough, new Object[] {"message"}, Locale.US);
    }

    @Test
    public void shouldAllowNoThrowable() {
        new CapturedActivityInfo(Level.INFO, MockI18n.passthrough, new Object[] {"task"}, marker, null, MockI18n.passthrough,
                                 new Object[] {"message"}, Locale.US);
    }

    @Test
    public void shouldAllowNoMessage() {
        new CapturedActivityInfo(Level.INFO, MockI18n.passthrough, new Object[] {"task"}, marker, new Throwable(), null, null,
                                 Locale.US);
    }

    @Test
    public void shouldAllowNoMessageParameters() {
        new CapturedActivityInfo(Level.INFO, MockI18n.passthrough, new Object[] {"task"}, marker, new Throwable(),
                                 MockI18n.noPlaceholders, null, Locale.US);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowMessageParametersWithNoMessage() {
        new CapturedActivityInfo(Level.INFO, MockI18n.passthrough, new Object[] {"task"}, marker, new Throwable(), null,
                                 new Object[] {"message"}, Locale.US);
    }

    @Test
    public void shouldAllowNoLocale() {
        new CapturedActivityInfo(Level.INFO, MockI18n.passthrough, new Object[] {"task"}, marker, new Throwable(),
                                 MockI18n.passthrough, new Object[] {"message"}, null);
    }

    @Test
    public void shouldProvideMarker() {
        assertThat(info.getMarker(), notNullValue());
    }

    @Test
    public void shouldProvideMessage() {
        assertThat(info.getMessage(), is("message"));
    }

    @Test
    public void shouldProvideTaskName() {
        assertThat(info.getTaskName(), is("task"));
    }

    @Test
    public void shouldProvideThrowable() {
        assertThat(info.getThrowable(), notNullValue());
    }

    @Test
    public void shouldIndicateType() {
        assertThat(info.isError(), is(false));
        assertThat(info.isWarning(), is(false));
        CapturedActivityInfo info = new CapturedActivityInfo(Level.ERROR, null, null, null, null, null, null, null);
        assertThat(info.isError(), is(true));
        assertThat(info.isWarning(), is(false));
        info = new CapturedActivityInfo(Level.WARNING, null, null, null, null, null, null, null);
        assertThat(info.isError(), is(false));
        assertThat(info.isWarning(), is(true));
        info = new CapturedActivityInfo(Level.DEBUG, null, null, null, null, null, null, null);
        assertThat(info.isError(), is(false));
        assertThat(info.isWarning(), is(false));
    }
}
