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
package org.modeshape.common.collection;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class ImmutableAppendedListTest {

    private List<String> list;
    private List<String> parent;
    private String[] data;

    @Before
    public void beforeEach() {
        data = new String[] {"a", "b", "c", "d", "e"};
        parent = new ArrayList<String>(5);
        for (int i = 0; i != 4; ++i) {
            parent.add(data[i]);
        }
        list = new ImmutableAppendedList<String>(parent, data[4]);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCreatingWithNullParentList() {
        list = new ImmutableAppendedList<String>(null, data[4]);
    }

    @Test
    public void shouldAllowCreatingWithEmptyParentList() {
        data = new String[] {data[0], data[1]};
        parent = Collections.singletonList(data[0]);
        list = new ImmutableAppendedList<String>(parent, data[1]);
        assertThat(list, hasItems(data));
        assertThat(list.size(), is(parent.size() + 1));
        assertThat(list.toArray(), is((Object[])data));
        assertThat(list.toArray(new String[list.size()]), is(data));
        assertThat(list.toArray(new String[list.size() - 1]), is(data)); // too small
        assertThat(list.isEmpty(), is(false));
    }

    @Test
    public void shouldAllowCreatingAppendedListWithNullForFinalElement() {
        data[4] = null;
        list = new ImmutableAppendedList<String>(parent, data[4]);
        assertThat(list, hasItems(data));
    }

    @Test
    public void shouldHaveSizeOfParentPlusOne() {
        assertThat(list.size(), is(parent.size() + 1));
    }

    @Test
    public void shouldConvertToArrayContainingAllValues() {
        assertThat(list.toArray(), is((Object[])data));
    }

    @Test
    public void shouldConvertToSuppliedArrayContainingAllValues() {
        assertThat(list.toArray(new String[list.size()]), is(data));
    }

    @Test
    public void shouldConvertToTooSmallSuppliedArrayContainingAllValues() {
        assertThat(list.toArray(new String[list.size() - 1]), is(data));
    }

    @Test
    public void shouldIterateOverAllValues() {
        Iterator<String> iter = list.iterator();
        int i = 0;
        while (iter.hasNext()) {
            assertThat(iter.next(), is(data[i++]));
        }
    }

    @Test
    public void shouldIterateOverAllValuesUsingListIterator() {
        List<String> copy = new ArrayList<String>(list);
        assertThat(copy.size(), is(list.size()));
        ListIterator<String> listIter = list.listIterator();
        ListIterator<String> copyIter = copy.listIterator();
        for (int i = 0; i != 3; ++i) {
            assertThat(listIter.hasPrevious(), is(false));
            assertThat(copyIter.hasPrevious(), is(false));
            while (listIter.hasNext()) {
                assertThat(listIter.next(), is(copyIter.next()));
            }
            assertThat(listIter.hasNext(), is(false));
            assertThat(copyIter.hasNext(), is(false));
            while (listIter.hasPrevious()) {
                assertThat(listIter.previous(), is(copyIter.previous()));
            }
        }
        assertThat(listIter.hasPrevious(), is(false));
        assertThat(copyIter.hasPrevious(), is(false));
    }

    @Test
    public void shouldIterateBackwardsOverAllValuesUsingListIterator() {
        List<String> copy = new ArrayList<String>(list);
        assertThat(copy.size(), is(list.size()));
        ListIterator<String> listIter = list.listIterator(list.size());
        ListIterator<String> copyIter = copy.listIterator(copy.size());
        assertThat(listIter.hasNext(), is(false));
        assertThat(copyIter.hasNext(), is(false));
        while (listIter.hasPrevious()) {
            assertThat(listIter.previous(), is(copyIter.previous()));
        }
        assertThat(listIter.hasPrevious(), is(false));
        assertThat(copyIter.hasPrevious(), is(false));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowAddingAnElement() {
        list.add(null);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowAddingAnElementByIndex() {
        list.add(0, null);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowAddingACollection() {
        list.addAll(parent);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowAddingACollectionWithIndex() {
        list.addAll(1, parent);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowRemovingAnElement() {
        list.remove(null);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowRemovingAnElementByIndex() {
        list.remove(0);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowRemovingAllElementsInACollection() {
        list.removeAll(null);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowRetainingAllElementsInACollection() {
        list.retainAll(null);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSettingElementByIndex() {
        list.set(0, null);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowClearingList() {
        list.clear();
    }

    @Test
    public void shouldReturnSameHashCodeMultipleTimes() {
        int hc = list.hashCode();
        for (int i = 0; i != 100; ++i) {
            assertThat(list.hashCode(), is(hc));
        }
    }

    @Test
    public void shouldReturnSameHashCodeAsEquivalentArrayList() {
        List<String> copy = new ArrayList<String>(list);
        assertThat(list.hashCode(), is(copy.hashCode()));
    }

    @Test
    public void shouldBeEqualToEquivalentArrayList() {
        List<String> copy = new ArrayList<String>(list);
        assertThat(list.equals(copy), is(true));
    }

    @Test
    public void shouldHaveToStringThatIsTheSameAsEquivalentArrayList() {
        List<String> copy = new ArrayList<String>(list);
        assertThat(list.toString(), is(copy.toString()));
    }

    @Test
    public void shouldFindLastIndexOfEachValue() {
        for (int i = 0; i != data.length; ++i) {
            String value = data[i];
            int lastIndex = list.lastIndexOf(value);
            assertThat(lastIndex, is(i));
        }
    }

    @Test
    public void shouldNotFindLastIndexOfValuesThatAreNotInList() {
        assertThat(list.lastIndexOf("not found"), is(-1));
        assertThat(list.lastIndexOf(null), is(-1));
    }

    @Test
    public void shouldFindIndexOfEachValue() {
        for (int i = 0; i != data.length; ++i) {
            String value = data[i];
            int lastIndex = list.lastIndexOf(value);
            assertThat(lastIndex, is(i));
        }
    }

    @Test
    public void shouldNotFindIndexOfValuesThatAreNotInList() {
        assertThat(list.lastIndexOf("not found"), is(-1));
        assertThat(list.lastIndexOf(null), is(-1));
    }

    @Test
    public void shouldGetValuesByIndex() {
        for (int i = 0; i != data.length; ++i) {
            String expectedValue = data[i];
            String actualValue = list.get(i);
            assertThat(actualValue, is(expectedValue));
        }
    }

    @Test
    public void shouldContainEachValue() {
        for (int i = 0; i != data.length; ++i) {
            String value = data[i];
            assertThat(list.contains(value), is(true));
        }
    }

    @Test
    public void shouldNotContainValuesThatAreNotInList() {
        assertThat(list.contains("not found"), is(false));
        assertThat(list.contains(null), is(false));
    }

    @Test
    public void shouldContainAllValuesInDuplicateCollection() {
        List<String> copy = new ArrayList<String>(list);
        assertThat(list.containsAll(copy), is(true));
        assertThat(copy.containsAll(list), is(true));
    }

    @Test
    public void shouldNeverBeEmpty() {
        assertThat(list.isEmpty(), is(false));
    }

}
