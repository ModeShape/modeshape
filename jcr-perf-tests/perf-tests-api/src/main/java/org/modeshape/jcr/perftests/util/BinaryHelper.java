/*
 * JBoss, Home of Professional Open Source
 * Copyright [2011], Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
