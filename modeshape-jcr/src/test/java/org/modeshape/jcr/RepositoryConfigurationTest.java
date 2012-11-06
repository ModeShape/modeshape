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
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.RepositoryConfiguration.AnonymousSecurity;
import org.modeshape.jcr.RepositoryConfiguration.JaasSecurity;
import org.modeshape.jcr.RepositoryConfiguration.Security;
import org.modeshape.jcr.RepositoryConfiguration.TransactionMode;

public class RepositoryConfigurationTest {
    private boolean print = false;

    @Before
    public void beforeEach() {
        print = false;
    }

    @Test
    public void shouldSuccessfullyValidateDefaultConfiguration() {
        assertValid(new RepositoryConfiguration("repoName"));
    }

    @Test
    public void shouldReportErrorWithNoName() {
        assertNotValid(1, "{}");
    }

    @Test
    public void shouldReportErrorWithExtraTopLevelProperties() {
        assertNotValid(1, "{ 'name' = 'nm', 'notValid' : false }");
    }

    @Test
    public void shouldReportErrorWithExtraStorageProperties() {
        assertNotValid(1, "{ 'name' = 'nm', 'storage' : { 'notValid' : false } }");
    }

    @Test
    public void shouldReportErrorWithExtraWorkspacesProperties() {
        assertNotValid(1, "{ 'name' = 'nm', \"workspaces\" : { \"notValid\" : false } }");
    }

    @Test
    public void shouldReportErrorWithExtraSecurityProperties() {
        assertNotValid(1, "{ 'name' = 'nm', \"security\" : { \"notValid\" : false } }");
    }

    @Test
    public void shouldReportErrorWithExtraQueryProperties() {
        assertNotValid(1, "{ 'name' = 'nm', \"query\" : { \"notValid\" : false } }");
    }

    @Test
    public void shouldReportErrorWithExtraSequencingProperties() {
        assertNotValid(1, "{ 'name' = 'nm', \"sequencing\" : { \"notValid\" : false, 'sequencers' : {} } }");
    }

    @Test
    public void shouldSuccessfullyValidateSampleRepositoryConfiguration() {
        RepositoryConfiguration config = assertValid("sample-repo-config.json");
        assertThat(config.getTransactionMode(), is(TransactionMode.AUTO));
    }

    @Test
    public void shouldSuccessfullyValidateSampleRepositoryConfiguration2() {
        RepositoryConfiguration config = assertValid("config/sample-repo-config.json");
        assertThat(config.getTransactionMode(), is(TransactionMode.NONE));
    }

    @Test
    public void shouldSuccessfullyValidateSampleRepositoryConfigurationWithIndexStorageInRam() {
        assertValid("config/index-storage-config-ram.json");
    }

    @Test
    public void shouldSuccessfullyValidateSampleRepositoryConfigurationWithIndexStorageOnFilesystem() {
        assertValid("config/index-storage-config-filesystem.json");
    }

    @Test
    public void shouldSuccessfullyValidateSampleRepositoryConfigurationWithIndexStorageOnFilesystemMaster() {
        assertValid("config/index-storage-config-filesystem-master.json");
    }

    @Test
    public void shouldSuccessfullyValidateSampleRepositoryConfigurationWithIndexStorageOnFilesystemSlave() {
        assertValid("config/index-storage-config-filesystem-slave.json");
    }

    @Test
    public void shouldSuccessfullyValidateSampleRepositoryConfigurationWithIndexStorageInInfinispan() {
        assertValid("config/index-storage-config-infinispan.json");
    }

    @Test
    public void shouldSuccessfullyValidateThoroughRepositoryConfiguration() {
        assertValid("config/thorough-repo-config.json");
    }

    @Test
    public void shouldSuccessfullyValidateThoroughRepositoryConfigurationWithDescriptions() {
        assertValid("config/thorough-with-desc-repo-config.json");
    }

    @Test
    public void shouldSuccessfullyValidateJndiBasedDataStoreBinaryStorageConfiguration() {
        assertValid("config/database-jndi-binary-storage.json");
    }

    @Test
    public void shouldSuccessfullyValidateDriverBasedBinaryStorageConfiguration() {
        assertValid("config/database-url-binary-storage.json");
    }

    @Test
    public void shouldSuccessfullyValidateCustomBinaryStorageConfiguration() {
        assertValid("config/custom-binary-storage.json");
    }

    @Test
    public void shouldNotSuccessfullyValidateSampleRepositoryConfigurationWithIndexStorageOnFilesystemAndExtraProperties() {
        assertNotValid(1, "config/invalid-index-storage-config-filesystem.json");
    }

    @Test
    public void shouldNotSuccessfullyValidateRepositoryConfigurationWithOldStyleSequencersArray() {
        assertNotValid(1, "config/invalid-old-style-sequencers-config.json");
    }

    @Test
    public void shouldNotSuccessfullyValidateRepositoryConfigurationWithOldStyleExtractorsArray() {
        assertNotValid(1, "config/invalid-old-style-extractors-config.json");
    }

    @Test
    public void shouldSuccessfullyValidateFederationConfiguration() {
        assertValid("config/repo-config-federation.json");
    }

    @Test
    public void shouldSuccessfullyValidateFileSystemFederationConfiguration() {
        assertValid("config/repo-config-filesystem-federation.json");
    }

    @Test
    public void shouldAlwaysReturnNonNullSecurityComponent() {
        RepositoryConfiguration config = new RepositoryConfiguration("repoName");
        assertThat(config.getSecurity(), is(notNullValue()));
    }

    @Test
    public void shouldNotConfigureJaasByDefault() {
        RepositoryConfiguration config = new RepositoryConfiguration("repoName");
        Security security = config.getSecurity();
        JaasSecurity jaas = security.getJaas();
        assertThat(jaas, is(nullValue()));
    }

    @Test
    public void shouldHavePolicyByDefaultWhenConfiguringJaas() throws Exception {
        RepositoryConfiguration config = RepositoryConfiguration.read("{ \"security\" : { \"jaas\" : {} } }");
        Security security = config.getSecurity();
        JaasSecurity jaas = security.getJaas();
        assertThat(jaas, is(notNullValue()));
        assertThat(jaas.getPolicyName(), is(RepositoryConfiguration.Default.JAAS_POLICY_NAME));
    }

    @Test
    public void shouldHaveDefinedPolicyWhenConfiguringJaas() throws Exception {
        RepositoryConfiguration config = RepositoryConfiguration.read("{ \"security\" : { \"jaas\" : { \"policyName\" : \"mypolicy\" } } }");
        Security security = config.getSecurity();
        JaasSecurity jaas = security.getJaas();
        assertThat(jaas, is(notNullValue()));
        assertThat(jaas.getPolicyName(), is("mypolicy"));
    }

    @Test
    public void shouldConfigureAnonymousByDefault() {
        RepositoryConfiguration config = new RepositoryConfiguration("repoName");
        Security security = config.getSecurity();
        AnonymousSecurity anon = security.getAnonymous();
        assertThat(anon, is(notNullValue()));
        assertThat(anon.getAnonymousUsername(), is(RepositoryConfiguration.Default.ANONYMOUS_USERNAME));
        assertThat(anon.getAnonymousRoles(), is(RepositoryConfiguration.Default.ANONYMOUS_ROLES));
    }

    @Test
    public void shouldNotConfigureAnonymousIfNoRolesAreSpecified() throws Exception {
        RepositoryConfiguration config = RepositoryConfiguration.read("{ \"security\" : { \"anonymous\" : { \"roles\" : [] } } }");
        Security security = config.getSecurity();
        AnonymousSecurity anon = security.getAnonymous();
        assertThat(anon, is(nullValue()));
    }

    @Test
    public void shouldHaveDefinedRolesWhenConfiguringAnonymous() throws Exception {
        RepositoryConfiguration config = RepositoryConfiguration.read("{ \"security\" : { \"jaas\" : { \"policyName\" : \"mypolicy\" } } }");
        Security security = config.getSecurity();
        JaasSecurity jaas = security.getJaas();
        assertThat(jaas, is(notNullValue()));
        assertThat(jaas.getPolicyName(), is("mypolicy"));
    }

    @Test
    public void shouldHaveDefinedAnonymousUsernameWhenConfiguringAnonymous() throws Exception {
        RepositoryConfiguration config = RepositoryConfiguration.read("{ \"security\" : { \"jaas\" : { \"policyName\" : \"mypolicy\" } } }");
        Security security = config.getSecurity();
        JaasSecurity jaas = security.getJaas();
        assertThat(jaas, is(notNullValue()));
        assertThat(jaas.getPolicyName(), is("mypolicy"));
    }

    @Test
    public void shouldAlwaysReturnNonNullQueryComponent() {
        RepositoryConfiguration config = new RepositoryConfiguration("repoName");
        assertThat(config.getQuery(), is(notNullValue()));
    }

    @Test
    public void shouldAlwaysReturnNonNullSequencingComponent() {
        RepositoryConfiguration config = new RepositoryConfiguration("repoName");
        assertThat(config.getSequencing(), is(notNullValue()));
    }

    @Test
    public void shouldAllowValidButSimpleRepositoryConfiguration() {
        assertValid("{ \"name\" : \"sample\", \"jndiName\" : \"modeshape_repo1\"}");
    }

    @Test
    public void shouldAllowValidButSimpleRepositoryConfigurationWithSingleQuotes() {
        assertValid("{ 'name' : 'sample', 'jndiName' : 'modeshape_repo1'}");
    }

    @Test
    public void shouldUseDefaultClusteringValues() throws Exception {
        RepositoryConfiguration config = RepositoryConfiguration.read("{ \"clustering\" : { } }");
        RepositoryConfiguration.Clustering clusteringConfiguration = config.getClustering();
        assertEquals(RepositoryConfiguration.Default.CLUSTER_NAME, clusteringConfiguration.getClusterName());
        assertEquals(RepositoryConfiguration.Default.CHANNEL_PROVIDER, clusteringConfiguration.getChannelProviderClassName());
        assertNull(clusteringConfiguration.getChannelConfiguration());
        assertNotNull(clusteringConfiguration.getDocument());
    }

    @Test
    public void shouldAllowClusteringToBeConfigured() throws Exception {
        String clusterName = "testCluster";
        String providerClass = "someClass";
        String channelConfig = "someConfig";

        RepositoryConfiguration config = RepositoryConfiguration.read("{ \"clustering\" : {\"clusterName\":\"" + clusterName
                                                                      + "\", \"channelProvider\":\"" + providerClass
                                                                      + "\", \"channelConfiguration\": \"" + channelConfig
                                                                      + "\"} }");
        RepositoryConfiguration.Clustering clusteringConfiguration = config.getClustering();
        assertEquals(clusterName, clusteringConfiguration.getClusterName());
        assertEquals(providerClass, clusteringConfiguration.getChannelProviderClassName());
        assertEquals(channelConfig, clusteringConfiguration.getChannelConfiguration());
        assertNotNull(clusteringConfiguration.getDocument());
    }

    @Test
    public void shouldAllowWorkspaceCacheContainerToBeConfigured() throws Exception {
        String cacheContainer = "my-container";

        RepositoryConfiguration config = RepositoryConfiguration.read("{ \"name\" : \"foo\", \"workspaces\" : {\"cacheConfiguration\":\""
                                                                      + cacheContainer + "\"} }");
        System.out.println(config.validate());
        assertThat(config.validate().hasProblems(), is(false));
        assertEquals(cacheContainer, config.getWorkspaceCacheConfiguration());

        config = RepositoryConfiguration.read("{ 'name' : 'foo', 'workspaces' : { 'cacheConfiguration' : '" + cacheContainer
                                              + "' } }");
        System.out.println(config.validate());
        assertThat(config.validate().hasProblems(), is(false));
        assertEquals(cacheContainer, config.getWorkspaceCacheConfiguration());
    }

    protected RepositoryConfiguration assertValid( RepositoryConfiguration config ) {
        Problems results = config.validate();
        assertThat(results.toString(), results.hasProblems(), is(false));
        return config;
    }

    protected RepositoryConfiguration assertValid( String configContent ) {
        return assertValid(assertRead(configContent));
    }

    protected void assertNotValid( int numberOfErrors,
                                   RepositoryConfiguration config ) {
        Problems results = config.validate();
        assertThat(results.toString(), results.hasProblems(), is(true));
        assertThat(results.toString(), results.hasErrors(), is(true));
        assertThat(results.toString(), results.errorCount(), is(numberOfErrors));
        if (print) {
            System.out.println(results);
        }
    }

    protected void assertNotValid( int numberOfErrors,
                                   String configContent ) {
        assertNotValid(numberOfErrors, assertRead(configContent));
    }

    protected RepositoryConfiguration assertRead( String content ) {
        try {
            return RepositoryConfiguration.read(content);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
            return null;
        }
    }

}
