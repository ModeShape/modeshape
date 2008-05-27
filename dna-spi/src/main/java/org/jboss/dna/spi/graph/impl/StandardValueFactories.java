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
package org.jboss.dna.spi.graph.impl;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.spi.graph.Binary;
import org.jboss.dna.spi.graph.DateTimeFactory;
import org.jboss.dna.spi.graph.NameFactory;
import org.jboss.dna.spi.graph.NamespaceRegistry;
import org.jboss.dna.spi.graph.PathFactory;
import org.jboss.dna.spi.graph.PropertyType;
import org.jboss.dna.spi.graph.Reference;
import org.jboss.dna.spi.graph.ValueFactories;
import org.jboss.dna.spi.graph.ValueFactory;

/**
 * The standard set of {@link ValueFactory value factories}.
 * @author Randall Hauch
 */
@Immutable
public class StandardValueFactories implements ValueFactories {

    // This class is implemented with separate members for each factory so that the typical usage is optimized.
    private final ValueFactory<String> stringFactory;
    private final ValueFactory<Binary> binaryFactory;
    private final ValueFactory<Boolean> booleanFactory;
    private final DateTimeFactory dateFactory;
    private final ValueFactory<BigDecimal> decimalFactory;
    private final ValueFactory<Double> doubleFactory;
    private final ValueFactory<Long> longFactory;
    private final NameFactory nameFactory;
    private final PathFactory pathFactory;
    private final ValueFactory<Reference> referenceFactory;
    private final ValueFactory<URI> uriFactory;
    private final ValueFactory<Object> objectFactory;
    private final Map<PropertyType, ValueFactory<?>> factories;

    private final NamespaceRegistry namespaceRegistry;
    private final TextEncoder encoder;

    /**
     * Create a standard set of value factories, using the {@link ValueFactory#DEFAULT_ENCODER default encoder/decoder}.
     * @param namespaceRegistry the namespace registry
     * @throws IllegalArgumentException if the namespace registry is null
     */
    public StandardValueFactories( NamespaceRegistry namespaceRegistry ) {
        this(namespaceRegistry, null);
    }

    /**
     * Create a standard set of value factories, using the supplied encoder/decoder.
     * @param namespaceRegistry the namespace registry
     * @param encoder the encoder that should be used; if null, the {@link ValueFactory#DEFAULT_ENCODER default encoder} is used.
     * @param extraFactories any extra factories that should be used; any factory will override the standard factories based upon
     * the {@link ValueFactory#getPropertyType() factory's property type}.
     * @throws IllegalArgumentException if the namespace registry is null
     */
    public StandardValueFactories( NamespaceRegistry namespaceRegistry, TextEncoder encoder, ValueFactory<?>... extraFactories ) {
        ArgCheck.isNotNull(namespaceRegistry, "namespaceRegistry");
        this.namespaceRegistry = namespaceRegistry;
        this.encoder = encoder != null ? encoder : ValueFactory.DEFAULT_ENCODER;
        Map<PropertyType, ValueFactory<?>> factories = new HashMap<PropertyType, ValueFactory<?>>();

        // Put the extra factories into the map first ...
        for (ValueFactory<?> factory : extraFactories) {
            if (factory == null) continue;
            factories.put(factory.getPropertyType(), factory);
        }

        // Now assign the members, using the factories in the map or (if null) the supplied default ...
        this.stringFactory = getFactory(factories, new StringValueFactory(this.encoder));
        this.binaryFactory = getFactory(factories, new InMemoryBinaryValueFactory(this.encoder, this.stringFactory));
        this.booleanFactory = getFactory(factories, new BooleanValueFactory(this.encoder, this.stringFactory));
        this.dateFactory = (DateTimeFactory)getFactory(factories, new JodaDateTimeValueFactory(this.encoder, this.stringFactory));
        this.decimalFactory = getFactory(factories, new DecimalValueFactory(this.encoder, this.stringFactory));
        this.doubleFactory = getFactory(factories, new DoubleValueFactory(this.encoder, this.stringFactory));
        this.longFactory = getFactory(factories, new LongValueFactory(this.encoder, this.stringFactory));
        this.nameFactory = (NameFactory)getFactory(factories, new NameValueFactory(this.namespaceRegistry, this.encoder, this.stringFactory));
        this.pathFactory = (PathFactory)getFactory(factories, new PathValueFactory(this.encoder, this.stringFactory, this.nameFactory));
        this.referenceFactory = getFactory(factories, new UuidReferenceValueFactory(this.encoder, this.stringFactory));
        this.uriFactory = getFactory(factories, new UriValueFactory(this.namespaceRegistry, this.encoder, this.stringFactory));
        this.objectFactory = getFactory(factories, new ObjectValueFactory(this.encoder, this.stringFactory, this.binaryFactory));

        // Wrap the factories with an unmodifiable ...
        this.factories = Collections.unmodifiableMap(factories);
    }

    @SuppressWarnings( "unchecked" )
    private static <T> ValueFactory<T> getFactory( Map<PropertyType, ValueFactory<?>> factories, ValueFactory<T> defaultFactory ) {
        PropertyType type = defaultFactory.getPropertyType();
        ValueFactory<?> factory = factories.get(type);
        if (factory == null) {
            factory = defaultFactory;
            factories.put(type, factory);
        }
        return (ValueFactory<T>)factory;
    }

    /**
     * @return encoder
     */
    public TextEncoder getTextEncoder() {
        return this.encoder;
    }

    /**
     * @return namespaceRegistry
     */
    public NamespaceRegistry getNamespaceRegistry() {
        return this.namespaceRegistry;
    }

    /**
     * @return factories
     */
    public Map<PropertyType, ValueFactory<?>> getMapOfValueFactories() {
        return this.factories;
    }

    /**
     * {@inheritDoc}
     */
    public ValueFactory<Binary> getBinaryFactory() {
        return this.binaryFactory;
    }

    /**
     * {@inheritDoc}
     */
    public ValueFactory<Boolean> getBooleanFactory() {
        return this.booleanFactory;
    }

    /**
     * {@inheritDoc}
     */
    public DateTimeFactory getDateFactory() {
        return this.dateFactory;
    }

    /**
     * {@inheritDoc}
     */
    public ValueFactory<BigDecimal> getDecimalFactory() {
        return this.decimalFactory;
    }

    /**
     * {@inheritDoc}
     */
    public ValueFactory<Double> getDoubleFactory() {
        return this.doubleFactory;
    }

    /**
     * {@inheritDoc}
     */
    public ValueFactory<Long> getLongFactory() {
        return this.longFactory;
    }

    /**
     * {@inheritDoc}
     */
    public NameFactory getNameFactory() {
        return this.nameFactory;
    }

    /**
     * {@inheritDoc}
     */
    public PathFactory getPathFactory() {
        return this.pathFactory;
    }

    /**
     * {@inheritDoc}
     */
    public ValueFactory<Reference> getReferenceFactory() {
        return this.referenceFactory;
    }

    /**
     * {@inheritDoc}
     */
    public ValueFactory<String> getStringFactory() {
        return this.stringFactory;
    }

    /**
     * {@inheritDoc}
     */
    public ValueFactory<URI> getUriFactory() {
        return this.uriFactory;
    }

    /**
     * {@inheritDoc}
     */
    public ValueFactory<Object> getObjectFactory() {
        return this.objectFactory;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<ValueFactory<?>> iterator() {
        return this.factories.values().iterator();
    }

    /**
     * {@inheritDoc}
     */
    public ValueFactory<?> getValueFactory( PropertyType type ) {
        ArgCheck.isNotNull(type, "type");
        return this.factories.get(type);
    }

    /**
     * {@inheritDoc}
     */
    public ValueFactory<?> getValueFactory( Object prototype ) {
        ArgCheck.isNotNull(prototype, "prototype");
        PropertyType inferredType = PropertyType.discoverType(prototype);
        assert inferredType != null;
        return this.factories.get(inferredType);
    }

}
