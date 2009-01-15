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
package org.jboss.dna.common;

import java.util.Locale;
import java.util.Set;
import org.jboss.dna.common.i18n.I18n;

/**
 * @author John Verhaeg
 * @author Randall Hauch
 */
public final class CommonI18n {

    // Make sure the following I18n.java-related fields are defined before all other fields to ensure a valid error message is
    // produced in the event of a missing/duplicate/unused property

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

    // Core-related fields
    public static I18n argumentDidNotContainKey;
    public static I18n argumentDidNotContainObject;
    public static I18n argumentMayNotBeEmpty;
    public static I18n argumentMayNotBeGreaterThan;
    public static I18n argumentMayNotBeLessThan;
    public static I18n argumentMayNotBeNegative;
    public static I18n argumentMayNotBeNull;
    public static I18n argumentMayNotBeNullOrZeroLength;
    public static I18n argumentMayNotBeNullOrZeroLengthOrEmpty;
    public static I18n argumentMayNotBePositive;
    public static I18n argumentMayNotContainNullValue;
    public static I18n argumentMustBeEmpty;
    public static I18n argumentMustBeEquals;
    public static I18n argumentMustBeGreaterThan;
    public static I18n argumentMustBeGreaterThanOrEqualTo;
    public static I18n argumentMustBeInstanceOf;
    public static I18n argumentMustBeLessThan;
    public static I18n argumentMustBeLessThanOrEqualTo;
    public static I18n argumentMustBeNegative;
    public static I18n argumentMustBeNull;
    public static I18n argumentMustBeNumber;
    public static I18n argumentMustBeOfMaximumSize;
    public static I18n argumentMustBeOfMinimumSize;
    public static I18n argumentMustBePositive;
    public static I18n argumentMustBeSameAs;
    public static I18n argumentMustNotBeEquals;
    public static I18n argumentMustNotBeSameAs;
    public static I18n componentClassnameNotValid;
    public static I18n componentNotConfigured;
    public static I18n dateParsingFailure;
    public static I18n initialActivityMonitorTaskName;
    public static I18n nullActivityMonitorTaskName;
    public static I18n pathAncestorDegreeIsInvalid;
    public static I18n pathCannotBeNormalized;
    public static I18n pathIsAlreadyAbsolute;
    public static I18n pathIsNotAbsolute;
    public static I18n pathIsNotRelative;
    public static I18n requiredToSuppliedParameterMismatch;

    static {
        try {
            I18n.initialize(CommonI18n.class);
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
