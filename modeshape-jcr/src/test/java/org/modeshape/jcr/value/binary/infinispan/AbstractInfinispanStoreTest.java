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


import org.infinispan.configuration.cache.Configuration;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.modeshape.jcr.value.binary.AbstractBinaryStoreTest;
import org.modeshape.jcr.value.binary.BinaryStore;

import java.net.InetAddress;

public abstract class AbstractInfinispanStoreTest extends AbstractBinaryStoreTest {

    protected static DefaultCacheManager cacheManager;
    private static final String BLOB = "blob";
    private static final String METADATA = "metadata";

    private InfinispanBinaryStore binaryStore;

    @BeforeClass
    public static void beforeClass() throws Exception {
        cacheManager = InfinispanTestUtil.beforeClassStartup(true);
    }

    @AfterClass
    public static void afterClass(){
       InfinispanTestUtil.afterClassShutdown(cacheManager);
    }

    @After
    public void after(){
        cacheManager.getCache(METADATA).stop();
        cacheManager.removeCache(METADATA);
        cacheManager.getCache(BLOB).stop();
        cacheManager.removeCache(BLOB);
    }

    void startBinaryStore(Configuration metadataConfiguration, Configuration blobConfiguration){
        cacheManager.defineConfiguration(METADATA, metadataConfiguration);
        cacheManager.startCache(METADATA);

        cacheManager.defineConfiguration(BLOB, blobConfiguration);
        cacheManager.startCache(BLOB);

        binaryStore = new InfinispanBinaryStore(cacheManager, true, METADATA, BLOB);
        binaryStore.start();
    }

    @Override
    protected BinaryStore getBinaryStore() {
        return binaryStore;
    }



}
