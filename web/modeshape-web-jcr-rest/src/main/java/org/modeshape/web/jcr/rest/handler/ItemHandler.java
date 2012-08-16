package org.modeshape.web.jcr.rest.handler;

import javax.jcr.Binary;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionManager;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.UnauthorizedException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.Base64;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.web.jcr.rest.RestHelper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resource handler that implements REST methods for items.
 *
 * @deprecated since 3.0, use {@link RestItemHandler}
 */
@Immutable
public class ItemHandler extends AbstractHandler {

    protected static final String PRIMARY_TYPE_PROPERTY = JcrConstants.JCR_PRIMARY_TYPE;
    protected static final String MIXIN_TYPES_PROPERTY = JcrConstants.JCR_MIXIN_TYPES;
    protected static final String PROPERTIES_HOLDER = "properties";
    protected static final String CHILD_NODE_HOLDER = "children";


    /**
     * Handles GET requests for an item in a workspace.
     *
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded repository name
     * @param rawWorkspaceName the URL-encoded workspace name
     * @param path the path to the item
     * @param depth the depth of the node graph that should be returned if {@code path} refers to a node. @{code 0} means return
     * the requested node only. A negative value indicates that the full subgraph under the node should be returned. This
     * parameter defaults to {@code 0} and is ignored if {@code path} refers to a property.
     * @return the JSON-encoded version of the item (and, if the item is a node, its subgraph, depending on the value of
     *         {@code depth})
     * @throws NotFoundException if the named repository does not exists, the named workspace does not exist, or the user does not
     * have access to the named workspace
     * @throws JSONException if there is an error encoding the node
     * @throws UnauthorizedException if the given login information is invalid
     * @throws RepositoryException if any other error occurs
     * @see #EMPTY_REPOSITORY_NAME
     * @see #EMPTY_WORKSPACE_NAME
     * @see Session#getItem(String)
     * @deprecated since 3.0
     */
    public String getItem( HttpServletRequest request,
                           String rawRepositoryName,
                           String rawWorkspaceName,
                           String path,
                           int depth ) throws JSONException, UnauthorizedException, RepositoryException {
        assert path != null;
        assert rawRepositoryName != null;
        assert rawWorkspaceName != null;

        Session session = getSession(request, rawRepositoryName, rawWorkspaceName);
        Item item;

        if ("/".equals(path) || "".equals(path)) {
            item = session.getRootNode();
        } else {
            try {
                item = session.getItem(path);
            } catch (PathNotFoundException pnfe) {
                throw new NotFoundException(pnfe.getMessage(), pnfe);
            }
        }

        JSONObject jsonObject = item instanceof Node ? jsonFor((Node)item, depth) : jsonFor((Property)item);
        return RestHelper.responseString(jsonObject, request);
    }

    /**
     * Returns the JSON-encoded version of the given property. If the property is single-valued, the returned string is the value
     * of the property encoded as a JSON string, including the name. If the property is multi-valued with {@code N} values, this
     * method returns a JSON array containing the JSON string for each value.
     * <p>
     * Note that if any of the values are binary, then <i>all</i> values will be first encoded as {@link Base64} string values.
     * However, if no values are binary, then all values will simply be the {@link Value#getString() string} representation of the
     * value.
     * </p>
     *
     * @param property the property to be encoded
     * @return the JSON-encoded version of the property
     * @throws JSONException if there is an error encoding the node
     * @throws RepositoryException if an error occurs accessing the property, its values, or its definition.
     * @see Property#getDefinition()
     * @see PropertyDefinition#isMultiple()
     */
    private JSONObject jsonFor( Property property ) throws JSONException, RepositoryException {
        boolean encoded = false;
        Object valueObject = null;
        if (property.getDefinition().isMultiple()) {
            Value[] values = property.getValues();
            for (Value value : values) {
                if (value.getType() == PropertyType.BINARY) {
                    encoded = true;
                    break;
                }
            }
            List<String> list = new ArrayList<String>(values.length);
            if (encoded) {
                for (Value value : values) {
                    list.add(RestHelper.jsonEncodedStringFor(value));
                }
            } else {
                for (Value value : values) {
                    list.add(value.getString());
                }
            }
            valueObject = new JSONArray(list);
        } else {
            Value value = property.getValue();
            encoded = value.getType() == PropertyType.BINARY;
            valueObject = encoded ? RestHelper.jsonEncodedStringFor(value) : value.getString();
        }
        String propertyName = property.getName();
        if (encoded) {
            propertyName = propertyName + BASE64_ENCODING_SUFFIX;
        }
        JSONObject jsonProperty = new JSONObject();
        jsonProperty.put(propertyName, valueObject);
        return jsonProperty;
    }

    /**
     * Recursively returns the JSON-encoding of a node and its children to depth {@code toDepth}.
     *
     * @param node the node to be encoded
     * @param toDepth the depth to which the recursion should extend; {@code 0} means no further recursion should occur.
     * @return the JSON-encoding of a node and its children to depth {@code toDepth}.
     * @throws JSONException if there is an error encoding the node
     * @throws RepositoryException if any other error occurs
     */
    private JSONObject jsonFor( Node node,
                                int toDepth ) throws JSONException, RepositoryException {
        JSONObject jsonNode = new JSONObject();

        JSONObject properties = new JSONObject();

        for (PropertyIterator iter = node.getProperties(); iter.hasNext(); ) {
            Property prop = iter.nextProperty();
            String propName = prop.getName();

            boolean encoded = false;

            if (prop.getDefinition().isMultiple()) {
                Value[] values = prop.getValues();
                // Do any of the property values need to be encoded ?
                for (Value value : values) {
                    if (value.getType() == PropertyType.BINARY) {
                        encoded = true;
                        break;
                    }
                }
                if (encoded) {
                    propName = propName + BASE64_ENCODING_SUFFIX;
                }
                JSONArray array = new JSONArray();
                for (int i = 0; i < values.length; i++) {
                    array.put(encoded ? RestHelper.jsonEncodedStringFor(values[i]) : values[i].getString());
                }
                properties.put(propName, array);

            } else {
                Value value = prop.getValue();
                encoded = value.getType() == PropertyType.BINARY;
                if (encoded) {
                    propName = propName + BASE64_ENCODING_SUFFIX;
                }
                properties.put(propName, encoded ? RestHelper.jsonEncodedStringFor(value) : value.getString());
            }

        }
        if (properties.length() > 0) {
            jsonNode.put(PROPERTIES_HOLDER, properties);
        }

        if (toDepth == 0) {
            List<String> children = new ArrayList<String>();

            for (NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
                Node child = iter.nextNode();

                String name = child.getIndex() == 1 ? child.getName() : child.getName() + "[" + child.getIndex() + "]";
                children.add(name);
            }

            if (children.size() > 0) {
                jsonNode.put(CHILD_NODE_HOLDER, new JSONArray(children));
            }
        } else {
            JSONObject children = new JSONObject();

            for (NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
                Node child = iter.nextNode();

                String name = child.getIndex() == 1 ? child.getName() : child.getName() + "[" + child.getIndex() + "]";
                children.put(name, jsonFor(child, toDepth - 1));
            }

            if (children.length() > 0) {
                jsonNode.put(CHILD_NODE_HOLDER, children);
            }
        }

        return jsonNode;
    }

    /**
     * Adds the content of the request as a node (or subtree of nodes) at the location specified by {@code path}.
     * <p>
     * The primary type and mixin type(s) may optionally be specified through the {@code jcr:primaryType} and
     * {@code jcr:mixinTypes} properties.
     * </p>
     *
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded repository name
     * @param rawWorkspaceName the URL-encoded workspace name
     * @param path the path to the item
     * @param fullNodeInResponse if true, indicates that a representation of the created node (including all properties and
     * children) should be returned; otherwise, only the path to the new node will be returned
     * @param requestContent the JSON-encoded representation of the node or nodes to be added
     * @return the JSON-encoded representation of the node or nodes that were added. This will differ from {@code requestContent}
     *         in that auto-created and protected properties (e.g., jcr:uuid) will be populated.
     * @throws NotFoundException if the parent of the item to be added does not exist
     * @throws UnauthorizedException if the user does not have the access required to create the node at this path
     * @throws JSONException if there is an error encoding the node
     * @throws RepositoryException if any other error occurs
     */
    public Response postItem( HttpServletRequest request,
                              String rawRepositoryName,
                              String rawWorkspaceName,
                              String path,
                              boolean fullNodeInResponse,
                              String requestContent )
            throws NotFoundException, UnauthorizedException, RepositoryException, JSONException {

        assert rawRepositoryName != null;
        assert rawWorkspaceName != null;
        assert path != null;
        JSONObject body = new JSONObject(requestContent);

        int lastSlashInd = path.lastIndexOf('/');
        String parentPath = lastSlashInd == -1 ? "/" : "/" + path.substring(0, lastSlashInd);
        String newNodeName = lastSlashInd == -1 ? path : path.substring(lastSlashInd + 1);

        Session session = getSession(request, rawRepositoryName, rawWorkspaceName);

        Node parentNode = (Node)session.getItem(parentPath);

        Node newNode = addNode(parentNode, newNodeName, body);

        session.save();

        if (fullNodeInResponse) {

            String json = jsonFor(newNode, -1).toString();
            return Response.status(Status.CREATED).entity(json).build();
        }

        return Response.status(Status.CREATED).entity(newNode.getPath()).build();

    }

    /**
     * Adds the node described by {@code jsonNode} with name {@code nodeName} to the existing node {@code parentNode}.
     *
     * @param parentNode the parent of the node to be added
     * @param nodeName the name of the node to be added
     * @param jsonNode the JSON-encoded representation of the node or nodes to be added.
     * @return the JSON-encoded representation of the node or nodes that were added. This will differ from {@code requestContent}
     *         in that auto-created and protected properties (e.g., jcr:uuid) will be populated.
     * @throws JSONException if there is an error encoding the node
     * @throws RepositoryException if any other error occurs
     */
    protected Node addNode( Node parentNode,
                            String nodeName,
                            JSONObject jsonNode ) throws RepositoryException, JSONException {
        Node newNode;

        JSONObject properties = getProperties(jsonNode);

        if (properties.has(PRIMARY_TYPE_PROPERTY)) {
            String primaryType = properties.getString(PRIMARY_TYPE_PROPERTY);
            newNode = parentNode.addNode(nodeName, primaryType);
        } else {
            newNode = parentNode.addNode(nodeName);
        }

        for (Iterator<?> iter = properties.keys(); iter.hasNext(); ) {
            String key = (String)iter.next();

            if (PRIMARY_TYPE_PROPERTY.equals(key)) {
                continue;
            }
            setPropertyOnNode(newNode, key, properties.get(key));
        }

        if (hasChildren(jsonNode)) {
            JSONObject children = getChildren(jsonNode);

            for (Iterator<?> iter = children.keys(); iter.hasNext(); ) {
                String childName = (String)iter.next();
                JSONObject child = children.getJSONObject(childName);

                addNode(newNode, childName, child);
            }
        }

        return newNode;
    }

    protected JSONObject getChildren( JSONObject jsonNode ) throws JSONException {
        return jsonNode.getJSONObject(CHILD_NODE_HOLDER);
    }

    protected boolean hasChildren( JSONObject jsonNode ) {
        return jsonNode.has(CHILD_NODE_HOLDER);
    }

    protected JSONObject getProperties( JSONObject jsonNode ) throws JSONException {
        return jsonNode.has(PROPERTIES_HOLDER) ? jsonNode.getJSONObject(PROPERTIES_HOLDER) : new JSONObject();
    }

    private Value decodeValue( String encodedValue,
                               ValueFactory valueFactory ) throws RepositoryException {
        InputStream stream = null;
        try {
            byte[] binaryValue = Base64.decode(encodedValue);

            stream = new ByteArrayInputStream(binaryValue);
            Binary binary = valueFactory.createBinary(stream);
            return valueFactory.createValue(binary);
        } catch (IOException ioe) {
            throw new RepositoryException(ioe);
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
                // Error accessing the value, so throw this ...
                throw new RepositoryException(e);
            }
        }
    }

    /**
     * Sets the named property on the given node. This method expects {@code value} to be either a JSON string or a JSON array of
     * JSON strings. If {@code value} is a JSON array, {@code Node#setProperty(String, String[]) the multi-valued property setter}
     * will be used.
     *
     * @param node the node on which the property is to be set
     * @param propName the name of the property to set
     * @param value the JSON-encoded values to be set
     * @throws RepositoryException if there is an error setting the property
     * @throws JSONException if {@code value} cannot be decoded
     */
    protected void setPropertyOnNode( Node node,
                                      String propName,
                                      Object value ) throws RepositoryException, JSONException {
        // Are the property values encoded ?
        boolean encoded = propName.endsWith(BASE64_ENCODING_SUFFIX);
        if (encoded) {
            int newLength = propName.length() - BASE64_ENCODING_SUFFIX.length();
            propName = newLength > 0 ? propName.substring(0, newLength) : "";
        }

        Value[] values;
        ValueFactory valueFactory = node.getSession().getValueFactory();
        if (value instanceof JSONArray) {
            JSONArray jsonValues = (JSONArray)value;
            values = new Value[jsonValues.length()];

            for (int i = 0; i < values.length; i++) {
                String strValue = jsonValues.getString(i);
                if (encoded) {
                    values[i] = decodeValue(strValue, valueFactory);
                } else {
                    values[i] = valueFactory.createValue(strValue);
                }
            }
        } else {
            String strValue = (String)value;
            if (encoded) {
                values = new Value[] { decodeValue(strValue, valueFactory) };
            } else {
                values = new Value[] { valueFactory.createValue(strValue) };
            }
        }

        if (propName.equals(ItemHandler.MIXIN_TYPES_PROPERTY)) {
            Set<String> toBeMixins = new HashSet<String>();
            for (Value theValue : values) {
                toBeMixins.add(theValue.getString());
            }
            Set<String> asIsMixins = new HashSet<String>();

            for (NodeType nodeType : node.getMixinNodeTypes()) {
                asIsMixins.add(nodeType.getName());
            }

            Set<String> mixinsToAdd = new HashSet<String>(toBeMixins);
            mixinsToAdd.removeAll(asIsMixins);
            asIsMixins.removeAll(toBeMixins);

            for (String nodeType : mixinsToAdd) {
                node.addMixin(nodeType);
            }

            for (String nodeType : asIsMixins) {
                node.removeMixin(nodeType);
            }
        } else {
            if (values.length == 1) {
                node.setProperty(propName, values[0]);

            } else {
                node.setProperty(propName, values);
            }
        }
    }

    /**
     * Deletes the item at {@code path}.
     *
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded repository name
     * @param rawWorkspaceName the URL-encoded workspace name
     * @param path the path to the item
     * @throws NotFoundException if no item exists at {@code path}
     * @throws UnauthorizedException if the user does not have the access required to delete the item at this path
     * @throws RepositoryException if any other error occurs
     */
    public void deleteItem( HttpServletRequest request,
                            String rawRepositoryName,
                            String rawWorkspaceName,
                            String path ) throws NotFoundException, UnauthorizedException, RepositoryException {

        assert rawRepositoryName != null;
        assert rawWorkspaceName != null;
        assert path != null;

        Session session = getSession(request, rawRepositoryName, rawWorkspaceName);

        doDelete(path, session);
        session.save();
    }

    protected void doDelete( String path,
                             Session session ) throws RepositoryException {
        Item item;
        try {
            item = session.getItem(path);
        } catch (PathNotFoundException pnfe) {
            throw new NotFoundException(pnfe.getMessage(), pnfe);
        }
        item.remove();
    }

    /**
     * Updates the properties at the path.
     * <p>
     * If path points to a property, this method expects the request content to be either a JSON array or a JSON string. The array
     * or string will become the values or value of the property. If path points to a node, this method expects the request
     * content to be a JSON object. The keys of the objects correspond to property names that will be set and the values for the
     * keys correspond to the values that will be set on the properties.
     * </p>
     *
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded repository name
     * @param rawWorkspaceName the URL-encoded workspace name
     * @param path the path to the item
     * @param requestContent the JSON-encoded representation of the values and, possibly, properties to be set
     * @return the JSON-encoded representation of the node on which the property or properties were set.
     * @throws NotFoundException if the parent of the item to be added does not exist
     * @throws UnauthorizedException if the user does not have the access required to create the node at this path
     * @throws JSONException if there is an error encoding the node
     * @throws RepositoryException if any other error occurs
     * @throws IOException if there is a problem reading the value
     */
    public String putItem( HttpServletRequest request,
                           String rawRepositoryName,
                           String rawWorkspaceName,
                           String path,
                           String requestContent ) throws UnauthorizedException, JSONException, RepositoryException, IOException {

        assert path != null;
        assert rawRepositoryName != null;
        assert rawWorkspaceName != null;

        Session session = getSession(request, rawRepositoryName, rawWorkspaceName);
        Item item = null;
        if ("".equals(path) || "/".equals(path)) {
            item = session.getRootNode();
        } else {
            try {
                item = session.getItem(path);
            } catch (PathNotFoundException pnfe) {
                throw new NotFoundException(pnfe.getMessage(), pnfe);
            }
        }

        Item updatedItem = updateItem(item, new JSONObject(requestContent));
        Node node = updatedItem instanceof Property ? updatedItem.getParent() : (Node)updatedItem;
        node.getSession().save();
        return jsonFor(node, 0).toString();
    }

    /**
     * Updates the existing item based upon the supplied JSON content.
     *
     * @param item the node or property to be updated
     * @param jsonItem the JSON of the item(s) to be updated
     * @return the node that was updated; never null
     * @throws JSONException if there is an error encoding the node
     * @throws RepositoryException if any other error occurs
     */
    protected Item updateItem( Item item,
                               JSONObject jsonItem ) throws RepositoryException, JSONException {
        if (item instanceof Node) {
            return updateNode((Node)item, jsonItem);
        } else {
            return updateProperty((Property)item, jsonItem);
        }
    }

    private Property updateProperty( Property property,
                                     JSONObject jsonItem ) throws RepositoryException, JSONException {
        String propertyName = property.getName();
        String jsonPropertyName = jsonItem.has(propertyName) ? propertyName : propertyName + BASE64_ENCODING_SUFFIX;
        Node node = property.getParent();
        setPropertyOnNode(node, jsonPropertyName, jsonItem.get(jsonPropertyName));
        return property;
    }

    private Node updateNode( Node node,
                             JSONObject jsonItem ) throws RepositoryException, JSONException {
        VersionableChanges changes = new VersionableChanges(node.getSession());
        try {
            node = updateNode(node, jsonItem, changes);
            changes.checkin();
        } catch (RepositoryException e) {
            changes.abort();
            throw e;
        } catch (JSONException e) {
            changes.abort();
            throw e;
        } catch (RuntimeException e) {
            changes.abort();
            throw e;
        }
        return node;
    }

    /**
     * Updates the existing node with the properties (and optionally children) as described by {@code jsonNode}.
     *
     * @param node the node to be updated
     * @param jsonNode the JSON-encoded representation of the node or nodes to be updated.
     * @param changes the versionable changes; may not be null
     * @return the Node that was updated; never null
     * @throws JSONException if there is an error encoding the node
     * @throws RepositoryException if any other error occurs
     */
    protected Node updateNode( Node node,
                               JSONObject jsonNode,
                               VersionableChanges changes ) throws RepositoryException, JSONException {
        // If the JSON object has a properties holder, then this is likely a subgraph ...
        JSONObject properties = jsonNode;
        if (jsonNode.has(PROPERTIES_HOLDER)) {
            properties = jsonNode.getJSONObject(PROPERTIES_HOLDER);
        }

        changes.checkout(node);

        for (Iterator<?> iter = properties.keys(); iter.hasNext(); ) {
            String key = (String)iter.next();
            if (PRIMARY_TYPE_PROPERTY.equals(key) || CHILD_NODE_HOLDER.equals(key)) {
                continue; // can't change the primary type
            }
            setPropertyOnNode(node, key, properties.get(key));
        }

        // If the JSON object has a children holder, then we need to update the list of children and child nodes ...
        if (hasChildren(jsonNode)) {
            Node parent = node;
            JSONObject children = getChildren(jsonNode);

            // Get the existing children ...
            Map<String, Node> existingChildNames = new LinkedHashMap<String, Node>();
            NodeIterator childIter = parent.getNodes();
            while (childIter.hasNext()) {
                Node child = childIter.nextNode();
                existingChildNames.put(nameOf(child), child);
            }

            for (Iterator<?> iter = children.keys(); iter.hasNext(); ) {
                String childName = (String)iter.next();
                JSONObject child = children.getJSONObject(childName);
                // Find the existing node ...
                if (parent.hasNode(childName)) {
                    // The node exists, so get it and update it ...
                    Node childNode = parent.getNode(childName);
                    updateNode(childNode, child, changes);
                    existingChildNames.remove(nameOf(childNode));
                } else {
                    // Have to add the new child ...
                    addNode(parent, childName, child);
                }
            }

            // Remove the children in reverse order (starting with the last child to be removed) ...
            LinkedList<Node> childNodes = new LinkedList<Node>(existingChildNames.values());
            Collections.reverse(childNodes);
            while (!childNodes.isEmpty()) {
                Node child = childNodes.removeLast();
                child.remove();
            }
        }

        return node;
    }

    private String nameOf( Node node ) throws RepositoryException {
        int index = node.getIndex();
        String childName = node.getName();
        return index == 1 ? childName : childName + "[" + index + "]";
    }

    protected static class VersionableChanges {
        private final Set<String> changedVersionableNodes = new HashSet<String>();
        private final Session session;
        private final VersionManager versionManager;

        protected VersionableChanges( Session session ) throws RepositoryException {
            this.session = session;
            assert this.session != null;
            this.versionManager = session.getWorkspace().getVersionManager();
        }

        public void checkout( Node node ) throws RepositoryException {
            boolean versionable = node.isNodeType("mix:versionable");
            if (versionable) {
                String path = node.getPath();
                versionManager.checkout(path);
                this.changedVersionableNodes.add(path);
            }
        }

        public void checkin() throws RepositoryException {
            if (this.changedVersionableNodes.isEmpty()) {
                return;
            }
            session.save();
            RepositoryException first = null;
            for (String path : this.changedVersionableNodes) {
                try {
                    if (versionManager.isCheckedOut(path)) {
                        versionManager.checkin(path);
                    }
                } catch (RepositoryException e) {
                    if (first == null) {
                        first = e;
                    }
                }
            }
            if (first != null) {
                throw first;
            }
        }

        public void abort() throws RepositoryException {
            if (this.changedVersionableNodes.isEmpty()) {
                return;
            }
            // Throw out all the changes ...
            session.refresh(false);
            RepositoryException first = null;
            for (String path : this.changedVersionableNodes) {
                try {
                    if (versionManager.isCheckedOut(path)) {
                        versionManager.checkin(path);
                    }
                } catch (RepositoryException e) {
                    if (first == null) {
                        first = e;
                    }
                }
            }
            if (first != null) {
                throw first;
            }
        }
    }

}
