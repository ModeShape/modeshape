/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    static {
        try {
            DriverManager.registerDriver(new JcrDriver(null));
        } catch (SQLException e) {
            logger.error(JdbcI18n.driverErrorRegistering, e.getMessage());
        }
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
    protected JcrDriver( JcrContextFactory namingContextFactory ) {
        super(HttpRepositoryDelegate.FACTORY, new DriverInfo(JdbcI18n.driverName.text(), JdbcI18n.driverVendor.text(),
                                                             JdbcI18n.driverVendorUrl.text(), JdbcI18n.driverVersion.text()),
              namingContextFactory);
    }
}
