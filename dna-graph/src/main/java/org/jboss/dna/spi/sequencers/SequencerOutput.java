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
package org.jboss.dna.spi.sequencers;

import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.NamespaceRegistry;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.ValueFactories;

/**
 * Interface for sequencers to use to generate their output.
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 */
public interface SequencerOutput {

    /**
     * Get the factories that can be used to create {@link Path paths} and other property values.
     * 
     * @return the collection of factories; never null
     * @deprecated Replaced by {@link SequencerContext#getFactories()}.
     */
    @Deprecated
    ValueFactories getFactories();

    /**
     * Convenience method to get the namespace registry used by the {@link ValueFactories#getNameFactory() name value factory}.
     * 
     * @return the namespace registry; never <code>null</code>
     * @deprecated Replaced by {@link SequencerContext#getNamespaceRegistry()}.
     */
    @Deprecated
    NamespaceRegistry getNamespaceRegistry();

    /**
     * Set the supplied property on the supplied node.
     * <p>
     * The {@link #getFactories() value factories} should be used to create paths, names, and values. These factories can be used
     * to create new values or convert values from one property type to another. (Note that each of the factories have methods
     * that create values from all of the property types.)
     * </p>
     * <p>
     * This method is provided as a convenience, but it identical to creating a {@link Path} and {@link Name} using the
     * {@link #getFactories() factories} and calling {@link #setProperty(Path, Name, Object...)}.
     * </p>
     * 
     * @param nodePath the path to the node containing the property; may not be null
     * @param propertyName the name of the property to be set
     * @param values the value(s) for the property; may be empty if any existing property is to be removed
     */
    void setProperty( String nodePath,
                      String propertyName,
                      Object... values );

    /**
     * Set the supplied reference on the supplied node.
     * <p>
     * This method is provided as a convenience, but it identical to creating a {@link Path} and {@link Name} using the
     * {@link #getFactories() factories} and calling {@link #setProperty(Path, Name, Object...)}.
     * </p>
     * 
     * @param nodePath the path to the node containing the property; may not be null
     * @param propertyName the name of the property to be set
     * @param paths the paths to the referenced property, which may be absolute paths or relative to the sequencer output node;
     *        may be empty if any existing property is to be removed
     */
    void setReference( String nodePath,
                       String propertyName,
                       String... paths );

    /**
     * Set the supplied property on the supplied node.
     * <p>
     * The {@link #getFactories() value factories} should be used to create paths, names, and values. These factories can be used
     * to create new values or convert values from one property type to another. (Note that each of the factories have methods
     * that create values from all of the property types.)
     * </p>
     * 
     * @param nodePath the path to the node containing the property; may not be null
     * @param propertyName the name of the property to be set
     * @param values the value(s) for the property; may be empty if any existing property is to be removed
     */
    void setProperty( Path nodePath,
                      Name propertyName,
                      Object... values );
}
