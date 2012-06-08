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
import java.util.ArrayList;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.collection.Problem;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrNtLexicon;

public class FileSystemRepositoryTest {

    private static final String TEST_REPOSITORY = "Test Repository Source";
    private static final String TEST_WORKSPACE = "defaultWorkspace";

    private static JcrConfiguration configuration;
    private static JcrEngine engine;
    private static List<Session> sessions = new ArrayList<Session>();
    private boolean print = false;
    private Stopwatch sw;

    @BeforeClass
    public static void beforeAll() throws Exception {
        configuration = new JcrConfiguration();
        configuration.loadFrom("src/test/resources/tck/filesystem/configRepository.xml");

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
        Session session = sessionFrom(engine);
        Node node1 = session.getRootNode().getNode("testroot/node1");
        assertThat(node1, is(notNullValue()));
        assertThat(node1.getPrimaryNodeType().getName(), is("nt:file"));
        Node node1Content = node1.getNode(stringFrom(JcrLexicon.CONTENT));
        assertThat(node1Content, is(notNullValue()));
        assertThat(node1Content.getPrimaryNodeType().getName(), is(stringFrom(JcrNtLexicon.RESOURCE)));
    }

    @Test
    public void shouldAllowNavigationOfAllContent() throws Exception {
        Session session = sessionFrom(engine);
        Node root = session.getRootNode();
        Node node1 = root.getNode("testroot/node1");
        Node node2 = root.getNode("testroot/yetAnotherNode");
        assertThat(node1.getPrimaryNodeType().getName(), is("nt:file"));
        assertThat(node2.getPrimaryNodeType().getName(), is("nt:file"));
    }

    @Test
    public void shouldAllowReadingSingleDirectory() throws Exception {
        // print = true;
        Session session = sessionFrom(engine);
        session.getRootNode();
        Node testroot = session.getNode("/testroot");
        sw.start();
        Node folder1 = testroot.getNode("folder1");
        sw.stop();
        if (print) System.out.println(sw);
        assertThat(folder1.getName(), is("folder1"));
        assertThat(folder1.getNodes().getSize(), is(3L));
        sw.reset();
        sw.start();
        Node folder1a = testroot.getNode("folder1");
        sw.stop();
        if (print) System.out.println(sw);
        assertThat(folder1a.getName(), is("folder1"));
    }

    @Test
    public void shouldFindAllModeResourceNodesUsingJcrSql2Query() throws Exception {
        Session session = sessionFrom(engine);
        String sql = "SELECT * FROM [mode:resource]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        if (print) System.out.println(result);
    }

    @Test
    public void shouldFindAllNtFileNodesUsingJcrSql2Query() throws Exception {
        Session session = sessionFrom(engine);
        String sql = "SELECT * FROM [nt:file]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        if (print) System.out.println(result);
    }

    @Test
    public void shouldFindModeResourceNodesWithAnyPropertyContainingStringUsingJcrSql2Query() throws Exception {
        Session session = sessionFrom(engine);
        String sql = "SELECT * FROM [mode:resource] WHERE CONTAINS(*,'food')";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        if (print) System.out.println(result);
        RowIterator rowIter = result.getRows();
        while (rowIter.hasNext()) {
            Row row = rowIter.nextRow();
            double score = row.getScore();
            assertThat(score > 0.0f, is(true));
        }
    }

    @Test
    public void shouldFindModeResourceNodesContainingStringUsingJcrSql2Query() throws Exception {
        Session session = sessionFrom(engine);
        String sql = "SELECT * FROM [mode:resource] WHERE CONTAINS([jcr:data],'food')";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        if (print) System.out.println(result);
        RowIterator rowIter = result.getRows();
        while (rowIter.hasNext()) {
            Row row = rowIter.nextRow();
            double score = row.getScore();
            assertThat(score > 0.0f, is(true));
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Utility Methods
    // ----------------------------------------------------------------------------------------------------------------

    protected Session sessionFrom( JcrEngine engine ) throws RepositoryException {
        Repository repository = engine.getRepository(TEST_REPOSITORY);
        Session session = repository.login(TEST_WORKSPACE);
        sessions.add(session);
        return session;
    }

    protected String stringFrom( Object object ) {
        return engine.getExecutionContext().getValueFactories().getStringFactory().create(object);
    }

    // protected static URL resourceUrl( String name ) {
    // return FileSystemRepositoryTest.class.getClassLoader().getResource(name);
    // }
    //
    // protected static InputStream resourceStream( String name ) {
    // return FileSystemRepositoryTest.class.getClassLoader().getResourceAsStream(name);
    // }

}
