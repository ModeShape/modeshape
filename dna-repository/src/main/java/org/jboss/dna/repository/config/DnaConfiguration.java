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
package org.jboss.dna.repository.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.Workspace;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.basic.RootPath;
import org.xml.sax.SAXException;

/**
 * 
 */
public class DnaConfiguration {

    protected static final String DEFAULT_WORKSPACE_NAME = "";
    protected static final String DEFAULT_PATH = "/";

    private final ExecutionContext context;
    private ConfigurationContent configurationContent;

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
        return loadFrom(new File(pathToConfigurationFile));
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
        InputStream stream = new FileInputStream(configurationFile);
        try {
            return loadFrom(stream);
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
        InputStream stream = urlToConfigurationFile.openStream();
        try {
            return loadFrom(stream);
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

        // Create the in-memory repository source in which the content will be stored ...
        InMemoryRepositorySource source = new InMemoryRepositorySource();
        source.setName("Configuration Repository");
        source.setDefaultWorkspaceName(DEFAULT_WORKSPACE_NAME);

        // Import the information into the source ...
        Path path = path(DEFAULT_PATH);
        Graph graph = Graph.create(source, context);
        graph.importXmlFrom(configurationFileInputStream).into(path);

        // The file was imported successfully, so now create the content information ...
        configurationContent = new ConfigurationContent(source, null, path, context);
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
            Workspace workspace = graph.useWorkspace(workspaceName); // should throw exception if not connectable
            assert workspace.getRoot() != null;
        }

        // Verify the path ...
        Path path = pathInWorkspace != null ? path(pathInWorkspace) : path(DEFAULT_PATH);
        Node parent = graph.getNodeAt(path);
        assert parent != null;

        // Now create the content information ...
        configurationContent = new ConfigurationContent(source, workspaceName, path, context);
        return this;
    }

    protected ConfigurationContent getConfigurationContent() {
        return configurationContent;
    }

    protected Path path( String path ) {
        return context.getValueFactories().getPathFactory().create(path);
    }

    public MimeTypeDetectorDetails<? extends DnaConfiguration> mimeTypeDetector( String name ) {
        return new MimeTypeDetectorBuilder<DnaConfiguration>(this);
    }

    /**
     * Construct an engine that reflects the current state of this configuration. This method always creates a new instance.
     * 
     * @return the resulting engine; never null
     */
    public DnaEngine build() {
        return new DnaEngine();
    }

    public interface Returnable<ReturnType> {
        /**
         * Return the configuration component.
         * 
         * @return the configuration component; never null
         */
        ReturnType and();
    }

    protected static class BaseReturnable<ReturnType> implements Returnable<ReturnType> {
        protected final ReturnType returnObject;

        protected BaseReturnable( ReturnType returnObject ) {
            this.returnObject = returnObject;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.config.DnaConfiguration.Returnable#and()
         */
        public ReturnType and() {
            return returnObject;
        }
    }

    /**
     * Interface used to set up and build a RepositorySource instance.
     * 
     * @param <ReturnType> the type of the configuration component
     */
    public interface MimeTypeDetectorDetails<ReturnType> extends Returnable<ReturnType> {
    }

    protected static class MimeTypeDetectorBuilder<ReturnType> extends BaseReturnable<ReturnType>
        implements MimeTypeDetectorDetails<ReturnType> {
        protected MimeTypeDetectorBuilder( ReturnType returnObject ) {
            super(returnObject);
        }
    }

    /**
     * Interface used to set up and build a RepositorySource instance.
     * 
     * @param <ReturnType> the type of the configuration component
     */
    public interface RepositorySourceDetails<ReturnType>
        extends SetProperties<RepositorySourceDetails<ReturnType>>, Returnable<ReturnType> {
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
         * Set the property value to an object.
         * 
         * @param beanPropertyName the name of the JavaBean-style property (e.g., "retryLimit")
         * @param value the new value for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setProperty( String beanPropertyName,
                                Object value );
    }

    protected class SourceDetails<ReturnType> extends BaseReturnable<ReturnType> implements RepositorySourceDetails<ReturnType> {

        protected SourceDetails( ReturnType returnObject ) {
            super(returnObject);
        }

        public RepositorySourceDetails<ReturnType> setProperty( String beanPropertyName,
                                                                int value ) {
            return setProperty(beanPropertyName, (Object)value);
        }

        public RepositorySourceDetails<ReturnType> setProperty( String beanPropertyName,
                                                                long value ) {
            return setProperty(beanPropertyName, (Object)value);
        }

        public RepositorySourceDetails<ReturnType> setProperty( String beanPropertyName,
                                                                short value ) {
            return setProperty(beanPropertyName, (Object)value);
        }

        public RepositorySourceDetails<ReturnType> setProperty( String beanPropertyName,
                                                                boolean value ) {
            return setProperty(beanPropertyName, (Object)value);
        }

        public RepositorySourceDetails<ReturnType> setProperty( String beanPropertyName,
                                                                float value ) {
            return setProperty(beanPropertyName, (Object)value);
        }

        public RepositorySourceDetails<ReturnType> setProperty( String beanPropertyName,
                                                                double value ) {
            return setProperty(beanPropertyName, (Object)value);
        }

        public RepositorySourceDetails<ReturnType> setProperty( String beanPropertyName,
                                                                String value ) {
            return setProperty(beanPropertyName, (Object)value);
        }

        public RepositorySourceDetails<ReturnType> setProperty( String beanPropertyName,
                                                                Object value ) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Representation of the current configuration content.
     */
    @Immutable
    protected static class ConfigurationContent {
        private final RepositorySource source;
        private final Path path;
        private final String workspace;
        private final ExecutionContext context;
        private Graph graph;

        protected ConfigurationContent( RepositorySource source,
                                        String workspace,
                                        Path path,
                                        ExecutionContext context ) {
            this.source = source;
            this.path = path != null ? path : RootPath.INSTANCE;
            this.workspace = workspace;
            this.context = context;
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
         * Return a copy of this configuration that uses the supplied path instead of this object's {@link #getPath() path}.
         * 
         * @param path the desired path for the new configuration; if null, then "/" is used
         * @return the new configuration
         */
        public ConfigurationContent with( Path path ) {
            return new ConfigurationContent(source, workspace, path, context);
        }

        /**
         * Return a copy of this configuration that uses the supplied workspace name instead of this object's
         * {@link #getWorkspace() workspace}.
         * 
         * @param workspace the desired workspace name for the new configuration; if null, then the default workspace will be used
         * @return the new configuration
         */
        public ConfigurationContent withWorkspace( String workspace ) {
            return new ConfigurationContent(source, workspace, path, context);
        }

        /**
         * Obtain a graph to this configuration repository. This method will always return the same graph instance.
         * 
         * @return the graph; never null
         */
        protected Graph graph() {
            if (graph == null) {
                graph = Graph.create(source, context);
                if (workspace != null) graph.useWorkspace(workspace);
            }
            return graph;
        }
    }
}
