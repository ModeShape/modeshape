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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Value;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;
import org.modeshape.sequencer.teiid.lexicon.CoreLexicon;
import org.modeshape.sequencer.teiid.lexicon.VdbLexicon;

/**
 * 
 */
public class VdbSequencerTest extends AbstractSequencerTest {

    @Override
    protected InputStream getRepositoryConfigStream() {
        return resourceStream("config/repo-config.json");
    }

    @Ignore
    // java.util.zip.ZipException: incomplete dynamic bit lengths tree
    @Test
    public void shouldSequencePartsSupplierVDB() throws Exception {
        createNodeWithContentFromFile("model/parts/PartsSupplierVDB.vdb", "model/parts/PartsSupplierVDB.vdb");
        Node outputNode = getOutputNode(this.rootNode, "vdbs/PartsSupplierVDB.vdb", 7);
        assertNotNull(outputNode);
        assertEquals(VdbLexicon.Vdb.VIRTUAL_DATABASE, outputNode.getPrimaryNodeType().getName());
    }

    @Test
    public void shouldSequenceBooksVDB() throws Exception {
        createNodeWithContentFromFile("BooksVDB.vdb", "model/books/BooksVDB.vdb");
        Node outputNode = getOutputNode(this.rootNode, "vdbs/BooksVDB.vdb", 7);
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
            assertThat(modelNode.getProperty(VdbLexicon.Model.PATH_IN_VDB).getString(), is("/TestRESTWarGen/BooksProcedures.xmi"));
            assertThat(modelNode.getProperty(VdbLexicon.Model.VISIBLE).getBoolean(), is(false));
            assertThat(modelNode.getProperty(VdbLexicon.Model.BUILT_IN).getBoolean(), is(false));
            assertThat(modelNode.getProperty(VdbLexicon.Model.CHECKSUM).getLong(), is(1855484649L));
            assertThat(modelNode.getProperty(VdbLexicon.Model.DESCRIPTION).getString(), is("This is a model description"));
            assertThat(modelNode.getProperty(VdbLexicon.Model.TYPE).getString(), is(CoreLexicon.ModelType.VIRTUAL));
            assertThat(modelNode.getProperty("modelClass").getString(), is("Relational"));
            assertThat(modelNode.getProperty("indexName").getString(), is("1159106455.INDEX"));

            modelNode = outputNode.getNode("MyBooks.xmi");
            assertThat(modelNode.getPrimaryNodeType().getName(), is(VdbLexicon.Model.MODEL));
            assertThat(modelNode.getProperty(VdbLexicon.Model.PATH_IN_VDB).getString(), is("/TestRESTWarGen/MyBooks.xmi"));
            assertThat(modelNode.getProperty(VdbLexicon.Model.VISIBLE).getBoolean(), is(true));
            assertThat(modelNode.getProperty(VdbLexicon.Model.TYPE).getString(), is(CoreLexicon.ModelType.PHYSICAL));
            assertThat(modelNode.getProperty(VdbLexicon.Model.BUILT_IN).getBoolean(), is(false));
            assertThat(modelNode.getProperty(VdbLexicon.Model.SOURCE_TRANSLATOR).getString(), is("MyBooks_mysql5"));
            assertThat(modelNode.getProperty(VdbLexicon.Model.SOURCE_JNDI_NAME).getString(), is("MyBooks"));
            assertThat(modelNode.getProperty(VdbLexicon.Model.SOURCE_NAME).getString(), is("MyBooks"));
            assertThat(modelNode.getProperty("modelClass").getString(), is("Relational"));
            assertThat(modelNode.getProperty("indexName").getString(), is("718925066.INDEX"));

            modelNode = outputNode.getNode("MyBooksView.xmi");
            assertThat(modelNode.getPrimaryNodeType().getName(), is(VdbLexicon.Model.MODEL));
            assertThat(modelNode.getProperty(VdbLexicon.Model.TYPE).getString(), is(CoreLexicon.ModelType.VIRTUAL));
            assertThat(modelNode.getProperty(VdbLexicon.Model.PATH_IN_VDB).getString(), is("/TestRESTWarGen/MyBooksView.xmi"));
            assertThat(modelNode.getProperty(VdbLexicon.Model.VISIBLE).getBoolean(), is(true));
            assertThat(modelNode.getProperty(VdbLexicon.Model.BUILT_IN).getBoolean(), is(false));
            assertThat(modelNode.getProperty(VdbLexicon.Model.CHECKSUM).getLong(), is(825941341L));
            assertThat(modelNode.getProperty("modelClass").getString(), is("Relational"));
            assertThat(modelNode.getProperty("indexName").getString(), is("2173178531.INDEX"));

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

    @Ignore
    // java.lang.ArrayIndexOutOfBoundsException: 4096
    @Test
    public void shouldSequenceVdbForQuickEmployees() throws Exception {
        createNodeWithContentFromFile("vdb/qe.vdb", "vdb/qe.vdb");
        Node outputNode = getOutputNode(this.rootNode, "vdbs/qe.vdb", 7);
        assertNotNull(outputNode);
        assertEquals(VdbLexicon.Vdb.VIRTUAL_DATABASE, outputNode.getPrimaryNodeType().getName());
    }

    @Ignore
    // java.lang.ArrayIndexOutOfBoundsException: 4096
    @Test
    public void shouldSequenceVdbForQuickEmployeesWithVersionSpecifiedInFileName() throws Exception {
        createNodeWithContentFromFile("vdb/qe.2.vdb", "vdb/qe.2.vdb");
        Node outputNode = getOutputNode(this.rootNode, "vdbs/qe.2.vdb", 7);
        assertNotNull(outputNode);
        assertEquals(VdbLexicon.Vdb.VIRTUAL_DATABASE, outputNode.getPrimaryNodeType().getName());
    }

    @Ignore
    // java.util.zip.ZipException: invalid block type
    @Test
    public void shouldSequenceVdbForPartsFromXml() throws Exception {
        createNodeWithContentFromFile("vdb/PartsFromXml.vdb", "vdb/PartsFromXml.vdb");
        Node outputNode = getOutputNode(this.rootNode, "vdbs/PartsFromXml.vdb", 7);
        assertNotNull(outputNode);
        assertEquals(VdbLexicon.Vdb.VIRTUAL_DATABASE, outputNode.getPrimaryNodeType().getName());
    }

    @Ignore
    // java.util.zip.ZipException: invalid block type
    @Test
    public void shouldSequenceVdbForYahooUdfTest() throws Exception {
        createNodeWithContentFromFile("vdb/YahooUdfTest.vdb", "vdb/YahooUdfTest.vdb");
        Node outputNode = getOutputNode(this.rootNode, "vdbs/YahooUdfTest.vdb", 7);
        assertNotNull(outputNode);
        assertEquals(VdbLexicon.Vdb.VIRTUAL_DATABASE, outputNode.getPrimaryNodeType().getName());
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

    protected void assertVersionInfo( String fileNameWithoutExtension,
                                      String expectedName,
                                      int expectedVersion ) {
        AtomicInteger actual = new AtomicInteger(1);
        String name = VdbSequencer.extractVersionInfomation(fileNameWithoutExtension, actual);
        assertThat(name, is(expectedName));
        assertThat(actual.get(), is(expectedVersion));
    }
}
