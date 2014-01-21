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
 * An exception signalling that a failure occurred within a document store.
 */
public class DocumentStoreException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String key;

    /**
     * @param key the key for the document
     */
    public DocumentStoreException( String key ) {
        super(key);
        this.key = key;
    }

    /**
     * @param key the key for the document
     * @param message the message
     */
    public DocumentStoreException( String key,
                                   String message ) {
        super(message);
        this.key = key;
    }

    /**
     * @param key the key for the document
     * @param cause the cause of this exception
     */
    public DocumentStoreException( String key,
                                   Throwable cause ) {
        super(key.toString(), cause);
        this.key = key;
    }

    /**
     * @param key the key for the document
     * @param message the message
     * @param cause the cause of this exception
     */
    public DocumentStoreException( String key,
                                   String message,
                                   Throwable cause ) {
        super(message, cause);
        this.key = key;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    /**
     * Get the document key
     * 
     * @return the key for the document
     */
    public String getKey() {
        return key;
    }
}
