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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.modeshape.sequencer.javafile.metadata.AbstractMetadata.PRIVATE;
import static org.modeshape.sequencer.javafile.metadata.AbstractMetadata.PUBLIC;
import static org.modeshape.sequencer.javafile.metadata.AbstractMetadata.STATIC;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.sequencer.javafile.metadata.AnnotationMetadata;
import org.modeshape.sequencer.javafile.metadata.FieldMetadata;
import org.modeshape.sequencer.javafile.metadata.ImportMetadata;
import org.modeshape.sequencer.javafile.metadata.JavaMetadata;
import org.modeshape.sequencer.javafile.metadata.MethodMetadata;
import org.modeshape.sequencer.javafile.metadata.ModifierMetadata;
import org.modeshape.sequencer.javafile.metadata.PackageMetadata;
import org.modeshape.sequencer.javafile.metadata.TypeMetadata;
import org.modeshape.sequencer.javafile.metadata.Variable;

/**
 * Unit test for {@link JavaMetadata}
 * 
 * @author Serge Pagop
 * @author Horia Chiorean
 */
public class JavaMetadataTest {
    private InputStream stream;
    private JavaMetadata javaMetadata;
    private ASTNode rootNode = null;

    @Before
    public void beforeEach() throws Exception {
        File source = new File(getClass().getClassLoader().getResource("org/acme/MySource.java").toURI());
        stream = new FileInputStream(source);
        javaMetadata = JavaMetadata.instance(stream, JavaMetadataUtil.length(stream), null);
        rootNode = CompilationUnitParser.runJLS3Conversion(JavaMetadataUtil.getJavaSourceFromTheInputStream(new FileInputStream(
                                                                                                                                source),
                                                                                                            source.length(),
                                                                                                            null),
                                                           true);
    }

    @After
    public void afterEach() throws Exception {
        if (stream != null) {
            try {
                stream.close();
            } finally {
                stream = null;
            }
        }
    }

    @Test
    public void shouldGetJavaSourceFromTheInputStream() throws Exception {
        char[] c = JavaMetadataUtil.getJavaSourceFromTheInputStream(stream, JavaMetadataUtil.length(stream), null);
        assertThat(c, is(notNullValue()));
    }

    @Test
    public void shouldRunJLS3Conversion() throws Exception {
        assertThat(rootNode, is(notNullValue()));
        // Verify we get a compilation unit node and that binding are correct
        assertTrue("Not a compilation unit", rootNode.getNodeType() == ASTNode.COMPILATION_UNIT);
    }

    @Test
    public void shouldCreatePackageMetadata() throws Exception {
        PackageMetadata packageMetadata = javaMetadata.createPackageMetadata((CompilationUnit)rootNode);
        assertThat(packageMetadata, is(notNullValue()));
        assertThat(packageMetadata.getName(), is("org.acme"));
        List<AnnotationMetadata> annotations = packageMetadata.getAnnotations();
        for (AnnotationMetadata annotationMetadata : annotations) {
            assertEquals(AnnotationMetadata.Type.MARKER, annotationMetadata.getType());
            assertThat(annotationMetadata.getName(), is("org.acme.annotation.MyPackageAnnotation"));
        }
    }

    @Test
    public void shouldCreateImportMetadata() throws Exception {
        List<ImportMetadata> data = javaMetadata.createImportMetadata((CompilationUnit)rootNode);
        assertEquals(2, data.size());
        ImportMetadata singleImport = data.get(0);
        assertEquals(ImportMetadata.Type.SINGLE, singleImport.getType());
        assertEquals("org.acme.annotation.MyClassAnnotation", singleImport.getName());

        ImportMetadata onDemandImport = data.get(1);
        assertEquals(ImportMetadata.Type.ON_DEMAND, onDemandImport.getType());
        assertEquals("java.util", onDemandImport.getName());
    }

    @Test
    public void shouldCreateTopLevelTypeMetadata() throws Exception {
        List<TypeMetadata> types = javaMetadata.createTypeMetadata((CompilationUnit)rootNode);

        assertEquals(1, types.size());

        TypeMetadata typeMetadata = types.get(0);
        assertClassMetadata(typeMetadata);
        assertClassAnnotations(typeMetadata);
        assertClassFields(typeMetadata);
        assertClassMethods(typeMetadata);
    }

    private void assertClassMethods( TypeMetadata typeMetadata ) {
        // get methods (member functions)
        List<MethodMetadata> methods = typeMetadata.getMethods();
        assertEquals(10, methods.size());

        assertMethodMetadata(methods.get(0), MethodMetadata.Type.CONSTRUCTOR, new String[0], null, "MySource", 0);

        MethodMetadata methodMetadata = methods.get(1);
        assertMethodMetadata(methodMetadata, MethodMetadata.Type.CONSTRUCTOR, new String[] {PUBLIC}, null, "MySource", 2);
        List<FieldMetadata> parameters = methodMetadata.getParameters();
        assertFieldMetadata(parameters.get(0), FieldMetadata.Type.PRIMITIVE, new String[0], "int", new String[] {"i"});
        assertFieldMetadata(parameters.get(1), FieldMetadata.Type.PRIMITIVE, new String[0], "int", new String[] {"j"});

        methodMetadata = methods.get(2);
        assertMethodMetadata(methodMetadata, MethodMetadata.Type.CONSTRUCTOR, new String[] {PUBLIC}, null, "MySource", 3);
        parameters = methodMetadata.getParameters();
        assertFieldMetadata(parameters.get(0), FieldMetadata.Type.PRIMITIVE, new String[0], "int", new String[] {"i"});
        assertFieldMetadata(parameters.get(1), FieldMetadata.Type.PRIMITIVE, new String[0], "int", new String[] {"j"});
        assertFieldMetadata(parameters.get(2), FieldMetadata.Type.SIMPLE, new String[0], "Object", new String[] {"o"});

        assertMethodMetadata(methods.get(3), MethodMetadata.Type.METHOD_TYPE_MEMBER, new String[] {PUBLIC}, "int", "getI", 0);

        methodMetadata = methods.get(4);
        assertMethodMetadata(methodMetadata, MethodMetadata.Type.METHOD_TYPE_MEMBER, new String[] {PUBLIC}, "void", "setI", 1);
        parameters = methodMetadata.getParameters();
        assertFieldMetadata(parameters.get(0), FieldMetadata.Type.PRIMITIVE, new String[0], "int", new String[] {"i"});

        methodMetadata = methods.get(5);
        assertMethodMetadata(methodMetadata, MethodMetadata.Type.METHOD_TYPE_MEMBER, new String[] {PUBLIC}, "void", "setJ", 1);
        parameters = methodMetadata.getParameters();
        assertFieldMetadata(parameters.get(0), FieldMetadata.Type.PRIMITIVE, new String[0], "int", new String[] {"j"});

        methodMetadata = methods.get(6);
        assertMethodMetadata(methodMetadata,
                             MethodMetadata.Type.METHOD_TYPE_MEMBER,
                             new String[] {PUBLIC},
                             "void",
                             "doSomething",
                             3);
        parameters = methodMetadata.getParameters();
        assertFieldMetadata(parameters.get(0), FieldMetadata.Type.PRIMITIVE, new String[0], "int", new String[] {"p1"});
        assertFieldMetadata(parameters.get(1), FieldMetadata.Type.PRIMITIVE, new String[0], "double", new String[] {"p2"});
        assertFieldMetadata(parameters.get(2), FieldMetadata.Type.SIMPLE, new String[0], "Object", new String[] {"o"});

        methodMetadata = methods.get(7);
        assertMethodMetadata(methodMetadata,
                             MethodMetadata.Type.METHOD_TYPE_MEMBER,
                             new String[] {PUBLIC},
                             "void",
                             "doSomething",
                             4);
        parameters = methodMetadata.getParameters();
        assertFieldMetadata(parameters.get(0), FieldMetadata.Type.PRIMITIVE, new String[0], "int", new String[] {"p1"});
        assertFieldMetadata(parameters.get(1), FieldMetadata.Type.PRIMITIVE, new String[0], "double", new String[] {"p2"});
        assertFieldMetadata(parameters.get(2), FieldMetadata.Type.PRIMITIVE, new String[0], "float", new String[] {"p3"});
        assertFieldMetadata(parameters.get(3), FieldMetadata.Type.SIMPLE, new String[0], "Object", new String[] {"o"});

        methodMetadata = methods.get(8);
        assertMethodMetadata(methodMetadata,
                             MethodMetadata.Type.METHOD_TYPE_MEMBER,
                             new String[] {PRIVATE},
                             "double",
                             "doSomething2",
                             2);
        parameters = methodMetadata.getParameters();
        assertFieldMetadata(parameters.get(0), FieldMetadata.Type.ARRAY, new String[0], "Object", new String[] {"oa"});
        assertFieldMetadata(parameters.get(1), FieldMetadata.Type.ARRAY, new String[0], "int", new String[] {"ia"});

        methodMetadata = methods.get(9);
        assertMethodMetadata(methodMetadata,
                             MethodMetadata.Type.METHOD_TYPE_MEMBER,
                             new String[] {PUBLIC},
                             "Object",
                             "doSomething3",
                             0);
    }

    private void assertClassFields( TypeMetadata typeMetadata ) {
        // get fields (member types)
        List<FieldMetadata> fields = typeMetadata.getFields();
        assertEquals(8, fields.size());
        assertFieldMetadata(fields.get(0), FieldMetadata.Type.PRIMITIVE, new String[] {PRIVATE}, "int", new String[] {"i", "j"});
        assertFieldMetadata(fields.get(1),
                            FieldMetadata.Type.PRIMITIVE,
                            new String[] {PRIVATE, STATIC},
                            "double",
                            new String[] {"a"});
        assertFieldMetadata(fields.get(2), FieldMetadata.Type.PARAMETRIZED, new String[] {PRIVATE}, "List", new String[] {"l"});
        assertFieldMetadata(fields.get(3), FieldMetadata.Type.PARAMETRIZED, new String[] {PRIVATE}, "A", new String[] {"o"});
        assertFieldMetadata(fields.get(4), FieldMetadata.Type.SIMPLE, new String[] {PRIVATE}, "X", new String[] {"x"});
        assertFieldMetadata(fields.get(5), FieldMetadata.Type.ARRAY, new String[] {PRIVATE}, "int", new String[] {"ia"});
        assertFieldMetadata(fields.get(6), FieldMetadata.Type.ARRAY, new String[] {PRIVATE}, "Object", new String[] {"oa"});
        assertFieldMetadata(fields.get(7), FieldMetadata.Type.ARRAY, new String[] {PRIVATE}, "Collection", new String[] {"ca"});
    }

    private void assertClassAnnotations( TypeMetadata typeMetadata ) {
        // annotations of the top level class
        List<AnnotationMetadata> annotations = typeMetadata.getAnnotations();
        assertEquals(1, annotations.size());

        AnnotationMetadata annotationMetadata = annotations.get(0);
        assertEquals(AnnotationMetadata.Type.MARKER, annotationMetadata.getType());
        assertThat(annotationMetadata.getName(), is("MyClassAnnotation"));
    }

    private void assertClassMetadata( TypeMetadata typeMetadata ) {
        assertEquals(TypeMetadata.Type.CLASS, typeMetadata.getType());
        // meta types of a top level class
        assertThat(typeMetadata.getName(), is("MySource"));
        // modifiers of the top level class
        assertNotNull(typeMetadata.getModifiers());
        assertFalse(typeMetadata.getModifiers().isEmpty());
        assertThat(typeMetadata.getModifiers().get(0).getName(), is(PUBLIC));
    }

    private void assertFieldMetadata( FieldMetadata fieldMetadata,
                                      FieldMetadata.Type expectedMetaType,
                                      String[] expectedModifiers,
                                      String expectedTypeName,
                                      String[] expectedVariables ) {
        assertEquals(expectedMetaType, fieldMetadata.getMetadataType());
        assertEquals(expectedTypeName, fieldMetadata.getType());

        List<ModifierMetadata> modifiers = fieldMetadata.getModifiers();
        assertEquals(expectedModifiers.length, modifiers.size());
        for (int i = 0; i < expectedModifiers.length; i++) {
            assertEquals(expectedModifiers[i], modifiers.get(i).getName());
        }

        List<Variable> variables = fieldMetadata.getVariables();
        assertEquals(expectedVariables.length, variables.size());
        for (int i = 0; i < expectedVariables.length; i++) {
            assertEquals(expectedVariables[i], variables.get(i).getName());
        }
    }

    private void assertMethodMetadata( MethodMetadata methodMetadata,
                                       MethodMetadata.Type expectedMetaType,
                                       String[] expectedModifiers,
                                       String returnType,
                                       String name,
                                       int expectedParamCount ) {
        assertEquals(expectedMetaType, methodMetadata.getType());
        if (returnType != null) {
            assertEquals(returnType, methodMetadata.getReturnType().getType());
        } else {
            assertNull(methodMetadata.getReturnType());
        }

        assertEquals(methodMetadata.getName(), name);

        List<ModifierMetadata> modifiers = methodMetadata.getModifiers();
        assertEquals(expectedModifiers.length, modifiers.size());
        for (int i = 0; i < expectedModifiers.length; i++) {
            assertEquals(expectedModifiers[i], modifiers.get(i).getName());
        }
        assertEquals(expectedParamCount, methodMetadata.getParameters().size());
    }

}
