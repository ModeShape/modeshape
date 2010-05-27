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

import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.sequencer.java.metadata.MethodMetadata;

/**
 * Sequencer for a {@link MethodMetadata}.
 */
public class MethodMetadataSequencer {

    /**
     * Create a path of method/constructor parameter.
     * 
     * @param pathFactory - {@link PathFactory}.
     * @param rootPath - Root path of the method/constructor.
     * @return the path of the parameter.
     */
    public static Path createMethodParamPath( PathFactory pathFactory,
                                              Path rootPath ) {
        return pathFactory.create(pathFactory.create(rootPath, JavaMetadataLexicon.PRIMITIVE_TYPE_VARIABLE),
                                  JavaMetadataLexicon.VARIABLE);
    }

    /**
     * create a root path for method parameter.
     * 
     * @param pathFactory the path factory to use
     * @param constructorParameterRootPath
     * @return root path for a method parameter.
     */
    public static Path createMethodParamRootPath( PathFactory pathFactory,
                                                  Path constructorParameterRootPath ) {
        return pathFactory.create(pathFactory.create(constructorParameterRootPath, JavaMetadataLexicon.TYPE_CHILD_NODE),
                                  JavaMetadataLexicon.PRIMITIVE_TYPE_CHILD_NODE);
    }

    private MethodMetadataSequencer() {
    }

}
