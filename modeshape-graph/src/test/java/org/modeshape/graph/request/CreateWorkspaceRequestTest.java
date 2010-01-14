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
import static org.junit.Assert.assertThat;
import org.modeshape.graph.request.CreateWorkspaceRequest.CreateConflictBehavior;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class CreateWorkspaceRequestTest extends AbstractRequestTest {

    private CreateWorkspaceRequest request;

    @Override
    @Before
    public void beforeEach() {
        super.beforeEach();
    }

    @Override
    protected Request createRequest() {
        return new CreateWorkspaceRequest(workspace1, CreateConflictBehavior.DO_NOT_CREATE);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNullWorkspaceName() {
        new CreateWorkspaceRequest(null, CreateConflictBehavior.DO_NOT_CREATE);
    }

    @Test
    public void shouldAllowCreatingRequestWithNullCreateConflictBehavior() {
        request = new CreateWorkspaceRequest(workspace1, null);
        assertThat(request.conflictBehavior(), is(CreateWorkspaceRequest.DEFAULT_CREATE_CONFLICT_BEHAVIOR));
    }

    @Test
    public void shouldCreateValidRequestWithValidLocation() {
        request = new CreateWorkspaceRequest(workspace1, CreateConflictBehavior.CREATE_WITH_ADJUSTED_NAME);
        assertThat(request.desiredNameOfNewWorkspace(), is(workspace1));
        assertThat(request.conflictBehavior(), is(CreateConflictBehavior.CREATE_WITH_ADJUSTED_NAME));
        assertThat(request.hasError(), is(false));
        assertThat(request.getError(), is(nullValue()));
    }

    @Test
    public void shouldConsiderEqualTwoRequestsWithSameLocations() {
        request = new CreateWorkspaceRequest(workspace1, CreateConflictBehavior.CREATE_WITH_ADJUSTED_NAME);
        CreateWorkspaceRequest request2 = new CreateWorkspaceRequest(workspace1, CreateConflictBehavior.CREATE_WITH_ADJUSTED_NAME);
        assertThat(request, is(request2));
    }

    @Test
    public void shouldConsiderEqualTwoRequestsWithDifferentCreateConflictBehaviors() {
        request = new CreateWorkspaceRequest(workspace1, CreateConflictBehavior.DO_NOT_CREATE);
        CreateWorkspaceRequest request2 = new CreateWorkspaceRequest(workspace1, CreateConflictBehavior.CREATE_WITH_ADJUSTED_NAME);
        assertThat(request, is(request2));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithDifferentDesiredWorkspaceNames() {
        request = new CreateWorkspaceRequest(workspace1, CreateConflictBehavior.CREATE_WITH_ADJUSTED_NAME);
        CreateWorkspaceRequest request2 = new CreateWorkspaceRequest(workspace2, CreateConflictBehavior.CREATE_WITH_ADJUSTED_NAME);
        assertThat(request.equals(request2), is(false));
    }
}
