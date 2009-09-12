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
package org.jboss.dna.connector.store.jpa;

import java.util.Locale;
import java.util.Set;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.i18n.I18n;

/**
 * The internationalized string constants for the <code>org.jboss.dna.connector.store.jpa*</code> packages.
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
    public static I18n unableToDeleteBecauseOfReferences;

    public static I18n workspaceAlreadyExists;
    public static I18n workspaceDoesNotExist;
    public static I18n unableToCreateWorkspaces;
    public static I18n connectionIsNoLongerOpen;

    public static I18n basicModelDescription;

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
