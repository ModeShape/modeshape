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
package org.modeshape.jcr.query.parse;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import org.modeshape.jcr.JcrValueFactory;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.query.JcrTypeSystem;
import org.modeshape.jcr.query.model.LiteralValue;
import org.modeshape.jcr.query.model.TypeSystem;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * An specialization of the {@link BasicSqlQueryParser} that uses a different language name that matches the JCR 2.0
 * specification.
 */
public class JcrSql2QueryParser extends BasicSqlQueryParser {

    public static final String LANGUAGE = Query.JCR_SQL2;

    public JcrSql2QueryParser() {
        super();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.query.parse.QueryParser#getLanguage()
     */
    @Override
    public String getLanguage() {
        return LANGUAGE;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.query.parse.BasicSqlQueryParser#literal(TypeSystem, Object)
     */
    @Override
    protected LiteralValue literal( TypeSystem typeSystem,
                                    Object value ) throws ValueFormatException {
        JcrValueFactory factory = ((JcrTypeSystem)typeSystem).getValueFactory();
        Value jcrValue = null;
        if (value instanceof String) {
            jcrValue = factory.createValue((String)value);
        } else if (value instanceof Boolean) {
            jcrValue = factory.createValue(((Boolean)value).booleanValue());
        } else if (value instanceof Binary) {
            jcrValue = factory.createValue((Binary)value);
        } else if (value instanceof DateTime) {
            jcrValue = factory.createValue(((DateTime)value).toCalendar());
        } else if (value instanceof Calendar) {
            jcrValue = factory.createValue((Calendar)value);
        } else if (value instanceof BigDecimal) {
            jcrValue = factory.createValue((BigDecimal)value);
        } else if (value instanceof Double) {
            jcrValue = factory.createValue((Double)value);
        } else if (value instanceof Long) {
            jcrValue = factory.createValue((Long)value);
        } else if (value instanceof Reference) {
            jcrValue = factory.createValue((Reference)value);
        } else if (value instanceof InputStream) {
            Binary binary = factory.createBinary((InputStream)value);
            jcrValue = factory.createValue(binary);
        } else if (value instanceof Node) {
            try {
                jcrValue = factory.createValue((Node)value);
            } catch (RepositoryException e) {
                throw new ValueFormatException(value, PropertyType.REFERENCE, e.getMessage());
            }
        } else {
            jcrValue = factory.createValue(value.toString());
        }
        return new LiteralValue(jcrValue, value);
    }

}
