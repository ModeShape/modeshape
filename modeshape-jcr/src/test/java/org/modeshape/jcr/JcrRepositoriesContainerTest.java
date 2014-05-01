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
        Repository repository = repositoryFor("some name", params);
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
        assertTrue(repositoryNames.contains("Another Test Repository"));

        url = "file:src/test/resources/config/repo-config.json";
        params.put(RepositoryFactory.URL, url);
        repositoryNames = repositoriesContainer.getRepositoryNames(params);
        Assert.assertEquals(2, repositoryNames.size());
        assertTrue(repositoryNames.contains("Another Test Repository"));
        assertTrue(repositoryNames.contains("CND Sequencer Test Repository"));
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
