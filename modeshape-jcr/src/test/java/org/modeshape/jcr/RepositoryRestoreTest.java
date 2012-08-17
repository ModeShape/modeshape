/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.api.Problems;

/**
 * Test performance writing graph subtrees of various sizes with varying number of properties
 */
public class RepositoryRestoreTest extends SingleUseAbstractTest {

    private File backupDirectory;

    @Override
    protected RepositoryConfiguration createRepositoryConfiguration( String repositoryName,
                                                                     Environment environment ) throws Exception {
        return RepositoryConfiguration.read("config/backup-repo-config.json").with(environment);
    }

    @Before
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        backupDirectory = new File("target/backupArea/repoBackups");
        FileUtil.delete(backupDirectory);
        backupDirectory.mkdirs();
    }

    @Test
    @Ignore( "Comment out when generating and writing export files" )
    public void testExporting() throws Exception {
        print = true;
        String path = "/backupAndRestoreTestContent";
        populateRepositoryContent(session(), path);
        FileOutputStream stream = new FileOutputStream("src/test/resources/io/generated-3-system-view.xml");
        session().exportSystemView(path, stream, false, false);
        stream.close();
    }

    @Test
    public void shouldBackupRepositoryWithMultipleWorkspaces() throws Exception {
        loadContent();
        Problems problems = session().getWorkspace().getRepositoryManager().backupRepository(backupDirectory);
        assertNoProblems(problems);

        // Start up a new repository
        RepositoryConfiguration config = RepositoryConfiguration.read("config/restore-repo-config.json").with(environment);
        JcrRepository newRepository = new JcrRepository(config);
        try {
            newRepository.start();

            // And restore it from the contents ...
            JcrSession newSession = newRepository.login();
            try {
                Problems restoreProblems = newSession.getWorkspace().getRepositoryManager().restoreRepository(backupDirectory);
                assertNoProblems(restoreProblems);
            } finally {
                newSession.logout();
            }
        } finally {
            newRepository.shutdown().get(10, TimeUnit.SECONDS);
        }
    }

    protected void assertNoProblems( Problems problems ) {
        if (problems.hasProblems()) {
            System.out.println(problems);
        }
        assertThat(problems.hasProblems(), is(false));
    }

    protected void loadContent() throws Exception {
        importIntoWorkspace("default", "io/generated-1-system-view.xml");
        importIntoWorkspace("ws2", "io/generated-2-system-view.xml");
        importIntoWorkspace("ws3", "io/generated-3-system-view.xml");
    }

    protected void importIntoWorkspace( String workspaceName,
                                        String resourcePath ) throws IOException, RepositoryException {
        Session session = null;
        try {
            session = repository().login(workspaceName);
        } catch (NoSuchWorkspaceException e) {
            // Create the workspace ...
            session().getWorkspace().createWorkspace(workspaceName);
            // Create a new session ...
            session = repository().login(workspaceName);
        }
        try {
            Node testNode = session().getRootNode().addNode("testContent-" + workspaceName);
            session().save();
            Stopwatch sw = new Stopwatch();
            sw.start();
            importContent(testNode, resourcePath, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
            sw.stop();
            // System.out.println("Time to import: " + sw.getTotalDuration());
        } finally {
            session.logout();
        }
    }

    protected void populateRepositoryContent( Session session,
                                              String testName ) throws Exception {
        int depth = 6;
        int breadth = 3;
        int properties = 6;
        session.getRootNode().addNode(testName, "nt:unstructured");
        createSubgraph(session(), testName, depth, breadth, properties, false, new Stopwatch(), print ? System.out : null, null);
    }
}
