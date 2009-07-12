package org.jboss.dna.connector.infinispan;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import java.io.File;
import java.io.IOException;
import javax.naming.Context;
import javax.naming.NamingException;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.test.ReadableConnectorTest;
import org.xml.sax.SAXException;

public class InfinispanConnectorReadableTest extends ReadableConnectorTest {

    private Context mockJndi;

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected RepositorySource setUpSource() throws NamingException {
        // Set the connection properties to be use the content of "./src/test/resources/repositories" as a repository ...
        String[] predefinedWorkspaceNames = new String[] {"aircraft", "cars"};
        InfinispanSource source = new InfinispanSource();
        source.setName("Test Repository");
        source.setPredefinedWorkspaceNames(predefinedWorkspaceNames);
        source.setDefaultWorkspaceName(predefinedWorkspaceNames[0]);
        source.setCreatingWorkspacesAllowed(false);

        // Set up the mock JNDI ...
        mockJndi = mock(Context.class);
        stub(mockJndi.lookup(anyString())).toReturn(null);
        source.setContext(mockJndi);

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
        graph.useWorkspace("aircraft");
        graph.importXmlFrom(new File("src/test/resources/aircraft.xml")).into("/");

        graph.useWorkspace("cars");
        graph.importXmlFrom(new File("src/test/resources/cars.xml")).into("/");
    }

}
