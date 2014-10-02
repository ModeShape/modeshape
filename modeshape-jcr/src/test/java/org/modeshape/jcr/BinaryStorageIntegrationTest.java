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

package org.modeshape.jcr;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.junit.Assert;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.binary.BinaryStore;

/**
 * Test suite that should include test cases which verify that a repository configured with various binary stores, correctly
 * stores the binary values.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class BinaryStorageIntegrationTest extends SingleUseAbstractTest {

    private static final char[] CHARS = new char[] {'a', 'b', 'c'};
    private static final Random RANDOM = new Random();

    @Override
    public void beforeEach() throws Exception {
        FileUtil.delete("target/persistent_repository");
        super.beforeEach();
    }

    @Test
    @FixFor( "MODE-1786" )
    public void shouldStoreBinariesIntoJDBCBinaryStore() throws Exception {
        startRepositoryWithConfiguration(resourceStream("config/repo-config-jdbc-binary-storage.json"));
        byte[] data = randomBytes(4 * 1024);
        storeBinaryAndAssert(data, "node");
        storeStringsAndAssert("stringNode");
    }

    @Test
    @FixFor("MODE-2051")
    public void shouldStoreBinariesIntoCacheBinaryStoreWithTransientRepository() throws Exception {
        startRepositoryWithConfiguration(resourceStream("config/repo-config-cache-binary-storage.json"));
        byte[] smallData = randomBytes(100);
        storeBinaryAndAssert(smallData, "smallNode");
        byte[] largeData = randomBytes(3 * 1025);
        storeBinaryAndAssert(largeData, "largeNode");
        storeStringsAndAssert("stringNode");
    }

    @Test
    @FixFor("MODE-2051")
    public void shouldStoreBinariesIntoSameCacheBinaryStoreAsRepository() throws Exception {
        FileUtil.delete("target/persistent_repository");
        startRepositoryWithConfiguration(resourceStream(
                "config/repo-config-cache-persistent-binary-storage-same-location.json"));
        byte[] smallData = randomBytes(100);
        storeBinaryAndAssert(smallData, "smallNode");
        byte[] largeData = randomBytes(3 * 1024);
        storeBinaryAndAssert(largeData, "largeNode");
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
    @FixFor( "MODE-2200" )
    public void shouldDecrementBinaryRefCountsWithFederations() throws Exception {
        new File("target/federation_persistent_3").mkdir();
        startRepositoryWithConfiguration(resourceStream("config/repo-config-persistent-infinispan-fs-connector3.json"));

        byte[] data = randomBytes(10);
        storeBinaryProperty(data, "skipNode1");
        jcrSession().getNode("/skipNode1").remove();
        jcrSession().save();
        FileUtil.delete("target/persistent_repository/");
    }

    @Test
    @FixFor( "MODE-2144" )
    public void shouldCleanupUnusedBinariesForFilesystemStore() throws Exception {
        startRepositoryWithConfiguration(resourceStream("config/repo-config-persistent-cache.json"));
        checkUnusedBinariesAreCleanedUp();
    }

    @Test
    @FixFor( "MODE-2302" )
    public void shouldReuseBinariesFromTrashForFilesystemStore() throws Exception {
        startRepositoryWithConfiguration(resourceStream("config/repo-config-persistent-cache.json"));
        int repCount = 5;
        for (int i = 0; i < repCount; i++) {
            // upload a file which should mark the binary as used
            tools.uploadFile(session, "/file1.txt", resourceStream("io/file1.txt"));
            session.save();
            BinaryValue storedValue = (BinaryValue)session.getProperty("/file1.txt/jcr:content/jcr:data").getBinary();
            BinaryKey key = storedValue.getKey();
            assertTrue("Binary not stored", binaryStore().hasBinary(key));

            // remove the file, which should move the binary to trash
            session.getNode("/file1.txt").remove();
            session.save();
        }
    }

    @Test
    @FixFor( "MODE-2144" )
    public void shouldCleanupUnusedBinariesForDatabaseStore() throws Exception {
        startRepositoryWithConfiguration(resourceStream("config/repo-config-jdbc-binary-storage-other.json"));
        checkUnusedBinariesAreCleanedUp();
    }

    @Test
    @FixFor( "MODE-2144" )
    public void shouldCleanupUnusedBinariesForCacheBinaryStore() throws Exception {
        startRepositoryWithConfiguration(resourceStream("config/repo-config-cache-binary-storage.json"));
        checkUnusedBinariesAreCleanedUp();
    }

    @Test
    @FixFor( "MODE-2303" )
    public void binaryUsageShouldChangeAfterSavingFS() throws Exception {
        startRepositoryWithConfiguration(resourceStream("config/repo-config-persistent-cache.json"));
        checkBinaryUsageAfterSaving();
    }

    @Test
    @FixFor( "MODE-2303" )
    public void binaryUsageShouldChangeAfterSavingInfinispan() throws Exception {
        startRepositoryWithConfiguration(resourceStream("config/repo-config-cache-binary-storage.json"));
        checkBinaryUsageAfterSaving();
    }

    @Test
    @FixFor( "MODE-2303" )
    public void binaryUsageShouldChangeAfterSavingJDBC() throws Exception {
        startRepositoryWithConfiguration(resourceStream("config/repo-config-jdbc-binary-storage.json"));
        checkBinaryUsageAfterSaving();
    }

    private String randomString(long size) {
        StringBuilder builder = new StringBuilder("");
        while (builder.length() < size) {
            builder.append(CHARS[RANDOM.nextInt(3)]);
        }
        return builder.toString();
    }

    private void storeStringsAndAssert( String stringNode ) throws Exception {
        Node testRoot = jcrSession().getRootNode().addNode(stringNode);

        long minStringSize = repository.getConfiguration().getBinaryStorage().getMinimumStringSize();
        //the small string should be stored as a string
        String smallString = randomString(minStringSize - 1);
        testRoot.setProperty("smallString", smallString);

        //the large string should be stored as a binary
        String largeString = randomString(minStringSize + 1);
        testRoot.setProperty("largeString", largeString);

        jcrSession().save();

        //use a separate session to validate because the original one still caches the properties as string...
        JcrSession readerSession = repository.login();
        try {
            Property smallStringProperty = readerSession.getProperty("/" + stringNode + "/smallString");
            assertEquals("Small string should've been stored as a string", PropertyType.STRING, smallStringProperty.getType());
            assertEquals("Incorrect stored string value", smallString, smallStringProperty.getString());

            Property largeStringProperty = readerSession.getProperty("/" + stringNode + "/largeString");
            assertEquals("Large string should've been stored as a binary", PropertyType.BINARY, largeStringProperty.getType());
            String binaryStringValue = IoUtil.read(largeStringProperty.getBinary().getStream());
            assertEquals("Incorrect stored string value", largeString, binaryStringValue);
        } finally {
            readerSession.logout();
        }
    }

    private void checkBinaryUsageAfterSaving() throws Exception {
        assertEquals("There should be no binaries in store", 0, binariesCount());

        // upload 2 binaries but don't save
        tools.uploadFile(session, "/file1.txt", resourceStream("io/file1.txt"));
        tools.uploadFile(session, "/file2.txt", resourceStream("io/file2.txt"));

        // run a cleanup
        Thread.sleep(2);
        binaryStore().removeValuesUnusedLongerThan(1, TimeUnit.MILLISECONDS);

        // check that the binaries have been removed
        assertEquals("There should be no binaries in store", 0, binariesCount());

        // discard all previous changes
        session.refresh(false);

        // upload a new file
        tools.uploadFile(session, "/file3.txt", resourceStream("io/file3.txt"));

        // and save
        session.save();

        // now check there is a binary
        assertEquals(1, binariesCount());

        // run a cleanup
        Thread.sleep(2);
        binaryStore().removeValuesUnusedLongerThan(1, TimeUnit.MILLISECONDS);

        //check the binary are still there
        assertEquals(1, binariesCount());
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
            // sleep to give the binary change listener to mark the binaries as unused
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

    private void storeBinaryAndAssert( byte[] data,
                                       String nodeName ) throws RepositoryException, IOException {
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
