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

import java.util.ArrayList;
import java.util.List;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.collection.Problem;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrTools;

public class FileSystemRepositoryDeleteTest {

    private static final String TEST_REPOSITORY = "Test Repository Source";
    private static final String TEST_WORKSPACE = "defaultWorkspace";

    private JcrConfiguration configuration;
    private JcrEngine engine;
    private List<Session> sessions = new ArrayList<Session>();
    @SuppressWarnings( "unused" )
    private boolean print = false;
    private JcrTools tools;

    protected Session sessionFrom( JcrEngine engine ) throws RepositoryException {
        Repository repository = engine.getRepository(TEST_REPOSITORY);
        Session session = repository.login(TEST_WORKSPACE);
        sessions.add(session);
        return session;
    }

    @Before
    public void beforeAll() throws Exception {
        print = false;
        FileSystemRepositoryWriteTest.setupFileSystem("target/test/repository/deletefilesystem",
                                                      "defaultWorkspace",
                                                      "otherWorkspace");

        configuration = new JcrConfiguration();
        configuration.loadFrom("src/test/resources/config/configRepositoryForPropertyStoreModifiable.xml");

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

    @Test
    @FixFor( "MODE-927" )
    public void testCreateAndDeleteANode() throws Exception {
        Session session = sessionFrom(engine);
        session.getRootNode().addNode("folderA", "nt:folder");
        session.save();
        // Node file = session.getRootNode().getNode("folderA").addNode("log4j.properties","nt:file");
        tools.uploadFile(session, "folderA/log4J.properties", FileSystemRepositoryWriteTest.resourceUrl("log4j.properties"));
        session.save();
        session.getNode("/folderA/log4J.properties").remove();
    }
}
