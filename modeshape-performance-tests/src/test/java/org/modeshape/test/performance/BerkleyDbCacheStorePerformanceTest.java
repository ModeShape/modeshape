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
package org.modeshape.test.performance;

import javax.jcr.Node;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.LoaderConfigurationBuilder;
import org.infinispan.loaders.bdbje.BdbjeCacheStore;
import org.junit.Test;
import org.modeshape.common.annotation.Performance;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.common.util.FileUtil;
import java.io.File;

public class BerkleyDbCacheStorePerformanceTest extends InMemoryPerformanceTest {

    private final File dbDir = new File("target/database");

    @Override
    protected void cleanUpFileSystem() {
        FileUtil.delete(dbDir);
    }

    @Override
    public void applyLoaderConfiguration( ConfigurationBuilder configurationBuilder ) {
        LoaderConfigurationBuilder lb = configurationBuilder.loaders().addCacheLoader().cacheLoader(new BdbjeCacheStore());
        lb.addProperty("location", dbDir.getAbsolutePath());
    }

    @Performance
    @Test
    public void shouldAllowCreatingMillionNodeSubgraphUsingMultipleSaves() throws Exception {
        repeatedlyCreateSubgraph(1, 2, 100, 0, false, true);
    }

    @Performance
    @Test
    public void shouldAllowCreatingManyManyUnstructuredNodesWithNoSameNameSiblings() throws Exception {
        System.out.print("Iterating ");
        // Each iteration adds another node under the root and creates the many nodes under that node ...
        Node node = session.getRootNode().addNode("testNode");
        session.save();

        Stopwatch sw = new Stopwatch();
        Stopwatch total = new Stopwatch();
        try {
            total.start();
            for (int i = 0; i != 50; ++i) {
                System.out.print(".");
                int count = 100;
                sw.start();
                for (int j = 0; j != count; ++j) {
                    node.addNode("childNode" + j);
                }
                session.save();
                sw.stop();
            }
            total.stop();
        } finally {
            System.out.println();
            System.out.println(total.getDetailedStatistics());
            System.out.println(sw.getDetailedStatistics());
        }
    }

}
