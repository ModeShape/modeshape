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

import org.modeshape.common.text.ParsingException;
import org.modeshape.sequencer.ddl.node.AstNode;

/**
 * Interface for parsing DDL files.
 */
public interface DdlParser {

    /**
     * Given the supplied token stream containing the DDL content, count the number of keywords that are used.
     * 
     * @param tokens the token stream containing the tokenized DDL content. may not be null
     * @return the number of tokens used in the stream; never negative
     */
    int getNumberOfKeyWords( DdlTokenStream tokens );

    /**
     * Parses a DDL string and adds discovered child {@link AstNode}s and properties. This method instantiates the tokenizer,
     * calls a method to allow subclasses to register keywords and statement start phrases with the tokenizer and finally performs
     * the tokenizing (i.e. tokens.start()) before calling the actual parse method.
     * 
     * @param ddl the input string to parse; may not be null
     * @param rootNode the top level {@link AstNode}; may not be null
     * @return true if parsing successful (i.e. no problems were discovered during parsing)
     * @throws ParsingException if there is an error parsing the supplied DDL content
     */
    public boolean parse( String ddl,
                          AstNode rootNode ) throws ParsingException;

    /**
     * Parses DDL content from the {@link DdlTokenStream} provided. Parsed data is converted to an AST via {@link AstNode}s. This
     * tree, represents all recognizable statements and properties within a DDL file. Note that db-specific dialects will need to
     * override many methods, as well as add methods to fully parse their specific DDL.
     * 
     * @param tokens the tokenized {@link DdlTokenStream} of the DDL input content; may not be null
     * @param rootNode the top level {@link AstNode}; may not be null
     * @return true if parsing successful (i.e. no problems were discovered during parsing)
     * @throws ParsingException if there is an error parsing the supplied DDL content
     */
    public boolean parse( DdlTokenStream tokens,
                          AstNode rootNode ) throws ParsingException;

    /**
     * Method provide a means for DB-specific Statement implementations can contribute DDL Start Phrases and Keywords. These words
     * are critical pieces of data the parser needs to segment the DDL file into statements. These statements all begin with
     * unique start phrases like: CREATE TABLE, DROP VIEW, ALTER TABLE. The base method provided here registers the set of SQL 92
     * based start phrases as well as the set of SQL 92 reserved words (i.e. CREATE, DROP, SCHEMA, CONSTRAINT, etc...).
     * 
     * @param tokens the token stream containing the tokenized DDL content. may not be null
     */
    public void registerWords( DdlTokenStream tokens );

    /**
     * Parse the supplied content and determine whether this parser is capable of understanding the content, including looking for
     * clues in the content.
     * 
     * @param ddl the input string to parse; may not be null
     * @return true if tokens contain DDL type matching instance of DdlParser.
     */
    public boolean isType( String ddl );

    /**
     * Get the identifier for this parser.
     * 
     * @return the parser's identifier; never null
     */
    public String getId();

}
