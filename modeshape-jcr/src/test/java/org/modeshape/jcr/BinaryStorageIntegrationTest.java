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

package org.modeshape.jcr;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.junit.Assert;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.BinaryStore;

/**
 * Test suite that should include test cases which verify that a repository configured with various binary stores, correctly
 * stores the binary values.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class BinaryStorageIntegrationTest extends SingleUseAbstractTest {

    private static final Random RANDOM = new Random();

    @Test
    @FixFor( "MODE-1786" )
    public void shouldStoreBinariesIntoJDBCBinaryStore() throws Exception {
        startRepositoryWithConfiguration(resourceStream("config/repo-config-jdbc-binary-storage.json"));
        byte[] data = randomBytes(4 * 1024);
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

    @Test
    @FixFor( "MODE-2144" )
    public void shouldCleanupUnusedBinariesForFilesystemStore() throws Exception {
        FileUtil.delete("target/persistent_repository");
        startRepositoryWithConfiguration(resourceStream("config/repo-config-persistent-cache.json"));

        checkUnusedBinariesAreCleanedUp();
    }

    @Test
    @FixFor( "MODE-2144" )
    public void shouldCleanupUnusedBinariesForDatabaseStore() throws Exception {
        startRepositoryWithConfiguration(resourceStream("config/repo-config-jdbc-binary-storage.json"));
        checkUnusedBinariesAreCleanedUp();
    }

    @Test
    @FixFor( "MODE-2144" )
    public void shouldCleanupUnusedBinariesForCacheBinaryStore() throws Exception {
        FileUtil.delete("target/persistent_repository");
        startRepositoryWithConfiguration(resourceStream("config/repo-config-cache-binary-storage.json"));
        checkUnusedBinariesAreCleanedUp();
    }

    private void checkUnusedBinariesAreCleanedUp() throws Exception {
        Session session = repository.login();

        try {
            assertEquals("There should be no binaries in store", 0, binariesCount());
            tools.uploadFile(session, "/file1.txt", resourceStream("io/file1.txt"));
            tools.uploadFile(session, "/file2.txt", resourceStream("io/file2.txt"));
            Node node3 = session.getRootNode().addNode("file3");
            node3.setProperty("binary", session.getValueFactory().createBinary(resourceStream("io/file3.txt")));
            session.save();

            //verify the mime-type has been extracted for the 2 nt:resources
            Binary file1Binary = (Binary)session.getNode("/file1.txt/jcr:content").getProperty("jcr:data").getBinary();
            assertEquals("Invalid mime-type", "text/plain", file1Binary.getMimeType());

            Binary file2Binary = (Binary)session.getNode("/file2.txt/jcr:content").getProperty("jcr:data").getBinary();
            assertEquals("Invalid mime-type", "text/plain", file2Binary.getMimeType());

            //expect 3 binaries
            assertEquals("Incorrect number of binaries in store", 3, binariesCount());

            session.removeItem("/file1.txt");
            session.removeItem("/file2.txt");
            session.save();
            //sleep to give the binary change listener to mark the binaries as unused
            Thread.sleep(100);
            binaryStore().removeValuesUnusedLongerThan(1, TimeUnit.MILLISECONDS);

            //expect 1 binary
            assertEquals("Incorrect number of binaries in store", 1, binariesCount());

            session.removeItem("/file3/binary");
            session.save();

            //sleep to give the binary change listener to mark the binaries as unused
            Thread.sleep(100);
            binaryStore().removeValuesUnusedLongerThan(1, TimeUnit.MILLISECONDS);

            assertEquals("There should be no binaries in store", 0, binariesCount());
        } finally {
            session.logout();
        }
    }

    private int binariesCount() throws Exception {
        int count = 0;
        for (BinaryKey binaryKey : binaryStore().getAllBinaryKeys()) {
            count++;
        }
        return count;
    }

    private BinaryStore binaryStore() {
        return repository.runningState().binaryStore();
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
