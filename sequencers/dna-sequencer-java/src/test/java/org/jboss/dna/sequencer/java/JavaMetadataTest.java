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
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Serge Pagop
 */
public class JavaMetadataTest {
    private File source;
    private InputStream stream;
    JavaMetadata javaMetadata;

    @Before
    public void beforeEach() throws Exception {
        source = new File("src/test/resources/org/acme/MySource.java");
        stream = getJavaSrc(source);

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
        ASTNode rootNode = CompilationUnitParser.runJLS3Conversion(JavaMetadataUtil.getJavaSourceFromTheInputStream(getJavaSrc(source),
                                                                                                                    source.length(),
                                                                                                                    null),
                                                                   true);
        assertThat(rootNode, is(notNullValue()));
        // Verify we get a compilation unit node and that binding are correct
        assertTrue("Not a compilation unit", rootNode.getNodeType() == ASTNode.COMPILATION_UNIT);
    }

    @Test
    public void shouldCreatePackageMetadata() throws Exception {
        javaMetadata = JavaMetadata.instance(stream, JavaMetadataUtil.length(stream), null, null);
        ASTNode rootNode = CompilationUnitParser.runJLS3Conversion(JavaMetadataUtil.getJavaSourceFromTheInputStream(getJavaSrc(source),
                                                                                                                    source.length(),
                                                                                                                    null),
                                                                   true);
        PackageMetadata packageMetadata = javaMetadata.createPackageMetadata((CompilationUnit)rootNode);
        assertThat(packageMetadata, is(notNullValue()));
        assertThat(packageMetadata.getName(), is("org.acme"));
        List<Annotation> annotations = packageMetadata.getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof MarkerAnnotation) {
                assertThat(annotation.getTypeName().getFullyQualifiedName(), is("org.acme.annotation.MyPackageAnnotation"));
            }
        }
    }
}
