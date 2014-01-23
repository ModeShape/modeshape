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

import javax.jcr.Node;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.persistence.leveldb.configuration.LevelDBStoreConfiguration;
import org.infinispan.persistence.leveldb.configuration.LevelDBStoreConfigurationBuilder;
import org.junit.Test;
import org.modeshape.common.annotation.Performance;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.common.util.FileUtil;

public class LevelDbCacheStorePerformanceTest extends InMemoryPerformanceTest {


    @Override
    protected void cleanUpFileSystem() {
        FileUtil.delete("target/leveldb");
        FileUtil.delete("target/leveldbdocuments");
    }

    @Override
    public void applyLoaderConfiguration( ConfigurationBuilder configurationBuilder ) {

        LevelDBStoreConfigurationBuilder builder = new LevelDBStoreConfigurationBuilder(configurationBuilder.persistence());
        builder.location("target/leveldb/content");
        builder.expiredLocation("target/leveldb/expired");
        builder.implementationType(LevelDBStoreConfiguration.ImplementationType.JAVA);
        builder.purgeOnStartup(true);
        configurationBuilder.persistence().addStore(builder);
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
