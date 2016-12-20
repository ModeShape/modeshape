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
package org.modeshape.jboss.service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.jcr.RepositoryException;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.security.ISecurityManagement;
import org.jgroups.Channel;
import org.jgroups.JChannel;
import org.jgroups.conf.XmlConfigurator;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.util.DelegatingClassLoader;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jboss.subsystem.MappedAttributeDefinition;
import org.modeshape.jcr.Environment;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.NoSuchRepositoryException;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.jcr.RepositoryStatistics;
import org.modeshape.schematic.Schematic;
import org.modeshape.schematic.document.Changes;
import org.modeshape.schematic.document.EditableArray;
import org.modeshape.schematic.document.EditableDocument;
import org.modeshape.schematic.document.Editor;
import org.wildfly.clustering.jgroups.ChannelFactory;

/**
 * A <code>RepositoryService</code> instance is the service responsible for initializing a {@link JcrRepository} in the ModeShape
 * engine using the information from the configuration.
 */
public class RepositoryService implements Service<JcrRepository>, Environment {

    private static final Logger LOG = Logger.getLogger(RepositoryService.class.getPackage().getName());

    private final InjectedValue<ModeShapeEngine> engineInjector = new InjectedValue<>();
    private final InjectedValue<BinaryStorage> binaryStorageInjector = new InjectedValue<>();
    private final InjectedValue<String> dataDirectoryPathInjector = new InjectedValue<>();
    private final InjectedValue<ModuleLoader> moduleLoaderInjector = new InjectedValue<>();
    private final InjectedValue<RepositoryStatistics> monitorInjector = new InjectedValue<>();
    private final InjectedValue<ChannelFactory> channelFactoryInjector = new InjectedValue<>();
    private final InjectedValue<ISecurityManagement> securityManagementServiceInjector = new InjectedValue<>();

    private final RepositoryConfiguration repositoryConfiguration;
    private final Set<ModuleIdentifier> additionalModuleDependencies = new LinkedHashSet<>();
    
    private String journalPath;
    private String journalRelativeTo;

    public RepositoryService(RepositoryConfiguration repositoryConfiguration, String additionalModuleDependencies) {
        this.repositoryConfiguration = repositoryConfiguration;
        if (!StringUtil.isBlank(additionalModuleDependencies)) {
            for (String moduleName : additionalModuleDependencies.split(",")) {
                ModuleIdentifier moduleId = moduleIdentifierFromName(moduleName);
                if (moduleId != null) {
                    this.additionalModuleDependencies.add(moduleId);
                }
            }
        }
    }

    @Override
    public JcrRepository getValue() throws IllegalStateException, IllegalArgumentException {
        try {
            return getEngine().getRepository(repositoryName());
        } catch (NoSuchRepositoryException e) {
            throw new IllegalStateException(e);
        }
    }

    private ModeShapeEngine getEngine() {
        return engineInjector.getValue();
    }

    @Override
    public ClassLoader getClassLoader( Object caller,
                                       String... classpathEntries ) {
        caller = Objects.requireNonNull(caller, "caller");
        Stream<ModuleIdentifier> optionalModuleIds = Arrays.stream(classpathEntries)
                                                           .filter(Objects::nonNull)
                                                           .map(this::moduleIdentifierFromName)
                                                           .filter(Objects::nonNull);
        List<ClassLoader> delegatingLoaders = Stream.concat(optionalModuleIds, this.additionalModuleDependencies.stream())
                                                    .map(moduleId -> {
                                                        try {
                                                            return moduleLoader().loadModule(moduleId).getClassLoader();
                                                        } catch (ModuleLoadException e) {
                                                            LOG.warnv(e,
                                                                    "Cannot load module from classpath entry with identifier: {0}",
                                                                    moduleId);
                                                            return null;
                                                        }
                                                    })
                                                    .filter(Objects::nonNull)
                                                    .collect(Collectors.toList());

        ClassLoader currentLoader = getClass().getClassLoader();
        ClassLoader callerLoader = caller.getClass().getClassLoader();
        if (!callerLoader.equals(currentLoader)) {
            // if the parent of fallback is the same as the current loader, just use that
            if (callerLoader.getParent().equals(currentLoader)) {
                currentLoader = callerLoader;
            } else {
                delegatingLoaders.add(callerLoader);
            }
        }

        return delegatingLoaders.isEmpty() ? currentLoader : new DelegatingClassLoader(currentLoader, delegatingLoaders);
    }
    
    private ModuleIdentifier moduleIdentifierFromName(String moduleName) {
        if (StringUtil.isBlank(moduleName)) {
            return null;
        }
        try {
            return ModuleIdentifier.fromString(moduleName.trim());
        } catch (IllegalArgumentException e) {
            LOG.warnv("{0} is not a valid module identifier", moduleName);
            return null;
        } 
    }

    @Override
    public void shutdown() {
        // Do nothing; this is the Environment's shutdown method
    }

    @Override
    public Channel getChannel(String name) throws Exception {
        LOG.debugv("getting JGroups channel named '{0}'", name);
        final ChannelFactory channelFactory = channelFactoryInjector.getOptionalValue();
        if (channelFactory != null) {
            LOG.debugv("JGroups configured to use server subsystem stack");
            // there is a cluster-stack attribute configured, so use that
            return channelFactory.createChannel(name);
        }
        // there is no cluster stack, so use a configured XML file 
        String clusterConfig = repositoryConfiguration.getClustering().getConfiguration();
        assert clusterConfig != null;
        LOG.debugv("reading JGroups config '{0}'", clusterConfig);
        InputStream configStream = new FileInputStream(clusterConfig);
        XmlConfigurator configurator = XmlConfigurator.getInstance(configStream);
        return new JChannel(configurator);
    }

    public final String repositoryName() {
        return repositoryConfiguration.getName();
    }

    @Override
    public void start( StartContext arg0 ) throws StartException {
        ModeShapeEngine engine = getEngine();

        try {
            final String repositoryName = repositoryName();

            // Get the binary storage configuration ...
            BinaryStorage binaryStorageConfig = binaryStorageInjector.getValue();
            assert binaryStorageConfig != null;
            EditableDocument binaryConfig = binaryStorageConfig.getBinaryConfiguration();

            // Create a new configuration document ...
            EditableDocument config = Schematic.newDocument(repositoryConfiguration.getDocument());
            config.getOrCreateDocument(FieldName.STORAGE).setDocument(FieldName.BINARY_STORAGE, binaryConfig);

            if (config.containsField(FieldName.JOURNALING)) {
                if (StringUtil.isBlank(this.journalRelativeTo)) {
                    this.journalRelativeTo = getDataDirectoryPathInjector().getValue();
                }
                if (StringUtil.isBlank(this.journalPath)) {
                    this.journalPath = "journal";
                }
                String finalJournalLocation = this.journalRelativeTo + "/" + this.journalPath;
                config.getDocument(FieldName.JOURNALING).setString(FieldName.JOURNAL_LOCATION, finalJournalLocation);
            }

            if (LOG.isDebugEnabled()) {
                LOG.debugv("ModeShape configuration for '{0}' repository: {1}", repositoryName, config);
                Problems problems = repositoryConfiguration.validate();
                if (!problems.isEmpty()) {
                    LOG.debugv("Problems with configuration for '{0}' repository: {1}", repositoryName, problems);
                }
            }
            
            // Create a new (updated) configuration ...
            RepositoryConfiguration updatedConfiguration = new RepositoryConfiguration(config, repositoryName);

            // Deploy the repository and use this as the environment ...
            engine.deploy(updatedConfiguration.with(this));
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop( StopContext context ) {
        ModeShapeEngine engine = getEngine();
        if (engine != null) {
            try {
                // Undeploy the repository ...
                engine.undeploy(repositoryName());
            } catch (NoSuchRepositoryException e) {
                // The repository doesn't exist, so no worries ...
            }
        }
    }

    /**
     * Immediately change and apply the specified field in the current repository configuration to the new value.
     * 
     * @param defn the attribute definition for the value; may not be null
     * @param newValue the new string value
     * @throws RepositoryException if there is a problem obtaining the repository configuration or applying the change
     * @throws OperationFailedException if there is a problem obtaining the raw value from the supplied model node
     */
    public void changeField( MappedAttributeDefinition defn,
                             ModelNode newValue ) throws RepositoryException, OperationFailedException {
        ModeShapeEngine engine = getEngine();
        String repositoryName = repositoryName();

        // Get a snapshot of the current configuration ...
        RepositoryConfiguration config = engine.getRepositoryConfiguration(repositoryName);

        // Now start to make changes ...
        Editor editor = config.edit();

        // Find the Document containing the field ...
        EditableDocument fieldContainer = editor;
        for (String fieldName : defn.getPathToContainerOfField()) {
            fieldContainer = editor.getOrCreateDocument(fieldName);
        }

        // Get the raw value from the model node ...
        Object rawValue = defn.getTypedValue(newValue);

        // Change the field ...
        String fieldName = defn.getFieldName();
        fieldContainer.set(fieldName, rawValue);

        // Apply the changes to the current configuration ...
        Changes changes = editor.getChanges();
        engine.update(repositoryName, changes);
    }

    /**
     * Immediately change and apply the specified index provider field in the current repository configuration to the new value.
     * 
     * @param defn the attribute definition for the value; may not be null
     * @param newValue the new string value
     * @param indexProviderName the name of the index provider
     * @throws RepositoryException if there is a problem obtaining the repository configuration or applying the change
     * @throws OperationFailedException if there is a problem obtaining the raw value from the supplied model node
     */
    public void changeIndexProviderField( MappedAttributeDefinition defn,
                                          ModelNode newValue,
                                          String indexProviderName ) throws RepositoryException, OperationFailedException {
        ModeShapeEngine engine = getEngine();
        String repositoryName = repositoryName();

        // Get a snapshot of the current configuration ...
        RepositoryConfiguration config = engine.getRepositoryConfiguration(repositoryName);

        // Now start to make changes ...
        Editor editor = config.edit();

        // Find the array of sequencer documents ...
        List<String> pathToContainer = defn.getPathToContainerOfField();
        EditableDocument providers = editor.getOrCreateDocument(pathToContainer.get(0));

        // The container should be an array ...
        for (String configuredProviderName : providers.keySet()) {
            // Look for the entry with a name that matches our sequencer name ...
            if (indexProviderName.equals(configuredProviderName)) {
                // All these entries should be nested documents ...
                EditableDocument provider = (EditableDocument)providers.get(configuredProviderName);

                // Change the field ...
                String fieldName = defn.getFieldName();
                // Get the raw value from the model node ...
                Object rawValue = defn.getTypedValue(newValue);
                // And update the field ...
                provider.set(fieldName, rawValue);
                break;
            }
        }

        // Get and apply the changes to the current configuration. Note that the 'update' call asynchronously
        // updates the configuration, and returns a Future<JcrRepository> that we could use if we wanted to
        // wait for the changes to take place. But we don't want/need to wait, so we'll not use the Future ...
        Changes changes = editor.getChanges();
        engine.update(repositoryName, changes);
    }

    /**
     * Immediately change and apply the specified index definition field in the current repository configuration to the new value.
     * 
     * @param defn the attribute definition for the value; may not be null
     * @param newValue the new string value
     * @param indexDefinitionName the name of the index definition
     * @throws RepositoryException if there is a problem obtaining the repository configuration or applying the change
     * @throws OperationFailedException if there is a problem obtaining the raw value from the supplied model node
     */
    public void changeIndexDefinitionField( MappedAttributeDefinition defn,
                                            ModelNode newValue,
                                            String indexDefinitionName ) throws RepositoryException, OperationFailedException {
        ModeShapeEngine engine = getEngine();
        String repositoryName = repositoryName();

        // Get a snapshot of the current configuration ...
        RepositoryConfiguration config = engine.getRepositoryConfiguration(repositoryName);

        // Now start to make changes ...
        Editor editor = config.edit();

        // Find the array of sequencer documents ...
        List<String> pathToContainer = defn.getPathToContainerOfField();
        EditableDocument indexes = editor.getOrCreateDocument(pathToContainer.get(0));

        // The container should be an array ...
        for (String configuredIndexName : indexes.keySet()) {
            // Look for the entry with a name that matches our sequencer name ...
            if (indexDefinitionName.equals(configuredIndexName)) {
                // All these entries should be nested documents ...
                EditableDocument provider = (EditableDocument)indexes.get(configuredIndexName);

                // Change the field ...
                String fieldName = defn.getFieldName();
                // Get the raw value from the model node ...
                Object rawValue = defn.getTypedValue(newValue);
                // And update the field ...
                provider.set(fieldName, rawValue);
                break;
            }
        }

        // Get and apply the changes to the current configuration. Note that the 'update' call asynchronously
        // updates the configuration, and returns a Future<JcrRepository> that we could use if we wanted to
        // wait for the changes to take place. But we don't want/need to wait, so we'll not use the Future ...
        Changes changes = editor.getChanges();
        engine.update(repositoryName, changes);
    }

    /**
     * Immediately change and apply the specified sequencer field in the current repository configuration to the new value.
     * 
     * @param defn the attribute definition for the value; may not be null
     * @param newValue the new string value
     * @param sequencerName the name of the sequencer
     * @throws RepositoryException if there is a problem obtaining the repository configuration or applying the change
     * @throws OperationFailedException if there is a problem obtaining the raw value from the supplied model node
     */
    public void changeSequencerField( MappedAttributeDefinition defn,
                                      ModelNode newValue,
                                      String sequencerName ) throws RepositoryException, OperationFailedException {
        ModeShapeEngine engine = getEngine();
        String repositoryName = repositoryName();

        // Get a snapshot of the current configuration ...
        RepositoryConfiguration config = engine.getRepositoryConfiguration(repositoryName);

        // Now start to make changes ...
        Editor editor = config.edit();

        // Find the array of sequencer documents ...
        List<String> pathToContainer = defn.getPathToContainerOfField();
        EditableDocument sequencing = editor.getOrCreateDocument(pathToContainer.get(0));
        EditableDocument sequencers = sequencing.getOrCreateArray(pathToContainer.get(1));

        // The container should be an array ...
        for (String configuredSequencerName : sequencers.keySet()) {
            // Look for the entry with a name that matches our sequencer name ...
            if (sequencerName.equals(configuredSequencerName)) {
                // All these entries should be nested documents ...
                EditableDocument sequencer = (EditableDocument)sequencers.get(configuredSequencerName);

                // Change the field ...
                String fieldName = defn.getFieldName();
                // Get the raw value from the model node ...
                Object rawValue = defn.getTypedValue(newValue);
                // And update the field ...
                sequencer.set(fieldName, rawValue);
                break;
            }
        }

        // Get and apply the changes to the current configuration. Note that the 'update' call asynchronously
        // updates the configuration, and returns a Future<JcrRepository> that we could use if we wanted to
        // wait for the changes to take place. But we don't want/need to wait, so we'll not use the Future ...
        Changes changes = editor.getChanges();
        engine.update(repositoryName, changes);
    }
    
    /**
     * Immediately change and apply the specified persistence field to the repository configuration
     * 
     * @param defn the attribute definition for the value; may not be null
     * @param newValue the new string value
     * @throws RepositoryException if there is a problem obtaining the repository configuration or applying the change
     * @throws OperationFailedException if there is a problem obtaining the raw value from the supplied model node
     */
    public void changePersistenceField(MappedAttributeDefinition defn,
                                       ModelNode newValue) throws RepositoryException, OperationFailedException {
        ModeShapeEngine engine = getEngine();
        String repositoryName = repositoryName();

        // Get a snapshot of the current configuration ...
        RepositoryConfiguration config = engine.getRepositoryConfiguration(repositoryName);

        // Now start to make changes ...
        Editor editor = config.edit();
        EditableDocument persistence = editor.getOrCreateDocument(FieldName.STORAGE).getOrCreateDocument(FieldName.PERSISTENCE);

        // Change the field ...
        String fieldName = defn.getFieldName();
        // Get the raw value from the model node ...
        Object rawValue = defn.getTypedValue(newValue);
        // And update the field ...
        persistence.set(fieldName, rawValue);        

        // Get and apply the changes to the current configuration. Note that the 'update' call asynchronously
        // updates the configuration, and returns a Future<JcrRepository> that we could use if we wanted to
        // wait for the changes to take place. But we don't want/need to wait, so we'll not use the Future ...
        Changes changes = editor.getChanges();
        engine.update(repositoryName, changes);
    }

    /**
     * Immediately change and apply the specified external source field in the current repository configuration to the new value.
     * 
     * @param defn the attribute definition for the value; may not be null
     * @param newValue the new string value
     * @param sourceName the name of the source
     * @throws RepositoryException if there is a problem obtaining the repository configuration or applying the change
     * @throws OperationFailedException if there is a problem obtaining the raw value from the supplied model node
     */
    public void changeSourceField( MappedAttributeDefinition defn,
                                   ModelNode newValue,
                                   String sourceName ) throws RepositoryException, OperationFailedException {
        ModeShapeEngine engine = getEngine();
        String repositoryName = repositoryName();

        // Get a snapshot of the current configuration ...
        RepositoryConfiguration config = engine.getRepositoryConfiguration(repositoryName);

        // Now start to make changes ...
        Editor editor = config.edit();

        // Find the array of sequencer documents ...
        EditableDocument externalSources = editor.getOrCreateDocument(FieldName.EXTERNAL_SOURCES);
        EditableDocument externalSource = externalSources.getDocument(sourceName);
        assert externalSource != null;

        // Change the field ...
        String fieldName = defn.getFieldName();
        // Get the raw value from the model node ...
        Object rawValue = defn.getTypedValue(newValue);
        // And update the field ...
        externalSource.set(fieldName, rawValue);

        // Get and apply the changes to the current configuration. Note that the 'update' call asynchronously
        // updates the configuration, and returns a Future<JcrRepository> that we could use if we wanted to
        // wait for the changes to take place. But we don't want/need to wait, so we'll not use the Future ...
        Changes changes = editor.getChanges();
        engine.update(repositoryName, changes);
    }

    /**
     * Immediately change and apply the specified extractor field in the current repository configuration to the new value.
     * 
     * @param defn the attribute definition for the value; may not be null
     * @param newValue the new string value
     * @param extractorName the name of the sequencer
     * @throws RepositoryException if there is a problem obtaining the repository configuration or applying the change
     * @throws OperationFailedException if there is a problem obtaining the raw value from the supplied model node
     */
    public void changeTextExtractorField( MappedAttributeDefinition defn,
                                          ModelNode newValue,
                                          String extractorName ) throws RepositoryException, OperationFailedException {
        ModeShapeEngine engine = getEngine();
        String repositoryName = repositoryName();

        // Get a snapshot of the current configuration ...
        RepositoryConfiguration config = engine.getRepositoryConfiguration(repositoryName);

        // Now start to make changes ...
        Editor editor = config.edit();

        // Find the array of sequencer documents ...
        List<String> pathToContainer = defn.getPathToContainerOfField();
        EditableDocument textExtracting = editor.getOrCreateDocument(pathToContainer.get(1));
        EditableDocument extractors = textExtracting.getOrCreateDocument(pathToContainer.get(2));

        // The container should be an array ...
        for (String configuredExtractorName : extractors.keySet()) {
            // Look for the entry with a name that matches our extractor name ...
            if (extractorName.equals(configuredExtractorName)) {
                // All these entries should be nested documents ...
                EditableDocument extractor = (EditableDocument)extractors.get(configuredExtractorName);
                // Change the field ...
                String fieldName = defn.getFieldName();
                // Get the raw value from the model node ...
                Object rawValue = defn.getTypedValue(newValue);
                // And update the field ...
                extractor.set(fieldName, rawValue);
                break;
            }
        }

        Changes changes = editor.getChanges();
        engine.update(repositoryName, changes);
    }

    /**
     * Immediately change and apply the specified authenticator field in the current repository configuration to the new value.
     * 
     * @param defn the attribute definition for the value; may not be null
     * @param newValue the new string value
     * @param authenticatorName the name of the authenticator
     * @throws RepositoryException if there is a problem obtaining the repository configuration or applying the change
     * @throws OperationFailedException if there is a problem obtaining the raw value from the supplied model node
     */
    public void changeAuthenticatorField( MappedAttributeDefinition defn,
                                          ModelNode newValue,
                                          String authenticatorName ) throws RepositoryException, OperationFailedException {
        ModeShapeEngine engine = getEngine();
        String repositoryName = repositoryName();

        // Get a snapshot of the current configuration ...
        RepositoryConfiguration config = engine.getRepositoryConfiguration(repositoryName);

        // Now start to make changes ...
        Editor editor = config.edit();

        // Find the array of sequencer documents ...
        EditableDocument security = editor.getOrCreateDocument(FieldName.SECURITY);
        EditableArray providers = security.getOrCreateArray(FieldName.PROVIDERS);

        // The container should be an array ...
        for (String configuredAuthenticatorName : providers.keySet()) {
            // Look for the entry with a name that matches our authenticator name ...
            if (authenticatorName.equals(configuredAuthenticatorName)) {
                // Find the document in the array with the name field value that matches ...
                boolean found = false;
                for (Object nested : providers) {
                    if (nested instanceof EditableDocument) {
                        EditableDocument doc = (EditableDocument)nested;
                        if (doc.getString(FieldName.NAME).equals(configuredAuthenticatorName)) {
                            // Change the field ...
                            String fieldName = defn.getFieldName();
                            // Get the raw value from the model node ...
                            Object rawValue = defn.getTypedValue(newValue);
                            // And update the field ...
                            doc.set(fieldName, rawValue);
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    // Add the nested document ...
                    EditableDocument doc = Schematic.newDocument();
                    doc.set(FieldName.NAME, configuredAuthenticatorName);
                    // Set the field ...
                    String fieldName = defn.getFieldName();
                    // Get the raw value from the model node ...
                    Object rawValue = defn.getTypedValue(newValue);
                    // And update the field ...
                    doc.set(fieldName, rawValue);
                    providers.add(doc);
                }
                break;
            }
        }

        Changes changes = editor.getChanges();
        engine.update(repositoryName, changes);
    }

    /**
     * @return the injector used to set the configuration details for the binaries storage
     */
    public InjectedValue<BinaryStorage> getBinaryStorageInjector() {
        return binaryStorageInjector;
    }

    /**
     * @return the injector used to get the repository statistics (never <code>null</code>)
     */
    public InjectedValue<RepositoryStatistics> getMonitorInjector() {
        return this.monitorInjector;
    }

    /**
     * @return the injector used to set the {@link ModeShapeEngine} reference
     */
    public InjectedValue<ModeShapeEngine> getEngineInjector() {
        return engineInjector;
    }

    /**
     * @return the injector used to set the data directory for this repository
     */
    public InjectedValue<String> getDataDirectoryPathInjector() {
        return dataDirectoryPathInjector;
    }

    /**
     * @return the injector used to set the jboss module loader
     */
    public InjectedValue<ModuleLoader> getModuleLoaderInjector() {
        return moduleLoaderInjector;
    }
    
    public InjectedValue<ISecurityManagement> getSecurityManagementServiceInjector() {
        return securityManagementServiceInjector;
    }

    public InjectedValue<ChannelFactory> getChannelFactoryInjector() {
        return channelFactoryInjector;
    }

    private ModuleLoader moduleLoader() {
        return moduleLoaderInjector.getValue();
    }

    /**
     * Sets the path (relative) of the journal.
     * 
     * @param journalPath a {@link String}, may not be null
     */
    public void setJournalPath( String journalPath ) {
        this.journalPath = journalPath;
    }

    /**
     * Sets the base folder of the journal
     * 
     * @param journalRelativeTo a {@link String}, may not be null
     */
    public void setJournalRelativeTo( String journalRelativeTo ) {
        this.journalRelativeTo = journalRelativeTo;
    }
}
