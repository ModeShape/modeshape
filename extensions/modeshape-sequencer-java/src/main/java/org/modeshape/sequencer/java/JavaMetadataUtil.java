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

import java.io.IOException;
import java.io.InputStream;
import org.eclipse.jdt.core.dom.Name;
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
     * @throws IOException - exceptional situation during calculating the length.
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
     * @throws IOException - exceptional error can be thrown during the reading of the file.
     */
    public static char[] getJavaSourceFromTheInputStream( InputStream inputStream,
                                                          long length,
                                                          String encoding ) throws IOException {
        char[] source = Util.getInputStreamAsCharArray(inputStream, (int)length, encoding);
        return source;
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

    /**
     * Create a path for the tree with index.
     * 
     * @param path the path.
     * @param index the index begin with 1.
     * @return the string
     * @throws IllegalArgumentException if the path is null, blank or empty, or if the index is not a positive value
     */
    public static String createPathWithIndex( String path,
                                              int index ) {
        CheckArg.isNotEmpty(path, "path");
        CheckArg.isPositive(index, "index");
        return path + "[" + index + "]";
    }

    /**
     * Create a path for the tree without index.
     * 
     * @param path - the path.
     * @return the string
     * @throws IllegalArgumentException if the path is null, blank or empty
     */
    public static String createPath( String path ) {
        CheckArg.isNotEmpty(path, "path");
        return path;
    }

    // prevent construction
    private JavaMetadataUtil() {
    }
}
