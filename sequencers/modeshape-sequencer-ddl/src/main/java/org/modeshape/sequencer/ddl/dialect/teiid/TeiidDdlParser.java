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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.modeshape.common.text.ParsingException;
import org.modeshape.sequencer.ddl.DdlTokenStream;
import org.modeshape.sequencer.ddl.StandardDdlParser;
import org.modeshape.sequencer.ddl.datatype.DataTypeParser;
import org.modeshape.sequencer.ddl.node.AstNode;

/**
 * A DDL parser for the Teiid dialect.
 */
public final class TeiidDdlParser extends StandardDdlParser implements TeiidDdlConstants {

    private static final String ID = "TEIID";

    private final Map<String, String> namespaceAliases;
    private final Collection<StatementParser> parsers;

    /**
     * Constructs a Teiid DDL parser.
     */
    public TeiidDdlParser() {
        final DataTypeParser dataTypeParser = new TeiidDataTypeParser();
        setDatatypeParser(dataTypeParser);
        this.namespaceAliases = new HashMap<String, String>();

        // setup statement parsers
        final List<StatementParser> temp = new ArrayList<StatementParser>(5);
        temp.add(new CreateTableParser(this));
        temp.add(new CreateProcedureParser(this));
        temp.add(new CreateTriggerParser(this));
        temp.add(new AlterOptionsParser(this));
        temp.add(new OptionNamespaceParser(this));
        this.parsers = Collections.unmodifiableCollection(temp);
    }

    boolean accessParseDefaultClause( final DdlTokenStream tokens,
                                      final AstNode columnNode ) {
        return super.parseDefaultClause(tokens, columnNode);
    }

    String accessParseUntilTerminator( final DdlTokenStream tokens ) {
        return super.parseUntilTerminator(tokens);
    }

    String accessParseUntilTerminatorIgnoreEmbeddedStatements( final DdlTokenStream tokens ) {
        return super.parseUntilTerminatorIgnoreEmbeddedStatements(tokens);
    }

    void addNamespaceAlias( final String alias,
                            final String identifier ) {
        this.namespaceAliases.put(alias, identifier);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#getCustomDataTypeStartWords()
     */
    @Override
    protected List<String> getCustomDataTypeStartWords() {
        return TeiidDataType.getStartWords();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#getId()
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     * @param alias the alias whose namespace URI is being requested (cannot be <code>null</code> or empty)
     * @return the URI or <code>null</code> if not found
     */
    String getNamespaceUri( final String alias ) {
        return this.namespaceAliases.get(alias);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#getValidSchemaChildTypes()
     */
    @Override
    protected String[] getValidSchemaChildTypes() {
        return TeiidDdlLexicon.getValidSchemaChildTypes();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#initializeTokenStream(org.modeshape.sequencer.ddl.DdlTokenStream)
     */
    @Override
    protected void initializeTokenStream( final DdlTokenStream tokens ) {
        super.initializeTokenStream(tokens);
        tokens.registerKeyWords(TeiidReservedWord.asList());
        tokens.registerKeyWords(TeiidNonReservedWord.asList());

        for (final DdlStatement stmt : DdlStatement.values()) {
            tokens.registerStatementStartPhrase(stmt.tokens());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseAlterStatement(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseAlterStatement( final DdlTokenStream tokens,
                                           final AstNode parentNode ) throws ParsingException {
        throw new TeiidDdlParsingException(tokens, "parseAlterStatement should not be called");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseAlterTableStatement(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseAlterTableStatement( final DdlTokenStream tokens,
                                                final AstNode parentNode ) throws ParsingException {
        throw new TeiidDdlParsingException(tokens, "parseAlterTableStatement should not be called");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseCollateClause(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode)
     */
    @Override
    protected boolean parseCollateClause( final DdlTokenStream tokens,
                                          final AstNode columnNode ) throws ParsingException {
        throw new TeiidDdlParsingException(tokens, "parseCollateClause should not be called");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseColumnConstraint(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode, boolean)
     */
    @Override
    protected boolean parseColumnConstraint( final DdlTokenStream tokens,
                                             final AstNode columnNode,
                                             final boolean isAlterTable ) throws ParsingException {
        throw new TeiidDdlParsingException(tokens, "parseCollateClause should not be called");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseColumnDefinition(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode, boolean)
     */
    @Override
    protected void parseColumnDefinition( final DdlTokenStream tokens,
                                          final AstNode tableNode,
                                          final boolean isAlterTable ) throws ParsingException {
        throw new TeiidDdlParsingException(tokens, "parseColumnDefinition should not be called");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseCreateSchemaStatement(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseCreateSchemaStatement( final DdlTokenStream tokens,
                                                  final AstNode parentNode ) throws ParsingException {
        throw new TeiidDdlParsingException(tokens, "parseCreateSchemaStatement should not be called");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseCreateStatement(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseCreateStatement( final DdlTokenStream tokens,
                                            final AstNode parentNode ) throws ParsingException {
        throw new TeiidDdlParsingException(tokens, "parseCreateStatement should not be called");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseCreateViewStatement(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseCreateViewStatement( final DdlTokenStream tokens,
                                                final AstNode parentNode ) throws ParsingException {
        throw new TeiidDdlParsingException(tokens, "parseCreateViewStatement should not be called");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseCustomStatement(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseCustomStatement( final DdlTokenStream tokens,
                                            final AstNode parentNode ) throws ParsingException {
        throw new TeiidDdlParsingException(tokens, "parseCustomStatement should not be called");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseDropStatement(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseDropStatement( final DdlTokenStream tokens,
                                          final AstNode parentNode ) throws ParsingException {
        throw new TeiidDdlParsingException(tokens, "parseDropStatement should not be called");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseGrantPrivileges(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      java.util.List)
     */
    @Override
    protected void parseGrantPrivileges( final DdlTokenStream tokens,
                                         final List<AstNode> privileges ) throws ParsingException {
        throw new TeiidDdlParsingException(tokens, "parseGrantPrivileges should not be called");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseGrantStatement(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseGrantStatement( final DdlTokenStream tokens,
                                           final AstNode parentNode ) throws ParsingException {
        throw new TeiidDdlParsingException(tokens, "parseGrantStatement should not be called");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseIgnorableStatement(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      java.lang.String, org.modeshape.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseIgnorableStatement( final DdlTokenStream tokens,
                                               final String name,
                                               final AstNode parentNode ) {
        throw new TeiidDdlParsingException(tokens, "parseIgnorableStatement should not be called");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseIgnorableStatement(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      java.lang.String, org.modeshape.sequencer.ddl.node.AstNode, java.lang.String)
     */
    @Override
    protected AstNode parseIgnorableStatement( final DdlTokenStream tokens,
                                               final String name,
                                               final AstNode parentNode,
                                               final String mixinType ) {
        throw new TeiidDdlParsingException(tokens, "parseIgnorableStatement should not be called");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseInsertStatement(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseInsertStatement( final DdlTokenStream tokens,
                                            final AstNode parentNode ) throws ParsingException {
        throw new TeiidDdlParsingException(tokens, "parseInsertStatement should not be called");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseNextCreateTableOption(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode)
     */
    @Override
    protected void parseNextCreateTableOption( final DdlTokenStream tokens,
                                               final AstNode tableNode ) throws ParsingException {
        throw new TeiidDdlParsingException(tokens, "parseNextCreateTableOption should not be called");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseNextStatement(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseNextStatement( final DdlTokenStream tokens,
                                          final AstNode parentNode ) {
        for (final StatementParser parser : this.parsers) {
            if (parser.matches(tokens)) {
                markStartOfStatement(tokens);
                final AstNode statementNode = parser.parse(tokens, parentNode);
                markEndOfStatement(tokens, statementNode);
                return statementNode;
            }
        }

        // Unparsable DDL statement
        throw new TeiidDdlParsingException(tokens, "Unparsable DDL statement");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseReferences(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode)
     */
    @Override
    protected void parseReferences( final DdlTokenStream tokens,
                                    final AstNode constraintNode ) throws ParsingException {
        throw new TeiidDdlParsingException(tokens, "parseReferences should not be called");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseRevokeStatement(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseRevokeStatement( final DdlTokenStream tokens,
                                            final AstNode parentNode ) throws ParsingException {
        throw new TeiidDdlParsingException(tokens, "parseRevokeStatement should not be called");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseSetStatement(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode)
     */
    @Override
    protected AstNode parseSetStatement( final DdlTokenStream tokens,
                                         final AstNode parentNode ) throws ParsingException {
        throw new TeiidDdlParsingException(tokens, "parseSetStatement should not be called");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseStatement(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      java.lang.String[], org.modeshape.sequencer.ddl.node.AstNode, java.lang.String)
     */
    @Override
    protected AstNode parseStatement( final DdlTokenStream tokens,
                                      final String[] stmt_start_phrase,
                                      final AstNode parentNode,
                                      final String mixinType ) {
        throw new TeiidDdlParsingException(tokens, "parseStatement should not be called");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#parseTableConstraint(org.modeshape.sequencer.ddl.DdlTokenStream,
     *      org.modeshape.sequencer.ddl.node.AstNode, boolean)
     */
    @Override
    protected void parseTableConstraint( final DdlTokenStream tokens,
                                         final AstNode tableNode,
                                         final boolean isAlterTable ) throws ParsingException {
        throw new TeiidDdlParsingException(tokens, "parseTableConstraint should not be called");
    }

}
