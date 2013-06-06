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
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.util.FileUtil;
import org.modeshape.connector.filesystem.FileSystemConnector;
import org.modeshape.jcr.Connectors.PathMappings;
import org.modeshape.jcr.federation.spi.Connector;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.WorkspaceAndPath;

/**
 * Because the {@link Connectors} test expects a {@link JcrRepository.RunningState} instance and directly manipulates the
 * repository content with the correct projection definitions, this class is actually relatively difficult to unit test.
 * Therefore, this test simply starts up a ModeShape repository instance in the normal fashion, and then uses package-level
 * methods to access the Connectors instance.
 */
public class ConnectorsTest extends SingleUseAbstractTest {

    private Connectors connectors;

    @Before
    @Override
    public void beforeEach() throws Exception {
        // We're using a Repository configuration that persists content, so clean it up ...
        FileUtil.delete("target/federation_persistent_repository");
        // And make sure that the 'generated-surefire' directory exists (it often doesn't when running in Eclipse)
        // and create an empty 'tmp' directory underneath it ...
        new File("target/generated-surefire/tmp").mkdirs();
        // Now start the repository ...
        startRepositoryWithConfiguration(resource("config/repo-config-filesystem-federation-with-persistence.json"));
        printMessage("Started repository...");
        connectors = repository().runningState().connectors();
    }

    @After
    @Override
    public void afterEach() throws Exception {
        super.afterEach();
        FileUtil.delete("target/federation_persistent_repository");
    }

    @Test
    public void shouldObtainPathMappingsForConnectorAndResolvePathsCorrectly() {
        Connector conn = connectors.getConnectorForSourceName("targetDirectory");
        assertThat(conn, is(instanceOf(FileSystemConnector.class)));
        PathMappings mappings = connectors.getPathMappings(conn);
        assertThat(mappings.getConnectorSourceName(), is("targetDirectory"));
        assertThat(mappings.getPathFactory(), is(notNullValue()));
        assertPathResolves(mappings, "/classes", "default", "/federation/classes");
        assertPathResolves(mappings, "/classes/org", "default", "/federation/classes/org");
        assertPathResolves(mappings, "/classes/org/modeshape", "default", "/federation/classes/org/modeshape");
        assertPathResolves(mappings, "/generated-sources", "default", "/federation/generated-sources");
        assertPathResolves(mappings, "/generated-surefire", "default", "/federation/surefire");
        assertPathResolves(mappings, "/generated-surefire/tmp", "default", "/federation/surefire/tmp");
    }

    protected void assertPathResolves( PathMappings mappings,
                                       String externalPath,
                                       String... workspaceNameAndPathPairs ) {
        Path extPath = mappings.getPathFactory().create(externalPath);
        Set<WorkspaceAndPath> expectedWsAndPaths = new HashSet<WorkspaceAndPath>();
        for (int i = 0; i != workspaceNameAndPathPairs.length; ++i) {
            String workspaceName = workspaceNameAndPathPairs[i];
            String repoPath = workspaceNameAndPathPairs[++i];
            Path repPath = mappings.getPathFactory().create(repoPath);
            expectedWsAndPaths.add(new WorkspaceAndPath(workspaceName, repPath));
        }
        Collection<WorkspaceAndPath> wsAndPaths = mappings.resolveExternalPathToInternal(extPath);
        Set<WorkspaceAndPath> actualWsAndPaths = new HashSet<WorkspaceAndPath>(wsAndPaths);
        assertThat(actualWsAndPaths, is(expectedWsAndPaths));
    }

}
