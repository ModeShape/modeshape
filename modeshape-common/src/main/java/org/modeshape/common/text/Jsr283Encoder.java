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

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import org.modeshape.common.annotation.Immutable;

/**
 * Encoder that escapes characters that are not allowed in JCR names. The mapping defined in Section 3.6.3 of the JSR-283 public
 * review document:
 * <table cellspacing="0" cellpadding="1" border="1">
 * <tr>
 * <th>Non-JCR character<br/>
 * (Unicode code point)</th>
 * <th>Private use<br/>
 * Unicode code point</th>
 * </tr>
 * <tr>
 * <td>(U+002A)</td>
 * <td>U+F02A</td>
 * </tr>
 * <tr>
 * <td>/ (U+002F)</td>
 * <td>U+F02F</td>
 * </tr>
 * <tr>
 * <td>: (U+003A)</td>
 * <td>U+F03A</td>
 * </tr>
 * <tr>
 * <td>[ (U+005B)</td>
 * <td>U+F05B</td>
 * </tr>
 * <tr>
 * <td>] (U+005D)</td>
 * <td>U+F05D</td>
 * </tr>
 * <tr>
 * <td>| (U+007C)</td>
 * <td>U+F07C</td>
 * </tr>
 * </table>
 * </p>
 */
@Immutable
public class Jsr283Encoder implements TextEncoder, TextDecoder {

    public static boolean containsEncodeableCharacters( String str ) {
        CharacterIterator iter = new StringCharacterIterator(str);
        for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
            if (c == '*' || c == '/' || c == ':' || c == '[' || c == ']' || c == '|') return true;
        }
        return false;

    }

    @Override
    public String encode( String publicName ) {
        if (publicName == null) return null;
        StringBuilder sb = new StringBuilder();
        CharacterIterator iter = new StringCharacterIterator(publicName);
        for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
            char mapped = c;
            if (c == '*') {
                mapped = '\uF02A';
            } else if (c == '/') {
                mapped = '\uF02F';
            } else if (c == ':') {
                mapped = '\uF03A';
            } else if (c == '[') {
                mapped = '\uF05B';
            } else if (c == ']') {
                mapped = '\uF05D';
            } else if (c == '|') {
                mapped = '\uF07C';
            }
            sb.append(mapped);
        }
        return sb.toString();
    }

    @Override
    public String decode( String jcrNodeName ) {
        if (jcrNodeName == null) return null;
        StringBuilder sb = new StringBuilder();
        CharacterIterator iter = new StringCharacterIterator(jcrNodeName);
        for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
            char mapped = c;
            if (c == '\uF02A') {
                mapped = '*';
            } else if (c == '\uF02F') {
                mapped = '/';
            } else if (c == '\uF03A') {
                mapped = ':';
            } else if (c == '\uF05B') {
                mapped = '[';
            } else if (c == '\uF05D') {
                mapped = ']';
            } else if (c == '\uF07C') {
                mapped = '|';
            }
            sb.append(mapped);

        }
        return sb.toString();
    }

}
