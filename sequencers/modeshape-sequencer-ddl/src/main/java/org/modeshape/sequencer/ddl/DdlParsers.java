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
package org.modeshape.sequencer.ddl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.text.Position;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlParser;
import org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlParser;
import org.modeshape.sequencer.ddl.dialect.postgres.PostgresDdlParser;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlParser;
import org.modeshape.sequencer.ddl.node.AstNode;
import org.modeshape.sequencer.ddl.node.AstNodeFactory;

/**
 * A set of parsers capable of understanding DDL file content. This class can be used directly to create an {@link AstNode} tree
 * representing nodes and properties for DDL statement components.
 * <p>
 * You can also provide an input or parent {@link AstNode} node as the starting point for your tree.
 * </p>
 * <p>
 * The parser is based on the SQL-92 and extended by specific dialects. These dialect-specific parsers provide db-specific parsing
 * of db-specific statements of statement extensions, features or properties.
 * </p>
 */
@Immutable
public class DdlParsers {

    /**
     * Sorts the parser scores.
     */
    private static final Comparator<Entry<DdlParser, Integer>> SORTER = new Comparator<Entry<DdlParser, Integer>>() {

        @Override
        public int compare( final Entry<DdlParser, Integer> thisEntry,
                            final Entry<DdlParser, Integer> thatEntry ) {
            // reverse order as we want biggest value to sort first
            int result = (thisEntry.getValue().compareTo(thatEntry.getValue()) * -1);

            // default to standard SQL parser if score is a tie
            if (result == 0) {
                if (StandardDdlParser.PARSE_ID.equals(thisEntry.getKey().getId())
                    && !StandardDdlParser.PARSE_ID.equals(thatEntry.getKey().getId())) {
                    return -1;
                }

                if (StandardDdlParser.PARSE_ID.equals(thatEntry.getKey().getId())
                    && !StandardDdlParser.PARSE_ID.equals(thisEntry.getKey().getId())) {
                    return 1;
                }
            }

            return result;
        }

    };

    public static final List<DdlParser> BUILTIN_PARSERS;

    static {
        List<DdlParser> parsers = new ArrayList<DdlParser>();
        parsers.add(new StandardDdlParser());
        parsers.add(new OracleDdlParser());
        parsers.add(new DerbyDdlParser());
        parsers.add(new PostgresDdlParser());
        parsers.add(new TeiidDdlParser());
        BUILTIN_PARSERS = Collections.unmodifiableList(parsers);
    }

    private List<DdlParser> parsers;
    private AstNodeFactory nodeFactory = new AstNodeFactory();

    /**
     * Create an instance that uses all of the {@link #BUILTIN_PARSERS built-in parsers}.
     */
    public DdlParsers() {
        this.parsers = BUILTIN_PARSERS;
    }

    /**
     * Create an instance that uses the supplied parsers, in order.
     * 
     * @param parsers the list of parsers; may be empty or null if the {@link #BUILTIN_PARSERS built-in parsers} should be used
     */
    public DdlParsers( List<DdlParser> parsers ) {
        this.parsers = (parsers != null && !parsers.isEmpty()) ? parsers : BUILTIN_PARSERS;
    }

    /**
     * @param ddl the DDL being parsed (cannot be <code>null</code> or empty)
     * @param parserId the identifier of the parser to use (can be <code>null</code> or empty if best matched parser should be
     *        used)
     * @return the root tree {@link AstNode}
     * @throws ParsingException if there is an error parsing the supplied DDL content
     * @throws IllegalArgumentException if a parser with the specified identifier cannot be found
     */
    public AstNode parseUsing( final String ddl,
                               final String parserId ) throws ParsingException {
        CheckArg.isNotEmpty(ddl, "ddl");
        CheckArg.isNotEmpty(parserId, "parserId");

        DdlParser parser = null;

        for (final DdlParser ddlParser : this.parsers) {
            if (parserId.equals(ddlParser.getId())) {
                parser = ddlParser;
                break;
            }
        }

        if (parser == null) {
            throw new ParsingException(Position.EMPTY_CONTENT_POSITION,
                                       DdlSequencerI18n.unknownParser.text(parserId));
        }

        // create DDL root node
        AstNode astRoot = this.nodeFactory.node(StandardDdlLexicon.STATEMENTS_CONTAINER);
        astRoot.setProperty(JcrConstants.JCR_PRIMARY_TYPE, JcrConstants.NT_UNSTRUCTURED);
        astRoot.setProperty(StandardDdlLexicon.PARSER_ID, parserId);

        // parse
        parser.parse(ddl, astRoot, null);

        return astRoot;
    }

    /**
     * Parse the supplied DDL content and return the {@link AstNode root node} of the AST representation.
     * 
     * @param ddl content string; may not be null
     * @param fileName the approximate name of the file containing the DDL content; may be null if this is not known
     * @return the root tree {@link AstNode}
     * @throws ParsingException if there is an error parsing the supplied DDL content
     */
    public AstNode parse( final String ddl,
                          final String fileName ) throws ParsingException {
        CheckArg.isNotEmpty(ddl, "ddl");
        RuntimeException firstException = null;

        // Go through each parser and score the DDL content
        final Map<DdlParser, Integer> scoreMap = new HashMap<DdlParser, Integer>(this.parsers.size());
        final DdlParserScorer scorer = new DdlParserScorer();

        for (final DdlParser parser : this.parsers) {
            try {
                parser.score(ddl, fileName, scorer);
                scoreMap.put(parser, scorer.getScore());
            } catch (RuntimeException e) {
                if (firstException == null) {
                    firstException = e;
                }
            } finally {
                scorer.reset();
            }
        }

        if (scoreMap.isEmpty()) {
            if (firstException == null) {
                throw new ParsingException(Position.EMPTY_CONTENT_POSITION,
                                           DdlSequencerI18n.errorParsingDdlContent.text(this.parsers.size()));
            }

            throw firstException;
        }

        // sort the scores
        final List<Entry<DdlParser, Integer>> scoredParsers = new ArrayList<Entry<DdlParser, Integer>>(scoreMap.entrySet());
        Collections.sort(scoredParsers, SORTER);

        firstException = null;
        AstNode astRoot = null;

        for (final Entry<DdlParser, Integer> scoredParser : scoredParsers) {
            try {
                final DdlParser parser = scoredParser.getKey();

                // create DDL root node
                astRoot = this.nodeFactory.node(StandardDdlLexicon.STATEMENTS_CONTAINER);
                astRoot.setProperty(JcrConstants.JCR_PRIMARY_TYPE, JcrConstants.NT_UNSTRUCTURED);
                astRoot.setProperty(StandardDdlLexicon.PARSER_ID, parser.getId());

                // parse
                parser.parse(ddl, astRoot, null);
                return astRoot; // successfully parsed
            } catch (final RuntimeException e) {
                if (astRoot != null) {
                    astRoot.removeFromParent();
                }

                if (firstException == null) {
                    firstException = e;
                }
            }
        }

        if (firstException == null) {
            throw new ParsingException(Position.EMPTY_CONTENT_POSITION, DdlSequencerI18n.errorParsingDdlContent.text());
        }

        throw firstException;
    }
}
