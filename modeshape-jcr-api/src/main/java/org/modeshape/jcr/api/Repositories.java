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
package org.modeshape.jcr.api;

import java.util.Set;

/**
 * Representation of a generic container of multiple {@link javax.jcr.Repository Repository} instances. This interface helps
 * decouple applications that use JCR repositories from the actual JCR implementation.
 */
public interface Repositories {

    /**
     * Get the names of the available repositories.
     * 
     * @return the immutable set of repository names provided by this server; never null
     */
    Set<String> getRepositoryNames();

    /**
     * Return the JCR Repository with the supplied name.
     * 
     * @param repositoryName the name of the repository to return; may not be null
     * @return the repository with the given name; never null
     * @throws javax.jcr.RepositoryException if no repository exists with the given name or there is an error communicating with
     *         the repository
     */
    javax.jcr.Repository getRepository( String repositoryName ) throws javax.jcr.RepositoryException;
}
