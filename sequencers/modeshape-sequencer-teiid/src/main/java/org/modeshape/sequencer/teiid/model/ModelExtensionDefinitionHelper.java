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
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import org.modeshape.common.collection.UnmodifiableProperties;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.modeshape.sequencer.teiid.TeiidI18n;
import org.modeshape.sequencer.teiid.lexicon.CoreLexicon;
import org.modeshape.sequencer.teiid.lexicon.ModelExtensionDefinitionLexicon.JcrId;
import org.modeshape.sequencer.teiid.lexicon.ModelExtensionDefinitionLexicon.ModelId;
import org.modeshape.sequencer.teiid.lexicon.ModelExtensionDefinitionLexicon.Utils;
import org.modeshape.sequencer.teiid.model.ReferenceResolver.UnresolvedReference;
import org.modeshape.sequencer.teiid.xmi.XmiElement;

/**
 * Helps with processing model MEDs.
 */
public class ModelExtensionDefinitionHelper {

    private static final Logger LOGGER = Logger.getLogger(ModelExtensionDefinitionHelper.class);
    private static final Map<String, Integer> TYPE_MAPPINGS;

    /**
     * A map of node type definition to full metaclass name used in MEDs.
     */
    private static final Properties METACLASS_MAPPINGS;

    static {
        METACLASS_MAPPINGS = loadMetaclassMappings();

        TYPE_MAPPINGS = new HashMap<String, Integer>();
        TYPE_MAPPINGS.put("biginteger", PropertyType.LONG);
        TYPE_MAPPINGS.put("bigdecimal", PropertyType.DOUBLE);
        TYPE_MAPPINGS.put("blob", PropertyType.BINARY);
        TYPE_MAPPINGS.put("boolean", PropertyType.BOOLEAN);
        TYPE_MAPPINGS.put("byte", PropertyType.LONG);
        TYPE_MAPPINGS.put("char", PropertyType.STRING);
        TYPE_MAPPINGS.put("clob", PropertyType.BINARY);
        TYPE_MAPPINGS.put("date", PropertyType.DATE);
        TYPE_MAPPINGS.put("double", PropertyType.DOUBLE);
        TYPE_MAPPINGS.put("float", PropertyType.DOUBLE);
        TYPE_MAPPINGS.put("integer", PropertyType.LONG);
        TYPE_MAPPINGS.put("long", PropertyType.LONG);
        TYPE_MAPPINGS.put("object", PropertyType.BINARY);
        TYPE_MAPPINGS.put("short", PropertyType.LONG);
        TYPE_MAPPINGS.put("string", PropertyType.STRING);
        TYPE_MAPPINGS.put("time", PropertyType.DATE);
        TYPE_MAPPINGS.put("timestamp", PropertyType.DATE);
        TYPE_MAPPINGS.put("xml", PropertyType.STRING);
    }

    private static Properties loadMetaclassMappings() {
        InputStream stream = null;
        final String fileName = "org/modeshape/sequencer/teiid/model/medNameMappings.properties";

        try {
            stream = ModelExtensionDefinitionHelper.class.getClassLoader().getResourceAsStream(fileName);
            final Properties props = new Properties();
            props.load(stream);
            return new UnmodifiableProperties(props);
        } catch (final IOException e) {
            throw new IllegalStateException(TeiidI18n.errorReadingMedMetaclassMappings.text(fileName, e.getLocalizedMessage()), e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (final IOException e) {
                } finally {
                    stream = null;
                }
            }
        }
    }

    private final ModelNodeWriter writer;

    /**
     * key is namespace prefix found in MED, value is prefix used to register in namespace registry
     */
    private final Map<String, String> medPrefixMap = new HashMap<String, String>();

    /**
     * key is model MED prefix, value is a set of full metaclass names
     */
    private final Map<String, Set<String>> medMetaclassMap = new HashMap<String, Set<String>>();

    /**
     * key is mixin name, value is the registered node type
     */
    private final Map<String, NodeTypeTemplate> mixinMap = new HashMap<String, NodeTypeTemplate>();

    ModelExtensionDefinitionHelper( final ModelNodeWriter writer ) {
        CheckArg.isNotNull(writer, "writer");
        this.writer = writer;
    }

    /**
     * Creates and registers, if necessary, a mixin and applies it to the specified node.
     * 
     * @param node the node the mixin is being added to (cannot be <code>null</code>)
     * @param medPrefix the model MED prefix (cannot be <code>null</code>)
     * @param metaclass the node type metaclass being used to create the mixin (cannot be <code>null</code>)
     * @param session the session used to create the mixin if necessary (cannot be <code>null</code>)
     * @throws Exception if there is a problem creating or applying the mixin
     */
    private void addMixin( final Node node,
                           final String medPrefix,
                           final String metaclass,
                           final Session session ) throws Exception {
        final String[] parts = metaclass.split("\\.", 2);
        final String mappedPrefix = this.medPrefixMap.get(medPrefix);
        final String mixinName = mappedPrefix + ':' + parts[1];
        NodeTypeTemplate mixin = this.mixinMap.get(mixinName);

        if (mixin == null) {
            final NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
            mixin = ntMgr.createNodeTypeTemplate();
            mixin.setMixin(true);
            mixin.setName(mixinName);

            // create mixin property definitions
            @SuppressWarnings( "unchecked" )
            final List<PropertyDefinitionTemplate> propDefns = mixin.getPropertyDefinitionTemplates();
            final Node medGroupNode = this.writer.getModelNode().getNode(CoreLexicon.JcrId.MODEL_EXTENSION_DEFINITIONS_GROUP_NODE);
            final Node medNode = medGroupNode.getNode(medPrefix);
            final NodeIterator itr = medNode.getNodes();

            while (itr.hasNext()) {
                final Node medChild = itr.nextNode();

                if (medChild.isNodeType(JcrId.EXTENDED_METACLASS)) {
                    final NodeIterator pitr = medChild.getNodes();

                    while (pitr.hasNext()) {
                        final Node metaclassChild = pitr.nextNode();

                        if (metaclassChild.isNodeType(JcrId.PROPERTY_DEFINITION)) {
                            final PropertyDefinitionTemplate propDefn = ntMgr.createPropertyDefinitionTemplate();
                            propDefn.setName(metaclassChild.getName());

                            { // default value
                                if (metaclassChild.hasProperty(JcrId.Property.DEFAULT_VALUE)) {
                                    final String defaultValue = metaclassChild.getProperty(JcrId.Property.DEFAULT_VALUE).getString();
                                    final Value value = this.writer.getContext().valueFactory().createValue(defaultValue);
                                    propDefn.setDefaultValues(new Value[] {value});
                                    propDefn.setAutoCreated(true);
                                }
                            }

                            { // mandatory
                                if (metaclassChild.hasProperty(JcrId.Property.REQUIRED)) {
                                    final boolean mandatory = metaclassChild.getProperty(JcrId.Property.REQUIRED).getBoolean();
                                    propDefn.setMandatory(mandatory);
                                }
                            }

                            { // type
                                final String type = metaclassChild.getProperty(JcrId.Property.RUNTIME_TYPE).getString();
                                Integer msType = TYPE_MAPPINGS.get(type);

                                if (msType == null) {
                                    msType = PropertyType.STRING;
                                }

                                propDefn.setRequiredType(msType);
                            }

                            propDefns.add(propDefn);
                            LOGGER.debug("added property '{0}' to mixin '{1}'", propDefn.getName(), mixinName);
                        }
                    }
                }
            }

            ntMgr.registerNodeType(mixin, true);
            this.mixinMap.put(mixinName, mixin);
            LOGGER.debug("added mixin '{0}' to registry", mixinName);
        }

        node.addMixin(mixinName);
        LOGGER.debug("added mixin '{0}' to node '{1}'", mixinName, node.getName());
    }

    private boolean hasMetaclassMapping( final String jcrType,
                                  final String metaclassName ) {
        final String value = METACLASS_MAPPINGS.getProperty(jcrType);

        if (StringUtil.isBlank(value)) {
            return false;
        }

        // mapping is one or more metaclass full names
        for (final String name : value.split(",")) {
            if (name.equals(metaclassName)) {
                return true;
            }
        }

        return false;
    }

    private void assignMedMixins( final Node node ) throws Exception {
        // the metaclass mappings replace the colon in the prefix:name with a period
        final String jcrType = node.getProperty(JcrConstants.JCR_PRIMARY_TYPE).getString().replaceFirst(":", ".");

        // if there is a metaclass mapping check to see if a model MED is extending that metaclass
        if (METACLASS_MAPPINGS.containsKey(jcrType)) {
            for (final String medPrefix : this.medPrefixMap.keySet()) {
                final Set<String> extendedMetaclasses = this.medMetaclassMap.get(medPrefix);

                if (extendedMetaclasses != null) {
                    // could have more than one MED applied need to add mixin for each
                    for (final String metaclass : extendedMetaclasses) {
                        if (hasMetaclassMapping(jcrType, metaclass)) {
                            addMixin(node, medPrefix, jcrType, node.getSession());
                        }
                    }
                }
            }
        }

        // set children MED default values
        final NodeIterator itr = node.getNodes();

        while (itr.hasNext()) {
            final Node kid = itr.nextNode();
            assignMedMixins(kid);
        }
    }

    void assignModelNodeChildrenMedMixins( final Node modelNode ) throws Exception {
        LOGGER.debug("==== ModelExtensionDefinitionHelper:assignModelNodeChildrenMedMixins");
        final NodeIterator itr = modelNode.getNodes();

        while (itr.hasNext()) {
            final Node kid = itr.nextNode();
            assignMedMixins(kid); // recurse children
        }
    }

    /**
     * @param registry the registry being used to check for existing prefixes (cannot be <code>null</code>)
     * @param nsPrefix the prefix that the suffix is added to (cannot be <code>null</code> or empty)
     * @param suffix the number appended to the prefix
     * @return the new unregistered, unique prefix (never <code>null</code> or empty)
     * @throws Exception if there is a problem accessing the registry
     */
    private String createMedPrefix( final NamespaceRegistry registry,
                                    final String nsPrefix,
                                    long suffix ) throws Exception {
        assert (registry != null);
        assert (!StringUtil.isBlank(nsPrefix));

        final String newPrefix = nsPrefix + suffix;

        // if new prefix is already registered create another prefix
        if (registry.isRegisteredPrefix(newPrefix)) {
            return createMedPrefix(registry, nsPrefix, ++suffix);
        }

        LOGGER.debug("created new namespace prefix '{0}'", newPrefix);
        return newPrefix;
    }

    /**
     * @return the core model object handler (never <code>null</code>)
     * @throws Exception if there is problem obtaining the handler
     */
    private ModelObjectHandler getHandler() throws Exception {
        return this.writer.getHandler(CoreLexicon.Namespace.URI);
    }

    /**
     * @param propertyName the property name whose MED prefix mapping is being requested (cannot be <code>null</code> or empty)
     * @return the mapped property name if exists or the original property name if mapping does not exist
     */
    String getMappedPropertyName( final String propertyName ) {
        CheckArg.isNotEmpty(propertyName, "propertyName");
        String mappedName = propertyName;
        final String[] propNameParts = propertyName.split(":", 2);

        if ((propNameParts.length == 2) && hasMed(propNameParts[0])) {
            final String registeredPrefix = getRegisteredMedPrefix(propNameParts[0]);

            // found a MED mapping so use new prefix
            if (!propNameParts[0].equals(registeredPrefix)) {
                mappedName = registeredPrefix + ':' + propNameParts[1];
            }
        }

        return mappedName;
    }

    /**
     * @param medPrefix the MED prefix whose namespace registry prefix is being requested (cannot be <code>null</code> or empty).
     * @return the namespace registry MED prefix or <code>null</code> if model does not contain MED
     */
    String getRegisteredMedPrefix( final String medPrefix ) {
        CheckArg.isNotEmpty(medPrefix, "medPrefix");
        return this.medPrefixMap.get(medPrefix);
    }

    /**
     * @param medNamespacePrefix the MED prefix being checked (cannot be <code>null</code> or empty)
     * @return <code>true</code> if the model has a MED with the specified prefix
     */
    boolean hasMed( final String medNamespacePrefix ) {
        CheckArg.isNotEmpty(medNamespacePrefix, "medNamespacePrefix");
        return this.medPrefixMap.containsKey(medNamespacePrefix);
    }

    private boolean isMedMetaclassNode( final Node node ) throws Exception {
        return ((node != null) && JcrId.EXTENDED_METACLASS.equals(node.getPrimaryNodeType().getName()));
    }

    private boolean isMedNode( final Node node ) throws Exception {
        return ((node != null) && JcrId.MODEL_EXTENSION_DEFINITION.equals(node.getPrimaryNodeType().getName()));
    }

    private boolean isMedPropertyDefinitionNode( final Node node ) throws Exception {
        return ((node != null) && JcrId.PROPERTY_DEFINITION.equals(node.getPrimaryNodeType().getName()));
    }

    private boolean isModelMedTag( final XmiElement tag,
                                   final Node annotatedObjectNode ) throws Exception {
        final String key = tag.getAttributeValue(CoreLexicon.ModelId.KEY, URI);

        if (Utils.isModelMedTagKey(key) && (tag.getParent() != null)) {
            return ((annotatedObjectNode != null) && annotatedObjectNode.isNodeType(CoreLexicon.JcrId.MODEL));
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
        uuid = this.writer.getResolver().resolveInternalReference(uuid);

        // go get the tag XMI element
        final XmiElement referencedElement = this.writer.getResolver().getUuidMappings().get(uuid);

        // make need to make sure referenced element is the for model types tag so follow the annotated object reference
        if ((referencedElement != null) && CoreLexicon.ModelId.TAGS.equals(referencedElement.getName())) {
            if (ModelId.MODEL_TYPES.equals(referencedElement.getAttributeValue(CoreLexicon.ModelId.KEY, URI))) {
                // get med node by using parent's annotated object
                String medUuid = referencedElement.getParent().getAttributeValue(CoreLexicon.ModelId.ANNOTATED_OBJECT, URI);
                medUuid = this.writer.getResolver().resolveInternalReference(medUuid);
                final Node medNode = this.writer.getResolver().getNode(medUuid);

                if (medNode != null) {
                    final Value newValue = this.writer.getContext().valueFactory().createValue(newModelType);
                    Value[] modelTypes = null;

                    if (medNode.hasProperty(JcrId.MODEL_TYPES)) {
                        final Property property = medNode.getProperty(JcrId.MODEL_TYPES);
                        final Value[] currentValues = property.getValues();
                        modelTypes = new Value[currentValues.length + 1];
                        System.arraycopy(currentValues, 0, modelTypes, 0, currentValues.length);
                        modelTypes[currentValues.length] = newValue;
                    } else {
                        modelTypes = new Value[] {newValue};
                    }

                    medNode.setProperty(JcrId.MODEL_TYPES, modelTypes);
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
        uuid = this.writer.getResolver().resolveInternalReference(uuid);

        // go get the tag XMI element
        final XmiElement referencedElement = this.writer.getResolver().getUuidMappings().get(uuid);

        // make sure referenced element is the for description tag
        if ((referencedElement != null) && CoreLexicon.ModelId.TAGS.equals(referencedElement.getName())) {
            if (ModelId.DESCRIPTION.equals(referencedElement.getAttributeValue(CoreLexicon.ModelId.KEY, URI))) {
                // get property definition node by using parent's annotated object
                String propDefUuid = referencedElement.getParent().getAttributeValue(CoreLexicon.ModelId.ANNOTATED_OBJECT, URI);
                propDefUuid = this.writer.getResolver().resolveInternalReference(propDefUuid);
                final Node propDefNode = this.writer.getResolver().getNode(propDefUuid);

                if (propDefNode != null) {
                    final Node descriptionNode = getHandler().addNode(propDefNode,
                                                                      JcrId.DESCRIPTION,
                                                                      tagElement.getUuid(),
                                                                      JcrId.LOCALIZED_DESCRIPTION);
                    descriptionNode.setProperty(JcrId.LOCALE, tagElement.getAttributeValue(CoreLexicon.ModelId.KEY, URI));
                    descriptionNode.setProperty(JcrId.TRANSLATION, tagElement.getAttributeValue(CoreLexicon.ModelId.VALUE, URI));

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
        uuid = this.writer.getResolver().resolveInternalReference(uuid);

        // go get the tag XMI element
        final XmiElement referencedElement = this.writer.getResolver().getUuidMappings().get(uuid);

        // make sure referenced element is the for the display name tag
        if ((referencedElement != null) && CoreLexicon.ModelId.TAGS.equals(referencedElement.getName())) {
            if (ModelId.Property.DISPLAY_NAME.equals(referencedElement.getAttributeValue(CoreLexicon.ModelId.KEY, URI))) {
                // get property definition node by using parent's annotated object
                String propDefUuid = referencedElement.getParent().getAttributeValue(CoreLexicon.ModelId.ANNOTATED_OBJECT, URI);
                propDefUuid = this.writer.getResolver().resolveInternalReference(propDefUuid);
                final Node propDefNode = this.writer.getResolver().getNode(propDefUuid);

                if (propDefNode != null) {
                    final Node displayNameNode = getHandler().addNode(propDefNode,
                                                                      JcrId.Property.DISPLAY_NAME,
                                                                      tagElement.getUuid(),
                                                                      JcrId.LOCALIZED_NAME);
                    displayNameNode.setProperty(JcrId.LOCALE, tagElement.getAttributeValue(CoreLexicon.ModelId.KEY, URI));
                    displayNameNode.setProperty(JcrId.TRANSLATION, tagElement.getAttributeValue(CoreLexicon.ModelId.VALUE, URI));

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

    boolean process( final Node modelNode,
                     final Node annotatedObjectNode,
                     final UnresolvedReference unresolved,
                     final XmiElement annotationTagElement ) throws Exception {
        final String key = annotationTagElement.getAttributeValue(CoreLexicon.ModelId.KEY, URI);

        if (StringUtil.isBlank(key)) {
            return false;
        }

        final String value = annotationTagElement.getAttributeValue(CoreLexicon.ModelId.VALUE, URI);
        final String[] parts = key.split(":", 2); // part 0 = namespace prefix, part 2 = property name

        if (isModelMedTag(annotationTagElement, annotatedObjectNode)) {
            // add MED group node to model if necessary
            Node medGroupNode = null;

            if (modelNode.hasNode(CoreLexicon.JcrId.MODEL_EXTENSION_DEFINITIONS_GROUP_NODE)) {
                medGroupNode = modelNode.getNode(CoreLexicon.JcrId.MODEL_EXTENSION_DEFINITIONS_GROUP_NODE);
            } else {
                medGroupNode = getHandler().addNode(modelNode,
                                                    CoreLexicon.JcrId.MODEL_EXTENSION_DEFINITIONS_GROUP_NODE,
                                                    null,
                                                    CoreLexicon.JcrId.MODEL_EXTENSION_DEFINITIONS_GROUP_NODE);
            }

            // add MED node
            assert (medGroupNode != null) : "MED group node is null";
            getHandler().addNode(medGroupNode, parts[1], // namespace prefix
                                 annotationTagElement.getUuid(),
                                 JcrId.MODEL_EXTENSION_DEFINITION);
            return true;
        }

        // MED node annotation
        if (isMedNode(annotatedObjectNode) && (annotatedObjectNode != null)) {
            // a MED node should have at least one metaclass child nodes
            if (Utils.isModelMedMetaclassTagKey(key)) {
                final String medPrefix = annotatedObjectNode.getName();
                final String metaclass = parts[1];
                getHandler().addNode(annotatedObjectNode, metaclass, annotationTagElement.getUuid(), JcrId.EXTENDED_METACLASS);

                // keep track of the MED metaclasses
                Set<String> extendedMetaclasses = this.medMetaclassMap.get(medPrefix);

                if (extendedMetaclasses == null) {
                    extendedMetaclasses = new HashSet<String>();
                    this.medMetaclassMap.put(medPrefix, extendedMetaclasses);
                }

                extendedMetaclasses.add(metaclass);
                LOGGER.debug("added metaclass node '{0}' to MED '{1}'", metaclass, medPrefix);
            } else if (Utils.isModelMedModelTypesTagKey(key)) {
                // ignore as this is only used as the annotated object for when the actual model types
                // are found in the model
            } else {
                annotatedObjectNode.setProperty(Utils.constructJcrName(key), value);
                LOGGER.debug("set MED property '{0}' to value '{1}' for MED '{2}'",
                             Utils.constructJcrName(key),
                             value,
                             annotatedObjectNode.getName());

                // if the namespace prefix, namespace URI, and version have been found go ahead and register the MED namespace
                if (annotatedObjectNode.hasProperty(JcrId.NAMESPACE_PREFIX)
                    && annotatedObjectNode.hasProperty(JcrId.NAMESPACE_URI) && annotatedObjectNode.hasProperty(JcrId.VERSION)) {
                    final String nsPrefix = annotatedObjectNode.getProperty(JcrId.NAMESPACE_PREFIX).getString();

                    // don't register if MED is already registered
                    if (!this.medPrefixMap.containsKey(nsPrefix)) {
                        registerNamespace(annotatedObjectNode.getSession(),
                                          (NamespaceRegistry)annotatedObjectNode.getSession().getWorkspace().getNamespaceRegistry(),
                                          nsPrefix,
                                          annotatedObjectNode.getProperty(JcrId.NAMESPACE_URI).getString(),
                                          annotatedObjectNode.getProperty(JcrId.VERSION).getLong());
                    }
                }
            }

            return true;
        }

        // extended metaclass annotation
        if (isMedMetaclassNode(annotatedObjectNode) && (annotatedObjectNode != null)) {
            // a MED node should have at least one metaclass child node
            if (Utils.isModelMedPropertyDefinitionTagKey(key)) {
                final String medPrefix = annotatedObjectNode.getParent().getName();
                final String mappedPrefix = this.medPrefixMap.get(medPrefix);
                getHandler().addNode(annotatedObjectNode,
                                     mappedPrefix + ':' + parts[1],
                                     annotationTagElement.getUuid(),
                                     JcrId.PROPERTY_DEFINITION);
                LOGGER.debug("added MED property definition node '{0}' to metaclass '{1}' in MED '{2}'",
                             mappedPrefix + ':' + parts[1],
                             annotatedObjectNode.getName(),
                             medPrefix);
            } else {
                annotatedObjectNode.setProperty(Utils.constructJcrName(key), value);
                LOGGER.debug("set MED metaclass property '{0}' to value '{1}' for metaclass '{2}'",
                             Utils.constructJcrName(key),
                             value,
                             annotatedObjectNode.getName());
            }

            return true;
        }

        // property definition annotation
        if (isMedPropertyDefinitionNode(annotatedObjectNode) && (annotatedObjectNode != null)) {
            // a MED node should have at least one property definition child node
            if (Utils.isModelMedPropertyDefinitionDescriptionTagKey(key)) {
                // handled later when the localized descriptions are processed
            } else if (Utils.isModelMedPropertyDefinitionDisplayNameTagKey(key)) {
                // handled later when the localized display names are processed
            } else {
                final String propName = Utils.constructJcrName(key);
                annotatedObjectNode.setProperty(propName, value);
                LOGGER.debug("set MED property definition property '{0}' to value '{1}' for property '{2}'",
                             propName,
                             value,
                             annotatedObjectNode.getName());
            }

            return true;
        }

        // added model type to MED
        if ((annotatedObjectNode == null) && medModelTypeProcessed(annotationTagElement, key)) {
            return true;
        }

        // added property definition description to MED
        if ((annotatedObjectNode == null) && medPropertyDefinitionDescriptionProcessed(annotationTagElement, key)) {
            return true;
        }

        // added property definition display name to MED
        if ((annotatedObjectNode == null) && medPropertyDefinitionDisplayNameProcessed(annotationTagElement, key)) {
            return true;
        }

        // model object has set value of MED property
        if (hasMed(parts[0])) {
            final String mappedPrefix = this.medPrefixMap.get(parts[0]);

            if (annotatedObjectNode != null) {
                getHandler().setProperty(annotatedObjectNode, mappedPrefix + ':' + parts[1], value);
            } else {
                unresolved.addProperty(mappedPrefix + ':' + parts[1], value, false);
            }
            return true;
        }

        // not MED-related
        return false;
    }

    private void registerNamespace( final Session session,
                                    final NamespaceRegistry registry,
                                    final String medPrefix,
                                    String medUri,
                                    final long medVersion ) throws Exception {
        assert ((session != null) && (registry != null) && !StringUtil.isBlank(medPrefix) && !StringUtil.isBlank(medUri));

        // exit if MED already registered
        if (this.medPrefixMap.containsKey(medPrefix)) {
            return;
        }

        { // make sure MED URI has proper suffix (slash version)
            final String medUriSuffix = "/" + medVersion;

            if (!medUri.endsWith(medUriSuffix)) {
                if (medUri.endsWith("/")) {
                    medUri += Long.toString(medVersion);
                } else {
                    medUri += medUriSuffix;
                }
            }
        }

        String registeredPrefix = null;
        String registeredUri = null;

        // see if there is a registered prefix for the MED URI
        if (registry.isRegisteredUri(medUri)) {
            registeredPrefix = registry.getPrefix(medUri);
        }

        // see if there is a registered URI for the MED prefix
        if (registry.isRegisteredPrefix(medPrefix)) {
            registeredUri = registry.getURI(medPrefix);
        }

        final boolean prefixMatch = !StringUtil.isBlank(registeredUri);
        final boolean uriMatch = !StringUtil.isBlank(registeredPrefix);

        if (prefixMatch) {
            // if prefixes and URIs match then namespace is already registered
            if (uriMatch) {
                this.medPrefixMap.put(medPrefix, medPrefix);
            } else {
                // if prefixes are equal but URIs are not, need to change prefix
                final String newPrefix = createMedPrefix(registry, medPrefix, medVersion);
                registry.registerNamespace(newPrefix, medUri);
                this.medPrefixMap.put(medPrefix, newPrefix);
                LOGGER.debug("registered namespace '{0}':'{1}'", newPrefix, medUri);
            }
        } else if (uriMatch) {
            // prefixes don't match, URIs match, use registry prefix
            this.medPrefixMap.put(medPrefix, registeredPrefix);
        } else {
            // neither prefix or URIs match so register namespace
            registry.registerNamespace(medPrefix, medUri);
            this.medPrefixMap.put(medPrefix, medPrefix);
            LOGGER.debug("registered namespace '{0}':'{1}'", medPrefix, medUri);
        }
    }
}
