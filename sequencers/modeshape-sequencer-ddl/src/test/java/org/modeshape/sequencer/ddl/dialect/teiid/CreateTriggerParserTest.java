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
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.sequencer.ddl.DdlConstants;
import org.modeshape.sequencer.ddl.node.AstNode;

/**
 * A test class for {@link CreateTriggerParser}.
 */
public class CreateTriggerParserTest extends TeiidDdlTest {

    private CreateTriggerParser parser;
    private AstNode rootNode;
    private AstNode tableRefNode;

    @Before
    public void beforeEach() {
        final TeiidDdlParser teiidDdlParser = new TeiidDdlParser();
        this.parser = new CreateTriggerParser(teiidDdlParser);
        this.rootNode = teiidDdlParser.nodeFactory().node("ddlRootNode");

        // create table that is referenced by create trigger statements
        final CreateTableParser tableParser = new CreateTableParser(teiidDdlParser);
        final String content = "CREATE FOREIGN TABLE foo(col1 string, col2 string)";
        this.tableRefNode = tableParser.parse(getTokens(content), this.rootNode);
    }

    @Test
    public void shouldParseUnresolvedTableReference() {
        final String content = "CREATE TRIGGER ON missingTable INSTEAD OF DELETE AS FOR EACH ROW DELETE FROM x WHERE foo = 'bar';";
        final AstNode triggerNode = this.parser.parse(getTokens(content), this.rootNode);
        assertThat(triggerNode.getName(), is("missingTable"));
        assertMixinType(triggerNode, TeiidDdlLexicon.CreateTrigger.STATEMENT);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.INSTEAD_OF, DdlConstants.DELETE);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.ATOMIC, true);
        assertThat(triggerNode.getProperty(TeiidDdlLexicon.CreateTrigger.TABLE_REFERENCE), is(nullValue()));
    }

    @Test
    public void shouldParseCreateTriggerDelete() {
        final String content = "CREATE TRIGGER ON foo INSTEAD OF DELETE AS FOR EACH ROW DELETE FROM x WHERE foo = 'bar';";
        final AstNode triggerNode = this.parser.parse(getTokens(content), this.rootNode);
        assertThat(triggerNode.getName(), is("foo"));
        assertMixinType(triggerNode, TeiidDdlLexicon.CreateTrigger.STATEMENT);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.INSTEAD_OF, DdlConstants.DELETE);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.ATOMIC, true);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.TABLE_REFERENCE, this.tableRefNode);

        // row actions
        assertThat(triggerNode.getChildCount(), is(1));
        assertThat(triggerNode.getChild(0).getName(), is(TeiidDdlLexicon.CreateTrigger.ROW_ACTION));
        assertMixinType(triggerNode.getChild(0), TeiidDdlLexicon.CreateTrigger.TRIGGER_ROW_ACTION);
        assertProperty(triggerNode.getChild(0), TeiidDdlLexicon.CreateTrigger.ACTION, "DELETE FROM x WHERE foo = 'bar';");
    }

    @Test
    public void shouldParseCreateTriggerDeleteWithAtomicBeginBlockOneStatement() {
        final String content = "CREATE TRIGGER ON foo INSTEAD OF DELETE AS FOR EACH ROW BEGIN ATOMIC DELETE FROM x WHERE foo = 'bar'; END";
        final AstNode triggerNode = this.parser.parse(getTokens(content), this.rootNode);
        assertThat(triggerNode.getName(), is("foo"));
        assertMixinType(triggerNode, TeiidDdlLexicon.CreateTrigger.STATEMENT);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.INSTEAD_OF, DdlConstants.DELETE);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.ATOMIC, true);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.TABLE_REFERENCE, this.tableRefNode);

        // row actions
        assertThat(triggerNode.getChildCount(), is(1));
        assertThat(triggerNode.getChild(0).getName(), is(TeiidDdlLexicon.CreateTrigger.ROW_ACTION));
        assertMixinType(triggerNode.getChild(0), TeiidDdlLexicon.CreateTrigger.TRIGGER_ROW_ACTION);
        assertProperty(triggerNode.getChild(0), TeiidDdlLexicon.CreateTrigger.ACTION, "DELETE FROM x WHERE foo = 'bar';");
    }

    @Test
    public void shouldParseCreateTriggerDeleteWithBeginBlockOneStatement() {
        final String content = "CREATE TRIGGER ON foo INSTEAD OF DELETE AS FOR EACH ROW BEGIN DELETE FROM x WHERE foo = 'bar'; END";
        final AstNode triggerNode = this.parser.parse(getTokens(content), this.rootNode);
        assertThat(triggerNode.getName(), is("foo"));
        assertMixinType(triggerNode, TeiidDdlLexicon.CreateTrigger.STATEMENT);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.INSTEAD_OF, DdlConstants.DELETE);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.ATOMIC, false);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.TABLE_REFERENCE, this.tableRefNode);

        // row actions
        assertThat(triggerNode.getChildCount(), is(1));
        assertThat(triggerNode.getChild(0).getName(), is(TeiidDdlLexicon.CreateTrigger.ROW_ACTION));
        assertMixinType(triggerNode.getChild(0), TeiidDdlLexicon.CreateTrigger.TRIGGER_ROW_ACTION);
        assertProperty(triggerNode.getChild(0), TeiidDdlLexicon.CreateTrigger.ACTION, "DELETE FROM x WHERE foo = 'bar';");
    }

    @Test
    public void shouldParseCreateTriggerDeleteWithBeginBlockTwoStatements() {
        final String content = "CREATE TRIGGER ON foo INSTEAD OF DELETE AS FOR EACH ROW BEGIN DELETE FROM x WHERE foo = 'bar'; DELETE FROM x WHERE bar = 'foo'; END";
        final AstNode triggerNode = this.parser.parse(getTokens(content), this.rootNode);
        assertThat(triggerNode.getName(), is("foo"));
        assertMixinType(triggerNode, TeiidDdlLexicon.CreateTrigger.STATEMENT);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.INSTEAD_OF, DdlConstants.DELETE);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.ATOMIC, false);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.TABLE_REFERENCE, this.tableRefNode);

        // row actions
        assertThat(triggerNode.getChildCount(), is(2));
        assertThat(triggerNode.getChild(0).getName(), is(TeiidDdlLexicon.CreateTrigger.ROW_ACTION));
        assertMixinType(triggerNode.getChild(0), TeiidDdlLexicon.CreateTrigger.TRIGGER_ROW_ACTION);
        assertProperty(triggerNode.getChild(0), TeiidDdlLexicon.CreateTrigger.ACTION, "DELETE FROM x WHERE foo = 'bar';");
        assertThat(triggerNode.getChild(1).getName(), is(TeiidDdlLexicon.CreateTrigger.ROW_ACTION));
        assertMixinType(triggerNode.getChild(1), TeiidDdlLexicon.CreateTrigger.TRIGGER_ROW_ACTION);
        assertProperty(triggerNode.getChild(1), TeiidDdlLexicon.CreateTrigger.ACTION, "DELETE FROM x WHERE bar = 'foo';");
    }

    @Test
    public void shouldParseCreateTriggerInsert() {
        final String content = "CREATE TRIGGER ON foo INSTEAD OF INSERT AS FOR EACH ROW insert into g1 (e1, e2) values (1, 'trig');";
        final AstNode triggerNode = this.parser.parse(getTokens(content), this.rootNode);
        assertThat(triggerNode.getName(), is("foo"));
        assertMixinType(triggerNode, TeiidDdlLexicon.CreateTrigger.STATEMENT);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.INSTEAD_OF, DdlConstants.INSERT);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.ATOMIC, true);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.TABLE_REFERENCE, this.tableRefNode);

        // row actions
        assertThat(triggerNode.getChildCount(), is(1));
        assertThat(triggerNode.getChild(0).getName(), is(TeiidDdlLexicon.CreateTrigger.ROW_ACTION));
        assertMixinType(triggerNode.getChild(0), TeiidDdlLexicon.CreateTrigger.TRIGGER_ROW_ACTION);
        assertProperty(triggerNode.getChild(0),
                       TeiidDdlLexicon.CreateTrigger.ACTION,
                       "insert into g1 (e1, e2) values (1, 'trig');");
    }

    @Test
    public void shouldParseCreateTriggerInsertWithAtomicBeginBlockOneStatement() {
        final String content = "CREATE TRIGGER ON foo INSTEAD OF INSERT AS FOR EACH ROW BEGIN ATOMIC insert into g1 (e1, e2) values (1, 'trig'); END";
        final AstNode triggerNode = this.parser.parse(getTokens(content), this.rootNode);
        assertThat(triggerNode.getName(), is("foo"));
        assertMixinType(triggerNode, TeiidDdlLexicon.CreateTrigger.STATEMENT);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.INSTEAD_OF, DdlConstants.INSERT);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.ATOMIC, true);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.TABLE_REFERENCE, this.tableRefNode);

        // row actions
        assertThat(triggerNode.getChildCount(), is(1));
        assertThat(triggerNode.getChild(0).getName(), is(TeiidDdlLexicon.CreateTrigger.ROW_ACTION));
        assertMixinType(triggerNode.getChild(0), TeiidDdlLexicon.CreateTrigger.TRIGGER_ROW_ACTION);
        assertProperty(triggerNode.getChild(0),
                       TeiidDdlLexicon.CreateTrigger.ACTION,
                       "insert into g1 (e1, e2) values (1, 'trig');");
    }

    @Test
    public void shouldParseCreateTriggerInsertWithBeginBlockOneStatement() {
        final String content = "CREATE TRIGGER ON foo INSTEAD OF INSERT AS FOR EACH ROW BEGIN insert into g1 (e1, e2) values (1, 'trig'); END";
        final AstNode triggerNode = this.parser.parse(getTokens(content), this.rootNode);
        assertThat(triggerNode.getName(), is("foo"));
        assertMixinType(triggerNode, TeiidDdlLexicon.CreateTrigger.STATEMENT);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.INSTEAD_OF, DdlConstants.INSERT);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.ATOMIC, false);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.TABLE_REFERENCE, this.tableRefNode);

        // row actions
        assertThat(triggerNode.getChildCount(), is(1));
        assertThat(triggerNode.getChild(0).getName(), is(TeiidDdlLexicon.CreateTrigger.ROW_ACTION));
        assertMixinType(triggerNode.getChild(0), TeiidDdlLexicon.CreateTrigger.TRIGGER_ROW_ACTION);
        assertProperty(triggerNode.getChild(0),
                       TeiidDdlLexicon.CreateTrigger.ACTION,
                       "insert into g1 (e1, e2) values (1, 'trig');");
    }

    @Test
    public void shouldParseCreateTriggerInsertWithBeginBlockTwoStatements() {
        final String content = "CREATE TRIGGER ON foo INSTEAD OF INSERT AS FOR EACH ROW BEGIN insert into g1 (e1, e2) values (1, 'trig'); insert into g1 (e1, e2) values (2, 'hammer'); END";
        final AstNode triggerNode = this.parser.parse(getTokens(content), this.rootNode);
        assertThat(triggerNode.getName(), is("foo"));
        assertMixinType(triggerNode, TeiidDdlLexicon.CreateTrigger.STATEMENT);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.INSTEAD_OF, DdlConstants.INSERT);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.ATOMIC, false);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.TABLE_REFERENCE, this.tableRefNode);

        // row actions
        assertThat(triggerNode.getChildCount(), is(2));
        assertThat(triggerNode.getChild(0).getName(), is(TeiidDdlLexicon.CreateTrigger.ROW_ACTION));
        assertMixinType(triggerNode.getChild(0), TeiidDdlLexicon.CreateTrigger.TRIGGER_ROW_ACTION);
        assertProperty(triggerNode.getChild(0),
                       TeiidDdlLexicon.CreateTrigger.ACTION,
                       "insert into g1 (e1, e2) values (1, 'trig');");
        assertThat(triggerNode.getChild(1).getName(), is(TeiidDdlLexicon.CreateTrigger.ROW_ACTION));
        assertMixinType(triggerNode.getChild(1), TeiidDdlLexicon.CreateTrigger.TRIGGER_ROW_ACTION);
        assertProperty(triggerNode.getChild(1),
                       TeiidDdlLexicon.CreateTrigger.ACTION,
                       "insert into g1 (e1, e2) values (2, 'hammer');");
    }

    @Test
    public void shouldParseCreateTriggerUpdate() {
        final String content = "CREATE TRIGGER ON foo INSTEAD OF UPDATE AS FOR EACH ROW UPDATE x SET foo = 'bar';";
        final AstNode triggerNode = this.parser.parse(getTokens(content), this.rootNode);
        assertThat(triggerNode.getName(), is("foo"));
        assertMixinType(triggerNode, TeiidDdlLexicon.CreateTrigger.STATEMENT);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.INSTEAD_OF, DdlConstants.UPDATE);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.ATOMIC, true);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.TABLE_REFERENCE, this.tableRefNode);

        // row actions
        assertThat(triggerNode.getChildCount(), is(1));
        assertThat(triggerNode.getChild(0).getName(), is(TeiidDdlLexicon.CreateTrigger.ROW_ACTION));
        assertMixinType(triggerNode.getChild(0), TeiidDdlLexicon.CreateTrigger.TRIGGER_ROW_ACTION);
        assertProperty(triggerNode.getChild(0), TeiidDdlLexicon.CreateTrigger.ACTION, "UPDATE x SET foo = 'bar';");
    }

    @Test
    public void shouldParseCreateTriggerUpdateWithAtomicBeginBlockOneStatement() {
        final String content = "CREATE TRIGGER ON foo INSTEAD OF UPDATE AS FOR EACH ROW BEGIN ATOMIC UPDATE x SET foo = 'bar'; END";
        final AstNode triggerNode = this.parser.parse(getTokens(content), this.rootNode);
        assertThat(triggerNode.getName(), is("foo"));
        assertMixinType(triggerNode, TeiidDdlLexicon.CreateTrigger.STATEMENT);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.INSTEAD_OF, DdlConstants.UPDATE);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.ATOMIC, true);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.TABLE_REFERENCE, this.tableRefNode);

        // row actions
        assertThat(triggerNode.getChildCount(), is(1));
        assertThat(triggerNode.getChild(0).getName(), is(TeiidDdlLexicon.CreateTrigger.ROW_ACTION));
        assertMixinType(triggerNode.getChild(0), TeiidDdlLexicon.CreateTrigger.TRIGGER_ROW_ACTION);
        assertProperty(triggerNode.getChild(0), TeiidDdlLexicon.CreateTrigger.ACTION, "UPDATE x SET foo = 'bar';");
    }

    @Test
    public void shouldParseCreateTriggerUpdateWithBeginBlockOneStatement() {
        final String content = "CREATE TRIGGER ON foo INSTEAD OF UPDATE AS FOR EACH ROW BEGIN UPDATE x SET foo = 'bar'; END";
        final AstNode triggerNode = this.parser.parse(getTokens(content), this.rootNode);
        assertThat(triggerNode.getName(), is("foo"));
        assertMixinType(triggerNode, TeiidDdlLexicon.CreateTrigger.STATEMENT);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.INSTEAD_OF, DdlConstants.UPDATE);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.ATOMIC, false);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.TABLE_REFERENCE, this.tableRefNode);

        // row actions
        assertThat(triggerNode.getChildCount(), is(1));
        assertThat(triggerNode.getChild(0).getName(), is(TeiidDdlLexicon.CreateTrigger.ROW_ACTION));
        assertMixinType(triggerNode.getChild(0), TeiidDdlLexicon.CreateTrigger.TRIGGER_ROW_ACTION);
        assertProperty(triggerNode.getChild(0), TeiidDdlLexicon.CreateTrigger.ACTION, "UPDATE x SET foo = 'bar';");
    }

    @Test
    public void shouldParseCreateTriggerUpdateWithBeginBlockTwoStatements() {
        final String content = "CREATE TRIGGER ON foo INSTEAD OF UPDATE AS FOR EACH ROW BEGIN UPDATE x SET foo = 'bar'; UPDATE x SET bar = 'foo'; END";
        final AstNode triggerNode = this.parser.parse(getTokens(content), this.rootNode);
        assertThat(triggerNode.getName(), is("foo"));
        assertMixinType(triggerNode, TeiidDdlLexicon.CreateTrigger.STATEMENT);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.INSTEAD_OF, DdlConstants.UPDATE);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.ATOMIC, false);
        assertProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.TABLE_REFERENCE, this.tableRefNode);

        // row actions
        assertThat(triggerNode.getChildCount(), is(2));
        assertThat(triggerNode.getChild(0).getName(), is(TeiidDdlLexicon.CreateTrigger.ROW_ACTION));
        assertMixinType(triggerNode.getChild(0), TeiidDdlLexicon.CreateTrigger.TRIGGER_ROW_ACTION);
        assertProperty(triggerNode.getChild(0), TeiidDdlLexicon.CreateTrigger.ACTION, "UPDATE x SET foo = 'bar';");
        assertThat(triggerNode.getChild(1).getName(), is(TeiidDdlLexicon.CreateTrigger.ROW_ACTION));
        assertMixinType(triggerNode.getChild(1), TeiidDdlLexicon.CreateTrigger.TRIGGER_ROW_ACTION);
        assertProperty(triggerNode.getChild(1), TeiidDdlLexicon.CreateTrigger.ACTION, "UPDATE x SET bar = 'foo';");
    }

}
