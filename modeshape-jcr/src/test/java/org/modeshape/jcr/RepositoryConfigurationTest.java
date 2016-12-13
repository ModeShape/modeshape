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
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.EnumSet;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.RepositoryConfiguration.AnonymousSecurity;
import org.modeshape.jcr.RepositoryConfiguration.Default;
import org.modeshape.jcr.RepositoryConfiguration.DocumentOptimization;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.jcr.RepositoryConfiguration.Indexes;
import org.modeshape.jcr.RepositoryConfiguration.JaasSecurity;
import org.modeshape.jcr.RepositoryConfiguration.Security;
import org.modeshape.jcr.api.index.IndexDefinition;
import org.modeshape.jcr.api.index.IndexDefinition.IndexKind;
import org.modeshape.jcr.value.binary.MongodbBinaryStore;
import org.modeshape.schematic.Schematic;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.EditableDocument;
import org.modeshape.schematic.document.Editor;

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
    public void shouldAcceptSequencerWithNoPathExpression() throws Exception {
        RepositoryConfiguration config = RepositoryConfiguration.read("{ 'name' : 'Repo', \"sequencing\" : { 'sequencers' : { 'foo' : { 'classname' : 'xsdsequencer' } } } }");
        assertValid(config);
    }

    @Test
    public void shouldNotReplaceBlankValuesWithNull() throws Exception {
        RepositoryConfiguration config = RepositoryConfiguration.read("{ 'name' : 'Repo', 'jndiName' : '' }");
        assertThat(config.getJndiName(), is(""));
    }

    @Test
    public void shouldReplaceVariables() {
        RepositoryConfiguration config = assertValid("{ 'name' = '${os.name} Repository' }");
        assertThat(config.getName(), is(System.getProperty("os.name") + " Repository"));
        System.out.println(config.getDocument());
    }

    @Test
    public void shouldSuccessfullyValidateSampleRepositoryConfiguration() {
        assertHasWarnings(0, "sample-repo-config.json");
    }

    @Test
    public void shouldSuccessfullyValidateSampleRepositoryConfiguration2() {
        assertValid("config/sample-repo-config.json");
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
    @FixFor( "MODE-2574" )
    public void shouldSuccessfullyValidateCassandraBinaryStorageConfiguration() {
        assertValid("config/cassandra-binary-storage.json");
    }
 
    @Test
    @FixFor( {"MODE-2575", "MODE-2635"} )
    public void shouldSuccessfullyValidateMongoBinaryStorageConfiguration() throws Exception {
        RepositoryConfiguration config = assertValid("config/mongo-binary-storage-full-config.json");
        Document storageDoc = config.getDocument().getDocument(FieldName.STORAGE).getDocument(FieldName.BINARY_STORAGE);
        assertEquals(Arrays.asList("192.1.68.1.1:90", "143.22.33.123:120"), storageDoc.get(FieldName.HOST_ADDRESSES));
        RepositoryConfiguration.BinaryStorage storage = config.getBinaryStorage();
        assertEquals(RepositoryConfiguration.FieldValue.BINARY_STORAGE_TYPE_MONGO, storage.getType());
        assertTrue(storage.getBinaryStore() instanceof  MongodbBinaryStore);
        
        // remove host and port, check that the config is still valid
        Editor editor = config.edit();
        EditableDocument storageDocEditable = editor.getDocument(FieldName.STORAGE).getDocument(FieldName.BINARY_STORAGE);
        storageDocEditable.remove(FieldName.HOST);
        storageDocEditable.remove(FieldName.PORT);
        RepositoryConfiguration configWithoutHostPort = new RepositoryConfiguration(editor.unwrap(), "mongo-config-1");
        assertValid(configWithoutHostPort);
                                                                     
        // remove host addresses as well and check that what remains is not valid
        storageDocEditable.remove(FieldName.HOST_ADDRESSES);
        RepositoryConfiguration invalidConfig = new RepositoryConfiguration(editor.unwrap(), "mongo-config-2");
        try {
            invalidConfig.getBinaryStorage().getBinaryStore();
            fail("Should not allow a Mongo binary storage without host, port and host addresses");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
    public void shouldSuccessfullyValidateS3BinaryStorageConfiguration() {
        assertValid("config/s3-binary-storage.json");
    }

    @Test
    public void shouldSuccessfullyValidateCompositeBinaryStorageConfiguration() {
        assertValid("config/composite-binary-storage.json");
    }

    @Test
    public void shouldSuccessfullyValidateCompositeBinaryStorageWithoutDefaultNamedStoreConfiguration() {
        assertNotValid(1, "config/composite-binary-storage-without-default.json");
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
    public void shouldSuccessfullyValidateFederationConfiguration() {
        assertValid("config/repo-config-mock-federation.json");
    }

    @Test
    public void shouldSuccessfullyValidateFileSystemFederationConfiguration() {
        assertValid("config/repo-config-filesystem-federation.json");
    }

    @Test
    public void shouldSuccessfullyValidateConfigurationWithGarbageCollection() {
        assertValid("config/repo-config-garbage-collection.json");
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

    @FixFor( "MODE-2160" )
    @Test
    public void shouldAlwaysReturnNonNullIndexesComponentForNoIndexes() {
        RepositoryConfiguration config = new RepositoryConfiguration("repoName");
        assertThat(config.getIndexes(), is(notNullValue()));
    }

    @FixFor( "MODE-2160" )
    @Test
    public void shouldAlwaysReturnNonNullIndexProvidersList() {
        RepositoryConfiguration config = new RepositoryConfiguration("repoName");
        assertThat(config.getIndexProviders(), is(notNullValue()));
    }

    @FixFor( "MODE-2160" )
    @Test
    public void shouldSuccessfullyValidateSampleRepositoryConfigurationWithIndexStorageInRam() {
        assertValid("config/repo-config-valid-index-providers.json");
    }

    @FixFor( "MODE-2160" )
    @Test
    public void shouldAllowValidRepositoryConfigurationWithIndexProvidersAndNoIndexes() {
        assertValid("config/repo-config-local-provider-no-indexes.json");
    }

    @FixFor( {"MODE-2160", "MODE-2279"} )
    @Test
    public void shouldAllowValidRepositoryConfigurationWithIndexProvidersAndNotionalIndexes() {
        RepositoryConfiguration config = assertValid("config/repo-config-local-provider-and-notional-indexes.json");
        Indexes indexes = config.getIndexes();
        EnumSet<IndexKind> found = EnumSet.noneOf(IndexKind.class);
        for (String indexName : indexes.getIndexNames()) {
            IndexDefinition defn = indexes.getIndex(indexName);
            IndexKind kind = defn.getKind();
            found.add(kind);
            assertThat(kind, is(notNullValue()));
        }
        assertThat(found, is(EnumSet.allOf(IndexKind.class)));
    }

    @FixFor( {"MODE-2160", "MODE-2279"} )
    @Test
    public void shouldAllowValidRepositoryConfigurationWithIndexProvidersAndIndexes() {
        RepositoryConfiguration config = assertValid("config/repo-config-local-provider-and-indexes.json");
        Indexes indexes = config.getIndexes();
        for (String indexName : indexes.getIndexNames()) {
            IndexDefinition defn = indexes.getIndex(indexName);
            assertThat(defn.getKind(), is(notNullValue()));
        }
    }

    @FixFor( "MODE-2160" )
    @Test
    public void shouldNotAllowRepositoryConfigurationWithIndexThatRefersToNonExistantIndexProvider() {
        assertValidWithWarnings(1, "config/invalid-index-with-unmatched-provider.json");
    }

    @FixFor( "MODE-2160" )
    @Test
    public void shouldNotAllowRepositoryConfigurationWithIndexThatHasNoProvider() {
        assertNotValid(1, "config/invalid-index-with-missing-provider.json");
    }

    @FixFor( "MODE-2160" )
    @Test
    public void shouldNotAllowRepositoryConfigurationWithIndexThatHasMalformedKind() {
        assertNotValid(1, "config/invalid-index-with-malformed-kind.json");
    }

    @FixFor( "MODE-2160" )
    @Test
    public void shouldNotAllowRepositoryConfigurationWithIndexThatHasMalformedColumns() {
        assertNotValid(1, "config/invalid-index-with-malformed-columns.json");
    }
    
    @FixFor( "MODE-2387" )
    @Test
    public void shouldAllowCustomSettingsForLocalIndexProvider() {
        assertValid("config/local-index-provider-with-custom-settings.json");        
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
    public void shouldAllowValidProjectionExpressions() throws Exception {
        assertValid("config/repo-config-federation-projections.json");
    }

    @Test
    public void shouldNotAllowInvalidProjectionExpressions() throws Exception {
        assertNotValid(9, "config/repo-config-federation-invalid-projections.json");
    }

    @Test
    public void shouldAllowJdbcBinaryStorage() throws Exception {
        assertValid("config/repo-config-jdbc-binary-storage.json");
    }

    @FixFor( "MODE-1988" )
    @Test
    public void shouldNotEnableDocumentOptimizationByDefault() {
        RepositoryConfiguration config = new RepositoryConfiguration("repoName");
        assertThat(config.getDocumentOptimization(), is(notNullValue()));
        assertThat(config.getDocumentOptimization().isEnabled(), is(false));
    }

    @FixFor( "MODE-1988" )
    @Test
    public void shouldEnableDocumentOptimizationWithEmptyDocumentOptimizationField() {
        Document doc = Schematic.newDocument(FieldName.NAME, "repoName", FieldName.STORAGE,
                                             Schematic.newDocument(FieldName.DOCUMENT_OPTIMIZATION, Schematic.newDocument()));
        RepositoryConfiguration config = new RepositoryConfiguration(doc, "repoName");
        DocumentOptimization opt = config.getDocumentOptimization();
        assertThat(opt, is(notNullValue()));
        assertThat(opt.isEnabled(), is(false));
    }

    @FixFor( "MODE-1988" )
    @Test
    public void shouldEnableDocumentOptimizationWithValidChildCountTargetAndToleranceValues() {
        Document docOpt = Schematic.newDocument(FieldName.OPTIMIZATION_CHILD_COUNT_TARGET, 500,
                                                FieldName.OPTIMIZATION_CHILD_COUNT_TOLERANCE, 10);
        Document doc = Schematic.newDocument(FieldName.NAME, "repoName", FieldName.STORAGE,
                                             Schematic.newDocument(FieldName.DOCUMENT_OPTIMIZATION, docOpt));
        RepositoryConfiguration config = new RepositoryConfiguration(doc, "repoName");
        DocumentOptimization opt = config.getDocumentOptimization();
        assertThat(opt, is(notNullValue()));
        assertThat(opt.isEnabled(), is(true));
        assertThat(opt.getIntervalInHours(), is(Default.OPTIMIZATION_INTERVAL_IN_HOURS));
        assertThat(opt.getInitialTimeExpression(), is(Default.OPTIMIZATION_INITIAL_TIME));
        assertThat(opt.getThreadPoolName(), is(Default.OPTIMIZATION_POOL));
    }

    @FixFor( "MODE-1988" )
    @Test
    public void shouldDisableDocumentOptimizationWithoutValidChildCountTargetValue() {
        Document docOpt = Schematic.newDocument(FieldName.OPTIMIZATION_CHILD_COUNT_TOLERANCE, 10);
        Document doc = Schematic.newDocument(FieldName.NAME, "repoName", FieldName.STORAGE,
                                             Schematic.newDocument(FieldName.DOCUMENT_OPTIMIZATION, docOpt));
        RepositoryConfiguration config = new RepositoryConfiguration(doc, "repoName");
        DocumentOptimization opt = config.getDocumentOptimization();
        assertThat(opt, is(notNullValue()));
        assertThat(opt.isEnabled(), is(false));
    }

    @Test
    @FixFor( "MODE-1683" )
    public void shouldReadJournalingConfiguration() {
        RepositoryConfiguration configuration = assertValid("config/repo-config-journaling.json");
        assertTrue(configuration.getJournaling().isEnabled());
    }

    @Test
    @FixFor( "MODE-2556" )
    public void journalShouldBeDisabledIfConfigurationSectionIsMissing() {
        Document doc = Schematic.newDocument(FieldName.NAME, "repoName");
        RepositoryConfiguration config = new RepositoryConfiguration(doc, "repoName");
        assertFalse(config.getJournaling().isEnabled()); 
    }

    @Test
    @FixFor( "MODE-2556" )
    public void journalShouldBeDisabledIfExplicitlyConfigured() {
        Document journalingConfig = Schematic.newDocument(FieldName.JOURNAL_ENABLED, false);
        Document doc = Schematic.newDocument(FieldName.NAME, "repoName", FieldName.JOURNALING, journalingConfig);
        RepositoryConfiguration config = new RepositoryConfiguration(doc, "repoName");
        assertFalse(config.getJournaling().isEnabled()); 
    }

    @Test
    public void shouldNotEnableClusteringIfMissingDocument() throws Exception {
        RepositoryConfiguration config = RepositoryConfiguration.read("{ 'name' = 'nm', 'storage' : {}}");
        RepositoryConfiguration.Clustering clusteringConfiguration = config.getClustering();
        assertFalse(clusteringConfiguration.isEnabled());
    }
    
    @Test
    public void shouldAllowClusteringToBeConfigured() throws Exception {
        String clusterName = "testCluster";
        String channelConfig = "someConfig";

        RepositoryConfiguration config = RepositoryConfiguration.read("{ \"clustering\" : {\"clusterName\":\"" + clusterName
                                                                      + "\", \"configuration\": \"" + channelConfig
                                                                      + "\"} }");
        RepositoryConfiguration.Clustering clusteringConfiguration = config.getClustering();
        assertTrue(clusteringConfiguration.isEnabled());
        assertEquals(clusterName, clusteringConfiguration.getClusterName());
        assertEquals(channelConfig, clusteringConfiguration.getConfiguration());
    }
    
    @Test
    public void shouldUseDefaultClusteringValues() throws Exception {
        RepositoryConfiguration config = RepositoryConfiguration.read("{ \"clustering\" : {} }");
        RepositoryConfiguration.Clustering clusteringConfiguration = config.getClustering();
        assertTrue(clusteringConfiguration.isEnabled());
        assertEquals(Default.CLUSTER_NAME, clusteringConfiguration.getClusterName());
        assertEquals(Default.CLUSTER_CONFIG, clusteringConfiguration.getConfiguration());
    }

    @Test
    public void shouldUseDefaultLockingTimeout() throws Exception {
        RepositoryConfiguration config = RepositoryConfiguration.read("{ 'name' = 'nm', 'storage' : {}}");
        assertEquals(Default.LOCK_TIMEOUT, config.getLockTimeoutMillis());
    }

    @Test
    public void shouldUseCustomLockingTimeout() throws Exception {
        RepositoryConfiguration config = RepositoryConfiguration.read("{ 'name' = 'nm', 'lockTimeoutMillis' : 100}");
        assertEquals(100, config.getLockTimeoutMillis());
    }
    
    protected RepositoryConfiguration assertValid( RepositoryConfiguration config ) {
        Problems results = config.validate();
        assertThat(results.toString(), results.hasProblems(), is(false));
        return config;
    }

    protected RepositoryConfiguration assertValidWithWarnings( int warnings,
                                                               RepositoryConfiguration config ) {
        Problems results = config.validate();
        assertThat(results.toString(), results.hasErrors(), is(false));
        assertThat(results.toString(), results.warningCount(), is(warnings));
        return config;
    }

    protected RepositoryConfiguration assertValid( String configContent ) {
        return assertValid(assertRead(configContent));

    }

    protected RepositoryConfiguration assertValidWithWarnings( int warnings,
                                                               String configContent ) {
        return assertValidWithWarnings(warnings, assertRead(configContent));

    }

    protected RepositoryConfiguration assertHasWarnings( int numberOfWarnings,
                                                         String configContent ) {
        return assertHasWarnings(numberOfWarnings, assertRead(configContent));
    }

    protected RepositoryConfiguration assertNotValid( int numberOfErrors,
                                                      RepositoryConfiguration config ) {
        Problems results = config.validate();
        assertThat(results.toString(), results.hasProblems(), is(true));
        assertThat(results.toString(), results.hasErrors(), is(true));
        assertThat(results.toString(), results.errorCount(), is(numberOfErrors));
        if (print) {
            System.out.println(results);
        }
        return config;
    }

    protected RepositoryConfiguration assertHasWarnings( int numberOfWarnings,
                                                         RepositoryConfiguration config ) {
        Problems results = config.validate();
        assertThat(results.toString(), results.warningCount(), is(numberOfWarnings));
        assertThat(results.toString(), results.hasWarnings(), is(numberOfWarnings != 0));
        if (print) {
            System.out.println(results);
        }
        return config;
    }

    protected void print( Object obj ) {
        if (print) {
            System.out.println(obj);
        }
    }

    protected RepositoryConfiguration assertNotValid( int numberOfErrors,
                                                      String configContent ) {
        return assertNotValid(numberOfErrors, assertRead(configContent));
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
