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
package org.modeshape.graph.query.model;

import java.util.Set;
import net.jcip.annotations.Immutable;

/**
 * A dynamic operand used in a {@link Comparison} constraint.
 */
@Immutable
public interface DynamicOperand extends LanguageObject {
    // private static final long serialVersionUID = 1L;
    //
    // private final Set<SelectorName> selectorNames;
    //
    // /**
    // * Create a arithmetic dynamic operand that operates upon the supplied selector name(s).
    // *
    // * @param selectorNames the selector names
    // * @throws IllegalArgumentException if the selector names array is null or empty, or if any of the values are null
    // */
    // protected DynamicOperand( SelectorName... selectorNames ) {
    // CheckArg.isNotNull(selectorNames, "selectorNames");
    // if (selectorNames.length == 1) {
    // CheckArg.isNotNull(selectorNames, "selectorNames[0]");
    // this.selectorNames = Collections.singleton(selectorNames[0]);
    // } else {
    // CheckArg.isNotNull(selectorNames, "selectorNames[0]");
    // this.selectorNames = Collections.unmodifiableSet(new LinkedHashSet<SelectorName>(Arrays.asList(selectorNames)));
    // int i = 0;
    // for (SelectorName name : this.selectorNames) {
    // CheckArg.isNotNull(name, "selectorNames[" + i++ + "]");
    // }
    // }
    // }
    //
    // /**
    // * Create a arithmetic dynamic operand that operates upon the supplied selector name(s).
    // *
    // * @param selectorNames the selector names
    // * @throws IllegalArgumentException if the name list is null or empty, or if any of the values are null
    // */
    // protected DynamicOperand( Collection<SelectorName> selectorNames ) {
    // CheckArg.isNotNull(selectorNames, "selectorName");
    // this.selectorNames = Collections.unmodifiableSet(new LinkedHashSet<SelectorName>(selectorNames));
    // }
    //
    // /**
    // * Create a arithmetic dynamic operand that operates upon the selector names given by the supplied dynamic operand(s).
    // *
    // * @param operand the operand defining the selector names
    // * @throws IllegalArgumentException if the operand is null
    // */
    // protected DynamicOperand( DynamicOperand operand ) {
    // CheckArg.isNotNull(operand, "operand");
    // this.selectorNames = operand.selectorNames(); // immutable, so we can reference it directly
    // }
    //
    // /**
    // * Create a arithmetic dynamic operand that operates upon the selector names given by the supplied dynamic operand(s).
    // *
    // * @param operands the operands defining the selector names
    // * @throws IllegalArgumentException if the operand is null
    // */
    // protected DynamicOperand( Iterable<? extends DynamicOperand> operands ) {
    // CheckArg.isNotNull(operands, "operands");
    // Set<SelectorName> names = new LinkedHashSet<SelectorName>();
    // for (DynamicOperand operand : operands) {
    // names.addAll(operand.selectorNames());
    // }
    // this.selectorNames = Collections.unmodifiableSet(names);
    // }
    //
    // /**
    // * Create a arithmetic dynamic operand that operates upon the selector names given by the supplied dynamic operand(s).
    // *
    // * @param operands the operands defining the selector names
    // * @throws IllegalArgumentException if the operand is null
    // */
    // protected DynamicOperand( DynamicOperand... operands ) {
    // CheckArg.isNotNull(operands, "operands");
    // Set<SelectorName> names = new LinkedHashSet<SelectorName>();
    // for (DynamicOperand operand : operands) {
    // names.addAll(operand.selectorNames());
    // }
    // this.selectorNames = Collections.unmodifiableSet(names);
    // }

    /**
     * Get the selector symbols to which this operand applies.
     * 
     * @return the immutable ordered set of non-null selector names used by this operand; never null and never empty
     */
    public Set<SelectorName> selectorNames();
}
