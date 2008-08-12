/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.sequencer.java;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.dna.sequencer.java.metadata.AnnotationMetadata;
import org.jboss.dna.sequencer.java.metadata.ClassMetadata;
import org.jboss.dna.sequencer.java.metadata.ConstructorMetadata;
import org.jboss.dna.sequencer.java.metadata.FieldMetadata;
import org.jboss.dna.sequencer.java.metadata.ImportMetadata;
import org.jboss.dna.sequencer.java.metadata.ImportOnDemandMetadata;
import org.jboss.dna.sequencer.java.metadata.JavaMetadata;
import org.jboss.dna.sequencer.java.metadata.MarkerAnnotationMetadata;
import org.jboss.dna.sequencer.java.metadata.MethodMetadata;
import org.jboss.dna.sequencer.java.metadata.MethodTypeMemberMetadata;
import org.jboss.dna.sequencer.java.metadata.PackageMetadata;
import org.jboss.dna.sequencer.java.metadata.PrimitiveFieldMetadata;
import org.jboss.dna.sequencer.java.metadata.ReferenceFieldMetadata;
import org.jboss.dna.sequencer.java.metadata.SingleImportMetadata;
import org.jboss.dna.sequencer.java.metadata.TypeMetadata;
import org.jboss.dna.sequencer.java.metadata.Variable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Serge Pagop
 */
public class JavaMetadataTest {
    private File source;
    private InputStream stream;
    private JavaMetadata javaMetadata;
    private ASTNode rootNode = null;

    @Before
    public void beforeEach() throws Exception {
        source = new File("src/test/workspace/projectX/src/org/acme/MySource.java");
        stream = getJavaSrc(source);
        javaMetadata = JavaMetadata.instance(stream, JavaMetadataUtil.length(stream), null, null);
        rootNode = CompilationUnitParser.runJLS3Conversion(JavaMetadataUtil.getJavaSourceFromTheInputStream(getJavaSrc(source),
                                                                                                            source.length(),
                                                                                                            null), true);
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

    protected FileInputStream getJavaSrc( File file ) throws FileNotFoundException {
        return new FileInputStream(file);
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
        List<AnnotationMetadata> annotations = packageMetadata.getAnnotationMetada();
        for (AnnotationMetadata annotationMetadata : annotations) {
            if (annotationMetadata instanceof MarkerAnnotationMetadata) {
                MarkerAnnotationMetadata maker = (MarkerAnnotationMetadata)annotationMetadata;
                assertThat(maker.getName(), is("org.acme.annotation.MyPackageAnnotation"));

            }
        }
    }

    @Test
    public void shouldCreateImportMetadata() throws Exception {
        List<ImportMetadata> data = javaMetadata.createImportMetadata((CompilationUnit)rootNode);
        for (Iterator<ImportMetadata> i = data.iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof ImportOnDemandMetadata) {
                ImportOnDemandMetadata onDemand = (ImportOnDemandMetadata)o;
                assertThat(onDemand.getName(), is("java.util"));
            } else {
                SingleImportMetadata singleImport = (SingleImportMetadata)o;
                assertThat(singleImport.getName(), is("org.acme.annotation.MyClassAnnotation"));
            }

        }
    }

    @Test
    public void shouldCreateTopLevelTypeMetadata() throws Exception {
        List<TypeMetadata> data = javaMetadata.createTypeMetadata((CompilationUnit)rootNode);
        assertTrue(data.size() > 0);
        for (TypeMetadata typeMetadata : data) {
            // meta data of a top level class
            if (typeMetadata instanceof ClassMetadata) {
                ClassMetadata classMetadata = (ClassMetadata)typeMetadata;
                assertThat(classMetadata.getName(), is("MySource"));
                // modifiers of the top level class
                assertNotNull(classMetadata.getModifiers());
                assertTrue(!classMetadata.getModifiers().isEmpty());
                assertThat(classMetadata.getModifiers().get(0).getName(), is("public"));
                // annotations of the top level class
                List<AnnotationMetadata> annotations = classMetadata.getAnnotations();
                for (AnnotationMetadata annotationMetadata : annotations) {
                    if (annotationMetadata instanceof MarkerAnnotationMetadata) {
                        MarkerAnnotationMetadata marker = (MarkerAnnotationMetadata)annotationMetadata;
                        assertNotNull(marker);
                        assertThat(marker.getName(), is("MyClassAnnotation"));
                    }
                }
                // get fields
                List<FieldMetadata> fields = classMetadata.getFields();
                assertNotNull(fields);
                assertTrue(fields.size() == 5);

                PrimitiveFieldMetadata primitiveFieldMetadata = (PrimitiveFieldMetadata)fields.get(0);
                assertTrue(primitiveFieldMetadata.getModifiers().size() == 1);
                assertThat(primitiveFieldMetadata.getType(), is("int"));
                assertThat(primitiveFieldMetadata.getVariables().get(0).getName(), is("i"));
                assertThat(primitiveFieldMetadata.getVariables().get(1).getName(), is("j"));

                PrimitiveFieldMetadata primitiveFieldMetadata2 = (PrimitiveFieldMetadata)fields.get(1);
                assertTrue(primitiveFieldMetadata2.getModifiers().size() == 2);
                assertThat(primitiveFieldMetadata2.getType(), is("double"));
                assertThat(primitiveFieldMetadata2.getVariables().get(0).getName(), is("a"));

                ReferenceFieldMetadata parameterizedFieldMetadata1 = (ReferenceFieldMetadata)fields.get(2);
                assertNotNull(parameterizedFieldMetadata1);
                assertTrue(parameterizedFieldMetadata1.getModifiers().size() == 1);
                assertThat(parameterizedFieldMetadata1.getType(), is("List"));
                assertThat(parameterizedFieldMetadata1.getVariables().get(0).getName(), is("l"));

                ReferenceFieldMetadata parameterizedFieldMetadata2 = (ReferenceFieldMetadata)fields.get(3);
                assertNotNull(parameterizedFieldMetadata2);
                assertTrue(parameterizedFieldMetadata2.getModifiers().size() == 1);
                assertThat(parameterizedFieldMetadata2.getType(), is("A"));
                assertThat(parameterizedFieldMetadata2.getVariables().get(0).getName(), is("o"));

                ReferenceFieldMetadata simpleFieldMetadata = (ReferenceFieldMetadata)fields.get(4);
                assertNotNull(simpleFieldMetadata);
                assertTrue(simpleFieldMetadata.getModifiers().size() == 1);
                assertThat(simpleFieldMetadata.getType(), is("X"));
                assertThat(simpleFieldMetadata.getVariables().get(0).getName(), is("x"));

                // get methods
                List<MethodMetadata> methods = classMetadata.getMethods();
                assertNotNull(methods);
                assertTrue(methods.size() == 6);

                MethodMetadata methodMetadata = methods.get(0);
                ConstructorMetadata constructorMetadata = (ConstructorMetadata)methodMetadata;
                assertNotNull(constructorMetadata);
                assertTrue(constructorMetadata.getModifiers().size() == 0);
                assertThat(constructorMetadata.getName(), is("MySource"));
                assertTrue(constructorMetadata.getParameters().size() == 0);

                MethodMetadata methodMetadata2 = methods.get(1);
                ConstructorMetadata constructorMetadata2 = (ConstructorMetadata)methodMetadata2;
                assertNotNull(constructorMetadata2);
                assertTrue(constructorMetadata2.getModifiers().size() == 1);
                assertThat(constructorMetadata2.getName(), is("MySource"));
                assertTrue(constructorMetadata2.getParameters().size() == 2);

                MethodTypeMemberMetadata methodTypeMemberMetadata1 = (MethodTypeMemberMetadata)methods.get(2);
                assertTrue(methodTypeMemberMetadata1.getModifiers().size() == 1);
                assertEquals(methodTypeMemberMetadata1.getReturnType().getName(), "int");
                assertNotNull(methodTypeMemberMetadata1);
                assertThat(methodTypeMemberMetadata1.getName(), is("getI"));
                assertTrue(methodTypeMemberMetadata1.getParameters().size() == 0);

                MethodTypeMemberMetadata methodTypeMemberMetadata3 = (MethodTypeMemberMetadata)methods.get(3);
                assertTrue(methodTypeMemberMetadata3.getModifiers().size() == 1);
                assertEquals(methodTypeMemberMetadata3.getReturnType().getName(), "void");
                assertNotNull(methodTypeMemberMetadata3);
                assertThat(methodTypeMemberMetadata3.getName(), is("setI"));
                assertTrue(methodTypeMemberMetadata3.getParameters().size() == 1);
                

                MethodTypeMemberMetadata methodTypeMemberMetadata4 = (MethodTypeMemberMetadata)methods.get(4);
                assertTrue(methodTypeMemberMetadata4.getModifiers().size() == 1);
                assertEquals(methodTypeMemberMetadata4.getReturnType().getName(), "void");
                assertNotNull(methodTypeMemberMetadata4);
                assertThat(methodTypeMemberMetadata4.getName(), is("setJ"));
                assertTrue(methodTypeMemberMetadata4.getParameters().size() == 1);

                MethodTypeMemberMetadata methodTypeMemberMetadata5 = (MethodTypeMemberMetadata)methods.get(5);
                assertTrue(methodTypeMemberMetadata5.getModifiers().size() == 1);
                assertEquals(methodTypeMemberMetadata5.getReturnType().getName(), "void");
                assertNotNull(methodTypeMemberMetadata5);
                assertThat(methodTypeMemberMetadata5.getName(), is("doSomething"));
                assertTrue(methodTypeMemberMetadata5.getParameters().size() == 2);
            }
        }
    }

}
