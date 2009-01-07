/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.properties;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import org.jboss.dna.graph.properties.ValueComparators;
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
