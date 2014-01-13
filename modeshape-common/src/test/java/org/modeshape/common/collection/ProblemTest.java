/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.common.collection;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.modeshape.common.CommonI18n;
import org.modeshape.common.collection.Problem.Status;
import org.modeshape.common.i18n.I18n;
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
