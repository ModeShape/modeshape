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
package org.modeshape.jcr.query;

import javax.jcr.PropertyType;
import javax.jcr.Value;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.query.model.Literal;
import org.modeshape.graph.query.model.Visitable;
import org.modeshape.jcr.query.qom.JcrLiteral;

/**
 * 
 */
public class Visitors extends org.modeshape.graph.query.model.Visitors {

    protected static final char OPEN_SQUARE = org.modeshape.graph.query.model.Visitors.OPEN_SQUARE;
    protected static final char CLOSE_SQUARE = org.modeshape.graph.query.model.Visitors.CLOSE_SQUARE;
    protected static final char DOUBLE_QUOTE = org.modeshape.graph.query.model.Visitors.DOUBLE_QUOTE;
    protected static final char SINGLE_QUOTE = org.modeshape.graph.query.model.Visitors.SINGLE_QUOTE;

    /**
     * Using a visitor, obtain the readable string representation of the supplied {@link Visitable object}
     * 
     * @param visitable the visitable
     * @return the string representation
     */
    public static String readable( Visitable visitable ) {
        // return visit(visitable, new ReadableVisitor()).getString();
        return visit(visitable, new JcrSql2Writer(DEFAULT_CONTEXT)).getString();
    }

    /**
     * Using a visitor, obtain the readable string representation of the supplied {@link Visitable object}
     * 
     * @param visitable the visitable
     * @param context the execution context in which the visitable should be converted to a string
     * @return the string representation
     */
    public static String readable( Visitable visitable,
                                   ExecutionContext context ) {
        // return visit(visitable, new ReadableVisitor()).getString();
        return visit(visitable, new JcrSql2Writer(context)).getString();
    }

    public static class JcrSql2Writer extends org.modeshape.graph.query.model.Visitors.JcrSql2Writer {
        public JcrSql2Writer( ExecutionContext context ) {
            super(context);
        }

        @Override
        protected JcrSql2Writer append( String string ) {
            return (JcrSql2Writer)super.append(string);
        }

        @Override
        public void visit( Literal literal ) {
            if (literal instanceof JcrLiteral) {
                JcrLiteral literalValue = (JcrLiteral)literal;
                Value value = literalValue.getLiteralValue();
                String typeName = null;
                ValueFactories factories = context.getValueFactories();
                switch (value.getType()) {
                    case PropertyType.UNDEFINED:
                    case PropertyType.STRING:
                        append(SINGLE_QUOTE);
                        String str = factories.getStringFactory().create(literalValue.value());
                        append(str);
                        append(SINGLE_QUOTE);
                        return;
                    case PropertyType.PATH:
                        append("CAST(");
                        append(factories.getPathFactory().create(literalValue.value()));
                        append(" AS ").append(PropertyType.TYPENAME_PATH.toUpperCase()).append(')');
                        return;
                    case PropertyType.NAME:
                        append("CAST(");
                        append(factories.getNameFactory().create(literalValue.value()));
                        append(" AS ").append(PropertyType.TYPENAME_NAME.toUpperCase()).append(')');
                        return;
                    case PropertyType.REFERENCE:
                        typeName = PropertyType.TYPENAME_REFERENCE;
                        break;
                    case PropertyType.WEAKREFERENCE:
                        typeName = PropertyType.TYPENAME_WEAKREFERENCE;
                        break;
                    case PropertyType.BINARY:
                        typeName = PropertyType.TYPENAME_BINARY;
                        break;
                    case PropertyType.BOOLEAN:
                        typeName = PropertyType.TYPENAME_BOOLEAN;
                        break;
                    case PropertyType.DATE:
                        typeName = PropertyType.TYPENAME_DATE;
                        break;
                    case PropertyType.DECIMAL:
                        typeName = PropertyType.TYPENAME_DECIMAL;
                        break;
                    case PropertyType.DOUBLE:
                        typeName = PropertyType.TYPENAME_DOUBLE;
                        break;
                    case PropertyType.LONG:
                        typeName = PropertyType.TYPENAME_LONG;
                        break;
                    case PropertyType.URI:
                        typeName = PropertyType.TYPENAME_URI;
                        break;
                }
                assert typeName != null;
                String str = factories.getStringFactory().create(literalValue.value());
                append("CAST('").append(str).append("' AS ").append(typeName.toUpperCase()).append(')');
            } else {
                super.visit(literal);
            }
        }
    }
}
