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

import java.util.ArrayList;
import java.util.List;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.util.StringUtil;
import org.modeshape.sequencer.ddl.DdlTokenStream;
import org.modeshape.sequencer.ddl.StandardDdlLexicon;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.DdlStatement;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.SchemaElementType;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.TeiidReservedWord;
import org.modeshape.sequencer.ddl.node.AstNode;

/**
 * A parser for the Teiid <alter options> DDL statement.
 * <p>
 * <code>
 * ALTER ( VIRTUAL | FOREIGN )? ( TABLE | VIEW | PROCEDURE ) <identifier> ( <alter options list> | <alter column options> )
 * </code>
 */
final class AlterOptionsParser extends StatementParser {

    AlterOptionsParser( final TeiidDdlParser teiidDdlParser ) {
        super(teiidDdlParser);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.dialect.teiid.StatementParser#matches(org.modeshape.sequencer.ddl.DdlTokenStream)
     */
    @Override
    boolean matches( final DdlTokenStream tokens ) {
        return tokens.matches(DdlStatement.ALTER_VIRTUAL_PROCEDURE.tokens())
               || tokens.matches(DdlStatement.ALTER_VIRTUAL_TABLE.tokens())
               || tokens.matches(DdlStatement.ALTER_VIRTUAL_VIEW.tokens())
               || tokens.matches(DdlStatement.ALTER_FOREIGN_PROCEDURE.tokens())
               || tokens.matches(DdlStatement.ALTER_FOREIGN_TABLE.tokens())
               || tokens.matches(DdlStatement.ALTER_FOREIGN_VIEW.tokens())
               || tokens.matches(DdlStatement.ALTER_PROCEDURE.tokens()) || tokens.matches(DdlStatement.ALTER_TABLE.tokens())
               || tokens.matches(DdlStatement.ALTER_VIEW.tokens());
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

        // ALTER TABLE <identifier> <alter options list>
        // ALTER TABLE <identifier> <alter column options>
        // ALTER VIEW <identifier> <alter options list>
        // ALTER VIEW <identifier> <alter column options>
        // ALTER PROCEDURE <identifier> <alter options list>
        // ALTER PROCEDURE <identifier> <alter column options>
        // ALTER VIRTUAL TABLE <identifier> <alter options list>
        // ALTER VIRTUAL TABLE <identifier> <alter column options>
        // ALTER VIRTUAL VIEW <identifier> <alter options list>
        // ALTER VIRTUAL VIEW <identifier> <alter column options>
        // ALTER VIRTUAL PROCEDURE <identifier> <alter options list>
        // ALTER VIRTUAL PROCEDURE <identifier> <alter column options>
        // ALTER FOREIGN TABLE <identifier> <alter options list>
        // ALTER FOREIGN TABLE <identifier> <alter column options>
        // ALTER FOREIGN VIEW <identifier> <alter options list>
        // ALTER FOREIGN VIEW <identifier> <alter column options>
        // ALTER FOREIGN PROCEDURE <identifier> <alter options list>
        // ALTER FOREIGN PROCEDURE <identifier> <alter column options>

        String nodeType = null;
        SchemaElementType schemaElementType = null;
        String refNodeType = null;

        if (tokens.canConsume(DdlStatement.ALTER_TABLE.tokens())) {
            nodeType = TeiidDdlLexicon.AlterOptions.TABLE_STATEMENT;
            schemaElementType = SchemaElementType.FOREIGN;
            refNodeType = TeiidDdlLexicon.CreateTable.TABLE_STATEMENT;
        } else if (tokens.canConsume(DdlStatement.ALTER_VIEW.tokens())) {
            nodeType = TeiidDdlLexicon.AlterOptions.VIEW_STATEMENT;
            schemaElementType = SchemaElementType.FOREIGN;
            refNodeType = TeiidDdlLexicon.CreateTable.VIEW_STATEMENT;
        } else if (tokens.canConsume(DdlStatement.ALTER_PROCEDURE.tokens())) {
            nodeType = TeiidDdlLexicon.AlterOptions.PROCEDURE_STATEMENT;
            schemaElementType = SchemaElementType.FOREIGN;
            refNodeType = TeiidDdlLexicon.CreateProcedure.PROCEDURE_STATEMENT;
        } else if (tokens.canConsume(DdlStatement.ALTER_VIRTUAL_TABLE.tokens())) {
            nodeType = TeiidDdlLexicon.AlterOptions.TABLE_STATEMENT;
            schemaElementType = SchemaElementType.VIRTUAL;
            refNodeType = TeiidDdlLexicon.CreateTable.TABLE_STATEMENT;
        } else if (tokens.canConsume(DdlStatement.ALTER_VIRTUAL_VIEW.tokens())) {
            nodeType = TeiidDdlLexicon.AlterOptions.VIEW_STATEMENT;
            schemaElementType = SchemaElementType.VIRTUAL;
            refNodeType = TeiidDdlLexicon.CreateTable.VIEW_STATEMENT;
        } else if (tokens.canConsume(DdlStatement.ALTER_VIRTUAL_PROCEDURE.tokens())) {
            nodeType = TeiidDdlLexicon.AlterOptions.PROCEDURE_STATEMENT;
            schemaElementType = SchemaElementType.VIRTUAL;
            refNodeType = TeiidDdlLexicon.CreateProcedure.PROCEDURE_STATEMENT;
        } else if (tokens.canConsume(DdlStatement.ALTER_FOREIGN_TABLE.tokens())) {
            nodeType = TeiidDdlLexicon.AlterOptions.TABLE_STATEMENT;
            schemaElementType = SchemaElementType.FOREIGN;
            refNodeType = TeiidDdlLexicon.CreateTable.TABLE_STATEMENT;
        } else if (tokens.canConsume(DdlStatement.ALTER_FOREIGN_VIEW.tokens())) {
            nodeType = TeiidDdlLexicon.AlterOptions.VIEW_STATEMENT;
            schemaElementType = SchemaElementType.FOREIGN;
            refNodeType = TeiidDdlLexicon.CreateTable.VIEW_STATEMENT;
        } else if (tokens.canConsume(DdlStatement.ALTER_FOREIGN_PROCEDURE.tokens())) {
            nodeType = TeiidDdlLexicon.AlterOptions.PROCEDURE_STATEMENT;
            schemaElementType = SchemaElementType.FOREIGN;
            refNodeType = TeiidDdlLexicon.CreateProcedure.PROCEDURE_STATEMENT;
        } else {
            throw new TeiidDdlParsingException(tokens, "Unparsable alter options statement");
        }

        assert (nodeType != null) : "Create alter options node type is null";
        assert (schemaElementType != null) : "Create alter options schema element type is null";
        assert (refNodeType != null) : "Create alter options reference node type is null";

        // parse table reference
        final String tableRefName = parseIdentifier(tokens);

        final AstNode alterOptionsNode = getNodeFactory().node(tableRefName, parentNode, nodeType);
        alterOptionsNode.setProperty(TeiidDdlLexicon.SchemaElement.TYPE, schemaElementType.toDdl());

        // find referenced table node
        final AstNode tableRefNode = getNode(parentNode, tableRefName, refNodeType);

        // can't find referenced table
        if (tableRefNode == null) {
            this.logger.debug("Alter options statement table reference '{0}' node not found", tableRefName);
        } else {
            alterOptionsNode.setProperty(TeiidDdlLexicon.AlterOptions.REFERENCE, tableRefNode);
        }

        // must have either a <alter options list> or a <alter column options>
        if (!parseAlterOptionsList(tokens, alterOptionsNode) && !parseAlterColumnOptions(tokens, alterOptionsNode)) {
            throw new TeiidDdlParsingException(tokens, "Unparsable alter options statement");
        }

        return alterOptionsNode;
    }

    /**
     * <alter column options> <code>
     * ALTER ( COLUMN | PARAMETER )? <identifier> <alter options list>
     * </code>
     * 
     * @param tokens the tokens being processed (cannot be <code>null</code>)
     * @param alterOptionsNode the alter options node of the alter column options (cannot be <code>null</code>)
     * @return <code>true</code> if an alter column options clause was successfully parsed
     * @throws ParsingException if there is a problem parsing the alter options statement
     */
    boolean parseAlterColumnOptions( final DdlTokenStream tokens,
                                     final AstNode alterOptionsNode ) throws ParsingException {

        // ALTER COLUMN <identifier> <alter options list>
        // ALTER PARAMATER <identifier> <alter options list>
        // ALTER <identifier> <alter options list>

        if (tokens.canConsume(TeiidReservedWord.ALTER.toDdl())) {
            String nodeType = null;

            if (tokens.canConsume(TeiidReservedWord.COLUMN.toDdl())) {
                nodeType = TeiidDdlLexicon.AlterOptions.COLUMN;
            } else if (tokens.canConsume(TeiidReservedWord.PARAMETER.toDdl())) {
                nodeType = TeiidDdlLexicon.AlterOptions.PARAMETER;
            } else {
                nodeType = TeiidDdlLexicon.AlterOptions.COLUMN;
            }

            assert (nodeType != null) : "Alter column options node type is null";

            final String refName = parseIdentifier(tokens);
            final AstNode columnOptionsNode = getNodeFactory().node(refName, alterOptionsNode, nodeType);

            // find referenced column/parameter
            final AstNode refTableNode = (AstNode)alterOptionsNode.getProperty(TeiidDdlLexicon.AlterOptions.REFERENCE);

            if (refTableNode == null) {
                this.logger.debug("Table/procedure/view node not found for alter column '{0}'", refName);
            } else {
                String refPropType = null;

                if (refTableNode.hasMixin(TeiidDdlLexicon.CreateProcedure.PROCEDURE_STATEMENT)) {
                    refPropType = TeiidDdlLexicon.CreateProcedure.PARAMETER;
                } else {
                    refPropType = TeiidDdlLexicon.CreateTable.TABLE_ELEMENT;
                }

                final AstNode refNode = getNode(refTableNode, refName, refPropType);

                // can't find referenced column node
                if (refNode == null) {
                    this.logger.debug("Alter column options reference column node not found: {0}", refName);
                }

                columnOptionsNode.setProperty(TeiidDdlLexicon.AlterOptions.REFERENCE, refNode);
            }

            if (parseAlterOptionsList(tokens, columnOptionsNode)) {
                return true; // well formed
            }

            throw new TeiidDdlParsingException(tokens, "Unparsable alter column options clause");
        }

        return false;
    }

    /**
     * <alter options list> <code>
     * OPTIONS <lparen> ( <add set option> | <drop option> ) ( <comma> ( <add set option> | <drop option> ) )* <rparen>
     * 
     * <add set option> == ( ADD | SET ) <option pair>
     * <drop option> == DROP <identifier>
     * </code>
     * 
     * @param tokens the tokens being processed (cannot be <code>null</code>)
     * @param parentNode the parent node of the alter options list (cannot be <code>null</code>)
     * @return <code>true</code> if an alter options list clause was successfully parsed; <code>false</code> if not an alter
     *         options list clause
     * @throws ParsingException if there is a problem parsing the alter options list clause
     */
    @SuppressWarnings( "unchecked" )
    boolean parseAlterOptionsList( final DdlTokenStream tokens,
                                   final AstNode parentNode ) throws ParsingException {

        // OPTIONS (<add set option>)
        // OPTIONS (<drop option>)
        // OPTIONS (<add set option>, <drop option>, <drop option>, <add set option>)

        if (tokens.matches(TeiidReservedWord.OPTIONS.toDdl())) {
            // must have opening paren
            if (!tokens.canConsume(TeiidReservedWord.OPTIONS.toDdl(), L_PAREN)) {
                throw new TeiidDdlParsingException(tokens, "Unparsable alter options list (left paren not found)");
            }

            if (tokens.matches(TeiidReservedWord.ADD.toDdl()) || tokens.matches(TeiidReservedWord.SET.toDdl())
                || tokens.matches(TeiidReservedWord.DROP.toDdl())) {

                // create options list node
                AstNode optionsListNode = null;

                if (parentNode.hasMixin(TeiidDdlLexicon.AlterOptions.COLUMN)
                    || parentNode.hasMixin(TeiidDdlLexicon.AlterOptions.PARAMETER)) {
                    optionsListNode = parentNode;
                } else {
                    optionsListNode = getNodeFactory().node(TeiidDdlLexicon.AlterOptions.ALTERS,
                                                            parentNode,
                                                            TeiidDdlLexicon.AlterOptions.OPTIONS_LIST);
                }

                // will have one or more add, set, or drop clauses separated by comma
                boolean keepParsing = tokens.hasNext();
                boolean foundOption = false;
                boolean foundComma = false;

                while (keepParsing) {
                    if (tokens.canConsume(TeiidReservedWord.ADD.toDdl()) || tokens.canConsume(TeiidReservedWord.SET.toDdl())) {
                        final String option = parseIdentifier(tokens);
                        final String value = parseValue(tokens);

                        // only add if there is a value
                        if (!StringUtil.isBlank(value)) {
                            final AstNode optionsNode = getNodeFactory().node(option,
                                                                              optionsListNode,
                                                                              StandardDdlLexicon.TYPE_STATEMENT_OPTION);
                            optionsNode.setProperty(StandardDdlLexicon.VALUE, value);
                        }

                        foundOption = true;
                        foundComma = false;
                    } else if (tokens.canConsume(TeiidReservedWord.DROP.toDdl())) {
                        final String option = parseIdentifier(tokens);
                        List<String> values = (List<String>)optionsListNode.getProperty(TeiidDdlLexicon.AlterOptions.DROPPED);

                        if (values == null) {
                            values = new ArrayList<String>();
                        }

                        values.add(option);
                        optionsListNode.setProperty(TeiidDdlLexicon.AlterOptions.DROPPED, values);

                        foundOption = true;
                        foundComma = false;
                    } else if (tokens.canConsume(COMMA)) {
                        // found comma before option
                        if (!foundOption) {
                            throw new TeiidDdlParsingException(tokens, "Unparsable alter options list (misplaced comma found)");
                        }

                        // 2 consecutive commas
                        if (foundComma) {
                            throw new TeiidDdlParsingException(tokens, "Unparsable alter options list (consecutive commas found)");
                        }

                        foundComma = true;
                    } else if (foundComma) {
                        throw new TeiidDdlParsingException(tokens, "Unparsable alter options list (comma found at end)");
                    } else {
                        keepParsing = false;
                    }
                }

                // must have ending paren
                if (!tokens.canConsume(R_PAREN)) {
                    throw new TeiidDdlParsingException(tokens, "Unparsable alter options list (right paren not found)");
                }

                return true;
            }

            throw new TeiidDdlParsingException(tokens, "Unparsable alter options list (no add, set, or drop found)");
        }

        return false; // not an <alter options list> clause
    }
    
	@Override
	protected void postProcess(AstNode rootNode) {
		
	}

}
