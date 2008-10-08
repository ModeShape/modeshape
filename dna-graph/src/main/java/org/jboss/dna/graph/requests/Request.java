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
package org.jboss.dna.graph.requests;

import org.jboss.dna.graph.connectors.RepositoryConnection;

/**
 * The abstract base class for all classes representing requests to be executed against a {@link RepositoryConnection}.
 * 
 * @author Randall Hauch
 */
public abstract class Request {

    private Throwable error;

    /**
     * Set the error for this request.
     * 
     * @param error the error to be associated with this request, or null if this request is to have no error
     */
    public void setError( Throwable error ) {
        this.error = error;
    }

    /**
     * Return whether there is an error associated with this request
     * 
     * @return true if there is an error, or false otherwise
     */
    public boolean hasError() {
        return this.error != null;
    }

    /**
     * Get the error associated with this request, if there is such an error.
     * 
     * @return the error, or null if there is none
     */
    public Throwable getError() {
        return error;
    }

}
