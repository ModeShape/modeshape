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
import org.modeshape.sequencer.teiid.lexicon.RelationalLexicon;
import org.modeshape.sequencer.teiid.lexicon.RelationalLexicon.JcrIds;
import org.modeshape.sequencer.teiid.lexicon.RelationalLexicon.ModelIds;
import org.modeshape.sequencer.teiid.xmi.XmiElement;

/**
 * The model object handler for the {@link org.modeshape.sequencer.teiid.lexicon.RelationalLexicon.Namespace#URI relational}
 * namespace.
 */
public final class RelationalModelObjectHandler extends ModelObjectHandler {

    private static final String URI = RelationalLexicon.Namespace.URI;

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.teiid.model.ModelObjectHandler#process(org.modeshape.sequencer.teiid.xmi.XmiElement, javax.jcr.Node)
     */
    @Override
    public void process( final XmiElement element,
                         final Node parentNode ) throws Exception {
        // Note: As of Sep 2011 Schema and Catalog no long can be created using Designer

        CheckArg.isNotNull(element, "element");
        CheckArg.isNotNull(parentNode, "node");
        CheckArg.isEquals(element.getNamespaceUri(), "namespace URI", URI, "relational URI");

        if (DEBUG) {
            debug("==== RelationalModelObjectHandler:process:element=" + element.getName());
        }

        final String type = element.getName();
        Node newNode = null;

        // TODO uniqueKey, uniqueConstraint, logicalRelationships, indexes, primaryKey, foreignKey, index, accessPattern,
        if (ModelIds.BASE_TABLE.equals(type)) {
            newNode = addNode(parentNode, element, URI, JcrIds.BASE_TABLE);
        } else if (ModelIds.COLUMNS.equals(type)) {
            newNode = addNode(parentNode, element, URI, JcrIds.COLUMN);
        } else if (ModelIds.VIEW.equals(type)) {
            newNode = addNode(parentNode, element, URI, JcrIds.VIEW);
        } else if (ModelIds.PROCEDURE_PARAMETER.equals(type)) {
            newNode = addNode(parentNode, element, URI, JcrIds.PROCEDURE_PARAMETER);
        } else if (ModelIds.PROCEDURE_RESULT.equals(type)) {
            newNode = addNode(parentNode, element, URI, JcrIds.PROCEDURE_RESULT);
        } else if (ModelIds.PROCEDURE.equals(type)) {
            newNode = addNode(parentNode, element, URI, JcrIds.PROCEDURE);
        } else if (ModelIds.PROCEDURES.equals(type)) {
            newNode = addNode(parentNode, element, URI, JcrIds.PROCEDURE);
        } else if (ModelIds.SCHEMA.equals(type)) {
            newNode = addNode(parentNode, element, URI, JcrIds.SCHEMA);
        } else if (ModelIds.SCHEMAS.equals(type)) {
            newNode = addNode(parentNode, element, URI, JcrIds.SCHEMA);
        } else if (ModelIds.CATALOG.equals(type)) {
            newNode = addNode(parentNode, element, URI, JcrIds.CATALOG);
        } else if (ModelIds.TYPE.equals(type)) {
            // TODO implement
        } else if (ModelIds.TABLES.equals(type)) {
            newNode = addNode(parentNode, element, URI, JcrIds.BASE_TABLE);
        } else if (ModelIds.ACCESS_PATTERN.equals(type)) {
            newNode = addNode(parentNode, element, URI, JcrIds.ACCESS_PATTERN);
        } else {
            if (DEBUG) {
                debug("**** relational type of " + type + " was not processed");
            }
        }

        // process new node
        if (newNode != null) {
            setProperties(newNode, element, URI);

            // process children
            for (final XmiElement kid : element.getChildren()) {
                process(kid, newNode);
            }
        }
    }
}
