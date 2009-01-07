/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.dna.sequencer.msoffice.word;

import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.hwpf.HWPFDocument;

/**
 * Reades table of contents from Word document
 * 
 * @author Michael Trezzi
 */
public class WordMetadataReader {

    @SuppressWarnings( "null" )
    public static WordMetadata invoke( InputStream stream ) throws IOException {
        WordMetadata metadata = new WordMetadata();
        HWPFDocument document = new HWPFDocument(stream);
        if (document != null) {
            // TODO
        }
        return metadata;
    }
}
