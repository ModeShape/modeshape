/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (under your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.request;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.HashCode;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.NodeConflictBehavior;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;

/**
 * Instruction to create the node under the specified location. This command will create the node and set the initial properties.
 */
public class CreateNodeRequest extends ChangeRequest implements Iterable<Property> {

    private static final long serialVersionUID = 1L;

    public static final NodeConflictBehavior DEFAULT_CONFLICT_BEHAVIOR = NodeConflictBehavior.APPEND;

    private final Location under;
    private final String workspaceName;
    private final Name childName;
    private final List<Property> properties;
    private final NodeConflictBehavior conflictBehavior;
    private Location actualLocation;

    /**
     * Create a request to create a node with the given properties under the supplied location.
     * 
     * @param parentLocation the location of the existing parent node, under which the new child should be created
     * @param workspaceName the name of the workspace containing the parent
     * @param childName the name of the new child to create under the existing parent
     * @param properties the properties of the new node, which should include any {@link Location#getIdProperties() identification
     *        properties} for the new node
     * @throws IllegalArgumentException if the location, workspace name, or child name is null
     */
    public CreateNodeRequest( Location parentLocation,
                              String workspaceName,
                              Name childName,
                              Property... properties ) {
        this(parentLocation, workspaceName, childName, DEFAULT_CONFLICT_BEHAVIOR, properties);
    }

    /**
     * Create a request to create a node with the given properties under the supplied location.
     * 
     * @param parentLocation the location of the existing parent node, under which the new child should be created
     * @param workspaceName the name of the workspace containing the parent
     * @param childName the name of the new child to create under the existing parent
     * @param properties the properties of the new node, which should include any {@link Location#getIdProperties() identification
     *        properties} for the new node
     * @throws IllegalArgumentException if the location, workspace name, or child name is null
     */
    public CreateNodeRequest( Location parentLocation,
                              String workspaceName,
                              Name childName,
                              Iterable<Property> properties ) {
        this(parentLocation, workspaceName, childName, DEFAULT_CONFLICT_BEHAVIOR, properties);
    }

    /**
     * Create a request to create a node with the given properties under the supplied location.
     * 
     * @param parentLocation the location of the existing parent node, under which the new child should be created
     * @param workspaceName the name of the workspace containing the parent
     * @param childName the name of the new child to create under the existing parent
     * @param properties the properties of the new node, which should include any {@link Location#getIdProperties() identification
     *        properties} for the new node
     * @throws IllegalArgumentException if the location, workspace name, or child name is null
     */
    public CreateNodeRequest( Location parentLocation,
                              String workspaceName,
                              Name childName,
                              Iterator<Property> properties ) {
        this(parentLocation, workspaceName, childName, DEFAULT_CONFLICT_BEHAVIOR, properties);
    }

    /**
     * Create a request to create a node with the given properties under the supplied location.
     * 
     * @param parentLocation the location of the existing parent node, under which the new child should be created
     * @param workspaceName the name of the workspace containing the parent
     * @param childName the name of the new child to create under the existing parent
     * @param properties the properties of the new node, which should include any {@link Location#getIdProperties() identification
     *        properties} for the new node
     * @param conflictBehavior the expected behavior if an equivalently-named child already exists under the <code>into</code>
     *        location
     * @throws IllegalArgumentException if the location, workspace name, child name, or the conflict behavior is null
     */
    public CreateNodeRequest( Location parentLocation,
                              String workspaceName,
                              Name childName,
                              NodeConflictBehavior conflictBehavior,
                              Property... properties ) {
        CheckArg.isNotNull(parentLocation, "parentLocation");
        CheckArg.isNotNull(workspaceName, "workspaceName");
        CheckArg.isNotNull(conflictBehavior, "conflictBehavior");
        CheckArg.isNotNull(childName, "childName");
        this.under = parentLocation;
        this.workspaceName = workspaceName;
        this.childName = childName;
        this.conflictBehavior = conflictBehavior;
        int number = properties.length + (under.hasIdProperties() ? under.getIdProperties().size() : 0);
        List<Property> props = new ArrayList<Property>(number);
        for (Property property : properties) {
            if (property != null) props.add(property);
        }
        this.properties = Collections.unmodifiableList(props);
    }

    /**
     * Create a request to create a node with the given properties under the supplied location.
     * 
     * @param parentLocation the location of the existing parent node, under which the new child should be created
     * @param workspaceName the name of the workspace containing the parent
     * @param childName the name of the new child to create under the existing parent
     * @param properties the properties of the new node, which should include any {@link Location#getIdProperties() identification
     *        properties} for the new node
     * @param conflictBehavior the expected behavior if an equivalently-named child already exists under the <code>into</code>
     *        location
     * @throws IllegalArgumentException if the location, workspace name, child name, or the conflict behavior is null
     */
    public CreateNodeRequest( Location parentLocation,
                              String workspaceName,
                              Name childName,
                              NodeConflictBehavior conflictBehavior,
                              Iterable<Property> properties ) {
        CheckArg.isNotNull(parentLocation, "parentLocation");
        CheckArg.isNotNull(workspaceName, "workspaceName");
        CheckArg.isNotNull(conflictBehavior, "conflictBehavior");
        CheckArg.isNotNull(childName, "childName");
        this.under = parentLocation;
        this.workspaceName = workspaceName;
        this.childName = childName;
        this.conflictBehavior = conflictBehavior;
        List<Property> props = new LinkedList<Property>();
        for (Property property : properties) {
            if (property != null) props.add(property);
        }
        this.properties = Collections.unmodifiableList(props);
    }

    /**
     * Create a request to create a node with the given properties under the supplied location.
     * 
     * @param parentLocation the location of the existing parent node, under which the new child should be created
     * @param workspaceName the name of the workspace containing the parent
     * @param childName the name of the new child to create under the existing parent
     * @param properties the properties of the new node, which should include any {@link Location#getIdProperties() identification
     *        properties} for the new node
     * @param conflictBehavior the expected behavior if an equivalently-named child already exists under the <code>into</code>
     *        location
     * @throws IllegalArgumentException if the location, workspace name, child name, or the conflict behavior is null
     */
    public CreateNodeRequest( Location parentLocation,
                              String workspaceName,
                              Name childName,
                              NodeConflictBehavior conflictBehavior,
                              Iterator<Property> properties ) {
        CheckArg.isNotNull(parentLocation, "parentLocation");
        CheckArg.isNotNull(workspaceName, "workspaceName");
        CheckArg.isNotNull(conflictBehavior, "conflictBehavior");
        CheckArg.isNotNull(childName, "childName");
        this.under = parentLocation;
        this.workspaceName = workspaceName;
        this.childName = childName;
        this.conflictBehavior = conflictBehavior;
        List<Property> props = new LinkedList<Property>();
        while (properties.hasNext()) {
            Property property = properties.next();
            if (property != null) props.add(property);
        }
        this.properties = Collections.unmodifiableList(props);
    }

    /**
     * Get the location defining the parent of the new node that is to be created.
     * 
     * @return the location of the parent node; never null
     */
    public Location under() {
        return under;
    }

    /**
     * Get the name of the workspace in which the node is to be createde
     * 
     * @return the name of the workspace; never null
     */
    public String inWorkspace() {
        return workspaceName;
    }

    /**
     * Get the name for the new child.
     * 
     * @return the child's name; never null
     */
    public Name named() {
        return childName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<Property> iterator() {
        return this.properties.iterator();
    }

    /**
     * Get the properties for the node. If the node's {@link #under() location} has identification properties, the resulting
     * properties will include the {@link Location#getIdProperties() identification properties}.
     * 
     * @return the collection of properties; never null
     */
    public Collection<Property> properties() {
        return properties;
    }

    /**
     * Get the expected behavior when copying the branch and the {@link #under() destination} already has a node with the same
     * name.
     * 
     * @return the behavior specification
     */
    public NodeConflictBehavior conflictBehavior() {
        return conflictBehavior;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.Request#isReadOnly()
     */
    @Override
    public boolean isReadOnly() {
        return false;
    }

    /**
     * Sets the actual and complete location of the node being created. This method must be called when processing the request,
     * and the actual location must have a {@link Location#getPath() path}.
     * 
     * @param actual the actual location of the node being created, or null if the {@link #under() current location} should be
     *        used
     * @throws IllegalArgumentException if the actual location does not represent the {@link Location#isSame(Location) same
     *         location} as the {@link #under() current location}, or if the actual location does not have a path.
     * @throws IllegalStateException if the request is frozen
     */
    public void setActualLocationOfNode( Location actual ) {
        checkNotFrozen();
        CheckArg.isNotNull(actual, "actual");
        if (!under.equals(actual, false)) { // not same if actual is null
        }
        assert actual != null;
        if (!actual.hasPath()) {
            throw new IllegalArgumentException(GraphI18n.actualLocationMustHavePath.text(actual));
        }
        assert actual.hasPath();
        if (under.hasPath() && !under.getPath().equals(actual.getPath().getParent())) {
            throw new IllegalArgumentException(GraphI18n.actualLocationNotEqualToInputLocation.text(actual, under));
        }
        this.actualLocation = actual;
    }

    /**
     * Get the actual location of the node that was created.
     * 
     * @return the actual location, or null if the actual location was not set
     */
    public Location getActualLocationOfNode() {
        return actualLocation;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.ChangeRequest#changes(java.lang.String, org.jboss.dna.graph.property.Path)
     */
    @Override
    public boolean changes( String workspace,
                            Path path ) {
        return this.workspaceName.equals(workspace) && under.hasPath() && under.getPath().isAtOrBelow(path);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.ChangeRequest#changedLocation()
     */
    @Override
    public Location changedLocation() {
        return actualLocation != null ? actualLocation : under;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.ChangeRequest#changedWorkspace()
     */
    @Override
    public String changedWorkspace() {
        return workspaceName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.Request#cancel()
     */
    @Override
    public void cancel() {
        super.cancel();
        this.actualLocation = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(under, childName, workspaceName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (this.getClass().isInstance(obj)) {
            CreateNodeRequest that = (CreateNodeRequest)obj;
            if (!this.under().isSame(that.under())) return false;
            if (!this.conflictBehavior().equals(that.conflictBehavior())) return false;
            if (!this.inWorkspace().equals(that.conflictBehavior())) return false;
            if (!this.properties().equals(that.properties())) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String parent = under() + "/";
        if (under.hasPath() && under.getPath().isRoot()) parent = "/";
        return "create in the \"" + workspaceName + "\" workspace the node \"" + parent + childName + "\" with properties "
               + properties();
    }

}
