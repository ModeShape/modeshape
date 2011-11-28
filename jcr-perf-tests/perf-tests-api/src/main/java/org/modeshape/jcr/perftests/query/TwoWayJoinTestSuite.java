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

import java.util.Random;

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

/**
 * Performance test for a  two-way join that selects <code>{@link TwoWayJoinTestSuite#nodeCount}</code> pairs from
 * a set of <code>nodeCount * nodeCount</code> nodes.
 */
public class TwoWayJoinTestSuite extends AbstractPerformanceTestSuite {

    private final Random random = new Random();

    private Session session;
    private Node root;
    private int nodeCount;

    public TwoWayJoinTestSuite( SuiteConfiguration suiteConfiguration ) {
        super(suiteConfiguration);
    }

    @Override
    public void beforeSuite() throws RepositoryException {
        session = newSession();
        root = session.getRootNode().addNode("testroot", "nt:unstructured");
        nodeCount = suiteConfiguration.getNodeCount();

        for (int i = 0; i < nodeCount; i++) {
            Node node = root.addNode("node" + i, "nt:unstructured");
            node.setProperty("foo", i);
            for (int j = 0; j < nodeCount; j++) {
                Node child = node.addNode("node" + j, "nt:unstructured");
                child.setProperty("bar", j);
            }
            session.save();
        }
    }

    @Override
    public void runTest() throws Exception {
        int fooValue = random.nextInt(nodeCount);
        String query = "SELECT a.foo AS a, b.bar AS b"
                + " FROM [nt:unstructured] AS a"
                + " INNER JOIN [nt:unstructured] AS b ON a.foo = b.bar"
                + " WHERE a.foo = " + fooValue;

        QueryManager manager = session.getWorkspace().getQueryManager();
        RowIterator iterator = manager.createQuery(query, Query.JCR_SQL2).execute().getRows();
        assert iterator.getSize() == nodeCount;
        while (iterator.hasNext()) {
            Row row = iterator.nextRow();
            long a = row.getValue("a").getLong();
            long b = row.getValue("b").getLong();
            assert a == fooValue;
            assert a == b;
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
