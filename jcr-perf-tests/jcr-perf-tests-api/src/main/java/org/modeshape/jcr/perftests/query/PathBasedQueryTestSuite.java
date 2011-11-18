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
import java.util.Random;

/**
 * <code>PathBasedQueryTestSuite</code> implements a performance test executing a
 * query that has a path constraint with low selectivity, whereas the predicate
 * is very selective.
 *
 * //TODO author=Horia Chiorean date=11/17/11 description=Decide whether to keep xpath query or not
 */
public class PathBasedQueryTestSuite extends AbstractPerformanceTestSuite {

    public static final int FIRST_LEVEL_NODE_COUNT = 5;
    public static final int SECOND_LEVEL_NODE_COUNT = 100;

    private Session session;
    private Node root;

    @Override
    public void beforeSuite() throws Exception {
        session = newSession();
        root = session.getRootNode().addNode(getClass().getSimpleName(), "nt:unstructured");
        int count = 0;
        for (int i = 0; i < FIRST_LEVEL_NODE_COUNT; i++) {
            Node n = root.addNode("node-" + i);
            for (int j = 0; j < SECOND_LEVEL_NODE_COUNT; j++) {
                n.addNode("node-" + j).setProperty("count", count++);
            }
        }
        session.save();
    }

    @Override
    public void runTest() throws Exception {
        QueryManager qm = session.getWorkspace().getQueryManager();
        Query q = qm.createQuery("/jcr:root" + root.getPath() + "/*/*[@count = 250]", Query.XPATH);
        for (int i = 0; i < FIRST_LEVEL_NODE_COUNT; i++) {
            Node result = q.execute().getNodes().nextNode();
            assert result != null;
        }
        Random rnd = new Random();
        for (int i = 0; i < FIRST_LEVEL_NODE_COUNT; i++) {
            q = qm.createQuery("/jcr:root" + root.getPath() + "/*/*[@count = " + rnd.nextInt(FIRST_LEVEL_NODE_COUNT * SECOND_LEVEL_NODE_COUNT) +
                    "]", Query.XPATH);
            Node result = q.execute().getNodes().nextNode();
            assert result != null;
        }
    }

    @Override
    public void afterSuite() throws Exception {
        root.remove();
        session.save();
        session.logout();
    }

    @Override
    public boolean isCompatibleWith( Repository repository ) {
        String xpathSupported = repository.getDescriptor(Repository.OPTION_QUERY_SQL_SUPPORTED);
        return xpathSupported != null && xpathSupported.equalsIgnoreCase(Boolean.TRUE.toString());
    }
}
