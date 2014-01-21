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

import org.modeshape.common.text.ParsingException;
import org.modeshape.sequencer.ddl.node.AstNode;

/**
 * Interface for parsing DDL files.
 */
public interface DdlParser {

    /**
     * Determine this parser's score for the given DDL string. This method is called to determine how well this parser handles the
     * supplied DDL, and is often called before the {@link #parse(String, AstNode, Object)} method.
     * 
     * @param ddl the input string to parse; may not be null
     * @param fileName the name of the DDL content, which may be used to improve the score; may be null if not known
     * @param scorer the scorer that should be used to record the score; may not be null
     * @return an object that will be passed to the {@link #parse(String, AstNode,Object)} method
     * @throws ParsingException if there is an error parsing the supplied DDL content
     */
    public Object score( String ddl,
                         String fileName,
                         DdlParserScorer scorer ) throws ParsingException;

    /**
     * Parses a DDL string, adding child {@link AstNode}s and properties to the supplied root. This method instantiates the
     * tokenizer, calls a method to allow subclasses to register keywords and statement start phrases with the tokenizer and
     * finally performs the tokenizing (i.e. tokens.start()) before calling the actual parse method.
     * 
     * @param ddl the input string to parse; may not be null
     * @param rootNode the top level {@link AstNode}; may not be null
     * @param scoreReturnObject the object returned from {@link #score(String, String, DdlParserScorer)} for the same DDL content;
     *        may be null if the {@link #score(String, String, DdlParserScorer)} method was not called
     * @throws ParsingException if there is an error parsing the supplied DDL content
     */
    public void parse( String ddl,
                       AstNode rootNode,
                       Object scoreReturnObject ) throws ParsingException;

    /**
     * Get the identifier for this parser.
     * 
     * @return the parser's identifier; never null
     */
    public String getId();

    /**
     * Allows parsers to post process the {@link AstNode} tree given the supplied root. Initial use-case would be to allow a
     * second pass through the tree to resolve any table references (FK's) that were defined out of order in the DDL
     * 
     * @param rootNode the top level {@link AstNode}; may not be null
     */
    public void postProcess( AstNode rootNode );

}
