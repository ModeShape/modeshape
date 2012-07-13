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

import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_LIKE;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
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
import java.util.Collections;
import java.util.HashSet;
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
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Literal;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.PropertyValue;
import javax.jcr.query.qom.Selector;
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
import org.modeshape.jcr.query.JcrQueryResult;
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

    private static final String[] SYSTEM_NODES_PATHS = new String[] {
            "/jcr:system/jcr:nodeTypes",
            "/jcr:system/jcr:versionStorage",
            "/jcr:system/mode:namespaces",
            "/jcr:system/mode:locks" };

    private static final boolean WRITE_INDEXES_TO_FILE = false;

    /** The total number of nodes at or below '/jcr:system' */
    protected static final int TOTAL_SYSTEM_NODE_COUNT = 238;

    /** The total number of nodes excluding '/jcr:system' */
    protected static final int TOTAL_NON_SYSTEM_NODE_COUNT = 25;

    /** The total number of nodes */
    protected static final int TOTAL_NODE_COUNT = TOTAL_SYSTEM_NODE_COUNT + TOTAL_NON_SYSTEM_NODE_COUNT;

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

    protected static String[] searchColumnNames() {
        return new String[] {};
    }

    private boolean print;

    @SuppressWarnings( "deprecation" )
    @BeforeClass
    public static void beforeAll() throws Exception {
        if (WRITE_INDEXES_TO_FILE) {
            File dir = new File("target/querytest/indexes");
            if (dir.exists()) FileUtil.delete(dir);

            String configFileName = JcrQueryManagerTest.class.getSimpleName() + ".json";
            String configFilePath = "config/" + configFileName;
            InputStream configStream = JcrQueryManagerTest.class.getClassLoader().getResourceAsStream(configFilePath);
            assertThat("Unable to find configuration file '" + configFilePath, configStream, is(notNullValue()));

            Document configDoc = Json.read(configStream);
            RepositoryConfiguration config = new RepositoryConfiguration(configDoc, configFileName);
            startRepository(config);
        } else {
            startRepository(null);
        }

        try {
            // Use a session to load the contents ...
            JcrSession session = repository.login();
            try {
                registerNodeTypes(session, "cnd/fincayra.cnd");
                registerNodeTypes(session, "cnd/magnolia.cnd");
                registerNodeTypes(session, "cnd/notionalTypes.cnd");
                registerNodeTypes(session, "cnd/cars.cnd");

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
                a.setProperty("something", "value3 quick brown fox");
                a.setProperty("somethingElse", "value2");
                a.setProperty("propA", "value1");
                Node other2 = other.addNode("NodeA", "nt:unstructured");
                other2.setProperty("something", "value2 quick brown cat wearing hat");
                other2.setProperty("propB", "value1");
                other2.setProperty("propC", "value2");
                Node other3 = other.addNode("NodeA", "nt:unstructured");
                other3.setProperty("something", new String[] {"black dog", "white dog"});
                other3.setProperty("propB", "value1");
                other3.setProperty("propC", "value3");
                Node c = other.addNode("NodeC", "notion:typed");
                c.setProperty("notion:booleanProperty", true);
                c.setProperty("notion:booleanProperty2", false);
                c.setProperty("propD", "value4");
                c.setProperty("propC", "value1");
                session.getRootNode().addNode("NodeB", "nt:unstructured").setProperty("myUrl", "http://www.acme.com/foo/bar");
                session.save();

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
            System.out.println(" plan -> " + ((JcrQueryResult)result).getPlan());
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

    @Test
    public void shouldBeAbleToCreateAndExecuteJcrSql2QueryToFindAllNodes() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [nt:base]", Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, TOTAL_NODE_COUNT);
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
        assertResults(query, result, TOTAL_NODE_COUNT);
        assertResultsHaveColumns(result, allColumnNames("nt:base"));
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
        assertResults(query, result, TOTAL_NODE_COUNT);
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
        assertResults(query, result, TOTAL_NODE_COUNT);
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
        assertResults(query, result, TOTAL_NODE_COUNT);
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
        assertResults(query, result, TOTAL_NODE_COUNT);
        assertResultsHaveColumns(result, allColumnNames("nt:base"));
        RowIterator iter = result.getRows();
        while (iter.hasNext()) {
            Row row = iter.nextRow();
            assertThat(row, is(notNullValue()));
        }
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
        // print = true;
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
        assertResults(query, result, TOTAL_NODE_COUNT);

        // Find all nodes that are NOT children of '/Cars' (and not under '/jcr:system') ...
        sql = "SELECT [jcr:path] FROM [nt:base] WHERE NOT(ISCHILDNODE([nt:base],'/Cars')) AND NOT(ISDESCENDANTNODE([nt:base],'/jcr:system')) ORDER BY [jcr:path]";
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        assertThat(query, is(notNullValue()));
        // print = true;
        result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 23L);
        assertResultsHaveColumns(result, new String[] {"jcr:path"});
        assertResultsHaveRows(result,
                              "jcr:path",
                              "/",
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
        assertResults(query, result, 1881L);
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

    @Ignore
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

    @Ignore
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

    @Ignore
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
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Full-text Search Queries
    // ----------------------------------------------------------------------------------------------------------------

    @Ignore
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

    @Ignore
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

    @Ignore
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
        assertResults(query, result, TOTAL_NODE_COUNT);
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
        assertResults(query, result, TOTAL_NODE_COUNT);
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
        assertResults(query, result, TOTAL_NODE_COUNT);
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
        assertResults(query, result, TOTAL_NODE_COUNT);
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
        assertResults(query, result, 2);
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
        Query query = session.getWorkspace().getQueryManager().createQuery("//Infiniti_x0020_G37[@car:year='2008']",
                                                                           Query.XPATH);
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

    @Ignore
    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithContainsCriteria() throws RepositoryException {
        Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root//*[jcr:contains(., 'liter')]",
                                                                           Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 2);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Ignore
    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteria() throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("/jcr:root//*[jcr:contains(., '\"liter V 12\"')]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Ignore
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

    @Ignore
    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithComplexContainsCriteriaWithHyphenAndNumberAndWildcard()
        throws RepositoryException {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("/jcr:root//*[jcr:contains(., '\"5-s*\"')]", Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));
        assertResults(query, result, 1);
        assertResultsHaveColumns(result, "jcr:primaryType", "jcr:path", "jcr:score");
    }

    @Ignore
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

    @Ignore
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

    @Ignore
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

    @Ignore
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

    @Ignore
    @FixFor( "MODE-790" )
    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldBeAbleToExecuteXPathQueryWithCompoundCriteria() throws Exception {
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery(
                                     "/jcr:root/Cars//element(*,car:Car)[@car:year='2008' and jcr:contains(., '\"liter V 12\"')]",
                                     Query.XPATH);
        assertThat(query, is(notNullValue()));
        QueryResult result = query.execute();
        assertThat(result, is(notNullValue()));

        assertResults(query, result, 1);
        assertResultsHaveColumns(result,
                                 "jcr:primaryType",
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
        assertResults(query, result, TOTAL_NODE_COUNT);
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

        // We don't have any elements that use this yet, but let's at least verify that it can execute.
        Query query = session.getWorkspace()
                             .getQueryManager()
                             .createQuery("//*[@newPrefix:someColumn = 'someValue']", Query.XPATH);
        query.execute();

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
        assertNodesAreFound(queryString, Query.JCR_SQL2, SYSTEM_NODES_PATHS);
    }

    @Test
    public void shouldFindSystemNodesUsingIsChildNodeCriteria() throws Exception {
        String queryString = "select [jcr:path] from [nt:base] where ischildnode('/jcr:system')";
        assertNodesAreFound(queryString, Query.JCR_SQL2, SYSTEM_NODES_PATHS);
    }

    @Test
    @Ignore("Ignored until 1550 is fixed")
    @FixFor( "MODE-1550" )
    public void shouldFindChildrenOfRootUsingIsChildNodeCriteria() throws Exception {
        session.getRootNode().addNode("node1");
        session.getRootNode().addNode("node2");

        String queryString = "select [jcr:path] from [nt:base] where ischildnode('/')";
        assertNodesAreFound(queryString, Query.JCR_SQL2, "/jcr:system", "/node1", "/node2");
    }

    private void assertNodesAreFound( String queryString,
                                      String queryType,
                                      String... expectedNodesPaths) throws RepositoryException {
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(queryString, queryType);
        QueryResult result = query.execute();

        List<String> actualNodePaths = new ArrayList<String>();
        for (NodeIterator nodeIterator = result.getNodes(); nodeIterator.hasNext();) {
            actualNodePaths.add(nodeIterator.nextNode().getPath().toLowerCase());
        }

        List<String> expectedNodePaths = Arrays.asList(expectedNodesPaths);

        assertEquals(actualNodePaths.toString(), expectedNodePaths.size(), actualNodePaths.size());
        for (String expectedPath : expectedNodePaths) {
            assertTrue(expectedPath + " not found", actualNodePaths.remove(expectedPath.toLowerCase()));
        }
    }
}
