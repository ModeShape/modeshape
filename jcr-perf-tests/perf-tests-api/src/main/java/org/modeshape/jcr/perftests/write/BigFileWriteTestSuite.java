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
import org.modeshape.jcr.perftests.util.BinaryImpl;
import java.util.Calendar;

public class BigFileWriteTestSuite extends AbstractPerformanceTestSuite {

    private static final int FILE_SIZE_MB = 100 * 1024 * 1024;

    private Session session;
    private Node root;
    private int fileCount = 2;

    public BigFileWriteTestSuite( SuiteConfiguration suiteConfiguration ) {
        super(suiteConfiguration);
    }

    @Override
    public void beforeSuite() throws RepositoryException {
        session = newSession();
        root = session.getRootNode().addNode("RootFolder", "nt:folder");
        session.save();
    }

    @Override
    public void runTest() throws RepositoryException {
        for (int i = 0; i < fileCount; i++) {
            Node file = root.addNode("BigFileWriteTestSuite" + i, "nt:file");
            Node content = file.addNode("jcr:content", "nt:resource");
            content.setProperty("jcr:mimeType", "application/octet-stream");
            content.setProperty("jcr:lastModified", Calendar.getInstance());
            content.setProperty("jcr:data", new BinaryImpl(FILE_SIZE_MB ));
            session.save();
        }
    }

    @Override
    protected void afterTestRun() throws Exception {
        for (int i = 0; i < fileCount; i++) {
            root.getNode("BigFileWriteTestSuite" + i).remove();
        }
        session.save();
    }

    @Override
    protected void afterSuite() throws Exception {
        root.remove();
        session.save();
    }
}
