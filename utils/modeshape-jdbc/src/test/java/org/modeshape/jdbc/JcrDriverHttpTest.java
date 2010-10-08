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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.modeshape.jdbc.delegate.ConnectionInfo;

/**
 * 
 */
public class JcrDriverHttpTest {

    private JcrDriver driver;
    private String validServerName;
    private String validRepositoryName;
    private String validWorkspaceName;
    private String validUrl;
    private Properties validProperties;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        validRepositoryName = "MyRepository";
        validServerName = "serverName:8080";
        validWorkspaceName = "default";
        validUrl = JcrDriver.HTTP_URL_PREFIX + validServerName + "/modeshape-rest/" + validRepositoryName + "/"
                   + validWorkspaceName + "?user=jsmith&password=secret";
        validProperties = new Properties();

        driver = new JcrDriver();
    }

    @After
    public void afterEach() throws Exception {
        try {
            DriverManager.deregisterDriver(driver);
        } finally {
            driver = null;
        }
    }

    @Test
    public void shouldNotBeJdbcCompliant() {
        assertThat(driver.jdbcCompliant(), is(false));
    }

    @Test
    public void shouldHaveMajorVersion() {
        assertThat(driver.getMajorVersion(), is(TestUtil.majorVersion()));
    }

    @Test
    public void shouldHaveMinorVersion() {
        assertThat(driver.getMinorVersion(), is(TestUtil.minorVersion()));
    }

    @Test
    public void shouldHaveVendorUrl() {
        assertThat(driver.getVendorUrl(), is(JdbcI18n.driverVendorUrl.text()));
    }

    @Test
    public void shouldHaveVendorName() {
        assertThat(driver.getVendorName(), is(JdbcI18n.driverVendor.text()));
    }

    @Test
    public void shouldHaveVersion() {
        assertThat(driver.getVersion(), is(JdbcI18n.driverVersion.text()));
    }

    @Test
    public void shouldReturnEmptyPropertyInfosWhenSuppliedValidAndCompleteUrlAndNoProperties() throws SQLException {
        DriverPropertyInfo[] infos = driver.getPropertyInfo(validUrl, validProperties);
        assertThat(infos.length, is(0));
    }

    @Test
    public void shouldReturnEmptyPropertyInfosWhenSuppliedValidUrlAndAllPropertiesWithRepositoryInHttp() throws SQLException {
        validUrl = JcrDriver.HTTP_URL_PREFIX + validServerName + "/modeshape-rest";
        validProperties.put(JcrDriver.WORKSPACE_PROPERTY_NAME, "MyWorkspace");
        validProperties.put(JcrDriver.USERNAME_PROPERTY_NAME, "jsmith");
        validProperties.put(JcrDriver.PASSWORD_PROPERTY_NAME, "secret");
        validProperties.put(JcrDriver.REPOSITORY_PROPERTY_NAME, validRepositoryName);
        DriverPropertyInfo[] infos = driver.getPropertyInfo(validUrl, validProperties);
        assertThat(infos.length, is(0));
    }

    @Test
    public void shouldReturnRepositoryPropertyInfoWhenMissingRequiredRepositoryName() throws SQLException {
        validUrl = JcrDriver.HTTP_URL_PREFIX + validServerName + "/modeshape-rest";
        validProperties.put(JcrDriver.WORKSPACE_PROPERTY_NAME, "MyWorkspace");
        validProperties.put(JcrDriver.USERNAME_PROPERTY_NAME, "jsmith");
        validProperties.put(JcrDriver.PASSWORD_PROPERTY_NAME, "secret");
        // validProperties.put(JdbcDriver.REPOSITORY_PROPERTY_NAME, validRepositoryName);
        DriverPropertyInfo[] infos = driver.getPropertyInfo(validUrl, validProperties);
        assertThat(infos.length, is(1));
        assertThat(infos[0].name, is(JdbcI18n.repositoryNamePropertyName.text()));
        assertThat(infos[0].description, is(JdbcI18n.repositoryNamePropertyDescription.text()));
        assertThat(infos[0].required, is(true));
    }

    @Test
    public void shouldReturnRepositoryPropertyInfoWhenMissingWorkspaceName() throws SQLException {
        validUrl = JcrDriver.HTTP_URL_PREFIX + validServerName + "/modeshape-rest";
        // validProperties.put(JdbcDriver.WORKSPACE_PROPERTY_NAME, "MyWorkspace");
        validProperties.put(JcrDriver.USERNAME_PROPERTY_NAME, "jsmith");
        validProperties.put(JcrDriver.PASSWORD_PROPERTY_NAME, "secret");
        validProperties.put(JcrDriver.REPOSITORY_PROPERTY_NAME, validRepositoryName);
        DriverPropertyInfo[] infos = driver.getPropertyInfo(validUrl, validProperties);
        assertThat(infos.length, is(1));
        assertThat(infos[0].name, is(JdbcI18n.workspaceNamePropertyName.text()));
        assertThat(infos[0].description, is(JdbcI18n.workspaceNamePropertyDescription.text()));
        assertThat(infos[0].required, is(false));
    }

    @Test
    public void shouldReturnRepositoryPropertyInfoWhenMissingUsername() throws SQLException {
        validUrl = JcrDriver.HTTP_URL_PREFIX + validServerName + "/modeshape-rest";
        validProperties.put(JcrDriver.WORKSPACE_PROPERTY_NAME, "MyWorkspace");
        // validProperties.put(JdbcDriver.USERNAME_PROPERTY_NAME, "jsmith");
        validProperties.put(JcrDriver.PASSWORD_PROPERTY_NAME, "secret");
        validProperties.put(JcrDriver.REPOSITORY_PROPERTY_NAME, validRepositoryName);
        DriverPropertyInfo[] infos = driver.getPropertyInfo(validUrl, validProperties);
        assertThat(infos.length, is(1));
        assertThat(infos[0].name, is(JdbcI18n.usernamePropertyName.text()));
        assertThat(infos[0].description, is(JdbcI18n.usernamePropertyDescription.text()));
        assertThat(infos[0].required, is(false));
    }

    @Test
    public void shouldReturnRepositoryPropertyInfoWhenMissingPassword() throws SQLException {
        validUrl = JcrDriver.HTTP_URL_PREFIX + validServerName + "/modeshape-rest";
        validProperties.put(JcrDriver.WORKSPACE_PROPERTY_NAME, "MyWorkspace");
        validProperties.put(JcrDriver.USERNAME_PROPERTY_NAME, "jsmith");
        // validProperties.put(JdbcDriver.PASSWORD_PROPERTY_NAME, "secret");
        validProperties.put(JcrDriver.REPOSITORY_PROPERTY_NAME, validRepositoryName);
        DriverPropertyInfo[] infos = driver.getPropertyInfo(validUrl, validProperties);
        assertThat(infos.length, is(1));
        assertThat(infos[0].name, is(JdbcI18n.passwordPropertyName.text()));
        assertThat(infos[0].description, is(JdbcI18n.passwordPropertyDescription.text()));
        assertThat(infos[0].required, is(false));
    }

    @Test
    public void shouldAcceptValidUrls() {
        assertThat(driver.acceptsURL(JcrDriver.HTTP_URL_PREFIX + validServerName + "/modeshape-rest/" + this.validRepositoryName
                                     + "/MyWorkspace" + "&user=jsmith&password=secret"), is(true));
        assertThat(driver.acceptsURL(JcrDriver.HTTP_URL_PREFIX + validServerName + "/modeshape-rest/" + this.validRepositoryName
                                     + "&user=jsmith&password=secret"), is(true));
        assertThat(driver.acceptsURL(JcrDriver.HTTP_URL_PREFIX + validServerName + "/modeshape-rest/"
                                     + "&user=jsmith&password=secret"), is(true));
    }

    @Test
    public void shouldNotAcceptInvalidUrls() {
        assertThat(driver.acceptsURL("jdbc:jcr:http"), is(false));
        assertThat(driver.acceptsURL(JcrDriver.HTTP_URL_PREFIX), is(false));
        assertThat(driver.acceptsURL(JcrDriver.HTTP_URL_PREFIX + " "), is(false));
        assertThat(driver.acceptsURL("file://"), is(false));
        assertThat(driver.acceptsURL("jdbc://"), is(false));
    }

    @Test
    public void shouldCreateConnectionInfoForUrlWithEscapedCharacters() throws SQLException {
        validUrl = JcrDriver.HTTP_URL_PREFIX + validServerName + "/modeshape%20rest"
                   + "?repositoryName=My%20Repository&workspace=My%20Workspace&user=j%20smith&password=secret";
        ConnectionInfo info = driver.createConnectionInfo(validUrl, validProperties);
        assertThat(info.getWorkspaceName(), is("My Workspace"));
        assertThat(info.getUsername(), is("j smith"));
        assertThat(info.getPassword(), is("secret".toCharArray()));
        assertThat(info.getRepositoryName(), is("My Repository"));
    }

    // @Test
    // public void shouldCreateConnectionWithDriverManagerAfterRegisteringDriver() throws SQLException {
    // DriverManager.registerDriver(driver);
    // Connection connection = DriverManager.getConnection(validUrl, validProperties);
    // assertThat(connection, is(notNullValue()));
    // assertThat(connection, is(instanceOf(JcrConnection.class)));
    // assertThat(connection.isWrapperFor(JcrConnection.class), is(true));
    // assertThat(connection.unwrap(JcrConnection.class), is(instanceOf(JcrConnection.class)));
    // }

}
