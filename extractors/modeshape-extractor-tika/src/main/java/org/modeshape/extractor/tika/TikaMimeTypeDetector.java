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

package org.modeshape.extractor.tika;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.io.TemporaryResources;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.mimetype.MimeTypeDetector;
import java.io.IOException;
import java.io.InputStream;

/**
 * Implementation of a {@link MimeTypeDetector} which uses Tika.
 *
 * @author Horia Chiorean
 */
public class TikaMimeTypeDetector extends MimeTypeDetector {

    @Override
    public String mimeTypeOf( final String name,
                              final Binary binaryValue ) throws RepositoryException, IOException {
        CheckArg.isNotNull(binaryValue, "binaryValue");
        return processStream(binaryValue, new StreamOperation<String>() {
            @Override
            public String execute( InputStream stream ) throws IOException {
                Metadata metadata = new Metadata();
                if (!StringUtil.isBlank(name)) {
                    metadata.set(Metadata.RESOURCE_NAME_KEY, name);
                }
                TemporaryResources tmp = new TemporaryResources();
                try {
                    TikaInputStream tikaInputStream = TikaInputStream.get(stream, tmp);
                    MediaType autoDetectedMimeType = new DefaultDetector(this.getClass().getClassLoader()).detect(tikaInputStream,
                                                                                                                  metadata);
                    return autoDetectedMimeType.toString();
                } finally {
                    tmp.close();
                }
            }
        });
    }
}
