package org.modeshape.jcr.api.observation;

/**
 * Extension of the {@link javax.jcr.observation.Event} interface allowing custom ModeShape events.
 *
 * @author Horia Chiorean
 */
public interface Event extends javax.jcr.observation.Event {

    /**
     * Interface which contains the constants for sequencing events.
     */
    public interface Sequencing {
        /**
         * Constant under which the path of the node which triggered the sequencing, in the case of an {@link Event.Sequencing#NODE_SEQUENCED}
         * event, will be found in the event info map
         */
        String SEQUENCED_NODE_PATH = "sequencedNodePath";
        /**
         * Constant under which the id of the node which triggered the sequencing, in the case of an {@link Event.Sequencing#NODE_SEQUENCED}
         * event, will be found in the event info map
         */
        String SEQUENCED_NODE_ID = "sequencedNodeId";

        /**
         * Constant under which the sequencer id (as configured) is set in the event info map.
         */
        String SEQUENCER_NAME = "sequencerName";

        /**
         * Constant under which the user id (username) of the user which triggered the sequencing is set in the event info map.
         */
        String USER_ID = "userId";

        /**
         * Constant under which the path resolved by the sequencer at the beginning of the sequencing process is set in the
         * event info map.
         */
        String SELECTED_PATH = "selectedPath";

        /**
         * Constant under which the configured output path of the sequencer is set in the event info map.
         */
        String OUTPUT_PATH = "outputPath";

        /**
         * Constant under which the {@link Throwable} object, in case of an {@link Event.Sequencing#NODE_SEQUENCING_FAILURE} event, will be
         * found in the event info map.
         */
        String SEQUENCING_FAILURE_CAUSE = "sequencingFailureCause";

        /**
         * Generated on persist when a node is successfully sequenced.
         * <ul>
         * <li>{@link #getPath} returns the absolute path of the node that was added due to sequencing.</li>
         * <li>{@link #getIdentifier} returns the identifier of the node that was sequenced.</li>
         * <li>{@link #getInfo} returns an <code>Map</code> object, which under the {@link org.modeshape.jcr.api.observation.Event.Sequencing#SEQUENCED_NODE_PATH} key has the
         * absolute path of the Node which triggered the sequencing and under the {@link org.modeshape.jcr.api.observation.Event.Sequencing#SEQUENCED_NODE_ID} key has the identifier
         * of the node which triggered the sequencing
         * </li>
         * </ul>
         */
        int NODE_SEQUENCED = 0x80;

        /**
         * Generated when the sequencing of an input node fails.
         *
         * <ul>
         *  <li>{@link #getPath} returns the absolute path of the input node that triggered the sequencing.</li>
         *  <li>{@link #getIdentifier} returns the identifier of the input node that triggered the sequencing</li>
         *  <li>{@link #getInfo} returns an <code>Map</code> object, which under the {@link org.modeshape.jcr.api.observation.Event.Sequencing#SEQUENCING_FAILURE_CAUSE} key
         *  contains the {@link Throwable} instance which caused the failure.
         *  </li>
         * </ul>
         */
        int NODE_SEQUENCING_FAILURE = 0x100;

        /**
         * Convenience event code for listeners which want to listen both for {@link #NODE_SEQUENCED} and {@link #NODE_SEQUENCING_FAILURE}
         * events.
         */
        int ALL = NODE_SEQUENCED | NODE_SEQUENCING_FAILURE;
    }
}
