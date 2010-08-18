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
package org.modeshape.common;

import java.util.Locale;
import java.util.Set;
import org.modeshape.common.i18n.I18n;

/**
 * The internationalized string constants for the <code>org.modeshape.common*</code> packages.
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
    public static I18n unableToAccessResourceFileFromClassLoader;

    // TokenStream
    public static I18n noMoreContent;
    public static I18n noMoreContentButWasExpectingToken;
    public static I18n unexpectedToken;
    public static I18n noMoreContentButWasExpectingCharacter;
    public static I18n unexpectedCharacter;
    public static I18n noMoreContentButWasExpectingTokenType;
    public static I18n unexpectedTokenType;
    public static I18n startMethodMustBeCalledBeforeNext;
    public static I18n startMethodMustBeCalledBeforeConsumingOrMatching;
    public static I18n noMatchingDoubleQuoteFound;
    public static I18n noMatchingSingleQuoteFound;
    public static I18n expectingValidIntegerAtLineAndColumn;
    public static I18n expectingValidLongAtLineAndColumn;
    public static I18n expectingValidBooleanAtLineAndColumn;
    public static I18n endPositionMustBeGreaterThanStartingPosition;

    // ComponentConfig annotations
    public static I18n componentConfigNamePropertyDescription;
    public static I18n componentConfigNamePropertyLabel;
    public static I18n componentConfigNamePropertyCategory;
    public static I18n componentConfigDescriptionPropertyDescription;
    public static I18n componentConfigDescriptionPropertyLabel;
    public static I18n componentConfigDescriptionPropertyCategory;
    public static I18n componentConfigClassnamePropertyDescription;
    public static I18n componentConfigClassnamePropertyLabel;
    public static I18n componentConfigClassnamePropertyCategory;
    public static I18n componentConfigClasspathPropertyDescription;
    public static I18n componentConfigClasspathPropertyLabel;
    public static I18n componentConfigClasspathPropertyCategory;

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
