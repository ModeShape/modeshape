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

/**
 * Sequencer which handles java source files.
 *
 * @author ?
 * @author Horia Chiorean
 */
public class JavaFileSequencer extends Sequencer {

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

        try (InputStream stream = binaryValue.getStream()) {
            new JdtRecorder().record(context, stream, binaryValue.getSize(), null, outputNode);
            return true;
        }
    }
}
