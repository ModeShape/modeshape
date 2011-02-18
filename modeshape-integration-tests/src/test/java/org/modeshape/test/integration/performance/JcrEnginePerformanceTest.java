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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import javax.jcr.Node;
import org.jboss.byteman.contrib.bmunit.BMScript;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.test.ModeShapeSingleUseTest;

@Ignore
@RunWith( BMUnitRunner.class )
public class JcrEnginePerformanceTest extends ModeShapeSingleUseTest {

    @Ignore
    @BMScript( value = "jcr-configuration-failure", dir = "src/test/byteman" )
    @Test( expected = SecurityException.class )
    public void shouldThrowSecurityExceptionWhenBuildingEngine() throws Exception {
        startEngineUsing("config/configRepositoryForJdbc.xml");
    }

    @Ignore
    @BMScript( value = "jcr-performance", dir = "src/test/byteman" )
    @Test
    public void shouldStartEngineAndRecordPerformanceTrace() throws Exception {
        startEngineUsing("config/configRepositoryForJdbc.xml");
        assertNode("/");

        // Ensure the "car" node types are registered ...
        assertNodeType("car:Car", false, false, true, false, null, 0, 11, "nt:unstructured");

        // Add some content ...
        importContent("jdbc/cars-system-view-with-uuids.xml");
        session().save();

        print = true;
        printSubgraph(assertNode("/Cars"));

        logout();

        Node utility = session().getNode("/Cars/Utility");
        assertThat(utility, is(notNullValue()));
    }

    @Ignore
    @BMScript( value = "jcr-performance-check-permissions", dir = "src/test/byteman" )
    @Test
    public void shouldStartEngineAndGetRootNode() throws Exception {
        startEngineUsing("config/configRepositoryForJdbc.xml");
        assertNode("/");
    }
}
