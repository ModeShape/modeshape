package org.modeshape.sequencer.ddl.dialect.derby;

import static org.modeshape.sequencer.ddl.StandardDdlLexicon.ALL_PRIVILEGES;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.COLUMN_ATTRIBUTE_TYPE;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.GRANTEE;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.GRANT_PRIVILEGE;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.NEW_NAME;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.PROPERTY_VALUE;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.SQL;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_ALTER_COLUMN_DEFINITION;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_ALTER_TABLE_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_COLUMN_DEFINITION;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_COLUMN_REFERENCE;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_CREATE_TABLE_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_COLUMN_DEFINITION;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_TABLE_CONSTRAINT_DEFINITION;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_GRANT_ON_TABLE_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_STATEMENT_OPTION;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.VALUE;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.IS_TABLE_TYPE;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.ORDER;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.ROLE_NAME;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TABLE_NAME;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_CREATE_FUNCTION_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_CREATE_INDEX_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_CREATE_PROCEDURE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_CREATE_ROLE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_CREATE_SYNONYM_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_CREATE_TRIGGER_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_DECLARE_GLOBAL_TEMPORARY_TABLE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_DROP_FUNCTION_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_DROP_INDEX_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_DROP_PROCEDURE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_DROP_ROLE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_DROP_SYNONYM_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_DROP_TRIGGER_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_FUNCTION_PARAMETER;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_GRANT_ON_FUNCTION_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_GRANT_ON_PROCEDURE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_GRANT_ROLES_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_INDEX_COLUMN_REFERENCE;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_LOCK_TABLE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_RENAME_INDEX_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_RENAME_TABLE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.UNIQUE_INDEX;
import java.util.ArrayList;
import java.util.List;
import org.modeshape.common.text.ParsingException;
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
 * Derby-specific DDL Parser. Includes custom data types as well as custom DDL statements.
 */
public class DerbyDdlParser extends StandardDdlParser implements DerbyDdlConstants, DerbyDdlConstants.DerbyStatementStartPhrases {
    private final String parserId = "DERBY";

    protected static final List<String[]> derbyDataTypeStrings = new ArrayList<String[]>(
                                                                                         DerbyDataTypes.CUSTOM_DATATYPE_START_PHRASES);

    private static final String TERMINATOR = DEFAULT_TERMINATOR;

    public DerbyDdlParser() {
        setDatatypeParser(new DerbyDataTypeParser());
        setDoUseTerminator(true);
        setTerminator(TERMINATOR);
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
        tokens.registerStatementStartPhrase(ALTER_PHRASES);
        tokens.registerStatementStartPhrase(CREATE_PHRASES);
        tokens.registerStatementStartPhrase(DROP_PHRASES);
        tokens.registerStatementStartPhrase(SET_PHRASES);
        tokens.registerStatementStartPhrase(MISC_PHRASES);
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
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#getValidSchemaChildTypes()
     */
    @Override
    protected Name[] getValidSchemaChildTypes() {
        return VALID_SCHEMA_CHILD_STMTS;
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

        AstNode result = super.parseCustomStatement(tokens, parentNode);
        if (result == null) {
            if (tokens.matches(STMT_LOCK_TABLE)) {
                result = parseLockTable(tokens, parentNode);
            } else if (tokens.matches(STMT_RENAME_TABLE)) {
                result = parseRenameTable(tokens, parentNode);
            } else if (tokens.matches(STMT_RENAME_INDEX)) {
                result = parseRenameIndex(tokens, parentNode);
            } else if (tokens.matches(STMT_DECLARE_GLOBAL_TEMP_TABLE)) {
                result = parseDeclareGlobalTempTable(tokens, parentNode);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseCreateStatement(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseCreateStatement( DdlTokenStream tokens,
                                            AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        if (tokens.matches(STMT_CREATE_INDEX) || tokens.matches(STMT_CREATE_UNIQUE_INDEX)) {
            return parseCreateIndex(tokens, parentNode);
        } else if (tokens.matches(STMT_CREATE_FUNCTION)) {
            return parseCreateFunction(tokens, parentNode);
        } else if (tokens.matches(STMT_CREATE_PROCEDURE)) {
            return parseStatement(tokens, STMT_CREATE_PROCEDURE, parentNode, TYPE_CREATE_PROCEDURE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_ROLE)) {
            return parseStatement(tokens, STMT_CREATE_ROLE, parentNode, TYPE_CREATE_ROLE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_SYNONYM)) {
            return parseCreateSynonym(tokens, parentNode);
        } else if (tokens.matches(STMT_CREATE_TRIGGER)) {
            return parseCreateTrigger(tokens, parentNode);
        }

        return super.parseCreateStatement(tokens, parentNode);

    }

    /**
     * Parses DDL CREATE INDEX
     * 
     * @param tokens the tokenized {@link DdlTokenStream} of the DDL input content; may not be null
     * @param parentNode the parent {@link AstNode} node; may not be null
     * @return the parsed CREATE INDEX
     * @throws ParsingException
     */
    protected AstNode parseCreateIndex( DdlTokenStream tokens,
                                        AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        markStartOfStatement(tokens);
        // CREATE [UNIQUE] INDEX index-Name
        // ON table-Name ( Simple-column-Name [ ASC | DESC ] [ , Simple-column-Name [ ASC | DESC ]] * )
        tokens.consume(CREATE); // CREATE

        boolean isUnique = tokens.canConsume("UNIQUE");

        tokens.consume("INDEX");
        String indexName = parseName(tokens);
        tokens.consume("ON");
        String tableName = parseName(tokens);

        AstNode indexNode = nodeFactory().node(indexName, parentNode, TYPE_CREATE_INDEX_STATEMENT);

        indexNode.setProperty(UNIQUE_INDEX, isUnique);
        indexNode.setProperty(TABLE_NAME, tableName);

        parseIndexTableColumns(tokens, indexNode);

        parseUntilTerminator(tokens);

        markEndOfStatement(tokens, indexNode);

        return indexNode;
    }

    private void parseIndexTableColumns( DdlTokenStream tokens,
                                         AstNode indexNode ) throws ParsingException {
        assert tokens != null;
        assert indexNode != null;

        // Assume we start with open parenthesis '(', then we parse comma separated list of column names followed by optional
        // ASC or DESC

        tokens.consume(L_PAREN); // EXPECTED

        while (!tokens.canConsume(R_PAREN)) {
            String colName = parseName(tokens);
            AstNode colRefNode = nodeFactory().node(colName, indexNode, TYPE_INDEX_COLUMN_REFERENCE);
            if (tokens.canConsume("ASC")) {
                colRefNode.setProperty(ORDER, "ASC");
            } else if (tokens.canConsume("DESC")) {
                colRefNode.setProperty(ORDER, "DESC");
            }
            tokens.canConsume(COMMA);
        }
    }

    /**
     * Parses DDL CREATE FUNCTION statement
     * 
     * @param tokens the tokenized {@link DdlTokenStream} of the DDL input content; may not be null
     * @param parentNode the parent {@link AstNode} node; may not be null
     * @return the parsed CREATE FUNCTION statement node
     * @throws ParsingException
     */
    protected AstNode parseCreateFunction( DdlTokenStream tokens,
                                           AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        markStartOfStatement(tokens);
        // CREATE FUNCTION function-name ( [ FunctionParameter [, FunctionParameter] ] * )
        // RETURNS ReturnDataType [ FunctionElement ] *

        // FunctionElement
        // {
        // | LANGUAGE { JAVA }
        // | {DETERMINISTIC | NOT DETERMINISTIC}
        // | EXTERNAL NAME string
        // | PARAMETER STYLE {JAVA | DERBY_JDBC_RESULT_SET}
        // | { NO SQL | CONTAINS SQL | READS SQL DATA }
        // | { RETURNS NULL ON NULL INPUT | CALLED ON NULL INPUT }
        // }
        tokens.consume(CREATE, "FUNCTION");

        String functionName = parseName(tokens);

        AstNode functionNode = nodeFactory().node(functionName, parentNode, TYPE_CREATE_FUNCTION_STATEMENT);

        parseFunctionParameters(tokens, functionNode);

        tokens.consume("RETURNS");

        if (tokens.canConsume("TABLE")) {
            AstNode tableNode = nodeFactory().node("TABLE", functionNode, TYPE_CREATE_TABLE_STATEMENT);
            parseColumnsAndConstraints(tokens, tableNode);
            tableNode.setProperty(IS_TABLE_TYPE, true);
        } else {
            // Assume DataType
            DataType datatype = getDatatypeParser().parse(tokens);
            if (datatype != null) {
                getDatatypeParser().setPropertiesOnNode(functionNode, datatype);
            } else {
                String msg = DdlSequencerI18n.missingReturnTypeForFunction.text(functionName);
                DdlParserProblem problem = new DdlParserProblem(Problems.WARNING, getCurrentMarkedPosition(), msg);
                addProblem(problem, functionNode);
            }
        }

        while (!isTerminator(tokens)) {
            if (tokens.matches("LANGUAGE")) {
                AstNode optionNode = nodeFactory().node("language", functionNode, TYPE_STATEMENT_OPTION);
                if (tokens.canConsume("LANGUAGE", "JAVA")) {
                    optionNode.setProperty(VALUE, "LANGUAGE JAVA");
                } else {
                    tokens.consume("LANGUAGE");
                    optionNode.setProperty(VALUE, "LANGUAGE");
                }
            } else if (tokens.canConsume("DETERMINISTIC")) {
                AstNode optionNode = nodeFactory().node("deterministic", functionNode, TYPE_STATEMENT_OPTION);
                optionNode.setProperty(VALUE, "DETERMINISTIC");
            } else if (tokens.canConsume("NOT", "DETERMINISTIC")) {
                AstNode optionNode = nodeFactory().node("deterministic", functionNode, TYPE_STATEMENT_OPTION);
                optionNode.setProperty(VALUE, "NOT DETERMINISTIC");
            } else if (tokens.canConsume("EXTERNAL", "NAME")) {
                String extName = parseName(tokens);
                AstNode optionNode = nodeFactory().node("externalName", functionNode, TYPE_STATEMENT_OPTION);
                optionNode.setProperty(VALUE, "EXTERNAL NAME" + SPACE + extName);
            } else if (tokens.canConsume("PARAMETER", "STYLE")) {
                AstNode optionNode = nodeFactory().node("parameterStyle", functionNode, TYPE_STATEMENT_OPTION);
                if (tokens.canConsume("JAVA")) {
                    optionNode.setProperty(VALUE, "PARAMETER STYLE" + SPACE + "JAVA");
                } else {
                    tokens.consume("DERBY_JDBC_RESULT_SET");
                    optionNode.setProperty(VALUE, "PARAMETER STYLE" + SPACE + "DERBY_JDBC_RESULT_SET");
                }
            } else if (tokens.canConsume("NO", "SQL")) {
                AstNode optionNode = nodeFactory().node("sqlStatus", functionNode, TYPE_STATEMENT_OPTION);
                optionNode.setProperty(VALUE, "NO SQL");
            } else if (tokens.canConsume("CONTAINS", "SQL")) {
                AstNode optionNode = nodeFactory().node("sqlStatus", functionNode, TYPE_STATEMENT_OPTION);
                optionNode.setProperty(VALUE, "CONTAINS SQL");
            } else if (tokens.canConsume("READS", "SQL", "DATA")) {
                AstNode optionNode = nodeFactory().node("sqlStatus", functionNode, TYPE_STATEMENT_OPTION);
                optionNode.setProperty(VALUE, "READS SQL DATA");
            } else if (tokens.canConsume("RETURNS", "NULL", "ON", "NULL", "INPUT")) {
                AstNode optionNode = nodeFactory().node("nullInput", functionNode, TYPE_STATEMENT_OPTION);
                optionNode.setProperty(VALUE, "RETURNS NULL ON NULL INPUT");
            } else if (tokens.canConsume("CALLED", "ON", "NULL", "INPUT")) {
                AstNode optionNode = nodeFactory().node("nullInput", functionNode, TYPE_STATEMENT_OPTION);
                optionNode.setProperty(VALUE, "CALLED ON NULL INPUT");
            } else {
                String msg = DdlSequencerI18n.errorParsingDdlContent.text(functionName);
                DdlParserProblem problem = new DdlParserProblem(Problems.ERROR, getCurrentMarkedPosition(), msg);
                addProblem(problem, functionNode);
                break;
            }
        }

        markEndOfStatement(tokens, functionNode);

        return functionNode;
    }

    private void parseFunctionParameters( DdlTokenStream tokens,
                                          AstNode functionNode ) throws ParsingException {
        assert tokens != null;
        assert functionNode != null;

        // Assume we start with open parenthesis '(', then we parse comma separated list of function parameters
        // which have the form: [ parameter-Name ] DataType
        // So, try getting datatype, if datatype == NULL, then parseName() & parse datatype, then repeat as long as next token is
        // ","

        tokens.consume(L_PAREN); // EXPECTED

        while (!tokens.canConsume(R_PAREN)) {
            DataType datatype = getDatatypeParser().parse(tokens);
            if (datatype == null) {
                String paramName = parseName(tokens);
                datatype = getDatatypeParser().parse(tokens);
                AstNode paramNode = nodeFactory().node(paramName, functionNode, TYPE_FUNCTION_PARAMETER);
                getDatatypeParser().setPropertiesOnNode(paramNode, datatype);
            } else {
                AstNode paramNode = nodeFactory().node("functionParameter", functionNode, TYPE_FUNCTION_PARAMETER);
                getDatatypeParser().setPropertiesOnNode(paramNode, datatype);
            }
            tokens.canConsume(COMMA);
        }
    }

    /**
     * Parses DDL CREATE PROCEDURE statement
     * 
     * @param tokens the tokenized {@link DdlTokenStream} of the DDL input content; may not be null
     * @param parentNode the parent {@link AstNode} node; may not be null
     * @return the parsed CREATE PROCEDURE statement node
     * @throws ParsingException
     */
    protected AstNode parseCreateProcedure( DdlTokenStream tokens,
                                            AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        markStartOfStatement(tokens);

        tokens.consume(CREATE, "PROCEDURE");

        String functionName = parseName(tokens);

        AstNode functionNode = nodeFactory().node(functionName, parentNode, TYPE_CREATE_FUNCTION_STATEMENT);

        markEndOfStatement(tokens, functionNode);

        return functionNode;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseDropStatement(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseDropStatement( DdlTokenStream tokens,
                                          AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        AstNode dropNode = null;

        String name = null;

        if (tokens.matches(STMT_DROP_FUNCTION)) {
            markStartOfStatement(tokens);
            tokens.consume(STMT_DROP_FUNCTION);
            name = parseName(tokens);
            dropNode = nodeFactory().node(name, parentNode, TYPE_DROP_FUNCTION_STATEMENT);
        } else if (tokens.matches(STMT_DROP_INDEX)) {
            markStartOfStatement(tokens);
            tokens.consume(STMT_DROP_INDEX);
            name = parseName(tokens);
            dropNode = nodeFactory().node(name, parentNode, TYPE_DROP_INDEX_STATEMENT);
        } else if (tokens.matches(STMT_DROP_PROCEDURE)) {
            markStartOfStatement(tokens);
            tokens.consume(STMT_DROP_PROCEDURE);
            name = parseName(tokens);
            dropNode = nodeFactory().node(name, parentNode, TYPE_DROP_PROCEDURE_STATEMENT);
        } else if (tokens.matches(STMT_DROP_ROLE)) {
            markStartOfStatement(tokens);
            tokens.consume(STMT_DROP_ROLE);
            name = parseName(tokens);
            dropNode = nodeFactory().node(name, parentNode, TYPE_DROP_ROLE_STATEMENT);
        } else if (tokens.matches(STMT_DROP_SYNONYM)) {
            markStartOfStatement(tokens);
            tokens.consume(STMT_DROP_SYNONYM);
            name = parseName(tokens);
            dropNode = nodeFactory().node(name, parentNode, TYPE_DROP_SYNONYM_STATEMENT);
        } else if (tokens.matches(STMT_DROP_TRIGGER)) {
            markStartOfStatement(tokens);
            tokens.consume(STMT_DROP_TRIGGER);
            name = parseName(tokens);
            dropNode = nodeFactory().node(name, parentNode, TYPE_DROP_TRIGGER_STATEMENT);
        }

        if (dropNode != null) {
            markEndOfStatement(tokens, dropNode);
        }

        if (dropNode == null) {
            dropNode = super.parseDropStatement(tokens, parentNode);
        }

        return dropNode;
    }

    /**
     * {@inheritDoc} Syntax for tables GRANT privilege-type ON [TABLE] { table-Name | view-Name } TO grantees Syntax for routines
     * GRANT EXECUTE ON { FUNCTION | PROCEDURE } routine-designator TO grantees Syntax for roles GRANT roleName [ {, roleName }* ]
     * TO grantees privilege-types ALL PRIVILEGES | privilege-list privilege-list table-privilege {, table-privilege }*
     * table-privilege DELETE | INSERT | REFERENCES [column list] | SELECT [column list] | TRIGGER | UPDATE [column list] column
     * list ( column-identifier {, column-identifier}* ) GRANT
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseGrantStatement(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseGrantStatement( DdlTokenStream tokens,
                                           AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;
        assert tokens.matches(GRANT);

        markStartOfStatement(tokens);

        // Syntax for tables
        //
        // GRANT privilege-type ON [TABLE] { table-Name | view-Name } TO grantees
        //
        // Syntax for routines
        //
        // GRANT EXECUTE ON { FUNCTION | PROCEDURE } {function-name | procedure-name} TO grantees
        //
        // Syntax for roles
        //
        // GRANT roleName [ {, roleName }* ] TO grantees

        // privilege-types
        //
        // ALL PRIVILEGES | privilege-list
        //
        AstNode grantNode = null;
        boolean allPrivileges = false;

        List<AstNode> privileges = new ArrayList<AstNode>();

        tokens.consume("GRANT");
        if (tokens.canConsume("EXECUTE", "ON")) {
            AstNode node = nodeFactory().node("privilege");
            nodeFactory().setType(node, GRANT_PRIVILEGE);
            node.setProperty(TYPE, "EXECUTE");
            privileges = new ArrayList<AstNode>();
            privileges.add(node);
            if (tokens.canConsume("FUNCTION")) {
                String name = parseName(tokens);
                grantNode = nodeFactory().node(name, parentNode, TYPE_GRANT_ON_FUNCTION_STATEMENT);
            } else {
                tokens.consume("PROCEDURE");
                String name = parseName(tokens);
                grantNode = nodeFactory().node(name, parentNode, TYPE_GRANT_ON_PROCEDURE_STATEMENT);
            }
        } else {

            if (tokens.canConsume("ALL", "PRIVILEGES")) {
                allPrivileges = true;
            } else {
                parseGrantPrivileges(tokens, privileges);

                if (privileges.isEmpty()) {
                    // ASSUME: GRANT roleName [ {, roleName }* ] TO grantees
                    grantNode = nodeFactory().node("grantRoles", parentNode, TYPE_GRANT_ROLES_STATEMENT);
                    do {
                        String roleName = parseName(tokens);
                        nodeFactory().node(roleName, grantNode, ROLE_NAME);
                    } while (tokens.canConsume(COMMA));
                }
            }
            if (grantNode == null) {
                tokens.consume("ON");
                tokens.canConsume(TABLE); // OPTIONAL
                String name = parseName(tokens);
                grantNode = nodeFactory().node(name, parentNode, TYPE_GRANT_ON_TABLE_STATEMENT);
                // Attach privileges to grant node
                for (AstNode node : privileges) {
                    node.setParent(grantNode);
                }
                if (allPrivileges) {
                    grantNode.setProperty(ALL_PRIVILEGES, allPrivileges);
                }
            }

        }

        tokens.consume("TO");

        do {
            String grantee = parseName(tokens);
            nodeFactory().node(grantee, grantNode, GRANTEE);
        } while (tokens.canConsume(COMMA));

        markEndOfStatement(tokens, grantNode);

        return grantNode;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseGrantPrivileges(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      java.util.List)
     */
    @Override
    protected void parseGrantPrivileges( DdlTokenStream tokens,
                                         List<AstNode> privileges ) throws ParsingException {
        // privilege-types
        //
        // ALL PRIVILEGES | privilege-list
        //
        // privilege-list
        //
        // table-privilege {, table-privilege }*
        //
        // table-privilege
        // DELETE |
        // INSERT |
        // REFERENCES [column list] |
        // SELECT [column list] |
        // TRIGGER |
        // UPDATE [column list]
        // column list
        // ( column-identifier {, column-identifier}* )

        do {
            AstNode node = null;

            if (tokens.canConsume(DELETE)) {
                node = nodeFactory().node("privilege");
                node.setProperty(TYPE, DELETE);
            } else if (tokens.canConsume(INSERT)) {
                node = nodeFactory().node("privilege");
                node.setProperty(TYPE, INSERT);
            } else if (tokens.canConsume("REFERENCES")) {
                node = nodeFactory().node("privilege");
                node.setProperty(TYPE, "REFERENCES");
                parseColumnNameList(tokens, node, TYPE_COLUMN_REFERENCE);
            } else if (tokens.canConsume(SELECT)) {
                node = nodeFactory().node("privilege");
                node.setProperty(TYPE, SELECT);
                parseColumnNameList(tokens, node, TYPE_COLUMN_REFERENCE);
            } else if (tokens.canConsume("TRIGGER")) {
                node = nodeFactory().node("privilege");
                node.setProperty(TYPE, "TRIGGER");
            } else if (tokens.canConsume(UPDATE)) {
                node = nodeFactory().node("privilege");
                node.setProperty(TYPE, UPDATE);
                parseColumnNameList(tokens, node, TYPE_COLUMN_REFERENCE);
            }
            if (node == null) {
                break;
            }
            nodeFactory().setType(node, GRANT_PRIVILEGE);
            privileges.add(node);

        } while (tokens.canConsume(COMMA));

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseAlterTableStatement(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseAlterTableStatement( DdlTokenStream tokens,
                                                AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        markStartOfStatement(tokens);

        // ALTER TABLE table-Name
        // {
        // ADD COLUMN column-definition |
        // ADD CONSTRAINT clause |
        // DROP [ COLUMN ] column-name [ CASCADE | RESTRICT ] |
        // DROP { PRIMARY KEY | FOREIGN KEY constraint-name | UNIQUE constraint-name | CHECK constraint-name | CONSTRAINT
        // constraint-name } |
        // ALTER [ COLUMN ] column-alteration |
        // LOCKSIZE { ROW | TABLE }
        // }

        tokens.consume(ALTER, TABLE); // consumes 'ALTER TABLE'
        String tableName = parseName(tokens);

        AstNode alterTableNode = nodeFactory().node(tableName, parentNode, TYPE_ALTER_TABLE_STATEMENT);

        // System.out.println("  >> PARSIN ALTER STATEMENT >>  TABLE Name = " + tableName);

        if (tokens.canConsume("ADD")) {
            if (isTableConstraint(tokens)) {
                parseTableConstraint(tokens, alterTableNode, true);
            } else {
                // This segment can also be enclosed in "()" brackets to handle multiple ColumnDefinition ADDs
                if (tokens.matches(L_PAREN)) {
                    parseColumns(tokens, alterTableNode, true);
                } else {
                    parseSingleTerminatedColumnDefinition(tokens, alterTableNode, true);
                }
            }

        } else if (tokens.canConsume("DROP")) {
            // DROP { PRIMARY KEY | FOREIGN KEY constraint-name | UNIQUE constraint-name | CHECK constraint-name | CONSTRAINT
            // constraint-name }
            if (tokens.canConsume("PRIMARY", "KEY")) {
                String name = parseName(tokens); // constraint name
                nodeFactory().node(name, alterTableNode, TYPE_DROP_TABLE_CONSTRAINT_DEFINITION);
            } else if (tokens.canConsume("FOREIGN", "KEY")) {
                String name = parseName(tokens); // constraint name
                nodeFactory().node(name, alterTableNode, TYPE_DROP_TABLE_CONSTRAINT_DEFINITION);
            } else if (tokens.canConsume("UNIQUE")) {
                String name = parseName(tokens); // constraint name
                nodeFactory().node(name, alterTableNode, TYPE_DROP_TABLE_CONSTRAINT_DEFINITION);
            } else if (tokens.canConsume("CHECK")) {
                String name = parseName(tokens); // constraint name
                nodeFactory().node(name, alterTableNode, TYPE_DROP_TABLE_CONSTRAINT_DEFINITION);
            } else if (tokens.canConsume("CONSTRAINT")) {
                String name = parseName(tokens); // constraint name
                nodeFactory().node(name, alterTableNode, TYPE_DROP_TABLE_CONSTRAINT_DEFINITION);
            } else {
                // DROP [ COLUMN ] column-name [ CASCADE | RESTRICT ]
                tokens.canConsume("COLUMN"); // "COLUMN" is optional

                String columnName = parseName(tokens);

                AstNode columnNode = nodeFactory().node(columnName, alterTableNode, TYPE_DROP_COLUMN_DEFINITION);
                columnNode.setProperty(StandardDdlLexicon.NAME, columnName);

                if (tokens.canConsume(DropBehavior.CASCADE)) {
                    columnNode.setProperty(StandardDdlLexicon.DROP_BEHAVIOR, DropBehavior.CASCADE);
                } else if (tokens.canConsume(DropBehavior.RESTRICT)) {
                    columnNode.setProperty(StandardDdlLexicon.DROP_BEHAVIOR, DropBehavior.RESTRICT);
                }
            }
        } else if (tokens.canConsume("ALTER")) {
            // column-alteration
            //
            // ALTER [ COLUMN ] column-Name SET DATA TYPE VARCHAR(integer) |
            // ALTER [ COLUMN ] column-Name SET DATA TYPE VARCHAR FOR BIT DATA(integer) |
            // ALTER [ COLUMN ] column-name SET INCREMENT BY integer-constant |
            // ALTER [ COLUMN ] column-name RESTART WITH integer-constant |
            // ALTER [ COLUMN ] column-name [ NOT ] NULL |
            // ALTER [ COLUMN ] column-name [ WITH | SET ] DEFAULT default-value |
            // ALTER [ COLUMN ] column-name DROP DEFAULT

            tokens.canConsume("COLUMN");
            String alterColumnName = parseName(tokens);

            AstNode columnNode = nodeFactory().node(alterColumnName, alterTableNode, TYPE_ALTER_COLUMN_DEFINITION);

            if (tokens.matches("DEFAULT")) {
                parseDefaultClause(tokens, columnNode);
            } else if (tokens.canConsume("SET")) {
                if (tokens.canConsume("DATA", "TYPE")) {
                    DataType datatype = getDatatypeParser().parse(tokens);

                    columnNode.setProperty(StandardDdlLexicon.DATATYPE_NAME, datatype.getName());
                    if (datatype.getLength() >= 0) {
                        columnNode.setProperty(StandardDdlLexicon.DATATYPE_LENGTH, datatype.getLength());
                    }
                    if (datatype.getPrecision() >= 0) {
                        columnNode.setProperty(StandardDdlLexicon.DATATYPE_PRECISION, datatype.getPrecision());
                    }
                    if (datatype.getScale() >= 0) {
                        columnNode.setProperty(StandardDdlLexicon.DATATYPE_SCALE, datatype.getScale());
                    }

                } else if (tokens.canConsume("INCREMENT")) {
                    tokens.consume("BY", DdlTokenStream.ANY_VALUE);
                }
                if (tokens.matches("DEFAULT")) {
                    parseDefaultClause(tokens, columnNode);
                }
            } else if (tokens.canConsume("WITH")) {
                parseDefaultClause(tokens, columnNode);
            } else {
                tokens.canConsume("RESTART", "WITH", DdlTokenStream.ANY_VALUE);
                tokens.canConsume("DROP", "DEFAULT");

                if (tokens.canConsume("NOT", "NULL")) {
                    columnNode.setProperty(StandardDdlLexicon.NULLABLE, "NOT NULL");
                } else if (tokens.canConsume("NULL")) {
                    columnNode.setProperty(StandardDdlLexicon.NULLABLE, "NULL");
                }
            }

        } else if (tokens.canConsume("LOCKSIZE")) {
            tokens.canConsume("ROWS");
            tokens.canConsume("TABLE");
        }

        markEndOfStatement(tokens, alterTableNode);

        return alterTableNode;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseColumnDefinition(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode, boolean)
     */
    @Override
    protected void parseColumnDefinition( DdlTokenStream tokens,
                                          AstNode tableNode,
                                          boolean isAlterTable ) throws ParsingException {
        // column-definition
        //
        // Simple-column-Name DataType
        // [ ColumnDefinition-level-constraint ]*
        // [ [ WITH ] DEFAULT { ConstantExpression | NULL } |generated-column-spec ]
        // [ ColumnDefinition-level-constraint ]*

        // generated-column-spec
        //
        // [ GENERATED { ALWAYS | BY DEFAULT } AS IDENTITY [ ( START WITH IntegerConstant [ ,INCREMENT BY IntegerConstant] ) ] ] ]

        // EXAMPLE COLUMNS
        // (i INT GENERATED BY DEFAULT AS IDENTITY (START WITH 2, INCREMENT BY 1),
        // ch CHAR(50));

        tokens.canConsume("COLUMN"); // FOR ALTER TABLE ADD [COLUMN] case
        String columnName = parseName(tokens);
        DataType datatype = getDatatypeParser().parse(tokens);

        AstNode columnNode = nodeFactory().node(columnName, tableNode, TYPE_COLUMN_DEFINITION);

        columnNode.setProperty(StandardDdlLexicon.DATATYPE_NAME, datatype.getName());
        if (datatype.getLength() >= 0) {
            columnNode.setProperty(StandardDdlLexicon.DATATYPE_LENGTH, datatype.getLength());
        }
        if (datatype.getPrecision() >= 0) {
            columnNode.setProperty(StandardDdlLexicon.DATATYPE_PRECISION, datatype.getPrecision());
        }
        if (datatype.getScale() >= 0) {
            columnNode.setProperty(StandardDdlLexicon.DATATYPE_SCALE, datatype.getScale());
        }

        // Now clauses and constraints can be defined in any order, so we need to keep parsing until we get to a comma
        // Now clauses and constraints can be defined in any order, so we need to keep parsing until we get to a comma
        StringBuffer unusedTokensSB = new StringBuffer();

        while (tokens.hasNext() && !tokens.matches(COMMA)) {
            boolean parsedDefaultClause = parseDefaultClause(tokens, columnNode);
            if (!parsedDefaultClause) {
                boolean parsedCollate = parseCollateClause(tokens, columnNode);
                boolean parsedConstraint = parseColumnConstraint(tokens, columnNode, isAlterTable);
                boolean parsedGeneratedColumn = parseGeneratedColumnSpecClause(tokens, columnNode);
                if (!parsedCollate && !parsedConstraint && !parsedGeneratedColumn) {
                    // THIS IS AN ERROR. NOTHING FOUND.
                    // NEED TO absorb tokens
                    unusedTokensSB.append(SPACE).append(tokens.consume());
                }
            }
            tokens.canConsume(DdlTokenizer.COMMENT);
        }

        if (unusedTokensSB.length() > 0) {
            String msg = DdlSequencerI18n.unusedTokensParsingColumnDefinition.text(tableNode.getProperty(StandardDdlLexicon.NAME));
            DdlParserProblem problem = new DdlParserProblem(Problems.WARNING, getCurrentMarkedPosition(), msg);
            problem.setUnusedSource(unusedTokensSB.toString());
            addProblem(problem, tableNode);
        }

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
        String tableElementString = getTableElementsString(tokens, false);

        DdlTokenStream localTokens = new DdlTokenStream(tableElementString, DdlTokenStream.ddlTokenizer(false), false);

        localTokens.start();

        StringBuffer unusedTokensSB = new StringBuffer();

        do {
            if (isColumnDefinitionStart(localTokens)) {
                parseColumnDefinition(localTokens, tableNode, isAlterTable);
            } else {
                // THIS IS AN ERROR. NOTHING FOUND.
                // NEED TO absorb tokens
                unusedTokensSB.append(SPACE).append(localTokens.consume());
            }
        } while (localTokens.canConsume(COMMA));

        if (unusedTokensSB.length() > 0) {
            String msg = DdlSequencerI18n.unusedTokensParsingColumnDefinition.text(tableNode.getProperty(StandardDdlLexicon.NAME));
            DdlParserProblem problem = new DdlParserProblem(Problems.WARNING, getCurrentMarkedPosition(), msg);
            problem.setUnusedSource(unusedTokensSB.toString());
            addProblem(problem, tableNode);
        }
    }

    private boolean parseGeneratedColumnSpecClause( DdlTokenStream tokens,
                                                    AstNode columnNode ) throws ParsingException {
        assert tokens != null;
        assert columnNode != null;
        // generated-column-spec
        //
        // [ GENERATED { ALWAYS | BY DEFAULT } AS IDENTITY [ ( START WITH IntegerConstant [ ,INCREMENT BY IntegerConstant] ) ] ] ]
        if (tokens.canConsume("GENERATED")) {
            StringBuffer sb = new StringBuffer("GENERATED");

            if (tokens.canConsume("ALWAYS")) {
                sb.append(SPACE).append("ALWAYS");
            } else {
                tokens.consume("BY", "DEFAULT");
                sb.append(SPACE).append("BY DEFAULT");
            }

            tokens.consume("AS", "IDENTITY");
            sb.append(SPACE).append("AS IDENTITY");

            if (tokens.canConsume(L_PAREN, "START", "WITH")) {
                String value = tokens.consume(); // integer constant
                sb.append(SPACE).append(L_PAREN).append(SPACE).append("START WITH").append(SPACE).append(value);
                if (tokens.canConsume(COMMA, "INCREMENT", "BY")) {
                    value = tokens.consume();// integer constant
                    sb.append(COMMA).append("INCREMENT BY").append(SPACE).append(value);
                }
                tokens.consume(R_PAREN);
                sb.append(SPACE).append(R_PAREN);
            }
            AstNode propNode = nodeFactory().node("GENERATED_COLUMN_SPEC", columnNode, COLUMN_ATTRIBUTE_TYPE);
            propNode.setProperty(PROPERTY_VALUE, sb.toString());

            return true;
        }

        return false;
    }

    private AstNode parseDeclareGlobalTempTable( DdlTokenStream tokens,
                                                 AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        markStartOfStatement(tokens);

        // DECLARE GLOBAL TEMPORARY TABLE table-Name
        // { column-definition [ , column-definition ] * }
        // [ ON COMMIT {DELETE | PRESERVE} ROWS ]
        // NOT LOGGED [ON ROLLBACK DELETE ROWS]

        tokens.consume(STMT_DECLARE_GLOBAL_TEMP_TABLE);
        String name = parseName(tokens);

        AstNode node = nodeFactory().node(name, parentNode, TYPE_DECLARE_GLOBAL_TEMPORARY_TABLE_STATEMENT);

        parseColumnsAndConstraints(tokens, node);

        if (tokens.canConsume("ON", "COMMIT")) {
            AstNode optionNode = nodeFactory().node("onCommit", node, TYPE_STATEMENT_OPTION);
            if (tokens.canConsume("DELETE", "ROWS")) {
                optionNode.setProperty(VALUE, "ON COMMIT DELETE ROWS");
            } else {
                tokens.consume("PRESERVE", "ROWS");
                optionNode.setProperty(VALUE, "ON COMMIT PRESERVE ROWS");
            }
        }
        tokens.consume("NOT", "LOGGED");

        if (tokens.canConsume("ON", "ROLLBACK", "DELETE", "ROWS")) {
            AstNode optionNode = nodeFactory().node("onRollback", node, TYPE_STATEMENT_OPTION);
            optionNode.setProperty(VALUE, "ON ROLLBACK DELETE ROWS");
        }

        markEndOfStatement(tokens, node);

        return node;
    }

    private AstNode parseLockTable( DdlTokenStream tokens,
                                    AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        markStartOfStatement(tokens);

        // LOCK TABLE table-Name IN { SHARE | EXCLUSIVE } MODE;

        tokens.consume(STMT_LOCK_TABLE);

        String name = parseName(tokens);

        AstNode node = nodeFactory().node(name, parentNode, TYPE_LOCK_TABLE_STATEMENT);

        tokens.consume("IN");

        if (tokens.canConsume("SHARE")) {
            AstNode propNode = nodeFactory().node("lockMode", node, TYPE_STATEMENT_OPTION);
            propNode.setProperty(VALUE, "SHARE");
        } else {
            tokens.consume("EXCLUSIVE");
            AstNode propNode = nodeFactory().node("lockMode", node, TYPE_STATEMENT_OPTION);
            propNode.setProperty(VALUE, "EXCLUSIVE");
        }
        tokens.consume("MODE");

        markEndOfStatement(tokens, node);

        return node;
    }

    private AstNode parseRenameTable( DdlTokenStream tokens,
                                      AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        markStartOfStatement(tokens);

        // RENAME TABLE SAMP.EMP_ACT TO EMPLOYEE_ACT;

        tokens.consume(STMT_RENAME_TABLE);

        String oldName = parseName(tokens);

        AstNode node = nodeFactory().node(oldName, parentNode, TYPE_RENAME_TABLE_STATEMENT);

        tokens.consume("TO");

        String newName = parseName(tokens);

        node.setProperty(NEW_NAME, newName);

        markEndOfStatement(tokens, node);

        return node;
    }

    private AstNode parseRenameIndex( DdlTokenStream tokens,
                                      AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        markStartOfStatement(tokens);

        // RENAME TABLE SAMP.EMP_ACT TO EMPLOYEE_ACT;

        tokens.consume(STMT_RENAME_INDEX);

        String oldName = parseName(tokens);

        AstNode node = nodeFactory().node(oldName, parentNode, TYPE_RENAME_INDEX_STATEMENT);

        tokens.consume("TO");

        String newName = parseName(tokens);

        node.setProperty(NEW_NAME, newName);

        markEndOfStatement(tokens, node);

        return node;
    }

    private AstNode parseCreateSynonym( DdlTokenStream tokens,
                                        AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        markStartOfStatement(tokens);
        // CREATE SYNONYM synonym-Name FOR { view-Name | table-Name }

        tokens.consume(STMT_CREATE_SYNONYM);

        String name = parseName(tokens);

        AstNode node = nodeFactory().node(name, parentNode, TYPE_CREATE_SYNONYM_STATEMENT);

        tokens.consume("FOR");

        String tableOrViewName = parseName(tokens);

        node.setProperty(TABLE_NAME, tableOrViewName);

        markEndOfStatement(tokens, node);

        return node;
    }

    private AstNode parseCreateTrigger( DdlTokenStream tokens,
                                        AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        markStartOfStatement(tokens);
        // CREATE TRIGGER TriggerName
        // { AFTER | NO CASCADE BEFORE }
        // { INSERT | DELETE | UPDATE [ OF column-Name [, column-Name]* ] }
        // ON table-Name
        // [ ReferencingClause ]
        // [ FOR EACH { ROW | STATEMENT } ] [ MODE DB2SQL ]
        // Triggered-SQL-statement

        // ReferencingClause
        // REFERENCING
        // {
        // { OLD | NEW } [ ROW ] [ AS ] correlation-Name [ { OLD | NEW } [ ROW ] [ AS ] correlation-Name ] |
        // { OLD TABLE | NEW TABLE } [ AS ] Identifier [ { OLD TABLE | NEW TABLE } [AS] Identifier ] |
        // { OLD_TABLE | NEW_TABLE } [ AS ] Identifier [ { OLD_TABLE | NEW_TABLE } [AS] Identifier ]
        // }

        // EXAMPLE:
        // CREATE TRIGGER t1 NO CASCADE BEFORE UPDATE ON x
        // FOR EACH ROW MODE DB2SQL
        // values app.notifyEmail('Jerry', 'Table x is about to be updated');

        tokens.consume(STMT_CREATE_TRIGGER);

        String name = parseName(tokens);

        AstNode node = nodeFactory().node(name, parentNode, TYPE_CREATE_TRIGGER_STATEMENT);

        String type = null;

        if (tokens.canConsume("AFTER")) {
            AstNode optionNode = nodeFactory().node("beforeOrAfter", node, TYPE_STATEMENT_OPTION);
            optionNode.setProperty(VALUE, "AFTER");
        } else {
            tokens.consume("NO", "CASCADE", "BEFORE");
            AstNode optionNode = nodeFactory().node("beforeOrAfter", node, TYPE_STATEMENT_OPTION);
            optionNode.setProperty(VALUE, "NO CASCADE BEFORE");
        }

        if (tokens.canConsume(INSERT)) {
            AstNode optionNode = nodeFactory().node("eventType", node, TYPE_STATEMENT_OPTION);
            optionNode.setProperty(VALUE, INSERT);
            type = INSERT;
        } else if (tokens.canConsume(DELETE)) {
            AstNode optionNode = nodeFactory().node("eventType", node, TYPE_STATEMENT_OPTION);
            optionNode.setProperty(VALUE, DELETE);
            type = DELETE;
        } else {
            tokens.consume(UPDATE);
            AstNode optionNode = nodeFactory().node("eventType", node, TYPE_STATEMENT_OPTION);
            optionNode.setProperty(VALUE, UPDATE);
            type = UPDATE;
        }

        if (tokens.canConsume("OF")) {
            // Parse comma separated column names
            String colName = parseName(tokens);
            nodeFactory().node(colName, node, TYPE_COLUMN_REFERENCE);

            while (tokens.canConsume(COMMA)) {
                colName = parseName(tokens);
                nodeFactory().node(colName, node, TYPE_COLUMN_REFERENCE);
            }
        }
        tokens.consume("ON");

        String tableName = parseName(tokens);

        node.setProperty(TABLE_NAME, tableName);

        if (tokens.canConsume("REFERENCING")) {
            // ReferencingClause
            // REFERENCING
            // {
            // { OLD | NEW } [ ROW ] [ AS ] correlation-Name [ { OLD | NEW } [ ROW ] [ AS ] correlation-Name ] |
            // { OLD TABLE | NEW TABLE } [ AS ] Identifier [ { OLD TABLE | NEW TABLE } [AS] Identifier ] |
            // { OLD_TABLE | NEW_TABLE } [ AS ] Identifier [ { OLD_TABLE | NEW_TABLE } [AS] Identifier ]
            // }

            StringBuffer sb = new StringBuffer();
            if (tokens.matchesAnyOf("OLD", "NEW")) {
                if (tokens.canConsume("OLD")) {
                    sb.append("OLD");
                } else {
                    tokens.consume("NEW");
                    sb.append("NEW");
                }
                if (tokens.canConsume("ROW")) {
                    sb.append(SPACE).append("ROW");
                }
                if (tokens.canConsume("AS")) {
                    sb.append(SPACE).append("AS");
                }
                if (tokens.matchesAnyOf("OLD", "NEW")) {
                    if (tokens.canConsume("OLD")) {
                        sb.append(SPACE).append("OLD");
                    } else {
                        tokens.consume("NEW");
                        sb.append(SPACE).append("NEW");
                    }

                    if (tokens.canConsume("ROW")) {
                        sb.append(SPACE).append("ROW");
                    }
                    if (tokens.canConsume("AS")) {
                        sb.append(SPACE).append("AS");
                    }
                    if (!tokens.matchesAnyOf("FOR", "MODE", type)) {
                        String corrName = parseName(tokens);
                        sb.append(SPACE).append(corrName);
                    }
                } else {
                    String corrName = parseName(tokens);
                    sb.append(SPACE).append(corrName);

                    if (tokens.matchesAnyOf("OLD", "NEW")) {
                        if (tokens.canConsume("OLD")) {
                            sb.append(SPACE).append("OLD");
                        } else {
                            tokens.consume("NEW");
                            sb.append(SPACE).append("NEW");
                        }

                        if (tokens.canConsume("ROW")) {
                            sb.append(SPACE).append("ROW");
                        }
                        if (tokens.canConsume("AS")) {
                            sb.append(SPACE).append("AS");
                        }
                        if (!tokens.matchesAnyOf("FOR", "MODE", type)) {
                            corrName = parseName(tokens);
                            sb.append(SPACE).append(corrName);
                        }
                    }
                }
            }
        }
        // [ FOR EACH { ROW | STATEMENT } ] [ MODE DB2SQL ]
        if (tokens.canConsume("FOR", "EACH")) {
            if (tokens.canConsume("ROW")) {
                AstNode optionNode = nodeFactory().node("forEach", node, TYPE_STATEMENT_OPTION);
                optionNode.setProperty(VALUE, "FOR EACH ROW");
            } else {
                tokens.consume("STATEMENT");
                AstNode optionNode = nodeFactory().node("forEach", node, TYPE_STATEMENT_OPTION);
                optionNode.setProperty(VALUE, "FOR EACH STATEMENT");
            }
        }
        if (tokens.canConsume("MODE")) {
            tokens.consume("DB2SQL");
            AstNode optionNode = nodeFactory().node("mode", node, TYPE_STATEMENT_OPTION);
            optionNode.setProperty(VALUE, "MODE DB2SQL");
        }

        String sql = parseUntilTerminatorIgnoreEmbeddedStatements(tokens);
        node.setProperty(SQL, sql);

        markEndOfStatement(tokens, node);

        return node;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#getDataTypeStartWords()
     */
    @Override
    protected List<String> getCustomDataTypeStartWords() {
        return DerbyDataTypes.CUSTOM_DATATYPE_START_WORDS;
    }

    class DerbyDataTypeParser extends DataTypeParser {

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.sequencer.ddl.datatype.DataTypeParser#isCustomDataType(org.modeshape.sequencer.ddl.DdlTokenStream)
         */
        @Override
        protected boolean isCustomDataType( DdlTokenStream tokens ) throws ParsingException {
            // Loop through the registered statement start string arrays and look for exact matches.

            for (String[] stmts : derbyDataTypeStrings) {
                if (tokens.matches(stmts)) return true;
            }
            return super.isCustomDataType(tokens);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.sequencer.ddl.datatype.DataTypeParser#parseApproxNumericType(org.modeshape.sequencer.ddl.DdlTokenStream)
         */
        @Override
        protected DataType parseApproxNumericType( DdlTokenStream tokens ) throws ParsingException {
            return super.parseApproxNumericType(tokens);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.sequencer.ddl.datatype.DataTypeParser#parseBitStringType(org.modeshape.sequencer.ddl.DdlTokenStream)
         */
        @Override
        protected DataType parseBitStringType( DdlTokenStream tokens ) throws ParsingException {
            return super.parseBitStringType(tokens);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.sequencer.ddl.datatype.DataTypeParser#parseCharStringType(org.modeshape.sequencer.ddl.DdlTokenStream)
         */
        @Override
        protected DataType parseCharStringType( DdlTokenStream tokens ) throws ParsingException {
            DataType result = super.parseCharStringType(tokens);

            canConsume(tokens, result, true, "FOR", "BIT", "DATA");

            return result;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.sequencer.ddl.datatype.DataTypeParser#parseCustomType(org.modeshape.sequencer.ddl.DdlTokenStream)
         */
        @Override
        protected DataType parseCustomType( DdlTokenStream tokens ) throws ParsingException {
            DataType dataType = null;
            String typeName = null;
            long length = 0;

            if (tokens.matches(DerbyDataTypes.DTYPE_BINARY_LARGE_OBJECT)
                || tokens.matches(DerbyDataTypes.DTYPE_CHARACTER_LARGE_OBJECT)) {
                dataType = new DataType();
                typeName = consume(tokens, dataType, true) + SPACE + consume(tokens, dataType, true) + SPACE
                           + consume(tokens, dataType, true);
                if (canConsume(tokens, dataType, true, L_PAREN)) {
                    String lengthValue = consume(tokens, dataType, false);
                    length = parseLong(lengthValue);
                    consume(tokens, dataType, true, R_PAREN);
                }
                dataType.setName(typeName);
                dataType.setLength(length);
            } else if (tokens.matches(DerbyDataTypes.DTYPE_CLOB) || tokens.matches(DerbyDataTypes.DTYPE_BLOB)) {
                dataType = new DataType();
                typeName = consume(tokens, dataType, true);
                if (canConsume(tokens, dataType, true, L_PAREN)) {
                    String lengthValue = consume(tokens, dataType, false);
                    length = parseLong(lengthValue);
                    consume(tokens, dataType, true, R_PAREN);
                }
                dataType.setName(typeName);
                dataType.setLength(length);
            } else if (tokens.matches(DerbyDataTypes.DTYPE_BIGINT)) {
                dataType = new DataType();
                typeName = consume(tokens, dataType, true);
                dataType.setName(typeName);
            } else if (tokens.matches(DerbyDataTypes.DTYPE_LONG_VARCHAR_FBD)) {
                dataType = new DataType();
                typeName = consume(tokens, dataType, true) + SPACE + consume(tokens, dataType, true) + SPACE
                           + consume(tokens, dataType, true) + SPACE + consume(tokens, dataType, true) + SPACE
                           + consume(tokens, dataType, true);
                dataType.setName(typeName);
            } else if (tokens.matches(DerbyDataTypes.DTYPE_LONG_VARCHAR)) {
                dataType = new DataType();
                typeName = consume(tokens, dataType, true) + SPACE + consume(tokens, dataType, true);
                typeName = consume(tokens, dataType, true);
                dataType.setName(typeName);
            } else if (tokens.matches(DerbyDataTypes.DTYPE_DOUBLE)) {
                dataType = new DataType();
                typeName = consume(tokens, dataType, true);
                dataType.setName(typeName);
            } else if (tokens.matches(DerbyDataTypes.DTYPE_XML)) {
                dataType = new DataType();
                typeName = consume(tokens, dataType, true);
                dataType.setName(typeName);
            }

            if (dataType == null) {
                super.parseCustomType(tokens);
            }
            return dataType;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.sequencer.ddl.datatype.DataTypeParser#parseDateTimeType(org.modeshape.sequencer.ddl.DdlTokenStream)
         */
        @Override
        protected DataType parseDateTimeType( DdlTokenStream tokens ) throws ParsingException {
            return super.parseDateTimeType(tokens);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.sequencer.ddl.datatype.DataTypeParser#parseExactNumericType(org.modeshape.sequencer.ddl.DdlTokenStream)
         */
        @Override
        protected DataType parseExactNumericType( DdlTokenStream tokens ) throws ParsingException {
            return super.parseExactNumericType(tokens);
        }

    }

}
