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
package org.modeshape.common.util;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class HashCodeTest {

    @Test
    public void shouldComputeHashCodeForOnePrimitive() {
        assertThat(HashCode._compute(1), is(not(0)));
        assertThat(HashCode.compute((long)8), is(not(0)));
        assertThat(HashCode._compute((short) 3), is(not(0)));
        assertThat(HashCode.compute(1.0f), is(not(0)));
        assertThat(HashCode.compute(1.0d), is(not(0)));
        assertThat(HashCode.compute(true), is(not(0)));
    }

    @Test
    public void shouldComputeHashCodeForMultiplePrimitives() {
        assertThat(HashCode._compute(1, 2, 3), is(not(0)));
        assertThat(HashCode.compute((long)8, (long)22, 33), is(not(0)));
        assertThat(HashCode._compute((short) 3, (long) 22, true), is(not(0)));
    }

    @Test
    public void shouldAcceptNoArguments() {
        assertThat(HashCode.compute(), is(0));
    }

    @Test
    public void shouldAcceptNullArguments() {
        assertThat(HashCode.compute((Object)null), is(0));
        assertThat(HashCode.compute("abc", (Object)null), is(not(0)));
    }

}
