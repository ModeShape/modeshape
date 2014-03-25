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
package org.modeshape.test.integration;

import static org.junit.Assert.assertTrue;
import java.io.File;
import javax.annotation.Resource;
import javax.jcr.Node;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.api.Session;

/**
 * Integration test that accesses the repository with disabled queries to verify the functionality still works, though executing
 * queries will fail.
 */
@RunWith( Arquillian.class )
public class NoQueriesIntegrationTest {

    @Resource( mappedName = "/jcr/noQueryRepository" )
    private JcrRepository repository;

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "noQueryRepository-test.war");
        // Add our custom Manifest, which has the additional Dependencies entry ...
        archive.setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
        return archive;
    }

    // TODO MODE-2178
    @Ignore
    @Test
    public void shouldStillBeAbleToAddNodes() throws Exception {
        Session session = repository.login();
        Node testNode = session.getRootNode().addNode("repos");
        session.save();
        session.logout();

        session = repository.login();
        Node testNode2 = session.getNode("/repos");
        assertTrue(testNode.isSame(testNode2));
        session.logout();

        // Queries return nothing ...
        session = repository.login();
        Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [nt:base]", Query.JCR_SQL2);
        QueryResult results = query.execute();
        assertTrue(results.getNodes().getSize() == 0);
        session.logout();
    }
}
