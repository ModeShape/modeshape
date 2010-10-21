package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.LinkedList;
import java.util.UUID;
import javax.jcr.RepositoryException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Location;
import org.modeshape.graph.connector.MockRepositoryConnection;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.request.LockBranchRequest;
import org.modeshape.graph.request.Request;
import org.modeshape.graph.request.UnlockBranchRequest;
import org.modeshape.graph.request.LockBranchRequest.LockScope;
import org.modeshape.jcr.WorkspaceLockManager.ModeShapeLock;

public class WorkspaceLockManagerTest {

    protected JcrGraph graph;
    private ExecutionContext context;
    private UUID validUuid;
    private Location validLocation;
    private String sourceName;
    private String workspaceName;
    private MockRepositoryConnection connection;
    private LinkedList<Request> executedRequests;

    private RepositoryNodeTypeManager repoTypeManager;
    protected WorkspaceLockManager workspaceLockManager;

    @Mock
    private RepositoryConnectionFactory connectionFactory;
    @Mock
    protected JcrRepository repository;
    @Mock
    protected RepositoryLockManager repoLockManager;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        executedRequests = new LinkedList<Request>();
        sourceName = "Source";
        workspaceName = "default";
        context = new ExecutionContext();
        connection = new MockRepositoryConnection(sourceName, executedRequests);
        when(connectionFactory.createConnection(sourceName)).thenReturn(connection);
        graph = JcrGraph.create(sourceName, connectionFactory, context);

        validUuid = UUID.randomUUID();
        validLocation = Location.create(validUuid);

        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        when(repository.getExecutionContext()).thenReturn(context);
        when(repository.getRepositorySourceName()).thenReturn(sourceName);
        when(repository.getPersistentRegistry()).thenReturn(context.getNamespaceRegistry());
        when(repository.createWorkspaceGraph(anyString(), (ExecutionContext)anyObject())).thenAnswer(new Answer<Graph>() {
            public Graph answer( InvocationOnMock invocation ) throws Throwable {
                return graph;
            }
        });
        when(repository.createSystemGraph(context)).thenAnswer(new Answer<Graph>() {
            public Graph answer( InvocationOnMock invocation ) throws Throwable {
                return graph;
            }
        });

        Path locksPath = pathFactory.createAbsolutePath(JcrLexicon.SYSTEM, ModeShapeLexicon.LOCKS);
        workspaceLockManager = new WorkspaceLockManager(context, repoLockManager, workspaceName, locksPath);

        when(repoLockManager.getLockManager(anyString())).thenAnswer(new Answer<WorkspaceLockManager>() {
            public WorkspaceLockManager answer( InvocationOnMock invocation ) throws Throwable {
                return workspaceLockManager;
            }
        });
        when(repoLockManager.createWorkspaceGraph(anyString(), (ExecutionContext)anyObject())).thenAnswer(new Answer<Graph>() {
            public Graph answer( InvocationOnMock invocation ) throws Throwable {
                return graph;
            }
        });
        when(repoLockManager.createSystemGraph(context)).thenAnswer(new Answer<Graph>() {
            public Graph answer( InvocationOnMock invocation ) throws Throwable {
                return graph;
            }
        });

        // Stub out the repository, since we only need a few methods ...
        Path nodeTypesPath = context.getValueFactories().getPathFactory().createAbsolutePath(JcrLexicon.SYSTEM,
                                                                                             JcrLexicon.NODE_TYPES);
        repoTypeManager = new RepositoryNodeTypeManager(repository, nodeTypesPath, true, true);

        when(repository.getRepositoryTypeManager()).thenReturn(repoTypeManager);

        executedRequests.clear();
    }

    protected Path createPath( String path ) {
        return context.getValueFactories().getPathFactory().create(path);
    }

    protected Path createPath( Path parent,
                               String path ) {
        return context.getValueFactories().getPathFactory().create(parent, path);
    }

    protected Name createName( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    protected Property createProperty( String name,
                                       Object... values ) {
        return context.getPropertyFactory().create(createName(name), values);
    }

    protected void assertNextRequestIsLock( Location at,
                                            LockScope lockScope,
                                            long lockTimeout ) {
        Request request = executedRequests.poll();
        assertThat(request, is(instanceOf(LockBranchRequest.class)));
        LockBranchRequest lock = (LockBranchRequest)request;
        assertThat(lock.at(), is(at));
        assertThat(lock.lockScope(), is(lockScope));
        assertThat(lock.lockTimeoutInMillis(), is(lockTimeout));
    }

    protected void assertNextRequestIsUnlock( Location at ) {
        Request request = executedRequests.poll();
        assertThat(request, is(instanceOf(UnlockBranchRequest.class)));
        UnlockBranchRequest unlock = (UnlockBranchRequest)request;
        assertThat(unlock.at(), is(at));
    }

    @Test
    public void shouldCreateLockRequestWhenLockingNode() throws RepositoryException {
        String lockOwner = "testOwner";
        boolean isDeep = false;

        JcrSession session = mock(JcrSession.class);
        when(session.getExecutionContext()).thenReturn(context);
        workspaceLockManager.lockNodeInRepository(session, validUuid, lockOwner, isDeep);

        assertNextRequestIsLock(validLocation, LockScope.SELF_ONLY, 0);
    }

    @Test
    public void shouldCreateLockRequestWhenUnlockingNode() {
        ModeShapeLock lock = workspaceLockManager.createLock("testOwner", UUID.randomUUID(), validUuid, false, false);
        workspaceLockManager.unlockNodeInRepository(context, lock);

        assertNextRequestIsUnlock(validLocation);
    }

}
