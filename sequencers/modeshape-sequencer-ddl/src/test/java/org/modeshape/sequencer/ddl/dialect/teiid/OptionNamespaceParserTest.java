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
