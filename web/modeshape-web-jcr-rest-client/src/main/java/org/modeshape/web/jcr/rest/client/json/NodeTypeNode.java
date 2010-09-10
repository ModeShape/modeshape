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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import net.jcip.annotations.Immutable;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.common.util.CheckArg;
import org.modeshape.web.jcr.rest.client.domain.NodeType;
import org.modeshape.web.jcr.rest.client.domain.Workspace;

/**
 * The <code>NodeTypeNode</code> class is responsible for knowing how to obtain a NodeType based 
 * on the Workspace.
 * <br>
 * 
 * An example <code>URL</code> to obtain all the node types would look like:
 * <br>
 * 	<i>{context root}/{repository name}/{workspace name}/items/jcr:system/jcr:nodeTypes"</i>
 * <br>
 * And an example url to obtain a specific node type would look like:
 * <br>
 * 	<i>{context root}/{repository name}/{workspace name}/items/jcr:system/jcr:nodeTypes/{node type name}</i>
 * 
 * <br>
 * A Node Type will not be created if:
 * <li>jcr:isMixin is true</li>
 * <li>jcr:multiple is true</li>
 */
@Immutable
public final class NodeTypeNode extends JsonNode {
	
    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

	/**
	 * EXCLUDE_TYEPS are those node types that are to be excluded from the inclusion
	 * in the node types returned.
	 */
	private static Set<String> EXCLUDE_TYPES = new HashSet<String>() ;
    /**
     * The workspace from where the node type is being obtained.
     */
    private final Workspace workspace;
    
    private final String depth;
        
    private Map<String, NodeType>nodeTypeMap = new HashMap<String, NodeType>();
    
    {
    	EXCLUDE_TYPES.add("mode:defined");
    	EXCLUDE_TYPES.add("*");
    }
    
    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * Use this constructor if wanting all node types for a workspace
     * @param workspace the workspace being used (never <code>null</code>)
     * @param relative_path is the relative location after the workspace
     * @param nodeDepth , nullable, can specify the depth of the node types to be returned
     * @throws Exception if there is a problem creating the folder node
     */
    public NodeTypeNode( Workspace workspace, String relative_path, String  nodeDepth) throws Exception {
        super(relative_path);

       	assert workspace != null;
 
        this.workspace = workspace;
        this.depth = nodeDepth;
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

        if (!path.startsWith("/")) {
            path = '/' + path;
        }

        // make sure path does NOT end with a '/'
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        // path needs to be encoded
        url.append(JsonUtils.encode(path));      	
        	
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

        JSONObject body = new JSONObject(jsonResponse);
        NodeType parent = createNodeType(null, body, null);

    	processBody(body, parent);
        return nodeTypeMap.values();
    }
    
    @SuppressWarnings("unchecked")
	protected void processBody(JSONObject body, NodeType parentNodeType) throws Exception {   	        
        NodeType parent = parentNodeType; 
     	
    	if (body.has("children")) {
    		Object obj = body.get("children");
 
    		if (obj instanceof JSONObject) {
    			
		    	JSONObject children = (JSONObject) obj;
		        for (Iterator<String> itr = children.keys(); itr.hasNext();) {
		            String key = JsonUtils.decode(itr.next());	            
		            Object child = children.get(key);
		            if (child != null) {
		            		
		            	if (child instanceof JSONObject) {
		            		JSONObject jsonchild = (JSONObject) child;
		            		createNodeType(key, jsonchild, parent);		

				   		} else if (child instanceof JSONArray) {
							JSONArray childarray = (JSONArray) child;
							for (int idx=0; idx<childarray.length(); idx++) {
								String cname = childarray.getString(idx);
								createNodeType(cname, null, parent);	
							}
				   		} else {
				   			throw new Exception("Program Error: didnt handle object type: " + child.getClass().getName());
				   		}

		            }

		        }
    		} else if (obj instanceof JSONArray) {
             	JSONArray childarray = (JSONArray) obj;
            	for (int i=0; i<childarray.length(); i++) {
            		String cname = childarray.getString(i);
            		createNodeType(cname, null, parent);
            	}
    		} else {
	   			throw new Exception("Program Error: didnt handle object type: " + obj.getClass().getName());
	   		}
    	}
    }

     
 	@SuppressWarnings({ "unchecked", "null" })
	private NodeType createNodeType(String childkey, JSONObject childNode, NodeType parentNodeType) throws Exception {
 		JSONObject jsonProperties = null;
  		if (childNode == null) {
       		NodeType type =  new NodeType(childkey, this.workspace, null);       		
       		if (parentNodeType != null)  parentNodeType.addChildNodeType(type);
       		return type;
 		} 
  		if (!childNode.has("properties")) {
       		NodeType type =  new NodeType(childkey, this.workspace, null);       		
       		if (parentNodeType != null)  parentNodeType.addChildNodeType(type);
    		if (childNode != null) {
    			processBody(childNode, type);
    		}
       		return type;
        }
  		
		Object cobj = childNode.get("properties");
		assert cobj != null;
		jsonProperties = (JSONObject) cobj;
		String nodeName = childkey;

		// determine if the node should be excluded
		
		// exclude if its a multi-valued definition
		if (jsonProperties.has("jcr:multiple")) {
			String isMultipleValue = jsonProperties.getString("jcr:multiple");
   			// do not add nodetypes where it represents a multivalue 
   			if (isMultipleValue!= null && isMultipleValue.equalsIgnoreCase("true")) {
   				return null;
   			}
		}
		
		// exclude if the definition is a mixin
		if (jsonProperties.has("jcr:isMixin")) {
			String isMixin = jsonProperties.getString("jcr:isMixin");
   			// do not add mixins 
   			if (isMixin!= null && isMixin.equalsIgnoreCase("true")) {
   				return null;
   			}
		}
		
		boolean propDefn = false;
    	if (childkey == null) {
    		if (jsonProperties.has("jcr:nodeTypeName")) {
    			nodeName = jsonProperties.getString("jcr:nodeTypeName");
    		} else if (jsonProperties.has("jcr:name")) {
    			nodeName = jsonProperties.getString("jcr:name");
    		} else {
    			return null;
    		}
    	} else if (childkey.startsWith("jcr:propertyDefinition")) {
    		if (jsonProperties.has("jcr:name")) {   		
    			nodeName = jsonProperties.getString("jcr:name");
    			propDefn = true;
    		} else {
    			// exlude when no jcr:name is present
    			return null;
    		}
    	} else if (childkey.startsWith("jcr:childNodeDefinition")) {
    		// exclude childNodeDefinitions
    		return null;
    		
    	}  
    	
    	CheckArg.isNotNull(nodeName, "nodeName ends up in null state for childkey: " + childkey);
    	
        if (EXCLUDE_TYPES.contains(nodeName)) return null;

        Set<NodeType> superTypes = new HashSet<NodeType>(3);
        
   		Properties properties = new Properties();
        for (Iterator<String> itr = jsonProperties.keys(); itr.hasNext();) {
            String key = JsonUtils.decode(itr.next());
            Object obj = jsonProperties.get(key);
            if (obj != null) {
            	if (obj instanceof JSONObject) {
		   			throw new Exception("Program Error: didnt handle object type: " + obj.getClass().getName());
            	} 
            	
            	if (key.equalsIgnoreCase("jcr:supertypes")) {

                 	JSONArray superArray = jsonProperties.getJSONArray(key);
                 	
                	for (int i=0; i<superArray.length(); i++) {
                		String cname = superArray.getString(i);
                		if (!EXCLUDE_TYPES.contains(cname)) {	                		
	                		NodeType superType = new WeakSuperTypeReference(cname, workspace, null, this.nodeTypeMap);
	                		superTypes.add(superType);
                		}                		
                	}
            	}
            	properties.put(key, obj.toString());
            }           	
         }

       	NodeType childnodeType = new NodeType(nodeName, this.workspace, properties);
              	
       	if (parentNodeType  != null) {
       		if (propDefn) {
       			 parentNodeType.addPropertyDefinitionNodeType(childnodeType);           	
       		} else {
       			parentNodeType.addChildNodeType(childnodeType);
       		}
       	}
        
        for (Iterator<NodeType> it=superTypes.iterator(); it.hasNext();) {
        	childnodeType.addSuperNodeType(it.next());
        }
        
 		this.nodeTypeMap.put(childnodeType.getName(), childnodeType);
		
		if (childNode != null) {
			processBody(childNode, childnodeType);
		}

        return childnodeType;

    }
   
    /**
     * @param jsonResponse the HTTP connection JSON response (never <code>null</code>) containing the NodeTypes
     * @return the node types for this workspace (never <code>null</code>)
     * @throws Exception if there is a problem obtaining the node types
     */

    @SuppressWarnings("unchecked")
	public NodeType getNodeType( String jsonResponse ) throws Exception {
        CheckArg.isNotNull(jsonResponse, "jsonResponse");
        JSONObject jsonChild = new JSONObject(jsonResponse);        
       
        NodeType nodetype = createNodeType(null, jsonChild, null);

       return nodetype;
    }
    
    /**
     * The Super Type weak reference is used because there's no guaranteed order for loading
     * all the node types, therefore, a referenced super type might not have been loaded
     * at the time of loading its child.   Therefore, the <code>findSuperTypeMap</code>
     * is used to find the super type after the fact. 
     *
     */
    class WeakSuperTypeReference extends NodeType {
        private Map<String, NodeType>findSuperTypeMap = null;
    	
        public WeakSuperTypeReference( String name,
        		Workspace workspace,
        		Properties properties,
        	    Map<String, NodeType>nodeTypeMap) {
        	super(name, workspace, properties);
        		findSuperTypeMap = nodeTypeMap;
         }
        
        @SuppressWarnings("unchecked")
		@Override
    	public List<NodeType> getPropertyDefinitions() {
        	NodeType superType = findSuperTypeMap.get(getName());
        	return (List<NodeType>) (superType != null ? superType.getPropertyDefinitions() : Collections.emptyList());
        }
        
        @SuppressWarnings("unchecked")
        @Override
    	public List<NodeType> getChildNodeDefinitions() {
        	NodeType superType = findSuperTypeMap.get(getName());
        	return (List<NodeType>) (superType != null ? superType.getChildNodeDefinitions() : Collections.emptyList());
        }
    	
    }

}
