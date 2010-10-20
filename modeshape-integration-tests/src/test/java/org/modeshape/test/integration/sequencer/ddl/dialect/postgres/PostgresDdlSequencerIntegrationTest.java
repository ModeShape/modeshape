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
package org.modeshape.test.integration.sequencer.ddl.dialect.postgres;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrSecurityContextCredentials;
import org.modeshape.jcr.JcrTools;
import org.modeshape.sequencer.ddl.StandardDdlLexicon;
import org.modeshape.sequencer.ddl.dialect.postgres.PostgresDdlLexicon;
import org.modeshape.test.integration.sequencer.ddl.DdlIntegrationTestUtil;

/**
 *
 */
public class PostgresDdlSequencerIntegrationTest extends DdlIntegrationTestUtil {
    private String resourceFolder = ddlTestResourceRootFolder + "/dialect/postgres/";

    @Before
    public void beforeEach() throws Exception {
        // Configure the ModeShape configuration. This could be done by loading a configuration from a file, or by
        // using a (local or remote) configuration repository, or by setting up the configuration programmatically.
        // This test uses the programmatic approach...

        tools = new JcrTools();

        String repositoryName = "ddlRepository";
        String workspaceName = "default";
        String repositorySource = "ddlRepositorySource";

        JcrConfiguration config = new JcrConfiguration();
        // Set up the in-memory source where we'll upload the content and where the sequenced output will be stored ...
        config.repositorySource(repositorySource)
              .usingClass(InMemoryRepositorySource.class)
              .setDescription("The repository for our content")
              .setProperty("defaultWorkspaceName", workspaceName);
        // Set up the JCR repository to use the source ...
        config.repository(repositoryName)
              .addNodeTypes(getUrl("org/modeshape/sequencer/ddl/StandardDdl.cnd"))
              .addNodeTypes(getUrl(resourceFolder + "PostgresDdl.cnd"))
              .registerNamespace(StandardDdlLexicon.Namespace.PREFIX, StandardDdlLexicon.Namespace.URI)
              .registerNamespace(PostgresDdlLexicon.Namespace.PREFIX, PostgresDdlLexicon.Namespace.URI)
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
    public void shouldSequencePostgresDdlFile() throws Exception {

        uploadFile(resourceFolder, "postgres_test_statements.ddl", "shouldSequencePostgresDdlFile");

        waitUntilSequencedNodesIs(1);

        // Find the node ...
        Node root = session.getRootNode();

        if (root.hasNode("ddls")) {
            Node ddlsNode = root.getNode("ddls/a/b");
            // System.out.println("   | NAME: " + ddlsNode.getName() + "  PATH: " + ddlsNode.getPath());
            for (NodeIterator iter = ddlsNode.getNodes(); iter.hasNext();) {
                Node ddlNode = iter.nextNode();

                long numStatements = ddlNode.getNodes().nextNode().getNodes().getSize();
                assertEquals(numStatements, 106);

                // printNodeProperties(ddlNode);

                verifyNodeType(ddlNode, "increment", "postgresddl:createFunctionStatement");
                verifyNode(ddlNode, "increment", "ddl:expression");
                verifyNodeType(ddlNode, "increment", "ddl:creatable");
                verifyNodeType(ddlNode, "increment", "postgresddl:functionOperand");
                verifyNode(ddlNode, "increment", "ddl:startLineNumber", 214);
                verifyNode(ddlNode, "increment", "ddl:startCharIndex", 7604);

                // COMMENT ON FUNCTION my_function (timestamp) IS ’Returns Roman Numeral’;
                verifyNodeType(ddlNode, "my_function", "postgresddl:commentOnStatement");
                verifyNode(ddlNode, "my_function", "ddl:expression");
                verifyNodeType(ddlNode, "my_function", "postgresddl:commentOperand");
                verifyNode(ddlNode, "my_function", "ddl:startLineNumber", 44);
                verifyNode(ddlNode, "my_function", "ddl:startCharIndex", 1573);
                verifyNode(ddlNode, "my_function", "postgresddl:comment", "'Returns Roman Numeral'");

                // ALTER TABLE foreign_companies RENAME COLUMN address TO city;
                Node alterTableNode = findNode(ddlNode, "foreign_companies", "postgresddl:alterTableStatement");
                assertNotNull(alterTableNode);
                Node renameColNode = findNode(alterTableNode, "address", "postgresddl:renamedColumn");
                assertNotNull(renameColNode);
                verifySingleValueProperty(renameColNode, "ddl:newName", "city");

                // GRANT EXECUTE ON FUNCTION divideByTwo(numerator int, IN demoninator int) TO george;
                Node grantNode = findNode(ddlNode, "divideByTwo", "postgresddl:grantOnFunctionStatement");
                assertNotNull(grantNode);
                Node parameter_1 = findNode(grantNode, "numerator", "postgresddl:functionParameter");
                assertNotNull(parameter_1);
                verifySingleValueProperty(parameter_1, "ddl:datatypeName", "int");
            }
        }
    }
}
