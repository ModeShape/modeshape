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
package org.modeshape.jcr;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import org.modeshape.common.util.Logger;
import org.modeshape.jcr.JcrRepository.RunningState;
import org.modeshape.jcr.Sequencers.SequencingContext;
import org.modeshape.jcr.Sequencers.SequencingWorkItem;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.monitor.DurationMetric;
import org.modeshape.jcr.api.monitor.ValueMetric;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.jcr.api.value.DateTime;

final class SequencingRunner implements Runnable {
    private final JcrRepository repository;
    private final SequencingWorkItem work;

    protected SequencingRunner( JcrRepository repository,
                                SequencingWorkItem work ) {
        this.repository = repository;
        this.work = work;
    }

    @Override
    public void run() {
        JcrSession inputSession = null;
        JcrSession outputSession = null;
        final RunningState state = repository.runningState();
        final RepositoryStatistics stats = state.statistics();
        try {
            // Create the required session(s) ...
            inputSession = state.loginInternalSession(work.getInputWorkspaceName());
            if (work.getOutputWorkspaceName() != null && !work.getOutputWorkspaceName().equals(work.getInputWorkspaceName())) {
                outputSession = state.loginInternalSession(work.getOutputWorkspaceName());
            } else {
                outputSession = inputSession;
            }

            // Get the sequencer ...
            Sequencer sequencer = state.sequencers().getSequencer(work.getSequencerName());
            if (sequencer == null) return;

            // Find the selected node ...
            Node selectedNode = inputSession.getNode(work.getSelectedPath());

            // Find the input that has changed and is to be sequenced ...
            Item inputItem = inputSession.getItem(work.getInputPath());
            Property changedProperty = null;
            if (inputItem instanceof Property) {
                changedProperty = (Property)inputItem;
            } else {
                Node changedNode = (Node)inputItem;
                // now look for a property that was changed or added ...
                changedProperty = changedNode.getProperty(work.getChangedPropertyName());
            }
            assert changedProperty != null;

            Node outputNode = null;
            String primaryType = null;
            if (work.getSelectedPath().equals(work.getOutputPath())) {
                // The output is to go directly under the sequenced node ...
                outputNode = selectedNode.getName().equals(JcrConstants.JCR_CONTENT) ? selectedNode.getParent() : selectedNode;
                primaryType = selectedNode.getPrimaryNodeType().getName();
            } else {
                // Find the parent of the output if it exists, or create the node(s) along the path if not ...
                Node parentOfOutput = null;
                try {
                    parentOfOutput = outputSession.getNode(work.getOutputPath());
                } catch (PathNotFoundException e) {
                    JcrTools tools = new JcrTools();
                    parentOfOutput = tools.findOrCreateNode(outputSession, work.getOutputPath());
                }

                // Now determine the name of top node in the output, using the last segment of the selected path ...
                String outputNodeName = computeOutputNodeName(selectedNode);

                // Remove any existing output (from a prior sequencing run on this same input) ...
                removeExistingOutputNodes(parentOfOutput, outputNodeName, work.getSelectedPath());

                // Create the output node ...
                outputNode = parentOfOutput.addNode(outputNodeName, JcrConstants.NT_UNSTRUCTURED);

                // and make sure the output node has the 'mode:derived' mixin ...
                outputNode.addMixin(DERIVED_NODE_TYPE_NAME);
                outputNode.setProperty(DERIVED_FROM_PROPERTY_NAME, work.getSelectedPath());
            }

            // Execute the sequencer ...
            DateTime now = outputSession.dateFactory().create();
            Sequencer.Context context = new SequencingContext(now);
            if (inputSession.isLive() && (inputSession == outputSession || outputSession.isLive())) {
                final long start = System.nanoTime();
                if (sequencer.execute(changedProperty, outputNode, context)) {
                    // Make sure that the sequencer did not change the primary type of the selected node ..
                    if (selectedNode == outputNode && !selectedNode.getPrimaryNodeType().getName().equals(primaryType)) {
                        String msg = RepositoryI18n.sequencersMayNotChangeThePrimaryTypeOfTheSelectedNode.text();
                        throw new RepositoryException(msg);
                    }
                    // Save the session
                    outputSession.save();
                    long durationInNanos = System.nanoTime() - start;
                    Map<String, String> payload = new HashMap<String, String>();
                    payload.put("sequencerName", sequencer.getName());
                    payload.put("sequencedPath", changedProperty.getPath());
                    payload.put("outputPath", outputNode.getPath());
                    stats.recordDuration(DurationMetric.SEQUENCER_EXECUTION_TIME, durationInNanos, TimeUnit.NANOSECONDS, payload);
                }
            }
        } catch (Throwable t) {
            Logger logger = Logger.getLogger(getClass());
            if (work.getOutputWorkspaceName() != null) {
                logger.error(t,
                             RepositoryI18n.errorWhileSequencingNodeIntoWorkspace,
                             work.getSequencerName(),
                             state.name(),
                             work.getInputPath(),
                             work.getInputWorkspaceName(),
                             work.getOutputPath(),
                             work.getOutputWorkspaceName());
            } else {
                logger.error(t,
                             RepositoryI18n.errorWhileSequencingNode,
                             work.getSequencerName(),
                             state.name(),
                             work.getInputPath(),
                             work.getInputWorkspaceName(),
                             work.getOutputPath());
            }
        } finally {
            stats.increment(ValueMetric.SEQUENCED_COUNT);
            stats.decrement(ValueMetric.SEQUENCER_QUEUE_SIZE);
            if (inputSession != null && inputSession.isLive()) inputSession.logout();
            if (outputSession != null && outputSession != inputSession && outputSession.isLive()) outputSession.logout();
        }
    }

    /**
     * Compute the name of the output node. If the selected node is named "jcr:content", this method assumes that the selected
     * node is a child of an 'nt:file' node, and so it returns the name of that 'nt:file' node. Otherwise, this method returns the
     * name of the selected node.
     * 
     * @param selectedNode the node that was selected for sequencing; may not be null
     * @return the name that should be used for the output node; never null
     * @throws RepositoryException if there is a problem accessing the repository content
     */
    protected final String computeOutputNodeName( Node selectedNode ) throws RepositoryException {
        String selectedNodeName = selectedNode.getName();
        if (selectedNodeName.equals(JcrConstants.JCR_CONTENT)) {
            try {
                return selectedNode.getParent().getName();
            } catch (ItemNotFoundException e) {
                // selected node must be the root node ?!?!
            }
        }
        return selectedNodeName;
    }

    protected static final String DERIVED_NODE_TYPE_NAME = "mode:derived";
    protected static final String DERIVED_FROM_PROPERTY_NAME = "mode:derivedFrom";

    /**
     * Remove any existing nodes that were generated by previous sequencing operations of the node at the selected path.
     * 
     * @param parentOfOutput the parent of the output; may not be null
     * @param outputNodeName the name of the output node; may not be null or empty
     * @param selectedPath the path of the node that was selected for sequencing
     * @throws RepositoryException if there is a problem accessing the repository content
     */
    protected final void removeExistingOutputNodes( Node parentOfOutput,
                                                    String outputNodeName,
                                                    String selectedPath ) throws RepositoryException {
        // Determine if there is an existing output node ...
        NodeIterator outputIter = parentOfOutput.getNodes(outputNodeName);
        while (outputIter.hasNext()) {
            Node outputNode = outputIter.nextNode();
            // See if this is indeed the output, which should have the 'mode:derived' mixin ...
            if (outputNode.isNodeType("mode:derived") && outputNode.hasProperty("mode:derivedFrom")) {
                // See if it was an output for the same input node ...
                String derivedFrom = outputNode.getProperty("mode:derivedFrom").getPath();
                if (selectedPath.equals(derivedFrom)) {
                    // Delete it ...
                    outputNode.remove();
                }
            }
        }

    }
}
