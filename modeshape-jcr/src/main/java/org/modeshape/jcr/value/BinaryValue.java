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
import org.modeshape.jcr.api.Binary;

/**
 * Value holder for binary data. BinaryValue extends the public {@link Binary} interface (which itself extends
 * {@link javax.jcr.Binary}) and adds requirements such as being serializable and comparable.
 */
@Immutable
public interface BinaryValue extends Comparable<BinaryValue>, Serializable, org.modeshape.jcr.api.Binary {

    /**
     * Get the length of this binary data.
     * <p>
     * Note that this method, unlike the standard {@link javax.jcr.Binary#getSize()} method, does not throw an exception.
     * </p>
     * 
     * @return the number of bytes in this binary data
     */
    @Override
    public long getSize();

    /**
     * Get the key for the binary value.
     * 
     * @return the key; never null
     */
    public BinaryKey getKey();
}
