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
package org.modeshape.sequencer.ddl.dialect.teiid;

import org.modeshape.common.text.ParsingException;
import org.modeshape.sequencer.ddl.DdlTokenStream;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.DdlStatement;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.TeiidReservedWord;
import org.modeshape.sequencer.ddl.node.AstNode;

/**
 * A parser for the Teiid <option namespace> DDL statement
 * <p>
 * <code>
 * SET NAMESPACE <string> AS <identifier>
 * </code>
 */
final class OptionNamespaceParser extends StatementParser {

    OptionNamespaceParser( final TeiidDdlParser teiidDdlParser ) {
        super(teiidDdlParser);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.dialect.teiid.StatementParser#matches(org.modeshape.sequencer.ddl.DdlTokenStream)
     */
    @Override
    boolean matches( final DdlTokenStream tokens ) {
        return tokens.matches(DdlStatement.OPTION_NAMESPACE.tokens());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.dialect.teiid.StatementParser#parse(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode)
     */
    @Override
    AstNode parse( final DdlTokenStream tokens,
                   final AstNode parentNode ) throws ParsingException {
        if (tokens.canConsume(DdlStatement.OPTION_NAMESPACE.tokens())) {
            final String uri = parseValue(tokens);

            if (tokens.canConsume(TeiidReservedWord.AS.toDdl())) {
                final String alias = parseIdentifier(tokens);
                addNamespaceAlias(alias, uri);
                final AstNode optionNamespaceNode = getNodeFactory().node(alias,
                                                                          parentNode,
                                                                          TeiidDdlLexicon.OptionNamespace.STATEMENT);
                optionNamespaceNode.setProperty(TeiidDdlLexicon.OptionNamespace.URI, uri);
                return optionNamespaceNode;
            }
        }

        throw new TeiidDdlParsingException(tokens, "Unparsable option namespace statement");
    }

    @Override
    protected void postProcess( AstNode rootNode ) {

    }
}
