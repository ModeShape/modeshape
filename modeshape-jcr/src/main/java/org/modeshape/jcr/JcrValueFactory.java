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
import java.util.Date;
import java.util.List;
import javax.jcr.*;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ReferenceFactory;
import org.modeshape.jcr.value.ValueFactories;

/**
 * The {@link ValueFactory} implementation for ModeShape.
 */
class JcrValueFactory implements org.modeshape.jcr.api.ValueFactory {

    private static final JcrValue[] EMPTY_ARRAY = new JcrValue[0];

    private final ValueFactories valueFactories;
    private final NamespaceRegistry namespaces;

    protected JcrValueFactory( ExecutionContext context ) {
        this.valueFactories = context.getValueFactories();
        this.namespaces = context.getNamespaceRegistry();
    }

    public JcrValue[] createValues( List<?> values,
                                    int propertyType ) throws ValueFormatException {
        CheckArg.isNotNull(values, "values");
        final int size = values.size();
        if (size == 0) return EMPTY_ARRAY;
        JcrValue[] jcrValues = new JcrValue[size];
        int count = 0;
        org.modeshape.jcr.value.ValueFactory<?> valueFactory = valueFactoryFor(propertyType);
        for (Object value : values) {
            try {
                Object objectValue = valueFactory.create(value);
                jcrValues[count++] = new JcrValue(valueFactories, propertyType, objectValue);
            } catch (org.modeshape.jcr.value.ValueFormatException vfe) {
                throw new ValueFormatException(vfe);
            }
        }
        return jcrValues;
    }

    @Override
    public JcrValue createValue( String value,
                                 int propertyType ) throws ValueFormatException {
        return new JcrValue(valueFactories, propertyType, convertValueToType(value, propertyType));
    }

    public JcrValue createValue( Object value,
                                 int propertyType ) throws ValueFormatException {
        return new JcrValue(valueFactories, propertyType, convertValueToType(value, propertyType));
    }

    @Override
    public JcrValue createValue( Node value ) throws RepositoryException {
        if (value == null) {
            return new JcrValue(valueFactories, PropertyType.REFERENCE, null);
        }
        if (!value.isNodeType(JcrMixLexicon.REFERENCEABLE.getString(namespaces))) {
            throw new RepositoryException(JcrI18n.nodeNotReferenceable.text());
        }
        Reference ref = valueFactories.getReferenceFactory().create(value.getIdentifier());
        return new JcrValue(valueFactories, PropertyType.REFERENCE, ref);
    }

    @Override
    public JcrValue createValue( Node value,
                                 boolean weak ) throws RepositoryException {
        if (value == null) {
            return new JcrValue(valueFactories, weak ? PropertyType.WEAKREFERENCE : PropertyType.REFERENCE, null);
        }
        if (!value.isNodeType(JcrMixLexicon.REFERENCEABLE.getString(namespaces))) {
            throw new RepositoryException(JcrI18n.nodeNotReferenceable.text());
        }
        ReferenceFactory factory = weak ? valueFactories.getWeakReferenceFactory() : valueFactories.getReferenceFactory();
        int refType = weak ? PropertyType.WEAKREFERENCE : PropertyType.REFERENCE;
        Reference ref = factory.create(value.getIdentifier());
        return new JcrValue(valueFactories, refType, ref);
    }

    @Override
    public JcrValue createValue( javax.jcr.Binary value ) {
        return new JcrValue(valueFactories, PropertyType.BINARY, value);
    }

    @Override
    public JcrValue createValue( InputStream value ) {
        BinaryValue binary = valueFactories.getBinaryFactory().create(value);
        return new JcrValue(valueFactories, PropertyType.BINARY, binary);
    }

    @Override
    public BinaryValue createBinary( InputStream value ) {
        return valueFactories.getBinaryFactory().create(value);
    }

    @Override
    public JcrValue createValue( Calendar value ) {
        DateTime dateTime = valueFactories.getDateFactory().create(value);
        return new JcrValue(valueFactories, PropertyType.DATE, dateTime);
    }

    @Override
    public JcrValue createValue( boolean value ) {
        return new JcrValue(valueFactories, PropertyType.BOOLEAN, value);
    }

    @Override
    public JcrValue createValue( double value ) {
        return new JcrValue(valueFactories, PropertyType.DOUBLE, value);
    }

    @Override
    public JcrValue createValue( long value ) {
        return new JcrValue(valueFactories, PropertyType.LONG, value);
    }

    @Override
    public JcrValue createValue( String value ) {
        return new JcrValue(valueFactories, PropertyType.STRING, value);
    }

    @Override
    public JcrValue createValue( BigDecimal value ) {
        return new JcrValue(valueFactories, PropertyType.DECIMAL, value);
    }

    @Override
    public BinaryValue createBinary( byte[] value )  {
        return valueFactories.getBinaryFactory().create(value);
    }

    @Override
    public JcrValue createValue( Date value ) throws ValueFormatException {
        return new JcrValue(valueFactories, PropertyType.DATE, value);
    }

    @Override
    public String createName( String localName ) {
        return valueFactories.getNameFactory().create(null, localName).getString();
    }

    @Override
    public String createName( String namespaceUri,
                              String localName ) {
        return valueFactories.getNameFactory().create(namespaceUri, localName).getString();
    }

    protected org.modeshape.jcr.value.ValueFactory<?> valueFactoryFor( int jcrPropertyType ) {
        switch (jcrPropertyType) {
            case PropertyType.BOOLEAN:
                return valueFactories.getBooleanFactory();
            case PropertyType.DATE:
                return valueFactories.getDateFactory();
            case PropertyType.NAME:
                return valueFactories.getNameFactory();
            case PropertyType.PATH:
                return valueFactories.getPathFactory();
            case PropertyType.REFERENCE:
            case PropertyType.WEAKREFERENCE:
                return valueFactories.getReferenceFactory();
            case PropertyType.DOUBLE:
                return valueFactories.getDoubleFactory();
            case PropertyType.LONG:
                return valueFactories.getLongFactory();
            case PropertyType.DECIMAL:
                return valueFactories.getDecimalFactory();
            case PropertyType.URI:
                return valueFactories.getUriFactory();

                // Anything can be converted to these types
            case PropertyType.BINARY:
                return valueFactories.getBinaryFactory();
            case PropertyType.STRING:
                return valueFactories.getStringFactory();
            case PropertyType.UNDEFINED:
                return valueFactories.getObjectFactory();
            default:
                assert false : "Unexpected JCR property type " + jcrPropertyType;
                // This should still throw an exception even if jcrPropertyType are turned off
                throw new IllegalStateException("Invalid property type " + jcrPropertyType);
        }

    }

    protected Object convertValueToType( Object value,
                                         int toType ) throws ValueFormatException {
        try {
            return valueFactoryFor(toType).create(value);
        } catch (org.modeshape.jcr.value.ValueFormatException vfe) {
            throw new ValueFormatException(vfe);
        }
    }

}
