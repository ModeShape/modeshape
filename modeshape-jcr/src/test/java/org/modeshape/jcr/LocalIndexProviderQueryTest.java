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

import javax.jcr.Node;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.util.FileUtil;

public class LocalIndexProviderQueryTest extends JcrQueryManagerTest {

    @BeforeClass
    public static void beforeAll() throws Exception {
        // Clean up the indexes and storage ...
        FileUtil.delete("target/LocalIndexProviderQueryTest");

        String configFileName = LocalIndexProviderQueryTest.class.getSimpleName() + ".json";
        JcrQueryManagerTest.beforeAll(configFileName);
    }

    @Test
    public void shouldHandleAddingAndRemovingIndexedNodes() throws Exception {
        Node added = session().getRootNode().addNode("extra", "car:Car");
        session().save();
        added.remove();
        session().save();
    }
}
