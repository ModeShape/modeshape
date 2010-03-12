package org.modeshape.jcr;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.version.Version;
import org.modeshape.graph.property.Name;

public class JcrVersionNode extends JcrNode implements Version {

    private static final Version[] EMPTY_VERSION_ARRAY = new Version[0];

    public JcrVersionNode( AbstractJcrNode node ) {
        super(node.cache, node.nodeId, node.location);

        assert !node.isRoot() : "Versions should always be located in the /jcr:system/jcr:versionStorage subgraph";
    }

    @Override
    public JcrVersionHistoryNode getContainingHistory() throws RepositoryException {
        return new JcrVersionHistoryNode(getParent());
    }

    @Override
    public Calendar getCreated() throws RepositoryException {
        return getProperty(JcrLexicon.CREATED).getDate();
    }

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

    private final Version[] getNodesForProperty(Name propertyName) throws RepositoryException {
        assert JcrLexicon.SUCCESSORS.equals(propertyName) || JcrLexicon.PREDECESSORS.equals(propertyName);
        
        Property references = getProperty(propertyName);
        
        if (references == null) return EMPTY_VERSION_ARRAY;
        
        Value[] values = references.getValues();
        
        List<JcrVersionNode> versions = new ArrayList<JcrVersionNode>(values.length);
        
        for (int i = 0; i < values.length; i++) {
            String uuid = values[i].getString();
            
            AbstractJcrNode node = session().getNodeByUUID(uuid);
            versions.add(new JcrVersionNode(node));
        }
        
        return versions.toArray(EMPTY_VERSION_ARRAY);
        
    }
}
