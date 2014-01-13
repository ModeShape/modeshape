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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import javax.jcr.Repository;
import javax.naming.Context;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modeshape.jcr.api.Repositories;
import org.modeshape.jdbc.delegate.ConnectionInfo;

/**
 * 
 */
public class JcrDriverTest {

    private LocalJcrDriver driver;
    private String jndiNameForRepository;
    private String jndiNameForRepositories;
    private String validUrl;
    private Properties validProperties;
    private String validRepositoryName;

    @Mock
    private Context jndi;
    @Mock
    private Repository repository;
    @Mock
    private Repositories repositories;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        validRepositoryName = "MyRepository";
        jndiNameForRepository = "java:MyRepository";
        jndiNameForRepositories = "java:Repositories";
        validUrl = LocalJcrDriver.JNDI_URL_PREFIX + jndiNameForRepository + "?workspace=MyWorkspace&user=jsmith&password=secret";
        validProperties = new Properties();

        when(jndi.lookup(jndiNameForRepository)).thenReturn(repository);
        when(jndi.lookup(jndiNameForRepositories)).thenReturn(repositories);
        when(repositories.getRepository(validRepositoryName)).thenReturn(repository);

        LocalJcrDriver.JcrContextFactory contextFactory = new LocalJcrDriver.JcrContextFactory() {
            @SuppressWarnings( "synthetic-access" )
            @Override
            public Context createContext( Properties properties ) {
                return jndi;
            }
        };

        driver = new LocalJcrDriver(contextFactory);
    }

    @After
    public void afterEach() throws Exception {
        try {
            DriverManager.deregisterDriver(driver);
        } finally {
            jndi = null;
            repositories = null;
            driver = null;
        }
    }

    @Test
    public void shouldNotBeJdbcCompliant() {
        assertThat(driver.jdbcCompliant(), is(false));
    }

    @Test
    public void shouldHaveMajorVersion() {
        assertThat(driver.getMajorVersion(), is(TestUtil.majorVersion(JdbcLocalI18n.driverVersion.text())));
    }

    @Test
    public void shouldHaveMinorVersion() {
        assertThat(driver.getMinorVersion(), is(TestUtil.minorVersion(JdbcLocalI18n.driverVersion.text())));
    }

    @Test
    public void shouldHaveVendorUrl() {
        assertThat(driver.getVendorUrl(), is(JdbcLocalI18n.driverVendorUrl.text()));
    }

    @Test
    public void shouldHaveVendorName() {
        assertThat(driver.getVendorName(), is(JdbcLocalI18n.driverVendor.text()));
    }

    @Test
    public void shouldHaveVersion() {
        assertThat(driver.getVersion(), is(JdbcLocalI18n.driverVersion.text()));
    }

    @Test
    public void shouldReturnEmptyPropertyInfosWhenSuppliedValidAndCompleteUrlAndNoProperties() throws SQLException {
        DriverPropertyInfo[] infos = driver.getPropertyInfo(validUrl, validProperties);
        assertThat(infos.length, is(0));
    }

    @Test
    public void shouldReturnEmptyPropertyInfosWhenSuppliedValidUrlAndAllPropertiesWithRepositoryInJndi() throws SQLException {
        validUrl = LocalJcrDriver.JNDI_URL_PREFIX + jndiNameForRepository;
        validProperties.put(LocalJcrDriver.WORKSPACE_PROPERTY_NAME, "MyWorkspace");
        validProperties.put(LocalJcrDriver.USERNAME_PROPERTY_NAME, "jsmith");
        validProperties.put(LocalJcrDriver.PASSWORD_PROPERTY_NAME, "secret");
        validProperties.put(LocalJcrDriver.REPOSITORY_PROPERTY_NAME, validRepositoryName);
        DriverPropertyInfo[] infos = driver.getPropertyInfo(validUrl, validProperties);
        assertThat(infos.length, is(0));
    }

    @Test
    public void shouldReturnEmptyPropertyInfosWhenSuppliedValidUrlAndAllPropertiesWithRepositoriesInJndi() throws SQLException {
        validUrl = LocalJcrDriver.JNDI_URL_PREFIX + jndiNameForRepositories;
        validProperties.put(LocalJcrDriver.WORKSPACE_PROPERTY_NAME, "MyWorkspace");
        validProperties.put(LocalJcrDriver.USERNAME_PROPERTY_NAME, "jsmith");
        validProperties.put(LocalJcrDriver.PASSWORD_PROPERTY_NAME, "secret");
        validProperties.put(LocalJcrDriver.REPOSITORY_PROPERTY_NAME, validRepositoryName);
        DriverPropertyInfo[] infos = driver.getPropertyInfo(validUrl, validProperties);
        assertThat(infos.length, is(0));
    }

    @Test
    public void shouldReturnRepositoryPropertyInfoWhenMissingRequiredRepositoryName() throws SQLException {
        validUrl = LocalJcrDriver.JNDI_URL_PREFIX + jndiNameForRepositories;
        validProperties.put(LocalJcrDriver.WORKSPACE_PROPERTY_NAME, "MyWorkspace");
        validProperties.put(LocalJcrDriver.USERNAME_PROPERTY_NAME, "jsmith");
        validProperties.put(LocalJcrDriver.PASSWORD_PROPERTY_NAME, "secret");
        // validProperties.put(JdbcDriver.REPOSITORY_PROPERTY_NAME, validRepositoryName);
        DriverPropertyInfo[] infos = driver.getPropertyInfo(validUrl, validProperties);
        assertThat(infos.length, is(1));
        assertThat(infos[0].name, is(JdbcLocalI18n.repositoryNamePropertyName.text()));
        assertThat(infos[0].description, is(JdbcLocalI18n.repositoryNamePropertyDescription.text()));
        assertThat(infos[0].required, is(true));
    }

    @Test
    public void shouldReturnRepositoryPropertyInfoWhenMissingWorkspaceName() throws SQLException {
        validUrl = LocalJcrDriver.JNDI_URL_PREFIX + jndiNameForRepositories;
        // validProperties.put(JdbcDriver.WORKSPACE_PROPERTY_NAME, "MyWorkspace");
        validProperties.put(LocalJcrDriver.USERNAME_PROPERTY_NAME, "jsmith");
        validProperties.put(LocalJcrDriver.PASSWORD_PROPERTY_NAME, "secret");
        validProperties.put(LocalJcrDriver.REPOSITORY_PROPERTY_NAME, validRepositoryName);
        DriverPropertyInfo[] infos = driver.getPropertyInfo(validUrl, validProperties);
        assertThat(infos.length, is(1));
        assertThat(infos[0].name, is(JdbcLocalI18n.workspaceNamePropertyName.text()));
        assertThat(infos[0].description, is(JdbcLocalI18n.workspaceNamePropertyDescription.text()));
        assertThat(infos[0].required, is(false));
    }

    @Test
    public void shouldReturnRepositoryPropertyInfoWhenMissingUsername() throws SQLException {
        validUrl = LocalJcrDriver.JNDI_URL_PREFIX + jndiNameForRepositories;
        validProperties.put(LocalJcrDriver.WORKSPACE_PROPERTY_NAME, "MyWorkspace");
        // validProperties.put(JdbcDriver.USERNAME_PROPERTY_NAME, "jsmith");
        validProperties.put(LocalJcrDriver.PASSWORD_PROPERTY_NAME, "secret");
        validProperties.put(LocalJcrDriver.REPOSITORY_PROPERTY_NAME, validRepositoryName);
        DriverPropertyInfo[] infos = driver.getPropertyInfo(validUrl, validProperties);
        assertThat(infos.length, is(1));
        assertThat(infos[0].name, is(JdbcLocalI18n.usernamePropertyName.text()));
        assertThat(infos[0].description, is(JdbcLocalI18n.usernamePropertyDescription.text()));
        assertThat(infos[0].required, is(false));
    }

    @Test
    public void shouldReturnRepositoryPropertyInfoWhenMissingPassword() throws SQLException {
        validUrl = LocalJcrDriver.JNDI_URL_PREFIX + jndiNameForRepositories;
        validProperties.put(LocalJcrDriver.WORKSPACE_PROPERTY_NAME, "MyWorkspace");
        validProperties.put(LocalJcrDriver.USERNAME_PROPERTY_NAME, "jsmith");
        // validProperties.put(JdbcDriver.PASSWORD_PROPERTY_NAME, "secret");
        validProperties.put(LocalJcrDriver.REPOSITORY_PROPERTY_NAME, validRepositoryName);
        DriverPropertyInfo[] infos = driver.getPropertyInfo(validUrl, validProperties);
        assertThat(infos.length, is(1));
        assertThat(infos[0].name, is(JdbcLocalI18n.passwordPropertyName.text()));
        assertThat(infos[0].description, is(JdbcLocalI18n.passwordPropertyDescription.text()));
        assertThat(infos[0].required, is(false));
    }

    @Test
    public void shouldAcceptValidUrls() {
        assertThat(driver.acceptsURL(LocalJcrDriver.JNDI_URL_PREFIX
                                     + "java:nameInJndi?workspace=MyWorkspace&user=jsmith&password=secret&teiidsupport=true"),
                   is(true));
        assertThat(driver.acceptsURL(LocalJcrDriver.JNDI_URL_PREFIX
                                     + "java:nameInJndi?workspace=MyWorkspace&user=jsmith&password=secret"),
                   is(true));
        assertThat(driver.acceptsURL(LocalJcrDriver.JNDI_URL_PREFIX + "java:nameInJndi?workspace=MyWorkspace&user=jsmith"),
                   is(true));
        assertThat(driver.acceptsURL(LocalJcrDriver.JNDI_URL_PREFIX + "java:nameInJndi?workspace=My%20Workspace"), is(true));
        assertThat(driver.acceptsURL(LocalJcrDriver.JNDI_URL_PREFIX + "java:nameInJndi"), is(true));
        assertThat(driver.acceptsURL(LocalJcrDriver.JNDI_URL_PREFIX + "java:nameInJndi?"), is(true));
        assertThat(driver.acceptsURL(LocalJcrDriver.JNDI_URL_PREFIX + "java"), is(true));
        assertThat(driver.acceptsURL(LocalJcrDriver.JNDI_URL_PREFIX + "j"), is(true));
        assertThat(driver.acceptsURL(LocalJcrDriver.JNDI_URL_PREFIX + "j "), is(true));
        assertThat(driver.acceptsURL(LocalJcrDriver.JNDI_URL_PREFIX + " j"), is(true));
    }

    @Test
    public void shouldNotAcceptInvalidUrls() {
        assertThat(driver.acceptsURL("jdbc:jcr:jndi"), is(false));
        assertThat(driver.acceptsURL(LocalJcrDriver.JNDI_URL_PREFIX), is(false));
        assertThat(driver.acceptsURL(LocalJcrDriver.JNDI_URL_PREFIX + " "), is(false));
        assertThat(driver.acceptsURL("file://"), is(false));
        assertThat(driver.acceptsURL("jdbc://"), is(false));
    }

    @Test
    public void shouldCreateConnectionInfoForUrlWithEscapedCharacters() throws SQLException {
        validUrl = LocalJcrDriver.JNDI_URL_PREFIX
                   + "java:nameInJndi?workspace=My%20Workspace&user=j%20smith&password=secret&repositoryName=My%20Repository&teiidsupport=true";
        ConnectionInfo info = driver.createConnectionInfo(validUrl, validProperties);
        assertThat(info.getWorkspaceName(), is("My Workspace"));
        assertThat(info.getUsername(), is("j smith"));
        assertThat(info.getPassword(), is("secret".toCharArray()));
        assertThat(info.getRepositoryName(), is("My Repository"));
        assertThat(info.isTeiidSupport(), is(Boolean.TRUE.booleanValue()));
    }

    @Test
    public void shouldCreateConnectionInfoButIndicateNoTeiidSupport() throws SQLException {
        validUrl = LocalJcrDriver.JNDI_URL_PREFIX
                   + "java:nameInJndi?workspace=My%20Workspace&user=j%20smith&password=secret&repositoryName=My%20Repository";
        ConnectionInfo info = driver.createConnectionInfo(validUrl, validProperties);
        assertThat(info.isTeiidSupport(), is(Boolean.FALSE.booleanValue()));
    }

    @Test
    public void shouldCreateConnectionWithValidUrlAndProperties() throws SQLException {
        Connection conn = driver.connect(validUrl, validProperties);
        assertThat(conn, is(notNullValue()));
        assertThat(conn.isClosed(), is(false));
        conn.close();
        assertThat(conn.isClosed(), is(true));
    }

    @Test
    public void shouldCreateConnectionWithDriverManagerAfterRegisteringDriver() throws SQLException {
        DriverManager.registerDriver(driver);
        Connection connection = DriverManager.getConnection(validUrl, validProperties);
        assertThat(connection, is(notNullValue()));
        assertThat(connection, is(instanceOf(JcrConnection.class)));
        assertThat(connection.isWrapperFor(JcrConnection.class), is(true));
        assertThat(connection.unwrap(JcrConnection.class), is(instanceOf(JcrConnection.class)));
    }

}
