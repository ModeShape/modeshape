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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import javax.jcr.Node;
import javax.jcr.Session;
import org.junit.Test;
import org.modeshape.common.util.FileUtil;

public class LocalEnvironmentTest {

    @Test
    public void shouldLoadDatabaseFromConfiguration() throws Exception {
        String pathToStorage = "target/repo/Library/content";
        FileUtil.delete(pathToStorage);

        // Create the repository configuration ...
        String configFilePath = "config/repo-config-inmemory-local-environment.json";
        InputStream configFileStream = getClass().getClassLoader().getResourceAsStream(configFilePath);
        RepositoryConfiguration repositoryConfiguration = RepositoryConfiguration.read(configFileStream, "doesn't matter");
        LocalEnvironment environment = new LocalEnvironment();

        assertNotNull(environment.getDb(repositoryConfiguration.getPersistenceConfiguration()));
    }

    @Test
    public void shouldStartRepositoryUsingLocalEnvironmentWithDefaultPersistenceConfiguration() throws Exception {
        // Create the repository configuration ...
        String configFilePath = "config/repo-config-inmemory-no-persistence.json";
        InputStream configFileStream = getClass().getClassLoader().getResourceAsStream(configFilePath);
        RepositoryConfiguration repositoryConfiguration = RepositoryConfiguration.read(configFileStream, "doesn't matter");

        LocalEnvironment environment = new LocalEnvironment();
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
            environment.shutdown();
        }
    }
       
    protected void print( Object msg ) {
        // System.out.println(msg);
    }
}
