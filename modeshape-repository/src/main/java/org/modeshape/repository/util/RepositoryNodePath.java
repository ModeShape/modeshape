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
package org.modeshape.repository.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.jcip.annotations.Immutable;
import org.modeshape.common.util.HashCode;
import org.modeshape.repository.RepositoryI18n;

/**
 * An immutable representation of a path to a node within the named workspace of a named repository source.
 */
@Immutable
public class RepositoryNodePath {

    protected static final Pattern PATTERN = Pattern.compile("([^:/]):(/.*)");

    public static RepositoryNodePath parse( String path,
                                            String repositorySourceName,
                                            String defaultRepositoryWorkspaceName ) {
        Matcher matcher = PATTERN.matcher(path);
        if (matcher.matches()) {
            try {
                return new RepositoryNodePath(repositorySourceName, matcher.group(1), matcher.group(2));
            } catch (Throwable t) {
                throw new IllegalArgumentException(RepositoryI18n.invalidRepositoryNodePath.text(path, t.getMessage()));
            }
        }
        return new RepositoryNodePath(repositorySourceName, defaultRepositoryWorkspaceName, path);

    }

    private final String repositorySourceName;
    private final String workspaceName;
    private final String nodePath;
    private final int hc;

    public RepositoryNodePath( String repositorySourceName,
                               String workspaceName,
                               String nodePath ) {
        this.repositorySourceName = repositorySourceName;
        this.workspaceName = workspaceName;
        this.nodePath = nodePath;
        this.hc = HashCode.compute(this.repositorySourceName, this.workspaceName, this.nodePath);
    }

    /**
     * @return nodePath
     */
    public String getNodePath() {
        return this.nodePath;
    }

    /**
     * @return repositoryName
     */
    public String getRepositorySourceName() {
        return this.repositorySourceName;
    }

    /**
     * @return the workspace name
     */
    public String getWorkspaceName() {
        return this.workspaceName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return this.hc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof RepositoryNodePath) {
            RepositoryNodePath that = (RepositoryNodePath)obj;
            if (!this.repositorySourceName.equals(that.repositorySourceName)) return false;
            if (!this.workspaceName.equals(that.workspaceName)) return false;
            if (!this.nodePath.equals(that.nodePath)) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return this.repositorySourceName + ":" + this.workspaceName + ":" + this.nodePath;
    }
}
