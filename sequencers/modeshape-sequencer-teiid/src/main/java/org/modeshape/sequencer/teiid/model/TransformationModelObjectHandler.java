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
import org.modeshape.common.util.CheckArg;
import org.modeshape.sequencer.teiid.lexicon.TransformLexicon;
import org.modeshape.sequencer.teiid.xmi.XmiElement;

/**
 * The model object handler for the {@link org.modeshape.sequencer.teiid.lexicon.TransformLexicon.Namespace#URI transformation}
 * namespace.
 */
public final class TransformationModelObjectHandler extends ModelObjectHandler {

    private static final String URI = TransformLexicon.Namespace.URI;

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.teiid.model.ModelObjectHandler#process(org.modeshape.sequencer.teiid.xmi.XmiElement, javax.jcr.Node)
     */
    @Override
    protected void process( final XmiElement element,
                            final Node parentNode ) throws Exception {
        CheckArg.isNotNull(element, "element");
        CheckArg.isNotNull(parentNode, "node");
        CheckArg.isEquals(element.getNamespaceUri(), "namespace URI", URI, "relational URI");

        if (DEBUG) {
            debug("==== TransformationModelObjectHandler:process:element=" + element.getName());
        }

        final String type = element.getName();
        if (TransformLexicon.ModelIds.TRANSFORMATION_CONTAINER.equals(type)) {
        } else if (TransformLexicon.ModelIds.TRANSFORMATION_MAPPINGS.equals(type)) {
            final String targetUuid = element.getAttributeValue(TransformLexicon.ModelIds.TARGET, TransformLexicon.Namespace.URI);

            if (targetUuid != null) {
                final String uuid = getResolver().resolveInternalReference(targetUuid);
                final Node transformationTargetNode = getResolver().getNode(uuid);

                if (transformationTargetNode == null) {
                    // TODO add to unresolved
                    if (DEBUG) {
                        debug("transformaion target UUID " + uuid + " added to unresolved");
                    }

                    return;
                }

                // add SQL mixin
                transformationTargetNode.addMixin(TransformLexicon.WITH_SQL);

                // Get the transformation details ...
                for (final XmiElement kidOfMapping : element.getChildren()) {
                    if (TransformLexicon.ModelIds.HELPER.equals(kidOfMapping.getName())) {
                        final XmiElement helperNested = kidOfMapping.findChild(TransformLexicon.ModelIds.NESTED);

                        if (helperNested == null) {
                            continue;
                        }

                        setProperty(transformationTargetNode,
                                    TransformLexicon.SELECT_SQL,
                                    helperNested.getAttributeValue(TransformLexicon.ModelIds.SELECT_SQL,
                                                                   TransformLexicon.Namespace.URI));
                        setProperty(transformationTargetNode,
                                    TransformLexicon.INSERT_SQL,
                                    helperNested.getAttributeValue(TransformLexicon.ModelIds.INSERT_SQL,
                                                                   TransformLexicon.Namespace.URI));
                        setProperty(transformationTargetNode,
                                    TransformLexicon.UPDATE_SQL,
                                    helperNested.getAttributeValue(TransformLexicon.ModelIds.UPDATE_SQL,
                                                                   TransformLexicon.Namespace.URI));
                        setProperty(transformationTargetNode,
                                    TransformLexicon.DELETE_SQL,
                                    helperNested.getAttributeValue(TransformLexicon.ModelIds.DELETE_SQL,
                                                                   TransformLexicon.Namespace.URI));
                        setProperty(transformationTargetNode,
                                    TransformLexicon.INSERT_ALLOWED,
                                    helperNested.getAttributeValue(TransformLexicon.ModelIds.INSERT_ALLOWED,
                                                                   TransformLexicon.Namespace.URI));
                        setProperty(transformationTargetNode,
                                    TransformLexicon.UPDATE_ALLOWED,
                                    helperNested.getAttributeValue(TransformLexicon.ModelIds.UPDATE_ALLOWED,
                                                                   TransformLexicon.Namespace.URI));
                        setProperty(transformationTargetNode,
                                    TransformLexicon.DELETE_ALLOWED,
                                    helperNested.getAttributeValue(TransformLexicon.ModelIds.DELETE_ALLOWED,
                                                                   TransformLexicon.Namespace.URI));
                        setProperty(transformationTargetNode,
                                    TransformLexicon.INSERT_SQL_DEFAULT,
                                    helperNested.getAttributeValue(TransformLexicon.ModelIds.INSERT_SQL_DEFAULT,
                                                                   TransformLexicon.Namespace.URI));
                        setProperty(transformationTargetNode,
                                    TransformLexicon.UPDATE_SQL_DEFAULT,
                                    helperNested.getAttributeValue(TransformLexicon.ModelIds.UPDATE_SQL_DEFAULT,
                                                                   TransformLexicon.Namespace.URI));
                        setProperty(transformationTargetNode,
                                    TransformLexicon.DELETE_SQL_DEFAULT,
                                    helperNested.getAttributeValue(TransformLexicon.ModelIds.DELETE_SQL_DEFAULT,
                                                                   TransformLexicon.Namespace.URI));
                    } else if (TransformLexicon.ModelIds.INPUTS.equals(kidOfMapping.getName())) {
                        // Record the inputs to the transformed object ...
                        // String inputHref = kidOfMapping.getAttributeValue(TransformLexicon.ModelIds.HREF,
                        // TransformLexicon.Namespace.URI);
                        //
                        // if (inputHref != null) {
                        // props.addRef(TransformLexicon.INPUTS, inputHref);
                        // }
                    } else if (TransformLexicon.ModelIds.NESTED.equals(kidOfMapping.getName())) {
                        // Record the nested transformation ...
                        // String outputHref = kidOfMapping.getAttributeValue(TransformLexicon.ModelIds.OUTPUTS,
                        // TransformLexicon.Namespace.URI);
                        // String nestedMmuuid = this.resolver.resolveInternalReference(outputHref);
                        // PropertySet nestedProps = propertiesFor(nestedMmuuid, true);
                        //
                        // for (XmiElement nestedKid : kidOfMapping.getChildren()) {
                        // if (TransformLexicon.ModelIds.INPUTS.equals(nestedKid)) {
                        // String inputHref = nestedKid.getAttributeValue(TransformLexicon.ModelIds.HREF,
                        // TransformLexicon.Namespace.URI);
                        // nestedProps.addRef(TransformLexicon.INPUTS, inputHref);
                        // nestedProps.add(JcrLexicon.MIXIN_TYPES.getString(), TransformLexicon.TRANSFORMED);
                        // }
                        // }
                    }
                }
            }
        } else {
            if (DEBUG) {
                debug("**** transformatoin type of " + type + " was not processed");
            }
        }
    }
}
