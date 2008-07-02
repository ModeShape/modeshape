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

import java.io.InputStream;
import java.util.List;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.dna.common.monitor.ProgressMonitor;
import org.jboss.dna.sequencer.java.importmetadata.ImportMetadata;
import org.jboss.dna.sequencer.java.packagemetadata.PackageMetadata;

/**
 * @author Serge Pagop
 */
public class JavaMetadata extends AbstractJavaMetadata {
    
    /** The package representation of a compilation unit. */
    private PackageMetadata packageMetadata;
    
    /** All the import declarations of a compilation unit. */
    private List<ImportMetadata> importMetadata;
    
    /** variables */
    
    /** methods */

    private JavaMetadata() {
    }

    /**
     * Creates a new instance of <code>JavaMetadata</code>, that will be used to get informations of a compilation unit.
     * 
     * @param inputStream - the <code>InputStream</code> in our case a <code>FileInputStream</code> of the java file.
     * @param length - the length of the java file.
     * @param encoding - the encoding that can be used.
     * @param progressMonitor - The basic <code>ProgressMonitor</code> that facilitates the updating and monitoring of progress
     *        towards the completion of an activity.
     * @return the new instace of <code>JavaMetadata</code>
     * @see java.io.File#length()
     */
    public static JavaMetadata instance( InputStream inputStream,
                                         long length,
                                         String encoding,
                                         ProgressMonitor progressMonitor ) {

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
            javaMetadata.importMetadata = javaMetadata.createImportMetadata(unit);
        }

        return javaMetadata;
    }

    /**
     * Gets the <code>PackageMetadata</code>.
     * 
     * @return the PackageMetadata or null if there is not package declaration for the unit.
     */
    public final PackageMetadata getPackageMetadata() {
        return packageMetadata;
    }

    /**
     * Gets a list of <code>ImportMetadata</code>.
     * 
     * @return all the importMetadata of this unit if there is one.
     */
    public List<ImportMetadata> getImportMetadata() {
        return importMetadata;
    }
}
