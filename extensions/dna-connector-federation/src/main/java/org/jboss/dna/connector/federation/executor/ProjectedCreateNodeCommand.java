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
package org.jboss.dna.connector.federation.executor;

import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.commands.CreateNodeCommand;
import org.jboss.dna.spi.graph.commands.NodeConflictBehavior;

/**
 * @author Randall Hauch
 */
public class ProjectedCreateNodeCommand extends ActsOnProjectedPathCommand<CreateNodeCommand> implements CreateNodeCommand {

    public ProjectedCreateNodeCommand( CreateNodeCommand delegate,
                                       Path projectedPath ) {
        super(delegate, projectedPath);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.CreateNodeCommand#getConflictBehavior()
     */
    public NodeConflictBehavior getConflictBehavior() {
        return getOriginalCommand().getConflictBehavior();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.CreateNodeCommand#getPropertyIterator()
     */
    public Iterable<Property> getPropertyIterator() {
        return getOriginalCommand().getPropertyIterator();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( CreateNodeCommand that ) {
        if (this == that) return 0;
        return this.getPath().compareTo(that.getPath());
    }

}
