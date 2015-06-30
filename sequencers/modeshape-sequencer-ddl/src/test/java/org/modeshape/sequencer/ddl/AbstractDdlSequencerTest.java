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
package org.modeshape.sequencer.ddl;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.modeshape.jcr.api.JcrConstants.JCR_MIXIN_TYPES;
import static org.modeshape.jcr.api.JcrConstants.JCR_PRIMARY_TYPE;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DDL_EXPRESSION;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DDL_START_CHAR_INDEX;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DDL_START_COLUMN_NUMBER;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DDL_START_LINE_NUMBER;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.STATEMENTS_CONTAINER;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;

/**
 * Base class for all the {@link DdlSequencer} tests.
 * 
 * @author Horia Chiorean
 */
public abstract class AbstractDdlSequencerTest extends AbstractSequencerTest {

    /**
     * @param node the node that has a reference to another node (cannot be <code>null</code>)
     * @param uuid the identifier of the referenced node (cannot be empty)
     * @param resolvedPath the path of the referenced node (cannot be empty)
     * @throws RepositoryException if an error occurs
     * @throws AssertionError if the resolved path does not equal the path of the referenced node
     */
    protected void verifyReference( final Node node,
                                    final String uuid,
                                    final String resolvedPath ) throws RepositoryException {
        final Session session = node.getSession();
        final Node referencedNode = session.getNodeByIdentifier(uuid);
        assertEquals(resolvedPath, referencedNode.getPath());
    }

    protected void verifyProperty( Node node,
                                   String propertyName,
                                   String expectedValue ) throws RepositoryException {
        Property property = node.getProperty(propertyName);
        Value value = property.isMultiple() ? property.getValues()[0] : property.getValue();
        assertEquals(expectedValue, value.getString());
    }

    protected void verifyProperty( Node node,
                                   String propertyName,
                                   long expectedValue ) throws RepositoryException {
        Property property = node.getProperty(propertyName);
        Value value = property.isMultiple() ? property.getValues()[0] : property.getValue();
        assertEquals(expectedValue, value.getLong());
    }

    protected boolean verifyHasProperty( Node node,
                                         String propNameStr ) throws RepositoryException {
        return node.hasProperty(propNameStr);
    }

    protected void verifyPrimaryType( Node node,
                                      String expectedValue ) throws RepositoryException {
        verifyProperty(node, JCR_PRIMARY_TYPE, expectedValue);
    }

    protected void verifyMixinType( Node node,
                                    String expectedValue ) throws RepositoryException {
        verifyProperty(node, JCR_MIXIN_TYPES, expectedValue);
    }

    protected void verifyMixinTypes( Node node,
                                     String... expectedValues ) throws RepositoryException {
        Value[] values = node.getProperty(JCR_MIXIN_TYPES).getValues();
        Set<String> valuesSet = new TreeSet<String>();
        for (Value value : values) {
            valuesSet.add(value.getString());
        }
        List<String> expectedValuesList = new ArrayList<String>(Arrays.asList(expectedValues));
        for (Iterator<String> expectedValuesIterator = expectedValuesList.iterator(); expectedValuesIterator.hasNext();) {
            assertTrue(valuesSet.contains(expectedValuesIterator.next()));
            expectedValuesIterator.remove();
        }
        assertTrue(expectedValuesList.isEmpty());
    }

    protected void verifyExpression( Node node,
                                     String expectedValue ) throws RepositoryException {
        verifyProperty(node, DDL_EXPRESSION, expectedValue);
    }

    protected void verifyBaseProperties( Node node,
                                         String primaryType,
                                         String lineNum,
                                         String colNum,
                                         String charIndex,
                                         long numChildren ) throws RepositoryException {
        verifyPrimaryType(node, primaryType);
        verifyProperty(node, DDL_START_LINE_NUMBER, lineNum);
        verifyProperty(node, DDL_START_COLUMN_NUMBER, colNum);
        verifyProperty(node, DDL_START_CHAR_INDEX, charIndex);
        assertThat(node.getNodes().getSize(), is(numChildren));
    }

    protected Node findNode( Node parent,
                             String nodePath,
                             String... mixinTypes ) throws Exception {
        Node child = parent.getNode(nodePath);
        assertNotNull(child);
        verifyMixinTypes(child, mixinTypes);
        return child;
    }

    protected Node sequenceDdl( String ddlFile,
                                final int waitTimeSeconds ) throws Exception {
        String fileName = ddlFile.substring(ddlFile.lastIndexOf("/") + 1);
        createNodeWithContentFromFile(fileName, ddlFile);
        Node outputNode = getOutputNode(rootNode, "ddl/" + fileName, waitTimeSeconds);

        assertNotNull(outputNode);
        assertThat(outputNode.getNodes().getSize(), is(1l));

        Node statementsNode = outputNode.getNode(STATEMENTS_CONTAINER);
        assertNotNull(statementsNode);
        return statementsNode;
    }

    protected Node sequenceDdl( String ddlFile ) throws Exception {
        String fileName = ddlFile.substring(ddlFile.lastIndexOf("/") + 1);
        createNodeWithContentFromFile(fileName, ddlFile);
        Node outputNode = getOutputNode(rootNode, "ddl/" + fileName);

        assertNotNull(outputNode);
        assertThat(outputNode.getNodes().getSize(), is(1l));

        Node statementsNode = outputNode.getNode(STATEMENTS_CONTAINER);
        assertNotNull(statementsNode);
        return statementsNode;
    }
}
