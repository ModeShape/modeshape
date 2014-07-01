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
package org.modeshape.sequencer.javafile;

import java.util.Map;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;

/**
 * The Parser, that process the a compilation unit.
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
        // needed to be parse 1.5 code
        Map<?, ?> options = createCompilerParameters();

        // Create parser
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(source);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(resolveBindings);
        parser.setCompilerOptions(options);
        // Parse compilation unit
        return parser.createAST(null);
    }

    @SuppressWarnings( "unchecked" )
    private static Map<?, ?> createCompilerParameters() {
        Map<Object, Object> options = JavaCore.getOptions();
        options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_7);
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_7);
        options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_7);
        return options;
    }

}
