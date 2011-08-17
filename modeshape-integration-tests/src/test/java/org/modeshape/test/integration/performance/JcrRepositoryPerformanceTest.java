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
package org.modeshape.test.integration.performance;

import javax.jcr.ImportUUIDBehavior;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.test.ModeShapeSingleUseTest;

public class JcrRepositoryPerformanceTest extends ModeShapeSingleUseTest {

    private static final int NUMBER_OF_COPIES = 150;
    private GuvnorEmulator guvnor;

    @Before
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
    }

    @Ignore( "Removed from automatic builds due to time of test. Can be run manually." )
    @Test
    public void shouldSimulateGuvnorUsageAgainstRepositoryWithInMemoryStore() throws Exception {
        startEngineUsing("config/configRepositoryForDroolsInMemoryPerformance.xml");
        sessionTo("Repo");
        assertNode("/", "mode:root");
        guvnor = new GuvnorEmulator(repository(), NUMBER_OF_COPIES, false);
        // import the file ...
        importContent(getClass(), "io/drools/mortgage-sample-repository.xml");
        session().refresh(false);

        // Verify the file was imported ...
        guvnor.verifyContent();

        guvnor.simulateGuvnorUsage(5);
    }

    @Ignore( "Removed from automatic builds due to time of test. Can be run manually." )
    @Test
    public void shouldSimulateGuvnorUsageAgainstRepositoryWithJpaStore() throws Exception {
        startEngineUsing("config/configRepositoryForDroolsJpaCreate.xml");
        sessionTo("Repo");
        assertNode("/", "mode:root");
        guvnor = new GuvnorEmulator(repository(), NUMBER_OF_COPIES, false);
        // import the file ...
        importContent(getClass(), "io/drools/mortgage-sample-repository.xml");
        session().refresh(false);

        // Verify the file was imported ...
        guvnor.verifyContent();

        guvnor.simulateGuvnorUsage(30);
    }

    @Ignore( "Removed from automatic builds due to time of test. Can be run manually." )
    @FixFor( "MODE-1113" )
    @Test
    public void shouldHaveImportContentAvailableAfterRestart() throws Exception {
        startEngineUsing("config/configRepositoryForDroolsJpaCreate.xml");
        sessionTo("Repo");
        assertNode("/", "mode:root");
        guvnor = new GuvnorEmulator(repository(), NUMBER_OF_COPIES, false);
        // import the file ...
        importContent(getClass(), "io/drools/mortgage-sample-repository.xml");
        session().refresh(false);
        printSubgraph(assertNode("/drools:repository"));

        // Verify the file was imported ...
        guvnor.verifyContent();

        // Now shut down the engine ...
        stopEngine();

        // And restart the engine ...
        startEngineUsing("config/configRepositoryForDroolsJpaNoNodeTypes.xml");
        sessionTo("Repo");
        assertNode("/", "mode:root");
        printSubgraph(assertNode("/drools:repository"));

        // VVerify the content is still here ...
        guvnor.verifyContent();
    }

    @FixFor( "MODE-1114" )
    @Test
    public void shouldImportMultipleTimesAsNewContent() throws Exception {
        startEngineUsing("config/configRepositoryForDroolsInMemoryPerformance.xml");
        sessionTo("Repo");
        assertNode("/", "mode:root");
        guvnor = new GuvnorEmulator(repository(), NUMBER_OF_COPIES, false);

        // import the file multiple times ...
        int importBehavior = ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW;
        for (int i = 0; i != 3; ++i) {
            importContent(getClass(), "io/drools/mortgage-sample-repository.xml", importBehavior);
            session().refresh(false);
            assertNode("/drools:repository");

            // Verify the file was imported ...
            guvnor.verifyContent();
        }

        guvnor.simulateGuvnorUsage(1);
    }

    @Ignore( "Removed from automatic builds due to time of test. Can be run manually." )
    @FixFor( "MODE-1114" )
    @Test
    public void shouldImportMultipleTimesAsNewContentUsingJpa() throws Exception {
        startEngineUsing("config/configRepositoryForDroolsJpaCreate.xml");
        sessionTo("Repo");
        assertNode("/", "mode:root");
        guvnor = new GuvnorEmulator(repository(), NUMBER_OF_COPIES, false);

        // import the file multiple times ...
        int importBehavior = ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW;
        for (int i = 0; i != 3; ++i) {
            importContent(getClass(), "io/drools/mortgage-sample-repository.xml", importBehavior);
            session().refresh(false);
            assertNode("/drools:repository");

            // Verify the file was imported ...
            guvnor.verifyContent();
        }

        guvnor.simulateGuvnorUsage(1);
    }

    @FixFor( "MODE-1114" )
    @Test
    public void shouldImportMultipleTimesAsReplacedContent() throws Exception {
        startEngineUsing("config/configRepositoryForDroolsInMemoryPerformance.xml");
        sessionTo("Repo");
        assertNode("/", "mode:root");
        guvnor = new GuvnorEmulator(repository(), NUMBER_OF_COPIES, false);

        // import the file multiple times ...
        int importBehavior = ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING;
        for (int i = 0; i != 2; ++i) {
            importContent(getClass(), "io/drools/mortgage-sample-repository.xml", importBehavior);
            session().refresh(false);
            assertNode("/drools:repository");

            // Verify the file was imported ...
            guvnor.verifyContent();
        }

        guvnor.simulateGuvnorUsage(1);
    }

    @Ignore( "Removed from automatic builds due to time of test. Can be run manually." )
    @FixFor( "MODE-1114" )
    @Test
    public void shouldImportMultipleTimesAsReplacedContentUsingJpa() throws Exception {
        startEngineUsing("config/configRepositoryForDroolsJpaCreate.xml");
        sessionTo("Repo");
        assertNode("/", "mode:root");
        guvnor = new GuvnorEmulator(repository(), NUMBER_OF_COPIES, false);

        // import the file multiple times ...
        int importBehavior = ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING;
        for (int i = 0; i != 2; ++i) {
            importContent(getClass(), "io/drools/mortgage-sample-repository.xml", importBehavior);
            session().refresh(false);
            assertNode("/drools:repository");

            // Verify the file was imported ...
            guvnor.verifyContent();
        }

        guvnor.simulateGuvnorUsage(1);
    }

    @FixFor( "MODE-1114" )
    @Test
    public void shouldImportOnceAndSimulateGuvnorUsage() throws Exception {
        startEngineUsing("config/configRepositoryForDroolsInMemoryPerformance.xml");
        sessionTo("Repo");
        assertNode("/", "mode:root");
        guvnor = new GuvnorEmulator(repository(), NUMBER_OF_COPIES, false);

        // import the file multiple times ...
        int importBehavior = ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING;
        importContent(getClass(), "io/drools/mortgage-sample-repository.xml", importBehavior);
        session().refresh(false);
        assertNode("/drools:repository");

        // Verify the file was imported ...
        guvnor.verifyContent();

        guvnor.simulateGuvnorUsage(1);
    }

    @FixFor( "MODE-1114" )
    @Test
    public void shouldImportOnceAndSimulateGuvnorUsageUsingJpa() throws Exception {
        startEngineUsing("config/configRepositoryForDroolsJpaCreate.xml");
        sessionTo("Repo");
        assertNode("/", "mode:root");
        guvnor = new GuvnorEmulator(repository(), NUMBER_OF_COPIES, false);

        // import the file multiple times ...
        int importBehavior = ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING;
        importContent(getClass(), "io/drools/mortgage-sample-repository.xml", importBehavior);
        session().refresh(false);
        assertNode("/drools:repository");

        // Verify the file was imported ...
        guvnor.verifyContent();

        guvnor.simulateGuvnorUsage(1);
    }

    @FixFor( "MODE-1114" )
    @Test
    public void shouldImportAndSimulateGuvnorUsageTwice() throws Exception {
        startEngineUsing("config/configRepositoryForDroolsInMemoryPerformance.xml");
        sessionTo("Repo");
        assertNode("/", "mode:root");
        guvnor = new GuvnorEmulator(repository(), NUMBER_OF_COPIES, false);

        // import the file multiple times ...
        int importBehavior = ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING;
        importContent(getClass(), "io/drools/mortgage-sample-repository.xml", importBehavior);
        session().refresh(false);
        assertNode("/drools:repository");

        // Verify the file was imported ...
        guvnor.verifyContent();

        guvnor.simulateGuvnorUsage(2);
        guvnor.printVersionHistory("/drools:repository/drools:package_area/mortgages/assets/ApplicantDsl");

        // Import over the top ...
        session().refresh(false);
        importContent(getClass(), "io/drools/mortgage-sample-repository.xml", importBehavior);
        session().refresh(false);
        assertNode("/drools:repository");

        // Verify the file was imported ...
        guvnor.verifyContent();
        guvnor.printVersionHistory("/drools:repository/drools:package_area/mortgages/assets/ApplicantDsl");

        guvnor.simulateGuvnorUsage(1);

        guvnor.printVersionHistory("/drools:repository/drools:package_area/mortgages/assets/ApplicantDsl");
    }

    @Ignore( "Removed from automatic builds due to time of test. Can be run manually." )
    @FixFor( "MODE-1114" )
    @Test
    public void shouldImportAndSimulateGuvnorUsageTwiceUsingJpa() throws Exception {
        startEngineUsing("config/configRepositoryForDroolsJpaCreate.xml");
        sessionTo("Repo");
        assertNode("/", "mode:root");
        guvnor = new GuvnorEmulator(repository(), NUMBER_OF_COPIES, false);

        // import the file multiple times ...
        int importBehavior = ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING;
        importContent(getClass(), "io/drools/mortgage-sample-repository.xml", importBehavior);
        session().refresh(false);
        assertNode("/drools:repository");

        // Verify the file was imported ...
        guvnor.verifyContent();
        guvnor.printVersionHistory("/drools:repository/drools:package_area/mortgages/assets/ApplicantDsl");

        // guvnor.simulateGuvnorUsage(1);

        // Import over the top ...
        session().refresh(false);
        importContent(getClass(), "io/drools/mortgage-sample-repository.xml", importBehavior);
        session().refresh(false);
        assertNode("/drools:repository");

        // Verify the file was imported ...
        guvnor.verifyContent();
        guvnor.printVersionHistory("/drools:repository/drools:package_area/mortgages/assets/ApplicantDsl");

        guvnor.simulateGuvnorUsage(1);

    }
}
