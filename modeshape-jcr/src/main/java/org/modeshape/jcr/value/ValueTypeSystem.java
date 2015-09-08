/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.value;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.modeshape.common.util.Base64;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.query.model.TypeSystem;
import org.modeshape.jcr.value.basic.NodeKeyReference;

/**
 * 
 */
public final class ValueTypeSystem extends TypeSystem {

    private final String defaultTypeName;
    protected final ValueFactories valueFactories;
    protected final ValueFactory<String> stringValueFactory;
    private final Map<PropertyType, TypeFactory<?>> typeFactoriesByPropertyType;
    private final Map<String, TypeFactory<?>> typeFactoriesByName;
    private final Map<String, PropertyType> propertyTypeByName;
    private final TypeFactory<String> stringFactory;
    private final TypeFactory<Boolean> booleanFactory;
    private final TypeFactory<Long> longFactory;
    private final TypeFactory<Double> doubleFactory;
    private final TypeFactory<BigDecimal> decimalFactory;
    private final TypeFactory<DateTime> dateFactory;
    private final TypeFactory<Path> pathFactory;
    private final TypeFactory<Name> nameFactory;
    private final TypeFactory<Reference> referenceFactory;
    private final TypeFactory<BinaryValue> binaryFactory;
    private final TypeFactory<NodeKey> nodeKeyFactory;

    /**
     * Create a type system using the supplied value factories.
     * 
     * @param valueFactories the value factories;
     * @throws IllegalArgumentException if the value factories are null
     */
    public ValueTypeSystem( ValueFactories valueFactories ) {
        this(valueFactories, null);
    }
   
    /**
     * Create a type system using the supplied value factories and locale
     * 
     * @param valueFactories the value factories;
     * @param locale the locale, may not be null
     * @throws IllegalArgumentException if the value factories are null
     */
    public ValueTypeSystem( final ValueFactories valueFactories, final Locale locale ) {
        this.valueFactories = valueFactories;
        this.defaultTypeName = PropertyType.STRING.getName().toUpperCase();
        Map<PropertyType, TypeFactory<?>> factories = new HashMap<>();
        this.stringValueFactory = valueFactories.getStringFactory();
        this.stringFactory = new Factory<String>(stringValueFactory) {
            @Override
            public String asString( Object value ) {
                return stringValueFactory.create(value);
            }

            @Override
            public String asReadableString( Object value ) {
                return stringValueFactory.create(value);
            }

            @Override
            @SuppressWarnings( { "unchecked", "rawtypes" } )
            public Comparator<String> getComparator() {
                return locale == null ? super.getComparator() : ValueComparators.collatorComparator(locale);
            }
        };
        this.booleanFactory = new Factory<Boolean>(valueFactories.getBooleanFactory());
        this.longFactory = new Factory<Long>(valueFactories.getLongFactory());
        this.doubleFactory = new Factory<Double>(valueFactories.getDoubleFactory());
        this.decimalFactory = new Factory<BigDecimal>(valueFactories.getDecimalFactory());
        this.dateFactory = new Factory<DateTime>(valueFactories.getDateFactory()) {

            @Override
            public DateTime create( String value ) throws ValueFormatException {
                DateTime result = valueFactory.create(value);
                // Convert the timestamp to UTC, since that's how everything should be queried ...
                return result.toUtcTimeZone();
            }
        };
        this.pathFactory = new Factory<Path>(valueFactories.getPathFactory());
        this.nameFactory = new Factory<Name>(valueFactories.getNameFactory());
        this.referenceFactory = new Factory<Reference>(valueFactories.getReferenceFactory());
        this.nodeKeyFactory = new NodeKeyTypeFactory(stringValueFactory);
        this.binaryFactory = new Factory<BinaryValue>(valueFactories.getBinaryFactory()) {
            @Override
            public String asReadableString( Object value ) {
                BinaryValue binary = this.valueFactory.create(value);
                // Just print out the SHA-1 hash in Base64, plus length
                return "(Binary,length=" + binary.getSize() + ",SHA1=" + Base64.encodeBytes(binary.getHash()) + ")";
            }

            @Override
            public long length( Object value ) {
                BinaryValue binary = this.valueFactory.create(value);
                return binary != null ? binary.getSize() : 0;
            }
        };
        factories.put(PropertyType.STRING, this.stringFactory);
        factories.put(PropertyType.BOOLEAN, this.booleanFactory);
        factories.put(PropertyType.DATE, this.dateFactory);
        factories.put(PropertyType.DECIMAL, new Factory<BigDecimal>(valueFactories.getDecimalFactory()));
        factories.put(PropertyType.DOUBLE, this.doubleFactory);
        factories.put(PropertyType.LONG, this.longFactory);
        factories.put(PropertyType.NAME, new Factory<Name>(valueFactories.getNameFactory()));
        factories.put(PropertyType.OBJECT, new Factory<Object>(valueFactories.getObjectFactory()));
        factories.put(PropertyType.PATH, this.pathFactory);
        factories.put(PropertyType.REFERENCE, new Factory<Reference>(valueFactories.getReferenceFactory()));
        factories.put(PropertyType.WEAKREFERENCE, new Factory<Reference>(valueFactories.getWeakReferenceFactory()));
        factories.put(PropertyType.SIMPLEREFERENCE, new Factory<Reference>(valueFactories.getSimpleReferenceFactory()));
        factories.put(PropertyType.URI, new Factory<URI>(valueFactories.getUriFactory()));
        factories.put(PropertyType.BINARY, this.binaryFactory);
        this.typeFactoriesByPropertyType = Collections.unmodifiableMap(factories);
        Map<String, PropertyType> propertyTypeByName = new HashMap<String, PropertyType>();
        for (Map.Entry<PropertyType, TypeFactory<?>> entry : this.typeFactoriesByPropertyType.entrySet()) {
            propertyTypeByName.put(entry.getValue().getTypeName(), entry.getKey());
        }
        this.propertyTypeByName = Collections.unmodifiableMap(propertyTypeByName);
        Map<String, TypeFactory<?>> byName = new HashMap<>();
        for (TypeFactory<?> factory : factories.values()) {
            byName.put(factory.getTypeName(), factory);
        }
        byName.put(nodeKeyFactory.getTypeName(), nodeKeyFactory);
        this.typeFactoriesByName = Collections.unmodifiableMap(byName);
    }

    @Override
    public String asString( Object value ) {
        return stringValueFactory.create(value);
    }

    @Override
    public TypeFactory<Boolean> getBooleanFactory() {
        return booleanFactory;
    }

    @Override
    public TypeFactory<String> getStringFactory() {
        return this.stringFactory;
    }

    @Override
    public TypeFactory<?> getDateTimeFactory() {
        return dateFactory;
    }

    @Override
    public String getDefaultType() {
        return defaultTypeName;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public Comparator<Object> getDefaultComparator() {
        return (Comparator<Object>)PropertyType.OBJECT.getComparator();
    }

    @Override
    public TypeFactory<Double> getDoubleFactory() {
        return doubleFactory;
    }

    @Override
    public TypeFactory<BigDecimal> getDecimalFactory() {
        return decimalFactory;
    }

    @Override
    public TypeFactory<Long> getLongFactory() {
        return longFactory;
    }

    @Override
    public TypeFactory<Path> getPathFactory() {
        return pathFactory;
    }

    @Override
    public TypeFactory<Name> getNameFactory() {
        return nameFactory;
    }

    @Override
    public TypeFactory<Reference> getReferenceFactory() {
        return referenceFactory;
    }

    @Override
    public TypeFactory<BinaryValue> getBinaryFactory() {
        return binaryFactory;
    }

    @Override
    public TypeFactory<NodeKey> getNodeKeyFactory() {
        return nodeKeyFactory;
    }

    @Override
    public TypeFactory<?> getTypeFactory( String typeName ) {
        if (typeName == null) return null;
        return typeFactoriesByName.get(typeName.toUpperCase()); // may be null
    }

    @Override
    public TypeFactory<?> getTypeFactory( Object prototype ) {
        ValueFactory<?> valueFactory = valueFactories.getValueFactory(prototype);
        if (valueFactory == null) return null;
        PropertyType type = valueFactory.getPropertyType();
        assert type != null;
        return typeFactoriesByPropertyType.get(type);
    }

    @Override
    public Set<String> getTypeNames() {
        return typeFactoriesByName.keySet();
    }

    @Override
    public String getCompatibleType( String type1,
                                     String type2 ) {
        if (type1 == null) {
            return type2 != null ? type2 : getDefaultType();
        }
        if (type2 == null) return type1;
        if (type1.equals(type2)) return type1;

        // neither is null ...
        PropertyType ptype1 = propertyTypeByName.get(type1);
        PropertyType ptype2 = propertyTypeByName.get(type2);
        assert ptype1 != null;
        assert ptype2 != null;
        if (ptype1 == PropertyType.STRING) return type1;
        if (ptype2 == PropertyType.STRING) return type2;
        // Dates are compatible with longs ...
        if (ptype1 == PropertyType.LONG && ptype2 == PropertyType.DATE) return type1;
        if (ptype1 == PropertyType.DATE && ptype2 == PropertyType.LONG) return type2;
        // Booleans and longs are compatible ...
        if (ptype1 == PropertyType.LONG && ptype2 == PropertyType.BOOLEAN) return type1;
        if (ptype1 == PropertyType.BOOLEAN && ptype2 == PropertyType.LONG) return type2;
        // Doubles and longs ...
        if (ptype1 == PropertyType.DOUBLE && ptype2 == PropertyType.LONG) return type1;
        if (ptype1 == PropertyType.LONG && ptype2 == PropertyType.DOUBLE) return type2;
        // Paths and names ...
        if (ptype1 == PropertyType.PATH && ptype2 == PropertyType.NAME) return type1;
        if (ptype1 == PropertyType.NAME && ptype2 == PropertyType.PATH) return type2;

        // Otherwise, it's just the default type (string) ...
        return getDefaultType();
    }

    @Override
    public TypeFactory<?> getCompatibleType( TypeFactory<?> type1,
                                             TypeFactory<?> type2 ) {
        return getTypeFactory(getCompatibleType(type1.getTypeName(), type2.getTypeName()));
    }

    protected class Factory<T> implements TypeFactory<T> {
        protected final PropertyType type;
        protected final ValueFactory<T> valueFactory;
        protected final String typeName;

        protected Factory( ValueFactory<T> valueFactory ) {
            this.valueFactory = valueFactory;
            this.type = this.valueFactory.getPropertyType();
            this.typeName = type.getName().toUpperCase();
        }

        @Override
        public String asReadableString( Object value ) {
            return asString(value);
        }

        @Override
        public String asString( Object value ) {
            if (value instanceof String) {
                // Convert to the typed value, then back to a string ...
                value = valueFactory.create((String)value);
            }
            return stringValueFactory.create(value);
        }

        @Override
        public T create( String value ) throws ValueFormatException {
            return valueFactory.create(value);
        }

        @Override
        public T create( Object value ) throws ValueFormatException {
            return valueFactory.create(value);
        }

        @Override
        @SuppressWarnings( "unchecked" )
        public Class<T> getType() {
            return (Class<T>)type.getValueClass();
        }

        @Override
        public long length( Object value ) {
            String str = asString(valueFactory.create(value));
            return str != null ? str.length() : 0;
        }

        @Override
        @SuppressWarnings( "unchecked" )
        public Comparator<T> getComparator() {
            return (Comparator<T>)type.getComparator();
        }

        @Override
        public String getTypeName() {
            return typeName;
        }

        @Override
        public String toString() {
            return "TypeFactory<" + getTypeName() + ">";
        }

    }

    protected static class NodeKeyTypeFactory implements TypeFactory<NodeKey> {
        private final ValueFactory<String> stringFactory;

        protected NodeKeyTypeFactory( ValueFactory<String> stringFactory ) {
            this.stringFactory = stringFactory;
        }

        @Override
        public Class<NodeKey> getType() {
            return NodeKey.class;
        }

        @Override
        public String getTypeName() {
            return getType().getName().toUpperCase();
        }

        @Override
        public String asString( Object value ) {
            return ((NodeKey)value).toString();
        }

        @Override
        public String asReadableString( Object value ) {
            return asString(value);
        }

        @Override
        public long length( Object value ) {
            return asString(value).length();
        }

        @Override
        public NodeKey create( Object value ) throws ValueFormatException {
            if (value == null) return null;
            if (value instanceof NodeKey) {
                return (NodeKey)value;
            }
            if (value instanceof NodeKeyReference) {
                return ((NodeKeyReference)value).getNodeKey();
            }
            String str = stringFactory.create(value);
            return create(str);
        }

        @Override
        public NodeKey create( String value ) throws ValueFormatException {
            if (NodeKey.isValidFormat(value)) {
                return new NodeKey(value);
            }
            throw new ValueFormatException(value, PropertyType.OBJECT, "Unable to convert " + value.getClass() + " to a NodeKey");
        }

        @Override
        public Comparator<NodeKey> getComparator() {
            return NodeKey.COMPARATOR;
        }
    }

}
