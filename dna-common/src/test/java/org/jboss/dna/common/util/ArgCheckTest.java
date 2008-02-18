/*
 * JBoss, Home of Professional Open Source. Copyright 2008, Red Hat Middleware LLC, and individual contributors as indicated by
 * the @author tags. See the copyright.txt file in the distribution for a full listing of individual contributors. This is free
 * software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your option) any later version. This software is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details. You should have received a
 * copy of the GNU Lesser General Public License along with this software; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.common.util;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;
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
        ArgCheck.isNonNegative(1, "test");
    }

    @Test
    public void isNonNegativeShouldNotThrowExceptionIfZero() {
        ArgCheck.isNonNegative(0, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNonNegativeShouldThrowExceptionIfNegative() {
        ArgCheck.isNonNegative(-1, "test");
    }

    @Test
    public void isNonPositiveShouldNotThrowExceptionIfNegativeNumber() {
        ArgCheck.isNonPositive(-1, "test");
    }

    @Test
    public void isNonPositiveShouldNotThrowExceptionIfZero() {
        ArgCheck.isNonPositive(0, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNonPositiveShouldThrowExceptionIfPositive() {
        ArgCheck.isNonPositive(1, "test");
    }

    @Test
    public void isNegativeShouldNotThrowExceptionIfNegativeNumber() {
        ArgCheck.isNegative(-1, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNegativeShouldThrowExceptionIfZero() {
        ArgCheck.isNegative(0, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNegativeShouldThrowExceptionIfPositive() {
        ArgCheck.isNegative(1, "test");
    }

    @Test
    public void isPositiveShouldNotThrowExceptionIfPositiveNumber() {
        ArgCheck.isPositive(1, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isPositiveShouldThrowExceptionIfZero() {
        ArgCheck.isPositive(0, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isPositiveShouldThrowExceptionIfNegative() {
        ArgCheck.isPositive(-1, "test");
    }

    @Test
    public void isNonNegativeLongShouldNotThrowExceptionIfPositiveNumber() {
        ArgCheck.isNonNegative(1l, "test");
    }

    @Test
    public void isNonNegativeLongShouldNotThrowExceptionIfZero() {
        ArgCheck.isNonNegative(0l, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNonNegativeLongShouldThrowExceptionIfNegative() {
        ArgCheck.isNonNegative(-1l, "test");
    }

    @Test
    public void isNonPositiveLongShouldNotThrowExceptionIfNegativeNumber() {
        ArgCheck.isNonPositive(-1l, "test");
    }

    @Test
    public void isNonPositiveLongShouldNotThrowExceptionIfZero() {
        ArgCheck.isNonPositive(0l, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNonPositiveLongShouldThrowExceptionIfPositive() {
        ArgCheck.isNonPositive(1l, "test");
    }

    @Test
    public void isNegativeLongShouldNotThrowExceptionIfNegativeNumber() {
        ArgCheck.isNegative(-1l, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNegativeLongShouldThrowExceptionIfZero() {
        ArgCheck.isNegative(0l, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNegativeLongShouldThrowExceptionIfPositive() {
        ArgCheck.isNegative(1l, "test");
    }

    @Test
    public void isPositiveLongShouldNotThrowExceptionIfPositiveNumber() {
        ArgCheck.isPositive(1l, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isPositiveLongShouldThrowExceptionIfZero() {
        ArgCheck.isPositive(0l, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isPositiveLongShouldThrowExceptionIfNegative() {
        ArgCheck.isPositive(-1l, "test");
    }

    @Test
    public void isNotEmptyStringShouldNotThrowExceptionIfGivenStringWithAtLeastOneCharacter() {
        ArgCheck.isNotEmpty("a string", "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotEmptyStringShouldThrowExceptionIfGivenNullString() {
        ArgCheck.isNotEmpty((String)null, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotEmptyStringShouldThrowExceptionIfGivenEmptyString() {
        ArgCheck.isNotEmpty("", "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotEmptyStringShouldThrowExceptionIfGivenStringWithOnlyWhitespace() {
        ArgCheck.isNotEmpty("\t\t  ", "test");
    }

    @Test
    public void isNotZeroLengthShouldNotThrowExceptionIfGivenAStringOneCharacterOrLonger() {
        ArgCheck.isNotZeroLength("a", "test");
    }

    @Test
    public void isNotZeroLengthShouldNotThrowExceptionIfGivenAStringWithOnlyWhitespace() {
        ArgCheck.isNotZeroLength(" ", "test");
        ArgCheck.isNotZeroLength("\t\t", "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotZeroLengthShouldThrowExceptionIfGivenAStringWithNoCharacters() {
        ArgCheck.isNotZeroLength("", "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotZeroLengthShouldThrowExceptionIfGivenANullString() {
        ArgCheck.isNotZeroLength(null, "test");
    }

    @Test
    public void isNotNullShouldNotThrowExceptionIfGivenNonNullReference() {
        ArgCheck.isNotNull("a", "test");
        ArgCheck.isNotNull(new Object(), "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotNullShouldThrowExceptionIfGivenNullReference() {
        ArgCheck.isNotNull(null, "test");
    }

    @Test
    public void getNotNullShouldReturnArgument() {
        assertThat("a", is("a"));
    }

    @Test( expected = IllegalArgumentException.class )
    public void getNotNullShouldThrowExceptionIfGivenNullReference() {
        ArgCheck.getNotNull(null, "test");
    }

    @Test
    public void isNullShouldNotThrowExceptionIfGivenNullReference() {
        ArgCheck.isNull(null, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNullShouldThrowExceptionIfGivenNonNullReference() {
        ArgCheck.isNull(this, "test");
    }

    @Test
    public void isInstanceOfShouldNotThrowExceptionIfReferenceIsInstanceOfTheSuppliedClass() {
        ArgCheck.isInstanceOf(this, this.getClass(), "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isInstanceOfShouldNotThrowExceptionIfReferenceIsNotInstanceOfTheSuppliedClass() {
        ArgCheck.isInstanceOf(" ", this.getClass(), "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isInstanceOfShouldNotThrowExceptionIfReferenceIsNull() {
        ArgCheck.isInstanceOf(null, this.getClass(), "test");
    }

    @Test
    public void getInstanceOfShouldReturnCastArgument() {
        Object obj = "a";
        ArgCheck.getInstanceOf(obj, String.class, "test").length();
    }

    @Test( expected = IllegalArgumentException.class )
    public void getInstanceOfShouldThrowExceptionIfGivenNullReference() {
        ArgCheck.getInstanceOf(null, getClass(), "test");
    }

    @Test
    public void isNotEmptyCollectionShouldNotThrowExceptionIfGivenCollectionWithAtLeastOneObject() {
        ArgCheck.isNotEmpty(Collections.singletonList(" "), "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotEmptyCollectionShouldThrowExceptionIfGivenNullCollection() {
        ArgCheck.isNotEmpty((Collection)null, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotEmptyCollectionShouldThrowExceptionIfGivenEmptyCollection() {
        ArgCheck.isNotEmpty(Collections.emptyList(), "test");
    }

    @Test
    public void isNotEmptyMapShouldNotThrowExceptionIfGivenMapWithAtLeastOneEntry() {
        ArgCheck.isNotEmpty(Collections.singletonMap("key", "value"), "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotEmptyMapShouldThrowExceptionIfGivenNullMap() {
        ArgCheck.isNotEmpty((Map)null, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotEmptyMapShouldThrowExceptionIfGivenEmptyMap() {
        ArgCheck.isNotEmpty(Collections.emptyMap(), "test");
    }

    @Test
    public void isNotEmptyArrayShouldNotThrowExceptionIfGivenArrayWithAtLeastOneEntry() {
        ArgCheck.isNotEmpty(new Object[] {"key", "value"}, "test");
    }

    @Test
    public void isNotEmptyArrayShouldNotThrowExceptionIfGivenArrayWithAtNullEntry() {
        ArgCheck.isNotEmpty(new Object[] {"key", "value", null}, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotEmptyArrayShouldThrowExceptionIfGivenNullArray() {
        ArgCheck.isNotEmpty((Object[])null, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotEmptyArrayShouldThrowExceptionIfGivenEmptyArray() {
        ArgCheck.isNotEmpty(new Object[] {}, "test");
    }

    @Test
    public void isNotSameShouldNotThrowAnExceptionIfPassedSameObject() {
        ArgCheck.isNotSame("a", "first", "b", "second");
        ArgCheck.isNotSame(new String("a"), "first", new String("a"), "second");
    }

    @Test
    public void isNotSameShouldNotThrowAnExceptionIfPassedSameObjectWithNoNames() {
        ArgCheck.isNotSame("a", null, "b", null);
        ArgCheck.isNotSame(new String("a"), null, new String("a"), null);
    }

    @Test
    public void isNotSameShouldNotThrowAnExceptionIfPassedNullFirstObjectAndNonNullSecondObject() {
        ArgCheck.isNotSame(null, "first", "b", "second");
    }

    @Test
    public void isNotSameShouldNotThrowAnExceptionIfPassedNonNullFirstObjectAndNullSecondObject() {
        ArgCheck.isNotSame("a", "first", null, "second");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotSameShouldThrowAnExceptionIfPassedNullFirstObjectAndNullSecondObject() {
        ArgCheck.isNotSame(null, "first", null, "second");
    }

    @Test( expected = IllegalArgumentException.class )
    public void isNotSameShouldThrowAnExceptionIfPassedSameReferenceForFirstSecondObject() {
        String obj = "something";
        ArgCheck.isNotSame(obj, "first", obj, "second");
    }

    @Test
    public void containsShouldNotThrowExceptionIfPassedObjectInCollection() {
        ArgCheck.contains(Collections.singletonList(" "), " ", "test");
    }

    @Test
    public void containsShouldNotThrowExceptionIfPassedNullIfCollectionContainsNull() {
        ArgCheck.contains(Collections.singletonList(null), null, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void containsShouldThrowExceptionIfPassedObjectNotInCollection() {
        ArgCheck.contains(Collections.singletonList(" "), "a", "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void containsShouldThrowExceptionIfPassedNullAndCollectionDoesNotContainNull() {
        ArgCheck.contains(Collections.singletonList(" "), null, "test");
    }

    @Test
    public void containsKeyShouldNotThrowExceptionIfPassedObjectInCollection() {
        ArgCheck.containsKey(Collections.singletonMap("key", "value"), "key", "test");
    }

    @Test
    public void containsKeyShouldNotThrowExceptionIfPassedNullIfMapContainsNullKey() {
        ArgCheck.containsKey(Collections.singletonMap(null, "value"), null, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void containsKeyShouldThrowExceptionIfPassedKeyNotInMap() {
        ArgCheck.containsKey(Collections.singletonMap("key", "value"), "a", "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void containsKeyShouldThrowExceptionIfPassedNullAndMapDoesNotContainNullKey() {
        ArgCheck.containsKey(Collections.singletonMap("key", "value"), null, "test");
    }

    @Test
    public void containsNoNullsCollectionShouldNotThrowExceptionIfGivenArrayWithAtLeastOneEntry() {
        ArgCheck.containsNoNulls(Collections.singletonList(" "), "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void containsNoNullsCollectionShouldThrowExceptionIfGivenNullCollection() {
        ArgCheck.containsNoNulls((Collection)null, "test");
    }

    @Test
    public void containsNoNullsCollectionShouldNotThrowExceptionIfGivenEmptyCollection() {
        ArgCheck.containsNoNulls(Collections.emptyList(), "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void containsNoNullsCollectionShouldThrowExceptionIfGivenCollectionWithNullEntry() {
        ArgCheck.containsNoNulls(Collections.singletonList(null), "test");
    }

    @Test
    public void containsNoNullsArrayShouldNotThrowExceptionIfGivenArrayWithAtLeastOneEntry() {
        ArgCheck.containsNoNulls(new Object[] {"key", "value"}, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void containsNoNullsArrayShouldThrowExceptionIfGivenNullArray() {
        ArgCheck.containsNoNulls((Object[])null, "test");
    }

    @Test
    public void containsNoNullsArrayShouldNotThrowExceptionIfGivenEmptyArray() {
        ArgCheck.containsNoNulls(new Object[] {}, "test");
    }

    @Test( expected = IllegalArgumentException.class )
    public void containsNoNullsArrayShouldThrowExceptionIfGivenArrayWithNullEntry() {
        ArgCheck.containsNoNulls(new Object[] {"some", null, "thing", null}, "test");
    }
}
