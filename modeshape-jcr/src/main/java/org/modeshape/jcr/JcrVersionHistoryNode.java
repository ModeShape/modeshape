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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
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
import org.modeshape.graph.Graph;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Reference;
import org.modeshape.graph.property.Path.Segment;

/**
 * Convenience wrapper around a version history {@link JcrNode node}.
 */
class JcrVersionHistoryNode extends JcrNode implements VersionHistory {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    public JcrVersionHistoryNode( AbstractJcrNode node ) {
        super(node.cache, node.nodeId, node.location);

        assert !node.isRoot() : "Version histories should always be located in the /jcr:system/jcr:versionStorage subgraph";
    }

    /**
     * @return a reference to the {@code jcr:versionLabels} child node of this history node.
     * @throws RepositoryException if an error occurs accessing this node
     */
    private AbstractJcrNode versionLabels() throws RepositoryException {
        Segment segment = segmentFrom(JcrLexicon.VERSION_LABELS);
        return nodeInfo().getChild(segment).getPayload().getJcrNode();
    }

    /**
     * @{inheritDoc
     */
    @Override
    public VersionIterator getAllVersions() throws RepositoryException {
        return new JcrVersionIterator(getNodes());
    }

    /**
     * @{inheritDoc
     */
    @Override
    public JcrVersionNode getRootVersion() throws RepositoryException {
        // Copied from AbstractJcrNode.getNode(String) to avoid double conversion. Needs to be refactored.
        Segment segment = context().getValueFactories().getPathFactory().createSegment(JcrLexicon.ROOT_VERSION);
        try {
            return (JcrVersionNode)nodeInfo().getChild(segment).getPayload().getJcrNode();
        } catch (org.modeshape.graph.property.PathNotFoundException e) {
            String msg = JcrI18n.childNotFoundUnderNode.text(segment, getPath(), cache.workspaceName());
            throw new PathNotFoundException(msg);
        } catch (RepositorySourceException e) {
            throw new RepositoryException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * @{inheritDoc
     */
    @Override
    public JcrVersionNode getVersion( String versionName ) throws VersionException, RepositoryException {
        try {
            AbstractJcrNode version = getNode(versionName);
            return (JcrVersionNode)version;
        } catch (PathNotFoundException pnfe) {
            throw new VersionException(JcrI18n.invalidVersionName.text(versionName, getPath()));
        }
    }

    /**
     * @{inheritDoc
     */
    @Override
    public JcrVersionNode getVersionByLabel( String label ) throws VersionException, RepositoryException {
        Property prop = versionLabels().getProperty(label);
        if (prop == null) throw new VersionException(JcrI18n.invalidVersionLabel.text(label, getPath()));

        AbstractJcrNode version = session().getNodeByUUID(prop.getString());

        assert version != null;

        return (JcrVersionNode)version;
    }

    /**
     * @{inheritDoc
     */
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
     * @return the version labels for that version
     * @throws RepositoryException if an error occurs accessing the repository
     */
    @SuppressWarnings( "deprecation" )
    private Collection<String> versionLabelsFor( Version version ) throws RepositoryException {
        if (!version.getParent().equals(this)) {
            throw new VersionException(JcrI18n.invalidVersion.text(version.getPath(), getPath()));
        }

        String versionUuid = version.getUUID();

        PropertyIterator iter = versionLabels().getProperties();

        List<String> labels = new LinkedList<String>();
        for (int i = 0; iter.hasNext(); i++) {
            Property prop = iter.nextProperty();

            if (versionUuid.equals(prop.getString())) {
                labels.add(prop.getName());
            }
        }

        return labels;
    }

    /**
     * @{inheritDoc
     */
    @Override
    public String[] getVersionLabels( Version version ) throws RepositoryException {
        return versionLabelsFor(version).toArray(EMPTY_STRING_ARRAY);
    }

    /**
     * @{inheritDoc
     */
    @Override
    public String getVersionableUUID() throws RepositoryException {
        return getProperty(JcrLexicon.VERSIONABLE_UUID).getString();
    }

    /**
     * @{inheritDoc
     */
    @Override
    public boolean hasVersionLabel( String label ) throws RepositoryException {
        return versionLabels().hasProperty(label);
    }

    /**
     * @{inheritDoc
     */
    @Override
    public boolean hasVersionLabel( Version version,
                                    String label ) throws RepositoryException {
        Collection<String> labels = versionLabelsFor(version);

        return labels.contains(label);
    }

    /**
     * @{inheritDoc
     */
    @SuppressWarnings( "deprecation" )
    @Override
    public void removeVersion( String versionName )
        throws ReferentialIntegrityException, AccessDeniedException, UnsupportedRepositoryOperationException, VersionException,
        RepositoryException {

        JcrVersionNode version = getVersion(versionName);

        /*
         * Verify that the only references to this version are from its predecessors and successors in the version history.s
         */
        Path versionHistoryPath = version.path().getParent();
        for (PropertyIterator iter = version.getReferences(); iter.hasNext();) {
            AbstractJcrProperty prop = (AbstractJcrProperty)iter.next();

            Path nodePath = prop.path().getParent();

            // If the property's parent is the root node, fail.
            if (nodePath.isRoot()) {
                throw new ReferentialIntegrityException(JcrI18n.cannotRemoveVersion.text(prop.getPath()));
            }

            if (!versionHistoryPath.equals(nodePath.getParent())) {
                throw new ReferentialIntegrityException(JcrI18n.cannotRemoveVersion.text(prop.getPath()));
            }

        }

        String versionUuid = version.getUUID();
        Value[] values;

        // Remove the reference to the dead version from the successors property of all the predecessors
        Property predecessors = version.getProperty(JcrLexicon.PREDECESSORS);
        values = predecessors.getValues();
        for (int i = 0; i < values.length; i++) {
            AbstractJcrNode predecessor = session().getNodeByUUID(values[i].getString());
            Value[] nodeSuccessors = predecessor.getProperty(JcrLexicon.SUCCESSORS).getValues();
            Value[] newNodeSuccessors = new Value[nodeSuccessors.length - 1];

            int idx = 0;
            for (int j = 0; j < nodeSuccessors.length; j++) {
                if (!versionUuid.equals(nodeSuccessors[j].getString())) {
                    newNodeSuccessors[idx++] = nodeSuccessors[j];
                }
            }

            predecessor.editor().setProperty(JcrLexicon.SUCCESSORS, newNodeSuccessors, PropertyType.REFERENCE, false);
        }

        // Remove the reference to the dead version from the predecessors property of all the successors
        Property successors = version.getProperty(JcrLexicon.SUCCESSORS);
        values = successors.getValues();
        for (int i = 0; i < values.length; i++) {
            AbstractJcrNode successor = session().getNodeByUUID(values[i].getString());
            Value[] nodePredecessors = successor.getProperty(JcrLexicon.PREDECESSORS).getValues();
            Value[] newNodePredecessors = new Value[nodePredecessors.length - 1];

            int idx = 0;
            for (int j = 0; j < nodePredecessors.length; j++) {
                if (!versionUuid.equals(nodePredecessors[j].getString())) {
                    newNodePredecessors[idx++] = nodePredecessors[j];
                }
            }

            successor.editor().setProperty(JcrLexicon.PREDECESSORS, newNodePredecessors, PropertyType.REFERENCE, false);
        }

        session().recordRemoval(version.location); // do this first before we destroy the node!
        version.editor().destroy();
    }

    /**
     * @{inheritDoc
     */
    @SuppressWarnings( "deprecation" )
    @Override
    public void addVersionLabel( String versionName,
                                 String label,
                                 boolean moveLabel ) throws VersionException, RepositoryException {
        AbstractJcrNode versionLabels = versionLabels();
        Version version = getVersion(versionName);

        try {
            // This throws a PNFE if the named property doesn't already exist
            versionLabels.getProperty(label);
            if (!moveLabel) throw new VersionException(JcrI18n.versionLabelAlreadyExists.text(label));

        } catch (PathNotFoundException pnfe) {
            // This gets thrown if the label doesn't already exist
        }

        Graph graph = cache.session().repository().createSystemGraph(context());
        Reference ref = context().getValueFactories().getReferenceFactory().create(version.getUUID());
        graph.set(label).on(versionLabels.location).to(ref);

        versionLabels.refresh(false);

    }

    /**
     * @{inheritDoc
     */
    @Override
    public void removeVersionLabel( String label ) throws VersionException, RepositoryException {
        AbstractJcrNode versionLabels = versionLabels();

        try {
            // This throws a PNFE if the named property doesn't already exist
            versionLabels.getProperty(label);
        } catch (PathNotFoundException pnfe) {
            // This gets thrown if the label doesn't already exist
            throw new VersionException(JcrI18n.invalidVersionLabel.text(label, getPath()));
        }

        Graph graph = cache.session().repository().createSystemGraph(context());
        graph.remove(label).on(versionLabels.location).and();

        versionLabels.refresh(false);
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
     * all nodes of the {@link JcrVersionHistoryNode version history}, silently ignoring the {@code jcr:rootVersion} and {@code
     * jcr:versionLabels} children.
     */
    class JcrVersionIterator implements VersionIterator {

        private final NodeIterator nodeIterator;
        private Version next;
        private int position = 0;

        public JcrVersionIterator( NodeIterator nodeIterator ) {
            super();
            this.nodeIterator = nodeIterator;
        }

        /**
         * @{inheritDoc
         */
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

        /**
         * @{inheritDoc
         */
        @Override
        public long getPosition() {
            return position;
        }

        /**
         * @{inheritDoc
         */
        @Override
        public long getSize() {
            // The number of version nodes is the number of child nodes of the version history - 1
            // (the jcr:versionLabels node)
            return nodeIterator.getSize() - 1;
        }

        /**
         * @{inheritDoc
         */
        @Override
        public void skip( long count ) {
            // Walk through the list to make sure that we don't accidentally count jcr:rootVersion or jcr:versionLabels as a
            // skipped node
            while (count-- > 0) {
                nextVersion();
            }
        }

        /**
         * @{inheritDoc
         */
        @Override
        public boolean hasNext() {
            if (this.next != null) return true;

            this.next = nextVersionIfPossible();

            return this.next != null;
        }

        /**
         * @{inheritDoc
         */
        @Override
        public Object next() {
            return nextVersion();
        }

        /**
         * @{inheritDoc
         */
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
    class LinearVersionIterator implements VersionIterator {

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

    class FrozenNodeIterator implements NodeIterator {
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
