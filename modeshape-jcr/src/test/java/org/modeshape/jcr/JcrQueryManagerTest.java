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
package org.modeshape.jcr;

import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_LIKE;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.query.qom.ChildNode;
import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Join;
import javax.jcr.query.qom.Literal;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.PropertyValue;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.query.qom.Selector;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Json;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.ValidateQuery.Predicate;
import org.modeshape.jcr.ValidateQuery.ValidationBuilder;
import org.modeshape.jcr.api.query.QueryManager;
import org.modeshape.jcr.api.query.qom.Limit;
import org.modeshape.jcr.api.query.qom.QueryObjectModelFactory;
import org.modeshape.jcr.api.query.qom.SelectQuery;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.query.JcrQueryResult;
import org.modeshape.jcr.security.SimplePrincipal;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;

/**
 * This is a test suite that operates against a complete JcrRepository instance created and managed using the JcrEngine.
 * Essentially this is an integration test, but it does test lower-level functionality of the implementation of the JCR interfaces
 * related to querying. (It is simply more difficult to unit test these implementations because of the difficulty in mocking the
 * many other components to replicate the same functionality.)
 * <p>
 * Also, because queries are read-only, the engine is set up once and used for the entire set of test methods.
 * </p>
 */
public class JcrQueryManagerTest extends MultiUseAbstractTest {

    private static final String[] INDEXED_SYSTEM_NODES_PATHS = new String[] {"/jcr:system/jcr:nodeTypes",
        "/jcr:system/mode:namespaces", "/jcr:system/mode:repository"};

    /** The total number of nodes excluding '/jcr:system' */
    protected static final int TOTAL_NON_SYSTEM_NODE_COUNT = 25;

    /** The total number of nodes at or below '/jcr:system' */
    protected static int totalSystemNodeCount;

    /** The total number of nodes */
    protected static int totalNodeCount;

    protected static URI resourceUri( String name ) throws URISyntaxException {
        return resourceUrl(name).toURI();
    }

    protected static URL resourceUrl( String name ) {
        return JcrQueryManagerTest.class.getClassLoader().getResource(name);
    }

    protected static InputStream resourceStream( String name ) {
        return JcrQueryManagerTest.class.getClassLoader().getResourceAsStream(name);
    }

    private static String[] prefixEach( String[] original,
                                        String prefix ) {
        String[] result = new String[original.length];
        for (int i = 0; i != original.length; ++i) {
            result[i] = prefix + original[i];
        }
        return result;
    }

    private static String[] allOf( String[]... arrays ) {
        List<String> result = new ArrayList<String>();
        for (String[] array : arrays) {
            for (String value : array) {
                result.add(value);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    protected static String[] carColumnNames() {
        return new String[] {"car:mpgCity", "car:lengthInInches", "car:maker", "car:userRating", "car:mpgHighway", "car:engine",
            "car:valueRating", "jcr:primaryType", "jcr:mixinTypes", "car:wheelbaseInInches", "car:model", "car:year", "car:msrp",
            "jcr:created", "jcr:createdBy", "jcr:name", "jcr:path", "jcr:score", "mode:depth", "mode:id", "mode:localName",
            "car:alternateModels"};
    }

    protected static String[] carColumnNames( String selectorName ) {
        return prefixEach(carColumnNames(), selectorName + ".");
    }

    protected static String[] allColumnNames() {
        return new String[] {"jcr:primaryType", "jcr:mixinTypes", "jcr:name", "jcr:path", "jcr:score", "mode:depth", "mode:id",
            "mode:localName"};
    }

    protected static String[] allColumnNames( String selectorName ) {
        return prefixEach(allColumnNames(), selectorName + ".");
    }

    protected static String[] typedColumnNames() {
        return new String[] {"notion:booleanProperty", "notion:booleanProperty2", "notion:stringProperty",
            "notion:booleanCreatedPropertyWithDefault", "notion:stringPropertyWithDefault",
            "notion:booleanAutoCreatedPropertyWithDefault", "notion:stringAutoCreatedPropertyWithDefault", "notion:longProperty",
            "notion:singleReference", "notion:multipleReferences", "jcr:primaryType", "jcr:mixinTypes", "jcr:name", "jcr:path",
            "jcr:score", "mode:depth", "mode:id", "mode:localName"};
    }

    protected static String[] typedColumnNames( String selectorName ) {
        return prefixEach(typedColumnNames(), selectorName + ".");
    }

    protected static String[] searchColumnNames() {
        return new String[] {"jcr:score"};
    }

    @BeforeClass
    public static void beforeAll() throws Exception {
        String configFileName = JcrQueryManagerTest.class.getSimpleName() + ".json";
        beforeAll(configFileName);
    }

    @SuppressWarnings( "deprecation" )
    protected static void beforeAll( String configFileName ) throws Exception {
        String configFilePath = "config/" + configFileName;
        InputStream configStream = JcrQueryManagerTest.class.getClassLoader().getResourceAsStream(configFilePath);
        assertThat("Unable to find configuration file '" + configFilePath, configStream, is(notNullValue()));

        Document configDoc = Json.read(configStream);
        RepositoryConfiguration config = new RepositoryConfiguration(configDoc, configFileName);
        startRepository(config);

        try {
            // Use a session to load the contents ...
            JcrSession session = repository.login();

            try {
                registerNodeTypes(session, "cnd/fincayra.cnd");
                registerNodeTypes(session, "cnd/magnolia.cnd");
                registerNodeTypes(session, "cnd/notionalTypes.cnd");
                registerNodeTypes(session, "cnd/cars.cnd");
                registerNodeTypes(session, "cnd/validType.cnd");
                registerNodeTypes(session, "cnd/mode-1900.cnd");
                registerNodeTypes(session, "cnd/noquery.cnd");

                InputStream stream = resourceStream("io/cars-system-view.xml");
                try {
                    session.getWorkspace().importXML("/", stream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    stream.close();
                }

                // Create a branch that contains some same-name-siblings ...
                Node other = session.getRootNode().addNode("Other", "nt:unstructured");
                Node a = other.addNode("NodeA", "nt:unstructured");
                a.addMixin("mix:referenceable");
                a.setProperty("something", "value3 quick brown fox");
                a.setProperty("somethingElse", "value2");
                a.setProperty("propA", "value1");
                Node other2 = other.addNode("NodeA", "nt:unstructured");
                other2.addMixin("mix:referenceable");
                other2.setProperty("something", "value2 quick brown cat wearing hat");
                other2.setProperty("propB", "value1");
                other2.setProperty("propC", "value2");
                Node other3 = other.addNode("NodeA", "nt:unstructured");
                other3.addMixin("mix:referenceable");
                other3.setProperty("something", new String[] {"black dog", "white dog"});
                other3.setProperty("propB", "value1");
                other3.setProperty("propC", "value3");
                Value[] refValues = new Value[2];
                refValues[0] = session.getValueFactory().createValue(other2);
                refValues[1] = session.getValueFactory().createValue(other3);
                other3.setProperty("otherNode", a);
                other3.setProperty("otherNodes", refValues);
                Node c = other.addNode("NodeC", "notion:typed");
                c.setProperty("notion:booleanProperty", true);
                c.setProperty("notion:booleanProperty2", false);
                c.setProperty("propD", "value4");
                c.setProperty("propC", "value1");
                c.setProperty("notion:singleReference", a);
                c.setProperty("notion:multipleReferences", refValues);
                Node b = session.getRootNode().addNode("NodeB", "nt:unstructured");
                b.setProperty("myUrl", "http://www.acme.com/foo/bar");
                b.setProperty("pathProperty", a.getPath());
                session.save();

                // Initialize the nodes count
                initNodesCount();

                // Prime creating a first XPath query and SQL query ...
                session.getWorkspace().getQueryManager().createQuery("//element(*,nt:unstructured)", Query.XPATH);
                session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [nt:base]", Query.JCR_SQL2);
            } finally {
                session.logout();
            }

            // Prime creating the schemata ...
            repository.nodeTypeManager().getRepositorySchemata();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private static void initNodesCount() throws RepositoryException {
        JcrSession session = repository.login();
        try {
            NodeCache systemSession = repository.createSystemSession(session.context(), true);
            totalSystemNodeCount = countAllNodesBelow(systemSession.getRootKey(), systemSession) - 1; // not root
            totalNodeCount = totalSystemNodeCount + TOTAL_NON_SYSTEM_NODE_COUNT;
        } finally {
            session.logout();
        }
    }

    private static int countAllNodesBelow( NodeKey nodeKey,
                                           NodeCache cache ) throws RepositoryException {
        CachedNode node = cache.getNode(nodeKey);
        if (node.isExcludedFromSearch(cache)) return 0;
        int result = 1;
        ChildReferences childReferences = node.getChildReferences(cache);
        for (Iterator<NodeKey> nodeKeyIterator = childReferences.getAllKeys(); nodeKeyIterator.hasNext();) {
            NodeKey childKey = nodeKeyIterator.next();
            result += countAllNodesBelow(childKey, cache);
        }
        return result;
    }

    @AfterClass
    public static void afterAll() throws Exception {
        MultiUseAbstractTest.afterAll();
    }

    @Before
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        print = false;
    }

    protected static void registerNodeTypes( JcrSession session,
                                             String pathToClasspathResource ) throws RepositoryException, IOException {
        URL url = resourceUrl(pathToClasspathResource);
        session.getWorkspace().getNodeTypeManager().registerNodeTypes(url, true);
    }

    @Override
    protected Name name( String name ) {
        return session.nameFactory().create(name);
    }

    protected Segment segment( String segment ) {
        return session.pathFactory().createSegment(segment);
    }

    protected List<Segment> segments( String... segments ) {
        List<Segment> result = new ArrayList<Segment>();
        for (String segment : segments) {
            result.add(segment(segment));
        }
        return result;
    }

    protected ValidationBuilder validateQuery() {
        return ValidateQuery.validateQuery().printDetail(print);
    }

    protected static class ResultPrinter {
        private final NodeIterator iter;
        private int rowNumber = 0;

        protected ResultPrinter( NodeIterator iter ) {
            this.iter = iter;
        }

        protected void printRow() throws RepositoryException {
            Node node = iter.nextNode();
            System.out.println(rowNumberStr() + " " + node.getPath());
        }

        protected void printRows() throws RepositoryException {
            while (iter.hasNext()) {
                printRow();
            }
        }

        protected String rowNumberStr() {
            return StringUtil.justifyRight("" + (++rowNumber), 4, ' ');
        }
    }

    protected static class ResultRowPrinter {
        private final RowIterator iter;
        private final String[] selectorNames;
        private int rowNumber = 0;

        protected ResultRowPrinter( RowIterator iter,
                                    String[] selectorNames ) {
            this.iter = iter;
            this.selectorNames = selectorNames;
        }

        protected void printRow() throws RepositoryException {
            Row row = iter.nextRow();
            System.out.print(rowNumberStr());
            for (String selectorName : selectorNames) {
                System.out.print(" ");
                System.out.print(row.getPath(selectorName));
            }
            System.out.println();
        }

        protected void printRows() throws RepositoryException {
            while (iter.hasNext()) {
                printRow();
            }
        }

        protected String rowNumberStr() {
            return StringUtil.justifyRight("" + (++rowNumber), 4, ' ');
        }
    }

    protected void assertResults( Query query,
                                  QueryResult result,
                                  long numberOfResults ) throws RepositoryException {
        assertThat(query, is(notNullValue()));
        assertThat(result, is(notNullValue()));
        if (print /*|| result.getNodes().getSize() != numberOfResults || result.getRows().getSize() != numberOfResults*/) {
            System.out.println();
            System.out.println(query);
            System.out.println(" plan -> " + ((org.modeshape.jcr.api.query.QueryResult)result).getPlan());
            System.out.println(result);
        }
        if (result.getSelectorNames().length == 1) {
            NodeIterator iter = result.getNodes();
            ResultPrinter resultPrinter = new ResultPrinter(iter);
            if (print || iter.getSize() != numberOfResults) {
                // print anyway since this is an error
                System.out.println();
                System.out.println(" plan -> " + ((JcrQueryResult)result).getPlan());
                System.out.println(result);
                System.out.println(query);
                resultPrinter.printRows();
            }
            assertThat(iter.getSize(), is(numberOfResults));
        } else {
            try {
                result.getNodes();
                if (!print) {
                    // print anyway since this is an error
                    System.out.println();
                    System.out.println(" plan -> " + ((JcrQueryResult)result).getPlan());
                    System.out.println(result);
                    System.out.println(query);
                }
                fail("should not be able to call this method when the query has multiple selectors");
            } catch (RepositoryException e) {
                // expected; can't call this when the query uses multiple selectors ...
            }
            RowIterator iter = result.getRows();
            ResultRowPrinter resultPrinter = new ResultRowPrinter(iter, result.getSelectorNames());
            if (print || iter.getSize() != numberOfResults) {
                // print anyway since this is an error
                System.out.println();
                System.out.println(" plan -> " + ((JcrQueryResult)result).getPlan());
                System.out.println(query);
                System.out.println(result);
                resultPrinter.printRows();
            }
            assertThat(iter.getSize(), is(numberOfResults));
        }
    }

    protected void assertResultsHaveColumns( QueryResult result,
                                             String... columnNames ) throws RepositoryException {
        List<String> expectedNames = new ArrayList<String>();
        for (String name : columnNames) {
            expectedNames.add(name);
        }
        List<String> actualNames = new ArrayList<String>();
        for (String name : result.getColumnNames()) {
            actualNames.add(name);
        }
        Collections.sort(expectedNames);
        Collections.sort(actualNames);
        assertThat(actualNames, is(expectedNames));
    }

    protected void assertResultsHaveRows( QueryResult result,
                                          String columnName,
                                          String... rowValuesAsStrings ) throws RepositoryException {
        RowIterator iter = result.getRows();
        int i = 0;
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row.getValue(columnName).getString(), is(rowValuesAsStrings[i++]));
        }
    }

    protected void assertValueIsNonNullReference( Row row,
                                                  String refColumnName ) throws RepositoryException {
        assertValueIsReference(row, refColumnName, false);
    }

    protected void assertValueIsReference( Row row,
                                           String refColumnName,
                                           boolean mayBeNull ) throws RepositoryException {
        Value value = row.getValue(refColumnName);
        if (!mayBeNull) {
            assertThat(value, is(notNullValue()));
            String ref = value.getString();
            Node referenced = session.getNodeByIdentifier(ref);
            assertThat(referenced, is(notNullValue()));
            assertEquals(ref, referenced.getIdentifier());
        }
    }

    public interface RowResult {
        public RowResult has( String columnName,
                              String value ) throws RepositoryException;

        public RowResult has( String columnName,
                              long value ) throws RepositoryException;

        public RowResult and( String columnName,
                              String value ) throws RepositoryException;

        public RowResult and( String columnName,
                              long value ) throws RepositoryException;
    }

    public class SpecificRowResult implements RowResult {
        private final Row row;

        public SpecificRowResult( Row row ) {
            this.row = row;
        }

        @Override
        public RowResult has( String columnName,
                              String value ) throws RepositoryException {
            assertThat(row.getValue(columnName).getString(), is(value));
            return this;
        }

        @Override
        public RowResult has( String columnName,
                              long value ) throws RepositoryException {
            assertThat(row.getValue(columnName).getLong(), is(value));
            return this;
        }

        @Override
        public RowResult and( String columnName,
                              String value ) throws RepositoryException {
            return has(columnName, value);
        }

        @Override
        public RowResult and( String columnName,
                              long value ) throws RepositoryException {
            return has(columnName, value);
        }
    }

    public class SomeRowResult implements RowResult {
        private final QueryResult result;
        private final List<Row> matchingRows = new LinkedList<Row>();

        public SomeRowResult( QueryResult result ) throws RepositoryException {
            this.result = result;
            for (RowIterator iterator = result.getRows(); iterator.hasNext();) {
                Row row = iterator.nextRow();
                matchingRows.add(row);
            }
        }

        @Override
        public RowResult has( String columnName,
                              long value ) throws RepositoryException {
            // Remove all the rows that don't have a matching value ...
            ListIterator<Row> iter = matchingRows.listIterator();
            while (iter.hasNext()) {
                Row row = iter.next();
                if (row.getValue(columnName).getLong() != value) iter.remove();
            }
            if (matchingRows.isEmpty()) {
                fail("Failed to find row");
            }
            return this;
        }

        @Override
        public RowResult has( String columnName,
                              String value ) throws RepositoryException {
            // Remove all the rows that don't have a matching value ...
            ListIterator<Row> iter = matchingRows.listIterator();
            while (iter.hasNext()) {
                Row row = iter.next();
                Value actualValue = row.getValue(columnName);
                if (actualValue == null && value == null) continue;
                if (actualValue != null && actualValue.getString().equals(value)) continue;
                iter.remove();
            }
            if (matchingRows.isEmpty()) {
                fail("Failed to find row");
            }
            return this;
        }

        @Override
        public RowResult and( String columnName,
                              long value ) throws RepositoryException {
            return has(columnName, value);
        }

        @Override
        public RowResult and( String columnName,
                              String value ) throws RepositoryException {
            return has(columnName, value);
        }

        @Override
        public String toString() {
            return result.toString();
        }
    }

    protected RowResult assertRow( QueryResult result ) throws RepositoryException {
        return new SomeRowResult(result);
    }

    protected RowResult assertRow( QueryResult result,
                                   int rowNumber ) throws RepositoryException {
        RowIterator rowIter = result.getRows();
        Row row = null;
        for (int i = 0; i != rowNumber; ++i) {
            row = rowIter.nextRow();
        }
        assertThat(row, is(notNullValue()));
        return new SpecificRowResult(row);
    }

    @Test
    public void shouldStartUp() {
        assertThat(session, is(notNullValue()));
    }

    @Test
    public void shouldHaveLoadedContent() throws RepositoryException {
        Node node = session.getRootNode().getNode("Cars");
        assertThat(node, is(notNullValue()));
        assertThat(node.hasNode("Sports"), is(true));
        assertThat(node.hasNode("Utility"), is(true));
        assertThat(node.hasNode("Hybrid"), is(true));
        // System.out.println(node.getNode("Hybrid").getNodes().nextNode().getPath());
        assertThat(node.hasNode("Hybrid/Toyota Prius"), is(true));
        assertThat(node.getPrimaryNodeType().getName(), is("nt:unstructured"));
    }

    @Test
    public void shouldReturnQueryManagerFromWorkspace() throws RepositoryException {
        assertThat(session.getWorkspace().getQueryManager(), is(notNullValue()));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // JCR-SQL2 Queries
    // ----------------------------------------------------------------------------------------------------------------

    @Ignore
    @Test
    public void shouldFailIfGetNodesOrGetRowsIsCalledAfterGetRowsCalledOncePerQueryResult() throws RepositoryException {
        String sql = "SELECT * FROM [nt:unstructured]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        org.modeshape.jcr.api.query.QueryResult result = (org.modeshape.jcr.api.query.QueryResult)query.execute();
        assertNotNull(result.getRows());
        try {
            result.getRows();
            fail();
        } catch (RepositoryException e) {
            // expected
        }
        try {
            result.getNodes();
            fail();
        } catch (RepositoryException e) {
            // expected
        }
    }

    @Ignore
    @Test
    public void shouldFailIfGetNodesOrGetRowsIsCalledAfterGetNodesCalledOncePerQueryResult() throws RepositoryException {
        String sql = "SELECT * FROM [nt:unstructured]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        org.modeshape.jcr.api.query.QueryResult result = (org.modeshape.jcr.api.query.QueryResult)query.execute();
        assertNotNull(result.getNodes());
        try {
            result.getRows();
            fail();
        } catch (RepositoryException e) {
            // expected
        }
        try {
            result.getNodes();
            fail();
        } catch (RepositoryException e) {
            // expected
        }
    }

    @Test
    public void shouldGetNodesOrderedByPath() throws RepositoryException {
        String sql = "SELECT * FROM [nt:unstructured] WHERE ISCHILDNODE('/Cars') ORDER BY [jcr:path]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(4).validate(query, query.execute());
    }

    @FixFor("MODE-2490")
    @Test
    public void shouldOrderByTwoColumnsEvenIfNullable() throws RepositoryException {
        String sql = "SELECT * FROM [car:Car] ORDER BY [car:maker] DESC NULLS FIRST, [car:msrp] ASC NULLS FIRST";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(13).validate(query, query.execute());
    }

    @FixFor( "MODE-2297" )
    @Test
    public void shouldExecuteQueryUsingSetOperationOfQueriesWithoutJoins() throws RepositoryException {
        // Make sure that /Other/NodeA[3] contains references to /Other/NodeA and /Other/NodeA[2] ...
        Node nodeA1 = session.getNode("/Other/NodeA");
        Node nodeA2 = session.getNode("/Other/NodeA[2]");
        Node nodeA3 = session.getNode("/Other/NodeA[3]");
        assertThat(nodeA3.getProperty("otherNode").getNode(), is(sameInstance(nodeA1)));
        assertThat(nodeA3.getProperty("otherNodes").getValues()[0].getString(), is(nodeA2.getIdentifier()));
        assertThat(nodeA3.getProperty("otherNodes").getValues()[1].getString(), is(nodeA3.getIdentifier()));

        // print = true;
        String sql1 = "SELECT [jcr:path] FROM [nt:unstructured] AS other WHERE ISCHILDNODE(other,'/Other')";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql1, Query.JCR_SQL2);
        validateQuery().rowCount(4).validate(query, query.execute());

        String sql2 = "SELECT [jcr:path] FROM [nt:unstructured] AS category WHERE ISCHILDNODE(category,'/Cars')";
        query = session.getWorkspace().getQueryManager().createQuery(sql2, Query.JCR_SQL2);
        validateQuery().rowCount(4).validate(query, query.execute());

        String sql = sql1 + " UNION " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(8).validate(query, query.execute());

        sql = sql2 + " UNION " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(8).validate(query, query.execute());

        sql = sql1 + " INTERSECT " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        sql = sql2 + " INTERSECT " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        sql = sql1 + " INTERSECT " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(4).validate(query, query.execute());

        sql = sql2 + " INTERSECT " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(4).validate(query, query.execute());

        sql = sql1 + " EXCEPT " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(4).validate(query, query.execute());

        sql = sql2 + " EXCEPT " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(4).validate(query, query.execute());
    }

    @FixFor( "MODE-2297" )
    @Test
    public void shouldExecuteQueryUsingSetOperationOfQueriesAccessSystemContent() throws RepositoryException {

        // print = true;
        String sql1 = "SELECT BASE.[jcr:path] from [mode:nodeTypes] as BASE " //
                      + "JOIN  [mode:system] AS PARENT ON ISCHILDNODE(BASE,PARENT) "//
                      + "JOIN [mode:namespaces] AS REF2 ON REF2.[jcr:uuid] = PARENT.[jcr:uuid]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql1, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        String sql2 = "SELECT BASEx.[jcr:path] from [mode:nodeTypes] as BASEx " //
                      + "JOIN  [mode:nodeTypes] AS REFx ON REFx.[jcr:primaryType] = BASEx.[jcr:uuid]";
        query = session.getWorkspace().getQueryManager().createQuery(sql2, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        String sql = sql1 + " UNION " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        sql = sql2 + " UNION " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        sql = sql1 + " INTERSECT " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        sql = sql2 + " INTERSECT " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        sql = sql1 + " INTERSECT " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        sql = sql2 + " INTERSECT " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        sql = sql1 + " EXCEPT " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        sql = sql2 + " EXCEPT " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());
    }

    @FixFor( "MODE-2297" )
    @Test
    public void shouldExecuteQueryUsingSetOperationOfQueriesWithJoins() throws RepositoryException {
        // Make sure that /Other/NodeA[3] contains references to /Other/NodeA and /Other/NodeA[2] ...
        Node nodeA1 = session.getNode("/Other/NodeA");
        Node nodeA2 = session.getNode("/Other/NodeA[2]");
        Node nodeA3 = session.getNode("/Other/NodeA[3]");
        assertThat(nodeA3.getProperty("otherNode").getNode(), is(sameInstance(nodeA1)));
        assertThat(nodeA3.getProperty("otherNodes").getValues()[0].getString(), is(nodeA2.getIdentifier()));
        assertThat(nodeA3.getProperty("otherNodes").getValues()[1].getString(), is(nodeA3.getIdentifier()));

        // print = true;
        String sql1 = "SELECT nodeA.[jcr:path] FROM [nt:unstructured] AS other JOIN [nt:unstructured] AS nodeA ON ISCHILDNODE(nodeA,other) WHERE PATH(other) = '/Other'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql1, Query.JCR_SQL2);
        validateQuery().rowCount(4).validate(query, query.execute());

        String sql2 = "SELECT category.[jcr:path] FROM [nt:unstructured] AS category JOIN [nt:unstructured] AS cars ON ISCHILDNODE(category,cars) WHERE PATH(cars) = '/Cars'";
        query = session.getWorkspace().getQueryManager().createQuery(sql2, Query.JCR_SQL2);
        validateQuery().rowCount(4).validate(query, query.execute());

        String sql = sql1 + " UNION " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(8).validate(query, query.execute());

        sql = sql2 + " UNION " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(8).validate(query, query.execute());

        sql = sql1 + " INTERSECT " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        sql = sql2 + " INTERSECT " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        sql = sql1 + " EXCEPT " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(4).validate(query, query.execute());

        sql = sql2 + " EXCEPT " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(4).validate(query, query.execute());
    }

    @FixFor( "MODE-2297" )
    @Test
    public void shouldExecuteQueryUsingSetOperationOfQueriesWithUnnecessaryIdentityJoinViaSameNodeJoinCriteria()
        throws RepositoryException {
        // Make sure that /Other/NodeA[3] contains references to /Other/NodeA and /Other/NodeA[2] ...
        Node nodeA1 = session.getNode("/Other/NodeA");
        Node nodeA2 = session.getNode("/Other/NodeA[2]");
        Node nodeA3 = session.getNode("/Other/NodeA[3]");
        assertThat(nodeA3.getProperty("otherNode").getNode(), is(sameInstance(nodeA1)));
        assertThat(nodeA3.getProperty("otherNodes").getValues()[0].getString(), is(nodeA2.getIdentifier()));
        assertThat(nodeA3.getProperty("otherNodes").getValues()[1].getString(), is(nodeA3.getIdentifier()));

        // print = true;
        String sql1 = "SELECT nodeA.[jcr:path] FROM [nt:unstructured] AS other "
                      + "JOIN [nt:unstructured] AS nodeA ON ISCHILDNODE(nodeA,other) "//
                      + "JOIN [nt:unstructured] AS unused ON ISSAMENODE(other,unused) "//
                      + "WHERE PATH(other) = '/Other'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql1, Query.JCR_SQL2);
        validateQuery().rowCount(4).validate(query, query.execute());

        String sql2 = "SELECT category.[jcr:path] FROM [nt:unstructured] AS category "
                      + "JOIN [nt:unstructured] AS cars ON ISCHILDNODE(category,cars) "//
                      + "JOIN [nt:unstructured] AS unused ON ISSAMENODE(cars,unused) "//
                      + "WHERE PATH(cars) = '/Cars'";
        query = session.getWorkspace().getQueryManager().createQuery(sql2, Query.JCR_SQL2);
        validateQuery().rowCount(4).validate(query, query.execute());

        String sql = sql1 + " UNION " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(8).validate(query, query.execute());

        sql = sql2 + " UNION " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(8).validate(query, query.execute());

        sql = sql1 + " INTERSECT " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        sql = sql2 + " INTERSECT " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        sql = sql1 + " EXCEPT " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(4).validate(query, query.execute());

        sql = sql2 + " EXCEPT " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(4).validate(query, query.execute());
    }

    @FixFor( "MODE-2297" )
    @Test
    public void shouldExecuteQueryUsingSetOperationOfQueriesWithOneSideReturningNoResults() throws RepositoryException {
        // Make sure that /Other/NodeA[3] contains references to /Other/NodeA and /Other/NodeA[2] ...
        Node nodeA1 = session.getNode("/Other/NodeA");
        Node nodeA2 = session.getNode("/Other/NodeA[2]");
        Node nodeA3 = session.getNode("/Other/NodeA[3]");
        assertThat(nodeA3.getProperty("otherNode").getNode(), is(sameInstance(nodeA1)));
        assertThat(nodeA3.getProperty("otherNodes").getValues()[0].getString(), is(nodeA2.getIdentifier()));
        assertThat(nodeA3.getProperty("otherNodes").getValues()[1].getString(), is(nodeA3.getIdentifier()));

        // print = true;
        String sql1 = "SELECT nodeA.[jcr:path] FROM [nt:unstructured] AS other "
                      + "JOIN [nt:unstructured] AS nodeA ON ISCHILDNODE(nodeA,other) "//
                      + "JOIN [nt:unstructured] AS unused ON ISSAMENODE(other,unused) "//
                      + "WHERE PATH(other) = '/Other'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql1, Query.JCR_SQL2);
        validateQuery().rowCount(4).validate(query, query.execute());

        String sql2 = "SELECT category.[jcr:path] FROM [nt:unstructured] AS category "
                      + "JOIN [nt:unstructured] AS cars ON ISCHILDNODE(category,cars) "//
                      + "JOIN [nt:unstructured] AS unused ON ISSAMENODE(cars,unused) "//
                      + "WHERE PATH(cars) = '/NonExistantNode'";
        query = session.getWorkspace().getQueryManager().createQuery(sql2, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        String sql = sql1 + " UNION " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(4).validate(query, query.execute());

        sql = sql2 + " UNION " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(4).validate(query, query.execute());

        sql = sql1 + " INTERSECT " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        sql = sql2 + " INTERSECT " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        sql = sql1 + " EXCEPT " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(4).validate(query, query.execute());

        sql = sql2 + " EXCEPT " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());
    }

    @FixFor( "MODE-2297" )
    @Test
    public void shouldExecuteQueryUsingSetOperationOfQueriesWithUnnecessaryIdentityJoinViaEquiJoinCriteria()
        throws RepositoryException {
        // Make sure that /Other/NodeA[3] contains references to /Other/NodeA and /Other/NodeA[2] ...
        Node nodeA1 = session.getNode("/Other/NodeA");
        Node nodeA2 = session.getNode("/Other/NodeA[2]");
        Node nodeA3 = session.getNode("/Other/NodeA[3]");
        assertThat(nodeA3.getProperty("otherNode").getNode(), is(sameInstance(nodeA1)));
        assertThat(nodeA3.getProperty("otherNodes").getValues()[0].getString(), is(nodeA2.getIdentifier()));
        assertThat(nodeA3.getProperty("otherNodes").getValues()[1].getString(), is(nodeA3.getIdentifier()));

        // print = true;
        String sql1 = "SELECT nodeA.[jcr:path] FROM [nt:unstructured] AS other "
                      + "JOIN [nt:unstructured] AS nodeA ON ISCHILDNODE(nodeA,other) "//
                      + "JOIN [nt:unstructured] AS unused ON other.[jcr:uuid] = unused.[jcr:uuid] "//
                      + "WHERE PATH(other) = '/Other'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql1, Query.JCR_SQL2);
        validateQuery().rowCount(4).validate(query, query.execute());

        String sql2 = "SELECT category.[jcr:path] FROM [nt:unstructured] AS category "
                      + "JOIN [nt:unstructured] AS cars ON ISCHILDNODE(category,cars) "//
                      + "JOIN [nt:unstructured] AS unused ON cars.[jcr:uuid] = unused.[jcr:uuid] "//
                      + "WHERE PATH(cars) = '/Cars'";
        query = session.getWorkspace().getQueryManager().createQuery(sql2, Query.JCR_SQL2);
        validateQuery().rowCount(4).validate(query, query.execute());

        String sql = sql1 + " UNION " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(8).validate(query, query.execute());

        sql = sql2 + " UNION " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(8).validate(query, query.execute());

        sql = sql1 + " INTERSECT " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        sql = sql2 + " INTERSECT " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        sql = sql1 + " EXCEPT " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(4).validate(query, query.execute());

        sql = sql2 + " EXCEPT " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(4).validate(query, query.execute());
    }

    @FixFor( "MODE-2297" )
    @Test
    public void shouldExecuteQueryUsingSetOperationOfQueriesWithNonSimilarQueries() throws RepositoryException {
        // Make sure that /Other/NodeA[3] contains references to /Other/NodeA and /Other/NodeA[2] ...
        Node nodeA1 = session.getNode("/Other/NodeA");
        Node nodeA2 = session.getNode("/Other/NodeA[2]");
        Node nodeA3 = session.getNode("/Other/NodeA[3]");
        assertThat(nodeA3.getProperty("otherNode").getNode(), is(sameInstance(nodeA1)));
        assertThat(nodeA3.getProperty("otherNodes").getValues()[0].getString(), is(nodeA2.getIdentifier()));
        assertThat(nodeA3.getProperty("otherNodes").getValues()[1].getString(), is(nodeA3.getIdentifier()));

        // print = true;
        String sql1 = "SELECT nodeA.[jcr:path] FROM [nt:unstructured] AS other "
                      + "JOIN [nt:unstructured] AS nodeA ON ISCHILDNODE(nodeA,other) "//
                      + "JOIN [nt:unstructured] AS unused ON other.[jcr:uuid] = unused.[jcr:uuid] "//
                      + "WHERE PATH(other) = '/Other'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql1, Query.JCR_SQL2);
        validateQuery().rowCount(4).validate(query, query.execute());

        String sql2 = "SELECT base.[jcr:path] FROM [nt:unstructured] AS base "
                      + "JOIN [nt:unstructured] AS unused ON base.[jcr:uuid] = unused.[jcr:uuid] ";
        query = session.getWorkspace().getQueryManager().createQuery(sql2, Query.JCR_SQL2);
        validateQuery().rowCount(24).validate(query, query.execute());

        String sql = sql1 + " UNION " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(24).validate(query, query.execute());

        sql = sql2 + " UNION " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(24).validate(query, query.execute());

        sql = sql1 + " INTERSECT " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(4).validate(query, query.execute());

        sql = sql2 + " INTERSECT " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(4).validate(query, query.execute());

        sql = sql1 + " EXCEPT " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        sql = sql2 + " EXCEPT " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(20).validate(query, query.execute());
    }

    @FixFor( "MODE-2297" )
    @Test
    public void shouldExecuteQueryUsingSetOperationOfQueriesWithConstraintsOnNonExistantProperties() throws RepositoryException {
        // print = true;
        String sql1 = "SELECT BASE.[jcr:path] from [nt:propertyDefinition] as BASE " //
                      + "JOIN  [nt:nodeType] AS A ON ISCHILDNODE(BASE,A) " //
                      + "JOIN [nt:unstructured] AS B ON B.[jcr:uuid] = A.[jcr:uuid] " //
                      + "WHERE B.[jcr:multiple] = true";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql1, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        String sql2 = "SELECT BASE.[jcr:path] from [nt:unstructured] as BASE " //
                      + "JOIN  [nt:nodeType] AS A ON ISCHILDNODE(BASE,A) " //
                      + "JOIN [nt:unstructured] AS B ON B.[jcr:uuid] = A.[jcr:uuid] " //
                      + "JOIN  [nt:unstructured] AS C ON C.x = BASE.[jcr:uuid] " //
                      + "WHERE A.propertyThatHasntToExistsAtAll = '2'";
        query = session.getWorkspace().getQueryManager().createQuery(sql2, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        String sql = sql1 + " UNION " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        sql = sql2 + " UNION " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        sql = sql1 + " INTERSECT " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        sql = sql2 + " INTERSECT " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        sql = sql1 + " EXCEPT " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());

        sql = sql2 + " EXCEPT " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldFindNodeByUuid() throws RepositoryException {
        // Find one of the car nodes ...
        Node nodeA = session.getNode("/Other/NodeA[2]");
        assertThat(nodeA, is(notNullValue()));
        String uuid = nodeA.getUUID(); // throws exception if not referenceable ...

        String sql = "SELECT * FROM [nt:unstructured] WHERE [jcr:uuid] = '" + uuid + "'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns(allColumnNames("nt:unstructured")).validate(query, result);

        sql = "SELECT * FROM nt:unstructured WHERE jcr:uuid = '" + uuid + "'";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = query.execute();
        validateQuery().rowCount(1).validate(query, result);
    }

    @Test
    public void shouldFailToSkipBeyondSize() throws RepositoryException {
        String sql = "SELECT * FROM [nt:base] WHERE [car:year] < 2009";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        final int count = 13;
        validateQuery().rowCount(count).warnings(1).hasColumns(allColumnNames("nt:base")).validate(query, result);

        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        result = query.execute();
        NodeIterator iter = result.getNodes();
        assertTrue(iter.hasNext());
        assertThat(iter.next(), is(notNullValue()));
        iter.skip(count - 2);
        assertTrue(iter.hasNext());
        assertThat(iter.next(), is(notNullValue()));
        // we're at the next one ...
        assertThat(iter.hasNext(), is(false));
        try {
            iter.skip(3);
            fail("Expected NoSuchElementException if skipping past end");
        } catch (NoSuchElementException e) {
            // expected
        }
        // Do it again and skip past the end ...
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        result = query.execute();
        iter = result.getNodes();
        try {
            iter.skip(count + 1);
            fail("Expected NoSuchElementException if skipping past end");
        } catch (NoSuchElementException e) {
            // expected
        }
    }

    @FixFor( "MODE-1901" )
    @Test
    public void shouldExplainQueryWithoutExecutingQuery() throws RepositoryException {
        String sql = "SELECT * FROM [nt:file]";
        org.modeshape.jcr.api.query.Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        org.modeshape.jcr.api.query.QueryResult result = query.explain();
        validateQuery().rowCount(0).warnings(0).onlyQueryPlan().validate(query, result);
    }

    @FixFor( "MODE-1888" )
    @Test
    public void shouldCaptureWarningsAboutPotentialTypos() throws RepositoryException {
        String sql = "SELECT [jcr.uuid] FROM [nt:file]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(0).warnings(1).hasColumns("jcr.uuid").validate(query, result);
    }

    @FixFor( "MODE-1888" )
    @Test
    public void shouldCaptureWarningsAboutUsingMisspelledColumnOnWrongSelector() throws RepositoryException {
        String sql = "SELECT file.[jcr.uuid] FROM [nt:file] AS file JOIN [mix:referenceable] AS ref ON ISSAMENODE(file,ref)";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(0).warnings(1).hasColumns("file.jcr.uuid").validate(query, result);
    }

    @FixFor( "MODE-1888" )
    @Test
    public void shouldCaptureWarningsAboutUsingColumnOnWrongSelector() throws RepositoryException {
        String sql = "SELECT ref.[jcr:lastModified] FROM [nt:file] AS file JOIN [mix:referenceable] AS ref ON ISSAMENODE(file,ref)";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(0).warnings(1).hasColumns("ref.jcr:lastModified").validate(query, result);
    }

    @FixFor( {"MODE-1888", "MODE-2297"} )
    @Test
    public void shouldNotCaptureWarningsAboutUsingJcrUuidColumnOnWrongSelector() throws RepositoryException {
        String sql = "SELECT file.[jcr:uuid] FROM [nt:file] AS file JOIN [mix:referenceable] AS ref ON ISSAMENODE(file,ref)";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(0).warnings(0).hasColumns("file.jcr:uuid").validate(query, result);
    }

    @FixFor( "MODE-1888" )
    @Test
    public void shouldCaptureWarningAboutUseOfResidualProperties() throws RepositoryException {
        String sql = "SELECT [foo_bar] FROM [nt:unstructured]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(24).warnings(1).hasColumns("foo_bar").validate(query, result);
    }

    @FixFor( "MODE-1888" )
    @Test
    public void shouldNotCaptureWarningAboutUseOfPseudoColumns() throws RepositoryException {
        String sql = "SELECT [jcr:path] FROM [nt:unstructured]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(24).noWarnings().hasColumns("jcr:path").validate(query, result);
    }

    @FixFor( "MODE-1671" )
    @Test
    public void shouldNotCaptureWarningAboutUseOfNodeIdPseudoColumn() throws RepositoryException {
        String sql = "SELECT [mode:id] FROM [nt:unstructured]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(24).noWarnings().hasColumns("mode:id").validate(query, result);
    }

    @FixFor( "MODE-1888" )
    @Test
    public void shouldCaptureWarningAboutUseOfPseudoColumnWithPeriodInsteadOfColonDelimiter() throws RepositoryException {
        String sql = "SELECT [jcr.path] FROM [nt:unstructured]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(24).warnings(1).validate(query, result);
    }

    @FixFor( "MODE-1888" )
    @Test
    public void shouldCaptureWarningAboutUseOfPseudoColumnWithUnderscoreInsteadOfColonDelimiter() throws RepositoryException {
        String sql = "SELECT [jcr_path] FROM [nt:unstructured]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(24).warnings(1).hasColumns("jcr_path").validate(query, result);
    }

    @FixFor( "MODE-1671" )
    @Test
    public void shouldReturnResolvableNodeIdentifierFromQuery() throws RepositoryException {
        String sql = "SELECT [mode:id] AS reallylongvaluethatwillprintcompletely  FROM [nt:unstructured]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        final Map<String, Node> nodesById = new HashMap<>();
        validateQuery().rowCount(24).noWarnings().hasColumns("reallylongvaluethatwillprintcompletely").onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                String id = row.getValue("mode:id").getString();
                Node nodeFromQuery = row.getNode();
                Node resolvedNode = session().getNodeByIdentifier(id);
                assertSame(nodeFromQuery, resolvedNode);
                nodesById.put(id, nodeFromQuery);
            }
        }).validate(query, result);

        for (Map.Entry<String, Node> entry : nodesById.entrySet()) {
            final String id = entry.getKey();
            final Node expectedNode = entry.getValue();

            sql = "SELECT [mode:id] AS reallylongvaluethatwillprintcompletely FROM [nt:unstructured] WHERE [mode:id] = '" + id
                  + "'";
            query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            result = query.execute();
            validateQuery().rowCount(1).noWarnings().hasColumns("reallylongvaluethatwillprintcompletely")
                           .onEachRow(new Predicate() {
                               @Override
                               public void validate( int rowNumber,
                                                     Row row ) throws RepositoryException {
                                   String id = row.getValue("mode:id").getString();
                                   Node nodeFromQuery = row.getNode();
                                   String nodeId = nodeFromQuery.getIdentifier();
                                   assertSame(nodeFromQuery, expectedNode);
                                   assertSame(nodeId, id);
                               }
                           }).validate(query, result);

            sql = "SELECT [mode:id] FROM [nt:unstructured] WHERE [mode:id] = $id";
            query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            query.bindValue("id", session().getValueFactory().createValue(id));
            result = query.execute();
            validateQuery().rowCount(1).noWarnings().hasColumns("mode:id").onEachRow(new Predicate() {
                @Override
                public void validate( int rowNumber,
                                      Row row ) throws RepositoryException {
                    String id = row.getValue("mode:id").getString();
                    Node nodeFromQuery = row.getNode();
                    assertSame(nodeFromQuery, expectedNode);
                    assertSame(nodeFromQuery.getIdentifier(), id);
                }
            }).validate(query, result);
        }
    }

    @FixFor( "MODE-1888" )
    @Test
    public void shouldCaptureWarningAboutUseOfNonPluralJcrMixinTypeColumn() throws RepositoryException {
        String sql = "SELECT [jcr:mixinType] FROM [nt:unstructured]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(24).warnings(1).hasColumns("jcr:mixinType").validate(query, result);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllNodes() throws RepositoryException {
        String sql = "SELECT * FROM [nt:base]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = allColumnNames("nt:base");
        validateQuery().rowCount(totalNodeCount).noWarnings().hasColumns(columnNames).validate(query, result);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllNodesWithOrderByPath() throws RepositoryException {
        String sql = "SELECT * FROM [nt:base] ORDER BY [jcr:path]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = allColumnNames("nt:base");
        validateQuery().rowCount(totalNodeCount).noWarnings().hasColumns(columnNames).validate(query, result);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllNodesWithOrderByPathUsingAlias() throws RepositoryException {
        String sql = "SELECT * FROM [nt:base] AS all ORDER BY all.[jcr:path]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = allColumnNames("all");
        validateQuery().rowCount(totalNodeCount).noWarnings().hasColumns(columnNames).validate(query, result);
    }

    @FixFor( "MODE-1671" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllNodesWithOrderByNodeId() throws RepositoryException {
        String sql = "SELECT * FROM [nt:base] ORDER BY [mode:id]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = allColumnNames("nt:base");
        validateQuery().rowCount(totalNodeCount).noWarnings().hasColumns(columnNames).validate(query, result);
    }

    @FixFor( "MODE-1671" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllNodesWithOrderByNodeIdUsingAlias() throws RepositoryException {
        String sql = "SELECT * FROM [nt:base] AS all ORDER BY all.[mode:id]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = allColumnNames("all");
        validateQuery().rowCount(totalNodeCount).noWarnings().hasColumns(columnNames).validate(query, result);
    }

    @FixFor( "MODE-1095" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithOrderByUsingPseudoColumnWithSelectStar() throws RepositoryException {
        String sql = "SELECT * FROM [car:Car] WHERE [car:year] < 2009 ORDER BY [jcr:path]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = carColumnNames("car:Car");
        validateQuery().rowCount(13).noWarnings().hasColumns(columnNames).validate(query, result);
    }

    @FixFor( "MODE-1095" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithOrderByUsingColumnWithSelectStar() throws RepositoryException {
        String sql = "SELECT * FROM [car:Car] WHERE [car:year] < 2009 ORDER BY [car:year]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = carColumnNames("car:Car");
        validateQuery().rowCount(13).noWarnings().hasColumns(columnNames).validate(query, result);
    }

    @FixFor( "MODE-1095" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithOrderByUsingColumnNotInSelect() throws RepositoryException {
        String sql = "SELECT [car:model], [car:maker] FROM [car:Car] WHERE [car:year] <= 2012 ORDER BY [car:year] DESC";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = {"car:model", "car:maker", "car:year"};
        validateQuery().rowCount(13).noWarnings().hasColumns(columnNames).validate(query, result);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithOrderByOnResidualColumn() throws RepositoryException {
        String sql = "SELECT x.* FROM [nt:unstructured] AS x ORDER BY x.propC";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(24).warnings(1).hasColumns(allOf(allColumnNames("x"), new String[] {"propC"}))
                       .validate(query, result);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithOrderByOnResidualColumnAndNoAlias() throws RepositoryException {
        String sql = "SELECT * FROM [nt:unstructured] ORDER BY propC";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = allOf(allColumnNames("nt:unstructured"), new String[] {"propC"});
        validateQuery().rowCount(24).warnings(1).hasColumns(columnNames).validate(query, result);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2Query() throws RepositoryException {
        String sql = "SELECT * FROM [nt:unstructured]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = allColumnNames("nt:unstructured");
        validateQuery().rowCount(24).warnings(0).hasColumns(columnNames).validate(query, result);
    }

    @FixFor( "MODE-1095" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithJoinCriteriaOnColumnsInSelect() throws RepositoryException {
        String sql = "SELECT x.*, x.somethingElse, y.*, y.propC FROM [nt:unstructured] AS x INNER JOIN [nt:unstructured] AS y ON x.somethingElse = y.propC";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = allOf(allColumnNames("x"), allColumnNames("y"), new String[] {"x.somethingElse", "y.propC"});
        validateQuery().rowCount(1).warnings(2).withRows().withRow("/Other/NodeA", "/Other/NodeA[2]").endRows()
                       .hasColumns(columnNames).validate(query, result);
    }

    @FixFor( "MODE-1095" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithJoinCriteriaOnColumnsInSelectAndOrderBy()
        throws RepositoryException {
        String sql = "SELECT x.*, y.* FROM [nt:unstructured] AS x INNER JOIN [nt:unstructured] AS y ON x.somethingElse = y.propC ORDER BY x.propC";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = allOf(allColumnNames("x"), allColumnNames("y"), new String[] {"x.propC"});
        validateQuery().rowCount(1).warnings(3).withRows().withRow("/Other/NodeA", "/Other/NodeA[2]").endRows()
                       .hasColumns(columnNames).validate(query, result);
    }

    @FixFor( "MODE-1095" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithJoinCriteriaOnSomeColumnsInSelect() throws RepositoryException {
        String sql = "SELECT y.*, y.propC FROM [nt:unstructured] AS x INNER JOIN [nt:unstructured] AS y ON x.somethingElse = y.propC";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = allOf(allColumnNames("y"), new String[] {"y.propC"});
        validateQuery().rowCount(1).warnings(2).withRows().withRow("/Other/NodeA[2]").endRows().hasColumns(columnNames)
                       .validate(query, result);
    }

    @FixFor( {"MODE-1095", "MODE-1680"} )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithOrderByAndJoinCriteriaOnColumnsNotInSelect()
        throws RepositoryException {
        String sql = "SELECT y.*, y.propC FROM [nt:unstructured] AS x INNER JOIN [nt:unstructured] AS y ON x.somethingElse = y.propC ORDER BY x.propC";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = allOf(allColumnNames("y"), new String[] {"y.propC"});
        validateQuery().rowCount(1).warnings(3).hasColumns(columnNames).hasNodesAtPaths("/Other/NodeA[2]")
                       .validate(query, result);
    }

    @FixFor( "MODE-1055" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllNodesWithCriteria() throws RepositoryException {
        String sql = "SELECT * FROM [nt:base] WHERE [car:year] < 2009";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(13).warnings(1).hasColumns(allColumnNames("nt:base")).validate(query, result);
    }

    @FixFor( "MODE-1055" )
    @Test
    public void shouldReturnNullValuesForNonExistantPropertiesInSelectClauseOfJcrSql2Query() throws RepositoryException {
        String sql = "SELECT bogus, laughable, [car:year] FROM [nt:base] WHERE [car:year] < 2009";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = {"bogus", "laughable", "car:year"};
        validateQuery().rowCount(13).warnings(3).hasColumns(columnNames).onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                assertThat(row.getValue("bogus"), is(nullValue()));
                assertThat(row.getValue("laughable"), is(nullValue()));
                assertThat(row.getValue("car:year"), is(not(nullValue())));
            }
        }).validate(query, result);
    }

    @FixFor( "MODE-1055" )
    @Test
    public void shouldNotMatchNodesWhenQueryUsesNonExistantPropertyInCriteriaInSelectClauseOfJcrSql2Query()
        throws RepositoryException {
        String sql = "SELECT bogus, laughable, [car:year] FROM [nt:base] WHERE argle < 2009";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = {"bogus", "laughable", "car:year"};
        validateQuery().rowCount(0).warnings(4).hasColumns(columnNames).validate(query, result);
    }

    @FixFor( "MODE-1055" )
    @Test
    public void shouldNotOrderByNonExistantPropertyInCriteriaInSelectClauseOfJcrSql2Query() throws RepositoryException {
        String sql = "SELECT bogus, laughable, [car:year] FROM [nt:base] ORDER BY argle";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = {"bogus", "laughable", "car:year", "argle"};
        validateQuery().rowCount(totalNodeCount).warnings(4).hasColumns(columnNames).validate(query, result);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllCarNodes() throws RepositoryException {
        String sql = "SELECT * FROM [car:Car]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(13).hasColumns(carColumnNames("car:Car")).validate(query, result);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllCarNodesOrderedByYear() throws RepositoryException {
        String sql = "SELECT * FROM [car:Car] ORDER BY [car:year]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(13).hasColumns(carColumnNames("car:Car")).validate(query, result);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllCarNodesOrderedByDescendingLexicographicalPriceAndNullsFirst()
        throws RepositoryException {
        String sql = "SELECT * FROM [car:Car] ORDER BY [car:msrp] DESC NULLS FIRST";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(13).hasColumns(carColumnNames("car:Car")).onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                // Results are sorted by lexicographic MSRP (as a string, not as a number)!!!
                if (rowNumber == 1) {
                    assertThat(row.getValue("car:model").getString(), is("DTS"));
                    assertThat(row.getValue("car:msrp"), is(nullValue()));
                    assertThat(row.getValue("car:mpgCity"), is(nullValue()));
                } else if (rowNumber == 2) {
                    assertThat(row.getValue("car:model").getString(), is("LR3"));
                    assertThat(row.getValue("car:msrp").getString(), is("$48,525"));
                    assertThat(row.getValue("car:mpgCity").getLong(), is(12L));
                } else if (rowNumber == 3) {
                    assertThat(row.getValue("car:model").getString(), is("IS350"));
                    assertThat(row.getValue("car:msrp").getString(), is("$36,305"));
                    assertThat(row.getValue("car:mpgCity").getLong(), is(18L));
                } else if (rowNumber == 11) {
                    assertThat(row.getValue("car:model").getString(), is("DB9"));
                    assertThat(row.getValue("car:msrp").getString(), is("$171,600"));
                    assertThat(row.getValue("car:mpgCity").getLong(), is(12L));
                }
            }
        }).validate(query, result);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllCarNodesOrderedByDescendingLexicographicalPriceAndNullsLast()
        throws RepositoryException {
        String sql = "SELECT * FROM [car:Car] ORDER BY [car:msrp] DESC NULLS LAST";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(13).hasColumns(carColumnNames("car:Car")).onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                // Results are sorted by lexicographic MSRP (as a string, not as a number)!!!
                if (rowNumber == 1) {
                    assertThat(row.getValue("car:model").getString(), is("LR3"));
                    assertThat(row.getValue("car:msrp").getString(), is("$48,525"));
                    assertThat(row.getValue("car:mpgCity").getLong(), is(12L));
                } else if (rowNumber == 2) {
                    assertThat(row.getValue("car:model").getString(), is("IS350"));
                    assertThat(row.getValue("car:msrp").getString(), is("$36,305"));
                    assertThat(row.getValue("car:mpgCity").getLong(), is(18L));
                } else if (rowNumber == 10) {
                    assertThat(row.getValue("car:model").getString(), is("DB9"));
                    assertThat(row.getValue("car:msrp").getString(), is("$171,600"));
                    assertThat(row.getValue("car:mpgCity").getLong(), is(12L));
                } else if (rowNumber == 13) {
                    assertThat(row.getValue("car:model").getString(), is("DTS"));
                    assertThat(row.getValue("car:msrp"), is(nullValue()));
                    assertThat(row.getValue("car:mpgCity"), is(nullValue()));
                }
            }
        }).validate(query, result);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllCarNodesOrderedByAscendingLexicographicalPriceAndNullsFirst()
        throws RepositoryException {
        String sql = "SELECT * FROM [car:Car] ORDER BY [car:msrp] ASC NULLS FIRST";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(13).hasColumns(carColumnNames("car:Car")).onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                // Results are sorted by lexicographic MSRP (as a string, not as a number)!!!
                if (rowNumber == 1) {
                    assertThat(row.getValue("car:model").getString(), is("DTS"));
                    assertThat(row.getValue("car:msrp"), is(nullValue()));
                    assertThat(row.getValue("car:mpgCity"), is(nullValue()));
                } else if (rowNumber == 13) {
                    assertThat(row.getValue("car:model").getString(), is("LR3"));
                    assertThat(row.getValue("car:msrp").getString(), is("$48,525"));
                    assertThat(row.getValue("car:mpgCity").getLong(), is(12L));
                } else if (rowNumber == 12) {
                    assertThat(row.getValue("car:model").getString(), is("IS350"));
                    assertThat(row.getValue("car:msrp").getString(), is("$36,305"));
                    assertThat(row.getValue("car:mpgCity").getLong(), is(18L));
                } else if (rowNumber == 12) {
                    assertThat(row.getValue("car:model").getString(), is("DB9"));
                    assertThat(row.getValue("car:msrp").getString(), is("$171,600"));
                    assertThat(row.getValue("car:mpgCity").getLong(), is(12L));
                }
            }
        }).validate(query, result);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllCarNodesOrderedByAscendingLexicographicalPriceAndNullsLast()
        throws RepositoryException {
        String sql = "SELECT * FROM [car:Car] ORDER BY [car:msrp] ASC NULLS LAST";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        // print = true;
        validateQuery().rowCount(13).hasColumns(carColumnNames("car:Car")).onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                // Results are sorted by lexicographic MSRP (as a string, not as a number)!!!
                if (rowNumber == 3) {
                    assertThat(row.getValue("car:model").getString(), is("DB9"));
                    assertThat(row.getValue("car:msrp").getString(), is("$171,600"));
                    assertThat(row.getValue("car:mpgCity").getLong(), is(12L));
                } else if (rowNumber == 11) {
                    assertThat(row.getValue("car:model").getString(), is("IS350"));
                    assertThat(row.getValue("car:msrp").getString(), is("$36,305"));
                    assertThat(row.getValue("car:mpgCity").getLong(), is(18L));
                } else if (rowNumber == 12) {
                    assertThat(row.getValue("car:model").getString(), is("LR3"));
                    assertThat(row.getValue("car:msrp").getString(), is("$48,525"));
                    assertThat(row.getValue("car:mpgCity").getLong(), is(12L));
                } else if (rowNumber == 13) {
                    assertThat(row.getValue("car:model").getString(), is("DTS"));
                    assertThat(row.getValue("car:msrp"), is(nullValue()));
                    assertThat(row.getValue("car:mpgCity"), is(nullValue()));
                }
            }
        }).validate(query, result);
    }

    @FixFor( "MODE-1234" )
    @Test
    public void shouldAllowEqualityCriteriaOnPropertyDefinedWithBooleanPropertyDefinition() throws RepositoryException {
        assertQueryWithBooleanValue(1, "");
        assertQueryWithBooleanValue(1, "WHERE [notion:booleanProperty] = true");
        assertQueryWithBooleanValue(0, "WHERE [notion:booleanProperty] = false");

        assertQueryWithBooleanValue(1, "WHERE [notion:booleanProperty] > false");
        assertQueryWithBooleanValue(1, "WHERE [notion:booleanProperty] >= false");
        assertQueryWithBooleanValue(0, "WHERE [notion:booleanProperty] > true");
        assertQueryWithBooleanValue(1, "WHERE [notion:booleanProperty] >= true");

        assertQueryWithBooleanValue(0, "WHERE [notion:booleanProperty] < false");
        assertQueryWithBooleanValue(0, "WHERE [notion:booleanProperty] <= false");
        assertQueryWithBooleanValue(0, "WHERE [notion:booleanProperty] < true");
        assertQueryWithBooleanValue(1, "WHERE [notion:booleanProperty] <= true");

        assertQueryWithBooleanValue(0, "WHERE [notion:booleanProperty2] = true");
        assertQueryWithBooleanValue(1, "WHERE [notion:booleanProperty2] = false");

        assertQueryWithBooleanValue(0, "WHERE [notion:booleanProperty2] > false");
        assertQueryWithBooleanValue(1, "WHERE [notion:booleanProperty2] >= false");
        assertQueryWithBooleanValue(0, "WHERE [notion:booleanProperty2] > true");
        assertQueryWithBooleanValue(0, "WHERE [notion:booleanProperty2] >= true");

        assertQueryWithBooleanValue(0, "WHERE [notion:booleanProperty2] < false");
        assertQueryWithBooleanValue(1, "WHERE [notion:booleanProperty2] <= false");
        assertQueryWithBooleanValue(1, "WHERE [notion:booleanProperty2] < true");
        assertQueryWithBooleanValue(1, "WHERE [notion:booleanProperty2] <= true");

    }

    protected void assertQueryWithBooleanValue( int numResults,
                                                String criteria ) throws RepositoryException {
        String[] columnNames = {"notion:booleanProperty", "notion:booleanProperty2"};
        String queryStr = "SELECT [notion:booleanProperty], [notion:booleanProperty2] FROM [notion:typed] AS node " + criteria;
        Query query = session.getWorkspace().getQueryManager().createQuery(queryStr, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(numResults).hasColumns(columnNames).validate(query, result);
    }

    @FixFor( "MODE-1611" )
    @Test
    public void shouldAllowQomEqualityCriteriaOnPropertyDefinedWithBooleanPropertyDefinition() throws RepositoryException {
        assertQomQueryWithBooleanValue(1, null, null, false);
        assertQomQueryWithBooleanValue(1, "notion:booleanProperty", JCR_OPERATOR_EQUAL_TO, true);
        assertQomQueryWithBooleanValue(0, "notion:booleanProperty", JCR_OPERATOR_EQUAL_TO, false);

        assertQomQueryWithBooleanValue(1, "notion:booleanProperty", JCR_OPERATOR_GREATER_THAN, false);
        assertQomQueryWithBooleanValue(1, "notion:booleanProperty", JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO, false);
        assertQomQueryWithBooleanValue(0, "notion:booleanProperty", JCR_OPERATOR_GREATER_THAN, true);
        assertQomQueryWithBooleanValue(1, "notion:booleanProperty", JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO, true);

        assertQomQueryWithBooleanValue(0, "notion:booleanProperty", JCR_OPERATOR_LESS_THAN, false);
        assertQomQueryWithBooleanValue(0, "notion:booleanProperty", JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO, false);
        assertQomQueryWithBooleanValue(0, "notion:booleanProperty", JCR_OPERATOR_LESS_THAN, true);
        assertQomQueryWithBooleanValue(1, "notion:booleanProperty", JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO, true);

        assertQomQueryWithBooleanValue(0, "notion:booleanProperty2", JCR_OPERATOR_EQUAL_TO, true);
        assertQomQueryWithBooleanValue(1, "notion:booleanProperty2", JCR_OPERATOR_EQUAL_TO, false);

        assertQomQueryWithBooleanValue(0, "notion:booleanProperty2", JCR_OPERATOR_GREATER_THAN, false);
        assertQomQueryWithBooleanValue(1, "notion:booleanProperty2", JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO, false);
        assertQomQueryWithBooleanValue(0, "notion:booleanProperty2", JCR_OPERATOR_GREATER_THAN, true);
        assertQomQueryWithBooleanValue(0, "notion:booleanProperty2", JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO, true);

        assertQomQueryWithBooleanValue(0, "notion:booleanProperty2", JCR_OPERATOR_LESS_THAN, false);
        assertQomQueryWithBooleanValue(1, "notion:booleanProperty2", JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO, false);
        assertQomQueryWithBooleanValue(1, "notion:booleanProperty2", JCR_OPERATOR_LESS_THAN, true);
        assertQomQueryWithBooleanValue(1, "notion:booleanProperty2", JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO, true);

    }

    protected void assertQomQueryWithBooleanValue( int numResults,
                                                   String propertyName,
                                                   String operator,
                                                   boolean propertyValue ) throws RepositoryException {
        QueryObjectModelFactory qomFactory = session.getWorkspace().getQueryManager().getQOMFactory();
        ValueFactory valueFactory = session.getValueFactory();
        Value propertyValueObj = valueFactory.createValue(propertyValue);

        Selector selector = qomFactory.selector("notion:typed", "node");
        Constraint constraint = null;
        if (propertyName != null && operator != null) {
            PropertyValue propValue = qomFactory.propertyValue("node", propertyName);
            Literal literal = qomFactory.literal(propertyValueObj);
            constraint = qomFactory.comparison(propValue, operator, literal);
        }
        Column[] columns = new Column[2];
        columns[0] = qomFactory.column("node", "notion:booleanProperty", "notion:booleanProperty");
        columns[1] = qomFactory.column("node", "notion:booleanProperty2", "notion:booleanProperty2");
        Ordering[] orderings = null;

        // Build and execute the query ...
        Query query = qomFactory.createQuery(selector, constraint, orderings, columns);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        String[] columnNames = {"notion:booleanProperty", "notion:booleanProperty2"};
        validateQuery().rowCount(numResults).hasColumns(columnNames).validate(query, result);
    }

    @FixFor( "MODE-1611" )
    @Test
    public void shouldAllowQomUseOfIsChildNodeInWhereClause() throws RepositoryException {
        QueryObjectModelFactory qomFactory = session.getWorkspace().getQueryManager().getQOMFactory();

        Selector selector = qomFactory.selector("nt:base", "category");
        ChildNode childNodeConstraint = qomFactory.childNode("category", "/Cars");
        Constraint constraint = childNodeConstraint;
        Column[] columns = new Column[0];
        Ordering[] orderings = null;

        // Build and execute the query ...
        Query query = qomFactory.createQuery(selector, constraint, orderings, columns);
        assertThat(query.getStatement(), is("SELECT * FROM [nt:base] AS category WHERE ISCHILDNODE(category,'/Cars')"));
        QueryResult result = query.execute();
        validateQuery().rowCount(4).hasColumns(allColumnNames("category")).validate(query, result);
    }

    @FixFor( "MODE-1057" )
    @Test
    public void shouldAllowEqualityNumericCriteriaOnPropertyDefinedWithNumericPropertyDefinition() throws RepositoryException {
        assertQueryWithLongValue(13, "");
        assertQueryWithLongValue(2, "WHERE [car:userRating] = 3");
        assertQueryWithLongValue(1, "WHERE [car:userRating] < 3");
        assertQueryWithLongValue(8, "WHERE [car:userRating] > 3");
        assertQueryWithLongValue(3, "WHERE [car:userRating] <= 3");
        assertQueryWithLongValue(10, "WHERE [car:userRating] >= 3");
        assertQueryWithLongValue(9, "WHERE [car:userRating] <> 3");
        assertQueryWithLongValue(9, "WHERE [car:userRating] != 3");
    }

    @FixFor( "MODE-1057" )
    @Test
    public void shouldAllowEqualityStringCriteriaOnPropertyDefinedWithNumericPropertyDefinition() throws RepositoryException {
        assertQueryWithLongValue(13, "");
        assertQueryWithLongValue(2, "WHERE [car:userRating] = '3'");
        assertQueryWithLongValue(1, "WHERE [car:userRating] < '3'");
        assertQueryWithLongValue(8, "WHERE [car:userRating] > '3'");
        assertQueryWithLongValue(3, "WHERE [car:userRating] <= '3'");
        assertQueryWithLongValue(10, "WHERE [car:userRating] >= '3'");
        assertQueryWithLongValue(9, "WHERE [car:userRating] <> '3'");
        assertQueryWithLongValue(9, "WHERE [car:userRating] != '3'");
    }

    protected void assertQueryWithLongValue( int numResults,
                                             String criteria ) throws RepositoryException {
        String[] columnNames = {"car:maker", "car:model", "car:year", "car:userRating"};
        String queryStr = "SELECT [car:maker], [car:model], [car:year], [car:userRating] FROM [car:Car] AS car " + criteria;
        Query query = session.getWorkspace().getQueryManager().createQuery(queryStr, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(numResults).hasColumns(columnNames).validate(query, result);
    }

    @FixFor( "MODE-2178" )
    @Test
    public void shouldAllowQueryForPropertyWithEmptyStringAsCriteria() throws RepositoryException {
        // print = true;
        String[] columnNames = {"car:engine", "car:maker", "car:model", "car:year", "car:userRating"};
        String queryStr = "SELECT [car:engine], [car:maker], [car:model], [car:year], [car:userRating] FROM [car:Car]";
        Query query = session.getWorkspace().getQueryManager().createQuery(queryStr, Query.JCR_SQL2);
        validateQuery().rowCount(13L).hasColumns(columnNames).validate(query, query.execute());

        queryStr = "SELECT [car:engine], [car:maker], [car:model], [car:year], [car:userRating] FROM [car:Car] WHERE LENGTH([car:engine]) = 0";
        query = session.getWorkspace().getQueryManager().createQuery(queryStr, Query.JCR_SQL2);
        validateQuery().rowCount(8L).hasColumns(columnNames).validate(query, query.execute());

        queryStr = "SELECT [car:engine], [car:maker], [car:model], [car:year], [car:userRating] FROM [car:Car] WHERE [car:engine] IS NULL OR [car:engine] = ''";
        query = session.getWorkspace().getQueryManager().createQuery(queryStr, Query.JCR_SQL2);
        validateQuery().rowCount(8L).hasColumns(columnNames).validate(query, query.execute());

        queryStr = "SELECT * FROM [car:Car] WHERE LENGTH([car:engine]) = 0";
        query = session.getWorkspace().getQueryManager().createQuery(queryStr, Query.JCR_SQL2);
        validateQuery().rowCount(8L).validate(query, query.execute());

        // There are some nodes that don't have a 'car:engine' property, but none of the existing property values is
        // an empty string, so this query should return no rows ...
        queryStr = "SELECT [car:engine], [car:maker], [car:model], [car:year], [car:userRating] FROM [car:Car] WHERE [car:engine] = ''";
        query = session.getWorkspace().getQueryManager().createQuery(queryStr, Query.JCR_SQL2);
        validateQuery().rowCount(0L).hasColumns(columnNames).validate(query, query.execute());

        Node car = session.getNode("/Cars/Hybrid/Toyota Prius");
        assertThat(car.hasProperty("car:engine"), is(false));
        try {
            // Change a node to have an empty 'car:engine' property value ...
            car.setProperty("car:engine", "");
            session.save();

            // Now issue the same query again, and this time we should have 1 row that has an empty 'car:engine' prop value ...
            query = session.getWorkspace().getQueryManager().createQuery(queryStr, Query.JCR_SQL2);
            validateQuery().rowCount(1L).hasColumns(columnNames).validate(query, query.execute());
        } finally {
            // Remove the property again ...
            car.getProperty("car:engine").remove();
            session.save();
        }
    }

    @FixFor( "MODE-1824" )
    @Test
    public void shouldBeAbleToExecuteQueryWithTwoColumns() throws RepositoryException {
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        QueryObjectModelFactory factory = queryManager.getQOMFactory();
        Selector car1Selector = factory.selector("car:Car", "car1");
        Selector car2Selector = factory.selector("car:Car", "car2");
        Join join = factory.join(car1Selector, car2Selector, QueryObjectModelConstants.JCR_JOIN_TYPE_INNER,
                                 factory.equiJoinCondition("car1", "car:maker", "car2", "car:maker"));
        Column[] columns = new Column[] {factory.column("car1", "car:maker", "maker"),
            factory.column("car2", "car:model", "model")};
        Query query = factory.createQuery(join, null, null, columns);
        QueryResult result = query.execute();
        String[] columnNames = {columns[0].getColumnName(), columns[1].getColumnName()}; // the aliases
        validateQuery().rowCount(21).hasColumns(columnNames).validate(query, result);
    }

    @Test
    public void shouldBeAbleToQueryWithLimitOfZeroOnNonJoin() throws RepositoryException {
        // Try with the LIMIT expression ...
        String sql = "SELECT [jcr:path] FROM [car:Car] LIMIT 0";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(0).hasColumns("jcr:path").validate(query, result);

        // Try with the method ...
        sql = "SELECT [jcr:path] FROM [car:Car]";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        query.setLimit(0);
        result = query.execute();
        validateQuery().rowCount(0).hasColumns("jcr:path").validate(query, result);
    }

    @Test
    public void shouldBeAbleToQueryWithTooLargeLimitOnNonJoin() throws RepositoryException {
        // Try with the LIMIT expression ...
        String sql = "SELECT [jcr:path] FROM [car:Car] LIMIT 100";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(13).hasColumns("jcr:path").validate(query, result);

        // Try with the method ...
        sql = "SELECT [jcr:path] FROM [car:Car]";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        query.setLimit(100);
        result = query.execute();
        validateQuery().rowCount(13).hasColumns("jcr:path").validate(query, result);
    }

    @Test
    public void shouldBeAbleToQueryWithLimitOnNonJoin() throws RepositoryException {
        // Try with the LIMIT expression ...
        String sql = "SELECT [jcr:path] FROM [car:Car] LIMIT 2";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(2).hasColumns("jcr:path").validate(query, result);

        // Try with the method ...
        sql = "SELECT [jcr:path] FROM [car:Car]";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        query.setLimit(2);
        result = query.execute();
        validateQuery().rowCount(2).hasColumns("jcr:path").validate(query, result);
    }

    @Test
    public void shouldBeAbleToQueryWithOffsetOnNonJoin() throws RepositoryException {
        // Try with the OFFSET expression ...
        String sql = "SELECT [jcr:path] FROM [car:Car] OFFSET 2";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(11).hasColumns("jcr:path").validate(query, result);

        // Try with the method ...
        sql = "SELECT [jcr:path] FROM [car:Car]";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        query.setOffset(2);
        result = query.execute();
        validateQuery().rowCount(11).hasColumns("jcr:path").validate(query, result);
    }

    @Test
    public void shouldBeAbleToQueryWithZeroOffsetOnNonJoin() throws RepositoryException {
        // Try with the OFFSET expression ...
        String sql = "SELECT [jcr:path] FROM [car:Car] OFFSET 0";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(13).hasColumns("jcr:path").validate(query, result);

        // Try with the method ...
        sql = "SELECT [jcr:path] FROM [car:Car]";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        query.setOffset(0);
        result = query.execute();
        validateQuery().rowCount(13).hasColumns("jcr:path").validate(query, result);
    }

    @Test
    public void shouldBeAbleToQueryWithTooLargeOffsetOnNonJoin() throws RepositoryException {
        // Try with the OFFSET expression ...
        String sql = "SELECT [jcr:path] FROM [car:Car] OFFSET 100";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(0).hasColumns("jcr:path").validate(query, result);

        // Try with the method ...
        sql = "SELECT [jcr:path] FROM [car:Car]";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        query.setOffset(100);
        result = query.execute();
        validateQuery().rowCount(0).hasColumns("jcr:path").validate(query, result);
    }

    @Test
    public void shouldBeAbleToQueryWithLimitAndOffsetOnNonJoin() throws RepositoryException {
        // Try with the OFFSET expression ...
        String sql = "SELECT [jcr:path] FROM [car:Car] ORDER BY [jcr:path]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        final String[] allCarPaths = {"/Cars/Hybrid/Nissan Altima", "/Cars/Hybrid/Toyota Highlander",
            "/Cars/Hybrid/Toyota Prius", "/Cars/Luxury/Bentley Continental", "/Cars/Luxury/Cadillac DTS",
            "/Cars/Luxury/Lexus IS350", "/Cars/Sports/Aston Martin DB9", "/Cars/Sports/Infiniti G37", "/Cars/Utility/Ford F-150",
            "/Cars/Utility/Hummer H3", "/Cars/Utility/Land Rover LR2", "/Cars/Utility/Land Rover LR3",
            "/Cars/Utility/Toyota Land Cruiser"};
        validateQuery().rowCount(13).hasColumns("jcr:path").hasNodesAtPaths(allCarPaths).validate(query, result);

        // Try with the OFFSET expression ...
        sql = "SELECT [jcr:path] FROM [car:Car] ORDER BY [jcr:path] LIMIT 2 OFFSET 2";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        result = query.execute();
        validateQuery().rowCount(2).hasColumns("jcr:path").hasNodesAtPaths(allCarPaths[2], allCarPaths[3])
                       .validate(query, result);

        // Try with the method ...
        sql = "SELECT [jcr:path] FROM [car:Car]";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        query.setOffset(2);
        result = query.execute();
        validateQuery().rowCount(11).hasColumns("jcr:path").validate(query, result);
    }

    @FixFor( "MODE-1865" )
    @Test
    public void shouldBeAbleToQueryWithLimit1() throws RepositoryException, Exception {
        Node top = session.getRootNode().addNode("top");
        for (int i = 0; i != 10; ++i) {
            Node parent = top.addNode("qwer" + i, "modetest:parent");
            parent.setProperty("modetest:parentField", 5L);
            Node intermediate = parent.getNode("modetest:folder");
            Node child = intermediate.addNode("asdf");
            child.setProperty("modetest:childField", i % 2 == 0 ? "bar" : "foo");
        }

        try {
            session.save();
            Thread.sleep(100L);
            String sql = "SELECT p.* AS parent FROM [modetest:parent] AS p INNER JOIN [modetest:child] AS c ON ISDESCENDANTNODE(c,p) WHERE p.[modetest:parentField] = CAST('5' AS LONG) AND c.[modetest:childField] = 'bar'";
            Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            QueryResult result = query.execute();
            validateQuery().rowCount(5).onEachRow(new Predicate() {
                @Override
                public void validate( int rowNumber,
                                      Row row ) throws RepositoryException {
                    // Path of all rows should have an even digit at the end ...
                    String path = row.getPath();
                    int lastIntValue = Integer.parseInt(path.substring(path.length() - 1));
                    assertTrue(lastIntValue % 2 == 0);
                }
            }).validate(query, result);

            // Try again but with LIMIT 1 (via method)...
            query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            query.setLimit(1L);
            result = query.execute();
            validateQuery().rowCount(1).validate(query, result);

            // Try again but with LIMIT 1 (via statement)...
            query = session.getWorkspace().getQueryManager().createQuery(sql + " LIMIT 1", Query.JCR_SQL2);
            result = query.execute();
            validateQuery().rowCount(1).validate(query, result);
        } finally {
            top.remove();
            session.save();
        }
    }

    @Test
    @FixFor( "MODE-2435" )
    public void shouldCorrectlyExecuteOrderByWithOffsetAndLimit() throws RepositoryException {
        // no order by
        String sql = "SELECT [jcr:path] FROM [car:Car] LIMIT 10 OFFSET 15";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        assertThat("result should contain zero rows", result.getRows().getSize(), is(0L));

        // with order by
        sql = "SELECT [jcr:path] FROM [car:Car] ORDER BY [jcr:path] LIMIT 10 OFFSET 15";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        result = query.execute();
        assertThat("result should contain zero rows", result.getRows().getSize(), is(0L));

        sql = "SELECT [jcr:path] FROM [car:Car] ORDER BY [jcr:path] LIMIT 1 OFFSET 0";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        result = query.execute();
        validateQuery().hasNodesAtPaths("/Cars/Hybrid/Nissan Altima").validate(query, result);

        sql = "SELECT [jcr:path] FROM [car:Car] ORDER BY [jcr:path] LIMIT 1 OFFSET 1";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        result = query.execute();
        validateQuery().hasNodesAtPaths("/Cars/Hybrid/Toyota Highlander").validate(query, result);

        sql = "SELECT [jcr:path] FROM [car:Car] ORDER BY [jcr:path] LIMIT 3 OFFSET 0";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        result = query.execute();
        validateQuery().hasNodesAtPaths("/Cars/Hybrid/Nissan Altima", 
                                        "/Cars/Hybrid/Toyota Highlander",
                                        "/Cars/Hybrid/Toyota Prius").validate(query, result);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllCarsUnderHybrid() throws RepositoryException {
        String sql = "SELECT car.[car:maker], car.[car:model], car.[car:year], car.[car:msrp], car.[jcr:path] FROM [car:Car] AS car WHERE PATH(car) LIKE '%/Hybrid/%' ORDER BY [car:model]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = {"car:maker", "car:model", "car:year", "car:msrp", "jcr:path"};
        validateQuery().rowCount(3).hasColumns(columnNames).onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                if (rowNumber == 1) {
                    assertThat(row.getValue("car:model").getString(), is("Altima"));
                    assertThat(row.getValue("car:msrp").getString(), is("$18,260"));
                    assertThat(row.getValue("car:year").getLong(), is(2008L));
                } else if (rowNumber == 2) {
                    assertThat(row.getValue("car:model").getString(), is("Highlander"));
                    assertThat(row.getValue("car:msrp").getString(), is("$34,200"));
                    assertThat(row.getValue("car:year").getLong(), is(2008L));
                } else if (rowNumber == 3) {
                    assertThat(row.getValue("car:model").getString(), is("Prius"));
                    assertThat(row.getValue("car:msrp").getString(), is("$21,500"));
                    assertThat(row.getValue("car:year").getLong(), is(2008L));
                }
            }
        }).validate(query, result);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithPathCriteria() throws RepositoryException {
        String sql = "SELECT [jcr:path] FROM [nt:unstructured] WHERE PATH() = '/Cars/Hybrid/Toyota Highlander'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:path").onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                assertThat(row.getNode().getPath(), is("/Cars/Hybrid/Toyota Highlander"));
            }
        }).validate(query, result);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryForNodeNamedHybrid() throws RepositoryException {
        String sql = "SELECT [jcr:path] FROM [nt:unstructured] AS hybrid WHERE NAME(hybrid) = 'Hybrid'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:path").onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                assertThat(row.getNode().getName(), is("Hybrid"));
            }
        }).validate(query, result);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryForNodeLocalNamedHybrid() throws RepositoryException {
        String sql = "SELECT [jcr:path] FROM [nt:unstructured] AS hybrid WHERE LOCALNAME(hybrid) = 'Hybrid'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:path").onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                assertThat(row.getNode().getName(), is("Hybrid"));
            }
        }).validate(query, result);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryUsingJoinToFindAllCarsUnderHybrid() throws RepositoryException {
        String sql = "SELECT car.[car:maker], car.[car:model], car.[car:year], car.[car:msrp] FROM [car:Car] AS car JOIN [nt:unstructured] AS hybrid ON ISCHILDNODE(car,hybrid) WHERE NAME(hybrid) = 'Hybrid' ORDER BY NAME(car)";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = {"car.car:maker", "car.car:model", "car.car:year", "car.car:msrp"};
        validateQuery().rowCount(3).hasColumns(columnNames).onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                if (rowNumber == 1) {
                    assertThat(row.getValue("car:model").getString(), is("Altima"));
                    assertThat(row.getValue("car:msrp").getString(), is("$18,260"));
                    assertThat(row.getValue("car:year").getLong(), is(2008L));
                } else if (rowNumber == 2) {
                    assertThat(row.getValue("car:model").getString(), is("Highlander"));
                    assertThat(row.getValue("car:msrp").getString(), is("$34,200"));
                    assertThat(row.getValue("car:year").getLong(), is(2008L));
                } else if (rowNumber == 3) {
                    assertThat(row.getValue("car:model").getString(), is("Prius"));
                    assertThat(row.getValue("car:msrp").getString(), is("$21,500"));
                    assertThat(row.getValue("car:year").getLong(), is(2008L));
                }
            }
        }).validate(query, result);
    }

    @FixFor( "MODE-1825" )
    @Test
    public void shouldBeAbleToExecuteQueryForAllColumns() throws RepositoryException {
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        QueryObjectModelFactory factory = queryManager.getQOMFactory();
        Selector car1Selector = factory.selector("car:Car", "car1");
        Selector car2Selector = factory.selector("car:Car", "car2");
        Join join = factory.join(car1Selector, car2Selector, QueryObjectModelConstants.JCR_JOIN_TYPE_INNER,
                                 factory.equiJoinCondition("car1", "car:maker", "car2", "car:maker"));
        Column[] columns = new Column[] {factory.column("car1", null, null)};
        Constraint constraint = factory.comparison(factory.propertyValue("car1", "car:maker"),
                                                   QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO,
                                                   factory.literal(session.getValueFactory().createValue("Toyota")));
        Ordering[] orderings = new Ordering[] {factory.descending(factory.propertyValue("car1", "car:year"))};
        Query query = factory.createQuery(join, constraint, orderings, columns);
        QueryResult result = query.execute();
        validateQuery().rowCount(9).hasColumns(carColumnNames("car1")).validate(query, result);
    }

    @FixFor( "MODE-1833" )
    @Test
    public void shouldBeAbleToQueryAllColumnsOnSimpleType() throws RepositoryException {
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        QueryObjectModelFactory factory = queryManager.getQOMFactory();
        Query query = factory.createQuery(factory.selector("modetest:simpleType", "type1"), null, null,
                                          new Column[] {factory.column("type1", null, null)});
        QueryResult result = query.execute();
        validateQuery().rowCount(0).validate(query, result);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllUnstructuredNodes() throws RepositoryException {
        String sql = "SELECT * FROM [nt:unstructured]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(24).hasColumns(allColumnNames("nt:unstructured")).validate(query, result);
    }

    @FixFor( "MODE-1309" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryUsingResidualPropertiesForJoinCriteria() throws RepositoryException {
        String sql = "SELECT x.propA AS pa, y.propB as pb FROM [nt:unstructured] AS x INNER JOIN [nt:unstructured] AS y ON x.propA = y.propB WHERE x.propA = 'value1'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        final Set<String> expectedPaths = new HashSet<String>();
        expectedPaths.add("/Other/NodeA[2]");
        expectedPaths.add("/Other/NodeA[3]");
        validateQuery().rowCount(2).hasColumns("pa", "pb").onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                // All the rows are identical ...
                assertThat(row.getValue("pa").getString(), is("value1"));
                assertThat(row.getValue("pb").getString(), is("value1"));
                // The path of the first column is the same ...
                assertThat(row.getNode("x").getPath(), is("/Other/NodeA"));
                // The path of the second selector will vary in each row ...
                assertThat(expectedPaths.remove(row.getNode("y").getPath()), is(true));
            }
        }).validate(query, result);
    }

    @FixFor( "MODE-1309" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QuerySelectingResidualProperty() throws RepositoryException {
        String sql = "SELECT a.propB FROM [nt:unstructured] AS a WHERE a.propB = 'value1'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(2).hasColumns("propB").onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                // All the rows are identical ...
                assertThat(row.getValue("propB").getString(), is("value1"));
            }
        }).validate(query, result);
    }

    @FixFor( "MODE-1309" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QuerySelectingResidualPropertyWithAlias() throws RepositoryException {
        String sql = "SELECT a.propB AS foo FROM [nt:unstructured] AS a WHERE a.propB = 'value1'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(2).hasColumns("foo").onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                // All the rows are identical ...
                assertThat(row.getValue("foo").getString(), is("value1"));
            }
        }).validate(query, result);
    }

    @FixFor( "MODE-1309" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QuerySelectingResidualPropertyWithAliasUsingAliasInConstraint()
        throws RepositoryException {
        String sql = "SELECT a.propB AS foo FROM [nt:unstructured] AS a WHERE a.foo = 'value1'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(2).hasColumns("foo").onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                // All the rows are identical ...
                assertThat(row.getValue("foo").getString(), is("value1"));
            }
        }).validate(query, result);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllUnstructuredNodesWithCriteriaOnMultiValuedProperty()
        throws RepositoryException {
        String sql = "SELECT * FROM [nt:unstructured] WHERE something = 'white dog' and something = 'black dog'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        // print = true;
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns(allColumnNames("nt:unstructured")).validate(query, result);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllUnstructuredNodesWithLikeCriteriaOnMultiValuedProperty()
        throws RepositoryException {
        String sql = "SELECT * FROM [nt:unstructured] WHERE something LIKE 'white%' and something LIKE 'black%'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        // print = true;
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns(allColumnNames("nt:unstructured")).validate(query, result);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithChildNodeJoin() throws RepositoryException {
        String sql = "SELECT car.* from [car:Car] as car JOIN [nt:unstructured] as category ON ISCHILDNODE(car,category) WHERE NAME(category) LIKE 'Utility'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        validateQuery().rowCount(5).hasColumns(carColumnNames("car")).validate(query, result);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithChildNodeJoinAndColumnsFromBothSidesOfJoin()
        throws RepositoryException {
        String sql = "SELECT car.*, category.[jcr:primaryType] from [car:Car] as car JOIN [nt:unstructured] as category ON ISCHILDNODE(car,category) WHERE NAME(category) LIKE 'Utility'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = allOf(carColumnNames("car"), new String[] {"category.jcr:primaryType"});
        validateQuery().rowCount(5).hasColumns(columnNames).validate(query, result);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithDescendantNodeJoinWithoutCriteria() throws RepositoryException {
        // The 'all' selector will find all nodes, including '/Car' and '/Car/Sports'. Thus,
        // '/Car/Sports/Infiniti G37' will be a descendant of both '/Car' and '/Car/Sports', and thus will appear
        // once joined with '/Car' and once joined with '/Car/Sports'. These two tuples will be similar, but they are
        // actually not repeats (since the columns from 'all' will be different).
        String sql = "SELECT * FROM [car:Car] as car JOIN [nt:unstructured] as all ON ISDESCENDANTNODE(car,all)";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = allOf(carColumnNames("car"), allColumnNames("all"));
        validateQuery().rowCount(26).hasColumns(columnNames).validate(query, result);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithDescendantNodeJoinWithDepthCriteria() throws RepositoryException {
        String sql = "SELECT * FROM [car:Car] as car JOIN [nt:unstructured] as category ON ISDESCENDANTNODE(car,category) WHERE DEPTH(category) = 2";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = allOf(carColumnNames("car"), allColumnNames("category"));
        validateQuery().rowCount(13).hasColumns(columnNames).validate(query, result);
    }

    @FixFor( "MODE-2151" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithChildCountCriteria() throws RepositoryException {
        String sql = "SELECT * FROM [car:Car] as car";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        String[] columnNames = carColumnNames("car");
        // No cars have children
        validateQuery().rowCount(13).hasColumns(columnNames).validate(query, query.execute());

        sql = "SELECT * FROM [car:Car] as car WHERE CHILDCOUNT(car) = 0";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(13).hasColumns(columnNames).validate(query, query.execute());

        sql = "SELECT [jcr:path], [mode:childCount] FROM [nt:unstructured] WHERE CHILDCOUNT() = 4";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(2).validate(query, query.execute());
    }

    @FixFor( "MODE-2151" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithChildNodeJoinWithChildCountCriteria() throws RepositoryException {
        String sql = "SELECT * FROM [car:Car] as car JOIN [nt:unstructured] as category ON ISCHILDNODE(car,category) WHERE CHILDCOUNT(category) > 4";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        String[] columnNames = allOf(carColumnNames("car"), allColumnNames("category"));
        // Just the "Utility" nodes ...
        validateQuery().rowCount(5).hasColumns(columnNames).validate(query, query.execute());

        sql = "SELECT * FROM [car:Car] as car JOIN [nt:unstructured] as category ON ISCHILDNODE(car,category) WHERE CHILDCOUNT(category) > 2";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        // Just the "Utility", "Hybrid", and "Luxury" nodes ...
        validateQuery().rowCount(11).hasColumns(columnNames).validate(query, query.execute());

        sql = "SELECT * FROM [car:Car] as car JOIN [nt:unstructured] as category ON ISCHILDNODE(car,category)";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        // All categories ...
        validateQuery().rowCount(13).hasColumns(columnNames).validate(query, query.execute());
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithDescendantNodeJoin() throws RepositoryException {
        String sql = "SELECT car.* from [car:Car] as car JOIN [nt:unstructured] as category ON ISDESCENDANTNODE(car,category) WHERE NAME(category) LIKE 'Utility'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(5).hasColumns(carColumnNames("car")).validate(query, result);
    }

    @FixFor( "MODE-1809" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2UnionOfQueriesWithJoins() throws RepositoryException {
        String sql1 = "SELECT car.* from [car:Car] as car JOIN [nt:unstructured] as category ON ISDESCENDANTNODE(car,category) WHERE NAME(category) LIKE 'Utility'";
        String sql2 = "SELECT car.* from [car:Car] as car JOIN [nt:unstructured] as category ON ISDESCENDANTNODE(car,category) WHERE NAME(category) LIKE 'Sports'";
        String sql = sql1 + " UNION " + sql2;

        String[] columnNames = carColumnNames("car");
        // print = true;
        Query query = session.getWorkspace().getQueryManager().createQuery(sql1, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(5).hasColumns(columnNames).validate(query, result);

        query = session.getWorkspace().getQueryManager().createQuery(sql2, Query.JCR_SQL2);
        result = query.execute();
        validateQuery().rowCount(2).hasColumns(columnNames).validate(query, result);

        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        result = query.execute();
        validateQuery().rowCount(7).hasColumns(columnNames).validate(query, result);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithDescendantNodeJoinAndColumnsFromBothSidesOfJoin()
        throws RepositoryException {
        String sql = "SELECT car.*, category.[jcr:primaryType] from [car:Car] as car JOIN [nt:unstructured] as category ON ISDESCENDANTNODE(car,category) WHERE NAME(category) LIKE 'Utility'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = allOf(carColumnNames("car"), new String[] {"category.jcr:primaryType"});
        validateQuery().rowCount(5).hasColumns(columnNames).validate(query, result);
    }

    @FixFor( "MODE-829" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithDescendantNodeJoinUsingNtBase() throws RepositoryException {
        String sql = "SELECT * FROM [nt:base] AS category JOIN [nt:base] AS cars ON ISDESCENDANTNODE(cars,category) WHERE ISCHILDNODE(category,'/Cars')";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = {"category.jcr:primaryType", "cars.jcr:primaryType", "cars.jcr:mixinTypes",
            "category.jcr:mixinTypes", "cars.jcr:name", "cars.jcr:path", "cars.jcr:score", "cars.mode:depth", "cars.mode:id",
            "cars.mode:localName", "category.jcr:name", "category.jcr:path", "category.jcr:score", "category.mode:depth",
            "category.mode:id", "category.mode:localName"};
        validateQuery().rowCount(13).hasColumns(columnNames).validate(query, result);
    }

    @FixFor( "MODE-829" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithDescendantNodeJoinUsingNtBaseAndNameConstraint()
        throws RepositoryException {
        String sql = "SELECT * FROM [nt:base] AS category JOIN [nt:base] AS cars ON ISDESCENDANTNODE(cars,category) WHERE ISCHILDNODE(category,'/Cars') AND NAME(cars) LIKE 'Toyota%'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = {"category.jcr:primaryType", "cars.jcr:primaryType", "cars.jcr:mixinTypes",
            "category.jcr:mixinTypes", "cars.jcr:name", "cars.jcr:path", "cars.jcr:score", "cars.mode:depth", "cars.mode:id",
            "cars.mode:localName", "category.jcr:name", "category.jcr:path", "category.jcr:score", "category.mode:depth",
            "category.mode:id", "category.mode:localName"};
        validateQuery().rowCount(3).hasColumns(columnNames).validate(query, result);
    }

    @FixFor( "MODE-829" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithDescendantNodeJoinUsingNonExistantNameColumnOnTypeWithResidualProperties()
        throws RepositoryException {
        String sql = "SELECT * FROM [nt:unstructured] AS category JOIN [nt:unstructured] AS cars ON ISDESCENDANTNODE(cars,category) WHERE ISCHILDNODE(category,'/Cars') AND cars.name = 'd2'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = {"category.jcr:primaryType", "cars.jcr:primaryType", "cars.jcr:mixinTypes",
            "category.jcr:mixinTypes", "cars.jcr:name", "cars.jcr:path", "cars.jcr:score", "cars.mode:depth", "cars.mode:id",
            "cars.mode:localName", "category.jcr:name", "category.jcr:path", "category.jcr:score", "category.mode:depth",
            "category.mode:id", "category.mode:localName"};
        // no nodes have a 'name' property (strictly speaking)
        validateQuery().rowCount(0).hasColumns(columnNames).validate(query, result);
    }

    @FixFor( "MODE-829" )
    @Test
    public void shouldReturnNoResultsForJcrSql2QueryWithDescendantNodeJoinUsingNonExistantNameColumnOnTypeWithNoResidualProperties()
        throws RepositoryException {
        String sql = "SELECT * FROM [nt:base] AS category JOIN [nt:base] AS cars ON ISDESCENDANTNODE(cars,category) WHERE ISCHILDNODE(category,'/Cars') AND cars.name = 'd2'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = {"category.jcr:primaryType", "cars.jcr:primaryType", "cars.jcr:mixinTypes",
            "category.jcr:mixinTypes", "cars.jcr:name", "cars.jcr:path", "cars.jcr:score", "cars.mode:depth", "cars.mode:id",
            "cars.mode:localName", "category.jcr:name", "category.jcr:path", "category.jcr:score", "category.mode:depth",
            "category.mode:id", "category.mode:localName"};
        // no results, because one side of the join has criteria on a non-existant column
        validateQuery().rowCount(0).hasColumns(columnNames).validate(query, result);
    }

    @FixFor( "MODE-869" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithSubqueryInCriteria() throws RepositoryException {
        String sql = "SELECT * FROM [car:Car] WHERE [car:maker] IN (SELECT [car:maker] FROM [car:Car] WHERE [car:year] >= 2008)";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        // there are 13 cars made by makers that made cars in or after 2008
        // The only car made before 2008 is Toyota Land Rover, but Toyota also makes the Highlander and Prius in 2008.
        validateQuery().rowCount(13).hasColumns(carColumnNames("car:Car")).validate(query, result);
    }

    @FixFor( "MODE-869" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithSubqueryInCriteria2() throws RepositoryException {
        String sql = "SELECT [car:maker] FROM [car:Car] WHERE PATH() LIKE '%/Hybrid/%'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(3).hasColumns("car:maker").validate(query, result);

        sql = "SELECT * FROM [car:Car] WHERE [car:maker] IN ('Toyota','Nissan')";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        result = query.execute();
        // the 4 kinds of cars made by makers that make hybrids
        validateQuery().rowCount(4).hasColumns(carColumnNames("car:Car")).validate(query, result);

        sql = "SELECT * FROM [car:Car] WHERE [car:maker] IN (SELECT [car:maker] FROM [car:Car] WHERE PATH() LIKE '%/Hybrid/%')";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        result = query.execute();
        // the 4 kinds of cars made by makers that make hybrids
        validateQuery().rowCount(4).hasColumns(carColumnNames("car:Car")).validate(query, result);
    }

    @FixFor( "MODE-2425" )
    @Test
    public void shouldBeAbleToUsePathOperandWithInQuery() throws RepositoryException {
        String sql = "SELECT [car:maker] FROM [car:Car] WHERE PATH()" +
                     " IN ('/Cars/Hybrid/Toyota Prius', '/Cars/Hybrid/Toyota Highlander', '/Cars/Hybrid/Nissan Altima')";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        final List<String> expectedMakers = new ArrayList<>(Arrays.asList("Toyota", "Toyota", "Nissan"));
        validateQuery().rowCount(3).onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber, Row row ) throws RepositoryException {
                String actualValue = row.getValue("car:maker").getString();
                expectedMakers.remove(actualValue);                
            }
        }).validate(query, result);
        assertTrue("Not all expected car makers found", expectedMakers.isEmpty());
    }

    @FixFor( "MODE-1873" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithSubqueryInCriteriaWhenSubquerySelectsPseudoColumn()
        throws RepositoryException {
        String sql = "SELECT [jcr:path] FROM [nt:unstructured] WHERE PATH() LIKE '/Other/%'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(4).hasColumns("jcr:path").validate(query, result);

        sql = "SELECT [jcr:path],[pathProperty] FROM [nt:unstructured] WHERE [pathProperty] = '/Other/NodeA'";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:path", "pathProperty").validate(query, result);

        sql = "SELECT [jcr:path],[pathProperty] FROM [nt:unstructured] WHERE [pathProperty] IN ('/Other/NodeA[2]', '/Other/NodeA', '/Other/NodeC', '/Other/NodeA[3]')";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:path", "pathProperty").validate(query, result);

        sql = "SELECT [jcr:path] FROM [nt:unstructured] WHERE [pathProperty] IN (SELECT [jcr:path] FROM [nt:unstructured] WHERE PATH() LIKE '/Other/%')";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:path").validate(query, result);
    }

    @FixFor( "MODE-909" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithOrderBy() throws RepositoryException {
        String sql = "SELECT [jcr:primaryType] from [nt:base] ORDER BY [jcr:primaryType]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(totalNodeCount).hasColumns("jcr:primaryType").onEachRow(new Predicate() {
            private String lastPrimaryType;

            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                String primaryType = row.getValue("jcr:primaryType").getString();
                if (lastPrimaryType != null) {
                    assertThat(primaryType.compareTo(lastPrimaryType) >= 0, is(true));
                }
                lastPrimaryType = primaryType;
            }
        }).validate(query, result);
    }

    protected Predicate pathOrder() {
        return new Predicate() {
            private Path lastPath;

            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                String pathStr = row.getValue("jcr:path").getString();
                Path path = path(pathStr);
                if (lastPath != null) {
                    assertThat(path.compareTo(lastPath) >= 0, is(true));
                }
                lastPath = path;
            }
        };
    }

    @FixFor( "MODE-2138" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithOrderByPathPseudoColumn() throws RepositoryException {
        String sql = "SELECT [jcr:path] from [nt:base] ORDER BY [jcr:path]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(totalNodeCount).hasColumns("jcr:path").onEachRow(pathOrder()).validate(query, result);
    }

    @FixFor( "MODE-2138" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithOrderByPath() throws RepositoryException {
        String sql = "SELECT [jcr:path] from [nt:base] ORDER BY [jcr:path]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(totalNodeCount).hasColumns("jcr:path").onEachRow(pathOrder()).validate(query, result);
    }

    @FixFor( {"MODE-1277", "MODE-1485"} )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithFullOuterJoin() throws RepositoryException {
        String sql = "SELECT car.[jcr:name], category.[jcr:primaryType], car.[jcr:path], category.[jcr:path] from [car:Car] as car FULL OUTER JOIN [nt:unstructured] as category ON ISCHILDNODE(car,category) WHERE NAME(car) LIKE 'Toyota*'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = {"car.jcr:name", "category.jcr:primaryType", "car.jcr:path", "category.jcr:path"};
        // Because every "Toyota" car has a parent, there will be no 'car' nodes that don't have a category. However,
        // we'll see every category node, and there are 25 of them. Therefore, we'll see 25 nodes.
        validateQuery().rowCount(25).hasColumns(columnNames).onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                // Every row should have a category ...
                assertNotNull(row.getNode("category"));
                // Every non-null car should be a Toyota ...
                Node car = row.getNode("car");
                if (car != null) assertTrue(car.getName().contains("Toyota"));
            }
        }).validate(query, result);
    }

    @FixFor( "MODE-1750" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithLeftOuterJoinOnNullCondition() throws RepositoryException {
        String sql = "SELECT car1.[jcr:name] from [car:Car] as car1 LEFT OUTER JOIN [car:Car] as car2 ON car1.[car:alternateModesl] = car2.[UUID]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(13).hasColumns("car1.jcr:name").validate(query, result);
    }

    @FixFor( "MODE-2187" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithLeftOuterJoinOnIsChildNode() throws RepositoryException {
        String sql = "SELECT left.[jcr:path] FROM [nt:unstructured] AS left LEFT OUTER JOIN [nt:unstructured] AS right ON ISCHILDNODE(left,right) WHERE ISDESCENDANTNODE(left,'/Cars')";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(17).hasColumns("left.jcr:path").validate(query, result);
    }

    @FixFor( "MODE-2494" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithTwoLeftOuterJoinsOnIsChildNodeWithSubsequentIsChildNode() throws RepositoryException {
        String sql = "SELECT parent.[jcr:path], child1.[jcr:name], desc.[jcr:name] FROM [nt:unstructured] AS parent " +
                "LEFT OUTER JOIN [nt:unstructured] AS child1 ON ISCHILDNODE(child1,parent) " +
                "INNER JOIN [nt:unstructured] AS desc on ISCHILDNODE(desc, child1) " +
                "LEFT OUTER JOIN [nt:unstructured] AS child2 ON ISCHILDNODE(child2,parent) " +
                "WHERE ISCHILDNODE(parent,'/') " +
                "AND NAME(child2) = 'Hybrid' " +
                "AND NAME(desc) LIKE 'Nissan%'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).validate(query, result);
    }

    @FixFor( "MODE-2494" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithTwoLeftOuterJoinsOnIsChildNodeWithSubsequentIsDescendantNode() throws RepositoryException {
        String sql = "SELECT parent.[jcr:path], child1.[jcr:name], desc.[jcr:name] FROM [nt:unstructured] AS parent " +
                "LEFT OUTER JOIN [nt:unstructured] AS child1 ON ISCHILDNODE(child1,parent) " +
                "INNER JOIN [nt:unstructured] AS desc on ISDESCENDANTNODE(desc, child1) " +
                "LEFT OUTER JOIN [nt:unstructured] AS child2 ON ISCHILDNODE(child2,parent) " +
                "WHERE ISCHILDNODE(parent,'/') " +
                "AND NAME(child2) = 'Hybrid' " +
                "AND NAME(desc) LIKE 'Nissan%'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).validate(query, result);
    }

    @FixFor( "MODE-2494" )
    @Test
    @Ignore("This is not fixed by the fix for MODE-2494, and points to a potentially deeper problem, " +
            "possibly in ReplaceViews." +
            "Note: this query has the same semantics as that in " +
            "'shouldBeAbleToCreateAndExecuteJcrSql2QueryWithTwoLeftOuterJoinsOnIsChildNodeWithSubsequentIsDescendantNode' " +
            "and should work exactly the same way.")
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithTwoLeftOuterJoinsOnIsChildNodeWithSubsequentIsDescendantNodeOutOfOrder() throws RepositoryException {
        String sql = "SELECT parent.[jcr:path], child1.[jcr:name], child2.[jcr:name], desc.[jcr:name] FROM [nt:unstructured] AS parent " +
                "LEFT OUTER JOIN [nt:unstructured] AS child1 ON ISCHILDNODE(child1,parent) " +
                "LEFT OUTER JOIN [nt:unstructured] AS child2 ON ISCHILDNODE(child2,parent) " +
                "INNER JOIN [nt:unstructured] AS desc on ISDESCENDANTNODE(desc, child1) " +
                "WHERE ISCHILDNODE(parent,'/') " +
                "AND NAME(child2) = 'Hybrid' " +
                "AND NAME(desc) LIKE 'Nissan%'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).validate(query, result);
    }

    @FixFor( "MODE-1750" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithRightOuterJoinOnNullCondition() throws RepositoryException {
        String sql = "SELECT car2.[jcr:name] from [car:Car] as car1 RIGHT OUTER JOIN [car:Car] as car2 ON car1.[car:alternateModesl] = car2.[UUID]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(13).hasColumns("car2.jcr:name").validate(query, result);
    }

    @FixFor( "MODE-2057" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithJoinAndNoCriteria() throws RepositoryException {
        String sql = "SELECT category.[jcr:path], cars.[jcr:path] FROM [nt:unstructured] AS category JOIN [car:Car] AS cars ON ISCHILDNODE(cars,category)";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(13).hasColumns("category.jcr:path", "cars.jcr:path").validate(query, result);
    }

    @FixFor( "MODE-2057" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithJoinAndDepthCriteria() throws RepositoryException {
        String sql = "SELECT category.[jcr:path], cars.[jcr:path] FROM [nt:unstructured] AS category JOIN [car:Car] AS cars ON ISCHILDNODE(cars,category) WHERE DEPTH(category) = 2";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(13).hasColumns("category.jcr:path", "cars.jcr:path").validate(query, result);
    }

    @FixFor( "MODE-2057" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithLeftOuterJoinAndDepthCriteria() throws RepositoryException {
        String sql = "SELECT category.[jcr:path], cars.[jcr:path] FROM [nt:unstructured] AS category LEFT OUTER JOIN [car:Car] AS cars ON ISCHILDNODE(cars,category) WHERE DEPTH(category) = 2";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(17).hasColumns("category.jcr:path", "cars.jcr:path").validate(query, result);
    }

    @FixFor( "MODE-2057" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithLeftOuterJoinWithFullTextSearch() throws RepositoryException {
        String sql = "SELECT category.[jcr:path], cars.[jcr:path] FROM [nt:unstructured] AS category LEFT OUTER JOIN [car:Car] AS cars ON ISCHILDNODE(cars,category) WHERE contains(category.*, 'Utility') AND contains(cars.*, 'Toyota') ";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("category.jcr:path", "cars.jcr:path").validate(query, result);
    }

    @FixFor( "MODE-2057" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithJoinWithFullTextSearch() throws RepositoryException {
        String sql = "SELECT category.[jcr:path], cars.[jcr:path] FROM [nt:unstructured] AS category JOIN [car:Car] AS cars ON ISCHILDNODE(cars,category) WHERE contains(category.*, 'Utility') AND contains(cars.*, 'Toyota') ";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("category.jcr:path", "cars.jcr:path").validate(query, result);
    }

    @FixFor( "MODE-2057" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithUnionAndFullTextSearch() throws RepositoryException {
        String sql = "SELECT category.[jcr:path] AS p FROM [nt:unstructured] AS category WHERE contains(category.*, 'Utility')"
                     + "UNION "
                     + "SELECT category.[jcr:path] AS p FROM [nt:unstructured] AS category JOIN [car:Car] AS cars ON ISCHILDNODE(cars,category) WHERE contains(cars.*, 'Toyota') ";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(2).hasColumns("p").validate(query, result);
    }

    @FixFor( "MODE-1679" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindReferenceableNodes() throws RepositoryException {
        String sql = "SELECT [jcr:uuid] FROM [mix:referenceable]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(4).hasColumns("jcr:uuid").validate(query, result);
    }

    @FixFor( "MODE-1679" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindJcrUuidOfNodeWithPathCriteria() throws RepositoryException {
        String sql = "SELECT [jcr:uuid] FROM [mix:referenceable] AS node WHERE PATH(node) = '/Other/NodeA[2]'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:uuid").validate(query, result);
    }

    @FixFor( "MODE-1679" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindNodesOfParticularPrimaryType() throws RepositoryException {
        String sql = "SELECT [notion:singleReference], [notion:multipleReferences] FROM [notion:typed]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("notion:singleReference", "notion:multipleReferences").onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                assertValueIsNonNullReference(row, "notion:singleReference");
                assertValueIsNonNullReference(row, "notion:multipleReferences");
            }
        }).validate(query, result);
    }

    @FixFor( "MODE-1679" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithSingleReferenceConstraintUsingSubquery() throws RepositoryException {
        String sql = "SELECT [notion:singleReference] FROM [notion:typed] WHERE [notion:singleReference] IN ( SELECT [jcr:uuid] FROM [mix:referenceable] AS node WHERE PATH(node) = '/Other/NodeA')";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("notion:singleReference").onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                assertValueIsNonNullReference(row, "notion:singleReference");
            }
        }).validate(query, result);
    }

    @FixFor( "MODE-1679" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithSingleReferenceConstraintUsingStringIdentifier()
        throws RepositoryException {
        String id = session.getNode("/Other/NodeA").getIdentifier();
        assertThat(id, is(notNullValue()));
        String sql = "SELECT [notion:singleReference] FROM [notion:typed] AS typed WHERE [notion:singleReference] = '" + id + "'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("notion:singleReference").onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                assertValueIsNonNullReference(row, "notion:singleReference");
            }
        }).validate(query, result);
    }

    @FixFor( "MODE-1679" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithSingleReferenceConstraintUsingJoin() throws RepositoryException {
        String sql = "SELECT typed.* FROM [notion:typed] AS typed JOIN [mix:referenceable] AS target ON typed.[notion:singleReference] = target.[jcr:uuid] WHERE PATH(target) = '/Other/NodeA'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns(typedColumnNames("typed")).onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                assertValueIsNonNullReference(row, "typed.notion:singleReference");
                assertValueIsNonNullReference(row, "typed.notion:multipleReferences");
            }
        }).validate(query, result);
    }

    @FixFor( "MODE-1679" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithMultipleReferenceConstraintUsingSubquery()
        throws RepositoryException {
        String sql = "SELECT [notion:multipleReferences] FROM [notion:typed] WHERE [notion:multipleReferences] IN ( SELECT [jcr:uuid] FROM [mix:referenceable] AS node WHERE PATH(node) = '/Other/NodeA[2]')";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("notion:multipleReferences").onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                assertValueIsNonNullReference(row, "notion:multipleReferences");
            }
        }).validate(query, result);
    }

    @FixFor( "MODE-1679" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithMultipleReferenceConstraintUsingStringIdentifier()
        throws RepositoryException {
        String id = session.getNode("/Other/NodeA[2]").getIdentifier();
        assertThat(id, is(notNullValue()));
        String sql = "SELECT [notion:multipleReferences] FROM [notion:typed] AS typed WHERE [notion:multipleReferences] = '" + id
                     + "'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("notion:multipleReferences").onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                assertValueIsNonNullReference(row, "notion:multipleReferences");
            }
        }).validate(query, result);
    }

    @FixFor( "MODE-1679" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithMultipleReferenceConstraintUsingJoin() throws RepositoryException {
        String sql = "SELECT typed.* FROM [notion:typed] AS typed JOIN [mix:referenceable] AS target ON typed.[notion:multipleReferences] = target.[jcr:uuid] WHERE PATH(target) = '/Other/NodeA[2]'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns(typedColumnNames("typed")).onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                assertValueIsNonNullReference(row, "typed.notion:singleReference");
                assertValueIsNonNullReference(row, "typed.notion:multipleReferences");
            }
        }).validate(query, result);
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedPathInSelectOfJcrSql2Query() throws RepositoryException {
        String sql = "select [jcr:primaryType], [jcr:path] FROM [nt:base]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(totalNodeCount).hasColumns("jcr:primaryType", "jcr:path").onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                assertNotNull(row.getValue("jcr:primaryType"));
            }
        }).validate(query, result);
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedPathInSelectAndCriteriaOfJcrSql2Query() throws RepositoryException {
        String sql = "select [jcr:primaryType], [jcr:path] FROM [nt:base] WHERE [jcr:path] LIKE '/Cars/%'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(17).hasColumns("jcr:primaryType", "jcr:path").onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                assertNotNull(row.getValue("jcr:primaryType"));
            }
        }).validate(query, result);
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedPathInSelectAndUnqualifiedNameInCriteriaOfJcrSql2Query()
        throws RepositoryException {
        String sql = "select [jcr:primaryType], [jcr:path] FROM [nt:base] WHERE [jcr:name] LIKE '%3%'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(4).hasColumns("jcr:primaryType", "jcr:path").onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                assertNotNull(row.getValue("jcr:primaryType"));
            }
        }).validate(query, result);
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedPathInSelectAndUnqualifiedLocalNameInCriteriaOfJcrSql2Query()
        throws RepositoryException {
        String sql = "select [jcr:primaryType], [jcr:path] FROM [nt:base] WHERE [mode:localName] LIKE '%3%'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(4).hasColumns("jcr:primaryType", "jcr:path").onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                assertNotNull(row.getValue("jcr:primaryType"));
            }
        }).validate(query, result);
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithJcrPathInJoinCriteriaOfJcrSql2Query() throws RepositoryException {
        String sql = "select base.[jcr:primaryType], base.[jcr:path], car.[car:year] FROM [nt:base] AS base JOIN [car:Car] AS car ON car.[jcr:path] = base.[jcr:path]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(13).hasColumns("base.jcr:primaryType", "base.jcr:path", "car.car:year").validate(query, result);
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldNotIncludePseudoColumnsInSelectStarOfJcrSql2Query() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("select * FROM [nt:base]", Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        validateQuery().rowCount(totalNodeCount).hasColumns(allColumnNames("nt:base")).validate(query, result);
    }

    @FixFor( "MODE-1738" )
    @Test
    public void shouldSupportJoinWithOrderByOnPseudoColumn() throws RepositoryException {
        String sql = "SELECT category.[jcr:path], cars.[car:maker], cars.[car:lengthInInches] FROM [nt:unstructured] AS category JOIN [car:Car] AS cars ON ISDESCENDANTNODE(cars,category) WHERE ISCHILDNODE(category,'/Cars') ORDER BY cars.[mode:localName]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] expectedColumns = {"category.jcr:path", "cars.car:maker", "cars.mode:localName", "cars.car:lengthInInches"};
        validateQuery().rowCount(13).hasColumns(expectedColumns).validate(query, result);
    }

    @FixFor( "MODE-1738" )
    @Test
    public void shouldSupportJoinWithOrderByOnActualColumn() throws RepositoryException {
        String sql = "SELECT category.[jcr:path], cars.[car:maker], cars.[car:lengthInInches] FROM [nt:unstructured] AS category JOIN [car:Car] AS cars ON ISDESCENDANTNODE(cars,category) WHERE ISCHILDNODE(category,'/Cars') ORDER BY cars.[car:maker]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] expectedColumns = {"category.jcr:path", "cars.car:maker", "cars.car:lengthInInches"};
        validateQuery().rowCount(13).hasColumns(expectedColumns).validate(query, result);
    }

    @FixFor( "MODE-1737" )
    @Test
    public void shouldSupportSelectDistinct() throws RepositoryException {
        String sql = "SELECT DISTINCT cars.[car:maker], cars.[car:lengthInInches] FROM [car:Car] AS cars";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] expectedColumns = {"car:maker", "car:lengthInInches"};
        validateQuery().rowCount(13).hasColumns(expectedColumns).validate(query, result);
    }

    @FixFor( "MODE-1737" )
    @Test
    public void shouldSupportJoinWithSelectDistinct() throws RepositoryException {
        String sql = "SELECT DISTINCT category.[jcr:path], cars.[car:maker], cars.[car:lengthInInches] FROM [nt:unstructured] AS category JOIN [car:Car] AS cars ON ISDESCENDANTNODE(cars,category) WHERE ISCHILDNODE(category,'/Cars')";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] expectedColumns = {"category.jcr:path", "cars.car:maker", "cars.car:lengthInInches"};
        validateQuery().rowCount(13).hasColumns(expectedColumns).validate(query, result);
    }

    @FixFor( "MODE-1020" )
    @Test
    public void shouldFindAllPublishAreas() throws Exception {
        String sql = "SELECT [jcr:path], [jcr:title], [jcr:description] FROM [mode:publishArea]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(0).hasColumns("jcr:path", "jcr:title", "jcr:description").validate(query, result);
    }

    @FixFor( "MODE-1052" )
    @Test
    public void shouldProperlyUseNotWithPathConstraints() throws Exception {
        // Find all nodes that are children of '/Cars' ... there should be 4 ...
        String sql = "SELECT [jcr:path] FROM [nt:base] WHERE ISCHILDNODE([nt:base],'/Cars') ORDER BY [jcr:path]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] paths = {"/Cars/Hybrid", "/Cars/Luxury", "/Cars/Sports", "/Cars/Utility"};
        validateQuery().rowCount(4).hasColumns("jcr:path").hasNodesAtPaths(paths).validate(query, result);

        // Find all nodes ... there should be 24 ...
        sql = "SELECT [jcr:path] FROM [nt:base] ORDER BY [jcr:path]";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        result = query.execute();
        validateQuery().rowCount(totalNodeCount).hasColumns("jcr:path").validate(query, result);

        // Find all nodes that are NOT children of '/Cars' (and not under '/jcr:system') ...
        sql = "SELECT [jcr:path] FROM [nt:base] WHERE NOT(ISCHILDNODE([nt:base],'/Cars')) AND NOT(ISDESCENDANTNODE([nt:base],'/jcr:system')) ORDER BY [jcr:path]";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        result = query.execute();
        paths = new String[] {"/", "/Cars", "/Cars/Hybrid/Nissan Altima", "/Cars/Hybrid/Toyota Highlander",
            "/Cars/Hybrid/Toyota Prius", "/Cars/Luxury/Bentley Continental", "/Cars/Luxury/Cadillac DTS",
            "/Cars/Luxury/Lexus IS350", "/Cars/Sports/Aston Martin DB9", "/Cars/Sports/Infiniti G37", "/Cars/Utility/Ford F-150",
            "/Cars/Utility/Hummer H3", "/Cars/Utility/Land Rover LR2", "/Cars/Utility/Land Rover LR3",
            "/Cars/Utility/Toyota Land Cruiser", "/NodeB", "/Other", "/Other/NodeA", "/Other/NodeA[2]", "/Other/NodeA[3]",
            "/Other/NodeC", "/jcr:system"};
        validateQuery().rowCount(22).hasColumns("jcr:path").validate(query, result);
    }

    @FixFor( "MODE-1110" )
    @Test
    public void shouldExecuteQueryWithThreeInnerJoinsAndCriteriaOnDifferentSelectors() throws Exception {
        String sql = "SELECT * from [nt:base] as car INNER JOIN [nt:base] as categories ON ISDESCENDANTNODE(car, categories) "
                     + " INNER JOIN [nt:base] as carsNode ON ISDESCENDANTNODE (categories, carsNode) "
                     + " WHERE PATH(carsNode) = '/Cars' AND ISDESCENDANTNODE( categories, '/Cars') OR car.[jcr:primaryType] IS NOT NULL";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        String[] columnNames = allOf(allColumnNames("car"), allColumnNames("categories"), allColumnNames("carsNode"));
        validateQuery().rowCount(13).hasColumns(columnNames).validate(query, result);
    }

    @FixFor( "MODE-2054" )
    @Test
    public void shouldExecuteJoinWithOrConstraintsOnEachSide() throws Exception {
        // Find all of the cars under 'Utility' and 'Luxury' categories ...
        String sql = "SELECT car.[jcr:path], category.[jcr:path] from [car:Car] as car INNER JOIN [nt:unstructured] as category ON ISCHILDNODE(car, category) "
                     + " WHERE NAME(category) LIKE '%y'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        // print = true;
        validateQuery().rowCount(8).validate(query, query.execute());

        sql = "SELECT car.[jcr:path], category.[jcr:path] from [car:Car] as car INNER JOIN [nt:unstructured] as category ON ISCHILDNODE(car, category) ";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(13).validate(query, query.execute());

        sql = "SELECT car.[jcr:path], category.[jcr:path] from [car:Car] as car INNER JOIN [nt:unstructured] as category ON ISCHILDNODE(car, category) "
              + " WHERE NAME(category) LIKE '%y' OR NAME(car) LIKE 'Toyota %'";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(10).validate(query, query.execute());
    }

    @FixFor( "MODE-1418" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithFullTextSearchWithSelectorAndOneProperty()
        throws RepositoryException {
        String sql = "select [jcr:path] from [nt:unstructured] as n where contains(n.something, 'cat wearing')";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:path").validate(query, result);
    }

    @FixFor( "MODE-1418" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithFullTextSearchWithSelectorAndAllProperties()
        throws RepositoryException {
        String sql = "select [jcr:path] from [nt:unstructured] as n where contains(n.*, 'cat wearing')";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:path").validate(query, result);
    }

    @FixFor( "MODE-1418" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithFullTextSearchWithNoSelectorAndOneProperty()
        throws RepositoryException {
        String sql = "select [jcr:path] from [nt:unstructured] as n where contains(something,'cat wearing')";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:path").hasNodesAtPaths("/Other/NodeA[2]").validate(query, result);
    }

    @FixFor( "MODE-1829" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithFullTextSearchUsingLeadingWildcard() throws RepositoryException {
        String sql = "select [jcr:path] from [nt:unstructured] as n where contains(n.something, '*earing')";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:path").validate(query, result);

        sql = "select [jcr:path] from [nt:unstructured] as n where contains(n.something, '*earing*')";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:path").validate(query, result);
    }

    @FixFor( "MODE-1829" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithFullTextSearchUsingTrailingWildcard() throws RepositoryException {
        String sql = "select [jcr:path] from [nt:unstructured] as n where contains(n.something, 'wea*')";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:path").validate(query, result);
    }

    @Test
    @FixFor( "MODE-1547" )
    public void shouldBeAbleToExecuteFullTextSearchQueriesOnPropertiesWhichIncludeStopWords() throws Exception {
        String propertyText = "the quick Brown fox jumps over to the dog in at the gate";
        Node ftsNode = session.getRootNode().addNode("FTSNode").setProperty("FTSProp", propertyText).getParent();
        ftsNode.addMixin("mix:title");
        try {
            session.save();

            executeQueryWithSingleResult("select [jcr:path] from [mix:title] as n where contains([mix:title].*,'"
                                         + propertyText + "')");

            executeQueryWithSingleResult("select [jcr:path] from [mix:title] as n where contains(FTSProp,'" + propertyText
                                         + "')");
            executeQueryWithSingleResult("select [jcr:path] from [mix:title] as n where contains(n.*,'" + propertyText
                                         + "')");

            executeQueryWithSingleResult("select [jcr:path] from [mix:title] as n where contains(FTSProp,'"
                                         + propertyText.toUpperCase() + "')");
            executeQueryWithSingleResult("select [jcr:path] from [mix:title] as n where contains(n.*,'"
                                         + propertyText.toUpperCase() + "')");

            executeQueryWithSingleResult("select [jcr:path] from [mix:title] as n where contains(FTSProp,'the quick Dog')");
            executeQueryWithSingleResult("select [jcr:path] from [mix:title] as n where contains(n.*,'the quick Dog')");

            executeQueryWithSingleResult("select [jcr:path] from [mix:title] as n where contains(FTSProp,'the quick jumps over gate')");
            executeQueryWithSingleResult("select [jcr:path] from [mix:title] as n where contains(n.*,'the quick jumps over gate')");

            executeQueryWithSingleResult("select [jcr:path] from [mix:title] as n where contains(FTSProp,'the gate')");
            executeQueryWithSingleResult("select [jcr:path] from [mix:title] as n where contains(n.*,'the gate')");
        } finally {
            // Try to remove the node (which messes up the expected results from subsequent tests) ...
            ftsNode.remove();
            session.save();
        }
    }

    @Test
    @FixFor( "MODE-2448" )
    public void shouldBeAbleToExecuteFullTextSearchQueriesOnPropertiesWhichIncludeUmlauts() throws Exception {
        String propertyText = "Änderung: der schnelle braune Fuchs springt über den Hund";
        Node ftsNode = session.getRootNode().addNode("FTSNode").setProperty("FTSProp", propertyText).getParent();
        ftsNode.addMixin("mix:title");
        try {
            session.save();

            executeQueryWithSingleResult("select [jcr:path] from [mix:title] as n where contains([mix:title].*,'"
                                         + propertyText + "')");

            executeQueryWithSingleResult("select [jcr:path] from [mix:title] as n where contains(FTSProp,'" + propertyText
                                         + "')");
            executeQueryWithSingleResult("select [jcr:path] from [mix:title] as n where contains(n.*,'" + propertyText
                                         + "')");

            executeQueryWithSingleResult("select [jcr:path] from [mix:title] as n where contains(FTSProp,'"
                                         + propertyText.toUpperCase() + "')");
            executeQueryWithSingleResult("select [jcr:path] from [mix:title] as n where contains(n.*,'"
                                         + propertyText.toUpperCase() + "')");

            executeQueryWithSingleResult("select [jcr:path] from [mix:title] as n where contains(FTSProp,'Änderung')");
            executeQueryWithSingleResult("select [jcr:path] from [mix:title] as n where contains(FTSProp,'änderung')");
            executeQueryWithSingleResult("select [jcr:path] from [mix:title] as n where contains(FTSProp,'ÄNDERUNG')");
            executeQueryWithSingleResult("select [jcr:path] from [mix:title] as n where contains(FTSProp,'über den Hund')");
            executeQueryWithSingleResult("select [jcr:path] from [mix:title] as n where contains(FTSProp,'Über den Hund')");
            executeQueryWithSingleResult("select [jcr:path] from [mix:title] as n where contains(FTSProp,'ÜbEr dEn Hund')");
        } finally {
            // Try to remove the node (which messes up the expected results from subsequent tests) ...
            ftsNode.remove();
            session.save();
        }
    }

    private void executeQueryWithSingleResult( String sql ) throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, JcrRepository.QueryLanguage.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:path").validate(query, result);
    }

    @FixFor( "MODE-1840" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithBindVariableInsideContains() throws RepositoryException {
        String sql = "select [jcr:path] from [nt:unstructured] as n where contains(n.something, $expression)";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        query.bindValue("expression", session.getValueFactory().createValue("cat wearing"));
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:path").hasNodesAtPaths("/Other/NodeA[2]").validate(query, result);
    }

    @FixFor( "MODE-1840" )
    @Test( expected = InvalidQueryException.class )
    public void shouldNotBeAbleToCreateAndExecuteJcrSql2QueryWithBindVariableInsideContainsIfVariableIsNotBound()
        throws RepositoryException {
        String sql = "select [jcr:path] from [nt:unstructured] as n where contains(n.something, $expression)";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // do not bind a value
        // query.bindValue("expression", session.getValueFactory().createValue("cat wearing"));
        query.execute();
    }

    @FixFor( "MODE-1145" )
    @Test
    public void shouldParseFincayraQuery() throws Exception {
        String sql = "SELECT * FROM [fincayra.Post] AS post";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(0).validate(query, result);

        sql = "SELECT * FROM [fincayra.User] AS u WHERE u.email='test1@innobuilt.com'";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        result = query.execute();
        validateQuery().rowCount(0).validate(query, result);

        sql = "SELECT post.\"jcr:uuid\", post.\"text\", post.\"user\" FROM [fincayra.Post] AS post JOIN [fincayra.User] AS u ON post.\"user\"=u.\"jcr:uuid\" WHERE u.email='test1@innobuilt.com'";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        result = query.execute();
        validateQuery().rowCount(0).hasColumns("post.jcr:uuid", "post.text", "post.user").validate(query, result);
    }

    @FixFor( "MODE-1145" )
    @Test
    public void shouldParseFincayraQuery2() throws Exception {
        String sql = "SELECT post.\"jcr:uuid\", post.\"text\", post.\"user\" FROM [fincayra.UnstrPost] AS post JOIN [fincayra.UnstrUser] AS u ON post.\"user\"=u.\"jcr:uuid\" WHERE u.email='test1@innobuilt.com'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(0).hasColumns("post.jcr:uuid", "post.text", "post.user").validate(query, result);
    }

    @FixFor( "MODE-1145" )
    @Test
    public void shouldParseQueryWithResidualPropertyInSelectAndCriteria() throws Exception {
        String sql = "SELECT [jcr:path], something FROM [nt:unstructured] AS u WHERE something LIKE 'value%'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        validateQuery().rowCount(2).hasColumns("jcr:path", "something").onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                assertThat(row.getNode().hasProperty("something"), is(true));
            }
        }).validate(query, result);
    }

    @Test
    public void shouldFindSystemNodesUsingPathCriteria() throws Exception {
        String queryString = "select [jcr:path] from [nt:base] where [jcr:path] like '/jcr:system/%' and [jcr:path] not like '/jcr:system/%/%'";
        assertNodesAreFound(queryString, Query.JCR_SQL2, INDEXED_SYSTEM_NODES_PATHS);
    }

    @FixFor( "MODE-2286" )
    @Test
    public void shouldFindSystemNodesUsingPathLikeCriteriaWithNoSnsIndexSpecified() throws Exception {
        String sql = "select [jcr:path] from [nt:base] where [jcr:path] like '/Other/NodeA'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(1).validate(query, query.execute());
    }

    @FixFor( "MODE-2286" )
    @Test
    public void shouldFindSystemNodesUsingPathLikeCriteria() throws Exception {
        String sql = "select [jcr:path] from [nt:base] where [jcr:path] like '/Other/NodeA[%]'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(3).validate(query, query.execute());
    }

    @FixFor( "MODE-2286" )
    @Test
    public void shouldFindSystemNodesUsingPathLikeCriteriaWithAllSnsIndexSpecified() throws Exception {
        String sql = "select [jcr:path] from [nt:base] where [jcr:path] like '/Other[1]/NodeA[%]'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(3).validate(query, query.execute());
    }

    @Test
    public void shouldFindSystemNodesUsingIsChildNodeCriteria() throws Exception {
        String queryString = "select [jcr:path] from [nt:base] where ischildnode('/jcr:system')";
        assertNodesAreFound(queryString, Query.JCR_SQL2, INDEXED_SYSTEM_NODES_PATHS);
    }

    @Test
    public void shouldFindBuiltInNodeTypes() throws Exception {
        String queryString = "select [jcr:path] from [nt:base] where ischildnode('/jcr:system/jcr:nodeTypes')";
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);
        QueryResult result = query.execute();
        long numNodetypes = session.getWorkspace().getNodeTypeManager().getAllNodeTypes().getSize();
        validateQuery().rowCount(numNodetypes).validate(query, result);
    }

    @Test
    @FixFor( "MODE-1550" )
    public void shouldFindChildrenOfRootUsingIsChildNodeCriteria() throws Exception {
        Node node1 = session.getRootNode().addNode("node1");
        Node node2 = session.getRootNode().addNode("node2");

        try {
            // We didn't save our changes, so we shouldn't find the newly-added nodes
            String queryString = "select [jcr:path] from [nt:base] where ischildnode('/')";
            assertNodesAreFound(queryString, Query.JCR_SQL2, "/jcr:system", "/Cars", "/Other", "/NodeB");

            // Now save the session, and re-query to find the newly-added nodes ...
            session.save();

            // We should now find the newly-added nodes ...
            assertNodesAreFound(queryString, Query.JCR_SQL2, "/jcr:system", "/Cars", "/Other", "/NodeB", "/node1", "/node2");
        } finally {
            node1.remove();
            node2.remove();
            session.save();
        }
    }

    @SuppressWarnings( "deprecation" )
    @FixFor( "MODE-1680" )
    @Test
    public void testOrderByWithAliases() throws Exception {
        // fill the repository with test data
        Node src = session.getRootNode().addNode("src", "nt:folder");

        // add node f1 with child jcr:content
        try {
            Node f1 = src.addNode("f1", "nt:file");
            f1.addMixin("mix:simpleVersionable");
            Node content1 = f1.addNode("jcr:content", "nt:resource");
            content1.setProperty("jcr:data", session.getValueFactory().createBinary("Node f1".getBytes()));

            // save and slip a bit to have difference in time of node creation.
            session.save();
            Thread.sleep(100);

            // add node f2 with child jcr:content
            Node f2 = src.addNode("f2", "nt:file");
            f2.addMixin("mix:simpleVersionable");
            Node content2 = f2.addNode("jcr:content", "nt:resource");
            content2.setProperty("jcr:data", session.getValueFactory().createBinary("Node f2".getBytes()));

            session.save();

            // print = true;
            printMessage("-------------------- MyQueryTest---------------------");

            String descOrder = "SELECT [nt:file].[jcr:created] FROM [nt:file] INNER JOIN [nt:base] AS content ON ISCHILDNODE(content,[nt:file]) WHERE ([nt:file].[jcr:mixinTypes] = 'mix:simpleVersionable' AND NAME([nt:file]) LIKE 'f%') ORDER BY content.[jcr:lastModified] DESC";
            String ascOrder = "SELECT [nt:file].[jcr:created] FROM [nt:file] INNER JOIN [nt:base] AS content ON ISCHILDNODE(content,[nt:file]) WHERE ([nt:file].[jcr:mixinTypes] = 'mix:simpleVersionable' AND NAME([nt:file]) LIKE 'f%') ORDER BY content.[jcr:lastModified] ASC";

            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(descOrder, Query.JCR_SQL2);
            QueryResult result = query.execute();

            // checking first query
            RowIterator it = result.getRows();
            assertEquals(2, it.getSize());

            Node n1 = it.nextRow().getNode();
            Node n2 = it.nextRow().getNode();

            assertEquals("f2", n1.getName());
            assertEquals("f1", n2.getName());

            // the same request with other order
            query = queryManager.createQuery(ascOrder, Query.JCR_SQL2);
            result = query.execute();

            // checking second query
            it = result.getRows();
            assertEquals(2, it.getSize());

            n1 = it.nextRow().getNode();
            n2 = it.nextRow().getNode();

            assertEquals("f1", n1.getName());
            assertEquals("f2", n2.getName());

            // Try the XPath query ...
            String descOrderX = "/jcr:root//element(*,nt:file)[(@jcr:mixinTypes = 'mix:simpleVersionable')] order by jcr:content/@jcr:lastModified descending";
            String ascOrderX = "/jcr:root//element(*,nt:file)[(@jcr:mixinTypes = 'mix:simpleVersionable')] order by jcr:content/@jcr:lastModified ascending";
            query = queryManager.createQuery(descOrderX, Query.XPATH);
            result = query.execute();
            // checking first query
            it = result.getRows();
            assertEquals(2, it.getSize());

            n1 = it.nextRow().getNode();
            n2 = it.nextRow().getNode();

            assertEquals("f2", n1.getName());
            assertEquals("f1", n2.getName());

            // the same request with other order
            query = queryManager.createQuery(ascOrderX, Query.XPATH);
            result = query.execute();

            // checking second query
            it = result.getRows();
            assertEquals(2, it.getSize());

            n1 = it.nextRow().getNode();
            n2 = it.nextRow().getNode();

            assertEquals("f1", n1.getName());
            assertEquals("f2", n2.getName());
        } finally {
            src.remove();
            session.save();
        }
    }

    @Test
    @FixFor( "MODE-1900" )
    public void shouldSelectDistinctNodesWhenJoiningMultiValueReferenceProperties() throws Exception {
        Node nodeA = session.getRootNode().addNode("A", "test:node");
        nodeA.setProperty("test:name", "A");

        Node nodeB = session.getRootNode().addNode("B", "test:node");
        nodeB.setProperty("test:name", "B");
        JcrValue nodeBRef = session.getValueFactory().createValue(nodeB);

        Node nodeC = session.getRootNode().addNode("C", "test:node");
        nodeC.setProperty("test:name", "C");
        JcrValue nodeCRef = session.getValueFactory().createValue(nodeC);

        try {
            Node relationship = nodeA.addNode("relationship", "test:relationship");
            relationship.setProperty("test:target", new JcrValue[] {nodeBRef, nodeCRef});

            session.save();
            QueryManager queryManager = session.getWorkspace().getQueryManager();

            // Find the reference in the node ...
            String queryString = "SELECT relationship.[test:target] FROM [test:relationship] AS relationship";
            Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);
            validateQuery().rowCount(1).validate(query, query.execute());

            // Find the UUIDs of the 'test:node' (which are all 'mix:referenceable') ...
            queryString = "SELECT [jcr:uuid], [jcr:path] FROM [test:node]";
            query = queryManager.createQuery(queryString, Query.JCR_SQL2);
            validateQuery().rowCount(3).onEachRow(new Predicate() {
                @Override
                public void validate( int rowNumber,
                                      Row row ) throws RepositoryException {
                    assertNotNull(row.getValue("jcr:uuid"));
                }
            }).validate(query, query.execute());

            queryString = "SELECT relationship.[test:target], target.[jcr:path] "
                          + "   FROM [test:relationship] AS relationship "
                          + "   JOIN [test:node] AS target ON relationship.[test:target] = target.[jcr:uuid] ";
            query = queryManager.createQuery(queryString, Query.JCR_SQL2);
            validateQuery().rowCount(2).validate(query, query.execute());

            queryString = "SELECT node.[jcr:path], relationship.[test:target], target.[jcr:path] FROM [test:node] AS node "
                          + "   JOIN [test:relationship] AS relationship ON ISCHILDNODE(relationship, node) "
                          + "   JOIN [test:node] AS target ON relationship.[test:target] = target.[jcr:uuid] "
                          + "   WHERE node.[test:name] = 'A'";
            query = queryManager.createQuery(queryString, Query.JCR_SQL2);
            validateQuery().rowCount(2).validate(query, query.execute());

            queryString = "SELECT DISTINCT target.* FROM [test:node] AS node "
                          + "   JOIN [test:relationship] AS relationship ON ISCHILDNODE(relationship, node) "
                          + "   JOIN [test:node] AS target ON relationship.[test:target] = target.[jcr:uuid] "
                          + "   WHERE node.[test:name] = 'A'";
            query = queryManager.createQuery(queryString, Query.JCR_SQL2);
            validateQuery().rowCount(2).validate(query, query.execute());
        } finally {
            nodeA.remove();
            nodeB.remove();
            nodeC.remove();
            session.save();
        }
    }

    @Test
    @FixFor( "MODE-1969" )
    public void shouldRetrieveStrongReferrers() throws Exception {
        Node nodeA = session.getRootNode().addNode("A");
        nodeA.addMixin("mix:referenceable");
        Node nodeB = session.getRootNode().addNode("B");
        nodeB.addMixin("mix:referenceable");

        Node referrerA = session.getRootNode().addNode("referrerA");
        referrerA.setProperty("nodeARef", nodeA);
        Node referrerB = session.getRootNode().addNode("referrerB");
        referrerB.setProperty("nodeBRef", nodeB);
        List<String> referrerIds = Arrays.asList(referrerA.getIdentifier(), referrerB.getIdentifier());
        Collections.sort(referrerIds);

        session.save();

        try {
            String queryString = "SELECT * from [nt:unstructured] where REFERENCE() IN " + idList(nodeA, nodeB);
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);
            QueryResult result = query.execute();
            final List<String> resultIds = new ArrayList<String>();
            validateQuery().rowCount(2).onEachRow(new Predicate() {
                @Override
                public void validate( int rowNumber,
                                      Row row ) throws RepositoryException {
                    resultIds.add(row.getNode().getIdentifier());
                }
            }).validate(query, result);

            Collections.sort(resultIds);
            assertEquals(referrerIds, resultIds);
        } finally {
            nodeA.remove();
            nodeB.remove();
            referrerA.remove();
            referrerB.remove();
            session.save();
        }
    }

    @Test
    @FixFor( "MODE-1969" )
    public void shouldRetrieveWeakReferrers() throws Exception {
        Node nodeA = session.getRootNode().addNode("A");
        nodeA.addMixin("mix:referenceable");
        Node nodeB = session.getRootNode().addNode("B");
        nodeB.addMixin("mix:referenceable");

        Node referrerA = session.getRootNode().addNode("referrerA");
        referrerA.setProperty("nodeAWRef", session.getValueFactory().createValue(nodeA, true));
        Node referrerB = session.getRootNode().addNode("referrerB");
        referrerB.setProperty("nodeBWRef", session.getValueFactory().createValue(nodeB, true));
        List<String> referrerIds = Arrays.asList(referrerA.getIdentifier(), referrerB.getIdentifier());
        Collections.sort(referrerIds);

        session.save();

        try {
            String queryString = "SELECT * from [nt:unstructured] where REFERENCE() IN " + idList(nodeA, nodeB);
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);
            QueryResult result = query.execute();
            final List<String> resultIds = new ArrayList<String>();
            validateQuery().rowCount(2).onEachRow(new Predicate() {
                @Override
                public void validate( int rowNumber,
                                      Row row ) throws RepositoryException {
                    resultIds.add(row.getNode().getIdentifier());
                }
            }).validate(query, result);

            Collections.sort(resultIds);
            assertEquals(referrerIds, resultIds);
        } finally {
            nodeA.remove();
            nodeB.remove();
            referrerA.remove();
            referrerB.remove();

            session.save();
        }
    }

    @Test
    @FixFor( "MODE-1969" )
    public void shouldRetrieveSimpleReferrers() throws Exception {
        Node nodeA = session.getRootNode().addNode("A");
        nodeA.addMixin("mix:referenceable");
        Node nodeB = session.getRootNode().addNode("B");
        nodeB.addMixin("mix:referenceable");

        Node referrerA = session.getRootNode().addNode("referrerA");
        referrerA.setProperty("nodeASRef", session.getValueFactory().createSimpleReference(nodeA));
        Node referrerB = session.getRootNode().addNode("referrerB");
        referrerB.setProperty("nodeBSRef", session.getValueFactory().createSimpleReference(nodeB));
        List<String> referrerIds = Arrays.asList(referrerA.getIdentifier(), referrerB.getIdentifier());
        Collections.sort(referrerIds);

        session.save();
        try {
            String queryString = "SELECT * from [nt:unstructured] where REFERENCE() IN " + idList(nodeA, nodeB);
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);
            QueryResult result = query.execute();

            final List<String> resultIds = new ArrayList<String>();
            validateQuery().rowCount(2).onEachRow(new Predicate() {
                @Override
                public void validate( int rowNumber,
                                      Row row ) throws RepositoryException {
                    resultIds.add(row.getNode().getIdentifier());
                }
            }).validate(query, result);

            Collections.sort(resultIds);
            assertEquals(referrerIds, resultIds);

        } finally {
            nodeA.remove();
            nodeB.remove();
            referrerA.remove();
            referrerB.remove();
            session.save();
        }
    }

    @Test
    @FixFor( "MODE-1969" )
    public void shouldRetrieveStrongWeakSimpleReferrers() throws Exception {
        Node nodeA = session.getRootNode().addNode("A");
        nodeA.addMixin("mix:referenceable");
        Node nodeB = session.getRootNode().addNode("B");
        nodeB.addMixin("mix:referenceable");

        Node referrerA = session.getRootNode().addNode("referrerA");
        referrerA.setProperty("nodeARef", nodeA);
        Node referrerB = session.getRootNode().addNode("referrerB");
        referrerB.setProperty("nodeBWRef", session.getValueFactory().createValue(nodeB, true));
        Node referrerC = session.getRootNode().addNode("referrerC");
        referrerC.setProperty("nodeCSRef", session.getValueFactory().createSimpleReference(nodeB));
        List<String> referrerIds = Arrays.asList(referrerA.getIdentifier(), referrerB.getIdentifier(), referrerC.getIdentifier());
        Collections.sort(referrerIds);

        session.save();

        try {
            String queryString = "SELECT * from [nt:unstructured] where REFERENCE() IN " + idList(nodeA, nodeB);
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);
            QueryResult result = query.execute();

            final List<String> resultIds = new ArrayList<String>();
            validateQuery().rowCount(3).onEachRow(new Predicate() {
                @Override
                public void validate( int rowNumber,
                                      Row row ) throws RepositoryException {
                    resultIds.add(row.getNode().getIdentifier());
                }
            }).validate(query, result);

            Collections.sort(resultIds);
            assertEquals(referrerIds, resultIds);
        } finally {
            nodeA.remove();
            nodeB.remove();
            referrerA.remove();
            referrerB.remove();
            referrerC.remove();
            session.save();
        }
    }

    @Test
    @FixFor( "MODE-2053" )
    public void shouldRunLeftOuterJoin() throws Exception {
        Node parent1 = session.getRootNode().addNode("name_p1", "modetest:intermediate");
        Node parent2 = session.getRootNode().addNode("name_p2", "modetest:intermediate");
        parent2.addNode("name_c1", "modetest:child");
        session.save();

        try {
            String queryString = "SELECT parent.* FROM [modetest:intermediate] as parent LEFT OUTER JOIN [modetest:child] as child ON ISCHILDNODE(child, parent)"
                                 + " WHERE parent.[jcr:name] LIKE 'name%' OR child.[jcr:name] LIKE 'name%'";
            assertNodesAreFound(queryString, Query.JCR_SQL2, "/name_p1", "/name_p2");
        } finally {
            parent1.remove();
            parent2.remove();
            session.save();
        }
    }

    @FixFor( "MODE-2027" )
    @Test
    public void shouldSearchAllPropertiesUsingDotSelectorJCRSql2FullTextSearch() throws RepositoryException {
        String sql = "SELECT cars.[jcr:path] FROM [car:Car] AS cars WHERE contains(., 'Toyota') ";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        final List<String> actual = new ArrayList<String>();
        validateQuery().onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                actual.add(row.getNode().getIdentifier());
            }
        }).validate(query, query.execute());

        sql = "SELECT cars.[jcr:path] FROM [car:Car] AS cars WHERE contains(cars.*, 'Toyota')";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        final List<String> expected = new ArrayList<String>();
        validateQuery().rowCount(actual.size()).onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                expected.add(row.getNode().getIdentifier());
            }
        }).validate(query, query.execute());

        assertEquals(expected, actual);
    }

    @FixFor( "MODE-2062" )
    @Test
    public void fullTextShouldWorkWithBindVar() throws Exception {
        Node n1 = session.getRootNode().addNode("n1");
        n1.setProperty("n1-prop-1", "wow");
        n1.setProperty("n1-prop-2", "any");

        Node n2 = session.getRootNode().addNode("n2");
        n2.setProperty("n2-prop-1", "test");

        try {
            session.save();

            // test with literal
            String queryString = "select * from [nt:unstructured] as a where contains(a.*, 'wow')";
            assertNodesAreFound(queryString, Query.JCR_SQL2, "/n1");

            // test with bind
            String queryStringWithBind = "select * from [nt:unstructured] as a where contains(a.*, $text)";
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(queryStringWithBind, Query.JCR_SQL2);
            query.bindValue("text", session.getValueFactory().createValue("wow"));
            QueryResult result = query.execute();
            validateQuery().rowCount(1).hasNodesAtPaths("/n1").validate(query, result);
        } finally {
            n1.remove();
            n2.remove();
            session.save();
        }
    }

    @FixFor( "MODE-2095" )
    @Test
    public void shouldSearchUsingDateRangeQuery() throws Exception {
        Node testParent = session.getRootNode().addNode("date_parent");
        Node node1 = testParent.addNode("date_node1");
        node1.setProperty("now", Calendar.getInstance());
        Node node2 = testParent.addNode("date_node2");
        node2.setProperty("now", Calendar.getInstance());
        session.save();

        try {
            String queryString = "select * from [nt:unstructured] as node where "
                                 + "node.now <= CAST('2999-10-21T00:00:00.000' AS DATE) and node.now >= CAST('1999-10-21T00:00:00.000' AS DATE)";
            assertNodesAreFound(queryString, Query.JCR_SQL2, "/date_parent/date_node1", "/date_parent/date_node2");
        } finally {
            testParent.remove();
            session.save();
        }
    }

    // // ----------------------------------------------------------------------------------------------------------------
    // // Full-text Search Queries
    // // ----------------------------------------------------------------------------------------------------------------
    //
    @FixFor( "MODE-1418" )
    @Test
    public void shouldBeAbleToCreateAndExecuteFullTextSearchQueryOfPhrase() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("cat wearing", JcrRepository.QueryLanguage.SEARCH);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns(searchColumnNames()).validate(query, result);
    }

    @FixFor( "MODE-905" )
    @Test
    public void shouldBeAbleToCreateAndExecuteFullTextSearchQuery() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("land", JcrRepository.QueryLanguage.SEARCH);
        QueryResult result = query.execute();
        validateQuery().rowCount(4).hasColumns(searchColumnNames()).validate(query, result);
    }

    @FixFor( "MODE-905" )
    @Test
    public void shouldBeAbleToCreateAndExecuteFullTextSearchQueryWithName() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("highlander", JcrRepository.QueryLanguage.SEARCH);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns(searchColumnNames()).validate(query, result);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // JCR-SQL Queries
    // ----------------------------------------------------------------------------------------------------------------

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryWithOrderByClause() throws RepositoryException {
        String sql = "SELECT car:model FROM car:Car WHERE car:model IS NOT NULL ORDER BY car:model ASC";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = query.execute();
        validateQuery().rowCount(13).hasColumns("jcr:path", "jcr:score", "car:model").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryWithOrderByPathClause() throws RepositoryException {
        String sql = "SELECT car:model FROM car:Car WHERE car:model IS NOT NULL ORDER BY PATH() ASC";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = query.execute();
        validateQuery().rowCount(13).hasColumns("jcr:path", "jcr:score", "car:model").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryWithPathCriteriaAndOrderByClause() throws RepositoryException {
        String sql = "SELECT car:model FROM car:Car WHERE jcr:path LIKE '/Cars/%' ORDER BY car:model ASC";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = query.execute();
        validateQuery().rowCount(13).hasColumns("jcr:path", "jcr:score", "car:model").validate(query, result);
    }

    /**
     * Tests that the child nodes (but no grandchild nodes) are returned.
     *
     * @throws RepositoryException
     */
    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryWithChildAxisCriteria() throws RepositoryException {
        String sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '/Cars/%' AND NOT jcr:path LIKE '/Cars/%/%'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = query.execute();
        validateQuery().rowCount(4).hasColumns(allColumnNames()).validate(query, result);
    }

    /**
     * Tests that the child nodes (but no grandchild nodes) are returned.
     *
     * @throws RepositoryException
     */
    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryWithContainsCriteria() throws RepositoryException {
        String sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '/Cars/%' AND NOT jcr:path LIKE '/Cars/%/%'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = query.execute();
        validateQuery().rowCount(4).hasColumns(allColumnNames()).validate(query, result);
    }

    @Test
    @FixFor( "MODE-791" )
    @SuppressWarnings( "deprecation" )
    public void shouldReturnNodesWithPropertyConstrainedByTimestamp() throws Exception {
        String sql = "SELECT car:model, car:year, car:maker FROM car:Car " + "WHERE jcr:path LIKE '/Cars/%' "
                     + "AND (car:msrp LIKE '$3%' OR car:msrp LIKE '$2') " + "AND (car:year LIKE '2008' OR car:year LIKE '2009') "
                     + "AND car:valueRating > '1' " + "AND jcr:created > TIMESTAMP '1974-07-10T00:00:00.000-05:00' "
                     + "AND jcr:created < TIMESTAMP '3074-07-10T00:00:00.000-05:00'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = query.execute();
        String[] columnNames = {"car:model", "car:year", "car:maker", "jcr:path", "jcr:score"};
        validateQuery().rowCount(5).hasColumns(columnNames).onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                assertTrue(row.getNode().hasProperty("car:model"));
            }
        }).validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedPathInSelectOfJcrSqlQuery() throws RepositoryException {
        String sql = "select jcr:primaryType, jcr:path FROM nt:base";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = query.execute();
        String[] columnNames = {"jcr:primaryType", "jcr:path", "jcr:score"};
        validateQuery().rowCount(totalNodeCount).hasColumns(columnNames).validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedPathInSelectAndCriteriaOfJcrSqlQuery() throws RepositoryException {
        String sql = "select jcr:primaryType, jcr:path FROM nt:base WHERE jcr:path LIKE '/Cars/%'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = query.execute();
        String[] columnNames = {"jcr:primaryType", "jcr:path", "jcr:score"};
        validateQuery().rowCount(17).hasColumns(columnNames).validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedPathInSelectAndUnqualifiedNameInCriteriaOfJcrSqlQuery() throws RepositoryException {
        String sql = "select jcr:primaryType, jcr:path FROM nt:base WHERE jcr:name LIKE '%3%'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = query.execute();
        String[] columnNames = {"jcr:primaryType", "jcr:path", "jcr:score"};
        validateQuery().rowCount(4).hasColumns(columnNames).validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedPathInSelectAndUnqualifiedLocalNameInCriteriaOfJcrSqlQuery()
        throws RepositoryException {
        String sql = "select jcr:primaryType, jcr:path FROM nt:base WHERE mode:localName LIKE '%3%'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = query.execute();
        String[] columnNames = {"jcr:primaryType", "jcr:path", "jcr:score"};
        validateQuery().rowCount(4).hasColumns(columnNames).validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithJcrPathInJoinCriteriaOfJcrSqlQuery() throws RepositoryException {
        String sql = "select nt:base.jcr:primaryType, nt:base.jcr:path, car:Car.car:year "
                     + "FROM nt:base, car:Car WHERE car:Car.jcr:path = nt:base.jcr:path";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = query.execute();
        String[] columnNames = {"nt:base.jcr:primaryType", "nt:base.jcr:path", "car:Car.car:year", "jcr:path", "jcr:score"};
        validateQuery().rowCount(13).hasColumns(columnNames).validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @FixFor( "MODE-934" )
    @Test
    public void shouldNotIncludePseudoColumnsInSelectStarOfJcrSqlQuery() throws RepositoryException {
        String sql = "select * FROM nt:base";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = query.execute();
        validateQuery().rowCount(totalNodeCount).hasColumns(allColumnNames()).validate(query, result);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // XPath Queries
    // ----------------------------------------------------------------------------------------------------------------

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToCreateXPathQuery() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//element(*,car:Car)", Query.XPATH);
        validateQuery().rowCount(13).validate(query, query.execute());

        query = session.getWorkspace().getQueryManager().createQuery("//element(*,nt:unstructured)", Query.XPATH);
        validateQuery().rowCount(24).validate(query, query.execute());
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllNodes() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//element(*,nt:base)", Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(totalNodeCount).hasColumns(allColumnNames()).validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllNodesOrderingByPath() throws RepositoryException {
        String xpath = "//element(*,nt:base) order by @jcr:path";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(totalNodeCount).hasColumns(allColumnNames()).validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllNodesOrderingByAttribute() throws RepositoryException {
        String xpath = "//element(*,car:Car) order by @car:maker";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(13).hasColumns("car:maker", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllUnstructuredNodes() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//element(*,nt:unstructured)", Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(24).hasColumns(allColumnNames()).validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllUnstructuredNodesOrderedByPropertyValue() throws RepositoryException {
        QueryManager manager = session.getWorkspace().getQueryManager();
        Query query = manager.createQuery("//element(*,nt:unstructured) order by @jcr:primaryType", Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(24).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);

        query = manager.createQuery("//element(*,car:Car) order by @car:year", Query.XPATH);
        result = query.execute();
        validateQuery().rowCount(13).hasColumns("car:year", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodesUnderNode() throws RepositoryException {
        String xpath = " /jcr:root/Cars/Hybrid/*";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(3).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodesUnderNodeAndWithProperty() throws RepositoryException {
        String xpath = " /jcr:root/Cars/Hybrid/*[@car:year]";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(3).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodesUnderNodeAndWithPropertyOrderedByProperty() throws RepositoryException {
        String xpath = " /jcr:root/Cars/Hybrid/*[@car:year] order by @car:year ascending";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(3).hasColumns("car:year", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodesUnderPath() throws RepositoryException {
        String xpath = " /jcr:root/Cars//*";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(17).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodesWithAllSnsIndexesUnderPath() throws RepositoryException {
        String xpath = " /jcr:root//NodeA";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(3).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodesUnderPathAndWithProperty() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery(" /jcr:root/Cars//*[@car:year]", Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(13).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodesUnderPathAndWithPropertyOrderedByProperty() throws RepositoryException {
        String xpath = " /jcr:root/Cars//*[@car:year] order by @car:year ascending";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(13).hasColumns("car:year", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllUnstructuredNodesOrderedByScore() throws RepositoryException {
        String xpath = "//element(*,nt:unstructured) order by jcr:score()";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(24).hasColumns(allColumnNames()).validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindSameNameSiblingsByIndex() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/Other/NodeA", Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(3).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);

        query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/Other/NodeA[2]", Query.XPATH);
        result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").hasNodesAtPaths("/Other/NodeA[2]")
                       .validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllCarNodes() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//element(*,car:Car)", Query.XPATH);
        QueryResult result = query.execute();
        String[] columnNames = {"jcr:primaryType", "jcr:mixinTypes", "jcr:path", "jcr:score", "jcr:created", "jcr:createdBy",
            "jcr:name", "mode:localName", "mode:depth", "mode:id", "car:mpgCity", "car:userRating", "car:mpgHighway",
            "car:engine", "car:model", "car:year", "car:maker", "car:lengthInInches", "car:valueRating", "car:wheelbaseInInches",
            "car:msrp", "car:alternateModels"};
        validateQuery().rowCount(13).hasColumns(columnNames).validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindRootNode() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root", Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindChildOfRootNode() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/Cars", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindChildOfRootNodeWithTypeCriteria() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/Cars[@jcr:primaryType]", Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodeWithPathAndAttrbuteCriteria() throws RepositoryException {
        String xpath = "/jcr:root/Cars/Sports/Infiniti_x0020_G37[@car:year='2008']";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodeWithAttrbuteCriteria() throws RepositoryException {
        String xpath = "//Infiniti_x0020_G37[@car:year='2008']";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodeWithPathUnderRootAndAttrbuteCriteria() throws RepositoryException {
        String xpath = "/jcr:root/NodeB[@myUrl='http://www.acme.com/foo/bar']";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @FixFor( "MODE-686" )
    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAnywhereNodeWithNameAndAttrbuteCriteriaMatchingUrl()
        throws RepositoryException {
        String xpath = "//NodeB[@myUrl='http://www.acme.com/foo/bar']";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodeWithNameMatch() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//NodeB", Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    /*
     * Adding this test case since its primarily a test of integration between the RNTM and the query engine
     */
    @Test( expected = InvalidNodeTypeDefinitionException.class )
    public void shouldNotAllowUnregisteringUsedPrimaryType() throws Exception {
        Session adminSession = null;
        try {
            adminSession = repository.login();
            assertThat(adminSession.getNamespaceURI("car"), is("http://www.modeshape.org/examples/cars/1.0"));
            // Re-register one of the used namespaces in a session ...
            adminSession.setNamespacePrefix("cars", "http://www.modeshape.org/examples/cars/1.0");

            JcrNodeTypeManager nodeTypeManager = (JcrNodeTypeManager)adminSession.getWorkspace().getNodeTypeManager();
            nodeTypeManager.unregisterNodeTypes(Collections.singletonList("cars:Car"));
        } finally {
            if (adminSession != null) adminSession.logout();
        }
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithContainsCriteria() throws RepositoryException {
        String xpath = "/jcr:root//*[jcr:contains(., 'liter')]";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        validateQuery().rowCount(2).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithContainsCriteriaAndPluralWord() throws RepositoryException {
        String xpath = "/jcr:root//*[jcr:contains(., 'liters')]";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteria() throws RepositoryException {
        String xpath = "/jcr:root//*[jcr:contains(., '\"liters V 12\"')]";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteriaWithHyphen() throws RepositoryException {
        String xpath = "/jcr:root//*[jcr:contains(., '\"5-speed\"')]";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteriaWithHyphenAndNumberAndWildcard()
        throws RepositoryException {
        String xpath = "/jcr:root//*[jcr:contains(., '\"spee*\"')]";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(2).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteriaWithNoHyphenAndNoWildcard() throws RepositoryException {
        String xpath = "/jcr:root//*[jcr:contains(., '\"heavy duty\"')]";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();

        // by default there is no stemming or punctuation replacement
        assertFalse(result.getRows().hasNext());
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteriaWithHyphenAndNoWildcard() throws RepositoryException {
        String xpath = "/jcr:root//*[jcr:contains(., '\"heavy-duty\"')]";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteriaWithNoHyphenAndWildcard() throws RepositoryException {
        String xpath = "/jcr:root//*[jcr:contains(., '\"heavy-du*\"')]";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteriaWithNoHyphenAndLeadingWildcard()
        throws RepositoryException {
        String xpath = "/jcr:root//*[jcr:contains(., '\"*avy-duty\"')]";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteriaWithHyphenAndWildcard() throws RepositoryException {
        String xpath = "/jcr:root//*[jcr:contains(., '\"heavy-du*\"')]";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @Ignore
    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteriaWithHyphenAndLeadingWildcard()
        throws RepositoryException {
        String xpath = "/jcr:root//*[jcr:contains(., '\"*-speed\"')]";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(2).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @FixFor( "MODE-790" )
    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithCompoundCriteria() throws Exception {
        String xpath = "/jcr:root/Cars//element(*,car:Car)[@car:year='2008' and jcr:contains(., '\"liters V 12\"')]";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        String[] columnNames = {"jcr:primaryType", "jcr:mixinTypes", "jcr:path", "jcr:score", "jcr:created", "jcr:createdBy",
            "jcr:name", "mode:localName", "mode:depth", "mode:id", "car:mpgCity", "car:userRating", "car:mpgHighway",
            "car:engine", "car:model", "car:year", "car:maker", "car:lengthInInches", "car:valueRating", "car:wheelbaseInInches",
            "car:msrp", "car:alternateModels"};
        validateQuery().rowCount(1).hasColumns(columnNames).validate(query, result);

        // Query again with a different criteria that should return no nodes ...
        xpath = "/jcr:root/Cars//element(*,car:Car)[@car:year='2007' and jcr:contains(., '\"liter V 12\"')]";
        query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = query.execute();
        validateQuery().rowCount(0).hasColumns(columnNames).validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithElementTestForChildrenOfRoot() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/element()", Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(4).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithElementTestForAllNodesBelowRoot() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root//element()", Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(totalNodeCount).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithElementTestForChildOfRootWithName() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/element(Cars)", Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithElementTestForSingleNodeBelowRootWithName() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root//element(Utility)", Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithElementTestForChildrenOfRootWithName() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/Other/element(NodeA)", Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(3).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithElementTestForMultipleNodesBelowRootWithName() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root//element(NodeA)", Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(3).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @Ignore
    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithRangeCriteria() throws RepositoryException {
        String xpath = "/jcr:root/Other/*[@somethingElse <= 'value2' and @somethingElse > 'value1']";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithNewlyRegisteredNamespace() throws RepositoryException {
        NamespaceRegistry namespaceRegistry = session.getWorkspace().getNamespaceRegistry();
        namespaceRegistry.registerNamespace("newPrefix", "newUri");

        try {
            // We don't have any elements that use this yet, but let's at least verify that it can execute.
            Query query = session.getWorkspace().getQueryManager()
                                 .createQuery("//*[@newPrefix:someColumn = 'someValue']", Query.XPATH);
            query.execute();
        } finally {
            namespaceRegistry.unregisterNamespace("newPrefix");
        }
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldNotReturnNodesWithNoPropertyForPropertyCriterion() throws Exception {
        String xpath = "/jcr:root/Cars//*[@car:wheelbaseInInches]";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                assertThat(row.getNode().hasProperty("car:wheelbaseInInches"), is(true));
            }
        }).validate(query, result);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldNotReturnNodesWithNoPropertyForLikeCriterion() throws Exception {
        String xpath = "/jcr:root/Cars//*[jcr:like(@car:wheelbaseInInches, '%')]";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:primaryType", "jcr:path", "jcr:score").onEachRow(new Predicate() {
            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                assertThat(row.getNode().hasProperty("car:wheelbaseInInches"), is(true));
            }
        }).validate(query, result);
    }

    @FixFor( "MODE-1144" )
    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldParseMagnoliaXPathQuery() throws Exception {
        String xpath = "//*[@jcr:primaryType='mgnl:content']//*[jcr:contains(., 'paragraph')]";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        assertNotNull(result);
        assertFalse(result.getRows().hasNext());
    }

    // ----------------------------------------------------------------------------------------------------------------
    // QOM Queries
    // ----------------------------------------------------------------------------------------------------------------

    @FixFor( "MODE-1468" )
    @Test
    public void shouldAllowCreationAndExecutionOfQueryObjectModel() throws Exception {
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        QueryObjectModelFactory qomFactory = queryManager.getQOMFactory();
        Selector selector = qomFactory.selector("car:Car", "car");
        PropertyValue propValue = qomFactory.propertyValue("car", "car:userRating");
        Literal literal = qomFactory.literal(session.getValueFactory().createValue("4")); // use a String since it's LIKE
        Constraint constraint = qomFactory.comparison(propValue, JCR_OPERATOR_LIKE, literal);
        Column[] columns = new Column[4];
        columns[0] = qomFactory.column("car", "car:maker", "maker");
        columns[1] = qomFactory.column("car", "car:model", "car:model");
        columns[2] = qomFactory.column("car", "car:year", "car:year");
        columns[3] = qomFactory.column("car", "car:userRating", "car:userRating");
        Ordering[] orderings = null;

        // Build and execute the query ...
        Query query1 = qomFactory.createQuery(selector, constraint, orderings, columns);
        QueryResult result1 = query1.execute();
        String[] columnNames = {"maker", "car:model", "car:year", "car:userRating"};
        validateQuery().rowCount(4).hasColumns(columnNames).validate(query1, result1);

        // Now get the JCR-SQL2 statement from the QOM ...
        String expectedExpr = "SELECT car.[car:maker] AS maker, car.[car:model], car.[car:year], car.[car:userRating] FROM [car:Car] AS car WHERE car.[car:userRating] LIKE '4'";
        String expr = query1.getStatement();
        assertThat(expr, is(expectedExpr));

        // Now execute it ...
        Query query2 = queryManager.createQuery(expr, Query.JCR_SQL2);
        QueryResult result2 = query2.execute();
        validateQuery().rowCount(4).hasColumns(columnNames).validate(query2, result2);
    }

    @FixFor( "MODE-1468" )
    @Test
    public void shouldAllowCreationAndExecutionOfQueryObjectModelWithLimit() throws Exception {
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        QueryObjectModelFactory qomFactory = queryManager.getQOMFactory();
        Selector selector = qomFactory.selector("car:Car", "car");
        PropertyValue propValue = qomFactory.propertyValue("car", "car:userRating");
        Literal literal = qomFactory.literal(session.getValueFactory().createValue("4")); // use a String since it's LIKE
        Constraint constraint = qomFactory.comparison(propValue, JCR_OPERATOR_LIKE, literal);
        Column[] columns = new Column[4];
        columns[0] = qomFactory.column("car", "car:maker", "maker");
        columns[1] = qomFactory.column("car", "car:model", "car:model");
        columns[2] = qomFactory.column("car", "car:year", "car:year");
        columns[3] = qomFactory.column("car", "car:userRating", "car:userRating");
        Ordering[] orderings = null;
        Limit limit = qomFactory.limit(2, 0);
        boolean isDistinct = false;

        // Build and execute the query ...
        SelectQuery selectQuery = qomFactory.select(selector, constraint, orderings, columns, limit, isDistinct);
        Query query1 = qomFactory.createQuery(selectQuery);
        QueryResult result1 = query1.execute();
        String[] columnNames = {"maker", "car:model", "car:year", "car:userRating"};
        validateQuery().rowCount(2).hasColumns(columnNames).validate(query1, result1);

        // Now get the JCR-SQL2 statement from the QOM ...
        String expectedExpr = "SELECT car.[car:maker] AS maker, car.[car:model], car.[car:year], car.[car:userRating] FROM [car:Car] AS car WHERE car.[car:userRating] LIKE '4' LIMIT 2";
        String expr = query1.getStatement();
        assertThat(expr, is(expectedExpr));

        // Now execute it ...
        Query query2 = queryManager.createQuery(expr, Query.JCR_SQL2);
        QueryResult result2 = query2.execute();
        validateQuery().rowCount(2).hasColumns(columnNames).validate(query2, result2);
    }

    @Test
    @FixFor( "MODE-2209" )
    public void queriesShouldTakePermissionsIntoAccount() throws Exception {
        AccessControlManager acm = session.getAccessControlManager();

        Node parent = session.getRootNode().addNode("parent");
        parent.addNode("child1");
        AccessControlList acl = acl("/parent/child1");
        parent.addNode("child2");
        session.save();

        try {
            String queryString = "select [jcr:path] from [nt:unstructured] as node where ISCHILDNODE(node, '/parent')";
            assertNodesAreFound(queryString, Query.JCR_SQL2, "/parent/child1", "/parent/child2");

            // remove the READ permission for child1
            acl.addAccessControlEntry(SimplePrincipal.EVERYONE,
                                      new Privilege[] {acm.privilegeFromName(Privilege.JCR_WRITE),
                                          acm.privilegeFromName(Privilege.JCR_REMOVE_NODE),
                                          acm.privilegeFromName(Privilege.JCR_MODIFY_ACCESS_CONTROL)});
            acm.setPolicy("/parent/child1", acl);
            session.save();

            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);
            QueryResult result = query.execute();

            // assert that only child2 is still visible in the query results
            NodeIterator nodes = result.getNodes();
            assertEquals(1, nodes.getSize());
            assertEquals("/parent/child2", nodes.nextNode().getPath());
            assertFalse(nodes.hasNext());

            RowIterator rows = result.getRows();
            assertEquals(1, rows.getSize());
            assertEquals("/parent/child2", rows.nextRow().getNode().getPath());
            assertFalse(rows.hasNext());
        } finally {
            acl.addAccessControlEntry(SimplePrincipal.EVERYONE, new Privilege[] {acm.privilegeFromName(Privilege.JCR_ALL)});
            acm.setPolicy("/parent/child1", acl);
            session.save();

            acm.removePolicy("/parent/child1", null);
            parent.remove();
            session.save();
        }
    }

    @Test
    @FixFor( "MODE-2220" )
    public void shouldSupportLowerCaseOperand() throws Exception {
        Node nodeA = session.getRootNode().addNode("A");
        nodeA.setProperty("something", "SOME UPPERCASE TEXT");
        session.save();

        try {
            String sql = "SELECT [jcr:path] FROM [nt:unstructured] AS node WHERE LOWER(node.something) LIKE '%uppercase%'";
            Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            NodeIterator nodes = query.execute().getNodes();
            assertEquals(1, nodes.getSize());
            assertEquals("/A", nodes.nextNode().getPath());
        } finally {
            nodeA.remove();
            session.save();
        }
    }

    @Test
    @FixFor( "MODE-2220" )
    public void shouldSupportUpperCaseOperand() throws Exception {
        String sql = "SELECT [jcr:path] FROM [nt:unstructured] AS node WHERE UPPER(node.something) LIKE '%FOX%'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        NodeIterator nodes = query.execute().getNodes();
        assertEquals(1, nodes.getSize());
        assertEquals("/Other/NodeA", nodes.nextNode().getPath());
    }
    
    @Test
    @FixFor( "MODE-2403" )
    public void likeOperandShouldBeCaseSensitive() throws Exception {
        Node n = session.getRootNode().addNode("N");
        n.setProperty("prop", "Capital0");
        session.save();

        try {
            String sql = "SELECT [jcr:path] FROM [nt:unstructured] AS node WHERE node.prop LIKE 'capital%'";
            Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            NodeIterator nodes = query.execute().getNodes();
            assertEquals(0, nodes.getSize());

            sql = "SELECT [jcr:path] FROM [nt:unstructured] AS node WHERE node.prop LIKE '%capital%'";
            query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            nodes = query.execute().getNodes();
            assertEquals(0, nodes.getSize());

            sql = "SELECT [jcr:path] FROM [nt:unstructured] AS node WHERE node.prop LIKE 'Capital%'";
            query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            nodes = query.execute().getNodes();
            assertEquals(1, nodes.getSize());
            assertEquals("/N", nodes.nextNode().getPath());

            sql = "SELECT [jcr:path] FROM [nt:unstructured] AS node WHERE node.prop LIKE '%Capital%'";
            query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            nodes = query.execute().getNodes();
            assertEquals(1, nodes.getSize());
            assertEquals("/N", nodes.nextNode().getPath());
        } finally {
            n.remove();
            session.save();
        }
    }
    
    @Test
    @FixFor( "MODE-2247" )
    public void shouldBeAbleToExecuteIntersectOperationWithSimpleCriteria() throws Exception {
        String sql1 = "SELECT car1.[jcr:path] FROM [car:Car] AS car1";
        String sql2 = "SELECT car2.[jcr:path] FROM [car:Car] AS car2 WHERE car2.[car:mpgCity] = 12";
        String queryString = sql1 + " INTERSECT " + sql2;

        List<String> expectedPaths = new ArrayList<>(Arrays.asList("/Cars/Sports/Aston Martin DB9",
                                                                   "/Cars/Utility/Land Rover LR3"));

        Query query = session.getWorkspace().getQueryManager().createQuery(queryString, Query.JCR_SQL2);
        NodeIterator nodes = query.execute().getNodes();
        assertEquals(2, nodes.getSize());
        while (nodes.hasNext()) {
            String path = nodes.nextNode().getPath();
            assertTrue(path + " not found", expectedPaths.remove(path));
        }
    }

    @Test
    @FixFor( "MODE-2247" )
    public void shouldBeAbleToExecuteIntersectOperationWithJoinCriteria() throws RepositoryException {
        String sql = "SELECT category.[jcr:path] AS p FROM [nt:unstructured] AS category "
                     + " INTERSECT  "
                     + "SELECT category.[jcr:path] AS p FROM [nt:unstructured] AS category JOIN [car:Car] AS cars ON ISCHILDNODE(cars,category) WHERE cars.[jcr:name]='Land Rover LR3'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("p").hasNodesAtPaths("/Cars/Utility").validate(query, result);
    }

    @Test
    @FixFor( "MODE-2247" )
    public void shouldBeAbleToExecuteIntersectAllOperation() throws RepositoryException {
        String sql = "SELECT category.[jcr:path] AS p FROM [nt:unstructured] AS category "
                     + " INTERSECT ALL "
                     + "SELECT category.[jcr:path] AS p FROM [nt:unstructured] AS category JOIN [car:Car] AS cars ON ISCHILDNODE(cars,category) WHERE cars.[jcr:name] LIKE '%Rover%'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(2).hasColumns("p").hasNodesAtPaths("/Cars/Utility", "/Cars/Utility").validate(query, result);
    }

    @Test
    @FixFor( "MODE-2247" )
    public void shouldBeAbleToExecuteExceptOperationWithSimpleCriteria() throws Exception {
        String sql1 = "SELECT car1.[jcr:path] FROM [car:Car] AS car1 WHERE car1.[jcr:name] LIKE '%Land Rover%'";
        String sql2 = "SELECT car2.[jcr:path] FROM [car:Car] AS car2 WHERE car2.[jcr:name] LIKE '%LR3%'";
        String queryString = sql1 + " EXCEPT " + sql2;
        Query query = session.getWorkspace().getQueryManager().createQuery(queryString, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasColumns("jcr:path").hasNodesAtPaths("/Cars/Utility/Land Rover LR2")
                       .validate(query, result);
    }

    @Test
    @FixFor( "MODE-2247" )
    public void shouldBeAbleToExecuteExceptOperationWithJoinCriteria() throws RepositoryException {
        String sql = "SELECT category.[jcr:path] AS p FROM [nt:unstructured] AS category WHERE ISCHILDNODE(category,'/Cars')"
                     + "EXCEPT "
                     + "SELECT category.[jcr:path] AS p FROM [nt:unstructured] AS category JOIN [car:Car] AS cars ON ISCHILDNODE(cars,category) "
                     + " WHERE cars.[jcr:name] LIKE '%Rover%' OR cars.[jcr:name] LIKE '%Toyota%'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        List<String> expectedPaths = new ArrayList<>(Arrays.asList("/Cars/Sports", "/Cars/Luxury"));

        NodeIterator nodes = query.execute().getNodes();
        assertEquals(2, nodes.getSize());
        while (nodes.hasNext()) {
            String path = nodes.nextNode().getPath();
            assertTrue(path + " not found", expectedPaths.remove(path));
        }
    }

    @Test
    @FixFor( "MODE-2247" )
    public void shouldBeAbleToExecuteExceptAllOperation() throws RepositoryException {
        String sql = "SELECT category.[jcr:path] AS p FROM [nt:unstructured] AS category JOIN [car:Car] AS cars ON ISCHILDNODE(cars,category) "
                     + " WHERE cars.[jcr:name] LIKE '%Rover%' "
                     + "EXCEPT ALL "
                     + "SELECT node.[jcr:path] AS p FROM [nt:unstructured] AS node WHERE NOT ISCHILDNODE(node,'/Cars')";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(2).hasColumns("p").hasNodesAtPaths("/Cars/Utility", "/Cars/Utility").validate(query, result);
    }

    @Test
    @FixFor( "MODE-2267" )
    public void shouldAllowNameAndLocalNameConstraints() throws Exception {
        Node node1 = session.getRootNode().addNode("test:testNode1");
        Node node2 = session.getRootNode().addNode("test:testNode2");
        session.save();
        try {
            String sql = "SELECT [jcr:path] FROM [nt:unstructured] AS node WHERE node.[mode:localName] LIKE '%testNode%'";
            Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            NodeIterator nodes = query.execute().getNodes();
            assertEquals(2, nodes.getSize());

            sql = "SELECT [jcr:path] FROM [nt:unstructured] AS node WHERE node.[jcr:name] LIKE '%test%'";
            query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            nodes = query.execute().getNodes();
            assertEquals(2, nodes.getSize());
        } finally {
            node1.remove();
            node2.remove();
            session.save();
        }
    }

    @Test
    @FixFor( "MODE-2275" )
    public void shouldAllowQueryingForRuntimeRegisteredNodeTypes() throws Exception {
        NamespaceRegistry namespaceRegistry = session.getWorkspace().getNamespaceRegistry();
        namespaceRegistry.registerNamespace("foo", "http://www.modeshape.org/foo/1.0");
        Node node1 = null;
        Node node2 = null;

        try {
            registerNodeType("foo:nodeType1");
            node1 = session.getRootNode().addNode("foo1", "foo:nodeType1");
            session.save();
            String sql = "SELECT node.[jcr:name] FROM [foo:nodeType1] AS node";
            Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            NodeIterator nodes = query.execute().getNodes();
            assertEquals(1, nodes.getSize());
            assertEquals("foo1", nodes.nextNode().getName());

            registerNodeType("foo:nodeType2");
            node2 = session.getRootNode().addNode("foo2", "foo:nodeType2");
            session.save();
            sql = "SELECT node.[jcr:name] FROM [foo:nodeType2] AS node";
            query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            nodes = query.execute().getNodes();
            assertEquals(1, nodes.getSize());
            assertEquals("foo2", nodes.nextNode().getName());
        } finally {
            // remove the nodes to avoid influencing the other tests
            if (node1 != null) {
                node1.remove();
            }
            if (node2 != null) {
                node2.remove();
            }
            session.save();

            // remove the custom types and namespaces to avoid influencing the other tests
            JcrNodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();
            nodeTypeManager.unregisterNodeType("foo:nodeType1");
            nodeTypeManager.unregisterNodeType("foo:nodeType2");
            namespaceRegistry.unregisterNamespace("foo");
        }
    }

    @Test
    @FixFor( "MODE-2329" )
    public void shouldAllowUsingExpandedSelectorNameInQOM() throws Exception {
        QueryObjectModelFactory qomFactory = session.getWorkspace().getQueryManager().getQOMFactory();

        Selector selector = qomFactory.selector(NodeType.NT_BASE, "category");

        // Build and execute the query ...
        Query query = qomFactory.createQuery(selector, qomFactory.childNode("category", "/Cars"), null, new Column[0]);
        assertThat(query.getStatement(), is("SELECT * FROM [{http://www.jcp.org/jcr/nt/1.0}base] AS category WHERE ISCHILDNODE(category,'/Cars')"));
        QueryResult result = query.execute();
        validateQuery().rowCount(4).hasColumns(allColumnNames("category")).validate(query, result);
    }

    @Test
    @FixFor( { "MODE-1055", "MODE-2347" } )
    public void shouldAllowMissingSelectorColumnsInQOM() throws Exception {
        Node node = session.getRootNode().addNode("test", "modetest:simpleType");
        node.setProperty("fieldA", "A_value");
        session.save();
        // query for a property present in a subtype which doesn't have any residuals, using a super-type selector
        String sql = "SELECT * FROM [nt:base] AS node WHERE node.fieldA = 'A_value'";
        try {
            QueryObjectModelFactory qomFactory = session.getWorkspace().getQueryManager().getQOMFactory();
            Selector selector = qomFactory.selector("nt:base", "node");
            PropertyValue propValue = qomFactory.propertyValue("node", "fieldA");
            Literal literal = qomFactory.literal(session.getValueFactory().createValue("A_value"));
            Constraint constraint = qomFactory.comparison(propValue, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal);
            Query query = qomFactory.createQuery(selector, constraint, null, new Column[0]);

            assertThat(query.getStatement(), is(sql));
            QueryResult result = query.execute();
            validateQuery().rowCount(1).validate(query, result);
        } finally {
            node.remove();
            session.save();
        }
    }

    @Test
    @FixFor( "MODE-2401" )
    public void shouldNotReturnNonQueryableNodeTypes() throws Exception {
        Node folder1 = session.getRootNode().addNode("folder1", "test:noQueryFolder");
        Node folder2 = session.getRootNode().addNode("folder2", "nt:folder");
        Node folder3 = session.getRootNode().addNode("folder3", "test:noQueryFolder");
        Node folder31 = folder3.addNode("folder3_1", "nt:folder");
        Node folder311 = folder31.addNode("folder3_1", "test:noQueryFolder");
        folder311.addNode("folder3_1_1", "nt:folder");
        session.save();

        try {
            String sql = "SELECT folder.[jcr:name] FROM [nt:folder] AS folder";
            Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            NodeIterator nodes = query.execute().getNodes();
            assertEquals(3, nodes.getSize());
            Set<String> names = new TreeSet<>();
            while (nodes.hasNext()) {
                names.add(nodes.nextNode().getName());
            }
            assertArrayEquals(new String[] { "folder2", "folder3_1", "folder3_1_1" }, names.toArray(new String[0]));
        } finally {
            folder1.remove();
            folder2.remove();
            folder3.remove();
            session.save();
        }
    } 
    
    @Test
    @FixFor( "MODE-2401" )
    public void shouldNotReturnNodeWithNoQueryMixin() throws Exception {
        Node folder1 = session.getRootNode().addNode("folder1", "nt:folder");
        folder1.addMixin("test:noQueryMixin");
        Node folder2 = session.getRootNode().addNode("folder2", "nt:folder");
        session.save();

        String sql = "SELECT folder.[jcr:name] FROM [nt:folder] AS folder";

        try {
            Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            NodeIterator nodes = query.execute().getNodes();
            assertEquals(1, nodes.getSize());
            Set<String> names = new TreeSet<>();
            while (nodes.hasNext()) {
                names.add(nodes.nextNode().getName());
            }
            assertArrayEquals(new String[] { "folder2" }, names.toArray(new String[0]));
            
            //add a mixin on the 2nd node and reindex
            folder2.addMixin("test:noQueryMixin");
            session.save();
            
            query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            nodes = query.execute().getNodes();
            assertEquals(0, nodes.getSize());
            
            //remove the mixins from both nodesx
            folder1.removeMixin("test:noQueryMixin");
            folder2.removeMixin("test:noQueryMixin");
            session.save();
            
            query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            nodes = query.execute().getNodes();
            assertEquals(2, nodes.getSize());
            names = new TreeSet<>();
            while (nodes.hasNext()) {
                names.add(nodes.nextNode().getName());
            }
            assertArrayEquals(new String[] { "folder1", "folder2" }, names.toArray(new String[0]));
        } finally {
            folder1.remove();
            folder2.remove();
            session.save();
        }
    }

    @Test
    @FixFor( "MODE-2491" )
    public void shouldSupportCaseOperandsForMultiValuedProperties() throws Exception {
        Node metaData = session.getRootNode().addNode("metaData", "nt:unstructured");
        metaData.setProperty("lowerCase", new String[]{"a", "b", "c"});        
        metaData.setProperty("upperCase", new String[] { "A", "B", "C" });
        session.save();

        try {
            String sql = "SELECT [jcr:path] FROM [nt:unstructured] AS node WHERE LOWER(node.upperCase) = 'a'";
            Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            NodeIterator nodes = query.execute().getNodes();
            assertEquals(1, nodes.getSize());
            assertEquals("/metaData", nodes.nextNode().getPath());

            sql = "SELECT [jcr:path] FROM [nt:unstructured] AS node WHERE LOWER(node.upperCase) = 'b'";
            query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            nodes = query.execute().getNodes();
            assertEquals(1, nodes.getSize());
            assertEquals("/metaData", nodes.nextNode().getPath());
            
            sql = "SELECT [jcr:path] FROM [nt:unstructured] AS node WHERE LOWER(node.upperCase) = 'c'";
            query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            nodes = query.execute().getNodes();
            assertEquals(1, nodes.getSize());
            assertEquals("/metaData", nodes.nextNode().getPath());   
            
            sql = "SELECT [jcr:path] FROM [nt:unstructured] AS node WHERE UPPER(node.lowerCase) = 'A'";
            query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            nodes = query.execute().getNodes();
            assertEquals(1, nodes.getSize());
            assertEquals("/metaData", nodes.nextNode().getPath());

            sql = "SELECT [jcr:path] FROM [nt:unstructured] AS node WHERE UPPER(node.lowerCase) = 'B'";
            query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            nodes = query.execute().getNodes();
            assertEquals(1, nodes.getSize());
            assertEquals("/metaData", nodes.nextNode().getPath());
            
            sql = "SELECT [jcr:path] FROM [nt:unstructured] AS node WHERE UPPER(node.lowerCase) = 'C'";
            query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            nodes = query.execute().getNodes();
            assertEquals(1, nodes.getSize());
            assertEquals("/metaData", nodes.nextNode().getPath());
        } finally {
            metaData.remove();
            session.save();
        }
    }

    @Test
    @FixFor( "MODE-2166" )
    public void shouldSupportCastDynamicOperand() throws Exception {
        String sql = "SELECT car.[jcr:path] FROM [car:Car] as car WHERE CAST(car.[car:year] AS long) = 1967";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().rowCount(1).hasNodesAtPaths("/Cars/Utility/Toyota Land Cruiser").validate(query, result);                            
    } 
    
    @Test(expected = org.modeshape.jcr.value.ValueFormatException.class)
    @FixFor( "MODE-2166" )
    public void shouldFailWhenAttemptingToCastToInvalidType() throws Exception {
        String sql = "SELECT car.[jcr:path] FROM [car:Car] as car WHERE CAST(car.[car:maker] AS DATE) = 'Toyota'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        validateQuery().validate(query, result);                            
    }
    
    @Test
    @FixFor( "MODE-1727" )
    public void shouldPerformOrderByUsingCustomLocale() throws Exception {
        Node testRoot = session.getRootNode().addNode("words", "nt:unstructured");
        Node word1 = testRoot.addNode("word1", "modetest:simpleType");
        word1.setProperty("fieldA", "peach");
        Node word2 = testRoot.addNode("word2", "modetest:simpleType");
        word2.setProperty("fieldA", "péché");
        Node word3 = testRoot.addNode("word3", "modetest:simpleType");
        word3.setProperty("fieldA", "pêche");
        Node word4 = testRoot.addNode("word4", "modetest:simpleType");
        word4.setProperty("fieldA", "sin");
        session.save();

        String sql = "SELECT word.[jcr:path] FROM [modetest:simpleType] as word ORDER BY word.fieldA ASC";

        try {
            Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            QueryResult result = query.execute();
            validateQuery()
                    .rowCount(4)
                    .hasNodesAtPaths("/words/word1", "/words/word2", "/words/word3", "/words/word4").validate(query, result);

            query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2, Locale.FRENCH);
            result = query.execute();
            validateQuery()
                    .rowCount(4)
                    .hasNodesAtPaths("/words/word1", "/words/word3", "/words/word2", "/words/word4").validate(query, result);
        } finally {
            session.getNode("/words").remove();
            session.save();
        }
    }

    private void registerNodeType( String typeName ) throws RepositoryException {
        NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();

        // Create a template for the node type ...
        NodeTypeTemplate type = nodeTypeManager.createNodeTypeTemplate();
        type.setName(typeName);
        type.setDeclaredSuperTypeNames(new String[] {"nt:unstructured"});
        type.setAbstract(false);
        type.setOrderableChildNodes(true);
        type.setMixin(false);
        type.setQueryable(true);
        nodeTypeManager.registerNodeType(type, true);
    }

    private String idList( Node... nodes ) throws RepositoryException {
        StringBuilder builder = new StringBuilder("(");
        for (int i = 0; i < nodes.length - 1; i++) {
            builder.append("'").append(nodes[i].getIdentifier()).append("'").append(",");
        }
        if (nodes.length > 0) {
            builder.append("'").append(nodes[nodes.length - 1].getIdentifier()).append("'");
        }
        builder.append(")");
        return builder.toString();
    }
}
