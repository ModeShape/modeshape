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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import java.io.InputStream;
import java.util.Calendar;
import java.util.UUID;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.Workspace;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.jcr.SessionCache.NodeInfo;
import org.jboss.dna.jcr.SessionCache.PropertyInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author jverhaeg
 */
public class AbstractJcrPropertyTest {

    private PropertyId propertyId;
    private PropertyInfo info;
    private ExecutionContext executionContext;
    private JcrNode node;
    private AbstractJcrProperty prop;
    @Mock
    private JcrSession session;
    @Mock
    private SessionCache cache;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        executionContext = new ExecutionContext();
        stub(session.getExecutionContext()).toReturn(executionContext);

        UUID uuid = UUID.randomUUID();
        node = new JcrNode(cache, uuid);
        propertyId = new PropertyId(uuid, JcrLexicon.MIMETYPE);
        prop = new MockAbstractJcrProperty(cache, propertyId);

        info = mock(PropertyInfo.class);
        stub(info.getPropertyId()).toReturn(propertyId);
        stub(info.getPropertyName()).toReturn(propertyId.getPropertyName());

        stub(cache.session()).toReturn(session);
        stub(cache.context()).toReturn(executionContext);
        stub(cache.findJcrProperty(propertyId)).toReturn(prop);
        stub(cache.findPropertyInfo(propertyId)).toReturn(info);
        stub(cache.getPathFor(info)).toReturn(path("/a/b/c/jcr:mimeType"));
        stub(cache.getPathFor(propertyId)).toReturn(path("/a/b/c/jcr:mimeType"));
        stub(cache.getPathFor(uuid)).toReturn(path("/a/b/c"));

        NodeInfo nodeInfo = mock(NodeInfo.class);
        stub(cache.findJcrNode(uuid)).toReturn(node);
        stub(cache.findNodeInfo(uuid)).toReturn(nodeInfo);
        stub(cache.getPathFor(uuid)).toReturn(path("/a/b/c"));
        stub(cache.getPathFor(nodeInfo)).toReturn(path("/a/b/c"));
    }

    protected Name name( String name ) {
        return executionContext.getValueFactories().getNameFactory().create(name);
    }

    protected Path path( String path ) {
        return executionContext.getValueFactories().getPathFactory().create(path);
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
    public void shouldNotAllowAncestorDepthGreaterThanPropertyDepth() throws Exception {
        prop.getAncestor(prop.getDepth() + 1);
    }

    @Test
    public void shouldProvideDepth() throws Exception {
        assertThat(prop.getDepth(), is(4));
    }

    @Test
    public void shouldProvideExecutionContext() throws Exception {
        assertThat(prop.context(), is(executionContext));
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
        assertThat(prop.getPath(), is("/a/b/c/jcr:mimeType"));
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
    public void shouldIndicateSameAsPropertyWithSameNodeAndSamePropertyName() throws Exception {
        Repository repository = mock(Repository.class);
        Workspace workspace = mock(Workspace.class);
        Workspace workspace2 = mock(Workspace.class);
        stub(workspace.getName()).toReturn("workspace");
        stub(workspace2.getName()).toReturn("workspace");
        JcrSession session2 = mock(JcrSession.class);
        SessionCache cache2 = mock(SessionCache.class);
        stub(session2.getRepository()).toReturn(repository);
        stub(session.getRepository()).toReturn(repository);
        stub(session2.getWorkspace()).toReturn(workspace2);
        stub(session.getWorkspace()).toReturn(workspace);
        stub(cache2.session()).toReturn(session2);
        stub(cache2.context()).toReturn(executionContext);

        // Make the other node have the same UUID ...
        UUID uuid = node.internalUuid();
        NodeInfo nodeInfo = mock(NodeInfo.class);
        PropertyInfo propertyInfo = mock(PropertyInfo.class);
        AbstractJcrNode otherNode = new JcrNode(cache2, uuid);
        stub(propertyInfo.getPropertyId()).toReturn(propertyId);
        stub(propertyInfo.getPropertyName()).toReturn(propertyId.getPropertyName());
        stub(cache2.findJcrNode(uuid)).toReturn(otherNode);
        stub(cache2.findNodeInfo(uuid)).toReturn(nodeInfo);
        stub(cache2.getPathFor(uuid)).toReturn(path("/a/b/c"));
        stub(cache2.getPathFor(nodeInfo)).toReturn(path("/a/b/c"));
        stub(cache2.findPropertyInfo(propertyId)).toReturn(info);

        assertThat(node.isSame(otherNode), is(true));

        Property prop = new MockAbstractJcrProperty(cache, propertyId);
        Property otherProp = new MockAbstractJcrProperty(cache2, propertyId);
        assertThat(prop.isSame(otherProp), is(true));
    }

    @Test
    public void shouldIndicateDifferentThanNodeWithDifferentParent() throws Exception {
        Repository repository = mock(Repository.class);
        Workspace workspace = mock(Workspace.class);
        Workspace workspace2 = mock(Workspace.class);
        stub(workspace.getName()).toReturn("workspace");
        stub(workspace2.getName()).toReturn("workspace");
        JcrSession session2 = mock(JcrSession.class);
        SessionCache cache2 = mock(SessionCache.class);
        stub(session2.getRepository()).toReturn(repository);
        stub(session.getRepository()).toReturn(repository);
        stub(session2.getWorkspace()).toReturn(workspace2);
        stub(session.getWorkspace()).toReturn(workspace);
        stub(cache2.session()).toReturn(session2);
        stub(cache2.context()).toReturn(executionContext);

        // Make the other node have a different UUID ...
        UUID uuid = UUID.randomUUID();
        PropertyId propertyId2 = new PropertyId(uuid, JcrLexicon.MIXIN_TYPES);
        NodeInfo nodeInfo = mock(NodeInfo.class);
        PropertyInfo propertyInfo = mock(PropertyInfo.class);
        AbstractJcrNode otherNode = new JcrNode(cache2, uuid);
        stub(propertyInfo.getPropertyId()).toReturn(propertyId2);
        stub(propertyInfo.getPropertyName()).toReturn(propertyId2.getPropertyName());
        stub(cache2.findJcrNode(uuid)).toReturn(otherNode);
        stub(cache2.findNodeInfo(uuid)).toReturn(nodeInfo);
        stub(cache2.getPathFor(uuid)).toReturn(path("/a/b/c"));
        stub(cache2.getPathFor(nodeInfo)).toReturn(path("/a/b/c"));

        assertThat(node.isSame(otherNode), is(false));

        Property prop = new MockAbstractJcrProperty(cache, propertyId);
        Property otherProp = new MockAbstractJcrProperty(cache2, propertyId2);
        assertThat(prop.isSame(otherProp), is(false));
    }

    @Test
    public void shouldIndicateDifferentThanPropertyWithSameNodeWithDifferentPropertyName() throws Exception {
        Repository repository = mock(Repository.class);
        Workspace workspace = mock(Workspace.class);
        Workspace workspace2 = mock(Workspace.class);
        stub(workspace.getName()).toReturn("workspace");
        stub(workspace2.getName()).toReturn("workspace");
        JcrSession session2 = mock(JcrSession.class);
        SessionCache cache2 = mock(SessionCache.class);
        stub(session2.getRepository()).toReturn(repository);
        stub(session.getRepository()).toReturn(repository);
        stub(session2.getWorkspace()).toReturn(workspace2);
        stub(session.getWorkspace()).toReturn(workspace);
        stub(cache2.session()).toReturn(session2);
        stub(cache2.context()).toReturn(executionContext);

        // Make the other node have the same UUID ...
        UUID uuid = node.internalUuid();
        NodeInfo nodeInfo = mock(NodeInfo.class);
        PropertyInfo propertyInfo = mock(PropertyInfo.class);
        PropertyId propertyId2 = new PropertyId(uuid, JcrLexicon.NAME);
        AbstractJcrNode otherNode = new JcrNode(cache2, uuid);
        stub(propertyInfo.getPropertyId()).toReturn(propertyId2);
        stub(propertyInfo.getPropertyName()).toReturn(propertyId2.getPropertyName());
        stub(cache2.findJcrNode(uuid)).toReturn(otherNode);
        stub(cache2.findNodeInfo(uuid)).toReturn(nodeInfo);
        stub(cache2.getPathFor(uuid)).toReturn(path("/a/b/c"));
        stub(cache2.getPathFor(nodeInfo)).toReturn(path("/a/b/c"));
        stub(cache2.findPropertyInfo(propertyId2)).toReturn(propertyInfo);

        assertThat(node.isSame(otherNode), is(true));

        Property prop = new MockAbstractJcrProperty(cache, propertyId);
        Property otherProp = new MockAbstractJcrProperty(cache2, propertyId2);
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

        MockAbstractJcrProperty( SessionCache cache,
                                 PropertyId propertyId ) {
            super(cache, propertyId);
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.Property#getNode()
         */
        @SuppressWarnings( "synthetic-access" )
        public Node getNode() {
            return node;
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
