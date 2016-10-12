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

        NamespaceRegistry namespaceRegistry = session.getWorkspace().getNamespaceRegistry();
        namespaceRegistry.registerNamespace("sramp", "http://s-ramp.org/xmlns/2010/s-ramp");

        session = repository.login();
        Node rootNode = session.getRootNode();

        Node artifactA = rootNode.addNode("artifact-a", "nt:unstructured");
        artifactA.setProperty("sramp:title", "foo");

        Node artifactB = rootNode.addNode("artifact-b", "nt:unstructured");
        String titles[] = {"foo", "bar"};
        artifactB.setProperty("sramp:title", titles);

        Node artifactC = rootNode.addNode("artifact-c", "nt:unstructured");
        artifactC.setProperty("sramp:name", "foo");

        session.save();
        session.logout();

        session = repository.login();

        //Let's test full-text search language
        String query = "foo";
        assertJcrFTQuery(query, 3);

        query = "bar";
        assertJcrFTQuery(query, 1);

        //Let's test equivalent JCR-SQL2 constructs
        query = "SELECT * from [nt:unstructured] as r where contains(r.*, 'foo')";
        assertJcrSql2Query(query, 3);

        query = "SELECT * from [nt:unstructured] as r where contains(r.*, 'bar')";
        assertJcrSql2Query(query, 1);

        //Let's try more specific full-text JCR-SQL2 constructs
        query = "SELECT * from [nt:unstructured] as r where contains(r.[sramp:name], 'foo')";
        assertJcrSql2Query(query, 1);

        query = "SELECT * from [nt:unstructured] as r where contains(r.[sramp:title], 'foo')";
        assertJcrSql2Query(query, 2);

        query = "SELECT * from [nt:unstructured] as r where contains(r.[sramp:title], 'bar')";
        assertJcrSql2Query(query, 1);

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
