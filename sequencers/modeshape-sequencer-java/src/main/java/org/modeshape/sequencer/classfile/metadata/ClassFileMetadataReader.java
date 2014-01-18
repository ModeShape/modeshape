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
package org.modeshape.sequencer.classfile.metadata;

import java.io.DataInputStream;
import java.io.InputStream;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.ClassFile;

/**
 * Utility for extracting metadata from Java class files
 */
public class ClassFileMetadataReader {

    public static ClassMetadata instance( InputStream stream ) throws Exception {
        ClassFile clazz = new ClassFile(new DataInputStream(stream));

        if ((AccessFlag.ENUM & clazz.getAccessFlags()) == AccessFlag.ENUM) {
            return new EnumMetadata(clazz);
        }

        return new ClassMetadata(clazz);
    }
}
