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
package org.modeshape.jcr.index.elasticsearch;

/**
 * Elasticsearch index specific exception.
 * 
 * @author kulikov
 */
public class EsIndexException extends RuntimeException {
    /**
     * Constructs a new instance of this class with <code>null</code> as its detail message.
     */
    public EsIndexException() {
    }

    /**
     * Constructs a new instance of this class with the specified detail message.
     * 
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
     */
    public EsIndexException( String message ) {
        super(message);
    }

    /**
     * Constructs a new instance of this class with the specified root cause.
     * 
     * @param rootCause root failure cause
     */
    public EsIndexException( Throwable rootCause ) {
        super(rootCause);
    }
    
}
