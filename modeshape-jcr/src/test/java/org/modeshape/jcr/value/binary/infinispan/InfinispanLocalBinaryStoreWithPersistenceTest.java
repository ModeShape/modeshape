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

import java.io.File;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.LoaderConfigurationBuilder;
import org.infinispan.loaders.file.FileCacheStore;
import org.junit.BeforeClass;

public class InfinispanLocalBinaryStoreWithPersistenceTest extends AbstractInfinispanStoreTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        cacheManager = InfinispanTestUtil.beforeClassStartup(false);

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.clustering().cacheMode(CacheMode.LOCAL);
        // Set up the file persistence ...
        configurationBuilder.loaders().shared(false);
        LoaderConfigurationBuilder loaderBuilder = configurationBuilder.loaders()
                                                                       .addCacheLoader()
                                                                       .cacheLoader(new FileCacheStore());
        loaderBuilder.addProperty("location", new File(System.getProperty("java.io.tmpdir"),
                                                       "InfinispanLocalBinaryStoreWithPersistenceTest").getAbsolutePath());
        loaderBuilder.purgeOnStartup(true);

        // Create the configurations for the two different caches ...
        Configuration blobConfiguration = configurationBuilder.build();
        Configuration metadataConfiguration = configurationBuilder.build();

        startBinaryStore(metadataConfiguration, blobConfiguration);
    }
}
