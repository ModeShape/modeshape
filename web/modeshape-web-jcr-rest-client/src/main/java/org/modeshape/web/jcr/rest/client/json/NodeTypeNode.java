/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.web.jcr.rest.client.json;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.PropertyType;
import javax.jcr.version.OnParentVersionAction;
import net.jcip.annotations.Immutable;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.common.util.CheckArg;
import org.modeshape.web.jcr.rest.client.domain.ChildNodeDefinition;
import org.modeshape.web.jcr.rest.client.domain.NodeType;
import org.modeshape.web.jcr.rest.client.domain.PropertyDefinition;
import org.modeshape.web.jcr.rest.client.domain.Workspace;

/**
 * The <code>NodeTypeNode</code> class is responsible for knowing how to obtain a NodeType based on the Workspace. <br>
 * An example <code>URL</code> to obtain all the node types would look like: <br>
 * <i>{context root}/{repository name}/{workspace name}/items/jcr:system/jcr:nodeTypes"</i> <br>
 * And an example url to obtain a specific node type would look like: <br>
 * <i>{context root}/{repository name}/{workspace name}/items/jcr:system/jcr:nodeTypes/{node type name}</i> <br>
 * A Node Type will not be created if: <li>jcr:isMixin is true</li> <li>jcr:multiple is true</li>
 */
@Immutable
public final class NodeTypeNode extends JsonNode {

    protected static final String NODE_TYPES_PATH = "jcr:system/jcr:nodeTypes";
    protected static final int NODE_TYPE_DEPTH = 5;
    private static final Map<String, Integer> PROPERTY_TYPES_BY_LOWERCASE_NAME;

    static {
        Map<String, Integer> types = new HashMap<String, Integer>();
        registerType(types, PropertyType.BINARY);
        registerType(types, PropertyType.BOOLEAN);
        registerType(types, PropertyType.DATE);
        registerType(types, PropertyType.DECIMAL);
        registerType(types, PropertyType.DOUBLE);
        registerType(types, PropertyType.LONG);
        registerType(types, PropertyType.NAME);
        registerType(types, PropertyType.PATH);
        registerType(types, PropertyType.REFERENCE);
        registerType(types, PropertyType.STRING);
        registerType(types, PropertyType.UNDEFINED);
        registerType(types, PropertyType.URI);
        registerType(types, PropertyType.WEAKREFERENCE);
        PROPERTY_TYPES_BY_LOWERCASE_NAME = Collections.unmodifiableMap(types);
    }

    private static void registerType( Map<String, Integer> typesByLowerCaseName,
                                      int propertyType ) {
        String name = PropertyType.nameFromValue(propertyType);
        typesByLowerCaseName.put(name.toLowerCase(), propertyType);
    }

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * EXCLUDE_TYEPS are those node types that are to be excluded from the inclusion in the node types returned.
     */
    private static Set<String> EXCLUDE_TYPES = Collections.singleton("mode:defined");
    /**
     * The workspace from where the node type is being obtained.
     */
    private final Workspace workspace;

    private Map<String, javax.jcr.nodetype.NodeType> nodeTypeMap = new HashMap<String, javax.jcr.nodetype.NodeType>();

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * Use this constructor if wanting all node types for a workspace
     * 
     * @param workspace the workspace being used (never <code>null</code>)
     * @throws Exception if there is a problem creating the folder node
     */
    public NodeTypeNode( Workspace workspace ) throws Exception {
        super(NODE_TYPES_PATH);

        assert workspace != null;
        this.workspace = workspace;
    }

    /**
     * Use this constructor if wanting all node types for a workspace
     * 
     * @param workspace the workspace being used (never <code>null</code>)
     * @param nodeTypeName the node type name; may not be null
     * @throws Exception if there is a problem creating the folder node
     */
    public NodeTypeNode( Workspace workspace,
                         String nodeTypeName ) throws Exception {
        super(NODE_TYPES_PATH + "/" + nodeTypeName);

        assert workspace != null;
        this.workspace = workspace;
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * @return the full path of folder within the workspace
     */
    public String getPath() {
        return getId();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The URL will NOT end in '/'.
     * 
     * @see org.modeshape.web.jcr.rest.client.json.JsonNode#getUrl()
     */
    @Override
    public URL getUrl() throws Exception {
        WorkspaceNode workspaceNode = new WorkspaceNode(this.workspace);
        StringBuilder url = new StringBuilder(workspaceNode.getUrl().toString());

        // make sure path starts with a '/'
        String path = getPath();
        if (!path.startsWith("/")) path = '/' + path;
        path = path.replaceAll("[/]+$", ""); // path should not end in with '/'

        // path needs to be encoded
        url.append(JsonUtils.encode(path));
        url.append("?depth=").append(NODE_TYPE_DEPTH);

        return new URL(url.toString());
    }

    /**
     * @param jsonResponse the HTTP connection JSON response (never <code>null</code>) containing the NodeTypes
     * @return the node types for this workspace (never <code>null</code>)
     * @throws Exception if there is a problem obtaining the node types
     */
    public Map<String, javax.jcr.nodetype.NodeType> getNodeTypes( String jsonResponse ) throws Exception {
        CheckArg.isNotNull(jsonResponse, "jsonResponse");

        Map<String, NodeType> nodeTypesByName = new HashMap<String, NodeType>();
        JSONObject body = new JSONObject(jsonResponse);

        // The response is the node representation of '/jcr:system/jcr:nodeTypes'
        // We don't care about this node at all, but we do care about it's children (the node types) ...
        if (body.has("children")) {
            Object obj = body.get("children");

            // The children are the node types, and they should always be full (with property and child node defns),
            // so that means 'children' will always contain is a JSONObject ...
            JSONObject children = (JSONObject)obj;
            for (Iterator<?> itr = children.keys(); itr.hasNext();) {
                String key = JsonUtils.decode(itr.next().toString());
                Object child = children.get(key);
                if (child != null) {
                    // and that means that each child is a JSONObject ...
                    createNodeType(key, (JSONObject)child, nodeTypesByName);
                }
            }
        }

        // Convert to a map of JCR node types ...
        Map<String, javax.jcr.nodetype.NodeType> result = new HashMap<String, javax.jcr.nodetype.NodeType>();
        for (NodeType nodeType : nodeTypesByName.values()) {
            result.put(nodeType.getName(), nodeType);
        }
        nodeTypeMap = result;
        return nodeTypeMap;
    }

    protected void createNodeType( String name,
                                   JSONObject body,
                                   Map<String, NodeType> nodeTypes ) throws Exception {
        JSONObject props = (JSONObject)body.get("properties");

        // [nt:nodeType]
        // - jcr:nodeTypeName (name) mandatory protected copy
        // - jcr:supertypes (name) multiple protected copy
        // - jcr:isAbstract (boolean) mandatory protected copy
        // - jcr:isMixin (boolean) mandatory protected copy
        // - jcr:isQueryable (boolean) mandatory protected copy
        // - jcr:hasOrderableChildNodes (boolean) mandatory protected copy
        // - jcr:primaryItemName (name) protected copy
        // + jcr:propertyDefinition (nt:propertyDefinition) = nt:propertyDefinition sns protected copy
        // + jcr:childNodeDefinition (nt:childNodeDefinition) = nt:childNodeDefinition sns protected copy

        String nodeTypeName = valueFrom(props, "jcr:nodeTypeName");
        if (EXCLUDE_TYPES.contains(nodeTypeName)) return;
        boolean isMixin = valueFrom(props, "jcr:isMixin", false);
        boolean isAbstract = valueFrom(props, "jcr:isAbstract", false);
        boolean orderableChildren = valueFrom(props, "jcr:hasOrderableChildNodes", false);
        boolean queryable = valueFrom(props, "jcr:isQueryable", false);
        String primaryItemName = valueFrom(props, "jcr:primaryItemName");
        List<String> superTypeNames = valuesFrom(props, "jcr:supertypes");
        superTypeNames.removeAll(EXCLUDE_TYPES);

        List<PropertyDefinition> propDefns = new ArrayList<PropertyDefinition>();
        List<ChildNodeDefinition> childDefns = new ArrayList<ChildNodeDefinition>();

        // Process the children (the property definition and child node definition objects) ...
        if (body.has("children")) {
            Object obj = body.get("children");
            JSONObject children = (JSONObject)obj;
            for (Iterator<?> itr = children.keys(); itr.hasNext();) {
                String key = JsonUtils.decode(itr.next().toString());
                Object child = children.get(key);
                if (child != null) {
                    // and that means that each child is a JSONObject ...
                    if (key.startsWith("jcr:propertyDefinition")) { // may contain a SNS index
                        PropertyDefinition defn = createPropertyDefinition(key, (JSONObject)child, nodeTypeName, nodeTypes);
                        if (defn != null) propDefns.add(defn);
                    } else if (key.startsWith("jcr:childNodeDefinition")) { // may contain a SNS index
                        ChildNodeDefinition defn = createChildNodeDefinition(key, (JSONObject)child, nodeTypeName, nodeTypes);
                        if (defn != null) childDefns.add(defn);
                    }
                }
            }
        }

        // Create the node, which is automatically added to the map ...
        new NodeType(nodeTypeName, isMixin, isAbstract, superTypeNames, propDefns, childDefns, primaryItemName,
                     orderableChildren, queryable, nodeTypes);
    }

    protected PropertyDefinition createPropertyDefinition( String defnName,
                                                           JSONObject body,
                                                           String declaringNodeTypeName,
                                                           Map<String, NodeType> nodeTypes ) throws Exception {
        JSONObject props = (JSONObject)body.get("properties");

        // [nt:propertyDefinition]
        // - jcr:name (name) protected
        // - jcr:autoCreated (boolean) mandatory protected
        // - jcr:mandatory (boolean) mandatory protected
        // - jcr:isFullTextSearchable (boolean) mandatory protected
        // - jcr:isQueryOrderable (boolean) mandatory protected
        // - jcr:onParentVersion (string) mandatory protected
        // < 'COPY', 'VERSION', 'INITIALIZE', 'COMPUTE',
        // 'IGNORE', 'ABORT'
        // - jcr:protected (boolean) mandatory protected
        // - jcr:requiredType (string) mandatory protected
        // < 'STRING', 'BINARY', 'LONG', 'DOUBLE', 'BOOLEAN',
        // 'DATE', 'NAME', 'PATH', 'REFERENCE', 'UNDEFINED'
        // - jcr:valueConstraints (string) multiple protected
        // - jcr:availableQueryOperators (name) mandatory multiple protected
        // - jcr:defaultValues (undefined) multiple protected
        // - jcr:multiple (boolean) mandatory protected

        String name = valueFrom(props, "jcr:name", "*");
        int requiredType = typeValueFrom(props, "jcr:requiredType", PropertyType.UNDEFINED);
        boolean isAutoCreated = valueFrom(props, "jcr:autoCreated", false);
        boolean isMandatory = valueFrom(props, "jcr:mandatory", false);
        boolean isProtected = valueFrom(props, "jcr:protected", false);
        boolean isFullTextSearchable = valueFrom(props, "jcr:isFullTextSearchable", false);
        boolean isMultiple = valueFrom(props, "jcr:multiple", false);
        boolean isQueryOrderable = valueFrom(props, "jcr:isQueryOrderable", false);
        int onParentVersion = OnParentVersionAction.valueFromName(props.getString("jcr:onParentVersion"));
        List<String> defaultValues = valuesFrom(props, "jcr:defaultValues");
        List<String> valueConstraints = valuesFrom(props, "jcr:valueConstraints");
        List<String> availableQueryOperations = valuesFrom(props, "jcr:availableQueryOperators");

        return new PropertyDefinition(declaringNodeTypeName, name, requiredType, isAutoCreated, isMandatory, isProtected,
                                      isFullTextSearchable, isMultiple, isQueryOrderable, onParentVersion, defaultValues,
                                      valueConstraints, availableQueryOperations, nodeTypes);
    }

    protected ChildNodeDefinition createChildNodeDefinition( String defnName,
                                                             JSONObject body,
                                                             String declaringNodeTypeName,
                                                             Map<String, NodeType> nodeTypes ) throws Exception {
        JSONObject props = (JSONObject)body.get("properties");

        // [nt:childNodeDefinition]
        // - jcr:name (name) protected
        // - jcr:autoCreated (boolean) mandatory protected
        // - jcr:mandatory (boolean) mandatory protected
        // - jcr:onParentVersion (string) mandatory protected
        // < 'COPY', 'VERSION', 'INITIALIZE', 'COMPUTE',
        // 'IGNORE', 'ABORT'
        // - jcr:protected (boolean) mandatory protected
        // - jcr:requiredPrimaryTypes (name) = 'nt:base' mandatory protected multiple
        // - jcr:defaultPrimaryType (name) protected
        // - jcr:sameNameSiblings (boolean) mandatory protected

        String name = valueFrom(props, "jcr:name", "*");
        boolean isAutoCreated = valueFrom(props, "jcr:autoCreated", false);
        boolean isMandatory = valueFrom(props, "jcr:mandatory", false);
        boolean isProtected = valueFrom(props, "jcr:protected", false);
        boolean allowsSns = valueFrom(props, "jcr:sameNameSiblings", false);
        Set<String> requiredTypes = new HashSet<String>(valuesFrom(props, "jcr:requiredPrimaryTypes"));
        String defaultPrimaryType = valueFrom(props, "jcr:defaultPrimaryType");
        int onParentVersion = OnParentVersionAction.valueFromName(props.getString("jcr:onParentVersion"));

        return new ChildNodeDefinition(declaringNodeTypeName, name, requiredTypes, isAutoCreated, isMandatory, isProtected,
                                       allowsSns, onParentVersion, defaultPrimaryType, nodeTypes);
    }

    protected List<String> valuesFrom( JSONObject properties,
                                       String name ) throws Exception {
        if (!properties.has(name)) {
            // Just an empty collection ...
            return Collections.emptyList();
        }
        Object prop = properties.get(name);
        if (prop instanceof JSONArray) {
            // There are multple values ...
            JSONArray superArray = (JSONArray)prop;
            int length = superArray.length();
            if (length == 0) return Collections.emptyList();
            List<String> result = new ArrayList<String>(length);
            for (int i = 0; i < length; i++) {
                String value = superArray.getString(i);
                result.add(value);
            }
            return result;
        }
        // Just a single value ...
        return Collections.singletonList(prop.toString());
    }

    protected boolean valueFrom( JSONObject properties,
                                 String name,
                                 boolean defaultValue ) throws Exception {
        if (!properties.has(name)) {
            return defaultValue;
        }
        return properties.getBoolean(name);
    }

    protected String valueFrom( JSONObject properties,
                                String name ) throws Exception {
        return valueFrom(properties, name, (String)null);
    }

    protected String valueFrom( JSONObject properties,
                                String name,
                                String defaultValue ) throws Exception {
        if (!properties.has(name)) {
            return defaultValue;
        }
        return properties.getString(name);
    }

    protected int typeValueFrom( JSONObject properties,
                                 String name,
                                 int defaultType ) throws Exception {
        if (!properties.has(name)) return defaultType;
        String typeName = properties.getString(name);
        Integer result = PROPERTY_TYPES_BY_LOWERCASE_NAME.get(typeName.toLowerCase());
        return result != null ? result.intValue() : defaultType;
    }
}
