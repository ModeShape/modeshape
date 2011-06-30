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
package org.modeshape.graph.request;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import java.io.Serializable;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.request.function.Function;
import org.modeshape.graph.request.function.FunctionContext;

public class FunctionRequestTest extends AbstractRequestTest {

    private Function function;
    private FunctionRequest request;
    private FunctionRequest request2;
    private Map<String, Serializable> inputs;
    private Map<String, Serializable> inputs2;
    protected boolean ran = false;

    @SuppressWarnings( "serial" )
    @Override
    @Before
    public void beforeEach() {
        super.beforeEach();
        ran = false;
        inputs = new java.util.HashMap<String, Serializable>();
        inputs2 = new java.util.HashMap<String, Serializable>();
        function = new Function() {
            @Override
            public void run( FunctionContext context ) {
                ran = true;
                context.setOutput("success", Boolean.TRUE);
            }
        };
    }

    @Override
    protected Request createRequest() {
        return new FunctionRequest(function, validPathLocation1, workspace1, inputs);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNullFunction() {
        new FunctionRequest(null, validPathLocation1, workspace1, inputs);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNullLocation() {
        new FunctionRequest(function, null, workspace1, inputs);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNullWorkspaceName() {
        new FunctionRequest(function, validPathLocation1, null, inputs);
    }

    @Test
    public void shouldAllowCreatingRequestWithNullInputMap() {
        new FunctionRequest(function, validPathLocation1, workspace1, null);
    }

    @Test
    public void shouldCreateValidRequestWithValidLocation() {
        request = new FunctionRequest(function, validPathLocation1, workspace1, inputs);
        assertThat(request.function(), is(sameInstance(function)));
        assertThat(request.at(), is(sameInstance(validPathLocation1)));
        assertThat(request.readWorkspace(), is(sameInstance(workspace1)));
        assertThat(request.inputs().isEmpty(), is(true));
        assertThat(request.hasError(), is(false));
        assertThat(request.getError(), is(nullValue()));
    }

    @Test
    public void shouldConsiderEqualTwoRequestsWithSameLocationAndFunctionAndWorkspace() {
        request = new FunctionRequest(function, validPathLocation1, workspace1, inputs);
        request2 = new FunctionRequest(function, validPathLocation1, workspace1, inputs);
        assertThat(request, is(request2));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithDifferentFunctions() {
        @SuppressWarnings( "serial" )
        Function function2 = new Function() {
            @Override
            public void run( FunctionContext context ) {
                ran = true;
                context.setOutput("success", Boolean.TRUE);
            }
        };
        request = new FunctionRequest(function, validPathLocation1, workspace1, inputs);
        request2 = new FunctionRequest(function2, validPathLocation1, workspace1, inputs);
        assertThat(request.equals(request2), is(false));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithDifferentInputs() {
        inputs2.put("Some input", "value");
        request = new FunctionRequest(function, validPathLocation1, workspace1, inputs);
        request2 = new FunctionRequest(function, validPathLocation1, workspace1, inputs2);
        assertThat(request.equals(request2), is(false));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithDifferentLocations() {
        request = new FunctionRequest(function, validPathLocation1, workspace1, inputs);
        request2 = new FunctionRequest(function, validPathLocation2, workspace1, inputs);
        assertThat(request.equals(request2), is(false));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithDifferentWorkspaceNames() {
        request = new FunctionRequest(function, validPathLocation1, workspace1, inputs);
        request2 = new FunctionRequest(function, validPathLocation1, workspace2, inputs);
        assertThat(request.equals(request2), is(false));
    }
}
