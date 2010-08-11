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
package org.modeshape.connector.jbosscache;

import java.util.Locale;
import java.util.Set;
import org.modeshape.common.i18n.I18n;

/**
 * The internationalized string constants for the <code>org.modeshape.connector.jbosscache*</code> packages.
 */
public final class JBossCacheConnectorI18n {

    public static I18n connectorName;
    public static I18n propertyIsRequired;
    public static I18n errorSerializingCachePolicyInSource;
    public static I18n objectFoundInJndiWasNotCacheFactory;
    public static I18n unableToCreateWorkspace;
    public static I18n configFileNotFound;
    public static I18n configFileNotValid;
    public static I18n workspaceNameWasNotValidConfiguration;
    public static I18n defaultCacheFactoryConfigurationNameWasNotValidConfiguration;

    public static I18n namePropertyDescription;
    public static I18n namePropertyLabel;
    public static I18n namePropertyCategory;
    public static I18n rootNodeUuidPropertyDescription;
    public static I18n rootNodeUuidPropertyLabel;
    public static I18n rootNodeUuidPropertyCategory;
    public static I18n cacheJndiNamePropertyDescription;
    public static I18n cacheJndiNamePropertyLabel;
    public static I18n cacheJndiNamePropertyCategory;
    public static I18n cacheFactoryJndiNamePropertyDescription;
    public static I18n cacheFactoryJndiNamePropertyLabel;
    public static I18n cacheFactoryJndiNamePropertyCategory;
    public static I18n cacheConfigurationNamePropertyDescription;
    public static I18n cacheConfigurationNamePropertyLabel;
    public static I18n cacheConfigurationNamePropertyCategory;
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

    static {
        try {
            I18n.initialize(JBossCacheConnectorI18n.class);
        } catch (final Exception err) {
            System.err.println(err);
        }
    }

    public static Set<Locale> getLocalizationProblemLocales() {
        return I18n.getLocalizationProblemLocales(JBossCacheConnectorI18n.class);
    }

    public static Set<String> getLocalizationProblems() {
        return I18n.getLocalizationProblems(JBossCacheConnectorI18n.class);
    }

    public static Set<String> getLocalizationProblems( Locale locale ) {
        return I18n.getLocalizationProblems(JBossCacheConnectorI18n.class, locale);
    }
}
