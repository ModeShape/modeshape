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

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.text.UrlEncoder;

/**
 * Wrapper for a URL that uses a format for referencing JCR nodes and content.
 */
public class MavenUrl {

    public static final int NO_PORT = -1;
    public static final String JCR_PROTOCOL = "jcr";
    protected static final String URL_PATH_DELIMITER = "/";

    private String hostname = "";
    private int port = NO_PORT;
    private String workspaceName = "";
    private String path = URL_PATH_DELIMITER;

    /**
     * Get the host name
     * 
     * @return the host name
     */
    public String getHostname() {
        return this.hostname;
    }

    /**
     * @param hostname the new host name
     */
    public void setHostname( String hostname ) {
        this.hostname = hostname != null ? hostname.trim() : "";
        this.hostname = trimDelimiters(this.hostname, true, true);
    }

    /**
     * Get the port. This method returns {@link #NO_PORT} if the port has not been specified.
     * 
     * @return the port
     */
    public int getPort() {
        return this.port;
    }

    /**
     * @param port the new port, or {@link #NO_PORT} if there is no port
     */
    public void setPort( int port ) {
        this.port = port;
    }

    public String getHostnameAndPort() {
        if (this.port == NO_PORT) return this.hostname;
        if (this.hostname.length() == 0) return "";
        return this.hostname + ":" + this.port;
    }

    /**
     * @return workspaceName
     */
    public String getWorkspaceName() {
        return this.workspaceName;
    }

    /**
     * Set the name of the workspace.
     * 
     * @param workspaceName the name of the workspace
     */
    public void setWorkspaceName( String workspaceName ) {
        this.workspaceName = workspaceName != null ? workspaceName.trim() : "";
        this.workspaceName = trimDelimiters(this.workspaceName, true, true);
    }

    protected String trimDelimiters( String string,
                                     boolean removeLeading,
                                     boolean removeTrailing ) {
        if (string == null || string.length() == 0) return "";
        if (removeLeading) string = string.replaceAll("^/+", "");
        if (removeTrailing) string = string.replaceAll("/+$", "");
        return string;
    }

    /**
     * @return path
     */
    public String getPath() {
        return this.path;
    }

    /**
     * @param path Sets path to the specified value.
     */
    public void setPath( String path ) {
        // Make sure the path starts with a '/' ...
        this.path = path != null ? URL_PATH_DELIMITER + path.trim() : URL_PATH_DELIMITER;
        this.path = this.path.replaceAll("^/{2,}", "/");
        assert this.path.startsWith(URL_PATH_DELIMITER);
    }

    /**
     * Get a URL that corresponds to the information in this object.
     * 
     * @param handler the URL stream handler that will be used to handle obtaining an input stream or an output stream on the
     *        resulting URL
     * @param encoder an encoder that will be used to escape any characters that are not allowed in URLs; {@link UrlEncoder} will
     *        be used if no encoder is specified
     * @return the URL
     * @throws MalformedURLException if the resulting URL would be malformed
     */
    public URL getUrl( URLStreamHandler handler,
                       TextEncoder encoder ) throws MalformedURLException {
        if (encoder == null) {
            encoder = new UrlEncoder().setSlashEncoded(false);
        }
        final boolean hasWorkspaceName = this.workspaceName.length() > 0;
        final boolean hasPath = this.path.length() > 1; // path includes leading delim
        String filePart = null;
        if (hasWorkspaceName && hasPath) {
            filePart = URL_PATH_DELIMITER + encoder.encode(this.workspaceName) + encoder.encode(this.path);
        } else if (hasWorkspaceName) {
            filePart = URL_PATH_DELIMITER + encoder.encode(this.workspaceName) + URL_PATH_DELIMITER;
        } else if (hasPath) {
            filePart = URL_PATH_DELIMITER + encoder.encode(this.path);
        } else {
            filePart = URL_PATH_DELIMITER;
        }
        int actualPort = this.hostname.length() != 0 ? this.port : NO_PORT;
        return new URL(JCR_PROTOCOL, this.hostname, actualPort, filePart, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        UrlEncoder encoder = new UrlEncoder().setSlashEncoded(false);
        String encodedWorkspace = encoder.encode(this.workspaceName);
        String encodedPath = encoder.encode(this.path);
        final boolean hasHostname = this.hostname.length() > 0;
        final boolean hasPath = encodedPath.length() > 1; // path includes leading delim
        StringBuilder sb = new StringBuilder();
        sb.append(JCR_PROTOCOL).append(":");
        if (hasHostname) {
            sb.append("//").append(this.hostname);
            if (this.port != NO_PORT) {
                sb.append(":").append(this.port);
            }
        }
        sb.append(URL_PATH_DELIMITER).append(encodedWorkspace);
        if (hasPath) {
            sb.append(encodedPath);
        } else {
            sb.append(URL_PATH_DELIMITER);
        }
        return sb.toString();
    }

    /**
     * Parse the supplied URL and determine if the URL fits the JCR URL format. If it does, return a {@link MavenUrl} instance;
     * otherwise return null. If the URL is malformed or otherwise invalid, this method also returns null.
     * <p>
     * The URL format is expected to fit the following pattern:
     * 
     * <pre>
     *    jcr://hostname:port/workspaceName/path/to/node
     * </pre>
     * 
     * where
     * <ul>
     * <li><b>hostname</b> is the name of the repository's host; typically, this is unspecified to refer to a repository in the
     * same VM</li>
     * <li><b>port</b> is the port on the host. If the hostname is unspecified, the port should be excluded.</li>
     * <li><b>workspaceName</b> is the name of the workspace in the repository</li>
     * <li><b>path/to/node</b> is the path of the node or property that is to be referenced</li>
     * </ul>
     * </p>
     * 
     * @param url the URL to be parsed
     * @param decoder the text encoder that should be used to decode the URL; may be null if no decoding should be done
     * @return the object representing the JCR information contained in the URL
     * @see #parse(URL, TextDecoder)
     */
    public static MavenUrl parse( String url,
                                  TextDecoder decoder ) {
        if (decoder == null) decoder = new UrlEncoder();
        // This regular expression has the following groups:
        // 1) //hostname:port
        // 2) hostname:port
        // 3) hostname
        // 4) :port
        // 5) port
        // 6) workspaceName
        // 7) path, including leading '/'
        Pattern urlPattern = Pattern.compile("jcr:(//(([^/:]*)(:([^/]*))?))?/([^/]*)(/?.*)");
        Matcher matcher = urlPattern.matcher(url);
        MavenUrl result = null;
        if (matcher.find()) {
            result = new MavenUrl();
            result.setHostname(matcher.group(3));
            String portStr = matcher.group(5);
            if (portStr != null && portStr.trim().length() != 0) {
                result.setPort(Integer.parseInt(portStr));
            }
            String workspaceName = decoder.decode(matcher.group(6));
            String path = decoder.decode(matcher.group(7));
            result.setWorkspaceName(workspaceName);
            result.setPath(path);
        }
        return result;
    }

    /**
     * Parse the supplied URL and determine if the URL fits the JCR URL format. If it does, return a {@link MavenUrl} instance;
     * otherwise return null. If the URL is malformed or otherwise invalid, this method also returns null.
     * 
     * @param url the URL to be parsed
     * @param decoder the text encoder that should be used to decode the URL; may be null if no decoding should be done
     * @return the object representing the JCR information contained in the URL
     * @see #parse(String,TextDecoder)
     */
    public static MavenUrl parse( URL url,
                                  TextDecoder decoder ) {
        if (url == null) return null;
        return parse(url.toExternalForm(), decoder);
    }
}
