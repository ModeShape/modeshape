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

package org.modeshape.test.integration;

import javax.annotation.Resource;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.observation.Event;
import static org.modeshape.jcr.api.observation.Event.Sequencing.NODE_SEQUENCED;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Arquillian integration tests that uses the predefined repository which contains sequencers, to test that sequencing is
 * successful. This test verifies that:
 *  - each of the built-in sequencers are configured in the repository
 *  - each sequencer, given an input file at the preconfigured path, sequences that file in the preconfigured output (see standalone-modeshape.xml)
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

    @Test
    public void shouldSequenceImage() throws Exception {
        uploadFileAndAssertSequenced("/image_file.png", "/derived/image", "org.modeshape.sequencer.image.ImageMetadataSequencer");
    }

    @Test
    public void shouldSequenceMp3() throws Exception {
        uploadFileAndAssertSequenced("/mp3_file.mp3", "/derived/mp3", "org.modeshape.sequencer.mp3.Mp3MetadataSequencer");
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
        uploadFileAndAssertSequenced("/class_file.clazz", "/derived/class", "org.modeshape.sequencer.classfile.ClassFileSequencer");
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
    public void shouldSequenceDelimitedTextFile() throws Exception  {
        uploadFileAndAssertSequenced("/delimited_file.csv", "/derived/text/delimited", "org.modeshape.sequencer.text.DelimitedTextSequencer");
    }

    @Test
    public void shouldSequenceFixedTextFile() throws Exception  {
        uploadFileAndAssertSequenced("/fixed_file.txt", "/derived/text/fixedWidth", "org.modeshape.sequencer.text.FixedWidthTextSequencer");
    }

    @Test
    public void shouldSequenceMsOfficeFile() throws Exception  {
        uploadFileAndAssertSequenced("/msoffice_file.xls", "/derived/msoffice", "org.modeshape.sequencer.msoffice.MSOfficeMetadataSequencer");
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


    private void uploadFileAndAssertSequenced( String fileName, String outputPathPrefix, String expectedSequencerClassConfigured ) throws Exception {
        assertSequencerConfigured(expectedSequencerClassConfigured);

        Session session = repository.login("default");

        ObservationManager observationManager = session.getWorkspace().getObservationManager();
        CountDownLatch latch = new CountDownLatch(1);
        SequencingListener listener = new SequencingListener(latch);
        observationManager.addEventListener(listener, NODE_SEQUENCED, null, true, null, null, false);

        ensureTestRootNodeExists(session);

        String inputNodePath = "/files" + fileName;
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("sequencer" + fileName);
        assertNotNull(resourceAsStream);
        //class files can't be named .class because the container tries to load them
        if (fileName.endsWith(".clazz")) {
            inputNodePath = "/files" + fileName.substring(0, fileName.indexOf(".clazz")) + ".class";
        }
        jcrTools.uploadFile(session, inputNodePath, resourceAsStream);
        session.save();

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        String outputNodePath = listener.getSequencedNodePath();
        assertTrue(outputNodePath.startsWith(outputPathPrefix));
        Node outputNode = session.getNode(outputNodePath);
        outputNode.remove();
        session.getNode(inputNodePath).remove();
        session.save();
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
        assertTrue("The expected sequencer:" + expectedSequencerClassConfigured + " is not configured in the repository", sequencerConfigured);
    }

    private void ensureTestRootNodeExists( Session session ) throws RepositoryException {
        try {
           session.getRootNode().getNode(SEQUENCING_EXPRESSION_INPUT_ROOT);
        } catch (PathNotFoundException e) {
            session.getRootNode().addNode(SEQUENCING_EXPRESSION_INPUT_ROOT).getPath();
            session.save();
        }
    }

    private final class SequencingListener implements EventListener {
        private final CountDownLatch latch;
        private volatile String sequencedNodePath;

        private SequencingListener( CountDownLatch latch ) {
            this.latch = latch;
        }

        @SuppressWarnings( "synthetic-access" )
        @Override
        public void onEvent( EventIterator events ) {
            try {
                Event event = (Event)events.nextEvent();
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
