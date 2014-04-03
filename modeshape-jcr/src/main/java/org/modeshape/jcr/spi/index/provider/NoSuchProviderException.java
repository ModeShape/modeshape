/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.modeshape.jcr.spi.index.provider;

import javax.jcr.RepositoryException;

/**
 * Exception used when an index does not exist.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class NoSuchProviderException extends RepositoryException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new instance of this class with <code>null</code> as its detail message.
     */
    public NoSuchProviderException() {
    }

    /**
     * Constructs a new instance of this class with the specified detail message.
     * 
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
     */
    public NoSuchProviderException( String message ) {
        super(message);
    }

    /**
     * Constructs a new instance of this class with the specified root cause.
     * 
     * @param rootCause root failure cause
     */
    public NoSuchProviderException( Throwable rootCause ) {
        super(rootCause);
    }

    /**
     * Constructs a new instance of this class with the specified detail message and root cause.
     * 
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
     * @param rootCause root failure cause
     */
    public NoSuchProviderException( String message,
                                 Throwable rootCause ) {
        super(message, rootCause);
    }

}
