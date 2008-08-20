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
import java.io.InputStream;
import java.util.Calendar;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Value;
import org.jboss.dna.spi.ExecutionContext;
import org.jboss.dna.spi.graph.Name;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author jverhaeg
 */
public class AbstractJcrPropertyTest {

    private AbstractJcrProperty prop;
    @Mock
    private Node node;
    @Mock
    private ExecutionContext executionContext;
    @Mock
    private Name name;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        prop = new MockAbstractJcrProperty(node, executionContext, name);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNoSession() throws Exception {
        new MockAbstractJcrProperty(null, executionContext, name);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNoExecutionContext() throws Exception {
        new MockAbstractJcrProperty(node, null, name);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNoName() throws Exception {
        new MockAbstractJcrProperty(node, executionContext, null);
    }

    @Test
    public void shouldProvideSession() throws Exception {
        Session session = Mockito.mock(Session.class);
        stub(node.getSession()).toReturn(session);
        assertThat(prop.getSession(), is(session));
    }

    @Test
    public void shouldProvideExecutionContext() throws Exception {
        assertThat(prop.getExecutionContext(), is(executionContext));
    }

    @Test
    public void shouldProvideNode() throws Exception {
        assertThat(prop.getNode(), is(node));
    }

    @Test
    public void shouldProvideName() throws Exception {
        stub(name.getString()).toReturn("name");
        assertThat(prop.getName(), is("name"));
    }

    private class MockAbstractJcrProperty extends AbstractJcrProperty {

        MockAbstractJcrProperty( Node node,
                                 ExecutionContext executionContext,
                                 Name name ) {
            super(node, executionContext, name);
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.Property#getBoolean()
         */
        public boolean getBoolean() {
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.Property#getDate()
         */
        public Calendar getDate() {
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.Property#getDouble()
         */
        public double getDouble() {
            return 0;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.Property#getLength()
         */
        public long getLength() {
            return 0;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.Property#getLengths()
         */
        public long[] getLengths() {
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.Property#getLong()
         */
        public long getLong() {
            return 0;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.Property#getStream()
         */
        public InputStream getStream() {
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.Property#getString()
         */
        public String getString() {
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.Property#getType()
         */
        public int getType() {
            return 0;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.Property#getValue()
         */
        public Value getValue() {
            return null;
        }
    }
}
