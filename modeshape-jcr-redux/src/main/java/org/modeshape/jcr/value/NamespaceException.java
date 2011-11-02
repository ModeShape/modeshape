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
package org.modeshape.jcr.value;

import org.modeshape.common.annotation.Immutable;

/**
 * A runtime exception denoting that a namespace was invalid or not found.
 */
@Immutable
public class NamespaceException extends RuntimeException {

    /**
     */
    private static final long serialVersionUID = 1300642242538881207L;

    /**
     * 
     */
    public NamespaceException() {
    }

    /**
     * @param message
     */
    public NamespaceException( String message ) {
        super(message);

    }

    /**
     * @param cause
     */
    public NamespaceException( Throwable cause ) {
        super(cause);

    }

    /**
     * @param message
     * @param cause
     */
    public NamespaceException( String message,
                               Throwable cause ) {
        super(message, cause);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return super.toString();
    }
}
