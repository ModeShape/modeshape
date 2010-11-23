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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import org.jboss.security.config.IDTrustConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.StringUtil;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.jcr.JcrRepository.Option;

/**
 * 
 */
public class JcrImportExportTest {

    static {
        // Initialize the JAAS configuration to allow for an admin login later
        // Initialize IDTrust
        String configFile = "security/jaas.conf.xml";
        IDTrustConfiguration idtrustConfig = new IDTrustConfiguration();

        try {
            idtrustConfig.config(configFile);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private JcrConfiguration configuration;
    private JcrEngine engine;
    private JcrRepository repository;
    private Session session;
    private boolean print;

    @Before
    public void beforeEach() throws Exception {
        configuration = new JcrConfiguration();
        configuration.repositorySource("source").usingClass(InMemoryRepositorySource.class).setDescription("The content store");
        configuration.repository("repo")
                     .setSource("source")
                     .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
                     .addNodeTypes(resourceUrl("cars.cnd"))
                     .setOption(Option.ANONYMOUS_USER_ROLES,
                                ModeShapeRoles.READONLY + "," + ModeShapeRoles.READWRITE + "," + ModeShapeRoles.ADMIN)
                     .setOption(Option.JAAS_LOGIN_CONFIG_NAME, "modeshape-jcr");
        engine = configuration.build();
        engine.start();

        // Start the repository ...
        repository = engine.getRepository("repo");

        // Log into a session ...
        session = repository.login();
    }

    @After
    public void afterEach() throws Exception {
        if (session != null) {
            try {
                session.logout();
            } finally {
                session = null;
            }
        }
        if (engine != null) {
            try {
                engine.shutdown();
                engine.awaitTermination(3, TimeUnit.SECONDS);
            } finally {
                engine = null;
                repository = null;
                configuration = null;
            }
        }
    }

    @Test
    public void shouldStartUp() {
        assertThat(session, is(notNullValue()));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Import System View with NO 'jcr:root' node and NO uuids
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldImportCarsSystemViewWithCreateNewBehaviorWhenImportedContentDoesNotContainJcrRoot() throws Exception {
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
        // Set up the repository ...
        assertImport("io/full-workspace-system-view.xml", "/", ImportBehavior.THROW); // no matching UUIDs expected
        assertNode("/page1");
    }

    @Test
    public void shouldImportFileExportedFromJackrabbitContainingBinaryData() throws Exception {
        // Load the Magnolia CND file ...
        CndNodeTypeReader cndReader = new CndNodeTypeReader(session);
        cndReader.read("magnolia.cnd");
        session.getWorkspace().getNodeTypeManager().registerNodeTypes(cndReader.getNodeTypeDefinitions(), false);
        assertThat(session.getWorkspace().getNodeTypeManager().getNodeType("mgnl:content"), is(notNullValue()));

        // Now import the file ...
        String filename = "io/system-export-with-binary-data-and-uuids.xml";
        assertImport(filename, "/a", ImportBehavior.THROW); // no matching UUIDs expected
    }

    @FixFor( "MODE-1026" )
    @Test
    public void shouldImportFileExportedFromJackrabbitContainingBinaryStringData() throws Exception {
        CndNodeTypeReader cndReader = new CndNodeTypeReader(session);
        cndReader.read("magnolia.cnd");
        session.getWorkspace().getNodeTypeManager().registerNodeTypes(cndReader.getNodeTypeDefinitions(), false);
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

        // Try apache ...
        byte[] apacheBytes = org.apache.util.Base64.decode(base64Str.getBytes("UTF-8"));
        if (print) {
            System.out.println("Apache:     " + toString(apacheBytes));
            System.out.println("   length:  " + apacheBytes.length);
        }

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
        for (byte b : bytes)
            sb.append(b);
        return sb.toString();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Import System View WITH 'jcr:root' node and WITH uuids
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldImportDocumentView() throws Exception {
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
    // Import System View WITH constraints
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldImportSystemViewWithReferenceConstraints() throws Exception {
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

    protected Path path( String path ) {
        return engine.getExecutionContext().getValueFactories().getPathFactory().create(path);
    }

    protected String relativePath( String path ) {
        return !path.startsWith("/") ? path : path.substring(1);
    }

    protected String asString( Object value ) {
        return engine.getExecutionContext().getValueFactories().getStringFactory().create(value);
    }

    protected Node assertNode( String path ) throws RepositoryException {
        // Verify that the parent node does exist now ...
        String relativePath = relativePath(path);
        Node root = session.getRootNode();
        if (relativePath.trim().length() == 0) {
            // This is the root path, so of course it exists ...
            assertThat(root, is(notNullValue()));
            return session.getNode(path);
        }
        if (print && !root.hasNode(relativePath)) {
            Node parent = root;
            int depth = 0;
            for (Segment segment : path(path)) {
                if (!parent.hasNode(asString(segment))) {
                    System.out.println("Unable to find '" + path + "'; lowest node is '" + parent.getPath() + "'");
                    break;
                }
                parent = parent.getNode(asString(segment));
                ++depth;
            }
        }
        assertThat(root.hasNode(relativePath), is(true));
        return session.getNode(path);
    }

    protected void assertNoNode( String path ) throws RepositoryException {
        // Verify that the parent node does exist now ...
        assertThat(session.getRootNode().hasNode(relativePath(path)), is(false));
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

    protected void addMixinRecursively( String path,
                                        String... nodeTypes ) throws RepositoryException {
        Node node = session.getRootNode().getNode(relativePath(path));
        addMixin(node, true, nodeTypes);
    }

    protected Node addMixin( String path,
                             String... nodeTypes ) throws RepositoryException {
        Node node = session.getRootNode().getNode(relativePath(path));
        return addMixin(node, false, nodeTypes);
    }

    protected Node addMixin( Node node,
                             boolean recursive,
                             String... nodeTypes ) throws RepositoryException {
        assertThat(node, is(notNullValue()));
        for (String nodeType : nodeTypes) {
            if (!hasMixin(node, nodeType)) {
                node.addMixin(nodeType);
            }
        }
        if (recursive) {
            NodeIterator children = node.getNodes();
            while (children.hasNext()) {
                addMixin(children.nextNode(), true, nodeTypes);
            }
        }
        return node;
    }

    protected boolean hasMixin( Node node,
                                String mixinNodeType ) throws RepositoryException {
        for (NodeType mixin : node.getMixinNodeTypes()) {
            if (mixin.getName().equals(mixinNodeType)) return true;
        }
        return false;
    }

    protected void print() throws RepositoryException {
        print(session.getRootNode(), true);
    }

    protected void print( String path ) throws RepositoryException {
        Node node = session.getRootNode().getNode(relativePath(path));
        print(node, true);
    }

    protected void print( Node node,
                          boolean includeSystem ) throws RepositoryException {
        if (print) {
            if (!includeSystem && node.getPath().equals("/jcr:system")) return;
            if (node.getDepth() != 0) {
                int snsIndex = node.getIndex();
                String segment = node.getName() + (snsIndex > 1 ? ("[" + snsIndex + "]") : "");
                System.out.println(StringUtil.createString(' ', 2 * node.getDepth()) + '/' + segment);
            }
            NodeIterator children = node.getNodes();
            while (children.hasNext()) {
                print(children.nextNode(), includeSystem);
            }
        }
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

    protected static URI resourceUri( String name ) throws URISyntaxException {
        return resourceUrl(name).toURI();
    }

    protected static URL resourceUrl( String name ) {
        return JcrQueryManagerTest.class.getClassLoader().getResource(name);
    }

    protected static InputStream resourceStream( String name ) {
        return JcrQueryManagerTest.class.getClassLoader().getResourceAsStream(name);
    }

}
