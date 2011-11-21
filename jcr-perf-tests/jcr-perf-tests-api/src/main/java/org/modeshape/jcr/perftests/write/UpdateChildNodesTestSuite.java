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
import org.modeshape.jcr.perftests.SuiteConfiguration;

/**
 * Test for measuring the performance of adding one extra child node to 
 * node with existing child nodes.
 */
public class UpdateChildNodesTestSuite extends AbstractPerformanceTestSuite {

    private Session session;
    private Node node;

    public UpdateChildNodesTestSuite( SuiteConfiguration suiteConfiguration ) {
        super(suiteConfiguration);
    }

    @Override
    public void beforeSuite() throws RepositoryException {
        session = newSession();
        node = session.getRootNode().addNode("testnode", "nt:unstructured");
        for (int i = 0; i < suiteConfiguration.getNodeCount(); i++) {
            node.addNode("node" + i, "nt:unstructured");
        }
    }

    @Override
    public void runTest() throws Exception {
        node.addNode("onemore", "nt:unstructured");
        session.save();
    }

    @Override
    public void afterSuite() throws RepositoryException {
        session.getRootNode().getNode("testnode").remove();
        session.save();
    }
}
