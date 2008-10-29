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
package org.jboss.dna.repository.sequencers;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.util.Iterator;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.jboss.dna.common.jcr.AbstractJcrRepositoryTest;
import org.jboss.dna.graph.properties.NamespaceRegistry;
import org.jboss.dna.graph.properties.Property;
import org.jboss.dna.graph.sequencers.SequencerContext;
import org.jboss.dna.repository.util.BasicJcrExecutionContext;
import org.jboss.dna.repository.util.JcrExecutionContext;
import org.jboss.dna.repository.util.JcrNamespaceRegistry;
import org.jboss.dna.repository.util.JcrTools;
import org.jboss.dna.repository.util.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author John Verhaeg
 */
public class SequencerNodeContextTest extends AbstractJcrRepositoryTest {

    private JcrExecutionContext execContext;
    private Session session;
    private JcrTools tools;
    @Mock
    private javax.jcr.Property sequencedProperty;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        final SessionFactory sessionFactory = new SessionFactory() {

            public Session createSession( String name ) throws RepositoryException {
                try {
                    return getRepository().login(getTestCredentials());
                } catch (IOException error) {
                    throw new RepositoryException(error);
                }
            }
        };
        NamespaceRegistry registry = new JcrNamespaceRegistry(sessionFactory, "doesn't matter");
        execContext = new BasicJcrExecutionContext(sessionFactory, registry, null, null);
        startRepository();
        session = getRepository().login(getTestCredentials());
        tools = new JcrTools();
    }

    @After
    public void after() {
        if (session != null) {
            try {
                session.logout();
            } finally {
                session = null;
            }
        }
    }

    private void verifyProperty( SequencerContext context,
                                 String name,
                                 Object... values ) {
        Property prop = context.getInputProperty(execContext.getValueFactories().getNameFactory().create(name));
        assertThat(prop, notNullValue());
        assertThat(prop.getName(), is(execContext.getValueFactories().getNameFactory().create(name)));
        assertThat(prop.isEmpty(), is(false));
        assertThat(prop.size(), is(values.length));
        assertThat(prop.isMultiple(), is(values.length > 1));
        assertThat(prop.isSingle(), is(values.length == 1));
        Iterator<?> iter = prop.getValues();
        for (Object val : values) {
            assertThat(iter.hasNext(), is(true));
            assertThat(iter.next(), is(val));
        }
    }

    @Test( expected = java.lang.AssertionError.class )
    public void shouldNotAllowNullInputNode() throws Exception {
        new SequencerNodeContext(null, sequencedProperty, execContext);
    }

    @Test( expected = java.lang.AssertionError.class )
    public void shouldNotAllowNullSequencedProperty() throws Exception {
        Node input = tools.findOrCreateNode(session, "/a");
        new SequencerNodeContext(input, null, execContext);
    }

    @Test( expected = java.lang.AssertionError.class )
    public void shouldNotAllowNullExecutionContext() throws Exception {
        Node input = tools.findOrCreateNode(session, "/a");
        new SequencerNodeContext(input, sequencedProperty, null);
    }

    @Test
    public void shouldProvideNamespaceRegistry() throws Exception {
        Node input = tools.findOrCreateNode(session, "/a/b/c");
        SequencerNodeContext sequencerContext = new SequencerNodeContext(input, sequencedProperty, execContext);
        assertThat(sequencerContext.getNamespaceRegistry(), notNullValue());
    }

    @Test
    public void shouldProvideValueFactories() throws Exception {
        Node input = tools.findOrCreateNode(session, "/a/b/c");
        SequencerNodeContext sequencerContext = new SequencerNodeContext(input, sequencedProperty, execContext);
        assertThat(sequencerContext.getValueFactories(), notNullValue());
    }

    @Test
    public void shouldProvidePathToInput() throws Exception {
        Node input = tools.findOrCreateNode(session, "/a/b/c");
        SequencerNodeContext sequencerContext = new SequencerNodeContext(input, sequencedProperty, execContext);
        assertThat(sequencerContext.getInputPath(), is(execContext.getValueFactories().getPathFactory().create("/a/b/c")));
    }

    @Test
    public void shouldNeverReturnNullInputProperties() throws Exception {
        Node input = tools.findOrCreateNode(session, "/a/b/c");
        SequencerNodeContext sequencerContext = new SequencerNodeContext(input, sequencedProperty, execContext);
        assertThat(sequencerContext.getInputProperties(), notNullValue());
        assertThat(sequencerContext.getInputProperties().isEmpty(), is(false));
    }

    @Test
    public void shouldProvideInputProperties() throws Exception {
        Node input = tools.findOrCreateNode(session, "/a/b/c");
        input.setProperty("x", true);
        input.setProperty("y", new String[] {"asdf", "xyzzy"});
        SequencerNodeContext sequencerContext = new SequencerNodeContext(input, sequencedProperty, execContext);
        assertThat(sequencerContext.getInputProperties(), notNullValue());
        assertThat(sequencerContext.getInputProperties().isEmpty(), is(false));
        assertThat(sequencerContext.getInputProperties().size(), is(3));
        verifyProperty(sequencerContext,
                       "jcr:primaryType",
                       execContext.getValueFactories().getNameFactory().create("{http://www.jcp.org/jcr/nt/1.0}unstructured"));
        verifyProperty(sequencerContext, "x", true);
        verifyProperty(sequencerContext, "y", "asdf", "xyzzy");
    }

    @Test
    public void shouldProvideMimeType() throws Exception {
        Node input = tools.findOrCreateNode(session, "/a/b/c");
        SequencerNodeContext sequencerContext = new SequencerNodeContext(input, sequencedProperty, execContext);
        assertThat(sequencerContext.getMimeType(), is("text/plain"));
    }
}
