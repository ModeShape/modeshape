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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.UUID;
import javax.annotation.Resource;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.RepositoryConfiguration;

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

    @Resource( mappedName = "java:/jcr/repo-clustered1" )
    private JcrRepository clusteredRepository1;

    @Resource( mappedName = "java:/jcr/repo-clustered2" )
    private JcrRepository clusteredRepository2;

    @Resource( mappedName = "java:/jcr/repo-clustered3" )
    private JcrRepository clusteredRepository3;

    @Resource( mappedName = "java:/jcr/repo-clustered4" )
    private JcrRepository clusteredRepository4;

    @Before
    public void before() {
        assertNotNull(clusteredRepository1);
        assertNotNull(clusteredRepository2);
        assertNotNull(clusteredRepository3);
        assertNotNull(clusteredRepository4);
    }

    @Test
    @FixFor({"MODE-1923", "MODE-1929", "MODE-2226"})
    public void clusteredRepositoryShouldHaveStartedUpUsingExternalJGroupsConfigFile() throws Exception {
        checkRepoConfiguration(clusteredRepository1, "modeshape-wf-it1", true, true);
        checkRepoConfiguration(clusteredRepository2, "modeshape-wf-it1", true, true);
        checkRepoStarted(clusteredRepository1, clusteredRepository2);
    }
    
    @Test
    public void clusteredRepositoryShouldHaveStartedUsingInternalJGroupsConfig() throws Exception {
        checkRepoConfiguration(clusteredRepository3, "modeshape-wf-it2", false, true);
        checkRepoConfiguration(clusteredRepository4, "modeshape-wf-it2", false, true);
        checkRepoStarted(clusteredRepository3, clusteredRepository4);
    }
    
    private void checkRepoConfiguration(JcrRepository repository, String clusterName,
                                        boolean usesConfiguration,
                                        boolean usesDbLocking) {
        RepositoryConfiguration.Clustering clustering = repository.getConfiguration().getClustering();
        assertTrue(clustering.isEnabled());
        assertEquals(clusterName, clustering.getClusterName());
        String configuration = clustering.getConfiguration();
        if (usesConfiguration) {
            assertNotNull(configuration);
        } else {
            assertEquals(RepositoryConfiguration.Default.CLUSTER_CONFIG, configuration);
        }
        assertEquals(usesDbLocking, clustering.useDbLocking());
    }

    private void checkRepoStarted(JcrRepository firstRepo, JcrRepository secondRepo) throws RepositoryException {
        Session firstSession = firstRepo.login();
        assertNotNull(firstSession);
        String uuid = UUID.randomUUID().toString();
        firstSession.getRootNode().addNode(uuid);
        firstSession.save();
        firstSession.logout();
        
        Session secondSession = secondRepo.login();
        assertNotNull(secondSession.getNode("/" + uuid));
        secondSession.logout();
    }
}
