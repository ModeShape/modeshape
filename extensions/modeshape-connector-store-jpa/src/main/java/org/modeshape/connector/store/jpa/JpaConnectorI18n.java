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
package org.modeshape.connector.store.jpa;

import java.util.Locale;
import java.util.Set;
import net.jcip.annotations.Immutable;
import org.modeshape.common.i18n.I18n;

/**
 * The internationalized string constants for the <code>org.modeshape.connector.store.jpa*</code> packages.
 */
@Immutable
public final class JpaConnectorI18n {

    public static I18n connectorName;
    public static I18n nodeDoesNotExist;
    public static I18n propertyIsRequired;
    public static I18n errorFindingDataSourceInJndi;
    public static I18n repositorySourceMustHaveName;
    public static I18n unknownModelName;
    public static I18n errorSettingContextClassLoader;
    public static I18n existingStoreSpecifiesUnknownModel;
    public static I18n unableToReadLargeValue;
    public static I18n unableToMoveRootNode;
    public static I18n locationShouldHavePathAndOrProperty;
    public static I18n invalidUuidForWorkspace;
    public static I18n invalidReferences;
    public static I18n invalidIsolationLevel;
    public static I18n unableToDeleteBecauseOfReferences;
    public static I18n dialectCouldNotBeDeterminedAndMustBeSpecified;

    public static I18n workspaceAlreadyExists;
    public static I18n workspaceDoesNotExist;
    public static I18n unableToCreateWorkspaces;
    public static I18n connectionIsNoLongerOpen;

    public static I18n basicModelDescription;
    public static I18n simpleModelDescription;

    public static I18n namePropertyDescription;
    public static I18n namePropertyLabel;
    public static I18n namePropertyCategory;
    public static I18n rootNodeUuidPropertyDescription;
    public static I18n rootNodeUuidPropertyLabel;
    public static I18n rootNodeUuidPropertyCategory;
    public static I18n creatingWorkspacesAllowedPropertyDescription;
    public static I18n creatingWorkspacesAllowedPropertyLabel;
    public static I18n creatingWorkspacesAllowedPropertyCategory;
    public static I18n defaultWorkspaceNamePropertyDescription;
    public static I18n defaultWorkspaceNamePropertyLabel;
    public static I18n defaultWorkspaceNamePropertyCategory;
    public static I18n predefinedWorkspaceNamesPropertyDescription;
    public static I18n predefinedWorkspaceNamesPropertyLabel;
    public static I18n predefinedWorkspaceNamesPropertyCategory;
    public static I18n retryLimitPropertyDescription;
    public static I18n retryLimitPropertyLabel;
    public static I18n retryLimitPropertyCategory;
    public static I18n updatesAllowedPropertyDescription;
    public static I18n updatesAllowedPropertyLabel;
    public static I18n updatesAllowedPropertyCategory;

    public static I18n dataSourceJndiNamePropertyDescription;
    public static I18n dataSourceJndiNamePropertyLabel;
    public static I18n dataSourceJndiNamePropertyCategory;

    public static I18n dialectPropertyDescription;
    public static I18n dialectPropertyLabel;
    public static I18n dialectPropertyCategory;
    public static I18n passwordPropertyDescription;
    public static I18n passwordPropertyLabel;
    public static I18n passwordPropertyCategory;
    public static I18n usernamePropertyDescription;
    public static I18n usernamePropertyLabel;
    public static I18n usernamePropertyCategory;
    public static I18n urlPropertyDescription;
    public static I18n urlPropertyLabel;
    public static I18n urlPropertyCategory;
    public static I18n isolationLevelPropertyDescription;
    public static I18n isolationLevelPropertyLabel;
    public static I18n isolationLevelPropertyCategory;
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
    public static I18n cacheTimeToLiveInMillisecondsPropertyDescription;
    public static I18n cacheTimeToLiveInMillisecondsPropertyLabel;
    public static I18n cacheTimeToLiveInMillisecondsPropertyCategory;
    public static I18n largeValueSizeInBytesPropertyDescription;
    public static I18n largeValueSizeInBytesPropertyLabel;
    public static I18n largeValueSizeInBytesPropertyCategory;
    public static I18n showSqlPropertyDescription;
    public static I18n showSqlPropertyLabel;
    public static I18n showSqlPropertyCategory;
    public static I18n compressDataPropertyDescription;
    public static I18n compressDataPropertyLabel;
    public static I18n compressDataPropertyCategory;
    public static I18n referentialIntegrityEnforcedPropertyDescription;
    public static I18n referentialIntegrityEnforcedPropertyLabel;
    public static I18n referentialIntegrityEnforcedPropertyCategory;
    public static I18n autoGenerateSchemaPropertyDescription;
    public static I18n autoGenerateSchemaPropertyLabel;
    public static I18n autoGenerateSchemaPropertyCategory;
    public static I18n modelNamePropertyDescription;
    public static I18n modelNamePropertyLabel;
    public static I18n modelNamePropertyCategory;

    static {
        try {
            I18n.initialize(JpaConnectorI18n.class);
        } catch (final Exception err) {
            System.err.println(err);
        }
    }

    public static Set<Locale> getLocalizationProblemLocales() {
        return I18n.getLocalizationProblemLocales(JpaConnectorI18n.class);
    }

    public static Set<String> getLocalizationProblems() {
        return I18n.getLocalizationProblems(JpaConnectorI18n.class);
    }

    public static Set<String> getLocalizationProblems( Locale locale ) {
        return I18n.getLocalizationProblems(JpaConnectorI18n.class, locale);
    }
}
