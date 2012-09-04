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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import org.modeshape.common.collection.Multimap;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.JcrMixLexicon;
import org.modeshape.jcr.api.sequencer.Sequencer.Context;
import org.modeshape.sequencer.teiid.TeiidI18n;
import org.modeshape.sequencer.teiid.VdbModel;
import org.modeshape.sequencer.teiid.lexicon.CoreLexicon;
import org.modeshape.sequencer.teiid.lexicon.DiagramLexicon;
import org.modeshape.sequencer.teiid.lexicon.JdbcLexicon;
import org.modeshape.sequencer.teiid.lexicon.RelationalLexicon;
import org.modeshape.sequencer.teiid.lexicon.TransformLexicon;
import org.modeshape.sequencer.teiid.model.ReferenceResolver.UnresolvedProperty;
import org.modeshape.sequencer.teiid.model.ReferenceResolver.UnresolvedReference;
import org.modeshape.sequencer.teiid.xmi.XmiElement;

/**
 * Writes the JCR node structure for a model.
 */
public final class ModelNodeWriter {

    private static final boolean DEBUG = false;

    private static void debug( final String message ) {
        System.err.println(message);
    }

    private final Context context;
    private final Map<String, ModelObjectHandler> handlers = new HashMap<String, ModelObjectHandler>();
    private final Node outputNode;
    private final ModelReader reader;
    private final Map<String, Class<? extends ModelObjectHandler>> registry = new HashMap<String, Class<? extends ModelObjectHandler>>();
    private final ReferenceResolver resolver;
    private final VdbModel vdbModel;

    /**
     * @param modelNode the model node where the output should be written to (cannot be <code>null</code>)
     * @param reader the model reader (cannot be <code>null</code>)
     * @param resolver the reference resolver (cannot be <code>null</code>)
     * @param vdbModel the VDB model (can be <code>null</code> if model did not come from a VDB)
     * @param context the sequencer context (cannot be <code>null</code>)
     * @throws Exception if the model node does not have a model primary node type
     */
    ModelNodeWriter( final Node modelNode,
                     final ModelReader reader,
                     final ReferenceResolver resolver,
                     final VdbModel vdbModel,
                     final Context context ) throws Exception {
        CheckArg.isNotNull(modelNode, "modelNode");
        CheckArg.isNotNull(reader, "reader");
        CheckArg.isNotNull(resolver, "resolver");
        CheckArg.isNotNull(context, "context");

        if (!modelNode.isNodeType(CoreLexicon.JcrId.MODEL)) {
            throw new RuntimeException(TeiidI18n.invalidModelNodeType.text(modelNode.getPath()));
        }

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

    public boolean isAcceptedPrimaryMetamodel( final String uri ) {
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
        long startTime = System.currentTimeMillis();
        boolean result = true;

        // use primary metamodel URI to determine if we should continue sequencing
        final String primaryMetamodelUri = this.reader.getPrimaryMetamodelUri();

        if ((primaryMetamodelUri == null) || !isAcceptedPrimaryMetamodel(primaryMetamodelUri)) {
            result = false;
        } else {
            // load model object handler registry
            loadRegistry();

            if (!writeModelObjects()) {
                result = false;
            } else {
                result = writeUnresolvedReferences();
            }
        }

        if (DEBUG) {
            debug("\n\nmodel write time=" + (System.currentTimeMillis() - startTime));
        }

        return result;
    }

    private boolean writeModelObjects() throws Exception {
        if (DEBUG) {
            debug("\n\n[begin writeModelObjects()]");
        }

        for (final XmiElement element : this.reader.getElements()) {
            final String nsUri = element.getNamespaceUri();
            final ModelObjectHandler handler = getHandler(nsUri);

            if (handler == null) {
                if (DEBUG) {
                    debug("ModelObjectHandler for namespace " + nsUri + " cannot be found");
                }

                continue;
            }

            handler.process(element, this.outputNode);
        }

        if (DEBUG) {
            debug("[end writeModelObjects()]\n\n");
        }

        return true;
    }

    public boolean writeUnresolvedReferences() throws Exception {
        // TODO this is modifying the unresolved references it is processing. may need to have multiple passes?

        if (DEBUG) {
            debug("\n\n[begin writeUnresolvedReferences()]");
        }

        for (final Entry<String, UnresolvedReference> entry : this.resolver.getUnresolved().entrySet()) {
            final Node resolved = this.resolver.getNode(entry.getKey());

            if (resolved == null) {
                if (DEBUG) {
                    debug("**** uuid " + entry.getKey() + " is still unresolved during last phase of writing model");
                }

                continue;
            }

            final UnresolvedReference unresolved = entry.getValue();
            final ValueFactory valueFactory = resolved.getSession().getValueFactory();

            // add mixins
            for (final String mixin : unresolved.getMixins()) {
                resolved.addMixin(mixin);

                if (DEBUG) {
                    debug("adding mixin " + mixin + " to resolved node " + resolved.getName());
                }
            }

            { // add properties
                for (final String propName : unresolved.getProperties().keySet()) {
                    UnresolvedProperty property = unresolved.getProperties().get(propName);
                    assert (property != null);

                    boolean multiValued = property.isMulti();

                    if (multiValued) {
                        Value[] propertyValues = new Value[property.getValues().size()];
                        int i = 0;

                        for (String value : property.getValues()) {
                            propertyValues[i++] = valueFactory.createValue(value);
                        }
                    } else {
                        // single valued
                        resolved.setProperty(propName, property.getValue());

                        if (DEBUG) {
                            debug("setting property '" + propName + "' with value '" + property.getValue()
                                  + "' to resolved node " + resolved.getName());
                        }
                    }
                }
            }

            { // add weakreferences
                for (final String propertyName : unresolved.getReferences().keySet()) {
                    final Collection<String> refs = unresolved.getReferences().get(propertyName);

                    if ((refs == null) || refs.isEmpty()) {
                        continue;
                    }

                    boolean multiValued = false;

                    if (resolved.hasProperty(propertyName)) {
                        multiValued = resolved.getProperty(propertyName).isMultiple();

                        if (multiValued) {
                            Value[] values = new Value[refs.size()];
                            int i = 0;

                            for (final String value : refs) {
                                Node referencedNode = this.resolver.getNode(value);

                                if (referencedNode == null) {
                                    this.resolver.addUnresolvedReference(value);
                                } else {
                                    values[i++] = valueFactory.createValue(referencedNode, true);
                                }
                            }

                            resolved.setProperty(propertyName, values);
                        } else {
                            // single valued property so just use first value
                            resolved.setProperty(propertyName, refs.iterator().next());
                        }
                    } else {
                        // TODO find using prop defns
                        resolved.getPrimaryNodeType().getPropertyDefinitions();
                    }
                }
            }

            { // add referenced node name to referencer property
                for (final String propertyName : unresolved.getReferenceNames().keySet()) {
                    final Collection<String> referencerUuids = unresolved.getReferenceNames().get(propertyName);

                    if ((referencerUuids == null) || referencerUuids.isEmpty()) {
                        continue;
                    }

                    for (final String uuid : referencerUuids) {
                        Node referencerNode = this.resolver.getNode(uuid);

                        if (referencerNode == null) {
                            // referencer node is unresolved
                            UnresolvedReference unresolvedReferencer = this.resolver.addUnresolvedReference(uuid);
                            // TODO find a way to not assume multi-valued though they all are right now
                            unresolvedReferencer.addProperty(propertyName, resolved.getName(), true);
                        } else {
                            referencerNode.setProperty(propertyName,
                                                       new Value[] {this.context.valueFactory().createValue(resolved.getName())});
                        }
                    }
                }
            }

            { // referencer references
                Multimap<String, String> refRefs = unresolved.getReferencerReferences();

                for (final String propertyName : refRefs.keySet()) {
                    if (!resolved.isNodeType(JcrMixLexicon.REFERENCEABLE.getString())) {
                        resolved.addMixin(JcrMixLexicon.REFERENCEABLE.getString());
                    }

                    Value weakRef = valueFactory.createValue(resolved, true);

                    // property needs to get set with the weak reference of the resolved node
                    for (final String referencerUuuid : refRefs.get(propertyName)) {
                        Node referencer = this.resolver.getNode(referencerUuuid);

                        if (referencer == null) {
                            UnresolvedReference unresolvedReferencer = this.resolver.addUnresolvedReference(referencerUuuid);
                            unresolvedReferencer.addReference(propertyName, entry.getKey());
                        } else {
                            // TODO multi-valued
                            referencer.setProperty(propertyName, weakRef);
                        }
                    }
                }
            }

            // mark as resolved
            this.resolver.resolved(unresolved);
        }

        if (DEBUG) {
            debug("number unresolved at end=" + this.resolver.getUnresolved().size());
            debug("[end writeUnresolvedReferences()]\n\n");
        }

        return true;
    }
}
