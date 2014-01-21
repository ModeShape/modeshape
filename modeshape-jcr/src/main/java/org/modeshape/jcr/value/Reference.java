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
package org.modeshape.jcr.value;

import java.io.Serializable;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.text.TextEncoder;

/**
 * A representation of a reference to another node. Node references may not necessarily resolve to an existing node.
 */
@Immutable
public interface Reference extends Comparable<Reference>, Serializable {

    /**
     * Get the string form of the Reference. The {@link Path#DEFAULT_ENCODER default encoder} is used to encode characters in the
     * reference.
     * 
     * @return the encoded string
     * @see #getString(TextEncoder)
     */
    public String getString();

    /**
     * Get the encoded string form of the Reference, using the supplied encoder to encode characters in the reference.
     * 
     * @param encoder the encoder to use, or null if the {@link Path#DEFAULT_ENCODER default encoder} should be used
     * @return the encoded string
     * @see #getString()
     */
    public String getString( TextEncoder encoder );

    /**
     * Determine whether this reference is considered a weak reference.
     * 
     * @return true if this is a weak reference, or false otherwise
     */
    public boolean isWeak();

    /**
     * Determine whether this reference is a reference to a node which belongs to another source as the owning node.
     *
     * @return true if the reference is foreign, false otherwise
     */
    public boolean isForeign();

    /**
     * Determine whether this reference is a uni-directional reference
     *
     * @return true if this is a uni-directional, or false otherwise
     */
    public boolean isSimple();

}
