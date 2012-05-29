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
 * Lesser General Public License for more details
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org
 */
package org.modeshape.sequencer.javafile;

import javax.jcr.*;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.sequencer.classfile.ClassFileSequencer;
import org.modeshape.sequencer.javafile.metadata.JavaMetadata;
import java.io.IOException;

/**
 * Sequencer which handles java source files.
 * 
 * @author  ?
 * @author Horia Chiorean
 */
public class JavaFileSequencer extends Sequencer {
    
    private static final SourceFileRecorder DEFAULT_SOURCE_FILE_RECORDER = new ClassSourceFileRecorder();
    private SourceFileRecorder sourceFileRecorder = DEFAULT_SOURCE_FILE_RECORDER;

    @Override
    public void initialize( NamespaceRegistry registry, NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        String classFileCnd = "/" + ClassFileSequencer.class.getPackage().getName().replaceAll("\\.","/") + "/sequencer-classfile.cnd";
        registerNodeTypes(classFileCnd, nodeTypeManager, true);
    }

    @Override
    public boolean execute( Property inputProperty, Node outputNode, Context context ) throws Exception {
        Binary binaryValue = inputProperty.getBinary();
        CheckArg.isNotNull(binaryValue, "binary");
        try {
            JavaMetadata javaMetadata = JavaMetadata.instance(binaryValue.getStream(), JavaMetadataUtil.length(binaryValue.getStream()), null);
            sourceFileRecorder.record(context, outputNode, javaMetadata);
            return true;
        } catch (Exception ex) {
            logger.error("Error sequencing file", ex);
            return false;
        }
    }

    /**
     * Sets the custom {@link SourceFileRecorder} by specifying a class name. This method attempts to instantiate an instance of
     * the custom {@link SourceFileRecorder} class prior to ensure that the new value represents a valid implementation.
     * 
     * @param sourceFileRecorderClassName the fully-qualified class name of the new custom class file recorder implementation;
     *        null indicates that {@link org.modeshape.sequencer.javafile.ClassSourceFileRecorder the class file recorder} should be used.
     * @throws ClassNotFoundException if the the class for the {@code SourceFileRecorder} implementation cannot be located
     * @throws IllegalAccessException if the row factory class or its nullary constructor is not accessible.
     * @throws InstantiationException if the row factory represents an abstract class, an interface, an array class, a primitive
     *         type, or void; or if the class has no nullary constructor; or if the instantiation fails for some other reason.
     * @throws ClassCastException if the instantiated class file recorder does not implement the {@link SourceFileRecorder}
     *         interface
     */
    public void setSourceFileRecorderClassName( String sourceFileRecorderClassName )
        throws ClassNotFoundException, IllegalAccessException, InstantiationException {

        if (sourceFileRecorderClassName == null) {
            this.sourceFileRecorder = DEFAULT_SOURCE_FILE_RECORDER;
            return;
        }

        Class<?> sourceFileRecorderClass = Class.forName(sourceFileRecorderClassName);
        this.sourceFileRecorder = (SourceFileRecorder)sourceFileRecorderClass.newInstance();
    }
}
