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
package org.modeshape.test.performance;

import java.io.File;
import javax.jcr.Node;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.loaders.bdbje.configuration.BdbjeCacheStoreConfigurationBuilder;
import org.junit.Test;
import org.modeshape.common.annotation.Performance;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.common.util.FileUtil;

public class BerkleyDbCacheStorePerformanceTest extends InMemoryPerformanceTest {

    private final File dbDir = new File("target/database");

    @Override
    protected void cleanUpFileSystem() {
        FileUtil.delete(dbDir);
    }

    @Override
    public void applyLoaderConfiguration( ConfigurationBuilder configurationBuilder ) {
        BdbjeCacheStoreConfigurationBuilder builder = new BdbjeCacheStoreConfigurationBuilder(configurationBuilder.loaders());
        builder.location(dbDir.getAbsolutePath());
        builder.purgeOnStartup(true);
        configurationBuilder.loaders().addStore(builder);
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
