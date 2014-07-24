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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
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
import org.modeshape.common.util.FileUtil;
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

    private static final String[] NON_INDEXED_SYSTEM_NODES_PATHS = new String[] {"/", "/jcr:system/mode:locks",
        "/jcr:system/jcr:versionStorage"};

    private static final boolean WRITE_INDEXES_TO_FILE = false;

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
            "jcr:created", "jcr:createdBy", "jcr:name", "jcr:path", "jcr:score", "mode:depth", "mode:localName",
            "car:alternateModels"};
    }

    protected static String[] carColumnNames( String selectorName ) {
        return prefixEach(carColumnNames(), selectorName + ".");
    }

    protected static String[] allColumnNames() {
        return new String[] {"jcr:primaryType", "jcr:mixinTypes", "jcr:name", "jcr:path", "jcr:score", "mode:depth",
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
            "jcr:score", "mode:depth", "mode:localName"};
    }

    protected static String[] typedColumnNames( String selectorName ) {
        return prefixEach(typedColumnNames(), selectorName + ".");
    }

    protected static String[] searchColumnNames() {
        return new String[] {"jcr:score"};
    }

    @SuppressWarnings( "deprecation" )
    @BeforeClass
    public static void beforeAll() throws Exception {
        if (WRITE_INDEXES_TO_FILE) {
            File dir = new File("target/querytest/indexes");
            if (dir.exists()) FileUtil.delete(dir);
        }
        String simpleName = JcrQueryManagerTest.class.getSimpleName();
        String configFileName = WRITE_INDEXES_TO_FILE ? simpleName + "Disk.json" : simpleName + ".json";

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
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private static void initNodesCount() throws RepositoryException {
        JcrSession session = repository.login();
        try {
            NodeCache systemSession = repository.createSystemSession(session.context(), true);
            totalSystemNodeCount = countAllNodesBelow(systemSession.getRootKey(), systemSession)
                                   - NON_INDEXED_SYSTEM_NODES_PATHS.length;
            totalNodeCount = totalSystemNodeCount + TOTAL_NON_SYSTEM_NODE_COUNT;
        } finally {
            session.logout();
        }
    }

    private static int countAllNodesBelow( NodeKey nodeKey,
                                           NodeCache cache ) throws RepositoryException {
        int result = 1;
        CachedNode node = cache.getNode(nodeKey);
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
            if (iter.getSize() != numberOfResults && !print) {
                // print anyway since this is an error
                System.out.println();
                System.out.println(query);
                System.out.println(" plan -> " + ((JcrQueryResult)result).getPlan());
                System.out.println(result);
            }
            assertThat(iter.getSize(), is(numberOfResults));
        } else {
            try {
                result.getNodes();
                if (!print) {
                    // print anyway since this is an error
                    System.out.println();
                    System.out.println(query);
                    System.out.println(" plan -> " + ((JcrQueryResult)result).getPlan());
                    System.out.println(result);
                }
                fail("should not be able to call this method when the query has multiple selectors");
            } catch (RepositoryException e) {
                // expected; can't call this when the query uses multiple selectors ...
            }
        }
        assertThat(result.getRows().getSize(), is(numberOfResults));
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

    @FixFor( "MODE-1901" )
    @Test
    public void shouldExplainQueryWithoutExecutingQuery() throws RepositoryException {
        String sql = "SELECT * FROM [nt:file]";
        org.modeshape.jcr.api.query.Query query = (org.modeshape.jcr.api.query.Query)session.getWorkspace()
                                                                                            .getQueryManager()
                                                                                            .createQuery(sql, Query.JCR_SQL2);
        org.modeshape.jcr.api.query.QueryResult result = query.explain();
        // print = true;
        printMessage(result.getWarnings());
        assertThat(result.getWarnings().size(), is(0));
        assertResults(query, result, 0L);
        assertThat(result.getPlan().trim().length() != 0, is(true));
    }

    @FixFor( "MODE-1888" )
    @Test
    public void shouldCaptureWarningsAboutPotentialTypos() throws RepositoryException {
        String sql = "SELECT [jcr.uuid] FROM [nt:file]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        org.modeshape.jcr.api.query.QueryResult result = (org.modeshape.jcr.api.query.QueryResult)query.execute();
        // print = true;
        printMessage(result.getWarnings());
        assertThat(result.getWarnings().size(), is(1));
        assertResults(query, result, 0L);
    }

    @FixFor( "MODE-1888" )
    @Test
    public void shouldCaptureWarningsAboutUsingMisspelledColumnOnWrongSelector() throws RepositoryException {
        String sql = "SELECT file.[jcr.uuid] FROM [nt:file] AS file JOIN [mix:referenceable] AS ref ON ISSAMENODE(file,ref)";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        org.modeshape.jcr.api.query.QueryResult result = (org.modeshape.jcr.api.query.QueryResult)query.execute();
        // print = true;
        printMessage(result.getWarnings());
        assertThat(result.getWarnings().size(), is(1));
        assertResults(query, result, 0L);
    }

    @FixFor( "MODE-1888" )
    @Test
    public void shouldCaptureWarningsAboutUsingColumnOnWrongSelector() throws RepositoryException {
        String sql = "SELECT file.[jcr:uuid] FROM [nt:file] AS file JOIN [mix:referenceable] AS ref ON ISSAMENODE(file,ref)";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        org.modeshape.jcr.api.query.QueryResult result = (org.modeshape.jcr.api.query.QueryResult)query.execute();
        // print = true;
        printMessage(result.getWarnings());
        assertThat(result.getWarnings().size(), is(1));
        assertResults(query, result, 0L);
    }

    @FixFor( "MODE-1888" )
    @Test
    public void shouldCaptureWarningAboutUseOfResidualProperties() throws RepositoryException {
        String sql = "SELECT [foo_bar] FROM [nt:unstructured]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        org.modeshape.jcr.api.query.QueryResult result = (org.modeshape.jcr.api.query.QueryResult)query.execute();
        assertThat(result.getWarnings().size(), is(1));
        // print = true;
        printMessage(result.getWarnings());
        assertResults(query, result, 24L);
    }

    @FixFor( "MODE-1888" )
    @Test
    public void shouldNotCaptureWarningAboutUseOfPseudoColumns() throws RepositoryException {
        String sql = "SELECT [jcr:path] FROM [nt:unstructured]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        org.modeshape.jcr.api.query.QueryResult result = (org.modeshape.jcr.api.query.QueryResult)query.execute();
        assertThat(result.getWarnings().size(), is(0));
        // print = true;
        printMessage(result.getWarnings());
        assertResults(query, result, 24L);
    }

    @FixFor( "MODE-1888" )
    @Test
    public void shouldCaptureWarningAboutUseOfPseudoColumnWithPeriodInsteadOfColonDelimiter() throws RepositoryException {
        String sql = "SELECT [jcr.path] FROM [nt:unstructured]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        org.modeshape.jcr.api.query.QueryResult result = (org.modeshape.jcr.api.query.QueryResult)query.execute();
        assertThat(result.getWarnings().size(), is(1));
        // print = true;
        printMessage(result.getWarnings());
        assertResults(query, result, 24L);
    }

    @FixFor( "MODE-1888" )
    @Test
    public void shouldCaptureWarningAboutUseOfPseudoColumnWithUnderscoreInsteadOfColonDelimiter() throws RepositoryException {
        String sql = "SELECT [jcr_path] FROM [nt:unstructured]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        org.modeshape.jcr.api.query.QueryResult result = (org.modeshape.jcr.api.query.QueryResult)query.execute();
        assertThat(result.getWarnings().size(), is(1));
        // print = true;
        printMessage(result.getWarnings());
        assertResults(query, result, 24L);
    }

    @FixFor( "MODE-1888" )
    @Test
    public void shouldCaptureWarningAboutUseOfNonPluralJcrMixinTypeColumn() throws RepositoryException {
        String sql = "SELECT [jcr:mixinType] FROM [nt:unstructured]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        org.modeshape.jcr.api.query.QueryResult result = (org.modeshape.jcr.api.query.QueryResult)query.execute();
        assertThat(result.getWarnings().size(), is(1));
        // print = true;
        printMessage(result.getWarnings());
        assertResults(query, result, 24L);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllNodes() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [nt:base]", Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, totalNodeCount);
        assertResultsHaveColumns(result, allColumnNames("nt:base"));
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllNodesWithOrderByPath() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT * FROM [nt:base] ORDER BY [jcr:path]", Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, totalNodeCount);
        assertResultsHaveColumns(result, allColumnNames("nt:base"));
    }

    @FixFor( "MODE-1095" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithOrderByUsingPseudoColumnWithSelectStar() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT * FROM [car:Car] WHERE [car:year] < 2009 ORDER BY [jcr:path]", Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13);
        assertResultsHaveColumns(result, carColumnNames("car:Car"));
    }

    @FixFor( "MODE-1095" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithOrderByUsingColumnWithSelectStar() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT * FROM [car:Car] WHERE [car:year] < 2009 ORDER BY [car:year]", Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13);
        assertResultsHaveColumns(result, carColumnNames("car:Car"));
    }

    @FixFor( "MODE-1095" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithOrderByUsingColumnNotInSelect() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT [car:model], [car:maker] FROM [car:Car] WHERE [car:year] <= 2012 ORDER BY [car:year] DESC",
                                          Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13);
        assertResultsHaveColumns(result, "car:model", "car:maker", "car:year");
    }

    @FixFor( "MODE-1095" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithJoinCriteriaOnColumnsInSelect() throws RepositoryException {
        String sql = "SELECT x.*, y.* FROM [nt:unstructured] AS x INNER JOIN [nt:unstructured] AS y ON x.somethingElse = y.propC ORDER BY x.propC";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, allOf(allColumnNames("x"), allColumnNames("y"), new String[] {"x.propC"}));
        RowIterator rows = result.getRows();
        Row row1 = rows.nextRow();
        assertThat(row1.getNode("x").getPath(), is("/Other/NodeA"));
        assertThat(row1.getNode("y").getPath(), is("/Other/NodeA[2]"));
    }

    @FixFor( {"MODE-1095", "MODE-1680"} )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithJoinCriteriaOnColumnsNotInSelect() throws RepositoryException {
        String sql = "SELECT y.* FROM [nt:unstructured] AS x INNER JOIN [nt:unstructured] AS y ON x.somethingElse = y.propC ORDER BY x.propC";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, allOf(allColumnNames("y")));
        RowIterator rows = result.getRows();
        Row row1 = rows.nextRow();
        assertThat(row1.getNode().getPath(), is("/Other/NodeA[2]"));
    }

    @FixFor( "MODE-1055" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllNodesWithCriteria() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT * FROM [nt:base] WHERE [car:year] < 2009", Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13);
        assertResultsHaveColumns(result, allColumnNames("nt:base"));
    }

    @FixFor( "MODE-1055" )
    @Test
    public void shouldReturnNullValuesForNonExistantPropertiesInSelectClauseOfJcrSql2Query() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT bogus, laughable, [car:year] FROM [nt:base] WHERE [car:year] < 2009",
                                          Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13);
        assertResultsHaveColumns(result, "bogus", "laughable", "car:year");
        RowIterator rows = result.getRows();
        while (rows.hasNext()) {
            Row row = rows.nextRow();
            assertThat(row.getValue("bogus"), is(nullValue()));
            assertThat(row.getValue("laughable"), is(nullValue()));
            assertThat(row.getValue("car:year"), is(not(nullValue())));
        }
    }

    @FixFor( "MODE-1055" )
    @Test
    public void shouldNotMatchNodesWhenQueryUsesNonExistantPropertyInCriteriaInSelectClauseOfJcrSql2Query()
        throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT bogus, laughable, [car:year] FROM [nt:base] WHERE argle < 2009", Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 0);
        assertResultsHaveColumns(result, "bogus", "laughable", "car:year");
    }

    @FixFor( "MODE-1055" )
    @Test
    public void shouldNotOrderByNonExistantPropertyInCriteriaInSelectClauseOfJcrSql2Query() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT bogus, laughable, [car:year] FROM [nt:base] ORDER BY argle", Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, totalNodeCount);
        assertResultsHaveColumns(result, "bogus", "laughable", "argle", "car:year");
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllCarNodes() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [car:Car]", Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13);
        assertResultsHaveColumns(result, carColumnNames("car:Car"));
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllCarNodesOrderedByYear() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT * FROM [car:Car] ORDER BY [car:year]", Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13);
        assertResultsHaveColumns(result, carColumnNames("car:Car"));
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllCarNodesOrderedByMsrp() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT * FROM [car:Car] ORDER BY [car:msrp] DESC", Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13);
        assertResultsHaveColumns(result, carColumnNames("car:Car"));
        // Results are sorted by lexicographic MSRP (as a string, not as a number)!!!
        assertRow(result, 1).has("car:model", "LR3").and("car:msrp", "$48,525").and("car:mpgCity", 12);
        assertRow(result, 2).has("car:model", "IS350").and("car:msrp", "$36,305").and("car:mpgCity", 18);
        assertRow(result, 10).has("car:model", "DB9").and("car:msrp", "$171,600").and("car:mpgCity", 12);
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
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, numResults);
        assertResultsHaveColumns(result, columnNames);
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
        assertThat(result, is(notNullValue()));
        assertResults(query, result, numResults);
        String[] columnNames = {"notion:booleanProperty", "notion:booleanProperty2"};
        assertResultsHaveColumns(result, columnNames);
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
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 4L); // just 4 children below "/Cars"
        String[] expectedColumns = {"category.jcr:primaryType", "category.jcr:mixinTypes", "category.jcr:name",
            "category.jcr:path", "category.jcr:score", "category.mode:depth", "category.mode:localName"};
        assertResultsHaveColumns(result, expectedColumns);
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
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, numResults);
        assertResultsHaveColumns(result, columnNames);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllCarsUnderHybrid() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT car.[car:maker], car.[car:model], car.[car:year], car.[car:msrp] FROM [car:Car] AS car WHERE PATH(car) LIKE '%/Hybrid/%'",
                                          Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 3L);
        assertResultsHaveColumns(result, "car:maker", "car:model", "car:year", "car:msrp");
        assertRow(result).has("car:model", "Altima").and("car:msrp", "$18,260").and("car:year", 2008);
        assertRow(result).has("car:model", "Highlander").and("car:msrp", "$34,200").and("car:year", 2008);
        assertRow(result).has("car:model", "Prius").and("car:msrp", "$21,500").and("car:year", 2008);
    }

    @FixFor( "MODE-1824" )
    @Test
    public void shouldBeAbleToExecuteQueryWithTwoColumns() throws RepositoryException {
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        QueryObjectModelFactory factory = queryManager.getQOMFactory();
        Selector car1Selector = factory.selector("car:Car", "car1");
        Selector car2Selector = factory.selector("car:Car", "car2");
        Join join = factory.join(car1Selector,
                                 car2Selector,
                                 QueryObjectModelConstants.JCR_JOIN_TYPE_INNER,
                                 factory.equiJoinCondition("car1", "car:maker", "car2", "car:maker"));
        Column[] columns = new Column[] {factory.column("car1", "car:maker", "maker"),
            factory.column("car2", "car:model", "model")};
        Query query = factory.createQuery(join, null, null, columns);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 21L);
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
            assertThat(result, is(notNullValue()));
            assertResults(query, result, 5);

            // Try again but with LIMIT 1 (via method)...
            query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
            query.setLimit(1L);
            result = query.execute();
            assertThat(result, is(notNullValue()));
            assertResults(query, result, 1);

            // Try again but with LIMIT 1 (via statement)...
            query = session.getWorkspace().getQueryManager().createQuery(sql + " LIMIT 1", Query.JCR_SQL2);
            result = query.execute();
            assertThat(result, is(notNullValue()));
            assertResults(query, result, 1);
        } finally {
            top.remove();
            session.save();
        }
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllCarsUnderHybridWithOrderBy() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT car.[car:maker], car.[car:model], car.[car:year], car.[car:msrp] FROM [car:Car] AS car WHERE PATH(car) LIKE '%/Hybrid/%' ORDER BY [jcr:name]",
                                          Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 3L);
        assertResultsHaveColumns(result, "car:maker", "jcr:name", "car:model", "car:year", "car:msrp");
        assertRow(result, 1).has("car:model", "Altima").and("car:msrp", "$18,260").and("car:year", 2008);
        assertRow(result, 2).has("car:model", "Highlander").and("car:msrp", "$34,200").and("car:year", 2008);
        assertRow(result, 3).has("car:model", "Prius").and("car:msrp", "$21,500").and("car:year", 2008);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryUsingJoinToFindAllCarsUnderHybrid() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT car.[car:maker], car.[car:model], car.[car:year], car.[car:msrp] FROM [car:Car] AS car JOIN [nt:unstructured] AS hybrid ON ISCHILDNODE(car,hybrid) WHERE NAME(hybrid) = 'Hybrid'",
                                          Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 3L);
        assertResultsHaveColumns(result, "car.car:maker", "car.car:model", "car.car:year", "car.car:msrp");
        assertRow(result).has("car:model", "Altima").and("car.car:msrp", "$18,260").and("car.car:year", 2008);
        assertRow(result).has("car:model", "Highlander").and("car.car:msrp", "$34,200").and("car.car:year", 2008);
        assertRow(result).has("car:model", "Prius").and("car.car:msrp", "$21,500").and("car.car:year", 2008);
    }

    @FixFor( "MODE-1825" )
    @Test
    public void shouldBeAbleToExecuteQueryForAllColumns() throws RepositoryException {
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        QueryObjectModelFactory factory = queryManager.getQOMFactory();
        Selector car1Selector = factory.selector("car:Car", "car1");
        Selector car2Selector = factory.selector("car:Car", "car2");
        Join join = factory.join(car1Selector,
                                 car2Selector,
                                 QueryObjectModelConstants.JCR_JOIN_TYPE_INNER,
                                 factory.equiJoinCondition("car1", "car:maker", "car2", "car:maker"));
        Column[] columns = new Column[] {factory.column("car1", null, null)};
        Constraint constraint = factory.comparison(factory.propertyValue("car1", "car:maker"),
                                                   QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO,
                                                   factory.literal(session.getValueFactory().createValue("Toyota")));
        Ordering[] orderings = new Ordering[] {factory.descending(factory.propertyValue("car1", "car:year"))};
        Query query = factory.createQuery(join, constraint, orderings, columns);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 9L);
    }

    @FixFor( "MODE-1833" )
    @Test
    public void shouldBeAbleToQueryAllColumnsOnSimpleType() throws RepositoryException {
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        QueryObjectModelFactory factory = queryManager.getQOMFactory();
        Query query = factory.createQuery(factory.selector("modetest:simpleType", "type1"),
                                          null,
                                          null,
                                          new Column[] {factory.column("type1", null, null)});
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 0L);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllUnstructuredNodes() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [nt:unstructured]", Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 24);
        assertResultsHaveColumns(result, allColumnNames("nt:unstructured"));
    }

    @FixFor( "MODE-1309" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryUsingResidualPropertiesForJoinCriteria() throws RepositoryException {
        String sql = "SELECT x.propA AS pa, y.propB as pb FROM [nt:unstructured] AS x INNER JOIN [nt:unstructured] AS y ON x.propA = y.propB WHERE x.propA = 'value1'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 2);
        assertResultsHaveColumns(result, "pa", "pb");
        RowIterator rows = result.getRows();
        Row row1 = rows.nextRow();
        Set<String> expectedPaths = new HashSet<String>();
        expectedPaths.add("/Other/NodeA[2]");
        expectedPaths.add("/Other/NodeA[3]");
        assertThat(row1.getValue("pa").getString(), is("value1")); // same value from either row
        assertThat(row1.getValue("pb").getString(), is("value1")); // same value from either row
        assertThat(row1.getNode("x").getPath(), is("/Other/NodeA"));
        assertThat(expectedPaths.remove(row1.getNode("y").getPath()), is(true));
        Row row2 = rows.nextRow();
        assertThat(row2.getValue("pa").getString(), is("value1")); // same value from either row
        assertThat(row2.getValue("pb").getString(), is("value1")); // same value from either row
        assertThat(row2.getNode("x").getPath(), is("/Other/NodeA"));
        assertThat(expectedPaths.remove(row2.getNode("y").getPath()), is(true));
    }

    @FixFor( "MODE-1309" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QuerySelectingResidualProperty() throws RepositoryException {
        String sql = "SELECT a.propB FROM [nt:unstructured] AS a WHERE a.propB = 'value1'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 2);
        assertResultsHaveColumns(result, "propB");
        Row row1 = result.getRows().nextRow();
        assertThat(row1.getValue("propB").getString(), is("value1")); // same value from either row
    }

    @FixFor( "MODE-1309" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QuerySelectingResidualPropertyWithAlias() throws RepositoryException {
        String sql = "SELECT a.propB AS foo FROM [nt:unstructured] AS a WHERE a.propB = 'value1'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 2);
        assertResultsHaveColumns(result, "foo");
        Row row1 = result.getRows().nextRow();
        assertThat(row1.getValue("foo").getString(), is("value1")); // same value from either row
    }

    @FixFor( "MODE-1309" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QuerySelectingResidualPropertyWithAliasUsingAliasInConstraint()
        throws RepositoryException {
        String sql = "SELECT a.propB AS foo FROM [nt:unstructured] AS a WHERE a.foo = 'value1'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 2);
        assertResultsHaveColumns(result, "foo");
        Row row1 = result.getRows().nextRow();
        assertThat(row1.getValue("foo").getString(), is("value1")); // same value from either row
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllUnstructuredNodesWithCriteriaOnMultiValuedProperty()
        throws RepositoryException {
        String sql = "SELECT * FROM [nt:unstructured] WHERE something = 'white dog' and something = 'black dog'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, allColumnNames("nt:unstructured"));
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllUnstructuredNodesWithLikeCriteriaOnMultiValuedProperty()
        throws RepositoryException {
        String sql = "SELECT * FROM [nt:unstructured] WHERE something LIKE 'white%' and something LIKE 'black%'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, allColumnNames("nt:unstructured"));
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithChildNodeJoin() throws RepositoryException {
        String sql = "SELECT car.* from [car:Car] as car JOIN [nt:unstructured] as category ON ISCHILDNODE(car,category) WHERE NAME(category) LIKE 'Utility'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 5);
        assertResultsHaveColumns(result, carColumnNames("car"));
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithChildNodeJoinAndColumnsFromBothSidesOfJoin()
        throws RepositoryException {
        String sql = "SELECT car.*, category.[jcr:primaryType] from [car:Car] as car JOIN [nt:unstructured] as category ON ISCHILDNODE(car,category) WHERE NAME(category) LIKE 'Utility'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 5L);
        assertResultsHaveColumns(result, allOf(carColumnNames("car"), new String[] {"category.jcr:primaryType"}));
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithDescendantNodeJoinWithoutCriteria() throws RepositoryException {
        // The 'all' selector will find all nodes, including '/Car' and '/Car/Sports'. Thus,
        // '/Car/Sports/Infiniti G37' will be a descendant of both '/Car' and '/Car/Sports', and thus will appear
        // once joined with '/Car' and once joined with '/Car/Sports'. These two tuples will be similar, but they are
        // actually not repeats (since the columns from 'all' will be different).
        String sql = "SELECT * FROM [car:Car] as car JOIN [nt:unstructured] as all ON ISDESCENDANTNODE(car,all)";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 26L);
        assertResultsHaveColumns(result, allOf(carColumnNames("car"), allColumnNames("all")));
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithDescendantNodeJoinWithDepthCriteria() throws RepositoryException {
        String sql = "SELECT * FROM [car:Car] as car JOIN [nt:unstructured] as category ON ISDESCENDANTNODE(car,category) WHERE DEPTH(category) = 2";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13L);
        assertResultsHaveColumns(result, allOf(carColumnNames("car"), allColumnNames("category")));
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithDescendantNodeJoin() throws RepositoryException {
        String sql = "SELECT car.* from [car:Car] as car JOIN [nt:unstructured] as category ON ISDESCENDANTNODE(car,category) WHERE NAME(category) LIKE 'Utility'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 5);
        assertResultsHaveColumns(result, carColumnNames("car"));
    }

    @FixFor( "MODE-1809" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2UnionOfQueriesWithJoins() throws RepositoryException {
        String sql1 = "SELECT car.* from [car:Car] as car JOIN [nt:unstructured] as category ON ISDESCENDANTNODE(car,category) WHERE NAME(category) LIKE 'Utility'";
        String sql2 = "SELECT car.* from [car:Car] as car JOIN [nt:unstructured] as category ON ISDESCENDANTNODE(car,category) WHERE NAME(category) LIKE 'Sports'";
        String sql = sql1 + " UNION " + sql2;
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 7);
        assertResultsHaveColumns(result, carColumnNames("car"));
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithDescendantNodeJoinAndColumnsFromBothSidesOfJoin()
        throws RepositoryException {
        String sql = "SELECT car.*, category.[jcr:primaryType] from [car:Car] as car JOIN [nt:unstructured] as category ON ISDESCENDANTNODE(car,category) WHERE NAME(category) LIKE 'Utility'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 5L);
        assertResultsHaveColumns(result, allOf(carColumnNames("car"), new String[] {"category.jcr:primaryType"}));
    }

    @FixFor( "MODE-829" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithDescendantNodeJoinUsingNtBase() throws RepositoryException {
        String sql = "SELECT * FROM [nt:base] AS category JOIN [nt:base] AS cars ON ISDESCENDANTNODE(cars,category) WHERE ISCHILDNODE(category,'/Cars')";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13L);
        String[] expectedColumns = {"category.jcr:primaryType", "cars.jcr:primaryType", "cars.jcr:mixinTypes",
            "category.jcr:mixinTypes", "cars.jcr:name", "cars.jcr:path", "cars.jcr:score", "cars.mode:depth",
            "cars.mode:localName", "category.jcr:name", "category.jcr:path", "category.jcr:score", "category.mode:depth",
            "category.mode:localName"};

        assertResultsHaveColumns(result, expectedColumns);
    }

    @FixFor( "MODE-829" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithDescendantNodeJoinUsingNtBaseAndNameConstraint()
        throws RepositoryException {
        String sql = "SELECT * FROM [nt:base] AS category JOIN [nt:base] AS cars ON ISDESCENDANTNODE(cars,category) WHERE ISCHILDNODE(category,'/Cars') AND NAME(cars) LIKE 'Toyota%'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 3L);
        String[] expectedColumns = {"category.jcr:primaryType", "cars.jcr:primaryType", "cars.jcr:mixinTypes",
            "category.jcr:mixinTypes", "cars.jcr:name", "cars.jcr:path", "cars.jcr:score", "cars.mode:depth",
            "cars.mode:localName", "category.jcr:name", "category.jcr:path", "category.jcr:score", "category.mode:depth",
            "category.mode:localName"};
        assertResultsHaveColumns(result, expectedColumns);
    }

    @FixFor( "MODE-829" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithDescendantNodeJoinUsingNonExistantNameColumnOnTypeWithResidualProperties()
        throws RepositoryException {
        String sql = "SELECT * FROM [nt:unstructured] AS category JOIN [nt:unstructured] AS cars ON ISDESCENDANTNODE(cars,category) WHERE ISCHILDNODE(category,'/Cars') AND cars.name = 'd2'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 0L); // no nodes have a 'name' property (strictly speaking)
        String[] expectedColumns = {"category.jcr:primaryType", "cars.jcr:primaryType", "cars.jcr:mixinTypes",
            "category.jcr:mixinTypes", "cars.jcr:name", "cars.jcr:path", "cars.jcr:score", "cars.mode:depth",
            "cars.mode:localName", "category.jcr:name", "category.jcr:path", "category.jcr:score", "category.mode:depth",
            "category.mode:localName"};
        assertResultsHaveColumns(result, expectedColumns);
    }

    @FixFor( "MODE-829" )
    @Test
    public void shouldReturnNoResultsForJcrSql2QueryWithDescendantNodeJoinUsingNonExistantNameColumnOnTypeWithNoResidualProperties()
        throws RepositoryException {
        String sql = "SELECT * FROM [nt:base] AS category JOIN [nt:base] AS cars ON ISDESCENDANTNODE(cars,category) WHERE ISCHILDNODE(category,'/Cars') AND cars.name = 'd2'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 0); // no results, because it is a join on an invalid column
        String[] expectedColumns = {"category.jcr:primaryType", "cars.jcr:primaryType", "cars.jcr:mixinTypes",
            "category.jcr:mixinTypes", "cars.jcr:name", "cars.jcr:path", "cars.jcr:score", "cars.mode:depth",
            "cars.mode:localName", "category.jcr:name", "category.jcr:path", "category.jcr:score", "category.mode:depth",
            "category.mode:localName"};
        assertResultsHaveColumns(result, expectedColumns);
    }

    @FixFor( "MODE-869" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithSubqueryInCriteria() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT * FROM [car:Car] WHERE [car:maker] IN (SELECT [car:maker] FROM [car:Car] WHERE [car:year] >= 2008)",
                                          Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13); // the 13 types of cars made by makers that made cars in 2008
        assertResultsHaveColumns(result, carColumnNames("car:Car"));
    }

    @FixFor( "MODE-869" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithSubqueryInCriteria2() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT * FROM [car:Car] WHERE [car:maker] IN (SELECT [car:maker] FROM [car:Car] WHERE PATH() LIKE '%/Hybrid/%')",
                                          Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 4); // the 4 types of cars made by makers that make hybrids
        assertResultsHaveColumns(result, carColumnNames("car:Car"));
    }

    @FixFor( "MODE-1873" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithSubqueryInCriteriaWhenSubquerySelectsPseudoColumn()
        throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT [jcr:path] FROM [nt:unstructured] WHERE [pathProperty] IN (SELECT [jcr:path] FROM [nt:unstructured] WHERE PATH() LIKE '/Other/%')",
                                          Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1); // the 4 types of cars made by makers that make hybrids
        assertResultsHaveColumns(result, new String[] {"jcr:path"});
    }

    @FixFor( "MODE-909" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithOrderBy() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT [jcr:primaryType] from [nt:base] ORDER BY [jcr:primaryType]", Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, totalNodeCount);
        assertResultsHaveColumns(result, new String[] {"jcr:primaryType"});
        RowIterator iter = result.getRows();
        String primaryType = "";
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            String nextPrimaryType = row.getValues()[0].getString();
            assertThat(nextPrimaryType.compareTo(primaryType) >= 0, is(true));
            primaryType = nextPrimaryType;
        }
    }

    @FixFor( {"MODE-1277", "MODE-1485"} )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithFullOuterJoin() throws RepositoryException {
        String sql = "SELECT car.[jcr:name], category.[jcr:primaryType] from [car:Car] as car FULL OUTER JOIN [nt:unstructured] as category ON ISCHILDNODE(car,category) WHERE NAME(car) LIKE 'Toyota*'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 72L);
        assertResultsHaveColumns(result, new String[] {"car.jcr:name", "category.jcr:primaryType"});
    }

    @FixFor( "MODE-1750" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithLeftOuterJoinOnNullCondition() throws RepositoryException {
        // given
        final String sql = "SELECT car1.[jcr:name] from [car:Car] as car1 LEFT OUTER JOIN [car:Car] as car2 ON car1.[car:alternateModesl] = car2.[UUID]";
        final Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));

        // when
        final QueryResult result = query.execute();

        // then
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13L);
        assertResultsHaveColumns(result, new String[] {"car1.jcr:name"});
    }

    @FixFor( "MODE-1750" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithRightOuterJoinOnNullCondition() throws RepositoryException {
        // given
        final String sql = "SELECT car2.[jcr:name] from [car:Car] as car1 RIGHT OUTER JOIN [car:Car] as car2 ON car1.[car:alternateModesl] = car2.[UUID]";
        final Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));

        // when
        final QueryResult result = query.execute();

        // then
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13L);
        assertResultsHaveColumns(result, new String[] {"car2.jcr:name"});
    }

    @FixFor( "MODE-2450" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithJoinAndNoCriteria() throws RepositoryException {
        String sql = "SELECT category.[jcr:path], cars.[jcr:path] FROM [nt:unstructured] AS category JOIN [car:Car] AS cars ON ISCHILDNODE(cars,category)";
        final Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));

        final QueryResult result = query.execute();

        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13L);
        assertResultsHaveColumns(result, new String[] {"category.jcr:path", "cars.jcr:path"});
    }

    @FixFor( "MODE-2450" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithJoinAndDepthCriteria() throws RepositoryException {
        String sql = "SELECT category.[jcr:path], cars.[jcr:path] FROM [nt:unstructured] AS category JOIN [car:Car] AS cars ON ISCHILDNODE(cars,category) WHERE DEPTH(category) = 2";
        final Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));

        final QueryResult result = query.execute();

        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13L);
        assertResultsHaveColumns(result, new String[] {"category.jcr:path", "cars.jcr:path"});
    }

    @FixFor( "MODE-2450" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithLeftOuterJoinAndDepthCriteria() throws RepositoryException {
        String sql = "SELECT category.[jcr:path], cars.[jcr:path] FROM [nt:unstructured] AS category LEFT OUTER JOIN [car:Car] AS cars ON ISCHILDNODE(cars,category) WHERE DEPTH(category) = 2";
        final Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));

        final QueryResult result = query.execute();

        assertThat(result, is(notNullValue()));
        assertResults(query, result, 17L);
        assertResultsHaveColumns(result, new String[] {"category.jcr:path", "cars.jcr:path"});
    }

    @FixFor( "MODE-2450" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithLeftOuterJoinWithFullTextSearch() throws RepositoryException {
        String sql = "SELECT category.[jcr:path], cars.[jcr:path] FROM [nt:unstructured] AS category LEFT OUTER JOIN [car:Car] AS cars ON ISCHILDNODE(cars,category) WHERE contains(category.*, 'Utility') AND contains(cars.*, 'Toyota') ";
        final Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));

        final QueryResult result = query.execute();

        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1L);
        assertResultsHaveColumns(result, new String[] {"category.jcr:path", "cars.jcr:path"});
    }

    @FixFor( "MODE-2450" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithJoinWithFullTextSearch() throws RepositoryException {
        String sql = "SELECT category.[jcr:path], cars.[jcr:path] FROM [nt:unstructured] AS category JOIN [car:Car] AS cars ON ISCHILDNODE(cars,category) WHERE contains(category.*, 'Utility') AND contains(cars.*, 'Toyota') ";
        final Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));

        final QueryResult result = query.execute();

        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1L);
        assertResultsHaveColumns(result, new String[] {"category.jcr:path", "cars.jcr:path"});
    }

    @FixFor( "MODE-2450" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithUnionAndFullTextSearch() throws RepositoryException {
        String sql = "SELECT category.[jcr:path] AS p FROM [nt:unstructured] AS category WHERE contains(category.*, 'Utility')"
                     + "UNION "
                     + "SELECT category.[jcr:path] AS p FROM [nt:unstructured] AS category JOIN [car:Car] AS cars ON ISCHILDNODE(cars,category) WHERE contains(cars.*, 'Toyota') ";
        final Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));

        final QueryResult result = query.execute();

        assertThat(result, is(notNullValue()));
        assertResults(query, result, 2L);
        assertResultsHaveColumns(result, new String[] {"p"});
    }

    @FixFor( "MODE-1679" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindReferenceableNodes() throws RepositoryException {
        String sql = "SELECT [jcr:uuid] FROM [mix:referenceable]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 4L);
        assertResultsHaveColumns(result, new String[] {"jcr:uuid"});
    }

    @FixFor( "MODE-1679" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindJcrUuidOfNodeWithPathCriteria() throws RepositoryException {
        String sql = "SELECT [jcr:uuid] FROM [mix:referenceable] AS node WHERE PATH(node) = '/Other/NodeA[2]'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1L);
        assertResultsHaveColumns(result, new String[] {"jcr:uuid"});
    }

    @FixFor( "MODE-1679" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindNodesOfParticularPrimaryType() throws RepositoryException {
        String sql = "SELECT [notion:singleReference], [notion:multipleReferences] FROM [notion:typed]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1L);
        assertResultsHaveColumns(result, "notion:singleReference", "notion:multipleReferences");
    }

    @FixFor( "MODE-1679" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithSingleReferenceConstraintUsingSubquery() throws RepositoryException {
        String sql = "SELECT [notion:singleReference] FROM [notion:typed] WHERE [notion:singleReference] IN ( SELECT [jcr:uuid] FROM [mix:referenceable] AS node WHERE PATH(node) = '/Other/NodeA')";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1L);
        assertResultsHaveColumns(result, "notion:singleReference");
    }

    @FixFor( "MODE-1679" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithSingleReferenceConstraintUsingStringIdentifier()
        throws RepositoryException {
        String id = session.getNode("/Other/NodeA").getIdentifier();
        assertThat(id, is(notNullValue()));
        String sql = "SELECT [notion:singleReference] FROM [notion:typed] AS typed WHERE [notion:singleReference] = '" + id + "'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1L);
        assertResultsHaveColumns(result, "notion:singleReference");
    }

    @FixFor( "MODE-1679" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithSingleReferenceConstraintUsingJoin() throws RepositoryException {
        String sql = "SELECT typed.* FROM [notion:typed] AS typed JOIN [mix:referenceable] AS target ON typed.[notion:singleReference] = target.[jcr:uuid] WHERE PATH(target) = '/Other/NodeA'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1L);
        assertResultsHaveColumns(result, typedColumnNames("typed"));
    }

    @FixFor( "MODE-1679" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithMultipleReferenceConstraintUsingSubquery()
        throws RepositoryException {
        String sql = "SELECT [notion:multipleReferences] FROM [notion:typed] WHERE [notion:multipleReferences] IN ( SELECT [jcr:uuid] FROM [mix:referenceable] AS node WHERE PATH(node) = '/Other/NodeA[2]')";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1L);
        assertResultsHaveColumns(result, "notion:multipleReferences");
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
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1L);
        assertResultsHaveColumns(result, "notion:multipleReferences");
    }

    @FixFor( "MODE-1679" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithMultipleReferenceConstraintUsingJoin() throws RepositoryException {
        String sql = "SELECT typed.* FROM [notion:typed] AS typed JOIN [mix:referenceable] AS target ON typed.[notion:multipleReferences] = target.[jcr:uuid] WHERE PATH(target) = '/Other/NodeA[2]'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1L);
        assertResultsHaveColumns(result, typedColumnNames("typed"));
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedPathInSelectOfJcrSql2Query() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("select [jcr:primaryType], [jcr:path] FROM [nt:base]", Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, totalNodeCount);
        assertResultsHaveColumns(result, new String[] {"jcr:primaryType", "jcr:path"});
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedPathInSelectAndCriteriaOfJcrSql2Query() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("select [jcr:primaryType], [jcr:path] FROM [nt:base] WHERE [jcr:path] LIKE '/Cars/%'",
                                          Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 17);
        assertResultsHaveColumns(result, new String[] {"jcr:primaryType", "jcr:path"});
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedPathInSelectAndUnqualifiedNameInCriteriaOfJcrSql2Query()
        throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("select [jcr:primaryType], [jcr:path] FROM [nt:base] WHERE [jcr:name] LIKE '%3%'",
                                          Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 4);
        assertResultsHaveColumns(result, new String[] {"jcr:primaryType", "jcr:path"});
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedPathInSelectAndUnqualifiedLocalNameInCriteriaOfJcrSql2Query()
        throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("select [jcr:primaryType], [jcr:path] FROM [nt:base] WHERE [mode:localName] LIKE '%3%'",
                                          Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 4);
        assertResultsHaveColumns(result, new String[] {"jcr:primaryType", "jcr:path"});
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithJcrPathInJoinCriteriaOfJcrSql2Query() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("select base.[jcr:primaryType], base.[jcr:path], car.[car:year] "
                                          + "FROM [nt:base] AS base JOIN [car:Car] AS car ON car.[jcr:path] = base.[jcr:path]",
                                          Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13);
        assertResultsHaveColumns(result, new String[] {"base.jcr:primaryType", "base.jcr:path", "car.car:year"});
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    @FixFor( "MODE-934" )
    @Test
    public void shouldNotIncludePseudoColumnsInSelectStarOfJcrSql2Query() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("select * FROM [nt:base]", Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, totalNodeCount);
        assertResultsHaveColumns(result, allColumnNames("nt:base"));
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    @FixFor( "MODE-1738" )
    @Test
    public void shouldSupportJoinWithOrderByOnPseudoColumn() throws RepositoryException {
        String sql = "SELECT category.[jcr:path], cars.[car:maker], cars.[car:lengthInInches] FROM [nt:unstructured] AS category JOIN [car:Car] AS cars ON ISDESCENDANTNODE(cars,category) WHERE ISCHILDNODE(category,'/Cars') ORDER BY cars.[mode:localName]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13L);
        String[] expectedColumns = {"category.jcr:path", "cars.car:maker", "cars.mode:localName", "cars.car:lengthInInches"};
        assertResultsHaveColumns(result, expectedColumns);
    }

    @FixFor( "MODE-1738" )
    @Test
    public void shouldSupportJoinWithOrderByOnActualColumn() throws RepositoryException {
        String sql = "SELECT category.[jcr:path], cars.[car:maker], cars.[car:lengthInInches] FROM [nt:unstructured] AS category JOIN [car:Car] AS cars ON ISDESCENDANTNODE(cars,category) WHERE ISCHILDNODE(category,'/Cars') ORDER BY cars.[car:maker]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13L);
        String[] expectedColumns = {"category.jcr:path", "cars.car:maker", "cars.car:lengthInInches"};
        assertResultsHaveColumns(result, expectedColumns);
    }

    @FixFor( "MODE-1737" )
    @Test
    public void shouldSupportSelectDistinct() throws RepositoryException {
        String sql = "SELECT DISTINCT cars.[car:maker], cars.[car:lengthInInches] FROM [car:Car] AS cars";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13L);
        String[] expectedColumns = {"car:maker", "car:lengthInInches"};
        assertResultsHaveColumns(result, expectedColumns);
    }

    @FixFor( "MODE-1737" )
    @Test
    public void shouldSupportJoinWithSelectDistinct() throws RepositoryException {
        String sql = "SELECT DISTINCT category.[jcr:path], cars.[car:maker], cars.[car:lengthInInches] FROM [nt:unstructured] AS category JOIN [car:Car] AS cars ON ISDESCENDANTNODE(cars,category) WHERE ISCHILDNODE(category,'/Cars')";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13L);
        String[] expectedColumns = {"category.jcr:path", "cars.car:maker", "cars.car:lengthInInches"};
        assertResultsHaveColumns(result, expectedColumns);
    }

    @FixFor( "MODE-1020" )
    @Test
    public void shouldFindAllPublishAreas() throws Exception {
        String sql = "SELECT [jcr:path], [jcr:title], [jcr:description] FROM [mode:publishArea]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 0L); // currently no records
        assertResultsHaveColumns(result, new String[] {"jcr:path", "jcr:title", "jcr:description"});
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    @FixFor( "MODE-1052" )
    @Test
    public void shouldProperlyUseNotWithPathConstraints() throws Exception {
        // Find all nodes that are children of '/Cars' ... there should be 4 ...
        String sql = "SELECT [jcr:path] FROM [nt:base] WHERE ISCHILDNODE([nt:base],'/Cars') ORDER BY [jcr:path]";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 4L);
        assertResultsHaveColumns(result, new String[] {"jcr:path"});
        assertResultsHaveRows(result, "jcr:path", "/Cars/Hybrid", "/Cars/Luxury", "/Cars/Sports", "/Cars/Utility");

        // Find all nodes ... there should be 24 ...
        sql = "SELECT [jcr:path] FROM [nt:base] ORDER BY [jcr:path]";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, totalNodeCount);

        // Find all nodes that are NOT children of '/Cars' (and not under '/jcr:system') ...
        sql = "SELECT [jcr:path] FROM [nt:base] WHERE NOT(ISCHILDNODE([nt:base],'/Cars')) AND NOT(ISDESCENDANTNODE([nt:base],'/jcr:system')) ORDER BY [jcr:path]";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 22L);
        assertResultsHaveColumns(result, new String[] {"jcr:path"});
        assertResultsHaveRows(result,
                              "jcr:path",
                              "/",
                              "/Cars",
                              "/Cars/Hybrid/Nissan Altima",
                              "/Cars/Hybrid/Toyota Highlander",
                              "/Cars/Hybrid/Toyota Prius",
                              "/Cars/Luxury/Bentley Continental",
                              "/Cars/Luxury/Cadillac DTS",
                              "/Cars/Luxury/Lexus IS350",
                              "/Cars/Sports/Aston Martin DB9",
                              "/Cars/Sports/Infiniti G37",
                              "/Cars/Utility/Ford F-150",
                              "/Cars/Utility/Hummer H3",
                              "/Cars/Utility/Land Rover LR2",
                              "/Cars/Utility/Land Rover LR3",
                              "/Cars/Utility/Toyota Land Cruiser",
                              "/NodeB",
                              "/Other",
                              "/Other/NodeA",
                              "/Other/NodeA[2]",
                              "/Other/NodeA[3]",
                              "/Other/NodeC",
                              "/jcr:system");
    }

    @FixFor( "MODE-1110" )
    @Test
    public void shouldExecuteQueryWithThreeInnerJoinsAndCriteriaOnDifferentSelectors() throws Exception {
        String sql = "SELECT * from [nt:base] as myfirstnodetypes INNER JOIN [nt:base] as mysecondnodetypes "
                     + "        ON ISDESCENDANTNODE(myfirstnodetypes, mysecondnodetypes) "
                     + "INNER JOIN [nt:base] as mythirdnodetypes "
                     + "        ON ISDESCENDANTNODE (mysecondnodetypes, mythirdnodetypes) "
                     + " WHERE ISDESCENDANTNODE( mythirdnodetypes, '/') OR " + "myfirstnodetypes.[jcr:primaryType] IS NOT NULL";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, new String[] {"myfirstnodetypes.jcr:path", "mythirdnodetypes.mode:depth",
            "mysecondnodetypes.mode:depth", "mythirdnodetypes.jcr:path", "mysecondnodetypes.jcr:path",
            "mythirdnodetypes.jcr:mixinTypes", "mythirdnodetypes.jcr:score", "myfirstnodetypes.jcr:score",
            "mythirdnodetypes.jcr:name", "mysecondnodetypes.jcr:mixinTypes", "mysecondnodetypes.jcr:score",
            "mythirdnodetypes.mode:localName", "myfirstnodetypes.jcr:primaryType", "mysecondnodetypes.jcr:primaryType",
            "mysecondnodetypes.jcr:name", "myfirstnodetypes.jcr:name", "mythirdnodetypes.jcr:primaryType",
            "myfirstnodetypes.jcr:mixinTypes", "myfirstnodetypes.mode:localName", "myfirstnodetypes.mode:depth",
            "mysecondnodetypes.mode:localName"});
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    @FixFor( "MODE-1418" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithFullTextSearchWithSelectorAndOneProperty()
        throws RepositoryException {
        String sql = "select [jcr:path] from [nt:unstructured] as n where contains(n.something, 'cat wearing')";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1L);
    }

    @FixFor( "MODE-1418" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithFullTextSearchWithSelectorAndAllProperties()
        throws RepositoryException {
        String sql = "select [jcr:path] from [nt:unstructured] as n where contains(n.*, 'cat wearing')";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1L);
    }

    @FixFor( "MODE-1418" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithFullTextSearchWithNoSelectorAndOneProperty()
        throws RepositoryException {
        String sql = "select [jcr:path] from [nt:unstructured] as n where contains(something,'cat wearing')";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1L);
        assertResultsHaveRows(result, "jcr:path", "/Other/NodeA[2]");
    }

    @FixFor( "MODE-1829" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithFullTextSearchUsingLeadingWildcard() throws RepositoryException {
        String sql = "select [jcr:path] from [nt:unstructured] as n where contains(n.something, '*earing')";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        sql = "select [jcr:path] from [nt:unstructured] as n where contains(n.something, '*earing*')";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1L);
    }

    @FixFor( "MODE-1829" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithFullTextSearchUsingTrailingWildcard() throws RepositoryException {
        String sql = "select [jcr:path] from [nt:unstructured] as n where contains(n.something, 'wea*')";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1L);
    }

    @Test
    @FixFor( "MODE-1547" )
    public void shouldBeAbleToExecuteFullTextSearchQueriesOnPropertiesWhichIncludeStopWords() throws Exception {
        String propertyText = "the quick Brown fox jumps over to the dog in at the gate";
        Node ftsNode = session.getRootNode().addNode("FTSNode").setProperty("FTSProp", propertyText).getParent();
        try {
            session.save();

            executeJcrSQL2AndExpectOneResult("select [jcr:path] from [nt:unstructured] as n where contains([nt:unstructured].*,'"
                                             + propertyText + "')");

            executeJcrSQL2AndExpectOneResult("select [jcr:path] from [nt:unstructured] as n where contains(FTSProp,'"
                                             + propertyText + "')");
            executeJcrSQL2AndExpectOneResult("select [jcr:path] from [nt:unstructured] as n where contains(n.*,'" + propertyText
                                             + "')");

            executeJcrSQL2AndExpectOneResult("select [jcr:path] from [nt:unstructured] as n where contains(FTSProp,'"
                                             + propertyText.toUpperCase() + "')");
            executeJcrSQL2AndExpectOneResult("select [jcr:path] from [nt:unstructured] as n where contains(n.*,'"
                                             + propertyText.toUpperCase() + "')");

            executeJcrSQL2AndExpectOneResult("select [jcr:path] from [nt:unstructured] as n where contains(FTSProp,'the quick Dog')");
            executeJcrSQL2AndExpectOneResult("select [jcr:path] from [nt:unstructured] as n where contains(n.*,'the quick Dog')");

            executeJcrSQL2AndExpectOneResult("select [jcr:path] from [nt:unstructured] as n where contains(FTSProp,'the quick jumps over gate')");
            executeJcrSQL2AndExpectOneResult("select [jcr:path] from [nt:unstructured] as n where contains(n.*,'the quick jumps over gate')");

            executeJcrSQL2AndExpectOneResult("select [jcr:path] from [nt:unstructured] as n where contains(FTSProp,'the gate')");
            executeJcrSQL2AndExpectOneResult("select [jcr:path] from [nt:unstructured] as n where contains(n.*,'the gate')");
        } finally {
            // Try to remove the node (which messes up the expected results from subsequent tests) ...
            ftsNode.remove();
            session.save();
        }
    }

    private void executeJcrSQL2AndExpectOneResult( String sql ) throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, JcrRepository.QueryLanguage.JCR_SQL2);
        QueryResult result = query.execute();
        assertResults(query, result, 1);
    }

    @FixFor( "MODE-1840" )
    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryWithBindVariableInsideContains() throws RepositoryException {
        String sql = "select [jcr:path] from [nt:unstructured] as n where contains(n.something, $expression)";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        query.bindValue("expression", session.getValueFactory().createValue("cat wearing"));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1L);
        assertResultsHaveColumns(result, new String[] {"jcr:path"});
        assertResultsHaveRows(result, "jcr:path", "/Other/NodeA[2]");
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

    // ----------------------------------------------------------------------------------------------------------------
    // Full-text Search Queries
    // ----------------------------------------------------------------------------------------------------------------

    @FixFor( "MODE-1418" )
    @Test
    public void shouldBeAbleToCreateAndExecuteFullTextSearchQueryOfPhrase() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("cat wearing", JcrRepository.QueryLanguage.SEARCH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        // print = true;
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, searchColumnNames());
    }

    @FixFor( "MODE-905" )
    @Test
    public void shouldBeAbleToCreateAndExecuteFullTextSearchQuery() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("land", JcrRepository.QueryLanguage.SEARCH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        // print = true;
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 3);
        assertResultsHaveColumns(result, searchColumnNames());
    }

    @FixFor( "MODE-905" )
    @Test
    public void shouldBeAbleToCreateAndExecuteFullTextSearchQueryWithName() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("highlander", JcrRepository.QueryLanguage.SEARCH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, searchColumnNames());
    }

    // ----------------------------------------------------------------------------------------------------------------
    // JCR-SQL Queries
    // ----------------------------------------------------------------------------------------------------------------

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryWithOrderByClause() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT car:model FROM car:Car WHERE car:model IS NOT NULL ORDER BY car:model ASC",
                                          Query.SQL);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13);
        assertResultsHaveColumns(result, "jcr:path", "jcr:score", "car:model");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryWithOrderByPathClause() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT car:model FROM car:Car WHERE car:model IS NOT NULL ORDER BY PATH() ASC",
                                          Query.SQL);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13);
        assertResultsHaveColumns(result, "jcr:path", "jcr:score", "car:model");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryWithPathCriteriaAndOrderByClause() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT car:model FROM car:Car WHERE jcr:path LIKE '/Cars/%' ORDER BY car:model ASC",
                                          Query.SQL);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13);
        assertResultsHaveColumns(result, "jcr:path", "jcr:score", "car:model");
    }

    /**
     * Tests that the child nodes (but no grandchild nodes) are returned.
     * 
     * @throws RepositoryException
     */
    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryWithChildAxisCriteria() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT * FROM nt:base WHERE jcr:path LIKE '/Cars/%' AND NOT jcr:path LIKE '/Cars/%/%'",
                                          Query.SQL);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 4); // the 4 types of cars
        assertResultsHaveColumns(result, allColumnNames());
    }

    /**
     * Tests that the child nodes (but no grandchild nodes) are returned.
     *
     * @throws RepositoryException
     */
    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryWithContainsCriteria() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT * FROM nt:base WHERE jcr:path LIKE '/Cars/%' AND NOT jcr:path LIKE '/Cars/%/%'",
                                          Query.SQL);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 4); // the 4 types of cars
        assertResultsHaveColumns(result, allColumnNames());
    }

    @Test
    @FixFor( "MODE-791" )
    @SuppressWarnings( "deprecation" )
    public void shouldReturnNodesWithPropertyConstrainedByTimestamp() throws Exception {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("SELECT car:model, car:maker FROM car:Car " + "WHERE jcr:path LIKE '/Cars/%' "
                                          + "AND (car:msrp LIKE '$3%' OR car:msrp LIKE '$2') "
                                          + "AND (car:year LIKE '2008' OR car:year LIKE '2009') " + "AND car:valueRating > '1' "
                                          + "AND jcr:created > TIMESTAMP '1974-07-10T00:00:00.000-05:00' "
                                          + "AND jcr:created < TIMESTAMP '3074-07-10T00:00:00.000-05:00'",
                                          Query.SQL);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 5);

        for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
            assertThat(iter.nextNode().hasProperty("car:model"), is(true));
        }
    }

    @SuppressWarnings( "deprecation" )
    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedPathInSelectOfJcrSqlQuery() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("select jcr:primaryType, jcr:path FROM nt:base", Query.SQL);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, totalNodeCount);
        assertResultsHaveColumns(result, new String[] {"jcr:primaryType", "jcr:path", "jcr:score"});
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    @SuppressWarnings( "deprecation" )
    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedPathInSelectAndCriteriaOfJcrSqlQuery() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("select jcr:primaryType, jcr:path FROM nt:base WHERE jcr:path LIKE '/Cars/%'",
                                          Query.SQL);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 17);
        assertResultsHaveColumns(result, new String[] {"jcr:primaryType", "jcr:path", "jcr:score"});
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    @SuppressWarnings( "deprecation" )
    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedPathInSelectAndUnqualifiedNameInCriteriaOfJcrSqlQuery() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("select jcr:primaryType, jcr:path FROM nt:base WHERE jcr:name LIKE '%3%'", Query.SQL);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 4);
        assertResultsHaveColumns(result, new String[] {"jcr:primaryType", "jcr:path", "jcr:score"});
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    @SuppressWarnings( "deprecation" )
    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithUnqualifiedPathInSelectAndUnqualifiedLocalNameInCriteriaOfJcrSqlQuery()
        throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("select jcr:primaryType, jcr:path FROM nt:base WHERE mode:localName LIKE '%3%'",
                                          Query.SQL);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 4);
        assertResultsHaveColumns(result, new String[] {"jcr:primaryType", "jcr:path", "jcr:score"});
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    @SuppressWarnings( "deprecation" )
    @FixFor( "MODE-934" )
    @Test
    public void shouldParseQueryWithJcrPathInJoinCriteriaOfJcrSqlQuery() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("select nt:base.jcr:primaryType, nt:base.jcr:path, car:Car.car:year "
                                          + "FROM nt:base, car:Car WHERE car:Car.jcr:path = nt:base.jcr:path",
                                          Query.SQL);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13);
        assertResultsHaveColumns(result, new String[] {"nt:base.jcr:primaryType", "nt:base.jcr:path", "car:Car.car:year",
            "jcr:path", "jcr:score"});
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    @SuppressWarnings( "deprecation" )
    @FixFor( "MODE-934" )
    @Test
    public void shouldNotIncludePseudoColumnsInSelectStarOfJcrSqlQuery() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("select * FROM nt:base", Query.SQL);
        assertThat(query, is(notNullValue()));
        // print = true;
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, totalNodeCount);
        assertResultsHaveColumns(result, allColumnNames());
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // XPath Queries
    // ----------------------------------------------------------------------------------------------------------------

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToCreateXPathQuery() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//element(*,car:Car)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        assertResults(query, query.execute(), 13);

        query = session.getWorkspace().getQueryManager().createQuery("//element(*,nt:unstructured)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        assertResults(query, query.execute(), 24);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllNodes() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//element(*,nt:base)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, totalNodeCount);
        assertResultsHaveColumns(result, allColumnNames());
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllNodesOrderingByPath() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("//element(*,nt:base) order by @jcr:path", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, totalNodeCount);
        assertResultsHaveColumns(result, allColumnNames());
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllNodesOrderingByAttribute() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("//element(*,car:Car) order by @car:maker", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        // print = true;
        assertResults(query, result, 13);
        assertResultsHaveColumns(result, "car:maker", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllUnstructuredNodes() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//element(*,nt:unstructured)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 24);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, allColumnNames());
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllUnstructuredNodesOrderedByPropertyValue() throws RepositoryException {
        QueryManager manager = session.getWorkspace().getQueryManager();
        Query query = manager.createQuery("//element(*,nt:unstructured) order by @jcr:primaryType", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 24);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");

        query = manager.createQuery("//element(*,car:Car) order by @car:year", Query.XPATH);
        assertThat(query, is(notNullValue()));
        result = query.execute();
        assertResults(query, result, 13);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "car:year", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodesUnderNode() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery(" /jcr:root/Cars/Hybrid/*", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 3);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodesUnderNodeAndWithProperty() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery(" /jcr:root/Cars/Hybrid/*[@car:year]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 3);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodesUnderNodeAndWithPropertyOrderedByProperty() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery(" /jcr:root/Cars/Hybrid/*[@car:year] order by @car:year ascending", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 3);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "car:year", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodesUnderPath() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery(" /jcr:root/Cars//*", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        // print = true;
        assertResults(query, result, 17);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodesWithAllSnsIndexesUnderPath() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery(" /jcr:root//NodeA", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        // print = true;
        assertResults(query, result, 3);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodesUnderPathAndWithProperty() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery(" /jcr:root/Cars//*[@car:year]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 13);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodesUnderPathAndWithPropertyOrderedByProperty() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery(" /jcr:root/Cars//*[@car:year] order by @car:year ascending", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 13);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, "car:year", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllUnstructuredNodesOrderedByScore() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("//element(*,nt:unstructured) order by jcr:score()", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 24);
        assertThat(result, is(notNullValue()));
        assertResultsHaveColumns(result, allColumnNames());
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindSameNameSiblingsByIndex() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/Other/NodeA", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 3);
        assertThat(result, is(notNullValue()));
        // assertThat(result.getNodes().nextNode().getIndex(), is(1));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");

        query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/Other/NodeA[2]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        result = query.execute();
        assertResults(query, result, 1);
        assertThat(result, is(notNullValue()));
        assertThat(result.getNodes().nextNode().getIndex(), is(2));
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAllCarNodes() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//element(*,car:Car)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 13);
        assertResultsHaveColumns(result,
                                 "jcr:primaryType",
                                 "jcr:mixinTypes",
                                 "jcr:path",
                                 "jcr:score",
                                 "jcr:created",
                                 "jcr:createdBy",
                                 "jcr:name",
                                 "mode:localName",
                                 "mode:depth",
                                 "car:mpgCity",
                                 "car:userRating",
                                 "car:mpgHighway",
                                 "car:engine",
                                 "car:model",
                                 "car:year",
                                 "car:maker",
                                 "car:lengthInInches",
                                 "car:valueRating",
                                 "car:wheelbaseInInches",
                                 "car:msrp",
                                 "car:alternateModels");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindRootNode() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindChildOfRootNode() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/Cars", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindChildOfRootNodeWithTypeCriteria() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/Cars[@jcr:primaryType]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodeWithPathAndAttrbuteCriteria() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("/jcr:root/Cars/Sports/Infiniti_x0020_G37[@car:year='2008']", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodeWithAttrbuteCriteria() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//Infiniti_x0020_G37[@car:year='2008']", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        // print = true;
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodeWithPathUnderRootAndAttrbuteCriteria() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("/jcr:root/NodeB[@myUrl='http://www.acme.com/foo/bar']", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindAnywhereNodeWithNameAndAttrbuteCriteriaMatchingUrl()
        throws RepositoryException {
        // See MODE-686
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("//NodeB[@myUrl='http://www.acme.com/foo/bar']", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryToFindNodeWithNameMatch() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("//NodeB", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    /*
     * Adding this test case since its primarily a test of integration between the RNTM and the query engine
     */
    @Test( expected = InvalidNodeTypeDefinitionException.class )
    public void shouldNotAllowUnregisteringUsedPrimaryType() throws Exception {
        Session adminSession = null;

        try {
            adminSession = repository.login();
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
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root//*[jcr:contains(., 'liter')]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithContainsCriteriaAndPluralWord() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("/jcr:root//*[jcr:contains(., 'liters')]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteria() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("/jcr:root//*[jcr:contains(., '\"liters V 12\"')]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteriaWithHyphen() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("/jcr:root//*[jcr:contains(., '\"5-speed\"')]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteriaWithHyphenAndNumberAndWildcard()
        throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("/jcr:root//*[jcr:contains(., '\"spee*\"')]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 2);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteriaWithNoHyphenAndNoWildcard() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("/jcr:root//*[jcr:contains(., '\"heavy duty\"')]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteriaWithHyphenAndNoWildcard() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("/jcr:root//*[jcr:contains(., '\"heavy-duty\"')]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteriaWithNoHyphenAndWildcard() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("/jcr:root//*[jcr:contains(., '\"heavy du*\"')]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteriaWithNoHyphenAndLeadingWildcard()
        throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("/jcr:root//*[jcr:contains(., '\"*avy duty\"')]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteriaWithHyphenAndWildcard() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("/jcr:root//*[jcr:contains(., '\"heavy-du*\"')]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Ignore
    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteriaWithHyphenAndLeadingWildcard()
        throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("/jcr:root//*[jcr:contains(., '\"*-speed\"')]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 2);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @FixFor( "MODE-790" )
    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithCompoundCriteria() throws Exception {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("/jcr:root/Cars//element(*,car:Car)[@car:year='2008' and jcr:contains(., '\"liters V 12\"')]",
                                          Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));

        assertResults(query, result, 1);
        assertResultsHaveColumns(result,
                                 "jcr:primaryType",
                                 "jcr:mixinTypes",
                                 "jcr:path",
                                 "jcr:score",
                                 "jcr:created",
                                 "jcr:createdBy",
                                 "jcr:name",
                                 "mode:localName",
                                 "mode:depth",
                                 "car:mpgCity",
                                 "car:userRating",
                                 "car:mpgHighway",
                                 "car:engine",
                                 "car:model",
                                 "car:year",
                                 "car:maker",
                                 "car:lengthInInches",
                                 "car:valueRating",
                                 "car:wheelbaseInInches",
                                 "car:msrp",
                                 "car:alternateModels");

        // Query again with a different criteria that should return no nodes ...
        query = session.getWorkspace()
                       .getQueryManager()
                       .createQuery("/jcr:root/Cars//element(*,car:Car)[@car:year='2007' and jcr:contains(., '\"liter V 12\"')]",
                                    Query.XPATH);
        assertThat(query, is(notNullValue()));
        result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 0);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithElementTestForChildrenOfRoot() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/element()", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 4L);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithElementTestForAllNodesBelowRoot() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root//element()", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, totalNodeCount);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithElementTestForChildOfRootWithName() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/element(Cars)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithElementTestForSingleNodeBelowRootWithName() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root//element(Utility)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithElementTestForChildrenOfRootWithName() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/Other/element(NodeA)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 3);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithElementTestForMultipleNodesBelowRootWithName() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root//element(NodeA)", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 3);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Ignore
    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithRangeCriteria() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("/jcr:root/Other/*[@somethingElse <= 'value2' and @somethingElse > 'value1']",
                                          Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithNewlyRegisteredNamespace() throws RepositoryException {
        session.getWorkspace().getNamespaceRegistry().registerNamespace("newPrefix", "newUri");

        try {
            // We don't have any elements that use this yet, but let's at least verify that it can execute.
            Query query = session.getWorkspace()
                                 .getQueryManager()
                                 .createQuery("//*[@newPrefix:someColumn = 'someValue']", Query.XPATH);
            query.execute();
        } finally {
            session.getWorkspace().getNamespaceRegistry().unregisterNamespace("newPrefix");
        }
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldNotReturnNodesWithNoPropertyForPropertyCriterion() throws Exception {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("/jcr:root/Cars//*[@car:wheelbaseInInches]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));

        for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
            assertThat(iter.nextNode().hasProperty("car:wheelbaseInInches"), is(true));
        }
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldNotReturnNodesWithNoPropertyForLikeCriterion() throws Exception {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("/jcr:root/Cars//*[jcr:like(@car:wheelbaseInInches, '%')]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));

        for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
            assertThat(iter.nextNode().hasProperty("car:wheelbaseInInches"), is(true));
        }
    }

    @FixFor( "MODE-1144" )
    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldParseMagnoliaXPathQuery() throws Exception {

        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("//*[@jcr:primaryType='mgnl:content']//*[jcr:contains(., 'paragraph')]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));

        for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
            assertThat(iter.nextNode().hasProperty("car:wheelbaseInInches"), is(true));
        }
    }

    @FixFor( "MODE-1145" )
    @Test
    public void shouldParseFincayraQuery() throws Exception {
        String sql = "SELECT post.\"jcr:uuid\", post.\"text\", post.\"user\" FROM [fincayra.Post] AS post JOIN [fincayra.User] AS u ON post.\"user\"=u.\"jcr:uuid\" WHERE u.email='test1@innobuilt.com'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
            assertThat(iter.nextNode().hasProperty("jcr:uuid"), is(true));
            assertThat(iter.nextNode().hasProperty("text"), is(true));
            assertThat(iter.nextNode().hasProperty("user"), is(true));
        }
    }

    @FixFor( "MODE-1145" )
    @Test
    public void shouldParseFincayraQuery2() throws Exception {
        String sql = "SELECT post.\"jcr:uuid\", post.\"text\", post.\"user\" FROM [fincayra.UnstrPost] AS post JOIN [fincayra.UnstrUser] AS u ON post.\"user\"=u.\"jcr:uuid\" WHERE u.email='test1@innobuilt.com'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
            assertThat(iter.nextNode().hasProperty("jcr:uuid"), is(true));
            assertThat(iter.nextNode().hasProperty("text"), is(true));
            assertThat(iter.nextNode().hasProperty("user"), is(true));
        }
    }

    @FixFor( "MODE-1145" )
    @Test
    public void shouldParseQueryWithResidualPropertyInSelectAndCriteria() throws Exception {
        String sql = "SELECT [jcr:path], something FROM [nt:unstructured] AS u WHERE something LIKE 'value%'";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result.getRows().getSize(), is(2L));
        for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
            assertThat(iter.nextNode().hasProperty("something"), is(true));
        }
    }

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
        assertThat(query1, is(notNullValue()));
        QueryResult result1 = query1.execute();
        assertThat(result1, is(notNullValue()));
        assertResults(query1, result1, 4L);
        assertResultsHaveColumns(result1, "maker", "car:model", "car:year", "car:userRating");

        // Now get the JCR-SQL2 statement from the QOM ...
        String expectedExpr = "SELECT car.[car:maker] AS maker, car.[car:model], car.[car:year], car.[car:userRating] FROM [car:Car] AS car WHERE car.[car:userRating] LIKE '4'";
        String expr = query1.getStatement();
        assertThat(expr, is(expectedExpr));

        // Now execute it ...
        Query query2 = queryManager.createQuery(expr, Query.JCR_SQL2);
        assertThat(query2, is(notNullValue()));
        QueryResult result2 = query2.execute();
        assertThat(result2, is(notNullValue()));
        assertResults(query2, result2, 4L);
        assertResultsHaveColumns(result2, "maker", "car:model", "car:year", "car:userRating");

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
        assertThat(query1, is(notNullValue()));
        QueryResult result1 = query1.execute();
        assertThat(result1, is(notNullValue()));
        assertResults(query1, result1, 2L);
        assertResultsHaveColumns(result1, "maker", "car:model", "car:year", "car:userRating");

        // Now get the JCR-SQL2 statement from the QOM ...
        String expectedExpr = "SELECT car.[car:maker] AS maker, car.[car:model], car.[car:year], car.[car:userRating] FROM [car:Car] AS car WHERE car.[car:userRating] LIKE '4' LIMIT 2";
        String expr = query1.getStatement();
        assertThat(expr, is(expectedExpr));

        // Now execute it ...
        Query query2 = queryManager.createQuery(expr, Query.JCR_SQL2);
        assertThat(query2, is(notNullValue()));
        QueryResult result2 = query2.execute();
        assertThat(result2, is(notNullValue()));
        assertResults(query2, result2, 2L);
        assertResultsHaveColumns(result2, "maker", "car:model", "car:year", "car:userRating");
    }

    @Test
    public void shouldFindSystemNodesUsingPathCriteria() throws Exception {
        String queryString = "select [jcr:path] from [nt:base] where [jcr:path] like '/jcr:system/%' and [jcr:path] not like '/jcr:system/%/%'";
        assertNodesAreFound(queryString, Query.JCR_SQL2, INDEXED_SYSTEM_NODES_PATHS);
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

        assertEquals(session.getWorkspace().getNodeTypeManager().getAllNodeTypes().getSize(), result.getNodes().getSize());
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
            Thread.sleep(1000);

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

            String queryString = "SELECT DISTINCT target.* " + "   FROM [test:node] AS node "
                                 + "   JOIN [test:relationship] AS relationship ON ISCHILDNODE(relationship, node) "
                                 + "   JOIN [test:node] AS target ON relationship.[test:target] = target.[jcr:uuid] "
                                 + "   WHERE node.[test:name] = 'A'";
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            QueryResult queryResult = queryManager.createQuery(queryString, Query.JCR_SQL2).execute();
            if (print) {
                System.out.println("queryResult = " + queryResult);
            }
            assertEquals(2, queryResult.getNodes().getSize());
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

            NodeIterator nodes = result.getNodes();
            assertEquals(2, nodes.getSize());
            List<String> resultIds = new ArrayList<String>();
            while (nodes.hasNext()) {
                resultIds.add(nodes.nextNode().getIdentifier());
            }
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

            NodeIterator nodes = result.getNodes();
            assertEquals(2, nodes.getSize());
            List<String> resultIds = new ArrayList<String>();
            while (nodes.hasNext()) {
                resultIds.add(nodes.nextNode().getIdentifier());
            }
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

            NodeIterator nodes = result.getNodes();
            assertEquals(3, nodes.getSize());
            List<String> resultIds = new ArrayList<String>();
            while (nodes.hasNext()) {
                resultIds.add(nodes.nextNode().getIdentifier());
            }
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
        NodeIterator nodes = query.execute().getNodes();
        List<Node> actual = new ArrayList<Node>();
        while (nodes.hasNext()) {
            actual.add(nodes.nextNode());
        }

        sql = "SELECT cars.[jcr:path] FROM [car:Car] AS cars WHERE contains(cars.*, 'Toyota')";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        nodes = query.execute().getNodes();
        List<Node> expected = new ArrayList<Node>();
        while (nodes.hasNext()) {
            expected.add(nodes.nextNode());
        }

        assertEquals(expected, actual);
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
            String queryString = "select * from [nt:unstructured] as node where " +
                                 "node.now <= CAST('2999-10-21T00:00:00.000' AS DATE) and node.now >= CAST('1999-10-21T00:00:00.000' AS DATE)";
            assertNodesAreFound(queryString, Query.JCR_SQL2, "/date_parent/date_node1", "/date_parent/date_node2");
        } finally {
            testParent.remove();
            session.save();
        }
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

            NodeIterator nit = result.getNodes();
            assertTrue(nit.hasNext());
            Node n11 = nit.nextNode();
            assertEquals("n1", n11.getName());
            assertTrue(!nit.hasNext());
        } finally {
            n1.remove();
            n2.remove();
            session.save();
        }
    }

    @Test
    @FixFor( "MODE-2173 ")
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

            //remove the READ permission for child1
            acl.addAccessControlEntry(SimplePrincipal.EVERYONE,
                                      new Privilege[] { acm.privilegeFromName(Privilege.JCR_WRITE),
                                                        acm.privilegeFromName(Privilege.JCR_REMOVE_NODE),
                                                        acm.privilegeFromName(Privilege.JCR_MODIFY_ACCESS_CONTROL)});
            acm.setPolicy("/parent/child1", acl);
            session.save();

            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);
            QueryResult result = query.execute();

            //assert that only child2 is still visible in the query results
            NodeIterator nodes = result.getNodes();
            //nodes are preloaded, so we know the correct size
            assertEquals(1, nodes.getSize());
            assertEquals("/parent/child2", nodes.nextNode().getPath());
            assertFalse(nodes.hasNext());

            RowIterator rows = result.getRows();
            //rows are not preloaded, so we don't know the actual size up front
            assertEquals(-1, rows.getSize());
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

    private String idList(Node...nodes) throws RepositoryException {
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
