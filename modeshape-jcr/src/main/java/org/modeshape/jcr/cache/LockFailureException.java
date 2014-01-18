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
 * An exception signalling that a node could not be locked.
 */
public class LockFailureException extends DocumentNotFoundException {

    private static final long serialVersionUID = 1L;

    private final NodeKey key;

    /**
     * @param key the key for the node that could not be locked
     */
    public LockFailureException( NodeKey key ) {
        super(key.toString());
        this.key = key;
    }

    /**
     * @param key the key for the node that could not be locked
     * @param message the message
     */
    public LockFailureException( NodeKey key,
                                 String message ) {
        super(key.toString(), message);
        this.key = key;
    }

    /**
     * @param key the key for the node that could not be locked
     * @param cause the cause of this exception
     */
    public LockFailureException( NodeKey key,
                                 Throwable cause ) {
        super(key.toString(), cause);
        this.key = key;
    }

    /**
     * @param key the key for the node that could not be locked
     * @param message the message
     * @param cause the cause of this exception
     */
    public LockFailureException( NodeKey key,
                                 String message,
                                 Throwable cause ) {
        super(key.toString(), message, cause);
        this.key = key;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    /**
     * Get the key for the node that could not be locked
     * 
     * @return the key for the node
     */
    public NodeKey getNodeKey() {
        return key;
    }
}
