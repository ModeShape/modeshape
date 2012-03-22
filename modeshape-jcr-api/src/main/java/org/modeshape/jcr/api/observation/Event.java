package org.modeshape.jcr.api.observation;

/**
 * Extension of the {@link javax.jcr.observation.Event} interface allowing custom ModeShape events.
 *
 * @author Horia Chiorean
 */
public interface Event extends javax.jcr.observation.Event {

    /**
     * Generated on persist when a node is sequenced.
     * <ul>
     * <li>{@link #getPath} returns the absolute path of the node that was added due to sequencing.</li>
     * <li>{@link #getIdentifier} returns the identifier of the node that was sequenced.</li>
     * <li>{@link #getInfo} returns an <code>Map</code> object, which under the <code>originalNodePath</code> key has the
     * absolute path of the Node which triggered the sequencing and under the <code>originalNodeId</code> key has the identifier
     * of the node which triggered the sequencing</code>
     * </li>
     * </ul>
     */
    public static final int NODE_SEQUENCED = 0x80;


    /**
     * Extra information holder
     */
    public final class Info {
        private Info() {
        }

        public static final String ORIGINAL_NODE_PATH = "originalNodePath";
        public static final String ORIGINAL_NODE_ID = "originalNodeId";
    }
}
