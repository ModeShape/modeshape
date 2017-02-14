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
package org.modeshape.jcr.index.lucene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.LocalIndexProviderTest;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.index.IndexDefinition;
import org.modeshape.jcr.api.index.InvalidIndexDefinitionException;
import org.modeshape.jcr.api.query.Query;

/**
 * Unit test for {@link LuceneIndexProvider}. Since this is a local provider in term of repository locality, we want to run
 * at least the same tests as for {@link org.modeshape.jcr.index.local.LocalIndexProvider}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class LuceneIndexProviderTest extends LocalIndexProviderTest {

    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        tools = new JcrTools();
    }

    @Override
    protected InputStream repositoryConfiguration() {
        return resource("config/repo-config-persistent-lucene-provider-no-indexes.json");
    }

    @Override
    protected String providerName() {
        return "lucene";
    }
    
    @Test
    @FixFor( "MODE-2520" )
    public void shouldUseMultiColumnIndex() throws Exception {
        registerNodeType("nt:testType");
        Map<String, Integer> properties = new HashMap<>();
        properties.put("stringProp", PropertyType.STRING);
        properties.put("longProp", PropertyType.LONG);
        properties.put("jcr:name", PropertyType.NAME);
        registerValueIndex("multiColIndex", "nt:testType", null, "*", properties);
        
        Node root = session().getRootNode();
        Node node1 = root.addNode("node1", "nt:testType");
        node1.setProperty("stringProp", "string1");

        Node node2 = root.addNode("node2", "nt:testType");
        node2.setProperty("longProp", 1);
        
        Node node3 = root.addNode("node3", "nt:testType");
        node3.setProperty("stringProp", "string3");
        node3.setProperty("longProp", 2);
        
        session.save();

        Query query = jcrSql2Query("SELECT * FROM [nt:testType] WHERE stringProp LIKE 'string%'");
        validateQuery().rowCount(2L).hasNodesAtPaths("/node1", "/node3").useIndex("multiColIndex").validate(query, query.execute());
        
        query = jcrSql2Query("SELECT * FROM [nt:testType] WHERE longProp >= 1");
        validateQuery().rowCount(2L).hasNodesAtPaths("/node2", "/node3").useIndex("multiColIndex").validate(query, query.execute());        

        query = jcrSql2Query("SELECT * FROM [nt:testType] WHERE longProp > 1 AND stringProp LIKE 'string%' ");
        validateQuery().rowCount(1L).hasNodesAtPaths("/node3").useIndex("multiColIndex").validate(query, query.execute());        

        query = jcrSql2Query("SELECT * FROM [nt:testType] WHERE NAME() LIKE 'node%' ");
        validateQuery().rowCount(3L).hasNodesAtPaths("/node1", "/node2", "/node3").useIndex("multiColIndex").validate(query, query.execute());        
    }
    
    @Test(expected = InvalidIndexDefinitionException.class)
    public void shouldNotAllowMultiColumnTextIndex() throws  Exception {
        Map<String, Integer> properties = new HashMap<>();
        properties.put("prop1", PropertyType.STRING);
        properties.put("prop2", PropertyType.STRING);
        registerIndex("multiColTextIndex", IndexDefinition.IndexKind.TEXT, providerName(), "nt:unstructured", null, "*", properties);
    }
    
    @Test
    public void shouldUseIndexForFTSOnStringProperty() throws Exception {
        registerNodeType("nt:testType");
        registerTextIndex("textIndex", "nt:testType", null, "*", "FTSProp", PropertyType.STRING);

        Node root = session().getRootNode();
        Node node1 = root.addNode("node", "nt:testType");
        String propertyText = "the quick Brown fox jumps over to the dog in at the gate";
        node1.setProperty("FTSProp", propertyText);
        session.save();


        Query query = jcrSql2Query("select [jcr:path] from [nt:testType] as n where contains([nt:testType].*,'" + propertyText + "')");
        validateQuery().rowCount(1L).hasNodesAtPaths("/node").useIndex("textIndex").validate(query, query.execute());

        jcrSql2Query("select [jcr:path] from [nt:testType] as n where contains(FTSProp,'" + propertyText + "')");
        validateQuery().rowCount(1L).hasNodesAtPaths("/node").useIndex("textIndex").validate(query, query.execute());

        jcrSql2Query("select [jcr:path] from [nt:testType] as n where contains(FTSProp,'" + propertyText.toUpperCase() + "')");
        validateQuery().rowCount(1L).hasNodesAtPaths("/node").useIndex("textIndex").validate(query, query.execute());

        jcrSql2Query("select [jcr:path] from [nt:testType] as n where contains(FTSProp,'the quick Dog')");
        validateQuery().rowCount(1L).hasNodesAtPaths("/node").useIndex("textIndex").validate(query, query.execute());

        jcrSql2Query("select [jcr:path] from [nt:testType] as n where contains(n.*,'the quick Dog')");
        validateQuery().rowCount(1L).hasNodesAtPaths("/node").useIndex("textIndex").validate(query, query.execute());

        jcrSql2Query("select [jcr:path] from [nt:testType] as n where contains(FTSProp,'the quick jumps over gate')");
        validateQuery().rowCount(1L).hasNodesAtPaths("/node").useIndex("textIndex").validate(query, query.execute());

        jcrSql2Query("select [jcr:path] from [nt:testType] as n where contains(n.*,'the quick jumps over gate')");
        validateQuery().rowCount(1L).hasNodesAtPaths("/node").useIndex("textIndex").validate(query, query.execute());

        jcrSql2Query("select [jcr:path] from [nt:testType] as n where contains(FTSProp,'the gate')");
        validateQuery().rowCount(1L).hasNodesAtPaths("/node").useIndex("textIndex").validate(query, query.execute());

        jcrSql2Query("select [jcr:path] from [nt:testType] as n where contains(n.*,'the gate')");
        validateQuery().rowCount(1L).hasNodesAtPaths("/node").useIndex("textIndex").validate(query, query.execute());
    }   
    
    @Test
    public void shouldUseIndexForFTSOnBinaryProperty() throws Exception {
        registerTextIndex("textIndex", "nt:resource", null, "*", "jcr:data", PropertyType.BINARY);

        tools.uploadFile(session, "/node", resourceStream("text-file.txt"));
        session.save();
       
        Query query = jcrSql2Query("select [jcr:path] from [nt:resource] as n where contains(n.*,'the quick jumps')");
        validateQuery().rowCount(1L).hasNodesAtPaths("/node/jcr:content").useIndex("textIndex").validate(query, query.execute());
    }

    @FixFor("MODE-2565")
    @Override
    @Test
    public void shouldNotReindexOnStartup() throws Exception {
        super.shouldNotReindexOnStartup();
    }
    
    @Test
    @Ignore("perf test")
    public void testPerformanceOnStringFields() throws Exception {
        registerNodeType("nt:testType");
        registerValueIndex("stringIndex", "nt:testType", null, "*", "stringProp",
                           PropertyType.STRING);
    
    
        Node rootNode = session.getRootNode();
        Node containerNode = rootNode.addNode("container", "nt:unstructured");
    
        int numNodes = 1000;
        long start = System.currentTimeMillis();
        for (int i = 0; i < numNodes; i++) {
            Node node = containerNode.addNode("node-" + i, "nt:testType");
            node.setProperty("id", i);
            node.setProperty("stringProp", "str" + i);
        }
        session.save();
        System.out.printf("Inserted %d in %d ms.\n", numNodes, System.currentTimeMillis() - start);
    
        Random random = new Random();
    
        start = System.currentTimeMillis();
        int numSearches = 1000;
        for (int i = 0; i < numSearches; i++) {
            int findId = random.nextInt(numNodes);
            String findStr = "str" + findId;
            String queryStr = "select test.[id] from [nt:testType] as test where test.stringProp = '" + findStr + "'";
            Query query = jcrSql2Query(session, queryStr);
            validateQuery().useIndex("stringIndex").hasNodesAtPaths("/container/node-" + findId).validate(query, query.execute());
        }
        System.out.printf("Searched %d nodes %d times in %d ms.\n", numNodes, numSearches, System.currentTimeMillis() - start);
        containerNode.remove();
        session.save();
    }

    @Override
    protected void assertStorageLocationUnchangedAfterRestart() throws Exception {
        // register the total size and last modified timestamp of the place where indexes are stored for the default provider..
        File indexesDir1 = new File("target/persistent_repository/indexes/lucene_primary/default/ref1");
        assertTrue(indexesDir1.exists() && indexesDir1.isDirectory() && indexesDir1.canRead());
        long sizeDir1 = FileUtil.size(indexesDir1.getPath());
        final AtomicLong lastModifiedDateDir1 = lastModifiedFileTime(indexesDir1, "_0.*");

        File indexesDir2 = new File("target/persistent_repository/indexes/lucene_primary/default/ref2");
        assertTrue(indexesDir2.exists() && indexesDir2.isDirectory() && indexesDir2.canRead());
        long sizeDir2 = FileUtil.size(indexesDir1.getPath());
        final AtomicLong lastModifiedDateDir2 = lastModifiedFileTime(indexesDir2, "_0.*");

        startRepository();
        printMessage("Repository restart complete");

        // and now check that the storage folder is unchanged
        assertEquals(sizeDir1, FileUtil.size(indexesDir1.getPath()));
        assertEquals(lastModifiedDateDir1.get(), lastModifiedFileTime(indexesDir1, "_0.*").get());
        assertEquals(sizeDir2, FileUtil.size(indexesDir2.getPath()));
        assertEquals(lastModifiedDateDir2.get(), lastModifiedFileTime(indexesDir2, "_0.*").get());
    }
}
