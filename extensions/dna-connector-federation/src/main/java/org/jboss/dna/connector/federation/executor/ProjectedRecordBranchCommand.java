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

import java.util.Iterator;
import org.jboss.dna.graph.commands.RecordBranchCommand;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.Property;

/**
 * @author Randall Hauch
 */
public class ProjectedRecordBranchCommand extends ActsOnProjectedPathCommand<RecordBranchCommand> implements RecordBranchCommand {

    public ProjectedRecordBranchCommand( RecordBranchCommand delegate,
                                         Path projectedPath ) {
        super(delegate, projectedPath);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.commands.RecordBranchCommand#record(org.jboss.dna.graph.properties.Path, java.lang.Iterable)
     */
    public boolean record( Path path,
                           Iterable<Property> properties ) {
        path = relativePathFrom(path);
        return getOriginalCommand().record(path, properties);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.commands.RecordBranchCommand#record(org.jboss.dna.graph.properties.Path, java.util.Iterator)
     */
    public boolean record( Path path,
                           Iterator<Property> properties ) {
        path = relativePathFrom(path);
        return getOriginalCommand().record(path, properties);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.commands.RecordBranchCommand#record(org.jboss.dna.graph.properties.Path,
     *      org.jboss.dna.graph.properties.Property[])
     */
    public boolean record( Path path,
                           Property... properties ) {
        path = relativePathFrom(path);
        return getOriginalCommand().record(path, properties);
    }

    protected Path relativePathFrom( Path path ) {
        if (path == null) return null;
        if (path.isAbsolute()) {
            // The path is absolute, so compute the relative path w/r/t the new path ...
            path = path.relativeTo(this.getPath());
        }
        return path;
    }

}
