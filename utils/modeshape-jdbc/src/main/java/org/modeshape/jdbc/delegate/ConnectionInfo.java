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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;

import org.modeshape.jcr.api.Repositories;
import org.modeshape.jdbc.JcrDriver;
import org.modeshape.jdbc.JdbcI18n;
import org.modeshape.jdbc.util.StringUtil;
import org.modeshape.jdbc.util.TextDecoder;
import org.modeshape.jdbc.util.UrlEncoder;

/**
 * The ConnectionInfo contains the information used to connect to the Jcr Repository.
 * 
 */
public abstract class ConnectionInfo {
    public static final TextDecoder URL_DECODER = new UrlEncoder();
	
    protected String url;
    protected String repositoryPath;
    protected Properties properties;
    private char propertyDelimiter='?';

    protected ConnectionInfo(String url, Properties properties) {
		this.url = url;
		this.properties = properties;
    }
    
    
    protected void init() {
	    Properties props = getProperties() != null ? (Properties) getProperties().clone() : new Properties();
	    repositoryPath = getUrl().substring(this.getUrlPrefix().length());
		  
	    // Find any URL parameters ...
	    int questionMarkIndex = repositoryPath.indexOf('?');
	    if (questionMarkIndex != -1) {
			if (repositoryPath.length() > questionMarkIndex + 1) {
			    String paramStr = repositoryPath.substring(questionMarkIndex + 1);
			    for (String param : paramStr.split("&")) {
			    	String[] pair = param.split("=");
					if (pair.length > 1) {
					    String key = URL_DECODER
						    .decode(pair[0] != null ? pair[0].trim()
							    : null);
					    String value = URL_DECODER
						    .decode(pair[1] != null ? pair[1].trim()
							    : null);
					    if (!props.containsKey(key)) {
					    	props.put(key, value);
					    }
					}
			    }
			}
			repositoryPath = repositoryPath.substring(0, questionMarkIndex).trim();
	    }

	    Properties newprops = new Properties();
	    newprops.putAll(props);
	    this.setProperties(newprops);
	    String url = getUrl();
	    this.setUrl(url != null ? url.trim() : null);

    }
    
    /**
     * Get the original URL of the connection.
     * 
     * @return the URL; never null
     */
    public String getUrl() {
    	return url;
    }
    
    /**
     * Get the part of the {@link #getUrl()} that indicates the path to connect to the Repository.  
     * This value should be prefixed by the {@link #getUrlPrefix()} in the {@link #getUrl()}.
     * @return String
     */
    public String getRepositoryPath() {
    	return this.repositoryPath;
    }

    /**
     * Get the immutable properties for the connection.
     * 
     * @return the properties; never null
     */
    public Properties getProperties() {
    	return properties;
    }

    /**
     * Get the name of the repository. This is required only if the 
     * {@link Repositories} instance is being used to obtain the Repository.
     * 
     * @return the name of the repository, or null if no repository name was
     *         specified
     */
    public String getRepositoryName() {
    	return properties.getProperty(JcrDriver.REPOSITORY_PROPERTY_NAME);
    }
    
    /**
     * Call to set the repository name.  This is called when no repository name is set 
     * on the URL, but there is only one repository in the list.
     * @param repositoryName
     */
    void setRepositoryName(String repositoryName) {
    	this.properties.setProperty(JcrDriver.REPOSITORY_PROPERTY_NAME, repositoryName);
    }

    /**
     * Get the name of the workspace. This is not required, and if abscent
     * implies obtaining the JCR Repository's default workspace.
     * 
     * @return the name of the workspace, or null if no workspace name was
     *         specified
     */
    public String getWorkspaceName() {
    	return properties.getProperty(JcrDriver.WORKSPACE_PROPERTY_NAME);
    }
    
    /**
     * Call to set the workspace name. This is not required, and if abscent
     * implies obtaining the JCR Repository's default workspace.
     * @param workSpaceName 

     */
    public void setWorkspaceName(String workSpaceName) {
    	properties.setProperty(JcrDriver.WORKSPACE_PROPERTY_NAME, workSpaceName);
    }

    /**
     * Get the JCR user name. This is not required, and if abscent implies that
     * no credentials should be used when obtaining a JCR Session.
     * 
     * @return the JCR user name, or null if no user name was specified
     */
    public String getUsername() {
    	return properties.getProperty(JcrDriver.USERNAME_PROPERTY_NAME);
    }

    /**
     * Get the JCR password. This is not required.
     * 
     * @return the JCR password, or null if no password was specified
     */
    public char[] getPassword() {
		String result = properties.getProperty(JcrDriver.PASSWORD_PROPERTY_NAME);
		return result != null ? result.toCharArray() : null;
    }
    
    /**
     * Return true of Teiid support is required for this connection.
     * @return true if Teiid support is required.
     */
	public boolean isTeiidSupport() {
		String result = properties.getProperty(JcrDriver.TEIID_SUPPORT_PROPERTY_NAME);
		if (result == null) return false;
		return result.equalsIgnoreCase(Boolean.TRUE.toString());
    }

    void setUrl(String url) {
    	this.url = url;
    }

    void setProperties(Properties properties) {
    	this.properties = properties;
    }

	/**
	 * Get the effective URL of this connection, which places all properties
	 * on the URL (with a '*' for each character in the password property)
	 * 
	 * @return the effective URL; never null
	 */
	public String getEffectiveUrl() {
	    StringBuilder url = new StringBuilder(this.getUrlPrefix());
	    url.append(this.getRepositoryPath());
	    char propertyDelim = getPropertyDelimiter();
	    for (String propertyName : getProperties().stringPropertyNames()) {
			String value = getProperties().getProperty(propertyName);
			if (value == null)
			    continue;
			if (JcrDriver.PASSWORD_PROPERTY_NAME.equals(propertyName)) {
			    value = StringUtil.createString('*', value.length());
			}
			url.append(propertyDelim).append(propertyName).append('=')
				.append(value);
			propertyDelim = '&';
	    }
	    return url.toString();
	}
	
	/**
	 * Return the starting property delimiter
	 * @return char property delimiter
	 */
	protected char getPropertyDelimiter() {
		return propertyDelimiter;
	}
	
	protected void setPropertyDelimiter(char delimiter) {
		this.propertyDelimiter = delimiter;
	}
	
	
    
    /**
     * Obtain the array of {@link DriverPropertyInfo} objects that describe the missing properties.
     * 
     * @return DriverPropertyInfo the property infos; never null but possibly empty
     */
    public DriverPropertyInfo[] getPropertyInfos( ) {
		List<DriverPropertyInfo> results = new ArrayList<DriverPropertyInfo>();
		
		addUrlPropertyInfo(results);
		addUserNamePropertyInfo(results);
		addPasswordPropertyInfo(results);
		addWorkspacePropertyInfo(results);
		addRepositoryNamePropertyInfo(results);
		
		return results.toArray(new DriverPropertyInfo[results.size()]);
    }
 
    protected void addUrlPropertyInfo(List<DriverPropertyInfo> results) {
		if (getUrl() == null) {
		    DriverPropertyInfo info = new DriverPropertyInfo(
			    JdbcI18n.urlPropertyName.text(), null);
		    info.description = JdbcI18n.urlPropertyDescription
			    .text(this.getEffectiveUrl(), getUrlExample());
		    info.required = true;
		    info.choices = new String[] { getUrlExample() };
		    results.add(info);
		}
    }
    
    protected void addUserNamePropertyInfo(List<DriverPropertyInfo> results) {
		if (getUsername() == null) {
		    DriverPropertyInfo info = new DriverPropertyInfo(
			    JdbcI18n.usernamePropertyName.text(), null);
		    info.description = JdbcI18n.usernamePropertyDescription.text();
		    info.required = false;
		    info.choices = null;
		    results.add(info);
		}
    }
    
    protected void addPasswordPropertyInfo(List<DriverPropertyInfo> results) {
		if (getPassword() == null) {
		    DriverPropertyInfo info = new DriverPropertyInfo(
			    JdbcI18n.passwordPropertyName.text(), null);
		    info.description = JdbcI18n.passwordPropertyDescription.text();
		    info.required = false;
		    info.choices = null;
		    results.add(info);
		}
    }
    
    protected void addWorkspacePropertyInfo(List<DriverPropertyInfo> results) {
		if (getWorkspaceName() == null) {
		    DriverPropertyInfo info = new DriverPropertyInfo(
			    JdbcI18n.workspaceNamePropertyName.text(), null);
		    info.description = JdbcI18n.workspaceNamePropertyDescription.text();
		    info.required = false;
		    info.choices = null;
		    results.add(info);
		}
    }
    
    protected  void addRepositoryNamePropertyInfo(List<DriverPropertyInfo> results) {
		if (getRepositoryName() == null) {
				DriverPropertyInfo info = new DriverPropertyInfo(JdbcI18n.repositoryNamePropertyName.text(), null);
				info.description = JdbcI18n.repositoryNamePropertyDescription.text();
				info.required = true;
				info.choices = null;
				results.add(info);
		}
    }
    	
    /**
     * The delegate should provide an example of the URL to be used	
     * @return String url example
     */
    public abstract String getUrlExample();
    
    /**
     * The delegate should provide the prefix defined by the {@link JcrDriver}
     * @return String url prefix
     */
    public abstract String getUrlPrefix();
    
    /**
     * Return the credentials based on the user name and password.
     * 
     * @return Credentials
     */
    public Credentials getCredentials() {
		String username = getUsername();
		char[] password = getPassword();
		if (username != null) {
		    return new SimpleCredentials(username, password);
		}
		return null;
    }
    
}