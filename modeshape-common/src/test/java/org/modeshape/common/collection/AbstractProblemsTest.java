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
package org.modeshape.common.collection;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.Iterator;
import org.modeshape.common.CommonI18n;
import org.modeshape.common.collection.Problem.Status;
import org.modeshape.common.i18n.I18n;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public abstract class AbstractProblemsTest {

    private Problems problems;
    private Problem error;
    private Problem warning;
    private Problem info;
    private I18n message;
    private Throwable throwable;
    private String location;
    private String resource;

    @Before
    public void beforeEach() throws Exception {
        problems = createProblems();
        message = CommonI18n.argumentMayNotBeNull;
        error = new Problem(Status.ERROR, 1, message, new Object[] {"error msg"}, null, null, null);
        warning = new Problem(Status.WARNING, 1, message, new Object[] {"warning msg"}, null, null, null);
        info = new Problem(Status.INFO, 1, message, new Object[] {"info msg"}, null, null, null);
        throwable = new IllegalArgumentException(message.text("throwable"));
        resource = "SomeResource";
        location = "/Meaningless/location";
    }

    protected abstract Problems createProblems();

    @Test
    public void shouldBeEmptyImmediatelyAfterInstantiation() {
        assertThat(problems.isEmpty(), is(true));
        assertThat(problems.size(), is(0));
    }

    @Test
    public void shouldAddErrorByMessageAndParametersUsingDefaultCode() {
        error = new Problem(Status.ERROR, Problem.DEFAULT_CODE, message, new Object[] {"error msg"}, null, null, null);
        problems.addError(error.getMessage(), error.getParameters());
        assertThat(problems.iterator().next(), is(error));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(1));
    }

    @Test
    public void shouldAddErrorByCodeAndMesssageAndParameters() {
        problems.addError(error.getCode(), error.getMessage(), error.getParameters());
        assertThat(problems.iterator().next(), is(error));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(1));
    }

    @Test
    public void shouldAddErrorByThrowableAndMessageAndParametersUsingDefaultCode() {
        error = new Problem(Status.ERROR, Problem.DEFAULT_CODE, message, new Object[] {"error msg"}, null, null, throwable);
        problems.addError(error.getThrowable(), error.getMessage(), error.getParameters());
        assertThat(problems.iterator().next(), is(error));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(1));
    }

    @Test
    public void shouldAddErrorByThrowableAndCodeAndMessageAndParameters() {
        error = new Problem(Status.ERROR, 1, message, new Object[] {"error msg"}, null, null, throwable);
        problems.addError(error.getThrowable(), error.getCode(), error.getMessage(), error.getParameters());
        assertThat(problems.iterator().next(), is(error));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(1));
    }

    @Test
    public void shouldAddErrorByMessageAndResourceAndLocationAndParametersUsingDefaultCode() {
        error = new Problem(Status.ERROR, Problem.DEFAULT_CODE, message, new Object[] {"error msg"}, resource, location, null);
        problems.addError(error.getResource(), error.getLocation(), error.getMessage(), error.getParameters());
        assertThat(problems.iterator().next(), is(error));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(1));
    }

    @Test
    public void shouldAddErrorByCodeAndMesssageAndResourceAndLocationAndParameters() {
        error = new Problem(Status.ERROR, 1, message, new Object[] {"error msg"}, resource, location, null);
        problems.addError(error.getCode(), error.getResource(), error.getLocation(), error.getMessage(), error.getParameters());
        assertThat(problems.iterator().next(), is(error));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(1));
    }

    @Test
    public void shouldAddErrorByThrowableAndMessageAndResourceAndLocationAndParametersUsingDefaultCode() {
        error = new Problem(Status.ERROR, Problem.DEFAULT_CODE, message, new Object[] {"error msg"}, resource, location,
                            throwable);
        problems.addError(error.getThrowable(),
                          error.getResource(),
                          error.getLocation(),
                          error.getMessage(),
                          error.getParameters());
        assertThat(problems.iterator().next(), is(error));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(1));
    }

    @Test
    public void shouldAddErrorByThrowableAndCodeAndMessageAndResourceAndLocationAndParameters() {
        error = new Problem(Status.ERROR, 1, message, new Object[] {"error msg"}, resource, location, throwable);
        problems.addError(error.getThrowable(),
                          error.getCode(),
                          error.getResource(),
                          error.getLocation(),
                          error.getMessage(),
                          error.getParameters());
        assertThat(problems.iterator().next(), is(error));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(1));
    }

    @Test
    public void shouldAddWarningByMessageAndParametersUsingDefaultCode() {
        warning = new Problem(Status.WARNING, Problem.DEFAULT_CODE, message, new Object[] {"warning msg"}, null, null, null);
        problems.addWarning(warning.getMessage(), warning.getParameters());
        assertThat(problems.iterator().next(), is(warning));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(1));
    }

    @Test
    public void shouldAddWarningByCodeAndMesssageAndParameters() {
        problems.addWarning(warning.getCode(), warning.getMessage(), warning.getParameters());
        assertThat(problems.iterator().next(), is(warning));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(1));
    }

    @Test
    public void shouldAddWarningByThrowableAndMessageAndParametersUsingDefaultCode() {
        warning = new Problem(Status.WARNING, Problem.DEFAULT_CODE, message, new Object[] {"warning msg"}, null, null, throwable);
        problems.addWarning(warning.getThrowable(), warning.getMessage(), warning.getParameters());
        assertThat(problems.iterator().next(), is(warning));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(1));
    }

    @Test
    public void shouldAddWarningByThrowableAndCodeAndMessageAndParameters() {
        warning = new Problem(Status.WARNING, 1, message, new Object[] {"warning msg"}, null, null, throwable);
        problems.addWarning(warning.getThrowable(), warning.getCode(), warning.getMessage(), warning.getParameters());
        assertThat(problems.iterator().next(), is(warning));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(1));
    }

    @Test
    public void shouldAddWarningByMessageAndResourceAndLocationAndParametersUsingDefaultCode() {
        warning = new Problem(Status.WARNING, Problem.DEFAULT_CODE, message, new Object[] {"warning msg"}, resource, location,
                              null);
        problems.addWarning(warning.getResource(), warning.getLocation(), warning.getMessage(), warning.getParameters());
        assertThat(problems.iterator().next(), is(warning));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(1));
    }

    @Test
    public void shouldAddWarningByCodeAndMesssageAndResourceAndLocationAndParameters() {
        warning = new Problem(Status.WARNING, 1, message, new Object[] {"warning msg"}, resource, location, null);
        problems.addWarning(warning.getCode(),
                            warning.getResource(),
                            warning.getLocation(),
                            warning.getMessage(),
                            warning.getParameters());
        assertThat(problems.iterator().next(), is(warning));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(1));
    }

    @Test
    public void shouldAddWarningByThrowableAndMessageAndResourceAndLocationAndParametersUsingDefaultCode() {
        warning = new Problem(Status.WARNING, Problem.DEFAULT_CODE, message, new Object[] {"warning msg"}, resource, location,
                              throwable);
        problems.addWarning(warning.getThrowable(),
                            warning.getResource(),
                            warning.getLocation(),
                            warning.getMessage(),
                            warning.getParameters());
        assertThat(problems.iterator().next(), is(warning));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(1));
    }

    @Test
    public void shouldAddWarningByThrowableAndCodeAndMessageAndResourceAndLocationAndParameters() {
        warning = new Problem(Status.WARNING, 1, message, new Object[] {"warning msg"}, resource, location, throwable);
        problems.addWarning(warning.getThrowable(),
                            warning.getCode(),
                            warning.getResource(),
                            warning.getLocation(),
                            warning.getMessage(),
                            warning.getParameters());
        assertThat(problems.iterator().next(), is(warning));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(1));
    }

    @Test
    public void shouldAddInfoByMessageAndParametersUsingDefaultCode() {
        info = new Problem(Status.INFO, Problem.DEFAULT_CODE, message, new Object[] {"info msg"}, null, null, null);
        problems.addInfo(info.getMessage(), info.getParameters());
        assertThat(problems.iterator().next(), is(info));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(1));
    }

    @Test
    public void shouldAddInfoByCodeAndMesssageAndParameters() {
        problems.addInfo(info.getCode(), info.getMessage(), info.getParameters());
        assertThat(problems.iterator().next(), is(info));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(1));
    }

    @Test
    public void shouldAddInfoByThrowableAndMessageAndParametersUsingDefaultCode() {
        info = new Problem(Status.INFO, Problem.DEFAULT_CODE, message, new Object[] {"info msg"}, null, null, throwable);
        problems.addInfo(info.getThrowable(), info.getMessage(), info.getParameters());
        assertThat(problems.iterator().next(), is(info));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(1));
    }

    @Test
    public void shouldAddInfoByThrowableAndCodeAndMessageAndParameters() {
        info = new Problem(Status.INFO, 1, message, new Object[] {"info msg"}, null, null, throwable);
        problems.addInfo(info.getThrowable(), info.getCode(), info.getMessage(), info.getParameters());
        assertThat(problems.iterator().next(), is(info));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(1));
    }

    @Test
    public void shouldAddInfoByMessageAndResourceAndLocationAndParametersUsingDefaultCode() {
        info = new Problem(Status.INFO, Problem.DEFAULT_CODE, message, new Object[] {"info msg"}, resource, location, null);
        problems.addInfo(info.getResource(), info.getLocation(), info.getMessage(), info.getParameters());
        assertThat(problems.iterator().next(), is(info));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(1));
    }

    @Test
    public void shouldAddInfoByCodeAndMesssageAndResourceAndLocationAndParameters() {
        info = new Problem(Status.INFO, 1, message, new Object[] {"info msg"}, resource, location, null);
        problems.addInfo(info.getCode(), info.getResource(), info.getLocation(), info.getMessage(), info.getParameters());
        assertThat(problems.iterator().next(), is(info));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(1));
    }

    @Test
    public void shouldAddInfoByThrowableAndMessageAndResourceAndLocationAndParametersUsingDefaultCode() {
        info = new Problem(Status.INFO, Problem.DEFAULT_CODE, message, new Object[] {"info msg"}, resource, location, throwable);
        problems.addInfo(info.getThrowable(), info.getResource(), info.getLocation(), info.getMessage(), info.getParameters());
        assertThat(problems.iterator().next(), is(info));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(1));
    }

    @Test
    public void shouldAddInfoByThrowableAndCodeAndMessageAndResourceAndLocationAndParameters() {
        info = new Problem(Status.INFO, 1, message, new Object[] {"info msg"}, resource, location, throwable);
        problems.addInfo(info.getThrowable(),
                         info.getCode(),
                         info.getResource(),
                         info.getLocation(),
                         info.getMessage(),
                         info.getParameters());
        assertThat(problems.iterator().next(), is(info));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(1));
    }

    @Test
    public void shouldAddProblemsAndMaintainOrder() {
        assertThat(problems.hasErrors(), is(false));
        assertThat(problems.hasWarnings(), is(false));
        assertThat(problems.hasInfo(), is(false));
        assertThat(problems.isEmpty(), is(true));
        assertThat(problems.size(), is(0));
        problems.addWarning(warning.getThrowable(),
                            warning.getCode(),
                            warning.getResource(),
                            warning.getLocation(),
                            warning.getMessage(),
                            warning.getParameters());
        assertThat(problems.hasErrors(), is(false));
        assertThat(problems.hasWarnings(), is(true));
        assertThat(problems.hasInfo(), is(false));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(1));
        problems.addError(error.getThrowable(),
                          error.getCode(),
                          error.getResource(),
                          error.getLocation(),
                          error.getMessage(),
                          error.getParameters());
        assertThat(problems.hasErrors(), is(true));
        assertThat(problems.hasWarnings(), is(true));
        assertThat(problems.hasInfo(), is(false));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(2));
        problems.addInfo(info.getThrowable(),
                         info.getCode(),
                         info.getResource(),
                         info.getLocation(),
                         info.getMessage(),
                         info.getParameters());
        assertThat(problems.hasErrors(), is(true));
        assertThat(problems.hasWarnings(), is(true));
        assertThat(problems.hasInfo(), is(true));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(3));
        problems.addWarning(warning.getThrowable(),
                            warning.getCode(),
                            warning.getResource(),
                            warning.getLocation(),
                            warning.getMessage(),
                            warning.getParameters());
        problems.addError(error.getThrowable(),
                          error.getCode(),
                          error.getResource(),
                          error.getLocation(),
                          error.getMessage(),
                          error.getParameters());
        problems.addWarning(warning.getThrowable(),
                            warning.getCode(),
                            warning.getResource(),
                            warning.getLocation(),
                            warning.getMessage(),
                            warning.getParameters());
        problems.addError(error.getThrowable(),
                          error.getCode(),
                          error.getResource(),
                          error.getLocation(),
                          error.getMessage(),
                          error.getParameters());
        assertThat(problems.hasErrors(), is(true));
        assertThat(problems.hasWarnings(), is(true));
        assertThat(problems.hasInfo(), is(true));
        assertThat(problems.isEmpty(), is(false));
        assertThat(problems.size(), is(7));
        Iterator<Problem> iter = problems.iterator();
        assertThat(iter.next(), is(warning));
        assertThat(iter.next(), is(error));
        assertThat(iter.next(), is(info));
        assertThat(iter.next(), is(warning));
        assertThat(iter.next(), is(error));
        assertThat(iter.next(), is(warning));
        assertThat(iter.next(), is(error));
        assertThat(iter.hasNext(), is(false));
        assertThat(problems.size(), is(7));
        assertThat(problems.hasErrors(), is(true));
        assertThat(problems.hasWarnings(), is(true));
        assertThat(problems.hasInfo(), is(true));
        assertThat(problems.isEmpty(), is(false));
    }

}
