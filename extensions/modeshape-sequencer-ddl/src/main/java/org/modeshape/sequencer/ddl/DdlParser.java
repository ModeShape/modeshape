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

}
