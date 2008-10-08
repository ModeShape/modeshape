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
import org.jboss.dna.graph.properties.Name;
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

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNullFromLocation() {
        new CopyBranchRequest(null, validPathLocation);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNullToLocation() {
        new CopyBranchRequest(validPathLocation, null);
    }

    @Test
    public void shouldCreateValidRequestWithValidFromLocationAndValidToLocation() {
        request = new RenameNodeRequest(validPathLocation1, newName);
        assertThat(request.at(), is(sameInstance(validPathLocation1)));
        assertThat(request.toName(), is(sameInstance(newName)));
        assertThat(request.hasError(), is(false));
        assertThat(request.getError(), is(nullValue()));
    }

    @Test
    public void shouldConsiderEqualTwoRequestsWithSameLocations() {
        request = new RenameNodeRequest(validPathLocation1, newName);
        RenameNodeRequest request2 = new RenameNodeRequest(validPathLocation1, newName);
        assertThat(request, is(request2));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithDifferentLocations() {
        request = new RenameNodeRequest(validPathLocation1, newName);
        RenameNodeRequest request2 = new RenameNodeRequest(validPathLocation2, newName);
        assertThat(request.equals(request2), is(false));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithSameLocationsAndDifferentNames() {
        request = new RenameNodeRequest(validPathLocation1, newName);
        RenameNodeRequest request2 = new RenameNodeRequest(validPathLocation1, createName("OtherName"));
        assertThat(request.equals(request2), is(false));
    }
}
