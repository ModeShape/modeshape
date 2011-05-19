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

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.collection.HashMultimap;
import org.modeshape.common.collection.Multimap;
import org.modeshape.common.util.Logger;
import org.modeshape.common.util.SizeMeasuringInputStream;
import org.modeshape.common.util.SizeMeasuringReader;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencerContext;
import org.modeshape.sequencer.xsd.XsdResolvers;
import org.modeshape.sequencer.xsd.XsdResolvers.SymbolSpace;
import org.xml.sax.InputSource;

/**
 * A class that can parse WSDL definitions, derive a graph structure from the content, and output that graph structure to a
 * supplied {@link SequencerOutput}.
 * <p>
 * This class is intended to be subclassed by supplying implementations for the {@link #parse(InputSource,String)} and
 * {@link #process(Object, Path, long)} methods.
 * </p>
 * 
 * @param <T> the type of object returned by the parser
 */
@NotThreadSafe
public abstract class WsdlReader<T> {

    public static final String UNBOUNDED = "unbounded";

    protected final SequencerOutput output;
    protected final StreamSequencerContext context;
    protected final Logger logger = Logger.getLogger(getClass());
    protected final Map<Path, Multimap<Name, Integer>> namesByParentPath = new HashMap<Path, Multimap<Name, Integer>>();
    protected final XsdResolvers resolvers = new XsdResolvers();
    protected List<ResolveFuture> resolveFutures = new LinkedList<ResolveFuture>();

    protected WsdlReader( SequencerOutput output,
                          StreamSequencerContext context ) {
        this.output = output;
        this.context = context;
    }

    /**
     * Get the sequencing context in which this reader is being used.
     * 
     * @return context the context; never null
     */
    public StreamSequencerContext getContext() {
        return context;
    }

    /**
     * Read the XML Schema Document from the supplied string, and produce the derived content. Any problems or exceptions are
     * written to the {@link #getContext() context's} {@link StreamSequencerContext#getProblems() problems}.
     * 
     * @param xsdContent the stream containing the XSD content; may not be null
     * @param docPath the path at which the derived content for the XSD should be written (usually this path represents the XSD
     *        file itself); may not be null
     */
    public void read( String xsdContent,
                      Path docPath ) {
        read(new InputSource(new StringReader(xsdContent)), docPath);
    }

    /**
     * Read the XML Schema Document from the supplied stream, and produce the derived content. Any problems or exceptions are
     * written to the {@link #getContext() context's} {@link StreamSequencerContext#getProblems() problems}.
     * 
     * @param stream the stream containing the XSD content; may not be null
     * @param docPath the path at which the derived content for the XSD should be written (usually this path represents the XSD
     *        file itself); may not be null
     */
    public void read( InputStream stream,
                      Path docPath ) {
        read(new InputSource(stream), docPath);
    }

    /**
     * Read the XML Schema Document from the supplied source, and produce the derived content. Any problems or exceptions are
     * written to the {@link #getContext() context's} {@link StreamSequencerContext#getProblems() problems}.
     * 
     * @param source the input source containing the XSD content; may not be null
     * @param docPath the path at which the derived content for the XSD should be written (usually this path represents the XSD
     *        file itself); may not be null
     */
    public void read( InputSource source,
                      Path docPath ) {
        logger.trace("Processing XSD '{0}'", string(docPath));
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
            T result = parse(source, docPath.getLastSegment().getName().getLocalName());

            // Convert the WSDL to content ...
            process(result, docPath, contentSize.get());

        } catch (Throwable e) {
            String location = context.getValueFactories().getStringFactory().create(context.getInputPath());
            context.getProblems().addError(e, WsdlI18n.errorReadingWsdlFile, location, e.getMessage());
        } finally {
            assert (reader != null && stream == null) || (reader == null && stream != null);
            try {
                if (reader != null) reader.close();
            } catch (Exception e) {
                String location = context.getValueFactories().getStringFactory().create(context.getInputPath());
                context.getProblems().addError(e, WsdlI18n.errorClosingWsdlFile, location, e.getMessage());
            } finally {
                try {
                    if (stream != null) stream.close();
                } catch (Exception e) {
                    String location = context.getValueFactories().getStringFactory().create(context.getInputPath());
                    context.getProblems().addError(e, WsdlI18n.errorClosingWsdlFile, location, e.getMessage());
                }
            }
        }
    }

    /**
     * Parse the supplied source (which contains either a {@link Reader} or an {@link InputStream}) and produce a representation
     * of the WSDL definition.
     * 
     * @param source the source containing the WSDL stream; never null
     * @param baseUri the URI of the document being read; never null or empty
     * @return the WSDL definition representation; may not be null
     * @throws Exception if there is a problem during parsing
     */
    protected abstract T parse( InputSource source,
                                String baseUri ) throws Exception;

    /**
     * Process the supplied representation of the WSDL definition that was returned from the {@link #parse(InputSource, String)}
     * method, derive the output content, and write that derived output content to the {@link #output SequencerOutput}.
     * 
     * @param parsedForm the representation of the WSDL definition, which will always be the value returned from
     *        {@link #parse(InputSource, String)}
     * @param docPath the path at which this method should generate the output structure derived from the WSDL representation
     * @param sizeOfFile the size of the WSDL stream, in bytes
     * @throws Exception if there is a problem during processing
     */
    protected abstract void process( T parsedForm,
                                     Path docPath,
                                     long sizeOfFile ) throws Exception;

    protected Path nextPath( Path parentPath,
                             Name name ) {
        Multimap<Name, Integer> names = namesByParentPath.get(parentPath);
        int sns = 1;
        if (names == null) {
            names = HashMultimap.create();
            names.put(name, sns);
            namesByParentPath.put(parentPath, names);
        } else {
            sns = names.get(name).size() + 1;
            names.put(name, 1);
        }
        return context.getValueFactories().getPathFactory().create(parentPath, name, sns);
    }

    protected Path path( Path parentPath,
                         Path subpath ) {
        return context.getValueFactories().getPathFactory().create(parentPath, subpath);
    }

    protected Path path( Path parentPath,
                         Name segment ) {
        return context.getValueFactories().getPathFactory().create(parentPath, segment);
    }

    protected Path path( Path parentPath,
                         String segment ) {
        return context.getValueFactories().getPathFactory().create(parentPath, segment);
    }

    protected Path path( String path ) {
        return context.getValueFactories().getPathFactory().create(path);
    }

    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    protected Name name( String namespaceUri,
                         String name ) {
        return context.getValueFactories().getNameFactory().create(namespaceUri, name);
    }

    protected String string( Object value ) {
        return context.getValueFactories().getStringFactory().create(value);
    }

    protected UUID setUuid( Path path ) {
        UUID uuid = context.getValueFactories().getUuidFactory().create();
        output.setProperty(path, JcrLexicon.UUID, uuid);
        return uuid;
    }

    protected void resolveReferences() {
        if (resolveFutures.isEmpty()) return;

        List<ResolveFuture> futures = resolveFutures;
        resolveFutures = new LinkedList<ResolveFuture>();
        for (ResolveFuture future : futures) {
            future.resolve(); // anything not resolved goes back on the new 'resolvedFutures' list ...
        }
    }

    protected String prefixForNamespace( String namespaceUri,
                                         String defaultPrefix ) {
        NamespaceRegistry registry = context.getNamespaceRegistry();
        String prefix = registry.getPrefixForNamespaceUri(namespaceUri, false);
        if (prefix == null) {
            if (defaultPrefix == null) {
                prefix = registry.getPrefixForNamespaceUri(namespaceUri, true);
            } else {
                int counter = 2;
                String proposedPrefix = defaultPrefix;
                while (registry.getNamespaceForPrefix(proposedPrefix) != null) {
                    proposedPrefix = defaultPrefix + counter++;
                }
                prefix = registry.register(proposedPrefix, namespaceUri);
            }
        }
        return prefix;
    }

    protected UUID setReference( Path path,
                                 Name propertyName,
                                 SymbolSpace kind,
                                 String namespace,
                                 String name ) {
        UUID typeUuid = resolvers.get(kind).lookup(namespace, name);
        if (typeUuid != null) {
            // The referenced object was already processed ...
            output.setProperty(path, propertyName, typeUuid);
        } else {
            // The referenced object may not have been processed, so put it in the queue ...
            resolveFutures.add(new ResolveFuture(path, propertyName, kind, namespace, name));
        }
        return typeUuid;
    }

    protected class ResolveFuture {
        private final Path path;
        private final Name propertyName;
        private final SymbolSpace refKind;
        private final String refNamespace;
        private final String refName;

        protected ResolveFuture( Path path,
                                 Name propertyName,
                                 SymbolSpace kind,
                                 String namespace,
                                 String name ) {
            this.path = path;
            this.propertyName = propertyName;
            this.refKind = kind;
            this.refNamespace = namespace;
            this.refName = name;
        }

        protected UUID resolve() {
            return setReference(path, propertyName, refKind, refNamespace, refName);
        }
    }

}
