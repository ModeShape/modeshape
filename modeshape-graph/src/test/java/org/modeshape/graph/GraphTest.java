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
package org.modeshape.graph;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.transaction.xa.XAResource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.observe.ChangeObserver;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.InvalidPathException;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.query.QueryResults;
import org.modeshape.graph.query.QueryResults.Columns;
import org.modeshape.graph.query.QueryResults.Statistics;
import org.modeshape.graph.query.model.Column;
import org.modeshape.graph.query.model.Constraint;
import org.modeshape.graph.query.model.Limit;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.query.parse.SqlQueryParser;
import org.modeshape.graph.query.process.QueryResultColumns;
import org.modeshape.graph.query.validate.ImmutableSchemata;
import org.modeshape.graph.query.validate.Schemata;
import org.modeshape.graph.request.AccessQueryRequest;
import org.modeshape.graph.request.CloneBranchRequest;
import org.modeshape.graph.request.CloneWorkspaceRequest;
import org.modeshape.graph.request.CompositeRequest;
import org.modeshape.graph.request.CopyBranchRequest;
import org.modeshape.graph.request.CreateNodeRequest;
import org.modeshape.graph.request.CreateWorkspaceRequest;
import org.modeshape.graph.request.DeleteBranchRequest;
import org.modeshape.graph.request.DestroyWorkspaceRequest;
import org.modeshape.graph.request.FullTextSearchRequest;
import org.modeshape.graph.request.GetWorkspacesRequest;
import org.modeshape.graph.request.InvalidRequestException;
import org.modeshape.graph.request.LockBranchRequest;
import org.modeshape.graph.request.MoveBranchRequest;
import org.modeshape.graph.request.ReadAllChildrenRequest;
import org.modeshape.graph.request.ReadAllPropertiesRequest;
import org.modeshape.graph.request.ReadBlockOfChildrenRequest;
import org.modeshape.graph.request.ReadNextBlockOfChildrenRequest;
import org.modeshape.graph.request.ReadNodeRequest;
import org.modeshape.graph.request.ReadPropertyRequest;
import org.modeshape.graph.request.Request;
import org.modeshape.graph.request.SetPropertyRequest;
import org.modeshape.graph.request.UnlockBranchRequest;
import org.modeshape.graph.request.UpdatePropertiesRequest;
import org.modeshape.graph.request.VerifyNodeExistsRequest;
import org.modeshape.graph.request.VerifyWorkspaceRequest;
import org.modeshape.graph.request.CloneWorkspaceRequest.CloneConflictBehavior;
import org.modeshape.graph.request.CreateWorkspaceRequest.CreateConflictBehavior;
import org.modeshape.graph.request.LockBranchRequest.LockScope;
import org.modeshape.graph.request.processor.RequestProcessor;

/**
 * @author Randall Hauch
 */
public class GraphTest {

    private Graph graph;
    private Results results;
    private ExecutionContext context;
    private Path validPath;
    private String validPathString;
    private Name validName;
    private String validNameString;
    private UUID validUuid;
    private Property validIdProperty1;
    private Property validIdProperty2;
    private Location validLocation;
    private String sourceName;
    private MockRepositoryConnection connection;
    private LinkedList<Request> executedRequests;
    private Columns nextColumns;
    private List<Object[]> nextTuples;
    private Statistics nextStatistics;
    private int numberOfExecutions;
    /** Populate this with the properties (by location) that are to be read */
    private Map<Location, Collection<Property>> properties;
    /** Populate this with the children (by location) that are to be read */
    private Map<Location, List<Location>> children;
    @Mock
    private RepositoryConnectionFactory connectionFactory;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        executedRequests = new LinkedList<Request>();
        sourceName = "Source";
        context = new ExecutionContext();
        connection = new MockRepositoryConnection();
        when(connectionFactory.createConnection(sourceName)).thenReturn(connection);
        graph = new Graph(sourceName, connectionFactory, context);
        validPathString = "/a/b/c";
        validUuid = UUID.randomUUID();
        validPath = createPath(validPathString);
        validNameString = "theName";
        validName = createName(validNameString);
        Name idProperty1Name = createName("id1");
        Name idProperty2Name = createName("id2");
        validIdProperty1 = context.getPropertyFactory().create(idProperty1Name, "1");
        validIdProperty2 = context.getPropertyFactory().create(idProperty2Name, "2");
        validLocation = Location.create(validPath);

        properties = new HashMap<Location, Collection<Property>>();
        children = new HashMap<Location, List<Location>>();

        nextColumns = null;
        nextTuples = null;
        nextStatistics = null;
    }

    static class IsAnyRequest extends ArgumentMatcher<Request> {
        @Override
        public boolean matches( Object request ) {
            return request instanceof Request;
        }
    }

    protected static Request anyRequest() {
        return argThat(new IsAnyRequest());
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

    protected void setPropertiesToReadOn( Location location,
                                          Property... properties ) {
        this.properties.put(location, Arrays.asList(properties));
    }

    protected void setChildrenToReadOn( Location location,
                                        Location... children ) {
        this.children.put(location, Arrays.asList(children));
    }

    protected void assertNextRequestIsMove( Location from,
                                            Location to ) {
        Request request = executedRequests.poll();
        assertThat(request, is(instanceOf(MoveBranchRequest.class)));
        MoveBranchRequest move = (MoveBranchRequest)request;
        assertThat(move.from(), is(from));
        assertThat(move.into(), is(to));
    }

    protected void assertNextRequestIsCopy( Location from,
                                            Location to ) {
        assertNextRequestIsCopy(this.graph.getCurrentWorkspaceName(), from, to);
    }

    protected void assertNextRequestIsCopy( String fromWorkspace,
                                            Location from,
                                            Location to ) {
        Request request = executedRequests.poll();
        assertThat(request, is(instanceOf(CopyBranchRequest.class)));
        CopyBranchRequest copy = (CopyBranchRequest)request;
        assertThat(copy.fromWorkspace(), is(fromWorkspace));
        assertThat(copy.from(), is(from));
        assertThat(copy.into(), is(to));
    }

    protected void assertNextRequestIsDelete( Location at ) {
        Request request = executedRequests.poll();
        assertThat(request, is(instanceOf(DeleteBranchRequest.class)));
        DeleteBranchRequest delete = (DeleteBranchRequest)request;
        assertThat(delete.at(), is(at));
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

    protected void assertNextRequestIsCreate( Location parent,
                                              String child,
                                              Property... properties ) {
        Name name = context.getValueFactories().getNameFactory().create(child);
        Request request = executedRequests.poll();
        assertThat(request, is(instanceOf(CreateNodeRequest.class)));
        CreateNodeRequest create = (CreateNodeRequest)request;
        assertThat(create.under(), is(parent));
        assertThat(create.named(), is(name));
        assertThat(create.properties(), hasItems(properties));
    }

    protected void assertNextRequestReadProperties( Location at,
                                                    Property... properties ) {
        Request request = executedRequests.poll();
        assertThat(request, is(instanceOf(ReadAllPropertiesRequest.class)));
        ReadAllPropertiesRequest readAll = (ReadAllPropertiesRequest)request;
        assertThat(readAll.at(), is(at));
        Map<Name, Property> propsByName = new HashMap<Name, Property>(readAll.getPropertiesByName());
        for (Property prop : properties) {
            assertThat(propsByName.remove(prop.getName()), is(prop));
        }
        assertThat(propsByName.isEmpty(), is(true));
    }

    protected void assertNextRequestReadProperty( Location at,
                                                  Property property ) {
        Request request = executedRequests.poll();
        assertThat(request, is(instanceOf(ReadPropertyRequest.class)));
        ReadPropertyRequest read = (ReadPropertyRequest)request;
        assertThat(read.on(), is(at));
        assertThat(read.getProperty(), is(property));
    }

    protected void assertNextRequestReadChildren( Location at,
                                                  Location... children ) {
        Request request = executedRequests.poll();
        assertThat(request, is(instanceOf(ReadAllChildrenRequest.class)));
        ReadAllChildrenRequest readAll = (ReadAllChildrenRequest)request;
        assertThat(readAll.of(), is(at));
        assertThat(readAll.getChildren(), hasItems(children));
    }

    protected void assertNextRequestReadBlockOfChildren( Location at,
                                                         int startIndex,
                                                         int maxCount,
                                                         Location... children ) {
        Request request = executedRequests.poll();
        assertThat(request, is(instanceOf(ReadBlockOfChildrenRequest.class)));
        ReadBlockOfChildrenRequest read = (ReadBlockOfChildrenRequest)request;
        assertThat(read.of(), is(at));
        assertThat(read.startingAtIndex(), is(startIndex));
        assertThat(read.endingBefore(), is(startIndex + maxCount));
        assertThat(read.count(), is(maxCount));
        assertThat(read.getChildren(), hasItems(children));
    }

    protected void assertNextRequestReadNextBlockOfChildren( Location previousSibling,
                                                             int maxCount,
                                                             Location... children ) {
        Request request = executedRequests.poll();
        assertThat(request, is(instanceOf(ReadNextBlockOfChildrenRequest.class)));
        ReadNextBlockOfChildrenRequest read = (ReadNextBlockOfChildrenRequest)request;
        assertThat(read.startingAfter(), is(previousSibling));
        assertThat(read.count(), is(maxCount));
        assertThat(read.getChildren(), hasItems(children));
    }

    protected void assertNextRequestReadNode( Location at ) {
        Request request = executedRequests.poll();
        assertThat(request, is(instanceOf(ReadNodeRequest.class)));
        ReadNodeRequest read = (ReadNodeRequest)request;
        assertThat(read.at(), is(at));
    }

    protected void assertNextRequestVerifyNodeExists( Location at ) {
        Request request = executedRequests.poll();
        assertThat(request, is(instanceOf(VerifyNodeExistsRequest.class)));
        VerifyNodeExistsRequest read = (VerifyNodeExistsRequest)request;
        assertThat(read.at(), is(at));
    }

    protected void assertNextRequestIsGetWorkspaces() {
        Request request = executedRequests.poll();
        assertThat(request, is(instanceOf(GetWorkspacesRequest.class)));
    }

    protected void assertNextRequestIsCreateWorkspace( String workspaceName,
                                                       CreateConflictBehavior createConflictBehavior ) {
        Request request = executedRequests.poll();
        assertThat(request, is(instanceOf(CreateWorkspaceRequest.class)));
        CreateWorkspaceRequest create = (CreateWorkspaceRequest)request;
        assertThat(create.desiredNameOfNewWorkspace(), is(workspaceName));
        assertThat(create.conflictBehavior(), is(createConflictBehavior));
    }

    protected void assertNextRequestIsCloneWorkspace( String originalWorkspaceName,
                                                      String workspaceName,
                                                      CreateConflictBehavior createConflictBehavior,
                                                      CloneConflictBehavior cloneBehavior ) {
        Request request = executedRequests.poll();
        assertThat(request, is(instanceOf(CloneWorkspaceRequest.class)));
        CloneWorkspaceRequest create = (CloneWorkspaceRequest)request;
        assertThat(create.nameOfWorkspaceToBeCloned(), is(originalWorkspaceName));
        assertThat(create.desiredNameOfTargetWorkspace(), is(workspaceName));
        assertThat(create.targetConflictBehavior(), is(createConflictBehavior));
        assertThat(create.cloneConflictBehavior(), is(cloneBehavior));
    }

    protected void assertNoMoreRequests() {
        assertThat(executedRequests.isEmpty(), is(true));
        numberOfExecutions = 0;
    }

    protected void extractRequestsFromComposite() {
        Request request = executedRequests.poll();
        assertThat(request, is(instanceOf(CompositeRequest.class)));
        executedRequests.addAll(0, ((CompositeRequest)request).getRequests());
    }

    protected void assertNextRequestUpdateProperties( Location on,
                                                      Property... properties ) {
        Request request = executedRequests.poll();
        assertThat(request, is(instanceOf(UpdatePropertiesRequest.class)));
        UpdatePropertiesRequest read = (UpdatePropertiesRequest)request;
        assertThat(read.on(), is(on));
        assertThat(read.properties().values(), hasItems(properties));
    }

    protected void assertNextRequestSetProperty( Location on,
                                                 Property property ) {
        Request request = executedRequests.poll();
        assertThat(request, is(instanceOf(SetPropertyRequest.class)));
        SetPropertyRequest read = (SetPropertyRequest)request;
        assertThat(read.on(), is(on));
        assertThat(read.property(), is(property));
    }

    protected void assertNextRequestAccessQuery( String workspaceName,
                                                 String tableName,
                                                 Columns columns,
                                                 Limit limit,
                                                 Constraint... andedConstraints ) {
        Request request = executedRequests.poll();
        assertThat(request, is(instanceOf(AccessQueryRequest.class)));
        AccessQueryRequest access = (AccessQueryRequest)request;
        assertThat(access.workspace(), is(workspaceName));
        assertThat(access.selectorName().name(), is(tableName));
        assertThat(access.resultColumns(), is(columns));
        assertThat(access.limit(), is(limit));
        assertThat(access.andedConstraints(), is(Arrays.asList(andedConstraints)));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Immediate requests
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldMoveNode() {
        graph.move(validPath).into(validIdProperty1, validIdProperty2);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsMove(Location.create(validPath), Location.create(validIdProperty1, validIdProperty2));
        assertNoMoreRequests();

        graph.move(validPathString).into(validIdProperty1, validIdProperty2);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsMove(Location.create(validPath), Location.create(validIdProperty1, validIdProperty2));
        assertNoMoreRequests();

        graph.move(validUuid).into(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsMove(Location.create(validUuid), Location.create(validPath));
        assertNoMoreRequests();
    }

    @Test
    public void shouldCopyNode() {
        graph.copy(validPath).into(validIdProperty1, validIdProperty2);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCopy(Location.create(validPath), Location.create(validIdProperty1, validIdProperty2));
        assertNoMoreRequests();

        graph.copy(validPathString).into(validIdProperty1, validIdProperty2);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCopy(Location.create(validPath), Location.create(validIdProperty1, validIdProperty2));
        assertNoMoreRequests();

        graph.copy(validUuid).into(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCopy(Location.create(validUuid), Location.create(validPath));
        assertNoMoreRequests();
    }

    @Test
    public void shouldCopyNodeFromOtherWorkspace() {
        graph.copy(validPath).fromWorkspace("other").into(validIdProperty1, validIdProperty2);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCopy("other", Location.create(validPath), Location.create(validIdProperty1, validIdProperty2));
        assertNoMoreRequests();

        graph.copy(validPathString).into(validIdProperty1, validIdProperty2);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCopy(Location.create(validPath), Location.create(validIdProperty1, validIdProperty2));
        assertNoMoreRequests();

        graph.copy(validUuid).into(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCopy(Location.create(validUuid), Location.create(validPath));
        assertNoMoreRequests();
    }

    @Test
    public void shouldDeleteNode() {
        graph.delete(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsDelete(Location.create(validPath));
        assertNoMoreRequests();

        graph.delete(validPathString);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsDelete(Location.create(validPath));
        assertNoMoreRequests();

        graph.delete(validUuid);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsDelete(Location.create(validUuid));
        assertNoMoreRequests();

        graph.delete(validIdProperty1);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsDelete(Location.create(validIdProperty1));
        assertNoMoreRequests();

        graph.delete(validIdProperty1, validIdProperty2);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsDelete(Location.create(validIdProperty1, validIdProperty2));
        assertNoMoreRequests();
    }

    @Test
    public void shouldCreateNode() {
        graph.create(validPath).and();
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCreate(Location.create(validPath.getParent()), "c");
        assertNoMoreRequests();

        graph.create(validPath, validIdProperty1).and();
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCreate(Location.create(validPath.getParent()), "c", validIdProperty1);
        assertNoMoreRequests();

        graph.create(validPath, validIdProperty1, validIdProperty2).and();
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCreate(Location.create(validPath.getParent()), "c", validIdProperty1, validIdProperty2);
        assertNoMoreRequests();

        graph.create(validPathString).and();
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCreate(Location.create(validPath.getParent()), "c");
        assertNoMoreRequests();

        graph.create(validPathString, validIdProperty1).and();
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCreate(Location.create(validPath.getParent()), "c", validIdProperty1);
        assertNoMoreRequests();

        graph.create(validPathString, validIdProperty1, validIdProperty2).and();
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCreate(Location.create(validPath.getParent()), "c", validIdProperty1, validIdProperty2);
        assertNoMoreRequests();
    }

    @Test
    public void shouldCreateNodeAndReturnGraph() {
        graph.create(validPath).and().getNodeAt(validPath);
        assertThat(numberOfExecutions, is(2));
        assertNextRequestIsCreate(Location.create(validPath.getParent()), "c");
        assertNextRequestReadNode(Location.create(validPath));
        assertNoMoreRequests();
    }

    @Test
    public void shouldCreateNodeAtPathWithPropertiesAndReturnLocation() {
        Location actual = graph.createAt(validPath).with(validIdProperty1).getLocation();
        assertThat(actual, is(notNullValue()));
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCreate(Location.create(validPath.getParent()), "c", validIdProperty1);
        assertNoMoreRequests();

        actual = graph.createAt(validPath).with(validIdProperty1).and(validIdProperty2).getLocation();
        assertThat(actual, is(notNullValue()));
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCreate(Location.create(validPath.getParent()), "c", validIdProperty1, validIdProperty2);
        assertNoMoreRequests();
    }

    @Test
    public void shouldCreateNodeAtPathWithPropertiesAndReturnNode() {
        Node node = graph.createAt(validPath).with(validIdProperty1).getNode();
        assertThat(node, is(notNullValue()));
        assertThat(numberOfExecutions, is(2));
        assertNextRequestIsCreate(Location.create(validPath.getParent()), "c", validIdProperty1);
        assertNextRequestReadNode(Location.create(validPath));
        assertNoMoreRequests();

        node = graph.createAt(validPath).with(validIdProperty1).and(validIdProperty2).getNode();
        assertThat(node, is(notNullValue()));
        assertThat(numberOfExecutions, is(2));
        assertNextRequestIsCreate(Location.create(validPath.getParent()), "c", validIdProperty1, validIdProperty2);
        assertNextRequestReadNode(Location.create(validPath));
        assertNoMoreRequests();
    }

    @Test
    public void shouldCreateNodeAtPathWithPropertiesAndReturnGraph() {
        graph.createAt(validPath).with(validIdProperty1).and().getNodeAt(validPath);
        assertThat(numberOfExecutions, is(2));
        assertNextRequestIsCreate(Location.create(validPath.getParent()), "c", validIdProperty1);
        assertNextRequestReadNode(Location.create(validPath));
        assertNoMoreRequests();

        graph.createAt(validPath).with(validIdProperty1).and(validIdProperty2).and().getNodeAt(validPath);
        assertThat(numberOfExecutions, is(2));
        assertNextRequestIsCreate(Location.create(validPath.getParent()), "c", validIdProperty1, validIdProperty2);
        assertNextRequestReadNode(Location.create(validPath));
        assertNoMoreRequests();
    }

    @Test
    public void shouldCreateNodesWithBatch() {
        graph.batch().create(validPath, validIdProperty1).and().remove("prop").on(validPathString).execute();
        graph.batch().move(validPath).and(validPath).into(validPathString).and().create(validPath).and().execute();
        graph.batch().createUnder(validLocation).nodeNamed("someName").and().delete(validLocation).execute();
    }

    @Test
    public void shouldGetPropertiesOnNode() {
        setPropertiesToReadOn(Location.create(validPath), validIdProperty1, validIdProperty2);
        Collection<Property> props = graph.getProperties().on(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadProperties(Location.create(validPath), validIdProperty1, validIdProperty2);
        assertNoMoreRequests();
        assertThat(props, hasItems(validIdProperty1, validIdProperty2));

        setPropertiesToReadOn(Location.create(validPath));
        props = graph.getProperties().on(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadProperties(Location.create(validPath));
        assertNoMoreRequests();
        assertThat(props.size(), is(0));
    }

    @Test
    public void shouldGetPropertiesByNameOnNode() {
        setPropertiesToReadOn(Location.create(validPath), validIdProperty1, validIdProperty2);
        Map<Name, Property> propsByName = graph.getPropertiesByName().on(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadProperties(Location.create(validPath), validIdProperty1, validIdProperty2);
        assertNoMoreRequests();
        assertThat(propsByName.get(validIdProperty1.getName()), is(validIdProperty1));
        assertThat(propsByName.get(validIdProperty2.getName()), is(validIdProperty2));

        setPropertiesToReadOn(Location.create(validPath));
        propsByName = graph.getPropertiesByName().on(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadProperties(Location.create(validPath));
        assertNoMoreRequests();
        assertThat(propsByName.isEmpty(), is(true));
    }

    @Test
    public void shouldGetPropertyOnNode() {
        setPropertiesToReadOn(Location.create(validPath), validIdProperty1, validIdProperty2);
        graph.getProperty(validIdProperty2.getName()).on(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadProperty(Location.create(validPath), validIdProperty2);
        assertNoMoreRequests();

        setPropertiesToReadOn(Location.create(validPath), validIdProperty1, validIdProperty2);
        graph.getProperty(validIdProperty2.getName().getString(context.getNamespaceRegistry())).on(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadProperty(Location.create(validPath), validIdProperty2);
        assertNoMoreRequests();
    }

    @Test
    public void shouldGetChildrenOnNode() {
        Location child1 = Location.create(createPath(validPath, "x"));
        Location child2 = Location.create(createPath(validPath, "y"));
        Location child3 = Location.create(createPath(validPath, "z"));
        setChildrenToReadOn(Location.create(validPath), child1, child2, child3);
        List<Location> children = graph.getChildren().of(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadChildren(Location.create(validPath), child1, child2, child3);
        assertNoMoreRequests();
        assertThat(children, hasItems(child1, child2, child3));

        setChildrenToReadOn(Location.create(validPath));
        children = graph.getChildren().of(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadChildren(Location.create(validPath));
        assertNoMoreRequests();
        assertThat(children.isEmpty(), is(true));
    }

    @Test
    public void shouldGetChildrenInBlockAtStartingIndex() {
        Location child1 = Location.create(createPath(validPath, "x"));
        Location child2 = Location.create(createPath(validPath, "y"));
        Location child3 = Location.create(createPath(validPath, "z"));
        setChildrenToReadOn(Location.create(validPath), child1, child2, child3);
        List<Location> children = graph.getChildren().inBlockOf(2).startingAt(0).under(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadBlockOfChildren(Location.create(validPath), 0, 2, child1, child2);
        assertNoMoreRequests();
        assertThat(children, hasItems(child1, child2));

        children = graph.getChildren().inBlockOf(2).startingAt(1).under(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadBlockOfChildren(Location.create(validPath), 1, 2, child2, child3);
        assertNoMoreRequests();
        assertThat(children, hasItems(child2, child3));

        children = graph.getChildren().inBlockOf(2).startingAt(2).under(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadBlockOfChildren(Location.create(validPath), 2, 2, child3);
        assertNoMoreRequests();
        assertThat(children, hasItems(child3));

        children = graph.getChildren().inBlockOf(2).startingAt(20).under(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadBlockOfChildren(Location.create(validPath), 20, 2);
        assertNoMoreRequests();
        assertThat(children.isEmpty(), is(true));

        children = graph.getChildren().inBlockOf(20).startingAt(0).under(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadBlockOfChildren(Location.create(validPath), 0, 20, child1, child2, child3);
        assertNoMoreRequests();
        assertThat(children, hasItems(child1, child2, child3));
    }

    @Test
    public void shouldGetChildrenInBlockAfterPreviousSibling() {
        Path pathX = createPath(validPath, "x");
        Path pathY = createPath(validPath, "y");
        Path pathZ = createPath(validPath, "z");
        Location child1 = Location.create(pathX);
        Location child2 = Location.create(pathY);
        Location child3 = Location.create(pathZ);
        setChildrenToReadOn(Location.create(validPath), child1, child2, child3);

        List<Location> children = graph.getChildren().inBlockOf(2).startingAfter(pathX);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadNextBlockOfChildren(Location.create(pathX), 2, child2, child3);
        assertNoMoreRequests();
        assertThat(children, hasItems(child2, child3));

        children = graph.getChildren().inBlockOf(3).startingAfter(pathX);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadNextBlockOfChildren(Location.create(pathX), 3, child2, child3);
        assertNoMoreRequests();
        assertThat(children, hasItems(child2, child3));

        children = graph.getChildren().inBlockOf(2).startingAfter(pathY);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadNextBlockOfChildren(Location.create(pathY), 2, child3);
        assertNoMoreRequests();
        assertThat(children, hasItems(child3));
    }

    @Test
    public void shouldSetPropertyWithEitherOnOrToMethodsCalledFirst() {
        graph.set("propName").on(validPath).to(3.0f);
        assertNextRequestSetProperty(Location.create(validPath), createProperty("propName", 3.0f));

        graph.set("propName").to(3.0f).on(validPath);
        assertNextRequestSetProperty(Location.create(validPath), createProperty("propName", 3.0f));
    }

    @Test
    public void shouldSetPropertyValueToPrimitiveTypes() {
        graph.set("propName").on(validPath).to(3.0F);
        assertNextRequestSetProperty(Location.create(validPath), createProperty("propName", new Float(3.0f)));

        graph.set("propName").on(validPath).to(1.0D);
        assertNextRequestSetProperty(Location.create(validPath), createProperty("propName", new Double(1.0)));

        graph.set("propName").on(validPath).to(false);
        assertNextRequestSetProperty(Location.create(validPath), createProperty("propName", Boolean.FALSE));

        graph.set("propName").on(validPath).to(3);
        assertNextRequestSetProperty(Location.create(validPath), createProperty("propName", new Integer(3)));

        graph.set("propName").on(validPath).to(5L);
        assertNextRequestSetProperty(Location.create(validPath), createProperty("propName", new Long(5)));

        graph.set("propName").on(validPath).to(validPath);
        assertNextRequestSetProperty(Location.create(validPath), createProperty("propName", validPath));

        graph.set("propName").on(validPath).to(validPath.getLastSegment().getName());
        assertNextRequestSetProperty(Location.create(validPath), createProperty("propName", validPath.getLastSegment().getName()));
        Date now = new Date();
        graph.set("propName").on(validPath).to(now);
        assertNextRequestSetProperty(Location.create(validPath), createProperty("propName", now));

        DateTime dtNow = context.getValueFactories().getDateFactory().create(now);
        graph.set("propName").on(validPath).to(dtNow);
        assertNextRequestSetProperty(Location.create(validPath), createProperty("propName", dtNow));

        Calendar calNow = Calendar.getInstance();
        calNow.setTime(now);
        graph.set("propName").on(validPath).to(calNow);
        assertNextRequestSetProperty(Location.create(validPath), createProperty("propName", dtNow));
    }

    @Test
    public void shouldSetMultiplePropertiesAtOnce() {
        Property p1 = createProperty("propName1", new Float(3.0f));
        Property p2 = createProperty("propName2", new Double(1.0));
        Property p3 = createProperty("propName3", "String value");
        graph.batch().set(p1, p2, p3).on(validPath).execute();
        assertNextRequestUpdateProperties(Location.create(validPath), p1, p2, p3);
    }

    @Test
    public void shouldCombineAdjacentSetPropertyCalls() {
        Property p1 = createProperty("propName1", new Float(3.0f));
        Property p2 = createProperty("propName2", new Double(1.0));
        Property p3 = createProperty("propName3", "String value");
        graph.batch().set(p1).on(validPath).and().set(p2).on(validPath).and().set(p3).on(validPath).execute();
        assertNextRequestUpdateProperties(Location.create(validPath), p1, p2, p3);
    }

    @Test
    public void shouldNotCombineNonAdjacentSetPropertyCalls() {
        Property p1 = createProperty("propName1", new Float(3.0f));
        Property p2 = createProperty("propName2", new Double(1.0));
        Property p3 = createProperty("propName3", "String value");
        graph.batch().set(p1).on(validPath).and().set(p2).on(validPath).and().set(p3).on(validUuid).execute();
        extractRequestsFromComposite();
        assertNextRequestUpdateProperties(Location.create(validPath), p1, p2);
        assertNextRequestSetProperty(Location.create(validUuid), p3);
    }

    @Test
    public void shouldReadNode() {
        Location child1 = Location.create(createPath(validPath, "x"));
        Location child2 = Location.create(createPath(validPath, "y"));
        Location child3 = Location.create(createPath(validPath, "z"));
        setChildrenToReadOn(Location.create(validPath), child1, child2, child3);
        setPropertiesToReadOn(Location.create(validPath), validIdProperty1, validIdProperty2);
        Node node = graph.getNodeAt(validPath);
        assertThat(node, is(notNullValue()));
        assertThat(node.getChildren(), hasItems(child1, child2));
        assertThat(node.getProperties(), hasItems(validIdProperty1, validIdProperty2));
        assertThat(node.getLocation(), is(Location.create(validPath)));
        assertThat(node.getGraph(), is(sameInstance(graph)));
        assertThat(node.getPropertiesByName().get(validIdProperty1.getName()), is(validIdProperty1));
        assertThat(node.getPropertiesByName().get(validIdProperty2.getName()), is(validIdProperty2));
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadNode(Location.create(validPath));
        assertNoMoreRequests();
    }

    @Test
    public void shouldReadSubgraph() {
        Location child1 = Location.create(createPath(validPath, "x"));
        Location child2 = Location.create(createPath(validPath, "y"));
        Location child3 = Location.create(createPath(validPath, "z"));
        setChildrenToReadOn(Location.create(validPath), child1, child2, child3);
        Location child11 = Location.create(createPath(child1.getPath(), "h"));
        Location child12 = Location.create(createPath(child1.getPath(), "i"));
        Location child13 = Location.create(createPath(child1.getPath(), "j"));
        setChildrenToReadOn(child1, child11, child12, child13);
        Location child121 = Location.create(createPath(child12.getPath(), "m"));
        Location child122 = Location.create(createPath(child12.getPath(), "n"));
        Location child123 = Location.create(createPath(child12.getPath(), "o"));
        setChildrenToReadOn(child12, child121, child122, child123);

        setPropertiesToReadOn(Location.create(validPath), validIdProperty1, validIdProperty2);
        setPropertiesToReadOn(child1, validIdProperty1);
        setPropertiesToReadOn(child2, validIdProperty2);
        setPropertiesToReadOn(child11, validIdProperty1);
        setPropertiesToReadOn(child12, validIdProperty2);
        setPropertiesToReadOn(child121, validIdProperty1);
        setPropertiesToReadOn(child122, validIdProperty2);

        Subgraph subgraph = graph.getSubgraphOfDepth(2).at(validPath);
        assertThat(subgraph, is(notNullValue()));
        assertThat(subgraph.getMaximumDepth(), is(2));
        assertThat(subgraph.getLocation(), is(Location.create(validPath)));

        // Get nodes by absolute path
        Node root = subgraph.getNode(Location.create(validPath));
        assertThat(root.getChildren(), hasItems(child1, child2, child3));
        assertThat(root.getProperties(), hasItems(validIdProperty1, validIdProperty2));

        Node node1 = subgraph.getNode(child1);
        assertThat(node1.getChildren(), hasItems(child11, child12, child13));
        assertThat(node1.getProperties(), hasItems(validIdProperty1));

        Node node2 = subgraph.getNode(child2);
        assertThat(node2.getChildren().isEmpty(), is(true));
        assertThat(node2.getProperties(), hasItems(validIdProperty2));

        Node node3 = subgraph.getNode(child3);
        assertThat(node3.getChildren().isEmpty(), is(true));
        assertThat(node3.getProperties().isEmpty(), is(true));

        // Get nodes that don't exist in subgraph ...
        assertThat(subgraph.getNode(child123), is(nullValue()));

        // Get nodes by relative path ...
        root = subgraph.getNode("./");
        assertThat(root.getChildren(), hasItems(child1, child2, child3));
        assertThat(root.getProperties(), hasItems(validIdProperty1, validIdProperty2));

        node1 = subgraph.getNode("x");
        assertThat(node1.getChildren(), hasItems(child11, child12, child13));
        assertThat(node1.getProperties(), hasItems(validIdProperty1));

        node2 = subgraph.getNode("y");
        assertThat(node2.getChildren().isEmpty(), is(true));
        assertThat(node2.getProperties(), hasItems(validIdProperty2));

        node3 = subgraph.getNode("z");
        assertThat(node3.getChildren().isEmpty(), is(true));
        assertThat(node3.getProperties().isEmpty(), is(true));
    }

    // @Test
    public void shouldConstructValidSubgraphToString() {
        Location child1 = Location.create(createPath(validPath, "x"));
        Location child2 = Location.create(createPath(validPath, "y"));
        Location child3 = Location.create(createPath(validPath, "z"));
        setChildrenToReadOn(Location.create(validPath), child1, child2, child3);
        Location child11 = Location.create(createPath(child1.getPath(), "h"));
        Location child12 = Location.create(createPath(child1.getPath(), "i"));
        Location child13 = Location.create(createPath(child1.getPath(), "j"));
        setChildrenToReadOn(child1, child11, child12, child13);
        Location child121 = Location.create(createPath(child12.getPath(), "m"));
        Location child122 = Location.create(createPath(child12.getPath(), "n"));
        Location child123 = Location.create(createPath(child12.getPath(), "o"));
        setChildrenToReadOn(child12, child121, child122, child123);

        setPropertiesToReadOn(Location.create(validPath), validIdProperty1, validIdProperty2);
        setPropertiesToReadOn(child1, validIdProperty1);
        setPropertiesToReadOn(child2, validIdProperty2);
        setPropertiesToReadOn(child11, validIdProperty1);
        setPropertiesToReadOn(child12, validIdProperty2);
        setPropertiesToReadOn(child121, validIdProperty1);
        setPropertiesToReadOn(child122, validIdProperty2);

        Subgraph subgraph = graph.getSubgraphOfDepth(2).at(validPath);
        assertThat(subgraph, is(notNullValue()));
        assertThat(subgraph.getMaximumDepth(), is(2));

        String expectedToStringValue = "Subgraph\n" + "<name = \"c\" id2 = \"2\" id1 = \"1\">\n"
                                       + "  <name = \"x\" id1 = \"1\">\n" + "  <name = \"y\" id2 = \"2\">\n"
                                       + "  <name = \"z\" >\n";

        // Get nodes by relative path ...
        Node root = subgraph.getNode("./");
        assertThat(root.getChildren(), hasItems(child1, child2, child3));
        assertThat(subgraph.toString(), is(expectedToStringValue));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Batched requests
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldMoveNodeInBatches() {
        graph.batch().move(validPath).into(validIdProperty1, validIdProperty2).execute();
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsMove(Location.create(validPath), Location.create(validIdProperty1, validIdProperty2));
        assertNoMoreRequests();

        graph.batch().move(validPathString).into(validIdProperty1, validIdProperty2).execute();
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsMove(Location.create(validPath), Location.create(validIdProperty1, validIdProperty2));
        assertNoMoreRequests();

        graph.batch().move(validUuid).into(validPath).execute();
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsMove(Location.create(validUuid), Location.create(validPath));
        assertNoMoreRequests();

        graph.batch()
             .move(validPath)
             .into(validIdProperty1, validIdProperty2)
             .and()
             .move(validPathString)
             .into(validIdProperty1, validIdProperty2)
             .and()
             .move(validUuid)
             .into(validPath)
             .execute();
        assertThat(numberOfExecutions, is(1));
        extractRequestsFromComposite();
        assertNextRequestIsMove(Location.create(validPath), Location.create(validIdProperty1, validIdProperty2));
        assertNextRequestIsMove(Location.create(validPath), Location.create(validIdProperty1, validIdProperty2));
        assertNextRequestIsMove(Location.create(validUuid), Location.create(validPath));
        assertNoMoreRequests();
    }

    @Test
    public void shouldCopyNodeInBatches() {
        graph.batch().copy(validPath).into(validIdProperty1, validIdProperty2).execute();
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCopy(Location.create(validPath), Location.create(validIdProperty1, validIdProperty2));
        assertNoMoreRequests();

        graph.batch().copy(validPathString).into(validIdProperty1, validIdProperty2).execute();
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCopy(Location.create(validPath), Location.create(validIdProperty1, validIdProperty2));
        assertNoMoreRequests();

        graph.batch().copy(validUuid).into(validPath).execute();
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCopy(Location.create(validUuid), Location.create(validPath));
        assertNoMoreRequests();

        graph.batch()
             .copy(validPath)
             .into(validIdProperty1, validIdProperty2)
             .and()
             .copy(validPathString)
             .into(validIdProperty1, validIdProperty2)
             .and()
             .copy(validUuid)
             .into(validPath)
             .execute();
        assertThat(numberOfExecutions, is(1));
        extractRequestsFromComposite();
        assertNextRequestIsCopy(Location.create(validPath), Location.create(validIdProperty1, validIdProperty2));
        assertNextRequestIsCopy(Location.create(validPath), Location.create(validIdProperty1, validIdProperty2));
        assertNextRequestIsCopy(Location.create(validUuid), Location.create(validPath));
        assertNoMoreRequests();
    }

    @Test
    public void shouldReadNodesInBatches() {
        Location child1 = Location.create(createPath(validPath, "x"));
        Location child2 = Location.create(createPath(validPath, "y"));
        Location child3 = Location.create(createPath(validPath, "z"));
        setChildrenToReadOn(Location.create(validPath), child1, child2, child3);
        Location child11 = Location.create(createPath(child1.getPath(), "h"));
        Location child12 = Location.create(createPath(child1.getPath(), "i"));
        Location child13 = Location.create(createPath(child1.getPath(), "j"));
        setChildrenToReadOn(child1, child11, child12, child13);
        Location child121 = Location.create(createPath(child12.getPath(), "m"));
        Location child122 = Location.create(createPath(child12.getPath(), "n"));
        Location child123 = Location.create(createPath(child12.getPath(), "o"));
        setChildrenToReadOn(child12, child121, child122, child123);

        setPropertiesToReadOn(Location.create(validPath), validIdProperty1, validIdProperty2);
        setPropertiesToReadOn(child1, validIdProperty1);
        setPropertiesToReadOn(child2, validIdProperty2);
        setPropertiesToReadOn(child11, validIdProperty1);
        setPropertiesToReadOn(child12, validIdProperty2);
        setPropertiesToReadOn(child121, validIdProperty1);
        setPropertiesToReadOn(child122, validIdProperty2);

        results = graph.batch().read(validPath).and().read(child11).and().read(child12).execute();
        assertThat(numberOfExecutions, is(1));
        extractRequestsFromComposite();
        assertNextRequestReadNode(Location.create(validPath));
        assertNextRequestReadNode(child11);
        assertNextRequestReadNode(child12);
        assertNoMoreRequests();

        assertThat(results, is(notNullValue()));
        Node node = results.getNode(validPath);
        assertThat(node, is(notNullValue()));
        assertThat(node.getChildren(), hasItems(child1, child2, child3));
        assertThat(node.getProperties(), hasItems(validIdProperty1, validIdProperty2));
        assertThat(node.getLocation(), is(Location.create(validPath)));
        assertThat(node.getGraph(), is(sameInstance(graph)));
        assertThat(node.getPropertiesByName().get(validIdProperty1.getName()), is(validIdProperty1));
        assertThat(node.getPropertiesByName().get(validIdProperty2.getName()), is(validIdProperty2));

        node = results.getNode(child11);
        assertThat(node, is(notNullValue()));
        assertThat(node.getChildren().size(), is(0));
        assertThat(node.getProperties(), hasItems(validIdProperty1));
        assertThat(node.getLocation(), is(child11));
        assertThat(node.getGraph(), is(sameInstance(graph)));
        assertThat(node.getPropertiesByName().get(validIdProperty1.getName()), is(validIdProperty1));

        node = results.getNode(child12);
        assertThat(node, is(notNullValue()));
        assertThat(node.getChildren(), hasItems(child121, child122, child123));
        assertThat(node.getProperties(), hasItems(validIdProperty2));
        assertThat(node.getLocation(), is(child12));
        assertThat(node.getGraph(), is(sameInstance(graph)));
        assertThat(node.getPropertiesByName().get(validIdProperty2.getName()), is(validIdProperty2));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Test that the test harness (helper methods and helper classes) are set up and working ...
    // ----------------------------------------------------------------------------------------------------------------
    @Test( expected = AssertionError.class )
    public void shouldPropertyCheckReadPropertiesUsingTestHarness1() {
        setPropertiesToReadOn(Location.create(validPath), validIdProperty1);
        graph.getProperties().on(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadProperties(Location.create(validPath), validIdProperty1, validIdProperty2); // wrong!
    }

    @Test( expected = AssertionError.class )
    public void shouldPropertyCheckReadPropertiesUsingTestHarness2() {
        graph.getProperties().on(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadProperties(Location.create(validPath), validIdProperty1, validIdProperty2); // wrong!
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Requests that result in errors
    // ----------------------------------------------------------------------------------------------------------------

    @Test( expected = InvalidPathException.class )
    public void shouldPropogateExceptionFromConnectorWhenMovingLocationIsNotFound() {
        connection.error = new InvalidPathException();
        graph.move(validUuid).into(validPath);
    }

    @Test( expected = InvalidPathException.class )
    public void shouldPropogateExceptionFromConnectorWhenCopyLocationIsNotFound() {
        connection.error = new InvalidPathException();
        graph.copy(validUuid).into(validPath);
    }

    @Test( expected = InvalidPathException.class )
    public void shouldPropogateExceptionFromConnectorWhenDeleteLocationIsNotFound() {
        connection.error = new InvalidPathException();
        graph.delete(validUuid);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Multiple immediate requests via method chaining
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldMoveNodesThroughMultipleMoveRequests() {
        graph.move(validPath).into(validIdProperty1, validIdProperty2).and().move(validUuid).into(validPathString);
        assertThat(numberOfExecutions, is(2));
        assertNextRequestIsMove(Location.create(validPath), Location.create(validIdProperty1, validIdProperty2));
        assertNextRequestIsMove(Location.create(validUuid), Location.create(createPath(validPathString)));
        assertNoMoreRequests();
    }

    @Test
    public void shouldMoveAndRenameNodesThroughMultipleMoveRequests() {
        graph.move(validPath).as(validName).into(validIdProperty1, validIdProperty2).and().move(validUuid).into(validPathString);
        assertThat(numberOfExecutions, is(2));
        assertNextRequestIsMove(Location.create(validPath), Location.create(validIdProperty1, validIdProperty2));
        assertNextRequestIsMove(Location.create(validUuid), Location.create(createPath(validPathString)));
        assertNoMoreRequests();
    }

    @Test
    public void shouldIgnoreIncompleteRequests() {
        graph.move(validPath); // missing 'into(...)'
        assertNoMoreRequests();

        graph.move(validPath).into(validUuid);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsMove(Location.create(validPath), Location.create(validUuid));
        assertNoMoreRequests();
    }

    // @Test
    // public void shouldBatchMultipleRequests() {
    // // Get the children of one node and the properties of another ...
    // results = graph.batch().readChildren().of("/a/b").and().readProperties().on(validUuid).execute();
    // }

    // ----------------------------------------------------------------------------------------------------------------
    // Workspace-related tests
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldCreateWorkspaceUsingSpecificName() {
        graph.createWorkspace().named("something");
        assertNextRequestIsCreateWorkspace("something", CreateConflictBehavior.DO_NOT_CREATE);
    }

    @Test
    public void shouldCreateWorkspaceAlteringNameIfRequired() {
        graph.createWorkspace().namedSomethingLike("something");
        assertNextRequestIsCreateWorkspace("something", CreateConflictBehavior.CREATE_WITH_ADJUSTED_NAME);
    }

    @Test
    public void shouldCreateWorkspaceByCloningExistingAndUsingSpecificName() {
        graph.createWorkspace().clonedFrom("original").named("something");
        assertNextRequestIsCloneWorkspace("original",
                                          "something",
                                          CreateConflictBehavior.DO_NOT_CREATE,
                                          CloneConflictBehavior.DO_NOT_CLONE);
    }

    @Test
    public void shouldCreateWorkspaceByCloningExistingAndAlteringNameIfRequired() {
        graph.createWorkspace().clonedFrom("original").namedSomethingLike("something");
        assertNextRequestIsCloneWorkspace("original",
                                          "something",
                                          CreateConflictBehavior.CREATE_WITH_ADJUSTED_NAME,
                                          CloneConflictBehavior.DO_NOT_CLONE);
    }

    @Test
    public void shouldUseExistingWorkspace() {
        graph.useWorkspace("something");
    }

    @Test
    public void shouldLockNodeButNotDescendants() {
        graph.lock(validPath).only().withDefaultTimeout();
        assertNextRequestIsLock(Location.create(validPath), LockScope.SELF_ONLY, 0);
    }

    @Test
    public void shouldLockNodeAndItsDescendants() {
        graph.lock(validPath).andItsDescendants().withTimeoutOf(12345);
        assertNextRequestIsLock(Location.create(validPath), LockScope.SELF_AND_DESCENDANTS, 12345);
    }

    @Test
    public void shouldUnlockNode() {
        graph.unlock(validPath);
        assertNextRequestIsUnlock(Location.create(validPath));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Read set number of properties on multiple nodes ...
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldReadOnePropertyOnMultipleNodes() {
        List<Location> locations = new ArrayList<Location>();
        locations.add(Location.create(createPath("/x/y/a")));
        locations.add(Location.create(createPath("/x/y/b")));
        locations.add(Location.create(createPath("/x/y/c")));
        for (Location location : locations) {
            Property prop1 = context.getPropertyFactory().create(validName, "1");
            Property prop2 = context.getPropertyFactory().create(createName("otherName"), "2");
            setPropertiesToReadOn(location, prop1, prop2);
        }

        Map<Location, Property> propertiesByLocation = graph.getProperty(validName).on(locations);
        assertThat(numberOfExecutions, is(1));
        extractRequestsFromComposite();
        for (Location location : locations) {
            Property prop = this.properties.get(location).iterator().next();
            assertNextRequestReadProperty(location, prop);
            assertThat(propertiesByLocation.get(location), is(prop));
        }
        assertNoMoreRequests();
    }

    @Test
    public void shouldReadMultiplePropertiesOnMultipleNodes() {
        List<Location> locations = new ArrayList<Location>();
        locations.add(Location.create(createPath("/x/y/a")));
        locations.add(Location.create(createPath("/x/y/b")));
        locations.add(Location.create(createPath("/x/y/c")));
        Name name1 = createName("name1");
        Name name2 = createName("name2");
        for (Location location : locations) {
            Property prop1 = context.getPropertyFactory().create(name1, "1");
            Property prop2 = context.getPropertyFactory().create(name2, "2");
            setPropertiesToReadOn(location, prop1, prop2);
        }
        Map<Location, Map<Name, Property>> propertiesByLocation = graph.getProperties(name1, name2).on(locations);
        assertThat(numberOfExecutions, is(1));
        extractRequestsFromComposite();
        for (Location location : locations) {
            Map<Name, Property> expectedProps = new HashMap<Name, Property>();
            for (Property prop : this.properties.get(location)) {
                expectedProps.put(prop.getName(), prop);
            }
            Property prop1 = expectedProps.get(name1);
            Property prop2 = expectedProps.get(name2);
            assertNextRequestReadProperty(location, prop1);
            assertNextRequestReadProperty(location, prop2);
            assertThat(propertiesByLocation.get(location).get(name1), is(prop1));
            assertThat(propertiesByLocation.get(location).get(name2), is(prop2));
        }
        assertNoMoreRequests();
    }

    @Test
    public void shouldPerformSearchWhenConnectorSupportsQueries() {
        // Set the expected results that will be returned from the connector ...
        Columns columns = mock(Columns.class);
        when(columns.iterator()).thenAnswer(new Answer<Iterator<Column>>() {
            public Iterator<Column> answer( InvocationOnMock invocation ) throws Throwable {
                return Collections.<Column>emptyList().iterator();
            }
        });
        List<Object[]> tuples = Collections.emptyList();
        Statistics stats = mock(Statistics.class);
        nextColumns = columns;
        nextTuples = tuples;
        nextStatistics = stats;

        // Execute the seach, and verify the results were consumed by the processor ...
        String fullTextSearchExpression = "term1 term2";
        QueryResults results = graph.search(fullTextSearchExpression, 10, 0);
        assertThat(nextColumns, is(nullValue()));
        assertThat(nextTuples, is(nullValue()));
        assertThat(nextStatistics, is(nullValue()));

        // The actual results should be what the processor returned ...
        assertThat(results.getColumns(), is(sameInstance(columns)));
        assertThat(results.getTuples(), is(sameInstance(tuples)));
        assertThat(results.getStatistics(), is(sameInstance(stats)));
    }

    @Test( expected = InvalidRequestException.class )
    public void shouldFailToPerformSearchWhenConnectorDoesNotSupportsQueries() {
        // Set the expected results that will be returned from the connector ...
        nextColumns = null;
        nextTuples = null;
        nextStatistics = null;

        // Execute the seach, and verify the results were consumed by the processor ...
        String fullTextSearchExpression = "term1 term2";
        graph.search(fullTextSearchExpression, 10, 0);
    }

    @Test
    public void shouldPerformQueryWhenConnectorSupportsQueries() {
        // Set the expected results that will be returned from the connector ...
        List<Object[]> tuples = Collections.singletonList(new Object[] {"v1", "v2", "v3"});
        Statistics statistics = mock(Statistics.class);
        nextTuples = tuples;
        nextStatistics = statistics;

        // Execute the query, and verify the results were consumed by the processor ...
        TypeSystem typeSystem = context.getValueFactories().getTypeSystem();
        Schemata schemata = ImmutableSchemata.createBuilder(typeSystem).addTable("t1", "c1", "c2", "c3").build();
        QueryCommand query = new SqlQueryParser().parseQuery("SELECT * FROM t1", typeSystem);
        QueryResults results = graph.query(query, schemata).execute();
        assertThat(nextColumns, is(nullValue()));
        assertThat(nextTuples, is(nullValue()));
        assertThat(nextStatistics, is(nullValue()));

        // The actual results should be what the processor returned ...
        assertThat(results.getColumns(), is(notNullValue()));
        assertThat(results.getTuples(), is(tuples));
        assertThat(results.getStatistics(), is(notNullValue()));
        assertNextRequestAccessQuery(graph.getCurrentWorkspaceName(), "t1", columns("t1", "c1", "c2", "c3"), Limit.NONE);
    }

    @Test( expected = InvalidRequestException.class )
    public void shouldFailToPerformQueryWhenConnectorDoesNotSupportsQueries() {
        // Set the expected results that will be returned from the connector ...
        nextColumns = null;
        nextTuples = null;
        nextStatistics = null;

        // Execute the query, and verify the results were consumed by the processor ...
        TypeSystem typeSystem = context.getValueFactories().getTypeSystem();
        Schemata schemata = ImmutableSchemata.createBuilder(typeSystem).addTable("t1", "c1", "c2", "c3").build();
        QueryCommand query = new SqlQueryParser().parseQuery("SELECT * FROM t1", typeSystem);
        graph.query(query, schemata).execute();
    }

    protected Columns columns( String tableName,
                               String... columnNames ) {
        List<Column> columnList = columnList(tableName, columnNames);
        List<String> columnTypes = typesFor(columnList);
        return new QueryResultColumns(columnList, columnTypes, false);
    }

    protected Columns columnsWithScores( String tableName,
                                         String... columnNames ) {
        List<Column> columnList = columnList(tableName, columnNames);
        List<String> columnTypes = typesFor(columnList);
        return new QueryResultColumns(columnList, columnTypes, true);
    }

    protected List<String> typesFor( List<Column> columns ) {
        List<String> types = new ArrayList<String>();
        for (int i = 0; i != columns.size(); ++i) {
            types.add(PropertyType.STRING.getName());
        }
        return types;
    }

    protected List<Column> columnList( String tableName,
                                       String... columnNames ) {
        List<Column> columns = new ArrayList<Column>();
        SelectorName selectorName = new SelectorName(tableName);
        for (String columnName : columnNames) {
            columns.add(new Column(selectorName, columnName, columnName));
        }
        return columns;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Implementation of RepositoryConnection and RequestProcessor for tests
    // ----------------------------------------------------------------------------------------------------------------

    /**
     * Implementation used by the {@link MockRepositoryConnection} to process the requests. The methods of this implementation do
     * little, other than populate the incoming read {@link Request}s with the properties and children that are to be read, using
     * the {@link GraphTest#children} and {@link GraphTest#properties} values.
     */
    @SuppressWarnings( "synthetic-access" )
    class Processor extends RequestProcessor {
        protected Processor() {
            super(sourceName, context, null);
        }

        @Override
        public void process( CopyBranchRequest request ) {
            // Create a child under the new parent ...
            if (request.into().hasPath()) {
                Name childName = request.desiredName();
                if (childName == null) childName = createName("child");
                Path childPath = context.getValueFactories().getPathFactory().create(request.into().getPath(), childName);
                Location newChild = actualLocationOf(Location.create(childPath));
                // Just update the actual location
                request.setActualLocations(actualLocationOf(request.from()), newChild);
            } else {
                // Just update the actual location
                request.setActualLocations(actualLocationOf(request.from()), actualLocationOf(request.into()));
            }
        }

        @Override
        public void process( CloneBranchRequest request ) {
            // Create a child under the new parent ...
            if (request.into().hasPath()) {
                Name childName = request.desiredName();
                if (childName == null) childName = request.desiredSegment().getName();

                Path childPath = context.getValueFactories().getPathFactory().create(request.into().getPath(), childName);
                Location newChild = actualLocationOf(Location.create(childPath));
                // Just update the actual location
                request.setActualLocations(actualLocationOf(request.from()), newChild);
            } else {
                // Just update the actual location
                request.setActualLocations(actualLocationOf(request.from()), actualLocationOf(request.into()));
            }
        }

        @Override
        public void process( CreateNodeRequest request ) {
            // Just update the actual location ...
            Location parent = actualLocationOf(request.under()); // just make sure it has a path ...
            Name name = request.named();
            Path childPath = context.getValueFactories().getPathFactory().create(parent.getPath(), name);
            request.setActualLocationOfNode(Location.create(childPath));
        }

        @Override
        public void process( DeleteBranchRequest request ) {
            // Just update the actual location
            request.setActualLocationOfNode(actualLocationOf(request.at()));
        }

        @Override
        public void process( LockBranchRequest request ) {
            // Just update the actual location
            request.setActualLocation(actualLocationOf(request.at()));
        }

        @Override
        public void process( UnlockBranchRequest request ) {
            // Just update the actual location
            request.setActualLocation(actualLocationOf(request.at()));
        }

        @Override
        public void process( MoveBranchRequest request ) {
            // Just update the actual location ...
            Name newName = request.desiredName();
            if (newName == null && request.from().hasPath()) newName = request.from().getPath().getLastSegment().getName();
            if (newName == null) newName = context.getValueFactories().getNameFactory().create("d");
            // Figure out the new name and path (if needed)...
            Path newPath = null;
            if (request.into().hasPath()) {
                newPath = context.getValueFactories().getPathFactory().create(request.into().getPath(), newName);
            } else if (request.from().hasPath()) {
                newPath = context.getValueFactories().getPathFactory().create("/a/b/c");
                newPath = context.getValueFactories().getPathFactory().create(newPath, newName);
            } else {
                newPath = context.getValueFactories().getPathFactory().create("/a/b/c");
                newPath = context.getValueFactories().getPathFactory().create(newPath, newName);
            }
            // Figure out the old name and path ...
            Path oldPath = null;
            if (request.from().hasPath()) {
                oldPath = request.from().getPath();
            } else {
                oldPath = context.getValueFactories().getPathFactory().create("/x/y/z");
                oldPath = context.getValueFactories().getPathFactory().create(oldPath, newName);
            }
            Location fromLocation = request.from().hasIdProperties() ? Location.create(oldPath, request.from().getIdProperties()) : Location.create(oldPath);
            Location intoLocation = request.into().hasIdProperties() ? Location.create(newPath, request.into().getIdProperties()) : Location.create(newPath);

            request.setActualLocations(fromLocation, intoLocation);
        }

        @Override
        public void process( ReadAllChildrenRequest request ) {
            // Read the children from the map ...
            if (children.containsKey(request.of())) {
                for (Location child : children.get(request.of())) {
                    request.addChild(child);
                }
            }
            // Set the actual location
            request.setActualLocationOfNode(actualLocationOf(request.of()));
        }

        @Override
        public void process( ReadAllPropertiesRequest request ) {
            // Read the properties from the map ...
            if (properties.containsKey(request.at())) {
                for (Property property : properties.get(request.at())) {
                    request.addProperty(property);
                }
            }
            // Set the actual location
            request.setActualLocationOfNode(actualLocationOf(request.at()));
        }

        @Override
        public void process( UpdatePropertiesRequest request ) {
            // Just update the actual location
            request.setActualLocationOfNode(actualLocationOf(request.on()));
            request.setNewProperties();
        }

        @Override
        public void process( VerifyWorkspaceRequest request ) {
            // Just update the actual location
            String workspaceName = request.workspaceName();
            if (workspaceName == null) workspaceName = "default";
            request.setActualWorkspaceName(workspaceName);
            request.setActualRootLocation(Location.create(context.getValueFactories().getPathFactory().createRootPath()));
        }

        @Override
        public void process( CreateWorkspaceRequest request ) {
            // Just update the actual location
            String workspaceName = request.desiredNameOfNewWorkspace();
            if (workspaceName == null) workspaceName = "default";
            request.setActualWorkspaceName(workspaceName);
            request.setActualRootLocation(Location.create(context.getValueFactories().getPathFactory().createRootPath()));
        }

        @Override
        public void process( DestroyWorkspaceRequest request ) {
        }

        @Override
        public void process( GetWorkspacesRequest request ) {
            request.setAvailableWorkspaceNames(Collections.singleton("Test workspace"));
        }

        @Override
        public void process( CloneWorkspaceRequest request ) {
            // Just update the actual location
            String workspaceName = request.desiredNameOfTargetWorkspace();
            assert workspaceName != null;
            request.setActualWorkspaceName(workspaceName);
            request.setActualRootLocation(Location.create(context.getValueFactories().getPathFactory().createRootPath()));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.AccessQueryRequest)
         */
        @Override
        public void process( AccessQueryRequest request ) {
            if (nextTuples == null) {
                super.process(request); // should result in error
            }
            request.setResults(nextTuples, nextStatistics);
            nextColumns = null;
            nextTuples = null;
            nextStatistics = null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.FullTextSearchRequest)
         */
        @Override
        public void process( FullTextSearchRequest request ) {
            if (nextTuples == null) {
                super.process(request); // should result in error
            }
            request.setResults(nextColumns, nextTuples, nextStatistics);
            nextColumns = null;
            nextTuples = null;
            nextStatistics = null;
        }

        private Location actualLocationOf( Location location ) {
            // If the location has a path, then use the location
            if (location.hasPath()) return location;
            // Otherwise, create a new location with an artificial path ...
            Path path = context.getValueFactories().getPathFactory().create("/a/b/c/d");
            return Location.create(path, location.getIdProperties());
        }
    }

    /**
     * A connection implementation that simply records the {@link Request} that is submitted, storing the request in the
     * {@link GraphTest#executedRequests} variable.
     * 
     * @author Randall Hauch
     */
    class MockRepositoryConnection implements RepositoryConnection {
        public Throwable error = null;
        private final RequestProcessor processor = new Processor();

        public void close() {
        }

        @SuppressWarnings( "synthetic-access" )
        public void execute( ExecutionContext context,
                             Request request ) throws RepositorySourceException {
            if (error != null) {
                request.setError(error);
                return;
            }
            if (request instanceof VerifyWorkspaceRequest == false) {
                // We don't want to track the number of verify workspace requests ...
                executedRequests.add(request);
                ++numberOfExecutions;
            }
            processor.process(request);
        }

        public CachePolicy getDefaultCachePolicy() {
            return null;
        }

        public String getSourceName() {
            return null;
        }

        public XAResource getXAResource() {
            return null;
        }

        public boolean ping( long time,
                             TimeUnit unit ) {
            return true;
        }

        public void setObserver( ChangeObserver observer ) {
        }
    }
}
