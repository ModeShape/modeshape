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
package org.modeshape.sequencer.ddl;

import java.util.Locale;
import java.util.Set;
import org.modeshape.common.i18n.I18n;

/**
 * The internationalized string constants for the <code>org.modeshape.sequencer.ddl*</code> packages.
 */
public final class DdlSequencerI18n {

    public static I18n sequencerTaskName;
    public static I18n errorSequencingDdlContent;
    public static I18n errorParsingDdlContent;
    public static I18n unknownCreateStatement;
    public static I18n unusedTokensDiscovered;
    public static I18n unusedTokensParsingColumnsAndConstraints;
    public static I18n unusedTokensParsingColumnDefinition;
    public static I18n alterTableOptionNotFound;
    public static I18n unusedTokensParsingCreateIndex;
    public static I18n missingReturnTypeForFunction;
    public static I18n unsupportedProcedureParameterDeclaration;
    public static I18n errorInstantiatingParserForGrammarUsingDefaultClasspath;
    public static I18n errorInstantiatingParserForGrammarClasspath;

    static {
        try {
            I18n.initialize(DdlSequencerI18n.class);
        } catch (final Exception err) {
            System.err.println(err);
        }
    }

    public static Set<Locale> getLocalizationProblemLocales() {
        return I18n.getLocalizationProblemLocales(DdlSequencerI18n.class);
    }

    public static Set<String> getLocalizationProblems() {
        return I18n.getLocalizationProblems(DdlSequencerI18n.class);
    }

    public static Set<String> getLocalizationProblems( Locale locale ) {
        return I18n.getLocalizationProblems(DdlSequencerI18n.class, locale);
    }
}
