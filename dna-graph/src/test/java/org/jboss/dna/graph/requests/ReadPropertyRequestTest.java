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
import org.jboss.dna.graph.properties.Property;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class ReadPropertyRequestTest extends AbstractRequestTest {

    private ReadPropertyRequest request;
    private Property validProperty;
    private Name validPropertyName;

    @Override
    @Before
    public void beforeEach() {
        super.beforeEach();
        validProperty = validProperty1;
        validPropertyName = validProperty.getName();
    }

    @Override
    protected Request createRequest() {
        return new ReadPropertyRequest(validPathLocation1, validPropertyName);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNullLocation() {
        new ReadPropertyRequest(null, validPropertyName);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNullPropertyName() {
        new CopyBranchRequest(validPathLocation, null);
    }

    @Test
    public void shouldCreateValidRequestWithValidLocation() {
        request = new ReadPropertyRequest(validPathLocation1, validPropertyName);
        assertThat(request.on(), is(sameInstance(validPathLocation1)));
        assertThat(request.hasError(), is(false));
        assertThat(request.getError(), is(nullValue()));
    }

    @Test
    public void shouldConsiderEqualTwoRequestsWithSameLocationsAndSamePropertyName() {
        request = new ReadPropertyRequest(validPathLocation1, validPropertyName);
        ReadPropertyRequest request2 = new ReadPropertyRequest(validPathLocation1, validPropertyName);
        assertThat(request, is(request2));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithDifferentLocations() {
        request = new ReadPropertyRequest(validPathLocation1, validPropertyName);
        ReadPropertyRequest request2 = new ReadPropertyRequest(validPathLocation2, validPropertyName);
        assertThat(request.equals(request2), is(false));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithSameLocationButDifferentPropertyNames() {
        request = new ReadPropertyRequest(validPathLocation1, validProperty2.getName());
        ReadPropertyRequest request2 = new ReadPropertyRequest(validPathLocation2, validPropertyName);
        assertThat(request.equals(request2), is(false));
    }

    @Test
    public void shouldAllowSettingProperties() {
        request = new ReadPropertyRequest(validPathLocation, validPropertyName);
        request.setProperty(validProperty1);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowSettingPropertyIfNameDoeNotMatch() {
        request = new ReadPropertyRequest(validPathLocation, validPropertyName);
        request.setProperty(validProperty2);
    }
}
