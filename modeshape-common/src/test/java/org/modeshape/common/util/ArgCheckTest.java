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

package org.modeshape.common.util;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.junit.Test;

/**
 *
 */
public class ArgCheckTest {

    @Test
    public void isNonNegativeShouldNotThrowExceptionIfPositiveNumber() {
        CheckArg.isNonNegative(1, "test");
    }

    @Test
    public void isNonNegativeShouldNotThrowExceptionIfZero() {
        CheckArg.isNonNegative(0, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNonNegativeShouldThrowExceptionIfNegative() {
        CheckArg.isNonNegative(-1, "test");
    }

    @Test
    public void isNonPositiveShouldNotThrowExceptionIfNegativeNumber() {
        CheckArg.isNonPositive(-1, "test");
    }

    @Test
    public void isNonPositiveShouldNotThrowExceptionIfZero() {
        CheckArg.isNonPositive(0, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNonPositiveShouldThrowExceptionIfPositive() {
        CheckArg.isNonPositive(1, "test");
    }

    @Test
    public void isNegativeShouldNotThrowExceptionIfNegativeNumber() {
        CheckArg.isNegative(-1, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNegativeShouldThrowExceptionIfZero() {
        CheckArg.isNegative(0, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNegativeShouldThrowExceptionIfPositive() {
        CheckArg.isNegative(1, "test");
    }

    @Test
    public void isPositiveShouldNotThrowExceptionIfPositiveNumber() {
        CheckArg.isPositive(1, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isPositiveShouldThrowExceptionIfZero() {
        CheckArg.isPositive(0, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isPositiveShouldThrowExceptionIfNegative() {
        CheckArg.isPositive(-1, "test");
    }

    @Test
    public void isNonNegativeLongShouldNotThrowExceptionIfPositiveNumber() {
        CheckArg.isNonNegative(1l, "test");
    }

    @Test
    public void isNonNegativeLongShouldNotThrowExceptionIfZero() {
        CheckArg.isNonNegative(0l, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNonNegativeLongShouldThrowExceptionIfNegative() {
        CheckArg.isNonNegative(-1l, "test");
    }

    @Test
    public void isNonPositiveLongShouldNotThrowExceptionIfNegativeNumber() {
        CheckArg.isNonPositive(-1l, "test");
    }

    @Test
    public void isNonPositiveLongShouldNotThrowExceptionIfZero() {
        CheckArg.isNonPositive(0l, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNonPositiveLongShouldThrowExceptionIfPositive() {
        CheckArg.isNonPositive(1l, "test");
    }

    @Test
    public void isNegativeLongShouldNotThrowExceptionIfNegativeNumber() {
        CheckArg.isNegative(-1l, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNegativeLongShouldThrowExceptionIfZero() {
        CheckArg.isNegative(0l, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNegativeLongShouldThrowExceptionIfPositive() {
        CheckArg.isNegative(1l, "test");
    }

    @Test
    public void isPositiveLongShouldNotThrowExceptionIfPositiveNumber() {
        CheckArg.isPositive(1l, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isPositiveLongShouldThrowExceptionIfZero() {
        CheckArg.isPositive(0l, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isPositiveLongShouldThrowExceptionIfNegative() {
        CheckArg.isPositive(-1l, "test");
    }

    @Test
    public void isNotEmptyStringShouldNotThrowExceptionIfGivenStringWithAtLeastOneCharacter() {
        CheckArg.isNotEmpty("a string", "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotEmptyStringShouldThrowExceptionIfGivenNullString() {
        CheckArg.isNotEmpty((String)null, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotEmptyStringShouldThrowExceptionIfGivenEmptyString() {
        CheckArg.isNotEmpty("", "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotEmptyStringShouldThrowExceptionIfGivenStringWithOnlyWhitespace() {
        CheckArg.isNotEmpty("\t\t  ", "test");
    }

    @Test
    public void isNotZeroLengthShouldNotThrowExceptionIfGivenAStringOneCharacterOrLonger() {
        CheckArg.isNotZeroLength("a", "test");
    }

    @Test
    public void isNotZeroLengthShouldNotThrowExceptionIfGivenAStringWithOnlyWhitespace() {
        CheckArg.isNotZeroLength(" ", "test");
        CheckArg.isNotZeroLength("\t\t", "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotZeroLengthShouldThrowExceptionIfGivenAStringWithNoCharacters() {
        CheckArg.isNotZeroLength("", "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotZeroLengthShouldThrowExceptionIfGivenANullString() {
        CheckArg.isNotZeroLength(null, "test");
    }

    @Test
    public void isNotNullShouldNotThrowExceptionIfGivenNonNullReference() {
        CheckArg.isNotNull("a", "test");
        CheckArg.isNotNull(new Object(), "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotNullShouldThrowExceptionIfGivenNullReference() {
        CheckArg.isNotNull(null, "test");
    }

    @Test
    public void getNotNullShouldReturnArgument() {
        assertThat("a", is("a"));
    }

    @Test( expected = IllegalArgumentException.class )
    public void getNotNullShouldThrowExceptionIfGivenNullReference() {
        CheckArg.getNotNull(null, "test");
    }

    @Test
    public void isNullShouldNotThrowExceptionIfGivenNullReference() {
        CheckArg.isNull(null, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNullShouldThrowExceptionIfGivenNonNullReference() {
        CheckArg.isNull(this, "test");
    }

    @Test
    public void isInstanceOfShouldNotThrowExceptionIfReferenceIsInstanceOfTheSuppliedClass() {
        CheckArg.isInstanceOf(this, this.getClass(), "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isInstanceOfShouldNotThrowExceptionIfReferenceIsNotInstanceOfTheSuppliedClass() {
        CheckArg.isInstanceOf(" ", this.getClass(), "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isInstanceOfShouldNotThrowExceptionIfReferenceIsNull() {
        CheckArg.isInstanceOf(null, this.getClass(), "test");
    }

    @Test
    public void getInstanceOfShouldReturnCastArgument() {
        Object obj = "a";
        CheckArg.getInstanceOf(obj, String.class, "test").length();
    }

    @Test( expected = IllegalArgumentException.class )
    public void getInstanceOfShouldThrowExceptionIfGivenNullReference() {
        CheckArg.getInstanceOf(null, getClass(), "test");
    }

    @Test
    public void isNotEmptyCollectionShouldNotThrowExceptionIfGivenCollectionWithAtLeastOneObject() {
        CheckArg.isNotEmpty(Collections.singletonList(" "), "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotEmptyCollectionShouldThrowExceptionIfGivenNullCollection() {
        CheckArg.isNotEmpty((Collection<?>)null, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotEmptyCollectionShouldThrowExceptionIfGivenEmptyCollection() {
        CheckArg.isNotEmpty(Collections.emptyList(), "test");
    }

    @Test
    public void isNotEmptyMapShouldNotThrowExceptionIfGivenMapWithAtLeastOneEntry() {
        CheckArg.isNotEmpty(Collections.singletonMap("key", "value"), "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotEmptyMapShouldThrowExceptionIfGivenNullMap() {
        CheckArg.isNotEmpty((Map<?, ?>)null, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotEmptyMapShouldThrowExceptionIfGivenEmptyMap() {
        CheckArg.isNotEmpty(Collections.emptyMap(), "test");
    }

    @Test
    public void isNotEmptyArrayShouldNotThrowExceptionIfGivenArrayWithAtLeastOneEntry() {
        CheckArg.isNotEmpty(new Object[] {"key", "value"}, "test");
    }

    @Test
    public void isNotEmptyArrayShouldNotThrowExceptionIfGivenArrayWithAtNullEntry() {
        CheckArg.isNotEmpty(new Object[] {"key", "value", null}, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotEmptyArrayShouldThrowExceptionIfGivenNullArray() {
        CheckArg.isNotEmpty((Object[])null, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotEmptyArrayShouldThrowExceptionIfGivenEmptyArray() {
        CheckArg.isNotEmpty(new Object[] {}, "test");
    }

    @Test
    public void isNotSameShouldNotThrowAnExceptionIfPassedSameObject() {
        CheckArg.isNotSame("a", "first", "b", "second");
        CheckArg.isNotSame(new String("a"), "first", new String("a"), "second");
    }

    @Test
    public void isNotSameShouldNotThrowAnExceptionIfPassedSameObjectWithNoNames() {
        CheckArg.isNotSame("a", null, "b", null);
        CheckArg.isNotSame(new String("a"), null, new String("a"), null);
    }

    @Test
    public void isNotSameShouldNotThrowAnExceptionIfPassedNullFirstObjectAndNonNullSecondObject() {
        CheckArg.isNotSame(null, "first", "b", "second");
    }

    @Test
    public void isNotSameShouldNotThrowAnExceptionIfPassedNonNullFirstObjectAndNullSecondObject() {
        CheckArg.isNotSame("a", "first", null, "second");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotSameShouldThrowAnExceptionIfPassedNullFirstObjectAndNullSecondObject() {
        CheckArg.isNotSame(null, "first", null, "second");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotSameShouldThrowAnExceptionIfPassedSameReferenceForFirstSecondObject() {
        String obj = "something";
        CheckArg.isNotSame(obj, "first", obj, "second");
    }

    @Test
    public void containsShouldNotThrowExceptionIfPassedObjectInCollection() {
        CheckArg.contains(Collections.singletonList(" "), " ", "test");
    }

    @Test
    public void containsShouldNotThrowExceptionIfPassedNullIfCollectionContainsNull() {
        CheckArg.contains(Collections.singletonList(null), null, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void containsShouldThrowExceptionIfPassedObjectNotInCollection() {
        CheckArg.contains(Collections.singletonList(" "), "a", "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void containsShouldThrowExceptionIfPassedNullAndCollectionDoesNotContainNull() {
        CheckArg.contains(Collections.singletonList(" "), null, "test");
    }

    @Test
    public void containsKeyShouldNotThrowExceptionIfPassedObjectInCollection() {
        CheckArg.containsKey(Collections.singletonMap("key", "value"), "key", "test");
    }

    @Test
    public void containsKeyShouldNotThrowExceptionIfPassedNullIfMapContainsNullKey() {
        CheckArg.containsKey(Collections.singletonMap(null, "value"), null, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void containsKeyShouldThrowExceptionIfPassedKeyNotInMap() {
        CheckArg.containsKey(Collections.singletonMap("key", "value"), "a", "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void containsKeyShouldThrowExceptionIfPassedNullAndMapDoesNotContainNullKey() {
        CheckArg.containsKey(Collections.singletonMap("key", "value"), null, "test");
    }

    @Test
    public void containsNoNullsCollectionShouldNotThrowExceptionIfGivenArrayWithAtLeastOneEntry() {
        CheckArg.containsNoNulls(Collections.singletonList(" "), "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void containsNoNullsCollectionShouldThrowExceptionIfGivenNullCollection() {
        CheckArg.containsNoNulls((Collection<?>)null, "test");
    }

    @Test
    public void containsNoNullsCollectionShouldNotThrowExceptionIfGivenEmptyCollection() {
        CheckArg.containsNoNulls(Collections.emptyList(), "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void containsNoNullsCollectionShouldThrowExceptionIfGivenCollectionWithNullEntry() {
        CheckArg.containsNoNulls(Collections.singletonList(null), "test");
    }

    @Test
    public void containsNoNullsArrayShouldNotThrowExceptionIfGivenArrayWithAtLeastOneEntry() {
        CheckArg.containsNoNulls(new Object[] {"key", "value"}, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void containsNoNullsArrayShouldThrowExceptionIfGivenNullArray() {
        CheckArg.containsNoNulls((Object[])null, "test");
    }

    @Test
    public void containsNoNullsArrayShouldNotThrowExceptionIfGivenEmptyArray() {
        CheckArg.containsNoNulls(new Object[] {}, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void containsNoNullsArrayShouldThrowExceptionIfGivenArrayWithNullEntry() {
        CheckArg.containsNoNulls(new Object[] {"some", null, "thing", null}, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotLessThanShouldThrowExceptionIfValueIsLessThanSuppliedValue() {
        CheckArg.isNotLessThan(0, 1, "value");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotGreaterThanShouldThrowExceptionIfValueIsGreaterThanSuppliedValue() {
        CheckArg.isNotGreaterThan(1, 0, "value");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotLessThanShouldThrowExceptionIfValueIsEqualToSuppliedValue() {
        CheckArg.isNotLessThan(1, 2, "value");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotGreaterThanShouldThrowExceptionIfValueIsEqualToSuppliedValue() {
        CheckArg.isNotGreaterThan(2, 1, "value");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isLessThanShouldThrowExceptionIfValueIsGreaterThanSuppliedValue() {
        CheckArg.isLessThan(1, 0, "value");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isGreaterThanShouldThrowExceptionIfValueIsLessThanSuppliedValue() {
        CheckArg.isGreaterThan(0, 1, "value");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isLessThanShouldThrowExceptionIfValueIsEqualToSuppliedValue() {
        CheckArg.isLessThan(1, 1, "value");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isGreaterThanShouldThrowExceptionIfValueIsEqualToSuppliedValue() {
        CheckArg.isGreaterThan(1, 1, "value");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isLessThanOrEqualToShouldThrowExceptionIfValueIsNotLessThanOrEqualToSuppliedValue() {
        CheckArg.isLessThanOrEqualTo(1, 0, "value");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isGreaterThanOrEqualToShouldThrowExceptionIfValueIsNotGreaterThanOrEqualToSuppliedValue() {
        CheckArg.isGreaterThanOrEqualTo(0, 1, "value");
    }

    @Test
    public void isNotLessThanShouldNotThrowExceptionIfValueIsNotLessThanSuppliedValue() {
        CheckArg.isNotLessThan(1, 1, "value");
        CheckArg.isNotLessThan(2, 1, "value");
        CheckArg.isNotLessThan(100, 1, "value");
    }

    @Test
    public void isNotGreaterThanShouldNotThrowExceptionIfValueIsNotGreaterThanSuppliedValue() {
        CheckArg.isNotGreaterThan(1, 1, "value");
        CheckArg.isNotGreaterThan(1, 2, "value");
        CheckArg.isNotGreaterThan(1, 100, "value");
    }

    @Test
    public void isLessThanShouldNotThrowExceptionIfValueIsLessThanSuppliedValue() {
        CheckArg.isLessThanOrEqualTo(1, 2, "value");
        CheckArg.isLessThanOrEqualTo(1, 100, "value");
    }

    @Test
    public void isGreaterThanShouldNotThrowExceptionIfValueIsGreaterThanSuppliedValue() {
        CheckArg.isGreaterThan(2, 1, "value");
        CheckArg.isGreaterThan(100, 1, "value");
    }

    @Test
    public void isLessThanOrEqualToShouldNotThrowExceptionIfValueIsLessThanOrEqualToSuppliedValue() {
        CheckArg.isLessThanOrEqualTo(1, 1, "value");
        CheckArg.isLessThanOrEqualTo(1, 2, "value");
        CheckArg.isLessThanOrEqualTo(1, 100, "value");
    }

    @Test
    public void isGreaterThanOrEqualToShouldNotThrowExceptionIfValueIsGreaterThanOrEqualToSuppliedValue() {
        CheckArg.isGreaterThanOrEqualTo(1, 1, "value");
        CheckArg.isGreaterThanOrEqualTo(2, 1, "value");
        CheckArg.isGreaterThanOrEqualTo(100, 1, "value");
    }

    @Test( expected = IllegalArgumentException.class )
    public void hasSizeOfAtLeastShouldThrowExceptionIfCollectionSizeIsSmallerThanSuppliedValue() {
        CheckArg.hasSizeOfAtLeast(Collections.singletonList(" "), 2, "value");
    }

    @Test( expected = IllegalArgumentException.class )
    public void hasSizeOfAtMostShouldThrowExceptionIfCollectionSizeIsLargerThanSuppliedValue() {
        CheckArg.hasSizeOfAtMost(Collections.singletonList(" "), 0, "value");
    }

    @Test
    public void hasSizeOfAtLeastShouldNotThrowExceptionIfCollectionSizeIsEqualToSuppliedValue() {
        CheckArg.hasSizeOfAtLeast(Collections.singletonList(" "), 1, "value");
    }

    @Test
    public void hasSizeOfAtMostShouldNotThrowExceptionIfCollectionSizeIsEqualToSuppliedValue() {
        CheckArg.hasSizeOfAtMost(Collections.singletonList(" "), 1, "value");
    }

    @Test
    public void hasSizeOfAtLeastShouldNotThrowExceptionIfCollectionSizeIsGreaterThanSuppliedValue() {
        CheckArg.hasSizeOfAtLeast(Collections.singletonList(" "), 0, "value");
    }

    @Test
    public void hasSizeOfAtMostShouldNotThrowExceptionIfCollectionSizeIsGreaterThanSuppliedValue() {
        CheckArg.hasSizeOfAtMost(Collections.singletonList(" "), 2, "value");
    }

    @Test( expected = IllegalArgumentException.class )
    public void hasSizeOfAtLeastShouldThrowExceptionIfMapSizeIsSmallerThanSuppliedValue() {
        CheckArg.hasSizeOfAtLeast(Collections.singletonMap("key", "value"), 2, "value");
    }

    @Test( expected = IllegalArgumentException.class )
    public void hasSizeOfAtMostShouldThrowExceptionIfMapSizeIsLargerThanSuppliedValue() {
        CheckArg.hasSizeOfAtMost(Collections.singletonMap("key", "value"), 0, "value");
    }

    @Test
    public void hasSizeOfAtLeastShouldNotThrowExceptionIfMapSizeIsEqualToSuppliedValue() {
        CheckArg.hasSizeOfAtLeast(Collections.singletonMap("key", "value"), 1, "value");
    }

    @Test
    public void hasSizeOfAtMostShouldNotThrowExceptionIfMapSizeIsEqualToSuppliedValue() {
        CheckArg.hasSizeOfAtMost(Collections.singletonMap("key", "value"), 1, "value");
    }

    @Test
    public void hasSizeOfAtLeastShouldNotThrowExceptionIfMapSizeIsGreaterThanSuppliedValue() {
        CheckArg.hasSizeOfAtLeast(Collections.singletonMap("key", "value"), 0, "value");
    }

    @Test
    public void hasSizeOfAtMostShouldNotThrowExceptionIfMapSizeIsGreaterThanSuppliedValue() {
        CheckArg.hasSizeOfAtMost(Collections.singletonMap("key", "value"), 2, "value");
    }

    @Test( expected = IllegalArgumentException.class )
    public void hasSizeOfAtLeastShouldThrowExceptionIfArraySizeIsSmallerThanSuppliedValue() {
        CheckArg.hasSizeOfAtLeast(new Object[] {"key", "value"}, 3, "value");
    }

    @Test( expected = IllegalArgumentException.class )
    public void hasSizeOfAtMostShouldThrowExceptionIfArraySizeIsLargerThanSuppliedValue() {
        CheckArg.hasSizeOfAtMost(new Object[] {"key", "value"}, 1, "value");
    }

    @Test
    public void hasSizeOfAtLeastShouldNotThrowExceptionIfArraySizeIsEqualToSuppliedValue() {
        CheckArg.hasSizeOfAtLeast(new Object[] {"key", "value"}, 2, "value");
    }

    @Test
    public void hasSizeOfAtMostShouldNotThrowExceptionIfArraySizeIsEqualToSuppliedValue() {
        CheckArg.hasSizeOfAtMost(new Object[] {"key", "value"}, 2, "value");
    }

    @Test
    public void hasSizeOfAtLeastShouldNotThrowExceptionIfArraySizeIsGreaterThanSuppliedValue() {
        CheckArg.hasSizeOfAtLeast(new Object[] {"key", "value"}, 1, "value");
    }

    @Test
    public void hasSizeOfAtMostShouldNotThrowExceptionIfArraySizeIsGreaterThanSuppliedValue() {
        CheckArg.hasSizeOfAtMost(new Object[] {"key", "value"}, 3, "value");
    }

}
