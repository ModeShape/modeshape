package org.modeshape.web.jcr.rest.handler;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.web.jcr.rest.RestHelper;

/**
 * Resource handler that implements REST methods for repositories and workspaces.
 *
 * @deprecated since 3.0, use {@link RestRepositoryHandler}
 */
@Immutable
public class RepositoryHandler extends AbstractHandler {

    /**
     * Returns the list of workspaces available to this user within the named repository.
     * 
     * @param rawRepositoryName the name of the repository; may not be null
     * @param request the servlet request; may not be null
     * @return the list of workspaces available to this user within the named repository.
     * @throws JSONException if there is an error encoding the response
     * @throws RepositoryException if there is any other error accessing the list of available workspaces for the repository
     *
     * @deprecated since 3.0
     */
    public String getWorkspaces( HttpServletRequest request,
                          String rawRepositoryName ) throws JSONException, RepositoryException {

        assert request != null;
        assert rawRepositoryName != null;

        JSONObject workspaces = new JSONObject();

        Session session = getSession(request, rawRepositoryName, null);
        rawRepositoryName = RestHelper.URL_ENCODER.encode(rawRepositoryName);

        String uri = request.getRequestURI();
        uri = uri.substring(0, uri.length() - rawRepositoryName.length() - 1);

        for (String name : session.getWorkspace().getAccessibleWorkspaceNames()) {
            if (name.trim().length() == 0) {
                name = EMPTY_WORKSPACE_NAME;
            }
            name = RestHelper.URL_ENCODER.encode(name);

            JSONObject workspace = new JSONObject();
            JSONObject resources = new JSONObject();
            String uriPrefix = uri + "/" + rawRepositoryName + "/" + name;
            resources.put("query", uriPrefix + "/query");
            resources.put("items", uriPrefix + "/items");
            workspace.put("name", name);
            workspace.put("resources", resources);

            JSONObject wrapper = new JSONObject();
            wrapper.put("workspace", workspace);
            workspaces.put(name, wrapper);
        }

        return RestHelper.responseString(workspaces, request);
    }

}
