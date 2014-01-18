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
package org.modeshape.jcr.cache;

/**
 * An exception signalling that a document could not be created because one already exists in the database.
 */
public class DocumentAlreadyExistsException extends DocumentStoreException {

    private static final long serialVersionUID = 1L;

    /**
     * @param key the key for the node that does not exist
     */
    public DocumentAlreadyExistsException( String key ) {
        super(key);
    }

    /**
     * @param key the key for the node that does not exist
     * @param message the message
     */
    public DocumentAlreadyExistsException( String key,
                                           String message ) {
        super(message);
    }

    /**
     * @param key the key for the node that does not exist
     * @param cause the cause of this exception
     */
    public DocumentAlreadyExistsException( String key,
                                           Throwable cause ) {
        super(key.toString(), cause);
    }

    /**
     * @param key the key for the node that does not exist
     * @param message the message
     * @param cause the cause of this exception
     */
    public DocumentAlreadyExistsException( String key,
                                           String message,
                                           Throwable cause ) {
        super(message, cause);
    }
}
