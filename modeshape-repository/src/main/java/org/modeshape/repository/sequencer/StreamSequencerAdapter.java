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
package org.modeshape.repository.sequencer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.modeshape.common.collection.Collections;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.util.Logger;
import org.modeshape.common.util.Reflection;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.JcrNtLexicon;
import org.modeshape.graph.Node;
import org.modeshape.graph.observe.NetChangeObserver.NetChange;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.sequencer.StreamSequencer;
import org.modeshape.graph.sequencer.StreamSequencerContext;
import org.modeshape.repository.RepositoryI18n;
import org.modeshape.repository.util.RepositoryNodePath;

/**
 * An adapter class that wraps a {@link StreamSequencer} instance to be a {@link Sequencer}.
 */
public class StreamSequencerAdapter implements Sequencer {

    private static final Logger LOGGER = Logger.getLogger(StreamSequencerAdapter.class);

    private SequencerConfig configuration;
    private final StreamSequencer streamSequencer;

    public StreamSequencerAdapter( StreamSequencer streamSequencer ) {
        this.streamSequencer = streamSequencer;
    }

    /**
     * {@inheritDoc}
     */
    public SequencerConfig getConfiguration() {
        return this.configuration;
    }

    /**
     * {@inheritDoc}
     */
    public void setConfiguration( SequencerConfig configuration ) {
        this.configuration = configuration;

        /*
         * Try to pass the configured properties through to the stream sequencer
         */
        if (configuration.getProperties() != null) {
            final Class<?> streamSequencerClass = streamSequencer.getClass();
            Reflection reflection = new Reflection(streamSequencerClass);
            // Try to set the 'classpath' property first ...
            try {
                reflection.invokeSetterMethodOnTarget("classpath", streamSequencer, configuration.getComponentClasspathArray());
            } catch (Exception e) {
                // Ignore, but try the list form ...
                try {
                    reflection.invokeSetterMethodOnTarget("classpath", streamSequencer, configuration.getComponentClasspath());
                } catch (Exception e2) {
                    // Ignore ...
                }
            }
            // Now set all the other properties ...
            for (Map.Entry<String, Object> entry : configuration.getProperties().entrySet()) {
                // Set the JavaBean-style property on the RepositorySource instance ...
                final String propertyName = entry.getKey();
                try {
                    reflection.invokeSetterMethodOnTarget(propertyName, streamSequencer, entry.getValue());
                    LOGGER.trace("Set '{0}' property from sequencer configuration on '{1}' stream sequencer implementation to {2}",
                                 propertyName,
                                 streamSequencerClass.getName(),
                                 entry.getValue());
                } catch (NoSuchMethodException e) {
                    // If the value is an Object[], see if the values are compatible with String[] and try again
                } catch (Exception ignore) {
                    LOGGER.debug("Unable to set '{0}' property from sequencer configuration on '{1}' stream sequencer implementation",
                                 propertyName,
                                 streamSequencerClass.getName());
                    // It's possible that these properties weren't intended for the stream sequencer anyway
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void execute( Node input,
                         String sequencedPropertyName,
                         NetChange changes,
                         Set<RepositoryNodePath> outputPaths,
                         SequencerContext context,
                         Problems problems ) throws SequencerException {
        // 'sequencedPropertyName' contains the name of the modified property on 'input' that resulted in the call to this
        // sequencer.
        // 'changes' contains all of the changes to this node that occurred in the transaction.
        // 'outputPaths' contains the paths of the node(s) where this sequencer is to save it's data.

        // Get the property that contains the data, given by 'propertyName' ...
        Property sequencedProperty = input.getProperty(sequencedPropertyName);

        if (sequencedProperty == null || sequencedProperty.isEmpty()) {
            String msg = RepositoryI18n.unableToFindPropertyForSequencing.text(sequencedPropertyName, input.getLocation());
            throw new SequencerException(msg);
        }

        // Get the binary property with the image content, and build the image metadata from the image ...
        ValueFactories factories = context.getExecutionContext().getValueFactories();
        SequencerOutputMap output = new SequencerOutputMap(factories);
        InputStream stream = null;
        Throwable firstError = null;
        Binary binary = factories.getBinaryFactory().create(sequencedProperty.getFirstValue());
        binary.acquire();
        try {
            // Parallel the JCR lemma for converting objects into streams
            stream = binary.getStream();
            StreamSequencerContext streamSequencerContext = createStreamSequencerContext(input,
                                                                                         sequencedProperty,
                                                                                         context,
                                                                                         problems);
            this.streamSequencer.sequence(stream, output, streamSequencerContext);
        } catch (Throwable t) {
            // Record the error ...
            firstError = t;
        } finally {
            try {
                if (stream != null) {
                    // Always close the stream, recording the error if we've not yet seen an error
                    try {
                        stream.close();
                    } catch (Throwable t) {
                        if (firstError == null) firstError = t;
                    } finally {
                        stream = null;
                    }
                }
                if (firstError != null) {
                    // Wrap and throw the first error that we saw ...
                    throw new SequencerException(firstError);
                }
            } finally {
                binary.release();
            }
        }

        // Accumulator of paths that we've added to the batch but have not yet been submitted to the graph
        Set<Path> builtPaths = new HashSet<Path>();

        // Find each output node and save the image metadata there ...
        for (RepositoryNodePath outputPath : outputPaths) {
            // Get the name of the repository source, workspace and the path to the output node
            final String repositoryWorkspaceName = outputPath.getWorkspaceName();
            final String nodePath = outputPath.getNodePath();

            // Find or create the output node in this session ...
            context.destinationGraph().useWorkspace(repositoryWorkspaceName);

            buildPathTo(nodePath, context, builtPaths);
            // Node outputNode = context.graph().getNodeAt(nodePath);

            // Now save the image metadata to the output node ...
            saveOutput(nodePath, output, context, builtPaths);
        }

        context.getDestination().submit();
    }

    /**
     * Creates all nodes along the given node path if they are missing. Ensures that nodePath is a valid path to a node.
     * 
     * @param nodePath the node path to create
     * @param context the sequencer context under which it should be created
     * @param builtPaths a set of the paths that have already been created but not submitted in this batch
     */
    private void buildPathTo( String nodePath,
                              SequencerContext context,
                              Set<Path> builtPaths ) {
        PathFactory pathFactory = context.getExecutionContext().getValueFactories().getPathFactory();
        Path targetPath = pathFactory.create(nodePath);

        buildPathTo(targetPath, context, builtPaths);
    }

    /**
     * Creates all nodes along the given node path if they are missing. Ensures that nodePath is a valid path to a node.
     * 
     * @param targetPath the node path to create
     * @param context the sequencer context under which it should be created
     * @param builtPaths a set of the paths that have already been created but not submitted in this batch
     */
    private void buildPathTo( Path targetPath,
                              SequencerContext context,
                              Set<Path> builtPaths ) {
        PathFactory pathFactory = context.getExecutionContext().getValueFactories().getPathFactory();
        PropertyFactory propFactory = context.getExecutionContext().getPropertyFactory();

        if (targetPath.isRoot()) return;
        Path workingPath = pathFactory.createRootPath();
        Path.Segment[] segments = targetPath.getSegmentsArray();
        int i = 0;
        Property primaryType = propFactory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.UNSTRUCTURED);
        for (int max = segments.length; i < max; i++) {
            workingPath = pathFactory.create(workingPath, segments[i]);

            if (!builtPaths.contains(workingPath)) {
                try {
                    context.destinationGraph().getNodeAt(workingPath);
                } catch (PathNotFoundException pnfe) {
                    context.getDestination().create(workingPath, primaryType);
                    builtPaths.add(workingPath);
                }
            }
        }
    }

    /**
     * Save the sequencing output to the supplied node. This method does not need to save the output, as that is done by the
     * caller of this method.
     * 
     * @param nodePath the existing node onto (or below) which the output is to be written; never null
     * @param output the (immutable) sequencing output; never null
     * @param context the execution context for this sequencing operation; never null
     * @param builtPaths a set of the paths that have already been created but not submitted in this batch
     */
    protected void saveOutput( String nodePath,
                               SequencerOutputMap output,
                               SequencerContext context,
                               Set<Path> builtPaths ) {
        if (output.isEmpty()) return;
        final PathFactory pathFactory = context.getExecutionContext().getValueFactories().getPathFactory();
        final PropertyFactory propertyFactory = context.getExecutionContext().getPropertyFactory();
        final Path outputNodePath = pathFactory.create(nodePath);

        // Iterate over the entries in the output, in Path's natural order (shorter paths first and in lexicographical order by
        // prefix and name)
        for (SequencerOutputMap.Entry entry : output) {
            Path targetNodePath = entry.getPath();

            // Resolve this path relative to the output node path, handling any parent or self references ...
            Path absolutePath = targetNodePath.isAbsolute() ? targetNodePath : outputNodePath.resolve(targetNodePath);

            List<Property> properties = new LinkedList<Property>();
            // Set all of the properties on this
            for (SequencerOutputMap.PropertyValue property : entry.getPropertyValues()) {
                Object value = property.getValue();
                Property newProperty = propertyFactory.create(property.getName(), value);
                properties.add(newProperty);
                // TODO: Handle reference properties - currently passed in as Paths
            }

            if (absolutePath.getParent() != null) {
                buildPathTo(absolutePath.getParent(), context, builtPaths);
            }
            context.getDestination().create(absolutePath, properties);
            builtPaths.add(absolutePath);
        }
    }

    protected String[] extractMixinTypes( Object value ) {
        if (value instanceof String[]) return (String[])value;
        if (value instanceof String) return new String[] {(String)value};
        return null;
    }

    protected StreamSequencerContext createStreamSequencerContext( Node input,
                                                                   Property sequencedProperty,
                                                                   SequencerContext context,
                                                                   Problems problems ) {
        assert input != null;
        assert sequencedProperty != null;
        assert context != null;
        assert problems != null;
        ValueFactories factories = context.getExecutionContext().getValueFactories();
        Path path = factories.getPathFactory().create(input.getLocation().getPath());

        Set<org.modeshape.graph.property.Property> props = Collections.<Property>unmodifiableSet(input.getPropertiesByName()
                                                                                                      .values());
        Name fileName = path.getLastSegment().getName();
        if (JcrLexicon.CONTENT.equals(fileName) && !path.isRoot()) {
            // We're actually sequencing the "jcr:content" child node of an "nt:file" node, but the name of
            // the file is actually the name of the "jcr:content" node's parent "nt:file" node.
            fileName = path.getParent().getLastSegment().getName();
        }
        String mimeType = getMimeType(context, sequencedProperty, fileName.getLocalName());
        return new StreamSequencerContext(context.getExecutionContext(), path, props, mimeType, problems);
    }

    protected String getMimeType( SequencerContext context,
                                  Property sequencedProperty,
                                  String name ) {
        SequencerException err = null;
        String mimeType = null;
        InputStream stream = null;
        try {
            // Parallel the JCR lemma for converting objects into streams
            stream = new ByteArrayInputStream(sequencedProperty.toString().getBytes());
            mimeType = context.getExecutionContext().getMimeTypeDetector().mimeTypeOf(name, stream);
            return mimeType;
        } catch (Exception error) {
            err = new SequencerException(error);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException error) {
                    // Only throw exception if an exception was not already thrown
                    if (err == null) err = new SequencerException(error);
                }
            }
        }
        throw err;
    }
}
