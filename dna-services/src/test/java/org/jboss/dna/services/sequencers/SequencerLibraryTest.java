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

package org.jboss.dna.services.sequencers;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import javax.jcr.Node;
import org.jboss.dna.maven.MavenId;
import org.jboss.dna.maven.MavenRepository;
import org.jboss.dna.maven.spi.JcrMavenUrlProvider;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class SequencerLibraryTest {

    private SequencerLibrary library;
    private ClassLoader emptyClassLoader;
    private MavenRepository mavenRepository;
    private SequencerConfig configA;
    private SequencerConfig configB;
    private SequencerConfig configA2;
    private String[] validRunRules;
    private MavenId[] validMavenIds;
    private Mockery context = new Mockery();

    @Before
    public void beforeEach() throws Exception {
        this.library = new SequencerLibrary();
        this.emptyClassLoader = new URLClassLoader(new URL[] {});
        this.mavenRepository = new MavenRepository(new JcrMavenUrlProvider());
        this.validRunRules = new String[] {"mime type is 'text/plain'"};
        this.validMavenIds = MavenId.createClasspath("com.acme:configA:1.0,com.acme:configB:1.0");
        this.configA = new SequencerConfig("configA", "Config A", MockSequencerA.class.getName(), validMavenIds, validRunRules);
        this.configB = new SequencerConfig("configB", "Config B", MockSequencerB.class.getName(), validMavenIds, validRunRules);
        this.configA2 = new SequencerConfig("conFigA", "Config A v2", MockSequencerA.class.getName(), validMavenIds, validRunRules);
    }

    @Test
    public void shouldBeInstantiableWithDefaultConstructor() {
        new SequencerLibrary();
    }

    @Test
    public void shouldHaveClassLoaderFromClassForParentClassLoader() {
        assertThat(this.library.getParentClassLoader(), is(notNullValue()));
        assertThat(this.library.getParentClassLoader(), is(sameInstance(library.getClass().getClassLoader())));
    }

    @Test
    public void shouldAllowSettingOfParentClassLoaderToNull() {
        assertThat(library.getParentClassLoader(), is(notNullValue()));
        this.library.setParentClassLoader(emptyClassLoader);
        assertThat(library.getParentClassLoader(), is(sameInstance(emptyClassLoader)));
        this.library.setParentClassLoader(null);
        assertThat(this.library.getParentClassLoader(), is(sameInstance(library.getClass().getClassLoader())));
    }

    @Test
    public void shouldHaveNullMavenRepositoryByDefault() {
        assertThat(this.library.getMavenRepository(), is(nullValue()));
    }

    @Test
    public void shouldAllowSettingMavenRepositoryToNull() {
        for (int i = 0; i != 2; ++i) {
            library.setMavenRepository(mavenRepository);
            assertThat(library.getMavenRepository(), is(sameInstance(mavenRepository)));
            library.setMavenRepository(null);
            assertThat(library.getMavenRepository(), is(nullValue()));
        }
    }

    @Test
    public void shouldAddSequencerWhenNoneExists() {
        assertThat(library.getSequencers().size(), is(0));
        library.addSequencer(configA);
        assertThat(library.getSequencers().size(), is(1));
    }

    @Test
    public void shouldAddSequencerWhenMatchingConfigAlreadyExistsUnlessNotChanged() {
        assertThat(library.getSequencers().size(), is(0));
        library.addSequencer(configA);
        assertThat(library.getSequencers().size(), is(1));
        assertThat(library.getSequencers().get(0).getConfiguration(), is(sameInstance(configA)));

        library.addSequencer(configA);
        assertThat(library.getSequencers().size(), is(1));
        assertThat(library.getSequencers().get(0).getConfiguration(), is(sameInstance(configA)));

        // Add another, but this isn't changed ...
        library.addSequencer(configA2);
        assertThat(library.getSequencers().size(), is(1));
        assertThat(library.getSequencers().get(0).getConfiguration(), is(sameInstance(configA)));

        // Change the configuration, then add another ...
        validRunRules = new String[] {"mime type is 'text/plain' or mime type is 'something'"};
        configA2 = new SequencerConfig("conFigA", "Config A v2", MockSequencerA.class.getName(), validMavenIds, validRunRules);
        library.addSequencer(configA2);
        assertThat(library.getSequencers().size(), is(1));
        assertThat(library.getSequencers().get(0).getConfiguration(), is(sameInstance(configA2)));

        // Add a second that isn't there
        library.addSequencer(configB);
        assertThat(library.getSequencers().size(), is(2));
        assertThat(library.getSequencers().get(1).getConfiguration(), is(sameInstance(configB)));
    }

    @Test
    public void shouldInstantiateAndConfigureSequencerWhenConfigurationAddedOrUpdated() {
        assertThat(library.getSequencers().size(), is(0));
        library.addSequencer(configA);
        List<ISequencer> sequencers = library.getSequencers();
        assertThat(sequencers.size(), is(1));
        MockSequencerA firstSequencer = (MockSequencerA)sequencers.get(0);
        assertThat(firstSequencer.isConfigured(), is(true));

        // Update the configuration, and a new sequencer should be instantiated ...
        library.addSequencer(configA);
        sequencers = library.getSequencers();
        assertThat(sequencers.size(), is(1));
        assertThat(sequencers.get(0), instanceOf(MockSequencerA.class));
        MockSequencerA secondSequencerA = (MockSequencerA)sequencers.get(0);
        assertThat(secondSequencerA.isConfigured(), is(true));

        // The current sequencer should be the same instance since it was unchanged ...
        assertThat(secondSequencerA, is(sameInstance(firstSequencer)));

        // The current sequencer should not be the same instance since it was just changed ...
        validRunRules = new String[] {"mime type is 'text/plain' or mime type is 'something'"};
        configA = new SequencerConfig("conFigA", "Config A v2", MockSequencerA.class.getName(), validMavenIds, validRunRules);
        library.addSequencer(configA);
        sequencers = library.getSequencers();
        assertThat(sequencers.size(), is(1));
        assertThat(sequencers.get(0), instanceOf(MockSequencerA.class));
        secondSequencerA = (MockSequencerA)sequencers.get(0);
        assertThat(secondSequencerA.isConfigured(), is(true));
        assertThat(secondSequencerA, is(not(sameInstance(firstSequencer))));

        // Add a second sequencer config, and the first sequencer should not be changed
        library.addSequencer(configB);
        sequencers = library.getSequencers();
        assertThat(sequencers.size(), is(2));
        assertThat(sequencers.get(0), instanceOf(MockSequencerA.class));
        assertThat(sequencers.get(0), is(sameInstance((ISequencer)secondSequencerA)));
        assertThat(sequencers.get(1), instanceOf(MockSequencerB.class));
        MockSequencerB firstSequencerB = (MockSequencerB)sequencers.get(1);

        // The very first sequencer instance should still be runnable ...
        final Node mockNode = context.mock(Node.class);
        for (int i = 0; i != 10; ++i) {
            firstSequencer.execute(mockNode);
        }
        assertThat(firstSequencer.getCounter(), is(10));

        // The second sequencer instance should still be runnable ...
        for (int i = 0; i != 10; ++i) {
            secondSequencerA.execute(mockNode);
        }
        assertThat(secondSequencerA.getCounter(), is(10));

        // The third sequencer instance should still be runnable ...
        for (int i = 0; i != 10; ++i) {
            firstSequencerB.execute(mockNode);
        }
        assertThat(firstSequencerB.getCounter(), is(10));
    }
}
