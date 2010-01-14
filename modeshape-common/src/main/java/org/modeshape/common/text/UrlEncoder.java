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
import net.jcip.annotations.Immutable;

/**
 * An encoder useful for converting text to be used within a URL, as defined by Section 2.3 of <a
 * href="http://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a>. Note that this class does not encode a complete URL (
 * {@link java.net.URLEncoder} and {@link java.net.URLDecoder} should be used for such purposes).
 */
@Immutable
public class UrlEncoder implements TextEncoder, TextDecoder {

    /**
     * Data characters that are allowed in a URI but do not have a reserved purpose are called unreserved. These include upper and
     * lower case letters, decimal digits, and a limited set of punctuation marks and symbols.
     * 
     * <pre>
     * unreserved  = alphanum | mark
     * mark        = &quot;-&quot; | &quot;_&quot; | &quot;.&quot; | &quot;!&quot; | &quot;&tilde;&quot; | &quot;*&quot; | &quot;'&quot; | &quot;(&quot; | &quot;)&quot;
     * </pre>
     * 
     * Unreserved characters can be escaped without changing the semantics of the URI, but this should not be done unless the URI
     * is being used in a context that does not allow the unescaped character to appear.
     */
    private static final BitSet RFC2396_UNRESERVED_CHARACTERS = new BitSet(256);
    private static final BitSet RFC2396_UNRESERVED_WITH_SLASH_CHARACTERS;

    public static final char ESCAPE_CHARACTER = '%';

    static {
        RFC2396_UNRESERVED_CHARACTERS.set('a', 'z' + 1);
        RFC2396_UNRESERVED_CHARACTERS.set('A', 'Z' + 1);
        RFC2396_UNRESERVED_CHARACTERS.set('0', '9' + 1);
        RFC2396_UNRESERVED_CHARACTERS.set('-');
        RFC2396_UNRESERVED_CHARACTERS.set('_');
        RFC2396_UNRESERVED_CHARACTERS.set('.');
        RFC2396_UNRESERVED_CHARACTERS.set('!');
        RFC2396_UNRESERVED_CHARACTERS.set('~');
        RFC2396_UNRESERVED_CHARACTERS.set('*');
        RFC2396_UNRESERVED_CHARACTERS.set('\'');
        RFC2396_UNRESERVED_CHARACTERS.set('(');
        RFC2396_UNRESERVED_CHARACTERS.set(')');

        RFC2396_UNRESERVED_WITH_SLASH_CHARACTERS = (BitSet)RFC2396_UNRESERVED_CHARACTERS.clone();
        RFC2396_UNRESERVED_WITH_SLASH_CHARACTERS.set('/');
    }

    private boolean slashEncoded = true;

    /**
     * {@inheritDoc}
     */
    public String encode( String text ) {
        if (text == null) return null;
        if (text.length() == 0) return text;
        return encode(text, isSlashEncoded() ? RFC2396_UNRESERVED_CHARACTERS : RFC2396_UNRESERVED_WITH_SLASH_CHARACTERS);
    }

    protected String encode( String text,
                             BitSet safeChars ) {
        final StringBuilder result = new StringBuilder();
        final CharacterIterator iter = new StringCharacterIterator(text);
        for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
            if (safeChars.get(c)) {
                // Safe character, so just pass through ...
                result.append(c);
            } else {
                // The character is not a safe character, and must be escaped ...
                result.append(ESCAPE_CHARACTER);
                result.append(Character.toLowerCase(Character.forDigit(c / 16, 16)));
                result.append(Character.toLowerCase(Character.forDigit(c % 16, 16)));
            }
        }
        return result.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String decode( String encodedText ) {
        if (encodedText == null) return null;
        if (encodedText.length() == 0) return encodedText;
        final StringBuilder result = new StringBuilder();
        final CharacterIterator iter = new StringCharacterIterator(encodedText);
        for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
            if (c == ESCAPE_CHARACTER) {
                boolean foundEscapedCharacter = false;
                // Found the first character in a potential escape sequence, so grab the next two characters ...
                char hexChar1 = iter.next();
                char hexChar2 = hexChar1 != CharacterIterator.DONE ? iter.next() : CharacterIterator.DONE;
                if (hexChar2 != CharacterIterator.DONE) {
                    // We found two more characters, but ensure they form a valid hexadecimal number ...
                    int hexNum1 = Character.digit(hexChar1, 16);
                    int hexNum2 = Character.digit(hexChar2, 16);
                    if (hexNum1 > -1 && hexNum2 > -1) {
                        foundEscapedCharacter = true;
                        result.append((char)(hexNum1 * 16 + hexNum2));
                    }
                }
                if (!foundEscapedCharacter) {
                    result.append(c);
                    if (hexChar1 != CharacterIterator.DONE) result.append(hexChar1);
                    if (hexChar2 != CharacterIterator.DONE) result.append(hexChar2);
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * @return slashEncoded
     */
    public boolean isSlashEncoded() {
        return this.slashEncoded;
    }

    /**
     * @param slashEncoded Sets slashEncoded to the specified value.
     * @return this object, for method chaining
     */
    public UrlEncoder setSlashEncoded( boolean slashEncoded ) {
        this.slashEncoded = slashEncoded;
        return this;
    }

}
