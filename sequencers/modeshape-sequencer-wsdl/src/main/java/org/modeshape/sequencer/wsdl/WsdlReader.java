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
package org.modeshape.sequencer.wsdl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.concurrent.atomic.AtomicLong;
import javax.jcr.Node;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.util.SizeMeasuringInputStream;
import org.modeshape.common.util.SizeMeasuringReader;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.sequencer.sramp.AbstractResolvingReader;
import org.xml.sax.InputSource;

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
        logger.debug("Processing XSD '{0}'", outputNode.getName());
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
