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
package org.modeshape.sequencer.ddl.dialect.teiid;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.sequencer.ddl.node.AstNode;

/**
 * A test class for {@link OptionNamespaceParser}.
 */
public class OptionNamespaceParserTest extends TeiidDdlTest {

    private OptionNamespaceParser parser;
    private AstNode rootNode;

    @Before
    public void beforeEach() {
        final TeiidDdlParser teiidDdlParser = new TeiidDdlParser();
        this.parser = new OptionNamespaceParser(teiidDdlParser);
        this.rootNode = teiidDdlParser.nodeFactory().node("ddlRootNode");
    }

    /**
     * See Teiid TestDDLParser#testNamespace()
     */
    @Test
    public void shouldParseNamespace() {
        final String uri = "http://teiid.org";
        final String alias = "teiid";
        final String content = "set namespace '" + uri + "' AS " + alias + ';';
        this.parser.parse(getTokens(content), this.rootNode);
        assertThat(this.parser.getNamespaceUri(alias), is(uri));
    }

    @Test
    public void shouldParseOptionNamespaceQuotedAliasedIdentifier() {
        // register the alias
        final String content = "SET NAMESPACE 'http://teiid.org/rest' AS REST;";
        this.parser.parse(getTokens(content), this.rootNode);

        // qualify the identifier with the namespace alias
        assertThat(this.parser.parseIdentifier(getTokens("'REST:name'")), is("{http://teiid.org/rest}name"));
    }

    @Test
    public void shouldParseOptionNamespaceUnquotedAliasedIdentifier() {
        // register the alias
        final String content = "SET NAMESPACE 'http://teiid.org/rest' AS REST;";
        this.parser.parse(getTokens(content), this.rootNode);

        // qualify the identifier with the namespace alias
        assertThat(this.parser.parseIdentifier(getTokens("REST:name")), is("{http://teiid.org/rest}name"));
    }

    @Test
    public void shouldParseOptionNamespaceDoubleQuotedAlias() {
        final String uri = "http://teiid.org/rest";
        final String alias = "REST";
        final String content = "SET NAMESPACE '" + uri + "' AS \"" + alias + "\";";
        this.parser.parse(getTokens(content), this.rootNode);
        assertThat(this.parser.getNamespaceUri(alias), is(uri));
    }

    @Test
    public void shouldParseOptionNamespaceDoubleQuotedUri() {
        final String uri = "http://teiid.org/rest";
        final String alias = "REST";
        final String content = "SET NAMESPACE \"" + uri + "\" AS " + alias + ';';
        this.parser.parse(getTokens(content), this.rootNode);
        assertThat(this.parser.getNamespaceUri(alias), is(uri));
    }

    @Test
    public void shouldParseOptionNamespaceIdentifier() {
        final String uri = "http://teiid.org/rest";
        final String alias = "REST";
        final String content = "SET NAMESPACE '" + uri + "' AS " + alias + ';';
        this.parser.parse(getTokens(content), this.rootNode);

        final String unqualifiedId = "name";
        final String aliasedId = alias + ':' + unqualifiedId;
        final String id = '{' + uri + '}' + unqualifiedId;
        assertThat(this.parser.parseIdentifier(getTokens(aliasedId)), is(id));
    }

    @Test
    public void shouldParseOptionNamespaceSingleQuotedAlias() {
        final String uri = "http://teiid.org/rest";
        final String alias = "REST";
        final String content = "SET NAMESPACE '" + uri + "' AS '" + alias + "';";
        this.parser.parse(getTokens(content), this.rootNode);
        assertThat(this.parser.getNamespaceUri(alias), is(uri));
    }

    @Test
    public void shouldParseOptionNamespaceSingleQuotedUri() {
        final String uri = "http://teiid.org/rest";
        final String alias = "REST";
        final String content = "SET NAMESPACE '" + uri + "' AS " + alias + ';';
        this.parser.parse(getTokens(content), this.rootNode);
        assertThat(this.parser.getNamespaceUri(alias), is(uri));
    }

}
