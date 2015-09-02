/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.common;

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
    public static I18n i18nBundleNotFoundInClasspath;

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
    public static I18n argumentMustBePowerOfTwo;
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

    public static I18n errorInitializingCustomLoggerFactory;

    public static I18n customLoggingAvailable;
    public static I18n slf4jAvailable;
    public static I18n log4jAvailable;
    public static I18n jdkFallback;

    public static I18n errorWhileClosingRingBufferConsumer;
    public static I18n errorClosingWrappedStream;
    
    public static I18n incorrectRingBufferSize;

    private CommonI18n() {
    }

    static {
        try {
            I18n.initialize(CommonI18n.class);
        } catch (final Exception err) {
            // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
            System.err.println(err);
        }
    }
}
