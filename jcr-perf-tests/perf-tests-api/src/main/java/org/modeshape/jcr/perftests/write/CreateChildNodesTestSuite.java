/*
 * JBoss, Home of Professional Open Source
 * Copyright [2011], Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
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
 * Test for measuring the performance of creating a node with child nodes.
 */
public class CreateChildNodesTestSuite extends AbstractPerformanceTestSuite {

    private Session session;
    private Node root;

    public CreateChildNodesTestSuite( SuiteConfiguration suiteConfiguration ) {
        super(suiteConfiguration);
    }

    @Override
    public void beforeSuite() throws RepositoryException {
        session = newSession();
        root = session.getRootNode().addNode("testnode", "nt:unstructured");
    }

    @Override
    public void runTest() throws Exception {
        for (int i = 0; i < suiteConfiguration.getNodeCount(); i++) {
            root.addNode("node" + i, "nt:unstructured");
        }
        session.save();
    }

    @Override
    protected void afterTestRun() throws Exception {
        for (int i = 0; i < suiteConfiguration.getNodeCount(); i++) {
            root.getNode("node" + i).remove();
        }
        session.save();
    }

    @Override
    protected void afterSuite() throws Exception {
        root.remove();
        session.save();
    }
}
