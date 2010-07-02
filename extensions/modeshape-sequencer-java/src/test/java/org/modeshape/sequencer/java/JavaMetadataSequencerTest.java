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
package org.modeshape.sequencer.java;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.sequencer.MockSequencerContext;
import org.modeshape.graph.sequencer.MockSequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencerContext;

/**
 * @author Serge Pagop
 * @author John Verhaeg
 */
public class JavaMetadataSequencerTest {
    private JavaMetadataSequencer sequencer;
    private InputStream content;
    private MockSequencerOutput output;
    private File source;
    private StreamSequencerContext context;
    private PathFactory pathFactory;

    @Before
    public void beforeEach() {
        pathFactory = new ExecutionContext().getValueFactories().getPathFactory();
        context = new MockSequencerContext();
        context.getNamespaceRegistry().register(JavaMetadataLexicon.Namespace.PREFIX, JavaMetadataLexicon.Namespace.URI);
        sequencer = new JavaMetadataSequencer();
        output = new MockSequencerOutput(context);
        source = new File("src/test/workspace/projectX/src/org/acme/MySource.java");
    }

    @After
    public void afterEach() throws Exception {
        if (content != null) {
            try {
                content.close();
            } finally {
                content = null;
            }
        }
    }

    protected FileInputStream getJavaSrc( File file ) throws FileNotFoundException {
        return new FileInputStream(file);
    }

    private Path path( Name... names ) {
        return pathFactory.createRelativePath(names);
    }

    private Path path( Path path,
                       Name name,
                       int index ) {
        return pathFactory.create(path, pathFactory.createSegment(name, index));
    }

    private Path path( Path path,
                       Name... names ) {
        for (Name name : names) {
            path = pathFactory.create(path, name);
        }

        return path;
    }

    @Test
    public void shouldGenerateMetadataForJavaSourceFile() throws IOException {
        content = getJavaSrc(source);
        assertThat(content, is(notNullValue()));
        sequencer.setSourceFileRecorder(new OriginalFormatSourceFileRecorder());
        sequencer.sequence(content, output, context);
        assertThat(output.getPropertyValues(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE), "jcr:primaryType"),
                   is(new Object[] {JavaMetadataLexicon.COMPILATION_UNIT_NODE}));

        // support sequencing package declaration( FQL name of the package). Not supported is to get information for package
        // annotation
        // from package-info.java
        assertThat(output.getPropertyValues(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                 JavaMetadataLexicon.PACKAGE_CHILD_NODE,
                                                 JavaMetadataLexicon.PACKAGE_DECLARATION_CHILD_NODE), "java:packageName"),
                   is(new Object[] {"org.acme"}));

        // TODO (find a solution to get the annotation of a package). Java Sequencer does not yet support sequencing of
        // package-info.java with package annotations
        // assertThat(output.getPropertyValues(
        // path(JavaMetadataLexicon.COMPILATION_UNIT_NODE, JavaMetadataLexicon.PACKAGE_CHILD_NODE,
        // JavaMetadataLexicon.PACKAGE_NAME/java:annotation/java:annotationDeclaration/java:annotationType/java:markerAnnotation[1]"
        // ,
        // "java:typeName"),
        // is(new Object[] {"org.acme.annotation.MyPackageAnnotation"}));

        // support for sequencing imports (single import and import on demand)
        assertThat(output.getPropertyValues(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                      JavaMetadataLexicon.IMPORT_CHILD_NODE,
                                                      JavaMetadataLexicon.IMPORT_DECLARATION_CHILD_NODE,
                                                      JavaMetadataLexicon.SINGLE_IMPORT_CHILD_NODE),
                                                 JavaMetadataLexicon.SINGLE_IMPORT_TYPE_DECLARATION_CHILD_NODE,
                                                 1), "java:singleImportName"),
                   is(new Object[] {"org.acme.annotation.MyClassAnnotation"}));
        assertThat(output.getPropertyValues(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                      JavaMetadataLexicon.IMPORT_CHILD_NODE,
                                                      JavaMetadataLexicon.IMPORT_DECLARATION_CHILD_NODE,
                                                      JavaMetadataLexicon.ON_DEMAND_IMPORT_CHILD_NODE),
                                                 JavaMetadataLexicon.ON_DEMAND_IMPORT_TYPE_DECLARATION_CHILD_NODE,
                                                 1), "java:onDemandImportName"), is(new Object[] {"java.util"}));

        // support for sequencing class definition (modifiers, class name)
        assertThat(output.getPropertyValues(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                      JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                      JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                      JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                      JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                      JavaMetadataLexicon.MODIFIER_CHILD_NODE),
                                                 JavaMetadataLexicon.MODIFIER_DECLARATION_CHILD_NODE,
                                                 1), "java:modifierName"), is(new Object[] {"public"}));
        assertThat(output.getPropertyValues(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                 JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                 JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE), "java:normalClassName"),
                   is(new Object[] {"MySource"}));

        // support for primitive type sequencing (modifiers, types, variables).Not supported is the javadoc
        assertThat(output.getPropertyValues(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                 JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                 JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.FIELD_CHILD_NODE,
                                                 JavaMetadataLexicon.FIELD_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.PRIMITIVE_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.MODIFIER_CHILD_NODE,
                                                 JavaMetadataLexicon.MODIFIER_DECLARATION_CHILD_NODE), "java:modifierName"),
                   is(new Object[] {"private"}));

        assertThat(output.getPropertyValues(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                 JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                 JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.FIELD_CHILD_NODE,
                                                 JavaMetadataLexicon.FIELD_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.PRIMITIVE_TYPE_CHILD_NODE), "java:primitiveTypeName"),
                   is(new Object[] {"int"}));

        assertThat(output.getPropertyValues(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                 JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                 JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.FIELD_CHILD_NODE,
                                                 JavaMetadataLexicon.FIELD_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.PRIMITIVE_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.PRIMITIVE_TYPE_VARIABLE,
                                                 JavaMetadataLexicon.VARIABLE), "java:variableName"), is(new Object[] {"i"}));

        assertThat(output.getPropertyValues(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                      JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                      JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                      JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                      JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                      JavaMetadataLexicon.FIELD_CHILD_NODE,
                                                      JavaMetadataLexicon.FIELD_TYPE_CHILD_NODE,
                                                      JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                      JavaMetadataLexicon.PRIMITIVE_TYPE_CHILD_NODE,
                                                      JavaMetadataLexicon.PRIMITIVE_TYPE_VARIABLE),
                                                 JavaMetadataLexicon.VARIABLE,
                                                 2), "java:variableName"), is(new Object[] {"j"}));

        assertThat(output.getPropertyValues(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                           JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                           JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.FIELD_CHILD_NODE,
                                                           JavaMetadataLexicon.FIELD_TYPE_CHILD_NODE,
                                                           JavaMetadataLexicon.TYPE_CHILD_NODE),
                                                      JavaMetadataLexicon.PRIMITIVE_TYPE_CHILD_NODE,
                                                      2),
                                                 JavaMetadataLexicon.MODIFIER_CHILD_NODE,
                                                 JavaMetadataLexicon.MODIFIER_DECLARATION_CHILD_NODE), "java:modifierName"),
                   is(new Object[] {"private"}));

        assertThat(output.getPropertyValues(path(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                                JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                                JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                                JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                                JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                                JavaMetadataLexicon.FIELD_CHILD_NODE,
                                                                JavaMetadataLexicon.FIELD_TYPE_CHILD_NODE,
                                                                JavaMetadataLexicon.TYPE_CHILD_NODE),
                                                           JavaMetadataLexicon.PRIMITIVE_TYPE_CHILD_NODE,
                                                           2), JavaMetadataLexicon.MODIFIER_CHILD_NODE),
                                                 JavaMetadataLexicon.MODIFIER_DECLARATION_CHILD_NODE,
                                                 2), "java:modifierName"), is(new Object[] {"static"}));

        assertThat(output.getPropertyValues(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                      JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                      JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                      JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                      JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                      JavaMetadataLexicon.FIELD_CHILD_NODE,
                                                      JavaMetadataLexicon.FIELD_TYPE_CHILD_NODE,
                                                      JavaMetadataLexicon.TYPE_CHILD_NODE),
                                                 JavaMetadataLexicon.PRIMITIVE_TYPE_CHILD_NODE,
                                                 2), "java:primitiveTypeName"), is(new Object[] {"double"}));

        assertThat(output.getPropertyValues(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                           JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                           JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.FIELD_CHILD_NODE,
                                                           JavaMetadataLexicon.FIELD_TYPE_CHILD_NODE,
                                                           JavaMetadataLexicon.TYPE_CHILD_NODE),
                                                      JavaMetadataLexicon.PRIMITIVE_TYPE_CHILD_NODE,
                                                      2),
                                                 JavaMetadataLexicon.PRIMITIVE_TYPE_VARIABLE,
                                                 JavaMetadataLexicon.VARIABLE), "java:variableName"), is(new Object[] {"a"}));

        // support for reference type sequencing ()

        assertThat(output.getPropertyValues(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                 JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                 JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.FIELD_CHILD_NODE,
                                                 JavaMetadataLexicon.FIELD_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.PARAMETERIZED_TYPE_MODIFIER_CHILD_NODE),
                                            "java:parameterizedTypeName"), is(new Object[] {"List"}));

        assertThat(output.getPropertyValues(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                      JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                      JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                      JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                      JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                      JavaMetadataLexicon.FIELD_CHILD_NODE,
                                                      JavaMetadataLexicon.FIELD_TYPE_CHILD_NODE,
                                                      JavaMetadataLexicon.TYPE_CHILD_NODE),
                                                 JavaMetadataLexicon.PARAMETERIZED_TYPE_MODIFIER_CHILD_NODE,
                                                 2), "java:parameterizedTypeName"), is(new Object[] {"A"}));
        assertThat(output.getPropertyValues(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                 JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                 JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.FIELD_CHILD_NODE,
                                                 JavaMetadataLexicon.FIELD_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.SIMPLE_TYPE_CHILD_NODE), "java:simpleTypeName"),
                   is(new Object[] {"X"}));
        assertThat(output.getPropertyValues(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                 JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                 JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.FIELD_CHILD_NODE,
                                                 JavaMetadataLexicon.FIELD_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.SIMPLE_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.SIMPLE_TYPE_VARIABLE,
                                                 JavaMetadataLexicon.VARIABLE), "java:variableName"), is(new Object[] {"x"}));

        assertThat(output.getPropertyValues(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                 JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                 JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.FIELD_CHILD_NODE,
                                                 JavaMetadataLexicon.FIELD_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.ARRAY_TYPE_CHILD_NODE), "java:arrayTypeName"),
                   is(new Object[] {"int"}));
        assertThat(output.getPropertyValues(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                 JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                 JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.FIELD_CHILD_NODE,
                                                 JavaMetadataLexicon.FIELD_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.ARRAY_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.ARRAY_TYPE_VARIABLE,
                                                 JavaMetadataLexicon.VARIABLE), "java:variableName"), is(new Object[] {"ia"}));

        assertThat(output.getPropertyValues(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                      JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                      JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                      JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                      JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                      JavaMetadataLexicon.FIELD_CHILD_NODE,
                                                      JavaMetadataLexicon.FIELD_TYPE_CHILD_NODE,
                                                      JavaMetadataLexicon.TYPE_CHILD_NODE),
                                                 JavaMetadataLexicon.ARRAY_TYPE_CHILD_NODE,
                                                 2), "java:arrayTypeName"), is(new Object[] {"Object"}));
        assertThat(output.getPropertyValues(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                           JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                           JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.FIELD_CHILD_NODE,
                                                           JavaMetadataLexicon.FIELD_TYPE_CHILD_NODE,
                                                           JavaMetadataLexicon.TYPE_CHILD_NODE),
                                                      JavaMetadataLexicon.ARRAY_TYPE_CHILD_NODE,
                                                      2), JavaMetadataLexicon.ARRAY_TYPE_VARIABLE, JavaMetadataLexicon.VARIABLE),
                                            "java:variableName"), is(new Object[] {"oa"}));

        assertThat(output.getPropertyValues(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                      JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                      JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                      JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                      JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                      JavaMetadataLexicon.FIELD_CHILD_NODE,
                                                      JavaMetadataLexicon.FIELD_TYPE_CHILD_NODE,
                                                      JavaMetadataLexicon.TYPE_CHILD_NODE),
                                                 JavaMetadataLexicon.ARRAY_TYPE_CHILD_NODE,
                                                 3), "java:arrayTypeName"), is(new Object[] {"Collection"}));
        assertThat(output.getPropertyValues(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                           JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                           JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.FIELD_CHILD_NODE,
                                                           JavaMetadataLexicon.FIELD_TYPE_CHILD_NODE,
                                                           JavaMetadataLexicon.TYPE_CHILD_NODE),
                                                      JavaMetadataLexicon.ARRAY_TYPE_CHILD_NODE,
                                                      3), JavaMetadataLexicon.ARRAY_TYPE_VARIABLE, JavaMetadataLexicon.VARIABLE),
                                            "java:variableName"), is(new Object[] {"ca"}));

        // support for methods sequencing (modifiers, return type, method name, parameters).Not supported are javadoc

        // MySource() constructor
        assertThat(output.getPropertyValues(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                 JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                 JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.CONSTRUCTOR_CHILD_NODE,
                                                 JavaMetadataLexicon.CONSTRUCTOR_DECLARATION_CHILD_NODE), "java:constructorName"),
                   is(new Object[] {"MySource"}));

        // public MySource(int i, int j, Object 0) constructor with parameters
        assertThat(output.getPropertyValues(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                           JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                           JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.CONSTRUCTOR_CHILD_NODE),
                                                      JavaMetadataLexicon.CONSTRUCTOR_DECLARATION_CHILD_NODE,
                                                      2),
                                                 JavaMetadataLexicon.MODIFIER_CHILD_NODE,
                                                 JavaMetadataLexicon.MODIFIER_DECLARATION_CHILD_NODE), "java:modifierName"),
                   is(new Object[] {"public"}));

        assertThat(output.getPropertyValues(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                      JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                      JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                      JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                      JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                      JavaMetadataLexicon.CONSTRUCTOR_CHILD_NODE),
                                                 JavaMetadataLexicon.CONSTRUCTOR_DECLARATION_CHILD_NODE,
                                                 2), "java:constructorName"), is(new Object[] {"MySource"}));

        assertThat(output.getPropertyValues(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                           JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                           JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.CONSTRUCTOR_CHILD_NODE),
                                                      JavaMetadataLexicon.CONSTRUCTOR_DECLARATION_CHILD_NODE,
                                                      2),
                                                 JavaMetadataLexicon.PARAMETER,
                                                 JavaMetadataLexicon.FORMAL_PARAMETER,
                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.PRIMITIVE_TYPE_CHILD_NODE), "java:primitiveTypeName"),
                   is(new Object[] {"int"}));
        assertThat(output.getPropertyValues(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                           JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                           JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.CONSTRUCTOR_CHILD_NODE),
                                                      JavaMetadataLexicon.CONSTRUCTOR_DECLARATION_CHILD_NODE,
                                                      2),
                                                 JavaMetadataLexicon.PARAMETER,
                                                 JavaMetadataLexicon.FORMAL_PARAMETER,
                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.PRIMITIVE_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.PRIMITIVE_TYPE_VARIABLE,
                                                 JavaMetadataLexicon.VARIABLE), "java:variableName"), is(new Object[] {"i"}));

        assertThat(output.getPropertyValues(path(path(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                                     JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                                     JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                                     JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                                     JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                                     JavaMetadataLexicon.CONSTRUCTOR_CHILD_NODE),
                                                                JavaMetadataLexicon.CONSTRUCTOR_DECLARATION_CHILD_NODE,
                                                                2), JavaMetadataLexicon.PARAMETER),
                                                      JavaMetadataLexicon.FORMAL_PARAMETER,
                                                      2),
                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.PRIMITIVE_TYPE_CHILD_NODE), "java:primitiveTypeName"),
                   is(new Object[] {"int"}));
        assertThat(output.getPropertyValues(path(path(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                                     JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                                     JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                                     JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                                     JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                                     JavaMetadataLexicon.CONSTRUCTOR_CHILD_NODE),
                                                                JavaMetadataLexicon.CONSTRUCTOR_DECLARATION_CHILD_NODE,
                                                                2), JavaMetadataLexicon.PARAMETER),
                                                      JavaMetadataLexicon.FORMAL_PARAMETER,
                                                      2),
                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.PRIMITIVE_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.PRIMITIVE_TYPE_VARIABLE,
                                                 JavaMetadataLexicon.VARIABLE), "java:variableName"), is(new Object[] {"j"}));

        assertThat(output.getPropertyValues(path(path(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                                     JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                                     JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                                     JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                                     JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                                     JavaMetadataLexicon.CONSTRUCTOR_CHILD_NODE),
                                                                JavaMetadataLexicon.CONSTRUCTOR_DECLARATION_CHILD_NODE,
                                                                2), JavaMetadataLexicon.PARAMETER),
                                                      JavaMetadataLexicon.FORMAL_PARAMETER,
                                                      3),
                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.SIMPLE_TYPE_CHILD_NODE), "java:simpleTypeName"),
                   is(new Object[] {"Object"}));
        assertThat(output.getPropertyValues(path(path(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                                     JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                                     JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                                     JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                                     JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                                     JavaMetadataLexicon.CONSTRUCTOR_CHILD_NODE),
                                                                JavaMetadataLexicon.CONSTRUCTOR_DECLARATION_CHILD_NODE,
                                                                2), JavaMetadataLexicon.PARAMETER),
                                                      JavaMetadataLexicon.FORMAL_PARAMETER,
                                                      3),
                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.SIMPLE_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.SIMPLE_TYPE_VARIABLE,
                                                 JavaMetadataLexicon.VARIABLE), "java:variableName"), is(new Object[] {"o"}));

        // public int getI() method
        assertThat(output.getPropertyValues(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                 JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                 JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.METHOD_CHILD_NODE,
                                                 JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.MODIFIER_CHILD_NODE,
                                                 JavaMetadataLexicon.MODIFIER_DECLARATION_CHILD_NODE), "java:modifierName"),
                   is(new Object[] {"public"}));
        assertThat(output.getPropertyValues(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                 JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                 JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.METHOD_CHILD_NODE,
                                                 JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.RETURN_TYPE,
                                                 JavaMetadataLexicon.PRIMITIVE_TYPE_CHILD_NODE), "java:primitiveTypeName"),
                   is(new Object[] {"int"}));
        assertThat(output.getPropertyValues(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                 JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                 JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                 JavaMetadataLexicon.METHOD_CHILD_NODE,
                                                 JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE), "java:methodName"),
                   is(new Object[] {"getI"}));

        // public void setI(int i) method
        assertThat(output.getPropertyValues(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                           JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                           JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.METHOD_CHILD_NODE),
                                                      JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                      2),
                                                 JavaMetadataLexicon.MODIFIER_CHILD_NODE,
                                                 JavaMetadataLexicon.MODIFIER_DECLARATION_CHILD_NODE), "java:modifierName"),
                   is(new Object[] {"public"}));
        assertThat(output.getPropertyValues(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                           JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                           JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.METHOD_CHILD_NODE),
                                                      JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                      2),
                                                 JavaMetadataLexicon.RETURN_TYPE,
                                                 JavaMetadataLexicon.PRIMITIVE_TYPE_CHILD_NODE), "java:primitiveTypeName"),
                   is(new Object[] {"void"}));
        assertThat(output.getPropertyValues(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                      JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                      JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                      JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                      JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                      JavaMetadataLexicon.METHOD_CHILD_NODE),
                                                 JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                 2), "java:methodName"), is(new Object[] {"setI"}));
        assertThat(output.getPropertyValues(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                           JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                           JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.METHOD_CHILD_NODE),
                                                      JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                      2),
                                                 JavaMetadataLexicon.PARAMETER,
                                                 JavaMetadataLexicon.FORMAL_PARAMETER,
                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.PRIMITIVE_TYPE_CHILD_NODE), "java:primitiveTypeName"),
                   is(new Object[] {"int"}));
        assertThat(output.getPropertyValues(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                           JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                           JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.METHOD_CHILD_NODE),
                                                      JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                      2),
                                                 JavaMetadataLexicon.PARAMETER,
                                                 JavaMetadataLexicon.FORMAL_PARAMETER,
                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.PRIMITIVE_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.PRIMITIVE_TYPE_VARIABLE,
                                                 JavaMetadataLexicon.VARIABLE), "java:variableName"), is(new Object[] {"i"}));

        // public void doSomething(int p1, int p2, Object o) method
        assertThat(output.getPropertyValues(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                           JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                           JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.METHOD_CHILD_NODE),
                                                      JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                      4),
                                                 JavaMetadataLexicon.MODIFIER_CHILD_NODE,
                                                 JavaMetadataLexicon.MODIFIER_DECLARATION_CHILD_NODE), "java:modifierName"),
                   is(new Object[] {"public"}));
        assertThat(output.getPropertyValues(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                           JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                           JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.METHOD_CHILD_NODE),
                                                      JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                      4),
                                                 JavaMetadataLexicon.RETURN_TYPE,
                                                 JavaMetadataLexicon.PRIMITIVE_TYPE_CHILD_NODE), "java:primitiveTypeName"),
                   is(new Object[] {"void"}));
        assertThat(output.getPropertyValues(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                      JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                      JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                      JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                      JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                      JavaMetadataLexicon.METHOD_CHILD_NODE),
                                                 JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                 4), "java:methodName"), is(new Object[] {"doSomething"}));
        assertThat(output.getPropertyValues(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                           JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                           JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.METHOD_CHILD_NODE),
                                                      JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                      4),
                                                 JavaMetadataLexicon.PARAMETER,
                                                 JavaMetadataLexicon.FORMAL_PARAMETER,
                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.PRIMITIVE_TYPE_CHILD_NODE), "java:primitiveTypeName"),
                   is(new Object[] {"int"}));
        assertThat(output.getPropertyValues(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                           JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                           JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.METHOD_CHILD_NODE),
                                                      JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                      4),
                                                 JavaMetadataLexicon.PARAMETER,
                                                 JavaMetadataLexicon.FORMAL_PARAMETER,
                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.PRIMITIVE_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.PRIMITIVE_TYPE_VARIABLE,
                                                 JavaMetadataLexicon.VARIABLE), "java:variableName"), is(new Object[] {"p1"}));
        assertThat(output.getPropertyValues(path(path(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                                     JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                                     JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                                     JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                                     JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                                     JavaMetadataLexicon.METHOD_CHILD_NODE),
                                                                JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                                4), JavaMetadataLexicon.PARAMETER),
                                                      JavaMetadataLexicon.FORMAL_PARAMETER,
                                                      2),
                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.PRIMITIVE_TYPE_CHILD_NODE), "java:primitiveTypeName"),
                   is(new Object[] {"double"}));
        assertThat(output.getPropertyValues(path(path(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                                     JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                                     JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                                     JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                                     JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                                     JavaMetadataLexicon.METHOD_CHILD_NODE),
                                                                JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                                4), JavaMetadataLexicon.PARAMETER),
                                                      JavaMetadataLexicon.FORMAL_PARAMETER,
                                                      2),
                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.PRIMITIVE_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.PRIMITIVE_TYPE_VARIABLE,
                                                 JavaMetadataLexicon.VARIABLE), "java:variableName"), is(new Object[] {"p2"}));

        assertThat(output.getPropertyValues(path(path(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                                     JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                                     JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                                     JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                                     JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                                     JavaMetadataLexicon.METHOD_CHILD_NODE),
                                                                JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                                4), JavaMetadataLexicon.PARAMETER),
                                                      JavaMetadataLexicon.FORMAL_PARAMETER,
                                                      3),
                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.SIMPLE_TYPE_CHILD_NODE), "java:simpleTypeName"),
                   is(new Object[] {"Object"}));
        assertThat(output.getPropertyValues(path(path(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                                     JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                                     JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                                     JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                                     JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                                     JavaMetadataLexicon.METHOD_CHILD_NODE),
                                                                JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                                4), JavaMetadataLexicon.PARAMETER),
                                                      JavaMetadataLexicon.FORMAL_PARAMETER,
                                                      3),
                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.SIMPLE_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.SIMPLE_TYPE_VARIABLE,
                                                 JavaMetadataLexicon.VARIABLE), "java:variableName"), is(new Object[] {"o"}));

        // private double doSomething2(Object[] oa, int[] ia) method
        assertThat(output.getPropertyValues(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                           JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                           JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.METHOD_CHILD_NODE),
                                                      JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                      5),
                                                 JavaMetadataLexicon.MODIFIER_CHILD_NODE,
                                                 JavaMetadataLexicon.MODIFIER_DECLARATION_CHILD_NODE), "java:modifierName"),
                   is(new Object[] {"private"}));
        assertThat(output.getPropertyValues(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                           JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                           JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.METHOD_CHILD_NODE),
                                                      JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                      5),
                                                 JavaMetadataLexicon.RETURN_TYPE,
                                                 JavaMetadataLexicon.PRIMITIVE_TYPE_CHILD_NODE), "java:primitiveTypeName"),
                   is(new Object[] {"double"}));
        assertThat(output.getPropertyValues(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                      JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                      JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                      JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                      JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                      JavaMetadataLexicon.METHOD_CHILD_NODE),
                                                 JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                 5), "java:methodName"), is(new Object[] {"doSomething2"}));
        assertThat(output.getPropertyValues(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                           JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                           JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.METHOD_CHILD_NODE),
                                                      JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                      5),
                                                 JavaMetadataLexicon.PARAMETER,
                                                 JavaMetadataLexicon.FORMAL_PARAMETER,
                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.ARRAY_TYPE_CHILD_NODE), "java:arrayTypeName"),
                   is(new Object[] {"Object"}));
        assertThat(output.getPropertyValues(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                           JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                           JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.METHOD_CHILD_NODE),
                                                      JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                      5),
                                                 JavaMetadataLexicon.PARAMETER,
                                                 JavaMetadataLexicon.FORMAL_PARAMETER,
                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.ARRAY_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.ARRAY_TYPE_VARIABLE,
                                                 JavaMetadataLexicon.VARIABLE), "java:variableName"), is(new Object[] {"oa"}));

        assertThat(output.getPropertyValues(path(path(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                                     JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                                     JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                                     JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                                     JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                                     JavaMetadataLexicon.METHOD_CHILD_NODE),
                                                                JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                                5), JavaMetadataLexicon.PARAMETER),
                                                      JavaMetadataLexicon.FORMAL_PARAMETER,
                                                      2),
                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.ARRAY_TYPE_CHILD_NODE), "java:arrayTypeName"),
                   is(new Object[] {"int"}));
        assertThat(output.getPropertyValues(path(path(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                                     JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                                     JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                                     JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                                     JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                                     JavaMetadataLexicon.METHOD_CHILD_NODE),
                                                                JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                                5), JavaMetadataLexicon.PARAMETER),
                                                      JavaMetadataLexicon.FORMAL_PARAMETER,
                                                      2),
                                                 JavaMetadataLexicon.TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.ARRAY_TYPE_CHILD_NODE,
                                                 JavaMetadataLexicon.ARRAY_TYPE_VARIABLE,
                                                 JavaMetadataLexicon.VARIABLE), "java:variableName"), is(new Object[] {"ia"}));

        // public Object doSomething3() method
        assertThat(output.getPropertyValues(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                           JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                           JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.METHOD_CHILD_NODE),
                                                      JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                      6),
                                                 JavaMetadataLexicon.MODIFIER_CHILD_NODE,
                                                 JavaMetadataLexicon.MODIFIER_DECLARATION_CHILD_NODE), "java:modifierName"),
                   is(new Object[] {"public"}));
        assertThat(output.getPropertyValues(path(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                           JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                           JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                           JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                           JavaMetadataLexicon.METHOD_CHILD_NODE),
                                                      JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                      6),
                                                 JavaMetadataLexicon.RETURN_TYPE,
                                                 JavaMetadataLexicon.SIMPLE_TYPE_CHILD_NODE), "java:simpleTypeName"),
                   is(new Object[] {"Object"}));
        assertThat(output.getPropertyValues(path(path(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                      JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                      JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                      JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                      JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                      JavaMetadataLexicon.METHOD_CHILD_NODE),
                                                 JavaMetadataLexicon.METHOD_DECLARATION_CHILD_NODE,
                                                 6), "java:methodName"), is(new Object[] {"doSomething3"}));

    }
}
