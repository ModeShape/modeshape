/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.perftests.read;

import javax.jcr.Node;
import javax.jcr.Session;
import org.modeshape.jcr.perftests.AbstractPerformanceTestSuite;
import java.util.Random;
import java.util.concurrent.Callable;

/**
 * Test case that traverses 10k unstructured nodes (100x100) while
 * 50 concurrent readers randomly access nodes from within this tree.
 */
public class ConcurrentReadTestSuite extends AbstractPerformanceTestSuite {

    protected static final int NODE_COUNT = 100;
    private static final int READER_COUNT = 20;

    private Session session;

    protected Node root;

    public void beforeSuite() throws Exception {
        session = newSession();
        root = session.getRootNode().addNode("testroot", "nt:unstructured");
        for (int i = 0; i < NODE_COUNT; i++) {
            Node node = root.addNode("node" + i, "nt:unstructured");
            for (int j = 0; j < NODE_COUNT; j++) {
                node.addNode("node" + j, "nt:unstructured");
            }
            session.save();
        }

        for (int i = 0; i < READER_COUNT; i++) {
            addBackgroundJob(new Reader());
        }
    }

    private class Reader implements Callable<Void> {

        private final Session session = newSession();
        private final Random random = new Random();

        public Void call() throws Exception {
            int i = random.nextInt(NODE_COUNT);
            int j = random.nextInt(NODE_COUNT);
            assert session.getRootNode().getNode("testroot/node" + i + "/node" + j) != null;
            return null;
        }
    }

    public void runTest() throws Exception {
        Reader reader = new Reader();
        for (int i = 0; i < 1000; i++) {
            reader.call();
        }
    }

    public void afterSuite() throws Exception {
//        for (int i = 0; i < NODE_COUNT; i++) {
//            root.getNode("node" + i).remove();
//            session.save();
//        }
        root.remove();
        session.save();
    }

}
