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

import static org.modeshape.sequencer.teiid.lexicon.CoreLexicon.Namespace.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import javax.jcr.Node;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.sequencer.teiid.VdbModel;
import org.modeshape.sequencer.teiid.VdbModel.ValidationMarker;
import org.modeshape.sequencer.teiid.lexicon.CoreLexicon;
import org.modeshape.sequencer.teiid.lexicon.CoreLexicon.JcrId;
import org.modeshape.sequencer.teiid.lexicon.ModelExtensionDefinitionLexicon;
import org.modeshape.sequencer.teiid.lexicon.VdbLexicon;
import org.modeshape.sequencer.teiid.lexicon.XmiLexicon;
import org.modeshape.sequencer.teiid.model.ReferenceResolver.UnresolvedReference;
import org.modeshape.sequencer.teiid.xmi.XmiElement;

/**
 * The model object handler for the {@link org.modeshape.sequencer.teiid.lexicon.CoreLexicon.Namespace#URI core} namespace.
 */
public final class CoreModelObjectHandler extends ModelObjectHandler {

    /**
     * Tags not to sequence and ignore. These prefixes are not registered.
     */
    private static final String[] IGNORED_MODEL_ANNOTATION_TAG_PREFIXES = {"connection", "connectionProfile", "translator"};

    private final List<String> meds = new ArrayList<String>();

    /**
     * @param nsPrefix the prefix being checked (cannot be <code>null</code> or empty)
     * @return <code>true</code> if the node is a VDB model or a Core model and the specified prefix is being ignored
     * @throws Exception if there is a problem obtaining node type information from the node
     */
    private boolean isIgnoredTag( final String nsPrefix ) throws Exception {
        CheckArg.isNotEmpty(nsPrefix, "nsPrefix");

        for (final String prefix : IGNORED_MODEL_ANNOTATION_TAG_PREFIXES) {
            if (prefix.equals(nsPrefix)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @see org.modeshape.sequencer.teiid.model.ModelObjectHandler#process(org.modeshape.sequencer.teiid.xmi.XmiElement,
     *      javax.jcr.Node)
     */
    @Override
    protected void process( final XmiElement element,
                            final Node modelNode ) throws Exception {
        CheckArg.isNotNull(element, "element");
        CheckArg.isNotNull(modelNode, "node");
        CheckArg.isEquals(element.getNamespaceUri(), "namespace URI", URI, "relational URI");

        LOGGER.debug("==== CoreModelObjectHandler:process:element={0}", element.getName());

        final String type = element.getName();
        if (CoreLexicon.ModelId.MODEL_ANNOTATION.equals(type)) {
            setProperty(modelNode, XmiLexicon.JcrId.UUID, element.getUuid());
            getResolver().record(element.getUuid(), modelNode);

            final ModelReader reader = getReader();
            setProperty(modelNode, JcrId.MODEL_TYPE, reader.getModelType());
            setProperty(modelNode, JcrId.PRIMARY_METAMODEL_URI, reader.getPrimaryMetamodelUri());
            setProperty(modelNode, JcrId.DESCRIPTION, reader.getDescription());
            setProperty(modelNode, JcrId.NAME_IN_SOURCE, reader.getNameInSource());
            modelNode.setProperty(JcrId.MAX_SET_SIZE, reader.getMaxSetSize());
            modelNode.setProperty(JcrId.VISIBLE, reader.isVisible());
            modelNode.setProperty(JcrId.SUPPORTS_DISTINCT, reader.supportsDistinct());
            modelNode.setProperty(JcrId.SUPPORTS_JOIN, reader.supportsJoin());
            modelNode.setProperty(JcrId.SUPPORTS_ORDER_BY, reader.supportsOrderBy());
            modelNode.setProperty(JcrId.SUPPORTS_OUTER_JOIN, reader.supportsOuterJoin());
            modelNode.setProperty(JcrId.SUPPORTS_WHERE_ALL, reader.supportsWhereAll());
            setProperty(modelNode, JcrId.PRODUCER_NAME, reader.getProducerName());
            setProperty(modelNode, JcrId.PRODUCER_VERSION, reader.getProducerVersion());
            setProperty(modelNode, JcrId.ORIGINAL_FILE, reader.getPath());

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

                        LOGGER.debug("added validation marker at path {0} and with severity of {1}",
                                     marker.getPath(),
                                     marker.getSeverity());
                    }
                }
            }

            // process model imports
            for (final XmiElement modelImport : reader.getModelImports()) {
                final Node importNode = addNode(modelNode, modelImport, URI, JcrId.IMPORT);

                setProperty(importNode, JcrId.MODEL_TYPE, modelImport.getAttributeValue(CoreLexicon.ModelId.MODEL_TYPE, URI));
                setProperty(importNode,
                            JcrId.PRIMARY_METAMODEL_URI,
                            modelImport.getAttributeValue(CoreLexicon.ModelId.PRIMARY_METAMODEL_URI, URI));
                setProperty(importNode,
                            JcrId.MODEL_LOCATION,
                            modelImport.getAttributeValue(CoreLexicon.ModelId.MODEL_LOCATION, URI));

                if (getVdbModel() != null) {
                    for (String importPath : getVdbModel().getImports()) {
                        if (importPath.endsWith(modelImport.getAttributeValue(CoreLexicon.ModelId.MODEL_LOCATION, URI))) {
                            setProperty(importNode, JcrId.PATH, importPath);
                            break;
                        }
                    }
                }
            }

            LOGGER.debug("[end writing model annotation]");
        } else if (CoreLexicon.ModelId.ANNOTATION_CONTAINER.equals(type)) {
            for (final XmiElement annotation : element.getChildren()) {
                if (CoreLexicon.ModelId.ANNOTATION.equals(annotation.getName())) {
                    String uuid = annotation.getAttributeValue(CoreLexicon.ModelId.ANNOTATED_OBJECT, URI);

                    if (StringUtil.isBlank(uuid)) {
                        // see if there is an annotated object child
                        for (XmiElement child : annotation.getChildren()) {
                            if (CoreLexicon.ModelId.ANNOTATED_OBJECT.equals(child.getName())) {
                                uuid = child.getAttributeValue(CoreLexicon.ModelId.HREF, URI);
                                break;
                            }
                        }
                    }

                    // remove any UUID prefix
                    uuid = getResolver().resolveInternalReference(uuid);

                    if (StringUtil.isBlank(uuid)) {
                        LOGGER.debug("annotated object UUID is blank");
                        continue;
                    }

                    final Node node = getResolver().getNode(uuid);
                    final UnresolvedReference unresolved = ((node == null) ? getResolver().addUnresolvedReference(uuid) : null);

                    // description
                    final String description = annotation.getAttributeValue(CoreLexicon.ModelId.DESCRIPTION, URI);

                    if (!StringUtil.isBlank(description)) {
                        if (node != null) {
                            node.addMixin(JcrId.ANNOTATED);
                            setProperty(node, JcrId.DESCRIPTION, description);
                        } else if (unresolved != null) {
                            unresolved.addMixin(JcrId.ANNOTATED);
                            unresolved.addProperty(JcrId.DESCRIPTION, description, false);
                        }
                    }

                    // keywords
                    final String keywordsValue = annotation.getAttributeValue(CoreLexicon.ModelId.KEYWORD, URI);

                    if (!StringUtil.isBlank(keywordsValue)) {
                        if ((node != null) && !node.isNodeType(JcrId.ANNOTATED)) {
                            node.addMixin(JcrId.ANNOTATED);
                        } else if (unresolved != null) {
                            unresolved.addMixin(JcrId.ANNOTATED);
                        }

                        for (final String keyword : keywordsValue.split("\\s")) {
                            if (node != null) {
                                addPropertyValue(node, JcrId.KEYWORDS, keyword);
                            } else if (unresolved != null) {
                                unresolved.addProperty(JcrId.KEYWORDS, keyword, true);
                            }
                        }
                    }

                    for (final XmiElement child : annotation.getChildren()) {
                        boolean hasTags = false;

                        if (CoreLexicon.ModelId.TAGS.equals(child.getName())) {
                            boolean addProperty = false;
                            final String key = child.getAttributeValue(CoreLexicon.ModelId.KEY, URI);

                            if (StringUtil.isBlank(key)) {
                                continue;
                            }

                            final String[] parts = key.split(":", 2);

                            // just add if no namespace
                            if (parts.length == 1) {
                                addProperty = true;
                            } else {
                                // tag has a namespace, see if it has been registered
                                final String nsPrefix = parts[0];

                                // see if ignored prefix
                                // TODO remove MED stuff here when MED sequencing is implemented
                                if (!isIgnoredTag(nsPrefix) && !this.meds.contains(nsPrefix)) {
                                    if (ModelExtensionDefinitionLexicon.Namespace.PREFIX.equals(nsPrefix)) {
                                        registerMed(parts[1]);
                                    } else {
                                        addProperty = true;
                                    }
                                }
                            }

                            if (addProperty) {
                                if (!hasTags) {
                                    hasTags = true;

                                    if (node != null) {
                                        node.addMixin(JcrId.TAGS);
                                    } else if (unresolved != null) {
                                        unresolved.addMixin(JcrId.TAGS);
                                    }
                                }

                                final String value = child.getAttributeValue(CoreLexicon.ModelId.VALUE, URI);

                                if (node != null) {
                                    node.setProperty(key, value);
                                } else if (unresolved != null) {
                                    unresolved.addProperty(key, value, false);
                                }
                            } else {
                                if (node != null) {
                                    LOGGER.debug("tag '{0}' not added as property of node '{1}'", key,  node.getName());
                                } else if (unresolved != null) {
                                    LOGGER.debug("tag '{0}' not added as property of node '{1}'", key, unresolved.getUuid());

                                }
                            }
                        } else {
                            LOGGER.debug("Unexpected element processing an annotation: {0}", child.getName());
                        }
                    }
                }
            }
        } else {
            LOGGER.debug("**** core type of '{0}' was not processed", type);
        }
    }

    private void registerMed( final String medPrefix ) {
        assert (medPrefix != null);
        this.meds.add(medPrefix);
        LOGGER.debug("registered MED '{0}'", medPrefix);
    }
}
