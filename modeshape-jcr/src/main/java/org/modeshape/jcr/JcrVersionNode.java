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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.version.Version;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.basic.NodeKeyReference;

/**
 * Convenience wrapper around a version {@link JcrNode node}.
 */
class JcrVersionNode extends JcrSystemNode implements Version {

    private static final Version[] EMPTY_VERSION_ARRAY = new Version[0];

    JcrVersionNode( JcrSession session,
                    NodeKey key ) {
        super(session, key);
    }

    @Override
    Type type() {
        return Type.VERSION;
    }

    @Override
    public JcrVersionHistoryNode getParent() throws ItemNotFoundException, RepositoryException {
        return (JcrVersionHistoryNode)super.getParent();
    }

    @Override
    public JcrVersionHistoryNode getContainingHistory() throws RepositoryException {
        return getParent();
    }

    @Override
    public Calendar getCreated() throws RepositoryException {
        return getProperty(JcrLexicon.CREATED).getDate();
    }

    /**
     * Returns the frozen node (the child node named {@code jcr:frozenNode}) for this version, if one exists.
     * 
     * @return the frozen node for this version, if one exists
     * @throws ItemNotFoundException if this version has no child with the name {@code jcr:frozenNode}. This should only happen
     *         for root versions in a version history.
     * @throws RepositoryException if an error occurs accessing the repository
     */
    @Override
    public AbstractJcrNode getFrozenNode() throws RepositoryException {
        return childNode(JcrLexicon.FROZEN_NODE, Type.SYSTEM);
    }

    @Override
    public Version[] getPredecessors() throws RepositoryException {
        return getNodesForProperty(JcrLexicon.PREDECESSORS);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the successor versions of this version. This corresponds to returning all the nt:version nodes referenced by the
     * jcr:successors multi-value property in the nt:version node that represents this version.
     * </p>
     * 
     * @see javax.jcr.version.Version#getSuccessors()
     */
    @Override
    public Version[] getSuccessors() throws RepositoryException {
        return getNodesForProperty(JcrLexicon.SUCCESSORS);
    }

    private final Version[] getNodesForProperty( Name propertyName ) throws RepositoryException {
        assert JcrLexicon.SUCCESSORS.equals(propertyName) || JcrLexicon.PREDECESSORS.equals(propertyName);

        Property references = getProperty(propertyName);
        if (references == null) return EMPTY_VERSION_ARRAY;

        Value[] values = references.getValues();
        List<JcrVersionNode> versions = new ArrayList<JcrVersionNode>(values.length);
        for (Value value : values) {
            NodeKey key = ((NodeKeyReference) ((JcrValue) value).value()).getNodeKey();
            AbstractJcrNode node = session().node(key, null);
            versions.add((JcrVersionNode)node);
        }
        return versions.toArray(EMPTY_VERSION_ARRAY);
    }

    private final JcrVersionNode getFirstNodeForProperty( Name propertyName ) throws RepositoryException {
        assert JcrLexicon.SUCCESSORS.equals(propertyName) || JcrLexicon.PREDECESSORS.equals(propertyName);

        Property references = getProperty(propertyName);
        if (references == null) return null;

        Value[] values = references.getValues();
        if (values.length == 0) return null;

        NodeKey key = ((NodeKeyReference) ((JcrValue)values[0]).value()).getNodeKey();
        AbstractJcrNode node = session().node(key, null);

        return (JcrVersionNode)node;
    }

    boolean isLinearSuccessorOf( JcrVersionNode other ) throws RepositoryException {
        if (!other.hasProperty(JcrLexicon.SUCCESSORS)) return false;

        Value[] successors = other.getProperty(JcrLexicon.SUCCESSORS).getValues();

        String id = getIdentifier();
        for (int i = 0; i < successors.length; i++) {
            if (id.equals(successors[i].getString())) {
                return true;
            }
        }

        return false;
    }

    boolean isEventualSuccessorOf( JcrVersionNode other ) throws RepositoryException {
        if (isLinearSuccessorOf(other)) {
            return true;
        }

        for (Version successor : other.getSuccessors()) {
            if (isEventualSuccessorOf((JcrVersionNode) successor)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public JcrVersionNode getLinearPredecessor() throws RepositoryException {
        return getFirstNodeForProperty(JcrLexicon.PREDECESSORS);
    }

    @Override
    public JcrVersionNode getLinearSuccessor() throws RepositoryException {
        return getFirstNodeForProperty(JcrLexicon.SUCCESSORS);
    }

}
