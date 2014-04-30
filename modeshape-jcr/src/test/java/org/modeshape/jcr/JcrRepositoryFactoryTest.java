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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.RepositoryFactory;

/**
 * Unit test for {@link JcrRepositoryFactory}
 */
@SuppressWarnings( "deprecation" )
public class JcrRepositoryFactoryTest {

    private String url;
    private Map<String, String> params;
    private Repository repository;
    private RepositoryFactory repositoryFactory;

    @Before
    public void beforeEach() throws Exception {
        Iterator<javax.jcr.RepositoryFactory> repositoryFactoryIterator = ServiceLoader.load(javax.jcr.RepositoryFactory.class)
                                                                                       .iterator();
        if (!repositoryFactoryIterator.hasNext()) {
            fail("No RepositoryFactory implementation located");
        }
        repositoryFactory = (RepositoryFactory)repositoryFactoryIterator.next();
    }

    @After
    public void afterEach() throws Exception {
        // Shut down all repositories after each test, since multiple tests may use the same URL ...
        repositoryFactory.shutdown(10, TimeUnit.SECONDS);
    }

    @Test
    public void shouldReturnRepositoryFromConfigurationFile() throws RepositoryException {
        url = "file:src/test/resources/config/simple-repo-config.json";
        params = Collections.singletonMap(RepositoryFactory.URL, url);

        repository = repositoryFor(params);
        assertThat(repository, is(notNullValue()));
    }

    @Test
    public void shouldNotReturnRepositoryFromConfigurationFileIfRepositoryNameMatches() throws RepositoryException {
        url = "file:src/test/resources/config/simple-repo-config.json";
        params = new HashMap<String, String>();
        params.put(RepositoryFactory.URL, url);
        params.put(RepositoryFactory.REPOSITORY_NAME, "Another Test Repository");

        repository = repositoryFor(params);
        assertThat(repository, is(notNullValue()));
    }

    @Test
    public void shouldNotReturnRepositoryFromConfigurationFileIfRepositoryNameDiffers() throws RepositoryException {
        url = "file:src/test/resources/config/simple-repo-config.json";
        params = new HashMap<String, String>();
        params.put(RepositoryFactory.URL, url);
        repository = repositoryFor(params);
        assertThat(repository, is(notNullValue()));

        params.put(RepositoryFactory.REPOSITORY_NAME, "some name");
        repository = repositoryFor(params);
        assertThat(repository, is(nullValue()));
    }

    @Test
    public void shouldReturnRepositoryFromConfigurationClasspathResourceUsingFileScheme() throws RepositoryException {
        url = "file:///config/simple-repo-config.json";
        params = Collections.singletonMap(RepositoryFactory.URL, url);

        repository = repositoryFor(params);
        assertThat(repository, is(notNullValue()));
    }

    @Test
    public void shouldReturnSameRepositoryFromSameConfigurationFile() throws RepositoryException {
        url = "file:///config/simple-repo-config.json";
        params = Collections.singletonMap(RepositoryFactory.URL, url);

        repository = repositoryFor(params);
        assertThat(repository, is(notNullValue()));

        Repository repository2 = repositoryFor(params);
        assertThat(repository2, is(notNullValue()));
        assertThat(repository, is(repository2));

    }

    @Test
    public void shouldNotReturnRepositoryForInvalidUrl() {
        try {
            url = "file:?Test Repository Source";
            repositoryFor(Collections.singletonMap(RepositoryFactory.URL, url));
            fail("Expected repository exception");
        } catch (RepositoryException e) {
            // expected
        }

        try {
            url = "file:src/test/resources/nonExistentFile";
            repositoryFor(Collections.singletonMap(RepositoryFactory.URL, url));
            fail("Expected repository exception");
        } catch (RepositoryException e) {
            // expected
        }

        try {
            url = "file:src/test/resources/nonExistentFile";
            repositoryFor(Collections.singletonMap(RepositoryFactory.URL, url));
            fail("Expected repository exception");
        } catch (RepositoryException e) {
            // expected
        }
    }

    protected Repository repositoryFor( Map<String, String> parameters ) throws RepositoryException {
        return repositoryFactory.getRepository(parameters);
    }
}
