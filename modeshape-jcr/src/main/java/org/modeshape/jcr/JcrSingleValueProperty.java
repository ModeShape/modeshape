/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;
import javax.jcr.AccessDeniedException;
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
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Property;

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
        Object value = property().getFirstValue();
        return valueToNode(value);
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
        try {
            referencedNode = path.isAbsolute() ? session().node(path) :session().node(cachedNode(), path);
        } catch (PathNotFoundException e) {
            //expected by the TCK
            throw new ItemNotFoundException(e.getMessage(), e);
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
            BinaryValue binary = context().getValueFactories().getBinaryFactory().create(property().getFirstValue());
            return new SelfClosingInputStream(binary);
        } catch (org.modeshape.jcr.value.ValueFormatException e) {
            throw new ValueFormatException(e.getMessage(), e);
        }
    }

    @Override
    public javax.jcr.Binary getBinary() throws ValueFormatException, RepositoryException {
        checkSession();
        try {
            return context().getValueFactories().getBinaryFactory().create(property().getFirstValue());
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
            case PropertyType.WEAKREFERENCE:
            case org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE:
                setValue(value.getString());
                break;
            default:
                throw new RepositoryException(JcrI18n.invalidPropertyType.text(value.getType()));
        }
    }

    protected void internalSetValue( JcrValue jcrValue )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        assert jcrValue != null;

        if (jcrValue.value() == null) {
            throw new ValueFormatException(JcrI18n.valueMayNotContainNull.text(getName()));
        }

        if (session.cache().isReadOnly()) {
            //expected by the tck
            throw new AccessDeniedException();
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
        internalSetValue(createValue(value, PropertyType.STRING).asType(this.getType()));
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
        internalSetValue(createValue(context().getValueFactories().getBinaryFactory().create(value), PropertyType.BINARY).asType(this.getType()));
    }

    @Override
    public void setValue( long value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkSession();
        checkForLock();
        checkForCheckedOut();
        internalSetValue(createValue(value, PropertyType.LONG).asType(this.getType()));
    }

    @Override
    public void setValue( double value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkSession();
        checkForLock();
        checkForCheckedOut();
        internalSetValue(createValue(value, PropertyType.DOUBLE).asType(this.getType()));
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
        internalSetValue(createValue(context().getValueFactories().getDateFactory().create(value), PropertyType.DATE).asType(this.getType()));
    }

    @Override
    public void setValue( boolean value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkSession();
        checkForLock();
        checkForCheckedOut();
        internalSetValue(createValue(value, PropertyType.BOOLEAN).asType(this.getType()));
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

        AbstractJcrNode jcrNode = session.getNodeByIdentifier(value.getIdentifier());
        if (!jcrNode.isInTheSameProcessAs(session.context().getProcessId())) {
            throw new RepositoryException(JcrI18n.nodeNotInTheSameSession.text(jcrNode.path()));
        }
        JcrValue referenceValue = session().valueFactory().createValue(jcrNode);
        internalSetValue(referenceValue.asType(this.getType()));
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

        BinaryValue binary = null;
        if (value instanceof BinaryValue) {
            binary = (BinaryValue)value;
        } else {
            // Otherwise, this isn't our instance, so copy the data ...
            binary = context().getValueFactories().getBinaryFactory().create(value.getStream());
        }
        internalSetValue(createValue(binary, PropertyType.BINARY).asType(this.getType()));
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
        internalSetValue(createValue(value, PropertyType.DECIMAL).asType(this.getType()));
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

    @Override
    public <T> T getAs( Class<T> type ) throws ValueFormatException, RepositoryException {
        if (type.isArray()) {
            throw new ValueFormatException(JcrI18n.unableToConvertPropertyValueToType.text(getPath(), type.getSimpleName()));
        }
        return super.getAs(type, 0);
    }
}
