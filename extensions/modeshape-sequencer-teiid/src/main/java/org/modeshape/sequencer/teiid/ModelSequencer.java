/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
package org.modeshape.sequencer.teiid;

import java.io.InputStream;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.NamespaceRegistry.Namespace;
import org.modeshape.graph.property.basic.LocalNamespaceRegistry;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencer;
import org.modeshape.graph.sequencer.StreamSequencerContext;

/**
 * A sequencer of Teiid XMI model files.
 */
public class ModelSequencer implements StreamSequencer {

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.sequencer.StreamSequencer#sequence(java.io.InputStream,
     *      org.modeshape.graph.sequencer.SequencerOutput, org.modeshape.graph.sequencer.StreamSequencerContext)
     */
    public void sequence( InputStream stream,
                          SequencerOutput output,
                          StreamSequencerContext context ) {
        // Use a local namespace registry so that we know which namespaces were used ...
        NamespaceRegistry registry = context.getNamespaceRegistry();
        LocalNamespaceRegistry localRegistry = new LocalNamespaceRegistry(registry);
        context = context.with(localRegistry);

        Graph graph = Graph.create(context);
        try {
            // Register some of the namespaces we'll need ...
            localRegistry.register(DiagramLexicon.Namespace.PREFIX, DiagramLexicon.Namespace.URI);
            localRegistry.register(TransformLexicon.Namespace.PREFIX, TransformLexicon.Namespace.URI);

            // Load the input into the transient graph ...
            graph.importXmlFrom(stream).usingAttributeForName("name").into("/");

            // Now read the graph ...
            Subgraph subgraph = graph.getSubgraphOfDepth(5).at("/xmi:XMI");

            // Register any namespaces that were used, but use the desired case (not what's used in XMI) ...
            XmiModelReader reader = new XmiModelReader(context.getInputPath(), subgraph, true);
            for (Namespace namespace : localRegistry.getLocalNamespaces()) {
                String uri = namespace.getNamespaceUri();
                if (!registry.isRegisteredNamespaceUri(uri)) {
                    String prefix = reader.namespacePrefix(namespace.getPrefix());
                    registry.register(prefix, uri);
                    // And re-register so that the sequencing context uses the updated prefixes ...
                    localRegistry.register(prefix, uri);
                }
            }

            // Now process the input graph and output the desired format ...
            reader.writePhase1(output);
            reader.writePhase2(output);

        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            context.getProblems().addError(e, TeiidI18n.errorSequencingModelContent, e.getLocalizedMessage());
        } finally {
            try {
                stream.close();
            } catch (Throwable e) {
                context.getProblems().addError(e, TeiidI18n.errorSequencingModelContent, e.getLocalizedMessage());
            }
        }
    }
}
