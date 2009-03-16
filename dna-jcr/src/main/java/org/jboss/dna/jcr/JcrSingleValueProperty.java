/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import java.io.InputStream;
import java.util.Calendar;
import java.util.UUID;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.Reference;
import org.jboss.dna.graph.property.ValueFactories;

/**
 * @author jverhaeg
 */
final class JcrSingleValueProperty extends AbstractJcrProperty {

    JcrSingleValueProperty( SessionCache cache,
                            PropertyId propertyId ) {
        super(cache, propertyId);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getBoolean()
     */
    public boolean getBoolean() throws RepositoryException {
        try {
            return context().getValueFactories().getBooleanFactory().create(property().getFirstValue());
        } catch (org.jboss.dna.graph.property.ValueFormatException e) {
            throw new ValueFormatException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getDate()
     */
    public Calendar getDate() throws RepositoryException {
        try {
            return context().getValueFactories().getDateFactory().create(property().getFirstValue()).toCalendar();
        } catch (org.jboss.dna.graph.property.ValueFormatException e) {
            throw new ValueFormatException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getDouble()
     */
    public double getDouble() throws RepositoryException {
        try {
            return context().getValueFactories().getDoubleFactory().create(property().getFirstValue());
        } catch (org.jboss.dna.graph.property.ValueFormatException e) {
            throw new ValueFormatException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getLength()
     */
    public long getLength() throws RepositoryException {
        return createValue(property().getFirstValue()).getLength();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#getLengths()
     */
    public long[] getLengths() throws ValueFormatException {
        throw new ValueFormatException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getLong()
     */
    public long getLong() throws RepositoryException {
        try {
            return context().getValueFactories().getLongFactory().create(property().getFirstValue());
        } catch (org.jboss.dna.graph.property.ValueFormatException e) {
            throw new ValueFormatException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getNode()
     */
    public final Node getNode() throws RepositoryException {
        try {
            ValueFactories factories = context().getValueFactories();
            Reference dnaReference = factories.getReferenceFactory().create(property().getFirstValue());
            UUID uuid = factories.getUuidFactory().create(dnaReference);
            return cache.findJcrNode(uuid);
        } catch (org.jboss.dna.graph.property.ValueFormatException e) {
            throw new ValueFormatException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getStream()
     */
    public InputStream getStream() throws RepositoryException {
        try {
            Binary binary = context().getValueFactories().getBinaryFactory().create(property().getFirstValue());
            return new SelfClosingInputStream(binary);
        } catch (org.jboss.dna.graph.property.ValueFormatException e) {
            throw new ValueFormatException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getString()
     */
    public String getString() throws RepositoryException {
        try {
            return context().getValueFactories().getStringFactory().create(property().getFirstValue());
        } catch (org.jboss.dna.graph.property.ValueFormatException e) {
            throw new ValueFormatException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getValue()
     */
    public Value getValue() throws RepositoryException {
        return createValue(property().getFirstValue());
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#getValues()
     */
    public Value[] getValues() throws ValueFormatException {
        throw new ValueFormatException();
    }

    /*
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>value</code> is <code>null</code>.
     * @see javax.jcr.Property#setValue(javax.jcr.Value)
     *
    @SuppressWarnings( "fallthrough" )
    public void setValue( Value value ) throws RepositoryException {
        CheckArg.isNotNull(value, "value");
        // TODOx: Check node type constraint
        try {
            jcrValue = JcrValue.class.cast(value);
        } catch (ClassCastException error) {
            // TODOx: not sure if this is even possible
            ValueFactories valueFactories = getExecutionContext().getValueFactories();
            int type = value.getType();
            switch (type) {
                case PropertyType.BINARY: {
                    jcrValue = new JcrValue<InputStream>(valueFactories, type, value.getStream());
                    break;
                }
                case PropertyType.BOOLEAN: {
                    jcrValue = new JcrValue<Boolean>(valueFactories, type, value.getBoolean());
                    break;
                }
                case PropertyType.DATE: {
                    jcrValue = new JcrValue<Calendar>(valueFactories, type, value.getDate());
                    break;
                }
                case PropertyType.DOUBLE: {
                    jcrValue = new JcrValue<Double>(valueFactories, type, value.getDouble());
                    break;
                }
                case PropertyType.LONG: {
                    jcrValue = new JcrValue<Long>(valueFactories, type, value.getLong());
                    break;
                }
                case PropertyType.REFERENCE: {
                    try {
                        jcrValue = new JcrValue<UUID>(valueFactories, type, UUID.fromString(value.getString()));
                    } catch (IllegalArgumentException fallsThroughToString) {
                    }
                }
                case PropertyType.NAME:
                case PropertyType.PATH:
                case PropertyType.STRING: {
                    jcrValue = new JcrValue<String>(valueFactories, type, value.getString());
                    break;
                }
                default: {
                    throw new AssertionError("Unsupported PropertyType: " + value.getType());
                }
            }
        }
    }
    */
}
