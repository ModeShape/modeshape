package org.modeshape.graph.connector.base;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.base.MockPathRepository.MockPathTransaction;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.graph.request.CreateWorkspaceRequest.CreateConflictBehavior;

public class PathRepositoryTest {

    private ExecutionContext context;
    private UUID rootNodeUuid;
    private String sourceName;
    private String defaultWorkspaceName;
    private Repository<MockPathNode, MockPathWorkspace> repository;
    private BaseRepositorySource source;

    @Before
    public void beforeEach() throws Exception {
        context = new ExecutionContext();
        rootNodeUuid = UUID.randomUUID();
        sourceName = "Path Source";
        defaultWorkspaceName = "default";

        RepositoryContext repositoryContext = mock(RepositoryContext.class);
        when(repositoryContext.getExecutionContext()).thenReturn(context);

        source = mock(BaseRepositorySource.class);
        when(source.areUpdatesAllowed()).thenReturn(true);
        when(source.getRootNodeUuidObject()).thenReturn(rootNodeUuid);
        when(source.getDefaultWorkspaceName()).thenReturn(defaultWorkspaceName);
        when(source.getName()).thenReturn(sourceName);
        when(source.getRepositoryContext()).thenReturn(repositoryContext);

        repository = new MockPathRepository(source);
    }

    private MockPathTransaction beginTransaction() {
        return (MockPathTransaction)repository.startTransaction(context, false);
    }

    private MockPathWorkspace defaultWorkspaceFor( MockPathTransaction txn ) {
        return txn.getWorkspace(defaultWorkspaceName, null);
    }

    private Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    private Segment segment( String name ) {
        return context.getValueFactories().getPathFactory().createSegment(name);
    }

    private PathFactory pathFactory() {
        return context.getValueFactories().getPathFactory();
    }

    @Test
    public void shouldHaveDefaultWorkspace() {
        assertThat(repository.getWorkspaceNames(), is(Collections.singleton(defaultWorkspaceName)));
    }

    @Test
    public void shouldBeAbleToStartTransaction() {
        assertThat(repository.startTransaction(context, false), is(notNullValue()));
    }

    @Test
    public void shouldBeAbleToCreateWorkspaceWithNoClone() {
        MockPathTransaction txn = beginTransaction();

        String newWorkspaceName = "new workspace";
        repository.createWorkspace(txn, newWorkspaceName, CreateConflictBehavior.DO_NOT_CREATE, null);

        txn.commit();

        Set<String> workspaceNames = new HashSet<String>(Arrays.asList(new String[] {defaultWorkspaceName, newWorkspaceName}));
        assertThat(repository.getWorkspaceNames(), is(workspaceNames));
    }

    @Test
    public void shouldBeAbleToReadRootNode() {
        MockPathTransaction txn = beginTransaction();
        MockPathWorkspace workspace = defaultWorkspaceFor(txn);

        Location location = Location.create(rootNodeUuid);

        PathNode rootNode = txn.getNode(workspace, location);
        assertThat(rootNode, is(notNullValue()));
        assertThat(rootNode.getParent(), is(nullValue()));
        assertThat(rootNode.getUuid(), is(rootNodeUuid));
        assertThat(rootNode.getName(), is(nullValue()));
    }

    private Property property( Name name,
                               Object... values ) {
        return context.getPropertyFactory().create(name, values);
    }

    @Test
    public void shouldBeAbleToSetPropertyOnRootNode() {
        MockPathTransaction txn = beginTransaction();
        MockPathWorkspace workspace = defaultWorkspaceFor(txn);

        Location location = Location.create(rootNodeUuid);
        MockPathNode rootNode = txn.getNode(workspace, location);

        Property property = property(ModeShapeLexicon.ROOT, true);
        rootNode = txn.setProperties(workspace, rootNode, Collections.singleton(property), null, false);
        assertThat(rootNode.getProperty(ModeShapeLexicon.ROOT), is(notNullValue()));
        txn.commit();

        txn = beginTransaction();
        workspace = defaultWorkspaceFor(txn);

        rootNode = txn.getNode(workspace, location);
        assertThat(rootNode.getProperty(ModeShapeLexicon.ROOT), is(notNullValue()));
    }

    @Test
    public void shouldBeAbleToAddChildToRootNode() {
        MockPathTransaction txn = beginTransaction();
        MockPathWorkspace workspace = defaultWorkspaceFor(txn);

        Location location = Location.create(rootNodeUuid);
        MockPathNode rootNode = txn.getNode(workspace, location);

        PathNode newNode = txn.addChild(workspace, rootNode, name("child"), -1, null, null);

        rootNode = txn.getNode(workspace, Location.create(newNode.getParent()));
        assertThat(rootNode.getChildren().contains(segment("child")), is(true));
        assertThat(newNode.getParent().isRoot(), is(true));
        txn.commit();

        txn = beginTransaction();
        workspace = defaultWorkspaceFor(txn);

        rootNode = txn.getNode(workspace, location);
        newNode = txn.getNode(workspace, Location.create(pathFactory().create("/child")));
        assertThat(newNode, is(notNullValue()));
        assertThat(newNode.getParent(), is(pathFactory().createRootPath()));
    }

    @Test
    public void shouldBeAbleToCopyChildToNewParent() {
        MockPathTransaction txn = beginTransaction();
        MockPathWorkspace workspace = defaultWorkspaceFor(txn);

        Location location = Location.create(rootNodeUuid);
        MockPathNode rootNode = txn.getNode(workspace, location);

        /*
         * Building this graph:
         * /a
         *   /b
         *     /c
         * /new
         */
        
        MockPathNode aNode = txn.addChild(workspace, rootNode, name("a"), -1, null, null);
        MockPathNode bNode = txn.addChild(workspace, aNode, name("b"), -1, null, null);
        MockPathNode cNode = txn.addChild(workspace, bNode, name("c"), -1, null, null);

        MockPathNode newNode = txn.addChild(workspace, rootNode, name("new"), -1, null, null);

        rootNode = txn.getNode(workspace, Location.create(newNode.getParent()));
        assertThat(rootNode.getChildren().contains(segment("a")), is(true));
        assertThat(rootNode.getChildren().contains(segment("new")), is(true));
        assertThat(cNode.getParent(), is(pathFactory().create("/a/b")));
        assertThat(txn.getNode(workspace, Location.create(pathFactory().create("/a/b/c"))), is(notNullValue()));

        // Have to refresh bNode after the child is added
        bNode = txn.getNode(workspace, Location.create(pathFactory().create("/a/b")));
        MockPathNode newBNode = txn.copyNode(workspace, bNode, workspace, newNode, null, true);
        newNode = txn.getNode(workspace, Location.create(newBNode.getParent()));

        assertThat(rootNode.getChildren().contains(segment("a")), is(true));
        assertThat(rootNode.getChildren().contains(segment("new")), is(true));
        assertThat(newNode.getChildren().contains(segment("b")), is(true));
        assertThat(newBNode.getChildren().contains(segment("c")), is(true));

        txn.commit();

        txn = beginTransaction();
        workspace = defaultWorkspaceFor(txn);

        rootNode = txn.getNode(workspace, location);
        newNode = txn.getNode(workspace, Location.create(pathFactory().create("/new")));
        assertThat(newNode, is(notNullValue()));
        assertThat(newNode.getParent(), is(pathFactory().createRootPath()));
    }

    @Test
    public void shouldBeAbleToMoveChildToNewParent() {
        MockPathTransaction txn = beginTransaction();
        MockPathWorkspace workspace = defaultWorkspaceFor(txn);

        Location location = Location.create(rootNodeUuid);
        MockPathNode rootNode = txn.getNode(workspace, location);

        /*
         * Building this graph:
         * /a
         *   /b
         *     /c
         * /new
         */

        MockPathNode aNode = txn.addChild(workspace, rootNode, name("a"), -1, null, null);
        MockPathNode bNode = txn.addChild(workspace, aNode, name("b"), -1, null, null);
        MockPathNode cNode = txn.addChild(workspace, bNode, name("c"), -1, null, null);

        MockPathNode newNode = txn.addChild(workspace, rootNode, name("new"), -1, null, null);

        rootNode = txn.getNode(workspace, Location.create(newNode.getParent()));
        assertThat(rootNode.getChildren().contains(segment("a")), is(true));
        assertThat(rootNode.getChildren().contains(segment("new")), is(true));
        assertThat(cNode.getParent(), is(pathFactory().create("/a/b")));
        assertThat(txn.getNode(workspace, Location.create(pathFactory().create("/a/b/c"))), is(notNullValue()));

        txn.commit();

        txn = beginTransaction();

        // Have to refresh bNode after the child is added
        bNode = txn.getNode(workspace, Location.create(pathFactory().create("/a/b")));
        Location newBLocation = txn.addChild(workspace, newNode, bNode, null, null);
        MockPathNode newBNode = txn.getNode(workspace, newBLocation);
        newNode = txn.getNode(workspace, Location.create(pathFactory().create("/new")));

        cNode = txn.getNode(workspace, Location.create(pathFactory().create("/new/b/c")));
        assertThat(cNode.getParent(), is(pathFactory().create("/new/b")));

        assertThat(rootNode.getChildren().contains(segment("a")), is(true));
        assertThat(rootNode.getChildren().contains(segment("new")), is(true));
        assertThat(newNode.getChildren().contains(segment("b")), is(true));
        assertThat(newBNode.getChildren().contains(segment("c")), is(true));

        txn.commit();

        txn = beginTransaction();
        workspace = defaultWorkspaceFor(txn);

        rootNode = txn.getNode(workspace, location);
        newNode = txn.getNode(workspace, Location.create(pathFactory().create("/new")));
        assertThat(newNode, is(notNullValue()));
        assertThat(newNode.getParent(), is(pathFactory().createRootPath()));
    }

    @Test
    public void shouldBeAbleToStackMoves() {
        MockPathTransaction txn = beginTransaction();
        MockPathWorkspace workspace = defaultWorkspaceFor(txn);

        Location location = Location.create(rootNodeUuid);
        MockPathNode rootNode = txn.getNode(workspace, location);

        /*
         * Building this graph:
         * /a
         *   /b
         *     /c
         * /new
         * /d
         *   /e
         */

        MockPathNode aNode = txn.addChild(workspace, rootNode, name("a"), -1, null, null);
        MockPathNode bNode = txn.addChild(workspace, aNode, name("b"), -1, null, null);
        MockPathNode cNode = txn.addChild(workspace, bNode, name("c"), -1, null, null);
        MockPathNode newNode = txn.addChild(workspace, rootNode, name("new"), -1, null, null);
        MockPathNode dNode = txn.addChild(workspace, rootNode, name("d"), -1, null, null);
        MockPathNode eNode = txn.addChild(workspace, dNode, name("e"), -1, null, null);

        txn.commit();

        txn = beginTransaction();

        bNode = txn.getNode(workspace, Location.create(pathFactory().create("/a/b")));
        Location newBLocation = txn.addChild(workspace, newNode, bNode, null, null);

        MockPathNode newBNode = txn.getNode(workspace, newBLocation);
        newNode = txn.getNode(workspace, Location.create(pathFactory().create("/new")));

        cNode = txn.getNode(workspace, Location.create(pathFactory().create("/new/b/c")));
        dNode = txn.getNode(workspace, Location.create(pathFactory().create("/d")));
        Location newDLocation = txn.addChild(workspace, cNode, dNode, null, null);
        txn.getNode(workspace, newDLocation);

        eNode = txn.getNode(workspace, Location.create(pathFactory().create("/new/b/c/d/e")));
        assertThat(eNode.getParent(), is(pathFactory().create("/new/b/c/d")));

        
        rootNode = txn.getNode(workspace, location);
        assertThat(rootNode.getChildren().contains(segment("new")), is(true));
        assertThat(newNode.getChildren().contains(segment("b")), is(true));
        assertThat(newBNode.getChildren().contains(segment("c")), is(true));

        txn.commit();

        txn = beginTransaction();
        workspace = defaultWorkspaceFor(txn);

        rootNode = txn.getNode(workspace, location);
        newNode = txn.getNode(workspace, Location.create(pathFactory().create("/new")));
        assertThat(newNode, is(notNullValue()));
        assertThat(newNode.getParent(), is(pathFactory().createRootPath()));
    }

}
