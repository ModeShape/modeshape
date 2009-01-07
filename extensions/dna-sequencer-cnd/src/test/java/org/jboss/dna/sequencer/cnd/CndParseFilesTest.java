/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.sequencer.cnd;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RuleReturnScope;
import org.antlr.runtime.tree.Tree;
import org.junit.After;
import org.junit.Test;

public class CndParseFilesTest {

    /*
     * AST looks like this:
     * 
     * root (nil)
     *      COMMENTS (*)
     *           comment-1
     *           comment-2
     *           ...
     *      NAMESPACES (*)
     *           namespace_mapping-1
     *           namespace_mapping-2
     *           ...
     *      NODE_TYPE_DEFINITIONS (*)
     *           NODE_TYPE_DEFINITION
     *                JCR_NAME
     *                     node_type_definition_name-1
     *                SUPERTYPES (*)
     *                     supertype-1
     *                     supertype-2
     *                     ...
     *                NODE_TYPE_OPTIONS (*)
     *                     option-1
     *                     option-2
     *                     ...
     *                PROPERTIES (*)
     *                     PROPERTY_DEFINITION
     *                          JCR_NAME
     *                               property_name-1
     *                          PROPERTY_TYPE (*)
     *                               property type
     *                          ATTRIBUTES (*)
     *                               attribute-1
     *                               attribute-2
     *                               ...
     *                     PROPERTY_DEFINITION
     *                          JCR_NAME
     *                               property_name-2
     *                          ...
     *                     ...
     *                CHILD_NODE_DEFINTIONS (*)
     *                     CHILD_NODE_DEFINTION
     *                          JCR_NAME
     *                              child_node_name-1
     *                          REQUIRED_TYPES (*)
     *                              required_type-1
     *                              required_type-2
     *                              ...
     *                          DEFAULT_TYPE (*)
     *                              default_type
     *                          ATTRIBUTES (*)
     *                              attribute-1
     *                              attribute-2
     *                              ...
     *                     CHILD_NODE_DEFINTION
     *                          JCR_NAME
     *                              child_node_name-2
     *                          ...
     *                     ...
     *           NODE_TYPE_DEFINITION
     *                JCR_NAME
     *                     node_type_definition_name-2
     *                ...
     *           ...
     *           
     *  Note: (*) indicates node only appears if a child node exists
     */

    // =============================================================================================================================
    // Constants
    // =============================================================================================================================
    private static final String TEST_DATA_PATH = "./src/test/resources/";

    private static final String BUILTIN_NODETYPES_CND = TEST_DATA_PATH + "builtin_nodetypes.cnd";
    private static final String EMPTY_CND = TEST_DATA_PATH + "empty.cnd";
    private static final String IMAGES_CND = TEST_DATA_PATH + "images.cnd";
    private static final String INVALID_CND = TEST_DATA_PATH + "invalid.cnd";
    private static final String MP3_CND = TEST_DATA_PATH + "mp3.cnd";

    private static final int COMMENTS_INDEX = 0;
    private static final int NAMESPACES_INDEX = 1;
    private static final int NODE_TYPE_DEFINITIONS_INDEX = 2;
    private static final int PROPERTIES_INDEX = 2;

    // =============================================================================================================================
    // Fields
    // =============================================================================================================================

    private InputStream stream;

    // =============================================================================================================================
    // Methods
    // =============================================================================================================================

    @After
    public void afterEach() throws IOException {
        if (this.stream != null) {
            try {
                this.stream.close();
            } finally {
                this.stream = null;
            }
        }
    }

    private void assertCommentsCount( Tree root,
                                      int count ) {
        Tree comments = root.getChild(COMMENTS_INDEX);
        assertThat("Tree type was not CndParser.COMMENTS", comments.getType(), is(CndParser.COMMENTS));

        // make sure right number of comments
        assertThat("Incorrect number of comments", comments.getChildCount(), is(count));
    }

    private void assertNamespacesCount( Tree root,
                                        int count ) {
        Tree namespaces = getNamespaces(root);
        assertThat("Incorrect number of namespaces", namespaces.getChildCount(), is(count));
    }

    private void assertNodeTypeDefinitionsCount( Tree root,
                                                 int count ) {
        Tree nodeTypeDefinitions = getNodeTypeDefinitions(root);
        assertThat("Incorrect number of node type definitions", nodeTypeDefinitions.getChildCount(), is(count));
    }

    private void assertNodeTypeDefinitionsPropertiesCount( Tree root,
                                                           int nodeTypeDefinitionIndex,
                                                           int count ) {
        Tree nodeTypeDefinitions = getNodeTypeDefinitions(root);

        Tree nodeType = nodeTypeDefinitions.getChild(nodeTypeDefinitionIndex);
        assertThat("Tree type was not CndParser.NODE_TYPE_DEFINITION", nodeType.getType(), is(CndParser.NODE_TYPE_DEFINITION));

        Tree properties = nodeType.getChild(PROPERTIES_INDEX); // properties container node
        assertThat("Tree type was not CndParser.PROPERTIES", properties.getType(), is(CndParser.PROPERTIES));

        // make sure right number of properties
        assertThat("Incorrect number of node type properties for first node type", properties.getChildCount(), is(count));
    }

    private CndParser createParser( File file ) throws IOException {
        afterEach(); // make sure previous stream is closed
        assertThat('\'' + file.getAbsolutePath() + "' does not exist", file.exists(), is(true));

        this.stream = new FileInputStream(file);
        CndLexer lexer = new CndLexer(new ANTLRInputStream(this.stream));
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        return new CndParser(tokens);
    }

    /**
     * @param root the AST root
     * @return the NAMESPACES tree node
     */
    private Tree getNamespaces( Tree root ) {
        Tree namespaces = root.getChild(NAMESPACES_INDEX);

        if (namespaces != null) {
            assertThat("Tree type was not CndParser.NAMESPACES", namespaces.getType(), is(CndParser.NAMESPACES));
        }

        return namespaces;
    }

    /**
     * @param root the AST root
     * @return the NODE_TYPE_DEFINITIONS tree node
     */
    private Tree getNodeTypeDefinitions( Tree root ) {
        Tree nodeTypeDefinitions = root.getChild(NODE_TYPE_DEFINITIONS_INDEX);

        if (nodeTypeDefinitions != null) {
            assertThat("Tree type was not CndParser.NODE_TYPE_DEFINITIONS",
                       nodeTypeDefinitions.getType(),
                       is(CndParser.NODE_TYPE_DEFINITIONS));
        }

        return nodeTypeDefinitions;
    }

    /**
     * @param fileName the name of the file to parse
     * @return the root node of the AST
     * @throws Exception if root is null
     */
    private Tree parse( String fileName ) throws Exception {
        return parse(new File(fileName).getCanonicalFile());
    }

    /**
     * @param file the file to parse
     * @return the root node of the AST
     * @throws Exception if root is null
     */
    private Tree parse( File file ) throws Exception {
        CndParser parser = createParser(file);
        RuleReturnScope result = parser.cnd();
        Tree root = (Tree)result.getTree();
        assertThat(root, notNullValue());

        return root;
    }

    // =============================================================================================================================
    // Tests
    // =============================================================================================================================

    @Test
    public void shouldParseEmptyCnd() throws Exception {
        Tree root = parse(EMPTY_CND);

        // make sure no children
        assertThat(root.getChildCount(), is(0));

        // make sure nothing got parsed
        assertThat(root.getTokenStartIndex(), is(-1));
    }

    @Test
    public void shouldNotParseInvalidCnd() throws Exception {
        Tree root = parse(INVALID_CND);

        // make sure nothing got parsed
        assertThat(root.getTokenStopIndex(), is(0));
    }

    // @Test
    public void shouldParseBuiltinNodeTypesCndComments() throws Exception {
        Tree root = parse(BUILTIN_NODETYPES_CND);
        assertCommentsCount(root, 16);
    }

    @Test
    public void shouldParseBuiltinNodeTypesCndNamespaces() throws Exception {
        Tree root = parse(BUILTIN_NODETYPES_CND);
        assertNamespacesCount(root, 4);
    }

    // @Test
    public void shouldParseBuiltinNodeTypesCndNodeTypeDefinitions() throws Exception {
        Tree root = parse(BUILTIN_NODETYPES_CND);

        // make sure correct number of node type definitions
        assertNodeTypeDefinitionsCount(root, 23);

        // make sure right number of node type definitions for each node type
        assertNodeTypeDefinitionsPropertiesCount(root, 0, 2); // [nt:base]
        assertNodeTypeDefinitionsPropertiesCount(root, 1, 2); // [nt:unstructured]
        assertNodeTypeDefinitionsPropertiesCount(root, 2, 1); // [mix:referenceable]
        assertNodeTypeDefinitionsPropertiesCount(root, 3, 2); // [mix:lockable]
        assertNodeTypeDefinitionsPropertiesCount(root, 4, 5); // [mix:versionable]
        assertNodeTypeDefinitionsPropertiesCount(root, 5, 1); // [nt:versionHistory]
        assertNodeTypeDefinitionsPropertiesCount(root, 6, 1); // [nt:versionLabels]
        assertNodeTypeDefinitionsPropertiesCount(root, 7, 3); // [nt:version]
        assertNodeTypeDefinitionsPropertiesCount(root, 8, 5); // [nt:frozenNode]
        assertNodeTypeDefinitionsPropertiesCount(root, 9, 1); // [nt:versionedChild]
        assertNodeTypeDefinitionsPropertiesCount(root, 10, 5); // [nt:nodeType]
        assertNodeTypeDefinitionsPropertiesCount(root, 11, 9); // [nt:propertyDefinition]
        assertNodeTypeDefinitionsPropertiesCount(root, 12, 8); // [nt:childNodeDefinition]
        assertNodeTypeDefinitionsPropertiesCount(root, 13, 1); // [nt:hierarchyNode]
        assertNodeTypeDefinitionsPropertiesCount(root, 14, 0); // [nt:folder]
        assertNodeTypeDefinitionsPropertiesCount(root, 15, 0); // [nt:file]
        assertNodeTypeDefinitionsPropertiesCount(root, 16, 1); // [nt:linkedFile]
        assertNodeTypeDefinitionsPropertiesCount(root, 17, 4); // [nt:resource]
        assertNodeTypeDefinitionsPropertiesCount(root, 18, 2); // [nt:query]
        assertNodeTypeDefinitionsPropertiesCount(root, 19, 0); // [rep:nodeTypes]
        assertNodeTypeDefinitionsPropertiesCount(root, 20, 0); // [rep:root]
        assertNodeTypeDefinitionsPropertiesCount(root, 21, 0); // [rep:system]
        assertNodeTypeDefinitionsPropertiesCount(root, 22, 0); // [rep:versionStorage]
    }

    @Test
    public void shouldParseImagesCndComments() throws Exception {
        Tree root = parse(IMAGES_CND);
        assertCommentsCount(root, 11);
    }

    @Test
    public void shouldParseImagesCndNamespaces() throws Exception {
        Tree root = parse(IMAGES_CND);
        assertNamespacesCount(root, 4);
    }

    @Test
    public void shouldParseImagesCndNodeTypeDefinitions() throws Exception {
        Tree root = parse(IMAGES_CND);

        // make sure correct number of node type definitions
        assertNodeTypeDefinitionsCount(root, 2);

        // make sure right number of node type definitions for each node type
        assertNodeTypeDefinitionsPropertiesCount(root, 0, 2); // [mix:mimeType]
        assertNodeTypeDefinitionsPropertiesCount(root, 1, 10); // [image:metadata]
    }

    @Test
    public void shouldParseMp3CndComments() throws Exception {
        Tree root = parse(MP3_CND);

        // this will be the COMMENTS imaginary container token
        Tree comments = root.getChild(COMMENTS_INDEX);
        assertThat("Tree type was not CndParser.COMMENTS", comments.getType(), is(CndParser.COMMENTS));

        // make sure right number of comments
        assertThat("Incorrect number of comments", comments.getChildCount(), is(10));
    }

    @Test
    public void shouldParseMp3CndNamespaces() throws Exception {
        Tree root = parse(MP3_CND);
        assertNamespacesCount(root, 4);
    }

    @Test
    public void shouldParseMp3CndNodeTypeDefinitions() throws Exception {
        Tree root = parse(MP3_CND);

        // make sure correct number of node type definitions
        assertNodeTypeDefinitionsCount(root, 2);

        // make sure right number of node type definitions for each node type
        assertNodeTypeDefinitionsPropertiesCount(root, 0, 2); // [mix:mimeType]
        assertNodeTypeDefinitionsPropertiesCount(root, 1, 5); // [mp3:metadata]
    }
}
