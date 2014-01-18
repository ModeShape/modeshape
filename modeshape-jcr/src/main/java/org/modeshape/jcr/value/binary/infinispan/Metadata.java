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
