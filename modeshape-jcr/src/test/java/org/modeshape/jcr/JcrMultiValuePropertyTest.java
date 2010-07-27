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
import java.util.List;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.PropertyDefinition;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.DateTimeFactory;
import org.modeshape.jcr.nodetype.PropertyDefinitionTemplate;

/**
 * @author jverhaeg
 */
public class JcrMultiValuePropertyTest extends AbstractJcrTest {

    private Property prop;
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

    /**
     * Initialize the expensive activities, and in particular the RepositoryNodeTypeManager instance.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void beforeAll() throws Exception {
        AbstractJcrTest.beforeAll();

        // Define the node definition that will have all the different kinds of properties ...
        JcrNodeTypeTemplate nodeType = new JcrNodeTypeTemplate(context);
        nodeType.setMixin(true);
        nodeType.setName("mixinWithAllPropTypes");
        List<PropertyDefinitionTemplate> propDefns = nodeType.getPropertyDefinitionTemplates();

        // Add a property for each type ...
        JcrPropertyDefinitionTemplate binaryDefn = new JcrPropertyDefinitionTemplate(context);
        binaryDefn.setName("binaryProperty");
        binaryDefn.setRequiredType(PropertyType.BINARY);
        binaryDefn.setMultiple(true);
        propDefns.add(binaryDefn);

        JcrPropertyDefinitionTemplate booleanDefn = new JcrPropertyDefinitionTemplate(context);
        booleanDefn.setName("booleanProperty");
        booleanDefn.setRequiredType(PropertyType.BOOLEAN);
        booleanDefn.setMultiple(true);
        propDefns.add(booleanDefn);

        JcrPropertyDefinitionTemplate dateDefn = new JcrPropertyDefinitionTemplate(context);
        dateDefn.setName("dateProperty");
        dateDefn.setRequiredType(PropertyType.DATE);
        dateDefn.setMultiple(true);
        propDefns.add(dateDefn);

        JcrPropertyDefinitionTemplate doubleDefn = new JcrPropertyDefinitionTemplate(context);
        doubleDefn.setName("doubleProperty");
        doubleDefn.setRequiredType(PropertyType.DOUBLE);
        doubleDefn.setMultiple(true);
        propDefns.add(doubleDefn);

        JcrPropertyDefinitionTemplate longDefn = new JcrPropertyDefinitionTemplate(context);
        longDefn.setName("longProperty");
        longDefn.setRequiredType(PropertyType.LONG);
        longDefn.setMultiple(true);
        propDefns.add(longDefn);

        JcrPropertyDefinitionTemplate nameDefn = new JcrPropertyDefinitionTemplate(context);
        nameDefn.setName("nameProperty");
        nameDefn.setRequiredType(PropertyType.NAME);
        nameDefn.setMultiple(true);
        propDefns.add(nameDefn);

        JcrPropertyDefinitionTemplate pathDefn = new JcrPropertyDefinitionTemplate(context);
        pathDefn.setName("pathProperty");
        pathDefn.setRequiredType(PropertyType.PATH);
        pathDefn.setMultiple(true);
        propDefns.add(pathDefn);

        JcrPropertyDefinitionTemplate refDefn = new JcrPropertyDefinitionTemplate(context);
        refDefn.setName("referenceProperty");
        refDefn.setRequiredType(PropertyType.REFERENCE);
        refDefn.setMultiple(true);
        propDefns.add(refDefn);

        JcrPropertyDefinitionTemplate stringDefn = new JcrPropertyDefinitionTemplate(context);
        stringDefn.setName("stringProperty");
        stringDefn.setRequiredType(PropertyType.STRING);
        stringDefn.setMultiple(true);
        propDefns.add(stringDefn);

        JcrPropertyDefinitionTemplate undefinedDefn = new JcrPropertyDefinitionTemplate(context);
        undefinedDefn.setName("undefinedProperty");
        undefinedDefn.setRequiredType(PropertyType.UNDEFINED);
        undefinedDefn.setMultiple(true);
        propDefns.add(undefinedDefn);

        // Add the node type ...
        rntm.registerNodeType(nodeType);
    }

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();
        context.getNamespaceRegistry().register("acme", "http://example.com");
        dateFactory = context.getValueFactories().getDateFactory();

        binaryValue = new byte[][] {"This is a binary value1".getBytes(), "This is a binary value2".getBytes()};
        dateValue = new DateTime[] {dateFactory.create(), dateFactory.create().plusDays(1)};
        doubleValue = new double[] {3.14159d, 1.0d};
        longValue = new long[] {100L, 101L};
        booleanValue = new boolean[] {true, false};
        stringValue = new String[] {"stringValue1", "string value 2"};
        nameValue = new String[] {"acme:SomeName1", "acme:SomeName2"};
        pathValue = new String[] {"/Cars/Hybrid/Toyota Highlander/acme:SomethingElse", "/Cars/acme:Wow"};

        // Add the mixin to the 'Cars' node ...
        cars = cache.findJcrNode(null, path("/Cars"));
        cars.addMixin("mixinWithAllPropTypes");

        altima = cache.findJcrNode(null, path("/Cars/Hybrid/Nissan Altima"));
        altima.addMixin("mix:referenceable");

        // Set each property ...
        cars.setProperty("booleanProperty", values(booleanValue));
        cars.setProperty("dateProperty", values(dateValue));
        cars.setProperty("doubleProperty", values(doubleValue));
        cars.setProperty("binaryProperty", values(binaryValue));
        cars.setProperty("longProperty", values(longValue));
        cars.setProperty("referenceProperty", values(new Node[] {altima}));
        cars.setProperty("stringProperty", values(PropertyType.STRING, stringValue));
        cars.setProperty("pathProperty", values(PropertyType.PATH, pathValue));
        cars.setProperty("nameProperty", values(PropertyType.NAME, nameValue));
        cars.setProperty("undefinedProperty", values(PropertyType.STRING, new String[] {"100", "200"}));
    }

    protected Value[] values( boolean[] values ) throws Exception {
        Value[] result = new Value[values.length];
        for (int i = 0; i != values.length; ++i) {
            result[i] = new JcrValue(context.getValueFactories(), cache, PropertyType.BOOLEAN, values[i]);
        }
        return result;
    }

    protected Value[] values( long[] values ) throws Exception {
        Value[] result = new Value[values.length];
        for (int i = 0; i != values.length; ++i) {
            result[i] = new JcrValue(context.getValueFactories(), cache, PropertyType.LONG, values[i]);
        }
        return result;
    }

    protected Value[] values( double[] values ) throws Exception {
        Value[] result = new Value[values.length];
        for (int i = 0; i != values.length; ++i) {
            result[i] = new JcrValue(context.getValueFactories(), cache, PropertyType.DOUBLE, values[i]);
        }
        return result;
    }

    protected Value[] values( byte[][] values ) throws Exception {
        Value[] result = new Value[values.length];
        for (int i = 0; i != values.length; ++i) {
            result[i] = new JcrValue(context.getValueFactories(), cache, PropertyType.BINARY, values[i]);
        }
        return result;
    }

    protected Value[] values( DateTime[] values ) throws Exception {
        Value[] result = new Value[values.length];
        for (int i = 0; i != values.length; ++i) {
            result[i] = new JcrValue(context.getValueFactories(), cache, PropertyType.DATE, values[i].toCalendar());
        }
        return result;
    }

    protected Value[] values( Node[] values ) throws Exception {
        Value[] result = new Value[values.length];
        for (int i = 0; i != values.length; ++i) {
            result[i] = new JcrValue(context.getValueFactories(), cache, PropertyType.REFERENCE, values[i]);
        }
        return result;
    }

    protected Value[] values( int type,
                              String[] values ) throws Exception {
        Value[] result = new Value[values.length];
        for (int i = 0; i != values.length; ++i) {
            result[i] = new JcrValue(context.getValueFactories(), cache, type, values[i]);
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
    public void shouldProvideValues() throws Exception {
        prop = cars.getProperty("booleanProperty");
        Value[] vals = prop.getValues();
        assertThat(vals, notNullValue());
        assertThat(vals.length, is(2));
        assertThat(vals[0].getBoolean(), is(true));
        assertThat(vals[1].getBoolean(), is(false));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideLength() throws Exception {
        prop = cars.getProperty("stringProperty");
        prop.getLength();
    }

    // @Test
    // public void shouldProvideLengths() throws Exception {
    // long[] lengths = prop.getLengths();
    // assertThat(lengths, notNullValue());
    // assertThat(lengths.length, is(1));
    // assertThat(lengths[0], is(4L));
    //
    // Object value = "value";
    // dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, value);
    // when(propertyInfo.getProperty()).thenReturn(dnaProperty);
    // when(definition.getRequiredType()).thenReturn(PropertyType.STRING);
    // when(definition.isMultiple()).thenReturn(true);
    // prop = new JcrMultiValueProperty(cache, propertyId);
    // lengths = prop.getLengths();
    // assertThat(lengths, notNullValue());
    // assertThat(lengths.length, is(1));
    // assertThat(lengths[0], is(5L));
    //
    // value = new Object();
    // long expectedLength = executionContext.getValueFactories().getBinaryFactory().create(value).getSize();
    // dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, value);
    // when(propertyInfo.getProperty()).thenReturn(dnaProperty);
    // when(definition.getRequiredType()).thenReturn(PropertyType.STRING);
    // when(definition.isMultiple()).thenReturn(true);
    // prop = new JcrMultiValueProperty(cache, propertyId);
    // lengths = prop.getLengths();
    // assertThat(lengths, notNullValue());
    // assertThat(lengths.length, is(1));
    // assertThat(lengths[0], is(expectedLength));
    //
    // String[] values = new String[] {"value1", "value2", "value 3 is longer"};
    // dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, (Object[])values);
    // when(propertyInfo.getProperty()).thenReturn(dnaProperty);
    // when(definition.getRequiredType()).thenReturn(PropertyType.STRING);
    // when(definition.isMultiple()).thenReturn(true);
    // prop = new JcrMultiValueProperty(cache, propertyId);
    // lengths = prop.getLengths();
    // assertThat(lengths, notNullValue());
    // assertThat(lengths.length, is(values.length));
    // assertThat(lengths[0], is((long)values[0].length()));
    // assertThat(lengths[1], is((long)values[1].length()));
    // assertThat(lengths[2], is((long)values[2].length()));
    // }
}
