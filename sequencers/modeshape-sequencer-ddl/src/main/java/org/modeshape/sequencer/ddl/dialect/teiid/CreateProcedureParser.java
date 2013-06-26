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

import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DATATYPE_LENGTH;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DATATYPE_NAME;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DATATYPE_PRECISION;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DATATYPE_SCALE;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.text.Position;
import org.modeshape.common.util.StringUtil;
import org.modeshape.sequencer.ddl.DdlTokenStream;
import org.modeshape.sequencer.ddl.StandardDdlLexicon;
import org.modeshape.sequencer.ddl.datatype.DataType;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.DdlStatement;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.SchemaElementType;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.TeiidNonReservedWord;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.TeiidReservedWord;
import org.modeshape.sequencer.ddl.node.AstNode;

/**
 * A parser for the Teiid <create procedure> DDL statement.
 * <p>
 * <code>
 * CREATE ( VIRTUAL | FOREIGN )? ( PROCEDURE | FUNCTION ) ( <identifier> <lparen> ( <procedure parameter> ( <comma> <procedure parameter> )* )? <rparen> ( RETURNS ( ( ( TABLE )? <lparen> <procedure result column> ( <comma> <procedure result column> )* <rparen> ) | <data type> ) )? ( <options clause> )? ( AS <statement> )? )
 * </code>
 */
final class CreateProcedureParser extends StatementParser {

    CreateProcedureParser( final TeiidDdlParser teiidDdlParser ) {
        super(teiidDdlParser);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.dialect.teiid.StatementParser#matches(org.modeshape.sequencer.ddl.DdlTokenStream)
     */
    @Override
    boolean matches( final DdlTokenStream tokens ) {
        return tokens.matches(DdlStatement.CREATE_VIRTUAL_FUNCTION.tokens())
               || tokens.matches(DdlStatement.CREATE_VIRTUAL_PROCEDURE.tokens())
               || tokens.matches(DdlStatement.CREATE_FOREIGN_FUNCTION.tokens())
               || tokens.matches(DdlStatement.CREATE_FOREIGN_PROCEDURE.tokens())
               || tokens.matches(DdlStatement.CREATE_FUNCTION.tokens()) || tokens.matches(DdlStatement.CREATE_PROCEDURE.tokens());
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
        boolean procedure = true;
        DdlStatement stmt = null;
        SchemaElementType schemaElementType = null;

        if (tokens.canConsume(DdlStatement.CREATE_VIRTUAL_FUNCTION.tokens())) {
            stmt = DdlStatement.CREATE_VIRTUAL_FUNCTION;
            schemaElementType = SchemaElementType.VIRTUAL;
            procedure = false;
        } else if (tokens.canConsume(DdlStatement.CREATE_VIRTUAL_PROCEDURE.tokens())) {
            stmt = DdlStatement.CREATE_VIRTUAL_PROCEDURE;
            schemaElementType = SchemaElementType.VIRTUAL;
        } else if (tokens.canConsume(DdlStatement.CREATE_FOREIGN_FUNCTION.tokens())) {
            stmt = DdlStatement.CREATE_FOREIGN_FUNCTION;
            schemaElementType = SchemaElementType.FOREIGN;
            procedure = false;
        } else if (tokens.canConsume(DdlStatement.CREATE_FOREIGN_PROCEDURE.tokens())) {
            stmt = DdlStatement.CREATE_FOREIGN_PROCEDURE;
            schemaElementType = SchemaElementType.FOREIGN;
        } else if (tokens.canConsume(DdlStatement.CREATE_FUNCTION.tokens())) {
            stmt = DdlStatement.CREATE_FUNCTION;
            schemaElementType = SchemaElementType.FOREIGN;
            procedure = false;
        } else if (tokens.canConsume(DdlStatement.CREATE_PROCEDURE.tokens())) {
            stmt = DdlStatement.CREATE_PROCEDURE;
            schemaElementType = SchemaElementType.FOREIGN;
        } else {
            throw new TeiidDdlParsingException(tokens, "Unparsable create procedure statement");
        }

        assert (stmt != null) : "Create procedure statement is null";
        assert (schemaElementType != null) : "Create procedure schema element type is null";

        // parse identifier
        final String id = parseIdentifier(tokens);
        final AstNode procedureNode = getNodeFactory().node(id,
                                                            parentNode,
                                                            (procedure ? TeiidDdlLexicon.CreateProcedure.PROCEDURE_STATEMENT : TeiidDdlLexicon.CreateProcedure.FUNCTION_STATEMENT));
        procedureNode.setProperty(TeiidDdlLexicon.SchemaElement.TYPE, schemaElementType.toDdl());

        // must have parens after identifier and may have one or more parameters
        parseProcedureParameters(tokens, procedureNode);

        // may have a returns clause
        parseReturnsClause(tokens, procedureNode);

        // may have an option clause
        parseOptionsClause(tokens, procedureNode);

        // may have AS clause
        parseAsClause(tokens, procedureNode);

        return procedureNode;
    }

    boolean parseAsClause( final DdlTokenStream tokens,
                           final AstNode procedureNode ) {
        if (tokens.canConsume(TeiidReservedWord.AS.toDdl())) {
            final String statement = parseStatement(tokens, 0, "", tokens.nextPosition(), "");

            if (StringUtil.isBlank(statement)) {
                throw new TeiidDdlParsingException(tokens, "Unparsable AS clause (no statement found)");
            }

            procedureNode.setProperty(TeiidDdlLexicon.CreateProcedure.STATEMENT, statement);
            return true;
        }

        return false;
    }

    /**
     * <procedure parameter> <code>
     * ( IN | OUT | INOUT | VARIADIC )? <identifier> <data type> ( NOT NULL )? ( RESULT )? ( DEFAULT <string> )? ( <options clause> )?
     * <code>
     * 
     * @param tokens the tokens being processed (cannot be <code>null</code> or empty)
     * @param procedureNode the create procedure node owning this parameter (cannot be <code>null</code>)
     */
    void parseProcedureParameter( final DdlTokenStream tokens,
                                  final AstNode procedureNode ) {
        String paramType = TeiidReservedWord.IN.toDdl();

        if (tokens.matches(TeiidReservedWord.IN.toDdl()) || tokens.matches(TeiidReservedWord.OUT.toDdl())
            || tokens.matches(TeiidReservedWord.INOUT.toDdl()) || tokens.matches(TeiidNonReservedWord.VARIADIC.toDdl())) {
            paramType = tokens.consume();
        }

        final String id = parseIdentifier(tokens);
        final AstNode parameterNode = getNodeFactory().node(id, procedureNode, TeiidDdlLexicon.CreateProcedure.PARAMETER);
        parameterNode.setProperty(TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, paramType);

        // parse data type
        final DataType dataType = getDataTypeParser().parse(tokens);
        getDataTypeParser().setPropertiesOnNode(parameterNode, dataType);

        // parse any optional clauses
        boolean foundNotNull = false;
        boolean foundResult = false;
        boolean foundDefault = false;
        boolean foundOptions = false;
        boolean keepParsing = true;

        while (keepParsing && (!foundNotNull || !foundResult || !foundDefault || foundOptions)) {
            if (tokens.canConsume(NOT_NULL)) {
                foundNotNull = true;
            } else if (tokens.canConsume(TeiidNonReservedWord.RESULT.toDdl())) {
                foundResult = true;
            } else if (parseDefaultClause(tokens, parameterNode)) {
                foundDefault = true;
            } else if (parseOptionsClause(tokens, parameterNode)) {
                foundOptions = true;
            } else {
                keepParsing = false;
            }
        }

        parameterNode.setProperty(StandardDdlLexicon.NULLABLE, (foundNotNull ? "NOT NULL" : "NULL"));
        parameterNode.setProperty(TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, foundResult);
    }

    /**
     * <procedure parameters> <code>
     * <lparen> ( <procedure parameter> ( <comma> <procedure parameter> )* )? <rparen>
     * <code>
     * 
     * @param tokens the tokens being processed (cannot be <code>null</code> or empty)
     * @param procedureNode the create procedure node owning these parameters (cannot be <code>null</code>)
     */
    void parseProcedureParameters( final DdlTokenStream tokens,
                                   final AstNode procedureNode ) {
        if (tokens.canConsume(L_PAREN)) {
            // parse parameters if any exist
            if (!tokens.matches(R_PAREN)) {
                parseProcedureParameter(tokens, procedureNode);

                while (tokens.canConsume(COMMA)) {
                    parseProcedureParameter(tokens, procedureNode);
                }
            }

            // must have ending paren
            if (!tokens.canConsume(R_PAREN)) {
                throw new TeiidDdlParsingException(tokens, "Unparsable procedure parameters (right paren not found)");
            }
        } else {
            throw new TeiidDdlParsingException(tokens, "Unparsable procedure parameters (left paren not found)");
        }
    }

    /**
     * <procedure result column>
     * <p>
     * <code>
     * <identifier> <data type> ( NOT NULL )? ( <options clause> )?
     * <code>
     * 
     * @param tokens the tokens being processed (cannot be <code>null</code> or empty)
     * @param resultSetNode the result set node owning this result column (cannot be <code>null</code>)
     */
    void parseProcedureResultColumn( final DdlTokenStream tokens,
                                     final AstNode resultSetNode ) {
        final String id = parseIdentifier(tokens);
        final DataType dataType = getDataTypeParser().parse(tokens);
        final boolean notNull = tokens.canConsume(NOT_NULL);

        final AstNode resultColumnNode = getNodeFactory().node(id, resultSetNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMN);
        resultColumnNode.setProperty(StandardDdlLexicon.NULLABLE, (notNull ? "NOT NULL" : "NULL"));
        getDataTypeParser().setPropertiesOnNode(resultColumnNode, dataType);

        // may have an options clause
        parseOptionsClause(tokens, resultColumnNode);
    }

    /**
     * <procedure result columns> <code>
     * ( TABLE )? <lparen> <procedure result column> ( <comma> <procedure result column> )* <rparen> )
     * <code>
     * 
     * @param tokens the tokens being processed (cannot be <code>null</code> or empty)
     * @param procedureNode the create procedure node owning these result columns (cannot be <code>null</code>)
     * @return <code>true</code> if procedure results columns were successfully parsed
     * @throws ParsingException if there is a problem parsing the procedure result columns
     */
    boolean parseProcedureResultColumns( final DdlTokenStream tokens,
                                         final AstNode procedureNode ) throws ParsingException {
        if (tokens.matches(TABLE, L_PAREN) || tokens.matches(L_PAREN)) {
            boolean table = tokens.canConsume(TABLE);

            if (tokens.canConsume(L_PAREN)) {
                // create result columns node
                final AstNode resultSetNode = getNodeFactory().node(TeiidDdlLexicon.CreateProcedure.RESULT_SET,
                                                                    procedureNode,
                                                                    TeiidDdlLexicon.CreateProcedure.RESULT_COLUMNS);
                resultSetNode.setProperty(TeiidDdlLexicon.CreateProcedure.TABLE_FLAG, table);

                parseProcedureResultColumn(tokens, resultSetNode); // must have at least one

                while (tokens.canConsume(COMMA)) {
                    parseProcedureResultColumn(tokens, resultSetNode);
                }

                // must have ending paren
                if (!tokens.canConsume(R_PAREN)) {
                    throw new TeiidDdlParsingException(tokens, "Unparsable procedure result columns (right paren not found)");
                }

                return true;
            }

            throw new TeiidDdlParsingException(tokens, "Unparsable procedure result columns (left paren not found)");
        }

        return false;
    }

    /**
     * And optional clause of the create procedure statement.
     * <p>
     * <code>
     * ( RETURNS ( ( ( TABLE )? <lparen> <procedure result column> ( <comma> <procedure result column> )* <rparen> ) | <data type> ) )?
     * </code>
     * 
     * @param tokens the tokens being processed (cannot be <code>null</code>)
     * @param procedureNode the procedure node of of the returns clause (cannot be <code>null</code>)
     * @return <code>true</code> if the returns clause was successfully parsed
     */
    boolean parseReturnsClause( final DdlTokenStream tokens,
                                final AstNode procedureNode ) {
        if (tokens.canConsume(TeiidReservedWord.RETURNS.toDdl())) {
            // must have either one or more result columns or a data type
            if (!parseProcedureResultColumns(tokens, procedureNode)) {
                final DataType dataType = getDataTypeParser().parse(tokens);

                // create result node
                final AstNode resultNode = getNodeFactory().node(TeiidDdlLexicon.CreateProcedure.RESULT_SET,
                                                                 procedureNode,
                                                                 TeiidDdlLexicon.CreateProcedure.RESULT_DATA_TYPE);
                resultNode.setProperty(DATATYPE_NAME, dataType.getName());

                if (dataType.getLength() != DataType.DEFAULT_LENGTH) {
                    resultNode.setProperty(DATATYPE_LENGTH, dataType.getLength());
                }

                if (dataType.getPrecision() != DataType.DEFAULT_PRECISION) {
                    resultNode.setProperty(DATATYPE_PRECISION, dataType.getPrecision());
                }

                if (dataType.getScale() != DataType.DEFAULT_SCALE) {
                    resultNode.setProperty(DATATYPE_SCALE, dataType.getScale());
                }
            }

            return true;
        }

        return false;
    }

    private String parseStatement( final DdlTokenStream tokens,
                                   int numBegins,
                                   final String statement,
                                   Position prevPosition,
                                   String prevValue ) throws ParsingException {
        final StringBuilder text = new StringBuilder(statement);

        while (tokens.hasNext()) {
            final Position currPosition = tokens.nextPosition();
            final String value = tokens.consume();

            if (TeiidReservedWord.BEGIN.toDdl().equals(value)) {
                text.append(getWhitespace(currPosition, prevPosition, prevValue));
                text.append(TeiidReservedWord.BEGIN.toDdl());
                return parseStatement(tokens, ++numBegins, text.toString(), currPosition, value);
            }

            if (TeiidReservedWord.END.toDdl().equals(value)) {
                text.append(getWhitespace(currPosition, prevPosition, prevValue));
                text.append(TeiidReservedWord.END.toDdl());
                return parseStatement(tokens, --numBegins, text.toString(), currPosition, value);
            }

            if (SEMICOLON.equals(value)) {
                if (numBegins > 0) {
                    text.append(getWhitespace(currPosition, prevPosition, prevValue));
                    text.append(SEMICOLON);
                    return parseStatement(tokens, numBegins, text.toString(), currPosition, value);
                }

                text.append(SEMICOLON);
                break;
            }

            text.append(getWhitespace(currPosition, prevPosition, prevValue));
            text.append(value);
            prevValue = value;
            prevPosition = currPosition;
        }

        return text.toString();
    }
    
	@Override
	protected void postProcess(AstNode rootNode) {
		
	}
}
