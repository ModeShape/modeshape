package org.modeshape.web.jcr.rest;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import org.modeshape.web.jcr.rest.model.RepositoryEntry;
import net.jcip.annotations.Immutable;

/**
 * RESTEasy handler to provide the JCR repositories hosted on the server at the URI below. Please note that this URI assumes a
 * context of {@code /resources} for the web application.
 * <table border="1">
 * <tr>
 * <th>URI Pattern</th>
 * <th>Description</th>
 * <th>Supported Methods</th>
 * </tr>
 * <tr>
 * <td>/resources</td>
 * <td>returns a list of accessible repositories</td>
 * <td>GET</td>
 * </tr>
 * </table>
 */

@Immutable
@Path( "/" )
public class ServerResource extends AbstractJcrResource {

    /**
     * Returns the list of JCR repositories available on this server
     * 
     * @param request the servlet request; may not be null
     * @return the list of JCR repositories available on this server
     */
    @GET
    @Path( "/" )
    @Produces( "application/json" )
    public Map<String, RepositoryEntry> getRepositories( @Context HttpServletRequest request ) {
        assert request != null;

        Map<String, RepositoryEntry> repositories = new HashMap<String, RepositoryEntry>();

        for (String name : RepositoryFactory.getJcrRepositoryNames()) {
            if (name.trim().length() == 0) {
                name = EMPTY_REPOSITORY_NAME;
            }
            name = URL_ENCODER.encode(name);
            repositories.put(name, new RepositoryEntry(request.getContextPath(), name));
        }

        return repositories;
    }

}
