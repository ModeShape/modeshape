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
package org.modeshape.sequencer.javafile;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.modeshape.sequencer.JavaSequencerHelper.JAVA_FILE_HELPER;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.IMPORTS;
import javax.jcr.Node;
import org.junit.Test;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;
import org.modeshape.sequencer.classfile.ClassFileSequencerLexicon;
import org.modeshape.sequencer.testdata.MockClass;
import org.modeshape.sequencer.testdata.MockEnum;

/**
 * Unit test for {@link JavaFileSequencer}
 *
 * @author Horia Chiorean
 */
public class JavaFileSequencerTest extends AbstractSequencerTest {

    private void assertClassImports( final Node compilationUnitNode ) throws Exception {
        assertThat(compilationUnitNode.hasNode(IMPORTS), is(true));

        final Node importsNode = compilationUnitNode.getNode(IMPORTS);
        assertThat(importsNode.getNodes().getSize(), is(3L));

        final Node serializableImport = importsNode.getNode("java.io.Serializable");
        assertThat(serializableImport.getPrimaryNodeType().getName(), is(ClassFileSequencerLexicon.IMPORT));

        final Node arrayListImport = importsNode.getNode("java.util.ArrayList");
        assertThat(arrayListImport.getPrimaryNodeType().getName(), is(ClassFileSequencerLexicon.IMPORT));

        final Node listImport = importsNode.getNode("java.util.List");
        assertThat(listImport.getPrimaryNodeType().getName(), is(ClassFileSequencerLexicon.IMPORT));
    }

    private void assertEnumImports( final Node compilationUnitNode ) throws Exception {
        assertThat(compilationUnitNode.hasNode(IMPORTS), is(true));

        final Node importsNode = compilationUnitNode.getNode(IMPORTS);
        assertThat(importsNode.getNodes().getSize(), is(2L));

        final Node randomImport = importsNode.getNode("java.util.Random");
        assertThat(randomImport.getPrimaryNodeType().getName(), is(ClassFileSequencerLexicon.IMPORT));

        final Node dateFormatImport = importsNode.getNode("java.text.DateFormat");
        assertThat(dateFormatImport.getPrimaryNodeType().getName(), is(ClassFileSequencerLexicon.IMPORT));
    }

    @Test
    public void sequenceEnum() throws Exception {
        String packagePath = MockEnum.class.getName().replaceAll("\\.", "/");
        createNodeWithContentFromFile("enum.java", packagePath + ".java");

        // expected by sequencer in a different location
        String expectedOutputPath = "java/enum.java";
        Node outputNode = getOutputNode(rootNode, expectedOutputPath);
        assertNotNull(outputNode);
        Node enumNode = outputNode.getNode(packagePath);
        JAVA_FILE_HELPER.assertSequencedMockEnum(enumNode);

        assertEnumImports(outputNode);
    }

    @Test
    public void sequenceJavaFile() throws Exception {
        String packagePath = MockClass.class.getName().replaceAll("\\.", "/");
        createNodeWithContentFromFile("mockclass.java", packagePath + ".java");

        // expected by sequencer in a different location
        String expectedOutputPath = "java/mockclass.java";
        Node outputNode = getOutputNode(rootNode, expectedOutputPath);
        assertNotNull(outputNode);
        Node javaNode = outputNode.getNode(packagePath);
        JAVA_FILE_HELPER.assertSequencedMockClass(javaNode);

        assertClassImports(outputNode);
    }

}
