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
import static org.modeshape.jcr.api.JcrConstants.NT_UNSTRUCTURED;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.PARSER_ID;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import org.junit.After;
import org.junit.Test;
import org.modeshape.common.junit.SkipLongRunning;
import org.modeshape.sequencer.ddl.AbstractDdlSequencerTest;
import org.modeshape.sequencer.ddl.DdlConstants;
import org.modeshape.sequencer.ddl.StandardDdlLexicon;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.SchemaElementType;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.TeiidDataType;

/**
 * Unit test for the {@link org.modeshape.sequencer.ddl.DdlSequencer} when Teiid dialects are parsed.
 */
public class TeiidDdlSequencerTest extends AbstractDdlSequencerTest {

    private Node statementsNode;

    @After
    public void verifyStatementsNode() throws Exception {
        verifyPrimaryType(this.statementsNode, NT_UNSTRUCTURED);
        verifyProperty(this.statementsNode, PARSER_ID, "TEIID");
    }

    @Test
    public void shouldSequenceOptionNamespace() throws Exception {
        this.statementsNode = sequenceDdl("ddl/dialect/teiid/optionNamespace.ddl");
        assertThat(this.statementsNode.getNodes().getSize(), is(2L));

        { // option namespace
            final Node optionNamespaceNode = this.statementsNode.getNode("REST");
            verifyMixinType(optionNamespaceNode, TeiidDdlLexicon.OptionNamespace.STATEMENT);
            verifyProperty(optionNamespaceNode, TeiidDdlLexicon.OptionNamespace.URI, "http://teiid.org/rest");
        }

        { // create procedure
            final Node optionNamespaceNode = this.statementsNode.getNode("g1Table");
            verifyMixinType(optionNamespaceNode, TeiidDdlLexicon.CreateProcedure.PROCEDURE_STATEMENT);
            verifyProperty(optionNamespaceNode,
                           TeiidDdlLexicon.CreateProcedure.STATEMENT,
                           "BEGIN\nSELECT XMLELEMENT(NAME \"all\", XMLAGG(XMLELEMENT(NAME \"row\", XMLFOREST(e1, e2)))) AS xml_out FROM Txns.G1;\nEND");

            { // REST:METHOD option
                final Node optionNode = optionNamespaceNode.getNode("{http://teiid.org/rest}METHOD");
                verifyMixinType(optionNode, StandardDdlLexicon.TYPE_STATEMENT_OPTION);
                verifyProperty(optionNode, StandardDdlLexicon.VALUE, "GET");
            }

            { // REST:URI option
                final Node optionNode = optionNamespaceNode.getNode("{http://teiid.org/rest}URI");
                verifyMixinType(optionNode, StandardDdlLexicon.TYPE_STATEMENT_OPTION);
                verifyProperty(optionNode, StandardDdlLexicon.VALUE, "g1");
            }
        }
    }

    @Test
    public void shouldSequenceFlatFileDdl() throws Exception {
        this.statementsNode = sequenceDdl("ddl/dialect/teiid/flatFile.ddl");
        assertThat(this.statementsNode.getNodes().getSize(), is(3L));

        { // getFiles
            final Node procedureNode = this.statementsNode.getNode("getFiles");
            verifyMixinType(procedureNode, TeiidDdlLexicon.CreateProcedure.PROCEDURE_STATEMENT);
            verifyProperty(procedureNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
            assertThat(procedureNode.getNodes().getSize(), is(3L)); // param, returns, option

            { // pathAndPattern parameter
                final Node paramNode = procedureNode.getNode("pathAndPattern");
                verifyMixinType(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER);
                verifyProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, "IN");
                verifyProperty(paramNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
                verifyProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, "false");
                assertThat(paramNode.getNodes().getSize(), is(1L)); // option

                { // ANNOTATION option
                    final Node optionNode = paramNode.getNode("ANNOTATION");
                    verifyMixinType(optionNode, StandardDdlLexicon.TYPE_STATEMENT_OPTION);
                    verifyProperty(optionNode,
                                   StandardDdlLexicon.VALUE,
                                   "The path and pattern of what files to return.  Currently the only pattern supported is *.<ext>, which returns only the files matching the given extension at the given path.");
                }
            }

            { // returns
                final Node resultNode = procedureNode.getNode(TeiidDdlLexicon.CreateProcedure.RESULT_SET);
                verifyMixinType(resultNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMNS);
                verifyProperty(resultNode, TeiidDdlLexicon.CreateProcedure.TABLE_FLAG, "true");
                assertThat(resultNode.getNodes().getSize(), is(2L)); // result columns

                { // file result column
                    final Node resultColumnNode = resultNode.getNode("file");
                    verifyMixinType(resultColumnNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMN);
                    verifyProperty(resultColumnNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.BLOB.toDdl());
                }

                { // filePath result column
                    final Node resultColumnNode = resultNode.getNode("filePath");
                    verifyMixinType(resultColumnNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMN);
                    verifyProperty(resultColumnNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
                }
            }

            { // ANNOTATION option
                final Node optionNode = procedureNode.getNode("ANNOTATION");
                verifyMixinType(optionNode, StandardDdlLexicon.TYPE_STATEMENT_OPTION);
                verifyProperty(optionNode,
                               StandardDdlLexicon.VALUE,
                               "Returns files that match the given path and pattern as BLOBs");
            }
        }

        { // getTextFiles
            final Node procedureNode = this.statementsNode.getNode("getTextFiles");
            verifyMixinType(procedureNode, TeiidDdlLexicon.CreateProcedure.PROCEDURE_STATEMENT);
            verifyProperty(procedureNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
            assertThat(procedureNode.getNodes().getSize(), is(3L)); // param, returns, option

            { // pathAndPattern parameter
                final Node paramNode = procedureNode.getNode("pathAndPattern");
                verifyMixinType(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER);
                verifyProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, "IN");
                verifyProperty(paramNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
                verifyProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, "false");
                assertThat(paramNode.getNodes().getSize(), is(1L)); // option

                { // ANNOTATION option
                    final Node optionNode = paramNode.getNode("ANNOTATION");
                    verifyMixinType(optionNode, StandardDdlLexicon.TYPE_STATEMENT_OPTION);
                    verifyProperty(optionNode,
                                   StandardDdlLexicon.VALUE,
                                   "The path and pattern of what files to return.  Currently the only pattern supported is *.<ext>, which returns only the files matching the given extension at the given path.");
                }
            }

            { // returns
                final Node resultNode = procedureNode.getNode(TeiidDdlLexicon.CreateProcedure.RESULT_SET);
                verifyMixinType(resultNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMNS);
                verifyProperty(resultNode, TeiidDdlLexicon.CreateProcedure.TABLE_FLAG, "true");
                assertThat(resultNode.getNodes().getSize(), is(2L)); // result columns

                { // file result column
                    final Node resultColumnNode = resultNode.getNode("file");
                    verifyMixinType(resultColumnNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMN);
                    verifyProperty(resultColumnNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.CLOB.toDdl());
                }

                { // filePath result column
                    final Node resultColumnNode = resultNode.getNode("filePath");
                    verifyMixinType(resultColumnNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMN);
                    verifyProperty(resultColumnNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
                }
            }

            { // ANNOTATION option
                final Node optionNode = procedureNode.getNode("ANNOTATION");
                verifyMixinType(optionNode, StandardDdlLexicon.TYPE_STATEMENT_OPTION);
                verifyProperty(optionNode,
                               StandardDdlLexicon.VALUE,
                               "Returns text files that match the given path and pattern as CLOBs");
            }
        }

        { // saveFile
            final Node procedureNode = this.statementsNode.getNode("saveFile");
            verifyMixinType(procedureNode, TeiidDdlLexicon.CreateProcedure.PROCEDURE_STATEMENT);
            verifyProperty(procedureNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
            assertThat(procedureNode.getNodes().getSize(), is(3L)); // 2 params, 1 option

            { // filePath parameter
                final Node paramNode = procedureNode.getNode("filePath");
                verifyMixinType(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER);
                verifyProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, "IN");
                verifyProperty(paramNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
                verifyProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, "false");
                assertThat(paramNode.getNodes().getSize(), is(0L));
            }

            { // file parameter
                final Node paramNode = procedureNode.getNode("file");
                verifyMixinType(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER);
                verifyProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, "IN");
                verifyProperty(paramNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.OBJECT.toDdl());
                verifyProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, "false");
                assertThat(paramNode.getNodes().getSize(), is(1L));

                { // ANNOTATION option
                    final Node optionNode = paramNode.getNode("ANNOTATION");
                    verifyMixinType(optionNode, StandardDdlLexicon.TYPE_STATEMENT_OPTION);
                    verifyProperty(optionNode,
                                   StandardDdlLexicon.VALUE,
                                   "The contents to save.  Can be one of CLOB, BLOB, or XML");
                }
            }

            { // ANNOTATION option
                final Node optionNode = procedureNode.getNode("ANNOTATION");
                verifyMixinType(optionNode, StandardDdlLexicon.TYPE_STATEMENT_OPTION);
                verifyProperty(optionNode,
                               StandardDdlLexicon.VALUE,
                               "Saves the given value to the given path.  Any existing file will be overriden.");
            }
        }
    }

    @Test
    public void shouldSequenceTwitterWebServiceDdl() throws Exception {
        this.statementsNode = sequenceDdl("ddl/dialect/teiid/twitterWebService.ddl");
        assertThat(this.statementsNode.getNodes().getSize(), is(2L));

        { // invoke
            final Node procedureNode = this.statementsNode.getNode("invoke");
            verifyMixinType(procedureNode, TeiidDdlLexicon.CreateProcedure.PROCEDURE_STATEMENT);
            verifyProperty(procedureNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
            assertThat(procedureNode.getNodes().getSize(), is(7L)); // 6 params, 1 option

            { // result parameter
                final Node paramNode = procedureNode.getNode("result");
                verifyMixinType(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER);
                verifyProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, "OUT");
                verifyProperty(paramNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.XML.toDdl());
                verifyProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, "true");
                assertThat(paramNode.getNodes().getSize(), is(0L));
            }
        }

        { // invokeHttp
            final Node procedureNode = this.statementsNode.getNode("invokeHttp");
            verifyMixinType(procedureNode, TeiidDdlLexicon.CreateProcedure.PROCEDURE_STATEMENT);
            verifyProperty(procedureNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
            assertThat(procedureNode.getNodes().getSize(), is(7L)); // 6 params, 1 option
        }
    }

    @Test
    public void shouldSequenceMySqlBqtDdl() throws Exception {
        this.statementsNode = sequenceDdl("ddl/dialect/teiid/mySqlBqt.ddl");
        assertThat(this.statementsNode.getNodes().getSize(), is(15L));
    }

    @Test
    public void shouldSequenceAlterOptionsDdl() throws Exception {
        this.statementsNode = sequenceDdl("ddl/dialect/teiid/alterOptions.ddl");
        assertThat(this.statementsNode.getNodes().getSize(), is(26L));

        { // myTable
            final NodeIterator itr = this.statementsNode.getNodes("myTable");
            assertThat(itr.getSize(), is(11L)); // 1 view, 10 alter tables
            verifyMixinType(itr.nextNode(), TeiidDdlLexicon.CreateTable.TABLE_STATEMENT);

            for (int i = 0; i < 10; ++i) {
                verifyMixinType(itr.nextNode(), TeiidDdlLexicon.AlterOptions.TABLE_STATEMENT);
            }
        }

        { // myView
            final NodeIterator itr = this.statementsNode.getNodes("myView");
            assertThat(itr.getSize(), is(11L)); // 1 view, 10 alter views

            final Node viewNode = itr.nextNode();
            verifyMixinType(viewNode, TeiidDdlLexicon.CreateTable.VIEW_STATEMENT);
            verifyProperty(viewNode, TeiidDdlLexicon.CreateTable.QUERY_EXPRESSION, "select e1, e2 from foo.bar");

            { // option
                final Node optionNode = viewNode.getNode("CARDINALITY");
                verifyMixinType(optionNode, StandardDdlLexicon.TYPE_STATEMENT_OPTION);
                verifyProperty(optionNode, StandardDdlLexicon.VALUE, "12");
            }

            // columns
            final Node v1ColNode = viewNode.getNode("v1");
            final Node v2ColNode = viewNode.getNode("v2");

            for (int i = 0; i < 10; ++i) {
                final Node alterNode = itr.nextNode();
                verifyMixinType(alterNode, TeiidDdlLexicon.AlterOptions.VIEW_STATEMENT);
                verifyProperty(alterNode, TeiidDdlLexicon.AlterOptions.REFERENCE, viewNode.getIdentifier());
                assertThat(alterNode.getNodes().getSize(), is(1L));

                final Node optionsListNode = alterNode.getNodes().nextNode();

                if (i < 6) {
                    verifyMixinType(optionsListNode, TeiidDdlLexicon.AlterOptions.OPTIONS_LIST);
                    assertThat(optionsListNode.hasProperty(TeiidDdlLexicon.AlterOptions.DROPPED), is(i > 3));
                    assertThat(optionsListNode.hasProperty(TeiidDdlLexicon.AlterOptions.REFERENCE), is(false));

                    if (i == 4) {
                        verifyProperty(optionsListNode, TeiidDdlLexicon.AlterOptions.DROPPED, "CARDINALITY");
                        assertThat(optionsListNode.getNodes().getSize(), is(0L));
                    } else if (i == 5) {
                        verifyProperty(optionsListNode, TeiidDdlLexicon.AlterOptions.DROPPED, "FOO");
                        assertThat(optionsListNode.getNodes().getSize(), is(0L));
                    } else {
                        assertThat(optionsListNode.hasProperty(TeiidDdlLexicon.AlterOptions.DROPPED), is(false));

                        // option
                        assertThat(optionsListNode.getNodes().getSize(), is(1L));
                        final Node optionNode = optionsListNode.getNodes().nextNode();
                        verifyMixinType(optionNode, StandardDdlLexicon.TYPE_STATEMENT_OPTION);

                        String name = null;
                        String value = null;

                        if (i == 0) {
                            name = "CARDINALITY";
                            value = "12";
                        } else if (i == 1) {
                            name = "FOO";
                            value = "BAR";
                        } else if (i == 2) {
                            name = "CARDINALITY";
                            value = "24";
                        } else if (i == 3) {
                            name = "FOO";
                            value = "BARBAR";
                        }

                        assertThat(optionNode.getName(), is(name));
                        verifyProperty(optionNode, StandardDdlLexicon.VALUE, value);

                    }
                } else {
                    verifyMixinType(alterNode.getNodes().nextNode(), TeiidDdlLexicon.AlterOptions.COLUMN);
                    assertThat(optionsListNode.hasProperty(TeiidDdlLexicon.AlterOptions.DROPPED), is(i > 7));

                    if ((i == 6) || (i == 8)) {
                        verifyProperty(optionsListNode, TeiidDdlLexicon.AlterOptions.REFERENCE, v1ColNode.getIdentifier());
                    } else {
                        verifyProperty(optionsListNode, TeiidDdlLexicon.AlterOptions.REFERENCE, v2ColNode.getIdentifier());
                    }

                    if (i == 8) {
                        verifyProperty(optionsListNode, TeiidDdlLexicon.AlterOptions.DROPPED, "NULL_VALUE_COUNT");
                        assertThat(optionsListNode.getNodes().getSize(), is(0L));
                    } else if (i == 9) {
                        verifyProperty(optionsListNode, TeiidDdlLexicon.AlterOptions.DROPPED, "FOO");
                        assertThat(optionsListNode.getNodes().getSize(), is(0L));
                    } else {
                        assertThat(optionsListNode.getNodes().getSize(), is(1L));
                    }
                }
            }
        }

        { // myProc
            final NodeIterator itr = this.statementsNode.getNodes("myProc");
            assertThat(itr.getSize(), is(4L)); // 1 view, 3 alter procedures

            final Node procedureNode = itr.nextNode();
            verifyMixinType(procedureNode, TeiidDdlLexicon.CreateProcedure.PROCEDURE_STATEMENT);
            assertThat(procedureNode.getNodes().getSize(), is(3L)); // 3 columns

            for (int i = 0; i < 3; ++i) {
                final Node alterNode = itr.nextNode();
                verifyMixinType(alterNode, TeiidDdlLexicon.AlterOptions.PROCEDURE_STATEMENT);
                verifyProperty(alterNode, TeiidDdlLexicon.AlterOptions.REFERENCE, procedureNode.getIdentifier());
                assertThat(alterNode.getNodes().getSize(), is(1L));

                final Node optionsListNode = alterNode.getNodes().nextNode();

                if (i != 1) {
                    verifyMixinType(optionsListNode, TeiidDdlLexicon.AlterOptions.OPTIONS_LIST);
                    assertThat(optionsListNode.hasProperty(TeiidDdlLexicon.AlterOptions.REFERENCE), is(false));

                    if (i == 0) {
                        assertThat(optionsListNode.hasProperty(TeiidDdlLexicon.AlterOptions.DROPPED), is(false));
                        assertThat(optionsListNode.getNodes().getSize(), is(1L)); // option

                        // option
                        final Node optionNode = optionsListNode.getNode("NAMEINSOURCE");
                        verifyMixinType(optionNode, StandardDdlLexicon.TYPE_STATEMENT_OPTION);
                        verifyProperty(optionNode, StandardDdlLexicon.VALUE, "x");
                    } else {
                        verifyProperty(optionsListNode, TeiidDdlLexicon.AlterOptions.DROPPED, "UPDATECOUNT");
                        assertThat(optionsListNode.getProperty(TeiidDdlLexicon.AlterOptions.DROPPED).getValues()[1].getString(),
                                   is("NAMEINSOURCE"));
                        assertThat(optionsListNode.getNodes().getSize(), is(0L));
                    }
                } else {
                    verifyMixinType(optionsListNode, TeiidDdlLexicon.AlterOptions.PARAMETER);
                    assertThat(optionsListNode.getNodes().getSize(), is(2L)); // 2 options

                    final Node p2ParmNode = procedureNode.getNode("p2");
                    verifyProperty(optionsListNode, TeiidDdlLexicon.AlterOptions.REFERENCE, p2ParmNode.getIdentifier());

                    { // option1
                        final Node optionNode = optionsListNode.getNode("x");
                        verifyMixinType(optionNode, StandardDdlLexicon.TYPE_STATEMENT_OPTION);
                        verifyProperty(optionNode, StandardDdlLexicon.VALUE, "y");
                    }

                    { // option2
                        final Node optionNode = optionsListNode.getNode("a");
                        verifyMixinType(optionNode, StandardDdlLexicon.TYPE_STATEMENT_OPTION);
                        verifyProperty(optionNode, StandardDdlLexicon.VALUE, "b");
                    }
                }
            }
        }
    }

    @Test
    public void shouldSequenceCreateTriggerDdl() throws Exception {
        this.statementsNode = sequenceDdl("ddl/dialect/teiid/createTrigger.ddl");
        assertThat(this.statementsNode.getNodes().getSize(), is(5L)); // 2 views, 3 triggers

        { // myView
            final NodeIterator itr = this.statementsNode.getNodes("myView");
            assertThat(itr.getSize(), is(2L)); // 1 view, 1 trigger

            // view
            final Node viewNode = itr.nextNode();
            verifyMixinType(viewNode, TeiidDdlLexicon.CreateTable.VIEW_STATEMENT);
            verifyProperty(viewNode, TeiidDdlLexicon.CreateTable.QUERY_EXPRESSION, "select * from foo");
            assertThat(viewNode.getNodes().getSize(), is(2L)); // 2 columns

            { // trigger node
                final Node triggerNode = itr.nextNode();
                verifyMixinType(triggerNode, TeiidDdlLexicon.CreateTrigger.STATEMENT);
                verifyProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.INSTEAD_OF, DdlConstants.INSERT);
                verifyProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.ATOMIC, "false");
                verifyProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.TABLE_REFERENCE, viewNode.getIdentifier());

                final NodeIterator actionItr = triggerNode.getNodes();
                assertThat(actionItr.getSize(), is(3L)); // 3 trigger row actions

                { // first action
                    final Node actionNode = actionItr.nextNode();
                    assertThat(actionNode.getName(), is(TeiidDdlLexicon.CreateTrigger.ROW_ACTION));
                    verifyMixinType(actionNode, TeiidDdlLexicon.CreateTrigger.TRIGGER_ROW_ACTION);
                    verifyProperty(actionNode,
                                   TeiidDdlLexicon.CreateTrigger.ACTION,
                                   "insert into myView (age, name) values (4, 'Lucas');");
                }

                { // second action
                    final Node actionNode = actionItr.nextNode();
                    assertThat(actionNode.getName(), is(TeiidDdlLexicon.CreateTrigger.ROW_ACTION));
                    verifyMixinType(actionNode, TeiidDdlLexicon.CreateTrigger.TRIGGER_ROW_ACTION);
                    verifyProperty(actionNode,
                                   TeiidDdlLexicon.CreateTrigger.ACTION,
                                   "insert into myView (age, name) values (6, 'Brady');");
                }

                { // third action
                    final Node actionNode = actionItr.nextNode();
                    assertThat(actionNode.getName(), is(TeiidDdlLexicon.CreateTrigger.ROW_ACTION));
                    verifyMixinType(actionNode, TeiidDdlLexicon.CreateTrigger.TRIGGER_ROW_ACTION);
                    verifyProperty(actionNode,
                                   TeiidDdlLexicon.CreateTrigger.ACTION,
                                   "insert into myView (age, name) values (11, 'Joshua');");
                }
            }
        }

        { // HS_VIEW
            final NodeIterator itr = this.statementsNode.getNodes("HS_VIEW");
            assertThat(itr.getSize(), is(3L)); // 1 view, 2 triggers

            // view
            final Node viewNode = itr.nextNode();
            verifyMixinType(viewNode, TeiidDdlLexicon.CreateTable.VIEW_STATEMENT);
            verifyProperty(viewNode, TeiidDdlLexicon.CreateTable.QUERY_EXPRESSION, "select * from Accounts.HEALTHSTATE");
            assertThat(viewNode.getNodes().getSize(), is(1L)); // 1 option

            { // trigger1
                final Node triggerNode = itr.nextNode();
                verifyMixinType(triggerNode, TeiidDdlLexicon.CreateTrigger.STATEMENT);
                verifyProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.INSTEAD_OF, DdlConstants.INSERT);
                verifyProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.ATOMIC, "true");
                verifyProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.TABLE_REFERENCE, viewNode.getIdentifier());
                assertThat(triggerNode.getNodes().getSize(), is(1L)); // 1 trigger row action

                final Node actionNode = triggerNode.getNodes().nextNode();
                assertThat(actionNode.getName(), is(TeiidDdlLexicon.CreateTrigger.ROW_ACTION));
                verifyMixinType(actionNode, TeiidDdlLexicon.CreateTrigger.TRIGGER_ROW_ACTION);
                verifyProperty(actionNode,
                               TeiidDdlLexicon.CreateTrigger.ACTION,
                               "SELECT RepHealth(New.HEALTHTIME, New.POLICYKEY, New.OBJKEY, New.HEALTHSTATE) from HS_VIEW;");
            }

            { // trigger2
                final Node triggerNode = itr.nextNode();
                verifyMixinType(triggerNode, TeiidDdlLexicon.CreateTrigger.STATEMENT);
                verifyProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.INSTEAD_OF, DdlConstants.UPDATE);
                verifyProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.ATOMIC, "true");
                verifyProperty(triggerNode, TeiidDdlLexicon.CreateTrigger.TABLE_REFERENCE, viewNode.getIdentifier());
                assertThat(triggerNode.getNodes().getSize(), is(1L)); // 1 trigger row action

                final Node actionNode = triggerNode.getNodes().nextNode();
                assertThat(actionNode.getName(), is(TeiidDdlLexicon.CreateTrigger.ROW_ACTION));
                verifyMixinType(actionNode, TeiidDdlLexicon.CreateTrigger.TRIGGER_ROW_ACTION);
                verifyProperty(actionNode,
                               TeiidDdlLexicon.CreateTrigger.ACTION,
                               "SELECT RepHealth(New.HEALTHTIME, New.POLICYKEY, New.OBJKEY, New.HEALTHSTATE) from HS_VIEW;");
            }
        }
    }

    @Test
    public void shouldSequenceAccountsDdl() throws Exception {
        this.statementsNode = sequenceDdl("ddl/dialect/teiid/accounts.ddl");
        assertThat(this.statementsNode.getNodes().getSize(), is(5L));

        { // accounts.ACCOUNT
            final Node tableNode = statementsNode.getNode("accounts.ACCOUNT");
            verifyMixinType(tableNode, TeiidDdlLexicon.CreateTable.TABLE_STATEMENT);
            verifyProperty(tableNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());

            { // ACCOUNT_ID column
                final NodeIterator itr = tableNode.getNodes("ACCOUNT_ID");
                assertThat(itr.getSize(), is(1L));
                final Node columnNode = itr.nextNode();
                verifyProperty(columnNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.LONG.toDdl());
                verifyProperty(columnNode, StandardDdlLexicon.DEFAULT_VALUE, 0);

                { // NAMEINSOURCE option
                    final Node optionNode = columnNode.getNode("NAMEINSOURCE");
                    verifyMixinType(optionNode, StandardDdlLexicon.TYPE_STATEMENT_OPTION);
                    verifyProperty(optionNode, StandardDdlLexicon.VALUE, "`ACCOUNT_ID`");
                }

                { // NATIVE_TYPE option
                    final Node optionNode = columnNode.getNode("NATIVE_TYPE");
                    verifyMixinType(optionNode, StandardDdlLexicon.TYPE_STATEMENT_OPTION);
                    verifyProperty(optionNode, StandardDdlLexicon.VALUE, "INT");
                }
            }

            { // SSN column
                final NodeIterator itr = tableNode.getNodes("SSN");
                assertThat(itr.getSize(), is(1L));
                final Node columnNode = itr.nextNode();
                verifyProperty(columnNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
                verifyProperty(columnNode, StandardDdlLexicon.DATATYPE_LENGTH, 10);
            }

            { // STATUS column
                final NodeIterator itr = tableNode.getNodes("STATUS");
                assertThat(itr.getSize(), is(1L));
                final Node columnNode = itr.nextNode();
                verifyProperty(columnNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
                verifyProperty(columnNode, StandardDdlLexicon.DATATYPE_LENGTH, 10);
            }

            { // TYPE column
                final NodeIterator itr = tableNode.getNodes("TYPE");
                assertThat(itr.getSize(), is(1L));
                final Node columnNode = itr.nextNode();
                verifyProperty(columnNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
                verifyProperty(columnNode, StandardDdlLexicon.DATATYPE_LENGTH, 10);
            }

            { // DATEOPENED column
                final NodeIterator itr = tableNode.getNodes("DATEOPENED");
                assertThat(itr.getSize(), is(1L));
                final Node columnNode = itr.nextNode();
                verifyProperty(columnNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.TIMESTAMP.toDdl());
                verifyProperty(columnNode, StandardDdlLexicon.NULLABLE, "NOT NULL");
                verifyProperty(columnNode, StandardDdlLexicon.DEFAULT_VALUE, "CURRENT_TIMESTAMP");
            }

            { // DATECLOSED column
                final NodeIterator itr = tableNode.getNodes("DATECLOSED");
                assertThat(itr.getSize(), is(1L));
                final Node columnNode = itr.nextNode();
                verifyProperty(columnNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.TIMESTAMP.toDdl());
                verifyProperty(columnNode, StandardDdlLexicon.NULLABLE, "NOT NULL");
                verifyProperty(columnNode, StandardDdlLexicon.DEFAULT_VALUE, "0000-00-00 00:00:00");
            }

            { // NAMEINSOURCE option
                final Node optionNode = tableNode.getNode("NAMEINSOURCE");
                verifyMixinType(optionNode, StandardDdlLexicon.TYPE_STATEMENT_OPTION);
                verifyProperty(optionNode, StandardDdlLexicon.VALUE, "`accounts`.`ACCOUNT`");
            }

            { // NATIVE_TYPE option
                final Node optionNode = tableNode.getNode("UPDATABLE");
                verifyMixinType(optionNode, StandardDdlLexicon.TYPE_STATEMENT_OPTION);
                verifyProperty(optionNode, StandardDdlLexicon.VALUE, "TRUE");
            }
        }

        { // accounts.CUSTOMER
            final Node tableNode = statementsNode.getNode("accounts.CUSTOMER");
            verifyMixinType(tableNode, TeiidDdlLexicon.CreateTable.TABLE_STATEMENT);
            verifyProperty(tableNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        }

        { // accounts.HOLDINGS
            final Node tableNode = statementsNode.getNode("accounts.HOLDINGS");
            verifyMixinType(tableNode, TeiidDdlLexicon.CreateTable.TABLE_STATEMENT);
            verifyProperty(tableNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        }

        { // accounts.PRODUCT
            final Node tableNode = statementsNode.getNode("accounts.PRODUCT");
            verifyMixinType(tableNode, TeiidDdlLexicon.CreateTable.TABLE_STATEMENT);
            verifyProperty(tableNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        }

        { // accounts.SUBSCRIPTIONS
            final Node tableNode = statementsNode.getNode("accounts.SUBSCRIPTIONS");
            verifyMixinType(tableNode, TeiidDdlLexicon.CreateTable.TABLE_STATEMENT);
            verifyProperty(tableNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        }
    }

    @Test
    @SkipLongRunning
    public void shouldSequenceGreenPlumDdl() throws Exception {
        // this DDL has column with type of OBJECT with a length
        this.statementsNode = sequenceDdl("ddl/dialect/teiid/GreenPlum.ddl");
        assertThat(this.statementsNode.getNodes().getSize(), is(410L));

        // make sure column with type of object has a length
        final Node tableNode = this.statementsNode.getNode("gp_toolkit.gp_log_command_timings");
        final Node columnNode = tableNode.getNode("logduration");
        verifyProperty(columnNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.OBJECT.toDdl());
        verifyProperty(columnNode, StandardDdlLexicon.DATATYPE_LENGTH, 49L);
    }

    @Test
    public void shouldSequenceProductsDdl() throws Exception {
        // this DDL has names that contains a ':'
        this.statementsNode = sequenceDdl("ddl/dialect/teiid/products.ddl");
        assertThat(this.statementsNode.getNodes().getSize(), is(6L));

        // table
        final Node tableNode = statementsNode.getNode(this.session.encode("Products.product:info"));
        verifyMixinType(tableNode, TeiidDdlLexicon.CreateTable.TABLE_STATEMENT);
        verifyProperty(tableNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());

        { // column
            final NodeIterator itr = tableNode.getNodes(this.session.encode("PRODUCT:ID"));
            assertThat(itr.getSize(), is(1L));
            final Node columnNode = itr.nextNode();
            verifyProperty(columnNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.BIGDECIMAL.toDdl());
            verifyProperty(columnNode, StandardDdlLexicon.NULLABLE, "NOT NULL");

            { // columnOption option
                final Node optionNode = columnNode.getNode("NAMEINSOURCE");
                verifyMixinType(optionNode, StandardDdlLexicon.TYPE_STATEMENT_OPTION);
                verifyProperty(optionNode, StandardDdlLexicon.VALUE, "\"PRODUCT:ID\"");
            }
        }

        { // table option
            final Node optionNode = tableNode.getNode("NAMEINSOURCE");
            verifyMixinType(optionNode, StandardDdlLexicon.TYPE_STATEMENT_OPTION);
            verifyProperty(optionNode, StandardDdlLexicon.VALUE, "\"Products\".\"product:info\"");
        }
    }

}
