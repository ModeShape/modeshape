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
import org.modeshape.graph.property.Name;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class RenameNodeRequestTest extends AbstractRequestTest {

    private RenameNodeRequest request;
    private Name newName;

    @Override
    @Before
    public void beforeEach() {
        super.beforeEach();
        newName = createName("SomethingElse");
    }

    @Override
    protected Request createRequest() {
        return new RenameNodeRequest(validPathLocation1, workspace1, newName);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNullFromLocation() {
        new RenameNodeRequest(null, workspace1, newName);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNullWorkspaceName() {
        new RenameNodeRequest(validPathLocation1, null, newName);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNullNewName() {
        new RenameNodeRequest(validPathLocation1, workspace1, null);
    }

    @Test
    public void shouldCreateValidRequestWithValidFromLocationAndValidToLocation() {
        request = new RenameNodeRequest(validPathLocation1, workspace1, newName);
        assertThat(request.at(), is(sameInstance(validPathLocation1)));
        assertThat(request.inWorkspace(), is(sameInstance(workspace1)));
        assertThat(request.toName(), is(sameInstance(newName)));
        assertThat(request.hasError(), is(false));
        assertThat(request.getError(), is(nullValue()));
    }

    @Test
    public void shouldConsiderEqualTwoRequestsWithSameLocations() {
        request = new RenameNodeRequest(validPathLocation1, new String(workspace1), newName);
        RenameNodeRequest request2 = new RenameNodeRequest(validPathLocation1, workspace1, newName);
        assertThat(request, is(request2));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithDifferentLocations() {
        request = new RenameNodeRequest(validPathLocation1, workspace1, newName);
        RenameNodeRequest request2 = new RenameNodeRequest(validPathLocation2, workspace1, newName);
        assertThat(request.equals(request2), is(false));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithDifferentWorkspaceNames() {
        request = new RenameNodeRequest(validPathLocation1, workspace1, newName);
        RenameNodeRequest request2 = new RenameNodeRequest(validPathLocation1, workspace2, newName);
        assertThat(request.equals(request2), is(false));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithSameLocationsAndDifferentNames() {
        request = new RenameNodeRequest(validPathLocation1, workspace1, newName);
        RenameNodeRequest request2 = new RenameNodeRequest(validPathLocation1, workspace1, createName("OtherName"));
        assertThat(request.equals(request2), is(false));
    }
}
