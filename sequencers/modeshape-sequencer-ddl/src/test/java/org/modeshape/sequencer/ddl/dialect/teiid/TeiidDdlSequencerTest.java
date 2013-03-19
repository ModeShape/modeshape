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
import org.modeshape.sequencer.ddl.AbstractDdlSequencerTest;
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
    public void shouldSequenceFlatFileDdl() throws Exception {
        this.statementsNode = sequenceDdl("ddl/dialect/teiid/flatFile.ddl", 300);
        assertThat(this.statementsNode.getNodes().getSize(), is(3L));

        { // getFiles
            final Node procedureNode = this.statementsNode.getNode("getFiles");
            verifyMixinType(procedureNode, TeiidDdlLexicon.CreateProcedure.PROCEDURE_NODE_TYPE);
            verifyProperty(procedureNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
            assertThat(procedureNode.getNodes().getSize(), is(3L)); // param, returns, option

            { // pathAndPattern parameter
                final Node paramNode = procedureNode.getNode("pathAndPattern");
                verifyMixinType(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_NODE_TYPE);
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
                verifyMixinType(resultNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMNS_NODE_TYPE);
                verifyProperty(resultNode, TeiidDdlLexicon.CreateProcedure.TABLE_FLAG, "true");
                assertThat(resultNode.getNodes().getSize(), is(2L)); // result columns

                { // file result column
                    final Node resultColumnNode = resultNode.getNode("file");
                    verifyMixinType(resultColumnNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMN_NODE_TYPE);
                    verifyProperty(resultColumnNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.BLOB.toDdl());
                }

                { // filePath result column
                    final Node resultColumnNode = resultNode.getNode("filePath");
                    verifyMixinType(resultColumnNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMN_NODE_TYPE);
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
            verifyMixinType(procedureNode, TeiidDdlLexicon.CreateProcedure.PROCEDURE_NODE_TYPE);
            verifyProperty(procedureNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
            assertThat(procedureNode.getNodes().getSize(), is(3L)); // param, returns, option

            { // pathAndPattern parameter
                final Node paramNode = procedureNode.getNode("pathAndPattern");
                verifyMixinType(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_NODE_TYPE);
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
                verifyMixinType(resultNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMNS_NODE_TYPE);
                verifyProperty(resultNode, TeiidDdlLexicon.CreateProcedure.TABLE_FLAG, "true");
                assertThat(resultNode.getNodes().getSize(), is(2L)); // result columns

                { // file result column
                    final Node resultColumnNode = resultNode.getNode("file");
                    verifyMixinType(resultColumnNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMN_NODE_TYPE);
                    verifyProperty(resultColumnNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.CLOB.toDdl());
                }

                { // filePath result column
                    final Node resultColumnNode = resultNode.getNode("filePath");
                    verifyMixinType(resultColumnNode, TeiidDdlLexicon.CreateProcedure.RESULT_COLUMN_NODE_TYPE);
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
            verifyMixinType(procedureNode, TeiidDdlLexicon.CreateProcedure.PROCEDURE_NODE_TYPE);
            verifyProperty(procedureNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
            assertThat(procedureNode.getNodes().getSize(), is(3L)); // 2 params, 1 option

            { // filePath parameter
                final Node paramNode = procedureNode.getNode("filePath");
                verifyMixinType(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_NODE_TYPE);
                verifyProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, "IN");
                verifyProperty(paramNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.STRING.toDdl());
                verifyProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, "false");
                assertThat(paramNode.getNodes().getSize(), is(0L));
            }

            { // file parameter
                final Node paramNode = procedureNode.getNode("file");
                verifyMixinType(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_NODE_TYPE);
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
            verifyMixinType(procedureNode, TeiidDdlLexicon.CreateProcedure.PROCEDURE_NODE_TYPE);
            verifyProperty(procedureNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
            assertThat(procedureNode.getNodes().getSize(), is(7L)); // 6 params, 1 option

            { // result parameter
                final Node paramNode = procedureNode.getNode("result");
                verifyMixinType(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_NODE_TYPE);
                verifyProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_TYPE, "OUT");
                verifyProperty(paramNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.XML.toDdl());
                verifyProperty(paramNode, TeiidDdlLexicon.CreateProcedure.PARAMETER_RESULT_FLAG, "true");
                assertThat(paramNode.getNodes().getSize(), is(0L));
            }
        }

        { // invokeHttp
            final Node procedureNode = this.statementsNode.getNode("invokeHttp");
            verifyMixinType(procedureNode, TeiidDdlLexicon.CreateProcedure.PROCEDURE_NODE_TYPE);
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
    public void shouldSequenceAccountsDdl() throws Exception {
        this.statementsNode = sequenceDdl("ddl/dialect/teiid/accounts.ddl");
        assertThat(this.statementsNode.getNodes().getSize(), is(5L));

        { // accounts.ACCOUNT
            final Node tableNode = statementsNode.getNode("accounts.ACCOUNT");
            verifyMixinType(tableNode, TeiidDdlLexicon.CreateTable.TABLE_NODE_TYPE);
            verifyProperty(tableNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());

            { // ACCOUNT_ID column
                final NodeIterator itr = tableNode.getNodes("ACCOUNT_ID");
                assertThat(itr.getSize(), is(1L));
                final Node columnNode = itr.nextNode();
                verifyProperty(columnNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.LONG.toDdl());
                verifyProperty(columnNode, TeiidDdlLexicon.CreateTable.CAN_BE_NULL, "false");
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
                verifyProperty(columnNode, TeiidDdlLexicon.CreateTable.CAN_BE_NULL, "false");
                verifyProperty(columnNode, StandardDdlLexicon.DEFAULT_VALUE, "CURRENT_TIMESTAMP");
            }

            { // DATECLOSED column
                final NodeIterator itr = tableNode.getNodes("DATECLOSED");
                assertThat(itr.getSize(), is(1L));
                final Node columnNode = itr.nextNode();
                verifyProperty(columnNode, StandardDdlLexicon.DATATYPE_NAME, TeiidDataType.TIMESTAMP.toDdl());
                verifyProperty(columnNode, TeiidDdlLexicon.CreateTable.CAN_BE_NULL, "false");
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
            verifyMixinType(tableNode, TeiidDdlLexicon.CreateTable.TABLE_NODE_TYPE);
            verifyProperty(tableNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        }

        { // accounts.HOLDINGS
            final Node tableNode = statementsNode.getNode("accounts.HOLDINGS");
            verifyMixinType(tableNode, TeiidDdlLexicon.CreateTable.TABLE_NODE_TYPE);
            verifyProperty(tableNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        }

        { // accounts.PRODUCT
            final Node tableNode = statementsNode.getNode("accounts.PRODUCT");
            verifyMixinType(tableNode, TeiidDdlLexicon.CreateTable.TABLE_NODE_TYPE);
            verifyProperty(tableNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        }

        { // accounts.SUBSCRIPTIONS
            final Node tableNode = statementsNode.getNode("accounts.SUBSCRIPTIONS");
            verifyMixinType(tableNode, TeiidDdlLexicon.CreateTable.TABLE_NODE_TYPE);
            verifyProperty(tableNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
        }
    }

}
