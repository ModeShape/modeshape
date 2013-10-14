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
package org.modeshape.jcr.value.binary.infinispan;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Class which holds various information about the binary data that is stored in ISPN.
 */
public final class Metadata implements Externalizable {

    private static final int VERSION_1 = 1;
    private static final int VERSION_2 = 2;

    /**
     * since v1
     */
    private long length;
    private int numberChunks;
    private long modificationTime;
    private long unusedSince;
    private String mimeType;
    private int numberTextChunks;

    /**
     * since v2
     */
    private int chunkSize;

    /**
     * Public no-arg ctr, required by serialization
     */
    public Metadata() {
    }

    protected Metadata( long modificationTime,
                        long length,
                        int numberOfChunks,
                        int chunkSize) {
        this(modificationTime, length, numberOfChunks, 0, null, 0, chunkSize);
    }

    protected Metadata( long modificationTime,
                        long length,
                        int numberOfChunks,
                        long unusedSince,
                        String mimeType,
                        int numberTextChunks,
                        int chunkSize) {
        this.length = length;
        this.modificationTime = modificationTime;
        this.numberChunks = numberOfChunks;
        this.unusedSince = unusedSince;
        this.mimeType = mimeType;
        this.numberTextChunks = numberTextChunks;
        this.chunkSize = chunkSize;
    }

    protected long getLength() {
        return length;
    }

    protected String getMimeType() {
        return mimeType;
    }

    protected Metadata withMimeType( String mimeType ) {
        return new Metadata(modificationTime, length, numberChunks, unusedSince, mimeType, numberTextChunks, chunkSize);
    }

    protected int getNumberTextChunks() {
        return numberTextChunks;
    }

    protected Metadata withNumberOfTextChunks( int numberTextChunks ) {
        return new Metadata(modificationTime, length, numberChunks, unusedSince, mimeType, numberTextChunks, chunkSize);
    }

    protected int getNumberChunks() {
        return numberChunks;
    }

    protected boolean isUnused() {
        return unusedSince > 0;
    }

    protected void markAsUnusedSince( long unusedSince ) {
        this.unusedSince = unusedSince;
    }

    protected void markAsUsed() {
        this.unusedSince = 0L;
    }

    protected int getChunkSize() {
        return chunkSize;
    }

    /**
     * @return unused time in MS or 0 if still in use
     */
    protected long unusedSince() {
        return unusedSince;
    }

    @Override
    public void writeExternal( ObjectOutput out ) throws IOException {
        out.writeShort(VERSION_2); // take 1st value as version number
        out.writeLong(length);
        out.writeInt(numberChunks);
        out.writeLong(modificationTime);
        out.writeLong(unusedSince);
        out.writeInt(numberTextChunks);
        if (mimeType != null) {
            out.writeBoolean(true);
            out.writeUTF(mimeType);
        } else {
            out.writeBoolean(false);
        }
        out.writeInt(chunkSize);
    }

    @Override
    public void readExternal( ObjectInput in ) throws IOException {
        int version = in.readShort(); //read the version
        length = in.readLong();
        numberChunks = in.readInt();
        modificationTime = in.readLong();
        unusedSince = in.readLong();
        numberTextChunks = in.readInt();
        if (in.readBoolean()) {
            mimeType = in.readUTF();
        }
        switch (version) {
            case VERSION_1: {
                chunkSize = InfinispanBinaryStore.DEFAULT_CHUNK_SIZE;
                break;
            }
            case VERSION_2: {
                chunkSize = in.readInt();
                break;
            }
        }
    }
}
