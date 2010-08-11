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
package org.modeshape.connector.svn;

import java.util.Locale;
import java.util.Set;
import org.modeshape.common.i18n.I18n;

/**
 * The internationalized string constants for the <code>org.modeshape.connector.svn*</code> packages.
 */
public final class SvnRepositoryConnectorI18n {

    public static I18n connectorName;
    public static I18n nodeDoesNotExist;
    public static I18n nodeIsActuallyUnknow;
    public static I18n propertyIsRequired;
    public static I18n errorSerializingCachePolicyInSource;
    public static I18n locationInRequestMustHavePath;
    public static I18n sourceIsReadOnly;
    public static I18n sourceDoesNotSupportCreatingWorkspaces;
    public static I18n sourceDoesNotSupportCloningWorkspaces;
    public static I18n sourceDoesNotSupportDeletingWorkspaces;
    public static I18n connectingFailureOrUserAuthenticationProblem;
    public static I18n pathForPredefinedWorkspaceDoesNotExist;
    public static I18n pathForPredefinedWorkspaceIsNotDirectory;
    public static I18n pathForPredefinedWorkspaceCannotBeRead;
    public static I18n workspaceDoesNotExist;
    public static I18n pathForDefaultWorkspaceDoesNotExist;
    public static I18n pathForDefaultWorkspaceIsNotDirectory;
    public static I18n pathForDefaultWorkspaceCannotBeRead;
    public static I18n sameNameSiblingsAreNotAllowed;
    public static I18n onlyTheDefaultNamespaceIsAllowed;
    public static I18n unableToCreateWorkspaces;
    public static I18n pathForRequestIsNotCorrect;
    public static I18n pathForRequestMustStartWithAForwardSlash;
    public static I18n nodeAlreadyExist;
    public static I18n unsupportedPrimaryType;
    public static I18n invalidPropertyNames;
    public static I18n invalidNameForResource;
    public static I18n invalidPathForResource;
    public static I18n missingRequiredProperty;
    public static I18n couldNotCreateFile;
    public static I18n couldNotReadData;
    public static I18n deleteFailed;
    public static I18n nodeOrderingNotSupported;

    public static I18n repositoryRootUrlPropertyDescription;
    public static I18n repositoryRootUrlPropertyLabel;
    public static I18n repositoryRootUrlPropertyCategory;
    public static I18n usernamePropertyDescription;
    public static I18n usernamePropertyLabel;
    public static I18n usernamePropertyCategory;
    public static I18n passwordPropertyDescription;
    public static I18n passwordPropertyLabel;
    public static I18n passwordPropertyCategory;
    public static I18n creatingWorkspacesAllowedPropertyDescription;
    public static I18n creatingWorkspacesAllowedPropertyLabel;
    public static I18n creatingWorkspacesAllowedPropertyCategory;
    public static I18n defaultWorkspaceNamePropertyDescription;
    public static I18n defaultWorkspaceNamePropertyLabel;
    public static I18n defaultWorkspaceNamePropertyCategory;
    public static I18n predefinedWorkspaceNamesPropertyDescription;
    public static I18n predefinedWorkspaceNamesPropertyLabel;
    public static I18n predefinedWorkspaceNamesPropertyCategory;
    public static I18n updatesAllowedPropertyDescription;
    public static I18n updatesAllowedPropertyLabel;
    public static I18n updatesAllowedPropertyCategory;

    static {
        try {
            I18n.initialize(SvnRepositoryConnectorI18n.class);
        } catch (final Exception err) {
            System.err.println(err);
        }
    }

    public static Set<Locale> getLocalizationProblemLocales() {
        return I18n.getLocalizationProblemLocales(SvnRepositoryConnectorI18n.class);
    }

    public static Set<String> getLocalizationProblems() {
        return I18n.getLocalizationProblems(SvnRepositoryConnectorI18n.class);
    }

    public static Set<String> getLocalizationProblems( Locale locale ) {
        return I18n.getLocalizationProblems(SvnRepositoryConnectorI18n.class, locale);
    }

}
