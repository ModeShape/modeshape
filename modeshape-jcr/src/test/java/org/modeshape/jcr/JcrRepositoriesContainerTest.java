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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.api.RepositoriesContainer;
import org.modeshape.jcr.api.RepositoryFactory;

/**
 * Unit test for {@link JcrRepositoriesContainer}
 */
public class JcrRepositoriesContainerTest extends JcrRepositoryFactoryTest {

    private RepositoriesContainer repositoriesContainer;

    @Override
    @Before
    public void beforeEach() throws Exception {
        Iterator<RepositoriesContainer> containerIterator = ServiceLoader.load(RepositoriesContainer.class).iterator();
        if (!containerIterator.hasNext()) {
            Assert.fail("Cannot located a RepositoriesContainer implementation");
        }
        repositoriesContainer = containerIterator.next();
    }

    @After
    @Override
    public void afterEach() throws Exception {
        // Shut down all repositories after each test, since multiple tests may use the same URL ...
        repositoriesContainer.shutdown(10, TimeUnit.SECONDS);
    }

    @Test
    public void shouldReturnNamedRepository() throws Exception {
        String url = "file:src/test/resources/config/simple-repo-config.json";
        Map<String, String> params = Collections.singletonMap(RepositoryFactory.URL, url);
        Repository repository = repositoryFor("Another Test Repository", params);
        assertNotNull(repository);
        // execute the call one more time
        repository = repositoryFor("Another Test Repository", params);
        assertNotNull(repository);
    }

    @Test
    @FixFor( "MODE-2201" )
    public void shouldReturnRepositoryUsingConfigurationInJarFile() throws Exception {
        String url = "jar:file:src/test/resources/config/wrapped-config.jar!/com/acme/repo-config.json";
        Map<String, String> params = Collections.singletonMap(RepositoryFactory.URL, url);
        Repository repository = repositoryFor("RepoFromJarFileConfiguration", params);
        assertNotNull(repository);
        // execute the call one more time
        Repository repository2 = repositoryFor("RepoFromJarFileConfiguration", params);
        assertNotNull(repository2);
        // The same Repository instance should be returned from both calls ...
        assertSame(repository, repository2);
    }

    @Test
    public void shouldNotReturnRepositoryIfNamesDontMatch() throws Exception {
        String url = "file:src/test/resources/config/simple-repo-config.json";
        Map<String, String> params = Collections.singletonMap(RepositoryFactory.URL, url);
        Repository repository = repositoryFor("wrong name", params);
        assertNull(repository);
    }

    @Test
    public void nameParameterShouldHavePrecedenceOverNameConfigurationParam() throws Exception {
        String url = "file:src/test/resources/config/simple-repo-config.json";
        Map<String, String> params = new HashMap<String, String>();
        params.put(RepositoryFactory.URL, url);
        params.put(RepositoryFactory.REPOSITORY_NAME, "wrong name");
        Repository repository = repositoryFor("Another Test Repository", params);

        assertNotNull(repository);
    }

    @Test
    public void shouldReturnRepositoryNames() throws Exception {
        assertTrue(repositoriesContainer.getRepositoryNames(null).isEmpty());

        String url = "file:src/test/resources/config/simple-repo-config.json";
        Map<String, String> params = new HashMap<String, String>();
        params.put(RepositoryFactory.URL, url);
        Set<String> repositoryNames = repositoriesContainer.getRepositoryNames(params);
        Assert.assertTrue(repositoryNames.contains("Another Test Repository"));

        url = "file:src/test/resources/config/repo-config.json";
        params.put(RepositoryFactory.URL, url);
        repositoryNames = repositoriesContainer.getRepositoryNames(params);
        Assert.assertEquals(2, repositoryNames.size());
        Assert.assertTrue(repositoryNames.contains("Another Test Repository"));
        Assert.assertTrue(repositoryNames.contains("CND Sequencer Test Repository"));
    }

    @Override
    protected Repository repositoryFor( Map<String, String> parameters ) throws RepositoryException {
        return repositoriesContainer.getRepository(null, parameters);
    }

    protected Repository repositoryFor( String name,
                                        Map<String, String> parameters ) throws RepositoryException {
        return repositoriesContainer.getRepository(name, parameters);
    }
}
