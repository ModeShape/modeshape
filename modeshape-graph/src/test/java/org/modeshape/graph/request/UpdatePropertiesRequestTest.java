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
import static org.junit.matchers.JUnitMatchers.hasItems;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Property;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class UpdatePropertiesRequestTest extends AbstractRequestTest {

    private UpdatePropertiesRequest request;
    private Map<Name, Property> validProperties;

    @Override
    @Before
    public void beforeEach() {
        super.beforeEach();
        validProperties = new HashMap<Name, Property>();
        validProperties.put(validProperty1.getName(), validProperty1);
        validProperties.put(validProperty2.getName(), validProperty2);
    }

    @Override
    protected Request createRequest() {
        return new UpdatePropertiesRequest(validPathLocation1, workspace1, validProperties);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNullFromLocation() {
        new UpdatePropertiesRequest(null, workspace1, validProperties);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNullWorkspaceName() {
        new UpdatePropertiesRequest(validPathLocation1, null, validProperties);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithNullPropertyName() {
        new UpdatePropertiesRequest(validPathLocation, workspace1, null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingRequestWithEmptyPropertyMap() {
        new UpdatePropertiesRequest(validPathLocation, workspace1, Collections.<Name, Property>emptyMap());
    }

    @Test
    public void shouldCreateValidRequestWithValidLocationAndValidProperty() {
        request = new UpdatePropertiesRequest(validPathLocation1, workspace1, validProperties);
        assertThat(request.on(), is(sameInstance(validPathLocation1)));
        assertThat(request.inWorkspace(), is(sameInstance(workspace1)));
        assertThat(request.hasError(), is(false));
        assertThat(request.getError(), is(nullValue()));
        assertThat(request.properties().values(), hasItems(validProperty1, validProperty2));
    }

    @Test
    public void shouldConsiderEqualTwoRequestsWithSameLocationsAndSameProperties() {
        request = new UpdatePropertiesRequest(validPathLocation1, new String(workspace1), validProperties);
        UpdatePropertiesRequest request2 = new UpdatePropertiesRequest(validPathLocation1, workspace1, validProperties);
        assertThat(request, is(request2));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithDifferentLocations() {
        request = new UpdatePropertiesRequest(validPathLocation1, workspace1, validProperties);
        UpdatePropertiesRequest request2 = new UpdatePropertiesRequest(validPathLocation2, workspace1, validProperties);
        assertThat(request.equals(request2), is(false));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithDifferentWorkspaceNames() {
        request = new UpdatePropertiesRequest(validPathLocation1, workspace1, validProperties);
        UpdatePropertiesRequest request2 = new UpdatePropertiesRequest(validPathLocation1, workspace2, validProperties);
        assertThat(request.equals(request2), is(false));
    }

    @Test
    public void shouldConsiderNotEqualTwoRequestsWithSameLocationButDifferentProperties() {
        request = new UpdatePropertiesRequest(validPathLocation1, workspace1, validProperties);
        Map<Name, Property> otherValidProperties = Collections.singletonMap(validProperty1.getName(), validProperty1);
        UpdatePropertiesRequest request2 = new UpdatePropertiesRequest(validPathLocation2, workspace1, otherValidProperties);
        assertThat(request.equals(request2), is(false));
    }

}
