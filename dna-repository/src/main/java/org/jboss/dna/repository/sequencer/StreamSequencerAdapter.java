/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.repository.sequencer;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.ValueFactories;
import org.jboss.dna.graph.sequencer.SequencerContext;
import org.jboss.dna.graph.sequencer.StreamSequencer;
import org.jboss.dna.repository.RepositoryI18n;
import org.jboss.dna.repository.mimetype.MimeType;
import org.jboss.dna.repository.observation.NodeChange;
import org.jboss.dna.repository.util.JcrExecutionContext;
import org.jboss.dna.repository.util.RepositoryNodePath;

/**
 * An adapter class that wraps a {@link StreamSequencer} instance to be a {@link Sequencer}.
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class StreamSequencerAdapter implements Sequencer {

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
    }

    /**
     * {@inheritDoc}
     */
    public void execute( Node input,
                         String sequencedPropertyName,
                         NodeChange changes,
                         Set<RepositoryNodePath> outputPaths,
                         JcrExecutionContext execContext,
                         Problems problems ) throws RepositoryException, SequencerException {
        // 'sequencedPropertyName' contains the name of the modified property on 'input' that resulted in the call to this
        // sequencer.
        // 'changes' contains all of the changes to this node that occurred in the transaction.
        // 'outputPaths' contains the paths of the node(s) where this sequencer is to save it's data.

        // Get the property that contains the data, given by 'propertyName' ...
        Property sequencedProperty = null;
        try {
            sequencedProperty = input.getProperty(sequencedPropertyName);
        } catch (PathNotFoundException e) {
            String msg = RepositoryI18n.unableToFindPropertyForSequencing.text(sequencedPropertyName, input.getPath());
            throw new SequencerException(msg, e);
        }

        // Get the binary property with the image content, and build the image metadata from the image ...
        SequencerOutputMap output = new SequencerOutputMap(execContext.getValueFactories());
        InputStream stream = null;
        Throwable firstError = null;
        try {
            stream = sequencedProperty.getStream();
            SequencerContext sequencerContext = createSequencerContext(input, sequencedProperty, execContext, problems);
            this.streamSequencer.sequence(stream, output, sequencerContext);
        } catch (Throwable t) {
            // Record the error ...
            firstError = t;
        } finally {
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
        }

        // Find each output node and save the image metadata there ...
        for (RepositoryNodePath outputPath : outputPaths) {
            Session session = null;
            try {
                // Get the name of the repository workspace and the path to the output node
                final String repositoryWorkspaceName = outputPath.getRepositoryWorkspaceName();
                final String nodePath = outputPath.getNodePath();

                // Create a session to the repository where the data should be written ...
                session = execContext.getSessionFactory().createSession(repositoryWorkspaceName);

                // Find or create the output node in this session ...
                Node outputNode = execContext.getTools().findOrCreateNode(session, nodePath);

                // Now save the image metadata to the output node ...
                if (saveOutput(outputNode, output, execContext)) {
                    session.save();
                }
            } finally {
                // Always close the session ...
                if (session != null) session.logout();
            }
        }
    }

    /**
     * Save the sequencing output to the supplied node. This method does not need to save the output, as that is done by the
     * caller of this method.
     * 
     * @param outputNode the existing node onto (or below) which the output is to be written; never null
     * @param output the (immutable) sequencing output; never null
     * @param context the execution context for this sequencing operation; never null
     * @return true if the output was written to the node, or false if no information was written
     * @throws RepositoryException
     */
    protected boolean saveOutput( Node outputNode,
                                  SequencerOutputMap output,
                                  JcrExecutionContext context ) throws RepositoryException {
        if (output.isEmpty()) return false;
        final PathFactory pathFactory = context.getValueFactories().getPathFactory();
        final NamespaceRegistry namespaceRegistry = context.getNamespaceRegistry();
        final Path outputNodePath = pathFactory.create(outputNode.getPath());
        final Name jcrPrimaryTypePropertyName = context.getValueFactories().getNameFactory().create("jcr:primaryType");

        // Iterate over the entries in the output, in Path's natural order (shorter paths first and in lexicographical order by
        // prefix and name)
        for (SequencerOutputMap.Entry entry : output) {
            Path targetNodePath = entry.getPath();
            Name primaryType = entry.getPrimaryTypeValue();

            // Resolve this path relative to the output node path, handling any parent or self references ...
            Path absolutePath = targetNodePath.isAbsolute() ? targetNodePath : outputNodePath.resolve(targetNodePath);
            Path relativePath = absolutePath.relativeTo(outputNodePath);

            // Find or add the node (which may involve adding intermediate nodes) ...
            Node targetNode = outputNode;
            for (int i = 0, max = relativePath.size(); i != max; ++i) {
                Path.Segment segment = relativePath.getSegment(i);
                String qualifiedName = segment.getString(namespaceRegistry);
                if (targetNode.hasNode(qualifiedName)) {
                    targetNode = targetNode.getNode(qualifiedName);
                } else {
                    // It doesn't exist, so create it ...
                    if (segment.hasIndex()) {
                        // Use a name without an index ...
                        qualifiedName = segment.getName().getString(namespaceRegistry);
                    }
                    // We only have the primary type for the final one ...
                    if (i == (max - 1) && primaryType != null) {
                        targetNode = targetNode.addNode(qualifiedName, primaryType.getString(namespaceRegistry,
                                                                                             Path.NO_OP_ENCODER));
                    } else {
                        targetNode = targetNode.addNode(qualifiedName);
                    }
                }
                assert targetNode != null;
            }
            assert targetNode != null;

            // Set all of the properties on this
            for (SequencerOutputMap.PropertyValue property : entry.getPropertyValues()) {
                String propertyName = property.getName().getString(namespaceRegistry, Path.NO_OP_ENCODER);
                Object value = property.getValue();
                if (jcrPrimaryTypePropertyName.equals(property.getName())) {
                    // Skip the primary type property (which is protected in Jackrabbit 1.5)
                    Logger.getLogger(this.getClass()).trace("Skipping property {0}/{1}={2}",
                                                            targetNode.getPath(),
                                                            propertyName,
                                                            value);
                    continue;
                }
                Logger.getLogger(this.getClass()).trace("Writing property {0}/{1}={2}", targetNode.getPath(), propertyName, value);
                if (value instanceof Boolean) {
                    targetNode.setProperty(propertyName, ((Boolean)value).booleanValue());
                } else if (value instanceof String) {
                    targetNode.setProperty(propertyName, (String)value);
                } else if (value instanceof String[]) {
                    targetNode.setProperty(propertyName, (String[])value);
                } else if (value instanceof Integer) {
                    targetNode.setProperty(propertyName, ((Integer)value).intValue());
                } else if (value instanceof Short) {
                    targetNode.setProperty(propertyName, ((Short)value).shortValue());
                } else if (value instanceof Long) {
                    targetNode.setProperty(propertyName, ((Long)value).longValue());
                } else if (value instanceof Float) {
                    targetNode.setProperty(propertyName, ((Float)value).floatValue());
                } else if (value instanceof Double) {
                    targetNode.setProperty(propertyName, ((Double)value).doubleValue());
                } else if (value instanceof Binary) {
                    Binary binaryValue = (Binary)value;
                    try {
                        binaryValue.acquire();
                        targetNode.setProperty(propertyName, binaryValue.getStream());
                    } finally {
                        binaryValue.release();
                    }
                } else if (value instanceof BigDecimal) {
                    targetNode.setProperty(propertyName, ((BigDecimal)value).doubleValue());
                } else if (value instanceof DateTime) {
                    targetNode.setProperty(propertyName, ((DateTime)value).toCalendar());
                } else if (value instanceof Date) {
                    DateTime instant = context.getValueFactories().getDateFactory().create((Date)value);
                    targetNode.setProperty(propertyName, instant.toCalendar());
                } else if (value instanceof Calendar) {
                    targetNode.setProperty(propertyName, (Calendar)value);
                } else if (value instanceof Name) {
                    Name nameValue = (Name)value;
                    String stringValue = nameValue.getString(namespaceRegistry);
                    targetNode.setProperty(propertyName, stringValue);
                } else if (value instanceof Path) {
                    // Find the path to reference node ...
                    Path pathToReferencedNode = (Path)value;
                    if (!pathToReferencedNode.isAbsolute()) {
                        // Resolve the path relative to the output node ...
                        pathToReferencedNode = outputNodePath.resolve(pathToReferencedNode);
                    }
                    // Find the referenced node ...
                    try {
                        Node referencedNode = outputNode.getNode(pathToReferencedNode.getString());
                        targetNode.setProperty(propertyName, referencedNode);
                    } catch (PathNotFoundException e) {
                        String msg = RepositoryI18n.errorGettingNodeRelativeToNode.text(value, outputNode.getPath());
                        throw new SequencerException(msg, e);
                    }
                } else if (value == null) {
                    // Remove the property ...
                    targetNode.setProperty(propertyName, (String)null);
                } else {
                    String msg = RepositoryI18n.unknownPropertyValueType.text(value, value.getClass().getName());
                    throw new SequencerException(msg);
                }
            }
        }

        return true;
    }

    protected String[] extractMixinTypes( Object value ) {
        if (value instanceof String[]) return (String[])value;
        if (value instanceof String) return new String[] {(String)value};
        return null;
    }

    protected SequencerContext createSequencerContext( Node input,
                                                       Property sequencedProperty,
                                                       ExecutionContext context,
                                                       Problems problems ) throws RepositoryException {
        assert input != null;
        assert sequencedProperty != null;
        assert context != null;
        assert problems != null;
        // Translate JCR path and property values to DNA constructs and cache them to improve performance and prevent
        // RepositoryException from being thrown by getters
        // Note: getMimeType() will still operate lazily, and thus throw a SequencerException, since it is very intrusive and
        // potentially slow-running.
        ValueFactories factories = context.getValueFactories();
        Path path = factories.getPathFactory().create(input.getPath());
        Set<org.jboss.dna.graph.property.Property> props = new HashSet<org.jboss.dna.graph.property.Property>();
        for (PropertyIterator iter = input.getProperties(); iter.hasNext();) {
            javax.jcr.Property jcrProp = iter.nextProperty();
            org.jboss.dna.graph.property.Property prop;
            if (jcrProp.getDefinition().isMultiple()) {
                Value[] jcrVals = jcrProp.getValues();
                Object[] vals = new Object[jcrVals.length];
                int ndx = 0;
                for (Value jcrVal : jcrVals) {
                    vals[ndx++] = convert(factories, jcrProp.getName(), jcrVal);
                }
                prop = context.getPropertyFactory().create(factories.getNameFactory().create(jcrProp.getName()), vals);
            } else {
                Value jcrVal = jcrProp.getValue();
                Object val = convert(factories, jcrProp.getName(), jcrVal);
                prop = context.getPropertyFactory().create(factories.getNameFactory().create(jcrProp.getName()), val);
            }
            props.add(prop);
        }
        props = Collections.unmodifiableSet(props);
        String mimeType = getMimeType(sequencedProperty, path.getLastSegment().getName().getLocalName());
        return new SequencerContext(context, path, props, mimeType, problems);
    }

    protected Object convert( ValueFactories factories,
                              String name,
                              Value jcrValue ) throws RepositoryException {
        switch (jcrValue.getType()) {
            case PropertyType.BINARY: {
                return factories.getBinaryFactory().create(jcrValue.getStream());
            }
            case PropertyType.BOOLEAN: {
                return factories.getBooleanFactory().create(jcrValue.getBoolean());
            }
            case PropertyType.DATE: {
                return factories.getDateFactory().create(jcrValue.getDate());
            }
            case PropertyType.DOUBLE: {
                return factories.getDoubleFactory().create(jcrValue.getDouble());
            }
            case PropertyType.LONG: {
                return factories.getLongFactory().create(jcrValue.getLong());
            }
            case PropertyType.NAME: {
                return factories.getNameFactory().create(jcrValue.getString());
            }
            case PropertyType.PATH: {
                return factories.getPathFactory().create(jcrValue.getString());
            }
            case PropertyType.REFERENCE: {
                return factories.getReferenceFactory().create(jcrValue.getString());
            }
            case PropertyType.STRING: {
                return factories.getStringFactory().create(jcrValue.getString());
            }
            default: {
                throw new RepositoryException(RepositoryI18n.unknownPropertyValueType.text(name, jcrValue.getType()));
            }
        }
    }

    @SuppressWarnings( "null" )
    // The need for the SuppressWarnings looks like an Eclipse bug
    protected String getMimeType( Property sequencedProperty,
                                  String name ) {
        SequencerException err = null;
        String mimeType = null;
        InputStream stream = null;
        try {
            stream = sequencedProperty.getStream();
            mimeType = MimeType.of(name, stream);
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
        if (err != null) throw err;
        return mimeType;
    }
}
