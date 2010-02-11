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
import java.util.LinkedList;
import java.util.List;
import net.jcip.annotations.Immutable;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.text.Position;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.JcrNtLexicon;
import org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlParser;
import org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlParser;
import org.modeshape.sequencer.ddl.dialect.postgres.PostgresDdlParser;
import org.modeshape.sequencer.ddl.node.AstNode;

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

    public static final List<DdlParser> BUILTIN_PARSERS;

    static {
        List<DdlParser> parsers = new ArrayList<DdlParser>();
        parsers.add(new StandardDdlParser());
        parsers.add(new OracleDdlParser());
        parsers.add(new DerbyDdlParser());
        parsers.add(new PostgresDdlParser());
        BUILTIN_PARSERS = Collections.unmodifiableList(parsers);
    }

    private List<DdlParser> parsers;

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
     * Parse the supplied DDL content and return the {@link AstNode root node} of the AST representation.
     * 
     * @param ddl content string; may not be null
     * @param fileName the approximate name of the file containing the DDL content; may be null if this is not known
     * @return the root tree {@link AstNode}
     * @throws ParsingException if there is an error parsing the supplied DDL content
     */
    public AstNode parse( String ddl,
                          String fileName ) throws ParsingException {
        assert ddl != null;

        // Go through each parser and score the DDL content ...
        List<ScoredParser> scoredParsers = new LinkedList<ScoredParser>();
        for (DdlParser parser : parsers) {
            DdlParserScorer scorer = new DdlParserScorer();
            try {
                Object scoreResult = parser.score(ddl, fileName, scorer);
                scoredParsers.add(new ScoredParser(parser, scorer, scoreResult));
            } catch (Throwable t) {
                // Continue ...
            }
        }

        if (!scoredParsers.isEmpty()) {
            Collections.sort(scoredParsers);
        } else {
            // Just keep the parsers in order ...
            for (DdlParser parser : parsers) {
                scoredParsers.add(new ScoredParser(parser, new DdlParserScorer(), null));
            }
        }

        // Go through each of the parsers (in order) to parse the content. Start with the first one
        // and return if there is no parsing failure; otherwise, just continue with the next parser
        // until no failure ...
        AstNode astRoot = null;
        RuntimeException firstException = null;
        for (ScoredParser scoredParser : scoredParsers) {
            try {
                astRoot = new AstNode(StandardDdlLexicon.STATEMENTS_CONTAINER);
                astRoot.setProperty(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.UNSTRUCTURED);
                DdlParser parser = scoredParser.getParser();
                parser.parse(ddl, astRoot, scoredParser.getScoringResult());
                astRoot.setProperty(StandardDdlLexicon.PARSER_ID, parser.getId());
                // Success, so return the resulting AST ...
                return astRoot;
            } catch (ParsingException t) {
                if (firstException == null) firstException = t;
                // Continue ...
            } catch (RuntimeException t) {
                if (firstException == null) firstException = t;
                // Continue ...
            }
        }
        if (firstException != null) {
            throw firstException;
        }
        // No exceptions, but nothing was found ...
        throw new ParsingException(new Position(-1, 1, 0), DdlSequencerI18n.errorParsingDdlContent.text());
    }

    protected static class ScoredParser implements Comparable<ScoredParser> {
        private final DdlParserScorer scorer;
        private final DdlParser parser;
        private final Object scoringResult;

        protected ScoredParser( DdlParser parser,
                                DdlParserScorer scorer,
                                Object scoringResult ) {
            this.parser = parser;
            this.scorer = scorer;
            this.scoringResult = scoringResult;
            assert this.parser != null;
            assert this.scorer != null;
        }

        /**
         * @return parser
         */
        public DdlParser getParser() {
            return parser;
        }

        /**
         * @return scorer
         */
        public DdlParserScorer getScorer() {
            return scorer;
        }

        /**
         * @return scoringResult
         */
        public Object getScoringResult() {
            return scoringResult;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo( ScoredParser that ) {
            if (that == null) return 1;
            return that.getScorer().getScore() - this.getScorer().getScore();
        }
    }
}
