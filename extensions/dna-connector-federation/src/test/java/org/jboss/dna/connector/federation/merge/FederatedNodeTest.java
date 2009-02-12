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
package org.jboss.dna.connector.federation.merge;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Path;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

/**
 * @author Randall Hauch
 */
public class FederatedNodeTest {

    private FederatedNode node;
    private Location location;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        location = new Location(mock(Path.class));
        node = new FederatedNode(location, "workspace");
    }

    @Test
    public void shouldHaveSamePathSuppliedToConstructor() {
        assertThat(node.at(), is(sameInstance(location)));
        assertThat(node.getActualLocationOfNode(), is(sameInstance(location)));
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

    // @Test
    // public void shouldHaveDefaultConflictBehaviorUponConstruction() {
    // assertThat(node.getConflictBehavior(), is(FederatedNode.DEFAULT_CONFLICT_BEHAVIOR));
    // }
    //
    // @Test
    // public void shouldAllowSettingConflictBehavior() {
    // NodeConflictBehavior behavior = NodeConflictBehavior.REPLACE;
    // assertThat(node.getConflictBehavior(), is(not(behavior)));
    // node.setConflictBehavior(behavior);
    // assertThat(node.getConflictBehavior(), is(behavior));
    // }
    //
    // @Test
    // public void shouldAllowSettingConflictBehaviorToNull() {
    // node.setConflictBehavior(null);
    // assertThat(node.getConflictBehavior(), is(FederatedNode.DEFAULT_CONFLICT_BEHAVIOR));
    //
    // // Set to something that is not the default, then set to null
    // NodeConflictBehavior behavior = NodeConflictBehavior.REPLACE;
    // assertThat(node.getConflictBehavior(), is(not(behavior)));
    // node.setConflictBehavior(behavior);
    // assertThat(node.getConflictBehavior(), is(behavior));
    // node.setConflictBehavior(null);
    // assertThat(node.getConflictBehavior(), is(FederatedNode.DEFAULT_CONFLICT_BEHAVIOR));
    // }

    @Test
    public void shouldHaveHashCodeThatIsTheHashCodeOfTheLocation() {
        node = new FederatedNode(location, "workspace");
        assertThat(node.hashCode(), is(location.hashCode()));
    }

}
