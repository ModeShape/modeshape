/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.connector.federation.merge;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import java.util.UUID;
import org.jboss.dna.graph.commands.NodeConflictBehavior;
import org.jboss.dna.graph.properties.Path;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class FederatedNodeTest {

    private FederatedNode node;
    private UUID uuid;
    @Mock
    private Path path;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        uuid = UUID.randomUUID();
        node = new FederatedNode(path, uuid);
    }

    @Test
    public void shouldHaveSameUuidSuppliedToConstructor() {
        assertThat(node.getUuid(), is(sameInstance(uuid)));
    }

    @Test
    public void shouldHaveSamePathSuppliedToConstructor() {
        assertThat(node.getPath(), is(sameInstance(path)));
    }

    @Test
    public void shouldNotHaveMergePlanUponConstruction() {
        assertThat(node.getMergePlan(), is(nullValue()));
    }

    @Test
    public void shouldAllowSettingMergePlan() {
        MergePlan mergePlan = mock(MergePlan.class);
        node.setMergePlan(mergePlan);
        assertThat(node.getMergePlan(), is(sameInstance(mergePlan)));
    }

    @Test
    public void shouldHaveDefaultConflictBehaviorUponConstruction() {
        assertThat(node.getConflictBehavior(), is(FederatedNode.DEFAULT_CONFLICT_BEHAVIOR));
    }

    @Test
    public void shouldAllowSettingConflictBehavior() {
        NodeConflictBehavior behavior = NodeConflictBehavior.REPLACE;
        assertThat(node.getConflictBehavior(), is(not(behavior)));
        node.setConflictBehavior(behavior);
        assertThat(node.getConflictBehavior(), is(behavior));
    }

    @Test
    public void shouldAllowSettingConflictBehaviorToNull() {
        node.setConflictBehavior(null);
        assertThat(node.getConflictBehavior(), is(FederatedNode.DEFAULT_CONFLICT_BEHAVIOR));

        // Set to something that is not the default, then set to null
        NodeConflictBehavior behavior = NodeConflictBehavior.REPLACE;
        assertThat(node.getConflictBehavior(), is(not(behavior)));
        node.setConflictBehavior(behavior);
        assertThat(node.getConflictBehavior(), is(behavior));
        node.setConflictBehavior(null);
        assertThat(node.getConflictBehavior(), is(FederatedNode.DEFAULT_CONFLICT_BEHAVIOR));
    }

    @Test
    public void shouldCompareFederatedNodesBasedUponPaths() {
        Path path2 = mock(Path.class);
        stub(path2.compareTo(path)).toReturn(1);
        stub(path.compareTo(path2)).toReturn(-1);
        FederatedNode node2 = new FederatedNode(path2, UUID.randomUUID());
        assertThat(node.compareTo(node2), is(-1));
        assertThat(node2.compareTo(node), is(1));
    }

    @Test
    public void shouldHaveHashCodeThatIsTheHashCodeOfTheUuid() {
        node = new FederatedNode(path, uuid);
        assertThat(node.hashCode(), is(uuid.hashCode()));
    }

}
