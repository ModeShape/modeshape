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
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import org.modeshape.jcr.perftests.AbstractPerformanceTestSuite;
import org.modeshape.jcr.perftests.SuiteConfiguration;
import java.util.Random;

/**
 * Performance test for a three-way join that selects <code>{@link ThreeWayJoinTestSuite#nodeCount}</code> triples from
 * a set of <code>nodeCount * nodeCount * nodeCount</code> nodes.
 * The query is constructed in a way that should allow a smart implementation to perform the join quite efficiently.
 *
 */
public class ThreeWayJoinTestSuite extends AbstractPerformanceTestSuite {

    private final Random random = new Random();

    private Session session;
    private Node root;
    private int nodeCount;

    public ThreeWayJoinTestSuite( SuiteConfiguration suiteConfiguration ) {
        super(suiteConfiguration);
    }

    @Override
    public void beforeSuite() throws RepositoryException {
        session = newSession();
        root = session.getRootNode().addNode("testroot", "nt:unstructured");
        nodeCount = suiteConfiguration.getNodeCount();

        for (int i = 0; i < nodeCount; i++) {
            Node foo = root.addNode("node" + i, "nt:unstructured");
            foo.setProperty("foo", i);
            for (int j = 0; j < nodeCount; j++) {
                Node bar = foo.addNode("node" + j, "nt:unstructured");
                bar.setProperty("bar", j);
                for (int k = 0; k < nodeCount; k++) {
                    Node baz = bar.addNode("node" + k, "nt:unstructured");
                    baz.setProperty("baz", k);
                }
            }
            session.save();
        }
    }

    @Override
    public void runTest() throws Exception {
        int randFooValue = random.nextInt(nodeCount);
        String query =
                "SELECT a.foo AS a, b.bar AS b, c.baz AS c"
                        + " FROM [nt:unstructured] AS a"
                        + " INNER JOIN [nt:unstructured] AS b ON a.foo = b.bar"
                        + " INNER JOIN [nt:unstructured] AS c ON b.bar = c.baz"
                        + " WHERE a.foo = " + randFooValue;

        QueryManager manager = session.getWorkspace().getQueryManager();
        RowIterator iterator = manager.createQuery(query, Query.JCR_SQL2).execute().getRows();
        assert iterator.getSize() == nodeCount * nodeCount * nodeCount;
        while (iterator.hasNext()) {
            Row row = iterator.nextRow();
            long a = row.getValue("a").getLong();
            long b = row.getValue("b").getLong();
            long c = row.getValue("c").getLong();

            assert a == randFooValue;
            assert b == randFooValue;
            assert c == randFooValue;
        }
    }

    @Override
    public void afterSuite() throws RepositoryException {
        root.remove();
        session.save();
    }

    @Override
    public boolean isCompatibleWithCurrentRepository() {
        String joins = suiteConfiguration.getRepository().getDescriptor(Repository.QUERY_JOINS);
        return joins != null && !joins.equals(Repository.QUERY_JOINS_NONE);
    }
}
