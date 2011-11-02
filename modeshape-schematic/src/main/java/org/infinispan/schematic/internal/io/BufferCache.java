/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.schematic.internal.io;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import org.infinispan.schematic.document.NotThreadSafe;

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
        if (byteBuffer.capacity() < MAXIMUM_SIZE) {
            this.charBuffer = charBuffer;
        }
    }
}
