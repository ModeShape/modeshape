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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import org.junit.Assert;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.SingleUseAbstractTest;

/**
 * Test suite that should include test cases which verify that a repository configured with various binary stores, correctly
 * stores the binary values.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class BinaryStorageTest extends SingleUseAbstractTest {

    private static final Random RANDOM = new Random();

    @Test
    @FixFor( "MODE-1786" )
    public void shouldStoreBinariesIntoJDBCBinaryStore() throws Exception {
        startRepositoryWithConfiguration(resourceStream("config/repo-config-jdbc-binary-storage.json"));
        byte[] data = randomBytes(AbstractBinaryStore.MEDIUM_BUFFER_SIZE * 2);
        storeAndAssert(data, "node");
    }

    @Test
    @FixFor("MODE-2051")
    public void shouldStoreBinariesIntoCacheBinaryStoreWithTransientRepository() throws Exception {
        startRepositoryWithConfiguration(resourceStream("config/repo-config-cache-binary-storage.json"));
        byte[] smallData = randomBytes(100);
        storeAndAssert(smallData, "smallNode");
        byte[] largeData = randomBytes(3 * 1025);
        storeAndAssert(largeData, "largeNode");
    }

    @Test
    @FixFor("MODE-2051")
    public void shouldStoreBinariesIntoSameCacheBinaryStoreAsRepository() throws Exception {
        FileUtil.delete("target/persistent_repository");
        startRepositoryWithConfiguration(resourceStream(
                "config/repo-config-cache-persistent-binary-storage-same-location.json"));
        byte[] smallData = randomBytes(100);
        storeAndAssert(smallData, "smallNode");
        byte[] largeData = randomBytes(3 * 1024);
        storeAndAssert(largeData, "largeNode");
    }

    @Test
    @FixFor("MODE-1752")
    public void shouldCorrectlySkipBytesFromCacheBinaryStoreStream() throws Exception {
        startRepositoryWithConfiguration(resourceStream("config/repo-config-cache-binary-storage.json"));
        //chunk size is configured to 1000
        byte[] data = randomBytes(3003);
        InputStream inputStream = storeBinaryProperty(data, "skipNode1");
        assertEquals(2, inputStream.skip(2));
        assertEquals(1, inputStream.skip(1));
        assertEquals(1000, inputStream.skip(1000));
        assertEquals(1001, inputStream.skip(1001));
        assertEquals(999, inputStream.skip(1000));
        assertEquals(0, inputStream.skip(1));
        assertEquals(0, inputStream.skip(10));
        inputStream.close();

        inputStream = storeBinaryProperty(data, "skipNode2");
        assertEquals(3003, inputStream.skip(3003));
        assertEquals(0, inputStream.skip(1));
        assertEquals(0, inputStream.skip(10));
        inputStream.close();

        inputStream = storeBinaryProperty(data, "skipNode3");
        assertEquals(2000, inputStream.skip(2000));
        assertEquals(1003, inputStream.skip(1003));
        assertEquals(0, inputStream.skip(1));
        inputStream.close();
    }

    @Test
    @FixFor("MODE-1752")
    public void shouldCorrectlyReadBytesAfterSkippingFromCacheBinaryStoreStream() throws Exception {
        startRepositoryWithConfiguration(resourceStream("config/repo-config-cache-binary-storage.json"));
        //chunk size is configured to 1000
        byte[] data = randomBytes(3003);
        InputStream inputStream = storeBinaryProperty(data, "skipNode1");
        assertEquals(2, inputStream.skip(2));
        byte[] expected = new byte[data.length - 2];
        System.arraycopy(data, 2, expected, 0, data.length - 2);
        assertArrayEquals(expected, IoUtil.readBytes(inputStream));

        inputStream = storeBinaryProperty(data, "skipNode2");
        assertEquals(3003, inputStream.skip(3003));
        assertArrayEquals(new byte[0], IoUtil.readBytes(inputStream));

        inputStream = storeBinaryProperty(data, "skipNode3");
        assertEquals(2000, inputStream.skip(2000));
        expected = new byte[data.length - 2000];
        System.arraycopy(data, 2000, expected, 0, data.length - 2000);
        assertArrayEquals(expected, IoUtil.readBytes(inputStream));
    }

    private byte[] randomBytes(int size) {
        byte[] data = new byte[size];
        RANDOM.nextBytes(data);
        return data;
    }

    private void storeAndAssert(byte[] data, String nodeName) throws RepositoryException, IOException {
        InputStream stream = storeBinaryProperty(data, nodeName);
        byte[] storedData = IoUtil.readBytes(stream);
        assertArrayEquals("Data retrieved does not match data stored", data, storedData);
    }

    private InputStream storeBinaryProperty( byte[] data,
                                             String nodeName ) throws RepositoryException {
        Node testRoot = jcrSession().getRootNode().addNode(nodeName);
        testRoot.setProperty("binary", session.getValueFactory().createValue(new ByteArrayInputStream(data)));
        jcrSession().save();

        Property binary = jcrSession().getNode("/" + nodeName).getProperty("binary");
        Assert.assertNotNull(binary);
        return binary.getBinary().getStream();
    }
}
