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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
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
import org.modeshape.jcr.api.query.QueryManager;
import org.modeshape.jcr.api.query.QueryResult;
import org.modeshape.jcr.spi.index.provider.IndexProvider;

public abstract class AbstractIndexProviderTest extends SingleUseAbstractTest {

    private static final String CONFIG_FILE = "config/repo-config-persistent-local-provider-no-indexes.json";
    private static final String STORAGE_DIR = "target/persistent_repository";


    @Before
    @Override
    public void beforeEach() throws Exception {
        // We're using a Repository configuration that persists content, so clean it up ...
        FileUtil.delete(storageDir());

        // Now start the repository ...
        startRepositoryWithConfiguration(repositoryConfiguration());
        printMessage("Started repository...");
    }

    protected InputStream repositoryConfiguration() {
        return resource(CONFIG_FILE);
    }

    protected String storageDir() {
        return STORAGE_DIR;
    }

    @After
    @Override
    public void afterEach() throws Exception {
        super.afterEach();
        FileUtil.delete(storageDir());
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
    @FixFor( "MODE-2473" )
    public void shouldSkipEntireBatches() throws Exception {
        registerNodeTypes("cnd/authors.cnd");
        registerValueIndex("baseNodes", "nt:base", null, "*", "jcr:primaryType", PropertyType.NAME);

        int batchSize = batchSize() + 1;
        for (int i = 0; i < batchSize; i++) {
            Node node = session.getRootNode().addNode("node_" + i, "my:content");
            node.setProperty("content", "content_" + i);
            node.setProperty("author", "author_" + i);
        }
        Node node = session.getRootNode().addNode("lastNode", "my:content");
        node.setProperty("content", "lastContent");
        node.setProperty("author", "lastAuthor");

        session.save();
        waitForIndexes();
        int totalNodesCount = batchSize + 1;
        NodeIterator nodeIterator = null;
        int[] positionsToSkip = new int[] {batchSize - 2, batchSize - 1, batchSize};
        List<Node> collectedNodes = new ArrayList<>();
        for (int positionToSkip : positionsToSkip ) {
            collectedNodes.clear();

            final String sql = "select content.[jcr:path] from [my:content] as content order by content.author";
            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            final Query query = queryManager.createQuery(sql, Query.JCR_SQL2);
            final QueryResult result = query.execute();

            nodeIterator = result.getNodes();
            assertEquals(batchSize + 1, nodeIterator.getSize());
            nodeIterator.skip(positionToSkip);
            while (nodeIterator.hasNext()) {
                collectedNodes.add(nodeIterator.nextNode());

            }
            assertEquals(collectedNodes.size(), totalNodesCount - positionToSkip);
        }

        // the last skip should produce just 1 node
        Node lastNode = collectedNodes.get(collectedNodes.size() - 1);
        assertEquals("lastContent", lastNode.getProperty("content").getString());
        assertNotNull("lastAuthor", lastNode.getProperty("author").getString());
    }

    @Override
    protected void startRepository() throws Exception {
        startRepositoryWithConfiguration(repositoryConfiguration());
    }

    protected ValueFactory valueFactory() throws RepositoryException {
        return session.getValueFactory();
    }

    protected void registerNodeType( String typeName ) throws RepositoryException {
        registerNodeType(typeName, true, false, "nt:unstructured");
    }
    
    protected void registerNodeType( String typeName, boolean queryable, boolean mixin, String...declaredSuperTypes) throws RepositoryException {
        NodeTypeManager mgr = session.getWorkspace().getNodeTypeManager();

        // Create a template for the node type ...
        NodeTypeTemplate type = mgr.createNodeTypeTemplate();
        type.setName(typeName);
        type.setDeclaredSuperTypeNames(declaredSuperTypes);
        type.setAbstract(false);
        type.setOrderableChildNodes(true);
        type.setMixin(mixin);
        type.setQueryable(queryable);
        mgr.registerNodeType(type, true);
    }

    protected void registerValueIndex( String indexName,
                                       String indexedNodeType,
                                       String desc,
                                       String workspaceNamePattern,
                                       String propertyName,
                                       int propertyType ) throws RepositoryException {
        registerIndex(indexName, IndexKind.VALUE, providerName(), indexedNodeType, desc, workspaceNamePattern, propertyName,
                      propertyType);
    }

    protected void registerNodeTypeIndex( String indexName,
                                          String indexedNodeType,
                                          String desc,
                                          String workspaceNamePattern,
                                          String propertyName,
                                          int propertyType ) throws RepositoryException {
        registerIndex(indexName, IndexKind.NODE_TYPE, providerName(), indexedNodeType, desc, workspaceNamePattern, propertyName,
                      propertyType);

    }

    protected void registerTextIndex( String indexName,
                                      String indexedNodeType,
                                      String desc,
                                      String workspaceNamePattern,
                                      String propertyName,
                                      int propertyType ) throws RepositoryException {
        registerIndex(indexName, IndexKind.TEXT, providerName(), indexedNodeType, desc, workspaceNamePattern, propertyName,
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

    protected void registerValueIndex( String indexName,
                                       String indexedNodeType,
                                       String desc,
                                       String workspaceNamePattern,
                                       Map<String, Integer> properties ) throws RepositoryException {
        registerIndex(indexName, IndexKind.VALUE, providerName(), indexedNodeType, desc, workspaceNamePattern, properties);
    }
    
    protected void registerIndex( String indexName,
                                  IndexKind kind,
                                  String providerName,
                                  String indexedNodeType,
                                  String desc,
                                  String workspaceNamePattern,
                                  Map<String, Integer> properties) throws RepositoryException {
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

        List<IndexColumnDefinition> colDefs = new ArrayList<>(properties.size());
        for (Map.Entry<String, Integer> entry : properties.entrySet())  {
            colDefs.add(indexManager().createIndexColumnDefinitionTemplate().setPropertyName(entry.getKey())
                                      .setColumnType(entry.getValue()));            
        }
        template.setColumnDefinitions(colDefs);

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

    protected int batchSize() {
        return IndexProvider.DEFAULT_BATCH_SIZE;
    }

    protected abstract boolean useSynchronousIndexes();
 
    protected abstract String providerName();

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
