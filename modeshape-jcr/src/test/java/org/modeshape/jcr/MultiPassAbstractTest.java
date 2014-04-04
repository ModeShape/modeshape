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
import java.util.concurrent.Callable;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.modeshape.common.junit.SkipTestRule;

/**
 * Abstract test class for tests that repeatedly starting/stopping repositories.
 * <p>
 * Subclasses should call {@link #startRunStop(RepositoryOperation, String)} one or more times in each test, where the supplied
 * {@link RepositoryOperation} implementation does the work on a running repository.
 * </p>
 * 
 * @author rhauch
 * @author hchiorean
 */
public abstract class MultiPassAbstractTest {
    @Rule
    public TestRule skipTestRule = new SkipTestRule();

    protected void startRunStop( RepositoryOperation operation,
                                 String repositoryConfigFile ) throws Exception {
        URL configUrl = getClass().getClassLoader().getResource(repositoryConfigFile);
        RepositoryConfiguration config = RepositoryConfiguration.read(configUrl);
        startRunStop(operation, config);
    }

    protected void startRunStop( RepositoryOperation operation,
                                 RepositoryConfiguration config ) throws Exception {
        JcrRepository repository = null;

        try {
            repository = new JcrRepository(config);
            repository.start();

            operation.setRepository(repository).call();
        } finally {
            if (repository != null) {
                TestingUtil.killRepositoryAndContainer(repository);
            }
        }
    }

    protected abstract class RepositoryOperation implements Callable<Void> {
        protected JcrRepository repository;

        protected RepositoryOperation setRepository( JcrRepository repository ) {
            this.repository = repository;
            return this;
        }
    }
}
