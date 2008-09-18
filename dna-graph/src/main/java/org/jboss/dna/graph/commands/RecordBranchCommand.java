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
package org.jboss.dna.graph.commands;

import java.util.Iterator;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.Property;

/**
 * Command that records the structure of a branch. To process this command, the recipient should walk the branch rooted at
 * {@link ActsOnPath#getPath()} and, for each node in the branch, {@link #record(Path, Iterable)} the node's information. If
 * {@link #record(Path, Iterable)} returns true, then the children of that node should also be recorded; if false, then the
 * recording the children of that node can be ignored.
 * 
 * @author Randall Hauch
 */
public interface RecordBranchCommand extends GraphCommand, ActsOnPath {

    /**
     * Sets the properties of the supplied node.
     * <p>
     * If the supplied path is a relative path, it is assumed to be relative to the {@link ActsOnPath#getPath() branch root}. If
     * the supplied path is an absolute path, it must be a {@link Path#isDecendantOf(Path) decendant} of the
     * {@link ActsOnPath#getPath() branch root}; if not, this method returns false and ignores the call.
     * </p>
     * <p>
     * This method should not be called multiple times with the same path. The behavior for such cases is not defined.
     * </p>
     * 
     * @param path the path for the node; may not be null
     * @param properties the properties for the node; may be null if there are no properties
     * @return true if the children of the node should be recorded, or false if this new node is as deep as the recording should
     *         go
     */
    boolean record( Path path,
                    Iterable<Property> properties );

    /**
     * Sets the properties of the supplied node.
     * <p>
     * If the supplied path is a relative path, it is assumed to be relative to the {@link ActsOnPath#getPath() branch root}. If
     * the supplied path is an absolute path, it must be a {@link Path#isDecendantOf(Path) decendant} of the
     * {@link ActsOnPath#getPath() branch root}; if not, this method returns false and ignores the call.
     * </p>
     * <p>
     * This method should not be called multiple times with the same path. The behavior for such cases is not defined.
     * </p>
     * 
     * @param path the path for the node; may not be null
     * @param properties the properties for the node; may be null if there are no properties
     * @return true if the children of the node should be recorded, or false if this new node is as deep as the recording should
     *         go
     */
    boolean record( Path path,
                    Iterator<Property> properties );

    /**
     * Sets the properties of the supplied node.
     * <p>
     * If the supplied path is a relative path, it is assumed to be relative to the {@link ActsOnPath#getPath() branch root}. If
     * the supplied path is an absolute path, it must be a {@link Path#isDecendantOf(Path) decendant} of the
     * {@link ActsOnPath#getPath() branch root}; if not, this method returns false and ignores the call.
     * </p>
     * <p>
     * This method should not be called multiple times with the same path. The behavior for such cases is not defined.
     * </p>
     * 
     * @param path the path for the node; may not be null
     * @param properties the properties for the node; may be null if there are no properties
     * @return true if the children of the node should be recorded, or false if this new node is as deep as the recording should
     *         go
     */
    boolean record( Path path,
                    Property... properties );

}
