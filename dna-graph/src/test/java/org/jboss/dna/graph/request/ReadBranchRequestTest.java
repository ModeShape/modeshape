/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.request;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import org.jboss.dna.graph.request.ReadBranchRequest;
import org.jboss.dna.graph.request.Request;
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
        return new ReadBranchRequest(validPathLocation1);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNullFromLocation() {
        new ReadBranchRequest(null);
    }

    @Test
    public void shouldCreateValidRequestWithValidLocation() {
        request = new ReadBranchRequest(validPathLocation1);
        assertThat(request.at(), is(sameInstance(validPathLocation1)));
        assertThat(request.hasError(), is(false));
        assertThat(request.getError(), is(nullValue()));
        assertThat(request.maximumDepth(), is(ReadBranchRequest.DEFAULT_MAXIMUM_DEPTH));
    }

    @Test
    public void shouldCreateValidRequestWithValidLocationAndMaximumDepth() {
        request = new ReadBranchRequest(validPathLocation1, 10);
        assertThat(request.at(), is(sameInstance(validPathLocation1)));
        assertThat(request.hasError(), is(false));
        assertThat(request.getError(), is(nullValue()));
        assertThat(request.maximumDepth(), is(10));
    }

    @Test
    public void shouldConsiderEqualTwoRequestsWithSameLocations() {
        request = new ReadBranchRequest(validPathLocation1);
        ReadBranchRequest request2 = new ReadBranchRequest(validPathLocation1);
        assertThat(request, is(request2));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithDifferentLocations() {
        request = new ReadBranchRequest(validPathLocation1, 20);
        ReadBranchRequest request2 = new ReadBranchRequest(validPathLocation2, 20);
        assertThat(request.equals(request2), is(false));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithSameLocationsButDifferentMaximumDepths() {
        request = new ReadBranchRequest(validPathLocation1, 20);
        ReadBranchRequest request2 = new ReadBranchRequest(validPathLocation1, 2);
        assertThat(request.equals(request2), is(false));
    }
}
