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
package org.jboss.dna.sequencer.java;

import java.util.List;
import org.jboss.dna.graph.properties.NameFactory;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.PathFactory;
import org.jboss.dna.graph.sequencers.SequencerOutput;
import org.jboss.dna.sequencer.java.metadata.ArrayTypeFieldMetadata;
import org.jboss.dna.sequencer.java.metadata.ModifierMetadata;
import org.jboss.dna.sequencer.java.metadata.Variable;

/**
 * Sequencer for array types.
 * 
 * @author Serge Pagop
 */
public class ArrayTypeFieldMetadataSequencer implements JavaSourceCndDefinition {

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
                                                  String methodParamRootPath ) {
        String methodFormalParamRootPath = ArrayTypeFieldMetadataSequencer.createRootPath(methodParamRootPath);
        Path methodParamChildNode = pathFactory.create(methodFormalParamRootPath);
        output.setProperty(methodParamChildNode, nameFactory.create(JAVA_ARRAY_TYPE_NAME), arrayTypeFieldMetadata.getType());
        Path ArrayTypeVariableChildNode = pathFactory.create(JavaMetadataUtil.createPath(methodFormalParamRootPath + SLASH
                                                                                         + JAVA_ARRAY_TYPE_VARIABLE + SLASH
                                                                                         + JAVA_VARIABLE));
        for (Variable variable : arrayTypeFieldMetadata.getVariables()) {
            VariableSequencer.sequenceTheVariable(output, nameFactory, variable, ArrayTypeVariableChildNode);
        }

    }

    /**
     * the root path.
     * 
     * @param basePath - the base path to use to build a root path.
     * @return the root path, that is compose from other base path.
     */
    public static String createRootPath( String basePath ) {
        return JavaMetadataUtil.createPath(basePath + SLASH + JAVA_TYPE_CHILD_NODE + SLASH + JAVA_ARRAY_TYPE_CHILD_NODE);
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
                                                String path,
                                                int index ) {

        // type
        Path arryTypeChildNode = pathFactory.create(path);
        output.setProperty(arryTypeChildNode, nameFactory.create(JAVA_ARRAY_TYPE_NAME), arrayTypeFieldMetadata.getType());
        // modifiers
        List<ModifierMetadata> modifiers = arrayTypeFieldMetadata.getModifiers();
        int arrayModifierIndex = 1;
        for (ModifierMetadata modifierMetadata : modifiers) {
            String modifierPath = JavaMetadataUtil.createPathWithIndex(path + SLASH + JAVA_ARRAY_TYPE_MODIFIER_CHILD_NODE + SLASH
                                                                       + JAVA_MODIFIER_DECLARATION_CHILD_NODE, arrayModifierIndex);
            Path modifierChildNode = pathFactory.create(modifierPath);
            output.setProperty(modifierChildNode, nameFactory.create(JAVA_MODIFIER_NAME), modifierMetadata.getName());
            arrayModifierIndex++;
        }
        // variables
        List<Variable> variables = arrayTypeFieldMetadata.getVariables();
        int arrayVariableIndex = 1;
        for (Variable variable : variables) {
            String variablePath = JavaMetadataUtil.createPathWithIndex(path + SLASH + JAVA_ARRAY_TYPE_VARIABLE + SLASH
                                                                       + JAVA_VARIABLE, arrayVariableIndex);
            Path primitiveChildNode = pathFactory.create(variablePath);
            VariableSequencer.sequenceTheVariable(output, nameFactory, variable, primitiveChildNode);
            arrayVariableIndex++;
        }
    }

}
