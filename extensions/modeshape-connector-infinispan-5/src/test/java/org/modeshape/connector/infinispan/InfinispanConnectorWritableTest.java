package org.modeshape.connector.infinispan;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import javax.naming.Context;
import javax.naming.NamingException;
import org.modeshape.graph.Graph;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.test.WritableConnectorTest;
import org.xml.sax.SAXException;

public class InfinispanConnectorWritableTest extends WritableConnectorTest {

    private Context mockJndi;

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected RepositorySource setUpSource() throws NamingException {
        String[] predefinedWorkspaceNames = new String[] {"default"};
        InfinispanSource source = new InfinispanSource();
        source.setName("Test Repository");
        source.setPredefinedWorkspaceNames(predefinedWorkspaceNames);
        source.setDefaultWorkspaceName(predefinedWorkspaceNames[0]);
        source.setCreatingWorkspacesAllowed(true);

        // Set up the mock JNDI ...
        mockJndi = mock(Context.class);
        when(mockJndi.lookup(anyString())).thenReturn(null);
        source.setContext(mockJndi);

        return source;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws SAXException
     * @throws IOException
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#initializeContent(org.modeshape.graph.Graph)
     */
    @Override
    protected void initializeContent( Graph graph ) throws IOException, SAXException {
    }
}
