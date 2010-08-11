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
package org.modeshape.connector.jcr;

import java.util.Locale;
import java.util.Set;
import org.modeshape.common.i18n.I18n;

/**
 * The internationalized string constants for the <code>org.modeshape.connector.filesystem*</code> packages.
 */
public final class JcrConnectorI18n {

    public static I18n connectorName;
    public static I18n errorSerializingObjectUsedInSource;
    public static I18n errorConvertingJcrValueOfType;
    public static I18n objectFoundInJndiWasNotRepository;
    public static I18n repositoryObjectNotFoundInJndi;
    public static I18n propertyIsRequired;
    public static I18n workspaceAlreadyExistsInRepository;
    public static I18n unableToCreateWorkspaceInRepository;
    public static I18n unableToDestroyWorkspaceInRepository;
    public static I18n unableToFindNodeWithoutPathOrUuid;
    public static I18n unableToFindNodeWithUuid;
    public static I18n nodeDoesNotExist;

    public static I18n namePropertyDescription;
    public static I18n namePropertyLabel;
    public static I18n namePropertyCategory;
    public static I18n retryLimitPropertyDescription;
    public static I18n retryLimitPropertyLabel;
    public static I18n retryLimitPropertyCategory;
    public static I18n updatesAllowedPropertyDescription;
    public static I18n updatesAllowedPropertyLabel;
    public static I18n updatesAllowedPropertyCategory;

    public static I18n repositoryJndiNamePropertyDescription;
    public static I18n repositoryJndiNamePropertyLabel;
    public static I18n repositoryJndiNamePropertyCategory;

    public static I18n passwordPropertyDescription;
    public static I18n passwordPropertyLabel;
    public static I18n passwordPropertyCategory;
    public static I18n usernamePropertyDescription;
    public static I18n usernamePropertyLabel;
    public static I18n usernamePropertyCategory;

    static {
        try {
            I18n.initialize(JcrConnectorI18n.class);
        } catch (final Exception err) {
            System.err.println(err);
        }
    }

    public static Set<Locale> getLocalizationProblemLocales() {
        return I18n.getLocalizationProblemLocales(JcrConnectorI18n.class);
    }

    public static Set<String> getLocalizationProblems() {
        return I18n.getLocalizationProblems(JcrConnectorI18n.class);
    }

    public static Set<String> getLocalizationProblems( Locale locale ) {
        return I18n.getLocalizationProblems(JcrConnectorI18n.class, locale);
    }
}
