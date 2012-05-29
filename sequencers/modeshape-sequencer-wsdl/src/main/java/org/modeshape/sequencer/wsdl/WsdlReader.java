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
package org.modeshape.sequencer.wsdl;

import javax.jcr.Node;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.util.SizeMeasuringInputStream;
import org.modeshape.common.util.SizeMeasuringReader;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.sequencer.sramp.AbstractResolvingReader;
import org.xml.sax.InputSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A class that can parse WSDL definitions and derive a node structure from the content.
 * <p>
 * This class is intended to be subclassed by supplying implementations for the {@link #parse(org.xml.sax.InputSource)}.
 * </p>
 *
 * @param <T> the type of object returned by the parser
 */
@NotThreadSafe
public abstract class WsdlReader<T> extends AbstractResolvingReader {

    /**
     * the URI of the document being read; never null or empty
     */
    protected final String baseUri;

    public WsdlReader( Sequencer.Context context,
                       String baseUri ) {
        super(context);
        this.baseUri = baseUri;
    }

    @Override
    public void read( InputSource source,
                      Node outputNode ) throws Exception {
        logger.debug("Processing XSD '{}'", outputNode.getName());
        Reader reader = null;
        InputStream stream = null;
        try {
            AtomicLong contentSize = new AtomicLong();
            if (source.getCharacterStream() != null) {
                reader = new SizeMeasuringReader(source.getCharacterStream(), contentSize);
                source = new InputSource(reader);
            } else {
                stream = new SizeMeasuringInputStream(source.getByteStream(), contentSize);
                source = new InputSource(stream);
            }

            // Parse the WSDL, measuring the number of bytes as we read ...
            T result = parse(source);

            // Convert the WSDL to content ...
            process(result, outputNode, contentSize.get());
        } finally {
            assert (reader != null) || (stream != null);
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.debug("Cannot close reader ", e);
                }
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    logger.debug("Cannot close stream ", e);
                }
            }
        }
    }

    /**
     * Parse the supplied source (which contains either a {@link java.io.Reader} or an {@link java.io.InputStream}) and produce a representation
     * of the WSDL definition.
     *
     * @param source the source containing the WSDL stream; never null
     * @return the WSDL definition representation; may not be null
     * @throws Exception if there is a problem during parsing
     */
    protected abstract T parse( InputSource source ) throws Exception;

    /**
     * Process the supplied representation of the WSDL definition that was returned from the {@link #parse(org.xml.sax.InputSource)}
     * method and write the derived output content under the {@link Node outputNode}.
     *
     * @param parsedForm the representation of the WSDL definition, which will always be the value returned from
     * {@link #parse(org.xml.sax.InputSource)}
     * @param outputNode the {@link javax.jcr.Node node} under which the derived content for the XSD should be written; may not be null
     * @param sizeOfFile the size of the WSDL stream, in bytes
     * @throws Exception if there is a problem during processing
     */
    protected abstract void process( T parsedForm,
                                     Node outputNode,
                                     long sizeOfFile ) throws Exception;
}
