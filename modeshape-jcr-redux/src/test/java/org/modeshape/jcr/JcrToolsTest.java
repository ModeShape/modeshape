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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.jcr.api.JcrTools;

/**
 *
 */
public class JcrToolsTest extends SingleUseAbstractTest {
    private JcrTools tools;
    private Node personNode;
    private Node addressNode;
    private Problems problems;
    private Node NULL_NODE;
    private String NULL_STRING;

    private static final String DEF_TYPE = "nt:unstructured";

    @Before
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        tools = new JcrTools();

        Node rootNode = session.getRootNode();

        personNode = rootNode.addNode("Person");
        personNode.setProperty("First Name", "Ryan");
        personNode.setProperty("Middle Name", "Joseph");
        personNode.setProperty("Last Name", "Franklin");
        personNode.setProperty("Age", 37);
        personNode.setProperty("Children", new String[] {"Sally", "Brent", "Michael"});

        addressNode = personNode.addNode("Address");
        addressNode.setProperty("Street", "Frost Avenue");
        addressNode.setProperty("House Number", 166);
        addressNode.setProperty("City", "Flagstaff");
        addressNode.setProperty("State", "AZ");
        addressNode.setProperty("Country", "US");
        addressNode.setProperty("Zip Code", 77777);

        problems = new SimpleProblems();
    }

    @Override
    @After
    public void afterEach() throws Exception {
        super.afterEach();
    }

    @Test
    public void shouldGetNode() throws RepositoryException {
        Node node = tools.getNode(session.getRootNode(), "Person", true);
        assertNotNull(node);
        assertThat(node.getName(), is("Person"));
        assertThat(problems.size(), is(0));
    }

    @Test
    public void shouldFailGetNodeWithNoDoesntExist() {
        try {
            tools.getNode(session.getRootNode(), "Animal", true);
        } catch (Exception e) {
            assertTrue(e instanceof PathNotFoundException);
        }
    }

    @Test
    public void shouldFailGetNodeNullParent() {
        try {
            tools.getNode(NULL_NODE, "Person", true);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void shouldFailGetNodeNullPath() {
        try {
            tools.getNode(personNode, NULL_STRING, true);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testGetReadable() {
        String personStr = tools.getReadable(personNode);
        assertThat(personStr, is("/Person"));
    }

    @Test
    public void testCreateNodeSessionPath() throws RepositoryException {
        Node node = tools.findOrCreateNode(session, "Hobby");
        assertNotNull(node);
        assertThat(node.getName(), is("Hobby"));
    }

    @Test
    public void testFindNodeSessionPath() throws RepositoryException {
        Node node = tools.findOrCreateNode(session, "Person");
        assertNotNull(node);
        assertThat(node.getName(), is("Person"));
    }

    @Test
    public void shouldFailFindOrCreateNodeSessionPathNullSession() {
        Session sess = null;
        try {
            tools.findOrCreateNode(sess, "Person");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void shouldFailFindOrCreateNodeSessionPathNullPath() {
        try {
            tools.findOrCreateNode(session, NULL_STRING);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testCreateNodeSessionPathType() throws RepositoryException {
        Node node = tools.findOrCreateNode(session, "Hobby", DEF_TYPE);
        assertNotNull(node);
        assertThat(node.getName(), is("Hobby"));
    }

    @Test
    public void testFindNodeSessionPathType() throws RepositoryException {
        Node node = tools.findOrCreateNode(session, "Person", DEF_TYPE);
        assertNotNull(node);
        assertThat(node.getName(), is("Person"));
    }

    @Test
    public void shouldFailFindOrCreateNodeSessionPathTypeNullSession() {
        Session sess = null;
        try {
            tools.findOrCreateNode(sess, "Person", DEF_TYPE);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void shouldFailFindOrCreateNodeSessionPathTypeNullPath() {
        try {
            tools.findOrCreateNode(session, NULL_STRING, DEF_TYPE);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testCreateNodeSessionPathTypeType() throws RepositoryException {
        Node node = tools.findOrCreateNode(session, "Hobby", DEF_TYPE, DEF_TYPE);
        assertNotNull(node);
        assertThat(node.getName(), is("Hobby"));
    }

    @Test
    public void testFindNodeSessionPathTypeType() throws RepositoryException {
        Node node = tools.findOrCreateNode(session, "Person", DEF_TYPE, DEF_TYPE);
        assertNotNull(node);
        assertThat(node.getName(), is("Person"));
    }

    @Test
    public void shouldFailFindOrCreateNodeSessionPathTypeTypeNullSession() {
        Session sess = null;
        try {
            tools.findOrCreateNode(sess, "Person", DEF_TYPE, DEF_TYPE);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void shouldFailFindOrCreateNodeSessionPathTypeTypeNullPath() {
        try {
            tools.findOrCreateNode(session, NULL_STRING, DEF_TYPE, DEF_TYPE);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void shouldCreateNodeNodePathTypeType() throws RepositoryException {
        Node node = tools.findOrCreateNode(personNode, "Hobby", DEF_TYPE, "nt:unstructured");
        assertNotNull(node);
        assertThat(node.getName(), is("Hobby"));
    }

    @Test
    public void shouldFindNodeParentPathTypeType() throws RepositoryException {
        Node node = tools.findOrCreateNode(personNode, "Address", DEF_TYPE, DEF_TYPE);
        assertNotNull(node);
        assertThat(node.getName(), is("Address"));
    }

    @Test
    public void shouldFailFindOrCreateNodeNodePathTypeTypeNullNode() {
        try {
            tools.findOrCreateNode(NULL_NODE, "/topNode", DEF_TYPE, DEF_TYPE);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void shouldFailFindOrCreateNodeNodePathTypeTypeNullPath() {
        try {
            tools.findOrCreateNode(personNode, NULL_STRING, DEF_TYPE, DEF_TYPE);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void shouldCreateChildNodeWithParentName() throws RepositoryException {
        Node childNode = tools.findOrCreateChild(personNode, "Hobby");
        assertNotNull(childNode);
        assertThat(childNode.getName(), is("Hobby"));
    }

    @Test
    public void shouldFindChildNodeWithParentName() throws RepositoryException {
        Node childNode = tools.findOrCreateChild(personNode, "Address");
        assertNotNull(childNode);
        assertThat(childNode.getName(), is("Address"));
    }

    @Test
    public void shouldFailFindOrCreateChildNodeStringNullNode() {
        try {
            tools.findOrCreateChild(NULL_NODE, "childNode");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void shouldFailFindOrCreateChildNodeStringNullPath() {
        try {
            tools.findOrCreateChild(addressNode, NULL_STRING);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testCreateChildNodeWithParentNameType() throws RepositoryException {
        Node childNode = tools.findOrCreateChild(personNode, "Hobby", DEF_TYPE);
        assertNotNull(childNode);
        assertThat(childNode.getName(), is("Hobby"));
    }

    @Test
    public void testFindChildNodeWithParentNameType() throws RepositoryException {
        Node childNode = tools.findOrCreateChild(personNode, "Address", DEF_TYPE);
        assertNotNull(childNode);
        assertThat(childNode.getName(), is("Address"));
    }

    @Test
    public void shouldFailFindOrCreateChildWithNullParentNameType() {
        try {
            tools.findOrCreateChild(NULL_NODE, "childNode", DEF_TYPE);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void shouldFailFindOrCreateChildNodeStringStringNullPath() {
        try {
            tools.findOrCreateChild(addressNode, NULL_STRING, DEF_TYPE);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }
}
