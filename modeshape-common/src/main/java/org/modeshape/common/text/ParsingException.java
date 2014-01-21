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
package org.modeshape.common.text;

/**
 * An exception representing a problem during parsing of text.
 */
public class ParsingException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final Position position;

    /**
     * @param position the position of the error; never null
     */
    public ParsingException( Position position ) {
        super();
        this.position = position;
    }

    /**
     * @param position the position of the error; never null
     * @param message the message
     * @param cause the underlying cause
     */
    public ParsingException( Position position,
                             String message,
                             Throwable cause ) {
        super(message, cause);
        this.position = position;
    }

    /**
     * @param position the position of the error; never null
     * @param message the message
     */
    public ParsingException( Position position,
                             String message ) {
        super(message);
        this.position = position;
    }

    public Position getPosition() {
        return position;
    }
}
