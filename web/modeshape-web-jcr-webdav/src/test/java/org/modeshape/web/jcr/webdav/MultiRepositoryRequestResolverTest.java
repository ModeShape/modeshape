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
package org.modeshape.web.jcr.webdav;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MultiRepositoryRequestResolverTest {

    private MultiRepositoryRequestResolver resolver;
    private ResolvedRequest resolved;

    @Mock
    private HttpServletRequest request;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        when(request.getPathInfo()).thenReturn("");
        resolver = new MultiRepositoryRequestResolver();
    }

    protected void setPath( String path ) {
        when(request.getPathInfo()).thenReturn(path);
    }

    protected void assertResolved( String repositoryName,
                                   String workspaceName,
                                   String path ) {
        assertThat(resolved, is(notNullValue()));
        assertThat(resolved.getRepositoryName(), is(repositoryName));
        assertThat(resolved.getWorkspaceName(), is(workspaceName));
        assertThat(resolved.getPath(), is(path));
    }

    protected void assertResolved( String urlPath,
                                   String repositoryName,
                                   String workspaceName,
                                   String path ) {
        setPath(urlPath);
        resolved = resolver.resolve(request, urlPath);
        assertResolved(repositoryName, workspaceName, path);
    }

    @Test
    public void shouldResolveNullUrlPath() {
        assertResolved(null, null, null, null);
    }

    @Test
    public void shouldResolveEmptyUrlPath() {
        assertResolved("", null, null, null);
    }

    @Test
    public void shouldResolveUrlPathWithSlash() {
        assertResolved("/", null, null, null);
    }

    @Test
    public void shouldResolveUrlPathWithSlashAndRepository() {
        assertResolved("/rep", "rep", null, null);
    }

    @Test
    public void shouldResolveUrlPathWithSlashAndRepositoryAndSlash() {
        assertResolved("/rep/", "rep", null, null);
    }

    @Test
    public void shouldResolveUrlPathWithSlashAndRepositoryAndWorkspace() {
        assertResolved("/rep/ws", "rep", "ws", "/");
    }

    @Test
    public void shouldResolveUrlPathWithSlashAndRepositoryAndWorkspaceAndSlash() {
        assertResolved("/rep/ws/", "rep", "ws", "/");
    }

    @Test
    public void shouldResolveUrlPathWithSlashAndRepositoryAndWorkspaceAndSingleSegmentPath() {
        assertResolved("/rep/ws/a", "rep", "ws", "/a");
    }

    @Test
    public void shouldResolveUrlPathWithSlashAndRepositoryAndWorkspaceAndMultipleSegmentPath() {
        assertResolved("/rep/ws/a/b/c", "rep", "ws", "/a/b/c");
    }

    @Test
    public void shouldResolveUrlPathWithSlashAndRepositoryAndBlankWorkspaceAndMultipleSegmentPath() {
        assertResolved("/rep//a/b/c", "rep", "", "/a/b/c");
    }

    @Test
    public void shouldResolveUrlPathWithSlashAndRepositoryAndBlankWorkspaceAndSlash() {
        assertResolved("/rep//", "rep", "", "/");
    }

    @Test
    public void shouldResolveUrlPathWithSlashAndBlankRepositoryAndBlankWorkspaceAndSlash() {
        assertResolved("///", "", "", "/");
    }

    @Test
    public void shouldResolveUrlPathWithSlashAndBlankRepositoryAndBlankWorkspaceAndSingleSegmentPath() {
        assertResolved("///a", "", "", "/a");
    }

    @Test
    public void shouldResolveUrlPathWithSlashAndBlankRepositoryAndBlankWorkspaceAndMultipleSegmentPath() {
        assertResolved("///a/b/c", "", "", "/a/b/c");
    }
}
