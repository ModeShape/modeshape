package org.modeshape.jcr;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import java.util.Collections;
import java.util.Map;
import java.util.ServiceLoader;
import javax.jcr.Repository;
import org.junit.Test;
import org.modeshape.jcr.api.RepositoryFactory;

public class JcrRepositoryFactoryTest {

    private String url;
    private Map<String, String> params;
    private Repository repository;

    @Test
    public void shouldReturnRepositoryFromConfigurationFile() {
        url = "file:src/test/resources/tck/default/configRepository.xml?repositoryName=Test Repository Source";
        params = Collections.singletonMap(JcrRepositoryFactory.URL, url);

        repository = repositoryFor(params);
        assertThat(repository, is(notNullValue()));
    }

    @Test
    public void shouldReturnRepositoryFromConfigurationClasspathResource() {
        url = "file:///tck/default/configRepository.xml?repositoryName=Test Repository Source";
        params = Collections.singletonMap(JcrRepositoryFactory.URL, url);

        repository = repositoryFor(params);
        assertThat(repository, is(notNullValue()));
    }

    @Test
    public void shouldReturnSameRepositoryFromSameConfigurationFile() {
        url = "file:src/test/resources/tck/default/configRepository.xml?repositoryName=Test Repository Source";
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

        url = "file:src/test/resources/tck/default/nonExistentFile";
        assertThat(repositoryFor(Collections.singletonMap(JcrRepositoryFactory.URL, url)), is(nullValue()));
    }

    @Test
    public void shouldReturnRepositoryWithoutNameIfOnlyOneRepositoryInEngine() {
        url = "file:src/test/resources/tck/default/configRepository.xml";
        params = Collections.singletonMap(JcrRepositoryFactory.URL, url);

        repository = repositoryFor(params);
        assertThat(repository, is(notNullValue()));

    }

    protected Repository repositoryFor( Map<String, String> parameters ) {
        Repository repository;
        for (RepositoryFactory factory : ServiceLoader.load(RepositoryFactory.class)) {
            repository = factory.getRepository(parameters);
            if (repository != null) return repository;
        }

        return null;
    }
}
