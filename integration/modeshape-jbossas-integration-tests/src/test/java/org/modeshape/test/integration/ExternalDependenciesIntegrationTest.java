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

import static org.junit.Assert.assertNotNull;
import java.io.File;
import javax.annotation.Resource;
import javax.jcr.Node;
import javax.jcr.Session;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.JcrRepository;

/**
 * Arquillian test for the integration between a repository and external modules, more specifically webapps deployed
 * in the server
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@RunWith( Arquillian.class )
public class ExternalDependenciesIntegrationTest {

    @Resource( mappedName = "java:/jcr/externalDependenciesRepository" )
    private JcrRepository repository;

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "external-dependencies.war");
        // Add our custom Manifest, which has the additional Dependencies entry ...
        archive.setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
        archive.addAsResource(new File("src/test/resources/config"), "config");
        return archive;
    }

    @Before
    public void before() {
        assertNotNull("repository not found", repository);
    }

    @Test
    public void repositoryShouldResolveFilesFromExternalDependency() throws Exception {
        Session session = repository.login();
        try {
            // validate initial content and custom node types
            Node cars = session.getNode("/cars");
            cars.addNode("car", "car:Car");
            session.save();
        } finally {
            session.logout();
        }
    }
}
