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
package org.modeshape.graph.property.basic;

import java.math.BigDecimal;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import net.jcip.annotations.Immutable;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.property.BinaryFactory;
import org.modeshape.graph.property.DateTimeFactory;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.property.ReferenceFactory;
import org.modeshape.graph.property.UuidFactory;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.ValueTypeSystem;
import org.modeshape.graph.query.model.TypeSystem;

/**
 * The standard set of {@link ValueFactory value factories}.
 */
@Immutable
public class StandardValueFactories extends AbstractValueFactories {

    // This class is implemented with separate members for each factory so that the typical usage is optimized.
    private final ValueFactory<String> stringFactory;
    private final BinaryFactory binaryFactory;
    private final ValueFactory<Boolean> booleanFactory;
    private final DateTimeFactory dateFactory;
    private final ValueFactory<BigDecimal> decimalFactory;
    private final ValueFactory<Double> doubleFactory;
    private final ValueFactory<Long> longFactory;
    private final NameFactory nameFactory;
    private final PathFactory pathFactory;
    private final ReferenceFactory referenceFactory;
    private final ReferenceFactory weakReferenceFactory;
    private final ValueFactory<URI> uriFactory;
    private final UuidFactory uuidFactory;
    private final ValueFactory<Object> objectFactory;

    private final NamespaceRegistry namespaceRegistry;

    private final TypeSystem typeSystem;

    /**
     * Create a standard set of value factories, using the {@link ValueFactory#DEFAULT_DECODER default decoder}.
     * 
     * @param namespaceRegistry the namespace registry
     * @throws IllegalArgumentException if the namespace registry is null
     */
    public StandardValueFactories( NamespaceRegistry namespaceRegistry ) {
        this(namespaceRegistry, null, null);
    }

    /**
     * Create a standard set of value factories, using the supplied encoder/decoder.
     * 
     * @param namespaceRegistry the namespace registry
     * @param decoder the decoder that should be used; if null, the {@link ValueFactory#DEFAULT_DECODER default decoder} is used.
     * @param encoder the encoder that should be used; if null, the {@link ValueFactory#DEFAULT_ENCODER default encoder} is used.
     * @param extraFactories any extra factories that should be used; any factory will override the standard factories based upon
     *        the {@link ValueFactory#getPropertyType() factory's property type}.
     * @throws IllegalArgumentException if the namespace registry is null
     */
    public StandardValueFactories( NamespaceRegistry namespaceRegistry,
                                   TextDecoder decoder,
                                   TextEncoder encoder,
                                   ValueFactory<?>... extraFactories ) {
        CheckArg.isNotNull(namespaceRegistry, "namespaceRegistry");
        this.namespaceRegistry = namespaceRegistry;
        decoder = decoder != null ? decoder : ValueFactory.DEFAULT_DECODER;
        encoder = encoder != null ? encoder : ValueFactory.DEFAULT_ENCODER;
        Map<PropertyType, ValueFactory<?>> factories = new HashMap<PropertyType, ValueFactory<?>>();

        // Put the extra factories into the map first ...
        for (ValueFactory<?> factory : extraFactories) {
            if (factory == null) continue;
            factories.put(factory.getPropertyType(), factory);
        }

        // Now assign the members, using the factories in the map or (if null) the supplied default ...
        this.stringFactory = getFactory(factories, new StringValueFactory(this.namespaceRegistry, decoder, encoder));

        // The binary factory should NOT use the string factory that converts namespaces to prefixes ...
        StringValueFactory stringFactoryWithoutNamespaces = new StringValueFactory(decoder, encoder);
        this.binaryFactory = (BinaryFactory)getFactory(factories, new InMemoryBinaryValueFactory(decoder,
                                                                                                 stringFactoryWithoutNamespaces));
        this.booleanFactory = getFactory(factories, new BooleanValueFactory(decoder, this.stringFactory));
        this.dateFactory = (DateTimeFactory)getFactory(factories, new JodaDateTimeValueFactory(decoder, this.stringFactory));
        this.decimalFactory = getFactory(factories, new DecimalValueFactory(decoder, this.stringFactory));
        this.doubleFactory = getFactory(factories, new DoubleValueFactory(decoder, this.stringFactory));
        this.longFactory = getFactory(factories, new LongValueFactory(decoder, this.stringFactory));
        this.nameFactory = (NameFactory)getFactory(factories, new NameValueFactory(this.namespaceRegistry, decoder,
                                                                                   this.stringFactory));
        this.pathFactory = (PathFactory)getFactory(factories, new PathValueFactory(decoder, this.stringFactory, this.nameFactory));
        this.referenceFactory = (ReferenceFactory)getFactory(factories, new ReferenceValueFactory(decoder, this.stringFactory,
                                                                                                  false));
        this.weakReferenceFactory = (ReferenceFactory)getFactory(factories, new ReferenceValueFactory(decoder,
                                                                                                      this.stringFactory, true));
        this.uuidFactory = (UuidFactory)getFactory(factories, new UuidValueFactory(decoder, this.stringFactory));
        this.uriFactory = getFactory(factories, new UriValueFactory(this.namespaceRegistry, decoder, this.stringFactory));
        this.objectFactory = getFactory(factories, new ObjectValueFactory(decoder, this.stringFactory, this.binaryFactory));

        this.typeSystem = new ValueTypeSystem(this);
    }

    @SuppressWarnings( "unchecked" )
    private static <T> ValueFactory<T> getFactory( Map<PropertyType, ValueFactory<?>> factories,
                                                   ValueFactory<T> defaultFactory ) {
        PropertyType type = defaultFactory.getPropertyType();
        ValueFactory<?> factory = factories.get(type);
        if (factory == null) {
            factory = defaultFactory;
            factories.put(type, factory);
        }
        return (ValueFactory<T>)factory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.ValueFactories#getTypeSystem()
     */
    public TypeSystem getTypeSystem() {
        return typeSystem;
    }

    /**
     * @return namespaceRegistry
     */
    public NamespaceRegistry getNamespaceRegistry() {
        return this.namespaceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    public BinaryFactory getBinaryFactory() {
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
    public ReferenceFactory getReferenceFactory() {
        return this.referenceFactory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.ValueFactories#getWeakReferenceFactory()
     */
    public ReferenceFactory getWeakReferenceFactory() {
        return this.weakReferenceFactory;
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
     * 
     * @see org.modeshape.graph.property.ValueFactories#getUuidFactory()
     */
    public UuidFactory getUuidFactory() {
        return this.uuidFactory;
    }

    /**
     * {@inheritDoc}
     */
    public ValueFactory<Object> getObjectFactory() {
        return this.objectFactory;
    }

}
