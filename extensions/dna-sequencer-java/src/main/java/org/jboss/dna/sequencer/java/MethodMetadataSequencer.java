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

import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.PathFactory;
import org.jboss.dna.sequencer.java.metadata.MethodMetadata;

/**
 * Sequencer for a {@link MethodMetadata}.
 * 
 * @author Serge PAgop
 *
 */
public class MethodMetadataSequencer implements JavaSourceCndDefinition {
    
    /**
     * Create  a path of method/constructor parameter. 
     * @param pathFactory - {@link PathFactory}.
     * @param rootPath - Root path of the method/constructor.
     * @return the path of the parameter.
     */
    public static Path createMethodParamPath( PathFactory pathFactory,
                                        String rootPath ) {
        String methodPrimitiveParamVariablePath = JavaMetadataUtil.createPath(rootPath
                                                                          + SLASH
                                                                          + JAVA_PRIMITIVE_TYPE_VARIABLE
                                                                          + SLASH + JAVA_VARIABLE);
        Path methodParamChildNode = pathFactory.create(methodPrimitiveParamVariablePath);
        return methodParamChildNode;
    }

    /**
     * create a root path for method parameter.
     * 
     * @param constructorParameterRootPath
     * @return root path for a method parameter.
     */
    public static String createMethodParamRootPath( String constructorParameterRootPath ) {
        String constructPrimitiveFormalParamRootPath = JavaMetadataUtil.createPath(constructorParameterRootPath + SLASH
                                                                          + JAVA_TYPE_CHILD_NODE + SLASH
                                                                          + JAVA_PRIMITIVE_TYPE_CHILD_NODE);
        return constructPrimitiveFormalParamRootPath;
    }

    private MethodMetadataSequencer() {
    }

}
