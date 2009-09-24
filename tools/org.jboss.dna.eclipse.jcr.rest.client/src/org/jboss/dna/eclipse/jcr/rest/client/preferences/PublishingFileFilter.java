/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.eclipse.jcr.rest.client.preferences;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.jboss.dna.common.util.CheckArg;

/**
 * The <code>PublishingFileFilter</code> is a file filter that uses the DNA preferences when filtering files.
 */
public final class PublishingFileFilter {

    // =======================================================================================================================
    // Fields
    // =======================================================================================================================

    /**
     * The file extensions that should not be involved in publishing operations.
     */
    private final String[] filteredFileExtensions;

    /**
     * The folder names that should not be involved in publishing operations.
     */
    private final String[] filteredFolderNames;

    // =======================================================================================================================
    // Constructors
    // =======================================================================================================================

    /**
     * Construct a filter using the current DNA preferences.
     */
    public PublishingFileFilter() {
        this.filteredFileExtensions = PrefUtils.getFilteredFileExtensions();
        this.filteredFolderNames = PrefUtils.getFilteredFolderNames();
    }

    // =======================================================================================================================
    // Methods
    // =======================================================================================================================

    /**
     * @param resource the resource being tested (never <code>null</code>)
     * @return <code>true</code> if the resource should be included (i.e., it is not filtered out)
     */
    public boolean accept( IResource resource ) {
        CheckArg.isNotNull(resource, "resource"); //$NON-NLS-1$

        if (resource instanceof IFolder) {
            String name = resource.getName();

            // see if folder name has been filtered
            for (String filteredName : this.filteredFolderNames) {
                if (filteredName.equals(name)) {
                    return false;
                }
            }

            // check parent
            if (resource.getParent() != null) {
                return accept(resource.getParent());
            }
        } else if (resource instanceof IFile) {
            // see if file extension has been filtered
            for (String extension : this.filteredFileExtensions) {
                if (resource.getFullPath().toString().endsWith('.' + extension)) {
                    return false;
                }
            }

            // check parent
            if (resource.getParent() != null) {
                return accept(resource.getParent());
            }
        }

        // must be project
        return true;
    }

}
