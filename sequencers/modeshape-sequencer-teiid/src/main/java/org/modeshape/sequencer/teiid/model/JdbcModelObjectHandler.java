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

import static org.modeshape.sequencer.teiid.lexicon.JdbcLexicon.Namespace.URI;
import javax.jcr.Node;
import org.modeshape.common.util.CheckArg;
import org.modeshape.sequencer.teiid.lexicon.JdbcLexicon.JcrId;
import org.modeshape.sequencer.teiid.lexicon.JdbcLexicon.ModelId;
import org.modeshape.sequencer.teiid.xmi.XmiElement;
import org.modeshape.sequencer.teiid.xmi.XmiPart;

/**
 * The model object handler for the JDBC namespace.
 */
public final class JdbcModelObjectHandler extends ModelObjectHandler {

    /**
     * @see org.modeshape.sequencer.teiid.model.ModelObjectHandler#getQName(org.modeshape.sequencer.teiid.xmi.XmiPart)
     */
    @Override
    protected String getQName( final XmiPart xmiPart ) {
        // transform model namespace prefix into the the JCR namespace prefix
        return (JcrId.NS_PREFIX + ':' + xmiPart.getName());
    }

    /**
     * @see org.modeshape.sequencer.teiid.model.ModelObjectHandler#process(org.modeshape.sequencer.teiid.xmi.XmiElement,
     *      javax.jcr.Node)
     */
    @Override
    protected void process( final XmiElement element,
                            final Node parentNode ) throws Exception {
        CheckArg.isNotNull(element, "element");
        CheckArg.isNotNull(parentNode, "outputNode");
        CheckArg.isEquals(element.getNamespaceUri(), "namespace URI", URI, "JDBC URI");

        if (DEBUG) {
            debug("==== JdbcModelObjectHandler:process:element=" + element.getName());
        }

        final String type = element.getName();
        Node newNode = null;

        if (ModelId.SOURCE.equals(type)) {
            newNode = addNode(parentNode, element, URI, JcrId.SOURCE);
        } else if (ModelId.IMPORT_SETTINGS.equals(type)) {
            newNode = addNode(parentNode, element, URI, JcrId.IMPORTED);
        } else if (ModelId.EXCLUDED_OBJECT_PATHS.equals(type)) {
            addPropertyValue(parentNode, JcrId.EXCLUDED_OBJECT_PATHS, element.getValue());
        } else if (ModelId.INCLUDED_CATALOG_PATHS.equals(type)) {
            addPropertyValue(parentNode, JcrId.INCLUDED_CATALOG_PATHS, element.getValue());
        } else if (ModelId.INCLUDED_SCHEMA_PATHS.equals(type)) {
            addPropertyValue(parentNode, JcrId.INCLUDED_SCHEMA_PATHS, element.getValue());
        } else if (ModelId.INCLUDED_TABLE_TYPES.equals(type)) {
            addPropertyValue(parentNode, JcrId.INCLUDED_TABLE_TYPES, element.getValue());
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
