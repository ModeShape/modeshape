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
import java.util.BitSet;

public class QuoteEncoder implements TextDecoder, TextEncoder {

    private static final BitSet ESCAPE_CHARACTERS = new BitSet(256);

    public static final char ESCAPE_CHARACTER = '\\';

    static {
        ESCAPE_CHARACTERS.set('"');
        ESCAPE_CHARACTERS.set('\\');
        ESCAPE_CHARACTERS.set('\n');
        ESCAPE_CHARACTERS.set('\t');
    }

    public QuoteEncoder() {
    }

    @Override
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

    @Override
    public String encode( String text ) {
        final StringBuilder result = new StringBuilder();
        final CharacterIterator iter = new StringCharacterIterator(text);
        for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
            if (ESCAPE_CHARACTERS.get(c)) {
                result.append(ESCAPE_CHARACTER);
                if (c == '\n') {
                    result.append('n');
                } else if (c == '\t') {
                    result.append('t');
                } else if (c == '\r') {
                    result.append('r');
                } else if (c == '\f') {
                    result.append('f');
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

}
