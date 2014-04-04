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

public class LocalEnvironmentTest {

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
                                           .persistence()
                                           .passivation(false)
                                           .addSingleFileStore()
                                           .shared(false)
                                           .preload(false)
                                           .async()
                                           .threadPoolSize(10)
                                           .enabled(true)
                                           .fetchPersistentState(false)
                                           .purgeOnStartup(false)
                                           .location(pathToStorage)
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
