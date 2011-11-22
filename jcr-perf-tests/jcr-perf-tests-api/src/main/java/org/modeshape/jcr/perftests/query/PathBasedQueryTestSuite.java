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
package org.modeshape.jcr.perftests.query;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import org.modeshape.jcr.perftests.AbstractPerformanceTestSuite;
import org.modeshape.jcr.perftests.SuiteConfiguration;
import java.util.Random;

/**
 * <code>PathBasedQueryTestSuite</code> implements a performance test executing a
 * query that has a path constraint with low selectivity, whereas the predicate
 * is very selective.
 *
 * //TODO author=Horia Chiorean date=11/17/11 description=Decide whether to keep xpath query or not
 */
public class PathBasedQueryTestSuite extends AbstractPerformanceTestSuite {

    private Session session;
    private Node root;
    private int nodeCount;

    public PathBasedQueryTestSuite( SuiteConfiguration suiteConfiguration ) {
        super(suiteConfiguration);
    }

    @Override
    public void beforeSuite() throws Exception {
        session = newSession();
        root = session.getRootNode().addNode(getClass().getSimpleName(), "nt:unstructured");
        nodeCount = suiteConfiguration.getNodeCount();
        int count = 0;
        for (int i = 0; i < nodeCount; i++) {
            Node n = root.addNode("node-" + i);
            for (int j = 0; j < nodeCount; j++) {
                n.addNode("node-" + j).setProperty("count", count++);
            }
        }
        session.save();
    }

    @Override
    public void runTest() throws Exception {
        QueryManager qm = session.getWorkspace().getQueryManager();
        Query q = qm.createQuery("/jcr:root" + root.getPath() + "/*/*[@count = " + (nodeCount * nodeCount - 1)  +"]", Query.XPATH);
        for (int i = 0; i < nodeCount; i++) {
            Node result = q.execute().getNodes().nextNode();
            assert result != null;
        }
        Random rnd = new Random();
        for (int i = 0; i < nodeCount; i++) {
            q = qm.createQuery("/jcr:root" + root.getPath() + "/*/*[@count = " + rnd.nextInt(nodeCount * nodeCount) + "]", Query.XPATH);
            Node result = q.execute().getNodes().nextNode();
            assert result != null;
        }
    }

    @Override
    public void afterSuite() throws Exception {
        root.remove();
        session.save();
    }

    @Override
    public boolean isCompatibleWithCurrentRepository() {
        String xpathSupported = suiteConfiguration.getRepository().getDescriptor(Repository.OPTION_QUERY_SQL_SUPPORTED);
        return xpathSupported != null && xpathSupported.equalsIgnoreCase(Boolean.TRUE.toString());
    }
}
