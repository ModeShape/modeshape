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
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.internal.compiler.util.Util;
import org.modeshape.common.util.CheckArg;

/**
 * Utility class for working with metadata.
 */
public class JavaMetadataUtil {
    /**
     * Get the length of the input stream.
     * 
     * @param stream - the <code>InputStream</code>
     * @return the length of the stream.
     * @throws java.io.IOException - exceptional situation during calculating the length.
     */
    public static long length( InputStream stream ) throws IOException {
        return stream.available();
    }

    /**
     * Gets Java source from the <code>InputStream</code>.
     * 
     * @param inputStream - the <code>FileInputStream</code>.
     * @param length - the length of the java file.
     * @param encoding - the encoding of the source, if there is one.
     * @return the array character of the java source.
     * @throws java.io.IOException - exceptional error can be thrown during the reading of the file.
     */
    public static char[] getJavaSourceFromTheInputStream( InputStream inputStream,
                                                          long length,
                                                          String encoding ) throws IOException {
        return Util.getInputStreamAsCharArray(inputStream, (int)length, encoding);
    }

    /**
     * Get the fully qualified name from the <code>Name</code>.
     * 
     * @param name - the name to process.
     * @return a FQN of the name.
     */
    public static String getName( Name name ) {
        CheckArg.isNotNull(name, "name");
        return name.getFullyQualifiedName();
    }

    public static String expressionString( Expression expression ) {
        if (expression instanceof StringLiteral) {
            return ((StringLiteral)expression).getLiteralValue();
        }
        return expression.toString();
    }

    // prevent construction
    private JavaMetadataUtil() {
    }
}
