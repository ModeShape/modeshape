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
import javax.jcr.Value;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.sequencer.teiid.lexicon.TransformLexicon;
import org.modeshape.sequencer.teiid.lexicon.TransformLexicon.JcrId;
import org.modeshape.sequencer.teiid.lexicon.TransformLexicon.ModelId;
import static org.modeshape.sequencer.teiid.lexicon.TransformLexicon.Namespace.URI;
import org.modeshape.sequencer.teiid.lexicon.XmiLexicon;
import org.modeshape.sequencer.teiid.model.ReferenceResolver.UnresolvedReference;
import org.modeshape.sequencer.teiid.xmi.XmiElement;

/**
 * The model object handler for the {@link org.modeshape.sequencer.teiid.lexicon.TransformLexicon.Namespace#URI transformation}
 * namespace.
 */
public final class TransformationModelObjectHandler extends ModelObjectHandler {

    /**
     * @see org.modeshape.sequencer.teiid.model.ModelObjectHandler#process(org.modeshape.sequencer.teiid.xmi.XmiElement,
     *      javax.jcr.Node)
     */
    @Override
    protected void process( final XmiElement element,
                            final Node parentNode ) throws Exception {
        CheckArg.isNotNull(element, "element");
        CheckArg.isNotNull(parentNode, "node");
        CheckArg.isEquals(element.getNamespaceUri(), "namespace URI", URI, "relational URI");

        debug("==== TransformationModelObjectHandler:process:element=" + element.getName());

        if (ModelId.TRANSFORMATION_CONTAINER.equals(element.getName())) {
            // just process children
            for (final XmiElement kid : element.getChildren()) {
                if (ModelId.TRANSFORMATION_MAPPINGS.equals(kid.getName())) {
                    processMappings(kid);
                } else {
                    debug("**** transformation container child of '" + kid.getName() + "' was not processed");
                }
            }
        } else {
            debug("**** transformation type of '" + element.getName() + "' was not processed");
        }
    }

    private void processInputs( final XmiElement inputs,
                                final Node transformed,
                                final UnresolvedReference unresolved ) throws Exception {
        assert (inputs != null);
        assert (ModelId.INPUTS.equals(inputs.getName()));

        // inputs is a referenced source table or a referenced column

        final ReferenceResolver resolver = getResolver();
        final String inputHref = inputs.getAttributeValue(ModelId.HREF, URI);
        final String inputUuid = resolver.resolveInternalReference(inputHref);

        // - transform:transformedFrom (weakreference)
        setReference(transformed, unresolved, JcrId.TRANSFORMED_FROM, inputUuid, true);

        // - transform:transformedFromHrefs (string)
        if (transformed != null) {
            addPropertyValue(transformed, JcrId.TRANSFORMED_FROM_HREFS, inputHref);
        } else if (unresolved != null) {
            unresolved.addProperty(JcrId.TRANSFORMED_FROM_HREFS, inputHref, true);
        } else {
            assert false;
        }

        // - transform:transformedFromXmiUuids (string)
        if (transformed != null) {
            addPropertyValue(transformed, JcrId.TRANSFORMED_FROM_XMI_UUIDS, inputUuid);
        } else if (unresolved != null) {
            unresolved.addProperty(JcrId.TRANSFORMED_FROM_XMI_UUIDS, inputUuid, true);
        } else {
            assert false;
        }

        // get the referenced source table or column node in order to get its name
        final Node source = resolver.getNode(inputUuid);

        // - transform:transformedFromNames (string)
        if ((transformed != null) && (source != null)) {
            addPropertyValue(transformed, JcrId.TRANSFORMED_FROM_NAMES, source.getName());
        } else {
            String referencerUuid = null;

            if (unresolved != null) {
                referencerUuid = unresolved.getUuid();
            } else if (transformed != null) {
                referencerUuid = transformed.getProperty(XmiLexicon.JcrId.UUID).getString();
            } else {
                assert false;
            }

            if (source == null) {
                UnresolvedReference unresolvedSource = resolver.addUnresolvedReference(inputUuid);
                unresolvedSource.addResolvedName(referencerUuid, JcrId.TRANSFORMED_FROM_NAMES);
            } else {
                if (unresolved != null) {
                    unresolved.addProperty(JcrId.TRANSFORMED_FROM_NAMES, source.getName(), true);
                } else {
                    assert false;
                }
            }
        }
    }

    private void processMappings( final XmiElement mappings ) throws Exception {
        assert (mappings != null);
        assert (ModelId.TRANSFORMATION_MAPPINGS.equals(mappings.getName()));

        debug("=========" + mappings.getName());
        final ReferenceResolver resolver = getResolver();
        final String targetUuid = mappings.getAttributeValue(ModelId.TARGET, URI);
        final String uuid = resolver.resolveInternalReference(targetUuid);
        final Node targetNode = resolver.getNode(uuid);
        UnresolvedReference unresolved = null;

        // add SQL mixin
        if (targetNode == null) {
            unresolved = resolver.addUnresolvedReference(uuid);
            unresolved.addMixin(JcrId.WITH_SQL);
            unresolved.addMixin(JcrId.TRANSFORMED);
        } else if (!targetNode.isNodeType(JcrId.WITH_SQL)) {
            targetNode.addMixin(JcrId.WITH_SQL);
            targetNode.addMixin(JcrId.TRANSFORMED);
        }

        // Get the transformation details ...
        for (final XmiElement kid : mappings.getChildren()) {
            if (ModelId.HELPER.equals(kid.getName())) {
                final XmiElement helperNested = kid.findChild(ModelId.NESTED, URI);

                if (helperNested == null) {
                    continue;
                }

                setNestedProperty(targetNode, unresolved, helperNested, JcrId.SELECT_SQL, ModelId.SELECT_SQL);
                setNestedProperty(targetNode, unresolved, helperNested, JcrId.INSERT_SQL, ModelId.INSERT_SQL);
                setNestedProperty(targetNode, unresolved, helperNested, JcrId.UPDATE_SQL, ModelId.UPDATE_SQL);
                setNestedProperty(targetNode, unresolved, helperNested, JcrId.DELETE_SQL, ModelId.DELETE_SQL);
                setNestedProperty(targetNode, unresolved, helperNested, JcrId.INSERT_ALLOWED, ModelId.INSERT_ALLOWED);
                setNestedProperty(targetNode, unresolved, helperNested, JcrId.UPDATE_ALLOWED, ModelId.UPDATE_ALLOWED);
                setNestedProperty(targetNode, unresolved, helperNested, JcrId.DELETE_ALLOWED, ModelId.DELETE_ALLOWED);
                setNestedProperty(targetNode, unresolved, helperNested, JcrId.INSERT_SQL_DEFAULT, ModelId.INSERT_SQL_DEFAULT);
                setNestedProperty(targetNode, unresolved, helperNested, JcrId.UPDATE_SQL_DEFAULT, ModelId.UPDATE_SQL_DEFAULT);
                setNestedProperty(targetNode, unresolved, helperNested, JcrId.DELETE_SQL_DEFAULT, ModelId.DELETE_SQL_DEFAULT);
            } else if (ModelId.INPUTS.equals(kid.getName())) {
                // input is the source tables
                processInputs(kid, targetNode, unresolved);
            } else if (ModelId.NESTED.equals(kid.getName())) {
                processNested(kid);
            } else {
                debug("**** transformation mapping child type of " + kid + " was not processed");
            }
        }
    }

    private void processNested( final XmiElement nested ) throws Exception {
        assert (nested != null);
        assert (ModelId.NESTED.equals(nested.getName()));

        // nested is a virtual column to source column(s) mapping
        // nested/outputs attribute is the virtual column
        // nested/inputs child element href attribute is the source column

        final ReferenceResolver resolver = getResolver();
        final String columnHref = nested.getAttributeValue(ModelId.OUTPUTS, URI);
        final String columnUuid = resolver.resolveInternalReference(columnHref);

        // virtual column node
        final Node columnNode = resolver.getNode(columnUuid);
        UnresolvedReference unresolvedColumn = null;

        if (columnNode == null) {
            unresolvedColumn = resolver.addUnresolvedReference(columnUuid);
            unresolvedColumn.addMixin(JcrId.TRANSFORMED);
        } else if (!columnNode.isNodeType(JcrId.TRANSFORMED)) {
            columnNode.addMixin(JcrId.TRANSFORMED);
        }

        // nested children are inputs which are the referenced source columns
        for (final XmiElement inputs : nested.getChildren()) {
            if (ModelId.INPUTS.equals(inputs.getName())) {
                processInputs(inputs, columnNode, unresolvedColumn);
            }
        }
    }

    private void setNestedProperty( final Node targetNode,
                                    final UnresolvedReference unresolved,
                                    final XmiElement nested,
                                    final String propertyName,
                                    final String attributeName ) throws Exception {
        assert (nested != null);
        assert ((propertyName != null) && !propertyName.isEmpty());
        assert ((attributeName != null) && !attributeName.isEmpty());

        final String value = nested.getAttributeValue(attributeName, TransformLexicon.Namespace.URI);

        if (!StringUtil.isBlank(value)) {
            if (targetNode == null) {
                unresolved.addProperty(propertyName, value, false);
            } else {
                setProperty(targetNode, propertyName, value);
            }
        }
    }

    private void setReference( final Node referencerNode,
                               final UnresolvedReference unresolvedReferencer,
                               final String propertyName,
                               final String referencedUuid,
                               final boolean multiValuedProperty ) throws Exception {
        assert ((referencerNode != null) || (unresolvedReferencer != null));
        assert ((propertyName != null) && !propertyName.isEmpty());
        assert ((referencedUuid != null) && !referencedUuid.isEmpty());

        // sets a weak reference

        final ReferenceResolver resolver = getResolver();
        final Node referencedNode = resolver.getNode(referencedUuid);
        UnresolvedReference unresolvedReference = null;

        if (unresolvedReferencer != null) {
            // add reference
            unresolvedReferencer.addReference(propertyName, referencedUuid);
        } else {
            if (referencedNode == null) {
                // resolved referencer, unresolved referenced
                unresolvedReference = resolver.addUnresolvedReference(referencedUuid);
                unresolvedReference.addReferencerReference(referencerNode.getProperty(XmiLexicon.JcrId.UUID).getString(),
                                                           propertyName);
            } else {
                // resolved referencer, resolved referenced
                if (!referencedNode.isNodeType(JcrConstants.MIX_REFERENCEABLE)) {
                    referencedNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
                }

                final Value weakRef = referencerNode.getSession().getValueFactory().createValue(referencedNode, true);

                if (multiValuedProperty) {
                    Value[] currentValues = null;
                    Value[] newValues = null;

                    if (referencedNode.hasProperty(propertyName)) {
                        currentValues = referencedNode.getProperty(propertyName).getValues();
                        newValues = new Value[currentValues.length + 1];
                        System.arraycopy(currentValues, 0, newValues, 0, currentValues.length);
                        newValues[currentValues.length] = weakRef;
                    } else {
                        newValues = new Value[] { weakRef };
                    }

                    referencedNode.setProperty(propertyName, newValues);
                } else {
                    // single valued
                    referencerNode.setProperty(propertyName, weakRef);
                }
            }
        }
    }
}
