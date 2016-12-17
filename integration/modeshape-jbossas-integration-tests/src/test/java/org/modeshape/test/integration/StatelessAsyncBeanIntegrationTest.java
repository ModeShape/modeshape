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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Session;
import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * Integration test that creates a node using a Stateless EJB and updates it using a Stateless Async EJB Method.
 *
 * @author Richard Lucas
 */
@RunWith(Arquillian.class)
public class StatelessAsyncBeanIntegrationTest {

    @Deployment
    public static WebArchive createWarDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "async-war-test.war").addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml")).addClass(CDIRepositoryProvider.class)
                .addClass(StatelessCMTBean.class);

        // Add our custom Manifest, which has the additional Dependencies entry ...
        archive.setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
        return archive;
    }

    @Inject
    private StatelessCMTBean statelessCMTBean;

    @Inject
    Session session;

    // This test passes when run against modeshape 4.0.0.Final but fails when run against 4.1.0.Final
    @Test
    public void testAsyncUpdate() throws Exception {
        // Create a node
        statelessCMTBean.createNode(); // If you comment out this line the test passes

        // Create a second node and update it asynchronously
        Node node = statelessCMTBean.createNode();
        statelessCMTBean.update(node.getPath());

        // Give the async method plenty of time to complete
        for (int i = 0; i < 5; i++) {
            System.out.println("testProperty = " + session.getNode(node.getPath()).getProperty("testProperty").getString());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Validate the node was updated
        String finalTestProperty = session.getNode(node.getPath()).getProperty("testProperty").getString();
        System.out.println("Final testProperty = " + finalTestProperty);
        assertEquals(finalTestProperty, "test2");

    }
}
