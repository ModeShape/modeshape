package org.modeshape.sequencer.ddl.dialect.mysql;

import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DDL_EXPRESSION;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DDL_ORIGINAL_EXPRESSION;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DDL_START_CHAR_INDEX;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DDL_START_COLUMN_NUMBER;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DDL_START_LINE_NUMBER;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.NEW_NAME;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_ALTER_ALGORITHM_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_ALTER_DATABASE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_ALTER_DEFINER_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_ALTER_EVENT_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_ALTER_FUNCTION_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_ALTER_LOGFILE_GROUP_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_ALTER_PROCEDURE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_ALTER_SCHEMA_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_ALTER_SERVER_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_ALTER_TABLESPACE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_ALTER_VIEW_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_CREATE_DEFINER_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_CREATE_EVENT_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_CREATE_FUNCTION_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_CREATE_INDEX_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_CREATE_PROCEDURE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_CREATE_SERVER_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_CREATE_TABLESPACE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_CREATE_TRIGGER_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_DROP_DATABASE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_DROP_EVENT_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_DROP_FUNCTION_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_DROP_INDEX_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_DROP_LOGFILE_GROUP_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_DROP_PROCEDURE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_DROP_SERVER_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_DROP_TABLESPACE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_DROP_TRIGGER_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_RENAME_DATABASE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_RENAME_SCHEMA_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_RENAME_TABLE_STATEMENT;
import java.util.ArrayList;
import java.util.List;
import org.modeshape.common.text.ParsingException;
import org.modeshape.sequencer.ddl.DdlTokenStream;
import org.modeshape.sequencer.ddl.StandardDdlParser;
import org.modeshape.sequencer.ddl.datatype.DataType;
import org.modeshape.sequencer.ddl.datatype.DataTypeParser;
import org.modeshape.sequencer.ddl.node.AstNode;

/**
 * MySql-specific DDL Parser. Includes custom data types as well as custom DDL statements.
 */
public class MySqlDdlParser extends StandardDdlParser implements MySqlDdlConstants, MySqlDdlConstants.MySqlStatementStartPhrases {
    private final String parserId = "MYSQL";

    static List<String[]> mysqlDataTypeStrings = new ArrayList<String[]>();
    /*
    * ===========================================================================================================================
    	* Data Definition Statements
    	ALTER [DATABASE | EVENT | FUNCTION | SERVER | TABLE | VIEW]
    	CREATE [DATABASE | EVENT | FUNCTION | INDEX | PROCEDURE | SERVER | TABLE | TRIGGER | VIEW]
    	DROP [DATABASE | EVENT | FUNCTION | INDEX | PROCEDURE | SERVER | TABLE | TRIGGER | VIEW]
    	RENAME TABLE
    */

    /*
    * ===========================================================================================================================
    * CREATE TABLE
    
    CREATE [TEMPORARY] TABLE [IF NOT EXISTS] tbl_name
    (create_definition,...)
    [table_options]
    [partition_options]

    Or:
    
    CREATE [TEMPORARY] TABLE [IF NOT EXISTS] tbl_name
        [(create_definition,...)]
        [table_options]
        [partition_options]
        select_statement
    
    Or:
    
    CREATE [TEMPORARY] TABLE [IF NOT EXISTS] tbl_name
        { LIKE old_tbl_name | (LIKE old_tbl_name) }
    
    create_definition:
        col_name column_definition
      | [CONSTRAINT [symbol]] PRIMARY KEY [index_type] (index_col_name,...)
          [index_option] ...
      | {INDEX|KEY} [index_name] [index_type] (index_col_name,...)
          [index_option] ...
      | [CONSTRAINT [symbol]] UNIQUE [INDEX|KEY]
          [index_name] [index_type] (index_col_name,...)
          [index_option] ...
      | {FULLTEXT|SPATIAL} [INDEX|KEY] [index_name] (index_col_name,...)
          [index_option] ...
      | [CONSTRAINT [symbol]] FOREIGN KEY
          [index_name] (index_col_name,...) reference_definition
      | CHECK (expr)
    
    column_definition:
        data_type [NOT NULL | NULL] [DEFAULT default_value]
          [AUTO_INCREMENT] [UNIQUE [KEY] | [PRIMARY] KEY]
          [COMMENT 'string']
          [COLUMN_FORMAT {FIXED|DYNAMIC|DEFAULT}]
          [STORAGE {DISK|MEMORY|DEFAULT}]
          [reference_definition]
    
    index_col_name:
        col_name [(length)] [ASC | DESC]
    
    index_type:
        USING {BTREE | HASH | RTREE}
    
    index_option:
        KEY_BLOCK_SIZE [=] value
      | index_type
      | WITH PARSER parser_name
    
    reference_definition:
        REFERENCES tbl_name (index_col_name,...)
          [MATCH FULL | MATCH PARTIAL | MATCH SIMPLE]
          [ON DELETE reference_option]
          [ON UPDATE reference_option]
    
    reference_option:
        RESTRICT | CASCADE | SET NULL | NO ACTION
    
    table_options:
        table_option [[,] table_option] ...
    
    table_option:
        ENGINE [=] engine_name
      | AUTO_INCREMENT [=] value
      | AVG_ROW_LENGTH [=] value
      | [DEFAULT] CHARACTER SET [=] charset_name
      | CHECKSUM [=] {0 | 1}
      | [DEFAULT] COLLATE [=] collation_name
      | COMMENT [=] 'string'
      | CONNECTION [=] 'connect_string'
      | DATA DIRECTORY [=] 'absolute path to directory'
      | DELAY_KEY_WRITE [=] {0 | 1}
      | INDEX DIRECTORY [=] 'absolute path to directory'
      | INSERT_METHOD [=] { NO | FIRST | LAST }
      | KEY_BLOCK_SIZE [=] value
      | MAX_ROWS [=] value
      | MIN_ROWS [=] value
      | PACK_KEYS [=] {0 | 1 | DEFAULT}
      | PASSWORD [=] 'string'
      | ROW_FORMAT [=] {DEFAULT|DYNAMIC|FIXED|COMPRESSED|REDUNDANT|COMPACT}
      | TABLESPACE tablespace_name [STORAGE {DISK|MEMORY|DEFAULT}]
      | UNION [=] (tbl_name[,tbl_name]...)
    
    partition_options:
        PARTITION BY
            { [LINEAR] HASH(expr)
            | [LINEAR] KEY(column_list)
            | RANGE(expr)
            | LIST(expr) }
        [PARTITIONS num]
        [SUBPARTITION BY
            { [LINEAR] HASH(expr)
            | [LINEAR] KEY(column_list) }
          [SUBPARTITIONS num]
        ]
        [(partition_definition [, partition_definition] ...)]
    
    partition_definition:
        PARTITION partition_name
            [VALUES {LESS THAN {(expr) | MAXVALUE} | IN (value_list)}]
            [[STORAGE] ENGINE [=] engine_name]
            [COMMENT [=] 'comment_text' ]
            [DATA DIRECTORY [=] 'data_dir']    	
            [INDEX DIRECTORY [=] 'index_dir']
            [MAX_ROWS [=] max_number_of_rows]
            [MIN_ROWS [=] min_number_of_rows]
            [TABLESPACE [=] tablespace_name]
            [NODEGROUP [=] node_group_id]
            [(subpartition_definition [, subpartition_definition] ...)]
    
    subpartition_definition:
        SUBPARTITION logical_name
            [[STORAGE] ENGINE [=] engine_name]
            [COMMENT [=] 'comment_text' ]
            [DATA DIRECTORY [=] 'data_dir']
            [INDEX DIRECTORY [=] 'index_dir']
            [MAX_ROWS [=] max_number_of_rows]
            [MIN_ROWS [=] min_number_of_rows]
            [TABLESPACE [=] tablespace_name]
            [NODEGROUP [=] node_group_id]
    
    select_statement:
        [IGNORE | REPLACE] [AS] SELECT ...   (Some legal select statement)

    
    * ===========================================================================================================================
    */
    private static final String TERMINATOR = DEFAULT_TERMINATOR;

    public MySqlDdlParser() {
        initialize();
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

    private void initialize() {
        setDatatypeParser(new MySqlDataTypeParser());

        setDoUseTerminator(true);

        setTerminator(TERMINATOR);

        mysqlDataTypeStrings.addAll(MySqlDataTypes.CUSTOM_DATATYPE_START_PHRASES);
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
        tokens.registerKeyWords(MySqlDataTypes.CUSTOM_DATATYPE_START_WORDS);
        tokens.registerStatementStartPhrase(ALTER_PHRASES);
        tokens.registerStatementStartPhrase(CREATE_PHRASES);
        tokens.registerStatementStartPhrase(DROP_PHRASES);
        tokens.registerStatementStartPhrase(MISC_PHRASES);
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

        if (tokens.matches(STMT_CREATE_INDEX)) {
            return parseStatement(tokens, MySqlStatementStartPhrases.STMT_CREATE_INDEX, parentNode, TYPE_CREATE_INDEX_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_UNIQUE_INDEX)) {
            return parseStatement(tokens,
                                  MySqlStatementStartPhrases.STMT_CREATE_UNIQUE_INDEX,
                                  parentNode,
                                  TYPE_CREATE_INDEX_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_FUNCTION)) {
            return parseStatement(tokens,
                                  MySqlStatementStartPhrases.STMT_CREATE_FUNCTION,
                                  parentNode,
                                  TYPE_CREATE_FUNCTION_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_PROCEDURE)) {
            return parseStatement(tokens,
                                  MySqlStatementStartPhrases.STMT_CREATE_PROCEDURE,
                                  parentNode,
                                  TYPE_CREATE_PROCEDURE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_SERVER)) {
            return parseStatement(tokens, MySqlStatementStartPhrases.STMT_CREATE_SERVER, parentNode, TYPE_CREATE_SERVER_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_TRIGGER)) {
            return parseStatement(tokens,
                                  MySqlStatementStartPhrases.STMT_CREATE_TRIGGER,
                                  parentNode,
                                  TYPE_CREATE_TRIGGER_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_EVENT)) {
            return parseStatement(tokens, MySqlStatementStartPhrases.STMT_CREATE_EVENT, parentNode, TYPE_CREATE_EVENT_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_TABLESPACE)) {
            return parseStatement(tokens,
                                  MySqlStatementStartPhrases.STMT_CREATE_TABLESPACE,
                                  parentNode,
                                  TYPE_CREATE_TABLESPACE_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_DEFINER)) {
            return parseStatement(tokens,
                                  MySqlStatementStartPhrases.STMT_CREATE_DEFINER,
                                  parentNode,
                                  TYPE_CREATE_DEFINER_STATEMENT);
        }

        return super.parseCreateStatement(tokens, parentNode);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseAlterStatement(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseAlterStatement( DdlTokenStream tokens,
                                           AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        if (tokens.matches(STMT_ALTER_ALGORITHM)) {
            return parseStatement(tokens, STMT_ALTER_ALGORITHM, parentNode, TYPE_ALTER_ALGORITHM_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_DATABASE)) {
            return parseStatement(tokens, STMT_ALTER_DATABASE, parentNode, TYPE_ALTER_DATABASE_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_DEFINER)) {
            return parseStatement(tokens, STMT_ALTER_DEFINER, parentNode, TYPE_ALTER_DEFINER_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_EVENT)) {
            return parseStatement(tokens, STMT_ALTER_EVENT, parentNode, TYPE_ALTER_EVENT_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_FUNCTION)) {
            return parseStatement(tokens, STMT_ALTER_FUNCTION, parentNode, TYPE_ALTER_FUNCTION_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_LOGFILE_GROUP)) {
            return parseStatement(tokens, STMT_ALTER_LOGFILE_GROUP, parentNode, TYPE_ALTER_LOGFILE_GROUP_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_PROCEDURE)) {
            return parseStatement(tokens, STMT_ALTER_PROCEDURE, parentNode, TYPE_ALTER_PROCEDURE_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_SCHEMA)) {
            return parseStatement(tokens, STMT_ALTER_SCHEMA, parentNode, TYPE_ALTER_SCHEMA_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_SERVER)) {
            return parseStatement(tokens, STMT_ALTER_SERVER, parentNode, TYPE_ALTER_SERVER_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_TABLESPACE)) {
            return parseStatement(tokens, STMT_ALTER_TABLESPACE, parentNode, TYPE_ALTER_TABLESPACE_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_SQL_SECURITY)) {
            return parseStatement(tokens, STMT_ALTER_SQL_SECURITY, parentNode, TYPE_ALTER_VIEW_STATEMENT);
        } else if (tokens.matches(STMT_ALTER_IGNORE_TABLE) || tokens.matches(STMT_ALTER_ONLINE_TABLE)
                   || tokens.matches(STMT_ALTER_ONLINE_IGNORE_TABLE) || tokens.matches(STMT_ALTER_OFFLINE_TABLE)
                   || tokens.matches(STMT_ALTER_OFFLINE_IGNORE_TABLE)) {
            return parseAlterTableStatement(tokens, parentNode);
        }

        return super.parseAlterStatement(tokens, parentNode);
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
        // TODO:
        //
        /*
         * 

        ALTER [ONLINE | OFFLINE] [IGNORE] TABLE tbl_name
        	alter_specification [, alter_specification] ...

        	alter_specification:
        	    table_options
        	  | ADD [COLUMN] col_name column_definition
        	        [FIRST | AFTER col_name ]
        	  | ADD [COLUMN] (col_name column_definition,...)
        	  | ADD {INDEX|KEY} [index_name]
        	        [index_type] (index_col_name,...) [index_option] ...
        	  | ADD [CONSTRAINT [symbol]] PRIMARY KEY
        	        [index_type] (index_col_name,...) [index_option] ...
        	  | ADD [CONSTRAINT [symbol]]
        	        UNIQUE [INDEX|KEY] [index_name]
        	        [index_type] (index_col_name,...) [index_option] ...
        	  | ADD FULLTEXT [INDEX|KEY] [index_name]
        	        (index_col_name,...) [index_option] ...
        	  | ADD SPATIAL [INDEX|KEY] [index_name]
        	        (index_col_name,...) [index_option] ...
        	  | ADD [CONSTRAINT [symbol]]
        	        FOREIGN KEY [index_name] (index_col_name,...)
        	        reference_definition
        	  | ALTER [COLUMN] col_name {SET DEFAULT literal | DROP DEFAULT}
        	  | CHANGE [COLUMN] old_col_name new_col_name column_definition
        	        [FIRST|AFTER col_name]
        	  | MODIFY [COLUMN] col_name column_definition
        	        [FIRST | AFTER col_name]
        	  | DROP [COLUMN] col_name
        	  | DROP PRIMARY KEY
        	  | DROP {INDEX|KEY} index_name
        	  | DROP FOREIGN KEY fk_symbol
        	  | DISABLE KEYS
        	  | ENABLE KEYS
        	  | RENAME [TO] new_tbl_name
        	  | ORDER BY col_name [, col_name] ...
        	  | CONVERT TO CHARACTER SET charset_name [COLLATE collation_name]
        	  | [DEFAULT] CHARACTER SET [=] charset_name [COLLATE [=] collation_name]
        	  | DISCARD TABLESPACE
        	  | IMPORT TABLESPACE
        	  | partition_options
        	  | ADD PARTITION (partition_definition)
        	  | DROP PARTITION partition_names
        	  | COALESCE PARTITION number
        	  | REORGANIZE PARTITION [partition_names INTO (partition_definitions)]
        	  | ANALYZE PARTITION partition_names
        	  | CHECK PARTITION partition_names
        	  | OPTIMIZE PARTITION partition_names
        	  | REBUILD PARTITION partition_names
        	  | REPAIR PARTITION partition_names
        	  | REMOVE PARTITIONING
         */

        return super.parseAlterTableStatement(tokens, parentNode);
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

        if (tokens.matches(STMT_RENAME_DATABASE)) {
            markStartOfStatement(tokens);

            // RENAME DATABASE db_name TO new_db_name;
            tokens.consume(STMT_RENAME_DATABASE);
            String oldName = parseName(tokens);
            tokens.consume("TO");
            AstNode node = nodeFactory().node(oldName, parentNode, TYPE_RENAME_DATABASE_STATEMENT);
            String newName = parseName(tokens);
            node.setProperty(NEW_NAME, newName);

            markEndOfStatement(tokens, node);
            return node;
        } else if (tokens.matches(STMT_RENAME_SCHEMA)) {
            markStartOfStatement(tokens);

            // RENAME SCHEMA schema_name TO new_schema_name;
            tokens.consume(STMT_RENAME_SCHEMA);
            String oldName = parseName(tokens);
            tokens.consume("TO");
            AstNode node = nodeFactory().node(oldName, parentNode, TYPE_RENAME_SCHEMA_STATEMENT);
            String newName = parseName(tokens);
            node.setProperty(NEW_NAME, newName);

            markEndOfStatement(tokens, node);
            return node;
        } else if (tokens.matches(STMT_RENAME_TABLE)) {
            markStartOfStatement(tokens);

            // RENAME TABLE old_table TO tmp_table,
            // new_table TO old_table,
            // tmp_table TO new_table;
            tokens.consume(STMT_RENAME_TABLE);

            String oldName = parseName(tokens);
            tokens.consume("TO");
            String newName = parseName(tokens);

            AstNode node = nodeFactory().node(oldName, parentNode, TYPE_RENAME_TABLE_STATEMENT);
            node.setProperty(NEW_NAME, newName);

            // IF NOT MULTIPLE RENAMES, FINISH AND RETURN
            if (!tokens.matches(COMMA)) {
                markEndOfStatement(tokens, node);
                return node;
            }

            // Assume multiple renames

            // Create list of nodes so we can re-set the expression of each to reflect ONE rename.
            List<AstNode> nodes = new ArrayList<AstNode>();
            nodes.add(node);

            while (tokens.matches(COMMA)) {
                tokens.consume(COMMA);
                oldName = parseName(tokens);
                tokens.consume("TO");
                newName = parseName(tokens);
                node = nodeFactory().node(oldName, parentNode, TYPE_RENAME_TABLE_STATEMENT);
                node.setProperty(NEW_NAME, newName);
                nodes.add(node);
            }

            markEndOfStatement(tokens, nodes.get(0));

            String originalExpression = (String)nodes.get(0).getProperty(DDL_EXPRESSION).getFirstValue();
            Object startLineNumber = nodes.get(0).getProperty(DDL_START_LINE_NUMBER).getFirstValue();
            Object startColumnNumber = nodes.get(0).getProperty(DDL_START_COLUMN_NUMBER).getFirstValue();
            Object startCharIndex = nodes.get(0).getProperty(DDL_START_CHAR_INDEX).getFirstValue();

            for (AstNode nextNode : nodes) {
                oldName = nextNode.getName().getString();
                newName = (String)nextNode.getProperty(NEW_NAME).getFirstValue();
                String express = "RENAME TABLE" + SPACE + oldName + SPACE + "TO" + SPACE + newName + SEMICOLON;
                nextNode.setProperty(DDL_EXPRESSION, express);
                nextNode.setProperty(DDL_ORIGINAL_EXPRESSION, originalExpression);
                nextNode.setProperty(DDL_START_LINE_NUMBER, startLineNumber);
                nextNode.setProperty(DDL_START_COLUMN_NUMBER, startColumnNumber);
                nextNode.setProperty(DDL_START_CHAR_INDEX, startCharIndex);
            }

            return nodes.get(0);
        }

        return super.parseCustomStatement(tokens, parentNode);
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

        if (tokens.matches(STMT_DROP_DATABASE)) {
            return parseStatement(tokens, STMT_DROP_DATABASE, parentNode, TYPE_DROP_DATABASE_STATEMENT);
        } else if (tokens.matches(STMT_DROP_EVENT)) {
            return parseStatement(tokens, STMT_DROP_EVENT, parentNode, TYPE_DROP_EVENT_STATEMENT);
        } else if (tokens.matches(STMT_DROP_FUNCTION)) {
            return parseStatement(tokens, STMT_DROP_FUNCTION, parentNode, TYPE_DROP_FUNCTION_STATEMENT);
        } else if (tokens.matches(STMT_DROP_INDEX)) {
            return parseStatement(tokens, STMT_DROP_INDEX, parentNode, TYPE_DROP_INDEX_STATEMENT);
        } else if (tokens.matches(STMT_DROP_OFFLINE_INDEX)) {
            return parseStatement(tokens, STMT_DROP_OFFLINE_INDEX, parentNode, TYPE_DROP_INDEX_STATEMENT);
        } else if (tokens.matches(STMT_DROP_ONLINE_INDEX)) {
            return parseStatement(tokens, STMT_DROP_ONLINE_INDEX, parentNode, TYPE_DROP_INDEX_STATEMENT);
        } else if (tokens.matches(STMT_DROP_LOGFILE_GROUP)) {
            return parseStatement(tokens, STMT_DROP_LOGFILE_GROUP, parentNode, TYPE_DROP_LOGFILE_GROUP_STATEMENT);
        } else if (tokens.matches(STMT_DROP_PROCEDURE)) {
            return parseStatement(tokens, STMT_DROP_PROCEDURE, parentNode, TYPE_DROP_PROCEDURE_STATEMENT);
        } else if (tokens.matches(STMT_DROP_SERVER)) {
            return parseStatement(tokens, STMT_DROP_SERVER, parentNode, TYPE_DROP_SERVER_STATEMENT);
        } else if (tokens.matches(STMT_DROP_TABLESPACE)) {
            return parseStatement(tokens, STMT_DROP_TABLESPACE, parentNode, TYPE_DROP_TABLESPACE_STATEMENT);
        } else if (tokens.matches(STMT_DROP_TRIGGER)) {
            return parseStatement(tokens, STMT_DROP_TRIGGER, parentNode, TYPE_DROP_TRIGGER_STATEMENT);
        }

        return super.parseDropStatement(tokens, parentNode);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#getDataTypeStartWords()
     */
    @Override
    protected List<String> getCustomDataTypeStartWords() {
        return MySqlDataTypes.CUSTOM_DATATYPE_START_WORDS;
    }

    // ===========================================================================================================================
    // ===========================================================================================================================
    class MySqlDataTypeParser extends DataTypeParser implements MySqlDdlConstants.MySqlDataTypes {

        // NOTE THAT MYSQL allows "UNSIGNED" and "ZEROFILL" as options AFTER the datatype definition
        // Need to override and do a CHECK for and CONSUME them.

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.sequencer.ddl.datatype.DataTypeParser#isCustomDataType(org.modeshape.sequencer.ddl.DdlTokenStream)
         */
        @Override
        protected boolean isCustomDataType( DdlTokenStream tokens ) throws ParsingException {
            // Loop through the registered statement start string arrays and look for exact matches.

            for (String[] stmts : mysqlDataTypeStrings) {
                if (tokens.matches(stmts)) return true;
            }
            return super.isCustomDataType(tokens);
        }

        @Override
        protected DataType parseApproxNumericType( DdlTokenStream tokens ) throws ParsingException {
            DataType dType = super.parseApproxNumericType(tokens);
            tokens.canConsume("UNSIGNED");
            tokens.canConsume("ZEROFILL");
            tokens.canConsume("UNSIGNED");
            return dType;
        }

        @Override
        protected DataType parseBitStringType( DdlTokenStream tokens ) throws ParsingException {
            return super.parseBitStringType(tokens);
        }

        @Override
        protected DataType parseCharStringType( DdlTokenStream tokens ) throws ParsingException {
            DataType result = super.parseCharStringType(tokens);

            tokens.canConsume("FOR", "BIT", "DATA");

            return result;
        }

        @Override
        protected DataType parseCustomType( DdlTokenStream tokens ) throws ParsingException {
            DataType dataType = null;

            if (tokens.matches(DTYPE_FIXED) || tokens.matches(DTYPE_DOUBLE)) {
                dataType = new DataType();
                String typeName = tokens.consume();
                dataType.setName(typeName);

                int precision = 0;
                int scale = 0;

                if (tokens.matches(L_PAREN)) {
                    consume(tokens, dataType, false, L_PAREN);
                    precision = (int)parseLong(tokens, dataType);
                    if (tokens.canConsume(COMMA)) {
                        scale = (int)parseLong(tokens, dataType);
                    } else {
                        scale = getDefaultScale();
                    }
                    tokens.consume(R_PAREN);
                } else {
                    precision = getDefaultPrecision();
                    scale = getDefaultScale();
                }
                dataType.setPrecision(precision);
                dataType.setScale(scale);
            } else if (tokens.matches(DTYPE_MEDIUMBLOB) || tokens.matches(DTYPE_LONGBLOB) || tokens.matches(DTYPE_BLOB)
                       || tokens.matches(DTYPE_TINYBLOB) || tokens.matches(DTYPE_YEAR) || tokens.matches(DTYPE_DATETIME)
                       || tokens.matches(DTYPE_BOOLEAN) || tokens.matches(DTYPE_BOOL)) {
                String typeName = tokens.consume();
                dataType = new DataType(typeName);
            } else if (tokens.matches(DTYPE_MEDIUMINT) || tokens.matches(DTYPE_TINYINT) || tokens.matches(DTYPE_VARBINARY)
                       || tokens.matches(DTYPE_BINARY) || tokens.matches(DTYPE_BIGINT)) {
                String typeName = tokens.consume();
                dataType = new DataType(typeName);
                long length = getDefaultLength();
                if (tokens.matches(L_PAREN)) {
                    length = parseBracketedLong(tokens, dataType);
                }
                dataType.setLength(length);
            } else if (tokens.matches(DTYPE_NATIONAL_VARCHAR)) {
                String typeName = getStatementTypeName(DTYPE_NATIONAL_VARCHAR);
                dataType = new DataType(typeName);
                tokens.consume(DTYPE_NATIONAL_VARCHAR);
                long length = getDefaultLength();
                if (tokens.matches(L_PAREN)) {
                    length = parseBracketedLong(tokens, dataType);
                }
                dataType.setLength(length);
            } else if (tokens.matches(DTYPE_MEDIUMTEXT) || tokens.matches(DTYPE_TEXT) || tokens.matches(DTYPE_LONGTEXT)
                       || tokens.matches(DTYPE_TINYTEXT)) {
                String typeName = tokens.consume();
                dataType = new DataType(typeName);
                tokens.canConsume("BINARY");
                tokens.canConsume("COLLATE", DdlTokenStream.ANY_VALUE);
                tokens.canConsume("CHARACTER", "SET", DdlTokenStream.ANY_VALUE);
                tokens.canConsume("COLLATE", DdlTokenStream.ANY_VALUE);
            } else if (tokens.matches(DTYPE_SET)) {
                // SET(value1,value2,value3,...) [CHARACTER SET charset_name] [COLLATE collation_name]
                String typeName = tokens.consume();
                dataType = new DataType(typeName);

                tokens.consume(L_PAREN);
                do {
                    tokens.consume();
                } while (tokens.canConsume(COMMA));
                tokens.consume(R_PAREN);

                tokens.canConsume("COLLATE", DdlTokenStream.ANY_VALUE);
                tokens.canConsume("CHARACTER", "SET", DdlTokenStream.ANY_VALUE);
                tokens.canConsume("COLLATE", DdlTokenStream.ANY_VALUE);
            } else if (tokens.matches(DTYPE_ENUM)) {
                // ENUM(value1,value2,value3,...) [CHARACTER SET charset_name] [COLLATE collation_name]
                String typeName = tokens.consume();
                dataType = new DataType(typeName);

                tokens.consume(L_PAREN);
                do {
                    tokens.consume();
                } while (tokens.canConsume(COMMA));
                tokens.consume(R_PAREN);

                tokens.canConsume("COLLATE", DdlTokenStream.ANY_VALUE);
                tokens.canConsume("CHARACTER", "SET", DdlTokenStream.ANY_VALUE);
                tokens.canConsume("COLLATE", DdlTokenStream.ANY_VALUE);
            }

            if (dataType == null) {
                dataType = super.parseCustomType(tokens);
            }

            // LOOKING for possible [UNSIGNED] [ZEROFILL] options

            tokens.canConsume("UNSIGNED");
            tokens.canConsume("ZEROFILL");
            tokens.canConsume("UNSIGNED");

            return dataType;
        }

        @Override
        protected DataType parseDateTimeType( DdlTokenStream tokens ) throws ParsingException {
            return super.parseDateTimeType(tokens);
        }

        @Override
        protected DataType parseExactNumericType( DdlTokenStream tokens ) throws ParsingException {
            DataType dType = super.parseExactNumericType(tokens);
            tokens.canConsume("UNSIGNED");
            tokens.canConsume("ZEROFILL");
            tokens.canConsume("UNSIGNED");
            return dType;
        }

    }

}
