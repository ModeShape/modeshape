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
import java.util.LinkedHashMap;
import java.util.Map;

public final class Metadata implements Externalizable {

    /* These are not final because of the #readExternal method */
    private long length;
    private int numberChunks;
    private Map<String, Integer> chunks;
    private long modificationTime;
    private long unusedSince;
    private String mimeType;
    private int numberTextChunks;

    public Metadata() {
    }

    public Metadata( long modificationTime,
                     long length,
                     Map<String, Integer> chunks ) {
        this.length = length;
        this.modificationTime = modificationTime;
        this.chunks = chunks;
        this.numberChunks = chunks.size();
        this.unusedSince = 0L;
        this.mimeType = null;
        this.numberTextChunks = 0;
    }

    private Metadata( long modificationTime,
                      long length,
                      Map<String, Integer> chunks,
                      int numberOfChunks,
                      long unusedSince,
                      String mimeType,
                      int numberTextChunks ) {
        this.length = length;
        this.modificationTime = modificationTime;
        this.chunks = chunks;
        this.numberChunks = numberOfChunks;
        this.unusedSince = unusedSince;
        this.mimeType = mimeType;
        this.numberTextChunks = numberTextChunks;
    }

    private Metadata( Metadata metadata ) {
        length = metadata.length;
        chunks = metadata.chunks;
        numberChunks = metadata.numberChunks;
        modificationTime = metadata.modificationTime;
        unusedSince = metadata.unusedSince;
        mimeType = metadata.mimeType;
        numberTextChunks = metadata.numberTextChunks;
    }

    public Metadata copy() {
        return new Metadata(this);
    }

    public long getLength() {
        return length;
    }

    public long getModificationTime() {
        return modificationTime;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Metadata withMimeType( String mimeType ) {
        return new Metadata(modificationTime, length, chunks, numberChunks, unusedSince, mimeType, numberTextChunks);
    }

    public int getNumberTextChunks() {
        return numberTextChunks;
    }

    public Metadata withNumberOfTextChunks( int numberTextChunks ) {
        return new Metadata(modificationTime, length, chunks, numberChunks, unusedSince, mimeType, numberTextChunks);
    }

    public int getNumberChunks() {
        return numberChunks;
    }

    public boolean isUnused() {
        return unusedSince > 0;
    }

    public void markAsUnusedSince( long unusedSince ) {
        this.unusedSince = unusedSince;
    }

    public void markAsUsed() {
        this.unusedSince = 0L;
    }

    /**
     * @return unused time in MS or 0 if still in use
     */
    public long unusedSince() {
        return unusedSince;
    }

    @Override
    public void writeExternal( ObjectOutput out ) throws IOException {
        // In version 2 was introduced the serialization of all referenced chunks and their size.
        if(chunks == null){
            // write version 1
            out.writeShort(1);
            out.writeLong(length);
            out.writeInt(numberChunks);
        } else {
            // write version 2
            out.writeShort(2);
            out.writeLong(length);
            out.writeInt(chunks.size());
            // chunkkey<->size pairs
            if(chunks.size() > 0){
                for(Map.Entry<String, Integer> entry : chunks.entrySet()){
                    out.writeUTF(entry.getKey());
                    out.writeInt(entry.getValue());
                }
            }
        }

        out.writeLong(modificationTime);
        out.writeLong(unusedSince);
        out.writeInt(numberTextChunks);
        if (mimeType != null) {
            out.writeBoolean(true);
            out.writeUTF(mimeType);
        } else {
            out.writeBoolean(false);
        }
    }

    @Override
    public void readExternal( ObjectInput in ) throws IOException {
        short version = in.readShort();
        length = in.readLong();
        numberChunks = in.readInt();
        if(version != 1){
            // read chunkkey <-> size pairs
            chunks = new LinkedHashMap<String, Integer>(numberChunks);
            for(int i = 0; i < numberChunks; i++){
                chunks.put(in.readUTF(), in.readInt());
            }
        }
        modificationTime = in.readLong();
        unusedSince = in.readLong();
        numberTextChunks = in.readInt();
        if (in.readBoolean()) {
            mimeType = in.readUTF();
        }
    }
}
