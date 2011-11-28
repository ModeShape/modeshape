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
package org.modeshape.jcr.perftests.query;

import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import org.modeshape.jcr.perftests.AbstractPerformanceTestSuite;
import org.modeshape.jcr.perftests.SuiteConfiguration;

/**
 * Performance test to check performance of queries on sub-trees.
 */
public class PathDescendantSearchTestSuite extends AbstractPerformanceTestSuite {

    private Session session;
    private Node root;
    private int nodeCount;

    public PathDescendantSearchTestSuite( SuiteConfiguration suiteConfiguration ) {
        super(suiteConfiguration);
    }

    @Override
    public void beforeSuite() throws RepositoryException {
        session = newSession();
        nodeCount = suiteConfiguration.getNodeCount();
        root = session.getRootNode().addNode("testroot", "nt:unstructured");
        for (int i = 0; i < nodeCount; i++) {
            Node node = root.addNode("node" + i, "nt:unstructured");
            for (int j = 0; j < nodeCount; j++) {
                Node child = node.addNode("node" + j, "nt:unstructured");
                child.setProperty("testcount", j);
            }
            session.save();
        }
    }

    @Override
    public void runTest() throws Exception {
        QueryManager manager = session.getWorkspace().getQueryManager();
        for (int i = 0; i < nodeCount; i++) {
            Query query = createQuery(manager, i);
            NodeIterator iterator = query.execute().getNodes();
            while (iterator.hasNext()) {
                Node node = iterator.nextNode();
                assert  node.getProperty("testcount").getLong() == i;
            }
        }
    }

    @Override
    public void afterSuite() throws RepositoryException {
        root.remove();
        session.save();
        session.logout();
    }

    protected Query createQuery( QueryManager manager, int i )
            throws RepositoryException {
        return manager.createQuery("/jcr:root/testroot//element(*,nt:base)[@testcount=" + i + "]", Query.XPATH);
    }
}
