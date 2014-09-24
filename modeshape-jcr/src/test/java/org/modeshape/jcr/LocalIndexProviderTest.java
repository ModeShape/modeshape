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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.util.Calendar;
import java.util.concurrent.CountDownLatch;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.query.Row;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.ValidateQuery.ValidationBuilder;
import org.modeshape.jcr.api.ValueFactory;
import org.modeshape.jcr.api.index.IndexColumnDefinition;
import org.modeshape.jcr.api.index.IndexDefinition.IndexKind;
import org.modeshape.jcr.api.index.IndexDefinitionTemplate;
import org.modeshape.jcr.api.index.IndexManager;
import org.modeshape.jcr.api.query.Query;
import org.modeshape.jcr.query.engine.IndexPlanners;

public class LocalIndexProviderTest extends SingleUseAbstractTest {

    private static final String PROVIDER_NAME = "local";
    private static final String CONFIG_FILE = "config/repo-config-persistent-local-provider-no-indexes.json";
    private static final String STORAGE_DIR = "target/persistent_repository";

    @Before
    @Override
    public void beforeEach() throws Exception {
        // We're using a Repository configuration that persists content, so clean it up ...
        FileUtil.delete(STORAGE_DIR);

        // Now start the repository ...
        startRepositoryWithConfiguration(resource(CONFIG_FILE));
        printMessage("Started repository...");
    }

    @After
    @Override
    public void afterEach() throws Exception {
        super.afterEach();
        FileUtil.delete(STORAGE_DIR);
        // Thread.sleep(100L); // wait for the repository to shut down and terminate all listeners
    }

    @Test
    public void shouldAllowRegisteringNewIndexDefinitionWithSingleStringColumn() throws Exception {
        String indexName = "descriptionIndex";
        registerValueIndex(indexName, "mix:title", "Index for the 'jcr:title' property on mix:title", "*", "jcr:title",
                           PropertyType.STRING);
        waitForIndexes();
        indexManager().unregisterIndexes(indexName);
        waitForIndexes();
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

        waitForIndexes();
        session.save();
        waitForIndexes();

        // Compute a query plan that should use this index ...
        Query query = jcrSql2Query("SELECT * FROM [mix:title] WHERE [jcr:title] = 'The Title'");
        validateQuery().rowCount(1L).useIndex("descriptionIndex").validate(query, query.execute());

        // Compute a query plan that should NOT use this index ...
        query = jcrSql2Query("SELECT * FROM [mix:title] WHERE [jcr:title] LIKE 'The Title'");
        validateQuery().rowCount(1L).useNoIndexes().validate(query, query.execute());

        // Compute a query plan that should use this index ...
        query = jcrSql2Query("SELECT * FROM [mix:title] WHERE [jcr:title] LIKE 'The %'");
        validateQuery().rowCount(1L).useNoIndexes().validate(query, query.execute());

        // Compute a query plan that should use this index ...
        query = jcrSql2Query("SELECT * FROM [mix:title] WHERE [jcr:title] LIKE '% Title'");
        validateQuery().rowCount(2L).useNoIndexes().validate(query, query.execute());

        // Compute a query plan that should use this index ...
        query = jcrSql2Query("SELECT * FROM [mix:title]");
        validateQuery().rowCount(2L).useNoIndexes().validate(query, query.execute());
    }

    @Test
    public void shouldUseSingleColumnStringIndexForQueryWithNoCriteriaOtherThanPrimaryTypeViaFromClause() throws Exception {
        registerValueIndex("unstructuredNodes", "nt:unstructured", null, "*", "jcr:primaryType", PropertyType.NAME);

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

        waitForIndexes();
        session.save();
        waitForIndexes();

        // Compute a query plan that should use this index ...
        Query query = jcrSql2Query("SELECT * FROM [nt:unstructured]");
        validateQuery().rowCount(3L).useIndex("unstructuredNodes").validate(query, query.execute());
    }

    @FixFor( "MODE-2307" )
    @Test
    public void shouldUseSingleColumnStringIndexForQueryWithSubselect() throws Exception {
        registerNodeType("nt:typeWithReference");
        registerNodeType("nt:typeWithSysName");
        registerValueIndex("refIndex", "nt:typeWithReference", null, "*", "referenceId", PropertyType.STRING);
        registerValueIndex("sysIndex", "nt:typeWithSysName", null, "*", "sysName", PropertyType.STRING);

        // print = true;
        waitForIndexes();

        Node root = session().getRootNode();
        Node newNode1 = root.addNode("nodeWithSysName", "nt:typeWithSysName");
        newNode1.setProperty("sysName", "X");
        newNode1.addMixin("mix:referenceable");
        Node newNode2 = root.addNode("nodeWithReference", "nt:typeWithReference");
        newNode2.setProperty("referenceId", newNode1.getIdentifier());
        session().save();

        waitForIndexes();

        // Compute a query plan that should use this index ...
        Query query = jcrSql2Query("SELECT A.* FROM [nt:typeWithReference] AS A WHERE A.referenceId = $sysName");
        query.bindValue("sysName", valueFactory().createValue(newNode1.getIdentifier()));
        validateQuery().rowCount(1L).useIndex("refIndex").validate(query, query.execute());

        query = jcrSql2Query("SELECT A.* FROM [nt:typeWithReference] AS A WHERE A.referenceId IN ( $sysName )");
        query.bindValue("sysName", valueFactory().createValue(newNode1.getIdentifier()));
        validateQuery().rowCount(1L).useIndex("refIndex").validate(query, query.execute());

        query = jcrSql2Query("SELECT B.[jcr:uuid] FROM [nt:typeWithSysName] AS B WHERE B.sysName = $sysName");
        query.bindValue("sysName", valueFactory().createValue("X"));
        validateQuery().rowCount(1L).useIndex("sysIndex").validate(query, query.execute());

        query = jcrSql2Query("SELECT A.* FROM [nt:typeWithReference] AS A WHERE A.referenceId  IN ( "
                             + "SELECT B.[jcr:uuid] FROM [nt:typeWithSysName] AS B WHERE B.sysName = $sysName )");
        query.bindValue("sysName", valueFactory().createValue("X"));
        validateQuery().rowCount(1L).validate(query, query.execute());

    }

    @FixFor( "MODE-2307" )
    @Test
    public void shouldUseSingleColumnStringIndexForQueryWithJoin() throws Exception {
        registerNodeType("nt:typeWithReference");
        registerNodeType("nt:typeWithSysName");
        registerValueIndex("refIndex", "nt:typeWithReference", null, "*", "referenceId", PropertyType.STRING);
        registerValueIndex("sysIndex", "nt:typeWithSysName", null, "*", "sysName", PropertyType.STRING);
        registerNodeTypeIndex("typesIndex", "nt:base", null, "*", "jcr:primaryType", PropertyType.STRING);

        // print = true;

        Node root = session().getRootNode();
        Node newNode1 = root.addNode("nodeWithSysName", "nt:typeWithSysName");
        newNode1.setProperty("sysName", "X");
        newNode1.addMixin("mix:referenceable");
        Node newNode2 = root.addNode("nodeWithReference", "nt:typeWithReference");
        newNode2.setProperty("referenceId", newNode1.getIdentifier());

        waitForIndexes();
        session.save();
        waitForIndexes();

        // Compute a query plan that should use this index ...
        Query query = jcrSql2Query("SELECT A.* FROM [nt:typeWithReference] AS A "
                                   + "JOIN [nt:typeWithSysName] AS B ON A.referenceId  = B.[jcr:uuid] " //
                                   + "WHERE B.sysName = $sysName");
        query.bindValue("sysName", valueFactory().createValue("X"));
        validateQuery().rowCount(1L).validate(query, query.execute());

    }

    @FixFor( "MODE-2312" )
    @Test
    public void shouldUseImplicitIdIndex() throws Exception {
        Node root = session().getRootNode();
        Node newNode1 = root.addNode("nodeA");
        newNode1.setProperty("foo", "X");
        newNode1.addMixin("mix:referenceable");
        Node newNode2 = root.addNode("nodeB");
        newNode2.setProperty("foo", "Y");
        session().save();

        // print = true;

        // Compute a query plan that should use this index ...
        final String uuid = newNode1.getIdentifier();
        Query query = jcrSql2Query("SELECT [jcr:path] FROM [nt:unstructured] WHERE [jcr:uuid] = '" + uuid + "'");
        validateQuery().rowCount(1L).useIndex(IndexPlanners.NODE_BY_ID_INDEX_NAME).validate(query, query.execute());

        query = jcrSql2Query("SELECT A.* FROM [nt:unstructured] AS A WHERE A.[jcr:uuid] = $uuidValue");
        query.bindValue("uuidValue", valueFactory().createValue(uuid));
        validateQuery().rowCount(1L).useIndex(IndexPlanners.NODE_BY_ID_INDEX_NAME).validate(query, query.execute());
    }

    @FixFor( "MODE-2312" )
    @Test
    public void shouldUseImplicitPathIndex() throws Exception {
        Node root = session().getRootNode();
        Node newNode1 = root.addNode("nodeA");
        newNode1.setProperty("foo", "X");
        newNode1.addMixin("mix:referenceable");
        Node newNode2 = root.addNode("nodeB");
        newNode2.setProperty("foo", "Y");
        session().save();

        // print = true;

        // Compute a query plan that should use this index ...
        final String pathValue = newNode1.getPath();
        Query query = jcrSql2Query("SELECT [jcr:path] FROM [nt:unstructured] WHERE [jcr:path] = '" + pathValue + "'");
        validateQuery().rowCount(1L).useIndex(IndexPlanners.NODE_BY_PATH_INDEX_NAME).validate(query, query.execute());

        query = jcrSql2Query("SELECT A.* FROM [nt:unstructured] AS A WHERE A.[jcr:path] = $pathValue");
        query.bindValue("pathValue", valueFactory().createValue(pathValue));
        validateQuery().rowCount(1L).useIndex(IndexPlanners.NODE_BY_PATH_INDEX_NAME).validate(query, query.execute());
    }

    @Test
    public void shouldUseSingleColumnStringIndexForQueryWithNoCriteriaOtherThanMixinViaFromClause() throws Exception {
        registerValueIndex("titleNodes", "mix:title", null, "*", "jcr:mixinTypes", PropertyType.NAME);

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

        waitForIndexes();
        session.save();
        waitForIndexes();

        // Compute a query plan that should use this index ...
        Query query = jcrSql2Query("SELECT * FROM [mix:title]");
        validateQuery().rowCount(2L).useIndex("titleNodes").validate(query, query.execute());
    }

    @Test
    public void shouldUseSingleColumnNodeTypeIndexForQueryWithNoCriteriaOtherThanPrimaryTypeViaFromClause() throws Exception {
        registerNodeTypeIndex("primaryTypes", "nt:base", null, "*", "jcr:primaryType", PropertyType.STRING);
        registerNodeTypeIndex("mixinTypes", "nt:base", null, "*", "jcr:mixinTypes", PropertyType.STRING);

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

        waitForIndexes();
        session.save();
        waitForIndexes();

        // Compute a query plan that should use this index ...
        Query query = jcrSql2Query("SELECT * FROM [mix:title]");
        validateQuery().rowCount(2L).useIndex("mixinTypes").validate(query, query.execute());

        // Compute a query plan that should use this index ...
        query = jcrSql2Query("SELECT * FROM [nt:unstructured]");
        validateQuery().rowCount(3L).useIndex("primaryTypes").validate(query, query.execute());
    }

    @FixFor( "MODE-2297" )
    @Test
    public void shouldExecuteQueryUsingSetOperationOfQueriesWithJoins() throws Exception {
        registerNodeType("nt:formInstVersion");
        registerNodeType("nt:formInst");

        Node root = session().getRootNode();
        Node baseNode = root.addNode("formInst", "nt:formInst");
        baseNode.addMixin("mix:referenceable");

        Node v1Node = baseNode.addNode("version", "nt:formInstVersion");
        v1Node.addMixin("mix:referenceable");

        Node v2Node = baseNode.addNode("version", "nt:formInstVersion");
        v2Node.addMixin("mix:referenceable");
        v2Node.setProperty("previous_version", v1Node.getIdentifier());

        // waitForIndexes();
        session.save();
        waitForIndexes();

        // print = true;
        String sql1 = "SELECT BASE.* from [nt:formInstVersion] as BASE " //
                      + "JOIN  [nt:formInst] AS FORMINST ON ISCHILDNODE(BASE,FORMINST)";
        Query query = session.getWorkspace().getQueryManager().createQuery(sql1, Query.JCR_SQL2);
        validateQuery().rowCount(2).validate(query, query.execute());

        String sql2 = "SELECT BASE.* from [nt:formInstVersion] as BASE " //
                      + "JOIN  [nt:formInst] AS FORMINST ON ISCHILDNODE(BASE,FORMINST) " //
                      + "JOIN  [nt:formInstVersion] AS FORMINSTNEXT ON FORMINSTNEXT.previous_version = BASE.[jcr:uuid]";
        query = session.getWorkspace().getQueryManager().createQuery(sql2, Query.JCR_SQL2);
        validateQuery().rowCount(1).validate(query, query.execute());

        String sql = sql1 + " UNION " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(2).validate(query, query.execute());

        sql = sql2 + " UNION " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(2).validate(query, query.execute());

        sql = sql1 + " INTERSECT " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(1).validate(query, query.execute());

        sql = sql2 + " INTERSECT " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(1).validate(query, query.execute());

        sql = sql1 + " EXCEPT " + sql2;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(1).validate(query, query.execute());

        sql = sql2 + " EXCEPT " + sql1;
        query = session.getWorkspace().getQueryManager().createQuery(sql, Query.JCR_SQL2);
        validateQuery().rowCount(0).validate(query, query.execute());
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

        waitForIndexes();
        session.save();
        waitForIndexes();

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

        waitForIndexes();
        session.save();
        waitForIndexes();

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
        validateQuery().rowCount(2L).validate(query, query.execute());
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

        waitForIndexes();
        session.save();
        waitForIndexes();

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

        waitForIndexes();
        session.save();
        waitForIndexes();

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

        waitForIndexes();
        session.save();
        waitForIndexes();

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

        waitForIndexes();
        session.save();
        waitForIndexes();

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

        waitForIndexes();
        session.save();
        waitForIndexes();

        // Issues a query that should NOT use this index because direct lookup by path is lower cost ...
        Query query = jcrSql2Query("SELECT * FROM [nt:unstructured] WHERE [jcr:path] = '/myFirstBook'");
        validateQuery().rowCount(1L).useIndex("NodeByPath").considerIndex("pathIndex").validate(query, query.execute());

        // Issues a query that should NOT use this index ...
        query = jcrSql2Query("SELECT * FROM [nt:unstructured] WHERE [jcr:path] LIKE '/my%Book'");
        validateQuery().rowCount(2L).useNoIndexes().validate(query, query.execute());

        // Issues some queries that should use this index ...
        query = jcrSql2Query("SELECT * FROM [nt:unstructured] WHERE [jcr:path] > '/mySecondBook'");
        validateQuery().rowCount(1L).useIndex("pathIndex").validate(query, query.execute());

        query = jcrSql2Query("SELECT * FROM [nt:unstructured] WHERE PATH() > '/mySecondBook'");
        validateQuery().rowCount(1L).useIndex("pathIndex").validate(query, query.execute());
    }

    @FixFor( "MODE-2290" )
    @Test
    public void shouldUseSingleColumnResidualPropertyIndexInQueryAgainstSameNodeType() throws Exception {
        registerValueIndex("pathIndex", "nt:unstructured", "Node path index", "*", "someProperty", PropertyType.STRING);
        registerValueIndex("titleIndex", "mix:title", "Title index", "*", "jcr:title", PropertyType.STRING);

        // print = true;

        // Add a node that uses this type ...
        Node book1 = session().getRootNode().addNode("myFirstBook");
        book1.addMixin("mix:title");
        book1.setProperty("jcr:title", "The Title");
        book1.setProperty("someProperty", "value1");

        Node book2 = session().getRootNode().addNode("mySecondBook");
        book2.addMixin("mix:title");
        book2.setProperty("jcr:title", "A Different Title");
        book2.setProperty("someProperty", "value2");

        Node other = book2.addNode("chapter");
        other.setProperty("propA", "a value for property A");
        other.setProperty("jcr:title", "The Title");
        other.setProperty("someProperty", "value1");

        waitForIndexes();
        session.save();
        waitForIndexes();

        // Issues some queries that should use this index ...
        Query query = jcrSql2Query("SELECT * FROM [nt:unstructured] WHERE someProperty = 'value1'");
        validateQuery().rowCount(2L).useIndex("pathIndex").validate(query, query.execute());

        query = jcrSql2Query("SELECT * FROM [nt:unstructured] WHERE someProperty = $value");
        query.bindValue("value", session().getValueFactory().createValue("value1"));
        validateQuery().rowCount(2L).useIndex("pathIndex").validate(query, query.execute());

        query = jcrSql2Query("SELECT table.* FROM [nt:unstructured] AS table WHERE table.someProperty = 'value1'");
        validateQuery().rowCount(2L).useIndex("pathIndex").validate(query, query.execute());

        query = jcrSql2Query("SELECT table.* FROM [nt:unstructured] AS table WHERE table.someProperty = $value");
        query.bindValue("value", session().getValueFactory().createValue("value1"));
        validateQuery().rowCount(2L).useIndex("pathIndex").validate(query, query.execute());

        query = jcrSql2Query("SELECT * FROM [mix:title] WHERE [jcr:title] = 'The Title'");
        validateQuery().rowCount(1L).useIndex("titleIndex").validate(query, query.execute());

        query = jcrSql2Query("SELECT title.* FROM [mix:title] as title WHERE title.[jcr:title] = 'The Title'");
        validateQuery().rowCount(1L).useIndex("titleIndex").validate(query, query.execute());
    }

    @FixFor( "MODE-2314" )
    @Test
    public void shouldIndexNodeAfterChange() throws Exception {
        // print = true;

        registerValueIndex("ref1", "nt:unstructured", "", null, "ref1", PropertyType.STRING);
        registerValueIndex("ref2", "nt:unstructured", "", null, "ref2", PropertyType.STRING);

        // Wait until all content has been indexed ...
        waitForIndexes(500L);

        Node newNode1 = session.getRootNode().addNode("nodeWithSysName", "nt:unstructured");
        session.save(); // THIS IS CAUSING the node not being indexed
        printMessage("Node Created ...");

        final String uuId1 = "cccccccccccccccccccccc-0000-1111-1234-123456789abcd";
        newNode1.setProperty("ref1", uuId1);
        newNode1.setProperty("ref2", uuId1);

        session.save();
        printMessage("Node updated ...");

        waitForIndexes();

        Query query = jcrSql2Query("SELECT A.ref1 FROM [nt:unstructured] AS A WHERE A.ref2 = $ref2");
        query.bindValue("ref2", session().getValueFactory().createValue(uuId1));
        validateQuery().rowCount(1L).useIndex("ref2").onEachRow(new ValidateQuery.Predicate() {

            @Override
            public void validate( int rowNumber,
                                  Row row ) throws RepositoryException {
                if (rowNumber == 1) {
                    assertThat(row.getValue("ref1").getString(), is(uuId1));
                }
            }
        }).validate(query, query.execute());
    }

    @FixFor( "MODE-2318" )
    @Test
    public void shouldNotReindexOnStartup() throws Exception {
        // print = true;
        registerValueIndex("ref1", "nt:unstructured", "", null, "ref1", PropertyType.STRING);
        registerValueIndex("ref2", "nt:unstructured", "", null, "ref2", PropertyType.STRING);

        waitForIndexes();

        Node newNode1 = session.getRootNode().addNode("nodeWithSysName", "nt:unstructured");
        // session1.save(); // THIS IS CAUSING the node not being indexed

        final String uuId1 = "cccccccccccccccccccccc-0000-1111-1234-123456789abcd";
        newNode1.setProperty("ref1", uuId1);
        newNode1.setProperty("ref2", uuId1);

        session.save();

        printMessage("Nodes Created ...");

        // Shutdown the repository and restart it ...
        stopRepository();
        printMessage("Stopped repository. Restarting ...");
        startRepositoryWithConfiguration(resource(CONFIG_FILE));
        printMessage("Repository restart complete");
    }

    @FixFor( "MODE-2292" )
    @Test
    public void shouldUseIndexesAfterRestarting() throws Exception {
        registerValueIndex("pathIndex", "nt:unstructured", "Node path index", "*", "someProperty", PropertyType.STRING);

        // print = true;

        // Add a node that uses this type ...
        Node book1 = session().getRootNode().addNode("myFirstBook");
        book1.addMixin("mix:title");
        book1.setProperty("jcr:title", "The Title");
        book1.setProperty("someProperty", "value1");

        Node book2 = session().getRootNode().addNode("mySecondBook");
        book2.addMixin("mix:title");
        book2.setProperty("jcr:title", "A Different Title");
        book2.setProperty("someProperty", "value2");

        Node other = book2.addNode("chapter");
        other.setProperty("propA", "a value for property A");
        other.setProperty("jcr:title", "The Title");
        other.setProperty("someProperty", "value1");

        waitForIndexes();
        session.save();
        waitForIndexes();

        // Issues some queries that should use this index ...
        final String queryStr = "SELECT * FROM [nt:unstructured] WHERE someProperty = 'value1'";
        Query query = jcrSql2Query(queryStr);
        validateQuery().rowCount(2L).useIndex("pathIndex").validate(query, query.execute());

        // Shutdown the repository and restart it ...
        stopRepository();
        printMessage("Stopped repository. Restarting ...");
        startRepositoryWithConfiguration(resource(CONFIG_FILE));
        printMessage("Repository restart complete");

        // Issues the same query and verify it uses an index...
        query = jcrSql2Query(queryStr);
        validateQuery().rowCount(2L).useIndex("pathIndex").validate(query, query.execute());
    }

    @FixFor( "MODE-2296" )
    @Test
    public void shouldUseIndexesEvenWhenLocalIndexDoesNotContainValueUsedInCriteria() throws Exception {
        registerValueIndex("pathIndex", "nt:unstructured", "Node path index", "*", "someProperty", PropertyType.STRING);

        // print = true;

        // Add a node that uses this type ...
        Node book1 = session().getRootNode().addNode("myFirstBook");
        book1.addMixin("mix:title");
        book1.setProperty("jcr:title", "The Title");
        book1.setProperty("someProperty", "value1");

        Node book2 = session().getRootNode().addNode("mySecondBook");
        book2.addMixin("mix:title");
        book2.setProperty("jcr:title", "A Different Title");
        book2.setProperty("someProperty", "value2");

        Node other = book2.addNode("chapter");
        other.setProperty("propA", "a value for property A");
        other.setProperty("jcr:title", "The Title");
        other.setProperty("someProperty", "value1");

        waitForIndexes();
        session.save();
        waitForIndexes();

        // Issues some queries that should use this index ...
        String queryStr = "SELECT * FROM [nt:unstructured] WHERE someProperty = 'non-existant'";
        Query query = jcrSql2Query(queryStr);
        validateQuery().rowCount(0L).useIndex("pathIndex").validate(query, query.execute());

        // Try with a join ...
        queryStr = "SELECT a.* FROM [nt:unstructured] AS a JOIN [nt:unstructured] AS b ON a.someProperty = b.someProperty WHERE b.someProperty = 'non-existant-value'";
        query = jcrSql2Query(queryStr);
        validateQuery().rowCount(0L).useIndex("pathIndex").validate(query, query.execute());
    }

    @FixFor( "MODE-2295" )
    @Test
    public void shouldUseIndexesCreatedOnSubtypeUsingColumnsFromResidualProperty()
        throws RepositoryException, InterruptedException {
        String typeName = "nt:someType2";
        registerNodeType(typeName);
        registerValueIndex("ntsome2sysname", typeName, null, "*", "sysName", PropertyType.STRING);

        waitForIndexes();

        Node newNode = session.getRootNode().addNode("SOMENODE", typeName);
        newNode.setProperty("sysName", "X");

        // BEFORE SAVING, issue a query that will NOT use the index. The query should not see the transient node ...
        String queryStr = "select BASE.* FROM [" + typeName + "] as BASE WHERE NAME(BASE) = 'SOMENODE'";
        Query query = jcrSql2Query(queryStr);
        validateQuery().rowCount(0L).useNoIndexes().validate(query, query.execute());

        // Now issue a query that will use the index. The query should not see the transient node...
        queryStr = "select BASE.* FROM [" + typeName + "] as BASE WHERE BASE.sysName='X'";
        query = jcrSql2Query(queryStr);
        validateQuery().rowCount(0L).useIndex("ntsome2sysname").validate(query, query.execute());

        // Save the transient data ...
        session.save();

        waitForIndexes();

        // Issue a query that will NOT use the index ...
        queryStr = "select BASE.* FROM [" + typeName + "] as BASE WHERE NAME(BASE) = 'SOMENODE'";
        query = jcrSql2Query(queryStr);
        validateQuery().rowCount(1L).useNoIndexes().validate(query, query.execute());

        // Now issue a query that will use the index.
        queryStr = "select BASE.* FROM [" + typeName + "] as BASE WHERE BASE.sysName='X'";
        query = jcrSql2Query(queryStr);
        validateQuery().rowCount(1L).useIndex("ntsome2sysname").validate(query, query.execute());

        registerValueIndex("ntusysname", "nt:unstructured", null, "*", "sysName", PropertyType.STRING);

        waitForIndexes();

        Node newNode2 = session.getRootNode().addNode("SOMENODE2", "nt:unstructured");
        newNode2.setProperty("sysName", "X");
        session.save();

        waitForIndexes();

        // print = true;

        queryStr = "select BASE.* FROM [nt:unstructured] as BASE WHERE BASE.sysName='X'";
        query = jcrSql2Query(queryStr);
        validateQuery().rowCount(2L).validate(query, query.execute());
    }

    @FixFor( "MODE-2313" )
    @Test
    public void shouldAllowAddingIndexWhileSessionsAreQuerying() throws Exception {
        registerValueIndex("titleNodes", "mix:title", null, "*", "jcr:mixinTypes", PropertyType.NAME);

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

        waitForIndexes();
        session.save();
        waitForIndexes();

        for (int i = 0; i != 5; ++i) {
            // Compute a query plan that should use this index ...
            Query query = jcrSql2Query("SELECT * FROM [mix:title]");
            for (int j = 0; j != 5; ++j) {
                validateQuery().rowCount(2L).useIndex("titleNodes").validate(query, query.execute());
            }
        }

        final int numThreads = 10;
        final int numQueriesEachThread = 100;
        final int numIndexes = 4;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch stopLatch = new CountDownLatch(numThreads);
        final Runnable queryRunner = new Runnable() {

            @Override
            public void run() {
                JcrSession session = null;
                try {
                    session = repository().login();
                    Query query = jcrSql2Query(session, "SELECT * FROM [mix:title]");
                    startLatch.await();
                    for (int i = 0; i != numQueriesEachThread; ++i) {
                        // Compute a query plan that should use this index, UNLESS the index is currently undergoing rebuilding...
                        validateQuery()./*rowCount(2L).useIndex("titleNodes")*/validate(query, query.execute());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                } finally {
                    printMessage("Completing thread");
                    if (session != null) session.logout();
                    stopLatch.countDown();
                }
            }
        };

        // Start the threads; they'll each wait just before they execute their queries ...
        for (int i = 0; i != numThreads; ++i) {
            Thread thread = new Thread(queryRunner);
            thread.start();
        }

        // Release the latch so everything starts ...
        startLatch.countDown();

        // and then create several indexes ...
        for (int i = 0; i != numIndexes; ++i) {
            registerValueIndex("extraIndex" + i, "nt:file", null, "*", "jcr:lastModified", PropertyType.DATE);
        }

        // Wait for the threads to complete ...
        stopLatch.await();
    }

    private ValueFactory valueFactory() throws RepositoryException {
        return session.getValueFactory();
    }

    private void registerNodeType( String typeName ) throws RepositoryException {
        NodeTypeManager mgr = session.getWorkspace().getNodeTypeManager();

        // Create a template for the node type ...
        NodeTypeTemplate type = mgr.createNodeTypeTemplate();
        type.setName(typeName);
        type.setDeclaredSuperTypeNames(new String[] {"nt:unstructured"});
        type.setAbstract(false);
        type.setOrderableChildNodes(true);
        type.setMixin(false);
        type.setQueryable(true);
        mgr.registerNodeType(type, true);
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

    protected void registerNodeTypeIndex( String indexName,
                                          String indexedNodeType,
                                          String desc,
                                          String workspaceNamePattern,
                                          String propertyName,
                                          int propertyType ) throws RepositoryException {
        registerIndex(indexName, IndexKind.NODE_TYPE, PROVIDER_NAME, indexedNodeType, desc, workspaceNamePattern, propertyName,
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
        template.setSynchronous(useSynchronousIndexes());
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
        return jcrSql2Query(session(), expr);
    }

    protected Query jcrSql2Query( JcrSession session,
                                  String expr ) throws RepositoryException {
        return session.getWorkspace().getQueryManager().createQuery(expr, Query.JCR_SQL2);
    }

    protected ValidationBuilder validateQuery() {
        return ValidateQuery.validateQuery().printDetail(print);
    }

    protected boolean useSynchronousIndexes() {
        return true;
    }

    protected void waitForIndexes( long extraTime ) throws InterruptedException {
        if (useSynchronousIndexes()) {
            Thread.sleep(100L);
        } else {
            Thread.sleep(1000L);
        }
        if (extraTime > 0L) Thread.sleep(extraTime);
    }

    protected void waitForIndexes() throws InterruptedException {
        waitForIndexes(0L);
    }

}
