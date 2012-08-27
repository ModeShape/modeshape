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
package org.modeshape.sequencer.teiid.model;

import java.util.HashMap;
import java.util.Map;
import javax.jcr.Node;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.sequencer.Sequencer.Context;
import org.modeshape.sequencer.teiid.VdbModel;
import org.modeshape.sequencer.teiid.lexicon.CoreLexicon;
import org.modeshape.sequencer.teiid.lexicon.DiagramLexicon;
import org.modeshape.sequencer.teiid.lexicon.JdbcLexicon;
import org.modeshape.sequencer.teiid.lexicon.RelationalLexicon;
import org.modeshape.sequencer.teiid.lexicon.TransformLexicon;
import org.modeshape.sequencer.teiid.xmi.XmiElement;

/**
 * Writes the JCR node structure for a model.
 */
public final class ModelNodeWriter {

    private static final boolean DEBUG = true;

    private static void debug( final String message ) {
        System.err.println(message);
    }

    private final Node outputNode;
    private final Map<String, Class<? extends ModelObjectHandler>> registry = new HashMap<String, Class<? extends ModelObjectHandler>>();
    private final Map<String, ModelObjectHandler> handlers = new HashMap<String, ModelObjectHandler>();
    private ReferenceResolver resolver;
    private final VdbModel vdbModel;
    private final ModelReader reader;
    private final Context context;

    /**
     * @param modelNode the model node where the output should be written to (cannot be <code>null</code>)
     * @param reader the model reader (cannot be <code>null</code>)
     * @param resolver the reference resolver (cannot be <code>null</code>)
     * @param vdbModel the VDB model (can be <code>null</code> if model did not come from a VDB)
     * @param context the sequencer context (cannot be <code>null</code>)
     */
    ModelNodeWriter( final Node modelNode,
                     final ModelReader reader,
                     final ReferenceResolver resolver,
                     final VdbModel vdbModel,
                     final Context context ) {
        CheckArg.isNotNull(modelNode, "modelNode");
        CheckArg.isNotNull(reader, "reader");
        CheckArg.isNotNull(resolver, "resolver");
        CheckArg.isNotNull(context, "context");

        this.outputNode = modelNode;
        this.resolver = resolver;
        this.reader = reader;
        this.vdbModel = vdbModel;
        this.context = context;
    }

    private ModelObjectHandler getHandler( final String namespaceUri ) throws Exception {
        CheckArg.isNotEmpty(namespaceUri, "namespaceUri");

        // see if handler has already been constructed
        ModelObjectHandler handler = this.handlers.get(namespaceUri);

        // construct if necessary
        if ((handler == null) && this.registry.containsKey(namespaceUri)) {
            final Class<? extends ModelObjectHandler> handlerClass = this.registry.get(namespaceUri);
            handler = handlerClass.newInstance();
            this.handlers.put(namespaceUri, handler);

            // set handler properties
            handler.setContext(this.context);
            handler.setReader(this.reader);
            handler.setResolver(this.resolver);
            handler.setVdbModel(this.vdbModel);
        }

        return handler;
    }

    public boolean isAcceptedPrimaryMetamodel( String uri ) {
        return RelationalLexicon.Namespace.URI.equals(uri);
    }

    private void loadRegistry() {
        this.registry.put(CoreLexicon.Namespace.URI, CoreModelObjectHandler.class);
        this.registry.put(DiagramLexicon.Namespace.URI, DiagramModelObjectHandler.class);
        this.registry.put(JdbcLexicon.Namespace.URI, JdbcModelObjectHandler.class);
        this.registry.put(RelationalLexicon.Namespace.URI, RelationalModelObjectHandler.class);
        this.registry.put(TransformLexicon.Namespace.URI, TransformationModelObjectHandler.class);
    }

    public boolean write() throws Exception {
        // use primary metamodel URI to determine if we should continue sequencing
        String primaryMetamodelUri = this.reader.getPrimaryMetamodelUri();

        if ((primaryMetamodelUri == null) || !isAcceptedPrimaryMetamodel(primaryMetamodelUri)) {
            return false;
        }

        // continue sequencing
        this.outputNode.addMixin(CoreLexicon.MODEL);

        // load model object handler registry
        loadRegistry();

        if (!writeModelObjects()) return false;
        // TODO might need to write default values at end
        return writeReferences();
    }

    private boolean writeModelObjects() throws Exception {
        for (XmiElement element : this.reader.getElements()) {
            String nsUri = element.getNamespaceUri();
            ModelObjectHandler handler = getHandler(nsUri);

            if (handler == null) {
                if (DEBUG) {
                    debug("ModelObjectHandler for namespace " + nsUri + " cannot be found");
                }

                continue;
            }

            handler.process(element, this.outputNode);
        }

        return true;
    }

    public boolean writeReferences() {
        // TODO implement
        // // Now attempt to resolve any references that were previously unresolved ...
        // for (Entry<String, Collection<String>> entry : this.resolver.getUnresolved().asMap().entrySet()) {
        // String propPath = entry.getKey();
        // Collection<String> mmuuids = entry.getValue();
        // Path path = propPath.getParent();
        // Name propName = propPath.getLastSegment().getName();
        // Object[] names = new String[mmuuids.size()];
        // int i = 0;
        //
        // for (UUID mmuuid : mmuuids) {
        // ResolvedReference ref = resolver.resolve(null, null, null, mmuuid);
        //
        // if (ref.getName() == null) {
        // names = null;
        // break;
        // }
        //
        // names[i++] = ref.getName();
        // }
        //
        // if (names != null && !useXmiUuidsAsJcrUuids) {
        // Name refNameName = nameForResolvedName(propName);
        // output.setProperty(path, refNameName, names);
        // }
        // }

        return true;
    }
}
