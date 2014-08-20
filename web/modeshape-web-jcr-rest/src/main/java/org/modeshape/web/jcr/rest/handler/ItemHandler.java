/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.web.jcr.rest.handler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.Binary;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionManager;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.Base64;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.web.jcr.rest.RestHelper;

/**
 * Resource handler that implements REST methods for items.
 */
@Immutable
public abstract class ItemHandler extends AbstractHandler {

    protected static final String CHILD_NODE_HOLDER = "children";

    private static final String PRIMARY_TYPE_PROPERTY = JcrConstants.JCR_PRIMARY_TYPE;
    private static final String MIXIN_TYPES_PROPERTY = JcrConstants.JCR_MIXIN_TYPES;
    private static final String PROPERTIES_HOLDER = "properties";

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

        if (properties.has(MIXIN_TYPES_PROPERTY)) {
            // Be sure to set this property first, before the other properties in case the other properties
            // are defined only on one of the mixin types ...
            updateMixins(newNode, properties.get(MIXIN_TYPES_PROPERTY));
        }

        for (Iterator<?> iter = properties.keys(); iter.hasNext();) {
            String key = (String)iter.next();

            if (PRIMARY_TYPE_PROPERTY.equals(key) || MIXIN_TYPES_PROPERTY.equals(key)) {
                continue;
            }
            setPropertyOnNode(newNode, key, properties.get(key));
        }

        if (hasChildren(jsonNode)) {
            List<JSONChild> children = getChildren(jsonNode);

            for (JSONChild child : children) {
                addNode(newNode, child.getName(), child.getBody());
            }
        }

        return newNode;
    }

    protected List<JSONChild> getChildren( JSONObject jsonNode ) throws JSONException {
        List<JSONChild> children;
        try {
            JSONObject childrenObject = jsonNode.getJSONObject(CHILD_NODE_HOLDER);
            children = new ArrayList<>(childrenObject.length());
            for (Iterator<?> iterator = childrenObject.keys(); iterator.hasNext();) {
                String childName = iterator.next().toString();
                //it is not possible to have SNS in the object form, so the index will always be 1
                children.add(new JSONChild(childName, childrenObject.getJSONObject(childName), 1));
            }
            return children;
        } catch (JSONException e) {
            JSONArray childrenArray = jsonNode.getJSONArray(CHILD_NODE_HOLDER);
            children = new ArrayList<>(childrenArray.length());
            Map<String, Integer> visitedNames = new HashMap<>(childrenArray.length());

            for (int i = 0; i < childrenArray.length(); i++) {
                JSONObject child = childrenArray.getJSONObject(i);
                if (child.length() == 0) {
                    continue;
                }
                if (child.length() > 1) {
                    logger.warn("The child object {0} has more than 1 elements, only the first one will be taken into account",
                                child);
                }
                String childName = child.keys().next().toString();
                int sns = visitedNames.containsKey(childName) ? visitedNames.get(childName) + 1 : 1;
                visitedNames.put(childName, sns);

                children.add(new JSONChild(childName, child.getJSONObject(childName), sns));
            }
            return children;
        }
    }

    protected boolean hasChildren( JSONObject jsonNode ) {
        return jsonNode.has(CHILD_NODE_HOLDER);
    }

    protected JSONObject getProperties( JSONObject jsonNode ) throws JSONException {
        return jsonNode.has(PROPERTIES_HOLDER) ? jsonNode.getJSONObject(PROPERTIES_HOLDER) : new JSONObject();
    }

    private Value createBinaryValue( String base64EncodedValue,
                                     ValueFactory valueFactory ) throws RepositoryException {
        InputStream stream = null;
        try {
            byte[] binaryValue = Base64.decode(base64EncodedValue);

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
                logger.debug(e, "Error while closing binary stream");
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

        Object values = convertToJcrValues(node, value, encoded);
        if (values == null) {
            // remove the property
            node.setProperty(propName, (Value) null);
        } else if (values instanceof Value) {
            node.setProperty(propName, (Value) values);
        } else {
            node.setProperty(propName, (Value[]) values);
        }
    }

    private Set<String> updateMixins( Node node,
                                      Object mixinsJsonValue ) throws JSONException, RepositoryException {
        Object valuesObject = convertToJcrValues(node, mixinsJsonValue, false);
        Value[] values = null;
        if (valuesObject == null) {
            values = new Value[0];
        } else if (valuesObject instanceof Value[]) {
            values = (Value[])valuesObject;
        } else {
            values = new Value[]{(Value)valuesObject};
        }

        Set<String> jsonMixins = new HashSet<String>(values.length);
        for (Value theValue : values) {
            jsonMixins.add(theValue.getString());
        }

        Set<String> mixinsToRemove = new HashSet<String>();
        for (NodeType nodeType : node.getMixinNodeTypes()) {
            mixinsToRemove.add(nodeType.getName());
        }

        Set<String> mixinsToAdd = new HashSet<String>(jsonMixins);
        mixinsToAdd.removeAll(mixinsToRemove);
        mixinsToRemove.removeAll(jsonMixins);

        for (String nodeType : mixinsToAdd) {
            node.addMixin(nodeType);
        }

        // return the list of mixins to be removed, as that needs to be processed last due to type validation
        return mixinsToRemove;
    }

    private Object convertToJcrValues( Node node,
                                        Object value,
                                        boolean encoded ) throws RepositoryException, JSONException {
        if (value == JSONObject.NULL || (value instanceof JSONArray && ((JSONArray)value).length() == 0)) {
            // for any null value of empty json array, return an empty array which will mean the property will be removed
            return null;
        }
        org.modeshape.jcr.api.ValueFactory valueFactory = (org.modeshape.jcr.api.ValueFactory)node.getSession().getValueFactory();
        if (value instanceof JSONArray) {
            JSONArray jsonValues = (JSONArray)value;
            Value[] values = new Value[jsonValues.length()];

            for (int i = 0; i < jsonValues.length(); i++) {
                if (encoded) {
                    values[i] = createBinaryValue(jsonValues.getString(i), valueFactory);
                } else {
                    values[i] = RestHelper.jsonValueToJCRValue(jsonValues.get(i), valueFactory);
                }
            }
            return values;
        }

        return encoded ? createBinaryValue(value.toString(), valueFactory) : RestHelper.jsonValueToJCRValue(value, valueFactory);
    }

    /**
     * Deletes the item at {@code path}.
     * 
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded repository name
     * @param rawWorkspaceName the URL-encoded workspace name
     * @param path the path to the item
     * @throws NotFoundException if no item exists at {@code path}
     * @throws NotAuthorizedException if the user does not have the access required to delete the item at this path
     * @throws RepositoryException if any other error occurs
     */
    public void deleteItem( HttpServletRequest request,
                            String rawRepositoryName,
                            String rawWorkspaceName,
                            String path ) throws NotFoundException, NotAuthorizedException, RepositoryException {

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
        }
        return updateProperty((Property)item, jsonItem);
    }

    private Property updateProperty( Property property,
                                     JSONObject jsonItem ) throws RepositoryException, JSONException {
        String propertyName = property.getName();
        String jsonPropertyName = jsonItem.has(propertyName) ? propertyName : propertyName + BASE64_ENCODING_SUFFIX;
        Node node = property.getParent();
        setPropertyOnNode(node, jsonPropertyName, jsonItem.get(jsonPropertyName));
        return property;
    }

    protected Node updateNode( Node node,
                               JSONObject jsonItem ) throws RepositoryException, JSONException {
        VersionableChanges changes = new VersionableChanges(node.getSession());
        try {
            node = updateNode(node, jsonItem, changes);
            changes.checkin();
        } catch (RepositoryException | JSONException | RuntimeException e) {
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

        // Change the primary type first ...
        if (properties.has(PRIMARY_TYPE_PROPERTY)) {
            String primaryType = properties.getString(PRIMARY_TYPE_PROPERTY);
            primaryType = primaryType.trim();
            if (primaryType.length() != 0 && !node.getPrimaryNodeType().getName().equals(primaryType)) {
                node.setPrimaryType(primaryType);
            }
        }

        Set<String> mixinsToRemove = new HashSet<String>();
        if (properties.has(MIXIN_TYPES_PROPERTY)) {
            // Next add new mixins, but don't remove old ones yet, because that needs to happen only after all the children
            // and properties have been processed
            mixinsToRemove = updateMixins(node, properties.get(MIXIN_TYPES_PROPERTY));
        }

        // Now set all the other properties ...
        for (Iterator<?> iter = properties.keys(); iter.hasNext();) {
            String key = (String)iter.next();
            if (PRIMARY_TYPE_PROPERTY.equals(key) || MIXIN_TYPES_PROPERTY.equals(key) || CHILD_NODE_HOLDER.equals(key)) {
                continue;
            }
            setPropertyOnNode(node, key, properties.get(key));
        }

        // If the JSON object has a children holder, then we need to update the list of children and child nodes ...
        if (hasChildren(jsonNode)) {
            updateChildren(node, jsonNode, changes);
        }

        // after all the children and properties have been processed, remove mixins because that will trigger validation
        for (String mixinToRemove : mixinsToRemove) {
            node.removeMixin(mixinToRemove);
        }

        return node;
    }

    private void updateChildren( Node node,
                                 JSONObject jsonNode,
                                 VersionableChanges changes ) throws JSONException, RepositoryException {
        Session session = node.getSession();

        // Get the existing children ...
        Map<String, Node> existingChildNames = new LinkedHashMap<>();
        List<String> existingChildrenToUpdate = new ArrayList<>();
        NodeIterator childIter = node.getNodes();
        while (childIter.hasNext()) {
            Node child = childIter.nextNode();
            String childName = nameOf(child);
            existingChildNames.put(childName, child);
            existingChildrenToUpdate.add(childName);
        }
        //keep track of the old/new order of children to be able to perform reorderings
        List<String> newChildrenToUpdate = new ArrayList<>();

        List<JSONChild> children = getChildren(jsonNode);
        for (JSONChild jsonChild : children) {
            String childName = jsonChild.getNameWithSNS();
            JSONObject child = jsonChild.getBody();
            // Find the existing node ...
            if (node.hasNode(childName)) {
                // The node exists, so get it and update it ...
                Node childNode = node.getNode(childName);
                String childNodeName = nameOf(childNode);
                newChildrenToUpdate.add(childNodeName);
                updateNode(childNode, child, changes);
                existingChildNames.remove(childNodeName);
            } else {
                //try to see if the child name is actually an identifier
                try {
                    Node childNode = session.getNodeByIdentifier(childName);
                    String childNodeName = nameOf(childNode);
                    if (childNode.getParent().getIdentifier().equals(node.getIdentifier())) {
                        //this is an existing child of the current node, referenced via an identifier
                        newChildrenToUpdate.add(childNodeName);
                        updateNode(childNode, child, changes);
                        existingChildNames.remove(childNodeName);
                    } else {
                        //this is a child belonging to another node
                        if (childNode.isNodeType("mix:shareable")) {
                            //if it's a shared node, we can't clone it because clone is not a session-scoped operation
                            logger.warn("The node {0} with the id {1} is a shared node belonging to another parent. It cannot be changed via the update operation",
                                        childNode.getPath(), childNode.getIdentifier());
                        } else {
                            //move the node into this parent
                            session.move(childNode.getPath(), node.getPath() + "/" + childNodeName);
                        }
                    }
                } catch (ItemNotFoundException e) {
                    //the child name is not a valid identifier, so treat it as a new child
                    addNode(node, childName, child);
                }
            }
        }

        // Remove the children in reverse order (starting with the last child to be removed) ...
        LinkedList<Node> childNodes = new LinkedList<Node>(existingChildNames.values());
        while (!childNodes.isEmpty()) {
            Node child = childNodes.removeLast();
            existingChildrenToUpdate.remove(child.getIdentifier());
            child.remove();
        }

        // Do any necessary reorderings
        if (newChildrenToUpdate.equals(existingChildrenToUpdate)) {
            //no order changes exist
            return;
        }

        for (int i = 0; i < newChildrenToUpdate.size() - 1; i++) {
            String startNodeName = newChildrenToUpdate.get(i);
            int startNodeOriginalPosition = existingChildrenToUpdate.indexOf(startNodeName);
            assert startNodeOriginalPosition != -1;

            for (int j = i + 1; j < newChildrenToUpdate.size(); j++) {
                String nodeName = newChildrenToUpdate.get(j);
                int nodeOriginalPosition = existingChildrenToUpdate.indexOf(nodeName);
                assert nodeOriginalPosition != -1;

                if (startNodeOriginalPosition > nodeOriginalPosition) {
                    //the start node should be moved *before* this node
                    node.orderBefore(startNodeName, nodeName);
                }
            }
        }
    }

    private String nameOf( Node node ) throws RepositoryException {
        int index = node.getIndex();
        String childName = node.getName();
        return index == 1 ? childName : childName + "[" + index + "]";
    }

    protected static class JSONChild {
        private final String name;
        private final JSONObject body;
        private final int snsIdx;

        protected JSONChild( String name, JSONObject body, int snsIdx ) {
            this.name = name;
            this.body = body;
            this.snsIdx = snsIdx;
        }

        public String getName() {
            return name;
        }

        public String getNameWithSNS() {
            return snsIdx > 1 ? name + "[" + snsIdx + "]" : name;
        }

        public JSONObject getBody() {
            return body;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("JSONChild{");
            sb.append("name='").append(getNameWithSNS()).append('\'');
            sb.append(", body=").append(body);
            sb.append('}');
            return sb.toString();
        }
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
