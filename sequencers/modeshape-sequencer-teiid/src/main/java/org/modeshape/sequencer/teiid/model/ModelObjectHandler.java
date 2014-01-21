/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.sequencer.teiid.model;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Value;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.sequencer.Sequencer.Context;
import org.modeshape.sequencer.teiid.VdbModel;
import org.modeshape.sequencer.teiid.lexicon.XmiLexicon;
import org.modeshape.sequencer.teiid.model.ReferenceResolver.UnresolvedReference;
import org.modeshape.sequencer.teiid.xmi.XmiAttribute;
import org.modeshape.sequencer.teiid.xmi.XmiElement;
import org.modeshape.sequencer.teiid.xmi.XmiPart;

/**
 * Base implementation of a model object handler.
 */
public abstract class ModelObjectHandler {

    protected static final Logger LOGGER = Logger.getLogger(ModelObjectHandler.class);

    private Context context; // set by handler framework and is never null
    private ModelReader reader; // set by handler framework and is never null
    private ReferenceResolver resolver; // set by handler framework and is never null
    private VdbModel vdbModel; // set by handler framework but can be null if model did not come from a VDB

    private ModelExtensionDefinitionHelper medHelper;

    /**
     * If the element or name attribute URI is empty, the primary node type is used as the name.
     * 
     * @param parentNode the parent node where the child node is being added (cannot be <code>null</code>)
     * @param nodeName the name to use to create the child node (can be <code>null</code>)
     * @param xmiUuid the value of the XMI UUID property (can be <code>null</code> or empty)
     * @param primaryNodeType the primary node type to use when creating the new node (cannot be <code>null</code> or empty)
     * @return the new node (never <code>null</code>)
     * @throws Exception if there is a problem creating the node
     */
    protected Node addNode( final Node parentNode,
                            final String nodeName,
                            final String xmiUuid,
                            final String primaryNodeType ) throws Exception {
        CheckArg.isNotNull(parentNode, "parentNode");
        CheckArg.isNotEmpty(nodeName, "nodeName");
        CheckArg.isNotEmpty(primaryNodeType, "primaryNodeType");

        final Node newNode = parentNode.addNode(nodeName, primaryNodeType);

        if (!StringUtil.isBlank(xmiUuid)) {
            setProperty(newNode, XmiLexicon.JcrId.UUID, xmiUuid);
            this.resolver.record(xmiUuid, newNode);
        }

        LOGGER.debug("adding node {0} to parent {1}", newNode.getName(), parentNode.getName());
        return newNode;
    }

    /**
     * If the element or name attribute URI is empty, the primary node type is used as the name.
     * 
     * @param parentNode the parent node where the child node is being added (cannot be <code>null</code>)
     * @param element the XMI element to use to create the child node (can be <code>null</code>)
     * @param nameAttributeUri the URI of the name property (can be <code>null</code> or empty)
     * @param primaryNodeType the primary node type to use when creating the new node (cannot be <code>null</code> or empty)
     * @return the new node (never <code>null</code>)
     * @throws Exception if there is a problem creating the node
     */
    protected Node addNode( final Node parentNode,
                            final XmiElement element,
                            final String nameAttributeUri,
                            final String primaryNodeType ) throws Exception {
        CheckArg.isNotNull(parentNode, "parentNode");
        CheckArg.isNotEmpty(primaryNodeType, "primaryNodeType");

        String name = null;

        if (StringUtil.isBlank(nameAttributeUri) || (element == null)) {
            name = primaryNodeType;
        } else {
            final XmiAttribute nameAttribute = element.getNameAttribute(nameAttributeUri);

            if (nameAttribute == null) {
                name = primaryNodeType;
            } else {
                name = nameAttribute.getValue();
            }
        }

        final String uuid = ((element == null) ? null : element.getUuid());
        return addNode(parentNode, name, uuid, primaryNodeType);
    }

    /**
     * @param node the node whose multi-valued property a value is being added to (cannot be <code>null</code>)
     * @param propertyName the multi-valued property name (cannot be <code>null</code> or empty)
     * @param newValue the value being added (cannot be <code>null</code> or empty)
     * @throws Exception if there is a problem adding the property value
     */
    protected void addPropertyValue( final Node node,
                                     final String propertyName,
                                     final String newValue ) throws Exception {
        CheckArg.isNotEmpty(newValue, "newValue");
        addPropertyValue(node, propertyName, this.context.valueFactory().createValue(newValue));
    }

    /**
     * @param node the node whose multi-valued property a value is being added to (cannot be <code>null</code>)
     * @param propertyName the multi-valued property name (cannot be <code>null</code> or empty)
     * @param newValue the value being added (cannot be <code>null</code> or empty)
     * @throws Exception if there is a problem adding the property value
     */
    protected void addPropertyValue( final Node node,
                                     final String propertyName,
                                     final Value newValue ) throws Exception {
        CheckArg.isNotNull(node, "node");
        CheckArg.isNotEmpty(propertyName, "propertyName");
        CheckArg.isNotNull(newValue, "newValue");

        if (node.hasProperty(propertyName)) {
            final Property property = node.getProperty(propertyName);
            final Value[] currentValues = property.getValues();
            final Value[] newValues = new Value[currentValues.length + 1];
            System.arraycopy(currentValues, 0, newValues, 0, currentValues.length);
            newValues[currentValues.length] = newValue;
            node.setProperty(propertyName, newValues);
        } else {
            node.setProperty(propertyName, new Value[] {newValue});
        }

        LOGGER.debug("added a value of '{0}' to multi-valued property '{1}' in node '{2}'",
                     newValue,
                     propertyName,
                     node.getName());
    }

    /**
     * @return context the sequencer context (never <code>null</code>)
     */
    protected Context getContext() {
        return this.context;
    }

    /**
     * @return the MED helper (never <code>null</code>)
     */
    protected ModelExtensionDefinitionHelper getMedHelper() {
        return this.medHelper;
    }

    /**
     * @param xmiPart the XMI part whose qualified name is being requested (cannot be <code>null</code>)
     * @return the qualified name obtained from the part (never <code>null</code> or empty)
     */
    protected String getQName( final XmiPart xmiPart ) {
        CheckArg.isNotNull(xmiPart, "xmiPart");
        return xmiPart.getQName();
    }

    /**
     * @return reader the model reader (never <code>null</code>)
     */
    protected ModelReader getReader() {
        return this.reader;
    }

    /**
     * @return the reference resolver (never <code>null</code>)
     */
    protected ReferenceResolver getResolver() {
        return this.resolver;
    }

    /**
     * @return vdbModel the VDB model (can be <code>null</code> if model did not come from a VDB)
     */
    protected VdbModel getVdbModel() {
        return this.vdbModel;
    }

    /**
     * @param element the element being processed (cannot be <code>null</code>)
     * @param parentNode the parent node to use if a node is created for the element (cannot be <code>null</code>)
     * @throws Exception if there is a problem processing the XMI element
     */
    protected abstract void process( final XmiElement element,
                                     final Node parentNode ) throws Exception;

    /**
     * If the value is <code>null</code> or empty the property is not set.
     * 
     * @param node the node whose boolean property is being set (cannot be <code>null</code>)
     * @param value the property value (can be <code>null</code> or empty)
     * @param jcrPropertyName the JCR property name (cannot be <code>null</code> or empty)
     * @throws Exception if there is a problem setting the property
     */
    protected void setBooleanProperty( final Node node,
                                       final String jcrPropertyName,
                                       final String value ) throws Exception {
        CheckArg.isNotNull(node, "node");
        CheckArg.isNotEmpty(jcrPropertyName, "jcrPropertyName");

        if (!StringUtil.isBlank(value)) {
            node.setProperty(jcrPropertyName, Boolean.parseBoolean(value));
        }
    }

    /**
     * <strong>This method should only be called by the framework loading model object handlers.</strong>
     * 
     * @param context the sequencer context being set (never <code>null</code>)
     */
    protected void setContext( final Context context ) {
        CheckArg.isNotNull(context, "context");
        this.context = context;
    }

    void setModelExtensionDefinitionHelper( final ModelExtensionDefinitionHelper medHelper ) {
        this.medHelper = medHelper;
    }

    /**
     * Sets the specified, <strong>single-valued</strong>, property only if the value is not <code>null</code> and not empty.
     * 
     * @param node the node whose property is being set (cannot be <code>null</code>)
     * @param propertyName the name of the property being set (cannot be <code>null</code>)
     * @param propertyValue the proposed property value (can be <code>null</code> or empty)
     * @throws Exception if there is a problem setting the property
     */
    protected void setProperty( final Node node,
                                final String propertyName,
                                final String propertyValue ) throws Exception {
        CheckArg.isNotNull(node, "node");
        CheckArg.isNotEmpty(propertyName, "propertyName");

        if (!StringUtil.isBlank(propertyValue)) {
            if (this.resolver.isReference(propertyValue)) {
                String refUuid = this.resolver.resolveInternalReference(propertyValue);
                Node refNode = this.resolver.getNode(refUuid);

                if (refNode == null) {
                    // unresolved reference
                    UnresolvedReference unresolved = this.resolver.addUnresolvedReference(refUuid);
                    unresolved.addReferencerReference(node.getProperty(XmiLexicon.JcrId.UUID).getString(), propertyName);
                } else {
                    // add weakreference
                    if (!refNode.isNodeType(JcrConstants.MIX_REFERENCEABLE)) {
                        refNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
                    }

                    Value weakReference = node.getSession().getValueFactory().createValue(refNode, true);
                    node.setProperty(propertyName, weakReference);
                }
            } else {
                node.setProperty(propertyName, propertyValue);
                LOGGER.debug("{0}:setting {1} = {2}", node.getName(), propertyName, propertyValue);
            }
        }
    }

    /**
     * <strong>This method should only be called by the framework loading model object handlers.</strong>
     * 
     * @param reader the model reader being set (never <code>null</code>)
     */
    protected void setReader( final ModelReader reader ) {
        CheckArg.isNotNull(reader, "reader");
        this.reader = reader;
    }

    /**
     * <strong>This method should only be called by the framework loading model object handlers.</strong>
     * 
     * @param resolver the reference resolver used during sequencing (cannot be <code>null</code>)
     */
    protected void setResolver( final ReferenceResolver resolver ) {
        CheckArg.isNotNull(resolver, "resolver");
        this.resolver = resolver;
    }

    /**
     * <strong>This method should only be called by the framework loading model object handlers.</strong>
     * 
     * @param vdbModel the VDB model being sequenced (can be <code>null</code> if model is not from a VDB)
     */
    protected void setVdbModel( final VdbModel vdbModel ) {
        this.vdbModel = vdbModel;
    }
}
