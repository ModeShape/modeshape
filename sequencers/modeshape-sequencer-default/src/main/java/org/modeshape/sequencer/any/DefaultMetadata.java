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
package org.modeshape.sequencer.any;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.modeshape.common.logging.Logger;

/**
 * Utility for extracting metadata from MP3 files.
 */
public class DefaultMetadata {

    protected static final Logger logger = Logger.getLogger(DefaultMetadata.class);
    
    private long contentSize;

    private DefaultMetadata() {

    }

    public static DefaultMetadata instance( InputStream stream ) throws Exception {

        DefaultMetadata me = null;
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("modeshape-sequencer-default", ".any");
            FileOutputStream fileOutputStream = new FileOutputStream(tmpFile);
            byte[] b = new byte[1024];
            while (stream.read(b) != -1) {
                fileOutputStream.write(b);
            }

            me = new DefaultMetadata();
            me.contentSize = tmpFile.length();

        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
        return me;

    }

    public long getContentSize() {
        return contentSize;
    }

}
