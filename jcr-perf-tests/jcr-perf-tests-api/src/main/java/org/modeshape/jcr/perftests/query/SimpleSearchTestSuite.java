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

import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import org.modeshape.jcr.perftests.AbstractPerformanceTestSuite;
import org.modeshape.jcr.perftests.SuiteConfiguration;

public class SimpleSearchTestSuite extends AbstractPerformanceTestSuite {

    private Session session;
    private Node root;
    private int nodeCount;

    public SimpleSearchTestSuite( SuiteConfiguration suiteConfiguration ) {
        super(suiteConfiguration);
    }

    protected Query createQuery( QueryManager manager, int i )
            throws RepositoryException {
        return manager.createQuery("//*[@testcount=" + i + "]", Query.XPATH);
    }

    @Override
    public void beforeSuite() throws RepositoryException {
        session = newSession();
        root = session.getRootNode().addNode("testroot", "nt:unstructured");
        nodeCount = suiteConfiguration.getNodeCount();
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
                assert node.getProperty("testcount").getLong() == i;
            }
        }
    }

    @Override
    public void afterSuite() throws RepositoryException {
        root.remove();
        session.save();
        session.logout();
    }

    @Override
    public boolean isCompatibleWithCurrentRepository() {
        String xpathSupported = suiteConfiguration.getRepository().getDescriptor(Repository.OPTION_QUERY_SQL_SUPPORTED);
        return xpathSupported != null && xpathSupported.equalsIgnoreCase(Boolean.TRUE.toString());
    }
}
