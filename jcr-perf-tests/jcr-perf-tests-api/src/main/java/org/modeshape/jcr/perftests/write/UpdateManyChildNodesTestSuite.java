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
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.modeshape.jcr.perftests.AbstractPerformanceTestSuite;

/**
 * Test for measuring the performance of adding one extra child node to 
 * node with {@value #CHILD_COUNT} existing child nodes.
 */
public class UpdateManyChildNodesTestSuite extends AbstractPerformanceTestSuite {

    private static final int CHILD_COUNT = 10 * 1000;

    private Session session;
    private Node node;

    @Override
    public void beforeSuite() throws RepositoryException {
        session = newSession();
        node = session.getRootNode().addNode("testnode", "nt:unstructured");
        for (int i = 0; i < CHILD_COUNT; i++) {
            node.addNode("node" + i, "nt:unstructured");
        }
    }

    @Override
    public void runTest() throws Exception {
        node.addNode("onemore", "nt:unstructured");
        session.save();
    }

    @Override
    public void afterTest() throws RepositoryException {
        node.getNode("onemore").remove();
        session.save();
    }

    @Override
    public void afterSuite() throws RepositoryException {
        session.save();
        session.logout();
    }
}
