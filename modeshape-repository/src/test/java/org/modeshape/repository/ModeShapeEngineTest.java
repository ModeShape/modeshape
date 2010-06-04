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
package org.modeshape.repository;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.collection.Problem;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.connector.path.cache.InMemoryWorkspaceCache.InMemoryCachePolicy;
import org.modeshape.graph.mimetype.ExtensionBasedMimeTypeDetector;
import org.modeshape.graph.mimetype.MimeTypeDetector;
import org.modeshape.repository.sequencer.MockStreamSequencerA;
import org.modeshape.repository.sequencer.SequencingService;

/**
 * @author Randall Hauch
 */

public class ModeShapeEngineTest {

    private ModeShapeEngine engine;

    @Before
    public void beforeEach() {
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

    @Test
    public void shouldAllowCreatingWithEmptyConfig() {
        engine = new ModeShapeConfiguration().build();
    }

    @Test
    public void shouldAllowCreatingWithConfigRepository() throws InterruptedException {
        InMemoryRepositorySource configSource = new InMemoryRepositorySource();
        configSource.setName("config repo");
        engine = new ModeShapeConfiguration().loadFrom(configSource).build();
        engine.start();

        assertThat(engine.getRepositorySource("config repo"), is(notNullValue()));
        assertThat(engine.getRepositorySource("config repo"), is(instanceOf(InMemoryRepositorySource.class)));

        RepositoryLibrary library = engine.getRepositoryService().getRepositoryLibrary();
        assertThat(library.getConnectionPool("config repo").getInUseCount(), is(0));

        RepositoryConnection connection = library.getConnectionPool("config repo").getConnection();
        assertThat(connection.ping(500, TimeUnit.MILLISECONDS), is(true));
        connection.close();

    }

    @Test
    public void shouldAllowCreatingMultipleRepositorySources() throws Exception {
        InMemoryRepositorySource configSource = new InMemoryRepositorySource();
        configSource.setName("config repo");
        engine = new ModeShapeConfiguration().loadFrom(configSource)
                                             .and()
                                             .repositorySource("JCR")
                                             .usingClass(InMemoryRepositorySource.class)
                                             .setDescription("Backing Repository for JCR Implementation")
                                             .setProperty("name", "JCR")
                                             .and()
                                             .save()
                                             .build();
        // Start the engine ...
        engine.start();

        // Verify the components are here ...
        assertThat(engine.getRepositorySource("config repo"), is(notNullValue()));
        assertThat(engine.getRepositorySource("config repo"), is(instanceOf(InMemoryRepositorySource.class)));

        assertThat(engine.getRepositorySource("JCR"), is(notNullValue()));
        assertThat(engine.getRepositorySource("JCR"), is(instanceOf(InMemoryRepositorySource.class)));
        assertThat(engine.getRepositorySource("JCR").getName(), is("JCR"));

        RepositoryLibrary library = engine.getRepositoryService().getRepositoryLibrary();
        RepositoryConnection connection = library.getConnectionPool("JCR").getConnection();
        assertThat(connection.ping(500, TimeUnit.MILLISECONDS), is(true));
        connection.close();

    }

    @Test
    public void shouldAllowAddingMimeTypeDetectors() throws Exception {
        InMemoryRepositorySource configSource = new InMemoryRepositorySource();
        configSource.setName("config repo");
        engine = new ModeShapeConfiguration().loadFrom(configSource)
                                             .and()
                                             .repositorySource("JCR")
                                             .usingClass(InMemoryRepositorySource.class)
                                             .setDescription("Backing Repository for JCR Implementation")
                                             .setProperty("name", "JCR")
                                             .and()
                                             .mimeTypeDetector("default")
                                             .usingClass(ExtensionBasedMimeTypeDetector.class)
                                             .setDescription("Default MimeTypeDetector")
                                             .and()
                                             .build();
        engine.start();

        assertThat(engine.getRepositorySource("config repo"), is(notNullValue()));
        assertThat(engine.getRepositorySource("config repo"), is(instanceOf(InMemoryRepositorySource.class)));

        MimeTypeDetector detector = engine.getExecutionContext().getMimeTypeDetector();
        assertThat(detector.mimeTypeOf("test", new ByteArrayInputStream("This is useless data".getBytes())), is("text/plain"));
    }

    @Test
    public void shouldAllowAddingSequencers() throws Exception {
        InMemoryRepositorySource configSource = new InMemoryRepositorySource();
        configSource.setName("config repo");
        engine = new ModeShapeConfiguration().loadFrom(configSource)
                                             .and()
                                             .repositorySource("JCR")
                                             .usingClass(InMemoryRepositorySource.class)
                                             .setDescription("Backing Repository for JCR Implementation")
                                             .setProperty("name", "JCR")
                                             .and()
                                             .sequencer("Mock Sequencer A")
                                             .usingClass(MockStreamSequencerA.class)
                                             .setDescription("A Mock Sequencer")
                                             .sequencingFrom("/**")
                                             .andOutputtingTo("/")
                                             .and()
                                             .build();
        engine.start();

        assertThat(engine.getRepositorySource("config repo"), is(notNullValue()));
        assertThat(engine.getRepositorySource("config repo"), is(instanceOf(InMemoryRepositorySource.class)));

        SequencingService sequencer = engine.getSequencingService();
        assertThat(sequencer.getStatistics().getNumberOfNodesSequenced(), is(0L));

        // Event e1 = mock(Event.class);
        // when(e1.getType()).thenReturn(Event.NODE_ADDED);
        // when(e1.getPath()).thenReturn("/test");
        // when(e1.getUserID()).thenReturn("Test");

        // changes = NodeChanges.create("", Arrays.asList(new Event[] { e1, }));
        // sequencer.onNodeChanges(changes);
        //        
        // // Shutdown the engine to force all pending tasks to complete
        // engine.shutdown();
        //        
        // assertThat(sequencer.getStatistics().getNumberOfNodesSequenced(), is(1L));

    }

    @Test
    public void shouldSetCachePolicyDefinedInConfigFile() throws Exception {
        engine = new ModeShapeConfiguration().loadFrom("src/test/resources/config/cacheConfigRepository.xml").build();
        engine.start();

        for (Problem problem : engine.getProblems()) {
            System.err.println(problem.toString());
        }
        assertThat(engine.getProblems().isEmpty(), is(true));

        assertThat(engine.getRepositorySource("Cache"), is(notNullValue()));
        assertThat(engine.getRepositorySource("Cache"), is(instanceOf(FakeRepositorySource.class)));

        FakeRepositorySource source = (FakeRepositorySource)engine.getRepositorySource("Cache");
        assertThat(source.getCachePolicy(), is(notNullValue()));
        assertThat(source.getCachePolicy(), is(instanceOf(InMemoryCachePolicy.class)));

        InMemoryCachePolicy cachePolicy = (InMemoryCachePolicy)source.getCachePolicy();
        assertThat(cachePolicy.getTimeToLive(), is(30L));
    }

    public static class MockMimeTypeDetector implements MimeTypeDetector {
        private String mimeType = "";

        public MockMimeTypeDetector() {

        }

        public void setMimeType( String mimeType ) {
            this.mimeType = mimeType;
        }

        public String mimeTypeOf( String name,
                                  InputStream is ) {
            return mimeType;
        }
    }

}
