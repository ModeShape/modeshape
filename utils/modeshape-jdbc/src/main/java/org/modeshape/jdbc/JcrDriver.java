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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import net.jcip.annotations.Immutable;
import org.modeshape.common.collection.Collections;
import org.modeshape.common.collection.UnmodifiableProperties;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.text.UrlEncoder;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.Repositories;

/**
 * A JDBC driver implementation that is able to access a JCR repository to query its contents using JCR-SQL2. <h3>Connection URLs</h3>
 * <p>
 * The driver accepts several URL format:
 * 
 * <pre>
 *     jdbc:jcr:jndi://{jndiName}
 * </pre>
 * 
 * or
 * 
 * <pre>
 *     jdbc:jcr:jndi://{jndiName}?{firstProperty}&amp;{secondProperty}&amp;...
 * </pre>
 * 
 * where
 * <ul>
 * <li><strong>{jndiName}</strong> is the JNDI name where the {@link Repository} or {@link Repositories} instance can be found;</li>
 * <li><strong>{firstProperty}</strong> consists of the first property name followed by '=' followed by the property's value;</li>
 * <li><strong>{secondProperty}</strong> consists of the second property name followed by '=' followed by the property's value;</li>
 * </ul>
 * Note that any use of URL encoding ('%' followed by a two-digit hexadecimal value) will be decoded before being used.
 * </p>
 * <p>
 * Here's an example of a URL that defines a {@link Repository} instance located at "<code>javax:MyJcrRepository</code>" with a
 * username of "jsmith", password of "secret", and workspace name of "My Workspace":
 * 
 * <pre>
 *     jdbc:jcr:jndi://javax%3AMyJcrRepository?username=jsmith&amp;password=secret&amp;workspace=My%20Workspace
 * </pre>
 * 
 * The "repository" property is required only if the object in JNDI is a {@link Repositories} object.
 * </p>
 */
public class JcrDriver implements java.sql.Driver {

    public static final String WORKSPACE_PROPERTY_NAME = "workspace";
    public static final String REPOSITORY_PROPERTY_NAME = "repository";
    public static final String USERNAME_PROPERTY_NAME = "username";
    public static final String PASSWORD_PROPERTY_NAME = "password";

    protected static final Set<String> ALL_PROPERTY_NAMES = Collections.unmodifiableSet(WORKSPACE_PROPERTY_NAME,
                                                                                        REPOSITORY_PROPERTY_NAME,
                                                                                        USERNAME_PROPERTY_NAME,
                                                                                        PASSWORD_PROPERTY_NAME);

    private static final String JNDI_URL_PREFIX = "jdbc:jcr:jndi://";

    private static final String EXAMPLE_DRIVER_URL = JNDI_URL_PREFIX + "{jndiName}";

    private static DriverMetadata driverMetadata;

    protected static final TextDecoder URL_DECODER = new UrlEncoder();

    private JndiContextFactory contextFactory = new JndiContextFactory() {
        public Context createContext( Properties properties ) throws NamingException {
            return properties == null || properties.isEmpty() ? new InitialContext() : new InitialContext(properties);
        }
    };

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
        if (url == null) return false;
        String trimmedUrl = url.trim();
        if (trimmedUrl.startsWith(JNDI_URL_PREFIX) && trimmedUrl.length() > JNDI_URL_PREFIX.length()) {
            // This fits the pattern so far ...
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Driver#getPropertyInfo(java.lang.String, java.util.Properties)
     */
    @Override
    public DriverPropertyInfo[] getPropertyInfo( String url,
                                                 Properties info ) /*throws SQLException*/{
        // Get the connection information ...
        ConnectionInfo connectionInfo = getConnectionInfo(url, info);
        if (connectionInfo == null) connectionInfo = new ConnectionInfo(url, info);
        return connectionInfo.getPropertyInfos(contextFactory);
    }

    /**
     * Get the information describing the connection. This method can be overridden to return a subclass of {@link ConnectionInfo}
     * that implements {@link ConnectionInfo#getCredentials()} for a specific JCR implementation.
     * 
     * @param url the JDBC URL
     * @param info the JDBC connection properties
     * @return the connection information, or null if the URL is null or not of the proper format
     */
    protected ConnectionInfo getConnectionInfo( String url,
                                                Properties info ) {
        if (url == null) return null;
        String trimmedUrl = url.trim();
        if (trimmedUrl.startsWith(JNDI_URL_PREFIX) && trimmedUrl.length() >= JNDI_URL_PREFIX.length()) {
            // Get the connection information ...
            return createConnectionInfo(trimmedUrl, info);
        }
        return null;
    }

    protected ConnectionInfo createConnectionInfo( String url,
                                                   Properties info ) {
        return new ConnectionInfo(url, info);
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

        // Get the connection information ...
        ConnectionInfo connectionInfo = getConnectionInfo(url, info);
        if (connectionInfo == null) {
            // This doesn't look like the right URL, so return null ...
            return null;
        }

        if (connectionInfo.getJndiName() == null) {
            String msg = JdbcI18n.urlMustContainJndiNameOfRepositoryOrRepositoriesObject.text();
            throw new SQLException(msg);
        }

        // Look up the object in JNDI and find the JCR Repository object ...
        String jndiName = connectionInfo.getJndiName();
        Context context = null;
        try {
            context = contextFactory.createContext(connectionInfo.getProperties());
        } catch (NamingException e) {
            throw new SQLException(JdbcI18n.unableToGetJndiContext.text(e.getLocalizedMessage()));
        }
        if (context == null) {
            throw new SQLException(JdbcI18n.unableToFindObjectInJndi.text(jndiName));
        }
        Repository repository = null;
        try {
            Object target = context.lookup(jndiName);
            if (target instanceof Repositories) {
                Repositories repositories = (Repositories)target;
                String repositoryName = connectionInfo.getRepositoryName();
                if (repositoryName == null) {
                    throw new SQLException(JdbcI18n.objectInJndiIsRepositories.text(jndiName));
                }
                try {
                    repository = repositories.getRepository(repositoryName);
                } catch (RepositoryException e) {
                    throw new SQLException(JdbcI18n.unableToFindNamedRepository.text(jndiName, repositoryName));
                }
            } else if (target instanceof Repository) {
                repository = (Repository)target;
            } else {
                throw new SQLException(JdbcI18n.objectInJndiMustBeRepositoryOrRepositories.text(jndiName));
            }
            assert repository != null;
        } catch (NamingException e) {
            throw new SQLException(JdbcI18n.unableToFindObjectInJndi.text(jndiName), e);
        }

        return new JcrConnection(repository, connectionInfo);
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

    @Immutable
    protected static class ConnectionInfo {
        private final String url;
        private final Properties properties;
        private final String jndiName;

        protected ConnectionInfo( String url,
                                  Properties properties ) {
            this.url = url != null ? url.trim() : null;
            String jndiName = null;
            Properties props = properties != null ? new Properties(properties) : new Properties();
            if (this.url != null) {
                assert this.url.startsWith(JNDI_URL_PREFIX);
                assert this.url.length() >= JNDI_URL_PREFIX.length();

                // Find the JNDI name ...
                jndiName = this.url.substring(JNDI_URL_PREFIX.length());

                // Find any URL parameters ...
                int questionMarkIndex = jndiName.indexOf('?');
                if (questionMarkIndex != -1) {
                    if (jndiName.length() > questionMarkIndex + 1) {
                        String paramStr = jndiName.substring(questionMarkIndex + 1);
                        for (String param : paramStr.split("&")) {
                            String[] pair = param.split("=");
                            if (pair.length > 1) {
                                String key = URL_DECODER.decode(pair[0] != null ? pair[0].trim() : null);
                                String value = URL_DECODER.decode(pair[1] != null ? pair[1].trim() : null);
                                if (!props.containsKey(key)) {
                                    props.put(key, value);
                                }
                            }
                        }
                    }
                    jndiName = jndiName.substring(0, questionMarkIndex).trim();
                }
            }
            this.jndiName = jndiName;
            this.properties = new UnmodifiableProperties(props);
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
         * Get the effective URL of this connection, which places all properties on the URL (with a '*' for each character in the
         * password property)
         * 
         * @return the effective URL; never null
         */
        public String getEffectiveUrl() {
            StringBuilder url = new StringBuilder(JNDI_URL_PREFIX);
            url.append(this.jndiName);
            char propertyDelim = '?';
            for (String propertyName : this.properties.stringPropertyNames()) {
                String value = this.properties.getProperty(propertyName);
                if (value == null) continue;
                if (PASSWORD_PROPERTY_NAME.equals(propertyName)) {
                    value = StringUtil.createString('*', value.length());
                }
                url.append(propertyDelim).append(propertyName).append('=').append(value);
                propertyDelim = '&';
            }
            return url.toString();
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
         * Get the name in JNDI of the {@link Repository} or {@link Repositories} object
         * 
         * @return the JNDI name; may be null if this connection information is invalid
         */
        public String getJndiName() {
            return jndiName;
        }

        /**
         * Get the name of the repository. This is required only if the object in JNDI is a {@link Repositories} instance.
         * 
         * @return the name of the repository, or null if no repository name was specified
         */
        public String getRepositoryName() {
            return properties.getProperty(REPOSITORY_PROPERTY_NAME);
        }

        /**
         * Get the name of the workspace. This is not required, and if abscent implies obtaining the JCR Repository's default
         * workspace.
         * 
         * @return the name of the workspace, or null if no workspace name was specified
         */
        public String getWorkspaceName() {
            return properties.getProperty(WORKSPACE_PROPERTY_NAME);
        }

        /**
         * Get the JCR username. This is not required, and if abscent implies that no credentials should be used when obtaining a
         * JCR Session.
         * 
         * @return the JCR username, or null if no username was specified
         */
        public String getUsername() {
            return properties.getProperty(USERNAME_PROPERTY_NAME);
        }

        /**
         * Get the JCR password. This is not required.
         * 
         * @return the JCR password, or null if no password was specified
         */
        public char[] getPassword() {
            String result = properties.getProperty(PASSWORD_PROPERTY_NAME);
            return result != null ? result.toCharArray() : null;
        }

        /**
         * Obtain the array of {@link DriverPropertyInfo} objects that describe the missing properties.
         * 
         * @param contextFactory the JNDI context factory; may be null if the object in JNDI should not be used to determine the
         *        property infos
         * @return the property infos; never null but possibly empty
         */
        public DriverPropertyInfo[] getPropertyInfos( JndiContextFactory contextFactory ) {
            List<DriverPropertyInfo> results = new ArrayList<DriverPropertyInfo>();
            if (url == null) {
                DriverPropertyInfo info = new DriverPropertyInfo(JdbcI18n.urlPropertyName.text(), null);
                info.description = JdbcI18n.urlPropertyDescription.text(EXAMPLE_DRIVER_URL);
                info.required = true;
                info.choices = new String[] {EXAMPLE_DRIVER_URL};
                results.add(info);
                contextFactory = null;
            }
            if (getUsername() == null) {
                DriverPropertyInfo info = new DriverPropertyInfo(JdbcI18n.usernamePropertyName.text(), null);
                info.description = JdbcI18n.usernamePropertyDescription.text();
                info.required = false;
                info.choices = null;
                results.add(info);
            }
            if (getPassword() == null) {
                DriverPropertyInfo info = new DriverPropertyInfo(JdbcI18n.passwordPropertyName.text(), null);
                info.description = JdbcI18n.passwordPropertyDescription.text();
                info.required = false;
                info.choices = null;
                results.add(info);
            }
            boolean nameRequired = false;
            if (getRepositoryName() == null) {
                boolean found = false;
                if (contextFactory != null) {
                    try {
                        Context context = contextFactory.createContext(properties);
                        Object obj = context.lookup(jndiName);
                        if (obj instanceof Repositories) {
                            nameRequired = true;
                            found = true;
                        } else if (obj instanceof Repository) {
                            found = true;
                        }
                    } catch (NamingException e) {
                        // do nothing about it ...
                    }
                }
                if (nameRequired || !found) {
                    DriverPropertyInfo info = new DriverPropertyInfo(JdbcI18n.repositoryNamePropertyName.text(), null);
                    info.description = JdbcI18n.repositoryNamePropertyDescription.text();
                    info.required = nameRequired;
                    info.choices = null;
                    results.add(info);
                }
            }
            if (getWorkspaceName() == null) {
                DriverPropertyInfo info = new DriverPropertyInfo(JdbcI18n.workspaceNamePropertyName.text(), null);
                info.description = JdbcI18n.workspaceNamePropertyDescription.text();
                info.required = false;
                info.choices = null;
                results.add(info);
            }
            return results.toArray(new DriverPropertyInfo[results.size()]);
        }

        public Credentials getCredentials() {
            String username = getUsername();
            char[] password = getPassword();
            if (username != null) {
                return new SimpleCredentials(username, password);
            }
            return null;
        }
    }

    static DriverMetadata getDriverMetadata() {
        if (driverMetadata == null) {
            driverMetadata = new DriverMetadata();
        }
        return driverMetadata;
    }

    @Immutable
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

    void setContextFactory( JndiContextFactory factory ) {
        assert factory != null;
        this.contextFactory = factory;
    }

    interface JndiContextFactory {
        Context createContext( Properties properties ) throws NamingException;
    }

}
