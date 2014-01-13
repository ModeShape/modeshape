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
package org.modeshape.common;

/**
 * A generic {@link RuntimeException runtime exception} representing a catastrophic and/or unrecoverable failure of the system.
 */
public class SystemFailureException extends RuntimeException {

    /**
     */
    private static final long serialVersionUID = 8281373010920861138L;

    /**
     * Construct a system failure exception with no message.
     */
    public SystemFailureException() {
    }

    /**
     * Construct a system failure exception with a single message.
     * 
     * @param message the message describing the failure
     */
    public SystemFailureException( String message ) {
        super(message);

    }

    /**
     * Construct a system failure exception with another exception that is the cause of the failure.
     * 
     * @param cause the original cause of the failure
     */
    public SystemFailureException( Throwable cause ) {
        super(cause);

    }

    /**
     * Construct a system failure exception with a single message and another exception that is the cause of the failure.
     * 
     * @param message the message describing the failure
     * @param cause the original cause of the failure
     */
    public SystemFailureException( String message,
                                   Throwable cause ) {
        super(message, cause);

    }

}
