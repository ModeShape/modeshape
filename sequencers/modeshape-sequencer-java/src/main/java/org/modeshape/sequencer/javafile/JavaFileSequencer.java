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
package org.modeshape.sequencer.javafile;

import java.io.IOException;
import java.io.InputStream;
import javax.jcr.Binary;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.sequencer.classfile.ClassFileSequencer;
import org.modeshape.sequencer.javafile.metadata.JavaMetadata;

/**
 * Sequencer which handles java source files.
 * 
 * @author ?
 * @author Horia Chiorean
 */
public class JavaFileSequencer extends Sequencer {

    private static final SourceFileRecorder DEFAULT_SOURCE_FILE_RECORDER = new ClassSourceFileRecorder();
    private SourceFileRecorder sourceFileRecorder = DEFAULT_SOURCE_FILE_RECORDER;

    @Override
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        String classFileCnd = "/" + ClassFileSequencer.class.getPackage().getName().replaceAll("\\.", "/")
                              + "/sequencer-classfile.cnd";
        registerNodeTypes(classFileCnd, nodeTypeManager, true);
    }

    @Override
    public boolean execute( Property inputProperty,
                            Node outputNode,
                            Context context ) throws Exception {
        Binary binaryValue = inputProperty.getBinary();
        CheckArg.isNotNull(binaryValue, "binary");
        InputStream stream = binaryValue.getStream();
        try {
            JavaMetadata javaMetadata = JavaMetadata.instance(stream, binaryValue.getSize(), null);
            sourceFileRecorder.record(context, outputNode, javaMetadata);
            return true;
        } catch (Exception ex) {
            getLogger().error(ex, "Error sequencing file");
            return false;
        } finally {
            stream.close();
        }
    }

    /**
     * Sets the custom {@link SourceFileRecorder} by specifying a class name. This method attempts to instantiate an instance of
     * the custom {@link SourceFileRecorder} class prior to ensure that the new value represents a valid implementation.
     * 
     * @param sourceFileRecorderClassName the fully-qualified class name of the new custom class file recorder implementation;
     *        null indicates that {@link org.modeshape.sequencer.javafile.ClassSourceFileRecorder the class file recorder} should
     *        be used.
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
