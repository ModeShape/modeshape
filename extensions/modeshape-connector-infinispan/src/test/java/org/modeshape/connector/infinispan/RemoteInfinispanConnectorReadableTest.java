package org.modeshape.connector.infinispan;

import java.io.File;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.modeshape.graph.Graph;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.test.ReadableConnectorTest;
import org.xml.sax.SAXException;

public class RemoteInfinispanConnectorReadableTest extends ReadableConnectorTest {
    @BeforeClass
    public static void createContainer() throws Exception {
        RemoteInfinispanTestHelper.createServer();
    }

    @AfterClass
    public static void closeConnection() throws Exception {
        RemoteInfinispanTestHelper.releaseServer();
    }

    /*@Before
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();

    }

    @After
    public void afterEach() throws Exception {
        server.stop();
    }
    */
    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected RepositorySource setUpSource() /* throws NamingException*/{
        // Create the hotrod server.
        /*Random r = new Random();
        int port = r.nextInt(20000);
        if(!bound) {
        try{
            
        } catch (IOException e) {
            throw new RuntimeException("Unable to read configuration file, ",e);
        }
        bound = true;
        }
        */
        String[] predefinedWorkspaceNames = new String[] {"aircraft", "cars"};
        RemoteInfinispanSource source = new RemoteInfinispanSource();
        source.setName("Test Repository");
        source.setPredefinedWorkspaceNames(predefinedWorkspaceNames);
        source.setDefaultWorkspaceName(predefinedWorkspaceNames[0]);
        source.setCreatingWorkspacesAllowed(false);
        source.setRemoteInfinispanServerList(String.format("%s:%s",
                                                           RemoteInfinispanTestHelper.HOST,
                                                           RemoteInfinispanTestHelper.PORT));
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
        graph.useWorkspace("aircraft");
        graph.importXmlFrom(new File("src/test/resources/aircraft.xml")).into("/");

        graph.useWorkspace("cars");
        graph.importXmlFrom(new File("src/test/resources/cars.xml")).into("/");
    }

}
