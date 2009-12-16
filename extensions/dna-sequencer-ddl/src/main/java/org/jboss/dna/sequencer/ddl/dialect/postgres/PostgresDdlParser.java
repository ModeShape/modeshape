package org.jboss.dna.sequencer.ddl.dialect.postgres;

import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.DDL_EXPRESSION;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.DDL_ORIGINAL_EXPRESSION;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.DDL_START_CHAR_INDEX;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.DDL_START_COLUMN_NUMBER;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.DDL_START_LINE_NUMBER;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.DEFAULT_OPTION;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.DEFAULT_PRECISION;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.DEFAULT_VALUE;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.DROP_BEHAVIOR;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.NEW_NAME;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_ALTER_COLUMN_DEFINITION;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_COLUMN_DEFINITION;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_CREATE_TABLE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_COLUMN_DEFINITION;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_DOMAIN_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_SCHEMA_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_TABLE_CONSTRAINT_DEFINITION;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_TABLE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_VIEW_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_MISSING_TERMINATOR;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_STATEMENT_OPTION;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_UNKNOWN_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.VALUE;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.SCHEMA_NAME;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ABORT_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ALTER_AGGREGATE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ALTER_CONVERSION_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ALTER_DATABASE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ALTER_FOREIGN_DATA_WRAPPER_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ALTER_FUNCTION_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ALTER_GROUP_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ALTER_INDEX_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ALTER_LANGUAGE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ALTER_OPERATOR_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ALTER_ROLE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ALTER_SCHEMA_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ALTER_SEQUENCE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ALTER_SERVER_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ALTER_TABLESPACE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ALTER_TABLE_STATEMENT_POSTGRES;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ALTER_TEXT_SEARCH_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ALTER_TRIGGER_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ALTER_TYPE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ALTER_USER_MAPPING_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ALTER_USER_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ALTER_VIEW_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ANALYZE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CLUSTER_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_COMMENT_ON_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_COPY_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CREATE_AGGREGATE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CREATE_CAST_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CREATE_CONSTRAINT_TRIGGER_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CREATE_CONVERSION_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CREATE_DATABASE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CREATE_FOREIGN_DATA_WRAPPER_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CREATE_FUNCTION_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CREATE_GROUP_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CREATE_INDEX_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CREATE_LANGUAGE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CREATE_OPERATOR_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CREATE_ROLE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CREATE_RULE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CREATE_SEQUENCE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CREATE_SERVER_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CREATE_TABLESPACE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CREATE_TEXT_SEARCH_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CREATE_TRIGGER_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CREATE_TYPE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CREATE_USER_MAPPING_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CREATE_USER_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_DEALLOCATE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_DECLARE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_DROP_AGGREGATE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_DROP_CAST_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_DROP_CONSTRAINT_TRIGGER_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_DROP_CONVERSION_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_DROP_DATABASE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_DROP_FOREIGN_DATA_WRAPPER_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_DROP_FUNCTION_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_DROP_GROUP_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_DROP_INDEX_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_DROP_LANGUAGE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_DROP_OPERATOR_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_DROP_OWNED_BY_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_DROP_ROLE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_DROP_RULE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_DROP_SEQUENCE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_DROP_SERVER_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_DROP_TABLESPACE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_DROP_TEXT_SEARCH_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_DROP_TRIGGER_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_DROP_TYPE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_DROP_USER_MAPPING_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_DROP_USER_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_EXPLAIN_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_FETCH_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_LISTEN_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_LOAD_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_LOCK_TABLE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_MOVE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_NOTIFY_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_PREPARE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_REASSIGN_OWNED_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_REINDEX_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_RELEASE_SAVEPOINT_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_RENAME_COLUMN;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ROLLBACK_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_SELECT_INTO_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_SHOW_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_TRUNCATE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_UNLISTEN_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_VACUUM_STATEMENT;
import java.util.ArrayList;
import java.util.List;
import org.jboss.dna.common.text.ParsingException;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.sequencer.ddl.DdlParserProblem;
import org.jboss.dna.sequencer.ddl.DdlSequencerI18n;
import org.jboss.dna.sequencer.ddl.DdlTokenStream;
import org.jboss.dna.sequencer.ddl.StandardDdlLexicon;
import org.jboss.dna.sequencer.ddl.StandardDdlParser;
import org.jboss.dna.sequencer.ddl.DdlTokenStream.DdlTokenizer;
import org.jboss.dna.sequencer.ddl.datatype.DataType;
import org.jboss.dna.sequencer.ddl.datatype.DataTypeParser;
import org.jboss.dna.sequencer.ddl.node.AstNode;

/**
 * Postgres-specific DDL Parser. Includes custom data types as well as custom DDL statements.
 */
public class PostgresDdlParser extends StandardDdlParser
    implements PostgresDdlConstants, PostgresDdlConstants.PostgresStatementStartPhrases {
    private final String parserId = "POSTGRES";

    static List<String[]> postgresDataTypeStrings = new ArrayList<String[]>();

    // SQL COMMANDS FOUND @ http://www.postgresql.org/docs/8.4/static/sql-commands.html

    private static final String TERMINATOR = ";";

    public PostgresDdlParser() {
        setDatatypeParser(new PostgresDataTypeParser());
        initialize();
    }

    private void initialize() {

        setDoUseTerminator(true);

        setTerminator(TERMINATOR);

        postgresDataTypeStrings.addAll(PostgresDataTypes.CUSTOM_DATATYPE_START_PHRASES);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.sequencer.ddl.StandardDdlParser#getId()
     */
    @Override
    public String getId() {
        return this.parserId;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.sequencer.ddl.StandardDdlParser#registerWords(org.jboss.dna.sequencer.ddl.DdlTokenStream)
     */
    @Override
    public void registerWords( DdlTokenStream tokens ) {
        super.registerWords(tokens);

        registerKeyWords(CUSTOM_KEYWORDS);
        registerKeyWords(PostgresDataTypes.CUSTOM_DATATYPE_START_WORDS);

        registerStatementStartPhrase(ALTER_PHRASES);
        registerStatementStartPhrase(CREATE_PHRASES);
        registerStatementStartPhrase(DROP_PHRASES);
        registerStatementStartPhrase(SET_PHRASES);
        registerStatementStartPhrase(MISC_PHRASES);

    }

    @Override
    protected void rewrite( DdlTokenStream tokens,
                            AstNode rootNode ) {
        assert tokens != null;
        assert rootNode != null;

        // We may hava a prepare statement that is followed by a missing terminator node

        List<AstNode> copyOfNodes = new ArrayList<AstNode>(rootNode.getChildren());
        AstNode prepareNode = null;
        boolean mergeNextStatement = false;
        for (AstNode child : copyOfNodes) {
            if (prepareNode != null && mergeNextStatement) {
                mergeNodes(tokens, prepareNode, child);
                rootNode.removeChild(child);
                prepareNode = null;
            }
            if (prepareNode != null && nodeFactory().hasMixinType(child, TYPE_MISSING_TERMINATOR)) {
                mergeNextStatement = true;
            } else {
                mergeNextStatement = false;
            }
            if (nodeFactory().hasMixinType(child, TYPE_PREPARE_STATEMENT)) {
                prepareNode = child;
            }
        }

        super.rewrite(tokens, rootNode); // Removes all extra "missing terminator" nodes

        // Now we need to walk the tree again looking for unknown nodes under the root
        // and attach them to the previous node, assuming the node can contain multiple nested statements.
        // CREATE FUNCTION is one of those types

        copyOfNodes = new ArrayList<AstNode>(rootNode.getChildren());
        boolean foundComplexNode = false;
        AstNode complexNode = null;
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
        for (Name mixin : COMPLEX_STMT_TYPES) {
            if (nodeFactory().hasMixinType(node, mixin)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected AstNode parseAlterStatement( DdlTokenStream tokens,
                                           AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        if (tokens.matches(STMT_ALTER_AGGREGATE)) {
            return parseStatement(tokens, STMT_ALTER_AGGREGATE, parentNode, TYPE_ALTER_AGGREGATE_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_CONVERSION)) {
            return parseStatement(tokens, STMT_ALTER_CONVERSION, parentNode, TYPE_ALTER_CONVERSION_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_DATABASE)) {
            return parseStatement(tokens, STMT_ALTER_DATABASE, parentNode, TYPE_ALTER_DATABASE_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_FOREIGN_DATA_WRAPPER)) {
            return parseStatement(tokens, STMT_ALTER_FOREIGN_DATA_WRAPPER, parentNode, TYPE_ALTER_FOREIGN_DATA_WRAPPER_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_FUNCTION)) {
            return parseStatement(tokens, STMT_ALTER_FUNCTION, parentNode, TYPE_ALTER_FUNCTION_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_GROUP)) {
            return parseStatement(tokens, STMT_ALTER_GROUP, parentNode, TYPE_ALTER_GROUP_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_INDEX)) {
            return parseStatement(tokens, STMT_ALTER_INDEX, parentNode, TYPE_ALTER_INDEX_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_LANGUAGE)) {
            return parseStatement(tokens, STMT_ALTER_LANGUAGE, parentNode, TYPE_ALTER_LANGUAGE_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_OPERATOR)) {
            return parseStatement(tokens, STMT_ALTER_OPERATOR, parentNode, TYPE_ALTER_OPERATOR_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_ROLE)) {
            return parseStatement(tokens, STMT_ALTER_ROLE, parentNode, TYPE_ALTER_ROLE_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_SCHEMA)) {
            return parseStatement(tokens, STMT_ALTER_SCHEMA, parentNode, TYPE_ALTER_SCHEMA_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_SEQUENCE)) {
            return parseStatement(tokens, STMT_ALTER_SEQUENCE, parentNode, TYPE_ALTER_SEQUENCE_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_SERVER)) {
            return parseStatement(tokens, STMT_ALTER_SERVER, parentNode, TYPE_ALTER_SERVER_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_TABLESPACE)) {
            return parseStatement(tokens, STMT_ALTER_TABLESPACE, parentNode, TYPE_ALTER_TABLESPACE_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_TEXT_SEARCH)) {
            return parseStatement(tokens, STMT_ALTER_TEXT_SEARCH, parentNode, TYPE_ALTER_TEXT_SEARCH_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_TRIGGER)) {
            return parseStatement(tokens, STMT_ALTER_TRIGGER, parentNode, TYPE_ALTER_TRIGGER_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_TYPE)) {
            return parseStatement(tokens, STMT_ALTER_TYPE, parentNode, TYPE_ALTER_TYPE_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_USER_MAPPING)) {
            return parseStatement(tokens, STMT_ALTER_USER_MAPPING, parentNode, TYPE_ALTER_USER_MAPPING_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_USER)) {
            return parseStatement(tokens, STMT_ALTER_USER, parentNode, TYPE_ALTER_USER_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_VIEW)) {
            return parseStatement(tokens, STMT_ALTER_VIEW, parentNode, TYPE_ALTER_VIEW_STATEMENT);
        }

        return super.parseAlterStatement(tokens, parentNode);

    }

    @Override
    protected AstNode parseAlterTableStatement( DdlTokenStream tokens,
                                                AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        markStartOfStatement(tokens);
        // TODO: Need to flesh out and store more info on alterTableStatement properties

        // NOTE: Not sure the rules of Postgress here. It appears that you can have comma separated clauses
        // but can't find many examples. Also don't know if you can mix clause types
        // EXAMPLE:
        //
        // ALTER TABLE distributors
        // ALTER COLUMN address TYPE varchar(80),
        // DROP COLUMN name RESTRICTED;

        // --ALTER TABLE [ ONLY ] name [ * ]
        // -- action [, ... ]

        // --where action is one of:
        // -- ADD [ COLUMN ] column type [ column_constraint [ ... ] ]
        // -- DROP [ COLUMN ] column [ RESTRICT | CASCADE ]
        // -- ALTER [ COLUMN ] column [ SET DATA ] TYPE type [ USING expression ]
        // -- ALTER [ COLUMN ] column SET DEFAULT expression
        // -- ALTER [ COLUMN ] column DROP DEFAULT
        // -- ALTER [ COLUMN ] column { SET | DROP } NOT NULL
        // -- ALTER [ COLUMN ] column SET STATISTICS integer
        // -- ALTER [ COLUMN ] column SET STORAGE { PLAIN | EXTERNAL | EXTENDED | MAIN }
        // -- ADD table_constraint
        // -- DROP CONSTRAINT constraint_name [ RESTRICT | CASCADE ]
        // -- DISABLE TRIGGER [ trigger_name | ALL | USER ]
        // -- ENABLE TRIGGER [ trigger_name | ALL | USER ]
        // -- ENABLE REPLICA TRIGGER trigger_name
        // -- ENABLE ALWAYS TRIGGER trigger_name
        // -- DISABLE RULE rewrite_rule_name
        // -- ENABLE RULE rewrite_rule_name
        // -- ENABLE REPLICA RULE rewrite_rule_name
        // -- ENABLE ALWAYS RULE rewrite_rule_name
        // -- CLUSTER ON index_name
        // -- SET WITHOUT CLUSTER
        // -- SET WITH OIDS
        // -- SET WITHOUT OIDS
        // -- SET ( storage_parameter = value [, ... ] )
        // -- RESET ( storage_parameter [, ... ] )
        // -- INHERIT parent_table
        // -- NO INHERIT parent_table
        // -- OWNER TO new_owner
        // -- SET TABLESPACE new_tablespace
        // =========== MISC.............
        // --ALTER TABLE [ ONLY ] name [ * ]
        // -- RENAME [ COLUMN ] column TO new_column
        // --ALTER TABLE name
        // -- RENAME TO new_name
        // --ALTER TABLE name new_tablespace
        // -- SET SCHEMA new_schema

        tokens.consume(); // consumes 'ALTER'
        tokens.consume("TABLE");

        tokens.canConsume("ONLY");
        String tableName = parseName(tokens);
        tokens.canConsume("*");

        // System.out.println("  >> PARSING ALTER STATEMENT >>  TABLE Name = " + tableName);
        AstNode alterTableNode = nodeFactory().node(tableName, parentNode, TYPE_ALTER_TABLE_STATEMENT_POSTGRES);

        do {
            parseAlterTableAction(tokens, alterTableNode);
        } while (tokens.canConsume(COMMA));

        markEndOfStatement(tokens, alterTableNode);

        return alterTableNode;
    }

    private void parseAlterTableAction( DdlTokenStream tokens,
                                        AstNode alterTableNode ) throws ParsingException {
        assert tokens != null;
        assert alterTableNode != null;

        if (tokens.canConsume("ADD")) { // ADD COLUMN
            if (isTableConstraint(tokens)) {
                parseTableConstraint(tokens, alterTableNode, true);
            } else {
                parseSingleCommaTerminatedColumnDefinition(tokens, alterTableNode, true);
            }

        } else if (tokens.canConsume("DROP")) { // DROP CONSTRAINT & DROP COLUMN
            if (tokens.canConsume("CONSTRAINT")) {
                String constraintName = parseName(tokens); // constraint name

                AstNode constraintNode = nodeFactory().node(constraintName, alterTableNode, TYPE_DROP_TABLE_CONSTRAINT_DEFINITION);

                if (tokens.canConsume(DropBehavior.CASCADE)) {
                    constraintNode.setProperty(DROP_BEHAVIOR, DropBehavior.CASCADE);
                } else if (tokens.canConsume(DropBehavior.RESTRICT)) {
                    constraintNode.setProperty(DROP_BEHAVIOR, DropBehavior.RESTRICT);
                }
            } else {
                // ALTER TABLE supplier
                // DROP COLUMN supplier_name;
                tokens.canConsume("COLUMN"); // "COLUMN" is optional

                String columnName = parseName(tokens);

                AstNode columnNode = nodeFactory().node(columnName, alterTableNode, TYPE_DROP_COLUMN_DEFINITION);

                if (tokens.canConsume(DropBehavior.CASCADE)) {
                    columnNode.setProperty(DROP_BEHAVIOR, DropBehavior.CASCADE);
                } else if (tokens.canConsume(DropBehavior.RESTRICT)) {
                    columnNode.setProperty(DROP_BEHAVIOR, DropBehavior.RESTRICT);
                }
            }
        } else if (tokens.matches("ALTER")) {
            // -- ALTER [ COLUMN ] column SET STORAGE { PLAIN | EXTERNAL | EXTENDED | MAIN }
            // -- ALTER [ COLUMN ] columnnew_tablespace SET STATISTICS integer
            // -- ALTER [ COLUMN ] column DROP DEFAULT
            // -- ALTER [ COLUMN ] column [ SET DATA ] TYPE type [ USING expression ]
            // -- ALTER [ COLUMN ] column SET DEFAULT expression
            // -- ALTER [ COLUMN ] column { SET | DROP } NOT NULL

            tokens.consume("ALTER");
            tokens.canConsume("COLUMN");
            String columnName = parseName(tokens);

            AstNode columnNode = nodeFactory().node(columnName, alterTableNode, TYPE_ALTER_COLUMN_DEFINITION);

            if (tokens.canConsume("SET", "STORAGE")) {
                tokens.consume(); // { PLAIN | EXTERNAL | EXTENDED | MAIN }
            } else if (tokens.canConsume("SET", "STATISTICS")) {
                tokens.consume(); // integer
            } else if (tokens.canConsume("DROP", "DEFAULT")) {

            } else if (tokens.canConsume("SET", "DATA")) {
                tokens.consume("TYPE");
                DataType datatype = getDatatypeParser().parse(tokens);

                getDatatypeParser().setPropertiesOnNode(columnNode, datatype);

                if (tokens.canConsume("USING")) {
                    // TODO: Not storing the following expression in properties.
                    parseUntilCommaOrTerminator(tokens);
                }
            } else if (tokens.canConsume("TYPE")) {
                DataType datatype = getDatatypeParser().parse(tokens);

                getDatatypeParser().setPropertiesOnNode(columnNode, datatype);

                if (tokens.canConsume("USING")) {
                    // TODO: Not storing the following expression in properties.
                    parseUntilCommaOrTerminator(tokens);
                }
            } else if (tokens.matches("SET", "DEFAULT")) {
                tokens.consume("SET");
                parseDefaultClause(tokens, columnNode);
            } else if (tokens.matches("SET") || tokens.matches("DROP")) {
                tokens.consume(); // { SET | DROP }
                tokens.canConsume("NOT", "NULL");
                tokens.canConsume("NULL");
            } else {
                System.out.println("  WARNING:  Option not found for ALTER TABLE - ALTER COLUMN. Check your DDL for incomplete statement.");
            }

        } else if (tokens.canConsume("ENABLE")) {
            AstNode optionNode = nodeFactory().node("action", alterTableNode, TYPE_STATEMENT_OPTION);
            StringBuffer sb = new StringBuffer("ENABLE");
            // -- ENABLE TRIGGER [ trigger_name | ALL | USER ]
            // -- ENABLE REPLICA TRIGGER trigger_name
            // -- ENABLE REPLICA RULE rewrite_rule_name
            // -- ENABLE ALWAYS TRIGGER trigger_name
            // -- ENABLE ALWAYS RULE rewrite_rule_name
            // -- ENABLE RULE rewrite_rule_name
            if (tokens.canConsume("TRIGGER")) {
                sb.append(SPACE).append("TRIGGER");
                if (!tokens.matches(getTerminator())) {
                    sb.append(SPACE).append(parseName(tokens)); // [ trigger_name | ALL | USER ]
                }
            } else if (tokens.canConsume("REPLICA", "TRIGGER")) {
                sb.append(SPACE).append("REPLICA TRIGGER");
                sb.append(SPACE).append(parseName(tokens)); // trigger_name
            } else if (tokens.canConsume("REPLICA", "RULE")) {
                sb.append(SPACE).append("REPLICA RULE");
                sb.append(SPACE).append(parseName(tokens)); // rewrite_rule_name
            } else if (tokens.canConsume("ALWAYS", "TRIGGER")) {
                sb.append(SPACE).append("ALWAYS TRIGGER");
                sb.append(SPACE).append(parseName(tokens)); // trigger_name
            } else if (tokens.canConsume("ALWAYS", "RULE")) {
                sb.append(SPACE).append("ALWAYS RULE");
                sb.append(SPACE).append(parseName(tokens)); // rewrite_rule_name
            } else if (tokens.canConsume("RULE")) {
                sb.append(SPACE).append("RULE");
                sb.append(SPACE).append(parseName(tokens)); // rewrite_rule_name
            } else {
                System.out.println("  WARNING:  Option not found for ALTER TABLE - ENABLE XXXX. Check your DDL for incomplete statement.");
            }
            optionNode.setProperty(VALUE, sb.toString());
        } else if (tokens.canConsume("DISABLE")) {
            AstNode optionNode = nodeFactory().node("action", alterTableNode, TYPE_STATEMENT_OPTION);
            StringBuffer sb = new StringBuffer("DISABLE");
            // -- DISABLE TRIGGER [ trigger_name | ALL | USER ]
            // -- DISABLE RULE rewrite_rule_name
            if (tokens.canConsume("TRIGGER")) {
                sb.append(SPACE).append("TRIGGER");
                if (!tokens.matches(getTerminator())) {
                    sb.append(SPACE).append(parseName(tokens)); // [ trigger_name | ALL | USER ]
                }
            } else if (tokens.canConsume("RULE")) {
                sb.append(SPACE).append("RULE");
                sb.append(SPACE).append(parseName(tokens)); // rewrite_rule_name
            } else {
                System.out.println("  WARNING:  Option not found for ALTER TABLE - DISABLE XXXX. Check your DDL for incomplete statement.");
            }
            optionNode.setProperty(VALUE, sb.toString());
        } else if (tokens.canConsume("CLUSTER", "ON")) {
            AstNode optionNode = nodeFactory().node("action", alterTableNode, TYPE_STATEMENT_OPTION);
            // -- CLUSTER ON index_name
            String indexName = parseName(tokens); // index_name
            optionNode.setProperty(VALUE, "CLUSTER ON" + SPACE + indexName);
        } else if (tokens.canConsume("OWNER", "TO")) {
            AstNode optionNode = nodeFactory().node("action", alterTableNode, TYPE_STATEMENT_OPTION);
            // -- OWNER TO new_owner
            optionNode.setProperty(VALUE, "OWNER TO" + SPACE + parseName(tokens));
        } else if (tokens.canConsume("INHERIT")) {
            AstNode optionNode = nodeFactory().node("action", alterTableNode, TYPE_STATEMENT_OPTION);
            // -- INHERIT parent_table
            optionNode.setProperty(VALUE, "INHERIT" + SPACE + parseName(tokens));
        } else if (tokens.canConsume("NO", "INHERIT")) {
            AstNode optionNode = nodeFactory().node("action", alterTableNode, TYPE_STATEMENT_OPTION);
            // -- NO INHERIT parent_table
            optionNode.setProperty(VALUE, "NO INHERIT" + SPACE + parseName(tokens));
        } else if (tokens.canConsume("SET", "TABLESPACE")) {
            AstNode optionNode = nodeFactory().node("action", alterTableNode, TYPE_STATEMENT_OPTION);
            // -- SET TABLESPACE new_tablespace
            optionNode.setProperty(VALUE, "SET TABLESPACE" + SPACE + parseName(tokens));
        } else if (tokens.canConsume("SET", "WITHOUT", "CLUSTER")) {
            AstNode optionNode = nodeFactory().node("action", alterTableNode, TYPE_STATEMENT_OPTION);
            optionNode.setProperty(VALUE, "SET WITHOUT CLUSTER");
        } else if (tokens.canConsume("SET", "WITHOUT", "OIDS")) {
            AstNode optionNode = nodeFactory().node("action", alterTableNode, TYPE_STATEMENT_OPTION);
            optionNode.setProperty(VALUE, "SET WITHOUT OIDS");
        } else if (tokens.canConsume("SET", "WITH", "OIDS")) {
            AstNode optionNode = nodeFactory().node("action", alterTableNode, TYPE_STATEMENT_OPTION);
            optionNode.setProperty(VALUE, "SET WITH OIDS");
        } else if (tokens.canConsume("RENAME", "TO")) {
            // --ALTER TABLE name
            // -- RENAME TO new_name
            String newTableName = parseName(tokens);
            alterTableNode.setProperty(NEW_NAME, newTableName);

        } else if (tokens.canConsume("RENAME")) {
            // --ALTER TABLE [ ONLY ] name [ * ]
            // -- RENAME [ COLUMN ] column TO new_column
            tokens.canConsume("COLUMN");
            String oldColumnName = parseName(tokens); // OLD COLUMN NAME
            tokens.consume("TO");
            String newColumnName = parseName(tokens); // NEW COLUMN NAME
            AstNode renameColumnNode = nodeFactory().node(oldColumnName, alterTableNode, TYPE_RENAME_COLUMN);
            renameColumnNode.setProperty(NEW_NAME, newColumnName);
        } else if (tokens.canConsume("SET", "SCHEMA")) {
            // ALTER TABLE myschema.distributors SET SCHEMA your schema;
            String schemaName = parseName(tokens);
            alterTableNode.setProperty(SCHEMA_NAME, schemaName);
        } else {
            System.out.println("  WARNING:  Option not found for ALTER TABLE. Check your DDL for incomplete statement.");
        }
    }

    private void parseSingleCommaTerminatedColumnDefinition( DdlTokenStream tokens,
                                                             AstNode tableNode,
                                                             boolean isAlterTable ) throws ParsingException {
        assert tokens != null;
        assert tableNode != null;

        tokens.canConsume("COLUMN");
        String columnName = parseName(tokens);
        DataType datatype = getDatatypeParser().parse(tokens);

        AstNode columnNode = nodeFactory().node(columnName, tableNode, TYPE_COLUMN_DEFINITION);

        getDatatypeParser().setPropertiesOnNode(columnNode, datatype);
        // Now clauses and constraints can be defined in any order, so we need to keep parsing until we get to a comma, a
        // terminator
        // or a new statement

        while (tokens.hasNext() && !tokens.matches(getTerminator()) && !tokens.matches(DdlTokenizer.STATEMENT_KEY)) {
            boolean parsedDefaultClause = parseDefaultClause(tokens, columnNode);
            if (!parsedDefaultClause) {
                parseCollateClause(tokens, columnNode);
                parseColumnConstraint(tokens, columnNode, isAlterTable);
            }
            consumeComment(tokens);
            if (tokens.matches(COMMA)) {
                break;
            }
        }
    }

    /**
     * Currently, only CREATE TABLE, CREATE VIEW, CREATE INDEX, CREATE SEQUENCE, CREATE TRIGGER and GRANT are accepted as clauses
     * within CREATE SCHEMA. {@inheritDoc}
     * 
     * @see org.jboss.dna.sequencer.ddl.StandardDdlParser#parseCreateSchemaStatement(org.jboss.dna.sequencer.ddl.DdlTokenStream,
     *      org.jboss.dna.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseCreateSchemaStatement( DdlTokenStream tokens,
                                                  AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        return super.parseCreateSchemaStatement(tokens, parentNode);
    }

    @Override
    protected AstNode parseCreateStatement( DdlTokenStream tokens,
                                            AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        if (tokens.matches(STMT_CREATE_TEMP_TABLE) || tokens.matches(STMT_CREATE_GLOBAL_TEMP_TABLE)
            || tokens.matches(STMT_CREATE_LOCAL_TEMP_TABLE)) {
            return parseCreateTableStatement(tokens, parentNode);
        } else if (tokens.matches(STMT_CREATE_AGGREGATE)) {
            return parseStatement(tokens, STMT_CREATE_AGGREGATE, parentNode, TYPE_CREATE_AGGREGATE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_CAST)) {
            return parseStatement(tokens, STMT_CREATE_CAST, parentNode, TYPE_CREATE_CAST_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_CONSTRAINT_TRIGGER)) {
            return parseStatement(tokens, STMT_CREATE_CONSTRAINT_TRIGGER, parentNode, TYPE_CREATE_CONSTRAINT_TRIGGER_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_CONVERSION)) {
            return parseStatement(tokens, STMT_CREATE_CONVERSION, parentNode, TYPE_CREATE_CONVERSION_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_DATABASE)) {
            return parseStatement(tokens, STMT_CREATE_DATABASE, parentNode, TYPE_CREATE_DATABASE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_FOREIGN_DATA_WRAPPER)) {
            return parseStatement(tokens,
                                  STMT_CREATE_FOREIGN_DATA_WRAPPER,
                                  parentNode,
                                  TYPE_CREATE_FOREIGN_DATA_WRAPPER_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_FUNCTION)) {
            return parseCreateFunctionStatement(tokens, parentNode);
        } else if (tokens.matches(STMT_CREATE_OR_REPLACE_FUNCTION)) {
            return parseCreateFunctionStatement(tokens, parentNode);
        } else if (tokens.matches(STMT_CREATE_GROUP)) {
            return parseStatement(tokens, STMT_CREATE_GROUP, parentNode, TYPE_CREATE_GROUP_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_INDEX)) {
            return parseStatement(tokens, STMT_CREATE_INDEX, parentNode, TYPE_CREATE_INDEX_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_UNIQUE_INDEX)) {
            return parseStatement(tokens, STMT_CREATE_UNIQUE_INDEX, parentNode, TYPE_CREATE_INDEX_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_LANGUAGE)) {
            return parseStatement(tokens, STMT_CREATE_LANGUAGE, parentNode, TYPE_CREATE_LANGUAGE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_TRUSTED_PROCEDURAL_LANGUAGE)) {
            return parseStatement(tokens, STMT_CREATE_TRUSTED_PROCEDURAL_LANGUAGE, parentNode, TYPE_CREATE_LANGUAGE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_PROCEDURAL_LANGUAGE)) {
            return parseStatement(tokens, STMT_CREATE_PROCEDURAL_LANGUAGE, parentNode, TYPE_CREATE_LANGUAGE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_OPERATOR)) {
            return parseStatement(tokens, STMT_CREATE_OPERATOR, parentNode, TYPE_CREATE_OPERATOR_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_ROLE)) {
            return parseStatement(tokens, STMT_CREATE_ROLE, parentNode, TYPE_CREATE_ROLE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_RULE) || tokens.matches(STMT_CREATE_OR_REPLACE_RULE)) {
            return parseCreateRuleStatement(tokens, parentNode);
        } else if (tokens.matches(STMT_CREATE_SEQUENCE)) {
            return parseStatement(tokens, STMT_CREATE_SEQUENCE, parentNode, TYPE_CREATE_SEQUENCE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_SERVER)) {
            return parseStatement(tokens, STMT_CREATE_SERVER, parentNode, TYPE_CREATE_SERVER_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_TABLESPACE)) {
            return parseStatement(tokens, STMT_CREATE_TABLESPACE, parentNode, TYPE_CREATE_TABLESPACE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_TEXT_SEARCH)) {
            return parseStatement(tokens, STMT_CREATE_TEXT_SEARCH, parentNode, TYPE_CREATE_TEXT_SEARCH_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_TRIGGER)) {
            return parseStatement(tokens, STMT_CREATE_TRIGGER, parentNode, TYPE_CREATE_TRIGGER_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_TYPE)) {
            return parseStatement(tokens, STMT_CREATE_TYPE, parentNode, TYPE_CREATE_TYPE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_USER_MAPPING)) {
            return parseStatement(tokens, STMT_CREATE_USER_MAPPING, parentNode, TYPE_CREATE_USER_MAPPING_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_USER)) {
            return parseStatement(tokens, STMT_CREATE_USER, parentNode, TYPE_CREATE_USER_STATEMENT);
        }

        return super.parseCreateStatement(tokens, parentNode);
    }

    @Override
    protected AstNode parseCreateTableStatement( DdlTokenStream tokens,
                                                 AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        markStartOfStatement(tokens);

        tokens.consume("CREATE"); // CREATE

        tokens.canConsumeAnyOf("LOCAL", "GLOBAL");
        tokens.canConsumeAnyOf("TEMP", "TEMPORARY");

        tokens.consume("TABLE"); // TABLE

        String tableName = parseName(tokens);
        AstNode tableNode = nodeFactory().node(tableName, parentNode, TYPE_CREATE_TABLE_STATEMENT);

        // //System.out.println("  >> PARSING CREATE TABLE >>  Name = " + tableName);
        // if( tokens.canConsume("AS") ) {
        // parseUntilTerminator(tokens);
        // } else if( tokens.matches(L_PAREN)){
        parseColumnsAndConstraints(tokens, tableNode);
        // }
        // // [ ON COMMIT { PRESERVE ROWS | DELETE ROWS | DROP } ]
        // // [ TABLESPACE tablespace ]
        // // [ WITH ( storage_parameter [= value] [, ... ] ) | WITH OIDS | WITHOUT OIDS ]
        // // [ WITH [ NO ] DATA ]
        // // AS query (SEE ABOVE)
        // if( tokens.canConsume("ON", "COMMIT") ) {
        // // PRESERVE ROWS | DELETE ROWS | DROP
        // tokens.canConsume("PRESERVE", "ROWS");
        // tokens.canConsume("DELETE", "ROWS");
        // tokens.canConsume("DROP");
        // } else if( tokens.canConsume("TABLESPACE") ) {
        // tokens.consume(); // tablespace name
        // } else if( tokens.canConsume("WITH", "OIDS") ||
        // tokens.canConsume("WITHOUT", "OUDS")) {
        // } else if( tokens.canConsume("WITH")) {
        // if( tokens.matches(L_PAREN) ) {
        // consumeParenBoundedTokens(tokens, true);
        // } else {
        // tokens.canConsume("NO");
        // tokens.canConsume("DATA");
        // }
        // }

        parseCreateTableOptions(tokens, tableNode);

        markEndOfStatement(tokens, tableNode);

        return tableNode;
    }

    @Override
    protected void parseNextCreateTableOption( DdlTokenStream tokens,
                                               AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        if (tokens.canConsume("ON", "COMMIT")) {
            // PRESERVE ROWS | DELETE ROWS | DROP
            tokens.canConsume("PRESERVE", "ROWS");
            tokens.canConsume("DELETE", "ROWS");
            tokens.canConsume("DROP");
        } else if (tokens.canConsume("TABLESPACE")) {
            tokens.consume(); // tablespace name
        } else if (tokens.canConsume("WITH", "OIDS") || tokens.canConsume("WITHOUT", "OUDS")) {
        } else if (tokens.canConsume("WITH")) {
            if (tokens.matches(L_PAREN)) {
                consumeParenBoundedTokens(tokens, true);
            } else {
                tokens.canConsume("NO");
                tokens.canConsume("DATA");
            }
        } else if (tokens.canConsume("AS")) {
            parseUntilTerminator(tokens);
        }
    }

    @Override
    protected boolean areNextTokensCreateTableOptions( DdlTokenStream tokens ) throws ParsingException {
        assert tokens != null;

        boolean result = false;

        if (tokens.matches("ON", "COMMIT") || tokens.matches("TABLESPACE") || tokens.matches("WITH") || tokens.matches("WITHOUT")
            || tokens.matches("AS")) {
            result = true;
        }

        return result;
    }

    @Override
    protected AstNode parseCreateViewStatement( DdlTokenStream tokens,
                                                AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        return super.parseCreateViewStatement(tokens, parentNode);
    }

    @Override
    protected boolean parseDefaultClause( DdlTokenStream tokens,
                                          AstNode columnNode ) {
        assert tokens != null;
        assert columnNode != null;

        /*
        	} else if( tokens.matches("NOW")){
        	    tokens.consume("NOW");
        	    tokens.consume('(');
        	    tokens.consume(')');
        	    defaultValue = "NOW()";
        	} else if( tokens.matches("NEXTVAL")){
        	    defaultValue = tokens.consume() + consumeParenBoundedTokens(tokens, true);
        	} 
         * 
         */
        // defaultClause
        // : 'WITH'? 'DEFAULT' defaultOption
        // ;
        // defaultOption : ('('? literal ')'?) | datetimeValueFunction
        // | 'SYSDATE' | 'USER' | 'CURRENT_USER' | 'SESSION_USER' | 'SYSTEM_USER' | 'NULL' | nowOption;
        String defaultValue = "";

        if (tokens.matchesAnyOf("WITH", "DEFAULT")) {
            if (tokens.matches("WITH")) {
                tokens.consume();
            }
            tokens.consume("DEFAULT");
            int optionID = -1;
            int precision = -1;

            if (tokens.canConsume("CURRENT_DATE")) {

                optionID = DEFAULT_ID_DATETIME;
                defaultValue = "CURRENT_DATE";
            } else if (tokens.canConsume("CURRENT_TIME")) {
                optionID = DEFAULT_ID_DATETIME;
                defaultValue = "CURRENT_TIME";
                if (tokens.canConsume(L_PAREN)) {
                    // EXPECT INTEGER
                    precision = integer(tokens.consume());
                    tokens.canConsume(R_PAREN);
                }
            } else if (tokens.canConsume("CURRENT_TIMESTAMP")) {
                optionID = DEFAULT_ID_DATETIME;
                defaultValue = "CURRENT_TIMESTAMP";
                if (tokens.canConsume(L_PAREN)) {
                    // EXPECT INTEGER
                    precision = integer(tokens.consume());
                    tokens.canConsume(R_PAREN);
                }
            } else if (tokens.canConsume("USER")) {
                optionID = DEFAULT_ID_USER;
                defaultValue = "USER";
            } else if (tokens.canConsume("CURRENT_USER")) {
                optionID = DEFAULT_ID_CURRENT_USER;
                defaultValue = "CURRENT_USER";
            } else if (tokens.canConsume("SESSION_USER")) {
                optionID = DEFAULT_ID_SESSION_USER;
                defaultValue = "SESSION_USER";
            } else if (tokens.canConsume("SYSTEM_USER")) {
                optionID = DEFAULT_ID_SYSTEM_USER;
                defaultValue = "SYSTEM_USER";
            } else if (tokens.canConsume("NULL")) {
                optionID = DEFAULT_ID_NULL;
                defaultValue = "NULL";
            } else if (tokens.canConsume(L_PAREN)) {
                optionID = DEFAULT_ID_LITERAL;
                while (!tokens.canConsume(R_PAREN)) {
                    defaultValue = defaultValue + tokens.consume();
                }
            } else if (tokens.matches("NOW")) {
                optionID = DEFAULT_ID_LITERAL;
                tokens.consume("NOW");
                tokens.consume('(');
                tokens.consume(')');
                defaultValue = "NOW()";
            } else if (tokens.matches("NEXTVAL")) {
                optionID = DEFAULT_ID_LITERAL;
                defaultValue = tokens.consume() + consumeParenBoundedTokens(tokens, true);
            } else {
                optionID = DEFAULT_ID_LITERAL;
                // Assume default was EMPTY or ''
                defaultValue = tokens.consume();
                // NOTE: default value could be a Real number as well as an integer, so
                // 1000.00 is valid
                if (tokens.canConsume(".")) {
                    defaultValue = defaultValue + '.' + tokens.consume();
                }
            }

            columnNode.setProperty(DEFAULT_OPTION, optionID);
            columnNode.setProperty(DEFAULT_VALUE, defaultValue);
            if (precision > -1) {
                columnNode.setProperty(DEFAULT_PRECISION, precision);
            }
            return true;
        }

        return false;
    }

    @Override
    protected AstNode parseCustomStatement( DdlTokenStream tokens,
                                            AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        if (tokens.matches(STMT_COMMENT_ON)) {
            return parseCommentStatement(tokens, parentNode);
        } else if (tokens.matches(STMT_ABORT)) {
            return parseStatement(tokens, STMT_ABORT, parentNode, TYPE_ABORT_STATEMENT);
        } else if (tokens.matches(STMT_ANALYZE)) {
            return parseStatement(tokens, STMT_ANALYZE, parentNode, TYPE_ANALYZE_STATEMENT);
        } else if (tokens.matches(STMT_CLUSTER)) {
            return parseStatement(tokens, STMT_CLUSTER, parentNode, TYPE_CLUSTER_STATEMENT);
        } else if (tokens.matches(STMT_COPY)) {
            return parseStatement(tokens, STMT_COPY, parentNode, TYPE_COPY_STATEMENT);
        } else if (tokens.matches(STMT_DEALLOCATE_PREPARE)) {
            return parseStatement(tokens, STMT_DEALLOCATE_PREPARE, parentNode, TYPE_DEALLOCATE_STATEMENT);
        } else if (tokens.matches(STMT_DEALLOCATE)) {
            return parseStatement(tokens, STMT_DEALLOCATE, parentNode, TYPE_DEALLOCATE_STATEMENT);
        } else if (tokens.matches(STMT_DECLARE)) {
            return parseStatement(tokens, STMT_DECLARE, parentNode, TYPE_DECLARE_STATEMENT);
        } else if (tokens.matches(STMT_EXPLAIN_ANALYZE)) {
            return parseStatement(tokens, STMT_EXPLAIN_ANALYZE, parentNode, TYPE_EXPLAIN_STATEMENT);
        } else if (tokens.matches(STMT_EXPLAIN)) {
            return parseStatement(tokens, STMT_EXPLAIN, parentNode, TYPE_EXPLAIN_STATEMENT);
        } else if (tokens.matches(STMT_FETCH)) {
            return parseStatement(tokens, STMT_FETCH, parentNode, TYPE_FETCH_STATEMENT);
        } else if (tokens.matches(STMT_LISTEN)) {
            return parseStatement(tokens, STMT_LISTEN, parentNode, TYPE_LISTEN_STATEMENT);
        } else if (tokens.matches(STMT_LOAD)) {
            return parseStatement(tokens, STMT_LOAD, parentNode, TYPE_LOAD_STATEMENT);
        } else if (tokens.matches(STMT_LOCK_TABLE)) {
            return parseStatement(tokens, STMT_LOCK_TABLE, parentNode, TYPE_LOCK_TABLE_STATEMENT);
        } else if (tokens.matches(STMT_MOVE)) {
            return parseStatement(tokens, STMT_MOVE, parentNode, TYPE_MOVE_STATEMENT);
        } else if (tokens.matches(STMT_NOTIFY)) {
            return parseStatement(tokens, STMT_NOTIFY, parentNode, TYPE_NOTIFY_STATEMENT);
        } else if (tokens.matches(STMT_PREPARE)) {
            return parseStatement(tokens, STMT_PREPARE, parentNode, TYPE_PREPARE_STATEMENT);
        } else if (tokens.matches(STMT_REASSIGN_OWNED)) {
            return parseStatement(tokens, STMT_REASSIGN_OWNED, parentNode, TYPE_REASSIGN_OWNED_STATEMENT);
        } else if (tokens.matches(STMT_REINDEX)) {
            return parseStatement(tokens, STMT_REINDEX, parentNode, TYPE_REINDEX_STATEMENT);
        } else if (tokens.matches(STMT_RELEASE_SAVEPOINT)) {
            return parseStatement(tokens, STMT_RELEASE_SAVEPOINT, parentNode, TYPE_RELEASE_SAVEPOINT_STATEMENT);
        } else if (tokens.matches(STMT_ROLLBACK)) {
            return parseStatement(tokens, STMT_ROLLBACK, parentNode, TYPE_ROLLBACK_STATEMENT);
        } else if (tokens.matches(STMT_SELECT_INTO)) {
            return parseStatement(tokens, STMT_SELECT_INTO, parentNode, TYPE_SELECT_INTO_STATEMENT);
        } else if (tokens.matches(STMT_SHOW)) {
            return parseStatement(tokens, STMT_SHOW, parentNode, TYPE_SHOW_STATEMENT);
        } else if (tokens.matches(STMT_TRUNCATE)) {
            return parseStatement(tokens, STMT_TRUNCATE, parentNode, TYPE_TRUNCATE_STATEMENT);
        } else if (tokens.matches(STMT_UNLISTEN)) {
            return parseStatement(tokens, STMT_UNLISTEN, parentNode, TYPE_UNLISTEN_STATEMENT);
        } else if (tokens.matches(STMT_VACUUM)) {
            return parseStatement(tokens, STMT_VACUUM, parentNode, TYPE_VACUUM_STATEMENT);
        }

        return super.parseCustomStatement(tokens, parentNode);
    }

    @Override
    protected AstNode parseDropStatement( DdlTokenStream tokens,
                                          AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        if (tokens.matches(STMT_DROP_AGGREGATE)) {
            return parseStatement(tokens, STMT_DROP_AGGREGATE, parentNode, TYPE_DROP_AGGREGATE_STATEMENT);
        } else if (tokens.matches(STMT_DROP_CAST)) {
            return parseStatement(tokens, STMT_DROP_CAST, parentNode, TYPE_DROP_CAST_STATEMENT);
        } else if (tokens.matches(STMT_DROP_CONSTRAINT_TRIGGER)) {
            return parseStatement(tokens, STMT_DROP_CONSTRAINT_TRIGGER, parentNode, TYPE_DROP_CONSTRAINT_TRIGGER_STATEMENT);
        } else if (tokens.matches(STMT_DROP_CONVERSION)) {
            return parseSimpleDropStatement(tokens, STMT_DROP_CONVERSION, parentNode, TYPE_DROP_CONVERSION_STATEMENT);
        } else if (tokens.matches(STMT_DROP_DATABASE)) {
            return parseSimpleDropStatement(tokens, STMT_DROP_DATABASE, parentNode, TYPE_DROP_DATABASE_STATEMENT);
        } else if (tokens.matches(STMT_DROP_FOREIGN_DATA_WRAPPER)) {
            return parseSimpleDropStatement(tokens,
                                            STMT_DROP_FOREIGN_DATA_WRAPPER,
                                            parentNode,
                                            TYPE_DROP_FOREIGN_DATA_WRAPPER_STATEMENT);
        } else if (tokens.matches(STMT_DROP_FUNCTION)) {
            return parseStatement(tokens, STMT_DROP_FUNCTION, parentNode, TYPE_DROP_FUNCTION_STATEMENT);
        } else if (tokens.matches(STMT_DROP_GROUP)) {
            return parseSimpleDropStatement(tokens, STMT_DROP_GROUP, parentNode, TYPE_DROP_GROUP_STATEMENT);
        } else if (tokens.matches(STMT_DROP_INDEX)) {
            return parseSimpleDropStatement(tokens, STMT_DROP_INDEX, parentNode, TYPE_DROP_INDEX_STATEMENT);
        } else if (tokens.matches(STMT_DROP_LANGUAGE)) {
            return parseSimpleDropStatement(tokens, STMT_DROP_LANGUAGE, parentNode, TYPE_DROP_LANGUAGE_STATEMENT);
        } else if (tokens.matches(STMT_DROP_PROCEDURAL_LANGUAGE)) {
            return parseSimpleDropStatement(tokens, STMT_DROP_PROCEDURAL_LANGUAGE, parentNode, TYPE_DROP_LANGUAGE_STATEMENT);
        } else if (tokens.matches(STMT_DROP_OPERATOR)) {
            return parseStatement(tokens, STMT_DROP_OPERATOR, parentNode, TYPE_DROP_OPERATOR_STATEMENT);
        } else if (tokens.matches(STMT_DROP_OWNED_BY)) {
            return parseSimpleDropStatement(tokens, STMT_DROP_OWNED_BY, parentNode, TYPE_DROP_OWNED_BY_STATEMENT);
        } else if (tokens.matches(STMT_DROP_ROLE)) {
            return parseSimpleDropStatement(tokens, STMT_DROP_ROLE, parentNode, TYPE_DROP_ROLE_STATEMENT);
        } else if (tokens.matches(STMT_DROP_RULE)) {
            return parseStatement(tokens, STMT_DROP_RULE, parentNode, TYPE_DROP_RULE_STATEMENT);
        } else if (tokens.matches(STMT_DROP_SEQUENCE)) {
            return parseSimpleDropStatement(tokens, STMT_DROP_SEQUENCE, parentNode, TYPE_DROP_SEQUENCE_STATEMENT);
        } else if (tokens.matches(STMT_DROP_SERVER)) {
            return parseSimpleDropStatement(tokens, STMT_DROP_SERVER, parentNode, TYPE_DROP_SERVER_STATEMENT);
        } else if (tokens.matches(STMT_DROP_TABLESPACE)) {
            return parseSimpleDropStatement(tokens, STMT_DROP_TABLESPACE, parentNode, TYPE_DROP_TABLESPACE_STATEMENT);
        } else if (tokens.matches(STMT_DROP_TEXT_SEARCH_CONFIGURATION)) {
            return parseSimpleDropStatement(tokens,
                                            STMT_DROP_TEXT_SEARCH_CONFIGURATION,
                                            parentNode,
                                            TYPE_DROP_TEXT_SEARCH_STATEMENT);
        } else if (tokens.matches(STMT_DROP_TEXT_SEARCH_DICTIONARY)) {
            return parseSimpleDropStatement(tokens, STMT_DROP_TEXT_SEARCH_DICTIONARY, parentNode, TYPE_DROP_TEXT_SEARCH_STATEMENT);
        } else if (tokens.matches(STMT_DROP_TEXT_SEARCH_PARSER)) {
            return parseSimpleDropStatement(tokens, STMT_DROP_TEXT_SEARCH_PARSER, parentNode, TYPE_DROP_TEXT_SEARCH_STATEMENT);
        } else if (tokens.matches(STMT_DROP_TEXT_SEARCH_TEMPLATE)) {
            return parseSimpleDropStatement(tokens, STMT_DROP_TEXT_SEARCH_TEMPLATE, parentNode, TYPE_DROP_TEXT_SEARCH_STATEMENT);
        } else if (tokens.matches(STMT_DROP_TRIGGER)) {
            return parseStatement(tokens, STMT_DROP_TRIGGER, parentNode, TYPE_DROP_TRIGGER_STATEMENT);
        } else if (tokens.matches(STMT_DROP_TYPE)) {
            return parseSimpleDropStatement(tokens, STMT_DROP_TYPE, parentNode, TYPE_DROP_TYPE_STATEMENT);
        } else if (tokens.matches(STMT_DROP_USER_MAPPING)) {
            return parseStatement(tokens, STMT_DROP_USER_MAPPING, parentNode, TYPE_DROP_USER_MAPPING_STATEMENT);
        } else if (tokens.matches(STMT_DROP_USER)) {
            return parseSimpleDropStatement(tokens, STMT_DROP_USER, parentNode, TYPE_DROP_USER_STATEMENT);
        } else if (tokens.matches(StatementStartPhrases.STMT_DROP_DOMAIN)) {
            // -- DROP DOMAIN [ IF EXISTS ] name [, ...] [ CASCADE | RESTRICT ]
            return parseSimpleDropStatement(tokens,
                                            StatementStartPhrases.STMT_DROP_DOMAIN,
                                            parentNode,
                                            TYPE_DROP_DOMAIN_STATEMENT);
        } else if (tokens.matches(StatementStartPhrases.STMT_DROP_TABLE)) {
            // -- DROP TABLE [ IF EXISTS ] name [, ...] [ CASCADE | RESTRICT ]
            return parseSimpleDropStatement(tokens, StatementStartPhrases.STMT_DROP_TABLE, parentNode, TYPE_DROP_TABLE_STATEMENT);
        } else if (tokens.matches(StatementStartPhrases.STMT_DROP_VIEW)) {
            // -- DROP VIEW [ IF EXISTS ] name [, ...] [ CASCADE | RESTRICT ]
            return parseSimpleDropStatement(tokens, StatementStartPhrases.STMT_DROP_VIEW, parentNode, TYPE_DROP_VIEW_STATEMENT);
        } else if (tokens.matches(StatementStartPhrases.STMT_DROP_SCHEMA)) {
            // -- DROP SCHEMA [ IF EXISTS ] name [, ...] [ CASCADE | RESTRICT ]
            return parseSimpleDropStatement(tokens,
                                            StatementStartPhrases.STMT_DROP_SCHEMA,
                                            parentNode,
                                            TYPE_DROP_SCHEMA_STATEMENT);
        }

        return super.parseDropStatement(tokens, parentNode);
    }

    private AstNode parseSimpleDropStatement( DdlTokenStream tokens,
                                              String[] startPhrase,
                                              AstNode parentNode,
                                              Name stmtType ) throws ParsingException {
        assert tokens != null;
        assert startPhrase != null && startPhrase.length > 0;
        assert parentNode != null;

        markStartOfStatement(tokens);

        String behavior = null;
        tokens.consume(startPhrase);
        boolean usesIfExists = tokens.canConsume("IF", "EXISTS"); // SUPER CLASS does not include "IF EXISTS"

        List<String> nameList = new ArrayList<String>();
        nameList.add(parseName(tokens));
        while (tokens.matches(COMMA)) {
            tokens.consume(COMMA);
            nameList.add(parseName(tokens));
        }

        if (tokens.canConsume("CASCADE")) {
            behavior = "CASCADE";
        } else if (tokens.canConsume("RESTRICT")) {
            behavior = "RESTRICT";
        }

        AstNode dropNode = nodeFactory().node(nameList.get(0), parentNode, stmtType);

        if (behavior != null) {
            dropNode.setProperty(DROP_BEHAVIOR, behavior);
        }

        markEndOfStatement(tokens, dropNode);

        // If there is only ONE name, then the EXPRESSION property is the whole expression and we don't need to set the
        // ORIGINAL EXPRESSION
        String originalExpression = (String)dropNode.getProperty(DDL_EXPRESSION).getFirstValue();
        Object startLineNumber = dropNode.getProperty(DDL_START_LINE_NUMBER).getFirstValue();
        Object startColumnNumber = dropNode.getProperty(DDL_START_COLUMN_NUMBER).getFirstValue();
        Object startCharIndex = dropNode.getProperty(DDL_START_CHAR_INDEX).getFirstValue();

        if (nameList.size() > 1) {
            for (int i = 1; i < nameList.size(); i++) {
                String nextName = nameList.get(i);
                AstNode newNode = createSingleDropNode(nextName,
                                                       startPhrase,
                                                       originalExpression,
                                                       usesIfExists,
                                                       behavior,
                                                       stmtType,
                                                       parentNode);
                newNode.setProperty(DDL_START_LINE_NUMBER, startLineNumber);
                newNode.setProperty(DDL_START_COLUMN_NUMBER, startColumnNumber);
                newNode.setProperty(DDL_START_CHAR_INDEX, startCharIndex);
            }

            // Since there is more than ONE name, then the EXPRESSION property of the first node's expression needs to be reset to
            // the first name and the ORIGINAL EXPRESSION property set to the entire statement.
            StringBuffer sb = new StringBuffer().append(getStatementTypeName(startPhrase));
            if (usesIfExists) {
                sb.append(SPACE).append("IF EXISTS");
            }
            sb.append(SPACE).append(nameList.get(0));
            if (behavior != null) {
                sb.append(SPACE).append(behavior);
            }
            sb.append(SEMICOLON);
            dropNode.setProperty(DDL_EXPRESSION, sb.toString());
            dropNode.setProperty(DDL_ORIGINAL_EXPRESSION, originalExpression);
        }

        return dropNode;
    }

    private AstNode createSingleDropNode( String name,
                                          String[] startPhrase,
                                          String originalExpression,
                                          boolean usesIfExists,
                                          String behavior,
                                          Name nodeType,
                                          AstNode parentNode ) {
        assert name != null;
        assert startPhrase != null && startPhrase.length > 0;
        assert nodeType != null;
        assert parentNode != null;

        AstNode newNode = nodeFactory().node(name, parentNode, nodeType);
        StringBuffer sb = new StringBuffer().append(getStatementTypeName(startPhrase));
        if (usesIfExists) {
            sb.append(SPACE).append("IF EXISTS");
        }
        sb.append(SPACE).append(name);
        if (behavior != null) {
            sb.append(SPACE).append(behavior);
        }
        sb.append(SEMICOLON);

        newNode.setProperty(DDL_EXPRESSION, sb.toString());
        newNode.setProperty(DDL_ORIGINAL_EXPRESSION, originalExpression);

        return newNode;
    }

    @Override
    protected AstNode parseGrantStatement( DdlTokenStream tokens,
                                           AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        return super.parseGrantStatement(tokens, parentNode);
    }

    @Override
    protected AstNode parseSetStatement( DdlTokenStream tokens,
                                         AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        return super.parseSetStatement(tokens, parentNode);
    }

    private AstNode parseCommentStatement( DdlTokenStream tokens,
                                           AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        markStartOfStatement(tokens);

        /*
        --  TABLE object_name |
        --  COLUMN table_name.column_name |
        --  AGGREGATE agg_name (agg_type [, ...] ) |
        --  CAST (sourcetype AS targettype) |
        --  CONSTRAINT constraint_name ON table_name |
        --  CONVERSION object_name |
        --  DATABASE object_name |
        --  DOMAIN object_name |
        --  FUNCTION func_name ( [ [ argmode ] [ argname ] argtype [, ...] ] ) |
        --  INDEX object_name |
        --  LARGE OBJECT large_object_oid |
        --  OPERATOR op (leftoperand_type, rightoperand_type) |
        --  OPERATOR CLASS object_name USING index_method |
        --  OPERATOR FAMILY object_name USING index_method |
        --  [ PROCEDURAL ] LANGUAGE object_name |
        --  ROLE object_name |
        --  RULE rule_name ON table_name |
        --  SCHEMA object_name |
        --  SEQUENCE object_name |
        --  TABLESPACE object_name |
        --  TEXT SEARCH CONFIGURATION object_name |
        --  TEXT SEARCH DICTIONARY object_name |
        --  TEXT SEARCH PARSER object_name |
        --  TEXT SEARCH TEMPLATE object_name |
        --  TRIGGER trigger_name ON table_name |
        --  TYPE object_name |
        --  VIEW object_name
        --} IS ’text’
         */
        tokens.consume("COMMENT", "ON"); // consumes 'COMMENT' 'ON'

        String objectType = null;
        String objectName = null;

        if (tokens.matches(TABLE)) {
            objectType = tokens.consume();
            objectName = parseName(tokens);
        } else if (tokens.matches("COLUMN")) {
            objectType = tokens.consume();
            objectName = parseName(tokens);
        } else if (tokens.matches("AGGREGATE")) {
            objectType = tokens.consume();
            objectName = parseName(tokens);
            // (agg_type [, ...] )
            consumeParenBoundedTokens(tokens, true);
        } else if (tokens.matches("CAST")) {
            objectType = tokens.consume();
            consumeParenBoundedTokens(tokens, true);
        } else if (tokens.matches("CONSTRAINT")) {
            objectType = tokens.consume();
            objectName = parseName(tokens);
            tokens.consume("ON");
            tokens.consume(); // table_name
        } else if (tokens.matches("CONVERSION")) {
            objectType = tokens.consume();
            objectName = parseName(tokens);
        } else if (tokens.matches("DATABASE")) {
            objectType = tokens.consume();
            objectName = parseName(tokens);
        } else if (tokens.matches("DOMAIN")) {
            objectType = tokens.consume();
            objectName = parseName(tokens);
        } else if (tokens.matches("FUNCTION")) {
            objectType = tokens.consume();
            objectName = parseName(tokens);
            consumeParenBoundedTokens(tokens, true);
        } else if (tokens.matches("INDEX")) {
            objectType = tokens.consume();
            objectName = parseName(tokens);
        } else if (tokens.matches("LARGE", "OBJECT")) {
            tokens.consume("LARGE", "OBJECT");
            objectType = "LARGE OBJECT";
            objectName = parseName(tokens);
        } else if (tokens.matches("OPERATOR", "FAMILY")) {
            tokens.consume("OPERATOR", "FAMILY");
            objectType = "OPERATOR FAMILY";
            objectName = parseName(tokens);
            tokens.consume("USING");
            tokens.consume(); // index_method
        } else if (tokens.matches("OPERATOR", "CLASS")) {
            tokens.consume("OPERATOR", "CLASS");
            objectType = "OPERATOR CLASS";
            objectName = parseName(tokens);
            tokens.consume("USING");
            tokens.consume(); // index_method
        } else if (tokens.matches("OPERATOR")) {
            objectType = tokens.consume();
            objectName = parseName(tokens);
            consumeParenBoundedTokens(tokens, true);
        } else if (tokens.matches("PROCEDURAL", "LANGUAGE")) {
            tokens.consume("PROCEDURAL", "LANGUAGE");
            objectType = "PROCEDURAL LANGUAGE";
            objectName = parseName(tokens);
        } else if (tokens.matches("LANGUAGE")) {
            objectType = tokens.consume();
            objectName = parseName(tokens);
        } else if (tokens.matches("ROLE")) {
            objectType = tokens.consume();
            objectName = parseName(tokens);
        } else if (tokens.matches("RULE")) {
            objectType = tokens.consume();
            objectName = parseName(tokens);
            tokens.consume("ON");
            tokens.consume(); // table_name
        } else if (tokens.matches("SCHEMA")) {
            objectType = tokens.consume();
            objectName = parseName(tokens);
        } else if (tokens.matches("SEQUENCE")) {
            objectType = tokens.consume();
            objectName = parseName(tokens);
        } else if (tokens.matches("TABLESPACE")) {
            objectType = tokens.consume();
            objectName = parseName(tokens);
        } else if (tokens.matches("TEXT", "SEARCH", "CONFIGURATION")) {
            tokens.consume("TEXT", "SEARCH", "CONFIGURATION");
            objectType = "TEXT SEARCH CONFIGURATION";
            objectName = parseName(tokens);
        } else if (tokens.matches("TEXT", "SEARCH", "DICTIONARY")) {
            tokens.consume("TEXT", "SEARCH", "DICTIONARY");
            objectType = "TEXT SEARCH DICTIONARY";
            objectName = parseName(tokens);
        } else if (tokens.matches("TEXT", "SEARCH", "PARSER")) {
            tokens.consume("TEXT", "SEARCH", "PARSER");
            objectType = "TEXT SEARCH PARSER";
            objectName = parseName(tokens);
        } else if (tokens.matches("TEXT", "SEARCH", "TEMPLATE")) {
            tokens.consume("TEXT", "SEARCH", "TEMPLATE");
            objectType = "TEXT SEARCH TEMPLATE";
            objectName = parseName(tokens);
        } else if (tokens.matches("TRIGGER")) {
            objectType = tokens.consume();
            objectName = parseName(tokens);
            tokens.consume("ON");
            tokens.consume(); // table_name
        } else if (tokens.matches("TYPE")) {
            objectType = tokens.consume();
            objectName = parseName(tokens);
        } else if (tokens.matches("VIEW")) {
            objectType = tokens.consume();
            objectName = parseName(tokens);
        }

        // System.out.println("  >> FOUND [COMMENT ON] STATEMENT >>  TABLE Name = " + objName);
        String commentString = null;

        tokens.consume("IS");
        if (tokens.matches("NULL")) {
            tokens.consume("NULL");
            commentString = "NULL";
        } else {
            commentString = parseUntilTerminator(tokens).trim();
        }

        AstNode commentNode = null;
        

        if (objectName != null) {
            commentNode = nodeFactory().node(objectName, parentNode, TYPE_COMMENT_ON_STATEMENT);
        } else {
            commentNode = nodeFactory().node("commentOn", parentNode, TYPE_COMMENT_ON_STATEMENT);
            commentNode.setProperty(PostgresDdlLexicon.TARGET_OBJECT_NAME, objectName);
        }
        commentNode.setProperty(PostgresDdlLexicon.COMMENT, commentString);
        commentNode.setProperty(PostgresDdlLexicon.TARGET_OBJECT_TYPE, objectType);
        
        markEndOfStatement(tokens, commentNode);

        return commentNode;
    }

    /**
     * Utility method designed to parse columns within an ALTER TABLE ADD statement.
     * 
     * @param tokens the tokenized {@link DdlTokenStream} of the DDL input content; may not be null
     * @param tableNode the parent {@link AstNode} node; may not be null
     * @param isAlterTable
     * @throws ParsingException
     */
    protected void parseColumns( DdlTokenStream tokens,
                                 AstNode tableNode,
                                 boolean isAlterTable ) throws ParsingException {
        assert tokens != null;
        assert tableNode != null;

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

    private AstNode parseCreateRuleStatement( DdlTokenStream tokens,
                                              AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        // CREATE [ OR REPLACE ] RULE name AS ON event
        // TO table [ WHERE condition ]
        // DO [ ALSO | INSTEAD ] { NOTHING | command | ( command ; command ... ) }
        //
        // EXAMPLE: CREATE RULE notify_me AS ON UPDATE TO mytable DO ALSO NOTIFY mytable;
        // parseStatement(tokens, STMT_CREATE_RULE, parentNode, TYPE_CREATE_RULE_STATEMENT);

        markStartOfStatement(tokens);

        boolean isReplace = tokens.canConsume(STMT_CREATE_OR_REPLACE_RULE);
        tokens.canConsume(STMT_CREATE_RULE);

        String name = parseName(tokens);

        AstNode node = nodeFactory().node(name, parentNode, TYPE_CREATE_RULE_STATEMENT);
        if (isReplace) {
            // TODO: SET isReplace = TRUE to node (possibly a cnd mixin of "replaceable"
        }
        parseUntilTerminatorIgnoreEmbeddedStatements(tokens);

        markEndOfStatement(tokens, node);

        return node;
    }

    private AstNode parseCreateFunctionStatement( DdlTokenStream tokens,
                                                  AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        markStartOfStatement(tokens);

        boolean isReplace = tokens.canConsume(STMT_CREATE_OR_REPLACE_FUNCTION);

        tokens.canConsume(STMT_CREATE_FUNCTION);

        String name = parseName(tokens);

        AstNode node = nodeFactory().node(name, parentNode, TYPE_CREATE_FUNCTION_STATEMENT);

        if (isReplace) {
            // TODO: SET isReplace = TRUE to node (possibly a cnd mixin of "replaceable"
        }

        parseUntilTerminator(tokens);

        markEndOfStatement(tokens, node);

        return node;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.sequencer.ddl.StandardDdlParser#getValidSchemaChildTypes()
     */
    @Override
    protected Name[] getValidSchemaChildTypes() {
        return PostgresStatementStartPhrases.VALID_SCHEMA_CHILD_STMTS;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.sequencer.ddl.StandardDdlParser#getDataTypeStartWords()
     */
    @Override
    protected List<String> getCustomDataTypeStartWords() {
        return PostgresDataTypes.CUSTOM_DATATYPE_START_WORDS;
    }

    class PostgresDataTypeParser extends DataTypeParser {

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.sequencer.ddl.datatype.DataTypeParser#isCustomDataType(org.jboss.dna.sequencer.ddl.DdlTokenStream)
         */
        @Override
        protected boolean isCustomDataType( DdlTokenStream tokens ) throws ParsingException {
            // Loop through the registered statement start string arrays and look for exact matches.

            for (String[] stmts : postgresDataTypeStrings) {
                if (tokens.matches(stmts)) return true;
            }
            return super.isCustomDataType(tokens);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.sequencer.ddl.datatype.DataTypeParser#parseApproxNumericType(org.jboss.dna.sequencer.ddl.DdlTokenStream)
         */
        @Override
        protected DataType parseApproxNumericType( DdlTokenStream tokens ) throws ParsingException {
            DataType result = null;
            String typeName = null;

            if (tokens.matches(PostgresDataTypes.DTYPE_FLOAT4) || tokens.matches(PostgresDataTypes.DTYPE_FLOAT8)) {
                typeName = tokens.consume();
                result = new DataType(typeName);
                int precision = 0;
                if (tokens.matches('(')) {
                    precision = parseBracketedInteger(tokens, result);
                }
                result.setPrecision(precision);
            }

            if (result == null) {
                result = super.parseApproxNumericType(tokens);
            }

            return result;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.sequencer.ddl.datatype.DataTypeParser#parseBitStringType(org.jboss.dna.sequencer.ddl.DdlTokenStream)
         */
        @Override
        protected DataType parseBitStringType( DdlTokenStream tokens ) throws ParsingException {
            return super.parseBitStringType(tokens);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.sequencer.ddl.datatype.DataTypeParser#parseBracketedInteger(org.jboss.dna.sequencer.ddl.DdlTokenStream,
         *      org.jboss.dna.sequencer.ddl.datatype.DataType)
         */
        @Override
        protected int parseBracketedInteger( DdlTokenStream tokens,
                                             DataType dataType ) {
            return super.parseBracketedInteger(tokens, dataType);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.sequencer.ddl.datatype.DataTypeParser#parseCharStringType(org.jboss.dna.sequencer.ddl.DdlTokenStream)
         */
        @Override
        protected DataType parseCharStringType( DdlTokenStream tokens ) throws ParsingException {
            DataType result = super.parseCharStringType(tokens);

            tokens.canConsume("FOR", "BIT", "DATA");

            return result;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.sequencer.ddl.datatype.DataTypeParser#parseCustomType(org.jboss.dna.sequencer.ddl.DdlTokenStream)
         */
        @Override
        protected DataType parseCustomType( DdlTokenStream tokens ) throws ParsingException {
            DataType result = null;
            String typeName = null;

            if (tokens.matches(PostgresDataTypes.DTYPE_BIGSERIAL) || tokens.matches(PostgresDataTypes.DTYPE_SERIAL)
                || tokens.matches(PostgresDataTypes.DTYPE_SERIAL4) || tokens.matches(PostgresDataTypes.DTYPE_SERIAL8)
                || tokens.matches(PostgresDataTypes.DTYPE_INT2) || tokens.matches(PostgresDataTypes.DTYPE_INT4)
                || tokens.matches(PostgresDataTypes.DTYPE_INT8) || tokens.matches(PostgresDataTypes.DTYPE_BOX)
                || tokens.matches(PostgresDataTypes.DTYPE_BOOL) || tokens.matches(PostgresDataTypes.DTYPE_BOOLEAN)
                || tokens.matches(PostgresDataTypes.DTYPE_BYTEA) || tokens.matches(PostgresDataTypes.DTYPE_CIDR)
                || tokens.matches(PostgresDataTypes.DTYPE_CIRCLE) || tokens.matches(PostgresDataTypes.DTYPE_INET)
                || tokens.matches(PostgresDataTypes.DTYPE_LINE) || tokens.matches(PostgresDataTypes.DTYPE_LSEG)
                || tokens.matches(PostgresDataTypes.DTYPE_MACADDR) || tokens.matches(PostgresDataTypes.DTYPE_MONEY)
                || tokens.matches(PostgresDataTypes.DTYPE_PATH) || tokens.matches(PostgresDataTypes.DTYPE_POINT)
                || tokens.matches(PostgresDataTypes.DTYPE_POLYGON) || tokens.matches(PostgresDataTypes.DTYPE_TEXT)
                || tokens.matches(PostgresDataTypes.DTYPE_TSQUERY) || tokens.matches(PostgresDataTypes.DTYPE_TSVECTOR)
                || tokens.matches(PostgresDataTypes.DTYPE_TXID_SNAPSHOT) || tokens.matches(PostgresDataTypes.DTYPE_UUID)
                || tokens.matches(PostgresDataTypes.DTYPE_VARBIT) || tokens.matches(PostgresDataTypes.DTYPE_XML)) {
                typeName = tokens.consume();
                result = new DataType(typeName);
            }

            if (result == null) {
                super.parseCustomType(tokens);
            }
            return result;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.sequencer.ddl.datatype.DataTypeParser#parseDateTimeType(org.jboss.dna.sequencer.ddl.DdlTokenStream)
         */
        @Override
        protected DataType parseDateTimeType( DdlTokenStream tokens ) throws ParsingException {
            DataType dtype = super.parseDateTimeType(tokens);

            tokens.canConsume("WITHOUT", "TIME", "ZONE");

            return dtype;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.sequencer.ddl.datatype.DataTypeParser#parseExactNumericType(org.jboss.dna.sequencer.ddl.DdlTokenStream)
         */
        @Override
        protected DataType parseExactNumericType( DdlTokenStream tokens ) throws ParsingException {
            return super.parseExactNumericType(tokens);
        }

    }

}
