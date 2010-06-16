/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
* See the AUTHORS.txt file in the distribution for a full listing of 
* individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.graph.sequencer;

import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;

/**
 * Interface for sequencers to use to generate their output.
 */
public interface SequencerOutput {

    /**
     * Set the supplied property on the supplied node.
     * <p>
     * The {@link StreamSequencerContext#getValueFactories() value factories} should be used to create paths, names, and values.
     * These factories can be used to create new values or convert values from one property type to another. (Note that each of
     * the factories have methods that create values from all of the property types.)
     * </p>
     * <p>
     * This method is provided as a convenience, but it identical to creating a {@link Path} and {@link Name} using the
     * {@link StreamSequencerContext#getValueFactories() factories} and calling {@link #setProperty(Path, Name, Object...)}.
     * </p>
     * 
     * @param nodePath the path to the node containing the property; may not be null
     * @param propertyName the name of the property to be set
     * @param values the value(s) for the property; may be empty if any existing property is to be removed
     * @deprecated As of ModeShape 2.0, the preferred approach is to use {@link #setProperty(Path, Name, Object...)}, which
     *             properly addresses the session having different namespace mappings. This method depends on the namespace
     *             mappings for the given URIs in the name components of the nodePath and propertyName to be mapped in the
     *             NamespaceRegistry of the ModeShapeEngine's (or JcrEngine's) ExecutionContext.
     */
    @Deprecated
    void setProperty( String nodePath,
                      String propertyName,
                      Object... values );

    /**
     * Set the supplied reference on the supplied node.
     * <p>
     * This method is provided as a convenience, but it identical to creating a {@link Path} and {@link Name} using the
     * {@link StreamSequencerContext#getValueFactories() factories} and calling {@link #setProperty(Path, Name, Object...)}.
     * </p>
     * 
     * @param nodePath the path to the node containing the property; may not be null
     * @param propertyName the name of the property to be set
     * @param paths the paths to the referenced property, which may be absolute paths or relative to the sequencer output node;
     *        may be empty if any existing property is to be removed
     * @deprecated As of ModeShape 2.0, the preferred approach is to use {@link #setProperty(Path, Name, Object...)}, which
     *             properly addresses the session having different namespace mappings. This method depends on the namespace
     *             mappings for the given URIs in the name components of the nodePath and propertyName to be mapped in the
     *             NamespaceRegistry of the ModeShapeEngine's (or JcrEngine's) ExecutionContext.
     */
    @Deprecated
    void setReference( String nodePath,
                       String propertyName,
                       String... paths );

    /**
     * Set the supplied property on the supplied node.
     * <p>
     * The {@link StreamSequencerContext#getValueFactories() value factories} should be used to create paths, names, and values.
     * These factories can be used to create new values or convert values from one property type to another. (Note that each of
     * the factories have methods that create values from all of the property types.)
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
