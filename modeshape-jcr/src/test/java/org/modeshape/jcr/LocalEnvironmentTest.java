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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import javax.jcr.Node;
import javax.jcr.Session;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.junit.Test;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.api.Repository;
import org.modeshape.transaction.lookup.AtomikosStandaloneJTAManagerLookup;

public class LocalEnvironmentTest extends AbstractTransactionalTest {

    @Test
    public void shouldStartRepositoryUsingLocalEnvironment() throws Exception {
        String pathToStorage = "target/repo/Library/content";
        FileUtil.delete(pathToStorage);

        // Create the repository configuration ...
        String configFilePath = "config/repo-config-inmemory-local-environment.json";
        InputStream configFileStream = getClass().getClassLoader().getResourceAsStream(configFilePath);
        RepositoryConfiguration repositoryConfiguration = RepositoryConfiguration.read(configFileStream, "doesn't matter");

        // Create the Infinispan configuration via a Local Environment ...
        ConfigurationBuilder builder = new ConfigurationBuilder();
        Configuration cacheConfig = builder.transaction()
                                           .transactionManagerLookup(new AtomikosStandaloneJTAManagerLookup())
                                           .transactionMode(TransactionMode.TRANSACTIONAL)
                                           .autoCommit(true)
                                           .lockingMode(LockingMode.PESSIMISTIC)
                                           .loaders()
                                           .passivation(false)
                                           .shared(false)
                                           .preload(false)
                                           .addFileCacheStore()
                                           .async()
                                           .threadPoolSize(10)
                                           .enabled(true)
                                           .fetchPersistentState(false)
                                           .purgeOnStartup(false)
                                           .addProperty("location", pathToStorage)
                                           .build();

        LocalEnvironment environment = new LocalEnvironment();
        Configuration newConfig = environment.defineCache(repositoryConfiguration.getCacheName(), cacheConfig);
        print(newConfig);
        repositoryConfiguration = repositoryConfiguration.with(environment);

        // Start the engine and repository ...
        ModeShapeEngine engine = new ModeShapeEngine();
        engine.start();

        try {
            JcrRepository repository = engine.deploy(repositoryConfiguration);
            Session session = repository.login();
            Node root = session.getRootNode();
            root.addNode("Library", "nt:folder");
            session.save();
            session.logout();

            session = repository.login();
            Node library = session.getNode("/Library");
            assertThat(library, is(notNullValue()));
            assertThat(library.getPrimaryNodeType().getName(), is("nt:folder"));
            session.logout();
        } finally {
            engine.shutdown().get();
            environment.shutdown(); // make sure all of the cache containers are shut down
        }

        // Redefine the LocalEnvironment, since we want to replicate the case where a new process is started,
        // and the previously used CacheContainer tends to stick around and not get cleaned up entirely within
        // the same process...
        environment = new LocalEnvironment();
        newConfig = environment.defineCache(repositoryConfiguration.getCacheName(), cacheConfig);
        print(newConfig);
        repositoryConfiguration = repositoryConfiguration.with(environment);

        // Start the engine and repository again to verify the content is being persisted ...
        engine = new ModeShapeEngine();
        engine.start();
        try {
            Repository repository = engine.deploy(repositoryConfiguration);
            Session session = repository.login();
            Node library = session.getNode("/Library");
            assertThat(library, is(notNullValue()));
            assertThat(library.getPrimaryNodeType().getName(), is("nt:folder"));
            session.logout();
        } finally {
            engine.shutdown().get();
            environment.shutdown(); // make sure all of the cache containers are shut down
        }
    }

    protected void print( Object msg ) {
        // System.out.println(msg);
    }
}
