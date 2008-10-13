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
package org.jboss.dna.graph.requests;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connectors.RepositoryConnection;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.Property;

/**
 * Instruction to read all of the children of a node at a specific location.
 * 
 * @author Randall Hauch
 */
public class ReadAllChildrenRequest extends Request implements Iterable<Location> {

    private final Location of;
    private final List<Location> children = new LinkedList<Location>();

    /**
     * Create a request to read the children of a node at the supplied location.
     * 
     * @param of the location of the node whose children are to be read
     * @throws IllegalArgumentException if the location is null
     */
    public ReadAllChildrenRequest( Location of ) {
        CheckArg.isNotNull(of, "of");
        this.of = of;
    }

    /**
     * Get the location defining the node whose children are to be read.
     * 
     * @return the location of the parent node; never null
     */
    public Location of() {
        return of;
    }

    /**
     * Get the children that were read from the {@link RepositoryConnection} after the request was processed. Each child is
     * represented by a location.
     * 
     * @return the children that were read; never null
     */
    public List<Location> getChildren() {
        return children;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<Location> iterator() {
        return children.iterator();
    }

    /**
     * Add to the list of children that has been read the child with the given path and identification properties. The children
     * should be added in order.
     * 
     * @param child the location of the child that was read
     * @throws IllegalArgumentException if the location is null
     * @see #addChild(Path, Property)
     * @see #addChild(Path, Property, Property...)
     */
    public void addChild( Location child ) {
        CheckArg.isNotNull(child, "child");
        this.children.add(child);
    }

    /**
     * Add to the list of children that has been read the child with the given path and identification properties. The children
     * should be added in order.
     * 
     * @param pathToChild the path of the child that was just read
     * @param firstIdProperty the first identification property of the child that was just read
     * @param remainingIdProperties the remaining identification properties of the child that was just read
     * @throws IllegalArgumentException if the path or identification properties are null
     * @see #addChild(Location)
     * @see #addChild(Path, Property)
     */
    public void addChild( Path pathToChild,
                          Property firstIdProperty,
                          Property... remainingIdProperties ) {
        Location child = new Location(pathToChild, firstIdProperty, remainingIdProperties);
        this.children.add(child);
    }

    /**
     * Add to the list of children that has been read the child with the given path and identification property. The children
     * should be added in order.
     * 
     * @param pathToChild the path of the child that was just read
     * @param idProperty the identification property of the child that was just read
     * @throws IllegalArgumentException if the path or identification properties are null
     * @see #addChild(Location)
     * @see #addChild(Path, Property, Property...)
     */
    public void addChild( Path pathToChild,
                          Property idProperty ) {
        Location child = new Location(pathToChild, idProperty);
        this.children.add(child);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (this.getClass().isInstance(obj)) {
            ReadAllChildrenRequest that = (ReadAllChildrenRequest)obj;
            if (!this.of().equals(that.of())) return false;
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
        return "read children of " + of();
    }

}
