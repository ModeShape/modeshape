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
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.stub;
import java.security.Principal;
import java.util.Collections;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import org.jboss.dna.jcr.GraphTools.NodeContent;
import org.jboss.dna.spi.graph.PathFactory;
import org.jboss.dna.spi.graph.ValueFactories;
import org.jboss.dna.spi.graph.impl.BasicPath;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author jverhaeg
 */
public class JcrSessionTest {

    private JcrSession session;
    @Mock
    private JcrRepository repository;
    @Mock
    private JcrExecutionContext executionContext;
    @Mock
    private GraphTools tools;
    @Mock
    private LoginContext loginContext;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        stub(executionContext.getLoginContext()).toReturn(loginContext);
        stub(executionContext.getGraphTools()).toReturn(tools);
        session = new JcrSession(repository, executionContext, JcrI18n.defaultWorkspaceName.text());
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNoRepository() throws Exception {
        new JcrSession(null, executionContext, JcrI18n.defaultWorkspaceName.text());
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNoExecutionContext() throws Exception {
        new JcrSession(repository, null, JcrI18n.defaultWorkspaceName.text());
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNoWorkspaceName() throws Exception {
        new JcrSession(repository, executionContext, null);
    }

    @Test
    public void shouldProvideAccessToRepository() throws Exception {
        assertThat((JcrRepository)session.getRepository(), is(repository));
    }

    @Test
    public void shouldProvideAccessToWorkspace() throws Exception {
        assertThat(session.getWorkspace(), notNullValue());
    }

    @Test
    public void shouldBeLiveBeforeLogout() throws Exception {
        assertThat(session.isLive(), is(true));
    }

    @Test
    public void shouldAllowLogout() throws Exception {
        session.logout();
    }

    @Test
    public void shouldNotBeLiveAfterLogout() throws Exception {
        session.logout();
        assertThat(session.isLive(), is(false));
    }

    @Test
    public void shouldProvideUserId() throws Exception {
        assertThat(session.getUserID(), nullValue());
        Principal principal = Mockito.mock(Principal.class);
        stub(principal.getName()).toReturn("name");
        Subject subject = new Subject(false, Collections.singleton(principal), Collections.EMPTY_SET, Collections.EMPTY_SET);
        stub(executionContext.getSubject()).toReturn(subject);
        assertThat(session.getUserID(), is("name"));
    }

    @Test
    public void shouldProvideRootNode() throws Exception {
        ValueFactories valueFactories = Mockito.mock(ValueFactories.class);
        PathFactory pathFactory = Mockito.mock(PathFactory.class);
        stub(pathFactory.createRootPath()).toReturn(BasicPath.ROOT);
        stub(valueFactories.getPathFactory()).toReturn(pathFactory);
        stub(executionContext.getValueFactories()).toReturn(valueFactories);
        NodeContent content = tools.new NodeContent();
        stub(tools.getNodeContent(BasicPath.ROOT)).toReturn(content);
        assertThat(session.getRootNode(), notNullValue());
    }
}
