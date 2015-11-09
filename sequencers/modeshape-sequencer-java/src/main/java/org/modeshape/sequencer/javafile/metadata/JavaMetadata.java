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
package org.modeshape.sequencer.javafile.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.modeshape.sequencer.javafile.AbstractJavaMetadata;
import org.modeshape.sequencer.javafile.CompilationUnitParser;
import org.modeshape.sequencer.javafile.JavaMetadataUtil;

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
     * @throws java.io.IOException if the input stream cannot be parsed
     */
    public static JavaMetadata instance( InputStream inputStream,
                                         long length,
                                         String encoding ) throws IOException {

        JavaMetadata javaMetadata = new JavaMetadata();
        char[] source = JavaMetadataUtil.getJavaSourceFromTheInputStream(inputStream, length, encoding);

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
     * Gets a list of {@link ImportMetadata} from the unit.
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
