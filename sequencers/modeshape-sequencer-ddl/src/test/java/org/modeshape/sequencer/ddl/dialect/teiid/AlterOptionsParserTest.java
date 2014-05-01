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

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.text.ParsingException;
import org.modeshape.sequencer.ddl.StandardDdlLexicon;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.SchemaElementType;
import org.modeshape.sequencer.ddl.node.AstNode;

/**
 * A test class for {@link AlterOptionsParser}.
 */
public class AlterOptionsParserTest extends TeiidDdlTest {

    private AstNode alterOptionsNodeTable;
    private AstNode tableRefNode;

    private AstNode alterOptionsNodeProcedure;
    private AstNode procedureRefNode;

    private AstNode alterOptionsNodeView;
    private AstNode viewRefNode;

    private AlterOptionsParser parser;
    private AstNode rootNode;

    @Before
    public void beforeEach() {
        final TeiidDdlParser teiidDdlParser = new TeiidDdlParser();
        this.parser = new AlterOptionsParser(teiidDdlParser);
        this.rootNode = teiidDdlParser.nodeFactory().node("ddlRootNode");

        { // create table that is referenced by alter options statements (all alter tables must reference foo)
            final String tableContent = "CREATE FOREIGN TABLE foo(bar string, sledge string)";
            final CreateTableParser tableParser = new CreateTableParser(teiidDdlParser);
            this.tableRefNode = tableParser.parse(getTokens(tableContent), this.rootNode);

            this.alterOptionsNodeTable = teiidDdlParser.nodeFactory().node("alterOptionsNodeTable",
                                                                           this.rootNode,
                                                                           TeiidDdlLexicon.AlterOptions.OPTIONS_LIST);
            this.alterOptionsNodeTable.setProperty(TeiidDdlLexicon.AlterOptions.REFERENCE, this.tableRefNode);
        }

        { // create procedure that is referenced by alter options statements (all alter procedures must reference myProc)
            final String procedureContent = "CREATE PROCEDURE myProc(p1 integer) RETURNS integer";
            final CreateProcedureParser procedureParser = new CreateProcedureParser(teiidDdlParser);
            this.procedureRefNode = procedureParser.parse(getTokens(procedureContent), this.rootNode);

            this.alterOptionsNodeProcedure = teiidDdlParser.nodeFactory().node("alterOptionsNodeProcedure",
                                                                               this.rootNode,
                                                                               TeiidDdlLexicon.AlterOptions.OPTIONS_LIST);
            this.alterOptionsNodeProcedure.setProperty(TeiidDdlLexicon.AlterOptions.REFERENCE, this.procedureRefNode);
        }

        { // create view that is referenced by alter options statements (all alter views must reference G1)
            final String viewContent = "CREATE View G1( e1 integer, e2 varchar) OPTIONS (CARDINALITY 12) AS select e1, e2 from foo.bar";
            final CreateTableParser viewParser = new CreateTableParser(teiidDdlParser);
            this.viewRefNode = viewParser.parse(getTokens(viewContent), this.rootNode);

            this.alterOptionsNodeView = teiidDdlParser.nodeFactory().node("alterOptionsNodeView",
                                                                          this.rootNode,
                                                                          TeiidDdlLexicon.AlterOptions.OPTIONS_LIST);
            this.alterOptionsNodeView.setProperty(TeiidDdlLexicon.AlterOptions.REFERENCE, this.viewRefNode);
        }
    }

    @Test
    public void shouldParseAlterForeignProcedureWithColumnOptions() {
        final String content = "ALTER FOREIGN PROCEDURE myProc ALTER PARAMETER p1 OPTIONS (ADD cardinality 100)";
        final AstNode alterOptionsNode = this.parser.parse(getTokens(content), this.rootNode);

        assertThat(alterOptionsNode.getName(), is("myProc"));
        assertMixinType(alterOptionsNode, TeiidDdlLexicon.AlterOptions.PROCEDURE_STATEMENT);
        assertProperty(alterOptionsNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        assertProperty(alterOptionsNode, TeiidDdlLexicon.AlterOptions.REFERENCE, this.procedureRefNode);
        assertThat(alterOptionsNode.childrenWithName("p1").size(), is(1));
    }

    @Test
    public void shouldParseAlterForeignProcedureWithOptionsList() {
        final String content = "ALTER FOREIGN PROCEDURE myProc OPTIONS (ADD cardinality 100)";
        final AstNode alterOptionsNode = this.parser.parse(getTokens(content), this.rootNode);

        assertThat(alterOptionsNode.getName(), is("myProc"));
        assertMixinType(alterOptionsNode, TeiidDdlLexicon.AlterOptions.PROCEDURE_STATEMENT);
        assertProperty(alterOptionsNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        assertProperty(alterOptionsNode, TeiidDdlLexicon.AlterOptions.REFERENCE, this.procedureRefNode);
        assertThat(alterOptionsNode.childrenWithName(TeiidDdlLexicon.AlterOptions.ALTERS).size(), is(1));
    }

    @Test
    public void shouldParseAlterForeignTableWithColumnOptions() {
        final String content = "ALTER FOREIGN TABLE foo ALTER COLUMN bar OPTIONS (ADD cardinality 100)";
        final AstNode alterOptionsNode = this.parser.parse(getTokens(content), this.rootNode);

        assertThat(alterOptionsNode.getName(), is("foo"));
        assertMixinType(alterOptionsNode, TeiidDdlLexicon.AlterOptions.TABLE_STATEMENT);
        assertProperty(alterOptionsNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        assertProperty(alterOptionsNode, TeiidDdlLexicon.AlterOptions.REFERENCE, this.tableRefNode);
        assertThat(alterOptionsNode.childrenWithName("bar").size(), is(1));
    }

    @Test
    public void shouldParseAlterForeignTableWithOptionsList() {
        final String content = "ALTER FOREIGN TABLE foo OPTIONS (ADD cardinality 100)";
        final AstNode alterOptionsNode = this.parser.parse(getTokens(content), this.rootNode);

        assertThat(alterOptionsNode.getName(), is("foo"));
        assertMixinType(alterOptionsNode, TeiidDdlLexicon.AlterOptions.TABLE_STATEMENT);
        assertProperty(alterOptionsNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        assertProperty(alterOptionsNode, TeiidDdlLexicon.AlterOptions.REFERENCE, this.tableRefNode);
        assertThat(alterOptionsNode.childrenWithName(TeiidDdlLexicon.AlterOptions.ALTERS).size(), is(1));
    }

    @Test
    public void shouldParseAlterForeignViewWithColumnOptions() {
        final String content = "ALTER FOREIGN VIEW G1 ALTER COLUMN e1 OPTIONS (ADD cardinality 100)";
        final AstNode alterOptionsNode = this.parser.parse(getTokens(content), this.rootNode);

        assertThat(alterOptionsNode.getName(), is("G1"));
        assertMixinType(alterOptionsNode, TeiidDdlLexicon.AlterOptions.VIEW_STATEMENT);
        assertProperty(alterOptionsNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        assertProperty(alterOptionsNode, TeiidDdlLexicon.AlterOptions.REFERENCE, this.viewRefNode);
        assertThat(alterOptionsNode.childrenWithName("e1").size(), is(1));
    }

    @Test
    public void shouldParseAlterForeignViewWithOptionsList() {
        final String content = "ALTER FOREIGN VIEW G1 OPTIONS (ADD cardinality 100)";
        final AstNode alterOptionsNode = this.parser.parse(getTokens(content), this.rootNode);

        assertThat(alterOptionsNode.getName(), is("G1"));
        assertMixinType(alterOptionsNode, TeiidDdlLexicon.AlterOptions.VIEW_STATEMENT);
        assertProperty(alterOptionsNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        assertProperty(alterOptionsNode, TeiidDdlLexicon.AlterOptions.REFERENCE, this.viewRefNode);
        assertThat(alterOptionsNode.childrenWithName(TeiidDdlLexicon.AlterOptions.ALTERS).size(), is(1));
    }

    @Test
    public void shouldParseAlterImplicitProcedureWithColumnOptions() {
        final String content = "ALTER PROCEDURE myProc ALTER PARAMETER p1 OPTIONS (ADD cardinality 100)";
        final AstNode alterOptionsNode = this.parser.parse(getTokens(content), this.rootNode);

        assertThat(alterOptionsNode.getName(), is("myProc"));
        assertMixinType(alterOptionsNode, TeiidDdlLexicon.AlterOptions.PROCEDURE_STATEMENT);
        assertProperty(alterOptionsNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        assertProperty(alterOptionsNode, TeiidDdlLexicon.AlterOptions.REFERENCE, this.procedureRefNode);
        assertThat(alterOptionsNode.childrenWithName("p1").size(), is(1));
    }

    @Test
    public void shouldParseAlterImplicitProcedureWithOptionsList() {
        final String content = "ALTER PROCEDURE myProc OPTIONS (ADD cardinality 100)";
        final AstNode alterOptionsNode = this.parser.parse(getTokens(content), this.rootNode);

        assertThat(alterOptionsNode.getName(), is("myProc"));
        assertMixinType(alterOptionsNode, TeiidDdlLexicon.AlterOptions.PROCEDURE_STATEMENT);
        assertProperty(alterOptionsNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        assertProperty(alterOptionsNode, TeiidDdlLexicon.AlterOptions.REFERENCE, this.procedureRefNode);
        assertThat(alterOptionsNode.childrenWithName(TeiidDdlLexicon.AlterOptions.ALTERS).size(), is(1));
    }

    @Test
    public void shouldParseAlterImplicitTableWithColumnOptions() {
        final String content = "ALTER TABLE foo ALTER COLUMN bar OPTIONS (ADD cardinality 100)";
        final AstNode alterOptionsNode = this.parser.parse(getTokens(content), this.rootNode);

        assertThat(alterOptionsNode.getName(), is("foo"));
        assertMixinType(alterOptionsNode, TeiidDdlLexicon.AlterOptions.TABLE_STATEMENT);
        assertProperty(alterOptionsNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        assertProperty(alterOptionsNode, TeiidDdlLexicon.AlterOptions.REFERENCE, this.tableRefNode);
        assertThat(alterOptionsNode.childrenWithName("bar").size(), is(1));
    }

    @Test
    public void shouldParseAlterImplicitTableWithOptionsList() {
        final String content = "ALTER TABLE foo OPTIONS (ADD cardinality 100)";
        final AstNode alterOptionsNode = this.parser.parse(getTokens(content), this.rootNode);

        assertThat(alterOptionsNode.getName(), is("foo"));
        assertMixinType(alterOptionsNode, TeiidDdlLexicon.AlterOptions.TABLE_STATEMENT);
        assertProperty(alterOptionsNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        assertProperty(alterOptionsNode, TeiidDdlLexicon.AlterOptions.REFERENCE, this.tableRefNode);
        assertThat(alterOptionsNode.childrenWithName(TeiidDdlLexicon.AlterOptions.ALTERS).size(), is(1));
    }

    @Test
    public void shouldParseAlterImplicitViewWithColumnOptions() {
        final String content = "ALTER VIEW G1 ALTER COLUMN e1 OPTIONS (ADD cardinality 100)";
        final AstNode alterOptionsNode = this.parser.parse(getTokens(content), this.rootNode);

        assertThat(alterOptionsNode.getName(), is("G1"));
        assertMixinType(alterOptionsNode, TeiidDdlLexicon.AlterOptions.VIEW_STATEMENT);
        assertProperty(alterOptionsNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        assertProperty(alterOptionsNode, TeiidDdlLexicon.AlterOptions.REFERENCE, this.viewRefNode);
        assertThat(alterOptionsNode.childrenWithName("e1").size(), is(1));
    }

    @Test
    public void shouldParseAlterImplicitViewWithOptionsList() {
        final String content = "ALTER VIEW G1 OPTIONS (ADD cardinality 100)";
        final AstNode alterOptionsNode = this.parser.parse(getTokens(content), this.rootNode);

        assertThat(alterOptionsNode.getName(), is("G1"));
        assertMixinType(alterOptionsNode, TeiidDdlLexicon.AlterOptions.VIEW_STATEMENT);
        assertProperty(alterOptionsNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        assertProperty(alterOptionsNode, TeiidDdlLexicon.AlterOptions.REFERENCE, this.viewRefNode);
        assertThat(alterOptionsNode.childrenWithName(TeiidDdlLexicon.AlterOptions.ALTERS).size(), is(1));
    }

    @Test
    public void shouldParseAlterVirtualProcedureWithColumnOptions() {
        final String content = "ALTER VIRTUAL PROCEDURE myProc ALTER PARAMETER p1 OPTIONS (ADD cardinality 100)";
        final AstNode alterOptionsNode = this.parser.parse(getTokens(content), this.rootNode);

        assertThat(alterOptionsNode.getName(), is("myProc"));
        assertMixinType(alterOptionsNode, TeiidDdlLexicon.AlterOptions.PROCEDURE_STATEMENT);
        assertProperty(alterOptionsNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.VIRTUAL.toDdl());
        assertProperty(alterOptionsNode, TeiidDdlLexicon.AlterOptions.REFERENCE, this.procedureRefNode);
        assertThat(alterOptionsNode.childrenWithName("p1").size(), is(1));
    }

    @Test
    public void shouldParseAlterVirtualProcedureWithOptionsList() {
        final String content = "ALTER VIRTUAL PROCEDURE myProc OPTIONS (ADD cardinality 100)";
        final AstNode alterOptionsNode = this.parser.parse(getTokens(content), this.rootNode);

        assertThat(alterOptionsNode.getName(), is("myProc"));
        assertMixinType(alterOptionsNode, TeiidDdlLexicon.AlterOptions.PROCEDURE_STATEMENT);
        assertProperty(alterOptionsNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.VIRTUAL.toDdl());
        assertProperty(alterOptionsNode, TeiidDdlLexicon.AlterOptions.REFERENCE, this.procedureRefNode);
        assertThat(alterOptionsNode.childrenWithName(TeiidDdlLexicon.AlterOptions.ALTERS).size(), is(1));
    }

    @Test
    public void shouldParseAlterVirtualTableWithColumnOptions() {
        final String content = "ALTER VIRTUAL TABLE foo ALTER COLUMN bar OPTIONS (ADD cardinality 100)";
        final AstNode alterOptionsNode = this.parser.parse(getTokens(content), this.rootNode);

        assertThat(alterOptionsNode.getName(), is("foo"));
        assertMixinType(alterOptionsNode, TeiidDdlLexicon.AlterOptions.TABLE_STATEMENT);
        assertProperty(alterOptionsNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.VIRTUAL.toDdl());
        assertProperty(alterOptionsNode, TeiidDdlLexicon.AlterOptions.REFERENCE, this.tableRefNode);
        assertThat(alterOptionsNode.childrenWithName("bar").size(), is(1));
    }

    @Test
    public void shouldParseAlterVirtualTableWithOptionsList() {
        final String content = "ALTER VIRTUAL TABLE foo OPTIONS (ADD cardinality 100)";
        final AstNode alterOptionsNode = this.parser.parse(getTokens(content), this.rootNode);

        assertThat(alterOptionsNode.getName(), is("foo"));
        assertMixinType(alterOptionsNode, TeiidDdlLexicon.AlterOptions.TABLE_STATEMENT);
        assertProperty(alterOptionsNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.VIRTUAL.toDdl());
        assertProperty(alterOptionsNode, TeiidDdlLexicon.AlterOptions.REFERENCE, this.tableRefNode);
        assertThat(alterOptionsNode.childrenWithName(TeiidDdlLexicon.AlterOptions.ALTERS).size(), is(1));
    }

    @Test
    public void shouldParseAlterVirtualViewWithColumnOptions() {
        final String content = "ALTER VIRTUAL VIEW G1 ALTER COLUMN e1 OPTIONS (ADD cardinality 100)";
        final AstNode alterOptionsNode = this.parser.parse(getTokens(content), this.rootNode);

        assertThat(alterOptionsNode.getName(), is("G1"));
        assertMixinType(alterOptionsNode, TeiidDdlLexicon.AlterOptions.VIEW_STATEMENT);
        assertProperty(alterOptionsNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.VIRTUAL.toDdl());
        assertProperty(alterOptionsNode, TeiidDdlLexicon.AlterOptions.REFERENCE, this.viewRefNode);
        assertThat(alterOptionsNode.childrenWithName("e1").size(), is(1));
    }

    @Test
    public void shouldParseAlterVirtualViewWithOptionsList() {
        final String content = "ALTER VIRTUAL VIEW G1 OPTIONS (ADD cardinality 100)";
        final AstNode alterOptionsNode = this.parser.parse(getTokens(content), this.rootNode);

        assertThat(alterOptionsNode.getName(), is("G1"));
        assertMixinType(alterOptionsNode, TeiidDdlLexicon.AlterOptions.VIEW_STATEMENT);
        assertProperty(alterOptionsNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.VIRTUAL.toDdl());
        assertProperty(alterOptionsNode, TeiidDdlLexicon.AlterOptions.REFERENCE, this.viewRefNode);
        assertThat(alterOptionsNode.childrenWithName(TeiidDdlLexicon.AlterOptions.ALTERS).size(), is(1));
    }

    // ********* parse alter options list clause tests ***********

    @Test
    public void shouldParseAlterAddOption() {
        final String content = "OPTIONS (ADD updatable 'true')";

        assertThat(this.parser.parseAlterOptionsList(getTokens(content), this.rootNode), is(true));
        final List<AstNode> nodes = this.rootNode.childrenWithName(TeiidDdlLexicon.AlterOptions.ALTERS);
        assertThat(nodes.size(), is(1));
        final AstNode optionsListNode = nodes.get(0);
        assertMixinType(optionsListNode, TeiidDdlLexicon.AlterOptions.OPTIONS_LIST);

        final List<AstNode> options = optionsListNode.childrenWithName("updatable");
        assertThat(options.size(), is(1));
        assertProperty(options.get(0), StandardDdlLexicon.VALUE, "true");
    }

    @Test
    public void shouldParseAlterSetOption() {
        final String content = "OPTIONS (SET updatable 'true')";

        assertThat(this.parser.parseAlterOptionsList(getTokens(content), this.rootNode), is(true));
        final List<AstNode> nodes = this.rootNode.childrenWithName(TeiidDdlLexicon.AlterOptions.ALTERS);
        assertThat(nodes.size(), is(1));
        final AstNode optionsListNode = nodes.get(0);
        assertMixinType(optionsListNode, TeiidDdlLexicon.AlterOptions.OPTIONS_LIST);

        final List<AstNode> options = optionsListNode.childrenWithName("updatable");
        assertThat(options.size(), is(1));
        assertProperty(options.get(0), StandardDdlLexicon.VALUE, "true");
    }

    @Test
    public void shouldParseAlterDropOption() {
        final String content = "OPTIONS (DROP updatable)";

        assertThat(this.parser.parseAlterOptionsList(getTokens(content), this.rootNode), is(true));
        final List<AstNode> nodes = this.rootNode.childrenWithName(TeiidDdlLexicon.AlterOptions.ALTERS);
        assertThat(nodes.size(), is(1));
        final AstNode optionsListNode = nodes.get(0);
        assertMixinType(optionsListNode, TeiidDdlLexicon.AlterOptions.OPTIONS_LIST);

        final Object value = optionsListNode.getProperty(TeiidDdlLexicon.AlterOptions.DROPPED);
        assertThat(value, is(instanceOf(List.class)));

        @SuppressWarnings( "unchecked" )
        final List<String> dropped = (List<String>)value;
        assertThat(dropped.size(), is(1));
        assertThat(dropped.get(0), is("updatable"));
    }

    @Test
    public void shouldParseAlterOptionList() {
        final String content = "OPTIONS (ADD sledge 'foo', SET hammer 'bar', DROP elvis, DROP ringo)";

        assertThat(this.parser.parseAlterOptionsList(getTokens(content), this.rootNode), is(true));
        final List<AstNode> nodes = this.rootNode.childrenWithName(TeiidDdlLexicon.AlterOptions.ALTERS);
        assertThat(nodes.size(), is(1));
        final AstNode optionsListNode = nodes.get(0);
        assertMixinType(optionsListNode, TeiidDdlLexicon.AlterOptions.OPTIONS_LIST);

        { // first option
            final List<AstNode> options = optionsListNode.childrenWithName("sledge");
            assertThat(options.size(), is(1));
            assertProperty(options.get(0), StandardDdlLexicon.VALUE, "foo");
        }

        { // second option
            final List<AstNode> options = optionsListNode.childrenWithName("hammer");
            assertThat(options.size(), is(1));
            assertProperty(options.get(0), StandardDdlLexicon.VALUE, "bar");
        }

        // dropped
        final Object value = optionsListNode.getProperty(TeiidDdlLexicon.AlterOptions.DROPPED);
        assertThat(value, is(instanceOf(List.class)));

        @SuppressWarnings( "unchecked" )
        final List<String> dropped = (List<String>)value;
        assertThat(dropped.size(), is(2));
        assertThat(dropped, hasItems("elvis", "ringo"));
    }

    @Test( expected = ParsingException.class )
    public void shouldNotParseEmptyOptionsList() {
        final String content = "OPTIONS ()";
        this.parser.parseAlterOptionsList(getTokens(content), this.rootNode);
    }

    @Test( expected = ParsingException.class )
    public void shouldNotParseOptionsListWithBeginningComma() {
        final String content = "OPTIONS (, SET updatable 'true')";
        this.parser.parseAlterOptionsList(getTokens(content), this.rootNode);
    }

    @Test( expected = ParsingException.class )
    public void shouldNotParseOptionsListWithEndingComma() {
        final String content = "OPTIONS (SET updatable 'true',)";
        this.parser.parseAlterOptionsList(getTokens(content), this.rootNode);
    }

    @Test( expected = ParsingException.class )
    public void shouldNotParseOptionsListWithExtraComma() {
        final String content = "OPTIONS (SET updatable 'true',, ADD sledge 'foo')";
        this.parser.parseAlterOptionsList(getTokens(content), this.rootNode);
    }

    @Test
    public void shouldNotParseOptionsListWithColumnOptions() {
        final String content = "ALTER COLUMN mycolumn OPTIONS (ADD updatable true)";
        assertThat(this.parser.parseAlterOptionsList(getTokens(content), this.rootNode), is(false));
    }

    // ********* parse alter column options clause tests ***********

    @Test
    public void shouldParseAlterColumnOption() {
        final String content = "ALTER COLUMN bar OPTIONS( ADD x 'y')"; // must use column name from referenced table

        assertThat(this.parser.parseAlterColumnOptions(getTokens(content), this.alterOptionsNodeTable), is(true));
        assertThat(this.alterOptionsNodeTable.getChildCount(), is(1));

        final AstNode alterColumnNode = this.alterOptionsNodeTable.getFirstChild();
        assertThat(alterColumnNode.getName(), is("bar"));
        assertMixinType(alterColumnNode, TeiidDdlLexicon.AlterOptions.COLUMN);
        assertThat(alterColumnNode.getProperty(TeiidDdlLexicon.AlterOptions.REFERENCE), is(notNullValue()));

        // option
        final List<AstNode> options = alterColumnNode.childrenWithName("x");
        assertThat(options.size(), is(1));
        assertProperty(options.get(0), StandardDdlLexicon.VALUE, "y");
    }

    @Test
    public void shouldParseImplicitAlterColumnOption() {
        final String content = "ALTER bar OPTIONS( ADD x 'y')"; // must use column name from referenced table

        assertThat(this.parser.parseAlterColumnOptions(getTokens(content), this.alterOptionsNodeTable), is(true));
        assertThat(this.alterOptionsNodeTable.getChildCount(), is(1));

        final AstNode alterColumnNode = this.alterOptionsNodeTable.getFirstChild();
        assertThat(alterColumnNode.getName(), is("bar"));
        assertMixinType(alterColumnNode, TeiidDdlLexicon.AlterOptions.COLUMN);
        assertThat(alterColumnNode.getProperty(TeiidDdlLexicon.AlterOptions.REFERENCE), is(notNullValue()));

        // option
        final List<AstNode> options = alterColumnNode.childrenWithName("x");
        assertThat(options.size(), is(1));
        assertProperty(options.get(0), StandardDdlLexicon.VALUE, "y");
    }

    @Test
    public void shouldParseAlterParameterOption() {
        final String content = "ALTER PARAMETER p1 OPTIONS (ADD x 'y')";

        assertThat(this.parser.parseAlterColumnOptions(getTokens(content), this.alterOptionsNodeProcedure), is(true));
        assertThat(this.alterOptionsNodeProcedure.getChildCount(), is(1));

        final AstNode alterParamNode = this.alterOptionsNodeProcedure.getFirstChild();
        assertThat(alterParamNode.getName(), is("p1"));
        assertMixinType(alterParamNode, TeiidDdlLexicon.AlterOptions.PARAMETER);
        assertThat(alterParamNode.getProperty(TeiidDdlLexicon.AlterOptions.REFERENCE), is(notNullValue()));

        // option
        final List<AstNode> options = alterParamNode.childrenWithName("x");
        assertThat(options.size(), is(1));
        assertProperty(options.get(0), StandardDdlLexicon.VALUE, "y");
    }

}
