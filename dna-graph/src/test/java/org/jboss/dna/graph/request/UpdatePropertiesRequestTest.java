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
import static org.junit.matchers.JUnitMatchers.hasItems;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.request.Request;
import org.jboss.dna.graph.request.UpdatePropertiesRequest;
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

    @Override
    protected Request createRequest() {
        return new UpdatePropertiesRequest(validPathLocation1, validProperty1);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNullFromLocation() {
        new UpdatePropertiesRequest(null, validProperty1);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNullPropertyName() {
        new UpdatePropertiesRequest(validPathLocation, (Property[])null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithEmptyPropertyNameArray() {
        new UpdatePropertiesRequest(validPathLocation, new Property[] {});
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNullPropertyNameIterator() {
        new UpdatePropertiesRequest(validPathLocation, (Iterator<Property>)null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithEmptyPropertyNameIterator() {
        new UpdatePropertiesRequest(validPathLocation, new ArrayList<Property>().iterator());
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNullPropertyNameIterable() {
        new UpdatePropertiesRequest(validPathLocation, (Iterable<Property>)null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithEmptyPropertyNameIterable() {
        new UpdatePropertiesRequest(validPathLocation, new ArrayList<Property>());
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
