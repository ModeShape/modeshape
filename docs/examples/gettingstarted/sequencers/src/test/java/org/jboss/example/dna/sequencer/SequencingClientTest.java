/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * as indicated by the @author tags. See the copyright.txt pngImageUrl in the
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.example.dna.sequencer;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.jboss.dna.common.util.FileUtil;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.jcr.JcrConfiguration;
import org.jboss.dna.jcr.JcrRepository;
import org.jboss.dna.sequencer.java.JavaMetadataSequencer;
import org.jboss.dna.sequencer.mp3.Mp3MetadataSequencer;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class SequencingClientTest {

    private URL pngImageUrl;
    private URL pictImageUrl;
    private URL jpegImageUrl;
    private URL mp3Url;
    private URL javaSourceUrl;
    private SequencingClient client;

    @Before
    public void beforeEach() throws MalformedURLException {
        this.pngImageUrl = Thread.currentThread().getContextClassLoader().getResource("caution.png");
        this.pictImageUrl = Thread.currentThread().getContextClassLoader().getResource("caution.pict");
        this.jpegImageUrl = Thread.currentThread().getContextClassLoader().getResource("caution.jpg");
        this.mp3Url = Thread.currentThread().getContextClassLoader().getResource("sample1.mp3");
        // Get the URL of source (MySource.java), that have to be sequencing
        this.javaSourceUrl = FileUtil.convertFileToURL("workspace/project1/src/org/acme/MySource.java");

        String repositoryId = "content";
        String workspaceName = "default";
        JcrConfiguration config = new JcrConfiguration();
        // Set up the in-memory source where we'll upload the content and where the sequenced output will be stored ...
        config.repositorySource("store")
              .usingClass(InMemoryRepositorySource.class)
              .setDescription("The repository for our content")
              .setProperty("defaultWorkspaceName", workspaceName);
        // Set up the JCR repository to use the source ...
        config.repository(repositoryId)
              .addNodeTypes(getClass().getClassLoader().getResource("sequencing.cnd"))
              .setSource("store")
              .setOption(JcrRepository.Option.JAAS_LOGIN_CONFIG_NAME, "dna-jcr");
        // Set up the image sequencer ...
        config.sequencer("Image Sequencer")
              .usingClass("org.jboss.dna.sequencer.image.ImageMetadataSequencer")
              .loadedFromClasspath()
              .setDescription("Sequences image files to extract the characteristics of the image")
              .sequencingFrom("//(*.(jpg|jpeg|gif|bmp|pcx|png|iff|ras|pbm|pgm|ppm|psd)[*])/jcr:content[@jcr:data]")
              .andOutputtingTo("/images/$1");
        // Set up the MP3 sequencer ...
        config.sequencer("MP3 Sequencer")
              .usingClass(Mp3MetadataSequencer.class)
              .setDescription("Sequences mp3 files to extract the id3 tags of the audio file")
              .sequencingFrom("//(*.mp3[*])/jcr:content[@jcr:data]")
              .andOutputtingTo("/mp3s/$1");
        // Set up the Java source file sequencer ...
        config.sequencer("Java Sequencer")
              .usingClass(JavaMetadataSequencer.class)
              .setDescription("Sequences mp3 files to extract the id3 tags of the audio file")
              .sequencingFrom("//(*.mp3[*])/jcr:content[@jcr:data]")
              .andOutputtingTo("/mp3s/$1");

        // Now start the client and tell it which repository and workspace to use ...
        client = new SequencingClient(config, repositoryId, workspaceName);
    }

    @After
    public void afterEach() throws Exception {
        if (client != null) client.shutdownRepository();
    }

    @Test
    public void shouldFindMedias() {
        assertThat(this.pictImageUrl, is(notNullValue()));
        assertThat(this.pngImageUrl, is(notNullValue()));
        assertThat(this.jpegImageUrl, is(notNullValue()));
        assertThat(this.mp3Url, is(notNullValue()));
    }

    @Test
    public void shouldStartupAndShutdownRepository() throws Exception {
        client.startRepository();
        client.shutdownRepository();

    }

    @Test
    public void shouldUploadAndSequencePngFile() throws Exception {
        client.setUserInterface(new MockUserInterface(this.pngImageUrl, "/a/b/caution.png", 1));
        client.startRepository();
        client.uploadFile();

        // Use a trick to wait until the sequencing has been done by sleeping (to give the sequencing time to start)
        // and to then shut down the DNA services (which will block until all sequencing has been completed) ...
        Thread.sleep(4000);

        assertThat(client.getStatistics().getNumberOfNodesSequenced(), is(1l));

        // The sequencers should have run, so perform the search.
        // The mock user interface checks the results.
        client.search();
    }

    @Ignore
    @Test
    public void shouldUploadAndSequenceJpegFile() throws Exception {
        client.setUserInterface(new MockUserInterface(this.jpegImageUrl, "/a/b/caution.jpeg", 1));
        client.startRepository();
        client.uploadFile();

        // Use a trick to wait until the sequencing has been done by sleeping (to give the sequencing time to start)
        // and to then shut down the DNA services (which will block until all sequencing has been completed) ...
        Thread.sleep(1000);

        // The sequencers should have run, so perform the search.
        // The mock user interface checks the results.
        client.search();

        assertThat(client.getStatistics().getNumberOfNodesSequenced(), is(1l));
    }

    @Ignore
    @Test
    public void shouldUploadAndNotSequencePictFile() throws Exception {
        client.setUserInterface(new MockUserInterface(this.pictImageUrl, "/a/b/caution.pict", 0));
        client.startRepository();
        client.uploadFile();

        // Use a trick to wait until the sequencing has been done by sleeping (to give the sequencing time to start)
        // and to then shut down the DNA services (which will block until all sequencing has been completed) ...
        Thread.sleep(1000);

        // The sequencers should have run, so perform the search.
        // The mock user interface checks the results.
        client.search();

        assertThat(client.getStatistics().getNumberOfNodesSequenced(), is(0l));
    }

    @Ignore
    @Test
    public void shouldUploadAndSequenceMp3File() throws Exception {
        client.setUserInterface(new MockUserInterface(this.mp3Url, "/a/b/test.mp3", 1));
        client.startRepository();
        client.uploadFile();

        // Use a trick to wait until the sequencing has been done by sleeping (to give the sequencing time to start)
        // and to then shut down the DNA services (which will block until all sequencing has been completed) ...
        Thread.sleep(1000);

        // The sequencers should have run, so perform the search.
        // The mock user interface checks the results.
        client.search();

        assertThat(client.getStatistics().getNumberOfNodesSequenced(), is(1l));
    }

    @Test
    public void shouldFindCompilationUnitSource() throws IOException {
        assertThat(this.javaSourceUrl, is(notNullValue()));
        InputStream stream = this.javaSourceUrl.openStream();
        try {
            byte[] buffer = new byte[1024];
            while (stream.read(buffer) != -1) {
            }
        } finally {
            stream.close();
        }
    }

    @Ignore
    @Test
    public void shouldUploadAndSequenceJavaSourceFile() throws Exception {
        client.setUserInterface(new MockUserInterface(this.javaSourceUrl, "/a/b/MySource.java", 1));
        client.startRepository();
        client.uploadFile();

        // Use a trick to wait until the sequencing has been done by sleeping (to give the sequencing time to start)
        // and to then shut down the DNA services (which will block until all sequencing has been completed) ...
        Thread.sleep(1000);

        // The sequencers should have run, so perform the search.
        // The mock user interface checks the results.
        client.search();
        assertThat(client.getStatistics().getNumberOfNodesSequenced(), is(1L));
    }

}
