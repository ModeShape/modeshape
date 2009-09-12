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
package org.jboss.dna.connector.filesystem;

import java.util.Locale;
import java.util.Set;
import org.jboss.dna.common.i18n.I18n;

/**
 * The internationalized string constants for the <code>org.jboss.dna.connector.filesystem*</code> packages.
 */
public final class FileSystemI18n {

    public static I18n connectorName;
    public static I18n workspaceDoesNotExist;
    public static I18n pathForDefaultWorkspaceDoesNotExist;
    public static I18n pathForDefaultWorkspaceIsNotDirectory;
    public static I18n pathForDefaultWorkspaceCannotBeRead;
    public static I18n pathForPredefinedWorkspaceDoesNotExist;
    public static I18n pathForPredefinedWorkspaceIsNotDirectory;
    public static I18n pathForPredefinedWorkspaceCannotBeRead;
    public static I18n pathForWorkspaceRootDoesNotExist;
    public static I18n pathForWorkspaceRootIsNotDirectory;
    public static I18n pathForWorkspaceRootCannotBeRead;
    public static I18n propertyIsRequired;
    public static I18n locationInRequestMustHavePath;
    public static I18n sameNameSiblingsAreNotAllowed;
    public static I18n nodeOrderingNotSupported;
    public static I18n onlyTheDefaultNamespaceIsAllowed;
    public static I18n sourceIsReadOnly;
    public static I18n pathIsReadOnly;
    public static I18n unableToCreateWorkspaces;

    // Writable messages
    public static I18n parentIsReadOnly;
    public static I18n fileAlreadyExists;
    public static I18n couldNotCreateFile;
    public static I18n unsupportedPrimaryType;
    public static I18n invalidNameForResource;
    public static I18n invalidPathForResource;
    public static I18n invalidPropertyNames;
    public static I18n couldNotWriteData;
    public static I18n couldNotUpdateData;
    public static I18n missingRequiredProperty;
    public static I18n deleteFailed;
    public static I18n copyFailed;
    public static I18n getCanonicalPathFailed;
    public static I18n maxPathLengthExceeded;

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
