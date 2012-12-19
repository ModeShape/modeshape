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

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import org.modeshape.common.collection.Multimap;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.JcrConstants;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Writes the JCR node structure for a model.
 */
public final class ModelNodeWriter {

    private static final Logger LOGGER = Logger.getLogger(ModelNodeWriter.class);

    private final Context context;
    private final Map<String, ModelObjectHandler> handlers = new HashMap<String, ModelObjectHandler>();
    private final Node outputNode;
    private final ModelReader reader;
    private final Map<String, Class<? extends ModelObjectHandler>> registry = new HashMap<String, Class<? extends ModelObjectHandler>>();
    private final ReferenceResolver resolver;
    private final VdbModel vdbModel;
    private final ModelExtensionDefinitionHelper medHelper;

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
        this.medHelper = new ModelExtensionDefinitionHelper(this);
    }

    Context getContext() {
        return this.context;
    }

    Node getModelNode() {
        return this.outputNode;
    }

    ReferenceResolver getResolver() {
        return this.resolver;
    }

    ModelObjectHandler getHandler( final String namespaceUri ) throws Exception {
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
            handler.setModelExtensionDefinitionHelper(this.medHelper);
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

                // add MED mixins to node
                this.medHelper.assignModelNodeChildrenMedMixins(this.outputNode);
            }
        }

        LOGGER.debug("model write time={0}\n\n", (System.currentTimeMillis() - startTime));

        return result;
    }

    private boolean writeModelObjects() throws Exception {
        LOGGER.debug("[begin writeModelObjects()]");

        for (final XmiElement element : this.reader.getElements()) {
            final String nsUri = element.getNamespaceUri();
            final ModelObjectHandler handler = getHandler(nsUri);

            if (handler == null) {
                LOGGER.debug("ModelObjectHandler for namespace {0} cannot be found", nsUri);
                continue;
            }

            handler.process(element, this.outputNode);
        }

        LOGGER.debug("[end writeModelObjects()]\n\n");
        return true;
    }

    public boolean writeUnresolvedReferences() throws Exception {
        LOGGER.debug("[begin writeUnresolvedReferences()]");

        // keep track of the unresolved references that have been resolved so that they can be marked as resolved later
        List<UnresolvedReference> resolvedReferences = new ArrayList<ReferenceResolver.UnresolvedReference>();

        for (final Entry<String, UnresolvedReference> entry : this.resolver.getUnresolved().entrySet()) {
            final Node resolved = this.resolver.getNode(entry.getKey());

            if (resolved == null) {
                LOGGER.debug("**** uuid {0} is still unresolved during last phase of writing model", entry.getKey());
                continue;
            }

            final UnresolvedReference unresolved = entry.getValue();
            final ValueFactory valueFactory = resolved.getSession().getValueFactory();

            // add mixins
            for (final String mixin : unresolved.getMixins()) {
                resolved.addMixin(mixin);
                LOGGER.debug("adding mixin {0} to resolved node {1}", mixin, resolved.getName());
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
                        final String mappedName = this.medHelper.getMappedPropertyName(propName);
                        resolved.setProperty(mappedName, property.getValue());
                        LOGGER.debug("setting property '{0}' with value '{1}' to resolved node {2}",
                                     propName,
                                     property.getValue(),
                                     resolved.getName());
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
                        LOGGER.debug("**** resolved property does not have property '{0}'. The value has {1}  reference(s) and first reference is '{2}'",
                                     propertyName,
                                     refs.size(),
                                     refs.iterator().next());
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
                            unresolvedReferencer.addProperty(propertyName, resolved.getName(), true);
                        } else {
                            referencerNode.setProperty(propertyName,
                                                       new Value[] { this.context.valueFactory().createValue(
                                                               resolved.getName()) });
                        }
                    }
                }
            }

            { // referencer references
                Multimap<String, String> refRefs = unresolved.getReferencerReferences();

                for (final String propertyName : refRefs.keySet()) {
                    if (!resolved.isNodeType(JcrConstants.MIX_REFERENCEABLE)) {
                        resolved.addMixin(JcrConstants.MIX_REFERENCEABLE);
                    }

                    Value weakRef = valueFactory.createValue(resolved, true);

                    // property needs to get set with the weak reference of the resolved node
                    for (final String referencerUuuid : refRefs.get(propertyName)) {
                        Node referencer = this.resolver.getNode(referencerUuuid);

                        if (referencer == null) {
                            UnresolvedReference unresolvedReferencer = this.resolver.addUnresolvedReference(referencerUuuid);
                            unresolvedReferencer.addReference(propertyName, entry.getKey());
                        } else {
                            if (referencer.hasProperty(propertyName)) {
                                Property prop = referencer.getProperty(propertyName);

                                if (prop.isMultiple()) {
                                    Value[] currentValues = prop.getValues();
                                    final Value[] newValues = new Value[currentValues.length + 1];
                                    System.arraycopy(currentValues, 0, newValues, 0, currentValues.length);
                                    newValues[currentValues.length] = weakRef;
                                    referencer.setProperty(propertyName, newValues);
                                } else {
                                    referencer.setProperty(propertyName, weakRef);
                                }
                            } else {
                                LOGGER.debug("**** weak reference property could be multi-value here");
                            }
                        }
                    }
                }
            }
        }

        // let resolver know the references were resolved
        if (!resolvedReferences.isEmpty()) {
            for (UnresolvedReference unresolved : resolvedReferences) {
                this.resolver.resolved(unresolved);
            }
        }

        LOGGER.debug("number unresolved at end={0}\n[end writeUnresolvedReferences()]\n\n", this.resolver.getUnresolved().size());

        return true;
    }
}
