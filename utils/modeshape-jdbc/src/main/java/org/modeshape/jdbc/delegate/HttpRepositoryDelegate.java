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
package org.modeshape.jdbc.delegate;

import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.QueryResult;

import org.modeshape.jdbc.JcrDriver;
import org.modeshape.jdbc.JdbcI18n;
import org.modeshape.jdbc.JcrDriver.JcrContextFactory;
import org.modeshape.web.jcr.rest.client.domain.QueryRow;
import org.modeshape.web.jcr.rest.client.domain.Server;
import org.modeshape.web.jcr.rest.client.domain.Workspace;
import org.modeshape.web.jcr.rest.client.json.JsonRestClient;

/**
 * 
 * The HTTPRepositoryDelegate provides remote Repository implementation to access the Jcr layer via HTTP lookup.
 */
public class HttpRepositoryDelegate extends AbstractRepositoryDelegate {
    private static final String HTTP_EXAMPLE_URL = JcrDriver.HTTP_URL_PREFIX + "{hostname}:{port}/{context root}";
    
    
    private JsonRestClient restClient;
    private Workspace workspace = null;
    private Map<String, NodeType> nodeTypes;
    
    public HttpRepositoryDelegate(String url, Properties info, JcrContextFactory contextFactory) {
    	super(url, info);
    }
       
    @Override
	protected ConnectionInfo createConnectionInfo(String url, Properties info) {
    	return new HttpConnectionInfo(url, info);
    }

    @Override
    public QueryResult execute(String query, String language) throws RepositoryException {
       	LOGGER.trace("Executing query: {0}" + query );

       	try {
			List<QueryRow> results = this.restClient.query(workspace, language, query);
			Iterator<QueryRow> resultsIt = results.iterator();
			while (resultsIt.hasNext()) {
				final QueryRow row = resultsIt.next();
				
			}
			
		} catch (Exception e) {
		}

    	return null;
    }
    
    /**
	 * {@inheritDoc}
	 *
	 * @see org.modeshape.jdbc.delegate.RepositoryDelegate#getDescriptor(java.lang.String)
	 */
	@Override
	public String getDescriptor(String descriptorKey) {
		return "";
	}
       
    @Override
    public synchronized NodeType nodeType( String name ) throws RepositoryException {
    	if (nodeTypes == null) nodeTypes();
    	
    	NodeType nodetype = nodeTypes.get(name);
    	if (nodetype == null) {
    		 throw new RepositoryException(JdbcI18n.unableToGetNodeType.text(name));		   		
    	}
    	
    	return nodetype;
    }
       
    @Override
    public synchronized List<NodeType> nodeTypes() throws RepositoryException {
    	if (nodeTypes == null) {
	    	Collection<org.modeshape.web.jcr.rest.client.domain.NodeType> results;
			try {
				
				results = this.restClient.getNodeTypes(workspace, "/jcr:system/jcr:nodeTypes", "?depth=5");
				Map<String, NodeType> loadingNodeTypes = new HashMap<String, NodeType>(results.size());
				Iterator<org.modeshape.web.jcr.rest.client.domain.NodeType> resultsIt = results.iterator();
				while (resultsIt.hasNext()) {		
					org.modeshape.web.jcr.rest.client.domain.NodeType nodetype = resultsIt.next();
					if (nodetype != null) {
					
						HttpNodeType localnodetype = new HttpNodeType(nodetype);
					
						loadingNodeTypes.put(localnodetype.getName(), localnodetype);
					}
								
				}
				if (loadingNodeTypes.size() > 0) {
					this.nodeTypes = loadingNodeTypes;
				} else {
				    String msg = JdbcI18n.unableToGetNodeTypes.text();
				    throw new RepositoryException(msg);			
				}
				
			} catch (Exception e) {
			    String msg = JdbcI18n.unableToGetNodeTypes.text();
			    throw new RepositoryException(msg, e);			
			}
    	}
		
		return new ArrayList<NodeType>(nodeTypes.values());
    }

    @Override
    protected  void createRepository() throws SQLException {
       	LOGGER.debug("Creating repository for HttpRepositoryDelegte" );

       	ConnectionInfo info = getConnectionInfo();
    	assert info != null;
       	
       	String path = info.getRepositoryPath();
       	if (path == null) {
       		throw new SQLException("Missing repo path from " +info.getUrl());
       	}
       	if (info.getUsername() == null) {
       		throw new SQLException("Missing username from " +info.getUrl());
       	}
       	if (info.getPassword() == null) {
       		throw new SQLException("Missing password path from " +info.getUrl());
       	}
       	if (info.getRepositoryName() == null) {
       		throw new SQLException("Missing repo name from " +info.getUrl());
       	}
        Server server = new Server("http://" + path, info.getUsername(), new String(info.getPassword()));
        org.modeshape.web.jcr.rest.client.domain.Repository repo = new org.modeshape.web.jcr.rest.client.domain.Repository(info.getRepositoryName(), server);
        workspace = new Workspace(info.getWorkspaceName(), repo);

        restClient = new JsonRestClient();       
 
        // this is only a connection test to confirm a connection can be made and results can be obtained.
        try {
			restClient.getRepositories(server ) ;			
		} catch (Exception e) {
			throw new SQLException(JdbcI18n.noRepositoryNamesFound.text(), e);
		}     
        
      	Set<String> repositoryNames = new HashSet<String>(1);
     	repositoryNames.add(info.getRepositoryName());
	
		this.setRepositoryNames(repositoryNames);
		
    }  
    
    /**
     * 
     * @see java.sql.Connection#isValid(int)
     */
    @Override
    public boolean isValid( final int timeout ) throws RepositoryException {
    	return false;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#commit()
     */
    @Override
    public void commit() throws RepositoryException {
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#rollback()
     */
    @Override
    public void rollback() throws RepositoryException {
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Connection#close()
     */
    @Override
    public void close() {
		restClient = null;
		workspace = null;   	
    	if (nodeTypes != null) nodeTypes.clear();
    }
    
    class HttpConnectionInfo extends ConnectionInfo {
 		/**
		 * @param url
		 * @param properties
		 */
		protected HttpConnectionInfo(String url, Properties properties) {
		    super(url, properties);
	
		}
		
	    @Override
		protected void init() {
	    	// parsing 2 ways of specifying the repository and workspace
	    	// 1)  defined using ?repositoryName
	    	// 2)  defined in the path  server:8080/modeshape-rest/respositoryName/workspaceName
	    	
	    	super.init();
	    	
	    	// if the workspace and/or repository name is not specified as a property on the url, 
	    	// then parse the url to obtain the values from the path, the url must be in the format:
	    	//   {hostname}:{port} / {context root} + / respositoryName / workspaceName 

	    	StringBuilder url = new StringBuilder();
	    	String[] urlsections = repositoryPath.split("/");
	    	// if there are only 2 sections, then the url can have the workspace or repository name specified in the path
	    	if (urlsections.length < 3) {
	    		return;
	    	}
	    	
	    	// the assignment of url section is working back to front, this is so in cases where
	    	// the {context} is changed to be made up of multiple sections, instead of the default (modeshape-rest), the
	    	// workspace should be the last section (if exist) and the repository should be before the
	    	// workspace.
	    	int workspacePos = -1;
	    	int repositoryPos = -1;
	    	int repoPos = 1;
	    	if (this.getWorkspaceName() == null && urlsections.length > 3) {
	    		workspacePos = urlsections.length -1;
	    		String workspaceName = urlsections[workspacePos];	    		
	    		this.setWorkspaceName(workspaceName);
	    		// if workspace is found, then repository is assume in the prior section
	    		repoPos = 2;
	    		
	    	}
	    	if (this.getRepositoryName() == null && urlsections.length > 2) {
	    		repositoryPos = urlsections.length - repoPos;
	    		String repositoryName = urlsections[repositoryPos];
	    		this.setRepositoryName(repositoryName);
	    	}
	    	
	    	// rebuild the url without the repositoryName or WorkspaceName because 
	    	// the createConnection() needs these separated.
	    	for (int i = 0; i < repositoryPos; i++) {
	    		url.append(urlsections[i]);
	    		if (i < repositoryPos -1) {
	    			url.append("/");
	    		}
	    	}
	    	
	    	this.repositoryPath = url.toString();

	    }
		
		@Override
	    public String getUrlExample() {
	    	return HTTP_EXAMPLE_URL;
	    }
		
		@Override
	    public String getUrlPrefix() {
			return JcrDriver.HTTP_URL_PREFIX;
		}
		
		@Override		
		protected void addUrlPropertyInfo(List<DriverPropertyInfo> results) {
			// if the repository path doesn't have at least the {context} 
			// example:  server:8080/modeshape-rest   where modeshape-rest is the context,
			// then the URL is considered invalid.
			if (repositoryPath.indexOf("/") == -1) {
				setUrl(null);
			}
			super.addUrlPropertyInfo(results);
		}

    }
    
    
    
    protected class HttpNodeType implements javax.jcr.nodetype.NodeType {
    	private org.modeshape.web.jcr.rest.client.domain.NodeType restnodetype;
     	
    	public HttpNodeType(org.modeshape.web.jcr.rest.client.domain.NodeType nodetype) {
    		assert nodetype != null;
    		restnodetype = nodetype;    		
    	}
    	
    	public void setNodeType(org.modeshape.web.jcr.rest.client.domain.NodeType nodetype) {
    		restnodetype = nodetype;
    	}
    	
    	public org.modeshape.web.jcr.rest.client.domain.NodeType getNodeType() {
    		return restnodetype;
    	}
    	
    	public boolean hasProperties() {
    		return (restnodetype.getProperties().size() > 0 ? true : false);
    	}

		@Override
		public String getName() {
			return restnodetype.getName();
		}

		@Override
		public String getPrimaryItemName() {
			return restnodetype.getProperty("jcr:primaryItemName");
		}

		@Override
		public boolean isMixin() {			
			return convertBoolean("jcr:isMixin");
		}

		@Override
		public boolean isQueryable() {
			return convertBoolean("jcr:isQueryable");
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.NodeType#canAddChildNode(java.lang.String)
		 */
		@Override
		public boolean canAddChildNode(String arg0) {
			return false;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.NodeType#canAddChildNode(java.lang.String, java.lang.String)
		 */
		@Override
		public boolean canAddChildNode(String arg0, String arg1) {
			return false;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.NodeType#canRemoveItem(java.lang.String)
		 */
		@Override
		public boolean canRemoveItem(String arg0) {
			return false;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.NodeType#canRemoveNode(java.lang.String)
		 */
		@Override
		public boolean canRemoveNode(String arg0) {
			return false;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.NodeType#canRemoveProperty(java.lang.String)
		 */
		@Override
		public boolean canRemoveProperty(String arg0) {
			return false;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.NodeType#canSetProperty(java.lang.String, javax.jcr.Value)
		 */
		@Override
		public boolean canSetProperty(String arg0, Value arg1) {
			return false;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.NodeType#canSetProperty(java.lang.String, javax.jcr.Value[])
		 */
		@Override
		public boolean canSetProperty(String arg0, Value[] arg1) {
			return false;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.NodeType#getChildNodeDefinitions()
		 */
		@Override
		public NodeDefinition[] getChildNodeDefinitions() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.NodeType#getDeclaredSubtypes()
		 */
		@Override
		public NodeTypeIterator getDeclaredSubtypes() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.NodeType#getDeclaredSupertypes()
		 */
		@Override
		public javax.jcr.nodetype.NodeType[] getDeclaredSupertypes() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.NodeType#getSubtypes()
		 */
		@Override
		public NodeTypeIterator getSubtypes() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.NodeType#getSupertypes()
		 */
		@Override
		public javax.jcr.nodetype.NodeType[] getSupertypes() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.NodeType#isNodeType(java.lang.String)
		 */
		@Override
		public boolean isNodeType(String arg0) {		
			String value = restnodetype.getProperty("jcr:nodeTypeName");
			return (value != null && arg0.equals(value));

		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.NodeTypeDefinition#getDeclaredChildNodeDefinitions()
		 */
		@Override
		public NodeDefinition[] getDeclaredChildNodeDefinitions() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.NodeTypeDefinition#getDeclaredPropertyDefinitions()
		 */
		@Override
		public PropertyDefinition[] getDeclaredPropertyDefinitions() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.NodeTypeDefinition#getDeclaredSupertypeNames()
		 */
		@Override
		public String[] getDeclaredSupertypeNames() {
//			String value = restnodetype.getProperty("jcr:supertypes"); -- array
//			return new String[] {value};
			return null;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.NodeTypeDefinition#hasOrderableChildNodes()
		 */
		@Override
		public boolean hasOrderableChildNodes() {
			return convertBoolean("jcr:hasOrderableChildNodes");		
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.NodeTypeDefinition#isAbstract()
		 */
		@Override
		public boolean isAbstract() {
			return convertBoolean("jcr:isAbstract");		
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.NodeType#getPropertyDefinitions()
		 */
		@Override
		public PropertyDefinition[] getPropertyDefinitions() {
			PropertyDefinition[] defns = null;
			int cnt  = restnodetype.getPropertyDefinitions().size() + restnodetype.getChildNodeDefinitions().size();
			defns = new PropertyDefinition[cnt];
			int i = 0;
			if (restnodetype.getPropertyDefinitions() != null) {
				for (Iterator<org.modeshape.web.jcr.rest.client.domain.NodeType> it=restnodetype.getPropertyDefinitions().iterator(); it.hasNext();){
					org.modeshape.web.jcr.rest.client.domain.NodeType nt = it.next();
					HttpPropertyDefinition propDefn = new HttpPropertyDefinition(nt.getName(), nt.getProperties()) ;
					defns[i] = propDefn;
					i++;
				}
			}
			
			if (restnodetype.getChildNodeDefinitions() != null) {
				for (Iterator<org.modeshape.web.jcr.rest.client.domain.NodeType> it=restnodetype.getChildNodeDefinitions().iterator(); it.hasNext();){
					org.modeshape.web.jcr.rest.client.domain.NodeType nt = it.next();
					HttpPropertyDefinition propDefn = new HttpPropertyDefinition(nt.getName(), nt.getProperties()) ;
					defns[i] = propDefn;
					i++;
				}
			}
			return defns;
		}
    	
		
		private boolean convertBoolean(String key ) {
			String value = restnodetype.getProperty(key);
			if (value == null || value.equalsIgnoreCase("false")) return false;			
			return true;
		}    	
    }
    
    protected class HttpPropertyDefinition implements PropertyDefinition {
    	private Properties properties = null;
    	private String name;
    	
    	public HttpPropertyDefinition(String name, Properties nodeTypeProperties) {
    		this.properties = nodeTypeProperties;
    		this.name = name;
    	}
    	

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.PropertyDefinition#getAvailableQueryOperators()
		 */
		@Override
		public String[] getAvailableQueryOperators() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.PropertyDefinition#getDefaultValues()
		 */
		@Override
		public Value[] getDefaultValues() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.PropertyDefinition#getRequiredType()
		 */
		@Override
		public int getRequiredType() {
			return 0;
//			String value = properties.getProperty("jcr:requiredType");
//			if (value == null) return PropertyDefinition.
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.PropertyDefinition#getValueConstraints()
		 */
		@Override
		public String[] getValueConstraints() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.PropertyDefinition#isFullTextSearchable()
		 */
		@Override
		public boolean isFullTextSearchable() {
			return false;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.PropertyDefinition#isMultiple()
		 */
		@Override
		public boolean isMultiple() {
			return false;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.PropertyDefinition#isQueryOrderable()
		 */
		@Override
		public boolean isQueryOrderable() {
			return true;
	//		return convertBoolean("jcr:isQueryable");
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.ItemDefinition#getDeclaringNodeType()
		 */
		@Override
		public NodeType getDeclaringNodeType() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.ItemDefinition#getName()
		 */
		@Override
		public String getName() {
			return  name;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.ItemDefinition#getOnParentVersion()
		 */
		@Override
		public int getOnParentVersion() {
			return 0;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.ItemDefinition#isAutoCreated()
		 */
		@Override
		public boolean isAutoCreated() {
			return convertBoolean("jcr:autoCreated");
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.ItemDefinition#isMandatory()
		 */
		@Override
		public boolean isMandatory() {
			return convertBoolean("jcr:mandatory");
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see javax.jcr.nodetype.ItemDefinition#isProtected()
		 */
		@Override
		public boolean isProtected() {
			return convertBoolean("jcr:protected");
		}
		
		
		private boolean convertBoolean(String key ) {
			String value = properties.getProperty(key);
			if (value == null || value.equalsIgnoreCase("false")) return false;			
			return true;
		} 
    	
    }
  
}
