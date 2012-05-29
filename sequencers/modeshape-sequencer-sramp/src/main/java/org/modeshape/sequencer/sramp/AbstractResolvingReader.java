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

package org.modeshape.sequencer.sramp;

import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.xml.sax.InputSource;

/**
 * Base class for the S-RAMP based readers, which hold the functionality for resolving references and registering namespaces
 * 
 * @author Horia Chiorean
 */
public abstract class AbstractResolvingReader {

    private final SymbolSpaceResolvers resolvers;
    private List<ResolveFuture> resolveFutures = new LinkedList<ResolveFuture>();

    protected final Sequencer.Context context;
    protected final Logger logger = Logger.getLogger(getClass());

    public AbstractResolvingReader( Sequencer.Context context,
                                    SymbolSpaceResolvers resolvers ) {
        CheckArg.isNotNull(context, "context");
        this.context = context;
        this.resolvers = resolvers != null ? resolvers : new SymbolSpaceResolvers();
    }

    public AbstractResolvingReader( Sequencer.Context context ) {
        this(context, null);
    }

    public SymbolSpaceResolvers getResolvers() {
        return resolvers;
    }

    /**
     * Get the sequencing context in which this reader is being used.
     * 
     * @return context the context; never null
     */
    public Sequencer.Context getContext() {
        return context;
    }

    /**
     * Read the document from the supplied stream, and produce the derived content.
     * 
     * @param stream the stream; may not be null
     * @param outputNode the parent node at which the derived content should be written; may not be null
     * @throws Exception if there is a problem reading the XSD content
     */
    public void read( InputStream stream,
                      Node outputNode ) throws Exception {
        read(new InputSource(stream), outputNode);
    }

    /**
     * Read the document from the supplied stream, and produce the derived content.
     * 
     * @param source the input source of the document; may not be null
     * @param outputNode the parent node at which the derived content should be written; may not be null
     * @throws Exception if there is a problem reading the XSD content
     */
    public abstract void read( InputSource source,
                               Node outputNode ) throws Exception;

    /**
     * @param symbolSpace the symbol space; may not be null
     * @param namespace the namespace URI; may not be null
     * @param name the name
     * @param identifier the identifier
     * @see NamespaceEntityResolver#register(String, String, String)
     */
    protected void registerForSymbolSpace( SymbolSpace symbolSpace,
                                           String namespace,
                                           String name,
                                           String identifier ) {
        resolvers.get(symbolSpace).register(namespace, name, identifier);
    }

    protected String registerNamespace( NamespaceRegistry registry,
                                        String namespaceUri,
                                        String defaultPrefix ) throws RepositoryException {
        List<String> allNamespaces = Arrays.asList(registry.getURIs());
        if (allNamespaces.contains(namespaceUri)) {
            return registry.getPrefix(namespaceUri);
        }

        List<String> allPrefixes = Arrays.asList(registry.getPrefixes());
        if (!allPrefixes.contains(defaultPrefix)) {
            registry.registerNamespace(defaultPrefix, namespaceUri);
            return defaultPrefix;
        }

        String generatedPrefix = generateNamespacePrefix(registry);
        registry.registerNamespace(generatedPrefix, namespaceUri);
        return generatedPrefix;
    }

    /**
     * Attempt to resolve any references that remain unresolved. This should be called if sharing a
     * {@link org.modeshape.sequencer.sramp.SymbolSpaceResolvers} with multiple {@link AbstractResolvingReader} instances.
     * 
     * @throws RepositoryException if there is a problem resolving references in the repository
     */
    public void resolveReferences() throws RepositoryException {
        if (resolveFutures.isEmpty()) return;

        List<ResolveFuture> futures = resolveFutures;
        resolveFutures = new LinkedList<ResolveFuture>();
        for (ResolveFuture future : futures) {
            future.resolve(); // anything not resolved goes back on the new 'resolvedFutures' list ...
        }
    }

    protected String setReference( Node node,
                                   String propertyName,
                                   SymbolSpace kind,
                                   String namespace,
                                   String name ) throws RepositoryException {
        String typeIdentifier = resolvers.get(kind).lookupIdentifier(namespace, name);
        if (typeIdentifier != null) {
            // The referenced object was already processed ...
            node.setProperty(propertyName, typeIdentifier);
        } else {
            // The referenced object may not have been processed, so put it in the queue ...
            resolveFutures.add(new ResolveFuture(node, propertyName, kind, namespace, name));
        }
        return typeIdentifier;
    }

    private String generateNamespacePrefix( NamespaceRegistry registry ) throws RepositoryException {
        String basePrefix = "ns";

        String prefix = basePrefix;
        int counter = 1;
        List<String> prefixes = Arrays.asList(registry.getPrefixes());
        while (prefixes.contains(prefix)) {
            prefix = basePrefix + counter;
            counter++;
        }
        return prefix;
    }

    private class ResolveFuture {
        private final Node node;
        private final String propertyName;
        private final SymbolSpace refKind;
        private final String refNamespace;
        private final String refName;

        protected ResolveFuture( Node node,
                                 String propertyName,
                                 SymbolSpace kind,
                                 String namespace,
                                 String name ) {
            this.node = node;
            this.propertyName = propertyName;
            this.refKind = kind;
            this.refNamespace = namespace;
            this.refName = name;
        }

        protected String resolve() throws RepositoryException {
            return setReference(node, propertyName, refKind, refNamespace, refName);
        }
    }
}
