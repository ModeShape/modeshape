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

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import javax.jcr.Node;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.sequencer.teiid.VdbModel;
import org.modeshape.sequencer.teiid.VdbModel.ValidationMarker;
import org.modeshape.sequencer.teiid.lexicon.CoreLexicon;
import org.modeshape.sequencer.teiid.lexicon.ModelExtensionDefinitionLexicon;
import org.modeshape.sequencer.teiid.lexicon.VdbLexicon;
import org.modeshape.sequencer.teiid.lexicon.XmiLexicon;
import org.modeshape.sequencer.teiid.xmi.XmiElement;

/**
 * The model object handler for the {@link org.modeshape.sequencer.teiid.lexicon.CoreLexicon.Namespace#URI core} namespace.
 */
public final class CoreModelObjectHandler extends ModelObjectHandler {

    private static final String[] IGNORED_MODEL_ANNOTATION_TAG_PREFIXES = {"connection", "connectionProfile", "translator"};
    private static final String URI = CoreLexicon.Namespace.URI;

    private final List<String> meds = new ArrayList<String>();

    private boolean isIgnoredTag( final Node node,
                                  final String nsPrefix ) throws Exception {
        CheckArg.isNotNull(node, "node");
        CheckArg.isNotEmpty(nsPrefix, "nsPrefix");

        if (node.isNodeType(CoreLexicon.MODEL) || node.isNodeType(VdbLexicon.Model.MODEL)) {
            for (final String prefix : IGNORED_MODEL_ANNOTATION_TAG_PREFIXES) {
                if (prefix.equals(nsPrefix)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.teiid.model.ModelObjectHandler#process(org.modeshape.sequencer.teiid.xmi.XmiElement, javax.jcr.Node)
     */
    @Override
    protected void process( final XmiElement element,
                            final Node modelNode ) throws Exception {
        CheckArg.isNotNull(element, "element");
        CheckArg.isNotNull(modelNode, "node");
        CheckArg.isEquals(element.getNamespaceUri(), "namespace URI", URI, "relational URI");

        if (DEBUG) {
            debug("==== CoreModelObjectHandler:process:element=" + element.getName());
        }

        final String type = element.getName();
        if (CoreLexicon.ModelIds.MODEL_ANNOTATION.equals(type)) {
            setProperty(modelNode, XmiLexicon.UUID, element.getUuid());
            getResolver().record(element.getUuid(), modelNode);

            final ModelReader reader = getReader();
            setProperty(modelNode, CoreLexicon.MODEL_TYPE, reader.getModelType());
            setProperty(modelNode, CoreLexicon.PRIMARY_METAMODEL_URI, reader.getPrimaryMetamodelUri());
            setProperty(modelNode, CoreLexicon.DESCRIPTION, reader.getDescription());
            setProperty(modelNode, CoreLexicon.NAME_IN_SOURCE, reader.getNameInSource());
            modelNode.setProperty(CoreLexicon.MAX_SET_SIZE, reader.getMaxSetSize());
            modelNode.setProperty(CoreLexicon.VISIBLE, reader.isVisible());
            modelNode.setProperty(CoreLexicon.SUPPORTS_DISTINCT, reader.supportsDistinct());
            modelNode.setProperty(CoreLexicon.SUPPORTS_JOIN, reader.supportsJoin());
            modelNode.setProperty(CoreLexicon.SUPPORTS_ORDER_BY, reader.supportsOrderBy());
            modelNode.setProperty(CoreLexicon.SUPPORTS_OUTER_JOIN, reader.supportsOuterJoin());
            modelNode.setProperty(CoreLexicon.SUPPORTS_WHERE_ALL, reader.supportsWhereAll());
            setProperty(modelNode, CoreLexicon.PRODUCER_NAME, reader.getProducerName());
            setProperty(modelNode, CoreLexicon.PRODUCER_VERSION, reader.getProducerVersion());
            setProperty(modelNode, CoreLexicon.ORIGINAL_FILE, reader.getPath());

            if (getVdbModel() != null) {
                final VdbModel vdbModel = getVdbModel();
                modelNode.setProperty(VdbLexicon.Model.VISIBLE, vdbModel.isVisible());
                modelNode.setProperty(VdbLexicon.Model.CHECKSUM, vdbModel.getChecksum());
                modelNode.setProperty(VdbLexicon.Model.BUILT_IN, vdbModel.isBuiltIn());
                setProperty(modelNode, VdbLexicon.Model.DESCRIPTION, vdbModel.getDescription());
                setProperty(modelNode, VdbLexicon.Model.PATH_IN_VDB, vdbModel.getPathInVdb());
                setProperty(modelNode, VdbLexicon.Model.SOURCE_TRANSLATOR, vdbModel.getSourceTranslator());
                setProperty(modelNode, VdbLexicon.Model.SOURCE_NAME, vdbModel.getSourceName());
                setProperty(modelNode, VdbLexicon.Model.SOURCE_JNDI_NAME, vdbModel.getSourceJndiName());
                setProperty(modelNode, VdbLexicon.Model.TYPE, vdbModel.getType());

                // write out any model properties from vdb.xml file
                for (final Entry<String, String> entry : vdbModel.getProperties().entrySet()) {
                    setProperty(modelNode, entry.getKey(), entry.getValue());
                }

                // write out and validation markers
                if (!vdbModel.getProblems().isEmpty()) {
                    // add group node
                    final Node markersGroupNode = modelNode.addNode(VdbLexicon.Model.MARKERS, VdbLexicon.Model.MARKERS);

                    for (final ValidationMarker marker : vdbModel.getProblems()) {
                        final Node markerNode = markersGroupNode.addNode(VdbLexicon.Model.Marker.MARKER,
                                                                         VdbLexicon.Model.Marker.MARKER);
                        setProperty(markerNode, VdbLexicon.Model.Marker.PATH, marker.getPath());
                        setProperty(markerNode, VdbLexicon.Model.Marker.MESSAGE, marker.getMessage());
                        setProperty(markerNode, VdbLexicon.Model.Marker.SEVERITY, marker.getSeverity().toString());
 
                        if (DEBUG) {
                            debug("added validation marker at path " + marker.getPath() + " and with severity of "
                                  + marker.getSeverity());
                        }
                    }
                }
            }

            // process model imports
            for (final XmiElement modelImport : reader.getModelImports()) {
                final Node importNode = addNode(modelNode, modelImport, URI, CoreLexicon.IMPORT);

                setProperty(importNode,
                            CoreLexicon.MODEL_TYPE,
                            modelImport.getAttributeValue(CoreLexicon.ModelIds.MODEL_TYPE, CoreLexicon.Namespace.URI));
                setProperty(importNode,
                            CoreLexicon.PRIMARY_METAMODEL_URI,
                            modelImport.getAttributeValue(CoreLexicon.ModelIds.PRIMARY_METAMODEL_URI, CoreLexicon.Namespace.URI));
                setProperty(importNode,
                            CoreLexicon.PATH,
                            modelImport.getAttributeValue(CoreLexicon.ModelIds.PATH, CoreLexicon.Namespace.URI));
                setProperty(importNode,
                            CoreLexicon.MODEL_LOCATION,
                            modelImport.getAttributeValue(CoreLexicon.ModelIds.MODEL_LOCATION, CoreLexicon.Namespace.URI));
            }

            if (DEBUG) {
                debug("[end writing model annotation]");
            }
        } else if (CoreLexicon.ModelIds.ANNOTATION_CONTAINER.equals(type)) {
            for (final XmiElement annotation : element.getChildren()) {
                if (CoreLexicon.ModelIds.ANNOTATION.equals(annotation.getName())) {
                    final String uuid = annotation.getAttributeValue(CoreLexicon.ModelIds.ANNOTATED_OBJECT,
                                                                     CoreLexicon.Namespace.URI);

                    if (StringUtil.isBlank(uuid)) {
                        if (DEBUG) {
                            debug("annotated object UUID is blank");
                        }

                        continue;
                    }

                    final Node node = getResolver().getNode(uuid);

                    if (node == null) {
                        if (DEBUG) {
                            debug("adding unresolved reference " + uuid);
                        }

                        // TODO add to unresolved and set properties
                        continue;
                    }

                    node.addMixin(CoreLexicon.ANNOTATED);
                    // TODO description and keywords

                    // Process the tags ...
                    node.addMixin(CoreLexicon.TAGS);

                    for (final XmiElement tag : annotation.getChildren()) {
                        if (CoreLexicon.ModelIds.TAGS.equals(tag.getName())) {
                            boolean addProperty = false;
                            final String key = tag.getAttributeValue(CoreLexicon.ModelIds.KEY, CoreLexicon.Namespace.URI);
                            final String[] parts = key.split(":", 2);

                            // check to see if tag namespace has been registered
                            if (parts.length == 1) {
                                addProperty = true;
                            } else {
                                // tag has a namespace
                                final String nsPrefix = parts[0];

                                // see if ignored prefix
                                // TODO remove MED stuff here when MED sequencing is implemented
                                if (!isIgnoredTag(node, nsPrefix) && !this.meds.contains(nsPrefix)) {
                                    if (ModelExtensionDefinitionLexicon.Namespace.PREFIX.equals(nsPrefix)) {
                                        registerMed(parts[1]);
                                    } else {
                                        addProperty = true;
                                    }
                                }
                            }

                            if (addProperty) {
                                final String value = tag.getAttributeValue(CoreLexicon.ModelIds.VALUE, CoreLexicon.Namespace.URI);
                                node.setProperty(key, value);
                            } else if (DEBUG) {
                                debug("tag " + key + " not added as property of node " + node.getName());
                            }
                        } else if (DEBUG) {
                            debug("Unexpected element processing an annotation: " + tag.getName());
                        }
                    }
                }
            }
        } else {
            if (DEBUG) {
                debug("**** core type of " + type + " was not processed");
            }
        }
    }

    private void registerMed( final String medPrefix ) {
        assert (medPrefix != null);
        this.meds.add(medPrefix);

        if (DEBUG) {
            debug("registered MED " + medPrefix);
        }
    }

}
