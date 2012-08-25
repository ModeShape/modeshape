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
package org.modeshape.jcr.value.binary.infinispan;


import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.*;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.AbstractBinaryStoreTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import static junit.framework.Assert.assertEquals;

public class ChunkStreamTest {

    private static DefaultCacheManager cacheManager;

    private Cache<String, byte[]> blobCache;

    @BeforeClass
    public static void beforeClass() throws Exception {
        cacheManager = InfinispanTestUtil.beforeClassStartup(false);
    }

    @AfterClass
    public static void afterClass(){
        InfinispanTestUtil.afterClassShutdown(cacheManager);
    }

    @Before
    public void before(){
        // blob
        blobCache = cacheManager.getCache("ChunkStreamTest");
    }

    @After
    public void after(){
        cacheManager.getCache("ChunkStreamTest").stop();
        cacheManager.removeCache("ChunkStreamTest");
    }

    @Test
    public void testStreamingLarge() throws IOException {
        ChunkOutputStream chunkOutputStream = new ChunkOutputStream(blobCache, AbstractBinaryStoreTest.LARGE_KEY.toString());
        chunkOutputStream.write(AbstractBinaryStoreTest.LARGE_DATA);
        chunkOutputStream.close();
        assertEquals(6, chunkOutputStream.getNumberChunks());

        ChunkInputStream chunkInputStream = new ChunkInputStream(blobCache, AbstractBinaryStoreTest.LARGE_KEY.toString());
        assertEquals(AbstractBinaryStoreTest.LARGE_KEY, BinaryKey.keyFor(IoUtil.readBytes(chunkInputStream)));
    }

    @Test
    public void testStreamingSmall() throws IOException {
        ChunkOutputStream chunkOutputStream = new ChunkOutputStream(blobCache, AbstractBinaryStoreTest.SMALL_KEY.toString());
        chunkOutputStream.write(AbstractBinaryStoreTest.SMALL_DATA);
        chunkOutputStream.close();
        assertEquals(1, chunkOutputStream.getNumberChunks());

        ChunkInputStream chunkInputStream = new ChunkInputStream(blobCache, AbstractBinaryStoreTest.SMALL_KEY.toString());
        assertEquals(AbstractBinaryStoreTest.SMALL_KEY, BinaryKey.keyFor(IoUtil.readBytes(chunkInputStream)));
    }

    @Test
    public void testStreamingZero() throws IOException {
        ChunkOutputStream chunkOutputStream = new ChunkOutputStream(blobCache, AbstractBinaryStoreTest.ZERO_KEY.toString());
        chunkOutputStream.write(AbstractBinaryStoreTest.ZERO_DATA);
        chunkOutputStream.close();
        assertEquals(0, chunkOutputStream.getNumberChunks());

        ChunkInputStream chunkInputStream = new ChunkInputStream(blobCache, AbstractBinaryStoreTest.ZERO_KEY.toString());
        assertEquals(AbstractBinaryStoreTest.ZERO_KEY, BinaryKey.keyFor(IoUtil.readBytes(chunkInputStream)));
    }

    @Test
    public void testStreamingSingleBytes() throws IOException {
        // usses read() and write() instead of read(byte[] ...)
        byte[] data = new byte[2048];
        new Random().nextBytes(data);
        BinaryKey dataKey = BinaryKey.keyFor(data);

        ChunkOutputStream chunkOutputStream = new ChunkOutputStream(blobCache, dataKey.toString());
        for (byte aData : data) {
            chunkOutputStream.write(aData);
        }
        chunkOutputStream.close();
        assertEquals(1, chunkOutputStream.getNumberChunks());
        ChunkInputStream chunkInputStream = new ChunkInputStream(blobCache, dataKey.toString());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int b;
        while((b = chunkInputStream.read()) != -1){
            byteArrayOutputStream.write(b);
        }
        chunkInputStream.close();
        assertEquals(dataKey, BinaryKey.keyFor(byteArrayOutputStream.toByteArray()));
    }
}
