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
package org.modeshape.repository;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.modeshape.graph.IsNodeWithChildren.hasChild;
import static org.modeshape.graph.IsNodeWithProperty.hasProperty;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Stack;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.connector.path.cache.InMemoryWorkspaceCache;
import org.modeshape.graph.mimetype.ExtensionBasedMimeTypeDetector;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.request.ReadBranchRequest;
import org.modeshape.repository.sequencer.MockStreamSequencerA;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Randall Hauch
 */
public class ModeShapeConfigurationTest {

    private ExecutionContext context;
    private ModeShapeConfiguration configuration;

    @Before
    public void beforeEach() {
        context = new ExecutionContext();
        configuration = new ModeShapeConfiguration();
    }

    protected Path.Segment segment( String segment ) {
        return context.getValueFactories().getPathFactory().createSegment(segment);
    }

    @Test
    public void shouldAllowCreatingWithNoArguments() {
        configuration = new ModeShapeConfiguration();
    }

    @Test
    public void shouldAllowCreatingWithSpecifiedExecutionContext() {
        configuration = new ModeShapeConfiguration(context);
    }

    @Test
    public void shouldHaveDefaultConfigurationSourceIfNotSpecified() {
        assertThat(configuration.getConfigurationDefinition(), is(notNullValue()));
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
        Subgraph subgraph = content.graph().getSubgraphOfDepth(ReadBranchRequest.NO_MAXIMUM_DEPTH).at("/");

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
        assertThat(subgraph.getNode("/mode:sources/Cache"), hasProperty("defaultWorkspaceName", "default"));
        assertThat(subgraph.getNode("/mode:sources/Cache"), hasProperty(ModeShapeLexicon.CLASSNAME,
                                                                        InMemoryRepositorySource.class.getName()));
        assertThat(subgraph.getNode("/mode:sources/Cache/cachePolicy"),
                   hasProperty(ModeShapeLexicon.CLASSNAME, InMemoryWorkspaceCache.InMemoryCachePolicy.class.getName()));

        assertThat(subgraph.getNode("/mode:mimeTypeDetectors").getChildren(), hasChild(segment("Detector")));
        assertThat(subgraph.getNode("/mode:mimeTypeDetectors/Detector"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:mimeTypeDetectors/Detector"),
                   hasProperty(ModeShapeLexicon.DESCRIPTION, "Standard extension-based MIME type detector"));
        assertThat(subgraph.getNode("/mode:mimeTypeDetectors/Detector"),
                   hasProperty(ModeShapeLexicon.CLASSNAME, ExtensionBasedMimeTypeDetector.class.getName()));

        assertThat(subgraph.getNode("/mode:sequencers").getChildren(), hasChild(segment("Image Sequencer")));
        assertThat(subgraph.getNode("/mode:sequencers/Image Sequencer"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sequencers/Image Sequencer"), hasProperty(ModeShapeLexicon.DESCRIPTION,
                                                                                     "Image metadata sequencer"));
        assertThat(subgraph.getNode("/mode:sequencers/Image Sequencer"),
                   hasProperty(ModeShapeLexicon.CLASSNAME, "org.modeshape.sequencer.image.ImageMetadataSequencer"));
        assertThat(subgraph.getNode("/mode:sequencers/Image Sequencer"), hasProperty(ModeShapeLexicon.PATH_EXPRESSION,
                                                                                     "/foo/source => /foo/target",
                                                                                     "/bar/source => /bar/target"));
    }

    @Test
    public void shoulLoadConfigurationFromFileObject() throws Exception {
        File file = new File("src/test/resources/config/configRepository.xml");
        assertThat(file.exists(), is(true));
        assertThat(file.canRead(), is(true));
        assertThat(file.isFile(), is(true));

        configuration.loadFrom(new File("src/test/resources/config/configRepository.xml"));

        assertThat(configuration.getProblems().isEmpty(), is(true));
    }

    @Test
    public void shoulLoadConfigurationFromURL() throws Exception {
        File file = new File("src/test/resources/config/configRepository.xml");
        assertThat(file.exists(), is(true));
        assertThat(file.canRead(), is(true));
        assertThat(file.isFile(), is(true));
        URL fileUrl = file.toURI().toURL();
        assertThat(fileUrl, is(notNullValue()));

        configuration.loadFrom(fileUrl);

        assertThat(configuration.getProblems().isEmpty(), is(true));
    }

    @Test
    public void shoulLoadConfigurationFromInputStream() throws Exception {
        File file = new File("src/test/resources/config/configRepository.xml");
        assertThat(file.exists(), is(true));
        assertThat(file.canRead(), is(true));
        assertThat(file.isFile(), is(true));
        InputStream stream = new FileInputStream(file);
        try {
            configuration.loadFrom(stream);
        } finally {
            stream.close();
        }

        assertThat(configuration.getProblems().isEmpty(), is(true));
    }

    @Test
    public void shoulLoadConfigurationFromRepositorySource() throws Exception {
        InMemoryRepositorySource source = new InMemoryRepositorySource();
        source.setName("name");
        configuration.loadFrom(source);
        assertThat(configuration.getProblems().isEmpty(), is(true));
        ModeShapeEngine engine = configuration.build();
        assertThat(engine, is(notNullValue()));
        assertThat(engine.getProblems().isEmpty(), is(true));
    }

    @Test
    public void shouldLoadConfigurationEvenAfterAlreadyHavingLoadedConfiguration() throws Exception {
        configuration.loadFrom("src/test/resources/config/configRepository.xml");
        configuration.loadFrom(new File("src/test/resources/config/configRepository.xml"));
        configuration.loadFrom(new File("src/test/resources/config/configRepository.xml").toURI().toURL());
        assertThat(configuration.getProblems().isEmpty(), is(true));
    }

    @Test
    public void shouldBuildEngineWithDefaultConfiguration() throws Exception {
        assertThat(configuration.getProblems().isEmpty(), is(true));
        ModeShapeEngine engine = configuration.build();
        assertThat(engine, is(notNullValue()));
        assertThat(engine.getProblems().isEmpty(), is(true));
    }

    @Test
    public void shouldLoadConfigurationFromInMemoryRepositorySource() {
        InMemoryRepositorySource configSource = new InMemoryRepositorySource();
        configSource.setName("config repo");
        configuration.loadFrom(configSource).and().save();
        assertThat(configuration, is(notNullValue()));
        ModeShapeConfiguration.ConfigurationDefinition content = configuration.getConfigurationDefinition();
        assertThat(content.getRepositorySource(), is(instanceOf(InMemoryRepositorySource.class)));
        InMemoryRepositorySource source = (InMemoryRepositorySource)content.getRepositorySource();
        assertThat(source.getName(), is(configSource.getName()));
        assertThat(source.getRetryLimit(), is(configSource.getRetryLimit()));
    }

    @Test
    public void shouldAllowAddingRepositorySourceByClassNameAndSettingProperties() {
        // Update the configuration and save it ...
        configuration.repositorySource("Source1")
                     .usingClass(InMemoryRepositorySource.class.getName())
                     .loadedFromClasspath()
                     .setDescription("description")
                     .setRetryLimit(5)
                     .and()
                     .save();

        // Verify that the graph has been updated correctly ...
        ModeShapeConfiguration.ConfigurationDefinition content = configuration.getConfigurationDefinition();
        Subgraph subgraph = content.graph().getSubgraphOfDepth(3).at("/");
        assertThat(subgraph.getNode("/mode:sources"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/Source1"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/Source1"), hasProperty(ModeShapeLexicon.DESCRIPTION, "description"));
        assertThat(subgraph.getNode("/mode:sources/Source1"), hasProperty(ModeShapeLexicon.RETRY_LIMIT, 5));
        assertThat(subgraph.getNode("/mode:sources/Source1"), hasProperty(ModeShapeLexicon.CLASSNAME,
                                                                          InMemoryRepositorySource.class.getName()));
    }

    @Test
    public void shouldAllowSettingDescriptionOnRepositorySourceUsingPrefixedPropertyName() {
        // Update the configuration and save it ...
        configuration.repositorySource("Source1")
                     .usingClass(InMemoryRepositorySource.class.getName())
                     .loadedFromClasspath()
                     .setProperty("mode:description", "desc")
                     .and()
                     .save();

        // Verify that the graph has been updated correctly ...
        ModeShapeConfiguration.ConfigurationDefinition content = configuration.getConfigurationDefinition();
        Subgraph subgraph = content.graph().getSubgraphOfDepth(3).at("/");
        assertThat(subgraph.getNode("/mode:sources"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/Source1"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/Source1"), hasProperty(ModeShapeLexicon.DESCRIPTION, "desc"));
    }

    @Test
    public void shouldAllowSettingDescriptionOnRepositorySourceUsingNonPrefixedPropertyName() {
        // Update the configuration and save it ...
        configuration.repositorySource("Source1")
                     .usingClass(InMemoryRepositorySource.class.getName())
                     .loadedFromClasspath()
                     .setProperty("description", "desc")
                     .and()
                     .save();

        // Verify that the graph has been updated correctly ...
        ModeShapeConfiguration.ConfigurationDefinition content = configuration.getConfigurationDefinition();
        Subgraph subgraph = content.graph().getSubgraphOfDepth(3).at("/");
        assertThat(subgraph.getNode("/mode:sources"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/Source1"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/Source1"), hasProperty(ModeShapeLexicon.DESCRIPTION, "desc"));
    }

    @Test
    public void shouldAllowSettingRetryLimitOnRepositorySourceUsingPrefixedPropertyName() {
        // Update the configuration and save it ...
        configuration.repositorySource("Source1")
                     .usingClass(InMemoryRepositorySource.class.getName())
                     .loadedFromClasspath()
                     .setProperty("mode:retryLimit", 5)
                     .and()
                     .save();

        // Verify that the graph has been updated correctly ...
        ModeShapeConfiguration.ConfigurationDefinition content = configuration.getConfigurationDefinition();
        Subgraph subgraph = content.graph().getSubgraphOfDepth(3).at("/");
        assertThat(subgraph.getNode("/mode:sources"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/Source1"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/Source1"), hasProperty(ModeShapeLexicon.RETRY_LIMIT, 5));
    }

    @Test
    public void shouldAllowSettingRetryLimitOnRepositorySourceUsingNonPrefixedPropertyName() {
        // Update the configuration and save it ...
        configuration.repositorySource("Source1")
                     .usingClass(InMemoryRepositorySource.class.getName())
                     .loadedFromClasspath()
                     .setProperty("retryLimit", 5)
                     .and()
                     .save();

        // Verify that the graph has been updated correctly ...
        ModeShapeConfiguration.ConfigurationDefinition content = configuration.getConfigurationDefinition();
        Subgraph subgraph = content.graph().getSubgraphOfDepth(3).at("/");
        assertThat(subgraph.getNode("/mode:sources"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/Source1"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/Source1"), hasProperty(ModeShapeLexicon.RETRY_LIMIT, 5));
    }

    @Test
    public void shouldAllowAddingRepositorySourceByClassNameAndClasspathAndSettingProperties() {
        // Update the configuration and save it ...
        configuration.repositorySource("Source1")
                     .usingClass(InMemoryRepositorySource.class.getName())
                     .loadedFrom("cp1", "cp2")
                     .setProperty("retryLimit", 5)
                     .and()
                     .save();

        // Verify that the graph has been updated correctly ...
        ModeShapeConfiguration.ConfigurationDefinition content = configuration.getConfigurationDefinition();
        Subgraph subgraph = content.graph().getSubgraphOfDepth(3).at("/");
        assertThat(subgraph.getNode("/mode:sources"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/Source1"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/Source1"), hasProperty(ModeShapeLexicon.RETRY_LIMIT, 5));
        assertThat(subgraph.getNode("/mode:sources/Source1"), hasProperty(ModeShapeLexicon.CLASSNAME,
                                                                          InMemoryRepositorySource.class.getName()));
        assertThat(subgraph.getNode("/mode:sources/Source1"), hasProperty(ModeShapeLexicon.CLASSPATH, "cp1", "cp2"));
    }

    @Test
    public void shouldAllowAddingRepositorySourceByClassReferenceAndSettingProperties() {
        // Update the configuration and save it ...
        configuration.repositorySource("Source1")
                     .usingClass(InMemoryRepositorySource.class)
                     .setProperty("retryLimit", 5)
                     .and()
                     .save();

        // Verify that the graph has been updated correctly ...
        ModeShapeConfiguration.ConfigurationDefinition content = configuration.getConfigurationDefinition();
        Subgraph subgraph = content.graph().getSubgraphOfDepth(3).at("/");
        assertThat(subgraph.getNode("/mode:sources"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/Source1"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/Source1"), hasProperty(ModeShapeLexicon.RETRY_LIMIT, 5));
        assertThat(subgraph.getNode("/mode:sources/Source1"), hasProperty(ModeShapeLexicon.CLASSNAME,
                                                                          InMemoryRepositorySource.class.getName()));
    }

    @Test
    public void shouldAllowAddingMimeTypeDetector() {
        // Update the configuration and save it ...
        configuration.mimeTypeDetector("detector")
                     .usingClass(ExtensionBasedMimeTypeDetector.class)
                     .setDescription("default detector")
                     .and()
                     .save();

        // Verify that the graph has been updated correctly ...
        ModeShapeConfiguration.ConfigurationDefinition content = configuration.getConfigurationDefinition();
        Subgraph subgraph = content.graph().getSubgraphOfDepth(3).at("/");
        assertThat(subgraph.getNode("/mode:mimeTypeDetectors").getChildren(), hasChild(segment("detector")));
        assertThat(subgraph.getNode("/mode:mimeTypeDetectors/detector"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:mimeTypeDetectors/detector"), hasProperty(ModeShapeLexicon.DESCRIPTION,
                                                                                     "default detector"));
        assertThat(subgraph.getNode("/mode:mimeTypeDetectors/detector"),
                   hasProperty(ModeShapeLexicon.CLASSNAME, ExtensionBasedMimeTypeDetector.class.getName()));
    }

    @Test
    public void shouldAllowAddingSequencer() {
        // Update the configuration and save it ...
        configuration.sequencer("sequencerA")
                     .usingClass(MockStreamSequencerA.class)
                     .setDescription("Mock Sequencer A")
                     .sequencingFrom("/foo/source")
                     .andOutputtingTo("/foo/target")
                     .sequencingFrom("/bar/source")
                     .andOutputtingTo("/bar/target")
                     .and()
                     .save();

        // Verify that the graph has been updated correctly ...
        ModeShapeConfiguration.ConfigurationDefinition content = configuration.getConfigurationDefinition();
        Subgraph subgraph = content.graph().getSubgraphOfDepth(3).at("/");
        assertThat(subgraph.getNode("/mode:sequencers").getChildren(), hasChild(segment("sequencerA")));
        assertThat(subgraph.getNode("/mode:sequencers/sequencerA"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sequencers/sequencerA"), hasProperty(ModeShapeLexicon.DESCRIPTION, "Mock Sequencer A"));
        assertThat(subgraph.getNode("/mode:sequencers/sequencerA"), hasProperty(ModeShapeLexicon.CLASSNAME,
                                                                                MockStreamSequencerA.class.getName()));
        System.out.println(subgraph.getNode("/mode:sequencers/sequencerA").getProperty(ModeShapeLexicon.PATH_EXPRESSION));
        assertThat(subgraph.getNode("/mode:sequencers/sequencerA"), hasProperty(ModeShapeLexicon.PATH_EXPRESSION,
                                                                                "/foo/source => /foo/target",
                                                                                "/bar/source => /bar/target"));
    }

    @Test
    public void shouldAllowConfigurationInMultipleSteps() {
        // Update the configuration and save it ...
        configuration.repositorySource("Source1")
                     .usingClass(InMemoryRepositorySource.class.getName())
                     .loadedFrom("cp1", "cp2")
                     .setProperty("retryLimit", 5);
        configuration.mimeTypeDetector("detector")
                     .usingClass(ExtensionBasedMimeTypeDetector.class)
                     .setDescription("default detector");
        configuration.sequencer("sequencerA")
                     .usingClass(MockStreamSequencerA.class)
                     .setDescription("Mock Sequencer A")
                     .sequencingFrom("/foo/source")
                     .andOutputtingTo("/foo/target")
                     .sequencingFrom("/bar/source")
                     .andOutputtingTo("/bar/target");
        configuration.save();

        // Verify that the graph has been updated correctly ...
        ModeShapeConfiguration.ConfigurationDefinition content = configuration.getConfigurationDefinition();
        Subgraph subgraph = content.graph().getSubgraphOfDepth(3).at("/");

        assertThat(subgraph.getNode("/mode:sources"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/Source1"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/Source1"), hasProperty(ModeShapeLexicon.RETRY_LIMIT, 5));
        assertThat(subgraph.getNode("/mode:sources/Source1"), hasProperty(ModeShapeLexicon.CLASSNAME,
                                                                          InMemoryRepositorySource.class.getName()));
        assertThat(subgraph.getNode("/mode:sources/Source1"), hasProperty(ModeShapeLexicon.CLASSPATH, "cp1", "cp2"));

        assertThat(subgraph.getNode("/mode:mimeTypeDetectors").getChildren(), hasChild(segment("detector")));
        assertThat(subgraph.getNode("/mode:mimeTypeDetectors/detector"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:mimeTypeDetectors/detector"), hasProperty(ModeShapeLexicon.DESCRIPTION,
                                                                                     "default detector"));
        assertThat(subgraph.getNode("/mode:mimeTypeDetectors/detector"),
                   hasProperty(ModeShapeLexicon.CLASSNAME, ExtensionBasedMimeTypeDetector.class.getName()));

        assertThat(subgraph.getNode("/mode:sequencers").getChildren(), hasChild(segment("sequencerA")));
        assertThat(subgraph.getNode("/mode:sequencers/sequencerA"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sequencers/sequencerA"), hasProperty(ModeShapeLexicon.DESCRIPTION, "Mock Sequencer A"));
        assertThat(subgraph.getNode("/mode:sequencers/sequencerA"), hasProperty(ModeShapeLexicon.CLASSNAME,
                                                                                MockStreamSequencerA.class.getName()));
        assertThat(subgraph.getNode("/mode:sequencers/sequencerA"), hasProperty(ModeShapeLexicon.PATH_EXPRESSION,
                                                                                "/foo/source => /foo/target",
                                                                                "/bar/source => /bar/target"));
    }

    @Test
    public void shouldSaveSimpleConfigurationToContentHandler() throws Exception {
        // Update the configuration and save it ...
        configuration.repositorySource("Source1")
                     .usingClass(InMemoryRepositorySource.class.getName())
                     .loadedFromClasspath()
                     .setProperty("retryLimit", 5)
                     .and()
                     .save();

        GraphHandler handler = new GraphHandler();

        configuration.storeTo(handler);

        Subgraph subgraph = handler.graph().getSubgraphOfDepth(3).at("/");
        assertThat(subgraph.getNode("/mode:sources"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/Source1"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/Source1"), hasProperty(ModeShapeLexicon.RETRY_LIMIT, "5"));
        assertThat(subgraph.getNode("/mode:sources/Source1"), hasProperty(ModeShapeLexicon.CLASSNAME,
                                                                          InMemoryRepositorySource.class.getName()));

    }

    @Test
    public void shouldSaveComplexConfigurationToContentHandler() throws Exception {
        // Update the configuration and save it ...
        configuration.repositorySource("Source1")
                     .usingClass(InMemoryRepositorySource.class.getName())
                     .loadedFrom("cp1", "cp2")
                     .setProperty("retryLimit", 5);
        configuration.mimeTypeDetector("detector")
                     .usingClass(ExtensionBasedMimeTypeDetector.class)
                     .setDescription("default detector");
        configuration.sequencer("sequencerA")
                     .usingClass(MockStreamSequencerA.class)
                     .setDescription("Mock Sequencer A")
                     .sequencingFrom("/foo/source")
                     .andOutputtingTo("/foo/target")
                     .sequencingFrom("/bar/source")
                     .andOutputtingTo("/bar/target");
        configuration.save();

        GraphHandler handler = new GraphHandler();

        configuration.storeTo(handler);

        Subgraph subgraph = handler.graph().getSubgraphOfDepth(3).at("/");
        assertThat(subgraph.getNode("/mode:sources"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/Source1"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sources/Source1"), hasProperty(ModeShapeLexicon.RETRY_LIMIT, "5"));
        assertThat(subgraph.getNode("/mode:sources/Source1"), hasProperty(ModeShapeLexicon.CLASSNAME,
                                                                          InMemoryRepositorySource.class.getName()));
        assertThat(subgraph.getNode("/mode:sources/Source1"), hasProperty(ModeShapeLexicon.CLASSPATH, "cp1", "cp2"));

        assertThat(subgraph.getNode("/mode:mimeTypeDetectors").getChildren(), hasChild(segment("detector")));
        assertThat(subgraph.getNode("/mode:mimeTypeDetectors/detector"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:mimeTypeDetectors/detector"), hasProperty(ModeShapeLexicon.DESCRIPTION,
                                                                                     "default detector"));
        assertThat(subgraph.getNode("/mode:mimeTypeDetectors/detector"),
                   hasProperty(ModeShapeLexicon.CLASSNAME, ExtensionBasedMimeTypeDetector.class.getName()));

        assertThat(subgraph.getNode("/mode:sequencers").getChildren(), hasChild(segment("sequencerA")));
        assertThat(subgraph.getNode("/mode:sequencers/sequencerA"), is(notNullValue()));
        assertThat(subgraph.getNode("/mode:sequencers/sequencerA"), hasProperty(ModeShapeLexicon.DESCRIPTION, "Mock Sequencer A"));
        assertThat(subgraph.getNode("/mode:sequencers/sequencerA"), hasProperty(ModeShapeLexicon.CLASSNAME,
                                                                                MockStreamSequencerA.class.getName()));

        assertThat(subgraph.getNode("/mode:sequencers/sequencerA"), hasProperty(ModeShapeLexicon.PATH_EXPRESSION,
                                                                                "/foo/source => /foo/target",
                                                                                "/bar/source => /bar/target"));

    }

    public class GraphHandler extends DefaultHandler {

        private PathFactory pathFactory;
        private NameFactory nameFactory;
        private PropertyFactory propFactory;
        private Graph graph;
        private InMemoryRepositorySource source;
        private Graph.Batch batch;
        private Stack<Path> parents;

        @SuppressWarnings( "synthetic-access" )
        public GraphHandler() {
            this.pathFactory = context.getValueFactories().getPathFactory();
            this.nameFactory = context.getValueFactories().getNameFactory();
            this.propFactory = context.getPropertyFactory();

            this.parents = new Stack<Path>();

            String name = "Handler Source";
            source = new InMemoryRepositorySource();
            source.setName(name);

            this.graph = Graph.create(source, context);
        }

        public Graph graph() {
            return graph;
        }

        private Path path( Path root,
                           String child ) {
            return pathFactory.create(root, child);
        }

        private Name name( String name ) {
            return nameFactory.create(name);
        }

        @Override
        public void startDocument() {
            batch = graph.batch();
        }

        @Override
        public void endDocument() {
            batch.execute();
        }

        @Override
        public void startElement( String uri,
                                  String localName,
                                  String qName,
                                  Attributes attributes ) {
            Path nodePath;

            if (parents.isEmpty()) {
                nodePath = pathFactory.createRootPath();
            } else {
                nodePath = path(parents.peek(), qName);
                batch.create(nodePath).and();
            }
            parents.push(nodePath);

            for (int i = 0; i < attributes.getLength(); i++) {
                String rawValue = attributes.getValue(i);
                String[] values = rawValue.split(",");

                Property prop = propFactory.create(name(attributes.getQName(i)), (Object[])values);

                batch.set(prop).on(nodePath).and();
            }

        }

        @Override
        public void endElement( String uri,
                                String localName,
                                String qName ) {
            parents.pop();
        }
    }
}
