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
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.mimetype.MimeTypeDetector;
import org.jboss.dna.repository.Configurator;
import org.jboss.dna.repository.DnaConfiguration;
import org.jboss.dna.repository.DnaConfigurationException;
import org.jboss.dna.repository.Configurator.ChooseClass;
import org.jboss.dna.repository.Configurator.ConfigRepositoryDetails;
import org.jboss.dna.repository.Configurator.MimeTypeDetectorDetails;
import org.jboss.dna.repository.Configurator.RepositoryDetails;

/**
 * A configuration builder for a {@link JcrEngine}.  This class is an internal domain-specific language (DSL),
 * and is designed to be used in a traditional way or in a method-chained manner:
 * <pre>
 * configuration.addRepository("Source1").usingClass(InMemoryRepositorySource.class).describedAs("description");
 * configuration.addMimeTypeDetector("detector")
 *              .usingClass(ExtensionBasedMimeTypeDetector.class)
 *              .describedAs("default detector");
 * configuration.addSequencer("MicrosoftDocs")
 *              .usingClass("org.jboss.dna.sequencer.msoffice.MSOfficeMetadataSequencer")
 *              .loadedFromClasspath()
 *              .named("Microsoft Document sequencer")
 *              .describedAs("Our primary sequencer for all .doc files")
 *              .sequencingFrom("/public//(*.(doc|xml|ppt)[*]/jcr:content[@jcr:data]")
 *              .andOutputtingTo("/documents/$1");
 * configuration.save();
 * </pre>
 */
public class JcrConfiguration
    implements Configurator.Initializer<JcrConfiguration>, /*Configurator.SequencerConfigurator<JcrConfiguration>,*/
    Configurator.RepositoryConfigurator<JcrConfiguration>, Configurator.MimeDetectorConfigurator<JcrConfiguration>,
    Configurator.Builder<JcrEngine> {

    private final DnaConfiguration.Builder<JcrConfiguration> builder;

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
        this.builder = new DnaConfiguration.Builder<JcrConfiguration>(context, this);
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

//    /**
//     * {@inheritDoc}
//     * 
//     * @see org.jboss.dna.repository.Configurator.SequencerConfigurator#addSequencer(java.lang.String)
//     */
//    public ChooseClass<Sequencer, SequencerDetails<JcrConfiguration>> addSequencer( String id ) {
//        CheckArg.isNotEmpty(id, "id");
//        return builder.addSequencer(id);
//    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.repository.Configurator.RepositoryConfigurator#addRepository(java.lang.String)
     */
    public ChooseClass<RepositorySource, RepositoryDetails<JcrConfiguration>> addRepository( String id ) {
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

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.repository.Configurator.Builder#build()
     */
    public JcrEngine build() throws DnaConfigurationException {
        save();
        return new JcrEngine(builder.buildDnaEngine());
    }
}
