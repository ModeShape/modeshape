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
import static org.junit.Assert.fail;
import java.util.AbstractMap;
import java.util.Map.Entry;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.sequencer.ddl.StandardDdlLexicon;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.SchemaElementType;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.TeiidDataType;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.TeiidNonReservedWord;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.TeiidReservedWord;
import org.modeshape.sequencer.ddl.node.AstNode;

/**
 * A test class for {@link CreateProcedureParser}.
 */
public class CreateProcedureParserTest extends TeiidDdlTest {

    private CreateProcedureParser parser;
    private AstNode rootNode;

    @Before
    public void beforeEach() {
        final TeiidDdlParser teiidDdlParser = new TeiidDdlParser();
        this.parser = new CreateProcedureParser(teiidDdlParser);
        this.rootNode = teiidDdlParser.nodeFactory().node("ddlRootNode");
    }

    @Test
    public void shouldParseCreateProcedure() {
        final String content = "CREATE PROCEDURE FOO(P1 integer) RETURNS (e1 integer, e2 varchar) AS SELECT * FROM PM1.G1;";
        final AstNode procedureNode = this.parser.parse(getTokens(content), this.rootNode);
        assertMixinType(procedureNode, TeiidDdlLexicon.CreateProcedure.PROCEDURE_STATEMENT);
        assertProperty(procedureNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        assertThat(procedureNode.getName(), is("FOO"));

        // 1 param, 1 result set
        assertThat(procedureNode.getChildCount(), is(2));

        { // first param
            final AstNode param1 = procedureNode.childrenWithName("P1").get(0);
            assertProperty(param1, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.INTEGER.toDdl());
            assertProperty(param1, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
        }

        { // result set
            final AstNode resultSetNode = procedureNode.childrenWithName(TeiidDdlLexicon.CreateProcedure.RESULT_SET).get(0);
            assertMixinType(resultSetNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMNS);
            assertProperty(resultSetNode, TeiidDdlLexicon.CreateProcedure.TABLE_FLAG, false);
            assertThat(resultSetNode.getChildCount(), is(2));

            AstNode resultCol1 = null;
            AstNode resultCol2 = null;

            if ("e1".equals(resultSetNode.getChild(0).getName())) {
                resultCol1 = resultSetNode.getChild(0);
                resultCol2 = resultSetNode.getChild(1);
            } else {
                resultCol2 = resultSetNode.getChild(0);
                resultCol1 = resultSetNode.getChild(1);
            }

            assertThat(resultCol1.getName(), is("e1"));
            assertProperty(resultCol1, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.INTEGER.toDdl());

            assertThat(resultCol2.getName(), is("e2"));
            assertProperty(resultCol2, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.VARCHAR.toDdl());
        }

        { // statement (comments are thrown out by tokenizer)
            assertProperty(procedureNode, TeiidDdlLexicon.CreateProcedure.STATEMENT, "SELECT * FROM PM1.G1;");
        }
    }

    /**
     * See Teiid TestDDLParser#testMixedCaseTypes()
     */
    @Test
    public void shouldParseMixedCaseTypes() {
        final String content = "CREATE FUNCTION SourceFunc(flag Boolean) RETURNS varchaR options (UUID 'z')";
        final AstNode functionNode = this.parser.parse(getTokens(content), this.rootNode);
        assertMixinType(functionNode, TeiidDdlLexicon.CreateProcedure.FUNCTION_STATEMENT);
        assertProperty(functionNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        assertThat(functionNode.getName(), is("SourceFunc"));

        // 1 param, 1 result set, 1 option
        assertThat(functionNode.getChildCount(), is(3));

        { // first param
            final AstNode param1 = functionNode.childrenWithName("flag").get(0);
            assertProperty(param1, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.BOOLEAN.toDdl());
            assertProperty(param1, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
        }

        { // result set
            final AstNode resultSetNode = functionNode.childrenWithName(TeiidDdlLexicon.CreateProcedure.RESULT_SET).get(0);
            assertMixinType(resultSetNode, TeiidDdlLexicon.CreateProcedure.RESULT_DATA_TYPE);
            assertThat(resultSetNode.getChildCount(), is(0));
            assertProperty(resultSetNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.VARCHAR.toDdl());
        }

        { // options
            assertProperty(functionNode.childrenWithName("UUID").get(0), StandardDdlLexicon.VALUE, "z");
            assertMixinType(functionNode.childrenWithName("UUID").get(0), StandardDdlLexicon.TYPE_STATEMENT_OPTION);
        }
    }

    /**
     * See Teiid TestDDLParser#testPushdownFunctionNoArgs()
     */
    @Test
    public void shouldParsePushdownFunctionNoArgs() {
        final String content = "CREATE FOREIGN FUNCTION SourceFunc() RETURNS integer OPTIONS (UUID 'hello world')";
        final AstNode functionNode = this.parser.parse(getTokens(content), this.rootNode);
        assertMixinType(functionNode, TeiidDdlLexicon.CreateProcedure.FUNCTION_STATEMENT);
        assertProperty(functionNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        assertThat(functionNode.getName(), is("SourceFunc"));

        // verify returns clause and options clause
        assertThat(functionNode.getChildCount(), is(2));

        AstNode resultSetNode = null;
        AstNode optionNode = null;

        if (functionNode.getChild(0).hasMixin(TeiidDdlLexicon.CreateProcedure.RESULT_DATA_TYPE)) {
            resultSetNode = functionNode.getChild(0);
            optionNode = functionNode.getChild(1);
        } else if (hasMixin(functionNode.getChild(0), StandardDdlLexicon.OPTION)) {
            optionNode = functionNode.getChild(0);
            resultSetNode = functionNode.getChild(1);
        } else {
            fail("Unexpected function child");
        }

        assert ((resultSetNode != null) && (optionNode != null));

        // verify result set
        assertProperty(resultSetNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.INTEGER.toDdl());

        // verify option
        assertThat(optionNode.getName(), is("UUID"));
        assertProperty(optionNode, StandardDdlLexicon.VALUE, "hello world");
    }

    /**
     * See Teiid TestDDLParser#testSourceProcedure()
     */
    @Test
    public void shouldParseSourceProcedure() {
        final String content = "CREATE FOREIGN PROCEDURE myProc(OUT p1 boolean, p2 varchar, INOUT p3 decimal) "
                               + "RETURNS (r1 varchar, r2 decimal) "
                               + "OPTIONS(RANDOM 'any', UUID 'uuid', NAMEINSOURCE 'nis', ANNOTATION 'desc', UPDATECOUNT '2');";
        final AstNode procedureNode = this.parser.parse(getTokens(content), this.rootNode);
        assertMixinType(procedureNode, TeiidDdlLexicon.CreateProcedure.PROCEDURE_STATEMENT);
        assertProperty(procedureNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        assertThat(procedureNode.getName(), is("myProc"));

        // 3 params, 1 result set, 5 options
        assertThat(procedureNode.getChildCount(), is(9));

        { // first param
            final AstNode param1 = procedureNode.childrenWithName("p1").get(0);
            assertProperty(param1, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.BOOLEAN.toDdl());
            assertProperty(param1, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.OUT.toDdl());
        }

        { // second param
            final AstNode param1 = procedureNode.childrenWithName("p2").get(0);
            assertProperty(param1, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.VARCHAR.toDdl());
            assertProperty(param1, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
        }

        { // third param
            final AstNode param1 = procedureNode.childrenWithName("p3").get(0);
            assertProperty(param1, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.DECIMAL.toDdl());
            assertProperty(param1, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.INOUT.toDdl());
        }

        { // result set
            final AstNode resultSetNode = procedureNode.childrenWithName(TeiidDdlLexicon.CreateProcedure.RESULT_SET).get(0);
            assertMixinType(resultSetNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMNS);
            assertProperty(resultSetNode, TeiidDdlLexicon.CreateProcedure.TABLE_FLAG, false);
            assertThat(resultSetNode.getChildCount(), is(2));

            AstNode resultCol1 = null;
            AstNode resultCol2 = null;

            if ("r1".equals(resultSetNode.getChild(0).getName())) {
                resultCol1 = resultSetNode.getChild(0);
                resultCol2 = resultSetNode.getChild(1);
            } else {
                resultCol2 = resultSetNode.getChild(0);
                resultCol1 = resultSetNode.getChild(1);
            }

            assertThat(resultCol1.getName(), is("r1"));
            assertProperty(resultCol1, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.VARCHAR.toDdl());

            assertThat(resultCol2.getName(), is("r2"));
            assertProperty(resultCol2, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.DECIMAL.toDdl());
        }

        { // options
            assertProperty(procedureNode.childrenWithName("RANDOM").get(0), StandardDdlLexicon.VALUE, "any");
            assertProperty(procedureNode.childrenWithName("UUID").get(0), StandardDdlLexicon.VALUE, "uuid");
            assertProperty(procedureNode.childrenWithName("NAMEINSOURCE").get(0), StandardDdlLexicon.VALUE, "nis");
            assertProperty(procedureNode.childrenWithName("ANNOTATION").get(0), StandardDdlLexicon.VALUE, "desc");
            assertProperty(procedureNode.childrenWithName("UPDATECOUNT").get(0), StandardDdlLexicon.VALUE, "2");
        }
    }

    /**
     * See Teiid TestDDLParser#testUDAggregate()
     */
    @Test
    public void shouldParseUdAggregate() {
        final String content = "CREATE VIRTUAL FUNCTION SourceFunc(flag boolean, msg varchar) RETURNS varchar "
                               + "OPTIONS(CATEGORY 'misc', AGGREGATE 'true', \"allows-distinct\" 'true', UUID 'y')";
        final AstNode functionNode = this.parser.parse(getTokens(content), this.rootNode);
        assertMixinType(functionNode, TeiidDdlLexicon.CreateProcedure.FUNCTION_STATEMENT);
        assertProperty(functionNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.VIRTUAL.toDdl());
        assertThat(functionNode.getName(), is("SourceFunc"));

        // 2 params, 1 result set, 4 options
        assertThat(functionNode.getChildCount(), is(7));

        { // first param
            final AstNode param1 = functionNode.childrenWithName("flag").get(0);
            assertProperty(param1, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.BOOLEAN.toDdl());
            assertProperty(param1, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
        }

        { // second param
            final AstNode param1 = functionNode.childrenWithName("msg").get(0);
            assertProperty(param1, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.VARCHAR.toDdl());
            assertProperty(param1, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
        }

        { // result set
            final AstNode resultSetNode = functionNode.childrenWithName(TeiidDdlLexicon.CreateProcedure.RESULT_SET).get(0);
            assertMixinType(resultSetNode, TeiidDdlLexicon.CreateProcedure.RESULT_DATA_TYPE);
            assertThat(resultSetNode.getChildCount(), is(0));
            assertProperty(resultSetNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.VARCHAR.toDdl());
        }

        { // options
            assertProperty(functionNode.childrenWithName("CATEGORY").get(0), StandardDdlLexicon.VALUE, "misc");
            assertProperty(functionNode.childrenWithName("AGGREGATE").get(0), StandardDdlLexicon.VALUE, "true");
            assertProperty(functionNode.childrenWithName("allows-distinct").get(0), StandardDdlLexicon.VALUE, "true");
            assertProperty(functionNode.childrenWithName("UUID").get(0), StandardDdlLexicon.VALUE, "y");
        }
    }

    /**
     * See Teiid TestDDLParser#testUDF()
     */
    @Test
    public void shouldParseUdf() {
        final String functionName = "SourceFunc";
        final String flagParam = "flag";
        final String msgParam = "msg";
        final Entry<String, String> option1 = new AbstractMap.SimpleEntry<String, String>("CATEGORY", "misc");
        final Entry<String, String> option2 = new AbstractMap.SimpleEntry<String, String>("DETERMINISM", "DETERMINISTIC");
        final Entry<String, String> option3 = new AbstractMap.SimpleEntry<String, String>("NULL-ON-NULL", "true");
        final Entry<String, String> option4 = new AbstractMap.SimpleEntry<String, String>("JAVA_CLASS", "foo");
        final Entry<String, String> option5 = new AbstractMap.SimpleEntry<String, String>("JAVA_METHOD", "bar");
        final Entry<String, String> option6 = new AbstractMap.SimpleEntry<String, String>("RANDOM", "any");
        final Entry<String, String> option7 = new AbstractMap.SimpleEntry<String, String>("UUID", "x");
        final String content = "CREATE VIRTUAL FUNCTION " + functionName + '(' + flagParam + " boolean, " + msgParam
                               + " varchar) RETURNS varchar " + "OPTIONS(" + option1.getKey() + " '" + option1.getValue() + "', "
                               + option2.getKey() + " '" + option2.getValue() + "', \"" + option3.getKey() + "\" '"
                               + option3.getValue() + "', " + option4.getKey() + " '" + option4.getValue() + "', "
                               + option5.getKey() + " '" + option5.getValue() + "', " + option6.getKey() + " '"
                               + option6.getValue() + "', " + option7.getKey() + " '" + option7.getValue() + "')";
        final AstNode functionNode = this.parser.parse(getTokens(content), this.rootNode);
        assertMixinType(functionNode, TeiidDdlLexicon.CreateProcedure.FUNCTION_STATEMENT);
        assertProperty(functionNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.VIRTUAL.toDdl());
        assertThat(functionNode.getName(), is("SourceFunc"));

        // 2 params, 1 result set, 7 options
        assertThat(functionNode.getChildCount(), is(10));

        { // first param
            assertThat(functionNode.childrenWithName(flagParam).size(), is(1));
            assertProperty(functionNode.childrenWithName(flagParam).get(0),
                           StandardDdlLexicon.DATATYPE_NAME,
                           TeiidDataType.BOOLEAN.toDdl());
        }

        { // second param
            assertThat(functionNode.childrenWithName(msgParam).size(), is(1));
            assertProperty(functionNode.childrenWithName(msgParam).get(0),
                           StandardDdlLexicon.DATATYPE_NAME,
                           TeiidDataType.VARCHAR.toDdl());
        }

        { // options
            assertProperty(functionNode.childrenWithName(option1.getKey()).get(0), StandardDdlLexicon.VALUE, option1.getValue());
            assertProperty(functionNode.childrenWithName(option2.getKey()).get(0), StandardDdlLexicon.VALUE, option2.getValue());
            assertProperty(functionNode.childrenWithName(option3.getKey()).get(0), StandardDdlLexicon.VALUE, option3.getValue());
            assertProperty(functionNode.childrenWithName(option4.getKey()).get(0), StandardDdlLexicon.VALUE, option4.getValue());
            assertProperty(functionNode.childrenWithName(option5.getKey()).get(0), StandardDdlLexicon.VALUE, option5.getValue());
            assertProperty(functionNode.childrenWithName(option6.getKey()).get(0), StandardDdlLexicon.VALUE, option6.getValue());
            assertProperty(functionNode.childrenWithName(option7.getKey()).get(0), StandardDdlLexicon.VALUE, option7.getValue());
        }
    }

    /**
     * See Teiid TestDDLParser#testVarArgs()
     */
    @Test
    public void shouldParseVarargs() {
        final String content = "CREATE FUNCTION SourceFunc(flag boolean) RETURNS varchar options (varargs 'true', UUID 'z')";
        final AstNode functionNode = this.parser.parse(getTokens(content), this.rootNode);
        assertMixinType(functionNode, TeiidDdlLexicon.CreateProcedure.FUNCTION_STATEMENT);
        assertProperty(functionNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        assertThat(functionNode.getName(), is("SourceFunc"));

        // 1 param, 1 result set, 2 options
        assertThat(functionNode.getChildCount(), is(4));

        { // first param
            final AstNode param1 = functionNode.childrenWithName("flag").get(0);
            assertProperty(param1, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.BOOLEAN.toDdl());
            assertProperty(param1, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
        }

        { // result set
            final AstNode resultSetNode = functionNode.childrenWithName(TeiidDdlLexicon.CreateProcedure.RESULT_SET).get(0);
            assertMixinType(resultSetNode, TeiidDdlLexicon.CreateProcedure.RESULT_DATA_TYPE);
            assertThat(resultSetNode.getChildCount(), is(0));
            assertProperty(resultSetNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.VARCHAR.toDdl());
        }

        { // options
            assertProperty(functionNode.childrenWithName("varargs").get(0), StandardDdlLexicon.VALUE, "true");
            assertProperty(functionNode.childrenWithName("UUID").get(0), StandardDdlLexicon.VALUE, "z");
        }
    }

    /**
     * See Teiid TestDDLParser#testVirtualProcedure()
     */
    @Test
    public void shouldParseVirtualProcedure() {
        final String content = "CREATE VIRTUAL PROCEDURE myProc(OUT p1 boolean, p2 varchar, INOUT p3 decimal) "
                               + "RETURNS (r1 varchar, r2 decimal) "
                               + "OPTIONS(RANDOM 'any', UUID 'uuid', NAMEINSOURCE 'nis', ANNOTATION 'desc', UPDATECOUNT '2') "
                               + "AS /*+ cache */ BEGIN select * from foo; END";
        final AstNode procedureNode = this.parser.parse(getTokens(content), this.rootNode);
        assertMixinType(procedureNode, TeiidDdlLexicon.CreateProcedure.PROCEDURE_STATEMENT);
        assertProperty(procedureNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.VIRTUAL.toDdl());
        assertThat(procedureNode.getName(), is("myProc"));

        // 3 params, 1 result set, 5 options
        assertThat(procedureNode.getChildCount(), is(9));

        { // first param
            final AstNode param1 = procedureNode.childrenWithName("p1").get(0);
            assertProperty(param1, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.BOOLEAN.toDdl());
            assertProperty(param1, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.OUT.toDdl());
        }

        { // second param
            final AstNode param2 = procedureNode.childrenWithName("p2").get(0);
            assertProperty(param2, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.VARCHAR.toDdl());
            assertProperty(param2, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
        }

        { // third param
            final AstNode param3 = procedureNode.childrenWithName("p3").get(0);
            assertProperty(param3, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.DECIMAL.toDdl());
            assertProperty(param3, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.INOUT.toDdl());
        }

        { // result set
            final AstNode resultSetNode = procedureNode.childrenWithName(TeiidDdlLexicon.CreateProcedure.RESULT_SET).get(0);
            assertMixinType(resultSetNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMNS);
            assertProperty(resultSetNode, TeiidDdlLexicon.CreateProcedure.TABLE_FLAG, false);
            assertThat(resultSetNode.getChildCount(), is(2));

            AstNode resultCol1 = null;
            AstNode resultCol2 = null;

            if ("r1".equals(resultSetNode.getChild(0).getName())) {
                resultCol1 = resultSetNode.getChild(0);
                resultCol2 = resultSetNode.getChild(1);
            } else {
                resultCol2 = resultSetNode.getChild(0);
                resultCol1 = resultSetNode.getChild(1);
            }

            assertThat(resultCol1.getName(), is("r1"));
            assertProperty(resultCol1, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.VARCHAR.toDdl());

            assertThat(resultCol2.getName(), is("r2"));
            assertProperty(resultCol2, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.DECIMAL.toDdl());
        }

        { // options
            assertProperty(procedureNode.childrenWithName("RANDOM").get(0), StandardDdlLexicon.VALUE, "any");
            assertProperty(procedureNode.childrenWithName("UUID").get(0), StandardDdlLexicon.VALUE, "uuid");
            assertProperty(procedureNode.childrenWithName("NAMEINSOURCE").get(0), StandardDdlLexicon.VALUE, "nis");
            assertProperty(procedureNode.childrenWithName("ANNOTATION").get(0), StandardDdlLexicon.VALUE, "desc");
            assertProperty(procedureNode.childrenWithName("UPDATECOUNT").get(0), StandardDdlLexicon.VALUE, "2");
        }

        { // statement (comments are thrown out by tokenizer)
            assertProperty(procedureNode, TeiidDdlLexicon.CreateProcedure.STATEMENT, "BEGIN select * from foo; END");
        }
    }

    @Test
    public void shouldParseFlatFileDdl1() {
        final String content = "CREATE FOREIGN PROCEDURE getFiles("
                               + "IN pathAndPattern string OPTIONS (ANNOTATION 'The path and pattern of what files to return.  Currently the only pattern supported is *.<ext>, which returns only the files matching the given extension at the given path.')) "
                               + "RETURNS TABLE (file blob, filePath string) "
                               + "OPTIONS (ANNOTATION 'Returns files that match the given path and pattern as BLOBs')";
        final AstNode procedureNode = this.parser.parse(getTokens(content), this.rootNode);
        assertMixinType(procedureNode, TeiidDdlLexicon.CreateProcedure.PROCEDURE_STATEMENT);
        assertProperty(procedureNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        assertThat(procedureNode.getName(), is("getFiles"));

        // 1 param, 1 result set, 1 option
        assertThat(procedureNode.getChildCount(), is(3));

        { // first param
            final AstNode param1 = procedureNode.childrenWithName("pathAndPattern").get(0);
            assertProperty(param1, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
            assertProperty(param1, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
            assertProperty(param1, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, false);
            assertThat(param1.getChildCount(), is(1)); // option
        }

        { // result set
            final AstNode resultSetNode = procedureNode.childrenWithName(TeiidDdlLexicon.CreateProcedure.RESULT_SET).get(0);
            assertMixinType(resultSetNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMNS);
            assertProperty(resultSetNode, TeiidDdlLexicon.CreateProcedure.TABLE_FLAG, true);
            assertThat(resultSetNode.getChildCount(), is(2));

            AstNode resultCol1 = null;
            AstNode resultCol2 = null;

            if ("file".equals(resultSetNode.getChild(0).getName())) {
                resultCol1 = resultSetNode.getChild(0);
                resultCol2 = resultSetNode.getChild(1);
            } else {
                resultCol2 = resultSetNode.getChild(0);
                resultCol1 = resultSetNode.getChild(1);
            }

            assertThat(resultCol1.getName(), is("file"));
            assertProperty(resultCol1, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.BLOB.toDdl());

            assertThat(resultCol2.getName(), is("filePath"));
            assertProperty(resultCol2, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
        }

        { // options
            assertProperty(procedureNode.childrenWithName("ANNOTATION").get(0),
                           StandardDdlLexicon.VALUE,
                           "Returns files that match the given path and pattern as BLOBs");
        }
    }

    @Test
    public void shouldParseFlatFileDdl2() {
        final String content = "CREATE FOREIGN PROCEDURE getTextFiles("
                               + "IN pathAndPattern string OPTIONS (ANNOTATION 'The path and pattern of what files to return.  Currently the only pattern supported is *.<ext>, which returns only the files matching the given extension at the given path.')) "
                               + "RETURNS TABLE (file clob, filePath string) "
                               + "OPTIONS (ANNOTATION 'Returns text files that match the given path and pattern as CLOBs')";
        final AstNode procedureNode = this.parser.parse(getTokens(content), this.rootNode);
        assertMixinType(procedureNode, TeiidDdlLexicon.CreateProcedure.PROCEDURE_STATEMENT);
        assertProperty(procedureNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        assertThat(procedureNode.getName(), is("getTextFiles"));

        // 1 param, 1 result set, 1 option
        assertThat(procedureNode.getChildCount(), is(3));

        { // first param
            final AstNode param1 = procedureNode.childrenWithName("pathAndPattern").get(0);
            assertProperty(param1, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
            assertProperty(param1, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
            assertProperty(param1, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, false);
            assertThat(param1.getChildCount(), is(1)); // option
        }

        { // result set
            final AstNode resultSetNode = procedureNode.childrenWithName(TeiidDdlLexicon.CreateProcedure.RESULT_SET).get(0);
            assertMixinType(resultSetNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMNS);
            assertProperty(resultSetNode, TeiidDdlLexicon.CreateProcedure.TABLE_FLAG, true);
            assertThat(resultSetNode.getChildCount(), is(2));

            AstNode resultCol1 = null;
            AstNode resultCol2 = null;

            if ("file".equals(resultSetNode.getChild(0).getName())) {
                resultCol1 = resultSetNode.getChild(0);
                resultCol2 = resultSetNode.getChild(1);
            } else {
                resultCol2 = resultSetNode.getChild(0);
                resultCol1 = resultSetNode.getChild(1);
            }

            assertThat(resultCol1.getName(), is("file"));
            assertProperty(resultCol1, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.CLOB.toDdl());

            assertThat(resultCol2.getName(), is("filePath"));
            assertProperty(resultCol2, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
        }

        { // options
            assertProperty(procedureNode.childrenWithName("ANNOTATION").get(0),
                           StandardDdlLexicon.VALUE,
                           "Returns text files that match the given path and pattern as CLOBs");
        }
    }

    @Test
    public void shouldParseFlatFileDdl3() {
        final String content = "CREATE FOREIGN PROCEDURE saveFile("
                               + "IN filePath string, "
                               + "IN file object OPTIONS (ANNOTATION 'The contents to save.  Can be one of CLOB, BLOB, or XML'))"
                               + "OPTIONS (ANNOTATION 'Saves the given value to the given path.  Any existing file will be overriden.')";
        final AstNode procedureNode = this.parser.parse(getTokens(content), this.rootNode);
        assertMixinType(procedureNode, TeiidDdlLexicon.CreateProcedure.PROCEDURE_STATEMENT);
        assertProperty(procedureNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        assertThat(procedureNode.getName(), is("saveFile"));

        // 2 params, 1 option
        assertThat(procedureNode.getChildCount(), is(3));

        { // first param
            final AstNode param1 = procedureNode.childrenWithName("filePath").get(0);
            assertProperty(param1, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
            assertProperty(param1, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
            assertProperty(param1, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, false);
            assertThat(param1.getChildCount(), is(0));
        }

        { // second param
            final AstNode param2 = procedureNode.childrenWithName("file").get(0);
            assertProperty(param2, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.OBJECT.toDdl());
            assertProperty(param2, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
            assertProperty(param2, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, false);
            assertThat(param2.getChildCount(), is(1)); // option
        }

        { // options
            assertProperty(procedureNode.childrenWithName("ANNOTATION").get(0),
                           StandardDdlLexicon.VALUE,
                           "Saves the given value to the given path.  Any existing file will be overriden.");
        }
    }

    @Test
    public void shouldParseWebService1() {
        final String content = "CREATE FOREIGN PROCEDURE invoke("
                               + "OUT result xml RESULT, "
                               + "IN binding string OPTIONS (ANNOTATION 'The invocation binding (HTTP, SOAP11, SOAP12).  May be set or allowed to default to null to use the default binding.'), "
                               + "IN action string OPTIONS (ANNOTATION 'With a SOAP invocation, action sets the SOAPAction.  With HTTP it sets the HTTP Method (GET, POST - default, etc.).'), "
                               + "IN request xml OPTIONS (ANNOTATION 'The XML document or root element that represents the request.  If the ExecutionFactory is configured in with a DefaultServiceMode of MESSAGE, then the SOAP request must contain the entire SOAP message.'), "
                               + "IN endpoint string OPTIONS (ANNOTATION 'The relative or abolute endpoint to use.  May be set or allowed to default to null to use the default endpoint address.'), "
                               + "IN stream boolean DEFAULT 'false' OPTIONS (ANNOTATION 'If the result should be streamed.'))"
                               + "OPTIONS (ANNOTATION 'Invokes a webservice that returns an XML result')";
        final AstNode procedureNode = this.parser.parse(getTokens(content), this.rootNode);
        assertMixinType(procedureNode, TeiidDdlLexicon.CreateProcedure.PROCEDURE_STATEMENT);
        assertProperty(procedureNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        assertThat(procedureNode.getName(), is("invoke"));

        // 6 params, 1 option
        assertThat(procedureNode.getChildCount(), is(7));

        { // first param
            final AstNode param1 = procedureNode.childrenWithName("result").get(0);
            assertProperty(param1, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.XML.toDdl());
            assertProperty(param1, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.OUT.toDdl());
            assertProperty(param1, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, true);
            assertThat(param1.getChildCount(), is(0));
        }

        { // second param
            final AstNode param2 = procedureNode.childrenWithName("binding").get(0);
            assertProperty(param2, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
            assertProperty(param2, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
            assertProperty(param2, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, false);
            assertThat(param2.getChildCount(), is(1)); // option
        }

        { // third param
            final AstNode param3 = procedureNode.childrenWithName("action").get(0);
            assertProperty(param3, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
            assertProperty(param3, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
            assertProperty(param3, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, false);
            assertThat(param3.getChildCount(), is(1)); // option
        }

        { // fourth param
            final AstNode param4 = procedureNode.childrenWithName("request").get(0);
            assertProperty(param4, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.XML.toDdl());
            assertProperty(param4, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
            assertProperty(param4, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, false);
            assertThat(param4.getChildCount(), is(1)); // option
        }

        { // fifth param
            final AstNode param5 = procedureNode.childrenWithName("endpoint").get(0);
            assertProperty(param5, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
            assertProperty(param5, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
            assertProperty(param5, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, false);
            assertThat(param5.getChildCount(), is(1)); // option
        }

        { // sixth param
            final AstNode param6 = procedureNode.childrenWithName("stream").get(0);
            assertProperty(param6, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.BOOLEAN.toDdl());
            assertProperty(param6, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
            assertProperty(param6, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, false);
            assertProperty(param6, StandardDdlLexicon.DEFAULT_VALUE, "false");
            assertThat(param6.getChildCount(), is(1)); // option
        }

        { // options
            assertProperty(procedureNode.childrenWithName("ANNOTATION").get(0),
                           StandardDdlLexicon.VALUE,
                           "Invokes a webservice that returns an XML result");
        }
    }

    @Test
    public void shouldParseWebService2() {
        final String content = "CREATE FOREIGN PROCEDURE invokeHttp("
                               + "OUT result blob RESULT, "
                               + "IN action string OPTIONS (ANNOTATION 'Sets the HTTP Method (GET, POST - default, etc.).'), "
                               + "IN request object OPTIONS (ANNOTATION 'The String, XML, BLOB, or CLOB value containing a payload (only for POST).'), "
                               + "IN endpoint string OPTIONS (ANNOTATION 'The relative or abolute endpoint to use.  May be set or allowed to default to null to use the default endpoint address.'), "
                               + "IN stream boolean DEFAULT 'false' OPTIONS (ANNOTATION 'If the result should be streamed.'), "
                               + "OUT contentType string) "
                               + "OPTIONS (ANNOTATION 'Invokes a webservice that returns an binary result')";
        final AstNode procedureNode = this.parser.parse(getTokens(content), this.rootNode);
        assertMixinType(procedureNode, TeiidDdlLexicon.CreateProcedure.PROCEDURE_STATEMENT);
        assertProperty(procedureNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        assertThat(procedureNode.getName(), is("invokeHttp"));

        // 6 params, 1 option
        assertThat(procedureNode.getChildCount(), is(7));

        { // first param
            final AstNode param1 = procedureNode.childrenWithName("result").get(0);
            assertProperty(param1, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.BLOB.toDdl());
            assertProperty(param1, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.OUT.toDdl());
            assertProperty(param1, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, true);
            assertThat(param1.getChildCount(), is(0));
        }

        { // second param
            final AstNode param2 = procedureNode.childrenWithName("action").get(0);
            assertProperty(param2, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
            assertProperty(param2, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
            assertProperty(param2, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, false);
            assertThat(param2.getChildCount(), is(1)); // option
        }

        { // third param
            final AstNode param3 = procedureNode.childrenWithName("request").get(0);
            assertProperty(param3, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.OBJECT.toDdl());
            assertProperty(param3, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
            assertProperty(param3, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, false);
            assertThat(param3.getChildCount(), is(1)); // option
        }

        { // fourth param
            final AstNode param4 = procedureNode.childrenWithName("endpoint").get(0);
            assertProperty(param4, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
            assertProperty(param4, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
            assertProperty(param4, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, false);
            assertThat(param4.getChildCount(), is(1)); // option
        }

        { // fifth param
            final AstNode param5 = procedureNode.childrenWithName("stream").get(0);
            assertProperty(param5, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.BOOLEAN.toDdl());
            assertProperty(param5, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
            assertProperty(param5, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, false);
            assertProperty(param5, StandardDdlLexicon.DEFAULT_VALUE, "false");
            assertThat(param5.getChildCount(), is(1)); // option
        }

        { // sixth param
            final AstNode param6 = procedureNode.childrenWithName("contentType").get(0);
            assertProperty(param6, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
            assertProperty(param6, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.OUT.toDdl());
            assertProperty(param6, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, false);
            assertThat(param6.getChildCount(), is(0));
        }

        { // options
            assertProperty(procedureNode.childrenWithName("ANNOTATION").get(0),
                           StandardDdlLexicon.VALUE,
                           "Invokes a webservice that returns an binary result");
        }
    }

    // ********* procedure parameter tests ***********

    @Test
    public void shouldParseImplicitParameterType() {
        final String content = "param string";
        this.parser.parseProcedureParameter(getTokens(content), this.rootNode);
        assertThat(this.rootNode.getChildCount(), is(1));

        final AstNode paramNode = this.rootNode.getChild(0);
        assertThat(paramNode.getName(), is("param"));
        assertMixinType(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER);
        assertProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
        assertProperty(paramNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
        assertProperty(paramNode, StandardDdlLexicon.NULLABLE, "NULL");
        assertProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, false);
    }

    @Test
    public void shouldParseInParameterType() {
        final String content = "IN param string";
        this.parser.parseProcedureParameter(getTokens(content), this.rootNode);
        assertThat(this.rootNode.getChildCount(), is(1));

        final AstNode paramNode = this.rootNode.getChild(0);
        assertThat(paramNode.getName(), is("param"));
        assertMixinType(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER);
        assertProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
        assertProperty(paramNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
        assertProperty(paramNode, StandardDdlLexicon.NULLABLE, "NULL");
        assertProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, false);
    }

    @Test
    public void shouldParseOutParameterType() {
        final String content = "OUT param string";
        this.parser.parseProcedureParameter(getTokens(content), this.rootNode);
        assertThat(this.rootNode.getChildCount(), is(1));

        final AstNode paramNode = this.rootNode.getChild(0);
        assertThat(paramNode.getName(), is("param"));
        assertMixinType(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER);
        assertProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.OUT.toDdl());
        assertProperty(paramNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
        assertProperty(paramNode, StandardDdlLexicon.NULLABLE, "NULL");
        assertProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, false);
    }

    @Test
    public void shouldParseInOutParameterType() {
        final String content = "INOUT param string";
        this.parser.parseProcedureParameter(getTokens(content), this.rootNode);
        assertThat(this.rootNode.getChildCount(), is(1));

        final AstNode paramNode = this.rootNode.getChild(0);
        assertThat(paramNode.getName(), is("param"));
        assertMixinType(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER);
        assertProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.INOUT.toDdl());
        assertProperty(paramNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
        assertProperty(paramNode, StandardDdlLexicon.NULLABLE, "NULL");
        assertProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, false);
    }

    @Test
    public void shouldParseVariadicParameterType() {
        final String content = "VARIADIC param string";
        this.parser.parseProcedureParameter(getTokens(content), this.rootNode);
        assertThat(this.rootNode.getChildCount(), is(1));

        final AstNode paramNode = this.rootNode.getChild(0);
        assertThat(paramNode.getName(), is("param"));
        assertMixinType(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER);
        assertProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidNonReservedWord.VARIADIC.toDdl());
        assertProperty(paramNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
        assertProperty(paramNode, StandardDdlLexicon.NULLABLE, "NULL");
        assertProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, false);
    }

    @Test
    public void shouldParseNotNullParameter() {
        final String content = "IN param string NOT NULL";
        this.parser.parseProcedureParameter(getTokens(content), this.rootNode);
        assertThat(this.rootNode.getChildCount(), is(1));

        final AstNode paramNode = this.rootNode.getChild(0);
        assertThat(paramNode.getName(), is("param"));
        assertMixinType(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER);
        assertProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
        assertProperty(paramNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
        assertProperty(paramNode, StandardDdlLexicon.NULLABLE, "NOT NULL");
        assertProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, false);
    }

    @Test
    public void shouldParseParameterWithResultFlag() {
        final String content = "IN param string RESULT";
        this.parser.parseProcedureParameter(getTokens(content), this.rootNode);
        assertThat(this.rootNode.getChildCount(), is(1));

        final AstNode paramNode = this.rootNode.getChild(0);
        assertThat(paramNode.getName(), is("param"));
        assertMixinType(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER);
        assertProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
        assertProperty(paramNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
        assertProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, true);
        assertProperty(paramNode, StandardDdlLexicon.NULLABLE, "NULL");
    }

    @Test
    public void shouldParseParameterWithDefaultValue() {
        final String content = "IN param string DEFAULT 'default-value'";
        this.parser.parseProcedureParameter(getTokens(content), this.rootNode);
        assertThat(this.rootNode.getChildCount(), is(1));

        final AstNode paramNode = this.rootNode.getChild(0);
        assertThat(paramNode.getName(), is("param"));
        assertMixinType(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER);
        assertProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
        assertProperty(paramNode, StandardDdlLexicon.DEFAULT_VALUE, "default-value");
        assertProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, false);
        assertProperty(paramNode, StandardDdlLexicon.NULLABLE, "NULL");
    }

    @Test
    public void shouldParseParameterWithOptionClause() {
        final String content = "IN param string OPTIONS (a 'a-value', b 'b-value')";
        this.parser.parseProcedureParameter(getTokens(content), this.rootNode);
        assertThat(this.rootNode.getChildCount(), is(1));

        final AstNode paramNode = this.rootNode.getChild(0);
        assertThat(paramNode.getName(), is("param"));
        assertMixinType(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER);
        assertProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
        assertProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, false);
        assertProperty(paramNode, StandardDdlLexicon.NULLABLE, "NULL");

        assertThat(paramNode.getChildCount(), is(2)); // 2 options

        AstNode optionA = null;
        AstNode optionB = null;

        if ("a".equals(paramNode.getChild(0).getName())) {
            optionA = paramNode.getChild(0);
            optionB = paramNode.getChild(1);
        } else {
            optionB = paramNode.getChild(0);
            optionA = paramNode.getChild(1);
        }

        assertThat(optionA.getName(), is("a"));
        assertMixinType(optionA, StandardDdlLexicon.TYPE_STATEMENT_OPTION);
        assertProperty(optionA, StandardDdlLexicon.VALUE, "a-value");

        assertThat(optionB.getName(), is("b"));
        assertMixinType(optionB, StandardDdlLexicon.TYPE_STATEMENT_OPTION);
        assertProperty(optionB, StandardDdlLexicon.VALUE, "b-value");
    }

    @Test
    public void shouldParseParameterWithUnorderedClauses() {
        final String content = "IN param string DEFAULT 'default-value' RESULT NOT NULL";
        this.parser.parseProcedureParameter(getTokens(content), this.rootNode);
        assertThat(this.rootNode.getChildCount(), is(1));

        final AstNode paramNode = this.rootNode.getChild(0);
        assertThat(paramNode.getName(), is("param"));
        assertMixinType(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER);
        assertProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, TeiidReservedWord.IN.toDdl());
        assertProperty(paramNode, StandardDdlLexicon.DEFAULT_VALUE, "default-value");
        assertProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, true);
        assertProperty(paramNode, StandardDdlLexicon.NULLABLE, "NOT NULL");
    }

    // ********* procedure parameters tests ***********

    @Test
    public void shouldParseNoParameters() {
        final String content = "()";
        this.parser.parseProcedureParameters(getTokens(content), this.rootNode);
        assertThat(this.rootNode.getChildCount(), is(0));
    }

    @Test
    public void shouldParseOneParameter() {
        final String content = "(INOUT param string)";
        this.parser.parseProcedureParameters(getTokens(content), this.rootNode);
        assertThat(this.rootNode.getChildCount(), is(1));
        assertMixinType(this.rootNode.getChild(0), TeiidDdlLexicon.CreateProcedure.PARAMETER);
    }

    @Test
    public void shouldParseMultipleParameters() {
        final String content = "(p1 string, in p2 boolean, out p3 integer, inout p4 decimal)";
        this.parser.parseProcedureParameters(getTokens(content), this.rootNode);
        assertThat(this.rootNode.getChildCount(), is(4));

        for (final AstNode paramNode : this.rootNode.getChildren()) {
            assertMixinType(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER);
        }
    }

    // ********* result column tests ***********

    @Test
    public void shouldParseResultColumn() {
        final String content = "r1 string";
        this.parser.parseProcedureResultColumn(getTokens(content), this.rootNode);
        assertThat(this.rootNode.getChildCount(), is(1));

        final AstNode paramNode = this.rootNode.getChild(0);
        assertThat(paramNode.getName(), is("r1"));
        assertMixinType(paramNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMN);
        assertProperty(paramNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
        assertProperty(paramNode, StandardDdlLexicon.NULLABLE, "NULL");
    }

    @Test
    public void shouldParseNotNullResultColumn() {
        final String content = "r1 string NOT NULL";
        this.parser.parseProcedureResultColumn(getTokens(content), this.rootNode);
        assertThat(this.rootNode.getChildCount(), is(1));

        final AstNode paramNode = this.rootNode.getChild(0);
        assertThat(paramNode.getName(), is("r1"));
        assertMixinType(paramNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMN);
        assertProperty(paramNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
        assertProperty(paramNode, StandardDdlLexicon.NULLABLE, "NOT NULL");
    }

    @Test
    public void shouldParseResultColumnWithOptionClause() {
        final String content = "r1 string OPTIONS (a 'a-value', b 'b-value')";
        this.parser.parseProcedureResultColumn(getTokens(content), this.rootNode);
        assertThat(this.rootNode.getChildCount(), is(1));

        final AstNode resultColumnNode = this.rootNode.getChild(0);
        assertThat(resultColumnNode.getName(), is("r1"));
        assertMixinType(resultColumnNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMN);
        assertProperty(resultColumnNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
        assertProperty(resultColumnNode, StandardDdlLexicon.NULLABLE, "NULL");

        assertThat(resultColumnNode.getChildCount(), is(2)); // 2 options

        AstNode optionA = null;
        AstNode optionB = null;

        if ("a".equals(resultColumnNode.getChild(0).getName())) {
            optionA = resultColumnNode.getChild(0);
            optionB = resultColumnNode.getChild(1);
        } else {
            optionB = resultColumnNode.getChild(0);
            optionA = resultColumnNode.getChild(1);
        }

        assertThat(optionA.getName(), is("a"));
        assertMixinType(optionA, StandardDdlLexicon.TYPE_STATEMENT_OPTION);
        assertProperty(optionA, StandardDdlLexicon.VALUE, "a-value");

        assertThat(optionB.getName(), is("b"));
        assertMixinType(optionB, StandardDdlLexicon.TYPE_STATEMENT_OPTION);
        assertProperty(optionB, StandardDdlLexicon.VALUE, "b-value");
    }

    // ********* result columns tests ***********

    @Test
    public void shouldParseOneResultColumn() {
        final String content = "(r1 string)";
        assertThat(this.parser.parseProcedureResultColumns(getTokens(content), this.rootNode), is(true));
        assertThat(this.rootNode.getChildCount(), is(1));

        final AstNode resultColumnsNode = this.rootNode.getChild(0);
        assertMixinType(resultColumnsNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMNS);
        assertProperty(resultColumnsNode, TeiidDdlLexicon.CreateProcedure.TABLE_FLAG, false);

        assertThat(resultColumnsNode.getChildCount(), is(1));
        assertMixinType(resultColumnsNode.getChild(0), TeiidDdlLexicon.CreateProcedure.RESULT_COLUMN);
    }

    @Test
    public void shouldParseOneResultColumnWithTable() {
        final String content = "table (r1 string)";
        assertThat(this.parser.parseProcedureResultColumns(getTokens(content), this.rootNode), is(true));
        assertThat(this.rootNode.getChildCount(), is(1));

        final AstNode resultColumnsNode = this.rootNode.getChild(0);
        assertMixinType(resultColumnsNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMNS);
        assertProperty(resultColumnsNode, TeiidDdlLexicon.CreateProcedure.TABLE_FLAG, true);

        assertThat(resultColumnsNode.getChildCount(), is(1));
        assertMixinType(resultColumnsNode.getChild(0), TeiidDdlLexicon.CreateProcedure.RESULT_COLUMN);
    }

    @Test
    public void shouldParseMultipleResultColumns() {
        final String content = "(r1 string, r2 boolean, r3 integer, r4 blob, r5 bigdecimal)";
        assertThat(this.parser.parseProcedureResultColumns(getTokens(content), this.rootNode), is(true));
        assertThat(this.rootNode.getChildCount(), is(1));

        final AstNode resultColumnsNode = this.rootNode.getChild(0);
        assertMixinType(resultColumnsNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMNS);
        assertProperty(resultColumnsNode, TeiidDdlLexicon.CreateProcedure.TABLE_FLAG, false);
        assertThat(resultColumnsNode.getChildCount(), is(5));

        for (final AstNode resultColumnNode : resultColumnsNode.getChildren()) {
            assertMixinType(resultColumnNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMN);
        }
    }

    @Test
    public void shouldParseMultipleResultColumnsWithTable() {
        final String content = "TABLE (r1 string, r2 boolean, r3 integer, r4 blob, r5 bigdecimal)";
        assertThat(this.parser.parseProcedureResultColumns(getTokens(content), this.rootNode), is(true));
        assertThat(this.rootNode.getChildCount(), is(1));

        final AstNode resultColumnsNode = this.rootNode.getChild(0);
        assertMixinType(resultColumnsNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMNS);
        assertProperty(resultColumnsNode, TeiidDdlLexicon.CreateProcedure.TABLE_FLAG, true);
        assertThat(resultColumnsNode.getChildCount(), is(5));

        for (final AstNode resultColumnNode : resultColumnsNode.getChildren()) {
            assertMixinType(resultColumnNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMN);
        }
    }

    @Test
    public void shouldNotParseDataTypeWithParseResultColumns() {
        final String content = "r1 string";
        assertThat(this.parser.parseProcedureResultColumns(getTokens(content), this.rootNode), is(false));
    }

    // ********* AS clause tests ***********

    @Test
    public void shouldParseAsClauseWithOneStatement() {
        final String content = "AS SELECT * FROM PM1.G1;";
        assertThat(this.parser.parseAsClause(getTokens(content), this.rootNode), is(true));
        assertProperty(this.rootNode, TeiidDdlLexicon.CreateProcedure.STATEMENT, "SELECT * FROM PM1.G1;");
    }

    @Test
    public void shouldParseAsClauseWithOneStatementNoEndingSemiColon() {
        final String content = "AS SELECT * FROM PM1.G1";
        assertThat(this.parser.parseAsClause(getTokens(content), this.rootNode), is(true));
        assertProperty(this.rootNode, TeiidDdlLexicon.CreateProcedure.STATEMENT, "SELECT * FROM PM1.G1");
    }

    @Test
    public void shouldParseAsClauseWithMultipleStatements() {
        final String content = "AS BEGIN SELECT * FROM G1;SELECT * FROM PM1.G2; END";
        assertThat(this.parser.parseAsClause(getTokens(content), this.rootNode), is(true));
        assertProperty(this.rootNode,
                       TeiidDdlLexicon.CreateProcedure.STATEMENT,
                       "BEGIN SELECT * FROM G1;SELECT * FROM PM1.G2; END");
    }

    // ********* returns clause tests ***********

    @Test
    public void shouldParseDataTypeReturnsClause() {
        final String content = "returns string";
        assertThat(this.parser.parseReturnsClause(getTokens(content), this.rootNode), is(true));
        assertThat(this.rootNode.getChildCount(), is(1));

        final AstNode resultSetNode = this.rootNode.getChild(0);
        assertThat(resultSetNode.getName(), is(TeiidDdlLexicon.CreateProcedure.RESULT_SET));
        assertMixinType(resultSetNode, TeiidDdlLexicon.CreateProcedure.RESULT_DATA_TYPE);
        assertProperty(resultSetNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
    }

    @Test
    public void shouldParseResultColumnsReturnsClause() {
        final String content = "returns (created_on varchar(25), from_user varchar(25), to_user varchar(25), profile_image_url varchar(25), source varchar(25), text varchar(140))";
        assertThat(this.parser.parseReturnsClause(getTokens(content), this.rootNode), is(true));
        assertThat(this.rootNode.getChildCount(), is(1));

        final AstNode resultSetNode = this.rootNode.getChild(0);
        assertThat(resultSetNode.getName(), is(TeiidDdlLexicon.CreateProcedure.RESULT_SET));
        assertMixinType(resultSetNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMNS);
    }

}
