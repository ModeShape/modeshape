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
package org.modeshape.test.integration.filesystem;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.collection.Problem;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.JcrNtLexicon;
import org.modeshape.graph.property.basic.FileSystemBinary;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;

public class FileSystemRepositoryScalabilityTest {

    private static final String TEST_REPOSITORY = "Source Code Repository";

    private static JcrConfiguration configuration;
    private static JcrEngine engine;
    private static List<Session> sessions = new ArrayList<Session>();
    private boolean print = false;
    private Stopwatch sw;

    @BeforeClass
    public static void beforeAll() throws Exception {
        new FileSystemBinary(new File(".")); // initialize the SHA-1 algorithm

        configuration = new JcrConfiguration();
        configuration.loadFrom("src/test/resources/config/configRepositoryForSourceCodeFileSystem.xml");

        // Create an engine and use it to populate the source ...
        engine = configuration.build();
        try {
            engine.start();
        } catch (RuntimeException e) {
            // There was a problem starting the engine ...
            System.err.println("There were problems starting the engine:");
            for (Problem problem : engine.getProblems()) {
                System.err.println(problem);
            }
            throw e;
        }
    }

    @AfterClass
    public static void afterAll() throws Exception {
        // Close all of the sessions ...
        for (Session session : sessions) {
            if (session.isLive()) session.logout();
        }
        sessions.clear();

        // Shut down the engines ...
        if (engine != null) {
            try {
                engine.shutdown();
            } finally {
                engine = null;
            }
        }
    }

    @Before
    public void beforeEach() {
        sw = new Stopwatch();
        print = false;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Tests
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldHaveContentInWorkspace() throws Exception {
        Session session = sessionFrom(engine, "modeshape-integration-tests");
        Node node1 = session.getRootNode().getNode("pom.xml");
        assertThat(node1, is(notNullValue()));
        assertThat(node1.getPrimaryNodeType().getName(), is("nt:file"));
        Node node1Content = node1.getNode(stringFrom(JcrLexicon.CONTENT));
        assertThat(node1Content, is(notNullValue()));
        assertThat(node1Content.getPrimaryNodeType().getName(), is(stringFrom(JcrNtLexicon.RESOURCE)));
    }

    @Test
    public void shouldAccessDirectoryWithManyFiles() throws Exception {
        print = true;
        Session session = sessionFrom(engine, "modeshape-integration-tests");
        sw.start();
        Node node1 = session.getNode("/src/test/resources/svn/local_repos/dummy_svn_repos/db/revprops");
        sw.stop();
        if (print) System.out.println(sw);
        assertThat(node1, is(notNullValue()));
        assertThat(node1.getPrimaryNodeType().getName(), is("nt:folder"));
        assertThat(node1.getNodes().getSize(), is(33L));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Utility Methods
    // ----------------------------------------------------------------------------------------------------------------

    protected Session sessionFrom( JcrEngine engine,
                                   String workspace ) throws RepositoryException {
        Repository repository = engine.getRepository(TEST_REPOSITORY);
        Session session = repository.login(workspace);
        sessions.add(session);
        return session;
    }

    protected String stringFrom( Object object ) {
        return engine.getExecutionContext().getValueFactories().getStringFactory().create(object);
    }
}
