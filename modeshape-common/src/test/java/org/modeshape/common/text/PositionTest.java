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
package org.modeshape.common.text;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class PositionTest {

    private int combinedIndex( int firstIndex,
                               int secondIndex ) {
        Position first = new Position(firstIndex, 1, 0);
        Position second = new Position(secondIndex, 1, 0);

        int firstPlusSecond = first.add(second).getIndexInContent();
        int secondPlusFirst = second.add(first).getIndexInContent();

        assertThat(firstPlusSecond, is(secondPlusFirst));

        return firstPlusSecond;
    }

    @Test
    public void shouldAddNoContentPositionToValidPosition() {
        // -1 to >=0
        assertThat(combinedIndex(-1, 0), is(0));
        assertThat(combinedIndex(-1, 1), is(1));
        assertThat(combinedIndex(-1, 10), is(10));
    }

    @Test
    public void shouldAddValidPositionToNoContentPosition() {
        // >= 0 to -1
        assertThat(combinedIndex(0, -1), is(0));
        assertThat(combinedIndex(1, -1), is(1));
        assertThat(combinedIndex(10, -1), is(10));
    }

    @Test
    public void shouldAddValidPositionToValidPosition() {
        // positive to positive
        assertThat(combinedIndex(1, 1), is(2));
        assertThat(combinedIndex(10, 1), is(11));
        assertThat(combinedIndex(1, 10), is(11));
        assertThat(combinedIndex(10, 10), is(20));
    }

    @Test
    public void shouldAddStartingPositionToStartingPosition() {
        // 0 to 0
        assertThat(combinedIndex(0, 0), is(0));
    }

    @Test
    public void shouldAddNoContentPositionToNoContentPosition() {
        // -1 to -1
        assertThat(combinedIndex(-1, -1), is(-1));
        assertThat(combinedIndex(-10, -1), is(-1));
        assertThat(combinedIndex(-1, -10), is(-1));
    }
}
