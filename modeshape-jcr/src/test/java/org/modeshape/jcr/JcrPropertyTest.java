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
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import javax.jcr.Binary;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.Property;

public class JcrPropertyTest extends MultiUseAbstractTest {

    protected AbstractJcrNode rootNode;
    protected AbstractJcrNode cars;
    protected AbstractJcrNode prius;
    protected AbstractJcrNode altima;
    protected AbstractJcrProperty altimaModel;
    protected JcrSession session2;
    protected AbstractJcrNode prius2;
    protected AbstractJcrProperty binaryProp;

    @BeforeClass
    public static final void beforeAll() throws Exception {
        MultiUseAbstractTest.beforeAll();

        // Import the node types and the data ...
        registerNodeTypes("cars.cnd");
        importContent("/", "io/cars-system-view-with-uuids.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

        // Create a binary property ...
        Node node = session.getRootNode().addNode("nodeWithBinaryProperty", "nt:unstructured");
        String value = "This is the string value";
        Binary binaryValue = session.getValueFactory().createBinary(new ByteArrayInputStream(value.getBytes()));
        node.setProperty("binProp", binaryValue);
        session.save();

        // Create a new workspace and import the data ...
        session.getWorkspace().createWorkspace("workspace2");
        JcrSession session2 = repository.login("workspace2");
        AbstractJcrNode session2Root = session2.getRootNode();
        importContent(session2Root, "io/cars-system-view-with-uuids.xml", ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
    }

    @AfterClass
    public static final void afterAll() throws Exception {
        MultiUseAbstractTest.afterAll();
    }

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();

        session2 = repository.login("workspace2");

        rootNode = session.getRootNode();
        cars = session.getNode("/Cars");
        prius = session.getNode("/Cars/Hybrid/Toyota Prius");
        altima = session.getNode("/Cars/Hybrid/Nissan Altima");
        altimaModel = altima.getProperty("car:model");
        binaryProp = rootNode.getNode("nodeWithBinaryProperty").getProperty("binProp");

        assertThat(rootNode, is(notNullValue()));
        assertThat(cars, is(notNullValue()));
        assertThat(prius, is(notNullValue()));
        assertThat(altima, is(notNullValue()));
        assertThat(altimaModel, is(notNullValue()));
        assertThat(binaryProp, is(notNullValue()));

        prius2 = session2.getNode("/Cars/Hybrid/Toyota Prius");
        assertThat(prius2, is(notNullValue()));
    }

    @Test
    public void shouldAllowVisitation() throws Exception {
        ItemVisitor visitor = Mockito.mock(ItemVisitor.class);
        altimaModel.accept(visitor);
        Mockito.verify(visitor).visit(altimaModel);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowVisitationIfNoVisitor() throws Exception {
        altimaModel.accept(null);
    }

    @Test(expected = ItemNotFoundException.class)
    public void shouldNotAllowNegativeAncestorDepth() throws Exception {
        altimaModel.getAncestor(-1);
    }

    @Test
    public void shouldReturnRootForAncestorOfDepthZero() throws Exception {
        assertThat(altimaModel.getAncestor(0), is((Item)rootNode));
    }

    @Test
    public void shouldReturnAncestorAtLevelOneForAncestorOfDepthOne() throws Exception {
        assertThat(altimaModel.getAncestor(1), is((Item)cars));
    }

    @Test
    public void shouldReturnSelfForAncestorOfDepthEqualToDepthOfNode() throws Exception {
        assertThat(altimaModel.getAncestor(altimaModel.getDepth()), is((Item)altimaModel));
        assertThat(altimaModel.getAncestor(altimaModel.getDepth() - 1), is((Item)altima));
    }

    @Test(expected = ItemNotFoundException.class)
    public void shouldFailToReturnAncestorWhenDepthIsGreaterThanNodeDepth() throws Exception {
        altimaModel.getAncestor(40);
    }

    @Test
    public void shouldIndicateIsNotNode() {
        assertThat(altimaModel.isNode(), is(false));
    }

    @Test
    public void shouldProvideExecutionContext() throws Exception {
        assertThat(altimaModel.context(), is(session().context()));
    }

    @Test
    public void shouldProvideName() throws Exception {
        assertThat(altimaModel.getName(), is("car:model"));
    }

    @Test
    public void shouldProvideParent() throws Exception {
        assertThat(altimaModel.getParent(), is((Node)altima));
    }

    @Test
    public void shouldProvidePath() throws Exception {
        assertThat(altimaModel.getPath(), is(altima.getPath() + "/car:model"));
    }

    @Test
    public void shouldProvideSession() throws Exception {
        assertThat(altimaModel.getSession(), is(session()));
    }

    @Test
    public void shouldReturnSameNodeEachTime() throws Exception {
        assertThat(prius.isSame(session.getNode("/Cars/Hybrid/Toyota Prius")), is(true));
    }

    @Test
    public void shouldReturnFalseFromIsSameIfTheWorkspaceNameIsDifferent() throws Exception {
        // Use the same id and location; use 'Toyota Prius'
        String priusUuid2 = prius2.getIdentifier();
        String priusUuid = prius.getIdentifier();
        assertThat(priusUuid, is(priusUuid2));
        assertThat(prius2.isSame(prius), is(false));

        // Check the properties ...
        javax.jcr.Property model = prius.getProperty("car:model");
        javax.jcr.Property model2 = prius2.getProperty("car:model");
        assertThat(model.isSame(model2), is(false));
    }

    @FixFor("MODE-1254")
    @Test
    public void shouldNotIncludeBinaryContentsInToString() throws Exception {
        // System.out.println(binaryProp.toString());
        // System.out.println(binaryProp.getParent().toString());
        assertThat(binaryProp.toString().indexOf("**binary-value") > 0, is(true));
        assertThat(binaryProp.getParent().toString().indexOf("**binary-value") > 0, is(true));
    }

    @FixFor("MODE-1308")
    @Test
    public void shouldAllowAnyBinaryImplementation() throws Exception {
        Node node = binaryProp.getParent();
        final String stringValue = "This is the string stringValue";
        Binary binaryValue = new InMemoryTestBinary(stringValue.getBytes());
        node.setProperty("binProp", binaryValue);

        // Get the actual binary value ...
        Binary nodeValue = node.getProperty("binProp").getBinary();
        assertThat(nodeValue, is(not(sameInstance(binaryValue))));
        assertThat(nodeValue, is(notNullValue()));
        assertThat(stringValue.getBytes().length, is((int)nodeValue.getSize()));

        // Check the contents ...
        byte[] buffer = new byte[100];
        int available;
        InputStream inputStream = nodeValue.getStream();
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        while ((available = inputStream.read(buffer)) != -1) {
            byteOut.write(buffer, 0, available);
        }
        assertThat(stringValue.getBytes(), is(byteOut.toByteArray()));
    }

    @FixFor( "MODE-1783" )
    @Test
    public void shouldValidateStringPropertyConstraints() throws Exception {
        JcrWorkspace workspace = session.getWorkspace();
        workspace.getNodeTypeManager().registerNodeTypes(getClass().getClassLoader().getResourceAsStream(
                "cnd/propertyWithConstraint.cnd"), true);

        Node testNode = session.getRootNode().addNode("testNode", "test:nodeType");
        try {
            testNode.setProperty("test:stringProp", "aa");
            fail("Regexp constraint not validated on property");
        } catch (ConstraintViolationException e) {
            //expected
            testNode.setProperty("test:stringProp", "a");
        }
        session.save();
        testNode = session.getNode("/testNode");

        try {
            testNode.setProperty("test:stringProp", "bb");
            fail("Regexp constraint not validated on property");
        } catch (ConstraintViolationException e) {
            //expected
        }
    }
    
    @Test
    @FixFor( "MODE-2385 ")
    public void shouldKeepOrderForMultiValuedReferenceProperties() throws Exception {
        Node node1 = session.getRootNode().addNode("node1");
        node1.addMixin(JcrConstants.MIX_REFERENCEABLE);
        JcrValue refValue1 = session.valueFactory().createValue(node1);
        Node node2 = session.getRootNode().addNode("node2");
        node2.addMixin(JcrConstants.MIX_REFERENCEABLE);
        JcrValue refValue2 = session.valueFactory().createValue(node2);
        Node node3 = session.getRootNode().addNode("node3");
        node3.addMixin(JcrConstants.MIX_REFERENCEABLE);
        JcrValue refValue3 = session.valueFactory().createValue(node3);

        Node owner = session.getRootNode().addNode("owner");
        owner.setProperty("mv-ref", new Value[]{refValue1, refValue2, refValue3});
        session.save();
        assertReferencePropertyHasPaths("/owner/mv-ref", "/node1", "/node2", "/node3");

        owner = session.getNode("/owner");
        owner.setProperty("mv-ref", new Value[]{refValue3, refValue2, refValue1});
        session.save();
        assertReferencePropertyHasPaths("/owner/mv-ref", "/node3", "/node2", "/node1");

        owner = session.getNode("/owner");
        owner.setProperty("mv-ref", new Value[]{refValue2, refValue3, refValue1});
        session.save();
        assertReferencePropertyHasPaths("/owner/mv-ref", "/node2", "/node3", "/node1");

        owner.setProperty("mv-ref", new Value[]{refValue3, refValue1});
        session.save();
        assertReferencePropertyHasPaths("/owner/mv-ref", "/node3", "/node1");

        owner.setProperty("mv-ref", new Value[]{refValue1});
        session.save();
        assertReferencePropertyHasPaths("/owner/mv-ref", "/node1");
    }
    
    private void assertReferencePropertyHasPaths(String refAbsPropertyPath, String...paths ) throws Exception {
        Property property = (Property)session.getProperty(refAbsPropertyPath);
        Value[] values = property.getValues();
        assertEquals("Incorrect number of references", paths.length, values.length);
        for (int i = 0; i < values.length; i++) {
            Value value = values[i];
            String nodeId = value.getString();
            Node referredNode = session.getNodeByIdentifier(nodeId);
            assertEquals("Incorrect referred node", paths[i], referredNode.getPath());
        }
    }
}
