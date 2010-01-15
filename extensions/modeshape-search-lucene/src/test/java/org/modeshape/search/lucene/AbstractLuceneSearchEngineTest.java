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
package org.modeshape.search.lucene;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.request.CloneBranchRequest;
import org.modeshape.graph.request.CloneWorkspaceRequest;
import org.modeshape.graph.request.CopyBranchRequest;
import org.modeshape.graph.request.CreateNodeRequest;
import org.modeshape.graph.request.CreateWorkspaceRequest;
import org.modeshape.graph.request.DeleteBranchRequest;
import org.modeshape.graph.request.DestroyWorkspaceRequest;
import org.modeshape.graph.request.GetWorkspacesRequest;
import org.modeshape.graph.request.MoveBranchRequest;
import org.modeshape.graph.request.ReadAllChildrenRequest;
import org.modeshape.graph.request.ReadAllPropertiesRequest;
import org.modeshape.graph.request.UpdatePropertiesRequest;
import org.modeshape.graph.request.VerifyWorkspaceRequest;
import org.modeshape.graph.search.SearchEngineWorkspace;
import org.modeshape.graph.search.AbstractSearchEngine.Workspaces;
import org.modeshape.search.lucene.AbstractLuceneSearchEngine.AbstractLuceneProcessor;
import org.modeshape.search.lucene.AbstractLuceneSearchEngine.WorkspaceSession;

/**
 * 
 */
public class AbstractLuceneSearchEngineTest {

    private ExecutionContext context;
    private AbstractLuceneProcessor<TestWorkspace, TestSession> processor;
    private Workspaces<TestWorkspace> workspaces;

    @SuppressWarnings( "unchecked" )
    @Before
    public void beforeEach() {
        context = new ExecutionContext();
        workspaces = mock(Workspaces.class);
        processor = new TestProcessor("source", context, workspaces, false);
    }

    protected Property property( String name,
                                 Object... values ) {
        return context.getPropertyFactory().create(name(name), values);
    }

    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    @Test
    public void shouldSerializeSingleValuedProperty() {
        Property p1 = property("p1", "v1");
        String serialized = processor.serializeProperty(p1);
        assertThat(serialized, is("p1=v1"));
    }

    @Test
    public void shouldSerializeTwoValuedProperty() {
        Property p1 = property("p1", "v1", "v2");
        String serialized = processor.serializeProperty(p1);
        assertThat(serialized, is("p1=v1\nv2"));
    }

    @Test
    public void shouldSerializeMultiValuedProperty() {
        Property p1 = property("p1", "v1", "v2", "v3");
        String serialized = processor.serializeProperty(p1);
        assertThat(serialized, is("p1=v1\nv2\nv3"));
    }

    @Test
    public void shouldDeserializeSingleValuedProperty() {
        Property p1 = property("p1", "v1");
        Property p1a = processor.deserializeProperty(processor.serializeProperty(p1));
        assertThat(p1a, is(p1));
    }

    @Test
    public void shouldDeserializeTwoValuedProperty() {
        Property p1 = property("p1", "v1", 4L);
        Property p1a = processor.deserializeProperty(processor.serializeProperty(p1));
        assertThat(p1a, is(p1));
    }

    @Test
    public void shouldDeserializeMultiValuedProperty() {
        // The values are stored as strings, so names and paths must be stored as string-values in the property
        Property p1 = property("p1",
                               "v1",
                               4L,
                               name("mode:something").getString(context.getNamespaceRegistry()),
                               UUID.randomUUID());
        Property p1a = processor.deserializeProperty(processor.serializeProperty(p1));
        assertThat(p1a, is(p1));
    }

    @Test
    public void shouldSerializeAndDeserializePropertyWithNameValues() {
        Property p1 = property("p1", name("v1"), name("mode:something"));
        Property p2 = processor.deserializeProperty(processor.serializeProperty(p1));
        assertThat(p2.getName(), is(p1.getName()));
        Object[] values1 = p1.getValuesAsArray();
        Object[] values2 = p2.getValuesAsArray();
        assertThat(values1.length, is(values2.length));
        // The standard way is to access the values with a value factory, so doing this does work ...
        NameFactory names = context.getValueFactories().getNameFactory();
        for (int i = 0; i != values1.length; ++i) {
            assertThat(names.create(values1[i]), is(names.create(values2[i])));
        }
    }

    protected static class TestProcessor extends AbstractLuceneProcessor<TestWorkspace, TestSession> {

        protected TestProcessor( String sourceName,
                                 ExecutionContext context,
                                 Workspaces<TestWorkspace> workspaces,
                                 boolean readOnly ) {
            super(sourceName, context, workspaces, null, null, readOnly);

        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.search.lucene.AbstractLuceneSearchEngine.AbstractLuceneProcessor#createSessionFor(org.modeshape.graph.search.SearchEngineWorkspace)
         */
        @Override
        protected TestSession createSessionFor( TestWorkspace workspace ) {
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.search.lucene.AbstractLuceneSearchEngine.AbstractLuceneProcessor#fullTextFieldName(java.lang.String)
         */
        @Override
        protected String fullTextFieldName( String propertyName ) {
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.VerifyWorkspaceRequest)
         */
        @Override
        public void process( VerifyWorkspaceRequest request ) {
            super.processUnknownRequest(request);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.GetWorkspacesRequest)
         */
        @Override
        public void process( GetWorkspacesRequest request ) {
            super.processUnknownRequest(request);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CreateWorkspaceRequest)
         */
        @Override
        public void process( CreateWorkspaceRequest request ) {
            super.processUnknownRequest(request);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CloneBranchRequest)
         */
        @Override
        public void process( CloneBranchRequest request ) {
            super.processUnknownRequest(request);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CloneWorkspaceRequest)
         */
        @Override
        public void process( CloneWorkspaceRequest request ) {
            super.processUnknownRequest(request);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.DestroyWorkspaceRequest)
         */
        @Override
        public void process( DestroyWorkspaceRequest request ) {
            super.processUnknownRequest(request);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CopyBranchRequest)
         */
        @Override
        public void process( CopyBranchRequest request ) {
            super.processUnknownRequest(request);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CreateNodeRequest)
         */
        @Override
        public void process( CreateNodeRequest request ) {
            super.processUnknownRequest(request);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.DeleteBranchRequest)
         */
        @Override
        public void process( DeleteBranchRequest request ) {
            super.processUnknownRequest(request);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.MoveBranchRequest)
         */
        @Override
        public void process( MoveBranchRequest request ) {
            super.processUnknownRequest(request);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadAllChildrenRequest)
         */
        @Override
        public void process( ReadAllChildrenRequest request ) {
            super.processUnknownRequest(request);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadAllPropertiesRequest)
         */
        @Override
        public void process( ReadAllPropertiesRequest request ) {
            super.processUnknownRequest(request);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.UpdatePropertiesRequest)
         */
        @Override
        public void process( UpdatePropertiesRequest request ) {
            super.processUnknownRequest(request);
        }

    }

    protected static abstract class TestSession implements WorkspaceSession {

    }

    protected static class TestWorkspace implements SearchEngineWorkspace {
        private final String name;
        private boolean destroyed = false;

        protected TestWorkspace( String name ) {
            this.name = name;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.search.SearchEngineWorkspace#destroy(org.modeshape.graph.ExecutionContext)
         */
        public void destroy( ExecutionContext context ) {
            destroyed = true;
        }

        public boolean isDestroyed() {
            return destroyed;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.search.SearchEngineWorkspace#getWorkspaceName()
         */
        public String getWorkspaceName() {
            return name;
        }
    }

}
