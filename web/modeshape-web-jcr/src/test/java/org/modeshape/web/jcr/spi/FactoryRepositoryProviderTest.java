package org.modeshape.web.jcr.spi;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Collections;
import java.util.Set;
import javax.servlet.ServletContext;
import org.junit.Before;
import org.junit.Test;

public class FactoryRepositoryProviderTest {

    private final String VALID_JCR_URL = "file:src/test/resources/repo-config.json";

    private RepositoryProvider provider;
    private ServletContext context;

    @Before
    public void beforeEach() throws Exception {
        context = mock(ServletContext.class);
        when(context.getInitParameter(FactoryRepositoryProvider.JCR_URL)).thenReturn(VALID_JCR_URL);
    }

    @Test
    public void shouldLoadFileBasedJcrEngineFromClassPath() throws Exception {
        provider = new FactoryRepositoryProvider();

        // This will start up the repository ...
        provider.startup(context);

        Set<String> repoNames = provider.getJcrRepositoryNames();

        assertThat(repoNames, is(Collections.singleton("Test Repository")));

        provider.shutdown();
    }

}
