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
package org.modeshape.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.component.ClassLoaderFactory;
import org.modeshape.common.component.StandardClassLoaderFactory;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.xml.StreamingContentHandler;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Location;
import org.modeshape.graph.Node;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.SubgraphNode;
import org.modeshape.graph.Workspace;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.mimetype.MimeTypeDetector;
import org.modeshape.graph.observe.ObservationBus;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathExpression;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.basic.RootPath;
import org.modeshape.graph.request.InvalidWorkspaceException;
import org.modeshape.graph.request.ReadBranchRequest;
import org.modeshape.graph.sequencer.StreamSequencer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * A configuration builder for a {@link ModeShapeEngine}. This class is an internal domain-specific language (DSL), and is
 * designed to be used in a traditional way or in a method-chained manner:
 * 
 * <pre>
 * configuration.repositorySource(&quot;Source1&quot;).setClass(InMemoryRepositorySource.class).setDescription(&quot;description&quot;);
 * configuration.mimeTypeDetector(&quot;detector&quot;).setClass(ExtensionBasedMimeTypeDetector.class).setDescription(&quot;default detector&quot;);
 * configuration.sequencer(&quot;MicrosoftDocs&quot;)
 *              .setClass(&quot;org.modeshape.sequencer.msoffice.MSOfficeMetadataSequencer&quot;)
 *              .setDescription(&quot;Our primary sequencer for all .doc files&quot;)
 *              .sequencingFrom(&quot;/public//(*.(doc|xml|ppt)[*]/jcr:content[@jcr:data]&quot;)
 *              .andOutputtingTo(&quot;/documents/$1&quot;);
 * configuration.save();
 * </pre>
 */
@NotThreadSafe
public class ModeShapeConfiguration {

    public static final String DEFAULT_WORKSPACE_NAME = "";
    public static final String DEFAULT_PATH = "/";
    public static final String DEFAULT_CONFIGURATION_SOURCE_NAME = "ModeShape Configuration Repository";

    private final ExecutionContext context;
    private final Problems problems = new SimpleProblems();
    private ConfigurationDefinition configurationContent;
    private Graph.Batch changes;

    private final Map<String, SequencerDefinition<? extends ModeShapeConfiguration>> sequencerDefinitions = new HashMap<String, SequencerDefinition<? extends ModeShapeConfiguration>>();
    private final Map<String, RepositorySourceDefinition<? extends ModeShapeConfiguration>> repositorySourceDefinitions = new HashMap<String, RepositorySourceDefinition<? extends ModeShapeConfiguration>>();
    private final Map<String, MimeTypeDetectorDefinition<? extends ModeShapeConfiguration>> mimeTypeDetectorDefinitions = new HashMap<String, MimeTypeDetectorDefinition<? extends ModeShapeConfiguration>>();
    private ClusterDefinition<? extends ModeShapeConfiguration> clusterDefinition;

    /**
     * Create a new configuration, using a default-constructed {@link ExecutionContext}.
     */
    public ModeShapeConfiguration() {
        this(new ExecutionContext());
    }

    /**
     * Create a new configuration using the supplied {@link ExecutionContext}.
     * 
     * @param context the execution context
     * @throws IllegalArgumentException if the path is null or empty
     */
    public ModeShapeConfiguration( ExecutionContext context ) {
        CheckArg.isNotNull(context, "context");
        this.context = context;

        // Create the in-memory repository source in which the content will be stored ...
        InMemoryRepositorySource source = new InMemoryRepositorySource();
        source.setName(DEFAULT_CONFIGURATION_SOURCE_NAME);
        source.setDefaultWorkspaceName(DEFAULT_WORKSPACE_NAME);

        // The file was imported successfully, so now create the content information ...
        configurationContent = new ConfigurationDefinition("dna", source, null, null, context, null);
    }

    /**
     * Set the name of this configuration.
     * 
     * @param name the configuration name; may be null if the default name should be used
     * @return this configuration
     */
    public ModeShapeConfiguration withName( String name ) {
        if (name != null) name = "dna";
        configurationContent = configurationContent.with(name);
        return this;
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
    public ModeShapeConfiguration loadFrom( String pathToConfigurationFile ) throws IOException, SAXException {
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
    public ModeShapeConfiguration loadFrom( String pathToConfigurationFile,
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
    public ModeShapeConfiguration loadFrom( File configurationFile ) throws IOException, SAXException {
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
    public ModeShapeConfiguration loadFrom( File configurationFile,
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
    public ModeShapeConfiguration loadFrom( URL urlToConfigurationFile ) throws IOException, SAXException {
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
    public ModeShapeConfiguration loadFrom( URL urlToConfigurationFile,
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
    public ModeShapeConfiguration loadFrom( InputStream configurationFileInputStream ) throws IOException, SAXException {
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
    public ModeShapeConfiguration loadFrom( InputStream configurationFileInputStream,
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
        configurationContent = configurationContent.with(pathToParent)
                                                   .with(source)
                                                   .withWorkspace(source.getDefaultWorkspaceName());
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
    public ModeShapeConfiguration loadFrom( RepositorySource source ) {
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
    public ModeShapeConfiguration loadFrom( RepositorySource source,
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
    public ModeShapeConfiguration loadFrom( RepositorySource source,
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
        } else {
            workspaceName = graph.getCurrentWorkspaceName(); // will be the default
        }

        // Verify the path ...
        Path path = pathInWorkspace != null ? path(pathInWorkspace) : path(DEFAULT_PATH);
        Node parent = graph.getNodeAt(path);
        assert parent != null;

        // Now create the content information ...
        configurationContent = configurationContent.with(source).withWorkspace(workspaceName).with(path);
        return this;
    }

    /**
     * Store the saved configuration to the file with the given name. Changes made without calling {@link #save()} will not be
     * written to the file.
     * 
     * @param file the name of the file to which the configuration should be stored
     * @throws SAXException if there is an error saving the configuration
     * @throws IOException if the file cannot be created or if there is an error writing the configuration to the file.
     */
    public void storeTo( String file ) throws SAXException, IOException {
        storeTo(new File(file));
    }

    /**
     * Store the saved configuration to the given file. Changes made without calling {@link #save()} will not be written to the
     * file.
     * 
     * @param file the name of the file to which the configuration should be stored
     * @throws SAXException if there is an error saving the configuration
     * @throws IOException if the file cannot be created or if there is an error writing the configuration to the file.
     */
    public void storeTo( File file ) throws SAXException, IOException {
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            storeTo(new StreamingContentHandler(os));
        } finally {
            if (os != null) os.close();
        }
    }

    /**
     * Store the saved configuration to the stream. Changes made without calling {@link #save()} will not be written to the
     * stream.
     * 
     * @param os the name of the file to which the configuration should be stored
     * @throws SAXException if there is an error saving the configuration
     */
    public void storeTo( OutputStream os ) throws SAXException {
        storeTo(new StreamingContentHandler(os));
    }

    /**
     * Traverse the saved configuration graph treating it as an XML document and calling the corresponding SAX event on the
     * provided {@link ContentHandler}. Changes made without calling {@link #save()} will not be written to the stream.
     * 
     * @param handler the content handler that will receive the SAX events
     * @throws SAXException if there is an error saving the configuration
     */
    public void storeTo( ContentHandler handler ) throws SAXException {
        Subgraph allContent = configurationGraph().getSubgraphOfDepth(ReadBranchRequest.NO_MAXIMUM_DEPTH).at("/");

        Set<NamespaceRegistry.Namespace> namespaces = this.context.getNamespaceRegistry().getNamespaces();
        Stack<String> mappedNamespacePrefixes = new Stack<String>();

        handler.startDocument();

        for (NamespaceRegistry.Namespace namespace : namespaces) {
            handler.startPrefixMapping(namespace.getPrefix(), namespace.getNamespaceUri());
            mappedNamespacePrefixes.push(namespace.getPrefix());
        }

        exportNode(handler, allContent, allContent.getRoot());
        while (!mappedNamespacePrefixes.isEmpty()) {
            handler.endPrefixMapping(mappedNamespacePrefixes.pop());
        }

        handler.endDocument();
    }

    private void exportNode( ContentHandler handler,
                             Subgraph subgraph,
                             SubgraphNode node ) throws SAXException {
        // Build the attributes

        NamespaceRegistry registry = this.context.getNamespaceRegistry();
        ValueFactory<String> stringFactory = this.context.getValueFactories().getStringFactory();

        AttributesImpl atts = new AttributesImpl();

        for (Property prop : node.getProperties()) {
            Name name = prop.getName();

            StringBuilder buff = new StringBuilder();
            boolean first = true;

            for (Object rawValue : prop) {
                if (first) {
                    first = false;
                } else {
                    buff.append(",");
                }
                buff.append(stringFactory.create(rawValue));
            }

            atts.addAttribute(name.getNamespaceUri(), name.getLocalName(), name.getString(registry), "string", buff.toString());
        }

        // Start the node
        Name nodeName;
        Path nodePath = node.getLocation().getPath();
        if (nodePath.isRoot()) {
            nodeName = name("configuration");
        } else {
            nodeName = node.getLocation().getPath().getLastSegment().getName();
        }
        String uri = nodeName.getNamespaceUri();
        String localName = nodeName.getLocalName();
        String qName = nodeName.getString(registry);
        handler.startElement(uri, localName, qName, atts);

        // Handle the children
        for (Location childLocation : node.getChildren()) {
            exportNode(handler, subgraph, subgraph.getNode(childLocation));
        }

        // Finish the node
        handler.endElement(uri, localName, qName);

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

    protected Graph configurationGraph() {
        ConfigurationDefinition content = getConfigurationDefinition();
        Graph graph = Graph.create(content.getRepositorySource(), content.getContext());
        if (content.getWorkspace() != null) {
            graph.useWorkspace(content.getWorkspace());
        }

        return graph;
    }

    protected Graph.Batch changes() {
        if (changes == null) {
            changes = configurationGraph().batch();
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
    public ModeShapeConfiguration save() {
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
    public ModeShapeConfiguration withClassLoaderFactory( ClassLoaderFactory classLoaderFactory ) {
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
    public Set<MimeTypeDetectorDefinition<? extends ModeShapeConfiguration>> mimeTypeDetectors() {
        // Get the children under the 'dna:mimeTypeDetectors' node ...
        Set<String> names = getNamesOfComponentsUnder(ModeShapeLexicon.MIME_TYPE_DETECTORS);
        names.addAll(this.mimeTypeDetectorDefinitions.keySet());
        Set<MimeTypeDetectorDefinition<? extends ModeShapeConfiguration>> results = new HashSet<MimeTypeDetectorDefinition<? extends ModeShapeConfiguration>>();
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
    public Set<RepositorySourceDefinition<? extends ModeShapeConfiguration>> repositorySources() {
        // Get the children under the 'dna:mimeTypeDetectors' node ...
        Set<String> names = getNamesOfComponentsUnder(ModeShapeLexicon.SOURCES);
        names.addAll(this.repositorySourceDefinitions.keySet());
        Set<RepositorySourceDefinition<? extends ModeShapeConfiguration>> results = new HashSet<RepositorySourceDefinition<? extends ModeShapeConfiguration>>();
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
    public Set<SequencerDefinition<? extends ModeShapeConfiguration>> sequencers() {
        // Get the children under the 'dna:mimeTypeDetectors' node ...
        Set<String> names = getNamesOfComponentsUnder(ModeShapeLexicon.SEQUENCERS);
        names.addAll(this.sequencerDefinitions.keySet());
        Set<SequencerDefinition<? extends ModeShapeConfiguration>> results = new HashSet<SequencerDefinition<? extends ModeShapeConfiguration>>();
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
    public MimeTypeDetectorDefinition<? extends ModeShapeConfiguration> mimeTypeDetector( String name ) {
        return mimeTypeDetectorDefinition(this, name);
    }

    /**
     * Obtain or create a definition for the {@link RepositorySource} with the supplied name or identifier. A new definition will
     * be created if there currently is no repository source defined with the supplied name.
     * 
     * @param name the name or identifier of the repository source
     * @return the details of the repository source definition; never null
     */
    public RepositorySourceDefinition<? extends ModeShapeConfiguration> repositorySource( String name ) {
        return repositorySourceDefinition(this, name);
    }

    /**
     * Obtain or create a definition for the {@link StreamSequencer sequencer} with the supplied name or identifier. A new
     * definition will be created if there currently is no sequencer defined with the supplied name.
     * 
     * @param name the name or identifier of the sequencer
     * @return the details of the sequencer definition; never null
     */
    public SequencerDefinition<? extends ModeShapeConfiguration> sequencer( String name ) {
        return sequencerDefinition(this, name);
    }

    /**
     * Obtain the definition for this engine's clustering. If no clustering definition exists, one will be created.
     * 
     * @return the clustering definition; never null
     */
    public ClusterDefinition<? extends ModeShapeConfiguration> clustering() {
        return clusterDefinition(this);
    }

    /**
     * Obtain the definition for this engine's global properties.
     * 
     * @return the global properties definition; never null
     */
    public GlobalProperties<? extends ModeShapeConfiguration> globalProperties() {
        return globalProperties(this);
    }

    /**
     * Set the interval for garbage collection, should garbage collection be required by the sources).
     * 
     * @param interval the interval magnitude
     * @param unit the interval time unit
     * @return this configuration object for method chaining purposes
     */
    public ModeShapeConfiguration withGarbageCollectionInterval( long interval,
                                                                 TimeUnit unit ) {
        long intervalInSeconds = unit.convert(interval, TimeUnit.SECONDS);
        globalProperties().setProperty(ModeShapeLexicon.GARBAGE_COLLECTION_INTERVAL, intervalInSeconds);
        return this;
    }

    /**
     * Convenience method to make the code that sets up this configuration easier to read. This method simply returns this object.
     * 
     * @return this configuration component; never null
     */
    public ModeShapeConfiguration and() {
        return this;
    }

    /**
     * Construct an engine that reflects the current state of this configuration. This method always creates a new instance.
     * 
     * @return the resulting engine; never null
     * @see #getExecutionContextForEngine()
     */
    public ModeShapeEngine build() {
        save();
        return new ModeShapeEngine(getExecutionContextForEngine(), getConfigurationDefinition());
    }

    /**
     * Utility method used by {@link #build()} to get the {@link ExecutionContext} instance for the engine. This method gives
     * subclasses the ability to override this behavior.
     * <p>
     * Currently, this method wraps the {@link #getExecutionContext() configuration's execution context} to provide
     * backward-compability with JBoss DNA namespaces. See MODE-647 for details.
     * </p>
     * 
     * @return the execution context to be used for the engine
     * @see #build()
     */
    protected ExecutionContext getExecutionContextForEngine() {
        return getExecutionContext();
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
         * @throws ModeShapeConfigurationException if the class could not be accessed and instantiated (if needed)
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
     * Interface used to set up and define the cluster configuration.
     * 
     * @param <ReturnType> the type of the configuration component that owns this definition object
     */
    public interface ClusterDefinition<ReturnType>
        extends Returnable<ReturnType>, SetProperties<ClusterDefinition<ReturnType>>, Removable<ReturnType> {
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
         * {@link ModeShapeLexicon#RETRY_LIMIT dna:retryLimit}" as the property name.
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
     * {@link ModeShapeConfiguration.SequencerDefinition#sequencingFrom(PathExpression) sequencer configuration}.
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
    protected <ReturnType extends ModeShapeConfiguration> MimeTypeDetectorDefinition<ReturnType> mimeTypeDetectorDefinition( ReturnType returnObject,
                                                                                                                             String name ) {
        MimeTypeDetectorDefinition<ReturnType> definition = (MimeTypeDetectorDefinition<ReturnType>)mimeTypeDetectorDefinitions.get(name);
        if (definition == null) {
            definition = new MimeTypeDetectorBuilder<ReturnType>(returnObject, changes(), path(),
                                                                 ModeShapeLexicon.MIME_TYPE_DETECTORS, name(name));
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
    protected <ReturnType extends ModeShapeConfiguration> RepositorySourceDefinition<ReturnType> repositorySourceDefinition( ReturnType returnObject,
                                                                                                                             String name ) {
        RepositorySourceDefinition<ReturnType> definition = (RepositorySourceDefinition<ReturnType>)repositorySourceDefinitions.get(name);
        if (definition == null) {
            definition = new SourceBuilder<ReturnType>(returnObject, changes(), path(), ModeShapeLexicon.SOURCES, name(name));
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
    protected <ReturnType extends ModeShapeConfiguration> SequencerDefinition<ReturnType> sequencerDefinition( ReturnType returnObject,
                                                                                                               String name ) {
        SequencerDefinition<ReturnType> definition = (SequencerDefinition<ReturnType>)sequencerDefinitions.get(name);
        if (definition == null) {
            definition = new SequencerBuilder<ReturnType>(returnObject, changes(), path(), ModeShapeLexicon.SEQUENCERS,
                                                          name(name));
            sequencerDefinitions.put(name, definition);
        }
        return definition;
    }

    /**
     * Utility method to construct a definition object for the clustering with the supplied name and return type.
     * 
     * @param <ReturnType> the type of the return object
     * @param returnObject the return object
     * @return the definition for the clustering; never null
     */
    @SuppressWarnings( "unchecked" )
    protected <ReturnType extends ModeShapeConfiguration> ClusterDefinition<ReturnType> clusterDefinition( ReturnType returnObject ) {
        if (clusterDefinition == null) {
            clusterDefinition = new ClusterBuilder<ReturnType>(returnObject, changes(), path(), ModeShapeLexicon.CLUSTERING);
        }
        return (ClusterDefinition<ReturnType>)clusterDefinition;
    }

    /**
     * Utility method to construct a setter for global properties.
     * 
     * @param <ReturnType> the type of the return object
     * @param returnObject the return object
     * @return the global properties setter; never null
     */
    protected <ReturnType extends ModeShapeConfiguration> GlobalProperties<ReturnType> globalProperties( ReturnType returnObject ) {
        return new GlobalProperties<ReturnType>(returnObject, changes(), path());
    }

    protected static class BaseReturnable<ReturnType> implements Returnable<ReturnType> {
        protected final ReturnType returnObject;

        protected BaseReturnable( ReturnType returnObject ) {
            this.returnObject = returnObject;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.repository.ModeShapeConfiguration.Returnable#and()
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
            return setProperty(ModeShapeLexicon.DESCRIPTION, description);
        }

        public String getDescription() {
            Property property = getProperty(ModeShapeLexicon.DESCRIPTION);
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
                    return setProperty(ModeShapeLexicon.CLASSNAME, classname);
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
                        setProperty(ModeShapeLexicon.CLASSPATH, classpath);
                    }
                    return setProperty(ModeShapeLexicon.CLASSNAME, classname);
                }
            };
        }

        public ThisType usingClass( Class<? extends ComponentType> componentClass ) {
            return setProperty(ModeShapeLexicon.CLASSNAME, componentClass.getCanonicalName());
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
            return setProperty(ModeShapeLexicon.RETRY_LIMIT, retryLimit);
        }

        @Override
        public RepositorySourceDefinition<ReturnType> setProperty( String propertyName,
                                                                   Object value ) {
            Name name = context.getValueFactories().getNameFactory().create(propertyName);
            // Check the "standard" names that should be prefixed with 'dna:'
            if (name.getLocalName().equals(ModeShapeLexicon.RETRY_LIMIT.getLocalName())) name = ModeShapeLexicon.RETRY_LIMIT;
            if (name.getLocalName().equals(ModeShapeLexicon.DESCRIPTION.getLocalName())) name = ModeShapeLexicon.DESCRIPTION;
            return super.setProperty(name, value);
        }

        @Override
        public Property getProperty( Name name ) {
            // Check the "standard" names that should be prefixed with 'dna:'
            if (name.getLocalName().equals(ModeShapeLexicon.RETRY_LIMIT.getLocalName())) name = ModeShapeLexicon.RETRY_LIMIT;
            if (name.getLocalName().equals(ModeShapeLexicon.DESCRIPTION.getLocalName())) name = ModeShapeLexicon.DESCRIPTION;
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
                Property existingExpressions = getProperty(ModeShapeLexicon.PATH_EXPRESSION);
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
            setProperty(ModeShapeLexicon.PATH_EXPRESSION, strings);
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

    protected static class ClusterBuilder<ReturnType>
        extends GraphComponentBuilder<ReturnType, ClusterDefinition<ReturnType>, ObservationBus>
        implements ClusterDefinition<ReturnType> {

        protected ClusterBuilder( ReturnType returnObject,
                                  Graph.Batch batch,
                                  Path path,
                                  Name... names ) {
            super(returnObject, batch, path, names);
        }

        @Override
        protected ClusterDefinition<ReturnType> thisType() {
            return this;
        }
    }

    protected static class GlobalProperties<ReturnType> extends GraphReturnable<ReturnType, GlobalProperties<ReturnType>> {
        protected GlobalProperties( ReturnType returnObject,
                                    Graph.Batch batch,
                                    Path path,
                                    Name... names ) {
            super(returnObject, batch, path, names);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.repository.ModeShapeConfiguration.GraphReturnable#thisType()
         */
        @Override
        protected GlobalProperties<ReturnType> thisType() {
            return this;
        }
    }

    /**
     * Representation of the current configuration content.
     */
    @Immutable
    public static class ConfigurationDefinition {
        private final String name;
        private final ClassLoaderFactory classLoaderFactory;
        private final RepositorySource source;
        private final Path path;
        private final String workspace;
        private final ExecutionContext context;
        private Graph graph;

        protected ConfigurationDefinition( String configurationName,
                                           RepositorySource source,
                                           String workspace,
                                           Path path,
                                           ExecutionContext context,
                                           ClassLoaderFactory classLoaderFactory ) {
            assert configurationName != null;
            assert source != null;
            this.name = configurationName;
            this.source = source;
            this.path = path != null ? path : RootPath.INSTANCE;
            this.workspace = workspace;
            this.context = context;
            this.classLoaderFactory = classLoaderFactory != null ? classLoaderFactory : new StandardClassLoaderFactory();
        }

        /**
         * Get the name of this configuration.
         * 
         * @return the configuration's name; never null
         */
        public String getName() {
            return name;
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
         * Return a copy of this configuration that uses the supplied name instead of this object's {@link #getPath() path}.
         * 
         * @param name the desired name for the new configuration; if null, then the name of this configuration is used
         * @return the new configuration
         */
        public ConfigurationDefinition with( String name ) {
            if (name == null) name = this.name;
            return new ConfigurationDefinition(name, source, workspace, path, context, classLoaderFactory);
        }

        /**
         * Return a copy of this configuration that uses the supplied path instead of this object's {@link #getPath() path}.
         * 
         * @param path the desired path for the new configuration; if null, then "/" is used
         * @return the new configuration
         */
        public ConfigurationDefinition with( Path path ) {
            return new ConfigurationDefinition(name, source, workspace, path, context, classLoaderFactory);
        }

        /**
         * Return a copy of this configuration that uses the supplied workspace name instead of this object's
         * {@link #getWorkspace() workspace}.
         * 
         * @param workspace the desired workspace name for the new configuration; if null, then the default workspace will be used
         * @return the new configuration
         */
        public ConfigurationDefinition withWorkspace( String workspace ) {
            return new ConfigurationDefinition(name, source, workspace, path, context, classLoaderFactory);
        }

        /**
         * Return a copy of this configuration that uses the supplied class loader factory instead of this object's
         * {@link #getClassLoaderFactory() class loader factory}.
         * 
         * @param classLoaderFactory the classloader factory, or null if the default factory should be used
         * @return the new configuration
         */
        public ConfigurationDefinition with( ClassLoaderFactory classLoaderFactory ) {
            CheckArg.isNotNull(source, "source");
            return new ConfigurationDefinition(name, source, workspace, path, context, classLoaderFactory);
        }

        /**
         * Return a copy of this configuration that uses the supplied repository source instead of this object's
         * {@link #getRepositorySource() repository source}.
         * 
         * @param source the repository source containing the configuration
         * @return the new configuration
         */
        public ConfigurationDefinition with( RepositorySource source ) {
            return new ConfigurationDefinition(name, source, workspace, path, context, classLoaderFactory);
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

        /**
         * Read a global configuration property.
         * 
         * @param name the property name
         * @return the property, or null if there is no such property
         */
        public Property getGlobalProperty( Name name ) {
            return graph().getProperty(ModeShapeLexicon.GARBAGE_COLLECTION_INTERVAL).on(getPath());
        }
    }
}
