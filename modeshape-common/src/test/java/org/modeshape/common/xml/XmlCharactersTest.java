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
package org.modeshape.common.xml;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 * 
 */
public class XmlCharactersTest {

    @Test
    public void shouldNotAllowColonInNcName() {
        assertThat(XmlCharacters.isValidNcNameStart(':'), is(false));
    }

    @Test
    public void shouldNotAllowDigitAsFirstCharacterInName() {
        assertThat(XmlCharacters.isValidNameStart('0'), is(false));
        assertThat(XmlCharacters.isValidNameStart('1'), is(false));
        assertThat(XmlCharacters.isValidNameStart('2'), is(false));
        assertThat(XmlCharacters.isValidNameStart('3'), is(false));
        assertThat(XmlCharacters.isValidNameStart('4'), is(false));
        assertThat(XmlCharacters.isValidNameStart('5'), is(false));
        assertThat(XmlCharacters.isValidNameStart('6'), is(false));
        assertThat(XmlCharacters.isValidNameStart('7'), is(false));
        assertThat(XmlCharacters.isValidNameStart('8'), is(false));
        assertThat(XmlCharacters.isValidNameStart('9'), is(false));
    }

    @Test
    public void shouldAllowLettersAsFirstCharacterInName() {
        for (char c = 'a'; c <= 'z'; ++c) {
            assertThat(XmlCharacters.isValidNameStart(c), is(true));
        }
        for (char c = 'A'; c <= 'Z'; ++c) {
            assertThat(XmlCharacters.isValidNameStart(c), is(true));
        }
    }

    @Test
    public void shouldNotAllowDigitAsFirstCharacterInNcName() {
        assertThat(XmlCharacters.isValidNcNameStart('0'), is(false));
        assertThat(XmlCharacters.isValidNcNameStart('1'), is(false));
        assertThat(XmlCharacters.isValidNcNameStart('2'), is(false));
        assertThat(XmlCharacters.isValidNcNameStart('3'), is(false));
        assertThat(XmlCharacters.isValidNcNameStart('4'), is(false));
        assertThat(XmlCharacters.isValidNcNameStart('5'), is(false));
        assertThat(XmlCharacters.isValidNcNameStart('6'), is(false));
        assertThat(XmlCharacters.isValidNcNameStart('7'), is(false));
        assertThat(XmlCharacters.isValidNcNameStart('8'), is(false));
        assertThat(XmlCharacters.isValidNcNameStart('9'), is(false));
    }
}
