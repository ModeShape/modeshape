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

import java.net.URL;
import java.util.Arrays;
import javax.jcr.Repository;
import org.infinispan.schematic.TestUtil;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.ModeShapeEngine.State;

/**
 * 
 */
public class TestingUtil {

    private static final Logger log = Logger.getLogger(TestingUtil.class);

    public static void killRepositories( Repository... repositories ) {
        killRepositories(Arrays.asList(repositories));
    }

    public static void killRepositories( Iterable<Repository> repositories ) {
        boolean killedAtLeastOne = false;
        for (Repository repository : repositories) {
            if (repository instanceof JcrRepository) {
                if (killedAtLeastOne) {
                    // We need to wait for a few seconds before we shut down the other repositories, since they're
                    // probably clustered, and the second one needs to be updated after the first one shuts down ...
                    try {
                        Thread.sleep(1000L);
                    } catch (Exception e) {
                        throw new SystemFailureException(e);
                    }
                }
                killRepository((JcrRepository)repository);
                killedAtLeastOne = true;
            }
        }
    }
    
    public static void killRepository( JcrRepository repository ) {
        if (repository == null || repository.getState() != State.RUNNING) return;
        try {
            // Rollback any open transactions ...
            TestUtil.killTransaction(repository.runningState().txnManager());

            // First shut down the repository ...
            repository.shutdown().get();
        } catch (Throwable t) {
            log.error(t, JcrI18n.errorKillingRepository, repository.getName(), t.getMessage());
        }
    }

    public static void killEngine( ModeShapeEngine engine ) {
        if (engine == null) return;
        try {
            if (engine.getState() != State.RUNNING) return;
            engine.getRepositoryKeys().forEach(key -> {
                try {
                    TestingUtil.killRepository(engine.getRepository(key));
                } catch (NoSuchRepositoryException e) {
                    //ignore
                }
            });
        } catch (Throwable t) {
            log.error(t, JcrI18n.errorKillingEngine, t.getMessage());
        }
    }

    public static JcrRepository startRepositoryWithConfig( String configFile ) throws Exception {
        URL configUrl = TestingUtil.class.getClassLoader().getResource(configFile);
        RepositoryConfiguration config = RepositoryConfiguration.read(configUrl);
        JcrRepository repository = null;
        repository = new JcrRepository(config);
        repository.start();
        return repository;
    }
}
