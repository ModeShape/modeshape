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

    protected Logger logger = Logger.getLogger(getClass());

    protected static final boolean DEBUG = false;

    protected void debug( final String message ) {
        if (DEBUG) {
            System.err.println(message);
        }
        logger.debug(message);
    }

    private Context context; // set by handler framework and is never null
    private ModelReader reader; // set by handler framework and is never null
    private ReferenceResolver resolver; // set by handler framework and is never null
    private VdbModel vdbModel; // set by handler framework but can be null if model did not come from a VDB

    /**
     * @param parentNode the parent node where the child node is being added (cannot be <code>null</code>)
     * @param element the XMI element to use to create the child node (cannot be <code>null</code>)
     * @param nameAttributeUri the URI of the name property (cannot be <code>null</code> or empty)
     * @param primaryNodeType the primary node type to use when creating the new node (cannot be <code>null</code> or empty)
     * @return the new node (never <code>null</code>)
     * @throws Exception if there is a problem creating the node
     */
    protected Node addNode( final Node parentNode,
                            final XmiElement element,
                            final String nameAttributeUri,
                            final String primaryNodeType ) throws Exception {
        CheckArg.isNotNull(parentNode, "parentNode");
        CheckArg.isNotNull(element, "element");
        CheckArg.isNotEmpty(nameAttributeUri, "nameAttributeUri");
        CheckArg.isNotEmpty(primaryNodeType, "primaryNodeType");

        String name = null;
        final XmiAttribute nameAttribute = element.getNameAttribute(nameAttributeUri);

        if (nameAttribute == null) {
            name = primaryNodeType;
        } else {
            name = nameAttribute.getValue();
        }

        final Node newNode = parentNode.addNode(name, primaryNodeType);
        setProperty(newNode, XmiLexicon.JcrId.UUID, element.getUuid());
        this.resolver.record(element.getUuid(), newNode);

        debug("adding node " + newNode.getName() + " to parent " + parentNode.getName());
        return newNode;
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

        debug("added a value of " + newValue + " to multi-valued property " + propertyName + " in node " + node.getName());
    }

    /**
     * @return context the sequencer context (never <code>null</code>)
     */
    protected Context getContext() {
        return this.context;
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
                debug(node.getName() + ":setting " + propertyName + " = " + propertyValue);
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
