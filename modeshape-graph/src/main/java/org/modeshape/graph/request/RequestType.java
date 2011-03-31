package org.modeshape.graph.request;

/**
 * Enumeration of supported request types.
 */
public enum RequestType {
    ACCESS_QUERY,
    COMPOSITE,
    CLONE_BRANCH,
    CLONE_WORKSPACE,
    COLLECT_GARBAGE,
    COPY_BRANCH,
    CREATE_NODE,
    CREATE_WORKSPACE,
    DELETE_BRANCH,
    DELETE_CHILDREN,
    DESTROY_WORKSPACE,
    FULL_TEXT_SEARCH,
    GET_WORKSPACES,
    LAST,
    LOCK_BRANCH,
    MOVE_BRANCH,
    READ_ALL_CHILDREN,
    READ_ALL_PROPERTIES,
    READ_BLOCK_OF_CHILDREN,
    READ_BRANCH,
    READ_NEXT_BLOCK_OF_CHILDREN,
    READ_NODE,
    READ_PROPERTY,
    REMOVE_PROPERTY,
    RENAME_NODE,
    SET_PROPERTY,
    UNLOCK_BRANCH,
    UPDATE_PROPERTIES,
    UPDATE_VALUES,
    VERIFY_NODE_EXISTS,
    VERIFY_WORKSPACE,
    INVALID, // used for testing
    FUNCTION,

    // Never rearrange these literals, or the integer value will change, affecting serialized instances
}
