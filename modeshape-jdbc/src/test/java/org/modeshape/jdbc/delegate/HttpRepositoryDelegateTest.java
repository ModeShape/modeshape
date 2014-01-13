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
public class HttpRepositoryDelegateTest extends RepositoryDelegateFactoryTest {

    private static final String REPOSITORY_NAME = "repositoryName";

    private static final String USER_NAME = "jsmith";
    private static final String PASSWORD = "secret";
    private static final String WORKSPACE = "MyWorkspace";
    private static final String SERVER = "serverName:8080";
    private static final String INVALID_URL = JcrDriver.HTTP_URL_PREFIX + SERVER;

    private static final String VALID_HTTP_URL = JcrDriver.HTTP_URL_PREFIX + SERVER + "/modeshape-rest";

    private static final String VALID_HTTP_URL_WITH_PARMS = VALID_HTTP_URL + "/" + REPOSITORY_NAME + "/" + WORKSPACE + "?user="
                                                            + USER_NAME + "&password=" + PASSWORD + "&"
                                                            + JcrDriver.TEIID_SUPPORT_PROPERTY_NAME + "=true";

    private RepositoryDelegate delegate;

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jdbc.delegate.RepositoryDelegateFactoryTest#factory()
     */
    @Override
    protected RepositoryDelegateFactory factory() {
        return HttpRepositoryDelegate.FACTORY;
    }

    @Test
    public void testNoContextOverride() throws SQLException {
        delegate = factory().createRepositoryDelegate(VALID_HTTP_URL_WITH_PARMS, new Properties(), null);
    }

    @Test
    public void connectionInfoShouldBeValid() throws SQLException {
        delegate = factory().createRepositoryDelegate(VALID_HTTP_URL_WITH_PARMS, new Properties(), null);

        assertNotNull(delegate.getConnectionInfo());
        assertThat(delegate.getConnectionInfo().getUsername(), is(USER_NAME));
        assertThat(delegate.getConnectionInfo().getPassword(), is(new String(PASSWORD).toCharArray()));

        assertThat(delegate.getConnectionInfo().getEffectiveUrl(),
                   is(VALID_HTTP_URL
                      + "?teiidsupport=true&user=jsmith&workspace=MyWorkspace&password=******&repositoryName=repositoryName"));

        assertThat(delegate.getConnectionInfo().isTeiidSupport(), is(Boolean.TRUE.booleanValue()));

        DriverPropertyInfo[] infos = delegate.getConnectionInfo().getPropertyInfos();
        assertThat(infos.length, is(0));

        assertThat((delegate.getConnectionInfo()).getRepositoryPath(), is(SERVER + "/modeshape-rest"));

        // System.out.println("URL: " + delegate.getConnectionInfo().getUrl());

    }

    @Test
    public void connectionPropertyInfoShouldIndicateMissingData() throws SQLException {
        delegate = factory().createRepositoryDelegate(INVALID_URL, new Properties(), null);

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

        delegate = factory().createRepositoryDelegate(VALID_HTTP_URL, validProperties, null);
        DriverPropertyInfo[] infos = delegate.getConnectionInfo().getPropertyInfos();
        assertThat(infos.length, is(0));
    }

}
