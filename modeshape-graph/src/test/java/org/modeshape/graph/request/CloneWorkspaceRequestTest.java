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
import org.modeshape.graph.request.CloneWorkspaceRequest.CloneConflictBehavior;
import org.modeshape.graph.request.CreateWorkspaceRequest.CreateConflictBehavior;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class CloneWorkspaceRequestTest extends AbstractRequestTest {

    private CloneWorkspaceRequest request;

    @Override
    @Before
    public void beforeEach() {
        super.beforeEach();
    }

    @Override
    protected Request createRequest() {
        return new CloneWorkspaceRequest(workspace1, workspace2, CreateConflictBehavior.DO_NOT_CREATE,
                                         CloneConflictBehavior.SKIP_CLONE);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNullCloneWorkspaceName() {
        new CloneWorkspaceRequest(null, workspace2, CreateConflictBehavior.DO_NOT_CREATE, CloneConflictBehavior.SKIP_CLONE);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNullTargetWorkspaceName() {
        new CloneWorkspaceRequest(workspace1, null, CreateConflictBehavior.DO_NOT_CREATE, CloneConflictBehavior.SKIP_CLONE);
    }

    @Test
    public void shouldAllowCreatingRequestWithNullCreateCreateConflictBehavior() {
        request = new CloneWorkspaceRequest(workspace1, workspace2, null, CloneConflictBehavior.SKIP_CLONE);
        assertThat(request.targetConflictBehavior(), is(CloneWorkspaceRequest.DEFAULT_CREATE_CONFLICT_BEHAVIOR));
        assertThat(request.cloneConflictBehavior(), is(CloneConflictBehavior.SKIP_CLONE));
    }

    @Test
    public void shouldAllowCreatingRequestWithNullCreateCloneConflictBehavior() {
        request = new CloneWorkspaceRequest(workspace1, workspace2, CreateConflictBehavior.CREATE_WITH_ADJUSTED_NAME, null);
        assertThat(request.targetConflictBehavior(), is(CreateConflictBehavior.CREATE_WITH_ADJUSTED_NAME));
        assertThat(request.cloneConflictBehavior(), is(CloneWorkspaceRequest.DEFAULT_CLONE_CONFLICT_BEHAVIOR));
    }

    @Test
    public void shouldCreateValidRequestWithValidLocation() {
        request = new CloneWorkspaceRequest(workspace1, workspace2, CreateConflictBehavior.CREATE_WITH_ADJUSTED_NAME,
                                            CloneConflictBehavior.SKIP_CLONE);
        assertThat(request.nameOfWorkspaceToBeCloned(), is(workspace1));
        assertThat(request.desiredNameOfTargetWorkspace(), is(workspace2));
        assertThat(request.targetConflictBehavior(), is(CreateConflictBehavior.CREATE_WITH_ADJUSTED_NAME));
        assertThat(request.cloneConflictBehavior(), is(CloneConflictBehavior.SKIP_CLONE));
        assertThat(request.hasError(), is(false));
        assertThat(request.getError(), is(nullValue()));
    }

    @Test
    public void shouldConsiderEqualTwoRequestsWithSameLocations() {
        CloneWorkspaceRequest request1 = null;
        CloneWorkspaceRequest request2 = null;
        request1 = new CloneWorkspaceRequest(workspace1, workspace2, CreateConflictBehavior.DO_NOT_CREATE,
                                             CloneConflictBehavior.SKIP_CLONE);
        request2 = new CloneWorkspaceRequest(workspace1, workspace2, CreateConflictBehavior.DO_NOT_CREATE,
                                             CloneConflictBehavior.SKIP_CLONE);
        assertThat(request1, is(request2));
    }

    @Test
    public void shouldConsiderEqualTwoRequestsWithDifferentCreateConflictBehaviors() {
        CloneWorkspaceRequest request1 = null;
        CloneWorkspaceRequest request2 = null;
        request1 = new CloneWorkspaceRequest(workspace1, workspace2, CreateConflictBehavior.CREATE_WITH_ADJUSTED_NAME,
                                             CloneConflictBehavior.SKIP_CLONE);
        request2 = new CloneWorkspaceRequest(workspace1, workspace2, CreateConflictBehavior.DO_NOT_CREATE,
                                             CloneConflictBehavior.SKIP_CLONE);
        assertThat(request1, is(request2));
    }

    @Test
    public void shouldConsiderEqualTwoRequestsWithDifferentCloneConflictBehaviors() {
        CloneWorkspaceRequest request1 = null;
        CloneWorkspaceRequest request2 = null;
        request1 = new CloneWorkspaceRequest(workspace1, workspace2, CreateConflictBehavior.DO_NOT_CREATE,
                                             CloneConflictBehavior.SKIP_CLONE);
        request2 = new CloneWorkspaceRequest(workspace1, workspace2, CreateConflictBehavior.DO_NOT_CREATE,
                                             CloneConflictBehavior.DO_NOT_CLONE);
        assertThat(request1, is(request2));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithDifferentDesiredWorkspaceNames() {
        CloneWorkspaceRequest request1 = null;
        CloneWorkspaceRequest request2 = null;
        request1 = new CloneWorkspaceRequest(workspace1, workspace2, CreateConflictBehavior.DO_NOT_CREATE,
                                             CloneConflictBehavior.SKIP_CLONE);
        request2 = new CloneWorkspaceRequest(workspace2, workspace1, CreateConflictBehavior.DO_NOT_CREATE,
                                             CloneConflictBehavior.DO_NOT_CLONE);
        assertThat(request1.equals(request2), is(false));
    }
}
