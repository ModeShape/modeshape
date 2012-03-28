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
import java.util.Iterator;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.ValueFactories;

/**
 * A {@link javax.jcr.Property JCR Property} implementation that has multiple values.
 * 
 * @see JcrSingleValueProperty
 */
@NotThreadSafe
final class JcrMultiValueProperty extends AbstractJcrProperty {

    static final JcrValue[] EMPTY_VALUES = new JcrValue[] {};

    JcrMultiValueProperty( AbstractJcrNode node,
                           Name name,
                           int propertyType ) {
        super(node, name, propertyType);
    }

    @Override
    public boolean isMultiple() {
        return true;
    }

    @Override
    public boolean getBoolean() throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    @Override
    public Calendar getDate() throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    @Override
    public double getDouble() throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    @Override
    public Node getNode() throws ValueFormatException, RepositoryException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    @Override
    public long getLength() throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    @Override
    public long[] getLengths() throws RepositoryException {
        checkSession();
        JcrValue[] values = getValues();
        long[] lengths = new long[values.length];
        int ndx = 0;
        for (JcrValue value : values) {
            lengths[ndx++] = value.getLength();
        }
        return lengths;
    }

    @Override
    public long getLong() throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    @Override
    public InputStream getStream() throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    @Override
    public String getString() throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Per the JCR specification, these values need to be created each time this method is called, since the Value cannot be used
     * after {@link Value#getStream()} is called/processed. The spec says that the client simply needs to obtain a new Value (or
     * {@link #getValues()} for {@link JcrMultiValueProperty multi-valued properites}).
     * </p>
     * 
     * @see javax.jcr.Property#getValues()
     */
    @Override
    public JcrValue[] getValues() throws RepositoryException {
        checkSession();
        Property innerProp = property();
        JcrValue[] values = new JcrValue[innerProp.size()];
        Iterator<?> iter = innerProp.iterator();
        for (int ndx = 0; iter.hasNext(); ndx++) {
            values[ndx] = createValue(iter.next());
        }
        return values;
    }

    @Override
    public final void setValue( Value[] values )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {

        if (values == null) {
            this.remove();
            return;
        }
        checkSession();
        checkForLock();
        checkForCheckedOut();

        Object[] literals = new Object[values.length];
        ValueFactories factories = null;
        for (int i = 0; i < values.length; i++) {
            Value value = values[i];
            if (value != null) {
                JcrValue jcrValue = null;
                int type = getType();
                if (value instanceof JcrValue) {
                    // This is ModeShape's implementation (created with the session's ValueFactory)
                    jcrValue = (JcrValue)value;
                    if (jcrValue.value() == null) {
                        throw new ValueFormatException(JcrI18n.valueMayNotContainNull.text(getName()));
                    }
                } else {
                    // This is a non-ModeShape Value object (from another implementation), so we need to replace it
                    // with our own implementation. The easiest way to do this is to create a wrapper JcrValue object
                    // and simply force the conversion ...
                    if (factories == null) factories = context().getValueFactories();
                    jcrValue = new JcrValue(factories, value).asType(type, true);
                }
                // Force a conversion (iff the types don't match) as per SetValueValueFormatExceptionTest in JR TCK
                literals[i] = jcrValue.asType(type, false).value();
            } else {
                literals[i] = null;
            }
        }

        Property newProperty = propertyFactory().create(name(), literals);
        mutable().setProperty(sessionCache(), newProperty);
    }

    @Override
    public final void setValue( String[] values )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {

        if (values == null) {
            this.remove();
            return;
        }
        checkSession();
        checkForLock();
        checkForCheckedOut();

        Property newProperty = null;
        if (values.length != 0) {
            int numValues = values.length;
            Object[] literals = new Object[numValues];
            for (int i = 0; i != numValues; ++i) {
                String value = values[i];
                if (value == null) {
                    literals[i] = null;
                } else {
                    JcrValue jcrValue = createValue(values[i], PropertyType.STRING).asType(this.getType());
                    literals[i] = jcrValue.value();
                }
            }
            newProperty = propertyFactory().create(name(), literals);
        }

        if (newProperty == null) {
            // must be empty ...
            newProperty = propertyFactory().create(name());
        }
        mutable().setProperty(sessionCache(), newProperty);
    }

    @Override
    public JcrValue getValue() throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    @Override
    public final void setValue( Value value ) throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    @Override
    public final void setValue( String value ) throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    @Override
    public final void setValue( InputStream value ) throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    @Override
    public final void setValue( long value ) throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    @Override
    public final void setValue( double value ) throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    @Override
    public final void setValue( Calendar value ) throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    @Override
    public final void setValue( boolean value ) throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    @Override
    public final void setValue( Node value ) throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    @Override
    public Binary getBinary() throws ValueFormatException, RepositoryException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    @Override
    public BigDecimal getDecimal() throws ValueFormatException, RepositoryException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    @Override
    public javax.jcr.Property getProperty() throws ValueFormatException, RepositoryException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    @Override
    public void setValue( BigDecimal value ) throws ValueFormatException, RepositoryException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    @Override
    public void setValue( Binary value ) throws ValueFormatException, RepositoryException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }
}
