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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.query.Row;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.api.index.IndexManager;
import org.modeshape.jcr.api.query.Query;
import org.modeshape.jcr.query.engine.IndexPlanners;

/**
 * This test verifies that the local index provider works when the indexes are updated <em>synchronous</em>. See
 * {@link LocalIndexProviderAsynchronousTest} for verification of the asynchronous cases.
 *
 * @author Randall Hauch (rhauch@redhat.com)
 * @author Horia Chiorean (hchiorea@redhat.com)
 * 
 * @see LocalIndexProviderAsynchronousTest
 */
public class LocalIndexProviderTest extends AbstractIndexProviderTest {

    @Override
    protected boolean useSynchronousIndexes() {
        return true;
    }
    
    @Override
    protected String providerName() {
        return "local";
    }

    // ---------------------------------------------------------------
    // Override these so that we can easily run them via JUnit runner.
    // ---------------------------------------------------------------

    @Override
    @Test
    public void shouldAllowRegisteringNewIndexDefinitionWithSingleStringColumn() throws Exception {
        super.shouldAllowRegisteringNewIndexDefinitionWithSingleStringColumn();
    }

    @Override
    @Test
    public void shouldUseSingleColumnStringIndexInQueryAgainstSameNodeType() throws Exception {
        super.shouldUseSingleColumnStringIndexInQueryAgainstSameNodeType();
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

        session.save();

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
        
        Node root = session().getRootNode();
        Node newNode1 = root.addNode("nodeWithSysName", "nt:typeWithSysName");
        newNode1.setProperty("sysName", "X");
        newNode1.addMixin("mix:referenceable");
        Node newNode2 = root.addNode("nodeWithReference", "nt:typeWithReference");
        newNode2.setProperty("referenceId", newNode1.getIdentifier());
        session().save();

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
        validateQuery().rowCount(1L).considerIndexes("refIndex", "sysIndex").validate(query, query.execute());

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

        session.save();

        // Compute a query plan that should use this index ...
        Query query = jcrSql2Query("SELECT A.* FROM [nt:typeWithReference] AS A "
                                   + "JOIN [nt:typeWithSysName] AS B ON A.referenceId  = B.[jcr:uuid] " //
                                   + "WHERE B.sysName = $sysName");
        query.bindValue("sysName", valueFactory().createValue("X"));
        validateQuery().rowCount(1L).considerIndexes("sysIndex", "refIndex", "typesIndex").validate(query, query.execute());

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

        session.save();

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

        session.save();
        
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

        session.save();

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

        session.save();

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
        
        session.save();
        
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
        validateQuery().useNoIndexes().rowCount(3L).validate(query, query.execute());
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

        session.save();

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

        session.save();

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

        session.save();

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

        session.save();

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

        session.save();

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

        session.save();

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
        startRepository();
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

        session.save();

        // Issues some queries that should use this index ...
        final String queryStr = "SELECT * FROM [nt:unstructured] WHERE someProperty = 'value1'";
        Query query = jcrSql2Query(queryStr);
        validateQuery().rowCount(2L).useIndex("pathIndex").validate(query, query.execute());

        // Shutdown the repository and restart it ...
        stopRepository();
        printMessage("Stopped repository. Restarting ...");
        startRepository();
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

        session.save();

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
        registerValueIndex("nt_someType2", typeName, null, "*", "sysName", PropertyType.STRING);
        
        Node newNode = session.getRootNode().addNode("SOMENODE", typeName);
        newNode.setProperty("sysName", "X");

        // BEFORE SAVING, issue a query that will NOT use the index. The query should not see the transient node ...
        String queryStr = "select BASE.* FROM [" + typeName + "] as BASE WHERE NAME(BASE) = 'SOMENODE'";
        Query query = jcrSql2Query(queryStr);
        validateQuery().rowCount(0L).useNoIndexes().validate(query, query.execute());

        // Now issue a query that will use the index. The query should not see the transient node...
        queryStr = "select BASE.* FROM [" + typeName + "] as BASE WHERE BASE.sysName='X'";
        query = jcrSql2Query(queryStr);
        validateQuery().rowCount(0L).useIndex("nt_someType2").validate(query, query.execute());

        // Save the transient data ...
        session.save();

        // Issue a query that will NOT use the index ...
        queryStr = "select BASE.* FROM [" + typeName + "] as BASE WHERE NAME(BASE) = 'SOMENODE'";
        query = jcrSql2Query(queryStr);
        validateQuery().rowCount(1L).useNoIndexes().validate(query, query.execute());

        // Now issue a query that will use the index.
        queryStr = "select BASE.* FROM [" + typeName + "] as BASE WHERE BASE.sysName='X'";
        query = jcrSql2Query(queryStr);
        validateQuery().rowCount(1L).useIndex("nt_someType2").validate(query, query.execute());

        registerValueIndex("nt_unstructured", "nt:unstructured", null, "*", "sysName", PropertyType.STRING);

        Node newNode2 = session.getRootNode().addNode("SOMENODE2", "nt:unstructured");
        newNode2.setProperty("sysName", "X");
        session.save();

        // print = true;

        queryStr = "select BASE.* FROM [nt:unstructured] as BASE WHERE BASE.sysName='X'";
        query = jcrSql2Query(queryStr);
        validateQuery().useIndex("nt_unstructured").rowCount(2L).validate(query, query.execute());
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

        session.save();

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

    @FixFor( "MODE-2346" )
    @Test
    public void shouldUseImplicitIndexesWithLowerCardinalityOverExplicitIndexes() throws Exception {
        String explicitNodesById = "explicitNodesById";
        registerValueIndex(explicitNodesById, "nt:unstructured", "Nodes by id explicit index", "*", "jcr:uuid",
                           PropertyType.STRING);

        String explicitNodesByPath = "explicitNodesByPath";
        registerValueIndex(explicitNodesByPath, "nt:unstructured", "Nodes by path explicit index", "*", "jcr:path",
                           PropertyType.PATH);

        Node root = session().getRootNode();
        Node nodeA = root.addNode("nodeA");
        nodeA.addMixin("mix:referenceable");
        session().save();

        // print = true;

        // Compute a query plan that should use this index ...
        final String uuid = nodeA.getIdentifier();
        Query query = jcrSql2Query("SELECT [jcr:path] FROM [nt:unstructured] WHERE [jcr:uuid] = '" + uuid + "'");
        validateQuery()
                .rowCount(1L)
                .considerIndexes(IndexPlanners.NODE_BY_ID_INDEX_NAME, explicitNodesById)
                .useIndex(IndexPlanners.NODE_BY_ID_INDEX_NAME)
                .validate(query, query.execute());

        query = jcrSql2Query("SELECT [jcr:path] FROM [nt:unstructured] WHERE [jcr:path] = '/nodeA'");
        validateQuery()
                .rowCount(1L)
                .considerIndexes(IndexPlanners.NODE_BY_PATH_INDEX_NAME, explicitNodesByPath)
                .useIndex(IndexPlanners.NODE_BY_PATH_INDEX_NAME)
                .validate(query, query.execute());

    }

    @FixFor( "MODE-2346" )
    @Test
    public void shouldUseExplicitIndexesWithLowerCardinalityOverImplicitIndexes() throws Exception {
        String explicitIndex = "explicitIndex";
        registerValueIndex(explicitIndex, "nt:unstructured", "Foo index", "*", "foo", PropertyType.STRING);

        // wait a bit to make sure the index definitions have been updated
        
        Node root = session().getRootNode();
        Node nodeA = root.addNode("nodeA");
        Node nodeB = nodeA.addNode("nodeB");
        nodeB.setProperty("foo", "X");
        session().save();

        // print = true;

        // Compute a query plan that should use this index ...
        Query query = jcrSql2Query(
                "SELECT [jcr:path] FROM [nt:unstructured] AS node WHERE ISDESCENDANTNODE(node, '/nodeA') AND node.[foo]='X'");
        validateQuery()
                .rowCount(1L)
                .considerIndexes(IndexPlanners.DESCENDANTS_BY_PATH_INDEX_NAME, explicitIndex)
                .useIndex(explicitIndex)
                .validate(query, query.execute());
    }

    @Test
    @FixFor( "MODE-2355" )
    public void shouldUseNoIndexesWhenIndexDoesntApplyToBothSidesOfOR() throws Exception {
        registerNodeTypes("cnd/authors.cnd");
        registerValueIndex("index1", "my:authored", "Authors index", "*", "author", PropertyType.STRING);
        registerValueIndex("index2", "my:authored", "Coauthors index", "*", "coAuthors", PropertyType.STRING);

        Node root = session.getRootNode();

        Node book1 = root.addNode("book1");
        book1.setPrimaryType("my:content");
        book1.setProperty("content", "book content 1");
        book1.setProperty("author", "author1");
        book1.setProperty("coAuthors", new String[] { "author2", "author3" });

        Node book2 = root.addNode("book2");
        book2.setPrimaryType("my:content");
        book2.setProperty("content", "book content 2");
        book2.setProperty("author", "author2");

        session.save();

        Query query = jcrSql2Query("select * from [my:content] where author in ($author) or coAuthors in ($author)");
        query.bindValue("author", session.getValueFactory().createValue("author1"));
        validateQuery()
                .useNoIndexes()
                .rowCount(1)
                .hasNodesAtPaths("/book1")
                .validate(query, query.execute());

        query.bindValue("author", session.getValueFactory().createValue("author2"));
        final List<String> expectedPaths = new ArrayList<>(Arrays.asList("/book1", "/book2"));
        validateQuery()
                .useNoIndexes()
                .rowCount(2)
                .onEachRow(new ValidateQuery.Predicate() {
                    @Override
                    public void validate( int rowNumber, Row row ) throws RepositoryException {
                        assertTrue("Path not found", expectedPaths.remove(row.getPath()));
                    }
                })
                .validate(query, query.execute());
        assertTrue("Not all paths found: " + expectedPaths, expectedPaths.isEmpty());
    }

    @FixFor( "MODE-2401" )
    @Test
    public void shouldNotConsiderNonQueryableNodeTypes() throws RepositoryException, InterruptedException {
        String typeName = "nt:nonQueryableFolder";
        registerNodeType(typeName, false, false, "nt:folder");
        registerNodeTypeIndex("typesIndex", "nt:folder", null, "*", "jcr:primaryType", PropertyType.STRING);

        session.getRootNode().addNode("nonQueryableFolder", typeName);
        session.getRootNode().addNode("regularFolder1", "nt:folder");
        Node folder2 = session.getRootNode().addNode("regularFolder2", typeName);
        folder2.addNode("subFolder", "nt:folder");
        session.save();

        final List<String> expectedResults = new ArrayList<>(Arrays.asList("/regularFolder1", "/regularFolder2/subFolder"));
        Query query = jcrSql2Query("select folder.[jcr:name] FROM [nt:folder] as folder");
        validateQuery()
                .rowCount(2L)
                .useIndex("typesIndex")
                .onEachRow(new ValidateQuery.Predicate() {
                    @Override
                    public void validate( int rowNumber, Row row ) throws RepositoryException {
                        expectedResults.remove(row.getPath());
                    }
                })
                .validate(query, query.execute());
        assertTrue("Not all expected nodes found: " + expectedResults, expectedResults.isEmpty());
    }


    @FixFor( "MODE-2401" )
    @Test
    public void shouldNotConsiderNonQueryableMixins() throws RepositoryException, InterruptedException {
        String mixinName = "nt:nonQueryableFolderMixin";
        registerNodeType(mixinName, false, true);
        registerNodeTypeIndex("typesIndex", "nt:folder", null, "*", "jcr:primaryType", PropertyType.STRING);

        Node folder1 = session.getRootNode().addNode("folder1", "nt:folder");
        Node folder2 = session.getRootNode().addNode("folder2", "nt:folder");
        folder2.addMixin(mixinName);
        session.save();
        
        // test with the initial node config
        final List<String> expectedResults = new ArrayList<>(Collections.singletonList("/folder1"));
        Query query = jcrSql2Query("select folder.[jcr:name] FROM [nt:folder] as folder");
        validateQuery()
                .rowCount(1L)
                .useIndex("typesIndex")
                .onEachRow(new ValidateQuery.Predicate() {
                    @Override
                    public void validate( int rowNumber, Row row ) throws RepositoryException {
                        expectedResults.remove(row.getPath());
                    }
                })
                .validate(query, query.execute());
        assertTrue("Not all expected nodes found: " + expectedResults, expectedResults.isEmpty());
        
        // add the mixin on the 1st node and reindex
        folder1.addMixin(mixinName);
        session.save();
        session.getWorkspace().reindex("/");
        query = jcrSql2Query("select folder.[jcr:name] FROM [nt:folder] as folder");
        validateQuery()
                .rowCount(0L)
                .useIndex("typesIndex")
                .validate(query, query.execute());
        
        // remove both mixins and reindex
        folder1.removeMixin(mixinName);
        folder2.removeMixin(mixinName);
        session.save();
        session.getWorkspace().reindex("/");

        final List<String> expectedResults2 = new ArrayList<>(Arrays.asList("/folder1", "/folder2"));
        query = jcrSql2Query("select folder.[jcr:name] FROM [nt:folder] as folder");
        validateQuery()
                .rowCount(2L)
                .useIndex("typesIndex")
                .onEachRow(new ValidateQuery.Predicate() {
                    @Override
                    public void validate( int rowNumber, Row row ) throws RepositoryException {
                        expectedResults2.remove(row.getPath());
                    }
                })
                .validate(query, query.execute());
        assertTrue("Not all expected nodes found: " + expectedResults2, expectedResults2.isEmpty());
    }

    @FixFor( "MODE-2432 ")
    @Test
    public void shouldExposeManagedIndexStatuses() throws Exception {
        String indexName = "explicitIndex";
        registerValueIndex(indexName, "nt:unstructured", "Foo index", "*", "foo", PropertyType.STRING);
        
        assertEquals(IndexManager.IndexStatus.NON_EXISTENT, indexManager().getIndexStatus("unknown", indexName, "default"));
        assertEquals(IndexManager.IndexStatus.NON_EXISTENT, indexManager().getIndexStatus(providerName(), "invalid_name", "default"));
        assertEquals(IndexManager.IndexStatus.NON_EXISTENT, indexManager().getIndexStatus(providerName(), indexName, "invalid_ws"));

        assertEquals(IndexManager.IndexStatus.ENABLED, indexManager().getIndexStatus(providerName(), indexName, "default")); 
        int nodeCount = 100;
        for (int i = 0; i < nodeCount; i++) {
            Node node = session.getRootNode().addNode("node_" + i);
            node.setProperty("foo", UUID.randomUUID().toString());
        }
        session.save();
        assertEquals(IndexManager.IndexStatus.ENABLED, indexManager().getIndexStatus(providerName(), indexName, "default"));
        Future<Boolean> reindexingResult = session.getWorkspace().reindexAsync();
        Thread.sleep(10);
        if (!reindexingResult.isDone()) {
            assertEquals(IndexManager.IndexStatus.REINDEXING, indexManager().getIndexStatus(providerName(), indexName, "default"));
        }
        assertEquals(true, reindexingResult.get());
        assertEquals(IndexManager.IndexStatus.ENABLED, indexManager().getIndexStatus(providerName(), indexName, "default"));
        
        indexManager().unregisterIndexes(indexName);
        // removing the actual index is async (event based)
        Thread.sleep(100);
        assertEquals(IndexManager.IndexStatus.NON_EXISTENT, indexManager().getIndexStatus(providerName(), indexName, "default"));
    }
    
    @Test
    @FixFor( "MODE-2432")
    public void shouldReturnIndexesWithACertainStatus() throws Exception {
        registerValueIndex("index1", "nt:unstructured", "Foo index", "*", "foo", PropertyType.STRING);
        registerValueIndex("index2", "nt:unstructured", "Bar index", "*", "bar", PropertyType.STRING);

        assertEquals(Arrays.asList("index1", "index2"), indexManager().getIndexNames(providerName(), "default",
                                                                                     IndexManager.IndexStatus.ENABLED));
        assertTrue(indexManager().getIndexNames(providerName(), "default", IndexManager.IndexStatus.REINDEXING).isEmpty());
        assertTrue(indexManager().getIndexNames("missing", "default", IndexManager.IndexStatus.ENABLED).isEmpty());
        assertTrue(indexManager().getIndexNames(providerName(), "missing", IndexManager.IndexStatus.ENABLED).isEmpty());
    }

    @Test
    @FixFor( "MODE-2498")
    public void shouldSelectCorrectIndexWhenMultipleIndexesUseTheSameAncestorProperty() throws Exception {
        registerNodeType("mix:custom", true, true, "mix:title");
        registerNodeType("mix:custom2", true, true, "mix:title");
        registerValueIndex("custom_names", "mix:custom", null, "*", "jcr:name", PropertyType.NAME);
        registerValueIndex("custom2_names", "mix:custom2", null, "*", "jcr:name", PropertyType.NAME);

        //print = true;

        // Add a node that uses this type ...
        Node root = session().getRootNode();
        Node book1 = root.addNode("myFirstBook");
        book1.addMixin("mix:custom");
        book1.setProperty("jcr:title", "The Title");

        session.save();
        
        // Compute a query plan that should use this index ...
        Query query = jcrSql2Query("SELECT * FROM [mix:custom] as custom where custom.[jcr:name] = 'myFirstBook'");
        validateQuery().rowCount(1L).useIndex("custom_names").validate(query, query.execute());
    }

    @Test
    public void shouldSelectIndexWhenMultipleAndedConstraintsApply() throws Exception {
        registerValueIndex("longValues", "nt:unstructured", "Long values index", "*", "value", PropertyType.LONG);

        Node root = session().getRootNode();
        int valuesCount = 5;
        for (int i = 0; i < valuesCount; i++) {
            String name = String.valueOf(i+1);
            Node node = root.addNode(name);
            node.setProperty("value", (long) (i+1));
        }
        session.save();

        String sql1 = "SELECT number.[jcr:name] FROM [nt:unstructured] as number WHERE (number.value > 1 AND number.value < 3) OR " +
                      "(number.value > 3 AND number.value < 5)";
        String sql2 = "SELECT number.[jcr:name] FROM [nt:unstructured] as number WHERE number.value <2";
        Query query = jcrSql2Query(sql1 + " UNION " + sql2);         
        validateQuery()
                .rowCount(2L)
                .useIndex("longValues")
                .hasNodesAtPaths("/2", "/4", "/1")
                .validate(query, query.execute());
    }
}
