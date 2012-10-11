package org.modeshape.web.jcr.rest.handler;

import static org.modeshape.web.jcr.rest.RestHelper.BINARY_METHOD_NAME;
import static org.modeshape.web.jcr.rest.RestHelper.ITEMS_METHOD_NAME;
import java.util.ArrayList;
import java.util.List;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.http.HttpServletRequest;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.Logger;
import org.modeshape.web.jcr.RepositoryManager;
import org.modeshape.web.jcr.WebLogger;
import org.modeshape.web.jcr.rest.RestHelper;
import org.modeshape.web.jcr.rest.model.RestItem;
import org.modeshape.web.jcr.rest.model.RestNode;
import org.modeshape.web.jcr.rest.model.RestProperty;

/**
 * Base class for the different rest handler implementations, to which the rest services delegate operations.
 */
public abstract class AbstractHandler {

    protected static final String BASE64_ENCODING_SUFFIX = "/base64/";

    /**
     * Name to be used when the repository name is empty string as {@code "//"} is not a valid path.
     */
    public static final String EMPTY_REPOSITORY_NAME = "<default>";

    /**
     * The name of the custom property which will contain the node id
     */
    public static final String NODE_ID_CUSTOM_PROPERTY = RestNode.ID_FIELD_NAME;

    /**
     * Name to be used when the workspace name is empty string as {@code "//"} is not a valid path.
     */
    protected static final String EMPTY_WORKSPACE_NAME = "<default>";

    protected final Logger logger = WebLogger.getLogger(getClass());

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
        return RepositoryManager.getSession(request, repositoryNameFor(rawRepositoryName), workspaceNameFor(rawWorkspaceName));
    }

    private String workspaceNameFor( String rawWorkspaceName ) {
        String workspaceName = RestHelper.URL_ENCODER.decode(rawWorkspaceName);

        if (EMPTY_WORKSPACE_NAME.equals(workspaceName)) {
            workspaceName = "";
        }

        return workspaceName;
    }

    private String repositoryNameFor( String rawRepositoryName ) {
        String repositoryName = RestHelper.URL_ENCODER.decode(rawRepositoryName);

        if (EMPTY_REPOSITORY_NAME.equals(repositoryName)) {
            repositoryName = "";
        }

        return repositoryName;
    }

    protected List<String> restPropertyValues( Property property,
                                               String baseUrl,
                                               Session session ) throws RepositoryException {
        List<String> result = new ArrayList<String>();

        if (property.isMultiple()) {
            Value[] values = property.getValues();
            if (values == null || values.length == 0) {
                return null;
            }
            if (values.length == 1) {
                String value = valueToString(property.getPath(), values[0], baseUrl, session);
                if (value != null) {
                    result.add(value);
                }
            } else {
                for (Value value : values) {
                    if (value == null) {
                        continue;
                    }
                    String valueString = valueToString(property.getPath(), value, baseUrl, session);
                    if (valueString != null) {
                        result.add(valueString);
                    }
                }
            }
        } else {
            result.add(valueToString(property.getPath(), property.getValue(), baseUrl, session));
        }
        return result;
    }

    protected String valueToString( String absPropertyPath,
                                    Value value,
                                    String baseUrl,
                                    Session session ) {
        if (value == null) {
            return null;
        }
        try {
            switch (value.getType()) {
                case PropertyType.BINARY: {
                    assert baseUrl != null;
                    return restValueForBinary(absPropertyPath, baseUrl);
                }
                case PropertyType.REFERENCE:
                case PropertyType.WEAKREFERENCE: {
                    assert session != null;
                    return restValueForReference(value, baseUrl, session);
                }
                default: {
                    return value.getString();
                }
            }
        } catch (Exception e) {
            logger.error("Cannot create JSON string from value ", e);
            return null;
        }
    }

    private String restValueForReference( Value value,
                                          String baseUrl,
                                          Session session ) throws RepositoryException {
        String nodeId = value.getString();
        Node referredNode = session.getNodeByIdentifier(nodeId);
        if (referredNode != null) {
            return RestHelper.urlFrom(baseUrl, ITEMS_METHOD_NAME, referredNode.getPath());
        }
        logger.warn("Cannot resolve reference with id: {0}", nodeId);
        return nodeId;
    }

    private String restValueForBinary( String absPropertyPath,
                                       String baseUrl ) {
        if (absPropertyPath == null) {
            logger.warn("Cannot generate rest representation of a binary value, because the property is unknown");
            return null;
        }
        return RestHelper.urlFrom(baseUrl, BINARY_METHOD_NAME, absPropertyPath);
    }

    protected Node getParentNode( Property property ) throws RepositoryException {
        Node parentNode = property.getParent();
        if (JcrConstants.JCR_CONTENT.equalsIgnoreCase(parentNode.getName()) && parentNode.getIndex() == 1) {
            parentNode = parentNode.getParent();
        }
        return parentNode;
    }

    protected Item itemAtPath( String path,
                               Session session ) throws RepositoryException {
        return isRootPath(path) ? session.getRootNode() : session.getItem(path);
    }

    protected Node nodeWithId( String id,
                               Session session ) throws RepositoryException {
        return session.getNodeByIdentifier(id);
    }

    protected boolean isRootPath( String path ) {
        return "/".equals(path) || "".equals(path);
    }

    protected RestItem createRestItem( HttpServletRequest request,
                                       int depth,
                                       Session session,
                                       Item item ) throws RepositoryException {
        String baseUrl = RestHelper.repositoryUrl(request);
        return item instanceof Node ? createRestNode(session, (Node)item, baseUrl, depth) : createRestProperty(session,
                                                                                                               (Property)item,
                                                                                                               baseUrl);
    }

    protected String parentPath( String path ) {
        int lastSlashInd = path.lastIndexOf('/');
        if (lastSlashInd == -1) {
            return "/";
        }
        String subPath = path.substring(0, lastSlashInd);
        return absPath(subPath);
    }

    protected String absPath( String pathString ) {
        return pathString.startsWith("/") ? pathString : "/" + pathString;
    }

    protected String nodeName( Node node ) throws RepositoryException {
        int index = node.getIndex();
        String name = node.getName();
        if (index != 1) name = name + "[" + index + "]";
        return name;
    }

    private RestNode createRestNode( Session session,
                                     Node node,
                                     String baseUrl,
                                     int depth ) throws RepositoryException {
        String nodeUrl = RestHelper.urlFrom(baseUrl, ITEMS_METHOD_NAME, node.getPath());
        boolean isRoot = node.getPath().equals("/");
        String parentUrl = isRoot ? RestHelper.urlFrom(baseUrl, ITEMS_METHOD_NAME, "..", "..") : RestHelper.urlFrom(baseUrl,
                                                                                                                    ITEMS_METHOD_NAME,
                                                                                                                    node.getParent()
                                                                                                                        .getPath());
        RestNode restNode = new RestNode(node.getName(), node.getIdentifier(), nodeUrl, parentUrl);

        // add the properties
        for (PropertyIterator propertyIterator = node.getProperties(); propertyIterator.hasNext();) {
            Property property = propertyIterator.nextProperty();
            restNode.addJcrProperty(createRestProperty(session, property, baseUrl));
        }

        // add the children
        for (NodeIterator nodeIterator = node.getNodes(); nodeIterator.hasNext();) {
            Node childNode = nodeIterator.nextNode();
            RestNode restChild = null;
            if (depth != 0) {
                restChild = createRestNode(session, childNode, baseUrl, depth - 1);
            } else {
                String childUrl = RestHelper.urlFrom(baseUrl, ITEMS_METHOD_NAME, childNode.getPath());
                restChild = new RestNode(nodeName(childNode), childNode.getIdentifier(), childUrl, nodeUrl);
            }
            restNode.addChild(restChild);
        }
        return restNode;
    }

    private RestProperty createRestProperty( Session session,
                                             Property property,
                                             String baseUrl ) throws RepositoryException {
        List<String> values = restPropertyValues(property, baseUrl, session);
        String url = RestHelper.urlFrom(baseUrl, ITEMS_METHOD_NAME, property.getPath());
        String parentUrl = RestHelper.urlFrom(baseUrl, ITEMS_METHOD_NAME, property.getParent().getPath());
        return new RestProperty(property.getName(), url, parentUrl, values);
    }
}
