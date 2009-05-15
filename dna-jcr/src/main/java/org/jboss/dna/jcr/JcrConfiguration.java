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

import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.mimetype.MimeTypeDetector;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.repository.Configurator;
import org.jboss.dna.repository.DnaConfiguration;
import org.jboss.dna.repository.DnaConfigurationException;
import org.jboss.dna.repository.DnaLexicon;
import org.jboss.dna.repository.Configurator.ChooseClass;
import org.jboss.dna.repository.Configurator.ConfigRepositoryDetails;
import org.jboss.dna.repository.Configurator.MimeTypeDetectorDetails;
import org.jboss.dna.repository.Configurator.RepositoryDetails;

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
    implements Configurator.Initializer<JcrConfiguration>, /*Configurator.SequencerConfigurator<JcrConfiguration>,*/
    Configurator.RepositoryConfigurator<JcrConfiguration>, Configurator.MimeDetectorConfigurator<JcrConfiguration>,
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
     * @see org.jboss.dna.repository.Configurator.Initializer#withConfigurationRepository()
     */
    public ChooseClass<RepositorySource, ConfigRepositoryDetails<JcrConfiguration>> withConfigurationRepository() {
        return builder.withConfigurationRepository();
    }

    // /**
    // * {@inheritDoc}
    // *
    // * @see org.jboss.dna.repository.Configurator.SequencerConfigurator#addSequencer(java.lang.String)
    // */
    // public ChooseClass<Sequencer, SequencerDetails<JcrConfiguration>> addSequencer( String id ) {
    // CheckArg.isNotEmpty(id, "id");
    // return builder.addSequencer(id);
    // }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.repository.Configurator.RepositoryConfigurator#addRepository(java.lang.String)
     */
    public ChooseClass<RepositorySource, JcrRepositoryDetails<JcrConfiguration>> addRepository( String id ) {
        CheckArg.isNotEmpty(id, "id");
        return builder.addRepository(id);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.repository.Configurator.RepositoryConfigurator#addRepository(org.jboss.dna.graph.connector.RepositorySource)
     */
    public JcrConfiguration addRepository( RepositorySource source ) {
        CheckArg.isNotNull(source, "source");
        CheckArg.isNotEmpty(source.getName(), "source.getName()");
        return builder.addRepository(source);
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

    public interface JcrRepositoryDetails<ReturnType>
        extends RepositoryDetails<ReturnType>, SetOptions<JcrRepositoryDetails<ReturnType>> {

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

    public static class Builder<ReturnType> extends DnaConfiguration.Builder<ReturnType> {

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

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.RepositoryConfigurator#addRepository(java.lang.String)
         */
        @Override
        public ChooseClass<RepositorySource, JcrRepositoryDetails<ReturnType>> addRepository( String id ) {
            CheckArg.isNotEmpty(id, "id");
            // Now create the "dna:source" node with the supplied id ...
            Path path = createOrReplaceNode(sourcesPath(), id);
            JcrRepositoryDetails<ReturnType> details = new JcrGraphRepositoryDetails<ReturnType>(path, builder);
            return new ClassChooser<RepositorySource, JcrRepositoryDetails<ReturnType>>(path, details);
        }

        public class JcrGraphRepositoryDetails<RT> extends GraphRepositoryDetails<RT> implements JcrRepositoryDetails<RT> {

            protected JcrGraphRepositoryDetails( Path path,
                                                 RT returnObject ) {
                super(path, returnObject);
            }

            @SuppressWarnings( "synthetic-access" )
            public OptionSetter<JcrRepositoryDetails<RT>> with( final JcrRepository.Option option ) {
                final Path optionsPath = createOrReplaceNode(path(), DnaLexicon.OPTIONS);

                final JcrRepositoryDetails<RT> details = this;

                return new OptionSetter<JcrRepositoryDetails<RT>>() {
                    public JcrRepositoryDetails<RT> setTo( String value ) {
                        Path optionPath = createOrReplaceNode(optionsPath, option.name());
                        configuration().set(DnaLexicon.VALUE).to(value).on(optionPath);

                        return details;
                    }
                };
            }
        }

    }
}
