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

import static org.junit.Assert.assertNotNull;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;
import org.modeshape.sequencer.classfile.ClassFileSequencerLexicon;
import org.modeshape.sequencer.classfile.metadata.Visibility;
import org.modeshape.sequencer.testdata.ClassType;

public final class ClassTypeSequencerTest extends AbstractSequencerTest {

    @Ignore
    @Test
    public void shouldTestFixForMode2272() throws Exception {
        final int count = 10;
        jcrSession().getRootNode().addNode("java"); // prevent SNS
        jcrSession().save();

        for (int i = 0; i < count; ++i) {
            createNodeWithContentFromFile("myclassannotation" + i + ".java", "org/acme/annotation/MyClassAnnotation.java");
            createNodeWithContentFromFile("mypackageannotation" + i + ".java", "org/acme/annotation/MyPackageAnnotation.java");
            createNodeWithContentFromFile("mysource" + i + ".java", "org/acme/MySource.java");
        }

        for (int i = 0; i < count; ++i) {
            String relativePath = "java/" + "myclassannotation" + i + ".java";
            Node outputNode = getOutputNode(rootNode, relativePath);
            assertNotNull("Failed to get " + relativePath, outputNode);

            relativePath = "java/" + "mypackageannotation" + i + ".java";
            outputNode = getOutputNode(rootNode, relativePath);
            assertNotNull("Failed to get " + relativePath, outputNode);

            relativePath = "java/" + "mysource" + i + ".java";
            outputNode = getOutputNode(rootNode, relativePath);
            assertNotNull("Failed to get " + relativePath, outputNode);
        }
    }

    @Test
    public void shouldSequenceClassInDefaultPackage() throws Exception {
        createNodeWithContentFromFile("defaultpackageclass.java", "DefaultPackageClass.java");
        final String expectedOutputPath = "java/defaultpackageclass.java";
        final Node compilationUnitNode = getOutputNode(rootNode, expectedOutputPath);
        assertThat(compilationUnitNode, is(notNullValue()));
    }

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

        { // class type
            final Node classNode = compilationUnitNode.getNode(packagePath);
            assertThat(classNode.getName(), is("ClassType"));
            assertThat(classNode.getProperty(ClassFileSequencerLexicon.NAME).getString(), is(classNode.getName()));
            assertThat(classNode.hasProperty(ClassFileSequencerLexicon.SEQUENCED_DATE), is(true));
            assertThat(classNode.getProperty(ClassFileSequencerLexicon.SUPER_CLASS_NAME).getString(), is("ArrayList<T>"));
            assertThat(classNode.getProperty(ClassFileSequencerLexicon.VISIBILITY).getString(),
                       is(Visibility.PUBLIC.getDescription()));
            assertThat(classNode.getProperty(ClassFileSequencerLexicon.ABSTRACT).getBoolean(), is(true));
            assertThat(classNode.getProperty(ClassFileSequencerLexicon.INTERFACE).getBoolean(), is(false));
            assertThat(classNode.getProperty(ClassFileSequencerLexicon.FINAL).getBoolean(), is(false));
            assertThat(classNode.getProperty(ClassFileSequencerLexicon.STRICT_FP).getBoolean(), is(false));
            assertThat(classNode.getProperty(ClassFileSequencerLexicon.STATIC).getBoolean(), is(false));
            assertThat(classNode.hasProperty(ClassFileSequencerLexicon.IMPORTS), is(false));
            assertThat(classNode.hasProperty(ClassFileSequencerLexicon.INTERFACES), is(true));
            assertThat(classNode.getProperty(ClassFileSequencerLexicon.INTERFACES).getValues().length, is(1));
            assertThat(classNode.getProperty(ClassFileSequencerLexicon.INTERFACES).getValues()[0].getString(), is("Serializable"));
            assertThat(classNode.hasNode(ClassFileSequencerLexicon.CONSTRUCTORS), is(false));

            { // initializer
                assertThat(classNode.hasNode(ClassFileSequencerLexicon.INITIALIZER), is(true));

                final Node initializerNode = classNode.getNode(ClassFileSequencerLexicon.INITIALIZER);
                assertThat(initializerNode.getNodes().getSize(), is(1L));

                final Node statementNode = initializerNode.getNodes().nextNode();
                assertThat(statementNode.getProperty(ClassFileSequencerLexicon.CONTENT).getString(),
                           is("System.out.println(System.currentTimeMillis());\n"));
            }

            { // annotations
                assertThat(classNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(true));
                assertThat(classNode.getNode(ClassFileSequencerLexicon.ANNOTATIONS).getNodes().getSize(), is(1l));

                final Node annotationNode = classNode.getNode(ClassFileSequencerLexicon.ANNOTATIONS).getNodes().nextNode();
                assertThat(annotationNode.getName(), is("SuppressWarnings"));
                assertThat(annotationNode.hasProperty(ClassFileSequencerLexicon.ANNOTATION_TYPE), is(true));
                assertThat(annotationNode.getProperty(ClassFileSequencerLexicon.ANNOTATION_TYPE).getString(),
                           is(ClassFileSequencerLexicon.AnnotationType.SINGLE_MEMBER.toString()));
                assertThat(annotationNode.getNodes().getSize(), is(1L));

                final String memberName = "default";
                assertThat(annotationNode.hasNode(memberName), is(true));
                final Node memberNode = annotationNode.getNode(memberName);
                assertThat(memberNode.getProperty(ClassFileSequencerLexicon.NAME).getString(), is(memberName));
                assertThat(memberNode.hasProperty(ClassFileSequencerLexicon.VALUE), is(true));
                assertThat(memberNode.getProperty(ClassFileSequencerLexicon.VALUE).getString(), is("{\"serial\",\"unused\"}"));
            }

            { // fields
                assertThat(classNode.hasNode(ClassFileSequencerLexicon.FIELDS), is(true));
                assertThat(classNode.getNode(ClassFileSequencerLexicon.FIELDS).getNodes().getSize(), is(4L));

                final Node fieldsNode = classNode.getNode(ClassFileSequencerLexicon.FIELDS);

                { // CONSTANT field
                    final Node fieldNode = fieldsNode.getNode("CONSTANT");
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.NAME).getString(), is(fieldNode.getName()));
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(), is("String"));
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.VISIBILITY).getString(),
                               is(Visibility.PUBLIC.getDescription()));
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.STATIC).getBoolean(), is(true));
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.FINAL).getBoolean(), is(true));
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.TRANSIENT).getBoolean(), is(false));
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.VOLATILE).getBoolean(), is(false));
                    assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(false));

                    assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.INITIALIZER), is(true));
                    final Node initializerNode = fieldNode.getNode(ClassFileSequencerLexicon.INITIALIZER);
                    assertThat(initializerNode.getProperty(ClassFileSequencerLexicon.CONTENT).getString(), is("\"constant\""));

                    assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.TYPE), is(true));
                    final Node fieldTypeNode = fieldNode.getNode(ClassFileSequencerLexicon.TYPE);
                    assertThat(fieldTypeNode.isNodeType(ClassFileSequencerLexicon.SIMPLE_TYPE), is(true));
                    assertThat(fieldTypeNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(), is("String"));

                    assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.JAVADOC), is(true));
                    final Node javadocNode = fieldNode.getNode(ClassFileSequencerLexicon.JAVADOC);
                    final String comment = "A constant";
                    assertThat(javadocNode.getProperty(ClassFileSequencerLexicon.COMMENT).getString().contains(comment), is(true));
                }

                { // twoString field
                    final Node fieldNode = fieldsNode.getNode("twoString");
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.NAME).getString(), is(fieldNode.getName()));
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(), is("Object"));
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.VISIBILITY).getString(),
                               is(Visibility.PUBLIC.getDescription()));
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.STATIC).getBoolean(), is(false));
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.FINAL).getBoolean(), is(false));
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.TRANSIENT).getBoolean(), is(false));
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.VOLATILE).getBoolean(), is(false));
                    assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(false));
                    assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.JAVADOC), is(false));

                    assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.TYPE), is(true));
                    final Node fieldTypeNode = fieldNode.getNode(ClassFileSequencerLexicon.TYPE);
                    assertThat(fieldTypeNode.isNodeType(ClassFileSequencerLexicon.SIMPLE_TYPE), is(true));
                    assertThat(fieldTypeNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(), is("Object"));

                    assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.INITIALIZER), is(true));
                    final Node initializerNode = fieldNode.getNode(ClassFileSequencerLexicon.INITIALIZER);
                    assertThat(initializerNode.getProperty(ClassFileSequencerLexicon.CONTENT).getString().contains("toString"),
                               is(true));
                }

                { // number field
                    final Node fieldNode = fieldsNode.getNode("number");
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.NAME).getString(), is(fieldNode.getName()));
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(), is("Number"));
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.VISIBILITY).getString(), is("package"));
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.STATIC).getBoolean(), is(false));
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.FINAL).getBoolean(), is(false));
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.TRANSIENT).getBoolean(), is(false));
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.VOLATILE).getBoolean(), is(false));
                    assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(false));
                    assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.JAVADOC), is(false));

                    assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.TYPE), is(true));
                    final Node fieldTypeNode = fieldNode.getNode(ClassFileSequencerLexicon.TYPE);
                    assertThat(fieldTypeNode.isNodeType(ClassFileSequencerLexicon.SIMPLE_TYPE), is(true));
                    assertThat(fieldTypeNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(), is("Number"));

                    assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.INITIALIZER), is(true));
                    final Node initializerNode = fieldNode.getNode(ClassFileSequencerLexicon.INITIALIZER);
                    assertThat(initializerNode.getProperty(ClassFileSequencerLexicon.CONTENT).getLong(), is(1L));
                }

                { // t field
                    final Node fieldNode = fieldsNode.getNode("t");
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.NAME).getString(), is(fieldNode.getName()));
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(), is("T"));
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.VISIBILITY).getString(),
                               is(Visibility.PRIVATE.getDescription()));
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.STATIC).getBoolean(), is(false));
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.FINAL).getBoolean(), is(false));
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.TRANSIENT).getBoolean(), is(false));
                    assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.VOLATILE).getBoolean(), is(false));
                    assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(false));
                    assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.JAVADOC), is(false));
                    assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.INITIALIZER), is(false));

                    assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.TYPE), is(true));
                    final Node fieldTypeNode = fieldNode.getNode(ClassFileSequencerLexicon.TYPE);
                    assertThat(fieldTypeNode.isNodeType(ClassFileSequencerLexicon.ARRAY_TYPE), is(true));
                    assertThat(fieldTypeNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(), is("T"));
                    assertThat(fieldTypeNode.getProperty(ClassFileSequencerLexicon.DIMENSIONS).getLong(), is(1L));
                    assertThat(fieldTypeNode.hasNode(ClassFileSequencerLexicon.COMPONENT_TYPE), is(true));
                    final Node componentTypeNode = fieldTypeNode.getNode(ClassFileSequencerLexicon.COMPONENT_TYPE);
                    assertThat(componentTypeNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(), is("T"));
                }
            }

            { // methods
                assertThat(classNode.hasNode(ClassFileSequencerLexicon.METHODS), is(true));
                assertThat(classNode.getNode(ClassFileSequencerLexicon.METHODS).getNodes().getSize(), is(4L));

                final Node methodsNode = classNode.getNode(ClassFileSequencerLexicon.METHODS);

                { // getId method
                    final Node methodNode = methodsNode.getNode("getId");
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.NAME).getString(), is(methodNode.getName()));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.RETURN_TYPE_CLASS_NAME).getString(), is("String"));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.VISIBILITY).getString(), is("package"));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.STATIC).getBoolean(), is(false));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.FINAL).getBoolean(), is(false));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.ABSTRACT).getBoolean(), is(true));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.STRICT_FP).getBoolean(), is(false));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.NATIVE).getBoolean(), is(false));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.SYNCHRONIZED).getBoolean(), is(false));
                    assertThat(methodNode.hasProperty(ClassFileSequencerLexicon.THROWN_EXCEPTIONS), is(false));
                    assertThat(methodNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(false));
                    assertThat(methodNode.hasNode(ClassFileSequencerLexicon.TYPE_PARAMETERS), is(false));
                    assertThat(methodNode.hasNode(ClassFileSequencerLexicon.METHOD_PARAMETERS), is(false));
                    assertThat(methodNode.hasNode(ClassFileSequencerLexicon.BODY), is(false));

                    assertThat(methodNode.hasNode(ClassFileSequencerLexicon.JAVADOC), is(true));
                    final Node javadocNode = methodNode.getNode(ClassFileSequencerLexicon.JAVADOC);
                    final String comment = "@return the identifier (never <code>null</code>)";
                    assertThat(javadocNode.getProperty(ClassFileSequencerLexicon.COMMENT).getString().contains(comment), is(true));

                    assertThat(methodNode.hasNode(ClassFileSequencerLexicon.RETURN_TYPE), is(true));
                    final Node returnTypeNode = methodNode.getNode(ClassFileSequencerLexicon.RETURN_TYPE);
                    assertThat(returnTypeNode.isNodeType(ClassFileSequencerLexicon.SIMPLE_TYPE), is(true));
                    assertThat(returnTypeNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(), is("String"));
                }

                { // set method
                    final Node methodNode = methodsNode.getNode("set");
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.NAME).getString(), is(methodNode.getName()));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.VISIBILITY).getString(), is("public"));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.STATIC).getBoolean(), is(false));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.FINAL).getBoolean(), is(true));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.ABSTRACT).getBoolean(), is(false));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.STRICT_FP).getBoolean(), is(false));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.NATIVE).getBoolean(), is(false));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.SYNCHRONIZED).getBoolean(), is(false));
                    assertThat(methodNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(true));
                    assertThat(methodNode.hasNode(ClassFileSequencerLexicon.TYPE_PARAMETERS), is(false));
                    assertThat(methodNode.hasNode(ClassFileSequencerLexicon.JAVADOC), is(false));
                    assertThat(methodNode.hasNode(ClassFileSequencerLexicon.BODY), is(true));

                    { // parameters
                        assertThat(methodNode.hasNode(ClassFileSequencerLexicon.METHOD_PARAMETERS), is(true));
                        assertThat(methodNode.getNode(ClassFileSequencerLexicon.METHOD_PARAMETERS).getNodes().getSize(), is(1L));
                        final Node paramNode = methodNode.getNode(ClassFileSequencerLexicon.METHOD_PARAMETERS).getNodes().nextNode();
                        assertThat(paramNode.getName(), is("t"));
                        assertThat(paramNode.getPrimaryNodeType().getName(), is(ClassFileSequencerLexicon.PARAMETER));
                        assertThat(paramNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(), is("T"));
                        assertThat(paramNode.getProperty(ClassFileSequencerLexicon.FINAL).getBoolean(), is(false));
                        assertThat(paramNode.getProperty(ClassFileSequencerLexicon.VARARGS).getBoolean(), is(true));
                        assertThat(paramNode.hasNode(ClassFileSequencerLexicon.INITIALIZER), is(false));

                        assertThat(paramNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(true));
                        assertThat(paramNode.getNode(ClassFileSequencerLexicon.ANNOTATIONS).getNodes().getSize(), is(1L));
                        final Node annotationNode = paramNode.getNode(ClassFileSequencerLexicon.ANNOTATIONS).getNodes().nextNode();
                        assertThat(annotationNode.getName(), is("SuppressWarnings"));
                        assertThat(annotationNode.hasProperty(ClassFileSequencerLexicon.ANNOTATION_TYPE), is(true));
                        assertThat(annotationNode.getProperty(ClassFileSequencerLexicon.ANNOTATION_TYPE).getString(),
                                   is(ClassFileSequencerLexicon.AnnotationType.SINGLE_MEMBER.toString()));
                        assertThat(annotationNode.getNodes().getSize(), is(1L));

                        final String memberName = "default";
                        assertThat(annotationNode.hasNode(memberName), is(true));
                        final Node memberNode = annotationNode.getNode(memberName);
                        assertThat(memberNode.getProperty(ClassFileSequencerLexicon.NAME).getString(), is(memberName));
                        assertThat(memberNode.hasProperty(ClassFileSequencerLexicon.VALUE), is(true));
                        assertThat(memberNode.getProperty(ClassFileSequencerLexicon.VALUE).getString(), is("unchecked"));
                    }

                    { // thrown exceptions
                        assertThat(methodNode.hasProperty(ClassFileSequencerLexicon.THROWN_EXCEPTIONS), is(true));
                        assertThat(methodNode.getProperty(ClassFileSequencerLexicon.THROWN_EXCEPTIONS).getValues().length, is(1));
                        assertThat(methodNode.getProperty(ClassFileSequencerLexicon.THROWN_EXCEPTIONS).getValues()[0].getString(),
                                   is("Exception"));
                    }

                    { // return type
                        assertThat(methodNode.getProperty(ClassFileSequencerLexicon.RETURN_TYPE_CLASS_NAME).getString(),
                                   is("void"));
                        assertThat(methodNode.hasNode(ClassFileSequencerLexicon.RETURN_TYPE), is(true));
                        final Node returnTypeNode = methodNode.getNode(ClassFileSequencerLexicon.RETURN_TYPE);
                        assertThat(returnTypeNode.isNodeType(ClassFileSequencerLexicon.PRIMITIVE_TYPE), is(true));
                        assertThat(returnTypeNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(), is("void"));
                    }
                }

                { // get method
                    final Node methodNode = methodsNode.getNode("get");
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.NAME).getString(), is(methodNode.getName()));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.RETURN_TYPE_CLASS_NAME).getString(), is("T"));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.VISIBILITY).getString(), is("public"));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.STATIC).getBoolean(), is(false));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.FINAL).getBoolean(), is(false));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.ABSTRACT).getBoolean(), is(false));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.STRICT_FP).getBoolean(), is(false));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.NATIVE).getBoolean(), is(false));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.SYNCHRONIZED).getBoolean(), is(true));
                    assertThat(methodNode.hasProperty(ClassFileSequencerLexicon.THROWN_EXCEPTIONS), is(false));
                    assertThat(methodNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(false));
                    assertThat(methodNode.hasNode(ClassFileSequencerLexicon.TYPE_PARAMETERS), is(false));
                    assertThat(methodNode.hasNode(ClassFileSequencerLexicon.METHOD_PARAMETERS), is(false));
                    assertThat(methodNode.hasNode(ClassFileSequencerLexicon.JAVADOC), is(false));

                    assertThat(methodNode.hasNode(ClassFileSequencerLexicon.RETURN_TYPE), is(true));
                    final Node returnTypeNode = methodNode.getNode(ClassFileSequencerLexicon.RETURN_TYPE);
                    assertThat(returnTypeNode.isNodeType(ClassFileSequencerLexicon.ARRAY_TYPE), is(true));
                    assertThat(returnTypeNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(), is("T"));
                    assertThat(returnTypeNode.getProperty(ClassFileSequencerLexicon.DIMENSIONS).getLong(), is(1L));
                    assertThat(returnTypeNode.hasNode(ClassFileSequencerLexicon.COMPONENT_TYPE), is(true));
                    final Node componentTypeNode = returnTypeNode.getNode(ClassFileSequencerLexicon.COMPONENT_TYPE);
                    assertThat(componentTypeNode.isNodeType(ClassFileSequencerLexicon.TYPE), is(true));
                    assertThat(componentTypeNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(), is("T"));

                    assertThat(methodNode.hasNode(ClassFileSequencerLexicon.BODY), is(true));
                    assertThat(methodNode.getNode(ClassFileSequencerLexicon.BODY).getNodes().getSize(), is(1L));

                    final Node statementNode = methodNode.getNode(ClassFileSequencerLexicon.BODY).getNodes().nextNode();
                    assertThat(statementNode.getName(), is(ClassFileSequencerLexicon.STATEMENT));
                    assertThat(statementNode.getProperty(ClassFileSequencerLexicon.CONTENT).getString(), is("return this.t;\n"));
                }

                { // shutdown method
                    final Node methodNode = methodsNode.getNode("shutdown");
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.NAME).getString(), is(methodNode.getName()));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.VISIBILITY).getString(), is("public"));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.STATIC).getBoolean(), is(false));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.FINAL).getBoolean(), is(false));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.ABSTRACT).getBoolean(), is(false));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.STRICT_FP).getBoolean(), is(false));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.NATIVE).getBoolean(), is(false));
                    assertThat(methodNode.getProperty(ClassFileSequencerLexicon.SYNCHRONIZED).getBoolean(), is(false));
                    assertThat(methodNode.hasProperty(ClassFileSequencerLexicon.THROWN_EXCEPTIONS), is(false));
                    assertThat(methodNode.hasNode(ClassFileSequencerLexicon.BODY), is(true));

                    assertThat(methodNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(true));
                    assertThat(methodNode.getNode(ClassFileSequencerLexicon.ANNOTATIONS).getNodes().getSize(), is(1l));
                    final Node annotationNode = methodNode.getNode(ClassFileSequencerLexicon.ANNOTATIONS).getNodes().nextNode();
                    assertThat(annotationNode.getName(), is("Deprecated"));
                    assertThat(annotationNode.hasProperty(ClassFileSequencerLexicon.ANNOTATION_TYPE), is(true));
                    assertThat(annotationNode.getProperty(ClassFileSequencerLexicon.ANNOTATION_TYPE).getString(),
                               is(ClassFileSequencerLexicon.AnnotationType.MARKER.toString()));
                    assertThat(annotationNode.getNodes().getSize(), is(0L));

                    assertThat(methodNode.hasNode(ClassFileSequencerLexicon.TYPE_PARAMETERS), is(true));
                    assertThat(methodNode.getNode(ClassFileSequencerLexicon.TYPE_PARAMETERS).getNodes().getSize(), is(1L));
                    final Node typeParamNode = methodNode.getNode(ClassFileSequencerLexicon.TYPE_PARAMETERS).getNodes().nextNode();
                    assertThat(typeParamNode.getName(), is("U"));
                    assertThat(typeParamNode.hasNode(ClassFileSequencerLexicon.BOUNDS), is(true));
                    assertThat(typeParamNode.getNode(ClassFileSequencerLexicon.BOUNDS).getNodes().getSize(), is(1L));
                    final Node boundNode = typeParamNode.getNode(ClassFileSequencerLexicon.BOUNDS).getNodes().nextNode();
                    assertThat(boundNode.getName(), is("Number"));

                    assertThat(methodNode.hasNode(ClassFileSequencerLexicon.JAVADOC), is(true));
                    final Node javadocNode = methodNode.getNode(ClassFileSequencerLexicon.JAVADOC);
                    final String comment = "Performs a shutdown";
                    assertThat(javadocNode.getProperty(ClassFileSequencerLexicon.COMMENT).getString().contains(comment), is(true));

                    { // parameters
                        assertThat(methodNode.hasNode(ClassFileSequencerLexicon.METHOD_PARAMETERS), is(true));
                        assertThat(methodNode.getNode(ClassFileSequencerLexicon.METHOD_PARAMETERS).getNodes().getSize(), is(1L));
                        final Node paramNode = methodNode.getNode(ClassFileSequencerLexicon.METHOD_PARAMETERS).getNodes().nextNode();
                        assertThat(paramNode.getName(), is("waitTime"));
                        assertThat(paramNode.getPrimaryNodeType().getName(), is(ClassFileSequencerLexicon.PARAMETER));
                        assertThat(paramNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(), is("U"));
                        assertThat(paramNode.getProperty(ClassFileSequencerLexicon.FINAL).getBoolean(), is(true));
                        assertThat(paramNode.getProperty(ClassFileSequencerLexicon.VARARGS).getBoolean(), is(false));
                        assertThat(paramNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(false));
                        assertThat(paramNode.hasNode(ClassFileSequencerLexicon.INITIALIZER), is(false));
                    }

                    { // return type
                        assertThat(methodNode.getProperty(ClassFileSequencerLexicon.RETURN_TYPE_CLASS_NAME).getString(),
                                   is("void"));
                        assertThat(methodNode.hasNode(ClassFileSequencerLexicon.RETURN_TYPE), is(true));
                        final Node returnTypeNode = methodNode.getNode(ClassFileSequencerLexicon.RETURN_TYPE);
                        assertThat(returnTypeNode.isNodeType(ClassFileSequencerLexicon.PRIMITIVE_TYPE), is(true));
                        assertThat(returnTypeNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(), is("void"));
                    }
                }
            }

            { // typeParameters
                assertThat(classNode.hasNode(ClassFileSequencerLexicon.TYPE_PARAMETERS), is(true));
                assertThat(classNode.getNode(ClassFileSequencerLexicon.TYPE_PARAMETERS).getNodes().getSize(), is(1L));
                assertThat(classNode.getNode(ClassFileSequencerLexicon.TYPE_PARAMETERS).getPrimaryNodeType().getName(),
                           is(ClassFileSequencerLexicon.TYPE_PARAMETERS));

                final Node typeParamNode = classNode.getNode(ClassFileSequencerLexicon.TYPE_PARAMETERS).getNode("T");
                assertThat(typeParamNode.getPrimaryNodeType().getName(), is(ClassFileSequencerLexicon.TYPE_PARAMETER));
                assertThat(typeParamNode.hasNode(ClassFileSequencerLexicon.BOUNDS), is(true));

                final Node boundsNode = typeParamNode.getNode(ClassFileSequencerLexicon.BOUNDS);
                assertThat(boundsNode.getNodes().getSize(), is(1L));
                assertThat(boundsNode.getNodes().nextNode().getName(), is("HashMap<String, ?>"));
            }

            { // implements
                assertThat(classNode.hasNode(ClassFileSequencerLexicon.IMPLEMENTS), is(true));
                assertThat(classNode.getNode(ClassFileSequencerLexicon.IMPLEMENTS).getNodes().getSize(), is(1L));

                final Node interfaceNode = classNode.getNode(ClassFileSequencerLexicon.IMPLEMENTS).getNode("Serializable");
                assertThat(interfaceNode.getPrimaryNodeType().getName(), is(ClassFileSequencerLexicon.SIMPLE_TYPE));
            }

            { // extends
                assertThat(classNode.hasNode(ClassFileSequencerLexicon.EXTENDS), is(true));
                assertThat(classNode.getNode(ClassFileSequencerLexicon.EXTENDS).getNodes().getSize(), is(1L));

                final Node superTypeNode = classNode.getNode(ClassFileSequencerLexicon.EXTENDS).getNode("ArrayList<T>");
                assertThat(superTypeNode.getPrimaryNodeType().getName(), is(ClassFileSequencerLexicon.PARAMETERIZED_TYPE));
            }

            { // javadoc
                assertThat(classNode.hasNode(ClassFileSequencerLexicon.JAVADOC), is(true));

                final Node javadocNode = classNode.getNode(ClassFileSequencerLexicon.JAVADOC);
                final String comment = "This is a class type test class";
                assertThat(javadocNode.getProperty(ClassFileSequencerLexicon.COMMENT).getString().contains(comment), is(true));
            }

            { // nested types
                assertThat(classNode.hasNode(ClassFileSequencerLexicon.NESTED_TYPES), is(true));
                assertThat(classNode.getNode(ClassFileSequencerLexicon.NESTED_TYPES).getNodes().getSize(), is(1L));

                final Node nestedTypeNode = classNode.getNode(ClassFileSequencerLexicon.NESTED_TYPES).getNodes().nextNode();
                assertThat(nestedTypeNode.getName(), is("Blah"));
                assertThat(nestedTypeNode.getProperty(ClassFileSequencerLexicon.NAME).getString(), is(nestedTypeNode.getName()));
                assertThat(nestedTypeNode.hasProperty(ClassFileSequencerLexicon.SEQUENCED_DATE), is(true));
                assertThat(nestedTypeNode.getProperty(ClassFileSequencerLexicon.SUPER_CLASS_NAME).getString(),
                           is("java.lang.Object"));
                assertThat(nestedTypeNode.getProperty(ClassFileSequencerLexicon.VISIBILITY).getString(), is("package"));
                assertThat(nestedTypeNode.getProperty(ClassFileSequencerLexicon.ABSTRACT).getBoolean(), is(false));
                assertThat(nestedTypeNode.getProperty(ClassFileSequencerLexicon.INTERFACE).getBoolean(), is(false));
                assertThat(nestedTypeNode.getProperty(ClassFileSequencerLexicon.FINAL).getBoolean(), is(false));
                assertThat(nestedTypeNode.getProperty(ClassFileSequencerLexicon.STRICT_FP).getBoolean(), is(false));
                assertThat(nestedTypeNode.getProperty(ClassFileSequencerLexicon.STATIC).getBoolean(), is(false));
                assertThat(nestedTypeNode.hasProperty(ClassFileSequencerLexicon.IMPORTS), is(false));

                assertThat(nestedTypeNode.hasProperty(ClassFileSequencerLexicon.INTERFACES), is(true));
                assertThat(nestedTypeNode.getProperty(ClassFileSequencerLexicon.INTERFACES).getValues().length, is(1));
                assertThat(nestedTypeNode.getProperty(ClassFileSequencerLexicon.INTERFACES).getValues()[0].getString(),
                           is("Comparable<T>"));

                assertThat(nestedTypeNode.hasNode(ClassFileSequencerLexicon.FIELDS), is(true));
                assertThat(nestedTypeNode.getNode(ClassFileSequencerLexicon.FIELDS).getNodes().getSize(), is(1L));

                assertThat(nestedTypeNode.hasNode(ClassFileSequencerLexicon.CONSTRUCTORS), is(true));
                assertThat(nestedTypeNode.getNode(ClassFileSequencerLexicon.CONSTRUCTORS).getNodes().getSize(), is(1L));

                assertThat(nestedTypeNode.hasNode(ClassFileSequencerLexicon.METHODS), is(true));
                assertThat(nestedTypeNode.getNode(ClassFileSequencerLexicon.METHODS).getNodes().getSize(), is(2L));
            }
        }
    }

    @Ignore
    @Test
    public void shouldSequenceTwoClassesFile() throws Exception {
        final String packagePath = ClassType.class.getName().replaceAll("\\.", "/");
        createNodeWithContentFromFile("twoouterclasses.java", packagePath + ".java");

        // expected by sequencer in a different location
        final String expectedOutputPath = "java/twoouterclasses.java";
        final Node compilationUnitNode = getOutputNode(rootNode, expectedOutputPath);
        assertThat(compilationUnitNode, is(notNullValue()));
        assertThat(compilationUnitNode.isNodeType(ClassFileSequencerLexicon.COMPILATION_UNIT), is(true));
        assertThat(compilationUnitNode.hasProperty(ClassFileSequencerLexicon.SEQUENCED_DATE), is(true));
        assertThat(compilationUnitNode.hasNode(ClassFileSequencerLexicon.MESSAGES), is(false));
        assertThat(compilationUnitNode.hasProperty(ClassFileSequencerLexicon.TYPES), is(true));
        assertThat(compilationUnitNode.getProperty(ClassFileSequencerLexicon.TYPES).getValues().length, is(2));
    }

}
