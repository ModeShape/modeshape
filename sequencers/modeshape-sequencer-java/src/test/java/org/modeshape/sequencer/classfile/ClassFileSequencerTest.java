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
package org.modeshape.sequencer.classfile;

import javax.jcr.Node;
import static junit.framework.Assert.assertNotNull;
import org.junit.Test;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;
import static org.modeshape.sequencer.JavaSequencerHelper.CLASS_FILE_HELPER;
import org.modeshape.sequencer.testdata.MockClass;
import org.modeshape.sequencer.testdata.MockEnum;

/**
 * Unit test for {@link ClassFileSequencer}
 * 
 * @author Horia Chiorean
 */
public class ClassFileSequencerTest extends AbstractSequencerTest {

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

        // expected by sequencer in a different location
        String expectedSequencedPathNewLocation = "classes/enum.class";
        outputNode = getOutputNode(rootNode, expectedSequencedPathNewLocation);
        assertNotNull(outputNode);
        enumNode = outputNode.getNode(packagePath);
        CLASS_FILE_HELPER.assertSequencedMockEnum(enumNode);
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

        // expected by sequencer in a different location
        String expectedSequencedPathNewLocation = "classes/mockclass.class";
        outputNode = getOutputNode(rootNode, expectedSequencedPathNewLocation);
        assertNotNull(outputNode);
        classNode = outputNode.getNode(packagePath);
        CLASS_FILE_HELPER.assertSequencedMockClass(classNode);
    }
}
