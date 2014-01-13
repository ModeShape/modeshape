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

import java.io.File;
import javax.annotation.Resource;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.RepositoryConfiguration;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Arquillian test which verifies that a repository using a clustered configuration starts up.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@RunWith( Arquillian.class)
public class ClusteredConfigurationIntegrationTest {

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "clustered-repo-config-test.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"));
        // Add our custom Manifest, which has the additional Dependencies entry ...
        archive.setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
        return archive;
    }

    @Resource( mappedName = "java:/jcr/repo-clustered" )
    private JcrRepository clusteredRepository;

    @Before
    public void before() {
        assertNotNull(clusteredRepository);
    }

    @Test
    @FixFor({"MODE-1923", "MODE-1929"})
    public void clusteredRepositoryShouldHaveStartedUp() throws Exception {
        RepositoryConfiguration.Clustering clusteringConfiguration = clusteredRepository.getConfiguration().getClustering();
        assertEquals("modeshape-cluster", clusteringConfiguration.getClusterName());
        Assert.assertNotNull(clusteringConfiguration.getChannel());

        Session session = clusteredRepository.login();
        assertNotNull(session);
        session.logout();
    }

    @Test
    @FixFor( "MODE-1935" )
    public void shouldIndexNodesOnMaster() throws Exception {
        Session session = clusteredRepository.login();
        session.getRootNode().addNode("test");
        session.save();

        String queryString = "select * from [nt:unstructured] where [jcr:name] like '%test%'";
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        QueryResult result = queryManager.createQuery(queryString, Query.JCR_SQL2).execute();
        assertTrue(result.getNodes().getSize() > 0);
    }
}
