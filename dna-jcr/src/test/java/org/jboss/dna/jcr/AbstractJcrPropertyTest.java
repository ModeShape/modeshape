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
package org.jboss.dna.jcr;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.stub;
import java.io.InputStream;
import java.util.Calendar;
import java.util.UUID;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Path;
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
    private Workspace workspace;
    @Mock
    private Repository repository;
    @Mock
    private JcrSession session;
    private AbstractJcrNode node;
    @Mock
    private NodeDefinition nodeDefinition;
    @Mock
    private PropertyDefinition propertyDefinition;
    private ExecutionContext executionContext;
    private org.jboss.dna.graph.property.Property dnaProperty;
    private Location rootLocation;
    private Location nodeLocation;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        executionContext = new ExecutionContext();
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, "text/plain");
        stub(propertyDefinition.getRequiredType()).toReturn(PropertyType.STRING);
        stub(session.getWorkspace()).toReturn(workspace);
        stub(session.getRepository()).toReturn(repository);
        stub(session.getExecutionContext()).toReturn(executionContext);

        UUID rootUuid = UUID.randomUUID();
        Path rootPath = executionContext.getValueFactories().getPathFactory().createRootPath();
        rootLocation = Location.create(rootPath, rootUuid);
        JcrRootNode rootNode = new JcrRootNode(session, rootLocation, nodeDefinition);
        stub(session.getNode(rootUuid)).toReturn(rootNode);

        UUID uuid = UUID.randomUUID();
        Path path = executionContext.getValueFactories().getPathFactory().create("/nodeName");
        nodeLocation = Location.create(path, uuid);
        node = new JcrNode(session, rootUuid, nodeLocation, nodeDefinition);
        stub(session.getNode(uuid)).toReturn(node);

        prop = new MockAbstractJcrProperty(node, propertyDefinition, dnaProperty);
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

    @Test( expected = ItemNotFoundException.class )
    public void shouldNotAllowNegativeAncestorDepth() throws Exception {
        stub(node.getAncestor(-2)).toThrow(new IllegalArgumentException());
        prop.getAncestor(-1);
    }

    @Test
    public void shouldProvideAncestor() throws Exception {
        assertThat(prop.getAncestor(prop.getDepth()), is((Item)prop));
        assertThat(prop.getAncestor(prop.getDepth() - 1), is((Item)node));
    }

    @Test( expected = ItemNotFoundException.class )
    public void shouldNotAllowAncestorDepthGreaterThanNodeDepth() throws Exception {
        prop.getAncestor(3);
    }

    @Test
    public void shouldProvideDepth() throws Exception {
        assertThat(prop.getDepth(), is(2));
    }

    @Test
    public void shouldProvideExecutionContext() throws Exception {
        assertThat(prop.getExecutionContext(), is(executionContext));
    }

    @Test
    public void shouldProvideName() throws Exception {
        assertThat(prop.getName(), is("jcr:mimeType"));
    }

    @Test
    public void shouldProvideParent() throws Exception {
        assertThat(prop.getParent(), is((Node)node));
    }

    @Test
    public void shouldProvidePath() throws Exception {
        assertThat(prop.getPath(), is("/nodeName/jcr:mimeType"));
    }

    @Test
    public void shouldProvideSession() throws Exception {
        assertThat(prop.getSession(), is((Session)session));
    }

    @Test
    public void shouldIndicateIsNotANode() {
        assertThat(prop.isNode(), is(false));
    }

    @Test
    public void shouldIndicateSameAsNodeWithSameParentAndSamePropertyName() throws Exception {
        org.jboss.dna.graph.property.Property otherDnaProperty = executionContext.getPropertyFactory()
                                                                                 .create(dnaProperty.getName());
        // Make the other node have the same UUID ...
        JcrNode otherNode = new JcrNode(session, rootLocation.getUuid(), nodeLocation, nodeDefinition);

        assertThat(node.isSame(otherNode), is(true));
        Property prop = new MockAbstractJcrProperty(node, propertyDefinition, otherDnaProperty);
        Property otherProp = new MockAbstractJcrProperty(otherNode, propertyDefinition, otherDnaProperty);
        assertThat(prop.isSame(otherProp), is(true));
    }

    @Test
    public void shouldIndicateDifferentThanNodeWithDifferentParent() throws Exception {
        org.jboss.dna.graph.property.Property otherDnaProperty = executionContext.getPropertyFactory()
                                                                                 .create(dnaProperty.getName());
        UUID otherUuid = UUID.randomUUID();
        Path otherPath = executionContext.getValueFactories().getPathFactory().create("/nodeName");
        Location location = Location.create(otherPath, otherUuid);
        JcrNode otherNode = new JcrNode(session, rootLocation.getUuid(), location, nodeDefinition);
        stub(session.getNode(otherUuid)).toReturn(otherNode);

        assertThat(node.isSame(otherNode), is(false));
        Property prop = new MockAbstractJcrProperty(node, propertyDefinition, otherDnaProperty);
        Property otherProp = new MockAbstractJcrProperty(otherNode, propertyDefinition, otherDnaProperty);
        assertThat(prop.isSame(otherProp), is(false));
    }

    @Test
    public void shouldIndicateDifferentThanPropertyWithSameNodeWithDifferentPropertyName() throws Exception {
        org.jboss.dna.graph.property.Property otherDnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.NAME);
        // Make the other node have the same UUID ...
        JcrNode otherNode = new JcrNode(session, rootLocation.getUuid(), nodeLocation, nodeDefinition);

        assertThat(node.isSame(otherNode), is(true));
        Property prop = new MockAbstractJcrProperty(node, propertyDefinition, dnaProperty);
        Property otherProp = new MockAbstractJcrProperty(otherNode, propertyDefinition, otherDnaProperty);
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

        MockAbstractJcrProperty( AbstractJcrNode node,
                                 PropertyDefinition propertyDefinition,
                                 org.jboss.dna.graph.property.Property dnaProperty ) {
            super(node, propertyDefinition, propertyDefinition.getRequiredType(), dnaProperty);
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.Property#getNode()
         */
        public Node getNode() {
            throw new UnsupportedOperationException(); // shouldn't be called
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
