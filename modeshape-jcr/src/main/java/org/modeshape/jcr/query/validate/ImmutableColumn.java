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
package org.modeshape.jcr.query.validate;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.collection.Collections;
import org.modeshape.jcr.api.query.qom.Operator;
import org.modeshape.jcr.query.validate.Schemata.Column;
import org.modeshape.jcr.value.PropertyType;

@Immutable
public class ImmutableColumn implements Column {

    public static final Set<Operator> ALL_OPERATORS = Collections.unmodifiableSet(EnumSet.allOf(Operator.class));
    public static final Set<Operator> NO_OPERATORS = Collections.unmodifiableSet(EnumSet.noneOf(Operator.class));

    public static final PropertyType DEFAULT_REQUIRED_TYPE = PropertyType.STRING;
    public static final boolean DEFAULT_FULL_TEXT_SEARCHABLE = false;
    public static final boolean DEFAULT_ORDERABLE = true;

    private final boolean fullTextSearchable;
    private final boolean orderable;
    private final boolean comparable;
    private final String name;
    private final String typeName;
    private final PropertyType requiredType;
    private final Set<Operator> operators;
    private final Object minimumValue;
    private final Object maximumValue;

    protected ImmutableColumn( String name,
                               String type,
                               PropertyType requiredType ) {
        this(name, type, requiredType, DEFAULT_FULL_TEXT_SEARCHABLE, DEFAULT_ORDERABLE, null, null, ALL_OPERATORS);
    }

    protected ImmutableColumn( String name,
                               String type ) {
        this(name, type, DEFAULT_REQUIRED_TYPE, DEFAULT_FULL_TEXT_SEARCHABLE, DEFAULT_ORDERABLE, null, null, ALL_OPERATORS);
    }

    protected ImmutableColumn( String name,
                               String type,
                               PropertyType requiredType,
                               boolean fullTextSearchable ) {
        this(name, type, requiredType, fullTextSearchable, DEFAULT_ORDERABLE, null, null, ALL_OPERATORS);
    }

    protected ImmutableColumn( String name,
                               String type,
                               PropertyType requiredType,
                               boolean fullTextSearchable,
                               boolean orderable,
                               boolean canContainReferences,
                               Object minimum,
                               Object maximum,
                               Operator... operators ) {
        this(name, type, requiredType, fullTextSearchable, orderable, minimum, maximum,
             operators != null && operators.length != 0 ? EnumSet.copyOf(Arrays.asList(operators)) : null);
    }

    protected ImmutableColumn( String name,
                               String type,
                               PropertyType requiredType,
                               boolean fullTextSearchable,
                               boolean orderable,
                               Object minimum,
                               Object maximum,
                               Set<Operator> operators ) {
        this.name = name;
        this.typeName = type;
        this.requiredType = requiredType;
        this.fullTextSearchable = fullTextSearchable;
        this.orderable = orderable;
        this.operators = operators == null || operators.isEmpty() ? ALL_OPERATORS : Collections.unmodifiableSet(EnumSet.copyOf(operators));
        this.minimumValue = minimum;
        this.maximumValue = maximum;
        if (this.operators.isEmpty()) {
            this.comparable = false;
        } else {
            boolean comparable = this.requiredType == PropertyType.STRING;
            if (!comparable) {
                // See if the operators expect comparable ...
                for (Operator operator : this.operators) {
                    if (operator == Operator.GREATER_THAN || operator == Operator.GREATER_THAN_OR_EQUAL_TO
                        || operator == Operator.LESS_THAN || operator == Operator.LESS_THAN_OR_EQUAL_TO) {
                        comparable = true;
                        break;
                    }
                }
            }
            this.comparable = comparable;
        }
        assert this.name != null;
        assert this.typeName != null;
        assert this.operators != null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPropertyTypeName() {
        return typeName;
    }

    @Override
    public boolean isFullTextSearchable() {
        return fullTextSearchable;
    }

    @Override
    public boolean isOrderable() {
        return orderable;
    }

    @Override
    public boolean isComparable() {
        return comparable;
    }

    @Override
    public Object getMinimum() {
        return minimumValue;
    }

    @Override
    public Object getMaximum() {
        return maximumValue;
    }

    @Override
    public PropertyType getRequiredType() {
        return requiredType;
    }

    @Override
    public Set<Operator> getOperators() {
        return operators;
    }

    @Override
    public String toString() {
        return this.name + "(" + typeName + ")";
    }
}
