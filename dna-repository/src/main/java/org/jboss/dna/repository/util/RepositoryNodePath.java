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
package org.jboss.dna.repository.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.HashCode;
import org.jboss.dna.repository.RepositoryI18n;

/**
 * @author Randall Hauch
 */
@Immutable
public class RepositoryNodePath {

    protected static final Pattern PATTERN = Pattern.compile("([^:/]):(/.*)");

    public static RepositoryNodePath parse( String path, String defaultRepositoryWorkspaceName ) {
        Matcher matcher = PATTERN.matcher(path);
        if (matcher.matches()) {
            try {
                return new RepositoryNodePath(matcher.group(1), matcher.group(2));
            } catch (Throwable t) {
                throw new IllegalArgumentException(RepositoryI18n.invalidRepositoryNodePath.text(path, t.getMessage()));
            }
        }
        return new RepositoryNodePath(defaultRepositoryWorkspaceName, path);

    }

    private final String repositoryName;
    private final String nodePath;
    private final int hc;

    public RepositoryNodePath( String repositoryName, String nodePath ) {
        this.repositoryName = repositoryName;
        this.nodePath = nodePath;
        this.hc = HashCode.compute(this.repositoryName, this.nodePath);
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
    public String getRepositoryWorkspaceName() {
        return this.repositoryName;
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
            if (!this.repositoryName.equals(that.repositoryName)) return false;
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
        return this.repositoryName + ":" + this.nodePath;
    }
}
