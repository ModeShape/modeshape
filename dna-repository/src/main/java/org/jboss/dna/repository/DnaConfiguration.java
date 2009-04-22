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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.component.ClassLoaderFactory;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.Reflection;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.mimetype.MimeTypeDetector;
import org.jboss.dna.graph.property.PathExpression;
import org.jboss.dna.graph.sequencer.StreamSequencer;
import org.jboss.dna.repository.mimetype.MimeTypeDetectorConfig;
import org.jboss.dna.repository.sequencer.Sequencer;
import org.jboss.dna.repository.sequencer.SequencerConfig;
import org.jboss.dna.repository.sequencer.StreamSequencerAdapter;

/**
 */
@Immutable
public class DnaConfiguration {

    protected RepositorySource configurationSource;
    protected String configurationSourceDescription;
    protected Graph configuration;
    private final ExecutionContext context;

    /**
     * Mapping of repository names to configured repositories
     */
    protected final Map<String, DnaRepositoryDetails> repositories;
    protected final Map<String, DnaMimeTypeDetectorDetails> mimeTypeDetectors;
    protected final Map<String, DnaSequencerDetails> sequencers;

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
     */
    public DnaConfiguration( ExecutionContext context ) {
        this.context = context;
        this.repositories = new HashMap<String, DnaRepositoryDetails>();
        this.mimeTypeDetectors = new HashMap<String, DnaMimeTypeDetectorDetails>();
        this.sequencers = new HashMap<String, DnaSequencerDetails>();

        // Set up the default configuration repository ...
        this.configurationSource = createDefaultConfigurationSource();

    }

    private DnaConfiguration( DnaConfiguration source ) {
        this.configuration = source.configuration;
        this.configurationSource = source.configurationSource;
        this.context = source.context;
        this.configurationSourceDescription = source.configurationSourceDescription;
        this.repositories = new HashMap<String, DnaRepositoryDetails>(source.repositories);
        this.mimeTypeDetectors = new HashMap<String, DnaMimeTypeDetectorDetails>(source.mimeTypeDetectors);
        this.sequencers = new HashMap<String, DnaSequencerDetails>(source.sequencers);
    }

    private DnaConfiguration with( String repositoryName,
                                   DnaRepositoryDetails details ) {
        DnaConfiguration newConfig = new DnaConfiguration(this);
        newConfig.repositories.put(repositoryName, details);

        return newConfig;
    }

    protected ExecutionContext getExecutionContext() {
        return this.context;
    }

    protected Graph getConfiguration() {
        if (this.configuration == null) {
            this.configuration = Graph.create(configurationSource, context);
        }
        return this.configuration;
    }

    /**
     * Method that is used to set up the default configuration repository source. By default, this method sets up the
     * {@link InMemoryRepositorySource} loaded from the classpath.
     * 
     * @return the default repository source
     */
    protected RepositorySource createDefaultConfigurationSource() {
        this.withConfigurationRepository()
            .usingClass(InMemoryRepositorySource.class.getName())
            .loadedFromClasspath()
            .describedAs("Configuration Repository")
            .with("name")
            .setTo("Configuration");
        return configurationSource;
    }

    /**
     * Specify that this configuration should use a particular {@link RepositorySource} for its configuration repository. By
     * default each configuration uses an internal transient repository for its configuration, but using this method will make the
     * configuration use a different repository (that is perhaps shared with other processes).
     * 
     * @return the interface for choosing the class, which returns the interface used to configure the repository source that will
     *         be used for the configuration repository; never null
     */
    public ChooseClass<RepositorySource, RepositoryDetails> withConfigurationRepository() {
        return addRepository(DnaEngine.CONFIGURATION_REPOSITORY_NAME);
    }

    /**
     * Add a new {@link RepositorySource repository} for this configuration. The new repository will have the supplied name, and
     * if the name of an existing repository is used, this will replace the existing repository configuration.
     * 
     * @param name the name of the new repository that is to be added
     * @return the interface for choosing the class, which returns the interface used to configure the repository source; never
     *         null
     * @throws IllegalArgumentException if the repository name is null, empty, or otherwise invalid
     * @see #addRepository(RepositorySource)
     */
    public ChooseClass<RepositorySource, RepositoryDetails> addRepository( final String name ) {
        return new ClassChooser<RepositorySource, RepositoryDetails>() {

            @Override
            protected RepositoryDetails getComponentBuilder( String className,
                                                             String... classpath ) {
                try {
                    Class<?> clazz = Class.forName(className);
                    Object newInstance = clazz.newInstance();
                    assert newInstance instanceof RepositorySource;

                    DnaRepositoryDetails details = new DnaRepositoryDetails((RepositorySource)newInstance);
                    DnaConfiguration.this.repositories.put(name, details);

                    return details;
                } catch (Exception ex) {
                    throw new DnaConfigurationException(ex);
                }
            }
        };
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
        return this.with(source.getName(), new DnaRepositoryDetails(source));
    }

    /**
     * Add a new {@link Sequencer sequencer} to this configuration. The new sequencer will have the supplied name, and if the name
     * of an existing sequencer is used, this will replace the existing sequencer configuration.
     * 
     * @param name the name of the new sequencer
     * @return the interface for choosing the class, which returns the interface used to configure the sequencer; never null
     * @throws IllegalArgumentException if the sequencer name is null, empty, or otherwise invalid
     */
    public ChooseClass<Sequencer, SequencerDetails> addSequencer( final String name ) {
        return new ClassChooser<Sequencer, SequencerDetails>() {

            @Override
            protected SequencerDetails getComponentBuilder( String className,
                                                            String... classpath ) {
                try {
                    Class<?> clazz = Class.forName(className);
                    Object newInstance = clazz.newInstance();
                    assert newInstance instanceof Sequencer;

                    DnaSequencerDetails details = new DnaSequencerDetails(name, (Sequencer)newInstance, classpath);
                    DnaConfiguration.this.sequencers.put(name, details);

                    return details;
                } catch (Exception ex) {
                    throw new DnaConfigurationException(ex);
                }
            }
        };
    }

    /**
     * Add a new {@link StreamSequencer sequencer} to this configuration. The new stream sequencer will have the supplied name,
     * and if the name of an existing sequencer is used, this will replace the existing sequencer configuration.
     * 
     * @param name the name of the new sequencer
     * @return the interface for choosing the class, which returns the interface used to configure the stream sequencer; never
     *         null
     * @throws IllegalArgumentException if the sequencer name is null, empty, or otherwise invalid
     */
    public ChooseClass<StreamSequencer, SequencerDetails> addStreamSequencer( final String name ) {
        return new ClassChooser<StreamSequencer, SequencerDetails>() {

            @Override
            protected SequencerDetails getComponentBuilder( String className,
                                                            String... classpath ) {
                try {
                    Class<?> clazz = Class.forName(className);
                    Object newInstance = clazz.newInstance();
                    assert newInstance instanceof StreamSequencer;

                    DnaSequencerDetails details = new DnaSequencerDetails(
                                                                          name,
                                                                          new StreamSequencerAdapter((StreamSequencer)newInstance),
                                                                          classpath);
                    DnaConfiguration.this.sequencers.put(name, details);

                    return details;
                } catch (Exception ex) {
                    throw new DnaConfigurationException(ex);
                }
            }
        };
    }

    /**
     * Add a new {@link MimeTypeDetector MIME type detector} to this configuration. The new detector will have the supplied name,
     * and if the name of an existing detector is used, this will replace the existing detector configuration.
     * 
     * @param name the name of the new detector
     * @return the interface for choosing the class, which returns the interface used to configure the detector; never null
     * @throws IllegalArgumentException if the detector name is null, empty, or otherwise invalid
     */
    public ChooseClass<MimeTypeDetector, MimeTypeDetectorDetails> addMimeTypeDetector( final String name ) {
        return new ClassChooser<MimeTypeDetector, MimeTypeDetectorDetails>() {

            @Override
            protected MimeTypeDetectorDetails getComponentBuilder( String className,
                                                                   String... classpath ) {
                try {
                    Class<?> clazz = Class.forName(className);
                    Object newInstance = clazz.newInstance();
                    assert newInstance instanceof MimeTypeDetector;

                    DnaMimeTypeDetectorDetails details = new DnaMimeTypeDetectorDetails(name, (MimeTypeDetector)newInstance,
                                                                                        classpath);
                    DnaConfiguration.this.mimeTypeDetectors.put(name, details);

                    return details;
                } catch (Exception ex) {
                    throw new DnaConfigurationException(ex);
                }
            }
        };
    }

    /**
     * Complete this configuration and create the corresponding engine.
     * 
     * @return the new engine configured by this instance
     */
    public DnaEngine build() {
        return new DnaEngine(this);
    }

    /**
     * Interface used to configure a {@link RepositorySource repository}.
     */
    public interface RepositoryDetails
        extends SetDescription<RepositoryDetails>, SetProperties<RepositoryDetails>, ConfigurationBuilder {

        RepositorySource getRepositorySource();
    }

    /**
     * Local implementation of the {@link MimeTypeDetectorDetails} interface that tracks all of the user-provided configuration
     * details.
     */
    protected class DnaMimeTypeDetectorDetails implements MimeTypeDetectorDetails {
        final MimeTypeDetector mimeTypeDetector;
        private String name;
        private String description;
        private Map<String, Object> properties;
        private String className;
        private String[] classpath;

        protected DnaMimeTypeDetectorDetails( String name,
                                              MimeTypeDetector mimeTypeDetector,
                                              String[] classpath ) {
            this.mimeTypeDetector = mimeTypeDetector;
            this.name = name;
            this.description = mimeTypeDetector.getClass().getName();
            this.properties = new HashMap<String, Object>();
            this.className = mimeTypeDetector.getClass().getName();
            this.classpath = classpath;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        protected String getDescription() {
            return description;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.SetDescription#describedAs(java.lang.String)
         */
        public MimeTypeDetectorDetails describedAs( String description ) {
            this.description = description;
            return this;
        }

        public PropertySetter<MimeTypeDetectorDetails> with( final String propertyName ) {
            return new MappingPropertySetter<MimeTypeDetectorDetails>(propertyName, this);
        }

        public MimeTypeDetector getMimeTypeDetector() {
            return mimeTypeDetector;
        }

        MimeTypeDetectorConfig getMimeTypeDetectorConfig() {
            return new MimeTypeDetectorConfig(name, description, properties, className, classpath);
        }

        public DnaConfiguration and() {
            return DnaConfiguration.this;
        }

    }

    /**
     * Local implementation of the {@link RepositoryDetails} interface that tracks all of the user-provided configuration details.
     */
    protected class DnaRepositoryDetails implements RepositoryDetails {
        private final RepositorySource source;
        private final String description;
        private Map<String, Object> properties;

        protected DnaRepositoryDetails( RepositorySource source ) {
            this.source = source;
            this.description = source.getName();
            this.properties = new HashMap<String, Object>();
        }

        protected DnaRepositoryDetails( RepositorySource source,
                                        String description ) {
            this.source = source;
            this.description = description;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        protected String getDescription() {
            return description;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.SetDescription#describedAs(java.lang.String)
         */
        public RepositoryDetails describedAs( String description ) {
            return new DnaRepositoryDetails(this.source, description);
        }

        public PropertySetter<RepositoryDetails> with( final String propertyName ) {
            return new BeanPropertySetter<RepositoryDetails>(source, propertyName, this);
        }

        public RepositorySource getRepositorySource() {
            return source;
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
     * Local implementation of the {@link SequencerDetails} interface that tracks all of the user-provided configuration details.
     */
    protected class DnaSequencerDetails implements SequencerDetails {
        private final Sequencer sequencer;
        private String name;
        private String description;
        private Map<String, Object> properties;
        private String className;
        private String[] classpath;
        final List<PathExpression> sourcePathExpressions;
        final List<PathExpression> targetPathExpressions;

        protected DnaSequencerDetails( String name,
                                       Sequencer sequencer,
                                       String[] classpath ) {
            this.sequencer = sequencer;
            this.name = name;
            this.description = sequencer.getClass().getName();
            this.properties = new HashMap<String, Object>();
            this.className = sequencer.getClass().getName();
            this.classpath = classpath;
            this.sourcePathExpressions = new ArrayList<PathExpression>();
            this.targetPathExpressions = new ArrayList<PathExpression>();
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        protected String getDescription() {
            return description;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.SetDescription#describedAs(java.lang.String)
         */
        public SequencerDetails describedAs( String description ) {
            this.description = description;
            return this;
        }

        public PropertySetter<SequencerDetails> with( final String propertyName ) {
            return new MappingPropertySetter<SequencerDetails>(propertyName, this);
        }

        public Sequencer getSequencer() {
            return sequencer;
        }

        SequencerConfig getSequencerConfig() {
            return new SequencerConfig(this.name, this.description, this.properties, this.className, this.classpath);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.ConfigurationBuilder#and()
         */
        public DnaConfiguration and() {
            return DnaConfiguration.this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.SequencerDetails#sequencingFrom(java.lang.String)
         */
        public PathExpressionOutput sequencingFrom( String inputExpressionPath ) {
            return this.sequencingFrom(PathExpression.compile(inputExpressionPath));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.SequencerDetails#sequencingFrom(org.jboss.dna.graph.property.PathExpression)
         */
        public PathExpressionOutput sequencingFrom( final PathExpression inputExpressionPath ) {
            final DnaSequencerDetails details = this;
            return new PathExpressionOutput() {

                /**
                 * {@inheritDoc}
                 * 
                 * @see org.jboss.dna.repository.DnaConfiguration.PathExpressionOutput#andOutputtingTo(java.lang.String)
                 */
                public DnaSequencerDetails andOutputtingTo( final String outputExpressionPath ) {
                    details.sourcePathExpressions.add(inputExpressionPath);
                    details.targetPathExpressions.add(PathExpression.compile(outputExpressionPath));
                    return details;
                }
            };
        }

    }

    /**
     * Interface used to configure a {@link Sequencer sequencer}.
     */
    public interface SequencerDetails extends SetDescription<SequencerDetails>, ConfigurationBuilder {

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
         * @return the interface used to specify the output path expression; never null
         */
        PathExpressionOutput sequencingFrom( PathExpression inputPathExpression );
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
        extends SetDescription<MimeTypeDetectorDetails>, SetProperties<MimeTypeDetectorDetails>, ConfigurationBuilder {
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

        Map<String, Object> getProperties();

        /**
         * Return a reference to the enclosing configuration so that it can be built or further configured.
         * 
         * @return a reference to the enclosing configuration
         */
        DnaConfiguration and();
    }

    /**
     * Abstract implementation of {@link ChooseClass} that has a single abstract method to obtain the interface that should be
     * returned when the class name and classpath have been selected.
     * 
     * @param <ComponentClass> the type of the component that is being chosen
     * @param <ReturnType> the interface that should be returned when the class name and classpath have been chosen.
     * @author Randall Hauch
     */
    protected abstract class ClassChooser<ComponentClass, ReturnType> implements ChooseClass<ComponentClass, ReturnType> {

        protected abstract ReturnType getComponentBuilder( String className,
                                                           String... classpath );

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.DnaConfiguration.ChooseClass#usingClass(java.lang.String)
         */
        public LoadedFrom<ReturnType> usingClass( final String classname ) {
            CheckArg.isNotEmpty(classname, "classname");
            return new LoadedFrom<ReturnType>() {
                public ReturnType loadedFromClasspath() {
                    return getComponentBuilder(classname);
                }

                public ReturnType loadedFrom( String... classpath ) {
                    return getComponentBuilder(classname, classpath);
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
            return getComponentBuilder(clazz.getName());
        }
    }

    /**
     * Utility method to instantiate a class.
     * 
     * @param <T> the interface or superclass that the instantiated object is to implement
     * @param interfaceType the interface or superclass type that the instantiated object is to implement
     * @param className the name of the class
     * @param classloaderNames the names of the class loaders
     * @return the new instance
     */
    @SuppressWarnings( "unchecked" )
    protected <T> T instantiate( Class<T> interfaceType,
                                 String className,
                                 String... classloaderNames ) {
        // Load the class and create the instance ...
        try {
            Class<?> clazz = getExecutionContext().getClassLoader(classloaderNames).loadClass(className);
            return (T)clazz.newInstance();
        } catch (Throwable err) {
            I18n msg = RepositoryI18n.errorCreatingInstanceOfClass;
            throw new DnaConfigurationException(msg.text(className, err.getMessage()), err);
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
     * Reusable implementation of {@link PropertySetter} that aggregates the properties to set to be placed into a map
     * 
     * @param <ReturnType>
     * @author Randall Hauch
     */
    protected class MappingPropertySetter<ReturnType extends ConfigurationBuilder> implements PropertySetter<ReturnType> {
        private final String beanPropertyName;
        private final ReturnType returnObject;

        protected MappingPropertySetter( String beanPropertyName,
                                         ReturnType returnObject ) {
            assert beanPropertyName != null;
            assert returnObject != null;
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
            returnObject.getProperties().put(beanPropertyName, value);
            return returnObject;
        }
    }
}
