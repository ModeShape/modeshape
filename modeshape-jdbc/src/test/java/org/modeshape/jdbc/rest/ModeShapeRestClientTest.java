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
package org.modeshape.jdbc.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.jcr.query.Query;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration test which tests the behavior of {@link ModeShapeRestClient}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class ModeShapeRestClientTest {

    private static ModeShapeRestClient REST_CLIENT;

    @BeforeClass
    public static void beforeClass() {
        // the values are coming from the modeshape-rest-war test webapp
        REST_CLIENT = new ModeShapeRestClient("http://localhost:8090/modeshape/repo/default", "dnauser", "password");
    }

    @Test
    public void shouldGetRepositories() throws Exception {
        Repositories repositories = REST_CLIENT.getRepositories();
        assertNotNull(repositories);
        Collection<Repositories.Repository> repositoryList = repositories.getRepositories();
        assertEquals(1, repositoryList.size());
        Repositories.Repository repository = repositories.iterator().next();
        assertEquals("repo", repository.getName());
        Map<String, Object> metadata = repository.getMetadata();
        assertNotNull(metadata);
        assertFalse(metadata.isEmpty());
    }

    @Test
    public void shouldGetRepositoryByName() throws Exception {
        assertNull(REST_CLIENT.getRepository("foobar"));
        Repositories.Repository repository = REST_CLIENT.getRepository("repo");
        assertNotNull(repository);
    }

    @Test
    public void shouldGetWorkspacesForRepository() throws Exception {
        Workspaces workspaces = REST_CLIENT.getWorkspaces("repo");
        assertNotNull(workspaces);
        List<String> wsList = workspaces.getWorkspaces();
        assertEquals(1, wsList.size());
        assertEquals("default", wsList.get(0));
    }

    @Test
    public void shouldGetNodeTypesForRepository() throws Exception {
        NodeTypes nodeTypes = REST_CLIENT.getNodeTypes();
        assertFalse(nodeTypes.isEmpty());
        assertNotNull(nodeTypes.getNodeType("nt:base"));
        assertNotNull(nodeTypes.getNodeType("nt:unstructured"));
        assertNull(nodeTypes.getNodeType("foobar"));
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldQueryRepository() throws Exception {
        QueryResult queryResult = REST_CLIENT.query("SELECT node.[jcr:path] FROM [mode:root] AS node", Query.JCR_SQL2);
        assertQueryResult(queryResult);

        queryResult = REST_CLIENT.query("SELECT jcr:path FROM mode:root", Query.SQL);
        assertQueryResult(queryResult);

        queryResult = REST_CLIENT.query("//element(*, mode:root)", Query.XPATH);
        assertQueryResult(queryResult);
    }

    private void assertQueryResult( QueryResult queryResult ) {
        Map<String, String> columns = queryResult.getColumns();
        String pathColumn = columns.get("jcr:path");
        assertNotNull(pathColumn);
        assertEquals("string", pathColumn.toLowerCase());

        List<QueryResult.Row> rows = queryResult.getRows();
        assertEquals(1, rows.size());
        QueryResult.Row result = rows.get(0);
        assertEquals("/", result.getValue("jcr:path"));
    }

    @Test
    public void shouldGetQueryPlan() throws Exception {
        assertNotNull(REST_CLIENT.queryPlan("SELECT node.[jcr:path] FROM [mode:root] AS node", Query.JCR_SQL2));
    }
}
