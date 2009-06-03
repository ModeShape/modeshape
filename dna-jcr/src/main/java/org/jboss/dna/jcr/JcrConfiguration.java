/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in JBoss DNA is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
package org.jboss.dna.jcr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.jboss.dna.cnd.CndImporter;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.io.Destination;
import org.jboss.dna.graph.io.GraphBatchDestination;
import org.jboss.dna.graph.mimetype.MimeTypeDetector;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.sequencer.StreamSequencer;
import org.jboss.dna.repository.Configurator;
import org.jboss.dna.repository.DnaConfiguration;
import org.jboss.dna.repository.DnaConfigurationException;
import org.jboss.dna.repository.Configurator.And;
import org.jboss.dna.repository.Configurator.ChooseClass;
import org.jboss.dna.repository.Configurator.ConfigSourceDetails;
import org.jboss.dna.repository.Configurator.MimeTypeDetectorDetails;
import org.jboss.dna.repository.Configurator.RepositorySourceDetails;
import org.jboss.dna.repository.Configurator.SequencerDetails;
import org.jboss.dna.repository.Configurator.SetName;

/**
 * A configuration builder for a {@link JcrEngine}. This class is an internal domain-specific language (DSL), and is designed to
 * be used in a traditional way or in a method-chained manner:
 * 
 * <pre>
 * configuration.addRepository(&quot;Source1&quot;).usingClass(InMemoryRepositorySource.class).describedAs(&quot;description&quot;);
 * configuration.addMimeTypeDetector(&quot;detector&quot;).usingClass(ExtensionBasedMimeTypeDetector.class).describedAs(&quot;default detector&quot;);
 * configuration.addSequencer(&quot;MicrosoftDocs&quot;)
 *              .usingClass(&quot;org.jboss.dna.sequencer.msoffice.MSOfficeMetadataSequencer&quot;)
 *              .loadedFromClasspath()
 *              .named(&quot;Microsoft Document sequencer&quot;)
 *              .describedAs(&quot;Our primary sequencer for all .doc files&quot;)
 *              .sequencingFrom(&quot;/public//(*.(doc|xml|ppt)[*]/jcr:content[@jcr:data]&quot;)
 *              .andOutputtingTo(&quot;/documents/$1&quot;);
 * configuration.save();
 * </pre>
 */
public class JcrConfiguration
    implements Configurator.Initializer<JcrConfiguration>, Configurator.SequencerConfigurator<JcrConfiguration>,
    Configurator.RepositorySourceConfigurator<JcrConfiguration>, Configurator.MimeDetectorConfigurator<JcrConfiguration>,
    Configurator.Builder<JcrEngine> {

    private final JcrConfiguration.Builder<JcrConfiguration> builder;

    /**
     * Create a new configuration for DNA.
     */
    public JcrConfiguration() {
        this(new ExecutionContext());
    }

    /**
     * Specify a new {@link ExecutionContext} that should be used for this DNA instance.
     * 
     * @param context the new context, or null if a default-constructed execution context should be used
     * @throws IllegalArgumentException if the supplied context reference is null
     */
    public JcrConfiguration( ExecutionContext context ) {
        this.builder = new JcrConfiguration.Builder<JcrConfiguration>(context, this);
    }

    /**
     * Get the execution context used by this configurator.
     * 
     * @return the execution context; never null
     */
    public final ExecutionContext getExecutionContext() {
        return builder.getExecutionContext();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.repository.Configurator.Initializer#withConfigurationSource()
     */
    public ChooseClass<RepositorySource, ConfigSourceDetails<JcrConfiguration>> withConfigurationSource() {
        return builder.withConfigurationSource();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.repository.Configurator.SequencerConfigurator#addSequencer(java.lang.String)
     */
    public ChooseClass<StreamSequencer, SequencerDetails<JcrConfiguration>> addSequencer( String id ) {
        CheckArg.isNotEmpty(id, "id");
        return builder.addSequencer(id);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.repository.Configurator.RepositorySourceConfigurator#addSource(java.lang.String)
     */
    public ChooseClass<RepositorySource, RepositorySourceDetails<JcrConfiguration>> addSource( String id ) {
        CheckArg.isNotEmpty(id, "id");
        return builder.addSource(id);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.repository.Configurator.RepositorySourceConfigurator#addSource(org.jboss.dna.graph.connector.RepositorySource)
     */
    public JcrConfiguration addSource( RepositorySource source ) {
        CheckArg.isNotNull(source, "source");
        CheckArg.isNotEmpty(source.getName(), "source.getName()");
        return builder.addSource(source);
    }

    /**
     * Add a JCR repository to this configuration.
     * 
     * @param id the identifier for this repository; may not be null or empty
     * @return the interface used to configure the repository
     */
    public SourceSetter<JcrRepositoryDetails<JcrConfiguration>> addRepository( final String id ) {
        CheckArg.isNotEmpty(id, "id");
        final JcrConfiguration.Builder<JcrConfiguration> builder = this.builder;
        return new SourceSetter<JcrRepositoryDetails<JcrConfiguration>>() {
            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.jcr.JcrConfiguration.SourceSetter#usingSource(java.lang.String)
             */
            public JcrRepositoryDetails<JcrConfiguration> usingSource( String sourceId ) {
                return builder.addRepository(id, sourceId);
            }
        };
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.repository.Configurator.MimeDetectorConfigurator#addMimeTypeDetector(java.lang.String)
     */
    public ChooseClass<MimeTypeDetector, MimeTypeDetectorDetails<JcrConfiguration>> addMimeTypeDetector( String id ) {
        CheckArg.isNotEmpty(id, "id");
        return builder.addMimeTypeDetector(id);
    }

    /**
     * Save any changes that have been made so far to the configuration. This method does nothing if no changes have been made.
     * 
     * @return this configuration object for method chaining purposes; never null
     */
    public JcrConfiguration save() {
        return builder.save();
    }

    protected Graph graph() {
        return builder.getGraph();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.repository.Configurator.Builder#build()
     */
    public JcrEngine build() throws DnaConfigurationException {
        save();
        return new JcrEngine(builder.buildDnaEngine());
    }

    /**
     * The interface used to set the RepositorySource that should be used.
     * 
     * @param <ReturnType> the interface returned from these methods
     */
    public interface SourceSetter<ReturnType> {
        /**
         * Set the repository source that should be used.
         * 
         * @param sourceId that identifier of the repository source
         * @return the next component to continue configuration; never null
         */
        ReturnType usingSource( String sourceId );
    }

    public interface JcrRepositoryDetails<ReturnType>
        extends SetOptions<JcrRepositoryDetails<ReturnType>>, SetNamespace<JcrRepositoryDetails<ReturnType>>,
        SetName<JcrRepositoryDetails<ReturnType>>,
        /* SetDescription<JcrRepositoryDetails<ReturnType>>, */
        And<ReturnType> {

        /**
         * Specify that the CND in the supplied string should be loaded into the repository.
         * 
         * @param cndContents the string containing the compact node definitions
         * @return this object for chained method invocation
         * @throws IllegalArgumentException if the string is null or empty
         * @throws DnaConfigurationException if there is an error reading the CND contents
         */
        JcrRepositoryDetails<ReturnType> withNodeTypes( String cndContents );

        /**
         * Specify that the CND file is to be loaded into the repository.
         * 
         * @param cndFile the CND file
         * @return this object for chained method invocation
         * @throws IllegalArgumentException if the file is null
         * @throws DnaConfigurationException if there is an error reading the file
         */
        JcrRepositoryDetails<ReturnType> withNodeTypes( File cndFile );

        /**
         * Specify that the CND file is to be loaded into the repository.
         * 
         * @param urlOfCndFile the URL of the CND file
         * @return this object for chained method invocation
         * @throws IllegalArgumentException if the URL is null
         * @throws DnaConfigurationException if there is an error reading the content at the URL
         */
        JcrRepositoryDetails<ReturnType> withNodeTypes( URL urlOfCndFile );

        /**
         * Specify that the CND file is to be loaded into the repository.
         * 
         * @param cndContent the stream containing the CND content
         * @return this object for chained method invocation
         * @throws IllegalArgumentException if the URL is null
         * @throws DnaConfigurationException if there is an error reading the stream at the URL
         */
        JcrRepositoryDetails<ReturnType> withNodeTypes( InputStream cndContent );

    }

    /**
     * Interface for configuring the {@link JcrRepository.Option JCR repository options} for a {@link JcrRepository JCR
     * repository}.
     * 
     * @param <ReturnType> the interface returned after the option has been set.
     */
    public interface SetOptions<ReturnType> {
        /**
         * Specify the repository option that is to be set. The value may be set using the interface returned by this method.
         * 
         * @param option the option to be set
         * @return the interface used to set the value for the property; never null
         */
        OptionSetter<ReturnType> with( JcrRepository.Option option );
    }

    /**
     * The interface used to set the value for a {@link JcrRepository.Option JCR repository option}.
     * 
     * @param <ReturnType> the interface returned from these methods
     * @see JcrConfiguration.SetOptions#with(org.jboss.dna.jcr.JcrRepository.Option)
     */
    public interface OptionSetter<ReturnType> {
        /**
         * Set the property value to an integer.
         * 
         * @param value the new value for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setTo( String value );
    }

    /**
     * Interface for setting a namespace for a {@link JcrRepository JCR repository}.
     * 
     * @param <ReturnType> the interface returned after the option has been set.
     */
    public interface SetNamespace<ReturnType> {
        /**
         * Specify the repository option that is to be set. The value may be set using the interface returned by this method.
         * 
         * @param uri the uri for the namespace
         * @return the interface used to set the value for the property; never null
         */
        NamespaceSetter<ReturnType> withNamespace( String uri );
    }

    /**
     * The interface used to set the prefix for a namespace.
     * 
     * @param <ReturnType> the interface returned from these methods
     * @see JcrConfiguration.SetNamespace#withNamespace(String)
     */
    public interface NamespaceSetter<ReturnType> {
        /**
         * Set the prefix for the namespace
         * 
         * @param prefix the prefix for the namespace
         * @return the next component to continue configuration; never null
         */
        ReturnType usingPrefix( String prefix );
    }

    public static class Builder<ReturnType> extends DnaConfiguration.Builder<ReturnType> {

        private Path repositoriesPath;

        /**
         * Specify a new {@link ExecutionContext} that should be used for this DNA instance.
         * 
         * @param context the new context, or null if a default-constructed execution context should be used
         * @param builder the builder object returned from all the methods
         * @throws IllegalArgumentException if the supplied context reference is null
         */
        public Builder( ExecutionContext context,
                        ReturnType builder ) {
            super(context, builder);
        }

        protected Graph getGraph() {
            return graph();
        }

        protected Path repositoriesPath() {
            // Make sure the "dna:repositories" node is there
            if (repositoriesPath == null) {
                Path path = pathFactory().create(this.configurationSource.getPath(), DnaLexicon.REPOSITORIES);
                Node node = graph().createIfMissing(path).andReturn();
                this.repositoriesPath = node.getLocation().getPath();
            }
            return this.repositoriesPath;
        }

        public JcrRepositoryDetails<ReturnType> addRepository( String id,
                                                               String sourceId ) {
            CheckArg.isNotEmpty(id, "id");
            // Now create the "dna:repositories/id" node ...
            Path path = createOrReplaceNode(repositoriesPath(), id);
            configuration().set(DnaLexicon.SOURCE_NAME).to(sourceId).on(path);
            return new JcrGraphRepositoryDetails<ReturnType>(path, builder);
        }

        public class JcrGraphRepositoryDetails<RT> implements JcrRepositoryDetails<RT> {
            private final Path path;
            private final RT returnObject;

            protected JcrGraphRepositoryDetails( Path path,
                                                 RT returnObject ) {
                this.path = path;
                this.returnObject = returnObject;
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.repository.Configurator.SetName#named(java.lang.String)
             */
            @SuppressWarnings( "synthetic-access" )
            public JcrRepositoryDetails<RT> named( String name ) {
                configuration().set(DnaLexicon.READABLE_NAME).to(name).on(name);
                return this;
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.jcr.JcrConfiguration.SetNamespace#withNamespace(java.lang.String)
             */
            @SuppressWarnings( "synthetic-access" )
            public NamespaceSetter<JcrRepositoryDetails<RT>> withNamespace( final String uri ) {
                final Path namespacesPath = createOrReplaceNode(path, DnaLexicon.NAMESPACES);
                final JcrRepositoryDetails<RT> details = this;
                return new NamespaceSetter<JcrRepositoryDetails<RT>>() {
                    /**
                     * {@inheritDoc}
                     * 
                     * @see org.jboss.dna.jcr.JcrConfiguration.NamespaceSetter#usingPrefix(java.lang.String)
                     */
                    public JcrRepositoryDetails<RT> usingPrefix( String prefix ) {
                        Path nsPath = createOrReplaceNode(namespacesPath, prefix);
                        configuration().set(DnaLexicon.URI).to(uri).on(nsPath);
                        return details;
                    }
                };
            }

            @SuppressWarnings( "synthetic-access" )
            public OptionSetter<JcrRepositoryDetails<RT>> with( final JcrRepository.Option option ) {
                final Path optionsPath = createOrReplaceNode(path, DnaLexicon.OPTIONS);
                final JcrRepositoryDetails<RT> details = this;
                return new OptionSetter<JcrRepositoryDetails<RT>>() {
                    public JcrRepositoryDetails<RT> setTo( String value ) {
                        Path optionPath = createOrReplaceNode(optionsPath, option.name());
                        configuration().set(DnaLexicon.VALUE).to(value).on(optionPath);
                        return details;
                    }
                };
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.jcr.JcrConfiguration.JcrRepositoryDetails#withNodeTypes(java.lang.String)
             */
            public JcrRepositoryDetails<RT> withNodeTypes( String content ) {
                CheckArg.isNotEmpty(content, "content");
                CndImporter importer = createCndImporter();
                try {
                    importer.importFrom(content, getProblems(), "stream");
                } catch (IOException e) {
                    throw new DnaConfigurationException(e);
                }
                return this;
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.jcr.JcrConfiguration.JcrRepositoryDetails#withNodeTypes(java.net.URL)
             */
            public JcrRepositoryDetails<RT> withNodeTypes( URL url ) {
                CheckArg.isNotNull(url, "url");
                // Obtain the stream ...
                InputStream stream = null;
                boolean foundError = false;
                try {
                    stream = url.openStream();
                    CndImporter importer = createCndImporter();
                    importer.importFrom(stream, getProblems(), url.toString());
                } catch (IOException e) {
                    foundError = true;
                    throw new DnaConfigurationException(e);
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            if (!foundError) {
                                throw new DnaConfigurationException(e);
                            }
                        }
                    }
                }
                return this;
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.jcr.JcrConfiguration.JcrRepositoryDetails#withNodeTypes(java.io.File)
             */
            public JcrRepositoryDetails<RT> withNodeTypes( File file ) {
                CheckArg.isNotNull(file, "file");
                if (file.exists() && file.canRead()) {
                    CndImporter importer = createCndImporter();
                    try {
                        importer.importFrom(file, getProblems());
                    } catch (IOException e) {
                        throw new DnaConfigurationException(e);
                    }
                    return this;
                }
                throw new DnaConfigurationException(JcrI18n.fileDoesNotExist.text(file.getPath()));
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.jcr.JcrConfiguration.JcrRepositoryDetails#withNodeTypes(java.io.InputStream)
             */
            public JcrRepositoryDetails<RT> withNodeTypes( InputStream stream ) {
                CndImporter importer = createCndImporter();
                try {
                    importer.importFrom(stream, getProblems(), "stream");
                } catch (IOException e) {
                    throw new DnaConfigurationException(e);
                }
                return this;
            }

            @SuppressWarnings( "synthetic-access" )
            protected CndImporter createCndImporter() {
                // The node types will be loaded into 'dna:repositories/{repositoryName}/dna:nodeTypes/' ...
                Path nodeTypesPath = createOrReplaceNode(path, DnaLexicon.NODE_TYPES);

                // Now set up the destination ...
                Destination destination = new GraphBatchDestination(graph().batch()); // will be executed

                // And create the importer that will load the CND content into the repository ...
                return new CndImporter(destination, nodeTypesPath);
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.repository.Configurator.And#and()
             */
            public RT and() {
                return returnObject;
            }
        }

    }
}
