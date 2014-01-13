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
package org.modeshape.common.i18n;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class MockI18nTest {

    @Test
    public void shouldPassthroughText() {
        String str = "This is some string to be passed through";
        assertThat(MockI18n.passthrough.text(str), is(str));
    }

    @Test
    public void shouldPassthroughTextWithoutReplacingParameters() {
        String str = "This is some string to be passed through with {1} parameters";
        assertThat(MockI18n.passthrough.text(str), is(str));
    }

}
