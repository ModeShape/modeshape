/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
package org.modeshape.sequencer.cnd;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static org.modeshape.sequencer.cnd.CndSequencerLexicon.AUTO_CREATED;
import static org.modeshape.sequencer.cnd.CndSequencerLexicon.CHILD_NODE_DEFINITION;
import static org.modeshape.sequencer.cnd.CndSequencerLexicon.DEFAULT_PRIMARY_TYPE;
import static org.modeshape.sequencer.cnd.CndSequencerLexicon.DEFAULT_VALUES;
import static org.modeshape.sequencer.cnd.CndSequencerLexicon.HAS_ORDERABLE_CHILD_NODES;
import static org.modeshape.sequencer.cnd.CndSequencerLexicon.IS_ABSTRACT;
import static org.modeshape.sequencer.cnd.CndSequencerLexicon.IS_FULL_TEXT_SEARCHABLE;
import static org.modeshape.sequencer.cnd.CndSequencerLexicon.IS_MIXIN;
import static org.modeshape.sequencer.cnd.CndSequencerLexicon.IS_QUERYABLE;
import static org.modeshape.sequencer.cnd.CndSequencerLexicon.IS_QUERY_ORDERABLE;
import static org.modeshape.sequencer.cnd.CndSequencerLexicon.MANDATORY;
import static org.modeshape.sequencer.cnd.CndSequencerLexicon.MULTIPLE;
import static org.modeshape.sequencer.cnd.CndSequencerLexicon.NAME;
import static org.modeshape.sequencer.cnd.CndSequencerLexicon.NODE_TYPE;
import static org.modeshape.sequencer.cnd.CndSequencerLexicon.NODE_TYPE_NAME;
import static org.modeshape.sequencer.cnd.CndSequencerLexicon.ON_PARENT_VERSION;
import static org.modeshape.sequencer.cnd.CndSequencerLexicon.PROPERTY_DEFINITION;
import static org.modeshape.sequencer.cnd.CndSequencerLexicon.PROTECTED;
import static org.modeshape.sequencer.cnd.CndSequencerLexicon.REQUIRED_PRIMARY_TYPES;
import static org.modeshape.sequencer.cnd.CndSequencerLexicon.REQUIRED_TYPE;
import static org.modeshape.sequencer.cnd.CndSequencerLexicon.SAME_NAME_SIBLINGS;
import static org.modeshape.sequencer.cnd.CndSequencerLexicon.SUPERTYPES;
import static org.modeshape.sequencer.cnd.CndSequencerLexicon.VALUE_CONSTRAINTS;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Value;
import javax.jcr.version.OnParentVersionAction;
import org.junit.Test;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;

/**
 * Unit test for {@link CndSequencer}
 * 
 * @author Horia Chiorean
 */
public class CndSequencerTest extends AbstractSequencerTest {

    @Override
    protected InputStream getRepositoryConfigStream() {
        return resourceStream("sequencer/cnd/repo-config.json");
    }

    @Test
    public void sequenceImages() throws Exception {
        Node imagesNode = createNodeWithContentFromFile("images.cnd", "sequencer/cnd/images.cnd");
        verifyImageMetadataNode(imagesNode);
        verifyEmbeddedImageNode(imagesNode);
    }

    private void verifyEmbeddedImageNode( Node imagesNode ) throws Exception {
        Node embeddedImageNode = getOutputNode(imagesNode, "image:embeddedImage");
        assertNotNull(embeddedImageNode);
        new NodeDefinitionVerifier().name("image:embeddedImage")
                                    .nodeTypeName("image:embeddedImage")
                                    .mixin(true)
                                    .verifyNode(embeddedImageNode);
        NodeIterator nodesIterator = embeddedImageNode.getNodes();
        Node propertyDefinition = nodesIterator.nextNode();
        new PropertyDefinitionVerifier().name("image:width").mandatory(false).requiredType("long").verify(propertyDefinition);
        assertFalse(nodesIterator.hasNext());
    }

    private void verifyImageMetadataNode( Node imagesNode ) throws Exception {
        Node imageMetadataNode = getOutputNode(imagesNode, "image:metadata");
        assertNotNull(imageMetadataNode);
        new NodeDefinitionVerifier().name("image:metadata")
                                    .superTypes("nt:unstructured", "mix:mimeType")
                                    .nodeTypeName("image:metadata")
                                    .verifyNode(imageMetadataNode);

        NodeIterator nodesIterator = imageMetadataNode.getNodes();
        Node propertyDefinition = nodesIterator.nextNode();
        new PropertyDefinitionVerifier().name("image:formatName")
                                        .mandatory(true)
                                        .requiredType("string")
                                        .valueConstraints("JPEG",
                                                          "GIF",
                                                          "PNG",
                                                          "BMP",
                                                          "PCX",
                                                          "IFF",
                                                          "RAS",
                                                          "PBM",
                                                          "PGM",
                                                          "PPM",
                                                          "PSD")
                                        .verify(propertyDefinition);

        propertyDefinition = nodesIterator.nextNode();
        new PropertyDefinitionVerifier().name("image:width").mandatory(false).requiredType("long").verify(propertyDefinition);

        propertyDefinition = nodesIterator.nextNode();
        new PropertyDefinitionVerifier().name("image:progressive")
                                        .mandatory(false)
                                        .requiredType("boolean")
                                        .verify(propertyDefinition);

        Node childNodeDefinition = nodesIterator.nextNode();
        new NodeDefinitionVerifier().name("image:subImage")
                                    .sameNameSiblings(true)
                                    .requiredPrimaryTypes("image:embeddedImage")
                                    .defaultPrimaryType("image:embeddedImage")
                                    .verifyChildNode(childNodeDefinition);

        assertFalse(nodesIterator.hasNext());
    }

    @Test
    public void ignoreInvalidFiles() throws Exception {
        final Node invalidNode = createNodeWithContentFromFile("invalid.cnd", "sequencer/cnd/invalid.cnd");
        Thread.sleep(TimeUnit.MILLISECONDS.convert(2, TimeUnit.SECONDS));
        NodeIterator nodeIterator = invalidNode.getNodes();
        assertEquals("/invalid.cnd/jcr:content", nodeIterator.nextNode().getPath());
        assertFalse(nodeIterator.hasNext());
    }

    protected List<String> sortedListToLowerCase( List<String> valuesList ) {
        List<String> result = new ArrayList<String>();
        for (String value : valuesList) {
            result.add(value.toLowerCase());
        }
        Collections.sort(result);
        return result;
    }

    protected List<String> sortedValuesToLowerCase( Value[] values ) throws Exception {
        List<String> result = new ArrayList<String>();
        for (Value value : values) {
            result.add(value.getString().toLowerCase());
        }
        Collections.sort(result);
        return result;
    }

    protected class NodeDefinitionVerifier {
        private String name;
        private boolean mixin = false;
        private boolean isAbstract = false;
        private boolean isQueryable = true;
        private String nodeTypeName;
        private boolean hasOrderableChildNodes = false;
        private List<String> superTypes = Collections.emptyList();

        private boolean autoCreated = false;
        private boolean mandatory = false;
        private String onParentVersion = OnParentVersionAction.ACTIONNAME_COPY.toLowerCase();
        private boolean isProtected = false;
        private String defaultPrimaryType;
        private List<String> requiredPrimaryTypes = Collections.emptyList();
        private boolean sameNameSiblings = false;

        NodeDefinitionVerifier autoCreated( final boolean autoCreated ) {
            this.autoCreated = autoCreated;
            return this;
        }

        NodeDefinitionVerifier name( final String name ) {
            this.name = name.toLowerCase();
            return this;
        }

        NodeDefinitionVerifier mixin( final boolean mixin ) {
            this.mixin = mixin;
            return this;
        }

        NodeDefinitionVerifier isAbstract( final boolean isAbstract ) {
            this.isAbstract = isAbstract;
            return this;
        }

        NodeDefinitionVerifier isQueryable( final boolean isQueryable ) {
            this.isQueryable = isQueryable;
            return this;
        }

        NodeDefinitionVerifier nodeTypeName( final String nodeTypeName ) {
            this.nodeTypeName = nodeTypeName.toLowerCase();
            return this;
        }

        NodeDefinitionVerifier hasOrderableChildNodes( final boolean hasOrderableChildNodes ) {
            this.hasOrderableChildNodes = hasOrderableChildNodes;
            return this;
        }

        NodeDefinitionVerifier superTypes( final String... superTypes ) {
            this.superTypes = sortedListToLowerCase(Arrays.asList(superTypes));
            return this;
        }

        NodeDefinitionVerifier mandatory( final boolean mandatory ) {
            this.mandatory = mandatory;
            return this;
        }

        NodeDefinitionVerifier onParentVersion( final String onParentVersion ) {
            this.onParentVersion = onParentVersion;
            return this;
        }

        NodeDefinitionVerifier isProtected( final boolean isProtected ) {
            this.isProtected = isProtected;
            return this;
        }

        NodeDefinitionVerifier requiredPrimaryTypes( final String... requiredPrimaryTypes ) {
            this.requiredPrimaryTypes = sortedListToLowerCase(Arrays.asList(requiredPrimaryTypes));
            return this;
        }

        NodeDefinitionVerifier sameNameSiblings( boolean sameNameSiblings ) {
            this.sameNameSiblings = sameNameSiblings;
            return this;
        }

        NodeDefinitionVerifier defaultPrimaryType( final String defaultPrimaryType ) {
            this.defaultPrimaryType = defaultPrimaryType.toLowerCase();
            return this;
        }

        void verifyNode( Node nodeDefinition ) throws Exception {
            assertEquals(NODE_TYPE.toLowerCase(), nodeDefinition.getProperty(JcrConstants.JCR_PRIMARY_TYPE)
                                                                .getString()
                                                                .toLowerCase());
            assertEquals(mixin, nodeDefinition.getProperty(IS_MIXIN).getBoolean());
            assertEquals(isAbstract, nodeDefinition.getProperty(IS_ABSTRACT).getBoolean());
            assertEquals(isQueryable, nodeDefinition.getProperty(IS_QUERYABLE).getBoolean());
            assertNotNull(nodeTypeName);
            assertEquals(nodeTypeName, nodeDefinition.getProperty(NODE_TYPE_NAME).getString().toLowerCase());
            assertEquals(hasOrderableChildNodes, nodeDefinition.getProperty(HAS_ORDERABLE_CHILD_NODES).getBoolean());
            assertEquals(superTypes, sortedValuesToLowerCase(nodeDefinition.getProperty(SUPERTYPES).getValues()));
        }

        void verifyChildNode( Node childNodeDefinition ) throws Exception {
            assertEquals(CHILD_NODE_DEFINITION.toLowerCase(), childNodeDefinition.getProperty(JcrConstants.JCR_PRIMARY_TYPE)
                                                                                 .getString()
                                                                                 .toLowerCase());
            assertNotNull(name);
            assertEquals(name, childNodeDefinition.getProperty(NAME).getString().toLowerCase());
            assertEquals(autoCreated, childNodeDefinition.getProperty(AUTO_CREATED).getBoolean());
            assertEquals(mandatory, childNodeDefinition.getProperty(MANDATORY).getBoolean());
            assertNotNull(onParentVersion);
            assertEquals(onParentVersion, childNodeDefinition.getProperty(ON_PARENT_VERSION).getString().toLowerCase());
            assertEquals(isProtected, childNodeDefinition.getProperty(PROTECTED).getBoolean());
            assertNotNull(defaultPrimaryType);
            assertEquals(defaultPrimaryType, childNodeDefinition.getProperty(DEFAULT_PRIMARY_TYPE).getString().toLowerCase());
            assertEquals(requiredPrimaryTypes, sortedValuesToLowerCase(childNodeDefinition.getProperty(REQUIRED_PRIMARY_TYPES)
                                                                                          .getValues()));
            assertEquals(sameNameSiblings, childNodeDefinition.getProperty(SAME_NAME_SIBLINGS).getBoolean());
            assertEquals(sameNameSiblings, childNodeDefinition.getProperty(SAME_NAME_SIBLINGS).getBoolean());
        }
    }

    private class PropertyDefinitionVerifier {
        private String name;
        private boolean autoCreated = false;
        private boolean mandatory = false;
        private String onParentVersion = OnParentVersionAction.ACTIONNAME_COPY.toLowerCase();
        private boolean isProtected = false;
        private String requiredType;
        private List<String> valueConstraints = Collections.emptyList();
        private List<String> defaultValues = Collections.emptyList();
        private boolean multiple = false;
        private boolean fullTextSearchable = true;
        private boolean queryOrderable = true;

        protected PropertyDefinitionVerifier() {
        }

        @SuppressWarnings( "unused" )
        PropertyDefinitionVerifier autoCreated( boolean autoCreated ) {
            this.autoCreated = autoCreated;
            return this;
        }

        @SuppressWarnings( "unused" )
        PropertyDefinitionVerifier defaultValues( String... defaultValues ) {
            this.defaultValues = sortedListToLowerCase(Arrays.asList(defaultValues));
            return this;
        }

        @SuppressWarnings( "unused" )
        PropertyDefinitionVerifier fullTextSearchable( boolean fullTextSearchable ) {
            this.fullTextSearchable = fullTextSearchable;
            return this;
        }

        @SuppressWarnings( "unused" )
        PropertyDefinitionVerifier isProtected( boolean isProtected ) {
            this.isProtected = isProtected;
            return this;

        }

        PropertyDefinitionVerifier mandatory( boolean mandatory ) {
            this.mandatory = mandatory;
            return this;
        }

        @SuppressWarnings( "unused" )
        PropertyDefinitionVerifier multiple( boolean multiple ) {
            this.multiple = multiple;
            return this;
        }

        PropertyDefinitionVerifier name( String name ) {
            this.name = name.toLowerCase();
            return this;
        }

        @SuppressWarnings( "unused" )
        PropertyDefinitionVerifier onParentVersion( String onParentVersion ) {
            this.onParentVersion = onParentVersion.toLowerCase();
            return this;
        }

        @SuppressWarnings( "unused" )
        PropertyDefinitionVerifier queryOrderable( boolean queryOrderable ) {
            this.queryOrderable = queryOrderable;
            return this;
        }

        PropertyDefinitionVerifier requiredType( String requiredType ) {
            this.requiredType = requiredType;
            return this;
        }

        PropertyDefinitionVerifier valueConstraints( String... valueConstraints ) {
            this.valueConstraints = sortedListToLowerCase(Arrays.asList(valueConstraints));
            return this;
        }

        void verify( Node propertyDefinition ) throws Exception {
            assertEquals(PROPERTY_DEFINITION.toLowerCase(), propertyDefinition.getProperty(JcrConstants.JCR_PRIMARY_TYPE)
                                                                              .getString()
                                                                              .toLowerCase());

            assertNotNull(name);
            assertEquals(name, propertyDefinition.getProperty(NAME).getString().toLowerCase());
            assertEquals(autoCreated, propertyDefinition.getProperty(AUTO_CREATED).getBoolean());
            assertEquals(mandatory, propertyDefinition.getProperty(MANDATORY).getBoolean());
            assertEquals(multiple, propertyDefinition.getProperty(MULTIPLE).getBoolean());
            assertEquals(fullTextSearchable, propertyDefinition.getProperty(IS_FULL_TEXT_SEARCHABLE).getBoolean());
            assertEquals(queryOrderable, propertyDefinition.getProperty(IS_QUERY_ORDERABLE).getBoolean());
            assertEquals(isProtected, propertyDefinition.getProperty(PROTECTED).getBoolean());
            assertNotNull(onParentVersion);
            assertEquals(onParentVersion, propertyDefinition.getProperty(ON_PARENT_VERSION).getString().toLowerCase());
            assertNotNull(requiredType);
            assertEquals(requiredType, propertyDefinition.getProperty(REQUIRED_TYPE).getString().toLowerCase());
            assertEquals(valueConstraints, sortedValuesToLowerCase(propertyDefinition.getProperty(VALUE_CONSTRAINTS).getValues()));
            assertEquals(defaultValues, sortedValuesToLowerCase(propertyDefinition.getProperty(DEFAULT_VALUES).getValues()));
        }
    }
}
