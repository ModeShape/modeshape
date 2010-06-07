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
package org.modeshape.jdbc;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.ModeShapeRoles;
import org.modeshape.jcr.JcrRepository.Option;

public class JcrMetaDataTest {

    private static JcrEngine engine;
    private static Repository repository;
    private static Session session;
    private static boolean print = false;
    private JcrMetaData metadata;
    @Mock
    private JcrConnection connection;

    // ----------------------------------------------------------------------------------------------------------------
    // Setup/Teardown methods
    // ----------------------------------------------------------------------------------------------------------------

    @BeforeClass
    public static void beforeAll() throws Exception {
        JcrConfiguration configuration = new JcrConfiguration();
        configuration.repositorySource("source").usingClass(InMemoryRepositorySource.class).setDescription("The content store");
        configuration.repository("repo")
                     .setSource("source")
                     .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
                     .addNodeTypes(resourceUrl("cars.cnd"))
                     .setOption(Option.ANONYMOUS_USER_ROLES,
                                ModeShapeRoles.READONLY + "," + ModeShapeRoles.READWRITE + "," + ModeShapeRoles.ADMIN)
                     .setOption(Option.JAAS_LOGIN_CONFIG_NAME, "modeshape-jcr");
        engine = configuration.build();
        engine.start();

        // Start the repository ...
        repository = engine.getRepository("repo");

        // Create the session and load the content ...
        session = repository.login();
        assertImport("cars-system-view-with-uuids.xml", "/");
    }

    @AfterClass
    public static void afterAll() {
        try {
            if (session != null) session.logout();
        } finally {
            session = null;
            try {
                engine.shutdown();
            } finally {
                engine = null;
            }
        }
    }

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        print = false;
        metadata = new JcrMetaData(connection, session);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Test methods
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldHaveSession() {
        assertThat(session, is(notNullValue()));
    }

    @Test
    public void shouldHaveMetaData() {
        assertThat(metadata, is(notNullValue()));
    }

    @Test
    public void shouldHaveMajorVersion() {
        assertThat(metadata.getDriverMajorVersion(), is(TestUtil.majorVersion()));
    }

    @Test
    public void shouldHaveMinorVersion() {
        assertThat(metadata.getDriverMinorVersion() > 1, is(TestUtil.hasMinorVersion()));
    }

    @Test
    public void shouldHaveVendorUrl() {
        assertThat(metadata.getDriverName(), is(JdbcI18n.driverName.text()));
    }

    @Test
    public void shouldHaveVendorName() {
        assertThat(metadata.getDriverVersion(), is(JdbcI18n.driverVersion.text()));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Utility methods
    // ----------------------------------------------------------------------------------------------------------------

    protected static URL resourceUrl( String name ) {
        return JcrMetaDataTest.class.getClassLoader().getResource(name);
    }

    protected static InputStream resourceStream( String name ) {
        return JcrMetaDataTest.class.getClassLoader().getResourceAsStream(name);
    }

    protected static Node assertImport( String resourceName,
                                        String pathToParent ) throws RepositoryException, IOException {
        InputStream istream = resourceStream(resourceName);
        return assertImport(istream, pathToParent);
    }

    protected static Node assertImport( File resource,
                                        String pathToParent ) throws RepositoryException, IOException {
        InputStream istream = new FileInputStream(resource);
        return assertImport(istream, pathToParent);
    }

    protected static Node assertImport( InputStream istream,
                                        String pathToParent ) throws RepositoryException, IOException {
        // Make the parent node if it does not exist ...
        Path parentPath = path(pathToParent);
        assertThat(parentPath.isAbsolute(), is(true));
        Node node = session.getRootNode();
        boolean found = true;
        for (Path.Segment segment : parentPath) {
            String name = asString(segment);
            if (found) {
                try {
                    node = node.getNode(name);
                    found = true;
                } catch (PathNotFoundException e) {
                    found = false;
                }
            }
            if (!found) {
                node = node.addNode(name, "nt:unstructured");
            }
        }
        if (!found) {
            // We added at least one node, so we need to save it before importing ...
            session.save();
        }

        // Verify that the parent node does exist now ...
        assertNode(pathToParent);

        // Now, load the content of the resource being imported ...
        assertThat(istream, is(notNullValue()));
        try {
            session.getWorkspace().importXML(pathToParent, istream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        } finally {
            istream.close();
        }

        session.save();
        return node;
    }

    protected static Path path( String path ) {
        return engine.getExecutionContext().getValueFactories().getPathFactory().create(path);
    }

    protected static String relativePath( String path ) {
        return !path.startsWith("/") ? path : path.substring(1);
    }

    protected static String asString( Object value ) {
        return engine.getExecutionContext().getValueFactories().getStringFactory().create(value);
    }

    protected static void assertNode( String path ) throws RepositoryException {
        // Verify that the parent node does exist now ...
        String relativePath = relativePath(path);
        Node root = session.getRootNode();
        if (relativePath.trim().length() == 0) {
            // This is the root path, so of course it exists ...
            assertThat(root, is(notNullValue()));
            return;
        }
        if (print && !root.hasNode(relativePath)) {
            Node parent = root;
            int depth = 0;
            for (Segment segment : path(path)) {
                if (!parent.hasNode(asString(segment))) {
                    System.out.println("Unable to find '" + path + "'; lowest node is '" + parent.getPath() + "'");
                    break;
                }
                parent = parent.getNode(asString(segment));
                ++depth;
            }
        }
        assertThat(root.hasNode(relativePath), is(true));
    }
}
