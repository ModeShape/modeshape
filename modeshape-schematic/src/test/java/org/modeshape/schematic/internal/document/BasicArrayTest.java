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
package org.modeshape.schematic.internal.document;

import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class BasicArrayTest {

    private BasicArray array;

    @Before
    public void beforeTest() {
        array = new BasicArray();
        for (int i = 0; i != 10; ++i) {
            array.addValue(i);
        }
    }

    @Test
    public void shouldReturnProperSubarray() {
        List<?> subArray = array.subList(3, 8);
        assert (Integer)subArray.get(0) == 3;
        assert (Integer)subArray.get(1) == 4;
        assert (Integer)subArray.get(2) == 5;
        assert (Integer)subArray.get(3) == 6;
        assert (Integer)subArray.get(4) == 7;

        Iterator<?> iter = subArray.iterator();
        int value = 3;
        while (iter.hasNext()) {
            assert (Integer)iter.next() == value++;
        }
    }

    @Test
    public void shouldCreateCopy() {
        BasicArray array2 = new BasicArray(array.size());
        array2.addAllValues(array);
        for (int i = 0; i != 10; ++i) {
            assert (Integer)array2.get(i) == i;
        }
    }

    @Test
    public void shouldCreateCopyOfSublist() {
        BasicArray array2 = new BasicArray(5);
        array2.addAllValues(array.subList(3, 8));
        for (int i = 0; i != 5; ++i) {
            assert (Integer)array2.get(i) == (i + 3);
        }
    }
}
