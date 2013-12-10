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
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import javax.jcr.Binary;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import org.hamcrest.collection.IsIn;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.value.DateTimeFactory;

/**
 * @author jverhaeg
 */
public class JcrMultiValuePropertyTest extends MultiUseAbstractTest {

    private org.modeshape.jcr.api.Property prop;
    private byte[][] binaryValue;
    private DateTime[] dateValue;
    private double[] doubleValue;
    private long[] longValue;
    private String[] stringValue;
    private boolean[] booleanValue;
    private String[] nameValue;
    private String[] pathValue;
    private DateTimeFactory dateFactory;
    protected AbstractJcrNode cars;
    protected AbstractJcrNode altima;
    protected AbstractJcrNode aston;

    /**
     * Initialize the expensive activities, and in particular the RepositoryNodeTypeManager instance.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void beforeAll() throws Exception {
        MultiUseAbstractTest.beforeAll();

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
        binaryDefn.setMultiple(true);
        propDefns.add(binaryDefn);

        PropertyDefinitionTemplate booleanDefn = mgr.createPropertyDefinitionTemplate();
        booleanDefn.setName("booleanProperty");
        booleanDefn.setRequiredType(PropertyType.BOOLEAN);
        booleanDefn.setMultiple(true);
        propDefns.add(booleanDefn);

        PropertyDefinitionTemplate dateDefn = mgr.createPropertyDefinitionTemplate();
        dateDefn.setName("dateProperty");
        dateDefn.setRequiredType(PropertyType.DATE);
        dateDefn.setMultiple(true);
        propDefns.add(dateDefn);

        PropertyDefinitionTemplate doubleDefn = mgr.createPropertyDefinitionTemplate();
        doubleDefn.setName("doubleProperty");
        doubleDefn.setRequiredType(PropertyType.DOUBLE);
        doubleDefn.setMultiple(true);
        propDefns.add(doubleDefn);

        PropertyDefinitionTemplate longDefn = mgr.createPropertyDefinitionTemplate();
        longDefn.setName("longProperty");
        longDefn.setRequiredType(PropertyType.LONG);
        longDefn.setMultiple(true);
        propDefns.add(longDefn);

        PropertyDefinitionTemplate nameDefn = mgr.createPropertyDefinitionTemplate();
        nameDefn.setName("nameProperty");
        nameDefn.setRequiredType(PropertyType.NAME);
        nameDefn.setMultiple(true);
        propDefns.add(nameDefn);

        PropertyDefinitionTemplate pathDefn = mgr.createPropertyDefinitionTemplate();
        pathDefn.setName("pathProperty");
        pathDefn.setRequiredType(PropertyType.PATH);
        pathDefn.setMultiple(true);
        propDefns.add(pathDefn);

        PropertyDefinitionTemplate refDefn = mgr.createPropertyDefinitionTemplate();
        refDefn.setName("referenceProperty");
        refDefn.setRequiredType(PropertyType.REFERENCE);
        refDefn.setMultiple(true);
        propDefns.add(refDefn);

        PropertyDefinitionTemplate ref2Defn = mgr.createPropertyDefinitionTemplate();
        ref2Defn.setName("referenceProperty2");
        ref2Defn.setRequiredType(PropertyType.REFERENCE);
        ref2Defn.setMultiple(true);
        propDefns.add(ref2Defn);

        PropertyDefinitionTemplate stringDefn = mgr.createPropertyDefinitionTemplate();
        stringDefn.setName("stringProperty");
        stringDefn.setRequiredType(PropertyType.STRING);
        stringDefn.setMultiple(true);
        propDefns.add(stringDefn);

        PropertyDefinitionTemplate undefinedDefn = mgr.createPropertyDefinitionTemplate();
        undefinedDefn.setName("undefinedProperty");
        undefinedDefn.setRequiredType(PropertyType.UNDEFINED);
        undefinedDefn.setMultiple(true);
        propDefns.add(undefinedDefn);

        // Add the node type ...
        mgr.registerNodeType(nodeType, true);

        // Import the node types and the data ...
        registerNodeTypes("cars.cnd");
        importContent("/", "io/cars-system-view-with-uuids.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
    }

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();
        session.getWorkspace().getNamespaceRegistry().registerNamespace("acme", "http://example.com");
        dateFactory = session.dateFactory();

        binaryValue = new byte[][] {"This is a binary value1".getBytes(), "This is a binary value2".getBytes()};
        dateValue = new DateTime[] {dateFactory.create(), dateFactory.create().plusDays(1)};
        doubleValue = new double[] {3.14159d, 1.0d};
        longValue = new long[] {100L, 101L};
        booleanValue = new boolean[] {true, false};
        stringValue = new String[] {"stringValue1", "string value 2"};
        nameValue = new String[] {"acme:SomeName1", "acme:SomeName2"};
        pathValue = new String[] {"/Cars/Hybrid/Toyota Highlander/acme:SomethingElse", "/Cars/acme:Wow"};

        // Add the mixin to the 'Cars' node ...
        cars = session.getNode("/Cars");
        cars.addMixin("mixinWithAllPropTypes");

        altima = session.getNode("/Cars/Hybrid/Nissan Altima");
        altima.addMixin("mix:referenceable");

        aston = session.getNode("/Cars/Sports/Aston Martin DB9");
        aston.addMixin("mix:referenceable");

        // Set each property ...
        cars.setProperty("booleanProperty", values(booleanValue));
        cars.setProperty("dateProperty", values(dateValue));
        cars.setProperty("doubleProperty", values(doubleValue));
        cars.setProperty("binaryProperty", values(binaryValue));
        cars.setProperty("longProperty", values(longValue));
        cars.setProperty("referenceProperty", values(new Node[] {altima, aston}));
        cars.setProperty("referenceProperty2",
                         values(PropertyType.STRING, new String[] {altima.getIdentifier(), aston.getIdentifier()}));
        cars.setProperty("stringProperty", values(PropertyType.STRING, stringValue));
        cars.setProperty("pathProperty", values(PropertyType.PATH, pathValue));
        cars.setProperty("nameProperty", values(PropertyType.NAME, nameValue));
        cars.setProperty("undefinedProperty", values(PropertyType.STRING, new String[] {"100", "200"}));
    }

    protected Value[] values( boolean[] values ) throws Exception {
        Value[] result = new Value[values.length];
        for (int i = 0; i != values.length; ++i) {
            result[i] = session.getValueFactory().createValue(values[i]);
        }
        return result;
    }

    protected Value[] values( long[] values ) throws Exception {
        Value[] result = new Value[values.length];
        for (int i = 0; i != values.length; ++i) {
            result[i] = session.getValueFactory().createValue(values[i]);
        }
        return result;
    }

    protected Value[] values( double[] values ) throws Exception {
        Value[] result = new Value[values.length];
        for (int i = 0; i != values.length; ++i) {
            result[i] = session.getValueFactory().createValue(values[i]);
        }
        return result;
    }

    protected Value[] values( byte[][] values ) throws Exception {
        Value[] result = new Value[values.length];
        for (int i = 0; i != values.length; ++i) {
            Binary binary = session.getValueFactory().createBinary(values[i]);
            result[i] = session.getValueFactory().createValue(binary);
        }
        return result;
    }

    protected Value[] values( DateTime[] values ) throws Exception {
        Value[] result = new Value[values.length];
        for (int i = 0; i != values.length; ++i) {
            result[i] = session.getValueFactory().createValue(values[i].toCalendar());
        }
        return result;
    }

    protected Value[] values( Node[] values ) throws Exception {
        Value[] result = new Value[values.length];
        for (int i = 0; i != values.length; ++i) {
            result[i] = session.getValueFactory().createValue(values[i]);
        }
        return result;
    }

    protected Value[] values( int type,
                              String[] values ) throws Exception {
        Value[] result = new Value[values.length];
        for (int i = 0; i != values.length; ++i) {
            result[i] = session.getValueFactory().createValue(values[i]);
        }
        return result;
    }

    @Test
    public void shouldIndicateHasMultipleValues() throws Exception {
        prop = cars.getProperty("booleanProperty");
        PropertyDefinition def = prop.getDefinition();
        assertThat(def.isMultiple(), is(true));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideBooleanForMultiValuedProperty() throws Exception {
        prop = cars.getProperty("booleanProperty");
        prop.getBoolean();
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideBooleanConversionForMultiValuedProperty() throws Exception {
        prop = cars.getProperty("booleanProperty");
        prop.getAs(Boolean.class);
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideDateForMultiValuedProperty() throws Exception {
        prop = cars.getProperty("dateProperty");
        prop.getDate();
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideDoubleForMultiValuedProperty() throws Exception {
        prop = cars.getProperty("doubleProperty");
        prop.getDouble();
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideLongForMultiValuedProperty() throws Exception {
        prop = cars.getProperty("longProperty");
        prop.getLong();
    }

    @SuppressWarnings( "deprecation" )
    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideStreamForMultiValuedProperty() throws Exception {
        prop = cars.getProperty("binaryProperty");
        prop.getStream();
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideStringForMultiValuedProperty() throws Exception {
        prop = cars.getProperty("stringProperty");
        prop.getString();
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideValue() throws Exception {
        prop = cars.getProperty("stringProperty");
        prop.getValue();
    }

    @Test
    public void shouldProvideBooleanValues() throws Exception {
        prop = cars.getProperty("booleanProperty");
        Value[] vals = prop.getValues();
        assertThat(vals, notNullValue());
        assertThat(vals.length, is(2));
        assertThat(vals[0].getBoolean(), is(true));
        assertThat(vals[1].getBoolean(), is(false));
        assertThat(prop.getAs(Boolean[].class), is(new Boolean[]{true, false}));
        assertThat(prop.getAs(Boolean.class, 0), is(true));
        assertThat(prop.getAs(Boolean.class, 1), is(false));
    }

    @Test
    public void shouldProvideBinaryValues() throws Exception {
        prop = cars.getProperty("binaryProperty");
        Value[] vals = prop.getValues();
        assertThat(vals, notNullValue());
        assertThat(vals.length, is(2));
        TreeSet<String> expectedBinaryValues = new TreeSet<String>(Arrays.asList("This is a binary value1", "This is a binary value2"));
        assertThat(expectedBinaryValues.contains(vals[0].getString()), is(true));
        assertThat(expectedBinaryValues.contains(vals[1].getString()), is(true));
        assertThat(expectedBinaryValues.contains(prop.getAs(String.class, 0)), is(true));
        assertThat(expectedBinaryValues.contains(prop.getAs(String.class, 1)), is(true));
        assertThat(prop.getAs(org.modeshape.jcr.api.Binary[].class).length, is(2));
        assertThat(prop.getAs(Binary[].class).length, is(2));
        assertThat(prop.getAs(InputStream[].class).length, is(2));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideLength() throws Exception {
        prop = cars.getProperty("stringProperty");
        prop.getLength();
    }

    @FixFor( "MODE-1720" )
    @Test
    public void shouldPropertyDereferenceReferenceProperties() throws Exception {
        prop = cars.getProperty("referenceProperty");
        Value[] vals = prop.getValues();
        for (Value value : vals) {
            String str = value.getString();
            Node node = cars.getSession().getNodeByIdentifier(str);
            assertThat(node, IsIn.isOneOf((Node)altima, (Node)aston));
        }

        Node[] nodes = prop.getAs(Node[].class);
        for (Node node : nodes) {
            assertThat(node, IsIn.isOneOf((Node)altima, (Node)aston));
        }
        NodeIterator nodeIterator = prop.getAs(NodeIterator.class);
        while (nodeIterator.hasNext()) {
            assertThat(nodeIterator.nextNode(), IsIn.isOneOf((Node)altima, (Node)aston));
        }
        assertThat(prop.getAs(Node.class, 0), IsIn.isOneOf((Node)altima, (Node)aston));
        assertThat(prop.getAs(Node.class, 1), IsIn.isOneOf((Node)altima, (Node)aston));
    }

    @FixFor( "MODE-1720" )
    @Test
    public void shouldPropertyDereferenceReferencePropertiesCreatedFromStringValues() throws Exception {
        prop = cars.getProperty("referenceProperty2");
        Value[] vals = prop.getValues();
        for (Value value : vals) {
            String str = value.getString();
            Node node = cars.getSession().getNodeByIdentifier(str);
            assertThat(node, IsIn.isOneOf((Node)altima, (Node)aston));
        }
    }
}
