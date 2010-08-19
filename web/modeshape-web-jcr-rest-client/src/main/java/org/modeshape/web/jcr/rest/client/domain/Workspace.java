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

import net.jcip.annotations.Immutable;

import org.modeshape.common.util.HashCode;
import org.modeshape.web.jcr.rest.client.RestClientI18n;

/**
 * The <code>Workspace</code> class is the business object for a ModeShape repository workspace.
 */
@Immutable
public class Workspace implements IModeShapeObject {

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The workspace name.
     */
    private final String name;

    /**
     * The repository where this workspace resides.
     */
    private final Repository repository;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * Constructs a new <code>Workspace</code>.
     * 
     * @param name the workspace name (never <code>null</code>)
     * @param repository the repository where this workspace resides (never <code>null</code>)
     * @throws IllegalArgumentException if any of the arguments are <code>null</code>
     */
    public Workspace( String name,
                      Repository repository ) {
    	assert name != null;
    	assert repository != null;
        this.name = name;
        this.repository = repository;
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

        Workspace otherWorkspace = (Workspace)obj;
        return (this.name.equals(otherWorkspace.name) && this.repository.equals(otherWorkspace.repository));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.web.jcr.rest.client.domain.IModeShapeObject#getName()
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the repository where this workspace is located (never <code>null</code>)
     */
    public Repository getRepository() {
        return this.repository;
    }

    /**
     * @return the server where this workspace is located (never <code>null</code>)
     */
    public Server getServer() {
        return this.repository.getServer();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.web.jcr.rest.client.domain.IModeShapeObject#getShortDescription()
     */
    public String getShortDescription() {
        return RestClientI18n.workspaceShortDescription.text(this.name, this.repository.getName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(this.name, this.repository);
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
