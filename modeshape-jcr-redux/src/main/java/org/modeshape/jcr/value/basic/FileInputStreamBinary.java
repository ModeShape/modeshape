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
package org.modeshape.jcr.value.basic;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.GraphI18n;
import org.modeshape.jcr.value.Binary;

/**
 * An implementation of {@link Binary} that wraps a data stream.
 * <p/>
 * <b>NOTE: This is not a fully valid implementation of {@link Binary} as it is not Immutable.</b>
 */
@NotThreadSafe
public class FileInputStreamBinary extends AbstractBinary {

    /**
     * Version {@value} .
     */
    private static final long serialVersionUID = 2L;

    private final FileChannel channel;
    private byte[] sha1hash;
    private int hc;

    public FileInputStreamBinary( FileInputStream stream ) {
        super();
        CheckArg.isNotNull(stream, "stream");
        this.channel = stream.getChannel();
    }

    @Override
    public int hashCode() {
        if (sha1hash == null) {
            // Idempotent, so doesn't matter if we recompute in concurrent threads ...
            sha1hash = computeHash(getBytes());
            hc = sha1hash.hashCode();
        }
        return hc;
    }

    @Override
    public long getSize() {
        try {
            return channel.position(0).size();
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

    @Override
    public byte[] getHash() {
        if (sha1hash == null) {
            // Idempotent, so doesn't matter if we recompute in concurrent threads ...
            sha1hash = computeHash(getBytes());
            hc = sha1hash.hashCode();
        }
        return sha1hash;
    }

    @Override
    public byte[] getBytes() {
        try {
            if (channel.size() > Integer.MAX_VALUE) {
                throw new IllegalStateException(GraphI18n.streamTooLarge.text(channel.size()));
            }

            ByteBuffer bytes = channel.map(MapMode.READ_ONLY, 0, channel.size());

            byte[] buff = new byte[(int)channel.size()];
            bytes.get(buff);

            return buff;
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

    @Override
    public InputStream getStream() {
        try {
            channel.position(0);
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
        return Channels.newInputStream(channel);
    }
}
