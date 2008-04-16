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
package org.jboss.dna.services.sequencers;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Set;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.jboss.dna.common.jcr.Path;
import org.jboss.dna.common.monitor.ProgressMonitor;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.services.ExecutionContext;
import org.jboss.dna.services.RepositoryNodePath;
import org.jboss.dna.services.ServicesI18n;
import org.jboss.dna.services.observation.NodeChange;

/**
 * @author Randall Hauch
 */
public abstract class StreamSequencer implements Sequencer {

    private SequencerConfig configuration;

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
    public void execute( Node input, String sequencedPropertyName, NodeChange changes, Set<RepositoryNodePath> outputPaths, ExecutionContext context, ProgressMonitor progressMonitor )
        throws RepositoryException, SequencerException {
        // 'sequencedPropertyName' contains the name of the modified property on 'input' that resuled the call to this sequencer
        // 'changes' contains all of the changes to this node that occurred in the transaction.
        // 'outputPaths' contains the paths of the node(s) where this sequencer is to save it's data

        try {
            progressMonitor.beginTask(100, ServicesI18n.sequencingPropertyOnNode, sequencedPropertyName, input.getPath());

            // Get the property that contains the image data, given by 'propertyName' ...
            Property imageDataProperty = null;
            try {
                imageDataProperty = input.getProperty(sequencedPropertyName);
            } catch (PathNotFoundException e) {
                String msg = ServicesI18n.unableToFindPropertyForSequencing.text(sequencedPropertyName, input.getPath());
                throw new SequencerException(msg, e);
            }
            progressMonitor.worked(10);

            // Get the binary property with the image content, and build the image metadata from the image ...
            SequencerOutputMap output = new SequencerOutputMap();
            InputStream stream = null;
            Throwable firstError = null;
            ProgressMonitor sequencingMonitor = progressMonitor.createSubtask(50);
            try {
                stream = imageDataProperty.getStream();
                sequence(stream, output, context, sequencingMonitor);
            } catch (Throwable t) {
                // Record the error ...
                firstError = t;
            } finally {
                sequencingMonitor.done();
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
            ProgressMonitor writingProgress = progressMonitor.createSubtask(40);
            writingProgress.beginTask(outputPaths.size(), ServicesI18n.writingOutputSequencedFromPropertyOnNodes, sequencedPropertyName, input.getPath(), outputPaths.size());
            for (RepositoryNodePath outputPath : outputPaths) {
                Session session = null;
                try {
                    // Get the name of the repository workspace and the path to the output node
                    final String repositoryWorkspaceName = outputPath.getRepositoryWorkspaceName();
                    final String nodePath = outputPath.getNodePath();

                    // Create a session to the repository where the data should be written ...
                    session = context.getSessionFactory().createSession(repositoryWorkspaceName);

                    // Find or create the output node in this session ...
                    Node outputNode = context.getTools().findOrCreateNode(session, nodePath);

                    // Now save the image metadata to the output node ...
                    if (saveOutput(outputNode, output, context)) {
                        session.save();
                    }
                } finally {
                    writingProgress.worked(1);
                    // Always close the session ...
                    if (session != null) session.logout();
                }
            }
            writingProgress.done();
        } finally {
            progressMonitor.done();
        }
    }

    /**
     * Save the sequencing output to the supplied node. This method does not need to save the output, as that is done by the
     * caller of this method.
     * @param outputNode the existing node onto (or below) which the output is to be written; never null
     * @param outputProperties the (immutable) sequencing output; never null
     * @param context the execution context for this sequencing operation; never null
     * @return true if the output was written to the node, or false if no information was written
     * @throws RepositoryException
     */
    protected boolean saveOutput( Node outputNode, SequencerOutputMap output, ExecutionContext context ) throws RepositoryException {
        if (output.isEmpty()) return false;
        final Path outputNodePath = new Path(outputNode.getPath());

        // Iterate over the entries in the output, in Path's natural order (shorter paths first and in lexicographical order by
        // prefix and name)
        for (SequencerOutputMap.Entry entry : output) {
            Path targetNodePath = entry.getPath();
            String primaryType = entry.getPrimaryTypeValue();

            // Resolve this path relative to the output node path, handling any parent or self references ...
            Path absolutePath = targetNodePath.isAbsolute() ? targetNodePath : outputNodePath.resolve(targetNodePath);
            Path relativePath = absolutePath.relativeTo(outputNodePath);

            // Find or add the node (which may involve adding intermediate nodes) ...
            Node targetNode = outputNode;
            for (int i = 0, max = relativePath.size(); i != max; ++i) {
                Path.Segment segment = relativePath.getSegment(i);
                String qualifiedName = segment.getQualifiedName(true);
                if (targetNode.hasNode(qualifiedName)) {
                    targetNode = targetNode.getNode(qualifiedName);
                } else {
                    // It doesn't exist, so create it ...
                    if (segment.hasIndex()) {
                        // Use a name without an index ...
                        qualifiedName = segment.getQualifiedName(false);
                    }
                    // We only have the primary type for the final one ...
                    if (i == (max - 1) && primaryType != null) {
                        targetNode = targetNode.addNode(qualifiedName, primaryType);
                    } else {
                        targetNode = targetNode.addNode(qualifiedName);
                    }
                }
                assert targetNode != null;
            }
            assert targetNode != null;

            // Set all of the properties on this
            for (SequencerOutputMap.PropertyValue property : entry.getPropertyValues()) {
                String propertyName = property.getName();
                Object value = property.getValue();
                Logger.getLogger(this.getClass()).trace("Writing property {0}/{1}={2}", targetNode.getPath(), propertyName, value);
                if (value instanceof Boolean) {
                    targetNode.setProperty(propertyName, ((Boolean)value).booleanValue());
                } else if (value instanceof String) {
                    targetNode.setProperty(propertyName, (String)value);
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
                } else if (value instanceof Calendar) {
                    targetNode.setProperty(propertyName, (Calendar)value);
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
                        String msg = ServicesI18n.errorGettingNodeRelativeToNode.text(value, outputNode.getPath());
                        throw new SequencerException(msg, e);
                    }
                } else if (value == null) {
                    // Remove the property ...
                    targetNode.setProperty(propertyName, (String)null);
                } else {
                    String msg = ServicesI18n.unknownPropertyValueType.text(value, value.getClass().getName());
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

    /**
     * Sequence the data found in the supplied stream, placing the output information into the supplied map.
     * @param stream the stream with the data to be sequenced; never null
     * @param output the output from the sequencing operation; never null
     * @param context the context in which this sequencer is executing; never null
     * @param progress the progress monitor that should be kept updated with the sequencer's progress and that should be
     * frequently consulted as to whether this operation has been {@link ProgressMonitor#isCancelled() cancelled}.
     */
    protected abstract void sequence( InputStream stream, SequencerOutput output, ExecutionContext context, ProgressMonitor progressMonitor );

}
