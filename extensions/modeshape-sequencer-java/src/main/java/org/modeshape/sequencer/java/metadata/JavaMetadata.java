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
package org.modeshape.sequencer.java.metadata;

import java.io.InputStream;
import java.util.List;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.modeshape.sequencer.java.AbstractJavaMetadata;
import org.modeshape.sequencer.java.CompilationUnitParser;
import org.modeshape.sequencer.java.JavaMetadataUtil;

/**
 * Metadata for a Java source file.
 */
public class JavaMetadata extends AbstractJavaMetadata {

    /** The package representation of a compilation unit. */
    private PackageMetadata packageMetadata;

    /** All the import declarations of a compilation unit. */
    private List<ImportMetadata> imports;

    /** Types of unit */
    private List<TypeMetadata> types;

    private JavaMetadata() {
        // private constructor to enforce static class pattern
    }

    /**
     * Creates a new instance of <code>JavaMetadata</code>, that will be used to get informations of a compilation unit.
     * 
     * @param inputStream - the <code>InputStream</code> in our case a <code>FileInputStream</code> of the java file.
     * @param length - the length of the java file.
     * @param encoding - the encoding that can be used.
     * @return the new instace of <code>JavaMetadata</code>
     * @see java.io.File#length()
     */
    public static JavaMetadata instance( InputStream inputStream,
                                         long length,
                                         String encoding ) {

        JavaMetadata javaMetadata = new JavaMetadata();
        char[] source = null;
        try {
            source = JavaMetadataUtil.getJavaSourceFromTheInputStream(inputStream, length, encoding);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        CompilationUnit unit = (CompilationUnit)CompilationUnitParser.runJLS3Conversion(source, true);
        if (unit != null) {
            javaMetadata.packageMetadata = javaMetadata.createPackageMetadata(unit);
            javaMetadata.imports = javaMetadata.createImportMetadata(unit);
            javaMetadata.types = javaMetadata.createTypeMetadata(unit);

        }

        return javaMetadata;
    }

    /**
     * Gets the {@link PackageMetadata} from the unit.
     * 
     * @return the PackageMetadata or null if there is not package declaration for the unit.
     */
    public final PackageMetadata getPackageMetadata() {
        return packageMetadata;
    }

    /**
     * Gets a list of {@linkImportMetadata} from the unit.
     * 
     * @return all imports of this unit if there is one.
     */
    public List<ImportMetadata> getImports() {
        return imports;
    }

    /**
     * Gets the list for type declarations (class/interface/enum/annotation) of this unit.
     * 
     * @return all typeMetadata of this unit.
     */
    public List<TypeMetadata> getTypeMetadata() {
        return types;
    }
}
