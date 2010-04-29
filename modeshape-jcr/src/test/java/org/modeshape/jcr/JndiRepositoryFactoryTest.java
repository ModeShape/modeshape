package org.modeshape.jcr;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import org.junit.Before;
import org.junit.Test;

public class JndiRepositoryFactoryTest {
    private static final String JCR_REPOSITORY_NAME = "Test Repository Source";
    private static final String REPOSITORY_SOURCE_NAME = "Store";

    private JndiRepositoryFactory factory = new JndiRepositoryFactory();
    private Reference reference = new Reference(JndiRepositoryFactory.class.getName());
    private RefAddr configFile;
    private RefAddr repositoryName = new StringRefAddr("repositoryName", JCR_REPOSITORY_NAME);

    @Before
    public void beforeEach() {
        reference.add(repositoryName);
    }
    
    @Test
    public void shouldFindConfigFileOnClasspath() throws Exception {
        configFile = new StringRefAddr("configFile", "/tck/default/configRepository.xml");
        reference.add(configFile);

        JcrRepository repo = factory.getObjectInstance(reference, null, null, null);

        assertThat(repo, is(notNullValue()));
        assertThat(repo.getRepositorySourceName(), is(REPOSITORY_SOURCE_NAME));
    }
    
    @Test
    public void shouldFindConfigFileInFileSystem() throws Exception {
        configFile = new StringRefAddr("configFile", "./src/test/resources/tck/default/configRepository.xml");
        reference.add(configFile);

        JcrRepository repo = factory.getObjectInstance(reference, null, null, null);

        assertThat(repo, is(notNullValue()));
        assertThat(repo.getRepositorySourceName(), is(REPOSITORY_SOURCE_NAME));
    }

    @Test
    public void shouldReturnSameRepository() throws Exception {
        configFile = new StringRefAddr("configFile", "/tck/default/configRepository.xml");
        reference.add(configFile);

        JcrRepository repo1 = factory.getObjectInstance(reference, null, null, null);
        JcrRepository repo2 = factory.getObjectInstance(reference, null, null, null);
        assertThat(repo1 == repo2, is(true));
    }
}
