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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit test for {@link JcrRepositoryFactory}
 */
@SuppressWarnings( "deprecation" )
public class JcrRepositoryFactoryTest extends AbstractTransactionalTest {

    private String url;
    private Map<String, String> params;
    private Repository repository;
    private RepositoryFactory repositoryFactory;

    @Before
    public void beforeEach() throws Exception {
        Iterator<javax.jcr.RepositoryFactory> repositoryFactoryIterator = ServiceLoader.load(javax.jcr.RepositoryFactory.class).iterator();
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
