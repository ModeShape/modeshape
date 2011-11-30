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
package org.modeshape.jcr;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.UUID;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Binary;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.basic.NodeKeyReference;
import org.modeshape.jcr.value.basic.UuidReference;

/**
 * ModeShape implementation of a {@link javax.jcr.Property JCR Property} with a single value.
 * 
 * @see JcrMultiValueProperty
 */
@NotThreadSafe
final class JcrSingleValueProperty extends AbstractJcrProperty {

    JcrSingleValueProperty( AbstractJcrNode node,
                            Name name,
                            int propertyType ) {
        super(node, name, propertyType);
    }

    @Override
    public boolean isMultiple() {
        return false;
    }

    @Override
    public boolean getBoolean() throws RepositoryException {
        checkSession();
        try {
            return context().getValueFactories().getBooleanFactory().create(property().getFirstValue());
        } catch (org.modeshape.jcr.value.ValueFormatException e) {
            throw new ValueFormatException(e.getMessage(), e);
        }
    }

    @Override
    public Calendar getDate() throws RepositoryException {
        checkSession();
        try {
            return context().getValueFactories().getDateFactory().create(property().getFirstValue()).toCalendar();
        } catch (org.modeshape.jcr.value.ValueFormatException e) {
            throw new ValueFormatException(e.getMessage(), e);
        }
    }

    @Override
    public double getDouble() throws RepositoryException {
        checkSession();
        try {
            return context().getValueFactories().getDoubleFactory().create(property().getFirstValue());
        } catch (org.modeshape.jcr.value.ValueFormatException e) {
            throw new ValueFormatException(e.getMessage(), e);
        }
    }

    @Override
    public BigDecimal getDecimal() throws ValueFormatException, RepositoryException {
        checkSession();
        try {
            return context().getValueFactories().getDecimalFactory().create(property().getFirstValue());
        } catch (org.modeshape.jcr.value.ValueFormatException e) {
            throw new ValueFormatException(e.getMessage(), e);
        }
    }

    @Override
    public long getLength() throws RepositoryException {
        checkSession();
        return createValue(property().getFirstValue()).getLength();
    }

    @Override
    public long[] getLengths() throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForSingleValuedProperty.text());
    }

    @Override
    public long getLong() throws RepositoryException {
        checkSession();
        try {
            return context().getValueFactories().getLongFactory().create(property().getFirstValue());
        } catch (org.modeshape.jcr.value.ValueFormatException e) {
            throw new ValueFormatException(e.getMessage(), e);
        }
    }

    @Override
    public final Node getNode() throws ItemNotFoundException, ValueFormatException, RepositoryException {
        checkSession();
        ValueFactories factories = context().getValueFactories();
        Object value = property().getFirstValue();
        try {
            if (value instanceof Reference) {
                NodeKey key = null;
                if (value instanceof NodeKeyReference) {
                    // REFERENCE and WEAKREFERENCE values are node keys ...
                    key = ((NodeKeyReference)value).getNodeKey();
                } else if (value instanceof UuidReference) {
                    // REFERENCE and WEAKREFERENCE values should be node keys, so create a key from the
                    // supplied UUID and this node's key ...
                    UUID uuid = ((UuidReference)value).getUuid();
                    key = getParent().key().withId(uuid.toString());
                } else {
                    assert false : "Unknown type of Reference value";
                }
                return session().node(key, null);
            }
            // STRING, PATH and NAME values will be convertable to a graph Path object ...
            Path path = factories.getPathFactory().create(value);
            // We're throwing a PathNotFoundException here because that's what the TCK unit tests expect.
            // See https://issues.apache.org/jira/browse/JCR-2648 for details
            return path.isAbsolute() ? session().node(path) : session().node(getParent().node(), path);
        } catch (org.modeshape.jcr.value.ValueFormatException e) {
            throw new ValueFormatException(e.getMessage(), e);
        }
    }

    @Override
    public AbstractJcrProperty getProperty() throws ItemNotFoundException, ValueFormatException, RepositoryException {
        checkSession();
        Path path = null;
        try {
            // Convert this property to a PATH to a property ...
            path = session().pathFactory().create(property().getFirstValue());
        } catch (org.modeshape.jcr.value.ValueFormatException e) {
            throw new ValueFormatException(e.getMessage(), e);
        }
        // Find the parent node of the referenced property ...
        AbstractJcrNode referencedNode = null;
        if (path.isAbsolute()) {
            referencedNode = session().node(path);
        } else {
            referencedNode = session().node(cachedNode(), path);
        }
        // Now get the property from the referenced node ...
        Name propertyName = path.getLastSegment().getName();
        if (!referencedNode.hasProperty(propertyName)) {
            String readablePath = path.getString(namespaces());
            String workspaceName = session().workspaceName();
            String msg = null;
            if (path.isAbsolute()) {
                msg = JcrI18n.pathNotFound.text(readablePath, workspaceName);
            } else {
                msg = JcrI18n.pathNotFoundRelativeTo.text(readablePath, getParent().getPath(), workspaceName);
            }
            throw new PathNotFoundException(msg);
        }
        return referencedNode.getProperty(propertyName);
    }

    @Override
    public InputStream getStream() throws RepositoryException {
        checkSession();
        try {
            Binary binary = context().getValueFactories().getBinaryFactory().create(property().getFirstValue());
            return new SelfClosingInputStream(binary);
        } catch (org.modeshape.jcr.value.ValueFormatException e) {
            throw new ValueFormatException(e.getMessage(), e);
        }
    }

    @Override
    public javax.jcr.Binary getBinary() throws ValueFormatException, RepositoryException {
        checkSession();
        try {
            Binary binary = context().getValueFactories().getBinaryFactory().create(property().getFirstValue());
            return binary;
        } catch (org.modeshape.jcr.value.ValueFormatException e) {
            throw new ValueFormatException(e.getMessage(), e);
        }
    }

    @Override
    public String getString() throws RepositoryException {
        checkSession();
        try {
            return context().getValueFactories().getStringFactory().create(property().getFirstValue());
        } catch (org.modeshape.jcr.value.ValueFormatException e) {
            throw new ValueFormatException(e.getMessage(), e);
        }
    }

    @Override
    public JcrValue getValue() throws RepositoryException {
        checkSession();
        return createValue(property().getFirstValue());
    }

    @Override
    public void setValue( Value value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkSession();
        checkForLock();
        checkForCheckedOut();
        JcrValue jcrValue = null;

        if (value == null) {
            // Then we're to delete the property ...
            mutable().removeProperty(sessionCache(), name());
            return;
        }

        if (value instanceof JcrValue) {
            jcrValue = (JcrValue)value;
            if (jcrValue.value() == null) {
                throw new ValueFormatException(JcrI18n.valueMayNotContainNull.text(getName()));
            }

            // Force a conversion as per SetValueValueFormatExceptionTest in JR TCK
            Object literal = jcrValue.asType(this.getType()).value();
            Property newProp = session().propertyFactory().create(name(), literal);
            mutable().setProperty(sessionCache(), newProp);
            return;
        }

        // We have to convert from one Value implementation to ours ...
        switch (value.getType()) {
            case PropertyType.STRING:
                setValue(value.getString());
                break;
            case PropertyType.BINARY:
                setValue(value.getBinary());
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
            case PropertyType.DECIMAL:
                setValue(value.getDecimal());
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
                throw new RepositoryException(JcrI18n.invalidPropertyType.text(value.getType()));
        }
    }

    protected void setValue( JcrValue jcrValue )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        assert jcrValue != null;

        if (jcrValue.value() == null) {
            throw new ValueFormatException(JcrI18n.valueMayNotContainNull.text(getName()));
        }

        // Force a conversion as per SetValueValueFormatExceptionTest in JR TCK
        Object literal = jcrValue.asType(this.getType()).value();
        Property newProp = session().propertyFactory().create(name(), literal);
        mutable().setProperty(sessionCache(), newProp);
    }

    @Override
    public void setValue( String value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (value == null) {
            this.remove();
            return;
        }
        checkSession();
        checkForLock();
        checkForCheckedOut();
        setValue(createValue(value, PropertyType.STRING).asType(this.getType()));
    }

    @Override
    public void setValue( InputStream value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (value == null) {
            this.remove();
            return;
        }
        checkSession();
        checkForLock();
        checkForCheckedOut();
        setValue(createValue(context().getValueFactories().getBinaryFactory().create(value), PropertyType.BINARY).asType(this.getType()));
    }

    @Override
    public void setValue( long value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkSession();
        checkForLock();
        checkForCheckedOut();
        setValue(createValue(new Long(value), PropertyType.LONG).asType(this.getType()));
    }

    @Override
    public void setValue( double value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkSession();
        checkForLock();
        checkForCheckedOut();
        setValue(createValue(new Double(value), PropertyType.DOUBLE).asType(this.getType()));
    }

    @Override
    public void setValue( Calendar value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (value == null) {
            this.remove();
            return;
        }
        checkSession();
        checkForLock();
        checkForCheckedOut();
        setValue(createValue(context().getValueFactories().getDateFactory().create(value), PropertyType.DATE).asType(this.getType()));
    }

    @Override
    public void setValue( boolean value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkSession();
        checkForLock();
        checkForCheckedOut();
        setValue(createValue(new Boolean(value), PropertyType.BOOLEAN).asType(this.getType()));
    }

    @Override
    public void setValue( Node value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (value == null) {
            this.remove();
            return;
        }
        checkSession();
        checkForLock();
        checkForCheckedOut();

        if (!value.isNodeType(JcrMixLexicon.REFERENCEABLE.getString(this.context().getNamespaceRegistry()))) {
            throw new ValueFormatException(JcrI18n.nodeNotReferenceable.text());
        }

        String id = value.getIdentifier();
        setValue(createValue(id, PropertyType.REFERENCE).asType(this.getType()));
    }

    @Override
    public void setValue( javax.jcr.Binary value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // Get the Graph Binary object out of the value ...
        if (value == null) {
            this.remove();
            return;
        }
        checkSession();
        checkForLock();
        checkForCheckedOut();

        Binary binary = null;
        if (value instanceof Binary) {
            binary = (Binary)value;
        } else {
            // Otherwise, this isn't our instance, so copy the data ...
            binary = context().getValueFactories().getBinaryFactory().create(value.getStream(), value.getSize());
        }
        setValue(createValue(binary, PropertyType.BINARY).asType(this.getType()));
    }

    @Override
    public void setValue( BigDecimal value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (value == null) {
            this.remove();
            return;
        }
        checkSession();
        checkForLock();
        checkForCheckedOut();
        setValue(createValue(value, PropertyType.DECIMAL).asType(this.getType()));
    }

    @Override
    public JcrValue[] getValues() throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForSingleValuedProperty.text());
    }

    @Override
    public void setValue( Value[] values ) throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForSingleValuedProperty.text());
    }

    @Override
    public void setValue( String[] values ) throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForSingleValuedProperty.text());
    }

}
