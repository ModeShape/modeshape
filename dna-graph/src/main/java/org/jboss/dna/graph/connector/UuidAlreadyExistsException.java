package org.jboss.dna.graph.connector;

import java.util.UUID;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.request.CopyBranchRequest;
import org.jboss.dna.graph.request.UuidConflictBehavior;

/**
 * Exception that indicates that a copy request failed because one of the UUIDs in the source branch already exists in the target
 * workspace and the {@link CopyBranchRequest#uuidConflictBehavior() UUID conflict behavior} is set to
 * {@link UuidConflictBehavior#THROW_EXCEPTION}.
 */
public class UuidAlreadyExistsException extends RepositorySourceException {

    private static final long serialVersionUID = 1L;

    public UuidAlreadyExistsException( String repositorySourceName,
                                       UUID uuid,
                                       String pathAsString,
                                       String workspaceName ) {
        super(repositorySourceName, GraphI18n.nodeAlreadyExistsWithUuid.text(uuid, pathAsString, workspaceName));
    }

}
