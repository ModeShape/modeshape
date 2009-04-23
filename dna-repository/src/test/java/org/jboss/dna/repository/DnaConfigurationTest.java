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
package org.jboss.dna.repository;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.jboss.dna.graph.IsNodeWithChildren.hasChild;
import static org.jboss.dna.graph.IsNodeWithProperty.hasProperty;
import static org.junit.Assert.assertThat;
import java.util.UUID;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Subgraph;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.cache.ImmutableCachePolicy;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.mimetype.ExtensionBasedMimeTypeDetector;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.repository.sequencer.MockSequencerA;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class DnaConfigurationTest {

    private ExecutionContext context;
    private DnaConfiguration configuration;

    @Before
    public void beforeEach() {
        context = new ExecutionContext();
        configuration = new DnaConfiguration();
    }

    protected Path.Segment segment( String segment ) {
        return context.getValueFactories().getPathFactory().createSegment(segment);
    }

    @Test
    public void shouldAllowCreatingWithNoArguments() {
        configuration = new DnaConfiguration();
    }

    @Test
    public void shouldAllowCreatingWithSpecifiedExecutionContext() {
        configuration = new DnaConfiguration(context);
    }

    @Test
    public void shouldHaveDefaultConfigurationSourceIfNotSpecified() {
        assertThat(configuration.graph(), is(notNullValue()));
    }

    @Test
    public void shouldAllowSpecifyingConfigurationRepository() {
        DnaConfiguration config = configuration.withConfigurationRepository()
                                               .usingClass("org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource")
                                               .loadedFromClasspath()
                                               .describedAs("description")
                                               .with("retryLimit")
                                               .setTo(5)
                                               .with("name")
                                               .setTo("repository name")
                                               .and()
                                               .save();
        assertThat(config, is(notNullValue()));
        assertThat(config.configurationSource.getRepositorySource(), is(instanceOf(InMemoryRepositorySource.class)));
        InMemoryRepositorySource source = (InMemoryRepositorySource)config.configurationSource.getRepositorySource();
        assertThat(source.getName(), is("repository name"));
        assertThat(source.getRetryLimit(), is(5));
    }

    @Test
    public void shouldAllowAddingRepositorySourceInstance() {
        UUID rootUuid = UUID.randomUUID();
        CachePolicy cachePolicy = new ImmutableCachePolicy(100);
        InMemoryRepositorySource newSource = new InMemoryRepositorySource();
        newSource.setName("name");
        newSource.setDefaultCachePolicy(cachePolicy);
        newSource.setDefaultWorkspaceName("default workspace name");
        newSource.setRetryLimit(100);
        newSource.setRootNodeUuid(rootUuid);

        // Update the configuration and save it ...
        configuration.addRepository(newSource).save();

        // Verify that the graph has been updated correctly ...
        Subgraph subgraph = configuration.graph().getSubgraphOfDepth(3).at("/");
        assertThat(subgraph.getNode("/dna:sources"), is(notNullValue()));
        assertThat(subgraph.getNode("/dna:sources/name"), is(notNullValue()));
        assertThat(subgraph.getNode("/dna:sources/name"), hasProperty(DnaLexicon.READABLE_NAME, "name"));
        assertThat(subgraph.getNode("/dna:sources/name"), hasProperty(DnaLexicon.RETRY_LIMIT, 100));
        assertThat(subgraph.getNode("/dna:sources/name"), hasProperty(DnaLexicon.DEFAULT_CACHE_POLICY, cachePolicy));
        assertThat(subgraph.getNode("/dna:sources/name"), hasProperty("defaultWorkspaceName", "default workspace name"));
        assertThat(subgraph.getNode("/dna:sources/name"), hasProperty("rootNodeUuid", rootUuid));
    }

    @Test
    public void shouldAllowAddingRepositorySourceByClassNameAndSettingProperties() {
        // Update the configuration and save it ...
        configuration.addRepository("Source1")
                     .usingClass(InMemoryRepositorySource.class.getName())
                     .loadedFromClasspath()
                     .describedAs("description")
                     .with("retryLimit")
                     .setTo(5)
                     .and()
                     .save();

        // Verify that the graph has been updated correctly ...
        Subgraph subgraph = configuration.graph().getSubgraphOfDepth(3).at("/");
        assertThat(subgraph.getNode("/dna:sources"), is(notNullValue()));
        assertThat(subgraph.getNode("/dna:sources/Source1"), is(notNullValue()));
        assertThat(subgraph.getNode("/dna:sources/Source1"), hasProperty(DnaLexicon.READABLE_NAME, "Source1"));
        assertThat(subgraph.getNode("/dna:sources/Source1"), hasProperty(DnaLexicon.RETRY_LIMIT, 5));
        assertThat(subgraph.getNode("/dna:sources/Source1"), hasProperty(DnaLexicon.CLASSNAME,
                                                                         InMemoryRepositorySource.class.getName()));
    }

    @Test
    public void shouldAllowAddingRepositorySourceByClassNameAndClasspathAndSettingProperties() {
        // Update the configuration and save it ...
        configuration.addRepository("Source1")
                     .usingClass(InMemoryRepositorySource.class.getName())
                     .loadedFrom("cp1", "cp2")
                     .describedAs("description")
                     .with("retryLimit")
                     .setTo(5)
                     .and()
                     .save();

        // Verify that the graph has been updated correctly ...
        Subgraph subgraph = configuration.graph().getSubgraphOfDepth(3).at("/");
        assertThat(subgraph.getNode("/dna:sources"), is(notNullValue()));
        assertThat(subgraph.getNode("/dna:sources/Source1"), is(notNullValue()));
        assertThat(subgraph.getNode("/dna:sources/Source1"), hasProperty(DnaLexicon.READABLE_NAME, "Source1"));
        assertThat(subgraph.getNode("/dna:sources/Source1"), hasProperty(DnaLexicon.RETRY_LIMIT, 5));
        assertThat(subgraph.getNode("/dna:sources/Source1"), hasProperty(DnaLexicon.CLASSNAME,
                                                                         InMemoryRepositorySource.class.getName()));
        assertThat(subgraph.getNode("/dna:sources/Source1"), hasProperty(DnaLexicon.CLASSPATH, "cp1", "cp2"));
    }

    @Test
    public void shouldAllowAddingRepositorySourceByClassReferenceAndSettingProperties() {
        // Update the configuration and save it ...
        configuration.addRepository("Source1")
                     .usingClass(InMemoryRepositorySource.class)
                     .describedAs("description")
                     .with("retryLimit")
                     .setTo(5)
                     .and()
                     .save();

        // Verify that the graph has been updated correctly ...
        Subgraph subgraph = configuration.graph().getSubgraphOfDepth(3).at("/");
        assertThat(subgraph.getNode("/dna:sources"), is(notNullValue()));
        assertThat(subgraph.getNode("/dna:sources/Source1"), is(notNullValue()));
        assertThat(subgraph.getNode("/dna:sources/Source1"), hasProperty(DnaLexicon.READABLE_NAME, "Source1"));
        assertThat(subgraph.getNode("/dna:sources/Source1"), hasProperty(DnaLexicon.RETRY_LIMIT, 5));
        assertThat(subgraph.getNode("/dna:sources/Source1"), hasProperty(DnaLexicon.DESCRIPTION, "description"));
        assertThat(subgraph.getNode("/dna:sources/Source1"), hasProperty(DnaLexicon.CLASSNAME,
                                                                         InMemoryRepositorySource.class.getName()));
    }

    @Test
    public void shouldAllowOverwritingRepositorySourceByRepositoryName() {
        // Update the configuration and save it ...
        configuration.addRepository("Source1")
                     .usingClass(InMemoryRepositorySource.class)
                     .describedAs("description")
                     .with("retryLimit")
                     .setTo(3)
                     .and()
                     .addRepository("Source1")
                     .usingClass(InMemoryRepositorySource.class)
                     .describedAs("new description")
                     .with("retryLimit")
                     .setTo(6)
                     .and()
                     .save();

        // Verify that the graph has been updated correctly ...
        Subgraph subgraph = configuration.graph().getSubgraphOfDepth(3).at("/");
        assertThat(subgraph.getNode("/dna:sources"), is(notNullValue()));
        assertThat(subgraph.getNode("/dna:sources").getChildren(), hasChild(segment("Source1")));
        assertThat(subgraph.getNode("/dna:sources/Source1"), is(notNullValue()));
        assertThat(subgraph.getNode("/dna:sources/Source1"), hasProperty(DnaLexicon.READABLE_NAME, "Source1"));
        assertThat(subgraph.getNode("/dna:sources/Source1"), hasProperty(DnaLexicon.RETRY_LIMIT, 6));
        assertThat(subgraph.getNode("/dna:sources/Source1"), hasProperty(DnaLexicon.DESCRIPTION, "new description"));
        assertThat(subgraph.getNode("/dna:sources/Source1"), hasProperty(DnaLexicon.CLASSNAME,
                                                                         InMemoryRepositorySource.class.getName()));
    }

    @Test
    public void shouldAllowAddingMimeTypeDetector() {
        // Update the configuration and save it ...
        configuration.addRepository("Source1")
                     .usingClass(InMemoryRepositorySource.class)
                     .describedAs("description")
                     .and()
                     .addMimeTypeDetector("detector")
                     .usingClass(ExtensionBasedMimeTypeDetector.class)
                     .describedAs("default detector")
                     .and()
                     .save();

        // Verify that the graph has been updated correctly ...
        Subgraph subgraph = configuration.graph().getSubgraphOfDepth(3).at("/");
        assertThat(subgraph.getNode("/dna:sources").getChildren(), hasChild(segment("Source1")));
        assertThat(subgraph.getNode("/dna:sources/Source1"), is(notNullValue()));
        assertThat(subgraph.getNode("/dna:sources/Source1"), hasProperty(DnaLexicon.READABLE_NAME, "Source1"));
        assertThat(subgraph.getNode("/dna:sources/Source1"), hasProperty(DnaLexicon.DESCRIPTION, "description"));
        assertThat(subgraph.getNode("/dna:sources/Source1"), hasProperty(DnaLexicon.CLASSNAME,
                                                                         InMemoryRepositorySource.class.getName()));
        assertThat(subgraph.getNode("/dna:mimeTypeDetectors").getChildren(), hasChild(segment("detector")));
        assertThat(subgraph.getNode("/dna:mimeTypeDetectors/detector"), is(notNullValue()));
        assertThat(subgraph.getNode("/dna:mimeTypeDetectors/detector"), hasProperty(DnaLexicon.READABLE_NAME, "detector"));
        assertThat(subgraph.getNode("/dna:mimeTypeDetectors/detector"), hasProperty(DnaLexicon.DESCRIPTION, "default detector"));
        assertThat(subgraph.getNode("/dna:mimeTypeDetectors/detector"),
                   hasProperty(DnaLexicon.CLASSNAME, ExtensionBasedMimeTypeDetector.class.getName()));
    }

    @Test
    public void shouldAllowAddingSequencer() {
        // Update the configuration and save it ...
        configuration.addRepository("Source1")
                     .usingClass(InMemoryRepositorySource.class)
                     .describedAs("description")
                     .named("A Source")
                     .and()
                     .addSequencer("sequencerA")
                     .usingClass(MockSequencerA.class)
                     .named("The (Main) Sequencer")
                     .describedAs("Mock Sequencer A")
                     .sequencingFrom("/foo/source")
                     .andOutputtingTo("/foo/target")
                     .sequencingFrom("/bar/source")
                     .andOutputtingTo("/bar/target")
                     .and()
                     .save();

        // Verify that the graph has been updated correctly ...
        Subgraph subgraph = configuration.graph().getSubgraphOfDepth(3).at("/");
        assertThat(subgraph.getNode("/dna:sources").getChildren(), hasChild(segment("Source1")));
        assertThat(subgraph.getNode("/dna:sources/Source1"), is(notNullValue()));
        assertThat(subgraph.getNode("/dna:sources/Source1"), hasProperty(DnaLexicon.READABLE_NAME, "A Source"));
        assertThat(subgraph.getNode("/dna:sources/Source1"), hasProperty(DnaLexicon.DESCRIPTION, "description"));
        assertThat(subgraph.getNode("/dna:sources/Source1"), hasProperty(DnaLexicon.CLASSNAME,
                                                                         InMemoryRepositorySource.class.getName()));
        assertThat(subgraph.getNode("/dna:sequencers").getChildren(), hasChild(segment("sequencerA")));
        assertThat(subgraph.getNode("/dna:sequencers/sequencerA"), is(notNullValue()));
        assertThat(subgraph.getNode("/dna:sequencers/sequencerA"), hasProperty(DnaLexicon.READABLE_NAME, "The (Main) Sequencer"));
        assertThat(subgraph.getNode("/dna:sequencers/sequencerA"), hasProperty(DnaLexicon.DESCRIPTION, "Mock Sequencer A"));
        assertThat(subgraph.getNode("/dna:sequencers/sequencerA"), hasProperty(DnaLexicon.CLASSNAME,
                                                                               MockSequencerA.class.getName()));
        System.out.println(subgraph.getNode("/dna:sequencers/sequencerA").getProperty(DnaLexicon.PATH_EXPRESSIONS));
        assertThat(subgraph.getNode("/dna:sequencers/sequencerA"), hasProperty(DnaLexicon.PATH_EXPRESSIONS,
                                                                               "/foo/source => /foo/target",
                                                                               "/bar/source => /bar/target"));
    }

    @Test
    public void shouldAllowConfigurationInMultipleSteps() {
        configuration.addRepository("Source1").usingClass(InMemoryRepositorySource.class).describedAs("description");
        configuration.addMimeTypeDetector("detector")
                     .usingClass(ExtensionBasedMimeTypeDetector.class)
                     .describedAs("default detector");
        configuration.addSequencer("sequencerA")
                     .usingClass(MockSequencerA.class)
                     .named("The (Main) Sequencer")
                     .describedAs("Mock Sequencer A")
                     .sequencingFrom("/foo/source")
                     .andOutputtingTo("/foo/target")
                     .sequencingFrom("/bar/source")
                     .andOutputtingTo("/bar/target");
        configuration.save();

        // Verify that the graph has been updated correctly ...
        Subgraph subgraph = configuration.graph().getSubgraphOfDepth(3).at("/");
        assertThat(subgraph.getNode("/dna:sources").getChildren(), hasChild(segment("Source1")));
        assertThat(subgraph.getNode("/dna:sources/Source1"), is(notNullValue()));
        assertThat(subgraph.getNode("/dna:sources/Source1"), hasProperty(DnaLexicon.READABLE_NAME, "Source1"));
        assertThat(subgraph.getNode("/dna:sources/Source1"), hasProperty(DnaLexicon.DESCRIPTION, "description"));
        assertThat(subgraph.getNode("/dna:sources/Source1"), hasProperty(DnaLexicon.CLASSNAME,
                                                                         InMemoryRepositorySource.class.getName()));
        assertThat(subgraph.getNode("/dna:mimeTypeDetectors").getChildren(), hasChild(segment("detector")));
        assertThat(subgraph.getNode("/dna:mimeTypeDetectors/detector"), is(notNullValue()));
        assertThat(subgraph.getNode("/dna:mimeTypeDetectors/detector"), hasProperty(DnaLexicon.READABLE_NAME, "detector"));
        assertThat(subgraph.getNode("/dna:mimeTypeDetectors/detector"), hasProperty(DnaLexicon.DESCRIPTION, "default detector"));
        assertThat(subgraph.getNode("/dna:mimeTypeDetectors/detector"),
                   hasProperty(DnaLexicon.CLASSNAME, ExtensionBasedMimeTypeDetector.class.getName()));
        assertThat(subgraph.getNode("/dna:sequencers").getChildren(), hasChild(segment("sequencerA")));
        assertThat(subgraph.getNode("/dna:sequencers/sequencerA"), is(notNullValue()));
        assertThat(subgraph.getNode("/dna:sequencers/sequencerA"), hasProperty(DnaLexicon.READABLE_NAME, "The (Main) Sequencer"));
        assertThat(subgraph.getNode("/dna:sequencers/sequencerA"), hasProperty(DnaLexicon.DESCRIPTION, "Mock Sequencer A"));
        assertThat(subgraph.getNode("/dna:sequencers/sequencerA"), hasProperty(DnaLexicon.CLASSNAME,
                                                                               MockSequencerA.class.getName()));
        System.out.println(subgraph.getNode("/dna:sequencers/sequencerA").getProperty(DnaLexicon.PATH_EXPRESSIONS));
        assertThat(subgraph.getNode("/dna:sequencers/sequencerA"), hasProperty(DnaLexicon.PATH_EXPRESSIONS,
                                                                               "/foo/source => /foo/target",
                                                                               "/bar/source => /bar/target"));
    }
}
