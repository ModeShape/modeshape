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

import java.util.List;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.sequencer.java.metadata.ArrayTypeFieldMetadata;
import org.modeshape.sequencer.java.metadata.ModifierMetadata;
import org.modeshape.sequencer.java.metadata.Variable;

/**
 * Sequencer for array types.
 */
public class ArrayTypeFieldMetadataSequencer {

    /**
     * Sequence all formal parameters of a method.
     * 
     * @param output - the {@link SequencerOutput}.
     * @param nameFactory - the {@link NameFactory}.
     * @param pathFactory - the {@link PathFactory}.
     * @param arrayTypeFieldMetadata - the meta data of a array type.
     * @param methodParamRootPath - Base path of the method declaration.
     */
    public static void sequenceMethodFormalParam( SequencerOutput output,
                                                  NameFactory nameFactory,
                                                  PathFactory pathFactory,
                                                  ArrayTypeFieldMetadata arrayTypeFieldMetadata,
                                                  Path methodParamRootPath ) {
        Path methodFormalParamRootPath = createRootPath(pathFactory, methodParamRootPath);
        output.setProperty(methodFormalParamRootPath,
                           nameFactory.create(JavaMetadataLexicon.ARRAY_TYPE_NAME),
                           arrayTypeFieldMetadata.getType());
        Path arrayTypeVariableChildNode = pathFactory.create(pathFactory.create(pathFactory.create(pathFactory.create(methodParamRootPath,
                                                                                                                      JavaMetadataLexicon.TYPE_CHILD_NODE),
                                                                                                   JavaMetadataLexicon.ARRAY_TYPE_CHILD_NODE),
                                                                                JavaMetadataLexicon.ARRAY_TYPE_VARIABLE),
                                                             JavaMetadataLexicon.VARIABLE);
        for (Variable variable : arrayTypeFieldMetadata.getVariables()) {
            VariableSequencer.sequenceTheVariable(output, nameFactory, variable, arrayTypeVariableChildNode);
        }

    }

    /**
     * the root path.
     * 
     * @param pathFactory the path factory to use
     * @param basePath - the base path to use to build a root path.
     * @return the root path, that is compose from other base path.
     */
    public static Path createRootPath( PathFactory pathFactory,
                                       Path basePath ) {
        return pathFactory.create(pathFactory.create(basePath, JavaMetadataLexicon.TYPE_CHILD_NODE),
                                  JavaMetadataLexicon.ARRAY_TYPE_CHILD_NODE);
    }

    /**
     * Sequence member data of array type.
     * 
     * @param arrayTypeFieldMetadata
     * @param pathFactory
     * @param nameFactory
     * @param output
     * @param path
     * @param index
     */
    public static void sequenceFieldMemberData( ArrayTypeFieldMetadata arrayTypeFieldMetadata,
                                                PathFactory pathFactory,
                                                NameFactory nameFactory,
                                                SequencerOutput output,
                                                Path path,
                                                int index ) {

        // type
        Path arryTypeChildNode = pathFactory.create(path);
        output.setProperty(arryTypeChildNode,
                           nameFactory.create(JavaMetadataLexicon.ARRAY_TYPE_NAME),
                           arrayTypeFieldMetadata.getType());
        // modifiers
        List<ModifierMetadata> modifiers = arrayTypeFieldMetadata.getModifiers();
        int arrayModifierIndex = 1;
        for (ModifierMetadata modifierMetadata : modifiers) {
            Path modifierPath = pathFactory.create(pathFactory.create(path, JavaMetadataLexicon.ARRAY_TYPE_MODIFIER_CHILD_NODE),
                                                   pathFactory.createSegment(JavaMetadataLexicon.MODIFIER_DECLARATION_CHILD_NODE,
                                                                             arrayModifierIndex));
            output.setProperty(modifierPath, nameFactory.create(JavaMetadataLexicon.MODIFIER_NAME), modifierMetadata.getName());
            arrayModifierIndex++;
        }
        // variables
        List<Variable> variables = arrayTypeFieldMetadata.getVariables();
        int arrayVariableIndex = 1;
        for (Variable variable : variables) {
            Path variablePath = pathFactory.create(pathFactory.create(path, JavaMetadataLexicon.ARRAY_TYPE_VARIABLE),
                                                   pathFactory.createSegment(JavaMetadataLexicon.VARIABLE, arrayVariableIndex));

            VariableSequencer.sequenceTheVariable(output, nameFactory, variable, variablePath);
            arrayVariableIndex++;
        }
    }

}
