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
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import javax.jcr.Binary;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.modeshape.common.FixFor;

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

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowVisitationIfNoVisitor() throws Exception {
        altimaModel.accept(null);
    }

    @Test( expected = ItemNotFoundException.class )
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

    @Test( expected = ItemNotFoundException.class )
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

    @FixFor( "MODE-1254" )
    @Test
    public void shouldNotIncludeBinaryContentsInToString() throws Exception {
        // System.out.println(binaryProp.toString());
        // System.out.println(binaryProp.getParent().toString());
        assertThat(binaryProp.toString().indexOf("**binary-value") > 0, is(true));
        assertThat(binaryProp.getParent().toString().indexOf("**binary-value") > 0, is(true));
    }

    @FixFor( "MODE-1308" )
    @Test
    public void shouldAllowAnyBinaryImplementation() throws Exception {
        Node node = binaryProp.getParent();
        final String stringValue = "This is the string stringValue";
        Binary binaryValue = new Binary() {
            @Override
            public InputStream getStream() {
                return new ByteArrayInputStream(stringValue.getBytes());
            }

            @Override
            public int read( byte[] b,
                             long position ) {
                byte[] content = stringValue.getBytes();
                int length = b.length + position < content.length ? b.length : (int)(content.length - position);
                System.arraycopy(content, (int)position, b, 0, length);
                return length;
            }

            @Override
            public long getSize() {
                return stringValue.getBytes().length;
            }

            @Override
            public void dispose() {
            }
        };
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
}
