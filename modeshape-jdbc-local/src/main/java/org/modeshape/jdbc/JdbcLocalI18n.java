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
package org.modeshape.jdbc;

import org.modeshape.common.i18n.I18n;

/**
 * The internationalized string constants for the <code>org.modeshape.jdbc*</code> packages.
 */
public final class JdbcLocalI18n {

    public static I18n i18nClassInterface;
    public static I18n i18nClassNotPublic;
    public static I18n i18nFieldFinal;
    public static I18n i18nFieldInvalidType;
    public static I18n i18nFieldNotPublic;
    public static I18n i18nFieldNotStatic;
    public static I18n i18nLocalizationFileNotFound;
    public static I18n i18nLocalizationProblems;
    public static I18n i18nPropertyDuplicate;
    public static I18n i18nPropertyMissing;
    public static I18n i18nPropertyUnused;
    public static I18n i18nRequiredToSuppliedParameterMismatch;

    public static I18n driverName;
    public static I18n driverVendor;
    public static I18n driverVendorUrl;
    public static I18n driverVersion;

    public static I18n driverErrorRegistering;

    /*
     * ConnectionInfo related text info
     */
    public static I18n usernamePropertyDescription;
    public static I18n passwordPropertyDescription;
    public static I18n workspaceNamePropertyDescription;
    public static I18n repositoryNamePropertyDescription;
    public static I18n urlPropertyDescription;
    public static I18n urlPropertyName;
    public static I18n usernamePropertyName;
    public static I18n passwordPropertyName;
    public static I18n workspaceNamePropertyName;
    public static I18n repositoryNamePropertyName;

    public static I18n invalidUrl;
    public static I18n invalidUrlPrefix;
    public static I18n failedToReadPropertiesFromManifest;
    public static I18n unableToFindNamedRepository;
    public static I18n noRepositoryNamesFound;
    public static I18n argumentMayNotBeNegative;
    public static I18n argumentMayNotBeNull;

    public static I18n connectionIsClosed;
    public static I18n statementIsClosed;
    public static I18n resultSetIsClosed;
    public static I18n resultSetIsForwardOnly;
    public static I18n noSuchColumn;
    public static I18n updatesNotSupported;
    public static I18n timeoutMayNotBeNegative;
    public static I18n classDoesNotImplementInterface;
    public static I18n invalidClientInfo;
    public static I18n invalidArgument;
    public static I18n invalidColumnIndex;
    public static I18n currentRowNotSet;
    public static I18n noJcrTypeMapped;
    public static I18n unableToGetNodeTypes;
    public static I18n noNodeTypesReturned;
    public static I18n unableToGetNodeType;
    public static I18n noSuchNodeType;

    public static I18n cannotConvertJcrValue;

    public static I18n repositoryNameInUse;

    /*
     * JNDI connection option related text info
     */
    public static I18n objectInJndiMustBeRepositoryOrRepositories;
    public static I18n unableToGetJndiContext;
    public static I18n urlMustContainJndiNameOfRepositoryOrRepositoriesObject;
    public static I18n unableToFindObjectInJndi;
    public static I18n objectInJndiIsRepositories;

    /*
     * File connection option relatd text info
     */
    public static I18n configurationFileNotSpecified;

    private JdbcLocalI18n() {
    }

    static {
        try {
            I18n.initialize(JdbcLocalI18n.class);
        } catch (final Exception err) {
            // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
            System.err.println(err);
        }
    }
}
