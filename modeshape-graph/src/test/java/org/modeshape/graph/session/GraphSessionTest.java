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
package org.modeshape.graph.session;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.session.GraphSession.Node;
import org.modeshape.graph.session.GraphSession.Operations;

/**
 * 
 */
public class GraphSessionTest {

    private static final Stopwatch LOADING_STOPWATCH = new Stopwatch();

    protected ExecutionContext context;
    protected InMemoryRepositorySource source;
    private Graph store;
    private GraphSession<Object, Object> cache;
    private PathFactory pathFactory;
    protected int numberOfConnections;

    @Before
    public void beforeEach() throws Exception {
        context = new ExecutionContext();
        pathFactory = context.getValueFactories().getPathFactory();
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

        RepositoryContext repositoryContext = new RepositoryContext() {
            public ExecutionContext getExecutionContext() {
                return context;
            }

            public Observer getObserver() {
                return null;
            }

            public RepositoryConnectionFactory getRepositoryConnectionFactory() {
                return null;
            }

            public Subgraph getConfiguration( int depth ) {
                Graph result = Graph.create(source, context);
                result.useWorkspace("configSpace");
                return result.getSubgraphOfDepth(depth).at("/");
            }
        };
        source.initialize(repositoryContext);

        store = Graph.create(source.getName(), connectionFactory, context);

        // Load the store with content ...
        LOADING_STOPWATCH.start();
        store.importXmlFrom(getClass().getClassLoader().getResourceAsStream("cars.xml")).into("/");
        LOADING_STOPWATCH.stop();
        numberOfConnections = 0; // reset the number of connections

        Operations<Object, Object> nodeOps = null; // use default
        String workspaceName = null; // use current
        cache = new GraphSession<Object, Object>(store, workspaceName, nodeOps);
    }

    @AfterClass
    public static void afterAll() {
        // System.out.println(LOADING_STOPWATCH);
    }

    @Test
    public void shouldHaveRootNodeWithCorrectNodeIdAndLocation() {
        Node<Object, Object> node = cache.getRoot();
        assertThat(node, is(notNullValue()));
        assertThat(node.getNodeId(), is(notNullValue()));
        assertThat(node.getLocation(), is(notNullValue()));
        assertNoMoreConnectionsUsed();
        assertNoChanges();
    }

    @Test
    public void shouldNotHaveAutomaticallyLoadedRootNode() {
        assertThat(cache.getRoot().isLoaded(), is(false));
        assertNoMoreConnectionsUsed();
        assertNoChanges();
    }

    @Test
    public void shouldHaveRootNodeWithChildren() {
        Node<Object, Object> node = cache.getRoot();
        assertChildren(node, "Cars");
        assertConnectionsUsed(1);
        assertNoChanges();
    }

    @Test
    public void shouldHaveNoExpirationIfSourceDoesNotHaveCachePolicy() {
        Node<Object, Object> node = cache.getRoot();
        node.load();
        assertThat(node.getExpirationTimeInMillis(), is(Long.MAX_VALUE));
        assertConnectionsUsed(1);
        assertNoChanges();
    }

    @Test
    public void shouldAutomaticallyLoadNodesWhenNavigatingToChildren() {
        Node<Object, Object> root = cache.getRoot();
        assertThat(root.isLoaded(), is(false));
        assertChildren(root, "Cars"); // causes loading of the root node
        assertThat(root.isLoaded(), is(true));
        assertConnectionsUsed(1);

        Node<Object, Object> cars = root.getChildren().iterator().next(); // only one child
        assertThat(cars.isLoaded(), is(false));
        assertChildren(cars, "Hybrid", "Sports", "Luxury", "Utility");
        assertConnectionsUsed(1);
        assertThat(cars.isLoaded(), is(true));
        assertThat(cars.getParent(), is(sameInstance(root)));

        Node<Object, Object> sports = cars.getChild(segment("Sports"));
        assertThat(sports.isLoaded(), is(false));
        assertChildren(sports, "Aston Martin DB9", "Infiniti G37");
        assertConnectionsUsed(1);
        assertThat(sports.isLoaded(), is(true));
        assertThat(sports.getParent(), is(sameInstance(cars)));

        Node<Object, Object> g37 = sports.getChild(segment("Infiniti G37"));
        assertThat(g37.isLoaded(), is(false));
        assertChildren(g37);
        assertConnectionsUsed(1);
        assertThat(g37.isLoaded(), is(true));
        assertThat(g37.isLeaf(), is(true));
        assertThat(g37.getParent(), is(sameInstance(sports)));

        // Try another branch ...
        Node<Object, Object> utility = cars.getChild(segment("Utility"));
        assertThat(utility.isLoaded(), is(false));
        assertChildren(utility, "Land Rover LR2", "Land Rover LR3", "Hummer H3", "Ford F-150");
        assertConnectionsUsed(1);
        assertThat(utility.isLoaded(), is(true));
        assertThat(utility.getParent(), is(sameInstance(cars)));

        Node<Object, Object> lr3 = utility.getChild(segment("Land Rover LR3"));
        assertThat(lr3.isLoaded(), is(false));
        assertChildren(lr3);
        assertConnectionsUsed(1);
        assertThat(lr3.isLoaded(), is(true));
        assertThat(lr3.isLeaf(), is(true));
        assertThat(lr3.getParent(), is(sameInstance(utility)));

        assertNoMoreConnectionsUsed();
        assertNoChanges();
    }

    @Test
    public void shouldFindNodesByPath() {
        Node<Object, Object> g37 = cache.findNodeWith(path("/Cars/Sports/Infiniti G37")); // loads the node and all parents
        assertConnectionsUsed(1);
        assertThat(g37.isLoaded(), is(true));
        assertChildren(g37);
        assertThat(g37.isLoaded(), is(true));
        assertThat(g37.isLeaf(), is(true));

        Node<Object, Object> sports = g37.getParent();
        assertThat(sports.isLoaded(), is(true));
        assertChildren(sports, "Aston Martin DB9", "Infiniti G37");

        Node<Object, Object> cars = sports.getParent();
        assertThat(cars.isLoaded(), is(true));
        assertChildren(cars, "Hybrid", "Sports", "Luxury", "Utility");

        Node<Object, Object> root = cars.getParent();
        assertThat(root, is(sameInstance(cache.getRoot())));
        assertThat(root.isLoaded(), is(true));
        assertChildren(root, "Cars"); // causes loading of the root node

        assertNoMoreConnectionsUsed();

        // Try another branch that should not be loaded...
        Node<Object, Object> utility = cars.getChild(segment("Utility"));
        assertThat(utility.isLoaded(), is(false));
        assertChildren(utility, "Land Rover LR2", "Land Rover LR3", "Hummer H3", "Ford F-150");
        assertConnectionsUsed(1);
        assertThat(utility.isLoaded(), is(true));
        assertThat(utility.getParent(), is(sameInstance(cars)));

        assertNoMoreConnectionsUsed();
        assertNoChanges();
    }

    @Test
    public void shouldFindNodesById() {
        cache.findNodeWith(path("/Cars/Sports/Infiniti G37")); // loads the node and all parents
        assertConnectionsUsed(1); // "Utility" was found because it is child of "Cars" loaded when "Sports" was loaded

        cache.getRoot().onCachedNodes(new GraphSession.NodeVisitor<Object, Object>() {
            @SuppressWarnings( "synthetic-access" )
            @Override
            public boolean visit( Node<Object, Object> node ) {
                assertThat(cache.findNodeWith(node.getNodeId(), null), is(sameInstance(node)));
                return true;
            }
        });
    }

    @Test
    public void shouldFindNodesByLocation() {
        cache.findNodeWith(path("/Cars/Sports/Infiniti G37")); // loads the node and all parents
        assertConnectionsUsed(1); // "Utility" was found because it is child of "Cars" loaded when "Sports" was loaded

        cache.getRoot().onCachedNodes(new GraphSession.NodeVisitor<Object, Object>() {
            @SuppressWarnings( "synthetic-access" )
            @Override
            public boolean visit( Node<Object, Object> node ) {
                assertThat(cache.findNodeWith(null, node.getLocation().getPath()), is(sameInstance(node)));
                return true;
            }
        });
    }

    @Test
    public void shouldFindNodesByIdAfterClearing() {
        Node<Object, Object> g37 = cache.findNodeWith(path("/Cars/Sports/Infiniti G37")); // loads the node and all parents
        assertConnectionsUsed(1);
        assertThat(g37.isLoaded(), is(true));
        assertChildren(g37);
        assertThat(g37.isLoaded(), is(true));
        assertThat(g37.isLeaf(), is(true));

        Node<Object, Object> sports = g37.getParent();
        Node<Object, Object> cars = sports.getParent();
        Node<Object, Object> root = cars.getParent();

        assertNoMoreConnectionsUsed();

        cache.getRoot().unload();

        Node<Object, Object> g37_b = cache.findNodeWith(g37.getNodeId(), g37.getPath());
        assertConnectionsUsed(1);

        Node<Object, Object> sports_b = g37_b.getParent();
        Node<Object, Object> cars_b = sports_b.getParent();
        Node<Object, Object> root_b = cars_b.getParent();
        assertThat(g37_b, is(not(sameInstance(g37))));
        assertThat(sports_b, is(not(sameInstance(sports))));
        assertThat(cars_b, is(not(sameInstance(cars))));
        assertThat(root_b, is(sameInstance(root)));
        assertThat(g37_b.isLeaf(), is(true));
        assertChildren(sports_b, "Aston Martin DB9", "Infiniti G37");
        assertChildren(cars_b, "Hybrid", "Sports", "Luxury", "Utility");
        assertChildren(root_b, "Cars");

        assertNoMoreConnectionsUsed();
        assertNoChanges();
    }

    @Test
    public void shouldMoveBranchAndRefresh() {
        Node<Object, Object> sports = cache.findNodeWith(path("/Cars/Sports"));
        Node<Object, Object> utility = cache.findNodeWith(path("/Cars/Utility"));
        assertConnectionsUsed(1); // "Utility" was found because it is child of "Cars" loaded when "Sports" was loaded

        sports.moveTo(utility);
        assertConnectionsUsed(1); // "Utility" was not fully loaded before
        assertNoMoreConnectionsUsed();

        Node<Object, Object> cars = cache.findNodeWith(path("/Cars"));

        for (int i = 0; i != 3; ++i) {
            assertChildren(cars, "Hybrid", "Luxury", "Utility");
            assertChildren(utility, "Land Rover LR2", "Land Rover LR3", "Hummer H3", "Ford F-150", "Sports");
            assertThat(sports.getParent(), is(utility));
            assertThat(utility.getParent(), is(cars));

            // Ensure that the changes were recorded appropriately ...
            assertThat(cars.isChanged(false), is(true)); // 'sports' removed as child
            assertThat(utility.isChanged(false), is(true)); // 'sports' added as child
            assertThat(sports.isChanged(false), is(true)); // path has changed
            assertThat(cache.hasPendingChanges(), is(true));
            assertThat(cache.changeDependencies.size(), is(1));
            assertThat(cache.changeDependencies.get(sports.getNodeId()).getMovedFrom(), is(cars.getNodeId()));
            assertThat(cache.operations.isExecuteRequired(), is(true));

            if (i == 0) {
                // 1st time: Refreshing "Utility" shouldn't work because "/Cars" was involved in the same move ...
                try {
                    cache.refresh(utility, false);
                    fail("Expected exception from the call to refresh");
                } catch (InvalidStateException e) {
                    // expected ...
                }
            } else if (i == 1) {
                // 2nd time: refresh "/Cars" but keep the changes ...
                cache.refresh(cars, true);
            } else if (i == 2) {
                // 3rd time:
                cache.refresh(cars, false);
            }
        }

        // Now the state should be back to the original representation, but we need to refind the nodes ...
        sports = cache.findNodeWith(path("/Cars/Sports"));
        utility = cache.findNodeWith(path("/Cars/Utility"));
        cars = cache.findNodeWith(path("/Cars"));

        assertChildren(cars, "Hybrid", "Sports", "Luxury", "Utility");
        assertChildren(utility, "Land Rover LR2", "Land Rover LR3", "Hummer H3", "Ford F-150");
        assertThat(sports.getParent(), is(cars));
        assertThat(utility.getParent(), is(cars));

        // Now there should be no changes ...
        assertNoChanges();
        // System.out.println(cache.root.getSnapshot(false));
    }

    @Test
    public void shouldMoveBranchAndSaveBranch() {
        Node<Object, Object> sports = cache.findNodeWith(path("/Cars/Sports"));
        Node<Object, Object> utility = cache.findNodeWith(path("/Cars/Utility"));
        assertConnectionsUsed(1); // "Utility" was found because it is child of "Cars" loaded when "Sports" was loaded

        sports.moveTo(utility);
        assertConnectionsUsed(1); // "Utility" was not fully loaded before
        assertNoMoreConnectionsUsed();

        Node<Object, Object> cars = cache.findNodeWith(path("/Cars"));
        Node<Object, Object> root = cache.getRoot();

        for (int i = 0; i != 2; ++i) {
            assertChildren(cars, "Hybrid", "Luxury", "Utility");
            assertChildren(utility, "Land Rover LR2", "Land Rover LR3", "Hummer H3", "Ford F-150", "Sports");
            assertThat(sports.getParent(), is(utility));
            assertThat(utility.getParent(), is(cars));

            // Ensure that the changes were recorded appropriately ...
            assertThat(cars.isChanged(false), is(true)); // 'sports' removed as child
            assertThat(utility.isChanged(false), is(true)); // 'sports' added as child
            assertThat(sports.isChanged(false), is(true)); // path has changed
            assertThat(cache.hasPendingChanges(), is(true));
            assertThat(cache.changeDependencies.size(), is(1));
            assertThat(cache.changeDependencies.get(sports.getNodeId()).getMovedFrom(), is(cars.getNodeId()));
            assertThat(cache.operations.isExecuteRequired(), is(true));

            if (i == 0) {
                // 1st time: Saving "Utility" shouldn't work because "/Cars" was involved in the same move ...
                try {
                    cache.save(utility);
                    fail("Expected exception from the call to save");
                } catch (ValidationException e) {
                    // expected ...
                }
            } else if (i == 1) {
                // 2nd time: Save "/Cars" but keep the changes ...
                assertConnectionsUsed(0);
                cache.save(cars);
                assertConnectionsUsed(2); // 1 to load children required by validation, 1 to perform save
            }
            // i=2 do nothing
        }

        // The affected nodes should now be stale ...
        assertThat(sports.isStale(), is(true));
        assertThat(utility.isStale(), is(true));
        assertThat(cars.isStale(), is(false)); // not stale because it was unloaded
        assertThat(cars.isLoaded(), is(false));
        assertThat(root.isStale(), is(false)); // not touched during saves

        // Now the state should reflect our changes, but we need to refind the nodes ...
        sports = cache.findNodeWith(path("/Cars/Utility/Sports"));
        assertConnectionsUsed(1);
        utility = cache.findNodeWith(path("/Cars/Utility"));
        assertNoMoreConnectionsUsed();

        assertChildren(cars, "Hybrid", "Luxury", "Utility");
        assertChildren(utility, "Land Rover LR2", "Land Rover LR3", "Hummer H3", "Ford F-150", "Sports");
        assertThat(sports.getParent(), is(utility));
        assertThat(utility.getParent(), is(cars));

        // Now there should be no changes ...
        assertNoChanges();

        // System.out.println(cache.root.getSnapshot(false));
    }

    @Test
    public void shouldMoveBranchAndSaveAll() {
        Node<Object, Object> sports = cache.findNodeWith(path("/Cars/Sports"));
        Node<Object, Object> utility = cache.findNodeWith(path("/Cars/Utility"));
        assertConnectionsUsed(1); // "Utility" was found because it is child of "Cars" loaded when "Sports" was loaded

        sports.moveTo(utility);
        assertConnectionsUsed(1); // "Utility" was not fully loaded before
        assertNoMoreConnectionsUsed();

        Node<Object, Object> cars = cache.findNodeWith(path("/Cars"));
        Node<Object, Object> root = cache.getRoot();

        assertChildren(cars, "Hybrid", "Luxury", "Utility");
        assertChildren(utility, "Land Rover LR2", "Land Rover LR3", "Hummer H3", "Ford F-150", "Sports");
        assertThat(sports.getParent(), is(utility));
        assertThat(utility.getParent(), is(cars));

        // Ensure that the changes were recorded appropriately ...
        assertThat(cars.isChanged(false), is(true)); // 'sports' removed as child
        assertThat(utility.isChanged(false), is(true)); // 'sports' added as child
        assertThat(sports.isChanged(false), is(true)); // path has changed
        assertThat(cache.hasPendingChanges(), is(true));
        assertThat(cache.changeDependencies.size(), is(1));
        assertThat(cache.changeDependencies.get(sports.getNodeId()).getMovedFrom(), is(cars.getNodeId()));
        assertThat(cache.operations.isExecuteRequired(), is(true));

        // Save the changes ...
        assertConnectionsUsed(0);
        cache.save();
        assertConnectionsUsed(2); // 1 to load children required by validation, 1 to perform save

        // The affected nodes should now be stale ...
        assertThat(sports.isStale(), is(true));
        assertThat(utility.isStale(), is(true));
        assertThat(cars.isStale(), is(true));
        assertThat(cars.isLoaded(), is(false));
        assertThat(root.isStale(), is(false)); // not touched during saves

        // Now the state should reflect our changes, but we need to refind the nodes ...
        sports = cache.findNodeWith(path("/Cars/Utility/Sports"));
        assertConnectionsUsed(1);
        utility = cache.findNodeWith(path("/Cars/Utility"));
        cars = cache.findNodeWith(path("/Cars"));
        assertNoMoreConnectionsUsed();

        assertChildren(cars, "Hybrid", "Luxury", "Utility");
        assertChildren(utility, "Land Rover LR2", "Land Rover LR3", "Hummer H3", "Ford F-150", "Sports");
        assertThat(sports.getParent(), is(utility));
        assertThat(utility.getParent(), is(cars));

        // Now there should be no changes ...
        assertNoChanges();

        // System.out.println(cache.root.getSnapshot(false));
    }

    @Test
    public void shouldRenameNodeByRemovingAndAddingAtEndOfChildren() {
        Node<Object, Object> sports = cache.findNodeWith(path("/Cars/Sports"));
        assertConnectionsUsed(1); // "Utility" was found because it is child of "Cars" loaded when "Sports" was loaded

        sports.rename(name("non-sports")); // "Sports" was already loaded, as was "Cars"
        assertNoMoreConnectionsUsed();

        Node<Object, Object> cars = cache.findNodeWith(path("/Cars"));

        assertChildren(cars, "Hybrid", "Luxury", "Utility", "non-sports");
        assertThat(sports.getParent(), is(cars));

        // Ensure that the changes were recorded appropriately ...
        assertThat(cars.isChanged(false), is(true)); // 'sports' renamed as child
        assertThat(sports.isChanged(false), is(true)); // path has changed
        assertThat(cache.hasPendingChanges(), is(true));
        assertThat(cache.operations.isExecuteRequired(), is(true));

        // Save "/Cars" but keep the changes ...
        assertConnectionsUsed(0);
        cache.save(cars);
        assertConnectionsUsed(2); // 1 to load children required by validation, 1 to perform save

        // Now the state should reflect our changes, but we need to refind the nodes ...
        Node<Object, Object> nonSports = cache.findNodeWith(path("/Cars/non-sports"));
        assertConnectionsUsed(1);
        assertNoMoreConnectionsUsed();

        assertChildren(cars, "Hybrid", "Luxury", "Utility", "non-sports");
        assertThat(nonSports.getParent(), is(cars));

        // Now there should be no changes ...
        assertNoChanges();

        // System.out.println(cache.root.getSnapshot(false));
    }

    @Test
    public void shouldRenameNodeByRemovingAndAddingAtEndOfChildrenEvenWithSameNameSiblings() {
        Node<Object, Object> sports = cache.findNodeWith(path("/Cars/Sports"));
        assertConnectionsUsed(1); // "Utility" was found because it is child of "Cars" loaded when "Sports" was loaded

        sports.rename(name("Utility")); // "Sports" was already loaded, as was "Cars"
        assertNoMoreConnectionsUsed();

        Node<Object, Object> cars = cache.findNodeWith(path("/Cars"));

        assertChildren(cars, "Hybrid", "Luxury", "Utility", "Utility[2]");
        assertThat(sports.getParent(), is(cars));

        // Ensure that the changes were recorded appropriately ...
        assertThat(cars.isChanged(false), is(true)); // 'sports' renamed as child
        assertThat(sports.isChanged(false), is(true)); // path has changed
        assertThat(cache.hasPendingChanges(), is(true));
        assertThat(cache.operations.isExecuteRequired(), is(true));

        // Save "/Cars" but keep the changes ...
        assertConnectionsUsed(0);
        cache.save(cars);
        assertConnectionsUsed(2); // 1 to load children required by validation, 1 to perform save

        // Now the state should reflect our changes, but we need to refind the nodes ...
        Node<Object, Object> utility2 = cache.findNodeWith(path("/Cars/Utility[2]"));
        assertConnectionsUsed(1);
        assertNoMoreConnectionsUsed();

        assertChildren(cars, "Hybrid", "Luxury", "Utility", "Utility[2]");
        assertThat(utility2.getParent(), is(cars));

        // Now there should be no changes ...
        assertNoChanges();

        // System.out.println(cache.root.getSnapshot(false));
    }

    @Test
    public void shouldReorderChildWithNoSnsIndexes() {
        Node<Object, Object> sports = cache.findNodeWith(path("/Cars/Sports"));
        Node<Object, Object> utility = cache.findNodeWith(path("/Cars/Utility"));
        Node<Object, Object> cars = cache.findNodeWith(path("/Cars"));
        assertConnectionsUsed(1); // "Utility" was found because it is child of "Cars" loaded when "Sports" was loaded

        assertChildren(cars, "Hybrid", "Sports", "Luxury", "Utility");

        cars.orderChildBefore(utility.getSegment(), sports.getSegment());
        assertNoMoreConnectionsUsed();

        Node<Object, Object> root = cache.getRoot();

        assertChildren(cars, "Hybrid", "Utility", "Sports", "Luxury");
        assertThat(sports.getParent(), is(cars));
        assertThat(utility.getParent(), is(cars));

        // Save the changes ...
        assertConnectionsUsed(0);
        cache.save();
        assertConnectionsUsed(2); // 1 to load children required by validation, 1 to perform save

        // The affected nodes should now be stale ...
        assertThat(sports.isStale(), is(true));
        assertThat(utility.isStale(), is(true));
        assertThat(cars.isStale(), is(true));
        assertThat(cars.isLoaded(), is(false));
        assertThat(root.isStale(), is(false)); // not touched during saves

        // Now the state should reflect our changes ...
        cars = cache.findNodeWith(path("/Cars"));
        assertChildren(cars, "Hybrid", "Utility", "Sports", "Luxury");

        // Now there should be no changes ...
        assertNoChanges();
    }

    @Test
    public void shouldReorderChildWithSnsIndexes() {
        Node<Object, Object> sports = cache.findNodeWith(path("/Cars/Sports"));
        Node<Object, Object> cars = cache.findNodeWith(path("/Cars"));
        assertConnectionsUsed(1); // "Utility" was found because it is child of "Cars" loaded when "Sports" was loaded

        Node<Object, Object> exp1 = cars.createChild(name("Experimental"));
        Node<Object, Object> exp2 = cars.createChild(name("Experimental"));
        Node<Object, Object> exp3 = cars.createChild(name("Experimental"));
        assertThat(cache.hasPendingChanges(), is(true));
        assertThat(exp1.getSegment().getIndex(), is(1));
        assertThat(exp2.getSegment().getIndex(), is(2));
        assertThat(exp3.getSegment().getIndex(), is(3));
        assertChildren(cars, "Hybrid", "Sports", "Luxury", "Utility", "Experimental", "Experimental[2]", "Experimental[3]");

        cars.orderChildBefore(exp3.getSegment(), sports.getSegment());
        assertNoMoreConnectionsUsed();

        assertThat(exp1.getSegment().getIndex(), is(2));
        assertThat(exp2.getSegment().getIndex(), is(3));
        assertThat(exp3.getSegment().getIndex(), is(1));
        assertChildren(cars, "Hybrid", "Experimental", "Sports", "Luxury", "Utility", "Experimental[2]", "Experimental[3]");

        // Save the changes ...
        assertConnectionsUsed(0);
        cache.save();
        assertConnectionsUsed(2); // 1 to load children required by validation, 1 to perform save

        // Now the state should reflect our changes ...
        cars = cache.findNodeWith(path("/Cars"));
        assertChildren(cars, "Hybrid", "Experimental", "Sports", "Luxury", "Utility", "Experimental[2]", "Experimental[3]");

        // Now there should be no changes ...
        assertNoChanges();
    }

    @Test
    public void shouldLoadSubgraphs() {
        cache.setDepthForLoadingNodes(4);
        Node<Object, Object> cars = cache.findNodeWith(path("/Cars")); // loads the node and all parents
        assertChildren(cars, "Hybrid", "Sports", "Luxury", "Utility");
        assertConnectionsUsed(1);

        Node<Object, Object> sports = cache.findNodeWith(path("/Cars/Sports"));
        Node<Object, Object> g37 = cache.findNodeWith(path("/Cars/Sports/Infiniti G37"));
        Node<Object, Object> db9 = cache.findNodeWith(path("/Cars/Sports/Aston Martin DB9"));
        assertConnectionsUsed(0); // should have been loaded with sports subgraph

        assertThat(sports.isLoaded(), is(true));
        assertThat(g37.isLoaded(), is(true));
        assertThat(db9.isLoaded(), is(true));
        assertChildren(sports, "Aston Martin DB9", "Infiniti G37");
        assertChildren(g37);
        assertChildren(db9);

        assertNoMoreConnectionsUsed();
        assertNoChanges();
    }

    @Test
    public void shouldMarkAsChangedWhenSettingProperties() {
        Node<Object, Object> g37 = cache.findNodeWith(path("/Cars/Sports/Infiniti G37"));
        assertThat(g37.isChanged(false), is(false));

        // Set the new property ...
        Property newProperty = createProperty("something", "value1");
        g37.setProperty(newProperty, false, null);
        assertThat(g37.isChanged(false), is(true));
        assertThat(cache.getRoot().isChanged(true), is(true));

        // Save the changes ...
        cache.save();
    }

    @Test
    public void shouldClearPropertyChangesWhenRefreshing() {
        Node<Object, Object> g37 = cache.findNodeWith(path("/Cars/Sports/Infiniti G37"));
        assertThat(g37.isChanged(false), is(false));

        // Set the new property ...
        Property newProperty = createProperty("something", "value1");
        g37.setProperty(newProperty, false, null);
        assertThat(g37.isChanged(false), is(true));
        assertThat(cache.getRoot().isChanged(true), is(true));

        // Refresh the changes ...
        cache.refresh(g37, false);

        assertThat(g37.isChanged(true), is(false));
        assertThat(cache.getRoot().isChanged(true), is(false));
        assertNoChanges();
    }

    @Test
    public void shouldCreateChildren() {
        Node<Object, Object> cars = cache.findNodeWith(path("/Cars")); // loads the node and all parents
        assertChildren(cars, "Hybrid", "Sports", "Luxury", "Utility");
        assertConnectionsUsed(1);

        Node<Object, Object> experimental = cars.createChild(name("Experimental"));
        assertThat(experimental.getParent(), is(sameInstance(cars)));
        assertChildren(cars, "Hybrid", "Sports", "Luxury", "Utility", "Experimental");
        assertThat(cars.isChanged(false), is(true));
        assertThat(experimental.isNew(), is(true));
        assertNoMoreConnectionsUsed();

        Node<Object, Object> experimental2 = cars.createChild(name("Experimental"));
        assertThat(experimental2.getParent(), is(sameInstance(cars)));
        assertChildren(cars, "Hybrid", "Sports", "Luxury", "Utility", "Experimental[1]", "Experimental[2]");
        assertThat(cars.isChanged(false), is(true));
        assertThat(experimental2.isNew(), is(true));
        assertNoMoreConnectionsUsed();
    }

    protected void assertChildren( Node<Object, Object> node,
                                   String... childNames ) {
        assertThat(node.getChildrenCount(), is(childNames.length));
        List<Path.Segment> segments = new LinkedList<Path.Segment>();
        for (String childName : childNames) {
            segments.add(pathFactory.createSegment(childName));
        }
        Iterator<Path.Segment> expectedIter = segments.iterator();
        Iterator<Node<Object, Object>> actualIter = node.getChildren().iterator();
        while (expectedIter.hasNext() && actualIter.hasNext()) {
            Node<Object, Object> actualNode = actualIter.next();
            Path actualPath = actualNode.getPath();
            Path.Segment actualSegment = actualPath.getLastSegment();
            Path.Segment expectedSegment = expectedIter.next();
            assertThat(actualSegment, is(expectedSegment));
        }
        assertThat(expectedIter.hasNext(), is(false));
        assertThat(actualIter.hasNext(), is(false));
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldThrowPathNotFoundExceptionWhenFailingToFindDeepMissingNode() throws Exception {
        this.cache.findNodeRelativeTo(cache.getRoot(), path("Cars/some/node/that/does/not/exist"), true);
    }

    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    protected Path.Segment segment( String segment ) {
        return context.getValueFactories().getPathFactory().createSegment(segment);
    }

    protected Path path( String path ) {
        return context.getValueFactories().getPathFactory().create(path);
    }

    protected Property createProperty( String name,
                                       Object... values ) {
        return context.getPropertyFactory().create(name(name), values);
    }

    protected void assertChildrenNotLoaded( Node<Object, Object> node ) {
        for (Node<Object, Object> child : node.getChildren()) {
            assertThat(child.isLoaded(), is(false));
        }
    }

    protected void assertNoChanges() {
        assertThat(cache.hasPendingChanges(), is(false));
        assertThat(cache.changeDependencies.isEmpty(), is(true));
        assertThat(cache.operations.isExecuteRequired(), is(false));
    }

    protected void assertNoMoreConnectionsUsed() {
        assertThat(numberOfConnections, is(0));
    }

    protected void assertConnectionsUsed( int number ) {
        assertThat(numberOfConnections, is(number));
        numberOfConnections = 0;
    }

}
