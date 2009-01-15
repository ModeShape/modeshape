/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.graph.connector;

/**
 * @author Randall Hauch
 */
public class RepositorySourceException extends RuntimeException {

    /**
     */
    private static final long serialVersionUID = -7704170453962924565L;
    private final String sourceName;

    /**
     * @param sourceName the identifier of the source from which this exception eminates
     */
    public RepositorySourceException( String sourceName ) {
        this.sourceName = sourceName;
    }

    /**
     * @param sourceName the identifier of the source from which this exception eminates
     * @param message
     */
    public RepositorySourceException( String sourceName, String message ) {
        super(message);
        this.sourceName = sourceName;
    }

    /**
     * @param sourceName the identifier of the source from which this exception eminates
     * @param cause
     */
    public RepositorySourceException( String sourceName, Throwable cause ) {
        super(cause);
        this.sourceName = sourceName;
    }

    /**
     * @param sourceName the identifier of the source from which this exception eminates
     * @param message
     * @param cause
     */
    public RepositorySourceException( String sourceName, String message, Throwable cause ) {
        super(message, cause);
        this.sourceName = sourceName;
    }

    /**
     * @return sourceName
     */
    public String getSourceName() {
        return this.sourceName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return super.toString();
    }

}
