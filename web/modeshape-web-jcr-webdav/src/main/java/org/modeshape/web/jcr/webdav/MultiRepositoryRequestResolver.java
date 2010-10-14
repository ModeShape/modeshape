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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 * A {@link RequestResolver} implementation that expects the first segment of the URI is the repository name, the second is the
 * workspace name, and the remaining form the node path. This resolver does handle the case when only the repository name is
 * specified, or only the repository and workspace names are supplied, or when not even the repository name is given.
 * 
 * @see SingleRepositoryRequestResolver
 */
public class MultiRepositoryRequestResolver implements RequestResolver {

    /**
     * The string representation of the Java version of the {@link #PATH_PATTERN} regular expression.
     */
    protected static final String PATH_PATTERN_STRING = "/?(([^/]*)(/([^/]*)?(/(.*))?)?)?";

    /**
     * The regular expression that is used to extract the repository name, workspace name, and node path. Group 2 will contain the
     * repository name, group 4 the workspace name, group 5 the node path (with leading slash). Any of these groups may be empty
     * (or null).
     * <p>
     * The regular expression is <code>/?(([^/]*)(/([^/]*)?(/(.*))?)?)?</code>.
     */
    protected static final Pattern PATH_PATTERN = Pattern.compile(PATH_PATTERN_STRING);

    protected static final int REPOSITORY_NAME_GROUP = 2;
    protected static final int WORKSPACE_WITH_SLASH_GROUP = 3;
    protected static final int WORKSPACE_NAME_GROUP = 4;
    protected static final int PATH_GROUP = 5;

    protected static final String ROOT_NODE_PATH = "/";

    @Override
    public void initialize( ServletContext context ) {
        // nothing to do
    }

    @Override
    public ResolvedRequest resolve( HttpServletRequest request,
                                    String relativePath ) {
        if (relativePath != null && relativePath.length() != 0) {
            Matcher matcher = PATH_PATTERN.matcher(relativePath);
            if (matcher.matches()) {
                String repositoryName = matcher.group(REPOSITORY_NAME_GROUP); // may be null
                String workspaceName = matcher.group(WORKSPACE_NAME_GROUP); // may be null
                String nodePath = matcher.group(PATH_GROUP); // may be null
                if (nodePath == null) {
                    if (workspaceName != null) {
                        if (workspaceName.length() == 0 && "/".equals(matcher.group(WORKSPACE_WITH_SLASH_GROUP))) {
                            // There really isn't a workspace
                            workspaceName = null;
                        } else {
                            // There's a real workspace, so set the path to the root node ...
                            nodePath = ROOT_NODE_PATH;
                        }
                    } else if (repositoryName != null && repositoryName.length() == 0) {
                        // There's no path or workspace, and the repository name is blank ...
                        repositoryName = null;
                    }
                } else {
                    // There is a path, so make sure that the repository and workspace names exist ...
                    if (repositoryName == null) repositoryName = "";
                    else if (workspaceName == null) workspaceName = "";
                }
                return new ResolvedRequest(request, repositoryName, workspaceName, nodePath);
            }
        }
        return new ResolvedRequest(request, null, null, null);
    }
}
