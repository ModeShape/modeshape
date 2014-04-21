/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.JcrRepository.RunningState;
import org.modeshape.jcr.Sequencers.SequencingContext;
import org.modeshape.jcr.Sequencers.SequencingWorkItem;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.monitor.DurationMetric;
import org.modeshape.jcr.api.monitor.ValueMetric;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.change.RecordingChanges;
import org.modeshape.jcr.value.Name;

final class SequencingRunner implements Runnable {

    /** We don't use the standard logging convention here; we want clients to easily configure logging for sequencing */
    private static final Logger LOGGER = Logger.getLogger("org.modeshape.jcr.sequencing.runner");
    private static final boolean TRACE = LOGGER.isTraceEnabled();
    private static final boolean DEBUG = LOGGER.isDebugEnabled();

    private static final String DERIVED_NODE_TYPE_NAME = "mode:derived";
    private static final String DERIVED_FROM_PROPERTY_NAME = "mode:derivedFrom";

    private final RunningState repository;
    private final SequencingWorkItem work;

    protected SequencingRunner( RunningState repository,
                                SequencingWorkItem work ) {
        this.repository = repository;
        this.work = work;
    }

    @Override
    public void run() {
        JcrSession inputSession = null;
        JcrSession outputSession = null;
        final RepositoryStatistics stats = repository.statistics();
        Sequencer sequencer = null;
        String sequencerName = null;
        try {
            // Create the required session(s) ...
            inputSession = repository.loginInternalSession(work.getInputWorkspaceName());
            if (work.getOutputWorkspaceName() != null && !work.getOutputWorkspaceName().equals(work.getInputWorkspaceName())) {
                outputSession = repository.loginInternalSession(work.getOutputWorkspaceName());
            } else {
                outputSession = inputSession;
            }

            // Get the sequencer ...
            sequencer = repository.sequencers().getSequencer(work.getSequencerId());
            if (sequencer == null) {
                if (DEBUG) {
                    LOGGER.debug("Unable to find sequencer with ID '{0}' in repository '{1}'; skipping input '{3}:{2}' and output '{5}:{4}'",
                                 work.getSequencerId(), repository.name(), work.getInputPath(), work.getInputWorkspaceName(),
                                 work.getOutputPath(), work.getOutputWorkspaceName());
                }
                return;
            }
            sequencerName = sequencer.getName();

            String logMsg = null;
            if (TRACE || DEBUG) {
                logMsg = StringUtil.createString("sequencer '{0}' in repository '{1}' with input '{3}:{2}' to produce '{5}:{4}'",
                                                 sequencerName,
                                                 repository.name(),
                                                 work.getInputPath(),
                                                 work.getInputWorkspaceName(),
                                                 work.getOutputPath(),
                                                 work.getOutputWorkspaceName() != null ? work.getOutputWorkspaceName() : work.getInputWorkspaceName());
                LOGGER.debug("Running {0}", logMsg);
            }

            // Find the selected node ...
            AbstractJcrNode selectedNode = inputSession.getNode(work.getSelectedPath());

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

            if (sequencer.hasAcceptedMimeTypes()) {
                // Get the MIME type, first by looking at the changed property's parent node
                // (or grand-parent node if parent is 'jcr:content') ...
                String mimeType = getInputMimeType(changedProperty);

                // See if the sequencer accepts the MIME type ...
                if (mimeType != null && !sequencer.isAccepted(mimeType)) {
                    LOGGER.debug("Skipping sequencing because MIME type of input doesn't match expectations for {0}", logMsg);
                    return; // nope
                }
            }

            AbstractJcrNode outputNode = null;
            String primaryType = null;
            if (work.getSelectedPath().equals(work.getOutputPath())) {
                // The output is to go directly under the sequenced node ...
                outputNode = selectedNode.getName().equals(JcrConstants.JCR_CONTENT) ? selectedNode.getParent() : selectedNode;
                primaryType = selectedNode.getPrimaryNodeType().getName();
            } else {
                // Find the parent of the output if it exists, or create the node(s) along the path if not ...
                AbstractJcrNode parentOfOutput = null;
                try {
                    parentOfOutput = outputSession.getNode(work.getOutputPath());
                } catch (PathNotFoundException e) {
                    LOGGER.trace("Creating missing output path for {0}", logMsg);
                    JcrTools tools = new JcrTools();
                    parentOfOutput = (AbstractJcrNode)tools.findOrCreateNode(outputSession, work.getOutputPath());
                }

                // Now determine the name of top node in the output, using the last segment of the selected path ...
                String outputNodeName = computeOutputNodeName(selectedNode);

                // Remove any existing output (from a prior sequencing run on this same input) ...
                removeExistingOutputNodes(parentOfOutput, outputNodeName, work.getSelectedPath(), logMsg);

                // Create the output node
                if (parentOfOutput.isNew() && parentOfOutput.getName().equals(outputNodeName)) {
                    // avoid creating a duplicate path with the same name
                    outputNode = parentOfOutput;
                } else {
                    if (TRACE) {
                        LOGGER.trace("Creating output node '{0}' under parent '{1}' for {2}", outputNodeName,
                                     parentOfOutput.getPath(), logMsg);
                    }
                    outputNode = parentOfOutput.addNode(outputNodeName, JcrConstants.NT_UNSTRUCTURED);
                }

                // and make sure the output node has the 'mode:derived' mixin ...
                outputNode.addMixin(DERIVED_NODE_TYPE_NAME);
                outputNode.setProperty(DERIVED_FROM_PROPERTY_NAME, work.getSelectedPath());
            }

            // Execute the sequencer ...
            DateTime now = outputSession.dateFactory().create();
            Sequencer.Context context = new SequencingContext(now, outputSession.getValueFactory());
            if (inputSession.isLive() && (inputSession == outputSession || outputSession.isLive())) {
                final long start = System.nanoTime();

                try {
                    LOGGER.trace("Executing {0}", logMsg);
                    if (sequencer.execute(changedProperty, outputNode, context)) {
                        LOGGER.trace("Completed executing {0}", logMsg);

                        // Make sure that the sequencer did not change the primary type of the selected node ..
                        if (selectedNode == outputNode && !selectedNode.getPrimaryNodeType().getName().equals(primaryType)) {
                            String msg = RepositoryI18n.sequencersMayNotChangeThePrimaryTypeOfTheSelectedNode.text();
                            throw new RepositoryException(msg);
                        }

                        // find the new nodes created by the sequencing before saving, so we can properly fire the events
                        List<AbstractJcrNode> outputNodes = findOutputNodes(outputNode);

                        // set the createdBy property (if it applies) to the user which triggered the sequencing, not the context
                        // of the saving session
                        setCreatedByIfNecessary(outputSession, outputNodes);

                        // outputSession
                        LOGGER.trace("Saving session used by {0}", logMsg);
                        outputSession.save();

                        // fire the sequencing event after save (hopefully by this time the transaction has been committed)
                        LOGGER.trace("Firing events resulting from {0}", logMsg);
                        fireSequencingEvent(selectedNode, outputNodes, outputSession, sequencerName);

                        long durationInNanos = Math.abs(System.nanoTime() - start);
                        Map<String, String> payload = new HashMap<String, String>();
                        payload.put("sequencerName", sequencer.getClass().getName());
                        payload.put("sequencedPath", changedProperty.getPath());
                        payload.put("outputPath", outputNode.getPath());
                        stats.recordDuration(DurationMetric.SEQUENCER_EXECUTION_TIME, durationInNanos, TimeUnit.NANOSECONDS,
                                             payload);
                    }
                } catch (Throwable t) {
                    fireSequencingFailureEvent(selectedNode, inputSession, t, sequencerName);
                    // let it bubble down, because we still want to log it and update the stats
                    throw t;
                }
            }
        } catch (Throwable t) {
            Logger logger = Logger.getLogger(getClass());
            if (work.getOutputWorkspaceName() != null) {
                logger.error(t, RepositoryI18n.errorWhileSequencingNodeIntoWorkspace, sequencerName, repository.name(),
                             work.getInputPath(), work.getInputWorkspaceName(), work.getOutputPath(),
                             work.getOutputWorkspaceName());
            } else {
                logger.error(t, RepositoryI18n.errorWhileSequencingNode, sequencerName, repository.name(), work.getInputPath(),
                             work.getInputWorkspaceName(), work.getOutputPath());
            }
        } finally {
            stats.increment(ValueMetric.SEQUENCED_COUNT);
            stats.decrement(ValueMetric.SEQUENCER_QUEUE_SIZE);
            if (inputSession != null && inputSession.isLive()) inputSession.logout();
            if (outputSession != null && outputSession != inputSession && outputSession.isLive()) outputSession.logout();
        }
    }

    /**
     * @param changedProperty the property being sequenced
     * @return the MIME type, or null if the MIME type could not be found
     * @throws ItemNotFoundException
     * @throws AccessDeniedException
     * @throws RepositoryException
     * @throws PathNotFoundException
     * @throws ValueFormatException
     * @throws IOException
     */
    static String getInputMimeType( Property changedProperty )
        throws ItemNotFoundException, AccessDeniedException, RepositoryException, PathNotFoundException, ValueFormatException,
        IOException {
        Node parent = changedProperty.getParent();
        String mimeType = null;
        if (parent.hasProperty(JcrConstants.JCR_MIME_TYPE)) {
            // The parent node has a 'jcr:mimeType' node ...
            Property property = parent.getProperty(JcrConstants.JCR_MIME_TYPE);
            if (!property.isMultiple()) {
                // The standard 'jcr:mimeType' property is single valued, but we're technically not checking if
                // the property has that particular property definition (only by name) ...
                mimeType = property.getString();
            }
        } else if (parent.getName().equals(JcrConstants.JCR_CONTENT)) {
            // There is no 'jcr:mimeType' property, and since the sequenced property is on the 'jcr:content' node,
            // get the parent (probably 'nt:file') node and look for the 'jcr:mimeType' property there ...
            try {
                parent = parent.getParent();
                if (parent.hasProperty(JcrConstants.JCR_MIME_TYPE)) {
                    Property property = parent.getProperty(JcrConstants.JCR_MIME_TYPE);
                    if (!property.isMultiple()) {
                        // The standard 'jcr:mimeType' property is single valued, but we're technically not checking if
                        // the property has that particular property definition (only by name) ...
                        mimeType = property.getString();
                    }
                }
            } catch (ItemNotFoundException e) {
                // must be the root ...
            }
        }
        if (mimeType == null && !changedProperty.isMultiple() && changedProperty.getType() == PropertyType.BINARY) {
            // Still don't know the MIME type of the property, so if it's a BINARY property we can check it ...
            javax.jcr.Binary binary = changedProperty.getBinary();
            if (binary instanceof org.modeshape.jcr.api.Binary) {
                mimeType = ((org.modeshape.jcr.api.Binary)binary).getMimeType(parent.getName());
            }
        }
        return mimeType;
    }

    private void setCreatedByIfNecessary( JcrSession outputSession,
                                          List<AbstractJcrNode> outputNodes ) throws RepositoryException {
        // if the mix:created mixin is on any of the new nodes, we need to set the createdBy here, otherwise it will be
        // set by the system session when it saves and it will default to "modeshape-worker"
        for (AbstractJcrNode node : outputNodes) {
            if (node.isNodeType(JcrMixLexicon.CREATED)) {
                node.setProperty(JcrLexicon.CREATED_BY, outputSession.getValueFactory().createValue(work.getUserId()), true,
                                 true, false, false);
            }
        }
    }

    private void fireSequencingEvent( AbstractJcrNode sequencedNode,
                                      List<AbstractJcrNode> outputNodes,
                                      JcrSession outputSession,
                                      String sequencerName ) throws RepositoryException {

        RecordingChanges sequencingChanges = new RecordingChanges(outputSession.context().getProcessId(),
                                                                  outputSession.getRepository().repositoryKey(),
                                                                  outputSession.workspaceName(), outputSession.getRepository()
                                                                                                              .journalId());
        Name primaryType = sequencedNode.getPrimaryTypeName();
        Set<Name> mixinTypes = sequencedNode.getMixinTypeNames();
        for (AbstractJcrNode outputNode : outputNodes) {

            sequencingChanges.nodeSequenced(sequencedNode.key(), sequencedNode.path(), primaryType, mixinTypes, outputNode.key(),
                                            outputNode.path(), work.getOutputPath(), work.getUserId(), work.getSelectedPath(),
                                            sequencerName);
        }
        sequencingChanges.freeze(outputSession.getUserID(), null, outputSession.context().getValueFactories().getDateFactory().create());
        repository.changeBus().notify(sequencingChanges);
    }

    private void fireSequencingFailureEvent( AbstractJcrNode sequencedNode,
                                             JcrSession inputSession,
                                             Throwable cause,
                                             String sequencerName ) throws RepositoryException {
        assert sequencedNode != null;
        assert inputSession != null;
        Name primaryType = sequencedNode.getPrimaryTypeName();
        Set<Name> mixinTypes = sequencedNode.getMixinTypeNames();
        RecordingChanges sequencingChanges = new RecordingChanges(inputSession.context().getProcessId(),
                                                                  inputSession.getRepository().repositoryKey(),
                                                                  inputSession.workspaceName(), inputSession.getRepository()
                                                                                                            .journalId());
        sequencingChanges.nodeSequencingFailure(sequencedNode.key(), sequencedNode.path(), primaryType, mixinTypes,
                                                work.getOutputPath(), work.getUserId(), work.getSelectedPath(), sequencerName,
                                                cause);
        repository.changeBus().notify(sequencingChanges);
    }

    /**
     * Finds the top nodes which have been created during the sequencing process, based on the original output node. It is
     * important that this is called before the session is saved, because it uses the new flag.
     * 
     * @param rootOutputNode the node under which the output of the sequencing process was written to.
     * @return the first level of output nodes that were created during the sequencing process; never null
     * @throws RepositoryException if there is a problem finding the output nodes
     */
    private List<AbstractJcrNode> findOutputNodes( AbstractJcrNode rootOutputNode ) throws RepositoryException {
        if (rootOutputNode.isNew()) {
            return Arrays.asList(rootOutputNode);
        }

        // if the node was not new, we need to find the new sequenced nodes
        List<AbstractJcrNode> nodes = new ArrayList<AbstractJcrNode>();
        NodeIterator childrenIt = rootOutputNode.getNodesInternal();
        while (childrenIt.hasNext()) {
            Node child = childrenIt.nextNode();
            if (child.isNew()) {
                nodes.add((AbstractJcrNode)child);
            }
        }
        return nodes;
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

    /**
     * Remove any existing nodes that were generated by previous sequencing operations of the node at the selected path.
     * 
     * @param parentOfOutput the parent of the output; may not be null
     * @param outputNodeName the name of the output node; may not be null or empty
     * @param selectedPath the path of the node that was selected for sequencing
     * @param logMsg the log message, or null if trace/debug logging is not being used (this is passed in for efficiency reasons)
     * @throws RepositoryException if there is a problem accessing the repository content
     */
    private void removeExistingOutputNodes( AbstractJcrNode parentOfOutput,
                                            String outputNodeName,
                                            String selectedPath,
                                            String logMsg ) throws RepositoryException {
        // Determine if there is an existing output node ...
        if (TRACE) {
            LOGGER.trace("Looking under '{0}' for existing output to be removed for {1}", parentOfOutput.getPath(), logMsg);
        }
        NodeIterator outputIter = parentOfOutput.getNodesInternal(outputNodeName);
        while (outputIter.hasNext()) {
            Node outputNode = outputIter.nextNode();
            // See if this is indeed the output, which should have the 'mode:derived' mixin ...
            if (outputNode.isNodeType(DERIVED_NODE_TYPE_NAME) && outputNode.hasProperty(DERIVED_FROM_PROPERTY_NAME)) {
                // See if it was an output for the same input node ...
                String derivedFrom = outputNode.getProperty(DERIVED_FROM_PROPERTY_NAME).getPath();
                if (selectedPath.equals(derivedFrom)) {
                    // Delete it ...
                    if (TRACE) {
                        LOGGER.trace("Removing existing output node '{0}' for {1}", outputNode.getPath(), logMsg);
                    }
                    outputNode.remove();
                }
            }
        }

    }
}
