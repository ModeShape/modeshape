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
package org.modeshape.common.util;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.CommonI18n;
import org.modeshape.common.collection.Problem;
import org.modeshape.common.collection.Problem.Status;
import org.modeshape.common.i18n.I18n;

public class ObjectPropertyTest {

    private ObjectProperty property;

    @Before
    public void beforeEach() {

    }

    @Test
    public void shouldGetAllObjectPropertiesOnJavaBean() throws Exception {
        Status status = Status.INFO;
        int code = 121;
        I18n msg = CommonI18n.argumentMayNotBeEmpty;
        Object[] params = new Object[] {"argName"};
        String resource = "The source";
        String location = "The place to be";
        Throwable throwable = null;
        Problem problem = new Problem(status, code, msg, params, resource, location, throwable);
        List<ObjectProperty> props = ObjectProperty.getAll(problem);
        Map<String, ObjectProperty> propsByName = ObjectProperty.getAllByName(problem);

        assertThat(props.size(), is(8));
        assertThat(propsByName.size(), is(8));
        assertThat(props.containsAll(propsByName.values()), is(true));

        property = propsByName.remove("Status");
        assertThat(property.getName(), is("Status"));
        assertThat(property.getLabel(), is("Status"));
        assertThat(property.getType().equals(Status.class), is(true));
        assertThat(property.getValue(), is((Object)status));
        assertThat(property.isReadOnly(), is(true));
        assertThat(property, is(findProperty(property.getName(), props)));

        property = propsByName.remove("Code");
        assertThat(property.getName(), is("Code"));
        assertThat(property.getLabel(), is("Code"));
        assertThat(property.getType().equals(Integer.TYPE), is(true));
        assertThat(property.getValue(), is((Object)code));
        assertThat(property.isReadOnly(), is(true));

        property = propsByName.remove("Message");
        assertThat(property.getName(), is("Message"));
        assertThat(property.getLabel(), is("Message"));
        assertThat(property.getType().equals(I18n.class), is(true));
        assertThat(property.getValue(), is((Object)msg));
        assertThat(property.isReadOnly(), is(true));

        property = propsByName.remove("MessageString");
        assertThat(property.getName(), is("MessageString"));
        assertThat(property.getLabel(), is("Message string"));
        assertThat(property.getType().equals(String.class), is(true));
        assertThat(property.getValue(), is((Object)msg.text(params)));
        assertThat(property.isReadOnly(), is(true));

        property = propsByName.remove("Parameters");
        assertThat(property.getName(), is("Parameters"));
        assertThat(property.getLabel(), is("Parameters"));
        assertThat(property.getType().equals(Object[].class), is(true));
        assertThat(property.getValue(), is((Object)params));
        assertThat(property.isReadOnly(), is(true));

        property = propsByName.remove("Resource");
        assertThat(property.getName(), is("Resource"));
        assertThat(property.getLabel(), is("Resource"));
        assertThat(property.getType().equals(String.class), is(true));
        assertThat(property.getValue(), is((Object)resource));
        assertThat(property.isReadOnly(), is(true));

        property = propsByName.remove("Location");
        assertThat(property.getName(), is("Location"));
        assertThat(property.getLabel(), is("Location"));
        assertThat(property.getType().equals(String.class), is(true));
        assertThat(property.getValue(), is((Object)location));
        assertThat(property.isReadOnly(), is(true));

        property = propsByName.remove("Throwable");
        assertThat(property.getName(), is("Throwable"));
        assertThat(property.getLabel(), is("Throwable"));
        assertThat(property.getType().equals(Throwable.class), is(true));
        assertThat(property.getValue(), is((Object)throwable));
        assertThat(property.isReadOnly(), is(true));

        assertThat(propsByName.isEmpty(), is(true));
    }

    protected ObjectProperty findProperty( String propertyName,
                                           List<ObjectProperty> properties ) {
        for (ObjectProperty prop : properties) {
            if (prop.getName().equals(propertyName)) return prop;
        }
        return null;
    }
}
