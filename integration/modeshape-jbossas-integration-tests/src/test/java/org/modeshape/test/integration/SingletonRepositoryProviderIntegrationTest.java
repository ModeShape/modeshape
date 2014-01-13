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
package org.modeshape.test.integration;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import java.io.File;
import javax.ejb.EJB;
import javax.jcr.Repository;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith( Arquillian.class )
public class SingletonRepositoryProviderIntegrationTest {

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "singleton-test.war")
                                       .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                                       .addClasses(SingletonRepositoryProvider.class, RepositoryProvider.class);
        // Add our custom Manifest, which has the additional Dependencies entry ...
        archive.setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
        return archive;
    }

    @EJB
    private SingletonRepositoryProvider provider;

    @Test
    public void testProviderFindingRepositoriesInJndi() throws Exception {
        Repository repository = provider.getRepositoryFromRepositoriesInJndi("jcr", "sample");
        assertThat(repository, is(notNullValue()));
        provider.loginAndLogout(repository);
        provider.loginAndLogout(repository, "default");

        // Test using the full-form of JNDI name ...
        Repository repository2 = provider.getRepositoryFromRepositoriesInJndi("java:/jcr", "sample");
        assertThat(repository2, is(sameInstance(repository)));
    }

    @Test
    public void testProviderFindingRepositoryInJndi() throws Exception {
        Repository repository = provider.getRepositoryFromJndi("jcr/sample");
        assertThat(repository, is(notNullValue()));
        provider.loginAndLogout(repository);
        provider.loginAndLogout(repository, "default");

        // Test using the full-form of JNDI name ...
        Repository repository2 = provider.getRepositoryFromJndi("java:/jcr/sample");
        assertThat(repository2, is(sameInstance(repository)));
    }

    @Test
    public void testProviderFindingRepositoryWithFactory() throws Exception {
        Repository repository = provider.getRepositoryUsingRepositoryFactory("jndi:jcr/sample");
        assertThat(repository, is(notNullValue()));
        provider.loginAndLogout(repository);
        provider.loginAndLogout(repository, "default");

        // Test using the URL form with a 'repositoryName' query parameter ...
        Repository repository2 = provider.getRepositoryUsingRepositoryFactory("jndi:jcr?repositoryName=sample");
        assertThat(repository2, is(sameInstance(repository)));

        // Test using the URL of Repositories and repository name ...
        Repository repository3 = provider.getRepositoryUsingRepositoryFactory("jndi:jcr", "sample");
        assertThat(repository3, is(sameInstance(repository)));
    }

}
