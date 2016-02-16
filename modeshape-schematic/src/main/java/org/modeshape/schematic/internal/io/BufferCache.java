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
package org.modeshape.schematic.internal.io;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import org.modeshape.schematic.annotation.NotThreadSafe;

/**
 * A cache of ByteBuffer, used in {@link BsonDataInput}.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 */
@NotThreadSafe
public class BufferCache {
    /**
     * The minimum number of bytes in each buffer.
     */
    public static final int MINIMUM_SIZE = 1048 * 8;
    /**
     * The maximum number of bytes in each cached buffer. If a buffer is too large, then using it and caching it in-memory will be
     * too costly.
     */
    public static final int MAXIMUM_SIZE = 1048 * 80;

    private ByteBuffer byteBuffer = ByteBuffer.allocate(MINIMUM_SIZE);
    private CharBuffer charBuffer = CharBuffer.allocate(MINIMUM_SIZE);

    public ByteBuffer getByteBuffer( int minimumSize ) {
        minimumSize = Math.max(minimumSize, MINIMUM_SIZE);
        ByteBuffer buffer = byteBuffer;
        if (buffer == null || buffer.capacity() < minimumSize) {
            // Allocate a new one ...
            buffer = ByteBuffer.allocate(minimumSize);
        } else {
            // The existing one is good enough ...
            byteBuffer = null;
            buffer.clear();
        }
        return buffer;
    }

    public CharBuffer getCharBuffer( int minimumSize ) {
        minimumSize = Math.max(minimumSize, MINIMUM_SIZE);
        CharBuffer buffer = charBuffer;
        if (buffer == null || buffer.capacity() < minimumSize) {
            // Allocate a new one ...
            buffer = CharBuffer.allocate(minimumSize);
        } else {
            // The existing one is good enough ...
            charBuffer = null;
            // But be sure to clear it out ...
            buffer.clear();
        }
        return buffer;
    }

    public void checkin( ByteBuffer byteBuffer ) {
        if (byteBuffer.capacity() < MAXIMUM_SIZE) {
            this.byteBuffer = byteBuffer;
        }
    }

    public void checkin( CharBuffer charBuffer ) {
        if (charBuffer.capacity() < MAXIMUM_SIZE) {
            this.charBuffer = charBuffer;
        }
    }
}
