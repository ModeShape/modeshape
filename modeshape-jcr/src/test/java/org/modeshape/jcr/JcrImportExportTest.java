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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.jboss.security.config.IDTrustConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.property.Path;
import org.modeshape.jcr.JcrRepository.Option;

/**
 * 
 */
public class JcrImportExportTest {

    static {
        // Initialize the JAAS configuration to allow for an admin login later
        // Initialize IDTrust
        String configFile = "security/jaas.conf.xml";
        IDTrustConfiguration idtrustConfig = new IDTrustConfiguration();

        try {
            idtrustConfig.config(configFile);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private JcrConfiguration configuration;
    private JcrEngine engine;
    private JcrRepository repository;
    private Session session;
    private boolean print;

    @Before
    public void beforeEach() throws Exception {
        configuration = new JcrConfiguration();
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

        // Log into a session ...
        session = repository.login();
    }

    @After
    public void afterEach() throws Exception {
        if (session != null) {
            try {
                session.logout();
            } finally {
                session = null;
            }
        }
        if (engine != null) {
            try {
                engine.shutdown();
                engine.awaitTermination(3, TimeUnit.SECONDS);
            } finally {
                engine = null;
                repository = null;
                configuration = null;
            }
        }
    }

    @Test
    public void shouldStartUp() {
        assertThat(session, is(notNullValue()));
    }

    @Test
    public void shouldImportCarsSystemViewIntoNodeUnderRootWithCreateNewBehavior() throws Exception {
        assertImport("io/cars-system-view.xml", "/a/b", ImportBehavior.CREATE_NEW);
        assertThat(session, is(notNullValue()));
        assertNode("/a/b/Cars");
        assertNode("/a/b/Cars/Hybrid");
        assertNode("/a/b/Cars/Hybrid/Toyota Prius");
        assertNode("/a/b/Cars/Sports/Infiniti G37");
        assertNode("/a/b/Cars/Utility/Land Rover LR3");
        assertNoNode("/a/b/Cars[2]");
        assertNoNode("/a/b/Cars/Hybrid[2]");
        assertNoNode("/a/b/Cars/Hybrid/Toyota Prius[2]");
        assertNoNode("/a/b/Cars/Sports[2]");
    }

    @Test
    public void shouldImportCarsSystemViewIntoNodeUnderRootWithRemoveExistingBehaviorWhenThereIsNoExistingAtAll()
        throws Exception {
        assertImport("io/cars-system-view.xml", "/a/b", ImportBehavior.REMOVE_EXISTING);
        assertThat(session, is(notNullValue()));
        assertNode("/a/b/Cars");
        assertNode("/a/b/Cars/Hybrid");
        assertNode("/a/b/Cars/Hybrid/Toyota Prius");
        assertNode("/a/b/Cars/Sports/Infiniti G37");
        assertNode("/a/b/Cars/Utility/Land Rover LR3");
        assertNoNode("/a/b/Cars[2]");
        assertNoNode("/a/b/Cars/Hybrid[2]");
        assertNoNode("/a/b/Cars/Hybrid/Toyota Prius[2]");
        assertNoNode("/a/b/Cars/Sports[2]");
    }

    @Test
    public void shouldImportCarsSystemViewIntoNodeUnderRootWithRemoveExistingBehaviorWhenThereIsNoExistingWithSameUuids()
        throws Exception {
        // Set up the repository with existing content ...
        assertImport("io/cars-system-view.xml", "/a/b", ImportBehavior.CREATE_NEW);
        // Now import and remove ...
        assertImport("io/cars-system-view.xml", "/a/b", ImportBehavior.REMOVE_EXISTING);
        assertThat(session, is(notNullValue()));
        assertNode("/a/b/Cars");
        assertNode("/a/b/Cars/Hybrid");
        assertNode("/a/b/Cars/Hybrid/Toyota Prius");
        assertNode("/a/b/Cars/Sports/Infiniti G37");
        assertNode("/a/b/Cars/Utility/Land Rover LR3");
        assertNode("/a/b/Cars[2]");
        assertNode("/a/b/Cars[2]/Hybrid");
        assertNode("/a/b/Cars[2]/Hybrid/Toyota Prius");
        assertNode("/a/b/Cars[2]/Sports/Infiniti G37");
        assertNode("/a/b/Cars[2]/Utility/Land Rover LR3");
    }

    @Test
    public void shouldStartUp3() {
        assertThat(session, is(notNullValue()));
    }

    @Test
    public void shouldImportCarsSystemViewIntoNodeUnderRootWithRemoveExistingBehaviorWhenThereIsExistingWithSameUuids() {
        // Export then import ...
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Utilities
    // ----------------------------------------------------------------------------------------------------------------

    protected static enum ImportBehavior {
        CREATE_NEW(ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW),
        REPLACE_EXISTING(ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING),
        REMOVE_EXISTING(ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING),
        THROW(ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

        private final int jcrValue;

        private ImportBehavior( int value ) {
            this.jcrValue = value;
        }

        public int getJcrValue() {
            return jcrValue;
        }
    }

    protected Path path( String path ) {
        return engine.getExecutionContext().getValueFactories().getPathFactory().create(path);
    }

    protected String asString( Object value ) {
        return engine.getExecutionContext().getValueFactories().getStringFactory().create(value);
    }

    protected void assertNode( String path ) throws RepositoryException {
        // Verify that the parent node does exist now ...
        assertThat(path.startsWith("/"), is(true));
        assertThat(session.getRootNode().hasNode(path.substring(1)), is(true));
    }

    protected void assertNoNode( String path ) throws RepositoryException {
        // Verify that the parent node does exist now ...
        assertThat(path.startsWith("/"), is(true));
        assertThat(session.getRootNode().hasNode(path.substring(1)), is(false));
    }

    protected Node assertImport( String resourceName,
                                 String pathToParent,
                                 ImportBehavior behavior ) throws RepositoryException, IOException {
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
        assertThat(pathToParent.startsWith("/"), is(true));
        assertThat(session.getRootNode().hasNode(pathToParent.substring(1)), is(true));

        // Now, load the content of the resource being imported ...
        InputStream istream = resourceStream(resourceName);
        assertThat(istream, is(notNullValue()));
        try {
            session.getWorkspace().importXML(pathToParent, istream, behavior.getJcrValue());
        } finally {
            istream.close();
        }

        session.save();
        return node;
    }

    protected static URI resourceUri( String name ) throws URISyntaxException {
        return resourceUrl(name).toURI();
    }

    protected static URL resourceUrl( String name ) {
        return JcrQueryManagerTest.class.getClassLoader().getResource(name);
    }

    protected static InputStream resourceStream( String name ) {
        return JcrQueryManagerTest.class.getClassLoader().getResourceAsStream(name);
    }

}
