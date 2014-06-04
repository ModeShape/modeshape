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
package org.modeshape.jcr.value.binary;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.value.BinaryValue;

/**
 * An empty {@link BinaryValue} value.
 */
@Immutable
public final class EmptyBinaryValue extends AbstractBinary {

    public static final BinaryValue INSTANCE = new EmptyBinaryValue();

    private static final long serialVersionUID = 1L;

    private EmptyBinaryValue() {
        super(keyFor(EMPTY_CONTENT));
    }

    @Override
    public int compareTo( BinaryValue other ) {
        if (other == this) return 0;
        if (other instanceof EmptyBinaryValue) return 0;
        return super.compareTo(other);
    }

    @Override
    public long getSize() {
        return 0L;
    }

    @Override
    public String getMimeType() {
        // There is no mime type ...
        return null;
    }

    @Override
    public String getMimeType( String name ) {
        // There is no mime type ...
        return null;
    }

    @Override
    protected InputStream internalStream() {
        return new ByteArrayInputStream(EMPTY_CONTENT);
    }
}
