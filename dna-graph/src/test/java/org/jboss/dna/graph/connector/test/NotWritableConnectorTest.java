/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.connector.test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.jboss.dna.common.util.IoUtil;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.request.InvalidRequestException;
import org.junit.Before;
import org.junit.Test;

/**
 * A class that provides standard verification tests for connectors that do not allow updates. This class is designed to be
 * extended for each connector, and in each subclass the {@link #setUpSource()} method is defined to provide a valid
 * {@link RepositorySource} for the connector to be tested, and that source should have some content.
 * <p>
 * Since these tests attempt to modify repository content, the repository is set up for each test, given each test a pristine
 * repository (as {@link #initializeContent(Graph) initialized} by the concrete test case class).
 * </p>
 */
public abstract class NotWritableConnectorTest extends AbstractConnectorTest {

    protected String[] validLargeValues;

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();

        // Load in the large value ...
        validLargeValues = new String[] {IoUtil.read(getClass().getClassLoader().getResourceAsStream("LoremIpsum1.txt")),
            IoUtil.read(getClass().getClassLoader().getResourceAsStream("LoremIpsum2.txt")),
            IoUtil.read(getClass().getClassLoader().getResourceAsStream("LoremIpsum3.txt"))};
    }

    /**
     * These tests require that the source supports updates, since all of the tests do some form of updates.
     */
    @Test
    public void shouldNotHaveUpdateCapabilities() {
        assertThat(source.getCapabilities().supportsUpdates(), is(false));
    }

    @Test( expected = InvalidRequestException.class )
    public void shouldNotAllowSettingPropertyOnRootNode() {
        graph.set("propA").to("valueA").on("/");
    }

    @Test( expected = InvalidRequestException.class )
    public void shouldNowAllowAddChildUnderRootNode() {
        graph.batch().create("/a").with("propB", "valueB").and("propC", "valueC").and().execute();
    }

    @Test( expected = InvalidRequestException.class )
    public void shouldNotAllowMultipleUpdatesInBatch() {
        Graph.Create<Graph.Batch> create = graph.batch().create("/a");
        for (int i = 0; i != 100; ++i) {
            create = create.with("property" + i, "value" + i);
        }
        create.and().execute();
    }

    @Test( expected = InvalidRequestException.class )
    public void shouldNotAllowDeletingNodes() {
        // Find a child under the root ...
        for (Location child : graph.getChildren().of("/")) {
            graph.delete(child); // first one should fail
            fail("No error when attempting to delete the first child under root");
        }
    }

    @Test( expected = InvalidRequestException.class )
    public void shouldNotAllowCopyingNodes() {
        // Find a child under the root ...
        for (Location child : graph.getChildren().of("/")) {
            graph.copy(child).into("/"); // first one should fail
            fail("No error when attempting to copy root's first child");
        }
    }

}
