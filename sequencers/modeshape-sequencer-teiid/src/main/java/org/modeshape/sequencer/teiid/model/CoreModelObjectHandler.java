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
import java.util.Arrays;
import java.util.Map.Entry;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Value;
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

    private boolean medModelTypeProcessed( final XmiElement tagElement,
                                           final String newModelType ) throws Exception {
        assert CoreLexicon.ModelId.TAGS.equals(tagElement.getName()) : "XMI element is not a tag element";

        // make sure annotated object points to the model types tag
        String uuid = tagElement.getParent().getAttributeValue(CoreLexicon.ModelId.ANNOTATED_OBJECT, URI);

        if (StringUtil.isBlank(uuid)) {
            return false;
        }

        // strip off the prefix if necessary
        uuid = getResolver().resolveInternalReference(uuid);

        // go get the tag XMI element
        final XmiElement referencedElement = getResolver().getUuidMappings().get(uuid);

        // make need to make sure referenced element is the for model types tag so follow the annotated object reference
        if ((referencedElement != null) && CoreLexicon.ModelId.TAGS.equals(referencedElement.getName())) {
            if (ModelExtensionDefinitionLexicon.ModelId.MODEL_TYPES.equals(referencedElement.getAttributeValue(CoreLexicon.ModelId.KEY,
                                                                                                               URI))) {
                // get med node by using parent's annotated object
                String medUuid = referencedElement.getParent().getAttributeValue(CoreLexicon.ModelId.ANNOTATED_OBJECT, URI);
                medUuid = getResolver().resolveInternalReference(medUuid);
                final Node medNode = getResolver().getNode(medUuid);

                if (medNode != null) {
                    final Value newValue = getContext().valueFactory().createValue(newModelType);
                    Value[] modelTypes = null;

                    if (medNode.hasProperty(ModelExtensionDefinitionLexicon.JcrId.MODEL_TYPES)) {
                        final Property property = medNode.getProperty(ModelExtensionDefinitionLexicon.JcrId.MODEL_TYPES);
                        final Value[] currentValues = property.getValues();
                        modelTypes = new Value[currentValues.length + 1];
                        System.arraycopy(currentValues, 0, modelTypes, 0, currentValues.length);
                        modelTypes[currentValues.length] = newValue;
                    } else {
                        modelTypes = new Value[] {newValue};
                    }

                    medNode.setProperty(ModelExtensionDefinitionLexicon.JcrId.MODEL_TYPES, modelTypes);
                    LOGGER.debug("added MED model type '{0}' to MED '{1}'", newModelType, medNode.getName());
                    return true;
                }
            }
        }

        return false;
    }

    private boolean medPropertyDefinitionDescriptionProcessed( final XmiElement tagElement,
                                                               final String newModelType ) throws Exception {
        assert CoreLexicon.ModelId.TAGS.equals(tagElement.getName()) : "XMI element is not a tag element";

        // make need to make sure annotated object points to the description tag so follow the annotated object reference
        String uuid = tagElement.getParent().getAttributeValue(CoreLexicon.ModelId.ANNOTATED_OBJECT, URI);

        if (StringUtil.isBlank(uuid)) {
            return false;
        }

        // strip off the prefix if necessary
        uuid = getResolver().resolveInternalReference(uuid);

        // go get the tag XMI element
        final XmiElement referencedElement = getResolver().getUuidMappings().get(uuid);

        // make sure referenced element is the for description tag
        if ((referencedElement != null) && CoreLexicon.ModelId.TAGS.equals(referencedElement.getName())) {
            if (ModelExtensionDefinitionLexicon.ModelId.DESCRIPTION.equals(referencedElement.getAttributeValue(CoreLexicon.ModelId.KEY,
                                                                                                               URI))) {
                // get property definition node by using parent's annotated object
                String propDefUuid = referencedElement.getParent().getAttributeValue(CoreLexicon.ModelId.ANNOTATED_OBJECT, URI);
                propDefUuid = getResolver().resolveInternalReference(propDefUuid);
                final Node propDefNode = getResolver().getNode(propDefUuid);

                if (propDefNode != null) {
                    final Node descriptionNode = addNode(propDefNode,
                                                         ModelExtensionDefinitionLexicon.JcrId.DESCRIPTION,
                                                         tagElement.getUuid(),
                                                         ModelExtensionDefinitionLexicon.JcrId.LOCALIZED_DESCRIPTION);
                    descriptionNode.setProperty(ModelExtensionDefinitionLexicon.JcrId.LOCALE,
                                                tagElement.getAttributeValue(CoreLexicon.ModelId.KEY, URI));
                    descriptionNode.setProperty(ModelExtensionDefinitionLexicon.JcrId.TRANSLATION,
                                                tagElement.getAttributeValue(CoreLexicon.ModelId.VALUE, URI));

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("set property definition description locale to '{0}' and translation to '{1}'",
                                     tagElement.getAttributeValue(CoreLexicon.ModelId.KEY, URI),
                                     tagElement.getAttributeValue(CoreLexicon.ModelId.VALUE, URI));
                    }

                    return true;
                }
            }
        }

        return false;
    }

    private boolean medPropertyDefinitionDisplayNameProcessed( final XmiElement tagElement,
                                                               final String newModelType ) throws Exception {
        assert CoreLexicon.ModelId.TAGS.equals(tagElement.getName()) : "XMI element is not a tag element";

        // make need to make sure annotated object points to the display name tag so follow the annotated object reference
        String uuid = tagElement.getParent().getAttributeValue(CoreLexicon.ModelId.ANNOTATED_OBJECT, URI);

        if (StringUtil.isBlank(uuid)) {
            return false;
        }

        // strip off the prefix if necessary
        uuid = getResolver().resolveInternalReference(uuid);

        // go get the tag XMI element
        final XmiElement referencedElement = getResolver().getUuidMappings().get(uuid);

        // make sure referenced element is the for the display name tag
        if ((referencedElement != null) && CoreLexicon.ModelId.TAGS.equals(referencedElement.getName())) {
            if (ModelExtensionDefinitionLexicon.ModelId.Property.DISPLAY_NAME.equals(referencedElement.getAttributeValue(CoreLexicon.ModelId.KEY,
                                                                                                                         URI))) {
                // get property definition node by using parent's annotated object
                String propDefUuid = referencedElement.getParent().getAttributeValue(CoreLexicon.ModelId.ANNOTATED_OBJECT, URI);
                propDefUuid = getResolver().resolveInternalReference(propDefUuid);
                final Node propDefNode = getResolver().getNode(propDefUuid);

                if (propDefNode != null) {
                    final Node displayNameNode = addNode(propDefNode,
                                                         ModelExtensionDefinitionLexicon.JcrId.Property.DISPLAY_NAME,
                                                         tagElement.getUuid(),
                                                         ModelExtensionDefinitionLexicon.JcrId.LOCALIZED_NAME);
                    displayNameNode.setProperty(ModelExtensionDefinitionLexicon.JcrId.LOCALE,
                                                tagElement.getAttributeValue(CoreLexicon.ModelId.KEY, URI));
                    displayNameNode.setProperty(ModelExtensionDefinitionLexicon.JcrId.TRANSLATION,
                                                tagElement.getAttributeValue(CoreLexicon.ModelId.VALUE, URI));

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("set property definition display name locale to '{0}' and translation to '{1}'",
                                     tagElement.getAttributeValue(CoreLexicon.ModelId.KEY, URI),
                                     tagElement.getAttributeValue(CoreLexicon.ModelId.VALUE, URI));
                    }

                    return true;
                }
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
                    for (final String importPath : getVdbModel().getImports()) {
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
                        for (final XmiElement child : annotation.getChildren()) {
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
                            final String key = child.getAttributeValue(CoreLexicon.ModelId.KEY, URI);

                            if (StringUtil.isBlank(key)) {
                                continue;
                            }

                            final String value = child.getAttributeValue(CoreLexicon.ModelId.VALUE, URI);
                            final String[] parts = key.split(":", 2); // part 0 = namespace prefix, part 2 = property name

                            // don't process if namespace is being ignored
                            if (isIgnoredTag(parts[0])) {
                                if (node != null) {
                                    LOGGER.debug("tag '{0}' not added as property of node '{1}'", key, node.getName());
                                } else if (unresolved != null) {
                                    LOGGER.debug("tag '{0}' not added as property of node '{1}'", key, unresolved.getUuid());
                                }

                                continue; // key should be ignored
                            }

                            if ((node != null) && LOGGER.isDebugEnabled()) {
                                LOGGER.debug("annotated object node name '{0}' has type of '{1}' and key of '{2}'",
                                             node.getName(),
                                             node.getPrimaryNodeType().getName(),
                                             key);
                            }

                            // see if a MED definition
                            if (ModelExtensionDefinitionLexicon.Utils.isModelMedTagKey(key)) {
                                // add MED group node to model if necessary
                                Node medGroupNode = null;

                                if (modelNode.hasNode(CoreLexicon.JcrId.MODEL_EXTENSION_DEFINITIONS_GROUP_NODE)) {
                                    medGroupNode = modelNode.getNode(CoreLexicon.JcrId.MODEL_EXTENSION_DEFINITIONS_GROUP_NODE);
                                } else {
                                    medGroupNode = addNode(modelNode,
                                                           CoreLexicon.JcrId.MODEL_EXTENSION_DEFINITIONS_GROUP_NODE,
                                                           null,
                                                           CoreLexicon.JcrId.MODEL_EXTENSION_DEFINITIONS_GROUP_NODE);
                                }

                                // add MED node
                                assert (medGroupNode != null) : "MED group node is null";
                                addNode(medGroupNode, parts[1], // namespace prefix
                                        child.getUuid(),
                                        ModelExtensionDefinitionLexicon.JcrId.MODEL_EXTENSION_DEFINITION);
                            } else if ((node != null)
                                       && ModelExtensionDefinitionLexicon.JcrId.MODEL_EXTENSION_DEFINITION.equals(node.getPrimaryNodeType().getName())) {
                                // a MED node should have at least one metaclass child nodes
                                if (ModelExtensionDefinitionLexicon.Utils.isModelMedMetaclassTagKey(key)) {
                                    addNode(node,
                                            parts[1],
                                            child.getUuid(),
                                            ModelExtensionDefinitionLexicon.JcrId.EXTENDED_METACLASS);
                                    LOGGER.debug("added metaclass node '{0}'", parts[1]);
                                } else if (ModelExtensionDefinitionLexicon.Utils.isModelMedModelTypesTagKey(key)) {
                                    // ignore as this is only used as the annotated object for when the actual model types
                                    // are found in the model
                                    continue;
                                } else {
                                    node.setProperty(ModelExtensionDefinitionLexicon.Utils.constructJcrName(key), value);
                                    LOGGER.debug("set MED property '{0}' to value '{1}'",
                                                 ModelExtensionDefinitionLexicon.Utils.constructJcrName(key),
                                                 value);

                                    // if both the prefix and URI have been found go ahead and register the namespace
                                    if (node.hasProperty(ModelExtensionDefinitionLexicon.JcrId.NAMESPACE_PREFIX)
                                        && node.hasProperty(ModelExtensionDefinitionLexicon.JcrId.NAMESPACE_URI)) {
                                        final NamespaceRegistry registry = node.getSession().getWorkspace().getNamespaceRegistry();

                                        if (!Arrays.asList(registry.getPrefixes()).contains(node.getProperty(ModelExtensionDefinitionLexicon.JcrId.NAMESPACE_PREFIX).getString())) {
                                            registry.registerNamespace(node.getProperty(ModelExtensionDefinitionLexicon.JcrId.NAMESPACE_PREFIX).getString(),
                                                                       node.getProperty(ModelExtensionDefinitionLexicon.JcrId.NAMESPACE_URI).getString());
                                        } else if (LOGGER.isDebugEnabled()) {
                                            LOGGER.debug("MED namespace '{0}' already registered",
                                                         node.getProperty(ModelExtensionDefinitionLexicon.JcrId.NAMESPACE_PREFIX).getString());
                                        }
                                    }
                                }
                            } else if ((node != null)
                                       && ModelExtensionDefinitionLexicon.JcrId.EXTENDED_METACLASS.equals(node.getPrimaryNodeType().getName())) {
                                // a MED node should have at least one metaclass child node
                                if (ModelExtensionDefinitionLexicon.Utils.isModelMedPropertyDefinitionTagKey(key)) {
                                    addNode(node,
                                            parts[1],
                                            child.getUuid(),
                                            ModelExtensionDefinitionLexicon.JcrId.PROPERTY_DEFINITION);
                                    LOGGER.debug("added MED property definition node '{0}'", parts[1]);
                                } else {
                                    node.setProperty(ModelExtensionDefinitionLexicon.Utils.constructJcrName(key), value);
                                    LOGGER.debug("set MED metaclass property '{0}' to value '{1}'",
                                                 ModelExtensionDefinitionLexicon.Utils.constructJcrName(key),
                                                 value);
                                }
                            } else if ((node != null)
                                       && ModelExtensionDefinitionLexicon.JcrId.PROPERTY_DEFINITION.equals(node.getPrimaryNodeType().getName())) {
                                // a MED node should have at least one property definition child node
                                if (ModelExtensionDefinitionLexicon.Utils.isModelMedPropertyDefinitionDescriptionTagKey(key)) {
                                    // handled later when the localized descriptions are processed
                                    continue;
                                } else if (ModelExtensionDefinitionLexicon.Utils.isModelMedPropertyDefinitionDisplayNameTagKey(key)) {
                                    // handled later when the localized display names are processed
                                    continue;
                                } else {
                                    node.setProperty(ModelExtensionDefinitionLexicon.Utils.constructJcrName(key), value);
                                    LOGGER.debug("set MED property definition property '{0}' to value '{1}'",
                                                 ModelExtensionDefinitionLexicon.Utils.constructJcrName(key),
                                                 value);
                                }
                            } else if ((unresolved != null) && medModelTypeProcessed(child, key)) {
                                // added model type to MED
                                continue;
                            } else if ((unresolved != null) && medPropertyDefinitionDescriptionProcessed(child, key)) {
                                // added property definition description to MED
                                continue;
                            } else if ((unresolved != null) && medPropertyDefinitionDisplayNameProcessed(child, key)) {
                                // added property definition display name to MED
                                continue;
                            } else {
                                if (!hasTags) {
                                    hasTags = true;

                                    if (node != null) {
                                        node.addMixin(JcrId.TAGS);
                                    } else if (unresolved != null) {
                                        unresolved.addMixin(JcrId.TAGS);
                                    }
                                }

                                if (node != null) {
                                    node.setProperty(key, value);
                                } else if (unresolved != null) {
                                    unresolved.addProperty(key, value, false);
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
}
