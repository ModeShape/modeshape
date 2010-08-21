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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.cnd.CndImporter;
import org.modeshape.common.collection.Problem;
import org.modeshape.common.collection.Problem.Status;
import org.modeshape.common.component.ClassLoaderFactory;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Location;
import org.modeshape.graph.Node;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.io.Destination;
import org.modeshape.graph.io.GraphBatchDestination;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.NamespaceRegistry.Namespace;
import org.modeshape.jcr.JcrRepository.Option;
import org.modeshape.repository.ModeShapeConfiguration;
import org.modeshape.repository.ModeShapeConfigurationException;
import org.xml.sax.SAXException;

/**
 * A configuration builder for a {@link JcrEngine}. This class is an internal domain-specific language (DSL), and is designed to
 * be used in a traditional way or in a method-chained manner:
 * 
 * <pre>
 * configuration.repositorySource(&quot;Source1&quot;).setClass(InMemoryRepositorySource.class).setDescription(&quot;description&quot;);
 * configuration.mimeTypeDetector(&quot;detector&quot;).setClass(ExtensionBasedMimeTypeDetector.class).setDescription(&quot;default detector&quot;);
 * configuration.sequencer(&quot;MicrosoftDocs&quot;)
 *              .setClass(&quot;org.modeshape.sequencer.msoffice.MSOfficeMetadataSequencer&quot;)
 *              .setDescription(&quot;Our primary sequencer for all .doc files&quot;)
 *              .sequencingFrom(&quot;/public//(*.(doc|xml|ppt)[*]/jcr:content[@jcr:data]&quot;)
 *              .andOutputtingTo(&quot;/documents/$1&quot;);
 * configuration.repository(&quot;MyRepository&quot;).setSource(&quot;Source1&quot;);
 * configuration.save();
 * </pre>
 */
@NotThreadSafe
public class JcrConfiguration extends ModeShapeConfiguration {

    /**
     * Interface used to define a JCR Repository that's accessible from the JcrEngine.
     * 
     * @param <ReturnType>
     */
    public interface RepositoryDefinition<ReturnType>
        extends Returnable<ReturnType>, Removable<ReturnType>, SetDescription<RepositoryDefinition<ReturnType>> {

        /**
         * Specify the name of the repository source that is to be used by this JCR repository.
         * 
         * @param sourceName the name of the repository source that should be exposed by this JCR repository
         * @return the interface used to set the value for the property; never null
         * @throws IllegalArgumentException if the source name parameter is null
         */
        RepositoryDefinition<ReturnType> setSource( String sourceName );

        /**
         * Get the name of the repository source that is to be used by this JCR repository.
         * 
         * @return the source name, or null if it has not yet been set
         */
        String getSource();

        /**
         * Specify the repository option that is to be set.
         * 
         * @param option the option to be set
         * @param value the new value for the option
         * @return the interface used to set the value for the property; never null
         * @throws IllegalArgumentException if either parameter is null
         */
        RepositoryDefinition<ReturnType> setOption( JcrRepository.Option option,
                                                    String value );

        /**
         * Specify the repository option that is to be set.
         * 
         * @param option the option to be set
         * @param value the new value for the option
         * @return the interface used to set the value for the property; never null
         * @throws IllegalArgumentException if either parameter is null
         */
        RepositoryDefinition<ReturnType> setOption( JcrRepository.Option option,
                                                    boolean value );

        /**
         * Specify the repository option that is to be set.
         * 
         * @param option the option to be set
         * @param value the new value for the option
         * @return the interface used to set the value for the property; never null
         * @throws IllegalArgumentException if either parameter is null
         */
        RepositoryDefinition<ReturnType> setOption( JcrRepository.Option option,
                                                    int value );

        /**
         * Specify the repository option that is to be set.
         * 
         * @param option the option to be set
         * @param value the new value for the option
         * @return the interface used to set the value for the property; never null
         * @throws IllegalArgumentException if either parameter is null
         */
        RepositoryDefinition<ReturnType> setOption( JcrRepository.Option option,
                                                    long value );

        /**
         * Specify the repository option that is to be set.
         * 
         * @param option the option to be set
         * @param value the new value for the option
         * @return the interface used to set the value for the property; never null
         * @throws IllegalArgumentException if either parameter is null
         */
        RepositoryDefinition<ReturnType> setOption( JcrRepository.Option option,
                                                    float value );

        /**
         * Specify the repository option that is to be set.
         * 
         * @param option the option to be set
         * @param value the new value for the option
         * @return the interface used to set the value for the property; never null
         * @throws IllegalArgumentException if either parameter is null
         */
        RepositoryDefinition<ReturnType> setOption( JcrRepository.Option option,
                                                    double value );

        /**
         * Get the value for the repository option.
         * 
         * @param option the option
         * @return the current option value, which may be null if the option has not been set (and its default would be used)
         * @throws IllegalArgumentException if the option parameter is null
         */
        String getOption( JcrRepository.Option option );

        /**
         * Specify that the CND file located at the supplied path should be loaded into the repository.
         * <p>
         * This method may be called multiple times, once per CND file.
         * </p>
         * 
         * @param pathToCndFile the path to the CND file
         * @return this object for chained method invocation
         * @throws IllegalArgumentException if the string is null or empty
         * @throws ModeShapeConfigurationException if there is an error reading the CND file
         */
        RepositoryDefinition<ReturnType> addNodeTypes( String pathToCndFile );

        /**
         * Specify that the CND file is to be loaded into the repository.
         * 
         * @param cndFile the CND file
         * @return this object for chained method invocation
         * @throws IllegalArgumentException if the file is null
         * @throws ModeShapeConfigurationException if there is an error reading the file
         */
        RepositoryDefinition<ReturnType> addNodeTypes( File cndFile );

        /**
         * Specify that the CND file is to be loaded into the repository.
         * <p>
         * This method may be called multiple times, once per CND file.
         * </p>
         * 
         * @param urlOfCndFile the URL of the CND file
         * @return this object for chained method invocation
         * @throws IllegalArgumentException if the URL is null
         * @throws ModeShapeConfigurationException if there is an error reading the content at the URL
         */
        RepositoryDefinition<ReturnType> addNodeTypes( URL urlOfCndFile );

        /**
         * Specify that the CND file is to be loaded into the repository.
         * <p>
         * This method may be called multiple times, once per CND file.
         * </p>
         * 
         * @param cndContent the stream containing the CND content
         * @return this object for chained method invocation
         * @throws IllegalArgumentException if the URL is null
         * @throws ModeShapeConfigurationException if there is an error reading the stream at the URL
         */
        RepositoryDefinition<ReturnType> addNodeTypes( InputStream cndContent );

        /**
         * Specify the namespace binding that should be made available in this repository.
         * 
         * @param prefix the namespace prefix; may not be null or empty, and must be a valid prefix
         * @param uri the uri for the namespace; may not be null or empty
         * @return the interface used to set the value for the property; never null
         */
        RepositoryDefinition<ReturnType> registerNamespace( String prefix,
                                                            String uri );
    }

    private final Map<String, RepositoryDefinition<? extends JcrConfiguration>> repositoryDefinitions = new HashMap<String, RepositoryDefinition<? extends JcrConfiguration>>();

    /**
     * Create a new configuration, using a default-constructed {@link ExecutionContext}.
     */
    public JcrConfiguration() {
        super();
    }

    /**
     * Create a new configuration using the supplied {@link ExecutionContext}.
     * 
     * @param context the execution context
     * @throws IllegalArgumentException if the path is null or empty
     */
    public JcrConfiguration( ExecutionContext context ) {
        super(context);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IOException
     * @throws SAXException
     * @see org.modeshape.repository.ModeShapeConfiguration#loadFrom(java.lang.String)
     */
    @Override
    public JcrConfiguration loadFrom( String pathToFile ) throws IOException, SAXException {
        super.loadFrom(pathToFile);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.repository.ModeShapeConfiguration#loadFrom(java.lang.String, java.lang.String)
     */
    @Override
    public JcrConfiguration loadFrom( String pathToConfigurationFile,
                                      String path ) throws IOException, SAXException {
        super.loadFrom(pathToConfigurationFile, path);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.repository.ModeShapeConfiguration#loadFrom(java.io.File)
     */
    @Override
    public JcrConfiguration loadFrom( File configurationFile ) throws IOException, SAXException {
        super.loadFrom(configurationFile);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.repository.ModeShapeConfiguration#loadFrom(java.io.File, java.lang.String)
     */
    @Override
    public JcrConfiguration loadFrom( File configurationFile,
                                      String path ) throws IOException, SAXException {
        super.loadFrom(configurationFile, path);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.repository.ModeShapeConfiguration#loadFrom(java.net.URL)
     */
    @Override
    public JcrConfiguration loadFrom( URL urlToConfigurationFile ) throws IOException, SAXException {
        super.loadFrom(urlToConfigurationFile);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.repository.ModeShapeConfiguration#loadFrom(java.net.URL, java.lang.String)
     */
    @Override
    public JcrConfiguration loadFrom( URL urlToConfigurationFile,
                                      String path ) throws IOException, SAXException {
        super.loadFrom(urlToConfigurationFile, path);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.repository.ModeShapeConfiguration#loadFrom(java.io.InputStream)
     */
    @Override
    public JcrConfiguration loadFrom( InputStream configurationFileInputStream ) throws IOException, SAXException {
        super.loadFrom(configurationFileInputStream);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.repository.ModeShapeConfiguration#loadFrom(java.io.InputStream, java.lang.String)
     */
    @Override
    public JcrConfiguration loadFrom( InputStream configurationFileInputStream,
                                      String path ) throws IOException, SAXException {
        super.loadFrom(configurationFileInputStream, path);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.repository.ModeShapeConfiguration#loadFrom(org.modeshape.graph.connector.RepositorySource)
     */
    @Override
    public JcrConfiguration loadFrom( RepositorySource source ) {
        super.loadFrom(source);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.repository.ModeShapeConfiguration#loadFrom(org.modeshape.graph.connector.RepositorySource,
     *      java.lang.String)
     */
    @Override
    public JcrConfiguration loadFrom( RepositorySource source,
                                      String workspaceName ) {
        super.loadFrom(source, workspaceName);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.repository.ModeShapeConfiguration#loadFrom(org.modeshape.graph.connector.RepositorySource,
     *      java.lang.String, java.lang.String)
     */
    @Override
    public JcrConfiguration loadFrom( RepositorySource source,
                                      String workspaceName,
                                      String pathInWorkspace ) {
        super.loadFrom(source, workspaceName, pathInWorkspace);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.repository.ModeShapeConfiguration#and()
     */
    @Override
    public JcrConfiguration and() {
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.repository.ModeShapeConfiguration#withClassLoaderFactory(org.modeshape.common.component.ClassLoaderFactory)
     */
    @Override
    public JcrConfiguration withClassLoaderFactory( ClassLoaderFactory classLoaderFactory ) {
        super.withClassLoaderFactory(classLoaderFactory);
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.repository.ModeShapeConfiguration#mimeTypeDetector(java.lang.String)
     */
    @Override
    public MimeTypeDetectorDefinition<JcrConfiguration> mimeTypeDetector( String name ) {
        return mimeTypeDetectorDefinition(this, name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.repository.ModeShapeConfiguration#repositorySource(java.lang.String)
     */
    @Override
    public RepositorySourceDefinition<JcrConfiguration> repositorySource( String name ) {
        return repositorySourceDefinition(this, name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.repository.ModeShapeConfiguration#sequencer(java.lang.String)
     */
    @Override
    public SequencerDefinition<JcrConfiguration> sequencer( String name ) {
        return sequencerDefinition(this, name);
    }

    /**
     * Obtain or create a definition for the {@link javax.jcr.Repository JCR Repository} with the supplied name or identifier. A
     * new definition will be created if there currently is no sequencer defined with the supplied name.
     * 
     * @param name the name or identifier of the sequencer
     * @return the details of the sequencer definition; never null
     */
    public RepositoryDefinition<JcrConfiguration> repository( String name ) {
        return repositoryDefinition(this, name);
    }

    /**
     * Get the list of sequencer definitions.
     * 
     * @return the unmodifiable set of definitions; never null but possibly empty if there are no definitions
     */
    public Set<RepositoryDefinition<JcrConfiguration>> repositories() {
        // Get the children under the 'dna:mimeTypeDetectors' node ...
        Set<String> names = getNamesOfComponentsUnder(ModeShapeLexicon.REPOSITORIES);
        names.addAll(this.repositoryDefinitions.keySet());
        Set<RepositoryDefinition<JcrConfiguration>> results = new HashSet<RepositoryDefinition<JcrConfiguration>>();
        for (String name : names) {
            results.add(repository(name));
        }
        return Collections.unmodifiableSet(results);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.repository.ModeShapeConfiguration#save()
     */
    @Override
    public JcrConfiguration save() {
        super.save();
        this.repositoryDefinitions.clear();
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.repository.ModeShapeConfiguration#build()
     */
    @Override
    public JcrEngine build() {
        save();
        return new JcrEngine(getExecutionContextForEngine(), getConfigurationDefinition());
    }

    /**
     * Utility method to construct a definition object for the repository with the supplied name and return type.
     * 
     * @param <ReturnType> the type of the return object
     * @param returnObject the return object
     * @param name the name of the repository
     * @return the definition for the repository
     */
    @SuppressWarnings( "unchecked" )
    protected <ReturnType extends JcrConfiguration> RepositoryDefinition<ReturnType> repositoryDefinition( ReturnType returnObject,
                                                                                                           String name ) {
        RepositoryDefinition<ReturnType> definition = (RepositoryDefinition<ReturnType>)repositoryDefinitions.get(name);
        if (definition == null) {
            definition = new RepositoryBuilder<ReturnType>(returnObject, changes(), path(), ModeShapeLexicon.REPOSITORIES,
                                                           name(name));
            repositoryDefinitions.put(name, definition);
        }
        return definition;
    }

    protected class RepositoryBuilder<ReturnType> extends GraphReturnable<ReturnType, RepositoryDefinition<ReturnType>>
        implements RepositoryDefinition<ReturnType> {
        private final EnumMap<JcrRepository.Option, String> optionValues = new EnumMap<Option, String>(Option.class);

        protected RepositoryBuilder( ReturnType returnObject,
                                     Graph.Batch batch,
                                     Path path,
                                     Name... names ) {
            super(returnObject, batch, path, names);
            // Load the current options ...
            try {
                Path optionsPath = context.getValueFactories().getPathFactory().create(path, ModeShapeLexicon.OPTIONS);
                Subgraph options = batch.getGraph().getSubgraphOfDepth(2).at(optionsPath);
                for (Location optionChild : options.getRoot().getChildren()) {
                    Node option = options.getNode(optionChild);
                    Property property = option.getProperty(ModeShapeLexicon.VALUE);
                    if (property != null && property.isEmpty()) {
                        try {
                            Option key = Option.findOption(optionChild.getPath()
                                                                      .getLastSegment()
                                                                      .getString(context.getNamespaceRegistry()));
                            String value = context.getValueFactories().getStringFactory().create(property.getFirstValue());
                            optionValues.put(key, value);
                        } catch (IllegalArgumentException e) {
                            // the key is not valid, so skip it ...
                        }
                    }
                }
            } catch (PathNotFoundException e) {
                // No current options
            }
        }

        @Override
        protected RepositoryDefinition<ReturnType> thisType() {
            return this;
        }

        public RepositoryDefinition<ReturnType> setSource( String sourceName ) {
            setProperty(ModeShapeLexicon.SOURCE_NAME, sourceName);
            return this;
        }

        public String getSource() {
            Property property = getProperty(ModeShapeLexicon.SOURCE_NAME);
            if (property != null && !property.isEmpty()) {
                return context.getValueFactories().getStringFactory().create(property.getFirstValue());
            }
            return null;
        }

        public RepositoryDefinition<ReturnType> setOption( JcrRepository.Option option,
                                                           String value ) {
            CheckArg.isNotNull(option, "option");
            CheckArg.isNotNull(value, "value");
            createIfMissing(ModeShapeLexicon.OPTIONS, option.name()).with(ModeShapeLexicon.VALUE, value.trim()).and();
            optionValues.put(option, value);
            return this;
        }

        public RepositoryDefinition<ReturnType> setOption( Option option,
                                                           boolean value ) {
            return setOption(option, Boolean.toString(value));
        }

        public RepositoryDefinition<ReturnType> setOption( Option option,
                                                           int value ) {
            return setOption(option, Integer.toString(value));
        }

        public RepositoryDefinition<ReturnType> setOption( Option option,
                                                           long value ) {
            return setOption(option, Long.toString(value));
        }

        public RepositoryDefinition<ReturnType> setOption( Option option,
                                                           float value ) {
            return setOption(option, Float.toString(value));
        }

        public RepositoryDefinition<ReturnType> setOption( Option option,
                                                           double value ) {
            return setOption(option, Double.toString(value));
        }

        public String getOption( Option option ) {
            CheckArg.isNotNull(option, "option");
            return optionValues.get(option);
        }

        public RepositoryDefinition<ReturnType> registerNamespace( String prefix,
                                                                   String uri ) {
            CheckArg.isNotEmpty(prefix, "prefix");
            CheckArg.isNotEmpty(uri, "uri");
            prefix = prefix.trim();
            uri = uri.trim();
            createIfMissing(ModeShapeLexicon.NAMESPACES, prefix).with(ModeShapeLexicon.URI, uri).and();
            return this;
        }

        public RepositoryDefinition<ReturnType> addNodeTypes( String pathToCndFile ) {
            CheckArg.isNotEmpty(pathToCndFile, "pathToCndFile");
            return addNodeTypes(new File(pathToCndFile));
        }

        public RepositoryDefinition<ReturnType> addNodeTypes( File file ) {
            CheckArg.isNotNull(file, "file");
            if (file.exists() && file.canRead()) {
                CndImporter importer = createCndImporter();
                try {
                    Set<Namespace> namespacesBefore = batch.getGraph().getContext().getNamespaceRegistry().getNamespaces();
                    importer.importFrom(file, getProblems());

                    // Record any new namespaces added by this import ...
                    registerNewNamespaces(namespacesBefore);
                } catch (IOException e) {
                    throw new ModeShapeConfigurationException(e);
                }
                return this;
            }
            throw new ModeShapeConfigurationException(JcrI18n.fileDoesNotExist.text(file.getPath()));
        }

        public RepositoryDefinition<ReturnType> addNodeTypes( URL url ) {
            CheckArg.isNotNull(url, "url");
            // Obtain the stream ...
            InputStream stream = null;
            boolean foundError = false;
            try {
                Set<Namespace> namespacesBefore = batch.getGraph().getContext().getNamespaceRegistry().getNamespaces();
                stream = url.openStream();
                CndImporter importer = createCndImporter();
                importer.importFrom(stream, getProblems(), url.toString());

                if (getProblems().hasErrors()) {
                    for (Problem problem : getProblems()) {
                        if (problem.getStatus() == Status.ERROR) {
                            throw new ModeShapeConfigurationException(problem.getThrowable());
                        }
                    }
                }
                // Record any new namespaces added by this import ...
                registerNewNamespaces(namespacesBefore);
            } catch (IOException e) {
                foundError = true;
                throw new ModeShapeConfigurationException(e);
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        if (!foundError) {
                            throw new ModeShapeConfigurationException(e);
                        }
                    }
                }
            }
            return this;
        }

        public RepositoryDefinition<ReturnType> addNodeTypes( InputStream cndContent ) {
            CndImporter importer = createCndImporter();
            try {
                Set<Namespace> namespacesBefore = batch.getGraph().getContext().getNamespaceRegistry().getNamespaces();
                importer.importFrom(cndContent, getProblems(), "stream");

                // Record any new namespaces added by this import ...
                registerNewNamespaces(namespacesBefore);
            } catch (IOException e) {
                throw new ModeShapeConfigurationException(e);
            }
            return this;
        }

        protected void registerNewNamespaces( Set<Namespace> namespacesBefore ) {
            Set<Namespace> namespacesAfter = batch.getGraph().getContext().getNamespaceRegistry().getNamespaces();
            Set<Namespace> newNamespaces = new HashSet<Namespace>(namespacesAfter);
            newNamespaces.removeAll(namespacesBefore);
            for (Namespace namespace : newNamespaces) {
                registerNamespace(namespace.getPrefix(), namespace.getNamespaceUri());
            }
        }

        protected CndImporter createCndImporter() {
            // The node types will be loaded into 'dna:repositories/{repositoryName}/dna:nodeTypes/' ...
            Path nodeTypesPath = subpath(JcrLexicon.NODE_TYPES);
            createIfMissing(JcrLexicon.NODE_TYPES).and();

            // Now set up the destination, but make it so that ...
            Destination destination = new GraphBatchDestination(batch, true); // will NOT be executed

            // And create the importer that will load the CND content into the repository ...
            return new CndImporter(destination, nodeTypesPath);
        }
    }

}
