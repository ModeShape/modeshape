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
package org.modeshape.web.jcr.rest.client.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.HashCode;
import org.modeshape.web.jcr.rest.client.RestClientI18n;

/**
 * The Repository class is the business object for a ModeShape repository.
 */
@Immutable
public class Repository implements IModeShapeObject {

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The repository name.
     */
    private final String name;

    /**
     * The server where this repository resides.
     */
    private final Server server;
    private final Map<String, Object> metadata;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * Constructs a new <code>Repository</code>.
     * 
     * @param name the repository name (never <code>null</code>)
     * @param server the server where this repository resides (never <code>null</code>)
     * @throws IllegalArgumentException if the name or server argument is <code>null</code>
     */
    public Repository( String name,
                       Server server ) {
        assert name != null;
        assert server != null;
        this.name = name;
        this.server = server;
        this.metadata = Collections.<String, Object>emptyMap();
    }

    /**
     * Constructs a new <code>Repository</code>.
     * 
     * @param name the repository name (never <code>null</code>)
     * @param server the server where this repository resides (never <code>null</code>)
     * @param metadata the metadata; may be null or empty
     * @throws IllegalArgumentException if the name or server argument is <code>null</code>
     */
    public Repository( String name,
                       Server server,
                       Map<String, Object> metadata ) {
        assert name != null;
        assert server != null;
        this.name = name;
        this.server = server;
        this.metadata = metadata != null ? Collections.unmodifiableMap(new HashMap<String, Object>(metadata)) : Collections.<String, Object>emptyMap();
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (this == obj) return true;
        if ((obj == null) || (getClass() != obj.getClass())) return false;

        Repository otherRepository = (Repository)obj;
        return (this.name.equals(otherRepository.name) && this.server.equals(otherRepository.server));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.web.jcr.rest.client.domain.IModeShapeObject#getName()
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * @return the server where this repository is located (never <code>null</code>)
     */
    public Server getServer() {
        return this.server;
    }

    /**
     * Get the metadata for this repository. This metadata typically includes all of a JCR Repository's descriptors, where the
     * values are usually strings or arrays of strings.
     * 
     * @return the immutable map of metadata; never null but possibly empty if the metadata is not known
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.web.jcr.rest.client.domain.IModeShapeObject#getShortDescription()
     */
    @Override
    public String getShortDescription() {
        return RestClientI18n.repositoryShortDescription.text(this.name, this.server.getName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(this.name, this.server);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getShortDescription();
    }

}
