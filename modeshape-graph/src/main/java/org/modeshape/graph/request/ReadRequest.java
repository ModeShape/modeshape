package org.modeshape.graph.request;

/**
 * A {@link Request} to read some set of nodes or properties from a graph.
 * 
 */
public interface ReadRequest {

    /**
     * Get the desired name of the workspace to access.
     * 
     * @return the desired name for the workspace
     */
    String readWorkspace();
}
