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
package org.modeshape.sequencer.ddl.dialect.teiid;

import org.modeshape.common.text.ParsingException;
import org.modeshape.sequencer.ddl.DdlTokenStream;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.DdlStatement;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.TeiidNonReservedWord;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.TeiidReservedWord;
import org.modeshape.sequencer.ddl.node.AstNode;

/**
 * A parser for the Teiid <create trigger> DDL statement.
 * <p>
 * <code>
 * CREATE TRIGGER ON <identifier> INSTEAD OF ( INSERT | UPDATE | DELETE ) AS <for each row trigger action>
 * 
 * <for each row trigger action> == FOR EACH ROW ( ( BEGIN ( ATOMIC )? ( <statement> )* END ) | <statement> )
 * <statement> == ( ( <identifier> <colon> )? ( <loop statement> | <while statement> | <compound statement> ) )
 * </code>
 */
final class CreateTriggerParser extends StatementParser {

    static final String[] FOR_EACH_ROW = new String[] {TeiidReservedWord.FOR.toDdl(), TeiidReservedWord.EACH.toDdl(),
        TeiidReservedWord.ROW.toDdl()};
    static final String[] INSTEAD_OF = new String[] {TeiidNonReservedWord.INSTEAD.toDdl(), TeiidReservedWord.OF.toDdl()};

    CreateTriggerParser( final TeiidDdlParser teiidDdlParser ) {
        super(teiidDdlParser);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.dialect.teiid.StatementParser#matches(org.modeshape.sequencer.ddl.DdlTokenStream)
     */
    @Override
    boolean matches( final DdlTokenStream tokens ) {
        return tokens.matches(DdlStatement.CREATE_TRIGGER.tokens());
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

        // CREATE TRIGGER ON <identifier> INSTEAD OF INSERT AS FOR EACH ROW <for each row trigger action>
        // CREATE TRIGGER ON <identifier> INSTEAD OF UPDATE AS FOR EACH ROW <for each row trigger action>
        // CREATE TRIGGER ON <identifier> INSTEAD OF DELETE AS FOR EACH ROW <for each row trigger action>

        if (tokens.canConsume(DdlStatement.CREATE_TRIGGER.tokens())) {
            final String tableRefName = parseIdentifier(tokens);

            if (tokens.canConsume(INSTEAD_OF)) {
                if (tokens.matches(INSERT) || tokens.matches(UPDATE) || tokens.matches(DELETE)) {
                    final String triggerType = tokens.consume();

                    if (tokens.canConsume(TeiidReservedWord.AS.toDdl())) {
                        final AstNode triggerNode = getNodeFactory().node(tableRefName,
                                                                          parentNode,
                                                                          TeiidDdlLexicon.CreateTrigger.STATEMENT);
                        triggerNode.setProperty(TeiidDdlLexicon.CreateTrigger.INSTEAD_OF, triggerType);

                        // find referenced table node
                        final AstNode tableRefNode = getNode(parentNode,
                                                             tableRefName,
                                                             TeiidDdlLexicon.CreateTable.TABLE_STATEMENT,
                                                             TeiidDdlLexicon.CreateTable.VIEW_STATEMENT);

                        // can't find referenced table node
                        if (tableRefNode == null) {
                            this.logger.debug("Create trigger statement table reference '{0}' node not found", tableRefName);
                        } else {
                            triggerNode.setProperty(TeiidDdlLexicon.CreateTrigger.TABLE_REFERENCE, tableRefNode);
                        }

                        if (tokens.canConsume(FOR_EACH_ROW)) {
                            boolean error = false;
                            final boolean beginBlock = tokens.canConsume(TeiidReservedWord.BEGIN.toDdl());
                            boolean atomic = ((beginBlock) ? tokens.canConsume(TeiidReservedWord.ATOMIC.toDdl()) : true);
                            triggerNode.setProperty(TeiidDdlLexicon.CreateTrigger.ATOMIC, atomic);

                            { // first row action
                                final String rowAction = parseUntilTerminatorIgnoreEmbeddedStatements(tokens).trim();
                                final AstNode rowActionNode = getNodeFactory().node(TeiidDdlLexicon.CreateTrigger.ROW_ACTION,
                                                                                    triggerNode,
                                                                                    TeiidDdlLexicon.CreateTrigger.TRIGGER_ROW_ACTION);

                                if (!tokens.canConsume(SEMICOLON)) {
                                    throw new TeiidDdlParsingException(tokens, "Unexpected create trigger statement");
                                }

                                rowActionNode.setProperty(TeiidDdlLexicon.CreateTrigger.ACTION, rowAction + ';');
                            }

                            if (beginBlock) {
                                // if only one statement will get an end here
                                if (!tokens.canConsume(TeiidReservedWord.END.toDdl())) {
                                    // multi statements found
                                    error = true;

                                    while (true) {
                                        if (!tokens.hasNext()) {
                                            break;
                                        }

                                        final String rowAction = parseUntilTerminatorIgnoreEmbeddedStatements(tokens).trim();
                                        final AstNode rowActionNode = getNodeFactory().node(TeiidDdlLexicon.CreateTrigger.ROW_ACTION,
                                                                                            triggerNode,
                                                                                            TeiidDdlLexicon.CreateTrigger.TRIGGER_ROW_ACTION);
                                        rowActionNode.setProperty(TeiidDdlLexicon.CreateTrigger.ACTION, rowAction);

                                        if (!tokens.canConsume(SEMICOLON)) {
                                            throw new TeiidDdlParsingException(tokens, "Unexpected create trigger statement");
                                        }

                                        rowActionNode.setProperty(TeiidDdlLexicon.CreateTrigger.ACTION, rowAction + ';');

                                        // make sure end found
                                        if (tokens.canConsume(TeiidReservedWord.END.toDdl())) {
                                            error = false;
                                            break;
                                        }
                                    }
                                }
                            }

                            if (!error) {
                                return triggerNode;
                            }
                        }
                    }
                }
            }
        }

        throw new TeiidDdlParsingException(tokens, "Unparsable create trigger statement");
    }
    
	@Override
	protected void postProcess(AstNode rootNode) {
		
	}
}
