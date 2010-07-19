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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.Workspace;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.modeshape.graph.Graph;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;

/**
 * 
 */
public class AbstractJcrPropertyTest extends AbstractJcrTest {

    protected AbstractJcrNode rootNode;
    protected AbstractJcrNode cars;
    protected AbstractJcrNode prius;
    protected AbstractJcrNode altima;
    protected AbstractJcrProperty altimaModel;

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();
        rootNode = cache.findJcrRootNode();
        cars = cache.findJcrNode(null, path("/Cars"));
        prius = cache.findJcrNode(null, path("/Cars/Hybrid/Toyota Prius"));
        altima = cache.findJcrNode(null, path("/Cars/Hybrid/Nissan Altima"));
        altimaModel = cache.findJcrProperty(altima.nodeId, altima.path(), Vehicles.Lexicon.MODEL);
    }

    @Test
    public void shouldAllowVisitation() throws Exception {
        ItemVisitor visitor = Mockito.mock(ItemVisitor.class);
        altimaModel.accept(visitor);
        Mockito.verify(visitor).visit(altimaModel);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowVisitationIfNoVisitor() throws Exception {
        altimaModel.accept(null);
    }

    @Test( expected = ItemNotFoundException.class )
    public void shouldNotAllowNegativeAncestorDepth() throws Exception {
        altimaModel.getAncestor(-1);
    }

    @Test
    public void shouldReturnRootForAncestorOfDepthZero() throws Exception {
        assertThat(altimaModel.getAncestor(0), is((Item)rootNode));
    }

    @Test
    public void shouldReturnAncestorAtLevelOneForAncestorOfDepthOne() throws Exception {
        assertThat(altimaModel.getAncestor(1), is((Item)cars));
    }

    @Test
    public void shouldReturnSelfForAncestorOfDepthEqualToDepthOfNode() throws Exception {
        assertThat(altimaModel.getAncestor(altimaModel.getDepth()), is((Item)altimaModel));
        assertThat(altimaModel.getAncestor(altimaModel.getDepth() - 1), is((Item)altima));
    }

    @Test( expected = ItemNotFoundException.class )
    public void shouldFailToReturnAncestorWhenDepthIsGreaterThanNodeDepth() throws Exception {
        altimaModel.getAncestor(40);
    }

    @Test
    public void shouldIndicateIsNotNode() {
        assertThat(altimaModel.isNode(), is(false));
    }

    @Test
    public void shouldProvideExecutionContext() throws Exception {
        assertThat(altimaModel.context(), is(context));
    }

    @Test
    public void shouldProvideName() throws Exception {
        assertThat(altimaModel.getName(), is("vehix:model"));
    }

    @Test
    public void shouldProvideParent() throws Exception {
        assertThat(altimaModel.getParent(), is((Node)altima));
    }

    @Test
    public void shouldProvidePath() throws Exception {
        assertThat(altimaModel.getPath(), is(altima.getPath() + "/vehix:model"));
    }

    @Test
    public void shouldProvideSession() throws Exception {
        assertThat(altimaModel.getSession(), is((Session)jcrSession));
    }

    @Test
    public void shouldReturnFalseFromIsSameIfTheRepositoryInstanceIsDifferent() throws Exception {
        // Set up the store ...
        InMemoryRepositorySource source2 = new InMemoryRepositorySource();
        source2.setName("store");
        Graph store2 = Graph.create(source2, context);
        store2.importXmlFrom(AbstractJcrTest.class.getClassLoader().getResourceAsStream("cars.xml")).into("/");
        JcrSession jcrSession2 = mock(JcrSession.class);
        when(jcrSession2.nodeTypeManager()).thenReturn(nodeTypes);
        when(jcrSession2.isLive()).thenReturn(true);
        SessionCache cache2 = new SessionCache(jcrSession2, store2.getCurrentWorkspaceName(), context, nodeTypes, store2);

        Workspace workspace2 = mock(Workspace.class);
        Repository repository2 = mock(JcrRepository.class);

        when(jcrSession2.getWorkspace()).thenReturn(workspace2);
        when(jcrSession2.getRepository()).thenReturn(repository2);
        when(workspace2.getName()).thenReturn("workspace1");

        // Use the same id and location ...
        javax.jcr.Node prius2 = cache2.findJcrNode(null, path("/Cars/Hybrid/Toyota Prius"));
        assertThat(prius2.isSame(prius), is(false));

        // Check the properties ...
        javax.jcr.Property model = prius.getProperty("vehix:model");
        javax.jcr.Property model2 = prius2.getProperty("vehix:model");
        assertThat(model.isSame(model2), is(false));
    }

    @Test
    public void shouldReturnFalseFromIsSameIfTheWorkspaceNameIsDifferent() throws Exception {
        // Set up the store ...
        InMemoryRepositorySource source2 = new InMemoryRepositorySource();
        source2.setName("store");
        Graph store2 = Graph.create(source2, context);
        store2.importXmlFrom(AbstractJcrTest.class.getClassLoader().getResourceAsStream("cars.xml")).into("/");
        JcrSession jcrSession2 = mock(JcrSession.class);
        when(jcrSession2.nodeTypeManager()).thenReturn(nodeTypes);
        when(jcrSession2.isLive()).thenReturn(true);
        SessionCache cache2 = new SessionCache(jcrSession2, store2.getCurrentWorkspaceName(), context, nodeTypes, store2);

        Workspace workspace2 = mock(Workspace.class);
        JcrRepository repository2 = mock(JcrRepository.class);
        RepositoryLockManager repoLockManager2 = mock(RepositoryLockManager.class);
        when(jcrSession2.getWorkspace()).thenReturn(workspace2);
        when(jcrSession2.getRepository()).thenReturn(repository2);
        when(workspace2.getName()).thenReturn("workspace2");

        WorkspaceLockManager lockManager = new WorkspaceLockManager(context, repoLockManager2, "workspace2", null);
        JcrLockManager jcrLockManager = new JcrLockManager(jcrSession2, lockManager);
        when(jcrSession2.lockManager()).thenReturn(jcrLockManager);

        // Use the same id and location; use 'Toyota Prius'
        // since the UUID is defined in 'cars.xml' and therefore will be the same
        javax.jcr.Node prius2 = cache2.findJcrNode(null, path("/Cars/Hybrid/Toyota Prius"));
        prius2.addMixin("mix:referenceable");
        prius.addMixin("mix:referenceable");
        String priusUuid2 = prius2.getIdentifier();
        String priusUuid = prius.getIdentifier();
        assertThat(priusUuid, is(priusUuid2));
        assertThat(prius2.isSame(prius), is(false));

        // Check the properties ...
        javax.jcr.Property model = prius.getProperty("vehix:model");
        javax.jcr.Property model2 = prius2.getProperty("vehix:model");
        assertThat(model.isSame(model2), is(false));
    }

    @Test
    public void shouldReturnFalseFromIsSameIfTheNodeUuidIsDifferent() throws Exception {
        // Set up the store ...
        InMemoryRepositorySource source2 = new InMemoryRepositorySource();
        source2.setName("store");
        Graph store2 = Graph.create(source2, context);
        store2.importXmlFrom(AbstractJcrTest.class.getClassLoader().getResourceAsStream("cars.xml")).into("/");
        JcrSession jcrSession2 = mock(JcrSession.class);
        when(jcrSession2.nodeTypeManager()).thenReturn(nodeTypes);
        when(jcrSession2.isLive()).thenReturn(true);
        SessionCache cache2 = new SessionCache(jcrSession2, store2.getCurrentWorkspaceName(), context, nodeTypes, store2);

        Workspace workspace2 = mock(Workspace.class);
        JcrRepository repository2 = mock(JcrRepository.class);
        RepositoryLockManager repoLockManager2 = mock(RepositoryLockManager.class);
        when(jcrSession2.getWorkspace()).thenReturn(workspace2);
        when(jcrSession2.getRepository()).thenReturn(repository2);
        when(workspace2.getName()).thenReturn("workspace1");

        WorkspaceLockManager lockManager = new WorkspaceLockManager(context, repoLockManager2, "workspace2", null);
        JcrLockManager jcrLockManager = new JcrLockManager(jcrSession2, lockManager);
        when(jcrSession2.lockManager()).thenReturn(jcrLockManager);

        // Use the same id and location; use 'Nissan Altima'
        // since the UUIDs will be different (cars.xml doesn't define on this node) ...
        javax.jcr.Node altima2 = cache2.findJcrNode(null, path("/Cars/Hybrid/Nissan Altima"));
        altima2.addMixin("mix:referenceable");
        altima.addMixin("mix:referenceable");
        String altimaUuid = altima.getIdentifier();
        String altimaUuid2 = altima2.getIdentifier();
        assertThat(altimaUuid, is(not(altimaUuid2)));
        assertThat(altima2.isSame(altima), is(false));

        // Check the properties ...
        javax.jcr.Property model = altima.getProperty("vehix:model");
        javax.jcr.Property model2 = altima2.getProperty("vehix:model");
        assertThat(model.isSame(model2), is(false));
    }

    @Test
    public void shouldReturnTrueFromIsSameIfTheNodeUuidAndWorkspaceNameAndRepositoryInstanceAreSame() throws Exception {
        // Set up the store ...
        InMemoryRepositorySource source2 = new InMemoryRepositorySource();
        source2.setName("store");
        Graph store2 = Graph.create(source2, context);
        store2.importXmlFrom(AbstractJcrTest.class.getClassLoader().getResourceAsStream("cars.xml")).into("/");
        JcrSession jcrSession2 = mock(JcrSession.class);
        when(jcrSession2.nodeTypeManager()).thenReturn(nodeTypes);
        when(jcrSession2.isLive()).thenReturn(true);
        SessionCache cache2 = new SessionCache(jcrSession2, store2.getCurrentWorkspaceName(), context, nodeTypes, store2);

        Workspace workspace2 = mock(Workspace.class);
        when(jcrSession2.getWorkspace()).thenReturn(workspace2);
        when(jcrSession2.getRepository()).thenReturn(repository);
        when(workspace2.getName()).thenReturn("workspace1");

        WorkspaceLockManager lockManager = new WorkspaceLockManager(context, repoLockManager, "workspace2", null);
        JcrLockManager jcrLockManager = new JcrLockManager(jcrSession2, lockManager);
        when(jcrSession2.lockManager()).thenReturn(jcrLockManager);

        // Use the same id and location ...
        javax.jcr.Node prius2 = cache2.findJcrNode(null, path("/Cars/Hybrid/Toyota Prius"));
        prius2.addMixin("mix:referenceable");
        prius.addMixin("mix:referenceable");
        String priusUuid = prius.getIdentifier();
        String priusUuid2 = prius2.getIdentifier();
        assertThat(priusUuid, is(priusUuid2));
        assertThat(prius2.isSame(prius), is(true));

        // Check the properties ...
        javax.jcr.Property model = prius.getProperty("vehix:model");
        javax.jcr.Property model2 = prius2.getProperty("vehix:model");
        javax.jcr.Property year2 = prius2.getProperty("vehix:year");
        assertThat(model.isSame(model2), is(true));
        assertThat(model.isSame(year2), is(false));
    }

}
