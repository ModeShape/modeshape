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
package org.modeshape.graph.connector;

import net.jcip.annotations.Immutable;

/**
 * A runtime exception signalling an error within a {@link RepositorySource}.
 */
@Immutable
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
    public RepositorySourceException( String sourceName,
                                      String message ) {
        super(message);
        this.sourceName = sourceName;
    }

    /**
     * @param sourceName the identifier of the source from which this exception eminates
     * @param cause
     */
    public RepositorySourceException( String sourceName,
                                      Throwable cause ) {
        super(cause);
        this.sourceName = sourceName;
    }

    /**
     * @param sourceName the identifier of the source from which this exception eminates
     * @param message
     * @param cause
     */
    public RepositorySourceException( String sourceName,
                                      String message,
                                      Throwable cause ) {
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
