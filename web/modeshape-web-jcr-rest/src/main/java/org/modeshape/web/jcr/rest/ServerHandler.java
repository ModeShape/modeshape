package org.modeshape.web.jcr.rest;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.web.jcr.RepositoryFactory;

/**
 * Resource handler that implements REST methods for servers.
 */
@Immutable
class ServerHandler extends AbstractHandler {

    /**
     * Returns the list of JCR repositories available on this server
     * 
     * @param request the servlet request; may not be null
     * @return the JSON-encoded version of the item (and, if the item is a node, its subgraph, depending on the value of
     *         {@code depth})
     * @throws JSONException if there is an error encoding the node
     * @throws RepositoryException if any other error occurs
     */
    public String getRepositories( HttpServletRequest request ) throws JSONException, RepositoryException {
        assert request != null;

        JSONObject jsonRepositories = new JSONObject();
        String uri = request.getRequestURI();

        if (uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }

        for (String repoName : RepositoryFactory.getJcrRepositoryNames()) {
            if (repoName.trim().length() == 0) {
                repoName = EMPTY_REPOSITORY_NAME;
            }
            String name = URL_ENCODER.encode(repoName);
            JSONObject repository = new JSONObject();
            JSONObject resources = new JSONObject();
            resources.put("workspaces", uri + "/" + name);
            repository.put("name", name);
            repository.put("resources", resources);

            // Get a Session so we can get the descriptors ...
            try {
                String workspaceName = null;
                Session session = getSession(request, name, workspaceName);
                if (session != null) {
                    JSONObject metadata = getRepositoryMetadata(session);
                    if (metadata != null) {
                        repository.put("metadata", metadata);
                    }
                }
                JSONObject mapped = new JSONObject();
                mapped.put("repository", repository);
                jsonRepositories.put(name, mapped);
                // } catch (RepositoryException e) {
                // // Ignore, because we can't log in and thus cannot figure out any of the workspace names ...
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        return jsonRepositories.toString();
    }
}
