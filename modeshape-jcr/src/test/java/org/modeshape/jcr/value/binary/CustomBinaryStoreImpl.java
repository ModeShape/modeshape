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
    public BinaryValue storeValue( InputStream stream, boolean markAsUnused ) {
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
    public void markAsUsed( Iterable<BinaryKey> keys ) {
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
    protected String getStoredMimeType( BinaryValue binaryValue ) {
        return null;
    }

    @Override
    protected void storeMimeType( BinaryValue binaryValue,
                                  String mimeType ) {
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
