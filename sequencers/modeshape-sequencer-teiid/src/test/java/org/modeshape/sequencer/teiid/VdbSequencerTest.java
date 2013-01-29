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
package org.modeshape.sequencer.teiid;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Value;
import org.junit.Test;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;
import org.modeshape.sequencer.teiid.lexicon.CoreLexicon;
import org.modeshape.sequencer.teiid.lexicon.RelationalLexicon;
import org.modeshape.sequencer.teiid.lexicon.TransformLexicon;
import org.modeshape.sequencer.teiid.lexicon.VdbLexicon;

/**
 * 
 */
public class VdbSequencerTest extends AbstractSequencerTest {

    @Override
    protected InputStream getRepositoryConfigStream() {
        return resourceStream("config/repo-config.json");
    }

    @Test
    public void shouldHaveValidCnds() throws Exception {
        registerNodeTypes("org/modeshape/sequencer/teiid/xmi.cnd");
        registerNodeTypes("org/modeshape/sequencer/teiid/jdbc.cnd");
        registerNodeTypes("org/modeshape/sequencer/teiid/mmcore.cnd");
        registerNodeTypes("org/modeshape/sequencer/teiid/relational.cnd");
        registerNodeTypes("org/modeshape/sequencer/teiid/transformation.cnd");
        registerNodeTypes("org/modeshape/sequencer/teiid/vdb.cnd");
    }

    @Test
    public void shouldSequenceBooksVDB() throws Exception {
        createNodeWithContentFromFile("BooksVDB.vdb", "model/books/BooksVDB.vdb");
        Node outputNode = getOutputNode(this.rootNode, "vdbs/BooksVDB.vdb");
        assertNotNull(outputNode);
        assertThat(outputNode.getPrimaryNodeType().getName(), is(VdbLexicon.Vdb.VIRTUAL_DATABASE));

        // check properties
        assertThat(outputNode.getProperty(VdbLexicon.Vdb.DESCRIPTION).getString(), is("This is a VDB description"));
        assertThat(outputNode.getProperty(VdbLexicon.Vdb.VERSION).getLong(), is(2L));
        assertThat(outputNode.getProperty(VdbLexicon.Vdb.PREVIEW).getBoolean(), is(true));
        assertThat(outputNode.getProperty("query-timeout").getLong(), is(10000L));

        { // child node models
            Node modelNode = outputNode.getNode("BooksProcedures.xmi");
            assertThat(modelNode.getPrimaryNodeType().getName(), is(VdbLexicon.Model.MODEL));
            assertThat(modelNode.getProperty(VdbLexicon.Model.PATH_IN_VDB).getString(), is("TestRESTWarGen/BooksProcedures.xmi"));
            assertThat(modelNode.getProperty(VdbLexicon.Model.VISIBLE).getBoolean(), is(false));
            assertThat(modelNode.getProperty(VdbLexicon.Model.BUILT_IN).getBoolean(), is(false));
            assertThat(modelNode.getProperty(VdbLexicon.Model.CHECKSUM).getLong(), is(1855484649L));
            assertThat(modelNode.getProperty(VdbLexicon.Model.DESCRIPTION).getString(), is("This is a model description"));
            assertThat(modelNode.getProperty(CoreLexicon.JcrId.MODEL_TYPE).getString(), is(CoreLexicon.ModelType.VIRTUAL));
            assertThat(modelNode.getProperty("modelClass").getString(), is("Relational"));
            assertThat(modelNode.getProperty("indexName").getString(), is("1159106455.INDEX"));

            modelNode = outputNode.getNode("MyBooks.xmi");
            assertThat(modelNode.getPrimaryNodeType().getName(), is(VdbLexicon.Model.MODEL));
            assertThat(modelNode.getProperty(VdbLexicon.Model.PATH_IN_VDB).getString(), is("TestRESTWarGen/MyBooks.xmi"));
            assertThat(modelNode.getProperty(VdbLexicon.Model.VISIBLE).getBoolean(), is(true));
            assertThat(modelNode.getProperty(CoreLexicon.JcrId.MODEL_TYPE).getString(), is(CoreLexicon.ModelType.PHYSICAL));
            assertThat(modelNode.getProperty(VdbLexicon.Model.BUILT_IN).getBoolean(), is(false));
            assertThat(modelNode.getProperty(VdbLexicon.Model.SOURCE_TRANSLATOR).getString(), is("MyBooks_mysql5"));
            assertThat(modelNode.getProperty(VdbLexicon.Model.SOURCE_JNDI_NAME).getString(), is("MyBooks"));
            assertThat(modelNode.getProperty(VdbLexicon.Model.SOURCE_NAME).getString(), is("MyBooks"));
            assertThat(modelNode.getProperty("modelClass").getString(), is("Relational"));
            assertThat(modelNode.getProperty("indexName").getString(), is("718925066.INDEX"));

            modelNode = outputNode.getNode("MyBooksView.xmi");
            assertThat(modelNode.getPrimaryNodeType().getName(), is(VdbLexicon.Model.MODEL));
            assertThat(modelNode.getProperty(CoreLexicon.JcrId.MODEL_TYPE).getString(), is(CoreLexicon.ModelType.VIRTUAL));
            assertThat(modelNode.getProperty(VdbLexicon.Model.PATH_IN_VDB).getString(), is("TestRESTWarGen/MyBooksView.xmi"));
            assertThat(modelNode.getProperty(VdbLexicon.Model.VISIBLE).getBoolean(), is(true));
            assertThat(modelNode.getProperty(VdbLexicon.Model.BUILT_IN).getBoolean(), is(false));
            assertThat(modelNode.getProperty(VdbLexicon.Model.CHECKSUM).getLong(), is(825941341L));
            assertThat(modelNode.getProperty("modelClass").getString(), is("Relational"));
            assertThat(modelNode.getProperty("indexName").getString(), is("2173178531.INDEX"));

            // transformation
            Node booksTable = modelNode.getNode("BOOKS");
            assertThat(booksTable.isNodeType(TransformLexicon.JcrId.TRANSFORMED), is(true));
            assertThat(booksTable.hasProperty(TransformLexicon.JcrId.TRANSFORMED_FROM_NAMES), is(true));
            assertThat(booksTable.getProperty(TransformLexicon.JcrId.TRANSFORMED_FROM_NAMES).getValues().length, is(1));
            assertThat(booksTable.getProperty(TransformLexicon.JcrId.TRANSFORMED_FROM_NAMES).getValues()[0].getString(),
                       is("BOOKS"));

            { // markers
                Node markersGroupNode = modelNode.getNode(VdbLexicon.Model.MARKERS);
                assertNotNull(markersGroupNode);
                NodeIterator itr = markersGroupNode.getNodes();
                assertThat(itr.getSize(), is(3L));

                String message1 = "The name BOOKS is the same (ignoring case) as 1 other object(s) under the same parent";
                String message2 = "Group does not exist: MyBooksView.BOOKS";
                String message3 = "The name BOOKS is the same (ignoring case) as 1 other object(s) under the same parent";
                int messagesFound = 0;

                while (itr.hasNext()) {
                    Node node = itr.nextNode();
                    assertThat(node.getPrimaryNodeType().getName(), is(VdbLexicon.Model.Marker.MARKER));
                    assertThat(node.getProperty(VdbLexicon.Model.Marker.PATH).getString(), is("BOOKS"));
                    assertThat(node.getProperty(VdbLexicon.Model.Marker.SEVERITY).getString(), is("ERROR"));

                    if (node.getProperty(VdbLexicon.Model.Marker.MESSAGE).getString().equals(message1)) {
                        ++messagesFound;
                        message1 = "message1 found";
                    } else if (node.getProperty(VdbLexicon.Model.Marker.MESSAGE).getString().equals(message2)) {
                        ++messagesFound;
                        message2 = "message2 found";
                    } else if (node.getProperty(VdbLexicon.Model.Marker.MESSAGE).getString().equals(message3)) {
                        ++messagesFound;
                        message3 = "message3 found";
                    }
                }

                assertThat(messagesFound, is(3));
            }
        }

        { // check child node entries
            boolean entry1Found = false;
            boolean entry2Found = false;
            Node entriesGroupNode = outputNode.getNode(VdbLexicon.Vdb.ENTRIES);
            assertNotNull(entriesGroupNode);
            assertThat(entriesGroupNode.getPrimaryNodeType().getName(), is(VdbLexicon.Vdb.ENTRIES));

            NodeIterator itr = entriesGroupNode.getNodes();
            assertThat(itr.getSize(), is(2L));

            while (itr.hasNext()) {
                Node entryNode = itr.nextNode();
                assertThat(entryNode.getPrimaryNodeType().getName(), is(VdbLexicon.Entry.ENTRY));

                if (!entry1Found && "path1".equals(entryNode.getProperty(VdbLexicon.Entry.PATH).getString())) {
                    entry1Found = true;
                    assertThat(entryNode.getProperty(VdbLexicon.Entry.DESCRIPTION).getString(), is("This is entry 1 description"));
                    assertThat(entryNode.getProperty("drummer").getString(), is("Ringo"));
                    assertThat(entryNode.getProperty("guitar").getString(), is("John"));
                } else if (!entry2Found && "path2".equals(entryNode.getProperty(VdbLexicon.Entry.PATH).getString())) {
                    entry2Found = true;
                    assertThat(entryNode.getProperty(VdbLexicon.Entry.PATH).getString(), is("path2"));
                    assertThat(entryNode.getProperty(VdbLexicon.Entry.DESCRIPTION).getString(), is("This is entry 2 description"));
                    assertThat(entryNode.getProperty("bass").getString(), is("Paul"));
                    assertThat(entryNode.getProperty("leadGuitar").getString(), is("George"));
                } else {
                    fail();
                }
            }

            assertThat((entry1Found && entry2Found), is(true));
        }

        { // check child node translators
            Node translatorsGroupNode = outputNode.getNode(VdbLexicon.Vdb.TRANSLATORS);
            assertNotNull(translatorsGroupNode);
            assertThat(translatorsGroupNode.getPrimaryNodeType().getName(), is(VdbLexicon.Vdb.TRANSLATORS));
            assertThat(translatorsGroupNode.getNodes().getSize(), is(1L));

            // check translator
            Node translatorNode = translatorsGroupNode.getNode("MyBooks_mysql5");
            assertNotNull(translatorNode);
            assertThat(translatorNode.getPrimaryNodeType().getName(), is(VdbLexicon.Translator.TRANSLATOR));
            assertThat(translatorNode.getProperty(VdbLexicon.Translator.TYPE).getString(), is("mysql5"));
            assertThat(translatorNode.getProperty(VdbLexicon.Translator.DESCRIPTION).getString(),
                       is("This is a translator description"));

            { // check translator Teiid properties
                assertThat(translatorNode.getProperty("nameInSource").getString(), is("bogusName"));
                assertThat(translatorNode.getProperty("supportsUpdate").getBoolean(), is(true));
            }
        }

        { // check child node data roles
            Node dataRolesGroupNode = outputNode.getNode(VdbLexicon.Vdb.DATA_ROLES);
            assertNotNull(dataRolesGroupNode);
            assertThat(dataRolesGroupNode.getPrimaryNodeType().getName(), is(VdbLexicon.Vdb.DATA_ROLES));
            assertThat(dataRolesGroupNode.getNodes().getSize(), is(1L));
            Node dataRoleNode = dataRolesGroupNode.getNode("My Data Role");
            assertNotNull(dataRoleNode);
            assertThat(dataRoleNode.getPrimaryNodeType().getName(), is(VdbLexicon.DataRole.DATA_ROLE));
            assertThat(dataRoleNode.getProperty(VdbLexicon.DataRole.DESCRIPTION).getString(), is("my data role description"));
            assertThat(dataRoleNode.getProperty(VdbLexicon.DataRole.ALLOW_CREATE_TEMP_TABLES).getBoolean(), is(true));
            assertThat(dataRoleNode.getProperty(VdbLexicon.DataRole.ANY_AUTHENTICATED).getBoolean(), is(true));

            { // mapped role names
                Value[] roleNames = dataRoleNode.getProperty(VdbLexicon.DataRole.MAPPED_ROLE_NAMES).getValues();
                assertThat(roleNames.length, is(2));
                assertThat("Sledge".equals(roleNames[0].getString()) || "Sledge".equals(roleNames[1].getString()), is(true));
                assertThat("Hammer".equals(roleNames[0].getString()) || "Hammer".equals(roleNames[1].getString()), is(true));
            }

            { // permissions
                Node permissionsGroupNode = dataRoleNode.getNode(VdbLexicon.DataRole.PERMISSIONS);
                assertNotNull(permissionsGroupNode);
                assertThat(permissionsGroupNode.getPrimaryNodeType().getName(), is(VdbLexicon.DataRole.PERMISSIONS));
                assertThat(permissionsGroupNode.getNodes().getSize(), is(3L));

                Node permNode = permissionsGroupNode.getNode("BooksProcedures");
                assertNotNull(permNode);
                assertThat(permNode.getPrimaryNodeType().getName(), is(VdbLexicon.DataRole.Permission.PERMISSION));
                assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_CREATE).getBoolean(), is(false));
                assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_READ).getBoolean(), is(true));
                assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_UPDATE).getBoolean(), is(true));
                assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_DELETE).getBoolean(), is(true));
                assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_EXECUTE).getBoolean(), is(false));
                assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_ALTER).getBoolean(), is(false));

                permNode = permissionsGroupNode.getNode("sysadmin");
                assertNotNull(permNode);
                assertThat(permNode.getPrimaryNodeType().getName(), is(VdbLexicon.DataRole.Permission.PERMISSION));
                assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_CREATE).getBoolean(), is(false));
                assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_READ).getBoolean(), is(true));
                assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_UPDATE).getBoolean(), is(false));
                assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_DELETE).getBoolean(), is(false));
                assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_EXECUTE).getBoolean(), is(false));
                assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_ALTER).getBoolean(), is(false));

                permNode = permissionsGroupNode.getNode("MyBooks");
                assertNotNull(permNode);
                assertThat(permNode.getPrimaryNodeType().getName(), is(VdbLexicon.DataRole.Permission.PERMISSION));
                assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_CREATE).getBoolean(), is(false));
                assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_READ).getBoolean(), is(true));
                assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_UPDATE).getBoolean(), is(true));
                assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_DELETE).getBoolean(), is(true));
                assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_EXECUTE).getBoolean(), is(false));
                assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_ALTER).getBoolean(), is(false));
            }
        }
    }

    @Test
    public void shouldExtractVersionInformation() {
        assertVersionInfo("something", "something", 1);
        assertVersionInfo("something.else", "something.else", 1);
        assertVersionInfo("something else", "something else", 1);
        assertVersionInfo("something.", "something", 1);
        assertVersionInfo("something.1", "something", 1);
        assertVersionInfo("something.12", "something", 12);
        assertVersionInfo("something.123", "something", 123);
        assertVersionInfo("something.4", "something", 4);
        assertVersionInfo("something.-4", "something", 4);
        assertVersionInfo("something.+4", "something", 4);
        assertVersionInfo("something. 4", "something", 4);
        assertVersionInfo("something.  4", "something", 4);
        assertVersionInfo("something.  -4", "something", 4);
        assertVersionInfo("something.  -1234  ", "something", 1234);
    }

    @Test
    public void shouldSequenceVdbTopPartsVDB() throws Exception {
        createNodeWithContentFromFile("vdb/TopPartsVDB.vdb", "vdb/TopPartsVDB.vdb");
        Node outputNode = getOutputNode(this.rootNode, "vdbs/TopPartsVDB.vdb");
        assertNotNull(outputNode);
        assertThat(outputNode.getPrimaryNodeType().getName(), is(VdbLexicon.Vdb.VIRTUAL_DATABASE));

        // check import VDB
        Node importVdbsGroupNode = outputNode.getNode(VdbLexicon.Vdb.IMPORT_VDBS);
        assertNotNull(importVdbsGroupNode);
        assertThat(importVdbsGroupNode.getPrimaryNodeType().getName(), is(VdbLexicon.Vdb.IMPORT_VDBS));
        assertThat(importVdbsGroupNode.getNodes().getSize(), is(1L));

        Node importVdbNode = importVdbsGroupNode.getNode("PartsVDB");
        assertNotNull(importVdbNode);
        assertThat(importVdbNode.getPrimaryNodeType().getName(), is(VdbLexicon.ImportVdb.IMPORT_VDB));
        assertThat(importVdbNode.getProperty(VdbLexicon.ImportVdb.VERSION).getLong(), is(1L));
        assertThat(importVdbNode.getProperty(VdbLexicon.ImportVdb.IMPORT_DATA_POLICIES).getBoolean(), is(false));
    }

    @Test
    public void shouldSequenceVdbBooksVdb() throws Exception {
        createNodeWithContentFromFile("vdb/BooksVdb.vdb", "vdb/BooksVdb.vdb");
        Node outputNode = getOutputNode(this.rootNode, "vdbs/BooksVdb.vdb");
        assertNotNull(outputNode);
        assertThat(outputNode.getPrimaryNodeType().getName(), is(VdbLexicon.Vdb.VIRTUAL_DATABASE));

        // check properties
        assertThat(outputNode.getProperty(VdbLexicon.Vdb.DESCRIPTION).getString(), is("This is a VDB description"));
        assertThat(outputNode.getProperty(VdbLexicon.Vdb.VERSION).getLong(), is(1L));
        assertThat(outputNode.getProperty(VdbLexicon.Vdb.PREVIEW).getBoolean(), is(false));
        assertThat(outputNode.getProperty("query-timeout").getLong(), is(1000000L));

        { // Books_Oracle model child node
            Node modelNode = outputNode.getNode("Books_Oracle.xmi");
            assertThat(modelNode.getPrimaryNodeType().getName(), is(VdbLexicon.Model.MODEL));
            assertThat(modelNode.getProperty(VdbLexicon.Model.PATH_IN_VDB).getString(), is("BooksProject/Books_Oracle.xmi"));
            assertThat(modelNode.getProperty(VdbLexicon.Model.VISIBLE).getBoolean(), is(true));
            assertThat(modelNode.getProperty(VdbLexicon.Model.BUILT_IN).getBoolean(), is(false));
            assertThat(modelNode.getProperty(VdbLexicon.Model.CHECKSUM).getLong(), is(4164741764L));
            assertThat(modelNode.getProperty(CoreLexicon.JcrId.MODEL_TYPE).getString(), is(CoreLexicon.ModelType.PHYSICAL));
            assertThat(modelNode.getProperty("modelClass").getString(), is("Relational"));
            assertThat(modelNode.getProperty("indexName").getString(), is("1388555674.INDEX"));
            assertThat(modelNode.getProperty(VdbLexicon.Model.SOURCE_TRANSLATOR).getString(), is("oracle"));
            assertThat(modelNode.getProperty(VdbLexicon.Model.SOURCE_JNDI_NAME).getString(), is("Books_Oracle"));
            assertThat(modelNode.getProperty(VdbLexicon.Model.SOURCE_NAME).getString(), is("Books_Oracle"));
        }

        { // BooksView model child node
            Node modelNode = outputNode.getNode("BooksView.xmi");
            assertThat(modelNode.getPrimaryNodeType().getName(), is(VdbLexicon.Model.MODEL));
            assertThat(modelNode.getProperty(VdbLexicon.Model.PATH_IN_VDB).getString(), is("BooksProject/BooksView.xmi"));
            assertThat(modelNode.getProperty(VdbLexicon.Model.VISIBLE).getBoolean(), is(true));
            assertThat(modelNode.getProperty(VdbLexicon.Model.BUILT_IN).getBoolean(), is(false));
            assertThat(modelNode.getProperty(VdbLexicon.Model.CHECKSUM).getLong(), is(2513252863L));
            assertThat(modelNode.getProperty(CoreLexicon.JcrId.MODEL_TYPE).getString(), is(CoreLexicon.ModelType.VIRTUAL));
            assertThat(modelNode.getProperty("modelClass").getString(), is("Relational"));
            assertThat(modelNode.getProperty("indexName").getString(), is("3982965936.INDEX"));

            Node modelImport = modelNode.getNode("Books_Oracle");
            assertNotNull(modelImport);
            assertThat(modelImport.getPrimaryNodeType().getName(), is(CoreLexicon.JcrId.IMPORT));
            assertThat(modelImport.getProperty(CoreLexicon.JcrId.MODEL_TYPE).getString(), is(CoreLexicon.ModelType.PHYSICAL));
            assertThat(modelImport.getProperty(CoreLexicon.JcrId.PRIMARY_METAMODEL_URI).getString(), is(RelationalLexicon.Namespace.URI));
            assertThat(modelImport.getProperty(CoreLexicon.JcrId.MODEL_LOCATION).getString(), is("Books_Oracle.xmi"));
            assertThat(modelImport.getProperty(CoreLexicon.JcrId.PATH).getString(), is("/BooksProject/Books_Oracle.xmi"));
        }

        { // data roles child nodes
            Node dataRolesGroupNode = outputNode.getNode(VdbLexicon.Vdb.DATA_ROLES);
            assertNotNull(dataRolesGroupNode);
            assertThat(dataRolesGroupNode.getPrimaryNodeType().getName(), is(VdbLexicon.Vdb.DATA_ROLES));
            assertThat(dataRolesGroupNode.getNodes().getSize(), is(2L));

            {
                Node dataRoleNode = dataRolesGroupNode.getNode("Another Data role");
                assertNotNull(dataRoleNode);
                assertThat(dataRoleNode.getPrimaryNodeType().getName(), is(VdbLexicon.DataRole.DATA_ROLE));
                assertThat(dataRoleNode.getProperty(VdbLexicon.DataRole.ALLOW_CREATE_TEMP_TABLES).getBoolean(), is(false));
                assertThat(dataRoleNode.getProperty(VdbLexicon.DataRole.ANY_AUTHENTICATED).getBoolean(), is(false));

                { // mapped role names
                    Value[] roleNames = dataRoleNode.getProperty(VdbLexicon.DataRole.MAPPED_ROLE_NAMES).getValues();
                    assertThat(roleNames.length, is(2));
                    assertThat("Role name 1".equals(roleNames[0].getString()) || "Role name 1".equals(roleNames[1].getString()),
                               is(true));
                    assertThat("Another role name".equals(roleNames[0].getString())
                               || "Another role name".equals(roleNames[1].getString()),
                               is(true));
                }

                { // permissions
                    Node permissionsGroupNode = dataRoleNode.getNode(VdbLexicon.DataRole.PERMISSIONS);
                    assertNotNull(permissionsGroupNode);
                    assertThat(permissionsGroupNode.getPrimaryNodeType().getName(), is(VdbLexicon.DataRole.PERMISSIONS));
                    assertThat(permissionsGroupNode.getNodes().getSize(), is(1L));

                    Node permNode = permissionsGroupNode.getNode("Books_Oracle");
                    assertNotNull(permNode);
                    assertThat(permNode.getPrimaryNodeType().getName(), is(VdbLexicon.DataRole.Permission.PERMISSION));
                    assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_CREATE).getBoolean(), is(false));
                    assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_READ).getBoolean(), is(true));
                    assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_UPDATE).getBoolean(), is(false));
                    assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_DELETE).getBoolean(), is(false));
                    assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_EXECUTE).getBoolean(), is(false));
                    assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_ALTER).getBoolean(), is(false));
                }
            }

            {
                Node dataRoleNode = dataRolesGroupNode.getNode("MyDataRole");
                assertNotNull(dataRoleNode);
                assertThat(dataRoleNode.getPrimaryNodeType().getName(), is(VdbLexicon.DataRole.DATA_ROLE));
                assertThat(dataRoleNode.getProperty(VdbLexicon.DataRole.DESCRIPTION).getString(), is("This is a data role description"));
                assertThat(dataRoleNode.getProperty(VdbLexicon.DataRole.ALLOW_CREATE_TEMP_TABLES).getBoolean(), is(false));
                assertThat(dataRoleNode.getProperty(VdbLexicon.DataRole.ANY_AUTHENTICATED).getBoolean(), is(false));

                { // permissions
                    Node permissionsGroupNode = dataRoleNode.getNode(VdbLexicon.DataRole.PERMISSIONS);
                    assertNotNull(permissionsGroupNode);
                    assertThat(permissionsGroupNode.getPrimaryNodeType().getName(), is(VdbLexicon.DataRole.PERMISSIONS));
                    assertThat(permissionsGroupNode.getNodes().getSize(), is(2L));

                    Node permNode = permissionsGroupNode.getNode("Books_Oracle");
                    assertNotNull(permNode);
                    assertThat(permNode.getPrimaryNodeType().getName(), is(VdbLexicon.DataRole.Permission.PERMISSION));
                    assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_CREATE).getBoolean(), is(false));
                    assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_READ).getBoolean(), is(true));
                    assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_UPDATE).getBoolean(), is(true));
                    assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_DELETE).getBoolean(), is(true));
                    assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_EXECUTE).getBoolean(), is(true));
                    assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_ALTER).getBoolean(), is(true));

                    permNode = permissionsGroupNode.getNode("sysadmin");
                    assertNotNull(permNode);
                    assertThat(permNode.getPrimaryNodeType().getName(), is(VdbLexicon.DataRole.Permission.PERMISSION));
                    assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_CREATE).getBoolean(), is(false));
                    assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_READ).getBoolean(), is(true));
                    assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_UPDATE).getBoolean(), is(false));
                    assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_DELETE).getBoolean(), is(false));
                    assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_EXECUTE).getBoolean(), is(false));
                    assertThat(permNode.getProperty(VdbLexicon.DataRole.Permission.ALLOW_ALTER).getBoolean(), is(false));
                }
            }
        }
    }

    @Test
    public void shouldSequenceVdbBqtVdb() throws Exception {
        thisLongRunningTestCanBeSkipped();

        createNodeWithContentFromFile("vdb/BqtVdb.vdb", "vdb/BqtVdb.vdb");
        Node outputNode = getOutputNode(this.rootNode, "vdbs/BqtVdb.vdb", 60);
        assertNotNull(outputNode);
        assertThat(outputNode.getPrimaryNodeType().getName(), is(VdbLexicon.Vdb.VIRTUAL_DATABASE));
        assertThat(outputNode.getNodes("*.xmi").getSize(), is(60L));

        // entry child nodes
        Node entriesGroupNode = outputNode.getNode(VdbLexicon.Vdb.ENTRIES);
        assertNotNull(entriesGroupNode);
        assertThat(entriesGroupNode.getPrimaryNodeType().getName(), is(VdbLexicon.Vdb.ENTRIES));

        NodeIterator itr = entriesGroupNode.getNodes();
        assertThat(itr.getSize(), is(2L));
    }

    @Test
    public void shouldSequenceVdbFirstPartsVdb() throws Exception {
        createNodeWithContentFromFile("vdb/FirstParts.vdb", "vdb/FirstParts.vdb");
        Node outputNode = getOutputNode(this.rootNode, "vdbs/FirstParts.vdb");
        assertNotNull(outputNode);
        assertThat(outputNode.getPrimaryNodeType().getName(), is(VdbLexicon.Vdb.VIRTUAL_DATABASE));
    }

    @Test
    public void shouldSequenceVdbPartsVdb() throws Exception {
        createNodeWithContentFromFile("vdb/PartsVdb.vdb", "vdb/PartsVdb.vdb");
        Node outputNode = getOutputNode(this.rootNode, "vdbs/PartsVdb.vdb");
        assertNotNull(outputNode);
        assertThat(outputNode.getPrimaryNodeType().getName(), is(VdbLexicon.Vdb.VIRTUAL_DATABASE));
    }

    @Test
    public void shouldSequenceVdbPortfolioViewVdb() throws Exception {
        // Portfolio.vdb is a 7.4 VDB
        createNodeWithContentFromFile("vdb/PortfolioView.vdb", "vdb/PortfolioView.vdb");
        Node outputNode = getOutputNode(this.rootNode, "vdbs/PortfolioView.vdb");
        assertNotNull(outputNode);
        assertThat(outputNode.getPrimaryNodeType().getName(), is(VdbLexicon.Vdb.VIRTUAL_DATABASE));
    }

    @Test
    public void shouldSequenceVdbPortfolioVdb() throws Exception {
        // Portfolio.vdb is a 7.4 VDB
        createNodeWithContentFromFile("vdb/Portfolio.vdb", "vdb/Portfolio.vdb");
        Node outputNode = getOutputNode(this.rootNode, "vdbs/Portfolio.vdb");
        assertNotNull(outputNode);
        assertThat(outputNode.getPrimaryNodeType().getName(), is(VdbLexicon.Vdb.VIRTUAL_DATABASE));
        assertThat(outputNode.getNodes().getSize(), is(4L));
    }

    @Test
    public void shouldSequenceVdbGatewayVDBVdb() throws Exception {
        // GatewayVDB.vdb is a 7.7 VDB
        createNodeWithContentFromFile("vdb/GatewayVDB.vdb", "vdb/GatewayVDB.vdb");
        Node outputNode = getOutputNode(this.rootNode, "vdbs/GatewayVDB.vdb");
        assertNotNull(outputNode);
        assertThat(outputNode.getPrimaryNodeType().getName(), is(VdbLexicon.Vdb.VIRTUAL_DATABASE));
        assertThat(outputNode.getNodes().getSize(), is(1L));
        
        // verify model and table nodes
        Node modelNode = outputNode.getNode("HSQLDB.xmi");
        assertNotNull(modelNode);
        Node tableNode = modelNode.getNode("GATEWAY_TABLE");
        assertNotNull(tableNode);
    }

    @Test
    public void shouldSequenceVdbBooksOTestVdb() throws Exception {
        // BooksOTest.vdb is a 7.7 VDB
        createNodeWithContentFromFile("vdb/BooksOTest.vdb", "vdb/BooksOTest.vdb");
        Node outputNode = getOutputNode(this.rootNode, "vdbs/BooksOTest.vdb");
        assertNotNull(outputNode);
        assertThat(outputNode.getPrimaryNodeType().getName(), is(VdbLexicon.Vdb.VIRTUAL_DATABASE));
        assertThat(outputNode.getNodes().getSize(), is(1L));
    }

    @Test
    public void shouldSequenceVdbFinancialsLinuxVdb() throws Exception {
        createNodeWithContentFromFile("vdb/Financials_Linux.vdb", "vdb/Financials_Linux.vdb");
        Node outputNode = getOutputNode(this.rootNode, "vdbs/Financials_Linux.vdb");
        assertNotNull(outputNode);
        assertThat(outputNode.getPrimaryNodeType().getName(), is(VdbLexicon.Vdb.VIRTUAL_DATABASE));
        assertThat(outputNode.getNodes().getSize(), is(5L));

        Node modelNode = outputNode.getNode("US_CustomerAccounts_VBL.xmi");
        assertNotNull(modelNode);
        assertThat(modelNode.getPrimaryNodeType().getName(), is(VdbLexicon.Vdb.MODEL));
        assertThat(modelNode.getProperty(VdbLexicon.Model.PATH_IN_VDB).getString(), is("Financials/VirtualBaseLayer/US_CustomerAccounts_VBL.xmi"));
    }

    @Test
    public void shouldSequenceVdbTwitterVdb() throws Exception {
        createNodeWithContentFromFile("vdb/twitter.vdb", "vdb/twitter.vdb");
        Node outputNode = getOutputNode(this.rootNode, "vdbs/twitter.vdb");
        assertNotNull(outputNode);
        assertThat(outputNode.getPrimaryNodeType().getName(), is(VdbLexicon.Vdb.VIRTUAL_DATABASE));
        assertThat(outputNode.getNodes().getSize(), is(3L)); // 2 models and 1 translator

        { // declarative source model
            final Node declarativeModelNode = outputNode.getNode("twitter");
            assertNotNull(declarativeModelNode);
            assertThat(declarativeModelNode.getPrimaryNodeType().getName(), is(VdbLexicon.Vdb.DECLARATIVE_MODEL));
            assertThat(declarativeModelNode.getProperty(VdbLexicon.Model.VISIBLE).getBoolean(), is(true));
            assertThat(declarativeModelNode.getProperty(VdbLexicon.Model.SOURCE_TRANSLATOR).getString(), is("rest"));
            assertThat(declarativeModelNode.getProperty(VdbLexicon.Model.SOURCE_JNDI_NAME).getString(), is("java:/twitterDS"));
            assertThat(declarativeModelNode.getProperty(VdbLexicon.Model.SOURCE_NAME).getString(), is("twitter"));
            assertThat(declarativeModelNode.getProperty(CoreLexicon.JcrId.MODEL_TYPE).getString(), is(CoreLexicon.ModelType.PHYSICAL));
        }

        { // declarative virtual model
            final Node declarativeModelNode = outputNode.getNode("twitterview");
            assertNotNull(declarativeModelNode);
            assertThat(declarativeModelNode.getPrimaryNodeType().getName(), is(VdbLexicon.Vdb.DECLARATIVE_MODEL));
            assertThat(declarativeModelNode.getProperty(VdbLexicon.Model.VISIBLE).getBoolean(), is(true));
            assertThat(declarativeModelNode.getProperty(VdbLexicon.Model.METADATA_TYPE).getString(), is(VdbModel.DEFAULT_METADATA_TYPE));
            assertThat(declarativeModelNode.getProperty(CoreLexicon.JcrId.MODEL_TYPE).getString(), is(CoreLexicon.ModelType.VIRTUAL));

            final String metadata = "CREATE VIRTUAL PROCEDURE getTweets(query varchar) RETURNS (created_on varchar(25),"
                                    + " from_user varchar(25), to_user varchar(25),"
                                    + " profile_image_url varchar(25), source varchar(25), text varchar(140)) AS"
                                    + " select tweet.* from"
                                    + " (call twitter.invokeHTTP(action => 'GET', endpoint =>querystring('',query as \"q\"))) w,"
                                    + " XMLTABLE('results' passing JSONTOXML('myxml', w.result) columns"
                                    + " created_on string PATH 'created_at'," + " from_user string PATH 'from_user',"
                                    + " to_user string PATH 'to_user'," + " profile_image_url string PATH 'profile_image_url',"
                                    + " source string PATH 'source'," + " text string PATH 'text') tweet;"
                                    + " CREATE VIEW Tweet AS select * FROM twitterview.getTweets;";
            assertThat(declarativeModelNode.getProperty(VdbLexicon.Model.MODEL_DEFINITION).getString(), is(metadata));
        }
    }

    protected void assertVersionInfo( String fileNameWithoutExtension,
                                      String expectedName,
                                      int expectedVersion ) {
        AtomicInteger actual = new AtomicInteger(1);
        String name = VdbSequencer.extractVersionInfomation(fileNameWithoutExtension, actual);
        assertThat(name, is(expectedName));
        assertThat(actual.get(), is(expectedVersion));
    }
}
