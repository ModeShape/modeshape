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
package org.modeshape.jcr.perftests.write;

import javax.jcr.Node;
import javax.jcr.Session;
import org.modeshape.jcr.perftests.SuiteConfiguration;
import org.modeshape.jcr.perftests.read.ConcurrentReadTestSuite;
import java.util.Random;
import java.util.concurrent.Callable;

/**
 * A {@link org.modeshape.jcr.perftests.read.ConcurrentReadTestSuite} with a single writer thread that continuously updates the nodes being accessed by the readers.
 */
public class ConcurrentReadWriteTestSuite extends ConcurrentReadTestSuite {

    public ConcurrentReadWriteTestSuite( SuiteConfiguration suiteConfiguration ) {
        super(suiteConfiguration);
    }

    @Override
    public void beforeSuite() throws Exception {
        super.beforeSuite();
        addBackgroundJob(new Writer());
    }

    private class Writer implements Callable<Void> {

        private final Session session = newSession();
        private final Random random = new Random();

        private long count = 0;

        public Void call() throws Exception {
            int i = random.nextInt(nodeCount);
            int j = random.nextInt(nodeCount);
            Node node = session.getRootNode().getNode("testroot/node" + i + "/node" + j);
            node.setProperty("count", count++);
            session.save();
            return null;
        }
    }
}
