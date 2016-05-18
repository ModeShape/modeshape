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
package org.modeshape.jcr.index.elasticsearch.client;

import javax.jcr.PropertyType;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.index.elasticsearch.EsIndexColumn;
import org.modeshape.jcr.index.elasticsearch.EsIndexColumns;

/**
 *
 * @author kulikov
 */
public class EsClientTest {

    private final static ExecutionContext context = new ExecutionContext();

    private final static EsIndexColumn def1 = new EsIndexColumn(context, "field1", PropertyType.STRING);
    private final static EsIndexColumn def2 = new EsIndexColumn(context, "field2", PropertyType.DECIMAL);
    private final static EsIndexColumn def3 = new EsIndexColumn(context, "field3", PropertyType.STRING);
    private final static EsIndexColumn def4 = new EsIndexColumn(context, "mixinTypes", PropertyType.NAME);
    private final static EsIndexColumn def5 = new EsIndexColumn(context, "myfield", PropertyType.STRING);
    private final static EsIndexColumns columns = new EsIndexColumns(def1, def2, def3, def4, def5);
    
    private static Node esNode;
    private final EsClient client = new EsClient("localhost", 9200);
    
    private final static String INDEX_NAME = "test-index";
    private final static String TYPE_NAME = "test-type";
    
    public EsClientTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        FileUtil.delete("target/data");
        try {
        esNode = NodeBuilder.nodeBuilder()
                .local(false)
                .settings(Settings.settingsBuilder().put("path.home", "target/data"))
                .node();
        } catch (Exception t) {
        }
    }
    
    @AfterClass
    public static void tearDownClass() throws InterruptedException {
        Thread.sleep(1000);
        esNode.close();
        FileUtil.delete("target/data");
    }
    
    @Before
    public void setUp() throws Exception {
        client.createIndex(INDEX_NAME, TYPE_NAME, columns.mappings("test-type"));    
        client.flush(INDEX_NAME);
    }
    
    @After
    public void tearDown() throws Exception {
        client.deleteIndex(INDEX_NAME);
    }

    @Test
    public void testStoreAndGetDocument() throws Exception {
        EsRequest doc = new EsRequest();
        doc.put("field1", "value1");
        client.storeDocument(INDEX_NAME, TYPE_NAME, "1", doc);
        
        EsRequest doc1 = client.getDocument(INDEX_NAME, TYPE_NAME, "1");
        assertEquals(doc.get("field1"), doc1.get("field1"));
    }

    @Test
    public void testUpdateDocument() throws Exception {
        EsRequest doc = new EsRequest();
        doc.put("field1", "value1");
        client.storeDocument(INDEX_NAME, TYPE_NAME, "1", doc);
        
        EsRequest doc1 = client.getDocument(INDEX_NAME, TYPE_NAME, "1");
        doc1.put("field1", "VALUE1");
        client.storeDocument(INDEX_NAME, TYPE_NAME, "1", doc1);

        EsRequest doc2 = client.getDocument(INDEX_NAME, TYPE_NAME, "1");
        assertEquals("VALUE1", doc2.get("field1"));
    }

    @Test
    public void testStoreAndGetDocumentWithArrays() throws Exception {
        EsRequest doc = new EsRequest();
        doc.put("field1", new String[]{"1", "2", "3"});
        client.storeDocument(INDEX_NAME, TYPE_NAME, "1", doc);
        
        EsRequest doc1 = client.getDocument(INDEX_NAME, TYPE_NAME, "1");
        assertTrue(java.util.Arrays.equals((Object[])doc.get("field1"), (Object[])doc1.get("field1")));
    }

    @Test
    public void testUpdateDocumentWithArrays() throws Exception {
        EsRequest doc = new EsRequest();
        doc.put("field1", new String[]{"1", "2", "3"});
        client.storeDocument(INDEX_NAME, TYPE_NAME, "1", doc);
        
        EsRequest doc1 = client.getDocument(INDEX_NAME, TYPE_NAME, "1");
        doc1.put("field1", new String[]{"1", "2", "3", "4"});
        
        client.storeDocument(INDEX_NAME, TYPE_NAME, "1", doc1);

        EsRequest doc2 = client.getDocument(INDEX_NAME, TYPE_NAME, "1");
        
        assertEquals(4, ((Object[])doc2.get("field1")).length);
    }
    
    @Test
    public void shouldReturnNull() throws Exception {
        EsRequest doc1 = client.getDocument(INDEX_NAME, TYPE_NAME, "1");
        assertTrue(doc1 == null);
    }

    @Test
    public void testCount() throws Exception {
        EsRequest doc = new EsRequest();
        doc.put("field1", "value1");
        assertTrue(client.storeDocument(INDEX_NAME, TYPE_NAME, "10", doc));
        Thread.sleep(1500);
        long count = client.count(INDEX_NAME, TYPE_NAME);
        assertEquals(1, count);
    }
    
    
}