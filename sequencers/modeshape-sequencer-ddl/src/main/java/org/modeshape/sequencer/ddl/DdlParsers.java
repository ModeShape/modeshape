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
package org.modeshape.sequencer.ddl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.text.Position;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlParser;
import org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlParser;
import org.modeshape.sequencer.ddl.dialect.postgres.PostgresDdlParser;
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
                if (StandardDdlParser.ID.equals(thisEntry.getKey().getId())
                    && !StandardDdlParser.ID.equals(thatEntry.getKey().getId())) {
                    return -1;
                }

                if (StandardDdlParser.ID.equals(thatEntry.getKey().getId())
                    && !StandardDdlParser.ID.equals(thisEntry.getKey().getId())) {
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

    private AstNode createDdlStatementsContainer( final String parserId ) {
        final AstNode node = this.nodeFactory.node(StandardDdlLexicon.STATEMENTS_CONTAINER);
        node.setProperty(JcrConstants.JCR_PRIMARY_TYPE, JcrConstants.NT_UNSTRUCTURED);
        node.setProperty(StandardDdlLexicon.PARSER_ID, parserId);
        return node;
    }

    /**
     * @param id the identifier of the parser being requested (cannot be <code>null</code> or empty)
     * @return the parser or <code>null</code> if not found
     */
    public DdlParser getParser( final String id ) {
        CheckArg.isNotEmpty(id, "id");

        for (final DdlParser parser : this.parsers) {
            if (parser.getId().equals(id)) {
                return parser;
            }
        }

        return null;
    }

    /**
     * @return a copy of the DDL parsers used in this instance (never <code>null</code> or empty)
     */
    public Set<DdlParser> getParsers() {
        return new HashSet<DdlParser>(this.parsers);
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

        DdlParser parser = getParser(parserId);

        if (parser == null) {
            throw new ParsingException(Position.EMPTY_CONTENT_POSITION, DdlSequencerI18n.unknownParser.text(parserId));
        }

        // create DDL root node
        AstNode astRoot = createDdlStatementsContainer(parserId);

        // parse
        parser.parse(ddl, astRoot, null);

        return astRoot;
    }

    /**
     * Parse the supplied DDL using multiple parsers, returning the result of each parser with its score in the order of highest
     * scoring to lowest scoring.
     * 
     * @param ddl the DDL being parsed (cannot be <code>null</code> or empty)
     * @param firstParserId the identifier of the first parser to use (cannot be <code>null</code> or empty)
     * @param secondParserId the identifier of the second parser to use (cannot be <code>null</code> or empty)
     * @param additionalParserIds the identifiers of additional parsers that should be used; may be empty but not contain a null
     *        identifier value
     * @return the list of {@link ParsingResult} instances, one for each parser, ordered from highest score to lowest score (never
     *         <code>null</code> or empty)
     * @throws ParsingException if there is an error parsing the supplied DDL content
     * @throws IllegalArgumentException if a parser with the specified identifier cannot be found
     */
    public List<ParsingResult> parseUsing( final String ddl,
                                           final String firstParserId,
                                           final String secondParserId,
                                           final String... additionalParserIds ) throws ParsingException {
        CheckArg.isNotEmpty(firstParserId, "firstParserId");
        CheckArg.isNotEmpty(secondParserId, "secondParserId");

        if (additionalParserIds != null) {
            CheckArg.containsNoNulls(additionalParserIds, "additionalParserIds");
        }

        final int numParsers = ((additionalParserIds == null) ? 2 : (additionalParserIds.length + 2));
        final List<DdlParser> selectedParsers = new ArrayList<DdlParser>(numParsers);

        { // add first parser
            final DdlParser parser = getParser(firstParserId);

            if (parser == null) {
                throw new ParsingException(Position.EMPTY_CONTENT_POSITION, DdlSequencerI18n.unknownParser.text(firstParserId));
            }

            selectedParsers.add(parser);
        }

        { // add second parser
            final DdlParser parser = getParser(secondParserId);

            if (parser == null) {
                throw new ParsingException(Position.EMPTY_CONTENT_POSITION, DdlSequencerI18n.unknownParser.text(secondParserId));
            }

            selectedParsers.add(parser);
        }

        // add remaining parsers
        if ((additionalParserIds != null) && (additionalParserIds.length != 0)) {
            for (final String id : additionalParserIds) {
                final DdlParser parser = getParser(id);

                if (parser == null) {
                    throw new ParsingException(Position.EMPTY_CONTENT_POSITION, DdlSequencerI18n.unknownParser.text(id));
                }

                selectedParsers.add(parser);
            }
        }

        return parseUsing(ddl, selectedParsers);
    }

    private List<ParsingResult> parseUsing( final String ddl,
                                            final List<DdlParser> parsers ) {
        CheckArg.isNotEmpty(ddl, "ddl");

        final List<ParsingResult> results = new ArrayList<DdlParsers.ParsingResult>(this.parsers.size());
        final DdlParserScorer scorer = new DdlParserScorer();

        for (final DdlParser parser : this.parsers) {
            final String parserId = parser.getId();
            int score = ParsingResult.NO_SCORE;
            AstNode rootNode = null;
            Exception error = null;

            try {
                // score
                final Object scorerOutput = parser.score(ddl, null, scorer);
                score = scorer.getScore();

                // create DDL root node
                rootNode = createDdlStatementsContainer(parserId);

                // parse
                parser.parse(ddl, rootNode, scorerOutput);
            } catch (final RuntimeException e) {
                error = e;
            } finally {
                final ParsingResult result = new ParsingResult(parserId, rootNode, score, error);
                results.add(result);
                scorer.reset();
            }
        }

        Collections.sort(results);
        return results;
    }

    /**
     * Parse the supplied DDL using all registered parsers, returning the result of each parser with its score in the order of
     * highest scoring to lowest scoring.
     * 
     * @param ddl the DDL being parsed (cannot be <code>null</code> or empty)
     * @return the list or {@link ParsingResult} instances, one for each parser, ordered from highest score to lowest score
     * @throws ParsingException if there is an error parsing the supplied DDL content
     * @throws IllegalArgumentException if a parser with the specified identifier cannot be found
     */
    public List<ParsingResult> parseUsingAll( final String ddl ) throws ParsingException {
        return parseUsing(ddl, this.parsers);
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
                astRoot = createDdlStatementsContainer(parser.getId());

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

    /**
     * Represents a parsing result of one parser parsing one DDL input.
     */
    @Immutable
    public class ParsingResult implements Comparable<ParsingResult> {

        public static final int NO_SCORE = -1;

        private final Exception error;
        private final String id;
        private final AstNode rootNode;
        private final int score;

        /**
         * @param parserId the parser identifier (cannot be <code>null</code> or empty)
         * @param rootTreeNode the node at the root of the parse tree (can be <code>null</code> if an error occurred)
         * @param parserScore the parsing score (can have {@link #NO_SCORE no score} if an error occurred
         * @param parsingError an error that occurred during parsing (can be <code>null</code>)
         */
        public ParsingResult( final String parserId,
                              final AstNode rootTreeNode,
                              final int parserScore,
                              final Exception parsingError ) {
            CheckArg.isNotEmpty(parserId, "parserId");

            this.id = parserId;
            this.rootNode = rootTreeNode;
            this.score = parserScore;
            this.error = parsingError;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        @Override
        public int compareTo( final ParsingResult that ) {
            if ((this == that) || (this.score == that.score)) {
                return 0;
            }

            return ((this.score > that.score) ? -1 : 1);
        }

        /**
         * @return the parsing error (<code>null</code> if no error occurred)
         */
        public Exception getError() {
            return this.error;
        }

        /**
         * @return the parser identifier (never <code>null</code> or empty)
         */
        public String getParserId() {
            return this.id;
        }

        /**
         * @return the root <code>AstNode</code> (can be <code>null</code> if a parsing error occurred)
         */
        public AstNode getRootTree() {
            return this.rootNode;
        }

        /**
         * @return the parsing score
         */
        public int getScore() {
            return this.score;
        }

    }

}
