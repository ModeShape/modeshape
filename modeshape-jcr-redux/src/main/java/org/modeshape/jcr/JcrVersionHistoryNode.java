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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;

/**
 * Convenience wrapper around a version history {@link JcrNode node}.
 */
@ThreadSafe
final class JcrVersionHistoryNode extends JcrSystemNode implements VersionHistory {

    JcrVersionHistoryNode( JcrSession session,
                           NodeKey key ) {
        super(session, key);
    }

    @Override
    Type type() {
        return Type.VERSION_HISTORY;
    }

    /**
     * Get the node that represents the version labels for this history. Each version label node (see Section 3.13.5.5 of the JCR
     * 2.0 specification) contains a REFERENCE property for each label, where the name of the property is the label and the
     * REFERENCE points to the 'nt:version' child node that has that label.
     * 
     * @return a reference to the {@code jcr:versionLabels} child node of this history node.
     * @throws RepositoryException if an error occurs accessing this node
     */
    protected final AbstractJcrNode versionLabels() throws RepositoryException {
        return childNode(JcrLexicon.VERSION_LABELS, Type.NODE);
    }

    @Override
    public VersionIterator getAllVersions() throws RepositoryException {
        return new JcrVersionIterator(getNodes());
    }

    @Override
    public JcrVersionNode getRootVersion() throws RepositoryException {
        return (JcrVersionNode)childNode(JcrLexicon.ROOT_VERSION, Type.VERSION);
    }

    @Override
    public JcrVersionNode getVersion( String versionName ) throws VersionException, RepositoryException {
        try {
            return (JcrVersionNode)getNode(versionName);
        } catch (PathNotFoundException pnfe) {
            throw new VersionException(JcrI18n.invalidVersionName.text(versionName, getPath()));
        }
    }

    @Override
    public JcrVersionNode getVersionByLabel( String label ) throws VersionException, RepositoryException {
        try {
            javax.jcr.Property prop = versionLabels().getProperty(label);
            return (JcrVersionNode)prop.getNode();
        } catch (PathNotFoundException e) {
            throw new VersionException(JcrI18n.invalidVersionLabel.text(label, getPath()));
        } catch (ItemNotFoundException e) {
            throw new VersionException(JcrI18n.labeledNodeNotFound.text(label, getPath()));
        }
    }

    @Override
    public String[] getVersionLabels() throws RepositoryException {
        PropertyIterator iter = versionLabels().getProperties();
        String[] labels = new String[(int)iter.getSize()];
        for (int i = 0; iter.hasNext(); i++) {
            labels[i] = iter.nextProperty().getName();
        }
        return labels;
    }

    /**
     * Returns the version labels that point to the given version
     * 
     * @param version the version for which the labels should be retrieved
     * @return the set of version labels for that version; never null
     * @throws RepositoryException if an error occurs accessing the repository
     */
    private Set<String> versionLabelsFor( Version version ) throws RepositoryException {
        if (!version.getParent().equals(this)) {
            throw new VersionException(JcrI18n.invalidVersion.text(version.getPath(), getPath()));
        }

        String versionId = version.getIdentifier();
        PropertyIterator iter = versionLabels().getProperties();
        if (iter.getSize() == 0) return Collections.emptySet();

        Set<String> labels = new HashSet<String>();
        while (iter.hasNext()) {
            javax.jcr.Property prop = iter.nextProperty();
            if (versionId.equals(prop.getString())) {
                labels.add(prop.getName());
            }
        }
        return labels;
    }

    @Override
    public String[] getVersionLabels( Version version ) throws RepositoryException {
        Set<String> labels = versionLabelsFor(version);
        return labels.toArray(new String[labels.size()]);
    }

    @Override
    public String getVersionableUUID() throws RepositoryException {
        return getProperty(JcrLexicon.VERSIONABLE_UUID).getString();
    }

    @Override
    public boolean hasVersionLabel( String label ) throws RepositoryException {
        return versionLabels().hasProperty(label);
    }

    @Override
    public boolean hasVersionLabel( Version version,
                                    String label ) throws RepositoryException {
        return versionLabelsFor(version).contains(label);
    }

    @Override
    public void removeVersion( String versionName )
        throws ReferentialIntegrityException, AccessDeniedException, UnsupportedRepositoryOperationException, VersionException,
        RepositoryException {

        JcrVersionNode version = getVersion(versionName);
        assert version.getParent() == this;

        /*
         * Verify that the only references to this version are from its predecessors and successors in the version history.
         */
        for (PropertyIterator iter = version.getReferences(); iter.hasNext();) {
            AbstractJcrProperty prop = (AbstractJcrProperty)iter.next();
            AbstractJcrNode referrer = prop.getParent();

            // If the property's parent is the root node, fail.
            if (referrer.isRoot()) {
                throw new ReferentialIntegrityException(JcrI18n.cannotRemoveVersion.text(prop.getPath()));
            }
            if (!this.equals(referrer)) {
                throw new ReferentialIntegrityException(JcrI18n.cannotRemoveVersion.text(prop.getPath()));
            }
        }

        String versionId = version.getIdentifier();

        // Get the predecessors and successors for the version being removed ...
        AbstractJcrProperty predecessors = version.getProperty(JcrLexicon.PREDECESSORS);
        AbstractJcrProperty successors = version.getProperty(JcrLexicon.SUCCESSORS);

        // Remove the reference to the dead version from the successors property of all the predecessors
        Set<JcrValue> addedValues = new HashSet<JcrValue>();
        for (Value predecessorValue : predecessors.getValues()) {
            addedValues.clear();
            List<JcrValue> newNodeSuccessors = new ArrayList<JcrValue>();

            // Add each of the successors from the version's predecessor ...
            AbstractJcrNode predecessor = session().getNodeByIdentifier(predecessorValue.getString());
            JcrValue[] nodeSuccessors = predecessor.getProperty(JcrLexicon.SUCCESSORS).getValues();
            addValuesNotInSet(nodeSuccessors, newNodeSuccessors, versionId, addedValues);

            // Add each of the successors from the version being removed ...
            addValuesNotInSet(successors.getValues(), newNodeSuccessors, versionId, addedValues);

            // Set the property ...
            JcrValue[] newSuccessors = newNodeSuccessors.toArray(new JcrValue[newNodeSuccessors.size()]);
            predecessor.setProperty(JcrLexicon.SUCCESSORS, newSuccessors, PropertyType.REFERENCE);
            addedValues.clear();
        }

        // Remove the reference to the dead version from the predecessors property of all the successors
        for (Value successorUuid : successors.getValues()) {
            addedValues.clear();
            List<JcrValue> newNodePredecessors = new ArrayList<JcrValue>();

            // Add each of the predecessors from the version's successor ...
            AbstractJcrNode successor = session().getNodeByIdentifier(successorUuid.getString());
            JcrValue[] nodePredecessors = successor.getProperty(JcrLexicon.PREDECESSORS).getValues();
            addValuesNotInSet(nodePredecessors, newNodePredecessors, versionId, addedValues);

            // Add each of the predecessors from the version being removed ...
            addValuesNotInSet(predecessors.getValues(), newNodePredecessors, versionId, addedValues);

            // Set the property ...
            JcrValue[] newPredecessors = newNodePredecessors.toArray(new JcrValue[newNodePredecessors.size()]);
            successor.setProperty(JcrLexicon.PREDECESSORS, newPredecessors, PropertyType.REFERENCE);
        }

        // Use a separate system session to destroy the version ...
        SessionCache system = session.createSystemCache(false);
        mutable().removeChild(system, key);
        system.destroy(key);
        system.save();
    }

    private void addValuesNotInSet( JcrValue[] values,
                                    List<JcrValue> newValues,
                                    String versionUuid,
                                    Set<JcrValue> exceptIn ) throws RepositoryException {
        for (JcrValue value : values) {
            if (!versionUuid.equals(value.getString()) && !exceptIn.contains(value)) {
                exceptIn.add(value);
                newValues.add(value);
            }
        }
    }

    @Override
    public void addVersionLabel( String versionName,
                                 String label,
                                 boolean moveLabel ) throws VersionException, RepositoryException {
        AbstractJcrNode versionLabels = versionLabels();
        JcrVersionNode version = getVersion(versionName);

        try {
            // This throws a PNFE if the named property doesn't already exist
            versionLabels.getProperty(label);
            if (!moveLabel) throw new VersionException(JcrI18n.versionLabelAlreadyExists.text(label));

        } catch (PathNotFoundException pnfe) {
            // This gets thrown if the label doesn't already exist
        }

        // Use a separate system session to set the REFERENCE property on the 'nt:versionLabels' child node ...
        SessionCache system = session.createSystemCache(false);
        Property ref = session.propertyFactory().create(nameFrom(label), version.key());
        versionLabels.mutable().setProperty(system, ref);
        system.save();
    }

    @Override
    public void removeVersionLabel( String label ) throws VersionException, RepositoryException {
        AbstractJcrNode versionLabels = versionLabels();

        Name propName = null;
        try {
            // This throws a PNFE if the named property doesn't already exist
            propName = versionLabels.getProperty(label).name();
        } catch (PathNotFoundException pnfe) {
            // This gets thrown if the label doesn't already exist
            throw new VersionException(JcrI18n.invalidVersionLabel.text(label, getPath()));
        }

        // Use a separate system session to remove the REFERENCE property on the 'nt:versionLabels' child node ...
        SessionCache system = session.createSystemCache(false);
        versionLabels.mutable().removeProperty(system, propName);
        system.save();
    }

    @Override
    public NodeIterator getAllFrozenNodes() throws RepositoryException {
        return new FrozenNodeIterator(getAllVersions());
    }

    @Override
    public NodeIterator getAllLinearFrozenNodes() throws RepositoryException {
        return new FrozenNodeIterator(getAllLinearVersions());
    }

    @Override
    public VersionIterator getAllLinearVersions() throws RepositoryException {
        AbstractJcrNode existingNode = session().getNodeByIdentifier(getVersionableIdentifier());
        if (existingNode == null) return getAllVersions();

        assert existingNode.isNodeType(JcrMixLexicon.VERSIONABLE);

        LinkedList<JcrVersionNode> versions = new LinkedList<JcrVersionNode>();
        JcrVersionNode baseVersion = existingNode.getBaseVersion();
        while (baseVersion != null) {
            versions.addFirst(baseVersion);
            baseVersion = baseVersion.getLinearPredecessor();
        }

        return new LinearVersionIterator(versions, versions.size());
    }

    @Override
    public String getVersionableIdentifier() throws RepositoryException {
        // ModeShape uses a node's UUID as it's identifier
        return getVersionableUUID();
    }

    /**
     * Iterator over the versions within a version history. This class wraps the {@link JcrChildNodeIterator node iterator} for
     * all nodes of the {@link JcrVersionHistoryNode version history}, silently ignoring the {@code jcr:rootVersion} and
     * {@code jcr:versionLabels} children.
     */
    @NotThreadSafe
    static class JcrVersionIterator implements VersionIterator {

        private final NodeIterator nodeIterator;
        private Version next;
        private int position = 0;

        public JcrVersionIterator( NodeIterator nodeIterator ) {
            super();
            this.nodeIterator = nodeIterator;
        }

        @Override
        public Version nextVersion() {
            Version next = this.next;

            if (next != null) {
                this.next = null;
                return next;
            }

            next = nextVersionIfPossible();
            if (next == null) {
                throw new NoSuchElementException();
            }

            position++;
            return next;
        }

        private JcrVersionNode nextVersionIfPossible() {
            while (nodeIterator.hasNext()) {
                AbstractJcrNode node = (AbstractJcrNode)nodeIterator.nextNode();

                Name nodeName;
                try {
                    nodeName = node.segment().getName();
                } catch (RepositoryException re) {
                    throw new IllegalStateException(re);
                }

                if (!JcrLexicon.VERSION_LABELS.equals(nodeName)) {
                    return (JcrVersionNode)node;
                }
            }

            return null;
        }

        @Override
        public long getPosition() {
            return position;
        }

        @Override
        public long getSize() {
            // The number of version nodes is the number of child nodes of the version history - 1
            // (the jcr:versionLabels node)
            return nodeIterator.getSize() - 1;
        }

        @Override
        public void skip( long count ) {
            // Walk through the list to make sure that we don't accidentally count jcr:rootVersion or jcr:versionLabels as a
            // skipped node
            while (count-- > 0) {
                nextVersion();
            }
        }

        @Override
        public boolean hasNext() {
            if (this.next != null) return true;
            this.next = nextVersionIfPossible();
            return this.next != null;
        }

        @Override
        public Object next() {
            return nextVersion();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * An implementation of {@link VersionIterator} that iterates over a given set of versions. This differs from
     * {@link JcrVersionIterator} in that it expects an exact list of versions to iterate over whereas {@code JcrVersionIterator}
     * expects list of children for a {@code nt:versionHistory} node and filters out the label child.
     */
    @NotThreadSafe
    static class LinearVersionIterator implements VersionIterator {

        private final Iterator<? extends Version> versions;
        private final int size;
        private int pos;

        protected LinearVersionIterator( Iterable<? extends Version> versions,
                                         int size ) {
            this.versions = versions.iterator();
            this.size = size;
            this.pos = 0;
        }

        @Override
        public long getPosition() {
            return pos;
        }

        @Override
        public long getSize() {
            return this.size;
        }

        @Override
        public void skip( long skipNum ) {
            while (skipNum-- > 0 && versions.hasNext()) {
                versions.next();
                pos++;
            }

        }

        @Override
        public Version nextVersion() {
            return versions.next();
        }

        @Override
        public boolean hasNext() {
            return versions.hasNext();
        }

        @Override
        public Object next() {
            return nextVersion();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @NotThreadSafe
    static final class FrozenNodeIterator implements NodeIterator {
        private final VersionIterator versions;

        FrozenNodeIterator( VersionIterator versionIter ) {
            this.versions = versionIter;
        }

        @Override
        public boolean hasNext() {
            return versions.hasNext();
        }

        @Override
        public Object next() {
            return nextNode();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Node nextNode() {
            try {
                return versions.nextVersion().getFrozenNode();
            } catch (RepositoryException re) {
                // ModeShape doesn't throw a RepositoryException on getFrozenNode() from a valid version node
                throw new IllegalStateException(re);
            }
        }

        @Override
        public long getPosition() {
            return versions.getPosition();
        }

        @Override
        public long getSize() {
            return versions.getSize();
        }

        @Override
        public void skip( long skipNum ) {
            versions.skip(skipNum);
        }
    }
}
