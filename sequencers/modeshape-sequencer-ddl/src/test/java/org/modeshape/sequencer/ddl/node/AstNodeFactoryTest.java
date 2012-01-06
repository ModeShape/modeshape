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
package org.modeshape.sequencer.ddl.node;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.sequencer.ddl.StandardDdlLexicon;

/**
 *
 */
public class AstNodeFactoryTest {
	private AstNodeFactory nodeFactory;
	private AstNode rootNode;
	
	@Before
	public void beforeEach() {
		nodeFactory = new AstNodeFactory();
		rootNode = nodeFactory.node("testRootNode");
		rootNode.setProperty(JcrConstants.JCR_MIXIN_TYPES, StandardDdlLexicon.STATEMENTS_CONTAINER);
		rootNode.setProperty(JcrConstants.JCR_PRIMARY_TYPE, JcrConstants.NT_UNSTRUCTURED);
	}

	@Test
	public void shouldCreateChildNode() {
		String name = "myNodeName";
		
		nodeFactory.node(name, rootNode, StandardDdlLexicon.TYPE_CREATE_TABLE_STATEMENT);
		
		assertThat(rootNode.getChildCount(), is(1));
		assertEquals(rootNode.getProperty(JcrConstants.JCR_MIXIN_TYPES), StandardDdlLexicon.STATEMENTS_CONTAINER);
		assertEquals(rootNode.getChild(0).getProperty(JcrConstants.JCR_MIXIN_TYPES), StandardDdlLexicon.TYPE_CREATE_TABLE_STATEMENT);
		assertThat(rootNode.getChild(0).getName(), is(name));
	}
	
	@Test
	public void shouldHaveMixinTypeForNode() {
		String name = "myNodeName";
		nodeFactory.node(name, rootNode, StandardDdlLexicon.TYPE_CREATE_TABLE_STATEMENT);
		
		assertThat(rootNode.getChildCount(), is(1));
		assertThat(nodeFactory.hasMixinType(rootNode, StandardDdlLexicon.STATEMENTS_CONTAINER), is(true));
		assertThat(nodeFactory.hasMixinType(rootNode.getChild(0), StandardDdlLexicon.TYPE_CREATE_TABLE_STATEMENT), is(true));
	}
	
	@Test
	public void shouldFindChildForNameAndType() {
		String name = "myNodeName";
		nodeFactory.node(name, rootNode, StandardDdlLexicon.TYPE_CREATE_TABLE_STATEMENT);
		
		AstNode node = nodeFactory.getChildforNameAndType(rootNode, name, StandardDdlLexicon.TYPE_CREATE_TABLE_STATEMENT);
		
		assertThat(rootNode.getChildCount(), is(1));
		assertThat(nodeFactory.hasMixinType(rootNode, StandardDdlLexicon.STATEMENTS_CONTAINER), is(true));
		
		assertNotNull(node);
	}
	
	@Test
	public void shouldNotFindChildForRightNameWrongType() {
		String name = "myNodeName";
		nodeFactory.node(name, rootNode, StandardDdlLexicon.TYPE_CREATE_TABLE_STATEMENT);
		
		AstNode node = nodeFactory.getChildforNameAndType(rootNode, name, StandardDdlLexicon.TYPE_CREATE_VIEW_STATEMENT);
		
		assertThat(rootNode.getChildCount(), is(1));
		assertThat(nodeFactory.hasMixinType(rootNode, StandardDdlLexicon.STATEMENTS_CONTAINER), is(true));
		
		assertNull(node);
	}
	
	@Test
	public void shouldNotFindChildForWrongNameRightType() {
		String name = "myNodeName";
		nodeFactory.node(name, rootNode, StandardDdlLexicon.TYPE_CREATE_TABLE_STATEMENT);
		
		AstNode node = nodeFactory.getChildforNameAndType(rootNode, "wrongName", StandardDdlLexicon.TYPE_CREATE_TABLE_STATEMENT);
		
		assertNull(node);
	}
	
	@Test
	public void shouldSetTypeOnNode() {
		String name = "myNodeName";
		AstNode node = nodeFactory.node(name);
		assertNotNull(node);
		nodeFactory.setType(node, StandardDdlLexicon.TYPE_CREATE_TABLE_STATEMENT);
		assertThat(nodeFactory.hasMixinType(node, StandardDdlLexicon.TYPE_CREATE_TABLE_STATEMENT), is(true));
	}
}
