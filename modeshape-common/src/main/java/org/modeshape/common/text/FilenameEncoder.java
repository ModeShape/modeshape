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

import java.util.BitSet;
import org.modeshape.common.annotation.Immutable;

/**
 * An encoder useful for converting text to be used within a filename on common file systems and operating systems, including
 * Linux, OS X, and Windows XP. This encoder is based upon the {@link UrlEncoder}, except that it removes the '*' character from
 * the list of safe characters.
 * 
 * @see UrlEncoder
 */
@Immutable
public class FilenameEncoder extends UrlEncoder {

    /**
     * Data characters that are allowed in a URI but do not have a reserved purpose are called unreserved. These include upper and
     * lower case letters, decimal digits, and a limited set of punctuation marks and symbols.
     * 
     * <pre>
     * unreserved  = alphanum | mark
     * mark        = &quot;-&quot; | &quot;_&quot; | &quot;.&quot; | &quot;!&quot; | &quot;&tilde;&quot; | &quot;'&quot; | &quot;(&quot; | &quot;)&quot;
     * </pre>
     * 
     * Unreserved characters can be escaped without changing the semantics of the URI, but this should not be done unless the URI
     * is being used in a context that does not allow the unescaped character to appear.
     */
    private static final BitSet SAFE_CHARACTERS = new BitSet(256);
    private static final BitSet SAFE_WITH_SLASH_CHARACTERS;

    public static final char ESCAPE_CHARACTER = '%';

    static {
        SAFE_CHARACTERS.set('a', 'z' + 1);
        SAFE_CHARACTERS.set('A', 'Z' + 1);
        SAFE_CHARACTERS.set('0', '9' + 1);
        SAFE_CHARACTERS.set('-');
        SAFE_CHARACTERS.set('_');
        SAFE_CHARACTERS.set('.');
        SAFE_CHARACTERS.set('!');
        SAFE_CHARACTERS.set('~');
        SAFE_CHARACTERS.set('\'');
        SAFE_CHARACTERS.set('(');
        SAFE_CHARACTERS.set(')');

        SAFE_WITH_SLASH_CHARACTERS = (BitSet)SAFE_CHARACTERS.clone();
        SAFE_WITH_SLASH_CHARACTERS.set('/');
    }

    @Override
    public String encode( String text ) {
        if (text == null) return null;
        if (text.length() == 0) return text;
        return encode(text, isSlashEncoded() ? SAFE_CHARACTERS : SAFE_WITH_SLASH_CHARACTERS);
    }

    /**
     * Set whether this encoder should use slash encoding.
     * 
     * @param slashEncoded Sets slashEncoded to the specified value.
     * @return this object, for method chaining
     */
    @Override
    public FilenameEncoder setSlashEncoded( boolean slashEncoded ) {
        super.setSlashEncoded(slashEncoded);
        return this;
    }

}
