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
package org.modeshape.graph.property;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.modeshape.common.util.Base64;
import org.modeshape.graph.query.model.TypeSystem;

/**
 * 
 */
public class ValueTypeSystem implements TypeSystem {

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
    private final TypeFactory<?> dateFactory;
    private final TypeFactory<?> pathFactory;
    private final TypeFactory<?> referenceFactory;
    private final TypeFactory<?> binaryFactory;

    /**
     * Create a type system using the supplied value factories.
     * 
     * @param valueFactories the value factories;
     * @throws IllegalArgumentException if the value factories are null
     */
    public ValueTypeSystem( ValueFactories valueFactories ) {
        this.valueFactories = valueFactories;
        this.defaultTypeName = PropertyType.STRING.getName().toUpperCase();
        Map<PropertyType, TypeFactory<?>> factories = new HashMap<PropertyType, TypeFactory<?>>();
        this.stringValueFactory = valueFactories.getStringFactory();
        this.stringFactory = new Factory<String>(stringValueFactory) {
            /**
             * {@inheritDoc}
             * 
             * @see org.modeshape.graph.property.ValueTypeSystem.Factory#asString(java.lang.Object)
             */
            @Override
            public String asString( Object value ) {
                return stringValueFactory.create(value);
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.modeshape.graph.property.ValueTypeSystem.Factory#asReadableString(java.lang.Object)
             */
            @Override
            public String asReadableString( Object value ) {
                return stringValueFactory.create(value);
            }
        };
        this.booleanFactory = new Factory<Boolean>(valueFactories.getBooleanFactory());
        this.longFactory = new Factory<Long>(valueFactories.getLongFactory());
        this.doubleFactory = new Factory<Double>(valueFactories.getDoubleFactory());
        this.decimalFactory = new Factory<BigDecimal>(valueFactories.getDecimalFactory());
        this.dateFactory = new Factory<DateTime>(valueFactories.getDateFactory()) {

            /**
             * {@inheritDoc}
             * 
             * @see org.modeshape.graph.property.ValueTypeSystem.Factory#create(java.lang.String)
             */
            @Override
            public DateTime create( String value ) throws ValueFormatException {
                DateTime result = valueFactory.create(value);
                // Convert the timestamp to UTC, since that's how everything should be queried ...
                return result.toUtcTimeZone();
            }
        };
        this.pathFactory = new Factory<Path>(valueFactories.getPathFactory());
        this.referenceFactory = new Factory<Reference>(valueFactories.getReferenceFactory());
        this.binaryFactory = new Factory<Binary>(valueFactories.getBinaryFactory()) {
            /**
             * {@inheritDoc}
             * 
             * @see org.modeshape.graph.property.ValueTypeSystem.Factory#asReadableString(java.lang.Object)
             */
            @Override
            public String asReadableString( Object value ) {
                Binary binary = this.valueFactory.create(value);
                // Just print out the SHA-1 hash in Base64, plus length
                return "(Binary,length=" + binary.getSize() + ",SHA1=" + Base64.encodeBytes(binary.getHash()) + ")";
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.modeshape.graph.property.ValueTypeSystem.Factory#length(java.lang.Object)
             */
            @Override
            public long length( Object value ) {
                Binary binary = this.valueFactory.create(value);
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
        factories.put(PropertyType.URI, new Factory<URI>(valueFactories.getUriFactory()));
        factories.put(PropertyType.UUID, new Factory<UUID>(valueFactories.getUuidFactory()));
        factories.put(PropertyType.BINARY, this.binaryFactory);
        this.typeFactoriesByPropertyType = Collections.unmodifiableMap(factories);
        Map<String, PropertyType> propertyTypeByName = new HashMap<String, PropertyType>();
        for (Map.Entry<PropertyType, TypeFactory<?>> entry : this.typeFactoriesByPropertyType.entrySet()) {
            propertyTypeByName.put(entry.getValue().getTypeName(), entry.getKey());
        }
        this.propertyTypeByName = Collections.unmodifiableMap(propertyTypeByName);
        Map<String, TypeFactory<?>> byName = new HashMap<String, TypeFactory<?>>();
        for (TypeFactory<?> factory : factories.values()) {
            byName.put(factory.getTypeName(), factory);
        }
        this.typeFactoriesByName = Collections.unmodifiableMap(byName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.TypeSystem#asString(java.lang.Object)
     */
    public String asString( Object value ) {
        return stringValueFactory.create(value);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.TypeSystem#getBooleanFactory()
     */
    public TypeFactory<Boolean> getBooleanFactory() {
        return booleanFactory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.TypeSystem#getStringFactory()
     */
    public TypeFactory<String> getStringFactory() {
        return this.stringFactory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.TypeSystem#getDateTimeFactory()
     */
    public TypeFactory<?> getDateTimeFactory() {
        return dateFactory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.TypeSystem#getDefaultType()
     */
    public String getDefaultType() {
        return defaultTypeName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.TypeSystem#getDefaultComparator()
     */
    @SuppressWarnings( "unchecked" )
    public Comparator<Object> getDefaultComparator() {
        return (Comparator<Object>)PropertyType.OBJECT.getComparator();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.TypeSystem#getDoubleFactory()
     */
    public TypeFactory<Double> getDoubleFactory() {
        return doubleFactory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.TypeSystem#getDecimalFactory()
     */
    public TypeFactory<BigDecimal> getDecimalFactory() {
        return decimalFactory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.TypeSystem#getLongFactory()
     */
    public TypeFactory<Long> getLongFactory() {
        return longFactory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.TypeSystem#getPathFactory()
     */
    public TypeFactory<?> getPathFactory() {
        return pathFactory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.TypeSystem#getReferenceFactory()
     */
    public TypeFactory<?> getReferenceFactory() {
        return referenceFactory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.TypeSystem#getBinaryFactory()
     */
    public TypeFactory<?> getBinaryFactory() {
        return binaryFactory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.TypeSystem#getTypeFactory(java.lang.String)
     */
    public TypeFactory<?> getTypeFactory( String typeName ) {
        if (typeName == null) return null;
        return typeFactoriesByName.get(typeName.toUpperCase()); // may be null
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.TypeSystem#getTypeFactory(java.lang.Object)
     */
    public TypeFactory<?> getTypeFactory( Object prototype ) {
        ValueFactory<?> valueFactory = valueFactories.getValueFactory(prototype);
        if (valueFactory == null) return null;
        PropertyType type = valueFactory.getPropertyType();
        assert type != null;
        return typeFactoriesByPropertyType.get(type);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.TypeSystem#getTypeNames()
     */
    public Set<String> getTypeNames() {
        return typeFactoriesByName.keySet();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.TypeSystem#getCompatibleType(java.lang.String, java.lang.String)
     */
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

    protected class Factory<T> implements TypeFactory<T> {
        protected final PropertyType type;
        protected final ValueFactory<T> valueFactory;
        protected final String typeName;

        protected Factory( ValueFactory<T> valueFactory ) {
            this.valueFactory = valueFactory;
            this.type = this.valueFactory.getPropertyType();
            this.typeName = type.getName().toUpperCase();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.TypeSystem.TypeFactory#asReadableString(java.lang.Object)
         */
        public String asReadableString( Object value ) {
            return asString(value);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.TypeSystem.TypeFactory#asString(java.lang.Object)
         */
        public String asString( Object value ) {
            if (value instanceof String) {
                // Convert to the typed value, then back to a string ...
                value = valueFactory.create((String)value);
            }
            return stringValueFactory.create(value);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.TypeSystem.TypeFactory#create(java.lang.String)
         */
        public T create( String value ) throws ValueFormatException {
            return valueFactory.create(value);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.TypeSystem.TypeFactory#create(java.lang.Object)
         */
        public T create( Object value ) throws ValueFormatException {
            return valueFactory.create(value);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.TypeSystem.TypeFactory#getType()
         */
        @SuppressWarnings( "unchecked" )
        public Class<T> getType() {
            return (Class<T>)type.getValueClass();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.TypeSystem.TypeFactory#length(java.lang.Object)
         */
        public long length( Object value ) {
            String str = asString(valueFactory.create(value));
            return str != null ? str.length() : 0;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.TypeSystem.TypeFactory#getComparator()
         */
        @SuppressWarnings( "unchecked" )
        public Comparator<T> getComparator() {
            return (Comparator<T>)type.getComparator();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.TypeSystem.TypeFactory#getTypeName()
         */
        public String getTypeName() {
            return typeName;
        }

    }
}
