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

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.modeshape.jcr.perftests.AbstractPerformanceTestSuite;
import org.modeshape.jcr.perftests.SuiteConfiguration;
import org.modeshape.jcr.perftests.util.BinaryImpl;

public class BigFileWriteTestSuite extends AbstractPerformanceTestSuite {

    private static final int FILE_SIZE_MB = 100;

    private Session session;
    private Node root;
    private int fileCount;

    public BigFileWriteTestSuite( SuiteConfiguration suiteConfiguration ) {
        super(suiteConfiguration);
    }

    @Override
    public void beforeSuite() throws RepositoryException {
        //        failOnRepositoryVersions("1.4", "1.5", "1.6");
        session = newSession();
        root = session.getRootNode().addNode("RootFolder", "nt:folder");
        session.save();
    }

    @Override
    public void runTest() throws RepositoryException {
        for (int i = 0; i < suiteConfiguration.getNodeCount(); i++) {
            Node file = root.addNode("BigFileWriteTestSuite" + i, "nt:file");
            Node content = file.addNode("jcr:content", "nt:resource");
            content.setProperty("jcr:mimeType", "application/octet-stream");
            content.setProperty("jcr:lastModified", Calendar.getInstance());
            content.setProperty("jcr:data", new BinaryImpl(FILE_SIZE_MB * 1024 * 1024));
            session.save();
        }
    }

    @Override
    protected void afterSuite() throws Exception {
        root.remove();
        session.save();
    }
}
