package org.jboss.dna.connector.infinispan;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import java.io.IOException;
import javax.naming.Context;
import javax.naming.NamingException;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.test.WritableConnectorTest;
import org.xml.sax.SAXException;

public class InfinispanConnectorWritableTest extends WritableConnectorTest {

    private Context mockJndi;

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.test.AbstractConnectorTest#setUpSource()
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
        stub(mockJndi.lookup(anyString())).toReturn(null);
        source.setContext(mockJndi);

        Graph graph = Graph.create(source, context);
        graph.useWorkspace("default");

        return source;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws SAXException
     * @throws IOException
     * @see org.jboss.dna.graph.connector.test.AbstractConnectorTest#initializeContent(org.jboss.dna.graph.Graph)
     */
    @Override
    protected void initializeContent( Graph graph ) throws IOException, SAXException {
    }
}
