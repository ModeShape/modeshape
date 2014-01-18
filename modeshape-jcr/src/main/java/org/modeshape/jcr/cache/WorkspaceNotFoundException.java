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
public class WorkspaceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String name;

    /**
     * @param name the workspace name
     */
    public WorkspaceNotFoundException( String name ) {
        super("The workspace " + name + " was not found");
        this.name = name;
    }

    /**
     * @param name the workspace name
     * @param message the message
     */
    public WorkspaceNotFoundException( String name,
                                       String message ) {
        super(message);
        this.name = name;
    }

    /**
     * @param name the workspace name
     * @param cause the cause of this exception
     */
    public WorkspaceNotFoundException( String name,
                                       Throwable cause ) {
        super("The workspace " + name + " was not found", cause);
        this.name = name;
    }

    /**
     * @param name the workspace name
     * @param message the message
     * @param cause the cause of this exception
     */
    public WorkspaceNotFoundException( String name,
                                       String message,
                                       Throwable cause ) {
        super(message, cause);
        this.name = name;
    }

    /**
     * Get the name of the workspace that was not found.
     * 
     * @return the workspace name
     */
    public String getName() {
        return name;
    }
}
