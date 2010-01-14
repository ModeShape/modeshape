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
package org.modeshape.web.jcr.rest.client;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isNull;
import org.modeshape.web.jcr.rest.client.Status.Severity;
import org.junit.Test;

/**
 * The <code>StatusTest</code> class is a test class for the {@link Status status} object.
 */
public final class StatusTest {

    // ===========================================================================================================================
    // Constants
    // ===========================================================================================================================

    private static final Status ERROR_STATUS = new Status(Severity.ERROR, null, null);

    private static final Status INFO_STATUS = new Status(Severity.INFO, null, null);

    private static final Status WARNING_STATUS = new Status(Severity.WARNING, null, null);

    private static final Status UNKNOWN_STATUS = new Status(Severity.UNKNOWN, null, null);

    private static final Status NULL_SEVERITY_STATUS = new Status(null, null, null);

    // ===========================================================================================================================
    // Tests
    // ===========================================================================================================================

    @Test
    public void shouldHaveErrorSeverity() {
        assertThat(ERROR_STATUS.isError(), is(true));

        // make sure other values are false
        assertThat(ERROR_STATUS.isInfo(), is(false));
        assertThat(ERROR_STATUS.isOk(), is(false));
        assertThat(ERROR_STATUS.isUnknown(), is(false));
        assertThat(ERROR_STATUS.isWarning(), is(false));
    }

    @Test
    public void shouldHaveInfoSeverity() {
        assertThat(INFO_STATUS.isInfo(), is(true));

        // make sure other values are false
        assertThat(INFO_STATUS.isError(), is(false));
        assertThat(INFO_STATUS.isOk(), is(false));
        assertThat(INFO_STATUS.isUnknown(), is(false));
        assertThat(INFO_STATUS.isWarning(), is(false));
    }

    @Test
    public void shouldHaveOkSeverity() {
        assertThat(Status.OK_STATUS.isOk(), is(true));

        // make sure other values are false
        assertThat(Status.OK_STATUS.isError(), is(false));
        assertThat(Status.OK_STATUS.isInfo(), is(false));
        assertThat(Status.OK_STATUS.isUnknown(), is(false));
        assertThat(Status.OK_STATUS.isWarning(), is(false));
    }

    @Test
    public void shouldHaveUnknownSeverity() {
        assertThat(UNKNOWN_STATUS.isUnknown(), is(true));

        // make sure other values are false
        assertThat(UNKNOWN_STATUS.isError(), is(false));
        assertThat(UNKNOWN_STATUS.isInfo(), is(false));
        assertThat(UNKNOWN_STATUS.isOk(), is(false));
        assertThat(UNKNOWN_STATUS.isWarning(), is(false));
    }

    @Test
    public void shouldHaveUnknownSeverityWhenNullSeverity() {
        assertThat(NULL_SEVERITY_STATUS.isUnknown(), is(true));

        // make sure other values are false
        assertThat(NULL_SEVERITY_STATUS.isError(), is(false));
        assertThat(NULL_SEVERITY_STATUS.isInfo(), is(false));
        assertThat(NULL_SEVERITY_STATUS.isOk(), is(false));
        assertThat(NULL_SEVERITY_STATUS.isWarning(), is(false));
    }

    @Test
    public void shouldHaveWarningSeverity() {
        assertThat(WARNING_STATUS.isWarning(), is(true));

        // make sure other values are false
        assertThat(WARNING_STATUS.isError(), is(false));
        assertThat(WARNING_STATUS.isInfo(), is(false));
        assertThat(WARNING_STATUS.isOk(), is(false));
        assertThat(WARNING_STATUS.isUnknown(), is(false));
    }

    @Test
    public void shouldNotHaveNullMessageWhenConstructedWithNullMessage() {
        assertThat(new Status(Severity.WARNING, null, null).getMessage(), not(isNull()));
    }

    @Test
    public void shouldBeAbleToPrintWithMessageAndNullException() {
        new Status(Severity.WARNING, "the message goes here", null).toString();
    }

    @Test
    public void shouldBeAbleToPrintWithMessageAndException() {
        new Status(Severity.WARNING, "the message goes here", new RuntimeException("exception message")).toString();
    }

    @Test
    public void shouldBeAbleToPrintWithNullMessageAndException() {
        new Status(Severity.WARNING, null, new RuntimeException("exception message")).toString();
    }

    @Test
    public void shouldBeAbleToPrintWithNullMessageAndNullException() {
        new Status(Severity.WARNING, null, null).toString();
    }

}
