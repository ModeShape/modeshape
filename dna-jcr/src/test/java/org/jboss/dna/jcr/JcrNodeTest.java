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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path.Segment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author jverhaeg
 */
public class JcrNodeTest {

    private JcrNode node;
    private Node root;
    @Mock
    private Session session;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        root = new JcrRootNode(session);
        Segment segment = Mockito.mock(Segment.class);
        Name name = Mockito.mock(Name.class);
        stub(name.getString()).toReturn("name");
        stub(segment.getName()).toReturn(name);
        stub(segment.getIndex()).toReturn(2);
        UUID uuid = UUID.randomUUID();
        node = new JcrNode(session, uuid, segment);
        stub(session.getNodeByUUID(uuid.toString())).toReturn(root);
        node.setProperties(new HashSet<Property>());
        node.setChildren(new ArrayList<Segment>());
    }

    @Test( expected = ItemNotFoundException.class )
    public void shouldNotAllowAncestorDepthGreaterThanNodeDepth() throws Exception {
        node.getAncestor(2);
    }

    @Test
    public void shouldProvideDepth() throws Exception {
        assertThat(node.getDepth(), is(1));
    }

    @Test
    public void shouldProvideIndex() throws Exception {
        assertThat(node.getIndex(), is(2));
    }

    @Test
    public void shouldProvideName() throws Exception {
        assertThat(node.getName(), is("name"));
    }

    @Test
    public void shouldProvidePath() throws Exception {
        assertThat(node.getPath(), is("/name[2]"));
    }
}
