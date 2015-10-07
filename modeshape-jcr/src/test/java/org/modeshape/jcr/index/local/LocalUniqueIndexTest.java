/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.modeshape.jcr.index.local;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.modeshape.jcr.api.query.qom.Operator;

public class LocalUniqueIndexTest extends AbstractLocalIndexTest {

    @Test
    public void shouldAllowCreatingLongValueIndex() {
        LocalUniqueIndex<Long> index = uniqueValueIndex(Long.class);
        assertThat(index, is(notNullValue()));
        assertThat(index.estimateTotalCount(), is(0L));
    }

    @Test
    public void shouldAllowBasicQueryOperationsOnLongValueIndex() {
        LocalUniqueIndex<Long> index = uniqueValueIndex(Long.class);
        loadLongIndex(index, 10);
        assertThat(index.estimateTotalCount(), is(10L));

        // Check a value that's in the index ...
        assertMatch(index, Operator.EQUAL_TO, 50L, key(5));
        assertMatch(index, Operator.NOT_EQUAL_TO, 50L, 1, 2, 3, 4, 6, 7, 8, 9, 10);
        assertMatch(index, Operator.LESS_THAN_OR_EQUAL_TO, 50L, 1, 2, 3, 4, 5);
        assertMatch(index, Operator.LESS_THAN, 50L, 1, 2, 3, 4);
        assertMatch(index, Operator.GREATER_THAN_OR_EQUAL_TO, 50L, 5, 6, 7, 8, 9, 10);
        assertMatch(index, Operator.GREATER_THAN, 50L, 6, 7, 8, 9, 10);

        // Check a value that's not in the index but is between values that are ...
        assertNoMatch(index, Operator.EQUAL_TO, 45L);
        assertMatch(index, Operator.NOT_EQUAL_TO, 45L, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertMatch(index, Operator.LESS_THAN_OR_EQUAL_TO, 45L, 1, 2, 3, 4);
        assertMatch(index, Operator.LESS_THAN, 45L, 1, 2, 3, 4);
        assertMatch(index, Operator.GREATER_THAN_OR_EQUAL_TO, 45L, 5, 6, 7, 8, 9, 10);
        assertMatch(index, Operator.GREATER_THAN, 45L, 5, 6, 7, 8, 9, 10);

        // Check a value that's not in the index but is greater than all values that are ...
        assertNoMatch(index, Operator.EQUAL_TO, 450L);
        assertMatch(index, Operator.NOT_EQUAL_TO, 450L, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertMatch(index, Operator.LESS_THAN_OR_EQUAL_TO, 450L, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertMatch(index, Operator.LESS_THAN, 450L, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertNoMatch(index, Operator.GREATER_THAN_OR_EQUAL_TO, 450L);
        assertNoMatch(index, Operator.GREATER_THAN, 450L);

        // Check a value that's not in the index but is less than all values that are ...
        assertNoMatch(index, Operator.EQUAL_TO, -1L);
        assertMatch(index, Operator.NOT_EQUAL_TO, -1L, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertNoMatch(index, Operator.LESS_THAN_OR_EQUAL_TO, -1L);
        assertNoMatch(index, Operator.LESS_THAN, -1L);
        assertMatch(index, Operator.GREATER_THAN_OR_EQUAL_TO, -1L, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertMatch(index, Operator.GREATER_THAN, -1L, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void shouldAllowBasicQueryOperationsOnStringValueIndex() {
        LocalUniqueIndex<String> index = uniqueValueIndex(String.class);
        loadStringIndex(index, 10);
        assertThat(index.estimateTotalCount(), is(10L));

        // Note the sorting of the string-based values puts key 10 after key 1 but before key 2
        assertThat(key(10).compareTo(key(1)), is(1));
        assertThat(key(10).compareTo(key(2)), is(-1));

        // Check a value that's in the index ...
        assertMatch(index, Operator.EQUAL_TO, "50", key(5));
        assertMatch(index, Operator.NOT_EQUAL_TO, "50", 1, 10, 2, 3, 4, 6, 7, 8, 9);
        assertMatch(index, Operator.LESS_THAN_OR_EQUAL_TO, "50", 1, 10, 2, 3, 4, 5);
        assertMatch(index, Operator.LESS_THAN, "50", 1, 10, 2, 3, 4);
        assertMatch(index, Operator.GREATER_THAN_OR_EQUAL_TO, "50", 5, 6, 7, 8, 9);
        assertMatch(index, Operator.GREATER_THAN, "50", 6, 7, 8, 9);

        // Check a value that's not in the index but is between values that are ...
        assertNoMatch(index, Operator.EQUAL_TO, "45");
        assertMatch(index, Operator.NOT_EQUAL_TO, "45", 1, 10, 2, 3, 4, 5, 6, 7, 8, 9);
        assertMatch(index, Operator.LESS_THAN_OR_EQUAL_TO, "45", 1, 10, 2, 3, 4);
        assertMatch(index, Operator.LESS_THAN, "45", 1, 10, 2, 3, 4);
        assertMatch(index, Operator.GREATER_THAN_OR_EQUAL_TO, "45", 5, 6, 7, 8, 9);
        assertMatch(index, Operator.GREATER_THAN, "45", 5, 6, 7, 8, 9);

        // Check a value that's not in the index but is greater than all values that are ...
        assertNoMatch(index, Operator.EQUAL_TO, "abcdef");
        assertMatch(index, Operator.NOT_EQUAL_TO, "abcdef", 1, 10, 2, 3, 4, 5, 6, 7, 8, 9);
        assertMatch(index, Operator.LESS_THAN_OR_EQUAL_TO, "abcdef", 1, 10, 2, 3, 4, 5, 6, 7, 8, 9);
        assertMatch(index, Operator.LESS_THAN, "abcdef", 1, 10, 2, 3, 4, 5, 6, 7, 8, 9);
        assertNoMatch(index, Operator.GREATER_THAN_OR_EQUAL_TO, "abcdef");
        assertNoMatch(index, Operator.GREATER_THAN, "abcdef");

        // Check a value that's not in the index but is less than all values that are ...
        assertNoMatch(index, Operator.EQUAL_TO, "");
        assertMatch(index, Operator.NOT_EQUAL_TO, "", 1, 10, 2, 3, 4, 5, 6, 7, 8, 9);
        assertNoMatch(index, Operator.LESS_THAN_OR_EQUAL_TO, "");
        assertNoMatch(index, Operator.LESS_THAN, "");
        assertMatch(index, Operator.GREATER_THAN_OR_EQUAL_TO, "", 1, 10, 2, 3, 4, 5, 6, 7, 8, 9);
        assertMatch(index, Operator.GREATER_THAN, "", 1, 10, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void shouldAllowRemovingAllValuesForKey() {
        LocalUniqueIndex<Long> index = uniqueValueIndex(Long.class);
        loadLongIndex(index, 10);

        // Check the values that are in the index ...
        assertThat(index.estimateTotalCount(), is(10L));
        assertMatch(index, Operator.EQUAL_TO, 20L, key(2));

        // Remove the value associated with a given key and verify they are indeed gone ...
        index.remove(key(2));
        assertNoMatch(index, Operator.EQUAL_TO, 20L);
        assertThat(index.estimateTotalCount(), is(9L));

        // Try to remove the values associated with a key not in the index ...
        assertNoMatch(index, Operator.EQUAL_TO, 200L);
        index.remove(key(20));
        assertNoMatch(index, Operator.EQUAL_TO, 200L);
        assertThat(index.estimateTotalCount(), is(9L));

        // Remove a value and key pair that's in the index, and verify they are indeed gone ...
        index.remove(key(3), null, 30L);
        assertNoMatch(index, Operator.EQUAL_TO, 30L);
        assertThat(index.estimateTotalCount(), is(8L));

        // Try to remove a non-existant value-key pair, and verify nothing is removed ...
        index.remove(key(3), null, 3000L);
        assertNoMatch(index, Operator.EQUAL_TO, 30L);
        assertThat(index.estimateTotalCount(), is(8L));
    }
}
