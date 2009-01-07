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
package org.jboss.dna.jcr;

import java.io.InputStream;
import java.util.Calendar;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.PropertyDefinition;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.ValueFactories;

/**
 * @author jverhaeg
 */
final class JcrProperty extends AbstractJcrProperty {

    private JcrValue<?> jcrValue;

    JcrProperty( Node node,
                 ExecutionContext executionContext,
                 Name name,
                 Object value ) {
        super(node, executionContext, name);
        assert value != null;
        ValueFactories valueFactories = executionContext.getValueFactories();
        jcrValue = createValue(valueFactories, new ValueInfo(value), value);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getBoolean()
     */
    public boolean getBoolean() throws RepositoryException {
        return jcrValue.getBoolean();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getDate()
     */
    public Calendar getDate() throws RepositoryException {
        return jcrValue.getDate();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getDefinition()
     */
    public PropertyDefinition getDefinition() {
        return new AbstractJcrPropertyDefinition() {

            public boolean isMultiple() {
                return false;
            }
        };
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getDouble()
     */
    public double getDouble() throws RepositoryException {
        return jcrValue.getDouble();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getLength()
     */
    public long getLength() throws RepositoryException {
        return jcrValue.getLength();
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
        return jcrValue.getLong();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getStream()
     */
    public InputStream getStream() throws RepositoryException {
        return jcrValue.getStream();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getString()
     */
    public String getString() throws RepositoryException {
        return jcrValue.getString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getType()
     */
    public int getType() {
        return jcrValue.getType();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getValue()
     */
    public Value getValue() {
        return jcrValue;
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
