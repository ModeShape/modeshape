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
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import javax.jcr.Binary;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.value.ValueFactory;

public class JcrSingleValuePropertyTest extends MultiUseAbstractTest {

    private Property prop;
    private byte[] binaryValue;
    private DateTime dateValue;
    private double doubleValue;
    private long longValue;
    private String stringValue;
    private boolean booleanValue;
    private String nameValue;
    private String pathValue;
    private ValueFactory<String> stringFactory;
    protected AbstractJcrNode cars;
    protected AbstractJcrNode altima;

    /**
     * Initialize the expensive activities, and in particular the RepositoryNodeTypeManager instance.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void beforeAll() throws Exception {
        MultiUseAbstractTest.beforeAll();

        // Import the node types and the data ...
        registerNodeTypes("cars.cnd");
        importContent("/", "io/cars-system-view-with-uuids.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

        // Now define a namespace we'll use in the tests ...
        session.getWorkspace().getNamespaceRegistry().registerNamespace("acme", "http://example.com");

        NodeTypeManager mgr = session.getWorkspace().getNodeTypeManager();
        // Define the node definition that will have all the different kinds of properties ...
        NodeTypeTemplate nodeType = mgr.createNodeTypeTemplate();
        nodeType.setMixin(true);
        nodeType.setName("mixinWithAllPropTypes");
        @SuppressWarnings( "unchecked" )
        List<PropertyDefinitionTemplate> propDefns = nodeType.getPropertyDefinitionTemplates();

        // Add a property for each type ...
        PropertyDefinitionTemplate binaryDefn = mgr.createPropertyDefinitionTemplate();
        binaryDefn.setName("binaryProperty");
        binaryDefn.setRequiredType(PropertyType.BINARY);
        propDefns.add(binaryDefn);

        PropertyDefinitionTemplate booleanDefn = mgr.createPropertyDefinitionTemplate();
        booleanDefn.setName("booleanProperty");
        booleanDefn.setRequiredType(PropertyType.BOOLEAN);
        propDefns.add(booleanDefn);

        PropertyDefinitionTemplate dateDefn = mgr.createPropertyDefinitionTemplate();
        dateDefn.setName("dateProperty");
        dateDefn.setRequiredType(PropertyType.DATE);
        propDefns.add(dateDefn);

        PropertyDefinitionTemplate doubleDefn = mgr.createPropertyDefinitionTemplate();
        doubleDefn.setName("doubleProperty");
        doubleDefn.setRequiredType(PropertyType.DOUBLE);
        propDefns.add(doubleDefn);

        PropertyDefinitionTemplate longDefn = mgr.createPropertyDefinitionTemplate();
        longDefn.setName("longProperty");
        longDefn.setRequiredType(PropertyType.LONG);
        propDefns.add(longDefn);

        PropertyDefinitionTemplate nameDefn = mgr.createPropertyDefinitionTemplate();
        nameDefn.setName("nameProperty");
        nameDefn.setRequiredType(PropertyType.NAME);
        propDefns.add(nameDefn);

        PropertyDefinitionTemplate pathDefn = mgr.createPropertyDefinitionTemplate();
        pathDefn.setName("pathProperty");
        pathDefn.setRequiredType(PropertyType.PATH);
        propDefns.add(pathDefn);

        PropertyDefinitionTemplate refDefn = mgr.createPropertyDefinitionTemplate();
        refDefn.setName("referenceProperty");
        refDefn.setRequiredType(PropertyType.REFERENCE);
        propDefns.add(refDefn);

        PropertyDefinitionTemplate stringDefn = mgr.createPropertyDefinitionTemplate();
        stringDefn.setName("stringProperty");
        stringDefn.setRequiredType(PropertyType.STRING);
        propDefns.add(stringDefn);

        PropertyDefinitionTemplate undefinedDefn = mgr.createPropertyDefinitionTemplate();
        undefinedDefn.setName("undefinedProperty");
        undefinedDefn.setRequiredType(PropertyType.UNDEFINED);
        propDefns.add(undefinedDefn);

        // Add the node type ...
        mgr.registerNodeType(nodeType, true);
    }

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();
        stringFactory = session.stringFactory();

        binaryValue = "This is a binary value".getBytes();
        dateValue = session.dateFactory().create();
        doubleValue = 3.14159d;
        longValue = 100L;
        booleanValue = true;
        stringValue = "stringValue";
        nameValue = "acme:SomeName";
        pathValue = "/Cars/Hybrid/Toyota Highlander/acme:SomethingElse";

        // Add the mixin to the 'Cars' node ...
        cars = session.getNode("/Cars");
        cars.addMixin("mixinWithAllPropTypes");

        altima = session.getNode("/Cars/Hybrid/Nissan Altima");
        altima.addMixin("mix:referenceable");

        // Set each property ...
        cars.setProperty("booleanProperty", booleanValue);
        cars.setProperty("dateProperty", dateValue.toCalendar());
        cars.setProperty("doubleProperty", doubleValue);
        cars.setProperty("binaryProperty", new ByteArrayInputStream(binaryValue));
        cars.setProperty("longProperty", longValue);
        cars.setProperty("referenceProperty", altima);
        cars.setProperty("stringProperty", stringValue);
        cars.setProperty("pathProperty", pathValue);
        cars.setProperty("nameProperty", nameValue);
        cars.setProperty("undefinedProperty", "100");
    }

    @Test
    public void shouldIndicateHasSingleValue() throws Exception {
        prop = cars.getProperty("booleanProperty");
        PropertyDefinition def = prop.getDefinition();
        assertThat(def.isMultiple(), is(false));
    }

    @Test
    public void shouldProvideBoolean() throws Exception {
        prop = cars.getProperty("booleanProperty");
        assertThat(prop.getBoolean(), is(booleanValue));
        assertThat(prop.getType(), is(PropertyType.BOOLEAN));
        assertThat(prop.getString(), is(stringFactory.create(booleanValue)));
        assertThat(prop.getLength(), is((long)stringFactory.create(booleanValue).length()));
        checkValue(prop);
    }

    @Test
    public void shouldProvideDate() throws Exception {
        prop = cars.getProperty("dateProperty");
        // see ModeShape-527 for reasons asserts are commented
        // assertThat(prop.getDate(), is(dateValue.toCalendar()));
        assertThat(prop.getLong(), is(dateValue.getMilliseconds()));
        // assertThat(prop.getString(), is(stringFactory.create(dateValue)));
        assertThat(prop.getType(), is(PropertyType.DATE));
        // assertThat(prop.getLength(), is((long)stringFactory.create(dateValue).length()));
        checkValue(prop);
    }

    @Test
    public void shouldProvideNode() throws Exception {
        prop = cars.getProperty("referenceProperty");
        assertThat(prop.getNode(), is((Node)altima));
        assertThat(prop.getType(), is(PropertyType.REFERENCE));
        assertThat(prop.getString(), is(altima.key().toString()));
        assertThat(prop.getLength(), is((long)altima.key().toString().length()));
        checkValue(prop);
    }

    @Test
    public void shouldProvideDouble() throws Exception {
        prop = cars.getProperty("doubleProperty");
        assertThat(prop.getDouble(), is(doubleValue));
        assertThat(prop.getString(), is(stringFactory.create(doubleValue)));
        assertThat(prop.getType(), is(PropertyType.DOUBLE));
        assertThat(prop.getLength(), is((long)stringFactory.create(doubleValue).length()));
        checkValue(prop);
    }

    @Test
    public void shouldProvideLong() throws Exception {
        prop = cars.getProperty("longProperty");
        assertThat(prop.getLong(), is(longValue));
        assertThat(prop.getString(), is(stringFactory.create(longValue)));
        assertThat(prop.getType(), is(PropertyType.LONG));
        assertThat(prop.getLength(), is((long)stringFactory.create(longValue).length()));
        checkValue(prop);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldProvideStream() throws Exception {
        prop = cars.getProperty("binaryProperty");
        assertThat(prop.getType(), is(PropertyType.BINARY));
        InputStream stream = prop.getStream();
        try {
            assertThat(stream, notNullValue());
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
        assertThat(prop.getString(), is(stringFactory.create(binaryValue)));
        assertThat(prop.getLength(), is((long)binaryValue.length)); // note this value!!
        checkValue(prop);
    }

    @Test
    public void shouldProvideBinary() throws Exception {
        prop = cars.getProperty("binaryProperty");
        assertThat(prop.getType(), is(PropertyType.BINARY));
        Binary binary = prop.getBinary();
        InputStream stream = binary.getStream();
        try {
            assertThat(stream, notNullValue());
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
        assertThat(prop.getString(), is(stringFactory.create(binaryValue)));
        assertThat(prop.getLength(), is((long)binaryValue.length)); // note this value!!
        assertThat(binary.getSize(), is((long)binaryValue.length)); // note this value!!
        checkValue(prop);
    }

    @Test
    public void shouldProvideString() throws Exception {
        prop = cars.getProperty("stringProperty");
        assertThat(prop.getString(), is(stringValue));
        assertThat(prop.getType(), is(PropertyType.STRING));
        assertThat(prop.getLength(), is((long)stringValue.length()));
        checkValue(prop);
    }

    @Test
    public void shouldAllowNameValue() throws Exception {
        prop = cars.getProperty("nameProperty");
        assertThat(prop.getType(), is(PropertyType.NAME));
        assertThat(prop.getString(), is(nameValue));
        assertThat(prop.getLength(), is((long)nameValue.length()));
        // Change the namespace registry ...
        session.setNamespacePrefix("acme2", "http://example.com");
        assertThat(prop.getString(), is("acme2:SomeName"));
        checkValue(prop);
    }

    @Test
    public void shouldAllowPathValue() throws Exception {
        prop = cars.getProperty("pathProperty");
        assertThat(prop.getType(), is(PropertyType.PATH));
        assertThat(prop.getString(), is(pathValue));
        // Change the namespace registry ...
        session.setNamespacePrefix("acme2", "http://example.com");
        assertThat(prop.getString(), is("/Cars/Hybrid/Toyota Highlander/acme2:SomethingElse"));
        checkValue(prop);
    }

    public void checkValue( Property prop ) throws Exception {
        // Should provide a value and not multiple values ...
        Value val = prop.getValue();
        assertThat(val, notNullValue());
        assertThat(prop.getDefinition(), notNullValue());

        // Should not allow multiple-value methods ...
        try {
            prop.getValues();
            fail("Should not be able to call 'getValues()' on a single-value property");
        } catch (ValueFormatException e) {
            // expected
        }
        try {
            prop.getLengths();
            fail("Should not be able to call 'getValues()' on a single-value property");
        } catch (ValueFormatException e) {
            // expected
        }
    }

}
