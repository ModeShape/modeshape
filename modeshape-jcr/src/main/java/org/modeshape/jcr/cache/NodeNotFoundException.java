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
 * An exception signalling that a node does not exist.
 */
public class NodeNotFoundException extends DocumentNotFoundException {

    private static final long serialVersionUID = 1L;

    private final NodeKey key;

    /**
     * @param key the key for the node that does not exist
     */
    public NodeNotFoundException( NodeKey key ) {
        super(key.toString());
        this.key = key;
    }

    /**
     * @param key the key for the node that does not exist
     * @param message the message
     */
    public NodeNotFoundException( NodeKey key,
                                  String message ) {
        super(key.toString(), message);
        this.key = key;
    }

    /**
     * @param key the key for the node that does not exist
     * @param cause the cause of this exception
     */
    public NodeNotFoundException( NodeKey key,
                                  Throwable cause ) {
        super(key.toString(), cause);
        this.key = key;
    }

    /**
     * @param key the key for the node that does not exist
     * @param message the message
     * @param cause the cause of this exception
     */
    public NodeNotFoundException( NodeKey key,
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
     * Get the key for the node that was not found
     * 
     * @return the key for the node
     */
    public NodeKey getNodeKey() {
        return key;
    }
}
