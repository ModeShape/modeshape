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
package org.modeshape.jcr.query.index;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.MultiUseAbstractTest;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.api.Session;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class QueryEngineTest extends MultiUseAbstractTest {

    @BeforeClass
    public static void beforeAll() throws Exception {
        URL url = QueryEngineTest.class.getClassLoader().getResource("config/repo-config-no-indexes.json");
        RepositoryConfiguration config = RepositoryConfiguration.read(url);
        startRepository(config);

        try {
            // Use a session to load the contents ...
            Session session = repository.login();

            try {
                registerNodeTypes(session, "cnd/fincayra.cnd");
                registerNodeTypes(session, "cnd/magnolia.cnd");
                registerNodeTypes(session, "cnd/notionalTypes.cnd");
                registerNodeTypes(session, "cnd/cars.cnd");
                registerNodeTypes(session, "cnd/validType.cnd");

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

                // // Initialize the nodes count
                // initNodesCount();
                //
                // // Prime creating a first XPath query and SQL query ...
                // session.getWorkspace().getQueryManager().createQuery("//element(*,nt:unstructured)", Query.XPATH);
                // session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [nt:base]", Query.JCR_SQL2);
            } finally {
                session.logout();
            }

            // Prime creating the schemata ...
            // repository.nodeTypeManager().getRepositorySchemata();
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

    @Test
    public void shouldQueryForAllNodes() throws Exception {
        String sql = "SELECT * FROM [nt:base]";
        // print = true;
        query(sql);
    }

    @Test
    public void shouldQueryForAllDescendantNodesUnderCars() throws Exception {
        String sql = "SELECT * FROM [nt:base] AS nodes WHERE ISDESCENDANTNODE(nodes,'/Cars')";
        // print = true;
        query(sql, 17, pathStartsWith("/Cars/"));
    }

    @Test
    public void shouldQueryForAllChildrenUnderCars() throws Exception {
        String sql = "SELECT * FROM [nt:base] AS nodes WHERE ISCHILDNODE(nodes,'/Cars')";
        // print = true;
        query(sql, 4, pathStartsWith("/Cars/"));
    }

    @Test
    public void shouldQueryForNodeAtPath() throws Exception {
        String sql = "SELECT * FROM [nt:base] AS nodes WHERE PATH(nodes) = '/Cars'";
        // print = true;
        query(sql, 1, pathStartsWith("/Cars"));
    }

    @Test
    public void shouldQueryForNodeAtPathAndUnnecessaryCriteria() throws Exception {
        String sql = "SELECT * FROM [nt:base] AS nodes WHERE PATH(nodes) = '/Cars' AND ISCHILDNODE(nodes,'/')";
        // print = true;
        query(sql, 1, pathStartsWith("/Cars"));
    }

    @Test
    public void shouldQueryForNodeUsingJoinWithChildNodesWithOnlyJoinCriteria() throws Exception {
        String sql = "SELECT * FROM [nt:unstructured] AS category JOIN [car:Car] AS car ON ISCHILDNODE(car,category)";
        // print = true;
        query(sql, 13);
    }

    @Test
    public void shouldQueryForCarNodesAtPathAndUnnecessaryCriteria() throws Exception {
        String sql = "SELECT * FROM [car:Car] AS nodes WHERE PATH(nodes) = '/Cars/Utility/Toyota Land Cruiser'";
        // print = true;
        query(sql, 1, pathStartsWith("/Cars/Utility/Toyota Land Cruiser"));
    }

    @Ignore
    @Test
    public void shouldQueryNodesWithMultipleCriteria() throws Exception {
        String sql = "SELECT * FROM [nt:base] WHERE ([acme:something] = 'foo' AND [acme:prop2] = 3.55 ) OR [acme:prop3] > 2";
        // print = true;
        query(sql);
    }

    protected static interface Predicate {
        boolean accept( Node node ) throws RepositoryException;
    }

    protected Predicate pathStartsWith( final String prefix ) {
        return new Predicate() {
            @Override
            public boolean accept( Node node ) throws RepositoryException {
                return node != null && node.getPath().startsWith(prefix);
            }
        };
    }

    protected void query( String sql ) throws Exception {
        query(sql, -1, null);
    }

    protected void query( String sql,
                          Predicate predicate ) throws Exception {
        query(sql, -1, predicate);
    }

    protected void query( String sql,
                          long numberOfResults ) throws Exception {
        query(sql, Query.JCR_SQL2, numberOfResults, null);
    }

    protected void query( String sql,
                          long numberOfResults,
                          Predicate predicate ) throws Exception {
        query(sql, Query.JCR_SQL2, numberOfResults, predicate);
    }

    protected void query( String sql,
                          String language,
                          long numberOfResults,
                          Predicate predicate ) throws Exception {
        Query query = ((Session)session).getWorkspace().getQueryManager().createQuery(sql, language);
        QueryResult result = query.execute();
        assertResults(query, result, numberOfResults, predicate);
    }

    protected void assertResults( Query query,
                                  QueryResult result,
                                  long numberOfResults,
                                  Predicate predicate ) throws RepositoryException {
        assertThat(query, is(notNullValue()));
        assertThat(result, is(notNullValue()));
        if (print /*|| result.getNodes().getSize() != numberOfResults || result.getRows().getSize() != numberOfResults*/) {
            System.out.println();
            System.out.println(query);
            System.out.println(((org.modeshape.jcr.api.query.QueryResult)result).getPlan());
            System.out.println(result);
        }
        if (result.getSelectorNames().length == 1) {
            NodeIterator iter = result.getNodes();
            if (numberOfResults >= 0 && iter.getSize() != -1) {
                assertThat(iter.getSize(), is(numberOfResults));
            }
            int i = 0;
            while (iter.hasNext()) {
                Node node = iter.nextNode();
                if (print) System.out.println(" " + StringUtil.justifyRight(Integer.toString(++i), 4, ' ') + ". "
                                              + node.getPath());
                if (predicate != null) assertThat(predicate.accept(node), is(true));
            }
            if (print) System.out.println(); // add a blank separator line
        } else {
            try {
                result.getNodes();
                fail("should not be able to call this method when the query has multiple selectors");
            } catch (RepositoryException e) {
                // expected; can't call this when the query uses multiple selectors ...
            }
            String[] selectorNames = result.getSelectorNames();
            RowIterator rows = result.getRows();
            int rowCount = 0;
            while (rows.hasNext()) {
                Row row = rows.nextRow();
                if (print) System.out.print(" " + StringUtil.justifyRight(Integer.toString(++rowCount), 4, ' ') + ". ");
                for (int i = 0; i != selectorNames.length; ++i) {
                    Node node = row.getNode(selectorNames[i]);
                    if (print) {
                        if (i != 0) System.out.print(", ");
                        System.out.print(node.getPath());
                    }
                }
                if (print) System.out.println(); // complete the line
            }
            if (print) System.out.println(); // add a blank separator line
        }
        if (numberOfResults >= 0 && result.getRows().getSize() != -1) {
            assertThat(result.getRows().getSize(), is(numberOfResults));
        }
    }

    protected static void registerNodeTypes( Session session,
                                             String pathToClasspathResource ) throws RepositoryException, IOException {
        URL url = resourceUrl(pathToClasspathResource);
        session.getWorkspace().getNodeTypeManager().registerNodeTypes(url, true);
    }

    protected static URL resourceUrl( String name ) {
        return QueryEngineTest.class.getClassLoader().getResource(name);
    }

}
