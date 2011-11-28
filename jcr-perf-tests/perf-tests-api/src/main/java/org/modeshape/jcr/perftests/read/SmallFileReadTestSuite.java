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
package org.modeshape.jcr.perftests.read;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.modeshape.jcr.perftests.AbstractPerformanceTestSuite;
import org.modeshape.jcr.perftests.SuiteConfiguration;
import org.modeshape.jcr.perftests.util.BinaryHelper;
import org.modeshape.jcr.perftests.util.BinaryImpl;
import java.util.Calendar;


/**
 * Performance test which reads repeatedly several nodes which represent small files.
 */
public class SmallFileReadTestSuite extends AbstractPerformanceTestSuite {

    private static final int FILE_SIZE_MB = 10 * 1024;

    private Session session;
    private Node root;
    private int fileCount;

    public SmallFileReadTestSuite( SuiteConfiguration suiteConfiguration ) {
        super(suiteConfiguration);
    }

    @Override
    public void beforeSuite() throws RepositoryException {
        fileCount = suiteConfiguration.getNodeCount();
        session = newSession();

        root = session.getRootNode().addNode("SmallFileReadTestSuite", "nt:folder");
        for (int i = 0; i < fileCount; i++) {
            Node file = root.addNode("file" + i, "nt:file");
            Node content = file.addNode("jcr:content", "nt:resource");
            content.setProperty("jcr:mimeType", "application/octet-stream");
            content.setProperty("jcr:lastModified", Calendar.getInstance());
            content.setProperty("jcr:data", new BinaryImpl(FILE_SIZE_MB));
        }
        session.save();
    }

    @Override
    public void runTest() throws Exception {
        for (int i = 0; i < fileCount; i++) {
            Node file = root.getNode("file" + i);
            Node content = file.getNode("jcr:content");
            BinaryHelper.assertExpectedSize(content.getProperty("jcr:data").getBinary(), FILE_SIZE_MB);
        }
    }

    @Override
    public void afterSuite() throws RepositoryException {
        root.remove();
        session.save();
        session.logout();
    }
}
