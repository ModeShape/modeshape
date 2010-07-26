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
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;

import org.modeshape.jdbc.JcrDriver;
import org.modeshape.jdbc.JdbcI18n;
import org.modeshape.jdbc.JcrDriver.JcrContextFactory;

/**
 * The RepositoryDelegateFactory is used to create the required type of {@link RepositoryDelegate} based upon the <i>url</i> provided.  
 * The <i>url</i> must be prefixed by either {@value JcrDriver#JNDI_URL_PREFIX} or {@value JcrDriver#HTTP_URL_PREFIX}.
 * 
 */
public class RepositoryDelegateFactory {
 
    
    private static final int JNDI_URL_OPTION = 1;
    private static final int HTTP_URL_OPTION = 2; 
    private static final int FILE_URL_OPTION = 3;

    public static RepositoryDelegate createRepositoryDelegate(String url, Properties info, JcrContextFactory contextFactory) throws SQLException {
	if (! acceptUrl(url)) {
	    throw new SQLException(JdbcI18n.invalidUrlPrefix
		    .text(JcrDriver.JNDI_URL_PREFIX, JcrDriver.HTTP_URL_PREFIX));
	}
	RepositoryDelegate jcri = create(url, info, contextFactory);
	return jcri;
    }

    
    public static boolean acceptUrl(String url) {
	return ( getUrlOption(url) > 0 ? true : false);
    }
    
    
    private static int  getUrlOption(String url){
        if (url == null || url.trim().length() == 0) return -1;
        String trimmedUrl = url.trim();
        if (trimmedUrl.startsWith(JcrDriver.JNDI_URL_PREFIX) && trimmedUrl.length() > JcrDriver.JNDI_URL_PREFIX.length()) {
            // This fits the pattern so far ...
            return JNDI_URL_OPTION;
        }
        if (trimmedUrl.startsWith(JcrDriver.HTTP_URL_PREFIX) && trimmedUrl.length() > JcrDriver.HTTP_URL_PREFIX.length()) {
            // This fits the pattern so far ...
            return HTTP_URL_OPTION;
        }
        if (trimmedUrl.startsWith(JcrDriver.FILE_URL_PREFIX) && trimmedUrl.length() > JcrDriver.FILE_URL_PREFIX.length()) {
            // This fits the pattern so far ...
            return FILE_URL_OPTION;
        }

        return -1;
    }
    
    private static RepositoryDelegate create(String url, Properties info, JcrContextFactory contextFactory) throws SQLException {
	
	switch (getUrlOption(url)) {
	case JNDI_URL_OPTION:
	    return new LocalRepositoryDelegate(url, info, contextFactory);

	    
	case HTTP_URL_OPTION:
	    throw new SQLFeatureNotSupportedException();
//	    return new HttpRepositoryDelegate(url, info, contextFactory );

	case FILE_URL_OPTION:
			return new FileRepositoryDelegate(url, info);
		
	default:
	    return null;
	}
    }
}
