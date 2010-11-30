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
package org.modeshape.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jcr.Repository;
import javax.naming.Context;
import javax.naming.NamingException;

import org.modeshape.jdbc.delegate.ConnectionInfo;
import org.modeshape.jdbc.delegate.RepositoryDelegateFactory;
import org.modeshape.jdbc.util.Collections;

/**
 * A JDBC driver implementation that is able to access a JCR repository to query its contents using JCR-SQL2. <h3>Connection URLs</h3>
 * <p>
 * The driver accepts several URL formats based on how the repository is configured:
 * 
 * <ol>
 * <li>configured for <i>local</i> access using JNDI 
 * <pre>
 *     jdbc:jcr:jndi:{jndiName}
 * </pre>
 * 
 * or
 * 
 * <pre>
 *     jdbc:jcr:jndi:{jndiName}?{firstProperty}&amp;{secondProperty}&amp;...
 * </pre>
 * 
 * where
 * <ul>
 * <li><strong>{jndiName}</strong> is the JNDI name where the {@link Repository} or  {@literal org.modeshape.jcr.api.Repositories} instance can be found;</li>
 * <li><strong>{firstProperty}</strong> consists of the first property name followed by '=' followed by the property's value;</li>
 * <li><strong>{secondProperty}</strong> consists of the second property name followed by '=' followed by the property's value;</li>
 * </ul>
 * Note that any use of URL encoding ('%' followed by a two-digit hexadecimal value) will be decoded before being used.
 * </p>
 * <p>
 * Here's an example of a URL that defines a {@link Repository} instance located at "<code>jcr/local</code>" with a 
 * repository name of "repository" and a user, password of "secret", and workspace name of "My Workspace":
 * 
 * <pre>
 *     jdbc:jcr:jndi:jcr/local?repositoryName=repository&user=jsmith&amp;password=secret&amp;workspace=My%20Workspace
 * </pre>
 * 
 * The "repository" property is required only if the object in JNDI is a {@literal org.modeshape.jcr.api.Repositories} object.
 * <br /><br />
 * <li>configured for <i>remote</i> access using REST interface.
 * <pre>
 *     jdbc:jcr:http://{hostname}:{port}?{firstProperty}&amp;{secondProperty}&amp;...
 * </pre>
 * where
 * <ul>
 * <li><strong>{hostname}</strong> is the host name where the {@link Repository} or {@literal org.modeshape.jcr.api.Repositories} instance can be found;</li>
 * <li><strong>{port}</strong> is the port to access the {@link Repository} or {@literal org.modeshape.jcr.api.Repositories} on the specified <i>hostname</i>;</li>
 * <li><strong>{firstProperty}</strong> consists of the first property name followed by '=' followed by the property's value;</li>
 * <li><strong>{secondProperty}</strong> consists of the second property name followed by '=' followed by the property's value;</li>
 * </ul>
 * Note that any use of URL encoding ('%' followed by a two-digit hexadecimal value) will be decoded before being used.
 * </p>
 * Note that any use of URL encoding ('%' followed by a two-digit hexadecimal value) will be decoded before being used.
 *
 */

public class JcrDriver implements java.sql.Driver {
	protected static Logger logger = Logger.getLogger("org.modeshape.jdbc"); //$NON-NLS-1$

    public static final String WORKSPACE_PROPERTY_NAME = "workspace";
    public static final String REPOSITORY_PROPERTY_NAME = "repositoryName";
    public static final String USERNAME_PROPERTY_NAME = "user";
    public static final String PASSWORD_PROPERTY_NAME = "password";
    public static final String TEIID_SUPPORT_PROPERTY_NAME = "teiidsupport";

    protected static final Set<String> ALL_PROPERTY_NAMES = Collections.unmodifiableSet(WORKSPACE_PROPERTY_NAME,
                                                                                        REPOSITORY_PROPERTY_NAME,
                                                                                        USERNAME_PROPERTY_NAME,
                                                                                        PASSWORD_PROPERTY_NAME,
                                                                                        TEIID_SUPPORT_PROPERTY_NAME);

    /* URL Prefix used for JNDI access */
    public static final String JNDI_URL_PREFIX = "jdbc:jcr:jndi:";
    /* URL Prefix used for remote access */ 
    public static final String HTTP_URL_PREFIX = "jdbc:jcr:http://";

 
    private static DriverMetadata driverMetadata;

    public JcrContextFactory contextFactory = null;
    
    private static JcrDriver INSTANCE = new JcrDriver();
    
    static {
        try {
            DriverManager.registerDriver(INSTANCE);
        } catch(SQLException e) {
            // Logging
            String logMsg = JdbcI18n.driverErrorRegistering.text(e.getMessage()); //$NON-NLS-1$
            logger.log(Level.SEVERE, logMsg);
        }
    }
    
    public static JcrDriver getInstance() {
        return INSTANCE;
    }
	

    /**
     * No-arg constructor, required by the {@link DriverManager}.
     */
    public JcrDriver() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Driver#acceptsURL(java.lang.String)
     */
    @Override
    public boolean acceptsURL( String url ) {
	return RepositoryDelegateFactory.acceptUrl(url);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Driver#getPropertyInfo(java.lang.String, java.util.Properties)
     */
    @Override
    public DriverPropertyInfo[] getPropertyInfo( String url,
                                                 Properties info ) throws SQLException{
        // Get the connection information ...
	return RepositoryDelegateFactory.createRepositoryDelegate(url, info, this.contextFactory).getConnectionInfo().getPropertyInfos();
    }

    /**
     * Get the information describing the connection. This method can be overridden to return a subclass of {@link ConnectionInfo}
     * that implements {@link ConnectionInfo#getCredentials()} for a specific JCR implementation.
     * 
     * @param url the JDBC URL
     * @param info the JDBC connection properties
     * @return the connection information, or null if the URL is null or not of the proper format
     * @throws SQLException 
     */
    protected ConnectionInfo createConnectionInfo( String url,
                                                   Properties info ) throws SQLException {
        return RepositoryDelegateFactory.createRepositoryDelegate(url, info, this.contextFactory).getConnectionInfo();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that if the supplied properties and URL contain properties with the same name, the value from the supplied Properties
     * object will take precedence.
     * </p>
     * 
     * @see java.sql.Driver#connect(java.lang.String, java.util.Properties)
     */
    @Override
    public Connection connect( String url,
                               Properties info ) throws SQLException {
	
	return RepositoryDelegateFactory.createRepositoryDelegate(url, info, this.contextFactory).createConnection();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Driver#getMajorVersion()
     */
    @Override
    public int getMajorVersion() {
        return getDriverMetadata().getMajorVersion();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Driver#getMinorVersion()
     */
    @Override
    public int getMinorVersion() {
        return getDriverMetadata().getMinorVersion();
    }

    public String getVendorName() {
        return getDriverMetadata().getVendorName();
    }

    public String getVendorUrl() {
        return getDriverMetadata().getVendorUrl();
    }

    public String getVersion() {
        return getDriverMetadata().getVersion();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Driver#jdbcCompliant()
     */
    @Override
    public boolean jdbcCompliant() {
        return false;
    }
    

    static DriverMetadata getDriverMetadata() {
        if (driverMetadata == null) {
            driverMetadata = new DriverMetadata();
        }
        return driverMetadata;
    }

    static class DriverMetadata {
        private final int major;
        private final int minor;

        protected DriverMetadata() {
            String[] coords = getVersion().split("[.-]");
            this.major = Integer.parseInt(coords[0]);
            this.minor = Integer.parseInt(coords[1]);
        }

        public String getVendorName() {
            return JdbcI18n.driverVendor.text();
        }

        public String getVendorUrl() {
            return JdbcI18n.driverVendorUrl.text();
        }

        public String getVersion() {
            return JdbcI18n.driverVersion.text();
        }

        public int getMajorVersion() {
            return major;
        }

        public int getMinorVersion() {
            return minor;
        }

        public String getName() {
            return JdbcI18n.driverName.text();
        }
    }

    public void setContextFactory( JcrContextFactory factory ) {
        assert factory != null;
        this.contextFactory = factory;
    }
    
    public interface JcrContextFactory {
        Context createContext( Properties properties ) throws NamingException;
    }
    
 

}
