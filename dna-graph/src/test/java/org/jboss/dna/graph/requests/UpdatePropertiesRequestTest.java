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
import static org.junit.matchers.JUnitMatchers.hasItems;
import java.util.ArrayList;
import java.util.List;
import org.jboss.dna.graph.properties.Property;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class UpdatePropertiesRequestTest extends AbstractRequestTest {

    private UpdatePropertiesRequest request;

    @Override
    @Before
    public void beforeEach() {
        super.beforeEach();
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
    public void shouldCreateValidRequestWithValidLocationAndValidProperty() {
        request = new UpdatePropertiesRequest(validPathLocation1, validProperty1);
        assertThat(request.on(), is(sameInstance(validPathLocation1)));
        assertThat(request.hasError(), is(false));
        assertThat(request.getError(), is(nullValue()));
        assertThat(request.properties(), hasItems(validProperty1));
    }

    @Test
    public void shouldCreateValidRequestWithValidLocationAndValidPropertyNames() {
        request = new UpdatePropertiesRequest(validPathLocation1, validProperty1, validProperty2);
        assertThat(request.on(), is(sameInstance(validPathLocation1)));
        assertThat(request.hasError(), is(false));
        assertThat(request.getError(), is(nullValue()));
        assertThat(request.properties(), hasItems(validProperty1, validProperty2));
    }

    @Test
    public void shouldCreateValidRequestWithValidLocationAndIteratorOverValidPropertyName() {
        List<Property> properties = new ArrayList<Property>();
        properties.add(validProperty1);
        request = new UpdatePropertiesRequest(validPathLocation1, properties.iterator());
        assertThat(request.on(), is(sameInstance(validPathLocation1)));
        assertThat(request.hasError(), is(false));
        assertThat(request.getError(), is(nullValue()));
        assertThat(request.properties(), hasItems(validProperty1));
    }

    @Test
    public void shouldCreateValidRequestWithValidLocationAndIteratorOverValidPropertyNames() {
        List<Property> properties = new ArrayList<Property>();
        properties.add(validProperty1);
        properties.add(validProperty2);
        request = new UpdatePropertiesRequest(validPathLocation1, properties.iterator());
        assertThat(request.on(), is(sameInstance(validPathLocation1)));
        assertThat(request.hasError(), is(false));
        assertThat(request.getError(), is(nullValue()));
        assertThat(request.properties(), hasItems(validProperty1, validProperty2));
    }

    @Test
    public void shouldCreateValidRequestWithValidLocationAndIterableWithValidPropertyName() {
        List<Property> properties = new ArrayList<Property>();
        properties.add(validProperty1);
        request = new UpdatePropertiesRequest(validPathLocation1, properties);
        assertThat(request.on(), is(sameInstance(validPathLocation1)));
        assertThat(request.hasError(), is(false));
        assertThat(request.getError(), is(nullValue()));
        assertThat(request.properties(), hasItems(validProperty1));
    }

    @Test
    public void shouldCreateValidRequestWithValidLocationAndIterableWithValidPropertyNames() {
        List<Property> properties = new ArrayList<Property>();
        properties.add(validProperty1);
        properties.add(validProperty2);
        request = new UpdatePropertiesRequest(validPathLocation1, properties);
        assertThat(request.on(), is(sameInstance(validPathLocation1)));
        assertThat(request.hasError(), is(false));
        assertThat(request.getError(), is(nullValue()));
        assertThat(request.properties(), hasItems(validProperty1, validProperty2));
    }

    @Test
    public void shouldConsiderEqualTwoRequestsWithSameLocationsAndSamePropertyNames() {
        request = new UpdatePropertiesRequest(validPathLocation1, validProperty1, validProperty2);
        UpdatePropertiesRequest request2 = new UpdatePropertiesRequest(validPathLocation1, validProperty1, validProperty2);
        assertThat(request, is(request2));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithDifferentLocations() {
        request = new UpdatePropertiesRequest(validPathLocation1, validProperty1, validProperty2);
        UpdatePropertiesRequest request2 = new UpdatePropertiesRequest(validPathLocation2, validProperty1, validProperty2);
        assertThat(request.equals(request2), is(false));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithSameLocationButDifferentPropertyNames() {
        request = new UpdatePropertiesRequest(validPathLocation1, validProperty1, validProperty2);
        UpdatePropertiesRequest request2 = new UpdatePropertiesRequest(validPathLocation2, validProperty1);
        assertThat(request.equals(request2), is(false));
    }

}
