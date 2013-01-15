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

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.value.Path;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Tests of round-trip importing/exporting of repository content.
 */
public class ImportExportTest extends SingleUseAbstractTest {

    private enum ExportType {
        SYSTEM,
        DOCUMENT
    }

    private static final String BAD_CHARACTER_STRING = "Test & <Test>*";

    private String expectedIdentifier ="e41075cb-a09a-4910-87b1-90ce8b4ca9dd";

    private void testImportExport( String sourcePath,
                                   String targetPath,
                                   ExportType useSystemView,
                                   boolean skipBinary,
                                   boolean noRecurse,
                                   boolean useWorkspace ) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if (useSystemView == ExportType.SYSTEM) {
            session.exportSystemView(sourcePath, baos, skipBinary, noRecurse);
        } else {
            session.exportDocumentView(sourcePath, baos, skipBinary, noRecurse);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        if (useWorkspace) {
            // import via workspace ...
            session.getWorkspace().importXML(targetPath, bais, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        } else {
            // import via session ...
            session.importXML(targetPath, bais, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        }
    }

    @Test
    public void shouldImportExportEscapedXmlCharactersInSystemViewUsingSession() throws Exception {
        String testName = "importExportEscapedXmlCharacters";
        Node rootNode = session.getRootNode();
        Node sourceNode = rootNode.addNode(testName + "Source", "nt:unstructured");
        Node targetNode = rootNode.addNode(testName + "Target", "nt:unstructured");

        // Test data
        sourceNode.setProperty("badcharacters", BAD_CHARACTER_STRING);
        assertThat(sourceNode.getProperty("badcharacters").getString(), is(BAD_CHARACTER_STRING));
        sourceNode.addNode(BAD_CHARACTER_STRING);

        testImportExport(sourceNode.getPath(), targetNode.getPath(), ExportType.SYSTEM, false, false, false);
        Node newSourceNode = targetNode.getNode(testName + "Source");
        newSourceNode.getNode(BAD_CHARACTER_STRING);
        assertThat(newSourceNode.getProperty("badcharacters").getString(), is(BAD_CHARACTER_STRING));
    }

    @Test
    public void shouldImportExportEscapedXmlCharactersInSystemViewUsingWorkspace() throws Exception {
        String testName = "importExportEscapedXmlCharacters";
        Node rootNode = session.getRootNode();
        Node sourceNode = rootNode.addNode(testName + "Source", "nt:unstructured");
        Node targetNode = rootNode.addNode(testName + "Target", "nt:unstructured");

        // Test data
        sourceNode.setProperty("badcharacters", BAD_CHARACTER_STRING);
        assertThat(sourceNode.getProperty("badcharacters").getString(), is(BAD_CHARACTER_STRING));
        sourceNode.addNode(BAD_CHARACTER_STRING);
        session.save();

        testImportExport(sourceNode.getPath(), targetNode.getPath(), ExportType.SYSTEM, false, false, true);
        Node newSourceNode = targetNode.getNode(testName + "Source");
        newSourceNode.getNode(BAD_CHARACTER_STRING);
        assertThat(newSourceNode.getProperty("badcharacters").getString(), is(BAD_CHARACTER_STRING));
    }

    @FixFor( "MODE-1405" )
    @Test
    public void shouldImportProtectedContentUsingWorkpace() throws Exception {
        String testName = "importExportEscapedXmlCharacters";
        Node rootNode = session.getRootNode();
        Node sourceNode = rootNode.addNode(testName + "Source", "nt:unstructured");
        Node targetNode = rootNode.addNode(testName + "Target", "nt:unstructured");

        // Test data
        Node child = sourceNode.addNode("child");
        child.addMixin("mix:created");
        session.save();

        // Verify there are 'jcr:createdBy' and 'jcr:created' properties ...
        assertThat(child.getProperty("jcr:createdBy").getString(), is(notNullValue()));
        assertThat(child.getProperty("jcr:created").getString(), is(notNullValue()));

        testImportExport(sourceNode.getPath(), targetNode.getPath(), ExportType.SYSTEM, false, false, true);
        Node newSourceNode = targetNode.getNode(testName + "Source");
        Node newChild = newSourceNode.getNode("child");
        // Verify there are 'jcr:createdBy' and 'jcr:created' properties ...
        assertThat(newChild.getProperty("jcr:createdBy").getString(), is(notNullValue()));
        assertThat(newChild.getProperty("jcr:created").getString(), is(notNullValue()));

    }

    @Ignore( "JR TCK is broken" )
    @Test
    public void shouldImportExportEscapedXmlCharactersInDocumentViewUsingSession() throws Exception {
        String testName = "importExportEscapedXmlCharacters";
        Node rootNode = session.getRootNode();
        Node sourceNode = rootNode.addNode(testName + "Source", "nt:unstructured");
        Node targetNode = rootNode.addNode(testName + "Target", "nt:unstructured");

        // Test data
        sourceNode.setProperty("badcharacters", BAD_CHARACTER_STRING);
        assertThat(sourceNode.getProperty("badcharacters").getString(), is(BAD_CHARACTER_STRING));
        sourceNode.addNode(BAD_CHARACTER_STRING);

        testImportExport(sourceNode.getPath(), targetNode.getPath(), ExportType.DOCUMENT, false, false, false);
        Node newSourceNode = targetNode.getNode(testName + "Source");
        newSourceNode.getNode(BAD_CHARACTER_STRING);
        assertThat(newSourceNode.getProperty("badcharacters").getString(), is(BAD_CHARACTER_STRING));
    }

    @Test
    public void shouldImportSystemViewWithUuidsAfterNodesWithSameUuidsAreDeletedInSessionAndSaved() throws Exception {
        // Register the Cars node types ...
        tools.registerNodeTypes(session, "cars.cnd");

        // Create the node under which the content will be imported ...
        session.getRootNode().addNode("/someNode");
        session.save();

        // Import the car content ...
        importFile("/someNode", "io/cars-system-view-with-uuids.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        session.save();
        // tools.printSubgraph(assertNode("/"));

        // Now delete the '/someNode/Cars' node (which is everything that was imported) ...
        Node cars = assertNode("/someNode/Cars");
        assertThat(cars.getIdentifier(), is(expectedIdentifier));
        assertNoNode("/someNode/Cars[2]");
        assertNoNode("/someNode[2]");
        cars.remove();
        session.save();

        // Now import again ...
        importFile("/someNode", "io/cars-system-view-with-uuids.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        session.save();

        // Verify the same Cars node exists ...
        cars = assertNode("/someNode/Cars");
        assertThat(cars.getIdentifier(), is(expectedIdentifier));
        assertNoNode("/someNode/Cars[2]");
        assertNoNode("/someNode[2]");
    }

    @Test
    public void shouldImportSystemViewWithUuidsAfterNodesWithSameUuidsAreDeletedInSessionButNotSaved() throws Exception {
        // Register the Cars node types ...
        tools.registerNodeTypes(session, "cars.cnd");

        // Create the node under which the content will be imported ...
        session.getRootNode().addNode("/someNode");
        session.save();

        // Import the car content ...
        importFile("/someNode", "io/cars-system-view-with-uuids.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        session.save();

        // Now delete the '/someNode/Cars' node (which is everything that was imported) ...
        Node cars = assertNode("/someNode/Cars");

        assertThat(cars.getIdentifier(), is(expectedIdentifier));
        assertNoNode("/someNode/Cars[2]");
        assertNoNode("/someNode[2]");
        cars.remove();
        assertNoNode("/someNode/Cars");
        // session.save(); // DO NOT SAVE

        // Now import again ...
        importFile("/someNode", "io/cars-system-view-with-uuids.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        session.save();

        // Verify the same Cars node exists ...
        cars = assertNode("/someNode/Cars");
        assertThat(cars.getIdentifier(), is(expectedIdentifier));
        assertNoNode("/someNode/Cars[2]");
        assertNoNode("/someNode[2]");
    }

    @Test
    public void shouldImportSystemViewWithUuidsIntoDifferentSpotAfterNodesWithSameUuidsAreDeletedInSessionButNotSaved()
        throws Exception {
        // Register the Cars node types ...
        tools.registerNodeTypes(session, "cars.cnd");

        // Create the node under which the content will be imported ...
        Node someNode = session.getRootNode().addNode("/someNode");
        session.getRootNode().addNode("/otherNode");
        session.save();

        // Import the car content ...
        importFile("/someNode", "io/cars-system-view-with-uuids.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        session.save();

        // Now delete the '/someNode/Cars' node (which is everything that was imported) ...
        Node cars = assertNode("/someNode/Cars");

        assertThat(cars.getIdentifier(), is(expectedIdentifier));
        assertNoNode("/someNode/Cars[2]");
        assertNoNode("/someNode[2]");
        cars.remove();

        // Now create a node at the same spot as cars, but with a different UUID ...
        Node newCars = someNode.addNode("Cars");
        assertThat(newCars.getIdentifier(), is(not(expectedIdentifier)));

        // session.save(); // DO NOT SAVE

        // Now import again ...
        importFile("/otherNode", "io/cars-system-view-with-uuids.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        session.save();

        // Verify the same Cars node exists ...
        cars = assertNode("/otherNode/Cars");
        assertThat(cars.getIdentifier(), is(expectedIdentifier));

        // Make sure some duplicate nodes didn't show up ...
        assertNoNode("/sameNode/Cars[2]");
        assertNoNode("/sameNode[2]");
        assertNoNode("/otherNode/Cars[2]");
        assertNoNode("/otherNode[2]");
    }

    @FixFor( "MODE-1137" )
    @Test
    public void shouldExportContentWithUnicodeCharactersAsDocumentView() throws Exception {
        Node unicode = session.getRootNode().addNode("unicodeContent");
        Node desc = unicode.addNode("descriptionNode");
        desc.setProperty("ex1", "étudiant (student)");
        desc.setProperty("ex2", "où (where)");
        desc.setProperty("ex3", "forêt (forest)");
        desc.setProperty("ex4", "naïve (naïve)");
        desc.setProperty("ex5", "garçon (boy)");
        desc.setProperty("ex6", "multi\nline\nvalue");
        desc.setProperty("ex7", "prop \"value\" with quotes");
        desc.setProperty("ex7", "values with \r various \t\n : characters");
        session.save();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        session.exportDocumentView("/unicodeContent", baos, false, false);
        baos.close();

        predefineWorkspace("workspace2");

        InputStream istream = new ByteArrayInputStream(baos.toByteArray());
        Session session2 = repository.login("workspace2");
        session2.getWorkspace().importXML("/", istream, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

        Node desc2 = session2.getNode("/unicodeContent/descriptionNode");
        assertSameProperties(desc, desc2);
    }

    @FixFor( "MODE-1137" )
    @Test
    public void shouldExportContentWithUnicodeCharactersAsSystemView() throws Exception {
        Node unicode = session.getRootNode().addNode("unicodeContent");
        Node desc = unicode.addNode("descriptionNode");
        desc.setProperty("ex1", "étudiant (student)");
        desc.setProperty("ex2", "où (where)");
        desc.setProperty("ex3", "forêt (forest)");
        desc.setProperty("ex4", "naïve (naïve)");
        desc.setProperty("ex5", "garçon (boy)");
        desc.setProperty("ex6", "multi\nline\nvalue");
        desc.setProperty("ex7", "prop \"value\" with quotes");
        desc.setProperty("ex7", "values with \n various \t\n : characters");
        session.save();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        session.exportSystemView("/unicodeContent", baos, false, false);
        baos.close();

        predefineWorkspace("workspace2");

        InputStream istream = new ByteArrayInputStream(baos.toByteArray());
        Session session2 = repository.login("workspace2");
        session2.getWorkspace().importXML("/", istream, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

        Node desc2 = session2.getNode("/unicodeContent/descriptionNode");
        assertSameProperties(desc, desc2);
    }

    @Test
    public void shouldImportCarsSystemViewWithCreateNewBehaviorWhenImportedContentDoesNotContainJcrRoot() throws Exception {
        // Register the Cars node types ...
        tools.registerNodeTypes(session, "cars.cnd");

        assertImport("io/cars-system-view.xml", "/a/b", ImportBehavior.CREATE_NEW);
        assertThat(session, is(notNullValue()));
        assertNode("/a/b/Cars");
        assertNode("/a/b/Cars/Hybrid");
        assertNode("/a/b/Cars/Hybrid/Toyota Prius");
        assertNode("/a/b/Cars/Sports/Infiniti G37");
        assertNode("/a/b/Cars/Utility/Land Rover LR3");
        assertNoNode("/a/b/Cars[2]");
        assertNoNode("/a/b/Cars/Hybrid[2]");
        assertNoNode("/a/b/Cars/Hybrid/Toyota Prius[2]");
        assertNoNode("/a/b/Cars/Sports[2]");
    }

    @Test
    public void shouldImportCarsSystemViewWithRemoveExistingBehaviorWhenImportedContentDoesNotContainJcrRoot() throws Exception {
        // Register the Cars node types ...
        tools.registerNodeTypes(session, "cars.cnd");

        assertImport("io/cars-system-view.xml", "/a/b", ImportBehavior.REMOVE_EXISTING);
        assertThat(session, is(notNullValue()));
        assertNode("/a/b/Cars");
        assertNode("/a/b/Cars/Hybrid");
        assertNode("/a/b/Cars/Hybrid/Toyota Prius");
        assertNode("/a/b/Cars/Sports/Infiniti G37");
        assertNode("/a/b/Cars/Utility/Land Rover LR3");
        assertNoNode("/a/b/Cars[2]");
        assertNoNode("/a/b/Cars/Hybrid[2]");
        assertNoNode("/a/b/Cars/Hybrid/Toyota Prius[2]");
        assertNoNode("/a/b/Cars/Sports[2]");
    }

    @Test
    public void shouldImportCarsSystemViewWithReplaceExistingBehaviorWhenImportedContentDoesNotContainJcrRoot() throws Exception {
        // Register the Cars node types ...
        tools.registerNodeTypes(session, "cars.cnd");

        assertImport("io/cars-system-view.xml", "/a/b", ImportBehavior.REPLACE_EXISTING);
        assertThat(session, is(notNullValue()));
        assertNode("/a/b/Cars");
        assertNode("/a/b/Cars/Hybrid");
        assertNode("/a/b/Cars/Hybrid/Toyota Prius");
        assertNode("/a/b/Cars/Sports/Infiniti G37");
        assertNode("/a/b/Cars/Utility/Land Rover LR3");
        assertNoNode("/a/b/Cars[2]");
        assertNoNode("/a/b/Cars/Hybrid[2]");
        assertNoNode("/a/b/Cars/Hybrid/Toyota Prius[2]");
        assertNoNode("/a/b/Cars/Sports[2]");
    }

    @Test
    public void shouldImportCarsSystemViewWithRemoveExistingBehaviorWhenImportedContentDoesNotContainJcrRootOrAnyUuids()
        throws Exception {
        // Register the Cars node types ...
        tools.registerNodeTypes(session, "cars.cnd");

        // Set up the repository with existing content ...
        assertImport("io/cars-system-view.xml", "/a/b", ImportBehavior.CREATE_NEW);
        assertNode("/a/b/Cars");
        assertNode("/a/b/Cars/Hybrid");
        assertNode("/a/b/Cars/Hybrid/Toyota Prius");
        assertNode("/a/b/Cars/Sports/Infiniti G37");
        assertNode("/a/b/Cars/Utility/Land Rover LR3");
        assertNoNode("/a/b/Cars[2]");
        assertNoNode("/a/b/Cars/Hybrid[2]");
        assertNoNode("/a/b/Cars/Hybrid/Toyota Prius[2]");
        assertNoNode("/a/b/Cars/Sports[2]");

        // Now import again to create a second copy ...
        assertImport("io/cars-system-view.xml", "/a/b", ImportBehavior.REMOVE_EXISTING);
        assertThat(session, is(notNullValue()));
        assertNode("/a/b/Cars");
        assertNode("/a/b/Cars/Hybrid");
        assertNode("/a/b/Cars/Hybrid/Toyota Prius");
        assertNode("/a/b/Cars/Sports/Infiniti G37");
        assertNode("/a/b/Cars/Utility/Land Rover LR3");
        assertNode("/a/b/Cars[2]");
        assertNode("/a/b/Cars[2]/Hybrid");
        assertNode("/a/b/Cars[2]/Hybrid/Toyota Prius");
        assertNode("/a/b/Cars[2]/Sports/Infiniti G37");
        assertNode("/a/b/Cars[2]/Utility/Land Rover LR3");
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Import System View with NO 'jcr:root' node but WITH uuids
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldImportCarsSystemViewWithCreateNewBehaviorWhenImportedContentDoesNotContainJcrRootButDoesContainUnusedUuids()
        throws Exception {
        // Register the Cars node types ...
        tools.registerNodeTypes(session, "cars.cnd");

        // Set up the repository with existing content ...
        assertImport("io/cars-system-view.xml", "/a/b", ImportBehavior.CREATE_NEW);
        assertNode("/a/b/Cars");
        assertNode("/a/b/Cars/Hybrid");
        assertNode("/a/b/Cars/Hybrid/Toyota Prius");
        assertNode("/a/b/Cars/Sports/Infiniti G37");
        assertNode("/a/b/Cars/Utility/Land Rover LR3");
        assertNoNode("/a/b/Cars[2]");
        assertNoNode("/a/b/Cars/Hybrid[2]");
        assertNoNode("/a/b/Cars/Hybrid/Toyota Prius[2]");
        assertNoNode("/a/b/Cars/Sports[2]");

        // And attempt to reimport the same content (with UUIDs) into the repository that already has that content ...
        // print = true;
        assertImport("io/cars-system-view-with-uuids.xml", "/a/b", ImportBehavior.REPLACE_EXISTING);
        print();

        // Verify that the original content has been left untouched ...
        assertThat(session, is(notNullValue()));
        assertNode("/a/b/Cars");
        assertNode("/a/b/Cars/Hybrid");
        assertNode("/a/b/Cars/Hybrid/Toyota Prius");
        assertNode("/a/b/Cars/Sports/Infiniti G37");
        assertNode("/a/b/Cars/Utility/Land Rover LR3");
        assertNode("/a/b/Cars[2]");
        assertNode("/a/b/Cars[2]/Hybrid");
        assertNode("/a/b/Cars[2]/Hybrid/Toyota Prius");
        assertNode("/a/b/Cars[2]/Sports");
    }

    @Test
    public void shouldImportCarsSystemViewWithCreateNewBehaviorWhenImportedContentDoesNotContainJcrRootButDoesContainAlreadyUsedUuids()
        throws Exception {
        // Register the Cars node types ...
        tools.registerNodeTypes(session, "cars.cnd");

        // Set up the repository with existing content ...
        assertImport("io/cars-system-view-with-uuids.xml", "/a/b", ImportBehavior.CREATE_NEW);
        assertNode("/a/b/Cars");
        assertNode("/a/b/Cars/Hybrid");
        assertNode("/a/b/Cars/Hybrid/Toyota Prius");
        assertNode("/a/b/Cars/Sports/Infiniti G37");
        assertNode("/a/b/Cars/Utility/Land Rover LR3");
        assertNoNode("/a/b/Cars[2]");
        assertNoNode("/a/b/Cars/Hybrid[2]");
        assertNoNode("/a/b/Cars/Hybrid/Toyota Prius[2]");
        assertNoNode("/a/b/Cars/Sports[2]");

        // And attempt to reimport the same content (with UUIDs) into the repository that already has that content ...
        // print = true;
        assertImport("io/cars-system-view-with-uuids.xml", "/a/b", ImportBehavior.CREATE_NEW);
        print();

        // Verify that the original content has been left untouched ...
        assertThat(session, is(notNullValue()));
        assertNode("/a/b/Cars");
        assertNode("/a/b/Cars/Hybrid");
        assertNode("/a/b/Cars/Hybrid/Toyota Prius");
        assertNode("/a/b/Cars/Sports/Infiniti G37");
        assertNode("/a/b/Cars/Utility/Land Rover LR3");
        assertNode("/a/b/Cars[2]");
        assertNode("/a/b/Cars[2]/Hybrid");
        assertNode("/a/b/Cars[2]/Hybrid/Toyota Prius");
        assertNode("/a/b/Cars[2]/Sports");
    }

    @Test
    public void shouldImportCarsSystemViewOverExistingContentWhenImportedContentDoesNotContainJcrRootButDoesContainAlreadyUsedUuids()
        throws Exception {
        // Register the Cars node types ...
        tools.registerNodeTypes(session, "cars.cnd");

        // Set up the repository with existing content ...
        assertImport("io/cars-system-view-with-uuids.xml", "/a/b", ImportBehavior.CREATE_NEW);
        assertNode("/a/b/Cars");
        assertNode("/a/b/Cars/Hybrid");
        assertNode("/a/b/Cars/Hybrid/Toyota Prius");
        assertNode("/a/b/Cars/Sports/Infiniti G37");
        assertNode("/a/b/Cars/Utility/Land Rover LR3");
        assertNoNode("/a/b/Cars[2]");
        assertNoNode("/a/b/Cars/Hybrid[2]");
        assertNoNode("/a/b/Cars/Hybrid/Toyota Prius[2]");
        assertNoNode("/a/b/Cars/Sports[2]");

        // And attempt to reimport the same content (with UUIDs) into the repository that already has that content ...
        // print = true;
        assertImport("io/cars-system-view-with-uuids.xml", "/a/b", ImportBehavior.REMOVE_EXISTING);
        print();

        // Verify that the original content has been replaced (since the SystemView contained UUIDs) and there is no copy ...
        assertThat(session, is(notNullValue()));
        assertNode("/a/b/Cars");
        assertNode("/a/b/Cars/Hybrid");
        assertNode("/a/b/Cars/Hybrid/Toyota Prius");
        assertNode("/a/b/Cars/Sports/Infiniti G37");
        assertNode("/a/b/Cars/Utility/Land Rover LR3");
        assertNoNode("/a/b/Cars[2]");
        assertNoNode("/a/b/Cars/Hybrid[2]");
        assertNoNode("/a/b/Cars/Hybrid/Toyota Prius[2]");
        assertNoNode("/a/b/Cars/Sports[2]");

        // And attempt to reimport the same content (with UUIDs) into the repository that already has that content ...
        // print = true;
        assertImport("io/cars-system-view-with-uuids.xml", "/a/b", ImportBehavior.REPLACE_EXISTING);
        print();

        // Verify that the original content has been replaced (since the SystemView contained UUIDs) and there is no copy ...
        assertThat(session, is(notNullValue()));
        assertNode("/a/b/Cars");
        assertNode("/a/b/Cars/Hybrid");
        assertNode("/a/b/Cars/Hybrid/Toyota Prius");
        assertNode("/a/b/Cars/Sports/Infiniti G37");
        assertNode("/a/b/Cars/Utility/Land Rover LR3");
        assertNoNode("/a/b/Cars[2]");
        assertNoNode("/a/b/Cars/Hybrid[2]");
        assertNoNode("/a/b/Cars/Hybrid/Toyota Prius[2]");
        assertNoNode("/a/b/Cars/Sports[2]");
    }

    @Test
    public void shouldImportCarsSystemViewWhenImportedContentDoesNotContainJcrRootButDoesContainAlreadyUsedUuids()
        throws Exception {
        // Register the Cars node types ...
        tools.registerNodeTypes(session, "cars.cnd");

        // Set up the repository with existing content ...
        assertImport("io/cars-system-view-with-uuids.xml", "/a/b", ImportBehavior.CREATE_NEW);
        assertNode("/a/b/Cars");
        assertNode("/a/b/Cars/Hybrid");
        assertNode("/a/b/Cars/Hybrid/Toyota Prius");
        assertNode("/a/b/Cars/Sports/Infiniti G37");
        assertNode("/a/b/Cars/Utility/Land Rover LR3");
        assertNoNode("/a/b/Cars[2]");
        assertNoNode("/a/b/Cars/Hybrid[2]");
        assertNoNode("/a/b/Cars/Hybrid/Toyota Prius[2]");
        assertNoNode("/a/b/Cars/Sports[2]");

        // And attempt to reimport the same content (with UUIDs) into the repository that already has that content ...
        // print = true;
        assertImport("io/cars-system-view-with-uuids.xml", "/a/c", ImportBehavior.REPLACE_EXISTING);
        print();

        // Verify that the original content has been replaced (since the SystemView contained UUIDs) and there is no copy ...
        assertThat(session, is(notNullValue()));
        assertNode("/a/b");
        assertNode("/a/c");
        assertNode("/a/b/Cars");
        assertNode("/a/b/Cars/Hybrid");
        assertNode("/a/b/Cars/Hybrid/Toyota Prius");
        assertNode("/a/b/Cars/Sports/Infiniti G37");
        assertNode("/a/b/Cars/Utility/Land Rover LR3");
        assertNoNode("/a/b/Cars[2]");
        assertNoNode("/a/b/Cars/Hybrid[2]");
        assertNoNode("/a/b/Cars/Hybrid/Toyota Prius[2]");
        assertNoNode("/a/b/Cars/Sports[2]");

        // And attempt to reimport the same content (with UUIDs) into the repository that already has that content ...
        // print = true;
        assertImport("io/cars-system-view-with-uuids.xml", "/a/d", ImportBehavior.REMOVE_EXISTING);
        print();

        // Verify that the original content has been replaced (since the SystemView contained UUIDs) and there is no copy ...
        assertThat(session, is(notNullValue()));
        assertNode("/a/b");
        assertNode("/a/c");
        assertNode("/a/d/Cars");
        assertNode("/a/d/Cars/Hybrid");
        assertNode("/a/d/Cars/Hybrid/Toyota Prius");
        assertNode("/a/d/Cars/Sports/Infiniti G37");
        assertNode("/a/d/Cars/Utility/Land Rover LR3");
        assertNoNode("/a/b/Cars[2]");
        assertNoNode("/a/b/Cars/Hybrid[2]");
        assertNoNode("/a/b/Cars/Hybrid/Toyota Prius[2]");
        assertNoNode("/a/b/Cars/Sports[2]");
    }

    @Test( expected = ItemExistsException.class )
    public void shouldFailToImportCarsSystemViewWithThrowBehaviorWhenImportedContentDoesNotContainJcrRootButDoesContainAlreadyUsedUuids()
        throws Exception {
        // Register the Cars node types ...
        tools.registerNodeTypes(session, "cars.cnd");

        // Set up the repository with existing content ...
        assertImport("io/cars-system-view-with-uuids.xml", "/a/b", ImportBehavior.CREATE_NEW);
        assertNode("/a/b/Cars");
        assertNode("/a/b/Cars/Hybrid");
        assertNode("/a/b/Cars/Hybrid/Toyota Prius");
        assertNode("/a/b/Cars/Sports/Infiniti G37");
        assertNode("/a/b/Cars/Utility/Land Rover LR3");
        assertNoNode("/a/b/Cars[2]");
        assertNoNode("/a/b/Cars/Hybrid[2]");
        assertNoNode("/a/b/Cars/Hybrid/Toyota Prius[2]");
        assertNoNode("/a/b/Cars/Sports[2]");

        // And attempt to reimport the same content (with UUIDs) into the repository that already has that content ...
        // print = true;
        assertImport("io/cars-system-view-with-uuids.xml", "/a/c", ImportBehavior.THROW);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Import System View WITH 'jcr:root' node and NO matching uuids
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldImportSystemViewOfEntireWorkspaceWithNoAlreadyUsedUuids() throws Exception {
        // Register the Cars node types ...
        tools.registerNodeTypes(session, "cars.cnd");

        // Set up the repository ...
        assertImport("io/full-workspace-system-view-with-uuids.xml", "/", ImportBehavior.THROW); // no matching UUIDs expected
        assertNode("/a/b/Cars");
        assertNode("/a/b/Cars/Hybrid");
        assertNode("/a/b/Cars/Hybrid/Toyota Prius");
        assertNode("/a/b/Cars/Sports/Infiniti G37");
        assertNode("/a/b/Cars/Utility/Land Rover LR3");
        assertNoNode("/a/b/Cars[2]");
        assertNoNode("/a/b/Cars/Hybrid[2]");
        assertNoNode("/a/b/Cars/Hybrid/Toyota Prius[2]");
        assertNoNode("/a/b/Cars/Sports[2]");
    }

    @Test
    public void shouldImportSystemViewOfEntireWorkspaceExportedFromJackrabbit() throws Exception {
        // Register the Cars node types ...
        tools.registerNodeTypes(session, "cars.cnd");

        // Set up the repository ...
        assertImport("io/full-workspace-system-view.xml", "/", ImportBehavior.THROW); // no matching UUIDs expected
        assertNode("/page1");
    }

    @Test
    public void shouldImportFileExportedFromJackrabbitContainingBinaryData() throws Exception {
        // Register the node types ...
        tools.registerNodeTypes(session, "cars.cnd");
        tools.registerNodeTypes(session, "cnd/magnolia.cnd");
        assertThat(session.getWorkspace().getNodeTypeManager().getNodeType("mgnl:content"), is(notNullValue()));

        // Now import the file ...
        String filename = "io/system-export-with-binary-data-and-uuids.xml";
        assertImport(filename, "/a", ImportBehavior.THROW); // no matching UUIDs expected
    }

    @FixFor( "MODE-1026" )
    @Test
    public void shouldImportFileExportedFromJackrabbitContainingBinaryStringData() throws Exception {
        // Register the node types ...
        tools.registerNodeTypes(session, "cars.cnd");
        tools.registerNodeTypes(session, "cnd/magnolia.cnd");
        assertThat(session.getWorkspace().getNodeTypeManager().getNodeType("mgnl:content"), is(notNullValue()));

        // Now import the file ...
        String filename = "io/system-export-with-xsitype-data-and-uuids.xml";
        assertImport(filename, "/a", ImportBehavior.THROW); // no matching UUIDs expected
        // print = true;
        print("/a");
        Node imageNode = assertNode("/a/company/image");
        assertThat(imageNode.getProperty("extension").getValue().getString(), is("gif"));
    }

    @Test
    public void shouldDecodeBase64() throws Exception {
        String base64Str = "R0lGODlhEAAQAMZpAGxZMW1bNW9bMm9cNnJdNXJdNnNfN3tnPX5oQIBqQYJrO4FrQoVtQIZuQohxQopyRopzQYtzRo10Qo51SI12SJB3SZN5Q5N5RpV7TJZ8TJd9SJh+TpyAT52CUJ+FUaOGU6OHUaSIUqGKUqaJVaiKVaGMV6mLVqWOXqyOVayOWLCSWa2VWrSVW7aXXbSbXbqaXrqaX7uaX7ubXsCfYrigcMKgY8OhY8SiZMWjZMWjZcelZsimZsqnZ8unZ8uoaMypaM2pac6qacOtbc+ratCsatKta8uwbdGvctOvbtSvbNWwbciyhdaxbdm2dda7ddq5gd26fN28gNe/ed6+htvCeuHAhd3EfOLCidrDmd7GfuLEj9/HfubKlufLmOjLmOnMmOnNmujNne3UpuzUqe3Vp+7VqO/VqO/Wqe7YsP///////////////////////////////////////////////////////////////////////////////////////////yH+EUNyZWF0ZWQgd2l0aCBHSU1QACH5BAEKAH8ALAAAAAAQABAAAAeJgH+Cg4SFhoeIiYqLhSciiR40S1hoY0JZVE5GK4MOZWdmZGJhJVumW1aDFGBfXl1cWiRSp1sufxYXV1VQUVNPMSAYDwgHEINNTEpJSEcwKR0VCwWERURDQUA9LSYcEwkDhD8+PDs6OCwjGxEIAYQyOTc2NTMqHxkNBgCFChIaKC8hGBBgJIARo0AAOw==";
        boolean print = false;

        // // Try apache ...
        // byte[] apacheBytes = org.apache.util.Base64.decode(base64Str.getBytes("UTF-8"));
        // if (print) {
        // System.out.println("Apache:     " + toString(apacheBytes));
        // System.out.println("   length:  " + apacheBytes.length);
        // }

        // Try jboss ...
        byte[] jbossBytes = org.jboss.util.Base64.decode(base64Str);
        if (print) {
            System.out.println("JBoss:      " + toString(jbossBytes));
            System.out.println("   length:  " + jbossBytes.length);
        }

        // Try jackrabbit ...
        ByteArrayOutputStream jrOutput = new ByteArrayOutputStream();
        org.apache.jackrabbit.test.api.Base64.decode(base64Str, jrOutput);
        byte[] jrBytes = jrOutput.toByteArray();
        if (print) {
            System.out.println("Jackrabbit: " + toString(jrBytes));
            System.out.println("   length:  " + jrBytes.length);
        }

        // Try modeshape ...
        byte[] msBytes = org.modeshape.common.util.Base64.decode(base64Str);
        if (print) {
            System.out.println("ModeShape:  " + toString(msBytes));
            System.out.println("   length:  " + msBytes.length);
        }

        // assertThat(apacheBytes, is(jbossBytes)); // apache pads 3 0s at the end
        assertThat(jrBytes, is(jbossBytes));
        assertThat(msBytes, is(jbossBytes));
    }

    String toString( byte[] bytes ) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(b);
        }
        return sb.toString();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Import Document and System View containing constraint violations
    // ----------------------------------------------------------------------------------------------------------------

    @FixFor( "MODE-1139" )
    @Test( expected = ConstraintViolationException.class )
    public void shouldThrowCorrectExceptionWhenImportingDocumentViewContainingPropertiesThatViolateConstraints() throws Exception {
        // Register the node types ...
        tools.registerNodeTypes(session, "cars.cnd");

        // Set up the repository ...
        assertImport("io/full-workspace-document-view-with-constraint-violation.xml", "/", ImportBehavior.THROW);
    }

    @FixFor( "MODE-1139" )
    @Test( expected = ConstraintViolationException.class )
    public void shouldThrowCorrectExceptionWhenImportingSystemViewContainingPropertiesThatViolateConstraints() throws Exception {
        // Register the node types ...
        tools.registerNodeTypes(session, "cars.cnd");

        // Set up the repository ...
        assertImport("io/full-workspace-system-view-with-constraint-violation.xml", "/", ImportBehavior.THROW);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Import Document View WITH 'jcr:root' node and WITH uuids
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldImportDocumentViewWithUuids() throws Exception {
        // Register the node types ...
        tools.registerNodeTypes(session, "cars.cnd");

        // Set up the repository ...
        assertImport("io/full-workspace-document-view-with-uuids.xml", "/", ImportBehavior.THROW); // no matching UUIDs expected
        assertNode("/a/b/Cars");
        assertNode("/a/b/Cars/Hybrid");
        assertNode("/a/b/Cars/Hybrid/Toyota Prius");
        assertNode("/a/b/Cars/Sports/Infiniti G37");
        assertNode("/a/b/Cars/Utility/Land Rover LR3");
        assertNoNode("/a/b/Cars[2]");
        assertNoNode("/a/b/Cars/Hybrid[2]");
        assertNoNode("/a/b/Cars/Hybrid/Toyota Prius[2]");
        assertNoNode("/a/b/Cars/Sports[2]");
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Import Document View WITH constraints
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldImportDocumentViewWithReferenceConstraints() throws Exception {
        // Register the node types ...
        tools.registerNodeTypes(session, "cars.cnd");

        // Set up the repository ...
        assertImport("io/full-workspace-document-view-with-uuids.xml", "/", ImportBehavior.THROW); // no matching UUIDs expected
        assertNode("/a/b/Cars");
        assertNode("/a/b/Cars/Hybrid");
        assertNode("/a/b/Cars/Hybrid/Toyota Prius");
        assertNode("/a/b/Cars/Sports/Infiniti G37");
        assertNode("/a/b/Cars/Utility/Land Rover LR3");
        assertNoNode("/a/b/Cars[2]");
        assertNoNode("/a/b/Cars/Hybrid[2]");
        assertNoNode("/a/b/Cars/Hybrid/Toyota Prius[2]");
        assertNoNode("/a/b/Cars/Sports[2]");
    }

    @FixFor( "MODE-1171" )
    @Test
    public void shouldExportAndImportMultiValuedPropertyWithSingleValue() throws Exception {
        Node rootNode = session.getRootNode();
        Node nodeA = rootNode.addNode("a", "nt:unstructured");
        Node nodeB = nodeA.addNode("b", "nt:unstructured");

        Value v = session.getValueFactory().createValue("singleValue");
        javax.jcr.Property prop = nodeB.setProperty("multiValuedProp", new Value[] {v});
        assertTrue(prop.isMultiple());

        prop = nodeB.setProperty("singleValuedProp", v);
        assertTrue(!prop.isMultiple());

        session.save();

        File exportFile = export("/a");
        try {

            nodeA.remove();
            session.save();

            assertImport(exportFile, "/", ImportBehavior.THROW);

            prop = session.getProperty("/a/b/multiValuedProp");

            assertTrue(prop.isMultiple());
            assertThat(prop.getValues().length, is(1));
            assertThat(prop.getValues()[0].getString(), is("singleValue"));

            prop = session.getProperty("/a/b/singleValuedProp");

            assertTrue(!prop.isMultiple());
            assertThat(prop.getString(), is("singleValue"));
        } finally {
            exportFile.delete();
        }

    }

    @Test
    public void shouldImportIntoWorkspaceTheDocumentViewOfTheContentUsedInTckTests() throws Exception {
        Session session3 = repository.login();

        session.nodeTypeManager().registerNodeTypes(resourceStream("tck/tck_test_types.cnd"), true);
        session.getWorkspace().importXML("/",
                                         resourceStream("tck/documentViewForTckTests.xml"),
                                         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
        assertThat(session.getRootNode().hasNode("testroot/workarea"), is(true));
        assertThat(session.getRootNode().getNode("testroot/workarea"), is(notNullValue()));
        assertThat(session.getNode("/testroot/workarea"), is(notNullValue()));
        assertNode("/testroot/workarea");

        Session session1 = repository.login();
        assertThat(session1.getRootNode().hasNode("testroot/workarea"), is(true));
        assertThat(session1.getRootNode().getNode("testroot/workarea"), is(notNullValue()));
        assertThat(session1.getNode("/testroot/workarea"), is(notNullValue()));
        session1.logout();

        assertThat(session3.getRootNode().hasNode("testroot/workarea"), is(true));
        assertThat(session3.getRootNode().getNode("testroot/workarea"), is(notNullValue()));
        assertThat(session3.getNode("/testroot/workarea"), is(notNullValue()));
        session3.logout();
    }

    @Test
    public void shouldImportIntoWorkspaceTheSystemViewOfTheContentUsedInTckTests() throws Exception {
        Session session3 = repository.login();

        session.nodeTypeManager().registerNodeTypes(resourceStream("tck/tck_test_types.cnd"), true);
        session.getWorkspace().importXML("/",
                                         resourceStream("tck/systemViewForTckTests.xml"),
                                         ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
        assertThat(session.getRootNode().hasNode("testroot/workarea"), is(true));
        assertThat(session.getRootNode().getNode("testroot/workarea"), is(notNullValue()));
        assertNode("/testroot/workarea");

        Session session1 = repository.login();
        assertThat(session1.getRootNode().hasNode("testroot/workarea"), is(true));
        assertThat(session1.getRootNode().getNode("testroot/workarea"), is(notNullValue()));
        assertThat(session1.getNode("/testroot/workarea"), is(notNullValue()));
        session1.logout();

        assertThat(session3.getRootNode().hasNode("testroot/workarea"), is(true));
        assertThat(session3.getRootNode().getNode("testroot/workarea"), is(notNullValue()));
        assertThat(session3.getNode("/testroot/workarea"), is(notNullValue()));
        session3.logout();
    }

    @Test
    public void shouldImportIntoSessionTheDocumentViewOfTheContentUsedInTckTests() throws Exception {
        Session session3 = repository.login();

        session.nodeTypeManager().registerNodeTypes(resourceStream("tck/tck_test_types.cnd"), true);
        session.importXML("/",
                          resourceStream("tck/documentViewForTckTests.xml"),
                          ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
        assertThat(session.getRootNode().hasNode("testroot/workarea"), is(true));
        assertThat(session.getRootNode().getNode("testroot/workarea"), is(notNullValue()));
        assertThat(session.getNode("/testroot/workarea"), is(notNullValue()));
        assertNode("/testroot/workarea");

        Session session1 = repository.login();
        assertThat(session1.getRootNode().hasNode("testroot/workarea"), is(false));

        session.save();

        assertThat(session1.getRootNode().hasNode("testroot/workarea"), is(true));
        assertThat(session1.getRootNode().getNode("testroot/workarea"), is(notNullValue()));
        assertThat(session1.getNode("/testroot/workarea"), is(notNullValue()));
        session1.logout();

        assertThat(session3.getRootNode().hasNode("testroot/workarea"), is(true));
        assertThat(session3.getRootNode().getNode("testroot/workarea"), is(notNullValue()));
        assertThat(session3.getNode("/testroot/workarea"), is(notNullValue()));
        session3.logout();
    }

    @Test
    public void shouldImportIntoSessionTheSystemViewOfTheContentUsedInTckTests() throws Exception {
        Session session3 = repository.login();

        session.nodeTypeManager().registerNodeTypes(resourceStream("tck/tck_test_types.cnd"), true);
        session.importXML("/",
                          resourceStream("tck/systemViewForTckTests.xml"),
                          ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
        assertThat(session.getRootNode().hasNode("testroot/workarea"), is(true));
        assertThat(session.getRootNode().getNode("testroot/workarea"), is(notNullValue()));
        assertNode("/testroot/workarea");

        Session session1 = repository.login();
        assertThat(session1.getRootNode().hasNode("testroot/workarea"), is(false));

        session.save();

        assertThat(session1.getRootNode().hasNode("testroot/workarea"), is(true));
        assertThat(session1.getRootNode().getNode("testroot/workarea"), is(notNullValue()));
        assertThat(session1.getNode("/testroot/workarea"), is(notNullValue()));
        session1.logout();

        assertThat(session3.getRootNode().hasNode("testroot/workarea"), is(true));
        assertThat(session3.getRootNode().getNode("testroot/workarea"), is(notNullValue()));
        assertThat(session3.getNode("/testroot/workarea"), is(notNullValue()));
        session3.logout();
    }

    @Test
    @FixFor( "MODE-1573" )
    public void shouldPerformRoundTripOnDocumentViewWithBinaryContent() throws Exception {
        JcrTools tools = new JcrTools();

        File binaryFile = new File("src/test/resources/io/binary.pdf");
        assert(binaryFile.exists() && binaryFile.isFile());

        File outputFile = File.createTempFile("modeshape_import_export_" + System.currentTimeMillis(), "_test");
        outputFile.deleteOnExit();
        tools.uploadFile(session, "file", binaryFile);
        session.save();
        session.exportDocumentView("/file", new FileOutputStream(outputFile), false, false);
        assertTrue(outputFile.length() > 0);

        session.getRootNode().getNode("file").remove();
        session.save();

        session.getWorkspace().importXML("/", new FileInputStream(outputFile), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        assertNotNull(session.getNode("/file"));
        assertNotNull(session.getNode("/file/jcr:content"));
        Property data = session.getNode("/file/jcr:content").getProperty("jcr:data");
        assertNotNull(data);
        Binary binary = (Binary)data.getBinary();
        assertNotNull(binary);
        assertEquals(binaryFile.length(), binary.getSize());
    }

    @FixFor( "MODE-1478" )
    @Test
    public void shouldBeAbleToImportDroolsXMLIntoSystemView() throws Exception {
        startRepositoryWithConfiguration(resourceStream("config/drools-repository.json"));
        session.importXML("/", resourceStream("io/drools-system-view.xml"), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);

        assertNode("/drools:repository", "nt:folder");
        assertNode("/drools:repository/drools:package_area", "nt:folder");
        assertNode("/drools:repository/drools:package_area/defaultPackage", "drools:packageNodeType");
        assertNode("/drools:repository/drools:package_area/defaultPackage/assets", "drools:versionableAssetFolder");
        assertNode("/drools:repository/drools:package_area/defaultPackage/assets/drools", "drools:assetNodeType");
        assertNode("/drools:repository/drools:packagesnapshot_area", "nt:folder");
        assertNode("/drools:repository/drools:tag_area", "nt:folder");
        assertNode("/drools:repository/drools:state_area", "nt:folder");
        assertNode("/drools:repository/drools.package.migrated", "nt:folder");
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Utilities
    // ----------------------------------------------------------------------------------------------------------------

    protected static enum ImportBehavior {
        CREATE_NEW(ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW),
        REPLACE_EXISTING(ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING),
        REMOVE_EXISTING(ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING),
        THROW(ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

        private final int jcrValue;

        private ImportBehavior( int value ) {
            this.jcrValue = value;
        }

        public int getJcrValue() {
            return jcrValue;
        }
    }

    protected void importFile( String importIntoPath,
                               String resourceName,
                               int importBehavior ) throws Exception {
        // Import the car content ...
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resourceName);
        assertThat(stream, is(notNullValue()));
        try {
            session.importXML(importIntoPath, stream, importBehavior); // shouldn't exist yet
        } finally {
            stream.close();
        }
    }

    protected Node assertImport( String resourceName,
                                 String pathToParent,
                                 ImportBehavior behavior ) throws RepositoryException, IOException {
        InputStream istream = resourceStream(resourceName);
        return assertImport(istream, pathToParent, behavior);
    }

    protected Node assertImport( File resource,
                                 String pathToParent,
                                 ImportBehavior behavior ) throws RepositoryException, IOException {
        InputStream istream = new FileInputStream(resource);
        return assertImport(istream, pathToParent, behavior);
    }

    protected Node assertImport( InputStream istream,
                                 String pathToParent,
                                 ImportBehavior behavior ) throws RepositoryException, IOException {
        // Make the parent node if it does not exist ...
        Path parentPath = path(pathToParent);
        assertThat(parentPath.isAbsolute(), is(true));
        Node node = session.getRootNode();
        boolean found = true;
        for (Path.Segment segment : parentPath) {
            String name = asString(segment);
            if (found) {
                try {
                    node = node.getNode(name);
                    found = true;
                } catch (PathNotFoundException e) {
                    found = false;
                }
            }
            if (!found) {
                node = node.addNode(name, "nt:unstructured");
            }
        }
        if (!found) {
            // We added at least one node, so we need to save it before importing ...
            session.save();
        }

        // Verify that the parent node does exist now ...
        assertNode(pathToParent);

        // Now, load the content of the resource being imported ...
        assertThat(istream, is(notNullValue()));
        try {
            session.getWorkspace().importXML(pathToParent, istream, behavior.getJcrValue());
        } finally {
            istream.close();
        }

        session.save();
        return node;
    }

    protected File export( String pathToParent ) throws IOException, RepositoryException {
        assertNode(pathToParent);

        // Export to a string ...
        File tmp = File.createTempFile("JcrImportExportText-", "");
        FileOutputStream ostream = new FileOutputStream(tmp);
        boolean skipBinary = false;
        boolean noRecurse = false;
        session.exportSystemView(pathToParent, ostream, skipBinary, noRecurse);
        return tmp;
    }

    protected void exportDocumentView( String pathToParent,
                                       OutputStream ostream ) throws RepositoryException, IOException {
        boolean skipBinary = false;
        boolean noRecurse = false;
        try {
            session.exportDocumentView(pathToParent, ostream, skipBinary, noRecurse);
        } finally {
            ostream.close();
        }
    }

}
