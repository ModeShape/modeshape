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

package org.modeshape.search.lucene;

/**
 * A {@link RuntimeException runtime exception} representing a problem operating against Lucene.
 */
public class LuceneException extends RuntimeException {

    /**
     */
    private static final long serialVersionUID = 8281373010920861138L;

    /**
     * Construct a system failure exception with no message.
     */
    public LuceneException() {
    }

    /**
     * Construct a system failure exception with a single message.
     * 
     * @param message the message describing the failure
     */
    public LuceneException( String message ) {
        super(message);

    }

    /**
     * Construct a system failure exception with another exception that is the cause of the failure.
     * 
     * @param cause the original cause of the failure
     */
    public LuceneException( Throwable cause ) {
        super(cause);

    }

    /**
     * Construct a system failure exception with a single message and another exception that is the cause of the failure.
     * 
     * @param message the message describing the failure
     * @param cause the original cause of the failure
     */
    public LuceneException( String message,
                            Throwable cause ) {
        super(message, cause);

    }

}
