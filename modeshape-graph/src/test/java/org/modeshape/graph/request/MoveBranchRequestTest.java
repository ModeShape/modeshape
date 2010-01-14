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
import org.modeshape.graph.NodeConflictBehavior;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.basic.BasicName;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class MoveBranchRequestTest extends AbstractRequestTest {

    private MoveBranchRequest request;

    @Override
    @Before
    public void beforeEach() {
        super.beforeEach();
    }

    @Override
    protected Request createRequest() {
        return new MoveBranchRequest(validPathLocation1, validPathLocation2, workspace2);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNullFromLocation() {
        new MoveBranchRequest(null, validPathLocation2, workspace2);
    }

    @Test
    public void shouldAllowCreatingRequestWithNullToLocation() {
        new MoveBranchRequest(validPathLocation1, null, workspace2);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNullWorkspaceName() {
        new MoveBranchRequest(validPathLocation1, validPathLocation2, null);
    }

    @Test
    public void shouldCreateValidRequestWithValidFromLocationAndValidToLocation() {
        request = new MoveBranchRequest(validPathLocation1, validPathLocation2, workspace1);
        assertThat(request.from(), is(sameInstance(validPathLocation1)));
        assertThat(request.into(), is(sameInstance(validPathLocation2)));
        assertThat(request.inWorkspace(), is(sameInstance(workspace1)));
        assertThat(request.hasError(), is(false));
        assertThat(request.getError(), is(nullValue()));
    }

    @Test
    public void shouldCreateValidRequestWithValidFromLocationAndValidToLocationAndValidBeforeLocation() {
        Name newName = new BasicName("", "newName");
        request = new MoveBranchRequest(validPathLocation1, validPathLocation2, validPathLocation, workspace1, newName, NodeConflictBehavior.DO_NOT_REPLACE);
        assertThat(request.from(), is(sameInstance(validPathLocation1)));
        assertThat(request.into(), is(sameInstance(validPathLocation2)));
        assertThat(request.before(), is(sameInstance(validPathLocation)));
        assertThat(request.inWorkspace(), is(sameInstance(workspace1)));
        assertThat(request.hasError(), is(false));
        assertThat(request.getError(), is(nullValue()));
    }

    @Test
    public void shouldConsiderEqualTwoRequestsWithSameLocations() {
        request = new MoveBranchRequest(validPathLocation1, validPathLocation2, workspace2);
        MoveBranchRequest request2 = new MoveBranchRequest(validPathLocation1, validPathLocation2, workspace2);
        assertThat(request, is(request2));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithDifferentLocations() {
        request = new MoveBranchRequest(validPathLocation1, validPathLocation2, workspace2);
        MoveBranchRequest request2 = new MoveBranchRequest(validPathLocation2, validPathLocation1, workspace2);
        assertThat(request.equals(request2), is(false));
    }
}
