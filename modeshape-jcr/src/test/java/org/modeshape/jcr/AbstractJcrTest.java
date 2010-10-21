/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.util.UUID;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Location;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;

/**
 * Abstract test that sets up a SessionCache environment with a real {@link RepositoryNodeTypeManager}, albeit with a mocked
 * JcrSession, Workspace, and Repository.
 * 
 * @see AbstractSessionTest for an alternative with a more complete environment
 */
public abstract class AbstractJcrTest {

    protected static ExecutionContext context;
    protected static JcrRepository repository;
    protected static RepositoryLockManager repoLockManager;
    protected static RepositoryNodeTypeManager rntm;
    protected InMemoryRepositorySource source;
    protected Graph store;
    protected int numberOfConnections;
    protected SessionCache cache;
    protected JcrSession jcrSession;
    protected JcrNodeTypeManager nodeTypes;
    protected WorkspaceLockManager lockManager;
    protected JcrLockManager jcrLockManager;
    protected Workspace workspace;

    /**
     * Initialize the expensive activities, and in particular the RepositoryNodeTypeManager instance.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void beforeAll() throws Exception {
        context = new ExecutionContext();

        // Create the node type manager ...
        context.getNamespaceRegistry().register(Vehicles.Lexicon.Namespace.PREFIX, Vehicles.Lexicon.Namespace.URI);
        repository = mock(JcrRepository.class);
        when(repository.getExecutionContext()).thenReturn(context);

        repoLockManager = mock(RepositoryLockManager.class);
        when(repository.getRepositoryLockManager()).thenReturn(repoLockManager);

        rntm = new RepositoryNodeTypeManager(repository, null, true, true);
        try {
            CndNodeTypeReader cndReader = new CndNodeTypeReader(context);
            cndReader.readBuiltInTypes();
            rntm.registerNodeTypes(cndReader);
            rntm.registerNodeTypes(Vehicles.getNodeTypes(context));
        } catch (RepositoryException re) {
            re.printStackTrace();
            throw new IllegalStateException("Could not load node type definition files", re);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new IllegalStateException("Could not access node type definition files", ioe);
        }
    }

    /**
     * Set up and initialize the store and session. This allows each test method to be independent; any changes made to the
     * sessions or store state will not be seen by other tests.
     * 
     * @throws Exception
     */
    @Before
    public void beforeEach() throws Exception {
        // Set up the store ...
        source = new InMemoryRepositorySource();
        source.setName("store");
        // Use a connection factory so we can count the number of connections that were made
        RepositoryConnectionFactory connectionFactory = new RepositoryConnectionFactory() {
            public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
                if (source.getName().equals(sourceName)) {
                    ++numberOfConnections;
                    return source.getConnection();
                }
                return null;
            }
        };
        store = Graph.create(source.getName(), connectionFactory, context);

        // Load the store with content ...
        String xmlResourceName = getResourceNameOfXmlFileToImport();
        store.importXmlFrom(AbstractJcrTest.class.getClassLoader().getResourceAsStream(xmlResourceName)).into("/");
        numberOfConnections = 0; // reset the number of connections

        // Stub the session, workspace, and repository; then stub some critical methods ...
        jcrSession = mock(JcrSession.class);
        workspace = mock(Workspace.class);
        String workspaceName = "workspace1";
        when(jcrSession.getExecutionContext()).thenReturn(context);
        when(jcrSession.getWorkspace()).thenReturn(workspace);
        when(jcrSession.getRepository()).thenReturn(repository);
        when(workspace.getName()).thenReturn(workspaceName);
        when(jcrSession.isLive()).thenReturn(true);
        when(jcrSession.getUserID()).thenReturn("username");

        lockManager = new WorkspaceLockManager(context, repoLockManager, workspaceName, null);
        jcrLockManager = new JcrLockManager(jcrSession, lockManager);

        when(jcrSession.lockManager()).thenReturn(jcrLockManager);

        // Create the node type manager for the session ...
        // no need to stub the 'JcrSession.checkPermission' methods, since we're never calling 'register' on the
        // JcrNodeTypeManager
        nodeTypes = new JcrNodeTypeManager(jcrSession, rntm);
        when(jcrSession.nodeTypeManager()).thenReturn(nodeTypes);

        cache = new SessionCache(jcrSession, store.getCurrentWorkspaceName(), context, nodeTypes, store);

        when(jcrSession.getNodeByIdentifier(anyString())).thenAnswer(new Answer<AbstractJcrNode>() {
            @Override
            public AbstractJcrNode answer( InvocationOnMock invocation ) throws Throwable {
                String uuidStr = invocation.getArguments()[0].toString();
                Location location = Location.create(UUID.fromString(uuidStr));
                return cache.findJcrNode(location);
            }
        });
    }

    protected String getResourceNameOfXmlFileToImport() {
        return "cars.xml";
    }

    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    protected Path relativePath( String relativePath ) {
        return context.getValueFactories().getPathFactory().create(relativePath);
    }

    protected Path path( String absolutePath ) {
        return context.getValueFactories().getPathFactory().create(absolutePath);
    }
}
