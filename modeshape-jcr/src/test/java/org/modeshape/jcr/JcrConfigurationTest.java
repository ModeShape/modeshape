/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
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
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.modeshape.graph.IsNodeWithChildren.hasChild;
import static org.modeshape.graph.IsNodeWithChildren.hasChildren;
import static org.modeshape.graph.IsNodeWithProperty.hasProperty;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.NodeTypeManager;
import org.jboss.security.config.IDTrustConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.cache.ImmutableCachePolicy;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.mimetype.ExtensionBasedMimeTypeDetector;
import org.modeshape.graph.property.Path;
import org.modeshape.jcr.JcrRepository.DefaultOption;
import org.modeshape.jcr.JcrRepository.Option;
import org.modeshape.repository.ModeShapeConfiguration;
import org.modeshape.repository.ModeShapeLexicon;
import org.modeshape.repository.ModeShapeConfiguration.ConfigurationDefinition;

public class JcrConfigurationTest {

    private JcrConfiguration configuration;
    private JcrEngine engine;

    @Before
    public void beforeEach() {
        configuration = new JcrConfiguration();
    }

    @After
    public void afterEach() throws Exception {
        if (engine != null) {
            try {
                engine.shutdown();
                engine.awaitTermination(3, TimeUnit.SECONDS);
            } finally {
                engine = null;
            }
        }
    }

    protected ExecutionContext context() {
        return configuration.getConfigurationDefinition().getContext();
    }

    protected Path path( String path ) {
        return context().getValueFactories().getPathFactory().create(path);
    }

    protected Path.Segment segment( String segment ) {
        return context().getValueFactories().getPathFactory().createSegment(segment);
    }

    @Test
    public void shouldAllowCreatingWithNoArguments() {
        configuration = new JcrConfiguration();
    }

    @Test
    public void shouldAllowCreatingWithSpecifiedExecutionContext() {
        ExecutionContext newContext = new ExecutionContext();
        configuration = new JcrConfiguration(newContext);
        assertThat(configuration.getConfigurationDefinition().getContext(), is(sameInstance(newContext)));
    }

    @Test
    public void shouldHaveDefaultConfigurationSourceIfNotSpecified() {
        assertThat(configuration.getConfigurationDefinition(), is(notNullValue()));
    }

    @Test
    public void shouldAllowAddingRepositorySourceInstance() {
        UUID rootUuid = UUID.randomUUID();
        CachePolicy cachePolicy = new ImmutableCachePolicy(100);

        // Update the configuration and save it ...
        configuration.repositorySource("name")
                     .usingClass(InMemoryRepositorySource.class)
                     .setRetryLimit(100)
                     .setProperty("defaultCachePolicy", cachePolicy)
                     .setProperty("defaultWorkspaceName", "default workspace name")
                     .setProperty("rootNodeUuid", rootUuid)
                     .and()
                     .save();

        // Verify that the graph has been updated correctly ...
        ModeShapeConfiguration.ConfigurationDefinition content = configuration.getConfigurationDefinition();
        Subgraph subgraph = content.graph().getSubgraphOfDepth(3).at("/");
        assertThat(subgraph.getNode("/mode:sources"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/name"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/name"), hasProperty(ModeShapeLexicon.RETRY_LIMIT, 100));
        assertThat(subgraph.getNode("/mode:sources/name"), hasProperty("defaultCachePolicy", cachePolicy));
        assertThat(subgraph.getNode("/mode:sources/name"), hasProperty("defaultWorkspaceName", "default workspace name"));
        assertThat(subgraph.getNode("/mode:sources/name"), hasProperty("rootNodeUuid", rootUuid));
    }

    @Test
    public void shouldAllowSettingUpConfigurationRepositoryWithDifferentConfigurationSourceName() throws Exception {
        configuration.repositorySource("Source2")
                     .usingClass(InMemoryRepositorySource.class.getName())
                     .loadedFromClasspath()
                     .setDescription("description")
                     .and()
                     .repository("JCR Repository")
                     .setSource("Source2")
                     .setOption(Option.JAAS_LOGIN_CONFIG_NAME, "test")
                     .and()
                     .save();

        // Start the engine ...
        engine = configuration.build();
        engine.start();
        // Get a graph to the configuration source ...
        RepositorySource configReposSource = engine.getRepositoryService()
                                                   .getRepositoryLibrary()
                                                   .getSource(JcrConfiguration.DEFAULT_CONFIGURATION_SOURCE_NAME);
        assertThat(configReposSource, is(notNullValue()));
        assertThat(configReposSource, is(instanceOf(InMemoryRepositorySource.class)));
        assertThat(configReposSource.getName(), is(JcrConfiguration.DEFAULT_CONFIGURATION_SOURCE_NAME));
        InMemoryRepositorySource configSource = (InMemoryRepositorySource)configReposSource;
        assertThat(configSource.getDefaultWorkspaceName(), is(JcrConfiguration.DEFAULT_WORKSPACE_NAME));
        Graph graph = engine.getGraph(JcrConfiguration.DEFAULT_CONFIGURATION_SOURCE_NAME);
        assertThat(graph, is(notNullValue()));
        assertThat(graph.getNodeAt("/"), is(notNullValue()));
        assertThat(graph.getNodeAt("/mode:sources"), is(notNullValue()));
        assertThat(graph.getNodeAt("/mode:sources/Source2"), hasProperty(ModeShapeLexicon.DESCRIPTION, "description"));
        assertThat(graph.getNodeAt("/mode:repositories/JCR Repository"), hasProperty(ModeShapeLexicon.SOURCE_NAME, "Source2"));

        // Get the repository ...
        JcrRepository repository = engine.getRepository("JCR Repository");
        assertThat(repository, is(notNullValue()));
    }

    @Test
    public void shouldAllowSettingUpConfigurationRepositoryWithDifferentWorkspaceName() throws Exception {
        InMemoryRepositorySource configSource = new InMemoryRepositorySource();
        configSource.setName("config2");
        configSource.setRetryLimit(5);
        configuration.loadFrom(configSource, "workspaceXYZ");
        configuration.repositorySource("Source2")
                     .usingClass(InMemoryRepositorySource.class.getName())
                     .loadedFromClasspath()
                     .setDescription("description")
                     .and()
                     .repository("JCR Repository")
                     .setSource("Source2")
                     .setOption(Option.JAAS_LOGIN_CONFIG_NAME, "test");
        configuration.save();
        // Save the configuration and start the engine ...
        engine = configuration.build();
        engine.start();

        ConfigurationDefinition configDefn = configuration.getConfigurationDefinition();
        assertThat(configDefn.getWorkspace(), is("workspaceXYZ"));
        assertThat(configDefn.getPath(), is(path("/")));

        // Get a graph to the configuration source ...
        RepositorySource configReposSource = engine.getRepositoryService().getRepositoryLibrary().getSource("config2");
        assertThat(configReposSource, is(notNullValue()));
        assertThat(configReposSource, is(instanceOf(InMemoryRepositorySource.class)));
        assertThat(configReposSource.getName(), is("config2"));
        InMemoryRepositorySource configSource2 = (InMemoryRepositorySource)configReposSource;
        assertThat(configSource2.getDefaultWorkspaceName(), is("")); // didn't change this

        Graph graph = engine.getGraph("config2");
        assertThat(graph, is(notNullValue()));
        assertThat(graph.getNodeAt("/"), is(notNullValue()));
        assertThat(graph.getNodeAt("/mode:sources"), is(notNullValue()));
        assertThat(graph.getNodeAt("/mode:sources/Source2"), hasProperty(ModeShapeLexicon.DESCRIPTION, "description"));
        assertThat(graph.getNodeAt("/mode:repositories/JCR Repository"), hasProperty(ModeShapeLexicon.SOURCE_NAME, "Source2"));

        // Get the repository ...
        JcrRepository repository = engine.getRepository("JCR Repository");
        assertThat(repository, is(notNullValue()));
    }

    @Test
    public void shouldAllowSpecifyingOptions() throws Exception {
        configuration.repositorySource("Source2")
                     .usingClass(InMemoryRepositorySource.class.getName())
                     .loadedFromClasspath()
                     .setDescription("description")
                     .and()
                     .repository("JCR Repository")
                     .setSource("Source2")
                     .setOption(Option.JAAS_LOGIN_CONFIG_NAME, "test");

        engine = configuration.build();
        engine.start();

        // Verify that the graph has been updated correctly ...
        Graph config = engine.getGraph(JcrConfiguration.DEFAULT_CONFIGURATION_SOURCE_NAME);
        Subgraph subgraph = config.getSubgraphOfDepth(6).at("/");
        assertThat(subgraph.getNode("/mode:sources"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/Source2"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/Source2"), hasProperty(ModeShapeLexicon.CLASSNAME,
                                                                          InMemoryRepositorySource.class.getName()));
        assertThat(subgraph.getNode("/mode:repositories"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:repositories/JCR Repository"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:repositories/JCR Repository"), hasProperty(ModeShapeLexicon.SOURCE_NAME, "Source2"));
        assertThat(subgraph.getNode("/mode:repositories/JCR Repository/mode:options"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:repositories/JCR Repository/mode:options/JAAS_LOGIN_CONFIG_NAME"),
                   hasProperty(ModeShapeLexicon.VALUE, "test"));

        JcrRepository repository = engine.getRepository("JCR Repository");

        Map<Option, String> options = new HashMap<Option, String>();
        options.put(Option.JAAS_LOGIN_CONFIG_NAME, "test");
        options.put(Option.PROJECT_NODE_TYPES, DefaultOption.PROJECT_NODE_TYPES);
        options.put(Option.READ_DEPTH, DefaultOption.READ_DEPTH);
        options.put(Option.INDEX_READ_DEPTH, DefaultOption.INDEX_READ_DEPTH);
        options.put(Option.ANONYMOUS_USER_ROLES, DefaultOption.ANONYMOUS_USER_ROLES);
        options.put(Option.TABLES_INCLUDE_COLUMNS_FOR_INHERITED_PROPERTIES,
                    DefaultOption.TABLES_INCLUDE_COLUMNS_FOR_INHERITED_PROPERTIES);
        options.put(Option.QUERY_EXECUTION_ENABLED, DefaultOption.QUERY_EXECUTION_ENABLED);
        options.put(Option.QUERY_INDEX_DIRECTORY, DefaultOption.QUERY_INDEX_DIRECTORY);
        options.put(Option.QUERY_INDEXES_UPDATED_SYNCHRONOUSLY, DefaultOption.QUERY_INDEXES_UPDATED_SYNCHRONOUSLY);
        options.put(Option.PERFORM_REFERENTIAL_INTEGRITY_CHECKS, DefaultOption.PERFORM_REFERENTIAL_INTEGRITY_CHECKS);
        options.put(Option.EXPOSE_WORKSPACE_NAMES_IN_DESCRIPTOR, DefaultOption.EXPOSE_WORKSPACE_NAMES_IN_DESCRIPTOR);
        options.put(Option.VERSION_HISTORY_STRUCTURE, DefaultOption.VERSION_HISTORY_STRUCTURE);
        options.put(Option.REPOSITORY_JNDI_LOCATION, DefaultOption.REPOSITORY_JNDI_LOCATION);
        String defaultRemoveDerivedValue = DefaultOption.REMOVE_DERIVED_CONTENT_WITH_ORIGINAL;
        if (engine.getSequencingService().getSequencers().isEmpty()) {
            defaultRemoveDerivedValue = Boolean.FALSE.toString();
        }
        options.put(Option.REMOVE_DERIVED_CONTENT_WITH_ORIGINAL, defaultRemoveDerivedValue);
        assertThat(repository.getOptions(), is(options));
    }

    @Test
    public void shouldLoadConfigurationFromFilePath() throws Exception {
        File file = new File("src/test/resources/config/configRepository.xml");
        assertThat(file.exists(), is(true));
        assertThat(file.canRead(), is(true));
        assertThat(file.isFile(), is(true));

        configuration.loadFrom("src/test/resources/config/configRepository.xml");

        assertThat(configuration.getProblems().isEmpty(), is(true));

        // Verify that the graph has been updated correctly ...
        ModeShapeConfiguration.ConfigurationDefinition content = configuration.getConfigurationDefinition();
        Subgraph subgraph = content.graph().getSubgraphOfDepth(6).at("/");

        assertThat(subgraph.getNode("/mode:sources"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/Cars"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/Cars"), hasProperty(ModeShapeLexicon.RETRY_LIMIT, "3"));
        assertThat(subgraph.getNode("/mode:sources/Cars"), hasProperty(ModeShapeLexicon.CLASSNAME,
                                                                       InMemoryRepositorySource.class.getName()));
        assertThat(subgraph.getNode("/mode:sources/Aircraft"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/Aircraft"), hasProperty("defaultWorkspaceName", "default"));
        assertThat(subgraph.getNode("/mode:sources/Aircraft"), hasProperty(ModeShapeLexicon.CLASSNAME,
                                                                           InMemoryRepositorySource.class.getName()));
        assertThat(subgraph.getNode("/mode:sources/Cache"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/Cache"), hasProperty(ModeShapeLexicon.CLASSNAME,
                                                                        InMemoryRepositorySource.class.getName()));

        assertThat(subgraph.getNode("/mode:mimeTypeDetectors").getChildren(), hasChild(segment("Detector")));
        assertThat(subgraph.getNode("/mode:mimeTypeDetectors/Detector"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:mimeTypeDetectors/Detector"),
                   hasProperty(ModeShapeLexicon.DESCRIPTION, "Standard extension-based MIME type detector"));
        assertThat(subgraph.getNode("/mode:mimeTypeDetectors/Detector"),
                   hasProperty(ModeShapeLexicon.CLASSNAME, ExtensionBasedMimeTypeDetector.class.getName()));

        assertThat(subgraph.getNode("/mode:repositories").getChildren(), hasChild(segment("Car Repository")));
        assertThat(subgraph.getNode("/mode:repositories/Car Repository"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:repositories/Car Repository"), hasProperty(ModeShapeLexicon.SOURCE_NAME, "Cars"));
        assertThat(subgraph.getNode("/mode:repositories/Car Repository").getChildren(), hasChild(segment("mode:options")));
        assertThat(subgraph.getNode("/mode:repositories/Car Repository/mode:options"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:repositories/Car Repository/mode:options").getChildren(),
                   hasChild(segment("jaasLoginConfigName")));
        assertThat(subgraph.getNode("/mode:repositories/Car Repository/mode:options/jaasLoginConfigName"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:repositories/Car Repository/mode:descriptors/query.xpath.doc.order"),
                   is(notNullValue()));
        assertThat(subgraph.getNode("/mode:repositories/Car Repository/mode:descriptors/myDescriptor"), is(notNullValue()));

        // Initialize IDTrust and a policy file (which defines the "modeshape-jcr" login config name)
        String configFile = "security/jaas.conf.xml";
        IDTrustConfiguration idtrustConfig = new IDTrustConfiguration();
        try {
            idtrustConfig.config(configFile);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        // Create and start the engine ...
        engine = configuration.build();
        engine.start();
        Repository repository = engine.getRepository("Car Repository");
        assertThat(repository, is(notNullValue()));
        assertThat(repository.getDescriptor("query.xpath.doc.order"), is("false"));
        assertThat(repository.getDescriptor("myDescriptor"), is("foo"));

        // Create a session, authenticating using one of the usernames defined by our JAAS policy file(s) ...
        Session session = null;
        try {
            session = repository.login(new SimpleCredentials("superuser", "superuser".toCharArray()));
        } finally {
            if (session != null) session.logout();
        }
    }

    @Test
    public void shouldAddNodeTypesAndNamespaces() throws Exception {
        File file = new File("src/test/resources/config/configRepository.xml");
        assertThat(file.exists(), is(true));
        assertThat(file.canRead(), is(true));
        assertThat(file.isFile(), is(true));

        configuration.loadFrom("src/test/resources/config/configRepository.xml");
        // Verify that the configration was loaded correctly ...
        assertThat(configuration.repository("Car Repository").getSource(), is("Cars"));
        // ModeShapeConfiguration.ConfigurationDefinition content1 = configuration.getConfigurationDefinition();
        // Subgraph subgraph1 = content1.graph().getSubgraphOfDepth(6).at("/");

        // Load the node types from the CND file, and save the configuration ...
        InputStream nodeTypes = getClass().getResourceAsStream("/tck/tck_test_types.cnd");
        configuration.repository("Car Repository").addNodeTypes(nodeTypes);
        configuration.save();
        // ModeShapeConfiguration.ConfigurationDefinition content2 = configuration.getConfigurationDefinition();
        // Subgraph subgraph2 = content2.graph().getSubgraphOfDepth(6).at("/");

        // Verify there were no problems loading the CND file ...
        assertThat(configuration.getProblems().isEmpty(), is(true));
        assertThat(configuration.getConfigurationDefinition()
                                .getContext()
                                .getNamespaceRegistry()
                                .isRegisteredNamespaceUri("http://www.modeshape.org/test/1.0"), is(true));

        // Verify that the graph has been updated correctly ...
        ModeShapeConfiguration.ConfigurationDefinition content = configuration.getConfigurationDefinition();
        Subgraph subgraph = content.graph().getSubgraphOfDepth(6).at("/");

        assertThat(subgraph.getNode("/mode:repositories").getChildren(), hasChild(segment("Car Repository")));
        assertThat(subgraph.getNode("/mode:repositories/Car Repository"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:repositories/Car Repository"), hasProperty(ModeShapeLexicon.SOURCE_NAME, "Cars"));
        assertThat(subgraph.getNode("/mode:repositories/Car Repository").getChildren(), hasChild(segment("mode:options")));
        assertThat(subgraph.getNode("/mode:repositories/Car Repository/jcr:nodeTypes"), is(notNullValue()));
        // for (Location child : subgraph.getNode("/mode:repositories/Car Repository/mode:nodeTypes").getChildren()) {
        // System.out.println(child.getPath().getLastSegment().getString(context().getNamespaceRegistry()));
        // }
        assertThat(subgraph.getNode("/mode:repositories/Car Repository/jcr:nodeTypes").getChildren(),
                   hasChildren(segment("modetest:noSameNameSibs"),
                               segment("modetest:referenceableUnstructured"),
                               segment("modetest:nodeWithMandatoryProperty"),
                               segment("modetest:nodeWithMandatoryChild"),
                               segment("modetest:unorderableUnstructured")));
        assertThat(subgraph.getNode("/mode:repositories/Car Repository/mode:namespaces"), is(notNullValue()));

        // Check that the namespace in the CND file was persisted correctly ...
        assertThat(subgraph.getNode("/mode:repositories/Car Repository/mode:namespaces").getChildren(),
                   hasChild(segment("modetest")));
        assertThat(subgraph.getNode("/mode:repositories/Car Repository/mode:namespaces/modetest"),
                   hasProperty(ModeShapeLexicon.URI, "http://www.modeshape.org/test/1.0"));

        // Initialize IDTrust and a policy file (which defines the "modeshape-jcr" login config name)
        String configFile = "security/jaas.conf.xml";
        IDTrustConfiguration idtrustConfig = new IDTrustConfiguration();
        try {
            idtrustConfig.config(configFile);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        // Create and start the engine ...
        engine = configuration.build();
        engine.start();
        Repository repository = engine.getRepository("Car Repository");
        assertThat(repository, is(notNullValue()));

        // Create a session, authenticating using one of the usernames defined by our JAAS policy file(s) ...
        Session session = null;
        try {
            session = repository.login(new SimpleCredentials("superuser", "superuser".toCharArray()));

            // Check that the namespace showed up ...
            assertThat(session.getNamespacePrefix("http://www.modeshape.org/test/1.0"), is("modetest"));

            // Check that some of the node types showed up ...
            NodeTypeManager ntm = session.getWorkspace().getNodeTypeManager();
            assertThat(ntm.getNodeType("modetest:noSameNameSibs"), is(notNullValue())); // throws exception
            assertThat(ntm.getNodeType("modetest:referenceableUnstructured"), is(notNullValue())); // throws exception
            assertThat(ntm.getNodeType("modetest:nodeWithMandatoryProperty"), is(notNullValue())); // throws exception
            assertThat(ntm.getNodeType("modetest:nodeWithMandatoryChild"), is(notNullValue())); // throws exception
            assertThat(ntm.getNodeType("modetest:unorderableUnstructured"), is(notNullValue())); // throws exception
        } finally {
            if (session != null) session.logout();
        }
    }
}
