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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.SecureHash;
import org.modeshape.common.util.SecureHash.Algorithm;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.value.BinaryKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSystemBinaryStoreTest extends AbstractBinaryStoreTest {

    protected static final int MIN_BINARY_SIZE = 20;

    public static final String[] CONTENT = new String[] {
        "Lorem ipsum" + UUID.randomUUID().toString(),
        "Lorem ipsum pulvinar" + UUID.randomUUID().toString(),
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent vel felis tellus, at pellentesque sem. "
        + UUID.randomUUID().toString(),
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec tortor nunc, blandit in tempor ut, venenatis ac magna. Vestibulum gravida."
        + UUID.randomUUID().toString(),
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Etiam volutpat tortor non eros bibendum vitae consectetur lacus eleifend. Mauris tempus."
        + UUID.randomUUID().toString(),
        "Morbi pulvinar volutpat sem id sagittis. Vestibulum ornare urna at massa iaculis vitae tincidunt nisi volutpat. Suspendisse auctor gravida viverra."
        + UUID.randomUUID().toString()};

    public static final String[] CONTENT_HASHES;

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemBinaryStoreTest.class);
    static {
        String[] sha1s = new String[CONTENT.length];
        int index = 0;
        for (String content : CONTENT) {
            sha1s[index++] = sha1(content);
        }
        CONTENT_HASHES = sha1s;
    }

    private static String sha1( String content ) {
        try {
            byte[] bytes = SecureHash.getHash(Algorithm.SHA_1, new ByteArrayInputStream(content.getBytes()));
            return SecureHash.asHexString(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected static File directory;
    protected static File trash;
    protected static FileSystemBinaryStore store;
    protected static boolean print = false;

    @Before
    public void beforeClass() {
        directory = new File("target/fsbs/");
        FileUtil.delete(directory);
        directory.mkdirs();
        trash = new File(directory, FileSystemBinaryStore.TRASH_DIRECTORY_NAME);
        store = new FileSystemBinaryStore(directory);
        store.setMinimumBinarySizeInBytes(MIN_BINARY_SIZE);
        store.setMimeTypeDetector(DEFAULT_DETECTOR);
        print = false;
    }

    @After
    public void afterClass() {
        FileUtil.delete(directory);
    }

    @Override
    protected BinaryStore getBinaryStore() {
        return store;
    }

    @Override
    @Test( expected = BinaryStoreException.class )
    public void shouldStoreZeroLengthBinary() throws BinaryStoreException, IOException {
        // the file system binary store will not store a 0 byte size content
        super.shouldStoreZeroLengthBinary();
    }

    @Test
    public void shouldCreateTrashFilesForUnusedBinaries() throws Exception {
        Set<String> storedSha1s = new HashSet<String>();
        for (int i = 0; i != CONTENT.length; ++i) {
            Binary binary = storeAndCheck(i);
            if (binary instanceof StoredBinaryValue) storedSha1s.add(binary.getHexHash());
        }

        // Make sure there are files for all stored values ...
        assertThat(countStoredFiles(), is(storedSha1s.size()));
        assertThat(countTrashFiles(), is(0));

        // Mark one of the files as being unused ...
        String unused = storedSha1s.iterator().next();
        store.markAsUnused(Collections.singleton(new BinaryKey(unused)));

        // Make sure the trash file was created
        assertThat(countStoredFiles(), is(storedSha1s.size()));
        assertThat(countTrashFiles(), is(1));
        // Check that the name of the trash file is the SHA1
        File trashFile = collectFiles(trash).get(0);
        assertNotNull(trashFile);
        assertEquals(unused, trashFile.getName());

        Thread.sleep(1100L); // Sleep more than a second, since modified times may only be accurate to nearest second ...
        store.removeValuesUnusedLongerThan(1, TimeUnit.SECONDS);

        // Make sure the file was removed from the trash ...
        assertThat(countStoredFiles(), is(storedSha1s.size() - 1));
        assertThat(countTrashFiles(), is(0));

        // And that all directories in the trash were removed (since they should be empty) ...
        assertThat(trash.listFiles().length, is(0));
    }

    @Test
    public void shouldCleanTrashFilesWhenFilesBecomeUsed() throws Exception {
        Set<Binary> binaries = new HashSet<Binary>();
        int storedCount = 0;
        for (int i = 0; i != CONTENT.length; ++i) {
            Binary binary = storeAndCheck(i);
            assertThat(binary, is(notNullValue()));
            binaries.add(binary);
            if (binary instanceof StoredBinaryValue) storedCount++;
        }

        // Make sure there are files for all stored values ...
        assertThat(countStoredFiles(), is(storedCount));
        assertThat(countTrashFiles(), is(0));

        // Mark one of the files as being unused ...
        String unused = binaries.iterator().next().getHexHash();
        BinaryKey unusedKey = new BinaryKey(unused);
        store.markAsUnused(Collections.singleton(unusedKey));

        // Make sure the file was moved to the trash ...
        assertThat(countStoredFiles(), is(storedCount));
        assertThat(countTrashFiles(), is(1));

        // Now access all the binary files which will not change there used/unused state
        for (Binary binary : binaries) {
            InputStream stream = binary.getStream();
            String content = IoUtil.read(stream);
            assertThat(content.length() != 0, is(true));
        }

        // Make sure there are files for all stored values ...
        assertThat(countStoredFiles(), is(storedCount));
        assertThat(countTrashFiles(), is(1));

        // Now mark the file explicitly as used and check that the file from the trash was removed
        store.markAsUsed(Collections.singleton(unusedKey));
        assertThat(countTrashFiles(), is(0));
    }

    @Test
    public void shouldStoreLargeFile() throws Exception {
        print = true;
        storeAndCheckResource("docs/postgresql-8.4.1-US.pdf", "3d4d11208cd130d92075e1111423667c76e61819", "17MB file", 17714435L);
    }

    @Test
    public void shouldCreateFileLock() throws IOException {
        File tmpFile = File.createTempFile("foo", "bar");
        File tmpDir = tmpFile.getParentFile();
        tmpFile.delete();
        assertThat(tmpDir.exists(), is(true));
        assertThat(tmpDir.canRead(), is(true));
        assertThat(tmpDir.canWrite(), is(true));
        assertThat(tmpDir.isDirectory(), is(true));
        File lockFile = new File(tmpDir, "lock");
        lockFile.createNewFile();
        RandomAccessFile raf = new RandomAccessFile(lockFile, "rw");
        FileLock fileLock = raf.getChannel().lock();
        fileLock.release();
        assertThat(lockFile.exists(), is(true));
        lockFile.delete();
    }

    @Test
    public void shouldKeepLockWhileMovingLockedFile() throws IOException {
        File tmpDir = new File("target");
        System.out.println("Temporary directory for tests: " + tmpDir.getAbsolutePath());
        assertThat(tmpDir.exists(), is(true));
        assertThat(tmpDir.canRead(), is(true));
        assertThat(tmpDir.canWrite(), is(true));
        assertThat(tmpDir.isDirectory(), is(true));
        File file1 = new File(tmpDir, "lockFile");
        // file1.createNewFile();

        // Lock the file ...
        RandomAccessFile raf = new RandomAccessFile(file1, "rw");
        FileLock fileLock = raf.getChannel().lock();

        // Now try moving our locked file ...
        File file2 = new File(tmpDir, "afterMove");
        if (!file1.renameTo(file2)) {
            LOGGER.warn("RenameTo not successful. Will be ignored if on Windows");
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                fileLock.release();
                return;
            }
        }

        fileLock.release();
        assertThat(file1.exists(), is(false));
        assertThat(file2.exists(), is(true));
    }

    @FixFor( "MODE-1358" )
    @Test
    public void shouldCopyFilesUsingStreams() throws Exception {
        // Copy a large file into a temporary file ...
        File tempFile = File.createTempFile("copytest", "pdf");
        RandomAccessFile destinationRaf = null;
        RandomAccessFile originalRaf = null;
        try {
            URL sourceUrl = getClass().getResource("/docs/postgresql-8.4.1-US.pdf");
            assertThat(sourceUrl, is(notNullValue()));
            File sourceFile = new File(sourceUrl.toURI());
            assertThat(sourceFile.exists(), is(true));
            assertThat(sourceFile.canRead(), is(true));
            assertThat(sourceFile.isFile(), is(true));

            boolean useBufferedStream = true;
            final int bufferSize = AbstractBinaryStore.bestBufferSize(sourceFile.length());

            destinationRaf = new RandomAccessFile(tempFile, "rw");
            originalRaf = new RandomAccessFile(sourceFile, "r");

            FileChannel destinationChannel = destinationRaf.getChannel();
            OutputStream output = Channels.newOutputStream(destinationChannel);
            if (useBufferedStream) output = new BufferedOutputStream(output, bufferSize);

            // Create an input stream to the original file ...
            FileChannel originalChannel = originalRaf.getChannel();
            InputStream input = Channels.newInputStream(originalChannel);
            if (useBufferedStream) input = new BufferedInputStream(input, bufferSize);

            // Copy the content ...
            Stopwatch sw = new Stopwatch();
            sw.start();
            IoUtil.write(input, output, bufferSize);
            sw.stop();
            System.out.println("Time to copy \"" + sourceFile.getName() + "\" (" + sourceFile.length() + " bytes): "
                               + sw.getTotalDuration());
        } finally {
            tempFile.delete();
            if (destinationRaf != null) destinationRaf.close();
            if (originalRaf != null) originalRaf.close();
        }

    }

    @Test
    public void multipleThreadsShouldReadTheSameFile() throws Exception {
        final String textBase = "The quick brown fox jumps over the lazy dog";
        StringBuilder builder = new StringBuilder();
        Random rand = new Random();
        while (builder.length() <= MIN_BINARY_SIZE) {
            builder.append(textBase.substring(0, rand.nextInt(textBase.length())));
        }
        final String text = builder.toString();
        final Binary storedValue = store.storeValue(new ByteArrayInputStream(text.getBytes()), false);
        ExecutorService executor = Executors.newFixedThreadPool(3);
        Callable<String> readingTask = new Callable<String>() {
            @Override
            public String call() throws Exception {
                File tempFile = File.createTempFile("test-binary-store", "bin");
                try {
                    FileOutputStream fos = new FileOutputStream(tempFile);
                    InputStream is = storedValue.getStream();
                    byte[] buff = new byte[100];
                    int available;
                    while ((available = is.read(buff)) != -1) {
                        fos.write(buff, 0, available);
                    }
                    fos.close();

                    return IoUtil.read(tempFile);
                } finally {
                    tempFile.delete();
                }
            }
        };
        List<Callable<String>> tasks = Arrays.asList(readingTask, readingTask, readingTask);
        List<Future<String>> futures = executor.invokeAll(tasks, 5, TimeUnit.SECONDS);
        for (Future<String> future : futures) {
            assertEquals(text, future.get());
        }
    }

    protected Binary storeAndCheck( int contentIndex ) throws Exception {
        return storeAndCheck(contentIndex, null);
    }

    protected Binary storeAndCheck( int contentIndex,
                                    Class<? extends Binary> valueClass ) throws Exception {
        String content = CONTENT[contentIndex];
        String sha1 = CONTENT_HASHES[contentIndex];
        InputStream stream = new ByteArrayInputStream(content.getBytes());

        Stopwatch sw = new Stopwatch();
        sw.start();
        Binary binary = store.storeValue(stream, false);
        sw.stop();
        if (print) System.out.println("Time to store 18MB file: " + sw.getTotalDuration());

        if (valueClass != null) {
            assertThat(binary, is(instanceOf(valueClass)));
        }
        if (content.length() == 0) {
            assertThat(binary, is(instanceOf(EmptyBinaryValue.class)));
        } else if (content.length() < MIN_BINARY_SIZE) {
            assertThat(binary, is(instanceOf(InMemoryBinaryValue.class)));
        } else {
            assertThat(binary, is(instanceOf(StoredBinaryValue.class)));
        }
        assertThat(binary.getHexHash(), is(sha1));
        String binaryContent = IoUtil.read(binary.getStream());
        assertThat(binaryContent, is(content));
        return binary;
    }

    protected void storeAndCheckResource( String resourcePath,
                                          String expectedSha1,
                                          String desc,
                                          long numBytes ) throws Exception {
        InputStream content = getClass().getClassLoader().getResourceAsStream(resourcePath);
        assertThat(content, is(notNullValue()));

        Stopwatch sw = new Stopwatch();
        sw.start();
        Binary binary = store.storeValue(content, false);
        sw.stop();
        if (print) System.out.println("Time to store " + desc + ": " + sw.getTotalDuration());

        if (numBytes == 0) {
            assertThat(binary, is(instanceOf(EmptyBinaryValue.class)));
        } else if (numBytes < MIN_BINARY_SIZE) {
            assertThat(binary, is(instanceOf(InMemoryBinaryValue.class)));
        } else {
            assertThat(binary, is(instanceOf(StoredBinaryValue.class)));
        }
        assertThat(binary.getHexHash(), is(expectedSha1));
        assertThat(binary.getSize(), is(numBytes));

        // Now try reading and comparing the two streams ...
        InputStream expected = getClass().getClassLoader().getResourceAsStream(resourcePath);
        InputStream actual = binary.getStream();
        byte[] buffer1 = new byte[1024];
        byte[] buffer2 = new byte[1024];
        int numRead = 0;
        while ((numRead = expected.read(buffer1)) == actual.read(buffer2)) {
            if (numRead == -1) break;
            for (int i = 0; i != numRead; ++i) {
                assertThat(buffer1[i], is(buffer2[i]));
            }
        }

        if (print) {
            // And try measuring how fast we can read the file ...
            sw = new Stopwatch();
            sw.start();
            while (-1 != actual.read(buffer2)) {
            }
            sw.stop();
            System.out.println("Time to read " + desc + ": " + sw.getTotalDuration());
        }
    }

    protected int countStoredFiles() throws IOException {
        return countFiles(directory, trash);
    }

    protected int countTrashFiles() throws IOException {
        return countFiles(trash);
    }

    protected int countFiles( File fileOrDir,
                              File... excluding ) throws IOException {
        if (excluding == null || excluding.length == 0) {
            return countFiles(fileOrDir, Collections.<File>emptySet());
        }
        return countFiles(fileOrDir, new HashSet<File>(Arrays.asList(excluding)));
    }

    protected int countFiles( File fileOrDir,
                              Set<File> excluding ) throws IOException {
        if (excluding.contains(fileOrDir)) return 0;
        int result = 0;
        if (fileOrDir.isDirectory() && !fileOrDir.isHidden()) {
            for (File child : fileOrDir.listFiles()) {
                result += countFiles(child, excluding);
            }
        } else if (fileOrDir.isFile() && !fileOrDir.isHidden()) {
            result++;
        }
        return result;
    }

    protected List<File> collectFiles(File dir)  {
        List<File> result = new ArrayList<File>();
        File[] files = dir.listFiles();
        if (files == null) {
            return result;
        }
        for (File file : files) {
            if (file.isFile() && file.canRead()) {
                result.add(file);
            } else if (file.isDirectory() && file.canRead()) {
                result.addAll(collectFiles(file));
            }
        }
        return result;
    }

}
