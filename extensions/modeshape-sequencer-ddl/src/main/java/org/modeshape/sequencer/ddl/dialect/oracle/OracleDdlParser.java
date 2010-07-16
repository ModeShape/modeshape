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
package org.modeshape.sequencer.ddl.dialect.oracle;

import static org.modeshape.sequencer.ddl.StandardDdlLexicon.CREATE_VIEW_QUERY_EXPRESSION;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DROP_BEHAVIOR;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DROP_OPTION_TYPE;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.NEW_NAME;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_ALTER_TABLE_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_COLUMN_REFERENCE;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_CREATE_VIEW_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_COLUMN_DEFINITION;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_TABLE_CONSTRAINT_DEFINITION;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_TABLE_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_GRANT_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_UNKNOWN_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlLexicon.*;
import java.util.ArrayList;
import java.util.List;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.property.Name;
import org.modeshape.sequencer.ddl.DdlParserProblem;
import org.modeshape.sequencer.ddl.DdlSequencerI18n;
import org.modeshape.sequencer.ddl.DdlTokenStream;
import org.modeshape.sequencer.ddl.StandardDdlLexicon;
import org.modeshape.sequencer.ddl.StandardDdlParser;
import org.modeshape.sequencer.ddl.DdlTokenStream.DdlTokenizer;
import org.modeshape.sequencer.ddl.datatype.DataType;
import org.modeshape.sequencer.ddl.datatype.DataTypeParser;
import org.modeshape.sequencer.ddl.node.AstNode;

/**
 * Oracle-specific DDL Parser. Includes custom data types as well as custom DDL statements.
 */
public class OracleDdlParser extends StandardDdlParser
    implements OracleDdlConstants, OracleDdlConstants.OracleStatementStartPhrases {
    private final String parserId = "ORACLE";

    static List<String[]> oracleDataTypeStrings = new ArrayList<String[]>();

    public OracleDdlParser() {
        super();

        setDatatypeParser(new OracleDataTypeParser());
        initialize();
    }

    private void initialize() {
        setTerminator(DEFAULT_TERMINATOR);

        setDoUseTerminator(true);

        oracleDataTypeStrings.addAll(OracleDataTypes.CUSTOM_DATATYPE_START_PHRASES); //
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#getId()
     */
    @Override
    public String getId() {
        return this.parserId;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#getIdentifyingKeywords()
     */
    @Override
    public String[] getIdentifyingKeywords() {
        return new String[] {getId(), "spool.log"};
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#initializeTokenStream(org.modeshape.sequencer.ddl.DdlTokenStream)
     */
    @Override
    protected void initializeTokenStream( DdlTokenStream tokens ) {
        super.initializeTokenStream(tokens);
        tokens.registerKeyWords(CUSTOM_KEYWORDS);
        tokens.registerKeyWords(OracleDataTypes.CUSTOM_DATATYPE_START_WORDS);
        tokens.registerStatementStartPhrase(ALTER_PHRASES);
        tokens.registerStatementStartPhrase(CREATE_PHRASES);
        tokens.registerStatementStartPhrase(DROP_PHRASES);
        tokens.registerStatementStartPhrase(MISC_PHRASES);
        tokens.registerStatementStartPhrase(SET_PHRASES);
    }

    @Override
    protected void rewrite( DdlTokenStream tokens,
                            AstNode rootNode ) {
        assert tokens != null;
        assert rootNode != null;

        // We may have a prepare statement that is followed by a missing terminator node
        // We also may have nodes that have an extra terminator node representing the '/' backslash
        // These nodes will have type "TYPE_BACKSLASH_TERMINATOR".

        List<AstNode> copyOfNodes = new ArrayList<AstNode>(rootNode.getChildren());

        AstNode complexNode = null;

        for (AstNode child : copyOfNodes) {

            if ((complexNode != null && nodeFactory().hasMixinType(child, TYPE_UNKNOWN_STATEMENT))
                || (complexNode != null && nodeFactory().hasMixinType(child, TYPE_BACKSLASH_TERMINATOR))) {
                mergeNodes(tokens, complexNode, child);
                rootNode.removeChild(child);
            } else {
                complexNode = null;
            }

            if (nodeFactory().hasMixinType(child, TYPE_CREATE_FUNCTION_STATEMENT)
                || nodeFactory().hasMixinType(child, TYPE_CREATE_TRIGGER_STATEMENT)
                || nodeFactory().hasMixinType(child, TYPE_CREATE_LIBRARY_STATEMENT)
                || nodeFactory().hasMixinType(child, TYPE_CREATE_PACKAGE_STATEMENT)
                || nodeFactory().hasMixinType(child, TYPE_CREATE_PROCEDURE_STATEMENT)) {
                complexNode = child;
            }
        }

        // We also may have nodes that have an extra terminator node representing the '/' backslash
        // These nodes will have type "TYPE_BACKSLASH_TERMINATOR".

        super.rewrite(tokens, rootNode); // Removes all extra "missing terminator" nodes

        // Now we need to walk the tree again looking for unknown nodes under the root
        // and attach them to the previous node, assuming the node can contain multiple nested statements.
        // CREATE FUNCTION is one of those types

        copyOfNodes = new ArrayList<AstNode>(rootNode.getChildren());
        boolean foundComplexNode = false;
        complexNode = null;
        for (AstNode child : copyOfNodes) {
            if (matchesComplexNode(child)) {
                foundComplexNode = true;
                complexNode = child;
            } else if (foundComplexNode) {
                if (complexNode != null && nodeFactory().hasMixinType(child, TYPE_UNKNOWN_STATEMENT)) {
                    mergeNodes(tokens, complexNode, child);
                    rootNode.removeChild(child);
                } else {
                    foundComplexNode = false;
                    complexNode = null;
                }
            }
        }
    }

    private boolean matchesComplexNode( AstNode node ) {
        assert node != null;

        for (Name mixin : COMPLEX_STMT_TYPES) {
            if (nodeFactory().hasMixinType(node, mixin)) {
                return true;
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#handleUnknownToken(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      java.lang.String)
     */
    @Override
    public AstNode handleUnknownToken( DdlTokenStream tokens,
                                       String tokenValue ) throws ParsingException {
        if (tokenValue.equals("/")) {
            return nodeFactory().node("backslashTerminator", getRootNode(), TYPE_BACKSLASH_TERMINATOR);
        }

        return null;
    }

    /**
     * {@inheritDoc} The CREATE SCHEMA statement can include CREATE TABLE, CREATE VIEW, and GRANT statements.
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseCreateSchemaStatement(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseCreateSchemaStatement( DdlTokenStream tokens,
                                                  AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        return super.parseCreateSchemaStatement(tokens, parentNode);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseCustomStatement(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseCustomStatement( DdlTokenStream tokens,
                                            AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        AstNode result = null;

        if (tokens.matches(STMT_COMMENT_ON)) {
            result = parseCommentStatement(tokens, parentNode);
        } else if (tokens.matches(STMT_ANALYZE)) {
            return parseStatement(tokens, STMT_ANALYZE, parentNode, TYPE_ANALYZE_STATEMENT);
        } else if (tokens.matches(STMT_ASSOCIATE_STATISTICS)) {
            return parseStatement(tokens, STMT_ASSOCIATE_STATISTICS, parentNode, TYPE_ASSOCIATE_STATISTICS_STATEMENT);
        } else if (tokens.matches(STMT_AUDIT)) {
            return parseStatement(tokens, STMT_AUDIT, parentNode, TYPE_AUDIT_STATEMENT);
        } else if (tokens.matches(STMT_COMMIT_FORCE) || tokens.matches(STMT_COMMIT_WORK) || tokens.matches(STMT_COMMIT_WRITE)) {
            return parseStatement(tokens, STMT_COMMIT, parentNode, TYPE_COMMIT_STATEMENT);
        } else if (tokens.matches(STMT_DISASSOCIATE_STATISTICS)) {
            return parseStatement(tokens, STMT_DISASSOCIATE_STATISTICS, parentNode, TYPE_DISASSOCIATE_STATISTICS_STATEMENT);
        } else if (tokens.matches(STMT_EXPLAIN_PLAN)) {
            return parseStatement(tokens, STMT_EXPLAIN_PLAN, parentNode, TYPE_EXPLAIN_PLAN_STATEMENT);
        } else if (tokens.matches(STMT_FLASHBACK)) {
            return parseStatement(tokens, STMT_FLASHBACK, parentNode, TYPE_FLASHBACK_STATEMENT);
        } else if (tokens.matches(STMT_LOCK_TABLE)) {
            return parseStatement(tokens, STMT_LOCK_TABLE, parentNode, TYPE_LOCK_TABLE_STATEMENT);
        } else if (tokens.matches(STMT_MERGE)) {
            return parseStatement(tokens, STMT_MERGE, parentNode, TYPE_MERGE_STATEMENT);
        } else if (tokens.matches(STMT_NOAUDIT)) {
            return parseStatement(tokens, STMT_NOAUDIT, parentNode, TYPE_NOAUDIT_STATEMENT);
        } else if (tokens.matches(STMT_PURGE)) {
            return parseStatement(tokens, STMT_PURGE, parentNode, TYPE_PURGE_STATEMENT);
        } else if (tokens.matches(STMT_RENAME)) {
            return parseStatement(tokens, STMT_RENAME, parentNode, TYPE_RENAME_STATEMENT);
        } else if (tokens.matches(STMT_ROLLBACK)) {
            return parseStatement(tokens, STMT_ROLLBACK, parentNode, TYPE_ROLLBACK_STATEMENT);
        } else if (tokens.matches(STMT_ROLLBACK_WORK)) {
            return parseStatement(tokens, STMT_ROLLBACK_WORK, parentNode, TYPE_ROLLBACK_STATEMENT);
        } else if (tokens.matches(STMT_ROLLBACK_TO_SAVEPOINT)) {
            return parseStatement(tokens, STMT_ROLLBACK_TO_SAVEPOINT, parentNode, TYPE_ROLLBACK_STATEMENT);
        } else if (tokens.matches(STMT_SAVEPOINT)) {
            return parseStatement(tokens, STMT_SAVEPOINT, parentNode, TYPE_SAVEPOINT_STATEMENT);
        } else if (tokens.matches(STMT_TRUNCATE)) {
            return parseStatement(tokens, STMT_TRUNCATE, parentNode, TYPE_TRUNCATE_STATEMENT);
        }

        if (result == null) {
            result = super.parseCustomStatement(tokens, parentNode);
        }
        return result;
    }

    @Override
    protected AstNode parseCreateStatement( DdlTokenStream tokens,
                                            AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        if (tokens.matches(STMT_CREATE_INDEX) || tokens.matches(STMT_CREATE_UNIQUE_INDEX)
            || tokens.matches(STMT_CREATE_BITMAP_INDEX)) {
            return parseCreateIndex(tokens, parentNode);
        } else if (tokens.matches(STMT_CREATE_CLUSTER)) {
            return parseCreateClusterStatement(tokens, parentNode);
        } else if (tokens.matches(STMT_CREATE_CONTEXT)) {
            return parseStatement(tokens, STMT_CREATE_CONTEXT, parentNode, TYPE_CREATE_CONTEXT_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_CONTROLFILE)) {
            return parseStatement(tokens, STMT_CREATE_CONTROLFILE, parentNode, TYPE_CREATE_CONTROLFILE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_DATABASE)) {
            return parseStatement(tokens, STMT_CREATE_DATABASE, parentNode, TYPE_CREATE_DATABASE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_PUBLIC_DATABASE)) {
            return parseStatement(tokens, STMT_CREATE_PUBLIC_DATABASE, parentNode, TYPE_CREATE_DATABASE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_DIMENSION)) {
            return parseCreateDimensionStatement(tokens, parentNode);
        } else if (tokens.matches(STMT_CREATE_DIRECTORY)) {
            return parseStatement(tokens, STMT_CREATE_DIRECTORY, parentNode, TYPE_CREATE_DIRECTORY_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_OR_REPLACE_DIRECTORY)) {
            return parseStatement(tokens, STMT_CREATE_OR_REPLACE_DIRECTORY, parentNode, TYPE_CREATE_DIRECTORY_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_DISKGROUP)) {
            return parseStatement(tokens, STMT_CREATE_DISKGROUP, parentNode, TYPE_CREATE_DISKGROUP_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_FUNCTION)) {
            // ============ > PARSE UNTIL '/' for STMT_CREATE_FUNCTION
            return parseCreateFunctionStatement(tokens, parentNode);
        } else if (tokens.matches(STMT_CREATE_OR_REPLACE_FUNCTION)) {
            // ============ > PARSE UNTIL '/' for STMT_CREATE_OR_REPLACE_FUNCTION
            return parseCreateFunctionStatement(tokens, parentNode);
        } else if (tokens.matches(STMT_CREATE_INDEXTYPE)) {
            return parseStatement(tokens, STMT_CREATE_INDEXTYPE, parentNode, TYPE_CREATE_INDEXTYPE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_JAVA)) {
            return parseCreateJavaStatement(tokens, parentNode);
        } else if (tokens.matches(STMT_CREATE_LIBRARY)) {
            // ============ > PARSE UNTIL '/' for STMT_CREATE_LIBRARY
            return parseStatement(tokens, STMT_CREATE_LIBRARY, parentNode, TYPE_CREATE_LIBRARY_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_OR_REPLACE_LIBRARY)) {
            // ============ > PARSE UNTIL '/' STMT_CREATE_OR_REPLACE_LIBRARY
            return parseStatement(tokens, STMT_CREATE_OR_REPLACE_LIBRARY, parentNode, TYPE_CREATE_LIBRARY_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_MATERIALIZED_VIEW)) {
            return parseMaterializedViewStatement(tokens, parentNode);
        } else if (tokens.matches(STMT_CREATE_OPERATOR)) {
            return parseStatement(tokens, STMT_CREATE_OPERATOR, parentNode, TYPE_CREATE_OPERATOR_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_OUTLINE)) {
            return parseStatement(tokens, STMT_CREATE_OUTLINE, parentNode, TYPE_CREATE_OUTLINE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_OR_REPLACE_OUTLINE)) {
            return parseStatement(tokens, STMT_CREATE_OR_REPLACE_OUTLINE, parentNode, TYPE_CREATE_OUTLINE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_PACKAGE)) {
            // ============ > PARSE UNTIL '/' for STMT_CREATE_PACKAGE
            return parseStatement(tokens, STMT_CREATE_PACKAGE, parentNode, TYPE_CREATE_PACKAGE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_OR_REPLACE_PACKAGE)) {
            // ============ > PARSE UNTIL '/' for STMT_CREATE_OR_REPLACE_PACKAGE
            return parseStatement(tokens, STMT_CREATE_OR_REPLACE_PACKAGE, parentNode, TYPE_CREATE_PACKAGE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_PFILE)) {
            return parseStatement(tokens, STMT_CREATE_PFILE, parentNode, TYPE_CREATE_PFILE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_PROCEDURE)) {
            // ============ > PARSE UNTIL '/' for STMT_CREATE_PROCEDURE
            return parseCreateProcedureStatement(tokens, parentNode);
        } else if (tokens.matches(STMT_CREATE_OR_REPLACE_PROCEDURE)) {
            // ============ > PARSE UNTIL '/' for STMT_CREATE_PROCEDURE
            return parseCreateProcedureStatement(tokens, parentNode);
        } else if (tokens.matches(STMT_CREATE_PROFILE)) {
            return parseStatement(tokens, STMT_CREATE_PROFILE, parentNode, TYPE_CREATE_PROFILE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_ROLE)) {
            return parseStatement(tokens, STMT_CREATE_ROLE, parentNode, TYPE_CREATE_ROLE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_ROLLBACK)) {
            return parseStatement(tokens, STMT_CREATE_ROLLBACK, parentNode, TYPE_CREATE_ROLLBACK_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_PUBLIC_ROLLBACK)) {
            return parseStatement(tokens, STMT_CREATE_PUBLIC_ROLLBACK, parentNode, TYPE_CREATE_ROLLBACK_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_SEQUENCE)) {
            return parseStatement(tokens, STMT_CREATE_SEQUENCE, parentNode, TYPE_CREATE_SEQUENCE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_SPFILE)) {
            return parseStatement(tokens, STMT_CREATE_SPFILE, parentNode, TYPE_CREATE_SPFILE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_SYNONYM)) {
            return parseStatement(tokens, STMT_CREATE_SYNONYM, parentNode, TYPE_CREATE_SYNONYM_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_OR_REPLACE_SYNONYM)) {
            return parseStatement(tokens, STMT_CREATE_OR_REPLACE_SYNONYM, parentNode, TYPE_CREATE_SYNONYM_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_OR_REPLACE_PUBLIC_SYNONYM)) {
            return parseStatement(tokens, STMT_CREATE_OR_REPLACE_PUBLIC_SYNONYM, parentNode, TYPE_CREATE_SYNONYM_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_PUBLIC_SYNONYM)) {
            return parseStatement(tokens, STMT_CREATE_PUBLIC_SYNONYM, parentNode, TYPE_CREATE_SYNONYM_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_TABLESPACE)) {
            return parseStatement(tokens, STMT_CREATE_TABLESPACE, parentNode, TYPE_CREATE_TABLESPACE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_TRIGGER)) {
            // ============ > PARSE UNTIL '/' for STMT_CREATE_OR_REPLACE_TRIGGER
            return parseSlashedStatement(tokens, STMT_CREATE_TRIGGER, parentNode, TYPE_CREATE_TRIGGER_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_OR_REPLACE_TRIGGER)) {
            // ============ > PARSE UNTIL '/' for STMT_CREATE_OR_REPLACE_TRIGGER
            return parseSlashedStatement(tokens, STMT_CREATE_OR_REPLACE_TRIGGER, parentNode, TYPE_CREATE_TRIGGER_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_TYPE)) {
            return parseStatement(tokens, STMT_CREATE_TYPE, parentNode, TYPE_CREATE_TYPE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_OR_REPLACE_TYPE)) {
            return parseStatement(tokens, STMT_CREATE_OR_REPLACE_TYPE, parentNode, TYPE_CREATE_TYPE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_USER)) {
            return parseStatement(tokens, STMT_CREATE_USER, parentNode, TYPE_CREATE_USER_STATEMENT);
        }

        return super.parseCreateStatement(tokens, parentNode);
    }

    private AstNode parseCreateClusterStatement( DdlTokenStream tokens,
                                                 AstNode parentNode ) throws ParsingException {
        markStartOfStatement(tokens);
        tokens.consume(STMT_CREATE_CLUSTER);
        String name = parseName(tokens);

        AstNode node = nodeFactory().node(name, parentNode, TYPE_CREATE_CLUSTER_STATEMENT);

        parseUntilTerminator(tokens);

        markEndOfStatement(tokens, node);

        return node;
    }

    private AstNode parseCreateDimensionStatement( DdlTokenStream tokens,
                                                   AstNode parentNode ) throws ParsingException {
        markStartOfStatement(tokens);
        tokens.consume(STMT_CREATE_DIMENSION);
        String name = parseName(tokens);

        AstNode node = nodeFactory().node(name, parentNode, TYPE_CREATE_DIMENSION_STATEMENT);

        parseUntilTerminator(tokens);

        markEndOfStatement(tokens, node);

        return node;
    }

    /**
     * Parses DDL CREATE FUNCTION statement
     * 
     * @param tokens the tokenized {@link DdlTokenStream} of the DDL input content; may not be null
     * @param parentNode the parent {@link AstNode} node; may not be null
     * @return the parsed CREATE FUNCTION statement node
     * @throws ParsingException
     */
    protected AstNode parseCreateFunctionStatement( DdlTokenStream tokens,
                                                    AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        markStartOfStatement(tokens);

        /* ----------------------------------------------------------------------
            CREATE [ OR REPLACE ] FUNCTION [ schema. ] function_name
              [ ( parameter_declaration [, parameter_declaration] ) 
              ]
              RETURN datatype
              [ { invoker_rights_clause
                | DETERMINISTIC
                | parallel_enable_clause
                | result_cache_clause
                }...
              ]
              { { AGGREGATE | PIPELINED }
                USING [ schema. ] implementation_type
              | [ PIPELINED ] { IS | AS } { [ declare_section ] body | call_spec }
              } ;
            
            parameter_declaration = parameter_name [ IN | { { OUT | { IN OUT }} [ NOCOPY ] } ] datatype [ { := | DEFAULT } expression ]
        ---------------------------------------------------------------------- */

        boolean isReplace = tokens.canConsume(STMT_CREATE_OR_REPLACE_FUNCTION);

        tokens.canConsume(STMT_CREATE_FUNCTION);

        String name = parseName(tokens);

        AstNode node = nodeFactory().node(name, parentNode, TYPE_CREATE_FUNCTION_STATEMENT);

        if (isReplace) {
            // TODO: SET isReplace = TRUE to node (possibly a cnd mixin of "replaceable"
        }

        boolean ok = parseParameters(tokens, node);

        if (ok) {
            if (tokens.canConsume("RETURN")) {
                DataType dType = getDatatypeParser().parse(tokens);
                if (dType != null) {
                    getDatatypeParser().setPropertiesOnNode(node, dType);
                }
            }
        }

        parseUntilFwdSlash(tokens, false);

        tokens.canConsume("/");

        markEndOfStatement(tokens, node);

        return node;
    }

    /**
     * Parses DDL CREATE PROCEDURE statement
     * 
     * @param tokens the tokenized {@link DdlTokenStream} of the DDL input content; may not be null
     * @param parentNode the parent {@link AstNode} node; may not be null
     * @return the parsed CREATE PROCEDURE statement node
     * @throws ParsingException
     */
    protected AstNode parseCreateProcedureStatement( DdlTokenStream tokens,
                                                     AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        markStartOfStatement(tokens);
        /* ----------------------------------------------------------------------
            CREATE [ OR REPLACE ] PROCEDURE [ schema. ] procedure_name
                [ ( parameter_declaration [, parameter_declaration ] ) ]
                [ AUTHID { CURRENT_USER | DEFINER  ]
                { IS | AS }
                { [ declare_section ] body | call_spec | EXTERNAL} ;
            
             call_spec = LANGUAGE { Java_declaration | C_declaration }
            
             Java_declaration = JAVA NAME string
            
             C_declaration = 
                C [ NAME name ]
                    LIBRARY lib_name
                    [ AGENT IN (argument[, argument ]...) ]
                    [ WITH CONTEXT ]
                    [ PARAMETERS (parameter[, parameter ]...) ]
                    
            parameter_declaration = parameter_name [ IN | { { OUT | { IN OUT }} [ NOCOPY ] } ] datatype [ { := | DEFAULT } expression ]
        ---------------------------------------------------------------------- */

        boolean isReplace = tokens.canConsume(STMT_CREATE_OR_REPLACE_PROCEDURE);

        tokens.canConsume(STMT_CREATE_PROCEDURE);

        String name = parseName(tokens);

        AstNode node = nodeFactory().node(name, parentNode, TYPE_CREATE_PROCEDURE_STATEMENT);

        if (isReplace) {
            // TODO: SET isReplace = TRUE to node (possibly a cnd mixin of "replaceable"
        }

        boolean ok = parseParameters(tokens, node);

        if (ok) {
            if (tokens.canConsume("AUTHID")) {
                if (tokens.canConsume("CURRENT_USER")) {
                    node.setProperty(AUTHID_VALUE, "AUTHID CURRENT_USER");
                } else {
                    tokens.consume("DEFINER");
                    node.setProperty(AUTHID_VALUE, "DEFINER");
                }
            }
        }

        parseUntilFwdSlash(tokens, false);

        tokens.canConsume("/");

        markEndOfStatement(tokens, node);

        return node;
    }

    private boolean parseParameters( DdlTokenStream tokens,
                                     AstNode procedureNode ) throws ParsingException {
        assert tokens != null;
        assert procedureNode != null;

        // parameter_declaration = parameter_name [ IN | { { OUT | { IN OUT }} [ NOCOPY ] } ] datatype [ { := | DEFAULT }
        // expression ]
        // Assume we start with open parenthesis '(', then we parse comma separated list of function parameters
        // which have the form: [ parameter-Name ] DataType
        // So, try getting datatype, if datatype == NULL, then parseName() & parse datatype, then repeat as long as next token is
        // ","

        tokens.consume(L_PAREN); // EXPECTED

        while (!tokens.canConsume(R_PAREN)) {

            String paramName = parseName(tokens);
            String inOutStr = null;
            if (tokens.matches("IN")) {
                if (tokens.canConsume("IN", "OUT")) {
                    if (tokens.canConsume("NOCOPY")) {
                        inOutStr = "IN OUT NOCOPY";
                    } else {
                        inOutStr = "IN OUT";
                    }
                } else {
                    tokens.consume("IN");
                    inOutStr = "IN";
                }

            } else if (tokens.matches("OUT")) {
                if (tokens.canConsume("OUT", "NOCOPY")) {
                    inOutStr = "OUT NOCOPY";
                } else {
                    tokens.consume("OUT");
                    inOutStr = "OUT";
                }
            }

            DataType datatype = getDatatypeParser().parse(tokens);
            AstNode paramNode = nodeFactory().node(paramName, procedureNode, TYPE_FUNCTION_PARAMETER);
            if (datatype != null) {
                getDatatypeParser().setPropertiesOnNode(paramNode, datatype);
            }

            if (tokens.matchesAnyOf(":=", "DEFAULT") || !tokens.matchesAnyOf(COMMA, R_PAREN)) {
                String msg = DdlSequencerI18n.unsupportedProcedureParameterDeclaration.text(procedureNode.getProperty(StandardDdlLexicon.NAME));
                DdlParserProblem problem = new DdlParserProblem(Problems.WARNING, getCurrentMarkedPosition(), msg);
                addProblem(problem, procedureNode);
                return false;
            }

            if (inOutStr != null) {
                paramNode.setProperty(IN_OUT_NO_COPY, inOutStr);
            }

            tokens.canConsume(COMMA);
        }

        return true;
    }

    /**
     * Parses DDL CREATE MATERIALIZED VIEW statement This could either be a standard view or a VIEW LOG ON statement.
     * 
     * @param tokens the tokenized {@link DdlTokenStream} of the DDL input content; may not be null
     * @param parentNode the parent {@link AstNode} node; may not be null
     * @return the parsed CREATE MATERIALIZED VIEW statement node
     * @throws ParsingException
     */
    protected AstNode parseMaterializedViewStatement( DdlTokenStream tokens,
                                                      AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        markStartOfStatement(tokens);

        /* ----------------------------------------------------------------------
            CREATE MATERIALIZED VIEW
              [ schema. ]materialized_view
              [ column_alias [, column_alias]... ]
              [ OF [ schema. ]object_type ] .................... (MORE...)

            EXAMPLES:
            
                CREATE MATERIALIZED VIEW LOG ON products
                   WITH ROWID, SEQUENCE (prod_id)
                   INCLUDING NEW VALUES;
                
                CREATE MATERIALIZED VIEW sales_mv
                   BUILD IMMEDIATE
                   REFRESH FAST ON COMMIT
                   AS SELECT t.calendar_year, p.prod_id, 
                      SUM(s.amount_sold) AS sum_sales
                      FROM times t, products p, sales s
                      WHERE t.time_id = s.time_id AND p.prod_id = s.prod_id
                      GROUP BY t.calendar_year, p.prod_id;
        ---------------------------------------------------------------------- */

        boolean isLog = tokens.canConsume(STMT_CREATE_MATERIALIZED_VEIW_LOG);

        tokens.canConsume(STMT_CREATE_MATERIALIZED_VIEW);

        String name = parseName(tokens);

        AstNode node = null;

        if (isLog) {
            node = nodeFactory().node(name, parentNode, TYPE_CREATE_MATERIALIZED_VIEW_LOG_STATEMENT);
        } else {
            node = nodeFactory().node(name, parentNode, TYPE_CREATE_MATERIALIZED_VIEW_STATEMENT);
        }

        parseUntilTerminator(tokens);

        markEndOfStatement(tokens, node);

        return node;
    }

    @Override
    protected AstNode parseGrantStatement( DdlTokenStream tokens,
                                           AstNode parentNode ) throws ParsingException {
        CheckArg.isNotNull(tokens, "tokens");
        CheckArg.isNotNull(parentNode, "parentNode");

        // GRANT { grant_system_privileges | grant_object_privileges } ;
        //
        // ** grant_system_privileges **
        //        
        // { system_privilege | role | ALL PRIVILEGES } [, { system_privilege | role | ALL PRIVILEGES } ]...
        // TO grantee_clause [ WITH ADMIN OPTION ]
        //
        // ** grant_object_privileges **
        //
        // { object_privilege | ALL [ PRIVILEGES ] } [ (column [, column ]...) ] [, { object_privilege | ALL [ PRIVILEGES ] } [
        // (column [, column ]...) ] ]...
        // on_object_clause
        // TO grantee_clause [ WITH HIERARCHY OPTION ] [ WITH GRANT OPTION ]

        // ** on_object_clause **
        //
        // { [ schema. ] object | { DIRECTORY directory_name | JAVA { SOURCE | RESOURCE } [ schema. ] object } }
        //
        // ** grantee_clause **
        //
        // { user [ IDENTIFIED BY password ] | role | PUBLIC } [, { user [ IDENTIFIED BY password ] | role | PUBLIC } ]...

        AstNode node = null;

        // Original implementation does NOT parse Insert statement, but just returns a generic TypedStatement
        markStartOfStatement(tokens);

        tokens.consume(GRANT);
        String name = GRANT;

        tokens.consume(); // First Privilege token

        node = nodeFactory().node(name, parentNode, TYPE_GRANT_STATEMENT);

        while (tokens.hasNext()
               && !isTerminator(tokens)
               && (!tokens.matches(DdlTokenizer.STATEMENT_KEY) || (tokens.matches(DdlTokenizer.STATEMENT_KEY) && tokens.matches("GRANT",
                                                                                                                                "OPTION")))) {
            tokens.consume();
        }

        markEndOfStatement(tokens, node);

        return node;

        // if( tokens.matches(GRANT, DdlTokenStream.ANY_VALUE, "TO")) {
        // markStartOfStatement(tokens);
        // tokens.consume(GRANT);
        // String privilege = tokens.consume();
        // tokens.consume("TO");
        // tokens.consume(); // TO Value
        // && problem != null
        // String value = parseUntilTerminator(tokens);
        //			
        // AstNode grantNode = nodeFactory().node("GRANT", parentNode, TYPE_GRANT_STATEMENT);
        // markEndOfStatement(tokens, grantNode);
        //			
        // return grantNode;
        // } else if( tokens.matches(GRANT, DdlTokenStream.ANY_VALUE, COMMA)) {
        // markStartOfStatement(tokens);
        // tokens.consume(GRANT);
        // String privilege = tokens.consume();
        // // Assume Multiple Privileges
        // while( tokens.canConsume(COMMA)) {
        // tokens.consume(); // Next privilege
        // }
        //			
        // tokens.consume("ON");
        // tokens.consume(); // TO Value
        //			
        // tokens.canConsume("WITH", GRANT);
        //			
        // String value = parseUntilTerminator(tokens);
        //			
        // AstNode grantNode = nodeFactory().node("GRANT", parentNode, TYPE_GRANT_STATEMENT);
        // markEndOfStatement(tokens, grantNode);
        //			
        // return grantNode;
        // } else if( tokens.matches(GRANT, DdlTokenStream.ANY_VALUE, "ON", DdlTokenStream.ANY_VALUE, "TO",
        // DdlTokenStream.ANY_VALUE, "WITH", GRANT)) {
        // markStartOfStatement(tokens);
        // //GRANT ALL ON bonuses TO hr WITH GRANT OPTION;
        //			
        // tokens.consume(GRANT);
        // String privilege = tokens.consume();
        // tokens.consume("ON");
        // tokens.consume(); // ON Value
        // tokens.consume("TO");
        // tokens.consume();
        // tokens.consume("WITH", GRANT);
        // String value = parseUntilTerminator(tokens);
        //
        // AstNode grantNode = nodeFactory().node("GRANT", parentNode, TYPE_GRANT_STATEMENT);
        // markEndOfStatement(tokens, grantNode);
        //			
        // return grantNode;
        // }
        // else if( tokens.matches(GRANT, DdlTokenStream.ANY_VALUE, "ON")) {
        // tokens.consume(GRANT);
        // String privilege = tokens.consume();
        // tokens.consume("ON");
        // tokens.consume(); // ON Value
        //			
        // String value = parseUntilTerminator(tokens);
        // stmt.appendSource(true, value);&& problem != null
        // stmt.setType("GRANT" + SPACE + privilege + SPACE + "ON");
        // consumeTerminator(tokens);
        // } else if( tokens.matches(GRANT, CREATE) ||
        // tokens.matches(GRANT, ALTER) ||
        // tokens.matches(GRANT, DROP) ||
        // tokens.matches(GRANT, "EXECUTE") ||
        // tokens.matches(GRANT, "MANAGE") ||
        // tokens.matches(GRANT, "QUERY") ||
        // tokens.matches(GRANT, "ON", "COMMIT") ||
        // tokens.matches(GRANT, "ANY") ||
        // tokens.matches(GRANT, "SELECT") ||
        // tokens.matches(GRANT, "RESTRICTED") ||
        // //
        // ========================================================================================================================
        // ===
        // //
        // ========================================================================================================================
        // ===
        // tokens.matches(GRANT, "FLASHBACK") ||
        // tokens.matches(GRANT, "GLOBAL") ||
        // tokens.matches(GRANT, "DEBUG") ||
        // tokens.matches(GRANT, "GLOBAL") ||
        // tokens.matches(GRANT, "ADVISOR") ||
        // tokens.matches(GRANT, "ADMINISTER") ||
        // tokens.matches(GRANT, "BACKUP") ||
        // tokens.matches(GRANT, "LOCK") ||
        // tokens.matches(GRANT, "UPDATE") ||
        // tokens.matches(GRANT, "DELETE") ||
        // tokens.matches(GRANT, "INSERT") ||
        // tokens.matches(GRANT, "UNLIMITED") ||
        // tokens.matches(GRANT, "UNDER") ||
        // tokens.matches(GRANT, "ANALYZE") ||
        // tokens.matches(GRANT, "AUDIT") ||
        // tokens.matches(GRANT, "COMMENT") ||
        // tokens.matches(GRANT, "EXEMPT") ||
        // tokens.matches(GRANT, "FORCE") ||
        // tokens.matches(GRANT, "RESUMABLE") ||
        // tokens.matches(GRANT, "SYSDBA") ||
        // tokens.matches(GRANT, "REFERENCES") ||
        // tokens.matches(GRANT, "SYSOPER") ||
        // tokens.matches(GRANT, "WRITE") ) {
        // tokens.consume(GRANT);
        //			
        // String nextTok = tokens.consume() + SPACE + tokens.consume() + SPACE + tokens.consume();
        //
        // String value = parseUntilTerminator(tokens);
        // stmt.setType("GRANT" + SPACE + nextTok);
        // consumeTerminator(tokens);
        // }
        //		
        //		
        // return grantNode;

        // return super.parseGrantStatement(tokens, parentNode);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseRevokeStatement(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseRevokeStatement( DdlTokenStream tokens,
                                            AstNode parentNode ) throws ParsingException {
        return parseStatement(tokens, STMT_REVOKE, parentNode, TYPE_REVOKE_STATEMENT);
    }

    @Override
    protected AstNode parseAlterTableStatement( DdlTokenStream tokens,
                                                AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        if (tokens.matches("ALTER", "TABLE", DdlTokenStream.ANY_VALUE, "ADD")) {

            // ALTER TABLE
            // ADD ( {column_definition | virtual_column_definition
            // [, column_definition | virtual_column_definition] ... } [ column_properties ]

            markStartOfStatement(tokens);

            tokens.consume(ALTER, TABLE);

            String tableName = parseName(tokens);

            AstNode alterTableNode = nodeFactory().node(tableName, parentNode, TYPE_ALTER_TABLE_STATEMENT);

            tokens.consume("ADD");

            // System.out.println("  >> PARSING ALTER STATEMENT >>  TABLE Name = " + tableName);

            if (isTableConstraint(tokens)) {
                parseTableConstraint(tokens, alterTableNode, true);
            } else {
                // This segment can also be enclosed in "()" brackets to handle multiple ColumnDefinition ADDs
                if (tokens.matches(L_PAREN, "REF")) {
                    // ALTER TABLE staff ADD (REF(dept) WITH ROWID);
                    tokens.consume(L_PAREN, "REF", L_PAREN);
                    parseName(tokens);
                    tokens.consume(R_PAREN, "WITH", "ROWID", R_PAREN);

                } else if (tokens.matches(L_PAREN, "SCOPE")) {
                    // ALTER TABLE staff ADD (SCOPE FOR (dept) IS offices);
                    tokens.consume(L_PAREN, "SCOPE", "FOR", L_PAREN);
                    parseName(tokens);
                    tokens.consume(R_PAREN, "IS");
                    parseName(tokens);
                    tokens.consume(R_PAREN);
                } else if (tokens.matches(L_PAREN)) {
                    parseColumns(tokens, alterTableNode, true);
                } else {
                    // Assume single ADD COLUMN
                    parseSingleTerminatedColumnDefinition(tokens, alterTableNode, true);
                }
            }

            parseUntilTerminator(tokens); // COULD BE "NESTED TABLE xxxxxxxx" option clause

            markEndOfStatement(tokens, alterTableNode);

            return alterTableNode;
        } else if (tokens.matches("ALTER", "TABLE", DdlTokenStream.ANY_VALUE, "DROP")) {
            markStartOfStatement(tokens);

            tokens.consume(ALTER, TABLE);

            String tableName = parseName(tokens);

            AstNode alterTableNode = nodeFactory().node(tableName, parentNode, TYPE_ALTER_TABLE_STATEMENT);

            tokens.consume(DROP);

            if (tokens.canConsume("CONSTRAINT")) {
                String constraintName = parseName(tokens); // constraint name

                AstNode constraintNode = nodeFactory().node(constraintName, alterTableNode, TYPE_DROP_TABLE_CONSTRAINT_DEFINITION);

                if (tokens.canConsume(DropBehavior.CASCADE)) {
                    constraintNode.setProperty(DROP_BEHAVIOR, DropBehavior.CASCADE);
                } else if (tokens.canConsume(DropBehavior.RESTRICT)) {
                    constraintNode.setProperty(DROP_BEHAVIOR, DropBehavior.RESTRICT);
                }
            } else if (tokens.canConsume("COLUMN")) {
                // ALTER TABLE supplier
                // DROP COLUMN supplier_name;

                String columnName = parseName(tokens);

                AstNode columnNode = nodeFactory().node(columnName, alterTableNode, TYPE_DROP_COLUMN_DEFINITION);

                if (tokens.canConsume(DropBehavior.CASCADE)) {
                    columnNode.setProperty(DROP_BEHAVIOR, DropBehavior.CASCADE);
                } else if (tokens.canConsume(DropBehavior.RESTRICT)) {
                    columnNode.setProperty(DROP_BEHAVIOR, DropBehavior.RESTRICT);
                }
            } else {
                parseUntilTerminator(tokens); // EXAMPLE: "DROP UNIQUE (email)", or "DROP (col_1, col_2)"
            }

            markEndOfStatement(tokens, alterTableNode);

            return alterTableNode;
        } else if (tokens.matches("ALTER", "TABLE", DdlTokenStream.ANY_VALUE, "RENAME")) {

            // ALTER TABLE customers RENAME TO my_customers;
            // ALTER TABLE customers RENAME CONSTRAINT cust_fname_nn TO cust_firstname_nn;
            markStartOfStatement(tokens);

            tokens.consume(ALTER, TABLE);

            String oldName = parseName(tokens);
            AstNode alterTableNode = nodeFactory().node(oldName, parentNode, TYPE_ALTER_TABLE_STATEMENT);

            if (tokens.canConsume("RENAME", "TO")) {
                String newName = parseName(tokens);
                alterTableNode.setProperty(NEW_NAME, newName);

                parseUntilTerminator(tokens);

            } else if (tokens.canConsume("RENAME", "COLUMN")) {
                String oldColumnName = parseName(tokens);
                tokens.consume("TO");
                String newColumnName = parseName(tokens);

                parseUntilTerminator(tokens);

                AstNode renameColumnNode = nodeFactory().node(oldColumnName, alterTableNode, TYPE_RENAME_COLUMN);
                renameColumnNode.setProperty(NEW_NAME, newColumnName);

            } else if (tokens.canConsume("RENAME", "CONSTRAINT")) {
                String oldConstraintName = parseName(tokens);
                tokens.consume("TO");
                String newConstraintName = parseName(tokens);

                parseUntilTerminator(tokens);

                AstNode renameColumnNode = nodeFactory().node(oldConstraintName, alterTableNode, TYPE_RENAME_CONSTRAINT);
                renameColumnNode.setProperty(NEW_NAME, newConstraintName);
            }

            markEndOfStatement(tokens, alterTableNode);

            return alterTableNode;
        } else if (tokens.matches("ALTER", "TABLE", DdlTokenStream.ANY_VALUE, "MODIFY")) {

        }

        return super.parseAlterTableStatement(tokens, parentNode);
    }

    @Override
    protected AstNode parseAlterStatement( DdlTokenStream tokens,
                                           AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        if (tokens.matches(ALTER, TABLE)) {
            return parseAlterTableStatement(tokens, parentNode);
        } else if (tokens.matches(STMT_ALTER_CLUSTER)) {
            return parseStatement(tokens, STMT_ALTER_CLUSTER, parentNode, TYPE_ALTER_CLUSTER_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_DATABASE)) {
            // could encounter: ALTER DATABASE RENAME FILE 'diskc:log3.log' TO 'diskb:log3.log';
            // So need to parse up past the RENAME check
            markStartOfStatement(tokens);
            tokens.consume(STMT_ALTER_DATABASE);
            AstNode result = nodeFactory().node("database", parentNode, TYPE_ALTER_DATABASE_STATEMENT);
            tokens.canConsume("RENAME");
            parseUntilTerminator(tokens);
            markEndOfStatement(tokens, result);
            return result;
        } else if (tokens.matches(STMT_ALTER_DIMENSION)) {
            return parseStatement(tokens, STMT_ALTER_DIMENSION, parentNode, TYPE_ALTER_DIMENSION_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_DISKGROUP)) {
            return parseStatement(tokens, STMT_ALTER_DISKGROUP, parentNode, TYPE_ALTER_DISKGROUP_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_FUNCTION)) {
            return parseStatement(tokens, STMT_ALTER_FUNCTION, parentNode, TYPE_ALTER_FUNCTION_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_INDEX)) {
            // could encounter: ALTER INDEX upper_ix RENAME TO xxxxxx
            // So need to parse up past the RENAME check
            markStartOfStatement(tokens);
            tokens.consume(ALTER, INDEX);
            String indexName = parseName(tokens);
            AstNode result = nodeFactory().node(indexName, parentNode, TYPE_ALTER_INDEX_STATEMENT);
            tokens.canConsume("RENAME");
            parseUntilTerminator(tokens);
            markEndOfStatement(tokens, result);
            return result;
        } else if (tokens.matches(STMT_ALTER_INDEXTYPE)) {
            return parseStatement(tokens, STMT_ALTER_INDEXTYPE, parentNode, TYPE_ALTER_INDEXTYPE_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_JAVA)) {
            return parseStatement(tokens, STMT_ALTER_JAVA, parentNode, TYPE_ALTER_JAVA_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_MATERIALIZED)) {
            return parseStatement(tokens, STMT_ALTER_MATERIALIZED, parentNode, TYPE_ALTER_MATERIALIZED_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_OPERATOR)) {
            return parseStatement(tokens, STMT_ALTER_OPERATOR, parentNode, TYPE_ALTER_OPERATOR_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_OUTLINE)) {
            return parseStatement(tokens, STMT_ALTER_OUTLINE, parentNode, TYPE_ALTER_OUTLINE_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_PACKAGE)) {
            return parseStatement(tokens, STMT_ALTER_PACKAGE, parentNode, TYPE_ALTER_PACKAGE_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_PROCEDURE)) {
            return parseStatement(tokens, STMT_ALTER_PROCEDURE, parentNode, TYPE_ALTER_PROCEDURE_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_PROFILE)) {
            return parseStatement(tokens, STMT_ALTER_PROFILE, parentNode, TYPE_ALTER_PROFILE_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_RESOURCE)) {
            return parseStatement(tokens, STMT_ALTER_RESOURCE, parentNode, TYPE_ALTER_RESOURCE_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_ROLE)) {
            return parseStatement(tokens, STMT_ALTER_ROLE, parentNode, TYPE_ALTER_ROLE_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_ROLLBACK)) {
            return parseStatement(tokens, STMT_ALTER_ROLLBACK, parentNode, TYPE_ALTER_ROLLBACK_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_SEQUENCE)) {
            return parseStatement(tokens, STMT_ALTER_SEQUENCE, parentNode, TYPE_ALTER_SEQUENCE_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_SESSION)) {
            return parseStatement(tokens, STMT_ALTER_SESSION, parentNode, TYPE_ALTER_SESSION_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_SYSTEM)) {
            return parseStatement(tokens, STMT_ALTER_SYSTEM, parentNode, TYPE_ALTER_SYSTEM_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_TABLESPACE)) {
            return parseStatement(tokens, STMT_ALTER_TABLESPACE, parentNode, TYPE_ALTER_TABLESPACE_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_TRIGGER)) {
            return parseStatement(tokens, STMT_ALTER_TRIGGER, parentNode, TYPE_ALTER_TRIGGER_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_TYPE)) {
            return parseStatement(tokens, STMT_ALTER_TYPE, parentNode, TYPE_ALTER_TYPE_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_USER)) {
            // could encounter: ALTER USER app_user1 GRANT .....
            // So need to parse up past the GRANT check
            markStartOfStatement(tokens);
            tokens.consume(STMT_ALTER_USER);
            String name = parseName(tokens);
            AstNode result = nodeFactory().node(name, parentNode, TYPE_ALTER_USER_STATEMENT);
            tokens.canConsume("GRANT");
            parseUntilTerminator(tokens);
            markEndOfStatement(tokens, result);
            return result;
        } else if (tokens.matches(STMT_ALTER_VIEW)) {
            return parseStatement(tokens, STMT_ALTER_VIEW, parentNode, TYPE_ALTER_VIEW_STATEMENT);
        }

        return super.parseAlterStatement(tokens, parentNode);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseCreateViewStatement(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseCreateViewStatement( DdlTokenStream tokens,
                                                AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        markStartOfStatement(tokens);
        // CREATE [OR REPLACE]
        // [[NO] FORCE] VIEW [schema.] view
        // [ ( { alias [ inline_constraint... ]
        // | out_of_line_constraint
        // }
        // [, { alias [ inline_constraint...]
        // | out_of_line_constraint
        // }
        // ]
        // )
        // | object_view_clause
        // | XMLType_view_clause
        // ]
        // AS subquery [ subquery_restriction_clause ] ;

        // NOTE: the query expression along with the CHECK OPTION clause require no SQL statement terminator.
        // So the CHECK OPTION clause will NOT

        String stmtType = "CREATE";
        tokens.consume("CREATE");
        if (tokens.canConsume("OR", "REPLACE")) {
            stmtType = stmtType + SPACE + "OR REPLACE";
        } else if (tokens.canConsume("NO", "FORCE")) {
            stmtType = stmtType + SPACE + "NO FORCE";
        } else if (tokens.canConsume("FORCE")) {
            stmtType = stmtType + SPACE + "FORCE";
        }

        tokens.consume("VIEW");
        stmtType = stmtType + SPACE + "VIEW";

        String name = parseName(tokens);

        AstNode createViewNode = nodeFactory().node(name, parentNode, TYPE_CREATE_VIEW_STATEMENT);

        // CONSUME COLUMNS
        parseColumnNameList(tokens, createViewNode, TYPE_COLUMN_REFERENCE);

        // (object_view_clause)
        //
        // OF [ schema. ] type_name
        // { WITH OBJECT IDENTIFIER
        // { DEFAULT | ( attribute [, attribute ]... ) }
        // | UNDER [ schema. ] superview
        // }
        // ( { out_of_line_constraint
        // | attribute { inline_constraint }...
        // } [, { out_of_line_constraint
        // | attribute { inline_constraint }...
        // }
        // ]...
        // )

        // (XMLType_view_clause)
        //
        // OF XMLTYPE [ XMLSchema_spec ]
        // WITH OBJECT IDENTIFIER
        // { DEFAULT | ( expr [, expr ]...) }

        // Basically, if next token matches "OF", then parse until token matches "AS"

        if (tokens.matches("OF")) {
            do {
                tokens.consume();
            } while (!tokens.matches("AS"));
        }

        tokens.consume("AS");

        String queryExpression = parseUntilTerminator(tokens);

        createViewNode.setProperty(CREATE_VIEW_QUERY_EXPRESSION, queryExpression);

        markEndOfStatement(tokens, createViewNode);

        return createViewNode;
    }

    /**
     * Parses DDL CREATE INDEX
     * 
     * @param tokens the tokenized {@link DdlTokenStream} of the DDL input content; may not be null
     * @param parentNode the parent {@link AstNode} node; may not be null
     * @return the parsed CREATE INDEX
     * @throws ParsingException
     */
    private AstNode parseCreateIndex( DdlTokenStream tokens,
                                      AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        // markStartOfStatement(tokens);
        //    	
        // // CREATE [ UNIQUE | BITMAP ] INDEX index-name ON
        // // { cluster_index_clause | table_index_clause | bitmap_join_index_clause }
        // // cluster_index_clause = CLUSTER cluster-name index_attributes
        // // table_index_clause =
        // // CREATE [UNIQUE] INDEX index-Name
        // // ON table-Name ( Simple-column-Name [ ASC | DESC ] [ , Simple-column-Name [ ASC | DESC ]] * )
        // tokens.consume(CREATE); // CREATE
        //    	
        // boolean isUnique = tokens.canConsume("UNIQUE");
        // boolean isBitmap = tokens.canConsume("BITMAP");
        // tokens.consume("INDEX");
        // String indexName = parseName(tokens);
        //    	
        // AstNode indexNode = nodeFactory().node(indexName, parentNode, TYPE_CREATE_INDEX_STATEMENT);
        //		
        // tokens.consume("ON");
        // boolean isCluster = tokens.canConsume("CLUSTER");
        //    	
        // String objName = parseName(tokens);
        //    	
        // indexNode.setProperty(UNIQUE_INDEX, isUnique);
        // indexNode.setProperty(BITMAP_INDEX, isBitmap);
        // if( !isCluster ) {
        // indexNode.setProperty(OracleDdlLexicon.TABLE_NAME, objName);
        // }
        //    	
        // parseUntilTerminator(tokens);
        //        
        // markEndOfStatement(tokens, indexNode);
        //        
        // return indexNode;

        return parseStatement(tokens, STMT_CREATE_INDEX, parentNode, TYPE_CREATE_INDEX_STATEMENT);
    }

    private AstNode parseCommentStatement( DdlTokenStream tokens,
                                           AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        markStartOfStatement(tokens);

        // COMMENT ON COLUMN CS_EXT_FILES.FILE_UID IS
        // 'UNIQUE INTERNAL IDENTIFIER, NOT EXPOSED'
        // %

        /*
        	commentCommand
        		:	'COMMENT' 'ON' 
        		  ( 'TABLE' tableName 
        		  | 'VIEW' tableName
        		  | 'INDEX' indexName
        		  | 'COLUMN' columnName 
        		  | 'PROCEDURE' procedureId
        		  | 'MATERIALIZED'? 'VIEW' tableName 
        		  ) 'IS' ('NULL' | (CharLiteral)+) commandEnd? //CharLiteral (',' unicodeCharLiteral)*) commandEnd?
        		;
         */
        tokens.consume("COMMENT"); // consumes 'COMMENT' 'ON'
        tokens.consume("ON");
        String obj = tokens.consume();
        String objName = parseName(tokens);

        // System.out.println("  >> FOUND [COMMENT ON] STATEMENT >>  TABLE Name = " + objName);
        String commentString = null;

        tokens.consume("IS");
        if (tokens.matches("NULL")) {
            tokens.consume("NULL");
            commentString = "NULL";
        } else {
            commentString = parseUntilTerminator(tokens).trim();
        }

        AstNode commentNode = nodeFactory().node(objName, parentNode, TYPE_COMMENT_ON_STATEMENT);
        commentNode.setProperty(OracleDdlLexicon.COMMENT, commentString);
        commentNode.setProperty(OracleDdlLexicon.TARGET_OBJECT_TYPE, obj);

        markEndOfStatement(tokens, commentNode);

        return commentNode;
    }

    @Override
    protected AstNode parseSetStatement( DdlTokenStream tokens,
                                         AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        if (tokens.matches(STMT_SET_CONSTRAINT)) {
            return parseStatement(tokens, STMT_SET_CONSTRAINT, parentNode, TYPE_SET_CONSTRAINT_STATEMENT);
        } else if (tokens.matches(STMT_SET_CONSTRAINTS)) {
            return parseStatement(tokens, STMT_SET_CONSTRAINTS, parentNode, TYPE_SET_CONSTRAINTS_STATEMENT);
        } else if (tokens.matches(STMT_SET_ROLE)) {
            return parseStatement(tokens, STMT_SET_ROLE, parentNode, TYPE_SET_ROLE_STATEMENT);
        } else if (tokens.matches(STMT_SET_TRANSACTION)) {
            return parseStatement(tokens, STMT_SET_TRANSACTION, parentNode, TYPE_SET_TRANSACTION_STATEMENT);
        }

        return super.parseSetStatement(tokens, parentNode);
    }

    @Override
    protected AstNode parseDropStatement( DdlTokenStream tokens,
                                          AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        AstNode dropNode = null;

        if (tokens.matches(StatementStartPhrases.STMT_DROP_TABLE)) {
            markStartOfStatement(tokens);

            // DROP TABLE [ schema. ]table [ CASCADE CONSTRAINTS ] [ PURGE ] ;

            tokens.consume(DROP, TABLE);

            String name = parseName(tokens);
            dropNode = nodeFactory().node(name, parentNode, TYPE_DROP_TABLE_STATEMENT);

            dropNode.setProperty(DROP_OPTION_TYPE, "TABLE");

            if (tokens.matchesAnyOf("CASCADE", "RESTRICT")) {
                StringBuffer sb = new StringBuffer();

                if (tokens.canConsume("CASCADE")) {
                    sb.append("CASCADE");
                    tokens.consume("CONSTRAINTS");
                    sb.append(SPACE).append("CONSTRAINTS");
                    AstNode optionNode = nodeFactory().node("OPTION", dropNode, StandardDdlLexicon.DROP_OPTION_TYPE);
                    optionNode.setProperty(StandardDdlLexicon.NAME, "CASCADE OR RESTRICT");
                    optionNode.setProperty(StandardDdlLexicon.VALUE, sb.toString());
                } else {
                    tokens.consume("RESTRICT");
                    sb.append("RESTRICT");
                    tokens.consume("CONSTRAINTS");
                    sb.append(SPACE).append("CONSTRAINTS");
                    AstNode optionNode = nodeFactory().node("OPTION", dropNode, StandardDdlLexicon.DROP_OPTION_TYPE);
                    optionNode.setProperty(StandardDdlLexicon.NAME, "CASCADE OR RESTRICT");
                    optionNode.setProperty(StandardDdlLexicon.VALUE, sb.toString());
                }

            }

            if (tokens.canConsume("PURGE")) {
                AstNode optionNode = nodeFactory().node("OPTION", dropNode, StandardDdlLexicon.DROP_OPTION_TYPE);
                optionNode.setProperty(StandardDdlLexicon.NAME, "PURGE");
                optionNode.setProperty(StandardDdlLexicon.VALUE, "PURGE");
            }

            markEndOfStatement(tokens, dropNode);

            return dropNode;
        } else if (tokens.matches(STMT_DROP_CLUSTER)) {
            return parseStatement(tokens, STMT_DROP_CLUSTER, parentNode, TYPE_DROP_CLUSTER_STATEMENT);
        } else if (tokens.matches(STMT_DROP_CONTEXT)) {
            return parseStatement(tokens, STMT_DROP_CONTEXT, parentNode, TYPE_DROP_CONTEXT_STATEMENT);
        } else if (tokens.matches(STMT_DROP_DATABASE)) {
            return parseStatement(tokens, STMT_DROP_DATABASE, parentNode, TYPE_DROP_DATABASE_STATEMENT);
        } else if (tokens.matches(STMT_DROP_PUBLIC_DATABASE)) {
            return parseStatement(tokens, STMT_DROP_PUBLIC_DATABASE, parentNode, TYPE_DROP_DATABASE_STATEMENT);
        } else if (tokens.matches(STMT_DROP_DIMENSION)) {
            return parseStatement(tokens, STMT_DROP_DIMENSION, parentNode, TYPE_DROP_DIMENSION_STATEMENT);
        } else if (tokens.matches(STMT_DROP_DIRECTORY)) {
            return parseStatement(tokens, STMT_DROP_DIRECTORY, parentNode, TYPE_DROP_DIRECTORY_STATEMENT);
        } else if (tokens.matches(STMT_DROP_DISKGROUP)) {
            return parseStatement(tokens, STMT_DROP_DISKGROUP, parentNode, TYPE_DROP_DISKGROUP_STATEMENT);
        } else if (tokens.matches(STMT_DROP_FUNCTION)) {
            return parseStatement(tokens, STMT_DROP_FUNCTION, parentNode, TYPE_DROP_FUNCTION_STATEMENT);
        } else if (tokens.matches(STMT_DROP_INDEX)) {
            return parseStatement(tokens, STMT_DROP_INDEX, parentNode, TYPE_DROP_INDEX_STATEMENT);
        } else if (tokens.matches(STMT_DROP_INDEXTYPE)) {
            return parseStatement(tokens, STMT_DROP_INDEXTYPE, parentNode, TYPE_DROP_INDEXTYPE_STATEMENT);
        } else if (tokens.matches(STMT_DROP_JAVA)) {
            return parseStatement(tokens, STMT_DROP_JAVA, parentNode, TYPE_DROP_JAVA_STATEMENT);
        } else if (tokens.matches(STMT_DROP_LIBRARY)) {
            return parseStatement(tokens, STMT_DROP_LIBRARY, parentNode, TYPE_DROP_LIBRARY_STATEMENT);
        } else if (tokens.matches(STMT_DROP_MATERIALIZED)) {
            return parseStatement(tokens, STMT_DROP_MATERIALIZED, parentNode, TYPE_DROP_MATERIALIZED_STATEMENT);
        } else if (tokens.matches(STMT_DROP_OPERATOR)) {
            return parseStatement(tokens, STMT_DROP_OPERATOR, parentNode, TYPE_DROP_OPERATOR_STATEMENT);
        } else if (tokens.matches(STMT_DROP_OUTLINE)) {
            return parseStatement(tokens, STMT_DROP_OUTLINE, parentNode, TYPE_DROP_OUTLINE_STATEMENT);
        } else if (tokens.matches(STMT_DROP_PACKAGE)) {
            return parseStatement(tokens, STMT_DROP_PACKAGE, parentNode, TYPE_DROP_PACKAGE_STATEMENT);
        } else if (tokens.matches(STMT_DROP_PROCEDURE)) {
            return parseStatement(tokens, STMT_DROP_PROCEDURE, parentNode, TYPE_DROP_PROCEDURE_STATEMENT);
        } else if (tokens.matches(STMT_DROP_PROFILE)) {
            return parseStatement(tokens, STMT_DROP_PROFILE, parentNode, TYPE_DROP_PROFILE_STATEMENT);
        } else if (tokens.matches(STMT_DROP_ROLE)) {
            return parseStatement(tokens, STMT_DROP_ROLE, parentNode, TYPE_DROP_ROLE_STATEMENT);
        } else if (tokens.matches(STMT_DROP_ROLLBACK)) {
            return parseStatement(tokens, STMT_DROP_ROLLBACK, parentNode, TYPE_DROP_ROLLBACK_STATEMENT);
        } else if (tokens.matches(STMT_DROP_SEQUENCE)) {
            return parseStatement(tokens, STMT_DROP_SEQUENCE, parentNode, TYPE_DROP_SEQUENCE_STATEMENT);
        } else if (tokens.matches(STMT_DROP_SYNONYM)) {
            return parseStatement(tokens, STMT_DROP_SYNONYM, parentNode, TYPE_DROP_SYNONYM_STATEMENT);
        } else if (tokens.matches(STMT_DROP_PUBLIC_SYNONYM)) {
            return parseStatement(tokens, STMT_DROP_PUBLIC_SYNONYM, parentNode, TYPE_DROP_SYNONYM_STATEMENT);
        } else if (tokens.matches(STMT_DROP_TABLESPACE)) {
            return parseStatement(tokens, STMT_DROP_TABLESPACE, parentNode, TYPE_DROP_TABLESPACE_STATEMENT);
        } else if (tokens.matches(STMT_DROP_TRIGGER)) {
            return parseStatement(tokens, STMT_DROP_TRIGGER, parentNode, TYPE_DROP_TRIGGER_STATEMENT);
        } else if (tokens.matches(STMT_DROP_TYPE)) {
            return parseStatement(tokens, STMT_DROP_TYPE, parentNode, TYPE_DROP_TYPE_STATEMENT);
        } else if (tokens.matches(STMT_DROP_USER)) {
            return parseStatement(tokens, STMT_DROP_USER, parentNode, TYPE_DROP_USER_STATEMENT);
        }

        return super.parseDropStatement(tokens, parentNode);
    }

    private AstNode parseCreateJavaStatement( DdlTokenStream tokens,
                                              AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        markStartOfStatement(tokens);
        tokens.consume(STMT_CREATE_JAVA);
        AstNode result = nodeFactory().node(getStatementTypeName(STMT_CREATE_JAVA), parentNode, TYPE_CREATE_JAVA_STATEMENT);

        // We want to parse until we find a terminator AND all brackets are matched.

        // Assume we start with open parenthesis '{', then we can count on walking through ALL tokens until we find the close
        // parenthesis '}'. If there are intermediate parenthesis, we can count on them being pairs.

        int iParen = 0;

        while (tokens.hasNext()) {
            if (tokens.matches('{')) {
                iParen++;
            } else if (tokens.matches('}')) {
                iParen--;
            }
            tokens.consume();

            if (isTerminator(tokens) && iParen == 0) {
                break;
            }
        }

        markEndOfStatement(tokens, result);

        return result;
    }

    /**
     * Utility method designed to parse columns within an ALTER TABLE ADD statement.
     * 
     * @param tokens the tokenized {@link DdlTokenStream} of the DDL input content; may not be null
     * @param tableNode
     * @param isAlterTable
     * @throws ParsingException
     */
    protected void parseColumns( DdlTokenStream tokens,
                                 AstNode tableNode,
                                 boolean isAlterTable ) throws ParsingException {
        assert tokens != null;
        assert tableNode != null;

        // TODO: Oracle has changed some things between versions 9i, and 10/11,
        // Basically they've added column properties (i.e. SORT option, ENCRYPT encryption_spec)
        // Need to 1) Override parseColumnDefinition shouldParseOracleProceduresAndFunctionsto handle these.

        String tableElementString = getTableElementsString(tokens, false);

        DdlTokenStream localTokens = new DdlTokenStream(tableElementString, DdlTokenStream.ddlTokenizer(false), false);

        localTokens.start();

        StringBuffer unusedTokensSB = new StringBuffer();

        do {
            if (isColumnDefinitionStart(localTokens)) {
                parseColumnDefinition(localTokens, tableNode, true);
            } else {
                // THIS IS AN ERROR. NOTHING FOUND.
                // NEED TO absorb tokens
                while (localTokens.hasNext() && !localTokens.matches(COMMA)) {
                    unusedTokensSB.append(SPACE).append(localTokens.consume());
                }
            }
        } while (localTokens.canConsume(COMMA));

        if (unusedTokensSB.length() > 0) {
            String msg = DdlSequencerI18n.unusedTokensParsingColumnDefinition.text(tableNode.getProperty(StandardDdlLexicon.NAME));
            DdlParserProblem problem = new DdlParserProblem(Problems.WARNING, getCurrentMarkedPosition(), msg);
            problem.setUnusedSource(unusedTokensSB.toString());
            addProblem(problem, tableNode);
        }
    }

    /**
     * Utility method to parse a generic statement given a start phrase and statement mixin type.
     * 
     * @param tokens the tokenized {@link DdlTokenStream} of the DDL input content; may not be null
     * @param stmt_start_phrase the string array statement start phrase
     * @param parentNode the parent {@link AstNode} node; may not be null
     * @param mixinType the mixin type of the newly created statement node
     * @return the new node
     */
    protected AstNode parseSlashedStatement( DdlTokenStream tokens,
                                             String[] stmt_start_phrase,
                                             AstNode parentNode,
                                             Name mixinType ) {
        assert tokens != null;
        assert stmt_start_phrase != null && stmt_start_phrase.length > 0;
        assert parentNode != null;

        markStartOfStatement(tokens);
        tokens.consume(stmt_start_phrase);
        AstNode result = nodeFactory().node(getStatementTypeName(stmt_start_phrase), parentNode, mixinType);

        parseUntilFwdSlash(tokens, false);

        consumeSlash(tokens);

        markEndOfStatement(tokens, result);

        return result;
    }

    /**
     * Various Oracle statements (i.e. "CREATE OR REPLACE PACKAGE", etc...) may contain multiple SQL statements that will be
     * terminated by the semicolon, ';'. In these cases, the terminator is now the '/' character.
     * 
     * @param tokens the tokenized {@link DdlTokenStream} of the DDL input content; may not be null
     * @param stopAtStatementStart
     * @return the parsed string.
     * @throws ParsingException
     */
    private String parseUntilFwdSlash( DdlTokenStream tokens,
                                       boolean stopAtStatementStart ) throws ParsingException {
        StringBuffer sb = new StringBuffer();
        if (stopAtStatementStart) {
            while (tokens.hasNext()

            && !tokens.matches(DdlTokenizer.STATEMENT_KEY) && !tokens.matches('/')) { // !tokens.matches(DdlTokenizer.STATEMENT_KEY
                // )
                // &&
                sb.append(SPACE).append(tokens.consume());
            }
        } else {
            while (tokens.hasNext() && !isFwdSlashedStatement(tokens) && !tokens.matches('/')) { // !tokens.matches(DdlTokenizer.
                // STATEMENT_KEY) &&
                sb.append(SPACE).append(tokens.consume());
            }
        }
        return sb.toString();
    }

    private boolean isFwdSlashedStatement( DdlTokenStream tokens ) throws ParsingException {
        for (int i = 0; i < SLASHED_STMT_PHRASES.length; i++) {
            if (tokens.matches(SLASHED_STMT_PHRASES[i])) {
                return true;
            }
        }

        return false;
    }

    private void consumeSlash( DdlTokenStream tokens ) throws ParsingException {
        tokens.canConsume("/");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#getValidSchemaChildTypes()
     */
    @Override
    protected Name[] getValidSchemaChildTypes() {
        return VALID_SCHEMA_CHILD_STMTS;
    }

    // ===========================================================================================================================
    // PARSE OBJECTS
    // ===========================================================================================================================

    /**
     * This class provides custom data type parsing for Oracle-specific data types.
     */
    class OracleDataTypeParser extends DataTypeParser {

        /*
         * (non-Javadoc)
         * @see org.modeshape.sequencer.ddl.datatype.DataTypeParser#parseCustomType(org.modeshape.common.text.DdlTokenStream)
         */
        @Override
        protected DataType parseCustomType( DdlTokenStream tokens ) throws ParsingException {
            DataType dataType = null;
            String typeName = null;

            if (tokens.matches(OracleDataTypes.DTYPE_BINARY_FLOAT)) {
                dataType = new DataType();
                typeName = consume(tokens, dataType, true, OracleDataTypes.DTYPE_BINARY_FLOAT);
                dataType.setName(typeName);
            } else if (tokens.matches(OracleDataTypes.DTYPE_BINARY_DOUBLE)) {
                dataType = new DataType();
                typeName = consume(tokens, dataType, true, OracleDataTypes.DTYPE_BINARY_DOUBLE);
                dataType.setName(typeName);
            } else if (tokens.matches(OracleDataTypes.DTYPE_LONG)) {
                dataType = new DataType();
                typeName = consume(tokens, dataType, true, OracleDataTypes.DTYPE_LONG);
                dataType.setName(typeName);
            } else if (tokens.matches(OracleDataTypes.DTYPE_LONG_RAW)) {
                dataType = new DataType();
                typeName = consume(tokens, dataType, true, OracleDataTypes.DTYPE_LONG_RAW);
                dataType.setName(typeName);
            } else if (tokens.matches(OracleDataTypes.DTYPE_BLOB)) {
                dataType = new DataType();
                typeName = consume(tokens, dataType, true, OracleDataTypes.DTYPE_BLOB);
                dataType.setName(typeName);
            } else if (tokens.matches(OracleDataTypes.DTYPE_CLOB)) {
                dataType = new DataType();
                typeName = consume(tokens, dataType, true, OracleDataTypes.DTYPE_CLOB);
                dataType.setName(typeName);
            } else if (tokens.matches(OracleDataTypes.DTYPE_NCLOB)) {
                dataType = new DataType();
                typeName = consume(tokens, dataType, true, OracleDataTypes.DTYPE_NCLOB);
                dataType.setName(typeName);
            } else if (tokens.matches(OracleDataTypes.DTYPE_BFILE)) {
                dataType = new DataType();
                typeName = consume(tokens, dataType, true, OracleDataTypes.DTYPE_BFILE);
                dataType.setName(typeName);
            } else if (tokens.matches(OracleDataTypes.DTYPE_VARCHAR2)) {
                dataType = new DataType();
                // VARCHAR2(size [BYTE | CHAR])
                typeName = consume(tokens, dataType, true, OracleDataTypes.DTYPE_VARCHAR2); // VARCHAR2
                consume(tokens, dataType, false, L_PAREN);
                long length = parseLong(tokens, dataType);
                canConsume(tokens, dataType, true, "BYTE");
                canConsume(tokens, dataType, true, "CHAR");
                consume(tokens, dataType, false, R_PAREN);
                dataType.setName(typeName);
                dataType.setLength(length);
            } else if (tokens.matches(OracleDataTypes.DTYPE_RAW)) {
                dataType = new DataType();
                typeName = consume(tokens, dataType, true, OracleDataTypes.DTYPE_RAW);
                long length = parseBracketedLong(tokens, dataType);
                dataType.setName(typeName);
                dataType.setLength(length);
            } else if (tokens.matches(OracleDataTypes.DTYPE_NVARCHAR2)) {
                dataType = new DataType();
                typeName = consume(tokens, dataType, true, OracleDataTypes.DTYPE_NVARCHAR2);
                long length = parseBracketedLong(tokens, dataType);
                dataType.setName(typeName);
                dataType.setLength(length);
            } else if (tokens.matches(OracleDataTypes.DTYPE_NUMBER)) {
                dataType = new DataType();
                typeName = consume(tokens, dataType, true, OracleDataTypes.DTYPE_NUMBER);
                int precision = 0;
                int scale = 0;
                if (tokens.matches(L_PAREN)) {
                    consume(tokens, dataType, false, L_PAREN);
                    precision = (int)parseLong(tokens, dataType);
                    if (canConsume(tokens, dataType, false, COMMA)) {
                        scale = (int)parseLong(tokens, dataType);
                    } else {
                        scale = getDefaultScale();
                    }
                    consume(tokens, dataType, false, R_PAREN);
                } else {
                    precision = getDefaultPrecision();
                    scale = getDefaultScale();
                }
                dataType.setName(typeName);
                dataType.setPrecision(precision);
                dataType.setScale(scale);
            } else if (tokens.matches(OracleDataTypes.DTYPE_INTERVAL_YEAR)) {
                // INTERVAL YEAR (year_precision) TO MONTH

            } else if (tokens.matches(OracleDataTypes.DTYPE_INTERVAL_DAY)) {
                // INTERVAL DAY (day_precision) TO SECOND (fractional_seconds_precision)
            }

            if (dataType == null) {
                dataType = super.parseCustomType(tokens);
            }

            return dataType;
        }

        /**
         * Because Oracle has an additional option on the CHAR datatype, we need to override the super method, check for CHAR type
         * and parse, else call super.parseCharStringType(). {@inheritDoc}
         * 
         * @see org.modeshape.sequencer.ddl.datatype.DataTypeParser#parseCharStringType(org.modeshape.sequencer.ddl.DdlTokenStream)
         */
        @Override
        protected DataType parseCharStringType( DdlTokenStream tokens ) throws ParsingException {
            DataType dataType = null;

            if (tokens.matches(OracleDataTypes.DTYPE_CHAR_ORACLE)) { // CHAR (size [BYTE | CHAR]) (i.e. CHAR (10 BYTE) )
                dataType = new DataType();
                String typeName = consume(tokens, dataType, true, OracleDataTypes.DTYPE_CHAR_ORACLE);
                consume(tokens, dataType, false, L_PAREN);
                long length = parseLong(tokens, dataType);
                canConsume(tokens, dataType, true, "BYTE");
                canConsume(tokens, dataType, true, "CHAR");
                consume(tokens, dataType, false, R_PAREN);
                dataType.setName(typeName);
                dataType.setLength(length);
            } else {
                dataType = super.parseCharStringType(tokens);
            }

            return dataType;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.sequencer.ddl.datatype.DataTypeParser#isCustomDataType(org.modeshape.sequencer.ddl.DdlTokenStream)
         */
        @Override
        protected boolean isCustomDataType( DdlTokenStream tokens ) throws ParsingException {
            for (String[] stmt : oracleDataTypeStrings) {
                if (tokens.matches(stmt)) return true;
            }

            return false;
        }

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#getDataTypeStartWords()
     */
    @Override
    protected List<String> getCustomDataTypeStartWords() {
        return OracleDataTypes.CUSTOM_DATATYPE_START_WORDS;
    }
}
