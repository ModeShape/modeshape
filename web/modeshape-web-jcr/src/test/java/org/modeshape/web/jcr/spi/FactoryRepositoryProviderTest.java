package org.modeshape.web.jcr.spi;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import java.util.Collections;
import java.util.Set;
import javax.servlet.ServletContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class FactoryRepositoryProviderTest {

    private final String VALID_JCR_URL = "file:src/test/resources/configRepository.xml";
    
    private RepositoryProvider provider;
    @Mock
    private ServletContext context;
    
    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        
        when(context.getInitParameter(FactoryRepositoryProvider.JCR_URL)).thenReturn(VALID_JCR_URL);
    }
    
    @Test
    public void shouldLoadFileBasedJcrEngineFromClassPath() throws Exception {
        provider = new FactoryRepositoryProvider();
        
        provider.startup(context);

        Set<String> repoNames = provider.getJcrRepositoryNames();

        assertThat(repoNames, is(Collections.singleton("Test Repository Source")));

        provider.shutdown();
    }
    
}
