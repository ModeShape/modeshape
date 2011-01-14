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
package org.modeshape.test.integration.sequencer.ddl;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.text.ParsingException;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrSecurityContextCredentials;
import org.modeshape.jcr.JcrTools;
import org.modeshape.sequencer.ddl.DdlParserScorer;
import org.modeshape.sequencer.ddl.StandardDdlLexicon;
import org.modeshape.sequencer.ddl.StandardDdlParser;
import org.modeshape.sequencer.ddl.node.AstNode;

/**
 *
 */
public class DdlSequencerIntegrationTest extends DdlIntegrationTestUtil {
    private JcrConfiguration config;
    private String repositoryName;
    private String workspaceName;

    @Before
    public void beforeEach() throws Exception {
        print = false;
        tools = new JcrTools();

        // Configure the ModeShape configuration. This could be done by loading a configuration from a file, or by
        // using a (local or remote) configuration repository, or by setting up the configuration programmatically.
        // This test uses the programmatic approach...

        repositoryName = "ddlRepository";
        workspaceName = "default";
        String repositorySource = "ddlRepositorySource";

        config = new JcrConfiguration();
        // Set up the in-memory source where we'll upload the content and where the sequenced output will be stored ...
        config.repositorySource(repositorySource)
              .usingClass(InMemoryRepositorySource.class)
              .setDescription("The repository for our content")
              .setProperty("defaultWorkspaceName", workspaceName);
        // Set up the JCR repository to use the source ...
        config.repository(repositoryName)
              .addNodeTypes(getUrl("org/modeshape/sequencer/ddl/StandardDdl.cnd"))
              .registerNamespace(StandardDdlLexicon.Namespace.PREFIX, StandardDdlLexicon.Namespace.URI)
              .setSource(repositorySource);
        // Set up the DDL sequencer ...
        config.sequencer("DDL Sequencer")
              .usingClass("org.modeshape.sequencer.ddl.DdlSequencer")
              .loadedFromClasspath()
              .setDescription("Sequences DDL files to extract individual statements and accompanying statement properties and values")
              .sequencingFrom("(//(*.(ddl)[*]))/jcr:content[@jcr:data]")
              .andOutputtingTo("/ddls/$1");
        config.save();
        this.engine = config.build();
        this.engine.start();

        this.session = this.engine.getRepository(repositoryName)
                                  .login(new JcrSecurityContextCredentials(new MyCustomSecurityContext()), workspaceName);

    }

    @After
    public void afterEach() throws Exception {
        if (this.session != null) {
            this.session.logout();
        }
        if (this.engine != null) {
            this.engine.shutdown();
        }
    }

    @Test
    public void shouldSequenceCreateSchemaDdlFile() throws Exception {

        uploadFile(ddlTestResourceRootFolder, "create_schema.ddl", "shouldSequenceCreateSchemaDdlFile");

        waitUntilSequencedNodesIs(1);
        // print = true;

        // Find the node ...
        Node root = session.getRootNode();

        if (root.hasNode("ddls")) {
            if (root.hasNode("ddls")) {
                Node ddlsNode = root.getNode("ddls");
                printSubgraph(ddlsNode);
                // System.out.println("   | NAME: " + ddlsNode.getName() + "  PATH: " + ddlsNode.getPath());
                for (NodeIterator iter = ddlsNode.getNodes(); iter.hasNext();) {
                    Node ddlNode = iter.nextNode();

                    // printNodeProperties(ddlNode);

                    verifyNode(ddlNode, "hollywood", "ddl:startLineNumber");
                    verifyNode(ddlNode, "winners", "ddl:expression");
                    verifyNode(ddlNode, "title", "ddl:datatypeLength");
                }
            }
        }

    }

    @Test
    public void shouldSequenceStandardDdlFile() throws Exception {
        uploadFile(ddlTestResourceRootFolder, "standard_test_statements.ddl", "shouldSequenceStandardDdlFile");

        waitUntilSequencedNodesIs(1);

        // Find the node ...
        Node root = session.getRootNode();

        if (root.hasNode("ddls")) {
            Node ddlsNode = root.getNode("ddls/a/b");
            // System.out.println("   | NAME: " + ddlsNode.getName() + "  PATH: " + ddlsNode.getPath());
            for (NodeIterator iter = ddlsNode.getNodes(); iter.hasNext();) {
                Node ddlNode = iter.nextNode();

                // printPropertiesRecursive(ddlNode);

                // printChildProperties(ddlNode.getNodes().nextNode());

                long numStatements = ddlNode.getNodes().nextNode().getNodes().getSize();
                assertEquals(44, numStatements);

                // assertNotNull <ns001:startLineNumber = 40, jcr:primaryType = nt:unstructured, ns001:startColumnNumber = 1,
                // jcr:mixinTypes = ns001:createAssertionStatement, ns001:expression = CREATE ASSERTION assertNotNull CHECK
                // (value != null) NOT DEFERRABLE, ns001:startCharIndex = 1400, ns001:searchCondition = value ! = null>
                // CONSTRAINT_ATTRIBUTE <jcr:primaryType = nt:unstructured, jcr:mixinTypes = ns001:constraintAttribute,
                // ns001:propValue = NOT DEFERRABLE>
                // assertIsZero <ns001:startLineNumber = 42, jcr:primaryType = nt:unstructured, ns001:startColumnNumber = 1,
                // jcr:mixinTypes = ns001:createAssertionStatement, ns001:expression = CREATE ASSERTION assertIsZero CHECK
                // (value != null and value == 0) INITIALLY DEFERRED, ns001:startCharIndex = 1469, ns001:searchCondition =
                // value ! = null and value = = 0>
                // CONSTRAINT_ATTRIBUTE <jcr:primaryType = nt:unstructured, jcr:mixinTypes = ns001:constraintAttribute,
                // ns001:propValue = INITIALLY DEFERRED>
                Node stmtNode = assertNode(ddlNode, "assertNotNull", "ddl:createAssertionStatement");
                Node constraintAttribute = assertNode(stmtNode, "CONSTRAINT_ATTRIBUTE", "ddl:constraintAttribute");
                verifySingleValueProperty(constraintAttribute, "ddl:propValue", "NOT DEFERRABLE");

                stmtNode = assertNode(ddlNode, "assertIsZero", "ddl:createAssertionStatement");
                constraintAttribute = assertNode(stmtNode, "CONSTRAINT_ATTRIBUTE", "ddl:constraintAttribute");
                verifySingleValueProperty(constraintAttribute, "ddl:propValue", "INITIALLY DEFERRED");
                // employee <ns001:startLineNumber = 40, jcr:primaryType = nt:unstructured, ns001:startColumnNumber = 1,
                // jcr:mixinTypes = ns001:createTableStatement, ns001:expression = CREATE TABLE employee (
                // empno NUMBER(4) NOT NULL,
                // empname CHAR(10),
                // job CHAR(9),
                // deptno NUMBER(2) NOT NULL,
                // CONSTRAINT emp_fk1 FOREIGN KEY (deptno) REFERENCES dept (deptno) INITIALLY IMMEDIATE,
                // CONSTRAINT emp_pk PRIMARY KEY (empno));, ns001:startCharIndex = 1404>
                // empno <ns001:datatypeName = NUMBER, jcr:primaryType = nt:unstructured, ns001:datatypePrecision = 4,
                // ns001:nullable = NOT NULL, jcr:mixinTypes = ns001:columnDefinition, ns001:datatypeScale = 0>
                // empname <ns001:datatypeName = CHAR, ns001:datatypeLength = 10, jcr:primaryType = nt:unstructured,
                // jcr:mixinTypes = ns001:columnDefinition>
                // job <ns001:datatypeName = CHAR, ns001:datatypeLength = 9, jcr:primaryType = nt:unstructured, jcr:mixinTypes
                // = ns001:columnDefinition>
                // deptno <ns001:datatypeName = NUMBER, jcr:primaryType = nt:unstructured, ns001:datatypePrecision = 2,
                // ns001:nullable = NOT NULL, jcr:mixinTypes = ns001:columnDefinition, ns001:datatypeScale = 0>
                // emp_fk1 <jcr:primaryType = nt:unstructured, jcr:mixinTypes = ns001:tableConstraint, ns001:constraintType =
                // 1>
                // deptno <jcr:primaryType = nt:unstructured, jcr:mixinTypes = ns001:columnReference>
                // dept <jcr:primaryType = nt:unstructured, jcr:mixinTypes = ns001:tableReference>
                // deptno <jcr:primaryType = nt:unstructured, jcr:mixinTypes = ns001:fkColumnReference>
                // CONSTRAINT_ATTRIBUTE <jcr:primaryType = nt:unstructured, jcr:mixinTypes = ns001:constraintAttribute,
                // ns001:propValue = INITIALLY IMMEDIATE>
                // emp_pk <jcr:primaryType = nt:unstructured, jcr:mixinTypes = ns001:tableConstraint, ns001:constraintType =
                // 2>
                // empno <jcr:primaryType = nt:unstructured, jcr:mixinTypes = ns001:columnReference>

                Node tbleNode = assertNode(ddlNode, "employee", "ddl:createTableStatement");
                Node columnNode = assertNode(ddlNode, "empname", "ddl:columnDefinition");
                verifySingleValueProperty(columnNode, "ddl:datatypeName", "CHAR");
                verifySingleValueProperty(columnNode, "ddl:datatypeLength", 10);
                Node constraintNode = assertNode(tbleNode, "emp_fk1", "ddl:tableConstraint");
                constraintAttribute = assertNode(constraintNode, "CONSTRAINT_ATTRIBUTE", "ddl:constraintAttribute");
                verifySingleValueProperty(constraintAttribute, "ddl:propValue", "INITIALLY IMMEDIATE");
                assertNode(constraintNode, "deptno", "ddl:columnReference");
                assertNode(constraintNode, "dept", "ddl:tableReference");
                assertNode(constraintNode, "deptno", "ddl:fkColumnReference");

                Node viewNode = assertNode(ddlNode, "view_1", "ddl:createViewStatement");
                assertNode(viewNode, "col1", "ddl:columnReference");

                Node tableNode = assertNode(ddlNode, "table_5", "ddl:createTableStatement");
                // printChildProperties(tableNode);
                assertEquals(18, tableNode.getNodes().getSize());
            }
        }
    }

    @Test
    public void shouldSequenceStandardDdlGrantStatements() throws Exception {

        uploadFile(ddlTestResourceRootFolder, "grant_test_statements.ddl", "shouldSequenceStandardDdlGrantStatements");

        waitUntilSequencedNodesIs(1);

        // Find the node ...
        Node root = session.getRootNode();

        if (root.hasNode("ddls")) {
            Node ddlsNode = root.getNode("ddls/a/b");
            // System.out.println("   | NAME: " + ddlsNode.getName() + "  PATH: " + ddlsNode.getPath());
            for (NodeIterator iter = ddlsNode.getNodes(); iter.hasNext();) {
                Node ddlNode = iter.nextNode();

                // printNodeProperties(ddlNode);

                long numStatements = ddlNode.getNodes().nextNode().getNodes().getSize();
                assertEquals(numStatements, 4);

                // GRANT SELECT ON TABLE purchaseOrders TO maria,harry;
                Node grantNode = assertNode(ddlNode, "purchaseOrders", "ddl:grantOnTableStatement");
                assertNode(grantNode, "maria", "ddl:grantee");
                Node privNode = assertNode(grantNode, "privilege", "ddl:grantPrivilege");
                verifySingleValueProperty(privNode, "ddl:type", "SELECT");

                // GRANT UPDATE, USAGE ON TABLE purchaseOrders FROM anita,zhi;
                grantNode = assertNode(ddlNode, "billedOrders", "ddl:grantOnTableStatement");
                privNode = assertNode(grantNode, "privilege", "ddl:grantPrivilege");
                verifySingleValueProperty(privNode, "ddl:type", "UPDATE");
                assertNode(grantNode, "anita", "ddl:grantee");
            }
        }
    }

    @Test
    public void shouldSequenceStandardDdlRevokeStatements() throws Exception {

        uploadFile(ddlTestResourceRootFolder, "revoke_test_statements.ddl", "shouldSequenceStandardDdlRevokeStatements");

        waitUntilSequencedNodesIs(1);

        // Find the node ...
        Node root = session.getRootNode();

        if (root.hasNode("ddls")) {
            Node ddlsNode = root.getNode("ddls/a/b");
            // System.out.println("   | NAME: " + ddlsNode.getName() + "  PATH: " + ddlsNode.getPath());
            for (NodeIterator iter = ddlsNode.getNodes(); iter.hasNext();) {
                Node ddlNode = iter.nextNode();

                // printNodeProperties(ddlNode);

                long numStatements = ddlNode.getNodes().nextNode().getNodes().getSize();
                assertEquals(numStatements, 4);

                // REVOKE SELECT ON TABLE purchaseOrders FROM maria,harry;
                Node revokeNode = assertNode(ddlNode, "purchaseOrders", "ddl:revokeOnTableStatement");
                assertNode(revokeNode, "maria", "ddl:grantee");
                Node privNode = assertNode(revokeNode, "privilege", "ddl:grantPrivilege");
                verifySingleValueProperty(privNode, "ddl:type", "SELECT");

                // REVOKE UPDATE, USAGE ON TABLE purchaseOrders FROM anita,zhi CASCADE;
                revokeNode = assertNode(ddlNode, "orderDetails", "ddl:revokeOnTableStatement");
                privNode = assertNode(revokeNode, "privilege", "ddl:grantPrivilege");
                verifySingleValueProperty(privNode, "ddl:type", "UPDATE");
                assertNode(revokeNode, "anita", "ddl:grantee");
            }
        }
    }

    @Test
    public void shouldSequenceStandardAlterTableStatements() throws Exception {

        uploadFile(ddlTestResourceRootFolder, "alter_table_statements.ddl", "shouldSequenceStandardAlterTableStatements");

        waitUntilSequencedNodesIs(1);

        // Find the node ...
        Node root = session.getRootNode();

        if (root.hasNode("ddls")) {
            Node ddlsNode = root.getNode("ddls/a/b");
            // System.out.println("   | NAME: " + ddlsNode.getName() + "  PATH: " + ddlsNode.getPath());
            for (NodeIterator iter = ddlsNode.getNodes(); iter.hasNext();) {
                Node ddlNode = iter.nextNode();

                // printNodeProperties(ddlNode);

                long numStatements = ddlNode.getNodes().nextNode().getNodes().getSize();
                assertEquals(numStatements, 14);

                // ALTER TABLE table_name_1 ADD COLUMN column_name VARCHAR(25) NOT NULL;
                Node alterNode = assertNode(ddlNode, "table_name_1", "ddl:alterTableStatement");
                assertEquals(alterNode.getNodes().getSize(), 1);
                Node addColumnNode = assertNode(alterNode, "column_name", "ddl:columnDefinition");
                verifySingleValueProperty(addColumnNode, "ddl:datatypeName", "VARCHAR");
                verifySingleValueProperty(addColumnNode, "ddl:datatypeLength", 25);
                verifySingleValueProperty(addColumnNode, "ddl:nullable", "NOT NULL");

                // ALTER TABLE schema_2.table_name_2 ADD schema_2.table_name_2.column_name INTEGER NOT NULL DEFAULT (25);
                alterNode = assertNode(ddlNode, "schema_2.table_name_2", "ddl:alterTableStatement");
                assertEquals(alterNode.getNodes().getSize(), 1);
                addColumnNode = assertNode(alterNode, "schema_2.table_name_2.column_name", "ddl:columnDefinition");
                verifySingleValueProperty(addColumnNode, "ddl:datatypeName", "INTEGER");
                verifySingleValueProperty(addColumnNode, "ddl:nullable", "NOT NULL");
                verifySingleValueProperty(addColumnNode, "ddl:defaultValue", "25");

                // ALTER TABLE table_name_4 ALTER COLUMN column_name SET DEFAULT (0);
                alterNode = assertNode(ddlNode, "table_name_4", "ddl:alterTableStatement");
                assertEquals(alterNode.getNodes().getSize(), 1);
                addColumnNode = assertNode(alterNode, "column_name", "ddl:alterColumnDefinition");
                verifySingleValueProperty(addColumnNode, "ddl:defaultValue", "0");

                // ALTER TABLE table_name_7 DROP COLUMN column_name RESTRICT;
                alterNode = assertNode(ddlNode, "table_name_7", "ddl:alterTableStatement");
                assertEquals(alterNode.getNodes().getSize(), 1);
                Node dropColumnNode = assertNode(alterNode, "column_name", "ddl:dropColumnDefinition");
                verifySingleValueProperty(dropColumnNode, "ddl:dropBehavior", "RESTRICT");

                // ALTER TABLE table_name_10 ADD CONSTRAINT pk_name PRIMARY KEY (column_name);
                alterNode = assertNode(ddlNode, "table_name_10", "ddl:alterTableStatement");
                assertEquals(alterNode.getNodes().getSize(), 1);
                Node constraintNode = assertNode(alterNode, "pk_name", "ddl:addTableConstraintDefinition");
                verifySingleValueProperty(constraintNode, "ddl:constraintType", "PRIMARY KEY");
                assertNode(constraintNode, "column_name", "ddl:columnReference");

                // ALTER TABLE table_name_14 DROP CONSTRAINT fk_name RESTRICT;
                alterNode = assertNode(ddlNode, "table_name_14", "ddl:alterTableStatement");
                assertEquals(alterNode.getNodes().getSize(), 1);
                Node dropConstraintNode = assertNode(alterNode, "fk_name", "ddl:dropTableConstraintDefinition");
                verifySingleValueProperty(dropConstraintNode, "ddl:dropBehavior", "RESTRICT");
            }
        }
    }

    @Test
    public void shouldParseUsingCustomGrammarIfGrammarsPropertyIsSetOnDdlSequencerConfiguration() throws Exception {
        config.sequencer("DDL Sequencer").setProperty("grammars", ArgleDdlParser.class.getName(), "oracle", "standard");
        config.save();
        this.engine = config.build();
        this.engine.start();

        this.session = this.engine.getRepository(repositoryName)
                                  .login(new JcrSecurityContextCredentials(new MyCustomSecurityContext()), workspaceName);

        ArgleDdlParser.parsed = 0;
        ArgleDdlParser.scored = 0;
        uploadFile(ddlTestResourceRootFolder, "create_schema.ddl", "shouldSequenceCreateSchemaDdlFile");
        waitUntilSequencedNodesIs(1);

        // Scored but not parsed ...
        assertThat(ArgleDdlParser.scored, is(1));
        assertThat(ArgleDdlParser.parsed, is(0));
    }

    @FixFor( "MODE-909" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithOrderBy() throws Exception {
        this.engine = config.build();
        this.engine.start();

        this.session = this.engine.getRepository(repositoryName)
                                  .login(new JcrSecurityContextCredentials(new MyCustomSecurityContext()), workspaceName);
        // Upload a file ...
        uploadFile(ddlTestResourceRootFolder, "create_schema.ddl", "shouldBeAbleToCreateAndExecuteJcrSql2QueryWithOrderBy");
        waitUntilSequencedNodesIs(1);
        Thread.sleep(500);

        // Now query for content ...
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT [jcr:primaryType] from [nt:base] ORDER BY [jcr:primaryType]", Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        // System.out.println(result);
        assertThat(result, is(notNullValue()));
        assertThat(result.getRows().getSize(), is(16L));
        RowIterator iter = result.getRows();
        String primaryType = "";
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            String nextPrimaryType = row.getValues()[0].getString();
            assertThat(nextPrimaryType.compareTo(primaryType) >= 0, is(true));
            primaryType = nextPrimaryType;
        }
    }

    public static class ArgleDdlParser extends StandardDdlParser {
        protected static int scored = 0;
        protected static int parsed = 0;

        public ArgleDdlParser() {
        }

        @Override
        public String getId() {
            return "ARGLE";
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.sequencer.ddl.StandardDdlParser#score(java.lang.String, java.lang.String,
         *      org.modeshape.sequencer.ddl.DdlParserScorer)
         */
        @Override
        public Object score( String ddl,
                             String fileName,
                             DdlParserScorer scorer ) throws ParsingException {
            ++scored;
            return new Object();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.sequencer.ddl.StandardDdlParser#parse(java.lang.String, org.modeshape.sequencer.ddl.node.AstNode,
         *      java.lang.Object)
         */
        @Override
        public void parse( String ddl,
                           AstNode rootNode,
                           Object scoreReturnObject ) throws ParsingException {
            ++parsed;
        }
    }
}
