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

import static org.junit.Assert.assertTrue;
import java.io.File;
import javax.annotation.Resource;
import javax.jcr.Node;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.api.Session;

/**
 * Integration test which verifies that various external sources are correctly set-up via the JBoss AS subsystem.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@RunWith( Arquillian.class )
public class NoQueriesIntegrationTest {

    static {
        System.setProperty("arquillian.launch", "jboss7-test");
    }

    @AfterClass
    public static void clearActiveContainer() {
        System.clearProperty("arquillian.launch");
    }

    @Resource( mappedName = "/jcr/noQueryRepository" )
    private JcrRepository repository;

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "noQueryRepository-test.war");
        // Add our custom Manifest, which has the additional Dependencies entry ...
        archive.setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
        return archive;
    }

    @Test
    public void shouldStillBeAbleToAddNodes() throws Exception {
        Session session = repository.login();
        Node testNode = session.getRootNode().addNode("repos");
        session.save();
        session.logout();

        session = repository.login();
        Node testNode2 = session.getNode("/repos");
        assertTrue(testNode.isSame(testNode2));
        session.logout();

        // Queries return nothing ...
        session = repository.login();
        Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [nt:base]", Query.JCR_SQL2);
        QueryResult results = query.execute();
        assertTrue(results.getNodes().getSize() == 0);
        session.logout();
    }
}
