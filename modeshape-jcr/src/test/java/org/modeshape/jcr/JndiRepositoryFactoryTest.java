package org.modeshape.jcr;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.net.URL;
import javax.jcr.Repository;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.Repositories;

public class JndiRepositoryFactoryTest extends AbstractTransactionalTest {
    private static final String JCR_REPOSITORY_NAME = "Test Repository";

    private JndiRepositoryFactory factory = new JndiRepositoryFactory();
    private Reference reference = new Reference(JndiRepositoryFactory.class.getName());
    private RefAddr configFile;
    private RefAddr repositoryName = new StringRefAddr("repositoryName", JCR_REPOSITORY_NAME);

    @Before
    public void beforeEach() {
        JaasTestUtil.initJaas("security/jaas.conf.xml");
    }

    @After
    public void afterEach() throws Exception {
        JndiRepositoryFactory.shutdown().get();
    }

    @Test
    public void shouldFindConfigFileOnClasspath() throws Exception {
        configFile = new StringRefAddr("configFile", "tck/default/repo-config.json");
        reference.add(configFile);
        reference.add(repositoryName);

        JcrRepository repo = (JcrRepository)factory.getObjectInstance(reference, null, null, null);
        try {
            assertThat(repo, is(notNullValue()));
            assertThat(repo.getName(), is(JCR_REPOSITORY_NAME));
        } finally {
            repo.shutdown();
        }
    }

    @Test
    public void shouldFindConfigFileInFileSystem() throws Exception {
        URL configFileUrl = getClass().getResource("/tck/default/repo-config.json");
        assertThat(configFileUrl, is(notNullValue()));
        File file = new File(configFileUrl.toURI());
        configFile = new StringRefAddr("configFile", file.getAbsolutePath());
        reference.add(configFile);
        reference.add(repositoryName);

        JcrRepository repo = (JcrRepository)factory.getObjectInstance(reference, null, null, null);
        try {
            assertThat(repo, is(notNullValue()));
            assertThat(repo.getName(), is(JCR_REPOSITORY_NAME));
        } finally {
            if (repo != null) repo.shutdown();
        }
    }

    @Test
    public void shouldReturnSameRepository() throws Exception {
        configFile = new StringRefAddr("configFile", "tck/default/repo-config.json");
        reference.add(configFile);
        reference.add(repositoryName);

        JcrRepository repo1 = null;
        JcrRepository repo2 = null;
        try {
            repo1 = (JcrRepository)factory.getObjectInstance(reference, null, null, null);
            repo2 = (JcrRepository)factory.getObjectInstance(reference, null, null, null);
            assertThat(repo1 == repo2, is(true));
        } finally {
            try {
                if (repo1 != null) repo1.shutdown();
            } finally {
                if (repo2 != null) repo2.shutdown();
            }
        }

    }

    @Test
    public void shouldReturnNullWhenNoRepositoryConfigurationsSpecified() throws Exception {
        configFile = new StringRefAddr("configFile", "tck/default/repo-config.json");

        Repositories repositories = (Repositories)factory.getObjectInstance(reference, null, null, null);
        assertThat(repositories, is(nullValue()));
    }

    @Test
    public void shouldReturnRepositoriesWhenOneRepositoryConfigurationsSpecified() throws Exception {
        configFile = new StringRefAddr("configFiles", "tck/default/repo-config.json");
        reference.add(configFile);

        Repositories repositories = (Repositories)factory.getObjectInstance(reference, null, null, null);
        assertThat(repositories, is(notNullValue()));
        Repository repo = repositories.getRepository(JCR_REPOSITORY_NAME);
        assertThat(repo, is(notNullValue()));
    }

    @Test
    public void shouldReturnRepositoriesWhenMultipleRepositoryConfigurationsSpecified() throws Exception {
        configFile = new StringRefAddr("configFiles", "config/simple-repo-config.json,tck/default/repo-config.json");
        reference.add(configFile);

        Repositories repositories = (Repositories)factory.getObjectInstance(reference, null, null, null);
        assertThat(repositories, is(notNullValue()));
        assertThat(repositories.getRepositoryNames().isEmpty(), is(false));
        assertThat(repositories.getRepository(JCR_REPOSITORY_NAME), is(notNullValue())); // for "repo-config"
        assertThat(repositories.getRepository("Another Test Repository"), is(notNullValue())); // for "simple-repo-config"
    }
}
