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

import java.util.ArrayList;
import java.util.List;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Value;
import org.junit.Test;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;
import org.modeshape.sequencer.classfile.ClassFileSequencerLexicon;
import org.modeshape.sequencer.classfile.metadata.Visibility;
import org.modeshape.sequencer.testdata.EnumType;

public final class EnumTypeSequencerTest extends AbstractSequencerTest {

    @Test
    public void shouldSequenceEnumTypeFile() throws Exception {
        final String packagePath = EnumType.class.getName().replaceAll("\\.", "/");
        createNodeWithContentFromFile("enumtype.java", packagePath + ".java");

        // expected by sequencer in a different location
        final String expectedOutputPath = "java/enumtype.java";
        final Node compilationUnitNode = getOutputNode(rootNode, expectedOutputPath);
        assertThat(compilationUnitNode, is(notNullValue()));
        assertThat(compilationUnitNode.isNodeType(ClassFileSequencerLexicon.COMPILATION_UNIT), is(true));
        assertThat(compilationUnitNode.hasProperty(ClassFileSequencerLexicon.SEQUENCED_DATE), is(true));
        assertThat(compilationUnitNode.hasProperty(ClassFileSequencerLexicon.TYPES), is(true));
        assertThat(compilationUnitNode.getProperty(ClassFileSequencerLexicon.TYPES).getValues().length, is(1));
        assertThat(compilationUnitNode.hasNode(ClassFileSequencerLexicon.MESSAGES), is(false));

        { // 2 imports
            assertThat(compilationUnitNode.hasNode(ClassFileSequencerLexicon.IMPORTS), is(true));
            assertThat(compilationUnitNode.getNode(ClassFileSequencerLexicon.IMPORTS).getNodes().getSize(), is(2L));

            final NodeIterator itr = compilationUnitNode.getNode(ClassFileSequencerLexicon.IMPORTS).getNodes();

            { // first import
                final Node firstImport = itr.nextNode();
                assertThat(firstImport.getName(), is("java.util.EnumSet"));
                assertThat(firstImport.hasProperty(ClassFileSequencerLexicon.STATIC), is(true));
                assertThat(firstImport.getProperty(ClassFileSequencerLexicon.STATIC).getBoolean(), is(false));
                assertThat(firstImport.hasProperty(ClassFileSequencerLexicon.ON_DEMAND), is(true));
                assertThat(firstImport.getProperty(ClassFileSequencerLexicon.ON_DEMAND).getBoolean(), is(false));
            }

            { // second import
                final Node secondImport = itr.nextNode();
                assertThat(secondImport.getName(), is("java.util"));
                assertThat(secondImport.hasProperty(ClassFileSequencerLexicon.STATIC), is(true));
                assertThat(secondImport.getProperty(ClassFileSequencerLexicon.STATIC).getBoolean(), is(false));
                assertThat(secondImport.hasProperty(ClassFileSequencerLexicon.ON_DEMAND), is(true));
                assertThat(secondImport.getProperty(ClassFileSequencerLexicon.ON_DEMAND).getBoolean(), is(true));
            }
        }

        { // comments
            assertThat(compilationUnitNode.hasNode(ClassFileSequencerLexicon.COMMENTS), is(true));
            assertThat(compilationUnitNode.getNode(ClassFileSequencerLexicon.COMMENTS).getNodes().getSize(), is(3L));

            final NodeIterator itr = compilationUnitNode.getNode(ClassFileSequencerLexicon.COMMENTS).getNodes();

            {
                final Node comment = itr.nextNode();
                assertThat(comment.getName(), is(ClassFileSequencerLexicon.COMMENT));
                assertThat(comment.hasProperty(ClassFileSequencerLexicon.COMMENT_TYPE), is(true));
                assertThat(comment.getProperty(ClassFileSequencerLexicon.COMMENT_TYPE).getString(),
                           is(ClassFileSequencerLexicon.CommentType.BLOCK.toString()));
                assertThat(comment.hasProperty(ClassFileSequencerLexicon.COMMENT), is(true));
                assertThat(comment.getProperty(ClassFileSequencerLexicon.COMMENT).getString().startsWith("/*\n * ModeShape"),
                           is(true));
            }

            {
                final Node comment = itr.nextNode();
                assertThat(comment.getName(), is(ClassFileSequencerLexicon.COMMENT));
                assertThat(comment.hasProperty(ClassFileSequencerLexicon.COMMENT_TYPE), is(true));
                assertThat(comment.getProperty(ClassFileSequencerLexicon.COMMENT_TYPE).getString(),
                           is(ClassFileSequencerLexicon.CommentType.LINE.toString()));
                assertThat(comment.hasProperty(ClassFileSequencerLexicon.COMMENT), is(true));
                assertThat(comment.getProperty(ClassFileSequencerLexicon.COMMENT).getString(), is("//CHECKSTYLE:OFF"));
            }

            {
                final Node comment = itr.nextNode();
                assertThat(comment.getName(), is(ClassFileSequencerLexicon.COMMENT));
                assertThat(comment.hasProperty(ClassFileSequencerLexicon.COMMENT_TYPE), is(true));
                assertThat(comment.getProperty(ClassFileSequencerLexicon.COMMENT_TYPE).getString(),
                           is(ClassFileSequencerLexicon.CommentType.LINE.toString()));
                assertThat(comment.hasProperty(ClassFileSequencerLexicon.COMMENT), is(true));
                assertThat(comment.getProperty(ClassFileSequencerLexicon.COMMENT).getString(), is("//CHECKSTYLE:ON"));
            }
        }

        { // enum type
            final Node enumNode = compilationUnitNode.getNode(packagePath);
            assertThat(enumNode.getName(), is("EnumType"));
            assertThat(enumNode.getProperty(ClassFileSequencerLexicon.NAME).getString(), is(enumNode.getName()));
            assertThat(enumNode.hasProperty(ClassFileSequencerLexicon.SEQUENCED_DATE), is(true));
            assertThat(enumNode.hasProperty(ClassFileSequencerLexicon.SUPER_CLASS_NAME), is(false));
            assertThat(enumNode.getProperty(ClassFileSequencerLexicon.VISIBILITY).getString(),
                       is(Visibility.PUBLIC.getDescription()));
            assertThat(enumNode.getProperty(ClassFileSequencerLexicon.ABSTRACT).getBoolean(), is(false));
            assertThat(enumNode.getProperty(ClassFileSequencerLexicon.INTERFACE).getBoolean(), is(false));
            assertThat(enumNode.getProperty(ClassFileSequencerLexicon.FINAL).getBoolean(), is(false));
            assertThat(enumNode.getProperty(ClassFileSequencerLexicon.STRICT_FP).getBoolean(), is(false));
            assertThat(enumNode.getProperty(ClassFileSequencerLexicon.STATIC).getBoolean(), is(false));
            assertThat(enumNode.hasProperty(ClassFileSequencerLexicon.IMPORTS), is(false));
            assertThat(enumNode.hasProperty(ClassFileSequencerLexicon.INTERFACES), is(true));
            assertThat(enumNode.getProperty(ClassFileSequencerLexicon.INTERFACES).getValues().length, is(1));
            assertThat(enumNode.getProperty(ClassFileSequencerLexicon.INTERFACES).getValues()[0].getString(), is("Cloneable"));

            assertThat(enumNode.hasNode(ClassFileSequencerLexicon.FIELDS), is(true));
            assertThat(enumNode.getNode(ClassFileSequencerLexicon.FIELDS).getNodes().getSize(), is(3L));

            final Node fieldsNode = enumNode.getNode(ClassFileSequencerLexicon.FIELDS);

            { // _lookup field
                final Node fieldNode = fieldsNode.getNode("_lookup");
                assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.NAME).getString(), is(fieldNode.getName()));
                assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(),
                           is("Map<Integer, EnumType>"));
                assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.VISIBILITY).getString(),
                           is(Visibility.PRIVATE.getDescription()));
                assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.STATIC).getBoolean(), is(true));
                assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.FINAL).getBoolean(), is(true));
                assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.TRANSIENT).getBoolean(), is(false));
                assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.VOLATILE).getBoolean(), is(false));
                assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(false));
                assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.INITIALIZER), is(false));
                assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.JAVADOC), is(false));

                assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.TYPE), is(true));
                final Node fieldTypeNode = fieldNode.getNode(ClassFileSequencerLexicon.TYPE);
                assertThat(fieldTypeNode.isNodeType(ClassFileSequencerLexicon.PARAMETERIZED_TYPE), is(true));
                assertThat(fieldTypeNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(),
                           is("Map<Integer, EnumType>"));
            }

            { // code field
                final Node fieldNode = fieldsNode.getNode("code");
                assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.NAME).getString(), is(fieldNode.getName()));
                assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(), is("int"));
                assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.VISIBILITY).getString(),
                           is(Visibility.PRIVATE.getDescription()));
                assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.STATIC).getBoolean(), is(false));
                assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.FINAL).getBoolean(), is(false));
                assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.TRANSIENT).getBoolean(), is(false));
                assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.VOLATILE).getBoolean(), is(false));
                assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(false));
                assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.INITIALIZER), is(false));
                assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.JAVADOC), is(false));

                assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.TYPE), is(true));
                final Node fieldTypeNode = fieldNode.getNode(ClassFileSequencerLexicon.TYPE);
                assertThat(fieldTypeNode.isNodeType(ClassFileSequencerLexicon.PRIMITIVE_TYPE), is(true));
                assertThat(fieldTypeNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(), is("int"));
            }

            { // executor field
                final Node fieldNode = fieldsNode.getNode("executor");
                assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.NAME).getString(), is(fieldNode.getName()));
                assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(), is("Executor"));
                assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.VISIBILITY).getString(),
                           is(Visibility.PACKAGE.getDescription()));
                assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.STATIC).getBoolean(), is(false));
                assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.FINAL).getBoolean(), is(false));
                assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.TRANSIENT).getBoolean(), is(false));
                assertThat(fieldNode.getProperty(ClassFileSequencerLexicon.VOLATILE).getBoolean(), is(false));
                assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(false));
                assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.INITIALIZER), is(false));
                assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.JAVADOC), is(false));

                assertThat(fieldNode.hasNode(ClassFileSequencerLexicon.TYPE), is(true));
                final Node fieldTypeNode = fieldNode.getNode(ClassFileSequencerLexicon.TYPE);
                assertThat(fieldTypeNode.isNodeType(ClassFileSequencerLexicon.SIMPLE_TYPE), is(true));
                assertThat(fieldTypeNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(), is("Executor"));
            }

            assertThat(enumNode.hasNode(ClassFileSequencerLexicon.CONSTRUCTORS), is(true));
            assertThat(enumNode.getNode(ClassFileSequencerLexicon.CONSTRUCTORS).getNodes().getSize(), is(1L));

            { // constructor
                final Node constructorNode = enumNode.getNode(ClassFileSequencerLexicon.CONSTRUCTORS).getNodes().nextNode();
                assertThat(constructorNode.getName(), is("EnumType"));
                assertThat(constructorNode.getProperty(ClassFileSequencerLexicon.RETURN_TYPE_CLASS_NAME).getString(), is("void"));
                assertThat(constructorNode.getProperty(ClassFileSequencerLexicon.VISIBILITY).getString(), is("private"));
                assertThat(constructorNode.getProperty(ClassFileSequencerLexicon.STATIC).getBoolean(), is(false));
                assertThat(constructorNode.getProperty(ClassFileSequencerLexicon.FINAL).getBoolean(), is(false));
                assertThat(constructorNode.getProperty(ClassFileSequencerLexicon.ABSTRACT).getBoolean(), is(false));
                assertThat(constructorNode.getProperty(ClassFileSequencerLexicon.STRICT_FP).getBoolean(), is(false));
                assertThat(constructorNode.getProperty(ClassFileSequencerLexicon.NATIVE).getBoolean(), is(false));
                assertThat(constructorNode.getProperty(ClassFileSequencerLexicon.SYNCHRONIZED).getBoolean(), is(false));
                assertThat(constructorNode.hasProperty(ClassFileSequencerLexicon.THROWN_EXCEPTIONS), is(false));
                assertThat(constructorNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(false));
                assertThat(constructorNode.hasNode(ClassFileSequencerLexicon.TYPE_PARAMETERS), is(false));
                assertThat(constructorNode.hasNode(ClassFileSequencerLexicon.JAVADOC), is(false));
                assertThat(constructorNode.hasNode(ClassFileSequencerLexicon.METHOD_PARAMETERS), is(true));
                assertThat(constructorNode.getNode(ClassFileSequencerLexicon.METHOD_PARAMETERS).getNodes().getSize(), is(1L));

                { // code parameter
                    final Node paramNode = constructorNode.getNode(ClassFileSequencerLexicon.METHOD_PARAMETERS).getNodes().nextNode();
                    assertThat(paramNode.getName(), is("code"));
                    assertThat(paramNode.getProperty(ClassFileSequencerLexicon.FINAL).getBoolean(), is(false));
                    assertThat(paramNode.getProperty(ClassFileSequencerLexicon.VARARGS).getBoolean(), is(false));
                    assertThat(paramNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(), is("int"));
                    assertThat(paramNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(false));
                    assertThat(paramNode.hasNode(ClassFileSequencerLexicon.INITIALIZER), is(false));
                    assertThat(paramNode.hasNode(ClassFileSequencerLexicon.TYPE), is(true));
                }

                { // constructor body statements
                    assertThat(constructorNode.hasNode(ClassFileSequencerLexicon.BODY), is(true));
                    assertThat(constructorNode.getNode(ClassFileSequencerLexicon.BODY).getNodes().getSize(), is(2L));

                    final NodeIterator itr = constructorNode.getNode(ClassFileSequencerLexicon.BODY).getNodes();

                    { // first statement
                        final Node statementNode = itr.nextNode();
                        assertThat(statementNode.getProperty(ClassFileSequencerLexicon.CONTENT).getString(),
                                   is("this.code=code;\n"));
                    }

                    { // second statement
                        final Node statementNode = itr.nextNode();
                        assertThat(statementNode.getProperty(ClassFileSequencerLexicon.CONTENT).getString(),
                                   is("this.executor=new Executor();\n"));
                    }
                }
            }

            assertThat(enumNode.hasNode(ClassFileSequencerLexicon.METHODS), is(true));
            assertThat(enumNode.getNode(ClassFileSequencerLexicon.METHODS).getNodes().getSize(), is(3L));

            final Node methodsNode = enumNode.getNode(ClassFileSequencerLexicon.METHODS);

            { // getId method
                final Node methodNode = methodsNode.getNode("getCode");
                assertThat(methodNode.getProperty(ClassFileSequencerLexicon.NAME).getString(), is(methodNode.getName()));
                assertThat(methodNode.getProperty(ClassFileSequencerLexicon.RETURN_TYPE_CLASS_NAME).getString(), is("int"));
                assertThat(methodNode.getProperty(ClassFileSequencerLexicon.VISIBILITY).getString(),
                           is(Visibility.PUBLIC.getDescription()));
                assertThat(methodNode.getProperty(ClassFileSequencerLexicon.STATIC).getBoolean(), is(false));
                assertThat(methodNode.getProperty(ClassFileSequencerLexicon.FINAL).getBoolean(), is(false));
                assertThat(methodNode.getProperty(ClassFileSequencerLexicon.ABSTRACT).getBoolean(), is(false));
                assertThat(methodNode.getProperty(ClassFileSequencerLexicon.STRICT_FP).getBoolean(), is(false));
                assertThat(methodNode.getProperty(ClassFileSequencerLexicon.NATIVE).getBoolean(), is(false));
                assertThat(methodNode.getProperty(ClassFileSequencerLexicon.SYNCHRONIZED).getBoolean(), is(false));
                assertThat(methodNode.hasProperty(ClassFileSequencerLexicon.THROWN_EXCEPTIONS), is(false));
                assertThat(methodNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(false));
                assertThat(methodNode.hasNode(ClassFileSequencerLexicon.TYPE_PARAMETERS), is(false));
                assertThat(methodNode.hasNode(ClassFileSequencerLexicon.METHOD_PARAMETERS), is(false));
                assertThat(methodNode.hasNode(ClassFileSequencerLexicon.JAVADOC), is(false));

                assertThat(methodNode.hasNode(ClassFileSequencerLexicon.RETURN_TYPE), is(true));
                final Node returnTypeNode = methodNode.getNode(ClassFileSequencerLexicon.RETURN_TYPE);
                assertThat(returnTypeNode.isNodeType(ClassFileSequencerLexicon.PRIMITIVE_TYPE), is(true));
                assertThat(returnTypeNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(), is("int"));

                assertThat(methodNode.hasNode(ClassFileSequencerLexicon.BODY), is(true));
                assertThat(methodNode.getNode(ClassFileSequencerLexicon.BODY).getNodes().getSize(), is(1L));
                final Node statementNode = methodNode.getNode(ClassFileSequencerLexicon.BODY).getNodes().nextNode();
                assertThat(statementNode.getProperty(ClassFileSequencerLexicon.CONTENT).getString(), is("return this.code;\n"));
            }

            { // execute method
                final Node methodNode = methodsNode.getNode("execute");
                assertThat(methodNode.getProperty(ClassFileSequencerLexicon.NAME).getString(), is(methodNode.getName()));
                assertThat(methodNode.getProperty(ClassFileSequencerLexicon.RETURN_TYPE_CLASS_NAME).getString(), is("void"));
                assertThat(methodNode.getProperty(ClassFileSequencerLexicon.VISIBILITY).getString(),
                           is(Visibility.PUBLIC.getDescription()));
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
                assertThat(methodNode.hasNode(ClassFileSequencerLexicon.JAVADOC), is(false));

                assertThat(methodNode.hasNode(ClassFileSequencerLexicon.RETURN_TYPE), is(true));
                final Node returnTypeNode = methodNode.getNode(ClassFileSequencerLexicon.RETURN_TYPE);
                assertThat(returnTypeNode.isNodeType(ClassFileSequencerLexicon.PRIMITIVE_TYPE), is(true));
                assertThat(returnTypeNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(), is("void"));
            }

            { // get method
                final Node methodNode = methodsNode.getNode("get");
                assertThat(methodNode.getProperty(ClassFileSequencerLexicon.NAME).getString(), is(methodNode.getName()));
                assertThat(methodNode.getProperty(ClassFileSequencerLexicon.RETURN_TYPE_CLASS_NAME).getString(), is("EnumType"));
                assertThat(methodNode.getProperty(ClassFileSequencerLexicon.VISIBILITY).getString(),
                           is(Visibility.PUBLIC.getDescription()));
                assertThat(methodNode.getProperty(ClassFileSequencerLexicon.STATIC).getBoolean(), is(true));
                assertThat(methodNode.getProperty(ClassFileSequencerLexicon.FINAL).getBoolean(), is(false));
                assertThat(methodNode.getProperty(ClassFileSequencerLexicon.ABSTRACT).getBoolean(), is(false));
                assertThat(methodNode.getProperty(ClassFileSequencerLexicon.STRICT_FP).getBoolean(), is(false));
                assertThat(methodNode.getProperty(ClassFileSequencerLexicon.NATIVE).getBoolean(), is(false));
                assertThat(methodNode.getProperty(ClassFileSequencerLexicon.SYNCHRONIZED).getBoolean(), is(false));
                assertThat(methodNode.hasProperty(ClassFileSequencerLexicon.THROWN_EXCEPTIONS), is(false));
                assertThat(methodNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(false));
                assertThat(methodNode.hasNode(ClassFileSequencerLexicon.TYPE_PARAMETERS), is(false));
                assertThat(methodNode.hasNode(ClassFileSequencerLexicon.JAVADOC), is(false));

                assertThat(methodNode.hasNode(ClassFileSequencerLexicon.METHOD_PARAMETERS), is(true));
                assertThat(methodNode.getNode(ClassFileSequencerLexicon.METHOD_PARAMETERS).getNodes().getSize(), is(1L));

                { // code parameter
                    final Node paramNode = methodNode.getNode(ClassFileSequencerLexicon.METHOD_PARAMETERS).getNodes().nextNode();
                    assertThat(paramNode.getName(), is("code"));
                    assertThat(paramNode.getProperty(ClassFileSequencerLexicon.FINAL).getBoolean(), is(false));
                    assertThat(paramNode.getProperty(ClassFileSequencerLexicon.VARARGS).getBoolean(), is(false));
                    assertThat(paramNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(), is("int"));
                    assertThat(paramNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(false));
                    assertThat(paramNode.hasNode(ClassFileSequencerLexicon.INITIALIZER), is(false));
                    assertThat(paramNode.hasNode(ClassFileSequencerLexicon.TYPE), is(true));
                }

                { // body
                    assertThat(methodNode.hasNode(ClassFileSequencerLexicon.BODY), is(true));
                    assertThat(methodNode.getNode(ClassFileSequencerLexicon.BODY).getNodes().getSize(), is(1L));
                    final Node statementNode = methodNode.getNode(ClassFileSequencerLexicon.BODY).getNodes().nextNode();
                    assertThat(statementNode.getProperty(ClassFileSequencerLexicon.CONTENT).getString(),
                               is("return _lookup.get(code);\n"));
                }

                assertThat(methodNode.hasNode(ClassFileSequencerLexicon.RETURN_TYPE), is(true));
                final Node returnTypeNode = methodNode.getNode(ClassFileSequencerLexicon.RETURN_TYPE);
                assertThat(returnTypeNode.isNodeType(ClassFileSequencerLexicon.SIMPLE_TYPE), is(true));
                assertThat(returnTypeNode.getProperty(ClassFileSequencerLexicon.TYPE_CLASS_NAME).getString(), is("EnumType"));
            }

            { // initializer
                assertThat(enumNode.hasNode(ClassFileSequencerLexicon.INITIALIZER), is(true));

                final Node initializerNode = enumNode.getNode(ClassFileSequencerLexicon.INITIALIZER);
                assertThat(initializerNode.getNodes().getSize(), is(2L));

                final NodeIterator itr = initializerNode.getNodes();

                { // first statement
                    final Node statementNode = itr.nextNode();
                    assertThat(statementNode.getProperty(ClassFileSequencerLexicon.CONTENT).getString(),
                               is("_lookup=new HashMap<Integer,EnumType>();\n"));
                }

                { // second statement
                    final Node statementNode = itr.nextNode();
                    assertThat(statementNode.getProperty(ClassFileSequencerLexicon.CONTENT).getString().startsWith("for (EnumType"),
                               is(true));
                }
            }

            { // nested types
                assertThat(enumNode.hasNode(ClassFileSequencerLexicon.NESTED_TYPES), is(true));
                assertThat(enumNode.getNode(ClassFileSequencerLexicon.NESTED_TYPES).getNodes().getSize(), is(1L));

                final Node nestedTypeNode = enumNode.getNode(ClassFileSequencerLexicon.NESTED_TYPES).getNodes().nextNode();
                assertThat(nestedTypeNode.getName(), is("Executor"));
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
                assertThat(nestedTypeNode.hasProperty(ClassFileSequencerLexicon.INTERFACES), is(false));
                assertThat(nestedTypeNode.hasNode(ClassFileSequencerLexicon.FIELDS), is(false));
                assertThat(nestedTypeNode.hasNode(ClassFileSequencerLexicon.CONSTRUCTORS), is(false));
                assertThat(nestedTypeNode.hasNode(ClassFileSequencerLexicon.METHODS), is(true));
                assertThat(nestedTypeNode.getNode(ClassFileSequencerLexicon.METHODS).getNodes().getSize(), is(1L));
            }

            // enum constants
            final Value[] jcrValues = enumNode.getProperty(ClassFileSequencerLexicon.ENUM_VALUES).getValues();
            final List<String> values = new ArrayList<>(jcrValues.length);
            for (final Value value : jcrValues)
                values.add(value.getString());
            assertThat(values, hasItems("DONE", "READY", "SKIPPED", "WAITING"));
            assertThat(enumNode.hasNode(ClassFileSequencerLexicon.ENUM_CONSTANTS), is(true));

            final Node constantsNode = enumNode.getNode(ClassFileSequencerLexicon.ENUM_CONSTANTS);

            { // DONE
                assertThat(constantsNode.hasNode("DONE"), is(true));
                final Node doneNode = constantsNode.getNode("DONE");
                assertThat(doneNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(false));
                assertThat(doneNode.hasNode(ClassFileSequencerLexicon.JAVADOC), is(true));
                assertThat(doneNode.hasNode(ClassFileSequencerLexicon.FIELDS), is(false));
                assertThat(doneNode.hasNode(ClassFileSequencerLexicon.METHODS), is(true));
                assertThat(doneNode.hasNode(ClassFileSequencerLexicon.NESTED_TYPES), is(false));

                assertThat(doneNode.hasNode(ClassFileSequencerLexicon.ARGUMENTS), is(true));
                assertThat(doneNode.getNode(ClassFileSequencerLexicon.ARGUMENTS).getNodes().getSize(), is(1L));
                final Node argNode = doneNode.getNode(ClassFileSequencerLexicon.ARGUMENTS).getNodes().nextNode();
                assertThat(argNode.getName(), is(ClassFileSequencerLexicon.ARGUMENT));
                assertThat(argNode.getProperty(ClassFileSequencerLexicon.CONTENT).getLong(), is(3L));
            }

            { // READY
                assertThat(constantsNode.hasNode("READY"), is(true));
                final Node doneNode = constantsNode.getNode("READY");
                assertThat(doneNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(false));
                assertThat(doneNode.hasNode(ClassFileSequencerLexicon.JAVADOC), is(true));
                assertThat(doneNode.hasNode(ClassFileSequencerLexicon.FIELDS), is(true));
                assertThat(doneNode.hasNode(ClassFileSequencerLexicon.METHODS), is(true));
                assertThat(doneNode.hasNode(ClassFileSequencerLexicon.NESTED_TYPES), is(false));

                assertThat(doneNode.hasNode(ClassFileSequencerLexicon.ARGUMENTS), is(true));
                assertThat(doneNode.getNode(ClassFileSequencerLexicon.ARGUMENTS).getNodes().getSize(), is(1L));
                final Node argNode = doneNode.getNode(ClassFileSequencerLexicon.ARGUMENTS).getNodes().nextNode();
                assertThat(argNode.getName(), is(ClassFileSequencerLexicon.ARGUMENT));
                assertThat(argNode.getProperty(ClassFileSequencerLexicon.CONTENT).getLong(), is(1L));
            }

            { // SKIPPED
                assertThat(constantsNode.hasNode("SKIPPED"), is(true));
                final Node doneNode = constantsNode.getNode("SKIPPED");
                assertThat(doneNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(false));
                assertThat(doneNode.hasNode(ClassFileSequencerLexicon.JAVADOC), is(true));
                assertThat(doneNode.hasNode(ClassFileSequencerLexicon.FIELDS), is(false));
                assertThat(doneNode.hasNode(ClassFileSequencerLexicon.METHODS), is(true));
                assertThat(doneNode.hasNode(ClassFileSequencerLexicon.NESTED_TYPES), is(false));

                assertThat(doneNode.hasNode(ClassFileSequencerLexicon.ARGUMENTS), is(true));
                assertThat(doneNode.getNode(ClassFileSequencerLexicon.ARGUMENTS).getNodes().getSize(), is(1L));
                final Node argNode = doneNode.getNode(ClassFileSequencerLexicon.ARGUMENTS).getNodes().nextNode();
                assertThat(argNode.getName(), is(ClassFileSequencerLexicon.ARGUMENT));
                assertThat(argNode.getProperty(ClassFileSequencerLexicon.CONTENT).getLong(), is(2L));
            }

            { // WAITING
                assertThat(constantsNode.hasNode("WAITING"), is(true));
                final Node doneNode = constantsNode.getNode("WAITING");
                assertThat(doneNode.hasNode(ClassFileSequencerLexicon.ANNOTATIONS), is(false));
                assertThat(doneNode.hasNode(ClassFileSequencerLexicon.JAVADOC), is(true));
                assertThat(doneNode.hasNode(ClassFileSequencerLexicon.FIELDS), is(false));
                assertThat(doneNode.hasNode(ClassFileSequencerLexicon.METHODS), is(true));
                assertThat(doneNode.hasNode(ClassFileSequencerLexicon.NESTED_TYPES), is(true));

                assertThat(doneNode.hasNode(ClassFileSequencerLexicon.ARGUMENTS), is(true));
                assertThat(doneNode.getNode(ClassFileSequencerLexicon.ARGUMENTS).getNodes().getSize(), is(1L));
                final Node argNode = doneNode.getNode(ClassFileSequencerLexicon.ARGUMENTS).getNodes().nextNode();
                assertThat(argNode.getName(), is(ClassFileSequencerLexicon.ARGUMENT));
                assertThat(argNode.getProperty(ClassFileSequencerLexicon.CONTENT).getLong(), is(0L));
            }
        }
    }
}
