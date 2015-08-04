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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.tika.mime.MediaType;
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
    @FixFor( "MODE-2051" )
    public void shouldStoreBinariesIntoCacheBinaryStoreWithTransientRepository() throws Exception {
        startRepositoryWithConfiguration(resourceStream("config/repo-config-cache-binary-storage.json"));
        byte[] smallData = randomBytes(100);
        storeBinaryAndAssert(smallData, "smallNode");
        byte[] largeData = randomBytes(3 * 1025);
        storeBinaryAndAssert(largeData, "largeNode");
        storeStringsAndAssert("stringNode");
    }

    @Test
    @FixFor( "MODE-2051" )
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
    @FixFor( "MODE-1752" )
    public void shouldCorrectlySkipBytesFromCacheBinaryStoreStream() throws Exception {
        startRepositoryWithConfiguration(resourceStream("config/repo-config-cache-binary-storage.json"));
        // chunk size is configured to 1000
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
    @FixFor( "MODE-1752" )
    public void shouldCorrectlyReadBytesAfterSkippingFromCacheBinaryStoreStream() throws Exception {
        startRepositoryWithConfiguration(resourceStream("config/repo-config-cache-binary-storage.json"));
        // chunk size is configured to 1000
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
    
    @Test
    @FixFor( "MODE-2484 ")
    public void shouldModifyTheSameBinaryPropertiesFromMultipleThreadsWithoutDeadlocking() throws Exception {
        startRepositoryWithConfiguration(resourceStream("config/repo-config-persistent-disk.json"));
        //verify we have no binaries yet (see afterEach)
        assertEquals(0, binariesCount());
        int threadCount = 7;
        final List<String> filesToUpload = Arrays.asList("data/large-file1.png", "data/large-file2.jpg",
                                                         "data/move-initial-data.xml");
        
        // create some folders which will each contain files
        for (int i = 1; i < threadCount; i++) {
            session.getRootNode().addNode("folder_" + i, "nt:folder");
        }
        session.save();

        // now fire a number of different threads which should all link the same files in a different order from different (disjoint)
        // nodes
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        final CyclicBarrier barrier = new CyclicBarrier(threadCount);
        final List<Future> results = new ArrayList<Future>();
        final AtomicInteger folderCounter = new AtomicInteger(1);
        for (int i = 0; i < threadCount; i++) {
            results.add(executorService.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    JcrSession session = repository.login();
                    List<String> localFilesToUpload = new ArrayList<String>(filesToUpload);
                    Collections.shuffle(localFilesToUpload);
                    try {
                        int folderIdx = folderCounter.getAndIncrement();
                        for (int i = 0; i < localFilesToUpload.size(); i++) {
                            tools.uploadFile(session, "/folder_" + folderIdx + "/file_" + (i + 1), resourceStream(localFilesToUpload.get(i)));
                        }
                        barrier.await();
                        session.save();
                        return null;
                    } finally {
                        session.logout();
                    }
                }
            }));
        }

        try {
            // the ISPN cache is configured with 2 seconds timeout, so we shouldn't wait a lot more
            for (Future result : results) {
                result.get(3, TimeUnit.SECONDS);
            }

            // check that the correct number of binaries is stored
            assertEquals(filesToUpload.size(), binariesCount());

            // now fire the same number of threads to remove the root nodes and therefore decrement the binary usage  
            results.clear();
            folderCounter.set(1);
            barrier.reset();
            for (int i = 0; i < threadCount; i++) {
                results.add(executorService.submit(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        JcrSession session = repository.login();
                        try {
                            int folderIdx = folderCounter.getAndIncrement();
                            session.getNode("/folder_" + folderIdx).remove();
                            barrier.await();
                            session.save();
                            return null;
                        } finally {
                            session.logout();
                        }
                    }
                }));
            }
            // the remove should be a lot faster...
            for (Future result : results) {
                result.get(1, TimeUnit.SECONDS);
            }
            
            // force a binary prune to validate that the binary values have been indeed marked as unused....
            Thread.sleep(100);
            binaryStore().removeValuesUnusedLongerThan(1, TimeUnit.MILLISECONDS);
            
            // we should've removed all the binaries since we marked them all as unused first....
            assertEquals(0, binariesCount());
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    @FixFor( "MODE-2484 ")
    public void shouldUpdateBinaryReferencesWhenChangingProperties() throws Exception {
        startRepositoryWithConfiguration(resourceStream("config/repo-config-persistent-disk.json"));
        registerNodeTypes("cnd/multi_binary.cnd");
        // verify we have no binaries yet (see afterEach)
        assertEquals(0, binariesCount());

        Node multiBinaryNode = session.getRootNode().addNode("multiBinary", "test:multiBinary");
        Binary binary1 = session.getValueFactory().createBinary(resourceStream("data/large-file1.png"));
        multiBinaryNode.setProperty("binary1", binary1);
        Binary binary2 = session.getValueFactory().createBinary(resourceStream("data/large-file2.jpg"));
        multiBinaryNode.setProperty("binary2", binary2);
        Binary binary3 = session.getValueFactory().createBinary(resourceStream("data/move-initial-data.xml"));
        multiBinaryNode.setProperty("binary3", binary3);
        session.save();
        
        // we should have 3 binary properties
        assertEquals(3, binariesCount());
        
        // set the first binary property to the same value as the third and force a cleanup to check one binary was marked as unused
        session.getNode("/multiBinary").setProperty("binary1", session.getValueFactory().createBinary(resourceStream(
                "data/move-initial-data.xml")));
        session.save();
        Thread.sleep(10);
        binaryStore().removeValuesUnusedLongerThan(1, TimeUnit.MILLISECONDS);
        assertEquals(2, binariesCount());

        // set the second binary property to the same value as the third and force a cleanup to check one binary was marked as unused
        session.getNode("/multiBinary").setProperty("binary2", session.getValueFactory().createBinary(resourceStream(
                "data/move-initial-data.xml")));
        session.save();
        Thread.sleep(10);
        binaryStore().removeValuesUnusedLongerThan(1, TimeUnit.MILLISECONDS);
        assertEquals(1, binariesCount());
        
        // now remove each property by setting it to null and check all binaries are removed
        session.getItem("/multiBinary/binary1").remove();
        session.getItem("/multiBinary/binary2").remove();
        session.getItem("/multiBinary/binary3").remove();
        session.save();
        Thread.sleep(10);
        binaryStore().removeValuesUnusedLongerThan(1, TimeUnit.MILLISECONDS);
        assertEquals(0, binariesCount());
    }


    @Test
    @FixFor( "MODE-2489" )
    public void shouldSupportContentBasedTypeDetection() throws Exception {
        startRepositoryWithConfiguration(resourceStream("config/repo-config-persistent-disk.json"));

        // upload 2 binaries but don't save
        tools.uploadFile(session, "/file1", resourceStream("io/file1.txt"));
        tools.uploadFile(session, "/file2", resourceStream("io/binary.pdf"));
        session.save();

        Node content1 = session.getNode("/file1").getNode("jcr:content");
        // even though the name of the file has no extension, full content based extraction should've been performed
        assertEquals(MediaType.TEXT_PLAIN.toString(), content1.getProperty("jcr:mimeType").getString());

        Node content2 = session.getNode("/file2").getNode("jcr:content");
        // even though the name of the file has no extension, full content based extraction should've been performed
        assertEquals("application/pdf", content2.getProperty("jcr:mimeType").getString());
    }
    
    @Test
    @FixFor( "MODE-2489" )
    public void shouldSupportNoMimeTypeDetection() throws Exception {
        startRepositoryWithConfiguration(resourceStream("config/repo-config-no-mimetype-detection.json"));

        // upload 2 binaries but don't save
        tools.uploadFile(session, "/file1.txt", resourceStream("io/file1.txt"));
        session.save();
        
        Node content = session.getNode("/file1.txt").getNode("jcr:content");
        try {
            content.getProperty("jcr:mimeType");
            fail("No mimetype should have been extracted");
        } catch (PathNotFoundException e) {
            //expected
        }
    }  
    
    @Test
    @FixFor( "MODE-2489" )
    public void shouldSupportNameBasedTypeDetection() throws Exception {
        startRepositoryWithConfiguration(resourceStream("config/repo-config-name-mimetype-detection.json"));

        tools.uploadFile(session, "/file1", resourceStream("io/file1.txt"));
        tools.uploadFile(session, "/file2.txt", resourceStream("io/file2.txt"));
        session.save();
        
        Node content1 = session.getNode("/file1").getNode("jcr:content");
        // because the name of the file does not have an extension, we're expecting a generic mime type here
        assertEquals("application/octet-stream", content1.getProperty("jcr:mimeType").getString());

        Node content2 = session.getNode("/file2.txt").getNode("jcr:content");
        // because the name of the file doe have an extension, we're expecting a specific mime type here
        assertEquals("text/plain", content2.getProperty("jcr:mimeType").getString());
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

            // verify the mime-type has been extracted for the 2 nt:resources
            Binary file1Binary = (Binary)session.getNode("/file1.txt/jcr:content").getProperty("jcr:data").getBinary();
            assertEquals("Invalid mime-type", "text/plain", file1Binary.getMimeType());

            Binary file2Binary = (Binary)session.getNode("/file2.txt/jcr:content").getProperty("jcr:data").getBinary();
            assertEquals("Invalid mime-type", "text/plain", file2Binary.getMimeType());

            // expect 3 binaries
            assertEquals("Incorrect number of binaries in store", 3, binariesCount());

            session.removeItem("/file1.txt");
            session.removeItem("/file2.txt");
            session.save();

            Thread.sleep(2);
            binaryStore().removeValuesUnusedLongerThan(1, TimeUnit.MILLISECONDS);

            // expect 1 binary
            assertEquals("Incorrect number of binaries in store", 1, binariesCount());

            session.removeItem("/file3/binary");
            session.save();

            // sleep to give the binary change listener to mark the binaries as unused
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
            if (binaryKey != null) count++;
        }
        return count;
    }

    private BinaryStore binaryStore() {
        return repository.runningState().binaryStore();
    }

    private byte[] randomBytes( int size ) {
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
