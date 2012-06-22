/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.net.URL;
import javax.jcr.Session;
import org.junit.Test;
import org.modeshape.common.FixFor;

/**
 * Tests that related to repeatedly starting/stopping repositories (without another repository configured in the @Before and @After
 * methods).
 */
public class JcrRepositoryStartupTest extends AbstractTransactionalTest {

    @Test
    @FixFor( {"MODE-1526", "MODE-1512"} )
    public void shouldKeepPersistentDataAcrossRestart() throws Exception {
        File contentFolder = new File("target/persistent_repository/store/persistentRepository");
        boolean testNodeShouldExist = contentFolder.exists() && contentFolder.isDirectory();

        URL configUrl = getClass().getClassLoader().getResource("config/repo-config-persistent-cache.json");
        RepositoryConfiguration config = RepositoryConfiguration.read(configUrl);

        JcrRepository repository = null;
        try {
            // Start the repository for the first time ...
            repository = new JcrRepository(config);
            repository.start();

            Session session = repository.login();
            if (testNodeShouldExist) {
                assertNotNull(session.getNode("/testNode"));
            } else {
                session.getRootNode().addNode("testNode");
                session.save();
            }
            session.logout();
            // System.out.println("SLEEPING for 5sec");
            // Thread.sleep(5000L);
        } finally {

            // Kill the repository and the cache manager (something we only do in testing),
            // which means we have to recreate the JcrRepository instance (and its Cache instance) ...
            TestingUtil.killRepositoryAndContainer(repository);
            System.out.println("Stopped repository and killed caches ...");
        }

        // forcibly delete the update lock (see MODE-1512) ...
        File lock = new File("target/persistent_repository/index/nodeinfo/write.lock");
        assertThat(lock.exists(), is(false));

        try {
            System.out.println("Starting repository again ...");
            config = RepositoryConfiguration.read(configUrl); // re-read, since the old one has an embedded cache-container
            repository = new JcrRepository(config);
            repository.start();

            Session session = repository.login();
            assertNotNull(session.getNode("/testNode"));
            session.logout();
        } finally {
            TestingUtil.killRepositoryAndContainer(repository);
        }
    }

}
