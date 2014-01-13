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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import org.junit.Test;
import org.modeshape.jdbc.LocalJcrDriver;

public class LocalRepositoryDelegateTest extends RepositoryDelegateFactoryTest {

    private static final String REPOSITORY_NAME = "repositoryName";

    private static final String USER_NAME = "jsmith";
    private static final String PASSWORD = "secret";
    private static final String WORKSPACE = "MyWorkspace";
    private static final String JNDINAME = "jcr/local";

    private static final String VALID_JNDI_URL = LocalJcrDriver.JNDI_URL_PREFIX + JNDINAME;

    private static final String INSUFFICIENT_JNDI_URL = LocalJcrDriver.JNDI_URL_PREFIX + "/";

    private static final String VALID_JNDI_URL_WITH_PARMS = VALID_JNDI_URL + "?workspace=" + WORKSPACE + "&user=" + USER_NAME
                                                            + "&password=" + PASSWORD + "&"
                                                            + LocalJcrDriver.REPOSITORY_PROPERTY_NAME + "=" + REPOSITORY_NAME;

    private RepositoryDelegate delegate;

    @Test
    public void testNoContextOverride() throws SQLException {
        delegate = factory().createRepositoryDelegate(VALID_JNDI_URL_WITH_PARMS, new Properties(), null);
    }

    @Test
    public void shouldCreateLocalRepositoryDelegate() throws SQLException {
        RepositoryDelegate delegate = factory().createRepositoryDelegate(VALID_JNDI_URL_WITH_PARMS, new Properties(), null);
        assertThat(delegate, instanceOf(LocalRepositoryDelegate.class));
    }

    @Test
    public void shouldAcceptValidURL() {
        assertThat(factory().acceptUrl(VALID_JNDI_URL_WITH_PARMS), is(true));
        assertThat(factory().acceptUrl(VALID_JNDI_URL), is(true));
    }

    @Test
    public void connectionInfoShouldBeValid() throws SQLException {
        delegate = factory().createRepositoryDelegate(VALID_JNDI_URL_WITH_PARMS, new Properties(), null);

        assertNotNull(delegate.getConnectionInfo());
        assertThat(delegate.getConnectionInfo().getUsername(), is(USER_NAME));
        assertThat(delegate.getConnectionInfo().getPassword(), is(new String(PASSWORD).toCharArray()));
        assertThat(delegate.getConnectionInfo().getWorkspaceName(), is(WORKSPACE));
        assertThat(delegate.getConnectionInfo().getRepositoryName(), is(REPOSITORY_NAME));

        assertThat(delegate.getConnectionInfo().getEffectiveUrl(),
                   is(LocalJcrDriver.JNDI_URL_PREFIX
                      + "jcr/local?user=jsmith&workspace=MyWorkspace&password=******&repositoryName=repositoryName"));

        DriverPropertyInfo[] infos = delegate.getConnectionInfo().getPropertyInfos();
        assertThat(infos.length, is(0));

        assertThat(((LocalRepositoryDelegate.JNDIConnectionInfo)delegate.getConnectionInfo()).getRepositoryPath(), is(JNDINAME));

        // System.out.println("URL: " delegate.getConnectionInfo().getUrl());

    }

    @Test
    public void connectionPropertyInfoShouldIndicateMissingData() throws SQLException {
        delegate = factory().createRepositoryDelegate(INSUFFICIENT_JNDI_URL, new Properties(), null);
        assertNotNull(delegate.getConnectionInfo());
        DriverPropertyInfo[] infos = delegate.getConnectionInfo().getPropertyInfos();
        assertThat(infos.length, is(4));
    }

    @Test
    public void shouldReturnEmptyPropertyInfosWhenSuppliedValidUrlAndAllPropertiesWithRepositoriesInJndi() throws SQLException {
        Properties validProperties = new Properties();
        validProperties.put(LocalJcrDriver.WORKSPACE_PROPERTY_NAME, WORKSPACE);
        validProperties.put(LocalJcrDriver.USERNAME_PROPERTY_NAME, USER_NAME);
        validProperties.put(LocalJcrDriver.PASSWORD_PROPERTY_NAME, PASSWORD);
        validProperties.put(LocalJcrDriver.REPOSITORY_PROPERTY_NAME, REPOSITORY_NAME);

        delegate = factory().createRepositoryDelegate(VALID_JNDI_URL, validProperties, null);
        DriverPropertyInfo[] infos = delegate.getConnectionInfo().getPropertyInfos();
        assertThat(infos.length, is(0));
    }

}
