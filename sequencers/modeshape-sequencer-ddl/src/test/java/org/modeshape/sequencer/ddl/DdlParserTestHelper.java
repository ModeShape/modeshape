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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_PROBLEM;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_UNKNOWN_STATEMENT;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import org.modeshape.sequencer.ddl.node.AstNode;

/**
 *
 */
public class DdlParserTestHelper implements DdlConstants {
    private boolean printToConsole = false;
    public final static String NEWLINE = "\n";
    protected StandardDdlParser parser;
    protected AstNode rootNode;
    protected DdlParserScorer scorer;

    /**
     * @param value String value to print to console
     */
    public void printTest( String value ) {
        if (printToConsole) {
            System.out.println("TEST:  " + value);
        }
    }

    /**
     * @param value String value to print to console
     */
    public void printResult( String value ) {
        if (printToConsole) {
            System.out.println(value);
        }
    }

    /**
     * @return printToConsole
     */
    public boolean isPrintToConsole() {
        return printToConsole;
    }

    public void setRootNode( AstNode rootNode ) {
        this.rootNode = rootNode;
    }

    public AstNode getRootNode() {
        return this.rootNode;
    }

    /**
     * @param printToConsole Sets printToConsole to the specified value.
     */
    public void setPrintToConsole( boolean printToConsole ) {
        this.printToConsole = printToConsole;
    }

    public boolean hasMixinType( AstNode node,
                                 String mixinType ) {
        return node.getMixins().contains(mixinType);
    }

    protected String getFileContent( String filePath ) {
        StringBuilder sb = new StringBuilder(1000);

        if (isPrintToConsole()) {
            System.out.println("   Getting Content for File = " + filePath);
        }

        if (filePath != null && filePath.length() > 0) {
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream(filePath)));
                int ch = in.read();
                while (ch > -1) {
                    sb.append((char)ch);
                    ch = in.read();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (java.io.IOException e) {
                    }
                }
            }
        }
        return sb.toString();
    }

    protected DdlTokenStream getTokens( final String content ) {
        final DdlTokenStream tokens = new DdlTokenStream(content, DdlTokenStream.ddlTokenizer(false), false);
        tokens.start();
        return tokens;
    }

    public void printUnknownStatements( StandardDdlParser parser,
                                        AstNode rootNode ) {
        printResult("============== UNKNOWN STATEMENTS =======================\n");
        List<AstNode> unknownNodes = parser.nodeFactory().getChildrenForType(rootNode, TYPE_UNKNOWN_STATEMENT);

        for (AstNode node : unknownNodes) {
            printResult(node.toString());
        }
        printResult("=========================================================\n");
    }

    public void printProblems( StandardDdlParser parser,
                               AstNode rootNode ) {
        printResult("==================== PROBLEMS ===========================\n");
        List<AstNode> problems = parser.nodeFactory().getChildrenForType(rootNode, TYPE_PROBLEM);

        for (AstNode node : problems) {
            printResult(node.toString());
        }
        printResult("=========================================================\n");
    }

    protected void assertScoreAndParse( String content,
                                        String filename,
                                        int childCount ) {
        // First try with scoring ...
        Object result = parser.score(content, filename, scorer);
        parser.parse(content, getRootNode(), result);
        assertThat(scorer.getScore() > 0, is(true));
        if (childCount >= 0) {
            assertThat(getRootNode().getChildCount(), is(childCount));
        }

        // Do it again, but this time without scoring first ...
        setRootNode(parser.nodeFactory().node("ddlRootNode"));
        parser.setRootNode(getRootNode());
        parser.parse(content, getRootNode(), null);
        if (childCount >= 0) {
            assertThat(getRootNode().getChildCount(), is(childCount));
        }
    }

    protected void assertMixinType( final AstNode node,
                                    final String expectedMixinType ) {
        assertThat(node.getMixins(), hasItem(expectedMixinType));
    }

    protected void assertProperty( final AstNode node,
                                   final String name,
                                   final Object expectedValue ) {
        Object actualValue = node.getProperty(name);
        assertThat(actualValue, is(expectedValue));
    }

}
