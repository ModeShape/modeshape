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
package org.jboss.dna.repository.sequencers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.repository.RepositoryI18n;
import org.jboss.dna.repository.mimetype.MimeType;
import org.jboss.dna.repository.util.ExecutionContext;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.NamespaceRegistry;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.ValueFactories;
import org.jboss.dna.spi.sequencers.SequencerContext;
import org.jboss.dna.spi.sequencers.StreamSequencer;

/**
 * Contains context information that is passed to {@link StreamSequencer stream sequencers}, including information about the input
 * node containing the data being sequenced.
 * 
 * @author John Verhaeg
 */
@Immutable
public class SequencerNodeContext implements SequencerContext {

    private final javax.jcr.Property sequencedProperty;
    private final ValueFactories factories;
    private final Path path;
    private final Set<Property> props;

    SequencerNodeContext( Node input,
                          javax.jcr.Property sequencedProperty,
                          ExecutionContext context ) throws RepositoryException {
        assert input != null;
        assert sequencedProperty != null;
        assert context != null;
        this.sequencedProperty = sequencedProperty;
        this.factories = context.getValueFactories();
        // Translate JCR path and property values to DNA constructs and cache them to improve performance and prevent
        // RepositoryException from being thrown by getters
        // Note: getMimeType() will still operate lazily, and thus throw a SequencerException, since it is very intrusive and
        // potentially slow-running.
        path = factories.getPathFactory().create(input.getPath());
        Set<Property> props = new HashSet<Property>();
        for (PropertyIterator iter = input.getProperties(); iter.hasNext();) {
            javax.jcr.Property jcrProp = iter.nextProperty();
            Property prop;
            if (jcrProp.getDefinition().isMultiple()) {
                Value[] jcrVals = jcrProp.getValues();
                Object[] vals = new Object[jcrVals.length];
                int ndx = 0;
                for (Value jcrVal : jcrVals) {
                    vals[ndx++] = convert(factories, jcrProp.getName(), jcrVal);
                }
                prop = context.getPropertyFactory().create(factories.getNameFactory().create(jcrProp.getName()), vals);
            } else {
                Value jcrVal = jcrProp.getValue();
                Object val = convert(factories, jcrProp.getName(), jcrVal);
                prop = context.getPropertyFactory().create(factories.getNameFactory().create(jcrProp.getName()), val);
            }
            props.add(prop);
        }
        this.props = Collections.unmodifiableSet(props);
    }

    private Object convert( ValueFactories factories,
                            String name,
                            Value jcrValue ) throws RepositoryException {
        switch (jcrValue.getType()) {
            case PropertyType.BINARY: {
                return factories.getBinaryFactory().create(jcrValue.getStream());
            }
            case PropertyType.BOOLEAN: {
                return factories.getBooleanFactory().create(jcrValue.getBoolean());
            }
            case PropertyType.DATE: {
                return factories.getDateFactory().create(jcrValue.getDate());
            }
            case PropertyType.DOUBLE: {
                return factories.getDoubleFactory().create(jcrValue.getDouble());
            }
            case PropertyType.LONG: {
                return factories.getLongFactory().create(jcrValue.getLong());
            }
            case PropertyType.NAME: {
                return factories.getNameFactory().create(jcrValue.getString());
            }
            case PropertyType.PATH: {
                return factories.getPathFactory().create(jcrValue.getString());
            }
            case PropertyType.REFERENCE: {
                return factories.getReferenceFactory().create(jcrValue.getString());
            }
            case PropertyType.STRING: {
                return factories.getStringFactory().create(jcrValue.getString());
            }
            default: {
                throw new RepositoryException(RepositoryI18n.unknownPropertyValueType.text(name, jcrValue.getType()));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public ValueFactories getFactories() {
        return factories;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.sequencers.SequencerContext#getInputPath()
     */
    public Path getInputPath() {
        return path;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.sequencers.SequencerContext#getInputProperties()
     */
    public Set<Property> getInputProperties() {
        return props;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.sequencers.SequencerContext#getInputProperty(org.jboss.dna.spi.graph.Name)
     */
    public Property getInputProperty( Name name ) {
        ArgCheck.isNotNull(name, "name");
        for (Property prop : props) {
            if (name.equals(prop.getName())) {
                return prop;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.sequencers.SequencerContext#getMimeType()
     */
    @SuppressWarnings( "null" )
    // The need for the SuppressWarnings looks like an Eclipse bug
    public String getMimeType() {
        SequencerException err = null;
        String mimeType = null;
        InputStream stream = null;
        try {
            stream = sequencedProperty.getStream();
            mimeType = MimeType.of(path.getLastSegment().getName().getLocalName(), stream);
            return mimeType;
        } catch (Exception error) {
            err = new SequencerException(error);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException error) {
                    // Only throw exception if an exception was not already thrown
                    if (err == null) err = new SequencerException(error);
                }
            }
        }
        if (err != null) throw err;
        return mimeType;
    }

    /**
     * {@inheritDoc}
     */
    public NamespaceRegistry getNamespaceRegistry() {
        return factories.getNameFactory().getNamespaceRegistry();
    }
}
