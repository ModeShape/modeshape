/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.sequencer.classfile;

import java.io.IOException;
import java.io.InputStream;
import javax.jcr.Binary;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.sequencer.classfile.metadata.ClassFileMetadataReader;
import org.modeshape.sequencer.classfile.metadata.ClassMetadata;

@ThreadSafe
public class ClassFileSequencer extends Sequencer {

    private static final ClassFileRecorder DEFAULT_CLASS_FILE_RECORDER = new DefaultClassFileRecorder();

    private ClassFileRecorder classFileRecorder = DEFAULT_CLASS_FILE_RECORDER;

    @Override
    public boolean execute( Property inputProperty,
                            Node outputNode,
                            Context context ) throws Exception {
        Binary binaryValue = inputProperty.getBinary();
        CheckArg.isNotNull(binaryValue, "binary");
        try (InputStream stream = binaryValue.getStream()) {
            ClassMetadata classMetadata = ClassFileMetadataReader.instance(stream);
            classFileRecorder.recordClass(context, outputNode, classMetadata);
            return true;
        }
    }

    @Override
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        registerNodeTypes("sequencer-classfile.cnd", nodeTypeManager, true);
    }

    /**
     * Sets the custom {@link ClassFileRecorder} by specifying a class name. This method attempts to instantiate an instance of
     * the custom {@link ClassFileRecorder} class prior to ensure that the new value represents a valid implementation.
     * 
     * @param classFileRecorderClassName the fully-qualified class name of the new custom class file recorder implementation; null
     *        indicates that {@link DefaultClassFileRecorder the default class file recorder} should be used.
     * @throws ClassNotFoundException if the the class for the {@code ClassFileRecorder} implementation cannot be located
     * @throws IllegalAccessException if the row factory class or its nullary constructor is not accessible.
     * @throws InstantiationException if the row factory represents an abstract class, an interface, an array class, a primitive
     *         type, or void; or if the class has no nullary constructor; or if the instantiation fails for some other reason.
     * @throws ClassCastException if the instantiated class file recorder does not implement the {@link ClassFileRecorder}
     *         interface
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
     *        {@link DefaultClassFileRecorder the default class file recorder} should be used.
     */
    public void setClassFileRecorder( ClassFileRecorder classFileRecorder ) {
        this.classFileRecorder = classFileRecorder == null ? DEFAULT_CLASS_FILE_RECORDER : classFileRecorder;
    }
}
