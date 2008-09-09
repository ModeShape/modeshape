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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.HashSet;
import java.util.Set;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Property;
import javax.jcr.Session;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author jverhaeg
 */
public class JcrRootNodeTest {

    private JcrRootNode root;
    @Mock
    private Session session;
    private Set<Property> properties;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        properties = new HashSet<Property>();
        root = new JcrRootNode(session);
        root.setProperties(properties);
    }

    @Test( expected = ItemNotFoundException.class )
    public void shouldNotAllowAncestorDepthGreaterThanNodeDepth() throws Exception {
        root.getAncestor(1);
    }

    @Test
    public void shouldHaveZeroDepth() throws Exception {
        assertThat(root.getDepth(), is(0));
    }

    @Test
    public void shouldIndicateIndexIsOne() throws Exception {
        assertThat(root.getIndex(), is(1));
    }

    @Test
    public void shouldHaveEmptyName() throws Exception {
        String name = root.getName();
        assertThat(name, notNullValue());
        assertThat(name.length(), is(0));
    }

    @Test( expected = ItemNotFoundException.class )
    public void shouldHaveNoParent() throws Exception {
        root.getParent();
    }

    @Test
    public void shouldProvidePath() throws Exception {
        assertThat(root.getPath(), is("/"));
    }
}
