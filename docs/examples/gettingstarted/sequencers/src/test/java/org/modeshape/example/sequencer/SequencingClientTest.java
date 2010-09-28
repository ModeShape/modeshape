/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * as indicated by the @author tags. See the copyright.txt pngImageUrl in the
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
package org.modeshape.example.sequencer;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.util.FileUtil;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.sequencer.classfile.ClassFileSequencer;
import org.modeshape.sequencer.classfile.ClassFileSequencerLexicon;
import org.modeshape.sequencer.java.JavaMetadataSequencer;
import org.modeshape.sequencer.mp3.Mp3MetadataSequencer;
import org.modeshape.sequencer.zip.ZipSequencer;

/**
 * @author Randall Hauch
 */
public class SequencingClientTest {

    private URL pngImageUrl;
    private URL pictImageUrl;
    private URL jpegImageUrl;
    private URL mp3Url;
    private URL jarUrl;
    private URL classUrl;
    private URL javaSourceUrl;
    private URL csvUrl;
    private URL fixedWidthFileUrl;
    private SequencingClient client;

    @Before
    public void beforeEach() throws MalformedURLException {
        this.pngImageUrl = Thread.currentThread().getContextClassLoader().getResource("caution.png");
        this.pictImageUrl = Thread.currentThread().getContextClassLoader().getResource("caution.pict");
        this.jpegImageUrl = Thread.currentThread().getContextClassLoader().getResource("caution.jpg");
        this.mp3Url = Thread.currentThread().getContextClassLoader().getResource("sample1.mp3");
        this.jarUrl = Thread.currentThread().getContextClassLoader().getResource("test.jar");
        this.classUrl = Thread.currentThread().getContextClassLoader().getResource("JcrRepository.clazz");
        this.csvUrl = Thread.currentThread().getContextClassLoader().getResource("test.csv");
        this.fixedWidthFileUrl = Thread.currentThread().getContextClassLoader().getResource("fixedWidthFile.txt");

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
              .registerNamespace(ClassFileSequencerLexicon.Namespace.PREFIX, ClassFileSequencerLexicon.Namespace.URI)
              .setSource("store")
              .setOption(JcrRepository.Option.JAAS_LOGIN_CONFIG_NAME, "modeshape-jcr");
        // Set up the image sequencer ...
        config.sequencer("Image Sequencer")
              .usingClass("org.modeshape.sequencer.image.ImageMetadataSequencer")
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
        // Set up the zip sequencer ...
        config.sequencer("Zip Sequencer")
              .usingClass(ZipSequencer.class)
              .setDescription("Sequences Zip, WAR, and JAR files to extract the contents")
              .sequencingFrom("//(*.(zip|war|jar)[*])/jcr:content[@jcr:data]")
              .andOutputtingTo("/zips/$1");
        // Set up the Java source file sequencer ...
        config.sequencer("Java Sequencer")
              .usingClass(JavaMetadataSequencer.class)
              .setDescription("Sequences Java files to extract the AST structure of the Java source code")
              .sequencingFrom("//(*.java[*])/jcr:content[@jcr:data]")
              .andOutputtingTo("/classes");
        // .andOutputtingTo("/java/$1");
        // Set up the Java class file sequencer ...
        // Only looking for one class to make verification easier
        config.sequencer("Java Class Sequencer")
              .usingClass(ClassFileSequencer.class)
              .setDescription("Sequences Java class files to extract the structure of the classes")
              .sequencingFrom("//JcrRepository.clazz[*]/jcr:content[@jcr:data]")
              .andOutputtingTo("/classes");
        // Set up the CSV file sequencer ...
        config.sequencer("CSV Sequencer")
              .usingClass("org.modeshape.sequencer.text.DelimitedTextSequencer")
              .loadedFromClasspath()
              .setDescription("Sequences CSV files to extract the contents")
              .sequencingFrom("//(*.csv[*])/jcr:content[@jcr:data]")
              .andOutputtingTo("/csv/$1");
        // Set up the fixed width file sequencer ...
        config.sequencer("Fixed Width Sequencer")
              .usingClass("org.modeshape.sequencer.text.FixedWidthTextSequencer")
              .loadedFromClasspath()
              .setDescription("Sequences fixed width files to extract the contents")
              .setProperty("commentMarker", "#")
              .setProperty("columnStartPositions", new int[] {10, 20, 30, 40})
              .sequencingFrom("//(*.txt[*])/jcr:content[@jcr:data]")
              .andOutputtingTo("/txt/$1");

        // Now start the client and tell it which repository and workspace to use ...
        client = new SequencingClient(config, repositoryId, workspaceName);
    }

    @After
    public void afterEach() throws Exception {
        if (client != null) client.shutdownRepository();
        // while (true) {
        // Thread.sleep(300000);
        // }
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

        waitUntilSequencedNodesIs(1);

        // The sequencers should have run, so perform the search.
        // The mock user interface checks the results.
        client.search();
    }

    @Test
    public void shouldUploadAndSequenceJpegFile() throws Exception {
        client.setUserInterface(new MockUserInterface(this.jpegImageUrl, "/a/b/caution.jpeg", 1));
        client.startRepository();
        client.uploadFile();

        waitUntilSequencedNodesIs(1);

        // The sequencers should have run, so perform the search.
        // The mock user interface checks the results.
        client.search();
    }

    @Test
    public void shouldUploadAndSequenceMp3File() throws Exception {
        client.setUserInterface(new MockUserInterface(this.mp3Url, "/a/b/test.mp3", 1));
        client.startRepository();
        client.uploadFile();

        waitUntilSequencedNodesIs(1);

        // The sequencers should have run, so perform the search.
        // The mock user interface checks the results.
        client.search();
    }

    @Test
    public void shouldUploadAndSequenceZipFile() throws Exception {
        client.setUserInterface(new MockUserInterface(this.jarUrl, "/a/b/test.jar", 168));
        client.startRepository();
        client.uploadFile();

        waitUntilSequencedNodesIs(1);

        // The sequencers should have run, so perform the search.
        // The mock user interface checks the results.
        client.search();
    }

    @Test
    public void shouldUploadAndSequenceClassFile() throws Exception {
        client.setUserInterface(new MockUserInterface(this.classUrl, "/a/b/JcrRepository.clazz", 1));
        client.startRepository();
        client.uploadFile();

        waitUntilSequencedNodesIs(1);

        // The sequencers should have run, so perform the search.
        // The mock user interface checks the results.
        client.search();
    }

    @Test
    public void shouldUploadAndSequenceJavaSourceFile() throws Exception {
        client.setUserInterface(new MockUserInterface(this.javaSourceUrl, "/a/b/MySource.java", 1));
        client.startRepository();
        client.uploadFile();

        waitUntilSequencedNodesIs(1);

        // The sequencers should have run, so perform the search.
        // The mock user interface checks the results.
        client.search();
    }

    @Test
    public void shouldUploadAndSequenceDelimitedFile() throws Exception {
        client.setUserInterface(new MockUserInterface(this.csvUrl, "/a/b/test.csv", 1));
        client.startRepository();
        client.uploadFile();

        waitUntilSequencedNodesIs(1);

        // The sequencers should have run, so perform the search.
        // The mock user interface checks the results.
        client.search();
    }

    @Test
    public void shouldUploadAndSequenceFixedWidthFile() throws Exception {
        client.setUserInterface(new MockUserInterface(this.fixedWidthFileUrl, "/a/b/fixedWidthFile.txt", 1));
        client.startRepository();
        client.uploadFile();

        waitUntilSequencedNodesIs(1);

        // The sequencers should have run, so perform the search.
        // The mock user interface checks the results.
        client.search();
    }

    protected void waitUntilSequencedNodesIs( int totalNumberOfNodesSequenced ) throws InterruptedException {
        // check 50 times, waiting 0.1 seconds between (for a total of 5 seconds max) ...
        long numFound = 0;
        for (int i = 0; i != 50; i++) {
            numFound = client.getStatistics().getNumberOfNodesSequenced();
            if (numFound >= totalNumberOfNodesSequenced) {
                // Wait for the sequenced output to be saved before searching ...
                Thread.sleep(500);
                return;
            }
            Thread.sleep(100);
        }
        fail("Expected to find " + totalNumberOfNodesSequenced + " nodes sequenced, but found " + numFound);
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

}
