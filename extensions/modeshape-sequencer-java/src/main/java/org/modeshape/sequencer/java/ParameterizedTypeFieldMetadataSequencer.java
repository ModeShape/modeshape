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
package org.modeshape.sequencer.java;

import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.sequencer.java.metadata.ModifierMetadata;
import org.modeshape.sequencer.java.metadata.ParameterizedTypeFieldMetadata;

/**
 * Sequencer for all paths of a {@link ParameterizedTypeFieldMetadata}.
 */
public class ParameterizedTypeFieldMetadataSequencer {

    /**
     * Create the root path for all path children of a parameterized type.
     * 
     * @param pathFactory the path factory to use
     * @param parameterizedIndex - index in case of multiple paths.
     * @return a path with a index starting by 1.
     */
    public static Path getParameterizedTypeFieldRootPath( PathFactory pathFactory,
                                                          int parameterizedIndex ) {
        Path basePath = pathFactory.createRelativePath(JavaMetadataLexicon.COMPILATION_UNIT_NODE,
                                                       JavaMetadataLexicon.UNIT_TYPE_CHILD_NODE,
                                                       JavaMetadataLexicon.CLASS_DECLARATION_CHILD_NODE,
                                                       JavaMetadataLexicon.NORMAL_CLASS_CHILD_NODE,
                                                       JavaMetadataLexicon.NORMAL_CLASS_DECLARATION_CHILD_NODE,
                                                       JavaMetadataLexicon.FIELD_CHILD_NODE,
                                                       JavaMetadataLexicon.FIELD_TYPE_CHILD_NODE,
                                                       JavaMetadataLexicon.TYPE_CHILD_NODE);
        return pathFactory.create(basePath, pathFactory.createSegment(JavaMetadataLexicon.PARAMETERIZED_TYPE_MODIFIER_CHILD_NODE,
                                                                      parameterizedIndex));
    }

    /**
     * Sequences the type name of the parameterized type.
     * 
     * @param parameterizedTypeFieldMetadata - the meta data.
     * @param parameterizedTypeFieldRootPath - the root path of a parameterized type.
     * @param output - the {@link SequencerOutput}.
     * @param pathFactory - the {@link PathFactory}.
     * @param nameFactory - the {@link NameFactory}.
     */
    public static void sequenceTheParameterizedTypeName( ParameterizedTypeFieldMetadata parameterizedTypeFieldMetadata,
                                                         Path parameterizedTypeFieldRootPath,
                                                         PathFactory pathFactory,
                                                         NameFactory nameFactory,
                                                         SequencerOutput output ) {
        output.setProperty(parameterizedTypeFieldRootPath,
                           JavaMetadataLexicon.PARAMETERIZED_TYPE_NAME,
                           parameterizedTypeFieldMetadata.getType());
    }

    /**
     * Create a path for the parameterized modifier.
     * 
     * @param pathFactory the path factory to use
     * @param parameterizedTypeFieldRootPath - the root path to be used.
     * @param parameterizedTypeModifierIndex - index in case of multiple modifiers.
     * @return the path.
     */
    public static Path getParameterizedTypeFieldRModifierPath( PathFactory pathFactory,
                                                               Path parameterizedTypeFieldRootPath,
                                                                 int parameterizedTypeModifierIndex ) {
        Path basePath = pathFactory.create(parameterizedTypeFieldRootPath,
                                           JavaMetadataLexicon.PARAMETERIZED_TYPE_MODIFIER_CHILD_NODE);
        return pathFactory.create(basePath, pathFactory.createSegment(JavaMetadataLexicon.MODIFIER_DECLARATION_CHILD_NODE,
                                                                      parameterizedTypeModifierIndex));
    }

    /**
     * Sequences a modifier of this parameterized type.
     * 
     * @param modifierMetadata - the meta data.
     * @param parameterizedTypeModifierPath - the path of a modifier.
     * @param pathFactory - the {@link PathFactory}.
     * @param nameFactory - the {@link NameFactory}.
     * @param output - the {@link SequencerOutput}.
     */
    public static void sequenceTheParameterizedTypeModifier( ModifierMetadata modifierMetadata,
                                                             Path parameterizedTypeModifierPath,
                                                             PathFactory pathFactory,
                                                             NameFactory nameFactory,
                                                             SequencerOutput output ) {
        output.setProperty(parameterizedTypeModifierPath,
                           nameFactory.create(JavaMetadataLexicon.MODIFIER_NAME),
                           modifierMetadata.getName());
    }

    /**
     * Get the path of a parameterized type variable.
     * 
     * @param pathFactory - the {@link PathFactory}.
     * @param parameterizedTypeFieldRootPath - the root path.
     * @param parameterizedTypeVariableIndex - the index in case of multiple paths
     * @return the path of the parameterized variable.
     */
    public static Path getParameterizedTypeFieldVariablePath( PathFactory pathFactory,
                                                              Path parameterizedTypeFieldRootPath,
                                                              int parameterizedTypeVariableIndex ) {
        Path basePath = pathFactory.create(parameterizedTypeFieldRootPath, JavaMetadataLexicon.PARAMETERIZED_TYPE_VARIABLE);
        return pathFactory.create(basePath, pathFactory.createSegment(JavaMetadataLexicon.VARIABLE,
                                                                      parameterizedTypeVariableIndex));
    }

    private ParameterizedTypeFieldMetadataSequencer() {
        // prevent constructor
    }
}
