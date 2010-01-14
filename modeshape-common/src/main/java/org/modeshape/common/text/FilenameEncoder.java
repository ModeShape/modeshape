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

import java.util.BitSet;
import net.jcip.annotations.Immutable;

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

    /**
     * {@inheritDoc}
     */
    @Override
    public String encode( String text ) {
        if (text == null) return null;
        if (text.length() == 0) return text;
        return encode(text, isSlashEncoded() ? SAFE_CHARACTERS : SAFE_WITH_SLASH_CHARACTERS);
    }

    /**
     * @param slashEncoded Sets slashEncoded to the specified value.
     * @return this object, for method chaining
     */
    @Override
    public FilenameEncoder setSlashEncoded( boolean slashEncoded ) {
        super.setSlashEncoded(slashEncoded);
        return this;
    }

}
