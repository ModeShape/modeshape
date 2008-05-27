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
package org.jboss.dna.spi.connector.commands;

import java.util.Iterator;
import org.jboss.dna.spi.cache.Cacheable;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Property;

/**
 * A command to get the children of a single node given its path.
 * @author Randall Hauch
 */
public interface CreateNodeCommand extends RepositoryCommand, ActsOnPath, Cacheable, ActsOnProperties {

    /**
     * Get the names of the children for this new node. The recipient of the command should {@link Iterator#remove() remove} any
     * child name that will not be stored.
     * @return the names of the node's children; never null, but possibly empty
     */
    Iterator<Name> getChildren();

    /**
     * Get the properties for this new node. The recipient of the command should {@link Iterator#remove() remove} any property
     * that will not be stored.
     * @return the property iterator; never null, but possibly empty
     */
    Iterator<Property> getProperties();
}
