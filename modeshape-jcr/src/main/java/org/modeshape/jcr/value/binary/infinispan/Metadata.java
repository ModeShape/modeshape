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

public class Metadata implements Externalizable {

    protected long length;
    protected int numberChunks;
    protected long modificationTime;
    protected long unusedSince;
    protected String mimeType;
    protected int numberTextChunks;

    public Metadata() {
    }

    public Metadata( Metadata metadata ) {
        length = metadata.length;
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

    public void setLength( long length ) {
        this.length = length;
    }

    public long getModificationTime() {
        return modificationTime;
    }

    public void setModificationTime( long modificationTime ) {
        this.modificationTime = modificationTime;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType( String mimeType ) {
        this.mimeType = mimeType;
    }

    public int getNumberTextChunks() {
        return numberTextChunks;
    }

    public void setNumberTextChunks( int numberTextChunks ) {
        this.numberTextChunks = numberTextChunks;
    }

    public int getNumberChunks() {
        return numberChunks;
    }

    public void setNumberChunks( int numberChunks ) {
        this.numberChunks = numberChunks;
    }

    public boolean isUnused() {
        return unusedSince > 0;
    }

    /**
     * @return unused time in MS or 0 if still in use
     */
    public long unusedSince() {
        return unusedSince;
    }

    public void setUnused() {
        unusedSince = System.currentTimeMillis();
    }

    public void setUsed() {
        unusedSince = 0;
    }

    @Override
    public void writeExternal( ObjectOutput out ) throws IOException {
        out.writeShort(1); // take 1st value as version number (maybe data format changes in future)
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
    }

    @Override
    public void readExternal( ObjectInput in ) throws IOException {
        in.readShort(); // ignore, no additional version ATM
        length = in.readLong();
        numberChunks = in.readInt();
        modificationTime = in.readLong();
        unusedSince = in.readLong();
        numberTextChunks = in.readInt();
        if (in.readBoolean()) {
            mimeType = in.readUTF();
        }
    }
}
