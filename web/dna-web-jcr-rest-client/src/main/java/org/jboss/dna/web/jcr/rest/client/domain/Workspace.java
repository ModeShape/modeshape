/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.web.jcr.rest.client.domain;

import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.HashCode;
import org.jboss.dna.web.jcr.rest.client.RestClientI18n;
import org.jboss.dna.web.jcr.rest.client.Status;
import org.jboss.dna.web.jcr.rest.client.domain.validation.WorkspaceValidator;

/**
 * The <code>Workspace</code> class is the business object for a DNA repository workspace.
 */
@Immutable
public final class Workspace implements IDnaObject {

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
     * @see WorkspaceValidator
     * @throws RuntimeException if any of the input parameters are invalid
     */
    public Workspace( String name,
                      Repository repository ) {
        CheckArg.isNotNull(name, "name"); //$NON-NLS-1$
        CheckArg.isNotNull(repository, "repository"); //$NON-NLS-1$

        // validate inputs
        Status status = WorkspaceValidator.isValid(name, repository);

        if (status.isError()) {
            throw new RuntimeException(status.getMessage(), status.getException());
        }

        // valid so construct
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

        // must have another server
        Workspace otherWorkspace = (Workspace)obj;
        return (this.name.equals(otherWorkspace.name) && this.repository.equals(otherWorkspace.repository));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.web.jcr.rest.client.domain.IDnaObject#getName()
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
     * @see org.jboss.dna.web.jcr.rest.client.domain.IDnaObject#getShortDescription()
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
