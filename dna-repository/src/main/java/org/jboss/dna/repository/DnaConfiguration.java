/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.collection.SimpleProblems;
import org.jboss.dna.common.component.ClassLoaderFactory;
import org.jboss.dna.common.component.StandardClassLoaderFactory;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.Workspace;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.mimetype.MimeTypeDetector;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathExpression;
import org.jboss.dna.graph.property.PathNotFoundException;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.basic.RootPath;
import org.jboss.dna.graph.request.InvalidWorkspaceException;
import org.jboss.dna.graph.sequencer.StreamSequencer;
import org.xml.sax.SAXException;

/**
 * A configuration builder for a {@link DnaEngine}. This class is an internal domain-specific language (DSL), and is designed to
 * be used in a traditional way or in a method-chained manner:
 * 
 * <pre>
 * configuration.repositorySource(&quot;Source1&quot;).setClass(InMemoryRepositorySource.class).setDescription(&quot;description&quot;);
 * configuration.mimeTypeDetector(&quot;detector&quot;).setClass(ExtensionBasedMimeTypeDetector.class).setDescription(&quot;default detector&quot;);
 * configuration.sequencer(&quot;MicrosoftDocs&quot;)
 *              .setClass(&quot;org.jboss.dna.sequencer.msoffice.MSOfficeMetadataSequencer&quot;)
 *              .setDescription(&quot;Our primary sequencer for all .doc files&quot;)
 *              .sequencingFrom(&quot;/public//(*.(doc|xml|ppt)[*]/jcr:content[@jcr:data]&quot;)
 *              .andOutputtingTo(&quot;/documents/$1&quot;);
 * configuration.save();
 * </pre>
 */
@NotThreadSafe
public class DnaConfiguration {

    public static final String DEFAULT_WORKSPACE_NAME = "";
    public static final String DEFAULT_PATH = "/";
    public static final String DEFAULT_CONFIGURATION_SOURCE_NAME = "DNA Configuration Repository";

    private final ExecutionContext context;
    private final Problems problems = new SimpleProblems();
    private ConfigurationDefinition configurationContent;
    private Graph.Batch changes;

    private final Map<String, SequencerDefinition<? extends DnaConfiguration>> sequencerDefinitions = new HashMap<String, SequencerDefinition<? extends DnaConfiguration>>();
    private final Map<String, RepositorySourceDefinition<? extends DnaConfiguration>> repositorySourceDefinitions = new HashMap<String, RepositorySourceDefinition<? extends DnaConfiguration>>();
    private final Map<String, MimeTypeDetectorDefinition<? extends DnaConfiguration>> mimeTypeDetectorDefinitions = new HashMap<String, MimeTypeDetectorDefinition<? extends DnaConfiguration>>();

    /**
     * Create a new configuration, using a default-constructed {@link ExecutionContext}.
     */
    public DnaConfiguration() {
        this(new ExecutionContext());
    }

    /**
     * Create a new configuration using the supplied {@link ExecutionContext}.
     * 
     * @param context the execution context
     * @throws IllegalArgumentException if the path is null or empty
     */
    public DnaConfiguration( ExecutionContext context ) {
        CheckArg.isNotNull(context, "context");
        this.context = context;

        // Create the in-memory repository source in which the content will be stored ...
        InMemoryRepositorySource source = new InMemoryRepositorySource();
        source.setName(DEFAULT_CONFIGURATION_SOURCE_NAME);
        source.setDefaultWorkspaceName(DEFAULT_WORKSPACE_NAME);

        // The file was imported successfully, so now create the content information ...
        configurationContent = new ConfigurationDefinition(source, null, null, context, null);
    }

    /**
     * Load the configuration from a file at the given path.
     * 
     * @param pathToConfigurationFile the path the file containing the configuration information
     * @return this configuration object, for convenience and method chaining
     * @throws IOException if there is an error or problem reading the file at the supplied location
     * @throws SAXException if the file is not a valid XML format
     * @throws IllegalArgumentException if the path is null or empty
     */
    public DnaConfiguration loadFrom( String pathToConfigurationFile ) throws IOException, SAXException {
        CheckArg.isNotEmpty(pathToConfigurationFile, "pathToConfigurationFile");
        return loadFrom(pathToConfigurationFile, DEFAULT_PATH);
    }

    /**
     * Load the configuration from a file at the given path.
     * 
     * @param pathToConfigurationFile the path the file containing the configuration information
     * @param path path within the content to the parent containing the configuration information, or null if the
     *        {@link #DEFAULT_PATH default path} should be used
     * @return this configuration object, for convenience and method chaining
     * @throws IOException if there is an error or problem reading the file at the supplied location
     * @throws SAXException if the file is not a valid XML format
     * @throws IllegalArgumentException if the path is null or empty
     */
    public DnaConfiguration loadFrom( String pathToConfigurationFile,
                                      String path ) throws IOException, SAXException {
        CheckArg.isNotEmpty(pathToConfigurationFile, "pathToConfigurationFile");
        return loadFrom(new File(pathToConfigurationFile), path);
    }

    /**
     * Load the configuration from a file.
     * 
     * @param configurationFile the file containing the configuration information
     * @return this configuration object, for convenience and method chaining
     * @throws IOException if there is an error or problem reading the supplied file
     * @throws SAXException if the file is not a valid XML format
     * @throws IllegalArgumentException if the file reference is null
     */
    public DnaConfiguration loadFrom( File configurationFile ) throws IOException, SAXException {
        CheckArg.isNotNull(configurationFile, "configurationFile");
        return loadFrom(configurationFile, DEFAULT_PATH);
    }

    /**
     * Load the configuration from a file.
     * 
     * @param configurationFile the file containing the configuration information
     * @param path path within the content to the parent containing the configuration information, or null if the
     *        {@link #DEFAULT_PATH default path} should be used
     * @return this configuration object, for convenience and method chaining
     * @throws IOException if there is an error or problem reading the supplied file
     * @throws SAXException if the file is not a valid XML format
     * @throws IllegalArgumentException if the file reference is null
     */
    public DnaConfiguration loadFrom( File configurationFile,
                                      String path ) throws IOException, SAXException {
        CheckArg.isNotNull(configurationFile, "configurationFile");
        InputStream stream = new FileInputStream(configurationFile);
        try {
            return loadFrom(stream, path);
        } finally {
            stream.close();
        }
    }

    /**
     * Load the configuration from a file at the supplied URL.
     * 
     * @param urlToConfigurationFile the URL of the file containing the configuration information
     * @return this configuration object, for convenience and method chaining
     * @throws IOException if there is an error or problem reading the file at the supplied URL
     * @throws SAXException if the file is not a valid XML format
     * @throws IllegalArgumentException if the URL is null
     */
    public DnaConfiguration loadFrom( URL urlToConfigurationFile ) throws IOException, SAXException {
        CheckArg.isNotNull(urlToConfigurationFile, "urlToConfigurationFile");
        return loadFrom(urlToConfigurationFile, DEFAULT_PATH);
    }

    /**
     * Load the configuration from a file at the supplied URL.
     * 
     * @param urlToConfigurationFile the URL of the file containing the configuration information
     * @param path path within the content to the parent containing the configuration information, or null if the
     *        {@link #DEFAULT_PATH default path} should be used
     * @return this configuration object, for convenience and method chaining
     * @throws IOException if there is an error or problem reading the file at the supplied URL
     * @throws SAXException if the file is not a valid XML format
     * @throws IllegalArgumentException if the URL is null
     */
    public DnaConfiguration loadFrom( URL urlToConfigurationFile,
                                      String path ) throws IOException, SAXException {
        CheckArg.isNotNull(urlToConfigurationFile, "urlToConfigurationFile");
        InputStream stream = urlToConfigurationFile.openStream();
        try {
            return loadFrom(stream, path);
        } finally {
            stream.close();
        }
    }

    /**
     * Load the configuration from a file at the supplied URL.
     * 
     * @param configurationFileInputStream the stream with the configuration information
     * @return this configuration object, for convenience and method chaining
     * @throws IOException if there is an error or problem reading the file at the supplied URL
     * @throws SAXException if the file is not a valid XML format
     * @throws IllegalArgumentException if the stream is null
     */
    public DnaConfiguration loadFrom( InputStream configurationFileInputStream ) throws IOException, SAXException {
        CheckArg.isNotNull(configurationFileInputStream, "configurationFileInputStream");
        return loadFrom(configurationFileInputStream, DEFAULT_PATH);
    }

    /**
     * Load the configuration from a file at the supplied URL.
     * 
     * @param configurationFileInputStream the stream with the configuration information
     * @param path path within the content to the parent containing the configuration information, or null if the
     *        {@link #DEFAULT_PATH default path} should be used
     * @return this configuration object, for convenience and method chaining
     * @throws IOException if there is an error or problem reading the file at the supplied URL
     * @throws SAXException if the file is not a valid XML format
     * @throws IllegalArgumentException if the stream is null
     */
    public DnaConfiguration loadFrom( InputStream configurationFileInputStream,
                                      String path ) throws IOException, SAXException {
        CheckArg.isNotNull(configurationFileInputStream, "configurationFileInputStream");

        // Create the in-memory repository source in which the content will be stored ...
        InMemoryRepositorySource source = new InMemoryRepositorySource();
        source.setName(DEFAULT_CONFIGURATION_SOURCE_NAME);
        source.setDefaultWorkspaceName(DEFAULT_WORKSPACE_NAME);

        // Import the information into the source ...
        Path pathToParent = path(path != null ? path : DEFAULT_PATH);
        Graph graph = Graph.create(source, context);
        graph.importXmlFrom(configurationFileInputStream).skippingRootElement(true).into(pathToParent);

        // The file was imported successfully, so now create the content information ...
        configurationContent = new ConfigurationDefinition(source, null, pathToParent, context, null);
        return this;
    }

    /**
     * Load the configuration from the repository content using the supplied repository source. This method assumes that the
     * supplied source has already been configured and is ready to {@link RepositorySource#getConnection() create connections}.
     * Also, the default workspace of the source will be used, and the configuration content may be found directly under the root
     * node.
     * 
     * @param source the source that defines the repository with the configuration content
     * @return this configuration object, for convenience and method chaining
     * @throws IllegalArgumentException if the source is null
     */
    public DnaConfiguration loadFrom( RepositorySource source ) {
        return loadFrom(source, null, null);
    }

    /**
     * Load the configuration from the repository content using the workspace in the supplied repository source. This method
     * assumes that the supplied source has already been configured and is ready to {@link RepositorySource#getConnection() create
     * connections}. Also, the configuration content may be found directly under the root node.
     * 
     * @param source the source that defines the repository with the configuration content
     * @param workspaceName the name of the workspace with the configuration content, or null if the source's default workspace
     *        should be used
     * @return this configuration object, for convenience and method chaining
     * @throws IllegalArgumentException if the source is null
     */
    public DnaConfiguration loadFrom( RepositorySource source,
                                      String workspaceName ) {
        CheckArg.isNotNull(source, "source");
        return loadFrom(source, workspaceName, null);
    }

    /**
     * Load the configuration from the repository content at the supplied path in the workspace in the supplied repository source.
     * This method assumes that the supplied source has already been configured and is ready to
     * {@link RepositorySource#getConnection() create connections}.
     * 
     * @param source the source that defines the repository with the configuration content
     * @param workspaceName the name of the workspace with the configuration content, or null if the source's default workspace
     *        should be used
     * @param pathInWorkspace the path to the parent node under which the configuration content may be found, or null if the
     *        content may be found under the root node
     * @return this configuration object, for convenience and method chaining
     * @throws IllegalArgumentException if the source is null
     */
    public DnaConfiguration loadFrom( RepositorySource source,
                                      String workspaceName,
                                      String pathInWorkspace ) {
        CheckArg.isNotNull(source, "source");

        // Verify connectivity ...
        Graph graph = Graph.create(source, context);
        if (workspaceName != null) {
            Workspace workspace = null;
            try {
                workspace = graph.useWorkspace(workspaceName); // should throw exception if not connectable
            } catch (InvalidWorkspaceException e) {
                // Try creating the workspace ...
                workspace = graph.createWorkspace().named(workspaceName);
            }
            assert workspace.getRoot() != null;
        }

        // Verify the path ...
        Path path = pathInWorkspace != null ? path(pathInWorkspace) : path(DEFAULT_PATH);
        Node parent = graph.getNodeAt(path);
        assert parent != null;

        // Now create the content information ...
        configurationContent = new ConfigurationDefinition(source, workspaceName, path, context, null);
        return this;
    }

    /**
     * Get the immutable representation of the information defining where the configuration content can be found.
     * 
     * @return the configuration definition
     */
    public ConfigurationDefinition getConfigurationDefinition() {
        return configurationContent;
    }

    protected ExecutionContext getExecutionContext() {
        return configurationContent.getContext();
    }

    protected Path path() {
        return configurationContent.getPath();
    }

    protected Path path( String path ) {
        return context.getValueFactories().getPathFactory().create(path);
    }

    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    /**
     * Get the problems (if any) that are associated with this configuration.
     * 
     * @return the problems
     */
    public Problems getProblems() {
        return problems;
    }

    protected Graph.Batch changes() {
        if (changes == null) {
            ConfigurationDefinition content = getConfigurationDefinition();
            Graph graph = Graph.create(content.getRepositorySource(), content.getContext());
            if (content.getWorkspace() != null) {
                graph.useWorkspace(content.getWorkspace());
            }
            changes = graph.batch();
        }
        return changes;
    }

    /**
     * Determine if there are any unsaved changes to this configuration that must be {@link #save() saved} before they take
     * effect.
     * 
     * @return true if a {@link #save()} is required, or false no changes have been made to the configuration since the last
     *         {@link #save()}
     */
    public boolean hasChanges() {
        Graph.Batch changes = this.changes;
        return changes != null && changes.isExecuteRequired();
    }

    /**
     * Persist any unsaved changes that have been made to this configuration. This method has no effect if there are currently
     * {@link #hasChanges() no unsaved changes}.
     * 
     * @return this configuration, for method chaining purposes
     */
    public DnaConfiguration save() {
        Graph.Batch changes = this.changes;
        if (changes != null && changes.isExecuteRequired()) {
            changes.execute();
        }
        this.changes = null;
        sequencerDefinitions.clear();
        mimeTypeDetectorDefinitions.clear();
        repositorySourceDefinitions.clear();
        return this;
    }

    /**
     * Specify the {@link ClassLoaderFactory} that should be used to load the classes for the various components. Most of the
     * definitions can specify the {@link LoadedFrom#loadedFrom(String...) classpath} that should be used, and that classpath is
     * passed to the supplied ClassLoaderFactory instance to obtain a {@link ClassLoader} for the class.
     * <p>
     * If not called, this configuration will use the class loader that loaded this configuration's class.
     * </p>
     * 
     * @param classLoaderFactory the class loader factory implementation, or null if the classes should be loaded using the class
     *        loader of this object
     * @return this configuration, for method chaining purposes
     */
    public DnaConfiguration withClassLoaderFactory( ClassLoaderFactory classLoaderFactory ) {
        this.configurationContent = this.configurationContent.with(classLoaderFactory);
        return this;
    }

    protected Set<String> getNamesOfComponentsUnder( Name parentName ) {
        Set<String> names = new HashSet<String>();
        try {
            ConfigurationDefinition content = this.getConfigurationDefinition();
            Path path = context.getValueFactories().getPathFactory().create(content.getPath(), parentName);
            for (Location child : content.graph().getChildren().of(path)) {
                names.add(child.getPath().getLastSegment().getString(context.getNamespaceRegistry()));
            }
        } catch (PathNotFoundException e) {
            // Nothing has been saved yet ...
        }
        return names;
    }

    /**
     * Get the list of MIME type detector definitions.
     * 
     * @return the unmodifiable set of definitions; never null but possibly empty if there are no definitions
     */
    public Set<MimeTypeDetectorDefinition<? extends DnaConfiguration>> mimeTypeDetectors() {
        // Get the children under the 'dna:mimeTypeDetectors' node ...
        Set<String> names = getNamesOfComponentsUnder(DnaLexicon.MIME_TYPE_DETECTORS);
        names.addAll(this.mimeTypeDetectorDefinitions.keySet());
        Set<MimeTypeDetectorDefinition<? extends DnaConfiguration>> results = new HashSet<MimeTypeDetectorDefinition<? extends DnaConfiguration>>();
        for (String name : names) {
            results.add(mimeTypeDetector(name));
        }
        return Collections.unmodifiableSet(results);
    }

    /**
     * Get the list of repository source definitions.
     * 
     * @return the unmodifiable set of definitions; never null but possibly empty if there are no definitions
     */
    public Set<RepositorySourceDefinition<? extends DnaConfiguration>> repositorySources() {
        // Get the children under the 'dna:mimeTypeDetectors' node ...
        Set<String> names = getNamesOfComponentsUnder(DnaLexicon.SOURCES);
        names.addAll(this.repositorySourceDefinitions.keySet());
        Set<RepositorySourceDefinition<? extends DnaConfiguration>> results = new HashSet<RepositorySourceDefinition<? extends DnaConfiguration>>();
        for (String name : names) {
            results.add(repositorySource(name));
        }
        return Collections.unmodifiableSet(results);
    }

    /**
     * Get the list of sequencer definitions.
     * 
     * @return the unmodifiable set of definitions; never null but possibly empty if there are no definitions
     */
    public Set<SequencerDefinition<? extends DnaConfiguration>> sequencers() {
        // Get the children under the 'dna:mimeTypeDetectors' node ...
        Set<String> names = getNamesOfComponentsUnder(DnaLexicon.SEQUENCERS);
        names.addAll(this.sequencerDefinitions.keySet());
        Set<SequencerDefinition<? extends DnaConfiguration>> results = new HashSet<SequencerDefinition<? extends DnaConfiguration>>();
        for (String name : names) {
            results.add(sequencer(name));
        }
        return Collections.unmodifiableSet(results);
    }

    /**
     * Obtain or create a definition for the {@link MimeTypeDetector MIME type detector} with the supplied name or identifier. A
     * new definition will be created if there currently is no MIME type detector defined with the supplied name.
     * 
     * @param name the name or identifier of the detector
     * @return the details of the MIME type detector definition; never null
     */
    public MimeTypeDetectorDefinition<? extends DnaConfiguration> mimeTypeDetector( String name ) {
        return mimeTypeDetectorDefinition(this, name);
    }

    /**
     * Obtain or create a definition for the {@link RepositorySource} with the supplied name or identifier. A new definition will
     * be created if there currently is no repository source defined with the supplied name.
     * 
     * @param name the name or identifier of the repository source
     * @return the details of the repository source definition; never null
     */
    public RepositorySourceDefinition<? extends DnaConfiguration> repositorySource( String name ) {
        return repositorySourceDefinition(this, name);
    }

    /**
     * Obtain or create a definition for the {@link StreamSequencer sequencer} with the supplied name or identifier. A new
     * definition will be created if there currently is no sequencer defined with the supplied name.
     * 
     * @param name the name or identifier of the sequencer
     * @return the details of the sequencer definition; never null
     */
    public SequencerDefinition<? extends DnaConfiguration> sequencer( String name ) {
        return sequencerDefinition(this, name);
    }

    /**
     * Convenience method to make the code that sets up this configuration easier to read. This method simply returns this object.
     * 
     * @return this configuration component; never null
     */
    public DnaConfiguration and() {
        return this;
    }

    /**
     * Construct an engine that reflects the current state of this configuration. This method always creates a new instance.
     * 
     * @return the resulting engine; never null
     */
    public DnaEngine build() {
        save();
        return new DnaEngine(getExecutionContext(), getConfigurationDefinition());
    }

    /**
     * Interface that defines the ability to obtain the configuration component.
     * 
     * @param <ReturnType> the interface returned from these methods
     */
    public interface Returnable<ReturnType> {
        /**
         * Return the configuration component.
         * 
         * @return the configuration component; never null
         */
        ReturnType and();
    }

    /**
     * Interface that defines the ability to remove the configuration component.
     * 
     * @param <ReturnType> the configuration interface returned from these methods
     */
    public interface Removable<ReturnType> {
        /**
         * Remove this configuration component.
         * 
         * @return the configuration; never null
         */
        ReturnType remove();
    }

    /**
     * The interface used to set a description on a component.
     * 
     * @param <ReturnType> the interface returned from these methods
     */
    public interface SetDescription<ReturnType> {
        /**
         * Specify the description of this component.
         * 
         * @param description the description; may be null or empty
         * @return the next component to continue configuration; never null
         */
        ReturnType setDescription( String description );

        /**
         * Get the description of this component.
         * 
         * @return the description, or null if there is no description
         */
        String getDescription();
    }

    /**
     * Interface for configuring the JavaBean-style properties of an object.
     * 
     * @param <ReturnType> the interface returned after the property has been set.
     * @author Randall Hauch
     */
    public interface SetProperties<ReturnType> {
        /**
         * Set the property value to an integer.
         * 
         * @param beanPropertyName the name of the JavaBean-style property (e.g., "retryLimit")
         * @param value the new value for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setProperty( String beanPropertyName,
                                int value );

        /**
         * Set the property value to a long number.
         * 
         * @param beanPropertyName the name of the JavaBean-style property (e.g., "retryLimit")
         * @param value the new value for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setProperty( String beanPropertyName,
                                long value );

        /**
         * Set the property value to a short.
         * 
         * @param beanPropertyName the name of the JavaBean-style property (e.g., "retryLimit")
         * @param value the new value for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setProperty( String beanPropertyName,
                                short value );

        /**
         * Set the property value to a boolean.
         * 
         * @param beanPropertyName the name of the JavaBean-style property (e.g., "retryLimit")
         * @param value the new value for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setProperty( String beanPropertyName,
                                boolean value );

        /**
         * Set the property value to a float.
         * 
         * @param beanPropertyName the name of the JavaBean-style property (e.g., "retryLimit")
         * @param value the new value for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setProperty( String beanPropertyName,
                                float value );

        /**
         * Set the property value to a double.
         * 
         * @param beanPropertyName the name of the JavaBean-style property (e.g., "retryLimit")
         * @param value the new value for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setProperty( String beanPropertyName,
                                double value );

        /**
         * Set the property value to a string.
         * 
         * @param beanPropertyName the name of the JavaBean-style property (e.g., "retryLimit")
         * @param value the new value for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setProperty( String beanPropertyName,
                                String value );

        /**
         * Set the property value to a string.
         * 
         * @param beanPropertyName the name of the JavaBean-style property (e.g., "retryLimit")
         * @param value the first string value for the property
         * @param additionalValues the additional string values for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setProperty( String beanPropertyName,
                                String value,
                                String... additionalValues );

        /**
         * Set the property value to an object.
         * 
         * @param beanPropertyName the name of the JavaBean-style property (e.g., "retryLimit")
         * @param value the new value for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setProperty( String beanPropertyName,
                                Object value );

        /**
         * Set the property values to an object.
         * 
         * @param beanPropertyName the name of the JavaBean-style property (e.g., "retryLimit")
         * @param values the array of new values for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setProperty( String beanPropertyName,
                                Object[] values );

        /**
         * Get the property.
         * 
         * @param beanPropertyName the name of the JavaBean-style property (e.g., "retryLimit")
         * @return the property object, or null if there is no such property
         */
        Property getProperty( String beanPropertyName );
    }

    /**
     * The interface used to configure the class used for a component.
     * 
     * @param <ComponentClassType> the class or interface that the component is to implement
     * @param <ReturnType> the interface returned from these methods
     */
    public interface ChooseClass<ComponentClassType, ReturnType> {

        /**
         * Specify the name of the class that should be instantiated for the instance. The classpath information will need to be
         * defined using the returned interface.
         * 
         * @param classname the name of the class that should be instantiated
         * @return the interface used to define the classpath information; never null
         * @throws IllegalArgumentException if the class name is null, empty, blank, or not a valid class name
         */
        LoadedFrom<ReturnType> usingClass( String classname );

        /**
         * Specify the class that should be instantiated for the instance. Because the class is already available to this class
         * loader, there is no need to specify the classloader information.
         * 
         * @param clazz the class that should be instantiated
         * @return the next component to continue configuration; never null
         * @throws DnaConfigurationException if the class could not be accessed and instantiated (if needed)
         * @throws IllegalArgumentException if the class reference is null
         */
        ReturnType usingClass( Class<? extends ComponentClassType> clazz );
    }

    /**
     * Interface for specifying from where the component's class is to be loaded.
     * 
     * @param <ReturnType> the interface returned from these methods
     */
    public interface LoadedFrom<ReturnType> {
        /**
         * Specify the names of the classloaders that form the classpath for the component, from which the component's class (and
         * its dependencies) can be loaded. The names correspond to the names supplied to the
         * {@link ExecutionContext#getClassLoader(String...)} methods.
         * 
         * @param classPathNames the names for the classloaders, as passed to the {@link ClassLoaderFactory} implementation (e.g.,
         *        the {@link ExecutionContext}).
         * @return the next component to continue configuration; never null
         * @see #loadedFromClasspath()
         * @see ExecutionContext#getClassLoader(String...)
         */
        ReturnType loadedFrom( String... classPathNames );

        /**
         * Specify that the component (and its dependencies) will be found on the current (or
         * {@link Thread#getContextClassLoader() current context}) classloader.
         * 
         * @return the next component to continue configuration; never null
         * @see #loadedFrom(String...)
         * @see ExecutionContext#getClassLoader(String...)
         */
        ReturnType loadedFromClasspath();
    }

    /**
     * Interface for a component that has a name.
     */
    public interface HasName {
        /**
         * Get the name.
         * 
         * @return the name; never null
         */
        String getName();
    }

    /**
     * Interface used to set up and define a MIME type detector instance.
     * 
     * @param <ReturnType> the type of the configuration component that owns this definition object
     */
    public interface MimeTypeDetectorDefinition<ReturnType>
        extends Returnable<ReturnType>, SetDescription<MimeTypeDetectorDefinition<ReturnType>>,
        SetProperties<MimeTypeDetectorDefinition<ReturnType>>,
        ChooseClass<MimeTypeDetector, MimeTypeDetectorDefinition<ReturnType>>, Removable<ReturnType> {
    }

    /**
     * Interface used to set up and define a RepositorySource instance.
     * 
     * @param <ReturnType> the type of the configuration component that owns this definition object
     */
    public interface RepositorySourceDefinition<ReturnType>
        extends Returnable<ReturnType>, SetDescription<RepositorySourceDefinition<ReturnType>>,
        SetProperties<RepositorySourceDefinition<ReturnType>>,
        ChooseClass<RepositorySource, RepositorySourceDefinition<ReturnType>>, Removable<ReturnType>, HasName {

        /**
         * Set the retry limit on the repository source. This is equivalent to calling {@link #setProperty(String, int)} with "
         * {@link DnaLexicon#RETRY_LIMIT dna:retryLimit}" as the property name.
         * 
         * @param retryLimit the retry limit
         * @return this definition, for method chaining purposes
         * @see RepositorySource#setRetryLimit(int)
         */
        RepositorySourceDefinition<ReturnType> setRetryLimit( int retryLimit );
    }

    /**
     * Interface used to set up and define a {@link StreamSequencer sequencer} instance.
     * 
     * @param <ReturnType> the type of the configuration component that owns this definition object
     */
    public interface SequencerDefinition<ReturnType>
        extends Returnable<ReturnType>, SetDescription<SequencerDefinition<ReturnType>>,
        SetProperties<SequencerDefinition<ReturnType>>, ChooseClass<StreamSequencer, SequencerDefinition<ReturnType>>,
        Removable<ReturnType> {

        /**
         * Specify the input {@link PathExpression path expression} represented as a string, which determines when this sequencer
         * will be executed.
         * 
         * @param inputPathExpression the path expression for nodes that, when they change, will be passed as an input to the
         *        sequencer
         * @return the interface used to specify the output path expression; never null
         */
        PathExpressionOutput<ReturnType> sequencingFrom( String inputPathExpression );

        /**
         * Specify the input {@link PathExpression path expression}, which determines when this sequencer will be executed.
         * 
         * @param inputPathExpression the path expression for nodes that, when they change, will be passed as an input to the
         *        sequencer
         * @return the interface used to continue specifying the configuration of the sequencer
         */
        SequencerDefinition<ReturnType> sequencingFrom( PathExpression inputPathExpression );

        /**
         * Get the path expressions from the saved configuration.
         * 
         * @return the set of path expressions; never null but possibly empty
         */
        Set<PathExpression> getPathExpressions();
    }

    /**
     * Interface used to specify the output path expression for a
     * {@link DnaConfiguration.SequencerDefinition#sequencingFrom(PathExpression) sequencer configuration}.
     * 
     * @param <ReturnType>
     */
    public interface PathExpressionOutput<ReturnType> {
        /**
         * Specify the output {@link PathExpression path expression}, which determines where this sequencer's output will be
         * placed.
         * 
         * @param outputExpression the path expression for the location(s) where output generated by the sequencer is to be placed
         * @return the interface used to continue specifying the configuration of the sequencer
         */
        SequencerDefinition<ReturnType> andOutputtingTo( String outputExpression );
    }

    /**
     * Utility method to construct a definition object for the detector with the supplied name and return type.
     * 
     * @param <ReturnType> the type of the return object
     * @param returnObject the return object
     * @param name the name of the detector
     * @return the definition for the detector
     */
    @SuppressWarnings( "unchecked" )
    protected <ReturnType extends DnaConfiguration> MimeTypeDetectorDefinition<ReturnType> mimeTypeDetectorDefinition( ReturnType returnObject,
                                                                                                                       String name ) {
        MimeTypeDetectorDefinition<ReturnType> definition = (MimeTypeDetectorDefinition<ReturnType>)mimeTypeDetectorDefinitions.get(name);
        if (definition == null) {
            definition = new MimeTypeDetectorBuilder<ReturnType>(returnObject, changes(), path(), DnaLexicon.MIME_TYPE_DETECTORS,
                                                                 name(name));
            mimeTypeDetectorDefinitions.put(name, definition);
        }
        return definition;
    }

    /**
     * Utility method to construct a definition object for the repository source with the supplied name and return type.
     * 
     * @param <ReturnType> the type of the return object
     * @param returnObject the return object
     * @param name the name of the repository source
     * @return the definition for the repository source
     */
    @SuppressWarnings( "unchecked" )
    protected <ReturnType extends DnaConfiguration> RepositorySourceDefinition<ReturnType> repositorySourceDefinition( ReturnType returnObject,
                                                                                                                       String name ) {
        RepositorySourceDefinition<ReturnType> definition = (RepositorySourceDefinition<ReturnType>)repositorySourceDefinitions.get(name);
        if (definition == null) {
            definition = new SourceBuilder<ReturnType>(returnObject, changes(), path(), DnaLexicon.SOURCES, name(name));
            repositorySourceDefinitions.put(name, definition);
        }
        return definition;
    }

    /**
     * Utility method to construct a definition object for the sequencer with the supplied name and return type.
     * 
     * @param <ReturnType> the type of the return object
     * @param returnObject the return object
     * @param name the name of the sequencer
     * @return the definition for the sequencer
     */
    @SuppressWarnings( "unchecked" )
    protected <ReturnType extends DnaConfiguration> SequencerDefinition<ReturnType> sequencerDefinition( ReturnType returnObject,
                                                                                                         String name ) {
        SequencerDefinition<ReturnType> definition = (SequencerDefinition<ReturnType>)sequencerDefinitions.get(name);
        if (definition == null) {
            definition = new SequencerBuilder<ReturnType>(returnObject, changes(), path(), DnaLexicon.SEQUENCERS, name(name));
            sequencerDefinitions.put(name, definition);
        }
        return definition;
    }

    protected static class BaseReturnable<ReturnType> implements Returnable<ReturnType> {
        protected final ReturnType returnObject;

        protected BaseReturnable( ReturnType returnObject ) {
            this.returnObject = returnObject;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.Returnable#and()
         */
        public ReturnType and() {
            return returnObject;
        }
    }

    /**
     * Base class for {@link Returnable} types that work on a node in the graph.
     * 
     * @param <ReturnType> the type to be returned
     * @param <ThisType> the type to be returned by the set properties, set description, etc. methods
     */
    protected static abstract class GraphReturnable<ReturnType, ThisType> extends BaseReturnable<ReturnType>
        implements SetDescription<ThisType>, SetProperties<ThisType>, Removable<ReturnType> {
        protected final ExecutionContext context;
        protected final Graph.Batch batch;
        protected final Path path;
        private Map<Name, Property> properties = new HashMap<Name, Property>();

        protected GraphReturnable( ReturnType returnObject,
                                   Graph.Batch batch,
                                   Path path,
                                   Name... names ) {
            super(returnObject);
            assert batch != null;
            assert path != null;
            assert names.length > 0;
            this.context = batch.getGraph().getContext();
            this.batch = batch;
            // Make sure there are nodes down to the supplied path ...
            createIfMissing(path, names).and();
            this.path = context.getValueFactories().getPathFactory().create(path, names);
            try {
                properties = batch.getGraph().getPropertiesByName().on(this.path);
            } catch (PathNotFoundException e) {
                // The node doesn't exist yet (wasn't yet saved)
                properties = new HashMap<Name, Property>();
            }
        }

        /**
         * Create the node at the supplied path under the current path, and return the Create operation for the last node created.
         * The caller <i>must</i> call {@link Graph.Create#and()} to complete the operation.
         * 
         * @param child the name of the child
         * @param segments the segments in the remainder of the path
         * @return the newly-created but incomplete operation
         */
        protected Graph.Create<Graph.Batch> createIfMissing( Name child,
                                                             String... segments ) {
            Path nodePath = context.getValueFactories().getPathFactory().create(path, child);
            Graph.Create<Graph.Batch> result = batch.create(nodePath).orUpdate();
            for (String name : segments) {
                result.and();
                nodePath = context.getValueFactories().getPathFactory().create(nodePath, name);
                result = batch.create(nodePath).orUpdate();
            }
            return result;
        }

        /**
         * Create the node at the supplied path under the current path, and return the Create operation for the last node created.
         * The caller <i>must</i> call {@link Graph.Create#and()} to complete the operation.
         * 
         * @param segment the name segment for the child
         * @return the newly-created but incomplete operation
         */
        protected Graph.Create<Graph.Batch> createIfMissing( Name segment ) {
            Path nodePath = context.getValueFactories().getPathFactory().create(path, segment);
            Graph.Create<Graph.Batch> result = batch.create(nodePath).orUpdate();
            return result;
        }

        /**
         * Create the node at the supplied path under the current path, and return the Create operation for the last node created.
         * The caller <i>must</i> call {@link Graph.Create#and()} to complete the operation.
         * 
         * @param path the path to the node
         * @param segments the segments in the remainder of the path
         * @return the newly-created but incomplete operation
         */
        protected Graph.Create<Graph.Batch> createIfMissing( Path path,
                                                             Name... segments ) {
            Path nodePath = path;
            Graph.Create<Graph.Batch> result = null;
            for (Name name : segments) {
                if (result != null) result.and();
                nodePath = context.getValueFactories().getPathFactory().create(nodePath, name);
                result = batch.create(nodePath).orUpdate();
            }
            return result;
        }

        protected Path subpath( Name... segments ) {
            return context.getValueFactories().getPathFactory().create(path, segments);
        }

        protected abstract ThisType thisType();

        public String getName() {
            return path.getLastSegment().getName().getString(context.getNamespaceRegistry());
        }

        public ThisType setDescription( String description ) {
            return setProperty(DnaLexicon.DESCRIPTION, description);
        }

        public String getDescription() {
            Property property = getProperty(DnaLexicon.DESCRIPTION);
            if (property != null && !property.isEmpty()) {
                return context.getValueFactories().getStringFactory().create(property.getFirstValue());
            }
            return null;
        }

        protected ThisType setProperty( Name propertyName,
                                        Object value ) {
            // Set the property via the batch ...
            batch.set(propertyName).on(path).to(value).and();
            // Record that we changed this property ...
            properties.put(propertyName, context.getPropertyFactory().create(propertyName, value));
            return thisType();
        }

        public ThisType setProperty( String propertyName,
                                     Object value ) {
            return setProperty(context.getValueFactories().getNameFactory().create(propertyName), value);
        }

        public ThisType setProperty( Name propertyName,
                                     Object[] values ) {
            // Set the property via the batch ...
            batch.set(propertyName).on(path).to(values).and();
            // Record that we changed this property ...
            properties.put(propertyName, context.getPropertyFactory().create(propertyName, values));
            return thisType();
        }

        public ThisType setProperty( String propertyName,
                                     Object[] values ) {
            return setProperty(context.getValueFactories().getNameFactory().create(propertyName), values);
        }

        public ThisType setProperty( String beanPropertyName,
                                     boolean value ) {
            return setProperty(beanPropertyName, (Object)value);
        }

        public ThisType setProperty( String beanPropertyName,
                                     int value ) {
            return setProperty(beanPropertyName, (Object)value);
        }

        public ThisType setProperty( String beanPropertyName,
                                     short value ) {
            return setProperty(beanPropertyName, (Object)value);
        }

        public ThisType setProperty( String beanPropertyName,
                                     long value ) {
            return setProperty(beanPropertyName, (Object)value);
        }

        public ThisType setProperty( String beanPropertyName,
                                     double value ) {
            return setProperty(beanPropertyName, (Object)value);
        }

        public ThisType setProperty( String beanPropertyName,
                                     float value ) {
            return setProperty(beanPropertyName, (Object)value);
        }

        public ThisType setProperty( String beanPropertyName,
                                     String value ) {
            return setProperty(beanPropertyName, (Object)value);
        }

        public ThisType setProperty( String beanPropertyName,
                                     String firstValue,
                                     String... additionalValues ) {
            Object[] values = new Object[1 + additionalValues.length];
            values[0] = firstValue;
            System.arraycopy(additionalValues, 0, values, 1, additionalValues.length);
            return setProperty(beanPropertyName, values);
        }

        public Property getProperty( String beanPropertyName ) {
            return properties.get(context.getValueFactories().getNameFactory().create(beanPropertyName));
        }

        public Property getProperty( Name beanPropertyName ) {
            return properties.get(beanPropertyName);
        }

        public ReturnType remove() {
            batch.delete(path);
            properties.clear();
            return and();
        }
    }

    /**
     * Base class for {@link Returnable} types that work on a node in the graph.
     * 
     * @param <ReturnType> the type to be returned
     * @param <ThisType> the type to be returned by the set properties, set description, etc. methods
     * @param <ComponentType> the type of the component being configured
     */
    protected static abstract class GraphComponentBuilder<ReturnType, ThisType, ComponentType>
        extends GraphReturnable<ReturnType, ThisType> implements ChooseClass<ComponentType, ThisType> {
        protected GraphComponentBuilder( ReturnType returnObject,
                                         Graph.Batch batch,
                                         Path path,
                                         Name... names ) {
            super(returnObject, batch, path, names);
        }

        public LoadedFrom<ThisType> usingClass( final String classname ) {
            return new LoadedFrom<ThisType>() {
                public ThisType loadedFromClasspath() {
                    return setProperty(DnaLexicon.CLASSNAME, classname);
                }

                public ThisType loadedFrom( String... classpath ) {
                    List<String> classpaths = new ArrayList<String>();
                    // Ignore any null, zero-length, or duplicate elements ...
                    for (String value : classpath) {
                        if (value == null) continue;
                        value = value.trim();
                        if (value.length() == 0) continue;
                        if (!classpaths.contains(value)) classpaths.add(value);
                    }
                    if (classpaths.size() != 0) {
                        classpath = classpaths.toArray(new String[classpaths.size()]);
                        setProperty(DnaLexicon.CLASSPATH, classpath);
                    }
                    return setProperty(DnaLexicon.CLASSNAME, classname);
                }
            };
        }

        public ThisType usingClass( Class<? extends ComponentType> componentClass ) {
            return setProperty(DnaLexicon.CLASSNAME, componentClass.getCanonicalName());
        }
    }

    protected static class MimeTypeDetectorBuilder<ReturnType>
        extends GraphComponentBuilder<ReturnType, MimeTypeDetectorDefinition<ReturnType>, MimeTypeDetector>
        implements MimeTypeDetectorDefinition<ReturnType> {
        protected MimeTypeDetectorBuilder( ReturnType returnObject,
                                           Graph.Batch batch,
                                           Path path,
                                           Name... names ) {
            super(returnObject, batch, path, names);
        }

        @Override
        protected MimeTypeDetectorBuilder<ReturnType> thisType() {
            return this;
        }

    }

    protected static class SourceBuilder<ReturnType>
        extends GraphComponentBuilder<ReturnType, RepositorySourceDefinition<ReturnType>, RepositorySource>
        implements RepositorySourceDefinition<ReturnType> {
        protected SourceBuilder( ReturnType returnObject,
                                 Graph.Batch batch,
                                 Path path,
                                 Name... names ) {
            super(returnObject, batch, path, names);
        }

        @Override
        protected RepositorySourceDefinition<ReturnType> thisType() {
            return this;
        }

        public RepositorySourceDefinition<ReturnType> setRetryLimit( int retryLimit ) {
            return setProperty(DnaLexicon.RETRY_LIMIT, retryLimit);
        }

        @Override
        public RepositorySourceDefinition<ReturnType> setProperty( String propertyName,
                                                                   Object value ) {
            Name name = context.getValueFactories().getNameFactory().create(propertyName);
            // Check the "standard" names that should be prefixed with 'dna:'
            if (name.getLocalName().equals(DnaLexicon.RETRY_LIMIT.getLocalName())) name = DnaLexicon.RETRY_LIMIT;
            if (name.getLocalName().equals(DnaLexicon.DESCRIPTION.getLocalName())) name = DnaLexicon.DESCRIPTION;
            return super.setProperty(name, value);
        }

        @Override
        public Property getProperty( Name name ) {
            // Check the "standard" names that should be prefixed with 'dna:'
            if (name.getLocalName().equals(DnaLexicon.RETRY_LIMIT.getLocalName())) name = DnaLexicon.RETRY_LIMIT;
            if (name.getLocalName().equals(DnaLexicon.DESCRIPTION.getLocalName())) name = DnaLexicon.DESCRIPTION;
            return super.getProperty(name);
        }
    }

    protected static class SequencerBuilder<ReturnType>
        extends GraphComponentBuilder<ReturnType, SequencerDefinition<ReturnType>, StreamSequencer>
        implements SequencerDefinition<ReturnType> {

        protected SequencerBuilder( ReturnType returnObject,
                                    Graph.Batch batch,
                                    Path path,
                                    Name... names ) {
            super(returnObject, batch, path, names);
        }

        @Override
        protected SequencerDefinition<ReturnType> thisType() {
            return this;
        }

        public Set<PathExpression> getPathExpressions() {
            Set<PathExpression> expressions = new HashSet<PathExpression>();
            try {
                Property existingExpressions = getProperty(DnaLexicon.PATH_EXPRESSION);
                if (existingExpressions != null) {
                    for (Object existing : existingExpressions.getValuesAsArray()) {
                        String existingExpression = context.getValueFactories().getStringFactory().create(existing);
                        expressions.add(PathExpression.compile(existingExpression));
                    }
                }
            } catch (PathNotFoundException e) {
                // Nothing saved yet ...
            }
            return expressions;
        }

        public SequencerDefinition<ReturnType> sequencingFrom( PathExpression expression ) {
            CheckArg.isNotNull(expression, "expression");
            Set<PathExpression> compiledExpressions = getPathExpressions();
            compiledExpressions.add(expression);
            String[] strings = new String[compiledExpressions.size()];
            int index = 0;
            for (PathExpression compiledExpression : compiledExpressions) {
                strings[index++] = compiledExpression.getExpression();
            }
            setProperty(DnaLexicon.PATH_EXPRESSION, strings);
            return this;
        }

        public PathExpressionOutput<ReturnType> sequencingFrom( final String fromPathExpression ) {
            CheckArg.isNotEmpty(fromPathExpression, "fromPathExpression");
            return new PathExpressionOutput<ReturnType>() {
                public SequencerDefinition<ReturnType> andOutputtingTo( String into ) {
                    CheckArg.isNotEmpty(into, "into");
                    return sequencingFrom(PathExpression.compile(fromPathExpression + " => " + into));
                }
            };
        }
    }

    /**
     * Representation of the current configuration content.
     */
    @Immutable
    public static class ConfigurationDefinition {
        private final ClassLoaderFactory classLoaderFactory;
        private final RepositorySource source;
        private final Path path;
        private final String workspace;
        private final ExecutionContext context;
        private Graph graph;

        protected ConfigurationDefinition( RepositorySource source,
                                           String workspace,
                                           Path path,
                                           ExecutionContext context,
                                           ClassLoaderFactory classLoaderFactory ) {
            this.source = source;
            this.path = path != null ? path : RootPath.INSTANCE;
            this.workspace = workspace;
            this.context = context;
            this.classLoaderFactory = classLoaderFactory != null ? classLoaderFactory : new StandardClassLoaderFactory();
        }

        /**
         * Get the repository source where the configuration content may be found
         * 
         * @return the source for the configuration repository; never null
         */
        public RepositorySource getRepositorySource() {
            return source;
        }

        /**
         * Get the path in the configuration repository where the configuration content may be found
         * 
         * @return the path to the configuration content; never null
         */
        public Path getPath() {
            return path;
        }

        /**
         * Get the name of the workspace used for the configuration repository.
         * 
         * @return the name of the workspace, or null if the default workspace should be used
         */
        public String getWorkspace() {
            return workspace;
        }

        /**
         * @return context
         */
        public ExecutionContext getContext() {
            return context;
        }

        /**
         * @return classLoaderFactory
         */
        public ClassLoaderFactory getClassLoaderFactory() {
            return classLoaderFactory;
        }

        /**
         * Return a copy of this configuration that uses the supplied path instead of this object's {@link #getPath() path}.
         * 
         * @param path the desired path for the new configuration; if null, then "/" is used
         * @return the new configuration
         */
        public ConfigurationDefinition with( Path path ) {
            return new ConfigurationDefinition(source, workspace, path, context, classLoaderFactory);
        }

        /**
         * Return a copy of this configuration that uses the supplied workspace name instead of this object's
         * {@link #getWorkspace() workspace}.
         * 
         * @param workspace the desired workspace name for the new configuration; if null, then the default workspace will be used
         * @return the new configuration
         */
        public ConfigurationDefinition withWorkspace( String workspace ) {
            return new ConfigurationDefinition(source, workspace, path, context, classLoaderFactory);
        }

        /**
         * Return a copy of this configuration that uses the supplied class loader factory instead of this object's
         * {@link #getClassLoaderFactory() class loader factory}.
         * 
         * @param classLoaderFactory the classloader factory, or null if the default factory should be used
         * @return the new configuration
         */
        public ConfigurationDefinition with( ClassLoaderFactory classLoaderFactory ) {
            return new ConfigurationDefinition(source, workspace, path, context, classLoaderFactory);
        }

        /**
         * Obtain a graph to this configuration repository. This method will always return the same graph instance.
         * 
         * @return the graph; never null
         */
        public Graph graph() {
            if (graph == null) {
                graph = Graph.create(source, context);
                if (workspace != null) graph.useWorkspace(workspace);
            }
            return graph;
        }
    }
}
