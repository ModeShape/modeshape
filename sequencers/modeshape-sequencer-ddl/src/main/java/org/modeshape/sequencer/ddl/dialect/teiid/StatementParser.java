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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.text.Position;
import org.modeshape.common.util.StringUtil;
import org.modeshape.sequencer.ddl.DdlConstants;
import org.modeshape.sequencer.ddl.DdlTokenStream;
import org.modeshape.sequencer.ddl.DdlTokenStream.DdlTokenizer;
import org.modeshape.sequencer.ddl.StandardDdlLexicon;
import org.modeshape.sequencer.ddl.datatype.DataTypeParser;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.TeiidReservedWord;
import org.modeshape.sequencer.ddl.node.AstNode;
import org.modeshape.sequencer.ddl.node.AstNodeFactory;

/**
 * A parser for a particular DDL statement.
 */
abstract class StatementParser implements DdlConstants {

    static final String[] NOT_NULL = new String[] {NOT, NULL};

    protected Logger logger = Logger.getLogger(getClass());
    private final TeiidDdlParser teiidDdlParser;

    protected StatementParser( final TeiidDdlParser teiidDdlParser ) {
        this.teiidDdlParser = teiidDdlParser;
    }

    protected void addNamespaceAlias( final String alias,
                                      final String identifier ) {
        this.teiidDdlParser.addNamespaceAlias(alias, identifier);
    }

    final protected DataTypeParser getDataTypeParser() {
        return this.teiidDdlParser.getDatatypeParser();
    }

    final String getNamespaceUri( final String alias ) {
        return this.teiidDdlParser.getNamespaceUri(alias);
    }

    final protected AstNodeFactory getNodeFactory() {
        return this.teiidDdlParser.nodeFactory();
    }

    /**
     * @param rootNode the parent node whose specified child node is being requested (cannot be <code>null</code>)
     * @param nodeName the name of the node being requested (cannot be <code>null</code> or empty)
     * @param nodeTypes the primary node types or mixins of the node being requested (cannot be <code>null</code> or empty)
     * @return the first matching node or <code>null</code> if not found
     */
    protected AstNode getNode( final AstNode rootNode,
                               final String nodeName,
                               final String... nodeTypes ) {
        assert (rootNode != null);
        assert ((nodeName != null) && !nodeName.isEmpty());
        assert ((nodeTypes != null) && (nodeTypes.length != 0));

        final List<AstNode> kids = rootNode.childrenWithName(nodeName);

        // not found
        if (kids.isEmpty()) {
            return null;
        }

        for (final AstNode kid : kids) {
            for (final String nodeType : nodeTypes) {
                if (kid.getMixins().contains(nodeType) || nodeType.equals(kid.getPrimaryType())) {
                    return kid;
                }
            }
        }

        return null; // not found
    }

    protected String getWhitespace( final Position current,
                                    final Position previous,
                                    final String prevValue ) {
        int currIndent = current.getIndexInContent();
        int currLine = current.getLine();

        int prevIndent = previous.getIndexInContent();
        int prevLine = previous.getLine();
        final int lfCount = (currLine - prevLine);

        int whitespaceCount = 0;

        if (currIndent > prevIndent) {
            whitespaceCount = (currIndent - prevValue.length() - prevIndent - lfCount);
        } else if (prevIndent > currIndent) {
            whitespaceCount = (prevIndent + prevValue.length() - currIndent);
        }


        if ((lfCount == 0) && (whitespaceCount == 0)) {
            return "";
        }

        final StringBuilder whitespace = new StringBuilder();

        if (lfCount > 0) {
            for (int i = 0; i < lfCount; ++i) {
                whitespace.append('\n');
            }
        }

        if (whitespaceCount > 0) {
            for (int i = 0; i < whitespaceCount; ++i) {
                whitespace.append(' ');
            }
        }

        return whitespace.toString();
    }

    /**
     * @param tokens the tokens being checked (never <code>null</code> or empty)
     * @return <code>true</code> if the parser can parse the tokens
     */
    abstract boolean matches( final DdlTokenStream tokens );

    /**
     * @param tokens the tokens being processed (never <code>null</code> or empty)
     * @param parentNode the parent node of the statements being processed (cannot be <code>null</code>)
     * @return the statement node created (never <code>null</code>)
     * @throws ParsingException if there is a problem parsing the tokens
     */
    abstract AstNode parse( final DdlTokenStream tokens,
                            final AstNode parentNode ) throws ParsingException;

    protected boolean parseDefaultClause( final DdlTokenStream tokens,
                                          final AstNode columnNode ) {
        return this.teiidDdlParser.accessParseDefaultClause(tokens, columnNode);
    }

    /**
     * <code>
     * <quoted_id> (<period> <quoted_id>)*
     * </code>
     * 
     * @param tokens the tokens being process (cannot be <code>null</code>)
     * @return the identifier (never <code>null</code> or empty)
     */
    String parseIdentifier( final DdlTokenStream tokens ) {
        boolean quoted = (tokens.matches(DdlTokenizer.DOUBLE_QUOTED_STRING) || tokens.matches(DdlTokenizer.SINGLE_QUOTED_STRING));
        String id = tokens.consume();

        if (quoted) {
            id = id.substring(1, (id.length() - 1));
        }

        // check for namespaced-prefixed ID (colon will be a token if identifier is not quoted)
        if (tokens.canConsume(':')) {
            // colon found
            String uri = this.teiidDdlParser.getNamespaceUri(id);

            if (StringUtil.isBlank(uri)) {
                // assume colon is part of the name
                id = (id + ':' + tokens.consume());
            } else {
                // namespace found
                id = ('{' + uri + '}' + tokens.consume());
            }
        } else {
            int index =  id.indexOf(':');
    
            if (index != -1) {
                final String prefix = id.substring(0, index);
                String uri = this.teiidDdlParser.getNamespaceUri(prefix);
    
                // assume colon is part of the name if URI is not found
                if (!StringUtil.isBlank(uri)) {
                    // namespace found
                    id = ('{' + uri + '}' + id.substring(index + 1));
                }
            }
        }

        if (tokens.canConsume('.')) {
            id += '.' + parseIdentifier(tokens);
        }

        return id;
    }

    /**
     * <code>
     * OPTIONS <lparen> <option pair> ( <comma> <option pair> )* <rparen>
     * 
     * <option pair> == <identifier> ( <non numeric literal> | ( <plus or minus> )? <unsigned numeric literal> )
     * </code>
     * 
     * @param tokens the tokens being processed (cannot be <code>null</code>)
     * @param parentNode the parent node (cannot be <code>null</code> if the tokens are processed)
     * @return <code>true</code> if an options clause was successfully parsed
     * @throws ParsingException if there is a problem parsing an options clause
     */
    boolean parseOptionsClause( final DdlTokenStream tokens,
                                final AstNode parentNode ) throws ParsingException {
        if (tokens.canConsume(TeiidReservedWord.OPTIONS.toDdl())) {
            if (tokens.canConsume(L_PAREN)) {
                final Map<String, String> options = new HashMap<String, String>();

                { // first option
                    final String key = parseIdentifier(tokens);
                    final String value = parseValue(tokens);

                    if (!StringUtil.isBlank(value)) {
                        options.put(key, value);
                    }
                }

                // other options
                while (tokens.canConsume(COMMA)) {
                    final String nextKey = parseIdentifier(tokens);
                    final String nextValue = parseValue(tokens);

                    if (!StringUtil.isBlank(nextValue)) {
                        options.put(nextKey, nextValue);
                    }
                }

                if (tokens.canConsume(R_PAREN)) {
                    if (!options.isEmpty()) {
                        for (final Entry<String, String> optionEntry : options.entrySet()) {
                            final AstNode optionNode = getNodeFactory().node(optionEntry.getKey(),
                                                                             parentNode,
                                                                             StandardDdlLexicon.TYPE_STATEMENT_OPTION);
                            optionNode.setProperty(StandardDdlLexicon.VALUE, optionEntry.getValue());
                        }
                    }

                    return true; // well formed options clause
                }

                throw new TeiidDdlParsingException(tokens, "Unparsable options clause");
            }
        }

        return false;
    }

    protected String parseUntilTerminator( final DdlTokenStream tokens ) {
        return this.teiidDdlParser.accessParseUntilTerminator(tokens);
    }

    protected String parseUntilTerminatorIgnoreEmbeddedStatements( final DdlTokenStream tokens ) {
        return this.teiidDdlParser.accessParseUntilTerminatorIgnoreEmbeddedStatements(tokens);
    }

    /**
     * <code>
     * ( <non numeric literal> | ( <plus or minus> )? <unsigned numeric literal> )
     * </code>
     * 
     * @param tokens the tokens being process (cannot be <code>null</code>)
     * @return the value (never <code>null</code> or empty)
     */
    protected String parseValue( final DdlTokenStream tokens ) {
        final boolean quoted = (tokens.matches(DdlTokenizer.DOUBLE_QUOTED_STRING) || tokens.matches(DdlTokenizer.SINGLE_QUOTED_STRING));
        final String id = tokens.consume();

        if (quoted) {
            return id.substring(1, (id.length() - 1));
        }

        return id;
    }
    
    /**
     * 
     * @param rootNode the top level {@link AstNode}; may not be null
     */
	abstract void postProcess(AstNode rootNode);

}
