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

import org.modeshape.common.annotation.Immutable;

/**
 * Interface for components that can decode text. This is the counterpart to {@link TextEncoder}.
 * 
 * @see TextEncoder
 */
@Immutable
public interface TextDecoder {

    /**
     * Return the decoded version of an encoded string
     * 
     * @param encodedText the encoded text
     * @return the unecoded form of the text, or null if the supplied node name is also null
     * @see TextEncoder#encode(String)
     */
    public String decode( String encodedText );

}
