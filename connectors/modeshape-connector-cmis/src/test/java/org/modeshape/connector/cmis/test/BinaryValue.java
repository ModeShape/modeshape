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
package org.modeshape.connector.cmis.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.data.ContentStream;

/**
 *
 * @author kulikov
 */
public class BinaryValue implements ContentStream {

    private long length;
    private String mimeType;
    private String fileName;

    private byte[] data;

    public BinaryValue(ContentStream stream) throws IOException {
        this.length = stream.getLength();
        this.mimeType = stream.getMimeType();
        this.fileName = stream.getFileName();
        this.copyData(stream.getStream());
    }

    private BinaryValue(byte[] data, long length, String mimeType, String fileName) {
        this.data = data;
        this.length = length;
        this.mimeType = mimeType;
        this.fileName = fileName;
    }

    @Override
    public long getLength() {
        return length;
    }

    @Override
    public BigInteger getBigLength() {
        return BigInteger.valueOf(length);
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public InputStream getStream() {
        return new ByteArrayInputStream(data);
    }

    @Override
    public List<CmisExtensionElement> getExtensions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setExtensions(List<CmisExtensionElement> list) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void copyData(InputStream in) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int b = 0;
        while (b != -1) {
            b = in.read();
            if (b != -1) {
                bout.write(b);
            }
        }
        data = bout.toByteArray();
    }
 }
