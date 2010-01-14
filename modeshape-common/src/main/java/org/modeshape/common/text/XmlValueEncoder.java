/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
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
package org.modeshape.common.text;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.HashMap;
import java.util.Map;
import net.jcip.annotations.Immutable;

/**
 * An encoder useful for converting text to be used within XML attribute values. The following translations will be performed:
 * <table cellspacing="0" cellpadding="1" border="1">
 * <tr>
 * <th>Raw (Unencoded)<br/>Character</th>
 * <th>Translated (Encoded)<br/>Entity</th>
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
    public String decode( String encodedText ) {
        if (encodedText == null) return null;
        StringBuilder sb = new StringBuilder();
        CharacterIterator iter = new StringCharacterIterator(encodedText);
        for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
            if (c == '&') {
                int index = iter.getIndex();

                do {
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
                c = '&';
                iter.setIndex(index);
            }

            sb.append(c);

        }
        return sb.toString();
    }
}
