package org.jboss.dna.sequencer.ddl.dialect.derby;

import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.COLUMN_ATTRIBUTE_TYPE;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.PROPERTY_VALUE;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_ALTER_COLUMN_DEFINITION;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_ALTER_TABLE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_COLUMN_DEFINITION;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_COLUMN_DEFINITION;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_TABLE_CONSTRAINT_DEFINITION;
import static org.jboss.dna.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TABLE_NAME;
import static org.jboss.dna.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_CREATE_FUNCTION_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_CREATE_INDEX_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_CREATE_PROCEDURE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_CREATE_ROLE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_CREATE_SYNONYM_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_CREATE_TRIGGER_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_DROP_FUNCTION_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_DROP_INDEX_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_DROP_PROCEDURE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_DROP_ROLE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_DROP_SYNONYM_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_DROP_TRIGGER_STATEMENT;
import static org.jboss.dna.sequencer.ddl.dialect.derby.DerbyDdlLexicon.UNIQUE_INDEX;
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
 * Derby-specific DDL Parser. Includes custom data types as well as custom DDL statements.
 */
public class DerbyDdlParser extends StandardDdlParser implements DerbyDdlConstants {
    private final String parserId = "DERBY";

    static List<String[]> derbyDataTypeStrings = new ArrayList<String[]>();

    private static final String TERMINATOR = DEFAULT_TERMINATOR;

    public DerbyDdlParser() {
        setDatatypeParser(new DerbyDataTypeParser());

        initialize();
    }

    private void initialize() {

        setDoUseTerminator(true);

        setTerminator(TERMINATOR);

        derbyDataTypeStrings.addAll(DerbyDataTypes.CUSTOM_DATATYPE_START_PHRASES);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.sequencer.ddl.StandardDdlParser#registerWords(org.jboss.dna.sequencer.ddl.DdlTokenStream)
     */
    @Override
    public void registerWords( DdlTokenStream tokens ) {
        tokens.registerKeyWords(CUSTOM_KEYWORDS);

        tokens.registerStatementStartPhrase(DerbyStatementStartPhrases.ALTER_PHRASES);
        tokens.registerStatementStartPhrase(DerbyStatementStartPhrases.CREATE_PHRASES);
        tokens.registerStatementStartPhrase(DerbyStatementStartPhrases.DROP_PHRASES);
        tokens.registerStatementStartPhrase(DerbyStatementStartPhrases.SET_PHRASES);
        tokens.registerStatementStartPhrase(DerbyStatementStartPhrases.MISC_PHRASES);
        super.registerWords(tokens);
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
     * @see org.jboss.dna.sequencer.ddl.StandardDdlParser#getValidSchemaChildTypes()
     */
    @Override
    protected Name[] getValidSchemaChildTypes() {
        return DerbyStatementStartPhrases.VALID_SCHEMA_CHILD_STMTS;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.sequencer.ddl.StandardDdlParser#parseCustomStatement(org.jboss.dna.sequencer.ddl.DdlTokenStream,
     *      org.jboss.dna.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseCustomStatement( DdlTokenStream tokens,
                                            AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        AstNode result = super.parseCustomStatement(tokens, parentNode);
        if (result == null) {
            if (tokens.matches(DerbyStatementStartPhrases.STMT_LOCK_TABLE)) {
                markStartOfStatement(tokens);
                tokens.consume(DerbyStatementStartPhrases.STMT_LOCK_TABLE);
                result = parseIgnorableStatement(tokens,
                                                 getStatementTypeName(DerbyStatementStartPhrases.STMT_LOCK_TABLE),
                                                 parentNode);
                markEndOfStatement(tokens, result);
            } else if (tokens.matches(DerbyStatementStartPhrases.STMT_RENAME_TABLE)) {
                markStartOfStatement(tokens);
                tokens.consume(DerbyStatementStartPhrases.STMT_RENAME_TABLE);
                result = parseIgnorableStatement(tokens,
                                                 getStatementTypeName(DerbyStatementStartPhrases.STMT_RENAME_TABLE),
                                                 parentNode);
                markEndOfStatement(tokens, result);
            } else if (tokens.matches(DerbyStatementStartPhrases.STMT_RENAME_INDEX)) {
                markStartOfStatement(tokens);
                tokens.consume(DerbyStatementStartPhrases.STMT_RENAME_INDEX);
                result = parseIgnorableStatement(tokens,
                                                 getStatementTypeName(DerbyStatementStartPhrases.STMT_RENAME_INDEX),
                                                 parentNode);
                markEndOfStatement(tokens, result);
            } else if (tokens.matches(DerbyStatementStartPhrases.STMT_DECLARE_GLOBAL_TEMP_TABLE)) {
                markStartOfStatement(tokens);
                tokens.consume(DerbyStatementStartPhrases.STMT_DECLARE_GLOBAL_TEMP_TABLE);
                result = parseIgnorableStatement(tokens,
                                                 getStatementTypeName(DerbyStatementStartPhrases.STMT_DECLARE_GLOBAL_TEMP_TABLE),
                                                 parentNode);
                markEndOfStatement(tokens, result);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.sequencer.ddl.StandardDdlParser#parseCreateStatement(org.jboss.dna.sequencer.ddl.DdlTokenStream,
     *      org.jboss.dna.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseCreateStatement( DdlTokenStream tokens,
                                            AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        if (tokens.matches(DerbyStatementStartPhrases.STMT_CREATE_INDEX)
            || tokens.matches(DerbyStatementStartPhrases.STMT_CREATE_UNIQUE_INDEX)) {
            return parseCreateIndex(tokens, parentNode);
        } else if (tokens.matches(DerbyStatementStartPhrases.STMT_CREATE_FUNCTION)) {
            return parseStatement(tokens,
                                  DerbyStatementStartPhrases.STMT_CREATE_FUNCTION,
                                  parentNode,
                                  TYPE_CREATE_FUNCTION_STATEMENT);
        } else if (tokens.matches(DerbyStatementStartPhrases.STMT_CREATE_PROCEDURE)) {
            return parseStatement(tokens,
                                  DerbyStatementStartPhrases.STMT_CREATE_PROCEDURE,
                                  parentNode,
                                  TYPE_CREATE_PROCEDURE_STATEMENT);
        } else if (tokens.matches(DerbyStatementStartPhrases.STMT_CREATE_ROLE)) {
            return parseStatement(tokens, DerbyStatementStartPhrases.STMT_CREATE_ROLE, parentNode, TYPE_CREATE_ROLE_STATEMENT);
        } else if (tokens.matches(DerbyStatementStartPhrases.STMT_CREATE_SYNONYM)) {
            return parseStatement(tokens,
                                  DerbyStatementStartPhrases.STMT_CREATE_SYNONYM,
                                  parentNode,
                                  TYPE_CREATE_SYNONYM_STATEMENT);
        } else if (tokens.matches(DerbyStatementStartPhrases.STMT_CREATE_TRIGGER)) {
            return parseStatement(tokens,
                                  DerbyStatementStartPhrases.STMT_CREATE_TRIGGER,
                                  parentNode,
                                  TYPE_CREATE_TRIGGER_STATEMENT);
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

        parseUntilTerminator(tokens);

        markEndOfStatement(tokens, indexNode);

        return indexNode;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.sequencer.ddl.StandardDdlParser#parseDropStatement(org.jboss.dna.sequencer.ddl.DdlTokenStream,
     *      org.jboss.dna.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseDropStatement( DdlTokenStream tokens,
                                          AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        AstNode dropNode = null;

        String name = null;

        if (tokens.matches(DerbyStatementStartPhrases.STMT_DROP_FUNCTION)) {
            markStartOfStatement(tokens);
            tokens.consume(DerbyStatementStartPhrases.STMT_DROP_FUNCTION);
            name = parseName(tokens);
            dropNode = nodeFactory().node(name, parentNode, TYPE_DROP_FUNCTION_STATEMENT);
        } else if (tokens.matches(DerbyStatementStartPhrases.STMT_DROP_INDEX)) {
            markStartOfStatement(tokens);
            tokens.consume(DerbyStatementStartPhrases.STMT_DROP_INDEX);
            name = parseName(tokens);
            dropNode = nodeFactory().node(name, parentNode, TYPE_DROP_INDEX_STATEMENT);
        } else if (tokens.matches(DerbyStatementStartPhrases.STMT_DROP_PROCEDURE)) {
            markStartOfStatement(tokens);
            tokens.consume(DerbyStatementStartPhrases.STMT_DROP_PROCEDURE);
            name = parseName(tokens);
            dropNode = nodeFactory().node(name, parentNode, TYPE_DROP_PROCEDURE_STATEMENT);

            // CREATE PROCEDURE procedure-Name ( [ ProcedureParameter [, ProcedureParameter] ] * ) [ ProcedureElement ] *
            // ProcedureParameter:
            // [ { IN | OUT | INOUT } ] [ parameter-Name ] DataType
            // ProcedureElement:
            //
            // {
            // | [ DYNAMIC ] RESULT SETS INTEGER
            // | LANGUAGE { JAVA }
            // | DeterministicCharacteristic
            // | EXTERNAL NAME string
            // | PARAMETER STYLE JAVA
            // | { NO SQL | MODIFIES SQL DATA | CONTAINS SQL | READS SQL DATA }
            // }

            // TODO: BARRY
        } else if (tokens.matches(DerbyStatementStartPhrases.STMT_DROP_ROLE)) {
            markStartOfStatement(tokens);
            tokens.consume(DerbyStatementStartPhrases.STMT_DROP_ROLE);
            name = parseName(tokens);
            dropNode = nodeFactory().node(name, parentNode, TYPE_DROP_ROLE_STATEMENT);
        } else if (tokens.matches(DerbyStatementStartPhrases.STMT_DROP_SYNONYM)) {
            markStartOfStatement(tokens);
            tokens.consume(DerbyStatementStartPhrases.STMT_DROP_SYNONYM);
            name = parseName(tokens);
            dropNode = nodeFactory().node(name, parentNode, TYPE_DROP_SYNONYM_STATEMENT);
            // CREATE SYNONYM synonym-Name FOR { view-Name | table-Name }

            // TODO: BARRY
        } else if (tokens.matches(DerbyStatementStartPhrases.STMT_DROP_TRIGGER)) {
            markStartOfStatement(tokens);
            tokens.consume(DerbyStatementStartPhrases.STMT_DROP_TRIGGER);
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
     * @see org.jboss.dna.sequencer.ddl.StandardDdlParser#parseGrantStatement(org.jboss.dna.sequencer.ddl.DdlTokenStream,
     *      org.jboss.dna.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseGrantStatement( DdlTokenStream tokens,
                                           AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        return super.parseGrantStatement(tokens, parentNode);
        // Statement stmt = null;
        //		
        // if( tokens.matches(GRANT, DdlTokenStream.ANY_VALUE, "TO")) {
        // stmt = new TypedStatement();
        // consume(tokens, stmt, false, GRANT);
        // String privilege = consume(tokens, stmt, true);
        // tokens.consume("TO");
        // String toValue = consume(tokens, stmt, true);
        //
        // String value = parseUntilTerminator(tokens);
        // stmt.appendSource(true, value);
        // stmt.setType("GRANT" + SPACE + privilege + SPACE + "TO" + SPACE + toValue);
        // consumeTerminator(tokens);
        // return stmt;
        // } else if( tokens.matches(GRANT, DdlTokenStream.ANY_VALUE, "ON")) {
        // stmt = new TypedStatement();
        // consume(tokens, stmt, false, GRANT);
        // String privilege = consume(tokens, stmt, true);
        // tokens.consume("ON");
        //			
        // tokens.canConsume("TABLE");
        //			
        // String onValue = tokens.consume();
        //			
        // String value = parseUntilTerminator(tokens);
        // stmt.appendSource(true, value);
        // stmt.setType("GRANT" + SPACE + privilege + SPACE + "ON" + SPACE + onValue);
        // consumeTerminator(tokens);
        // return stmt;
        // } else if( tokens.matches(GRANT, "ALL", "PRIVILEGES", "ON")) {
        // stmt = new TypedStatement();
        // consume(tokens, stmt, false, GRANT);
        // String privilege = consume(tokens, stmt, true,"ALL", "PRIVILEGES", "ON");
        // tokens.canConsume("TABLE");
        //			
        // String onValue = tokens.consume();
        //			
        // String value = parseUntilTerminator(tokens);
        // stmt.appendSource(true, value);
        // stmt.setType("GRANT" + SPACE + privilege + SPACE + "ON" + SPACE + onValue);
        // consumeTerminator(tokens);
        // return stmt;
        // } else if( tokens.matches(GRANT, "SELECT") ||
        // tokens.matches(GRANT, "UPDATE") ||
        // tokens.matches(GRANT, "DELETE") ||
        // tokens.matches(GRANT, "INSERT") ||
        // tokens.matches(GRANT, "TRIGGER") ||
        // tokens.matches(GRANT, "REFERENCES")) {
        // stmt = new TypedStatement();
        // consume(tokens, stmt, false, GRANT);
        //			
        // String nextTok = consume(tokens, stmt, true) + SPACE + consume(tokens, stmt, true) + SPACE + consume(tokens, stmt,
        // true);
        //
        // String value = parseUntilTerminator(tokens);
        // stmt.appendSource(true, value);
        // stmt.setType("GRANT" + SPACE + nextTok);
        // consumeTerminator(tokens);
        // return stmt;
        // }
        //		
        //		
        // return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.sequencer.ddl.StandardDdlParser#parseAlterTableStatement(org.jboss.dna.sequencer.ddl.DdlTokenStream,
     *      org.jboss.dna.sequencer.ddl.node.AstNode)
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
     * @see org.jboss.dna.sequencer.ddl.StandardDdlParser#parseColumnDefinition(org.jboss.dna.sequencer.ddl.DdlTokenStream,
     *      org.jboss.dna.sequencer.ddl.node.AstNode, boolean)
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

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.sequencer.ddl.StandardDdlParser#getDataTypeStartWords()
     */
    @Override
    protected List<String> getCustomDataTypeStartWords() {
        return DerbyDataTypes.CUSTOM_DATATYPE_START_WORDS;
    }

    class DerbyDataTypeParser extends DataTypeParser {

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.sequencer.ddl.datatype.DataTypeParser#isCustomDataType(org.jboss.dna.sequencer.ddl.DdlTokenStream)
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
         * @see org.jboss.dna.sequencer.ddl.datatype.DataTypeParser#parseApproxNumericType(org.jboss.dna.sequencer.ddl.DdlTokenStream)
         */
        @Override
        protected DataType parseApproxNumericType( DdlTokenStream tokens ) throws ParsingException {
            return super.parseApproxNumericType(tokens);
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

            canConsume(tokens, result, true, "FOR", "BIT", "DATA");

            return result;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.sequencer.ddl.datatype.DataTypeParser#parseCustomType(org.jboss.dna.sequencer.ddl.DdlTokenStream)
         */
        @Override
        protected DataType parseCustomType( DdlTokenStream tokens ) throws ParsingException {
            DataType dataType = null;
            String typeName = null;
            int length = 0;

            if (tokens.matches(DerbyDataTypes.DTYPE_BINARY_LARGE_OBJECT)
                || tokens.matches(DerbyDataTypes.DTYPE_CHARACTER_LARGE_OBJECT)) {
                dataType = new DataType();
                typeName = consume(tokens, dataType, true) + SPACE + consume(tokens, dataType, true) + SPACE
                           + consume(tokens, dataType, true);
                boolean isKMGLength = false;
                String kmgValue = null;
                if (canConsume(tokens, dataType, true, L_PAREN)) {
                    String lengthValue = consume(tokens, dataType, false);
                    kmgValue = getKMG(lengthValue);

                    isKMGLength = isKMGInteger(lengthValue);

                    length = parseInteger(lengthValue);

                    consume(tokens, dataType, true, R_PAREN);
                }

                dataType.setName(typeName);
                dataType.setLength(length);
                dataType.setKMGLength(isKMGLength);
                dataType.setKMGValue(kmgValue);
            } else if (tokens.matches(DerbyDataTypes.DTYPE_CLOB) || tokens.matches(DerbyDataTypes.DTYPE_BLOB)) {
                dataType = new DataType();
                typeName = consume(tokens, dataType, true);
                boolean isKMGLength = false;
                String kmgValue = null;
                if (canConsume(tokens, dataType, true, L_PAREN)) {
                    String lengthValue = consume(tokens, dataType, false);
                    kmgValue = getKMG(lengthValue);

                    isKMGLength = isKMGInteger(lengthValue);

                    length = parseInteger(lengthValue);

                    consume(tokens, dataType, true, R_PAREN);
                }

                dataType.setName(typeName);
                dataType.setLength(length);
                dataType.setKMGLength(isKMGLength);
                dataType.setKMGValue(kmgValue);
            } else if (tokens.matches(DerbyDataTypes.DTYPE_BIGINT)) {
                typeName = consume(tokens, dataType, true);
                dataType = new DataType(typeName);
            } else if (tokens.matches(DerbyDataTypes.DTYPE_LONG_VARCHAR_FBD)) {
                typeName = consume(tokens, dataType, true) + SPACE + consume(tokens, dataType, true) + SPACE
                           + consume(tokens, dataType, true) + SPACE + consume(tokens, dataType, true) + SPACE
                           + consume(tokens, dataType, true);
                dataType = new DataType(typeName);
            } else if (tokens.matches(DerbyDataTypes.DTYPE_LONG_VARCHAR)) {
                typeName = consume(tokens, dataType, true) + SPACE + consume(tokens, dataType, true);
                typeName = consume(tokens, dataType, true);
                dataType = new DataType(typeName);
            } else if (tokens.matches(DerbyDataTypes.DTYPE_DOUBLE)) {
                typeName = consume(tokens, dataType, true);
                dataType = new DataType(typeName);
            } else if (tokens.matches(DerbyDataTypes.DTYPE_XML)) {
                typeName = consume(tokens, dataType, true);
                dataType = new DataType(typeName);
            }

            if (dataType == null) {
                super.parseCustomType(tokens);
            }
            return dataType;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.sequencer.ddl.datatype.DataTypeParser#parseDateTimeType(org.jboss.dna.sequencer.ddl.DdlTokenStream)
         */
        @Override
        protected DataType parseDateTimeType( DdlTokenStream tokens ) throws ParsingException {
            return super.parseDateTimeType(tokens);
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
