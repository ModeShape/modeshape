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
 * Lesser General Public License for more details
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org
 */
package org.modeshape.sequencer.xml;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;

/**
 * Base test class for the different {@link XmlSequencer} implementation tests
 *
 * @author Horia Chiorean
 */
public abstract class AbstractXmlSequencerTest extends AbstractSequencerTest {

    protected Node sequenceAndAssertDocument( String documentFilename ) throws Exception {
        createNodeWithContentFromFile(documentFilename, documentFilename);
        Node document = getOutputNode(rootNode, "xml/" + documentFilename);
        assertNotNull(document);
        assertEquals(XmlLexicon.DOCUMENT, document.getPrimaryNodeType().getName());
        return document;
    }

    protected Node assertElement( Node document,
                                  String elementRelativePath,
                                  String... propertyNameValuePairs ) throws RepositoryException {
        return assertNode(document, elementRelativePath, XmlLexicon.ELEMENT, propertyNameValuePairs);
    }

    protected Node assertNode( Node rootNode,
                             String relativePath,
                             String expectedType,
                             String... propertyNameValuePairs ) throws RepositoryException {
        Node node = rootNode.getNode(relativePath);
        assertNotNull(node);
        assertEquals(expectedType, node.getPrimaryNodeType().getName());

        for (String nameValuePair : propertyNameValuePairs) {
            String[] elements = nameValuePair.split("=", 2);
            assertEquals(2, elements.length);
            String expectedName = elements[0];
            String expectedValue = elements[1];

            Property elementProperty = node.getProperty(expectedName);
            assertNotNull("Property not found", elementProperty);
            assertEquals(expectedValue, elementProperty.getString());

        }
        return node;
    }
}
