/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    public void shouldResolveUrlPathWithSlashAndRepositoryAndMultipleSlashWorkspaceAndMultipleSegmentPath() {
        assertResolved("/rep//a/b/c", "rep", "a", "/b/c");
        assertResolved("/rep///a/b/c", "rep", "a", "/b/c");
        assertResolved("/rep////a/b/c", "rep", "a", "/b/c");
    }

    @Test
    public void shouldResolveUrlPathWithSlashAndRepositoryAndMultipleSlashWorkspaceAndMultipleSlashSegmentPath() {
        assertResolved("/rep//a//b/c", "rep", "a", "/b/c");
        assertResolved("/rep///a/b///c", "rep", "a", "/b/c");
        assertResolved("/rep////a/b/c///", "rep", "a", "/b/c/");
    }

    @Test
    public void shouldResolveUrlPathWithSlashAndRepositoryAndSlashWorkspaceAndMultipleSlashSegmentPath() {
        assertResolved("/rep/a//b/c", "rep", "a", "/b/c");
        assertResolved("/rep/a/b///c", "rep", "a", "/b/c");
        assertResolved("/rep/a/b/c///", "rep", "a", "/b/c/");
    }

    @Test
    public void shouldResolveUrlPathWithSlashAndRepositoryAndBlankWorkspaceAndSlash() {
        assertResolved("/rep//", "rep", "", "/");
    }

    @Test
    public void shouldResolveUrlPathWithSlashAndBlankRepositoryAndBlankWorkspaceAndSlash() {
        assertResolved("///", null, null, null);
    }

    @Test
    public void shouldResolveUrlPathWithSlashAndBlankRepositoryAndBlankWorkspaceAndSingleSegmentPath() {
        assertResolved("///a", "a", null, null);
    }

    @Test
    public void shouldResolveUrlPathWithSlashAndBlankRepositoryAndBlankWorkspaceAndMultipleSegmentPath() {
        assertResolved("///a/b/c", "a", "b", "/c");
    }
}
