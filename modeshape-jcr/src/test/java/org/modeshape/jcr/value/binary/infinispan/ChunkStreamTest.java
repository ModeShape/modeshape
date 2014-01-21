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
package org.modeshape.jcr.value.binary.infinispan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.AbstractBinaryStoreTest;

/**
 * Unit test for {@link ChunkStreamTest} and {@link ChunkOutputStream}
 */
public class ChunkStreamTest {

    private static final Random RANDOM = new Random();
    private static DefaultCacheManager cacheManager;

    private Cache<String, byte[]> blobCache;
    private int chunkSize;

    @BeforeClass
    public static void beforeClass() throws Exception {
        cacheManager = InfinispanTestUtil.beforeClassStartup(false);
    }

    @AfterClass
    public static void afterClass() {
        InfinispanTestUtil.afterClassShutdown(cacheManager);
    }

    @Before
    public void before() {
        // blob
        blobCache = cacheManager.getCache("ChunkStreamTest");
        blobCache.start();
        chunkSize = InfinispanBinaryStore.DEFAULT_CHUNK_SIZE;
    }

    @After
    public void after() {
        cacheManager.getCache("ChunkStreamTest").stop();
    }

    @Test
    public void testStreamingLarge() throws IOException {
        ChunkOutputStream chunkOutputStream = new ChunkOutputStream(blobCache,
                                                                    AbstractBinaryStoreTest.STORED_LARGE_KEY.toString());
        chunkOutputStream.write(AbstractBinaryStoreTest.STORED_LARGE_BINARY);
        chunkOutputStream.close();
        assertEquals(1, chunkOutputStream.chunksCount());

        ChunkInputStream chunkInputStream = new ChunkInputStream(blobCache, AbstractBinaryStoreTest.STORED_LARGE_KEY.toString(),
                                                                 chunkSize, AbstractBinaryStoreTest.STORED_LARGE_BINARY.length);
        assertEquals(AbstractBinaryStoreTest.STORED_LARGE_KEY, BinaryKey.keyFor(IoUtil.readBytes(chunkInputStream)));
    }

    @Test
    public void testStreamingSmall() throws IOException {
        ChunkOutputStream chunkOutputStream = new ChunkOutputStream(blobCache, AbstractBinaryStoreTest.IN_MEMORY_KEY.toString());
        chunkOutputStream.write(AbstractBinaryStoreTest.IN_MEMORY_BINARY);
        chunkOutputStream.close();
        assertEquals(1, chunkOutputStream.chunksCount());

        ChunkInputStream chunkInputStream = new ChunkInputStream(blobCache, AbstractBinaryStoreTest.IN_MEMORY_KEY.toString(),
                                                                 chunkSize, AbstractBinaryStoreTest.IN_MEMORY_BINARY.length);
        assertEquals(AbstractBinaryStoreTest.IN_MEMORY_KEY, BinaryKey.keyFor(IoUtil.readBytes(chunkInputStream)));
    }

    @Test
    public void testStreamingZero() throws IOException {
        ChunkOutputStream chunkOutputStream = new ChunkOutputStream(blobCache,
                                                                    AbstractBinaryStoreTest.EMPTY_BINARY_KEY.toString());
        chunkOutputStream.write(AbstractBinaryStoreTest.EMPTY_BINARY);
        chunkOutputStream.close();
        assertEquals(0, chunkOutputStream.chunksCount());

        ChunkInputStream chunkInputStream = new ChunkInputStream(blobCache, AbstractBinaryStoreTest.EMPTY_BINARY_KEY.toString(),
                                                                 chunkSize, AbstractBinaryStoreTest.EMPTY_BINARY.length);
        assertEquals(AbstractBinaryStoreTest.EMPTY_BINARY_KEY, BinaryKey.keyFor(IoUtil.readBytes(chunkInputStream)));
    }

    @Test
    public void testStreamingSingleBytes() throws IOException {
        // usses read() and write() instead of read(byte[] ...)
        byte[] data = new byte[2048];
        RANDOM.nextBytes(data);
        BinaryKey dataKey = BinaryKey.keyFor(data);

        ChunkOutputStream chunkOutputStream = new ChunkOutputStream(blobCache, dataKey.toString());
        for (byte aData : data) {
            chunkOutputStream.write(aData);
        }
        chunkOutputStream.close();
        assertEquals(1, chunkOutputStream.chunksCount());
        ChunkInputStream chunkInputStream = new ChunkInputStream(blobCache, dataKey.toString(), chunkSize, data.length);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int b;
        while ((b = chunkInputStream.read()) != -1) {
            byteArrayOutputStream.write(b);
        }
        chunkInputStream.close();
        assertEquals(dataKey, BinaryKey.keyFor(byteArrayOutputStream.toByteArray()));
    }

    @Test
    public void shouldStreamMultipleChunks() throws Exception {
        byte[] data = new byte[InfinispanBinaryStore.DEFAULT_CHUNK_SIZE * 3];
        RANDOM.nextBytes(data);
        BinaryKey dataKey = BinaryKey.keyFor(data);

        ChunkOutputStream chunkOutputStream = new ChunkOutputStream(blobCache, dataKey.toString());
        IoUtil.write(new ByteArrayInputStream(data), chunkOutputStream);

        assertEquals(3, chunkOutputStream.chunksCount());
        ChunkInputStream chunkInputStream = new ChunkInputStream(blobCache, dataKey.toString(), chunkSize, data.length);
        byte[] storedData = IoUtil.readBytes(chunkInputStream);
        assertArrayEquals("Invalid data read from the stream", data, storedData);
    }

    @Test
    @FixFor( "MODE-1752" )
    public void shouldSkipMultipleChunks() throws Exception {
        int totalSize = chunkSize * 3;
        byte[] data = new byte[totalSize];
        RANDOM.nextBytes(data);
        BinaryKey dataKey = BinaryKey.keyFor(data);

        ChunkOutputStream chunkOutputStream = new ChunkOutputStream(blobCache, dataKey.toString());
        IoUtil.write(new ByteArrayInputStream(data), chunkOutputStream);

        ChunkInputStream chunkInputStream = new ChunkInputStream(blobCache, dataKey.toString(), chunkSize, data.length);
        // skip 2 bytes, expect the same amount
        assertEquals(2, chunkInputStream.skip(2));
        // skip the equivalent of 2 chunks, expect the same amount
        assertEquals(chunkSize * 2, chunkInputStream.skip(chunkSize * 2));
        // skip on chunk, expect on chunk - 2 bytes because of the above skip calls
        assertEquals(chunkSize - 2, chunkInputStream.skip(chunkSize));
        // already at EOS so expect no more skipping
        assertEquals(0, chunkInputStream.skip(1));
        assertEquals(0, chunkInputStream.skip(chunkSize));

        chunkInputStream = new ChunkInputStream(blobCache, dataKey.toString(), chunkSize, data.length);
        assertEquals(totalSize, chunkInputStream.skip(totalSize));
        assertEquals(0, chunkInputStream.skip(chunkSize));
        assertEquals(totalSize, new ChunkInputStream(blobCache, dataKey.toString(), chunkSize, data.length).skip(totalSize + 1));
        assertEquals(totalSize, new ChunkInputStream(blobCache, dataKey.toString(), chunkSize, data.length).skip(chunkSize * 4));
    }

    @Test
    @FixFor( "MODE-1752" )
    public void shouldCorrectlyReadAfterSkippingMultipleChunks() throws Exception {
        byte[] data = new byte[chunkSize * 3];
        RANDOM.nextBytes(data);
        BinaryKey dataKey = BinaryKey.keyFor(data);

        ChunkOutputStream chunkOutputStream = new ChunkOutputStream(blobCache, dataKey.toString());
        IoUtil.write(new ByteArrayInputStream(data), chunkOutputStream);

        // skip 2 bytes and read the rest
        ChunkInputStream chunkInputStream = new ChunkInputStream(blobCache, dataKey.toString(), chunkSize, data.length);
        chunkInputStream.skip(2);
        byte[] expected = new byte[data.length - 2];
        System.arraycopy(data, 2, expected, 0, expected.length);
        assertArrayEquals(expected, IoUtil.readBytes(chunkInputStream));

        // skip 1 chunk and read until nothing is left
        chunkInputStream = new ChunkInputStream(blobCache, dataKey.toString(), chunkSize, data.length);
        chunkInputStream.skip(chunkSize);
        expected = new byte[data.length - chunkSize];
        System.arraycopy(data, chunkSize, expected, 0, expected.length);
        assertArrayEquals(expected, IoUtil.readBytes(chunkInputStream));

        // skip the equivalent of 2 chunks, read the rest
        chunkInputStream = new ChunkInputStream(blobCache, dataKey.toString(), chunkSize, data.length);
        chunkInputStream.skip(chunkSize * 2);
        expected = new byte[data.length - (chunkSize * 2)];
        System.arraycopy(data, chunkSize * 2, expected, 0, expected.length);
        assertArrayEquals(expected, IoUtil.readBytes(chunkInputStream));

        chunkInputStream.skip(chunkSize);
        expected = new byte[0];
        assertArrayEquals(expected, IoUtil.readBytes(chunkInputStream));
    }

    @Test
    @FixFor( "MODE-1752" )
    public void shouldCorrectlyReadAfterDirectlySkippingMultipleChunks() throws Exception {
        int chunkSize = InfinispanBinaryStore.DEFAULT_CHUNK_SIZE;
        int totalSize = chunkSize * 3;
        byte[] data = new byte[totalSize];
        RANDOM.nextBytes(data);
        BinaryKey dataKey = BinaryKey.keyFor(data);

        ChunkOutputStream chunkOutputStream = new ChunkOutputStream(blobCache, dataKey.toString());
        IoUtil.write(new ByteArrayInputStream(data), chunkOutputStream);

        // skip 2 bytes and read the rest
        ChunkInputStream chunkInputStream = new ChunkInputStream(blobCache, dataKey.toString(), chunkSize, totalSize);
        chunkInputStream.skip(2);
        byte[] expected = new byte[data.length - 2];
        System.arraycopy(data, 2, expected, 0, expected.length);
        assertArrayEquals(expected, IoUtil.readBytes(chunkInputStream));

        // skip 1 chunk and read until nothing is left
        chunkInputStream = new ChunkInputStream(blobCache, dataKey.toString(), chunkSize, totalSize);
        chunkInputStream.skip(chunkSize);
        expected = new byte[data.length - chunkSize];
        System.arraycopy(data, chunkSize, expected, 0, expected.length);
        assertArrayEquals(expected, IoUtil.readBytes(chunkInputStream));

        // skip the equivalent of 2 chunks, read the rest
        chunkInputStream = new ChunkInputStream(blobCache, dataKey.toString(), chunkSize, totalSize);
        chunkInputStream.skip(chunkSize * 2);
        expected = new byte[data.length - (chunkSize * 2)];
        System.arraycopy(data, chunkSize * 2, expected, 0, expected.length);
        assertArrayEquals(expected, IoUtil.readBytes(chunkInputStream));

        chunkInputStream.skip(chunkSize);
        expected = new byte[0];
        assertArrayEquals(expected, IoUtil.readBytes(chunkInputStream));
    }
}
