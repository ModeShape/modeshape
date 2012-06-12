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
import javax.jcr.Repository;
import javax.naming.Context;
import javax.naming.NamingException;
import org.modeshape.common.collection.Collections;
import org.modeshape.common.logging.Logger;
import org.modeshape.jdbc.delegate.ConnectionInfo;
import org.modeshape.jdbc.delegate.LocalRepositoryDelegate;
import org.modeshape.jdbc.delegate.RepositoryDelegate;
import org.modeshape.jdbc.delegate.RepositoryDelegateFactory;

/**
 * A JDBC driver implementation that is able to access a JCR repository to query its contents using JCR-SQL2. <h3>Connection URLs</h3>
 * <p>
 * The driver accepts the following URL format for accessing a <i>local</i> JCR repository using JNDI:
 * 
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
 * <li><strong>{jndiName}</strong> is the JNDI name where the {@link Repository} or {@literal org.modeshape.jcr.api.Repositories}
 * instance can be found;</li>
 * <li><strong>{firstProperty}</strong> consists of the first property name followed by '=' followed by the property's value;</li>
 * <li><strong>{secondProperty}</strong> consists of the second property name followed by '=' followed by the property's value;</li>
 * </ul>
 * Note that any use of URL encoding ('%' followed by a two-digit hexadecimal value) will be decoded before being used.
 * </p>
 * <p>
 * Here's an example of a URL that defines a {@link Repository} instance located at "<code>jcr/local</code>" with a repository
 * name of "repository" and a user, password of "secret", and workspace name of "My Workspace":
 * 
 * <pre>
 *     jdbc:jcr:jndi:jcr/local?repositoryName=repository&user=jsmith&amp;password=secret&amp;workspace=My%20Workspace
 * </pre>
 * 
 * The "repository" property is required only if the object in JNDI is a {@literal org.modeshape.jcr.api.Repositories} object.
 * </p>
 * <p>
 * Note that any use of URL encoding ('%' followed by a two-digit hexadecimal value) will be decoded before being used.
 * </p>
 */

public class LocalJcrDriver implements java.sql.Driver {
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

    private static LocalJcrDriver INSTANCE = new LocalJcrDriver();

    static {
        try {
            DriverManager.registerDriver(INSTANCE);
        } catch (SQLException e) {
            // Logging
            logger.error(JdbcLocalI18n.driverErrorRegistering, e.getMessage());
        }
    }

    private final JcrContextFactory contextFactory;
    private final RepositoryDelegateFactory delegateFactory;
    private final DriverInfo driverInfo;

    /**
     * No-arg constructor, required by the {@link DriverManager}.
     */
    public LocalJcrDriver() {
        this(null);
    }

    /**
     * Create an instance of this driver using the supplied JNDI naming context factory. This is useful for testing, but is
     * otherwise not generally recommended.
     * 
     * @param namingContextFactory the naming context factory; may be null if one should be created automatically
     */
    protected LocalJcrDriver( JcrContextFactory namingContextFactory ) {
        this(LocalRepositoryDelegate.FACTORY, new DriverInfo(JdbcLocalI18n.driverName.text(), JdbcLocalI18n.driverVendor.text(),
                                                             JdbcLocalI18n.driverVendorUrl.text(),
                                                             JdbcLocalI18n.driverVersion.text()), namingContextFactory);
    }

    /**
     * Constructor for subclasses, that should be called by subclasses no-arg constructor.
     * 
     * @param delegateFactory the factory that should be used to create {@link RepositoryDelegate} instances; may not be null
     * @param driverInfo the information about the driver; may not be null
     * @param namingContextFactory the naming context factory; may be null if one should be created automatically
     */
    protected LocalJcrDriver( RepositoryDelegateFactory delegateFactory,
                              DriverInfo driverInfo,
                              JcrContextFactory namingContextFactory ) {
        assert delegateFactory != null;
        assert driverInfo != null;
        this.delegateFactory = delegateFactory;
        this.driverInfo = driverInfo;
        this.contextFactory = namingContextFactory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Driver#acceptsURL(java.lang.String)
     */
    @Override
    public boolean acceptsURL( String url ) {
        return delegateFactory.acceptUrl(url);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Driver#getPropertyInfo(java.lang.String, java.util.Properties)
     */
    @Override
    public DriverPropertyInfo[] getPropertyInfo( String url,
                                                 Properties info ) throws SQLException {
        // Get the connection information ...
        return delegateFactory.createRepositoryDelegate(url, info, this.contextFactory).getConnectionInfo().getPropertyInfos();
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
        return delegateFactory.createRepositoryDelegate(url, info, this.contextFactory).getConnectionInfo();
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

        return delegateFactory.createRepositoryDelegate(url, info, this.contextFactory).createConnection(getDriverInfo());
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Driver#getMajorVersion()
     */
    @Override
    public int getMajorVersion() {
        return getDriverInfo().getMajorVersion();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Driver#getMinorVersion()
     */
    @Override
    public int getMinorVersion() {
        return getDriverInfo().getMinorVersion();
    }

    public String getVendorName() {
        return getDriverInfo().getVendorName();
    }

    public String getVendorUrl() {
        return getDriverInfo().getVendorUrl();
    }

    public String getVersion() {
        return getDriverInfo().getVersion();
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

    protected final DriverInfo getDriverInfo() {
        return driverInfo;
    }

    /**
     * And interface that can be passed to this driver's contructor to create the JNDI naming context given the set of connection
     * properties.
     */
    public interface JcrContextFactory {
        /**
         * Create a JNDI naming context from the supplied connection properties.
         * 
         * @param properties the connection properties; may be null or empty
         * @return the naming context; may not be null
         * @throws NamingException if there is a problem creating or obtaining the naming context
         */
        Context createContext( Properties properties ) throws NamingException;
    }

}
