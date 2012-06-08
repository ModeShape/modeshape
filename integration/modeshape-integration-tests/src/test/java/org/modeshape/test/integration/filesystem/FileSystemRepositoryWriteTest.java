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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.jcr.Binary;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.collection.Problem;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrTools;

public class FileSystemRepositoryWriteTest {

    private static final String TEST_REPOSITORY = "Test Repository Source";
    private static final String TEST_WORKSPACE = "defaultWorkspace";

    private JcrConfiguration configuration;
    private JcrEngine engine;
    private List<Session> sessions = new ArrayList<Session>();
    @SuppressWarnings( "unused" )
    private boolean print = false;
    private JcrTools tools;

    @Before
    public void beforeAll() throws Exception {
        print = false;
        setupFileSystem("target/test/repository/filesystem", "defaultWorkspace", "otherWorkspace");

        configuration = new JcrConfiguration();
        configuration.loadFrom("src/test/resources/config/configRepositoryForModifiableFileSystem.xml");

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
        tools = new JcrTools();
    }

    @After
    public void afterAll() throws Exception {
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

    // ----------------------------------------------------------------------------------------------------------------
    // Tests
    // ----------------------------------------------------------------------------------------------------------------

    @FixFor( "MODE-866" )
    @Test
    public void shouldAllowCreatingNewFolders() throws Exception {
        Session session = sessionFrom(engine);
        session.getRootNode().addNode("folderA", "nt:folder");
        session.getRootNode().addNode("folderA/folderB", "nt:folder");
        session.save();
        session.getRootNode().addNode("folderA/folderB/folderC", "nt:folder");
        session.save();
        assertThat(session.getNode("/folderA/folderB/folderC").getPrimaryNodeType().getName(), is("nt:folder"));
    }

    @FixFor( "MODE-866" )
    @Test
    public void shouldAllowCreatingNewFiles() throws Exception {
        Session session = sessionFrom(engine);
        session.getRootNode().addNode("folderA", "nt:folder");
        session.getRootNode().addNode("folderA/folderB", "nt:folder");
        session.save();
        tools.uploadFile(session, "folderA/folderB/log4J.properties", resourceUrl("log4j.properties"));
        session.save();
        assertThat(session.getNode("/folderA/folderB").getPrimaryNodeType().getName(), is("nt:folder"));
        assertThat(session.getNode("/folderA/folderB/log4J.properties").getPrimaryNodeType().getName(), is("nt:file"));

        Session session2 = sessionFrom(engine);
        Binary binary = session2.getProperty("/folderA/folderB/log4J.properties/jcr:content/jcr:data").getBinary();
        assertThat(IoUtil.read(binary.getStream()), is(IoUtil.read(resourceUrl("log4j.properties").openStream())));
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

    protected static void setupFileSystem( String path,
                                           String... workspaceNames ) throws Exception {
        File file = new File(path);
        if (file.exists()) {
            // Clean up what's there ...
            FileUtil.delete(file);
        }

        // Now make sure these directories exist ...
        file.mkdirs();

        // Now create the workspace folders ...
        for (String workspaceName : workspaceNames) {
            new File(file, workspaceName).mkdir();
        }
    }

    protected static URL resourceUrl( String name ) {
        URL url = FileSystemRepositoryTest.class.getClassLoader().getResource(name);
        assertThat(url, is(notNullValue()));
        return url;
    }

    // protected static InputStream resourceStream( String name ) {
    // return FileSystemRepositoryTest.class.getClassLoader().getResourceAsStream(name);
    // }

}
