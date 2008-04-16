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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.jboss.dna.common.jcr.Path;
import org.jboss.dna.common.monitor.ProgressMonitor;
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
            Map<Path, Object> output = new HashMap<Path, Object>();
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
            Map<Path, Object> immutableOutput = Collections.unmodifiableMap(output);
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
                    if (saveOutput(outputNode, immutableOutput)) {
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
     * @param node the existing node onto (or below) which the output is to be written; never null
     * @param outputProperties the (immutable) sequencing output; never null
     * @return true if the output was written to the node, or false if no information was written
     * @throws RepositoryException
     */
    protected boolean saveOutput( Node node, Map<Path, Object> outputProperties ) throws RepositoryException {
        if (outputProperties.isEmpty()) return false;

        // Get the paths and sort them in Path's natural order (which puts "jcr:name", "jcr:primaryType", and "jcr:mixinTypes"
        // first) ...
        LinkedList<Path> paths = new LinkedList<Path>(outputProperties.keySet());
        Collections.sort(paths);

        final Path nodePath = new Path(node.getPath());

        // Iterate over the paths in the correct order, with the shortest path being first.
        //
        // This logic is somewhat complicated, because it looks for "jcr:primaryType" and "jcr:mixinType"
        // properties (which appear before the others on the same node). These are not only optional
        // but they may be the only properties on a node before the properties on another node.
        //
        // Another complication is that the paths may have more than one segment, which means
        // that intermediate nodes need to be found (or created if they don't exist) using the
        // default primaryType.
        // 
        // Therefore, this logic iterates over the path, and for each path checks to see whether
        // it is one of the special properties. Then the referenced node is found
        //
        String primaryType = null;
        String[] mixinTypes = null;
        Node targetNode = null;
        Path targetNodePath = null;
        while (!paths.isEmpty()) {
            Path relativePath = paths.remove();

            // Resolve this relative path to an absolute path
            // Path resolvedRelativePath = resolveRelativePath(nodePath, relativePath);
            //
            // if (false) {
            // // If this path represents the "jcr:name", "jcr:primaryType" or "jcr:mixinTypes" properties ...
            // Path.Segment propertySegment = resolvedRelativePath.getLastSegment();
            // if (propertySegment.getPrefix().equals("jcr")) {
            // String segmentName = propertySegment.getName();
            // if (segmentName.equals("name")) {
            // primaryType = null;
            // mixinTypes = null;
            // // Do nothing special with the name, so go on to the next path ...
            // continue;
            // }
            // if (segmentName.equals("primaryType")) {
            // // Record the primary type, and reset the mixinTypes (which should be next if there is one) ...
            // primaryType = (String)outputProperties.get(relativePath);
            // mixinTypes = null;
            // // Peek at the next path, which may be a different node altogether ...
            // Path next = resolveRelativePath(nodePath, paths.peek());
            // if (next != null && next.hasSameAncestor(resolvedRelativePath) && next.endsWith("jcr:mixinTypes")) {
            // // The next node is the mixinTypes for the same node as the primary type, so get it ...
            // Path originalNext = paths.remove();
            // Object value = outputProperties.get(originalNext);
            // mixinTypes = extractMixinTypes(value);
            // }
            // } else if (segmentName.equals("mixinTypes")) {
            // // There was no primary type, so record the mixin type ...
            // primaryType = null;
            // Object value = outputProperties.get(relativePath);
            // mixinTypes = extractMixinTypes(value);
            // }
            //
            // // If there is no target node, we need to resolve it ...
            // if (targetNode != null) {
            //
            // }
            //
            // // Find/create the node given the primary type and optional mixin types ...
            // Path relativePathToNode = resolvedRelativePath.getAncestor();
            // targetNode = node;
            // if (relativePathToNode.size() > 1) {
            // // Find/create the nodes down to the node we're interested in
            // // and we don't have values for primaryType or mixinTypes for these ...
            // for (Path.Segment segment : relativePathToNode.getAncestor()) {
            // Node child = targetNode.getNode(segment.getQualifiedName(true));
            // if (child == null) {
            // child = targetNode.addNode(segment.getQualifiedName(false));
            // }
            // targetNode = child;
            // }
            // }
            // // Find/create the node for the last segment of the path, for which we have a primaryType and/or mixinType ...
            // Path.Segment segmentToNode = relativePathToNode.getLastSegment();
            // Node existingNode = targetNode.getNode(segmentToNode.getQualifiedName(true));
            // if (existingNode == null) {
            // existingNode = targetNode.addNode(segmentToNode.getQualifiedName(false), primaryType);
            // }
            // targetNode = existingNode;
            //
            // // Continue to grab the next path ...
            // continue;
            // }
            // }

        }

        return true;
    }

    // protected Path resolveRelativePath( Path startingNode, Path pathToProperty ) {
    // Path absolutePath = pathToProperty.isAbsolute() ? pathToProperty : startingNode.resolve(pathToProperty); // always
    // // normalized
    // Path resolvedRelativePath = absolutePath.relativeTo(startingNode);
    // return resolvedRelativePath;
    // }

    protected String[] extractMixinTypes( Object value ) {
        if (value instanceof String[]) return (String[])value;
        if (value instanceof String) return new String[] {(String)value};
        return null;
    }

    /**
     * Sequence the data found in the supplied stream, placing the output information into the supplied map.
     * @param stream the stream with the data to be sequenced; never null
     * @param output the map into which should be placed the sequencing output
     * @param context the context in which this sequencer is executing; never null
     * @param progress the progress monitor that should be kept updated with the sequencer's progress and that should be
     * frequently consulted as to whether this operation has been {@link ProgressMonitor#isCancelled() cancelled}.
     */
    protected abstract void sequence( InputStream stream, Map<Path, Object> output, ExecutionContext context, ProgressMonitor progressMonitor );

}
