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
