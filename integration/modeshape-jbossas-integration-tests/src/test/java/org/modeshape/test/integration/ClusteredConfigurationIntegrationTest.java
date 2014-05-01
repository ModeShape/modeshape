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

package org.modeshape.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.File;
import javax.annotation.Resource;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.RepositoryConfiguration;

/**
 * Arquillian test which verifies that a repository using a clustered configuration starts up.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@RunWith( Arquillian.class)
public class ClusteredConfigurationIntegrationTest {

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "clustered-repo-config-test.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"));
        // Add our custom Manifest, which has the additional Dependencies entry ...
        archive.setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
        return archive;
    }

    @Resource( mappedName = "java:/jcr/repo-clustered" )
    private JcrRepository clusteredRepository;

    @Before
    public void before() {
        assertNotNull(clusteredRepository);
    }

    @Test
    @FixFor({"MODE-1923", "MODE-1929"})
    public void clusteredRepositoryShouldHaveStartedUp() throws Exception {
        RepositoryConfiguration.Clustering clusteringConfiguration = clusteredRepository.getConfiguration().getClustering();
        assertEquals("modeshape-cluster", clusteringConfiguration.getClusterName());
        assertNotNull(clusteringConfiguration.getChannel());

        Session session = clusteredRepository.login();
        assertNotNull(session);
        session.logout();
    }

    @Test
    @FixFor( "MODE-1935" )
    public void shouldIndexNodesOnMaster() throws Exception {
        Session session = clusteredRepository.login();
        session.getRootNode().addNode("test");
        session.save();

        String queryString = "select * from [nt:unstructured] where [jcr:name] like '%test%'";
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        QueryResult result = queryManager.createQuery(queryString, Query.JCR_SQL2).execute();
        assertTrue(result.getNodes().getSize() > 0);
    }
}
