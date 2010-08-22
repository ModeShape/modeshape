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
package org.modeshape.connector.store.jpa.util;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.SecureHash;
import org.modeshape.common.util.StringUtil;
import org.modeshape.connector.store.jpa.util.Serializer.ReferenceValues;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.property.Reference;
import org.modeshape.graph.property.ValueFactories;

/**
 * @author Randall Hauch
 */
public class SerializerTest {

    private Serializer serializer;
    private ExecutionContext context;
    private LargeValuesHolder largeValues;
    private PropertyFactory propertyFactory;
    private ValueFactories valueFactories;
    private ReferenceValues references;

    @Before
    public void beforeEach() {
        context = new ExecutionContext();
        propertyFactory = context.getPropertyFactory();
        valueFactories = context.getValueFactories();
        serializer = new Serializer(context, false);
        largeValues = new LargeValuesHolder();
        references = Serializer.NO_REFERENCES_VALUES;
    }

    @Test
    public void shouldSerializeAndDeserializeLongProperty() throws Exception {
        Property prop = createProperty("p1", new Long(1));
        assertSerializableAndDeserializable(serializer, prop);
        assertThat(largeValues.getCount(), is(0));
    }

    @Test
    public void shouldSerializeAndDeserializeIntegerProperty() throws Exception {
        Property prop = createProperty("p1", new Integer(1));
        assertSerializableAndDeserializable(serializer, prop);
        assertThat(largeValues.getCount(), is(0));
    }

    @Test
    public void shouldSerializeAndDeserializeShortProperty() throws Exception {
        Property prop = createProperty("p1", new Short((short)1));
        assertSerializableAndDeserializable(serializer, prop);
        assertThat(largeValues.getCount(), is(0));
    }

    @Test
    public void shouldSerializeAndDeserializeFloatProperty() throws Exception {
        Property prop = createProperty("p1", new Float(1.0f));
        assertSerializableAndDeserializable(serializer, prop);
        assertThat(largeValues.getCount(), is(0));
    }

    @Test
    public void shouldSerializeAndDeserializeDoubleProperty() throws Exception {
        Property prop = createProperty("p1", new Double(1.0d));
        assertSerializableAndDeserializable(serializer, prop);
        assertThat(largeValues.getCount(), is(0));
    }

    @Test
    public void shouldSerializeAndDeserializeBooleanProperty() throws Exception {
        Property prop = createProperty("p1", new Boolean(true));
        assertSerializableAndDeserializable(serializer, prop);
        assertThat(largeValues.getCount(), is(0));
    }

    @Test
    public void shouldSerializeAndDeserializeNameProperty() throws Exception {
        Property prop = createProperty("p1", valueFactories.getNameFactory().create("something"));
        assertSerializableAndDeserializable(serializer, prop);
        assertThat(largeValues.getCount(), is(0));
    }

    @Test
    public void shouldSerializeAndDeserializePathProperty() throws Exception {
        Property prop = createProperty("p1", valueFactories.getPathFactory().create("/a/b/c/something"));
        assertSerializableAndDeserializable(serializer, prop);
        assertThat(largeValues.getCount(), is(0));
    }

    @Test
    public void shouldSerializeAndDeserializeDateTimeProperty() throws Exception {
        Property prop = createProperty("p1", valueFactories.getDateFactory().createUtc());
        assertSerializableAndDeserializable(serializer, prop);
        assertThat(largeValues.getCount(), is(0));

        prop = createProperty("p1", valueFactories.getDateFactory().create());
        assertSerializableAndDeserializable(serializer, prop);
        assertThat(largeValues.getCount(), is(0));
    }

    @Test
    public void shouldSerializeAndDeserializeUuidProperty() throws Exception {
        Property prop = createProperty("p1", UUID.randomUUID());
        assertSerializableAndDeserializable(serializer, prop);
        assertThat(largeValues.getCount(), is(0));
    }

    @Test
    public void shouldSerializeAndDeserializeUriProperty() throws Exception {
        Property prop = createProperty("p1", new URI("http://example.com"));
        assertSerializableAndDeserializable(serializer, prop);
        assertThat(largeValues.getCount(), is(0));
    }

    @Test
    public void shouldSerializeAndDeserializeReferenceProperty() throws Exception {
        UUID uuid = UUID.randomUUID();
        Property prop = createProperty("p1", valueFactories.getReferenceFactory().create(uuid.toString()));
        assertSerializableAndDeserializable(serializer, prop);
        assertThat(largeValues.getCount(), is(0));
    }

    @Test
    public void shouldSerializeAndDeserializeBigDecimalProperty() throws Exception {
        Property prop = createProperty("p1", valueFactories.getDecimalFactory().create("1.0123455243284347375478525485466895512"));
        assertSerializableAndDeserializable(serializer, prop);
        assertThat(largeValues.getCount(), is(0));
    }

    @Test
    public void shouldSerializeAndDeserializeSmallBinaryProperty() throws Exception {
        String value = "v1";
        Property prop = createProperty("p1", valueFactories.getBinaryFactory().create(value));
        assertSerializableAndDeserializable(serializer, prop);
        assertThat(largeValues.getCount(), is(0));
    }

    @Test
    public void shouldSerializeAndDeserializeLargeBinaryProperty() throws Exception {
        String value = "really really long string that will be converted to a binary value and tested like that";
        Property prop = createProperty("p1", valueFactories.getBinaryFactory().create(value));
        assertSerializableAndDeserializable(serializer, prop);
        assertThat(largeValues.getCount(), is(1));
    }

    @Test
    public void shouldSerializeAndDeserializeSmallStringProperty() throws Exception {
        Property prop = createProperty("p1", "v1");
        assertSerializableAndDeserializable(serializer, prop);
        assertThat(largeValues.getCount(), is(0));
    }

    @Test
    public void shouldSerializeAndDeserializeLargeStringProperty() throws Exception {
        String value = "v234567890123456789012345678901234567890";
        Property prop = createProperty("p1", value);
        assertSerializableAndDeserializable(serializer, prop);
        assertThat(largeValues.getCount(), is(1));
        assertThat(largeValues.get(value).value, is((Object)value));
    }

    @Test
    public void shouldSerializeAndDeserializeSmallAndLargeStringProperty() throws Exception {
        Property prop1 = createProperty("p1", "v1");
        String value = "v234567890123456789012345678901234567890";
        Property prop2 = createProperty("p2", value);
        Property prop3 = createProperty("p3", "v2");
        Property prop4 = createProperty("p4", new String(value)); // make sure it's a different String object

        assertSerializableAndDeserializable(serializer, prop1, prop2, prop3, prop4);
        assertThat(largeValues.getCount(), is(1));
    }

    @Test
    public void shouldSerializeAndDeserializeMixtureOfSmallAndLargeProperties() throws Exception {
        Property prop1 = createProperty("p1", "v1");
        String value = "v234567890123456789012345678901234567890";
        Property prop2 = createProperty("p2", value);
        Property prop3 = createProperty("p3", "v2");
        Property prop4 = createProperty("p4", new String(value)); // make sure it's a different String object
        Property prop5 = createProperty("p5", valueFactories.getBinaryFactory().create("something"));
        String binaryValue = "really really long string that will be converted to a binary value and tested like that";
        Property prop6 = createProperty("p6", valueFactories.getBinaryFactory().create(binaryValue));
        UUID uuid7 = UUID.randomUUID();
        Reference ref7 = valueFactories.getReferenceFactory().create(uuid7);
        Property prop7 = createProperty("p7", ref7);
        UUID uuid8 = UUID.randomUUID();
        Reference ref8 = valueFactories.getReferenceFactory().create(uuid8);
        Property prop8 = createProperty("p8", ref8);

        assertSerializableAndDeserializable(serializer, prop1, prop2, prop3, prop4, prop5, prop6, prop7, prop8);
        assertThat(largeValues.getCount(), is(2));
    }

    @Test
    public void shouldReserializePropertiesWithUpdates() throws Exception {
        Property prop1 = createProperty("p1", "v1");
        String value = "v234567890123456789012345678901234567890";
        Property prop2 = createProperty("p2", value);
        Property prop3 = createProperty("p3", "v2");
        Property prop4 = createProperty("p4", new String(value)); // make sure it's a different String object
        Property prop5 = createProperty("p5", valueFactories.getBinaryFactory().create("something"));
        String binaryValueStr = "really really long string that will be converted to a binary value and tested like that";
        Binary binaryValue = valueFactories.getBinaryFactory().create(binaryValueStr);
        Property prop6 = createProperty("p6", binaryValue);
        UUID uuid7 = UUID.randomUUID();
        Reference ref7 = valueFactories.getReferenceFactory().create(uuid7);
        Property prop7 = createProperty("p7", ref7);
        UUID uuid8 = UUID.randomUUID();
        Reference ref8 = valueFactories.getReferenceFactory().create(uuid8);
        Property prop8 = createProperty("p8", ref8);

        Property prop2b = createProperty("p2");
        Property prop3b = createProperty("p3", "v3");
        String binaryValueStr2 = binaryValueStr + " but modified";
        Binary binaryValue2 = valueFactories.getBinaryFactory().create(binaryValueStr2);
        Property prop6b = createProperty("p6", binaryValue2);

        Property[] initial = new Property[] {prop1, prop2, prop3, prop4, prop5, prop6, prop7, prop8};
        Property[] updated = new Property[] {prop2b, prop3b, prop6b};
        Name[] deleted = new Name[] {};
        SkippedLargeValues removedLargeValues = new SkippedLargeValues();
        assertReserializable(serializer, removedLargeValues, initial, updated, deleted);

        assertThat(largeValues.getCount(), is(3));
        assertThat(removedLargeValues.getCount(), is(2)); // p2's value and p6's original value
        assertThat(largeValues.get(serializer.computeHash(value)), is(notNullValue()));
        assertThat(largeValues.get(binaryValue2), is(notNullValue()));
        assertThat(largeValues.get(binaryValue2), is(notNullValue()));
        assertThat(removedLargeValues.isSkipped(binaryValue), is(true));
    }

    @Test
    public void shouldAdjustReferences() throws Exception {
        Property prop1 = createProperty("p1", "v1");
        String value = "v234567890123456789012345678901234567890";
        Property prop2 = createProperty("p2", value);
        Property prop3 = createProperty("p3", "v2");
        Property prop4 = createProperty("p4", new String(value)); // make sure it's a different String object
        Property prop5 = createProperty("p5", valueFactories.getBinaryFactory().create("something"));
        String binaryValueStr = "really really long string that will be converted to a binary value and tested like that";
        Binary binaryValue = valueFactories.getBinaryFactory().create(binaryValueStr);
        Property prop6 = createProperty("p6", binaryValue);
        UUID uuid7 = UUID.randomUUID();
        Reference ref7 = valueFactories.getReferenceFactory().create(uuid7);
        Property prop7 = createProperty("p7", ref7);
        UUID uuid8 = UUID.randomUUID();
        Reference ref8 = valueFactories.getReferenceFactory().create(uuid8);
        Property prop8 = createProperty("p8", ref8);

        // Serialize the properties (and verify they're serialized properly) ...
        Property[] props = new Property[] {prop1, prop2, prop3, prop4, prop5, prop6, prop7, prop8};
        byte[] content = serialize(serializer, props);
        List<Property> properties = deserialize(serializer, content);
        assertThat(properties, hasItems(props));

        // Define the old-to-new UUID mapping ...
        UUID newUuid7 = UUID.randomUUID();
        Map<String, String> oldToNewUuids = new HashMap<String, String>();
        oldToNewUuids.put(uuid7.toString(), newUuid7.toString());
        // note that 'uuid8' is not included, so 'ref8' should be untouched

        // Now update the references in the serialized properties ...
        ByteArrayInputStream bais = new ByteArrayInputStream(content);
        ObjectInputStream ois = new ObjectInputStream(bais);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        try {
            serializer.adjustReferenceProperties(ois, oos, oldToNewUuids);
        } finally {
            baos.close();
            oos.close();
        }
        byte[] newContent = baos.toByteArray();

        // Now deserialize the updated content ...
        properties = deserialize(serializer, newContent);

        // Update a new 'prop7' ...
        Reference newRef7 = valueFactories.getReferenceFactory().create(newUuid7);
        Property newProp7 = createProperty("p7", newRef7);
        Property[] newProps = new Property[] {prop1, prop2, prop3, prop4, prop5, prop6, newProp7, prop8};

        // Finally verify that the updated content matches the expected new properties ...
        assertThat(properties, hasItems(newProps));
    }

    protected Property createProperty( String name,
                                       Object... values ) {
        return propertyFactory.create(valueFactories.getNameFactory().create(name), values);
    }

    protected void assertSerializableAndDeserializable( Serializer serializer,
                                                        Property... properties ) throws IOException, ClassNotFoundException {
        for (Property property : properties) {
            // Serialize the properties one at a time ...
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            try {
                serializer.serializeProperty(oos, property, largeValues, references);
            } finally {
                oos.close();
            }
            byte[] bytes = baos.toByteArray();

            // Deserialize ...
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            Property copy = null;
            try {
                copy = serializer.deserializeProperty(ois, largeValues);
            } finally {
                ois.close();
            }
            // Check the property ...
            assertThat(copy, is(property));
        }

        // Now serialize and deserialize the list of properties ...
        List<Property> propertyList = Arrays.asList(properties);
        List<Property> outputProperties = new ArrayList<Property>(propertyList.size());

        // Serialize the properties one at a time ...
        byte[] bytes = serialize(serializer, propertyList.toArray(new Property[propertyList.size()]));

        // Deserialize ...
        outputProperties = deserialize(serializer, bytes);

        // Check the properties match ...
        assertThat(outputProperties.size(), is(propertyList.size()));
        assertThat(outputProperties, hasItems(propertyList.toArray(new Property[propertyList.size()])));
    }

    protected byte[] serialize( Serializer serializer,
                                Property... originalProperties ) throws IOException {
        // Serialize the properties one at a time ...
        Collection<Property> initialProps = Arrays.asList(originalProperties);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        try {
            serializer.serializeProperties(oos, initialProps.size(), initialProps, largeValues, references);
        } finally {
            oos.close();
        }
        return baos.toByteArray();
    }

    protected List<Property> deserialize( Serializer serializer,
                                          byte[] content ) throws IOException, ClassNotFoundException {
        // Deserialize ...
        List<Property> afterProperties = new ArrayList<Property>();
        ByteArrayInputStream bais = new ByteArrayInputStream(content);
        ObjectInputStream ois = new ObjectInputStream(bais);
        try {
            serializer.deserializeAllProperties(ois, afterProperties, largeValues);
        } finally {
            ois.close();
        }
        return afterProperties;
    }

    protected void assertReserializable( Serializer serializer,
                                         Serializer.LargeValues removedLargeValues,
                                         Property[] originalProperties,
                                         Property[] updatedProperties,
                                         Name[] removedProperties ) throws IOException, ClassNotFoundException {
        Collection<Name> propertiesThatStay = new HashSet<Name>();
        Collection<Name> propertiesThatAreDeleted = new HashSet<Name>();
        Set<Name> propertiesThatAreNew = new HashSet<Name>();
        for (Property prop : originalProperties) {
            propertiesThatStay.add(prop.getName());
        }
        for (Property prop : updatedProperties) {
            if (propertiesThatStay.add(prop.getName())) {
                // The property is new since it wasn't in the original set of names ...
                propertiesThatAreNew.add(prop.getName());
            }
        }
        for (Name removedPropertyName : removedProperties) {
            propertiesThatAreDeleted.add(removedPropertyName);
            propertiesThatStay.remove(removedPropertyName);
            assertThat(propertiesThatAreNew.contains(removedPropertyName), is(false));
        }

        // Serialize the properties one at a time ...
        byte[] bytes = serialize(serializer, originalProperties);

        // Now reserialize, updating the properties ...
        Map<Name, Property> updatedProps = new HashMap<Name, Property>();
        for (Property property : updatedProperties) {
            updatedProps.put(property.getName(), property);
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        Set<Name> createdProperties = new HashSet<Name>();
        try {
            serializer.reserializeProperties(ois,
                                             oos,
                                             updatedProps,
                                             largeValues,
                                             removedLargeValues,
                                             createdProperties,
                                             references);
        } finally {
            oos.close();
            ois.close();
        }

        // Deserialize ...
        List<Property> afterProperties = deserialize(serializer, baos.toByteArray());

        Collection<Name> namesAfter = new HashSet<Name>();
        for (Property prop : afterProperties) {
            namesAfter.add(prop.getName());
        }

        // Check the properties match ...
        assertThat(afterProperties.size(), is(propertiesThatStay.size()));
        assertThat(namesAfter, is(propertiesThatStay));
        for (Name deleted : propertiesThatAreDeleted) {
            assertThat(namesAfter.contains(deleted), is(false));
        }
        assertThat(createdProperties, is(propertiesThatAreNew));
    }

    protected class SkippedLargeValues implements Serializer.LargeValues {
        private int minimumSize = 20;
        private Set<String> skippedKeys = new HashSet<String>();

        public SkippedLargeValues() {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.connector.store.jpa.util.Serializer.LargeValues#getMinimumSize()
         */
        public long getMinimumSize() {
            return minimumSize;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.connector.store.jpa.util.Serializer.LargeValues#read(org.modeshape.graph.property.ValueFactories,
         *      byte[], long)
         */
        public Object read( ValueFactories valueFactories,
                            byte[] hash,
                            long length ) {
            String key = StringUtil.getHexString(hash);
            return skippedKeys.add(key);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.connector.store.jpa.util.Serializer.LargeValues#write(byte[], long,
         *      org.modeshape.graph.property.PropertyType, java.lang.Object)
         */
        public void write( byte[] hash,
                           long length,
                           PropertyType type,
                           Object value ) {
            throw new UnsupportedOperationException();
        }

        public boolean isSkipped( Binary binary ) {
            String key = StringUtil.getHexString(binary.getHash());
            return isSkipped(key);
        }

        public boolean isSkipped( String key ) {
            return skippedKeys.contains(key);
        }

        public int getCount() {
            return skippedKeys.size();
        }
    }

    protected class LargeValuesHolder implements Serializer.LargeValues {
        private int minimumSize = 20;
        private final Map<String, LargeValue> largeValuesByHexHash = new HashMap<String, LargeValue>();

        public LargeValuesHolder() {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.connector.store.jpa.util.Serializer.LargeValues#getMinimumSize()
         */
        public long getMinimumSize() {
            return minimumSize;
        }

        /**
         * @param minimumSize Sets minimumSize to the specified value.
         */
        public void setMinimumSize( int minimumSize ) {
            CheckArg.isPositive(minimumSize, "minimumSize");
            this.minimumSize = minimumSize;
        }

        public int getCount() {
            return this.largeValuesByHexHash.size();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.connector.store.jpa.util.Serializer.LargeValues#read(org.modeshape.graph.property.ValueFactories,
         *      byte[], long)
         */
        public Object read( ValueFactories valueFactories,
                            byte[] hash,
                            long length ) {
            LargeValue largeValue = get(hash);
            return largeValue != null ? largeValue.value : null;
        }

        public LargeValue get( String obj ) throws NoSuchAlgorithmException {
            byte[] hash = SecureHash.getHash(SecureHash.Algorithm.SHA_1, obj.getBytes());
            return get(hash);
        }

        public LargeValue get( Binary obj ) {
            return get(obj.getHash());
        }

        public LargeValue get( byte[] hash ) {
            String hexHash = StringUtil.getHexString(hash);
            return largeValuesByHexHash.get(hexHash);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.connector.store.jpa.util.Serializer.LargeValues#write(byte[], long,
         *      org.modeshape.graph.property.PropertyType, java.lang.Object)
         */
        public void write( byte[] hash,
                           long length,
                           PropertyType type,
                           Object value ) {
            String hexHash = StringUtil.getHexString(hash);
            largeValuesByHexHash.put(hexHash, new LargeValue(hash, length, type, value));
        }

        protected class LargeValue {
            protected final byte[] hash;
            protected final long length;
            protected final PropertyType type;
            protected final Object value;

            protected LargeValue( byte[] hash,
                                  long length,
                                  PropertyType type,
                                  Object value ) {
                assert hash != null;
                assert length > 0;
                assert type != null;
                assert value != null;
                this.hash = hash;
                this.length = length;
                this.type = type;
                this.value = value;
            }
        }
    }

}
