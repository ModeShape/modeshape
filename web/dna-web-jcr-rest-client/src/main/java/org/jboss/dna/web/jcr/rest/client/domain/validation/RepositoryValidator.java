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
import org.jboss.dna.web.jcr.rest.client.domain.Server;

/**
 * The <code>RepositoryValidator</code> class provides validation for creating a {@link Repository DNA repository}.
 */
public final class RepositoryValidator {

    // ===========================================================================================================================
    // Class Methods
    // ===========================================================================================================================

    /**
     * @param name the name being validated
     * @return a validation status (never <code>null</code>)
     */
    public static Status isNameValid( String name ) {
        if ((name == null) || (name.length() == 0)) {
            return new Status(Severity.ERROR, RestClientI18n.repositoryEmptyNameMsg.text(), null);
        }

        return Status.OK_STATUS;
    }

    /**
     * @param server the server being validated
     * @return a validation status (never <code>null</code>)
     */
    public static Status isServerValid( Server server ) {
        if (server == null) {
            return new Status(Severity.ERROR, RestClientI18n.repositoryNullServerMsg.text(), null);
        }

        return Status.OK_STATUS;
    }

    /**
     * @param name the name being validated
     * @param server the server being validated
     * @return a validation status (never <code>null</code>)
     */
    public static Status isValid( String name,
                                  Server server ) {
        Status status = isNameValid(name);

        if (!status.isError()) {
            status = isServerValid(server);
        }

        return status;
    }

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * Disallow construction.
     */
    private RepositoryValidator() {
        // nothing to do
    }

}
