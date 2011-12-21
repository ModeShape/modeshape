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
package org.modeshape.sequencer.classfile;

import javax.jcr.*;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.sequencer.classfile.metadata.ClassFileMetadataReader;
import org.modeshape.sequencer.classfile.metadata.ClassMetadata;
import java.io.IOException;

@ThreadSafe
public class ClassFileSequencer extends Sequencer {

    private static final ClassFileRecorder DEFAULT_CLASS_FILE_RECORDER = new DefaultClassFileRecorder();

    private ClassFileRecorder classFileRecorder = DEFAULT_CLASS_FILE_RECORDER;

    @Override
    public boolean execute( Property inputProperty, Node outputNode, Context context ) throws Exception {
        Binary binaryValue = inputProperty.getBinary();
        CheckArg.isNotNull(binaryValue, "binary");
        ClassMetadata classMetadata = ClassFileMetadataReader.instance(binaryValue.getStream());
        classFileRecorder.recordClass(context, outputNode, classMetadata);
        return true;
    }

    @Override
    public void initialize( NamespaceRegistry registry, NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        registerNodeTypes("sequencer-classfile.cnd", nodeTypeManager, true);
    }

    /**
     * Sets the custom {@link ClassFileRecorder} by specifying a class name. This method attempts to instantiate an instance of
     * the custom {@link ClassFileRecorder} class prior to ensure that the new value represents a valid implementation.
     *
     * @param classFileRecorderClassName the fully-qualified class name of the new custom class file recorder implementation; null
     * indicates that {@link DefaultClassFileRecorder the default class file recorder} should be used.
     * @throws ClassNotFoundException if the the class for the {@code ClassFileRecorder} implementation cannot be located
     * @throws IllegalAccessException if the row factory class or its nullary constructor is not accessible.
     * @throws InstantiationException if the row factory represents an abstract class, an interface, an array class, a primitive
     * type, or void; or if the class has no nullary constructor; or if the instantiation fails for some other reason.
     * @throws ClassCastException if the instantiated class file recorder does not implement the {@link ClassFileRecorder}
     * interface
     */
    public void setClassFileRecorderClassName( String classFileRecorderClassName )
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {

        if (classFileRecorderClassName == null) {
            this.classFileRecorder = DEFAULT_CLASS_FILE_RECORDER;
            return;
        }

        Class<?> classFileRecorderClass = Class.forName(classFileRecorderClassName);
        this.classFileRecorder = (ClassFileRecorder)classFileRecorderClass.newInstance();
    }

    /**
     * Sets a custom {@link ClassFileRecorder}. If {@code classFileRecorder} is null, then the {@link DefaultClassFileRecorder
     * default class file recorder} will be used.
     *
     * @param classFileRecorder the new custom class file recorder implementation; null indicates that
     * {@link DefaultClassFileRecorder the default class file recorder} should be used.
     */
    public void setClassFileRecorder( ClassFileRecorder classFileRecorder ) {
        this.classFileRecorder = classFileRecorder == null ? DEFAULT_CLASS_FILE_RECORDER : classFileRecorder;
    }
}
