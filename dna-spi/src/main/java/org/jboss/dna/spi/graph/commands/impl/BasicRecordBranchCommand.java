/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.spi.graph.commands.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.commands.CreateNodeCommand;
import org.jboss.dna.spi.graph.commands.NodeConflictBehavior;
import org.jboss.dna.spi.graph.commands.RecordBranchCommand;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class BasicRecordBranchCommand extends BasicGraphCommand implements RecordBranchCommand {

    private final int maxDepth;
    private final Path topOfBranch;
    private final List<CreateNodeCommand> commands;
    private final NodeConflictBehavior conflictBehavior;

    /**
     * @param topOfBranch the top node in the branch that is to be recorded; may not be null
     * @param conflictBehavior the desired behavior when a node exists at the <code>path</code>; may not be null
     */
    public BasicRecordBranchCommand( Path topOfBranch,
                                     NodeConflictBehavior conflictBehavior ) {
        this(topOfBranch, conflictBehavior, null, Integer.MAX_VALUE);
    }

    /**
     * @param topOfBranch the top node in the branch that is to be recorded; may not be null
     * @param conflictBehavior the desired behavior when a node exists at the <code>path</code>; may not be null
     * @param commands the list into which the {@link CreateNodeCommand}s should be place; may be null
     */
    public BasicRecordBranchCommand( Path topOfBranch,
                                     NodeConflictBehavior conflictBehavior,
                                     List<CreateNodeCommand> commands ) {
        this(topOfBranch, conflictBehavior, commands, Integer.MAX_VALUE);
    }

    /**
     * @param topOfBranch the top node in the branch that is to be recorded; may not be null
     * @param conflictBehavior the desired behavior when a node exists at the <code>path</code>; may not be null
     * @param commands the list into which the {@link CreateNodeCommand}s should be place; may be null
     * @param maxDepthOfBranch the maximum depth of the branch that should be recorded, or {@link Integer#MAX_VALUE} if there is
     *        no limit
     */
    public BasicRecordBranchCommand( Path topOfBranch,
                                     NodeConflictBehavior conflictBehavior,
                                     List<CreateNodeCommand> commands,
                                     int maxDepthOfBranch ) {
        super();
        assert topOfBranch != null;
        assert conflictBehavior != null;
        assert maxDepthOfBranch > 0;
        this.topOfBranch = topOfBranch;
        this.maxDepth = maxDepthOfBranch;
        this.conflictBehavior = conflictBehavior;
        this.commands = commands != null ? commands : new LinkedList<CreateNodeCommand>();
    }

    /**
     * {@inheritDoc}
     */
    public Path getPath() {
        return topOfBranch;
    }

    /**
     * {@inheritDoc}
     */
    public boolean record( Path path,
                           Iterable<Property> properties ) {
        // Determine the relative path for this node ...
        if (path.isAbsolute()) {
            if (!path.isDecendantOf(this.topOfBranch)) return false;
            path = path.relativeTo(this.topOfBranch);
        }
        int numberOfLevelsLeft = maxDepth - path.size();
        if (numberOfLevelsLeft < 0) return false;
        List<Property> propertyList = Collections.emptyList();
        if (properties != null) {
            if (properties instanceof List) {
                propertyList = (List<Property>)properties;
            } else {
                propertyList = new LinkedList<Property>();
                for (Property property : properties) {
                    propertyList.add(property);
                }
            }
        }
        record(new BasicCreateNodeCommand(path, propertyList, this.conflictBehavior));
        return numberOfLevelsLeft > 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean record( Path path,
                           Iterator<Property> properties ) {
        int numberOfLevelsLeft = maxDepth - path.size();
        if (numberOfLevelsLeft < 0) return false;
        List<Property> propertyList = Collections.emptyList();
        if (properties != null) {
            propertyList = new LinkedList<Property>();
            while (properties.hasNext()) {
                Property property = properties.next();
                propertyList.add(property);
            }
        }
        record(new BasicCreateNodeCommand(path, propertyList, this.conflictBehavior));
        return numberOfLevelsLeft > 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean record( Path path,
                           Property... properties ) {
        int numberOfLevelsLeft = maxDepth - path.size();
        if (numberOfLevelsLeft < 0) return false;
        List<Property> propertyList = Collections.emptyList();
        if (properties != null) {
            propertyList = new ArrayList<Property>(properties.length);
            for (Property property : properties) {
                propertyList.add(property);
            }
        }
        record(new BasicCreateNodeCommand(path, propertyList, this.conflictBehavior));
        return numberOfLevelsLeft > 0;
    }

    /**
     * @return maxDepth
     */
    public int getMaxDepth() {
        return this.maxDepth;
    }

    /**
     * Method that is called whenver a node is recorded by the recipient of this command. This implementation simply records it,
     * but subclasses can override this method to respond immediately.
     * 
     * @param command the command containing the node information; never null and always above the {@link #getMaxDepth() maximum
     *        depth}.
     */
    protected void record( CreateNodeCommand command ) {
        commands.add(command);
    }

    /**
     * Return the commands to create the nodes, in the order they were recorded.
     * 
     * @return the mutable list of {@link CreateNodeCommand create node commands}; never null
     */
    public List<CreateNodeCommand> getCreateNodeCommands() {
        return this.commands;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " at " + this.getPath();
    }

}
