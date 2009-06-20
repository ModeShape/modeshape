package org.jboss.dna.graph.request;

/**
 * An enumeration used by {@link CopyBranchRequest} for the choice of handling duplicate UUIDs, such as when a node is to be
 * copied to another location where a node already exists.
 * 
 * @author Randall Hauch
 */
public enum UuidConflictBehavior {

    ALWAYS_CREATE_NEW_UUID,
    REPLACE_EXISTING_NODE,
    THROW_EXCEPTION;
}
