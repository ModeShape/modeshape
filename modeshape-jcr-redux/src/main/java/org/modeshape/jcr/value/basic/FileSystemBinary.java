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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.value.Binary;
import org.modeshape.jcr.value.IoException;

/**
 * An implementation of {@link Binary} that is used to access the content of a file on the file system without holding any content
 * in-memory.
 */
@Immutable
public class FileSystemBinary extends AbstractBinary {
    /**
     * Version {@value} .
     */
    private static final long serialVersionUID = 1L;

    private File file;
    private byte[] sha1hash;
    private int hc;

    public FileSystemBinary( File file ) {
        super();
        CheckArg.isNotNull(file, "file");
        this.file = file;
    }

    @Override
    public int hashCode() {
        if (sha1hash == null) {
            // Idempotent, so doesn't matter if we recompute in concurrent threads ...
            sha1hash = computeHash(file);
            hc = sha1hash.hashCode();
        }
        return hc;
    }

    @Override
    public long getSize() {
        return this.file.length();
    }

    @Override
    public byte[] getHash() {
        if (sha1hash == null) {
            // Idempotent, so doesn't matter if we recompute in concurrent threads ...
            sha1hash = computeHash(file);
            hc = sha1hash.hashCode();
        }
        return sha1hash;
    }

    @Override
    public byte[] getBytes() {
        try {
            return IoUtil.readBytes(file);
        } catch (IOException e) {
            throw new IoException(e);
        }
    }

    @Override
    public InputStream getStream() {
        try {
            return new BufferedInputStream(new FileInputStream(this.file));
        } catch (IOException e) {
            throw new IoException(e);
        }
    }

    private void writeObject( ObjectOutputStream out ) throws IOException {
        out.writeUTF(this.file.getPath());
    }

    private void readObject( ObjectInputStream in ) throws IOException {
        String path = in.readUTF();
        this.file = new File(path);
    }
}
