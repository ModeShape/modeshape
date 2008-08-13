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
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.stub;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.jboss.dna.common.monitor.ProgressMonitor;
import org.jboss.dna.common.monitor.SimpleProgressMonitor;
import org.jboss.dna.spi.sequencers.MockSequencerOutput;
import org.jboss.dna.spi.sequencers.SequencerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Serge Pagop
 */
public class JavaMetadataSequencerTest {
    private JavaMetadataSequencer sequencer;
    private InputStream content;
    private MockSequencerOutput output;
    private ProgressMonitor progress;
    private File source;
    @Mock
    private SequencerContext context;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        sequencer = new JavaMetadataSequencer();
        output = new MockSequencerOutput();
        output.getNamespaceRegistry().register("java", "http://jboss.org/dna/java/1.0");
        this.progress = new SimpleProgressMonitor("Test java monitor activity");
        source = new File("src/test/workspace/projectX/src/org/acme/MySource.java");
        stub(context.getFactories()).toReturn(output.getFactories());
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

    @Test
    public void shouldGenerateMetadataForJavaSourceFile() throws IOException {
        content = getJavaSrc(source);
        assertThat(content, is(notNullValue()));
        sequencer.sequence(content, output, context, progress);
        assertThat(output.getPropertyValues("java:compilationUnit", "jcr:primaryType"), is(new Object[] {"java:compilationUnit"}));

        // support sequencing package declaration( FQL name of the package). Not supported is to get information for package
        // annotation
        // from package-info.java
        assertThat(output.getPropertyValues("java:compilationUnit/java:package/java:packageDeclaration", "java:packageName"),
                   is(new Object[] {"org.acme"}));

        // TODO (find a solution to get the annotation of a package). Java Sequencer does not yet support sequencing of
        // package-info.java with package annotations
        // assertThat(output.getPropertyValues("java:compilationUnit/java:package/java:packageDeclaration/java:annotation/java:annotationDeclaration/java:annotationType/java:markerAnnotation[1]",
        // "java:typeName"),
        // is(new Object[] {"org.acme.annotation.MyPackageAnnotation"}));

        // support for sequencing imports (single import and import on demand)
        assertThat(output.getPropertyValues("java:compilationUnit/java:import/java:importDeclaration/java:singleImport/java:singleTypeImportDeclaration[1]",
                                            "java:typeName"),
                   is(new Object[] {"org.acme.annotation.MyClassAnnotation"}));
        assertThat(output.getPropertyValues("java:compilationUnit/java:import/java:importDeclaration/java:importOnDemand/java:typeImportOnDemandDeclaration[1]",
                                            "java:typeName"),
                   is(new Object[] {"java.util"}));

        // support for sequencing class definition (moodifiers, class name)
        assertThat(output.getPropertyValues("java:compilationUnit/java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:modifier/java:modifierDeclaration[1]",
                                            "java:modifierName"),
                   is(new Object[] {"public"}));
        assertThat(output.getPropertyValues("java:compilationUnit/java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration",
                                            "java:name"),
                   is(new Object[] {"MySource"}));

        // support for primitive type sequencing (modifiers, types, variables).Not supported is the javadoc
        assertThat(output.getPropertyValues("java:compilationUnit/java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:field/java:fieldType/java:type/java:primitiveType[1]/java:modifier/java:modifierDeclaration[1]",
                                            "java:modifierName"),
                   is(new Object[] {"private"}));

        assertThat(output.getPropertyValues("java:compilationUnit/java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:field/java:fieldType/java:type/java:primitiveType[1]",
                                            "java:typeName"),
                   is(new Object[] {"int"}));

        assertThat(output.getPropertyValues("java:compilationUnit/java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:field/java:fieldType/java:type/java:primitiveType[1]/java:variable[1]",
                                            "java:variableName"),
                   is(new Object[] {"i"}));

        assertThat(output.getPropertyValues("java:compilationUnit/java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:field/java:fieldType/java:type/java:primitiveType[1]/java:variable[2]",
                                            "java:variableName"),
                   is(new Object[] {"j"}));

        assertThat(output.getPropertyValues("java:compilationUnit/java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:field/java:fieldType/java:type/java:primitiveType[2]/java:modifier/java:modifierDeclaration[1]",
                                            "java:modifierName"),
                   is(new Object[] {"private"}));

        assertThat(output.getPropertyValues("java:compilationUnit/java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:field/java:fieldType/java:type/java:primitiveType[2]/java:modifier/java:modifierDeclaration[2]",
                                            "java:modifierName"),
                   is(new Object[] {"static"}));

        assertThat(output.getPropertyValues("java:compilationUnit/java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:field/java:fieldType/java:type/java:primitiveType[2]",
                                            "java:typeName"),
                   is(new Object[] {"double"}));

        assertThat(output.getPropertyValues("java:compilationUnit/java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:field/java:fieldType/java:type/java:primitiveType[2]/java:variable[1]",
                                            "java:variableName"),
                   is(new Object[] {"a"}));

        // support for reference type sequencing ()

        // assertThat(output.getPropertyValues("java:compilationUnit/java:type/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:field/java:fieldType/java:type/java:referenceType",
        // "java:name"),
        // is(new Object[] {"l"}));
        // assertThat(output.getPropertyValues("java:compilationUnit/java:type/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:field/java:fieldType/java:type/java:referenceType",
        // "java:name"),
        // is(new Object[] {"o"}));
        // assertThat(output.getPropertyValues("java:compilationUnit/java:type/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:field/java:fieldType/java:type/java:referenceType",
        // "java:name"),
        // is(new Object[] {"x"}));

        // support for methods sequencing (modifiers, return type, method name, parameters).Not supported are javadoc
        assertThat(output.getPropertyValues("java:compilationUnit/java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:constructor/java:constructorDeclaration[1]",
                                            "java:name"),
                   is(new Object[] {"MySource"}));

        assertThat(output.getPropertyValues("java:compilationUnit/java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:constructor/java:constructorDeclaration[2]/java:modifier/java:modifierDeclaration[1]",
                                            "java:modifierName"),
                   is(new Object[] {"public"}));
        assertThat(output.getPropertyValues("java:compilationUnit/java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:constructor/java:constructorDeclaration[2]",
                                            "java:name"),
                   is(new Object[] {"MySource"}));

        assertThat(output.getPropertyValues("java:compilationUnit/java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:constructor/java:constructorDeclaration[2]/java:parameter/java:formalParameter[1]/java:type/java:primitiveType",
                                            "java:typeName"),
                   is(new Object[] {"int"}));
        assertThat(output.getPropertyValues("java:compilationUnit/java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:constructor/java:constructorDeclaration[2]/java:parameter/java:formalParameter[1]/java:type/java:primitiveType/java:variable",
                                            "java:variableName"),
                   is(new Object[] {"i"}));

        assertThat(output.getPropertyValues("java:compilationUnit/java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:constructor/java:constructorDeclaration[2]/java:parameter/java:formalParameter[1]/java:type/java:primitiveType",
                                            "java:typeName"),
                   is(new Object[] {"int"}));
        assertThat(output.getPropertyValues("java:compilationUnit/java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:constructor/java:constructorDeclaration[2]/java:parameter/java:formalParameter[2]/java:type/java:primitiveType/java:variable",
                                            "java:variableName"),
                   is(new Object[] {"j"}));

        assertThat(output.getPropertyValues("java:compilationUnit/java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:method/java:methodDeclaration[1]",
                                            "java:name"),
                   is(new Object[] {"getI"}));
        assertThat(output.getPropertyValues("java:compilationUnit/java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:method/java:methodDeclaration[2]",
                                            "java:name"),
                   is(new Object[] {"setI"}));
        assertThat(output.getPropertyValues("java:compilationUnit/java:unitType/java:classDeclaration/java:normalClass/java:normalClassDeclaration/java:method/java:methodDeclaration[4]",
                                            "java:name"),
                   is(new Object[] {"doSomething"}));

    }

}
