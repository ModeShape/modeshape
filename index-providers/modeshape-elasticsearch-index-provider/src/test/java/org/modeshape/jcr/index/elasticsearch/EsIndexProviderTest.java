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
package org.modeshape.jcr.index.elasticsearch;

import java.io.InputStream;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.NodeBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.EsValidateQuery;
import org.modeshape.jcr.LocalIndexProviderTest;
import org.modeshape.jcr.ValidateQuery;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.query.Query;

/**
 * Unit test for {@link EsIndexProvider}. 
 * 
 * @author kulikov
 */
public class EsIndexProviderTest extends LocalIndexProviderTest {

    private static org.elasticsearch.node.Node esNode;
    
    @BeforeClass
    public static void setUpClass() {
        FileUtil.delete("target/persistent_repository");
        FileUtil.delete("target/data");
        Settings localSettings = Settings.settingsBuilder()
                .put("http.enabled", true)
                .put("number_of_shards", 1)
                .put("number_of_replicas", 1)
                .put("path.home", "target/data")
                .build();
        //configure Elasticsearch node
        esNode =  NodeBuilder.nodeBuilder().settings(localSettings).local(false).build().start();

    }
    
    @AfterClass
    public static void tearDownClass() {
            esNode.close();
            FileUtil.delete("target/data");
    }
    
    @Override
    public void beforeEach() throws Exception {
        startRepositoryWithConfiguration(repositoryConfiguration());
        printMessage("Started repository...");
        tools = new JcrTools();
    }

    @Override
    public void afterEach() throws Exception {
        try {
            stopRepository();        
        } finally {
            FileUtil.delete("target/persistent_repository");
        }
    }
    
    @Override
    protected boolean startRepositoryAutomatically() {
        return false;
    }

    @Override
    protected InputStream repositoryConfiguration() {
        return resource("config/repo-config1.json");
    }

    @Override
    protected String providerName() {
        return "elasticsearch";
    }
    
    @Override
    protected ValidateQuery.ValidationBuilder validateQuery() { 
        return EsValidateQuery.validateQuery().printDetail(print);
    }
    
    @Test
    @Override
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
                .rowCount(3L)
                .useIndex("longValues")
                .hasNodesAtPaths("/2", "/4", "/1")
                .validate(query, query.execute());
    }
    
    @Test
    @Override
    public void shouldUseIndexesAfterRestarting() throws Exception {
        //This test case is excluded due to local ES node failre after
        //client disconnection
    }
    
}
