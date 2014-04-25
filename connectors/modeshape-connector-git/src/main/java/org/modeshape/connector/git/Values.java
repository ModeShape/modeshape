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
package org.modeshape.connector.git;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.BinaryStoreException;

@Immutable
public class Values {

    private final ValueFactories factories;
    private final BinaryStore binaryStore;

    public Values( ValueFactories factories,
                   BinaryStore binaryStore ) {
        this.factories = factories;
        this.binaryStore = binaryStore;
    }

    public DateTime dateFrom( int secondsSinceEpoch ) {
        long millisSinceEpoch = TimeUnit.MILLISECONDS.convert(secondsSinceEpoch, TimeUnit.SECONDS);
        return factories.getDateFactory().create(millisSinceEpoch);
    }

    public Object referenceTo( String id ) {
        return factories.getReferenceFactory().create(id);
    }

    public BinaryValue binaryFor( BinaryKey key,
                                  long size ) {
        InputStream is = null;
        try {
            is = binaryStore.getInputStream(key);
            return factories.getBinaryFactory().find(key, size);
        } catch (BinaryStoreException e) {
            // Must not have found it ...
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
        return null;
    }

    public BinaryValue binaryFrom( InputStream stream ) {
        return factories.getBinaryFactory().create(stream);
    }

}
