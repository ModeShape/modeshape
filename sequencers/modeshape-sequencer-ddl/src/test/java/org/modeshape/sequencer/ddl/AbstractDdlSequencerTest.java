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
package org.modeshape.sequencer.ddl;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.modeshape.jcr.api.JcrConstants.JCR_MIXIN_TYPES;
import static org.modeshape.jcr.api.JcrConstants.JCR_PRIMARY_TYPE;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.*;
import java.util.*;

/**
 * Base class for all the {@link DdlSequencer} tests.
 *
 * @author Horia Chiorean
 */
public abstract class AbstractDdlSequencerTest extends AbstractSequencerTest {

    protected void verifyProperty( Node node, String propertyName, String expectedValue ) throws RepositoryException {
        Property property = node.getProperty(propertyName);
        Value value = property.isMultiple() ? property.getValues()[0] : property.getValue();
        assertEquals(expectedValue, value.getString());
    }

    protected void verifyProperty( Node node, String propertyName, long expectedValue ) throws RepositoryException {
        Property property = node.getProperty(propertyName);
        Value value = property.isMultiple() ? property.getValues()[0] : property.getValue();
        assertEquals(expectedValue, value.getLong());
    }

    protected boolean verifyHasProperty( Node node, String propNameStr ) throws RepositoryException {
        return node.hasProperty(propNameStr);
    }

    protected void verifyPrimaryType( Node node, String expectedValue ) throws RepositoryException {
        verifyProperty(node, JCR_PRIMARY_TYPE, expectedValue);
    }

    protected void verifyMixinType( Node node, String expectedValue ) throws RepositoryException {
        verifyProperty(node, JCR_MIXIN_TYPES, expectedValue);
    }
    
    protected void verifyMixinTypes( Node node, String... expectedValues ) throws RepositoryException {
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

    protected void verifyExpression( Node node, String expectedValue ) throws RepositoryException {
        verifyProperty(node, DDL_EXPRESSION, expectedValue);
    }

    protected void verifyBaseProperties( Node node, String primaryType, String lineNum, String colNum, String charIndex, long numChildren ) throws RepositoryException {
        verifyPrimaryType(node, primaryType);
        verifyProperty(node, DDL_START_LINE_NUMBER, lineNum);
        verifyProperty(node, DDL_START_COLUMN_NUMBER, colNum);
        verifyProperty(node, DDL_START_CHAR_INDEX, charIndex);
        assertThat(node.getNodes().getSize(), is(numChildren));
    }

    protected Node findNode( Node parent, String nodePath, String... mixinTypes ) throws Exception {
        Node child = parent.getNode(nodePath);
        assertNotNull(child);
        verifyMixinTypes(child, mixinTypes);
        return child;
    }

    protected Node sequenceDdl( String ddlFile ) throws Exception {
        String fileName = ddlFile.substring(ddlFile.lastIndexOf("/") + 1);
        createNodeWithContentFromFile(fileName, ddlFile);
        Node sequencedNode = getSequencedNode(rootNode, "ddl/" + fileName);

        assertNotNull(sequencedNode);
        assertThat(sequencedNode.getNodes().getSize(), is(1l));

        Node statementsNode = sequencedNode.getNode(STATEMENTS_CONTAINER);
        assertNotNull(statementsNode);
        return statementsNode;
    }
}
