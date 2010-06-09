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

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import org.modeshape.graph.query.model.Literal;

/**
 * Implementation of the literal value static operand for the JCR Query Object Model and the Graph API.
 */
public class JcrLiteral extends Literal implements javax.jcr.query.qom.Literal, JcrStaticOperand {

    private static final long serialVersionUID = 1L;

    private final Value jcrValue;

    public static Object rawValue( Value value ) throws RepositoryException {
        switch (value.getType()) {
            case PropertyType.BINARY:
                return value.getBinary();
            case PropertyType.BOOLEAN:
                return value.getBoolean();
            case PropertyType.DATE:
                return value.getDate();
            case PropertyType.DECIMAL:
                return value.getDecimal();
            case PropertyType.DOUBLE:
                return value.getDouble();
            case PropertyType.LONG:
                return value.getLong();
            case PropertyType.NAME:
            case PropertyType.PATH:
            case PropertyType.REFERENCE:
            case PropertyType.STRING:
            case PropertyType.URI:
            case PropertyType.WEAKREFERENCE:
            default:
                return value.getString();
        }
    }

    /**
     * @param value the JCR value
     * @throws RepositoryException if there is a problem obtaining the raw value from the supplied JCR value.
     */
    public JcrLiteral( Value value ) throws RepositoryException {
        super(rawValue(value));
        this.jcrValue = value;
    }

    public JcrLiteral( Value value,
                       Object rawValue ) {
        super(rawValue);
        this.jcrValue = value;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.Literal#getLiteralValue()
     */
    @Override
    public Value getLiteralValue() {
        return jcrValue;
    }
}
