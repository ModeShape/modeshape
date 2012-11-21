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
package org.modeshape.connector.git;

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
        try {
            binaryStore.getInputStream(key);
            return factories.getBinaryFactory().find(key, size);
        } catch (BinaryStoreException e) {
            // Must not have found it ...
        }
        return null;
    }

    public BinaryValue binaryFrom( InputStream stream ) {
        return factories.getBinaryFactory().create(stream);
    }

}
