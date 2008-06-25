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
package org.jboss.dna.common.collection;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.jboss.dna.common.CommonI18n;
import org.jboss.dna.common.collection.Problem.Status;
import org.jboss.dna.common.i18n.I18n;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class ProblemTest {

    private Problem error;
    private Problem warning;
    private Problem info;
    private I18n message;
    private Object[] messageParameters;
    private Throwable throwable;
    private String location;
    private String resource;

    @Before
    public void beforeEach() throws Exception {
        message = CommonI18n.argumentMayNotBeNull;
        throwable = new IllegalArgumentException(message.text("throwable"));
        messageParameters = new Object[] {"message"};
        resource = "SomeResource";
        location = "/Meaningless/location";
        error = new Problem(Status.ERROR, 1, message, messageParameters, resource, location, throwable);
        warning = new Problem(Status.WARNING, 1, message, messageParameters, resource, location, throwable);
        info = new Problem(Status.INFO, 1, message, messageParameters, resource, location, throwable);
    }

    @Test
    public void shouldHaveToString() {
        assertThat(error.toString(), is("ERROR: (1) " + message.text("message") + " Resource=\"" + resource + "\" At \""
                                        + location + "\" (threw " + throwable.getLocalizedMessage() + ")"));
    }

    @Test
    public void shouldHaveToStringWithoutDefaultCode() {
        error = new Problem(Status.ERROR, Problem.DEFAULT_CODE, message, new Object[] {"message"}, null, null, null);
        assertThat(error.toString(), is("ERROR: " + message.text("message")));
    }

    @Test
    public void shouldHaveMessageString() {
        messageParameters = new Object[] {"error msg"};
        error = new Problem(Status.ERROR, 1, message, messageParameters, resource, location, throwable);
        messageParameters = new Object[] {"warning msg"};
        warning = new Problem(Status.WARNING, 1, message, messageParameters, resource, location, throwable);
        messageParameters = new Object[] {"info msg"};
        info = new Problem(Status.INFO, 1, message, messageParameters, resource, location, throwable);
        assertThat(error.getMessageString(), is(message.text("error msg")));
        assertThat(warning.getMessageString(), is(message.text("warning msg")));
        assertThat(info.getMessageString(), is(message.text("info msg")));
    }
}
