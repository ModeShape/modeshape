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

/**
 * An exception that signals a timeout has occurred.
 */
public class TimeoutException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Create a new timeout exception with the specified message and cause.
     * 
     * @param message the message
     * @param rootCause the cause
     */
    public TimeoutException( String message,
                             Throwable rootCause ) {
        super(message, rootCause);
    }

    /**
     * Create a new timeout exception with the specified message and cause.
     * 
     * @param message the message
     */
    public TimeoutException( String message ) {
        super(message);
    }

    /**
     * Create a new timeout exception with the specified message and cause.
     * 
     * @param rootCause the cause
     */
    public TimeoutException( Throwable rootCause ) {
        super(rootCause);
    }
}
