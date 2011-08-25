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
import java.util.List;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Reference;
import org.modeshape.graph.property.ReferenceFactory;
import org.modeshape.graph.property.ValueFactories;

/**
 * The {@link ValueFactory} implementation for ModeShape.
 */
class JcrValueFactory implements ValueFactory {

    private static final JcrValue[] EMPTY_ARRAY = new JcrValue[0];

    private final ValueFactories valueFactories;
    private final SessionCache sessionCache;
    private final NamespaceRegistry namespaces;

    protected JcrValueFactory( JcrSession session ) {
        this.valueFactories = session.getExecutionContext().getValueFactories();
        this.sessionCache = session.cache();
        this.namespaces = session.namespaces();
    }

    public JcrValue[] createValues( List<?> values,
                                    int propertyType ) throws ValueFormatException {
        final int size = values.size();
        if (size == 0) return EMPTY_ARRAY;
        JcrValue[] jcrValues = new JcrValue[size];
        int count = 0;
        org.modeshape.graph.property.ValueFactory<?> valueFactory = valueFactoryFor(propertyType);
        for (Object value : values) {
            try {
                Object objectValue = valueFactory.create(value);
                jcrValues[count++] = new JcrValue(valueFactories, sessionCache, propertyType, objectValue);
            } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                throw new ValueFormatException(vfe);
            }
        }
        return jcrValues;
    }

    public JcrValue createValue( String value,
                                 int propertyType ) throws ValueFormatException {
        return new JcrValue(valueFactories, sessionCache, propertyType, convertValueToType(value, propertyType));
    }

    public JcrValue createValue( Node value ) throws RepositoryException {
        if (!value.isNodeType(JcrMixLexicon.REFERENCEABLE.getString(namespaces))) {
            throw new RepositoryException(JcrI18n.nodeNotReferenceable.text());
        }
        Reference ref = valueFactories.getReferenceFactory().create(value.getIdentifier());
        return new JcrValue(valueFactories, sessionCache, PropertyType.REFERENCE, ref);
    }

    public JcrValue createValue( Node value,
                                 boolean weak ) throws RepositoryException {
        if (!value.isNodeType(JcrMixLexicon.REFERENCEABLE.getString(namespaces))) {
            throw new RepositoryException(JcrI18n.nodeNotReferenceable.text());
        }
        ReferenceFactory factory = weak ? valueFactories.getWeakReferenceFactory() : valueFactories.getReferenceFactory();
        int refType = weak ? PropertyType.WEAKREFERENCE : PropertyType.REFERENCE;
        Reference ref = factory.create(value.getIdentifier());
        return new JcrValue(valueFactories, sessionCache, refType, ref);
    }

    public JcrValue createValue( javax.jcr.Binary value ) {
        return new JcrValue(valueFactories, sessionCache, PropertyType.BINARY, value);
    }

    public JcrValue createValue( InputStream value ) {
        Binary binary = valueFactories.getBinaryFactory().create(value);
        return new JcrValue(valueFactories, sessionCache, PropertyType.BINARY, binary);
    }

    public JcrBinary createBinary( InputStream value ) {
        Binary binary = valueFactories.getBinaryFactory().create(value);
        return new JcrBinary(binary);
    }

    public JcrValue createValue( Calendar value ) {
        DateTime dateTime = valueFactories.getDateFactory().create(value);
        return new JcrValue(valueFactories, sessionCache, PropertyType.DATE, dateTime);
    }

    public JcrValue createValue( boolean value ) {
        return new JcrValue(valueFactories, sessionCache, PropertyType.BOOLEAN, value);
    }

    public JcrValue createValue( double value ) {
        return new JcrValue(valueFactories, sessionCache, PropertyType.DOUBLE, value);
    }

    public JcrValue createValue( long value ) {
        return new JcrValue(valueFactories, sessionCache, PropertyType.LONG, value);
    }

    public JcrValue createValue( String value ) {
        return new JcrValue(valueFactories, sessionCache, PropertyType.STRING, value);
    }

    public JcrValue createValue( BigDecimal value ) {
        return new JcrValue(valueFactories, sessionCache, PropertyType.DECIMAL, value);
    }

    protected org.modeshape.graph.property.ValueFactory<?> valueFactoryFor( int jcrPropertyType ) {
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
        } catch (org.modeshape.graph.property.ValueFormatException vfe) {
            throw new ValueFormatException(vfe);
        }
    }

}
