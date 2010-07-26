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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.version.Version;
import org.modeshape.graph.property.Name;

/**
 * Convenience wrapper around a version {@link JcrNode node}.
 */
class JcrVersionNode extends JcrNode implements Version {

    private static final Version[] EMPTY_VERSION_ARRAY = new Version[0];

    JcrVersionNode( AbstractJcrNode node ) {
        super(node.cache, node.nodeId, node.location);

        assert !node.isRoot() : "Versions should always be located in the /jcr:system/jcr:versionStorage subgraph";
    }

    /**
     * @{inheritDoc
     */
    @Override
    public JcrVersionHistoryNode getContainingHistory() throws RepositoryException {
        return new JcrVersionHistoryNode(getParent());
    }

    /**
     * @{inheritDoc
     */
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
    public AbstractJcrNode getFrozenNode() throws RepositoryException {
        return getNode(JcrLexicon.FROZEN_NODE);
    }

    /**
     * @{inheritDoc
     */
    @Override
    public Version[] getPredecessors() throws RepositoryException {
        return getNodesForProperty(JcrLexicon.PREDECESSORS);
    }

    /**
     * Returns the successor versions of this version. This corresponds to returning all the nt:version nodes referenced by the
     * jcr:successors multi-value property in the nt:version node that represents this version.
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

        for (int i = 0; i < values.length; i++) {
            String uuid = values[i].getString();

            AbstractJcrNode node = session().getNodeByUUID(uuid);
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

        String uuid = values[0].getString();
        AbstractJcrNode node = session().getNodeByUUID(uuid);

        return (JcrVersionNode)node;
    }

    boolean isSuccessorOf( JcrVersionNode other ) throws RepositoryException {
        if (!other.hasProperty(JcrLexicon.SUCCESSORS)) return false;

        Value[] successors = other.getProperty(JcrLexicon.SUCCESSORS).getValues();

        String uuidString = uuid().toString();
        for (int i = 0; i < successors.length; i++) {
            if (uuidString.equals(successors[i].getString())) {
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
