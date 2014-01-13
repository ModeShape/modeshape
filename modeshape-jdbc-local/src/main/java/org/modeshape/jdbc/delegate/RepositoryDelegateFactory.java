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
package org.modeshape.jdbc.delegate;

import java.sql.SQLException;
import java.util.Properties;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jdbc.JdbcLocalI18n;
import org.modeshape.jdbc.LocalJcrDriver;
import org.modeshape.jdbc.LocalJcrDriver.JcrContextFactory;

/**
 * The RepositoryDelegateFactory is used to create the required type of {@link RepositoryDelegate} based upon the <i>url</i>
 * provided. The <i>url</i> must be prefixed by {@value LocalJcrDriver#JNDI_URL_PREFIX}.
 */
public class RepositoryDelegateFactory {

    protected static final int PROTOCOL_UNKNOWN = -1;
    protected static final int PROTOCOL_JNDI = 1;

    protected RepositoryDelegateFactory() {
    }

    /**
     * Create a RepositoryDelegate instance, given the connection information.
     * 
     * @param url the JDBC URL; may be null
     * @param info the connection properties
     * @param contextFactory the factory for a JCR context; may not be null
     * @return the RepositoryDelegate for the supplied connection information
     * @throws SQLException if the URL is unknown
     */
    public RepositoryDelegate createRepositoryDelegate( String url,
                                                        Properties info,
                                                        JcrContextFactory contextFactory ) throws SQLException {
        if (!acceptUrl(url)) {
            throw new SQLException(JdbcLocalI18n.invalidUrlPrefix.text(LocalJcrDriver.JNDI_URL_PREFIX));
        }
        return create(determineProtocol(url), url, info, contextFactory);
    }

    /**
     * Determine if this factory accepts the supplied URL.
     * 
     * @param url the connection URL
     * @return true if this factory accepts the supplied URL, or false otherwise
     */
    public boolean acceptUrl( String url ) {
        return !StringUtil.isBlank(url) && determineProtocol(url.trim()) > 0;
    }

    protected int determineProtocol( String url ) {
        assert url != null;
        assert url.length() != 0;
        if (url.startsWith(LocalJcrDriver.JNDI_URL_PREFIX) && url.length() > LocalJcrDriver.JNDI_URL_PREFIX.length()) {
            // This fits the pattern so far ...
            return PROTOCOL_JNDI;
        }
        return PROTOCOL_UNKNOWN;
    }

    protected RepositoryDelegate create( int protocol,
                                         String url,
                                         Properties info,
                                         JcrContextFactory contextFactory ) {
        switch (protocol) {
            case PROTOCOL_JNDI:
                return new LocalRepositoryDelegate(url, info, contextFactory);
            default:
                throw new IllegalArgumentException("Invalid protocol: " + protocol);
        }
    }
}
