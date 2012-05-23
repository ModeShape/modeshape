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

import java.sql.DriverManager;
import java.sql.SQLException;
import javax.jcr.Repository;
import org.modeshape.jdbc.delegate.HttpRepositoryDelegate;

/**
 * A JDBC driver implementation that is able to access a JCR repository to query its contents using JCR-SQL2. <h3>Connection URLs</h3>
 * <p>
 * The driver accepts several URL formats based on how the repository is configured:
 * <ol>
 * <li>configured for <i>local</i> access using JNDI
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
 * The "repository" property is required only if the object in JNDI is a {@literal org.modeshape.jcr.api.Repositories} object. <br />
 * <br />
 * </p>
 * <li>configured for <i>remote</i> access using REST interface.
 * 
 * <pre>
 *     jdbc:jcr:http://{hostname}:{port}?{firstProperty}&amp;{secondProperty}&amp;...
 * </pre>
 * 
 * where
 * <ul>
 * <li><strong>{hostname}</strong> is the host name where the {@link Repository} or {@literal org.modeshape.jcr.api.Repositories}
 * instance can be found;</li>
 * <li><strong>{port}</strong> is the port to access the {@link Repository} or {@literal org.modeshape.jcr.api.Repositories} on
 * the specified <i>hostname</i>;</li>
 * <li><strong>{firstProperty}</strong> consists of the first property name followed by '=' followed by the property's value;</li>
 * <li><strong>{secondProperty}</strong> consists of the second property name followed by '=' followed by the property's value;</li>
 * </ul>
 * <p>
 * Note that any use of URL encoding ('%' followed by a two-digit hexadecimal value) will be decoded before being used.
 * </p>
 * </ol>
 */

public class JcrDriver extends LocalJcrDriver {

    /* URL Prefix used for remote access */
    public static final String HTTP_URL_PREFIX = "jdbc:jcr:http://";

    private static JcrDriver INSTANCE = new JcrDriver();

    static {
        try {
            DriverManager.registerDriver(INSTANCE);
        } catch (SQLException e) {
            logger.error(JdbcI18n.driverErrorRegistering, e.getMessage());
        }
    }

    public static JcrDriver getInstance() {
        return INSTANCE;
    }

    /**
     * No-arg constructor, required by the {@link DriverManager}.
     */
    public JcrDriver() {
        this(null);
    }

    /**
     * Create an instance of this driver using the supplied JNDI naming context factory. This is useful for testing, but is
     * otherwise not generally recommended.
     * 
     * @param namingContextFactory the naming context factory; may be null if one should be created automatically
     */
    public JcrDriver( JcrContextFactory namingContextFactory ) {
        super(HttpRepositoryDelegate.FACTORY, new DriverInfo(JdbcI18n.driverName.text(), JdbcI18n.driverVendor.text(),
                                                             JdbcI18n.driverVendorUrl.text(), JdbcI18n.driverVersion.text()),
              namingContextFactory);
    }
}
