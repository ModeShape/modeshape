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
package org.modeshape.jdbc;

import org.modeshape.jdbc.util.I18n;

/**
 * The internationalized string constants for the <code>org.modeshape.jdbc*</code> packages.
 */
public final class JdbcI18n {
    
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

    
    public static I18n repositoryNameInUse;
    
    /*
     * JNDI connection option related text info
     */
    public static I18n objectInJndiMustBeRepositoryOrRepositories;
    public static I18n unableToGetJndiContext;
    public static I18n urlMustContainJndiNameOfRepositoryOrRepositoriesObject;
    public static I18n unableToFindObjectInJndi;
    public static I18n objectInJndiIsRepositories;

    /**
     * HTTP connection option related text info
     */

    
    /*
     * File connection option relatd text info
     */
    public static I18n configurationFileNotSpecified;
    
    
    
    static {
        try {
            I18n.initialize(JdbcI18n.class);
        } catch (final Exception err) {
            System.err.println(err);
        }
    }
}
