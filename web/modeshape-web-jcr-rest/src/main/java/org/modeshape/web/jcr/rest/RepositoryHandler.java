package org.modeshape.web.jcr.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import net.jcip.annotations.Immutable;
import org.modeshape.web.jcr.rest.model.WorkspaceEntry;

/**
 * Resource handler that implements REST methods for repositories and workspaces.
 */
@Immutable
class RepositoryHandler extends AbstractHandler {

    /**
     * Returns the list of workspaces available to this user within the named repository.
     * 
     * @param rawRepositoryName the name of the repository; may not be null
     * @param request the servlet request; may not be null
     * @return the list of workspaces available to this user within the named repository.
     * @throws IOException if the given repository name does not map to any repositories and there is an error writing the error
     *         code to the response.
     * @throws RepositoryException if there is any other error accessing the list of available workspaces for the repository
     */
    public Map<String, WorkspaceEntry> getWorkspaces( HttpServletRequest request,
                                                      String rawRepositoryName ) throws RepositoryException, IOException {

        assert request != null;
        assert rawRepositoryName != null;

        Map<String, WorkspaceEntry> workspaces = new HashMap<String, WorkspaceEntry>();

        Session session = getSession(request, rawRepositoryName, null);
        rawRepositoryName = URL_ENCODER.encode(rawRepositoryName);

        for (String name : session.getWorkspace().getAccessibleWorkspaceNames()) {
            if (name.trim().length() == 0) {
                name = EMPTY_WORKSPACE_NAME;
            }
            name = URL_ENCODER.encode(name);
            workspaces.put(name, new WorkspaceEntry(request.getContextPath(), rawRepositoryName, name));
        }

        return workspaces;
    }

}
