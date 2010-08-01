package org.modeshape.web.jcr.rest;

import java.io.IOException;
import java.io.InputStream;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.http.HttpServletRequest;
import org.modeshape.common.text.UrlEncoder;
import org.modeshape.common.util.Base64;
import org.modeshape.web.jcr.RepositoryFactory;

abstract class AbstractHandler {

    protected static final String BASE64_ENCODING_SUFFIX = "/base64/";

    protected static final UrlEncoder URL_ENCODER = new UrlEncoder();

    /** Name to be used when the repository name is empty string as {@code "//"} is not a valid path. */
    public static final String EMPTY_REPOSITORY_NAME = "<default>";
    /** Name to be used when the workspace name is empty string as {@code "//"} is not a valid path. */
    public static final String EMPTY_WORKSPACE_NAME = "<default>";


    /**
     * Returns an active session for the given workspace name in the named repository.
     * 
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded name of the repository in which the session is created
     * @param rawWorkspaceName the URL-encoded name of the workspace to which the session should be connected
     * @return an active session with the given workspace in the named repository
     * @throws RepositoryException if any other error occurs
     */
    protected Session getSession( HttpServletRequest request,
                                String rawRepositoryName,
                                String rawWorkspaceName ) throws RepositoryException {
        assert request != null;

        return RepositoryFactory.getSession(request, repositoryNameFor(rawRepositoryName), workspaceNameFor(rawWorkspaceName));
    }

    private String workspaceNameFor( String rawWorkspaceName ) {
        String workspaceName = URL_ENCODER.decode(rawWorkspaceName);

        if (EMPTY_WORKSPACE_NAME.equals(workspaceName)) {
            workspaceName = "";
        }

        return workspaceName;
    }

    private String repositoryNameFor( String rawRepositoryName ) {
        String repositoryName = URL_ENCODER.decode(rawRepositoryName);

        if (EMPTY_REPOSITORY_NAME.equals(repositoryName)) {
            repositoryName = "";
        }

        return repositoryName;
    }

    /**
     * Return the JSON-compatible string representation of the given property value. If the value is a {@link PropertyType#BINARY
     * binary} value, then this method returns the Base-64 encoding of that value. Otherwise, it just returns the string
     * representation of the value.
     * 
     * @param value the property value; may not be null
     * @return the string representation of the value
     * @throws RepositoryException if there is a problem accessing the value
     */
    protected String jsonEncodedStringFor( Value value ) throws RepositoryException {
        // Encode the binary value in Base64 ...
        InputStream stream = value.getBinary().getStream();
        try {
            return Base64.encode(stream);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // Error accessing the value, so throw this ...
                    throw new RepositoryException(e);
                }
            }
        }
    }
    
}
