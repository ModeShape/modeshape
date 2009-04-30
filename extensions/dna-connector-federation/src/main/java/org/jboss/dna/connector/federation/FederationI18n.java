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
package org.jboss.dna.connector.federation;

import java.util.Locale;
import java.util.Set;
import org.jboss.dna.common.i18n.I18n;

/**
 * @author Randall Hauch
 */
public final class FederationI18n {

    public static I18n requiredNodeDoesNotExistRelativeToNode;
    public static I18n propertyIsRequired;
    public static I18n nodeDoesNotExistAtPath;
    public static I18n nodeDoesNotExistAtLocation;
    public static I18n errorRemovingNodeFromCache;

    public static I18n unableToCreateExecutionContext;
    public static I18n federatedRepositoryCannotBeFound;
    public static I18n unableToAuthenticateConnectionToFederatedRepository;
    public static I18n repositoryHasBeenShutDown;
    public static I18n errorAddingProjectionRuleParseMethod;
    public static I18n pathInProjection;
    public static I18n pathsInProjection;
    public static I18n unableToPerformOperationUnlessLocationIsFromSingleProjection;
    public static I18n unableToPerformOperationUnlessLocationsAreFromSingleProjection;
    public static I18n unableToPerformOperationSpanningWorkspaces;

    public static I18n workspaceDoesNotExist;
    public static I18n noDefaultWorkspace;

    static {
        try {
            I18n.initialize(FederationI18n.class);
        } catch (final Exception err) {
            System.err.println(err);
        }
    }

    public static Set<Locale> getLocalizationProblemLocales() {
        return I18n.getLocalizationProblemLocales(FederationI18n.class);
    }

    public static Set<String> getLocalizationProblems() {
        return I18n.getLocalizationProblems(FederationI18n.class);
    }

    public static Set<String> getLocalizationProblems( Locale locale ) {
        return I18n.getLocalizationProblems(FederationI18n.class, locale);
    }
}
