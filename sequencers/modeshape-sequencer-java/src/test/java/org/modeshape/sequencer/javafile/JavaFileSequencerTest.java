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

import javax.jcr.Node;
import static junit.framework.Assert.assertNotNull;
import org.junit.Test;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;
import static org.modeshape.sequencer.JavaSequencerHelper.JAVA_FILE_HELPER;
import org.modeshape.sequencer.testdata.MockClass;
import org.modeshape.sequencer.testdata.MockEnum;


/**
 * Unit test for {@link JavaFileSequencer}
 * 
 * @author Horia Chiorean
 */
public class JavaFileSequencerTest extends AbstractSequencerTest {

    @Test
    public void sequenceEnum() throws Exception {
        String packagePath = MockEnum.class.getName().replaceAll("\\.", "/");
        createNodeWithContentFromFile("enum.java", packagePath + ".java");

        //expected by sequencer in a different location
        String expectedOutputPath = "java/enum.java";
        Node sequencedNode = getSequencedNode(rootNode, expectedOutputPath);
        assertNotNull(sequencedNode);
        Node enumNode = sequencedNode.getNode(packagePath);
        JAVA_FILE_HELPER.assertSequencedMockEnum(enumNode);
    }

    @Test
    public void sequenceJavaFile() throws Exception {
        String packagePath = MockClass.class.getName().replaceAll("\\.", "/");
        createNodeWithContentFromFile("mockclass.java", packagePath + ".java");

        //expected by sequencer in a different location
        String expectedOutputPath = "java/mockclass.java";
        Node sequencedNode = getSequencedNode(rootNode, expectedOutputPath);
        assertNotNull(sequencedNode);
        Node javaNode = sequencedNode.getNode(packagePath);
        JAVA_FILE_HELPER.assertSequencedMockClass(javaNode);
    }
}
