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
package org.modeshape.sequencer.classfile;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.modeshape.sequencer.JavaSequencerHelper.CLASS_FILE_HELPER;
import static org.modeshape.sequencer.classfile.ClassFileSequencerLexicon.IMPORTS;
import java.util.ArrayList;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.Value;
import org.junit.Test;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;
import org.modeshape.sequencer.testdata.MockClass;
import org.modeshape.sequencer.testdata.MockEnum;

/**
 * Unit test for {@link ClassFileSequencer}
 * 
 * @author Horia Chiorean
 */
public class ClassFileSequencerTest extends AbstractSequencerTest {

    private void assertClassImports( final Node classNode ) throws Exception {
        assertThat(classNode.hasProperty(IMPORTS), is(true));

        final Value[] values = classNode.getProperty(IMPORTS).getValues();
        assertThat(values.length, is(3));

        final List<String> items = new ArrayList<String>(3);
        items.add(values[0].getString());
        items.add(values[1].getString());
        items.add(values[2].getString());
        assertThat(items, hasItems("java.io.Serializable", "java.util.ArrayList", "java.util.List"));
    }

    private void assertEnumImports( final Node classNode ) throws Exception {
        assertThat(classNode.hasProperty(IMPORTS), is(true));

        final Value[] values = classNode.getProperty(IMPORTS).getValues();
        assertThat(values.length, is(2));

        final List<String> items = new ArrayList<String>(2);
        items.add(values[0].getString());
        items.add(values[1].getString());
        assertThat(items, hasItems("java.util.Random", "java.text.DateFormat"));
    }

    @Test
    public void sequenceEnum() throws Exception {
        String packagePath = MockEnum.class.getName().replaceAll("\\.", "/");
        createNodeWithContentFromFile("enum.class", packagePath + ".class");

        // expected by sequencer in the same location
        String expectedSequencedPathSameLocation = "enum.class/org";
        Node outputNode = getOutputNode(rootNode, expectedSequencedPathSameLocation);
        assertNotNull(outputNode);
        Node enumNode = outputNode.getNode(packagePath.substring(packagePath.indexOf("/") + 1));
        CLASS_FILE_HELPER.assertSequencedMockEnum(enumNode);
        assertEnumImports(enumNode);

        // expected by sequencer in a different location
        String expectedSequencedPathNewLocation = "classes/enum.class";
        outputNode = getOutputNode(rootNode, expectedSequencedPathNewLocation);
        assertNotNull(outputNode);
        enumNode = outputNode.getNode(packagePath);
        CLASS_FILE_HELPER.assertSequencedMockEnum(enumNode);
        assertEnumImports(enumNode);
    }

    @Test
    public void sequenceClass() throws Exception {
        String packagePath = MockClass.class.getName().replaceAll("\\.", "/");
        createNodeWithContentFromFile("mockclass.class", packagePath + ".class");

        // expected by sequencer in the same location
        String expectedSequencedPathSameLocation = "mockclass.class/org";
        Node outputNode = getOutputNode(rootNode, expectedSequencedPathSameLocation);
        assertNotNull(outputNode);
        Node classNode = outputNode.getNode(packagePath.substring(packagePath.indexOf("/") + 1));
        CLASS_FILE_HELPER.assertSequencedMockClass(classNode);
        assertClassImports(classNode);

        // expected by sequencer in a different location
        String expectedSequencedPathNewLocation = "classes/mockclass.class";
        outputNode = getOutputNode(rootNode, expectedSequencedPathNewLocation);
        assertNotNull(outputNode);
        classNode = outputNode.getNode(packagePath);
        CLASS_FILE_HELPER.assertSequencedMockClass(classNode);
        assertClassImports(classNode);
    }

}
