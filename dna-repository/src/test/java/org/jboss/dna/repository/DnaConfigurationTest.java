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
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.mimetype.MimeTypeDetector;
import org.jboss.dna.repository.DnaConfiguration.DnaSequencerDetails;
import org.jboss.dna.repository.sequencer.MockSequencerA;
import org.jboss.dna.repository.sequencer.MockSequencerB;
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

    @Test
    public void shouldAllowCreatingWithNoArguments() {
        configuration = new DnaConfiguration();
    }

    @Test
    public void shouldAllowCreatingWithSpecifiedExecutionContext() {
        configuration = new DnaConfiguration(context);
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
                                               .and();
        assertThat(config, is(notNullValue()));
        assertThat(config.repositories.get(null), is(nullValue()));
    }

    @Test
    public void shouldAllowAddingRepositorySourceInstance() {
        RepositorySource newSource = mock(RepositorySource.class);
        configuration.addRepository(newSource);
    }

    @Test
    public void shouldAllowAddingRepositorySourceByClassNameAndSettingProperties() {
        DnaConfiguration config = configuration.addRepository(DnaEngine.CONFIGURATION_REPOSITORY_NAME)
                                               .usingClass("org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource")
                                               .loadedFromClasspath()
                                               .describedAs("description")
                                               .with("retryLimit")
                                               .setTo(5)
                                               .with("name")
                                               .setTo("repository name")
                                               .and();
        assertThat(config, is(notNullValue()));

        RepositorySource source = config.repositories.get(DnaEngine.CONFIGURATION_REPOSITORY_NAME).getRepositorySource();
        assertThat(source, is(notNullValue()));
        assertThat(source, is(instanceOf(InMemoryRepositorySource.class)));

        InMemoryRepositorySource isource = (InMemoryRepositorySource)source;
        assertThat(isource.getRetryLimit(), is(5));
        assertThat(isource.getName(), is("repository name"));
    }

    @Test
    public void shouldAllowAddingRepositorySourceByClassReferenceAndSettingProperties() {
        DnaConfiguration config = configuration.addRepository(DnaEngine.CONFIGURATION_REPOSITORY_NAME)
                                               .usingClass(InMemoryRepositorySource.class)
                                               .describedAs("description")
                                               .with("retryLimit")
                                               .setTo(5)
                                               .with("name")
                                               .setTo("repository name")
                                               .and();

        assertThat(config, is(notNullValue()));
        assertThat(config.repositories.get(null), is(nullValue()));

        RepositorySource source = config.repositories.get(DnaEngine.CONFIGURATION_REPOSITORY_NAME).getRepositorySource();
        assertThat(source, is(notNullValue()));
        assertThat(source, is(instanceOf(InMemoryRepositorySource.class)));

        InMemoryRepositorySource isource = (InMemoryRepositorySource)source;
        assertThat(isource.getRetryLimit(), is(5));
        assertThat(isource.getName(), is("repository name"));
    }

    @Test
    public void shouldAllowOverwritingRepositorySourceByRepositoryName() {
        DnaConfiguration config = configuration.addRepository(DnaEngine.CONFIGURATION_REPOSITORY_NAME)
                                               .usingClass(InMemoryRepositorySource.class)
                                               .describedAs("description")
                                               .with("retryLimit")
                                               .setTo(3)
                                               .and()
                                               .addRepository(DnaEngine.CONFIGURATION_REPOSITORY_NAME)
                                               .usingClass(InMemoryRepositorySource.class)
                                               .describedAs("description")
                                               .with("name")
                                               .setTo("repository name")
                                               .and();

        assertThat(config, is(notNullValue()));
        assertThat(config.repositories.get(null), is(nullValue()));

        RepositorySource source = config.repositories.get(DnaEngine.CONFIGURATION_REPOSITORY_NAME).getRepositorySource();
        assertThat(source, is(notNullValue()));
        assertThat(source, is(instanceOf(InMemoryRepositorySource.class)));

        InMemoryRepositorySource isource = (InMemoryRepositorySource)source;
        // This relies on the default retry limit for an InMemoryRepositorySource being 0
        // If the original source was not overwritten, this would be 3
        assertThat(isource.getRetryLimit(), is(0));
        assertThat(isource.getName(), is("repository name"));
    }

    @Test
    public void shouldAllowAddingMimeTypeDetector() {
        RepositorySource newSource = mock(RepositorySource.class);
        MimeTypeDetector newDetector = mock(MimeTypeDetector.class);
        DnaConfiguration config = configuration.addRepository(newSource)
                                               .addMimeTypeDetector("default")
                                               .usingClass(newDetector.getClass())
                                               .describedAs("default mime type detector")
                                               .and();

        assertThat(config, is(notNullValue()));
        assertThat(config.mimeTypeDetectors.get("default").getMimeTypeDetector(), instanceOf(MimeTypeDetector.class));
        assertThat(config.mimeTypeDetectors.get("invalid name"), is(nullValue()));
        assertThat(config.mimeTypeDetectors.get("default").getDescription(), is("default mime type detector"));
    }

    @Test
    public void shouldAllowAddingSequencer() {
        RepositorySource newSource = mock(RepositorySource.class);

        DnaConfiguration config = configuration.addRepository(newSource)
                                               .addSequencer("sequencerA")
                                               .usingClass(MockSequencerA.class)
                                               .describedAs("Mock Sequencer A")
                                               .sequencingFrom("/foo/source")
                                               .andOutputtingTo("/foo/target")
                                               .sequencingFrom("/bar/source")
                                               .andOutputtingTo("/bar/target")
                                               .and()
                                               .addSequencer("sequencerB")
                                               .usingClass(MockSequencerB.class)
                                               .sequencingFrom("/baz/source")
                                               .andOutputtingTo("/baz/target")
                                               .describedAs("Mock Sequencer B")
                                               .and();

        assertThat(config, is(notNullValue()));
        assertThat(config.sequencers.get("default"), is(nullValue()));
        assertThat(config.sequencers.get("sequencerA").getSequencer(), instanceOf(MockSequencerA.class));
        assertThat(config.sequencers.get("sequencerB").getSequencer(), instanceOf(MockSequencerB.class));

        DnaSequencerDetails detailsA = config.sequencers.get("sequencerA");
        assertThat(detailsA.getDescription(), is("Mock Sequencer A"));
        assertThat(detailsA.sourcePathExpressions.size(), is(2));
        assertThat(detailsA.sourcePathExpressions.get(0).toString(), is("/foo/source"));
        assertThat(detailsA.targetPathExpressions.get(0).toString(), is("/foo/target"));
        assertThat(detailsA.sourcePathExpressions.get(1).toString(), is("/bar/source"));
        assertThat(detailsA.targetPathExpressions.get(1).toString(), is("/bar/target"));

        DnaSequencerDetails detailsB = config.sequencers.get("sequencerB");
        assertThat(detailsB.getDescription(), is("Mock Sequencer B"));
        assertThat(detailsB.sourcePathExpressions.size(), is(1));
        assertThat(detailsB.sourcePathExpressions.get(0).toString(), is("/baz/source"));
        assertThat(detailsB.targetPathExpressions.get(0).toString(), is("/baz/target"));
    }

}
