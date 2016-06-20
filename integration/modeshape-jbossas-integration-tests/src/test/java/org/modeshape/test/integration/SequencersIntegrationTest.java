/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.modeshape.jcr.api.observation.Event.Sequencing.NODE_SEQUENCED;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.JcrSession;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.observation.Event;

/**
 * Arquillian integration tests that uses the predefined repository which contains sequencers, to test that sequencing is
 * successful. This test verifies that: - each of the built-in sequencers are configured in the repository - each sequencer, given
 * an input file at the preconfigured path, sequences that file in the preconfigured output (see standalone-modeshape.xml)
 *
 * @author Horia Chiorean
 */

@RunWith( Arquillian.class )
public class SequencersIntegrationTest {

    private static final String SEQUENCING_EXPRESSION_INPUT_ROOT = "files";

    @Resource( mappedName = "/jcr/artifacts" )
    private JcrRepository repository;

    private JcrTools jcrTools = new JcrTools();

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "sequencers-test.war")
                         .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                         .addAsResource(new File("src/test/resources/sequencer"))
                         .setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
    }

    @Before
    public void before() throws Exception {
        JcrSession session = repository.login();
        try {
            jcrTools.findOrCreateChild(session.getRootNode(), SEQUENCING_EXPRESSION_INPUT_ROOT);
        } catch (RepositoryException e) {
            session.logout();
        }
    }

    @After
    public void after() throws Exception {
        JcrSession session = repository.login();
        try {
            remove(session, "/" + SEQUENCING_EXPRESSION_INPUT_ROOT);
            remove(session, "/derived");
            session.save();
        } finally {
            session.logout();
        }
    }

    private void remove( JcrSession session, String absPath ) throws RepositoryException {
        Node node = null;
        try {
            node = session.getNode(absPath);
            node.remove();
        } catch (PathNotFoundException e) {
            //ignore   
        }
    }

    @Test
    public void shouldSequenceImage() throws Exception {
        uploadFileAndAssertSequenced("/image_file.jpg", "/derived/image",
                                     "org.modeshape.sequencer.image.ImageMetadataSequencer");
    }

    @Test
    public void shouldSequenceMp3() throws Exception {
        uploadFileAndAssertSequenced("/mp3_file.mp3", "/derived/mp3", "org.modeshape.sequencer.mp3.Mp3MetadataSequencer");
    }

    @Test
    public void shouldSequenceOgg() throws Exception {
        uploadFileAndAssertSequenced("/sample.ogg", "/derived/audio", "org.modeshape.sequencer.audio.AudioMetadataSequencer");
    }

    @Test
    public void shouldSequenceZip() throws Exception {
        uploadFileAndAssertSequenced("/zip_file.zip", "/derived/zip", "org.modeshape.sequencer.zip.ZipSequencer");
    }

    @Test
    public void shouldSequenceJavaFile() throws Exception {
        uploadFileAndAssertSequenced("/java_file.java", "/derived/java", "org.modeshape.sequencer.javafile.JavaFileSequencer");
    }

    @Test
    public void shouldSequenceClassFile() throws Exception {
        uploadFileAndAssertSequenced("/class_file.clazz",
                                     "/derived/class",
                                     "org.modeshape.sequencer.classfile.ClassFileSequencer");
    }

    @Test
    public void shouldSequenceDdlFile() throws Exception {
        uploadFileAndAssertSequenced("/ddl_file.ddl", "/derived/ddl", "org.modeshape.sequencer.ddl.DdlSequencer");
    }

    @Test
    public void shouldSequenceCndFile() throws Exception {
        uploadFileAndAssertSequenced("/cnd_file.cnd", "/derived/cnd", "org.modeshape.sequencer.cnd.CndSequencer");
    }

    @Test
    public void shouldSequenceDelimitedTextFile() throws Exception {
        uploadFileAndAssertSequenced("/delimited_file.csv",
                                     "/derived/text/delimited",
                                     "org.modeshape.sequencer.text.DelimitedTextSequencer");
    }

    @Test
    public void shouldSequenceFixedTextFile() throws Exception {
        uploadFileAndAssertSequenced("/fixed_file.txt",
                                     "/derived/text/fixedWidth",
                                     "org.modeshape.sequencer.text.FixedWidthTextSequencer");
    }

    @Test
    public void shouldSequenceMsOfficeFile() throws Exception {
        uploadFileAndAssertSequenced("/msoffice_file.xls",
                                     "/derived/msoffice",
                                     "org.modeshape.sequencer.msoffice.MSOfficeMetadataSequencer");
    }

    @Test
    public void shouldSequenceWsdlFile() throws Exception {
        uploadFileAndAssertSequenced("/wsdl_file.wsdl", "/derived/wsdl", "org.modeshape.sequencer.wsdl.WsdlSequencer");
    }

    @Test
    public void shouldSequenceXMLFile() throws Exception {
        uploadFileAndAssertSequenced("/xml_file.xml", "/derived/xml", "org.modeshape.sequencer.xml.XmlSequencer");
    }

    @Test
    public void shouldSequenceXSDFile() throws Exception {
        uploadFileAndAssertSequenced("/xsd_file.xsd", "/derived/xsd", "org.modeshape.sequencer.xsd.XsdSequencer");
    }

    @Test
    public void shouldSequencePDFFile() throws Exception {
        uploadFileAndAssertSequenced("/sample.pdf", "/derived/pdf", "org.modeshape.sequencer.pdf.PdfMetadataSequencer");
    }

    @Test
    public void shouldSequenceEpubFile() throws Exception {
        uploadFileAndAssertSequenced("/sample.epub", "/derived/epub", "org.modeshape.sequencer.epub.EpubMetadataSequencer");
    }

    @Test
    public void shouldSequenceODFFile() throws Exception {
        uploadFileAndAssertSequenced("/text.odt", "/derived/odf", "org.modeshape.sequencer.odf.OdfMetadataSequencer");
    }

    @Test
    @FixFor( "MODE-2288" )
    public void shouldManuallySequenceZip() throws Exception {
        JcrSession session = repository.login("default");
        String outputNode = "output_zip_" + UUID.randomUUID().toString();
        ((Node) session.getRootNode()).addNode(outputNode);

        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("sequencer/zip_file_1.zip");
        assertNotNull(resourceAsStream);
        jcrTools.uploadFile(session, "/testRoot/zip", resourceAsStream);
        session.save();

        String outputPath = "/" + outputNode;
        Node output = session.getNode(outputPath);
        Property binaryProperty = session.getProperty("/testRoot/zip/jcr:content/jcr:data");
        session.sequence("zip-sequencer-manual", binaryProperty, output);
        session.save();

        assertEquals(1, ((Node) session.getNode(outputPath)).getNodes().getSize());
    }

    private void uploadFileAndAssertSequenced( String fileName,
                                               String outputPathPrefix,
                                               String expectedSequencerClassConfigured ) throws Exception {
        assertSequencerConfigured(expectedSequencerClassConfigured);

        Session session = repository.login("default");

        ObservationManager observationManager = session.getWorkspace().getObservationManager();
        CountDownLatch latch = new CountDownLatch(1);
        SequencingListener listener = new SequencingListener(latch);
        observationManager.addEventListener(listener, NODE_SEQUENCED, null, true, null, null, false);

        String inputNodePath = "/files" + fileName;
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("sequencer" + fileName);
        assertNotNull(resourceAsStream);
        // class files can't be named .class because the container tries to load them
        if (fileName.endsWith(".clazz")) {
            inputNodePath = "/files" + fileName.substring(0, fileName.indexOf(".clazz")) + ".class";
        }
        jcrTools.uploadFile(session, inputNodePath, resourceAsStream);
        session.save();

        assertTrue(latch.await(15, TimeUnit.SECONDS));
        String outputNodePath = listener.getSequencedNodePath();
        assertTrue(outputNodePath.startsWith(outputPathPrefix));
    }

    private void assertSequencerConfigured( String expectedSequencerClassConfigured ) {
        List<RepositoryConfiguration.Component> sequencers = repository.getConfiguration().getSequencing().getSequencers();
        boolean sequencerConfigured = true;
        for (RepositoryConfiguration.Component component : sequencers) {
            String className = component.getClassname();
            String classNameFromAlias = RepositoryConfiguration.getBuiltInSequencerClassName(className);
            sequencerConfigured = expectedSequencerClassConfigured.equalsIgnoreCase(className)
                                  || expectedSequencerClassConfigured.equalsIgnoreCase(classNameFromAlias);
            if (sequencerConfigured) {
                break;
            }
        }

        if (!sequencerConfigured) {
            System.err.println("Sequencer configuration: " + sequencers);
        }
        assertTrue("The expected sequencer:" + expectedSequencerClassConfigured + " is not configured in the repository",
                   sequencerConfigured);
    }


    protected static final class SequencingListener implements EventListener {
        private final CountDownLatch latch;
        private volatile String sequencedNodePath;

        protected SequencingListener( CountDownLatch latch ) {
            this.latch = latch;
        }

        @Override
        public void onEvent( EventIterator events ) {
            try {
                Event event = (Event) events.nextEvent();
                this.sequencedNodePath = event.getPath();
                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        String getSequencedNodePath() {
            return sequencedNodePath;
        }
    }

}
