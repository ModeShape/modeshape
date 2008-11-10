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
package org.jboss.dna.graph.requests;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class ReadBlockOfChildrenRequestTest extends AbstractRequestTest {

    private ReadBlockOfChildrenRequest request;

    @Override
    @Before
    public void beforeEach() {
        super.beforeEach();
    }

    @Override
    protected Request createRequest() {
        return new ReadBlockOfChildrenRequest(validPathLocation1, 2, 10);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNullFromLocation() {
        new ReadBlockOfChildrenRequest(null, 0, 1);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNegativeStartingIndex() {
        new ReadBlockOfChildrenRequest(validPathLocation1, -1, 1);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNegativeCount() {
        new ReadBlockOfChildrenRequest(validPathLocation1, 1, -1);
    }

    @Test
    public void shouldCreateValidRequestWithValidLocation() {
        request = new ReadBlockOfChildrenRequest(validPathLocation1, 2, 10);
        assertThat(request.of(), is(sameInstance(validPathLocation1)));
        assertThat(request.hasError(), is(false));
        assertThat(request.getError(), is(nullValue()));
    }

    @Test
    public void shouldConsiderEqualTwoRequestsWithSameLocations() {
        request = new ReadBlockOfChildrenRequest(validPathLocation1, 2, 20);
        ReadBlockOfChildrenRequest request2 = new ReadBlockOfChildrenRequest(validPathLocation1, 2, 20);
        assertThat(request, is(request2));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithDifferentLocations() {
        request = new ReadBlockOfChildrenRequest(validPathLocation1, 20, 20);
        ReadBlockOfChildrenRequest request2 = new ReadBlockOfChildrenRequest(validPathLocation2, 2, 20);
        assertThat(request.equals(request2), is(false));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithSameLocationsButDifferentStartingIndexes() {
        request = new ReadBlockOfChildrenRequest(validPathLocation1, 20, 20);
        ReadBlockOfChildrenRequest request2 = new ReadBlockOfChildrenRequest(validPathLocation1, 2, 20);
        assertThat(request.equals(request2), is(false));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithSameLocationsButDifferentBlockSizes() {
        request = new ReadBlockOfChildrenRequest(validPathLocation1, 2, 2);
        ReadBlockOfChildrenRequest request2 = new ReadBlockOfChildrenRequest(validPathLocation1, 2, 20);
        assertThat(request.equals(request2), is(false));
    }
}
