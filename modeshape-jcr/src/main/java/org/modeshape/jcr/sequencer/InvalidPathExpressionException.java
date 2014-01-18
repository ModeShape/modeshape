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
package org.modeshape.jcr.sequencer;

import org.modeshape.common.annotation.Immutable;

/**
 * A runtime exception that represents that an invalid {@link PathExpression path expression} was specified.
 */
@Immutable
public class InvalidPathExpressionException extends RuntimeException {

    /**
     */
    private static final long serialVersionUID = 358951801604727022L;

    /**
     * 
     */
    public InvalidPathExpressionException() {
    }

    /**
     * @param message
     */
    public InvalidPathExpressionException( String message ) {
        super(message);

    }

    /**
     * @param cause
     */
    public InvalidPathExpressionException( Throwable cause ) {
        super(cause);

    }

    /**
     * @param message
     * @param cause
     */
    public InvalidPathExpressionException( String message,
                                           Throwable cause ) {
        super(message, cause);

    }

}
