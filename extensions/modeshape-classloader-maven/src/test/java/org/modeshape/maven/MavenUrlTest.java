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
package org.modeshape.maven;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class MavenUrlTest {

    private MavenUrl valid;
    private String validHostname;
    private int validPort;
    private String validWorkspace;
    private String validPath;
    private URLStreamHandler urlStreamHandler;

    @Before
    public void beforeEach() {
        this.validHostname = "localhost";
        this.validPort = 1000;
        this.validWorkspace = "workspaceTwo";
        this.validPath = "/path/to/some/node";
        this.valid = new MavenUrl();
        this.valid.setHostname(this.validHostname);
        this.valid.setPort(this.validPort);
        this.valid.setWorkspaceName(this.validWorkspace);
        this.valid.setPath(this.validPath);
        this.urlStreamHandler = new URLStreamHandler() {

            @Override
            protected URLConnection openConnection( URL u ) {
                return null;
            }
        };
    }

    @Test
    public void shouldAcceptNullOrBlankHostButStoreAsEmptyString() {
        this.valid.setHostname(null);
        assertThat(this.valid.getHostname(), is(""));
        this.valid.setHostname("  ");
        assertThat(this.valid.getHostname(), is(""));
    }

    @Test
    public void shouldAcceptNullOrBlankWorkspaceNameButStoreAsEmptyString() {
        this.valid.setWorkspaceName(null);
        assertThat(this.valid.getWorkspaceName(), is(""));
        this.valid.setWorkspaceName("   ");
        assertThat(this.valid.getWorkspaceName(), is(""));
    }

    @Test
    public void shouldRemoveLeadingAndTrailingDelimitersFromWorkspace() {
        this.valid.setWorkspaceName("//workspaceName");
        assertThat(this.valid.getWorkspaceName(), is("workspaceName"));
        this.valid.setWorkspaceName("//workspaceName//");
        assertThat(this.valid.getWorkspaceName(), is("workspaceName"));
        this.valid.setWorkspaceName("workspaceName//");
        assertThat(this.valid.getWorkspaceName(), is("workspaceName"));
        this.valid.setWorkspaceName("//");
        assertThat(this.valid.getWorkspaceName(), is(""));
    }

    @Test
    public void shouldAcceptNullOrBlankPathButStorePathWithLeadingDelimiter() {
        this.valid.setPath(null);
        assertThat(this.valid.getPath(), is("/"));
        this.valid.setPath("   ");
        assertThat(this.valid.getPath(), is("/"));
    }

    @Test
    public void shouldAcceptPathWithoutLeadingDelimiterButStorePathWithLeadingDelimiter() {
        this.valid.setPath("path/x");
        assertThat(this.valid.getPath(), is("/path/x"));
    }

    @Test
    public void shouldAllowPathWithOneOrMoreTrailingDelimiters() {
        this.valid.setPath("path/x/");
        assertThat(this.valid.getPath(), is("/path/x/"));
        this.valid.setPath("path/x///");
        assertThat(this.valid.getPath(), is("/path/x///"));
    }

    @Test
    public void shouldParseUrlsWithValidFormat() throws Exception {
        parse("jcr://localhost:10001/workspaceName/path/to/node/x", "localhost", 10001, "workspaceName", "/path/to/node/x");
        parse("jcr://localhost:10001//path/to/node/x", "localhost", 10001, "", "/path/to/node/x");
        parse("jcr://localhost:10001/", "localhost", 10001, "", "/");
        parse("jcr://localhost:10001/workspaceName/", "localhost", 10001, "workspaceName", "/");
        parse("jcr://localhost:10001/workspaceName//", "localhost", 10001, "workspaceName", "/", "jcr://localhost:10001/workspaceName/");
        parse("jcr://localhost:10001/workspaceName//a//", "localhost", 10001, "workspaceName", "/a//", "jcr://localhost:10001/workspaceName/a//");
        parse("jcr://localhost:10001/workspaceName", "localhost", 10001, "workspaceName", "/", "jcr://localhost:10001/workspaceName/");

        // Note that the default string form for a URL with no authority (that is, no hostname or port) is to remove two of the 3
        // slashes. In other words, "file:/a/b/c" rather than "file:///a/b/c"
        parse("jcr:///workspaceName/path/to/node/x", "", MavenUrl.NO_PORT, "workspaceName", "/path/to/node/x", "jcr:/workspaceName/path/to/node/x");
        parse("jcr:////path/to/node/x", "", MavenUrl.NO_PORT, "", "/path/to/node/x", "jcr://path/to/node/x");
        parse("jcr:/workspaceName/path/to/node/x", "", MavenUrl.NO_PORT, "workspaceName", "/path/to/node/x", "jcr:/workspaceName/path/to/node/x");
        parse("jcr://path/to/node/x", "path", MavenUrl.NO_PORT, "to", "/node/x", "jcr://path/to/node/x");
    }

    @Test
    public void shouldNotParseUrlsWithInvalidFormat() {
        assertThat(MavenUrl.parse("http:///workspaceName/path/to/node/x", null), is(nullValue()));
    }

    public void parse( String url, String expectedHostname, int expectedPort, String expectedWorkspaceName, String expectedPath ) throws Exception {
        parse(url, expectedHostname, expectedPort, expectedWorkspaceName, expectedPath, url);
    }

    public void parse( String url, String expectedHostname, int expectedPort, String expectedWorkspaceName, String expectedPath, String expectedUrl ) throws Exception {
        MavenUrl result = MavenUrl.parse(url, null);
        assertThat(result, is(notNullValue()));
        assertThat(result.getHostname(), is(expectedHostname));
        assertThat(result.getPort(), is(expectedPort));
        assertThat(result.getWorkspaceName(), is(expectedWorkspaceName));
        assertThat(result.getPath(), is(expectedPath));

        URL resultingUrl = result.getUrl(urlStreamHandler, null);
        assertThat(resultingUrl.toString(), is(expectedUrl));
    }

    public MavenUrl createUrl( String hostname, int port, String workspaceName, String path, String expectedUrl ) throws Exception {
        MavenUrl url = new MavenUrl();
        url.setHostname(hostname);
        url.setPort(port);
        url.setWorkspaceName(workspaceName);
        url.setPath(path);

        URL resultingUrl = url.getUrl(urlStreamHandler, null);
        assertThat(resultingUrl.toString(), is(expectedUrl));
        assertThat(url.toString(), is(expectedUrl));
        return url;
    }

    @Test
    public void shouldCreateUrlWithHostnameAndPortAndWorkspaceAndPath() throws Exception {
        createUrl("localhost", 10001, "workspace", "/a/b/c", "jcr://localhost:10001/workspace/a/b/c");
    }

    @Test
    public void shouldCreateUrlWithHostnameAndPortAndWorkspaceAndNoPath() throws Exception {
        createUrl("localhost", 10001, "workspace", null, "jcr://localhost:10001/workspace/");
        createUrl("localhost", 10001, "workspace", "", "jcr://localhost:10001/workspace/");
        createUrl("localhost", 10001, "workspace", "   ", "jcr://localhost:10001/workspace/");
    }

    @Test
    public void shouldCreateUrlWithHostnameAndPortAndPathAndNoWorkspace() throws Exception {
        createUrl("localhost", 10001, null, "/a/b/c", "jcr://localhost:10001//a/b/c");
        createUrl("localhost", 10001, "", "/a/b/c", "jcr://localhost:10001//a/b/c");
        createUrl("localhost", 10001, "   ", "/a/b/c", "jcr://localhost:10001//a/b/c");
    }

    @Test
    public void shouldCreateUrlWithHostnameAndWorkspaceAndPathAndNoPort() throws Exception {
        createUrl("localhost", MavenUrl.NO_PORT, "workspace", "/a/b/c", "jcr://localhost/workspace/a/b/c");
    }

    @Test
    public void shouldCreateUrlWithWorkspaceAndPathAndNoHostnameOrPort() throws Exception {
        createUrl(null, MavenUrl.NO_PORT, "workspace", "/a/b/c", "jcr:/workspace/a/b/c");
        createUrl("", MavenUrl.NO_PORT, "workspace", "/a/b/c", "jcr:/workspace/a/b/c");
        createUrl("   ", MavenUrl.NO_PORT, "workspace", "/a/b/c", "jcr:/workspace/a/b/c");
    }

    @Test
    public void shouldCreateUrlWithPortWorkspaceAndPathAndNoHostname() throws Exception {
        createUrl(null, 10001, "workspace", "/a/b/c", "jcr:/workspace/a/b/c");
        createUrl("", 10001, "workspace", "/a/b/c", "jcr:/workspace/a/b/c");
        createUrl("   ", 10001, "workspace", "/a/b/c", "jcr:/workspace/a/b/c");
    }

    @Test
    public void shouldCreateUrlWithPathAndNoHostnameOrPortOrWorkspace() throws Exception {
        createUrl(null, MavenUrl.NO_PORT, null, "/a/b/c", "jcr://a/b/c");
        createUrl("", MavenUrl.NO_PORT, null, "/a/b/c", "jcr://a/b/c");
        createUrl("   ", MavenUrl.NO_PORT, null, "/a/b/c", "jcr://a/b/c");
    }
}
