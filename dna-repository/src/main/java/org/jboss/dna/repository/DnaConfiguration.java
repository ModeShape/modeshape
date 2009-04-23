/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
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
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.component.ClassLoaderFactory;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.text.Inflector;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.Reflection;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.mimetype.MimeTypeDetector;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathExpression;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.ValueFormatException;
import org.jboss.dna.repository.sequencer.Sequencer;

/**
 */
@Immutable
public class DnaConfiguration {

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

    protected class Source {
        protected RepositorySource source;
        protected String description;
        protected Path path;
    }

    private final ExecutionContext context;
    protected Source configurationSource;
    private Path sourcesPath;
    private Path sequencersPath;
    private Path detectorsPath;
    private Graph graph;
    private Graph.Batch batch;

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
        CheckArg.isNotNull(context, "context");
        this.context = context;

        // Set up the default configuration repository ...
        this.configurationSource = createDefaultConfigurationSource();
    }

    /**
     * Method that is used to set up the default configuration repository source. By default, this method sets up the
     * {@link InMemoryRepositorySource} loaded from the classpath.
     * 
     * @return the default repository source
     */
    protected Source createDefaultConfigurationSource() {
        InMemoryRepositorySource defaultSource = new InMemoryRepositorySource();
        defaultSource.setName("Configuration");
        Source result = new Source();
        result.source = defaultSource;
        result.path = this.context.getValueFactories().getPathFactory().createRootPath();
        result.description = "Configuration Repository";
        return result;
    }

    protected final ExecutionContext context() {
        return this.context;
    }

    protected final PathFactory pathFactory() {
        return context().getValueFactories().getPathFactory();
    }

    /**
     * Get the graph containing the configuration information. This should be called only after the
     * {@link #withConfigurationRepository() configuration repository} is set up.
     * 
     * @return the configuration repository graph; never null
     * @see #graph()
     */
    protected final Graph graph() {
        if (this.graph == null) {
            this.graph = Graph.create(configurationSource.source, context);
        }
        return this.graph;
    }

    /**
     * Get the graph batch that can be used to change the configuration, where the changes are enqueued until {@link #save()
     * saved}. This should be called only after the {@link #withConfigurationRepository() configuration repository} is set up.
     * 
     * @return the latest batch for changes to the configuration repository; never null
     * @see #graph()
     */
    protected final Graph.Batch configuration() {
        if (this.batch == null) {
            this.batch = graph().batch();
        }
        return this.batch;
    }

    /**
     * Save any changes that have been made so far to the configuration. This method does nothing if no changes have been made.
     * 
     * @return this configuration object for method chaining purposes; never null
     */
    public DnaConfiguration save() {
        if (this.batch != null) {
            this.batch.execute();
            this.batch = this.graph.batch();
        }
        return this;
    }

    /**
     * Specify that this configuration should use a particular {@link RepositorySource} for its configuration repository. By
     * default each configuration uses an internal transient repository for its configuration, but using this method will make the
     * configuration use a different repository (that is perhaps shared with other processes).
     * 
     * @return the interface for choosing the class, which returns the interface used to configure the repository source that will
     *         be used for the configuration repository; never null
     */
    public ChooseClass<RepositorySource, ConfigRepositoryDetails> withConfigurationRepository() {
        final Source source = this.configurationSource;
        // The config repository is different, since it has to load immediately ...
        return new ChooseClass<RepositorySource, ConfigRepositoryDetails>() {
            public LoadedFrom<ConfigRepositoryDetails> usingClass( final String className ) {
                return new LoadedFrom<ConfigRepositoryDetails>() {
                    @SuppressWarnings( "unchecked" )
                    public ConfigRepositoryDetails loadedFrom( String... classpath ) {
                        ClassLoader classLoader = context().getClassLoader(classpath);
                        Class<? extends RepositorySource> clazz = null;
                        try {
                            clazz = (Class<? extends RepositorySource>)classLoader.loadClass(className);
                        } catch (ClassNotFoundException err) {
                            throw new DnaConfigurationException(RepositoryI18n.unableToLoadClassUsingClasspath.text(className,
                                                                                                                    classpath));
                        }
                        return usingClass(clazz);
                    }

                    @SuppressWarnings( "unchecked" )
                    public ConfigRepositoryDetails loadedFromClasspath() {
                        Class<? extends RepositorySource> clazz = null;
                        try {
                            clazz = (Class<? extends RepositorySource>)Class.forName(className);
                        } catch (ClassNotFoundException err) {
                            throw new DnaConfigurationException(RepositoryI18n.unableToLoadClass.text(className));
                        }
                        return usingClass(clazz);
                    }
                };
            }

            public ConfigRepositoryDetails usingClass( Class<? extends RepositorySource> repositorySource ) {
                try {
                    source.source = repositorySource.newInstance();
                } catch (InstantiationException err) {
                    I18n msg = RepositoryI18n.errorCreatingInstanceOfClass;
                    throw new DnaConfigurationException(msg.text(repositorySource.getName(), err.getLocalizedMessage()), err);
                } catch (IllegalAccessException err) {
                    I18n msg = RepositoryI18n.errorCreatingInstanceOfClass;
                    throw new DnaConfigurationException(msg.text(repositorySource.getName(), err.getLocalizedMessage()), err);
                }
                return new ConfigurationSourceDetails();
            }
        };
    }

    /**
     * Add a new {@link RepositorySource repository} for this configuration. The new repository will have the supplied name, and
     * if the name of an existing repository is used, this will replace the existing repository configuration.
     * 
     * @param id the id of the new repository that is to be added
     * @return the interface for choosing the class, which returns the interface used to configure the repository source; never
     *         null
     * @throws IllegalArgumentException if the repository name is null, empty, or otherwise invalid
     * @see #addRepository(RepositorySource)
     */
    public ChooseClass<RepositorySource, RepositoryDetails> addRepository( final String id ) {
        CheckArg.isNotEmpty(id, "id");
        // Now create the "dna:source" node with the supplied id ...
        Path sourcePath = pathFactory().create(sourcesPath(), id);
        configuration().create(sourcePath).with(DnaLexicon.READABLE_NAME, id).and();
        return new ClassChooser<RepositorySource, RepositoryDetails>(sourcePath, new GraphRepositoryDetails(sourcePath));
    }

    /**
     * Add a new {@link RepositorySource repository} for this configuration. The new repository will have the supplied name, and
     * if the name of an existing repository is used, this will replace the existing repository configuration.
     * 
     * @param source the {@link RepositorySource} instance that should be used
     * @return this configuration object, for method-chaining purposes
     * @throws IllegalArgumentException if the repository source reference is null
     * @see #addRepository(String)
     */
    public DnaConfiguration addRepository( RepositorySource source ) {
        CheckArg.isNotNull(source, "source");
        CheckArg.isNotEmpty(source.getName(), "source.getName()");
        String name = source.getName();
        RepositoryDetails details = addRepository(source.getName()).usingClass(source.getClass().getName()).loadedFromClasspath();
        // Record all of the bean properties ...
        Path sourcePath = pathFactory().create(sourcesPath(), name);
        Reflection reflector = new Reflection(source.getClass());
        for (String propertyName : reflector.findGetterPropertyNames()) {
            Object value;
            try {
                value = reflector.invokeGetterMethodOnTarget(propertyName, source);
                if (value == null) continue;
                propertyName = Inflector.getInstance().lowerCamelCase(propertyName);
                if (NAMES_TO_MAP.containsKey(propertyName)) {
                    configuration().set(NAMES_TO_MAP.get(propertyName)).to(value).on(sourcePath);
                } else {
                    configuration().set(propertyName).to(value).on(sourcePath);
                }
            } catch (ValueFormatException err) {
                throw err;
            } catch (Throwable err) {
                // Unable to call getter and set property
            }
        }
        return details.and();
    }

    /**
     * Add a new {@link Sequencer sequencer} to this configuration. The new sequencer will have the supplied name, and if the name
     * of an existing sequencer is used, this will replace the existing sequencer configuration.
     * 
     * @param id the identifier of the new sequencer
     * @return the interface for choosing the class, which returns the interface used to configure the sequencer; never null
     * @throws IllegalArgumentException if the sequencer name is null, empty, or otherwise invalid
     */
    public ChooseClass<Sequencer, SequencerDetails> addSequencer( final String id ) {
        CheckArg.isNotEmpty(id, "id");
        // Now create the "dna:sequencer" node with the supplied id ...
        Path sequencerPath = pathFactory().create(sequencersPath(), id);
        configuration().create(sequencerPath).with(DnaLexicon.READABLE_NAME, id).and();
        return new ClassChooser<Sequencer, SequencerDetails>(sequencerPath, new GraphSequencerDetails(sequencerPath));
    }

    /**
     * Add a new {@link MimeTypeDetector MIME type detector} to this configuration. The new detector will have the supplied name,
     * and if the name of an existing detector is used, this will replace the existing detector configuration.
     * 
     * @param id the id of the new detector
     * @return the interface for choosing the class, which returns the interface used to configure the detector; never null
     * @throws IllegalArgumentException if the detector name is null, empty, or otherwise invalid
     */
    public ChooseClass<MimeTypeDetector, MimeTypeDetectorDetails> addMimeTypeDetector( final String id ) {
        CheckArg.isNotEmpty(id, "id");
        // Now create the "dna:sequencer" node with the supplied id ...
        Path detectorPath = pathFactory().create(detectorsPath(), id);
        configuration().create(detectorPath).with(DnaLexicon.READABLE_NAME, id).and();
        return new ClassChooser<MimeTypeDetector, MimeTypeDetectorDetails>(detectorPath,
                                                                           new GraphMimeTypeDetectorDetails(detectorPath));
    }

    /**
     * Complete this configuration and create the corresponding engine.
     * 
     * @return the new engine configured by this instance
     * @throws DnaConfigurationException if the engine cannot be created from this configuration.
     */
    public DnaEngine build() throws DnaConfigurationException {
        save();
        return new DnaEngine(this);
    }

    protected Path sourcesPath() {
        // Make sure the "dna:sources" node is there
        if (sourcesPath == null) {
            Path path = pathFactory().create(this.configurationSource.path, DnaLexicon.SOURCES);
            Node node = graph().createIfMissing(path).andReturn();
            this.sourcesPath = node.getLocation().getPath();
        }
        return this.sourcesPath;
    }

    protected Path sequencersPath() {
        // Make sure the "dna:sequencers" node is there
        if (sequencersPath == null) {
            Path path = pathFactory().create(this.configurationSource.path, DnaLexicon.SEQUENCERS);
            Node node = graph().createIfMissing(path).andReturn();
            this.sequencersPath = node.getLocation().getPath();
        }
        return this.sequencersPath;
    }

    protected Path detectorsPath() {
        // Make sure the "dna:mimeTypeDetectors" node is there
        if (detectorsPath == null) {
            Path path = pathFactory().create(this.configurationSource.path, DnaLexicon.MIME_TYPE_DETECTORS);
            Node node = graph().createIfMissing(path).andReturn();
            this.detectorsPath = node.getLocation().getPath();
        }
        return this.detectorsPath;
    }

    /**
     * Interface used to configure a {@link RepositorySource repository}.
     */
    public interface RepositoryDetails
        extends SetName<RepositoryDetails>, SetDescription<RepositoryDetails>, SetProperties<RepositoryDetails>,
        ConfigurationBuilder {
    }

    public interface ConfigRepositoryDetails
        extends SetDescription<ConfigRepositoryDetails>, SetProperties<ConfigRepositoryDetails>, ConfigurationBuilder {
        /**
         * Specify the path under which the configuration content is to be found. This path is assumed to be "/" by default.
         * 
         * @param path the path to the configuration content in the configuration source; may not be null
         * @return this instance for method chaining purposes; never null
         */
        public ConfigRepositoryDetails under( String path );
    }

    /**
     * Interface used to configure a {@link Sequencer sequencer}.
     */
    public interface SequencerDetails extends SetName<SequencerDetails>, SetDescription<SequencerDetails>, ConfigurationBuilder {

        /**
         * Specify the input {@link PathExpression path expression} represented as a string, which determines when this sequencer
         * will be executed.
         * 
         * @param inputPathExpression the path expression for nodes that, when they change, will be passed as an input to the
         *        sequencer
         * @return the interface used to specify the output path expression; never null
         */
        PathExpressionOutput sequencingFrom( String inputPathExpression );

        /**
         * Specify the input {@link PathExpression path expression}, which determines when this sequencer will be executed.
         * 
         * @param inputPathExpression the path expression for nodes that, when they change, will be passed as an input to the
         *        sequencer
         * @return the interface used to continue specifying the configuration of the sequencer
         */
        SequencerDetails sequencingFrom( PathExpression inputPathExpression );
    }

    /**
     * Interface used to specify the output path expression for a {@link SequencerDetails#sequencingFrom(PathExpression) sequencer
     * configuration}.
     */
    public interface PathExpressionOutput {
        /**
         * Specify the output {@link PathExpression path expression}, which determines where this sequencer's output will be
         * placed.
         * 
         * @param outputExpression the path expression for the location(s) where output generated by the sequencer is to be placed
         * @return the interface used to continue specifying the configuration of the sequencer
         */
        SequencerDetails andOutputtingTo( String outputExpression );
    }

    /**
     * Interface used to configure a {@link MimeTypeDetector MIME type detector}.
     */
    public interface MimeTypeDetectorDetails
        extends SetName<MimeTypeDetectorDetails>, SetDescription<MimeTypeDetectorDetails>,
        SetProperties<MimeTypeDetectorDetails>, ConfigurationBuilder {
    }

    /**
     * Interface for configuring the JavaBean-style properties of an object.
     * 
     * @param <ReturnType> the interface returned after the property has been set.
     * @author Randall Hauch
     */
    public interface SetProperties<ReturnType> {
        /**
         * Specify the name of the JavaBean-style property that is to be set. The value may be set using the interface returned by
         * this method.
         * 
         * @param beanPropertyName the name of the JavaBean-style property (e.g., "retryLimit")
         * @return the interface used to set the value for the property; never null
         */
        PropertySetter<ReturnType> with( String beanPropertyName );
    }

    /**
     * The interface used to set the value for a JavaBean-style property.
     * 
     * @param <ReturnType> the interface returned from these methods
     * @author Randall Hauch
     * @see SetProperties#with(String)
     */
    public interface PropertySetter<ReturnType> {
        /**
         * Set the property value to an integer.
         * 
         * @param value the new value for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setTo( int value );

        /**
         * Set the property value to a long number.
         * 
         * @param value the new value for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setTo( long value );

        /**
         * Set the property value to a short.
         * 
         * @param value the new value for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setTo( short value );

        /**
         * Set the property value to a boolean.
         * 
         * @param value the new value for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setTo( boolean value );

        /**
         * Set the property value to a float.
         * 
         * @param value the new value for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setTo( float value );

        /**
         * Set the property value to a double.
         * 
         * @param value the new value for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setTo( double value );

        /**
         * Set the property value to a string.
         * 
         * @param value the new value for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setTo( String value );

        /**
         * Set the property value to an object.
         * 
         * @param value the new value for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setTo( Object value );
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
        ReturnType describedAs( String description );
    }

    /**
     * The interface used to set a human readable name on a component.
     * 
     * @param <ReturnType> the interface returned from these methods
     */
    public interface SetName<ReturnType> {
        /**
         * Specify the human-readable name for this component.
         * 
         * @param description the description; may be null or empty
         * @return the next component to continue configuration; never null
         */
        ReturnType named( String description );
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
     * Interface for classes that can return a reference to their DNA configuration.
     */
    public interface ConfigurationBuilder {

        /**
         * Return a reference to the enclosing configuration so that it can be built or further configured.
         * 
         * @return a reference to the enclosing configuration
         */
        DnaConfiguration and();
    }

    protected class ConfigurationSourceDetails implements ConfigRepositoryDetails {
        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.SetDescription#describedAs(java.lang.String)
         */
        public ConfigRepositoryDetails describedAs( String description ) {
            DnaConfiguration.this.configurationSource.description = description;
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.SetProperties#with(java.lang.String)
         */
        public PropertySetter<ConfigRepositoryDetails> with( String propertyName ) {
            return new BeanPropertySetter<ConfigRepositoryDetails>(DnaConfiguration.this.configurationSource.source,
                                                                   propertyName, this);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.ConfigRepositoryDetails#under(java.lang.String)
         */
        public ConfigRepositoryDetails under( String path ) {
            CheckArg.isNotNull(path, "path");
            DnaConfiguration.this.configurationSource.path = context().getValueFactories().getPathFactory().create(path);
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.ConfigurationBuilder#and()
         */
        public DnaConfiguration and() {
            return DnaConfiguration.this;
        }
    }

    protected class GraphRepositoryDetails implements RepositoryDetails {
        private final Path path;

        protected GraphRepositoryDetails( Path path ) {
            assert path != null;
            this.path = path;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.SetName#named(java.lang.String)
         */
        public RepositoryDetails named( String name ) {
            configuration().set(DnaLexicon.READABLE_NAME).to(name).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.SetDescription#describedAs(java.lang.String)
         */
        public RepositoryDetails describedAs( String description ) {
            configuration().set(DnaLexicon.DESCRIPTION).to(description).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.SetProperties#with(java.lang.String)
         */
        public PropertySetter<RepositoryDetails> with( String propertyName ) {
            return new GraphPropertySetter<RepositoryDetails>(path, propertyName, this);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.ConfigurationBuilder#and()
         */
        public DnaConfiguration and() {
            return DnaConfiguration.this;
        }
    }

    protected class GraphSequencerDetails implements SequencerDetails {
        private final Path path;

        protected GraphSequencerDetails( Path path ) {
            assert path != null;
            this.path = path;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.SequencerDetails#sequencingFrom(java.lang.String)
         */
        public PathExpressionOutput sequencingFrom( final String from ) {
            CheckArg.isNotEmpty(from, "from");
            return new PathExpressionOutput() {
                /**
                 * {@inheritDoc}
                 * 
                 * @see org.jboss.dna.repository.DnaConfiguration.PathExpressionOutput#andOutputtingTo(java.lang.String)
                 */
                public SequencerDetails andOutputtingTo( String into ) {
                    CheckArg.isNotEmpty(into, "into");
                    return sequencingFrom(PathExpression.compile(from + " => " + into));
                }
            };
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.SetName#named(java.lang.String)
         */
        public SequencerDetails named( String name ) {
            configuration().set(DnaLexicon.READABLE_NAME).to(name).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.SequencerDetails#sequencingFrom(org.jboss.dna.graph.property.PathExpression)
         */
        public SequencerDetails sequencingFrom( PathExpression expression ) {
            CheckArg.isNotNull(expression, "expression");
            configuration().set(DnaLexicon.PATH_EXPRESSIONS).on(path).to(expression.getExpression());
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.SetDescription#describedAs(java.lang.String)
         */
        public SequencerDetails describedAs( String description ) {
            configuration().set(DnaLexicon.DESCRIPTION).to(description).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.ConfigurationBuilder#and()
         */
        public DnaConfiguration and() {
            return DnaConfiguration.this;
        }
    }

    protected class GraphMimeTypeDetectorDetails implements MimeTypeDetectorDetails {
        private final Path path;

        protected GraphMimeTypeDetectorDetails( Path path ) {
            assert path != null;
            this.path = path;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.SetName#named(java.lang.String)
         */
        public MimeTypeDetectorDetails named( String name ) {
            configuration().set(DnaLexicon.READABLE_NAME).to(name).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.SetProperties#with(java.lang.String)
         */
        public PropertySetter<MimeTypeDetectorDetails> with( String propertyName ) {
            return new GraphPropertySetter<MimeTypeDetectorDetails>(path, propertyName, this);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.SetDescription#describedAs(java.lang.String)
         */
        public MimeTypeDetectorDetails describedAs( String description ) {
            configuration().set(DnaLexicon.DESCRIPTION).to(description).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.ConfigurationBuilder#and()
         */
        public DnaConfiguration and() {
            return DnaConfiguration.this;
        }
    }

    /**
     * Abstract implementation of {@link ChooseClass} that has a single abstract method to obtain the interface that should be
     * returned when the class name and classpath have been selected.
     * 
     * @param <ComponentClass> the type of the component that is being chosen
     * @param <ReturnType> the interface that should be returned when the class name and classpath have been chosen.
     * @author Randall Hauch
     */
    protected class ClassChooser<ComponentClass, ReturnType> implements ChooseClass<ComponentClass, ReturnType> {
        protected final Path pathOfComponentNode;
        protected final ReturnType returnObject;

        protected ClassChooser( Path pathOfComponentNode,
                                ReturnType returnObject ) {
            assert pathOfComponentNode != null;
            assert returnObject != null;
            this.pathOfComponentNode = pathOfComponentNode;
            this.returnObject = returnObject;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.ChooseClass#usingClass(java.lang.String)
         */
        public LoadedFrom<ReturnType> usingClass( final String classname ) {
            CheckArg.isNotEmpty(classname, "classname");
            configuration().set(DnaLexicon.CLASSNAME).to(classname).on(pathOfComponentNode);
            return new LoadedFrom<ReturnType>() {
                public ReturnType loadedFromClasspath() {
                    return returnObject;
                }

                public ReturnType loadedFrom( String... classpath ) {
                    CheckArg.isNotEmpty(classpath, "classpath");
                    if (classpath.length == 1 && classpath[0] != null) {
                        configuration().set(DnaLexicon.CLASSPATH).to(classpath[0]).on(pathOfComponentNode);
                    } else {
                        Object[] remaining = new String[classpath.length - 1];
                        System.arraycopy(classpath, 1, remaining, 0, remaining.length);
                        configuration().set(DnaLexicon.CLASSPATH).to(classpath[0], remaining).on(pathOfComponentNode);
                    }
                    return returnObject;
                }
            };
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.ChooseClass#usingClass(java.lang.Class)
         */
        public ReturnType usingClass( Class<? extends ComponentClass> clazz ) {
            CheckArg.isNotNull(clazz, "clazz");
            return usingClass(clazz.getName()).loadedFromClasspath();
        }
    }

    /**
     * Reusable implementation of {@link PropertySetter} that sets the JavaBean-style property using reflection.
     * 
     * @param <ReturnType>
     * @author Randall Hauch
     */
    protected class BeanPropertySetter<ReturnType> implements PropertySetter<ReturnType> {
        private final Object javaBean;
        private final String beanPropertyName;
        private final ReturnType returnObject;

        protected BeanPropertySetter( Object javaBean,
                                      String beanPropertyName,
                                      ReturnType returnObject ) {
            assert javaBean != null;
            assert beanPropertyName != null;
            assert returnObject != null;
            this.javaBean = javaBean;
            this.beanPropertyName = beanPropertyName;
            this.returnObject = returnObject;
        }

        public ReturnType setTo( boolean value ) {
            return setTo((Object)value);
        }

        public ReturnType setTo( int value ) {
            return setTo((Object)value);
        }

        public ReturnType setTo( long value ) {
            return setTo((Object)value);
        }

        public ReturnType setTo( short value ) {
            return setTo((Object)value);
        }

        public ReturnType setTo( float value ) {
            return setTo((Object)value);
        }

        public ReturnType setTo( double value ) {
            return setTo((Object)value);
        }

        public ReturnType setTo( String value ) {
            return setTo((Object)value);
        }

        public ReturnType setTo( Object value ) {
            // Set the JavaBean-style property on the RepositorySource instance ...
            Reflection reflection = new Reflection(javaBean.getClass());
            try {
                reflection.invokeSetterMethodOnTarget(beanPropertyName, javaBean, value);
            } catch (Throwable err) {
                I18n msg = RepositoryI18n.errorSettingJavaBeanPropertyOnInstanceOfClass;
                throw new DnaConfigurationException(msg.text(beanPropertyName, javaBean.getClass(), err.getMessage()), err);
            }
            return returnObject;
        }
    }

    /**
     * Reusable implementation of {@link PropertySetter} that sets the property on the specified node in the configuration graph.
     * 
     * @param <ReturnType>
     * @author Randall Hauch
     */
    protected class GraphPropertySetter<ReturnType> implements PropertySetter<ReturnType> {
        private final Path path;
        private final String beanPropertyName;
        private final ReturnType returnObject;

        protected GraphPropertySetter( Path path,
                                       String beanPropertyName,
                                       ReturnType returnObject ) {
            assert path != null;
            assert beanPropertyName != null;
            assert returnObject != null;
            this.path = path;
            this.beanPropertyName = Inflector.getInstance().lowerCamelCase(beanPropertyName);
            this.returnObject = returnObject;
        }

        public ReturnType setTo( boolean value ) {
            if (NAMES_TO_MAP.containsKey(beanPropertyName)) {
                configuration().set(NAMES_TO_MAP.get(beanPropertyName)).to(value).on(path);
            } else {
                configuration().set(beanPropertyName).to(value).on(path);
            }
            return returnObject;
        }

        public ReturnType setTo( int value ) {
            if (NAMES_TO_MAP.containsKey(beanPropertyName)) {
                configuration().set(NAMES_TO_MAP.get(beanPropertyName)).to(value).on(path);
            } else {
                configuration().set(beanPropertyName).to(value).on(path);
            }
            return returnObject;
        }

        public ReturnType setTo( long value ) {
            if (NAMES_TO_MAP.containsKey(beanPropertyName)) {
                configuration().set(NAMES_TO_MAP.get(beanPropertyName)).to(value).on(path);
            } else {
                configuration().set(beanPropertyName).to(value).on(path);
            }
            return returnObject;
        }

        public ReturnType setTo( short value ) {
            if (NAMES_TO_MAP.containsKey(beanPropertyName)) {
                configuration().set(NAMES_TO_MAP.get(beanPropertyName)).to(value).on(path);
            } else {
                configuration().set(beanPropertyName).to(value).on(path);
            }
            return returnObject;
        }

        public ReturnType setTo( float value ) {
            if (NAMES_TO_MAP.containsKey(beanPropertyName)) {
                configuration().set(NAMES_TO_MAP.get(beanPropertyName)).to(value).on(path);
            } else {
                configuration().set(beanPropertyName).to(value).on(path);
            }
            return returnObject;
        }

        public ReturnType setTo( double value ) {
            if (NAMES_TO_MAP.containsKey(beanPropertyName)) {
                configuration().set(NAMES_TO_MAP.get(beanPropertyName)).to(value).on(path);
            } else {
                configuration().set(beanPropertyName).to(value).on(path);
            }
            return returnObject;
        }

        public ReturnType setTo( String value ) {
            if (NAMES_TO_MAP.containsKey(beanPropertyName)) {
                configuration().set(NAMES_TO_MAP.get(beanPropertyName)).to(value).on(path);
            } else {
                configuration().set(beanPropertyName).to(value).on(path);
            }
            return returnObject;
        }

        public ReturnType setTo( Object value ) {
            if (NAMES_TO_MAP.containsKey(beanPropertyName)) {
                configuration().set(NAMES_TO_MAP.get(beanPropertyName)).to(value).on(path);
            } else {
                configuration().set(beanPropertyName).to(value).on(path);
            }
            return returnObject;
        }
    }
}
