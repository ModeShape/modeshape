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
package org.modeshape.jcr.perftests.read;

import javax.jcr.Node;
import javax.jcr.Session;
import org.modeshape.jcr.perftests.AbstractPerformanceTestSuite;
import org.modeshape.jcr.perftests.SuiteConfiguration;

/**
 * <code>ReadPropertyTestSuite</code> implements a performance test, which reads
 * three properties: one with a jcr prefix, one with the empty prefix and a
 * third one, which does not exist.
 */
public class ReadPropertyTestSuite extends AbstractPerformanceTestSuite {

    private static final int PROP_COUNT = 1000;

    private Session session;
    private Node root;

    public ReadPropertyTestSuite( SuiteConfiguration suiteConfiguration ) {
        super(suiteConfiguration);
    }

    @Override
    public void beforeSuite() throws Exception {
        session = newSession();
        root = session.getRootNode().addNode(getClass().getSimpleName(), "nt:unstructured");
        for (int i = 0; i < PROP_COUNT; i++) {
            root.setProperty("property" + i, "value" + i);

        }
        session.save();
    }

    @Override
    public void runTest() throws Exception {
        for (int i = 0; i < PROP_COUNT; i++) {
            String primaryType = root.getProperty("jcr:primaryType").getString();
            assert "nt:unstructured".equals(primaryType);
            String expectedValue = "value" + i;
            String propName = "property" + i;
            assert expectedValue.equals(root.getProperty(propName).getString());
            assert !root.hasProperty("does-not-exist");
        }
    }

    @Override
    public void afterSuite() throws Exception {
        root.remove();
        session.save();
        session.logout();
    }
}
