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
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;
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
        node = new AbstractJcrNode(session, properties) {

            public String getName() {
                return null;
            }

            public Node getParent() {
                return null;
            }
        };
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNoSession() throws Exception {
        new AbstractJcrNode(null, properties) {

            public String getName() {
                return null;
            }

            public Node getParent() {
                return null;
            }
        };
    }

    @Test
    public void shouldProvideSession() throws Exception {
        assertThat(node.getSession(), is(session));
    }

    @Test
    public void shouldProvideUuid() throws Exception {
        Property property = Mockito.mock(Property.class);
        stub(property.getName()).toReturn("jcr:uuid");
        Value value = Mockito.mock(Value.class);
        stub(value.getString()).toReturn("uuid");
        stub(property.getValue()).toReturn(value);
        properties.add(property);
        assertThat(node.getUUID(), is("uuid"));
    }

    @Test
    public void shouldProvideNamedProperty() throws Exception {
        Property property = Mockito.mock(Property.class);
        stub(property.getName()).toReturn("test");
        properties.add(property);
        assertThat(node.getProperty("test"), is(property));
    }
}
