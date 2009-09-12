/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.sequencer.java;

import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.sequencer.SequencerOutput;
import org.jboss.dna.sequencer.java.metadata.ModifierMetadata;
import org.jboss.dna.sequencer.java.metadata.ParameterizedTypeFieldMetadata;

/**
 * Sequencer for all paths of a {@link ParameterizedTypeFieldMetadata}.
 */
public class ParameterizedTypeFieldMetadataSequencer implements JavaSourceCndDefinition {

    /**
     * Create the root path for all path children of a parameterized type.
     * 
     * @param parameterizedIndex - index in case of multiple paths.
     * @return a path with a index starting by 1.
     */
    public static String getParameterizedTypeFieldRootPath( int parameterizedIndex ) {
        String simpleTypeFieldRootPath = JavaMetadataUtil.createPathWithIndex(JAVA_COMPILATION_UNIT_NODE + SLASH
                                                                              + JAVA_UNIT_TYPE_CHILD_NODE + SLASH
                                                                              + JAVA_CLASS_DECLARATION_CHILD_NODE + SLASH
                                                                              + JAVA_NORMAL_CLASS_CHILD_NODE + SLASH
                                                                              + JAVA_NORMAL_CLASS_DECLARATION_CHILD_NODE + SLASH
                                                                              + JAVA_FIELD_CHILD_NODE + SLASH
                                                                              + JAVA_FIELD_TYPE_CHILD_NODE + SLASH
                                                                              + JAVA_TYPE_CHILD_NODE + SLASH
                                                                              + JAVA_PARAMETERIZED_TYPE_CHILD_NODE,
                                                                              parameterizedIndex);
        return simpleTypeFieldRootPath;
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
                                                         String parameterizedTypeFieldRootPath,
                                                         PathFactory pathFactory,
                                                         NameFactory nameFactory,
                                                         SequencerOutput output ) {
        Path parameterizedTypeFieldChildNode = pathFactory.create(parameterizedTypeFieldRootPath);
        output.setProperty(parameterizedTypeFieldChildNode,
                           nameFactory.create(JAVA_PARAMETERIZED_TYPE_NAME),
                           parameterizedTypeFieldMetadata.getType());
    }

    /**
     * Create a path for the parameterized modifier.
     * 
     * @param parameterizedTypeFieldRootPath - the root path to be used.
     * @param parameterizedTypeModifierIndex - index in case of multiple modifiers.
     * @return the path.
     */
    public static String getParameterizedTypeFieldRModifierPath( String parameterizedTypeFieldRootPath,
                                                                 int parameterizedTypeModifierIndex ) {
        String parameterizedTypeModifierPath = JavaMetadataUtil.createPathWithIndex(parameterizedTypeFieldRootPath + SLASH
                                                                                    + JAVA_PARAMETERIZED_TYPE_MODIFIER_CHILD_NODE
                                                                                    + SLASH
                                                                                    + JAVA_MODIFIER_DECLARATION_CHILD_NODE,
                                                                                    parameterizedTypeModifierIndex);
        return parameterizedTypeModifierPath;
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
                                                             String parameterizedTypeModifierPath,
                                                             PathFactory pathFactory,
                                                             NameFactory nameFactory,
                                                             SequencerOutput output ) {
        Path parameterizedTypeModifieChildNode = pathFactory.create(parameterizedTypeModifierPath);
        output.setProperty(parameterizedTypeModifieChildNode, nameFactory.create(JAVA_MODIFIER_NAME), modifierMetadata.getName());
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
                                                              String parameterizedTypeFieldRootPath,
                                                              int parameterizedTypeVariableIndex ) {
        String variablePath = JavaMetadataUtil.createPathWithIndex(parameterizedTypeFieldRootPath + SLASH
                                                                   + JAVA_PARAMETERIZED_TYPE_VARIABLE + SLASH + JAVA_VARIABLE,
                                                                   parameterizedTypeVariableIndex);
        Path parameterizedTypeVariableChildNode = pathFactory.create(variablePath);
        return parameterizedTypeVariableChildNode;
    }

    private ParameterizedTypeFieldMetadataSequencer() {
        // prevent constructor
    }
}
