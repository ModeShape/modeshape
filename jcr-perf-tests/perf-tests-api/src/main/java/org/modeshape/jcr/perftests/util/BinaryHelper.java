/*
 *
 *  * ModeShape (http://www.modeshape.org)
 *  * See the COPYRIGHT.txt file distributed with this work for information
 *  * regarding copyright ownership.  Some portions may be licensed
 *  * to Red Hat, Inc. under one or more contributor license agreements.
 *  * See the AUTHORS.txt file in the distribution for a full listing of
 *  * individual contributors.
 *  *
 *  * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 *  * is licensed to you under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * ModeShape is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */
package org.modeshape.jcr.perftests.util;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class used by tests when working with {@link javax.jcr.Binary} implementations
 *
 * @author Horia Chiorean
 */
public final class BinaryHelper {

    private BinaryHelper() {
    }

    /**
     * Asserts that the given binary source has the expected size (in bytes). The operation reads the data from the binary
     * instance into a byte array and checks the size of the byte array
     * @param source a <code>Binary</code> instance.
     * @param expectedSize the expected size of the binary
     */
    public static void assertExpectedSize( Binary source, int expectedSize ) throws RepositoryException, IOException {
        ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
        InputStream inputStream = source.getStream();
        try {
            byte[] buff = new byte[1000];
            int available;
            while ((available = inputStream.read(buff)) != -1) {
                byteArrayOutput.write(buff, 0, available);
            }
            assert byteArrayOutput.size() == expectedSize;
        } finally {
            inputStream.close();
            byteArrayOutput.close();
        }
    }
}
