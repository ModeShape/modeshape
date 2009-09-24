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
package org.jboss.dna.web.jcr.rest.client.domain.validation;

import org.jboss.dna.web.jcr.rest.client.RestClientI18n;
import org.jboss.dna.web.jcr.rest.client.Status;
import org.jboss.dna.web.jcr.rest.client.Status.Severity;
import org.jboss.dna.web.jcr.rest.client.domain.Repository;
import org.jboss.dna.web.jcr.rest.client.domain.Workspace;

/**
 * The <code>WorkspaceValidator</code> class provides validation for creating a {@link Workspace DNA workspace}.
 */
public final class WorkspaceValidator {

    // ===========================================================================================================================
    // Class Methods
    // ===========================================================================================================================

    /**
     * @param name the name being validated
     * @return a validation status (never <code>null</code>)
     */
    public static Status isNameValid( String name ) {
        if ((name == null) || (name.length() == 0)) {
            return new Status(Severity.ERROR, RestClientI18n.workspaceEmptyNameMsg.text(), null);
        }

        return Status.OK_STATUS;
    }

    /**
     * @param repository the repository being validated
     * @return a validation status (never <code>null</code>)
     */
    public static Status isRepositoryValid( Repository repository ) {
        if (repository == null) {
            return new Status(Severity.ERROR, RestClientI18n.workspaceNullRepositoryMsg.text(), null);
        }

        return Status.OK_STATUS;
    }

    /**
     * @param name the name being validated
     * @param repository the repository being validated
     * @return a validation status (never <code>null</code>)
     */
    public static Status isValid( String name,
                                  Repository repository ) {
        Status status = isNameValid(name);

        if (!status.isError()) {
            status = isRepositoryValid(repository);
        }

        return status;
    }

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * Disallow construction.
     */
    private WorkspaceValidator() {
        // nothing to do
    }

}
