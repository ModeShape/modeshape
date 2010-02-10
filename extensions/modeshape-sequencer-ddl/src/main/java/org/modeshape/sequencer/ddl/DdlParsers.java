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

        // Go through each parser and grab the one with the greatest score ...
        int maxScore = 0;
        AstNode astOfMaxScore = null;
        Throwable firstException = null;
        for (DdlParser parser : parsers) {
            DdlParserScorer scorer = new DdlParserScorer();
            try {
                AstNode rootNode = new AstNode(StandardDdlLexicon.STATEMENTS_CONTAINER);
                rootNode.setProperty(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.UNSTRUCTURED);
                parser.parse(ddl, fileName, rootNode, scorer);
                rootNode.setProperty(StandardDdlLexicon.PARSER_ID, parser.getId());
                int score = scorer.getScore();
                if (score > maxScore) {
                    maxScore = score;
                    astOfMaxScore = rootNode;
                }
            } catch (Throwable t) {
                if (firstException == null) firstException = t;
                // Continue ...
            }
        }
        if (astOfMaxScore == null) {
            if (firstException != null) {
                if (firstException instanceof ParsingException) {
                    throw (ParsingException)firstException;
                }
                throw (RuntimeException)firstException;
            }
            // No exceptions, but nothing was found ...
            throw new ParsingException(new Position(-1, 1, 0), DdlSequencerI18n.errorParsingDdlContent.text());
        }
        return astOfMaxScore;
    }
}
