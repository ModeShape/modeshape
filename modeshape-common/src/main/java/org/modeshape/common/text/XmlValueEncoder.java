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
import java.util.HashMap;
import java.util.Map;
import org.modeshape.common.annotation.Immutable;

/**
 * An encoder useful for converting text to be used within XML attribute values. The following translations will be performed:
 * <table cellspacing="0" cellpadding="1" border="1">
 * <tr>
 * <th>Raw (Unencoded)<br/>
 * Character</th>
 * <th>Translated (Encoded)<br/>
 * Entity</th>
 * </tr>
 * <tr>
 * <td>&amp;</td>
 * <td>&amp;amp;</td>
 * </tr>
 * <tr>
 * <td>&lt;</td>
 * <td>&amp;lt;</td>
 * </tr>
 * <tr>
 * <td>&gt;</td>
 * <td>&amp;gt;</td>
 * </tr>
 * <tr>
 * <td>&quot;</td>
 * <td>&amp;quot;</td>
 * </tr>
 * <tr>
 * <td>&#039;</td>
 * <td>&amp;#039;</td>
 * </tr>
 * <tr>
 * <td>All Others</td>
 * <td>No Translation</td>
 * </tr>
 * </table>
 * </p>
 */
@Immutable
public class XmlValueEncoder implements TextEncoder, TextDecoder {

    private static final Map<String, Character> SPECIAL_ENTITIES;

    static {
        SPECIAL_ENTITIES = new HashMap<String, Character>();

        SPECIAL_ENTITIES.put("quot", '"');
        SPECIAL_ENTITIES.put("gt", '>');
        SPECIAL_ENTITIES.put("lt", '<');
        SPECIAL_ENTITIES.put("amp", '&');

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.text.TextEncoder#encode(java.lang.String)
     */
    @Override
    public String encode( String text ) {
        if (text == null) return null;
        StringBuilder sb = new StringBuilder();
        CharacterIterator iter = new StringCharacterIterator(text);
        for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '\'':
                    sb.append("&#039;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.text.TextDecoder#decode(java.lang.String)
     */
    @Override
    public String decode( String encodedText ) {
        if (encodedText == null) return null;
        StringBuilder sb = new StringBuilder();
        CharacterIterator iter = new StringCharacterIterator(encodedText);
        for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
            if (c == '&') {
                int index = iter.getIndex();

                do {
                    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
                    c = iter.next();
                } while (c != CharacterIterator.DONE && c != ';');

                // We found a closing semicolon
                if (c == ';') {
                    String s = encodedText.substring(index + 1, iter.getIndex());

                    if (SPECIAL_ENTITIES.containsKey(s)) {
                        sb.append(SPECIAL_ENTITIES.get(s));
                        continue;

                    }

                    if (s.length() > 0 && s.charAt(0) == '#') {
                        try {
                            sb.append((char)Short.parseShort(s.substring(1, s.length())));
                            continue;
                        } catch (NumberFormatException nfe) {
                            // This is possible in malformed encodings, but let it fall through
                        }
                    }
                }

                // Malformed encoding, restore state and pass poorly encoded data back
                // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
                c = '&';
                iter.setIndex(index);
            }

            sb.append(c);

        }
        return sb.toString();
    }
}
