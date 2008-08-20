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
package org.jboss.dna.jcr;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import org.jboss.dna.spi.ExecutionContext;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.ValueFactories;

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
        if (value instanceof Boolean) {
            jcrValue = new JcrValue<Boolean>(valueFactories, PropertyType.BOOLEAN, (Boolean)value);
        } else if (value instanceof Date) {
            jcrValue = new JcrValue<Date>(valueFactories, PropertyType.DATE, (Date)value);
        } else if (value instanceof Calendar) {
            jcrValue = new JcrValue<Calendar>(valueFactories, PropertyType.DATE, (Calendar)value);
        } else if (value instanceof Double) {
            jcrValue = new JcrValue<Double>(valueFactories, PropertyType.DOUBLE, (Double)value);
        } else if (value instanceof Float) {
            jcrValue = new JcrValue<Float>(valueFactories, PropertyType.DOUBLE, (Float)value);
        } else if (value instanceof Integer) {
            jcrValue = new JcrValue<Integer>(valueFactories, PropertyType.LONG, (Integer)value);
        } else if (value instanceof Long) {
            jcrValue = new JcrValue<Long>(valueFactories, PropertyType.LONG, (Long)value);
        } else if (value instanceof UUID) {
            jcrValue = new JcrValue<UUID>(valueFactories, PropertyType.STRING, (UUID)value);
        } else if (value instanceof String) {
            jcrValue = new JcrValue<String>(valueFactories, PropertyType.STRING, (String)value);
        } else {
            jcrValue = new JcrValue<Object>(valueFactories, PropertyType.BINARY, value);
        }
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
}
