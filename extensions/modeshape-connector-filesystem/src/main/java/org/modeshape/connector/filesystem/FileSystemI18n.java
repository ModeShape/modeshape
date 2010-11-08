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
package org.modeshape.connector.filesystem;

import java.util.Locale;
import java.util.Set;
import org.modeshape.common.i18n.I18n;

/**
 * The internationalized string constants for the <code>org.modeshape.connector.filesystem*</code> packages.
 */
public final class FileSystemI18n {

    public static I18n connectorName;
    public static I18n workspaceDoesNotExist;
    public static I18n pathForWorkspaceRootDoesNotExist;
    public static I18n pathForWorkspaceRootIsNotDirectory;
    public static I18n pathForWorkspaceRootCannotBeRead;
    public static I18n propertyIsRequired;
    public static I18n sameNameSiblingsAreNotAllowed;
    public static I18n nodeOrderingNotSupported;
    public static I18n onlyTheDefaultNamespaceIsAllowed;
    public static I18n sourceIsReadOnly;
    public static I18n pathIsReadOnly;
    public static I18n unableToCreateWorkspaces;
    public static I18n couldNotCreateDirectory;
    public static I18n ancestorInPathIsFile;
    public static I18n pathForWorkspaceCannotBeRead;
    public static I18n pathForWorkspaceIsNotDirectory;

    public static I18n workspaceRootPathPropertyDescription;
    public static I18n workspaceRootPathPropertyLabel;
    public static I18n workspaceRootPathPropertyCategory;
    public static I18n defaultWorkspaceNamePropertyDescription;
    public static I18n defaultWorkspaceNamePropertyLabel;
    public static I18n defaultWorkspaceNamePropertyCategory;
    public static I18n predefinedWorkspaceNamesPropertyDescription;
    public static I18n predefinedWorkspaceNamesPropertyLabel;
    public static I18n predefinedWorkspaceNamesPropertyCategory;
    public static I18n updatesAllowedPropertyDescription;
    public static I18n updatesAllowedPropertyLabel;
    public static I18n updatesAllowedPropertyCategory;
    public static I18n exclusionPatternPropertyDescription;
    public static I18n exclusionPatternPropertyLabel;
    public static I18n exclusionPatternPropertyCategory;
    public static I18n maxPathLengthPropertyDescription;
    public static I18n maxPathLengthPropertyLabel;
    public static I18n maxPathLengthPropertyCategory;
    public static I18n eagerFileLoadingPropertyDescription;
    public static I18n eagerFileLoadingPropertyLabel;
    public static I18n eagerFileLoadingPropertyCategory;
    public static I18n determineMimeTypeUsingContentPropertyDescription;
    public static I18n determineMimeTypeUsingContentPropertyLabel;
    public static I18n determineMimeTypeUsingContentPropertyCategory;
    public static I18n extraPropertiesPropertyDescription;
    public static I18n extraPropertiesPropertyLabel;
    public static I18n extraPropertiesPropertyCategory;
    public static I18n customPropertiesFactoryPropertyDescription;
    public static I18n customPropertiesFactoryPropertyLabel;
    public static I18n customPropertiesFactoryPropertyCategory;

    // Writable messages
    public static I18n parentIsReadOnly;
    public static I18n fileAlreadyExists;
    public static I18n couldNotCreateFile;
    public static I18n cannotRenameFileToExcludedPattern;
    public static I18n cannotCreateFileAsExcludedPattern;
    public static I18n unsupportedPrimaryType;
    public static I18n invalidNameForResource;
    public static I18n invalidPathForResource;
    public static I18n invalidPropertyNames;
    public static I18n couldNotReadData;
    public static I18n couldNotWriteData;
    public static I18n couldNotCopyData;
    public static I18n missingRequiredProperty;
    public static I18n deleteFailed;
    public static I18n getCanonicalPathFailed;
    public static I18n maxPathLengthExceeded;
    public static I18n couldNotStoreProperty;
    public static I18n couldNotStoreProperties;

    static {
        try {
            I18n.initialize(FileSystemI18n.class);
        } catch (final Exception err) {
            System.err.println(err);
        }
    }

    public static Set<Locale> getLocalizationProblemLocales() {
        return I18n.getLocalizationProblemLocales(FileSystemI18n.class);
    }

    public static Set<String> getLocalizationProblems() {
        return I18n.getLocalizationProblems(FileSystemI18n.class);
    }

    public static Set<String> getLocalizationProblems( Locale locale ) {
        return I18n.getLocalizationProblems(FileSystemI18n.class, locale);
    }
}
