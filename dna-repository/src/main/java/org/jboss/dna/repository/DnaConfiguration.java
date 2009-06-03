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
package org.jboss.dna.repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.mimetype.MimeTypeDetector;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.sequencer.StreamSequencer;
import org.jboss.dna.repository.Configurator.ChooseClass;
import org.jboss.dna.repository.Configurator.ConfigSourceDetails;
import org.jboss.dna.repository.Configurator.ConfigurationRepository;
import org.jboss.dna.repository.Configurator.MimeTypeDetectorDetails;
import org.jboss.dna.repository.Configurator.RepositorySourceDetails;
import org.jboss.dna.repository.Configurator.SequencerDetails;

/**
 * 
 */
public class DnaConfiguration
    implements Configurator.Initializer<DnaConfiguration>, Configurator.SequencerConfigurator<DnaConfiguration>,
    Configurator.RepositorySourceConfigurator<DnaConfiguration>, Configurator.MimeDetectorConfigurator<DnaConfiguration>,
    Configurator.Builder<DnaEngine> {

    protected static final Map<String, Name> NAMES_TO_MAP;
    static {
        Map<String, Name> names = new HashMap<String, Name>();
        names.put(DnaLexicon.READABLE_NAME.getLocalName(), DnaLexicon.READABLE_NAME);
        names.put(DnaLexicon.DESCRIPTION.getLocalName(), DnaLexicon.DESCRIPTION);
        names.put(DnaLexicon.DEFAULT_CACHE_POLICY.getLocalName(), DnaLexicon.DEFAULT_CACHE_POLICY);
        names.put(DnaLexicon.RETRY_LIMIT.getLocalName(), DnaLexicon.RETRY_LIMIT);
        names.put(DnaLexicon.PATH_EXPRESSIONS.getLocalName(), DnaLexicon.PATH_EXPRESSIONS);
        names.put(DnaLexicon.CLASSNAME.getLocalName(), DnaLexicon.CLASSNAME);
        names.put(DnaLexicon.CLASSPATH.getLocalName(), DnaLexicon.CLASSPATH);
        NAMES_TO_MAP = Collections.unmodifiableMap(names);
    }

    private final Builder<DnaConfiguration> builder;

    /**
     * Create a new configuration for DNA.
     */
    public DnaConfiguration() {
        this(new ExecutionContext());
    }

    /**
     * Specify a new {@link ExecutionContext} that should be used for this DNA instance.
     * 
     * @param context the new context, or null if a default-constructed execution context should be used
     * @throws IllegalArgumentException if the supplied context reference is null
     */
    public DnaConfiguration( ExecutionContext context ) {
        this.builder = new Builder<DnaConfiguration>(context, this);
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
    public ChooseClass<RepositorySource, ConfigSourceDetails<DnaConfiguration>> withConfigurationSource() {
        return builder.withConfigurationSource();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.repository.Configurator.RepositorySourceConfigurator#addSource(java.lang.String)
     */
    public ChooseClass<RepositorySource, RepositorySourceDetails<DnaConfiguration>> addSource( String id ) {
        return builder.addSource(id);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.repository.Configurator.RepositorySourceConfigurator#addSource(org.jboss.dna.graph.connector.RepositorySource)
     */
    public DnaConfiguration addSource( RepositorySource source ) {
        return builder.addSource(source);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.repository.Configurator.SequencerConfigurator#addSequencer(java.lang.String)
     */
    public ChooseClass<StreamSequencer, SequencerDetails<DnaConfiguration>> addSequencer( String id ) {
        return builder.addSequencer(id);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.repository.Configurator.MimeDetectorConfigurator#addMimeTypeDetector(java.lang.String)
     */
    public ChooseClass<MimeTypeDetector, MimeTypeDetectorDetails<DnaConfiguration>> addMimeTypeDetector( String id ) {
        return builder.addMimeTypeDetector(id);
    }

    /**
     * Save any changes that have been made so far to the configuration. This method does nothing if no changes have been made.
     * 
     * @return this configuration object for method chaining purposes; never null
     */
    public DnaConfiguration save() {
        return builder.save();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.repository.Configurator.Builder#build()
     */
    public DnaEngine build() throws DnaConfigurationException {
        save();
        return new DnaEngine(builder.getExecutionContext(), builder.configurationSource);
    }

    protected Graph graph() {
        return builder.graph();
    }

    protected ConfigurationRepository configurationRepository() {
        return builder.configurationSource;
    }

    public static class Builder<ReturnType> extends Configurator<ReturnType>
        implements Configurator.Initializer<ReturnType>, Configurator.SequencerConfigurator<ReturnType>,
        Configurator.RepositorySourceConfigurator<ReturnType>, Configurator.MimeDetectorConfigurator<ReturnType> {

        private Path sourcesPath;
        private Path sequencersPath;
        private Path detectorsPath;

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

        public DnaEngine buildDnaEngine() {
            return new DnaEngine(context, configurationSource);
        }

        protected Path sourcesPath() {
            // Make sure the "dna:sources" node is there
            if (sourcesPath == null) {
                Path path = pathFactory().create(this.configurationSource.getPath(), DnaLexicon.SOURCES);
                Node node = graph().createIfMissing(path).andReturn();
                this.sourcesPath = node.getLocation().getPath();
            }
            return this.sourcesPath;
        }

        protected Path sequencersPath() {
            // Make sure the "dna:sequencers" node is there
            if (sequencersPath == null) {
                Path path = pathFactory().create(this.configurationSource.getPath(), DnaLexicon.SEQUENCERS);
                Node node = graph().createIfMissing(path).andReturn();
                this.sequencersPath = node.getLocation().getPath();
            }
            return this.sequencersPath;
        }

        protected Path detectorsPath() {
            // Make sure the "dna:mimeTypeDetectors" node is there
            if (detectorsPath == null) {
                Path path = pathFactory().create(this.configurationSource.getPath(), DnaLexicon.MIME_TYPE_DETECTORS);
                Node node = graph().createIfMissing(path).andReturn();
                this.detectorsPath = node.getLocation().getPath();
            }
            return this.detectorsPath;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.Initializer#withConfigurationSource()
         */
        public ChooseClass<RepositorySource, ConfigSourceDetails<ReturnType>> withConfigurationSource() {
            return new ConfigurationRepositoryClassChooser<ReturnType>(builder);
        }
        
        /**
         * {@inheritDoc}
         *
         * @see org.jboss.dna.repository.Configurator.Initializer#configurationSource()
         */
        public ConfigSourceDetails<ReturnType> configurationSource() {
            return configurationSource;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SequencerConfigurator#addSequencer(java.lang.String)
         */
        public ChooseClass<StreamSequencer, SequencerDetails<ReturnType>> addSequencer( String id ) {
            CheckArg.isNotEmpty(id, "id");
            // Now create the "dna:sequencer" node with the supplied id ...
            Path path = createOrReplaceNode(sequencersPath(), id, DnaLexicon.READABLE_NAME, id);
            SequencerDetails<ReturnType> details = new GraphSequencerDetails<ReturnType>(path, builder);
            return new ClassChooser<StreamSequencer, SequencerDetails<ReturnType>>(path, details);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.RepositorySourceConfigurator#addSource(java.lang.String)
         */
        public ChooseClass<RepositorySource, RepositorySourceDetails<ReturnType>> addSource( String id ) {
            CheckArg.isNotEmpty(id, "id");
            // Now create the "dna:source" node with the supplied id ...
            Path path = createOrReplaceNode(sourcesPath(), id, DnaLexicon.READABLE_NAME, id);
            RepositorySourceDetails<ReturnType> details = new GraphRepositorySourceDetails<ReturnType>(path, builder);
            return new ClassChooser<RepositorySource, RepositorySourceDetails<ReturnType>>(path, details);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.RepositorySourceConfigurator#addSource(org.jboss.dna.graph.connector.RepositorySource)
         */
        public ReturnType addSource( RepositorySource source ) {
            CheckArg.isNotNull(source, "source");
            CheckArg.isNotEmpty(source.getName(), "source.getName()");
            String name = source.getName();
            RepositorySourceDetails<ReturnType> details = addSource(source.getName()).usingClass(source.getClass().getName())
                                                                                     .loadedFromClasspath();
            // Record all of the bean properties ...
            Path sourcePath = pathFactory().create(sourcesPath(), name);
            recordBeanPropertiesInGraph(sourcePath, source);
            return details.and();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.MimeDetectorConfigurator#addMimeTypeDetector(java.lang.String)
         */
        public ChooseClass<MimeTypeDetector, MimeTypeDetectorDetails<ReturnType>> addMimeTypeDetector( String id ) {
            CheckArg.isNotEmpty(id, "id");
            // Now create the "dna:sequencer" node with the supplied id ...
            Path detectorPath = createOrReplaceNode(detectorsPath(), id, DnaLexicon.READABLE_NAME, id);
            MimeTypeDetectorDetails<ReturnType> details = new GraphMimeTypeDetectorDetails<ReturnType>(detectorPath, builder);
            return new ClassChooser<MimeTypeDetector, MimeTypeDetectorDetails<ReturnType>>(detectorPath, details);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator#nameFor(java.lang.String)
         */
        @Override
        protected Name nameFor( String name ) {
            Name result = NAMES_TO_MAP.get(name);
            if (result == null) result = context.getValueFactories().getNameFactory().create(name);
            return result;
        }
    }

}
