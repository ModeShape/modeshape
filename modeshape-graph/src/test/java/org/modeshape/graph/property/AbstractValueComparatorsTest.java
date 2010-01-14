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
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.property.basic.SimpleNamespaceRegistry;
import org.modeshape.graph.property.basic.StandardValueFactories;
import org.junit.Test;

/**
 * @author Randall Hauch
 * @param <T>
 */
public abstract class AbstractValueComparatorsTest<T> {

    public static final NamespaceRegistry NAMESPACE_REGISTRY = new SimpleNamespaceRegistry();
    public static final ValueFactories VALUE_FACTORIES = new StandardValueFactories(NAMESPACE_REGISTRY);

    protected final Comparator<T> comparator;
    protected final T[] validNonNullValues;

    protected AbstractValueComparatorsTest( Comparator<T> comparator,
                                            T... validNonNullValues ) {
        CheckArg.isNotNull(comparator, "comparator");
        CheckArg.isNotEmpty(validNonNullValues, "validNonNullValues");
        this.comparator = comparator;
        this.validNonNullValues = validNonNullValues;
    }

    protected Comparator<T> getComparator() {
        return this.comparator;
    }

    @Test
    public void shouldConsiderNullLessThanValidNonNull() {
        assertThat(comparator.compare(validNonNullValues[0], null) > 0, is(true));
        assertThat(comparator.compare(null, validNonNullValues[0]) < 0, is(true));
    }

    @Test
    public void shouldConsiderTwoNullsToBeEquivalent() {
        assertThat(comparator.compare(null, null), is(0));
    }

    @Test
    public void shouldConsiderSameInstanceToBeEquivalent() {
        for (T validNonNullValue : validNonNullValues) {
            assertThat(comparator.compare(validNonNullValue, validNonNullValue), is(0));
        }
    }

    protected List<T> getValues( boolean includeNull ) {
        List<T> values = new ArrayList<T>(validNonNullValues.length);
        for (T validNonNullValue : validNonNullValues) {
            assertThat(validNonNullValue, is(notNullValue()));
            values.add(validNonNullValue);
        }
        if (includeNull) values.add(null);
        return values;
    }

    protected List<T> getSortedValues( boolean includeNull ) {
        List<T> values = getValues(includeNull);
        Collections.sort(values, comparator);
        return values;
    }

    // protected void assertSortsSimilarTo( Comparator<T> otherComparator ) {
    // assertThat(otherComparator, is(notNullValue()));
    // List<T> values = getValues(true);
    // List<T> otherValues = getValues(true);
    // Collections.sort(values, comparator);
    // Collections.sort(otherValues, comparator);
    // assertThat(values, is(otherValues));
    //
    // values = getValues(false);
    // otherValues = getValues(false);
    // Collections.sort(values, comparator);
    // Collections.sort(otherValues, comparator);
    // assertThat(values, is(otherValues));
    // }

    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldBeCompatibleWithCompareTo() {
        List<T> values = getValues(false);
        Collections.sort(values, comparator);
        for (int i = 0; i != (values.size() - 1); ++i) {
            T value1 = values.get(i);
            T value2 = values.get(i + 1);
            assertThat(value1, is(instanceOf(Comparable.class)));
            Comparable<T> comparable1 = (Comparable<T>)value1;
            int result = comparable1.compareTo(value2);
            assertThat(result < 0 || result == 0, is(true));
            assertThat(comparable1.compareTo(value1), is(0));
        }
    }

    protected void assertValuesCompareUsing( T value1,
                                             T value2 ) {
        int value1ToValue2 = comparator.compare(value1, value2);
        int value2ToValue1 = comparator.compare(value2, value1);
        if (value1ToValue2 == 0) {
            assertThat(value2ToValue1, is(0));
        } else if (value1ToValue2 < 0) {
            assertThat(value2ToValue1 > 0, is(true));
        } else { // if ( value1ToValue2 > 0 )
            assertThat(value2ToValue1 < 0, is(true));
        }
    }
}
