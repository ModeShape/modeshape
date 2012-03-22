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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import java.util.Collections;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import org.junit.After;
import org.junit.Test;
import org.modeshape.jcr.api.RepositoryFactory;

public class JcrRepositoryFactoryTest {

    private String url;
    private Map<String, String> params;
    private Repository repository;

    @After
    public void afterEach() throws Exception {
        // Shut down all repositories after each test, since multiple tests may use the same URL ...
        JcrRepositoryFactory.shutdownAll().get(10, TimeUnit.SECONDS);
    }

    @Test
    public void shouldReturnRepositoryFromConfigurationFile() {
        url = "file:src/test/resources/config/simple-repo-config.json";
        params = Collections.singletonMap(JcrRepositoryFactory.URL, url);

        repository = repositoryFor(params);
        assertThat(repository, is(notNullValue()));
    }

    @Test
    public void shouldReturnRepositoryFromConfigurationClasspathResourceUsingFileScheme() {
        url = "file:///config/simple-repo-config.json";
        params = Collections.singletonMap(JcrRepositoryFactory.URL, url);

        repository = repositoryFor(params);
        assertThat(repository, is(notNullValue()));
    }

    @Test
    public void shouldReturnSameRepositoryFromSameConfigurationFile() {
        url = "file:///config/simple-repo-config.json";
        params = Collections.singletonMap(JcrRepositoryFactory.URL, url);

        repository = repositoryFor(params);
        assertThat(repository, is(notNullValue()));

        Repository repository2 = repositoryFor(params);
        assertThat(repository2, is(notNullValue()));
        assertThat(repository, is(repository2));

    }

    @Test
    public void shouldNotReturnRepositoryForInvalidUrl() {
        url = "file:?Test Repository Source";
        assertThat(repositoryFor(Collections.singletonMap(JcrRepositoryFactory.URL, url)), is(nullValue()));

        url = "file:src/test/resources/nonExistentFile";
        assertThat(repositoryFor(Collections.singletonMap(JcrRepositoryFactory.URL, url)), is(nullValue()));

        url = "file:src/test/resources/nonExistentFile";
        assertThat(repositoryFor(Collections.singletonMap(JcrRepositoryFactory.URL, url)), is(nullValue()));
    }

    protected Repository repositoryFor( Map<String, String> parameters ) {
        Repository repository;
        for (RepositoryFactory factory : ServiceLoader.load(RepositoryFactory.class)) {
            try {
                repository = factory.getRepository(parameters);
                if (repository != null) return repository;
            } catch (RepositoryException re) {
                throw new IllegalStateException(re);
            }
        }

        return null;
    }
}
