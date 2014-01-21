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
package org.modeshape.jcr;

import javax.jcr.RepositoryException;

/**
 * Exception thrown when an operation attempts to access a repository that does not exist.
 */
public class NoSuchRepositoryException extends RepositoryException {

    private static final long serialVersionUID = 1L;

    public NoSuchRepositoryException() {
        super();
    }

    public NoSuchRepositoryException( String message,
                                      Throwable rootCause ) {
        super(message, rootCause);
    }

    public NoSuchRepositoryException( String message ) {
        super(message);
    }

    public NoSuchRepositoryException( Throwable rootCause ) {
        super(rootCause);
    }

}
