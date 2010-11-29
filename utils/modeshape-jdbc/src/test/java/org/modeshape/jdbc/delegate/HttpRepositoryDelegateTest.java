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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import org.junit.Test;
import org.modeshape.jdbc.JcrDriver;

/**
 * 
 */
public class HttpRepositoryDelegateTest {

    private static final String REPOSITORY_NAME = "repositoryName";

    private static final String USER_NAME = "jsmith";
    private static final String PASSWORD = "secret";
    private static final String WORKSPACE = "MyWorkspace";
    private static final String SERVER = "serverName:8080";
    private static final String INVALID_URL = JcrDriver.HTTP_URL_PREFIX + SERVER;

    private static final String VALID_HTTP_URL = JcrDriver.HTTP_URL_PREFIX + SERVER + "/modeshape-rest";

    private static final String VALID_HTTP_URL_WITH_PARMS = VALID_HTTP_URL + "/" + REPOSITORY_NAME + "/" + WORKSPACE + "?user="
                                                            + USER_NAME + "&password=" + PASSWORD + "&" + JcrDriver.TEIID_SUPPORT_PROPERTY_NAME 
                                                            + "=true";

    private RepositoryDelegate delegate;

    @Test
    public void testNoContextOverride() throws SQLException {
        delegate = RepositoryDelegateFactory.createRepositoryDelegate(VALID_HTTP_URL_WITH_PARMS, new Properties(), null);
    }

    @Test
    public void connectionInfoShouldBeValid() throws SQLException {
        delegate = RepositoryDelegateFactory.createRepositoryDelegate(VALID_HTTP_URL_WITH_PARMS, new Properties(), null);

        assertNotNull(delegate.getConnectionInfo());
        assertThat(delegate.getConnectionInfo().getUsername(), is(USER_NAME));
        assertThat(delegate.getConnectionInfo().getPassword(), is(new String(PASSWORD).toCharArray()));

        assertThat(delegate.getConnectionInfo().getEffectiveUrl(),
                   is(VALID_HTTP_URL + "?teiidsupport=true&user=jsmith&workspace=MyWorkspace&password=******&repositoryName=repositoryName"));

        assertThat(delegate.getConnectionInfo().isTeiidSupport(), is(Boolean.TRUE.booleanValue()));

        DriverPropertyInfo[] infos = delegate.getConnectionInfo().getPropertyInfos();
        assertThat(infos.length, is(0));

        assertThat((delegate.getConnectionInfo()).getRepositoryPath(), is(SERVER + "/modeshape-rest"));

        // System.out.println("URL: " + delegate.getConnectionInfo().getUrl());

    }

    @Test
    public void connectionPropertyInfoShouldIndicateMissingData() throws SQLException {
        delegate = RepositoryDelegateFactory.createRepositoryDelegate(INVALID_URL, new Properties(), null);

        assertNotNull(delegate.getConnectionInfo());

        DriverPropertyInfo[] infos = delegate.getConnectionInfo().getPropertyInfos();
        assertThat(infos.length, is(5));
    }

    @Test
    public void shouldReturnEmptyPropertyInfosWhenSuppliedValidUrlAndAllPropertiesWithRepositoriesInHTTP() throws SQLException {
        Properties validProperties = new Properties();
        validProperties.put(JcrDriver.WORKSPACE_PROPERTY_NAME, WORKSPACE);
        validProperties.put(JcrDriver.USERNAME_PROPERTY_NAME, USER_NAME);
        validProperties.put(JcrDriver.PASSWORD_PROPERTY_NAME, PASSWORD);
        validProperties.put(JcrDriver.REPOSITORY_PROPERTY_NAME, REPOSITORY_NAME);

        delegate = RepositoryDelegateFactory.createRepositoryDelegate(VALID_HTTP_URL, validProperties, null);
        DriverPropertyInfo[] infos = delegate.getConnectionInfo().getPropertyInfos();
        assertThat(infos.length, is(0));
    }

}
