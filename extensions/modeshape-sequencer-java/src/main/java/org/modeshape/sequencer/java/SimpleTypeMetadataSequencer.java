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
import org.modeshape.sequencer.java.metadata.SimpleTypeFieldMetadata;
import org.modeshape.sequencer.java.metadata.Variable;

/**
 * The sequencer of the {@link SimpleTypeFieldMetadata}
 */
public class SimpleTypeMetadataSequencer {

    private SimpleTypeMetadataSequencer() {
        // prevent construction
    }

    /**
     * @param output
     * @param nameFactory
     * @param pathFactory
     * @param simpleTypeFieldMetadata
     * @param methodParamRootPath
     */
    public static void sequenceMethodFormalParam( SequencerOutput output,
                                                  NameFactory nameFactory,
                                                  PathFactory pathFactory,
                                                  SimpleTypeFieldMetadata simpleTypeFieldMetadata,
                                                  Path methodParamRootPath ) {

        Path methodSimpleTypeFormalParamRootPath = SimpleTypeMetadataSequencer.createRootPath(pathFactory, methodParamRootPath);
        SimpleTypeMetadataSequencer.sequenceConstructorSimpleTypeName(simpleTypeFieldMetadata,
                                                                      methodSimpleTypeFormalParamRootPath,
                                                                      output,
                                                                      nameFactory,
                                                                      pathFactory);
        Path methodSimpleTypeParamChildNode = SimpleTypeMetadataSequencer.createSimpleTypeParamPath(pathFactory,
                                                                                                    methodSimpleTypeFormalParamRootPath);
        for (Variable variable : simpleTypeFieldMetadata.getVariables()) {
            VariableSequencer.sequenceTheVariable(output, nameFactory, variable, methodSimpleTypeParamChildNode);
        }
    }

    /**
     * the root path.
     * 
     * @param pathFactory the path factory
     * @param basePath - the base path to use to build a root path.
     * @return the root path, that is compose from other base path.
     */
    public static Path createRootPath( PathFactory pathFactory,
                                       Path basePath ) {

        return pathFactory.create(pathFactory.create(basePath, JavaMetadataLexicon.TYPE_CHILD_NODE),
                                  JavaMetadataLexicon.SIMPLE_TYPE_CHILD_NODE);
    }

    /**
     * Sequence the type name of the simple type.
     * 
     * @param simpleTypeFieldMetadata - the {@link SimpleTypeFieldMetadata}.
     * @param rootPath - the path.
     * @param output - the {@link SequencerOutput}.
     * @param nameFactory - the {@link NameFactory}.
     * @param pathFactory - the {@link PathFactory}.
     */
    public static void sequenceConstructorSimpleTypeName( SimpleTypeFieldMetadata simpleTypeFieldMetadata,
                                                          Path rootPath,
                                                          SequencerOutput output,
                                                          NameFactory nameFactory,
                                                          PathFactory pathFactory ) {

        Path constructorSimpleTypeParamChildNode = pathFactory.create(rootPath);
        output.setProperty(constructorSimpleTypeParamChildNode,
                           nameFactory.create(JavaMetadataLexicon.SIMPLE_TYPE_NAME),
                           simpleTypeFieldMetadata.getType());

    }

    /**
     * Create the path of parameter.
     * 
     * @param pathFactory - The {@link PathFactory}.
     * @param rootPath - the root path need to build the path.
     * @return the path of a variable node.
     */
    public static Path createSimpleTypeParamPath( PathFactory pathFactory,
                                                  Path rootPath ) {
        return pathFactory.create(pathFactory.create(rootPath, JavaMetadataLexicon.SIMPLE_TYPE_VARIABLE),
                                  JavaMetadataLexicon.VARIABLE);
    }

    /**
     * Sequence the return type of a method.
     * 
     * @param output
     * @param nameFactory
     * @param pathFactory
     * @param simpleTypeFieldMetadata
     * @param methodRootPath
     */
    public static void sequenceMethodReturnType( SequencerOutput output,
                                                 NameFactory nameFactory,
                                                 PathFactory pathFactory,
                                                 SimpleTypeFieldMetadata simpleTypeFieldMetadata,
                                                 Path methodRootPath ) {
        Path methodReturnPrimitiveTypeChildNode = pathFactory.create(pathFactory.create(methodRootPath,
                                                                                        JavaMetadataLexicon.RETURN_TYPE),
                                                                     JavaMetadataLexicon.SIMPLE_TYPE_CHILD_NODE);
        output.setProperty(methodReturnPrimitiveTypeChildNode,
                           nameFactory.create(JavaMetadataLexicon.SIMPLE_TYPE_NAME),
                           simpleTypeFieldMetadata.getType());
    }

}
