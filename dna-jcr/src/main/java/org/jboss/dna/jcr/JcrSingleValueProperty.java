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
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.Reference;
import org.jboss.dna.graph.property.ValueFactories;

/**
 *
 */
final class JcrSingleValueProperty extends AbstractJcrProperty {

    JcrSingleValueProperty( SessionCache cache,
                            PropertyId propertyId ) {
        super(cache, propertyId);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.AbstractJcrProperty#isMultiple()
     */
    @Override
    boolean isMultiple() {
        return false;
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
     * @see javax.jcr.Property#setValue(javax.jcr.Value)
     */
    public void setValue( Value value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        JcrValue jcrValue = null;
        if (value instanceof JcrValue) {
            jcrValue = (JcrValue)value;

            // Force a conversion as per SetValueValueFormatExceptionTest in JR TCK
            jcrValue.asType(this.getType());
            
            cache.getEditorFor(propertyId.getNodeId()).setProperty(propertyId.getPropertyName(), jcrValue);
            return;
        }
        if (value == null) {
            // Then we're to delete the property ...
            cache.getEditorFor(propertyId.getNodeId()).removeProperty(propertyId.getPropertyName());
            return;
        }
        
        // We have to convert from one Value implementation to ours ...
        switch (value.getType()) {
            case PropertyType.STRING:
                setValue(value.getString());
                break;
            case PropertyType.BINARY:
                setValue(value.getStream());
                break;
            case PropertyType.BOOLEAN:
                setValue(value.getBoolean());
                break;
            case PropertyType.DATE:
                setValue(value.getDate());
                break;
            case PropertyType.DOUBLE:
                setValue(value.getDouble());
                break;
            case PropertyType.LONG:
                setValue(value.getLong());
                break;
            case PropertyType.NAME:
                setValue(value.getString());
                break;
            case PropertyType.PATH:
                setValue(value.getString());
                break;
            case PropertyType.REFERENCE:
                setValue(value.getString());
                break;
            default:
                throw new RepositoryException();
        }
    }

    protected void setValue( JcrValue jcrValue )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        assert jcrValue != null;
        cache.getEditorFor(propertyId.getNodeId()).setProperty(propertyId.getPropertyName(), jcrValue);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(java.lang.String)
     */
    public void setValue( String value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (value == null) {
            this.remove();
            return;
        }
        setValue(createValue(value, PropertyType.STRING).asType(this.getType()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(java.io.InputStream)
     */
    public void setValue( InputStream value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (value == null) {
            this.remove();
            return;
        }
        setValue(createValue(context().getValueFactories().getBinaryFactory().create(value), PropertyType.BINARY).asType(this.getType()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(long)
     */
    public void setValue( long value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        setValue(createValue(new Long(value), PropertyType.LONG).asType(this.getType()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(double)
     */
    public void setValue( double value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        setValue(createValue(new Double(value), PropertyType.DOUBLE).asType(this.getType()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(java.util.Calendar)
     */
    public void setValue( Calendar value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (value == null) {
            this.remove();
            return;
        }
        setValue(createValue(context().getValueFactories().getDateFactory().create(value), PropertyType.DATE).asType(this.getType()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(boolean)
     */
    public void setValue( boolean value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        setValue(createValue(new Boolean(value), PropertyType.BOOLEAN).asType(this.getType()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(javax.jcr.Node)
     */
    public void setValue( Node value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (value == null) {
            this.remove();
            return;
        }
        
        if (!value.isNodeType(JcrMixLexicon.REFERENCEABLE.getString(this.context().getNamespaceRegistry()))) {
            throw new ValueFormatException(JcrI18n.nodeNotReferenceable.text());
        }
        
        String uuid = value.getUUID();
        setValue(createValue(uuid, PropertyType.REFERENCE).asType(this.getType()));
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

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(javax.jcr.Value[])
     */
    public void setValue( Value[] values ) throws ValueFormatException {
        throw new ValueFormatException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(java.lang.String[])
     */
    public void setValue( String[] values ) throws ValueFormatException {
        throw new ValueFormatException();
    }
}
