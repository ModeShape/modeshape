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
package org.modeshape.sequencer.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
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
