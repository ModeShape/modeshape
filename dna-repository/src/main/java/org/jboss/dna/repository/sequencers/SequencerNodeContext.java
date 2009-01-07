/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
import java.security.AccessControlContext;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.NamespaceRegistry;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.Property;
import org.jboss.dna.graph.properties.PropertyFactory;
import org.jboss.dna.graph.properties.ValueFactories;
import org.jboss.dna.graph.sequencers.SequencerContext;
import org.jboss.dna.graph.sequencers.StreamSequencer;
import org.jboss.dna.repository.RepositoryI18n;
import org.jboss.dna.repository.mimetype.MimeType;
import org.jboss.dna.repository.util.JcrExecutionContext;

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
    private final JcrExecutionContext context;
    private final Problems problems;

    SequencerNodeContext( Node input,
                          javax.jcr.Property sequencedProperty,
                          JcrExecutionContext context,
                          Problems problems ) throws RepositoryException {
        assert input != null;
        assert sequencedProperty != null;
        assert context != null;
        assert problems != null;
        this.context = context;
        this.sequencedProperty = sequencedProperty;
        this.problems = problems;
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
     * 
     * @see org.jboss.dna.graph.ExecutionContext#getAccessControlContext()
     */
    public AccessControlContext getAccessControlContext() {
        return context.getAccessControlContext();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.component.ClassLoaderFactory#getClassLoader(java.lang.String[])
     */
    public ClassLoader getClassLoader( String... classpath ) {
        return context.getClassLoader(classpath);
    }

    /**
     * {@inheritDoc}
     */
    public ValueFactories getValueFactories() {
        return factories;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.sequencers.SequencerContext#getInputPath()
     */
    public Path getInputPath() {
        return path;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.sequencers.SequencerContext#getInputProperties()
     */
    public Set<Property> getInputProperties() {
        return props;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.sequencers.SequencerContext#getInputProperty(org.jboss.dna.graph.properties.Name)
     */
    public Property getInputProperty( Name name ) {
        CheckArg.isNotNull(name, "name");
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
     * @see org.jboss.dna.graph.sequencers.SequencerContext#getProblems()
     */
    public Problems getProblems() {
        return problems;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.sequencers.SequencerContext#getMimeType()
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
     * 
     * @see org.jboss.dna.graph.sequencers.SequencerContext#getLogger(java.lang.Class)
     */
    public Logger getLogger( Class<?> clazz ) {
        return context.getLogger(clazz);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.sequencers.SequencerContext#getLogger(java.lang.String)
     */
    public Logger getLogger( String name ) {
        return context.getLogger(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.ExecutionContext#getLoginContext()
     */
    public LoginContext getLoginContext() {
        return context.getLoginContext();
    }

    /**
     * {@inheritDoc}
     */
    public NamespaceRegistry getNamespaceRegistry() {
        return factories.getNameFactory().getNamespaceRegistry();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.ExecutionContext#getPropertyFactory()
     */
    public PropertyFactory getPropertyFactory() {
        return context.getPropertyFactory();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.ExecutionContext#getSubject()
     */
    public Subject getSubject() {
        return context.getSubject();
    }
}
