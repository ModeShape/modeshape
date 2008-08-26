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
package org.jboss.dna.jcr;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.stub;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.jcr.Item;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author jverhaeg
 */
public class AbstractJcrNodeTest {

    private AbstractJcrNode node;
    @Mock
    private Session session;
    private Set<Property> properties;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        properties = new HashSet<Property>();
        node = new AbstractJcrNode(session) {

            public int getDepth() {
                return 0;
            }

            public String getName() {
                return null;
            }

            public Node getParent() {
                return null;
            }

            public String getPath() {
                return null;
            }
        };
        node.setProperties(properties);
    }

    @Test
    public void shouldAllowVisitation() throws Exception {
        ItemVisitor visitor = Mockito.mock(ItemVisitor.class);
        node.accept(visitor);
        Mockito.verify(visitor).visit(node);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNoSession() throws Exception {
        new AbstractJcrNode(null) {

            public int getDepth() {
                return 0;
            }

            public String getName() {
                return null;
            }

            public Node getParent() {
                return null;
            }

            public String getPath() {
                return null;
            }
        };
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNegativeAncestorDepth() throws Exception {
        node.getAncestor(-1);
    }

    @Test
    public void shouldProvideAncestor() throws Exception {
        assertThat(node.getAncestor(0), is((Item)node));
    }

    @Test
    public void shouldProvideSession() throws Exception {
        assertThat(node.getSession(), is(session));
    }

    @Test
    public void shouldProvideInternalUuid() throws Exception {
        UUID uuid = UUID.randomUUID();
        node.setInternalUuid(uuid);
        assertThat(node.getInternalUuid(), is(uuid));
    }

    @Test
    public void shouldProvideNamedProperty() throws Exception {
        Property property = Mockito.mock(Property.class);
        stub(property.getName()).toReturn("test");
        properties.add(property);
        assertThat(node.getProperty("test"), is(property));
    }

    @Test
    public void shouldBeANode() {
        assertThat(node.isNode(), is(true));
    }
}
