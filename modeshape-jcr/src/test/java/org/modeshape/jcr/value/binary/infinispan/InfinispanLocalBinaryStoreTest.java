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



import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.After;
import org.junit.Before;
import org.modeshape.jcr.value.binary.AbstractBinaryStoreTest;
import org.modeshape.jcr.value.binary.BinaryStore;

public class InfinispanLocalBinaryStoreTest extends AbstractBinaryStoreTest {

    private DefaultCacheManager cacheManager;
    private BinaryStore binaryStore;


    @Before
    public void before(){
        // setup ISPN
        GlobalConfigurationBuilder globalConfigurationBuilder = new GlobalConfigurationBuilder();
        globalConfigurationBuilder.transport().transport(null);

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.clustering().cacheMode(CacheMode.LOCAL);

        cacheManager = new DefaultCacheManager(globalConfigurationBuilder.build(), true);

        // blob
        cacheManager.defineConfiguration("blob", configurationBuilder.build());
        cacheManager.startCache("blob");
        // metadata
        cacheManager.defineConfiguration("metadata", configurationBuilder.build());
        cacheManager.startCache("metadata");

        // start binary store
        binaryStore = new InfinispanBinaryStore(cacheManager, false, "metadata", "blob");
        binaryStore.setMimeTypeDetector(new DummyMimeTypeDetector());
        ((InfinispanBinaryStore)binaryStore).start();
    }

    @After
    public void after(){
        if(cacheManager != null){
            cacheManager.stop();
        }
    }

    @Override
    protected BinaryStore getBinaryStore() {
        return binaryStore;
    }
}
