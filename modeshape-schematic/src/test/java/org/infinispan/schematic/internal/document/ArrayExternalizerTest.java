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
package org.infinispan.schematic.internal.document;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.infinispan.schematic.document.Array;
import org.junit.Test;

public class ArrayExternalizerTest extends AbstractExternalizerTest {

    @Test
    public void shouldRoundTripSimpleArrayOfStrings() throws Exception {
        BasicArray array = new BasicArray();
        array.addValue("value1");
        array.addValue("value2");
        array.addValue("value3");
        assertRoundTrip(array);
    }

    @Test
    public void shouldRoundTripSimpleArrayOfMixedValues() throws Exception {
        BasicArray array = new BasicArray();
        array.addValue("value1");
        array.addValue(43);
        array.addValue(new BasicDocument("field1", "value1"));
        assertRoundTrip(array);
    }

    protected void assertRoundTrip( Array array ) throws Exception {
        byte[] bytes = marshall(array);
        Array newArray = (Array)unmarshall(bytes);
        assertThat(newArray, is(array));
    }

}
