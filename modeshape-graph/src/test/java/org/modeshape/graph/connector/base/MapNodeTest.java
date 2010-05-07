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
package org.modeshape.graph.connector.base;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.Path.Segment;

public class MapNodeTest {

    private MapNode node;
    private ExecutionContext context;

    @Before
    public void beforeEach() {
        context = new ExecutionContext();

        UUID uuid = UUID.randomUUID();
        Segment name = segment("myNode[3]");
        UUID parent = UUID.randomUUID();
        List<Property> props = properties(property("propA", "valueA"), property("propB", "valueB"));
        node = new MapNode(uuid, name, parent, props, null);
    }

    public Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    public Segment segment( String segment ) {
        return context.getValueFactories().getPathFactory().createSegment(segment);
    }

    public Property property( String name,
                              Object... values ) {
        return context.getPropertyFactory().create(name(name), values);
    }

    public List<Property> properties( Property... properties ) {
        return Arrays.asList(properties);
    }

    @Test
    public void shouldInstantiateNodeWithUuid() {
        UUID uuid = UUID.randomUUID();
        node = new MapNode(uuid);
        assertThat(node.getUuid(), is(sameInstance(uuid)));
        assertThat(node.getParent(), is(nullValue()));
        assertThat(node.getName(), is(nullValue()));
        assertThat(node.getChildren().isEmpty(), is(true));
        assertThat(node.getProperties().isEmpty(), is(true));
    }

    @Test
    public void shouldInstantiateNodeWithUuidAndNameAndParent() {
        UUID uuid = UUID.randomUUID();
        Segment name = segment("myNode[3]");
        UUID parent = UUID.randomUUID();
        node = new MapNode(uuid, name, parent, (Map<Name, Property>)null, null);
        assertThat(node.getUuid(), is(sameInstance(uuid)));
        assertThat(node.getParent(), is(parent));
        assertThat(node.getName(), is(name));
        assertThat(node.getChildren().isEmpty(), is(true));
        assertThat(node.getProperties().isEmpty(), is(true));
    }

    @Test
    public void shouldInstantiateNodeWithUuidAndNoNameOrParent() {
        UUID uuid = UUID.randomUUID();
        node = new MapNode(uuid, null, null, (Map<Name, Property>)null, null);
        assertThat(node.getUuid(), is(sameInstance(uuid)));
        assertThat(node.getParent(), is(nullValue()));
        assertThat(node.getName(), is(nullValue()));
        assertThat(node.getChildren().isEmpty(), is(true));
        assertThat(node.getProperties().isEmpty(), is(true));
    }

    @Test
    public void shouldInstantiateNodeWithUuidAndNameAndParentAndProperties() {
        UUID uuid = UUID.randomUUID();
        Segment name = segment("myNode[3]");
        UUID parent = UUID.randomUUID();
        List<Property> props = properties(property("propA", "valueA"), property("propB", "valueB"));
        node = new MapNode(uuid, name, parent, props, null);
        assertThat(node.getUuid(), is(sameInstance(uuid)));
        assertThat(node.getParent(), is(parent));
        assertThat(node.getName(), is(name));
        assertThat(node.getChildren().isEmpty(), is(true));
        assertThat(node.getProperties().size(), is(2));
        assertThat(node.getProperty(name("propA")), is(props.get(0)));
        assertThat(node.getProperty(name("propB")), is(props.get(1)));
    }

    @Test
    public void shouldCreateCopyWithNewParent() {
        UUID uuid = UUID.randomUUID();
        assertThat(node.getParent(), is(not(uuid)));
        node = node.withParent(uuid);
        assertThat(node.getParent(), is(uuid));
    }

    @Test
    public void shouldCreateCopyWithNewChild() {
        UUID uuid = UUID.randomUUID();
        node = node.withChild(uuid);
        assertThat(node.getChildren().size(), is(1));
        assertThat(node.getChildren().get(0), is(uuid));
    }

    @Test
    public void shouldCreateCopyWithNewChildAtParticularIndex() {
        UUID uuid = UUID.randomUUID();
        node = node.withChild(0, uuid);
        assertThat(node.getChildren().size(), is(1));
        assertThat(node.getChildren().get(0), is(uuid));

        UUID uuid2 = UUID.randomUUID();
        node = node.withChild(1, uuid2);
        assertThat(node.getChildren().size(), is(2));
        assertThat(node.getChildren().get(0), is(uuid));
        assertThat(node.getChildren().get(1), is(uuid2));

        UUID uuid3 = UUID.randomUUID();
        node = node.withChild(1, uuid3);
        assertThat(node.getChildren().size(), is(3));
        assertThat(node.getChildren().get(0), is(uuid));
        assertThat(node.getChildren().get(1), is(uuid3));
        assertThat(node.getChildren().get(2), is(uuid2));
    }

}
