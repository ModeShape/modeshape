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
package org.modeshape.jcr.value.binary;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

/**
 * @author kulikov
 */
public class CustomBinaryStoreImpl extends AbstractBinaryStore {
    private String provisionedValue;
    private String key;

    public String getProvisionedValue() {
        return this.provisionedValue;
    }

    public String getKey() {
        return key;
    }

    @Override
    public BinaryValue storeValue( InputStream stream ) {
        return null;
    }

    @Override
    public InputStream getInputStream( BinaryKey key ) {
        return null;
    }

    @Override
    public void markAsUnused( Iterable<BinaryKey> keys ) {
    }

    @Override
    public void removeValuesUnusedLongerThan( long minimumAge,
                                              TimeUnit unit ) {
    }

    @Override
    public void storeExtractedText( BinaryValue source,
                                    String extractedText ) {
    }

    @Override
    protected String getStoredMimeType( BinaryValue binaryValue ) throws BinaryStoreException {
        return null;
    }

    @Override
    protected void storeMimeType( BinaryValue binaryValue,
                                  String mimeType ) throws BinaryStoreException {
    }

    @Override
    public String getExtractedText( BinaryValue source ) {
        return null;
    }

    @Override
    public Iterable<BinaryKey> getAllBinaryKeys() {
        return null;
    }

}
