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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import org.junit.Test;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;
import org.modeshape.sequencer.classfile.ClassFileSequencerLexicon;
import org.modeshape.sequencer.testdata.ClassType;

public final class InterfaceTypeSequencerTest extends AbstractSequencerTest {

    @Test
    public void shouldSequenceClassTypeFile() throws Exception {
        final String packagePath = ClassType.class.getName().replaceAll("\\.", "/");
        createNodeWithContentFromFile("classtype.java", packagePath + ".java");

        // expected by sequencer in a different location
        final String expectedOutputPath = "java/classtype.java";
        final Node compilationUnitNode = getOutputNode(rootNode, expectedOutputPath);
        assertThat(compilationUnitNode, is(notNullValue()));
        assertThat(compilationUnitNode.isNodeType(ClassFileSequencerLexicon.COMPILATION_UNIT), is(true));
        assertThat(compilationUnitNode.hasProperty(ClassFileSequencerLexicon.SEQUENCED_DATE), is(true));
        assertThat(compilationUnitNode.hasProperty(ClassFileSequencerLexicon.TYPES), is(true));
        assertThat(compilationUnitNode.getProperty(ClassFileSequencerLexicon.TYPES).getValues().length, is(1));
        assertThat(compilationUnitNode.hasNode(ClassFileSequencerLexicon.MESSAGES), is(false));

        { // 2 imports
            assertThat(compilationUnitNode.hasNode(ClassFileSequencerLexicon.IMPORTS), is(true));
            assertThat(compilationUnitNode.getNode(ClassFileSequencerLexicon.IMPORTS).getNodes().getSize(), is(3L));

            final NodeIterator itr = compilationUnitNode.getNode(ClassFileSequencerLexicon.IMPORTS).getNodes();

            { // first import
                final Node firstImport = itr.nextNode();
                assertThat(firstImport.getName(), is("java.io.Serializable"));
                assertThat(firstImport.hasProperty(ClassFileSequencerLexicon.STATIC), is(true));
                assertThat(firstImport.getProperty(ClassFileSequencerLexicon.STATIC).getBoolean(), is(false));
                assertThat(firstImport.hasProperty(ClassFileSequencerLexicon.ON_DEMAND), is(true));
                assertThat(firstImport.getProperty(ClassFileSequencerLexicon.ON_DEMAND).getBoolean(), is(false));
            }

            { // second import
                final Node secondImport = itr.nextNode();
                assertThat(secondImport.getName(), is("java.util.ArrayList"));
                assertThat(secondImport.hasProperty(ClassFileSequencerLexicon.STATIC), is(true));
                assertThat(secondImport.getProperty(ClassFileSequencerLexicon.STATIC).getBoolean(), is(false));
                assertThat(secondImport.hasProperty(ClassFileSequencerLexicon.ON_DEMAND), is(true));
                assertThat(secondImport.getProperty(ClassFileSequencerLexicon.ON_DEMAND).getBoolean(), is(false));
            }

            { // third import
                final Node thirdImport = itr.nextNode();
                assertThat(thirdImport.getName(), is("java.util.HashMap"));
                assertThat(thirdImport.hasProperty(ClassFileSequencerLexicon.STATIC), is(true));
                assertThat(thirdImport.getProperty(ClassFileSequencerLexicon.STATIC).getBoolean(), is(false));
                assertThat(thirdImport.hasProperty(ClassFileSequencerLexicon.ON_DEMAND), is(true));
                assertThat(thirdImport.getProperty(ClassFileSequencerLexicon.ON_DEMAND).getBoolean(), is(false));
            }
        }

        { // comments
            assertThat(compilationUnitNode.hasNode(ClassFileSequencerLexicon.COMMENTS), is(true));
            assertThat(compilationUnitNode.getNode(ClassFileSequencerLexicon.COMMENTS).getNodes().getSize(), is(2L));

            final NodeIterator itr = compilationUnitNode.getNode(ClassFileSequencerLexicon.COMMENTS).getNodes();

            { // file block header
                final Node firstComment = itr.nextNode();
                assertThat(firstComment.getName(), is(ClassFileSequencerLexicon.COMMENT));
                assertThat(firstComment.hasProperty(ClassFileSequencerLexicon.COMMENT_TYPE), is(true));
                assertThat(firstComment.getProperty(ClassFileSequencerLexicon.COMMENT_TYPE).getString(),
                           is(ClassFileSequencerLexicon.CommentType.BLOCK.toString()));
                assertThat(firstComment.hasProperty(ClassFileSequencerLexicon.COMMENT), is(true));
                assertThat(firstComment.getProperty(ClassFileSequencerLexicon.COMMENT).getString().startsWith("/*\n * ModeShape"),
                           is(true));
            }

            { // line comment for field CONSTANT
                final Node secondComment = itr.nextNode();
                assertThat(secondComment.getName(), is(ClassFileSequencerLexicon.COMMENT));
                assertThat(secondComment.hasProperty(ClassFileSequencerLexicon.COMMENT_TYPE), is(true));
                assertThat(secondComment.getProperty(ClassFileSequencerLexicon.COMMENT_TYPE).getString(),
                           is(ClassFileSequencerLexicon.CommentType.LINE.toString()));
                assertThat(secondComment.hasProperty(ClassFileSequencerLexicon.COMMENT), is(true));
                assertThat(secondComment.getProperty(ClassFileSequencerLexicon.COMMENT).getString(), is("// a line comment"));
            }
        }
    }

}
