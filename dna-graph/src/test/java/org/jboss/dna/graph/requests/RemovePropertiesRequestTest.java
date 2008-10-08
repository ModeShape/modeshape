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
import org.jboss.dna.graph.properties.Name;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class RemovePropertiesRequestTest extends AbstractRequestTest {

    private RemovePropertiesRequest request;
    private Name validPropertyName1;
    private Name validPropertyName2;
    private Name validPropertyName3;

    @Override
    @Before
    public void beforeEach() {
        super.beforeEach();
        validPropertyName1 = createName("foo1");
        validPropertyName2 = createName("foo2");
        validPropertyName3 = createName("foo3");
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
    public void shouldCreateValidRequestWithValidLocationAndValidPropertyName() {
        request = new RemovePropertiesRequest(validPathLocation1, validPropertyName1);
        assertThat(request.from(), is(sameInstance(validPathLocation1)));
        assertThat(request.hasError(), is(false));
        assertThat(request.getError(), is(nullValue()));
        assertThat(request.propertyNames(), hasItems(validPropertyName1));
    }

    @Test
    public void shouldCreateValidRequestWithValidLocationAndValidPropertyNames() {
        request = new RemovePropertiesRequest(validPathLocation1, validPropertyName1, validPropertyName2);
        assertThat(request.from(), is(sameInstance(validPathLocation1)));
        assertThat(request.hasError(), is(false));
        assertThat(request.getError(), is(nullValue()));
        assertThat(request.propertyNames(), hasItems(validPropertyName1, validPropertyName2));
    }

    @Test
    public void shouldCreateValidRequestWithValidLocationAndIteratorOverValidPropertyName() {
        List<Name> names = new ArrayList<Name>();
        names.add(validPropertyName1);
        request = new RemovePropertiesRequest(validPathLocation1, names);
        assertThat(request.from(), is(sameInstance(validPathLocation1)));
        assertThat(request.hasError(), is(false));
        assertThat(request.getError(), is(nullValue()));
        assertThat(request.propertyNames(), hasItems(validPropertyName1));
    }

    @Test
    public void shouldCreateValidRequestWithValidLocationAndIteratorOverValidPropertyNames() {
        List<Name> names = new ArrayList<Name>();
        names.add(validPropertyName1);
        names.add(validPropertyName2);
        names.add(validPropertyName3);
        request = new RemovePropertiesRequest(validPathLocation1, names.iterator());
        assertThat(request.from(), is(sameInstance(validPathLocation1)));
        assertThat(request.hasError(), is(false));
        assertThat(request.getError(), is(nullValue()));
        assertThat(request.propertyNames(), hasItems(validPropertyName1, validPropertyName2, validPropertyName3));
    }

    @Test
    public void shouldCreateValidRequestWithValidLocationAndIterableWithValidPropertyNames() {
        List<Name> names = new ArrayList<Name>();
        names.add(validPropertyName1);
        names.add(validPropertyName2);
        names.add(validPropertyName3);
        request = new RemovePropertiesRequest(validPathLocation1, names);
        assertThat(request.from(), is(sameInstance(validPathLocation1)));
        assertThat(request.hasError(), is(false));
        assertThat(request.getError(), is(nullValue()));
        assertThat(request.propertyNames(), hasItems(validPropertyName1, validPropertyName2, validPropertyName3));
    }

    @Test
    public void shouldConsiderEqualTwoRequestsWithSameLocationsAndSamePropertyNames() {
        request = new RemovePropertiesRequest(validPathLocation1, validPropertyName1, validPropertyName2);
        RemovePropertiesRequest request2 = new RemovePropertiesRequest(validPathLocation1, validPropertyName1, validPropertyName2);
        assertThat(request, is(request2));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithDifferentLocations() {
        request = new RemovePropertiesRequest(validPathLocation1, validPropertyName1, validPropertyName2);
        RemovePropertiesRequest request2 = new RemovePropertiesRequest(validPathLocation2, validPropertyName1, validPropertyName2);
        assertThat(request.equals(request2), is(false));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithSameLocationButDifferentPropertyNames() {
        request = new RemovePropertiesRequest(validPathLocation1, validPropertyName1, validPropertyName2);
        RemovePropertiesRequest request2 = new RemovePropertiesRequest(validPathLocation2, validPropertyName2, validPropertyName3);
        assertThat(request.equals(request2), is(false));
    }

}
