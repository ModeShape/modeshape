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
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.spi.ExecutionContext;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.NamespaceRegistry;
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
    private Session session;
    @Mock
    private Node node;
    @Mock
    private NamespaceRegistry namespaceRegistry;
    @Mock
    private ExecutionContext executionContext;
    @Mock
    private Name name;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        stub(node.getSession()).toReturn(session);
        stub(executionContext.getNamespaceRegistry()).toReturn(namespaceRegistry);
        prop = new MockAbstractJcrProperty(node, executionContext, name);
    }

    @Test
    public void shouldAllowVisitation() throws Exception {
        ItemVisitor visitor = Mockito.mock(ItemVisitor.class);
        prop.accept(visitor);
        Mockito.verify(visitor).visit(prop);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowVisitationIfNoVisitor() throws Exception {
        prop.accept(null);
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

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNegativeAncestorDepth() throws Exception {
        stub(node.getAncestor(-2)).toThrow(new IllegalArgumentException());
        prop.getAncestor(-1);
    }

    @Test
    public void shouldProvideAncestor() throws Exception {
        assertThat(prop.getAncestor(0), is((Item)prop));
        stub(node.getAncestor(0)).toReturn(node);
        assertThat(prop.getAncestor(1), is((Item)node));
    }

    @Test( expected = ItemNotFoundException.class )
    public void shouldNotAllowAncestorDepthGreaterThanNodeDepth() throws Exception {
        stub(node.getAncestor(1)).toThrow(new ItemNotFoundException());
        prop.getAncestor(2);
    }

    @Test
    public void shouldProvideDepth() throws Exception {
        assertThat(prop.getDepth(), is(1));
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
        stub(name.getString(namespaceRegistry)).toReturn("name");
        assertThat(prop.getName(), is("name"));
    }

    @Test
    public void shouldProvideParent() throws Exception {
        assertThat(prop.getParent(), is(node));
    }

    @Test
    public void shouldProvidePath() throws Exception {
        stub(node.getPath()).toReturn("/nodeName");
        stub(name.getString(namespaceRegistry)).toReturn("propertyName");
        assertThat(prop.getPath(), is("/nodeName/propertyName"));
    }

    @Test
    public void shouldProvideSession() throws Exception {
        Session session = Mockito.mock(Session.class);
        stub(node.getSession()).toReturn(session);
        assertThat(prop.getSession(), is(session));
    }

    @Test
    public void shouldIndicateIsNotANode() {
        assertThat(prop.isNode(), is(false));
    }

    @Test
    public void shouldIndicateSameAsNodeWithSameParentAndName() throws Exception {
        stub(name.getString(namespaceRegistry)).toReturn("propertyName");
        Node otherNode = Mockito.mock(Node.class);
        stub(otherNode.getSession()).toReturn(session);
        Name otherName = Mockito.mock(Name.class);
        stub(otherName.getString(namespaceRegistry)).toReturn("propertyName");
        stub(node.isSame(otherNode)).toReturn(true);
        Property otherProp = new MockAbstractJcrProperty(otherNode, executionContext, otherName);
        assertThat(prop.isSame(otherProp), is(true));
    }

    @Test
    public void shouldIndicateDifferentThanNodeWithDifferentParent() throws Exception {
        stub(name.getString(namespaceRegistry)).toReturn("propertyName");
        Node otherNode = Mockito.mock(Node.class);
        stub(otherNode.getSession()).toReturn(session);
        Name otherName = Mockito.mock(Name.class);
        stub(otherName.getString(namespaceRegistry)).toReturn("propertyName");
        stub(node.isSame(otherNode)).toReturn(false);
        Property otherProp = new MockAbstractJcrProperty(otherNode, executionContext, otherName);
        assertThat(prop.isSame(otherProp), is(false));
    }

    @Test
    public void shouldIndicateDifferentThanNodeWithDifferentName() throws Exception {
        stub(name.getString(namespaceRegistry)).toReturn("propertyName");
        Node otherNode = Mockito.mock(Node.class);
        stub(otherNode.getSession()).toReturn(session);
        Name otherName = Mockito.mock(Name.class);
        stub(otherName.getString(namespaceRegistry)).toReturn("propertyName2");
        stub(node.isSame(otherNode)).toReturn(true);
        Property otherProp = new MockAbstractJcrProperty(otherNode, executionContext, otherName);
        assertThat(prop.isSame(otherProp), is(false));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSetBooleanValue() {
        prop.setValue(false);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSetCalendarValue() {
        prop.setValue(Calendar.getInstance());
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSetDoubleValue() {
        prop.setValue(0.0);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSetInputStreamValue() {
        prop.setValue(Mockito.mock(InputStream.class));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSetLongValue() {
        prop.setValue(0L);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSetNodeValue() {
        prop.setValue(Mockito.mock(Node.class));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSetStringValue() {
        prop.setValue("");
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSetStringArrayValue() {
        prop.setValue(StringUtil.EMPTY_STRING_ARRAY);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSetValueValue() {
        prop.setValue(Mockito.mock(Value.class));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSetValueArrayValue() {
        prop.setValue(new Value[0]);
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
         * @see javax.jcr.Property#getDefinition()
         */
        public PropertyDefinition getDefinition() {
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

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.Property#getValues()
         */
        public Value[] getValues() {
            return null;
        }
    }
}
