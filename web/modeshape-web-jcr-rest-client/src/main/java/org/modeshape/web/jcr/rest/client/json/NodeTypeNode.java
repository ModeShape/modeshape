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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import net.jcip.annotations.Immutable;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.common.util.CheckArg;
import org.modeshape.web.jcr.rest.client.domain.NodeType;
import org.modeshape.web.jcr.rest.client.domain.Workspace;

/**
 * The <code>NodeTypeNode</code> class is responsible for knowing how to obtain a NodeType based on the Workspace. <br>
 * An example <code>URL</code> to obtain all the node types would look like: <br>
 * <i>{context root}/{repository name}/{workspace name}/items/jcr:system/jcr:nodeTypes"</i> <br>
 * And an example url to obtain a specific node type would look like: <br>
 * <i>{context root}/{repository name}/{workspace name}/items/jcr:system/jcr:nodeTypes/{node type name}</i>
 */
@Immutable
public final class NodeTypeNode extends JsonNode {

    /* the path used to get to the node types */
    // private static final String GET_NODE_TYPES_URL = "/jcr:system/jcr:nodeTypes";

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The workspace from where the node type is being obtained.
     */
    private final Workspace workspace;

    private final String depth;

    private Map<String, NodeType> nodeTypeMap = new HashMap<String, NodeType>();

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * Use this constructor if wanting all node types for a workspace
     * 
     * @param workspace the workspace being used (never <code>null</code>)
     * @param relative_path is the relative location after the workspace
     * @param nodeDepth , nullable, can specify the depth of the node types to be returned
     * @throws Exception if there is a problem creating the folder node
     */
    public NodeTypeNode( Workspace workspace,
                         String relative_path,
                         String nodeDepth ) throws Exception {
        super(relative_path);

        assert workspace != null;

        this.workspace = workspace;
        this.depth = nodeDepth;
    }

    // /**
    // * Use this constructor if wanting a specific node type
    // * @param workspace the workspace being used (never <code>null</code>)
    // * @param relative_path is the relative location after the workspace
    // * @param nodeTypeName is the node type to be returned
    // * @throws Exception if there is a problem creating the folder node
    // */
    // public NodeTypeNode( Workspace workspace, String relative_path) throws Exception {
    // super(relative_path);
    //
    // CheckArg.isNotNull(workspace, "workspace");
    // CheckArg.isNotNull(relative_path, "relative_path");
    // 
    // this.workspace = workspace;
    // this.depth = 2;
    // }

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

        if (!path.startsWith("/")) {
            path = '/' + path;
        }

        // make sure path does NOT end with a '/'
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        // path needs to be encoded
        url.append(JsonUtils.encode(path));

        // if wanting a specific node type, need to append the node type name and the depth in order to get its properties
        // if (this.nodeTypeName != null) {
        // // url.append("/").append(JsonUtils.encode(this.nodeTypeName)).append(JsonUtils.encode("?depth=" + this.depth));
        // url.append("/").append((this.nodeTypeName));
        // } else if (depth >= 0) {
        // url.append(("?depth=" + this.depth));
        // }

        if (this.depth != null) {
            url.append(this.depth);
        }

        return new URL(url.toString());
    }

    /**
     * @param jsonResponse the HTTP connection JSON response (never <code>null</code>) containing the NodeTypes
     * @return the node types for this workspace (never <code>null</code>)
     * @throws Exception if there is a problem obtaining the node types
     */
    public Collection<NodeType> getNodeTypes( String jsonResponse ) throws Exception {
        CheckArg.isNotNull(jsonResponse, "jsonResponse");
        Collection<NodeType> nodetypes = new ArrayList<NodeType>();
        JSONObject body = new JSONObject(jsonResponse);
        processBody(body, nodetypes, null);
        return nodetypes;
    }

    @SuppressWarnings( "unchecked" )
    protected void processBody( JSONObject body,
                                Collection<NodeType> nodetypes,
                                NodeType parentNodeType ) throws Exception {
        NodeType parent = parentNodeType;

        parent = createNodeType(null, body, null);
        nodetypes.add(parent);
        this.nodeTypeMap.put(parent.getName(), parent);

        if (body.has("children")) {
            Object obj = body.get("children");

            if (obj instanceof JSONObject) {

                JSONObject children = (JSONObject)obj;
                for (Iterator<String> itr = children.keys(); itr.hasNext();) {
                    String key = JsonUtils.decode(itr.next());

                    Object child = children.get(key);
                    if (child != null) {

                        if (child instanceof JSONObject) {
                            JSONObject jsonchild = (JSONObject)child;
                            NodeType nodeType = createNodeType(key, jsonchild, parent);
                            nodetypes.add(nodeType);
                            processNodeType(nodeType, jsonchild, parent);

                        } else if (child instanceof JSONArray) {
                            JSONArray childarray = (JSONArray)child;
                            for (int idx = 0; idx < childarray.length(); idx++) {
                                String cname = childarray.getString(idx);
                                NodeType childnodeType = createNodeType(cname, null, parent);
                                nodetypes.add(childnodeType);
                            }
                        } else {
                            throw new Exception("Program Error: didnt handle object type: " + child.getClass().getName());
                        }

                    }

                }
            } else if (obj instanceof JSONArray) {
                JSONArray childarray = (JSONArray)obj;
                for (int i = 0; i < childarray.length(); i++) {
                    String cname = childarray.getString(i);
                    createNodeType(cname, null, parent);
                }
            } else {
                throw new Exception("Program Error: didnt handle object type: " + obj.getClass().getName());
            }
        }
    }

    @SuppressWarnings( "unchecked" )
    private void processNodeType( NodeType nodeType,
                                  JSONObject child,
                                  NodeType parentNodeType ) throws Exception {

        if (child.has("children")) {
            Object obj = child.get("children");

            if (obj instanceof JSONObject) {
                JSONObject children = (JSONObject)obj;
                for (Iterator<String> itr = children.keys(); itr.hasNext();) {
                    String childkey = JsonUtils.decode(itr.next());
                    Object childObj = children.get(childkey);

                    if (childObj instanceof JSONObject) {
                        JSONObject childJson = (JSONObject)childObj;
                        NodeType childnodeType = createNodeType(childkey, childJson, nodeType);
                        processNodeType(childnodeType, (JSONObject)childObj, nodeType);

                    } else {
                        throw new Exception("Program Error: class type not handled " + childObj.getClass().getName());
                    }
                }

            } else if (obj instanceof JSONArray) {
                JSONArray childarray = (JSONArray)obj;
                for (int i = 0; i < childarray.length(); i++) {
                    String cname = childarray.getString(i);
                    createNodeType(cname, null, nodeType);
                }

            } else {
                throw new Exception("Program Error: didnt handle object type: " + obj.getClass().getName());
            }

        }

    }

    @SuppressWarnings( "unchecked" )
    private NodeType createNodeType( String childkey,
                                     JSONObject childNode,
                                     NodeType parentNodeType ) throws Exception {
        JSONObject jsonProperties = null;
        if (childNode == null) {
            NodeType type = new NodeType(childkey, this.workspace, null);
            if (parentNodeType != null) parentNodeType.addChildNodeType(type);
            return type;
        } else if (!childNode.has("properties")) {
            NodeType type = new NodeType(childkey, this.workspace, null);
            if (parentNodeType != null) parentNodeType.addChildNodeType(type);
            return type;
        } else {
            Object cobj = childNode.get("properties");
            assert cobj != null;
            jsonProperties = (JSONObject)cobj;
        }

        Properties properties = new Properties();

        // keys are the repository names
        for (Iterator<String> itr = jsonProperties.keys(); itr.hasNext();) {
            String key = JsonUtils.decode(itr.next());
            Object obj = jsonProperties.get(key);
            if (obj != null) {
                if (obj instanceof JSONObject) {
                    // JSONObject child = (JSONObject) obj;

                    throw new Exception("Program Error: didnt handle object type: " + obj.getClass().getName());
                    // processBody(child, nodetypes, nodeType);
                }
                properties.put(key, obj.toString());
            }
        }

        String nodeName = childkey;
        NodeType childnodeType = null;

        if (nodeName == null) {
            nodeName = properties.getProperty("jcr:primaryType", childkey);
            childnodeType = new NodeType(nodeName, this.workspace, properties);
            if (parentNodeType != null) parentNodeType.addChildNodeType(childnodeType);

        } else if (childkey.equalsIgnoreCase("jcr:propertyDefinition")) {
            nodeName = properties.getProperty("jcr:name", childkey);
            childnodeType = new NodeType(nodeName, this.workspace, properties);
            if (parentNodeType != null) parentNodeType.addPropertyDefinitionNodeType(childnodeType);

        } else if (childkey.equalsIgnoreCase("jcr:childNodeDefinition")) {
            nodeName = properties.getProperty("jcr:name", childkey);
            childnodeType = new NodeType(nodeName, this.workspace, properties);
            if (parentNodeType != null) parentNodeType.addChildNodeDefinitionNodeType(childnodeType);

        } else if (properties.getProperty("jcr:nodeTypeName") != null) {
            nodeName = properties.getProperty("jcr:nodeTypeName", childkey);
            childnodeType = new NodeType(nodeName, this.workspace, properties);
            if (parentNodeType != null) parentNodeType.addChildNodeType(childnodeType);
        }

        if (nodeName == null) {
            nodeName = (childkey != null ? childkey : "NotDefined");
        }

        CheckArg.isNotNull(nodeName, "nodeName ends up in null state for childkey: " + childkey);
        return childnodeType;

    }

    /**
     * @param jsonResponse the HTTP connection JSON response (never <code>null</code>) containing the NodeTypes
     * @return the node types for this workspace (never <code>null</code>)
     * @throws Exception if there is a problem obtaining the node types
     */

    @SuppressWarnings( "unchecked" )
    public NodeType getNodeType( String jsonResponse ) throws Exception {
        CheckArg.isNotNull(jsonResponse, "jsonResponse");
        JSONObject jsonChild = new JSONObject(jsonResponse);

        Properties properties = new Properties();

        NodeType nodetype = null;

        // keys are the repository names
        for (Iterator<String> itr = jsonChild.keys(); itr.hasNext();) {
            String name = JsonUtils.decode(itr.next());
            Object obj = jsonChild.get(name);
            if (obj != null && obj instanceof JSONObject) {
                JSONObject child = (JSONObject)obj;
                nodetype = new NodeType(name, this.workspace, properties);
                this.processNodeType(nodetype, child, null);

                return nodetype;
            }
        }

        return null;
    }

}
