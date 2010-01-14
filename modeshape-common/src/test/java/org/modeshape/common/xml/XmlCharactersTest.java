/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
