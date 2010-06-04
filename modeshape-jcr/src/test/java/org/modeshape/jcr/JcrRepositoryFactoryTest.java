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
<<<<<<< HEAD
        url = "file:src/test/resources/tck/default/configRepository.xml?repositoryName=Test Repository Source";
=======
        url = "jcr:modeshape:file://src/test/resources/tck/default/configRepository.xml?Test Repository Source";
>>>>>>> MODE-770 Add Support for JCR2 RepositoryFactory
        params = Collections.singletonMap(JcrRepositoryFactory.URL, url);

        repository = repositoryFor(params);
        assertThat(repository, is(notNullValue()));
    }

    @Test
    public void shouldReturnRepositoryFromConfigurationClasspathResource() {
<<<<<<< HEAD
        url = "file:///tck/default/configRepository.xml?repositoryName=Test Repository Source";
=======
        url = "jcr:modeshape:file:///tck/default/configRepository.xml?Test Repository Source";
>>>>>>> MODE-770 Add Support for JCR2 RepositoryFactory
        params = Collections.singletonMap(JcrRepositoryFactory.URL, url);

        repository = repositoryFor(params);
        assertThat(repository, is(notNullValue()));
    }

    @Test
    public void shouldReturnSameRepositoryFromSameConfigurationFile() {
<<<<<<< HEAD
        url = "file:src/test/resources/tck/default/configRepository.xml?repositoryName=Test Repository Source";
=======
        url = "jcr:modeshape:file://src/test/resources/tck/default/configRepository.xml?Test Repository Source";
>>>>>>> MODE-770 Add Support for JCR2 RepositoryFactory
        params = Collections.singletonMap(JcrRepositoryFactory.URL, url);

        repository = repositoryFor(params);
        assertThat(repository, is(notNullValue()));

        Repository repository2 = repositoryFor(params);
        assertThat(repository2, is(notNullValue()));
        assertThat(repository, is(repository2));

    }

    @Test
    public void shouldNotReturnRepositoryForInvalidUrl() {
<<<<<<< HEAD
        url = "file:?Test Repository Source";
        assertThat(repositoryFor(Collections.singletonMap(JcrRepositoryFactory.URL, url)), is(nullValue()));

        url = "file:src/test/resources/tck/default/nonExistentFile";
        assertThat(repositoryFor(Collections.singletonMap(JcrRepositoryFactory.URL, url)), is(nullValue()));
=======
        url = "jcr:modeshape:file://?Test Repository Source";
        assertThat(repositoryFor(Collections.singletonMap(JcrRepositoryFactory.URL, url)), is(nullValue()));

        url = "jcr:modeshape:file://src/test/resources/tck/default/nonExistentFile";
        assertThat(repositoryFor(Collections.singletonMap(JcrRepositoryFactory.URL, url)), is(nullValue()));

        url = "jcr:modeshape:badProtocol://src/test/resources/tck/default/configRepository.xml";
        assertThat(repositoryFor(Collections.singletonMap(JcrRepositoryFactory.URL, url)), is(nullValue()));

        url = "jcr:wrongVendor:file://src/test/resources/tck/default/configRepository.xml";
        assertThat(repositoryFor(Collections.singletonMap(JcrRepositoryFactory.URL, url)), is(nullValue()));

        url = "other:modeshape:file://src/test/resources/tck/default/configRepository.xml";
        assertThat(repositoryFor(Collections.singletonMap(JcrRepositoryFactory.URL, url)), is(nullValue()));

        
>>>>>>> MODE-770 Add Support for JCR2 RepositoryFactory
    }

    @Test
    public void shouldReturnRepositoryWithoutNameIfOnlyOneRepositoryInEngine() {
<<<<<<< HEAD
        url = "file:src/test/resources/tck/default/configRepository.xml";
=======
        url = "jcr:modeshape:file://src/test/resources/tck/default/configRepository.xml";
>>>>>>> MODE-770 Add Support for JCR2 RepositoryFactory
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
