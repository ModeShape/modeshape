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
import java.util.BitSet;

/**
 * 
 */
public class QuoteEncoder implements TextDecoder, TextEncoder {

    private static final BitSet ESCAPE_CHARACTERS = new BitSet(256);

    public static final char ESCAPE_CHARACTER = '\\';

    static {
        ESCAPE_CHARACTERS.set('"');
        ESCAPE_CHARACTERS.set('\\');
        ESCAPE_CHARACTERS.set('\n');
        ESCAPE_CHARACTERS.set('\t');
    }

    /**
     * 
     */
    public QuoteEncoder() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.text.TextDecoder#decode(java.lang.String)
     */
    public String decode( String encodedText ) {
        if (encodedText == null) return null;
        if (encodedText.length() == 0) return "";
        final StringBuilder result = new StringBuilder();
        final CharacterIterator iter = new StringCharacterIterator(encodedText);
        for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
            if (c == ESCAPE_CHARACTER) {
                // Eat this escape character, and process the next character ...
                char nextChar = iter.next();
                if (nextChar == 'n') {
                    result.append('\n');
                } else if (nextChar == 't') {
                    result.append('\t');
                } else if (nextChar == 'r') {
                    result.append('\r');
                } else if (nextChar == 'f') {
                    result.append('\f');
                } else {
                    result.append(nextChar);
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.text.TextEncoder#encode(java.lang.String)
     */
    public String encode( String text ) {
        final StringBuilder result = new StringBuilder();
        final CharacterIterator iter = new StringCharacterIterator(text);
        for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
            if (ESCAPE_CHARACTERS.get(c)) {
                result.append(ESCAPE_CHARACTER);
                if (c == '\n') {
                    c = 'n';
                } else if (c == '\t') {
                    c = 't';
                } else if (c == '\r') {
                    c = 'r';
                } else if (c == '\f') {
                    c = 'f';
                }
            }
            result.append(c);
        }
        return result.toString();
    }

}
