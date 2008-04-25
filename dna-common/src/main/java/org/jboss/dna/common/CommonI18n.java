/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
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

    /**
     * Parameters:
     * <ol>
     * <li>{@link I18n#id() ID}</li>
     * <li>Number of supplied arguments</li>
     * <li>Localized text before parameter substitution</li>
     * <li>Localized text after parameter substitution</li>
     * </ol>
     */
    public static I18n i18nArgumentsMismatchedParameter;
    public static I18n i18nArgumentMismatchedParameters;
    public static I18n i18nArgumentsMismatchedParameters;
    public static I18n i18nReplaceArgumentsMismatchedParameter;
    public static I18n i18nReplaceArgumentMismatchedParameters;
    public static I18n i18nReplaceArgumentsMismatchedParameters;
    public static I18n i18nClassInterface;
    public static I18n i18nClassNotPublic;
    public static I18n i18nFieldFinal;
    public static I18n i18nFieldInvalidType;
    public static I18n i18nFieldNotPublic;
    public static I18n i18nFieldNotStatic;
    public static I18n i18nLocalizationFileNotFound;
    public static I18n i18nLocalizationProblems;

    /**
     * Parameters:
     * <ol>
     * <li>{@link I18n#id() Property}</li>
     * <li>Localization file URL</li>
     * </ol>
     */
    public static I18n i18nPropertyDuplicate;
    public static I18n i18nPropertyMissing;
    public static I18n i18nPropertyUnused;

    // Core-related fields
    public static I18n componentClassnameNotValid;
    public static I18n componentNotConfigured;
    public static I18n progressMonitorBeginTask;
    public static I18n progressMonitorStatus;
    public static I18n nullProgressMonitorTaskName;

    public static I18n argumentMayNotBeNegative;
    public static I18n argumentMayNotBePositive;
    public static I18n argumentMustBeNegative;
    public static I18n argumentMustBePositive;
    public static I18n argumentMustBeNumber;
    public static I18n argumentMayNotBeNullOrZeroLength;
    public static I18n argumentMayNotBeNullOrZeroLengthOrEmpty;
    public static I18n argumentMayNotBeNull;
    public static I18n argumentMustBeNull;
    public static I18n argumentMustBeInstanceOf;
    public static I18n argumentMustBeSameAs;
    public static I18n argumentMustNotBeSameAs;
    public static I18n argumentMustBeEquals;
    public static I18n argumentMustNotBeEquals;
    public static I18n argumentMayNotBeEmpty;
    public static I18n argumentDidNotContainObject;
    public static I18n argumentDidNotContainKey;
    public static I18n argumentMayNotContainNullValue;

    public static I18n dateParsingFailure;

    public static I18n pathAncestorDegreeIsInvalid;
    public static I18n pathIsAlreadyAbsolute;
    public static I18n pathIsNotAbsolute;
    public static I18n pathIsNotRelative;
    public static I18n pathCannotBeNormalized;

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
