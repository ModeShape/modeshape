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
package org.modeshape.test.integration.sequencer;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.io.File;
import javax.jcr.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JavaSequencerIntegrationTest extends AbstractSequencerTest {

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.test.integration.sequencer.AbstractSequencerTest#getResourcePathToConfigurationFile()
     */
    @Override
    protected String getResourcePathToConfigurationFile() {
        return "config/configRepositoryForJavaSequencing.xml";
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.test.integration.AbstractSingleUseModeShapeTest#getRepositoryName()
     */
    @Override
    protected String getRepositoryName() {
        return "Content";
    }

    @Before
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        session.getWorkspace().getNamespaceRegistry().registerNamespace("java", "http://www.modeshape.org/java/1.0");
        session.getWorkspace().getNamespaceRegistry().registerNamespace("class",
                                                                        "http://www.modeshape.org/sequencer/javaclass/1.0");
    }

    @After
    @Override
    public void afterEach() throws Exception {
        super.afterEach();
    }

    @Test
    public void shouldSequenceJavaSourceFile() throws Exception {
        // print = true;
        File file = new File("src/test/java/org/modeshape/test/integration/ClusteringTest.java");
        assertThat(file.exists(), is(true));
        uploadFile(file.toURI().toURL(), "/files/");
        waitUntilSequencedNodesIs(1);
        Thread.sleep(200); // wait a bit while the new content is indexed
        // printSubgraph(assertNode("/"));

        // Find the sequenced node ...
        String path = "/sequenced/java/ClusteringTest.java";
        Node java = assertNode(path, "nt:unstructured");
        printSubgraph(java);

        assertNode(path + "/ClusteringTest", "class:class", "mode:derived");
        assertNode(path + "/ClusteringTest/class:constructors", "class:constructors");
        assertNode(path + "/ClusteringTest/class:methods", "class:methods");
        assertNode(path + "/ClusteringTest/class:methods/beforeAll()", "class:method");
        assertNode(path + "/ClusteringTest/class:methods/beforeAll()/class:annotations", "class:annotations");
        assertNode(path + "/ClusteringTest/class:methods/beforeAll()/class:annotations/BeforeClass", "class:annotation");
        assertNode(path + "/ClusteringTest/class:methods/afterAll()", "class:method");
        assertNode(path + "/ClusteringTest/class:methods/afterAll()/class:annotations", "class:annotations");
        assertNode(path + "/ClusteringTest/class:methods/afterAll()/class:annotations/AfterClass", "class:annotation");
        assertNode(path + "/ClusteringTest/class:methods/shouldAllowMultipleEnginesToAccessSameDatabase()", "class:method");
        assertNode(path + "/ClusteringTest/class:methods/shouldAllowMultipleEnginesToAccessSameDatabase()/class:annotations",
                   "class:annotations");
        assertNode(path + "/ClusteringTest/class:methods/shouldAllowMultipleEnginesToAccessSameDatabase()/class:annotations/Test",
                   "class:annotation");
        assertNode(path + "/ClusteringTest/class:methods/shouldReceiveNotificationsFromAllEnginesWhenChangingContentInOne()",
                   "class:method");
        assertNode(path
                   + "/ClusteringTest/class:methods/shouldReceiveNotificationsFromAllEnginesWhenChangingContentInOne()/class:annotations",
                   "class:annotations");
        assertNode(path
                   + "/ClusteringTest/class:methods/shouldReceiveNotificationsFromAllEnginesWhenChangingContentInOne()/class:annotations/Test",
                   "class:annotation");
        // etc.

        printQuery("SELECT * FROM [class:class]", 1);
        printQuery("SELECT * FROM [class:constructors]", 1);
        printQuery("SELECT * FROM [class:methods]", 1);
        printQuery("SELECT * FROM [class:method]", 12);
        printQuery("SELECT * FROM [class:annotations]", 18);
        printQuery("SELECT * FROM [class:annotation]", 9);
    }

    protected void assertSequenceable( Class<?> javaClass ) throws Exception {
        String className = javaClass.getCanonicalName();
        String sourceName = className.replaceAll("[.]", "/") + ".java";
        String typeName = javaClass.getSimpleName();
        String packageName = javaClass.getPackage().getName().replaceAll("[.]", "/");
        File file = new File("src/test/java/" + sourceName);
        assertThat(file.exists(), is(true));
        uploadFile(file.toURI().toURL(), "/files/" + packageName);
        waitUntilSequencedNodesIs(1);
        Thread.sleep(200); // wait a bit while the new content is indexed
        // printSubgraph(assertNode("/"));

        // Find the sequenced node ...
        String path = "/sequenced/java/" + sourceName;
        Node java = assertNode(path, "nt:unstructured");
        printSubgraph(java);

        assertNode(path + "/" + typeName, "class:class", "mode:derived");
        assertNode(path + "/" + typeName + "/class:constructors", "class:constructors");
        assertNode(path + "/" + typeName + "/class:methods", "class:methods");
        // etc.

        printQuery("SELECT * FROM [class:class]", 1);
        printQuery("SELECT * FROM [class:constructors]", 1);
        printQuery("SELECT * FROM [class:methods]", 1);
        // printQuery("SELECT * FROM [class:method]", 12);
        // printQuery("SELECT * FROM [class:annotations]", 18);
        // printQuery("SELECT * FROM [class:annotation]", 9);
    }

    @Test
    public void shouldSequenceJavaSource1FileBelowSequencedPath() throws Exception {
        assertSequenceable(AbstractSequencerTest.class);
    }

    @Test
    public void shouldSequenceJavaSource2FileBelowSequencedPath() throws Exception {
        assertSequenceable(CndSequencerIntegrationTest.class);
    }

    @Test
    public void shouldSequenceJavaSource3FileBelowSequencedPath() throws Exception {
        assertSequenceable(JavaSequencerIntegrationTest.class);
    }

    @Test
    public void shouldSequenceJavaSource4FileBelowSequencedPath() throws Exception {
        assertSequenceable(TeiidSequencerIntegrationTest.class);
    }

    @Test
    public void shouldSequenceJavaSource5FileBelowSequencedPath() throws Exception {
        assertSequenceable(XmlSequencerIntegrationTest.class);
    }

    @Test
    public void shouldSequenceJavaSource6FileBelowSequencedPath() throws Exception {
        assertSequenceable(ZipSequencerIntegrationTest.class);
    }

    @Test
    public void shouldSequenceJavaSourceFileBelowSequencedPath2() throws Exception {
        // print = true;
        File file = new File("src/test/java/org/modeshape/test/integration/sequencer/SequencerTest.java");
        assertThat(file.exists(), is(true));
        uploadFile(file.toURI().toURL(), "/files/org/modeshape/test/integration/sequencer");
        waitUntilSequencedNodesIs(1);
        Thread.sleep(200); // wait a bit while the new content is indexed
        // printSubgraph(assertNode("/"));

        // Find the sequenced node ...
        String path = "/sequenced/java/org/modeshape/test/integration/sequencer/SequencerTest.java";
        Node java = assertNode(path, "nt:unstructured");
        printSubgraph(java);

        assertNode(path + "/SequencerTest", "class:class", "mode:derived");
        assertNode(path + "/SequencerTest/class:constructors", "class:constructors");
        assertNode(path + "/SequencerTest/class:methods", "class:methods");
        // etc.

        printQuery("SELECT * FROM [class:class]", 1);
        printQuery("SELECT * FROM [class:constructors]", 1);
        printQuery("SELECT * FROM [class:methods]", 1);
        // printQuery("SELECT * FROM [class:method]", 12);
        // printQuery("SELECT * FROM [class:annotations]", 18);
        // printQuery("SELECT * FROM [class:annotation]", 9);
    }
}
