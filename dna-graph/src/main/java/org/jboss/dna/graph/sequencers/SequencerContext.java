/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.graph.sequencers;

import java.util.Set;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.Property;

/**
 * @author John Verhaeg
 */
public interface SequencerContext extends ExecutionContext {

    /**
     * Return the path of the input node containing the content being sequenced.
     * 
     * @return input node's path.
     */
    Path getInputPath();

    /**
     * Return the set of properties from the input node containing the content being sequenced.
     * 
     * @return the input node's properties; never <code>null</code>.
     */
    Set<Property> getInputProperties();

    /**
     * Return the property with the supplied name from the input node containing the content being sequenced.
     * 
     * @param name
     * @return the input node property, or <code>null</code> if none exists.
     */
    Property getInputProperty( Name name );

    /**
     * Return the MIME-type of the content being sequenced.
     * 
     * @return the MIME-type
     */
    String getMimeType();

    /**
     * Get an interface that can be used to record various problems, warnings, and errors that are not extreme enough to warrant
     * throwing exceptions.
     * 
     * @return the interface for recording problems; never null
     */
    Problems getProblems();
}
