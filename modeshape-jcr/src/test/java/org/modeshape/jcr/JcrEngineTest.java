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
import java.net.URL;
import java.util.concurrent.TimeUnit;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import org.junit.After;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.collection.Problem;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.jcr.JcrRepository.Option;

/**
 * 
 */
public class JcrEngineTest {

    protected static URL resourceUrl( String name ) {
        return JcrQueryManagerTest.class.getClassLoader().getResource(name);
    }

    private JcrConfiguration configuration;
    private JcrEngine engine;
    private JcrRepository repository;
    private Session session;

    @After
    public void afterEach() {
        configuration = null;

        try {
            if (session != null) {
                session.logout();
            }
        } finally {
            session = null;
            try {
                if (engine != null) {
                    engine.shutdown();
                }
            } finally {
                engine = null;
            }
        }
    }

    @Test
    public void shouldCreateRepositoryEngineFromConfigurationFileWithDefaultNamespace() throws Exception {
        configuration = new JcrConfiguration().loadFrom("src/test/resources/config/configRepositoryWithDefaultNamespace.xml");
        engine = configuration.build();
        engine.start();
        repository = engine.getRepository("mode:Car Repository");
        session = repository.login();
    }

    @Test
    public void shouldCreateRepositoryConfiguredWithOneCompactNodeTypeDefinitionFile() throws Exception {
        configuration = new JcrConfiguration();
        configuration.repositorySource("car-source")
                     .usingClass(InMemoryRepositorySource.class)
                     .setDescription("The automobile content");
        configuration.repository("cars")
                     .setSource("car-source")
                     .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
                     .addNodeTypes(resourceUrl("cars.cnd"))
                     .setOption(Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);
        engine = configuration.build();
        engine.start();
        repository = engine.getRepository("cars");
        session = repository.login();

        assertNodeType("car:Car", false, false, true, false, null, 0, 12, "nt:unstructured", "mix:created");
    }

    @Test( expected = JcrConfigurationException.class )
    public void shouldFailToStartIfAnyRepositorySourceDoesNotDefineClass() throws Exception {
        configuration = new JcrConfiguration();
        configuration.repositorySource("car-source")
        // .usingClass(InMemoryRepositorySource.class)
                     .setDescription("The automobile content");
        configuration.repository("cars")
                     .setSource("car-source-non-existant")
                     .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
                     .addNodeTypes(resourceUrl("cars.cnd"))
                     .setOption(Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);
        engine = configuration.build();
        engine.start();
    }

    @Test( expected = JcrConfigurationException.class )
    public void shouldFailToStartIfAnyRepositoryReferencesNonExistantSource() throws Exception {
        configuration = new JcrConfiguration();
        configuration.repositorySource("car-source")
                     .usingClass(InMemoryRepositorySource.class)
                     .setDescription("The automobile content");
        configuration.repository("cars")
                     .setSource("car-source-non-existant")
                     .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
                     .addNodeTypes(resourceUrl("cars.cnd"))
                     .setOption(Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);
        engine = configuration.build();
        engine.start();
    }

    @Test
    public void shouldCreateRepositoryEngineFromConfigurationFileWithInitialContentInFileWithRelativePath() throws Exception {
        configuration = new JcrConfiguration().loadFrom("src/test/resources/config/configRepositoryWithInitialContent.xml");
        engine = configuration.build();
        engine.start();
        repository = engine.getRepository("My Repository");
        session = repository.login();

        javax.jcr.Node cars = session.getRootNode().getNode("Cars");
        javax.jcr.Node prius = session.getRootNode().getNode("Cars/Hybrid/Toyota Prius");
        javax.jcr.Node g37 = session.getRootNode().getNode("Cars/Sports/Infiniti G37");
        assertThat(cars, is(notNullValue()));
        assertThat(prius, is(notNullValue()));
        assertThat(g37, is(notNullValue()));
    }

    @Test
    public void shouldCreateRepositoryEngineFromConfigurationFileWithInitialContentInFileOnClasspath() throws Exception {
        configuration = new JcrConfiguration().loadFrom("src/test/resources/config/configRepositoryWithInitialContentOnClasspath.xml");
        engine = configuration.build();
        engine.start();
        repository = engine.getRepository("My Repository");
        session = repository.login();

        javax.jcr.Node cars = session.getRootNode().getNode("Cars");
        javax.jcr.Node prius = session.getRootNode().getNode("Cars/Hybrid/Toyota Prius");
        javax.jcr.Node g37 = session.getRootNode().getNode("Cars/Sports/Infiniti G37");
        assertThat(cars, is(notNullValue()));
        assertThat(prius, is(notNullValue()));
        assertThat(g37, is(notNullValue()));
    }

    // @Test
    // public void shouldCreateRepositoryConfiguredWithOneXmlNodeTypeDefinitionFiles() throws Exception {
    // configuration = new JcrConfiguration();
    // configuration.repositorySource("car-source")
    // .usingClass(InMemoryRepositorySource.class)
    // .setDescription("The automobile content");
    // configuration.repository("cars")
    // .setSource("car-source")
    // .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
    // .addNodeTypes(resourceUrl("xmlNodeTypeRegistration/owfe_nodetypes.xml"))
    // .setOption(Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);
    // engine = configuration.build();
    // engine.start();
    //
    // repository = engine.getRepository("cars");
    // session = repository.login();
    //
    // assertNodeType("mgnl:workItem", false, false, true, true, null, 1, 1, "nt:hierarchyNode");
    // }

    // @Test
    // public void shouldCreateRepositoryConfiguredWithMultipleNodeTypeDefinitionFiles() throws Exception {
    // configuration = new JcrConfiguration();
    // configuration.repositorySource("car-source")
    // .usingClass(InMemoryRepositorySource.class)
    // .setDescription("The automobile content");
    // configuration.repository("cars")
    // .setSource("car-source")
    // .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
    // .addNodeTypes(resourceUrl("cars.cnd"))
    // .addNodeTypes(resourceUrl("xmlNodeTypeRegistration/owfe_nodetypes.xml"))
    // .setOption(Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);
    // engine = configuration.build();
    // engine.start();
    //
    // repository = engine.getRepository("cars");
    // session = repository.login();
    //
    // assertNodeType("car:Car", false, false, true, false, null, 0, 11, "nt:unstructured", "mix:created");
    // assertNodeType("mgnl:workItem", false, false, true, true, null, 1, 1, "nt:hierarchyNode");
    // }

    protected void assertNodeType( String name,
                                   boolean isAbstract,
                                   boolean isMixin,
                                   boolean isQueryable,
                                   boolean hasOrderableChildNodes,
                                   String primaryItemName,
                                   int numberOfDeclaredChildNodeDefinitions,
                                   int numberOfDeclaredPropertyDefinitions,
                                   String... supertypes ) throws Exception {
        NodeType nodeType = session.getWorkspace().getNodeTypeManager().getNodeType(name);
        assertThat(nodeType, is(notNullValue()));
        assertThat(nodeType.isAbstract(), is(isAbstract));
        assertThat(nodeType.isMixin(), is(isMixin));
        assertThat(nodeType.isQueryable(), is(isQueryable));
        assertThat(nodeType.hasOrderableChildNodes(), is(hasOrderableChildNodes));
        assertThat(nodeType.getPrimaryItemName(), is(primaryItemName));
        assertThat(nodeType.getDeclaredSupertypes().length, is(supertypes.length));
        for (int i = 0; i != supertypes.length; ++i) {
            assertThat(nodeType.getDeclaredSupertypes()[i].getName(), is(supertypes[i]));
        }
        assertThat(nodeType.getDeclaredChildNodeDefinitions().length, is(numberOfDeclaredChildNodeDefinitions));
        assertThat(nodeType.getDeclaredPropertyDefinitions().length, is(numberOfDeclaredPropertyDefinitions));

    }

    @Test
    public void shouldAllowCreatingWorkspaces() throws Exception {
        configuration = new JcrConfiguration().loadFrom("src/test/resources/config/configRepositoryWithDefaultNamespace.xml");
        engine = configuration.build();
        engine.start();
        repository = engine.getRepository("mode:Car Repository");

        String workspaceName = "MyNewWorkspace";

        // Create a session ...
        Session jcrSession = repository.login();
        Workspace defaultWorkspace = jcrSession.getWorkspace();
        defaultWorkspace.createWorkspace(workspaceName);
        assertAccessibleWorkspace(defaultWorkspace, workspaceName);
        jcrSession.logout();
    }

    @FixFor( "MODE-1180" )
    @Test
    public void shouldCreateRepositoriesUponStartup() throws Exception {
        configuration = new JcrConfiguration();
        configuration.repositorySource("car-source")
                     .usingClass(InMemoryRepositorySource.class)
                     .setDescription("The automobile content")
                     .setProperty("defaultWorkspaceName", "default");
        configuration.repository("cars")
                     .setSource("car-source")
                     .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
                     .addNodeTypes(resourceUrl("cars.cnd"))
                     .setInitialContent("src/test/resources/initialWorkspaceContent.xml", "default")
                     .setOption(Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);
        configuration.repositorySource("product-source")
                     .usingClass(InMemoryRepositorySource.class)
                     .setDescription("The products content")
                     .setProperty("defaultWorkspaceName", "default");
        configuration.repository("products")
                     .setSource("product-source")
                     .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
                     .addNodeTypes(resourceUrl("cars.cnd"))
                     .setInitialContent("src/test/resources/initialWorkspaceContent.xml", "default")
                     .setOption(Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);
        engine = configuration.build();
        assertThat(engine.getProblems().hasErrors(), is(false));
        engine.start(true, 1L, TimeUnit.NANOSECONDS);
        repository = engine.getRepository("products");
        repository = engine.getRepository("cars");
        session = repository.login();

        assertNodeType("car:Car", false, false, true, false, null, 0, 12, "nt:unstructured", "mix:created");

        // Check that the content is there...
        assertThat(session.getRootNode().hasNode("Cars"), is(true));
        assertThat(session.getNode("/Cars"), is(notNullValue()));
    }

    @FixFor( "MODE-1119" )
    @Test
    public void shouldCreateRepositoryConfiguredWithNoInitialContent() throws Exception {
        configuration = new JcrConfiguration();
        configuration.repositorySource("car-source")
                     .usingClass(InMemoryRepositorySource.class)
                     .setDescription("The automobile content")
                     .setProperty("defaultWorkspaceName", "default");
        configuration.repository("cars")
                     .setSource("car-source")
                     .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
                     .addNodeTypes(resourceUrl("cars.cnd"))
                     .setOption(Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);
        engine = configuration.build();
        assertThat(engine.getProblems().hasErrors(), is(false));
        engine.start();
        repository = engine.getRepository("cars");
        session = repository.login();

        assertNodeType("car:Car", false, false, true, false, null, 0, 12, "nt:unstructured", "mix:created");

        // Check that the content is not there...
        assertThat(session.getRootNode().hasNode("Cars"), is(false));
    }

    @FixFor( "MODE-1119" )
    @Test
    public void shouldCreateRepositoryConfiguredWithCorrectInitialContentPath() throws Exception {
        configuration = new JcrConfiguration();
        configuration.repositorySource("car-source")
                     .usingClass(InMemoryRepositorySource.class)
                     .setDescription("The automobile content")
                     .setProperty("defaultWorkspaceName", "default");
        configuration.repository("cars")
                     .setSource("car-source")
                     .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
                     .addNodeTypes(resourceUrl("cars.cnd"))
                     .setInitialContent("src/test/resources/initialWorkspaceContent.xml", "default")
                     .setOption(Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);
        engine = configuration.build();
        assertThat(engine.getProblems().hasErrors(), is(false));
        engine.start();
        repository = engine.getRepository("cars");
        session = repository.login();

        assertNodeType("car:Car", false, false, true, false, null, 0, 12, "nt:unstructured", "mix:created");

        // Check that the content is there...
        assertThat(session.getRootNode().hasNode("Cars"), is(true));
        assertThat(session.getNode("/Cars"), is(notNullValue()));
    }

    @FixFor( "MODE-1119" )
    @Test( expected = RepositoryException.class )
    public void shouldCreateRepositoryConfiguredWithValidInitialContentPathButEmptyFile() throws Exception {
        configuration = new JcrConfiguration();
        configuration.repositorySource("car-source")
                     .usingClass(InMemoryRepositorySource.class)
                     .setDescription("The automobile content")
                     .setProperty("defaultWorkspaceName", "default");
        configuration.repository("cars")
                     .setSource("car-source")
                     .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
                     .addNodeTypes(resourceUrl("cars.cnd"))
                     .setInitialContent("src/test/resources/emptyFile.xml", "default")
                     .setOption(Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);
        engine = configuration.build();
        assertThat(engine.getProblems().hasErrors(), is(false));
        engine.start();
        repository = engine.getRepository("cars");
    }

    @FixFor( "MODE-1119" )
    @Test( expected = RepositoryException.class )
    public void shouldCreateRepositoryConfiguredWithIncorrectInitialContentPath() throws Exception {
        configuration = new JcrConfiguration();
        configuration.repositorySource("car-source")
                     .usingClass(InMemoryRepositorySource.class)
                     .setDescription("The automobile content")
                     .setProperty("defaultWorkspaceName", "default");
        configuration.repository("cars")
                     .setSource("car-source")
                     .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
                     .addNodeTypes(resourceUrl("cars.cnd"))
                     .setInitialContent("src/test/resources/blah/blah/blah", "default")
                     .setOption(Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);
        engine = configuration.build();
        assertThat(engine.getProblems().hasErrors(), is(false));
        engine.start();
        repository = engine.getRepository("cars");
    }

    @FixFor( "MODE-1119" )
    @Test
    public void shouldRecordProblemWhenStartingRepositoriesConfiguredWithValidInitialContentPathButEmptyFile() throws Exception {
        configuration = new JcrConfiguration();
        configuration.repositorySource("car-source")
                     .usingClass(InMemoryRepositorySource.class)
                     .setDescription("The automobile content")
                     .setProperty("defaultWorkspaceName", "default");
        configuration.repository("cars")
                     .setSource("car-source")
                     .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
                     .addNodeTypes(resourceUrl("cars.cnd"))
                     .setInitialContent("src/test/resources/emptyFile.xml", "default")
                     .setOption(Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);
        engine = configuration.build();
        assertThat(engine.getProblems().hasErrors(), is(false));
        engine.start(true);
        assertThat(engine.getProblems().hasErrors(), is(true));
        assertThat(engine.getProblems().size(), is(1)); // one error
        assertThat(engine.getProblems().iterator().next().getStatus(), is(Problem.Status.ERROR));
    }

    @FixFor( "MODE-1119" )
    @Test( expected = RepositoryException.class )
    public void shouldRecordProblemWhenStartingRepositoriesConfiguredWithValidInitialContentPathButEmptyFileAndFailWhenGettingRepository()
        throws Exception {
        configuration = new JcrConfiguration();
        configuration.repositorySource("car-source")
                     .usingClass(InMemoryRepositorySource.class)
                     .setDescription("The automobile content")
                     .setProperty("defaultWorkspaceName", "default");
        configuration.repository("cars")
                     .setSource("car-source")
                     .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
                     .addNodeTypes(resourceUrl("cars.cnd"))
                     .setInitialContent("src/test/resources/emptyFile.xml", "default")
                     .setOption(Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);
        engine = configuration.build();
        assertThat(engine.getProblems().hasErrors(), is(false));
        engine.start(true);
        assertThat(engine.getProblems().hasErrors(), is(true));
        assertThat(engine.getProblems().size(), is(1)); // one error
        assertThat(engine.getProblems().iterator().next().getStatus(), is(Problem.Status.ERROR));
        // The following will fail ...
        engine.getRepository("cars");
    }

    @FixFor( "MODE-1119" )
    @Test
    public void shouldRecordProblemWhenStartingRepositoriesConfiguredWithIncorrectInitialContentPath() throws Exception {
        configuration = new JcrConfiguration();
        configuration.repositorySource("car-source")
                     .usingClass(InMemoryRepositorySource.class)
                     .setDescription("The automobile content")
                     .setProperty("defaultWorkspaceName", "default");
        configuration.repository("cars")
                     .setSource("car-source")
                     .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
                     .addNodeTypes(resourceUrl("cars.cnd"))
                     .setInitialContent("src/test/resources/blah/blah/blah", "default")
                     .setOption(Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);
        engine = configuration.build();
        assertThat(engine.getProblems().hasErrors(), is(false));
        engine.start(true);
        assertThat(engine.getProblems().hasErrors(), is(true));
        assertThat(engine.getProblems().size(), is(1)); // one error
        assertThat(engine.getProblems().iterator().next().getStatus(), is(Problem.Status.ERROR));
    }

    protected void assertAccessibleWorkspace( Session session,
                                              String workspaceName ) throws Exception {
        assertAccessibleWorkspace(session.getWorkspace(), workspaceName);
    }

    protected void assertAccessibleWorkspace( Workspace workspace,
                                              String workspaceName ) throws Exception {
        assertContains(workspace.getAccessibleWorkspaceNames(), workspaceName);
    }

    protected void assertContains( String[] actuals,
                                   String... expected ) {
        // Each expected must appear in the actuals ...
        for (String expect : expected) {
            if (expect == null) continue;
            boolean found = false;
            for (String actual : actuals) {
                if (expect.equals(actual)) {
                    found = true;
                    break;
                }
            }
            assertThat("Did not find '" + expect + "' in the actuals: " + actuals, found, is(true));
        }
    }
}
