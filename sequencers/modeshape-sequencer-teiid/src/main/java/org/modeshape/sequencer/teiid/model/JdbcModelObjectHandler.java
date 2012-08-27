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
import org.modeshape.sequencer.teiid.lexicon.JdbcLexicon.JcrIds;
import org.modeshape.sequencer.teiid.lexicon.JdbcLexicon.ModelIds;
import org.modeshape.sequencer.teiid.lexicon.JdbcLexicon.Namespace;
import org.modeshape.sequencer.teiid.xmi.XmiElement;
import org.modeshape.sequencer.teiid.xmi.XmiPart;

/**
 * The model object handler for the JDBC namespace.
 */
public final class JdbcModelObjectHandler extends ModelObjectHandler {

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.teiid.model.ModelObjectHandler#getQName(org.modeshape.sequencer.teiid.xmi.XmiPart)
     */
    @Override
    protected String getQName( final XmiPart xmiPart ) {
        // transform model namespace prefix into the the JCR namespace prefix
        return (JcrIds.NS_PREFIX + ':' + xmiPart.getName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.teiid.model.ModelObjectHandler#process(org.modeshape.sequencer.teiid.xmi.XmiElement, javax.jcr.Node)
     */
    @Override
    protected void process( final XmiElement element,
                            final Node parentNode ) throws Exception {
        CheckArg.isNotNull(element, "element");
        CheckArg.isNotNull(parentNode, "outputNode");
        CheckArg.isEquals(element.getNamespaceUri(), "namespace URI", Namespace.URI, "JDBC URI");

        if (DEBUG) {
            debug("==== JdbcModelObjectHandler:process:element=" + element.getName());
        }

        final String type = element.getName();
        Node newNode = null;

        if (ModelIds.SOURCE.equals(type)) {
            newNode = addNode(parentNode, element, Namespace.URI, JcrIds.SOURCE);
        } else if (ModelIds.IMPORT_SETTINGS.equals(type)) {
            newNode = addNode(parentNode, element, Namespace.URI, JcrIds.IMPORTED);
        } else if (ModelIds.EXCLUDED_OBJECT_PATHS.equals(type)) {
            addPropertyValue(parentNode, JcrIds.EXCLUDED_OBJECT_PATHS, element.getValue());
        } else if (ModelIds.INCLUDED_CATALOG_PATHS.equals(type)) {
            addPropertyValue(parentNode, JcrIds.INCLUDED_CATALOG_PATHS, element.getValue());
        } else if (ModelIds.INCLUDED_SCHEMA_PATHS.equals(type)) {
            addPropertyValue(parentNode, JcrIds.INCLUDED_SCHEMA_PATHS, element.getValue());
        } else if (ModelIds.INCLUDED_TABLE_TYPES.equals(type)) {
            addPropertyValue(parentNode, JcrIds.INCLUDED_TABLE_TYPES, element.getValue());
        }

        // process new node
        if (newNode != null) {
            setProperties(newNode, element, Namespace.URI);

            // process children
            for (final XmiElement kid : element.getChildren()) {
                process(kid, newNode);
            }
        }
    }
}
