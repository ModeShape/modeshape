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
package org.modeshape.graph.property;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import org.modeshape.graph.property.ValueComparators;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class StringValueComparatorTest extends AbstractValueComparatorsTest<String> {

    public StringValueComparatorTest() {
        super(ValueComparators.STRING_COMPARATOR, "valid value 1", "Valid valid 2");
    }

    @Test
    public void shouldBeCaseInsensitive() {
        assertThat(comparator.compare(validNonNullValues[0].toUpperCase(), validNonNullValues[0].toLowerCase()), is(not(0)));
    }

    @Test
    public void shouldMatchStringCompareToMethod() {
        assertThat(comparator.compare(validNonNullValues[0], validNonNullValues[1]), is(validNonNullValues[0].compareTo(validNonNullValues[1])));
        assertThat(comparator.compare(validNonNullValues[1], validNonNullValues[0]), is(validNonNullValues[1].compareTo(validNonNullValues[0])));
        assertThat(comparator.compare(validNonNullValues[1], validNonNullValues[1]), is(validNonNullValues[1].compareTo(validNonNullValues[1])));
        assertThat(comparator.compare(validNonNullValues[0], validNonNullValues[0]), is(validNonNullValues[0].compareTo(validNonNullValues[0])));
    }

    @Test
    public void shouldSortValues() {
        assertThat(getSortedValues(false), hasItems(validNonNullValues[0], validNonNullValues[1]));
    }

}
