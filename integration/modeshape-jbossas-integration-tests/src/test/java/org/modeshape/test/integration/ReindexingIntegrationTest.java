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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import java.io.File;
import javax.annotation.Resource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.RepositoryConfiguration;

/**
 * Arquillian integration test that checks that various reindexing configurations (see standalone-modeshape.xml) work correctly.
 * 
 * @author Horia Chiorean
 */
@RunWith( Arquillian.class )
public class ReindexingIntegrationTest {

    @Resource( mappedName = "java:/jcr/reindexingRepository" )
    private JcrRepository reindexingRepository;

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "reindexingRepository-test.war");
        // Add our custom Manifest, which has the additional Dependencies entry ...
        archive.setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
        return archive;
    }

    @Before
    public void before() {
        assertNotNull("preconfiguredRepository repository not found", reindexingRepository);
    }
    
    @Test
    public void shouldAllowCustomReindexingConfiguration() throws Exception {
        // do a login to force the repo to start
        reindexingRepository.login();
        RepositoryConfiguration.Reindexing reindexing = reindexingRepository.getConfiguration().getReindexing();
        assertFalse(reindexing.isAsync());        
        assertEquals(RepositoryConfiguration.ReindexingMode.INCREMENTAL, reindexing.mode());        
    }
}
