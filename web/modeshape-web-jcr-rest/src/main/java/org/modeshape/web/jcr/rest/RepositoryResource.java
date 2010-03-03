package org.modeshape.web.jcr.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import net.jcip.annotations.Immutable;
import org.modeshape.web.jcr.rest.model.WorkspaceEntry;

/**
 * RESTEasy handler to provide the JCR repository at the URI below. Please note that the URI assumes a context of {@code
 * /resources} for the web application.
 * <table border="1">
 * <tr>
 * <th>URI Pattern</th>
 * <th>Description</th>
 * <th>Supported Methods</th>
 * </tr>
 * <tr>
 * <td>/resources/{repositoryName}</td>
 * <td>returns a list of accessible workspaces within that repository</td>
 * <td>GET</td>
 * </tr>
 * </table>
 */
@Immutable
@Path("/")
public class RepositoryResource extends AbstractJcrResource {


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
    @GET
    @Path( "/{repositoryName}" )
    @Produces( "application/json" )
    public Map<String, WorkspaceEntry> getWorkspaces( @Context HttpServletRequest request,
                                                      @PathParam( "repositoryName" ) String rawRepositoryName )
        throws RepositoryException, IOException {

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
