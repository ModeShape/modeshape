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
package org.jboss.dna.sequencer.java;

import org.jboss.dna.sequencer.java.metadata.SimpleTypeFieldMetadata;
import org.jboss.dna.sequencer.java.metadata.Variable;
import org.jboss.dna.spi.graph.NameFactory;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PathFactory;
import org.jboss.dna.spi.sequencers.SequencerOutput;

/**
 * The sequencer of the {@link SimpleTypeFieldMetadata}
 * 
 * @author Serge Pagop
 */
public class SimpleTypeMetadataSequencer implements JavaSourceCndDefinition {

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
                                                  String methodParamRootPath ) {

        String methodSimpleTypeFormalParamRootPath = SimpleTypeMetadataSequencer.createRootPath(methodParamRootPath);
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
     * @param basePath - the base path to use to build a root path.
     * @return the root path, that is compose from other base path.
     */
    public static String createRootPath( String basePath ) {
        return JavaMetadataUtil.createPath(basePath + SLASH + JAVA_TYPE_CHILD_NODE + SLASH + JAVA_SIMPLE_TYPE_CHILD_NODE);
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
                                                          String rootPath,
                                                          SequencerOutput output,
                                                          NameFactory nameFactory,
                                                          PathFactory pathFactory ) {

        Path constructorSimpleTypeParamChildNode = pathFactory.create(rootPath);
        output.setProperty(constructorSimpleTypeParamChildNode,
                           nameFactory.create(JAVA_SIMPLE_TYPE_NAME),
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
                                                  String rootPath ) {
        String paramVariablePath = JavaMetadataUtil.createPath(rootPath + SLASH + JAVA_SIMPLE_TYPE_VARIABLE + SLASH
                                                               + JAVA_VARIABLE);
        return pathFactory.create(paramVariablePath);
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
                                                 String methodRootPath ) {
        String methodReturnSimpleTypePath = JavaMetadataUtil.createPath(methodRootPath + SLASH + JAVA_RETURN_TYPE + SLASH
                                                                        + JAVA_SIMPLE_TYPE_CHILD_NODE);

        Path methodReturnPrimitiveTypeChildNode = pathFactory.create(methodReturnSimpleTypePath);
        output.setProperty(methodReturnPrimitiveTypeChildNode,
                           nameFactory.create(JAVA_SIMPLE_TYPE_NAME),
                           simpleTypeFieldMetadata.getType());
    }

}
