/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.sequencer.java;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;

/**
 * The Parser, that process the a compilation unit.
 * 
 * @author Serge Pagop
 */
public class CompilationUnitParser {

    /**
     * Parses and process the java source code as a compilation unit and the result it abstract syntax tree (AST) representation
     * and this action uses the third edition of java Language Specification, that gets the possibility to support J2SE 5 during
     * the parsing.
     * 
     * @param source - the java source to be parsed (i.e. the char[] contains Java source).
     * @param resolveBindings - for resolving bindings to get more informations from the unit.
     * @return Abstract syntax tree representation.
     */
    public static ASTNode runJLS3Conversion( char[] source,
                                             boolean resolveBindings ) {
        // Create parser
        ASTParser parser;
        parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(source);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(resolveBindings);
        // Parse compilation unit
        return parser.createAST(null);
    }

}
