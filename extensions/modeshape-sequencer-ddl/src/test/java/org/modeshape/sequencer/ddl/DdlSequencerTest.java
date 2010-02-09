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
package org.modeshape.sequencer.ddl;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.SubgraphNode;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.io.GraphSequencerOutput;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.ValueFormatException;
import org.modeshape.graph.sequencer.MockSequencerContext;
import org.modeshape.graph.sequencer.StreamSequencerContext;
import org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon;
import org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon;
import org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlLexicon;
import org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlParser;
import org.modeshape.sequencer.ddl.dialect.postgres.PostgresDdlLexicon;

/**
 *
 */
public class DdlSequencerTest {
    private DdlSequencer sequencer;
    private InputStream content;
    private GraphSequencerOutput output;
    private StreamSequencerContext context;
    private Graph graph;

    // private Graph expectedGraph;

    @Before
    public void beforeEach() {
        context = new MockSequencerContext("/a/myDdlSequencer.cnd");
        context.getNamespaceRegistry().register("jcr", "http://www.jcp.org/jcr/1.0");
        context.getNamespaceRegistry().register("nt", "http://www.jcp.org/jcr/nt/1.0");
        context.getNamespaceRegistry().register("mix", "http://www.jcp.org/jcr/mix/1.0");
        context.getNamespaceRegistry().register(StandardDdlLexicon.Namespace.PREFIX, StandardDdlLexicon.Namespace.URI);
        context.getNamespaceRegistry().register(DerbyDdlLexicon.Namespace.PREFIX, DerbyDdlLexicon.Namespace.URI);
        context.getNamespaceRegistry().register(OracleDdlLexicon.Namespace.PREFIX, OracleDdlLexicon.Namespace.URI);
        context.getNamespaceRegistry().register(PostgresDdlLexicon.Namespace.PREFIX, PostgresDdlLexicon.Namespace.URI);
        context.getNamespaceRegistry().register(MySqlDdlLexicon.Namespace.PREFIX, MySqlDdlLexicon.Namespace.URI);

        InMemoryRepositorySource source = new InMemoryRepositorySource();
        source.setName("actual");
        graph = Graph.create(source, context);

        output = new GraphSequencerOutput(graph);

        sequencer = new DdlSequencer();
    }

    @After
    public void afterEach() throws Exception {
        if (content != null) {
            try {
                content.close();
            } finally {
                content = null;
            }
        }
    }

    /**
     * Utility to create a {@link Path.Segment} from a string, where there will be no index
     * 
     * @param name the string form of the path segment, which may include a 1-based same-name-sibling index
     * @return the path object
     * @throws ValueFormatException if a path could not be created from the supplied string
     */
    protected Path.Segment segment( String name ) {
        return context.getValueFactories().getPathFactory().createSegment(name);
    }

    /**
     * Utility to create a {@link Name} from a string.
     * 
     * @param name the string form of the name
     * @return the name object
     * @throws ValueFormatException if a name could not be created from the supplied string
     */
    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    /**
     * Utility to create a {@link Path} from a string.
     * 
     * @param path the string form of the path
     * @return the path object
     * @throws ValueFormatException if a path could not be created from the supplied string
     */
    protected Path path( String path ) {
        return context.getValueFactories().getPathFactory().create(path);
    }

    /**
     * Utility to create a string value from an {@link Object}
     * 
     * @param value the value object
     * @return the string value of the object
     * @throws ValueFormatException if a path could not be created from the supplied string
     */
    protected String value( Object value ) {
        return context.getValueFactories().getStringFactory().create(value);
    }

    private boolean verifyProperty( SubgraphNode node,
                                    String propNameStr,
                                    String expectedValue ) {
        Name propName = name(propNameStr);
        for (Property prop : node.getProperties()) {
            if (prop.getName().equals(propName)) {
                for (Object nextVal : prop.getValuesAsArray()) {
                    String valueStr = value(nextVal);
                    if (valueStr.equals(expectedValue)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean verifyHasProperty( SubgraphNode node,
                                       String propNameStr ) {
        Name propName = name(propNameStr);
        for (Property prop : node.getProperties()) {
            if (prop.getName().equals(propName)) {
                return true;
            }
        }
        return false;
    }

    private boolean verifyPrimaryType( SubgraphNode node,
                                       String expectedValue ) {
        return verifyProperty(node, "jcr:primaryType", expectedValue);
    }

    private boolean verifyMixinType( SubgraphNode node,
                                     String expectedValue ) {
        return verifyProperty(node, "jcr:mixinTypes", expectedValue);
    }

    private boolean verifyExpression( SubgraphNode node,
                                      String expectedValue ) {
        return verifyProperty(node, "ddl:expression", expectedValue);
    }

    private void verifyBaseProperties( SubgraphNode node,
                                       String primaryType,
                                       String lineNum,
                                       String colNum,
                                       String charIndex,
                                       int numChildren ) {
        assertThat(verifyPrimaryType(node, primaryType), is(true));
        assertThat(verifyProperty(node, "ddl:startLineNumber", lineNum), is(true));
        assertThat(verifyProperty(node, "ddl:startColumnNumber", colNum), is(true));
        assertThat(verifyProperty(node, "ddl:startCharIndex", charIndex), is(true));
        assertThat(node.getChildren().size(), is(numChildren));
    }

    @Test
    public void shouldSequenceCreateSchema() throws IOException {
        // CREATE SCHEMA hollywood
        // CREATE TABLE films (title varchar(255), release date, producerName varchar(255))
        // CREATE VIEW winners AS
        // SELECT title, release FROM films WHERE producerName IS NOT NULL;

        // Subgraph
        // <name = "/" uuid = "0a3501e8-a6ec-4a0e-89b7-9cf64476b18b">
        // <name = "statements" primaryType = "nt:unstructured" uuid = "56f305d6-aa76-4b1a-8e06-bd0c2981c2ff" parserId =
        // "POSTGRES">
        // <name = "hollywood" startLineNumber = "1" primaryType = "nt:unstructured" uuid = "88d2901d-eeaa-4bd6-80c4-243ea56f7c20"
        // startColumnNumber = "1" mixinTypes = "ddl:createSchemaStatement" expression = "CREATE SCHEMA hollywood" startCharIndex
        // = "0">
        // <name = "films" startLineNumber = "2" primaryType = "nt:unstructured" uuid = "8ff9e0a1-ad32-48b0-8a76-011fd6a35142"
        // startColumnNumber = "5" mixinTypes = "ddl:createTableStatement" expression =
        // "CREATE TABLE films (title varchar(255), release date, producerName varchar(255))" startCharIndex = "28">
        // <name = "title" datatypeName = "VARCHAR" datatypeLength = "255" primaryType = "nt:unstructured" uuid =
        // "c1a65f89-2ed4-4a39-858d-b4fd06c60879" mixinTypes = "ddl:columnDefinition">
        // <name = "release" datatypeName = "DATE" primaryType = "nt:unstructured" uuid = "67b8ee6d-341e-4e7e-bb79-bdec0caa6865"
        // mixinTypes = "ddl:columnDefinition">
        // <name = "producerName" datatypeName = "VARCHAR" datatypeLength = "255" primaryType = "nt:unstructured" uuid =
        // "83deb1a7-8ccb-479c-ab38-acc64be99251" mixinTypes = "ddl:columnDefinition">
        // <name = "winners" startLineNumber = "3" primaryType = "nt:unstructured" uuid = "c8610a1c-be27-4b16-9b0b-fc631dbfd00b"
        // startColumnNumber = "5" mixinTypes = "ddl:createViewStatement" expression =
        // "CREATE VIEW winners AS SELECT title, release FROM films WHERE producerName IS NOT NULL;" queryExpression =
        // " SELECT title, release FROM films WHERE producerName IS NOT NULL" startCharIndex = "113">

        URL url = this.getClass().getClassLoader().getResource("ddl/create_schema.ddl");
        assertThat(url, is(notNullValue()));
        content = url.openStream();
        assertThat(content, is(notNullValue()));
        sequencer.sequence(content, output, context);

        output.close();

        // File expectedFile = new File("src/test/resources/");
        // if ( expectedFile.exists() ) {
        // expectedGraph.importXmlFrom(expectedFile).into("/");

        Subgraph subgraph = graph.getSubgraphOfDepth(10).at("/");
        assertThat(subgraph, is(notNullValue()));
        // String value = subgraph.toString();
        // System.out.println(value);

        SubgraphNode rootNode = subgraph.getNode(".");
        assertThat(rootNode.getChildren().size(), is(1));

        SubgraphNode statementsNode = rootNode.getNode(path("ddl:statements"));
        assertNotNull(statementsNode);
        assertThat(statementsNode.getChildren().size(), is(1));

        assertThat(verifyPrimaryType(statementsNode, "nt:unstructured"), is(true));
        assertThat(verifyProperty(statementsNode, "ddl:parserId", "POSTGRES"), is(true));

        SubgraphNode schemaNode = statementsNode.getNode(path("hollywood"));
        assertNotNull(schemaNode);
        verifyBaseProperties(schemaNode, "nt:unstructured", "1", "1", "0", 2);
        assertThat(verifyMixinType(schemaNode, "ddl:createSchemaStatement"), is(true));
        assertThat(verifyExpression(schemaNode, "CREATE SCHEMA hollywood"), is(true));

        SubgraphNode filmsNode = schemaNode.getNode(path("films"));
        assertNotNull(filmsNode);
        verifyBaseProperties(filmsNode, "nt:unstructured", "2", "5", "28", 3);
        assertThat(verifyMixinType(filmsNode, "ddl:createTableStatement"), is(true));
        assertThat(verifyExpression(filmsNode, "CREATE TABLE films (title varchar(255), release date, producerName varchar(255))"),
                   is(true));

        SubgraphNode winnersNode = schemaNode.getNode(path("winners"));
        assertNotNull(winnersNode);
        verifyBaseProperties(winnersNode, "nt:unstructured", "3", "5", "113", 0);
        assertThat(verifyMixinType(winnersNode, "ddl:createViewStatement"), is(true));
        assertThat(verifyExpression(winnersNode,
                                    "CREATE VIEW winners AS SELECT title, release FROM films WHERE producerName IS NOT NULL;"),
                   is(true));

        // Check Column Properties
        SubgraphNode titleNode = filmsNode.getNode(path("title"));
        assertNotNull(titleNode);
        assertThat(verifyPrimaryType(titleNode, "nt:unstructured"), is(true));
        assertThat(verifyProperty(titleNode, "ddl:datatypeName", "VARCHAR"), is(true));
        assertThat(verifyProperty(titleNode, "ddl:datatypeLength", "255"), is(true));
        assertThat(verifyMixinType(titleNode, "ddl:columnDefinition"), is(true));

        SubgraphNode releaseNode = filmsNode.getNode(path("release"));
        assertNotNull(releaseNode);
        assertThat(verifyPrimaryType(releaseNode, "nt:unstructured"), is(true));
        assertThat(verifyProperty(releaseNode, "ddl:datatypeName", "DATE"), is(true));
        assertThat(verifyHasProperty(releaseNode, "ddl:datatypeLength"), is(false));
        assertThat(verifyMixinType(titleNode, "ddl:columnDefinition"), is(true));

        // Map<Property> props = output.getProperties(nodePath)
    }

    @Test
    public void shouldSequenceCreateTable() throws IOException {
        // CREATE TABLE IDTABLE
        // (
        // IDCONTEXT VARCHAR(20) NOT NULL PRIMARY KEY,
        // NEXTID NUMERIC
        // );

        // Subgraph
        // <name = "/" uuid = "8325c96e-e81c-4f16-9c08-33b408e4f7cf">
        // <name = "statements" primaryType = "nt:unstructured" uuid = "ac307790-7378-44fe-8a06-e4292574e2f1" parserId = "SQL92">
        // <name = "IDTABLE" startLineNumber = "1" primaryType = "nt:unstructured" uuid = "cb0e1c03-0815-4b3b-92a0-b95a5980be53"
        // startColumnNumber = "1" mixinTypes = "ddl:createTableStatement" expression = "CREATE TABLE IDTABLE
        // (
        // IDCONTEXT VARCHAR(20) NOT NULL PRIMARY KEY,
        // NEXTID NUMERIC
        // );" startCharIndex = "0">
        // <name = "IDCONTEXT" datatypeName = "VARCHAR" datatypeLength = "20" primaryType = "nt:unstructured" uuid =
        // "ab269004-852a-4731-9886-9ec8dff230b9" nullable = "NOT NULL" mixinTypes = "ddl:columnDefinition">
        // <name = "PK_1" primaryType = "nt:unstructured" uuid = "7395e24e-db3d-412d-91fe-d479258b8641" mixinTypes =
        // "ddl:tableConstraint" constraintType = "2">
        // <name = "IDCONTEXT" primaryType = "nt:unstructured" uuid = "43f052c7-fbf9-4243-a995-86092c367177" mixinTypes =
        // "ddl:columnReference">
        // <name = "NEXTID" datatypeName = "NUMERIC" primaryType = "nt:unstructured" uuid = "91dc78bc-0c43-4f80-9579-4010e31a7ae3"
        // datatypePrecision = "0" mixinTypes = "ddl:columnDefinition" datatypeScale = "0">

        String targetExpression = "CREATE TABLE IDTABLE\n" + "(\n" + "  IDCONTEXT  VARCHAR(20) NOT NULL PRIMARY KEY,\n"
                                  + "  NEXTID     NUMERIC\n" + ");";

        URL url = this.getClass().getClassLoader().getResource("ddl/create_table.ddl");
        assertThat(url, is(notNullValue()));
        content = url.openStream();
        assertThat(content, is(notNullValue()));
        sequencer.sequence(content, output, context);

        output.close();

        Subgraph subgraph = graph.getSubgraphOfDepth(10).at("/");
        assertThat(subgraph, is(notNullValue()));
        // String value = subgraph.toString();
        // System.out.println(value);

        SubgraphNode rootNode = subgraph.getNode(".");
        assertThat(rootNode.getChildren().size(), is(1));

        SubgraphNode statementsNode = rootNode.getNode(path("ddl:statements"));
        assertNotNull(statementsNode);
        assertThat(statementsNode.getChildren().size(), is(1));
        assertThat(verifyPrimaryType(statementsNode, "nt:unstructured"), is(true));
        assertThat(verifyProperty(statementsNode, "ddl:parserId", "SQL92"), is(true));

        SubgraphNode tableNode = statementsNode.getNode(path("IDTABLE"));
        assertNotNull(tableNode);
        verifyBaseProperties(tableNode, "nt:unstructured", "1", "1", "0", 3);
        assertThat(verifyMixinType(tableNode, "ddl:createTableStatement"), is(true));
        assertThat(verifyExpression(tableNode, targetExpression), is(true));

        // Check Column Properties
        SubgraphNode idcontextNode = tableNode.getNode(path("IDCONTEXT"));
        assertNotNull(idcontextNode);
        assertThat(verifyPrimaryType(idcontextNode, "nt:unstructured"), is(true));
        assertThat(verifyProperty(idcontextNode, "ddl:datatypeName", "VARCHAR"), is(true));
        assertThat(verifyProperty(idcontextNode, "ddl:datatypeLength", "20"), is(true));
        assertThat(verifyMixinType(idcontextNode, "ddl:columnDefinition"), is(true));

        SubgraphNode nextidNode = tableNode.getNode(path("NEXTID"));
        assertNotNull(nextidNode);
        assertThat(verifyPrimaryType(nextidNode, "nt:unstructured"), is(true));
        assertThat(verifyProperty(nextidNode, "ddl:datatypeName", "NUMERIC"), is(true));
        assertThat(verifyProperty(nextidNode, "ddl:datatypePrecision", "0"), is(true));
        assertThat(verifyProperty(nextidNode, "ddl:datatypeScale", "0"), is(true));
        assertThat(verifyHasProperty(nextidNode, "ddl:datatypeLength"), is(false));
        assertThat(verifyMixinType(nextidNode, "ddl:columnDefinition"), is(true));

        SubgraphNode pk_1_Node = tableNode.getNode(path("PK_1"));
        assertNotNull(pk_1_Node);
        assertThat(verifyPrimaryType(pk_1_Node, "nt:unstructured"), is(true));
        assertThat(verifyProperty(pk_1_Node, "ddl:constraintType", "PRIMARY KEY"), is(true));
        assertThat(verifyMixinType(pk_1_Node, "ddl:tableConstraint"), is(true));

        // One column reference
        assertThat(pk_1_Node.getChildren().size(), is(1));
        SubgraphNode idcontectRefNode = pk_1_Node.getNode(path("IDCONTEXT"));
        assertNotNull(idcontectRefNode);
        assertThat(verifyPrimaryType(idcontectRefNode, "nt:unstructured"), is(true));
        assertThat(verifyMixinType(idcontectRefNode, "ddl:columnReference"), is(true));
    }

    @Test
    public void shouldSequenceStatementsWithDoubleQuotes() throws IOException {
        // ALTER JAVA CLASS "Agent"
        // RESOLVER (("/home/java.101/bin/*" pm)(* public))
        // RESOLVE;
        //    
        // CREATE SERVER foo FOREIGN DATA WRAPPER "default";
        //
        // CREATE RULE "_RETURN" AS
        // ON SELECT TO t1
        // DO INSTEAD
        // SELECT * FROM t2;

        // Subgraph
        // <name = "/" uuid = "233d07e5-8431-4844-856b-ac80d0129c01">
        // <name = "statements" primaryType = "nt:unstructured" uuid = "dbd23d78-8005-4b7b-aa61-eac98726b8d4" parserId =
        // "POSTGRES">
        // <name = "unknownStatement" startLineNumber = "1" primaryType = "nt:unstructured" uuid =
        // "b3fb1e1f-284b-46bd-9a06-86d1b85dd5b3" startColumnNumber = "1" mixinTypes = "ddl:unknownStatement" expression =
        // "ALTER JAVA CLASS "Agent"
        // RESOLVER (("/home/java.101/bin/*" pm)(* public))
        // RESOLVE;" startCharIndex = "0">
        // <name = "CREATE SERVER" startLineNumber = "5" primaryType = "nt:unstructured" uuid =
        // "10217bab-877f-4b45-b169-2f81bb31f620" startColumnNumber = "1" mixinTypes = "postgresddl:createServerStatement"
        // expression = "CREATE SERVER foo FOREIGN DATA WRAPPER "default";" startCharIndex = "93">
        // <name = "_RETURN" startLineNumber = "7" primaryType = "nt:unstructured" uuid = "2b655039-f239-4930-8c1b-a3b9a8e7c949"
        // startColumnNumber = "1" mixinTypes = "postgresddl:createRuleStatement" expression = "CREATE RULE "_RETURN" AS
        // ON SELECT TO t1
        // DO INSTEAD
        // SELECT * FROM t2;" startCharIndex = "144">

        URL url = this.getClass().getClassLoader().getResource("ddl/d_quoted_statements.ddl");
        assertThat(url, is(notNullValue()));
        content = url.openStream();
        assertThat(content, is(notNullValue()));
        sequencer.sequence(content, output, context);

        output.close();

        Subgraph subgraph = graph.getSubgraphOfDepth(10).at("/");
        assertThat(subgraph, is(notNullValue()));
        // String value = subgraph.toString();
        // System.out.println(value);

        SubgraphNode rootNode = subgraph.getNode(".");
        assertThat(rootNode.getChildren().size(), is(1));

        SubgraphNode statementsNode = rootNode.getNode(path("ddl:statements"));
        assertNotNull(statementsNode);
        assertThat(statementsNode.getChildren().size(), is(3));
        assertThat(verifyPrimaryType(statementsNode, "nt:unstructured"), is(true));
        assertThat(verifyProperty(statementsNode, "ddl:parserId", "POSTGRES"), is(true));

        SubgraphNode firstNode = statementsNode.getNode(path("unknownStatement"));
        assertNotNull(firstNode);
        verifyBaseProperties(firstNode, "nt:unstructured", "1", "1", "0", 0);
        assertThat(verifyMixinType(firstNode, "ddl:unknownStatement"), is(true));

        SubgraphNode serverNode = statementsNode.getNode(path("CREATE SERVER"));
        assertNotNull(serverNode);
        verifyBaseProperties(serverNode, "nt:unstructured", "5", "1", "93", 0);
        assertThat(verifyMixinType(serverNode, "postgresddl:createServerStatement"), is(true));

        SubgraphNode ruleNode = statementsNode.getNode(path("_RETURN"));
        assertNotNull(ruleNode);
        verifyBaseProperties(ruleNode, "nt:unstructured", "7", "1", "144", 0);
        assertThat(verifyMixinType(ruleNode, "postgresddl:createRuleStatement"), is(true));

    }

    @Test
    public void shouldGenerateNodeTypesForCreateTables() throws IOException {
        URL url = this.getClass().getClassLoader().getResource("ddl/createTables.ddl");
        assertThat(url, is(notNullValue()));
        content = url.openStream();
        assertThat(content, is(notNullValue()));
        sequencer.sequence(content, output, context);

        output.close();

        Subgraph subgraph = graph.getSubgraphOfDepth(10).at("/");
        assertThat(subgraph, is(notNullValue()));
        // String value = subgraph.toString();
        // System.out.println(value);

        SubgraphNode rootNode = subgraph.getNode(".");
        assertThat(rootNode.getChildren().size(), is(1));

        SubgraphNode statementsNode = rootNode.getNode(path("ddl:statements"));
        assertNotNull(statementsNode);
        assertThat(statementsNode.getChildren().size(), is(20));
        assertThat(verifyPrimaryType(statementsNode, "nt:unstructured"), is(true));
        assertThat(verifyProperty(statementsNode, "ddl:parserId", "SQL92"), is(true));

        // Check one table
        // CREATE TABLE RT_MDLS
        // (
        // MDL_UID NUMERIC(20) NOT NULL,
        // MDL_UUID VARCHAR(64) NOT NULL,
        // MDL_NM VARCHAR(255) NOT NULL,
        // MDL_VERSION VARCHAR(50),
        // DESCRIPTION VARCHAR(255),
        // MDL_URI VARCHAR(255),
        // MDL_TYPE NUMERIC(3),
        // IS_PHYSICAL CHAR(1) NOT NULL,
        // MULTI_SOURCED CHAR(1) DEFAULT '0',
        // VISIBILITY NUMERIC(3)
        //   
        // );
        // <name = "RT_MDLS" startLineNumber = "80" primaryType = "nt:unstructured" uuid = "a11e48d5-0908-467b-ba79-a497df87a576"
        // startColumnNumber = "1" mixinTypes = "ddl:createTableStatement" expression = "CREATE TABLE RT_MDLS...." startCharIndex
        // = "2258">
        // <name = "MDL_UID" datatypeName = "NUMERIC" primaryType = "nt:unstructured" uuid =
        // "85b9c2b3-cadc-425c-a736-656be1475780" datatypePrecision = "20" nullable = "NOT NULL" mixinTypes =
        // "ddl:columnDefinition" datatypeScale = "0">
        // <name = "MDL_UUID" datatypeName = "VARCHAR" datatypeLength = "64" primaryType = "nt:unstructured" uuid =
        // "88766ab0-32e6-44f6-9244-78d48b2242d4" nullable = "NOT NULL" mixinTypes = "ddl:columnDefinition">
        // <name = "MDL_NM" datatypeName = "VARCHAR" datatypeLength = "255" primaryType = "nt:unstructured" uuid =
        // "cb29fa8e-03f0-4e82-8040-5d853aa8a1f9" nullable = "NOT NULL" mixinTypes = "ddl:columnDefinition">
        // <name = "MDL_VERSION" datatypeName = "VARCHAR" datatypeLength = "50" primaryType = "nt:unstructured" uuid =
        // "169a4e2f-79a4-47a4-8e23-4fa1ea09c876" mixinTypes = "ddl:columnDefinition">
        // <name = "DESCRIPTION" datatypeName = "VARCHAR" datatypeLength = "255" primaryType = "nt:unstructured" uuid =
        // "00682ead-5bb0-4c0d-b243-6edbf56dcb4a" mixinTypes = "ddl:columnDefinition">
        // <name = "MDL_URI" datatypeName = "VARCHAR" datatypeLength = "255" primaryType = "nt:unstructured" uuid =
        // "851f94e1-5d43-4a58-be6c-720a6c3bec7f" mixinTypes = "ddl:columnDefinition">
        // <name = "MDL_TYPE" datatypeName = "NUMERIC" primaryType = "nt:unstructured" uuid =
        // "a7b74a6f-872d-4e8b-a2da-4ebf6ef38ed4" datatypePrecision = "3" mixinTypes = "ddl:columnDefinition" datatypeScale = "0">
        // <name = "IS_PHYSICAL" datatypeName = "CHAR" datatypeLength = "1" primaryType = "nt:unstructured" uuid =
        // "b96aeaaa-4bd8-4154-9daf-50aac2fd9529" nullable = "NOT NULL" mixinTypes = "ddl:columnDefinition">
        // <name = "MULTI_SOURCED" datatypeName = "CHAR" defaultValue = "'0'" datatypeLength = "1" defaultOption = "0" primaryType
        // = "nt:unstructured" uuid = "75c48a37-f65c-4854-bca4-8d504cf8b658" mixinTypes = "ddl:columnDefinition">
        // <name = "VISIBILITY" datatypeName = "NUMERIC" primaryType = "nt:unstructured" uuid =
        // "345ac63b-a343-408d-9262-6e009ad3e0c2" datatypePrecision = "3" mixinTypes = "ddl:columnDefinition" datatypeScale = "0">

        SubgraphNode tableNode = statementsNode.getNode(path("RT_MDLS"));
        assertNotNull(tableNode);
        verifyBaseProperties(tableNode, "nt:unstructured", "80", "1", "2258", 10);
        assertThat(verifyMixinType(tableNode, "ddl:createTableStatement"), is(true));

        // Check Column Properties
        SubgraphNode node_1 = tableNode.getNode(path("MDL_UUID"));
        assertNotNull(node_1);
        assertThat(verifyPrimaryType(node_1, "nt:unstructured"), is(true));
        assertThat(verifyProperty(node_1, "ddl:datatypeName", "VARCHAR"), is(true));
        assertThat(verifyProperty(node_1, "ddl:datatypeLength", "64"), is(true));
        assertThat(verifyMixinType(node_1, "ddl:columnDefinition"), is(true));

        SubgraphNode node_2 = tableNode.getNode(path("MDL_TYPE"));
        assertNotNull(node_2);
        assertThat(verifyPrimaryType(node_2, "nt:unstructured"), is(true));
        assertThat(verifyProperty(node_2, "ddl:datatypeName", "NUMERIC"), is(true));
        assertThat(verifyProperty(node_2, "ddl:datatypePrecision", "3"), is(true));
        assertThat(verifyProperty(node_2, "ddl:datatypeScale", "0"), is(true));
        assertThat(verifyHasProperty(node_2, "ddl:datatypeLength"), is(false));
        assertThat(verifyMixinType(node_2, "ddl:columnDefinition"), is(true));
    }

    @Ignore
    @Test
    public void shouldSequenceDerbyDdl() throws IOException {
        URL url = this.getClass().getClassLoader().getResource("ddl/dialect/derby/derby_test_statements.ddl");
        assertThat(url, is(notNullValue()));
        content = url.openStream();
        assertThat(content, is(notNullValue()));
        sequencer.sequence(content, output, context);

        output.close();

        Subgraph subgraph = graph.getSubgraphOfDepth(10).at("/");
        assertThat(subgraph, is(notNullValue()));
        // String value = subgraph.toString();
        // System.out.println(value);

        SubgraphNode rootNode = subgraph.getNode(".");
        assertThat(rootNode.getChildren().size(), is(1));

        SubgraphNode statementsNode = rootNode.getNode(path("ddl:statements"));
        assertNotNull(statementsNode);
        assertThat(statementsNode.getChildren().size(), is(64));
        assertThat(verifyPrimaryType(statementsNode, "nt:unstructured"), is(true));
        assertThat(verifyProperty(statementsNode, "ddl:parserId", "DERBY"), is(true));

        // CREATE INDEX IXSALE ON SAMP.SALES (SALES);
        // <name = "IXSALE" startLineNumber = "87" primaryType = "nt:unstructured" uuid = "85740506-ac6e-46f3-8118-be0c9eb1bc57"
        // startColumnNumber = "1" mixinTypes = "derbyddl:createIndexStatement" tableName = "SAMP.SALES" expression =
        // "CREATE INDEX IXSALE ON SAMP.SALES (SALES);" unique = "false" startCharIndex = "2886">

        SubgraphNode indexNode = statementsNode.getNode(path("IXSALE"));
        assertNotNull(indexNode);
        verifyBaseProperties(indexNode, "nt:unstructured", "87", "1", "2886", 0);
        assertThat(verifyMixinType(indexNode, "derbyddl:createIndexStatement"), is(true));

        // CREATE SCHEMA FLIGHTS AUTHORIZATION anita;
        // <name = "FLIGHTS" startLineNumber = "98" primaryType = "nt:unstructured" uuid = "e1c8227d-c663-4d4b-8506-b1e41aa1f7f6"
        // startColumnNumber = "1" mixinTypes = "ddl:createSchemaStatement" expression =
        // "CREATE SCHEMA FLIGHTS AUTHORIZATION anita;" startCharIndex = "3218">
        SubgraphNode schemaNode = statementsNode.getNode(path("FLIGHTS"));
        assertNotNull(schemaNode);
        verifyBaseProperties(schemaNode, "nt:unstructured", "98", "1", "3218", 0);
        assertThat(verifyMixinType(schemaNode, "ddl:createSchemaStatement"), is(true));
        assertThat(verifyExpression(schemaNode, "CREATE SCHEMA FLIGHTS AUTHORIZATION anita;"), is(true));

        // DROP PROCEDURE some_procedure_name;
        // <name = "unknownStatement[3]" startLineNumber = "172" primaryType = "nt:unstructured" uuid =
        // "1f57c0ec-4361-4f64-963d-dcb919c27656" startColumnNumber = "1" mixinTypes = "ddl:unknownStatement" expression =
        // "DROP PROCEDURE some_procedure_name;" startCharIndex = "5438">
        SubgraphNode unknownNode_1 = statementsNode.getNode(path("some_procedure_name"));
        assertNotNull(unknownNode_1);
        verifyBaseProperties(unknownNode_1, "nt:unstructured", "172", "1", "5438", 0);
        assertThat(verifyMixinType(unknownNode_1, "derbyddl:dropProcedureStatement"), is(true));
        assertThat(verifyExpression(unknownNode_1, "DROP PROCEDURE some_procedure_name;"), is(true));

        // ALTER TABLE SAMP.DEPARTMENT
        // ADD CONSTRAINT NEW_UNIQUE UNIQUE (DEPTNO);

        // <name = "SAMP.DEPARTMENT" startLineNumber = "16" primaryType = "nt:unstructured" uuid =
        // "b3c54a3c-4779-48a0-a2f3-80e5079bb62c" startColumnNumber = "1" mixinTypes = "ddl:alterTableStatement" expression =
        // "ALTER TABLE SAMP.DEPARTMENT
        // ADD CONSTRAINT NEW_UNIQUE UNIQUE (DEPTNO);
        //
        // -- add a new foreign key constraint to the
        // -- Cities table. Each row in Cities is checked
        // -- to make sure it satisfied the constraints.
        // -- if any rows don't satisfy the constraint, the
        // -- constraint is not added" startCharIndex = "478">
        // <name = "NEW_UNIQUE" primaryType = "nt:unstructured" uuid = "c678bb9d-a0ac-4f69-b8a0-ec890557bc20" mixinTypes =
        // "ddl:addTableConstraintDefinition" constraintType = "0">
        // <name = "DEPTNO" primaryType = "nt:unstructured" uuid = "0f019782-e72d-485a-8f8c-e62954fd6ae6" mixinTypes =
        // "ddl:columnReference">
        SubgraphNode alterTableNode = statementsNode.getNode(path("SAMP.DEPARTMENT"));
        assertNotNull(alterTableNode);
        verifyBaseProperties(alterTableNode, "nt:unstructured", "16", "1", "478", 1);
        assertThat(verifyMixinType(alterTableNode, "ddl:alterTableStatement"), is(true));

        SubgraphNode uniqueNode = alterTableNode.getNode(path("NEW_UNIQUE"));
        assertNotNull(uniqueNode);
        assertThat(verifyPrimaryType(uniqueNode, "nt:unstructured"), is(true));
        assertThat(verifyProperty(uniqueNode, "ddl:constraintType", "0"), is(true));
        assertThat(verifyMixinType(uniqueNode, "ddl:addTableConstraintDefinition"), is(true));

        // One column reference
        assertThat(uniqueNode.getChildren().size(), is(1));
        SubgraphNode colRefNode = uniqueNode.getNode(path("DEPTNO"));
        assertNotNull(colRefNode);
        assertThat(verifyPrimaryType(colRefNode, "nt:unstructured"), is(true));
        assertThat(verifyMixinType(colRefNode, "ddl:columnReference"), is(true));
    }

    @Test
    public void shouldSequenceOracleDdl() throws IOException {
        URL url = this.getClass().getClassLoader().getResource("ddl/dialect/oracle/oracle_test_statements_2.ddl");
        assertThat(url, is(notNullValue()));
        content = url.openStream();
        assertThat(content, is(notNullValue()));
        sequencer.sequence(content, output, context);

        output.close();

        Subgraph subgraph = graph.getSubgraphOfDepth(10).at("/");
        assertThat(subgraph, is(notNullValue()));
        // String value = subgraph.toString();
        // System.out.println(value);

        SubgraphNode rootNode = subgraph.getNode(".");
        assertThat(rootNode.getChildren().size(), is(1));

        SubgraphNode statementsNode = rootNode.getNode(path("ddl:statements"));
        assertNotNull(statementsNode);
        assertThat(statementsNode.getChildren().size(), is(50));
        assertThat(verifyPrimaryType(statementsNode, "nt:unstructured"), is(true));
        assertThat(verifyProperty(statementsNode, "ddl:parserId", "ORACLE"), is(true));

        // <name = "CREATE OR REPLACE DIRECTORY" startLineNumber = "164" primaryType = "nt:unstructured" uuid =
        // "c45eb2bb-1b85-469d-9dfc-0012fdfd8ac4" startColumnNumber = "1" mixinTypes = "oracleddl:createDirectoryStatement"
        // expression = "CREATE OR REPLACE DIRECTORY bfile_dir AS '/private1/LOB/files';" startCharIndex = "3887">
        SubgraphNode createOrReplDirNode = statementsNode.getNode(path("CREATE OR REPLACE DIRECTORY"));
        assertNotNull(createOrReplDirNode);
        verifyBaseProperties(createOrReplDirNode, "nt:unstructured", "164", "1", "3886", 0);
        assertThat(verifyMixinType(createOrReplDirNode, "oracleddl:createDirectoryStatement"), is(true));

        // <name = "countries" startLineNumber = "9" primaryType = "nt:unstructured" uuid = "70f45acc-57b0-41c9-b166-bcba4f8c75b8"
        // startColumnNumber = "1" mixinTypes = "ddl:alterTableStatement" expression = "ALTER TABLE countries
        // ADD (duty_pct NUMBER(2,2) CHECK (duty_pct < 10.5),
        // visa_needed VARCHAR2(3));" startCharIndex = "89">
        // <name = "duty_pct" datatypeName = "NUMBER" primaryType = "nt:unstructured" uuid =
        // "20079cae-5de1-425c-925e-df230410ea69" datatypePrecision = "2" mixinTypes = "ddl:columnDefinition" datatypeScale = "2">
        // <name = "CHECK_1" primaryType = "nt:unstructured" name = "CHECK_1" uuid = "210039d7-ebe7-47a8-94be-c3adb70b2885"
        // mixinTypes = "ddl:addTableConstraintDefinition" constraintType = "3" searchCondition = "( duty_pct < 10 . 5 )">
        // <name = "visa_needed" datatypeName = "VARCHAR2" datatypeLength = "3" primaryType = "nt:unstructured" uuid =
        // "b7b6bf1d-6a2b-411a-aa63-974d13ba20e8" mixinTypes = "ddl:columnDefinition">
        SubgraphNode countriesNode = statementsNode.getNode(path("countries"));
        assertNotNull(countriesNode);
        verifyBaseProperties(countriesNode, "nt:unstructured", "9", "1", "89", 3);
        assertThat(verifyMixinType(countriesNode, "ddl:alterTableStatement"), is(true));

        SubgraphNode duty_pct_node = countriesNode.getNode(path("duty_pct"));
        assertNotNull(duty_pct_node);
        assertThat(verifyPrimaryType(duty_pct_node, "nt:unstructured"), is(true));
        assertThat(verifyProperty(duty_pct_node, "ddl:datatypeName", "NUMBER"), is(true));
        assertThat(verifyProperty(duty_pct_node, "ddl:datatypePrecision", "2"), is(true));
        assertThat(verifyProperty(duty_pct_node, "ddl:datatypeScale", "2"), is(true));
        assertThat(verifyHasProperty(duty_pct_node, "ddl:datatypeLength"), is(false));
        assertThat(verifyMixinType(duty_pct_node, "ddl:columnDefinition"), is(true));

        SubgraphNode check_1_node = countriesNode.getNode(path("CHECK_1"));
        assertNotNull(check_1_node);
        assertThat(verifyPrimaryType(check_1_node, "nt:unstructured"), is(true));
        assertThat(verifyProperty(check_1_node, "ddl:constraintType", "CHECK"), is(true));
        assertThat(verifyMixinType(check_1_node, "ddl:addTableConstraintDefinition"), is(true));
        assertThat(verifyProperty(check_1_node, "ddl:searchCondition", "( duty_pct < 10 . 5 )"), is(true));

        SubgraphNode visa_needed_node = countriesNode.getNode(path("visa_needed"));
        assertNotNull(visa_needed_node);
        assertThat(verifyPrimaryType(visa_needed_node, "nt:unstructured"), is(true));
        assertThat(verifyProperty(visa_needed_node, "ddl:datatypeName", "VARCHAR2"), is(true));
        assertThat(verifyProperty(visa_needed_node, "ddl:datatypeLength", "3"), is(true));
        assertThat(verifyMixinType(visa_needed_node, "ddl:columnDefinition"), is(true));

        // <name = "app_user1" startLineNumber = "33" primaryType = "nt:unstructured" uuid =
        // "8c660ae8-2078-4263-a0e7-8f517cefd3a0" startColumnNumber = "1" mixinTypes = "oracleddl:alterUserStatement" expression =
        // "ALTER USER app_user1
        // GRANT CONNECT THROUGH sh
        // WITH ROLE warehouse_user;" startCharIndex = "624">

        SubgraphNode app_user1Node = statementsNode.getNode(path("app_user1"));
        assertNotNull(app_user1Node);
        verifyBaseProperties(app_user1Node, "nt:unstructured", "33", "1", "624", 0);
        assertThat(verifyMixinType(app_user1Node, "oracleddl:alterUserStatement"), is(true));
    }

    protected String[] builtInGrammars() {
        List<String> builtInParserNames = new ArrayList<String>();
        for (DdlParser parser : DdlParsers.BUILTIN_PARSERS) {
            builtInParserNames.add(parser.getId().toLowerCase());
        }
        return builtInParserNames.toArray(new String[builtInParserNames.size()]);
    }

    @Test
    public void shouldHaveDefaultListOfGrammars() {
        String[] grammars = sequencer.getGrammars();
        assertThat(grammars, is(notNullValue()));
        assertThat(grammars, is(builtInGrammars()));
    }

    @Test
    public void shouldCreateListOfDdlParserInstancesForDefaultListOfGrammars() {
        List<DdlParser> parsers = sequencer.getParserList(context);
        assertThat(parsers, is(DdlParsers.BUILTIN_PARSERS));
    }

    @Test
    public void shouldAllowSettingGrammarsWithEmptyArray() {
        sequencer.setGrammars(new String[] {});
        assertThat(sequencer.getGrammars(), is(builtInGrammars()));
        assertThat(context.getProblems().isEmpty(), is(true));
    }

    @Test
    public void shouldAllowSettingGrammarsWithNullArray() {
        sequencer.setGrammars(null);
        assertThat(sequencer.getGrammars(), is(builtInGrammars()));
        assertThat(context.getProblems().isEmpty(), is(true));
    }

    @Test
    public void shouldAllowSettingGrammarsToNonEmptyArrayOfValidBuiltInGrammars() {
        String[] grammars = new String[] {new OracleDdlParser().getId(), new StandardDdlParser().getId()};
        sequencer.setGrammars(grammars);
        assertThat(sequencer.getGrammars(), is(grammars));
        assertThat(context.getProblems().isEmpty(), is(true));
    }

    @Test
    public void shouldAllowSettingGrammarsToNonEmptyArrayOfValidAndNonExistantBuiltInGrammars() {
        String[] grammars = new String[] {new OracleDdlParser().getId(), new StandardDdlParser().getId(), "argle"};
        sequencer.setGrammars(grammars);
        assertThat(sequencer.getGrammars(), is(grammars));
        assertThat(context.getProblems().isEmpty(), is(true));
    }

    @Test
    public void shouldCreateDdlParserInstancesForAllValidBuiltInGrammars() {
        String[] grammars = new String[] {new OracleDdlParser().getId(), new StandardDdlParser().getId(), "argle"};
        sequencer.setGrammars(grammars);
        assertThat(sequencer.getGrammars(), is(grammars));
        List<DdlParser> parsers = sequencer.getParserList(context);
        assertThat(parsers.get(0), is((DdlParser)new OracleDdlParser()));
        assertThat(parsers.get(1), is((DdlParser)new StandardDdlParser()));
        assertThat(parsers.size(), is(2));
        assertThat(context.getProblems().isEmpty(), is(false));
        assertThat(context.getProblems().hasErrors(), is(true));
    }

    @Test
    public void shouldCreateDdlParserInstancesForAllValidBuiltInGrammarsAndInstantiableParser() {
        String[] grammars = new String[] {new OracleDdlParser().getId(), new StandardDdlParser().getId(),
            ArgleDdlParser.class.getName()};
        sequencer.setGrammars(grammars);
        assertThat(sequencer.getGrammars(), is(grammars));
        List<DdlParser> parsers = sequencer.getParserList(context);
        assertThat(context.getProblems().isEmpty(), is(true));
        assertThat(parsers.get(0), is((DdlParser)new OracleDdlParser()));
        assertThat(parsers.get(1), is((DdlParser)new StandardDdlParser()));
        assertThat(parsers.get(2), is((DdlParser)new ArgleDdlParser()));
        assertThat(parsers.size(), is(3));
    }

    protected static class ArgleDdlParser extends StandardDdlParser {
        @Override
        public String getId() {
            return "ARGLE";
        }
    }
}
