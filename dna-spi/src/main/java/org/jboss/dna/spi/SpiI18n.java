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
package org.jboss.dna.spi;

import java.util.Locale;
import java.util.Set;
import org.jboss.dna.common.CommonI18n;
import org.jboss.dna.common.i18n.I18n;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
public final class SpiI18n {

    public static I18n closedConnectionMayNotBeUsed;
    public static I18n errorConvertingIo;
    public static I18n errorConvertingType;
    public static I18n errorReadingPropertyValueBytes;
    public static I18n invalidIndexInSegmentName;
    public static I18n invalidQualifiedNameString;
    public static I18n maximumPoolSizeMayNotBeSmallerThanCorePoolSize;
    public static I18n missingEndBracketInSegmentName;
    public static I18n noNamespaceRegisteredForPrefix;
    public static I18n pathAncestorDegreeIsInvalid;
    public static I18n pathCannotBeNormalized;
    public static I18n pathIsAlreadyAbsolute;
    public static I18n pathIsNotAbsolute;
    public static I18n pathIsNotRelative;
    public static I18n repositoryConnectionPoolIsNotRunning;
    public static I18n unableToCreateSubpathBeginIndexGreaterThanOrEqualToEndingIndex;
    public static I18n unableToCreateSubpathBeginIndexGreaterThanOrEqualToSize;
    public static I18n unableToCreateValue;
    public static I18n unableToDiscoverPropertyTypeForNullValue;
    public static I18n unableToObtainValidRepositoryAfterAttempts;
    public static I18n validPathMayNotContainEmptySegment;
    public static I18n valueJavaTypeNotCompatibleWithPropertyType;
    public static I18n pathExpressionMayNotBeBlank;
    public static I18n pathExpressionIsInvalid;
    public static I18n pathExpressionHasInvalidSelect;
    public static I18n pathExpressionHasInvalidMatch;

    public static I18n executingGraphCommand;
    public static I18n executedGraphCommand;
    public static I18n closingCommandExecutor;
    public static I18n closedCommandExecutor;

    static {
        try {
            I18n.initialize(SpiI18n.class);
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
