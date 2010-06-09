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
package org.modeshape.jcr.query.qom;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;
import javax.jcr.Binary;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.query.qom.Literal;
import javax.jcr.query.qom.StaticOperand;
import org.modeshape.graph.query.model.FullTextSearch;
import org.modeshape.graph.query.model.SelectorName;

/**
 * Implementation of the full-text search constraint for the JCR Query Object Model and the Graph API.
 */
public class JcrFullTextSearch extends FullTextSearch implements javax.jcr.query.qom.FullTextSearch, JcrConstraint {

    private static final long serialVersionUID = 1L;

    private final StaticOperand fullTextSearchOperand;

    protected static String toString( StaticOperand operand ) throws RepositoryException {
        if (operand instanceof Literal) {
            return ((Literal)operand).getLiteralValue().getString();
        }
        return operand.toString();
    }

    /**
     * Create a constraint defining a full-text search against the property values on node within the search scope.
     * 
     * @param selectorName the name of the node selector defining the search scope
     * @param propertyName the name of the property to be searched; may be null if all property values are to be searched
     * @param fullTextSearchExpression the search expression
     * @throws RepositoryException if there is an error converting the full text search expression to a string
     */
    public JcrFullTextSearch( SelectorName selectorName,
                              String propertyName,
                              StaticOperand fullTextSearchExpression ) throws RepositoryException {
        super(selectorName, propertyName, toString(fullTextSearchExpression));
        this.fullTextSearchOperand = fullTextSearchExpression;
    }

    /**
     * Create a constraint defining a full-text search against the property values on node within the search scope.
     * 
     * @param selectorName the name of the node selector defining the search scope
     * @param propertyName the name of the property to be searched; may be null if all property values are to be searched
     * @param fullTextSearchExpression the search expression
     * @param term the term, if known; may be null if the term representation is to be computed
     */
    public JcrFullTextSearch( SelectorName selectorName,
                              String propertyName,
                              final String fullTextSearchExpression,
                              Term term ) {
        super(selectorName, propertyName, fullTextSearchExpression, term);
        this.fullTextSearchOperand = new JcrLiteral(new Value() {

            @Override
            public int getType() {
                return PropertyType.STRING;
            }

            @Override
            public String getString() {
                return fullTextSearchExpression;
            }

            @Override
            public InputStream getStream() throws RepositoryException {
                throw new ValueFormatException();
            }

            @Override
            public long getLong() throws ValueFormatException, RepositoryException {
                throw new ValueFormatException();
            }

            @Override
            public double getDouble() throws ValueFormatException, RepositoryException {
                throw new ValueFormatException();
            }

            @Override
            public BigDecimal getDecimal() throws ValueFormatException, RepositoryException {
                throw new ValueFormatException();
            }

            @Override
            public Calendar getDate() throws ValueFormatException, RepositoryException {
                throw new ValueFormatException();
            }

            @Override
            public boolean getBoolean() throws ValueFormatException, RepositoryException {
                throw new ValueFormatException();
            }

            @Override
            public Binary getBinary() throws RepositoryException {
                throw new ValueFormatException();
            }
        }, fullTextSearchExpression);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.PropertyValue#getPropertyName()
     */
    @Override
    public String getPropertyName() {
        return propertyName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.FullTextSearch#getFullTextSearchExpression()
     */
    @Override
    public StaticOperand getFullTextSearchExpression() {
        return fullTextSearchOperand;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.PropertyValue#getSelectorName()
     */
    @Override
    public String getSelectorName() {
        return selectorName().name();
    }
}
