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
import org.modeshape.jcr.perftests.SuiteConfiguration;
import java.util.Random;
import java.util.concurrent.Callable;

/**
 * Test case that traverses a set of  unstructured nodes while
 * a number of concurrent readers randomly access nodes from within this tree.
 */
public class ConcurrentReadTestSuite extends AbstractPerformanceTestSuite {

    private static final int CONCURRENT_READER_COUNT = 20;
    public static final int READERS_COUNT = 1000;

    protected int nodeCount;

    private Session session;
    private Node root;

    public ConcurrentReadTestSuite( SuiteConfiguration suiteConfiguration ) {
        super(suiteConfiguration);
    }

    public void beforeSuite() throws Exception {
        session = newSession();
        root = session.getRootNode().addNode("testroot", "nt:unstructured");
        nodeCount = suiteConfiguration.getNodeCount();

        for (int i = 0; i < nodeCount; i++) {
            Node node = root.addNode("node" + i, "nt:unstructured");
            for (int j = 0; j < nodeCount; j++) {
                node.addNode("node" + j, "nt:unstructured");
            }
            session.save();
        }

        for (int i = 0; i < CONCURRENT_READER_COUNT; i++) {
            addBackgroundJob(new Reader());
        }
    }

    private class Reader implements Callable<Void> {

        private final Session session = newSession();
        private final Random random = new Random();

        public Void call() throws Exception {
            int i = random.nextInt(nodeCount);
            int j = random.nextInt(nodeCount);
            assert session.getRootNode().getNode("testroot/node" + i + "/node" + j) != null;
            return null;
        }
    }

    public void runTest() throws Exception {
        Reader reader = new Reader();
        for (int i = 0; i < READERS_COUNT; i++) {
            reader.call();
        }
    }

    public void afterSuite() throws Exception {
        root.remove();
        session.save();
    }
}
