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

import java.io.IOException;
import java.io.InputStream;
import org.eclipse.jdt.internal.compiler.util.Util;

/**
 * @author Serge Pagop
 */
public class JavaMetadataUtil {
    /**
     * Get the length of the inputstream.
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
     * @param encoding - the encoding of the sourece, if there is one.
     * @return the array character of the java source.
     * @throws IOException - exceptional error can be thrown during the reading of the file.
     */
    public static char[] getJavaSourceFromTheInputStream( InputStream inputStream,
                                                          long length,
                                                          String encoding ) throws IOException {
        char[] source = Util.getInputStreamAsCharArray(inputStream, (int)length, encoding);
        return source;
    }
}
