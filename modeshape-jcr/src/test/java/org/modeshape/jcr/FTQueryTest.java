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

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.junit.Test;

/**
 * A test case for more complicated query tests that require a fair amount of unique setup.
 */
public class FTQueryTest extends SingleUseAbstractTest {

    @Test
    public void shouldDemonstrateStrictMatching() throws Exception {
        // Register some namespaces ...
        NamespaceRegistry namespaceRegistry = session.getWorkspace().getNamespaceRegistry();
        namespaceRegistry.registerNamespace("sramp", "http://s-ramp.org/xmlns/2010/s-ramp");

        // Add some artifact nodes.
        session = repository.login();
        Node rootNode = session.getRootNode();

        // Node - Artifact A
        Node artifactA = rootNode.addNode("artifact-a", "nt:unstructured");
        artifactA.setProperty("sramp:title", "The class");

        session.save();
        session.logout();

        // Now it's time to do some querying.
        session = repository.login();

        String query = "class";
        assertJcrFTQuery(query, 1);

        query = "classes";
        assertJcrFTQuery(query, 0);

        query = "lass";
        assertJcrFTQuery(query, 0);

        query = "ass";
        assertJcrFTQuery(query, 0);

        session.logout();
    }
    
    protected QueryResult assertJcrFTQuery( String sql,
            long expectedRowCount ) throws RepositoryException {
        Query query = session().getWorkspace().getQueryManager().createQuery(sql, "search");
        QueryResult results = query.execute();
        printMessage(query.getStatement());
        ValidateQuery.validateQuery().printDetail(print).rowCount(expectedRowCount).validate(query, results);
        return results;
}

}
