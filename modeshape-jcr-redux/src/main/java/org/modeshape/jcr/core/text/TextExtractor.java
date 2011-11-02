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
package org.modeshape.jcr.core.text;

import java.io.IOException;
import java.io.InputStream;

/**
 * An abstraction for components that are able to extract text content from an input stream.
 */
public interface TextExtractor {

    /**
     * Determine if this extractor is capable of processing content with the supplied MIME type.
     * 
     * @param mimeType the MIME type; never null
     * @return true if this extractor can process content with the supplied MIME type, or false otherwise.
     */
    boolean supportsMimeType( String mimeType );

    /**
     * Sequence the data found in the supplied stream, placing the output information into the supplied map.
     * <p>
     * ModeShape's SequencingService determines the sequencers that should be executed by monitoring the changes to one or more
     * workspaces that it is monitoring. Changes in those workspaces are aggregated and used to determine which sequencers should
     * be called. If the sequencer implements this interface, then this method is called with the property that is to be sequenced
     * along with the interface used to register the output. The framework takes care of all the rest.
     * </p>
     * 
     * @param stream the stream with the data to be sequenced; never <code>null</code>
     * @param output the output from the sequencing operation; never <code>null</code>
     * @param context the context for the sequencing operation; never <code>null</code>
     * @throws IOException if there is a problem reading the stream
     */
    void extractFrom( InputStream stream,
                      TextExtractorOutput output,
                      TextExtractorContext context ) throws IOException;

}
