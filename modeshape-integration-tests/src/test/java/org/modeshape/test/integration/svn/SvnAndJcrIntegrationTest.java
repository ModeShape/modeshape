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
package org.modeshape.test.integration.svn;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.connector.svn.SvnRepositorySource;
import org.modeshape.graph.SecurityContext;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrSecurityContextCredentials;
import org.modeshape.jcr.JcrRepository.Option;

/**
 * 
 */
public class SvnAndJcrIntegrationTest {
    private JcrEngine engine;
    private Session session;

    @Before
    public void beforeEach() throws Exception {
        final String repositoryUrl = "http://anonsvn.jboss.org/repos/dna/";
        final String[] predefinedWorkspaceNames = {"trunk/dna-common/src/main/java/org/jboss/dna/common/xml",};
        final String svnRepositorySource = "svnRepositorySource";
        final String repositoryName = "svnRepository";
        final JcrConfiguration configuration = new JcrConfiguration();
        configuration.repositorySource(svnRepositorySource).usingClass(SvnRepositorySource.class).setProperty("password", "").setProperty("username",
                                                                                                                                          "anonymous").setProperty("repositoryRootUrl",
                                                                                                                                                                   repositoryUrl).setProperty("predefinedWorkspaceNames",
                                                                                                                                                                                              predefinedWorkspaceNames).setProperty("defaultWorkspaceName",
                                                                                                                                                                                                                                    predefinedWorkspaceNames[0]).setProperty("creatingWorkspacesAllowed",
                                                                                                                                                                                                                                                                             false);

        configuration.repository(repositoryName).setSource(svnRepositorySource).setOption(Option.QUERY_EXECUTION_ENABLED, "true");

        configuration.save();
        this.engine = configuration.build();
        this.engine.start();

        this.session = this.engine.getRepository(repositoryName).login(new JcrSecurityContextCredentials(
                                                                                                         new MyCustomSecurityContext()));

    }

    @After
    public void afterEach() throws Exception {
        if (this.session != null) {
            this.session.logout();
        }
        if (this.engine != null) {
            this.engine.shutdown();
        }
    }

    @Test
    public void shouldIterateOverChildrenOfRoot() throws Exception {
        System.out.println("Getting the root node and it's children ...");
        NodeIterator nodeIterator = this.session.getRootNode().getNodes();

        while (nodeIterator.hasNext()) {
            System.out.println(nodeIterator.nextNode());
        }
        assertThat(this.session.getRootNode().getNode("XmlCharacters.java"), is(notNullValue()));
    }

    @Test
    public void shouldProvideAccessToJcrDataNodeUnderFileNode() throws Exception {
        System.out.println("Getting /package-info.java/jcr:content and then walking its properties ...");
        Node resourceNodeOfPomFile = this.session.getRootNode().getNode("package-info.java/jcr:content");
        assertThat(resourceNodeOfPomFile, is(notNullValue()));

        for (PropertyIterator iter = resourceNodeOfPomFile.getProperties(); iter.hasNext();) {
            Property property = iter.nextProperty();
            assertThat(property.getName(), is(notNullValue()));
        }
    }

    protected class MyCustomSecurityContext implements SecurityContext {
        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.SecurityContext#getUserName()
         */
        public String getUserName() {
            return "Fred";
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.SecurityContext#hasRole(java.lang.String)
         */
        public boolean hasRole( String roleName ) {
            return true;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.SecurityContext#logout()
         */
        public void logout() {
            // do something
        }
    }
}
