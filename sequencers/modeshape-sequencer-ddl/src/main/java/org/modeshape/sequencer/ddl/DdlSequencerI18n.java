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
package org.modeshape.sequencer.ddl;

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
    public static I18n ddlNotScoredByParsers;
    public static I18n unknownParser;

    private DdlSequencerI18n() {
    }

    static {
        try {
            I18n.initialize(DdlSequencerI18n.class);
        } catch (final Exception err) {
            // CHECKSTYLE IGNORE check FOR NEXT 2 LINES
            System.err.println(err);
        }
    }
}
