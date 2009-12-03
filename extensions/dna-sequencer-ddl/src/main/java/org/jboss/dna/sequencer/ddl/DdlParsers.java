/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.sequencer.ddl;

import java.util.ArrayList;
import java.util.List;
import org.jboss.dna.common.text.ParsingException;
import org.jboss.dna.common.text.Position;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.JcrNtLexicon;
import org.jboss.dna.sequencer.ddl.dialect.derby.DerbyDdlParser;
import org.jboss.dna.sequencer.ddl.dialect.oracle.OracleDdlParser;
import org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlParser;
import org.jboss.dna.sequencer.ddl.node.AstNode;

/**
 * A parser of DDL file content. This class can be used directly to create an {@link AstNode} tree representing nodes and
 * properties for DDL statement components.
 * <p>
 * You can also provide an input or parent {@link AstNode} node as the starting point for your tree.
 * <p>
 * The parser is based on the SQL-92 and extended by specific dialects. These dialect-specific parsers provide db-specific parsing
 * of db-specific statements of statement extensions, features or properties.
 */
public class DdlParsers {
    private List<DdlParser> parsers;

    public DdlParsers() {
        parsers = new ArrayList<DdlParser>();
        parsers.add(new StandardDdlParser());
        parsers.add(new OracleDdlParser());
        parsers.add(new DerbyDdlParser());
        parsers.add(new PostgresDdlParser());
        //parsers.add(new MySqlDdlParser());
    }

    /**
     * Parses input ddl string and adds discovered child {@link AstNode}s and properties to a new root node.
     * 
     * @param ddl content string; may not be null
     * @return the root tree {@link AstNode}
     * @throws ParsingException
     */
    public AstNode parse( String ddl ) throws ParsingException {
        assert ddl != null;
        AstNode rootNode = new AstNode(StandardDdlLexicon.STATEMENTS_CONTAINER);
        rootNode.setProperty(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.UNSTRUCTURED);

        parse(ddl, rootNode);

        return rootNode;
    }

    /**
     * Parses input ddl string and adds discovered child {@link AstNode}s and properties.
     * 
     * @param ddl content string; may not be null
     * @param rootNode the root {@link AstNode}; may not be null
     * @return true if parsed successfully
     * @throws ParsingException
     */
    public boolean parse( String ddl,
                          AstNode rootNode ) throws ParsingException {
        assert ddl != null;
        assert rootNode != null;
        // Find registered parser for DDL

        DdlTokenStream tokens = null;
        DdlParser validParser = null;
        DdlTokenStream validTokens = null;
        

        // FIRST token should be DIALECT
        // for (DdlParser parser : library.getInstances()) {
        for (DdlParser parser : parsers) {
            if (parser.isType(ddl)) {
                validParser = parser;
                break;
            }
        }

        if (validParser == null) {
            // NO TYPE DEFINED IN DDL file
            // FIND BEST KEYWORD FIT

            int keywordCount = 0;
            // for (DdlParser parser : library.getInstances()) {
            for (DdlParser parser : parsers) {
                tokens = new DdlTokenStream(ddl, DdlTokenStream.ddlTokenizer(false), false);
                parser.registerWords(tokens);
                tokens.start(); // COMPLETE TOKENIZATION
                int numKeywords = parser.getNumberOfKeyWords(tokens);
                if (numKeywords > keywordCount) {
                    keywordCount = numKeywords;
                    validParser = parser;
                    validTokens = tokens;
                }
            }
            if (validTokens != null) {
                validTokens.rewind();
            }
        } else {
            validTokens = new DdlTokenStream(ddl, DdlTokenStream.ddlTokenizer(false), false);
            validParser.registerWords(validTokens);
            validTokens.start(); // COMPLETE TOKENIZATION
        }

        if (validParser == null) {
            String msg = "NO VALID PARSER FOUND";
            throw new ParsingException(new Position(-1, 1, 0), msg);
        }

        // tokens = new DdlTokenStream(ddl, DdlTokenStream.ddlTokenizer(false), false);
        // validParser.registerWords(tokens);
        // tokens.start();
        boolean success = validParser.parse(validTokens, rootNode);
        rootNode.setProperty(StandardDdlLexicon.PARSER_ID, validParser.getId());

        return success;
    }
}
