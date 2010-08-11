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
package org.modeshape.connector.meta.jdbc;

import java.util.Locale;
import java.util.Set;
import org.modeshape.common.CommonI18n;
import org.modeshape.common.i18n.I18n;

/**
 * The internationalized string constants for the <code>org.modeshape.connector.meta.jdbc.*</code> packages.
 */
public final class JdbcMetadataI18n {

    public static I18n errorClosingConnection;
    public static I18n errorObtainingConnection;

    public static I18n sourceIsReadOnly;

    public static I18n couldNotGetDatabaseMetadata;
    public static I18n couldNotGetCatalogNames;
    public static I18n couldNotGetSchemaNames;
    public static I18n couldNotGetTableNames;
    public static I18n couldNotGetTable;
    public static I18n couldNotGetColumn;
    public static I18n duplicateTablesWithSameName;
    public static I18n couldNotGetProcedureNames;
    public static I18n couldNotGetProcedure;

    public static I18n repositorySourceMustHaveName;
    public static I18n errorFindingDataSourceInJndi;
    public static I18n errorSettingContextClassLoader;
    public static I18n driverClassNameAndUrlAreRequired;
    public static I18n couldNotSetDriverProperties;

    public static I18n defaultWorkspaceNamePropertyDescription;
    public static I18n defaultWorkspaceNamePropertyLabel;
    public static I18n defaultWorkspaceNamePropertyCategory;
    public static I18n retryLimitPropertyDescription;
    public static I18n retryLimitPropertyLabel;
    public static I18n retryLimitPropertyCategory;
    public static I18n updatesAllowedPropertyDescription;
    public static I18n updatesAllowedPropertyLabel;
    public static I18n updatesAllowedPropertyCategory;

    public static I18n dataSourceJndiNamePropertyDescription;
    public static I18n dataSourceJndiNamePropertyLabel;
    public static I18n dataSourceJndiNamePropertyCategory;

    public static I18n passwordPropertyDescription;
    public static I18n passwordPropertyLabel;
    public static I18n passwordPropertyCategory;
    public static I18n usernamePropertyDescription;
    public static I18n usernamePropertyLabel;
    public static I18n usernamePropertyCategory;
    public static I18n urlPropertyDescription;
    public static I18n urlPropertyLabel;
    public static I18n urlPropertyCategory;
    public static I18n driverClassNamePropertyDescription;
    public static I18n driverClassNamePropertyLabel;
    public static I18n driverClassNamePropertyCategory;
    public static I18n driverClassloaderNamePropertyDescription;
    public static I18n driverClassloaderNamePropertyLabel;
    public static I18n driverClassloaderNamePropertyCategory;
    public static I18n maximumConnectionsInPoolPropertyDescription;
    public static I18n maximumConnectionsInPoolPropertyLabel;
    public static I18n maximumConnectionsInPoolPropertyCategory;
    public static I18n minimumConnectionsInPoolPropertyDescription;
    public static I18n minimumConnectionsInPoolPropertyLabel;
    public static I18n minimumConnectionsInPoolPropertyCategory;
    public static I18n maximumConnectionIdleTimeInSecondsPropertyDescription;
    public static I18n maximumConnectionIdleTimeInSecondsPropertyLabel;
    public static I18n maximumConnectionIdleTimeInSecondsPropertyCategory;
    public static I18n maximumSizeOfStatementCachePropertyDescription;
    public static I18n maximumSizeOfStatementCachePropertyLabel;
    public static I18n maximumSizeOfStatementCachePropertyCategory;
    public static I18n numberOfConnectionsToAcquireAsNeededPropertyDescription;
    public static I18n numberOfConnectionsToAcquireAsNeededPropertyLabel;
    public static I18n numberOfConnectionsToAcquireAsNeededPropertyCategory;
    public static I18n idleTimeInSecondsBeforeTestingConnectionsPropertyDescription;
    public static I18n idleTimeInSecondsBeforeTestingConnectionsPropertyLabel;
    public static I18n idleTimeInSecondsBeforeTestingConnectionsPropertyCategory;
    public static I18n defaultCatalogNamePropertyDescription;
    public static I18n defaultCatalogNamePropertyLabel;
    public static I18n defaultCatalogNamePropertyCategory;
    public static I18n defaultSchemaNamePropertyDescription;
    public static I18n defaultSchemaNamePropertyLabel;
    public static I18n defaultSchemaNamePropertyCategory;
    public static I18n metadataCollectorClassNamePropertyDescription;
    public static I18n metadataCollectorClassNamePropertyLabel;
    public static I18n metadataCollectorClassNamePropertyCategory;

    static {
        try {
            I18n.initialize(JdbcMetadataI18n.class);
        } catch (final Exception err) {
            System.err.println(err);
        }
    }

    public static Set<Locale> getLocalizationProblemLocales() {
        return I18n.getLocalizationProblemLocales(CommonI18n.class);
    }

    public static Set<String> getLocalizationProblems() {
        return I18n.getLocalizationProblems(CommonI18n.class);
    }

    public static Set<String> getLocalizationProblems( Locale locale ) {
        return I18n.getLocalizationProblems(CommonI18n.class, locale);
    }

}
