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
public class LocalRepositoryDelegateTest {

    private static final String REPOSITORY_NAME = "repositoryName";

    private static final String USER_NAME = "jsmith";
    private static final String PASSWORD = "secret";
    private static final String WORKSPACE = "MyWorkspace";
    private static final String JNDINAME = "jcr/local";
    private static final String INVALID_URL = JcrDriver.JNDI_URL_PREFIX + "notExist";

    private static final String VALID_JNDI_URL = JcrDriver.JNDI_URL_PREFIX + JNDINAME;

    private static final String VALID_JNDI_URL_WITH_PARMS = VALID_JNDI_URL + "?workspace=" + WORKSPACE + "&user=" + USER_NAME
                                                            + "&password=" + PASSWORD + "&" + JcrDriver.REPOSITORY_PROPERTY_NAME
                                                            + "=" + REPOSITORY_NAME + "&" + JcrDriver.TEIID_SUPPORT_PROPERTY_NAME 
                                                            + "=true";

    private RepositoryDelegate delegate;

    @Test
    public void testNoContextOverride() throws SQLException {
        delegate = RepositoryDelegateFactory.createRepositoryDelegate(VALID_JNDI_URL_WITH_PARMS, new Properties(), null);
    }

    @Test
    public void connectionInfoShouldBeValid() throws SQLException {
        delegate = RepositoryDelegateFactory.createRepositoryDelegate(VALID_JNDI_URL_WITH_PARMS, new Properties(), null);

        assertNotNull(delegate.getConnectionInfo());
        assertThat(delegate.getConnectionInfo().getUsername(), is(USER_NAME));
        assertThat(delegate.getConnectionInfo().getPassword(), is(new String(PASSWORD).toCharArray()));
        assertThat(delegate.getConnectionInfo().getWorkspaceName(), is(WORKSPACE));
        assertThat(delegate.getConnectionInfo().getRepositoryName(), is(REPOSITORY_NAME));

        assertThat(delegate.getConnectionInfo().getEffectiveUrl(),
                   is(JcrDriver.JNDI_URL_PREFIX
                      + "jcr/local?teiidsupport=true&user=jsmith&workspace=MyWorkspace&password=******&repositoryName=repositoryName"));
        
        assertThat(delegate.getConnectionInfo().isTeiidSupport(), is(Boolean.TRUE.booleanValue()));

        DriverPropertyInfo[] infos = delegate.getConnectionInfo().getPropertyInfos();
        assertThat(infos.length, is(0));

        assertThat(((LocalRepositoryDelegate.JNDIConnectionInfo)delegate.getConnectionInfo()).getRepositoryPath(), is(JNDINAME));

        // System.out.println("URL: " + delegate.getConnectionInfo().getUrl());

    }

    @Test
    public void connectionPropertyInfoShouldIndicateMissingData() throws SQLException {
        delegate = RepositoryDelegateFactory.createRepositoryDelegate(INVALID_URL, new Properties(), null);

        assertNotNull(delegate.getConnectionInfo());

        DriverPropertyInfo[] infos = delegate.getConnectionInfo().getPropertyInfos();
        assertThat(infos.length, is(4));

    }

    @Test
    public void shouldReturnEmptyPropertyInfosWhenSuppliedValidUrlAndAllPropertiesWithRepositoriesInJndi() throws SQLException {
        Properties validProperties = new Properties();
        validProperties.put(JcrDriver.WORKSPACE_PROPERTY_NAME, WORKSPACE);
        validProperties.put(JcrDriver.USERNAME_PROPERTY_NAME, USER_NAME);
        validProperties.put(JcrDriver.PASSWORD_PROPERTY_NAME, PASSWORD);
        validProperties.put(JcrDriver.REPOSITORY_PROPERTY_NAME, REPOSITORY_NAME);

        delegate = RepositoryDelegateFactory.createRepositoryDelegate(VALID_JNDI_URL, validProperties, null);
        DriverPropertyInfo[] infos = delegate.getConnectionInfo().getPropertyInfos();
        assertThat(infos.length, is(0));
    }

}
