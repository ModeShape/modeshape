/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.stub;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.transaction.xa.XAResource;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.connector.RepositorySourceListener;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.InvalidPathException;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.request.CompositeRequest;
import org.jboss.dna.graph.request.CopyBranchRequest;
import org.jboss.dna.graph.request.CreateNodeRequest;
import org.jboss.dna.graph.request.DeleteBranchRequest;
import org.jboss.dna.graph.request.MoveBranchRequest;
import org.jboss.dna.graph.request.ReadAllChildrenRequest;
import org.jboss.dna.graph.request.ReadAllPropertiesRequest;
import org.jboss.dna.graph.request.ReadBlockOfChildrenRequest;
import org.jboss.dna.graph.request.ReadNextBlockOfChildrenRequest;
import org.jboss.dna.graph.request.ReadNodeRequest;
import org.jboss.dna.graph.request.ReadPropertyRequest;
import org.jboss.dna.graph.request.Request;
import org.jboss.dna.graph.request.UpdatePropertiesRequest;
import org.jboss.dna.graph.request.processor.RequestProcessor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class GraphTest {

    private Graph graph;
    private Results results;
    private ExecutionContext context;
    private Path validPath;
    private String validPathString;
    private UUID validUuid;
    private Property validIdProperty1;
    private Property validIdProperty2;
    private Location validLocation;
    private String sourceName;
    private MockRepositoryConnection connection;
    private LinkedList<Request> executedRequests;
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
        stub(connectionFactory.createConnection(sourceName)).toReturn(connection);
        graph = new Graph(sourceName, connectionFactory, context);
        validPathString = "/a/b/c";
        validUuid = UUID.randomUUID();
        validPath = createPath(validPathString);
        Name idProperty1Name = createName("id1");
        Name idProperty2Name = createName("id2");
        validIdProperty1 = context.getPropertyFactory().create(idProperty1Name, "1");
        validIdProperty2 = context.getPropertyFactory().create(idProperty2Name, "2");
        validLocation = new Location(validPath);

        properties = new HashMap<Location, Collection<Property>>();
        children = new HashMap<Location, List<Location>>();
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
        assertThat(executedRequests.poll(), is((Request)new MoveBranchRequest(from, to)));
    }

    protected void assertNextRequestIsCopy( Location from,
                                            Location to ) {
        assertThat(executedRequests.poll(), is((Request)new CopyBranchRequest(from, to)));
    }

    protected void assertNextRequestIsDelete( Location at ) {
        assertThat(executedRequests.poll(), is((Request)new DeleteBranchRequest(at)));
    }

    protected void assertNextRequestIsCreate( Location parent,
                                              String child,
                                              Property... properties ) {
        Name name = context.getValueFactories().getNameFactory().create(child);
        assertThat(executedRequests.poll(), is((Request)new CreateNodeRequest(parent, name, properties)));
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
        assertThat(read.properties(), hasItems(properties));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Immediate requests
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldMoveNode() {
        graph.move(validPath).into(validIdProperty1, validIdProperty2);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsMove(new Location(validPath), new Location(validIdProperty1, validIdProperty2));
        assertNoMoreRequests();

        graph.move(validPathString).into(validIdProperty1, validIdProperty2);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsMove(new Location(validPath), new Location(validIdProperty1, validIdProperty2));
        assertNoMoreRequests();

        graph.move(validUuid).into(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsMove(new Location(validUuid), new Location(validPath));
        assertNoMoreRequests();
    }

    @Test
    public void shouldCopyNode() {
        graph.copy(validPath).into(validIdProperty1, validIdProperty2);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCopy(new Location(validPath), new Location(validIdProperty1, validIdProperty2));
        assertNoMoreRequests();

        graph.copy(validPathString).into(validIdProperty1, validIdProperty2);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCopy(new Location(validPath), new Location(validIdProperty1, validIdProperty2));
        assertNoMoreRequests();

        graph.copy(validUuid).into(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCopy(new Location(validUuid), new Location(validPath));
        assertNoMoreRequests();
    }

    @Test
    public void shouldDeleteNode() {
        graph.delete(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsDelete(new Location(validPath));
        assertNoMoreRequests();

        graph.delete(validPathString);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsDelete(new Location(validPath));
        assertNoMoreRequests();

        graph.delete(validUuid);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsDelete(new Location(validUuid));
        assertNoMoreRequests();

        graph.delete(validIdProperty1);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsDelete(new Location(validIdProperty1));
        assertNoMoreRequests();

        graph.delete(validIdProperty1, validIdProperty2);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsDelete(new Location(validIdProperty1, validIdProperty2));
        assertNoMoreRequests();
    }

    @Test
    public void shouldCreateNode() {
        graph.create(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCreate(new Location(validPath.getParent()), "c");
        assertNoMoreRequests();

        graph.create(validPath, validIdProperty1);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCreate(new Location(validPath.getParent()), "c", validIdProperty1);
        assertNoMoreRequests();

        graph.create(validPath, validIdProperty1, validIdProperty2);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCreate(new Location(validPath.getParent()), "c", validIdProperty1, validIdProperty2);
        assertNoMoreRequests();

        graph.create(validPathString);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCreate(new Location(validPath.getParent()), "c");
        assertNoMoreRequests();

        graph.create(validPathString, validIdProperty1);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCreate(new Location(validPath.getParent()), "c", validIdProperty1);
        assertNoMoreRequests();

        graph.create(validPathString, validIdProperty1, validIdProperty2);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCreate(new Location(validPath.getParent()), "c", validIdProperty1, validIdProperty2);
        assertNoMoreRequests();
    }

    @Test
    public void shouldCreateNodesWithBatch() {
        graph.batch().create(validPath, validIdProperty1).and().remove("prop").on(validPathString).execute();
        graph.batch().move(validPath).and(validPath).into(validPathString).and().create(validPath).execute();
        graph.batch().createUnder(validLocation).nodeNamed("someName").and().delete(validLocation).execute();
    }

    @Test
    public void shouldGetPropertiesOnNode() {
        setPropertiesToReadOn(new Location(validPath), validIdProperty1, validIdProperty2);
        Collection<Property> props = graph.getProperties().on(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadProperties(new Location(validPath), validIdProperty1, validIdProperty2);
        assertNoMoreRequests();
        assertThat(props, hasItems(validIdProperty1, validIdProperty2));

        setPropertiesToReadOn(new Location(validPath));
        props = graph.getProperties().on(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadProperties(new Location(validPath));
        assertNoMoreRequests();
        assertThat(props.size(), is(0));
    }

    @Test
    public void shouldGetPropertiesByNameOnNode() {
        setPropertiesToReadOn(new Location(validPath), validIdProperty1, validIdProperty2);
        Map<Name, Property> propsByName = graph.getPropertiesByName().on(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadProperties(new Location(validPath), validIdProperty1, validIdProperty2);
        assertNoMoreRequests();
        assertThat(propsByName.get(validIdProperty1.getName()), is(validIdProperty1));
        assertThat(propsByName.get(validIdProperty2.getName()), is(validIdProperty2));

        setPropertiesToReadOn(new Location(validPath));
        propsByName = graph.getPropertiesByName().on(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadProperties(new Location(validPath));
        assertNoMoreRequests();
        assertThat(propsByName.isEmpty(), is(true));
    }

    @Test
    public void shouldGetPropertyOnNode() {
        setPropertiesToReadOn(new Location(validPath), validIdProperty1, validIdProperty2);
        graph.getProperty(validIdProperty2.getName()).on(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadProperty(new Location(validPath), validIdProperty2);
        assertNoMoreRequests();

        setPropertiesToReadOn(new Location(validPath), validIdProperty1, validIdProperty2);
        graph.getProperty(validIdProperty2.getName().getString(context.getNamespaceRegistry())).on(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadProperty(new Location(validPath), validIdProperty2);
        assertNoMoreRequests();
    }

    @Test
    public void shouldGetChildrenOnNode() {
        Location child1 = new Location(createPath(validPath, "x"));
        Location child2 = new Location(createPath(validPath, "y"));
        Location child3 = new Location(createPath(validPath, "z"));
        setChildrenToReadOn(new Location(validPath), child1, child2, child3);
        List<Location> children = graph.getChildren().of(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadChildren(new Location(validPath), child1, child2, child3);
        assertNoMoreRequests();
        assertThat(children, hasItems(child1, child2, child3));

        setChildrenToReadOn(new Location(validPath));
        children = graph.getChildren().of(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadChildren(new Location(validPath));
        assertNoMoreRequests();
        assertThat(children.isEmpty(), is(true));
    }

    @Test
    public void shouldGetChildrenInBlockAtStartingIndex() {
        Location child1 = new Location(createPath(validPath, "x"));
        Location child2 = new Location(createPath(validPath, "y"));
        Location child3 = new Location(createPath(validPath, "z"));
        setChildrenToReadOn(new Location(validPath), child1, child2, child3);
        List<Location> children = graph.getChildren().inBlockOf(2).startingAt(0).under(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadBlockOfChildren(new Location(validPath), 0, 2, child1, child2);
        assertNoMoreRequests();
        assertThat(children, hasItems(child1, child2));

        children = graph.getChildren().inBlockOf(2).startingAt(1).under(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadBlockOfChildren(new Location(validPath), 1, 2, child2, child3);
        assertNoMoreRequests();
        assertThat(children, hasItems(child2, child3));

        children = graph.getChildren().inBlockOf(2).startingAt(2).under(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadBlockOfChildren(new Location(validPath), 2, 2, child3);
        assertNoMoreRequests();
        assertThat(children, hasItems(child3));

        children = graph.getChildren().inBlockOf(2).startingAt(20).under(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadBlockOfChildren(new Location(validPath), 20, 2);
        assertNoMoreRequests();
        assertThat(children.isEmpty(), is(true));

        children = graph.getChildren().inBlockOf(20).startingAt(0).under(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadBlockOfChildren(new Location(validPath), 0, 20, child1, child2, child3);
        assertNoMoreRequests();
        assertThat(children, hasItems(child1, child2, child3));
    }

    @Test
    public void shouldGetChildrenInBlockAfterPreviousSibling() {
        Path pathX = createPath(validPath, "x");
        Path pathY = createPath(validPath, "y");
        Path pathZ = createPath(validPath, "z");
        Location child1 = new Location(pathX);
        Location child2 = new Location(pathY);
        Location child3 = new Location(pathZ);
        setChildrenToReadOn(new Location(validPath), child1, child2, child3);

        List<Location> children = graph.getChildren().inBlockOf(2).startingAfter(pathX);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadNextBlockOfChildren(new Location(pathX), 2, child2, child3);
        assertNoMoreRequests();
        assertThat(children, hasItems(child2, child3));

        children = graph.getChildren().inBlockOf(3).startingAfter(pathX);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadNextBlockOfChildren(new Location(pathX), 3, child2, child3);
        assertNoMoreRequests();
        assertThat(children, hasItems(child2, child3));

        children = graph.getChildren().inBlockOf(2).startingAfter(pathY);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadNextBlockOfChildren(new Location(pathY), 2, child3);
        assertNoMoreRequests();
        assertThat(children, hasItems(child3));
    }

    @Test
    public void shouldSetPropertiesWithEitherOnOrToMethodsCalledFirst() {
        graph.set("propName").on(validPath).to(3.0f);
        assertNextRequestUpdateProperties(new Location(validPath), createProperty("propName", 3.0f));

        graph.set("propName").to(3.0f).on(validPath);
        assertNextRequestUpdateProperties(new Location(validPath), createProperty("propName", 3.0f));
    }

    @Test
    public void shouldSetPropertyValueToPrimitiveTypes() {
        graph.set("propName").on(validPath).to(3.0F);
        assertNextRequestUpdateProperties(new Location(validPath), createProperty("propName", new Float(3.0f)));

        graph.set("propName").on(validPath).to(1.0D);
        assertNextRequestUpdateProperties(new Location(validPath), createProperty("propName", new Double(1.0)));

        graph.set("propName").on(validPath).to(false);
        assertNextRequestUpdateProperties(new Location(validPath), createProperty("propName", Boolean.FALSE));

        graph.set("propName").on(validPath).to(3);
        assertNextRequestUpdateProperties(new Location(validPath), createProperty("propName", new Integer(3)));

        graph.set("propName").on(validPath).to(5L);
        assertNextRequestUpdateProperties(new Location(validPath), createProperty("propName", new Long(5)));

        graph.set("propName").on(validPath).to(validPath);
        assertNextRequestUpdateProperties(new Location(validPath), createProperty("propName", validPath));

        graph.set("propName").on(validPath).to(validPath.getLastSegment().getName());
        assertNextRequestUpdateProperties(new Location(validPath), createProperty("propName",
                                                                                  validPath.getLastSegment().getName()));
        Date now = new Date();
        graph.set("propName").on(validPath).to(now);
        assertNextRequestUpdateProperties(new Location(validPath), createProperty("propName", now));

        DateTime dtNow = context.getValueFactories().getDateFactory().create(now);
        graph.set("propName").on(validPath).to(dtNow);
        assertNextRequestUpdateProperties(new Location(validPath), createProperty("propName", dtNow));

        Calendar calNow = Calendar.getInstance();
        calNow.setTime(now);
        graph.set("propName").on(validPath).to(calNow);
        assertNextRequestUpdateProperties(new Location(validPath), createProperty("propName", dtNow));

    }

    @Test
    public void shouldReadNode() {
        Location child1 = new Location(createPath(validPath, "x"));
        Location child2 = new Location(createPath(validPath, "y"));
        Location child3 = new Location(createPath(validPath, "z"));
        setChildrenToReadOn(new Location(validPath), child1, child2, child3);
        setPropertiesToReadOn(new Location(validPath), validIdProperty1, validIdProperty2);
        Node node = graph.getNodeAt(validPath);
        assertThat(node, is(notNullValue()));
        assertThat(node.getChildren(), hasItems(child1, child2));
        assertThat(node.getProperties(), hasItems(validIdProperty1, validIdProperty2));
        assertThat(node.getLocation(), is(new Location(validPath)));
        assertThat(node.getGraph(), is(sameInstance(graph)));
        assertThat(node.getPropertiesByName().get(validIdProperty1.getName()), is(validIdProperty1));
        assertThat(node.getPropertiesByName().get(validIdProperty2.getName()), is(validIdProperty2));
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadNode(new Location(validPath));
        assertNoMoreRequests();
    }

    @Test
    public void shouldReadSubgraph() {
        Location child1 = new Location(createPath(validPath, "x"));
        Location child2 = new Location(createPath(validPath, "y"));
        Location child3 = new Location(createPath(validPath, "z"));
        setChildrenToReadOn(new Location(validPath), child1, child2, child3);
        Location child11 = new Location(createPath(child1.getPath(), "h"));
        Location child12 = new Location(createPath(child1.getPath(), "i"));
        Location child13 = new Location(createPath(child1.getPath(), "j"));
        setChildrenToReadOn(child1, child11, child12, child13);
        Location child121 = new Location(createPath(child12.getPath(), "m"));
        Location child122 = new Location(createPath(child12.getPath(), "n"));
        Location child123 = new Location(createPath(child12.getPath(), "o"));
        setChildrenToReadOn(child12, child121, child122, child123);

        setPropertiesToReadOn(new Location(validPath), validIdProperty1, validIdProperty2);
        setPropertiesToReadOn(child1, validIdProperty1);
        setPropertiesToReadOn(child2, validIdProperty2);
        setPropertiesToReadOn(child11, validIdProperty1);
        setPropertiesToReadOn(child12, validIdProperty2);
        setPropertiesToReadOn(child121, validIdProperty1);
        setPropertiesToReadOn(child122, validIdProperty2);

        Subgraph subgraph = graph.getSubgraphOfDepth(2).at(validPath);
        assertThat(subgraph, is(notNullValue()));
        assertThat(subgraph.getMaximumDepth(), is(2));
        assertThat(subgraph.getLocation(), is(new Location(validPath)));

        // Get nodes by absolute path
        Node root = subgraph.getNode(new Location(validPath));
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

    // ----------------------------------------------------------------------------------------------------------------
    // Batched requests
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldMoveNodeInBatches() {
        graph.batch().move(validPath).into(validIdProperty1, validIdProperty2).execute();
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsMove(new Location(validPath), new Location(validIdProperty1, validIdProperty2));
        assertNoMoreRequests();

        graph.batch().move(validPathString).into(validIdProperty1, validIdProperty2).execute();
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsMove(new Location(validPath), new Location(validIdProperty1, validIdProperty2));
        assertNoMoreRequests();

        graph.batch().move(validUuid).into(validPath).execute();
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsMove(new Location(validUuid), new Location(validPath));
        assertNoMoreRequests();

        graph.batch().move(validPath).into(validIdProperty1, validIdProperty2).and().move(validPathString).into(validIdProperty1,
                                                                                                                validIdProperty2).and().move(validUuid).into(validPath).execute();
        assertThat(numberOfExecutions, is(1));
        extractRequestsFromComposite();
        assertNextRequestIsMove(new Location(validPath), new Location(validIdProperty1, validIdProperty2));
        assertNextRequestIsMove(new Location(validPath), new Location(validIdProperty1, validIdProperty2));
        assertNextRequestIsMove(new Location(validUuid), new Location(validPath));
        assertNoMoreRequests();
    }

    @Test
    public void shouldCopyNodeInBatches() {
        graph.batch().copy(validPath).into(validIdProperty1, validIdProperty2).execute();
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCopy(new Location(validPath), new Location(validIdProperty1, validIdProperty2));
        assertNoMoreRequests();

        graph.batch().copy(validPathString).into(validIdProperty1, validIdProperty2).execute();
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCopy(new Location(validPath), new Location(validIdProperty1, validIdProperty2));
        assertNoMoreRequests();

        graph.batch().copy(validUuid).into(validPath).execute();
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsCopy(new Location(validUuid), new Location(validPath));
        assertNoMoreRequests();

        graph.batch().copy(validPath).into(validIdProperty1, validIdProperty2).and().copy(validPathString).into(validIdProperty1,
                                                                                                                validIdProperty2).and().copy(validUuid).into(validPath).execute();
        assertThat(numberOfExecutions, is(1));
        extractRequestsFromComposite();
        assertNextRequestIsCopy(new Location(validPath), new Location(validIdProperty1, validIdProperty2));
        assertNextRequestIsCopy(new Location(validPath), new Location(validIdProperty1, validIdProperty2));
        assertNextRequestIsCopy(new Location(validUuid), new Location(validPath));
        assertNoMoreRequests();
    }

    @Test
    public void shouldReadNodesInBatches() {
        Location child1 = new Location(createPath(validPath, "x"));
        Location child2 = new Location(createPath(validPath, "y"));
        Location child3 = new Location(createPath(validPath, "z"));
        setChildrenToReadOn(new Location(validPath), child1, child2, child3);
        Location child11 = new Location(createPath(child1.getPath(), "h"));
        Location child12 = new Location(createPath(child1.getPath(), "i"));
        Location child13 = new Location(createPath(child1.getPath(), "j"));
        setChildrenToReadOn(child1, child11, child12, child13);
        Location child121 = new Location(createPath(child12.getPath(), "m"));
        Location child122 = new Location(createPath(child12.getPath(), "n"));
        Location child123 = new Location(createPath(child12.getPath(), "o"));
        setChildrenToReadOn(child12, child121, child122, child123);

        setPropertiesToReadOn(new Location(validPath), validIdProperty1, validIdProperty2);
        setPropertiesToReadOn(child1, validIdProperty1);
        setPropertiesToReadOn(child2, validIdProperty2);
        setPropertiesToReadOn(child11, validIdProperty1);
        setPropertiesToReadOn(child12, validIdProperty2);
        setPropertiesToReadOn(child121, validIdProperty1);
        setPropertiesToReadOn(child122, validIdProperty2);

        results = graph.batch().read(validPath).and().read(child11).and().read(child12).execute();
        assertThat(numberOfExecutions, is(1));
        extractRequestsFromComposite();
        assertNextRequestReadNode(new Location(validPath));
        assertNextRequestReadNode(child11);
        assertNextRequestReadNode(child12);
        assertNoMoreRequests();

        assertThat(results, is(notNullValue()));
        Node node = results.getNode(validPath);
        assertThat(node, is(notNullValue()));
        assertThat(node.getChildren(), hasItems(child1, child2, child3));
        assertThat(node.getProperties(), hasItems(validIdProperty1, validIdProperty2));
        assertThat(node.getLocation(), is(new Location(validPath)));
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
        setPropertiesToReadOn(new Location(validPath), validIdProperty1);
        graph.getProperties().on(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadProperties(new Location(validPath), validIdProperty1, validIdProperty2); // wrong!
    }

    @Test( expected = AssertionError.class )
    public void shouldPropertyCheckReadPropertiesUsingTestHarness2() {
        graph.getProperties().on(validPath);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestReadProperties(new Location(validPath), validIdProperty1, validIdProperty2); // wrong!
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
        assertNextRequestIsMove(new Location(validPath), new Location(validIdProperty1, validIdProperty2));
        assertNextRequestIsMove(new Location(validUuid), new Location(createPath(validPathString)));
        assertNoMoreRequests();
    }

    @Test
    public void shouldIgnoreIncompleteRequests() {
        graph.move(validPath); // missing 'into(...)'
        assertNoMoreRequests();

        graph.move(validPath).into(validUuid);
        assertThat(numberOfExecutions, is(1));
        assertNextRequestIsMove(new Location(validPath), new Location(validUuid));
        assertNoMoreRequests();
    }

    // @Test
    // public void shouldBatchMultipleRequests() {
    // // Get the children of one node and the properties of another ...
    // results = graph.batch().readChildren().of("/a/b").and().readProperties().on(validUuid).execute();
    // }

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
            super(sourceName, context);
        }

        @Override
        public void process( CopyBranchRequest request ) {
            // Create a child under the new parent ...
            if (request.into().hasPath()) {
                Name childName = request.desiredName();
                if (childName == null) childName = createName("child");
                Path childPath = context.getValueFactories().getPathFactory().create(request.into().getPath(), childName);
                Location newChild = actualLocationOf(new Location(childPath));
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
            request.setActualLocationOfNode(new Location(childPath));
        }

        @Override
        public void process( DeleteBranchRequest request ) {
            // Just update the actual location
            request.setActualLocationOfNode(actualLocationOf(request.at()));
        }

        @Override
        public void process( MoveBranchRequest request ) {
            // Just update the actual location
            request.setActualLocations(actualLocationOf(request.from()), actualLocationOf(request.into()));
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
        }

        private Location actualLocationOf( Location location ) {
            // If the location has a path, then use the location
            if (location.hasPath()) return location;
            // Otherwise, create a new location with an artificial path ...
            Path path = context.getValueFactories().getPathFactory().create("/a/b/c/d");
            return new Location(path, location.getIdProperties());
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
            executedRequests.add(request);
            ++numberOfExecutions;
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

        public void setListener( RepositorySourceListener listener ) {
        }

    }
}
