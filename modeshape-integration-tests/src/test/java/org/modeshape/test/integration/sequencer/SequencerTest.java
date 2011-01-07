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
package org.modeshape.test.integration.sequencer;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.File;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import org.junit.After;
import org.junit.Test;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.ModeShapeRoles;
import org.modeshape.jcr.JcrRepository.Option;
import org.modeshape.sequencer.image.ImageMetadataLexicon;
import org.modeshape.test.integration.AbstractModeShapeTest;

public class SequencerTest extends AbstractModeShapeTest {

    @Override
    @After
    public void afterEach() throws Exception {
        super.afterEach();
        configuration = null;

        try {
            if (session != null) {
                session.logout();
            }
        } finally {
            session = null;
            try {
                if (engine != null) {
                    engine.shutdown();
                }
            } finally {
                engine = null;
            }
        }
    }

    @Test
    public void shouldRegisterNodeTypesDefinedInConfigurationFileWithDefaultNamespace() throws Exception {
        configuration = new JcrConfiguration().loadFrom("src/test/resources/config/configRepositoryWithDefaultNamespace.xml");
        engine = configuration.build();
        engine.start();
        repository = engine.getRepository("mode:Car Repository");
        session = repository.login();

        assertNodeType("ddl:tableOperand", true, false, true, false, null, 0, 0, "nt:base", "ddl:operand");
        assertNodeType("derbyddl:functionOperand", true, false, true, false, null, 0, 0, "nt:base", "ddl:operand");
        assertNodeType("text:column", false, true, true, false, null, 0, 1);
        assertNodeType("relational:column",
                       false,
                       false,
                       true,
                       false,
                       null,
                       0,
                       42,
                       "nt:unstructured",
                       "relational:relationalEntity");
        assertNodeType("relational:baseTable", false, false, true, true, null, 0, 0, "relational:table");
        assertNodeTypes("relational:relationalEntity",
                        "relational:column",
                        "relational:columnSet",
                        "relational:uniqueKey",
                        "relational:primaryKey",
                        "relational:foreignKey",
                        "jdbcs:imported");
    }

    @Test
    public void shouldRegisterNodeTypes() throws Exception {
        configuration = new JcrConfiguration();
        configuration.repositorySource("source").usingClass(InMemoryRepositorySource.class).setDescription("The content store");
        configuration.repository("repo")
                     .setSource("source")
                     .addNodeTypes(resourceUrl("org/modeshape/connector/meta/jdbc/connector-metajdbc.cnd"))
                     .addNodeTypes(resourceUrl("org/modeshape/sequencer/classfile/sequencer-classfile.cnd"))
                     .addNodeTypes(resourceUrl("org/modeshape/sequencer/image/images.cnd"))
                     .addNodeTypes(resourceUrl("org/modeshape/sequencer/java/javaSource.cnd"))
                     .addNodeTypes(resourceUrl("org/modeshape/sequencer/mp3/mp3.cnd"))
                     .addNodeTypes(resourceUrl("org/modeshape/sequencer/msoffice/msoffice.cnd"))
                     .addNodeTypes(resourceUrl("org/modeshape/sequencer/text/sequencer-text.cnd"))
                     .addNodeTypes(resourceUrl("org/modeshape/sequencer/xml/xml.cnd"))
                     .addNodeTypes(resourceUrl("org/modeshape/sequencer/zip/zip.cnd"))
                     .addNodeTypes(resourceUrl("org/modeshape/sequencer/ddl/StandardDdl.cnd"))
                     .addNodeTypes(resourceUrl("org/modeshape/sequencer/ddl/dialect/derby/DerbyDdl.cnd"))
                     .addNodeTypes(resourceUrl("org/modeshape/sequencer/ddl/dialect/oracle/OracleDdl.cnd"))
                     .addNodeTypes(resourceUrl("org/modeshape/sequencer/ddl/dialect/postgres/PostgresDdl.cnd"))
                     .addNodeTypes(resourceUrl("org/modeshape/sequencer/teiid/teiid.cnd"))
                     .setOption(Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);
        engine = configuration.build();
        engine.start();
        repository = engine.getRepository("repo");
        session = repository.login();

        // assertNodeType("ddl:tableOperand", true, false, true, false, null, 0, 0, "nt:base", "ddl:operand");
        // assertNodeType("derbyddl:functionOperand", true, false, true, false, null, 0, 0, "nt:base", "ddl:operand");
        // assertNodeType("text:column", false, true, true, false, null, 0, 1);
        assertNodeType("relational:column",
                       false,
                       false,
                       true,
                       false,
                       null,
                       0,
                       42,
                       "nt:unstructured",
                       "relational:relationalEntity");
        assertNodeType("relational:baseTable", false, false, true, true, null, 0, 0, "relational:table");
        assertNodeTypes("relational:relationalEntity",
                        "relational:column",
                        "relational:columnSet",
                        "relational:uniqueKey",
                        "relational:primaryKey",
                        "relational:foreignKey",
                        "jdbcs:imported");
    }

    @Test
    public void shouldSequenceContentInOneSourceAndStoreDerivedContentInAnother() throws Exception {
        String repoId = "content";
        String repoSrcId = "store";
        String workSpace = "images";

        String metaRepoId = "metadata";
        String metaRepoSrcId = "imageexif";
        String metaWorkSpace = "info";

        // Configuration
        configuration = new JcrConfiguration();

        // Image repository source
        configuration.repositorySource(repoSrcId)
                     .usingClass(InMemoryRepositorySource.class)
                     .setDescription("The repository for our content")
                     .setProperty("defaultWorkspaceName", workSpace);

        // Metadata repository source
        configuration.repositorySource(metaRepoSrcId)
                     .usingClass(InMemoryRepositorySource.class)
                     .setDescription("The meta repository for our content")
                     .setProperty("defaultWorkspaceName", metaWorkSpace);

        // Image repository
        configuration.repository(repoId).registerNamespace(ImageMetadataLexicon.Namespace.PREFIX,
                                                           ImageMetadataLexicon.Namespace.URI).setSource(repoSrcId);

        // Metadata repository
        configuration.repository(metaRepoId)
                     .addNodeTypes("src/test/resources/sequencers/cnd/images.cnd")
                     .registerNamespace("example", "http://www.example.com/exif")
                     .setSource(metaRepoSrcId);

        // Sequencer
        configuration.sequencer("Image Sequencer")
                     .usingClass("org.modeshape.sequencer.image.ImageMetadataSequencer")
                     .loadedFromClasspath()
                     .setDescription("Sequences image files to extract the characteristics of the image")
                     .sequencingFrom("store:images://(*.(jpg|jpeg|gif|bmp|pcx|png|iff|ras|pbm|pgm|ppm|psd)[*])/jcr:content[@jcr:data]")
                     .andOutputtingTo("imageexif:info:/$1");

        engine = configuration.build();
        if (!engine.getProblems().isEmpty()) {
            System.out.println(engine.getProblems());
        }
        engine.start();
        repository = engine.getRepository(repoId);
        session = repository.login();

        // Add the "files" node ...
        session.getRootNode().addNode("files", "nt:unstructured");
        session.save();

        // Upload an image ...
        File file = new File("src/test/resources/sequencers/image/caution.gif");
        assertThat(file.exists(), is(true));
        uploadFile(file.toURI().toURL(), "/files/");
        waitUntilSequencedNodesIs(1);
        Thread.sleep(200); // wait a bit while the new content is indexed

        // Now look for the derived content ...
        Repository imageRepo = engine.getRepository(metaRepoId);
        Session imageSession = imageRepo.login();

        Node caution = imageSession.getNode("/caution.gif");
        Node metadata = caution.getNode("image:metadata");
        assertThat(metadata.getProperty("image:height").getLong(), is(48L));

        // print = true;
        printSubgraph(caution);
    }

    // @Test
    // public void shouldCreateRepositoryConfiguredWithOneXmlNodeTypeDefinitionFiles() throws Exception {
    // configuration = new JcrConfiguration();
    // configuration.repositorySource("car-source")
    // .usingClass(InMemoryRepositorySource.class)
    // .setDescription("The automobile content");
    // configuration.repository("cars")
    // .setSource("car-source")
    // .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
    // .addNodeTypes(resourceUrl("xmlNodeTypeRegistration/owfe_nodetypes.xml"))
    // .setOption(Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);
    // engine = configuration.build();
    // engine.start();
    //
    // repository = engine.getRepository("cars");
    // session = repository.login();
    //
    // assertNodeType("mgnl:workItem", false, false, true, true, null, 1, 1, "nt:hierarchyNode");
    // }

    // @Test
    // public void shouldCreateRepositoryConfiguredWithMultipleNodeTypeDefinitionFiles() throws Exception {
    // configuration = new JcrConfiguration();
    // configuration.repositorySource("car-source")
    // .usingClass(InMemoryRepositorySource.class)
    // .setDescription("The automobile content");
    // configuration.repository("cars")
    // .setSource("car-source")
    // .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
    // .addNodeTypes(resourceUrl("cars.cnd"))
    // .addNodeTypes(resourceUrl("xmlNodeTypeRegistration/owfe_nodetypes.xml"))
    // .setOption(Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);
    // engine = configuration.build();
    // engine.start();
    //
    // repository = engine.getRepository("cars");
    // session = repository.login();
    //
    // assertNodeType("car:Car", false, false, true, false, null, 0, 11, "nt:unstructured", "mix:created");
    // assertNodeType("mgnl:workItem", false, false, true, true, null, 1, 1, "nt:hierarchyNode");
    // }

    @Override
    protected void assertNodeType( String name,
                                   boolean isAbstract,
                                   boolean isMixin,
                                   boolean isQueryable,
                                   boolean hasOrderableChildNodes,
                                   String primaryItemName,
                                   int numberOfDeclaredChildNodeDefinitions,
                                   int numberOfDeclaredPropertyDefinitions,
                                   String... supertypes ) throws Exception {
        NodeType nodeType = session.getWorkspace().getNodeTypeManager().getNodeType(name);
        assertThat(nodeType, is(notNullValue()));
        assertThat(nodeType.isAbstract(), is(isAbstract));
        assertThat(nodeType.isMixin(), is(isMixin));
        assertThat(nodeType.isQueryable(), is(isQueryable));
        assertThat(nodeType.hasOrderableChildNodes(), is(hasOrderableChildNodes));
        assertThat(nodeType.getPrimaryItemName(), is(primaryItemName));
        for (int i = 0; i != supertypes.length; ++i) {
            assertThat(nodeType.getDeclaredSupertypes()[i].getName(), is(supertypes[i]));
        }
        assertThat(nodeType.getDeclaredSupertypes().length, is(supertypes.length));
        assertThat(nodeType.getDeclaredChildNodeDefinitions().length, is(numberOfDeclaredChildNodeDefinitions));
        assertThat(nodeType.getDeclaredPropertyDefinitions().length, is(numberOfDeclaredPropertyDefinitions));
    }

    @Override
    protected void assertNodeTypes( String... nodeTypeNames ) throws Exception {
        for (String nodeTypeName : nodeTypeNames) {
            NodeType nodeType = session.getWorkspace().getNodeTypeManager().getNodeType(nodeTypeName);
            assertThat(nodeType, is(notNullValue()));
        }
    }

}
