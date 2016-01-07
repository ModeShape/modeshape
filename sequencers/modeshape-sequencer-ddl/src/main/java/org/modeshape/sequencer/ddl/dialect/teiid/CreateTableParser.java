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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.text.Position;
import org.modeshape.common.util.StringUtil;
import org.modeshape.sequencer.ddl.DdlTokenStream;
import org.modeshape.sequencer.ddl.StandardDdlLexicon;
import org.modeshape.sequencer.ddl.datatype.DataType;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.DdlStatement;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.SchemaElementType;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.TeiidFutureReserveWord;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.TeiidNonReservedWord;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.TeiidReservedWord;
import org.modeshape.sequencer.ddl.node.AstNode;

/**
 * A parser for the Teiid <create table> DDL statement.
 * <p>
 * <code>
 * CREATE ( FOREIGN TABLE | ( VIRTUAL )? VIEW ) <identifier> <create table body> ( AS <query expression> )?
 * </code>
 */
final class CreateTableParser extends StatementParser {

    protected static final String PRIMARY_KEY_PREFIX = "PK_";
    protected static final String FOREIGN_KEY_PREFIX = "FK_";
    protected static final String UNIQUE_CONSTRAINT_PREFIX = "UC_";
    protected static final String INDEX_PREFIX = "NDX_";
    protected static final String ACCESS_PATTERN_PREFIX = "AP_";

    /**
     * Sequence is any number of characters, word boundary, column name, word boundary, and any number of characters.
     */
    private static final String REGEX = ".*\\b(?i)%s(?-i)\\b.*";

    private List<UnresolvedTableReferenceNode> unresolvedTableReferences;

    /**
     * @param expression the expression being looked at (cannot be <code>null</code>)
     * @param columnName the name of the column being looked for in the expression (cannot be <code>null</code>)
     * @return <code>true</code> if the expression contains the column name
     */
    static boolean contains( final String expression,
                             final String columnName ) {
        return expression.matches(String.format(REGEX, columnName));
    }

    CreateTableParser( final TeiidDdlParser teiidDdlParser ) {
        super(teiidDdlParser);
        unresolvedTableReferences = new ArrayList<UnresolvedTableReferenceNode>(10);
    }

    private String createConstraintName( final String constraintType,
                                         final AstNode tableNode ) {
        String prefix = null;

        if (PRIMARY_KEY.equals(constraintType)) {
            prefix = PRIMARY_KEY_PREFIX;
        } else if (FOREIGN_KEY.equals(constraintType)) {
            prefix = FOREIGN_KEY_PREFIX;
        } else if (TeiidReservedWord.UNIQUE.toDdl().equals(constraintType)) {
            prefix = UNIQUE_CONSTRAINT_PREFIX;
        } else if (TeiidNonReservedWord.INDEX.toDdl().equals(constraintType)) {
            prefix = INDEX_PREFIX;
        } else if (TeiidNonReservedWord.ACCESSPATTERN.toDdl().equals(constraintType)) {
            prefix = ACCESS_PATTERN_PREFIX;
        }

        int index = 1;

        while (true) {
            final String name = prefix + index;
            final List<AstNode> kids = tableNode.childrenWithName(name);

            if (kids.isEmpty()) {
                return name;
            }

            ++index;
        }
    }

    /**
     * @param tableNode the table node whose specified column node child is being requested (cannot be <code>null</code>)
     * @param columnName the name of the column node being requested (cannot be <code>null</code> or empty)
     * @return the column node or <code>null</code> if not found
     */
    private AstNode getColumnNode( final AstNode tableNode,
                                   final String columnName ) {
        assert (tableNode != null);
        assert (columnName != null);

        final List<AstNode> kids = tableNode.childrenWithName(columnName);

        // not found
        if (kids.isEmpty()) {
            return null;
        }

        // assume child is a column
        if (kids.size() == 1) {
            return kids.get(0);
        }

        // need to find column
        for (final AstNode kid : kids) {
            if (kid.getMixins().contains(StandardDdlLexicon.TYPE_COLUMN_DEFINITION)) {
                return kid;
            }
        }

        return null; // not found
    }

    /**
     * {@inheritDoc}
     *
     * @see org.modeshape.sequencer.ddl.dialect.teiid.StatementParser#matches(org.modeshape.sequencer.ddl.DdlTokenStream)
     */
    @Override
    boolean matches( final DdlTokenStream tokens ) {
        return tokens.matches(DdlStatement.CREATE_FOREIGN_TABLE.tokens())
               || tokens.matches(DdlStatement.CREATE_VIRTUAL_VIEW.tokens())
               || tokens.matches(DdlStatement.CREATE_VIEW.tokens())
               || tokens.matches(DdlStatement.CREATE_GLOBAL_TEMPORARY_TABLE.tokens())
               || tokens.matches(DdlStatement.CREATE_LOCAL_FOREIGN_TEMPORARY_TABLE.tokens())
               || tokens.matches(DdlStatement.CREATE_FOREIGN_TEMPORARY_TABLE.tokens())
               || tokens.matches(DdlStatement.CREATE_LOCAL_TEMPORARY_TABLE.tokens())
               || tokens.matches(DdlStatement.CREATE_TEMPORARY_TABLE.tokens());
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

        // CREATE FOREIGN TABLE <identifier> <create table body>
        // CREATE FOREIGN TABLE <identifier> <create table body> AS <query expression>
        // CREATE GLOBAL TEMPORARY TABLE  <identifier> <create table body>
        // CREATE VIRTUAL VIEW <identifier> <create table body>
        // CREATE VIRTUAL VIEW <identifier> <create table body> AS <query expression>
        // CREATE VIEW <identifier> <create table body>
        // CREATE VIEW <identifier> <create table body> AS <query expression>
        // CREATE LOCAL FOREIGN TEMPORARY TABLE ...
        // CREATE FOREIGN TEMPORARY TABLE ...
        // CREATE LOCAL TEMPORARY TABLE ...
        // CREATE TEMPORARY TABLE ...

        boolean includeLocalKeyword = false;

        DdlStatement stmt = null;
        String nodeType = null;
        SchemaElementType schemaElementType = null;

        if (tokens.canConsume(DdlStatement.CREATE_FOREIGN_TABLE.tokens())) {
            stmt = DdlStatement.CREATE_FOREIGN_TABLE;
            nodeType = TeiidDdlLexicon.CreateTable.TABLE_STATEMENT;
            schemaElementType = SchemaElementType.FOREIGN;
        } else if (tokens.canConsume(DdlStatement.CREATE_GLOBAL_TEMPORARY_TABLE.tokens())) {
            stmt = DdlStatement.CREATE_GLOBAL_TEMPORARY_TABLE;
            nodeType = TeiidDdlLexicon.CreateTable.GLOBAL_TEMP_TABLE_STATEMENT;
            schemaElementType = SchemaElementType.VIRTUAL;
        } else if (tokens.canConsume(DdlStatement.CREATE_VIEW.tokens()) ||
                        tokens.canConsume(DdlStatement.CREATE_VIRTUAL_VIEW.tokens())) {
            stmt = DdlStatement.CREATE_VIEW;
            nodeType = TeiidDdlLexicon.CreateTable.VIEW_STATEMENT;
            schemaElementType = SchemaElementType.VIRTUAL;
        } else if (tokens.matches(DdlStatement.CREATE_LOCAL_TEMPORARY_TABLE.tokens())
                   || tokens.matches(DdlStatement.CREATE_TEMPORARY_TABLE.tokens())) {
            stmt = DdlStatement.CREATE_TEMPORARY_TABLE;
            nodeType = TeiidDdlLexicon.CreateTable.LOCAL_TEMP_TABLE_STATEMENT;
            schemaElementType = SchemaElementType.VIRTUAL;

            if (tokens.canConsume(DdlStatement.CREATE_LOCAL_TEMPORARY_TABLE.tokens())) {
                includeLocalKeyword = true;
            } else {
                tokens.consume(DdlStatement.CREATE_TEMPORARY_TABLE.tokens());
                assert !includeLocalKeyword;
            }
        } else if (tokens.matches(DdlStatement.CREATE_LOCAL_FOREIGN_TEMPORARY_TABLE.tokens())
                   || tokens.matches(DdlStatement.CREATE_FOREIGN_TEMPORARY_TABLE.tokens())) {
            stmt = DdlStatement.CREATE_FOREIGN_TEMPORARY_TABLE;
            nodeType = TeiidDdlLexicon.CreateTable.FOREIGN_TEMP_TABLE_STATEMENT;
            schemaElementType = SchemaElementType.VIRTUAL;

            if (tokens.canConsume(DdlStatement.CREATE_LOCAL_FOREIGN_TEMPORARY_TABLE.tokens())) {
                includeLocalKeyword = true;
            } else {
                tokens.consume(DdlStatement.CREATE_FOREIGN_TEMPORARY_TABLE.tokens());
                assert !includeLocalKeyword;
            }
        } else {
            throw new TeiidDdlParsingException(tokens, "Unparsable create table or view statement");
        }

        assert (stmt != null) : "Create table or view statement is null";
        assert (nodeType != null) : "nodeType is null";
        assert (schemaElementType != null) : "schemaElementType is null";

        // parse identifier and create node
        final String id = parseIdentifier(tokens);
        final AstNode tableNode = getNodeFactory().node(id, parentNode, nodeType);
        tableNode.setProperty(TeiidDdlLexicon.SchemaElement.TYPE, schemaElementType.toDdl());

        if (stmt == DdlStatement.CREATE_TEMPORARY_TABLE) {
            parseLocalTemporaryTable(tokens, tableNode, includeLocalKeyword);
        } else if (stmt == DdlStatement.CREATE_FOREIGN_TEMPORARY_TABLE) {
            parseForeignTemporaryTable(tokens, tableNode, includeLocalKeyword);
        } else {
            // must have a table body
            parseTableBody(tokens, tableNode);

            // may have an AS clause if not a global temp table
            if (tokens.hasNext() && tokens.canConsume(TeiidReservedWord.AS.toDdl())) {
                if (DdlStatement.CREATE_GLOBAL_TEMPORARY_TABLE.equals(stmt))
                    throw new TeiidDdlParsingException(tokens, "Unparsable create table statement: CREATE GLOBAL TEMPORARY TABLES do not have AS statements");
                // must have a query expression
                if (!parseQueryExpression(tokens, tableNode)) {
                    throw new TeiidDdlParsingException(tokens, "Unparsable create table statement");
                }
            }
        }

        return tableNode;
    }

    /**
     * Precondition: everything up to and including the table identifier has been consumed.
     * <pre>
     * <code>
     * CREATE ( LOCAL )? FOREIGN TEMPORARY TABLE &lt;identifier&gt; &lt;create table body&gt; ON &lt;identifier&gt;
     * </code>
     * </pre>
     * @param tokens the tokens being processed (never <code>null</code>)
     * @param tableNode the temporary table node (never <code>null</code>)
     * @param includeLocalKeyword <code>true</code> if the <i>LOCAL</i> keyword should be persisted
     * @throws ParsingException if the tokens cannot be parsed
     */
    private void parseForeignTemporaryTable( final DdlTokenStream tokens,
                                             final AstNode tableNode,
                                             final boolean includeLocalKeyword ) throws ParsingException {
        tableNode.setProperty(TeiidDdlLexicon.CreateTable.INCLUDE_LOCAL_KEYWORD, includeLocalKeyword);

        parseTableBody(tokens, tableNode);

        // must have ON keyword and the referenced schema/model name
        if (!tokens.canConsume(ON)) {
            throw new TeiidDdlParsingException(tokens, "Missing ON keyword parsing foreign temporary table");
        }

        final String schemaName = parseIdentifier(tokens);
        tableNode.setProperty(TeiidDdlLexicon.CreateTable.SCHEMA_REFERENCE, schemaName);
    }

    /**
     * Precondition: PRIMARY KEY tokens have already been consumed
     * <pre>
     * <code>
     * PRIMARY KEY &lt;column list&gt;
     * </code>
     * </pre>
     * @param tokens the tokens being processed (never <code>null</code>)
     * @param tableNode the temporary table node (never <code>null</code>)
     * @throws ParsingException if the tokens cannot be parsed
     */
    private void parseTemporaryTablePrimaryKeyElement( final DdlTokenStream tokens,
                                                       final AstNode tableNode ) throws ParsingException {
        // get the actual referenced column nodes using the tokens
        final List<AstNode> tempTableElements = parseReferenceList(tokens, tableNode, null);
        tableNode.setProperty(TeiidDdlLexicon.CreateTable.PRIMARY_KEY_COLUMNS, tempTableElements.toArray(new Object[tempTableElements.size()]));
    }

    /**
     * <pre>
     * <code>
     * &lt;identifier&gt; ( &lt;data type&gt; | SERIAL ) ( NOT NULL )?
     * </code>
     * </pre>
     * @param tokens the tokens being processed (never <code>null</code>)
     * @param tableNode the temporary table node (never <code>null</code>)
     * @throws ParsingException if the tokens cannot be parsed
     */
    private void parseTemporaryTableElement( final DdlTokenStream tokens,
                                             final AstNode tableNode ) throws ParsingException {
        final String id = parseIdentifier(tokens);
        final boolean includeSerialAlias = tokens.matches(TeiidDdlConstants.TeiidNonReservedWord.SERIAL.toDdl());

        final DataType datatype = getDataTypeParser().parse(tokens);

        // create temporary table element and set properties of datatype
        final AstNode tempTableElement = getNodeFactory().node(id, tableNode, TeiidDdlLexicon.CreateTable.TEMP_TABLE_ELEMENT);
        getDataTypeParser().setPropertiesOnNode(tempTableElement, datatype);
        tempTableElement.setProperty(TeiidDdlLexicon.CreateTable.INCLUDE_SERIAL_ALIAS, includeSerialAlias);

        // may have NOT NULL
        final boolean nullable = !tokens.canConsume(NOT, NULL);
        tempTableElement.setProperty(StandardDdlLexicon.NULLABLE, (nullable ? "NULL" : "NOT NULL"));
    }

    /**
     * Precondition: everything up to and including the table identifier has been consumed.
     * <pre>
     * <code>
     * CREATE ( LOCAL )? TEMPORARY TABLE &lt;identifier&gt;> &lt;lparen&gt; &lt;temporary table element&gt;
     *     ( &lt;comma&gt; &lt;temporary table element&gt; )* ( &lt;comma&gt; PRIMARY KEY &lt;column list&gt; )? &lt;rparen&gt;
     *     ( ON COMMIT PRESERVE ROWS )?
     * </code>
     * </pre>
     * @param tokens the tokens being processed (never <code>null</code>)
     * @param tableNode the temporary table node (never <code>null</code>)
     * @param includeLocalKeyword <code>true</code> if the <i>LOCAL</i> keyword should be persisted
     * @throws ParsingException if the tokens cannot be parsed
     */
    private void parseLocalTemporaryTable( final DdlTokenStream tokens,
                                           final AstNode tableNode,
                                           final boolean includeLocalKeyword ) throws ParsingException {
        tableNode.setProperty(TeiidDdlLexicon.CreateTable.INCLUDE_LOCAL_KEYWORD, includeLocalKeyword);

        // must have a left paren
        if (!tokens.canConsume(L_PAREN)) {
            throw new TeiidDdlParsingException(tokens,
                                               "Missing table elements starting left paren parsing local temporary table");
        }

        // must have at least one temp table element
        parseTemporaryTableElement(tokens, tableNode);

        // process any remaining temp table elements and constraints
        while (tokens.canConsume(COMMA)) {
            // may have primary key and it must be last token
            if (tokens.canConsume(PRIMARY, KEY)) {
                parseTemporaryTablePrimaryKeyElement(tokens, tableNode);
                break;
            }

            parseTemporaryTableElement(tokens, tableNode);
        }

        // must have a right paren
        if (!tokens.canConsume(R_PAREN)) {
            throw new TeiidDdlParsingException(tokens, "Missing table elements ending right paren parsing local temporary table");
        }

        // may have ON COMMIT clause
        boolean includeOnCommitClause = false;

        if (tokens.canConsume(new String[] {ON,
                                            TeiidFutureReserveWord.COMMIT.name(),
                                            TeiidNonReservedWord.PRESERVE.toDdl(),
                                            TeiidReservedWord.ROWS.toDdl()})) {
            includeOnCommitClause = true;
        }

        tableNode.setProperty(TeiidDdlLexicon.CreateTable.INCLUDE_ON_COMMIT_CLAUSE, includeOnCommitClause);
    }

    /**
     * The tokens must start with a left paren, end with a right paren, and have content between. Any parens in the content must
     * have matching parens.
     *
     * @param tokens the tokens being processed (cannot be <code>null</code> but or empty)
     * @return the expression (never <code>null</code> or empty.
     * @throws ParsingException if there is a problem parsing the query expression
     */
    private String parseExpression( final DdlTokenStream tokens ) throws ParsingException {
        if (!tokens.hasNext() || !tokens.matches(L_PAREN)) {
            throw new TeiidDdlParsingException(tokens, "Unparsable expression list");
        }

        Position prevPosition = tokens.nextPosition();
        String prevValue = tokens.consume(); // don't include first paren in expression

        int numLeft = 1;
        int numRight = 0;

        final StringBuilder text = new StringBuilder();

        while (tokens.hasNext()) {
            final Position currPosition = tokens.nextPosition();

            if (tokens.matches(L_PAREN)) {
                ++numLeft;
            } else if (tokens.matches(R_PAREN)) {
                if (numLeft == ++numRight) {
                    tokens.consume(R_PAREN); // don't include last paren in expression

                    if ((currPosition.getIndexInContent() - prevValue.length() + 1 - prevPosition.getIndexInContent()) > 1) {
                        text.append(SPACE);
                    }

                    break;
                }
            }

            final String value = tokens.consume();

            text.append(getWhitespace(currPosition, prevPosition, prevValue));
            text.append(value);
            prevValue = value;
            prevPosition = currPosition;
        }

        if (numLeft != numRight) {
            throw new TeiidDdlParsingException(tokens, "Unparsable expression list");
        }

        return text.toString();
    }

    /**
     * Assumes tokens have a one set of parens with one or more identifiers separated by commas. All of these tokens are consumed.
     * <code>
     * <column list>
     *
     * <lparen> <identifier> ( <comma> <identifier> )* <rparen>
     * </code>
     *
     * @param tokens the tokens being processed (cannot be <code>null</code>)
     * @return the list of identifiers (never <code>null</code> and never empty)
     * @throws ParsingException if there is a problem parsing the identifier list
     */
    List<String> parseIdentifierList( final DdlTokenStream tokens ) throws ParsingException {
        if (tokens.canConsume(L_PAREN)) {
            final List<String> identifiers = new ArrayList<String>();
            final String firstId = parseIdentifier(tokens);
            identifiers.add(firstId);

            // process remaining IDs
            while (tokens.canConsume(COMMA)) {
                final String nextId = parseIdentifier(tokens);
                identifiers.add(nextId);
            }

            if (tokens.canConsume(R_PAREN)) {
                return identifiers;
            }
        }

        throw new TeiidDdlParsingException(tokens, "Unparsable identifier list");
    }

    /**
     * <code>
     * <query expression> == ( WITH <with list element> ( <comma> <with list element> )* )? <query expression body>
     * <with list element> == <identifier> ( <column list> )? AS <lparen> <query expression> <rparen>
     * <query expression body> == <query term> ( ( UNION | EXCEPT ) ( ALL | DISTINCT )? <query term> )* ( <order by clause> )? ( <limit clause> )? ( <option clause> )?
     * <code>
     *
     * @param tokens the tokens being processed (cannot be <code>null</code> but can be empty)
     * @param tableNode the table node owning this query expression (cannot be <code>null</code>)
     * @return <code>true</code> if a query expression was successfully parsed
     */
    boolean parseQueryExpression( final DdlTokenStream tokens,
                                  final AstNode tableNode ) {
        final String queryExpression = parseUntilTerminator(tokens);

        if (StringUtil.isBlank(queryExpression)) {
            return false;
        }

        tableNode.setProperty(TeiidDdlLexicon.CreateTable.QUERY_EXPRESSION, queryExpression);
        return true;
    }

    /**
     * Assumes tokens have a one set of parens with one or more identifiers separated by commas. All of these tokens are consumed.
     *
     * @param tokens the tokens being processed (cannot be <code>null</code> or empty)
     * @param tableNode the table node used to lookup referenced column nodes (cannot be <code>null</code>)
     * @param unresolvedReferencedTableNode the unresolved table references component; may be null
     * @return the collection of referenced column nodes (never <code>null</code> or empty)
     * @throws ParsingException if a node of a referenced column cannot be found
     */
    private List<AstNode> parseReferenceList( final DdlTokenStream tokens,
                                              final AstNode tableNode,
                                              final UnresolvedTableReferenceNode unresolvedReferencedTableNode )
        throws ParsingException {
        final List<String> columns = parseIdentifierList(tokens);
        final List<AstNode> references = new ArrayList<AstNode>(columns.size());

        /* If tableNode == null bypass column node setting */
        for (final String columnName : columns) {
            if (unresolvedReferencedTableNode != null) {
                unresolvedReferencedTableNode.addColumnReferenceName(columnName);
                continue;
            }

            final AstNode referencedColumnNode = getColumnNode(tableNode, columnName);

            // can't find referenced column
            if (referencedColumnNode == null) {
                final String msg = String.format("Referenced column '%s' in table '%s' was not found",
                                                 columnName,
                                                 tableNode.getName());
                throw new TeiidDdlParsingException(tokens, msg);
            }

            references.add(referencedColumnNode);
        }

        return references;
    }

    /**
     * <code>
     * <create table body>
     *
     * ( <lparen> <table element> ( <comma> <table element> )* ( <comma> ( CONSTRAINT <identifier> )? ( <primary key> | <other constraints> | <foreign key> ) ( <options clause> )? )* <rparen> )? ( <options clause> )?
     * </code>
     *
     * @param tokens the tokens being processed (cannot be <code>null</code>)
     * @param tableNode the table node whose body is being processed (cannot be <code>null</code>)
     */
    void parseTableBody( final DdlTokenStream tokens,
                         final AstNode tableNode ) {
        // may have one or more table elements
        if (tokens.canConsume(L_PAREN)) {
            // process first table element
            parseTableElement(tokens, tableNode);

            // process any remaining table elements and constraints
            while (tokens.canConsume(COMMA)) {
                if (!parseTableBodyConstraint(tokens, tableNode)) {
                    parseTableElement(tokens, tableNode);
                }
            }

            if (!tokens.canConsume(R_PAREN)) {
                throw new TeiidDdlParsingException(tokens, "Unparsable table body");
            }
        }

        // may have options clause
        parseOptionsClause(tokens, tableNode);
    }

    /**
     * <code>
     * ( CONSTRAINT <identifier> )? ( <primary key> | <other constraints> | <foreign key> ) ( <options clause> )
     * </code>
     *
     * @param tokens the tokens being processed (cannot be <code>null</code>)
     * @param tableNode the table node whose constraint is being processed (cannot be <code>null</code>)
     * @return <code>true</code> if a table body constraint was successfully parsed
     * @throws ParsingException if there is a problem parsing a table body constraint
     */
    boolean parseTableBodyConstraint( final DdlTokenStream tokens,
                                      final AstNode tableNode ) throws ParsingException {
        if (!tokens.hasNext()) {
            return false;
        }

        // must find CONSTRAINT, PRIMARY KEY, FOREIGN KEY, UNIQUE, ACCESSPATTERN, INDEX
        if (tokens.matches(CONSTRAINT) || tokens.matches(PRIMARY, KEY, L_PAREN) || tokens.matches(FOREIGN, KEY, L_PAREN)
            || tokens.matches(UNIQUE, L_PAREN) || tokens.matches(TeiidNonReservedWord.ACCESSPATTERN.toDdl(), L_PAREN)
            || tokens.matches(TeiidNonReservedWord.INDEX.toDdl(), L_PAREN)) {
            String constraintType = null;

            // may have identifier
            String constraintId = (tokens.canConsume(CONSTRAINT) ? parseIdentifier(tokens) : null);
            AstNode constraintNode = null;
            String nodeType = null;

            // <primary key> == PRIMARY KEY <column list>
            // <foreign key> == FOREIGN KEY <column list> REFERENCES <identifier> ( <column list> )?
            // <other constraints> == ( ( UNIQUE | ACCESSPATTERN ) <column list> ) or ( INDEX <lparen> <expression list> <rparen>

            if (tokens.matches(PRIMARY, KEY) || tokens.matches(FOREIGN, KEY) || tokens.matches(TeiidReservedWord.UNIQUE.toDdl())
                || tokens.matches(TeiidNonReservedWord.ACCESSPATTERN.toDdl())) {
                if (tokens.canConsume(PRIMARY, KEY)) {
                    constraintType = PRIMARY_KEY;
                    nodeType = TeiidDdlLexicon.Constraint.TABLE_ELEMENT;
                } else if (tokens.canConsume(FOREIGN, KEY)) {
                    constraintType = FOREIGN_KEY;
                    nodeType = TeiidDdlLexicon.Constraint.FOREIGN_KEY_CONSTRAINT;
                } else if (tokens.matchesAnyOf(TeiidReservedWord.UNIQUE.toDdl(), TeiidNonReservedWord.ACCESSPATTERN.toDdl())) {
                    constraintType = tokens.consume();
                    nodeType = TeiidDdlLexicon.Constraint.TABLE_ELEMENT;
                }

                assert (constraintType != null);
                assert (nodeType != null);

                // process column list and create references to their nodes
                final List<AstNode> references = parseReferenceList(tokens, tableNode, null);

                // set a constraint ID if needed
                if (StringUtil.isBlank(constraintId)) {
                    constraintId = createConstraintName(constraintType, tableNode);
                }

                // create constraint node
                constraintNode = getNodeFactory().node(constraintId, tableNode, nodeType);
                constraintNode.setProperty(TeiidDdlLexicon.Constraint.TYPE, constraintType);
                constraintNode.setProperty(TeiidDdlLexicon.Constraint.REFERENCES, references);

                if (FOREIGN_KEY.equals(constraintType)) {
                    if (tokens.canConsume(TeiidReservedWord.REFERENCES.toDdl())) {
                        final String referencesTableName = parseIdentifier(tokens);
                        final AstNode referencesTableNode = getNode(tableNode.getParent(),
                                                                    referencesTableName,
                                                                    TeiidDdlLexicon.CreateTable.TABLE_STATEMENT,
                                                                    TeiidDdlLexicon.CreateTable.VIEW_STATEMENT);

                        UnresolvedTableReferenceNode unresolvedTableReferenceNode = null;
                        // can't find referenced table
                        if (referencesTableNode == null) {
                            unresolvedTableReferenceNode = new UnresolvedTableReferenceNode(constraintNode, referencesTableName);
                            unresolvedTableReferences.add(unresolvedTableReferenceNode);
                        } else {
                            constraintNode.setProperty(TeiidDdlLexicon.Constraint.TABLE_REFERENCE, referencesTableNode);
                        }

                        // may have referenced columns so check for opening paren before parsing refs
                        if (tokens.matches(L_PAREN)) {
                            final List<AstNode> constraintReferences = parseReferenceList(tokens,
                                                                                          referencesTableNode,
                                                                                          unresolvedTableReferenceNode);

                            if (!constraintReferences.isEmpty()) {
                                constraintNode.setProperty(TeiidDdlLexicon.Constraint.TABLE_REFERENCE_REFERENCES,
                                                           constraintReferences);
                            }
                        }
                    } else {
                        throw new TeiidDdlParsingException(tokens,
                                                           "Unparsable table body foreign key constraint (missing REFERENCES keyword)");
                    }
                }
            } else if (tokens.matches(TeiidNonReservedWord.INDEX.toDdl(), L_PAREN)) {
                tokens.consume(TeiidNonReservedWord.INDEX.toDdl());
                constraintType = TeiidNonReservedWord.INDEX.toDdl();

                if (StringUtil.isBlank(constraintId)) {
                    constraintId = createConstraintName(constraintType, tableNode);
                }

                constraintNode = getNodeFactory().node(constraintId, tableNode, TeiidDdlLexicon.Constraint.INDEX_CONSTRAINT);
                constraintNode.setProperty(TeiidDdlLexicon.Constraint.TYPE, constraintType);

                final String expression = parseExpression(tokens);
                constraintNode.setProperty(TeiidDdlLexicon.Constraint.EXPRESSION, expression);

                // look for table element references in the expression and set references
                final List<AstNode> columns = tableNode.getChildren(TeiidDdlLexicon.CreateTable.TABLE_ELEMENT);
                final Set<AstNode> referencedColumns = new HashSet<AstNode>(columns.size());

                for (final AstNode column : columns) {
                    if (contains(expression, column.getName())) {
                        referencedColumns.add(column);
                    }
                }

                if (!referencedColumns.isEmpty()) {
                    constraintNode.setProperty(TeiidDdlLexicon.Constraint.REFERENCES, new ArrayList<AstNode>(referencedColumns));
                }
            } else {
                throw new TeiidDdlParsingException(tokens, "Unparsable table body constraint");
            }

            // may have an options clause
            parseOptionsClause(tokens, constraintNode);
            return true;
        }

        return false;
    }

    /**
     * <code>
     * <table element>
     *
     * <identifier> <data type> ( NOT NULL )? (  UNIQUE | ( INDEX | AUTO_INCREMENT )+ | ( PRIMARY KEY ) )? ( DEFAULT <string> )? ( <options clause> )?
     * </code>
     *
     * @param tokens the tokens being processed (cannot be <code>null</code>)
     * @param tableNode the table node whose body is being processed (cannot be <code>null</code>)
     * @return the table element node (never <code>null</code>)
     * @throws ParsingException if the table element cannot be parsed
     */
    AstNode parseTableElement( final DdlTokenStream tokens,
                               final AstNode tableNode ) throws ParsingException {

        final String id = parseIdentifier(tokens);
        final DataType datatype = getDataTypeParser().parse(tokens);

        final AstNode columnNode = getNodeFactory().node(id, tableNode, TeiidDdlLexicon.CreateTable.TABLE_ELEMENT);
        getDataTypeParser().setPropertiesOnNode(columnNode, datatype);

        boolean foundDefaultClause = false;
        boolean foundOptionsClause = false;
        boolean foundConstraintType = false;

        //
        // AutoIncrement and NotNull will normally be false but there is an exception when
        // SERIAL is parsed as the data type.
        // This converts to an auto-incrementing / notnull INTEGER
        //
        boolean foundAutoIncrement = datatype.isAutoIncrement();
        boolean foundNotNull = datatype.isNotNull();

        // look for 5 optional parts in any order
        while (tokens.hasNext()
               && (!foundNotNull || !foundDefaultClause || !foundOptionsClause || !foundConstraintType || !foundAutoIncrement)) {
            if (!foundNotNull && tokens.canConsume(NOT_NULL)) {
                foundNotNull = true;
            } else if (!foundAutoIncrement && tokens.canConsume(TeiidNonReservedWord.AUTO_INCREMENT.toDdl())) {
                foundAutoIncrement = true;
            } else if (!foundConstraintType
                       && (tokens.matches(TeiidReservedWord.UNIQUE.toDdl()) || tokens.matches(INDEX) || tokens.matches(PRIMARY,
                                                                                                                       KEY))) {
                foundConstraintType = true;
                String constraintType = null;

                if (tokens.matches(PRIMARY, KEY) || tokens.matches(TeiidReservedWord.UNIQUE.toDdl())) {
                    if (tokens.canConsume(TeiidReservedWord.UNIQUE.toDdl())) {
                        constraintType = TeiidReservedWord.UNIQUE.toDdl();
                    } else {
                        tokens.consume(PRIMARY, KEY);
                        constraintType = PRIMARY_KEY;
                    }

                    // create constraint node
                    final AstNode constraintNode = getNodeFactory().node(constraintType,
                                                                         tableNode,
                                                                         TeiidDdlLexicon.Constraint.TABLE_ELEMENT);
                    constraintNode.setProperty(TeiidDdlLexicon.Constraint.TYPE, constraintType);

                    // create a single element list since property is multi-valued
                    constraintNode.setProperty(TeiidDdlLexicon.Constraint.REFERENCES, Collections.singletonList(columnNode));
                } else if (tokens.canConsume(INDEX)) {
                    constraintType = INDEX;

                    // create constraint node
                    final AstNode constraintNode = getNodeFactory().node(constraintType,
                                                                         tableNode,
                                                                         TeiidDdlLexicon.Constraint.INDEX_CONSTRAINT);
                    constraintNode.setProperty(TeiidDdlLexicon.Constraint.TYPE, constraintType);

                    // create a single element list since property is multi-valued
                    constraintNode.setProperty(TeiidDdlLexicon.Constraint.REFERENCES, Collections.singletonList(columnNode));
                } else {
                    throw new TeiidDdlParsingException(tokens, "Unparsable table body unnamed constraint");
                }
            } else if (!foundDefaultClause && parseDefaultClause(tokens, columnNode)) {
                foundDefaultClause = true;
            } else if (!foundOptionsClause && parseOptionsClause(tokens, columnNode)) {
                foundOptionsClause = true;
            } else {
                break; // none of the optional clauses found
            }
        }

        columnNode.setProperty(TeiidDdlLexicon.CreateTable.AUTO_INCREMENT, foundAutoIncrement);
        columnNode.setProperty(StandardDdlLexicon.NULLABLE, (foundNotNull ? "NOT NULL" : "NULL"));

        return columnNode;
    }

    /**
     * Process the AstNode tree to look for table references from foreign keys that are not resolved
     */
    @Override
    protected void postProcess( AstNode rootNode ) {
        for (UnresolvedTableReferenceNode node : unresolvedTableReferences) {
            final AstNode constraintNode = node.getContraintNode();
            final String referencesTableName = node.getTableReferenceName();

            final AstNode referencesTableNode = getNode(constraintNode.getParent().getParent(),
                                                        referencesTableName,
                                                        TeiidDdlLexicon.CreateTable.TABLE_STATEMENT,
                                                        TeiidDdlLexicon.CreateTable.VIEW_STATEMENT);

            // can't find referenced table
            if (referencesTableNode == null) {
                this.logger.debug("Create table statment foreign key reference table '{0}' was not found", referencesTableName);
            } else {
                // Now set the TABLE_REFERENCE property
                constraintNode.setProperty(TeiidDdlLexicon.Constraint.TABLE_REFERENCE, referencesTableNode);
                final List<AstNode> references = new ArrayList<AstNode>(node.getColumnReferenceNames().size());

                // If any column names references exists, go find the real objects and set the TABLE_REFERENCE_REFERENCES property
                for (String columnName : node.getColumnReferenceNames()) {
                    final AstNode referencedColumnNode = getColumnNode(referencesTableNode, columnName);

                    // can't find referenced column
                    if (referencedColumnNode == null) {
                        this.logger.debug("Create table statement node of column reference '{0}' was not found", columnName);
                    } else {
                        references.add(referencedColumnNode);
                    }
                }

                if (!references.isEmpty()) {
                    constraintNode.setProperty(TeiidDdlLexicon.Constraint.TABLE_REFERENCE_REFERENCES, references);
                }
            }
        }

        unresolvedTableReferences.clear();
    }

    class UnresolvedTableReferenceNode {
        AstNode contraintNode;
        String tableReferenceName;
        Set<String> columnReferenceNames;

        public UnresolvedTableReferenceNode( AstNode node,
                                             String name ) {
            this.contraintNode = node;
            this.tableReferenceName = name;
            this.columnReferenceNames = new HashSet<String>();
        }

        public AstNode getContraintNode() {
            return contraintNode;
        }

        public String getTableReferenceName() {
            return tableReferenceName;
        }

        public void addColumnReferenceName( String name ) {
            this.columnReferenceNames.add(name);
        }

        public Set<String> getColumnReferenceNames() {
            return this.columnReferenceNames;
        }
    }

}
