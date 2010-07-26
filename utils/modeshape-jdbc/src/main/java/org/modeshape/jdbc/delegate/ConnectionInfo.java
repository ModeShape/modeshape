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
import java.util.Properties;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;

import org.modeshape.jcr.api.Repositories;
import org.modeshape.jdbc.JcrDriver;

/**
 * The ConnectionInfo contains the information used to connect to the Jcr Repository.
 * 
 */
public abstract class ConnectionInfo {
    private String url;
    private Properties properties;

    protected ConnectionInfo(String url, Properties properties) {
	this.url = url;
	this.properties = properties;

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
     * Get the immutable properties for the connection.
     * 
     * @return the properties; never null
     */
    public Properties getProperties() {
	return properties;
    }

    /**
     * Get the name of the repository. This is required only if the object in
     * JNDI is a {@link Repositories} instance.
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
     * Get the JCR username. This is not required, and if abscent implies that
     * no credentials should be used when obtaining a JCR Session.
     * 
     * @return the JCR username, or null if no username was specified
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

    void setUrl(String url) {
	this.url = url;
    }

    void setProperties(Properties properties) {
	this.properties = properties;
    }

    /**
     * Get the effective URL of this connection, which places all properties on
     * the URL (with a '*' for each character in the password property)
     * 
     * @return the effective URL; never null
     */
    public abstract String getEffectiveUrl();
    
    /**
     * Obtain the array of {@link DriverPropertyInfo} objects that describe the missing properties.
     * 
     * @return DriverPropertyInfo the property infos; never null but possibly empty
     */
    public abstract DriverPropertyInfo[] getPropertyInfos( ) ;

    /**
     * Return the credentials based on the username and password.
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