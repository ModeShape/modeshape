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

import java.util.Calendar;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.ValidateQuery.ValidationBuilder;
import org.modeshape.jcr.api.index.IndexColumnDefinition;
import org.modeshape.jcr.api.index.IndexDefinition.IndexKind;
import org.modeshape.jcr.api.index.IndexDefinitionTemplate;
import org.modeshape.jcr.api.index.IndexManager;
import org.modeshape.jcr.api.query.Query;

public class LocalIndexProviderTest extends SingleUseAbstractTest {

    private static final String PROVIDER_NAME = "local";

    @Before
    @Override
    public void beforeEach() throws Exception {
        // We're using a Repository configuration that persists content, so clean it up ...
        FileUtil.delete("target/local_index_test_repository");

        // Now start the repository ...
        startRepositoryWithConfiguration(resource("config/repo-config-local-provider-no-indexes.json"));
        printMessage("Started repository...");
    }

    @After
    @Override
    public void afterEach() throws Exception {
        super.afterEach();
        FileUtil.delete("target/local_index_test_repository");
    }

    @Test
    public void shouldAllowRegisteringNewIndexDefinitionWithSingleStringColumn() throws Exception {
        String indexName = "descriptionIndex";
        registerValueIndex(indexName, "mix:title", "Index for the 'jcr:title' property on mix:title", "*", "jcr:title",
                           PropertyType.STRING);
        Thread.sleep(500L);

        indexManager().unregisterIndexes(indexName);
        Thread.sleep(500L);
    }

    @Test
    public void shouldUseSingleColumnStringIndexInQueryAgainstSameNodeType() throws Exception {
        registerValueIndex("descriptionIndex", "mix:title", "Index for the 'jcr:title' property on mix:title", "*", "jcr:title",
                           PropertyType.STRING);

        // print = true;

        // Add a node that uses this type ...
        Node root = session().getRootNode();
        Node book1 = root.addNode("myFirstBook");
        book1.addMixin("mix:title");
        book1.setProperty("jcr:title", "The Title");

        Node book2 = root.addNode("mySecondBook");
        book2.addMixin("mix:title");
        book2.setProperty("jcr:title", "A Different Title");

        // Create a node that is not a 'mix:title' and therefore won't be included in the SELECT clauses ...
        Node other = root.addNode("somethingElse");
        other.setProperty("propA", "a value for property A");
        other.setProperty("jcr:title", "The Title");

        Thread.sleep(500L);
        session.save();
        Thread.sleep(500L);

        // Compute a query plan that should use this index ...
        Query query = jcrSql2Query("SELECT * FROM [mix:title] WHERE [jcr:title] = 'The Title'");
        validateQuery().rowCount(1L).validate(query, query.execute());

        // Compute a query plan that should use this index ...
        query = jcrSql2Query("SELECT * FROM [mix:title] WHERE [jcr:title] LIKE 'The Title'");
        validateQuery().rowCount(1L).validate(query, query.execute());

        // Compute a query plan that should use this index ...
        query = jcrSql2Query("SELECT * FROM [mix:title] WHERE [jcr:title] LIKE 'The %'");
        validateQuery().rowCount(1L).validate(query, query.execute());

        // Compute a query plan that should use this index ...
        query = jcrSql2Query("SELECT * FROM [mix:title] WHERE [jcr:title] LIKE '% Title'");
        validateQuery().rowCount(2L).validate(query, query.execute());
    }

    @Test
    public void shouldNotUseSingleColumnStringIndexInQueryAgainstSuperType() throws Exception {
        registerValueIndex("descriptionIndex", "mix:title", "Index for the 'jcr:title' property on mix:title", "*", "jcr:title",
                           PropertyType.STRING);

        // print = true;

        // Add a node that uses this type ...
        Node root = session().getRootNode();
        Node book1 = root.addNode("myFirstBook");
        book1.addMixin("mix:title");
        book1.setProperty("jcr:title", "The Title");

        Node book2 = root.addNode("mySecondBook");
        book2.addMixin("mix:title");
        book2.setProperty("jcr:title", "A Different Title");

        // Create a node that is not a 'mix:title' and therefore won't be included in the query ...
        Node other = root.addNode("somethingElse");
        other.setProperty("propA", "a value for property A");
        other.setProperty("jcr:title", "The Title");

        Thread.sleep(500L);
        session.save();
        Thread.sleep(500L);

        // Compute a query plan that will NOT use this index, since the selector doesn't match the index's node type.
        // If we would use this index, the index doesn't know about non-mix:title nodes like the 'other' node ...
        Query query = jcrSql2Query("SELECT * FROM [nt:base] WHERE [jcr:title] = 'The Title'");
        validateQuery().rowCount(2L).validate(query, query.execute());

        // Compute a query plan that will NOT use this index, since the selector doesn't match the index's node type.
        // If we would use this index, the index doesn't know about non-mix:title nodes like the 'other' node ...
        query = jcrSql2Query("SELECT * FROM [nt:base] WHERE [jcr:title] LIKE '% Title'");
        validateQuery().rowCount(3L).validate(query, query.execute());
    }

    @Test
    public void shouldUseSingleColumnLongIndexInQueryAgainstSameNodeType() throws Exception {
        registerNodeTypes("cnd/notionalTypes.cnd");
        registerValueIndex("longIndex", "notion:typed", "Long value index", "*", "notion:longProperty", PropertyType.LONG);

        // print = true;

        // Add a node that uses this type ...
        Node root = session().getRootNode();
        Node obj1 = root.addNode("notionalObjectA", "notion:typed");
        obj1.setProperty("notion:longProperty", 1234L);
        obj1.setProperty("notion:booleanProperty", true);

        Node obj2 = root.addNode("notionalObjectB", "notion:typed");
        obj2.setProperty("notion:longProperty", 2345L);
        obj2.setProperty("notion:booleanProperty", true);

        Node obj3 = root.addNode("notionalObjectB", "notion:typed");
        obj3.setProperty("notion:longProperty", -1L);
        obj3.setProperty("notion:booleanProperty", true);

        // Create a node that is not a 'notion:typed' and therefore won't be included in the query ...
        Node other = root.addNode("somethingElse");
        other.setProperty("notion:longProperty", 100L);
        other.setProperty("jcr:title", "The Title");

        Thread.sleep(500L);
        session.save();
        Thread.sleep(500L);

        // Issues some queries that should use this index ...
        Query query = jcrSql2Query("SELECT * FROM [notion:typed] WHERE [notion:longProperty] = 1234");
        validateQuery().rowCount(1L).validate(query, query.execute());

        query = jcrSql2Query("SELECT * FROM [notion:typed] WHERE [notion:longProperty] <= 1234");
        validateQuery().rowCount(2L).validate(query, query.execute());

        query = jcrSql2Query("SELECT * FROM [notion:typed] WHERE [notion:longProperty] < 1234");
        validateQuery().rowCount(1L).validate(query, query.execute());

        query = jcrSql2Query("SELECT * FROM [notion:typed] WHERE [notion:longProperty] > 0");
        validateQuery().rowCount(2L).validate(query, query.execute());

        query = jcrSql2Query("SELECT * FROM [notion:typed] WHERE [notion:longProperty] BETWEEN 1234 AND 5678");
        validateQuery().rowCount(2L).validate(query, query.execute());

        query = jcrSql2Query("SELECT * FROM [notion:typed] WHERE [notion:longProperty] BETWEEN 1234 EXCLUSIVE AND 5678");
        validateQuery().rowCount(1L).validate(query, query.execute());

        query = jcrSql2Query("SELECT * FROM [notion:typed] WHERE [notion:longProperty] NOT BETWEEN 1234 EXCLUSIVE AND 5678");
        validateQuery().rowCount(2L).validate(query, query.execute());

        query = jcrSql2Query("SELECT * FROM [notion:typed] WHERE [notion:longProperty] <= -1");
        validateQuery().rowCount(1L).validate(query, query.execute());

        query = jcrSql2Query("SELECT * FROM [notion:typed] WHERE [notion:longProperty] <= CAST('-1' AS LONG)");
        validateQuery().rowCount(1L).validate(query, query.execute());

        query = jcrSql2Query("SELECT * FROM [notion:typed] WHERE [notion:longProperty] < -1");
        validateQuery().rowCount(0L).validate(query, query.execute());

        // Issue a query that does not use this index ...
        query = jcrSql2Query("SELECT * FROM [nt:unstructured] WHERE [notion:longProperty] > 10");
        validateQuery().rowCount(3L).validate(query, query.execute());
    }

    @Test
    public void shouldUseSingleColumnDateIndexInQueryAgainstSameNodeType() throws Exception {
        registerValueIndex("dateIndex", "mix:lastModified", "Date value index", "*", "jcr:lastModified", PropertyType.DATE);

        // print = true;

        // Add a node that uses this type ...
        Node root = session().getRootNode();
        Node obj1 = root.addNode("notionalObjectA");
        obj1.addMixin("mix:lastModified");

        Node obj2 = root.addNode("notionalObjectB");
        obj2.addMixin("mix:lastModified");

        // Create a node that is not a 'notion:typed' and therefore won't be included in the query ...
        Node other = root.addNode("somethingElse");
        other.setProperty("jcr:lastModified", Calendar.getInstance());

        Thread.sleep(500L);
        session.save();
        Thread.sleep(500L);

        // Issues some queries that should use this index ...
        Query query = jcrSql2Query("SELECT * FROM [mix:lastModified] WHERE [jcr:lastModified] > CAST('2012-10-21T00:00:00.000' AS DATE)");
        validateQuery().rowCount(2L).validate(query, query.execute());

        query = jcrSql2Query("SELECT * FROM [mix:lastModified] WHERE [jcr:lastModified] < CAST('2999-10-21T00:00:00.000' AS DATE)");
        validateQuery().rowCount(2L).validate(query, query.execute());

        // Issue a query that does not use this index ...
        query = jcrSql2Query("SELECT * FROM [nt:unstructured] WHERE [jcr:lastModified] > CAST('2012-10-21T00:00:00.000' AS DATE)");
        validateQuery().rowCount(3L).validate(query, query.execute());
    }

    @Test
    public void shouldUseSingleColumnDateAsLongIndexInQueryAgainstSameNodeType() throws Exception {
        registerValueIndex("dateIndex", "mix:lastModified", "Date value index", "*", "jcr:lastModified", PropertyType.LONG);

        // print = true;

        // Add a node that uses this type ...
        Node root = session().getRootNode();
        Node obj1 = root.addNode("notionalObjectA");
        obj1.addMixin("mix:lastModified");

        Node obj2 = root.addNode("notionalObjectB");
        obj2.addMixin("mix:lastModified");

        // Create a node that is not a 'notion:typed' and therefore won't be included in the query ...
        Node other = root.addNode("somethingElse");
        other.setProperty("jcr:lastModified", Calendar.getInstance());

        Thread.sleep(500L);
        session.save();
        Thread.sleep(500L);

        // Issues some queries that should use this index ...
        Query query = jcrSql2Query("SELECT * FROM [mix:lastModified] WHERE [jcr:lastModified] > CAST('2012-10-21T00:00:00.000' AS DATE)");
        validateQuery().rowCount(2L).validate(query, query.execute());

        query = jcrSql2Query("SELECT * FROM [mix:lastModified] WHERE [jcr:lastModified] < CAST('2999-10-21T00:00:00.000' AS DATE)");
        validateQuery().rowCount(2L).validate(query, query.execute());

        // Issue a query that does not use this index ...
        query = jcrSql2Query("SELECT * FROM [nt:unstructured] WHERE [jcr:lastModified] > CAST('2012-10-21T00:00:00.000' AS DATE)");
        validateQuery().rowCount(3L).validate(query, query.execute());
    }

    @Test
    public void shouldUseSingleColumnNodeNameIndexInQueryAgainstSameNodeType() throws Exception {
        registerValueIndex("nameIndex", "nt:base", "Node name index", "*", "jcr:name", PropertyType.NAME);

        // print = true;

        // Add a node that uses this type ...
        Node book1 = session().getRootNode().addNode("myFirstBook");
        book1.addMixin("mix:title");
        book1.setProperty("jcr:title", "The Title");

        Node book2 = session().getRootNode().addNode("mySecondBook");
        book2.addMixin("mix:title");
        book2.setProperty("jcr:title", "A Different Title");

        Node other = session().getRootNode().addNode("somethingElse");
        other.setProperty("propA", "a value for property A");
        other.setProperty("jcr:title", "The Title");

        Thread.sleep(500L);
        session.save();
        Thread.sleep(500L);

        // Issues some queries that should use this index ...
        Query query = jcrSql2Query("SELECT * FROM [nt:base] WHERE [jcr:name] = 'myFirstBook'");
        validateQuery().rowCount(1L).validate(query, query.execute());

        query = jcrSql2Query("SELECT * FROM [nt:base] WHERE NAME() LIKE 'myFirstBook'");
        validateQuery().rowCount(1L).validate(query, query.execute());

        query = jcrSql2Query("SELECT * FROM [nt:unstructured] WHERE NAME() LIKE '%Book'");
        validateQuery().rowCount(2L).validate(query, query.execute());
    }

    @Test
    public void shouldUseSingleColumnNodeDepthIndexInQueryAgainstSameNodeType() throws Exception {
        registerValueIndex("depthIndex", "nt:unstructured", "Node depth index", "*", "mode:depth", PropertyType.LONG);

        // print = true;

        // Add a node that uses this type ...
        Node book1 = session().getRootNode().addNode("myFirstBook");
        book1.addMixin("mix:title");
        book1.setProperty("jcr:title", "The Title");

        Node book2 = session().getRootNode().addNode("mySecondBook");
        book2.addMixin("mix:title");
        book2.setProperty("jcr:title", "A Different Title");

        Node other = book2.addNode("chapter");
        other.setProperty("propA", "a value for property A");
        other.setProperty("jcr:title", "The Title");

        Thread.sleep(500L);
        session.save();
        Thread.sleep(500L);

        // Issues some queries that should use this index ...
        Query query = jcrSql2Query("SELECT * FROM [nt:unstructured] WHERE [mode:depth] > 0");
        validateQuery().rowCount(3L).validate(query, query.execute());

        query = jcrSql2Query("SELECT * FROM [nt:unstructured] WHERE DEPTH() > 0");
        validateQuery().rowCount(3L).validate(query, query.execute());

        query = jcrSql2Query("SELECT * FROM [nt:unstructured] WHERE DEPTH() > 1");
        validateQuery().rowCount(1L).validate(query, query.execute());

        query = jcrSql2Query("SELECT * FROM [nt:unstructured] WHERE DEPTH() >= 2");
        validateQuery().rowCount(1L).validate(query, query.execute());
    }

    @Test
    public void shouldUseSingleColumnNodePathIndexInQueryAgainstSameNodeType() throws Exception {
        registerValueIndex("pathIndex", "nt:unstructured", "Node path index", "*", "jcr:path", PropertyType.PATH);

        // print = true;

        // Add a node that uses this type ...
        Node book1 = session().getRootNode().addNode("myFirstBook");
        book1.addMixin("mix:title");
        book1.setProperty("jcr:title", "The Title");

        Node book2 = session().getRootNode().addNode("mySecondBook");
        book2.addMixin("mix:title");
        book2.setProperty("jcr:title", "A Different Title");

        Node other = book2.addNode("chapter");
        other.setProperty("propA", "a value for property A");
        other.setProperty("jcr:title", "The Title");

        Thread.sleep(500L);
        session.save();
        Thread.sleep(500L);

        // Issues some queries that should use this index ...
        Query query = jcrSql2Query("SELECT * FROM [nt:unstructured] WHERE [jcr:path] = '/myFirstBook'");
        validateQuery().rowCount(1L).validate(query, query.execute());

        query = jcrSql2Query("SELECT * FROM [nt:unstructured] WHERE [jcr:path] LIKE '/my%Book'");
        validateQuery().rowCount(2L).validate(query, query.execute());

        query = jcrSql2Query("SELECT * FROM [nt:unstructured] WHERE [jcr:path] > '/mySecondBook'");
        validateQuery().rowCount(1L).validate(query, query.execute());

        query = jcrSql2Query("SELECT * FROM [nt:unstructured] WHERE PATH() > '/mySecondBook'");
        validateQuery().rowCount(1L).validate(query, query.execute());
    }

    protected void registerValueIndex( String indexName,
                                       String indexedNodeType,
                                       String desc,
                                       String workspaceNamePattern,
                                       String propertyName,
                                       int propertyType ) throws RepositoryException {
        registerIndex(indexName, IndexKind.VALUE, PROVIDER_NAME, indexedNodeType, desc, workspaceNamePattern, propertyName,
                      propertyType);
    }

    protected void registerIndex( String indexName,
                                  IndexKind kind,
                                  String providerName,
                                  String indexedNodeType,
                                  String desc,
                                  String workspaceNamePattern,
                                  String propertyName,
                                  int propertyType ) throws RepositoryException {
        // Create the index template ...
        IndexDefinitionTemplate template = indexManager().createIndexDefinitionTemplate();
        template.setName(indexName);
        template.setKind(kind);
        template.setNodeTypeName(indexedNodeType);
        template.setProviderName(providerName);
        if (workspaceNamePattern != null) {
            template.setWorkspaceNamePattern(workspaceNamePattern);
        } else {
            template.setAllWorkspaces();
        }
        if (desc != null) {
            template.setDescription(desc);
        }

        // Set up the columns ...
        IndexColumnDefinition colDefn = indexManager().createIndexColumnDefinitionTemplate().setPropertyName(propertyName)
                                                      .setColumnType(propertyType);
        template.setColumnDefinitions(colDefn);

        // Register the index ...
        indexManager().registerIndex(template, false);
    }

    protected IndexManager indexManager() throws RepositoryException {
        return session().getWorkspace().getIndexManager();
    }

    protected Query jcrSql2Query( String expr ) throws RepositoryException {
        return (Query)session().getWorkspace().getQueryManager().createQuery(expr, Query.JCR_SQL2);
    }

    protected ValidationBuilder validateQuery() {
        return ValidateQuery.validateQuery().printDetail(print);
    }

}
