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
 * Lesser General Public License for more details
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org
 */
package org.modeshape.sequencer.javafile;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.modeshape.sequencer.JavaSequencerHelper.JAVA_FILE_HELPER;
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
 * Unit test for {@link JavaFileSequencer}
 * 
 * @author Horia Chiorean
 */
public class JavaFileSequencerTest extends AbstractSequencerTest {

    private void assertClassImports( final Node classNode ) throws Exception {
        assertThat(classNode.hasProperty(IMPORTS), is(true));

        final Value[] values = classNode.getProperty(IMPORTS).getValues();
        assertThat(values.length, is(2));

        final List<String> items = new ArrayList<String>(3);
        items.add(values[0].getString());
        items.add(values[1].getString());
        assertThat(items, hasItems("java.io.Serializable", "java.util"));
    }

    private void assertEnumImports( final Node classNode ) throws Exception {
        assertThat(classNode.hasProperty(IMPORTS), is(true));

        final Value[] values = classNode.getProperty(IMPORTS).getValues();
        assertThat(values.length, is(2));

        final List<String> items = new ArrayList<String>(2);
        items.add(values[0].getString());
        items.add(values[1].getString());
        assertThat(items, hasItems("java.util.Random", "java.text"));
    }

    @Test
    public void sequenceEnum() throws Exception {
        String packagePath = MockEnum.class.getName().replaceAll("\\.", "/");
        createNodeWithContentFromFile("enum.java", packagePath + ".java");

        //expected by sequencer in a different location
        String expectedOutputPath = "java/enum.java";
        Node outputNode = getOutputNode(rootNode, expectedOutputPath);
        assertNotNull(outputNode);
        Node enumNode = outputNode.getNode(packagePath);
        JAVA_FILE_HELPER.assertSequencedMockEnum(enumNode);
        assertEnumImports(enumNode);
    }

    @Test
    public void sequenceJavaFile() throws Exception {
        String packagePath = MockClass.class.getName().replaceAll("\\.", "/");
        createNodeWithContentFromFile("mockclass.java", packagePath + ".java");

        //expected by sequencer in a different location
        String expectedOutputPath = "java/mockclass.java";
        Node outputNode = getOutputNode(rootNode, expectedOutputPath);
        assertNotNull(outputNode);
        Node javaNode = outputNode.getNode(packagePath);
        JAVA_FILE_HELPER.assertSequencedMockClass(javaNode);
        assertClassImports(javaNode);
    }

}
