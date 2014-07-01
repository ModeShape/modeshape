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
import org.modeshape.sequencer.classfile.metadata.Visibility;
import org.modeshape.sequencer.testdata.AnnotationType;

public final class AnnotationTypeSequencerTest extends AbstractSequencerTest {

    @Test
    public void shouldSequenceAnnotationTypeFile() throws Exception {
        final String packagePath = AnnotationType.class.getName().replaceAll("\\.", "/");
        createNodeWithContentFromFile("annotationtype.java", packagePath + ".java");

        // expected by sequencer in a different location
        final String expectedOutputPath = "java/annotationtype.java";
        final Node compilationUnitNode = getOutputNode(rootNode, expectedOutputPath);
        assertThat(compilationUnitNode, is(notNullValue()));
        assertThat(compilationUnitNode.isNodeType(ClassFileSequencerLexicon.COMPILATION_UNIT), is(true));
        assertThat(compilationUnitNode.hasProperty(ClassFileSequencerLexicon.SEQUENCED_DATE), is(true));
        assertThat(compilationUnitNode.hasProperty(ClassFileSequencerLexicon.TYPES), is(true));
        // assertThat(compilationUnitNode.getProperty(ClassFileSequencerLexicon.TYPES).getValues().length, is(2));
        assertThat(compilationUnitNode.hasNode(ClassFileSequencerLexicon.MESSAGES), is(false));

        { // 2 imports
            assertThat(compilationUnitNode.hasNode(ClassFileSequencerLexicon.IMPORTS), is(true));
            assertThat(compilationUnitNode.getNode(ClassFileSequencerLexicon.IMPORTS).getNodes().getSize(), is(2L));

            final NodeIterator itr = compilationUnitNode.getNode(ClassFileSequencerLexicon.IMPORTS).getNodes();

            { // first import
                final Node firstImport = itr.nextNode();
                assertThat(firstImport.getName(), is("java.lang.annotation.ElementType"));
                assertThat(firstImport.hasProperty(ClassFileSequencerLexicon.STATIC), is(true));
                assertThat(firstImport.getProperty(ClassFileSequencerLexicon.STATIC).getBoolean(), is(false));
                assertThat(firstImport.hasProperty(ClassFileSequencerLexicon.ON_DEMAND), is(true));
                assertThat(firstImport.getProperty(ClassFileSequencerLexicon.ON_DEMAND).getBoolean(), is(false));
            }

            { // second import
                final Node secondImport = itr.nextNode();
                assertThat(secondImport.getName(), is("java.lang.annotation.Target"));
                assertThat(secondImport.hasProperty(ClassFileSequencerLexicon.STATIC), is(true));
                assertThat(secondImport.getProperty(ClassFileSequencerLexicon.STATIC).getBoolean(), is(false));
                assertThat(secondImport.hasProperty(ClassFileSequencerLexicon.ON_DEMAND), is(true));
                assertThat(secondImport.getProperty(ClassFileSequencerLexicon.ON_DEMAND).getBoolean(), is(false));
            }
        }

        { // comments
            assertThat(compilationUnitNode.hasNode(ClassFileSequencerLexicon.COMMENTS), is(true));
            assertThat(compilationUnitNode.getNode(ClassFileSequencerLexicon.COMMENTS).getNodes().getSize(), is(2L));

            final NodeIterator itr = compilationUnitNode.getNode(ClassFileSequencerLexicon.COMMENTS).getNodes();

            { // file header
                final Node firstComment = itr.nextNode();
                assertThat(firstComment.getName(), is(ClassFileSequencerLexicon.COMMENT));
                assertThat(firstComment.hasProperty(ClassFileSequencerLexicon.COMMENT_TYPE), is(true));
                assertThat(firstComment.getProperty(ClassFileSequencerLexicon.COMMENT_TYPE).getString(),
                           is(ClassFileSequencerLexicon.CommentType.BLOCK.toString()));
                assertThat(firstComment.hasProperty(ClassFileSequencerLexicon.COMMENT), is(true));
                assertThat(firstComment.getProperty(ClassFileSequencerLexicon.COMMENT).getString().startsWith("/*\n * ModeShape"),
                           is(true));
            }

            { // line comment
                final Node lineComment = itr.nextNode();
                assertThat(lineComment.getName(), is(ClassFileSequencerLexicon.COMMENT));
                assertThat(lineComment.hasProperty(ClassFileSequencerLexicon.COMMENT_TYPE), is(true));
                assertThat(lineComment.getProperty(ClassFileSequencerLexicon.COMMENT_TYPE).getString(),
                           is(ClassFileSequencerLexicon.CommentType.LINE.toString()));
                assertThat(lineComment.hasProperty(ClassFileSequencerLexicon.COMMENT), is(true));
                assertThat(lineComment.getProperty(ClassFileSequencerLexicon.COMMENT).getString(), is("// an annotation type"));
            }
        }

        { // annotation type
            final Node annotationTypeNode = compilationUnitNode.getNode(packagePath);
            assertThat(annotationTypeNode.getName(), is("AnnotationType"));
            assertThat(annotationTypeNode.getProperty(ClassFileSequencerLexicon.NAME).getString(),
                       is(annotationTypeNode.getName()));
            assertThat(annotationTypeNode.hasProperty(ClassFileSequencerLexicon.SEQUENCED_DATE), is(true));
            assertThat(annotationTypeNode.getProperty(ClassFileSequencerLexicon.VISIBILITY).getString(),
                       is(Visibility.PUBLIC.getDescription()));

            assertThat(annotationTypeNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(true));
            assertThat(annotationTypeNode.getNode(ClassFileSequencerLexicon.ANNOTATIONS).getNodes().getSize(), is(1L));
            annotationTypeNode.getNode(ClassFileSequencerLexicon.ANNOTATIONS).getNode("Target"); // throws exception if not found

            assertThat(annotationTypeNode.hasNode(ClassFileSequencerLexicon.JAVADOC), is(true));
            final Node javadocNode = annotationTypeNode.getNode(ClassFileSequencerLexicon.JAVADOC);
            final String comment = "This is an annotation type test class.";
            assertThat(javadocNode.getProperty(ClassFileSequencerLexicon.COMMENT).getString().contains(comment), is(true));

            assertThat(annotationTypeNode.hasNode(ClassFileSequencerLexicon.FIELDS), is(true));
            assertThat(annotationTypeNode.getNode(ClassFileSequencerLexicon.FIELDS).getNodes().getSize(), is(1L));
            annotationTypeNode.getNode(ClassFileSequencerLexicon.FIELDS).getNode("choice"); // throws exception if not found

            assertThat(annotationTypeNode.hasNode(ClassFileSequencerLexicon.NESTED_TYPES), is(true));
            assertThat(annotationTypeNode.getNode(ClassFileSequencerLexicon.NESTED_TYPES).getNodes().getSize(), is(2L));
            annotationTypeNode.getNode(ClassFileSequencerLexicon.NESTED_TYPES).getNode("Decision");
            annotationTypeNode.getNode(ClassFileSequencerLexicon.NESTED_TYPES).getNode("Status"); // throws exception if not found

            { // annotation type members
                assertThat(annotationTypeNode.hasNode(ClassFileSequencerLexicon.ANNOTATION_TYPE_MEMBERS), is(true));
                assertThat(annotationTypeNode.getNode(ClassFileSequencerLexicon.ANNOTATION_TYPE_MEMBERS).getNodes().getSize(),
                           is(3L));
                final NodeIterator itr = annotationTypeNode.getNode(ClassFileSequencerLexicon.ANNOTATION_TYPE_MEMBERS).getNodes();

                { // showStopper
                    final Node memberNode = itr.nextNode();
                    assertThat(memberNode.getPrimaryNodeType().getName(), is(ClassFileSequencerLexicon.ANNOTATION_TYPE_MEMBER));
                    assertThat(memberNode.getProperty(ClassFileSequencerLexicon.NAME).getString(), is("showStopper"));
                    assertThat(memberNode.getProperty(ClassFileSequencerLexicon.ABSTRACT).getBoolean(), is(true));
                    assertThat(memberNode.getProperty(ClassFileSequencerLexicon.VISIBILITY).getString(), is("package"));
                    assertThat(memberNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(true));
                    assertThat(memberNode.hasNode(ClassFileSequencerLexicon.JAVADOC), is(false));
                    assertThat(memberNode.hasNode(ClassFileSequencerLexicon.DEFAULT), is(false));
                }

                { // status
                    final Node memberNode = itr.nextNode();
                    assertThat(memberNode.getPrimaryNodeType().getName(), is(ClassFileSequencerLexicon.ANNOTATION_TYPE_MEMBER));
                    assertThat(memberNode.getProperty(ClassFileSequencerLexicon.NAME).getString(), is("status"));
                    assertThat(memberNode.getProperty(ClassFileSequencerLexicon.ABSTRACT).getBoolean(), is(false));
                    assertThat(memberNode.getProperty(ClassFileSequencerLexicon.VISIBILITY).getString(), is("public"));
                    assertThat(memberNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(false));
                    assertThat(memberNode.hasNode(ClassFileSequencerLexicon.JAVADOC), is(true));

                    assertThat(memberNode.hasNode(ClassFileSequencerLexicon.DEFAULT), is(true));
                    final Node defaultNode = memberNode.getNode(ClassFileSequencerLexicon.DEFAULT);
                    assertThat(defaultNode.getPrimaryNodeType().getName(), is(ClassFileSequencerLexicon.EXPRESSION));
                    assertThat(defaultNode.getProperty(ClassFileSequencerLexicon.CONTENT).getString(), is("Status.UNCONFIRMED"));
                }

                { // ref
                    final Node memberNode = itr.nextNode();
                    assertThat(memberNode.getPrimaryNodeType().getName(), is(ClassFileSequencerLexicon.ANNOTATION_TYPE_MEMBER));
                    assertThat(memberNode.getProperty(ClassFileSequencerLexicon.NAME).getString(), is("ref"));
                    assertThat(memberNode.getProperty(ClassFileSequencerLexicon.ABSTRACT).getBoolean(), is(false));
                    assertThat(memberNode.getProperty(ClassFileSequencerLexicon.VISIBILITY).getString(), is("package"));
                    assertThat(memberNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(false));
                    assertThat(memberNode.hasNode(ClassFileSequencerLexicon.JAVADOC), is(false));

                    assertThat(memberNode.hasNode(ClassFileSequencerLexicon.DEFAULT), is(true));
                    final Node defaultNode = memberNode.getNode(ClassFileSequencerLexicon.DEFAULT);
                    assertThat(defaultNode.getPrimaryNodeType().getName(), is(ClassFileSequencerLexicon.EXPRESSION));
                    assertThat(defaultNode.getProperty(ClassFileSequencerLexicon.CONTENT).getString(), is("@Reference"));
                }
            }
        }
    }

}
