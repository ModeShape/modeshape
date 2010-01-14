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
import java.util.Iterator;
import org.modeshape.graph.Location;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class ReadBranchRequestTest extends AbstractRequestTest {

    private ReadBranchRequest request;

    @Override
    @Before
    public void beforeEach() {
        super.beforeEach();
    }

    @Override
    protected Request createRequest() {
        return new ReadBranchRequest(validPathLocation1, workspace1);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNullFromLocation() {
        new ReadBranchRequest(null, workspace1);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNullWorkspaceName() {
        new ReadBranchRequest(validPathLocation1, null);
    }

    @Test
    public void shouldCreateValidRequestWithValidLocation() {
        request = new ReadBranchRequest(validPathLocation1, workspace1);
        assertThat(request.at(), is(sameInstance(validPathLocation1)));
        assertThat(request.inWorkspace(), is(sameInstance(workspace1)));
        assertThat(request.hasError(), is(false));
        assertThat(request.getError(), is(nullValue()));
        assertThat(request.maximumDepth(), is(ReadBranchRequest.DEFAULT_MAXIMUM_DEPTH));
    }

    @Test
    public void shouldCreateValidRequestWithValidLocationAndMaximumDepth() {
        request = new ReadBranchRequest(validPathLocation1, workspace1, 10);
        assertThat(request.at(), is(sameInstance(validPathLocation1)));
        assertThat(request.hasError(), is(false));
        assertThat(request.getError(), is(nullValue()));
        assertThat(request.maximumDepth(), is(10));
    }

    @Test
    public void shouldConsiderEqualTwoRequestsWithSameLocations() {
        request = new ReadBranchRequest(validPathLocation1, workspace1);
        ReadBranchRequest request2 = new ReadBranchRequest(validPathLocation1, new String(workspace1));
        assertThat(request, is(request2));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithDifferentLocations() {
        request = new ReadBranchRequest(validPathLocation1, workspace1, 20);
        ReadBranchRequest request2 = new ReadBranchRequest(validPathLocation2, workspace1, 20);
        assertThat(request.equals(request2), is(false));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithDifferentWorkspaceNames() {
        request = new ReadBranchRequest(validPathLocation1, workspace1, 20);
        ReadBranchRequest request2 = new ReadBranchRequest(validPathLocation1, workspace2, 20);
        assertThat(request.equals(request2), is(false));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithSameLocationsButDifferentMaximumDepths() {
        request = new ReadBranchRequest(validPathLocation1, workspace1, 20);
        ReadBranchRequest request2 = new ReadBranchRequest(validPathLocation1, workspace1, 2);
        assertThat(request.equals(request2), is(false));
    }

    @Test
    public void shouldIterateOverNodesInBranchOfDepthOne() {
        request = new ReadBranchRequest(location("/a"), workspace1, 1);
        request.setActualLocationOfNode(request.at());
        request.setChildren(location("/a"), location("/a/b"), location("/a/c"));
        Iterator<Location> actual = request.iterator();
        assertThat(actual.hasNext(), is(true));
        assertThat(actual.next(), is(location("/a")));
        assertThat(actual.hasNext(), is(false));
    }

    @Test
    public void shouldIterateOverNodesInBranchOfDepthTwo() {
        request = new ReadBranchRequest(location("/a"), workspace1, 2);
        request.setActualLocationOfNode(request.at());
        request.setChildren(location("/a"), location("/a/b"), location("/a/c"), location("/a/d"));
        request.setChildren(location("/a/b"), location("/a/b/j"), location("/a/b/k"));
        request.setChildren(location("/a/c"), location("/a/c/j"), location("/a/c/k"));
        request.setChildren(location("/a/d"));
        Iterator<Location> actual = request.iterator();
        assertThat(actual.hasNext(), is(true));
        assertThat(actual.next(), is(location("/a")));
        assertThat(actual.hasNext(), is(true));
        assertThat(actual.next(), is(location("/a/b")));
        assertThat(actual.hasNext(), is(true));
        assertThat(actual.next(), is(location("/a/c")));
        assertThat(actual.hasNext(), is(true));
        assertThat(actual.next(), is(location("/a/d")));
        assertThat(actual.hasNext(), is(false));
    }

    @Test
    public void shouldIterateOverNodesInBranchOfDepthThree() {
        request = new ReadBranchRequest(location("/a"), workspace1, 3);
        request.setActualLocationOfNode(request.at());
        request.setChildren(location("/a"), location("/a/b"), location("/a/c"), location("/a/d"));
        request.setChildren(location("/a/b"), location("/a/b/j"), location("/a/b/k"));
        request.setChildren(location("/a/c"), location("/a/c/j"), location("/a/c/k"));
        request.setChildren(location("/a/c/j"), location("/a/c/j/j1"), location("/a/c/j/j2"));
        request.setChildren(location("/a/c/k"), location("/a/c/k/k1"), location("/a/c/k/k2"));
        request.setChildren(location("/a/b/j"), location("/a/b/j/m"), location("/a/b/j/n"));
        request.setChildren(location("/a/b/k"));
        request.setChildren(location("/a/d"));
        Iterator<Location> actual = request.iterator();
        assertThat(actual.hasNext(), is(true));
        assertThat(actual.next(), is(location("/a")));
        assertThat(actual.hasNext(), is(true));
        assertThat(actual.next(), is(location("/a/b")));
        assertThat(actual.hasNext(), is(true));
        assertThat(actual.next(), is(location("/a/b/j")));
        assertThat(actual.hasNext(), is(true));
        assertThat(actual.next(), is(location("/a/b/k")));
        assertThat(actual.hasNext(), is(true));
        assertThat(actual.next(), is(location("/a/c")));
        assertThat(actual.hasNext(), is(true));
        assertThat(actual.next(), is(location("/a/c/j")));
        assertThat(actual.hasNext(), is(true));
        assertThat(actual.next(), is(location("/a/c/k")));
        assertThat(actual.hasNext(), is(true));
        assertThat(actual.next(), is(location("/a/d")));
        assertThat(actual.hasNext(), is(false));
    }
}
